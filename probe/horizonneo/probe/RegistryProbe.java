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
        // BlockStateProperties needs the game's registries (its sounds): its LIT is BooleanProperty.create("lit").
        net.minecraft.world.level.block.state.properties.BooleanProperty lit = net.minecraft.world.level.block.state.properties.BooleanProperty.create("lit");
        net.minecraft.world.level.block.state.properties.BooleanProperty made = net.minecraft.world.level.block.state.properties.BooleanProperty.create("lit");
        p("property", () -> lit + " " + lit.getName() + " " + lit.getValueClass().getName() + " " + lit.getPossibleValues() + " " + lit.getName(true) + " " + lit.getName(false)
                + " " + lit.getValue("true") + " " + lit.getValue("TRUE") + " " + lit.getValue("x") + " " + lit.equals(made) + " " + (lit == made) + " " + (lit.hashCode() == made.hashCode())
                + " " + lit.equals(net.minecraft.world.level.block.state.properties.BooleanProperty.create("powered")) + " " + (lit.hashCode() == lit.generateHashCode()));
        // A property's hash starts from its class object's, which changes from run to run: only differences are asked.
        p("property hash", () -> (lit.generateHashCode() - net.minecraft.world.level.block.state.properties.BooleanProperty.create("x").generateHashCode()) + " " + (lit.generateHashCode() == made.generateHashCode()));
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
