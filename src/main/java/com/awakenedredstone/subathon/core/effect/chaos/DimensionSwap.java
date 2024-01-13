package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.ServerUtils;
import com.awakenedredstone.subathon.util.Texts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class DimensionSwap extends Chaos {

    public DimensionSwap() {
        super(10);
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Subathon.schedule(0, () -> {
                try {
                    ServerWorld world = serverPlayer.server.getWorld(player.getWorld().getRegistryKey() == World.NETHER ? World.OVERWORLD : World.NETHER);
                    boolean isTargetNether = world.getRegistryKey() == World.NETHER;
                    BlockPos pos = BlockPos.ofFloored(isTargetNether ? player.getPos().multiply(1 / 8d, 1, 1 / 8d) : player.getPos().multiply(8, 1, 8));
                    serverPlayer.teleport(world, pos.getX() + 0.5, isTargetNether ? Math.max(5, Math.min(pos.getY(), 120)) : pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());

                    for (int y = -1; y <= 2; y++) {
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                BlockPos blockPos = pos.add(x, y, z);
                                BlockState state = world.getBlockState(blockPos);
                                //TODO: tags
                                if (state.isAir() || !Block.isFaceFullSquare(state.getCollisionShape(world, blockPos), Direction.UP) || state.isLiquid()) {
                                    world.setBlockState(blockPos, Blocks.GLASS.getDefaultState());
                                }
                            }
                        }
                    }

                    world.setBlockState(pos.add(0, 0, 0), Blocks.AIR.getDefaultState());
                    world.setBlockState(pos.add(0, 1, 0), Blocks.AIR.getDefaultState());
                } catch (Exception e) {
                    ServerUtils.broadcast(Texts.of("<red>Failed to execute dimension swap!"));
                }
            });
        }
        return true;
    }
}
