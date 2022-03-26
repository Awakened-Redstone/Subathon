package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.util.MessageUtils;
import com.github.twitch4j.pubsub.domain.ChannelBitsData;
import com.github.twitch4j.pubsub.domain.SubGiftData;
import com.github.twitch4j.pubsub.domain.SubscriptionData;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.commons.lang.StringUtils;

import static com.awakenedredstone.subathon.Subathon.getConfigData;
import static com.awakenedredstone.subathon.Subathon.integration;

public class EventListener {

    public void subscriptionListener(ChannelSubscribeEvent event) {
        SubscriptionData data = event.getData();
        if (data.getIsGift()) return;
        Subscription tier = Subscription.valueOf(data.getSubPlan().name().replace("TWITCH_", ""));
        Text message = new TranslatableText("subathon.messages.subscription", data.getDisplayName(), tier.getName());

        integration.addSubs(getConfigData().subModifiers.get(tier.name().toLowerCase()));

        MessageUtils.sendEventMessage(message);
    }

    public void giftListener(ChannelSubGiftEvent event) {
        SubGiftData data = event.getData();
        Subscription tier = Subscription.valueOf(data.getTier().name().replace("TWITCH_", ""));
        Text message = new TranslatableText("subathon.messages.gift", StringUtils.isNotBlank(data.getDisplayName()) ? data.getDisplayName() : "Anonymous", data.getCount(), tier.getName());
        if (tier == Subscription.PRIME) {
            MessageUtils.sendGlobalMessage(player -> player.sendMessage(new LiteralText(String.format("§e%s how in the world did you manage to gift a §c§lPRIME §esub?",
                    StringUtils.isNotBlank(data.getDisplayName()) ? data.getDisplayName() : "Anonymous")), false));
        }

        integration.addSubs(getConfigData().subModifiers.get(tier.name().toLowerCase()) * data.getCount());

        MessageUtils.sendEventMessage(message);
    }

    public void cheerListener(ChannelBitsEvent event) {
        ChannelBitsData data = event.getData();
        Text message = new TranslatableText("subathon.messages.cheer", data.isAnonymous() ? "Anonymous" : data.getUserName(), data.getBitsUsed());
        if (getConfigData().cumulativeBits) {
            if (getConfigData().cumulativeIgnoreMin || data.getBitsUsed() >= getConfigData().bitMin) {
                integration.addBits(data.getBitsUsed());
            }
        } else if(data.getBitsUsed() >= getConfigData().bitMin) {
            integration.increaseValueFromBits(getConfigData().onePerCheer ? 1 : Math.floorDiv(data.getBitsUsed(), getConfigData().bitMin));
        }

        MessageUtils.sendEventMessage(message);
    }
}
