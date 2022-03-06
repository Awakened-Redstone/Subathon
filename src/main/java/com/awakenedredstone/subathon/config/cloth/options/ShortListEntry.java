package com.awakenedredstone.subathon.config.cloth.options;

import me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class ShortListEntry extends TextFieldListEntry<Short> {
    private static final Function<String, String> stripCharacters = (s) -> {
        StringBuilder builder = new StringBuilder();
        char[] var2 = s.toCharArray();
        int var3 = var2.length;
        char[] var4 = var2;
        int var5 = var2.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            char c = var4[var6];
            if (Character.isDigit(c) || c == '-') {
                builder.append(c);
            }
        }

        return builder.toString();
    };
    private int minimum;
    private int maximum;
    private final Consumer<Short> saveConsumer;

    /** @deprecated */
    @Deprecated
    @ApiStatus.Internal
    public ShortListEntry(Text fieldName, Short value, Text resetButtonKey, Supplier<Short> defaultValue, Consumer<Short> saveConsumer) {
        super(fieldName, value, resetButtonKey, defaultValue);
        this.minimum = -2147483647;
        this.maximum = 2147483647;
        this.saveConsumer = saveConsumer;
    }

    /** @deprecated */
    @Deprecated
    @ApiStatus.Internal
    public ShortListEntry(Text fieldName, Short value, Text resetButtonKey, Supplier<Short> defaultValue, Consumer<Short> saveConsumer, Supplier<Optional<Text[]>> tooltipSupplier) {
        this(fieldName, value, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, false);
    }

    /** @deprecated */
    @Deprecated
    @ApiStatus.Internal
    public ShortListEntry(Text fieldName, Short value, Text resetButtonKey, Supplier<Short> defaultValue, Consumer<Short> saveConsumer, Supplier<Optional<Text[]>> tooltipSupplier, boolean requiresRestart) {
        super(fieldName, value, resetButtonKey, defaultValue, tooltipSupplier, requiresRestart);
        this.minimum = -2147483647;
        this.maximum = 2147483647;
        this.saveConsumer = saveConsumer;
    }

    protected String stripAddText(String s) {
        return (String)stripCharacters.apply(s);
    }

    protected void textFieldPreRender(TextFieldWidget widget) {
        try {
            double i = (double)Integer.parseInt(this.textFieldWidget.getText());
            if (!(i < (double)this.minimum) && !(i > (double)this.maximum)) {
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

    public ShortListEntry setMaximum(int maximum) {
        this.maximum = maximum;
        return this;
    }

    public ShortListEntry setMinimum(int minimum) {
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

    public Optional<Text> getError() {
        try {
            int i = Integer.parseInt(this.textFieldWidget.getText());
            if (i > this.maximum) {
                return Optional.of(new TranslatableText("text.cloth-config.error.too_large", this.maximum));
            }

            if (i < this.minimum) {
                return Optional.of(new TranslatableText("text.cloth-config.error.too_small", this.minimum));
            }
        } catch (NumberFormatException var2) {
            return Optional.of(new TranslatableText("text.cloth-config.error.not_valid_number_int"));
        }

        return super.getError();
    }
}

