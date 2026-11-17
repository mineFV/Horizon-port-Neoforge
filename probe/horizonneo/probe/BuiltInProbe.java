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

    static void run() {
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
        p("registry components", () -> (BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.withDefaultNamespace("max_stack_size")) == DataComponents.MAX_STACK_SIZE) + " "
                + BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(DataComponents.DAMAGE) + " " + BuiltInRegistries.DATA_COMPONENT_TYPE.get(nope) + " " + BuiltInRegistries.DATA_COMPONENT_TYPE
                + " " + BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(nope) + " " + BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(nope));
    }

    static void p(String key, Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
