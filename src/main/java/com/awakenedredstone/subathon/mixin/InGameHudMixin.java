package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.events.HudRenderCallback;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value= EnvType.CLIENT)
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "tick()V", at = @At(value = "HEAD"))
    public void tick(CallbackInfo ci) {
        HudRenderCallback.TICK.invoker().onHudTick();
    }

    @Inject(method = "tick(Z)V", at = @At(value = "HEAD"))
    public void preTick(boolean paused, CallbackInfo ci) {
        HudRenderCallback.PRE_TICK.invoker().onHudPreTick(paused);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;render(Lnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V")))
    public void render(MatrixStack matrixStack, float tickDelta, CallbackInfo callbackInfo) {
        HudRenderCallback.RENDER.invoker().onHudRender(matrixStack, tickDelta);
    }
}
