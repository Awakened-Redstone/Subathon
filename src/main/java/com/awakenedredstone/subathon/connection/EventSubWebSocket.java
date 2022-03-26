package com.awakenedredstone.subathon.connection;

import com.awakenedredstone.subathon.Subathon;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.twitch4j.common.util.ThreadUtils;
import com.github.twitch4j.common.util.TimeUtils;
import com.github.twitch4j.common.util.TypeConvert;
import com.github.twitch4j.pubsub.domain.PubSubResponse;
import com.github.twitch4j.pubsub.enums.PubSubType;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFrame;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.awakenedredstone.subathon.Subathon.LOGGER;
import static com.awakenedredstone.subathon.Subathon.webSocketFactory;

public class EventSubWebSocket {
    private volatile long lastPing = TimeUtils.getCurrentTimeInMillis() - 4 * 60 * 1000;
    private volatile long lastPong = TimeUtils.getCurrentTimeInMillis();
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private volatile Future<?> backoffClearer;
    private volatile WebSocket webSocket;
    private final Object lock = new Object[0];
    private final String URL = "wss://charmed-glacier-henley.glitch.me";
    private final ScheduledExecutorService taskExecutor = ThreadUtils.getDefaultScheduledThreadPoolExecutor("subathon-eventsub-" + RandomStringUtils.random(4, true, true), 1);
    private final ExponentialBackoffStrategy backoff = ExponentialBackoffStrategy.builder()
            .immediateFirst(false)
            .baseMillis(Duration.ofSeconds(1).toMillis())
            .jitter(true)
            .multiplier(2.0)
            .maximumBackoff(Duration.ofMinutes(2).toMillis())
            .build();

    private void createWebSocket() {
        synchronized (this.lock) {
            try {
                // WebSocket
                this.webSocket = webSocketFactory.createSocket(URL);
                this.webSocket.setPingInterval(15000);
                this.webSocket.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.82 Safari/537.36");

                // WebSocket Listeners
                this.webSocket.clearListeners();
                this.webSocket.addListener(new WebSocketAdapter() {

                    @Override
                    public void onConnected(WebSocket ws, Map<String, List<String>> headers) {
                        LOGGER.info("Connecting to EventSub WebSocket {}", URL);

                        // Connection Success
                        connectionState = ConnectionState.CONNECTED;
                        backoffClearer = taskExecutor.schedule(() -> {
                            if (connectionState == ConnectionState.CONNECTED)
                                backoff.reset();
                        }, 30, TimeUnit.SECONDS);

                        LOGGER.info("Connected to EventSub WebSocket {}", URL);

                        //TODO: Send auth message so the server sends the data to this websocket connection
                        JsonObject json = new JsonObject();
                        json.add("type", new JsonPrimitive("AUTH"));
                        JsonObject data = new JsonObject();
                        data.add("token", new JsonPrimitive("")); //TODO: get "token" from AuthData
                        data.add("broadcaster_user_login", new JsonPrimitive("")); //TODO: get "login" from AuthData
                        data.add("broadcaster_user_id", new JsonPrimitive("")); //TODO: get "user_id" from AuthData
                        json.add("data", data);
                        webSocket.sendText(Subathon.GSON.toJsonTree(json).getAsString());
                    }

                    @Override
                    public void onTextMessage(WebSocket ws, String text) {
                        {
                            try {
                                LOGGER.info("Received WebSocketMessage: " + text);

                                // parse message
                                PubSubResponse message = TypeConvert.jsonToObject(text, PubSubResponse.class);
                                if (message.getType().equals(PubSubType.MESSAGE)) {
                                    String topic = message.getData().getTopic();
                                    String[] topicParts = StringUtils.split(topic, '.');
                                    String topicName = topicParts[0];
                                    String lastTopicIdentifier = topicParts[topicParts.length - 1];
                                    String type = message.getData().getMessage().getType();
                                    JsonNode msgData = message.getData().getMessage().getMessageData();
                                    String rawMessage = message.getData().getMessage().getRawMessage();

                                    // Handle Messages
                                    //TODO: Handle messages
                                } else if (message.getType().equals(PubSubType.RESPONSE)) {

                                    // topic subscription success or failed, response to listen command
                                    // System.out.println(message.toString());
                                    if (message.getError().length() > 0) {
                                        if (message.getError().equalsIgnoreCase("ERR_BADAUTH")) {
                                            LOGGER.error("EventSub WebSocket: You used a invalid oauth token to subscribe to the topic. Please use a token that is authorized for the specified channel.");
                                        } else {
                                            LOGGER.error("EventSub WebSocket: Failed to subscribe to topic - [" + message.getError() + "]");
                                        }
                                    }

                                } else if (message.getType().equals(PubSubType.PONG)) {
                                    LOGGER.debug("EventSub WebSocket: Received PONG response!");
                                    lastPong = TimeUtils.getCurrentTimeInMillis();
                                } else if (message.getType().equals(PubSubType.RECONNECT)) {
                                    LOGGER.warn("EventSub WebSocket: Server instance we're connected to will go down for maintenance soon, reconnecting to obtain a new connection!");
                                    reconnect();
                                } else {
                                    // unknown message
                                    LOGGER.debug("EventSub WebSocket: Unknown Message Type: " + message);
                                }
                            } catch (Exception ex) {
                                LOGGER.warn("EventSub WebSocket: Unparsable Message: " + text + " - [" + ex.getMessage() + "]");
                                ex.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                        if (!connectionState.equals(ConnectionState.DISCONNECTING)) {
                            LOGGER.info("Connection to EventSub WebSocket lost (WebSocket)! Retrying soon ...");

                            // connection lost - reconnecting
                            if (backoffClearer != null) backoffClearer.cancel(false);
                            taskExecutor.schedule(() -> reconnect(), backoff.get(), TimeUnit.MILLISECONDS);
                        } else {
                            connectionState = ConnectionState.DISCONNECTED;
                            LOGGER.info("Disconnected from EventSub WebSocket (WebSocket)!");
                        }
                    }

                });


            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
    }

    public void connect() {
        synchronized (this.lock) {
            if (this.connectionState.equals(ConnectionState.DISCONNECTED) || this.connectionState.equals(ConnectionState.RECONNECTING)) {
                try {
                    this.connectionState = ConnectionState.CONNECTING;
                    this.createWebSocket();
                    this.lastPong = TimeUtils.getCurrentTimeInMillis();
                    this.lastPing = this.lastPong - 240000L;
                    this.webSocket.connect();
                } catch (Exception var14) {
                    LOGGER.error("EventSub WebSocket: Connection to EventSub WebSocket failed: {} - Retrying ...", var14.getMessage());
                    if (this.backoffClearer != null) {
                        try {
                            this.backoffClearer.cancel(false);
                        } catch (Exception ignored) {}
                    }

                    try {
                        this.backoff.sleep();
                    } catch (Exception ignored) {} finally {
                        this.reconnect();
                    }
                }
            }
        }
    }

    public void disconnect() {
        synchronized (this.lock) {
            if (this.connectionState.equals(ConnectionState.CONNECTED)) {
                this.connectionState = ConnectionState.DISCONNECTING;
            }

            this.connectionState = ConnectionState.DISCONNECTED;
            if (this.webSocket != null) {
                this.webSocket.clearListeners();
                this.webSocket.disconnect();
                this.webSocket = null;
            }

            if (this.backoffClearer != null) {
                this.backoffClearer.cancel(false);
            }

        }
    }

    public void reconnect() {
        synchronized (this.lock) {
            this.connectionState = ConnectionState.RECONNECTING;
            this.disconnect();
            this.connect();
        }
    }
}
