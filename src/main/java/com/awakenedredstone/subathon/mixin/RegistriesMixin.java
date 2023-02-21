package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.event.RegistryFreezeCallback;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Registries.class)
public class RegistriesMixin {

    @Inject(method = "freezeRegistries", at = @At("HEAD"))
    private static void subathon$freezeRegistryEvent(CallbackInfo ci) {
        RegistryFreezeCallback.EVENT.invoker().invoke();
    }
}
