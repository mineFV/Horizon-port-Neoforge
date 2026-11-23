package net.minecraft.world.phys.shapes;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * The stand-in for the game's VoxelShape: a grid of cells over exact coordinates, each full or not, its class the
 * game's (CubeVoxelShape over a cube's grid, ArrayVoxelShape else). After Horizon's horizon_core::geometry::shape
 * (MIT, tested against vanilla there): its grid, discrete bounds, joins (Shapes), optimize and the greedy boxes
 * forAllBoxes and toAabbs give. Observed in a server (`standins`, probe/ShapeProbe.java). Not here: getFaceShape and
 * clip (they refuse), the edges, collide, closestPointTo, singleEncompassing, getCoords.
 */
public abstract class VoxelShape {
    /** The boundaries along each axis: a grid of (length - 1) cells. */
    final double[][] coords;
    final int[] sizes;
    /** The full cells, by (x * ny + y) * nz + z. */
    final long[] cells;
    /** The axes whose boundaries are a cube's, i / size (a merge of two keeps that). */
    final boolean[] cubeAxes;
    final int[] discreteMin;
    final int[] discreteMax;
    /** The greedy boxes: minX, minY, minZ, maxX, maxY, maxZ each. */
    final double[][] boxes;

    VoxelShape(double[][] coords, long[] cells, boolean[] cubeAxes, int[] discreteMin, int[] discreteMax, double[][] boxes) {
        this.coords = coords;
        this.sizes = new int[] {coords[0].length - 1, coords[1].length - 1, coords[2].length - 1};
        this.cells = cells;
        this.cubeAxes = cubeAxes;
        this.discreteMin = discreteMin;
        this.discreteMax = discreteMax;
        this.boxes = boxes;
    }

    /** A grid's shape: its discrete bounds from its full cells, its boxes found once. */
    static VoxelShape build(double[][] coords, boolean[] occupied, boolean cube, boolean[] cubeAxes) {
        int nx = coords[0].length - 1, ny = coords[1].length - 1, nz = coords[2].length - 1;
        long count = (long) nx * ny * nz;
        if (count > 1 << 20) throw new UnsupportedOperationException("a voxel shape of more than 2^20 cells isn't modelled (horizon-port-neoforge)");
        long[] cells = new long[(int) ((count + 63) / 64)];
        int[] min = {nx, ny, nz};
        int[] max = {0, 0, 0};
        for (int i = 0; i < occupied.length; i++) {
            if (!occupied[i]) continue;
            cells[i >> 6] |= 1L << (i & 63);
            int[] p = {i / (ny * nz), i / nz % ny, i % nz};
            for (int a = 0; a < 3; a++) {
                min[a] = Math.min(min[a], p[a]);
                max[a] = Math.max(max[a], p[a] + 1);
            }
        }
        double[][] boxes = decompose(coords, cells, nx, ny, nz);
        return cube ? new CubeVoxelShape(coords, cells, cubeAxes, min, max, boxes) : new ArrayVoxelShape(coords, cells, cubeAxes, min, max, boxes);
    }

    /** A cube's grid of these sizes: boundaries i / size. */
    static VoxelShape cube(int[] sizes, boolean[] occupied) {
        double[][] coords = new double[3][];
        for (int a = 0; a < 3; a++) {
            coords[a] = new double[sizes[a] + 1];
            for (int i = 0; i <= sizes[a]; i++) coords[a][i] = (double) i / (double) sizes[a];
        }
        return build(coords, occupied, true, new boolean[] {true, true, true});
    }

    /** The same shape with these discrete bounds (an empty one keeps the box it was made of). */
    VoxelShape withDiscreteBounds(int[] min, int[] max) {
        return this instanceof CubeVoxelShape ? new CubeVoxelShape(coords, cells, cubeAxes, min, max, boxes) : new ArrayVoxelShape(coords, cells, cubeAxes, min, max, boxes);
    }

    static boolean full(long[] cells, int i) {
        return (cells[i >> 6] & (1L << (i & 63))) != 0;
    }

