package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.mixin.EntityMixin;
import com.awakenedredstone.subathon.mixin.ExperienceOrbEntityMixin;
import com.awakenedredstone.subathon.mixin.ShulkerBulletEntityMixin;
import com.awakenedredstone.subathon.util.Texts;
import com.awakenedredstone.subathon.util.VectorHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Random;

public class RandomizeEntities extends Chaos {

    public RandomizeEntities() {
        super(10);
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        int successCount = 0;
        List<Entity> entities = player.getWorld().getEntitiesByClass(Entity.class, player.getBoundingBox().expand(15), entity -> true).stream()
                .filter(entity -> Subathon.COMMON_CONFIGS.protectedEntities().contains(Registries.ENTITY_TYPE.getId(entity.getType()).toString())).toList();

        if (entities.size() < 5) return false;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            for (Entity oldEntity : entities) {
                int tryLimit = 10;
                boolean success = false;
                while (!success && tryLimit-- > 0) {
                    try {
                        Random random = new Random();
                        var minecraftRandom = player.getWorld().getRandom();
                        EntityType<?> entityType = Registries.ENTITY_TYPE.getRandom(minecraftRandom).get().value();
                        Identifier identifier = Registries.ENTITY_TYPE.getId(entityType);
                        Entity newEntity = entityType.create(player.getWorld());
                        if (newEntity == null) continue;
                        newEntity.setPos(oldEntity.getX(), oldEntity.getY(), oldEntity.getZ());

                        if (Subathon.COMMON_CONFIGS.excludedEntities().contains(identifier.toString())) {
                            continue;
                        } else if (newEntity instanceof CreeperEntity creeper) {
                            creeper.setFuseSpeed((short) 30);
                            ((EntityMixin) creeper).getDataTracker().set(CreeperEntity.CHARGED, random.nextInt(4) == 0);
                        } else if (newEntity instanceof TntEntity tnt) {
                            tnt.setFuse((short) 80);
                        } else if (newEntity instanceof VexEntity) {
                            Subathon.getInstance().getScheduler().schedule(Subathon.getServer(), 600, newEntity::kill);
                        } else if (newEntity instanceof ExperienceOrbEntity instance) {
                            ((ExperienceOrbEntityMixin) instance).setAmount(random.nextInt(1000));
                        } else if (newEntity instanceof ShulkerBulletEntity instance) {
                            ((ShulkerBulletEntityMixin) instance).setTarget(player);
                        } else if (newEntity instanceof TntMinecartEntity instance) {
                            instance.prime();
                        } else if (newEntity instanceof LightningEntity instance) {
                            instance.setPos(player.getX(), player.getY(), player.getZ());
                        } else if (newEntity instanceof PersistentProjectileEntity instance) {
                            instance.setOwner(player);
                            instance.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
                            instance.setVelocity(VectorHelper.getMovementVelocity(newEntity.getPos(), player.getPos().add(0, 1, 0), 1));
                        } else if (newEntity instanceof ProjectileEntity instance) {
                            instance.setOwner(player);
                            instance.setVelocity(VectorHelper.getMovementVelocity(newEntity.getPos(), player.getPos().add(0, 1, 0), 1));
                        } else if (newEntity instanceof AbstractPiglinEntity instance) {
                            instance.setImmuneToZombification(true);
                        } else if (newEntity instanceof HoglinEntity instance) {
                            instance.setImmuneToZombification(true);
                        }

                        if (newEntity instanceof MobEntity mob) {
                            mob.initialize(serverPlayer.getServerWorld(), player.getWorld().getLocalDifficulty(player.getBlockPos()), SpawnReason.EVENT, null, null);
                        }

                        Subathon.getInstance().getScheduler().schedule(Subathon.getServer(), 1, () -> {
                            serverPlayer.getWorld().spawnEntity(newEntity);
                            oldEntity.discard();
                        });
                        success = true;
                    } catch (Exception e) {
                        success = false;
                        continue;
                    }
                }
                if (success) successCount++;
            }
        }

        if (successCount >= entities.size() * 0.7) player.sendMessage(Texts.of("text.subathon.chaos.subathon.randomize_entities.message"));
        return successCount >= entities.size() * 0.7;
    }
}
