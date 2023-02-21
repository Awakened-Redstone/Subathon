package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.chaos.process.Chaos;
import com.awakenedredstone.subathon.core.effect.chaos.process.RegisterChaos;
import com.awakenedredstone.subathon.entity.FireballEntity;
import com.awakenedredstone.subathon.util.MessageUtils;
import com.awakenedredstone.subathon.util.Texts;
import com.awakenedredstone.subathon.util.Utils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@RegisterChaos("subathon:kaboom")
public class Kaboom extends Chaos {

    @Override
    public boolean trigger(World world) {
        MessageUtils.broadcast(this::trigger, Subathon.id("chaos"));
        return true;
    }

    @Override
    public boolean trigger(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Random random = new Random();
            AtomicInteger diff = new AtomicInteger();
            if (!player.world.isSkyVisible(player.getBlockPos())) {
                player.sendMessage(Texts.of("text.subathon.chaos.subathon.kaboom.message.special"), true);
            } else player.sendMessage(Texts.of("text.subathon.chaos.subathon.kaboom.message.normal"), true);
            Vec3d pos = serverPlayer.getPos().add(0, 50, 0);
            boolean drill = random.nextBoolean();
            Utils.makeCylinder(pos, vec3d -> {
                FireballEntity fireballEntity = new FireballEntity(player.world, player, 0, -20, 0, 5, (int) player.getY());
                fireballEntity.refreshPositionAndAngles(vec3d.x, drill ? vec3d.y + diff.getAndIncrement() * 2 : vec3d.y, vec3d.z, fireballEntity.getYaw(), fireballEntity.getPitch());
                Subathon.scheduler.schedule(Subathon.server, 1, () -> serverPlayer.getWorld().spawnEntityAndPassengers(fireballEntity));
            }, 5, 5, true);
        } else return false;
        return true;
    }
}
