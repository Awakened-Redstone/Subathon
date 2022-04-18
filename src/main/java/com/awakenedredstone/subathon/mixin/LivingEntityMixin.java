package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.config.Mode;
import com.awakenedredstone.subathon.events.LivingEntityCallback;
import com.awakenedredstone.subathon.util.ConfigUtils;
import com.awakenedredstone.subathon.util.ConversionUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow public abstract AttributeContainer getAttributes();

    @Shadow public abstract void setHealth(float health);

    @Shadow public abstract float getHealth();

    @Shadow @Final private static TrackedData<Float> HEALTH;

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

    @Inject(method = "getAttributeValue", at = @At("HEAD"), cancellable = true)
    public final void getAttributeValue(EntityAttribute attribute, CallbackInfoReturnable<Double> cir) {
        if (attribute == EntityAttributes.GENERIC_MAX_HEALTH && ((this.isPlayer() && ConfigUtils.isModeEnabled(Mode.HEALTH)) || ConfigUtils.isModeEnabled(Mode.SUPER_HEALTH))) {
            double newHealth = this.getAttributes().getValue(attribute) + Subathon.integration.data.value;
            if (this.getHealth() > newHealth) this.dataTracker.set(HEALTH, MathHelper.clamp(this.getHealth(), 0.0f, ConversionUtils.toFloat(newHealth)));
            cir.setReturnValue(newHealth);
        }
    }
}
