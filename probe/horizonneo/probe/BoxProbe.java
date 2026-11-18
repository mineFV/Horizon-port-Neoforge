package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The game's boxes: made, moved, grown, shrunk, intersected and compared, NaN and -0.0 included. */
final class BoxProbe {
    private BoxProbe() {}

    static void run() {
        AABB[] boxes = {new AABB(0, 0, 0, 1, 1, 1), new AABB(3, -2, 5, -1, 4, 0.5), new AABB(-0.0, 0.0, 0, 0.0, -0.0, 0), new AABB(Double.NaN, 1, 2, 3, Double.NaN, 4),
            new AABB(new BlockPos(5, -7, 9)), new AABB(new Vec3(1.5, 2.5, 3.5), new Vec3(-1, 0, 8)), AABB.unitCubeFromLowerCorner(new Vec3(0.25, -3, 1e9)),
            AABB.encapsulatingFullBlocks(new BlockPos(4, 2, -3), new BlockPos(-1, 5, 0)), AABB.ofSize(new Vec3(1, 2, 3), 4, 0.5, -2)};
        for (int i = 0; i < boxes.length; i++) {
            AABB b = boxes[i];
            p("box " + i, () -> b + " " + b.hashCode() + " " + b.getXsize() + " " + b.getYsize() + " " + b.getZsize() + " " + b.getSize() + " " + b.hasNaN() + " " + b.getCenter()
                    + " " + b.getBottomCenter() + " " + b.getMinPosition() + " " + b.getMaxPosition() + " " + b.min(Direction.Axis.Y) + " " + b.max(Direction.Axis.Z));
            p("box changed " + i, () -> b.move(1, -2, 0.5) + " " + b.move(new BlockPos(1, 2, 3)) + " " + b.inflate(0.5) + " " + b.inflate(1, -2, 0) + " " + b.deflate(0.25)
                    + " " + b.contract(1, -1, 0) + " " + b.expandTowards(-2, 0, 3) + " " + b.expandTowards(new Vec3(1, -1, 0)) + " " + b.setMinX(10) + " " + b.setMaxY(-10)
                    + " " + b.setMinZ(2) + " " + b.setMaxX(0) + " " + b.setMinY(0) + " " + b.setMaxZ(0));
            for (int j = 0; j < boxes.length; j++) {
                AABB c = boxes[j];
                p("box pair " + i + " " + j, () -> b.intersects(c) + " " + b.intersect(c) + " " + b.minmax(c) + " " + b.equals(c) + " " + (b.hashCode() == c.hashCode()));
            }
            p("box points " + i, () -> b.contains(new Vec3(0, 0, 0)) + " " + b.contains(0.5, 0.5, 0.5) + " " + b.contains(1, 1, 1) + " " + b.distanceToSqr(new Vec3(5, -5, 0.5))
                    + " " + b.intersects(new Vec3(0, 0, 0), new Vec3(2, 2, 2)) + " " + b.intersects(-1, -1, -1, 0, 0, 0));
        }
        p("box equal", () -> new AABB(0, 0, 0, 1, 1, 1).equals(new AABB(1, 1, 1, 0, 0, 0)) + " " + new AABB(0.0, 0, 0, 1, 1, 1).equals(new AABB(-0.0, 0, 0, 1, 1, 1)));
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
