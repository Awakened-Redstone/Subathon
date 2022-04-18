package com.awakenedredstone.subathon.util;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.config.MessageMode;
import com.awakenedredstone.subathon.config.Mode;
import net.minecraft.text.LiteralText;

import static com.awakenedredstone.subathon.util.ConversionUtils.toByte;
import static com.awakenedredstone.subathon.util.ConversionUtils.toShort;

public class ConfigUtils {

    public static Mode getMode() {
        try {
            return Mode.valueOf(Subathon.getConfigData().mode.toUpperCase());
        } catch (IllegalArgumentException exception) {
            if (Subathon.server != null)
                Subathon.server.getCommandSource().sendFeedback(new LiteralText(Subathon.getConfigData().mode.toUpperCase() + " is not a valid mode"), true);
            Subathon.LOGGER.error(Subathon.getConfigData().mode.toUpperCase() + " is not a valid mode");
            return Mode.NONE;
        }
    }

    public static boolean isModeEnabled(Mode mode) {
        return getMode().equals(mode);
    }

    public static MessageMode getMessageMode() {
        try {
            return MessageMode.valueOf(Subathon.getConfigData().messageMode.toUpperCase());
        } catch (IllegalArgumentException exception) {
            if (Subathon.server != null)
                Subathon.server.getCommandSource().sendFeedback(new LiteralText(Subathon.getConfigData().messageMode.toUpperCase() + " is not a valid message mode"), true);
            Subathon.LOGGER.error(Subathon.getConfigData().messageMode.toUpperCase() + " is not a valid message mode");
            return MessageMode.CHAT;
        }
    }

    public static String ticksToTime(int totalTicks) {
        byte ticks = toByte(totalTicks % 20);
        byte seconds = toByte((totalTicks /= 20) % 60);
        byte minutes = toByte((totalTicks /= 60) % 60);
        short hours = toShort(totalTicks / 60);
        return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, ticks);
    }
}
