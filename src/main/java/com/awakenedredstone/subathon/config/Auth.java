package com.awakenedredstone.subathon.config;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.json.JsonHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.util.stream.Collectors;

public class Auth {

    private AuthData authData;
    private final File authFile = new File(getConfigDirectory(), "auth.json");
    @SuppressWarnings("FieldCanBeLocal")
    private final byte configVersion = 1;

    public File getConfigDirectory() {
        return new File(".", "config/subathon");
    }

    public void loadAuth() {
        if (authFile.exists() && authFile.isFile() && authFile.canRead()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(authFile))) {
                String json = reader.lines().collect(Collectors.joining("\n"));
                StringReader stringReader = new StringReader(json);

                authData = Subathon.GSON.fromJson(stringReader, AuthData.class);
            } catch (IOException e) {
                Subathon.LOGGER.error("Failed to load configurations!", e);
                //TODO: Stop mod initialization
            }
        }
    }

    public void fromString(String json) throws JsonSyntaxException {
        authData = Subathon.GSON.fromJson(json.lines().collect(Collectors.joining("\n")), AuthData.class);
    }

    public JsonObject generateDefaultConfig() {
        JsonObject json = new JsonObject();
        json.add("version", new JsonPrimitive(configVersion));
        return json;
    }

    public AuthData getAuthData() {
        return authData;
    }

    public void save() {
        JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(authData).getAsJsonObject(), authFile);
    }
}
