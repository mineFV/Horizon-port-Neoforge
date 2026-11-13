package horizonneo;

import java.util.Objects;

/** A count of checks. The tests are plain programs, so a JDK is all they need. */
final class Checks {
    /** Something that should be refused. */
    interface Attempt {
        void run() throws Exception;
    }

    private final String subject;
    private int right, wrong;

    Checks(String subject) {
        this.subject = subject;
    }

    void same(String what, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) right++;
        else wrong(what + ": expected <" + expected + ">, got <" + actual + ">");
    }

    void yes(String what, boolean holds) {
        if (holds) right++;
        else wrong(what);
    }

    /** `attempt` must throw, with `part` in its message. */
    void refuses(String what, String part, Attempt attempt) {
        try {
            attempt.run();
        } catch (Exception e) {
            if (String.valueOf(e.getMessage()).contains(part)) right++;
            else wrong(what + ": expected a message holding <" + part + ">, got <" + e.getMessage() + ">");
            return;
        }
        wrong(what + ": expected a refusal, got none");
    }

    private void wrong(String message) {
        wrong++;
        System.out.println("  FAIL: " + subject + ": " + message);
    }

    /** Deletes a folder a test made, and all in it. */
    static void delete(java.nio.file.Path dir) throws java.io.IOException {
        if (!java.nio.file.Files.exists(dir)) return;
        try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.walk(dir)) {
            for (java.nio.file.Path p : s.sorted(java.util.Comparator.reverseOrder()).toList()) java.nio.file.Files.delete(p);
        }
    }

    /** Prints the count and returns the program's exit code. */
    int done() {
        if (wrong == 0) System.out.println("  ok: " + subject + ": " + right + " checks");
        else System.out.println("  FAILED: " + subject + ": " + wrong + " of " + (right + wrong) + " checks");
        return wrong == 0 ? 0 : 1;
    }
}
