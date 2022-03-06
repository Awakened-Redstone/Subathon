package com.awakenedredstone.subathon.config;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.json.JsonHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.*;
import java.util.stream.Collectors;

public class Config {

    private ConfigData configData;
    private final File configFile = new File(getConfigDirectory(), "subathon.json");
    @SuppressWarnings("FieldCanBeLocal")
    private final byte configVersion = 1;

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
        JsonObject json = new JsonObject();
        json.add("version", new JsonPrimitive(configVersion));
        json.add("clientId", new JsonPrimitive(""));
        json.add("clientSecret", new JsonPrimitive(""));
        json.add("effect", new JsonPrimitive("JUMP"));
        json.add("channelName", new JsonPrimitive(""));
        json.add("effectAmplifier", new JsonPrimitive(0.1));
        json.add("subModifier", new JsonPrimitive(1));
        return json;
    }

    public ConfigData getConfigData() {
        return configData;
    }

    public boolean save() {
        return JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(configData).getAsJsonObject(), configFile);
    }
}

