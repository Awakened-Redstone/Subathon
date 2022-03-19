package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.config.MessageMode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.core.domain.Event;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.awakenedredstone.subathon.Subathon.*;

public class Bot implements Runnable {
    public static TwitchClient twitchClient;
    private static float counter = 0;
    private OAuth2Credential accessToken;
    private static short subsUntilIncrement = getConfigData().subsPerIncrement;
    private static short bits = 0;

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

        if (getConfigData().enableSubs) {
            twitchClient.getPubSub().listenForSubscriptionEvents(accessToken, getAuthData().user_id);
            twitchClient.getPubSub().listenForChannelSubGiftsEvents(accessToken, getAuthData().user_id);
        }
        if (getConfigData().enableBits) {
            twitchClient.getPubSub().listenForCheerEvents(accessToken, getAuthData().user_id);
        }
        twitchClient.getEventManager().onEvent(GiftSubscriptionsEvent.class, this::executeAction);
        twitchClient.getEventManager().onEvent(SubscriptionEvent.class, this::executeAction);
        twitchClient.getEventManager().onEvent(CheerEvent.class, this::executeAction);

        server.getPlayerManager().getPlayerList().forEach(player -> player.sendMessage(new TranslatableText("subathon.started"), true));
    }

    public <T extends Event> void executeAction(T event) {
        Text message = null;

        String text = Float.toString(getConfigData().effectAmplifier);
        int integerPlaces = text.indexOf('.');
        int decimalPlaces = text.length() - integerPlaces - 1;
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(decimalPlaces);

        //Set the message to the corresponding event
        if (event instanceof SubscriptionEvent sub) {
            if (sub.getGifted()) return;
            String type = "";
            switch (sub.getSubPlan()) {
                case TWITCH_PRIME -> type = "prime";
                case TIER1 -> type = "tier1";
                case TIER2 -> type = "tier2";
                case TIER3 -> type = "tier3";
            }
            message = new TranslatableText("subathon.messages." + type, sub.getUser().getName());
            if (type.isEmpty()) {
                server.getPlayerManager().getPlayerList().forEach(player -> sendPositionedText(player, new TranslatableText("subathon.messages.error.fatal"), 12, 40, 0xFFFFFF, true, true, -1));
            } else subsUntilIncrement -= getConfigData().subModifiers.get(type);
        } else if (event instanceof GiftSubscriptionsEvent sub) {
            message = new TranslatableText("subathon.messages.gift", sub.getUser().getName(), sub.getCount());
            String type = "";
            switch (sub.getSubscriptionPlan()) {
                case "1000" -> type = "tier1";
                case "2000" -> type = "tier2";
                case "3000" -> type = "tier3";
            }
            if (type.isEmpty()) {
                server.getPlayerManager().getPlayerList().forEach(player -> sendPositionedText(player, new TranslatableText("subathon.messages.error.fatal"), 12, 40, 0xFFFFFF, true, true, -1));
            } else
                subsUntilIncrement -= getConfigData().subModifiers.get(type) * (Subathon.getConfigData().onePerGift ? 1 : sub.getCount());
        } else if (event instanceof CheerEvent cheer) {
            if (cheer.getBits() >= getConfigData().bitMin) {
                message = new TranslatableText("subathon.messages.cheer", cheer.getUser().getName(), cheer.getBits());
                if (getConfigData().onePerCheer) {
                    counter += getConfigData().bitModifier * getConfigData().effectAmplifier;
                } else if (getConfigData().cumulativeBits) {
                    bits += (short) ((int) cheer.getBits());
                    counter += ((short) Math.floor((float) bits / (float) getConfigData().bitMin) * getConfigData().bitModifier) * getConfigData().effectAmplifier;
                    bits %= getConfigData().bitMin;
                } else {
                    counter += ((short) Math.floor((float) cheer.getBits() / (float) getConfigData().bitMin) * getConfigData().bitModifier) * getConfigData().effectAmplifier;
                }
                server.getPlayerManager().getPlayerList().forEach(player -> sendPositionedText(player, new LiteralText(String.format("The effect amplifier is now %s", df.format(counter))), 12, 28, 0xFFFFFF, true, true, 1));
            } else if (getConfigData().cumulativeIgnoreMin && getConfigData().cumulativeBits) {
                message = new TranslatableText("subathon.messages.cheer", cheer.getUser().getName(), cheer.getBits());
                bits += cheer.getBits();
                if (bits >= getConfigData().bitMin) {
                    counter += ((short) Math.floor((float) bits / (float) getConfigData().bitMin) * getConfigData().bitModifier) * getConfigData().effectAmplifier;
                    bits %= getConfigData().bitMin;
                    server.getPlayerManager().getPlayerList().forEach(player -> sendPositionedText(player, new LiteralText(String.format("The effect amplifier is now %s", df.format(counter))), 12, 28, 0xFFFFFF, true, true, 1));
                }
            }
        }

        if (message == null) return;
        Text finalMessage = message;
        server.getPlayerManager().getPlayerList().forEach(player -> {
            switch (MessageMode.valueOf(getConfigData().messageMode)) {
                case CHAT -> player.sendMessage(finalMessage, false);
                case OVERLAY -> sendPositionedText(player, finalMessage, 12, 16, 0xFFFFFF, true, true, 0);
            }
        });

        //Check if it should increment the counter or not
        if (subsUntilIncrement > 0) return;
        else {
            while (subsUntilIncrement <= 0) {
                subsUntilIncrement += getConfigData().subsPerIncrement;
                counter += getConfigData().effectAmplifier;
            }
        }

        //Show a message to inform the player that the modifier has been increased
        //TODO: change the effect message, ex: "Your jump is now 1.2", "Your speed is now 0.2", etc.
        server.getPlayerManager().getPlayerList().forEach(player -> sendPositionedText(player, new LiteralText(String.format("The effect amplifier is now %s", df.format(counter))), 12, 28, 0xFFFFFF, true, true, 1));
    }

    private OAuth2Credential getToken() {
        if (getAuthData().access_token == null) throw new NullPointerException("access_token is missing!");
        return new OAuth2Credential("twitch",
                getAuthData().access_token,
                "",
                null, null,
                5184000,
                List.of("channel:read:subscriptions", "bits:read"));
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
                URLEncoder.encode("token", StandardCharsets.UTF_8),
                URLEncoder.encode("7scb6ymkzkne4uh5nuyg7nf7j5v213", StandardCharsets.UTF_8),
                URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8),
                scopes.stream().map(Object::toString).collect(Collectors.joining("%20")),
                URLEncoder.encode(state, StandardCharsets.UTF_8));
    }

    public Map<String, Object> validate() {
        try {
            OkHttpClient client = new OkHttpClient();
            ObjectMapper objectMapper = new ObjectMapper();
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse("https://id.twitch.tv/oauth2/validate")).newBuilder();

            Request request = new Request.Builder()
                    .url(urlBuilder.build().toString())
                    .header("Authorization", "Bearer " + accessToken.getAccessToken())
                    .build();

            Response response = client.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();

            return objectMapper.readValue(responseBody, new TypeReference<HashMap<String, Object>>() {
            });
        } catch (Exception e) {
            LOGGER.error("Failed to validate token!", e);
        }
        return null;
    }

    public static float getCounter() {
        return counter;
    }

    public static void setCounter(float value) {
        Bot.counter = value;
    }

    public static short getBits() {
        return bits;
    }

    public static void setBits(short value) {
        Bot.bits = value;
    }

    public static short getSubsUntilIncrement() {
        return subsUntilIncrement;
    }

    public static void setSubsUntilIncrement(short value) {
        Bot.subsUntilIncrement = value;
    }

}
