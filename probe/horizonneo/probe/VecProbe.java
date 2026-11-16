package horizonneo.probe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** The game's Vec3, as GameProbe observes the rest (see there). */
final class VecProbe {
    private VecProbe() {}

    static final double[] C = {0.0, -0.0, 0.5, -1.25, 3.0, 1e-5, 1e-3, 64.5, -1e9, 1e300, Double.NaN, Double.POSITIVE_INFINITY};

    static void run() {
        p("vec3 zero", () -> Vec3.ZERO + " " + (Vec3.ZERO == Vec3.ZERO) + " " + Vec3.ZERO.hashCode());
        Vec3[] vs = new Vec3[C.length];
        for (int i = 0; i < C.length; i++) vs[i] = new Vec3(C[i], C[(i + 3) % C.length], C[(i + 7) % C.length]);
        for (int i = 0; i < vs.length; i++) {
            Vec3 a = vs[i];
            String k = "vec3 " + i;
            p(k + " text", () -> a + " " + a.x + " " + a.y + " " + a.z + " " + a.x() + " " + a.hashCode() + " " + a.equals(new Vec3(a.x, a.y, a.z)));
            p(k + " lengths", () -> bits(a.length()) + " " + bits(a.lengthSqr()) + " " + bits(a.horizontalDistance()) + " " + bits(a.horizontalDistanceSqr()));
            p(k + " normalize", () -> a.normalize() + " " + (a.normalize() == Vec3.ZERO) + " " + a.reverse() + " " + a.scale(2.5) + " " + a.scale(1.0).equals(a) + " " + (a.scale(1.0) == a));
            for (Direction.Axis ax : Direction.Axis.values()) p(k + " axis " + ax, () -> bits(a.get(ax)) + " " + a.with(ax, 7.25));
            for (Direction d : Direction.values()) p(k + " relative " + d, () -> a.relative(d, 1.5) + " " + a.relative(d, -0.0));
            for (int j = 0; j < vs.length; j++) {
                Vec3 b = vs[j];
                String kk = k + " " + j;
                p(kk + " ops", () -> a.add(b) + " " + a.subtract(b) + " " + a.vectorTo(b) + " " + a.multiply(b) + " " + a.cross(b) + " " + bits(a.dot(b)));
                p(kk + " distances", () -> bits(a.distanceTo(b)) + " " + bits(a.distanceToSqr(b)) + " " + bits(a.distanceToSqr(b.x, b.y, b.z)) + " " + a.closerThan(b, 10.0) + " " + a.closerThan(b, 10.0, 2.0));
                p(kk + " lerp equals", () -> a.lerp(b, 0.25) + " " + a.equals(b) + " " + a.add(b.x, 0, -b.z) + " " + a.subtract(b.x, 1, 2) + " " + a.multiply(b.x, 2, 0));
            }
            p(k + " equals other", () -> a.equals(null) + " " + a.equals(a.toString()) + " " + (a.add(0, 0, 0) == a) + " " + (a.subtract(Vec3.ZERO) == a));
        }
        p("vec3 signed zeros", () -> new Vec3(0, 0, 0).equals(new Vec3(-0.0, 0, 0)) + " " + new Vec3(-0.0, 0, 0).hashCode() + " " + new Vec3(Double.NaN, 0, 0).equals(new Vec3(Double.NaN, 0, 0)));
        for (int[] at : GameProbe.PLACES) {
            BlockPos pos = new BlockPos(at[0], at[1], at[2]);
            p("vec3 of " + pos, () -> Vec3.atLowerCornerOf(pos) + " " + Vec3.atCenterOf(pos) + " " + Vec3.atBottomCenterOf(pos) + " " + Vec3.upFromBottomCenterOf(pos, 0.25)
                    + " " + Vec3.atLowerCornerWithOffset(pos, 0.5, -0.5, 2.0) + " " + pos.getCenter() + " " + BlockPos.containing(Vec3.atCenterOf(pos)));
        }
        p("vec3 near", () -> new Vec3(1, 2, 3).closerThan(new Vec3(1, 2, 4), 1.0) + " " + new Vec3(1, 2, 3).closerThan(new Vec3(1, 2, 3.999), 1.0)
                + " " + new Vec3(0, 0, 0).closerThan(new Vec3(1, 5, 0), 1.5, 10.0) + " " + new Vec3(0, 0, 0).closerThan(new Vec3(1, 5, 0), 1.5, 4.0));
        p("vec3 tiny normalize", () -> new Vec3(1e-5, 0, 0).normalize() + " " + new Vec3(1e-4, 0, 0).normalize() + " " + new Vec3(9.9e-5, 0, 0).normalize() + " " + (new Vec3(1e-5, 0, 0).normalize() == Vec3.ZERO));
    }

    static long bits(double d) {
        return Double.doubleToLongBits(d);
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
