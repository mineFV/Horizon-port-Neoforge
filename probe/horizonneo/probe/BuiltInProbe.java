package horizonneo.probe;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/** The game's registries: ids and values, defaults, holders and keys (in a server: the registries are filled there). */
final class BuiltInProbe {
    private BuiltInProbe() {}

    /** A registry of the probe's own, made as a mod makes one, and two entries in it. */
    static final ResourceKey<net.minecraft.core.Registry<String>> WORDS_KEY = ResourceKey.createRegistryKey(ResourceLocation.parse("horizonprobe:words"));
    static final net.minecraft.core.Registry<String> WORDS = new net.neoforged.neoforge.registries.RegistryBuilder<>(WORDS_KEY)
            .defaultKey(ResourceLocation.parse("horizonprobe:none")).sync(true).maxId(100).create();
    static final net.neoforged.neoforge.registries.DeferredRegister<String> WORD_ENTRIES = net.neoforged.neoforge.registries.DeferredRegister.create(WORDS_KEY, "horizonprobe");
    static final net.neoforged.neoforge.registries.DeferredHolder<String, String> ALPHA = WORD_ENTRIES.register("alpha", () -> "alpha-value");
    static final net.neoforged.neoforge.registries.DeferredHolder<String, String> NONE = WORD_ENTRIES.register("none", () -> "none-value");

