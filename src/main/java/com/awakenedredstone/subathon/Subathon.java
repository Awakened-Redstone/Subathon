package com.awakenedredstone.subathon;

import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.config.Config;
import com.awakenedredstone.subathon.config.ConfigData;
import com.awakenedredstone.subathon.json.JsonHelper;
import com.awakenedredstone.subathon.potions.SubathonStatusEffect;
import com.awakenedredstone.subathon.twitch.EventListener;
import com.awakenedredstone.subathon.twitch.TwitchIntegration;
import com.awakenedredstone.subathon.util.MessageUtils;
import com.awakenedredstone.subathon.util.SubathonData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neovisionaries.ws.client.WebSocketFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Subathon implements ModInitializer {
    public static String MOD_ID = "subathon";
    public static MinecraftServer server;
    public static TwitchIntegration integration = new TwitchIntegration();

    public static final StatusEffect SUBATHON_EFFECT = new SubathonStatusEffect();
    public static final EventListener eventListener = new EventListener();
    public static final WebSocketFactory webSocketFactory = new WebSocketFactory();
    public static final Config config = new Config();
    public static final Logger LOGGER = LoggerFactory.getLogger("Subathon");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private int potionTick = 10;

    private static final File configFile = new File(config.getConfigDirectory(), "subathon.json");

    @Override
    public void onInitialize() {
        Registry.register(Registry.STATUS_EFFECT, new Identifier(MOD_ID, "subathon"), SUBATHON_EFFECT);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (Subathon.integration.data.value != 0 && potionTick-- == 0) {
                potionTick = 10;
                int level = Math.round(integration.data.value / getConfigData().effectMultiplier);
                MessageUtils.broadcast(player -> player.addStatusEffect(new StatusEffectInstance(SUBATHON_EFFECT, potionTick + 5, level - 1, false, false)), "apply_potion");
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sender.sendPacket(new Identifier(Subathon.MOD_ID, "has_mod"), PacketByteBufs.create());

            if (handler.player.hasPermissionLevel(1)) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(integration.isRunning ? 1 : 0);
                sender.sendPacket(new Identifier(Subathon.MOD_ID, "bot_status"), buf);
            }

            if (integration.data != null) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeFloat(integration.data.value / getConfigData().effectMultiplier);
                sender.sendPacket(new Identifier(Subathon.MOD_ID, "value"), buf);
            }
        });
        ServerLifecycleEvents.SERVER_STARTING.register(this::registerCommands);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> registerCommands(server));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Subathon.server = server;

            File file = server.getSavePath(WorldSavePath.ROOT).resolve("subathon_data.json").toFile();
            if (!file.exists()) JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(new SubathonData()).getAsJsonObject(), file);
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                SubathonData data = Subathon.GSON.fromJson(reader, SubathonData.class);
                if (getConfigData().runAtServerStart) integration.start(data);
                else integration.data = data;
            } catch (IOException e) {
                if (getConfigData().runAtServerStart) Subathon.LOGGER.error("Failed to start the integration!", e);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> integration.stop(false));
        generateConfig();
        config.loadConfigs();
    }

    private void registerCommands(MinecraftServer server) {
        SubathonCommand.register(server.getCommandManager().getDispatcher());
    }

    public static ConfigData getConfigData() {
        return config.getConfigData();
    }

    public static void generateConfig() {
        File dir = config.getConfigDirectory();
        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            if (!configFile.exists()) {
                JsonHelper.writeJsonToFile(config.generateDefaultConfig(), configFile);
            }
        }
    }
}
