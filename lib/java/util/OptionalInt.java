package java.util;

/** The model of java.util.OptionalInt. */
public final class OptionalInt {
    private static final OptionalInt EMPTY = new OptionalInt();

    private final boolean isPresent;
    private final int value;

    private OptionalInt() {
        this.isPresent = false;
        this.value = 0;
    }

    public static OptionalInt empty() {
        return EMPTY;
    }

    private OptionalInt(int value) {
        this.isPresent = true;
        this.value = value;
    }

    public static OptionalInt of(int value) {
        return new OptionalInt(value);
    }

    public int getAsInt() {
        if (!isPresent) throw new NoSuchElementException("No value present");
        return value;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public boolean isEmpty() {
        return !isPresent;
    }

    public void ifPresent(java.util.function.IntConsumer action) {
        if (isPresent) action.accept(value);
    }

    public void ifPresentOrElse(java.util.function.IntConsumer action, Runnable emptyAction) {
        if (isPresent) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    public int orElse(int other) {
        return isPresent ? value : other;
    }

    public int orElseGet(java.util.function.IntSupplier supplier) {
        return isPresent ? value : supplier.getAsInt();
    }

    public int orElseThrow() {
        if (!isPresent) throw new NoSuchElementException("No value present");
        return value;
    }

    public <X extends Throwable> int orElseThrow(java.util.function.Supplier<? extends X> exceptionSupplier) throws X {
        if (isPresent) return value;
        throw exceptionSupplier.get();
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof OptionalInt other && (isPresent && other.isPresent ? value == other.value : isPresent == other.isPresent);
    }

    public int hashCode() {
        return isPresent ? Integer.hashCode(value) : 0;
    }

    public String toString() {
        return isPresent ? "OptionalInt[".concat(String.valueOf(value)).concat("]") : "OptionalInt.empty";
    }
}
