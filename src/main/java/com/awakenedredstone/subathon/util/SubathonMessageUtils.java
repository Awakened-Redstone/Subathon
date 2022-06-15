package com.awakenedredstone.subathon.util;

import com.awakenedredstone.cubecontroller.util.MessageUtils;
import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.twitch.Subscription;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Function;

import static com.awakenedredstone.cubecontroller.util.ConversionUtils.toByte;
import static com.awakenedredstone.cubecontroller.util.ConversionUtils.toShort;
import static com.awakenedredstone.subathon.Subathon.getConfigData;
import static com.awakenedredstone.subathon.Subathon.identifier;

public class SubathonMessageUtils {

    public static void sendEventMessage(String user, String target, int amount, Subscription tier, SubathonCommand.Events event, String message) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(user);
        buf.writeString(target);
        buf.writeInt(amount);
        buf.writeEnumConstant(tier);
        buf.writeEnumConstant(event);
        buf.writeString(message);
        MessageUtils.broadcastPacket(identifier("event"), buf);
    }

    public static void updateIntegrationStatus(IntegrationStatus status) {
        Subathon.integration.status = status;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(status);
        MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "bot_status"), buf), identifier("integration/status"));
    }

    public static void informTimers() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(getConfigData().updateTimer > 0 ? getConfigData().updateTimer : -1);
        MessageUtils.broadcastPacket(Subathon.identifier("timer"), buf);
    }

    public static void broadcastTitle(Text title, Function<Text, Packet<?>> constructor, Identifier identifier) {
        MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, title, constructor), identifier);
    }

    public static String ticksToTime(int totalTicks) {
        byte ticks = toByte(totalTicks % 20);
        byte seconds = toByte((totalTicks /= 20) % 60);
        byte minutes = toByte((totalTicks /= 60) % 60);
        short hours = toShort(totalTicks / 60);
        return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, ticks);
    }

    public static String ticksToSimpleTime(int _totalTicks) {
        double totalTicks = _totalTicks;
        byte seconds = toByte((totalTicks /= 20) % 60);
        byte minutes = toByte((totalTicks /= 60) % 60);
        short hours = toShort(totalTicks / 60);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
