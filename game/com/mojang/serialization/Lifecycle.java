package com.mojang.serialization;

/** The stand-in for DataFixerUpper's Lifecycle: stable, experimental or deprecated since a version (observed, `standins`). */
public class Lifecycle {
    private static final Lifecycle STABLE = new Lifecycle("Stable");
    private static final Lifecycle EXPERIMENTAL = new Lifecycle("Experimental");

    private final String text;

    private Lifecycle(String text) {
        this.text = text;
    }

    public static Lifecycle experimental() {
        return EXPERIMENTAL;
    }

    public static Lifecycle stable() {
        return STABLE;
    }

    public static Lifecycle deprecated(int since) {
        return new Deprecated(since);
    }

    /** Experimental wins, then the deprecated one of the older version, then stable. */
    public Lifecycle add(Lifecycle other) {
        if (this == EXPERIMENTAL || other == EXPERIMENTAL) return EXPERIMENTAL;
        if (this instanceof Deprecated d) {
            if (other instanceof Deprecated o && o.since < d.since) return other;
            return this;
        }
        if (other instanceof Deprecated) return other;
        return STABLE;
    }

    @Override
    public String toString() {
        return text;
    }

    public static final class Deprecated extends Lifecycle {
        private final int since;

        public Deprecated(int since) {
            super("Deprecated");
            this.since = since;
        }

        public int since() {
            return since;
        }

        /** Observed: Object's (its class, "@" and its identity hash). */
        @Override
        public String toString() {
            return "com.mojang.serialization.Lifecycle$Deprecated@" + Integer.toHexString(System.identityHashCode(this));
        }
    }
}
