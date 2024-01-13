package com.awakenedredstone.subathon.client.ui.configure;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.ui.BaseScreen;
import com.awakenedredstone.subathon.util.MapBuilder;
import com.awakenedredstone.subathon.util.Texts;
import com.awakenedredstone.subathon.util.Utils;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.config.ui.component.ConfigTextBox;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.parsing.UIModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.apache.http.util.Asserts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class PotionWeightsScreen extends BaseScreen<FlowLayout> {

    public PotionWeightsScreen() {
        super(FlowLayout.class, "potion_effects_screen");
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        Utils.load(ConfigScreen.class);

        Subathon.getInstance().potionsRandom.reset();
        Subathon.COMMON_CONFIGS.potionWeights().forEach((identifier, integer) -> Subathon.getInstance().potionsRandom.add(integer, Registries.STATUS_EFFECT.get(identifier)));

        FlowLayout items = rootComponent.childById(FlowLayout.class, "effects");
        Objects.requireNonNull(items, "Effects block is required!");

        ButtonComponent doneButton = rootComponent.childById(ButtonComponent.class, "done");
        Asserts.notNull(doneButton, "doneButton");
        doneButton.onPress(button -> this.close());


        ButtonComponent refreshButton = rootComponent.childById(ButtonComponent.class, "refresh");
        Asserts.notNull(refreshButton, "refreshButton");
        refreshButton.onPress(button -> {
            refreshWeights(items);
        });

        Registries.STATUS_EFFECT.stream().forEach(statusEffect -> {
            Identifier identifier = Registries.STATUS_EFFECT.getId(statusEffect);
            var template = model.expandTemplate(FlowLayout.class, "effect", new MapBuilder.StringMap()
                .put("translation", statusEffect.getTranslationKey())
                .put("namespace", identifier.getNamespace())
                .put("path", identifier.getPath())
                .putAny("identifier", identifier)
                .build());

            template.mouseEnter().subscribe(() -> template.surface((context, component1) -> context.drawGradientRect(
                component1.x(), component1.y(), component1.width(), component1.height(),
                0xC0101010, 0x00101010, 0x00101010, 0xC0101010
            )));

            template.mouseLeave().subscribe(() -> template.surface(Surface.BLANK));

            FlowLayout container = template.childById(FlowLayout.class, "config-field");
            Objects.requireNonNull(items, "Configs block is required!");

            container.child(createTextBox(model, identifier, textBox -> textBox.configureForNumber(Integer.class)).baseComponent());

            String percentageString = String.format("%.5g", Subathon.getInstance().potionsRandom.percentage(statusEffect)) + "%";
            String weightString = Subathon.getInstance().potionsRandom.getWeight(statusEffect) + "/" + Subathon.getInstance().potionsRandom.getTotal();
            List<TooltipComponent> tooltip = new ArrayList<>();
            tooltip.add(TooltipComponent.of(Texts.of("<yellow>" + percentageString + "</yellow>").asOrderedText()));
            tooltip.add(TooltipComponent.of(Texts.of("<gray>" + weightString + "</gray>").asOrderedText()));
            template.tooltip(tooltip);

            items.child(template);
        });

        drawSurface(rootComponent, client);
    }

    @Override
    public void close() {
        Subathon.COMMON_CONFIGS.save();
        Subathon.getInstance().potionsRandom.reset();
        Subathon.COMMON_CONFIGS.potionWeights().forEach((identifier, integer) -> Subathon.getInstance().potionsRandom.add(integer, Registries.STATUS_EFFECT.get(identifier)));
        super.close();
    }

    public OptionComponentFactory.Result<FlowLayout, ConfigTextBox> createTextBox(UIModel model, Identifier identifier, Consumer<ConfigTextBox> processor) {
        var optionComponent = model.expandTemplate(FlowLayout.class, "text-box-config-option",
            new MapBuilder.StringMap()
                .putAny("config-option-value", Subathon.COMMON_CONFIGS.potionWeights().getOrDefault(identifier, 1))
                .build());

        var valueBox = optionComponent.childById(ConfigTextBox.class, "value-box");
        var resetButton = optionComponent.childById(ButtonComponent.class, "reset-button");

        resetButton.active = !valueBox.getText().equals("1");
        resetButton.onPress(button -> {
            valueBox.setText("1");
            button.active = false;
        });

        valueBox.onChanged().subscribe(s -> {
            resetButton.active = !s.equals("1");
            try {
                Subathon.COMMON_CONFIGS.potionWeights().put(identifier, Integer.parseInt(s));
                refreshWeights(getComponent(FlowLayout.class, "effects"));
            } catch (Exception ignored) {
            }
        });

        processor.accept(valueBox);

        optionComponent.removeChild(valueBox);

        return new OptionComponentFactory.Result<>(optionComponent, valueBox);
    }

    private void refreshWeights(FlowLayout items) {
        Subathon.getInstance().potionsRandom.reset();
        Subathon.COMMON_CONFIGS.potionWeights().forEach((identifier, integer) -> Subathon.getInstance().potionsRandom.add(integer, Registries.STATUS_EFFECT.get(identifier)));

        items.children().stream().filter(v -> v.id() != null).forEach(component -> {
            StatusEffect effect = Registries.STATUS_EFFECT.get(new Identifier(component.id()));
            String percentageString = String.format("%.5g", Subathon.getInstance().potionsRandom.percentage(effect)) + "%";
            String weightString = Subathon.getInstance().potionsRandom.getWeight(effect) + "/" + Subathon.getInstance().potionsRandom.getTotal();
            List<TooltipComponent> tooltip = new ArrayList<>();
            tooltip.add(TooltipComponent.of(Texts.of("<yellow>" + percentageString + "</yellow>").asOrderedText()));
            tooltip.add(TooltipComponent.of(Texts.of("<gray>" + weightString + "</gray>").asOrderedText()));
            component.tooltip(tooltip);
        });
    }
}
