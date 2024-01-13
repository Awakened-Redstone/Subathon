package com.awakenedredstone.subathon.core.effect.chaos.safe;

import com.awakenedredstone.subathon.core.effect.chaos.Chaos;
import com.awakenedredstone.subathon.core.effect.chaos.compat.ok_boomer.RandoScreenRotation;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

public class CompatChaos extends Chaos {
    private final Chaos chaos;
    private final String modId;

    public CompatChaos(Chaos chaos, String modId) {
        super(chaos.getDefaultWeight());
        this.chaos = chaos;
        this.modId = modId;
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        boolean hasMod = FabricLoader.getInstance().isModLoaded(modId);
        if (hasMod) chaos.playerTrigger(player);
        return hasMod;
    }
}
