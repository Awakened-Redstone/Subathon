package com.awakenedredstone.subathon.core.data;

import com.awakenedredstone.subathon.Subathon;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import dev.onyxstudios.cca.api.v3.world.WorldComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.world.WorldComponentInitializer;

public class Components implements WorldComponentInitializer, EntityComponentInitializer {
    public static final ComponentKey<WorldComponent> WORLD_DATA = ComponentRegistryV3.INSTANCE.getOrCreate(Subathon.id("world_data"), WorldComponent.class);
    public static final ComponentKey<PlayerComponent> PLAYER_DATA = ComponentRegistryV3.INSTANCE.getOrCreate(Subathon.id("player_data"), PlayerComponent.class);

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
        registry.register(WORLD_DATA, WorldComponent::new);
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(PLAYER_DATA, PlayerComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
    }
}