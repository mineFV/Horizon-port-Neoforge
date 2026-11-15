package java.lang.ref;

/** The model of java.lang.ref.SoftReference: see Reference. */
public class SoftReference<T> extends Reference<T> {
    public SoftReference(T referent) {
        super(referent);
    }

    public SoftReference(T referent, ReferenceQueue<? super T> q) {
        super(referent);
    }
}
