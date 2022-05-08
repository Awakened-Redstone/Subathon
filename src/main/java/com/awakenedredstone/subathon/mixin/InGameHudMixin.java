package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.events.HudRenderCallback;
import com.awakenedredstone.subathon.render.PositionedText;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Environment(value= EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow @Final private MinecraftClient client;

    @Shadow private int scaledHeight;

    @Shadow protected abstract void drawHeart(MatrixStack matrices, InGameHud.HeartType type, int x, int y, int v, boolean blinking, boolean halfHeart);

    @Shadow @Final private Random random;

    @Shadow public abstract TextRenderer getTextRenderer();

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

    @Inject(method = "renderHealthBar", at = @At(value = "HEAD"), cancellable = true)
    public void renderHealthBar(MatrixStack matrices, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking, CallbackInfo ci) {
        if (maxHealth > 220) {
            SubathonClient.positionedTexts.put(-16L, new PositionedText(new LiteralText(String.format("x%d", health)), true, x + 10, y, 0xFFFFFF, 1, 1, 0, 0));

            InGameHud.HeartType heartType = InGameHud.HeartType.fromPlayerState(player);
            int v = 9 * (player.world.getLevelProperties().isHardcore() ? 5 : 0);
            int q = y;
            if (lastHealth + absorption <= 4) {
                q += this.random.nextInt(2);
            }
            this.drawHeart(matrices, InGameHud.HeartType.CONTAINER, x, q, v, blinking, false);
            if (blinking) {
                this.drawHeart(matrices, heartType, x, q, v, true, health == 1);
            }
            this.drawHeart(matrices, heartType, x, q, v, false, health == 1);
            ci.cancel();
        }
    }

    @Inject(method = "drawHeart", at = @At(value = "HEAD"), cancellable = true)
    public void drawHeart(MatrixStack matrices, InGameHud.HeartType type, int x, int y, int v, boolean blinking, boolean halfHeart, CallbackInfo ci) {
        if (y < -3) ci.cancel();
    }

    @ModifyVariable(method = "renderStatusBars", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/util/math/MathHelper;ceil(F)I"), index = 15)
    public int renderStatusBars(int value) {
        if (client.player == null) return value;
        return client.player.getMaxHealth() > 220 ? 1 : value;
    }
}
