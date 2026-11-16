package net.minecraft.nbt;

import java.util.Optional;
import net.minecraft.core.BlockPos;

/** The stand-in for the game's NbtUtils: what Mekanism uses of it, observed (`standins`). */
public final class NbtUtils {
    private NbtUtils() {}

    /** Observed: an IntArrayTag of x, y, z. */
    public static Tag writeBlockPos(BlockPos pos) {
        return new IntArrayTag(new int[] {pos.getX(), pos.getY(), pos.getZ()});
    }

    /** Observed: the position when the key holds three ints, else empty. */
    public static Optional<BlockPos> readBlockPos(CompoundTag tag, String key) {
        int[] a = tag.getIntArray(key);
        return a.length == 3 ? Optional.of(new BlockPos(a[0], a[1], a[2])) : Optional.empty();
    }
}
