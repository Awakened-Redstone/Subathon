package com.awakenedredstone.subathon.mixin;

import net.minecraft.world.timer.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Queue;


@Mixin(Timer.class)
public interface TimerMixin<T> {

    @Accessor Queue<Timer.Event<T>> getEvents();
}
