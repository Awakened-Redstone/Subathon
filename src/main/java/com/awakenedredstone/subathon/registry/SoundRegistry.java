package com.awakenedredstone.subathon.registry;

import com.awakenedredstone.subathon.Subathon;
import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

public class SoundRegistry implements AutoRegistryContainer<SoundEvent> {
    public static final SoundEvent SHOCKWAVE = SoundEvent.of(Subathon.id("sfx.generic.shockwave"));

    @Override
    public Registry<SoundEvent> getRegistry() {
        return Registries.SOUND_EVENT;
    }

    @Override
    public Class<SoundEvent> getTargetFieldType() {
        return SoundEvent.class;
    }
}
