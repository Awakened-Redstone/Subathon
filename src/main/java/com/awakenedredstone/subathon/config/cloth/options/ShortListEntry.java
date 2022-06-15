package com.awakenedredstone.subathon.config.cloth.options;

import me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class ShortListEntry extends TextFieldListEntry<Short> {
    private static final Function<String, String> stripCharacters = (s) -> {
        StringBuilder builder = new StringBuilder();

        for (char c : s.toCharArray()) {
            if (Character.isDigit(c) || c == '-') {
                builder.append(c);
            }
        }

        return builder.toString();
    };
    private short minimum;
    private short maximum;
    private final Consumer<Short> saveConsumer;
    private final RenderAction<ShortListEntry> render;

    /** @deprecated */
    @Deprecated
    @ApiStatus.Internal
    public ShortListEntry(Text fieldName, Short value, Text resetButtonKey, Supplier<Short> defaultValue, Consumer<Short> saveConsumer, RenderAction<ShortListEntry> render) {
        super(fieldName, value, resetButtonKey, defaultValue);
        this.render = render;
        this.minimum = Short.MIN_VALUE;
        this.maximum = Short.MAX_VALUE;
        this.saveConsumer = saveConsumer;
    }

    /** @deprecated */
    @Deprecated
    @ApiStatus.Internal
    public ShortListEntry(Text fieldName, Short value, Text resetButtonKey, Supplier<Short> defaultValue, Consumer<Short> saveConsumer, Supplier<Optional<Text[]>> tooltipSupplier, RenderAction<ShortListEntry> render) {
        this(fieldName, value, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, false, render);
    }

    /** @deprecated */
    @Deprecated
    @ApiStatus.Internal
    public ShortListEntry(Text fieldName, Short value, Text resetButtonKey, Supplier<Short> defaultValue, Consumer<Short> saveConsumer, Supplier<Optional<Text[]>> tooltipSupplier, boolean requiresRestart, RenderAction<ShortListEntry> render) {
        super(fieldName, value, resetButtonKey, defaultValue, tooltipSupplier, requiresRestart);
        this.render = render;
        this.minimum = Short.MIN_VALUE;
        this.maximum = Short.MAX_VALUE;
        this.saveConsumer = saveConsumer;
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        try {
            this.render.onRender(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta, this);
        } catch (ClassCastException ignored) {}
    }

    protected String stripAddText(String s) {
        return stripCharacters.apply(s);
    }

    protected void textFieldPreRender(TextFieldWidget widget) {
        try {
            short i = Short.parseShort(this.textFieldWidget.getText());
            if (!(i < this.minimum) && !(i > this.maximum)) {
                widget.setEditableColor(14737632);
            } else {
                widget.setEditableColor(16733525);
            }
        } catch (NumberFormatException var4) {
            widget.setEditableColor(16733525);
        }

    }

    protected boolean isMatchDefault(String text) {
        return this.getDefaultValue().isPresent() && text.equals(this.defaultValue.get().toString());
    }

    public void save() {
        if (this.saveConsumer != null) {
            this.saveConsumer.accept(this.getValue());
        }

    }

    public ShortListEntry setMaximum(short maximum) {
        this.maximum = maximum;
        return this;
    }

    public ShortListEntry setMinimum(short minimum) {
        this.minimum = minimum;
        return this;
    }

    public Short getValue() {
        try {
            return Short.valueOf(this.textFieldWidget.getText());
        } catch (Exception var2) {
            return 0;
        }
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(this.textFieldWidget.getText());
    }

    public Optional<Text> getError() {
        try {
            short i = Short.parseShort(this.textFieldWidget.getText());
            if (i > this.maximum) {
                return Optional.of(Text.translatable("text.cloth-config.error.too_large", this.maximum));
            }

            if (i < this.minimum) {
                return Optional.of(Text.translatable("text.cloth-config.error.too_small", this.minimum));
            }
        } catch (NumberFormatException var2) {
            return Optional.of(Text.translatable("text.subathon.config.error.not_valid_number_short"));
        }

        return super.getError();
    }
}

