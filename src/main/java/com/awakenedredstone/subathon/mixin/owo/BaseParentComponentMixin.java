package com.awakenedredstone.subathon.mixin.owo;

import com.awakenedredstone.subathon.duck.owo.ComponentDuck;
import io.wispforest.owo.ui.base.BaseParentComponent;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Environment(EnvType.CLIENT)
@Mixin(BaseParentComponent.class)
public abstract class BaseParentComponentMixin implements ComponentDuck {
    private final List<Listener> updateListeners = new ArrayList<>();

    @Inject(method = "update", at = @At(value = "HEAD"), locals = LocalCapture.CAPTURE_FAILSOFT, remap = false)
    private void subathon$callUpdateListener(float delta, int mouseX, int mouseY, CallbackInfo ci) {
        updateListeners.forEach(listener -> listener.update(delta, mouseX, mouseY));
    }

    @Override
    public void subathon$registerUpdateListener(Listener listener) {
        updateListeners.add(listener);
    }

    /*@Inject(method = "drawChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 1, shift = At.Shift.AFTER), remap = false)
    private void subathon$fixMatrixDrift(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta, List<Component> children, CallbackInfo ci) {
        context.getMatrices().translate(0, 0, -1);
    }*/
}
