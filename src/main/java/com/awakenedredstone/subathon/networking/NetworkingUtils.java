package com.awakenedredstone.subathon.networking;

import com.awakenedredstone.subathon.Subathon;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.UUID;

public final class NetworkingUtils {
    public static void send(Identifier channelName, PacketByteBuf buf) {
        if (Subathon.getServer() != null) {
            Subathon.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                ServerPlayNetworking.send(player, channelName, buf);
            });
        }
    }

    public static void send(UUID playerUuid, Identifier channelName, PacketByteBuf buf) {
        if (Subathon.getServer() != null) {
            ServerPlayerEntity player = Subathon.getServer().getPlayerManager().getPlayer(playerUuid);
            if (player != null) ServerPlayNetworking.send(player, channelName, buf);
        }
    }
}