package java.util.stream;

import java.util.Objects;
import java.util.Spliterator;

/** The model of java.util.stream.StreamSupport: a stream over a spliterator, whose characteristics it reads now. */
public final class StreamSupport {
    private StreamSupport() {}

    public static <T> Stream<T> stream(Spliterator<T> spliterator, boolean parallel) {
        Objects.requireNonNull(spliterator);
        return new ReferencePipeline<>(spliterator, parallel);
    }
}
