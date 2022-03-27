package com.awakenedredstone.subathon.mixin;

import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractInventoryScreen.class)
public class AbstractInventoryScreenMixin {

    @Inject(method = "getStatusEffectDescription(Lnet/minecraft/entity/effect/StatusEffectInstance;)Lnet/minecraft/text/Text;", at = @At("RETURN"), cancellable = true)
    private void getStatusEffectDescription(StatusEffectInstance statusEffect, CallbackInfoReturnable<Text> cir) {
        if (statusEffect.getAmplifier() == 1 || statusEffect.getAmplifier() > 9) {
            MutableText mutableText = cir.getReturnValue().shallowCopy();
            mutableText.append(" ").append(new LiteralText("" + (statusEffect.getAmplifier() + 1)));
            cir.setReturnValue(mutableText);
        }
    }
}
