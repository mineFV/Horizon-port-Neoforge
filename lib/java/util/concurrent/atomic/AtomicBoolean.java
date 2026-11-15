package java.util.concurrent.atomic;

/** The model of java.util.concurrent.atomic.AtomicBoolean: an int, 0 or 1, as the JDK's; see AtomicInteger. */
public class AtomicBoolean implements java.io.Serializable {
    private volatile int value;

    public AtomicBoolean(boolean initialValue) {
        value = initialValue ? 1 : 0;
    }

    public AtomicBoolean() {}

    public final boolean get() {
        return value != 0;
    }

    public final void set(boolean newValue) {
        value = newValue ? 1 : 0;
    }

    public final void lazySet(boolean newValue) {
        set(newValue);
    }

    public final boolean getPlain() {
        return get();
    }

    public final void setPlain(boolean newValue) {
        set(newValue);
    }

    public final boolean getAcquire() {
        return get();
    }

    public final void setRelease(boolean newValue) {
        set(newValue);
    }

    public final boolean compareAndSet(boolean expectedValue, boolean newValue) {
        return jCompareAndSet(expectedValue ? 1 : 0, newValue ? 1 : 0);
    }

    public final boolean weakCompareAndSetPlain(boolean expectedValue, boolean newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(boolean expectedValue, boolean newValue) {
        return compareAndSet(expectedValue, newValue);
    }

    public final boolean getAndSet(boolean newValue) {
        return jGetAndSet(newValue ? 1 : 0) != 0;
    }

    public String toString() {
        return Boolean.toString(get());
    }

    private native int jGetAndSet(int newValue);

    private native boolean jCompareAndSet(int expectedValue, int newValue);
}
