package com.awakenedredstone.subathon.core.effect.chaos.compat.ok_boomer;

import com.awakenedredstone.subathon.core.effect.chaos.Chaos;
import com.awakenedredstone.subathon.mixin.EntityMixin;
import io.wispforest.okboomer.OkBoomer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.RotationAxis;

import java.util.Random;

public class RandoScreenRotation extends Chaos {

    public RandoScreenRotation() {
        super(27);
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        OkBoomer.screenRotation = new Random().nextFloat(-180.001f, 180.001f);
        return true;
    }
}
