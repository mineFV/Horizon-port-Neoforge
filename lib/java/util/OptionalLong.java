package java.util;

/** The model of java.util.OptionalLong. */
public final class OptionalLong {
    private static final OptionalLong EMPTY = new OptionalLong();

    private final boolean isPresent;
    private final long value;

    private OptionalLong() {
        this.isPresent = false;
        this.value = 0;
    }

    public static OptionalLong empty() {
        return EMPTY;
    }

    private OptionalLong(long value) {
        this.isPresent = true;
        this.value = value;
    }

    public static OptionalLong of(long value) {
        return new OptionalLong(value);
    }

    public long getAsLong() {
        if (!isPresent) throw new NoSuchElementException("No value present");
        return value;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public boolean isEmpty() {
        return !isPresent;
    }

    public void ifPresent(java.util.function.LongConsumer action) {
        if (isPresent) action.accept(value);
    }

    public void ifPresentOrElse(java.util.function.LongConsumer action, Runnable emptyAction) {
        if (isPresent) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    public long orElse(long other) {
        return isPresent ? value : other;
    }

    public long orElseGet(java.util.function.LongSupplier supplier) {
        return isPresent ? value : supplier.getAsLong();
    }

    public long orElseThrow() {
        if (!isPresent) throw new NoSuchElementException("No value present");
        return value;
    }

    public <X extends Throwable> long orElseThrow(java.util.function.Supplier<? extends X> exceptionSupplier) throws X {
        if (isPresent) return value;
        throw exceptionSupplier.get();
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof OptionalLong other && (isPresent && other.isPresent ? value == other.value : isPresent == other.isPresent);
    }

    public int hashCode() {
        return isPresent ? Long.hashCode(value) : 0;
    }

    public String toString() {
        return isPresent ? "OptionalLong[".concat(String.valueOf(value)).concat("]") : "OptionalLong.empty";
    }
}
