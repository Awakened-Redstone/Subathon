package com.awakenedredstone.subathon.core.effect.chaos;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Random;

public class Health extends Chaos {

    public Health() {
        super(30);
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        float healthAmount = new Random().nextFloat(7, player.getMaxHealth() + 20);
        //gets the player's health percentage and stores it
        float healthPercentage = player.getHealth() / player.getMaxHealth();

        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(healthAmount);
        player.damage(player.getDamageSources().generic(), 0.0001f);

        //sets the player's health to the percentage of their max health, with a small chance of a random decay
        player.setHealth(Math.max(healthPercentage * player.getMaxHealth() - new Random().nextFloat(0, 3), 1));
        return true;
    }
}
