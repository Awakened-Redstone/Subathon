package com.awakenedredstone.subathon.core.data;

import com.awakenedredstone.subathon.Subathon;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class ComponentManager {

    public static TwitchDataComponent<?> getComponent(World world, PlayerEntity player) {
        if (Subathon.COMMON_CONFIGS.sharedEffects()) {
            return Components.WORLD_DATA.get(world);
        } else {
            return Components.PLAYER_DATA.get(player);
        }
    }
}
