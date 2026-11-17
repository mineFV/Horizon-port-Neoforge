package com.example.counter;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * The mod: its blocks and block entity types, registered on the mod's bus, and a capability provider registered when
 * NeoForge fires its event (a listener given as a method reference, without its event's class, as Mekanism's are).
 */
@Mod(Counter.MODID)
public final class Counter {
    public static final String MODID = "counter";

    public Counter(IEventBus bus) {
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
