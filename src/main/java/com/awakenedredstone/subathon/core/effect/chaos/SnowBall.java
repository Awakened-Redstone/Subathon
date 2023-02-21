package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.chaos.process.Chaos;
import com.awakenedredstone.subathon.core.effect.chaos.process.RegisterChaos;
import com.awakenedredstone.subathon.util.MessageUtils;
import com.awakenedredstone.subathon.util.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@RegisterChaos("subathon:snowball")
public class SnowBall extends Chaos {

    @Override
    public boolean trigger(World world) {
        MessageUtils.broadcast(this::trigger, Subathon.id("chaos"));
        return true;
    }

    @Override
    public boolean trigger(PlayerEntity player) {
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
