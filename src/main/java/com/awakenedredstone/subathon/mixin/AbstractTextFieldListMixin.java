package com.awakenedredstone.subathon.mixin;

import me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Environment(value= EnvType.CLIENT)
@Mixin(TextFieldListEntry.class) @Pseudo
public abstract class AbstractTextFieldListMixin extends TooltipListEntry {

    public AbstractTextFieldListMixin(Text fieldName, @Nullable Supplier tooltipSupplier) {
        super(fieldName, tooltipSupplier);
    }

    @Inject(method = "textFieldPreRender", at = @At("TAIL"))
    private void textFieldPreRender(TextFieldWidget widget, CallbackInfo ci) {
        if (getConfigError().isPresent()) widget.setEditableColor(16733525);
        else widget.setEditableColor(14737632);
    }
}
