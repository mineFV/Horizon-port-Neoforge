package com.mojang.serialization;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.K1;

/**
 * The stand-in for DataFixerUpper's DataResult: a value (Success) or an error with a message and maybe a partial value
 * (Error), each with a lifecycle (observed, `standins`).
 */
public interface DataResult<R> extends App<DataResult.Mu, R> {
    final class Mu implements K1 {
        private Mu() {}
    }

    @SuppressWarnings("unchecked")
    static <R> DataResult<R> unbox(App<Mu, R> box) {
        return (DataResult<R>) box;
    }

    static <R> DataResult<R> success(R result) {
        return new Success<>(result, Lifecycle.experimental());
    }

    static <R> DataResult<R> success(R result, Lifecycle lifecycle) {
        return new Success<>(result, lifecycle);
    }

    static <R> DataResult<R> error(Supplier<String> message, R partial) {
        return new Error<>(message, Optional.of(partial), Lifecycle.experimental());
    }

    static <R> DataResult<R> error(Supplier<String> message) {
        return new Error<>(message, Optional.empty(), Lifecycle.experimental());
    }

    static <R> DataResult<R> error(Supplier<String> message, R partial, Lifecycle lifecycle) {
        return new Error<>(message, Optional.of(partial), lifecycle);
    }

    static <R> DataResult<R> error(Supplier<String> message, Lifecycle lifecycle) {
        return new Error<>(message, Optional.empty(), lifecycle);
    }

    static String appendMessages(String first, String second) {
        return first + "; " + second;
    }

    Optional<R> result();

    Optional<Error<R>> error();

    Lifecycle lifecycle();

    boolean hasResultOrPartial();

    Optional<R> resultOrPartial(Consumer<String> onError);

    Optional<R> resultOrPartial();

    <E extends Throwable> R getOrThrow(Function<String, E> exception) throws E;

    <E extends Throwable> R getPartialOrThrow(Function<String, E> exception) throws E;

    default R getOrThrow() {
        return getOrThrow(IllegalStateException::new);
    }

    default R getPartialOrThrow() {
        return getPartialOrThrow(IllegalStateException::new);
    }

    <T> T mapOrElse(Function<? super R, ? extends T> success, Function<? super Error<R>, ? extends T> error);

    DataResult<R> ifSuccess(Consumer<? super R> action);

    DataResult<R> ifError(Consumer<? super Error<R>> action);

    DataResult<R> promotePartial(Consumer<String> onError);

    DataResult<R> setPartial(Supplier<R> partial);

    DataResult<R> setPartial(R partial);

    DataResult<R> mapError(UnaryOperator<String> change);

    DataResult<R> setLifecycle(Lifecycle lifecycle);

    default DataResult<R> addLifecycle(Lifecycle lifecycle) {
        return setLifecycle(lifecycle().add(lifecycle));
    }

    boolean isSuccess();

    default boolean isError() {
        return !isSuccess();
    }

    default <T> DataResult<T> map(Function<? super R, ? extends T> function) {
        if (this instanceof Success<R> s) return new Success<>(function.apply(s.value()), s.lifecycle());
        Error<R> e = (Error<R>) this;
        return new Error<>(e.messageSupplier(), e.partialValue().map(function), e.lifecycle());
    }

    default <R2> DataResult<R2> flatMap(Function<? super R, ? extends DataResult<R2>> function) {
        if (this instanceof Success<R> s) {
            DataResult<R2> next = function.apply(s.value());
            return next.setLifecycle(s.lifecycle().add(next.lifecycle()));
        }
        Error<R> e = (Error<R>) this;
        if (e.partialValue().isEmpty()) return new Error<>(e.messageSupplier(), Optional.empty(), e.lifecycle());
        DataResult<R2> next = function.apply(e.partialValue().get());
        Lifecycle lifecycle = e.lifecycle().add(next.lifecycle());
        if (next instanceof Error<R2> n) {
            return new Error<>(() -> appendMessages(e.message(), n.message()), n.partialValue(), lifecycle);
        }
        return new Error<>(e.messageSupplier(), next.result(), lifecycle);
    }

    /** Observed (`standins`): as apply2stable, the function's lifecycle experimental. */
    default <R2, S> DataResult<S> apply2(java.util.function.BiFunction<R, R2, S> function, DataResult<R2> second) {
        return horizonAp2(Lifecycle.experimental(), function, this, second);
    }

    /**
     * Observed: both results' values given to the function, the lifecycles added (the function's stable); an error's
     * messages joined, the second's first, its partial value the function's of both partial values when both have one.
     */
    default <R2, S> DataResult<S> apply2stable(java.util.function.BiFunction<R, R2, S> function, DataResult<R2> second) {
        return horizonAp2(Lifecycle.stable(), function, this, second);
    }

