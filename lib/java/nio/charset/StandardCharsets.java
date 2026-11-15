package java.nio.charset;

/** The model of java.nio.charset.StandardCharsets: UTF-8, ISO-8859-1, US-ASCII (the UTF-16 ones aren't modelled). */
public final class StandardCharsets {
    private StandardCharsets() {}

    public static final Charset UTF_8 = new Standard("UTF-8", 0);
    public static final Charset ISO_8859_1 = new Standard("ISO-8859-1", 1);
    public static final Charset US_ASCII = new Standard("US-ASCII", 2);

    private static final class Standard extends Charset {
        private final int kind;

        Standard(String name, int kind) {
            super(name, null);
            this.kind = kind;
        }

        int jKind() {
            return kind;
        }
    }
}