    boolean isFull(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= sizes[0] || y >= sizes[1] || z >= sizes[2]) return false;
        return full(cells, (x * sizes[1] + y) * sizes[2] + z);
    }

    /** The full cells as boxes: by y, then x, a run along z, widened along x, then along y. */
    private static double[][] decompose(double[][] coords, long[] cells, int nx, int ny, int nz) {
        long[] remaining = cells.clone();
        java.util.ArrayList<double[]> out = new java.util.ArrayList<>();
        for (int y = 0; y < ny; y++) {
            for (int x = 0; x < nx; x++) {
                int run = -1;
                for (int z = 0; z <= nz; z++) {
                    if (z < nz && full(remaining, (x * ny + y) * nz + z)) {
                        if (run < 0) run = z;
                        continue;
                    }
                    if (run < 0) continue;
                    int zStart = run;
                    run = -1;
                    clear(remaining, ny, nz, x, y, zStart, z);
                    int xEnd = x + 1;
                    while (xEnd < nx && strip(remaining, ny, nz, xEnd, y, zStart, z)) {
                        clear(remaining, ny, nz, xEnd, y, zStart, z);
                        xEnd++;
                    }
                    int yEnd = y + 1;
                    while (yEnd < ny) {
                        boolean all = true;
                        for (int u = x; u < xEnd && all; u++) all = strip(remaining, ny, nz, u, yEnd, zStart, z);
                        if (!all) break;
                        for (int u = x; u < xEnd; u++) clear(remaining, ny, nz, u, yEnd, zStart, z);
                        yEnd++;
                    }
                    out.add(new double[] {coords[0][x], coords[1][y], coords[2][zStart], coords[0][xEnd], coords[1][yEnd], coords[2][z]});
                }
            }
        }
        return out.toArray(new double[0][]);
    }

    private static boolean strip(long[] cells, int ny, int nz, int x, int y, int min, int max) {
        for (int z = min; z < max; z++) if (!full(cells, (x * ny + y) * nz + z)) return false;
        return true;
    }

    private static void clear(long[] cells, int ny, int nz, int x, int y, int min, int max) {
        for (int z = min; z < max; z++) {
            int i = (x * ny + y) * nz + z;
            cells[i >> 6] &= ~(1L << (i & 63));
        }
    }

    public double min(Direction.Axis axis) {
        int a = axis.ordinal();
        int index = discreteMin[a];
        return index >= sizes[a] ? 1.0 / 0.0 : coords[a][index];
    }

    public double max(Direction.Axis axis) {
        int a = axis.ordinal();
        int index = discreteMax[a];
        return index <= 0 ? -1.0 / 0.0 : coords[a][index];
    }

    public AABB bounds() {
        if (isEmpty()) throw new UnsupportedOperationException("No bounds for empty shape.");
        return new AABB(min(Direction.Axis.X), min(Direction.Axis.Y), min(Direction.Axis.Z), max(Direction.Axis.X), max(Direction.Axis.Y), max(Direction.Axis.Z));
    }

    public boolean isEmpty() {
        return boxes.length == 0;
    }

    /** Moved: an array shape over the moved boundaries, even for no offset; the empty shape stays itself. */
    public VoxelShape move(double x, double y, double z) {
        if (isEmpty()) return Shapes.empty();
        double[] offset = {x, y, z};
        double[][] moved = new double[3][];
        for (int a = 0; a < 3; a++) {
            moved[a] = coords[a].clone();
            for (int i = 0; i < moved[a].length; i++) moved[a][i] += offset[a];
        }
        double[][] movedBoxes = new double[boxes.length][];
        for (int i = 0; i < boxes.length; i++) {
            double[] b = boxes[i];
            movedBoxes[i] = new double[] {b[0] + x, b[1] + y, b[2] + z, b[3] + x, b[4] + y, b[5] + z};
        }
        return new ArrayVoxelShape(moved, cells, new boolean[3], discreteMin, discreteMax, movedBoxes);
    }

    /** Its boxes joined one by one, from nothing, each made a shape (Shapes.create). */
    public VoxelShape optimize() {
        VoxelShape result = Shapes.empty();
        for (double[] b : boxes) result = Shapes.joinUnoptimized(result, Shapes.create(b[0], b[1], b[2], b[3], b[4], b[5]), BooleanOp.OR);
        return result;
    }

    public void forAllBoxes(Shapes.DoubleLineConsumer action) {
        for (double[] b : boxes) action.consume(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    public java.util.List<AABB> toAabbs() {
        java.util.List<AABB> list = new java.util.ArrayList<>();
        forAllBoxes((x1, y1, z1, x2, y2, z2) -> list.add(new AABB(x1, y1, z1, x2, y2, z2)));
        return list;
    }

    /** Refused: a face's shape isn't modelled. */
    public VoxelShape getFaceShape(Direction direction) {
        throw new UnsupportedOperationException("a voxel shape's face isn't modelled (horizon-port-neoforge)");
    }

    /** Refused: tracing a line through a shape isn't modelled. */
    public net.minecraft.world.phys.BlockHitResult clip(net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to, net.minecraft.core.BlockPos pos) {
        throw new UnsupportedOperationException("tracing a line through a voxel shape isn't modelled (horizon-port-neoforge)");
    }

    /** Observed: "EMPTY", or its bounds. */
    @Override
    public String toString() {
        return isEmpty() ? "EMPTY" : "VoxelShape[" + bounds() + "]";
    }
}
