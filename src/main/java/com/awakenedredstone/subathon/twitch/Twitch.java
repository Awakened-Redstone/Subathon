package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.mixin.twitch4j.TwitchEventSocketPoolAccessor;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClientPool;
import com.github.twitch4j.TwitchClientPoolBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.eventsub.EventSubTransport;
import com.github.twitch4j.eventsub.EventSubTransportMethod;
import com.github.twitch4j.eventsub.condition.ChannelEventSubCondition;
import com.github.twitch4j.eventsub.events.*;
import com.github.twitch4j.eventsub.socket.TwitchEventSocketPool;
import com.github.twitch4j.eventsub.socket.events.EventSocketDeleteSubscriptionFailureEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionFailureEvent;
import com.github.twitch4j.eventsub.socket.events.EventSocketSubscriptionSuccessEvent;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.graphql.internal.FetchCommunityPointsSettingsQuery;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
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

public class Twitch {
    private static final TwitchClientPool clientPool;
    //private static final TwitchChatConnectionPool chatPool;
    public static final TwitchEventSocketPool eventSub;
    private static final Map<String, List<EventSubSubscription>> subscriptions = new HashMap<>();
    private static final Map<UUID, LinkedList<String>> subscriptionQueue = new HashMap<>();
    public static final Map<UUID, Data> data = new HashMap<>();

    public static void init() {
        eventSub.getEventManager().onEvent(EventSocketSubscriptionSuccessEvent.class, Twitch::subscriptionSuccess);
        eventSub.getEventManager().onEvent(EventSocketSubscriptionFailureEvent.class, Twitch::subscriptionFailure);
        eventSub.getEventManager().onEvent(EventSocketDeleteSubscriptionFailureEvent.class, Twitch::subscriptionDeleteFail);
    }

    public static void connect(String token, ServerPlayerEntity player) {
        Subathon.LOGGER.info("Starting Twitch connection of {}", player.getDisplayName().getString());
        sendStatusPacket("core", ConnectionState.LOADING, ConnectionType.CONNECT, player);

        OAuth2Credential auth = new OAuth2Credential("twitch", token);
        TwitchIdentityProvider identityProvider = new TwitchIdentityProvider(null, null, null);
        Pair<String, String> userData = identityProvider.getAdditionalCredentialInformation(auth).map(v -> new Pair<>(v.getUserId(), v.getUserName())).orElse(null);

        if (userData == null || StringUtils.isBlank(userData.getLeft()) || StringUtils.isBlank(userData.getRight())) {
            Subathon.LOGGER.warn("Twitch connection of {} failed, could not get user information!", player.getDisplayName().getString());

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString("core").writeEnumConstant(ConnectionState.FAILURE).writeBoolean(true);
            ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
            ServerPlayNetworking.send(player, Subathon.id("connection_fail"), PacketByteBufs.create());
            return;
        }

        getDataFromToken(token).ifPresent(data -> {
            data.channelId = userData.getLeft();
            data.channelName = userData.getRight();
        });

        ServerPlayNetworking.send(player, Subathon.id("account_name"), PacketByteBufs.create().writeString(userData.getRight()));

        ArrayList<EventSubSubscription> pool = new ArrayList<>();
        subscriptions.put(token, pool);

        LinkedList<String> queue = new LinkedList<>();
        subscriptionQueue.put(player.getUuid(), queue);

        EventSubSubscription subs = SubscriptionTypes.CHANNEL_SUBSCRIBE.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        addSubscription(pool, queue, subs, eventSub.register(auth, subs));
        sendStatusPacket("subs", ConnectionState.LOADING, ConnectionType.CONNECT, player);

        EventSubSubscription resubs = SubscriptionTypes.CHANNEL_SUBSCRIPTION_MESSAGE.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        addSubscription(pool, queue, resubs, eventSub.register(auth, resubs));
        sendStatusPacket("resubs", ConnectionState.LOADING, ConnectionType.CONNECT, player);

        EventSubSubscription gifts = SubscriptionTypes.CHANNEL_SUBSCRIPTION_GIFT.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        addSubscription(pool, queue, gifts, eventSub.register(auth, gifts));
        sendStatusPacket("gifts", ConnectionState.LOADING, ConnectionType.CONNECT, player);

        EventSubSubscription cheer = SubscriptionTypes.CHANNEL_CHEER.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        addSubscription(pool, queue, cheer, eventSub.register(auth, cheer));
        sendStatusPacket("bits", ConnectionState.LOADING, ConnectionType.CONNECT, player);

        EventSubSubscription redemption = SubscriptionTypes.CHANNEL_POINTS_CUSTOM_REWARD_REDEMPTION_ADD.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        addSubscription(pool, queue, redemption, eventSub.register(auth, redemption));
        sendStatusPacket("channel_points", ConnectionState.LOADING, ConnectionType.CONNECT, player);
    }

