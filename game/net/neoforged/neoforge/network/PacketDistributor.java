package net.neoforged.neoforge.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The stand-in for NeoForge's PacketDistributor: sending the mod's payloads. Horizon's clients don't run the mod's
 * client side, so a payload to them has no receiver: it is dropped (the maintainer's decision, 2026-11-19), the
 * server's state staying as the mod set it. The translation runs on the server's side, which has no server to send
 * to: sendToServer is refused.
 */
public final class PacketDistributor {
    private PacketDistributor() {}

    private static UnsupportedOperationException refused(CustomPacketPayload payload) {
        return new UnsupportedOperationException("sending the payload " + (payload == null ? null : payload.type().id()) + " isn't modelled (horizon-port-neoforge)");
    }

    public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        throw refused(payload);
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        // Dropped: no client of Horizon's receives the mod's payloads.
    }

    public static void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        // Dropped: no client of Horizon's receives the mod's payloads.
    }

    public static void sendToPlayersTrackingEntity(net.minecraft.world.entity.Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        // Dropped: no client of Horizon's receives the mod's payloads.
    }

    public static void sendToPlayersTrackingEntityAndSelf(net.minecraft.world.entity.Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        // Dropped: no client of Horizon's receives the mod's payloads.
    }
}
