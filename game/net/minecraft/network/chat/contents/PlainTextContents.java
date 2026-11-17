package net.minecraft.network.chat.contents;

import net.minecraft.network.chat.ComponentContents;

/** The stand-in for the game's PlainTextContents: a literal text (observed, `standins --horizon-src`). */
public interface PlainTextContents extends ComponentContents {
    PlainTextContents EMPTY = new PlainTextContents() {
        @Override
        public String text() {
            return "";
        }

        @Override
        public String toString() {
            return "empty";
        }
    };

    static PlainTextContents create(String text) {
        return text.isEmpty() ? EMPTY : new LiteralContents(text);
    }

    String text();

    record LiteralContents(String text) implements PlainTextContents {
        @Override
        public String toString() {
            return "literal{" + text + "}";
        }
    }
}
