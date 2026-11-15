package java.util.stream;

import java.util.function.Consumer;

/**
 * What a stream's stage pushes its elements into, as the JDK's sinks do: begin with the size if known (-1 if not),
 * each element, end; a short-circuiting operation asks between elements whether to go on.
 */
interface Sink extends Consumer<Object> {
    default void begin(long size) {}

    default void end() {}

    default boolean cancellationRequested() {
        return false;
    }

    /** A stage's sink: what it doesn't change, it passes on. */
    abstract class Chained implements Sink {
        final Sink downstream;

        Chained(Sink downstream) {
            this.downstream = java.util.Objects.requireNonNull(downstream);
        }

        public void begin(long size) {
            downstream.begin(size);
        }

        public void end() {
            downstream.end();
        }

        public boolean cancellationRequested() {
            return downstream.cancellationRequested();
        }
    }
}
