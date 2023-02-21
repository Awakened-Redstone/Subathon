package com.awakenedredstone.subathon.mixin.owo;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(ConfigScreen.class)
@Environment(EnvType.CLIENT)
public interface ConfigScreenMixin {
    //@Invoker OptionComponentFactory callFactoryForOption(Option<?> option);
    @Accessor static Map<Predicate<Option<?>>, OptionComponentFactory<?>> getDEFAULT_FACTORIES() { return null; };
}
