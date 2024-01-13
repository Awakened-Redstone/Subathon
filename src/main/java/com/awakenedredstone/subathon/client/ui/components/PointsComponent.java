package com.awakenedredstone.subathon.client.ui.components;

import com.awakenedredstone.subathon.Subathon;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.AnimatableProperty;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.ui.parsing.UIParsing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.w3c.dom.Element;

import java.util.Map;

public class PointsComponent extends BaseComponent {
    protected final MinecraftClient client = MinecraftClient.getInstance();
    protected final TextRenderer textRenderer = client.textRenderer;
    protected float scale = 1;
    private long lastPoints = 0;

    private long getPoints() {
        return Subathon.getPoints(client.player);
    }

    private Text getPointsText() {
        return Text.translatable("text.subathon.points", getPoints());
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return (int) ((this.textRenderer.getWidth(getPointsText()) + 8) * scale);
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return (int) ((this.textRenderer.fontHeight + 8) * scale);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        MatrixStack matrices = context.getMatrices();
        if (getPoints() != lastPoints) {
            this.notifyParentIfMounted();
            lastPoints = getPoints();
        }

        matrices.push();

        int x = this.x;
        int y = this.y;

        if (this.horizontalSizing.get().isContent()) {
            x += this.horizontalSizing.get().value;
        }
        if (this.verticalSizing.get().isContent()) {
            y += this.verticalSizing.get().value;
        }


        context.drawGradientRect(
            x, y, width(), height(),
            0xC0101010, 0xC0101010, 0xD0101010, 0xD0101010
        );

        matrices.translate(0.5, 0.5, 0);
        matrices.scale(this.scale, this.scale, 1);

        x += (int) (3.5 * scale);
        y += (int) (4.5 * scale);

        context.drawTextWithShadow(client.textRenderer, getPointsText(), (int) (x / scale), (int) (y / scale), AnimatableProperty.of(Color.WHITE).get().argb());

        matrices.translate(-0.5, -0.5, 0);

        matrices.pop();
    }

    public PointsComponent scale(float scale) {
        this.scale = scale;
        this.notifyParentIfMounted();
        return this;
    }

    @Override
    public void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        super.parseProperties(model, element, children);

        UIParsing.apply(children, "scale", UIParsing::parseFloat, this::scale);
    }

    public static PointsComponent parse(Element element) {
        return new PointsComponent();
    }
}
