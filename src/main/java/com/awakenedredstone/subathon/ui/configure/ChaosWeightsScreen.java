package com.awakenedredstone.subathon.ui.configure;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.chaos.process.Chaos;
import com.awakenedredstone.subathon.core.effect.chaos.process.ChaosRegistry;
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
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.http.util.Asserts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ChaosWeightsScreen extends BaseScreen<FlowLayout> {

    public ChaosWeightsScreen() {
        super(FlowLayout.class, "chaos_effects_screen");
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        Utils.load(ConfigScreen.class);

        Subathon.chaosRandom = new WeightedRandom<>();
        Subathon.COMMON_CONFIGS.chaosWeights().forEach((identifier, integer) -> Subathon.chaosRandom.add(integer, ChaosRegistry.registry.get(identifier)));

        FlowLayout items = rootComponent.childById(FlowLayout.class, "effects");
        Objects.requireNonNull(items, "Effects block is required!");

        ButtonComponent doneButton = rootComponent.childById(ButtonComponent.class, "done");
        Asserts.notNull(doneButton, "doneButton");
        doneButton.onPress(button -> this.close());


        ButtonComponent refreshButton = rootComponent.childById(ButtonComponent.class, "refresh");
        Asserts.notNull(refreshButton, "refreshButton");
        refreshButton.onPress(button -> {
            Subathon.chaosRandom = new WeightedRandom<>();
            Subathon.COMMON_CONFIGS.chaosWeights().forEach((identifier, integer) -> Subathon.chaosRandom.add(integer, ChaosRegistry.registry.get(identifier)));
            
            items.children().stream().filter(v -> v.id() != null).forEach(component -> {
                Identifier identifier = new Identifier(component.id());
                Chaos chaos = ChaosRegistry.registry.get(identifier);
                String translationKey = "text.subathon.chaos." + identifier.toTranslationKey();
                String percentageString = String.format("%.5g", Subathon.chaosRandom.percentage(chaos)) + "%";
                String weightString = Subathon.chaosRandom.getWeight(chaos) + "/" + Subathon.chaosRandom.getTotal();
                if (I18n.hasTranslation(translationKey + ".tooltip")) {
                    List<TooltipComponent> tooltip = new ArrayList<>(client.textRenderer.wrapLines(Texts.of(translationKey + ".tooltip"), client.getWindow().getScaledWidth() / 2)
                            .stream().map(TooltipComponent::of).toList());
                    tooltip.add(TooltipComponent.of(Text.empty().asOrderedText()));
                    tooltip.add(TooltipComponent.of(Texts.of("<yellow>" + percentageString + "</yellow>").asOrderedText()));
                    tooltip.add(TooltipComponent.of(Texts.of("<gray>" + weightString + "</gray>").asOrderedText()));
                    component.tooltip(tooltip);
                } else {
                    List<TooltipComponent> tooltip = new ArrayList<>();
                    tooltip.add(TooltipComponent.of(Texts.of("<yellow>" + percentageString + "</yellow>").asOrderedText()));
                    tooltip.add(TooltipComponent.of(Texts.of("<gray>" + weightString + "</gray>").asOrderedText()));
                    component.tooltip(tooltip);
                }
            });
        });

        ChaosRegistry.registry.entrySet().stream().forEach(entry -> {
            Identifier identifier = entry.getKey();
            String translationKey = "text.subathon.chaos." + identifier.toTranslationKey();
            var template = model.expandTemplate(FlowLayout.class, "effect", new MapBuilder.StringMap()
                    .put("translation", translationKey)
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

            String percentageString = String.format("%.5g", Subathon.chaosRandom.percentage(entry.getValue())) + "%";
            String weightString = Subathon.chaosRandom.getWeight(entry.getValue()) + "/" + Subathon.chaosRandom.getTotal();
            if (I18n.hasTranslation(translationKey + ".tooltip")) {
                List<TooltipComponent> tooltip = new ArrayList<>(client.textRenderer.wrapLines(Texts.of(translationKey + ".tooltip"), client.getWindow().getScaledWidth() / 2)
                        .stream().map(TooltipComponent::of).toList());
                tooltip.add(TooltipComponent.of(Text.empty().asOrderedText()));
                tooltip.add(TooltipComponent.of(Texts.of("<yellow>" + percentageString + "</yellow>").asOrderedText()));
                tooltip.add(TooltipComponent.of(Texts.of("<gray>" + weightString + "</gray>").asOrderedText()));
                template.tooltip(tooltip);
            } else {
                List<TooltipComponent> tooltip = new ArrayList<>();
                tooltip.add(TooltipComponent.of(Texts.of("<yellow>" + percentageString + "</yellow>").asOrderedText()));
                tooltip.add(TooltipComponent.of(Texts.of("<gray>" + weightString + "</gray>").asOrderedText()));
                template.tooltip(tooltip);
            }

            items.child(template);
        });

        drawSurface(rootComponent, client);
    }

    @Override
    public void close() {
        Subathon.COMMON_CONFIGS.save();
        Subathon.chaosRandom = new WeightedRandom<>();
        Subathon.COMMON_CONFIGS.chaosWeights().forEach((identifier, integer) -> Subathon.chaosRandom.add(integer, ChaosRegistry.registry.get(identifier)));
        super.close();
    }

    public static OptionComponentFactory.Result<FlowLayout, ConfigTextBox> createTextBox(UIModel model, Identifier identifier, Consumer<ConfigTextBox> processor) {
        var optionComponent = model.expandTemplate(FlowLayout.class, "text-box-config-option",
                new MapBuilder.StringMap()
                        .put("config-option-name", "text.config.subathon/mode.option.weight")
                        .putAny("config-option-value", Subathon.COMMON_CONFIGS.chaosWeights().getOrDefault(identifier, 1))
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
                Subathon.COMMON_CONFIGS.chaosWeights().put(identifier, Integer.parseInt(s));
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
