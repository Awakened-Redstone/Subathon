package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.chaos.process.Chaos;
import com.awakenedredstone.subathon.core.effect.chaos.process.RegisterChaos;
import com.awakenedredstone.subathon.util.MessageUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Random;

@RegisterChaos("subathon:drop_inventory")
public class DropInventory extends Chaos {

    @Override
    public boolean trigger(World world) {
        MessageUtils.broadcast(this::trigger, Subathon.id("chaos"));
        return true;
    }

    @Override
    public boolean trigger(PlayerEntity player) {
        boolean droppedSomething = false;
        if (!player.world.isClient) {
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