    static void run() {
        recipeTypes();
        ResourceLocation diamond = ResourceLocation.withDefaultNamespace("diamond"), nope = ResourceLocation.withDefaultNamespace("nope");
        ResourceLocation plain = ResourceLocation.parse("horizonprobe:plain");
        p("registry item", () -> BuiltInRegistries.ITEM.getKey(Items.DIAMOND) + " " + (BuiltInRegistries.ITEM.get(diamond) == Items.DIAMOND) + " " + BuiltInRegistries.ITEM.get(nope)
                + " " + BuiltInRegistries.ITEM.getOptional(nope) + " " + (BuiltInRegistries.ITEM.getOptional(diamond).orElse(null) == Items.DIAMOND) + " " + BuiltInRegistries.ITEM.containsKey(diamond)
                + " " + BuiltInRegistries.ITEM.containsKey(nope) + " " + BuiltInRegistries.ITEM.getDefaultKey() + " " + BuiltInRegistries.ITEM.getKey(ComponentProbe.PLAIN.get())
                + " " + (BuiltInRegistries.ITEM.get(plain) == ComponentProbe.PLAIN.get()) + " " + BuiltInRegistries.ITEM.getKeyOrNull(Items.DIAMOND) + " " + BuiltInRegistries.ITEM.containsValue(Items.DIAMOND));
        p("registry item keys", () -> BuiltInRegistries.ITEM.getResourceKey(Items.DIAMOND) + " " + BuiltInRegistries.ITEM.key() + " " + BuiltInRegistries.ITEM
                + " " + (BuiltInRegistries.ITEM.wrapAsHolder(Items.DIAMOND) == Items.DIAMOND.builtInRegistryHolder()) + " " + BuiltInRegistries.ITEM.getHolder(diamond).isPresent()
                + " " + BuiltInRegistries.ITEM.getHolder(nope).isPresent() + " " + (BuiltInRegistries.ITEM.getHolder(diamond).get() == Items.DIAMOND.builtInRegistryHolder())
                + " " + (BuiltInRegistries.ITEM.get(ResourceKey.create(Registries.ITEM, diamond)) == Items.DIAMOND) + " " + BuiltInRegistries.ITEM.get(ResourceKey.create(Registries.ITEM, nope))
                + " " + BuiltInRegistries.ITEM.containsKey(ResourceKey.create(Registries.ITEM, nope)));
        p("registry item missing", () -> BuiltInRegistries.ITEM.getOrThrow(ResourceKey.create(Registries.ITEM, nope)));
        p("registry item missing holder", () -> BuiltInRegistries.ITEM.getHolderOrThrow(ResourceKey.create(Registries.ITEM, nope)));
        p("registry fluid", () -> BuiltInRegistries.FLUID.getKey(Fluids.WATER) + " " + (BuiltInRegistries.FLUID.get(ResourceLocation.withDefaultNamespace("lava")) == Fluids.LAVA) + " "
                + (BuiltInRegistries.FLUID.get(nope) == Fluids.EMPTY) + " " + BuiltInRegistries.FLUID.getDefaultKey() + " " + BuiltInRegistries.FLUID);
        p("registry block", () -> BuiltInRegistries.BLOCK.getKey(CapabilityProbe.BLOCK.get()) + " " + BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace("stone"))
                + " " + BuiltInRegistries.BLOCK.get(nope) + " " + BuiltInRegistries.BLOCK.getDefaultKey() + " " + BuiltInRegistries.BLOCK.containsKey(nope)
                + " " + BuiltInRegistries.ENTITY_TYPE.getDefaultKey());
        ResourceLocation alpha = ResourceLocation.parse("horizonprobe:alpha"), other = ResourceLocation.parse("horizonprobe:other");
        p("registry own", () -> WORDS.get(alpha) + " " + WORDS.get(other) + " " + WORDS.getKey("alpha-value") + " " + WORDS.getKey("stranger") + " " + WORDS.containsKey(alpha)
                + " " + WORDS.containsKey(other) + " " + WORDS.getOptional(other) + " " + WORDS.key() + " " + WORDS + " " + ALPHA.get() + " " + WORDS.getKeyOrNull("stranger")
                + " " + WORDS.containsValue("none-value") + " " + WORDS.getResourceKey("alpha-value") + " " + WORDS.getHolder(alpha).map(h -> h.value()).orElse("?")
                + " " + ((net.minecraft.core.DefaultedRegistry<String>) WORDS).getDefaultKey());
        p("registry components", () -> (BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.withDefaultNamespace("max_stack_size")) == DataComponents.MAX_STACK_SIZE) + " "
                + BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(DataComponents.DAMAGE) + " " + BuiltInRegistries.DATA_COMPONENT_TYPE.get(nope) + " " + BuiltInRegistries.DATA_COMPONENT_TYPE
                + " " + BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(nope) + " " + BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(nope));
    }

    /**
     * The game's recipe types: their texts and classes, one object each, and the registry's ids for them; a kind of the
     * probe's own, not registered, has none.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void recipeTypes() {
        net.minecraft.world.item.crafting.RecipeType[] types = {net.minecraft.world.item.crafting.RecipeType.CRAFTING, net.minecraft.world.item.crafting.RecipeType.SMELTING,
            net.minecraft.world.item.crafting.RecipeType.BLASTING, net.minecraft.world.item.crafting.RecipeType.SMOKING, net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING,
            net.minecraft.world.item.crafting.RecipeType.STONECUTTING, net.minecraft.world.item.crafting.RecipeType.SMITHING};
        net.minecraft.core.Registry registry = BuiltInRegistries.RECIPE_TYPE;
        for (net.minecraft.world.item.crafting.RecipeType t : types) {
            p("recipe type " + t, () -> t.getClass().getName() + " " + registry.getKey(t) + " " + (registry.get(registry.getKey(t)) == t) + " " + registry.containsKey(registry.getKey(t))
                    + " " + registry.getResourceKey(t) + " " + registry.containsValue(t) + " " + t.equals(types[0]) + " " + (t.hashCode() == System.identityHashCode(t)));
        }
        net.minecraft.world.item.crafting.RecipeType own = new net.minecraft.world.item.crafting.RecipeType() {
            @Override
            public String toString() {
                return "own";
            }
        };
        p("recipe type own", () -> registry.getKey(own) + " " + registry.getResourceKey(own) + " " + registry.containsValue(own) + " " + registry.get(ResourceLocation.withDefaultNamespace("nope"))
                + " " + registry.containsKey(ResourceLocation.withDefaultNamespace("nope")) + " " + registry.getOptional(ResourceLocation.withDefaultNamespace("crafting")).isPresent());
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
