package java.util.concurrent;

/** The model of java.util.concurrent.TimeUnit: conversions saturate at Long's bounds, as the JDK's. */
public enum TimeUnit {
    NANOSECONDS(1L),
    MICROSECONDS(1000L),
    MILLISECONDS(1000L * 1000L),
    SECONDS(1000L * 1000L * 1000L),
    MINUTES(1000L * 1000L * 1000L * 60L),
    HOURS(1000L * 1000L * 1000L * 60L * 60L),
    DAYS(1000L * 1000L * 1000L * 60L * 60L * 24L);

    private final long scale;

    TimeUnit(long scale) {
        this.scale = scale;
    }

    /** d of this unit in a unit of the given scale, saturated. */
    private long to(long d, long other) {
        if (scale == other) return d;
        if (scale < other) return d / (other / scale);
        long ratio = scale / other;
        long max = Long.MAX_VALUE / ratio;
        if (d > max) return Long.MAX_VALUE;
        if (d < -max) return Long.MIN_VALUE;
        return d * ratio;
    }

    public long convert(long sourceDuration, TimeUnit sourceUnit) {
        return sourceUnit.to(sourceDuration, scale);
    }

    public long toNanos(long duration) {
        return to(duration, NANOSECONDS.scale);
    }

    public long toMicros(long duration) {
        return to(duration, MICROSECONDS.scale);
    }

    public long toMillis(long duration) {
        return to(duration, MILLISECONDS.scale);
    }

    public long toSeconds(long duration) {
        return to(duration, SECONDS.scale);
    }

    public long toMinutes(long duration) {
        return to(duration, MINUTES.scale);
    }

    public long toHours(long duration) {
        return to(duration, HOURS.scale);
    }

    public long toDays(long duration) {
        return to(duration, DAYS.scale);
    }

    public void sleep(long timeout) throws InterruptedException {
        if (timeout > 0) Thread.sleep(toMillis(timeout));
    }
}
