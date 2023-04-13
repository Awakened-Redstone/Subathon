package com.awakenedredstone.subathon.client.ui;

import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.twitch.EventMessages;
import com.awakenedredstone.subathon.util.MapBuilder;
import com.awakenedredstone.subathon.util.Texts;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.util.Drawer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.Text;

import java.util.*;

@Environment(EnvType.CLIENT)
public class NotificationsScreen extends BaseScreen<FlowLayout> {
    private List<SubathonClient.Notification> notifications = List.copyOf(SubathonClient.messages);

    public NotificationsScreen() {
        super(FlowLayout.class, "notifications_screen");
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    protected void build(FlowLayout rootComponent) {
        FlowLayout items = rootComponent.childById(FlowLayout.class, "notifications");
        Objects.requireNonNull(items, "Notifications block is required!");

        for (SubathonClient.Notification notification : SubathonClient.messages) {
            var template = model.expandTemplate(FlowLayout.class, "notification", new MapBuilder<String, String>()
                    .put("id", UUID.randomUUID().toString())
                    .put("type", getType(notification.message(), notification.placeholders()))
                    .put("message", Texts.of(notification.message().translation, map -> map.putAll(notification.placeholders())).getString())
                    .build());

            template.mouseEnter().subscribe(() -> {
                template.surface((matrices, component) -> {
                    Drawer.drawGradientRect(matrices,
                            component.x(), component.y(), component.width(), component.height(),
                            0xC0101010, 0x00101010, 0x00101010, 0xC0101010
                    );
                });
            });

            template.mouseLeave().subscribe(() -> {
                template.surface(Surface.BLANK);
            });

            List<TooltipComponent> tooltip = client.textRenderer.wrapLines(Text.literal(notification.content()), client.getWindow().getScaledWidth() - 16)
                    .stream().map(TooltipComponent::of).toList();
            template.tooltip(tooltip);

            template.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == 0) {
                    this.playDownSound(MinecraftClient.getInstance().getSoundManager());
                    client.setScreen(new NotificationMessageScreen(notification));
                }
                return true;
            });

            items.child(template);
        }

        var closeButton = rootComponent.childById(ButtonComponent.class, "done");
        closeButton.onPress(button -> this.close());
    }

    @Override
    public void tick() {
        if (client == null || client.currentScreen != this) return;
        if (!new HashSet<>(notifications).containsAll(SubathonClient.messages)) {
            HashSet<SubathonClient.Notification> messagesCopy = new HashSet<>(SubathonClient.messages);
            notifications.forEach(messagesCopy::remove);
            notifications = List.copyOf(SubathonClient.messages);

            FlowLayout items = this.uiAdapter.rootComponent.childById(FlowLayout.class, "notifications");
            Objects.requireNonNull(items, "Notifications block is required!");

            for (SubathonClient.Notification notification : messagesCopy) {
                var template = model.expandTemplate(FlowLayout.class, "notification", new MapBuilder<String, String>()
                        .put("id", UUID.randomUUID().toString())
                        .put("type", getType(notification.message(), notification.placeholders()))
                        .put("message", Texts.of(notification.message().translation, map -> map.putAll(notification.placeholders())).getString())
                        .build());

                template.mouseEnter().subscribe(() -> {
                    template.surface((matrices, component) -> {
                        Drawer.drawGradientRect(matrices,
                                component.x(), component.y(), component.width(), component.height(),
                                0xC0101010, 0x00101010, 0x00101010, 0xC0101010
                        );
                    });
                });

                template.mouseLeave().subscribe(() -> {
                    template.surface(Surface.BLANK);
                });

                List<TooltipComponent> tooltip = client.textRenderer.wrapLines(Text.literal(notification.content()), client.getWindow().getScaledWidth() - 16)
                        .stream().map(TooltipComponent::of).toList();
                template.tooltip(tooltip);

                template.mouseDown().subscribe((mouseX, mouseY, button) -> {
                    if (button == 0) {
                        this.playDownSound(MinecraftClient.getInstance().getSoundManager());
                        client.setScreen(new NotificationMessageScreen(notification));
                    }
                    return true;
                });

                items.child(template);
            }
        }
        super.tick();
    }

    public static String getType(EventMessages message, Map<String, String> map) {
        int amount = Integer.parseInt(map.getOrDefault("%amount%", "-1"));
        return switch (message) {
            case SUB, RESUB, GIFT -> "gift";
            case CHEER -> {
                if (amount < 100) yield "1";
                else if (amount < 1000) yield "100";
                else if (amount < 10000) yield "1000";
                else if (amount < 100000) yield "10000";
                else yield "100000";
            }
            default -> "unknown";
        };
    }
}
