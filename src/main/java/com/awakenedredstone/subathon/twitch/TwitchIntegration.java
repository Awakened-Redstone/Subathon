package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.MessageUtils;
import com.awakenedredstone.subathon.util.ProcessSubGift;
import com.awakenedredstone.subathon.util.SubathonData;
import com.awakenedredstone.subathon.util.TwitchUtils;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.apache.commons.lang.StringUtils;

import static com.awakenedredstone.subathon.Subathon.eventListener;
import static com.awakenedredstone.subathon.Subathon.getConfigData;

public class TwitchIntegration {
    private TwitchClient twitchClient = null;
    public SubathonData data;
    public boolean isRunning = false;

    public TwitchIntegration() {}

    public void start(SubathonData data) {
        if (StringUtils.isBlank(getConfigData().channelName)) {
            MessageUtils.sendError(new TranslatableText("subathon.messages.error.missing_channel_name"));
            return;
        }

        this.data = data;
        if (twitchClient != null) {
            MessageUtils.sendError(new TranslatableText("subathon.messages.integration.online"));
            return;
        }

        TwitchClientBuilder builder = TwitchClientBuilder.builder().withEnableHelix(true).withEnableChat(true);
        twitchClient = builder.build();

        JsonObject channelData = TwitchUtils.getChannelData();
        if (channelData == null) {
            MessageUtils.sendError(new TranslatableText("subathon.messages.error.invalid_channel_name"));
            stop(false);
            return;
        }
        getConfigData().channelId = channelData.get("channelId").getAsString();
        getConfigData().channelDisplayName = channelData.get("displayName").getAsString();

        twitchClient.getChat().joinChannel(getConfigData().channelName);

        //Subscribe events
        if (getConfigData().enableSubs) {
            //Register events for getting SpecificSubGiftEvent
            twitchClient.getEventManager().onEvent(SubscriptionEvent.class, ProcessSubGift::onSubscription);
            twitchClient.getEventManager().onEvent(GiftSubscriptionsEvent.class, ProcessSubGift::onGift);

            //Register main events
            twitchClient.getEventManager().onEvent(SubscriptionEvent.class, eventListener::subscriptionListener);
            twitchClient.getEventManager().onEvent(GiftSubscriptionsEvent.class, eventListener::giftListener);
            twitchClient.getEventManager().onEvent(SpecificSubGiftEvent.class, eventListener::specificGiftListener);
        }
        if (getConfigData().enableBits) twitchClient.getChat().getEventManager().onEvent(CheerEvent.class, eventListener::cheerListener);

        isRunning = true;

        {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(1);
            MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "bot_status"), buf), "bot_status");
        }
        {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeFloat(data.value / getConfigData().effectMultiplier);
            MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "value"), buf), "value");
        }
        MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, new TranslatableText("subathon.messages.start.title"), TitleS2CPacket::new), "start_title");
        MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, new TranslatableText("subathon.messages.start.subtitle"), SubtitleS2CPacket::new), "start_subtitle");

        //Plays the sound 3 times for a higher volume
        MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.VOICE, 100, 0.9f), "start_sound");
        MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.VOICE, 100, 0.9f), "start_sound");
        MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.VOICE, 100, 0.9f), "start_sound");
    }

    public void stop() {
        stop(true);
    }

    public void stop(boolean notify) {
        if (twitchClient != null) twitchClient.close();
        twitchClient = null;
        isRunning = false;

        if (notify) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(0);
            MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "bot_status"), buf), "bot_status");
            MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, new TranslatableText("subathon.messages.stop.title"), TitleS2CPacket::new), "stop_title");
            MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, new TranslatableText("subathon.messages.stop.subtitle"), SubtitleS2CPacket::new), "stop_subtitle");

            //Plays the sound 2 times for a higher volume
            MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.VOICE, 100, 0.8f), "stop_sound");
            MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.VOICE, 100, 0.8f), "stop_sound");
        }
    }

    public void addBits(int bits) {
        data.bits += bits;
        if (data.bits >= getConfigData().bitMin) {
            data.bits %= getConfigData().bitMin;
            increaseValueFromBits(getConfigData().onePerCheer ? 1 : Math.floorDiv(data.bits, getConfigData().bitMin));
        }
    }

    public void addSubs(int subs) {
        data.subs += subs;
        if (data.subs >= getConfigData().subsPerIncrement) {
            increaseValue(getConfigData().onePerGift ? 1 : Math.floorDiv(data.subs, getConfigData().subsPerIncrement));
            data.subs %= getConfigData().subsPerIncrement;
        }
    }

    public void increaseValueFromBits(int amount) {
        increaseValue(amount * getConfigData().bitModifier);
    }

    public void increaseValue(float amount) {
        data.value += amount * (getConfigData().effectMultiplier * getConfigData().effectIncrement);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(data.value / getConfigData().effectMultiplier);
        MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "value"), buf), "value");
    }

    public void setValue(float amount) {
        data.value = amount;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(data.value / getConfigData().effectMultiplier);
        MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "value"), buf), "value");
    }

    public TwitchClient getTwitchClient() {
        return this.twitchClient;
    }
}
