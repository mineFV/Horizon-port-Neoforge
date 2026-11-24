package net.minecraft.world.item.crafting;

import net.minecraft.resources.ResourceLocation;

/** Not the game's: the game's recipe types by their ids in BuiltInRegistries.RECIPE_TYPE, and their ids. */
public final class HorizonRecipeTypes {
    private HorizonRecipeTypes() {}

    private static final RecipeType<?>[] GAME = {RecipeType.CRAFTING, RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING,
        RecipeType.STONECUTTING, RecipeType.SMITHING};

    /** The game's recipe type of an id ("minecraft:crafting"); null when the game has none of that id. */
    public static RecipeType<?> byId(String id) {
        for (RecipeType<?> t : GAME) if (("minecraft:" + t).equals(id)) return t;
        return null;
    }

    /** The id of one of the game's recipe types; null for any other object. */
    public static ResourceLocation idOf(Object value) {
        for (RecipeType<?> t : GAME) if (t == value) return ResourceLocation.withDefaultNamespace(t.toString());
        return null;
    }
}
