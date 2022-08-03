package com.awakenedredstone.subathon.mixin;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(PersistentProjectileEntity.class)
public interface PersistentProjectileEntityMixin {
    @Accessor int getLife();
    @Accessor void setLife(int value);
}
