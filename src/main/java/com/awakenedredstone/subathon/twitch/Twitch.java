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
import com.github.twitch4j.eventsub.events.*;
import com.github.twitch4j.eventsub.socket.TwitchEventSocketPool;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class Twitch {
    private static final TwitchClientPool clientPool;
    //private static final TwitchChatConnectionPool chatPool;
    public static final TwitchEventSocketPool eventSub;
    private static final Map<String, List<EventSubSubscription>> subscriptions = new HashMap<>();
    public static final Map<UUID, Data> data = new HashMap<>();

    public static void init() {/**/}

    public static void connect(String token, ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString("core").writeBoolean(true).writeBoolean(false);
        ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);

        OAuth2Credential auth = new OAuth2Credential("twitch", token);
        TwitchIdentityProvider identityProvider = new TwitchIdentityProvider(null, null, null);
        Pair<String, String> userData = identityProvider.getAdditionalCredentialInformation(auth).map(v -> new Pair<>(v.getUserId(), v.getUserName())).orElse(null);

        if (userData == null || StringUtils.isBlank(userData.getLeft()) || StringUtils.isBlank(userData.getRight())) {
            buf = PacketByteBufs.create();
            buf.writeString("core").writeBoolean(false).writeBoolean(true);
            ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
            ServerPlayNetworking.send(player, Subathon.id("connection_fail"), PacketByteBufs.create());
            return;
        }

        getDataFromToken(token).ifPresent(data -> {
            data.channelId = userData.getLeft();
            data.channelName = userData.getRight();
        });

        buf = PacketByteBufs.create();
        buf.writeString(userData.getRight());
        ServerPlayNetworking.send(player, Subathon.id("account_name"), buf);

        ArrayList<EventSubSubscription> list = new ArrayList<>();
        subscriptions.put(token, list);

        EventSubSubscription subs = SubscriptionTypes.CHANNEL_SUBSCRIBE.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        buf = PacketByteBufs.create();
        buf.writeString("subs").writeBoolean(eventSub.register(auth, subs)).writeBoolean(false);
        ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
        list.add(subs);

        EventSubSubscription resubs = SubscriptionTypes.CHANNEL_SUBSCRIPTION_MESSAGE.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        buf = PacketByteBufs.create();
        buf.writeString("resubs").writeBoolean(eventSub.register(auth, resubs)).writeBoolean(false);
        ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
        list.add(resubs);

        EventSubSubscription gifts = SubscriptionTypes.CHANNEL_SUBSCRIPTION_GIFT.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        buf = PacketByteBufs.create();
        buf.writeString("gifts").writeBoolean(eventSub.register(auth, gifts)).writeBoolean(false);
        ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
        list.add(gifts);

        EventSubSubscription cheer = SubscriptionTypes.CHANNEL_CHEER.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        buf = PacketByteBufs.create();
        buf.writeString("bits").writeBoolean(eventSub.register(auth, cheer)).writeBoolean(false);
        ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
        list.add(cheer);

        EventSubSubscription redemption = SubscriptionTypes.CHANNEL_POINTS_CUSTOM_REWARD_REDEMPTION_ADD.prepareSubscription(builder -> builder.broadcasterUserId(userData.getLeft()).build(), EventSubTransport.builder().method(EventSubTransportMethod.WEBSOCKET).build());
        buf = PacketByteBufs.create();
        buf.writeString("channel_points").writeBoolean(eventSub.register(auth, redemption)).writeBoolean(false);
        ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);
        list.add(redemption);

        boolean isUserConnected = ((TwitchEventSocketPoolAccessor) (Object) eventSub).getPoolByUserId().containsKey(userData.getLeft());
        buf = PacketByteBufs.create();
        buf.writeString("core").writeBoolean(isUserConnected).writeBoolean(true);
        ServerPlayNetworking.send(player, Subathon.id("twitch_status"), buf);

        if (!isUserConnected) {
            ServerPlayNetworking.send(player, Subathon.id("connection_fail"), PacketByteBufs.create());
            return;
        }
    }

    public static void disconnect(String token) {
        if (token != null && subscriptions.containsKey(token)) {
            List<EventSubSubscription> subscriptionList = subscriptions.get(token);
            List<String> strings = subscriptionList.stream().map(EventSubSubscription::getId).toList();
            eventSub.getSubscriptions().stream().filter(v -> strings.contains(v.getId())).forEach(eventSub::unregister);
            subscriptions.remove(token);
        }
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

    /*public static void toggleReward(String token, String rewardId, boolean enable) {
        new CompletableFuture<>().completeAsync(() -> {
            OAuth2Credential auth = new OAuth2Credential("twitch", token);
            TwitchIdentityProvider identityProvider = new TwitchIdentityProvider(null, null, null);
            String userId = identityProvider.getAdditionalCredentialInformation(auth).map(OAuth2Credential::getUserId).orElse(null);

            List<CustomReward> rewards = clientPool.getHelix().getCustomRewards(token, userId, Collections.singletonList(rewardId), true).execute().getRewards();
            if (rewards.isEmpty()) return null;

            CustomReward reward = rewards.get(0);
            clientPool.getHelix().updateCustomReward(token, userId, rewardId, reward.withIsEnabled(enable));
            return null;
        });
    }*/

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

    private static Optional<Data> getDataFromToken(String token) {
        return data.values().stream().filter(v -> v.token.equals(token)).findFirst();
    }

    static Optional<Data> getDataFromId(String id) {
        return data.values().stream().filter(v -> v.channelId.equals(id)).findFirst();
    }

    static Optional<UUID> getUuidFromId(String id) {
        return data.entrySet().stream().filter(v -> v.getValue().channelId.equals(id)).map(Map.Entry::getKey).findFirst();
    }

    static Optional<Pair<UUID, Data>> getFromId(String id) {
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

    public static class Data {
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
