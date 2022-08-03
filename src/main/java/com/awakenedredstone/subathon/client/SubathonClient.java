package com.awakenedredstone.subathon.client;

import com.awakenedredstone.cubecontroller.events.HudRenderEvents;
import com.awakenedredstone.cubecontroller.events.MinecraftClientCallback;
import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.screen.EventLogScreen;
import com.awakenedredstone.subathon.client.screen.PercentageScreen;
import com.awakenedredstone.subathon.client.screen.PickChannelScreen;
import com.awakenedredstone.subathon.client.texture.TwitchSpriteManager;
import com.awakenedredstone.subathon.client.toast.TwitchEventToast;
import com.awakenedredstone.subathon.commands.SubathonCommand;
import com.awakenedredstone.subathon.twitch.Subscription;
import com.awakenedredstone.subathon.util.IntegrationStatus;
import de.guntram.mcmod.crowdintranslate.CrowdinTranslate;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.lang.StringUtils;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.awakenedredstone.subathon.Subathon.getConfigData;
import static com.awakenedredstone.subathon.Subathon.identifier;

@Environment(EnvType.CLIENT)
public class SubathonClient implements ClientModInitializer {
    private static boolean showData = false;
    private static IntegrationStatus integrationStatus = IntegrationStatus.UNKNOWN;
    public static TwitchSpriteManager twitchSpriteManager;
    public static int nextUpdate = -1;
    public static final List<TwitchEvent> events = new ArrayList<>();
    public static final List<Identifier> TWITCH_SPRITES = new ArrayList<>(Arrays.asList(identifier("gift"),
            identifier("1"),
            identifier("100"),
            identifier("1000"),
            identifier("10000"),
            identifier("100000")));

