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
public class ByteListEntry extends TextFieldListEntry<Byte> {
    private static final Function<String, String> stripCharacters = (s) -> {
        StringBuilder builder = new StringBuilder();

        for (char c : s.toCharArray()) {
            if (Character.isDigit(c) || c == '-') {
                builder.append(c);
            }
        }

        return builder.toString();
    };
    private byte minimum;
    private byte maximum;
    private final Consumer<Byte> saveConsumer;
    private final RenderAction<ByteListEntry> render;

    /** @deprecated */
    @Deprecated
    @ApiStatus.Internal
    public ByteListEntry(Text fieldName, Byte value, Text resetButtonKey, Supplier<Byte> defaultValue, Consumer<Byte> saveConsumer, RenderAction<ByteListEntry> render) {
        super(fieldName, value, resetButtonKey, defaultValue);
        this.render = render;
        this.minimum = Byte.MIN_VALUE;
        this.maximum = Byte.MAX_VALUE;
        this.saveConsumer = saveConsumer;
    }

    /** @deprecated */
    @Deprecated
    @ApiStatus.Internal
    public ByteListEntry(Text fieldName, Byte value, Text resetButtonKey, Supplier<Byte> defaultValue, Consumer<Byte> saveConsumer, Supplier<Optional<Text[]>> tooltipSupplier, RenderAction<ByteListEntry> render) {
        this(fieldName, value, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, false, render);
    }

    /** @deprecated */
    @Deprecated
    @ApiStatus.Internal
    public ByteListEntry(Text fieldName, Byte value, Text resetButtonKey, Supplier<Byte> defaultValue, Consumer<Byte> saveConsumer, Supplier<Optional<Text[]>> tooltipSupplier, boolean requiresRestart, RenderAction<ByteListEntry> render) {
        super(fieldName, value, resetButtonKey, defaultValue, tooltipSupplier, requiresRestart);
        this.render = render;
        this.minimum = Byte.MIN_VALUE;
        this.maximum = Byte.MAX_VALUE;
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
            byte i = Byte.parseByte(this.textFieldWidget.getText());
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

    public ByteListEntry setMaximum(byte maximum) {
        this.maximum = maximum;
        return this;
    }

    public ByteListEntry setMinimum(byte minimum) {
        this.minimum = minimum;
        return this;
    }

    public Byte getValue() {
        try {
            return Byte.valueOf(this.textFieldWidget.getText());
        } catch (Exception var2) {
            return 0;
        }
    }

    public boolean isEmpty() {
        return StringUtils.isBlank(this.textFieldWidget.getText());
    }

    public Optional<Text> getError() {
        try {
            byte i = Byte.parseByte(this.textFieldWidget.getText());
            if (i > this.maximum) {
                return Optional.of(Text.translatable("text.cloth-config.error.too_large", this.maximum));
            }

            if (i < this.minimum) {
                return Optional.of(Text.translatable("text.cloth-config.error.too_small", this.minimum));
            }
        } catch (NumberFormatException var2) {
            return Optional.of(Text.translatable("text.subathon.config.error.not_valid_number_byte"));
        }

        return super.getError();
    }
}

