package com.awakenedredstone.subathon.client.toast;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

@Environment(EnvType.CLIENT)
public record TwitchEventToast(Identifier spriteId, Text title, Text message) implements Toast {

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        TextRenderer textRenderer = manager.getClient().textRenderer;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        context.drawTexture(TEXTURE, 0, 0, 0, 0, this.getWidth(), this.getHeight());
        List<OrderedText> titleList = textRenderer.wrapLines(title, 125);
        List<OrderedText> list = textRenderer.wrapLines(message, 125);
        if (list.size() == 1) {
            context.drawText(textRenderer, title, 30, 7, 0xFF88FF | 0xFF000000, false);
            context.drawText(textRenderer, list.get(0), 30, 18, -1, false);
        } else if (list.isEmpty()) {
            context.drawText(textRenderer, title, 30, 11, 0xFF88FF | 0xFF000000, false);
        } else {
            int j = 1500;
            float f = 300.0f;
            if (startTime < 1500L) {
                int k = MathHelper.floor(MathHelper.clamp((float) (1500L - startTime) / 300.0f, 0.0f, 1.0f) * 255.0f) << 24 | 0x4000000;
                if (titleList.size() <= 1) {
                    context.drawText(textRenderer, title, 30, 11, 0xFF88FF | k, false);
                } else {
                    int l = this.getHeight() / 2 - titleList.size() * textRenderer.fontHeight / 2;
                    int line = 0;
                    for (OrderedText orderedText : titleList) {
                        if (++line > 2) break;

                        context.drawText(textRenderer, orderedText, 30, l, 0xFFFFFF | k, false);
                        l += textRenderer.fontHeight;
                    }
                }
            } else {
                int k = MathHelper.floor(MathHelper.clamp((float) (startTime - 1500L) / 300.0f, 0.0f, 1.0f) * 252.0f) << 24 | 0x4000000;
                int l = this.getHeight() / 2 - list.size() * textRenderer.fontHeight / 2;
                int line = 0;
                for (OrderedText orderedText : list) {
                    if (++line > 2) break;

                    context.drawText(textRenderer, orderedText, 30, l, 0xFFFFFF | k, false);
                    l += textRenderer.fontHeight;
                }
            }
        }

        try {
            //final TwitchSpriteManager spriteManager = SubathonClient.twitchSpriteManager;
            //Sprite sprite = spriteManager.getSprite(spriteId);
            Sprite sprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, spriteId).getSprite();
            //RenderSystem.setShaderTexture(0, spriteId);
            RenderSystem.setShaderTexture(0, sprite.getAtlasId());

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.drawSprite(8, 8, 0, 18, 18, sprite);
            //DrawableHelper.drawTexture(matrix, 8, 8, 18, 18, 0, 0, 18, 18, 18, 18);
        } catch (Exception e) {/**/}
        return startTime >= 5000L ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
}