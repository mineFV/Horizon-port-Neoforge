package java.text;

/**
 * The model of java.text.Format: NumberFormat's superclass, nothing of its own (format(Object), parseObject and the
 * field positions aren't modelled).
 */
public abstract class Format implements java.io.Serializable, Cloneable {
    protected Format() {}
}
