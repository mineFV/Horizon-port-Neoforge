package it.unimi.dsi.fastutil.objects;

/** The stand-in for fastutil's AbstractObject2ObjectFunction: its default return value. */
public abstract class AbstractObject2ObjectFunction<K, V> implements Object2ObjectFunction<K, V>, java.io.Serializable {
    protected V defRetValue;

    protected AbstractObject2ObjectFunction() {}

    @Override
    public void defaultReturnValue(V rv) {
        defRetValue = rv;
    }

    @Override
    public V defaultReturnValue() {
        return defRetValue;
    }
}
