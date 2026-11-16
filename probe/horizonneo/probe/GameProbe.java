package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

/**
 * The same calls on the game's classes, as one program: `standins` compiles it once against a NeoForge server's jars
 * and once against the translator's stand-ins (game/), runs both and compares what they print. It observes what the
 * game answers; it never reads the game's code. Only the API both have is called. Where a method gives an object,
 * its class and whether it is one of the objects it was given are printed too: a mod can tell both.
 */
public final class GameProbe {
    private GameProbe() {}

    static final int MAX = Integer.MAX_VALUE, MIN = Integer.MIN_VALUE;
    static final int[][] PLACES = {
        {0, 0, 0}, {1, 2, 3}, {-1, -2, -3}, {5, 64, -7}, {3, 64, -7}, {5, 60, -7}, {5, 64, 9},
        {MAX, 0, 0}, {MIN, 0, 0}, {0, MAX, 0}, {0, MIN, 0}, {0, 0, MAX}, {0, 0, MIN},
        {30000000, 319, -30000000}, {-30000000, -64, 30000000}, {-33554432, -2048, 33554431}, {33554432, 2048, -33554433},
    };
    static final int[] STEPS = {0, 1, -1, 2, 5, -16, MAX, MIN};
    static final double[] DOUBLES = {0.0, -0.0, 0.5, -0.5, 0.999999, -0.000001, 1.0, -1.0, 2.5, -2.5, 63.99999999,
        -64.00000001, 1e9, -1e9, 2147483647.5, 2147483648.0, -2147483648.5, -2147483649.0, 1e300, -1e300,
        Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE};
    static final float[] FLOATS = {0f, -0f, 1f, -1f, 0.5f, -0.5f, 0.70710677f, -0.70710677f, 2f, 1e-30f, Float.NaN,
        Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};

    public static void main(String[] args) {
        directions();
        axes();
        BlockPos[] places = new BlockPos[PLACES.length];
        for (int i = 0; i < PLACES.length; i++) places[i] = new BlockPos(PLACES[i][0], PLACES[i][1], PLACES[i][2]);
        for (int i = 0; i < places.length; i++) place("place " + i, places[i], places);
        vectors(places);
        mutable(places);
        containing();
        subclasses();
        NbtProbe.run();
        IdProbe.run();
        MthProbe.run();
        VecProbe.run();
        EquipProbe.run();
        RegistryProbe.run();
    }

    static void directions() {
        p("direction values", () -> Direction.values().length + " " + (Direction.values() == Direction.values()));
        for (Direction d : Direction.values()) {
            String k = "direction " + d.ordinal();
            p(k + " name", d::name);
            p(k + " toString", d::toString);
            p(k + " serializedName", d::getSerializedName);
            p(k + " opposite", () -> d.getOpposite().ordinal());
            p(k + " steps", () -> d.getStepX() + " " + d.getStepY() + " " + d.getStepZ());
            p(k + " axis", () -> d.getAxis().ordinal());
            p(k + " axisDirection", () -> d.getAxisDirection().ordinal());
            p(k + " normal", () -> shown(d.getNormal()) + " same " + (d.getNormal() == d.getNormal()));
            p(k + " clockWise", () -> d.getClockWise().ordinal());
            p(k + " counterClockWise", () -> d.getCounterClockWise().ordinal());
            p(k + " 2D", d::get2DDataValue);
            p(k + " yRot", () -> Float.floatToIntBits(d.toYRot()));
            p(k + " class", () -> d.getClass().getName());
        }
        for (int i = -13; i <= 13; i++) {
            int v = i;
            p("from3D " + v, () -> Direction.from3DDataValue(v).ordinal());
        }
        for (float x : FLOATS) {
            for (float y : FLOATS) {
                for (float z : FLOATS) p("nearest " + x + " " + y + " " + z, () -> Direction.getNearest(x, y, z).ordinal());
            }
        }
    }

