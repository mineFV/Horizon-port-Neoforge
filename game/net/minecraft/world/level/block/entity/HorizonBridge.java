package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.HorizonRegistries;

/**
 * Not the game's: what Horizon's hook points call, translated with the mod (horizon-port-neoforge's bridge, CLAUDE.md
 * "Step (d)", decision 4). A block entity is made, given its level and ticked as the game does it: the block's own
 * newBlockEntity, its getTicker once, then that ticker each tick (Level.tickBlockEntities). Its saved compound goes
 * between Horizon's NBT and the mod's CompoundTag through the tag functions below, which Rust calls one value at a
 * time (no method here is overloaded: each has one name in the translation).
 */
public final class HorizonBridge {
    /** The level a server's block entities tick in: one, a server's (isClientSide false). */
    private static final ServerLevel LEVEL = new ServerLevel();
    /** What a block entity's load and save are given: nothing of it is read by the stand-ins. */
    private static final HolderLookup.Provider REGISTRIES = new HorizonLookup();

    private HorizonBridge() {}

    /** The mod's event bus, which its constructor is given: registering on it has nothing to do here (DeferredRegister). */
    public static net.neoforged.bus.api.IEventBus bus() {
        return new net.neoforged.bus.api.HorizonEventBus();
    }

    /** The mod's container, from its neoforge.mods.toml, which its constructor may be given with its bus. */
    public static net.neoforged.fml.ModContainer container(String modId, String version, String displayName, net.neoforged.bus.api.IEventBus bus) {
        return new net.neoforged.fml.HorizonModContainer(modId, version, displayName, bus);
    }

    /** The game's ids of a registry ("item", "fluid"), comma-separated, given before the mod loads (HorizonRegistries.known). */
    public static void known(String registry, String ids) {
        HorizonRegistries.known(registry, ids);
    }

    /**
     * Fires on the mod's bus, once it is loaded, what NeoForge fires then: NewRegistryEvent, its configs' loading
     * (ModConfigEvent.Loading: the order against the registries' isn't observed), then RegisterCapabilitiesEvent.
     * RegisterEvent isn't fired: DeferredRegister registers without it (HorizonRegistries).
     */
    public static void capabilities(net.neoforged.bus.api.IEventBus bus) {
        bus.post(net.neoforged.neoforge.registries.HorizonRegistries.newRegistryEvent());
        if (net.neoforged.fml.ModList.get().horizonOwn() instanceof net.neoforged.fml.HorizonModContainer own && own.getEventBus() == bus) own.horizonLoadConfigs();
        bus.post(net.neoforged.neoforge.capabilities.HorizonCapabilities.event());
    }

    public static Level level() {
        return LEVEL;
    }

    /**
     * The block entity the block registered under `blockId` makes at that position (EntityBlock.newBlockEntity, with
     * the block's default state), in the level; null when no such block is registered or it makes none.
     */
    public static BlockEntity make(String blockId, int x, int y, int z) {
        Object value = HorizonRegistries.get(ResourceKey.create(Registries.BLOCK, ResourceLocation.parse(blockId)));
        if (!(value instanceof Block block) || !(block instanceof EntityBlock entityBlock)) return null;
        BlockState state = block.defaultBlockState();
        BlockEntity made = entityBlock.newBlockEntity(new BlockPos(x, y, z), state);
        if (made != null) made.setLevel(LEVEL);
        return made;
    }

