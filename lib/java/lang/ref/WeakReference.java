package java.lang.ref;

/** The model of java.lang.ref.WeakReference: see Reference. */
public class WeakReference<T> extends Reference<T> {
    public WeakReference(T referent) {
        super(referent);
    }

    public WeakReference(T referent, ReferenceQueue<? super T> q) {
        super(referent);
    }
}
