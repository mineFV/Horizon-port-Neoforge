package it.unimi.dsi.fastutil.longs;

/** The stand-in for fastutil's AbstractLong2ObjectFunction: its default return value. */
public abstract class AbstractLong2ObjectFunction<V> implements Long2ObjectFunction<V>, java.io.Serializable {
    protected V defRetValue;

    protected AbstractLong2ObjectFunction() {}

    @Override
    public void defaultReturnValue(V rv) {
        defRetValue = rv;
    }

    @Override
    public V defaultReturnValue() {
        return defRetValue;
    }
}
