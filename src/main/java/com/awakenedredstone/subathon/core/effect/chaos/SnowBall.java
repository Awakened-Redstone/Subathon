package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.util.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class SnowBall extends Chaos {

    public SnowBall() {
        super(20);
    }

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        var ref = new Object() {
            int success = 0;
        };
        if (player instanceof ServerPlayerEntity) {
            Utils.makeSphere(player.getPos(), vec3d -> {
                try {
                    BlockState state = player.getWorld().getBlockState(BlockPos.ofFloored(vec3d));
                    //TODO: tags
                    if (state.isAir() || state.isLiquid()) {
                        player.getWorld().setBlockState(BlockPos.ofFloored(vec3d), Blocks.POWDER_SNOW.getDefaultState());
                        ref.success++;
                    }
                } catch (Exception e) {/**/}
            }, 5, 5, 5, true);
        }
        return ref.success >= 10;
    }
}
