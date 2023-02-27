package com.awakenedredstone.subathon.registry;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.Effect;
import com.awakenedredstone.subathon.core.effect.chaos.Chaos;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.SimpleRegistry;

public class SubathonRegistries {
    public static final SimpleRegistry<Effect> EFFECTS = FabricRegistryBuilder.createSimple(Effect.class, Subathon.id("effects")).buildAndRegister();
    public static final SimpleRegistry<Chaos> CHAOS = FabricRegistryBuilder.createSimple(Chaos.class, Subathon.id("chaos")).buildAndRegister();
}
