package com.awakenedredstone.subathon.mixin;


import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.util.IntegrationStatus;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.DeOpCommand;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Objects;

@Mixin(DeOpCommand.class)
public class DeOpCommandMixin {

    @Inject(method = "deop", at = @At("TAIL"))
    private static void deop(ServerCommandSource source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir) {
        PlayerManager playerManager = source.getServer().getPlayerManager();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(IntegrationStatus.UNKNOWN);
        targets.forEach(player -> ServerPlayNetworking.send(Objects.requireNonNull(playerManager.getPlayer(player.getId())), new Identifier(Subathon.MOD_ID, "bot_status"), buf));
    }
}
