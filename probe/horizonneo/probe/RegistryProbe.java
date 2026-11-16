package horizonneo.probe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Registry keys and holders, as GameProbe observes the rest (see there): what needs no registry filled. */
final class RegistryProbe {
    private RegistryProbe() {}

    // Raw types: the game's keys are typed by its Registry, which the stand-ins leave out; the calls are the same.
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void run() {
        ResourceLocation stone = ResourceLocation.withDefaultNamespace("stone"), counter = ResourceLocation.fromNamespaceAndPath("counter", "counter");
        ResourceKey block = Registries.BLOCK, type = Registries.BLOCK_ENTITY_TYPE;
        p("key registries", () -> block + " " + type + " " + Registries.ITEM + " " + Registries.ROOT_REGISTRY_NAME + " " + block.location() + " " + block.registry());
        ResourceKey a = ResourceKey.create(block, stone), b = ResourceKey.create(block, stone), c = ResourceKey.create(type, counter);
        p("key made", () -> a + " " + (a == b) + " " + a.equals(b) + " " + a.isFor(block) + " " + a.isFor(type) + " " + c + " " + a.location() + " " + a.registry());
        p("key order", () -> a.compareTo(c) + " " + c.compareTo(a) + " " + a.compareTo(b) + " " + ResourceKey.create(block, counter).compareTo(a));
        p("key registry made", () -> (ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("block")) == block) + " " + ResourceKey.createRegistryKey(counter));
        // A DeferredHolder binds to the game's registries when it is made, which need the game started: not asked here.
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
