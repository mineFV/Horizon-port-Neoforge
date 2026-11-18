package net.neoforged.neoforge.common;

/** The stand-in for NeoForge's SoundActions: the actions it names. */
public final class SoundActions {
    public static final SoundAction BUCKET_FILL = SoundAction.get("bucket_fill");
    public static final SoundAction BUCKET_EMPTY = SoundAction.get("bucket_empty");
    public static final SoundAction FLUID_VAPORIZE = SoundAction.get("fluid_vaporize");
    public static final SoundAction CAULDRON_DRIP = SoundAction.get("cauldron_drip");

    private SoundActions() {}
}
