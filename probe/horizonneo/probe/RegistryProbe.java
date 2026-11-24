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
        enchantments();
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
        net.minecraft.world.level.block.state.properties.DirectionProperty all = net.minecraft.world.level.block.state.properties.DirectionProperty.create("facing");
        net.minecraft.world.level.block.state.properties.DirectionProperty four = net.minecraft.world.level.block.state.properties.DirectionProperty.create("facing",
                net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST);
        net.minecraft.world.level.block.state.properties.DirectionProperty listed = net.minecraft.world.level.block.state.properties.DirectionProperty.create("facing",
                java.util.List.of(net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.NORTH));
        net.minecraft.world.level.block.state.properties.DirectionProperty some = net.minecraft.world.level.block.state.properties.DirectionProperty.create("facing", d -> d.getAxis().isHorizontal());
        p("direction property", () -> all + " | " + four + " | " + listed + " | " + some + " | " + all.getName(net.minecraft.core.Direction.UP) + " " + all.getValue("up") + " " + all.getValue("UP")
                + " " + four.getValue("up") + " " + all.equals(four) + " " + four.equals(some) + " " + four.equals(listed) + " " + (four.hashCode() == some.hashCode()) + " " + all.getPossibleValues().getClass().getSimpleName().isEmpty());
        // A property's hash starts from its class object's, which changes from run to run: only differences are asked.
        p("property hash", () -> (lit.generateHashCode() - net.minecraft.world.level.block.state.properties.BooleanProperty.create("x").generateHashCode()) + " " + (lit.generateHashCode() == made.generateHashCode()));
    }

    /** The game's enchantments' keys: texts, one object each, made as ResourceKey.create makes them. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void enchantments() {
        Object[] keys = {net.minecraft.world.item.enchantment.Enchantments.PROTECTION, net.minecraft.world.item.enchantment.Enchantments.FIRE_PROTECTION, net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING, net.minecraft.world.item.enchantment.Enchantments.BLAST_PROTECTION, net.minecraft.world.item.enchantment.Enchantments.PROJECTILE_PROTECTION, net.minecraft.world.item.enchantment.Enchantments.RESPIRATION, net.minecraft.world.item.enchantment.Enchantments.AQUA_AFFINITY, net.minecraft.world.item.enchantment.Enchantments.THORNS, net.minecraft.world.item.enchantment.Enchantments.DEPTH_STRIDER, net.minecraft.world.item.enchantment.Enchantments.FROST_WALKER, net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE, net.minecraft.world.item.enchantment.Enchantments.SOUL_SPEED, net.minecraft.world.item.enchantment.Enchantments.SWIFT_SNEAK, net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, net.minecraft.world.item.enchantment.Enchantments.SMITE, net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS, net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK, net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, net.minecraft.world.item.enchantment.Enchantments.LOOTING, net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE, net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY, net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, net.minecraft.world.item.enchantment.Enchantments.FORTUNE, net.minecraft.world.item.enchantment.Enchantments.POWER, net.minecraft.world.item.enchantment.Enchantments.PUNCH, net.minecraft.world.item.enchantment.Enchantments.FLAME, net.minecraft.world.item.enchantment.Enchantments.INFINITY, net.minecraft.world.item.enchantment.Enchantments.LUCK_OF_THE_SEA, net.minecraft.world.item.enchantment.Enchantments.LURE, net.minecraft.world.item.enchantment.Enchantments.LOYALTY, net.minecraft.world.item.enchantment.Enchantments.IMPALING, net.minecraft.world.item.enchantment.Enchantments.RIPTIDE, net.minecraft.world.item.enchantment.Enchantments.CHANNELING, net.minecraft.world.item.enchantment.Enchantments.MULTISHOT, net.minecraft.world.item.enchantment.Enchantments.QUICK_CHARGE, net.minecraft.world.item.enchantment.Enchantments.PIERCING, net.minecraft.world.item.enchantment.Enchantments.DENSITY, net.minecraft.world.item.enchantment.Enchantments.BREACH, net.minecraft.world.item.enchantment.Enchantments.WIND_BURST, net.minecraft.world.item.enchantment.Enchantments.MENDING, net.minecraft.world.item.enchantment.Enchantments.VANISHING_CURSE};
        p("enchantment keys", () -> {
            StringBuilder b = new StringBuilder();
            for (Object k : keys) b.append(k).append(' ');
            ResourceKey silk = net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH;
            return b.append(silk == ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("silk_touch"))).append(' ').append(Registries.ENCHANTMENT)
                    .append(' ').append(silk.location()).append(' ').append(silk.registry()).append(' ').append(silk.isFor(Registries.ENCHANTMENT)).append(' ').append(new net.minecraft.world.item.enchantment.Enchantments() != null);
        });
    }

    static void p(String key, java.util.function.Supplier<Object> call) {
        GameProbe.p(key, call);
    }
}
