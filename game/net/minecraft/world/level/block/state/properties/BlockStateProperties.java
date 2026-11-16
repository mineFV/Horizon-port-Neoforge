package net.minecraft.world.level.block.state.properties;

/**
 * The stand-in for the game's BlockStateProperties: the properties a machine sets (others aren't here yet). FACING's
 * order is the one NeoForge's error message gives (`dump`'s exporter, texts.missing_property); HORIZONTAL_FACING's is
 * checked the same way (texts.horizontal_facing).
 */
public class BlockStateProperties {
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final DirectionProperty FACING = DirectionProperty.create("facing",
            net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST,
            net.minecraft.core.Direction.UP, net.minecraft.core.Direction.DOWN);
    /** Observed (texts.horizontal_facing): the horizontal directions in Direction's order, north, south, west, east. */
    public static final DirectionProperty HORIZONTAL_FACING = DirectionProperty.create("facing", d -> d.getAxis().isHorizontal());

    private BlockStateProperties() {}
}
