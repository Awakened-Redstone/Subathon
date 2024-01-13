package com.awakenedredstone.subathon.client.ui.configure;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.ui.BaseScreen;
import com.awakenedredstone.subathon.core.effect.chaos.Chaos;
import com.awakenedredstone.subathon.registry.SubathonRegistries;
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

        Subathon.getInstance().chaosRandom.reset();
        Subathon.COMMON_CONFIGS.chaosWeights().forEach((identifier, integer) -> Subathon.getInstance().chaosRandom.add(integer, SubathonRegistries.CHAOS.get(identifier)));


        FlowLayout items = rootComponent.childById(FlowLayout.class, "effects");
        Objects.requireNonNull(items, "Effects block is required!");

        ButtonComponent doneButton = rootComponent.childById(ButtonComponent.class, "done");
        Asserts.notNull(doneButton, "doneButton");
        doneButton.onPress(button -> this.close());


        ButtonComponent refreshButton = rootComponent.childById(ButtonComponent.class, "refresh");
        Asserts.notNull(refreshButton, "refreshButton");
        refreshButton.onPress(button -> refreshWeights(items));

        SubathonRegistries.CHAOS.stream().forEach(chaos -> {
            Identifier identifier = chaos.getIdentifier();
            String translationKey = "text.subathon.chaos." + identifier.toTranslationKey();
            var template = model.expandTemplate(FlowLayout.class, "effect", new MapBuilder.StringMap()
                .put("translation", translationKey)
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

            String percentageString = String.format("%.5g", Subathon.getInstance().chaosRandom.percentage(chaos)) + "%";
            String weightString = Subathon.getInstance().chaosRandom.getWeight(chaos) + "/" + Subathon.getInstance().chaosRandom.getTotal();
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
        Subathon.getInstance().chaosRandom.reset();
        Subathon.COMMON_CONFIGS.chaosWeights().forEach((identifier, integer) -> Subathon.getInstance().chaosRandom.add(integer, SubathonRegistries.CHAOS.get(identifier)));
        super.close();
    }

    public OptionComponentFactory.Result<FlowLayout, ConfigTextBox> createTextBox(UIModel model, Identifier identifier, Consumer<ConfigTextBox> processor) {
        Chaos chaos = SubathonRegistries.CHAOS.get(identifier);

        var optionComponent = model.expandTemplate(FlowLayout.class, "text-box-config-option",
            new MapBuilder.StringMap()
                .putAny("config-option-value", Subathon.COMMON_CONFIGS.chaosWeights().getOrDefault(identifier, chaos.getDefaultWeight()))
                .build());

        var valueBox = optionComponent.childById(ConfigTextBox.class, "value-box");
        var resetButton = optionComponent.childById(ButtonComponent.class, "reset-button");

        resetButton.active = !valueBox.getText().equals(String.valueOf(chaos.getDefaultWeight()));
        resetButton.onPress(button -> {
            valueBox.setText(String.valueOf(chaos.getDefaultWeight()));
            button.active = false;
        });

        valueBox.onChanged().subscribe(s -> {
            resetButton.active = !s.equals(String.valueOf(chaos.getDefaultWeight()));
            try {
                Subathon.COMMON_CONFIGS.chaosWeights().put(identifier, Integer.parseInt(s));
                refreshWeights(getComponent(FlowLayout.class, "effects"));
            } catch (Exception ignored) {
            }
        });

        processor.accept(valueBox);

        optionComponent.removeChild(valueBox);

        return new OptionComponentFactory.Result<>(optionComponent, valueBox);
    }

    private void refreshWeights(FlowLayout items) {
        Subathon.getInstance().chaosRandom.reset();
        Subathon.COMMON_CONFIGS.chaosWeights().forEach((identifier, integer) -> Subathon.getInstance().chaosRandom.add(integer, SubathonRegistries.CHAOS.get(identifier)));

        items.children().stream().filter(v -> v.id() != null).forEach(component -> {
            Identifier identifier = new Identifier(component.id());
            Chaos chaos = SubathonRegistries.CHAOS.get(identifier);
            String translationKey = "text.subathon.chaos." + identifier.toTranslationKey();
            String percentageString = String.format("%.5g", Subathon.getInstance().chaosRandom.percentage(chaos)) + "%";
            String weightString = Subathon.getInstance().chaosRandom.getWeight(chaos) + "/" + Subathon.getInstance().chaosRandom.getTotal();
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
    }
}
