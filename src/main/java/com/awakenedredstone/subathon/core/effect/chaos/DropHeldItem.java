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

@RegisterChaos("subathon:drop_held")
public class DropHeldItem extends Chaos {

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
            PlayerInventory playerInventory = player.getInventory();
            ItemStack itemStack = playerInventory.dropSelectedItem(true);
            droppedSomething = !itemStack.isEmpty();
            player.dropItem(itemStack, random.nextInt(10) == 0, true);
        }
        return droppedSomething;
    }
}
