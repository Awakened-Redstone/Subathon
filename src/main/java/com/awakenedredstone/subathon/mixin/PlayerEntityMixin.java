package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.config.Mode;
import com.awakenedredstone.subathon.util.ConfigUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.math.BigDecimal;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "getMovementSpeed", at = @At(value = "RETURN"), cancellable = true)
    public void getMovementSpeed(CallbackInfoReturnable<Float> cir) {
        BigDecimal value = BigDecimal.valueOf(Subathon.integration.data.value).multiply(BigDecimal.valueOf(0.01f));
        if (ConfigUtils.isModeEnabled(Mode.SPEED)) cir.setReturnValue(BigDecimal.valueOf(this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)).add(value).floatValue());
        if (ConfigUtils.isModeEnabled(Mode.SLOWNESS)) cir.setReturnValue(Math.max(BigDecimal.valueOf(this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)).subtract(value).floatValue(), 0.001f));
    }
}
