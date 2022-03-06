package com.awakenedredstone.subathon.commands;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.twitch.Bot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.util.Util;
import net.minecraft.util.logging.UncaughtExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.awakenedredstone.subathon.Subathon.bot;
import static com.awakenedredstone.subathon.Subathon.thread;

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
        })).then(CommandManager.literal("authorize").executes((source) -> {
            openLink(source.getSource());
            return 0;
        })).then(CommandManager.literal("setModifier").then(CommandManager.argument("amount", IntegerArgumentType.integer()).executes((source) -> {
            executeSetModifier(source.getSource(), IntegerArgumentType.getInteger(source, "amount"));
            return 0;
        }))));
    }

    public static void execute(ServerCommandSource source, boolean enable) throws CommandSyntaxException {
        final CommandSyntaxException[] exception = {null};
        if (enable) {
            source.sendFeedback(new LiteralText("Subathon started!"), false);
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
            source.sendFeedback(new LiteralText("Subathon stopped. The modifier is still being applied!"), true);
            source.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                try {
                    executeTitle(source, player, new LiteralText("\u00a7c\u00a7lSubathon stopped!"), TitleS2CPacket::new);
                    executeTitle(source, player, new LiteralText("\u00a74The modifier is still being applied!"), SubtitleS2CPacket::new);
                    player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.MASTER, 100, 0.8f);
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
        }
    }

    private static void executeReload(ServerCommandSource source) throws CommandSyntaxException {
        source.sendFeedback(new LiteralText("Reloading configurations."), true);
        Subathon.config.loadConfigs();
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player) player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
        source.sendFeedback(new LiteralText("Configurations reloaded"), true);
    }

    public static void executeSetModifier(ServerCommandSource source, int amount) {
        source.sendFeedback(new LiteralText("Setting modifier to " + amount), true);
        Bot.setCounter(amount);
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity player) player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 100, 1f);
    }

    public static void executeGetInfo(ServerCommandSource source) throws CommandSyntaxException {
        if (bot == null) {
            source.sendFeedback(new LiteralText("The bot is offline!"), false);
            return;
        }
        source.sendFeedback(new LiteralText("Getting information, please wait..."), false);
        final Map<String, Object> validate = bot.validate();
        if (validate == null) {
            source.sendFeedback(new LiteralText("Failed to validate, this is not good!"), false);
            execute(source, false);
            return;
        }
        source.sendFeedback(new LiteralText("\n\n"), false);
        source.sendFeedback(new LiteralText("==========================================="), false);
        source.sendFeedback(new LiteralText(String.format("    Client ID: §k%s", validate.get("client_id"))), false);
        source.sendFeedback(new LiteralText(String.format("    Login: %s", validate.get("login"))), false);
        source.sendFeedback(new LiteralText(String.format("    Scopes: %s", ((List<?>) validate.get("scopes")).stream().map(Object::toString).collect(Collectors.joining(" ")))), false);
        source.sendFeedback(new LiteralText(String.format("    User Id: %s", validate.get("user_id"))), false);
        source.sendFeedback(new LiteralText(String.format("    Expires In: %s", validate.get("expires_in"))), false);
        source.sendFeedback(new LiteralText("==========================================="), false);
    }

    private static void openLink(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && player.getUuid() == client.player.getUuid()) {
                player.sendMessage(new LiteralText("Opening authentication page"), false);
                Util.getOperatingSystem().open(Bot.getAuthenticationUrl(List.of("channel:read:subscriptions"), null));
            }
        } catch (Exception e) {
            source.sendError(new LiteralText("Only players can run this command!"));
        }
    }

    private static void executeTitle(ServerCommandSource source, ServerPlayerEntity player, Text title, Function<Text, Packet<?>> constructor) throws CommandSyntaxException {
        player.networkHandler.sendPacket(constructor.apply(Texts.parse(source, title, player, 0)));
    }
}
