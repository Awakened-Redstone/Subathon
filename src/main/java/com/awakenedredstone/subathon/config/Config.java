package com.awakenedredstone.subathon.config;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.json.JsonHelper;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class Config {

    private ConfigData configData;
    private final File configFile = FabricLoader.getInstance().getConfigDir().resolve("subathon.json").toFile();

    public void loadOrCreateConfig() {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir();
            if ((dir.toFile().exists() && dir.toFile().isDirectory()) || dir.toFile().mkdirs()) {
                if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
                    configData = Subathon.GSON.fromJson(new FileReader(configFile), ConfigData.class);
                } else if (!configFile.exists()) {
                    JsonHelper.writeJsonToFile(defaultConfig(), getConfigFile());
                    configData = new ConfigData();
                }
                return;
            }
            Subathon.LOGGER.debug("Subathon configurations loaded");
        } catch (Exception exception) {
            Subathon.LOGGER.error("An error occurred when trying to load the configurations!", exception);
            return;
        }

        Subathon.LOGGER.error("Failed to load the configurations!");
    }

    public void loadConfigs() {
        if (configFile.exists() && configFile.isFile() && configFile.canRead()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String json = reader.lines().collect(Collectors.joining("\n"));
                StringReader stringReader = new StringReader(json);

                configData = Subathon.GSON.fromJson(stringReader, ConfigData.class);
            } catch (IOException e) {
                Subathon.LOGGER.error("Failed to load configurations!", e);
                //TODO: Stop mod initialization
            }
        }
    }

    public JsonObject defaultConfig() {
        return Subathon.GSON.toJsonTree(new ConfigData()).getAsJsonObject();
    }

    public ConfigData getConfigData() {
        return configData;
    }

    public void save() {
        JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(configData).getAsJsonObject(), configFile);
    }

    public File getConfigFile() {
        return configFile;
    }
}

