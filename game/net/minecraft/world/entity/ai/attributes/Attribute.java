package net.minecraft.world.entity.ai.attributes;

/**
 * The stand-in for the game's Attribute: its description id, default value, whether clients get it and how a change
 * of it reads (observed in a server, `standins --horizon-src`). What it does to an entity is Horizon's.
 */
public class Attribute implements net.neoforged.neoforge.common.extensions.IAttributeExtension {
    private final String descriptionId;
    private final double defaultValue;
    private boolean syncable;
    private Sentiment sentiment = Sentiment.POSITIVE;

    protected Attribute(String descriptionId, double defaultValue) {
        this.descriptionId = descriptionId;
        this.defaultValue = defaultValue;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public boolean isClientSyncable() {
        return syncable;
    }

    public Attribute setSyncable(boolean syncable) {
        this.syncable = syncable;
        return this;
    }

    public Attribute setSentiment(Sentiment sentiment) {
        this.sentiment = sentiment;
        return this;
    }

    public double sanitizeValue(double value) {
        return value;
    }

    public String getDescriptionId() {
        return descriptionId;
    }

    public net.minecraft.ChatFormatting getStyle(boolean positive) {
        return sentiment.getStyle(positive);
    }

    /** The stand-in for Attribute.Sentiment: whether more of it is good, bad or neither, and its colour. */
    public enum Sentiment {
        POSITIVE,
        NEUTRAL,
        NEGATIVE;

        /** Observed: blue for a good change, red for a bad one, gray for neither. */
        public net.minecraft.ChatFormatting getStyle(boolean positive) {
            return switch (this) {
                case POSITIVE -> positive ? net.minecraft.ChatFormatting.BLUE : net.minecraft.ChatFormatting.RED;
                case NEUTRAL -> net.minecraft.ChatFormatting.GRAY;
                case NEGATIVE -> positive ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.BLUE;
            };
        }
    }
}
