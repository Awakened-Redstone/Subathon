package com.awakenedredstone.subathon.util;

import com.awakenedredstone.cubecontroller.CubeController;
import com.awakenedredstone.cubecontroller.GameControl;
import com.awakenedredstone.subathon.Subathon;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.timer.Timer;
import net.minecraft.world.timer.TimerCallback;

import java.util.*;

public class ScheduleUtils {
    private final Queue<Event> events = new PriorityQueue<>(createEventComparator());

    public void destroy() {
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

    public record UpdateControlValue(Identifier identifier, double amount) implements TimerCallback<MinecraftServer> {

        @Override
        public void call(MinecraftServer server, Timer<MinecraftServer> events, long time) {
            Optional<GameControl> controlOptional = CubeController.getControl(identifier);
            if (controlOptional.isPresent()) {
                GameControl control = controlOptional.get();
                control.value(control.value() + amount);
            }
        }

        public static class Serializer extends TimerCallback.Serializer<MinecraftServer, UpdateControlValue> {
            public Serializer() {
                super(Subathon.identifier("update_value"), UpdateControlValue.class);
            }

            @Override
            public void serialize(NbtCompound nbt, UpdateControlValue callback) {
                nbt.putString("control", callback.identifier().toString());
                nbt.putDouble("amount", callback.amount());
            }

            @Override
            public UpdateControlValue deserialize(NbtCompound nbt) {
                Identifier identifier = new Identifier(nbt.getString("control"));
                double amount = nbt.getDouble("amount");
                return new UpdateControlValue(identifier, amount);
            }
        }
    }
}
