package com.awakenedredstone.subathon.core.effect.chaos;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.Random;

public class DropHeldItem extends Chaos {

    public DropHeldItem() {
        super(19);
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        boolean droppedSomething = false;
        if (!player.getWorld().isClient()) {
            Random random = new Random();
            PlayerInventory playerInventory = player.getInventory();
            ItemStack itemStack = playerInventory.dropSelectedItem(true);
            droppedSomething = !itemStack.isEmpty();
            player.dropItem(itemStack, random.nextInt(10) == 0, true);
        }
        return droppedSomething;
    }
}
