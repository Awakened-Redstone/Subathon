package com.awakenedredstone.subathon.events;

import com.awakenedredstone.cubecontroller.events.CubeControllerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface SubathonEvents extends CubeControllerEvents {
    Event<CubeControllerEvents> CHAOS = EventFactory.createArrayBacked(CubeControllerEvents.class, (listeners) -> control -> {
        for (CubeControllerEvents event : listeners) {
            event.invoke(control);
        }
    });
}
