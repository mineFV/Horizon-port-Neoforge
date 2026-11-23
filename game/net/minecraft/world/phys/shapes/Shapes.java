package net.minecraft.world.phys.shapes;

import net.minecraft.world.phys.AABB;

/**
 * The stand-in for the game's Shapes: the empty shape and the block, a box made a shape (a box thinner than EPSILON
 * nothing, one on a grid of up to 1/8 a cube's, else an array shape of its two boundaries), the joins of two shapes
 * (their boundaries merged as the game merges them: two cube grids at their least common multiple while small, the
 * same boundaries kept, else merged within EPSILON) and their optimized form, after Horizon's
 * horizon_core::geometry::shape (MIT, tested against vanilla there). Observed in a server (`standins`,
 * probe/ShapeProbe.java). Not here: INFINITY, collide, the faces' occlusion.
 */
public final class Shapes {
    public static final double EPSILON = 1.0E-7;
    public static final double BIG_EPSILON = 1.0E-6;
    private static final VoxelShape BLOCK = VoxelShape.cube(new int[] {1, 1, 1}, new boolean[] {true});
    private static final VoxelShape EMPTY = VoxelShape.build(new double[][] {{0.0}, {0.0}, {0.0}}, new boolean[0], false, new boolean[3]);

    public Shapes() {}

    public static VoxelShape empty() {
        return EMPTY;
    }

    public static VoxelShape block() {
        return BLOCK;
    }

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
        return create(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static VoxelShape create(AABB aabb) {
        return create(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    public static VoxelShape create(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (maxX - minX < EPSILON || maxY - minY < EPSILON || maxZ - minZ < EPSILON) return empty();
        int bx = findBits(minX, maxX), by = findBits(minY, maxY), bz = findBits(minZ, maxZ);
        if (bx < 0 || by < 0 || bz < 0) {
            return VoxelShape.build(new double[][] {{minX, maxX}, {minY, maxY}, {minZ, maxZ}}, new boolean[] {true}, false, new boolean[3]);
        }
        if (bx == 0 && by == 0 && bz == 0) return block();
        int sx = 1 << bx, sy = 1 << by, sz = 1 << bz;
        int lx = (int) Math.round(minX * sx), ly = (int) Math.round(minY * sy), lz = (int) Math.round(minZ * sz);
        int hx = (int) Math.round(maxX * sx), hy = (int) Math.round(maxY * sy), hz = (int) Math.round(maxZ * sz);
        boolean[] occupied = new boolean[sx * sy * sz];
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    occupied[(x * sy + y) * sz + z] = x >= lx && x < hx && y >= ly && y < hy && z >= lz && z < hz;
                }
            }
        }
        return VoxelShape.cube(new int[] {sx, sy, sz}, occupied).withDiscreteBounds(new int[] {lx, ly, lz}, new int[] {hx, hy, hz});
    }

    /** The grid (up to 1/8) both ends of a box's side are on, within EPSILON: its power of two, or -1. */
    protected static int findBits(double min, double max) {
        if (min < -EPSILON || max > 1.0000001) return -1;
        for (int bits = 0; bits <= 3; bits++) {
            int size = 1 << bits;
            double lo = min * size, hi = max * size;
            if (Math.abs(lo - Math.round(lo)) < EPSILON * size && Math.abs(hi - Math.round(hi)) < EPSILON * size) return bits;
        }
        return -1;
    }

    public static VoxelShape or(VoxelShape first, VoxelShape second) {
        return join(first, second, BooleanOp.OR);
    }

    public static VoxelShape or(VoxelShape first, VoxelShape... others) {
        VoxelShape result = first;
        for (VoxelShape other : others) result = or(result, other);
        return result;
    }

    public static VoxelShape join(VoxelShape first, VoxelShape second, BooleanOp op) {
        return joinUnoptimized(first, second, op).optimize();
    }

