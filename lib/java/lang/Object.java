package java.lang;

/**
 * The model of java.lang.Object. hashCode is the object's identity, as the JVM's is (its value differs from a JVM's,
 * which differs from run to run anyway). clone is declared, so that javac clones the enums' values() as it does
 * against the JDK (without it, it copies them with System.arraycopy), but only an array's is translated.
 */
public class Object {
    public Object() {}

    public native int hashCode();

    /** The object's class: one object for each type, the same as its class constant. */
    public final native Class<?> getClass();

    protected native Object clone() throws CloneNotSupportedException;

    public final void wait() throws InterruptedException {
        jWait(0L);
    }

    public final void wait(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis < 0) throw new IllegalArgumentException("timeout value is negative");
        jWait(timeoutMillis);
    }

    public final void notify() {
        jNotify();
    }

    public final void notifyAll() {
        jNotify();
    }

    private native void jWait(long timeoutMillis);

    private native void jNotify();

    public boolean equals(Object obj) {
        return this == obj;
    }

    public String toString() {
        return jClassName().concat("@").concat(Integer.toHexString(hashCode()));
    }

    /** The binary name of the object's class (`com.example.Foo$Bar`): what getClass().getName() gives. */
    final native String jClassName();
}
