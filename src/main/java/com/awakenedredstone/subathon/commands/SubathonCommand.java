package com.awakenedredstone.subathon.commands;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.json.JsonHelper;
import com.awakenedredstone.subathon.util.SubathonData;
import com.github.twitch4j.chat.events.TwitchEvent;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.common.events.domain.EventUser;
import com.github.twitch4j.common.util.TwitchUtils;
import com.github.twitch4j.pubsub.domain.ChannelBitsData;
import com.github.twitch4j.pubsub.domain.CommerceMessage;
import com.github.twitch4j.pubsub.domain.SubGiftData;
import com.github.twitch4j.pubsub.domain.SubscriptionData;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.WorldSavePath;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.awakenedredstone.subathon.Subathon.getConfigData;
import static com.awakenedredstone.subathon.Subathon.integration;

public class SubathonCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("subathon").requires((source) -> {
            return source.hasPermissionLevel(2);
        }).then(CommandManager.literal("start").executes((source) -> {
            executeStart(source.getSource());
            return 0;
        })).then(CommandManager.literal("stop").executes((source) -> {
            executeStop(source.getSource());
            return 0;
        })).then(CommandManager.literal("restart").executes((source) -> {
            executeRestart(source.getSource());
            return 0;
        })).then(CommandManager.literal("reload").executes((source) -> {
            executeReload(source.getSource());
            return 0;
        })).then(CommandManager.literal("info").executes((source) -> {
            executeGetInfo(source.getSource());
            return 0;
        })).then(CommandManager.literal("setModifier").then(CommandManager.argument("amount", FloatArgumentType.floatArg()).executes((source) -> {
            executeSetModifier(source.getSource(), FloatArgumentType.getFloat(source, "amount"));
            return 0;
        }))).then(CommandManager.literal("setBits").then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 32767)).executes((source) -> {
            executeSetBits(source.getSource(), IntegerArgumentType.getInteger(source, "amount"));
            return 0;
        }))).then(CommandManager.literal("setSubs").then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 32767)).executes((source) -> {
            executeSetSubs(source.getSource(), IntegerArgumentType.getInteger(source, "amount"));
            return 0;
        }))).then(CommandManager.literal("test")
                .then(CommandManager.literal("sub")
                        .then(CommandManager.argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .executes(source -> {
                                    try {
                                        executeTest(source.getSource(), Events.SUBSCRIPTION, (short) 0, SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                    } catch (IllegalArgumentException e) {
                                        throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                                    }
                                    return 0;
                                })
                        )
                ).then(CommandManager.literal("resub")
                        .then(CommandManager.argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .then(CommandManager.argument("months", IntegerArgumentType.integer(1, 32767))
                                        .executes(source -> {
                                            try {
                                                executeTest(source.getSource(), Events.RESUBSCRIPTION, (short) IntegerArgumentType.getInteger(source, "months"),
                                                        SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                            } catch (IllegalArgumentException e) {
                                                throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                                            }
                                            return 0;
                                        })
                                )
                        )
                ).then(CommandManager.literal("subGift")
                        .then(CommandManager.argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 32767))
                                        .executes(source -> {
                                            try {
                                                executeTest(source.getSource(), Events.SUB_GIFT, (short) IntegerArgumentType.getInteger(source, "amount"),
                                                        SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                            } catch (IllegalArgumentException e) {
                                                throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                                            }
                                            return 0;
                                        })
                                )
                        )
                ).then(CommandManager.literal("giftUser")
                        .then(CommandManager.argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .executes(source -> {
                                    try {
                                        executeTest(source.getSource(), Events.GIFT_USER, (short) 1, SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                    } catch (IllegalArgumentException e) {
                                        throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                                    }
                                    return 0;
                                })
                        )
                ).then(CommandManager.literal("cheer")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 32767))
                                .executes(source -> {
                                    executeTest(source.getSource(), Events.CHEER, (short) IntegerArgumentType.getInteger(source, "amount"), null);
                                    return 0;
                                })
                        )
                )
        ));
    }

    public static void executeStart(ServerCommandSource source) {
        source.sendFeedback(new TranslatableText("subathon.messages.start.notice"), true);
        File file = source.getServer().getSavePath(WorldSavePath.ROOT).resolve("subathon_data.json").toFile();
        if (!file.exists()) JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(new SubathonData()).getAsJsonObject(), file);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            SubathonData data = Subathon.GSON.fromJson(reader, SubathonData.class);
            integration.start(data);
        } catch (IOException e) {
            Subathon.LOGGER.error("Failed to start the integration!", e);
            source.sendError(new TranslatableText("subathon.command.start.fail"));
        }
    }

    public static void executeStop(ServerCommandSource source) {
        source.sendFeedback(new TranslatableText("subathon.messages.stop"), true);
        if (integration.isRunning) integration.stop();
        else integration.stop(false);
    }

    public static void executeRestart(ServerCommandSource source) {
        source.sendFeedback(new TranslatableText("subathon.messages.restart.notice"), true);
        integration.stop(false);
        File file = source.getServer().getSavePath(WorldSavePath.ROOT).resolve("subathon_data.json").toFile();
        if (!file.exists()) JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(new SubathonData()).getAsJsonObject(), file);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            SubathonData data = Subathon.GSON.fromJson(reader, SubathonData.class);
            integration.start(data);
        } catch (IOException e) {
            Subathon.LOGGER.error("Failed to start the integration!", e);
            source.sendError(new TranslatableText("subathon.command.start.fail"));
        }
    }

    private static void executeReload(ServerCommandSource source) {
        source.sendFeedback(new TranslatableText("subathon.command.reloading"), true);
        Subathon.config.loadConfigs();
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player)
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
        source.sendFeedback(new TranslatableText("subathon.command.reloaded"), true);
        source.sendFeedback(new TranslatableText("subathon.command.reload.notice"), true);
    }

    public static void executeSetModifier(ServerCommandSource source, float amount) {
        source.sendFeedback(new LiteralText("Setting modifier value to " + amount), true);
        Subathon.integration.setValue(amount * getConfigData().effectMultiplier);
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player)
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
    }

    public static void executeSetBits(ServerCommandSource source, int amount) {
        source.sendFeedback(new LiteralText("Setting bits count to " + amount), true);
        integration.data.bits = amount;
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player)
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
    }

    public static void executeSetSubs(ServerCommandSource source, int amount) {
        source.sendFeedback(new LiteralText("Setting sub count to " + amount), true);
        integration.data.subs = amount;
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player)
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
    }

    public static void executeGetInfo(ServerCommandSource source) throws CommandSyntaxException {
        source.sendError(new LiteralText("This command is temporarily disabled."));
        //TODO: Show status and info
    }

    public static void executeTest(ServerCommandSource source, Events event, short count, @Nullable SubTiers tier) throws CommandSyntaxException {
        if (!integration.isRunning) {
            source.sendError(new TranslatableText("subathon.command.error.offline"));
            return;
        }

        TwitchEvent testEvent = null;
        switch (event) {
            case SUBSCRIPTION -> {
                if (!getConfigData().enableSubs)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.module_disabled", "subscriptions")).create();
                if (tier == null)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                testEvent = (new SubscriptionEvent(null, new EventChannel(getConfigData().channelId, getConfigData().channelName),
                        new EventUser(getConfigData().channelId, getConfigData().channelDisplayName),
                        tier.ordinalName, Optional.empty(), 1, false,
                        null, 0, null, 1, 0, Collections.emptyList()));
            }
            case RESUBSCRIPTION -> {
                if (!getConfigData().enableSubs)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.module_disabled", "subscriptions")).create();
                if (tier == null)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                testEvent = (new SubscriptionEvent(null, new EventChannel(getConfigData().channelId, getConfigData().channelName),
                        new EventUser(getConfigData().channelId, getConfigData().channelDisplayName),
                        tier.ordinalName, Optional.empty(), (int) count, false,
                        null, new Random().nextInt(count + 1), null, 1, 0, Collections.emptyList()));
            }
            case SUB_GIFT -> {
                if (!getConfigData().enableSubs)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.module_disabled", "subscriptions")).create();
                if (tier == null)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                testEvent = (new GiftSubscriptionsEvent(new EventChannel(getConfigData().channelId, getConfigData().channelName),
                        new EventUser(getConfigData().channelId, getConfigData().channelDisplayName),
                        tier.ordinalName, (int) count, count + new Random().nextInt(count)));
            }
            case GIFT_USER -> {
                if (!getConfigData().enableSubs)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.module_disabled", "subscriptions")).create();
                if (tier == null) throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                testEvent = (new SubscriptionEvent(null, new EventChannel(getConfigData().channelId, getConfigData().channelName),
                        new EventUser(getConfigData().channelId, getConfigData().channelDisplayName),
                        tier.ordinalName, Optional.empty(), 1, true,
                        new EventUser("158540511", "Subathon"),
                        0, 1, 1, 0, Collections.emptyList()));
            }
            case CHEER -> {
                if (!getConfigData().enableBits)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.module_disabled", "bits")).create();
                testEvent = (new CheerEvent(null, new EventChannel(getConfigData().channelId, getConfigData().channelName),
                        new EventUser(getConfigData().channelId, getConfigData().channelDisplayName),
                        "", (int) count, 0, 0, Collections.emptyList()));
            }
        }
        if (testEvent == null)
            throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.fatal")).create();
        source.sendFeedback(new TranslatableText("subathon.command.test.trying", event.name()), true);
        integration.getTwitchClient().getEventManager().publish(testEvent);
    }

    @Deprecated
    public static ChannelSubscribeEvent transform(SubscriptionEvent e) {
        SubscriptionData data = new SubscriptionData();
        data.setUserName(e.getUser().getName());
        data.setDisplayName(e.getUser().getName());
        data.setUserId(e.getUser().getId());
        data.setChannelName(e.getChannel().getName());
        data.setChannelId(e.getChannel().getId());
        data.setTimestamp(e.getFiredAtInstant());
        data.setSubPlan(e.getSubPlan());
        data.setSubPlanName(String.format("Channel Subscription (%s)", e.getChannel().getName()));
        data.setMonths(e.getMonths());
        data.setCumulativeMonths(e.getSubStreak());
        data.setStreakMonths(e.getSubStreak());
        data.setMultiMonthDuration(e.getMultiMonthDuration());
        data.setRecipientId(e.getUser().getId());
        data.setRecipientDisplayName(e.getUser().getName());
        data.setRecipientUserName(e.getUser().getName());
        data.setIsGift(e.getGifted());
        data.setBenefitEndMonth(e.getGiftMonths() == null ? null : ZonedDateTime.now().plus(e.getGiftMonths(), ChronoUnit.MONTHS).getMonth().getValue());
        e.getMessage().map(msg -> {
            CommerceMessage cm = new CommerceMessage();
            cm.setMessage(msg);
            return cm;
        }).ifPresent(data::setSubMessage);
        return new ChannelSubscribeEvent(data);
    }

    @Deprecated
    public static ChannelSubGiftEvent transform(GiftSubscriptionsEvent e) {
        SubGiftData data = new SubGiftData();
        data.setCount(e.getCount());
        data.setTier(SubscriptionPlan.fromString(e.getSubscriptionPlan()));
        data.setUserId(e.getUser().getId());
        data.setUserName(e.getUser().getName());
        data.setDisplayName(e.getUser().getName());
        data.setChannelId(e.getChannel().getId());
        data.setUuid(UUID.randomUUID().toString());
        data.setType("mystery-gift-purchase");
        return new ChannelSubGiftEvent(data);
    }

    @Deprecated
    public static ChannelBitsEvent transform(CheerEvent e) {
        ChannelBitsData data = new ChannelBitsData();
        data.setUserId(e.getUser().getId());
        data.setUserName(e.getUser().getName());
        data.setChannelName(e.getChannel().getName());
        data.setChannelId(e.getChannel().getId());
        data.setTime(e.getFiredAtInstant().toString());
        data.setChatMessage(e.getMessage());
        data.setBitsUsed(e.getBits());
        data.setTotalBitsUsed(e.getBits());
        data.setContext("cheer");
        data.isAnonymous(TwitchUtils.ANONYMOUS_CHEERER.equals(e.getUser()));
        return new ChannelBitsEvent(data);
    }

    enum SubTiers {
        PRIME("Prime"),
        TIER1("1000"),
        TIER2("2000"),
        TIER3("3000");

        private final String ordinalName;

        SubTiers(String ordinalName) {
            this.ordinalName = ordinalName;
        }
    }

    enum Events {
        SUBSCRIPTION,
        RESUBSCRIPTION,
        SUB_GIFT,
        GIFT_USER,
        CHEER
    }
}
