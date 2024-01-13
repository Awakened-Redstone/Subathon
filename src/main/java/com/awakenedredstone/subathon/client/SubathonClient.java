package com.awakenedredstone.subathon.client;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonPrimitive;
import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.command.SubathonClientCommand;
import com.awakenedredstone.subathon.client.render.entity.BigBombEntityRenderer;
import com.awakenedredstone.subathon.client.render.entity.model.BigBombModel;
import com.awakenedredstone.subathon.client.render.fx.Listener;
import com.awakenedredstone.subathon.client.render.fx.Shockwave;
import com.awakenedredstone.subathon.client.toast.TwitchEventToast;
import com.awakenedredstone.subathon.client.ui.ConnectScreen;
import com.awakenedredstone.subathon.client.ui.NotificationsScreen;
import com.awakenedredstone.subathon.client.ui.components.PointsComponent;
import com.awakenedredstone.subathon.config.ClientConfigs;
import com.awakenedredstone.subathon.config.ConfigsClient;
import com.awakenedredstone.subathon.duck.owo.ComponentDuck;
import com.awakenedredstone.subathon.mixin.owo.LabelComponentAccessor;
import com.awakenedredstone.subathon.owo.SubathonTextBox;
import com.awakenedredstone.subathon.registry.EntityRegistry;
import com.awakenedredstone.subathon.integration.twitch.EventMessages;
import com.awakenedredstone.subathon.integration.twitch.Twitch;
import com.awakenedredstone.subathon.util.ClientUtils;
import com.awakenedredstone.subathon.util.MapBuilder;
import com.awakenedredstone.subathon.util.Texts;
import com.github.twitch4j.graphql.internal.FetchCommunityPointsSettingsQuery;
import io.github.xanthic.cache.api.Cache;
import io.github.xanthic.cache.core.CacheApi;
import io.wispforest.owo.ui.base.BaseParentComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Easing;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.hud.Hud;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.ui.parsing.UIModelLoader;
import io.wispforest.owo.ui.parsing.UIParsing;
import lombok.Getter;
import me.x150.renderer.event.RenderEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.krlite.equator.visual.color.AccurateColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.apache.http.util.Asserts;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static com.awakenedredstone.subathon.Subathon.id;
import static com.awakenedredstone.subathon.Subathon.spriteId;

@Environment(EnvType.CLIENT)
public class SubathonClient implements ClientModInitializer {
    public static final EntityModelLayer BIG_BOMB_MODEL_LAYER = new EntityModelLayer(Subathon.id("big_bomb"), "main");

    @Getter
    private static SubathonClient instance;
    private boolean compatibleServer = false;

    public final List<Notification> messages = new ArrayList<>();
    public final Queue<Notification> quickMessages = new LinkedList<>();
    public final Queue<QuickMessageTimer> quickMessageTimers = new LinkedList<>();

    //In memory cache
    public final Cache<String, String> cache = CacheApi.create(spec -> {/**/});
    public final Cache<String, FlowLayout> hudRenderCache = CacheApi.create(spec -> {/**/});
    private UIModel model = null;
    public int nextUpdate = -1;

    public final Set<FetchCommunityPointsSettingsQuery.CustomReward> rewards = new LinkedHashSet<>();
    public final RuntimeRewardTextures runtimeRewardTextures = new RuntimeRewardTextures();
    public final Map<String, Twitch.ConnectionState> connectionStatus = new HashMap<>();
    public ConfigsClient.ConnectionType connectionType = null;
    public boolean authenticated = false;

    @Getter @Deprecated
    private final List<Shockwave> shockwaves = new ArrayList<>();

    public static final ClientConfigs CLIENT_CONFIGS;

    private final KeyBinding connectKeybind = ClientUtils.addKeybind("connect", GLFW.GLFW_KEY_F4);
    private final KeyBinding notificationsKeybind = ClientUtils.addKeybind("notifications", GLFW.GLFW_KEY_H);
    //private final KeyBinding devKeybind = ClientUtils.addKeybind("dev", GLFW.GLFW_KEY_R);

