package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class ReturnToSpawn extends Chaos {

    public ReturnToSpawn() {
        super(6);
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            float spawnAngle = serverPlayer.getSpawnAngle();
            ServerWorld world = Subathon.getServer().getWorld(serverPlayer.getSpawnPointDimension());
            BlockPos spawnPointPosition = serverPlayer.getSpawnPointPosition();

            Optional<Vec3d> respawnPosition = world != null && spawnPointPosition != null ?
                    PlayerEntity.findRespawnPosition(world, spawnPointPosition, spawnAngle, serverPlayer.isSpawnForced(), true) : Optional.empty();

            ServerWorld serverWorld = world != null && respawnPosition.isPresent() ? world : Subathon.getServer().getOverworld();

            if (respawnPosition.isPresent()) {
                Vec3d vec3d = respawnPosition.get();
                if (serverWorld == player.getWorld()) {
                    serverPlayer.networkHandler.requestTeleport(vec3d.x, vec3d.y, vec3d.z, spawnAngle, 0.0f);
                } else {
                    serverPlayer.teleport(world, vec3d.x, vec3d.y, vec3d.z, spawnAngle, 0.0f);
                }
            } else {
                BlockPos spawnPos = Subathon.getServer().getOverworld().getSpawnPos();
                float spawnAngle1 = Subathon.getServer().getOverworld().getSpawnAngle();
                if (serverWorld == player.getWorld()) {
                    serverPlayer.networkHandler.requestTeleport(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), spawnAngle1, 0.0f);
                } else {
                    serverPlayer.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), spawnAngle1, 0.0f);
                }
            }
        }
        return true;
    }
}
