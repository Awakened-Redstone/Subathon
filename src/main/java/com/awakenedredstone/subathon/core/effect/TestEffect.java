package com.awakenedredstone.subathon.core.effect;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.parsing.UIModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class TestEffect extends Effect {

    public TestEffect() {
        scalable = false;
    }

    @Override
    public void trigger(PlayerEntity player) {/**/}

    @Override
    public void trigger(World world) {/**/}

    @Override
    public void generateConfig(FlowLayout container, UIModel model) {/**/}
}
