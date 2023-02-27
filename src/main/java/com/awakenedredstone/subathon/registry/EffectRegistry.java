package com.awakenedredstone.subathon.registry;

import com.awakenedredstone.subathon.core.effect.ChaosEffect;
import com.awakenedredstone.subathon.core.effect.Effect;
import com.awakenedredstone.subathon.core.effect.JumpEffect;
import com.awakenedredstone.subathon.core.effect.RandoPotionsEffect;
import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.minecraft.registry.Registry;

@SuppressWarnings("unused")
public class EffectRegistry implements AutoRegistryContainer<Effect> {

    public static final Effect JUMP = new JumpEffect();
    public static final Effect RANDO_POTIONS = new RandoPotionsEffect();
    public static final Effect CHAOS = new ChaosEffect();
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