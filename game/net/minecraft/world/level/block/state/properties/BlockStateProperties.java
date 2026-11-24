package net.minecraft.world.level.block.state.properties;

/**
 * The stand-in for the game's BlockStateProperties: the properties a machine sets (others aren't here yet). FACING's
 * order is the one NeoForge's error message gives (`dump`'s exporter, texts.missing_property); HORIZONTAL_FACING's is
 * checked the same way (texts.horizontal_facing).
 */
public class BlockStateProperties {
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
    public static final BooleanProperty FALLING = BooleanProperty.create("falling");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    // Each enum's constants given in their order, as create(name, class) takes them: asking a class for its constants
    // (getEnumConstants) would need every enum's values() in the translation, and this class is initialized everywhere.
    public static final EnumProperty<DoubleBlockHalf> DOUBLE_BLOCK_HALF = EnumProperty.create("half", DoubleBlockHalf.class, DoubleBlockHalf.UPPER, DoubleBlockHalf.LOWER);
    public static final EnumProperty<BedPart> BED_PART = EnumProperty.create("part", BedPart.class, BedPart.HEAD, BedPart.FOOT);
    public static final DirectionProperty FACING = DirectionProperty.create("facing",
            net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST,
            net.minecraft.core.Direction.UP, net.minecraft.core.Direction.DOWN);
    /** Observed (texts.horizontal_facing): the horizontal directions in Direction's order, north, south, west, east. */
    public static final DirectionProperty HORIZONTAL_FACING = DirectionProperty.create("facing", d -> d.getAxis().isHorizontal());

    private BlockStateProperties() {}
}
