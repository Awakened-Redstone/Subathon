package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.potions.SubathonStatusEffect;
import com.awakenedredstone.subathon.util.MessageUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(value = EnvType.CLIENT)
@Mixin(AbstractInventoryScreen.class)
public class AbstractInventoryScreenMixin {

    @Inject(method = "getStatusEffectDescription(Lnet/minecraft/entity/effect/StatusEffectInstance;)Lnet/minecraft/text/Text;", at = @At("RETURN"), cancellable = true)
    private void getStatusEffectDescription(StatusEffectInstance statusEffect, CallbackInfoReturnable<Text> cir) {
        if (statusEffect.getEffectType() instanceof SubathonStatusEffect) {
            MutableText mutableText = cir.getReturnValue().shallowCopy();
            mutableText.append(" ").append(new LiteralText("" + (MessageUtils.formatDouble(SubathonClient.value))));
            cir.setReturnValue(mutableText);
        } else if (statusEffect.getAmplifier() < 0) {
            MutableText mutableText = cir.getReturnValue().shallowCopy();
            mutableText.append(" ").append("?");
            cir.setReturnValue(mutableText);
        } else if (statusEffect.getAmplifier() > 9) {
            MutableText mutableText = cir.getReturnValue().shallowCopy();
            mutableText.append(" ").append(String.valueOf(statusEffect.getAmplifier()));
            cir.setReturnValue(mutableText);
        }
    }
}
