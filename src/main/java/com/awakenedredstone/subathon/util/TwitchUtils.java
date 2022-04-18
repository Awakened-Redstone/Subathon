package com.awakenedredstone.subathon.util;

import com.awakenedredstone.subathon.Subathon;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

public class TwitchUtils {
    public static void getChannelData(String channelName, Consumer<JsonObject> consumer) {
        try {
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(String.format("https://api.ivr.fi/twitch/resolve/%s", channelName))).newBuilder();

            Request request = new Request.Builder().url(urlBuilder.build().toString()).build();

            Subathon.OKHTTPCLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    consumer.accept(null);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    String responseBody = Objects.requireNonNull(response.body()).string();

                    JsonObject responseJson = Subathon.GSON.fromJson(responseBody, JsonObject.class);
                    JsonObject json = new JsonObject();
                    if (responseJson.get("status").getAsInt() != 200) {
                        consumer.accept(null);
                        return;
                    }
                    json.add("channelId", responseJson.get("id"));
                    json.add("displayName", responseJson.get("displayName"));
                    consumer.accept(json);
                }
            });
        } catch (Exception e) {
            consumer.accept(null);
        }
    }

}
