package com.awakenedredstone.subathon;

import com.awakenedredstone.subathon.commands.DebugCommand;
import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.config.*;
import com.awakenedredstone.subathon.connection.EventSubWebSocket;
import com.awakenedredstone.subathon.json.JsonHelper;
import com.awakenedredstone.subathon.twitch.Bot;
import com.awakenedredstone.subathon.twitch.EventListener;
import com.awakenedredstone.subathon.twitch.TwitchIntegration;
import com.awakenedredstone.subathon.util.SubathonData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neovisionaries.ws.client.WebSocketFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class Subathon implements ModInitializer {
    public static String MOD_ID = "suathon";
    public static MinecraftServer server;
    public static Bot bot;
    public static TwitchIntegration integration = new TwitchIntegration();
    public static final EventListener eventListener = new EventListener();
    public static final WebSocketFactory webSocketFactory = new WebSocketFactory();
    public static final EventSubWebSocket eventSub = new EventSubWebSocket();

    public static final Config config = new Config();
    public static final Auth auth = new Auth();
    public static final Logger LOGGER = LoggerFactory.getLogger("Subathon");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File configFile = new File(config.getConfigDirectory(), "subathon.json");

    public static ConfigData getConfigData() {
        return config.getConfigData();
    }
    //TODO: Support multiple auth data for multiplayer support
    public static AuthData getAuthData() {
        return auth.getAuthData();
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::registerCommands);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> registerCommands(server));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Subathon.server = server;

            if (getAuthData().access_token != null) {
                File file = server.getSavePath(WorldSavePath.ROOT).resolve("subathon_data.json").toFile();
                if (!file.exists()) JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(new SubathonData()).getAsJsonObject(), file);
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        SubathonData data = Subathon.GSON.fromJson(reader, SubathonData.class);
                        integration.start(data);
                    } catch (IOException e) {
                        Subathon.LOGGER.error("Failed to start the integration!", e);
                    }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> integration.stop());
        generateConfig();
        config.loadConfigs();
        auth.loadAuth();
    }

    private void registerCommands(MinecraftServer server) {
        SubathonCommand.register(server.getCommandManager().getDispatcher());
        DebugCommand.register(server.getCommandManager().getDispatcher());
    }

    public static void generateConfig() {
        File dir = config.getConfigDirectory();
        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            if (!configFile.exists()) {
                JsonHelper.writeJsonToFile(config.generateDefaultConfig(), configFile);
            }
        }
    }

    public static Mode getEffect() {
        try {
            return Mode.valueOf(getConfigData().mode.toUpperCase());
        } catch (IllegalArgumentException exception) {
            if (server != null) server.getCommandSource().sendFeedback(new LiteralText(getConfigData().mode.toUpperCase() + " is not a valid effect"), true);
            LOGGER.error(getConfigData().mode.toUpperCase() + " is not a valid effect");
            return Mode.NONE;
        }
    }

    public static void sendPositionedText(ServerPlayerEntity player, Text text, int x, int y, int color, boolean center, boolean shadow) {
        sendPositionedText(player, text, x, y, color, center, shadow, new Random().nextLong());
    }

    public static void sendPositionedText(ServerPlayerEntity player, Text text, int x, int y, int color, boolean center, boolean shadow, long id) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeText(text);
        buf.writeBoolean(shadow);
        buf.writeBoolean(center);
        int[] values = new int[]{x, y, color};
        buf.writeIntArray(values);
        buf.writeLong(id);
        ServerPlayNetworking.send(player, new Identifier(Subathon.MOD_ID, "positioned_text"), buf);
    }
}
