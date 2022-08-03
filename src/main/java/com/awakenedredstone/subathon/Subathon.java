package com.awakenedredstone.subathon;

import com.awakenedredstone.cubecontroller.CubeController;
import com.awakenedredstone.cubecontroller.GameControl;
import com.awakenedredstone.cubecontroller.util.MessageUtils;
import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.config.Config;
import com.awakenedredstone.subathon.config.ConfigData;
import com.awakenedredstone.subathon.events.SubathonEvents;
import com.awakenedredstone.subathon.twitch.EventListener;
import com.awakenedredstone.subathon.twitch.TwitchIntegration;
import com.awakenedredstone.subathon.util.ScheduleUtils;
import com.awakenedredstone.subathon.util.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.neovisionaries.ws.client.WebSocketFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.timer.TimerCallbackSerializer;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Subathon implements ModInitializer {
    public static MinecraftServer server;
    public static ScheduleUtils scheduler;
    public static TwitchIntegration integration = new TwitchIntegration();
    public static CommandBossBar mainProgressBar;
    public static CommandBossBar usersProgressBar;
    public static Queue<Runnable> onNextTick = new LinkedBlockingQueue<>();

    public static final String MOD_ID = "subathon";
    public static final List<Pair<Double, Integer>> subTimers = new ArrayList<>();
    public static final OkHttpClient OKHTTPCLIENT = new OkHttpClient();
    public static final EventListener eventListener = new EventListener();
    public static final WebSocketFactory webSocketFactory = new WebSocketFactory();
    public static final Config config = new Config();
    public static final Logger LOGGER = LoggerFactory.getLogger("Subathon");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final List<EntityType<?>> NON_MISC_ENTITY_TYPES = Registry.ENTITY_TYPE.stream().filter(v -> v.getSpawnGroup() != SpawnGroup.MISC).toList();
    public static final List<EntityType<?>> ENTITY_TYPES = Registry.ENTITY_TYPE.stream().toList();

    @Override
    public void onInitialize() {
        CubeController.registerControl(identifier("chaos"), SubathonEvents.CHAOS, false);

        TimerCallbackSerializer.INSTANCE.registerSerializer(new ScheduleUtils.UpdateControlValue.Serializer());

        registerListeners();

        config.loadOrCreateConfig();
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        SubathonCommand.register(dispatcher);
    }

    private void registerListeners() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (getConfigData().updateTimer > 0 && server.getTicks() % getConfigData().updateTimer == 0) {
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
            }

            //Send the server ticks to the players every second
            if (getConfigData().updateTimer > 0 && server.getTicks() % getConfigData().updateTimer >= getConfigData().updateTimer - 3) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(server.getTicks() % getConfigData().updateTimer + 20);
                MessageUtils.broadcastPacket(identifier("next_update"), buf);
            }

            while (!onNextTick.isEmpty()) onNextTick.poll().run();
            scheduler.tick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            mainProgressBar.addPlayer(handler.player);
            usersProgressBar.addPlayer(handler.player);

            sender.sendPacket(new Identifier(Subathon.MOD_ID, "has_mod"), PacketByteBufs.create());

            if (handler.player.hasPermissionLevel(1)) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeEnumConstant(Subathon.integration.status);
                sender.sendPacket(new Identifier(Subathon.MOD_ID, "bot_status"), buf);
            }
        });

        CommandRegistrationCallback.EVENT.register(this::registerCommands);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Subathon.integration = new TwitchIntegration();
            Subathon.server = server;
            Subathon.scheduler = new ScheduleUtils();

            server.getOverworld().getPersistentStateManager().getOrCreate(SubathonData::fromNbt, SubathonData::new, MOD_ID);

            mainProgressBar = server.getBossBarManager().add(new Identifier(MOD_ID, "loading_progress_main"), Text.translatable("text.subathon.load.main", "", 0));
            mainProgressBar.setColor(BossBar.Color.RED);
            mainProgressBar.setMaxValue(6);
            mainProgressBar.setValue(0);
            mainProgressBar.setVisible(false);

            usersProgressBar = server.getBossBarManager().add(new Identifier(MOD_ID, "loading_progress_users"), Text.translatable("text.subathon.load.users", 0, 0));
            usersProgressBar.setColor(BossBar.Color.RED);
            usersProgressBar.setMaxValue(1);
            usersProgressBar.setValue(0);
            usersProgressBar.setVisible(false);

            if (getConfigData().runAtServerStart) integration.simpleExecutor.schedule(integration::start, 5, TimeUnit.SECONDS);
            Utils.buildMobWeightsCache();
            Utils.buildChaosWeightsCache();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            integration.stop(false);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Subathon.server = null;
            Subathon.scheduler.destroy();
            Subathon.scheduler = null;
        });

        SubathonEvents.CHAOS.register(control -> onNextTick.add(() -> Utils.CHAOS_WEIGHTS_CACHE.next().accept(control)));
    }

    public static ConfigData getConfigData() {
        return config.getConfigData();
    }

    @Deprecated(forRemoval = true)
    public static <T extends Enum<?>> MutableText getEnumDisplayName(String modId, Class<T> clazz, T value) {
        if (!clazz.isEnum()) return Text.literal(String.format("%s is not an enum class!", clazz.getSimpleName())).formatted(Formatting.RED);
        return Text.translatable(String.format("enum.%s.%s.%s", modId, clazz.getSimpleName().toLowerCase(), value.name().toLowerCase()));
    }

    @Deprecated(forRemoval = true)
    public static <T extends Enum<?>> MutableText getEnumDisplayName(String modId, Class<T> clazz, String valueName) {
        if (!clazz.isEnum()) return Text.literal(String.format("%s is not an enum class!", clazz.getSimpleName())).formatted(Formatting.RED);
        return Text.translatable(String.format("enum.%s.%s.%s", modId, clazz.getSimpleName().toLowerCase(), valueName.toLowerCase()));
    }

    public static Identifier identifier(String path) {
        return new Identifier(MOD_ID, path);
    }

    public static boolean shouldInvoke(Identifier identifier) {
        Boolean config = getConfigData().invoke.get(identifier.toString());
        if (config != null) return config;
        else return true;
    }
}