    static {
        CLIENT_CONFIGS = ClientConfigs.createAndLoad(builder -> {
            builder.registerSerializer(UUID.class, (uuid, marshaller) -> {
                JsonArray array = new JsonArray();
                array.add(new JsonPrimitive(uuid.getMostSignificantBits()));
                array.add(new JsonPrimitive(uuid.getLeastSignificantBits()));
                return array;
            });
            builder.registerDeserializer(JsonArray.class, UUID.class, (json, m) -> new UUID(json.getLong(0, 0), json.getLong(1, 0)));
        });
    }

    @Override
    public void onInitializeClient() {
        instance = this;

        //RenderEvents.WORLD.register(Listener::onWorldRendered);
        WorldRenderEvents.END.register(Listener::onWorldRendered);
        RenderEvents.HUD.register(Listener::onHudRendered);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> SubathonClientCommand.register(dispatcher));

        EntityRendererRegistry.register(EntityRegistry.FIREBALL, (context) -> new FlyingItemEntityRenderer<>(context, 3.0f, true));
        EntityRendererRegistry.register(EntityRegistry.BIG_BOMB, BigBombEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(BIG_BOMB_MODEL_LAYER, BigBombModel::getTexturedModelData);

        registerPacketListeners();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (compatibleServer) {
                while (connectKeybind.wasPressed()) {
                    client.setScreen(new ConnectScreen());
                }

                while (notificationsKeybind.wasPressed()) {
                    client.setScreen(new NotificationsScreen());
                }
            }

            //while (devKeybind.wasPressed()) {}
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            removePointsHud();
            authenticated = false;
            connectionStatus.clear();
            runtimeRewardTextures.clear();
            rewards.clear();
            cache.clear();
        });

        Hud.add(Subathon.id("quick_notifications"), () -> {
            if (model == null) model = UIModelLoader.getPreloaded(id("notifications"));
            FlowLayout template = model.expandTemplate(FlowLayout.class, "notifications", new HashMap<>());
            hudRenderCache.put("subathon-notifications-parent", template);
            return template;
        });

        CLIENT_CONFIGS.subscribeToShowValue(show -> {
            if (show) {
                addPointsHud();
            } else {
                removePointsHud();
            }
        });

        CLIENT_CONFIGS.subscribeToPointsFontScale(scale -> {
            if (Hud.hasComponent(Subathon.id("points_view"))) {
                Component hud = Hud.getComponent(id("points_view"));
                if (hud instanceof FlowLayout parent) {
                    Component children = parent.children().get(0);
                    if (children instanceof PointsComponent) {
                        ((PointsComponent) children).scale(scale);
                    }
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                if (model == null) model = UIModelLoader.getPreloaded(id("notifications"));

                FlowLayout parent = hudRenderCache.get("subathon-notifications-parent");

                if (parent == null || !compatibleServer) return;

                quickMessageTimers.removeIf(timer -> !parent.children().contains(timer.component) || timer.timer < -20);

                while (!quickMessages.isEmpty()) {
                    Notification notification = quickMessages.poll();
                    FlowLayout template = model.expandTemplate(FlowLayout.class, "notification", new HashMap<>());
                    LabelComponent label = template.childById(LabelComponent.class, "subathon.notification.label");
                    LabelComponentAccessor labelMixin = (LabelComponentAccessor) label;
                    label.text(Texts.of(notification.quickMessage.translation, map -> map.putAll(notification.placeholders)));
                    quickMessageTimers.add(new QuickMessageTimer(template, client));
                    int vSize = Sizing.content(2).inflate(0, labelMixin::callDetermineVerticalContentSize);
                    int hSize = Sizing.content(4).inflate(0, labelMixin::callDetermineHorizontalContentSize);
                    template.verticalSizing().animate(200, Easing.CUBIC, Sizing.fixed(vSize)).forwards();
                    template.horizontalSizing(Sizing.fixed(hSize));
                    parent.child(template);
                }

                List<Component> children = parent.children();
                while (children.size() > 10) {
                    parent.removeChild(children.get(0));
                }
            }
        });
    }

    private void registerPacketListeners() {
        ClientPlayNetworking.registerGlobalReceiver(Subathon.id("mod_version"), (client, handler, buf, responseSender) -> {
            String serverVersion = buf.readString();
            client.execute(() -> {
                compatibleServer = true;
                @SuppressWarnings("OptionalGetWithoutIsPresent")
                String version = FabricLoader.getInstance().getModContainer(Subathon.MOD_ID).get().getMetadata().getVersion().getFriendlyString();
                if (serverVersion.equals(version)) this.compatibleServer = true;
                else {
                    SystemToast systemToast = SystemToast.create(client, SystemToast.Type.NARRATOR_TOGGLE,
                            Text.translatable("toast.subathon.warning.mismatch_version.title"),
                            Text.translatable("toast.subathon.warning.mismatch_version.description"));

                    client.getToastManager().add(systemToast);
                }
                addPointsHud();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Subathon.id("eventsub_warning"), (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                SystemToast systemToast = SystemToast.create(client, SystemToast.Type.NARRATOR_TOGGLE,
                        Text.translatable("toast.subathon.warning.eventsub.title"),
                        Text.translatable("toast.subathon.warning.eventsub.description"));

                client.getToastManager().add(systemToast);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Subathon.id("ui_warning"), (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                SystemToast systemToast = SystemToast.create(client, SystemToast.Type.NARRATOR_TOGGLE,
                        Text.translatable("toast.subathon.warning.ui.title"),
                        Text.translatable("toast.subathon.warning.ui.description"));

                client.getToastManager().add(systemToast);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Subathon.id("twitch_status"), (client, handler, buf, responseSender) -> {
            String object = buf.readString();
            Twitch.ConnectionState status = buf.readEnumConstant(Twitch.ConnectionState.class);
            Twitch.ConnectionType type = buf.readEnumConstant(Twitch.ConnectionType.class);
            boolean complete = buf.readBoolean();
            client.execute(() -> {
                if (object.isEmpty()) {
                    SystemToast systemToast = SystemToast.create(client, SystemToast.Type.NARRATOR_TOGGLE,
                            Text.translatable("toast.subathon.error.eventsub.unknown_connection.title"),
                            Text.translatable("toast.subathon.error.eventsub.unknown_connection.description"));

                    client.getToastManager().add(systemToast);
                    return;
                }

                this.connectionStatus.put(object, status);
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.f, 0.1f));
                if (type == Twitch.ConnectionType.CONNECT) {
                    if (complete) {
                        this.authenticated = true;
                        if (SubathonClient.CLIENT_CONFIGS.rewardId() != null) {
                            UUID rewardId = SubathonClient.CLIENT_CONFIGS.rewardId();
                            Twitch.getInstance().toggleReward(this.cache.get("token"), rewardId, true);
                            ClientPlayNetworking.send(Subathon.id("reward_id"), PacketByteBufs.create().writeUuid(safeUUID(rewardId)));
                        }
                    }
                } else if (type == Twitch.ConnectionType.DISCONNECT) {
                    if (complete) {
                        this.authenticated = false;
                        this.connectionType = null;
                    }
                }
                if (client.currentScreen instanceof ConnectScreen screen) {
                    FlowLayout rootComponent = screen.rootComponent();

                    FlowLayout items = screen.getComponent(rootComponent, FlowLayout.class, "connect-items");
                    ButtonComponent connectButton = screen.getComponent(rootComponent, ButtonComponent.class, "connect-button");

                    FlowLayout extraOptions = screen.getExtraOptions();
                    Asserts.notNull(extraOptions, "extraOptions");

                    ButtonComponent disconnectButton = screen.getComponent(extraOptions, ButtonComponent.class, "disconnect");
                    //ButtonComponent reconnectButton = screen.getComponent(extraOptions, ButtonComponent.class, "reconnect");
                    ButtonComponent resetCacheButton = screen.getComponent(extraOptions, ButtonComponent.class, "reset-cache");
                    ButtonComponent resetKeyButton = screen.getComponent(extraOptions, ButtonComponent.class, "reset-key");

                    if (type == Twitch.ConnectionType.CONNECT) {
                        if (complete) {
                            connectButton.setMessage(Text.translatable("text.subathon.screen.connect.button.connected"));
                            disconnectButton.active = true;
                            //reconnectButton.active = true;
                        } else {
                            connectButton.setMessage(Text.translatable("text.subathon.screen.connect.button.packets"));
                            connectButton.active = false;
                        }
                    } else if (type == Twitch.ConnectionType.DISCONNECT) {
                        if (complete) {
                            connectButton.setMessage(Text.translatable("text.subathon.screen.connect.button.connect"));
                            connectButton.active = true;
                            disconnectButton.active = false;
                            //reconnectButton.active = false;
                            resetCacheButton.active = true;
                            resetKeyButton.active = Subathon.CONFIG_DIR.resolve("auth").toFile().exists();
                        }
                    } else if (status != Twitch.ConnectionState.UNKNOWN) {
                        connectButton.setMessage(Text.translatable("text.subathon.screen.connect.button.packets"));
                        connectButton.active = false;
                    }
                    FlowLayout item = screen.getComponent(items, FlowLayout.class, "connect." + object);
                    LabelComponent statusComponent = screen.getComponent(item, LabelComponent.class, "status");
                    statusComponent.text(Text.translatable("text.subathon.screen.connect." + status.name().toLowerCase()));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Subathon.id("account_name"), (client, handler, buf, responseSender) -> {
            String name = buf.readString();
            client.execute(() -> {
                cache.put("accountName", name);
                if (client.currentScreen instanceof ConnectScreen screen) {
                    var accountLabel = screen.getComponent(screen.rootComponent(), LabelComponent.class, "account");

                    if (this.cache.get("accountName") != null) {
                        if (this.connectionType.requiresAuth()) {
                            accountLabel.text(Texts.of("text.subathon.screen.connect.account", new MapBuilder.StringMap()
                                    .putAny("user", this.cache.get("accountName"))
                                    .build()));
                        } else {
                            accountLabel.text(Texts.of("text.subathon.screen.connect.account.authless", new MapBuilder.StringMap()
                                    .putAny("user", this.cache.get("accountName"))
                                    .build()));
                        }
                    } else {
                        accountLabel.text(Texts.of("text.subathon.screen.connect.account.disconnected"));
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Subathon.id("error"), (client, handler, buf, responseSender) -> {
            String message = buf.readString();
            client.execute(() -> {
                client.getToastManager().add(new TwitchEventToast(spriteId("warning"),
                        Text.translatable("toast.subathon.error." + message + ".title"),
                        Text.translatable("toast.subathon.error." + message + ".description")));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Subathon.id("connection_fail"), (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                client.getToastManager().add(new TwitchEventToast(spriteId("warning"),
                        Text.translatable("toast.subathon.error.eventsub.connection.failed.title"),
                        Text.translatable("toast.subathon.error.eventsub.connection.failed.description")));

                authenticated = false;

                if (client.currentScreen instanceof ConnectScreen screen) {
                    FlowLayout rootComponent = screen.rootComponent();
                    ButtonComponent connectButton = screen.getComponent(rootComponent, ButtonComponent.class, "connect-button");
                    Asserts.notNull(connectButton, "connectButton");

                    var accountLabel = screen.getComponent(rootComponent, LabelComponent.class, "account");
                    accountLabel.text(Texts.of("text.subathon.screen.connect.account.disconnected"));

                    FlowLayout extraOptions = screen.extraOptions();

                    ButtonComponent disconnectButton = screen.getComponent(extraOptions, ButtonComponent.class, "disconnect");
                    ButtonComponent reconnectButton = screen.getComponent(extraOptions, ButtonComponent.class, "reconnect");
                    ButtonComponent resetCacheButton = screen.getComponent(extraOptions, ButtonComponent.class, "reset-cache");
                    ButtonComponent resetKeyButton = screen.getComponent(extraOptions, ButtonComponent.class, "reset-key");

                    runtimeRewardTextures.clear();
                    rewards.clear();

                    disconnectButton.active = false;
                    reconnectButton.active = false;
                    resetCacheButton.active = true;
                    resetKeyButton.active = Subathon.CONFIG_DIR.resolve("auth").toFile().exists();

                    connectButton.setMessage(Text.translatable("text.subathon.screen.connect.button.connect"));
                    connectButton.active = true;
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Subathon.id("event_message"), (client, handler, buf, responseSender) -> {
            EventMessages message = buf.readEnumConstant(EventMessages.class);
            EventMessages quickMessage = buf.readEnumConstant(EventMessages.class);
            Map<String, String> placeholders = buf.readMap(PacketByteBuf::readString, PacketByteBuf::readString);
            String content = buf.readString();
            client.execute(() -> {
                Notification notification = new Notification(message, quickMessage, placeholders, content, UUID.randomUUID());
                quickMessages.add(notification);
                messages.add(notification);

                if (CLIENT_CONFIGS.toasts.enabled()) {
                    Identifier icon = spriteId(NotificationsScreen.getType(message, placeholders));

                    switch (message) {
                        case SUB, RESUB -> {
                            if (CLIENT_CONFIGS.toasts.subs.enabled()) {
                                ConfigsClient.Client$Toasts.Client$Subs.Tier tier = getTier(quickMessage);
                                if (tier == null) {
                                    client.getToastManager().add(new TwitchEventToast(spriteId("warning"),
                                            Text.literal("Error!"), Text.literal("Failed to create notification toast!")));
                                    return;
                                }

                                if (CLIENT_CONFIGS.toasts.subs.minimumTier().compareTo(tier) <= 0) {
                                    client.getToastManager().add(new TwitchEventToast(icon,
                                            Texts.of(notification.getToastTitle(), map -> map.putAll(notification.placeholders)),
                                            Texts.of(notification.message().translation, map -> map.putAll(notification.placeholders()))));
                                }
                            }
                        }
                        case GIFT -> {
                            if (!CLIENT_CONFIGS.toasts.gifts.enabled()) break;

                            int amount = Integer.parseInt(placeholders.get("amount"));
                            ConfigsClient.Client$Toasts.Client$Subs.Tier tier = getTier(quickMessage);

                            if ((CLIENT_CONFIGS.toasts.gifts.tier1.enabled() && tier == ConfigsClient.Client$Toasts.Client$Subs.Tier.TIER1 && amount >= CLIENT_CONFIGS.toasts.gifts.tier1.minimum()) ||
                                    (CLIENT_CONFIGS.toasts.gifts.tier2.enabled() && tier == ConfigsClient.Client$Toasts.Client$Subs.Tier.TIER2 && amount >= CLIENT_CONFIGS.toasts.gifts.tier2.minimum()) ||
                                    (CLIENT_CONFIGS.toasts.gifts.tier3.enabled() && tier == ConfigsClient.Client$Toasts.Client$Subs.Tier.TIER3) && amount >= CLIENT_CONFIGS.toasts.gifts.tier3.minimum()) {
                                client.getToastManager().add(new TwitchEventToast(icon,
                                        Texts.of(notification.getToastTitle(), map -> map.putAll(notification.placeholders)),
                                        Texts.of(notification.message().translation, map -> map.putAll(notification.placeholders()))));
                            }
                        }

                        case CHEER -> {
                            int amount = Integer.parseInt(placeholders.get("amount"));
                            if (CLIENT_CONFIGS.toasts.bits.enabled() && amount >= CLIENT_CONFIGS.toasts.bits.minimum()) {
                                client.getToastManager().add(new TwitchEventToast(icon,
                                        Texts.of(notification.getToastTitle(), map -> map.putAll(notification.placeholders)),
                                        Texts.of(notification.message().translation, map -> map.putAll(notification.placeholders()))));
                            }
                        }
                    }
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Subathon.id("next_update"), (client, handler, buf, responseSender) -> {
            int time = buf.readInt();
            client.execute(() -> {
                nextUpdate = time;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Subathon.id("fx/shockwave"), (client, handler, buf, responseSender) -> {
            Vec3d center = new Vec3d(buf.readVector3f());
            float startRadius = buf.readFloat();
            float endRadius = buf.readFloat();
            long endTime = buf.readLong();
            int color = buf.readInt();
            client.execute(() -> shockwaves.add(new Shockwave(center, startRadius, endRadius, client.world.getTime(), endTime, AccurateColor.fromARGB(color))));
        });
    }

    static {
        UIParsing.registerFactory("subathon-text-box", element -> new SubathonTextBox());
    }

    private ConfigsClient.Client$Toasts.Client$Subs.Tier getTier(EventMessages message) {
        return switch (message) {
            case QUICK_SUB_TIER1, QUICK_GIFT_TIER1 -> ConfigsClient.Client$Toasts.Client$Subs.Tier.TIER1;
            case QUICK_SUB_TIER2, QUICK_GIFT_TIER2 -> ConfigsClient.Client$Toasts.Client$Subs.Tier.TIER2;
            case QUICK_SUB_TIER3, QUICK_GIFT_TIER3 -> ConfigsClient.Client$Toasts.Client$Subs.Tier.TIER3;
            default -> null;
        };
    }

    private void addPointsHud() {
        Hud.add(Subathon.id("points_view"), () -> {
            FlowLayout parent = Containers.verticalFlow(Sizing.content(), Sizing.content());
            parent.positioning(Positioning.relative(0, 100));
            PointsComponent pointsComponent = new PointsComponent();
            pointsComponent.scale(CLIENT_CONFIGS.pointsFontScale());
            parent.child(pointsComponent);
            return parent;
        });
    }

    private void removePointsHud() {
        Hud.remove(Subathon.id("points_view"));
    }

    public record Notification(EventMessages message, EventMessages quickMessage, Map<String, String> placeholders, String content, UUID uuid) {
        public String getToastTitle() {
            return quickMessage.translation.replace("quick_notification", "toast");
        }
    }

    public static class QuickMessageTimer {
        private final BaseParentComponent component;
        public float timer = (float) (CLIENT_CONFIGS.quickMessageStayTime() / 20d);

        public QuickMessageTimer(BaseParentComponent component, MinecraftClient client) {
            this.component = component;
            getDuckComponent().subathon$registerUpdateListener((delta, mouseX, mouseY) -> {
                if (timer > 0) {
                    timer -= delta;
                } else if (timer > -20) {
                    LabelComponent label = component.childById(LabelComponent.class, "subathon.notification.label");
                    if (label.positioning().animation() == null) {
                        LabelComponentAccessor labelMixin = (LabelComponentAccessor) label;
                        int hSize = Sizing.content().inflate(0, labelMixin::callDetermineHorizontalContentSize);
                        label.positioning().animate(200, Easing.CUBIC, Positioning.absolute(hSize * 2 + 32, 0)).forwards();
                    }
                    timer--;
                } else {
                    component.parent().removeChild(component);
                    timer = -1300;
                }
            });
        }

        public BaseParentComponent getComponent() {
            return component;
        }

        public ComponentDuck getDuckComponent() {
            return (ComponentDuck) component;
        }
    }

    public static UUID safeUUID(UUID uuid) {
        return uuid == null ? new UUID(0, 0) : uuid;
    }
}
