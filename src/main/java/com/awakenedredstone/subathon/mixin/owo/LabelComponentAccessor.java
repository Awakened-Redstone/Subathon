package com.awakenedredstone.subathon.mixin.owo;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LabelComponent.class)
@Environment(EnvType.CLIENT)
public interface LabelComponentAccessor {
    @Invoker int callDetermineVerticalContentSize(Sizing sizing);
    @Invoker int callDetermineHorizontalContentSize(Sizing sizing);
}
