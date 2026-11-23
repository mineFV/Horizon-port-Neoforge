package horizonneo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * The mod's classes as the translator sees them (the uniform object model, CLAUDE.md phase 4 (b)): every object is
 * an `Arc<Obj>` with a class id and its fields in slots, a superclass's first, as a JVM lays them out; `null` is
 * `None`. This answers what the JVM would: a field's slot, a static field's place, which method a call selects
 * for each class an object can be of, whether a type is another's subtype.
 *
 * The world is closed: the only objects are those the translated code makes (`new`, arrays, lambdas). So a type
 * test or a virtual call is decided over the classes the translation instantiates. A supertype outside the jar
 * (the game's, Java's library) hides its own supertypes: a question that needs them has no answer, and what asks
 * it isn't translated.
 */
final class JavaWorld {
    static final String OBJECT = "java/lang/Object";
    /** The arrays of primitives, with fixed ids 1 to 8 (their stores truncate by their kind). */
    static final List<String> PRIMITIVE_ARRAYS = List.of("[Z", "[B", "[C", "[S", "[I", "[J", "[F", "[D");

    /** The jar's classes and the library model's (`Library`): what the translator knows of. */
    final Map<String, ClassData> classes;
    /** The jar's own classes. */
    final Set<String> jar;
    /** Classes outside the jar and the model, known for their supertypes alone. */
    private final Map<String, ClassData> outside;
    /** Ids by type: a class's internal name, an array's descriptor, a lambda's key. */
    private final Map<String, Integer> ids = new TreeMap<>();
    private int next = PRIMITIVE_ARRAYS.size() + 1;

    JavaWorld(Map<String, ClassData> jar) {
        this(jar, Map.of());
    }

    JavaWorld(Map<String, ClassData> jar, Map<String, ClassData> model) {
        this(jar, model, Map.of());
    }

    /**
     * outside: classes of the game, NeoForge and the libraries the mod runs with (`--server`), known for their
     * supertypes alone: they answer subtype questions, and are never translated nor laid out.
     */
    JavaWorld(Map<String, ClassData> jar, Map<String, ClassData> model, Map<String, ClassData> outside) {
        this.outside = outside;
        Map<String, ClassData> all = new TreeMap<>(model);
        all.putAll(jar);
        this.classes = all;
        this.jar = new java.util.TreeSet<>(jar.keySet());
        for (int i = 0; i < PRIMITIVE_ARRAYS.size(); i++) ids.put(PRIMITIVE_ARRAYS.get(i), i + 1);
        for (String name : classes.keySet()) if (concrete(name)) ids.put(name, next++);
    }

    // ---- ids ----

    /** A type's id, given when first asked for an array or a lambda. */
    int id(String type) {
        Integer id = ids.get(type);
        if (id != null) return id;
        // An array, a lambda, or a type with no objects of its own (an interface, an abstract class, a type outside
        // the jar): it has an id when a class object stands for it, or for the class objects' tables.
        ids.put(type, next);
        return next++;
    }

    Map<String, Integer> ids() {
        return ids;
    }

    // ---- classes ----

    ClassData cls(String name) {
        return classes.get(name);
    }

    boolean interfaceType(String name) {
        ClassData c = classes.get(name);
        return c != null && (c.access & ClassData.INTERFACE) != 0;
    }

    /** The jar's services files (META-INF/services): each service's providers, as listed, by internal name. */
    final Map<String, List<String>> services = new TreeMap<>();

    /**
     * The providers ServiceLoader makes of a service of the jar's: each listed once, in order; null when that isn't
     * known (a service outside the jar may have providers elsewhere) or one can't be made as the JDK makes it: a
     * public, concrete class of the jar, a subtype of the service, with a public constructor without parameters.
     */
    List<String> providers(String service) {
        if (!jar.contains(service)) return null;
        List<String> listed = services.getOrDefault(service, List.of());
        for (String p : listed) {
            ClassData c = jar.contains(p) ? classes.get(p) : null;
            if (c == null || !concrete(p) || (c.access & ClassData.PUBLIC) == 0 || !Boolean.TRUE.equals(subtype(p, service))) return null;
            boolean constructor = false;
            for (ClassData.Method m : c.methods) if (m.name().equals("<init>") && m.descriptor().equals("()V") && (m.access() & ClassData.PUBLIC) != 0) constructor = true;
            if (!constructor) return null;
        }
        return listed;
    }

