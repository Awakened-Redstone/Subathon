package com.awakenedredstone.subathon.ui.configure;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.ui.BaseScreen;
import com.awakenedredstone.subathon.util.MapBuilder;
import com.awakenedredstone.subathon.util.Texts;
import com.awakenedredstone.subathon.util.Utils;
import com.awakenedredstone.subathon.util.WeightedRandom;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.config.ui.component.ConfigTextBox;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.ui.util.Drawer;
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

        Subathon.potionsRandom = new WeightedRandom<>();
        Subathon.COMMON_CONFIGS.potionWeights().forEach((identifier, integer) -> Subathon.potionsRandom.add(integer, Registries.STATUS_EFFECT.get(identifier)));

        FlowLayout items = rootComponent.childById(FlowLayout.class, "effects");
        Objects.requireNonNull(items, "Effects block is required!");

        ButtonComponent doneButton = rootComponent.childById(ButtonComponent.class, "done");
        Asserts.notNull(doneButton, "doneButton");
        doneButton.onPress(button -> this.close());


        ButtonComponent refreshButton = rootComponent.childById(ButtonComponent.class, "refresh");
        Asserts.notNull(refreshButton, "refreshButton");
        refreshButton.onPress(button -> {
            Subathon.potionsRandom = new WeightedRandom<>();
            Subathon.COMMON_CONFIGS.potionWeights().forEach((identifier, integer) -> Subathon.potionsRandom.add(integer, Registries.STATUS_EFFECT.get(identifier)));
            
            items.children().stream().filter(v -> v.id() != null).forEach(component -> {
                StatusEffect effect = Registries.STATUS_EFFECT.get(new Identifier(component.id()));
                String percentageString = String.format("%.5g", Subathon.potionsRandom.percentage(effect)) + "%";
                String weightString = Subathon.potionsRandom.getWeight(effect) + "/" + Subathon.chaosRandom.getTotal();
                List<TooltipComponent> tooltip = new ArrayList<>();
                tooltip.add(TooltipComponent.of(Texts.of("<yellow>" + percentageString + "</yellow>").asOrderedText()));
                tooltip.add(TooltipComponent.of(Texts.of("<gray>" + weightString + "</gray>").asOrderedText()));
                component.tooltip(tooltip);
            });
        });

        Registries.STATUS_EFFECT.stream().forEach(statusEffect -> {
            Identifier identifier = Registries.STATUS_EFFECT.getId(statusEffect);
            var template = model.expandTemplate(FlowLayout.class, "effect", new MapBuilder.StringMap()
                    .put("translation", statusEffect.getTranslationKey())
                    .put("namespace", identifier.getNamespace())
                    .put("path", identifier.getPath())
                    .putAny("identifier", identifier)
                    .build());

            template.mouseEnter().subscribe(() -> template.surface((matrices, component1) -> Drawer.drawGradientRect(matrices,
                    component1.x(), component1.y(), component1.width(), component1.height(),
                    0xC0101010, 0x00101010, 0x00101010, 0xC0101010
            )));

            template.mouseLeave().subscribe(() -> template.surface(Surface.BLANK));

            FlowLayout container = template.childById(FlowLayout.class, "config-field");
            Objects.requireNonNull(items, "Configs block is required!");

            container.child(createTextBox(model, identifier, textBox -> textBox.configureForNumber(Integer.class)).optionProvider());

            String percentageString = String.format("%.5g", Subathon.potionsRandom.percentage(statusEffect)) + "%";
            String weightString = Subathon.potionsRandom.getWeight(statusEffect) + "/" + Subathon.chaosRandom.getTotal();
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
        Subathon.potionsRandom = new WeightedRandom<>();
        Subathon.COMMON_CONFIGS.potionWeights().forEach((identifier, integer) -> Subathon.potionsRandom.add(integer, Registries.STATUS_EFFECT.get(identifier)));
        super.close();
    }

    public static OptionComponentFactory.Result<FlowLayout, ConfigTextBox> createTextBox(UIModel model, Identifier identifier, Consumer<ConfigTextBox> processor) {
        var optionComponent = model.expandTemplate(FlowLayout.class, "text-box-config-option",
                new MapBuilder.StringMap()
                        .put("config-option-name", "text.config.subathon/mode.option.weight")
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
            } catch (Exception ignored) {}
        });

        processor.accept(valueBox);

        optionComponent.removeChild(valueBox);

        return new OptionComponentFactory.Result<>(optionComponent, valueBox);
    }
    
    private boolean isOver(Component component) {
        double clientMouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
        double clientMouseY = client.mouse.getY() / client.getWindow().getScaleFactor();
        return component.isInBoundingBox(clientMouseX, clientMouseY);
    }
}
