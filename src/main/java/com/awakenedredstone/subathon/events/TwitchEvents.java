package com.awakenedredstone.subathon.events;

import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface TwitchEvents {
    Event<ModEvents> VALUE_UPDATE = EventFactory.createArrayBacked(ModEvents.class, (listeners) -> () -> {
        for (ModEvents event : listeners) {
            event.valueUpdated();
        }
    });

    @FunctionalInterface
    interface ChannelEvents {
        /**
         * Called when someone subscribes (gifts included!)
         * @param event the event
         */
        void onSubscription(SubscriptionEvent event);
    }

    @FunctionalInterface
    interface ModEvents {
        /**
         * Called when the value is updated.
         */
        void valueUpdated();
    }

    /*Event<CubeControllerEvents> POTION_CHAOS = EventFactory.createArrayBacked(CubeControllerEvents.class, (listeners) -> () -> {
        for (CubeControllerEvents event : listeners) {
            event.trigger();
        }
    });*/
}
