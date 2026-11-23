package net.minecraft.world.phys.shapes;

/**
 * The stand-in for the game's CollisionContext: a type, what a shape is asked for (an entity's collisions are Horizon's:
 * a state's shapes refuse). Not here: empty, of and the entity's context.
 */
public interface CollisionContext {
    boolean isDescending();

    boolean isAbove(VoxelShape shape, net.minecraft.core.BlockPos pos, boolean canAscend);

    boolean isHoldingItem(net.minecraft.world.item.Item item);

    boolean canStandOnFluid(net.minecraft.world.level.material.FluidState above, net.minecraft.world.level.material.FluidState fluid);
}