    public static VoxelShape joinUnoptimized(VoxelShape first, VoxelShape second, BooleanOp op) {
        if (op.apply(false, false)) throw new IllegalArgumentException();
        if (first == second) return op.apply(true, true) ? first : empty();
        boolean onlyFirst = op.apply(true, false), onlySecond = op.apply(false, true);
        if (first.isEmpty()) return onlySecond ? second : empty();
        if (second.isEmpty()) return onlyFirst ? first : empty();
        Object[][] m = mergers(first, second, onlyFirst, onlySecond);
        int nx = coords(m[0]).length - 1, ny = coords(m[1]).length - 1, nz = coords(m[2]).length - 1;
        if ((long) nx * ny * nz > 1 << 20) throw new UnsupportedOperationException("a voxel shape of more than 2^20 cells isn't modelled (horizon-port-neoforge)");
        boolean[] occupied = new boolean[nx * ny * nz];
        int i = 0;
        for (int x = 0; x < nx; x++) {
            for (int y = 0; y < ny; y++) {
                for (int z = 0; z < nz; z++) {
                    boolean a = first.isFull(first(m[0])[x], first(m[1])[y], first(m[2])[z]);
                    boolean b = second.isFull(second(m[0])[x], second(m[1])[y], second(m[2])[z]);
                    occupied[i++] = op.apply(a, b);
                }
            }
        }
        boolean cube = discreteCube(m[0]) && discreteCube(m[1]) && discreteCube(m[2]);
        VoxelShape shape = VoxelShape.build(new double[][] {coords(m[0]), coords(m[1]), coords(m[2])}, occupied, cube,
                new boolean[] {cubeRange(m[0]), cubeRange(m[1]), cubeRange(m[2])});
        if (shape.isEmpty()) {
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE + 1;
            return shape.withDiscreteBounds(new int[] {min, min, min}, new int[] {max, max, max});
        }
        return shape;
    }

    public static boolean joinIsNotEmpty(VoxelShape first, VoxelShape second, BooleanOp op) {
        if (op.apply(false, false)) throw new IllegalArgumentException();
        boolean firstEmpty = first.isEmpty(), secondEmpty = second.isEmpty();
        if (firstEmpty || secondEmpty) return op.apply(!firstEmpty, !secondEmpty);
        if (first == second) return op.apply(true, true);
        boolean onlyFirst = op.apply(true, false), onlySecond = op.apply(false, true);
        for (net.minecraft.core.Direction.Axis axis : net.minecraft.core.Direction.Axis.values()) {
            if (first.max(axis) < second.min(axis) - EPSILON || second.max(axis) < first.min(axis) - EPSILON) return onlyFirst || onlySecond;
        }
        Object[][] m = mergers(first, second, onlyFirst, onlySecond);
        int nx = coords(m[0]).length - 1, ny = coords(m[1]).length - 1, nz = coords(m[2]).length - 1;
        for (int x = 0; x < nx; x++) {
            for (int y = 0; y < ny; y++) {
                for (int z = 0; z < nz; z++) {
                    if (op.apply(first.isFull(first(m[0])[x], first(m[1])[y], first(m[2])[z]), second.isFull(second(m[0])[x], second(m[1])[y], second(m[2])[z]))) return true;
                }
            }
        }
        return false;
    }

    public interface DoubleLineConsumer {
        void consume(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    }

    /**
     * The merged boundaries of one axis, for each merged cell the cell of each shape it is in, and whether they are a
     * cube's and the merge a discrete cubes' one: {double[] coords, int[] first, int[] second, boolean[] {cubeRange,
     * discreteCube}} (an array, not a class of its own: the game has none by that name).
     */
    private static Object[] merger(double[] coords, int[] first, int[] second, boolean cubeRange, boolean discreteCube) {
        return new Object[] {coords, first, second, new boolean[] {cubeRange, discreteCube}};
    }

    private static double[] coords(Object[] m) {
        return (double[]) m[0];
    }

    private static int[] first(Object[] m) {
        return (int[]) m[1];
    }

    private static int[] second(Object[] m) {
        return (int[]) m[2];
    }

    private static boolean cubeRange(Object[] m) {
        return ((boolean[]) m[3])[0];
    }

    private static boolean discreteCube(Object[] m) {
        return ((boolean[]) m[3])[1];
    }

