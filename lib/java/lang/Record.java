package java.lang;

/** The model of java.lang.Record: a record's equals, hashCode and toString come from ObjectMethods (the translator). */
public abstract class Record {
    protected Record() {}

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
