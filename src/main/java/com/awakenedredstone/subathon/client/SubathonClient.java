package com.awakenedredstone.subathon.client;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.renderer.PositionedText;
import com.awakenedredstone.subathon.util.MessageUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

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
                    Subathon.LOGGER.info("[POSITIONED TEXT] {}", text.getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n"));
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

        HudRenderCallback.EVENT.register((matrix, delta) -> {
            if (Subathon.integration.isRunning) {
                MinecraftClient client = MinecraftClient.getInstance();
                Text message = new TranslatableText("subathon.messages.value", MessageUtils.formatFloat(Subathon.integration.data.value));
                int y = client.getWindow().getScaledHeight() - client.textRenderer.fontHeight - 4;
                int x = client.getWindow().getScaledWidth() - client.textRenderer.getWidth(message) - 4;
                if (client.currentScreen instanceof ChatScreen) y -= 12;
                positionedTexts.put(-12L, new PositionedText(message, true, new int[]{x, y, 0xFFFFFF}));
            }
        });

        HudRenderCallback.EVENT.register((matrix, delta) -> {
            if (Subathon.getAuthData().access_token == null) {
                MinecraftClient client = MinecraftClient.getInstance();
                int y = client.getWindow().getScaledHeight() - client.textRenderer.fontHeight - 4;
                if (client.currentScreen instanceof ChatScreen) y -= 12;
                positionedTexts.put(-11L, new PositionedText(new TranslatableText("subathon.messages.not_connected"), true, new int[]{4, y, 0xFF5555}));
            } else if (!Subathon.integration.isRunning) {
                MinecraftClient client = MinecraftClient.getInstance();
                int y = client.getWindow().getScaledHeight() - client.textRenderer.fontHeight - 4;
                if (client.currentScreen instanceof ChatScreen) y -= 12;
                positionedTexts.put(-12L, new PositionedText(new TranslatableText("subathon.messages.offline"), true, new int[]{4, y, 0xFF5555}));
            }
        });
    }
}
