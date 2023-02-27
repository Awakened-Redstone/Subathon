package com.awakenedredstone.subathon.core.effect;

import com.awakenedredstone.subathon.registry.SubathonRegistries;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.parsing.UIModel;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public abstract class Effect {
    @Getter(lazy = true)
    private final Identifier identifier = getIdentifierFromRegistry();
    public boolean enabled = false;
    public double scale = 1;
    protected boolean scalable = true;

    public abstract void trigger(PlayerEntity player);

    public abstract void trigger(World world);

    public abstract void generateConfig(FlowLayout container, UIModel model);

    public boolean isScalable() {
        return scalable;
    }

    private Identifier getIdentifierFromRegistry() {
        return SubathonRegistries.EFFECTS.getId(this);
    }
}
