package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.registry.SubathonRegistries;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.List;

public abstract class Chaos {
    @Getter(lazy = true)
    private final Identifier identifier = getIdentifierFromRegistry();

    public boolean globalTrigger(World world) {
        List<ServerPlayerEntity> players = Subathon.server.getPlayerManager().getPlayerList();
        short successCount = 0;
        for (ServerPlayerEntity player : players) {
            if (playerTrigger(player)) successCount++;
        }

        return ((double) successCount / players.size()) > 0.85;
    }
    public abstract boolean playerTrigger(PlayerEntity player);

    private Identifier getIdentifierFromRegistry() {
        return SubathonRegistries.CHAOS.getId(this);
    }
}
