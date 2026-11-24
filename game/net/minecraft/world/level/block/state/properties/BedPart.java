package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

/** The stand-in for the game's BedPart: a bed's two parts and their names (observed). */
public enum BedPart implements StringRepresentable {
    HEAD("head"),
    FOOT("foot");

    private final String name;

    BedPart(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
