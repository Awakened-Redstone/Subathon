package com.awakenedredstone.subathon.registry;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.world.gen.feature.SculkPatchFeature;
import com.awakenedredstone.subathon.world.gen.feature.SculkPatchFeatureConfig;
import com.awakenedredstone.subathon.world.gen.feature.SimpleBlockFeature;
import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.gen.feature.*;

public class FeatureRegistry {
    public static final Feature<SculkPatchFeatureConfig> SCULK_PATCH = register("sculk_patch", new SculkPatchFeature(SculkPatchFeatureConfig.CODEC));
    public static final Feature<SimpleBlockFeatureConfig> SIMPLE_BLOCK = register("simple_block", new SimpleBlockFeature(SimpleBlockFeatureConfig.CODEC));


    public static void init() {/**/}

    private static <C extends FeatureConfig, F extends Feature<C>> F register(String name, F feature) {
        return (F) Registry.register(Registries.FEATURE, Subathon.id(name), feature);
    }
}
