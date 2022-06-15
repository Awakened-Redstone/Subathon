package com.awakenedredstone.subathon.config.cloth.options;

import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class ByteFieldBuilder extends FieldBuilder<Byte, ByteListEntry> {
    private Consumer<Byte> saveConsumer = null;
    private RenderAction render = (q, w, e, r, t, y, u, i, o, p, a) -> {};
    private Function<Byte, Optional<Text[]>> tooltipSupplier = (i) -> {
        return Optional.empty();
    };
    private final byte value;
    private Byte min = null;
    private Byte max = null;

    public ByteFieldBuilder(Text fieldNameKey, byte value) {
        super(Text.translatable("text.cloth-config.reset_value"), fieldNameKey);
        this.value = value;
    }

    public ByteFieldBuilder requireRestart() {
        this.requireRestart(true);
        return this;
    }

    public ByteFieldBuilder setErrorSupplier(Function<Byte, Optional<Text>> errorSupplier) {
        this.errorSupplier = errorSupplier;
        return this;
    }

    public ByteFieldBuilder setSaveConsumer(Consumer<Byte> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return this;
    }

    public ByteFieldBuilder setDefaultValue(Supplier<Byte> defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public ByteFieldBuilder setDefaultValue(byte defaultValue) {
        this.defaultValue = () -> {
            return defaultValue;
        };
        return this;
    }

    public ByteFieldBuilder setTooltipSupplier(Function<Byte, Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public ByteFieldBuilder setTooltipSupplier(Supplier<Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = (i) -> {
            return tooltipSupplier.get();
        };
        return this;
    }

    public ByteFieldBuilder setTooltip(Optional<Text[]> tooltip) {
        this.tooltipSupplier = (i) -> {
            return tooltip;
        };
        return this;
    }

    public ByteFieldBuilder setTooltip(Text... tooltip) {
        this.tooltipSupplier = (i) -> {
            return Optional.ofNullable(tooltip);
        };
        return this;
    }

    public ByteFieldBuilder setMin(byte min) {
        this.min = min;
        return this;
    }

    public ByteFieldBuilder setMax(byte max) {
        this.max = max;
        return this;
    }

    public ByteFieldBuilder removeMin() {
        this.min = null;
        return this;
    }

    public ByteFieldBuilder removeMax() {
        this.max = null;
        return this;
    }

    public ByteFieldBuilder setRender(RenderAction render) {
        this.render = render;
        return this;
    }

    public ByteFieldBuilder removeRender() {
        this.render = (q, w, e, r, t, y, u, i, o, p, a) -> {};
        return this;
    }

    @NotNull
    public ByteListEntry build() {
        ByteListEntry entry = new ByteListEntry(this.getFieldNameKey(), this.value, this.getResetButtonKey(), this.defaultValue, this.saveConsumer, null, this.isRequireRestart(), this.render);
        if (this.min != null) {
            entry.setMinimum(this.min);
        }

        if (this.max != null) {
            entry.setMaximum(this.max);
        }

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