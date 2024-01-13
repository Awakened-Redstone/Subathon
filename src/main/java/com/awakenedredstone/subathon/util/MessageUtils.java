package com.awakenedredstone.subathon.util;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.override.ExtendedOperatorList;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class MessageUtils {

    public static void broadcast(Consumer<ServerPlayerEntity> consumer, Identifier identifier) {
        if (Subathon.getServer() != null) {
            broadcast(Subathon.getServer().getPlayerManager().getPlayerList(), consumer, identifier);
        }
    }

    public static void broadcastPacket(Identifier identifier, PacketByteBuf buf) {
        broadcast(player -> ServerPlayNetworking.send(player, identifier, buf), new Identifier(identifier.getNamespace(), "packet/" + identifier.getPath()));
    }

    public static void broadcast(List<ServerPlayerEntity> players, Consumer<ServerPlayerEntity> consumer, Identifier identifier) {
        try {
            players.forEach(consumer);
        } catch (Exception e) {
            if (Subathon.getServer() != null) Subathon.getServer().getCommandSource().sendFeedback(() -> broadcastError(identifier), true);
            Subathon.LOGGER.error("Error at broadcast " + identifier, e);
        }
    }

    public static void broadcastToOps(Consumer<ServerPlayerEntity> consumer, Identifier identifier) {
        if (Subathon.getServer() != null) {
            if (Subathon.getServer().isDedicated()) {
                try {
                    if (Subathon.getServer().getPlayerManager().getOpList() instanceof ExtendedOperatorList ops) {
                        List<UUID> uuids = Arrays.stream(ops.getUUIDs()).toList();
                        Subathon.getServer().getPlayerManager().getPlayerList().stream().filter(player -> uuids.contains(player.getUuid())).forEach(consumer);
                    } else {
                        List<String> names = Arrays.stream(Subathon.getServer().getPlayerManager().getOpList().getNames()).toList();
                        Subathon.getServer().getPlayerManager().getPlayerList().stream().filter(player -> names.contains(player.getGameProfile().getName())).forEach(consumer);
                    }
                } catch (Exception e) {
                    Subathon.getServer().getCommandSource().sendFeedback(() -> broadcastError(identifier), true);
                    Subathon.LOGGER.error("Error at broadcast " + identifier, e);
                }
            } else {
                broadcast(consumer, identifier);
            }
        }
    }

    private static Text broadcastError(Identifier identifier) {
        return Text.translatable("text.subathon.error.broadcast", identifier).formatted(Formatting.RED);
    }
}