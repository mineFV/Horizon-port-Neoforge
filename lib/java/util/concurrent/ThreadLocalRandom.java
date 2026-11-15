package java.util.concurrent;

import java.util.Random;

/**
 * The model of java.util.concurrent.ThreadLocalRandom: a Random of each thread's, seeded as new Random() is (the
 * JDK's own generator is another; both are random). setSeed is an UnsupportedOperationException, as the JDK's.
 */
public class ThreadLocalRandom extends Random {
    private static final ThreadLocal<ThreadLocalRandom> LOCAL = ThreadLocal.withInitial(ThreadLocalRandom::new);

    private final boolean initialized;

    private ThreadLocalRandom() {
        initialized = true;
    }

    public static ThreadLocalRandom current() {
        return LOCAL.get();
    }

    public void setSeed(long seed) {
        if (initialized) throw new UnsupportedOperationException();
        super.setSeed(seed);
    }

    public int nextInt(int origin, int bound) {
        if (origin >= bound) throw new IllegalArgumentException("bound must be greater than origin");
        int n = bound - origin;
        if (n > 0) return nextInt(n) + origin;
        int r;
        do {
            r = nextInt();
        } while (r < origin || r >= bound);
        return r;
    }
}
