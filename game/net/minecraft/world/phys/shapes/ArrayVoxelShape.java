package net.minecraft.world.phys.shapes;

/** The stand-in for the game's ArrayVoxelShape: a shape over boundaries of any values (VoxelShape). */
public class ArrayVoxelShape extends VoxelShape {
    ArrayVoxelShape(double[][] coords, long[] cells, boolean[] cubeAxes, int[] discreteMin, int[] discreteMax, double[][] boxes) {
        super(coords, cells, cubeAxes, discreteMin, discreteMax, boxes);
    }
}
