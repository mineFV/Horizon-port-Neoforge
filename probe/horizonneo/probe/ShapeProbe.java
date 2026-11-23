package horizonneo.probe;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The game's voxel shapes, as GameProbe observes the rest (see there): shapes made of boxes (on a cube's grid or
 * not, thin, outside the block, NaN), their joins by each kind of operation, optimized and moved, unions of boxes
 * drawn at random and turned about the vertical as a mod turns its blocks' shapes (Mekanism's VoxelShapeUtils), each
 * shape's class, bounds, boxes and text; the operations' truth tables; the rotations.
 */
final class ShapeProbe {
    private ShapeProbe() {}

    static void run() {
        VoxelShape[] shapes = {
            Shapes.empty(), Shapes.block(), Shapes.box(0, 0, 0, 1, 0.5, 1), Shapes.box(0.25, 0, 0.25, 0.75, 1, 0.75),
            Shapes.box(0.0625, 0.125, 0, 0.9375, 0.875, 1), Shapes.box(0.1, 0.2, 0.3, 0.4, 0.5, 0.6), Shapes.box(0, 0, 0, 1, 1e-8, 1),
            Shapes.box(0, 0, 0, 1, 1.0000001, 1), Shapes.box(-0.5, 0, 0, 0.5, 1, 1), Shapes.box(0.5, 0.5, 0.5, 2, 2, 2),
            Shapes.box(0, 0, 0, 0.125, 0.125, 0.125), Shapes.box(0.375, 0.375, 0.375, 0.625, 0.625, 0.625), Shapes.box(1e-8, 0.875, -1e-8, 0.12500001, 1, 9e-8),
            Shapes.create(new AABB(0.2, 0.2, 0.2, 0.8, 0.8, 0.8)), Shapes.box(0, 0, 0, 0.5, 1, 1).move(0.5, 0, 0), Shapes.block().move(0, 0, 0),
            Shapes.block().move(-1, 2.5, 0.1), Shapes.empty().move(1, 1, 1), Shapes.box(0, 0, 0, 1, 0.1875, 1), Shapes.box(0.3125, 0.1875, 0.3125, 0.6875, 1, 0.6875),
        };
        for (int i = 0; i < shapes.length; i++) {
            VoxelShape s = shapes[i];
            p("shape " + i, () -> describe(s));
            p("shape " + i + " optimized", () -> describe(s.optimize()));
        }
        p("shape nan", () -> describe(Shapes.create(Double.NaN, 0, 0, 1, 1, 1)));
        p("shape infinite", () -> describe(Shapes.create(Double.NEGATIVE_INFINITY, 0, 0, Double.POSITIVE_INFINITY, 1, 1)));
        p("shape box wrong way", () -> describe(Shapes.box(0.5, 0, 0, 0.4, 1, 1)));
        p("shape same", () -> (Shapes.box(0, 0, 0, 1, 1, 1) == Shapes.block()) + " " + (Shapes.create(0, 0, 0, 1, 1e-8, 1) == Shapes.empty()) + " "
                + (Shapes.box(0, 0, 0, 0.5, 1, 1) == Shapes.box(0, 0, 0, 0.5, 1, 1)));
        BooleanOp[] ops = {BooleanOp.OR, BooleanOp.AND, BooleanOp.ONLY_FIRST, BooleanOp.ONLY_SECOND, BooleanOp.NOT_SAME};
        String[] names = {"or", "and", "first", "second", "xor"};
        int[] picks = {0, 1, 2, 3, 4, 5, 8, 9, 11, 12, 13, 14, 18, 19};
        for (int i : picks) {
            for (int j : picks) {
                for (int k = 0; k < ops.length; k++) {
                    VoxelShape a = shapes[i], b = shapes[j];
                    BooleanOp op = ops[k];
                    p("join " + i + " " + j + " " + names[k], () -> {
                        VoxelShape raw = Shapes.joinUnoptimized(a, b, op);
                        return describe(raw) + " | " + describe(raw.optimize()) + " | " + describe(Shapes.join(a, b, op)) + " | " + Shapes.joinIsNotEmpty(a, b, op)
                                + " " + (raw == a) + (raw == b) + (raw == Shapes.empty());
                    });
                }
            }
        }
        p("join same", () -> describe(Shapes.joinUnoptimized(shapes[2], shapes[2], BooleanOp.OR)) + " " + (Shapes.joinUnoptimized(shapes[2], shapes[2], BooleanOp.OR) == shapes[2])
                + " " + (Shapes.joinUnoptimized(shapes[2], shapes[2], BooleanOp.ONLY_FIRST) == Shapes.empty()) + " " + Shapes.joinIsNotEmpty(shapes[2], shapes[2], BooleanOp.NOT_SAME));
        p("join unbounded", () -> describe(Shapes.joinUnoptimized(shapes[1], shapes[2], BooleanOp.TRUE)));
        p("join is not empty unbounded", () -> Shapes.joinIsNotEmpty(shapes[1], shapes[2], BooleanOp.NOT_OR));
        p("or many", () -> describe(Shapes.or(shapes[2], shapes[3], shapes[4], shapes[11])) + " | " + describe(Shapes.or(shapes[5])));
        for (int i : new int[] {1, 2, 4, 5, 9, 14}) {
            VoxelShape s = shapes[i];
            p("move " + i, () -> describe(s.move(0.25, -1, 3)) + " | " + describe(s.move(0, 0, 0)) + " | " + describe(s.move(-0.0, 1e-9, 1e300)));
        }
        // Unions of boxes drawn at random, and the same turned as Mekanism turns its shapes.
        long x = 0x5DEECE66DL;
        double[] grid = {0, 0.0625, 0.125, 0.1875, 0.25, 0.3125, 0.375, 0.5, 0.625, 0.6875, 0.75, 0.8125, 0.875, 0.9375, 1, 0.1, 0.3, 0.7, 0.33, -0.25, 1.25};
        for (int c = 0; c < 300; c++) {
            int n = 1 + c % 4;
            double[][] boxes = new double[n][];
            for (int k = 0; k < n; k++) {
                double[] v = new double[6];
                for (int d = 0; d < 3; d++) {
                    x = x * 6364136223846793005L + 1442695040888963407L;
                    double u = grid[(int) ((x >>> 33) % grid.length)];
                    x = x * 6364136223846793005L + 1442695040888963407L;
                    double w = grid[(int) ((x >>> 33) % grid.length)];
                    v[d] = Math.min(u, w);
                    v[d + 3] = Math.max(u, w);
                }
                boxes[k] = v;
            }
            p("random " + c, () -> {
                VoxelShape s = Shapes.empty();
                for (double[] v : boxes) s = Shapes.or(s, Shapes.box(v[0], v[1], v[2], v[3], v[4], v[5]));
                return describe(s) + " | " + describe(turned(s));
            });
        }
        for (int t = 0; t < 16; t++) {
            int table = t;
            p("op " + t, () -> {
                BooleanOp[] all = {BooleanOp.FALSE, BooleanOp.NOT_OR, BooleanOp.ONLY_SECOND, BooleanOp.NOT_FIRST, BooleanOp.ONLY_FIRST, BooleanOp.NOT_SECOND,
                    BooleanOp.NOT_SAME, BooleanOp.NOT_AND, BooleanOp.AND, BooleanOp.SAME, BooleanOp.SECOND, BooleanOp.CAUSES, BooleanOp.FIRST,
                    BooleanOp.CAUSED_BY, BooleanOp.OR, BooleanOp.TRUE};
                BooleanOp op = all[table];
                return "" + op.apply(false, false) + op.apply(false, true) + op.apply(true, false) + op.apply(true, true);
            });
        }
        p("rotations", () -> {
            StringBuilder b = new StringBuilder();
            for (Rotation r : Rotation.values()) {
                b.append(r).append(' ').append(r.getSerializedName()).append(' ').append(r.ordinal()).append(':');
                for (Rotation q : Rotation.values()) b.append(' ').append(r.getRotated(q));
                for (Direction d : Direction.values()) b.append(' ').append(r.rotate(d));
                for (int count : new int[] {4, 16, 6}) {
                    for (int index = 0; index < count; index++) b.append(' ').append(r.rotate(index, count));
                }
                b.append("; ");
            }
            return b.append(Rotation.valueOf("CLOCKWISE_180"));
        });
    }

    /** A shape turned a quarter about the vertical, as Mekanism's VoxelShapeUtils does: each box turned, joined, optimized. */
    static VoxelShape turned(VoxelShape shape) {
        VoxelShape result = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            VoxelShape part = Shapes.create(new AABB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX));
            result = Shapes.joinUnoptimized(result, part, BooleanOp.OR);
        }
        return result.optimize();
    }

    static String describe(VoxelShape s) {
        StringBuilder b = new StringBuilder(s.getClass().getSimpleName()).append(' ').append(s.isEmpty());
        for (Direction.Axis a : Direction.Axis.values()) b.append(' ').append(s.min(a)).append('/').append(s.max(a));
        String bounds;
        try {
            bounds = String.valueOf(s.bounds());
        } catch (RuntimeException e) {
            bounds = "throws " + e.getClass().getName() + ": " + e.getMessage();
        }
        return b.append(' ').append(bounds).append(' ').append(s.toAabbs()).append(' ').append(s).append(' ')
                .append(s == Shapes.empty()).append(s == Shapes.block()).toString();
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
