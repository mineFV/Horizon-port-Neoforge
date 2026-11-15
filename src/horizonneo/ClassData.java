package horizonneo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * One class file, read as far as the analysis needs (the Java Virtual Machine Specification, chapter
 * 4): its names, its fields and methods with their annotations, and for each method the constants,
 * field accesses, calls and lambdas of its code, in the order they are written.
 *
 * Nothing is decompiled: no expression, statement or control flow is rebuilt. What a method does is
 * known only as that flat list of steps.
 */
final class ClassData {
    static final int STATIC = 0x0008, PUBLIC = 0x0001, SYNTHETIC = 0x1000, INTERFACE = 0x0200, ABSTRACT = 0x0400, FINAL = 0x0010;

    /**
     * An annotation: its type as a descriptor (`Lnet/neoforged/fml/common/Mod;`) and its values by name.
     * A value is a String, Number, Boolean, Character, EnumConstant, TypeName, Annotation, or a List of these.
     */
    record Annotation(String type, Map<String, Object> values) {
        /** The value `name` when it is text, else null. */
        String text(String name) {
            return values.get(name) instanceof String text ? text : null;
        }

        /** The value `name` as a list: an array's items, one value as a list of one, nothing as none. */
        List<Object> list(String name) {
            Object value = values.get(name);
            if (value == null) return List.of();
            return value instanceof List<?> items ? List.copyOf(items) : List.of(value);
        }
    }

    /** An enum constant in an annotation: `Dist.CLIENT` is (`Lnet/neoforged/api/distmarker/Dist;`, `CLIENT`). */
    record EnumConstant(String type, String name) {}

    /** A class in an annotation, as a descriptor: `Minecraft.class` is `Lnet/minecraft/client/Minecraft;`. */
    record TypeName(String descriptor) {}

    /**
     * signature: the generic type as the class file keeps it (`...DeferredHolder<LBlock;LMyBlock;>;`), or null.
     * constant: its ConstantValue (an Integer, Long, Float, Double or String), set before the class initializes; or null.
     */
    record Field(int access, String name, String descriptor, String signature, List<Annotation> annotations, Object constant) {
        boolean isStatic() {
            return (access & STATIC) != 0;
        }

        Annotation annotation(String type) {
            return find(annotations, type);
        }
    }

    /**
     * code: the steps of the method's body the analysis looks at, in order; empty without a body. body: the
     * body itself, as bytes, for the translator (`Bytecode` decodes it); null without one.
     */
    record Method(int access, String name, String descriptor, List<Annotation> annotations, List<Op> code, Body body) {
        boolean isStatic() {
            return (access & STATIC) != 0;
        }

        Annotation annotation(String type) {
            return find(annotations, type);
        }
    }

    /** A method's Code attribute: its bytecode, its limits and its exception handlers, in the class's order. */
    record Body(byte[] code, int maxStack, int maxLocals, List<Handler> handlers) {}

    /** An exception handler: [start, end) of the code it covers, where it goes, and what it catches (null for any). */
    record Handler(int start, int end, int handler, String type) {}

    /**
     * The constant pool, read only through what a method's code names: a number, a text or a class for `ldc`,
     * a field or method for the instructions that name one, a class for `new`, `checkcast` and the like.
     */
    interface Pool {
        /** What `ldc` loads: an Integer, Float, Long, Double, String, or a TypeName for a class; null for any other. */
        Object constant(int index);

        /** A field or method: owner, name, descriptor. */
        String[] member(int index);

        /** A class's internal name (or an array's descriptor). */
        String className(int index);

        /** An invokedynamic made by javac for a lambda or a method reference; null for any other. */
        Op lambda(int index);

        /** An invokedynamic's name and descriptor. */
        String[] dynamic(int index);

        /** An invokedynamic of LambdaMetafactory.metafactory (not altMetafactory): what it makes; null for any other. */
        LambdaSite lambdaSite(int index);

        /** An invokedynamic's bootstrap method (owner, name) and its static arguments as `constant` reads them. */
        Bootstrap bootstrap(int index);
    }

    /** An invokedynamic's bootstrap: the method's class and name, and its static arguments (null for those `constant` doesn't read). */
    record Bootstrap(String owner, String name, List<Object> arguments) {}

