package com.awakenedredstone.subathon.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@FunctionalInterface
public interface RegistryFreezeCallback {
    Event<RegistryFreezeCallback> EVENT = EventFactory.createArrayBacked(RegistryFreezeCallback.class, (listeners) -> () -> {
        for (RegistryFreezeCallback event : listeners) {
            event.invoke();
        }
    });

    void invoke();
}