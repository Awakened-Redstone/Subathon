package com.awakenedredstone.subathon.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShulkerBulletEntity.class)
public interface ShulkerBulletEntityMixin {
    @Accessor void setTarget(Entity target);
}