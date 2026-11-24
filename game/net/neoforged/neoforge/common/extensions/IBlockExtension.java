package net.neoforged.neoforge.common.extensions;

/** The stand-in for the game's IBlockExtension: its supertypes, a block's stack for a player's pick, what a neighbour's change does. */
public interface IBlockExtension {
    /** Observed (probe/BlockItemProbe.java, neighbours): nothing, unless the block says otherwise. */
    default void onNeighborChange(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, net.minecraft.core.BlockPos neighbor) {}

    /** Observed (probe/BlockItemProbe.java, clones): the block's own getCloneItemStack, given the level, the position and the state. */
    default net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.phys.HitResult target, net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player) {
        return ((net.minecraft.world.level.block.Block) this).getCloneItemStack(level, pos, state);
    }

    /**
     * Whether a block passes the weak redstone power it is given on to its neighbours: redstone in the world is
     * Horizon's, so it refuses (a block's own override runs).
     */
    default boolean shouldCheckWeakPower(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.SignalGetter level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
        throw new UnsupportedOperationException("whether a block passes weak redstone power on isn't modelled (horizon-port-neoforge)");
    }
}
