package com.awakenedredstone.subathon.client;

import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.twitch.Subscription;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

@Environment(EnvType.CLIENT)
public record TwitchEvent(String name, int amount, Subscription tier, SubathonCommand.Events event, String target) {

    public Text getMessage() {
        switch (event) {
            case SUBSCRIPTION -> {
                return new LiteralText(I18n.translate("subathon.messages.event.sub", name, tier.getName()));
            }
            case RESUBSCRIPTION -> {
                return new LiteralText(I18n.translate("subathon.messages.event.resub", name, amount, tier.getName()));
            }
            case SUB_GIFT -> {
                return new LiteralText(I18n.translate("subathon.messages.event.gift", name, amount, tier.getName(), amount != 1 ? "s" : ""));
            }
            case GIFT_USER -> {
                return new LiteralText(I18n.translate("subathon.messages.event.gift_user", name, amount, tier.getName(), target));
            }
            case CHEER -> {
                return new LiteralText(I18n.translate("subathon.messages.event.cheer", name, amount));
            }
        }
        return new TranslatableText("subathon.messages.event.error");
    }
}