    /** A method handle among a bootstrap's arguments: its kind (1 getField ... 9 invokeInterface) and what it names. */
    record Handle(int kind, String owner, String name, String descriptor) {}

    /**
     * A lambda or method reference, as LambdaMetafactory.metafactory makes it: the interface's method (name, and its
     * erased descriptor), the call site's descriptor (the captured values in, the interface out), the method handle
     * it runs (its kind, 5 virtual, 6 static, 7 special, 8 a constructor, 9 interface) and the interface method's
     * real descriptor.
     */
    record LambdaSite(String samName, String site, String erased, int kind, String owner, String name, String descriptor, String instantiated) {}

    /** What a step of a method's code is. */
    enum Kind {
        /** A local variable holding an object is read: value is its slot (0 is `this` in an instance method). */
        LOCAL,
        /** A text constant: value. */
        STRING,
        /** A number constant (booleans are 0 and 1): value. */
        NUMBER,
        /** A class constant (`Foo.class`): owner. */
        TYPE,
        GET_STATIC, PUT_STATIC, GET_FIELD, PUT_FIELD,
        /** A method call: value is how ("static", "virtual", "special", "interface"). */
        CALL,
        /** An object is made: owner is its class. */
        NEW,
        /** A lambda or method reference is made: owner, name and descriptor are the method it runs; value is a Lambda. */
        LAMBDA
    }

    /** One step. Which of owner, name, descriptor and value are set depends on the kind. */
    record Op(Kind kind, String owner, String name, String descriptor, Object value) {}

    /**
     * How a lambda or method reference is used. functionalInterface: what it is passed as
     * (`java/util/function/Consumer`). instantiatedType: the interface's method with the real types
     * (`(Lnet/neoforged/.../ClientTickEvent$Post;)V`). captured: the descriptors of the values it
     * carries along (`this`, locals). constructor: it is `Foo::new`.
     */
    record Lambda(String functionalInterface, String instantiatedType, List<String> captured, boolean constructor) {}

