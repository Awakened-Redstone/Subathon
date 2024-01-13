package com.awakenedredstone.subathon.registry;

import com.awakenedredstone.subathon.core.effect.*;
import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.minecraft.registry.Registry;

@SuppressWarnings("unused")
public class EffectRegistry implements AutoRegistryContainer<Effect> {

    public static final Effect JUMP = new JumpEffect();
    public static final Effect RANDO_POTIONS = new RandoPotionsEffect();
    public static final Effect CHAOS = new ChaosEffect();
    public static final Effect INCREASE_RECIPE_OUTPUT = new IncreaseRecipeOutputEffect();
    //public static final Effect TEST = new TestEffect();

    @Override
    public Registry<Effect> getRegistry() {
        return SubathonRegistries.EFFECTS;
    }

    @Override
    public Class<Effect> getTargetFieldType() {
        return Effect.class;
    }
}