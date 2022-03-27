package com.awakenedredstone.subathon.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class PositionedText {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private Text positionedText;
    private final boolean positionedTextShadow;
    private final int[] positionedTextData;
    private int displayTicks = 80;
    private int remainingTime = displayTicks;
    private int fadeOutTicks = 40;
    private int fadeInTicks = 5;
    private final int fontScale;

    public PositionedText(Text positionedText, boolean positionedTextShadow, int[] positionedTextData) {
        this(positionedText, positionedTextShadow, positionedTextData, 1);
    }

    public PositionedText(Text positionedText, boolean positionedTextShadow, int[] positionedTextData, int fontScale) {
        this.positionedText = positionedText;
        this.positionedTextShadow = positionedTextShadow;
        this.positionedTextData = positionedTextData;
        this.fontScale = fontScale;
    }

    public void render(MatrixStack matrix, float tickDelta) {
        if (positionedText == null) return;
        if (client.options.hudHidden) return;
        client.getProfiler().push("titleAndSubtitle");
        if (remainingTime > 0) {
            float time = (float)this.remainingTime - tickDelta;
            int fade = 255;
            if (remainingTime > fadeOutTicks + displayTicks) {
                float o = (float)(fadeInTicks + displayTicks + fadeOutTicks) - time;
                fade = (int)(o * 255.0f / (float)fadeInTicks);
            }
            if (remainingTime <= fadeOutTicks) {
                fade = (int)(time * 255.0f / (float)fadeOutTicks);
            }
            if ((fade = MathHelper.clamp(fade, 0, 255)) > 8) {
                int fadeAlpha = fade << 24 & 0xFF000000;
                int alpha = positionedTextData[2] >> 24 & 0xFF;
                int red = positionedTextData[2] >> 16 & 0xFF;
                int green = positionedTextData[2] >> 8 & 0xFF;
                int blue = positionedTextData[2] & 0xFF;
                int rgb = (alpha << 24) + (red << 16) + (green << 8) + (blue);
                matrix.push();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                matrix.translate(positionedTextData[0], positionedTextData[1], 0.0);
                matrix.push();
                matrix.scale(fontScale, fontScale, fontScale);
                if (positionedTextShadow) {
                    client.textRenderer.drawWithShadow(matrix, positionedText, 0, 0, rgb | fadeAlpha);
                } else {
                    client.textRenderer.draw(matrix, positionedText, 0, 0, rgb | fadeAlpha);
                }
                matrix.pop();
                RenderSystem.disableBlend();
                matrix.pop();
            }
        }
        MinecraftClient.getInstance().getProfiler().pop();
    }

    public void tick() {
        if (this.remainingTime > 0) {
            --this.remainingTime;
            if (this.remainingTime <= 0) {
                this.positionedText = null;
            }
        }
    }
}