    static void axes() {
        p("axis values", () -> Direction.Axis.values().length + " " + (Direction.Axis.values() == Direction.Axis.values()));
        for (Direction.Axis a : Direction.Axis.values()) {
            String k = "axis " + a.ordinal();
            p(k + " name", a::name);
            p(k + " toString", a::toString);
            p(k + " serializedName", a::getSerializedName);
            p(k + " horizontal", a::isHorizontal);
            p(k + " vertical", a::isVertical);
            p(k + " class", () -> a.getClass().getName());
            p(k + " choose", () -> a.choose(1.5, -2.5, 3.25) + " " + a.choose(Double.NaN, -0.0, 7.0));
            p(k + " chooseInt", () -> a.choose(1, -2, 3));
            for (Direction d : Direction.values()) p(k + " test " + d.ordinal(), () -> a.test(d));
            p(k + " test null", () -> a.test(null));
        }
        for (Direction.AxisDirection a : Direction.AxisDirection.values()) {
            String k = "axisDirection " + a.ordinal();
            p(k + " name", a::name);
            p(k + " toString", a::toString);
            p(k + " step", a::getStep);
            p(k + " opposite", () -> a.opposite().ordinal());
        }
    }

    static void place(String k, BlockPos a, BlockPos[] places) {
        p(k + " toString", a::toString);
        p(k + " hashCode", a::hashCode);
        p(k + " asLong", a::asLong);
        p(k + " of", () -> BlockPos.of(a.asLong()).toString());
        p(k + " unpacked", () -> BlockPos.getX(a.asLong()) + " " + BlockPos.getY(a.asLong()) + " " + BlockPos.getZ(a.asLong()));
        p(k + " steps", () -> made(a, a.above()) + " " + made(a, a.below()) + " " + made(a, a.north()) + " " + made(a, a.south())
                + " " + made(a, a.west()) + " " + made(a, a.east()));
        p(k + " immutable", () -> made(a, a.immutable()));
        for (Direction d : Direction.values()) {
            p(k + " relative " + d.ordinal(), () -> made(a, a.relative(d)));
            for (int n : STEPS) p(k + " relative " + d.ordinal() + " by " + n, () -> made(a, a.relative(d, n)));
        }
        for (int n : STEPS) {
            p(k + " by " + n, () -> made(a, a.above(n)) + " " + made(a, a.below(n)) + " " + made(a, a.north(n)) + " "
                    + made(a, a.south(n)) + " " + made(a, a.west(n)) + " " + made(a, a.east(n)));
            for (int m : STEPS) p(k + " offset " + n + " " + m, () -> made(a, a.offset(n, m, -n)));
        }
        p(k + " equals null", () -> a.equals(null));
        p(k + " equals text", () -> a.equals("BlockPos"));
        for (int j = 0; j < places.length; j++) {
            BlockPos b = places[j];
            p(k + " compareTo " + j, () -> a.compareTo(b));
            p(k + " equals " + j, () -> a.equals(b));
            p(k + " equals copy " + j, () -> a.equals(new BlockPos(b.getX(), b.getY(), b.getZ())));
            p(k + " offset place " + j, () -> made(a, a.offset(b)));
            p(k + " subtract " + j, () -> made(a, a.subtract(b)));
            p(k + " distSqr " + j, () -> Double.doubleToLongBits(a.distSqr(b)));
        }
    }

    static void vectors(BlockPos[] places) {
        p("Vec3i.ZERO", () -> shown(Vec3i.ZERO) + " " + (Vec3i.ZERO == Vec3i.ZERO));
        p("BlockPos.ZERO", () -> shown(BlockPos.ZERO) + " " + (BlockPos.ZERO == (Object) Vec3i.ZERO));
        for (int i = 0; i < PLACES.length; i++) {
            Vec3i v = new Vec3i(PLACES[i][0], PLACES[i][1], PLACES[i][2]);
            String k = "vec " + i;
            p(k + " toString", () -> shown(v));
            p(k + " hashCode", v::hashCode);
            p(k + " xyz", () -> v.getX() + " " + v.getY() + " " + v.getZ());
            for (int j = 0; j < places.length; j++) {
                BlockPos b = places[j];
                Vec3i w = new Vec3i(b.getX(), b.getY(), b.getZ());
                p(k + " equals place " + j, () -> v.equals(b) + " " + b.equals(v) + " " + v.equals(w));
                p(k + " compareTo place " + j, () -> v.compareTo(b) + " " + b.compareTo(v));
                p(k + " place offset " + j, () -> made(b, b.offset(v)) + " " + made(b, b.subtract(v)) + " " + b.distSqr(v));
            }
        }
    }

