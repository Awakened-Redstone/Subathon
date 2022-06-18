package com.awakenedredstone.subathon.util;

import com.awakenedredstone.cubecontroller.CubeController;
import com.awakenedredstone.cubecontroller.GameControl;
import com.awakenedredstone.subathon.Subathon;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.timer.Timer;
import net.minecraft.world.timer.TimerCallback;

import java.util.Optional;

public class ScheduleUtils {
    public static void scheduleDelay(Identifier identifier, MinecraftServer server, long delay, TimerCallback<MinecraftServer> callback) {
        Timer<MinecraftServer> timer = server.getSaveProperties().getMainWorldProperties().getScheduledEvents();
        timer.setEvent(identifier.toString(), server.getSaveProperties().getMainWorldProperties().getTime() + delay, callback);
    }

    public static void schedule(Identifier identifier,MinecraftServer server, long time, TimerCallback<MinecraftServer> callback) {
        Timer<MinecraftServer> timer = server.getSaveProperties().getMainWorldProperties().getScheduledEvents();
        timer.setEvent(identifier.toString(), time, callback);
    }

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
