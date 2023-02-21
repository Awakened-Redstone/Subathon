package com.awakenedredstone.subathon.core.effect.chaos.process;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Chaos {
    static final Map<Identifier, Chaos> chaosList = new HashMap<>();

    public abstract boolean trigger(World world);
    public abstract boolean trigger(PlayerEntity player);

    public static Map<Identifier, Chaos> getChaosList() {
        return Collections.unmodifiableMap(chaosList);
    }
}
