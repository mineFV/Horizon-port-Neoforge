package java.lang;

/**
 * The model of java.lang.System: the time, identity hashes. Not modelled: arraycopy, the streams (out, err, in),
 * properties, the environment, exit.
 */
public final class System {
    private System() {}

    /** The milliseconds since 1970-01-01T00:00Z, by the machine's clock. */
    public static native long currentTimeMillis();

    /** Nanoseconds from an origin of the run's own (the JDK's is arbitrary too). */
    public static native long nanoTime();

    public static int identityHashCode(Object x) {
        return x == null ? 0 : jIdentity(x);
    }

    /** Object.hashCode's identity, whatever the class's own hashCode is. */
    private static native int jIdentity(Object x);
}
