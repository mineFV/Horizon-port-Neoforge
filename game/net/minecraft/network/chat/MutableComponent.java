package net.minecraft.network.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;

/** The stand-in for the game's MutableComponent (observed, `standins --horizon-src`). */
public class MutableComponent implements Component {
    private final ComponentContents contents;
    private final List<Component> siblings;
    private Style style;

    MutableComponent(ComponentContents contents, List<Component> siblings, Style style) {
        this.contents = contents;
        this.siblings = siblings;
        this.style = style;
    }

    public static MutableComponent create(ComponentContents contents) {
        return new MutableComponent(contents, new ArrayList<>(), Style.EMPTY);
    }

    @Override
    public ComponentContents getContents() {
        return contents;
    }

    @Override
    public List<Component> getSiblings() {
        return siblings;
    }

    public MutableComponent setStyle(Style style) {
        this.style = style;
        return this;
    }

    @Override
    public Style getStyle() {
        return style;
    }

    public MutableComponent append(String text) {
        return text.isEmpty() ? this : append(Component.literal(text));
    }

    public MutableComponent append(Component sibling) {
        siblings.add(sibling);
        return this;
    }

    public MutableComponent withStyle(UnaryOperator<Style> change) {
        return setStyle(change.apply(getStyle()));
    }

    public MutableComponent withStyle(Style style) {
        return setStyle(style.applyTo(getStyle()));
    }

    public MutableComponent withStyle(ChatFormatting... formats) {
        return setStyle(getStyle().applyFormats(formats));
    }

    public MutableComponent withStyle(ChatFormatting format) {
        return setStyle(getStyle().applyFormat(format));
    }

    public MutableComponent withColor(int rgb) {
        return setStyle(getStyle().withColor(rgb));
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof MutableComponent m && contents.equals(m.contents) && style.equals(m.style) && siblings.equals(m.siblings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contents, style, siblings);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(contents.toString());
        boolean styled = !style.isEmpty(), more = !siblings.isEmpty();
        if (styled || more) {
            b.append('[');
            if (styled) b.append("style=").append(style);
            if (styled && more) b.append(", ");
            if (more) b.append("siblings=").append(siblings);
            b.append(']');
        }
        return b.toString();
    }
}
