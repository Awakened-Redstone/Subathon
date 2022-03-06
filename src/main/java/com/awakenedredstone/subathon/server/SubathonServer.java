package com.awakenedredstone.subathon.server;

import com.awakenedredstone.subathon.commands.SubathonCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

@Environment(EnvType.CLIENT)
public class SubathonServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
    }
}
