package com.awakenedredstone.subathon.util;

import com.awakenedredstone.subathon.Subathon;
import com.google.gson.JsonObject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Objects;

public class TwitchUtils {
    public static JsonObject getChannelData() {
        try {
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(String.format("https://api.ivr.fi/twitch/resolve/%s", Subathon.getConfigData().channelName))).newBuilder();

            Request request = new Request.Builder()
                    .url(urlBuilder.build().toString())
                    .build();

            Response response = new OkHttpClient().newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();

            JsonObject responseJson = Subathon.GSON.fromJson(responseBody, JsonObject.class);
            JsonObject json = new JsonObject();
            json.add("channelId", responseJson.get("id"));
            json.add("displayName", responseJson.get("displayName"));
            return json;
        } catch (Exception e) {
            return null;
        }
    }

}
