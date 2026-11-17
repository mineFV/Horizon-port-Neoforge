package net.neoforged.neoforge.capabilities;

import java.util.ArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The stand-in for NeoForge's RegisterCapabilitiesEvent: providers added to their capability, for blocks, block entity
 * types and items (observed, `standins --horizon-src`); entities aren't modelled. The bridge fires it on the mod's bus.
 */
public class RegisterCapabilitiesEvent extends net.neoforged.bus.api.Event implements net.neoforged.fml.event.IModBusEvent {
    RegisterCapabilitiesEvent() {}

    public <T, C> void registerBlock(BlockCapability<T, C> capability, IBlockCapabilityProvider<T, C> provider, Block... blocks) {
        java.util.Objects.requireNonNull(provider);
        if (blocks.length == 0) throw new IllegalArgumentException("Must register at least one block");
        synchronized (capability) {
            for (Block block : blocks) capability.providers.computeIfAbsent(java.util.Objects.requireNonNull(block), b -> new ArrayList<>()).add(provider);
        }
    }

    @SuppressWarnings("unchecked")
    public <T, C, BE extends BlockEntity> void registerBlockEntity(BlockCapability<T, C> capability, BlockEntityType<BE> type, ICapabilityProvider<? super BE, C, T> provider) {
        IBlockCapabilityProvider<T, C> adapted = (level, pos, state, blockEntity, context) -> {
            if (blockEntity == null || blockEntity.getType() != type) return null;
            return provider.getCapability((BE) blockEntity, context);
        };
        for (Block block : type.getValidBlocks()) registerBlock(capability, adapted, block);
    }

    public boolean isBlockRegistered(BlockCapability<?, ?> capability, Block block) {
        synchronized (capability) {
            return capability.providers.containsKey(block);
        }
    }

    public void setProxyable(BlockCapability<?, ?> capability) {
        capability.setProxyable(true);
    }

    public void setNonProxyable(BlockCapability<?, ?> capability) {
        capability.setProxyable(false);
    }

    public <T, C> void registerItem(ItemCapability<T, C> capability, ICapabilityProvider<ItemStack, C, T> provider, ItemLike... items) {
        java.util.Objects.requireNonNull(provider);
        if (items.length == 0) throw new IllegalArgumentException("Must register at least one item");
        synchronized (capability) {
            for (ItemLike like : items) capability.providers.computeIfAbsent(item(like), i -> new ArrayList<>()).add(provider);
        }
    }

    public boolean isItemRegistered(ItemCapability<?, ?> capability, Item item) {
        synchronized (capability) {
            return capability.providers.containsKey(item);
        }
    }

    /** An item-like's item: an item's itself, a registered one's; a block's item (BlockItem) isn't modelled. */
    private static Item item(ItemLike like) {
        if (like instanceof Item item) return item;
        if (like instanceof net.neoforged.neoforge.registries.DeferredHolder<?, ?> holder && holder.get() instanceof Item item) return item;
        throw new UnsupportedOperationException("the item of " + like + " isn't modelled (horizon-port-neoforge)");
    }
}
