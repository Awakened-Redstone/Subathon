package com.awakenedredstone.subathon.core.effect;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.process.Effect;
import com.awakenedredstone.subathon.core.effect.process.RegisterEffect;
import com.awakenedredstone.subathon.ui.configure.PotionWeightsScreen;
import com.awakenedredstone.subathon.util.MessageUtils;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.parsing.UIModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Random;

@RegisterEffect("subathon:rando_potions")
public class RandoPotionsEffect extends Effect {

    public RandoPotionsEffect(Identifier identified) {
        super(identified);
        scalable = false;
    }

    @Override
    public void trigger(PlayerEntity player) {
        applyRandoEffect(player);
    }

    @Override
    public void trigger(World world) {
        MessageUtils.broadcast(this::applyRandoEffect, Subathon.id("apply/rando_potions"));
    }

    private void applyRandoEffect(PlayerEntity player) {
        Random random = new Random();
        double durationGaussian = Math.abs(random.nextGaussian(15, 20));
        if (durationGaussian < 5) durationGaussian += 5;
        double amplifierGaussian = Math.abs(random.nextGaussian(0, 23));
        int duration = (int) Math.round(durationGaussian * 20);
        int amplifier = (int) Math.round(amplifierGaussian);
        StatusEffect effect = Subathon.potionsRandom.next();
        if (player.hasStatusEffect(effect)) {
            StatusEffectInstance playerEffect = player.getStatusEffect(effect);
            duration += playerEffect.getDuration();
            amplifier = Math.max(amplifier, playerEffect.getAmplifier());
            player.removeStatusEffect(playerEffect.getEffectType());
        }
        player.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier, false, false, true));
    }

    @Override
    public void generateConfig(FlowLayout container, UIModel model) {
        container.child(Components.button(Text.translatable("text.subathon.screen.potion_effects.open"),
                        button -> MinecraftClient.getInstance().setScreen(new PotionWeightsScreen()))
                .sizing(Sizing.content()));

        //Option<List<?>> excludedPotions = Subathon.COMMON_CONFIGS.optionForKey(Option.Key.ROOT.child("excludedPotions"));
        //var result = POTION_LIST().make(model, excludedPotions);
        //container.child(result.optionContainer());
    }

    /*@SuppressWarnings({"unchecked", "rawtypes"})
    private OptionComponentFactory<List<?>> POTION_LIST() {
        return (model, option) -> {
            var layout = new PotionListOptionContainer(option);
            return new OptionComponentFactory.Result(layout, layout);
        };
    }*/
}
