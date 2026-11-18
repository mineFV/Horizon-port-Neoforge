package horizonneo.probe;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * The probe as a NeoForge mod (`standins --horizon-src`): when the server has started, with the game's registries
 * filled, it asks GameProbe's questions and writes the answers to horizon-probe.txt where the server runs. Run by
 * `dump`'s path (Horizon's NeoForge oracle), first among the listeners, as the oracle stops the server in its own.
 */
@Mod("horizonprobe")
public final class InServer {
    public InServer(IEventBus bus, net.neoforged.fml.ModContainer container) {
        // A config file written before NeoForge reads it: values to read and to correct (ConfigFiles).
        try {
            Files.createDirectories(Path.of("config"));
            Files.writeString(Path.of("config", "horizonprobe-fixed.toml"), ConfigFiles.FIXED_TEXT, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        ConfigProbe.register(container, bus);
        ComponentProbe.ITEMS.register(bus);
        EntityTypeProbe.ENTITIES.register(bus);
        BlockItemProbe.BLOCKS.register(bus);
        BlockItemProbe.ITEMS.register(bus);
        CapabilityProbe.BLOCKS.register(bus);
        BuiltInProbe.WORD_ENTRIES.register(bus);
        bus.addListener(net.neoforged.neoforge.registries.NewRegistryEvent.class, e -> e.register(BuiltInProbe.WORDS));
        bus.addListener(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent.class, CapabilityProbe::register);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ServerStartedEvent event) -> {
            GameProbe.REGISTRIES = event.getServer().registryAccess();
            run();
            ids();
        });
    }

    /** The game's fluid ids, which `dump`'s facts don't have, to horizon-ids.txt: the bridge gives them to the stand-ins. */
    static void ids() {
        try {
            java.util.List<String> fluids = new java.util.ArrayList<>();
            for (net.minecraft.resources.ResourceLocation id : net.minecraft.core.registries.BuiltInRegistries.FLUID.keySet()) fluids.add(id.toString());
            java.util.Collections.sort(fluids);
            java.util.List<String> components = new java.util.ArrayList<>();
            for (net.minecraft.resources.ResourceLocation id : net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.keySet()) components.add(id.toString());
            java.util.Collections.sort(components);
            java.util.List<String> items = new java.util.ArrayList<>();
            for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
                if (id.startsWith("minecraft:")) items.add(id + "=" + horizonneo.probe.InServer.components(item.components()) + name(item, id));
            }
            Files.writeString(Path.of("horizon-ids.txt"), "fluid " + String.join(",", fluids) + "\ncomponent " + String.join(",", components)
                    + "\ncomponents " + String.join(";", items) + "\n", StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // The file's absence tells it.
        }
    }

    /** ",name:" and an item's description id, when it isn't "item." and its id (a block's item's is its block's). */
    static String name(net.minecraft.world.item.Item item, String id) {
        String own = "item." + id.replace(':', '.').replace('/', '.');
        return item.getDescriptionId().equals(own) ? "" : ",name:" + item.getDescriptionId();
    }

    /** An item's components as HorizonRegistries.known("components") takes them: type:value, in their order. */
    static String components(net.minecraft.core.component.DataComponentMap map) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        boolean other = false;
        for (net.minecraft.core.component.TypedDataComponent<?> typed : map) {
            String key = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(typed.type()).getPath();
            Object v = typed.value();
            if (v instanceof Integer n && java.util.Set.of("max_stack_size", "max_damage", "damage", "repair_cost").contains(key)) parts.add(key + ":" + n);
            else if (v instanceof net.minecraft.world.item.Rarity rarity) parts.add(key + ":" + rarity.getSerializedName());
            else if (v == net.minecraft.util.Unit.INSTANCE && key.equals("fire_resistant")) parts.add(key);
            else if (v == net.minecraft.world.item.component.ItemLore.EMPTY || v == net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY
                    || v == net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY) parts.add(key + ":empty");
            else other = true;
        }
        if (other) parts.add("other");
        return String.join(",", parts);
    }

    static void run() {
        try (PrintStream out = new PrintStream(Files.newOutputStream(Path.of("horizon-probe.txt")), true, StandardCharsets.UTF_8)) {
            GameProbe.OUT = out;
            GameProbe.main(new String[] {"server"});
        } catch (Throwable e) {
            try {
                Files.writeString(Path.of("horizon-probe.txt"), "failed: " + e, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                // Nothing more can be said: the file's absence tells it.
            }
        }
    }
}
