package com.awakenedredstone.subathon.config.cloth.options;

import com.awakenedredstone.subathon.Subathon;
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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PasteListEntry extends TooltipListEntry<String> {
    protected ButtonWidget buttonWidget;
    protected ButtonWidget resetButton;
    protected List<ClickableWidget> widgets;
    private final Consumer<String> saveConsumer;
    private String value = "";
    private final Supplier<String> defaultValue;
    private boolean modified = false;
    private String type;

    @Deprecated
    public PasteListEntry(Text fieldName, @Nullable Supplier<Optional<Text[]>> tooltipSupplier, Consumer<String> saveConsumer, String value, Supplier<String> defaultValue) {
        this(fieldName, tooltipSupplier, false, saveConsumer, value, defaultValue);
    }

    @Deprecated
    public PasteListEntry(Text fieldName, @Nullable Supplier<Optional<Text[]>> tooltipSupplier, boolean requiresRestart, Consumer<String> saveConsumer, String value, Supplier<String> defaultValue) {
        super(fieldName, tooltipSupplier, requiresRestart);
        this.saveConsumer = saveConsumer;
        this.value = value;
        this.defaultValue = defaultValue;
        this.resetButton = new ButtonWidget(0, 0, MinecraftClient.getInstance().textRenderer.getWidth(new TranslatableText("text.cloth-config.reset_value")) + 6, 20, new TranslatableText("text.cloth-config.reset_value"), (widget) -> {
            this.value = getDefaultValue().get();
            buttonWidget.setMessage(new TranslatableText("text.subathon.paste"));
            this.modified = true;
        });
        this.buttonWidget = new ButtonWidget(0, 0, 150 - this.resetButton.getWidth() - 2, 20, new TranslatableText("text.subathon.paste"), button -> {
            button.setMessage(new TranslatableText("text.subathon.pasted"));
            this.value = MinecraftClient.getInstance().keyboard.getClipboard();
            this.modified = true;
        });
        this.widgets = Lists.newArrayList(new ClickableWidget[]{this.buttonWidget, this.resetButton});
    }

    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        Window window = MinecraftClient.getInstance().getWindow();
        this.resetButton.active = this.isEditable() && !value.equals(getDefaultValue().get());
        this.resetButton.y = y;
        this.buttonWidget.y = y;
        Text displayedFieldName = this.getDisplayedFieldName();
        if (MinecraftClient.getInstance().textRenderer.isRightToLeft()) {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, displayedFieldName.asOrderedText(), (float) (window.getScaledWidth() - x - MinecraftClient.getInstance().textRenderer.getWidth(displayedFieldName)), (float) (y + 6), this.getPreferredTextColor());
            this.resetButton.x = x;
            this.buttonWidget.x = x + this.resetButton.getWidth();
        } else {
            MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, displayedFieldName.asOrderedText(), (float) x, (float) (y + 6), this.getPreferredTextColor());
            this.resetButton.x = x + entryWidth - this.resetButton.getWidth();
            this.buttonWidget.x = x + entryWidth - 150;
        }

        this.resetButton.render(matrices, mouseX, mouseY, delta);
        this.buttonWidget.render(matrices, mouseX, mouseY, delta);
    }

    public void setType(String type) {
        this.type = type;
    }

    public Optional<String> getDefaultValue() {
        return defaultValue == null ? Optional.empty() : Optional.ofNullable(defaultValue.get());
    }

    public boolean isEdited() {
        return modified;
    }

    public List<? extends Element> children() {
        return this.widgets;
    }

    public String getValue() {
        return "You are not allowed to get this information!";
    }

    public void save() {
        if (this.saveConsumer != null) {
            this.saveConsumer.accept(value);
        }
    }

    @Override
    public Optional<Text> getError() {
        if (Objects.equals(type, "JSON")) {
            try {
                Subathon.GSON.fromJson(value, Dummy.class);
            } catch (Exception var2) {
                return Optional.of(new TranslatableText("text.subathon.error.not_valid_json"));
            }
        }
        return super.getError();
    }

    public List<? extends Selectable> narratables() {
        return this.widgets;
    }
}
