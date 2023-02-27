package com.awakenedredstone.subathon.core.effect.chaos;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LargeEntitySpawnHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class RandomCompatibleEnchant extends Chaos {

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        Random random = new Random();
        PlayerInventory inventory = player.getInventory();
        if (inventory.isEmpty()) return false;
        int success = 0;
        for (int i = 0; i < 3; i++) {
            int tries = inventory.size();
            ItemStack stack;
            do {
                if (tries-- <= 0) return false;
                stack = inventory.getStack(random.nextInt(inventory.size()));
            } while (stack.isEmpty());

            int level = (int) Math.round(Math.abs(random.nextGaussian(15, 20)));
            if (level < 5) level += 5;
            if (level > 30) level = 30 - level;
            if (!stack.equals(EnchantmentHelper.enchant(player.getRandom(), stack, level, random.nextBoolean()))) success++;
        }
        return success >= 1;
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
            entity.setPos(mutable.getX(), mutable.getY(), mutable.getZ());
            world.spawnEntityAndPassengers(entity);
            return Optional.of(entity);
        }
        return Optional.empty();
    }
}
