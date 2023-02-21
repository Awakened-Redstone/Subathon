package com.awakenedredstone.subathon.mixin.twitch4j;

import com.github.twitch4j.eventsub.socket.TwitchEventSocketPool;
import com.github.twitch4j.eventsub.socket.TwitchSingleUserEventSocketPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(TwitchEventSocketPool.class)
public interface TwitchEventSocketPoolAccessor {
    @Accessor Map<String, TwitchSingleUserEventSocketPool> getPoolByUserId();
}
