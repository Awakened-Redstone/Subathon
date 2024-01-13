package com.awakenedredstone.subathon.core.data;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.registry.SubathonCriteria;
import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

public class WorldComponent extends TwitchDataComponent<World> implements Component, AutoSyncedComponent {

    public WorldComponent(World world) {
        super(world, Components.WORLD_DATA);
    }

    /*@Override
    public void addPoints(long amount) {
        super.addPoints(amount);
        Subathon.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            if (player != null) {
                SubathonCriteria.EFFECT_CRITERION.trigger(player, points);
            }
        });
    }*/

    /*@Override
    public void setPoints(int amount) {
        super.setPoints(amount);
        Subathon.getServer().getPlayerManager().getPlayerList().forEach(player -> {
            if (player != null) {
                SubathonCriteria.EFFECT_CRITERION.trigger(player, points);
            }
        });
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
