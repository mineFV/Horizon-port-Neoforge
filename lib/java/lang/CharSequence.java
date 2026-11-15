package java.lang;

/** The model of java.lang.CharSequence: what String needs of it. */
public interface CharSequence {
    int length();

    char charAt(int index);

    CharSequence subSequence(int start, int end);

    String toString();

    default boolean isEmpty() {
        return this.length() == 0;
    }
}
