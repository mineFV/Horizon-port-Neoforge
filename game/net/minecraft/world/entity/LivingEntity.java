package net.minecraft.world.entity;

/** The stand-in for the game's LivingEntity: the type alone (entities aren't modelled). */
public abstract class LivingEntity extends Entity implements Attackable, net.neoforged.neoforge.common.extensions.ILivingEntityExtension {
    protected LivingEntity() {}
}
