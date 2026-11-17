package net.minecraft.world.item;

import net.minecraft.util.StringRepresentable;

/** The stand-in for the game's Rarity: its constants and their names (colours aren't modelled). */
public enum Rarity implements StringRepresentable, net.neoforged.fml.common.asm.enumextension.IExtensibleEnum {
    COMMON("common"),
    UNCOMMON("uncommon"),
    RARE("rare"),
    EPIC("epic");

    private final String name;

    Rarity(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
