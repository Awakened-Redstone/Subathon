package com.awakenedredstone.subathon.entity;

import com.awakenedredstone.subathon.registry.EntityRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public class FireballEntity
extends AbstractFireballEntity {
    private int explosionPower = 1;
    private int targetY = Integer.MAX_VALUE;

    public FireballEntity(EntityType<? extends FireballEntity> entityType, World world) {
        super(entityType, world);
    }

    public FireballEntity(World world, LivingEntity owner, double velocityX, double velocityY, double velocityZ, int explosionPower, int targetY) {
        super(EntityRegistry.FIREBALL, owner, velocityX, velocityY, velocityZ, world);
        this.explosionPower = explosionPower;
        this.targetY = targetY;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.world.isClient) {
            if (hitResult.getType() == HitResult.Type.ENTITY && !(((EntityHitResult)hitResult).getEntity() instanceof PlayerEntity)) return;
            if (hitResult.getType() == HitResult.Type.BLOCK && getY() <= targetY) {
                boolean bl = this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
                this.world.createExplosion(this, this.getX(), this.getY(), this.getZ(), (float) this.explosionPower, bl, World.ExplosionSourceType.MOB);
                this.discard();
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (this.world.isClient) {
            return;
        }
        Entity entity = entityHitResult.getEntity();
        Entity entity2 = this.getOwner();
        entity.damage(DamageSource.fireball(this, entity2), 6.0f);
        if (entity2 instanceof LivingEntity) {
            this.applyDamageEffects((LivingEntity)entity2, entity);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setVelocity(0, -2, 0);
        powerX = 0;
        powerY = -2;
        powerZ = 0;
    }

    @Override
    public void setVelocity(Vec3d velocity) {
        super.setVelocity(new Vec3d(0, -2, 0));
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        super.setVelocity(0, -2, 0);
    }

    @Override
    public void setVelocity(double x, double y, double z, float speed, float divergence) {
        super.setVelocity(0, -2, 0, speed, divergence);
    }

    @Override
    public void setVelocityClient(double x, double y, double z) {
        super.setVelocityClient(0, -2, 0);
    }

    @Override
    public void addVelocity(Vec3d velocity) {
        super.addVelocity(new Vec3d(0, -2, 0));
    }

    @Override
    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        super.addVelocity(0, deltaY, 0);
    }

    @Override
    public boolean isImmuneToExplosion() {
        return true;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("ExplosionPower", this.explosionPower);
        nbt.putInt("TargetY", this.targetY);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("ExplosionPower", NbtElement.NUMBER_TYPE)) {
            this.explosionPower = nbt.getInt("ExplosionPower");
        }
        if (nbt.contains("ExplosionPower", NbtElement.NUMBER_TYPE)) {
            this.targetY = nbt.getInt("TargetY");
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }
}