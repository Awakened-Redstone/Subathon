package com.awakenedredstone.subathon.ui.configure;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.process.Effect;
import com.awakenedredstone.subathon.duck.ComponentDuck;
import com.awakenedredstone.subathon.mixin.owo.BaseComponentMixin;
import com.awakenedredstone.subathon.ui.BaseScreen;
import com.awakenedredstone.subathon.util.MapBuilder;
import com.awakenedredstone.subathon.util.Texts;
import com.awakenedredstone.subathon.util.Utils;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.config.ui.component.ConfigTextBox;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.parsing.UIModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
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
public class EffectsScreen extends BaseScreen<FlowLayout> {

    public EffectsScreen() {
        super(FlowLayout.class, "effects_screen");
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        Utils.load(ConfigScreen.class);
        FlowLayout items = rootComponent.childById(FlowLayout.class, "effects");
        Objects.requireNonNull(items, "Effects block is required!");

        ButtonComponent doneButton = rootComponent.childById(ButtonComponent.class, "done");
        Asserts.notNull(doneButton, "doneButton");
        doneButton.onPress(button -> this.close());

        var ref = new Object() {
            Identifier settingsOpen = null;
            FlowLayout openComponent = null;
        };

        rootComponent.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (button == 0) {
                if (ref.settingsOpen != null && !isMouseOver(ref.openComponent)) {
                    Subathon.COMMON_CONFIGS.save();
                    rootComponent.removeChild(ref.openComponent);
                    ref.settingsOpen = null;
                    ref.openComponent = null;
                    return true;
                }
            }
            return false;
        });

        Subathon.COMMON_CONFIGS.effects().forEach((identifier, effect) -> {
            var template = model.expandTemplate(FlowLayout.class, "mode", new MapBuilder<String, String>()
                    .put("id", identifier.toTranslationKey())
                    .build());

            var options = model.expandTemplate(FlowLayout.class, "mode-options", new MapBuilder<String, String>()
                    .put("scale", String.valueOf(effect.scale))
                    .build());

            var container = options.childById(FlowLayout.class, "container");
            var scroller = options.childById(ScrollContainer.class, "scroller");
            if (effect.isScalable()) {
                OptionComponentFactory.Result scaleOption = createTextBox(model, effect, textBox -> textBox.configureForNumber(Double.class));
                container.child(scaleOption.baseComponent());
            }
            effect.generateConfig(container, this.model);
            if (container.children().isEmpty()) {
                container.child(Components.label(Text.translatable("text.config.subathon/mode.error.empty")));
            }

            ((ComponentDuck) scroller).subathon$registerUpdateListener((delta, mouseX, mouseY) -> {
                int componentY = options.positioning().get().y;
                int contentSize = ((BaseComponentMixin) container).callDetermineVerticalContentSize(Sizing.content());
                int verticalSize = contentSize == 0 ? 100 : Math.min(contentSize, client.getWindow().getScaledHeight() - componentY - 20);
                scroller.sizing(options.horizontalSizing().get(), Sizing.fixed(verticalSize));
            });

            template.mouseEnter().subscribe(() -> template.surface(HOVER));

            template.mouseLeave().subscribe(() -> template.surface(Surface.BLANK));

            template.childById(LabelComponent.class, "status").text(Text.translatable("text.subathon.screen.effects." + (effect.enabled ? "enabled" : "disabled")));

            template.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (ref.settingsOpen != null && isMouseOver(ref.openComponent)) return false;
                int absoluteMouseY = (int) (this.client.mouse.getY() / this.client.getWindow().getScaleFactor());
                if (button == 0) {
                    if (ref.settingsOpen != null) {
                        Subathon.COMMON_CONFIGS.save();
                        rootComponent.removeChild(ref.openComponent);
                        ref.settingsOpen = null;
                        ref.openComponent = null;
                    }
                    this.playDownSound(MinecraftClient.getInstance().getSoundManager());
                    effect.enabled = !effect.enabled;
                    template.childById(LabelComponent.class, "status").text(Text.translatable("text.subathon.screen.effects." + (effect.enabled ? "enabled" : "disabled")));
                } else if (button == 1) {
                    this.playDownSound(MinecraftClient.getInstance().getSoundManager());
                    options.positioning(Positioning.absolute((int) mouseX, absoluteMouseY));
                    if (ref.settingsOpen != null) {
                        if (ref.settingsOpen != identifier) {
                            Subathon.COMMON_CONFIGS.save();
                            rootComponent.removeChild(ref.openComponent);
                            ref.settingsOpen = identifier;
                            ref.openComponent = options;
                            rootComponent.child(options);
                        } else {
                            Subathon.COMMON_CONFIGS.save();
                            rootComponent.removeChild(ref.openComponent);
                            ref.settingsOpen = null;
                            ref.openComponent = null;
                        }
                    } else {
                        ref.settingsOpen = identifier;
                        ref.openComponent = options;
                        rootComponent.child(options);
                    }
                }
                return true;
            });

            String translationKey = "text.subathon.effects." + identifier.toTranslationKey();
            if (I18n.hasTranslation(translationKey + ".tooltip")) {
                List<TooltipComponent> tooltip = new ArrayList<>(client.textRenderer.wrapLines(Texts.of(translationKey + ".tooltip"), client.getWindow().getScaledWidth() / 2)
                        .stream().map(TooltipComponent::of).toList());
                template.tooltip(tooltip);
            }

            items.child(template);
        });

        drawSurface(rootComponent, client);
    }

    @Override
    public void close() {
        Subathon.COMMON_CONFIGS.save();
        super.close();
    }

    public static OptionComponentFactory.Result createTextBox(UIModel model, Effect effect, Consumer<ConfigTextBox> processor) {
        var optionComponent = model.expandTemplate(FlowLayout.class, "text-box-config-option",
                new MapBuilder<String, String>()
                        .put("config-option-name", "text.config.subathon/mode.option.scale")
                        .put("config-option-value", String.valueOf(effect.scale))
                        .build());

        var valueBox = optionComponent.childById(ConfigTextBox.class, "value-box");
        var resetButton = optionComponent.childById(ButtonComponent.class, "reset-button");

        resetButton.active = !valueBox.getText().equals("1.0");
        resetButton.onPress(button -> {
            valueBox.setText("1.0");
            button.active = false;
        });

        valueBox.onChanged().subscribe(s -> {
            resetButton.active = !s.equals("1.0");
            try {
                effect.scale = Double.parseDouble(s);
            } catch (Exception ignored) {}
        });

        processor.accept(valueBox);

        optionComponent.removeChild(valueBox);

        return new OptionComponentFactory.Result(optionComponent, valueBox);
    }
    
    private boolean isMouseOver(Component component) {
        double clientMouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
        double clientMouseY = client.mouse.getY() / client.getWindow().getScaleFactor();
        return component.isInBoundingBox(clientMouseX, clientMouseY);
    }
}
