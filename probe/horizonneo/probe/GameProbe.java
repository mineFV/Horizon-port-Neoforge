package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The same calls on the game's classes, as one program: `standins` compiles it once against a NeoForge server's jars
 * and once against the translator's stand-ins (game/), runs both and compares what they print. It observes what the
 * game answers; it never reads the game's code. Only the API both have is called.
 */
public final class GameProbe {
    private GameProbe() {}

    static final int MAX = Integer.MAX_VALUE, MIN = Integer.MIN_VALUE;
    static final int[][] PLACES = {
        {0, 0, 0}, {1, 2, 3}, {-1, -2, -3}, {5, 64, -7}, {3, 64, -7}, {5, 60, -7}, {5, 64, 9},
        {MAX, 0, 0}, {MIN, 0, 0}, {0, MAX, 0}, {0, MIN, 0}, {0, 0, MAX}, {0, 0, MIN},
        {30000000, 319, -30000000}, {-30000000, -64, 30000000}, {-33554432, -2048, 33554431}, {33554432, 2048, -33554433},
    };

    public static void main(String[] args) {
        for (Direction d : Direction.values()) {
            String k = "direction " + d.ordinal();
            p(k + " name", d::name);
            p(k + " toString", d::toString);
            p(k + " serializedName", d::getSerializedName);
            p(k + " opposite", () -> d.getOpposite().ordinal());
            p(k + " steps", () -> d.getStepX() + " " + d.getStepY() + " " + d.getStepZ());
        }
        BlockPos[] places = new BlockPos[PLACES.length];
        for (int i = 0; i < PLACES.length; i++) places[i] = new BlockPos(PLACES[i][0], PLACES[i][1], PLACES[i][2]);
        for (int i = 0; i < places.length; i++) {
            BlockPos a = places[i];
            String k = "place " + i;
            p(k + " toString", a::toString);
            p(k + " hashCode", a::hashCode);
            p(k + " asLong", a::asLong);
            p(k + " of", () -> BlockPos.of(a.asLong()).toString());
            p(k + " unpacked", () -> BlockPos.getX(a.asLong()) + " " + BlockPos.getY(a.asLong()) + " " + BlockPos.getZ(a.asLong()));
            p(k + " steps", () -> a.above() + " " + a.below() + " " + a.north() + " " + a.south() + " " + a.west() + " " + a.east());
            for (Direction d : Direction.values()) p(k + " relative " + d.ordinal(), () -> a.relative(d).toString());
            p(k + " equals null", () -> a.equals(null));
            p(k + " equals text", () -> a.equals("BlockPos"));
            for (int j = 0; j < places.length; j++) {
                BlockPos b = places[j];
                p(k + " compareTo " + j, () -> a.compareTo(b));
                p(k + " equals " + j, () -> a.equals(b));
                p(k + " equals copy " + j, () -> a.equals(new BlockPos(b.getX(), b.getY(), b.getZ())));
            }
        }
    }

    /** One observation: its key and what the call gave, or the class of what it threw. */
    static void p(String key, Supplier<Object> call) {
        String shown;
        try {
            shown = String.valueOf(call.get());
        } catch (RuntimeException | LinkageError e) {
            shown = "throws " + e.getClass().getName();
        }
        System.out.println(key + " -> " + shown);
    }
}
