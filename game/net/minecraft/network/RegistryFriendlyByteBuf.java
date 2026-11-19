package net.minecraft.network;

/** The stand-in for the game's RegistryFriendlyByteBuf: a network buffer with the registries (not modelled: FriendlyByteBuf). */
public class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    private final net.minecraft.core.RegistryAccess registryAccess;

    public RegistryFriendlyByteBuf(io.netty.buffer.ByteBuf source, net.minecraft.core.RegistryAccess registryAccess) {
        super(source);
        this.registryAccess = registryAccess;
    }

    public net.minecraft.core.RegistryAccess registryAccess() {
        return registryAccess;
    }
}
