package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.mixin.EntityMixin;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Random;

public class Yeet extends Chaos {

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Random random = new Random();
            serverPlayer.addVelocity(random.nextDouble(100) - 50, random.nextDouble(55) - 5, random.nextDouble(100) - 50);
            ((EntityMixin) player).callScheduleVelocityUpdate();
        }
        return true;
    }
}
