package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's AbstractReference2ObjectFunction: its default return value. */
public abstract class AbstractReference2ObjectFunction<K, V> implements Reference2ObjectFunction<K, V>, java.io.Serializable {
    protected V defRetValue;

    protected AbstractReference2ObjectFunction() {}

    @Override
    public void defaultReturnValue(V rv) {
        defRetValue = rv;
    }

    @Override
    public V defaultReturnValue() {
        return defRetValue;
    }
}