    private static Object[][] mergers(VoxelShape a, VoxelShape b, boolean onlyFirst, boolean onlySecond) {
        Object[] x = merge(1, a.coords[0], b.coords[0], a.cubeAxes[0], b.cubeAxes[0], onlyFirst, onlySecond);
        Object[] y = merge(coords(x).length - 1, a.coords[1], b.coords[1], a.cubeAxes[1], b.cubeAxes[1], onlyFirst, onlySecond);
        long multiplier = (long) (coords(x).length - 1) * (coords(y).length - 1);
        if (multiplier > Integer.MAX_VALUE) throw new UnsupportedOperationException("a voxel shape this large isn't modelled (horizon-port-neoforge)");
        Object[] z = merge((int) multiplier, a.coords[2], b.coords[2], a.cubeAxes[2], b.cubeAxes[2], onlyFirst, onlySecond);
        return new Object[][] {x, y, z};
    }

    private static Object[] merge(int multiplier, double[] a, double[] b, boolean aCube, boolean bCube, boolean onlyFirst, boolean onlySecond) {
        int na = a.length - 1, nb = b.length - 1;
        if (aCube && bCube) {
            int gcd = gcd(na, nb);
            long lcm = (long) (na / gcd) * nb;
            if ((long) multiplier * lcm <= 256) {
                int n = (int) lcm;
                double[] coords = new double[n + 1];
                for (int i = 0; i <= n; i++) coords[i] = (double) i / (double) n;
                int[] first = new int[n], second = new int[n];
                for (int i = 0; i < n; i++) {
                    first[i] = i / (nb / gcd);
                    second[i] = i / (na / gcd);
                }
                return merger(coords, first, second, true, true);
            }
        }
        if (a[na] < b[0] - EPSILON) return disjoint(a, b, false);
        if (b[nb] < a[0] - EPSILON) return disjoint(b, a, true);
        if (a.length == b.length) {
            boolean same = true;
            for (int i = 0; i < a.length && same; i++) same = a[i] == b[i];
            if (same) {
                int[] cells = new int[na];
                for (int i = 0; i < na; i++) cells[i] = i;
                return merger(a.clone(), cells, cells.clone(), aCube, false);
            }
        }
        double[] coords = new double[a.length + b.length];
        int[] first = new int[a.length + b.length], second = new int[a.length + b.length];
        int n = 0, i = 0, j = 0;
        double last = 0.0 / 0.0;
        while (i < a.length || j < b.length) {
            boolean aEnd = i == a.length, bEnd = j == b.length;
            boolean chooseA = !aEnd && (bEnd || a[i] < b[j] + EPSILON);
            if (chooseA) {
                i++;
                if (!onlyFirst && (j == 0 || bEnd)) continue;
            } else {
                j++;
                if (!onlySecond && (i == 0 || aEnd)) continue;
            }
            double value = chooseA ? a[i - 1] : b[j - 1];
            if (!(last >= value - EPSILON)) {
                coords[n] = value;
                first[n] = i - 1;
                second[n] = j - 1;
                n++;
                last = value;
            } else {
                first[n - 1] = i - 1;
                second[n - 1] = j - 1;
            }
        }
        if (n <= 1) return merger(new double[] {0.0}, new int[0], new int[0], false, false);
        return merger(java.util.Arrays.copyOf(coords, n), java.util.Arrays.copyOf(first, n - 1), java.util.Arrays.copyOf(second, n - 1), false, false);
    }

    private static Object[] disjoint(double[] lower, double[] upper, boolean swap) {
        int nl = lower.length, nu = upper.length;
        int[] first = new int[nl + nu - 1], second = new int[nl + nu - 1];
        for (int i = 0; i < nl; i++) {
            first[i] = i;
            second[i] = -1;
        }
        for (int i = 0; i < nu - 1; i++) {
            first[nl + i] = nl - 1;
            second[nl + i] = i;
        }
        double[] coords = new double[nl + nu];
        System.arraycopy(lower, 0, coords, 0, nl);
        System.arraycopy(upper, 0, coords, nl, nu);
        return swap ? merger(coords, second, first, false, false) : merger(coords, first, second, false, false);
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int r = a % b;
            a = b;
            b = r;
        }
        return a;
    }
}
