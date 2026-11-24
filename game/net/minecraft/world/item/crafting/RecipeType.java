package net.minecraft.world.item.crafting;

/**
 * The stand-in for the game's RecipeType: a kind of recipe, and the game's seven, each an object of its own whose text
 * is its name (observed, `standins`), held by BuiltInRegistries.RECIPE_TYPE under the game's namespace
 * (HorizonRecipeTypes), as the game registers them. Registering one more (register) isn't here: a mod makes its own
 * kinds and registers them itself.
 */
public interface RecipeType<T> {
    RecipeType<?> CRAFTING = make("crafting");
    RecipeType<?> SMELTING = make("smelting");
    RecipeType<?> BLASTING = make("blasting");
    RecipeType<?> SMOKING = make("smoking");
    RecipeType<?> CAMPFIRE_COOKING = make("campfire_cooking");
    RecipeType<?> STONECUTTING = make("stonecutting");
    RecipeType<?> SMITHING = make("smithing");

    /** One of the game's: an object of a class of its own (RecipeType$1), its text its name. */
    private static <T> RecipeType<T> make(String identifier) {
        return new RecipeType<T>() {
            @Override
            public String toString() {
                return identifier;
            }
        };
    }
}
