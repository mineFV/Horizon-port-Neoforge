package horizonneo.machines;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * horizon-port-neoforge's companion to Horizon's NeoForge oracle: what the oracle's facts don't hold of a mod's
 * machines. Horizon's oracle (tools/neoforge_oracle.py) loads it beside the mod with `--deps`, unchanged.
 *
 * Once the server has started, before the oracle's exporter writes its facts and stops the server, it places
 * each block of the namespaces (`horizon.oracle.namespaces`) that is an EntityBlock, alone in the overworld,
 * and reads what NeoForge gives of it there: the block entity's type, whether it ticks on the server, its saved
 * keys, and the item, energy and fluid capabilities with no side and through each face; and the menu its block
 * entity provides, with each slot where the menu puts it. Then it takes the block away. The file is
 * `<oracle out without .json>.machines.json`.
 *
 * Every value is read in its own try: one that throws is named under "unread" with why, so that a value that
 * couldn't be read is never taken for one that isn't there. Energy is filled to read the most one extract moves:
 * the block is thrown away afterwards.
 */
@Mod("horizon_port_machines")
public final class MachinesExporter {
    /** Where each block is placed: away from the oracle's own floor, at 0 99 0. */
    static final BlockPos AT = new BlockPos(5, 120, 5);

    public MachinesExporter(IEventBus modBus) {
        // Before the oracle's exporter, which stops the server once it has written its facts.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ServerStartedEvent.class, MachinesExporter::started);
    }

    private static void started(ServerStartedEvent event) {
        String facts = System.getProperty("horizon.oracle.out", "horizon-oracle.json");
        Path out = Path.of(facts.replaceAll("\\.json$", "") + ".machines.json");
        Set<String> namespaces = new TreeSet<>(Arrays.asList(System.getProperty("horizon.oracle.namespaces", "").split(",")));
        namespaces.remove("");
        JsonObject o;
        try {
            o = export(event.getServer(), namespaces);
        } catch (Throwable e) {
            o = new JsonObject();
            o.addProperty("schema", 1);
            o.addProperty("failed", String.valueOf(e));
        }
        try {
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            System.out.println("horizon-port-machines: wrote " + out);
        } catch (Throwable e) {
            System.err.println("horizon-port-machines: " + out + " couldn't be written: " + e);
        }
    }

    static JsonObject export(MinecraftServer server, Set<String> namespaces) {
        ServerLevel level = server.overworld();
        JsonObject o = new JsonObject();
        o.addProperty("schema", 1);
        JsonArray ns = new JsonArray();
        namespaces.forEach(ns::add);
        o.add("namespaces", ns);

        JsonObject types = new JsonObject();
        for (ResourceLocation id : BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet()) {
            if (!namespaces.contains(id.getNamespace())) continue;
            BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id);
            JsonArray blocks = new JsonArray();
            for (ResourceLocation b : new TreeSet<>(BuiltInRegistries.BLOCK.keySet())) {
                if (type.isValid(BuiltInRegistries.BLOCK.get(b).defaultBlockState())) blocks.add(b.toString());
            }
            JsonObject t = new JsonObject();
            t.add("blocks", blocks);
            types.add(id.toString(), t);
        }
        o.add("block_entity_types", types);

        JsonArray menus = new JsonArray();
        for (ResourceLocation id : new TreeSet<>(BuiltInRegistries.MENU.keySet())) if (namespaces.contains(id.getNamespace())) menus.add(id.toString());
        o.add("menu_types", menus);

        List<net.minecraft.world.item.Item> items = new java.util.ArrayList<>();
        for (ResourceLocation id : new TreeSet<>(BuiltInRegistries.ITEM.keySet())) if (namespaces.contains(id.getNamespace())) items.add(BuiltInRegistries.ITEM.get(id));
        for (ResourceLocation id : new TreeSet<>(BuiltInRegistries.ITEM.keySet())) if (id.getNamespace().equals("minecraft")) items.add(BuiltInRegistries.ITEM.get(id));
        candidates = List.copyOf(items);

