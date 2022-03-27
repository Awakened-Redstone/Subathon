package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.util.MessageUtils;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.common.util.TwitchUtils;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import static com.awakenedredstone.subathon.Subathon.getConfigData;
import static com.awakenedredstone.subathon.Subathon.integration;

public class EventListener {

    public void subscriptionListener(SubscriptionEvent event) {
        if (event.getGifted()) return;
        Subscription tier = Subscription.valueOf(event.getSubPlan().name().replace("TWITCH_", ""));
        String name = event.getMessageEvent() != null ? event.getMessageEvent().getTagValue("display-name").orElse(event.getUser().getName()) : event.getUser().getName();
        Text message = new TranslatableText("subathon.messages.subscription", name, tier.getName());

        integration.addSubs(getConfigData().subModifiers.get(tier.name().toLowerCase()));

        MessageUtils.sendEventMessage(message);
    }

    public void giftListener(GiftSubscriptionsEvent event) {
        SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromString(event.getSubscriptionPlan());
        Subscription tier = Subscription.valueOf(subscriptionPlan.name().replace("TWITCH_", ""));
        String name = event.getUser().equals(TwitchUtils.ANONYMOUS_GIFTER) ? "Anonymous" : event.getUser().getName();
        Text message = new TranslatableText("subathon.messages.gift", name, event.getCount(), tier.getName(), event.getCount() != 1 ? "s" : "");
        if (tier == Subscription.PRIME) {
            MessageUtils.broadcast(player -> player.sendMessage(new LiteralText(
                    String.format("§e%s how in the world did you manage to gift a §c§lPRIME §esub?", name)), false), "what");
        }

        integration.addSubs(getConfigData().subModifiers.get(tier.name().toLowerCase()) * event.getCount());

        MessageUtils.sendEventMessage(message);
    }

    public void specificGiftListener(SpecificSubGiftEvent e) {
        if (e.getEvents().size() == 1) {
            SubscriptionEvent event = e.getEvents().stream().toList().get(0);
            SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromString(event.getSubscriptionPlan());
            Subscription tier = Subscription.valueOf(subscriptionPlan.name().replace("TWITCH_", ""));
            String gifterName = event.getUser().equals(TwitchUtils.ANONYMOUS_GIFTER) ? "Anonymous" :
                    (event.getMessageEvent() != null ? event.getMessageEvent().getTagValue("display-name").orElse(event.getUser().getName()) : event.getUser().getName());
            String name = event.getUser().getName();
            Text message = new TranslatableText("subathon.messages.gift_user", gifterName, name, tier.getName());
            if (tier == Subscription.PRIME) {
                MessageUtils.broadcast(player -> player.sendMessage(new LiteralText(
                        String.format("§e%s how in the world did you manage to gift a §c§lPRIME §esub?", name)), false), "what");
            }

            integration.addSubs(getConfigData().subModifiers.get(tier.name().toLowerCase()));

            MessageUtils.sendEventMessage(message);
        } else {
            e.getEvents().forEach(event -> {
                SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromString(event.getSubscriptionPlan());
                Subscription tier = Subscription.valueOf(subscriptionPlan.name().replace("TWITCH_", ""));
                String gifterName = event.getUser().equals(TwitchUtils.ANONYMOUS_GIFTER) ? "Anonymous" :
                        (event.getMessageEvent() != null ? event.getMessageEvent().getTagValue("display-name").orElse(event.getUser().getName()) : event.getUser().getName());
                String name = event.getUser().getName();
                Text message = new TranslatableText("subathon.messages.gift_user", gifterName, name, tier.getName());
                if (tier == Subscription.PRIME) {
                    MessageUtils.broadcast(player -> player.sendMessage(new LiteralText(
                            String.format("§e%s how in the world did you manage to gift a §c§lPRIME §esub?", name)), false), "what");
                }

                integration.addSubs(getConfigData().subModifiers.get(tier.name().toLowerCase()));

                MessageUtils.broadcast(player -> player.sendMessage(message, false), "event_message");
            });
        }
    }

    public void cheerListener(CheerEvent event) {
        String name = event.getUser().equals(TwitchUtils.ANONYMOUS_CHEERER) ? "Anonymous" :
                (event.getMessageEvent() != null ? event.getMessageEvent().getTagValue("display-name").orElse(event.getUser().getName()) : event.getUser().getName());
        Text message = new TranslatableText("subathon.messages.cheer", name, event.getBits());
        if (getConfigData().cumulativeBits) {
            if (getConfigData().cumulativeIgnoreMin || event.getBits() >= getConfigData().bitMin) {
                integration.addBits(event.getBits());
            }
        } else if(event.getBits() >= getConfigData().bitMin) {
            integration.increaseValueFromBits(getConfigData().onePerCheer ? 1 : Math.floorDiv(event.getBits(), getConfigData().bitMin));
        }

        MessageUtils.sendEventMessage(message);
    }
}
