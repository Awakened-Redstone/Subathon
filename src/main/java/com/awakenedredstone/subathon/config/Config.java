package com.awakenedredstone.subathon.config;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.json.JsonHelper;
import com.google.gson.JsonObject;

import java.io.*;
import java.util.stream.Collectors;

public class Config {

    private ConfigData configData;
    private final File configFile = new File(getConfigDirectory(), "subathon.json");

    public File getConfigDirectory() {
        return new File(".", "config/subathon");
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

    public JsonObject generateDefaultConfig() {
        return Subathon.GSON.toJsonTree(new ConfigData()).getAsJsonObject();
    }

    public ConfigData getConfigData() {
        return configData;
    }

    public void save() {
        JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(configData).getAsJsonObject(), configFile);
    }
}

