package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.ui.WarningScreen;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class AuthUtils {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Environment(EnvType.CLIENT)
    public static CompletableFuture<AuthResult> requestAuth() {
        CompletableFuture<AuthResult> future = new CompletableFuture<>();
        MinecraftClient client = MinecraftClient.getInstance();

        String state = UUID.randomUUID().toString();

        Util.getOperatingSystem().open(getAuthenticationUrl(List.of("channel:read:subscriptions", "bits:read", "channel:manage:redemptions"), state));

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);
            executor.schedule(() -> server.stop(3), 60, TimeUnit.SECONDS);
            server.createContext("/", exchange -> {
                String response = "<script>window.location = window.location.pathname + \"auth\" + new URL(document.URL.replace(\"#\", \"?\")).search;</script>";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            });

            server.createContext("/auth", exchange -> {
                String queryString = exchange.getRequestURI().getQuery();
                Map<String, String> query = new HashMap<>();
                if (queryString != null) {
                    for (String s : queryString.split("&")) {
                        String[] split = s.split("=");
                        query.put(split[0], split[1]);
                    }
                }

                if (query.containsKey("error")) {
                    if (query.get("error").equals("access_denied")) {
                        future.complete(new AuthResult(Optional.empty(), AuthResult.ErrorType.REFUSED, null));
                        sendResponse(exchange);
                        server.stop(3);
                        return;
                    } else {
                        future.complete(new AuthResult(Optional.empty(), AuthResult.ErrorType.UNKNOWN, null));
                        sendResponse(exchange);
                        server.stop(3);
                        return;
                    }
                }

                if (query.containsKey("access_token")) {
                    if (!query.get("state").equals(state)) {
                        future.complete(new AuthResult(Optional.empty(), AuthResult.ErrorType.INVALID, null));
                        client.setScreen(new WarningScreen());
                        sendResponse(exchange);
                        server.stop(3);
                        return;
                    }

                    future.complete(new AuthResult(Optional.ofNullable(query.get("access_token")), AuthResult.ErrorType.NONE, null));
                    sendResponse(exchange);
                    server.stop(3);
                }

                String response = "Invalid auth!";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            });
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            future.complete(new AuthResult(Optional.empty(), AuthResult.ErrorType.EXCEPTION, e));
        }

        return future;
    }

    /**
     * Get Authentication Url
     *
     * @param scopes requested scopes
     * @param state  state - csrf protection
     * @return url
     */
    public static String getAuthenticationUrl(List<Object> scopes, String state) {
        return getAuthenticationUrl("http://localhost:3000", scopes, state);
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
                URLEncoder.encode("fis0ggy06huug74vseeac1jv3dk6d9", StandardCharsets.UTF_8),
                URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8),
                URLEncoder.encode(scopes.stream().map(Object::toString).collect(Collectors.joining(" ")), StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8));
    }

    private static void sendResponse(HttpExchange exchange) throws IOException {
        String response = "Authentication complete, you can close this tab now";
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public record AuthResult(Optional<String> result, ErrorType error, Throwable exception) {
        public enum ErrorType {
            NONE,
            EXCEPTION,
            REFUSED,
            INVALID,
            UNKNOWN
        }
    }
}
