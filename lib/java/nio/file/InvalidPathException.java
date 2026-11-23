package java.nio.file;

/** The model of java.nio.file.InvalidPathException: its input, reason and message (the reason, then the input). */
public class InvalidPathException extends IllegalArgumentException {
    private final String input;
    private final int index;

    public InvalidPathException(String input, String reason, int index) {
        super(reason);
        if (input == null || reason == null) throw new NullPointerException();
        if (index < -1) throw new IllegalArgumentException();
        this.input = input;
        this.index = index;
    }

    public InvalidPathException(String input, String reason) {
        this(input, reason, -1);
    }

    public String getInput() {
        return input;
    }

    public String getReason() {
        return super.getMessage();
    }

    public int getIndex() {
        return index;
    }

    public String getMessage() {
        StringBuilder b = new StringBuilder(getReason());
        if (index > -1) b.append(" at index ").append(index);
        return b.append(": ").append(input).toString();
    }
}
