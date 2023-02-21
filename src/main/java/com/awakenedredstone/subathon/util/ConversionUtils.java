package com.awakenedredstone.subathon.util;

import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversionUtils {
    private static final Pattern TIME_PATTERN = Pattern.compile("^(?:(?:(\\d{1,4}):)?([0-5]?\\d):)?([0-5]?\\d)(?:\\.([0-1]?\\d))?$|^\\d{1,10}t$|^\\d{1,10}$");
    private static final Pattern TIME_PATTERN_HMST = Pattern.compile("^(?:(?:(\\d{1,4}):)?([0-5]?\\d):)?([0-5]?\\d)(?:\\.([0-1]?\\d))?$");
    private static final Pattern TIME_PATTERN_TICKS = Pattern.compile("^\\d{1,10}t$");
    private static final Pattern TIME_PATTERN_SECONDS = Pattern.compile("^\\d{1,10}$");

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

    public static int timeStringToTicks(String value) {
        int time = 0;
        Matcher HMST = TIME_PATTERN_HMST.matcher(value);
        if (HMST.matches()) {
            int h = (StringUtils.isNumeric(HMST.group(1)) && StringUtils.isNotBlank(HMST.group(1)) ? Short.parseShort(HMST.group(1)) : 0);
            int m = (StringUtils.isNumeric(HMST.group(2)) && StringUtils.isNotBlank(HMST.group(2)) ? Byte.parseByte(HMST.group(2)) : 0) + h * 60;
            int s = (StringUtils.isNumeric(HMST.group(3)) && StringUtils.isNotBlank(HMST.group(3)) ? Byte.parseByte(HMST.group(3)) : 0) + m * 60;
            int t = (StringUtils.isNumeric(HMST.group(4)) && StringUtils.isNotBlank(HMST.group(4)) ? Byte.parseByte(HMST.group(4)) : 0) + s * 20;
            time = t;
        } else if (TIME_PATTERN_TICKS.matcher(value).matches()) {
            time = toInt(Long.parseLong(value.replace("t", "")));
        } else if (TIME_PATTERN_SECONDS.matcher(value).matches()) {
            time = toInt(Long.parseLong(value) * 20);
        }
        return time;
    }

    public static byte toByte(double value) {
        return (byte) MathHelper.clamp(value, Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    public static byte toByte(float value) {
        return (byte) MathHelper.clamp(value, Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    public static byte toByte(long value) {
        return (byte) MathHelper.clamp(value, Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    public static byte toByte(int value) {
        return (byte) MathHelper.clamp(value, Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    public static byte toByte(short value) {
        return (byte) MathHelper.clamp(value, Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    public static short toShort(double value) {
        return (short) MathHelper.clamp(value, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    public static short toShort(float value) {
        return (short) MathHelper.clamp(value, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    public static short toShort(long value) {
        return (short) MathHelper.clamp(value, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    public static short toShort(int value) {
        return (short) MathHelper.clamp(value, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    public static int toInt(double value) {
        return (int) MathHelper.clamp(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static int toInt(float value) {
        return (int) MathHelper.clamp(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static int toInt(long value) {
        return (int) MathHelper.clamp(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static long toLong(double value) {
        return (long) MathHelper.clamp(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static long toLong(float value) {
        return (long) MathHelper.clamp(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static float toFloat(double value) {
        return (float) MathHelper.clamp(value, -Float.MAX_VALUE, Float.MAX_VALUE);
    }
}