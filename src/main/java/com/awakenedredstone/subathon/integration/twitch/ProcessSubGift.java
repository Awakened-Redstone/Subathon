package com.awakenedredstone.subathon.integration.twitch;

import com.github.twitch4j.chat.events.channel.GiftSubscriptionsEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import com.github.twitch4j.common.util.TwitchUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessSubGift {
    private static final Map<String, Map<String, Collection<BlockingQueue<SubscriptionEvent>>>> buffersByGifterIdByChannelId = new ConcurrentHashMap<>();

    public static void onGift(GiftSubscriptionsEvent e) {
        buffersByGifterIdByChannelId.computeIfAbsent(e.getChannel().getId(), c -> new ConcurrentHashMap<>())
                .computeIfAbsent(getGifterId(e.getUser()), s -> ConcurrentHashMap.newKeySet(1))
                .add(new ArrayBlockingQueue<>(e.getCount()));
    }

    public static void onSubscription(SubscriptionEvent e) {
        if (!e.getGifted()) return;

        Map<String, Collection<BlockingQueue<SubscriptionEvent>>> buffersByGifterId = buffersByGifterIdByChannelId.get(e.getChannel().getId());
        if (buffersByGifterId == null || buffersByGifterId.isEmpty()) {
            Twitch.getInstance().getChatPool().getEventManager().publish(new SpecificSubGiftEvent(e.getChannel(), e.getGiftedBy(), Collections.singleton(e)));
            return;
        }

        String gifterId = getGifterId(e.getGiftedBy());
        Collection<BlockingQueue<SubscriptionEvent>> buffers = buffersByGifterId.get(gifterId);
        if (buffers == null || buffers.isEmpty()) {
            Twitch.getInstance().getChatPool().getEventManager().publish(new SpecificSubGiftEvent(e.getChannel(), e.getGiftedBy(), Collections.singleton(e)));
            return;
        }

        for (BlockingQueue<SubscriptionEvent> buffer : buffers) {
            if (buffer.offer(e)) {
                if (buffer.remainingCapacity() <= 0 && buffers.remove(buffer)) {
                    if (buffers.isEmpty()) buffersByGifterId.computeIfPresent(gifterId, (k, v) -> v.isEmpty() ? null : v);
                }
                break;
            }
        }
    }

    private static String getGifterId(EventUser user) {
        return user == null || user.equals(TwitchUtils.ANONYMOUS_GIFTER) ? TwitchUtils.ANONYMOUS_GIFTER.getId() : user.getId();
    }
}