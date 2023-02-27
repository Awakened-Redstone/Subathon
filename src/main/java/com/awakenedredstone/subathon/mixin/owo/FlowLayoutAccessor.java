package com.awakenedredstone.subathon.mixin.owo;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(value = FlowLayout.class, remap = false)
public interface FlowLayoutAccessor {
    @Invoker int callDetermineVerticalContentSize(Sizing sizing);
    @Invoker int callDetermineHorizontalContentSize(Sizing sizing);
}
