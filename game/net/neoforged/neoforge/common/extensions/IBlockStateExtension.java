package net.neoforged.neoforge.common.extensions;

/** The stand-in for NeoForge's IBlockStateExtension: what a state is in the world, Horizon's, refused when asked. */
public interface IBlockStateExtension {
    /** Observed (probe/BlockItemProbe.java, neighbours): its block's onNeighborChange, given this state and the rest as they come. */
    default void onNeighborChange(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, net.minecraft.core.BlockPos neighbor) {
        net.minecraft.world.level.block.state.BlockState state = (net.minecraft.world.level.block.state.BlockState) this;
        state.getBlock().onNeighborChange(state, level, pos, neighbor);
    }

    default boolean getWeakChanges(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos) {
        throw new UnsupportedOperationException("a block state's weak changes in the world aren't modelled (horizon-port-neoforge)");
    }

    default boolean isFlammable(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face) {
        throw new UnsupportedOperationException("whether a block state burns in the world isn't modelled (horizon-port-neoforge)");
    }

    default int getLightEmission(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        throw new UnsupportedOperationException("a block state's light in the world isn't modelled (horizon-port-neoforge)");
    }
}
