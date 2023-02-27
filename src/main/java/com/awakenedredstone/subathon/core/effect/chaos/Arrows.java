package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.mixin.PersistentProjectileEntityAccessor;
import com.awakenedredstone.subathon.util.Utils;
import com.awakenedredstone.subathon.util.VectorHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Arrows extends Chaos {

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Vec3d playerPos = player.getPos();
            Random random = new Random();

            List<Runnable> arrows = new ArrayList<>();

            Utils.makeCylinder(playerPos, arrowPos -> {
                arrowPos = arrowPos.add(random.nextDouble(2) - 1, 2 + random.nextDouble(2), random.nextDouble(2) - 1);
                ArrowEntity arrowEntity = new ArrowEntity(player.getWorld(), player);
                arrowEntity.setPos(arrowPos.x, arrowPos.y, arrowPos.z);
                Vec3d velocity = VectorHelper.getMovementVelocity(arrowPos, playerPos.add(0, 1, 0), 1);
                arrowEntity.setVelocity(velocity);
                arrowEntity.setDamage(Utils.getRandomFromRange(0, 10));
                arrowEntity.setPunch(Utils.getRandomFromRange(8, 20));
                ((PersistentProjectileEntityAccessor) arrowEntity).setLife(1150);
                arrowEntity.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                arrows.add(() -> player.getWorld().spawnEntity(arrowEntity));
            }, 5, 5, false);

            Collections.shuffle(arrows);

            arrows.forEach(runnable -> Subathon.scheduler.schedule(Subathon.server, 1, runnable));
        } else return false;
        return true;
    }
}
