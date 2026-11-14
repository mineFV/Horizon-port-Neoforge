package horizonneo.dumper;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * horizon-port-neoforge's dumper: once the server has started, it writes what the game registered for the
 * namespaces it is given (blocks, items, block entities, menus, fluids, creative tabs) as JSON, then stops the
 * server. It reads what the game holds and calls nothing of the mod's beyond what the game itself calls.
 *
 * Every value is read in its own try: one that throws is left out and named under "unread" with why, so
 * the tool never mistakes a value it couldn't read for one that isn't there.
 *
 * System properties: horizon.dump.namespaces (comma-separated), horizon.dump.out (the JSON file).
 *
 * The sound, light and resistance it reads are the game's own, without a level: NeoForge deprecates them for
 * versions that take a level and a position, which a dump has neither of. What a block decides from its
 * neighbours isn't in the dump.
 */
@SuppressWarnings("deprecation")
@Mod("horizon_port_dumper")
public final class Dumper {
    /** A block with more states than this has its states counted, not listed. */
    static final int MAX_STATES = 4096;

    public Dumper(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, Dumper::started);
    }

    private static void started(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        String out = System.getProperty("horizon.dump.out");
        Set<String> namespaces = Set.of(System.getProperty("horizon.dump.namespaces", "").split(","));
        try {
            JsonObject facts = dump(server, namespaces);
            Files.writeString(Path.of(out), new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(facts), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            System.err.println("horizon-port-neoforge dumper: " + e);
        } finally {
            server.halt(false);
        }
    }

    static JsonObject dump(MinecraftServer server, Set<String> namespaces) {
        JsonObject facts = new JsonObject();
        facts.addProperty("schema", 1);
        facts.addProperty("game", server.getServerVersion());
        JsonArray ns = new JsonArray();
        namespaces.forEach(ns::add);
        facts.add("namespaces", ns);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        Map<SoundType, String> sounds = vanillaSounds();
        List<BlockEntityType<?>> blockEntities = mine(BuiltInRegistries.BLOCK_ENTITY_TYPE, namespaces);

        JsonArray blocks = new JsonArray();
        for (ResourceLocation id : ids(BuiltInRegistries.BLOCK, namespaces)) {
            blocks.add(block(id, BuiltInRegistries.BLOCK.get(id), sounds, blockEntities));
        }
        facts.add("blocks", blocks);

        JsonArray items = new JsonArray();
        for (ResourceLocation id : ids(BuiltInRegistries.ITEM, namespaces)) items.add(item(id, BuiltInRegistries.ITEM.get(id), ops));
        facts.add("items", items);

        JsonArray types = new JsonArray();
        for (ResourceLocation id : ids(BuiltInRegistries.BLOCK_ENTITY_TYPE, namespaces)) {
            JsonObject o = new JsonObject();
            o.addProperty("id", id.toString());
            types.add(o);
        }
        facts.add("blockEntityTypes", types);

        JsonArray menus = new JsonArray();
        for (ResourceLocation id : ids(BuiltInRegistries.MENU, namespaces)) {
            JsonObject o = new JsonObject();
            o.addProperty("id", id.toString());
            menus.add(o);
        }
        facts.add("menus", menus);

        JsonArray fluids = new JsonArray();
        for (ResourceLocation id : ids(BuiltInRegistries.FLUID, namespaces)) fluids.add(fluid(id, BuiltInRegistries.FLUID.get(id)));
        facts.add("fluids", fluids);

        JsonArray tabs = new JsonArray();
        for (ResourceLocation id : ids(BuiltInRegistries.CREATIVE_MODE_TAB, namespaces)) tabs.add(tab(id, BuiltInRegistries.CREATIVE_MODE_TAB.get(id)));
        facts.add("creativeTabs", tabs);
        return facts;
    }

    // ---- blocks ----

    private static JsonObject block(ResourceLocation id, Block block, Map<SoundType, String> sounds, List<BlockEntityType<?>> blockEntities) {
        JsonObject o = new JsonObject(), unread = new JsonObject();
        o.addProperty("id", id.toString());
        o.addProperty("class", block.getClass().getName());
        BlockState def = block.defaultBlockState();
        read(o, unread, "hardness", () -> new JsonPrimitive(block.defaultDestroyTime()));
        read(o, unread, "resistance", () -> new JsonPrimitive(block.getExplosionResistance()));
        read(o, unread, "sounds", () -> {
            String like = sounds.get(def.getSoundType());
            if (like == null) throw new IllegalStateException("a sound type no vanilla block has");
            return new JsonPrimitive(like);
        });
        read(o, unread, "item", () -> new JsonPrimitive(BuiltInRegistries.ITEM.getKey(block.asItem()).toString()));
        o.addProperty("entityBlock", block instanceof EntityBlock);
        read(o, unread, "blockEntityTypes", () -> {
            JsonArray a = new JsonArray();
            for (BlockEntityType<?> t : blockEntities) {
                if (t.isValid(def)) a.add(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(t).toString());
            }
            return a;
        });
        read(o, unread, "properties", () -> {
            JsonArray a = new JsonArray();
            for (Property<?> p : block.getStateDefinition().getProperties()) a.add(property(p));
            return a;
        });
        read(o, unread, "defaultState", () -> values(def));
        List<BlockState> states = block.getStateDefinition().getPossibleStates();
        o.addProperty("stateCount", states.size());
        if (states.size() <= MAX_STATES) {
            JsonArray a = new JsonArray();
            for (BlockState s : states) a.add(state(s));
            o.add("states", a);
        } else {
            unread.addProperty("states", states.size() + " states, more than " + MAX_STATES);
        }
        o.add("unread", unread);
        return o;
    }

    private static JsonObject property(Property<?> p) {
        JsonObject o = new JsonObject();
        o.addProperty("name", p.getName());
        o.addProperty("type", p instanceof BooleanProperty ? "boolean" : p instanceof IntegerProperty ? "int" : p instanceof EnumProperty<?> ? "enum" : "other");
        JsonArray values = new JsonArray();
        for (String v : names(p)) values.add(v);
        o.add("values", values);
        return o;
    }

    private static <T extends Comparable<T>> List<String> names(Property<T> p) {
        List<String> out = new ArrayList<>();
        for (T v : p.getPossibleValues()) out.add(p.getName(v));
        return out;
    }

    private static JsonObject values(BlockState s) {
        JsonObject o = new JsonObject();
        for (Property<?> p : s.getProperties()) o.addProperty(p.getName(), value(s, p));
        return o;
    }

    private static <T extends Comparable<T>> String value(BlockState s, Property<T> p) {
        return p.getName(s.getValue(p));
    }

    private static JsonObject state(BlockState s) {
        JsonObject o = new JsonObject(), unread = new JsonObject();
        o.add("values", values(s));
        read(o, unread, "light", () -> new JsonPrimitive(s.getLightEmission()));
        read(o, unread, "shape", () -> boxes(s.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)));
        read(o, unread, "collision", () -> boxes(s.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)));
        read(o, unread, "renderShape", () -> new JsonPrimitive(s.getRenderShape().name()));
        read(o, unread, "occludes", () -> new JsonPrimitive(s.canOcclude()));
        read(o, unread, "randomTicks", () -> new JsonPrimitive(s.isRandomlyTicking()));
        if (!unread.isEmpty()) o.add("unread", unread);
        return o;
    }

    private static JsonArray boxes(VoxelShape shape) {
        JsonArray a = new JsonArray();
        for (AABB b : shape.toAabbs()) {
            JsonArray box = new JsonArray();
            for (double d : new double[] {b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ}) box.add(d);
            a.add(box);
        }
        return a;
    }

    /** Each vanilla sound type, as the first vanilla block (in registry order) that has it. */
    private static Map<SoundType, String> vanillaSounds() {
        Map<SoundType, String> out = new IdentityHashMap<>();
        for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet().stream().filter(k -> k.getNamespace().equals("minecraft")).sorted().toList()) {
            try {
                out.putIfAbsent(BuiltInRegistries.BLOCK.get(id).defaultBlockState().getSoundType(), id.toString());
            } catch (RuntimeException e) {
                // That block's sound isn't a reference for any other.
            }
        }
        return out;
    }

    // ---- items ----

    private static JsonObject item(ResourceLocation id, Item item, RegistryOps<JsonElement> ops) {
        JsonObject o = new JsonObject(), unread = new JsonObject();
        o.addProperty("id", id.toString());
        o.addProperty("class", item.getClass().getName());
        read(o, unread, "maxStackSize", () -> new JsonPrimitive(item.getDefaultMaxStackSize()));
        if (item instanceof BlockItem b) o.addProperty("placesBlock", BuiltInRegistries.BLOCK.getKey(b.getBlock()).toString());
        JsonObject components = new JsonObject(), unreadComponents = new JsonObject();
        try {
            for (TypedDataComponent<?> c : item.components()) {
                String key = String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(c.type()));
                try {
                    components.add(key, encode(c, ops));
                } catch (RuntimeException e) {
                    unreadComponents.addProperty(key, why(e));
                }
            }
        } catch (RuntimeException e) {
            unread.addProperty("components", why(e));
        }
        o.add("components", components);
        if (!unreadComponents.isEmpty()) o.add("unreadComponents", unreadComponents);
        o.add("unread", unread);
        return o;
    }

    private static <T> JsonElement encode(TypedDataComponent<T> c, RegistryOps<JsonElement> ops) {
        DataComponentType<T> type = c.type();
        Codec<T> codec = type.codec();
        if (codec == null) throw new IllegalStateException("a component that isn't saved (no codec)");
        return codec.encodeStart(ops, c.value()).getOrThrow(IllegalStateException::new);
    }

    // ---- fluids, tabs ----

    private static JsonObject fluid(ResourceLocation id, Fluid fluid) {
        JsonObject o = new JsonObject(), unread = new JsonObject();
        o.addProperty("id", id.toString());
        o.addProperty("class", fluid.getClass().getName());
        read(o, unread, "source", () -> new JsonPrimitive(fluid.isSource(fluid.defaultFluidState())));
        read(o, unread, "bucket", () -> new JsonPrimitive(BuiltInRegistries.ITEM.getKey(fluid.getBucket()).toString()));
        read(o, unread, "type", () -> {
            FluidType t = fluid.getFluidType();
            JsonObject type = new JsonObject();
            type.addProperty("lightLevel", t.getLightLevel());
            type.addProperty("density", t.getDensity());
            type.addProperty("temperature", t.getTemperature());
            type.addProperty("viscosity", t.getViscosity());
            type.addProperty("descriptionId", t.getDescriptionId());
            return type;
        });
        o.add("unread", unread);
        return o;
    }

    private static JsonObject tab(ResourceLocation id, CreativeModeTab tab) {
        JsonObject o = new JsonObject(), unread = new JsonObject();
        o.addProperty("id", id.toString());
        read(o, unread, "title", () -> {
            Component title = tab.getDisplayName();
            return new JsonPrimitive(title.getContents() instanceof TranslatableContents t ? t.getKey() : title.getString());
        });
        o.add("unread", unread);
        return o;
    }

    // ---- helpers ----

    private static <T> List<ResourceLocation> ids(Registry<T> registry, Set<String> namespaces) {
        return registry.keySet().stream().filter(k -> namespaces.contains(k.getNamespace())).sorted().toList();
    }

    private static <T> List<T> mine(Registry<T> registry, Set<String> namespaces) {
        List<T> out = new ArrayList<>();
        for (ResourceLocation id : ids(registry, namespaces)) out.add(registry.get(id));
        return out;
    }

    private static void read(JsonObject o, JsonObject unread, String key, Supplier<JsonElement> value) {
        try {
            o.add(key, value.get());
        } catch (RuntimeException | LinkageError e) {
            unread.addProperty(key, why(e));
        }
    }

    private static String why(Throwable e) {
        return e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
    }
}
