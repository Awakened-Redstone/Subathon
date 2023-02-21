package com.awakenedredstone.subathon.core.effect.chaos.process;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.MessageUtils;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Random;

@RegisterChaos("subathon:rando_potion")
public class RandoPotion extends Chaos {

    @Override
    public boolean trigger(World world) {
        MessageUtils.broadcast(this::trigger, Subathon.id("apply/rando_potions"));
        return true;
    }

    @Override
    public boolean trigger(PlayerEntity player) {
        Random random = new Random();
        double durationGaussian = Math.abs(random.nextGaussian(15, 20));
        if (durationGaussian < 5) durationGaussian += 5;
        double amplifierGaussian = Math.abs(random.nextGaussian(0, 23));
        int duration = (int) Math.round(durationGaussian * 20);
        int amplifier = (int) Math.round(amplifierGaussian);
        StatusEffect effect = Subathon.potionsRandom.next();
        if (player.hasStatusEffect(effect)) {
            StatusEffectInstance playerEffect = player.getStatusEffect(effect);
            duration += playerEffect.getDuration();
            amplifier = Math.max(amplifier, playerEffect.getAmplifier());
            player.removeStatusEffect(playerEffect.getEffectType());
        }
        player.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier, false, false, true));
        return true;
    }
}
