package net.minecraft.core.component;

/**
 * Not the game's: the network codec a component type has from its codec when it was given none. The network isn't
 * modelled: using it says so.
 */
final class HorizonStreamCodec<B, V> implements net.minecraft.network.codec.StreamCodec<B, V> {
    @Override
    public V decode(B input) {
        throw new UnsupportedOperationException("the network codec of a component isn't modelled (horizon-port-neoforge)");
    }

    @Override
    public void encode(B output, V value) {
        throw new UnsupportedOperationException("the network codec of a component isn't modelled (horizon-port-neoforge)");
    }
}
