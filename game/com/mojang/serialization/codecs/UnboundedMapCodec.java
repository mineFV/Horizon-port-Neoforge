package com.mojang.serialization.codecs;

import java.util.Map;
import com.mojang.serialization.Codec;

/** The stand-in for DataFixerUpper's UnboundedMapCodec: a map of keys and values of two codecs (its text observed). */
public record UnboundedMapCodec<K, V>(Codec<K> keyCodec, Codec<V> elementCodec) implements BaseMapCodec<K, V>, Codec<Map<K, V>> {
    @Override
    public String toString() {
        return "UnboundedMapCodec[" + keyCodec + " -> " + elementCodec + "]";
    }
}
