package old.config.cloth.options;

import net.minecraft.client.util.math.MatrixStack;

public interface RenderAction {
    void onRender(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta, ShortListEntry entry);
}
