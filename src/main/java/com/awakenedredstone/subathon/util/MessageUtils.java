package com.awakenedredstone.subathon.util;

import com.awakenedredstone.subathon.config.MessageMode;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.text.DecimalFormat;
import java.util.function.Consumer;

import static com.awakenedredstone.subathon.Subathon.*;

public class MessageUtils {

    public static void sendError(Text message) {
        LOGGER.error(message.getString());
        server.getCommandSource().sendFeedback(message, true);
    }

    public static void sendGlobalMessage(Consumer<ServerPlayerEntity> message) {
        server.getPlayerManager().getPlayerList().forEach(message);
    }

    public static void sendEventMessage(Text message) {
        switch (MessageMode.valueOf(getConfigData().messageMode)) {
            case CHAT -> sendGlobalMessage(player -> player.sendMessage(message, false));
            case OVERLAY -> sendGlobalMessage(player -> sendPositionedText(player, message, 12, 16, 0xFFFFFF, true, true, 0));
        }
    }

    public static String formatFloat(float value) {
        String text = Float.toString(getConfigData().effectAmplifier);
        int integerPlaces = text.indexOf('.');
        int decimalPlaces = text.length() - integerPlaces - 1;
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(decimalPlaces);
        return df.format(value);
    }
}
