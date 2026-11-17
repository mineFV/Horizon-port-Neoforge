package net.minecraft.network.chat;

import net.minecraft.ChatFormatting;

/** The stand-in for the game's TextColor: a colour and, for a legacy one, its name (observed, `standins --horizon-src`). */
public final class TextColor {
    private final int value;
    private final String name;

    private TextColor(int value, String name) {
        this.value = value & 0xFFFFFF;
        this.name = name;
    }

    private static final java.util.Map<ChatFormatting, TextColor> LEGACY = new java.util.EnumMap<>(ChatFormatting.class);

    static {
        for (ChatFormatting f : ChatFormatting.values()) if (f.isColor()) LEGACY.put(f, new TextColor(f.getColor(), f.getName()));
    }

    public int getValue() {
        return value;
    }

    public String serialize() {
        if (name != null) return name;
        char[] out = new char[7];
        out[0] = '#';
        for (int i = 0; i < 6; i++) out[6 - i] = "0123456789ABCDEF".charAt((value >> (4 * i)) & 15);
        return new String(out);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof TextColor c && value == c.value;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value, name);
    }

    @Override
    public String toString() {
        return serialize();
    }

    public static TextColor fromLegacyFormat(ChatFormatting formatting) {
        return LEGACY.get(formatting);
    }

    public static TextColor fromRgb(int rgb) {
        return new TextColor(rgb, null);
    }
}
