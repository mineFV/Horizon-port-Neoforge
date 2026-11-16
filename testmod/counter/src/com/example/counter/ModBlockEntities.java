package com.example.counter;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Counter.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CounterBlockEntity>> COUNTER =
            TYPES.register("counter", () -> BlockEntityType.Builder.of(CounterBlockEntity::new, ModBlocks.COUNTER.get()).build(null));

    private ModBlockEntities() {}
}
