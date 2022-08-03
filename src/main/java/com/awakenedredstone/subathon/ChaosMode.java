package com.awakenedredstone.subathon;

import com.awakenedredstone.cubecontroller.GameControl;
import com.awakenedredstone.cubecontroller.util.MessageUtils;
import com.awakenedredstone.subathon.mixin.EntityMixin;
import com.awakenedredstone.subathon.mixin.PersistentProjectileEntityMixin;
import com.awakenedredstone.subathon.mixin.ServerWorldMixin;
import com.awakenedredstone.subathon.util.Utils;
import com.awakenedredstone.subathon.util.VectorHelper;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public enum ChaosMode {
    ARROWS(20, control -> MessageUtils.broadcast(player -> {
        if (!player.world.isClient) {
            Vec3d playerPos = player.getPos();
            Random random = new Random();

            List<Runnable> arrows = new ArrayList<>();

            Utils.makeCylinder(playerPos, arrowPos -> {
                arrowPos = arrowPos.add(random.nextDouble(2) - 1, 2 + random.nextDouble(2), random.nextDouble(2) - 1);
                ArrowEntity arrowEntity = new ArrowEntity(player.getWorld(), player);
                arrowEntity.setPos(arrowPos.x, arrowPos.y, arrowPos.z);
                Vec3d velocity = VectorHelper.getMovementVelocity(arrowPos, playerPos.add(0, 1, 0), 1);
                arrowEntity.setVelocity(velocity);
                arrowEntity.setDamage(Utils.getRandomFromRange(Subathon.getConfigData().arrowMinDamage, Subathon.getConfigData().arrowMaxDamage));
                arrowEntity.setPunch(Utils.getRandomFromRange(Subathon.getConfigData().arrowMinKnockback, Subathon.getConfigData().arrowMaxKnockback));
                ((PersistentProjectileEntityMixin) arrowEntity).setLife(1150);
                arrowEntity.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                arrows.add(() -> player.getWorld().spawnEntity(arrowEntity));
            }, 5, 5, false);

            Collections.shuffle(arrows);

            arrows.forEach(Runnable::run);
        }
    }, Subathon.identifier("control/chaos/arrows"))),

    MOBS(15, control -> MessageUtils.broadcast(player -> {
        for (int i = 0; i < Subathon.getConfigData().mobsToSpawn; i++) {
            if (!player.world.isClient) {
                Random random = new Random();
                EntityType<?> entityType = Utils.MOB_WEIGHTS_CACHE.next();
                Entity entity = entityType.create(player.getWorld());
                entity.setPos(player.getX(), player.getY() + 1, player.getZ());

                if (entity instanceof CreeperEntity creeper) {
                    creeper.setFuseSpeed(random.nextInt(4) == 0 ? (short) 15 : (short) 30);
                    ((EntityMixin) creeper).getDataTracker().set(CreeperEntity.CHARGED, random.nextInt(4) == 0);
                } else if (entity instanceof TntEntity tnt) {
                    tnt.setFuse(random.nextInt(4) == 0 ? (short) 20 : (short) 80);
                } else if (entity instanceof WardenEntity) {
                    LargeEntitySpawnHelper.trySpawnAt(EntityType.WARDEN, SpawnReason.TRIGGERED, player.getWorld(),
                            player.getBlockPos(), 30, 15, 30,
                            (world, pos, state, abovePos, aboveState) -> (aboveState.isAir() || aboveState.getMaterial().isLiquid()) && !state.isAir());
                    return;
                } else if (entity instanceof VexEntity) {
                    Subathon.scheduler.schedule(Subathon.server, 600, entity::kill);
                }

                if (entity instanceof MobEntity mob) {
                    mob.initialize(player.getWorld(), player.getWorld().getLocalDifficulty(player.getBlockPos()), SpawnReason.EVENT, null, null);
                }

                player.getWorld().spawnEntity(entity);
            }
        }
    }, Subathon.identifier("control/chaos/mobs"))),

    NUKE(1, control -> MessageUtils.broadcast(player -> {
        if (!player.world.isClient) {
            player.getWorld().createExplosion(player, player.getX(), player.getY(), player.getZ(),
                    Utils.getRandomFromRange(Subathon.getConfigData().explosionMinPower, Subathon.getConfigData().explosionMaxPower),
                    Explosion.DestructionType.BREAK);
        }
    }, Subathon.identifier("control/chaos/nuke"))),

    RANDOM_TIME(1.2, control -> {
        Random random = new Random();
        Subathon.server.getOverworld().setTimeOfDay(random.nextLong(24001));
    }),

    RANDOM_WEATHER(1.2, control -> {
        Random random = new Random();
        int clearWeatherTime = ((ServerWorldMixin) Subathon.server.getOverworld()).getWorldProperties().getClearWeatherTime();
        int rainTime = ((ServerWorldMixin) Subathon.server.getOverworld()).getWorldProperties().getRainTime();
        boolean raining = ((ServerWorldMixin) Subathon.server.getOverworld()).getWorldProperties().isRaining();
        boolean thundering = ((ServerWorldMixin) Subathon.server.getOverworld()).getWorldProperties().isThundering();

        boolean rain = random.nextBoolean();
        if (!thundering) {
            Subathon.server.getOverworld().setWeather(clearWeatherTime, rainTime, true, rain);
        } else {
            Subathon.server.getOverworld().setWeather(clearWeatherTime, rainTime, rain, false);
        }
    }),

    FIREBALLS(5, control -> MessageUtils.broadcast(player -> {
        if (!player.world.isClient) {
            Vec3d playerPos = player.getPos();
            Random random = new Random();

            List<Runnable> list = new ArrayList<>();

            Utils.makeCylinder(playerPos, fireballPos -> {
                fireballPos = fireballPos.add(random.nextDouble(2) - 1, 2 + random.nextDouble(2), random.nextDouble(2) - 1);
                Vec3d velocity = VectorHelper.getMovementVelocity(fireballPos, playerPos.add(0, 1, 0), 1);
                FireballEntity fireballEntity = new FireballEntity(player.world, player, velocity.x, velocity.y, velocity.z, 3);
                fireballEntity.setPos(fireballPos.x, fireballPos.y, fireballPos.z);
                list.add(() -> player.world.spawnEntity(fireballEntity));
                Subathon.scheduler.schedule(Subathon.server, 200, fireballEntity::discard);
            }, 5, 5, false);

            Collections.shuffle(list);

            list.forEach(Runnable::run);
        }
    }, Subathon.identifier("control/chaos/fireballs"))),

    YEET(0.3, control -> MessageUtils.broadcast(player -> {
        Random random = new Random();
        Vec3d yeetPos = player.getPos().add(random.nextDouble(24) - 12, random.nextDouble(10) + 2, random.nextDouble(24) - 12);
        Vec3d velocity = VectorHelper.getMovementVelocity(player.getPos(), yeetPos, random.nextFloat(15));
        player.setVelocity(velocity);
        ((EntityMixin) player).callScheduleVelocityUpdate();
    }, Subathon.identifier("control/chaos/yeet"))),

    DROP_ITEM(0.8, control -> MessageUtils.broadcast(player -> {
        if (!player.world.isClient) {
            Random random = new Random();
            PlayerInventory playerInventory = player.getInventory();
            ItemStack itemStack = playerInventory.dropSelectedItem(true);
            player.currentScreenHandler.getSlotIndex(playerInventory, playerInventory.selectedSlot)
                    .ifPresent(index -> player.currentScreenHandler.setPreviousTrackedSlot(index, playerInventory.getMainHandStack()));
            player.dropItem(itemStack, random.nextInt(10) == 0, true);
        }
    }, Subathon.identifier("control/chaos/drop_item"))),

    DROP_HOTBAR(0.4, control -> MessageUtils.broadcast(player -> {
        if (!player.world.isClient()) {
            Random random = new Random();
            boolean throwRandomly = random.nextInt(10) == 0;
            for (int i = 0; i < 9; i++) {
                ItemStack itemStack = player.getInventory().getStack(i);
                if (itemStack.isEmpty()) {
                    continue;
                }
                ItemStack finalStack = player.getInventory().removeStack(i, itemStack.getCount());
                player.dropItem(finalStack, throwRandomly, true);
            }
        }
    }, Subathon.identifier("control/chaos/drop_hotbar"))),

    DROP_INV(0.2, control -> MessageUtils.broadcast(player -> {
        if (!player.world.isClient) {
            Random random = new Random();
            boolean throwRandomly = random.nextInt(10) != 0;
            for (int i = 0; i < player.getInventory().main.size(); i++) {
                ItemStack itemStack = player.getInventory().getStack(i);
                if (itemStack.isEmpty()) {
                    continue;
                }
                ItemStack finalStack = player.getInventory().removeStack(i, itemStack.getCount());
                player.dropItem(finalStack, throwRandomly, true);
            }
        }
    }, Subathon.identifier("control/chaos/drop_inv"))),

    DROP_EVERYTHING(0.1, control -> MessageUtils.broadcast(player -> {
        if (!player.world.isClient) {
            Random random = new Random();
            boolean throwRandomly = random.nextInt(10) != 0;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack itemStack = player.getInventory().getStack(i);
                if (itemStack.isEmpty()) {
                    continue;
                }
                ItemStack finalStack = player.getInventory().removeStack(i, itemStack.getCount());
                player.dropItem(finalStack, throwRandomly, true);
            }
        }
    }, Subathon.identifier("control/chaos/drop_everything"))),

    BACK_TO_SPAWN(0.1, control -> MessageUtils.broadcast(player -> {
                float spawnAngle = player.getSpawnAngle();
                ServerWorld world = Subathon.server.getWorld(player.getSpawnPointDimension());
                BlockPos spawnPointPosition = player.getSpawnPointPosition();

                Optional<Vec3d> respawnPosition = world != null && spawnPointPosition != null ?
                        PlayerEntity.findRespawnPosition(world, spawnPointPosition, spawnAngle, player.isSpawnForced(), true) : Optional.empty();

                ServerWorld serverWorld = world != null && respawnPosition.isPresent() ? world : Subathon.server.getOverworld();

                if (respawnPosition.isPresent()) {
                    Vec3d vec3d = respawnPosition.get();
                    if (serverWorld == player.getWorld()) {
                        player.networkHandler.requestTeleport(vec3d.x, vec3d.y, vec3d.z, spawnAngle, 0.0f);
                    } else {
                        player.teleport(world, vec3d.x, vec3d.y, vec3d.z, spawnAngle, 0.0f);
                    }
                } else {
                    BlockPos spawnPos = Subathon.server.getOverworld().getSpawnPos();
                    float spawnAngle1 = Subathon.server.getOverworld().getSpawnAngle();
                    if (serverWorld == player.getWorld()) {
                        player.networkHandler.requestTeleport(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), spawnAngle1, 0.0f);
                    } else {
                        player.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), spawnAngle1, 0.0f);
                    }
                }
            },
            Subathon.identifier("control/chaos/back_to_spawn"))),

    ANVIL_RAIN(1, control -> MessageUtils.broadcast(player -> {
        if (!player.world.isClient) {
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.sendMessage(Text.translatable("text.subathon.warning.volume"), true);
            Utils.makeCylinder(player.getPos().add(0, 50, 0), vec3d -> {
                FallingBlockEntity fallingBlockEntity = new FallingBlockEntity(player.getWorld(), vec3d.x, vec3d.y, vec3d.z, Blocks.DAMAGED_ANVIL.getDefaultState());
                fallingBlockEntity.setHurtEntities(2.0f, 40);
                fallingBlockEntity.dropItem = false;
                player.getWorld().spawnEntity(fallingBlockEntity);
            }, 5, 5, true);
        }
    }, Subathon.identifier("control/chaos/anvil_rain"))),

    TNT_RAIN(1, control -> MessageUtils.broadcast(player -> {
        if (!player.world.isClient) {
            AtomicInteger timer = new AtomicInteger(120);
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.sendMessage(Text.translatable("text.subathon.warning.volume"), true);
            Utils.makeCylinder(player.getPos().add(0, 50, 0), vec3d -> {
                TntEntity tntEntity = new TntEntity(player.getWorld(), vec3d.x, vec3d.y, vec3d.z, null);
                tntEntity.setVelocity(0, 0, 0);
                tntEntity.setFuse(120);
                player.getWorld().spawnEntity(tntEntity);
            }, 5, 5, true);
        }
    }, Subathon.identifier("control/chaos/anvil_rain"))),

    FIREBALL_RAIN(1, control -> MessageUtils.broadcast(player -> {
        if (!player.world.isClient) {
            AtomicInteger diff = new AtomicInteger();
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.sendMessage(Text.translatable("text.subathon.warning.volume"), true);
            Utils.makeCylinder(player.getPos().add(0, 50, 0), vec3d -> {
                FireballEntity fireballEntity = new FireballEntity(player.world, player, 0, -10, 0, 5);
                fireballEntity.setPos(vec3d.x, vec3d.y + diff.getAndIncrement() * 2, vec3d.z);
                player.getWorld().spawnEntity(fireballEntity);
                Subathon.scheduler.schedule(Subathon.server, 200, fireballEntity::discard);
            }, 5, 5, true);
        }
    }, Subathon.identifier("control/chaos/fireball_rain"))),

    HUNGER(0.3, control -> MessageUtils.broadcast(player -> {
        HungerManager hungerManager = player.getHungerManager();
        int foodLevel = hungerManager.getFoodLevel();
        float saturationLevel = hungerManager.getSaturationLevel();
        hungerManager.add(-foodLevel, -saturationLevel);
    }, Subathon.identifier("control/chaos/hunger"))),

    STORM(0.3, control -> MessageUtils.broadcast(player -> {
        if (!player.world.isClient) {
            List<Runnable> list = new ArrayList<>();
            AtomicInteger timer = new AtomicInteger();
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.playSound(SoundEvents.BLOCK_BELL_USE, SoundCategory.MASTER, 100, 0);
            player.sendMessage(Text.translatable("text.subathon.warning.volume"), true);
            Utils.makeCylinder(player.getPos(), vec3d -> {
                LightningEntity lightningEntity = new LightningEntity(EntityType.LIGHTNING_BOLT, player.getWorld());
                lightningEntity.setPos(vec3d.x, vec3d.y, vec3d.z);
                list.add(() -> Subathon.scheduler.schedule(Subathon.server, timer.getAndIncrement(), () -> player.getWorld().spawnEntity(lightningEntity)));
            }, 10, 10, true);

            Collections.shuffle(list);
            list.forEach(Runnable::run);
        }
    }, Subathon.identifier("control/chaos/storm"))),
    ;

    private final double defaultWeight;
    private final Consumer<GameControl> consumer;

    ChaosMode(double defaultWeight, Consumer<GameControl> consumer) {
        this.defaultWeight = defaultWeight;
        this.consumer = consumer;
    }

    public Consumer<GameControl> getConsumer() {
        return consumer;
    }

    public void accept(GameControl control) {
        consumer.accept(control);
    }

    public double getDefaultWeight() {
        return defaultWeight;
    }
}
