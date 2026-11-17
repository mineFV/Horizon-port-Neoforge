package net.minecraft;

import net.minecraft.util.StringRepresentable;

/** The stand-in for the game's ChatFormatting: its codes, ids and colours (observed, `standins --horizon-src`). */
public enum ChatFormatting implements StringRepresentable {
    BLACK("black", '0', 0, 0),
    DARK_BLUE("dark_blue", '1', 1, 0xAA),
    DARK_GREEN("dark_green", '2', 2, 0xAA00),
    DARK_AQUA("dark_aqua", '3', 3, 0xAAAA),
    DARK_RED("dark_red", '4', 4, 0xAA0000),
    DARK_PURPLE("dark_purple", '5', 5, 0xAA00AA),
    GOLD("gold", '6', 6, 0xFFAA00),
    GRAY("gray", '7', 7, 0xAAAAAA),
    DARK_GRAY("dark_gray", '8', 8, 0x555555),
    BLUE("blue", '9', 9, 0x5555FF),
    GREEN("green", 'a', 10, 0x55FF55),
    AQUA("aqua", 'b', 11, 0x55FFFF),
    RED("red", 'c', 12, 0xFF5555),
    LIGHT_PURPLE("light_purple", 'd', 13, 0xFF55FF),
    YELLOW("yellow", 'e', 14, 0xFFFF55),
    WHITE("white", 'f', 15, 0xFFFFFF),
    OBFUSCATED("obfuscated", 'k', true),
    BOLD("bold", 'l', true),
    STRIKETHROUGH("strikethrough", 'm', true),
    UNDERLINE("underline", 'n', true),
    ITALIC("italic", 'o', true),
    RESET("reset", 'r', -1, null);

    public static final char PREFIX_CODE = '§';

    private final String name;
    private final char code;
    private final boolean isFormat;
    private final String toString;
    private final int id;
    private final Integer color;

    ChatFormatting(String name, char code, int id, Integer color) {
        this(name, code, false, id, color);
    }

    ChatFormatting(String name, char code, boolean isFormat) {
        this(name, code, isFormat, -1, null);
    }

    ChatFormatting(String name, char code, boolean isFormat, int id, Integer color) {
        this.name = name;
        this.code = code;
        this.isFormat = isFormat;
        this.id = id;
        this.color = color;
        this.toString = "§" + code;
    }

    public char getChar() {
        return code;
    }

    public int getId() {
        return id;
    }

    public boolean isFormat() {
        return isFormat;
    }

    public boolean isColor() {
        return !isFormat && this != RESET;
    }

    public Integer getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return toString;
    }

    public static ChatFormatting getById(int id) {
        if (id < 0) return RESET;
        for (ChatFormatting f : values()) if (f.id == id) return f;
        return null;
    }

    public static ChatFormatting getByCode(char code) {
        char lower = code >= 'A' && code <= 'Z' ? (char) (code + 32) : code;
        for (ChatFormatting f : values()) if (f.code == lower) return f;
        return null;
    }

    @Override
    public String getSerializedName() {
        return getName();
    }
}
