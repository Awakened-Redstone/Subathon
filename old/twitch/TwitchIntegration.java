package old.twitch;

import old.Subathon;
import old.json.JsonHelper;
import old.util.BotStatus;
import old.util.MessageUtils;
import old.util.ProcessSubGift;
import old.util.TwitchUtils;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
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
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.logging.UncaughtExceptionHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class TwitchIntegration {
    public final ScheduledExecutorService simpleExecutor = Executors.newScheduledThreadPool(1);
    public ScheduledExecutorService executor = Executors.newScheduledThreadPool(6);
    public Collection<Future<?>> runningThreads = new ArrayList<>();
    public final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private TwitchClient twitchClient = null;
    public SubathonData data;
    public boolean isRunning = false;
    private Thread initThread = null;
    private final int timeout = 30000;

    public TwitchIntegration() {}

    public void start(SubathonData data) {
        {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeEnumConstant(BotStatus.STARTING);
            MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "bot_status"), buf), "bot_status");
        }

        Subathon.mainProgressBar.setVisible(true);
        Subathon.mainProgressBar.setValue(1);
        Subathon.mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.pre_init"), 1));

        this.data = data;

        if (Subathon.getConfigData().channels.isEmpty()) {
            MessageUtils.sendError(new TranslatableText("text.subathon.error.missing_channel_name"));
            simpleExecutor.execute(new ClearProgressBar());
            return;
        }

        if (twitchClient != null) {
            MessageUtils.sendError(new TranslatableText("text.subathon.error.integration.online"));
            simpleExecutor.execute(new ClearProgressBar());
            return;
        }

        Subathon.mainProgressBar.setValue(2);
        Subathon.mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.init"), 2));
        TwitchClientBuilder builder = TwitchClientBuilder.builder().withEnableHelix(true).withEnableChat(true);
        twitchClient = builder.build();

        Subathon.mainProgressBar.setValue(3);
        Subathon.mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.subscribe"), 3));
        //Subscribe events
        if (Subathon.getConfigData().enableSubs) {
            //Register events for getting SpecificSubGiftEvent
            twitchClient.getEventManager().onEvent(SubscriptionEvent.class, ProcessSubGift::onSubscription);
            twitchClient.getEventManager().onEvent(GiftSubscriptionsEvent.class, ProcessSubGift::onGift);

            //Register main events
            twitchClient.getEventManager().onEvent(SubscriptionEvent.class, Subathon.eventListener::subscriptionListener);
            twitchClient.getEventManager().onEvent(GiftSubscriptionsEvent.class, Subathon.eventListener::giftListener);
            twitchClient.getEventManager().onEvent(SpecificSubGiftEvent.class, Subathon.eventListener::specificGiftListener);
        }
        if (Subathon.getConfigData().enableBits)
            twitchClient.getChat().getEventManager().onEvent(CheerEvent.class, Subathon.eventListener::cheerListener);

        Subathon.mainProgressBar.setValue(4);
        Subathon.mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.joining"), 4));

        final byte[] joinedChannels = {0};
        int totalChannels = Subathon.getConfigData().channels.size();
        Subathon.getConfigData().channelIds = new ArrayList<>(Collections.nCopies(totalChannels, ""));
        Subathon.getConfigData().channelDisplayNames = new ArrayList<>(Collections.nCopies(totalChannels, ""));

        Subathon.usersProgressBar.setMaxValue(Subathon.getConfigData().channels.size());
        Subathon.usersProgressBar.setValue(0);
        Subathon.usersProgressBar.setName(new TranslatableText("text.subathon.load.users", 0, totalChannels));
        Subathon.usersProgressBar.setVisible(true);

        initThread = new Thread(() -> {
            Object lock = new Object();
            synchronized (lock) {
                IntStream.range(0, totalChannels).forEach(index -> {
                    String channelName = Subathon.getConfigData().channels.get(index);
                    TwitchUtils.getChannelData(channelName, channelData -> {
                        if (channelData == null || channelData.get("channelId").isJsonNull() || channelData.get("displayName").isJsonNull()) {
                            MessageUtils.sendError(new TranslatableText("text.subathon.error.invalid_channel_name", channelName));
                        } else {
                            Subathon.getConfigData().channelIds.set(index, channelData.get("channelId").getAsString());
                            Subathon.getConfigData().channelDisplayNames.set(index, channelData.get("displayName").getAsString());
                            twitchClient.getChat().joinChannel(channelName);
                        }

                        Subathon.usersProgressBar.setName(new TranslatableText("text.subathon.load.users", ++joinedChannels[0], totalChannels));
                        Subathon.usersProgressBar.setValue(joinedChannels[0]);

                        if (joinedChannels[0] == totalChannels) {
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    });
                });

                try {
                    lock.wait(timeout);
                    if (joinedChannels[0] != totalChannels) {
                        MessageUtils.sendError(new LiteralText("Failed to start integration!"));
                        simpleExecutor.execute(new ClearProgressBar());
                        Subathon.OKHTTPCLIENT.dispatcher().cancelAll();
                        return;
                    }
                } catch (Exception e) {
                    MessageUtils.sendError(new LiteralText("Failed to start integration!"), e);
                    simpleExecutor.execute(new ClearProgressBar());
                    Subathon.OKHTTPCLIENT.dispatcher().cancelAll();
                    return;
                }

                Subathon.usersProgressBar.setVisible(false);
                Subathon.mainProgressBar.setValue(5);
                Subathon.mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.broadcast"), 5));
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

                {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeInt(Subathon.getConfigData().resetTimer);
                    buf.writeInt(Subathon.getConfigData().updateTimer);
                    MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "timers"), buf), "timers");
                }

                MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, new TranslatableText("text.subathon.integration.start.title"), TitleS2CPacket::new), "start_title");
                MessageUtils.broadcast(player -> MessageUtils.sendTitle(player, new TranslatableText("text.subathon.integration.start.subtitle"), SubtitleS2CPacket::new), "start_subtitle");

                //Plays the sound 3 times for a higher volume
                MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.VOICE, 100, 0.9f), "start_sound");
                MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.VOICE, 100, 0.9f), "start_sound");
                MessageUtils.broadcast(player -> player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.VOICE, 100, 0.9f), "start_sound");

                Subathon.mainProgressBar.setValue(6);
                Subathon.mainProgressBar.setName(new TranslatableText("text.subathon.load.main", new TranslatableText("text.subathon.load.stage.complete"), 6));

                simpleExecutor.schedule(new ClearProgressBar(), 3, TimeUnit.SECONDS);
            }
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
            MessageUtils.sendError(new TranslatableText("text.subathon.error.integration.reload.offline"));
        }

        {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeEnumConstant(BotStatus.RELOADING);
            MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "bot_status"), buf), "bot_status");
        }

        File file = Subathon.server.getSavePath(WorldSavePath.ROOT).resolve("subathon_data.json").toFile();
        if (!file.exists()) JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(new SubathonData()).getAsJsonObject(), file);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            data = Subathon.GSON.fromJson(reader, SubathonData.class);
        } catch (IOException e) {
            MessageUtils.sendError(new LiteralText("Failed to start the integration!"));
        }

        final byte[] joinedChannels = {0};
        int totalChannels = Subathon.getConfigData().channels.size();

        Subathon.getConfigData().channelIds = new ArrayList<>(Collections.nCopies(totalChannels, ""));
        Subathon.getConfigData().channelDisplayNames = new ArrayList<>(Collections.nCopies(totalChannels, ""));

        Subathon.usersProgressBar.setMaxValue(Subathon.getConfigData().channels.size());
        Subathon.usersProgressBar.setValue(0);
        Subathon.usersProgressBar.setName(new TranslatableText("text.subathon.load.users", 0, totalChannels));
        Subathon.usersProgressBar.setVisible(true);

        initThread = new Thread(() -> {
            twitchClient.getChat().getChannels().forEach(twitchClient.getChat()::leaveChannel);
            Object lock = new Object();
            synchronized (lock) {
                IntStream.range(0, totalChannels).forEach(index -> {
                    String channelName = Subathon.getConfigData().channels.get(index);
                    TwitchUtils.getChannelData(channelName, channelData -> {
                        if (channelData == null || channelData.get("channelId").isJsonNull() || channelData.get("displayName").isJsonNull()) {
                            MessageUtils.sendError(new TranslatableText("text.subathon.error.invalid_channel_name", channelName));
                        } else {
                            Subathon.getConfigData().channelIds.set(index, channelData.get("channelId").getAsString());
                            Subathon.getConfigData().channelDisplayNames.set(index, channelData.get("displayName").getAsString());
                            twitchClient.getChat().joinChannel(channelName);
                        }

                        Subathon.usersProgressBar.setName(new TranslatableText("text.subathon.load.users", ++joinedChannels[0], totalChannels));
                        Subathon.usersProgressBar.setValue(joinedChannels[0]);

                        if (joinedChannels[0] == totalChannels) {
                            synchronized (lock) {
                                lock.notify();
                            }
                        }
                    });
                });

                try {
                    lock.wait(timeout);
                    if (joinedChannels[0] != totalChannels) {
                        MessageUtils.sendError(new LiteralText("Failed to start integration!"));
                        simpleExecutor.execute(new ClearProgressBar());
                        Subathon.OKHTTPCLIENT.dispatcher().cancelAll();
                        return;
                    }
                } catch (Exception e) {
                    MessageUtils.sendError(new LiteralText("Failed to start integration!"), e);
                    simpleExecutor.execute(new ClearProgressBar());
                    Subathon.OKHTTPCLIENT.dispatcher().cancelAll();
                    return;
                }

                {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeEnumConstant(BotStatus.RUNNING);
                    MessageUtils.broadcastToOps(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "bot_status"), buf), "bot_status");
                }

                {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeInt(Subathon.getConfigData().resetTimer);
                    buf.writeInt(Subathon.getConfigData().updateTimer);
                    MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "timers"), buf), "timers");
                }

                simpleExecutor.schedule(() -> {
                    Subathon.usersProgressBar.setVisible(false);
                    Subathon.usersProgressBar.setName(new TranslatableText("text.subathon.load.users", 0, 0));
                    Subathon.usersProgressBar.setMaxValue(1);
                    Subathon.usersProgressBar.setValue(0);
                }, 100, TimeUnit.MILLISECONDS);
            }
        });

        initThread.setName("Twitch integration reload");
        initThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(Subathon.LOGGER));
        initThread.setDaemon(true);
        initThread.start();
    }

    public void addBits(int bits) {
        data.bits += bits;
        if (data.bits >= Subathon.getConfigData().bitMin) {
            data.bits %= Subathon.getConfigData().bitMin;
            increaseValueFromBits(Subathon.getConfigData().onePerCheer ? 1 : Math.floorDiv(data.bits, Subathon.getConfigData().bitMin));
        }
    }

    public void addSubs(int subs) {
        data.subs += subs;
        if (data.subs >= Subathon.getConfigData().subsPerIncrement) {
            increaseValue(Subathon.getConfigData().onePerGift ? 1 : Math.floorDiv(data.subs, Subathon.getConfigData().subsPerIncrement));
            data.subs %= Subathon.getConfigData().subsPerIncrement;
        }
    }

    public void increaseValueFromBits(int amount) {
        increaseValue(amount * Subathon.getConfigData().bitModifier);
    }

    public void increaseValue(double amount) {
        double increase = amount * (Subathon.getConfigData().effectMultiplier * Subathon.getConfigData().effectIncrement);
        if (Subathon.getConfigData().updateTimer > 0) Subathon.integration.data.tempValue += increase;
        else data.value += increase;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(getDisplayValue());
        MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "value"), buf), "value");
    }

    public void increaseValue(double amount, boolean force) {
        if (force) {
            data.value += amount * (Subathon.getConfigData().effectMultiplier * Subathon.getConfigData().effectIncrement);
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeDouble(getDisplayValue());
            MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "value"), buf), "value");
        } else increaseValue(amount);
    }

    public void setValue(double amount) {
        data.value = amount;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(getDisplayValue());
        MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "value"), buf), "value");
    }

    public double getDisplayValue() {
        return Subathon.integration.data.value / Subathon.getConfigData().effectMultiplier;
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
            Subathon.mainProgressBar.setVisible(false);
            Subathon.mainProgressBar.setName(new TranslatableText("text.subathon.load.main", "", 0));
            Subathon.mainProgressBar.setValue(0);

            Subathon.usersProgressBar.setVisible(false);
            Subathon.usersProgressBar.setName(new TranslatableText("text.subathon.load.users", 0, 0));
            Subathon.usersProgressBar.setMaxValue(1);
            Subathon.usersProgressBar.setValue(0);
        }
    }
}
