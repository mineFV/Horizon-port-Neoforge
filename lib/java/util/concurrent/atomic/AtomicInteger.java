package java.util.concurrent.atomic;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * The model of java.util.concurrent.atomic.AtomicInteger: its value is the object's first slot, read and written under
 * the object's lock; the read-modify-write natives hold it throughout, as the JDK's are atomic. updateAndGet and its
 * kin retry as the JDK's do (the function may be called again when another thread changed the value).
 */
public class AtomicInteger extends Number implements java.io.Serializable {
    private volatile int value;

    public AtomicInteger(int initialValue) {
        value = initialValue;
    }

    public AtomicInteger() {}

    public final int get() {
        return value;
    }

    public final void set(int newValue) {
        value = newValue;
    }

    public final void lazySet(int newValue) {
        value = newValue;
    }

    public final int getPlain() {
        return value;
    }

    public final void setPlain(int newValue) {
        value = newValue;
    }

    public final int getAcquire() {
        return value;
    }

    public final void setRelease(int newValue) {
        value = newValue;
    }

    public final int getAndSet(int newValue) {
        return jGetAndSet(newValue);
    }

    public final boolean compareAndSet(int expectedValue, int newValue) {
        return jCompareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetPlain(int expectedValue, int newValue) {
        return jCompareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(int expectedValue, int newValue) {
        return jCompareAndSet(expectedValue, newValue);
    }

    public final int getAndIncrement() {
        return jGetAndAdd(1);
    }

    public final int getAndDecrement() {
        return jGetAndAdd(-1);
    }

    public final int getAndAdd(int delta) {
        return jGetAndAdd(delta);
    }

    public final int incrementAndGet() {
        return jGetAndAdd(1) + 1;
    }

    public final int decrementAndGet() {
        return jGetAndAdd(-1) - 1;
    }

    public final int addAndGet(int delta) {
        return jGetAndAdd(delta) + delta;
    }

    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = updateFunction.applyAsInt(prev);
            if (jCompareAndSet(prev, next)) return prev;
            haveNext = (prev == (prev = get()));
        }
    }

    public final int updateAndGet(IntUnaryOperator updateFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = updateFunction.applyAsInt(prev);
            if (jCompareAndSet(prev, next)) return next;
            haveNext = (prev == (prev = get()));
        }
    }

    public final int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = accumulatorFunction.applyAsInt(prev, x);
            if (jCompareAndSet(prev, next)) return prev;
            haveNext = (prev == (prev = get()));
        }
    }

    public final int accumulateAndGet(int x, IntBinaryOperator accumulatorFunction) {
        int prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = accumulatorFunction.applyAsInt(prev, x);
            if (jCompareAndSet(prev, next)) return next;
            haveNext = (prev == (prev = get()));
        }
    }

    public String toString() {
        return Integer.toString(get());
    }

    public int intValue() {
        return (int) get();
    }

    public long longValue() {
        return (long) get();
    }

    public float floatValue() {
        return (float) get();
    }

    public double doubleValue() {
        return (double) get();
    }

    private native int jGetAndSet(int newValue);

    private native boolean jCompareAndSet(int expectedValue, int newValue);

    private native int jGetAndAdd(int delta);
}
