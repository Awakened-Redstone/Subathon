package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.events.LivingEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V", shift = At.Shift.AFTER))
    private void jump(CallbackInfo ci) {
        LivingEntityCallback.JUMP.invoker().onJump((LivingEntity) (Object) this);
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void tick(CallbackInfo ci) {
        LivingEntityCallback.TICK.invoker().onTick((LivingEntity) (Object) this);
    }
}
