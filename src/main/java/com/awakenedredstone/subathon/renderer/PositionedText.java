package com.awakenedredstone.subathon.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class PositionedText {
    private final Text positionedText;
    private final boolean positionedTextShadow;
    private final int[] positionedTextData;
    private int positionedTextTime;

    public PositionedText(Text positionedText, boolean positionedTextShadow, int[] positionedTextData) {
        this.positionedText = positionedText;
        this.positionedTextShadow = positionedTextShadow;
        this.positionedTextData = positionedTextData;
    }

    public void render(MatrixStack matrix) {
        if (positionedText == null) return;
        if (positionedTextTime < 80) {
            if (positionedTextShadow) {
                MinecraftClient.getInstance().textRenderer.drawWithShadow(matrix, positionedText, positionedTextData[0], positionedTextData[1], positionedTextData[2]);
            } else {
                MinecraftClient.getInstance().textRenderer.draw(matrix, positionedText, positionedTextData[0], positionedTextData[1], positionedTextData[2]);
            }
            if (!MinecraftClient.getInstance().isPaused()) positionedTextTime++;
        } else if (positionedTextTime < 120) {
            int alpha = positionedTextData[2] >> 24 & 0xFF;
            int red = positionedTextData[2] >> 16 & 0xFF;
            int green = positionedTextData[2] >> 8 & 0xFF;
            int blue = positionedTextData[2] & 0xFF;
            int rgb = ((alpha - ((255 / 40) * (positionedTextTime - 79))) << 24) + (red << 16) + (green << 8) + (blue);

            if (positionedTextShadow) {
                MinecraftClient.getInstance().textRenderer.drawWithShadow(matrix, positionedText, positionedTextData[0], positionedTextData[1], rgb);
            } else {
                MinecraftClient.getInstance().textRenderer.draw(matrix, positionedText, positionedTextData[0], positionedTextData[1], rgb);
            }
            if (!MinecraftClient.getInstance().isPaused()) positionedTextTime++;
        }
    }
}
