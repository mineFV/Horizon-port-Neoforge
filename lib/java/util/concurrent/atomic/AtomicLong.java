package java.util.concurrent.atomic;

import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * The model of java.util.concurrent.atomic.AtomicLong: its value is the object's first slot, read and written under
 * the object's lock; the read-modify-write natives hold it throughout, as the JDK's are atomic. updateAndGet and its
 * kin retry as the JDK's do (the function may be called again when another thread changed the value).
 */
public class AtomicLong extends Number implements java.io.Serializable {
    private volatile long value;

    public AtomicLong(long initialValue) {
        value = initialValue;
    }

    public AtomicLong() {}

    public final long get() {
        return value;
    }

    public final void set(long newValue) {
        value = newValue;
    }

    public final void lazySet(long newValue) {
        value = newValue;
    }

    public final long getPlain() {
        return value;
    }

    public final void setPlain(long newValue) {
        value = newValue;
    }

    public final long getAcquire() {
        return value;
    }

    public final void setRelease(long newValue) {
        value = newValue;
    }

    public final long getAndSet(long newValue) {
        return jGetAndSet(newValue);
    }

    public final boolean compareAndSet(long expectedValue, long newValue) {
        return jCompareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetPlain(long expectedValue, long newValue) {
        return jCompareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(long expectedValue, long newValue) {
        return jCompareAndSet(expectedValue, newValue);
    }

    public final long getAndIncrement() {
        return jGetAndAdd(1);
    }

    public final long getAndDecrement() {
        return jGetAndAdd(-1);
    }

    public final long getAndAdd(long delta) {
        return jGetAndAdd(delta);
    }

    public final long incrementAndGet() {
        return jGetAndAdd(1) + 1;
    }

    public final long decrementAndGet() {
        return jGetAndAdd(-1) - 1;
    }

    public final long addAndGet(long delta) {
        return jGetAndAdd(delta) + delta;
    }

    public final long getAndUpdate(LongUnaryOperator updateFunction) {
        long prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = updateFunction.applyAsLong(prev);
            if (jCompareAndSet(prev, next)) return prev;
            haveNext = (prev == (prev = get()));
        }
    }

    public final long updateAndGet(LongUnaryOperator updateFunction) {
        long prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = updateFunction.applyAsLong(prev);
            if (jCompareAndSet(prev, next)) return next;
            haveNext = (prev == (prev = get()));
        }
    }

    public final long getAndAccumulate(long x, LongBinaryOperator accumulatorFunction) {
        long prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = accumulatorFunction.applyAsLong(prev, x);
            if (jCompareAndSet(prev, next)) return prev;
            haveNext = (prev == (prev = get()));
        }
    }

    public final long accumulateAndGet(long x, LongBinaryOperator accumulatorFunction) {
        long prev = get(), next = 0;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = accumulatorFunction.applyAsLong(prev, x);
            if (jCompareAndSet(prev, next)) return next;
            haveNext = (prev == (prev = get()));
        }
    }

    public String toString() {
        return Long.toString(get());
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

    private native long jGetAndSet(long newValue);

    private native boolean jCompareAndSet(long expectedValue, long newValue);

    private native long jGetAndAdd(long delta);
}
