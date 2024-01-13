package com.awakenedredstone.subathon.client.ui;

import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.util.Texts;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class NotificationMessageScreen extends BaseScreen<FlowLayout> {
    private final SubathonClient.Notification notification;

    public NotificationMessageScreen(SubathonClient.Notification notification) {
        super(FlowLayout.class, "notification_message_screen");
        this.notification = notification;
    }

    public NotificationMessageScreen(SubathonClient.Notification notification, Screen parent) {
        super(FlowLayout.class, "notification_message_screen", parent);
        this.notification = notification;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    protected void build(FlowLayout rootComponent) {
        int index = SubathonClient.getInstance().messages.indexOf(notification);

        var previous = rootComponent.childById(ButtonComponent.class, "previous");
        previous.onPress(button -> {
            client.setScreen(new NotificationMessageScreen(SubathonClient.getInstance().messages.get(index - 1), parent));
        });

        var next = rootComponent.childById(ButtonComponent.class, "next");
        next.onPress(button -> {
            client.setScreen(new NotificationMessageScreen(SubathonClient.getInstance().messages.get(index + 1), parent));
        });

        if (index == 0) previous.active = false;
        if (index >= SubathonClient.getInstance().messages.size() - 1) next.active = false;

        var closeButton = rootComponent.childById(ButtonComponent.class, "done");
        closeButton.onPress(button -> this.close());

        var title = rootComponent.childById(LabelComponent.class, "title");
        title.text(Texts.of(notification.message().translation, notification.placeholders()));
        title.shadow(true);

        var message = rootComponent.childById(LabelComponent.class, "message");
        message.text(Text.literal(notification.content())).shadow(true);
        message.shadow(true);
    }
}
