package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.client.SubathonClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(TextureManager.class)
@Environment(EnvType.CLIENT)
public class TextureManagerMixin {
    @Shadow @Final private Map<Identifier, AbstractTexture> textures;

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void subathon$addRuntimeTextures(Identifier id, CallbackInfoReturnable<AbstractTexture> cir) {
        if (!textures.containsKey(id) && SubathonClient.getInstance().runtimeRewardTextures.getTextures().containsKey(id)) {
            cir.setReturnValue(SubathonClient.getInstance().runtimeRewardTextures.getTexture(id));
        }
    }
}
