package com.awakenedredstone.subathon;

import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.config.Config;
import com.awakenedredstone.subathon.config.ConfigData;
import com.awakenedredstone.subathon.config.cloth.ClothConfig;
import com.awakenedredstone.subathon.json.JsonHelper;
import com.awakenedredstone.subathon.twitch.Bot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Subathon implements ModInitializer {
    public static String MOD_ID = "suathon";
    public static MinecraftServer server;
    public static Thread thread;
    public static Bot bot;

    public static final Config config = new Config();
    public static final Logger LOGGER = LoggerFactory.getLogger("Subathon");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File configFile = new File(config.getConfigDirectory(), "subathon.json");

    public static ConfigData getConfigData() {
        return config.getConfigData();
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> SubathonCommand.register(server.getCommandManager().getDispatcher()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> SubathonCommand.register(server.getCommandManager().getDispatcher()));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> Subathon.server = server);
        File dir = config.getConfigDirectory();
        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            if (!configFile.exists()) {
                JsonHelper.writeJsonToFile(config.generateDefaultConfig(), configFile);
            }
        }
        config.loadConfigs();
    }

    public static Effect getEffect() {
        try {
            return Effect.valueOf(getConfigData().effect.toUpperCase());
        } catch (IllegalArgumentException exception) {
            if (server != null) server.getCommandSource().sendFeedback(new LiteralText(getConfigData().effect.toUpperCase() + " is not a valid effect"), true);
            LOGGER.error(getConfigData().effect.toUpperCase() + " is not a valid effect");
            return Effect.NONE;
        }
    }
}