    /** apply2's and apply2stable's. Not DataFixerUpper's API. */
    static <A, B, S> DataResult<S> horizonAp2(Lifecycle lifecycle, java.util.function.BiFunction<A, B, S> function, DataResult<A> a, DataResult<B> b) {
        Lifecycle all = lifecycle.add(a.lifecycle()).add(b.lifecycle());
        if (a instanceof Success<A> sa && b instanceof Success<B> sb) return new Success<>(function.apply(sa.value(), sb.value()), all);
        Optional<A> pa = a instanceof Success<A> sa ? Optional.of(sa.value()) : ((Error<A>) a).partialValue();
        Optional<B> pb = b instanceof Success<B> sb ? Optional.of(sb.value()) : ((Error<B>) b).partialValue();
        Optional<S> partial = pa.isPresent() && pb.isPresent() ? Optional.of(function.apply(pa.get(), pb.get())) : Optional.empty();
        Supplier<String> message;
        if (a instanceof Error<A> ea && b instanceof Error<B> eb) message = () -> appendMessages(eb.message(), ea.message());
        else if (a instanceof Error<A> ea) message = ea.messageSupplier();
        else message = ((Error<B>) b).messageSupplier();
        return new Error<>(message, partial, all);
    }

    record Success<R>(R value, Lifecycle lifecycle) implements DataResult<R> {
        @Override
        public Optional<R> result() {
            return Optional.of(value);
        }

        @Override
        public Optional<Error<R>> error() {
            return Optional.empty();
        }

        @Override
        public boolean hasResultOrPartial() {
            return true;
        }

        @Override
        public Optional<R> resultOrPartial(Consumer<String> onError) {
            return Optional.of(value);
        }

        @Override
        public Optional<R> resultOrPartial() {
            return Optional.of(value);
        }

        @Override
        public <E extends Throwable> R getOrThrow(Function<String, E> exception) {
            return value;
        }

        @Override
        public <E extends Throwable> R getPartialOrThrow(Function<String, E> exception) {
            return value;
        }

        @Override
        public <T> T mapOrElse(Function<? super R, ? extends T> success, Function<? super Error<R>, ? extends T> error) {
            return success.apply(value);
        }

        @Override
        public DataResult<R> ifSuccess(Consumer<? super R> action) {
            action.accept(value);
            return this;
        }

        @Override
        public DataResult<R> ifError(Consumer<? super Error<R>> action) {
            return this;
        }

        @Override
        public DataResult<R> promotePartial(Consumer<String> onError) {
            return this;
        }

        @Override
        public DataResult<R> setPartial(Supplier<R> partial) {
            return this;
        }

        @Override
        public DataResult<R> setPartial(R partial) {
            return this;
        }

        @Override
        public DataResult<R> mapError(UnaryOperator<String> change) {
            return this;
        }

        @Override
        public DataResult<R> setLifecycle(Lifecycle lifecycle) {
            return this.lifecycle == lifecycle ? this : new Success<>(value, lifecycle);
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public String toString() {
            return "DataResult.Success[" + value + "]";
        }
    }

    record Error<R>(Supplier<String> messageSupplier, Optional<R> partialValue, Lifecycle lifecycle) implements DataResult<R> {
        public String message() {
            return messageSupplier.get();
        }

        @Override
        public Optional<R> result() {
            return Optional.empty();
        }

        @Override
        public Optional<Error<R>> error() {
            return Optional.of(this);
        }

        @Override
        public boolean hasResultOrPartial() {
            return partialValue.isPresent();
        }

        @Override
        public Optional<R> resultOrPartial(Consumer<String> onError) {
            onError.accept(message());
            return partialValue;
        }

        @Override
        public Optional<R> resultOrPartial() {
            return partialValue;
        }

        @Override
        public <E extends Throwable> R getOrThrow(Function<String, E> exception) throws E {
            throw exception.apply(message());
        }

        @Override
        public <E extends Throwable> R getPartialOrThrow(Function<String, E> exception) throws E {
            if (partialValue.isPresent()) return partialValue.get();
            throw exception.apply(message());
        }

        @Override
        public <T> T mapOrElse(Function<? super R, ? extends T> success, Function<? super Error<R>, ? extends T> error) {
            return error.apply(this);
        }

        @Override
        public DataResult<R> ifSuccess(Consumer<? super R> action) {
            return this;
        }

        @Override
        public DataResult<R> ifError(Consumer<? super Error<R>> action) {
            action.accept(this);
            return this;
        }

        @Override
        public DataResult<R> promotePartial(Consumer<String> onError) {
            onError.accept(message());
            return partialValue.<DataResult<R>>map(v -> new Success<>(v, lifecycle)).orElse(this);
        }

        @Override
        public Error<R> setPartial(Supplier<R> partial) {
            return new Error<>(messageSupplier, Optional.of(partial.get()), lifecycle);
        }

        @Override
        public Error<R> setPartial(R partial) {
            return new Error<>(messageSupplier, Optional.of(partial), lifecycle);
        }

        @Override
        public Error<R> mapError(UnaryOperator<String> change) {
            return new Error<>(() -> change.apply(message()), partialValue, lifecycle);
        }

        @Override
        public Error<R> setLifecycle(Lifecycle lifecycle) {
            return new Error<>(messageSupplier, partialValue, lifecycle);
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public String toString() {
            return "DataResult.Error['" + message() + "'" + partialValue.map(v -> ": " + v).orElse("") + "]";
        }
    }
}
