package com.awakenedredstone.subathon.integration;

import com.github.philippheuer.events4j.api.IEventManager;

public abstract class PlatformLink implements AutoCloseable {
    /**
     * Get the event manager
     *
     * @return EventManager
     */
    public abstract IEventManager getEventManager();

    @Override
    public abstract void close();
}
