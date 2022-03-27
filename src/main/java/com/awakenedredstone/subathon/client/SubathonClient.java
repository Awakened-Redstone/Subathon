package com.awakenedredstone.subathon.client;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.events.HudRenderCallback;
import com.awakenedredstone.subathon.renderer.PositionedText;
import com.awakenedredstone.subathon.util.MessageUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
    private BotStatus botStatus = BotStatus.UNKNOWN;
    private boolean showData = false;
    private float value = 0.0f;

    @Override
    public void onInitializeClient() {
        //Packet sent by the server to inform the client that it has the mod
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(Subathon.MOD_ID, "has_mod"), (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                this.showData = true;
            });
        });

        //Packet sent by the server to inform the client the current modifier value
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(Subathon.MOD_ID, "value"), (client, handler, buf, responseSender) -> {
            float value = buf.readFloat();
            client.execute(() -> {
                this.value = value;
            });
        });

        //Packet sent by the server to inform the client the bot status
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(Subathon.MOD_ID, "bot_status"), (client, handler, buf, responseSender) -> {
            int status = buf.readInt();
            client.execute(() -> {
                switch (status) {
                    case 0 -> botStatus = BotStatus.OFFLINE;
                    case 1 -> botStatus = BotStatus.RUNNING;
                    default -> botStatus = BotStatus.UNKNOWN;
                }
            });
        });

        //Packet sent by the server to render a text on a chosen position of the screen
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


        HudRenderCallback.RENDER.register((matrix, delta) -> {
            positionedTexts.forEach((id, text) -> text.render(matrix, delta));
        });

        HudRenderCallback.TICK.register(() -> {
            positionedTexts.forEach((id, text) -> text.tick());
        });

        HudRenderCallback.PRE_TICK.register(paused -> {
            if (showData) {
                MinecraftClient client = MinecraftClient.getInstance();
                int fontScale = Subathon.getConfigData().fontScale;
                Text message = new TranslatableText("subathon.messages.value", MessageUtils.formatFloat(value));
                int y = client.getWindow().getScaledHeight() - (client.textRenderer.fontHeight * fontScale) - 4;
                int x = client.getWindow().getScaledWidth() - (client.textRenderer.getWidth(message) * fontScale) - 4;
                if (client.currentScreen instanceof ChatScreen) y -= 12;
                positionedTexts.put(-11L, new PositionedText(message, true, new int[]{x, y, 0xFFFFFF}, fontScale));
            }
        });

        HudRenderCallback.PRE_TICK.register(paused -> {
            if (showData) {
                if (botStatus == BotStatus.OFFLINE) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    int y = client.getWindow().getScaledHeight() - client.textRenderer.fontHeight - 4;
                    if (client.currentScreen instanceof ChatScreen) y -= 12;
                    positionedTexts.put(-12L, new PositionedText(new TranslatableText("subathon.messages.offline"), true, new int[]{4, y, 0xFF5555}));
                }
            }
        });
    }

    private enum BotStatus {
        UNKNOWN,
        RUNNING,
        OFFLINE
    }
}
