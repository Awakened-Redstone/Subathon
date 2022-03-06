package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.Subathon;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.core.domain.Event;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.helix.domain.User;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.awakenedredstone.subathon.Subathon.*;

public class Bot implements Runnable {
    public static TwitchClient twitchClient;
    private static float counter = 0;

    @Override
    public void run() {
        twitchClient = TwitchClientBuilder.builder().withEnableHelix(true).withEnablePubSub(true).build();
        TwitchIdentityProvider identity = new TwitchIdentityProvider(getConfigData().clientId, getConfigData().clientSecret, "https://twitch.tv/");
        OAuth2Credential token = identity.getAppAccessToken();
        OAuth2Credential accessToken = new OAuth2Credential("subathon", token.getAccessToken(), token.getRefreshToken(), null, null, token.getExpiresIn(), List.of("channel_subscriptions"));
        List<User> users = twitchClient.getHelix().getUsers(accessToken.getAccessToken(), null, List.of(getConfigData().channelName)).execute().getUsers();
        if (users.isEmpty()) {
            LOGGER.error(String.format("Could not find user \"%s\"! Cancelling bot initialization!", getConfigData().channelName));
            twitchClient.close();
            return;
        }
        twitchClient.getPubSub().listenForSubscriptionEvents(accessToken, users.get(0).getId());
        twitchClient.getPubSub().listenForChannelSubGiftsEvents(accessToken, users.get(0).getId());
        twitchClient.getEventManager().onEvent(GiftSubscriptionsEvent.class, this::executeAction);
        twitchClient.getEventManager().onEvent(SubscriptionEvent.class, this::executeAction);
    }

    public <T extends Event> void executeAction(T event) {
        Text message = new LiteralText("Error! Failed to set sub message!");
        if (event instanceof SubscriptionEvent sub) {
            if (sub.getGifted()) return;
            if (sub.getSubPlan() == SubscriptionPlan.TWITCH_PRIME) {
                message = new TranslatableText("subathon.messages.prime", sub.getUser().getName());
            } else {
                message = new TranslatableText("subathon.messages.default", sub.getUser().getName());
            }
        } else if (event instanceof GiftSubscriptionsEvent sub) {
            message = new TranslatableText("subathon.messages.gift", sub.getUser().getName(), sub.getCount());
        }
        counter += Subathon.getConfigData().effectAmplifier;
        Text finalMessage = message;
        server.getPlayerManager().getPlayerList().forEach(player -> positionedText(player, finalMessage, 12, 16, 0xFFFFFF, true, true, 0));
        server.getPlayerManager().getPlayerList().forEach(player -> positionedText(player, new LiteralText(String.format("The effect amplifier is now %s", counter)), 12, 28, 0xFFFFFF, true, true, 1));
        //TODO: change the effect message, ex: "Your jump is now 1.2", "Your speed is now 0.2", etc.
    }

    public static float getCounter() {
        return counter;
    }

    public static void setCounter(int counter) {
        Bot.counter = counter;
    }

    public void positionedText(ServerPlayerEntity player, Text text, int x, int y, int color, boolean center, boolean shadow) {
        positionedText(player, text, x, y, color, center, shadow, new Random().nextLong());
    }

    public void positionedText(ServerPlayerEntity player, Text text, int x, int y, int color, boolean center, boolean shadow, long id) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeText(text);
        buf.writeBoolean(shadow);
        buf.writeBoolean(center);
        int[] values = new int[]{x, y, color};
        buf.writeIntArray(values);
        buf.writeLong(id);
        ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "positioned_text"), buf);
    }
}
