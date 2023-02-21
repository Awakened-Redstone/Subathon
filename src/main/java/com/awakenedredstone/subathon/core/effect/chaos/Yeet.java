package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.chaos.process.Chaos;
import com.awakenedredstone.subathon.core.effect.chaos.process.RegisterChaos;
import com.awakenedredstone.subathon.mixin.EntityMixin;
import com.awakenedredstone.subathon.util.MessageUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.Random;

@RegisterChaos("subathon:yeet")
public class Yeet extends Chaos {

    @Override
    public boolean trigger(World world) {
        MessageUtils.broadcast(this::trigger, Subathon.id("chaos"));
        return true;
    }

    @Override
    public boolean trigger(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Random random = new Random();
            serverPlayer.addVelocity(random.nextDouble(100) - 50, random.nextDouble(55) - 5, random.nextDouble(100) - 50);
            ((EntityMixin) player).callScheduleVelocityUpdate();
        }
        return true;
    }
}
