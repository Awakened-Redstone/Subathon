package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.Subathon;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClientPool;
import com.github.twitch4j.TwitchClientPoolBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.eventsub.EventSubTransport;
import com.github.twitch4j.eventsub.EventSubTransportMethod;
import com.github.twitch4j.eventsub.condition.ChannelEventSubCondition;
import com.github.twitch4j.eventsub.events.*;
import com.github.twitch4j.eventsub.socket.TwitchEventSocketPoolPatch;
import com.github.twitch4j.eventsub.socket.events.EventSocketDeleteSubscriptionFailureEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketDeleteSubscriptionSuccessEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionFailureEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionSuccessEvent;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.graphql.internal.FetchCommunityPointsSettingsQuery;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import lombok.Getter;
import lombok.SneakyThrows;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public final class Twitch {
    @Getter
    private static final Twitch instance = new Twitch();

    private final TwitchClientPool clientPool;
    //private static final TwitchChatConnectionPool chatPool;
    //public static final TwitchEventSocketPool eventSub;
    public final TwitchEventSocketPoolPatch eventSub;
    private final Map<String, List<EventSubSubscription>> subscriptions = new HashMap<>();
    private final Map<UUID, LinkedList<String>> subscriptionQueue = new HashMap<>();
    private final Map<UUID, Data> data = new HashMap<>();

    public Map<UUID, Data> getData() {
        return Collections.unmodifiableMap(data);
    }

    public void addData(UUID uuid, Data newData) {
        data.put(uuid, newData);
    }

    public Twitch() {
        boolean isDev = Subathon.COMMON_CONFIGS.useMockApi();

        clientPool = TwitchClientPoolBuilder.builder()
                .withEnableHelix(true)
                .withEnableGraphQL(true)
                .build();

        /*chatPool = TwitchChatConnectionPool.builder()
                .maxSubscriptionsPerConnection(20)
                .build();*/

        var eventSubBuilder = /*TwitchEventSocketPool*/TwitchEventSocketPoolPatch.builder().helix(clientPool.getHelix());
        TwitchHelix helix = null;
        if (isDev) helix = TwitchHelixBuilder.builder().withBaseUrl("http://0.0.0.0:3000").build();
        eventSub = isDev ? eventSubBuilder.baseUrl("ws://0.0.0.0:3000").helix(helix).build() : eventSubBuilder.build();

        eventSub.getEventManager().onEvent(ChannelSubscribeEvent.class, TwitchEvents::onSubscription);
        eventSub.getEventManager().onEvent(ChannelSubscriptionMessageEvent.class, TwitchEvents::onSubscriptionMessage);
        eventSub.getEventManager().onEvent(ChannelSubscriptionGiftEvent.class, TwitchEvents::onSubscriptionGift);
        eventSub.getEventManager().onEvent(ChannelCheerEvent.class, TwitchEvents::onCheer);
        eventSub.getEventManager().onEvent(CustomRewardRedemptionAddEvent.class, TwitchEvents::onRewardRedemption);

        eventSub.getEventManager().onEvent(EventSocketSubscriptionSuccessEvent.class, this::subscriptionSuccess);
        eventSub.getEventManager().onEvent(EventSocketSubscriptionFailureEvent.class, this::subscriptionFailure);
        eventSub.getEventManager().onEvent(EventSocketDeleteSubscriptionSuccessEvent.class, this::subscriptionDeleteSuccess);
        eventSub.getEventManager().onEvent(EventSocketDeleteSubscriptionFailureEvent.class, this::subscriptionDeleteFailure);
    }

    public void connect(String token, ServerPlayerEntity player) {
        if (!subscriptionQueue.computeIfAbsent(player.getUuid(), k -> new LinkedList<>()).isEmpty()) {
            ServerPlayNetworking.send(player, Subathon.id("error"), PacketByteBufs.create().writeString("eventsub.queue"));
            return;
        }

        Subathon.LOGGER.debug("Starting Twitch connection of {}", player.getDisplayName().getString());
        sendStatusPacket("core", ConnectionState.LOADING, ConnectionType.CONNECT, player);

        OAuth2Credential auth = new OAuth2Credential("twitch", token);
        TwitchIdentityProvider identityProvider = new TwitchIdentityProvider(null, null, null);
        Pair<String, String> userData = identityProvider.getAdditionalCredentialInformation(auth).map(v -> new Pair<>(v.getUserId(), v.getUserName())).orElse(null);

        if (userData == null || StringUtils.isBlank(userData.getLeft()) || StringUtils.isBlank(userData.getRight())) {
            Subathon.LOGGER.warn("Twitch connection of {} failed, could not get user information!", player.getDisplayName().getString());

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString("core").writeEnumConstant(ConnectionState.FAILURE).writeEnumConstant(ConnectionType.CONNECT).writeBoolean(true);
            ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
            ServerPlayNetworking.send(player, Subathon.id("connection_fail"), PacketByteBufs.create());
            return;
        }

        getDataFromToken(token).ifPresent(data -> {
            data.channelId = userData.getLeft();
            data.channelName = userData.getRight();
        });

        ServerPlayNetworking.send(player, Subathon.id("account_name"), PacketByteBufs.create().writeString(userData.getRight()));

        List<EventSubSubscription> pool = subscriptions.computeIfAbsent(token, k -> new ArrayList<>());
        LinkedList<String> queue = subscriptionQueue.get(player.getUuid());

        EventSubSubscription subs = SubscriptionTypes.CHANNEL_SUBSCRIBE.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        addSubscription(pool, queue, subs, eventSub.register(auth, subs), player);

        EventSubSubscription resubs = SubscriptionTypes.CHANNEL_SUBSCRIPTION_MESSAGE.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        addSubscription(pool, queue, resubs, eventSub.register(auth, resubs), player);

        EventSubSubscription gifts = SubscriptionTypes.CHANNEL_SUBSCRIPTION_GIFT.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        addSubscription(pool, queue, gifts, eventSub.register(auth, gifts), player);

        EventSubSubscription cheer = SubscriptionTypes.CHANNEL_CHEER.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        addSubscription(pool, queue, cheer, eventSub.register(auth, cheer), player);

        EventSubSubscription redemption = SubscriptionTypes.CHANNEL_POINTS_CUSTOM_REWARD_REDEMPTION_ADD.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).rewardId("").build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        addSubscription(pool, queue, redemption, eventSub.register(auth, redemption), player);
    }

    public void disconnect(String token) {
        new Thread(() -> {
            Subathon.LOGGER.info("Starting disconnect");
            if (token != null && subscriptions.containsKey(token)) {
                Optional<UUID> uuidOptional = getUuidFromToken(token);
                ServerPlayerEntity player = Subathon.server.getPlayerManager().getPlayer(uuidOptional.orElse(null));
                sendStatusPacket("core", ConnectionState.LOADING, ConnectionType.DISCONNECT, player);

                if (uuidOptional.isPresent() && !subscriptionQueue.get(uuidOptional.get()).isEmpty()) {
                    if (player != null) {
                        ServerPlayNetworking.send(player, Subathon.id("error"), PacketByteBufs.create().writeString("eventsub.queue"));
                        return;
                    } else {
                        subscriptionQueue.remove(uuidOptional.get());
                    }
                }

                List<EventSubSubscription> subscriptionList = subscriptions.get(token);
                subscriptionList.forEach(sub -> {
                    boolean unregister = eventSub.unregister(sub);
                    informConnection(sub, unregister ? ConnectionState.LOADING : ConnectionState.UNKNOWN, ConnectionType.DISCONNECT, player);
                    Subathon.LOGGER.info("{} returned {}", sub.getRawType(), unregister);
                    if (unregister && uuidOptional.isPresent()) {
                        subscriptionQueue.get(uuidOptional.get()).add(sub.getRawType());
                    }
                });

                if (uuidOptional.isPresent() && subscriptionQueue.get(uuidOptional.get()).isEmpty() && player != null) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeString("core").writeEnumConstant(ConnectionState.UNKNOWN).writeEnumConstant(ConnectionType.DISCONNECT).writeBoolean(true);
                    ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
                }

                subscriptions.remove(token);
            }
        }).start();
    }

    public void removeConnection(UUID uuid) {
        disconnect(data.remove(uuid).token);
    }

    public void reset() {
        eventSub.getSubscriptions().forEach(eventSub::unregister);
        subscriptions.clear();
        subscriptionQueue.clear();
        data.clear();
    }

    @SneakyThrows
    public void close() {
        subscriptions.clear();
        clientPool.close();
        //chatPool.close();
        eventSub.close();
    }

    @Environment(EnvType.CLIENT)
    public void toggleReward(String token, UUID rewardId, boolean enable) {
        if (rewardId == null) return;
        if (token == null) throw new IllegalArgumentException("Token can not be null!");
        CompletableFuture.runAsync(() -> {
            OAuth2Credential auth = new OAuth2Credential("twitch", token);
            TwitchIdentityProvider identityProvider = new TwitchIdentityProvider(null, null, null);
            String userId = identityProvider.getAdditionalCredentialInformation(auth).map(OAuth2Credential::getUserId).orElseThrow(() -> new IllegalArgumentException("User ID not found"));
            CompletableFuture.supplyAsync(() -> clientPool.getHelix().getCustomRewards(token, userId, Collections.singletonList(rewardId.toString()), true).execute())
                    .thenApply(response -> response.getRewards().get(0))
                    .thenAccept(reward -> clientPool.getHelix().updateCustomReward(token, userId, rewardId.toString(), reward.withIsEnabled(enable)));
        });
    }

    @Environment(EnvType.CLIENT)
    public CompletableFuture<List<FetchCommunityPointsSettingsQuery.CustomReward>> getChannelCustomRewards(String token) {
        CompletableFuture<List<FetchCommunityPointsSettingsQuery.CustomReward>> completableFuture = new CompletableFuture<>();

        completableFuture.completeAsync(() -> {
            OAuth2Credential auth = new OAuth2Credential("twitch", token);
            TwitchIdentityProvider identityProvider = new TwitchIdentityProvider(null, null, null);
            String userName = identityProvider.getAdditionalCredentialInformation(auth).map(OAuth2Credential::getUserName).orElse(null);

            Future<FetchCommunityPointsSettingsQuery.Data> future = clientPool.getGraphQL().fetchChannelPointRewards(null, userName).queue();

            try {
                FetchCommunityPointsSettingsQuery.Channel channel = future.get().channel();
                if (channel == null) {
                    return Collections.emptyList();
                }

                FetchCommunityPointsSettingsQuery.CommunityPointsSettings settings = channel.communityPointsSettings();
                if (settings == null || settings.customRewards() == null) {
                    return Collections.emptyList();
                }

                @SuppressWarnings("DataFlowIssue")
                var rewards = new ArrayList<>(settings.customRewards());
                rewards.sort(Comparator.comparingInt(FetchCommunityPointsSettingsQuery.CustomReward::cost));
                return rewards;
            } catch (Exception e) {
                Subathon.LOGGER.error("Failed to get channel rewards!", e);
                return Collections.emptyList();
            }
        });

        return completableFuture;
    }

    private void sendStatusPacket(String conn, ConnectionState state, ConnectionType type, ServerPlayerEntity player) {
        if (player == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(conn).writeEnumConstant(state).writeEnumConstant(type).writeBoolean(false);
        ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
    }

    private void addSubscription(List<EventSubSubscription> pool, LinkedList<String> queue, EventSubSubscription subscription, boolean success, ServerPlayerEntity player) {
        if (success) {
            pool.add(subscription);
            queue.add(subscription.getRawType());
        }

        sendStatusPacket(getSubscriptionShortName(subscription), success ? ConnectionState.LOADING : ConnectionState.FAILURE, ConnectionType.CONNECT, player);
    }

    private void subscriptionSuccess(EventSocketSubscriptionSuccessEvent event) {
        getUuidFromChannelId(((ChannelEventSubCondition) event.getSubscription().getCondition()).getBroadcasterUserId())
                .ifPresentOrElse(
                        uuid -> processSubscription(uuid, event.getSubscription(), ConnectionState.SUCCESS),
                        () -> Subathon.LOGGER.error("Failed to get user of a connection")
                );
    }

    private void subscriptionFailure(EventSocketSubscriptionFailureEvent event) {
        getUuidFromChannelId(((ChannelEventSubCondition) event.getSubscription().getCondition()).getBroadcasterUserId())
                .ifPresentOrElse(
                        uuid -> processSubscription(uuid, event.getSubscription(), ConnectionState.FAILURE),
                        () -> Subathon.LOGGER.error("Failed to get user of a connection")
                );
    }

    private void processSubscription(UUID uuid, EventSubSubscription subscription, ConnectionState conn) {
        ServerPlayerEntity player = Subathon.server.getPlayerManager().getPlayer(uuid);
        if (player == null) {
            Subathon.LOGGER.error("Failed to get player with UUID {}, twitch link may be broken until the server restarts!", uuid.toString());
            return;
        }
        var queue = subscriptionQueue.get(uuid);
        queue.remove(subscription.getRawType());

        informConnection(subscription, conn, ConnectionType.CONNECT, player);

        Optional<Data> data;
        if (queue.isEmpty() && (data = getDataFromUuid(uuid)).isPresent()) {
            boolean isUserConnected = (/*(TwitchEventSocketPoolAccessor) (Object)*/ eventSub).getPoolByUserId().containsKey(data.get().channelId);
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString("core").writeEnumConstant(isUserConnected ? ConnectionState.SUCCESS : ConnectionState.FAILURE).writeEnumConstant(ConnectionType.CONNECT).writeBoolean(true);
            ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);

            if (!isUserConnected) {
                Subathon.LOGGER.warn("Twitch connection of {} failed, final connection check failed!", player.getDisplayName().getString());
                ServerPlayNetworking.send(player, Subathon.id("connection_fail"), PacketByteBufs.create());
            }
        }
    }

    public void subscriptionDeleteSuccess(EventSocketDeleteSubscriptionSuccessEvent event) {
        getUuidFromChannelId(((ChannelEventSubCondition) event.getSubscription().getCondition()).getBroadcasterUserId())
                .ifPresentOrElse(
                        uuid -> processDeletedSubscription(uuid, event.getSubscription(), ConnectionState.OFFLINE),
                        () -> Subathon.LOGGER.error("Failed to get user of a connection")
                );
    }

    public void subscriptionDeleteFailure(EventSocketDeleteSubscriptionFailureEvent event) {
        getUuidFromChannelId(((ChannelEventSubCondition) event.getSubscription().getCondition()).getBroadcasterUserId())
                .ifPresentOrElse(
                        uuid -> processDeletedSubscription(uuid, event.getSubscription(), ConnectionState.FAILURE),
                        () -> Subathon.LOGGER.error("Failed to get user of a connection")
                );
    }

    private void processDeletedSubscription(@NotNull UUID uuid, @NotNull EventSubSubscription subscription, @NotNull ConnectionState state) {
        ServerPlayerEntity player = Subathon.server.getPlayerManager().getPlayer(uuid);
        if (player == null) {
            Subathon.LOGGER.error("Failed to get player with UUID {}, twitch link may be broken until the server restarts!", uuid);
            return;
        }

        var queue = subscriptionQueue.get(uuid);
        boolean removed = queue.remove(subscription.getRawType());
        if (!removed) {
            Subathon.LOGGER.error("{} was deleted ({}) but never added to the queue", subscription.getRawType(), state.name());
        }

        informConnection(subscription, state, ConnectionType.DISCONNECT, player);

        if (queue.isEmpty()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString("core").writeEnumConstant(ConnectionState.OFFLINE).writeEnumConstant(ConnectionType.DISCONNECT).writeBoolean(true);
            ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
        }
    }

    private void informConnection(@NotNull EventSubSubscription subscription, @NotNull ConnectionState state, ConnectionType type, @Nullable ServerPlayerEntity player) {
        if (player == null) return;

        sendStatusPacket(getSubscriptionShortName(subscription), state, type, player);
    }

    private String getSubscriptionShortName(@NotNull EventSubSubscription subscription) {
        return switch (subscription.getRawType()) {
            case "channel.subscribe" -> "subs";
            case "channel.subscription.message" -> "resubs";
            case "channel.subscription.gift" -> "gifts";
            case "channel.cheer" -> "bits";
            case "channel.channel_points_custom_reward_redemption.add" -> "channel_points";
            default -> "";
        };
    }

    private Optional<Data> getDataFromToken(String token) {
        return data.values().stream().filter(v -> v.token.equals(token)).findFirst();
    }

    Optional<UUID> getUuidFromChannelId(String id) {
        return data.entrySet().stream().filter(v -> v.getValue().channelId.equals(id)).map(Map.Entry::getKey).findFirst();
    }

    Optional<UUID> getUuidFromToken(String id) {
        return data.entrySet().stream().filter(v -> v.getValue().token.equals(id)).map(Map.Entry::getKey).findFirst();
    }

    Optional<Data> getDataFromUuid(UUID uuid) {
        return data.entrySet().stream().filter(v -> v.getKey().equals(uuid)).map(Map.Entry::getValue).findFirst();
    }

    Optional<Pair<UUID, Data>> getPairFromChannelId(String id) {
        return data.entrySet().stream().filter(v -> v.getValue().channelId.equals(id)).map(v -> new Pair<>(v.getKey(), v.getValue())).findFirst();
    }

    public enum ConnectionState {
        OFFLINE,
        SUCCESS,
        FAILURE,
        UNKNOWN,
        LOADING
    }

    public enum ConnectionType {
        CONNECT,
        DISCONNECT
    }

    public static final class Data {
        private final String token;
        private String channelId = null;
        private String channelName = null;
        private UUID rewardId = null;

        public Data(String token) {
            this.token = token;
        }

        public void printData() {
            Subathon.LOGGER.error("Twitch$Data info:\nReward ID: {}\nChannel ID:{}\nChannel name:{}", rewardId, channelId, channelName);
        }

        public Optional<UUID> getRewardId() {
            return Optional.ofNullable(rewardId);
        }

        public void setRewardId(UUID rewardId) {
            this.rewardId = rewardId;
        }

        public String getChannelId() {
            return channelId;
        }

        public String getChannelName() {
            return channelName;
        }
    }
}
