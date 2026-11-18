package com.example.counter;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * The mod: its blocks and block entity types, registered on the mod's bus, and a capability provider registered when
 * NeoForge fires its event (a listener given as a method reference, without its event's class, as Mekanism's are).
 */
@Mod(Counter.MODID)
public final class Counter {
    public static final String MODID = "counter";

    /** Its config: how many ticks a second counts (read each tick, once NeoForge has loaded the config). */
    static final ModConfigSpec.IntValue TICKS_PER_SECOND;
    static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        TICKS_PER_SECOND = b.comment("Ticks in a second").defineInRange("ticks_per_second", 20, 1, 1000);
        SPEC = b.build();
    }

    public Counter(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
        ModBlocks.BLOCKS.register(bus);
        ModBlockEntities.TYPES.register(bus);
        bus.addListener(Counter::capabilities);
    }

    /** How many times the capabilities were registered with the counter's block: the block entity saves it. */
    static int capabilities;

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.COUNTER.get(), (counter, side) -> null);
        if (event.isBlockRegistered(Capabilities.ItemHandler.BLOCK, ModBlocks.COUNTER.get())) capabilities++;
    }
}
