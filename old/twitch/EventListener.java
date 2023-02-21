package old.twitch;

import old.Subathon;
import old.commands.SubathonCommand;
import old.util.MessageUtils;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.common.util.TwitchUtils;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class EventListener {

    public void subscriptionListener(SubscriptionEvent event) {
        if (event.getGifted()) return;
        Subscription tier = Subscription.valueOf(event.getSubPlan().name().replace("TWITCH_", ""));
        String name = event.getMessageEvent() != null ? event.getMessageEvent().getTagValue("display-name").orElse(event.getUser().getName()) : event.getUser().getName();
        Text message = new TranslatableText("text.subathon.event.subscription", name, tier.getName());

        Subathon.integration.addSubs(Subathon.getConfigData().subModifiers.get(tier.name().toLowerCase()));

        MessageUtils.sendEventMessage(message);

        {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(name);
            buf.writeInt(event.getMonths());
            buf.writeEnumConstant(tier);
            buf.writeEnumConstant(event.getMonths() == 1 ? SubathonCommand.Events.SUBSCRIPTION : SubathonCommand.Events.RESUBSCRIPTION);
            buf.writeString("");
            MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "event"), buf), "event_packet");
        }
    }

    public void giftListener(GiftSubscriptionsEvent event) {
        SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromString(event.getSubscriptionPlan());
        Subscription tier = Subscription.valueOf(subscriptionPlan.name().replace("TWITCH_", ""));
        String name = event.getUser().equals(TwitchUtils.ANONYMOUS_GIFTER) ? "Anonymous" : event.getUser().getName();
        Text message = new TranslatableText("text.subathon.event.gift", name, event.getCount(), tier.getName(), event.getCount() != 1 ? "s" : "");
        if (tier == Subscription.PRIME) {
            MessageUtils.broadcast(player -> player.sendMessage(new LiteralText(
                    String.format("§e%s how in the world did you manage to gift a §c§lPRIME §esub?", name)), false), "what");
        }

        Subathon.integration.addSubs(Subathon.getConfigData().subModifiers.get(tier.name().toLowerCase()) * event.getCount());

        MessageUtils.sendEventMessage(message);

        {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(name);
            buf.writeInt(event.getCount());
            buf.writeEnumConstant(tier);
            buf.writeEnumConstant(SubathonCommand.Events.SUB_GIFT);
            buf.writeString("");
            MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "event"), buf), "event_packet");
        }
    }

    public void specificGiftListener(SpecificSubGiftEvent e) {
        if (e.getEvents().size() == 1) {
            SubscriptionEvent event = e.getEvents().stream().toList().get(0);
            SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromString(event.getSubscriptionPlan());
            Subscription tier = Subscription.valueOf(subscriptionPlan.name().replace("TWITCH_", ""));
            String gifterName = event.getUser().equals(TwitchUtils.ANONYMOUS_GIFTER) ? "Anonymous" :
                    (event.getMessageEvent() != null ? event.getMessageEvent().getTagValue("display-name").orElse(event.getUser().getName()) : event.getUser().getName());
            String name = event.getUser().getName();
            Text message = new TranslatableText("text.subathon.event.gift_user", gifterName, name, tier.getName());
            if (tier == Subscription.PRIME) {
                MessageUtils.broadcast(player -> player.sendMessage(new LiteralText(
                        String.format("§e%s how in the world did you manage to gift a §c§lPRIME §esub?", name)), false), "what");
            }

            Subathon.integration.addSubs(Subathon.getConfigData().subModifiers.get(tier.name().toLowerCase()));

            MessageUtils.sendEventMessage(message);
        } else {
            e.getEvents().forEach(event -> {
                SubscriptionPlan subscriptionPlan = SubscriptionPlan.fromString(event.getSubscriptionPlan());
                Subscription tier = Subscription.valueOf(subscriptionPlan.name().replace("TWITCH_", ""));
                String gifterName = event.getUser().equals(TwitchUtils.ANONYMOUS_GIFTER) ? "Anonymous" :
                        (event.getMessageEvent() != null ? event.getMessageEvent().getTagValue("display-name").orElse(event.getUser().getName()) : event.getUser().getName());
                String name = event.getUser().getName();
                Text message = new TranslatableText("text.subathon.event.gift_user", gifterName, name, tier.getName());
                if (tier == Subscription.PRIME) {
                    MessageUtils.broadcast(player -> player.sendMessage(new LiteralText(
                            String.format("§e%s how in the world did you manage to gift a §c§lPRIME §esub?", name)), false), "what");
                }

                Subathon.integration.addSubs(Subathon.getConfigData().subModifiers.get(tier.name().toLowerCase()));

                MessageUtils.broadcast(player -> player.sendMessage(message, false), "event_message");

                {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeString(gifterName);
                    buf.writeInt(0);
                    buf.writeEnumConstant(tier);
                    buf.writeEnumConstant(SubathonCommand.Events.GIFT_USER);
                    buf.writeString(name);
                    MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "event"), buf), "event_packet");
                }
            });
        }
    }

    public void cheerListener(CheerEvent event) {
        String name = event.getUser().equals(TwitchUtils.ANONYMOUS_CHEERER) ? "Anonymous" :
                (event.getMessageEvent() != null ? event.getMessageEvent().getTagValue("display-name").orElse(event.getUser().getName()) : event.getUser().getName());
        Text message = new TranslatableText("text.subathon.event.cheer", name, event.getBits());
        if (Subathon.getConfigData().cumulativeBits) {
            if (Subathon.getConfigData().cumulativeIgnoreMin || event.getBits() >= Subathon.getConfigData().bitMin) {
                Subathon.integration.addBits(event.getBits());
            }
        } else if(event.getBits() >= Subathon.getConfigData().bitMin) {
            Subathon.integration.increaseValueFromBits(Subathon.getConfigData().onePerCheer ? 1 : Math.floorDiv(event.getBits(), Subathon.getConfigData().bitMin));
        }

        MessageUtils.sendEventMessage(message);

        {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(name);
            buf.writeInt(event.getBits());
            buf.writeEnumConstant(Subscription.PRIME);
            buf.writeEnumConstant(SubathonCommand.Events.CHEER);
            buf.writeString("");
            MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "event"), buf), "event_packet");
        }
    }
}
