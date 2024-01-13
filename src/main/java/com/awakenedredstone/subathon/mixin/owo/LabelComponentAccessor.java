package com.awakenedredstone.subathon.mixin.owo;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = LabelComponent.class, remap = false)
@Environment(EnvType.CLIENT)
public interface LabelComponentAccessor {
    @Invoker(remap = false) int callDetermineVerticalContentSize(Sizing sizing);
    @Invoker(remap = false) int callDetermineHorizontalContentSize(Sizing sizing);
}