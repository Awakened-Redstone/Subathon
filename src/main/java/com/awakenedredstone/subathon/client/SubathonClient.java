package com.awakenedredstone.subathon.client;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.renderer.PositionedText;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class SubathonClient implements ClientModInitializer {
    private static final Map<Long, PositionedText> positionedTexts = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(Subathon.MOD_ID, "positioned_text"), (client, handler, buf, responseSender) -> {
            try {
                Text text = buf.readText();
                boolean withShadow = buf.readBoolean();
                boolean centeredText = buf.readBoolean();
                int[] values = buf.readIntArray();
                long id = buf.readLong();
                if (values.length != 3) {
                    Subathon.LOGGER.error("Packet \"suathon:positioned_text\" int array size is different than 3");
                    return;
                }
                int baseX = values[0];
                int baseY = values[1];
                client.execute(() -> {
                    final int[] positionedTextData = new int[3];
                    Text positionedText;
                    boolean positionedTextShadow;

                    positionedText = text;
                    positionedTextData[2] = values[2];
                    if (centeredText) {
                        positionedTextData[0] = client.getWindow().getScaledWidth() / 2 - client.textRenderer.getWidth(text) / 2;
                    } else {
                        positionedTextData[0] = baseX;
                    }
                    positionedTextData[1] = baseY;
                    positionedTextShadow = withShadow;

                    positionedTexts.put(id, new PositionedText(positionedText, positionedTextShadow, positionedTextData));
                });
            } catch (Exception e) {
                Subathon.LOGGER.error("Packet \"suathon:positioned_text\" failed to be processed", e);
            }
        });

        HudRenderCallback.EVENT.register((matrix, delta) -> {
            positionedTexts.forEach((id, text) -> text.render(matrix));
        });
    }
}
