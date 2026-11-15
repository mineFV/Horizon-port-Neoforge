package java.lang;

import java.util.Objects;
import java.util.function.Supplier;

/** The model of java.lang.ThreadLocal: each thread's value in its Thread object, made by initialValue at first. */
public class ThreadLocal<T> {
    public ThreadLocal() {}

    protected T initialValue() {
        return null;
    }

    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        Objects.requireNonNull(supplier);
        return new ThreadLocal<S>() {
            @Override
            protected S initialValue() {
                return supplier.get();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public T get() {
        java.util.IdentityHashMap<ThreadLocal<?>, Object> map = Thread.currentThread().jLocals();
        if (map.containsKey(this)) return (T) map.get(this);
        T value = initialValue();
        map.put(this, value);
        return value;
    }

    public void set(T value) {
        Thread.currentThread().jLocals().put(this, value);
    }

    public void remove() {
        Thread.currentThread().jLocals().remove(this);
    }
}
