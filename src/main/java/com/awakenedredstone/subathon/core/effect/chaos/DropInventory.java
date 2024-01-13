package com.awakenedredstone.subathon.core.effect.chaos;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.Random;

public class DropInventory extends Chaos {

    public DropInventory() {
        super(10);
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        boolean droppedSomething = false;
        if (!player.getWorld().isClient()) {
            Random random = new Random();
            boolean throwRandomly = random.nextInt(10) != 0;
            for (int i = 0; i < player.getInventory().main.size(); i++) {
                ItemStack itemStack = player.getInventory().getStack(i);
                if (itemStack.isEmpty()) {
                    continue;
                } else droppedSomething = true;
                ItemStack finalStack = player.getInventory().removeStack(i, itemStack.getCount());
                player.dropItem(finalStack, throwRandomly, true);
            }
        }
        return droppedSomething;
    }
}