    private static final KeyBinding logKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.subathon.event_logs", // The translation key of the keybinding's user
            InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_R, // The keycode of the key
            "category.subathon.keybinds" // The translation key of the keybinding's category.
    ));
    private static final KeyBinding rewardKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.subathon.pick_reward", // The translation key of the keybinding's user
            InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_H, // The keycode of the key
            "category.subathon.keybinds" // The translation key of the keybinding's category.
    ));
    private static final KeyBinding percentageKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.subathon.percentage", // The translation key of the keybinding's user
            InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_O, // The keycode of the key
            "category.subathon.keybinds" // The translation key of the keybinding's category.
    ));

    @Override
    public void onInitializeClient() {
        CrowdinTranslate.downloadTranslations("subathon-mod", Subathon.MOD_ID);

        MinecraftClientCallback.SPRITE_MANAGER.register(client -> {
            twitchSpriteManager = new TwitchSpriteManager(client.getTextureManager());
            ((ReloadableResourceManagerImpl) client.getResourceManager()).registerReloader(twitchSpriteManager);
        });

        ClientLoginConnectionEvents.DISCONNECT.register((handler, client) -> {
            events.clear();
            integrationStatus = IntegrationStatus.UNKNOWN;
            showData = false;
            nextUpdate = -1;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (logKeyBinding.isPressed()) {
                client.setScreen(new EventLogScreen(client.currentScreen));
            }

            //TODO: Multiplayer support
            if (rewardKeyBinding.isPressed()) {
                if (client.getServer() == null || client.getServer().isDedicated() || client.isConnectedToRealms()) {
                    MutableText error = Text.literal("This function only works on singleplayer!");
                    if (client.player != null) {
                        client.player.sendMessage(error.formatted(Formatting.RED));
                    } else {
                        client.getToastManager().add(new SystemToast(SystemToast.Type.TUTORIAL_HINT, Text.literal("Function unavailable"), error));
                    }
                } else if (client.player != null && !client.player.hasPermissionLevel(2)) {
                    MutableText error = Text.literal("You need operator to use this function!");
                    client.player.sendMessage(error.formatted(Formatting.RED));
                }
                client.setScreen(new PickChannelScreen(client.currentScreen));
            }

            //TODO: Multiplayer support
            if (percentageKeyBinding.isPressed()) {
                if (client.getServer() == null || client.getServer().isDedicated() || client.isConnectedToRealms()) {
                    MutableText error = Text.literal("This function only works on singleplayer!");
                    if (client.player != null) {
                        client.player.sendMessage(error.formatted(Formatting.RED));
                    } else {
                        client.getToastManager().add(new SystemToast(SystemToast.Type.TUTORIAL_HINT, Text.literal("Function unavailable"), error));
                    }
                } else if (client.player != null && !client.player.hasPermissionLevel(2)) {
                    MutableText error = Text.literal("You need operator to use this function!");
                    client.player.sendMessage(error.formatted(Formatting.RED));
                }
                client.setScreen(new PercentageScreen(client.currentScreen));
            }

            while (logKeyBinding.wasPressed()) {/**/}
            while (rewardKeyBinding.wasPressed()) {/**/}
            while (percentageKeyBinding.wasPressed()) {/**/}
        });

        //Packet sent by the server to inform the client that it has the mod
        ClientPlayNetworking.registerGlobalReceiver(Subathon.identifier("has_mod"), (client, handler, buf, responseSender) ->
                client.execute(() -> {
                    showData = true;
                    responseSender.sendPacket(new Identifier("has_mod"), PacketByteBufs.create());
                }));

        //Packet sent by the server to inform the client the current tick
        ClientPlayNetworking.registerGlobalReceiver(Subathon.identifier("next_update"), (client, handler, buf, responseSender) -> {
            int value = buf.readInt();
            client.execute(() -> SubathonClient.nextUpdate = value);
        });

        //Packet sent by the server to inform the client the bot status
        ClientPlayNetworking.registerGlobalReceiver(Subathon.identifier("bot_status"), (client, handler, buf, responseSender) -> {
            IntegrationStatus status = buf.readEnumConstant(IntegrationStatus.class);
            client.execute(() -> integrationStatus = status);
        });

        //Packet sent by the server with a new event
        ClientPlayNetworking.registerGlobalReceiver(Subathon.identifier("event"), (client, handler, buf, responseSender) -> {
            String user = buf.readString();
            String target = buf.readString();
            int amount = buf.readInt();
            Subscription tier = buf.readEnumConstant(Subscription.class);
            SubathonCommand.Events event = buf.readEnumConstant(SubathonCommand.Events.class);
            String message = buf.readString();
            client.execute(() -> {
                TwitchEvent twitchEvent = new TwitchEvent(user, amount, tier, event, target, message);
                events.add(twitchEvent);

                if (client.currentScreen instanceof EventLogScreen) {
                    ((EventLogScreen) client.currentScreen).addToList(twitchEvent);
                }

                switch (event) {
                    case SUBSCRIPTION -> {
                        if (getConfigData().showEventsInChat)
                            client.inGameHud.getChatHud().addMessage(Text.translatable("text.subathon.event.subscription", user, tier.getName()));

                        if (getConfigData().showToasts && tier.getValue() >= getConfigData().minSubTierForToast.getValue()) {
                            Text title = Text.translatable("toast.event.sub", tier.getName());
                            Text msg = Text.translatable("toast.event.sub.message", user, tier.getName());
                            client.getToastManager().add(new TwitchEventToast(identifier("gift"), title, msg));
                        }
                    }
                    case RESUBSCRIPTION -> {
                        if (getConfigData().showEventsInChat)
                            client.inGameHud.getChatHud().addMessage(Text.translatable("text.subathon.event.subscription", user, tier.getName()));

                        if (getConfigData().showToasts && tier.getValue() >= getConfigData().minSubTierForToast.getValue()) {
                            Text title = Text.translatable("toast.event.resub", tier.getName());
                            Text msg;
                            List<OrderedText> list = client.textRenderer.wrapLines(Text.literal(user + ": " + message), 125);
                            if (list.size() <= 2 && StringUtils.isNotBlank(message)) {
                                msg = Text.literal(user + ": " + message);
                            } else msg = Text.translatable("toast.event.resub.message", user, tier.getName());
                            client.getToastManager().add(new TwitchEventToast(identifier("gift"), title, msg));
                        }
                    }
                    case SUB_GIFT -> {
                        String key = amount != 1 ? "text.subathon.event.gift.plural" : "text.subathon.event.gift.singular";
                        if (getConfigData().showEventsInChat)
                            client.inGameHud.getChatHud().addMessage(Text.translatable(key, user, amount, tier.getName()));

                        if (getConfigData().showToasts && amount >= getConfigData().minSubsGiftedForToast.get(tier)) {
                            Text title = Text.translatable("toast.event.gift", tier.getName(), amount);
                            Text msg = Text.translatable("toast.event.gift.message", user, amount, tier.getName());
                            client.getToastManager().add(new TwitchEventToast(identifier("gift"), title, msg));
                        }
                    }
                    case GIFT_USER -> {
                        if (getConfigData().showEventsInChat)
                            client.inGameHud.getChatHud().addMessage(Text.translatable("text.subathon.event.gift_user", user, target, tier.getName()));
                    }
                    case CHEER -> {
                        if (getConfigData().showEventsInChat) {
                            MutableText message1 = Text.translatable("text.subathon.event.cheer", user, amount);
                            message1.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(message))));
                            client.inGameHud.getChatHud().addMessage(message1);
                        }

                        if (getConfigData().showToasts && amount >= getConfigData().minBitsForToast) {
                            Text title = Text.translatable("toast.event.cheer", amount);
                            Text msg;
                            List<OrderedText> list = client.textRenderer.wrapLines(Text.literal(user + ": " + message), 125);
                            if (list.size() <= 2) {
                                msg = Text.literal(user + ": " + message);
                            } else msg = Text.translatable("toast.event.cheer.message", user, amount);
                            client.getToastManager().add(new TwitchEventToast(getBitsBadge(amount), title, msg));
                        }
                    }
                    case REWARD -> {
                        if (getConfigData().showEventsInChat)
                            client.inGameHud.getChatHud().addMessage(Text.translatable("text.subathon.event.reward", user));
                    }
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (showData && nextUpdate > 0 && !client.isPaused()) {
                nextUpdate--;
            }
        });

        HudRenderEvents.PRE_TICK.register(paused -> {
            if (showData) {
                if (integrationStatus != IntegrationStatus.UNKNOWN) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    int y = client.getWindow().getScaledHeight() - client.textRenderer.fontHeight - 4;
                    if (client.currentScreen instanceof ChatScreen) y -= 12;
                    String status = integrationStatus.name().toLowerCase();
                    client.textRenderer.drawWithShadow(new MatrixStack(), Text.translatable(String.format("text.subathon.integration.%s", status)), 4, y, 0xFFFFFF);
                }
            }
        });
    }

    private Identifier getBitsBadge(int amount) {
        if (amount >= 100000) return identifier("100000");
        else if (amount >= 10000) return identifier("10000");
        else if (amount >= 1000) return identifier("1000");
        else if (amount >= 100) return identifier("100");
        else return identifier("1");
    }
}
