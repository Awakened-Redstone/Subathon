package com.awakenedredstone.subathon.mixin.owo;

import com.awakenedredstone.subathon.duck.owo.LabelComponentDuck;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.util.pond.OwoTextRendererExtension;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = LabelComponent.class, remap = false)
@Environment(EnvType.CLIENT)
public abstract class LabelComponentMixin extends BaseComponent implements LabelComponentDuck {
    @Shadow(remap = false) protected VerticalAlignment verticalTextAlignment;
    @Shadow(remap = false) protected List<OrderedText> wrappedText;
    @Shadow(remap = false) @Final protected TextRenderer textRenderer;
    @Shadow(remap = false) protected HorizontalAlignment horizontalTextAlignment;

    @Shadow(remap = false) protected abstract int determineHorizontalContentSize(Sizing sizing);

    @Shadow(remap = false) protected boolean shadow;
    @Shadow(remap = false) @Final protected AnimatableProperty<Color> color;
    private boolean isScaled = false;
    private float scale = 1;

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, remap = false)
    private void subathon$addScaling(MatrixStack matrices, int mouseX, int mouseY, float partialTicks, float delta, CallbackInfo ci) {
        if (isScaled) {
            int parentHorizontalSizing = ((FlowLayoutAccessor) parent).callDetermineHorizontalContentSize(Sizing.content());
            int parentVerticalSizing = ((FlowLayoutAccessor) parent).callDetermineVerticalContentSize(Sizing.content());
            int horizontalSizing = determineHorizontalContentSize(Sizing.content());
            int verticalSizing = determineVerticalContentSize(Sizing.content());
            int newWidth = (int) (horizontalSizing * scale) + (parentHorizontalSizing - horizontalSizing);
            int newHeight = (int) (verticalSizing * scale) + (parentVerticalSizing - verticalSizing);
            parent.sizing(Sizing.fixed(newWidth), Sizing.fixed(newHeight));

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
