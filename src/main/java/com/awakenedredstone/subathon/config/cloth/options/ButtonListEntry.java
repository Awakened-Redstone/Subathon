package com.awakenedredstone.subathon.config.cloth.options;

import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ButtonListEntry extends TooltipListEntry<String> {
    protected ButtonWidget buttonWidget;
    protected List<ClickableWidget> widgets;

    @Deprecated
    public ButtonListEntry(Text fieldName, @Nullable Supplier<Optional<Text[]>> tooltipSupplier, ButtonWidget.PressAction pressAction) {
        this(fieldName, tooltipSupplier, false, pressAction);
    }

    @Deprecated
    public ButtonListEntry(Text fieldName, @Nullable Supplier<Optional<Text[]>> tooltipSupplier, boolean requiresRestart, ButtonWidget.PressAction pressAction) {
        super(fieldName, tooltipSupplier, requiresRestart);
        this.buttonWidget = new ButtonWidget(0, 0, 150, 20, new TranslatableText("text.subathon.auth"), pressAction);
        this.widgets = Lists.newArrayList(new ClickableWidget[]{this.buttonWidget});
    }

    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Window window = MinecraftClient.getInstance().getWindow();
        this.buttonWidget.y = y;
        Text displayedFieldName = this.getDisplayedFieldName();
        if (MinecraftClient.getInstance().textRenderer.isRightToLeft()) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, displayedFieldName.asOrderedText(), (float)(window.getScaledWidth() - x - MinecraftClient.getInstance().textRenderer.getWidth(displayedFieldName)), (float)(y + 6), this.getPreferredTextColor());
            this.buttonWidget.x = x;
        } else {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, displayedFieldName.asOrderedText(), (float)x, (float)(y + 6), this.getPreferredTextColor());
            this.buttonWidget.x = x + entryWidth - 150;
        }

        this.buttonWidget.render(matrices, mouseX, mouseY, delta);
    }

    public Optional<String> getDefaultValue() {
        return Optional.of("");
    }

    public boolean isEdited() {
        return false;
    }

    public List<? extends Element> children() {
        return this.widgets;
    }

    public String getValue() {
        return "There is literally nothing here.";
    }

    public void save() {}

    public List<? extends Selectable> narratables() {
        return this.widgets;
    }
}
