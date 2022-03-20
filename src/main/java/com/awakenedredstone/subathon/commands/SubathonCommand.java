package com.awakenedredstone.subathon.commands;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.twitch.Bot;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.common.events.TwitchEvent;
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
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.logging.UncaughtExceptionHandler;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.awakenedredstone.subathon.Subathon.*;

public class SubathonCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("subathon").requires((source) -> {
            return source.hasPermissionLevel(4);
        }).then(CommandManager.literal("start").executes((source) -> {
            execute(source.getSource(), true);
            return 0;
        })).then(CommandManager.literal("stop").executes((source) -> {
            execute(source.getSource(), false);
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
        }))).then(CommandManager.literal("setSubsUntilIncrement").then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 32767)).executes((source) -> {
            executeSetSubsUntilIncrement(source.getSource(), IntegerArgumentType.getInteger(source, "amount"));
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

    public static void execute(ServerCommandSource source, boolean enable) throws CommandSyntaxException {
        if (getAuthData().access_token == null)
            throw new SimpleCommandExceptionType(new TranslatableText("subathon.messages.error.missing_auth")).create();
        final CommandSyntaxException[] exception = {null};
        if (enable) {
            if (thread != null && Bot.twitchClient != null) {
                source.sendError(new TranslatableText("subathon.command.error.already_online"));
                return;
            } else if (thread != null) {
                source.sendError(new TranslatableText("subathon.command.error.fatal"));
                execute(source, false);
                return;
            } else if (Bot.twitchClient != null) {
                source.sendError(new TranslatableText("subathon.command.error.fatal"));
                execute(source, false);
                return;
            }
            source.sendFeedback(new LiteralText("Subathon starting, please wait for the bot to be ready!"), false);
            source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                try {
                    executeTitle(source, player, new LiteralText("\u00a7c\u00a7lSubathon started!"), TitleS2CPacket::new);
                    executeTitle(source, player, new LiteralText("\u00a74Let the games begin!"), SubtitleS2CPacket::new);
                    player.sendMessage(new TranslatableText("subathon.starting"), true);
                    player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 100, 1f);
                    player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 100, 1f);
                    player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 100, 1f);
                    player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 100, 1f);
                } catch (CommandSyntaxException e) {
                    exception[0] = e;
                }
            });
            if (exception[0] != null) throw exception[0];
            thread = new Thread(bot = new Bot());
            thread.setName("Subathon Bot");
            thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(Subathon.LOGGER));
            thread.setDaemon(true);
            thread.start();
        } else {
            if (thread == null && Bot.twitchClient == null) {
                source.sendError(new TranslatableText("subathon.command.error.already_offline"));
                return;
            }
            source.sendFeedback(new LiteralText("Subathon stopped. The modifier is still being applied!"), true);
            source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                try {
                    executeTitle(source, player, new LiteralText("\u00a7c\u00a7lSubathon stopped!"), TitleS2CPacket::new);
                    executeTitle(source, player, new LiteralText("\u00a74The modifier is still being applied!"), SubtitleS2CPacket::new);
                    player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.MASTER, 100, 0.8f);
                    player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.MASTER, 100, 0.8f);
                    player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.MASTER, 100, 0.8f);
                } catch (CommandSyntaxException e) {
                    exception[0] = e;
                }
            });
            if (exception[0] != null) throw exception[0];
            if (Bot.twitchClient != null) Bot.twitchClient.close();
            if (thread != null && thread.isAlive() && !thread.isInterrupted()) thread.interrupt();
            Bot.twitchClient = null;
            thread = null;
        }
    }

    private static void executeReload(ServerCommandSource source) {
        source.sendFeedback(new TranslatableText("subathon.command.reloading"), true);
        Subathon.config.loadConfigs();
        Subathon.auth.loadAuth();
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player)
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
        source.sendFeedback(new TranslatableText("subathon.command.reloaded"), true);
        source.sendFeedback(new TranslatableText("subathon.command.reload.notice"), true);
    }

    public static void executeSetModifier(ServerCommandSource source, float amount) {
        source.sendFeedback(new LiteralText("Setting modifier to " + amount), true);
        Bot.setCounter(amount);
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player)
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
    }

    public static void executeSetBits(ServerCommandSource source, int amount) {
        source.sendFeedback(new LiteralText("Setting bits to " + amount), true);
        Bot.setBits((short) amount);
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player)
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
    }

    public static void executeSetSubsUntilIncrement(ServerCommandSource source, int amount) {
        source.sendFeedback(new LiteralText("Setting subsUntilIncrement to " + amount), true);
        Bot.setSubsUntilIncrement((short) amount);
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player)
            player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
    }

    public static void executeGetInfo(ServerCommandSource source) throws CommandSyntaxException {
        if (getAuthData().access_token == null) throw new SimpleCommandExceptionType(new TranslatableText("subathon.messages.error.missing_auth")).create();
        if (thread == null && Bot.twitchClient == null) {
            source.sendError(new TranslatableText("subathon.command.error.offline"));
            return;
        }
        source.sendFeedback(new LiteralText("Getting information, please wait..."), false);
        final Map<String, Object> validate = bot.validate();
        if (validate == null) {
            source.sendError(new LiteralText("Failed to validate, this is not good!"));
            execute(source, false);
            return;
        }

        if ((validate.get("status") != null && (int) validate.get("status") != 200) || (validate.get("code") != null && (int) validate.get("code") != 200)) {
            source.sendError(new LiteralText("Oh no, something went wrong!"));
            source.sendFeedback(new LiteralText("==========================================="), false);
            source.sendFeedback(new LiteralText(String.format("    Status code: %s", validate.get("status"))), false);
            source.sendFeedback(new LiteralText(String.format("    Message: %s", validate.get("message"))), false);
            source.sendFeedback(new LiteralText("==========================================="), false);
            execute(source, false);
            return;
        }

        int expiresIn = (int) validate.get("expires_in");
        source.sendFeedback(new LiteralText("\n\n"), false);
        source.sendFeedback(new LiteralText("==========================================="), false);
        source.sendFeedback(new LiteralText(String.format("    Login: %s", validate.get("login"))), false);
        source.sendFeedback(new LiteralText(String.format("    Scopes: %s", ((List<?>) validate.get("scopes")).stream().map(Object::toString).collect(Collectors.joining(" | ")))), false);
        source.sendFeedback(new LiteralText(String.format("    User Id: %s", validate.get("user_id"))), false);
        source.sendFeedback(new LiteralText(String.format("    Expires In: %s", expiresIn < 600 ? "§c§l" + expiresIn + "§r" : expiresIn)), false);
        source.sendFeedback(new LiteralText("==========================================="), false);
    }

    public static void executeTest(ServerCommandSource source, Events event, short count, @Nullable SubTiers tier) throws CommandSyntaxException {
        if (getAuthData().access_token == null) throw new SimpleCommandExceptionType(new TranslatableText("subathon.messages.error.missing_auth")).create();
        if (getAuthData().display_name == null) throw new SimpleCommandExceptionType(new TranslatableText("subathon.messages.error.incomplete_auth")).create();

        if (thread == null && Bot.twitchClient == null) {
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
                testEvent = transform(new SubscriptionEvent(null, new EventChannel(getAuthData().user_id, getAuthData().login),
                        new EventUser(getAuthData().user_id, getAuthData().display_name),
                        tier.ordinalName, Optional.empty(), 1, false,
                        null, 0, null, 1, 0, Collections.emptyList()));
            }
            case RESUBSCRIPTION -> {
                if (!getConfigData().enableSubs)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.module_disabled", "subscriptions")).create();
                if (tier == null)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                testEvent = transform(new SubscriptionEvent(null, new EventChannel(getAuthData().user_id, getAuthData().login),
                        new EventUser(getAuthData().user_id, getAuthData().display_name),
                        tier.ordinalName, Optional.empty(), (int) count, false,
                        null, new Random().nextInt(count + 1), null, 1, 0, Collections.emptyList()));
            }
            case SUB_GIFT -> {
                if (!getConfigData().enableSubs)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.module_disabled", "subscriptions")).create();
                if (tier == null)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.invalid_tier")).create();
                testEvent = transform(new GiftSubscriptionsEvent(new EventChannel(getAuthData().user_id, getAuthData().login), new EventUser(getAuthData().user_id, getAuthData().display_name),
                        tier.ordinalName, (int) count, count + new Random().nextInt(count)));
            }
            case CHEER -> {
                if (!getConfigData().enableBits)
                    throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.module_disabled", "bits")).create();
                testEvent = transform(new CheerEvent(null, new EventChannel(getAuthData().user_id, getAuthData().login), new EventUser(getAuthData().user_id, getAuthData().display_name),
                        "", (int) count, 0, 0, Collections.emptyList()));
            }
        }
        if (testEvent == null)
            throw new SimpleCommandExceptionType(new TranslatableText("subathon.command.error.fatal")).create();
        source.sendFeedback(new TranslatableText("subathon.command.test.trying", event.name()), true);
        Bot.twitchClient.getEventManager().publish(testEvent);
    }

    private static void executeTitle(ServerCommandSource source, ServerPlayerEntity player, Text title, Function<Text, Packet<?>> constructor) throws CommandSyntaxException {
        player.networkHandler.sendPacket(constructor.apply(Texts.parse(source, title, player, 0)));
    }

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
        CHEER
    }
}
