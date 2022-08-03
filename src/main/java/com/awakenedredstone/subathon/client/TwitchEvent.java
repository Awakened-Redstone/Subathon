package com.awakenedredstone.subathon.client;

import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.twitch.Subscription;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public record TwitchEvent(String user, int amount, Subscription tier, SubathonCommand.Events event, String target, String message) {

    public MutableText getMessage() {
        return switch (event) {
            case SUBSCRIPTION -> Text.literal(I18n.translate("gui.subathon.event_logs.sub", user, tier.getName()));

            case RESUBSCRIPTION -> {
                MutableText text = Text.literal(I18n.translate("gui.subathon.event_logs.resub", user, amount, tier.getName()));
                if (!message.isEmpty()) text.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(message))));
                yield  text;
            }

            case SUB_GIFT -> {
                String key = amount != 1 ? "gui.subathon.event_logs.gift.plural" : "gui.subathon.event_logs.gift.singular";
                yield Text.literal(I18n.translate(key, user, amount, tier.getName()));
            }

            case GIFT_USER -> Text.literal(I18n.translate("gui.subathon.event_logs.gift_user", user, amount, tier.getName(), target));

            case CHEER -> Text.literal(I18n.translate("gui.subathon.event_logs.cheer", user, amount))
                    .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(message))));

            case REWARD -> Text.literal(I18n.translate("gui.subathon.event_logs.reward", user));

            default -> Text.translatable("gui.subathon.event_logs.error");
        };
    }
}
