package horizonneo.probe;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The game's small value classes and enums, as GameProbe observes the rest: chunk positions (packed, hashed, written,
 * their bounds, regions and distances, on coordinates at random and at the ints' bounds), what an interaction came to,
 * a two-block block's halves and a bed's parts, and fastutil's consumer of booleans.
 */
final class ValueProbe {
    private ValueProbe() {}

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }

    /** What a chunk position says of itself, in one text. */
    static String chunk(ChunkPos c) {
        return c + " " + c.x + " " + c.z + " " + c.toLong() + " " + c.hashCode() + " " + ChunkPos.hash(c.x, c.z) + " " + c.getMinBlockX() + " " + c.getMinBlockZ() + " " + c.getMaxBlockX()
                + " " + c.getMaxBlockZ() + " " + c.getMiddleBlockX() + " " + c.getMiddleBlockZ() + " " + c.getBlockX(3) + " " + c.getBlockZ(-7) + " " + c.getRegionX() + " " + c.getRegionZ()
                + " " + c.getRegionLocalX() + " " + c.getRegionLocalZ() + " " + c.getWorldPosition() + " " + ChunkPos.getX(c.toLong()) + " " + ChunkPos.getZ(c.toLong());
    }

    /** BlockStateProperties' halves and parts (it needs the game's registries: in a server only). */
    static void properties() {
        p("block half properties", () -> new StringBuilder().append(BlockStateProperties.OPEN).append(' ').append(BlockStateProperties.DOUBLE_BLOCK_HALF).append(' ')
                .append(BlockStateProperties.BED_PART).append(' ').append(BlockStateProperties.DOUBLE_BLOCK_HALF.getPossibleValues()).append(BlockStateProperties.BED_PART.getValue("foot"))
                .append(BlockStateProperties.BED_PART.getValue("x")).append(BlockStateProperties.OPEN.getName()).append(BlockStateProperties.DOUBLE_BLOCK_HALF.getValueClass().getName())
                .append(' ').append(BlockStateProperties.DOUBLE_BLOCK_HALF == BlockStateProperties.DOUBLE_BLOCK_HALF));
    }

    static void run() {
        int[] edges = {0, 1, -1, 15, 16, -16, -17, 31, 32, -33, 1875066, -1875066, 134217727, -134217728, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int x : edges) {
            p("chunk pos " + x, () -> {
                StringBuilder b = new StringBuilder();
                for (int z : edges) b.append(chunk(new ChunkPos(x, z))).append(" | ");
                return b;
            });
        }
        p("chunk pos random", () -> {
            java.util.Random r = new java.util.Random(11);
            long h = 1;
            for (int i = 0; i < 20000; i++) {
                ChunkPos c = new ChunkPos(r.nextInt(), r.nextInt());
                ChunkPos d = new ChunkPos(r.nextInt(2000) - 1000, r.nextInt(2000) - 1000);
                h = 31 * h + chunk(c).hashCode();
                h = 31 * h + chunk(d).hashCode();
                h = 31 * h + c.getChessboardDistance(d) + 7L * c.distanceSquared(d) + 13L * d.distanceSquared(c.toLong()) + 17L * d.getChessboardDistance(c.x, c.z);
            }
            return h;
        });
        p("chunk pos made", () -> {
            StringBuilder b = new StringBuilder();
            for (int v : new int[] {0, 15, 16, -1, -16, -17, 29999999, -29999999}) {
                BlockPos pos = new BlockPos(v, v / 3, -v);
                b.append(new ChunkPos(pos)).append(ChunkPos.asLong(pos)).append(' ').append(ChunkPos.asLong(v, -v)).append(' ').append(new ChunkPos(ChunkPos.asLong(v, -v))).append(' ');
            }
            ChunkPos a = new ChunkPos(3, -4);
            return b.append(ChunkPos.INVALID_CHUNK_POS).append(' ').append(ChunkPos.ZERO).append(' ').append(ChunkPos.REGION_SIZE).append(ChunkPos.REGION_MAX_INDEX).append(' ')
                    .append(a.equals(new ChunkPos(3, -4))).append(a.equals(new ChunkPos(-4, 3))).append(a.equals("[3, -4]")).append(a.equals(null)).append(' ')
                    .append(new ChunkPos(-1L)).append(new ChunkPos(Long.MIN_VALUE)).append(new ChunkPos(0x0000000100000002L));
        });
        p("interaction results", () -> {
            StringBuilder b = new StringBuilder();
            for (InteractionResult r : InteractionResult.values()) b.append(r).append(r.ordinal()).append(r.consumesAction()).append(r.shouldSwing()).append(r.indicateItemUse()).append(' ');
            return b.append(InteractionResult.sidedSuccess(true)).append(InteractionResult.sidedSuccess(false)).append(InteractionResult.valueOf("PASS"));
        });
        p("block halves", () -> {
            StringBuilder b = new StringBuilder();
            for (DoubleBlockHalf h : DoubleBlockHalf.values()) b.append(h.name()).append(h).append(h.getSerializedName()).append(h.getDirectionToOther()).append(h.getOtherHalf()).append(' ');
            for (BedPart h : BedPart.values()) b.append(h.name()).append(h).append(h.getSerializedName()).append(' ');
            return b;
        });
        p("boolean consumer", () -> {
            StringBuilder b = new StringBuilder();
            BooleanConsumer c = v -> b.append(v ? 'T' : 'F');
            c.accept(true);
            c.accept(Boolean.FALSE);
            c.andThen((BooleanConsumer) v -> b.append(v ? 't' : 'f')).accept(true);
            c.andThen((java.util.function.Consumer<Boolean>) v -> b.append('<').append(v).append('>')).accept(false);
            String npe;
            try {
                c.accept((Boolean) null);
                npe = "no";
            } catch (NullPointerException e) {
                npe = "npe";
            }
            String after;
            try {
                c.andThen((BooleanConsumer) null);
                after = "no";
            } catch (NullPointerException e) {
                after = "npe";
            }
            return b + " " + npe + " " + after;
        });
    }
}
