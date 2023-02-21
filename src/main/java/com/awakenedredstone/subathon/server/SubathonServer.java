package com.awakenedredstone.subathon.server;

import com.awakenedredstone.subathon.twitch.Twitch;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

@Environment(EnvType.SERVER)
public class SubathonServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Twitch.nuke();
        });
    }
}
