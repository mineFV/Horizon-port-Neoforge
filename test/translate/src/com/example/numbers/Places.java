package com.example.numbers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

/** Made up: the game's positions and directions, through the translator's stand-ins (game/). */
public final class Places {
    private Places() {}

    public static long packed(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }

    public static int unpackedX(long p) {
        return BlockPos.getX(p);
    }

    public static int unpackedY(long p) {
        return BlockPos.getY(p);
    }

    public static int unpackedZ(long p) {
        return BlockPos.getZ(p);
    }

    public static int opposite(int d) {
        return Direction.values()[d].getOpposite().ordinal();
    }

    public static int steps(int d) {
        Direction dir = Direction.values()[d];
        return dir.getStepX() * 100 + dir.getStepY() * 10 + dir.getStepZ();
    }

    public static int nameLength(int d) {
        return Direction.values()[d].getSerializedName().length();
    }

    /** From a packed position, steps along the axes (d's bits say which), then up: packed again. */
    public static long walk(long p, int d) {
        BlockPos at = BlockPos.of(p);
        if ((d & 1) != 0) at = at.east();
        if ((d & 2) != 0) at = at.north();
        if ((d & 4) != 0) at = at.below().west().south();
        return at.above().asLong();
    }

    /** Positions as keys: a HashMap of them, in the JDK's order, and their texts. */
    public static String keys(int n) {
        java.util.Map<BlockPos, Integer> m = new java.util.HashMap<>();
        for (int i = 0; i < n; i++) m.merge(new BlockPos(i % 5, i * 7 - 20, -i), i, Integer::sum);
        m.merge(new BlockPos(0, -20, 0), 100, Integer::sum);
        return m.toString();
    }

    /**
     * Positions sorted (Vec3i.compareTo) and a direction's text, joined. Coordinates of a world: with extremes the
     * subtraction wraps and the order isn't one, which the model's sort can tell (lib/README.md).
     */
    public static String sorted(int d) {
        java.util.List<BlockPos> l = new java.util.ArrayList<>();
        int[] c = {0, 7, -3, 30000000, -30000000, 64};
        for (int i = 0; i < c.length; i++) l.add(new BlockPos(c[(i + d) % c.length], c[(i * 2 + d) % c.length], c[(i * 5) % c.length]));
        java.util.Collections.sort(l);
        return Direction.values()[d] + " " + l;
    }

    /** compareTo itself, extremes included: the first coordinate that differs, subtracted with wrapping. */
    public static int compare(int a, int b) {
        return new BlockPos(a, 1, b).compareTo(new BlockPos(b, 1, a)) + 3 * new BlockPos(0, a, 0).compareTo(new BlockPos(0, b, 0));
    }

    /** The rest of the stand-ins' API: a MutableBlockPos moved in place, axes, turns (and their exceptions), texts. */
    public static String more(int n) {
        Direction d = Direction.from3DDataValue(n);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(n, -n, n * 3);
        StringBuilder s = new StringBuilder();
        s.append(m).append(' ').append(m.move(d)).append(' ').append(m.immutable()).append(' ').append(m.offset(0, 0, 0) == m);
        s.append(' ').append(m.setWithOffset(BlockPos.ZERO, d.getOpposite())).append(' ').append(m.hashCode());
        s.append(' ').append(d.getAxis()).append(' ').append(d.getAxis().choose(1.5, -2.5, n)).append(' ').append(d.getAxisDirection());
        s.append(' ').append(d.getNormal()).append(' ').append(d.getNormal() == d.getNormal()).append(' ').append(d.toYRot());
        try {
            s.append(' ').append(d.getClockWise()).append(' ').append(d.getCounterClockWise());
        } catch (IllegalStateException e) {
            s.append(" throws ").append(e.getMessage());
        }
        s.append(' ').append(Direction.getNearest(n - 3.5f, 1f, -n)).append(' ').append(BlockPos.containing(n * -0.75, 1e300, n / 3.0));
        s.append(' ').append(new BlockPos(1, 2, 3) {}).append(' ').append(new BlockPos(n, 0, 0).distSqr(new Vec3i(0, n, 1)));
        s.append(' ').append(new BlockPos(n, n, n).relative(d, n).below(n).above(0));
        return s.toString();
    }

