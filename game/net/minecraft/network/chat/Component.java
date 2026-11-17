package net.minecraft.network.chat;

import java.util.List;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * The stand-in for the game's Component: contents, a style and siblings. Its plain text (getString) is its contents'
 * then its siblings', a translation's from the game's language (Language), observed (`standins --horizon-src`).
 */
public interface Component extends com.mojang.brigadier.Message, FormattedText {
    Style getStyle();

    ComponentContents getContents();

    List<Component> getSiblings();

    @Override
    default String getString() {
        StringBuilder b = new StringBuilder();
        HorizonText.append(this, b);
        return b.toString();
    }

    default MutableComponent plainCopy() {
        return MutableComponent.create(getContents());
    }

    default MutableComponent copy() {
        return new MutableComponent(getContents(), new java.util.ArrayList<>(getSiblings()), getStyle());
    }

    static Component nullToEmpty(String text) {
        return text != null ? literal(text) : CommonComponents.EMPTY;
    }

    static MutableComponent literal(String text) {
        return MutableComponent.create(PlainTextContents.create(text));
    }

    static MutableComponent translatable(String key) {
        return MutableComponent.create(new TranslatableContents(key, null, TranslatableContents.NO_ARGS));
    }

    static MutableComponent translatable(String key, Object... args) {
        return MutableComponent.create(new TranslatableContents(key, null, args));
    }

    static MutableComponent translatableWithFallback(String key, String fallback) {
        return MutableComponent.create(new TranslatableContents(key, fallback, TranslatableContents.NO_ARGS));
    }

    static MutableComponent translatableWithFallback(String key, String fallback, Object... args) {
        return MutableComponent.create(new TranslatableContents(key, fallback, args));
    }

    static MutableComponent empty() {
        return MutableComponent.create(PlainTextContents.EMPTY);
    }

    static Component translationArg(net.minecraft.resources.ResourceLocation id) {
        return literal(id.toString());
    }

    static Component translationArg(java.util.UUID id) {
        return literal(id.toString());
    }
}
