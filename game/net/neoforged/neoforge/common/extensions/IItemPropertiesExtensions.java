package net.neoforged.neoforge.common.extensions;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;

/** The stand-in for NeoForge's IItemPropertiesExtensions: a component by a supplier of its type. */
public interface IItemPropertiesExtensions {
    default <T> Item.Properties component(Supplier<? extends DataComponentType<T>> type, T value) {
        return ((Item.Properties) this).component(type.get(), value);
    }
}