    /** A class of the jar that can have objects: not an interface nor abstract, with every superclass in the jar. */
    boolean concrete(String name) {
        ClassData c = classes.get(name);
        if (c == null || (c.access & (ClassData.INTERFACE | ClassData.ABSTRACT)) != 0) return false;
        return layout(name) != null;
    }

    /**
     * The instance fields' types in slot order (I, J, F, D, A), the superclasses' first; null when a superclass
     * isn't in the jar.
     */
    String layout(String name) {
        if (name.equals(OBJECT)) return "";
        ClassData c = classes.get(name);
        if (c == null || c.superName == null) return null;
        String sup = layout(c.superName);
        if (sup == null) return null;
        StringBuilder b = new StringBuilder(sup);
        for (ClassData.Field f : c.fields) if (!f.isStatic()) b.append(kind(f.descriptor()));
        return b.toString();
    }

    /** The stack kind of a descriptor: I (int, boolean, byte, char, short), J, F, D, A (a reference). */
    static char kind(String d) {
        return switch (d.charAt(0)) {
            case 'J' -> 'J';
            case 'F' -> 'F';
            case 'D' -> 'D';
            case 'L', '[' -> 'A';
            default -> 'I';
        };
    }

    // ---- fields ----

    /** A field as resolved: the class that declares it, its slot there, its kind. why: why it can't be, else null. */
    private Set<String> jarDeclared;

    /** Whether a class of the jar declares a method of this name and descriptor (MethodRust.sides). */
    boolean jarDeclares(String name, String descriptor) {
        if (jarDeclared == null) {
            Set<String> s = new java.util.HashSet<>();
            for (String c : jar) for (ClassData.Method m : classes.get(c).methods) s.add(m.name() + m.descriptor());
            jarDeclared = s;
        }
        return jarDeclared.contains(name + descriptor);
    }

    record FieldAt(String declaring, int slot, char kind, String descriptor, Object constant, String why) {}

    FieldAt field(String owner, String name, String descriptor, boolean isStatic) {
        if (isStatic) {
            // The JVM's order: the class, its superinterfaces, then its superclass.
            Deque<String> todo = new ArrayDeque<>(List.of(owner));
            Set<String> seen = new HashSet<>();
            while (!todo.isEmpty()) {
                String c = todo.poll();
                if (!seen.add(c)) continue;
                ClassData data = classes.get(c);
                if (data == null) {
                    if (c.equals(OBJECT)) continue;
                    return new FieldAt(null, 0, ' ', descriptor, null, "a static field outside the jar: " + owner.replace('/', '.') + "." + name + " (in " + c.replace('/', '.') + " or above it)");
                }
                int slot = 0;
                for (ClassData.Field f : data.fields) {
                    if (!f.isStatic()) continue;
                    if (f.name().equals(name) && f.descriptor().equals(descriptor)) {
                        return new FieldAt(c, slot, kind(descriptor), descriptor, f.constant(), null);
                    }
                    slot++;
                }
                todo.addAll(data.interfaces);
                if (data.superName != null) todo.add(data.superName);
            }
            return new FieldAt(null, 0, ' ', descriptor, null, "a static field it can't find: " + owner.replace('/', '.') + "." + name);
        }
        for (String c = owner; c != null && !c.equals(OBJECT); ) {
            ClassData data = classes.get(c);
            if (data == null) return new FieldAt(null, 0, ' ', descriptor, null, "a field of a class outside the jar: " + c.replace('/', '.') + "." + name);
            String before = layout(data.superName);
            if (before == null) return new FieldAt(null, 0, ' ', descriptor, null, "a field of a class whose superclass is outside the jar: " + c.replace('/', '.') + "." + name);
            int slot = before.length();
            for (ClassData.Field f : data.fields) {
                if (f.isStatic()) continue;
                if (f.name().equals(name) && f.descriptor().equals(descriptor)) return new FieldAt(c, slot, kind(descriptor), descriptor, null, null);
                slot++;
            }
            c = data.superName;
        }
        return new FieldAt(null, 0, ' ', descriptor, null, "a field it can't find: " + owner.replace('/', '.') + "." + name);
    }

