package com.awakenedredstone.subathon.util;

import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;

public class Utils {

    @Deprecated
    public static double getNbtDoubleSafe(NbtCompound compound, String key, double fallback) {
        return compound.contains(key, NbtElement.NUMBER_TYPE) ? compound.getDouble(key) : fallback;
    }

    public static void updateMainLoadProgress(CommandBossBar bossBar, String state, byte progress, byte steps) {
        bossBar.setName(Text.translatable("text.subathon.load.main", Text.translatable("text.subathon.load.stage." + state), progress, steps));
        bossBar.setValue(progress);
    }
}
