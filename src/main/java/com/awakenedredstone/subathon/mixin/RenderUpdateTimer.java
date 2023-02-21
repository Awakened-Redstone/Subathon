package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.util.ConversionUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value = EnvType.CLIENT)
@Mixin(InventoryScreen.class)
public abstract class RenderUpdateTimer extends AbstractInventoryScreen<PlayerScreenHandler> {
    public RenderUpdateTimer(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SubathonClient.CLIENT_CONFIGS.showTimer() && SubathonClient.nextUpdate > -1) {
            Text message = Text.literal(ConversionUtils.ticksToSimpleTime(SubathonClient.nextUpdate));
            this.textRenderer.draw(matrices, message, x + 129, this.y + 74, 0x404040);
        }
    }
}