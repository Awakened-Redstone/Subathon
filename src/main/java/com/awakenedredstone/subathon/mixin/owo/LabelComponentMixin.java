package com.awakenedredstone.subathon.mixin.owo;

import com.awakenedredstone.subathon.duck.LabelComponentDuck;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.VerticalFlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.util.pond.OwoTextRendererExtension;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LabelComponent.class)
@Environment(EnvType.CLIENT)
public abstract class LabelComponentMixin extends BaseComponent implements LabelComponentDuck {
    @Shadow protected VerticalAlignment verticalTextAlignment;
    @Shadow protected List<OrderedText> wrappedText;
    @Shadow @Final protected TextRenderer textRenderer;
    @Shadow protected HorizontalAlignment horizontalTextAlignment;

    @Shadow protected abstract int determineHorizontalContentSize(Sizing sizing);

    @Shadow protected boolean shadow;
    @Shadow @Final protected AnimatableProperty<Color> color;
    private boolean isScaled = false;
    private float scale = 1;

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void subathon$addScaling(MatrixStack matrices, int mouseX, int mouseY, float partialTicks, float delta, CallbackInfo ci) {
        if (isScaled) {
            int parentHorizontalSizing = ((FlowLayoutAccessor) parent).callDetermineHorizontalContentSize(Sizing.content());
            int parentVerticalSizing = ((FlowLayoutAccessor) parent).callDetermineVerticalContentSize(Sizing.content());
            int horizontalSizing = determineHorizontalContentSize(Sizing.content());
            int verticalSizing = determineVerticalContentSize(Sizing.content());
            int newWidth = (int) (horizontalSizing * scale) + (parentHorizontalSizing - horizontalSizing);
            parent.sizing(Sizing.fixed(newWidth),
                    Sizing.fixed((int) (verticalSizing * scale) + (parentVerticalSizing - verticalSizing)));

            int originalWidth = horizontalSizing + (parentHorizontalSizing - horizontalSizing);

            try {
                int x = this.x + (originalWidth - newWidth);
                int y = this.y;

                if (this.horizontalSizing.get().isContent()) {
                    x += this.horizontalSizing.get().value;
                }
                if (this.verticalSizing.get().isContent()) {
                    y += this.verticalSizing.get().value;
                }

                switch (this.verticalTextAlignment) {
                    case CENTER -> y += (this.height - ((this.wrappedText.size() * (this.textRenderer.fontHeight + 2)) - 2)) / 2;
                    case BOTTOM -> y += this.height - ((this.wrappedText.size() * (this.textRenderer.fontHeight + 2)) - 2);
                }

                ((OwoTextRendererExtension) this.textRenderer).owo$beginCache();

                for (int i = 0; i < this.wrappedText.size(); i++) {
                    var renderText = this.wrappedText.get(i);
                    int renderX = x;

                    switch (this.horizontalTextAlignment) {
                        case CENTER -> renderX += (this.width - this.textRenderer.getWidth(renderText)) / 2;
                        case RIGHT -> renderX += this.width - this.textRenderer.getWidth(renderText);
                    }

                    matrices.push();
                    matrices.translate(renderX, (y + i * 11), 0);
                    matrices.scale(scale, scale, scale);
                    if (this.shadow) {
                        this.textRenderer.drawWithShadow(matrices, renderText, 0, 0, this.color.get().argb());
                    } else {
                        this.textRenderer.draw(matrices, renderText, 0, 0, this.color.get().argb());
                    }
                    matrices.pop();
                }
            } finally {
                ((OwoTextRendererExtension) this.textRenderer).owo$submitCache();
            }
            ci.cancel();
        }
    }

    /*@Inject(method = "draw", at = @At("RETURN"))
    private void subathon$popMatrice(MatrixStack matrices, int mouseX, int mouseY, float partialTicks, float delta, CallbackInfo ci) {
        if (isScaled) matrices.pop();
    }*/

    @Override
    public void subathon$setScale(float scale) {
        this.scale = scale;
        isScaled = scale != 1;
    }
}
