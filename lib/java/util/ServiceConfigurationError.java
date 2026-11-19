package java.util;

/** The model of java.util.ServiceConfigurationError: a provider that couldn't be made. */
public class ServiceConfigurationError extends Error {
    public ServiceConfigurationError(String msg) {
        super(msg);
    }

    public ServiceConfigurationError(String msg, Throwable cause) {
        super(msg, cause);
    }
}
