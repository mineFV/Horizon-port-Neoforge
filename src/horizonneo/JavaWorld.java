package horizonneo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    /** Ids by type: a class's internal name, an array's descriptor, a lambda's key. */
    private final Map<String, Integer> ids = new TreeMap<>();
    private int next = PRIMITIVE_ARRAYS.size() + 1;

    JavaWorld(Map<String, ClassData> jar) {
        this(jar, Map.of());
    }

    JavaWorld(Map<String, ClassData> jar, Map<String, ClassData> model) {
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
        if (!type.startsWith("[") && !type.startsWith(LAMBDA)) throw new IllegalArgumentException("no id for " + type);
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

    /** A type's superclass and interfaces, from the jar, the model or the JDK; null when none of them has it. */
    private ClassData known(String name) {
        ClassData c = classes.get(name);
        return c != null ? c : jdkClass(name);
    }

    // ---- subtypes ----

    /** Whether type a (a class, an array descriptor or a lambda key) is a subtype of t; null when it can't be told. */
    Boolean subtype(String a, String t) {
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
        if (target != null && (target.access & ClassData.INTERFACE) == 0) {
            // A class: only the superclasses lead to it (a lambda's class has Object's alone).
            if (a.startsWith(LAMBDA)) return false;
            for (String c = classes.containsKey(a) ? classes.get(a).superName : null; c != null; ) {
                if (c.equals(t)) return true;
                ClassData data = known(c);
                if (data == null) return c.equals(OBJECT) || jar.contains(t) ? false : null;
                c = data.superName;
            }
            return classes.containsKey(a) ? false : null;
        }
        boolean unknown = false;
        Deque<String> todo = new ArrayDeque<>(List.of(start));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (!seen.add(c)) continue;
            if (c.equals(t)) return true;
            ClassData data = known(c);
            if (data == null) {
                // A type outside the jar can't be a subtype of one of the jar's: the game and Java's library don't
                // know the mod, and a mod this one is built on can't be built on it in turn. Only a type outside
                // the jar may hide behind one.
                if (!c.equals(OBJECT) && !jar.contains(t)) unknown = true;
                continue;
            }
            todo.addAll(data.interfaces);
            if (data.superName != null) todo.add(data.superName);
        }
        return unknown ? null : false;
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
            if (data == null) return new Selected(null, "on " + r.replace('/', '.') + " it may be " + c.replace('/', '.') + "'s, outside the jar");
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
            ClassData data = classes.get(i);
            if (data == null) return new Selected(null, "on " + r.replace('/', '.') + " it may be a default method of " + i.replace('/', '.') + ", outside the jar");
            ClassData.Method m = declared(i, name, descriptor);
            if (m != null && !m.isStatic() && (m.access() & (ClassData.ABSTRACT | 0x0002)) == 0) found.add(new JarRust.Id(i, name, descriptor));
            todo.addAll(data.interfaces);
        }
        if (found.size() == 1) return new Selected(found.get(0), null);
        if (declared(OBJECT, name, descriptor) == null && objectMethod(name, descriptor)) return new Selected(null, "it is java.lang.Object's own " + name);
        return new Selected(null, found.isEmpty() ? "nothing implements " + name + descriptor + " on " + r.replace('/', '.') : "several default methods could be " + name);
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
