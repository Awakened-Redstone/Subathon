package com.awakenedredstone.subathon.world.gen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.VerticalSurfaceType;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public record SculkPatchFeatureConfig(BlockStateProvider groundState, RegistryEntry<PlacedFeature> vegetationFeature, VerticalSurfaceType surface, IntProvider depth, float extraBottomBlockChance, int verticalRange, float vegetationChance, IntProvider horizontalRadius, float extraEdgeColumnChance) implements FeatureConfig {

    public static final Codec<SculkPatchFeatureConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BlockStateProvider.TYPE_CODEC.fieldOf("ground_state").forGetter(config -> config.groundState),
            PlacedFeature.REGISTRY_CODEC.fieldOf("vegetation_feature").forGetter(config -> config.vegetationFeature),
            VerticalSurfaceType.CODEC.fieldOf("surface").forGetter(config -> config.surface),
            IntProvider.createValidatingCodec(1, 128).fieldOf("depth").forGetter(config -> config.depth),
            Codec.floatRange(0.0f, 1.0f).fieldOf("extra_bottom_block_chance").forGetter(config -> config.extraBottomBlockChance),
            Codec.intRange(1, 256).fieldOf("vertical_range").forGetter(config -> config.verticalRange),
            Codec.floatRange(0.0f, 1.0f).fieldOf("vegetation_chance").forGetter(config -> config.vegetationChance),
            IntProvider.VALUE_CODEC.fieldOf("xz_radius").forGetter(config -> config.horizontalRadius),
            Codec.floatRange(0.0f, 1.0f).fieldOf("extra_edge_column_chance").forGetter(config -> config.extraEdgeColumnChance)
        ).apply(instance, SculkPatchFeatureConfig::new));

}