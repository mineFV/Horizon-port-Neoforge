package net.minecraft.network.chat;

/** The stand-in for the game's HoverEvent: the type alone, for Style (making one isn't modelled). */
public class HoverEvent {
    private HoverEvent() {}

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException("hover events aren't modelled");
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException("hover events aren't modelled");
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("hover events aren't modelled");
    }
}
