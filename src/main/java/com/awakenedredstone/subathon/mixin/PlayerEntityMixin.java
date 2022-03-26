package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.config.Mode;
import com.awakenedredstone.subathon.twitch.Bot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "getMovementSpeed", at = @At(value = "RETURN"), cancellable = true)
    public void getMovementSpeed(CallbackInfoReturnable<Float> cir) {
        if (Subathon.getEffect() == Mode.SPEED) cir.setReturnValue((float)this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) + Bot.getCounter());
        if (Subathon.getEffect() == Mode.SLOWNESS) cir.setReturnValue(Math.max((float)this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) - Bot.getCounter(), 0.0001f));

    }
}