    /** The block's ticker for its block entity on the server (EntityBlock.getTicker), asked once; null when none. */
    public static BlockEntityTicker<?> ticker(BlockEntity entity) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof EntityBlock entityBlock)) return null;
        return entityBlock.getTicker(LEVEL, state, entity.getType());
    }

    /** One tick of a block entity with its ticker, as the level runs it. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void tickWith(BlockEntityTicker ticker, BlockEntity entity) {
        // The state in the world now (its properties may have changed since the last tick); the block entity's
        // follows it when it is still its block's, as the level keeps them together.
        BlockState state = LEVEL.getBlockState(entity.getBlockPos());
        if (state.getBlock() == entity.getBlockState().getBlock()) entity.setBlockState(state);
        else state = entity.getBlockState();
        ticker.tick(LEVEL, entity.getBlockPos(), state, entity);
    }

    public static void load(BlockEntity entity, CompoundTag tag) {
        entity.loadWithComponents(tag, REGISTRIES);
    }

    public static CompoundTag save(BlockEntity entity) {
        return entity.saveWithoutMetadata(REGISTRIES);
    }

    // ---- tags, one value at a time ----

    public static CompoundTag compound() {
        return new CompoundTag();
    }

    public static void put(CompoundTag compound, String key, Tag tag) {
        compound.put(key, tag);
    }

    public static Tag byteTag(int value) {
        return ByteTag.valueOf((byte) value);
    }

    public static Tag shortTag(int value) {
        return ShortTag.valueOf((short) value);
    }

    public static Tag intTag(int value) {
        return IntTag.valueOf(value);
    }

    public static Tag longTag(long value) {
        return LongTag.valueOf(value);
    }

    public static Tag floatTag(float value) {
        return FloatTag.valueOf(value);
    }

    public static Tag doubleTag(double value) {
        return DoubleTag.valueOf(value);
    }

    public static Tag stringTag(String value) {
        return StringTag.valueOf(value);
    }

    public static ListTag list() {
        return new ListTag();
    }

    /** Adds to a list; false when its elements are of another kind (a list of one kind, as the game's). */
    public static boolean add(ListTag list, Tag tag) {
        return list.addTag(list.size(), tag);
    }

    public static byte[] bytes(int n) {
        return new byte[n];
    }

    public static void setByte(byte[] array, int i, int value) {
        array[i] = (byte) value;
    }

    public static Tag byteArray(byte[] array) {
        return new ByteArrayTag(array);
    }

    public static int[] ints(int n) {
        return new int[n];
    }

    public static void setInt(int[] array, int i, int value) {
        array[i] = value;
    }

    public static Tag intArray(int[] array) {
        return new IntArrayTag(array);
    }

    public static long[] longs(int n) {
        return new long[n];
    }

    public static void setLong(long[] array, int i, long value) {
        array[i] = value;
    }

    public static Tag longArray(long[] array) {
        return new LongArrayTag(array);
    }

    /** A tag's kind, as NBT numbers them (1 byte ... 12 long array). */
    public static int kind(Tag tag) {
        return tag.getId();
    }

    public static int asInt(Tag tag) {
        return ((NumericTag) tag).getAsInt();
    }

    public static long asLong(Tag tag) {
        return ((NumericTag) tag).getAsLong();
    }

    public static float asFloat(Tag tag) {
        return ((NumericTag) tag).getAsFloat();
    }

    public static double asDouble(Tag tag) {
        return ((NumericTag) tag).getAsDouble();
    }

    public static String text(Tag tag) {
        return tag.getAsString();
    }

    /** A compound's number of keys, a list's or an array's of elements. */
    public static int size(Tag tag) {
        if (tag instanceof CompoundTag compound) return compound.size();
        return ((CollectionTag<?>) tag).size();
    }

    /** A compound's key at i, in its keys' order. */
    public static String keyAt(CompoundTag compound, int i) {
        int n = 0;
        for (String key : compound.getAllKeys()) if (n++ == i) return key;
        return null;
    }

    public static Tag get(CompoundTag compound, String key) {
        return compound.get(key);
    }

    /** A list's or an array's element at i (an array's as a tag of its kind). */
    public static Tag at(Tag listOrArray, int i) {
        return ((CollectionTag<?>) listOrArray).get(i);
    }

    /** What the bridge gives a block entity's load and save: the game's registries aren't read. */
    static final class HorizonLookup implements HolderLookup.Provider {
    }
}
