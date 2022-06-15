package com.awakenedredstone.subathon.config.cloth.options;

import net.minecraft.client.util.math.MatrixStack;

@FunctionalInterface
public interface RenderAction<Entry> {
    void onRender(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta, Entry entry);
}
