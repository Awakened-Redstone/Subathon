package com.awakenedredstone.subathon.core.effect.process;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.parsing.UIModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public abstract class Effect {
    public final Identifier identifier;
    public boolean enabled = false;
    public double scale = 1;
    protected boolean scalable = true;

    public Effect(Identifier identified) {
        this.identifier = identified;
    }

    public abstract void trigger(PlayerEntity player);

    public abstract void trigger(World world);

    public abstract void generateConfig(FlowLayout container, UIModel model);

    public boolean isScalable() {
        return scalable;
    }
}
