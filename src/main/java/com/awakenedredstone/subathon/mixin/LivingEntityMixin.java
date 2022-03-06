package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.Effect;
import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.twitch.Bot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow private float movementSpeed;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V", shift = At.Shift.AFTER))
    private void jump(CallbackInfo ci) {
        if (Subathon.getEffect() == Effect.JUMP) increaseJump();
    }

    private void increaseJump() {
        this.addVelocity(0, Bot.getCounter() * Subathon.getConfigData().effectMultiplier, 0);
    }
}
