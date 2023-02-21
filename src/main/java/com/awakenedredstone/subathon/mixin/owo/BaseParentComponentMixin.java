package com.awakenedredstone.subathon.mixin.owo;

import com.awakenedredstone.subathon.duck.BaseParentComponentDuck;
import com.awakenedredstone.subathon.duck.ComponentDuck;
import io.wispforest.owo.ui.base.BaseParentComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(BaseParentComponent.class)
public abstract class BaseParentComponentMixin implements BaseParentComponentDuck, ComponentDuck {
    private final List<Listener> updateListeners = new ArrayList<>();
    private boolean render = true;

    @Override
    public boolean subathon$render() {
        return render;
    }

    @Override
    public void subathon$render(boolean render) {
        this.render = render;
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, remap = false)
    private void subathon$addRenderToggle(MatrixStack par1, int par2, int par3, float par4, float par5, CallbackInfo ci) {
        if (!render) ci.cancel();
    }

    @Inject(method = "update", at = @At(value = "HEAD"), locals = LocalCapture.CAPTURE_FAILSOFT, remap = false)
    private void subathon$callUpdateListener(float delta, int mouseX, int mouseY, CallbackInfo ci) {
        updateListeners.forEach(listener -> listener.update(delta, mouseX, mouseY));
    }

    @Override
    public void subathon$registerUpdateListener(Listener listener) {
        updateListeners.add(listener);
    }
}
