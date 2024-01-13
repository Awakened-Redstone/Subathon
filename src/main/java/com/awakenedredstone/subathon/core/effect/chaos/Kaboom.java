package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.entity.FireballEntity;
import com.awakenedredstone.subathon.util.Texts;
import com.awakenedredstone.subathon.util.Utils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Kaboom extends Chaos {

    public Kaboom() {
        super(10);
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Random random = new Random();
            AtomicInteger diff = new AtomicInteger();
            if (!player.getWorld().isSkyVisible(player.getBlockPos())) {
                player.sendMessage(Texts.of("text.subathon.chaos.subathon.kaboom.message.special"), true);
            } else player.sendMessage(Texts.of("text.subathon.chaos.subathon.kaboom.message.normal"), true);
            Vec3d pos = serverPlayer.getPos().add(0, 50, 0);
            boolean drill = random.nextBoolean();
            Utils.makeCylinder(pos, vec3d -> {
                FireballEntity fireballEntity = new FireballEntity(player.getWorld(), player, 0, -20, 0, 5, (int) player.getY());
                fireballEntity.refreshPositionAndAngles(vec3d.x, drill ? vec3d.y + diff.getAndIncrement() * 2 : vec3d.y, vec3d.z, fireballEntity.getYaw(), fireballEntity.getPitch());
                Subathon.getInstance().getScheduler().schedule(Subathon.getServer(), 1, () -> serverPlayer.getWorld().spawnEntity(fireballEntity));
            }, 5, 5, true);
        } else return false;
        return true;
    }
}
