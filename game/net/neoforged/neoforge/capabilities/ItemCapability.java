package net.neoforged.neoforge.capabilities;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The stand-in for NeoForge's ItemCapability: one for each name, and its providers for each item, asked in the order
 * they were registered until one gives something (observed, `standins --horizon-src`).
 */
public final class ItemCapability<T, C> extends BaseCapability<T, C> {
    final Map<Item, List<ICapabilityProvider<ItemStack, C, T>>> providers = new IdentityHashMap<>();

    private ItemCapability(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
        super(name, typeClass, contextClass);
    }

    public static <T, C> ItemCapability<T, C> create(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
        return HorizonCapabilities.made("item", name, typeClass, contextClass, () -> new ItemCapability<>(name, typeClass, contextClass));
    }

    public static <T> ItemCapability<T, Void> createVoid(ResourceLocation name, Class<T> typeClass) {
        return create(name, typeClass, void.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static synchronized List<ItemCapability<?, ?>> getAll() {
        return (List) HorizonCapabilities.all(ItemCapability.class);
    }

    public T getCapability(ItemStack stack, C context) {
        List<ICapabilityProvider<ItemStack, C, T>> list;
        synchronized (this) {
            list = providers.get(stack.getItem());
            list = list == null ? List.of() : new ArrayList<>(list);
        }
        for (ICapabilityProvider<ItemStack, C, T> p : list) {
            T found = p.getCapability(stack, context);
            if (found != null) return found;
        }
        return null;
    }
}
