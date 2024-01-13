package com.awakenedredstone.subathon.core.effect.chaos;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.Random;

public class ShuffleInventory extends Chaos {

    public ShuffleInventory() {
        super(18);
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        Random random = new Random();
        PlayerInventory inventory = player.getInventory();
        int size = inventory.main.size();
        for (int i = 0; i < size; i++) {
            int slotA = random.nextInt(size);
            int slotB = random.nextInt(size);

            ItemStack stackA = inventory.getStack(slotA);
            ItemStack stackB = inventory.getStack(slotB);

            if (stackA != null) {
                inventory.removeStack(slotA);
            }
            if (stackB != null) {
                inventory.removeStack(slotB);
            }

            if (stackA != null) {
                inventory.setStack(slotB, stackA);
            }
            if (stackB != null) {
                inventory.setStack(slotA, stackB);
            }
        }
        return !inventory.isEmpty();
    }
}
