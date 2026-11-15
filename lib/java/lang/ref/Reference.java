package java.lang.ref;

/**
 * The model of java.lang.ref.Reference: the collector never clears it, which is one of the runs the JDK allows (it
 * may clear a reference once its object is only weakly reachable, or never). The object stays alive as long as the
 * reference does.
 */
public abstract class Reference<T> {
    private T referent;

    Reference(T referent) {
        this.referent = referent;
    }

    public T get() {
        return referent;
    }

    public final boolean refersTo(T obj) {
        return referent == obj;
    }

    public void clear() {
        referent = null;
    }


    @Deprecated
    public boolean isEnqueued() {
        return false;
    }
}
