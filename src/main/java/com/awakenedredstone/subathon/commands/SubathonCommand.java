package com.awakenedredstone.subathon.commands;

import com.awakenedredstone.cubecontroller.CubeController;
import com.awakenedredstone.cubecontroller.GameControl;
import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.IntegrationStatus;
import com.github.twitch4j.chat.events.TwitchEvent;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.common.events.domain.EventUser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.*;

import static com.awakenedredstone.subathon.Subathon.*;
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
                .then(literal("clean").executes((source) -> executeClean(source.getSource())))
                .then(literal("set").requires((source) -> source.hasPermissionLevel(2))
                        .then(literal("subs").then(argument("amount", IntegerArgumentType.integer(0, 32767))
                                .executes((source) -> executeSet(source.getSource(), ValueType.SUBS, 0, IntegerArgumentType.getInteger(source, "amount")))))
                        .then(literal("bits").then(argument("amount", IntegerArgumentType.integer(0, 32767))
                                .executes((source) -> executeSet(source.getSource(), ValueType.BITS, 0, IntegerArgumentType.getInteger(source, "amount"))))))
                .then(literal("get").requires((source) -> source.hasPermissionLevel(2))
                        .then(literal("subs").executes(source -> executeGet(source.getSource(), ValueType.SUBS)))
                        .then(literal("bits").executes(source -> executeGet(source.getSource(), ValueType.BITS))))
                .then(literal("test").requires((source) -> source.hasPermissionLevel(2))
                        .then(literal("sub").then(argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .executes(source -> {
                                    try {
                                        return executeTest(source.getSource(), Events.SUBSCRIPTION, 0, SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                    } catch (IllegalArgumentException e) {
                                        throw new SimpleCommandExceptionType(Text.translatable("subathon.command.error.invalid_tier")).create();
                                    }
                                })))
                        .then(literal("resub").then(argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .then(argument("months", IntegerArgumentType.integer(1))
                                        .executes(source -> {
                                            try {
                                                return executeTest(source.getSource(), Events.RESUBSCRIPTION, IntegerArgumentType.getInteger(source, "months"),
                                                        SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                            } catch (IllegalArgumentException e) {
                                                throw new SimpleCommandExceptionType(Text.translatable("subathon.command.error.invalid_tier")).create();
                                            }
                                        }))))
                        .then(literal("subGift").then(argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .then(argument("amount", IntegerArgumentType.integer(1))
                                        .executes(source -> {
                                            try {
                                                return executeTest(source.getSource(), Events.SUB_GIFT, IntegerArgumentType.getInteger(source, "amount"),
                                                        SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                            } catch (IllegalArgumentException e) {
                                                throw new SimpleCommandExceptionType(Text.translatable("subathon.command.error.invalid_tier")).create();
                                            }
                                        }))))
                        .then(literal("giftUser").then(argument("tier", StringArgumentType.word())
                                .suggests((source, builder) -> CommandSource.suggestMatching(Arrays.stream(SubTiers.values()).map(v -> v.name().toLowerCase()).toList(), builder))
                                .executes(source -> {
                                    try {
                                        return executeTest(source.getSource(), Events.GIFT_USER, 1, SubTiers.valueOf(StringArgumentType.getString(source, "tier").toUpperCase()));
                                    } catch (IllegalArgumentException e) {
                                        throw new SimpleCommandExceptionType(Text.translatable("subathon.command.error.invalid_tier")).create();
                                    }
                                })))
                        .then(literal("cheer").then(argument("amount", IntegerArgumentType.integer(1))
                                .executes(source -> executeTest(source.getSource(), Events.CHEER, IntegerArgumentType.getInteger(source, "amount"), null))))));
    }

    /*TODO:UPDATE_THIS*/
    public static int executeStart(ServerCommandSource source) throws CommandSyntaxException {
        if (integration.status != IntegrationStatus.OFFLINE)
            throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.already_online")).create();
        try {
            source.sendFeedback(Text.translatable("commands.subathon.start.message"), true);
            source.sendFeedback(Text.translatable("commands.subathon.start.warning"), true);
            integration.start();
        } catch (Exception e) {
            LOGGER.error("Failed to execute command!", e);
            throw e;
        }
        return 0;
    }

    /*TODO:UPDATE_THIS*/
    public static int executeStop(ServerCommandSource source) {
        source.sendFeedback(Text.translatable("commands.subathon.stop.message"), true);

        try {
            if (integration.status != IntegrationStatus.OFFLINE) {
                integration.stop();
            } else {
                source.sendFeedback(Text.translatable("commands.subathon.stop.offline"), true);
                source.sendFeedback(Text.translatable("commands.subathon.stop.silent"), true);
                integration.stop(false);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute command!", e);
            throw e;
        }

        return 0;
    }

    /*TODO:UPDATE_THIS*/
    public static int executeRestart(ServerCommandSource source) throws CommandSyntaxException {
        if (integration.status == IntegrationStatus.OFFLINE)
            throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.offline")).create();

        try {
            source.sendFeedback(Text.translatable("commands.subathon.restart.message"), true);
            source.sendFeedback(Text.translatable("commands.subathon.restart.warning"), true);

            integration.stop(false);
            integration.start();
        } catch (Exception e) {
            LOGGER.error("Failed to execute command!", e);
            throw e;
        }

        return 0;
    }

    /*TODO:UPDATE_THIS*/
    public static int executeReload(ServerCommandSource source) throws CommandSyntaxException {
        try {
            source.sendFeedback(Text.translatable("commands.subathon.reload.start"), true);

            Subathon.config.loadConfigs();
            if (integration.status != IntegrationStatus.OFFLINE) integration.reload();

            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player) {
                player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
                player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
            }

            source.sendFeedback(Text.translatable("commands.subathon.reload.complete"), true);
            source.sendFeedback(Text.translatable("commands.subathon.reload.warning"), true);
        } catch (Exception e) {
            LOGGER.error("Failed to execute command!", e);
            throw e;
        }

        return 0;
    }

    public static int executeSet(ServerCommandSource source, ValueType type, float value1, int value2) {
        try {
            switch (type) {
                case SUBS -> {
                    integration.data.subs = value2;
                    source.sendFeedback(Text.translatable("commands.subathon.set.subs", value2), true);
                }
                case BITS -> {
                    integration.data.bits = value2;
                    source.sendFeedback(Text.translatable("commands.subathon.set.bits", value2), true);
                }
            }

            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player) {
                player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
                player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute command!", e);
            throw e;
        }

        return 0;
    }

    public static int executeGet(ServerCommandSource source, ValueType type) {
        try {
            switch (type) {
                case SUBS ->
                        source.sendFeedback(Text.translatable("commands.subathon.get.subs", integration.data.subs), false);
                case BITS ->
                        source.sendFeedback(Text.translatable("commands.subathon.get.bits", integration.data.bits), false);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute command!", e);
            throw e;
        }

        return 0;
    }

    public static int executeGetInfo(ServerCommandSource source) throws CommandSyntaxException {
        if (integration.getTwitchClient() == null)
            throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.offline")).create();

        try {
            Set<String> channels = integration.getTwitchClient().getChat().getChannels();
            String channelList = channels.isEmpty() ? "None" : String.join(", ", channels);
            source.sendFeedback(Text.translatable("commands.subathon.info", channelList).formatted(Formatting.GRAY).formatted(Formatting.ITALIC), false);

            return 0;
        } catch (Exception e) {
            LOGGER.error("Failed to execute command!", e);
            throw e;
        }
    }

    public static int executeClean(ServerCommandSource source) {
        CubeController.GAME_CONTROL.forEach(control -> source.getServer().getSaveProperties().getMainWorldProperties().getScheduledEvents().remove(control.identifier().toString()));

        try {

            while (!integration.data.nextValues.isEmpty()) {
                Map.Entry<Identifier, Double> pair = integration.data.nextValues.pollFirstEntry();
                Identifier identifier = pair.getKey();
                double value = pair.getValue();

                Optional<GameControl> controlOptional = CubeController.getControl(identifier);
                controlOptional.ifPresent(control -> {
                    if (control.valueBased()) control.value(control.value() + value);
                    if (control.hasEvent() && shouldInvoke(control.identifier())) control.invoke();
                });
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute command!", e);
            throw e;
        }

        source.sendFeedback(Text.translatable("commands.subathon.clean"), true);

        return 0;
    }

    public static int executeTest(ServerCommandSource source, Events event, int count, @Nullable SubTiers tier) throws CommandSyntaxException {
        if (integration.status == IntegrationStatus.OFFLINE)
            throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.offline")).create();

        try {
            TwitchEvent testEvent = null;
            switch (event) {
                case SUBSCRIPTION -> {
                    if (!getConfigData().enableSubs)
                        throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.module_disabled", "subscriptions")).create();
                    if (tier == null)
                        throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.invalid_tier")).create();

                    testEvent = (new SubscriptionEvent(null, new EventChannel("0", getConfigData().channels.get(0)),
                            new EventUser("1", getConfigData().channels.get(0)),
                            tier.ordinalName, Optional.empty(), 1, false,
                            null, 0, null, 1, 0, Collections.emptyList()));
                }
                case RESUBSCRIPTION -> {
                    if (!getConfigData().enableSubs)
                        throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.module_disabled", "subscriptions")).create();
                    if (tier == null)
                        throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.invalid_tier")).create();

                    testEvent = (new SubscriptionEvent(null, new EventChannel("0", getConfigData().channels.get(0)),
                            new EventUser("1", getConfigData().channels.get(0)),
                            tier.ordinalName, Optional.of("Did I hear SUBATHON?"), (int) count, false,
                            null, new Random().nextInt(count + 1), null, 1, 0, Collections.emptyList()));
                }
                case SUB_GIFT -> {
                    if (!getConfigData().enableSubs)
                        throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.module_disabled", "subscriptions")).create();
                    if (tier == null)
                        throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.invalid_tier")).create();

                    testEvent = (new GiftSubscriptionsEvent(new EventChannel("0", getConfigData().channels.get(0)),
                            new EventUser("1", getConfigData().channels.get(0)),
                            tier.ordinalName, (int) count, count + new Random().nextInt(count)));
                }
                case GIFT_USER -> {
                    if (!getConfigData().enableSubs)
                        throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.module_disabled", "subscriptions")).create();
                    if (tier == null)
                        throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.invalid_tier")).create();

                    testEvent = (new SubscriptionEvent(null, new EventChannel("0", getConfigData().channels.get(0)),
                            new EventUser("1", getConfigData().channels.get(0)),
                            tier.ordinalName, Optional.empty(), 1, true,
                            new EventUser("158540511", "Subathon"),
                            0, 1, 1, 0, Collections.emptyList()));
                }
                case CHEER -> {
                    if (!getConfigData().enableBits)
                        throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.module_disabled", "bits")).create();

                    testEvent = (new CheerEvent(null, new EventChannel("0", getConfigData().channels.get(0)),
                            new EventUser("1", getConfigData().channels.get(0)),
                            String.format("Cheer%d Jump king!", count), (int) count, 0, 0, Collections.emptyList()));
                }
            }

            if (testEvent == null)
                throw new SimpleCommandExceptionType(Text.translatable("commands.subathon.error.fatal")).create();

            source.sendFeedback(Text.translatable("commands.subathon.test.trigger", event.name()).formatted(Formatting.GRAY).formatted(Formatting.ITALIC), true);

            integration.getTwitchClient().getEventManager().publish(testEvent);
        } catch (CommandSyntaxException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to execute command!", e);
            throw e;
        }

        return 0;
    }

    enum ValueType {
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
