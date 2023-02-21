package com.awakenedredstone.subathon;

import com.awakenedredstone.subathon.core.effect.chaos.process.ChaosRegistry;
import com.awakenedredstone.subathon.core.effect.process.EffectRegistry;
import com.awakenedredstone.subathon.twitch.Twitch;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class SubathonPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        EffectRegistry.registerPackage("com.awakenedredstone.subathon.core.effect");
        ChaosRegistry.registerPackage("com.awakenedredstone.subathon.core.effect.chaos");

        MixinExtrasBootstrap.init();
        EffectRegistry.init();
        ChaosRegistry.init();
    }
}
