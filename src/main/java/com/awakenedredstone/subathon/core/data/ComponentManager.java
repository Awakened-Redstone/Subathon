package com.awakenedredstone.subathon.core.data;

import com.awakenedredstone.subathon.Subathon;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public class ComponentManager {

    public static TwitchDataComponent<?> getComponent(MinecraftServer server, PlayerEntity player) {
        if (Subathon.COMMON_CONFIGS.sharedEffects() || player == null) {
            return Components.WORLD_DATA.get(server.getOverworld());
        } else {
            return Components.PLAYER_DATA.get(player);
        }
    }
}
