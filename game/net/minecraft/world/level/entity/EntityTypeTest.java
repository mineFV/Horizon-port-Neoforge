package net.minecraft.world.level.entity;

/** The stand-in for the game's EntityTypeTest: what tells an entity of a type (entities aren't modelled). */
public interface EntityTypeTest<B, T extends B> {
    T tryCast(B value);

    Class<? extends B> getBaseClass();
}
