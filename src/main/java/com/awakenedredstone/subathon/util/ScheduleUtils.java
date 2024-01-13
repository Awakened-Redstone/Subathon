package com.awakenedredstone.subathon.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.timer.Timer;
import net.minecraft.world.timer.TimerCallback;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;

public class ScheduleUtils {
    private final Queue<Event> events = new PriorityQueue<>(createEventComparator());

    public void close() {
        events.clear();
    }

    public void tick(MinecraftServer server) {
        Event event;
        while ((event = this.events.peek()) != null && event.triggerTime <= server.getOverworld().getTime()) {
            this.events.remove();
            event.callback.run();
        }
    }

    private static <T> Comparator<Event> createEventComparator() {
        return Comparator.comparingLong(event -> event.triggerTime);
    }

    public void schedule(long delay, Runnable callback) {
        events.add(new Event(delay, callback));
    }

    public void schedule(MinecraftServer server, long delay, Runnable callback) {
        events.add(new Event(server.getSaveProperties().getMainWorldProperties().getTime() + delay, callback));
    }

    public static void scheduleDelay(MinecraftServer server, long delay, TimerCallback<MinecraftServer> callback) {
        if (server == null) return;
        Timer<MinecraftServer> timer = server.getSaveProperties().getMainWorldProperties().getScheduledEvents();
        timer.setEvent("subathon#" + UUID.randomUUID(), server.getSaveProperties().getMainWorldProperties().getTime() + delay, callback);
    }

    public static void schedule(MinecraftServer server, long time, TimerCallback<MinecraftServer> callback) {
        if (server == null) return;
        Timer<MinecraftServer> timer = server.getSaveProperties().getMainWorldProperties().getScheduledEvents();
        timer.setEvent("subathon#" + UUID.randomUUID(), time, callback);
    }

    record Event(long triggerTime, Runnable callback) {}
}