package com.awakenedredstone.subathon.core.effect;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.ui.configure.ChaosWeightsScreen;
import com.awakenedredstone.subathon.config.component.MobListOptionContainer;
import com.awakenedredstone.subathon.core.effect.chaos.Chaos;
import com.awakenedredstone.subathon.registry.ChaosRegistry;
import com.awakenedredstone.subathon.util.MapBuilder;
import com.awakenedredstone.subathon.util.ServerUtils;
import com.awakenedredstone.subathon.util.Texts;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.parsing.UIModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;

public class ChaosEffect extends Effect {

    public ChaosEffect() {
        scalable = false;
    }

    @Override
    public void trigger(PlayerEntity player) {
        boolean success;
        int remainingTries = Subathon.COMMON_CONFIGS.chaosTries();
        do {
            Chaos chaos = Subathon.getInstance().chaosRandom.next();
            success = chaos.playerTrigger(player);
            Subathon.LOGGER.debug(chaos.getIdentifier() + " -> " + success);
            if (remainingTries-- <= 0) {
                player.sendMessage(Texts.of("text.subathon.chaos.error", new MapBuilder.StringMap().putAny("amount", Subathon.COMMON_CONFIGS.chaosTries()).build()), false);
                break;
            }
        } while (!success);
    }

    @Override
    public void trigger(World world) {
        boolean success;
        int remainingTries = Subathon.COMMON_CONFIGS.chaosTries();
        do {
            Chaos chaos = Subathon.getInstance().chaosRandom.next();
            success = chaos.globalTrigger(world);
            //ServerUtils.broadcast(Text.literal(chaos.getIdentifier() + " -> " + success));
            if (remainingTries-- <= 0) {
                ServerUtils.broadcast(Texts.of("text.subathon.chaos.error", new MapBuilder.StringMap().putAny("amount", Subathon.COMMON_CONFIGS.chaosTries()).build()));
                break;
            }
        } while (!success);
    }

    @Override
    public void generateConfig(FlowLayout container, UIModel model) {
        container.child(Components.button(Text.translatable("text.subathon.screen.chaos_effects.open"),
                        button -> MinecraftClient.getInstance().setScreen(new ChaosWeightsScreen()))
                .sizing(Sizing.content()));

        Option<List<?>> excludedEntities = Subathon.COMMON_CONFIGS.optionForKey(Option.Key.ROOT.child("excludedEntities"));
        var result = MOB_LIST().make(model, excludedEntities);
        container.child((Component) result.optionProvider());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private OptionComponentFactory<List<?>> MOB_LIST() {
        return (model, option) -> {
            var layout = new MobListOptionContainer(option);
            return new OptionComponentFactory.Result<>(layout, layout);
        };
    }
}
