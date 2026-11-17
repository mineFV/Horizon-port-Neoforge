package net.minecraft.network.codec;

/** Not the game's: the network's codecs, made as the game makes them; the network isn't modelled, so they don't run. */
public final class HorizonStreamCodecs {
    private HorizonStreamCodecs() {}

    /** A codec of the network: what made it. */
    public static final class Opaque<B, V> implements StreamCodec<B, V> {
        private final String made;

        public Opaque(String made) {
            this.made = made;
        }

        @Override
        public V decode(B input) {
            throw new UnsupportedOperationException("the network isn't modelled (horizon-port-neoforge): " + made);
        }

        @Override
        public void encode(B output, V value) {
            throw new UnsupportedOperationException("the network isn't modelled (horizon-port-neoforge): " + made);
        }

        @Override
        public String toString() {
            return "StreamCodec[" + made + "]";
        }
    }
}