        JsonObject blocks = new JsonObject();
        level.getChunkAt(AT);
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        for (ResourceLocation id : new TreeSet<>(BuiltInRegistries.BLOCK.keySet())) {
            if (!namespaces.contains(id.getNamespace())) continue;
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (!(block instanceof EntityBlock)) continue;
            blocks.add(id.toString(), block(level, player, block));
        }
        o.add("blocks", blocks);
        return o;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static JsonObject block(ServerLevel level, FakePlayer player, Block block) {
        JsonObject o = new JsonObject(), unread = new JsonObject();
        BlockState state = block.defaultBlockState();
        try {
            level.setBlock(AT, state, 2 | 16);
            BlockEntity be = level.getBlockEntity(AT);
            if (be == null) {
                unread.addProperty("block_entity", "the block made none when it was placed alone");
            } else {
                // NeoForge calls onLoad at the level's next tick, which never comes before the oracle stops the
                // server: a block entity that sets up its sides there (Mekanism's) is called here, and taken off
                // the level's list so that it isn't called again once removed.
                read(o, unread, "loaded", () -> {
                    be.onLoad();
                    forget(level, be);
                    return NONE;
                });
                read(o, unread, "block_entity", () -> new JsonPrimitive(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString()));
                read(o, unread, "class", () -> new JsonPrimitive(be.getClass().getName()));
                read(o, unread, "ticks_on_server", () -> new JsonPrimitive(((EntityBlock) block).getTicker(level, state, (BlockEntityType) be.getType()) != null));
                // The ticker's class, as the JVM names it: a lambda's is "Owner$$Lambda/0x...". Its code, which the
                // JVM writes to DUMP_LAMBDA_PROXY_CLASS_FILES/ where the server runs (dump asks it to), names the
                // mod's method it runs: it goes along as it is, for the tool to read (Tickers).
                read(o, unread, "ticker", () -> {
                    Object ticker = ((EntityBlock) block).getTicker(level, state, (BlockEntityType) be.getType());
                    return ticker == null ? NONE : new JsonPrimitive(ticker.getClass().getName());
                });
                read(o, unread, "ticker_class", () -> {
                    Object ticker = ((EntityBlock) block).getTicker(level, state, (BlockEntityType) be.getType());
                    if (ticker == null || !ticker.getClass().isHidden()) return NONE;
                    // "a.b.Owner$$Lambda/0x12" is written as a/b/Owner$$Lambda.0x12.class.
                    String name = ticker.getClass().getName();
                    int slash = name.indexOf('/');
                    String file0 = slash < 0 ? name.replace('.', '/') : name.substring(0, slash).replace('.', '/') + "." + name.substring(slash + 1);
                    java.nio.file.Path file = java.nio.file.Path.of("DUMP_LAMBDA_PROXY_CLASS_FILES", file0 + ".class");
                    if (!java.nio.file.Files.isRegularFile(file)) throw new IllegalStateException("the JVM didn't write the ticker's class (" + file + ")");
                    return new JsonPrimitive(java.util.Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(file)));
                });
                read(o, unread, "saved", () -> {
                    CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
                    JsonArray keys = new JsonArray();
                    new TreeSet<>(tag.getAllKeys()).forEach(keys::add);
                    return keys;
                });
                read(o, unread, "items", () -> items(level));
                read(o, unread, "energy", () -> energy(level));
                read(o, unread, "fluids", () -> fluids(level));
                read(o, unread, "menu", () -> menu(be, state, level, player));
            }
        } catch (Throwable e) {
            unread.addProperty("placing", String.valueOf(e));
        } finally {
            try {
                level.removeBlockEntity(AT);
                level.setBlock(AT, Blocks.AIR.defaultBlockState(), 2 | 16);
            } catch (Throwable e) {
                unread.addProperty("removing", String.valueOf(e));
            }
        }
        o.add("unread", unread);
        return o;
    }

    /** Takes the block entity off NeoForge's lists of those whose onLoad is still to come. */
    private static void forget(ServerLevel level, BlockEntity be) throws ReflectiveOperationException {
        for (String name : List.of("freshBlockEntities", "pendingFreshBlockEntities")) {
            Field f = net.minecraft.world.level.Level.class.getDeclaredField(name);
            f.setAccessible(true);
            ((List<?>) f.get(level)).remove(be);
        }
    }

    // ---- capabilities ----

    /** The faces in Horizon's order, which is Direction's: down, up, north, south, west, east. */
    static final List<Direction> FACES = List.of(Direction.values());

    /** Items tried in slots, one at a time: the mod's own first, then the game's. Set by export. */
    static List<net.minecraft.world.item.Item> candidates = List.of();

    /**
     * The block's item slots: the handler NeoForge gives with no side, and the one through each face. A face's
     * slots are numbered by its own handler, which may be a proxy (Mekanism's), so where each one is in the
     * container is measured: an item is put through the face (or into the container) and looked for on the
     * other side. A face slot that takes one of the candidates is an input; one that gives back what is in it,
     * an output. A slot no candidate goes into stays unmapped, and is counted.
     */
    private static JsonElement items(ServerLevel level) {
        IItemHandler all = level.getCapability(Capabilities.ItemHandler.BLOCK, AT, (Direction) null);
        java.util.Map<Direction, IItemHandler> sides = new java.util.EnumMap<>(Direction.class);
        for (Direction d : FACES) {
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, AT, d);
            if (h != null && h.getSlots() > 0) sides.put(d, h);
        }
        if ((all == null || all.getSlots() == 0) && sides.isEmpty()) return NONE;
        JsonObject o = new JsonObject();
        if (all != null) {
            o.addProperty("slots", all.getSlots());
            JsonArray limits = new JsonArray();
            for (int i = 0; i < all.getSlots(); i++) limits.add(all.getSlotLimit(i));
            o.add("limits", limits);
        }
        JsonObject faces = new JsonObject();
        for (Direction d : FACES) {
            IItemHandler h = sides.get(d);
            JsonObject f = new JsonObject();
            f.addProperty("slots", h == null ? 0 : h.getSlots());
            if (h != null) {
                f.addProperty("same", h == all);
                if (all != null) face(all, h, f);
            }
            faces.add(d.getSerializedName(), f);
        }
        o.add("faces", faces);
        return o;
    }

    /** Where each slot of the face's handler is in the container, and which take and give items. */
    private static void face(IItemHandler all, IItemHandler face, JsonObject f) {
        JsonArray reaches = new JsonArray(), inputs = new JsonArray(), outputs = new JsonArray();
        int unmapped = 0;
        for (int i = 0; i < face.getSlots(); i++) {
            Integer slot = null;
            boolean input = false, output = false;
            // Through the face first: what goes in is an input.
            net.minecraft.world.item.ItemStack in = accepted(face, i);
            if (in != null) {
                input = true;
                List<net.minecraft.world.item.ItemStack> before = snapshot(all);
                face.insertItem(i, in, false);
                slot = changed(all, before);
                output = !face.extractItem(i, 1, true).isEmpty();
                empty(all, face, i, slot);
            }
            // Else into the container, slot by slot, to see which one the face shows.
            for (int j = 0; slot == null && all.getSlots() > 0 && j < all.getSlots(); j++) {
                net.minecraft.world.item.ItemStack put = accepted(all, j);
                if (put == null) continue;
                all.insertItem(j, put, false);
                if (!face.getStackInSlot(i).isEmpty() && net.minecraft.world.item.ItemStack.isSameItem(face.getStackInSlot(i), put)) {
                    slot = j;
                    output = !face.extractItem(i, 1, true).isEmpty();
                }
                empty(all, face, i, j);
            }
            if (slot == null) {
                unmapped++;
                continue;
            }
            reaches.add(slot);
            if (input) inputs.add(slot);
            if (output) outputs.add(slot);
        }
        f.add("reaches", reaches);
        f.add("inputs", inputs);
        f.add("outputs", outputs);
        if (unmapped > 0) f.addProperty("unmapped", unmapped);
    }

    /** One of the candidates the slot takes, one item of it; null when none. */
    private static net.minecraft.world.item.ItemStack accepted(IItemHandler h, int slot) {
        if (!h.getStackInSlot(slot).isEmpty()) return null;
        for (net.minecraft.world.item.Item item : candidates) {
            net.minecraft.world.item.ItemStack one = new net.minecraft.world.item.ItemStack(item);
            if (one.isEmpty() || !h.isItemValid(slot, one)) continue;
            if (h.insertItem(slot, one.copy(), true).isEmpty()) return one;
        }
        return null;
    }

    private static List<net.minecraft.world.item.ItemStack> snapshot(IItemHandler h) {
        List<net.minecraft.world.item.ItemStack> out = new java.util.ArrayList<>();
        for (int j = 0; j < h.getSlots(); j++) out.add(h.getStackInSlot(j).copy());
        return out;
    }

    /** The one container slot whose stack is not what it was; null when none or several. */
    private static Integer changed(IItemHandler h, List<net.minecraft.world.item.ItemStack> before) {
        Integer found = null;
        for (int j = 0; j < h.getSlots() && j < before.size(); j++) {
            if (net.minecraft.world.item.ItemStack.matches(h.getStackInSlot(j), before.get(j))) continue;
            if (found != null) return null;
            found = j;
        }
        return found;
    }

    /** Takes back what was put in, through whichever handler gives it. */
    private static void empty(IItemHandler all, IItemHandler face, int i, Integer j) {
        face.extractItem(i, 64, false);
        if (j != null) {
            all.extractItem(j, 64, false);
            if (!all.getStackInSlot(j).isEmpty() && all instanceof net.neoforged.neoforge.items.IItemHandlerModifiable m) {
                m.setStackInSlot(j, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }

    private static JsonElement energy(ServerLevel level) {
        IEnergyStorage all = level.getCapability(Capabilities.EnergyStorage.BLOCK, AT, (Direction) null);
        java.util.Map<Direction, IEnergyStorage> sides = new java.util.EnumMap<>(Direction.class);
        for (Direction d : FACES) {
            IEnergyStorage h = level.getCapability(Capabilities.EnergyStorage.BLOCK, AT, d);
            if (h != null) sides.put(d, h);
        }
        if (all == null && sides.isEmpty()) return NONE;
        JsonObject o = new JsonObject();
        if (all != null) o.add("unsided", storage(all));
        JsonObject faces = new JsonObject();
        for (Direction d : FACES) faces.add(d.getSerializedName(), sides.containsKey(d) ? storage(sides.get(d)) : new JsonObject());
        o.add("faces", faces);
        // Filled through whatever takes energy, the most one extract moves through each: min(its limit, the
        // capacity). The block is thrown away afterwards.
        List<IEnergyStorage> fillers = new java.util.ArrayList<>();
        if (all != null) fillers.add(all);
        fillers.addAll(sides.values());
        IEnergyStorage any = all != null ? all : sides.values().iterator().next();
        for (IEnergyStorage f : fillers) {
            for (int i = 0; i < 10_000 && any.getEnergyStored() < any.getMaxEnergyStored(); i++) {
                if (f.receiveEnergy(Integer.MAX_VALUE, false) == 0) break;
            }
        }
        o.addProperty("filled", any.getEnergyStored());
        if (all != null) o.getAsJsonObject("unsided").addProperty("extract_at_most", all.extractEnergy(Integer.MAX_VALUE, true));
        for (Direction d : sides.keySet()) faces.getAsJsonObject(d.getSerializedName()).addProperty("extract_at_most", sides.get(d).extractEnergy(Integer.MAX_VALUE, true));
        return o;
    }

    /** What one view of the buffer says, empty: the most one insert moves is min(its limit, the capacity). */
    private static JsonObject storage(IEnergyStorage h) {
        JsonObject f = new JsonObject();
        f.addProperty("capacity", h.getMaxEnergyStored());
        f.addProperty("stored", h.getEnergyStored());
        f.addProperty("can_receive", h.canReceive());
        f.addProperty("can_extract", h.canExtract());
        f.addProperty("insert_at_most", h.receiveEnergy(Integer.MAX_VALUE, true));
        return f;
    }

    private static JsonElement fluids(ServerLevel level) {
        IFluidHandler all = level.getCapability(Capabilities.FluidHandler.BLOCK, AT, (Direction) null);
        JsonObject faces = new JsonObject();
        boolean any = all != null;
        for (Direction d : FACES) {
            IFluidHandler h = level.getCapability(Capabilities.FluidHandler.BLOCK, AT, d);
            JsonObject f = new JsonObject();
            f.addProperty("tanks", h == null ? 0 : h.getTanks());
            f.addProperty("same", h != null && h == all);
            if (h != null) any = true;
            faces.add(d.getSerializedName(), f);
        }
        if (!any) return NONE;
        JsonObject o = new JsonObject();
        if (all != null) {
            JsonArray tanks = new JsonArray();
            for (int i = 0; i < all.getTanks(); i++) tanks.add(all.getTankCapacity(i));
            o.add("tanks", tanks);
        }
        o.add("faces", faces);
        return o;
    }

    // ---- the menu ----

    private static JsonElement menu(BlockEntity be, BlockState state, ServerLevel level, FakePlayer player) throws Exception {
        MenuProvider provider = be instanceof MenuProvider p ? p : state.getMenuProvider(level, AT);
        if (provider == null) return NONE;
        JsonObject o = new JsonObject();
        o.addProperty("from", be instanceof MenuProvider ? "block_entity" : "block");
        Component title = provider.getDisplayName();
        o.addProperty("title", title.getString());
        if (title.getContents() instanceof TranslatableContents t) o.addProperty("title_key", t.getKey());
        AbstractContainerMenu menu = provider.createMenu(1, player.getInventory(), player);
        if (menu == null) {
            o.addProperty("made", false);
            return o;
        }
        try {
            o.addProperty("type", BuiltInRegistries.MENU.getKey(menu.getType()).toString());
        } catch (UnsupportedOperationException e) {
            o.addProperty("type_unread", "the menu has no type");
        }
        o.addProperty("class", menu.getClass().getName());
        JsonArray slots = new JsonArray();
        JsonArray inventory = null;
        for (Slot s : menu.slots) {
            if (s.container instanceof Inventory) {
                if (inventory == null) {
                    inventory = new JsonArray();
                    inventory.add(s.x);
                    inventory.add(s.y);
                }
                continue;
            }
            JsonObject slot = new JsonObject();
            slot.addProperty("index", s.getContainerSlot());
            slot.addProperty("x", s.x);
            slot.addProperty("y", s.y);
            slot.addProperty("container", s.container.getClass().getName());
            slots.add(slot);
        }
        o.add("slots", slots);
        if (inventory != null) o.add("inventory", inventory);
        o.addProperty("player_slots", menu.slots.size() - slots.size());
        // AbstractContainerMenu keeps its data slots private; NeoForge runs on Mojang's names, so this is its field.
        try {
            Field data = AbstractContainerMenu.class.getDeclaredField("dataSlots");
            data.setAccessible(true);
            o.addProperty("data_slots", ((List<?>) data.get(menu)).size());
        } catch (ReflectiveOperationException | RuntimeException e) {
            o.addProperty("data_slots_unread", String.valueOf(e));
        }
        menu.removed(player);
        return o;
    }

    // ---- reading ----

    interface Read {
        JsonElement get() throws Exception;
    }

    /** A value read in its own try: a throw is named under unread; none is left out. */
    private static void read(JsonObject o, JsonObject unread, String key, Read value) {
        try {
            JsonElement v = value.get();
            if (v != NONE) o.add(key, v);
        } catch (Throwable e) {
            unread.addProperty(key, String.valueOf(e));
        }
    }

    /** "The block has none": the key isn't written. Compared by identity. */
    static final JsonElement NONE = new JsonArray();
}
