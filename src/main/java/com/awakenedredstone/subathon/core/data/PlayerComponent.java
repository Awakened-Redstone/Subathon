package com.awakenedredstone.subathon.core.data;

import com.awakenedredstone.subathon.registry.SubathonCriteria;
import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerComponent extends TwitchDataComponent<PlayerEntity> implements dev.onyxstudios.cca.api.v3.entity.PlayerComponent<Component>, AutoSyncedComponent {

    public PlayerComponent(PlayerEntity player) {
        super(player, Components.PLAYER_DATA);
    }

    /*@Override
    public void addPoints(long amount) {
        super.addPoints(amount);
        SubathonCriteria.EFFECT_CRITERION.trigger((ServerPlayerEntity) target, points);
    }*/

    /*@Override
    public void setPoints(int amount) {
        super.setPoints(amount);
        SubathonCriteria.EFFECT_CRITERION.trigger((ServerPlayerEntity) target, points);
    }*/

    @Override
    public void readFromNbt(NbtCompound tag) {
        this.points = tag.getInt("points");
        this.pointStorage = tag.getInt("pointStorage");
        this.bitStorage = tag.getInt("bitStorage");
        this.subPointsStorage = tag.getInt("subPointsStorage");
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.putLong("points", points);
        tag.putLong("pointStorage", pointStorage);
        tag.putLong("bitStorage", bitStorage);
        tag.putLong("subPointsStorage", subPointsStorage);
    }
}