    /** A class's static fields, in slot order. */
    List<ClassData.Field> statics(String name) {
        List<ClassData.Field> out = new ArrayList<>();
        for (ClassData.Field f : classes.get(name).fields) if (f.isStatic()) out.add(f);
        return out;
    }

    /** The classes initialized before this one, from the top: its superclasses in the jar, then it. */
    List<String> initOrder(String name) {
        List<String> out = new ArrayList<>();
        for (String c = name; c != null && classes.containsKey(c); c = classes.get(c).superName) out.add(0, c);
        return out;
    }

    /** Whether initializing the class reaches a superclass outside the jar (other than Object), whose own initializer isn't known. */
    boolean initLeaves(String name) {
        String c = name;
        while (c != null && classes.containsKey(c)) c = classes.get(c).superName;
        return c != null && !c.equals(OBJECT);
    }

    // ---- the JDK's own types, for their supertypes ----

    private final Map<String, ClassData> jdk = new TreeMap<>();

    /**
     * A class of Java's library the model doesn't have, read from the JDK this tool runs on (`jrt:/`), for its
     * supertypes only: they are the same in every JDK since the type came in. null when it isn't one of the JDK's.
     */
    ClassData jdkClass(String name) {
        if (!name.startsWith("java/") && !name.startsWith("javax/")) return null;
        if (jdk.containsKey(name)) return jdk.get(name);
        ClassData found = null;
        try {
            java.nio.file.FileSystem jrt = java.nio.file.FileSystems.getFileSystem(java.net.URI.create("jrt:/"));
            int slash = name.lastIndexOf('/');
            java.nio.file.Path modules = jrt.getPath("/packages", name.substring(0, slash).replace('/', '.'));
            if (java.nio.file.Files.isDirectory(modules)) {
                try (var list = java.nio.file.Files.list(modules)) {
                    for (java.nio.file.Path module : list.toList()) {
                        java.nio.file.Path file = jrt.getPath("/modules", module.getFileName().toString(), name + ".class");
                        if (java.nio.file.Files.isRegularFile(file)) found = ClassData.parse(java.nio.file.Files.readAllBytes(file));
                    }
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            found = null;
        }
        jdk.put(name, found);
        return found;
    }

    /** A type's superclass and interfaces, from the jar, the model, the JDK or the classes outside; null when none has it. */
    private ClassData known(String name) {
        ClassData c = classes.get(name);
        if (c != null) return c;
        c = jdkClass(name);
        return c != null ? c : outside.get(name);
    }

    /**
     * The classes of these jars, by internal name, the first jar's first: what `--server` reads from a NeoForge
     * server's libraries (NeoForge's patched classes before the game's own, as `Dump.classpath` orders them).
     */
    static Map<String, ClassData> outside(List<java.nio.file.Path> jars) throws java.io.IOException {
        Map<String, ClassData> out = new java.util.HashMap<>();
        for (java.nio.file.Path p : jars) {
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(p.toFile())) {
                for (var e = zip.entries(); e.hasMoreElements(); ) {
                    java.util.zip.ZipEntry entry = e.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory() || !name.endsWith(".class") || name.startsWith("META-INF/") || name.endsWith("module-info.class")) continue;
                    String internal = name.substring(0, name.length() - ".class".length());
                    if (out.containsKey(internal)) continue;
                    try {
                        ClassData c = ClassData.parse(ModJar.read(zip, entry, Analyzer.MAX_CLASS));
                        out.putIfAbsent(c.name, c);
                    } catch (RuntimeException | java.io.IOException unreadable) {
                        // A class this reader can't parse stays unknown: questions about it stay unanswered.
                    }
                }
            }
        }
        return out;
    }

