package java.lang.ref;

/** The model of java.lang.ref.ReferenceQueue: nothing is ever enqueued (see Reference). */
public class ReferenceQueue<T> {
    public ReferenceQueue() {}

    public Reference<? extends T> poll() {
        return null;
    }
}
