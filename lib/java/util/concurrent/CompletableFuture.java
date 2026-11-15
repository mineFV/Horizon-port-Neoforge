package java.util.concurrent;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The model of java.util.concurrent.CompletableFuture, run as the JDK's runs it: a stage's dependents wait on its
 * stack, the last added first; when it completes, those of a dependent that completes in turn are handled before
 * the stage's remaining ones, as the JDK's postComplete does. An exception a function throws completes the
 * dependent with a CompletionException around it; join throws that (or a CompletionException around the stage's
 * own exception), get an ExecutionException around the cause. The functions run outside any lock. Async tasks run
 * each on a thread of their own (as the JDK does when its common pool has no parallelism). Not modelled: the Async
 * stages, orTimeout and its kin, obtrude, anyOf's and the either stages, minimalCompletionStage.
 */
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    /** Guards every future's result and stack; waiters wait on it. */
    static final Object LOCK = new Object();

    static final class AltResult {
        final Throwable ex;

        AltResult(Throwable x) {
            this.ex = x;
        }
    }

    static final AltResult NIL = new AltResult(null);

    volatile Object result;
    Completion stack;

    public CompletableFuture() {}

    CompletableFuture(Object r) {
        this.result = r;
    }

    // ---- completing ----

    final boolean internalComplete(Object r) {
        synchronized (LOCK) {
            if (result != null) return false;
            result = r;
            LOCK.notifyAll();
            return true;
        }
    }

    final boolean completeNull() {
        return internalComplete(NIL);
    }

    final Object encodeValue(T t) {
        return (t == null) ? NIL : t;
    }

    final boolean completeValue(T t) {
        return internalComplete((t == null) ? NIL : t);
    }

    static AltResult encodeThrowable(Throwable x) {
        return new AltResult((x instanceof CompletionException) ? x : new CompletionException(x));
    }

    final boolean completeThrowable(Throwable x) {
        return internalComplete(encodeThrowable(x));
    }

    /** x, as the result of a dependent of a stage whose result was r: the same result when x is r's already. */
    static Object encodeThrowable(Throwable x, Object r) {
        if (!(x instanceof CompletionException)) x = new CompletionException(x);
        else if (r instanceof AltResult && x == ((AltResult) r).ex) return r;
        return new AltResult(x);
    }

    final boolean completeThrowable(Throwable x, Object r) {
        return internalComplete(encodeThrowable(x, r));
    }

    static Object encodeRelay(Object r) {
        Throwable x;
        if (r instanceof AltResult && (x = ((AltResult) r).ex) != null && !(x instanceof CompletionException)) r = new AltResult(new CompletionException(x));
        return r;
    }

    final boolean completeRelay(Object r) {
        return internalComplete(encodeRelay(r));
    }

    // ---- the stack of dependents ----

    static final int SYNC = 0;
    static final int NESTED = -1;

    abstract static class Completion {
        Completion next;

        /** Runs it if its stage(s) are complete; the dependent to go on with (NESTED), when it has dependents. */
        abstract CompletableFuture<?> tryFire(int mode);
    }

    final void pushStack(Completion c) {
        synchronized (LOCK) {
            c.next = stack;
            stack = c;
        }
    }

    /** Pushes c unless complete; fires it at once if complete by then. */
    final void unipush(Completion c) {
        if (c != null) {
            synchronized (LOCK) {
                if (result == null) {
                    c.next = stack;
                    stack = c;
                }
            }
            if (result != null) c.tryFire(SYNC);
        }
    }

    /**
     * Pops and fires the dependents; a dependent that completes and has dependents of its own has them moved here,
     * one at a time, the last but one first (as the JDK's).
     */
    final void postComplete() {
        CompletableFuture<?> f = this;
        Completion h;
        while (true) {
            synchronized (LOCK) {
                h = f.stack;
                if (h == null) {
                    if (f == this) return;
                    f = this;
                    h = f.stack;
                    if (h == null) return;
                }
                Completion t = h.next;
                f.stack = t;
                if (t != null) {
                    if (f != this) {
                        h.next = stack;
                        stack = h;
                        continue;
                    }
                    h.next = null;
                }
            }
            CompletableFuture<?> d = h.tryFire(NESTED);
            f = (d == null) ? this : d;
        }
    }

    /** After a dependent of a fired: a's dependents too (unless nested), then this one's (or this, to go on with). */
    final CompletableFuture<T> postFire(CompletableFuture<?> a, int mode) {
        if (a != null && a.stack != null && mode >= 0 && a.result != null) a.postComplete();
        if (result != null && stack != null) {
            if (mode < 0) return this;
            postComplete();
        }
        return null;
    }

    abstract static class UniCompletion<T, V> extends Completion {
        CompletableFuture<V> dep;
        CompletableFuture<T> src;
        private boolean claimed;

        UniCompletion(CompletableFuture<V> dep, CompletableFuture<T> src) {
            this.dep = dep;
            this.src = src;
        }

        /** Once: the stage's completer and the one that pushed it may both try. */
        final boolean claim() {
            synchronized (LOCK) {
                if (claimed) return false;
                claimed = true;
                return true;
            }
        }
    }

    static final class UniApply<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends V> fn;

        UniApply(CompletableFuture<V> dep, CompletableFuture<T> src, Function<? super T, ? extends V> fn) {
            super(dep, src);
            this.fn = fn;
        }

        @SuppressWarnings("unchecked")
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            Object r;
            Throwable x;
            Function<? super T, ? extends V> f;
            if ((a = src) == null || (r = a.result) == null || (d = dep) == null || (f = fn) == null) return null;
            tryComplete:
            if (d.result == null) {
                if (r instanceof AltResult) {
                    if ((x = ((AltResult) r).ex) != null) {
                        d.completeThrowable(x, r);
                        break tryComplete;
                    }
                    r = null;
                }
                try {
                    if (!claim()) return null;
                    d.completeValue(f.apply((T) r));
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    static final class UniAccept<T> extends UniCompletion<T, Void> {
        Consumer<? super T> fn;

        UniAccept(CompletableFuture<Void> dep, CompletableFuture<T> src, Consumer<? super T> fn) {
            super(dep, src);
            this.fn = fn;
        }

        @SuppressWarnings("unchecked")
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            Object r;
            Throwable x;
            Consumer<? super T> f;
            if ((a = src) == null || (r = a.result) == null || (d = dep) == null || (f = fn) == null) return null;
            tryComplete:
            if (d.result == null) {
                if (r instanceof AltResult) {
                    if ((x = ((AltResult) r).ex) != null) {
                        d.completeThrowable(x, r);
                        break tryComplete;
                    }
                    r = null;
                }
                try {
                    if (!claim()) return null;
                    f.accept((T) r);
                    d.completeNull();
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    static final class UniRun<T> extends UniCompletion<T, Void> {
        Runnable fn;

        UniRun(CompletableFuture<Void> dep, CompletableFuture<T> src, Runnable fn) {
            super(dep, src);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            Object r;
            Throwable x;
            Runnable f;
            if ((a = src) == null || (r = a.result) == null || (d = dep) == null || (f = fn) == null) return null;
            if (d.result == null) {
                if (r instanceof AltResult && (x = ((AltResult) r).ex) != null) {
                    d.completeThrowable(x, r);
                } else {
                    try {
                        if (!claim()) return null;
                        f.run();
                        d.completeNull();
                    } catch (Throwable ex) {
                        d.completeThrowable(ex);
                    }
                }
            }
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    static final class UniWhenComplete<T> extends UniCompletion<T, T> {
        BiConsumer<? super T, ? super Throwable> fn;

        UniWhenComplete(CompletableFuture<T> dep, CompletableFuture<T> src, BiConsumer<? super T, ? super Throwable> fn) {
            super(dep, src);
            this.fn = fn;
        }

        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> d;
            CompletableFuture<T> a;
            Object r;
            BiConsumer<? super T, ? super Throwable> f;
            if ((a = src) == null || (r = a.result) == null || (d = dep) == null || (f = fn) == null || !d.uniWhenComplete(r, f, this)) return null;
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    @SuppressWarnings("unchecked")
    final boolean uniWhenComplete(Object r, BiConsumer<? super T, ? super Throwable> f, UniWhenComplete<T> c) {
        T t;
        Throwable x = null;
        if (result == null) {
            try {
                if (c != null && !c.claim()) return false;
                if (r instanceof AltResult) {
                    x = ((AltResult) r).ex;
                    t = null;
                } else {
                    t = (T) r;
                }
                f.accept(t, x);
                if (x == null) {
                    internalComplete(r);
                    return true;
                }
            } catch (Throwable ex) {
                if (x == null) x = ex;
                else if (x != ex) x.addSuppressed(ex);
            }
            completeThrowable(x, r);
        }
        return true;
    }

    static final class UniHandle<T, V> extends UniCompletion<T, V> {
        BiFunction<? super T, Throwable, ? extends V> fn;

        UniHandle(CompletableFuture<V> dep, CompletableFuture<T> src, BiFunction<? super T, Throwable, ? extends V> fn) {
            super(dep, src);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            Object r;
            BiFunction<? super T, Throwable, ? extends V> f;
            if ((a = src) == null || (r = a.result) == null || (d = dep) == null || (f = fn) == null || !d.uniHandle(r, f, this)) return null;
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    @SuppressWarnings("unchecked")
    final <S> boolean uniHandle(Object r, BiFunction<? super S, Throwable, ? extends T> f, UniCompletion<S, T> c) {
        S s;
        Throwable x;
        if (result == null) {
            try {
                if (c != null && !c.claim()) return false;
                if (r instanceof AltResult) {
                    x = ((AltResult) r).ex;
                    s = null;
                } else {
                    x = null;
                    s = (S) r;
                }
                completeValue(f.apply(s, x));
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    static final class UniExceptionally<T> extends UniCompletion<T, T> {
        Function<? super Throwable, ? extends T> fn;

        UniExceptionally(CompletableFuture<T> dep, CompletableFuture<T> src, Function<? super Throwable, ? extends T> fn) {
            super(dep, src);
            this.fn = fn;
        }

        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> d;
            CompletableFuture<T> a;
            Object r;
            Function<? super Throwable, ? extends T> f;
            if ((a = src) == null || (r = a.result) == null || (d = dep) == null || (f = fn) == null || !d.uniExceptionally(r, f, this)) return null;
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    final boolean uniExceptionally(Object r, Function<? super Throwable, ? extends T> f, UniExceptionally<T> c) {
        Throwable x;
        if (result == null) {
            try {
                if (c != null && !c.claim()) return false;
                if (r instanceof AltResult && (x = ((AltResult) r).ex) != null) completeValue(f.apply(x));
                else internalComplete(r);
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    /** Completes dep as src completes (for thenCompose's stage and anyOf's kin). */
    static final class UniRelay<U, T extends U> extends UniCompletion<T, U> {
        UniRelay(CompletableFuture<U> dep, CompletableFuture<T> src) {
            super(dep, src);
        }

        final CompletableFuture<U> tryFire(int mode) {
            CompletableFuture<U> d;
            CompletableFuture<T> a;
            Object r;
            if ((a = src) == null || (r = a.result) == null || (d = dep) == null) return null;
            if (d.result == null) d.completeRelay(r);
            src = null;
            dep = null;
            return d.postFire(a, mode);
        }
    }

    static final class UniCompose<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends CompletionStage<V>> fn;

        UniCompose(CompletableFuture<V> dep, CompletableFuture<T> src, Function<? super T, ? extends CompletionStage<V>> fn) {
            super(dep, src);
            this.fn = fn;
        }

        @SuppressWarnings("unchecked")
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            Function<? super T, ? extends CompletionStage<V>> f;
            Object r;
            Throwable x;
            if ((a = src) == null || (r = a.result) == null || (d = dep) == null || (f = fn) == null) return null;
            tryComplete:
            if (d.result == null) {
                if (r instanceof AltResult) {
                    if ((x = ((AltResult) r).ex) != null) {
                        d.completeThrowable(x, r);
                        break tryComplete;
                    }
                    r = null;
                }
                try {
                    if (!claim()) return null;
                    CompletableFuture<V> g = f.apply((T) r).toCompletableFuture();
                    if ((r = g.result) != null) {
                        d.completeRelay(r);
                    } else {
                        g.unipush(new UniRelay<V, V>(d, g));
                        if (d.result == null) return null;
                    }
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null;
            dep = null;
            fn = null;
            return d.postFire(a, mode);
        }
    }

    /** Both stages' completions: pushed on the first, with a CoCompletion on the second. */
    abstract static class BiCompletion<T, U, V> extends UniCompletion<T, V> {
        CompletableFuture<U> snd;

        BiCompletion(CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(dep, src);
            this.snd = snd;
        }
    }

    static final class CoCompletion extends Completion {
        BiCompletion<?, ?, ?> base;

        CoCompletion(BiCompletion<?, ?, ?> base) {
            this.base = base;
        }

        final CompletableFuture<?> tryFire(int mode) {
            BiCompletion<?, ?, ?> c;
            CompletableFuture<?> d;
            if ((c = base) == null || (d = c.tryFire(mode)) == null) return null;
            base = null;
            return d;
        }
    }

    /** Pushes c on this and (if incomplete) a CoCompletion of it on b. */
    final void bipush(CompletableFuture<?> b, BiCompletion<?, ?, ?> c) {
        if (c != null) {
            boolean pushed = false;
            synchronized (LOCK) {
                if (result == null) {
                    c.next = stack;
                    stack = c;
                    pushed = true;
                }
            }
            if (pushed) {
                if (b != null && b != this && b.result == null) {
                    Completion q = new CoCompletion(c);
                    synchronized (LOCK) {
                        if (b.result == null) {
                            q.next = b.stack;
                            b.stack = q;
                            return;
                        }
                    }
                }
                c.tryFire(SYNC);
            } else if (b != null && b != this) {
                b.unipush(c);
            } else {
                c.tryFire(SYNC);
            }
        }
    }

    final CompletableFuture<T> postFire(CompletableFuture<?> a, CompletableFuture<?> b, int mode) {
        if (b != null && b.stack != null && mode >= 0 && b.result != null) b.postComplete();
        return postFire(a, mode);
    }

    static final class BiApply<T, U, V> extends BiCompletion<T, U, V> {
        BiFunction<? super T, ? super U, ? extends V> fn;

        BiApply(CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> snd, BiFunction<? super T, ? super U, ? extends V> fn) {
            super(dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            Object r, s;
            BiFunction<? super T, ? super U, ? extends V> f;
            if ((a = src) == null || (r = a.result) == null || (b = snd) == null || (s = b.result) == null || (d = dep) == null || (f = fn) == null
                    || !d.biApply(r, s, f, this)) return null;
            src = null;
            snd = null;
            dep = null;
            fn = null;
            return d.postFire(a, b, mode);
        }
    }

    @SuppressWarnings("unchecked")
    final <R, S> boolean biApply(Object r, Object s, BiFunction<? super R, ? super S, ? extends T> f, BiApply<R, S, T> c) {
        Throwable x;
        tryComplete:
        if (result == null) {
            if (r instanceof AltResult) {
                if ((x = ((AltResult) r).ex) != null) {
                    completeThrowable(x, r);
                    break tryComplete;
                }
                r = null;
            }
            if (s instanceof AltResult) {
                if ((x = ((AltResult) s).ex) != null) {
                    completeThrowable(x, s);
                    break tryComplete;
                }
                s = null;
            }
            try {
                if (c != null && !c.claim()) return false;
                completeValue(f.apply((R) r, (S) s));
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    /** allOf's: complete when both are, exceptionally with the first's exception (else the second's). */
    static final class BiRelay<T, U> extends BiCompletion<T, U, Void> {
        BiRelay(CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(dep, src, snd);
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            Object r, s, z;
            Throwable x;
            if ((a = src) == null || (r = a.result) == null || (b = snd) == null || (s = b.result) == null || (d = dep) == null) return null;
            if (d.result == null) {
                if ((r instanceof AltResult && (x = ((AltResult) (z = r)).ex) != null) || (s instanceof AltResult && (x = ((AltResult) (z = s)).ex) != null)) {
                    d.completeThrowable(x, z);
                } else {
                    d.completeNull();
                }
            }
            src = null;
            snd = null;
            dep = null;
            return d.postFire(a, b, mode);
        }
    }

    static CompletableFuture<Void> andTree(CompletableFuture<?>[] cfs, int lo, int hi) {
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        if (lo > hi) {
            d.result = NIL;
        } else {
            CompletableFuture<?> a, b;
            Object r, s, z;
            Throwable x;
            int mid = (lo + hi) >>> 1;
            if ((a = (lo == mid ? cfs[lo] : andTree(cfs, lo, mid))) == null
                    || (b = (lo == hi ? a : (hi == mid + 1) ? cfs[hi] : andTree(cfs, mid + 1, hi))) == null) throw new NullPointerException();
            if ((r = a.result) == null || (s = b.result) == null) {
                a.bipush(b, new BiRelay<>(d, a, b));
            } else if ((r instanceof AltResult && (x = ((AltResult) (z = r)).ex) != null) || (s instanceof AltResult && (x = ((AltResult) (z = s)).ex) != null)) {
                d.result = encodeThrowable(x, z);
            } else {
                d.result = NIL;
            }
        }
        return d;
    }

    // ---- the stages ----

    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        if (fn == null) throw new NullPointerException();
        Object r;
        if ((r = result) != null) {
            CompletableFuture<U> d = new CompletableFuture<U>();
            Throwable x;
            if (r instanceof AltResult) {
                if ((x = ((AltResult) r).ex) != null) {
                    d.result = encodeThrowable(x, r);
                    return d;
                }
                r = null;
            }
            try {
                d.result = d.encodeValue(fn.apply((T) r));
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
            return d;
        }
        CompletableFuture<U> d = new CompletableFuture<U>();
        unipush(new UniApply<T, U>(d, this, fn));
        return d;
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        if (action == null) throw new NullPointerException();
        Object r;
        if ((r = result) != null) {
            CompletableFuture<Void> d = new CompletableFuture<Void>();
            Throwable x;
            if (r instanceof AltResult) {
                if ((x = ((AltResult) r).ex) != null) {
                    d.result = encodeThrowable(x, r);
                    return d;
                }
                r = null;
            }
            try {
                action.accept((T) r);
                d.result = NIL;
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
            return d;
        }
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        unipush(new UniAccept<T>(d, this, action));
        return d;
    }

    public CompletableFuture<Void> thenRun(Runnable action) {
        if (action == null) throw new NullPointerException();
        Object r;
        if ((r = result) != null) {
            CompletableFuture<Void> d = new CompletableFuture<Void>();
            Throwable x;
            if (r instanceof AltResult && (x = ((AltResult) r).ex) != null) {
                d.result = encodeThrowable(x, r);
            } else {
                try {
                    action.run();
                    d.result = NIL;
                } catch (Throwable ex) {
                    d.result = encodeThrowable(ex);
                }
            }
            return d;
        }
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        unipush(new UniRun<T>(d, this, action));
        return d;
    }

    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        if (action == null) throw new NullPointerException();
        CompletableFuture<T> d = new CompletableFuture<T>();
        Object r;
        if ((r = result) == null) unipush(new UniWhenComplete<T>(d, this, action));
        else d.uniWhenComplete(r, action, null);
        return d;
    }

    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        if (fn == null) throw new NullPointerException();
        CompletableFuture<U> d = new CompletableFuture<U>();
        Object r;
        if ((r = result) == null) unipush(new UniHandle<T, U>(d, this, fn));
        else d.uniHandle(r, fn, null);
        return d;
    }

    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        if (fn == null) throw new NullPointerException();
        CompletableFuture<T> d = new CompletableFuture<T>();
        Object r;
        if ((r = result) == null) unipush(new UniExceptionally<T>(d, this, fn));
        else d.uniExceptionally(r, fn, null);
        return d;
    }

    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        if (fn == null) throw new NullPointerException();
        CompletableFuture<U> d = new CompletableFuture<U>();
        Object r, s;
        Throwable x;
        if ((r = result) == null) {
            unipush(new UniCompose<T, U>(d, this, fn));
        } else {
            if (r instanceof AltResult) {
                if ((x = ((AltResult) r).ex) != null) {
                    d.result = encodeThrowable(x, r);
                    return d;
                }
                r = null;
            }
            try {
                CompletableFuture<U> g = fn.apply((T) r).toCompletableFuture();
                if ((s = g.result) != null) d.result = encodeRelay(s);
                else g.unipush(new UniRelay<U, U>(d, g));
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        return d;
    }

    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        CompletableFuture<U> b;
        Object r, s;
        if (fn == null || (b = narrow(other.toCompletableFuture())) == null) throw new NullPointerException();
        CompletableFuture<V> d = new CompletableFuture<V>();
        if ((r = result) == null || (s = b.result) == null) bipush(b, new BiApply<T, U, V>(d, this, b, fn));
        else d.biApply(r, s, fn, null);
        return d;
    }

    @SuppressWarnings("unchecked")
    private static <U> CompletableFuture<U> narrow(CompletableFuture<? extends U> f) {
        return (CompletableFuture<U>) f;
    }

    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    // ---- making them ----

    public static <U> CompletableFuture<U> completedFuture(U value) {
        return new CompletableFuture<U>((value == null) ? NIL : value);
    }

    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        if (ex == null) throw new NullPointerException();
        return new CompletableFuture<U>(new AltResult(ex));
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, ASYNC_POOL);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        if (supplier == null || executor == null) throw new NullPointerException();
        CompletableFuture<U> d = new CompletableFuture<U>();
        executor.execute(() -> {
            if (d.result == null) {
                try {
                    d.completeValue(supplier.get());
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            d.postComplete();
        });
        return d;
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, ASYNC_POOL);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        if (runnable == null || executor == null) throw new NullPointerException();
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        executor.execute(() -> {
            if (d.result == null) {
                try {
                    runnable.run();
                    d.completeNull();
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            d.postComplete();
        });
        return d;
    }

    /** A thread for each task: the JDK's choice when its common pool has no parallelism. */
    static final Executor ASYNC_POOL = r -> new Thread(r).start();

    public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        return andTree(cfs, 0, cfs.length - 1);
    }

    // ---- completing from outside, reading ----

    public boolean complete(T value) {
        boolean triggered = completeValue(value);
        postComplete();
        return triggered;
    }

    public boolean completeExceptionally(Throwable ex) {
        if (ex == null) throw new NullPointerException();
        boolean triggered = internalComplete(new AltResult(ex));
        postComplete();
        return triggered;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = (result == null) && internalComplete(new AltResult(new CancellationException()));
        postComplete();
        return cancelled || isCancelled();
    }

    public boolean isDone() {
        return result != null;
    }

    public boolean isCancelled() {
        Object r;
        return ((r = result) instanceof AltResult) && (((AltResult) r).ex instanceof CancellationException);
    }

    public boolean isCompletedExceptionally() {
        Object r;
        return ((r = result) instanceof AltResult) && r != NIL;
    }

    private Object waitingGet() throws InterruptedException {
        synchronized (LOCK) {
            while (result == null) LOCK.wait();
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T reportGet(Object r) throws InterruptedException, ExecutionException {
        if (r == null) throw new InterruptedException();
        if (r instanceof AltResult) {
            Throwable x, cause;
            if ((x = ((AltResult) r).ex) == null) return null;
            if (x instanceof CancellationException) throw (CancellationException) x;
            if ((x instanceof CompletionException) && (cause = x.getCause()) != null) x = cause;
            throw new ExecutionException(x);
        }
        return (T) r;
    }

    @SuppressWarnings("unchecked")
    private static <T> T reportJoin(Object r) {
        if (r instanceof AltResult) {
            Throwable x;
            if ((x = ((AltResult) r).ex) == null) return null;
            if (x instanceof CancellationException) throw (CancellationException) x;
            if (x instanceof CompletionException) throw (CompletionException) x;
            throw new CompletionException(x);
        }
        return (T) r;
    }

    public T get() throws InterruptedException, ExecutionException {
        Object r;
        if ((r = result) == null) r = waitingGet();
        return reportGet(r);
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long end = System.nanoTime() + unit.toNanos(timeout);
        Object r;
        synchronized (LOCK) {
            while ((r = result) == null) {
                long left = (end - System.nanoTime()) / 1_000_000L;
                if (left <= 0) throw new TimeoutException();
                LOCK.wait(left);
            }
        }
        return reportGet(r);
    }

    public T join() {
        Object r;
        if ((r = result) == null) {
            try {
                r = waitingGet();
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
        }
        return reportJoin(r);
    }

    public T getNow(T valueIfAbsent) {
        Object r;
        return ((r = result) == null) ? valueIfAbsent : reportJoin(r);
    }
}