    /** NBT: a compound written, copied, merged and read back, with its text (SNBT) and hash. */
    public static String nbt(int n) {
        net.minecraft.nbt.CompoundTag c = new net.minecraft.nbt.CompoundTag();
        c.putInt("ticks", n * 7919);
        c.putLong("energy", (long) n << 33);
        c.putDouble("heat", n / 3.0);
        c.putFloat("ratio", n * 0.1f);
        c.putBoolean("active", n % 2 == 0);
        c.putString(n % 3 == 0 ? "na me" : "name", "machine \"" + n + "'");
        c.putUUID("owner", new java.util.UUID(n * 31L, -n));
        c.put("at", net.minecraft.nbt.NbtUtils.writeBlockPos(new BlockPos(n, -n, 2 * n)));
        net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < n % 4; i++) {
            net.minecraft.nbt.CompoundTag slot = new net.minecraft.nbt.CompoundTag();
            slot.putByte("Slot", (byte) i);
            slot.putShort("count", (short) (i * 300));
            l.add(slot);
        }
        c.put("slots", l);
        for (int i = 0; i < n; i++) c.putInt("k" + (i * 13 % 17), i);
        net.minecraft.nbt.CompoundTag d = c.copy();
        net.minecraft.nbt.CompoundTag extra = new net.minecraft.nbt.CompoundTag();
        extra.putIntArray("ticks", new int[] {n, n + 1});
        d.merge(extra);
        return c + " " + c.getAllKeys() + " " + c.hashCode() + " " + d.getInt("ticks") + " " + c.getInt("ticks") + " " + c.getUUID("owner")
                + " " + net.minecraft.nbt.NbtUtils.readBlockPos(c, "at") + " " + c.getList("slots", 10).getCompound(1) + " " + c.getBoolean("active")
                + " " + c.getFloat("heat") + " " + c.getLong("ratio") + " " + c.equals(d) + " " + d.get("ticks");
    }

    static final String[] IDS = {"mekanism:steel_casing", "stone", ":b", "a:b:c", "Bad Id", "x/y.z", "é", "tab\t", ""};

    /** Ids: parsed, built, compared, hashed and their errors' messages; ByIdMap's strategies. */
    public static String ids(int n) {
        String text = IDS[Math.floorMod(n, IDS.length)];
        StringBuilder b = new StringBuilder();
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(text);
        b.append(id).append(' ');
        try {
            net.minecraft.resources.ResourceLocation p = net.minecraft.resources.ResourceLocation.parse(text);
            b.append(p.hashCode()).append(' ').append(p.toLanguageKey("block")).append(' ').append(p.toShortLanguageKey()).append(' ').append(p.withSuffix("_" + n));
            b.append(' ').append(p.compareTo(net.minecraft.resources.ResourceLocation.withDefaultNamespace("stone"))).append(' ').append(p.toDebugFileName());
        } catch (net.minecraft.ResourceLocationException e) {
            b.append(e.getMessage());
        }
        net.minecraft.util.ByIdMap.OutOfBoundsStrategy[] all = net.minecraft.util.ByIdMap.OutOfBoundsStrategy.values();
        java.util.function.IntFunction<net.minecraft.util.ByIdMap.OutOfBoundsStrategy> f =
                net.minecraft.util.ByIdMap.continuous(Enum::ordinal, all, all[Math.floorMod(n, 3)]);
        return b.append(' ').append(f.apply(n)).append(' ').append(f.apply(-n)).toString();
    }

    /** The game's Mth on the bounds and between them. */
    public static String mth(int n) {
        double d = n * 0.75 - (n % 3 == 0 ? 1e10 : 0) + (n % 5 == 0 ? Double.NaN : 0);
        float f = (float) (n * 37.5);
        return net.minecraft.util.Mth.floor(d) + " " + net.minecraft.util.Mth.ceil(d) + " " + net.minecraft.util.Mth.floor(f) + " " + net.minecraft.util.Mth.ceil(f)
                + " " + net.minecraft.util.Mth.clamp(n * 3, -10, 10) + " " + net.minecraft.util.Mth.clamp(d, -2.0, 2.0) + " " + net.minecraft.util.Mth.clamp(f, 10f, -10f)
                + " " + net.minecraft.util.Mth.wrapDegrees(f) + " " + net.minecraft.util.Mth.rotLerp(0.25f, f, -f) + " " + net.minecraft.util.Mth.lerp(0.3, d, n)
                + " " + net.minecraft.util.Mth.positiveCeilDiv(n * 7, n % 4 == 0 ? 3 : n % 4) + " " + net.minecraft.util.Mth.lerpDiscrete(n / 10f, -n, n * n)
                + " " + net.minecraft.util.Mth.length(d, f) + " " + net.minecraft.util.Mth.sqrt(f) + " " + net.minecraft.util.Mth.invSqrt(n) + " " + net.minecraft.util.Mth.equal(d, d + 1e-6)
                + " " + net.minecraft.util.Mth.SQRT_OF_TWO;
    }

    public static int eastX(int x) {
        return new BlockPos(x, 0, 0).east().getX();
    }
}
