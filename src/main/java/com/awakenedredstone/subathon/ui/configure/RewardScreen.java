package com.awakenedredstone.subathon.ui.configure;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.duck.owo.ComponentDuck;
import com.awakenedredstone.subathon.owo.SubathonTextBox;
import com.awakenedredstone.subathon.twitch.Twitch;
import com.awakenedredstone.subathon.ui.BaseScreen;
import com.awakenedredstone.subathon.util.MapBuilder;
import com.awakenedredstone.subathon.util.Texts;
import com.awakenedredstone.subathon.util.Utils;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.ui.util.Drawer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Asserts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class RewardScreen extends BaseScreen<FlowLayout> {
    private UUID selected = SubathonClient.CLIENT_CONFIGS.rewardId();
    private boolean setMenuOpen = false;
    private SubathonTextBox textBox;

    public RewardScreen() {
        super(FlowLayout.class, "reward_screen");
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        Utils.load(ConfigScreen.class);
        FlowLayout items = rootComponent.childById(FlowLayout.class, "reward-items");

        ButtonComponent doneButton = rootComponent.childById(ButtonComponent.class, "done");
        Asserts.notNull(doneButton, "doneButton");
        doneButton.tooltip(Texts.of("text.subathon.screen.rewards.button.done.tooltip"));
        doneButton.onPress(button -> this.close());

        {
            var template = model.expandTemplate(FlowLayout.class, "set-id", new MapBuilder.StringMap().build());

            FlowLayout container = template.childById(FlowLayout.class, "config-field");
            Objects.requireNonNull(items, "Configs block is required!");

            textBox = createTextBox(model, SubathonTextBox::configureForUuid).optionProvider();
            textBox.text(parseSelected());
            container.child(textBox);

            ButtonComponent editButton = rootComponent.childById(ButtonComponent.class, "edit");
            Asserts.notNull(editButton, "doneButton");
            editButton.tooltip(Texts.of("text.subathon.screen.rewards.button.edit.tooltip"));
            editButton.onPress(button -> {
                if (setMenuOpen) rootComponent.removeChild(template);
                else rootComponent.child(template);
                setMenuOpen = !setMenuOpen;
            });

            ((ComponentDuck) template).subathon$registerUpdateListener((delta, mouseX, mouseY) -> {
                int x = editButton.x() - template.width() + 20 - template.margins().get().right() + template.margins().get().left();
                int y = editButton.y() - template.height() - template.margins().get().bottom() + template.margins().get().top();
                template.positioning(Positioning.absolute(x, y));
            });
        }

        SubathonClient.rewards.stream().filter(reward -> !reward.isUserInputRequired()).forEach(reward -> {
            var ref = new Object() {
                boolean wasShiftDown = Screen.hasShiftDown();
            };

            NativeImageBackedTexture image = (NativeImageBackedTexture) SubathonClient.runtimeRewardTextures.getTexture(Subathon.id("reward/" + reward.id()));
            var template = model.expandTemplate(FlowLayout.class, "reward", new MapBuilder.StringMap()
                    .put("id", reward.id())
                    .put("reward_name", reward.title())
                    .putAny("size", image.getImage().getWidth())
                    .build());

            if (template.id() == null) throw new IllegalStateException("Reward entry on UI can not be missing the ID!");
            UUID id = UUID.fromString(template.id());

            if (Objects.equals(id, selected)) {
                template.surface((matrices, component) ->
                        Drawer.drawGradientRect(matrices,
                                component.x(), component.y(), component.width(), component.height(),
                                0xC0006000, 0x00001000, 0x00001000, 0xC0006000
                        ));
            }

            ((ComponentDuck) template).subathon$registerUpdateListener((delta, mouseX, mouseY) -> {
                if ((ref.wasShiftDown != Screen.hasShiftDown() && isMouseOver(template)) || (template.tooltip() == null || template.tooltip().isEmpty())) {
                    int cost = reward.cost();
                    boolean paused = reward.isPaused();
                    boolean enabled = reward.isEnabled();
                    boolean subOnly = reward.isSubOnly();
                    boolean skipRequestQueue = reward.shouldRedemptionsSkipRequestQueue();
                    String prompt = reward.prompt();
                    List<Text> tooltip = new ArrayList<>();
                    tooltip.add(Text.literal(prompt == null ? "No description" : prompt));
                    tooltip.add(Text.empty());
                    tooltip.add(Texts.of("text.subathon.screen.rewards.cost", new MapBuilder.StringMap().putAny("%cost%", cost).build()));
                    if (Screen.hasShiftDown()) {
                        tooltip.add(Texts.of("text.subathon.screen.rewards.subOnly", new MapBuilder.StringMap().putAny("%boolean%", subOnly).build()));
                        tooltip.add(Texts.of("text.subathon.screen.rewards.paused", new MapBuilder.StringMap().putAny("%boolean%", paused).build()));
                        tooltip.add(Texts.of("text.subathon.screen.rewards.enabled", new MapBuilder.StringMap().putAny("%boolean%", enabled).build()));
                    } else {
                        tooltip.add(Texts.of("text.subathon.screen.rewards.more"));
                    }
                    if (skipRequestQueue) {
                        tooltip.add(Text.empty());
                        tooltip.add(Texts.of("text.subathon.screen.rewards.skip_queue.warning"));
                    }
                    template.tooltip(tooltip);
                    ref.wasShiftDown = Screen.hasShiftDown();
                }
            });

            template.mouseEnter().subscribe(() -> {
                if (!Objects.equals(id, selected)) {
                    //noinspection CodeBlock2Expr
                    template.surface((matrices, component) -> {
                        Drawer.drawGradientRect(matrices,
                                component.x(), component.y(), component.width(), component.height(),
                                0xC0101010, 0x00101010, 0x00101010, 0xC0101010
                        );
                    });
                }
            });

            template.mouseLeave().subscribe(() -> {
                if (!Objects.equals(id, selected)) template.surface(Surface.BLANK);
            });

            template.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == 0) {
                    if (selected == null || !selected.equals(id)) {
                        if (selected != null) {
                            FlowLayout item = rootComponent.childById(FlowLayout.class, selected.toString());
                            if (item != null) item.surface(Surface.BLANK);
                        }
                        this.playDownSound(MinecraftClient.getInstance().getSoundManager());
                        selected = id;
                        textBox.text(parseSelected());
                        template.surface((matrices, component) ->
                                Drawer.drawGradientRect(matrices,
                                        component.x(), component.y(), component.width(), component.height(),
                                        0xC0006000, 0x00001000, 0x00001000, 0xC0006000
                                ));
                    }
                } else if (button == 1) {
                    if (Objects.equals(selected, id)) {
                        this.playDownSound(MinecraftClient.getInstance().getSoundManager());
                        selected = null;
                        textBox.text(parseSelected());
                        //noinspection CodeBlock2Expr
                        template.surface((matrices, component) -> {
                            Drawer.drawGradientRect(matrices,
                                    component.x(), component.y(), component.width(), component.height(),
                                    0xC0101010, 0x00101010, 0x00101010, 0xC0101010
                            );
                        });
                    }
                }
                return true;
            });

            items.child(template);
        });

        ButtonComponent setRewardButton = rootComponent.childById(ButtonComponent.class, "the-button");
        Asserts.notNull(setRewardButton, "setRewardButton");
        setRewardButton.tooltip(Texts.of("text.subathon.screen.rewards.button.set.tooltip"));
        setRewardButton.onPress(buttonComponent -> save());

        drawSurface(rootComponent, client);
    }

    @Override
    public void close() {
        save();
        super.close();
    }

    private void save() {
        textBox.text(parseSelected());
        if (SubathonClient.CLIENT_CONFIGS.rewardId() != null) {
            Twitch.getInstance().toggleReward(SubathonClient.cache.get("token"), SubathonClient.CLIENT_CONFIGS.rewardId(), false);
        }

        SubathonClient.CLIENT_CONFIGS.rewardId(selected);
        Twitch.getInstance().toggleReward(SubathonClient.cache.get("token"), selected, true);
        ClientPlayNetworking.send(Subathon.id("reward_id"), PacketByteBufs.create().writeUuid(safeSelectedUUID()));
    }

    private void updateGui() {
        FlowLayout rootComponent = uiAdapter.rootComponent;
        FlowLayout items = rootComponent.childById(FlowLayout.class, "reward-items");
        for (FlowLayout reward : items.children().stream().map(component -> (FlowLayout) component).toList()) {
            if (reward.id() == null) throw new IllegalStateException("Reward entry on UI can not be missing the ID!");
            UUID id = UUID.fromString(reward.id());
            if (Objects.equals(id, selected)) {
                reward.surface((matrices, component) ->
                        Drawer.drawGradientRect(matrices,
                                component.x(), component.y(), component.width(), component.height(),
                                0xC0006000, 0x00001000, 0x00001000, 0xC0006000
                        ));
            } else reward.surface(Surface.BLANK);
        }
    }

    public OptionComponentFactory.Result<FlowLayout, SubathonTextBox> createTextBox(UIModel model, Consumer<SubathonTextBox> processor) {
        var optionComponent = model.expandTemplate(FlowLayout.class, "text-box-config-option",
                new MapBuilder.StringMap()
                        .put("config-option-name", "text.config.subathon/mode.option.weight")
                        .putAny("config-option-value", parseSelected())
                        .build());

        var valueBox = optionComponent.childById(SubathonTextBox.class, "value-box");

        valueBox.onChanged().subscribe(s -> {
            if (!valueBox.isValid()) return;
            selected = StringUtils.isBlank(s) ? null : UUID.fromString(s);
            updateGui();
        });

        processor.accept(valueBox);

        optionComponent.removeChild(valueBox);

        return new OptionComponentFactory.Result<>(optionComponent, valueBox);
    }

    private boolean isMouseOver(Component component) {
        double clientMouseX = client.mouse.getX() / client.getWindow().getScaleFactor();
        double clientMouseY = client.mouse.getY() / client.getWindow().getScaleFactor();
        return component.isInBoundingBox(clientMouseX, clientMouseY);
    }

    private String parseSelected() {
        return selected == null ? "" : selected.toString();
    }

    private UUID safeSelectedUUID() {
        return selected == null ? new UUID(0, 0) : selected;
    }
}
