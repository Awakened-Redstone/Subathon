package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.core.domain.Event;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.helix.domain.User;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import okhttp3.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.awakenedredstone.subathon.Subathon.*;

public class Bot implements Runnable {
    public static TwitchClient twitchClient;
    private static float counter = 0;
    private OAuth2Credential accessToken;
    private static int subsUntilIncrement = getConfigData().subModifier;

    @Override
    public void run() {
        twitchClient = TwitchClientBuilder.builder().withEnableHelix(true).withEnablePubSub(true).build();
        try {
            accessToken = getToken();
        } catch (Exception exception) {
            LOGGER.error("Failed to get the accessToken! Cancelling bot initialization!", exception);
            server.getCommandSource().sendFeedback(new LiteralText("Could not start the bot!"), true);
            try {
                SubathonCommand.execute(server.getCommandSource(), false);
            } catch (CommandSyntaxException e) {
                LOGGER.error("An error occurred when trying to execute \"/subathon stop\"", e);
            }
            twitchClient.close();
            return;
        }

        twitchClient.getPubSub().listenForSubscriptionEvents(accessToken, getAuthData().user_id);
        twitchClient.getPubSub().listenForChannelSubGiftsEvents(accessToken, getAuthData().user_id);
        twitchClient.getEventManager().onEvent(GiftSubscriptionsEvent.class, this::executeAction);
        twitchClient.getEventManager().onEvent(SubscriptionEvent.class, this::executeAction);

        server.getPlayerManager().getPlayerList().forEach(player -> {
            player.sendMessage(new TranslatableText("subathon.started"), true);
        });
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
            subsUntilIncrement -= 1;
        } else if (event instanceof GiftSubscriptionsEvent sub) {
            message = new TranslatableText("subathon.messages.gift", sub.getUser().getName(), sub.getCount());
            subsUntilIncrement -= sub.getCount();
        }
        if (subsUntilIncrement > 0) return;
        else subsUntilIncrement = getConfigData().subModifier;
        counter += Subathon.getConfigData().effectAmplifier;
        Text finalMessage = message;
        server.getPlayerManager().getPlayerList().forEach(player -> positionedText(player, finalMessage, 12, 16, 0xFFFFFF, true, true, 0));
        server.getPlayerManager().getPlayerList().forEach(player -> positionedText(player, new LiteralText(String.format("The effect amplifier is now %s", counter)), 12, 28, 0xFFFFFF, true, true, 1));
        //TODO: change the effect message, ex: "Your jump is now 1.2", "Your speed is now 0.2", etc.
    }

    private OAuth2Credential getToken() {
        if (getAuthData().access_token == null) throw new NullPointerException("access_token is missing!");
        if (getAuthData().refresh_token == null) throw new NullPointerException("refresh_token is missing!");
        return new OAuth2Credential("twitch",
                getAuthData().access_token,
                getAuthData().refresh_token,
                null, null,
                getAuthData().expires_in,
                List.of("channel:read:subscriptions"));
    }

    /**
     * Get Authentication Url
     *
     * @param scopes requested scopes
     * @param state  state - csrf protection
     * @return url
     */
    public static String getAuthenticationUrl(List<Object> scopes, String state) {
        return getAuthenticationUrl("https://subathon-mod.glitch.me", scopes, state);
    }

    /**
     * Get Authentication Url
     *
     * @param redirectUrl overwrite the redirect url with a custom one
     * @param scopes      requested scopes
     * @param state       state - csrf protection
     * @return url
     */
    public static String getAuthenticationUrl(String redirectUrl, List<Object> scopes, String state) {
        if (state == null) {
            state = "twitch|" + UUID.randomUUID();
        }
        return String.format("%s?response_type=%s&client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                "https://id.twitch.tv/oauth2/authorize",
                URLEncoder.encode("code", StandardCharsets.UTF_8),
                URLEncoder.encode("7scb6ymkzkne4uh5nuyg7nf7j5v213", StandardCharsets.UTF_8),
                URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8),
                scopes.stream().map(Object::toString).collect(Collectors.joining(" ")),
                URLEncoder.encode(state, StandardCharsets.UTF_8));
    }

    public Map<String, Object> validate() {
        try {
            OkHttpClient client = new OkHttpClient();
            ObjectMapper objectMapper = new ObjectMapper();
            HttpUrl.Builder urlBuilder = HttpUrl.parse("https://id.twitch.tv/oauth2/validate").newBuilder();

            Request request = new Request.Builder()
                    .url(urlBuilder.build().toString())
                    .header("Authorization", "Bearer " + accessToken.getAccessToken())
                    .build();

            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            if (response.isSuccessful()) {
                Map<String, Object> resultMap = objectMapper.readValue(responseBody, new TypeReference<HashMap<String, Object>>() {});

                return resultMap;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to validate token!", e);
        }
        return null;
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
