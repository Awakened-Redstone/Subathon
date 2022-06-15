package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.cubecontroller.util.MessageUtils;
import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.util.SubathonMessageUtils;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.common.util.TwitchUtils;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import static com.awakenedredstone.subathon.Subathon.*;

public class EventListener {

    /*TODO:UPDATE_THIS*/
    public void subscriptionListener(SubscriptionEvent event) {
        if (event.getGifted()) return;
        Subscription tier = Subscription.valueOf(event.getSubPlan().name().replace("TWITCH_", ""));
        String user = event.getMessageEvent() != null ? event.getMessageEvent().getTagValue("display-user").orElse(event.getUser().getName()) : event.getUser().getName();
        MutableText message = Text.translatable("text.subathon.event.subscription", user, tier.getName());
        if (event.getMessage().isPresent()) {
            message.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(event.getMessage().get()))));
        }

        SubathonCommand.Events type = event.getMonths() == 1 ? SubathonCommand.Events.SUBSCRIPTION : SubathonCommand.Events.RESUBSCRIPTION;

        integration.addSubs(getConfigData().subModifiers.get(tier));

        SubathonMessageUtils.sendEventMessage(user, "", event.getMonths(), tier, type, event.getMessage().orElse(""));
    }

    /*TODO:UPDATE_THIS*/
    public void giftListener(GiftSubscriptionsEvent event) {
        SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromString(event.getSubscriptionPlan());
        Subscription tier = Subscription.valueOf(subscriptionPlan.name().replace("TWITCH_", ""));
        String user = event.getUser().equals(TwitchUtils.ANONYMOUS_GIFTER) ? "Anonymous" : event.getUser().getName();
        Text message = Text.translatable("text.subathon.event.gift", user, event.getCount(), tier.getName(), event.getCount() != 1 ? "s" : "");
        if (tier == Subscription.PRIME) {
            String sorryWhat = String.format("§e%s how in the world did you manage to gift a §c§lPRIME §esub?", user);
            MessageUtils.broadcast(player -> player.sendMessage(Text.literal(sorryWhat), false), identifier("how"));
        }

        integration.addSubs(getConfigData().subModifiers.get(tier) * event.getCount());

        SubathonMessageUtils.sendEventMessage(user, "", event.getCount(), tier, SubathonCommand.Events.SUB_GIFT, "");
    }

    /*TODO:UPDATE_THIS*/
    public void specificGiftListener(SpecificSubGiftEvent e) {
        e.getEvents().forEach(event -> {
            SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromString(event.getSubscriptionPlan());
            Subscription tier = Subscription.valueOf(subscriptionPlan.name().replace("TWITCH_", ""));
            String user = event.getUser().equals(TwitchUtils.ANONYMOUS_GIFTER) ? "Anonymous" :
                    (event.getMessageEvent() != null ? event.getMessageEvent().getTagValue("display-user").orElse(event.getUser().getName()) : event.getUser().getName());
            String target = event.getUser().getName();
            Text message = Text.translatable("text.subathon.event.gift_user", user, target, tier.getName());
            if (tier == Subscription.PRIME) {
                String sorryWhat = String.format("§e%s how in the world did you manage to gift a §c§lPRIME §esub?", target);
                MessageUtils.broadcast(player -> player.sendMessage(Text.literal(sorryWhat), false), identifier("how"));
            }

            integration.addSubs(getConfigData().subModifiers.get(tier));

            SubathonMessageUtils.sendEventMessage(user, target, 0, tier, SubathonCommand.Events.GIFT_USER, "");
        });
    }

    /*TODO:UPDATE_THIS*/
    public void cheerListener(CheerEvent event) {
        String user = event.getUser().equals(TwitchUtils.ANONYMOUS_CHEERER) ? "Anonymous" :
                (event.getMessageEvent() != null ? event.getMessageEvent().getTagValue("display-user").orElse(event.getUser().getName()) : event.getUser().getName());
        MutableText message = Text.translatable("text.subathon.event.cheer", user, event.getBits());
        if (getConfigData().cumulativeBits) {
            if (getConfigData().cumulativeIgnoreMin || event.getBits() >= getConfigData().bitMin) {
                integration.addBits(event.getBits());
            }
        } else if (event.getBits() >= getConfigData().bitMin) {
            int toAdd = getConfigData().onePerCheer ? 1 : Math.floorDiv(event.getBits(), getConfigData().bitMin);
            integration.increaseValue(toAdd * getConfigData().bitModifier);
        }

        message.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(event.getMessage()))));

        SubathonMessageUtils.sendEventMessage(user, "", event.getBits(), Subscription.NONE, SubathonCommand.Events.CHEER, event.getMessage());
    }
}
