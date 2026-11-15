package java.util.concurrent.atomic;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/** The model of java.util.concurrent.atomic.AtomicReference: compared by identity, as the JDK's; see AtomicInteger. */
public class AtomicReference<V> implements java.io.Serializable {
    private volatile V value;

    public AtomicReference(V initialValue) {
        value = initialValue;
    }

    public AtomicReference() {}

    public final V get() {
        return value;
    }

    public final void set(V newValue) {
        value = newValue;
    }

    public final void lazySet(V newValue) {
        value = newValue;
    }

    public final V getPlain() {
        return value;
    }

    public final void setPlain(V newValue) {
        value = newValue;
    }

    public final V getAcquire() {
        return value;
    }

    public final void setRelease(V newValue) {
        value = newValue;
    }

    public final boolean compareAndSet(V expectedValue, V newValue) {
        return jCompareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetPlain(V expectedValue, V newValue) {
        return jCompareAndSet(expectedValue, newValue);
    }

    public final boolean weakCompareAndSetVolatile(V expectedValue, V newValue) {
        return jCompareAndSet(expectedValue, newValue);
    }

    @SuppressWarnings("unchecked")
    public final V getAndSet(V newValue) {
        return (V) jGetAndSet(newValue);
    }

    public final V getAndUpdate(UnaryOperator<V> updateFunction) {
        V prev = get(), next = null;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = updateFunction.apply(prev);
            if (jCompareAndSet(prev, next)) return prev;
            haveNext = (prev == (prev = get()));
        }
    }

    public final V updateAndGet(UnaryOperator<V> updateFunction) {
        V prev = get(), next = null;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = updateFunction.apply(prev);
            if (jCompareAndSet(prev, next)) return next;
            haveNext = (prev == (prev = get()));
        }
    }

    public final V getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction) {
        V prev = get(), next = null;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = accumulatorFunction.apply(prev, x);
            if (jCompareAndSet(prev, next)) return prev;
            haveNext = (prev == (prev = get()));
        }
    }

    public final V accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction) {
        V prev = get(), next = null;
        for (boolean haveNext = false;;) {
            if (!haveNext) next = accumulatorFunction.apply(prev, x);
            if (jCompareAndSet(prev, next)) return next;
            haveNext = (prev == (prev = get()));
        }
    }

    public String toString() {
        return String.valueOf(get());
    }

    private native Object jGetAndSet(Object newValue);

    private native boolean jCompareAndSet(Object expectedValue, Object newValue);
}
