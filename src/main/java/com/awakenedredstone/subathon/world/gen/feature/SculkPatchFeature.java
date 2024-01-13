package com.awakenedredstone.subathon.world.gen.feature;

import com.awakenedredstone.subathon.Subathon;
import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.VegetationPatchFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class SculkPatchFeature extends Feature<SculkPatchFeatureConfig> {
    public SculkPatchFeature(Codec<SculkPatchFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<SculkPatchFeatureConfig> context) {
        StructureWorldAccess structureWorldAccess = context.getWorld();
        SculkPatchFeatureConfig config = context.getConfig();
        Random random = context.getRandom();
        BlockPos blockPos = context.getOrigin();
        int radiusX = config.horizontalRadius().get(random) + 1;
        int radiusZ = config.horizontalRadius().get(random) + 1;
        Set<BlockPos> set = this.placeGroundAndGetPositions(structureWorldAccess, config, random, blockPos, radiusX, radiusZ);
        this.generateVegetation(context, structureWorldAccess, config, random, set, radiusX, radiusZ);
        return !set.isEmpty();
    }

    protected Set<BlockPos> placeGroundAndGetPositions(StructureWorldAccess world, SculkPatchFeatureConfig config, Random random, BlockPos pos, int radiusX, int radiusZ) {
        TagKey<Block> goesUnder = TagKey.of(RegistryKeys.BLOCK, Subathon.id("sculk_patch_can_go_under"));
        Predicate<BlockState> goesUnderPredicate = state -> state.isIn(goesUnder) || state.getBlock() instanceof FluidBlock || state.isAir();

        BlockPos.Mutable mutable = pos.mutableCopy();
        BlockPos.Mutable mutable2 = mutable.mutableCopy();
        Direction direction = config.surface().getDirection();
        Direction direction2 = direction.getOpposite();
        HashSet<BlockPos> set = new HashSet<>();
        for (int x = -radiusX; x <= radiusX; ++x) {
            boolean bl = x == -radiusX || x == radiusX;
            for (int z = -radiusZ; z <= radiusZ; ++z) {
                boolean bl2 = z == -radiusZ || z == radiusZ;
                boolean bl3 = bl || bl2;
                boolean bl4 = bl && bl2;
                boolean bl5 = bl3 && !bl4;

                if (bl4 || bl5 && (config.extraEdgeColumnChance() == 0.0f || random.nextFloat() > config.extraEdgeColumnChance())) continue;
                mutable.set(pos, x, 0, z);
                for (int i = 0; world.testBlockState(mutable, goesUnderPredicate) && i < config.verticalRange(); ++i) {
                    mutable.move(direction);
                }
                for (int i = 0; world.testBlockState(mutable, state -> !goesUnderPredicate.test(state)) && i < config.verticalRange(); ++i) {
                    mutable.move(direction2);
                }
                mutable2.set(mutable, config.surface().getDirection());
                BlockState blockState = world.getBlockState(mutable2);
                TagKey<Block> force = TagKey.of(RegistryKeys.BLOCK, Subathon.id("sculk_replaceable_shockwave"));
                Predicate<BlockState> forcePredicate = state -> state.isIn(force);
                if (blockState.isReplaceable() || (!blockState.isSideSolidFullSquare(world, mutable2, config.surface().getDirection().getOpposite()) && !forcePredicate.test(blockState))) continue;
                int depth = config.depth().get(random) + (config.extraBottomBlockChance() > 0.0f && random.nextFloat() < config.extraBottomBlockChance() ? 1 : 0);
                BlockPos blockPos = mutable2.toImmutable();
                boolean placed = this.placeGround(world, config, random, mutable2, depth);
                if (!placed) continue;
                set.add(blockPos);
            }
        }
        return set;
    }

    protected void generateVegetation(FeatureContext<SculkPatchFeatureConfig> context, StructureWorldAccess world, SculkPatchFeatureConfig config, Random random, Set<BlockPos> positions, int radiusX, int radiusZ) {
        for (BlockPos blockPos : positions) {
            if (!(config.vegetationChance() > 0.0f) || !(random.nextFloat() < config.vegetationChance())) continue;
            this.generateVegetationFeature(world, config, context.getGenerator(), random, blockPos);
        }
    }

    protected boolean generateVegetationFeature(StructureWorldAccess world, SculkPatchFeatureConfig config, ChunkGenerator generator, Random random, BlockPos pos) {
        return config.vegetationFeature().value().generateUnregistered(world, generator, random, pos.offset(config.surface().getDirection().getOpposite()));
    }

    protected boolean placeGround(StructureWorldAccess world, SculkPatchFeatureConfig config, Random random, BlockPos.Mutable pos, int depth) {
        TagKey<Block> ignore = TagKey.of(RegistryKeys.BLOCK, Subathon.id("sculk_patch_ignore"));
        Predicate<BlockState> ignorePredicate = state -> state.isIn(ignore);
        for (int i = 0; i < depth; ++i) {
            BlockState blockState2;
            BlockState blockState = config.groundState().get(random, pos);
            BlockState original = world.getBlockState(pos);

            //Replace bedrock = bad
            if (original.getBlock().getHardness() == -1) continue;

            //Hard blocks have a chance to not be replaced
            if (original.getBlock().getBlastResistance() > 20 && random.nextFloat() > (35 / original.getBlock().getBlastResistance())) continue;

            if (blockState.isOf((blockState2 = world.getBlockState(pos)).getBlock()) || ignorePredicate.test(blockState2)) continue;
            if (blockState2.isReplaceable()) return i != 0;
            world.setBlockState(pos, blockState, Block.NOTIFY_LISTENERS, 0);
            pos.move(config.surface().getDirection());
        }
        return true;
    }
}