    static void mutable(BlockPos[] places) {
        p("mutable new", () -> shown(new BlockPos.MutableBlockPos()));
        for (int i = 0; i < places.length; i++) {
            BlockPos b = places[i];
            String k = "mutable " + i;
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(b.getX(), b.getY(), b.getZ());
            p(k + " toString", () -> shown(m));
            p(k + " hashCode", m::hashCode);
            p(k + " asLong", m::asLong);
            p(k + " equals", () -> m.equals(b) + " " + b.equals(m));
            p(k + " compareTo", () -> m.compareTo(b) + " " + b.compareTo(m));
            p(k + " immutable", () -> made(m, m.immutable()));
            p(k + " steps", () -> made(m, m.above()) + " " + made(m, m.east()) + " " + made(m, m.relative(Direction.SOUTH)));
            for (int n : STEPS) {
                p(k + " by " + n, () -> made(m, m.above(n)) + " " + made(m, m.relative(Direction.WEST, n)));
                p(k + " offset " + n, () -> made(m, m.offset(n, 0, n)) + " " + made(m, m.offset(0, 0, 0)));
            }
            for (int j = 0; j < places.length; j++) {
                BlockPos c = places[j];
                p(k + " offset place " + j, () -> made(m, m.offset(c)) + " " + made(m, m.subtract(c)));
            }
            p(k + " set", () -> {
                BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
                StringBuilder s = new StringBuilder();
                s.append(made(n, n.set(b.getX(), b.getY(), b.getZ()))).append(' ');
                for (Direction d : Direction.values()) s.append(made(n, n.move(d))).append(' ');
                for (Direction d : Direction.values()) s.append(made(n, n.setWithOffset(b, d))).append(' ');
                s.append(made(n, n.setWithOffset(b, 3, -4, MAX))).append(' ');
                s.append(made(n, n.setWithOffset(b, places[(placeIndex(b, places) + 1) % places.length]))).append(' ');
                s.append(n.hashCode()).append(' ').append(n.asLong()).append(' ').append(n.getX()).append(' ').append(n.getY()).append(' ').append(n.getZ());
                return s;
            });
        }
    }

    /** A mod's own subclass of BlockPos, named and anonymous: their texts and equality. */
    static final class Marker extends BlockPos {
        Marker(int x, int y, int z) {
            super(x, y, z);
        }
    }

    static void subclasses() {
        BlockPos named = new Marker(1, 2, 3);
        BlockPos anonymous = new BlockPos(1, 2, 3) {};
        BlockPos plain = new BlockPos(1, 2, 3);
        p("subclass named", () -> named + " " + named.equals(plain) + " " + plain.equals(named) + " " + named.hashCode() + " " + made(named, named.above()));
        p("subclass anonymous", () -> anonymous + " " + anonymous.equals(named) + " " + made(anonymous, anonymous.immutable()) + " " + made(named, named.immutable()));
    }

    static void containing() {
        for (double x : DOUBLES) {
            for (double y : DOUBLES) p("containing " + x + " " + y, () -> shown(BlockPos.containing(x, y, -x)));
        }
    }

    static int placeIndex(BlockPos b, BlockPos[] places) {
        for (int i = 0; i < places.length; i++) if (places[i] == b) return i;
        return -1;
    }

    /** An object a method gave: its class, its text, and whether it is the one it was called on. */
    static String made(Object from, Object made) {
        return (made == from ? "this " : "") + shown(made);
    }

    static String shown(Object o) {
        return o == null ? "null" : o.getClass().getName() + ":" + o;
    }

    /** One observation: its key and what the call gave, or what it threw (its class and message). */
    static void p(String key, Supplier<Object> call) {
        String shown;
        try {
            shown = String.valueOf(call.get());
        } catch (RuntimeException | LinkageError e) {
            // A NullPointerException's message is the JVM's, built from the bytecode (its variables' names): the
            // stand-ins don't reproduce it (lib/README.md), so only its class is compared.
            shown = "throws " + e.getClass().getName() + (e instanceof NullPointerException ? "" : ": " + e.getMessage());
        }
        System.out.println(key + " -> " + shown);
    }
}
