package net.minecraft.world.phys;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;

/**
 * The stand-in for the game's Vec3: a point in doubles, observed (`standins`, probe/VecProbe.java): every method
 * gives a new Vec3, equality compares each coordinate as Double.compare does (0.0 and -0.0 differ, NaN equals NaN).
 * xRot, yRot and zRot aren't here: they use the game's sine, which Mth's stand-in leaves out.
 */
public class Vec3 implements Position {
    public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);

    public final double x;
    public final double y;
    public final double z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Vec3 atLowerCornerOf(Vec3i v) {
        return new Vec3(v.getX(), v.getY(), v.getZ());
    }

    public static Vec3 atLowerCornerWithOffset(Vec3i v, double dx, double dy, double dz) {
        return new Vec3(v.getX() + dx, v.getY() + dy, v.getZ() + dz);
    }

    public static Vec3 atCenterOf(Vec3i v) {
        return atLowerCornerWithOffset(v, 0.5, 0.5, 0.5);
    }

    public static Vec3 atBottomCenterOf(Vec3i v) {
        return atLowerCornerWithOffset(v, 0.5, 0.0, 0.5);
    }

    public static Vec3 upFromBottomCenterOf(Vec3i v, double up) {
        return atLowerCornerWithOffset(v, 0.5, up, 0.5);
    }

    /** Observed: the other point less this one. */
    public Vec3 vectorTo(Vec3 other) {
        return new Vec3(other.x - x, other.y - y, other.z - z);
    }

    /** Observed: ZERO itself when the length is below 1.0E-4, else each coordinate divided by the length. */
    public Vec3 normalize() {
        double length = Math.sqrt(x * x + y * y + z * z);
        return length < 1.0E-4 ? ZERO : new Vec3(x / length, y / length, z / length);
    }

    public double dot(Vec3 v) {
        return x * v.x + y * v.y + z * v.z;
    }

    public Vec3 cross(Vec3 v) {
        return new Vec3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x);
    }

    public Vec3 subtract(Vec3 v) {
        return subtract(v.x, v.y, v.z);
    }

    public Vec3 subtract(double dx, double dy, double dz) {
        return add(-dx, -dy, -dz);
    }

    public Vec3 add(Vec3 v) {
        return add(v.x, v.y, v.z);
    }

    public Vec3 add(double dx, double dy, double dz) {
        return new Vec3(x + dx, y + dy, z + dz);
    }

    /** Observed: the squared distance below the squared limit. */
    public boolean closerThan(Position p, double distance) {
        return distanceToSqr(p.x(), p.y(), p.z()) < distance * distance;
    }

    public double distanceTo(Vec3 v) {
        double dx = v.x - x, dy = v.y - y, dz = v.z - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double distanceToSqr(Vec3 v) {
        double dx = v.x - x, dy = v.y - y, dz = v.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceToSqr(double ox, double oy, double oz) {
        double dx = ox - x, dy = oy - y, dz = oz - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Observed: the horizontal squared distance below the squared limit, and the height difference below its own. */
    public boolean closerThan(Vec3 v, double horizontal, double vertical) {
        double dx = v.x - x, dy = v.y - y, dz = v.z - z;
        return dx * dx + dz * dz < horizontal * horizontal && Math.abs(dy) < vertical;
    }

    public Vec3 scale(double f) {
        return multiply(f, f, f);
    }

    public Vec3 reverse() {
        return scale(-1.0);
    }

    public Vec3 multiply(Vec3 v) {
        return multiply(v.x, v.y, v.z);
    }

    public Vec3 multiply(double fx, double fy, double fz) {
        return new Vec3(x * fx, y * fy, z * fz);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSqr() {
        return x * x + y * y + z * z;
    }

    public double horizontalDistance() {
        return Math.sqrt(x * x + z * z);
    }

    public double horizontalDistanceSqr() {
        return x * x + z * z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vec3 v)) return false;
        return Double.compare(v.x, x) == 0 && Double.compare(v.y, y) == 0 && Double.compare(v.z, z) == 0;
    }

    /** Observed: each coordinate's bits folded to an int, combined as 31 times the previous plus the next. */
    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(x);
        int h = (int) (bits ^ bits >>> 32);
        bits = Double.doubleToLongBits(y);
        h = 31 * h + (int) (bits ^ bits >>> 32);
        bits = Double.doubleToLongBits(z);
        return 31 * h + (int) (bits ^ bits >>> 32);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    /** Observed: each coordinate from this one towards the other's, by t (Mth.lerp). */
    public Vec3 lerp(Vec3 to, double t) {
        return new Vec3(x + t * (to.x - x), y + t * (to.y - y), z + t * (to.z - z));
    }

    public double get(Direction.Axis axis) {
        return axis.choose(x, y, z);
    }

    public Vec3 with(Direction.Axis axis, double v) {
        double nx = axis == Direction.Axis.X ? v : x;
        double ny = axis == Direction.Axis.Y ? v : y;
        double nz = axis == Direction.Axis.Z ? v : z;
        return new Vec3(nx, ny, nz);
    }

    /** Observed: the direction's normal times the distance, added. */
    public Vec3 relative(Direction d, double distance) {
        return new Vec3(x + d.getStepX() * distance, y + d.getStepY() * distance, z + d.getStepZ() * distance);
    }

    @Override
    public final double x() {
        return x;
    }

    @Override
    public final double y() {
        return y;
    }

    @Override
    public final double z() {
        return z;
    }
}
