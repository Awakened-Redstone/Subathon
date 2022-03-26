package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.util.MessageUtils;
import com.awakenedredstone.subathon.util.SubathonData;
import com.awakenedredstone.subathon.util.TwitchUtils;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.minecraft.text.TranslatableText;

import java.util.List;

import static com.awakenedredstone.subathon.Subathon.*;

public class TwitchIntegration implements Runnable {
    private final TwitchClientBuilder builder = TwitchClientBuilder.builder().withEnableHelix(true).withEnablePubSub(true);
    private TwitchClient twitchClient = null;
    public SubathonData data;
    public boolean isRunning = false;

    public TwitchIntegration() {}

    public void run() {}

    public void start(SubathonData data) {
        this.data = data;
        if (twitchClient != null) {
            MessageUtils.sendError(new TranslatableText("subathon.messages.integration.online"));
            return;
        }

        twitchClient = builder.build();

        JsonObject validate = TwitchUtils.validate(getAuthData().access_token);
        if (validate == null || (validate.has("status") && validate.get("status").getAsInt() != 200) || (validate.has("code") && validate.get("code").getAsInt() != 200)) {
            MessageUtils.sendError(new TranslatableText("subathon.messages.integration.fail_to_start"));
            return;
        }

        OAuth2Credential accessToken = getToken(GSON.fromJson(validate.get("scopes").getAsJsonArray(), new TypeToken<List<String>>() {}.getType()));
        if (accessToken == null) {
            MessageUtils.sendError(new TranslatableText("subathon.messages.integration.fail_to_start"));
            return;
        }

        if (getConfigData().enableSubs) {
            twitchClient.getPubSub().listenForSubscriptionEvents(accessToken, getAuthData().user_id);
            twitchClient.getPubSub().listenForChannelSubGiftsEvents(accessToken, getAuthData().user_id);
        }
        if (getConfigData().enableBits) {
            twitchClient.getPubSub().listenForCheerEvents(accessToken, getAuthData().user_id);
        }

        //Subscribe events
        twitchClient.getEventManager().onEvent(ChannelSubscribeEvent.class, eventListener::subscriptionListener);
        twitchClient.getEventManager().onEvent(ChannelSubGiftEvent.class, eventListener::giftListener);
        twitchClient.getEventManager().onEvent(ChannelBitsEvent.class, eventListener::cheerListener);

        isRunning = true;
    }

    public void stop() {
        if (twitchClient != null) twitchClient.close();
        twitchClient = null;
        isRunning = false;
    }

    private OAuth2Credential getToken(List<String> scopes) {
        if (getAuthData().access_token == null) return null;
        return new OAuth2Credential("twitch",
                getAuthData().access_token,
                "",
                getAuthData().user_id,
                getAuthData().login,
                0, scopes);
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
            data.subs %= getConfigData().subsPerIncrement;
            increaseValue(getConfigData().onePerGift ? 1 : Math.floorDiv(data.subs, getConfigData().subsPerIncrement));
        }
    }

    public void increaseValueFromBits(int amount) {
        increaseValue(amount * getConfigData().bitModifier);
    }

    public void increaseValue(int amount) {
        data.value += amount * getConfigData().effectAmplifier;
    }
}
