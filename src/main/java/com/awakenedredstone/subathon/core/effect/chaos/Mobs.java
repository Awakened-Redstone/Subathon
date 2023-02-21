package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.chaos.process.Chaos;
import com.awakenedredstone.subathon.core.effect.chaos.process.RegisterChaos;
import com.awakenedredstone.subathon.mixin.EntityMixin;
import com.awakenedredstone.subathon.mixin.ExperienceOrbEntityMixin;
import com.awakenedredstone.subathon.mixin.ShulkerBulletEntityMixin;
import com.awakenedredstone.subathon.util.MessageUtils;
import com.awakenedredstone.subathon.util.Texts;
import com.awakenedredstone.subathon.util.VectorHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Random;

@RegisterChaos("subathon:mobs")
public class Mobs extends Chaos {

    @Override
    public boolean trigger(World world) {
        MessageUtils.broadcast(this::trigger, Subathon.id("chaos"));
        return true;
    }

    @Override
    public boolean trigger(PlayerEntity player) {
        int successCount = 0;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            for (int i = 0; i < 13; i++) {
                int tryLimit = 10;
                boolean success = false;
                while (!success && tryLimit-- > 0) {
                    try {
                        Random random = new Random();
                        var minecraftRandom = player.world.random;
                        EntityType<?> entityType = Registries.ENTITY_TYPE.getRandom(minecraftRandom).get().value();
                        Identifier identifier = Registries.ENTITY_TYPE.getId(entityType);
                        Entity entity = entityType.create(player.getWorld());
                        if (entity == null) continue;
                        entity.setPos(player.getX() + random.nextInt(20) - 10, player.getY() + 1, player.getZ() + random.nextInt(20) - 10);

                        if (Subathon.COMMON_CONFIGS.excludedMobs().contains(identifier.toString())) {
                            continue;
                        } else if (entity instanceof CreeperEntity creeper) {
                            creeper.setFuseSpeed(random.nextInt(4) == 0 ? (short) 15 : (short) 30);
                            ((EntityMixin) creeper).getDataTracker().set(CreeperEntity.CHARGED, random.nextInt(4) == 0);
                        } else if (entity instanceof TntEntity tnt) {
                            tnt.setFuse(random.nextInt(4) == 0 ? (short) 20 : (short) 80);
                        } /*else if (entity instanceof WardenEntity) {
                            LargeEntitySpawnHelper.trySpawnAt(EntityType.WARDEN, SpawnReason.TRIGGERED, serverPlayer.getWorld(),
                                    player.getBlockPos(), 30, 15, 30,
                                    (world, pos, state, abovePos, aboveState) -> (aboveState.isAir() || aboveState.getMaterial().isLiquid()) && !state.isAir());
                            success = true;
                            continue;
                        }*/ else if (entity instanceof VexEntity) {
                            Subathon.scheduler.schedule(Subathon.server, 600, entity::kill);
                        } else if (entity instanceof ExperienceOrbEntity instance) {
                            ((ExperienceOrbEntityMixin) instance).setAmount(random.nextInt(1000));
                        } else if (entity instanceof EnderPearlEntity instance) {
                            instance.setOwner(player);
                        } else if (entity instanceof ShulkerBulletEntity instance) {
                            ((ShulkerBulletEntityMixin) instance).setTarget(player);
                        } else if (entity instanceof TntMinecartEntity instance) {
                            instance.prime();
                        } else if (entity instanceof LightningEntity instance) {
                            instance.setPos(player.getX(), player.getY(), player.getZ());
                        } else if (entity instanceof PersistentProjectileEntity instance) {
                            instance.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
                            instance.setVelocity(VectorHelper.getMovementVelocity(entity.getPos(), player.getPos().add(0, 1, 0), 1));
                        } else if (entity instanceof ProjectileEntity instance) {
                            instance.setVelocity(VectorHelper.getMovementVelocity(entity.getPos(), player.getPos().add(0, 1, 0), 1));
                        } else if (entity instanceof AbstractPiglinEntity instance) {
                            instance.setImmuneToZombification(true);
                        } else if (entity instanceof HoglinEntity instance) {
                            instance.setImmuneToZombification(true);
                        }

                        if (entity instanceof MobEntity mob) {
                            mob.initialize(serverPlayer.getWorld(), player.getWorld().getLocalDifficulty(player.getBlockPos()), SpawnReason.TRIGGERED, null, null);
                            if (trySpawnAt(mob, serverPlayer.getWorld(), player.getBlockPos(), 30, 15, 30,
                                    (world, pos, state, abovePos, aboveState) -> (aboveState.isAir() || aboveState.getMaterial().isLiquid()) && !state.isAir()).isPresent()) {
                                success = true;
                                continue;
                            }
                        }

                        Subathon.scheduler.schedule(Subathon.server, 1, () -> serverPlayer.getWorld().spawnEntityAndPassengers(entity));
                        success = true;
                    } catch (Exception e) {
                        success = false;
                        continue;
                    }
                }
                if (success) successCount++;
            }
        }
        if (successCount >= 7) player.sendMessage(Texts.of("text.subathon.chaos.subathon.mobs.message"));
        return successCount >= 7;
    }

    private static <T extends MobEntity> Optional<T> trySpawnAt(@NotNull T entity, @NotNull ServerWorld world, @NotNull BlockPos pos, int tries, int horizontalRange, int verticalRange, @NotNull LargeEntitySpawnHelper.Requirements requirements) {
        BlockPos.Mutable mutable = pos.mutableCopy();
        for (int i = 0; i < tries; ++i) {
            int j = MathHelper.nextBetween(world.random, -horizontalRange, horizontalRange);
            int k = MathHelper.nextBetween(world.random, -horizontalRange, horizontalRange);
            mutable.set(pos, j, verticalRange, k);
            if (!world.getWorldBorder().contains(mutable) || !LargeEntitySpawnHelper.findSpawnPos(world, verticalRange, mutable, requirements)) continue;
            entity.setPos(mutable.getX(), mutable.getY() + 1, mutable.getZ());
            Subathon.scheduler.schedule(Subathon.server, 1, () -> world.spawnEntityAndPassengers(entity));
            return Optional.of(entity);
        }
        return Optional.empty();
    }
}
