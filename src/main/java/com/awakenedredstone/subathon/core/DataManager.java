package com.awakenedredstone.subathon.core;

import com.awakenedredstone.subathon.core.effect.Effect;
import com.awakenedredstone.subathon.registry.SubathonRegistries;

import java.util.List;

public class DataManager {
    public static List<? extends Effect> getActiveEffects() {
        return SubathonRegistries.EFFECTS.stream().filter(effect -> effect.enabled).toList();
    }
}
