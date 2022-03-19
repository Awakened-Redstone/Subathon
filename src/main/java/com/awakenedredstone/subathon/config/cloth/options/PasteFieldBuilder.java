package com.awakenedredstone.subathon.config.cloth.options;

import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class PasteFieldBuilder extends FieldBuilder<String, PasteListEntry> {
    private Consumer<String> saveConsumer = null;
    private String value = "";
    private String type;
    private Function<String, Optional<Text[]>> tooltipSupplier = (str) -> {
        return Optional.empty();
    };

    public PasteFieldBuilder(Text fieldNameKey, String value) {
        super(new TranslatableText("text.cloth-config.reset_value"), fieldNameKey);
        this.value = value;
    }

    public PasteFieldBuilder setErrorSupplier(Function<String, Optional<Text>> errorSupplier) {
        this.errorSupplier = errorSupplier;
        return this;
    }

    public PasteFieldBuilder requireRestart() {
        this.requireRestart(true);
        return this;
    }

    public PasteFieldBuilder setSaveConsumer(Consumer<String> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return this;
    }

    public PasteFieldBuilder setDefaultValue(Supplier<String> defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public PasteFieldBuilder setDefaultValue(String defaultValue) {
        this.defaultValue = () -> {
            return (String)Objects.requireNonNull(defaultValue);
        };
        return this;
    }

    public PasteFieldBuilder setTooltipSupplier(Supplier<Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = (str) -> {
            return (Optional<Text[]>)tooltipSupplier.get();
        };
        return this;
    }

    public PasteFieldBuilder setTooltipSupplier(Function<String, Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public PasteFieldBuilder setTooltip(Optional<Text[]> tooltip) {
        this.tooltipSupplier = (str) -> {
            return tooltip;
        };
        return this;
    }

    public PasteFieldBuilder setTooltip(Text... tooltip) {
        this.tooltipSupplier = (str) -> {
            return Optional.ofNullable(tooltip);
        };
        return this;
    }

    public PasteFieldBuilder setType(String type) {
        this.type= type;
        return this;
    }

    @NotNull
    public PasteListEntry build() {
        PasteListEntry entry = new PasteListEntry(this.getFieldNameKey(), null, this.isRequireRestart(), this.saveConsumer, value, defaultValue);
        entry.setTooltipSupplier(() -> {
            return this.tooltipSupplier.apply(entry.getValue());
        });
        if (type != null) {
            entry.setType(type);
        }
        if (this.errorSupplier != null) {
            entry.setErrorSupplier(() -> {
                return this.errorSupplier.apply(entry.getValue());
            });
        }

        return entry;
    }
}
