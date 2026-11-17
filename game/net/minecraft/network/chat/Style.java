package net.minecraft.network.chat;

import java.util.Objects;
import net.minecraft.ChatFormatting;

/**
 * The stand-in for the game's Style: a colour and the formats (bold, italic, underlined, strikethrough, obfuscated),
 * each set or not. Click and hover events, insertions and fonts aren't modelled. Texts observed (`standins --horizon-src`).
 */
public class Style {
    public static final Style EMPTY = new Style(null, null, null, null, null, null);

    final TextColor color;
    final Boolean bold;
    final Boolean italic;
    final Boolean underlined;
    final Boolean strikethrough;
    final Boolean obfuscated;

    private Style(TextColor color, Boolean bold, Boolean italic, Boolean underlined, Boolean strikethrough, Boolean obfuscated) {
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
    }

    private static Style of(TextColor color, Boolean bold, Boolean italic, Boolean underlined, Boolean strikethrough, Boolean obfuscated) {
        if (color == null && bold == null && italic == null && underlined == null && strikethrough == null && obfuscated == null) return EMPTY;
        return new Style(color, bold, italic, underlined, strikethrough, obfuscated);
    }

    public TextColor getColor() {
        return color;
    }

    public boolean isBold() {
        return bold == Boolean.TRUE;
    }

    public boolean isItalic() {
        return italic == Boolean.TRUE;
    }

    public boolean isStrikethrough() {
        return strikethrough == Boolean.TRUE;
    }

    public boolean isUnderlined() {
        return underlined == Boolean.TRUE;
    }

    public boolean isObfuscated() {
        return obfuscated == Boolean.TRUE;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public Style withColor(TextColor color) {
        return Objects.equals(this.color, color) ? this : of(color, bold, italic, underlined, strikethrough, obfuscated);
    }

    public Style withColor(ChatFormatting formatting) {
        return withColor(formatting != null ? TextColor.fromLegacyFormat(formatting) : null);
    }

    public Style withColor(int rgb) {
        return withColor(TextColor.fromRgb(rgb));
    }

    public Style withBold(Boolean bold) {
        return Objects.equals(this.bold, bold) ? this : of(color, bold, italic, underlined, strikethrough, obfuscated);
    }

    public Style withItalic(Boolean italic) {
        return Objects.equals(this.italic, italic) ? this : of(color, bold, italic, underlined, strikethrough, obfuscated);
    }

    public Style withUnderlined(Boolean underlined) {
        return Objects.equals(this.underlined, underlined) ? this : of(color, bold, italic, underlined, strikethrough, obfuscated);
    }

    public Style withStrikethrough(Boolean strikethrough) {
        return Objects.equals(this.strikethrough, strikethrough) ? this : of(color, bold, italic, underlined, strikethrough, obfuscated);
    }

    public Style withObfuscated(Boolean obfuscated) {
        return Objects.equals(this.obfuscated, obfuscated) ? this : of(color, bold, italic, underlined, strikethrough, obfuscated);
    }

    /** A format sets its flag; a colour, the colour; RESET, nothing at all. */
    public Style applyFormat(ChatFormatting f) {
        TextColor c = color;
        Boolean b = bold, i = italic, s = strikethrough, u = underlined, o = obfuscated;
        switch (f) {
            case OBFUSCATED -> o = true;
            case BOLD -> b = true;
            case STRIKETHROUGH -> s = true;
            case UNDERLINE -> u = true;
            case ITALIC -> i = true;
            case RESET -> {
                return EMPTY;
            }
            default -> c = TextColor.fromLegacyFormat(f);
        }
        return of(c, b, i, u, s, o);
    }

    public Style applyFormats(ChatFormatting... formats) {
        Style s = this;
        for (ChatFormatting f : formats) s = s.applyFormat(f);
        return s;
    }

    /** This style over another: what this one doesn't set, the other's. */
    public Style applyTo(Style other) {
        if (this == EMPTY) return other;
        if (other == EMPTY) return this;
        return of(color != null ? color : other.color, bold != null ? bold : other.bold, italic != null ? italic : other.italic,
                underlined != null ? underlined : other.underlined, strikethrough != null ? strikethrough : other.strikethrough,
                obfuscated != null ? obfuscated : other.obfuscated);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("{");
        boolean[] first = {true};
        field(b, first, "color", color);
        field(b, first, "bold", bold);
        field(b, first, "italic", italic);
        field(b, first, "underlined", underlined);
        field(b, first, "strikethrough", strikethrough);
        field(b, first, "obfuscated", obfuscated);
        return b.append('}').toString();
    }

    /** Observed: a colour as color=its text, a format as its name when set, !its name when unset. */
    private static void field(StringBuilder b, boolean[] first, String name, Object value) {
        if (value == null) return;
        if (!first[0]) b.append(',');
        first[0] = false;
        if (value instanceof Boolean set) b.append(set ? "" : "!").append(name);
        else b.append(name).append('=').append(value);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Style s && Objects.equals(color, s.color) && Objects.equals(bold, s.bold) && Objects.equals(italic, s.italic)
                && Objects.equals(underlined, s.underlined) && Objects.equals(strikethrough, s.strikethrough) && Objects.equals(obfuscated, s.obfuscated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, bold, italic, underlined, strikethrough, obfuscated);
    }
}
