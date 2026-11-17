package net.minecraft.network.chat;

/** The stand-in for the game's CommonComponents: those of plain text (observed, `standins --horizon-src`). */
public class CommonComponents {
    public static final Component EMPTY = Component.empty();
    public static final Component NEW_LINE = Component.literal("\n");
    public static final Component ELLIPSIS = Component.literal("...");
    public static final Component SPACE = Component.literal(" ");

    public CommonComponents() {}

    public static MutableComponent space() {
        return Component.literal(" ");
    }
}
