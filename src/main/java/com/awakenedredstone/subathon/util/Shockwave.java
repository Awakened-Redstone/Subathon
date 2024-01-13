package com.awakenedredstone.subathon.util;

import com.awakenedredstone.subathon.Subathon;
import lombok.Builder;
import lombok.NonNull;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.krlite.equator.visual.color.AccurateColor;
import net.krlite.equator.visual.color.Palette;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

@Builder
public class Shockwave {
    @NonNull ServerWorld world;
    @NonNull Vec3d pos;
    float radius;
    long duration;
    @Builder.Default
    @NonNull AccurateColor color = Palette.CYAN;

    public void spawn() {
        sendPacket();

        int centerX = (int) pos.getX();
        int centerY = (int) pos.getY();
        int centerZ = (int) pos.getZ();

        Set<BlockPos> blocks = new HashSet<>();
        //I need to know what is the size of the step I need to linearly apply the timed effect to the blocks, so I have to
        //use the radius and the duration to figure that out
        int step = (int) Math.ceil((double) radius / duration);
        int time = 0;

        for (int r = 0; r < radius; r++) {
            for (float angle = 0; angle < 360; angle += 180f / r) {
                double x = centerX + r * Math.cos(Math.toRadians(angle));
                double z = centerZ + r * Math.sin(Math.toRadians(angle));
                BlockPos pos2 = BlockPos.ofFloored(x, centerY, z);
                //placeSculk(world, pos2);
                if (blocks.add(pos2)) {
                    Subathon.getInstance().getScheduler().schedule(Subathon.getServer(), time, () -> {
                        placeSculk(world, pos2);
                    });
                }
            }
            //properly increase the time every step
            if (r % step == 0) {
                time++;
            }
        }
    }

    private void sendPacket() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVector3f(pos.toVector3f());
        buf.writeFloat(0);
        buf.writeFloat(radius);
        buf.writeLong(world.getTime() + duration);
        buf.writeInt(color.toInt());

        Subathon.getServer().getPlayerManager().sendToAll(ServerPlayNetworking.createS2CPacket(Subathon.id("fx/shockwave"), buf));
    }

    private void placeSculk(ServerWorld world, BlockPos blockPos) {
        world.getRegistryManager().getOptional(RegistryKeys.CONFIGURED_FEATURE)
            .flatMap(registry -> registry.getEntry(RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Subathon.id("sculk_patch"))))
            .ifPresent(reference -> reference.value().generate(world, world.getChunkManager().getChunkGenerator(), world.random, blockPos));
    }
}
