package com.awakenedredstone.subathon.client;

import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.twitch.Subscription;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.*;

@Environment(EnvType.CLIENT)
public record TwitchEvent(String name, int amount, Subscription tier, SubathonCommand.Events event, String message) {

    public Text getMessage() {
        switch (event) {
            case SUBSCRIPTION -> {
                return new LiteralText(I18n.translate("gui.subathon.event_logs.sub", name, tier.getName()));
            }
            case RESUBSCRIPTION -> {
                BaseText text = new LiteralText(I18n.translate("gui.subathon.event_logs.resub", name, amount, tier.getName()));
                if (!message.isEmpty()) text.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(message))));
                return text;
            }
            case SUB_GIFT -> {
                return new LiteralText(I18n.translate("gui.subathon.event_logs.gift", name, amount, tier.getName(), amount != 1 ? "s" : ""));
            }
            case GIFT_USER -> {
                return new LiteralText(I18n.translate("gui.subathon.event_logs.gift_user", name, amount, tier.getName(), message));
            }
            case CHEER -> {
                return new LiteralText(I18n.translate("gui.subathon.event_logs.cheer", name, amount))
                        .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText(message))));
            }
        }
        return new TranslatableText("gui.subathon.event_logs.error");
    }
}
