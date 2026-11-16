package net.minecraft.world.level.block.state;

/** The stand-in for the game's StateHolder: the owner of a state (its block). Properties aren't modelled. */
public abstract class StateHolder<O, S> {
    protected final O owner;

    protected StateHolder(O owner) {
        this.owner = owner;
    }

    /** Observed (`dump`'s exporter): its owner's text, for a state without properties (none is modelled). */
    @Override
    public String toString() {
        return String.valueOf(owner);
    }
}
