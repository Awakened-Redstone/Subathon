package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.mixin.PersistentProjectileEntityAccessor;
import com.awakenedredstone.subathon.util.Utils;
import com.awakenedredstone.subathon.util.VectorHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Regen extends Chaos {

    public Regen() {
        super(50);
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        //return false if player health is above 80%
        if (player.getHealth() > player.getMaxHealth() * 0.8f) return false;
        float healAmount = new Random().nextFloat(-2, player.getMaxHealth());
        player.heal(healAmount);
        return true;
    }
}
