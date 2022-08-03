package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.cubecontroller.CubeController;
import com.awakenedredstone.cubecontroller.GameControl;
import com.awakenedredstone.cubecontroller.util.MessageUtils;
import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.*;
import com.github.twitch4j.TwitchClientPool;
import com.github.twitch4j.TwitchClientPoolBuilder;
import com.github.twitch4j.chat.TwitchChatConnectionPool;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.awakenedredstone.subathon.Subathon.*;

public class TwitchIntegration {
    public final ScheduledExecutorService simpleExecutor = Executors.newScheduledThreadPool(3);
    private TwitchClientPool twitchClient;
    private TwitchChatConnectionPool chatPool;
    public InternalData data = new InternalData(0, 0, new TreeMap<>());
    public IntegrationStatus status = IntegrationStatus.OFFLINE;

    public void start() {
        if (getConfigData().channels.isEmpty()) {
            MessageUtils.sendError(Text.translatable("text.subathon.error.missing_channel_name"));
            simpleExecutor.execute(new ClearProgressBar());
            return;
        }

        if (twitchClient != null) {
            MessageUtils.sendError(Text.translatable("text.subathon.error.integration.online"));
            simpleExecutor.execute(new ClearProgressBar());
            return;
        }

        byte progress = 0;
        final byte steps = 5;
        mainProgressBar.setMaxValue(steps);

        mainProgressBar.setVisible(true);
        SubathonMessageUtils.updateIntegrationStatus(IntegrationStatus.STARTING);
        Utils.updateMainLoadProgress(mainProgressBar, "init", ++progress, steps);
        twitchClient = createBuilder(getConfigData().devMode).build();

        byte finalProgress = progress;
        simpleExecutor.execute(() -> init(twitchClient, finalProgress, steps));
    }

    public void stop() {
        stop(true);
    }

