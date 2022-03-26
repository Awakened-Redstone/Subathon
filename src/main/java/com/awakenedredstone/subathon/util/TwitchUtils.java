package com.awakenedredstone.subathon.util;

import com.awakenedredstone.subathon.Subathon;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.awakenedredstone.subathon.Subathon.LOGGER;

public class TwitchUtils {
    /**
     * Get Authentication Url
     *
     * @param scopes requested scopes
     * @param state  state - csrf protection
     * @return url
     */
    public static String getAuthenticationUrl(List<Object> scopes, String state) {
        return getAuthenticationUrl("https://subathon-mod.glitch.me", scopes, state);
    }

    /**
     * Get Authentication Url
     *
     * @param redirectUrl overwrite the redirect url with a custom one
     * @param scopes      requested scopes
     * @param state       state - csrf protection
     * @return url
     */
    public static String getAuthenticationUrl(String redirectUrl, List<Object> scopes, String state) {
        if (state == null) {
            state = "twitch|" + UUID.randomUUID();
        }
        return String.format("%s?response_type=%s&client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                "https://id.twitch.tv/oauth2/authorize",
                URLEncoder.encode("token", StandardCharsets.UTF_8),
                URLEncoder.encode("7scb6ymkzkne4uh5nuyg7nf7j5v213", StandardCharsets.UTF_8),
                URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8),
                scopes.stream().map(Object::toString).collect(Collectors.joining("%20")),
                URLEncoder.encode(state, StandardCharsets.UTF_8));
    }

    public static JsonObject validate(String token) {
        try {
            OkHttpClient client = new OkHttpClient();
            ObjectMapper objectMapper = new ObjectMapper();
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse("https://id.twitch.tv/oauth2/validate")).newBuilder();

            Request request = new Request.Builder()
                    .url(urlBuilder.build().toString())
                    .header("Authorization", String.format("Bearer %s", token))
                    .build();

            Response response = client.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();

            return Subathon.GSON.toJsonTree(responseBody).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("Failed to validate token!", e);
        }
        return null;
    }

}
