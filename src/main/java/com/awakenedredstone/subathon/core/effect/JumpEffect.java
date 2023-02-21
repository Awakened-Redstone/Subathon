package com.awakenedredstone.subathon.core.effect;

import com.awakenedredstone.subathon.core.effect.process.Effect;
import com.awakenedredstone.subathon.core.effect.process.RegisterEffect;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.parsing.UIModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

@RegisterEffect("subathon:jump")
public class JumpEffect extends Effect {

    public JumpEffect(Identifier identified) {
        super(identified);
    }

    @Override
    public void trigger(PlayerEntity player) {/**/}

    @Override
    public void trigger(World world) {/**/}

    @Override
    public void generateConfig(FlowLayout container, UIModel model) {}
}
