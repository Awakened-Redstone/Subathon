package com.awakenedredstone.subathon.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class PositionedText {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private Text positionedText;
    private final boolean positionedTextShadow;
    private final float x;
    private final float y;
    private final int color;
    private int stayTicks = 80;
    private int remainingTime = stayTicks;
    private int fadeOutTicks = 40;
    private int fadeInTicks = 5;
    private final float fontScale;

    public PositionedText(Text text, boolean shadow, int[] data) {
        this(text, shadow, data[0], data[1], data[2], data.length > 3 ? data[3] : 1, data.length > 4 ? data[4] : -1, data.length > 5 ? data[5] : -1, data.length > 6 ? data[6] : -1);
    }

    public PositionedText(Text text, boolean shadow, float x, float y, int color) {
        this(text, shadow, x, y, color, 1);
    }

    public PositionedText(Text text, boolean shadow, float x, float y, int color, float fontScale) {
        this(text, shadow, x, y, color, fontScale, -1, -1, -1);
    }

    public PositionedText(Text text, boolean shadow, float x, float y, int color, float fontScale, int stay, int fadeIn, int fadeOut) {
        this.positionedText = text;
        this.positionedTextShadow = shadow;
        this.x = x;
        this.y = y;
        this.color = color;
        this.fontScale = fontScale;
        if (stay > 0) this.stayTicks = stay;
        if (stay > 0) this.remainingTime = stay;
        if (fadeIn >= 0) this.fadeInTicks = fadeIn;
        if (fadeOut >= 0) this.fadeOutTicks = fadeOut;
    }

    public void render(MatrixStack matrix, float tickDelta) {
        if (positionedText == null) return;
        if (client.options.hudHidden) return;
        client.getProfiler().push("titleAndSubtitle");
        if (remainingTime > 0) {
            float time = (float)this.remainingTime - tickDelta;
            int fade = 255;
            if (remainingTime > fadeOutTicks + stayTicks) {
                float o = (float)(fadeInTicks + stayTicks + fadeOutTicks) - time;
                fade = (int)(o * 255.0f / (float)fadeInTicks);
            }
            if (remainingTime <= fadeOutTicks) {
                fade = (int)(time * 255.0f / (float)fadeOutTicks);
            }
            if ((fade = MathHelper.clamp(fade, 0, 255)) > 8) {
                int fadeAlpha = fade << 24 & 0xFF000000;
                int alpha = color >> 24 & 0xFF;
                int red = color >> 16 & 0xFF;
                int green = color >> 8 & 0xFF;
                int blue = color & 0xFF;
                int rgb = (alpha << 24) + (red << 16) + (green << 8) + (blue);
                matrix.push();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                matrix.translate(x, y, 0.0);
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
