package java.lang.ref;

/** The model of java.lang.ref.PhantomReference: see Reference. */
public class PhantomReference<T> extends Reference<T> {
    public PhantomReference(T referent, ReferenceQueue<? super T> q) {
        super(referent);
    }

    /** A phantom reference's object can't be got. */
    public T get() {
        return null;
    }
}