    public static void disconnect(String token) {
        new Thread(() -> {
            if (token != null && subscriptions.containsKey(token)) {
                ServerPlayerEntity player = Subathon.server.getPlayerManager().getPlayer(getUuidFromToken(token).orElse(null));
                sendStatusPacket("core", ConnectionState.OFFLINE, ConnectionType.DISCONNECT, player);
                List<EventSubSubscription> subscriptionList = subscriptions.get(token);
                List<String> strings = subscriptionList.stream().map(EventSubSubscription::getId).toList();
                eventSub.getSubscriptions().stream().filter(v -> strings.contains(v.getId())).forEach(sub -> {
                    boolean unregister = eventSub.unregister(sub);
                    informConnection(sub, unregister ? ConnectionState.OFFLINE : ConnectionState.UNKNOWN, ConnectionType.DISCONNECT, player);
                });
                subscriptions.remove(token);
            }
        }).start();
    }

    public static void reset() {
        eventSub.getSubscriptions().forEach(eventSub::unregister);
        subscriptions.clear();
    }

    public static void nuke() {
        subscriptions.clear();
        clientPool.close();
        //chatPool.close();
        try {
            eventSub.close();
        } catch (Exception ignored) {/**/}
    }

    @Environment(EnvType.CLIENT)
    public static void toggleReward(String token, UUID rewardId, boolean enable) {
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
    public static CompletableFuture<List<FetchCommunityPointsSettingsQuery.CustomReward>> getChannelCustomRewards(String token) {
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

    private static void sendStatusPacket(String conn, ConnectionState state, ConnectionType type, ServerPlayerEntity player) {
        if (player == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(conn).writeEnumConstant(state).writeEnumConstant(type).writeBoolean(false);
        ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
    }

    private static void addSubscription(List<EventSubSubscription> pool, LinkedList<String> queue, EventSubSubscription subscription, boolean success) {
        if (success) {
            pool.add(subscription);
            queue.add(subscription.getRawType());
        }
    }

    private static void subscriptionSuccess(EventSocketSubscriptionSuccessEvent event) {
        getUuidFromChannelId(((ChannelEventSubCondition) event.getSubscription().getCondition()).getBroadcasterUserId())
                .ifPresentOrElse(
                        uuid -> processSubscription(uuid, event.getSubscription(), ConnectionState.SUCCESS),
                        () -> Subathon.LOGGER.error("Failed to get user of a connection")
                );
    }

    private static void subscriptionFailure(EventSocketSubscriptionFailureEvent event) {
        getUuidFromChannelId(((ChannelEventSubCondition) event.getSubscription().getCondition()).getBroadcasterUserId())
                .ifPresentOrElse(
                        uuid -> processSubscription(uuid, event.getSubscription(), ConnectionState.FAILURE),
                        () -> Subathon.LOGGER.error("Failed to get user of a connection")
                );
    }

    private static void processSubscription(UUID uuid, EventSubSubscription subscription, ConnectionState conn) {
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
            boolean isUserConnected = ((TwitchEventSocketPoolAccessor) (Object) eventSub).getPoolByUserId().containsKey(data.get().channelId);
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString("core").writeEnumConstant(isUserConnected ? ConnectionState.SUCCESS : ConnectionState.FAILURE).writeEnumConstant(ConnectionType.CONNECT).writeBoolean(true);
            ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);

            if (!isUserConnected) {
                Subathon.LOGGER.warn("Twitch connection of {} failed, final connection check failed!", player.getDisplayName().getString());
                ServerPlayNetworking.send(player, Subathon.id("connection_fail"), PacketByteBufs.create());
            }
        }
    }

    public static void subscriptionDeleteFail(EventSocketDeleteSubscriptionFailureEvent event) {
        getUuidFromChannelId(((ChannelEventSubCondition) event.getSubscription().getCondition()).getBroadcasterUserId())
                .ifPresentOrElse(
                        uuid -> {
                            ServerPlayerEntity player = Subathon.server.getPlayerManager().getPlayer(uuid);
                            if (player == null) {
                                Subathon.LOGGER.error("Failed to get player with UUID {}, twitch link may be broken until the server restarts!", uuid);
                                return;
                            }

                            informConnection(event.getSubscription(), ConnectionState.FAILURE, ConnectionType.DISCONNECT, player);
                        },
                        () -> Subathon.LOGGER.error("Failed to get user of a connection")
                );
    }

    private static void informConnection(@NotNull EventSubSubscription subscription, @NotNull ConnectionState state, ConnectionType type, @Nullable ServerPlayerEntity player) {
        if (player == null) return;
        String conn = switch (subscription.getRawType()) {
            case "channel.subscribe" -> "subs";
            case "channel.subscription.message" -> "resubs";
            case "channel.subscription.gift" -> "gifts";
            case "channel.cheer" -> "bits";
            case "channel.channel_points_custom_reward_redemption.add" -> "channel_points";
            default -> "";
        };

        sendStatusPacket(conn, state, type, player);
    }

    private static Optional<Data> getDataFromToken(String token) {
        return data.values().stream().filter(v -> v.token.equals(token)).findFirst();
    }

    static Optional<UUID> getUuidFromChannelId(String id) {
        return data.entrySet().stream().filter(v -> v.getValue().channelId.equals(id)).map(Map.Entry::getKey).findFirst();
    }

    static Optional<UUID> getUuidFromToken(String id) {
        return data.entrySet().stream().filter(v -> v.getValue().token.equals(id)).map(Map.Entry::getKey).findFirst();
    }

    static Optional<Data> getDataFromUuid(UUID uuid) {
        return data.entrySet().stream().filter(v -> v.getKey().equals(uuid)).map(Map.Entry::getValue).findFirst();
    }

    static Optional<Pair<UUID, Data>> getPairFromChannelId(String id) {
        return data.entrySet().stream().filter(v -> v.getValue().channelId.equals(id)).map(v -> new Pair<>(v.getKey(), v.getValue())).findFirst();
    }

    static {
        boolean isDev = Subathon.COMMON_CONFIGS.useMockApi();

        clientPool = TwitchClientPoolBuilder.builder()
                .withEnableHelix(true)
                .withEnableGraphQL(true)
                .build();

        /*chatPool = TwitchChatConnectionPool.builder()
                .maxSubscriptionsPerConnection(20)
                .build();*/

        var eventSubBuilder = TwitchEventSocketPool.builder().helix(clientPool.getHelix());
        TwitchHelix helix = null;
        if (isDev) helix = TwitchHelixBuilder.builder().withBaseUrl("http://0.0.0.0:3000").build();
        eventSub = isDev ? eventSubBuilder.baseUrl("ws://0.0.0.0:3000").helix(helix).build() : eventSubBuilder.build();

        eventSub.getEventManager().onEvent(ChannelSubscribeEvent.class, TwitchEvents::onSubscription);
        eventSub.getEventManager().onEvent(ChannelSubscriptionMessageEvent.class, TwitchEvents::onSubscriptionMessage);
        eventSub.getEventManager().onEvent(ChannelSubscriptionGiftEvent.class, TwitchEvents::onSubscriptionGift);
        eventSub.getEventManager().onEvent(ChannelCheerEvent.class, TwitchEvents::onCheer);
        eventSub.getEventManager().onEvent(CustomRewardRedemptionAddEvent.class, TwitchEvents::onRewardRedemption);
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
        public final String token;
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
