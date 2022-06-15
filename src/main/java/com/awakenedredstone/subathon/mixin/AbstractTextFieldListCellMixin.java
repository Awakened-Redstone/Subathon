package com.awakenedredstone.subathon.mixin;

import me.shedaniel.clothconfig2.gui.entries.AbstractListListEntry;
import me.shedaniel.clothconfig2.gui.entries.AbstractTextFieldListListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value= EnvType.CLIENT)
@Mixin(AbstractTextFieldListListEntry.AbstractTextFieldListCell.class) @Pseudo
public abstract class AbstractTextFieldListCellMixin extends AbstractListListEntry.AbstractListCell {
    @Shadow protected TextFieldWidget widget;

    public AbstractTextFieldListCellMixin(@Nullable Object value, AbstractListListEntry listListEntry) {
        super(value, listListEntry);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta, CallbackInfo ci) {
        if (!isSelected) {
            fill(matrices, x, y + 12, x + entryWidth - 12, y + 13, this.getConfigError().isPresent() ? 0xffff5555 : 0xff707070);
        }
    }
}
