package com.example.counter;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** The mod: its blocks and block entity types, registered on the mod's bus. */
@Mod(Counter.MODID)
public final class Counter {
    public static final String MODID = "counter";

    public Counter(IEventBus bus) {
        ModBlocks.BLOCKS.register(bus);
        ModBlockEntities.TYPES.register(bus);
    }
}
