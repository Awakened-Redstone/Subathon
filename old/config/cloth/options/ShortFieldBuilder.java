package old.config.cloth.options;

import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class ShortFieldBuilder extends FieldBuilder<Short, ShortListEntry> {
    private Consumer<Short> saveConsumer = null;
    private RenderAction render = (q, w, e, r, t, y, u, i, o, p, a) -> {};
    private Function<Short, Optional<Text[]>> tooltipSupplier = (i) -> {
        return Optional.empty();
    };
    private final short value;
    private Short min = null;
    private Short max = null;

    public ShortFieldBuilder(Text fieldNameKey, short value) {
        super(new TranslatableText("text.cloth-config.reset_value"), fieldNameKey);
        this.value = value;
    }

    public ShortFieldBuilder requireRestart() {
        this.requireRestart(true);
        return this;
    }

    public ShortFieldBuilder setErrorSupplier(Function<Short, Optional<Text>> errorSupplier) {
        this.errorSupplier = errorSupplier;
        return this;
    }

    public ShortFieldBuilder setSaveConsumer(Consumer<Short> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return this;
    }

    public ShortFieldBuilder setDefaultValue(Supplier<Short> defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public ShortFieldBuilder setDefaultValue(short defaultValue) {
        this.defaultValue = () -> {
            return defaultValue;
        };
        return this;
    }

    public ShortFieldBuilder setTooltipSupplier(Function<Short, Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public ShortFieldBuilder setTooltipSupplier(Supplier<Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = (i) -> {
            return tooltipSupplier.get();
        };
        return this;
    }

    public ShortFieldBuilder setTooltip(Optional<Text[]> tooltip) {
        this.tooltipSupplier = (i) -> {
            return tooltip;
        };
        return this;
    }

    public ShortFieldBuilder setTooltip(Text... tooltip) {
        this.tooltipSupplier = (i) -> {
            return Optional.ofNullable(tooltip);
        };
        return this;
    }

    public ShortFieldBuilder setMin(short min) {
        this.min = min;
        return this;
    }

    public ShortFieldBuilder setMax(short max) {
        this.max = max;
        return this;
    }

    public ShortFieldBuilder removeMin() {
        this.min = null;
        return this;
    }

    public ShortFieldBuilder removeMax() {
        this.max = null;
        return this;
    }

    public ShortFieldBuilder setRender(RenderAction render) {
        this.render = render;
        return this;
    }

    public ShortFieldBuilder removeRender() {
        this.render = (q, w, e, r, t, y, u, i, o, p, a) -> {};
        return this;
    }

    @NotNull
    public ShortListEntry build() {
        ShortListEntry entry = new ShortListEntry(this.getFieldNameKey(), this.value, this.getResetButtonKey(), this.defaultValue, this.saveConsumer, null, this.isRequireRestart(), this.render);
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