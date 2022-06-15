package com.awakenedredstone.subathon.client.toast;

import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.client.texture.TwitchSpriteManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

@Environment(value = EnvType.CLIENT)
public record TwitchEventToast(Identifier spriteId, Text title, Text message) implements Toast {

    @Override
    public Visibility draw(MatrixStack matrix, ToastManager manager, long startTime) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        manager.drawTexture(matrix, 0, 0, 0, 0, this.getWidth(), this.getHeight());
        int i;
        List<OrderedText> list = manager.getClient().textRenderer.wrapLines(message, 125);
        if (list.size() == 1) {
            manager.getClient().textRenderer.draw(matrix, title, 30.0f, 7.0f, 0xFF88FF | 0xFF000000);
            manager.getClient().textRenderer.draw(matrix, list.get(0), 30.0f, 18.0f, -1);
        } else if (list.size() < 1) {
            manager.getClient().textRenderer.draw(matrix, title, 30.0f, 11.0f, 0xFF88FF | 0xFF000000);
        } else {
            int j = 1500;
            float f = 300.0f;
            if (startTime < 1500L) {
                int k = MathHelper.floor(MathHelper.clamp((float) (1500L - startTime) / 300.0f, 0.0f, 1.0f) * 255.0f) << 24 | 0x4000000;
                manager.getClient().textRenderer.draw(matrix, title, 30.0f, 11.0f, 0xFF88FF | k);
            } else {
                int k = MathHelper.floor(MathHelper.clamp((float) (startTime - 1500L) / 300.0f, 0.0f, 1.0f) * 252.0f) << 24 | 0x4000000;
                int l = this.getHeight() / 2 - list.size() * manager.getClient().textRenderer.fontHeight / 2;
                int line = 0;
                for (OrderedText orderedText : list) {
                    if (++line > 2) break;

                    manager.getClient().textRenderer.draw(matrix, orderedText, 30.0f, (float) l, 0xFFFFFF | k);
                    l += manager.getClient().textRenderer.fontHeight;
                }
            }
        }

        final TwitchSpriteManager spriteManager = SubathonClient.twitchSpriteManager;
        Sprite sprite = spriteManager.getSprite(spriteId);
        RenderSystem.setShaderTexture(0, sprite.getAtlas().getId());
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        DrawableHelper.drawSprite(matrix, 8, 8, 0, 18, 18, sprite);
        return startTime >= 5000L ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
    }
}
