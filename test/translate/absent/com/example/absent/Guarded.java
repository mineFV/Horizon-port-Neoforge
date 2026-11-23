package com.example.absent;

import com.othermod.Gadget;

/**
 * Names another mod's class that isn't there when it runs (com.othermod.Gadget: compiled against, left out): each
 * instruction naming it throws when it runs, and only then, as the JVM resolves it (observed by the probe's
 * AbsentProbe, in a server too); a cast or an instanceof of null resolves nothing.
 */
public final class Guarded {
    private Guarded() {}

    /** Whether the other mod is loaded: it isn't. */
    static boolean loaded() {
        return false;
    }

    static Object nothing() {
        return null;
    }

    static Object something() {
        return "text";
    }

    static Object call() {
        return Gadget.value();
    }

    static Object make() {
        return new Gadget();
    }

    static Object read() {
        return Gadget.count;
    }

    static Object write() {
        Gadget.count = 3;
        return "written";
    }

    static Object field() {
        return ((Gadget) nothing()).size;
    }

    static Object array() {
        return new Gadget[2];
    }

    static Object arrays() {
        return new Gadget[2][3];
    }

    static Object constant() {
        return Gadget.class;
    }

    static Object arrayConstant() {
        return Gadget[].class;
    }

    static Object castNull() {
        return (Gadget) nothing();
    }

    static Object testNull() {
        return nothing() instanceof Gadget;
    }

    static Object cast() {
        return (Gadget) something();
    }

    static Object test() {
        return something() instanceof Gadget;
    }

    static Object guarded() {
        return loaded() ? Gadget.value() : "not run";
    }

    static Object caught() {
        try {
            return Gadget.value();
        } catch (NoClassDefFoundError e) {
            return "caught " + e.getMessage();
        }
    }

    static String show(int k) {
        try {
            Object v = switch (k) {
                case 0 -> call();
                case 1 -> make();
                case 2 -> read();
                case 3 -> write();
                case 4 -> field();
                case 5 -> array();
                case 6 -> arrays();
                case 7 -> constant();
                case 8 -> arrayConstant();
                case 9 -> castNull();
                case 10 -> testNull();
                case 11 -> cast();
                case 12 -> test();
                case 13 -> guarded();
                default -> caught();
            };
            return "gives " + v;
        } catch (Throwable e) {
            Throwable c = e.getCause();
            return e.getClass().getName() + ": " + e.getMessage() + " | cause "
                    + (c == null ? "null" : c.getClass().getName() + ": " + c.getMessage() + " | its cause " + c.getCause());
        }
    }

    /** Each way of naming the class, and what it gives or throws, a line each (twice the call: the second throws alike). */
    public static String all() {
        String[] names = {"call", "make", "read", "write", "field", "array", "arrays", "constant", "arrayConstant", "castNull", "testNull", "cast", "test", "guarded", "caught"};
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < names.length; k++) b.append(names[k]).append(" -> ").append(show(k)).append('\n');
        b.append("call again -> ").append(show(0)).append('\n');
        return b.toString();
    }
}
