package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.chaos.process.Chaos;
import com.awakenedredstone.subathon.core.effect.chaos.process.RegisterChaos;
import com.awakenedredstone.subathon.util.MessageUtils;
import com.awakenedredstone.subathon.util.Texts;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LargeEntitySpawnHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@RegisterChaos("subathon:random_item")
public class RandomItem extends Chaos {

    @Override
    public boolean trigger(World world) {
        MessageUtils.broadcast(this::trigger, Subathon.id("chaos"));
        return true;
    }

    @Override
    public boolean trigger(PlayerEntity player) {
        int success = 0;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Random random = new Random();
            for (int i = 0; i < 20; i++) {
                Item item = Registries.ITEM.getRandom(player.getRandom()).get().value();
                ItemStack itemStack = new ItemStack(item);
                NbtCompound nbt = new NbtCompound();
                for (int j = 0; j < 10; j++) {
                    nbt.putString(uuid(), uuid());
                }
                itemStack.setNbt(nbt);
                itemStack.setCustomName(Text.literal(uuid()));

                ItemEntity itemEntity = new ItemEntity(player.world, player.getX(), player.getY(), player.getZ(), itemStack);
                itemEntity.setPickupDelay(0);
                if (random.nextBoolean()) {
                    Optional<ItemEntity> entity = trySpawnAt(itemEntity, serverPlayer.getWorld(), player.getBlockPos(), 30, 5, 5,
                            (world, pos, state, abovePos, aboveState) -> (aboveState.isAir() || aboveState.getMaterial().isLiquid()) && !state.isAir());
                    if (entity.isPresent()) success++;
                } else {
                    Subathon.scheduler.schedule(Subathon.server, 1, () -> serverPlayer.getWorld().spawnEntityAndPassengers(itemEntity));
                    success++;
                }
            }
            if (success >= 16) player.sendMessage(Texts.of("text.subathon.chaos.subathon.random_item.message"));
        }
        return success >= 16;
    }

    private String uuid() {
        return UUID.randomUUID().toString();
    }

    public static <T extends Entity> Optional<T> trySpawnAt(@NotNull T entity, @NotNull ServerWorld world, @NotNull BlockPos pos, int tries, int horizontalRange, int verticalRange, @NotNull LargeEntitySpawnHelper.Requirements requirements) {
        BlockPos.Mutable mutable = pos.mutableCopy();
        for (int i = 0; i < tries; ++i) {
            int j = MathHelper.nextBetween(world.random, -horizontalRange, horizontalRange);
            int k = MathHelper.nextBetween(world.random, -horizontalRange, horizontalRange);
            mutable.set(pos, j, verticalRange, k);
            if (!world.getWorldBorder().contains(mutable) || !LargeEntitySpawnHelper.findSpawnPos(world, verticalRange, mutable, requirements)) continue;
            entity.setPos(mutable.getX(), mutable.getY() + 1, mutable.getZ());
            world.spawnEntityAndPassengers(entity);
            return Optional.of(entity);
        }
        return Optional.empty();
    }
}
