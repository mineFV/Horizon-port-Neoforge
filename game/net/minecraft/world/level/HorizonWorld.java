package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.HorizonRegistries;

/**
 * Not the game's: a level's questions to Horizon's world (horizon-port-neoforge's bridge). `ask` is native: the
 * translation sends the question to the thread that holds Horizon's world (bridge.rs) and waits for the answer.
 * One question for now: the block at a position, by its id.
 */
public final class HorizonWorld {
    private HorizonWorld() {}

    /** Horizon's answer to a question ("block x y z": the block's id there); null when no world answers. */
    static native String ask(String question);

    /**
     * The state at a position: its block's (the mod's own, or one made for a block the mod didn't register, with its
     * id), in its default state: states' properties aren't modelled yet.
     */
    public static BlockState blockState(BlockPos pos) {
        String id = ask("block " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        if (id == null) throw new IllegalStateException("no world answers for " + pos);
        return HorizonRegistries.block(id).defaultBlockState();
    }
}
