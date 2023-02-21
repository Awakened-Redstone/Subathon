package com.awakenedredstone.subathon.core.effect;

import com.awakenedredstone.subathon.core.effect.process.Effect;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.parsing.UIModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class Dummy extends Effect {
    public Dummy(Identifier identified, double scale, boolean enabled) {
        super(identified);
        this.scale = scale;
        this.enabled = enabled;
    }

    @Override
    public void trigger(PlayerEntity player) {/**/}

    @Override
    public void trigger(World world) {/**/}

    @Override
    public void generateConfig(FlowLayout container, UIModel model) {}
}
