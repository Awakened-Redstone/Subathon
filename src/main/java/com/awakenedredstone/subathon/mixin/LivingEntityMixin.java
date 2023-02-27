package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.data.ComponentManager;
import com.awakenedredstone.subathon.core.effect.Effect;
import com.awakenedredstone.subathon.registry.SubathonRegistries;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyExpressionValue(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getJumpVelocity()F"))
    private float subathon$increaseJump(float original) {;
        Effect jump = SubathonRegistries.EFFECTS.get(Subathon.id("jump"));
        if (jump == null) return original;
        if (jump.enabled && (LivingEntity) (Object) this instanceof PlayerEntity player) {
            return (float) (original + (jump.scale * ComponentManager.getComponent(world, player).getPoints()));
        } else return original;
    }
}
