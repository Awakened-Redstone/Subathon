package com.awakenedredstone.subathon.potions;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class SubathonStatusEffect extends StatusEffect {
    public SubathonStatusEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x98D982);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) { return false; }

    //Does nothing since this is a dummy potion effect
    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {}
}
