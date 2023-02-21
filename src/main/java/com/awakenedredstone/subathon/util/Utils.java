package com.awakenedredstone.subathon.util;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class Utils {
    private static final Pattern TIME_PATTERN = Pattern.compile("^(?:(?:(\\d{1,4}):)?([0-5]?\\d):)?([0-5]?\\d)(?:\\.([0-1]?\\d))?$|^\\d{1,10}t$|^\\d{1,10}$");
    private static final Pattern TIME_PATTERN_HMST = Pattern.compile("^(?:(?:(\\d{1,4}):)?([0-5]?\\d):)?([0-5]?\\d)(?:\\.([0-1]?\\d))?$");
    private static final Pattern TIME_PATTERN_TICKS = Pattern.compile("^\\d{1,10}t$");
    private static final Pattern TIME_PATTERN_SECONDS = Pattern.compile("^\\d{1,10}$");

    public static <T> void load(@NotNull Class<T> clazz) {
        try {
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
        } catch (ClassNotFoundException ignored) {/**/}
    }

    public static <T> Class<?> getClass(@NotNull String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {/**/}
        return null;
    }

    public static boolean isValidUuid(String uuid) {
        try{
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException exception){
            return false;
        }
    }

    public static int getRandomFromRange(int min, int max) {
        int range = max - min;
        return new Random().nextInt(range <= 0 ? 1 : range) + min;
    }

    public static boolean isValidTimeString(String value) {
        return TIME_PATTERN_HMST.matcher(value).matches() || TIME_PATTERN_TICKS.matcher(value).matches() || TIME_PATTERN_SECONDS.matcher(value).matches();
    }

    public static void makeCylinder(Vec3d pos, Consumer<Vec3d> function, double radiusX, double radiusZ, boolean filled) {
        radiusX += 0.5;
        radiusZ += 0.5;

        final double invRadiusX = 1 / radiusX;
        final double invRadiusZ = 1 / radiusZ;

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);

        double nextXn = 0;
        forX: for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            nextXn = (x + 1) * invRadiusX;
            double nextZn = 0;
            forZ: for (int z = 0; z <= ceilRadiusZ; ++z) {
                final double zn = nextZn;
                nextZn = (z + 1) * invRadiusZ;

                double distanceSq = lengthSq(xn, zn);
                if (distanceSq > 1) {
                    if (z == 0) {
                        break forX;
                    }
                    break forZ;
                }

                if (!filled) {
                    if (lengthSq(nextXn, zn) <= 1 && lengthSq(xn, nextZn) <= 1) {
                        continue;
                    }
                }

                function.accept(pos.add(x, 0, z));
                function.accept(pos.add(-x, 0, z));
                function.accept(pos.add(x, 0, -z));
                function.accept(pos.add(-x, 0, -z));
            }
        }
    }

    public static void makeSphere(Vec3d pos, Consumer<Vec3d> function, double radiusX, double radiusY, double radiusZ, boolean filled) {
        int affected = 0;

        radiusX += 0.5;
        radiusY += 0.5;
        radiusZ += 0.5;

        final double invRadiusX = 1 / radiusX;
        final double invRadiusY = 1 / radiusY;
        final double invRadiusZ = 1 / radiusZ;

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusY = (int) Math.ceil(radiusY);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);

        double nextXn = 0;
        forX: for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            nextXn = (x + 1) * invRadiusX;
            double nextYn = 0;
            forY: for (int y = 0; y <= ceilRadiusY; ++y) {
                final double yn = nextYn;
                nextYn = (y + 1) * invRadiusY;
                double nextZn = 0;
                forZ: for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    nextZn = (z + 1) * invRadiusZ;

                    double distanceSq = lengthSq(xn, yn, zn);
                    if (distanceSq > 1) {
                        if (z == 0) {
                            if (y == 0) {
                                break forX;
                            }
                            break forY;
                        }
                        break forZ;
                    }

                    if (!filled) {
                        if (lengthSq(nextXn, yn, zn) <= 1 && lengthSq(xn, nextYn, zn) <= 1 && lengthSq(xn, yn, nextZn) <= 1) {
                            continue;
                        }
                    }

                    function.accept(pos.add(x, y, z));
                    function.accept(pos.add(-x, y, z));
                    function.accept(pos.add(x, -y, z));
                    function.accept(pos.add(x, y, -z));
                    function.accept(pos.add(-x, -y, z));
                    function.accept(pos.add(x, -y, -z));
                    function.accept(pos.add(-x, y, -z));
                    function.accept(pos.add(-x, -y, -z));
                }
            }
        }
    }

    public static double lengthSq(double x, double y, double z) {
        return (x * x) + (y * y) + (z * z);
    }

    public static double lengthSq(double x, double z) {
        return (x * x) + (z * z);
    }
}