    /** A class file this tool can't read. */
    static final class Malformed extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Malformed(String message) {
            super(message);
        }
    }

    final String name;
    /** null for java/lang/Object and module descriptors. */
    final String superName;
    final List<String> interfaces;
    final int access;
    final List<Annotation> annotations;
    final List<Field> fields;
    final List<Method> methods;
    /**
     * Every class this one names, in its constant pool or in the types of its members and of what it
     * calls, as internal names (`net/minecraft/client/Minecraft`), sorted.
     */
    final Set<String> classRefs;
    /** The constant pool, for the translator. */
    final Pool pool;
    /**
     * From the InnerClasses attribute, this class's own entry: the class it is a member of (null when it is a local or
     * anonymous class, or not nested) and its simple name as the source names it (null when anonymous); nested is
     * false when there is no entry for it, a top-level class.
     */
    boolean nested;
    String outerName;
    String innerName;

    private ClassData(String name, String superName, List<String> interfaces, int access, List<Annotation> annotations,
                      List<Field> fields, List<Method> methods, Set<String> classRefs, Pool pool) {
        this.name = name;
        this.superName = superName;
        this.interfaces = List.copyOf(interfaces);
        this.access = access;
        this.annotations = List.copyOf(annotations);
        this.fields = List.copyOf(fields);
        this.methods = List.copyOf(methods);
        this.classRefs = classRefs;
        this.pool = pool;
    }

    Annotation annotation(String type) {
        return find(annotations, type);
    }

    private static Annotation find(List<Annotation> annotations, String type) {
        for (Annotation a : annotations) if (a.type().equals(type)) return a;
        return null;
    }

    static ClassData parse(byte[] bytes) throws IOException {
        try {
            return new Parser(bytes).read();
        } catch (Malformed | BufferUnderflowException | IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new IOException("not a class file this tool can read" + (e instanceof Malformed ? ": " + e.getMessage() : ""));
        }
    }

    // ---- descriptors ----

    /** The parameter types of a method descriptor, each as a descriptor: `(ILjava/lang/String;)V` is `I` and `Ljava/lang/String;`. */
    static List<String> parameters(String methodDescriptor) {
        List<String> out = new ArrayList<>();
        int i = methodDescriptor.indexOf('(') + 1, end = methodDescriptor.indexOf(')');
        if (i <= 0 || end < 0) throw new Malformed("`" + methodDescriptor + "` isn't a method descriptor");
        while (i < end) {
            int from = i;
            while (methodDescriptor.charAt(i) == '[') i++;
            if (methodDescriptor.charAt(i) == 'L') i = methodDescriptor.indexOf(';', i);
            if (i < 0 || i >= end) throw new Malformed("`" + methodDescriptor + "` isn't a method descriptor");
            i++;
            out.add(methodDescriptor.substring(from, i));
        }
        return out;
    }

    /** The return type of a method descriptor, as a descriptor. */
    static String returns(String methodDescriptor) {
        return methodDescriptor.substring(methodDescriptor.indexOf(')') + 1);
    }

    /** `Lfoo/Bar;` as `foo/Bar`; null for a primitive or an array. */
    static String internal(String descriptor) {
        return descriptor.length() > 2 && descriptor.charAt(0) == 'L' && descriptor.endsWith(";")
                ? descriptor.substring(1, descriptor.length() - 1) : null;
    }

    /** `foo/Bar$Baz` as `Bar.Baz`: a class's name as source writes it, without its package. */
    static String simple(String internalName) {
        return internalName.substring(internalName.lastIndexOf('/') + 1).replace('$', '.');
    }

    // ---- reading ----

    private static final class Parser implements Pool {
        private static final String LAMBDA_FACTORY = "java/lang/invoke/LambdaMetafactory";

        private final ByteBuffer in;
        // The constant pool: each entry's tag, its own value (text or number) and the entries it points to.
        private int[] tag;
        private Object[] value;
        private int[] first, second;
        // BootstrapMethods: for each, the method handle entry and its arguments' entries.
        private final List<int[]> bootstraps = new ArrayList<>();

        Parser(byte[] bytes) {
            this.in = ByteBuffer.wrap(bytes);
        }

        ClassData read() {
            if (in.getInt() != 0xCAFEBABE) throw new Malformed("it doesn't start as a class file does");
            in.getInt();
            pool();
            int access = u2();
            String name = className(u2());
            int superIndex = u2();
            String superName = superIndex == 0 ? null : className(superIndex);
            List<String> interfaces = new ArrayList<>();
            for (int n = u2(); n > 0; n--) interfaces.add(className(u2()));

            List<Field> fields = new ArrayList<>();
            for (int n = u2(); n > 0; n--) {
                int fieldAccess = u2();
                String fieldName = utf(u2()), descriptor = utf(u2());
                Attributes a = attributes();
                fields.add(new Field(fieldAccess, fieldName, descriptor, a.signature, a.annotations, a.constant));
            }

            // Code is decoded after the class's own attributes: lambdas need its BootstrapMethods.
            List<Object[]> methodHeads = new ArrayList<>();
            for (int n = u2(); n > 0; n--) {
                int methodAccess = u2();
                String methodName = utf(u2()), descriptor = utf(u2());
                methodHeads.add(new Object[] {methodAccess, methodName, descriptor, attributes()});
            }
            Attributes own = attributes();

            List<Method> methods = new ArrayList<>();
            for (Object[] head : methodHeads) {
                Attributes a = (Attributes) head[3];
                List<Op> code = a.codeLength < 0 ? List.of() : code(a.codeStart, a.codeLength);
                Body body = null;
                if (a.codeLength >= 0) {
                    byte[] bytes = new byte[a.codeLength];
                    in.get(a.codeStart, bytes);
                    body = new Body(bytes, a.maxStack, a.maxLocals, List.copyOf(a.handlers));
                }
                methods.add(new Method((Integer) head[0], (String) head[1], (String) head[2], a.annotations, code, body));
            }

            Set<String> classRefs = new TreeSet<>();
            for (int i = 1; i < tag.length; i++) {
                if (tag[i] == 7) {
                    String ref = utf(first[i]);
                    if (ref.startsWith("[")) named(ref, classRefs);
                    else classRefs.add(ref);
                } else if (tag[i] == 12) {
                    named(utf(second[i]), classRefs);
                } else if (tag[i] == 16) {
                    named(utf(first[i]), classRefs);
                }
            }
            for (Field f : fields) named(f.descriptor(), classRefs);
            for (Method m : methods) named(m.descriptor(), classRefs);
            ClassData out = new ClassData(name, superName, interfaces, access, own.annotations, fields, methods, classRefs, this);
            for (int[] entry : own.inner) {
                if (entry[0] == 0 || !className(entry[0]).equals(name)) continue;
                out.nested = true;
                out.outerName = entry[1] == 0 ? null : className(entry[1]);
                out.innerName = entry[2] == 0 ? null : utf(entry[2]);
            }
            return out;
        }

        /** Adds the classes a descriptor names: `(ILfoo/Bar;)Lfoo/Baz;` names foo/Bar and foo/Baz. */
        private static void named(String descriptor, Set<String> into) {
            for (int i = descriptor.indexOf('L'); i >= 0; ) {
                int end = descriptor.indexOf(';', i);
                if (end < 0) return;
                into.add(descriptor.substring(i + 1, end));
                i = descriptor.indexOf('L', end);
            }
        }

        // ---- the constant pool ----

        private void pool() {
            int count = u2();
            tag = new int[count];
            value = new Object[count];
            first = new int[count];
            second = new int[count];
            for (int i = 1; i < count; i++) {
                int t = u1();
                tag[i] = t;
                switch (t) {
                    case 1 -> value[i] = modifiedUtf8();
                    case 3 -> value[i] = in.getInt();
                    case 4 -> value[i] = in.getFloat();
                    case 5 -> value[i] = in.getLong();
                    case 6 -> value[i] = in.getDouble();
                    case 7, 8, 16, 19, 20 -> first[i] = u2();
                    case 9, 10, 11, 12, 17, 18 -> {
                        first[i] = u2();
                        second[i] = u2();
                    }
                    case 15 -> {
                        first[i] = u1();
                        second[i] = u2();
                    }
                    default -> throw new Malformed("constant pool entry " + i + " has the unknown tag " + t);
                }
                // A long or a double takes two entries.
                if (t == 5 || t == 6) i++;
            }
        }

        /** A constant pool string: the JVM's own flavour of UTF-8, which DataInputStream reads. */
        private String modifiedUtf8() {
            int length = u2();
            byte[] framed = new byte[length + 2];
            framed[0] = (byte) (length >> 8);
            framed[1] = (byte) length;
            in.get(framed, 2, length);
            try {
                return new DataInputStream(new ByteArrayInputStream(framed)).readUTF();
            } catch (IOException e) {
                throw new Malformed("a constant pool string isn't valid");
            }
        }

        private void expect(int index, int... tags) {
            if (index <= 0 || index >= tag.length) throw new Malformed("constant pool index " + index + " is out of range");
            for (int t : tags) if (tag[index] == t) return;
            throw new Malformed("constant pool entry " + index + " isn't of the kind its use needs");
        }

        private String utf(int index) {
            expect(index, 1);
            return (String) value[index];
        }

        @Override
        public String className(int index) {
            expect(index, 7);
            return utf(first[index]);
        }

        @Override
        public Object constant(int index) {
            expect(index, 3, 4, 5, 6, 7, 8, 15, 16, 17);
            return switch (tag[index]) {
                case 3, 4, 5, 6 -> value[index];
                case 8 -> utf(first[index]);
                case 7 -> new TypeName(utf(first[index]));
                default -> null;
            };
        }

        @Override
        public String[] dynamic(int index) {
            expect(index, 18);
            int nameAndType = second[index];
            expect(nameAndType, 12);
            return new String[] {utf(first[nameAndType]), utf(second[nameAndType])};
        }

        /** A field or method reference as owner, name, descriptor. */
        @Override
        public String[] member(int index) {
            expect(index, 9, 10, 11);
            int nameAndType = second[index];
            expect(nameAndType, 12);
            return new String[] {className(first[index]), utf(first[nameAndType]), utf(second[nameAndType])};
        }

        // ---- attributes ----

        /** What the attributes of a class, field or method hold, as far as it is read. */
        private static final class Attributes {
            final List<Annotation> annotations = new ArrayList<>();
            String signature;
            int codeStart, codeLength = -1, maxStack, maxLocals;
            Object constant;
            final List<Handler> handlers = new ArrayList<>();
            /** InnerClasses: inner class, outer class, inner name, flags (constant pool indexes; 0 for none). */
            final List<int[]> inner = new ArrayList<>();
        }

        private Attributes attributes() {
            Attributes out = new Attributes();
            for (int n = u2(); n > 0; n--) {
                String name = utf(u2());
                int length = in.getInt();
                if (length < 0 || length > in.remaining()) throw new Malformed("the attribute " + name + " runs past the end of the file");
                int end = in.position() + length;
                switch (name) {
                    case "RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations" -> {
                        for (int k = u2(); k > 0; k--) out.annotations.add(annotation(0));
                    }
                    case "Signature" -> out.signature = utf(u2());
                    case "InnerClasses" -> {
                        for (int k = u2(); k > 0; k--) out.inner.add(new int[] {u2(), u2(), u2(), u2()});
                    }
                    case "ConstantValue" -> out.constant = constant(u2());
                    case "Code" -> {
                        out.maxStack = u2();
                        out.maxLocals = u2();
                        out.codeLength = in.getInt();
                        out.codeStart = in.position();
                        if (out.codeLength < 0 || out.codeLength > end - out.codeStart) throw new Malformed("a method's code runs past its attribute");
                        in.position(out.codeStart + out.codeLength);
                        for (int k = u2(); k > 0; k--) {
                            int from = u2(), to = u2(), handler = u2(), type = u2();
                            out.handlers.add(new Handler(from, to, handler, type == 0 ? null : className(type)));
                        }
                    }
                    case "BootstrapMethods" -> {
                        for (int k = u2(); k > 0; k--) {
                            int handle = u2();
                            int[] entry = new int[u2() + 1];
                            entry[0] = handle;
                            for (int j = 1; j < entry.length; j++) entry[j] = u2();
                            bootstraps.add(entry);
                        }
                    }
                    default -> {
                    }
                }
                in.position(end);
            }
            return out;
        }

        private Annotation annotation(int depth) {
            if (depth > 16) throw new Malformed("annotations nested too deep");
            String type = utf(u2());
            Map<String, Object> values = new LinkedHashMap<>();
            for (int n = u2(); n > 0; n--) values.put(utf(u2()), elementValue(depth));
            return new Annotation(type, values);
        }

        private Object elementValue(int depth) {
            int kind = u1();
            switch (kind) {
                case 'B', 'S', 'I' -> {
                    int index = u2();
                    expect(index, 3);
                    return value[index];
                }
                case 'Z' -> {
                    int index = u2();
                    expect(index, 3);
                    return (Integer) value[index] != 0;
                }
                case 'C' -> {
                    int index = u2();
                    expect(index, 3);
                    return (char) (int) (Integer) value[index];
                }
                case 'J' -> {
                    int index = u2();
                    expect(index, 5);
                    return value[index];
                }
                case 'F' -> {
                    int index = u2();
                    expect(index, 4);
                    return value[index];
                }
                case 'D' -> {
                    int index = u2();
                    expect(index, 6);
                    return value[index];
                }
                case 's' -> {
                    return utf(u2());
                }
                case 'e' -> {
                    String type = utf(u2());
                    return new EnumConstant(type, utf(u2()));
                }
                case 'c' -> {
                    return new TypeName(utf(u2()));
                }
                case '@' -> {
                    return annotation(depth + 1);
                }
                case '[' -> {
                    List<Object> items = new ArrayList<>();
                    for (int n = u2(); n > 0; n--) items.add(elementValue(depth + 1));
                    return List.copyOf(items);
                }
                default -> throw new Malformed("an annotation value has the unknown kind " + (char) kind);
            }
        }

        // ---- code ----

        /** The steps of one method's code that the analysis looks at. */
        private List<Op> code(int start, int length) {
            List<Op> ops = new ArrayList<>();
            int end = start + length;
            for (int pc = start; pc < end; ) {
                int op = at(pc);
                if (op >= 0x02 && op <= 0x08) ops.add(number(op - 0x03));
                else if (op == 0x09 || op == 0x0a) ops.add(number((long) (op - 0x09)));
                else if (op >= 0x0b && op <= 0x0d) ops.add(number((float) (op - 0x0b)));
                else if (op == 0x0e || op == 0x0f) ops.add(number((double) (op - 0x0e)));
                else if (op == 0x10) ops.add(number((int) in.get(pc + 1)));
                else if (op == 0x11) ops.add(number((int) in.getShort(pc + 1)));
                else if (op == 0x12) constant(at(pc + 1), ops);
                else if (op == 0x13 || op == 0x14) constant(in.getShort(pc + 1) & 0xFFFF, ops);
                else if (op == 0x19) ops.add(new Op(Kind.LOCAL, null, null, null, at(pc + 1)));
                else if (op >= 0x2a && op <= 0x2d) ops.add(new Op(Kind.LOCAL, null, null, null, op - 0x2a));
                else if (op == 0xc4 && at(pc + 1) == 0x19) ops.add(new Op(Kind.LOCAL, null, null, null, in.getShort(pc + 2) & 0xFFFF));
                else if (op >= 0xb2 && op <= 0xb5) {
                    String[] m = member(in.getShort(pc + 1) & 0xFFFF);
                    Kind kind = switch (op) {
                        case 0xb2 -> Kind.GET_STATIC;
                        case 0xb3 -> Kind.PUT_STATIC;
                        case 0xb4 -> Kind.GET_FIELD;
                        default -> Kind.PUT_FIELD;
                    };
                    ops.add(new Op(kind, m[0], m[1], m[2], null));
                } else if (op >= 0xb6 && op <= 0xb9) {
                    String[] m = member(in.getShort(pc + 1) & 0xFFFF);
                    String how = switch (op) {
                        case 0xb6 -> "virtual";
                        case 0xb7 -> "special";
                        case 0xb8 -> "static";
                        default -> "interface";
                    };
                    ops.add(new Op(Kind.CALL, m[0], m[1], m[2], how));
                } else if (op == 0xba) {
                    Op lambda = lambda(in.getShort(pc + 1) & 0xFFFF);
                    if (lambda != null) ops.add(lambda);
                } else if (op == 0xbb) {
                    ops.add(new Op(Kind.NEW, className(in.getShort(pc + 1) & 0xFFFF), null, null, null));
                }
                int size = size(pc, start);
                if (size <= 0 || pc + size > end) throw new Malformed("a method's code doesn't end where it says");
                pc += size;
            }
            return List.copyOf(ops);
        }

        private static Op number(Number n) {
            return new Op(Kind.NUMBER, null, null, null, n);
        }

        /** What `ldc` loads: a number, a text or a class. Other constants aren't looked at. */
        private void constant(int index, List<Op> ops) {
            expect(index, 3, 4, 5, 6, 7, 8, 15, 16, 17);
            switch (tag[index]) {
                case 3, 4, 5, 6 -> ops.add(number((Number) value[index]));
                case 8 -> ops.add(new Op(Kind.STRING, null, null, null, utf(first[index])));
                case 7 -> ops.add(new Op(Kind.TYPE, utf(first[index]), null, null, null));
                default -> {
                }
            }
        }

        /**
         * An invokedynamic made by javac for a lambda or a method reference (its bootstrap method is
         * LambdaMetafactory's): the method it runs and the types it is used with. Null for any other.
         */
        @Override
        public Op lambda(int index) {
            expect(index, 18);
            int nameAndType = second[index];
            expect(nameAndType, 12);
            String site = utf(second[nameAndType]);
            if (first[index] >= bootstraps.size()) throw new Malformed("an invokedynamic names a bootstrap method the class doesn't have");
            int[] bootstrap = bootstraps.get(first[index]);
            expect(bootstrap[0], 15);
            if (!member(second[bootstrap[0]])[0].equals(LAMBDA_FACTORY) || bootstrap.length < 4) return null;
            // Its arguments: the interface method's erased type, the method to run, the interface method's real type.
            if (tag[bootstrap[2]] != 15 || tag[bootstrap[3]] != 16) return null;
            String[] target = member(second[bootstrap[2]]);
            String functionalInterface = internal(returns(site));
            if (functionalInterface == null) return null;
            // Handle kind 8 is REF_newInvokeSpecial: a constructor reference.
            Lambda how = new Lambda(functionalInterface, utf(first[bootstrap[3]]), parameters(site), first[bootstrap[2]] == 8);
            return new Op(Kind.LAMBDA, target[0], target[1], target[2], how);
        }

        @Override
        public Bootstrap bootstrap(int index) {
            expect(index, 18);
            if (first[index] >= bootstraps.size()) throw new Malformed("an invokedynamic names a bootstrap method the class doesn't have");
            int[] bootstrap = bootstraps.get(first[index]);
            expect(bootstrap[0], 15);
            String[] method = member(second[bootstrap[0]]);
            List<Object> arguments = new ArrayList<>();
            for (int k = 1; k < bootstrap.length; k++) {
                int t = tag[bootstrap[k]];
                if (t == 15) {
                    String[] target = member(second[bootstrap[k]]);
                    arguments.add(new Handle(first[bootstrap[k]], target[0], target[1], target[2]));
                } else {
                    arguments.add(t == 3 || t == 4 || t == 5 || t == 6 || t == 7 || t == 8 ? constant(bootstrap[k]) : null);
                }
            }
            return new Bootstrap(method[0], method[1], arguments);
        }

        @Override
        public LambdaSite lambdaSite(int index) {
            expect(index, 18);
            int nameAndType = second[index];
            expect(nameAndType, 12);
            if (first[index] >= bootstraps.size()) throw new Malformed("an invokedynamic names a bootstrap method the class doesn't have");
            int[] bootstrap = bootstraps.get(first[index]);
            expect(bootstrap[0], 15);
            String[] factory = member(second[bootstrap[0]]);
            if (!factory[0].equals(LAMBDA_FACTORY) || !factory[1].equals("metafactory") || bootstrap.length != 4) return null;
            if (tag[bootstrap[1]] != 16 || tag[bootstrap[2]] != 15 || tag[bootstrap[3]] != 16) return null;
            String[] target = member(second[bootstrap[2]]);
            return new LambdaSite(utf(first[nameAndType]), utf(second[nameAndType]), utf(first[bootstrap[1]]), first[bootstrap[2]],
                    target[0], target[1], target[2], utf(first[bootstrap[3]]));
        }

        /** How many bytes the instruction at pc takes; `start` is where its method's code begins. */
        private int size(int pc, int start) {
            int op = at(pc);
            if (op == 0xaa || op == 0xab) {
                // tableswitch and lookupswitch: padded so that their numbers start on a multiple of four.
                int pad = (4 - (pc - start + 1) % 4) % 4;
                int numbers = pc + 1 + pad;
                long entries = op == 0xaa
                        ? 12L + ((long) in.getInt(numbers + 8) - in.getInt(numbers + 4) + 1) * 4
                        : 8L + (long) in.getInt(numbers + 4) * 8;
                if (entries < 8 || entries > Integer.MAX_VALUE / 2) throw new Malformed("a switch in a method's code is too big");
                return 1 + pad + (int) entries;
            }
            if (op == 0xc4) return at(pc + 1) == 0x84 ? 6 : 4;
            if (op == 0xb9 || op == 0xba || op == 0xc8 || op == 0xc9) return 5;
            if (op == 0xc5) return 4;
            if (op == 0x11 || op == 0x13 || op == 0x14 || op == 0x84 || (op >= 0x99 && op <= 0xa8) || (op >= 0xb2 && op <= 0xb8)
                    || op == 0xbb || op == 0xbd || op == 0xc0 || op == 0xc1 || op == 0xc6 || op == 0xc7) return 3;
            if (op == 0x10 || op == 0x12 || (op >= 0x15 && op <= 0x19) || (op >= 0x36 && op <= 0x3a) || op == 0xa9 || op == 0xbc) return 2;
            return 1;
        }

        private int at(int position) {
            return in.get(position) & 0xFF;
        }

        private int u1() {
            return in.get() & 0xFF;
        }

        private int u2() {
            return in.getShort() & 0xFFFF;
        }
    }
}
