package java.lang;

/**
 * The model of java.lang.Object. hashCode is the object's identity, as the JVM's is (its value differs from a JVM's,
 * which differs from run to run anyway). getClass and clone aren't modelled yet.
 */
public class Object {
    public Object() {}

    public native int hashCode();

    public boolean equals(Object obj) {
        return this == obj;
    }

    public String toString() {
        return jClassName().concat("@").concat(Integer.toHexString(hashCode()));
    }

    /** The binary name of the object's class (`com.example.Foo$Bar`): what getClass().getName() gives. */
    final native String jClassName();
}
