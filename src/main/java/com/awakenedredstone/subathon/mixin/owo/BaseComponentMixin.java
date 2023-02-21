package com.awakenedredstone.subathon.mixin.owo;

import com.awakenedredstone.subathon.duck.ComponentDuck;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BaseComponent.class)
@Environment(EnvType.CLIENT)
public interface BaseComponentMixin {
    @Invoker int callDetermineVerticalContentSize(Sizing sizing);
    @Invoker int callDetermineHorizontalContentSize(Sizing sizing);
}
