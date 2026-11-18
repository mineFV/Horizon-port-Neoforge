package net.minecraft.world.item;

import net.minecraft.world.level.material.MapColor;

/** The stand-in for the game's DyeColor: each dye's id, name and colours (observed, `standins --horizon-src`). */
public enum DyeColor implements net.minecraft.util.StringRepresentable {
    WHITE(0, "white", -393218, MapColor.SNOW, 15790320, 16777215),
    ORANGE(1, "orange", -425955, MapColor.COLOR_ORANGE, 15435844, 16738335),
    MAGENTA(2, "magenta", -3715395, MapColor.COLOR_MAGENTA, 12801229, 16711935),
    LIGHT_BLUE(3, "light_blue", -12930086, MapColor.COLOR_LIGHT_BLUE, 6719955, 10141901),
    YELLOW(4, "yellow", -75715, MapColor.COLOR_YELLOW, 14602026, 16776960),
    LIME(5, "lime", -8337633, MapColor.COLOR_LIGHT_GREEN, 4312372, 12582656),
    PINK(6, "pink", -816214, MapColor.COLOR_PINK, 14188952, 16738740),
    GRAY(7, "gray", -12103854, MapColor.COLOR_GRAY, 4408131, 8421504),
    LIGHT_GRAY(8, "light_gray", -6447721, MapColor.COLOR_LIGHT_GRAY, 11250603, 13882323),
    CYAN(9, "cyan", -15295332, MapColor.COLOR_CYAN, 2651799, 65535),
    PURPLE(10, "purple", -7785800, MapColor.COLOR_PURPLE, 8073150, 10494192),
    BLUE(11, "blue", -12827478, MapColor.COLOR_BLUE, 2437522, 255),
    BROWN(12, "brown", -8170446, MapColor.COLOR_BROWN, 5320730, 9127187),
    GREEN(13, "green", -10585066, MapColor.COLOR_GREEN, 3887386, 65280),
    RED(14, "red", -5231066, MapColor.COLOR_RED, 11743532, 16711680),
    BLACK(15, "black", -14869215, MapColor.COLOR_BLACK, 1973019, 0);

    private final int id;
    private final String name;
    private final int textureDiffuseColor;
    private final MapColor mapColor;
    private final int fireworkColor;
    private final int textColor;

    DyeColor(int id, String name, int textureDiffuseColor, MapColor mapColor, int fireworkColor, int textColor) {
        this.id = id;
        this.name = name;
        this.textureDiffuseColor = textureDiffuseColor;
        this.mapColor = mapColor;
        this.fireworkColor = fireworkColor;
        this.textColor = textColor;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTextureDiffuseColor() {
        return textureDiffuseColor;
    }

    public MapColor getMapColor() {
        return mapColor;
    }

    public int getFireworkColor() {
        return fireworkColor;
    }

    public int getTextColor() {
        return textColor;
    }

    /** Observed: an id out of 0 to 15 is WHITE. */
    public static DyeColor byId(int id) {
        return id >= 0 && id < 16 ? values()[id] : WHITE;
    }

    /** Observed: by its name, as it is; else the fallback. */
    public static DyeColor byName(String name, DyeColor fallback) {
        for (DyeColor d : values()) if (d.name.equals(name)) return d;
        return fallback;
    }

    /** Observed: the dye of that firework colour, else null. */
    public static DyeColor byFireworkColor(int color) {
        for (DyeColor d : values()) if (d.fireworkColor == color) return d;
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
