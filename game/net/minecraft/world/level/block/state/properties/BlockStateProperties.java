package net.minecraft.world.level.block.state.properties;

/** The stand-in for the game's BlockStateProperties: its boolean properties a machine sets (others aren't here yet). */
public class BlockStateProperties {
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    private BlockStateProperties() {}
}
