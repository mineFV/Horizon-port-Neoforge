package net.minecraft.world.level;

/** The stand-in for the game's BlockGetter: its supertypes, and what is at a position (game/README.md, "What isn't here"). */
public interface BlockGetter extends net.minecraft.world.level.LevelHeightAccessor, net.neoforged.neoforge.common.extensions.IBlockGetterExtension {
    net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(net.minecraft.core.BlockPos pos);

    net.minecraft.world.level.block.state.BlockState getBlockState(net.minecraft.core.BlockPos pos);

    net.minecraft.world.level.material.FluidState getFluidState(net.minecraft.core.BlockPos pos);
}
