package com.awakenedredstone.subathon.config.cloth.options;

import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class ButtonFieldBuilder extends FieldBuilder<String, ButtonListEntry> {
    private ButtonWidget.PressAction pressAction = (button) -> {};
    private Function<String, Optional<Text[]>> tooltipSupplier = (str) -> {
        return Optional.empty();
    };

    public ButtonFieldBuilder(Text fieldNameKey) {
        super(new TranslatableText("text.cloth-config.reset_value"), fieldNameKey);
    }

    public ButtonFieldBuilder setErrorSupplier(Function<String, Optional<Text>> errorSupplier) {
        this.errorSupplier = errorSupplier;
        return this;
    }

    public ButtonFieldBuilder requireRestart() {
        this.requireRestart(true);
        return this;
    }

    public ButtonFieldBuilder setPressAction(ButtonWidget.PressAction pressAction) {
        this.pressAction = pressAction;
        return this;
    }

    public ButtonFieldBuilder setDefaultValue(Supplier<String> defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public ButtonFieldBuilder setDefaultValue(String defaultValue) {
        this.defaultValue = () -> {
            return (String)Objects.requireNonNull(defaultValue);
        };
        return this;
    }

    public ButtonFieldBuilder setTooltipSupplier(Supplier<Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = (str) -> {
            return (Optional<Text[]>)tooltipSupplier.get();
        };
        return this;
    }

    public ButtonFieldBuilder setTooltipSupplier(Function<String, Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public ButtonFieldBuilder setTooltip(Optional<Text[]> tooltip) {
        this.tooltipSupplier = (str) -> {
            return tooltip;
        };
        return this;
    }

    public ButtonFieldBuilder setTooltip(Text... tooltip) {
        this.tooltipSupplier = (str) -> {
            return Optional.ofNullable(tooltip);
        };
        return this;
    }

    @NotNull
    public ButtonListEntry build() {
        ButtonListEntry entry = new ButtonListEntry(this.getFieldNameKey(), null, this.isRequireRestart(), pressAction);
        entry.setTooltipSupplier(() -> {
            return this.tooltipSupplier.apply(entry.getValue());
        });
        if (this.errorSupplier != null) {
            entry.setErrorSupplier(() -> {
                return this.errorSupplier.apply(entry.getValue());
            });
        }

        return entry;
    }
}
