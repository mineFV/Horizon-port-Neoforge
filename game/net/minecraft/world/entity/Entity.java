package net.minecraft.world.entity;

/** The stand-in for the game's Entity: the type alone, for the signatures that name it (entities aren't modelled). */
public abstract class Entity extends net.neoforged.neoforge.attachment.AttachmentHolder implements net.minecraft.network.syncher.SyncedDataHolder, net.minecraft.world.Nameable,
        net.minecraft.world.level.entity.EntityAccess, net.minecraft.commands.CommandSource, net.minecraft.world.scores.ScoreHolder, net.neoforged.neoforge.common.extensions.IEntityExtension {
    protected Entity() {}

    /** The game's compares entities' ids: entities aren't modelled. */
    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException("entities aren't modelled");
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException("entities aren't modelled");
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("entities aren't modelled");
    }
}
