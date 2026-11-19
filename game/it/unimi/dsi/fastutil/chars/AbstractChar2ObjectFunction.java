package it.unimi.dsi.fastutil.chars;

/** The stand-in for fastutil's AbstractChar2ObjectFunction: its default return value. */
public abstract class AbstractChar2ObjectFunction<V> implements Char2ObjectFunction<V>, java.io.Serializable {
    protected V defRetValue;

    protected AbstractChar2ObjectFunction() {}

    @Override
    public void defaultReturnValue(V rv) {
        defRetValue = rv;
    }

    @Override
    public V defaultReturnValue() {
        return defRetValue;
    }
}
