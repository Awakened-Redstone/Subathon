package com.awakenedredstone.subathon.client;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.screen.EventLogScreen;
import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.events.HudRenderCallback;
import com.awakenedredstone.subathon.renderer.PositionedText;
import com.awakenedredstone.subathon.twitch.Subscription;
import com.awakenedredstone.subathon.util.BotStatus;
import com.awakenedredstone.subathon.util.MessageUtils;
import de.guntram.mcmod.crowdintranslate.CrowdinTranslate;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.awakenedredstone.subathon.util.ConversionUtils.toFloat;

@Environment(EnvType.CLIENT)
public class SubathonClient implements ClientModInitializer {
    public static final Map<Long, PositionedText> positionedTexts = new HashMap<>();
    public static final List<TwitchEvent> events = new ArrayList<>();
    private BotStatus botStatus = BotStatus.UNKNOWN;
    private boolean showData = false;
    public static double value = 0.0;
    public static int resetTimer = 0;
    public static int updateTimer = 0;
    public static int serverTicks = 0;

    private static KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.subathon.event_logs", // The translation key of the keybinding's name
            InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_R, // The keycode of the key
            "category.subathon.keybinds" // The translation key of the keybinding's category.
    ));

    @Override
    public void onInitializeClient() {
        CrowdinTranslate.downloadTranslations("subathon-mod", Subathon.MOD_ID);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keyBinding.isPressed()) {
                client.setScreen(new EventLogScreen(client.currentScreen));
            }
            while (keyBinding.wasPressed()) {}
        });

        //Packet sent by the server to inform the client that it has the mod
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(Subathon.MOD_ID, "has_mod"), (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                this.showData = true;
                responseSender.sendPacket(new Identifier("has_mod"), PacketByteBufs.create());
            });
        });

        //Packet sent by the server to inform the client the current modifier value
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(Subathon.MOD_ID, "value"), (client, handler, buf, responseSender) -> {
            double value = buf.readDouble();
            client.execute(() -> {
                SubathonClient.value = value;
            });
        });

        //Packet sent by the server to inform the client the current tick
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(Subathon.MOD_ID, "server_ticks"), (client, handler, buf, responseSender) -> {
            int value = buf.readInt();
            client.execute(() -> {
                SubathonClient.serverTicks = value;
            });
        });

        //Packet sent by the server to inform the client the timer values
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(Subathon.MOD_ID, "timers"), (client, handler, buf, responseSender) -> {
            int resetTimer = buf.readInt();
            int updateTimer = buf.readInt();
            client.execute(() -> {
                SubathonClient.resetTimer = resetTimer;
                SubathonClient.updateTimer = updateTimer;
            });
        });

        //Packet sent by the server to inform the client the bot status
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(Subathon.MOD_ID, "bot_status"), (client, handler, buf, responseSender) -> {
            BotStatus status = buf.readEnumConstant(BotStatus.class);
            client.execute(() -> {
                botStatus = status;
            });
        });

        //Packet sent by the server with a new event
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(Subathon.MOD_ID, "event"), (client, handler, buf, responseSender) -> {
             String name = buf.readString();
             int amount = buf.readInt();
             Subscription tier = buf.readEnumConstant(Subscription.class);
             SubathonCommand.Events event = buf.readEnumConstant(SubathonCommand.Events.class);
             String target = buf.readString();
            client.execute(() -> {
                TwitchEvent twitchEvent = new TwitchEvent(name, amount, tier, event, target);
                events.add(twitchEvent);

                if (client.currentScreen instanceof EventLogScreen) {
                    ((EventLogScreen) client.currentScreen).addToList(twitchEvent);
                }
            });
        });

        //Deprecated: Packet sent by the server to render a text on a chosen position of the screen
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
                client.execute(() -> {
                    Subathon.LOGGER.info("[POSITIONED TEXT] {}", text.getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n"));
                    final int[] positionedTextData = values.clone();
                    Text positionedText;
                    boolean positionedTextShadow;

                    positionedText = text;
                    if (centeredText) {
                        positionedTextData[0] = client.getWindow().getScaledWidth() / 2 - client.textRenderer.getWidth(text) / 2;
                    } else {
                        positionedTextData[0] = values[0];
                    }
                    positionedTextShadow = withShadow;

                    positionedTexts.put(id, new PositionedText(positionedText, positionedTextShadow, positionedTextData));
                });
            } catch (Exception e) {
                Subathon.LOGGER.error("Packet \"suathon:positioned_text\" failed to be processed", e);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (showData && serverTicks != 0 && !client.isPaused()) {
                serverTicks++;
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
                float fontScale = Subathon.getConfigData().fontScale;
                Text message = new TranslatableText("text.subathon.integration.value", MessageUtils.formatDouble(value));
                float y = toFloat(client.getWindow().getScaledHeight() - (client.textRenderer.fontHeight * fontScale) - 4);
                float x = toFloat(client.getWindow().getScaledWidth() - (client.textRenderer.getWidth(message) * fontScale) - 4);
                if (client.currentScreen instanceof ChatScreen) y -= 12;
                IntegratedServer minecraftServer = client.getServer();
                if (client.options.showAutosaveIndicator && minecraftServer != null && minecraftServer.isSaving()) y -= 12;
                positionedTexts.put(-11L, new PositionedText(message, true, x, y, 0xFFFFFF, fontScale));

                if (Subathon.getConfigData().showResetTimer && resetTimer != 0 && serverTicks != 0) {
                    message = new LiteralText(MessageUtils.ticksToSimpleTime(resetTimer - serverTicks % resetTimer));
                    x = toFloat(client.getWindow().getScaledWidth() - (client.textRenderer.getWidth(message) * (fontScale / 2)) - 4);
                    positionedTexts.put(-10L, new PositionedText(message, true, x, y -= 12 * fontScale / 2, 0xFFFFFF, fontScale / 2, 4, 0, 0));
                }

                if (Subathon.getConfigData().showUpdateTimer && updateTimer != 0 && serverTicks != 0) {
                    message = new LiteralText(MessageUtils.ticksToSimpleTime(updateTimer - serverTicks % updateTimer + 19));
                    x = toFloat(client.getWindow().getScaledWidth() - (client.textRenderer.getWidth(message) * (fontScale / 2)) - 4);
                    positionedTexts.put(-9L, new PositionedText(message, true, x, y -= 12 * fontScale / 2, 0xFFFFFF, fontScale / 2, 4, 0, 0));
                }
            }
        });

        HudRenderCallback.PRE_TICK.register(paused -> {
            if (showData) {
                if (botStatus != BotStatus.UNKNOWN) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    int y = client.getWindow().getScaledHeight() - client.textRenderer.fontHeight - 4;
                    if (client.currentScreen instanceof ChatScreen) y -= 12;
                    String status = botStatus.name().toLowerCase();
                    positionedTexts.put(-12L, new PositionedText(new TranslatableText(String.format("text.subathon.integration.%s", status)),
                            true, new int[]{4, y, 0xFFFFFF}));
                }
            }
        });
    }
}
