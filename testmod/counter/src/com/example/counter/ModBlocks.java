package com.example.counter;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Counter.MODID);
    public static final DeferredBlock<CounterBlock> COUNTER =
            BLOCKS.register("counter", () -> new CounterBlock(BlockBehaviour.Properties.of().strength(2.0F)));

    private ModBlocks() {}
}
