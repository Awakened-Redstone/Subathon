package com.awakenedredstone.subathon.util;

import com.awakenedredstone.subathon.ChaosMode;
import com.awakenedredstone.subathon.Subathon;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Random;
import java.util.function.Consumer;

public class Utils {
    public static RandomCollection<EntityType<?>> MOB_WEIGHTS_CACHE = new RandomCollection<>();
    public static RandomCollection<ChaosMode> CHAOS_WEIGHTS_CACHE = new RandomCollection<>();

    @Deprecated
    public static double getNbtDoubleSafe(NbtCompound compound, String key, double fallback) {
        return compound.contains(key, NbtElement.NUMBER_TYPE) ? compound.getDouble(key) : fallback;
    }

    public static void updateMainLoadProgress(CommandBossBar bossBar, String state, byte progress, byte steps) {
        bossBar.setName(Text.translatable("text.subathon.load.main", Text.translatable("text.subathon.load.stage." + state), progress, steps));
        bossBar.setValue(progress);
    }

    public static void buildMobWeightsCache() {
        MOB_WEIGHTS_CACHE = new RandomCollection<>();
        Subathon.getConfigData().mobWeight.forEach((identifier, weight) -> EntityType.get(identifier).ifPresent(entityType -> MOB_WEIGHTS_CACHE.add(weight, entityType)));
    }

    public static void buildChaosWeightsCache() {
        CHAOS_WEIGHTS_CACHE = new RandomCollection<>();
        for (ChaosMode value : ChaosMode.values()) {
            CHAOS_WEIGHTS_CACHE.add(Subathon.getConfigData().actWeight.getOrDefault(value.name().toLowerCase(), value.getDefaultWeight()), value);
        }
    }

    public static int getRandomFromRange(int min, int max) {
        int range = max - min;
        return new Random().nextInt(range <= 0 ? 1 : range) + min;
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

    private static double lengthSq(double x, double z) {
        return (x * x) + (z * z);
    }
}
