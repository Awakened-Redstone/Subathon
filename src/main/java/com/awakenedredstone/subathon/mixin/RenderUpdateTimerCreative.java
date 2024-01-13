package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.util.ConversionUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value = EnvType.CLIENT)
@Mixin(CreativeInventoryScreen.class)
public abstract class RenderUpdateTimerCreative extends AbstractInventoryScreen<CreativeInventoryScreen.CreativeScreenHandler> {
    @Shadow private static ItemGroup selectedTab;

    @Shadow public abstract void render(DrawContext context, int mouseX, int mouseY, float delta);

    public RenderUpdateTimerCreative(CreativeInventoryScreen.CreativeScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SubathonClient.CLIENT_CONFIGS.showTimer() && SubathonClient.getInstance().nextUpdate > -1) {
            Text message = Text.literal(ConversionUtils.ticksToSimpleTime(SubathonClient.getInstance().nextUpdate));
            int x;
            int y;
            if (selectedTab.getType() == ItemGroup.Type.SEARCH) return;
            if (selectedTab.getType() == ItemGroup.Type.INVENTORY) {
                x = this.x + 130;
                y = this.y + 42;
            } else {
                int width = textRenderer.getWidth(message);
                x = this.x + 187 - width;
                y = this.y + 6;
            }
            context.drawText(this.textRenderer, message, x, y, 0x404040, false);
        }
    }
}