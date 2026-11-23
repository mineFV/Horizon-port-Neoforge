package horizonneo.probe;

/**
 * What naming a class that isn't there does, as another mod's class that isn't installed: StandIns deletes
 * HorizonProbeAbsent once the probe is compiled (StandIns.ABSENT). The JVM resolves a class the first time an
 * instruction naming it runs, so each throws there, and only there; a type test on null resolves nothing.
 */
final class AbsentProbe {
    private AbsentProbe() {}

    static void run() {
        p("absent class call", () -> thrown(() -> HorizonProbeAbsent.value()));
        p("absent class call again", () -> thrown(() -> HorizonProbeAbsent.value()));
        p("absent class new", () -> thrown(() -> new HorizonProbeAbsent()));
        p("absent class static field", () -> thrown(() -> HorizonProbeAbsent.count));
        p("absent class static field set", () -> thrown(() -> HorizonProbeAbsent.count = 3));
        p("absent class field of null", () -> thrown(() -> ((HorizonProbeAbsent) nothing()).size));
        p("absent class array", () -> thrown(() -> new HorizonProbeAbsent[2]));
        p("absent class arrays", () -> thrown(() -> new HorizonProbeAbsent[2][3]));
        p("absent class constant", () -> thrown(() -> HorizonProbeAbsent.class));
        p("absent class array constant", () -> thrown(() -> HorizonProbeAbsent[].class));
        p("absent class cast of null", () -> thrown(() -> (HorizonProbeAbsent) nothing()));
        p("absent class test of null", () -> thrown(() -> nothing() instanceof HorizonProbeAbsent));
        p("absent class cast", () -> thrown(() -> (HorizonProbeAbsent) something()));
        p("absent class test", () -> thrown(() -> something() instanceof HorizonProbeAbsent));
        p("absent class not run", () -> thrown(() -> guarded(false)));
    }

    static Object nothing() {
        return null;
    }

    static Object something() {
        return "text";
    }

    /** A branch that names the class, not taken: the class naming it loads and runs all the same. */
    static String guarded(boolean run) {
        if (run) return String.valueOf(HorizonProbeAbsent.value());
        return "not run";
    }

    /** What running call gives, or what it throws: its class and message, its cause's, and that one's cause. */
    static String thrown(java.util.function.Supplier<Object> call) {
        try {
            return "gives " + call.get();
        } catch (Throwable e) {
            Throwable c = e.getCause();
            return e.getClass().getName() + ": " + e.getMessage() + " | cause "
                    + (c == null ? "null" : c.getClass().getName() + ": " + c.getMessage() + " | its cause " + c.getCause());
        }
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}

/** A class the probe names, deleted once it is compiled: it isn't there when the probe runs. */
final class HorizonProbeAbsent {
    static int count = 1;
    int size;

    static int value() {
        return 2;
    }
}