    public void stop(boolean notify) {
        simpleExecutor.execute(new ClearProgressBar());

        if (twitchClient != null) twitchClient.close();
        if (chatPool != null) chatPool.close();

        twitchClient = null;
        chatPool = null;

        SubathonMessageUtils.updateIntegrationStatus(IntegrationStatus.OFFLINE);

        if (notify) {
            MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, Text.translatable("text.subathon.integration.stop.title"), TitleS2CPacket::new), identifier("integration/stop_title"));
            MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, Text.translatable("text.subathon.integration.stop.subtitle"), SubtitleS2CPacket::new), identifier("integration/stop_subtitle"));

            //Plays the sound 2 times for a higher volume
            MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.VOICE, 100, 0.8f), identifier("integration/stop_sound"));
            MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.VOICE, 100, 0.8f), identifier("integration/stop_sound"));
        }
    }

    public void reload() {
        mainProgressBar.setMaxValue(4);
        if (status == IntegrationStatus.OFFLINE) {
            MessageUtils.sendError(Text.translatable("text.subathon.error.integration.reload.offline"));
            return;
        }

        if (getConfigData().channels.isEmpty()) {
            MessageUtils.sendError(Text.translatable("text.subathon.error.missing_channel_name"));
            simpleExecutor.execute(new ClearProgressBar());
            return;
        }

        SubathonMessageUtils.updateIntegrationStatus(IntegrationStatus.RELOADING);
        init(twitchClient, (byte) 0, (byte) 4);
    }

    private void init(TwitchClientPool client, byte progress, byte steps) {
        if (chatPool != null) chatPool.close();
        chatPool = TwitchChatConnectionPool.builder().maxSubscriptionsPerConnection(20).build();
        mainProgressBar.setMaxValue(steps);
        Utils.updateMainLoadProgress(mainProgressBar, "subscribe", ++progress, steps);
        //Subscribe events
        if (getConfigData().enableSubs) {
            //Register events for getting SpecificSubGiftEvent
            chatPool.getEventManager().onEvent(SubscriptionEvent.class, ProcessSubGift::onSubscription);
            chatPool.getEventManager().onEvent(GiftSubscriptionsEvent.class, ProcessSubGift::onGift);

            //Register main events
            chatPool.getEventManager().onEvent(SubscriptionEvent.class, eventListener::subscriptionListener);
            chatPool.getEventManager().onEvent(GiftSubscriptionsEvent.class, eventListener::giftListener);
            chatPool.getEventManager().onEvent(SpecificSubGiftEvent.class, eventListener::specificGiftListener);
        }
        if (getConfigData().enableBits)
            chatPool.getEventManager().onEvent(CheerEvent.class, eventListener::cheerListener);
        if (getConfigData().enableRewards)
            twitchClient.getPubSub().getEventManager().onEvent(RewardRedeemedEvent.class, eventListener::rewardListener);

        Utils.updateMainLoadProgress(mainProgressBar, "joining", ++progress, steps);
        new TwitchUtils().joinChannels(chatPool, client);

        Utils.updateMainLoadProgress(mainProgressBar, "broadcast", ++progress, steps);
        SubathonMessageUtils.updateIntegrationStatus(IntegrationStatus.RUNNING);
        SubathonMessageUtils.informTimers();

        SubathonMessageUtils.broadcastTitle(Text.translatable("text.subathon.integration.start.title"), TitleS2CPacket::new, identifier("integration/start_title"));
        SubathonMessageUtils.broadcastTitle(Text.translatable("text.subathon.integration.start.subtitle"), SubtitleS2CPacket::new, identifier("integration/start_subtitle"));

        MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.VOICE, 100, 0.9f), identifier("integration/start_sound"));

        Utils.updateMainLoadProgress(mainProgressBar, "complete", ++progress, steps);
        simpleExecutor.schedule(new ClearProgressBar(), 3, TimeUnit.SECONDS);
    }

    private TwitchClientPoolBuilder createBuilder(boolean devMode) {
        TwitchClientPoolBuilder builder = TwitchClientPoolBuilder.builder()
                .withEnableHelix(true)
                .withEnablePubSubPool(true)
                .withMaxTopicsPerPubSubInstance(15)
                .withEnableGraphQL(true);

        if (devMode) {
            return builder.withHelixBaseUrl(getConfigData().mockApiUrl);
        } else {
            return builder;
        }
    }

    public void addBits(int bits) {
        data.bits += bits;
        if (data.bits >= getConfigData().bitMin) {
            int toAdd = getConfigData().onePerCheer ? 1 : Math.floorDiv(data.bits, getConfigData().bitMin);
            increaseValue(toAdd * getConfigData().bitModifier);
            data.bits %= getConfigData().bitMin;
        }
    }

    public void addSubs(int subs) {
        data.subs += subs;
        if (data.subs >= getConfigData().subsPerIncrement) {
            increaseValue(getConfigData().onePerGift ? 1 : Math.floorDiv(data.subs, getConfigData().subsPerIncrement));
            data.subs %= getConfigData().subsPerIncrement;
        }
    }

    public void increaseValue(double amount) {
        CubeController.GAME_CONTROL.stream().filter(GameControl::enabled).forEach(control -> increaseValue(control, amount));
    }

    public void increaseValue(GameControl control, double amount) {
        double toAdd = control.valueBased() ? amount * getConfigData().scales.get(control.identifier().toString()) : 0;
        if (getConfigData().updateTimer > 0) {
            double queued = integration.data.nextValues.getOrDefault(control.identifier(), 0d);
            integration.data.nextValues.put(control.identifier(), queued + toAdd);
        } else {
            if (control.valueBased()) control.value(control.value() + toAdd);
            if (control.hasEvent() && Subathon.shouldInvoke(control.identifier())) for (int i = 0; i <amount; i++) control.invoke();
        }

        if (getConfigData().resetTimer > 0 && control.valueBased()) {
            ScheduleUtils.scheduleDelay(server, getConfigData().resetTimer, new ScheduleUtils.UpdateControlValue(control.identifier(), -toAdd));
        }
    }

    public TwitchClientPool getTwitchClient() {
        return this.twitchClient;
    }

    public TwitchChatConnectionPool getChatPool() {
        return this.chatPool;
    }

    /*TODO:UPDATE_THIS*/
    public static class ClearProgressBar implements Runnable {
        public void run() {
            mainProgressBar.setVisible(false);
            mainProgressBar.setName(Text.translatable("text.subathon.load.main", "", 0));
            mainProgressBar.setValue(0);

            usersProgressBar.setVisible(false);
            usersProgressBar.setName(Text.translatable("text.subathon.load.users", 0, 0));
            usersProgressBar.setMaxValue(1);
            usersProgressBar.setValue(0);
        }
    }
}
