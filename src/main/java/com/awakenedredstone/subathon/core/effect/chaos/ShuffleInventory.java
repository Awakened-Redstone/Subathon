package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.chaos.process.Chaos;
import com.awakenedredstone.subathon.core.effect.chaos.process.RegisterChaos;
import com.awakenedredstone.subathon.util.MessageUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Random;

@RegisterChaos("subathon:shuffle")
public class ShuffleInventory extends Chaos {

    @Override
    public boolean trigger(World world) {
        MessageUtils.broadcast(this::trigger, Subathon.id("chaos"));
        return true;
    }

    @Override
    public boolean trigger(PlayerEntity player) {
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
