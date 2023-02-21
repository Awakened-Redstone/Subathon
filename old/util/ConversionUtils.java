package old.util;

import net.minecraft.util.math.MathHelper;

public class ConversionUtils {
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
        return (float) MathHelper.clamp(value, Float.MIN_VALUE, Float.MAX_VALUE);
    }
}
