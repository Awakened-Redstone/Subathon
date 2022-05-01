package com.awakenedredstone.subathon.client.screen;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.client.TwitchEvent;
import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.List;

@Environment(value = EnvType.CLIENT)
public class EventMessageScreen extends Screen {
    private final Screen parent;
    private final TwitchEvent event;
    private final boolean filterMessages;

    public EventMessageScreen(Screen parent, TwitchEvent event, boolean filterMessages) {
        super(new TranslatableText("gui.subathon.event_logs"));
        this.parent = parent;
        this.event = event;
        this.filterMessages = filterMessages;
    }

    @Override
    protected void init() {
        this.addDrawable(new EventMessageWidget());
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 75, this.height - 28, 150, 20, ScreenTexts.DONE, button -> this.client.setScreen(this.parent)));
        int w = 14, h = 22;
        List<TwitchEvent> events = SubathonClient.events.stream().filter(event -> !filterMessages || (!event.message().isEmpty() && event.event() != SubathonCommand.Events.GIFT_USER)).toList();
        int index = events.indexOf(event);
        if (index > 0)
            this.addDrawableChild(new TexturedButtonWidget(4, (this.height - 68) / 2 + h / 2, w, h, w, 0, h,
                    new Identifier(Subathon.MOD_ID, "textures/gui/widgets.png"), 64, 64,
                    button -> this.client.setScreen(new EventMessageScreen(parent, events.get(index - 1), filterMessages))));
        if (index < events.size() - 1)
            this.addDrawableChild(new TexturedButtonWidget(this.width - w - 4, (this.height - 68) / 2 + h / 2, w, h, 0, 0, h,
                    new Identifier(Subathon.MOD_ID, "textures/gui/widgets.png"), 64, 64,
                    button -> this.client.setScreen(new EventMessageScreen(parent, events.get(index + 1), filterMessages))));
        super.init();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Environment(value = EnvType.CLIENT)
    class EventMessageWidget extends DrawableHelper implements Drawable {
        protected final int width = EventMessageScreen.this.width;
        protected final int height = EventMessageScreen.this.height;
        protected final int top = 32;
        protected final int bottom = EventMessageScreen.this.height - 36;
        protected final int right = EventMessageScreen.this.width;
        protected final int left = 0;

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            renderBackground(tessellator, bufferBuilder);
            renderHorizontalShadow(tessellator, bufferBuilder);

            DrawableHelper.drawCenteredText(matrices, EventMessageScreen.this.textRenderer, event.getMessage(), width / 2, top + 8, 0xFFFFFF);
            byte[] lineNumber = new byte[]{0};
            if (event.event() != SubathonCommand.Events.GIFT_USER)
                EventMessageScreen.this.textRenderer.getTextHandler().wrapLines(event.message(), width - 64, Style.EMPTY).forEach(line -> {
                    if (lineNumber[0] * (textRenderer.fontHeight + 4) > bottom - top - 44 - textRenderer.fontHeight * 2) {
                        DrawableHelper.drawCenteredText(matrices, EventMessageScreen.this.textRenderer,
                                new TranslatableText("text.subathon.error.message_too_long"), width / 2,
                                top + 32 + (lineNumber[0] * (textRenderer.fontHeight + 4)) + 3, 0xFFFFFF);
                        return;
                    }
                    ;
                    DrawableHelper.drawTextWithShadow(matrices, EventMessageScreen.this.textRenderer, new LiteralText(line.getString()),
                            32, top + 32 + (lineNumber[0]++ * (textRenderer.fontHeight + 4)), 0xFFFFFF);
                });

            RenderSystem.enableTexture();
            RenderSystem.disableBlend();
        }

        public void renderBackground(Tessellator tessellator, BufferBuilder bufferBuilder) {
            RenderSystem.setShaderTexture(0, DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            float f = 32.0f;
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bufferBuilder.vertex(this.left, this.bottom, 0.0).texture(this.left / 32.0f, this.bottom / 32.0f).color(32, 32, 32, 255).next();
            bufferBuilder.vertex(this.right, this.bottom, 0.0).texture(this.right / 32.0f, this.bottom / 32.0f).color(32, 32, 32, 255).next();
            bufferBuilder.vertex(this.right, this.top, 0.0).texture(this.right / 32.0f, this.top / 32.0f).color(32, 32, 32, 255).next();
            bufferBuilder.vertex(this.left, this.top, 0.0).texture(this.left / 32.0f, this.top / 32.0f).color(32, 32, 32, 255).next();
            tessellator.draw();
        }

        public void renderHorizontalShadow(Tessellator tessellator, BufferBuilder bufferBuilder) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(519);
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bufferBuilder.vertex(this.left, this.top, -100.0).texture(0.0f, (float) this.top / 32.0f).color(64, 64, 64, 255).next();
            bufferBuilder.vertex(this.left + this.width, this.top, -100.0).texture((float) this.width / 32.0f, (float) this.top / 32.0f).color(64, 64, 64, 255).next();
            bufferBuilder.vertex(this.left + this.width, 0.0, -100.0).texture((float) this.width / 32.0f, 0.0f).color(64, 64, 64, 255).next();
            bufferBuilder.vertex(this.left, 0.0, -100.0).texture(0.0f, 0.0f).color(64, 64, 64, 255).next();
            bufferBuilder.vertex(this.left, this.height, -100.0).texture(0.0f, (float) this.height / 32.0f).color(64, 64, 64, 255).next();
            bufferBuilder.vertex(this.left + this.width, this.height, -100.0).texture((float) this.width / 32.0f, (float) this.height / 32.0f).color(64, 64, 64, 255).next();
            bufferBuilder.vertex(this.left + this.width, this.bottom, -100.0).texture((float) this.width / 32.0f, (float) this.bottom / 32.0f).color(64, 64, 64, 255).next();
            bufferBuilder.vertex(this.left, this.bottom, -100.0).texture(0.0f, (float) this.bottom / 32.0f).color(64, 64, 64, 255).next();
            tessellator.draw();
            RenderSystem.depthFunc(515);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
            RenderSystem.disableTexture();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(this.left, this.top + 4, 0.0).color(0, 0, 0, 0).next();
            bufferBuilder.vertex(this.right, this.top + 4, 0.0).color(0, 0, 0, 0).next();
            bufferBuilder.vertex(this.right, this.top, 0.0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(this.left, this.top, 0.0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(this.left, this.bottom, 0.0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(this.right, this.bottom, 0.0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(this.right, this.bottom - 4, 0.0).color(0, 0, 0, 0).next();
            bufferBuilder.vertex(this.left, this.bottom - 4, 0.0).color(0, 0, 0, 0).next();
            tessellator.draw();
        }
    }
}
