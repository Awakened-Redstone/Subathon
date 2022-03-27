package com.awakenedredstone.subathon.util;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.config.MessageMode;
import com.awakenedredstone.subathon.override.ExtendedOperatorList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.awakenedredstone.subathon.Subathon.*;

public class MessageUtils {

    public static void sendError(Text message) {
        LOGGER.error(message.getString());
        server.getCommandSource().sendFeedback(message, true);
    }

    public static void broadcast(Consumer<ServerPlayerEntity> consumer, String id) {
        try {
            server.getPlayerManager().getPlayerList().forEach(consumer);
        } catch (Exception e) {
            server.getCommandSource().sendFeedback(new TranslatableText("subathon.messages.error.broadcast", id), true);
        }
    }

    public static void broadcastToOps(Consumer<ServerPlayerEntity> consumer, String id) {
        if (server.isDedicated()) {
            try {
                if (server.getPlayerManager().getOpList() instanceof ExtendedOperatorList ops) {
                    List<UUID> uuids = Arrays.stream(ops.getUUIDs()).toList();
                    server.getPlayerManager().getPlayerList().stream().filter(player -> uuids.contains(player.getUuid())).forEach(consumer);
                } else {
                    List<String> names = Arrays.stream(server.getPlayerManager().getOpList().getNames()).toList();
                    server.getPlayerManager().getPlayerList().stream().filter(player -> names.contains(player.getGameProfile().getName())).forEach(consumer);
                }
            } catch (Exception e) {
                server.getCommandSource().sendFeedback(new TranslatableText("subathon.messages.error.broadcast", id), true);
            }
        } else {
            broadcast(consumer, id);
        }
    }

    public static void sendEventMessage(Text message) {
        switch (MessageMode.valueOf(getConfigData().messageMode.toUpperCase())) {
            case CHAT -> broadcast(player -> player.sendMessage(message, false), "event_message");
            case OVERLAY -> broadcast(player -> sendPositionedText(player, message, 12, 16, 0xFFFFFF, true, true, 0), "event_message");
        }
    }

    public static void sendTitle(ServerCommandSource source, ServerPlayerEntity player, Text title, Function<Text, Packet<?>> constructor) throws CommandSyntaxException {
        player.networkHandler.sendPacket(constructor.apply(Texts.parse(source, title, player, 0)));
    }

    public static void sendTitle(ServerPlayerEntity player, Text title, Function<Text, Packet<?>> constructor) {
        player.networkHandler.sendPacket(constructor.apply(title));
    }

    public static String formatFloat(float value) {
        String text = Float.toString(getConfigData().effectMultiplier);
        int integerPlaces = text.indexOf('.');
        int decimalPlaces = text.length() - integerPlaces - 1;
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(decimalPlaces);
        return df.format(value);
    }

    public static void sendPositionedText(ServerPlayerEntity player, Text text, int x, int y, int color, boolean center, boolean shadow) {
        sendPositionedText(player, text, x, y, color, center, shadow, new Random().nextLong());
    }

    public static void sendPositionedText(ServerPlayerEntity player, Text text, int x, int y, int color, boolean center, boolean shadow, long id) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeText(text);
        buf.writeBoolean(shadow);
        buf.writeBoolean(center);
        int[] values = new int[]{x, y, color};
        buf.writeIntArray(values);
        buf.writeLong(id);
        ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "positioned_text"), buf);
    }
}