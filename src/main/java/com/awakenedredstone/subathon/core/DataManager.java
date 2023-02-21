package com.awakenedredstone.subathon.core;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.process.Effect;

import java.util.List;

public class DataManager {
    public static List<? extends Effect> getActiveEffects() {
        return Subathon.COMMON_CONFIGS.effects().values().stream().filter(effect -> effect.enabled).toList();
    }
}
