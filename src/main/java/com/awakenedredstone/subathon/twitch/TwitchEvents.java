package com.awakenedredstone.subathon.twitch;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.data.ComponentManager;
import com.awakenedredstone.subathon.networking.NetworkingUtils;
import com.awakenedredstone.subathon.util.MapBuilder;
import com.awakenedredstone.subathon.util.Texts;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.eventsub.events.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TwitchEvents {

    static void onSubscription(ChannelSubscribeEvent event) {
        PlayerManager playerManager = Subathon.server.getPlayerManager();
        Twitch.getInstance().getUuidFromChannelId(event.getBroadcasterUserId()).ifPresentOrElse(uuid -> {
            if (event.isGift()) return;
            ServerPlayerEntity player = playerManager.getPlayer(uuid);

            if (player == null) {
                playerManager.broadcast(Texts.of("<red>Event triggered to offline player! This should never happen, please report this issue!",
                        new MapBuilder.StringMap().build()), false);
                return;
            }

            Map<String, String> placeholders = new MapBuilder.StringMap()
                    .put("%user%", event.getUserName())
                    .put("%tier%", getTierNumber(event.getTier()))
                    .build();

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeEnumConstant(EventMessages.SUB);
            switch (event.getTier()) {
                case NONE -> buf.writeEnumConstant(EventMessages.QUICK_ERROR);
                case TIER1, TWITCH_PRIME -> buf.writeEnumConstant(EventMessages.QUICK_SUB_TIER1);
                case TIER2 -> buf.writeEnumConstant(EventMessages.QUICK_SUB_TIER2);
                case TIER3 -> buf.writeEnumConstant(EventMessages.QUICK_SUB_TIER3);
            }
            buf.writeMap(placeholders, PacketByteBuf::writeString, PacketByteBuf::writeString);
            buf.writeString("");
            NetworkingUtils.send(uuid, Subathon.id("event_message"), buf);

            int points = switch (event.getTier()) {
                case NONE -> 0;
                case TIER1, TWITCH_PRIME -> Subathon.COMMON_CONFIGS.subs.tier1.points();
                case TIER2 -> Subathon.COMMON_CONFIGS.subs.tier2.points();
                case TIER3 -> Subathon.COMMON_CONFIGS.subs.tier3.points();
            };

            if (Subathon.updateCooldown() > 0) {
                Subathon.delayedEvents.add(() -> ComponentManager.getComponent(Subathon.server.getOverworld(), player).addSubPoints(points));
            } else {
                ComponentManager.getComponent(Subathon.server.getOverworld(), player).addSubPoints(points);
            }
        }, () -> {
            playerManager.broadcast(Texts.of("<red>Failed to find data associated with the channel \"%channel%\"! Please report this failure to the mod issues page!",
                    new MapBuilder.StringMap().put("%channel%", event.getBroadcasterUserName()).build()), false);

            Twitch.getInstance().getData().values().forEach(Twitch.Data::printData);
        });
    }

    static void onSubscriptionMessage(ChannelSubscriptionMessageEvent event) {
        PlayerManager playerManager = Subathon.server.getPlayerManager();
        Twitch.getInstance().getUuidFromChannelId(event.getBroadcasterUserId()).ifPresentOrElse(uuid -> {
            ServerPlayerEntity player = playerManager.getPlayer(uuid);

            if (player == null) {
                playerManager.broadcast(Texts.of("<red>Event triggered to offline player! This should never happen, please report this issue!",
                        new MapBuilder.StringMap().build()), false);
                return;
            }

            Map<String, String> placeholders = new MapBuilder.StringMap()
                    .put("%user%", event.getUserName())
                    .putAny("%time%", event.getCumulativeMonths())
                    .put("%tier%", getTierNumber(event.getTier()))
                    .putAny("%duration%", event.getDurationMonths())
                    .build();

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeEnumConstant(event.getDurationMonths() > 1 ? EventMessages.RESUB_MULTIMONTH : EventMessages.RESUB);
            switch (event.getTier()) {
                case NONE -> buf.writeEnumConstant(EventMessages.QUICK_ERROR);
                case TIER1, TWITCH_PRIME -> buf.writeEnumConstant(EventMessages.QUICK_SUB_TIER1);
                case TIER2 -> buf.writeEnumConstant(EventMessages.QUICK_SUB_TIER2);
                case TIER3 -> buf.writeEnumConstant(EventMessages.QUICK_SUB_TIER3);
            }
            buf.writeMap(placeholders, PacketByteBuf::writeString, PacketByteBuf::writeString);
            buf.writeString(event.getMessage().getText());
            NetworkingUtils.send(uuid, Subathon.id("event_message"), buf);

            int points = switch (event.getTier()) {
                case NONE -> 0;
                case TIER1, TWITCH_PRIME -> Subathon.COMMON_CONFIGS.subs.tier1.points();
                case TIER2 -> Subathon.COMMON_CONFIGS.subs.tier2.points();
                case TIER3 -> Subathon.COMMON_CONFIGS.subs.tier3.points();
            } * event.getDurationMonths();

            if (Subathon.updateCooldown() > 0) {
                Subathon.delayedEvents.add(() -> ComponentManager.getComponent(Subathon.server.getOverworld(), player).addSubPoints(points));
            } else {
                ComponentManager.getComponent(Subathon.server.getOverworld(), player).addSubPoints(points);
            }
        }, () -> {
            playerManager.broadcast(Texts.of("<red>Failed to find data associated with the channel \"%channel%\"! Please report this failure to the mod issues page!",
                    new MapBuilder.StringMap().put("%channel%", event.getBroadcasterUserName()).build()), false);

            Twitch.getInstance().getData().values().forEach(Twitch.Data::printData);
        });
    }

    static void onSubscriptionGift(ChannelSubscriptionGiftEvent event) {
        PlayerManager playerManager = Subathon.server.getPlayerManager();
        Twitch.getInstance().getUuidFromChannelId(event.getBroadcasterUserId()).ifPresentOrElse(uuid -> {
            ServerPlayerEntity player = playerManager.getPlayer(uuid);

            if (player == null) {
                playerManager.broadcast(Texts.of("<red>Event triggered to offline player! This should never happen, please report this issue!",
                        new MapBuilder.StringMap().build()), false);
                return;
            }

            Map<String, String> placeholders = new MapBuilder.StringMap()
                    .put("%user%", event.getUserName())
                    .putAny("%amount%", event.getTotal())
                    .put("%tier%", getTierNumber(event.getTier()))
                    .build();

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeEnumConstant(EventMessages.GIFT);
            switch (event.getTier()) {
                case NONE -> buf.writeEnumConstant(EventMessages.QUICK_ERROR);
                case TIER1, TWITCH_PRIME -> buf.writeEnumConstant(EventMessages.QUICK_GIFT_TIER1);
                case TIER2 -> buf.writeEnumConstant(EventMessages.QUICK_GIFT_TIER2);
                case TIER3 -> buf.writeEnumConstant(EventMessages.QUICK_GIFT_TIER3);
            }
            buf.writeMap(placeholders, PacketByteBuf::writeString, PacketByteBuf::writeString);
            buf.writeString("");
            NetworkingUtils.send(Subathon.id("event_message"), buf);

            int points = switch (event.getTier()) {
                case NONE -> 0;
                case TIER1, TWITCH_PRIME -> Subathon.COMMON_CONFIGS.subs.tier1.points();
                case TIER2 -> Subathon.COMMON_CONFIGS.subs.tier2.points();
                case TIER3 -> Subathon.COMMON_CONFIGS.subs.tier3.points();
            };

            if (Subathon.updateCooldown() > 0) {
                Subathon.delayedEvents.add(() -> ComponentManager.getComponent(Subathon.server.getOverworld(), player).addSubPoints(points * event.getTotal()));
            } else {
                ComponentManager.getComponent(Subathon.server.getOverworld(), player).addSubPoints(points * event.getTotal());
            }
        }, () -> {
            playerManager.broadcast(Texts.of("<red>Failed to find data associated with the channel \"%channel%\"! Please report this failure to the mod issues page!",
                    new MapBuilder.StringMap().put("%channel%", event.getBroadcasterUserName()).build()), false);

            Twitch.getInstance().getData().values().forEach(Twitch.Data::printData);
        });
    }

    static void onCheer(ChannelCheerEvent event) {
        PlayerManager playerManager = Subathon.server.getPlayerManager();
        Twitch.getInstance().getUuidFromChannelId(event.getBroadcasterUserId()).ifPresentOrElse(uuid -> {
            ServerPlayerEntity player = playerManager.getPlayer(uuid);

            if (player == null) {
                playerManager.broadcast(Texts.of("<red>Event triggered to offline player! This should never happen, please report this issue!",
                        new MapBuilder.StringMap().build()), false);
                return;
            }
            Map<String, String> placeholders = new MapBuilder.StringMap()
                    .put("%user%", event.isAnonymous() ? "Anonymous" : event.getUserName())
                    .putAny("%amount%", event.getBits())
                    .build();

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeEnumConstant(EventMessages.CHEER);
            buf.writeEnumConstant(EventMessages.QUICK_CHEER);
            buf.writeMap(placeholders, PacketByteBuf::writeString, PacketByteBuf::writeString);
            buf.writeString(event.getMessage());
            NetworkingUtils.send(Subathon.id("event_message"), buf);

            if (Subathon.updateCooldown() > 0) {
                Subathon.delayedEvents.add(() -> ComponentManager.getComponent(Subathon.server.getOverworld(), player).addBits(event.getBits()));
            } else {
                ComponentManager.getComponent(Subathon.server.getOverworld(), player).addBits(event.getBits());
            }
        }, () -> {
            playerManager.broadcast(Texts.of("<red>Failed to find data associated with the channel \"%channel%\"! Please report this failure to the mod issues page!",
                    new MapBuilder.StringMap().put("%channel%", event.getBroadcasterUserName()).build()), false);

            Twitch.getInstance().getData().values().forEach(Twitch.Data::printData);
        });
    }

    static void onRewardRedemption(CustomRewardRedemptionAddEvent event) {
        PlayerManager playerManager = Subathon.server.getPlayerManager();
        Twitch.getInstance().getPairFromChannelId(event.getBroadcasterUserId()).ifPresentOrElse(pair -> {
            UUID uuid = pair.getLeft();
            Twitch.Data data = pair.getRight();
            ServerPlayerEntity player = playerManager.getPlayer(uuid);

            Optional<UUID> rewardId = data.getRewardId();
            if (rewardId.isPresent() && rewardId.get().equals(UUID.fromString(event.getReward().getId()))) {
                if (Subathon.updateCooldown() > 0) {
                    Subathon.delayedEvents.add(() -> ComponentManager.getComponent(Subathon.server.getOverworld(), player).addRewardPoints(1));
                } else {
                    ComponentManager.getComponent(Subathon.server.getOverworld(), player).addRewardPoints(1);
                }
            }
        }, () -> {
            playerManager.broadcast(Texts.of("<red>Failed to find data associated with the channel \"%channel%\"! Please report this failure to the mod issues page!",
                    new MapBuilder.StringMap().put("%channel%", event.getBroadcasterUserName()).build()), false);

            Twitch.getInstance().getData().values().forEach(Twitch.Data::printData);
        });
    }

    private static String getTierNumber(SubscriptionPlan tier) {
        return switch (tier) {
            case NONE -> "0? What";
            case TIER1, TWITCH_PRIME -> "1";
            case TIER2 -> "2";
            case TIER3 -> "3";
        };
    }
}
