package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.util.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class SnowBall extends Chaos {

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        var ref = new Object() {
            int success = 0;
        };
        if (player instanceof ServerPlayerEntity) {
            Utils.makeSphere(player.getPos(), vec3d -> {
                try {
                    if (player.world.getBlockState(new BlockPos(vec3d)).isAir() || player.world.getBlockState(new BlockPos(vec3d)).getMaterial().isLiquid()) {
                        player.world.setBlockState(new BlockPos(vec3d), Blocks.POWDER_SNOW.getDefaultState());
                        ref.success++;
                    }
                } catch (Exception e) {/**/}
            }, 5, 5, 5, true);
        }
        return ref.success >= 10;
    }
}
