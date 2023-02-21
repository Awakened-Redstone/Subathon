package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.util.ConversionUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

    public RenderUpdateTimerCreative(CreativeInventoryScreen.CreativeScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SubathonClient.CLIENT_CONFIGS.showTimer() && SubathonClient.nextUpdate > -1) {
            Text message = Text.literal(ConversionUtils.ticksToSimpleTime(SubathonClient.nextUpdate));
            int width = textRenderer.getWidth(message);
            if (selectedTab == ItemGroups.INVENTORY) {
                this.textRenderer.draw(matrices, message, x + 130, this.y + 42, 0x404040);
            } else if (selectedTab != ItemGroups.SEARCH) {
                this.textRenderer.draw(matrices, message, x + 187 - width, this.y + 6, 0x404040);
            }
        }
    }
}