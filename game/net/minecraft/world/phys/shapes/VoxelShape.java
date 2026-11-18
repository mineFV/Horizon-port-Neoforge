package net.minecraft.world.phys.shapes;

/** The stand-in for the game's VoxelShape: its type alone (blocks' shapes are Horizon's: BlockBehaviour.BlockStateBase refuses them). */
public abstract class VoxelShape {
    VoxelShape() {}

    /** Not modelled: there is no shape to write. */
    @Override
    public String toString() {
        throw new UnsupportedOperationException("a shape's text isn't modelled (horizon-port-neoforge)");
    }
}
