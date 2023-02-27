package com.awakenedredstone.subathon.core.effect.chaos;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public class InfestedSphere extends Chaos {

    @Override
    public boolean playerTrigger(PlayerEntity player) {
        var ref = new Object() {
            int success = 0;
        };
        if (player instanceof ServerPlayerEntity) {
            Random random = new Random();
            Utils.makeSphere(player.getPos(), vec3d -> {
                try {
                    if (player.world.getBlockState(new BlockPos(vec3d)).getBlock().getHardness() == -1) return;
                    Block block = Subathon.infestedBlocks.get(random.nextInt(Subathon.infestedBlocks.size()));
                    player.world.setBlockState(new BlockPos(vec3d), block.getDefaultState());
                    ref.success++;
                } catch (Exception e) {/**/}
            }, 5, 5, 5, false);
        }
        return ref.success >= 10;
    }
}
