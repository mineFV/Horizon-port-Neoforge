package net.minecraft;

/** The stand-in for the game's ResourceLocationException: an invalid id. */
public class ResourceLocationException extends RuntimeException {
    public ResourceLocationException(String message) {
        super(message);
    }

    public ResourceLocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
