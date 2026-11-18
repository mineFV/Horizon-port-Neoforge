package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.MapColor;

/** The game's map colours and dye colours: their ids, colours and names (in a server: the game's are made there). */
final class ColorProbe {
    private ColorProbe() {}

    static void run() {
        MapColor[] named = {MapColor.NONE, MapColor.GRASS, MapColor.SAND, MapColor.WOOL, MapColor.FIRE, MapColor.ICE, MapColor.METAL, MapColor.PLANT, MapColor.SNOW, MapColor.CLAY, MapColor.DIRT, MapColor.STONE, MapColor.WATER, MapColor.WOOD, MapColor.QUARTZ, MapColor.COLOR_ORANGE, MapColor.COLOR_MAGENTA, MapColor.COLOR_LIGHT_BLUE, MapColor.COLOR_YELLOW, MapColor.COLOR_LIGHT_GREEN, MapColor.COLOR_PINK, MapColor.COLOR_GRAY, MapColor.COLOR_LIGHT_GRAY, MapColor.COLOR_CYAN, MapColor.COLOR_PURPLE, MapColor.COLOR_BLUE, MapColor.COLOR_BROWN, MapColor.COLOR_GREEN, MapColor.COLOR_RED, MapColor.COLOR_BLACK, MapColor.GOLD, MapColor.DIAMOND, MapColor.LAPIS, MapColor.EMERALD, MapColor.PODZOL, MapColor.NETHER, MapColor.TERRACOTTA_WHITE, MapColor.TERRACOTTA_ORANGE, MapColor.TERRACOTTA_MAGENTA, MapColor.TERRACOTTA_LIGHT_BLUE, MapColor.TERRACOTTA_YELLOW, MapColor.TERRACOTTA_LIGHT_GREEN, MapColor.TERRACOTTA_PINK, MapColor.TERRACOTTA_GRAY, MapColor.TERRACOTTA_LIGHT_GRAY, MapColor.TERRACOTTA_CYAN, MapColor.TERRACOTTA_PURPLE, MapColor.TERRACOTTA_BLUE, MapColor.TERRACOTTA_BROWN, MapColor.TERRACOTTA_GREEN, MapColor.TERRACOTTA_RED, MapColor.TERRACOTTA_BLACK, MapColor.CRIMSON_NYLIUM, MapColor.CRIMSON_STEM, MapColor.CRIMSON_HYPHAE, MapColor.WARPED_NYLIUM, MapColor.WARPED_STEM, MapColor.WARPED_HYPHAE, MapColor.WARPED_WART_BLOCK, MapColor.DEEPSLATE, MapColor.RAW_IRON, MapColor.GLOW_LICHEN};
        String[] names = {"NONE", "GRASS", "SAND", "WOOL", "FIRE", "ICE", "METAL", "PLANT", "SNOW", "CLAY", "DIRT", "STONE", "WATER", "WOOD", "QUARTZ", "COLOR_ORANGE", "COLOR_MAGENTA", "COLOR_LIGHT_BLUE", "COLOR_YELLOW", "COLOR_LIGHT_GREEN", "COLOR_PINK", "COLOR_GRAY", "COLOR_LIGHT_GRAY", "COLOR_CYAN", "COLOR_PURPLE", "COLOR_BLUE", "COLOR_BROWN", "COLOR_GREEN", "COLOR_RED", "COLOR_BLACK", "GOLD", "DIAMOND", "LAPIS", "EMERALD", "PODZOL", "NETHER", "TERRACOTTA_WHITE", "TERRACOTTA_ORANGE", "TERRACOTTA_MAGENTA", "TERRACOTTA_LIGHT_BLUE", "TERRACOTTA_YELLOW", "TERRACOTTA_LIGHT_GREEN", "TERRACOTTA_PINK", "TERRACOTTA_GRAY", "TERRACOTTA_LIGHT_GRAY", "TERRACOTTA_CYAN", "TERRACOTTA_PURPLE", "TERRACOTTA_BLUE", "TERRACOTTA_BROWN", "TERRACOTTA_GREEN", "TERRACOTTA_RED", "TERRACOTTA_BLACK", "CRIMSON_NYLIUM", "CRIMSON_STEM", "CRIMSON_HYPHAE", "WARPED_NYLIUM", "WARPED_STEM", "WARPED_HYPHAE", "WARPED_WART_BLOCK", "DEEPSLATE", "RAW_IRON", "GLOW_LICHEN"};
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < named.length; i++) b.append(names[i]).append('=').append(named[i].id).append('/').append(named[i].col).append(' ');
        p("map colors", () -> b);
        p("map colors by id", () -> {
            StringBuilder o = new StringBuilder();
            for (int i = 0; i < 64; i++) o.append(MapColor.byId(i).id).append(' ');
            return o;
        });
        int[] outside = {-1, 64, 65, 300};
        for (int i : outside) p("map color by id " + i, () -> MapColor.byId(i).id);
        p("map brightness", () -> {
            StringBuilder o = new StringBuilder();
            for (MapColor.Brightness br : MapColor.Brightness.values()) o.append(br).append(' ').append(br.id).append(' ').append(br.modifier).append(' ')
                    .append(MapColor.GRASS.calculateRGBColor(br)).append(' ').append(MapColor.GRASS.getPackedId(br)).append(' ').append(MapColor.byId(0).calculateRGBColor(br)).append(" | ");
            return o.append(MapColor.getColorFromPackedId(29)).append(' ').append(MapColor.getColorFromPackedId(-3)).append(' ').append(MapColor.getColorFromPackedId(250))
                    .append(' ').append(MapColor.Brightness.byId(2)).append(' ').append(MapColor.NONE.equals(MapColor.byId(0)));
        });
        for (DyeColor d : DyeColor.values()) {
            p("dye " + d.name(), () -> d + " " + d.getId() + " " + d.getName() + " " + d.getSerializedName() + " " + d.getTextureDiffuseColor() + " " + d.getMapColor().id + " "
                    + d.getFireworkColor() + " " + d.getTextColor() + " " + (DyeColor.byId(d.getId()) == d) + " " + (DyeColor.byName(d.getName(), null) == d)
                    + " " + (DyeColor.byFireworkColor(d.getFireworkColor()) == d));
        }
        p("dye edges", () -> DyeColor.byId(-1) + " " + DyeColor.byId(16) + " " + DyeColor.byName("nope", DyeColor.RED) + " " + DyeColor.byName("WHITE", null) + " " + DyeColor.byFireworkColor(5));
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
