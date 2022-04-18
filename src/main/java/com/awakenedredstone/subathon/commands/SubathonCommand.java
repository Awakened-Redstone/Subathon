package com.awakenedredstone.subathon.commands;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.json.JsonHelper;
import com.awakenedredstone.subathon.twitch.SubathonData;
import com.awakenedredstone.subathon.twitch.TwitchIntegration;
import com.github.twitch4j.chat.events.TwitchEvent;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.common.events.domain.EventUser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static com.awakenedredstone.subathon.Subathon.getConfigData;
import static com.awakenedredstone.subathon.Subathon.integration;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SubathonCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("subathon").requires((source) -> source.hasPermissionLevel(2) || source.getServer().isSingleplayer())
                .then(literal("start").executes((source) -> executeStart(source.getSource())))
                .then(literal("stop").executes((source) -> executeStop(source.getSource())))
                .then(literal("restart").executes((source) -> executeRestart(source.getSource())))
                .then(literal("reload").executes((source) -> executeReload(source.getSource())))
                .then(literal("info").executes((source) -> executeGetInfo(source.getSource())))
                .then(literal("set").requires((source) -> source.hasPermissionLevel(2))
                        .then(literal("modifier").then(argument("amount", FloatArgumentType.floatArg())
                                .executes((source) -> executeSet(source.getSource(), ValueType.MODIFIER, FloatArgumentType.getFloat(source, "amount"), 0))))
                        .then(literal("temp_modifier").then(argument("amount", FloatArgumentType.floatArg())
                                .executes((source) -> executeSet(source.getSource(), ValueType.TEMP_MODIFIER, FloatArgumentType.getFloat(source, "amount"), 0))))
                        .then(literal("subs").then(argument("amount", IntegerArgumentType.integer(0, 32767))
                                .executes((source) -> executeSet(source.getSource(), ValueType.SUBS, 0, IntegerArgumentType.getInteger(source, "amount")))))
                        .then(literal("bits").then(argument("amount", IntegerArgumentType.integer(0, 32767))
                                .executes((source) -> executeSet(source.getSource(), ValueType.BITS, 0, IntegerArgumentType.getInteger(source, "amount"))))))
                .then(literal("get").requires((source) -> source.hasPermissionLevel(2))
                        .then(literal("modifier").executes(source -> executeGet(source.getSource(), ValueType.MODIFIER)))
                        .then(literal("subs").executes(source -> executeGet(source.getSource(), ValueType.SUBS)))
                        .then(literal("bits").executes(source -> executeGet(source.getSource(), ValueType.BITS))))
                .then(literal("test").requires((source) -> source.hasPermissionLevel(2))
                        .then(literal("sub").then(argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .executes(source -> {
                                    try {
                                        return executeTest(source.getSource(), Events.SUBSCRIPTION, (short) 0, SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                    } catch (IllegalArgumentException e) {
                                        throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                                    }
                                })))
                        .then(literal("resub").then(argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .then(argument("months", IntegerArgumentType.integer(1, 32767))
                                        .executes(source -> {
                                            try {
                                                return executeTest(source.getSource(), Events.RESUBSCRIPTION, (short) IntegerArgumentType.getInteger(source, "months"),
                                                        SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                            } catch (IllegalArgumentException e) {
                                                throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                                            }
                                        }))))
                        .then(literal("subGift").then(argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .then(argument("amount", IntegerArgumentType.integer(1, 32767))
                                        .executes(source -> {
                                            try {
                                                return executeTest(source.getSource(), Events.SUB_GIFT, (short) IntegerArgumentType.getInteger(source, "amount"),
                                                        SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                            } catch (IllegalArgumentException e) {
                                                throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                                            }
                                        }))))
                        .then(literal("giftUser").then(argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .executes(source -> {
                                    try {
                                        return executeTest(source.getSource(), Events.GIFT_USER, (short) 1, SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                    } catch (IllegalArgumentException e) {
                                        throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                                    }
                                })))
                        .then(literal("cheer").then(argument("amount", IntegerArgumentType.integer(1, 32767))
                                .executes(source -> executeTest(source.getSource(), Events.CHEER, (short) IntegerArgumentType.getInteger(source, "amount"), null))))));
    }

    public static int executeStart(ServerCommandSource source) {
        source.sendFeedback(new TranslatableText("commands.subathon.start.message"), true);
        source.sendFeedback(new TranslatableText("commands.subathon.start.warning"), true);

        File file = source.getServer().getSavePath(WorldSavePath.ROOT).resolve("subathon_data.json").toFile();
        if (!file.exists())
            JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(new SubathonData()).getAsJsonObject(), file);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            SubathonData data = Subathon.GSON.fromJson(reader, SubathonData.class);
            integration.start(data);
        } catch (IOException e) {
            integration.simpleExecutor.execute(new TwitchIntegration.ClearProgressBar());
            Subathon.LOGGER.error("Failed to start the integration!", e);
            source.sendError(new TranslatableText("commands.subathon.start.fail"));
            return -1;
        }

        return 0;
    }

    public static int executeStop(ServerCommandSource source) {
        source.sendFeedback(new TranslatableText("commands.subathon.stop.message"), true);

        if (integration.isRunning) integration.stop();
        else {
            source.sendFeedback(new TranslatableText("commands.subathon.stop.offline"), true);
            source.sendFeedback(new TranslatableText("commands.subathon.stop.silent"), true);
            integration.stop(false);
        }

        return 0;
    }

    public static int executeRestart(ServerCommandSource source) throws CommandSyntaxException {
        if (!integration.isRunning)
            throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.offline")).create();

        source.sendFeedback(new TranslatableText("commands.subathon.restart.message"), true);
        source.sendFeedback(new TranslatableText("commands.subathon.restart.warning"), true);

        integration.stop(false);

        File file = source.getServer().getSavePath(WorldSavePath.ROOT).resolve("subathon_data.json").toFile();
        if (!file.exists())
            JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(new SubathonData()).getAsJsonObject(), file);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            SubathonData data = Subathon.GSON.fromJson(reader, SubathonData.class);
            integration.start(data);
        } catch (IOException e) {
            Subathon.LOGGER.error("Failed to start the integration!", e);
            source.sendError(new TranslatableText("commands.subathon.start.fail"));
        }

        return 0;
    }

    public static int executeReload(ServerCommandSource source) {
        source.sendFeedback(new TranslatableText("commands.subathon.reload.start"), true);

        Subathon.config.loadConfigs();
        if (integration.isRunning) integration.reload();

        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player) {
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
        }

        source.sendFeedback(new TranslatableText("commands.subathon.reload.complete"), true);
        source.sendFeedback(new TranslatableText("commands.subathon.reload.warning"), true);

        return 0;
    }

    public static int executeSet(ServerCommandSource source, ValueType type, float value1, int value2) {
        switch (type) {
            case MODIFIER -> {
                integration.setValue(value1);
                source.sendFeedback(new TranslatableText("commands.subathon.set.value", value1), true);
            }
            case TEMP_MODIFIER -> {
                integration.data.tempValue = value1;
                source.sendFeedback(new TranslatableText("commands.subathon.set.temp_value", value1), true);
            }
            case SUBS -> {
                integration.data.subs = value2;
                source.sendFeedback(new TranslatableText("commands.subathon.set.subs", value2), true);
            }
            case BITS -> {
                integration.data.bits = value2;
                source.sendFeedback(new TranslatableText("commands.subathon.set.bits", value2), true);
            }
        }

        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player) {
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
        }

        return 0;
    }

    public static int executeGet(ServerCommandSource source, ValueType type) {
        switch (type) {
            case MODIFIER -> {
                source.sendFeedback(new TranslatableText("commands.subathon.get.internal_value", integration.data.value), false);
                source.sendFeedback(new TranslatableText("commands.subathon.get.display_value", integration.getDisplayValue()), false);
                source.sendFeedback(new TranslatableText("commands.subathon.get.temp_value", integration.data.tempValue), false);
            }
            case SUBS -> source.sendFeedback(new TranslatableText("commands.subathon.get.subs", integration.data.subs), false);
            case BITS -> source.sendFeedback(new TranslatableText("commands.subathon.get.bits", integration.data.bits), false);
        }

        return 0;
    }

    public static int executeGetInfo(ServerCommandSource source) throws CommandSyntaxException {
        if (integration.getTwitchClient() == null) throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.offline")).create();

        Set<String> channels = integration.getTwitchClient().getChat().getChannels();
        String channelList = channels.isEmpty() ? "None" : String.join(", ", channels);
        source.sendFeedback(new TranslatableText("commands.subathon.info", channelList).formatted(Formatting.GRAY).formatted(Formatting.ITALIC), false);

        return 0;
    }

    public static int executeTest(ServerCommandSource source, Events event, short count, @Nullable SubTiers tier) throws CommandSyntaxException {
        if (!integration.isRunning)
            throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.offline")).create();

        TwitchEvent testEvent = null;
        switch (event) {
            case SUBSCRIPTION -> {
                if (!getConfigData().enableSubs)
                    throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.module_disabled", "subscriptions")).create();
                if (tier == null)
                    throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.invalid_tier")).create();

                testEvent = (new SubscriptionEvent(null, new EventChannel(getConfigData().channelIds.get(0), getConfigData().channels.get(0)),
                        new EventUser(getConfigData().channelIds.get(0), getConfigData().channelDisplayNames.get(0)),
                        tier.ordinalName, Optional.empty(), 1, false,
                        null, 0, null, 1, 0, Collections.emptyList()));
            }
            case RESUBSCRIPTION -> {
                if (!getConfigData().enableSubs)
                    throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.module_disabled", "subscriptions")).create();
                if (tier == null)
                    throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.invalid_tier")).create();

                testEvent = (new SubscriptionEvent(null, new EventChannel(getConfigData().channelIds.get(0), getConfigData().channels.get(0)),
                        new EventUser(getConfigData().channelIds.get(0), getConfigData().channelDisplayNames.get(0)),
                        tier.ordinalName, Optional.empty(), (int) count, false,
                        null, new Random().nextInt(count + 1), null, 1, 0, Collections.emptyList()));
            }
            case SUB_GIFT -> {
                if (!getConfigData().enableSubs)
                    throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.module_disabled", "subscriptions")).create();
                if (tier == null)
                    throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.invalid_tier")).create();

                testEvent = (new GiftSubscriptionsEvent(new EventChannel(getConfigData().channelIds.get(0), getConfigData().channels.get(0)),
                        new EventUser(getConfigData().channelIds.get(0), getConfigData().channelDisplayNames.get(0)),
                        tier.ordinalName, (int) count, count + new Random().nextInt(count)));
            }
            case GIFT_USER -> {
                if (!getConfigData().enableSubs)
                    throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.module_disabled", "subscriptions")).create();
                if (tier == null)
                    throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.invalid_tier")).create();

                testEvent = (new SubscriptionEvent(null, new EventChannel(getConfigData().channelIds.get(0), getConfigData().channels.get(0)),
                        new EventUser(getConfigData().channelIds.get(0), getConfigData().channelDisplayNames.get(0)),
                        tier.ordinalName, Optional.empty(), 1, true,
                        new EventUser("158540511", "Subathon"),
                        0, 1, 1, 0, Collections.emptyList()));
            }
            case CHEER -> {
                if (!getConfigData().enableBits)
                    throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.module_disabled", "bits")).create();

                testEvent = (new CheerEvent(null, new EventChannel(getConfigData().channelIds.get(0), getConfigData().channels.get(0)),
                        new EventUser(getConfigData().channelIds.get(0), getConfigData().channelDisplayNames.get(0)),
                        "", (int) count, 0, 0, Collections.emptyList()));
            }
        }

        if (testEvent == null)
            throw new SimpleCommandExceptionType(new TranslatableText("commands.subathon.error.fatal")).create();

        source.sendFeedback(new TranslatableText("commands.subathon.test.trigger", event.name()).formatted(Formatting.GRAY).formatted(Formatting.ITALIC), true);

        integration.getTwitchClient().getEventManager().publish(testEvent);

        return 0;
    }

    enum ValueType {
        MODIFIER,
        TEMP_MODIFIER,
        SUBS,
        BITS
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

    public enum Events {
        SUBSCRIPTION,
        RESUBSCRIPTION,
        SUB_GIFT,
        GIFT_USER,
        CHEER
    }
}
