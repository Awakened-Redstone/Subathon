package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.BotStatus;
import com.awakenedredstone.subathon.util.MessageUtils;
import com.awakenedredstone.subathon.util.ProcessSubGift;
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
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.logging.UncaughtExceptionHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static com.awakenedredstone.subathon.Subathon.*;

public class TwitchIntegration {
    public final ScheduledExecutorService simpleExecutor = Executors.newScheduledThreadPool(1);
    public ScheduledExecutorService executor = Executors.newScheduledThreadPool(6);
    public Collection<Future<?>> runningThreads = new ArrayList<>();
    public final BlockingQueue<Thread> queue = new LinkedBlockingQueue<>();
    private TwitchClient twitchClient = null;
    public SubathonData data;
    public boolean isRunning = false;
    private Thread initThread = null;

    public TwitchIntegration() {
    }

    public void start(SubathonData data) {
        {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeEnumConstant(BotStatus.STARTING);
            MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "bot_status"), buf), "bot_status");
        }

        mainProgressBar.setVisible(true);
        mainProgressBar.setValue(1);
        mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.pre_init"), 1));

        this.data = data;

        if (getConfigData().channels.isEmpty()) {
            MessageUtils.sendError(new TranslatableText("text.subathon.error.missing_channel_name"));
            simpleExecutor.execute(new ClearProgressBar());
            return;
        }

        if (twitchClient != null) {
            MessageUtils.sendError(new TranslatableText("text.subathon.error.integration.online"));
            simpleExecutor.execute(new ClearProgressBar());
            return;
        }

        mainProgressBar.setValue(2);
        mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.init"), 2));
        TwitchClientBuilder builder = TwitchClientBuilder.builder().withEnableHelix(true).withEnableChat(true);
        twitchClient = builder.build();

        mainProgressBar.setValue(3);
        mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.subscribe"), 3));
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
        if (getConfigData().enableBits)
            twitchClient.getChat().getEventManager().onEvent(CheerEvent.class, eventListener::cheerListener);

        mainProgressBar.setValue(4);
        mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.joining"), 4));

        final byte[] joinedChannels = {0};
        int totalChannels = getConfigData().channels.size();
        getConfigData().channelIds = new ArrayList<>(Collections.nCopies(totalChannels, ""));
        getConfigData().channelDisplayNames = new ArrayList<>(Collections.nCopies(totalChannels, ""));

        usersProgressBar.setMaxValue(getConfigData().channels.size());
        usersProgressBar.setValue(0);
        usersProgressBar.setName(new TranslatableText("text.subathon.load.users", 0, totalChannels));
        usersProgressBar.setVisible(true);

        //TODO: Use async IO
        initThread = new Thread(() -> {
            final boolean[] failed = {false};
            IntStream.range(0, totalChannels).forEach(index -> {
                String channelName = getConfigData().channels.get(index);
                Thread thread = new Thread(() -> {
                    JsonObject channelData = TwitchUtils.getChannelData(channelName);
                    if (channelData == null) {
                        MessageUtils.sendError(new TranslatableText("text.subathon.error.invalid_channel_name", channelName));
                    } else {
                        getConfigData().channelIds.set(index, channelData.get("channelId").getAsString());
                        getConfigData().channelDisplayNames.set(index, channelData.get("displayName").getAsString());
                        twitchClient.getChat().joinChannel(channelName);
                    }

                    usersProgressBar.setName(new TranslatableText("text.subathon.load.users", ++joinedChannels[0], totalChannels));
                    usersProgressBar.setValue(joinedChannels[0]);
                });

                thread.setName(String.format("Channel data query [%s]", channelName));
                thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(Subathon.LOGGER));
                thread.setDaemon(true);

                try {
                    queue.put(thread);
                } catch (InterruptedException e) {
                    MessageUtils.sendError(new TranslatableText("text.subathon.error.fatal"), e);
                    simpleExecutor.execute(new ClearProgressBar());
                    stop(false);
                    failed[0] = true;
                }
            });

            while (!queue.isEmpty()) {
                try {
                    runningThreads.add(executor.submit(queue.take()));
                } catch (Exception e) {
                    MessageUtils.sendError(new LiteralText("Failed to start integration!"), e);
                    simpleExecutor.execute(new ClearProgressBar());
                    clearRunningThreads();
                    stop(false);
                    return;
                }
            }

            try {
                for (Future<?> thread : runningThreads) {
                    thread.get();
                }
            } catch (Exception e) {
                MessageUtils.sendError(new LiteralText("Failed to start integration!"), e);
                simpleExecutor.execute(new ClearProgressBar());
                clearRunningThreads();
                return;
            }

            if (failed[0]) {
                return;
            }

            usersProgressBar.setVisible(false);
            mainProgressBar.setValue(5);
            mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.broadcast"), 5));
            isRunning = true;

            {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeEnumConstant(BotStatus.RUNNING);
                MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "bot_status"), buf), "bot_status");
            }
            {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeDouble(getDisplayValue());
                MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "value"), buf), "value");
            }
            MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, new TranslatableText("text.subathon.integration.start.title"), TitleS2CPacket::new), "start_title");
            MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, new TranslatableText("text.subathon.integration.start.subtitle"), SubtitleS2CPacket::new), "start_subtitle");

            //Plays the sound 3 times for a higher volume
            MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.VOICE, 100, 0.9f), "start_sound");
            MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.VOICE, 100, 0.9f), "start_sound");
            MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.VOICE, 100, 0.9f), "start_sound");

            mainProgressBar.setValue(6);
            mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.complete"), 6));

            simpleExecutor.schedule(new ClearProgressBar(), 3, TimeUnit.SECONDS);
        });

        initThread.setName("Twitch integration startup");
        initThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(Subathon.LOGGER));
        initThread.setDaemon(true);
        initThread.start();
    }

    public void stop() {
        stop(true);
    }

    public void stop(boolean notify) {
        if (initThread != null && !initThread.isInterrupted()) initThread.interrupt();
        while (!queue.isEmpty()) {
            queue.remove();
        }
        clearRunningThreads();
        simpleExecutor.execute(new ClearProgressBar());
        if (twitchClient != null) {
            twitchClient.getChat().getChannels().forEach(twitchClient.getChat()::leaveChannel);
            twitchClient.close();
        }
        twitchClient = null;
        isRunning = false;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(BotStatus.OFFLINE);
        MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "bot_status"), buf), "bot_status");

        if (notify) {
            MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, new TranslatableText("text.subathon.integration.stop.title"), TitleS2CPacket::new), "stop_title");
            MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, new TranslatableText("text.subathon.integration.stop.subtitle"), SubtitleS2CPacket::new), "stop_subtitle");

            //Plays the sound 2 times for a higher volume
            MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.VOICE, 100, 0.8f), "stop_sound");
            MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.VOICE, 100, 0.8f), "stop_sound");
        }
    }

    public void reload() {
        if (!isRunning) {
            MessageUtils.sendError(new LiteralText("Cannot reload while the integration isn't running!"));
        }

        {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeEnumConstant(BotStatus.RELOADING);
            MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "bot_status"), buf), "bot_status");
        }

        final byte[] joinedChannels = {0};
        int totalChannels = getConfigData().channels.size();

        getConfigData().channelIds = new ArrayList<>(Collections.nCopies(totalChannels, ""));
        getConfigData().channelDisplayNames = new ArrayList<>(Collections.nCopies(totalChannels, ""));

        usersProgressBar.setMaxValue(getConfigData().channels.size());
        usersProgressBar.setValue(0);
        usersProgressBar.setName(new TranslatableText("text.subathon.load.users", 0, totalChannels));
        usersProgressBar.setVisible(true);

        initThread = new Thread(() -> {
            twitchClient.getChat().getChannels().forEach(twitchClient.getChat()::leaveChannel);
            final boolean[] failed = {false};
            IntStream.range(0, totalChannels).forEach(index -> {
                String channelName = getConfigData().channels.get(index);
                Thread thread = new Thread(() -> {
                    JsonObject channelData = TwitchUtils.getChannelData(channelName);
                    if (channelData == null) {
                        MessageUtils.sendError(new TranslatableText("text.subathon.error.invalid_channel_name", channelName));
                    } else {
                        getConfigData().channelIds.set(index, channelData.get("channelId").getAsString());
                        getConfigData().channelDisplayNames.set(index, channelData.get("displayName").getAsString());
                        twitchClient.getChat().joinChannel(channelName);
                    }

                    usersProgressBar.setName(new TranslatableText("text.subathon.load.users", ++joinedChannels[0], totalChannels));
                    usersProgressBar.setValue(joinedChannels[0]);
                });

                thread.setName(String.format("Channel data query [%s]", channelName));
                thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(Subathon.LOGGER));
                thread.setDaemon(true);

                try {
                    queue.put(thread);
                } catch (InterruptedException e) {
                    MessageUtils.sendError(new TranslatableText("text.subathon.error.fatal"), e);
                    simpleExecutor.execute(new ClearProgressBar());
                    stop(false);
                    failed[0] = true;
                }
            });

            while (!queue.isEmpty()) {
                try {
                    runningThreads.add(executor.submit(queue.take()));
                } catch (Exception e) {
                    MessageUtils.sendError(new LiteralText("Failed to start integration!"), e);
                    simpleExecutor.execute(new ClearProgressBar());
                    clearRunningThreads();
                    stop(false);
                    return;
                }
            }

            try {
                for (Future<?> thread : runningThreads) {
                    thread.get();
                }
            } catch (Exception e) {
                MessageUtils.sendError(new LiteralText("Failed to start integration!"), e);
                simpleExecutor.execute(new ClearProgressBar());
                clearRunningThreads();
                return;
            }

            if (failed[0]) {
                return;
            }

            {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeEnumConstant(BotStatus.RUNNING);
                MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "bot_status"), buf), "bot_status");
            }

            simpleExecutor.schedule(() -> {
                usersProgressBar.setVisible(false);
                usersProgressBar.setName(new TranslatableText("text.subathon.load.users", 0, 0));
                usersProgressBar.setMaxValue(1);
                usersProgressBar.setValue(0);
            }, 100, TimeUnit.MILLISECONDS);
        });

        initThread.setName("Twitch integration reload");
        initThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(Subathon.LOGGER));
        initThread.setDaemon(true);
        initThread.start();
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
        buf.writeDouble(getDisplayValue());
        MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "value"), buf), "value");
    }

    public void setValue(float amount) {
        data.value = amount;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(getDisplayValue());
        MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "value"), buf), "value");
    }

    public double getDisplayValue() {
        return integration.data.value / getConfigData().effectMultiplier;
    }

    public TwitchClient getTwitchClient() {
        return this.twitchClient;
    }

    private void clearRunningThreads() {
        runningThreads.removeIf(future -> {
            future.cancel(true);
            return true;
        });
    }

    public static class ClearProgressBar implements Runnable {
        public void run() {
            mainProgressBar.setVisible(false);
            mainProgressBar.setName(new TranslatableText("text.subathon.load.main", "", 0));
            mainProgressBar.setValue(0);

            usersProgressBar.setVisible(false);
            usersProgressBar.setName(new TranslatableText("text.subathon.load.users", 0, 0));
            usersProgressBar.setMaxValue(1);
            usersProgressBar.setValue(0);
        }
    }
}
