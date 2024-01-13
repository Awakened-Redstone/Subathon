package com.awakenedredstone.subathon.world.gen.feature;

import com.awakenedredstone.subathon.Subathon;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.SimpleBlockFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.function.Predicate;

public class SimpleBlockFeature extends Feature<SimpleBlockFeatureConfig> {
    public SimpleBlockFeature(Codec<SimpleBlockFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<SimpleBlockFeatureConfig> context) {
        TagKey<Block> ignore = TagKey.of(RegistryKeys.BLOCK, Subathon.id("sculk_patch_ignore"));
        Predicate<BlockState> ignorePredicate = state -> state.isIn(ignore);

        SimpleBlockFeatureConfig simpleBlockFeatureConfig = context.getConfig();
        StructureWorldAccess world = context.getWorld();
        BlockPos blockPos = context.getOrigin();
        BlockState original = world.getBlockState(blockPos);
        BlockState blockState = simpleBlockFeatureConfig.toPlace().get(context.getRandom(), blockPos);
        if (!blockState.canPlaceAt(world, blockPos) || ignorePredicate.test(original)) return false;
        if (blockState.getBlock() instanceof TallPlantBlock) {
            if (!world.isAir(blockPos.up())) return false;
            TallPlantBlock.placeAt(world, blockState, blockPos, 2);
            return true;
        } else {
            if (world.isWater(blockPos) && blockState.getProperties().contains(Properties.WATERLOGGED)) {
                blockState = blockState.with(Properties.WATERLOGGED, true);
            }
            world.setBlockState(blockPos, blockState, Block.NOTIFY_LISTENERS, 0);
        }
        return true;
    }
}