package net.minecraft.world.phys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The stand-in for the game's AABB: a box between two corners, each made from the smaller and the larger of the
 * coordinates given; moved, grown, shrunk, intersected and compared (observed, `standins`, NaN and -0.0 included).
 * Clipping a ray isn't here.
 */
public class AABB {
    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;

    public AABB(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public AABB(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    public AABB(Vec3 a, Vec3 b) {
        this(a.x, a.y, a.z, b.x, b.y, b.z);
    }

    public static AABB unitCubeFromLowerCorner(Vec3 v) {
        return new AABB(v.x, v.y, v.z, v.x + 1.0, v.y + 1.0, v.z + 1.0);
    }

    public static AABB encapsulatingFullBlocks(BlockPos a, BlockPos b) {
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    public AABB setMinX(double v) {
        return new AABB(v, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB setMinY(double v) {
        return new AABB(minX, v, minZ, maxX, maxY, maxZ);
    }

    public AABB setMinZ(double v) {
        return new AABB(minX, minY, v, maxX, maxY, maxZ);
    }

    public AABB setMaxX(double v) {
        return new AABB(minX, minY, minZ, v, maxY, maxZ);
    }

    public AABB setMaxY(double v) {
        return new AABB(minX, minY, minZ, maxX, v, maxZ);
    }

    public AABB setMaxZ(double v) {
        return new AABB(minX, minY, minZ, maxX, maxY, v);
    }

    public double min(Direction.Axis axis) {
        return axis.choose(minX, minY, minZ);
    }

    public double max(Direction.Axis axis) {
        return axis.choose(maxX, maxY, maxZ);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AABB b)) return false;
        return Double.compare(b.minX, minX) == 0 && Double.compare(b.minY, minY) == 0 && Double.compare(b.minZ, minZ) == 0
                && Double.compare(b.maxX, maxX) == 0 && Double.compare(b.maxY, maxY) == 0 && Double.compare(b.maxZ, maxZ) == 0;
    }

    @Override
    public int hashCode() {
        long l = Double.doubleToLongBits(minX);
        int i = (int) (l ^ l >>> 32);
        l = Double.doubleToLongBits(minY);
        i = 31 * i + (int) (l ^ l >>> 32);
        l = Double.doubleToLongBits(minZ);
        i = 31 * i + (int) (l ^ l >>> 32);
        l = Double.doubleToLongBits(maxX);
        i = 31 * i + (int) (l ^ l >>> 32);
        l = Double.doubleToLongBits(maxY);
        i = 31 * i + (int) (l ^ l >>> 32);
        l = Double.doubleToLongBits(maxZ);
        return 31 * i + (int) (l ^ l >>> 32);
    }

    public AABB contract(double x, double y, double z) {
        double x1 = minX, y1 = minY, z1 = minZ, x2 = maxX, y2 = maxY, z2 = maxZ;
        if (x < 0.0) x1 -= x;
        else if (x > 0.0) x2 -= x;
        if (y < 0.0) y1 -= y;
        else if (y > 0.0) y2 -= y;
        if (z < 0.0) z1 -= z;
        else if (z > 0.0) z2 -= z;
        return new AABB(x1, y1, z1, x2, y2, z2);
    }

    public AABB expandTowards(Vec3 v) {
        return expandTowards(v.x, v.y, v.z);
    }

    public AABB expandTowards(double x, double y, double z) {
        double x1 = minX, y1 = minY, z1 = minZ, x2 = maxX, y2 = maxY, z2 = maxZ;
        if (x < 0.0) x1 += x;
        else if (x > 0.0) x2 += x;
        if (y < 0.0) y1 += y;
        else if (y > 0.0) y2 += y;
        if (z < 0.0) z1 += z;
        else if (z > 0.0) z2 += z;
        return new AABB(x1, y1, z1, x2, y2, z2);
    }

    public AABB inflate(double x, double y, double z) {
        return new AABB(minX - x, minY - y, minZ - z, maxX + x, maxY + y, maxZ + z);
    }

    public AABB inflate(double d) {
        return inflate(d, d, d);
    }

    public AABB intersect(AABB o) {
        return new AABB(Math.max(minX, o.minX), Math.max(minY, o.minY), Math.max(minZ, o.minZ), Math.min(maxX, o.maxX), Math.min(maxY, o.maxY), Math.min(maxZ, o.maxZ));
    }

    public AABB minmax(AABB o) {
        return new AABB(Math.min(minX, o.minX), Math.min(minY, o.minY), Math.min(minZ, o.minZ), Math.max(maxX, o.maxX), Math.max(maxY, o.maxY), Math.max(maxZ, o.maxZ));
    }

    public AABB move(double x, double y, double z) {
        return new AABB(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
    }

    public AABB move(BlockPos pos) {
        return new AABB(minX + pos.getX(), minY + pos.getY(), minZ + pos.getZ(), maxX + pos.getX(), maxY + pos.getY(), maxZ + pos.getZ());
    }

    public AABB move(Vec3 v) {
        return move(v.x, v.y, v.z);
    }

    public boolean intersects(AABB o) {
        return intersects(o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ);
    }

    public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2) {
        return minX < x2 && maxX > x1 && minY < y2 && maxY > y1 && minZ < z2 && maxZ > z1;
    }

    public boolean intersects(Vec3 a, Vec3 b) {
        return intersects(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z), Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    public boolean contains(Vec3 v) {
        return contains(v.x, v.y, v.z);
    }

    public boolean contains(double x, double y, double z) {
        return x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ;
    }

    public double getSize() {
        return (getXsize() + getYsize() + getZsize()) / 3.0;
    }

    public double getXsize() {
        return maxX - minX;
    }

    public double getYsize() {
        return maxY - minY;
    }

    public double getZsize() {
        return maxZ - minZ;
    }

    public AABB deflate(double x, double y, double z) {
        return inflate(-x, -y, -z);
    }

    public AABB deflate(double d) {
        return inflate(-d);
    }

    public double distanceToSqr(Vec3 v) {
        double x = Math.max(Math.max(minX - v.x, v.x - maxX), 0.0);
        double y = Math.max(Math.max(minY - v.y, v.y - maxY), 0.0);
        double z = Math.max(Math.max(minZ - v.z, v.z - maxZ), 0.0);
        return x * x + y * y + z * z;
    }

    @Override
    public String toString() {
        return "AABB[" + minX + ", " + minY + ", " + minZ + "] -> [" + maxX + ", " + maxY + ", " + maxZ + "]";
    }

    public boolean hasNaN() {
        return Double.isNaN(minX) || Double.isNaN(minY) || Double.isNaN(minZ) || Double.isNaN(maxX) || Double.isNaN(maxY) || Double.isNaN(maxZ);
    }

    public Vec3 getCenter() {
        return new Vec3(net.minecraft.util.Mth.lerp(0.5, minX, maxX), net.minecraft.util.Mth.lerp(0.5, minY, maxY), net.minecraft.util.Mth.lerp(0.5, minZ, maxZ));
    }

    public Vec3 getBottomCenter() {
        return new Vec3(net.minecraft.util.Mth.lerp(0.5, minX, maxX), minY, net.minecraft.util.Mth.lerp(0.5, minZ, maxZ));
    }

    public Vec3 getMinPosition() {
        return new Vec3(minX, minY, minZ);
    }

    public Vec3 getMaxPosition() {
        return new Vec3(maxX, maxY, maxZ);
    }

    public static AABB ofSize(Vec3 center, double x, double y, double z) {
        return new AABB(center.x - x / 2.0, center.y - y / 2.0, center.z - z / 2.0, center.x + x / 2.0, center.y + y / 2.0, center.z + z / 2.0);
    }
}
