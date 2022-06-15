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
        switch (event) {
            case SUBSCRIPTION -> {
                return Text.literal(I18n.translate("gui.subathon.event_logs.sub", user, tier.getName()));
            }
            case RESUBSCRIPTION -> {
                MutableText text = Text.literal(I18n.translate("gui.subathon.event_logs.resub", user, amount, tier.getName()));
                if (!message.isEmpty()) text.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(message))));
                return text;
            }
            case SUB_GIFT -> {
                String key = amount != 1 ? "gui.subathon.event_logs.gift.plural" : "gui.subathon.event_logs.gift.singular";
                return Text.literal(I18n.translate(key, user, amount, tier.getName()));
            }
            case GIFT_USER -> {
                return Text.literal(I18n.translate("gui.subathon.event_logs.gift_user", user, amount, tier.getName(), target));
            }
            case CHEER -> {
                return Text.literal(I18n.translate("gui.subathon.event_logs.cheer", user, amount))
                        .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(message))));
            }
        }
        return Text.translatable("gui.subathon.event_logs.error");
    }
}