    /**
     * A supertype of c (c itself included) that isn't known, or null when every one is: an object of such a class
     * would leave every question "is it a ...?" about it without an answer, so the translation doesn't make one.
     */
    String unknownSupertype(String c) {
        Deque<String> todo = new ArrayDeque<>(List.of(c));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String t = todo.poll();
            if (!seen.add(t) || t.equals(OBJECT)) continue;
            ClassData data = known(t);
            if (data == null) return t;
            todo.addAll(data.interfaces);
            if (data.superName != null) todo.add(data.superName);
        }
        return null;
    }

    // ---- subtypes ----

    /**
     * A type's supertypes, found once: its superclasses in a chain (broken: it reached a class not read, not
     * Object), and all its supertypes (unknown: one of them wasn't read). Answering each "is a a t?" from them, rather
     * than remembering each answer, keeps a big jar's pairs out of memory.
     */
    private record Supers(Set<String> chain, boolean chainBroken, Set<String> all, boolean unknownAny) {}

    private final Map<String, Supers> supers = new java.util.HashMap<>();

    private Supers supers(String start) {
        Supers found = supers.get(start);
        if (found != null) return found;
        Set<String> chain = new HashSet<>();
        boolean broken = false;
        ClassData first = known(start);
        if (first != null) {
            for (String c = first.superName; c != null; ) {
                chain.add(c);
                ClassData data = known(c);
                if (data == null) {
                    broken = !c.equals(OBJECT);
                    break;
                }
                c = data.superName;
            }
        }
        Set<String> all = new HashSet<>();
        boolean unknown = false;
        Deque<String> todo = new ArrayDeque<>(List.of(start));
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (!all.add(c)) continue;
            ClassData data = known(c);
            if (data == null) {
                if (!c.equals(OBJECT)) unknown = true;
                continue;
            }
            todo.addAll(data.interfaces);
            if (data.superName != null) todo.add(data.superName);
        }
        found = new Supers(chain, broken, all, unknown);
        supers.put(start, found);
        return found;
    }

    /** Whether type a (a class, an array descriptor or a lambda key) is a subtype of t; null when it can't be told. */
    Boolean subtype(String a, String t) {
        return subtypeOf(a, t);
    }

    private Boolean subtypeOf(String a, String t) {
        if (a.equals(t) || t.equals(OBJECT)) return true;
        if (a.startsWith("[")) {
            if (t.equals("java/lang/Cloneable") || t.equals("java/io/Serializable")) return true;
            if (!t.startsWith("[")) return false;
            String ac = a.substring(1), tc = t.substring(1);
            if (ac.length() == 1 || tc.length() == 1) return ac.equals(tc);
            return subtype(type(ac), type(tc));
        }
        if (t.startsWith("[")) return false;
        String start = a.startsWith(LAMBDA) ? lambdaInterface(a) : a;
        if (start.equals(t)) return true;
        ClassData target = classes.get(t);
        // A class of the library model that isn't public (a nested one of java.util's) has no subclass outside java.*,
        // where only the JDK may put classes: none of the jar's or the game's is one.
        if (target != null && (target.access & ClassData.PUBLIC) == 0 && t.startsWith("java/") && !a.startsWith("java/") && !a.startsWith("[")) return false;
        // A final class has no subclass: only itself is one (an array or a lambda's class never).
        if (target != null && (target.access & ClassData.FINAL) != 0 && (target.access & ClassData.INTERFACE) == 0) return false;
        if (target != null && (target.access & ClassData.INTERFACE) == 0) {
            // A class: only the superclasses lead to it (a lambda's class has Object's alone).
            if (a.startsWith(LAMBDA)) return false;
            if (known(a) == null) return jar.contains(t) && !jar.contains(a) ? false : null;
            Supers sup = supers(a);
            if (sup.chain().contains(t)) return true;
            if (!sup.chainBroken() || jar.contains(t)) return false;
            return null;
        }
        // A type outside the jar can't be a subtype of one of the jar's: the game and Java's library don't know the
        // mod, and a mod this one is built on can't be built on it in turn. Only a type outside the jar may hide
        // behind one.
        Supers sup = supers(start);
        if (sup.all().contains(t)) return true;
        if (sup.unknownAny() && !jar.contains(t)) return null;
        return false;
    }

    /** `Lfoo/Bar;` as foo/Bar, `[I` as itself. */
    static String type(String descriptor) {
        return descriptor.startsWith("L") ? descriptor.substring(1, descriptor.length() - 1) : descriptor;
    }

    // ---- methods ----

    /** The method a class declares, by name and descriptor; null when none. */
    ClassData.Method declared(String owner, String name, String descriptor) {
        ClassData c = classes.get(owner);
        if (c == null) return null;
        for (ClassData.Method m : c.methods) if (m.name().equals(name) && m.descriptor().equals(descriptor)) return m;
        return null;
    }

    /** A selected method: its id, or why there is none certain. */
    record Selected(JarRust.Id id, String why) {}

    /**
     * What invokevirtual or invokeinterface selects on an object of class r (JVMS 5.4.6): the first class up from r
     * that declares it, not private nor static nor abstract; else the one default method among its interfaces.
     */
    Selected select(String r, String name, String descriptor) {
        for (String c = r; c != null; ) {
            ClassData data = classes.get(c);
            if (data == null && c.equals(OBJECT)) break;
            if (data == null) {
                // Read from outside (the JDK's, the game's with --server): it answers which methods it declares.
                ClassData read = r.startsWith(LAMBDA) || !interfaceOutside(c) ? null : known(c);
                if (read == null) return new Selected(null, "on " + r.replace('/', '.') + " it may be " + c.replace('/', '.') + "'s, outside the jar");
                for (ClassData.Method d : read.methods) {
                    if (d.name().equals(name) && d.descriptor().equals(descriptor) && !d.isStatic() && (d.access() & (ClassData.ABSTRACT | 0x0002)) == 0) {
                        return new Selected(null, "on " + r.replace('/', '.') + " it is " + c.replace('/', '.') + "'s, outside the jar");
                    }
                }
                break;
            }
            ClassData.Method m = declared(c, name, descriptor);
            if (m != null && !m.isStatic() && (m.access() & 0x0002) == 0) {
                if ((m.access() & ClassData.ABSTRACT) != 0) break;
                return new Selected(new JarRust.Id(c, name, descriptor), null);
            }
            c = data.superName;
        }
        // Default methods: the one non-abstract among the superinterfaces.
        List<JarRust.Id> found = new ArrayList<>();
        Deque<String> todo = new ArrayDeque<>();
        for (String c = r; c != null && classes.containsKey(c); c = classes.get(c).superName) todo.addAll(classes.get(c).interfaces);
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String i = todo.poll();
            if (!seen.add(i)) continue;
            ClassData data = known(i);
            if (data == null) return new Selected(null, "on " + r.replace('/', '.') + " it may be a default method of " + i.replace('/', '.') + ", outside the jar");
            ClassData.Method m = null;
            for (ClassData.Method d : data.methods) if (d.name().equals(name) && d.descriptor().equals(descriptor)) m = d;
            if (m != null && !m.isStatic() && (m.access() & (ClassData.ABSTRACT | 0x0002)) == 0) {
                // An interface read from outside (the JDK's, the game's) declares this default: running it isn't translated.
                if (!classes.containsKey(i)) return new Selected(null, "on " + r.replace('/', '.') + " it is a default method of " + i.replace('/', '.') + ", outside the jar");
                found.add(new JarRust.Id(i, name, descriptor));
            }
            todo.addAll(data.interfaces);
        }
        // The maximally-specific ones (JVMS 5.4.3.3): a default an interface overrides in a subinterface isn't one.
        List<JarRust.Id> specific = new ArrayList<>();
        for (JarRust.Id f : found) {
            boolean overridden = false;
            for (JarRust.Id g : found) if (g != f && Boolean.TRUE.equals(subtype(g.owner(), f.owner()))) overridden = true;
            if (!overridden) specific.add(f);
        }
        found = specific;
        if (found.size() == 1) return new Selected(found.get(0), null);
        if (declared(OBJECT, name, descriptor) == null && objectMethod(name, descriptor)) return new Selected(null, "it is java.lang.Object's own " + name);
        return new Selected(null, found.isEmpty() ? "nothing implements " + name + descriptor + " on " + r.replace('/', '.') : "several default methods could be " + name);
    }

    /**
     * What invokevirtual or invokeinterface selects on a lambda: its class extends Object and implements the interface,
     * so Object's methods come first (an interface that redeclares equals, as Comparator does, doesn't hide Object's),
     * then the interface's own and its defaults.
     */
    Selected selectOnLambda(String key, String name, String descriptor) {
        ClassData.Method m = declared(OBJECT, name, descriptor);
        if (m != null && !m.isStatic() && (m.access() & (ClassData.ABSTRACT | 0x0002)) == 0) return new Selected(new JarRust.Id(OBJECT, name, descriptor), null);
        return select(lambdaInterface(key), name, descriptor);
    }

    /** An enum's own class (not a constant's body): ACC_ENUM, and Enum its superclass. */
    boolean enumClass(String c) {
        ClassData data = classes.get(c);
        return data != null && (data.access & 0x4000) != 0 && "java/lang/Enum".equals(data.superName);
    }

    /**
     * The classes in the jars the mod carries (META-INF/jarjar): there when it runs, though not read here, so never
     * taken for absent.
     */
    final Set<String> carried = new HashSet<>();

    /**
     * Whether class c isn't there when the mod runs, known only with --server: not the jar's, the model's, one of the
     * server's libraries (the game's and NeoForge's among them), one the jar carries, nor the JDK's. Another mod's most
     * often: a mod's translation runs alone on Horizon, so another mod's classes aren't there, even where a modpack
     * would have them (the maintainer's decision, 2026-11-23); or the client's (clientOnly). Java's own library's are
     * never taken for absent: the JDK the mod runs on may not be the one this tool runs on.
     */
    boolean absent(String c) {
        if (!outside.containsKey("net/minecraft/server/MinecraftServer") || c.startsWith("[") || Library.library(c)) return false;
        return !classes.containsKey(c) && !outside.containsKey(c) && !carried.contains(c) && jdkClass(c) == null;
    }

    /**
     * Whether c is a class of the game or NeoForge that a dedicated server doesn't have (the client's), known only
     * with --server: a server's classes were read (MinecraftServer among them) and c isn't one of them.
     */
    boolean clientOnly(String c) {
        if (!outside.containsKey("net/minecraft/server/MinecraftServer")) return false;
        if (!(c.startsWith("net/minecraft/") || c.startsWith("com/mojang/blaze3d/") || c.startsWith("net/neoforged/"))) return false;
        return !classes.containsKey(c) && !outside.containsKey(c);
    }

    /**
     * Whether c is a class marked @OnlyIn(Dist.CLIENT), of the jar or read with --server: on a dedicated server,
     * NeoForge refuses to load it (observed: a RuntimeException, "Attempted to load class c for invalid dist
     * DEDICATED_SERVER", when an instruction naming it runs).
     */
    boolean clientMarked(String c) {
        ClassData d = jar.contains(c) ? classes.get(c) : outside.get(c);
        if (d == null) return false;
        for (ClassData.Annotation a : d.annotations) {
            if (a.type().equals("Lnet/neoforged/api/distmarker/OnlyIn;") && a.values().get("value") instanceof ClassData.EnumConstant e && e.name().equals("CLIENT")) return true;
        }
        return false;
    }

    /** A supertype of c that is the client's (clientOnly or clientMarked), c itself not counted; null when none is. */
    String clientSupertype(String c) {
        java.util.Deque<String> todo = new java.util.ArrayDeque<>();
        Set<String> seen = new java.util.HashSet<>();
        ClassData d = known(c);
        if (d == null) return null;
        if (d.superName != null) todo.add(d.superName);
        todo.addAll(d.interfaces);
        while (!todo.isEmpty()) {
            String t = todo.poll();
            if (!seen.add(t)) continue;
            if (clientOnly(t) || clientMarked(t)) return t;
            ClassData x = known(t);
            if (x == null) continue;
            if (x.superName != null) todo.add(x.superName);
            todo.addAll(x.interfaces);
        }
        return null;
    }

    /** A type's superclass and interfaces as far as they are known (the jar, the model, the JDK, --server); null when it isn't. */
    ClassData knownClass(String name) {
        return known(name);
    }

    /** Whether c is an interface known from outside the jar and the model. */
    private boolean interfaceOutside(String c) {
        ClassData data = known(c);
        return data != null && !classes.containsKey(c) && (data.access & ClassData.INTERFACE) != 0;
    }

    private static boolean objectMethod(String name, String descriptor) {
        return Set.of("equals(Ljava/lang/Object;)Z", "hashCode()I", "toString()Ljava/lang/String;", "getClass()Ljava/lang/Class;", "clone()Ljava/lang/Object;",
                "finalize()V", "notify()V", "notifyAll()V", "wait()V", "wait(J)V", "wait(JI)V").contains(name + descriptor);
    }

    /**
     * What invokespecial calls (JVMS 6.5, invokespecial): a constructor or a private method of the class named; a
     * superclass's method, from that class up.
     */
    Selected special(String current, String owner, String name, String descriptor) {
        if (name.equals("<init>")) {
            if (owner.equals(OBJECT)) return new Selected(null, null);
            if (!classes.containsKey(owner)) return new Selected(null, "it calls the constructor of " + owner.replace('/', '.') + ", outside the jar");
            return declared(owner, name, descriptor) == null ? new Selected(null, "no such constructor") : new Selected(new JarRust.Id(owner, name, descriptor), null);
        }
        ClassData.Method m = declared(owner, name, descriptor);
        if (m != null && (m.access() & 0x0002) != 0) return new Selected(new JarRust.Id(owner, name, descriptor), null);
        return select(owner, name, descriptor);
    }

    // ---- lambdas ----

    static final String LAMBDA = "lambda:";

    /** The key of a primitive type's or void's class object: "primitive:I" for int.class. */
    static final String PRIMITIVE = "primitive:";

    /** The descriptors of the primitive types and void. */
    static final String PRIMITIVES = "ZBCSIJFDV";

    /** The ObjectMethods sites (a record's equals, hashCode, toString), by key. */
    final Map<String, MethodRust.ObjectMethodsSite> objectMethods = new LinkedHashMap<>();

    /** The lambda sites the translation keeps, by key (`lambda:owner#index`). */
    final Map<String, ClassData.LambdaSite> lambdas = new LinkedHashMap<>();

    static String lambdaKey(String owner, int index) {
        return LAMBDA + owner + "#" + index;
    }

    String lambdaInterface(String key) {
        return JavaWorld.type(ClassData.returns(lambdas.get(key).site()));
    }

    /** The concrete classes of the jar, sorted. */
    List<String> concreteClasses() {
        List<String> out = new ArrayList<>();
        for (String name : classes.keySet()) if (concrete(name)) out.add(name);
        return out;
    }
}
