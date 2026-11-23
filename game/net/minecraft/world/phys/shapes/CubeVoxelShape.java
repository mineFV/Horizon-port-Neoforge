package net.minecraft.world.phys.shapes;

/** The stand-in for the game's CubeVoxelShape: a shape over a cube's boundaries, i / size (VoxelShape). */
public final class CubeVoxelShape extends VoxelShape {
    CubeVoxelShape(double[][] coords, long[] cells, boolean[] cubeAxes, int[] discreteMin, int[] discreteMax, double[][] boxes) {
        super(coords, cells, cubeAxes, discreteMin, discreteMax, boxes);
    }
}
