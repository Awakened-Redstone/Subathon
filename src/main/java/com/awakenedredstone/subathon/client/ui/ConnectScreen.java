package com.awakenedredstone.subathon.client.ui;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.config.ConfigsClient;
import com.awakenedredstone.subathon.owo.SubathonTextBox;
import com.awakenedredstone.subathon.integration.twitch.AuthUtils;
import com.awakenedredstone.subathon.integration.twitch.Twitch;
import com.awakenedredstone.subathon.util.FileUtil;
import com.awakenedredstone.subathon.util.MapBuilder;
import com.awakenedredstone.subathon.util.Texts;
import com.awakenedredstone.subathon.util.Utils;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.parsing.UIModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Asserts;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static com.awakenedredstone.subathon.config.ConfigsClient.ConnectionType.EVENTSUB;

@Environment(EnvType.CLIENT)
public class ConnectScreen extends BaseScreen<FlowLayout> {
    private FlowLayout extraOptions;

    public ConnectScreen() {
        super(FlowLayout.class, "connect_screen");
    }

    @Override
    @SuppressWarnings({"DuplicatedCode"})
    protected void build(FlowLayout rootComponent) {
        Utils.load(ConfigScreen.class);
        Asserts.notNull(client, "client");

        var accountLabel = getComponent(rootComponent, LabelComponent.class, "account");

        if (SubathonClient.getInstance().getInstance().authenticated) {
            if (SubathonClient.getInstance().getInstance().connectionType.requiresAuth()) {
                accountLabel.text(Texts.of("text.subathon.screen.connect.account", new MapBuilder.StringMap()
                    .putAny("user", SubathonClient.getInstance().getInstance().cache.get("accountName"))
                    .build()));
            } else {
                accountLabel.text(Texts.of("text.subathon.screen.connect.account.authless", new MapBuilder.StringMap()
                    .putAny("user", SubathonClient.getInstance().getInstance().cache.get("accountName"))
                    .build()));
            }
        } else {
            accountLabel.text(Texts.of("text.subathon.screen.connect.account.disconnected"));
        }

        //region SET HOVER EFFECT
        FlowLayout items = getComponent(rootComponent, FlowLayout.class, "connect-items");

        for (Component child : items.children()) {
            if (child instanceof FlowLayout component) {
                component.mouseEnter().subscribe(() -> component.surface((context, component1) -> {
                    MatrixStack matrices = context.getMatrices();
                    matrices.push();
                    matrices.translate(0, 0, -0.1);
                    context.drawGradientRect(
                        component1.x(), component1.y(), component1.width(), component1.height(),
                        0xC0101010, 0x00101010, 0x00101010, 0xC0101010
                    );
                    matrices.pop();
                }));

                component.mouseLeave().subscribe(() -> component.surface(Surface.BLANK));
            }
        }
        //endregion

        ButtonComponent connectButton = getComponent(rootComponent, ButtonComponent.class, "connect-button");

        extraOptions = model.expandTemplate(FlowLayout.class, "extra-options", new HashMap<>());

        ButtonComponent extraOptionsButton = getComponent(rootComponent, ButtonComponent.class, "sandwich");
        ButtonComponent disconnectButton = getComponent(extraOptions, ButtonComponent.class, "disconnect");
        ButtonComponent reconnectButton = getComponent(extraOptions, ButtonComponent.class, "reconnect");
        ButtonComponent resetCacheButton = getComponent(extraOptions, ButtonComponent.class, "reset-cache");
        ButtonComponent resetKeyButton = getComponent(extraOptions, ButtonComponent.class, "reset-key");

        reconnectButton.active = false;
        reconnectButton.tooltip(Text.literal("Please use disconnect and connect for now"));

        FlowLayout twitchUsername = model.expandTemplate(FlowLayout.class, "twitch-username", new HashMap<>());
        ButtonComponent confirmButton = getComponent(twitchUsername, ButtonComponent.class, "confirm");
        ButtonComponent cancelButton = getComponent(twitchUsername, ButtonComponent.class, "cancel");
        SubathonTextBox usernameTextBox = createTextBox(model, SubathonTextBox::configureForTwitchUsername).optionProvider();
        usernameTextBox.text(SubathonClient.CLIENT_CONFIGS.twitchUsername());
        twitchUsername.child(0, usernameTextBox);

        confirmButton.onPress(button -> {
            rootComponent.removeChild(twitchUsername);
            String channel = usernameTextBox.getText();
            SubathonClient.CLIENT_CONFIGS.twitchUsername(channel);
            button.setMessage(Text.translatable("text.subathon.screen.connect.button.authenticated"));
            ClientPlayNetworking.send(Subathon.id("channel"), PacketByteBufs.create().writeString(channel));
            SubathonClient.getInstance().connectionType = ConfigsClient.ConnectionType.IRC;
        });

        cancelButton.onPress(button -> {
            rootComponent.removeChild(twitchUsername);
            connectButton.active = true;
            resetKeyButton.active = true;
            resetCacheButton.active = true;
        });

        Consumer<ButtonComponent> connect = button -> {
            button.active = false;
            resetKeyButton.active = false;
            resetCacheButton.active = false;
            if (this.client == null || this.client.player == null) return;

            //region CONNECTION
            if (SubathonClient.getInstance().authenticated) return;

            switch (SubathonClient.CLIENT_CONFIGS.connectionType()) {
                case EVENTSUB -> {
                    Optional<String> cachedKey = FileUtil.readFile(Subathon.CONFIG_DIR.resolve("auth").toFile());

                    if (cachedKey.isEmpty()) {
                        AuthUtils.requestAuth().whenCompleteAsync((authResult, throwable) -> {
                            if (authResult.error() != AuthUtils.AuthResult.ErrorType.NONE || authResult.result().isEmpty()) {
                                if (authResult.error() == AuthUtils.AuthResult.ErrorType.INVALID) return;
                                button.setMessage(Text.translatable("text.subathon.screen.connect.button.failed"));
                                //TODO: finish this
                            } else {
                                String authKey = authResult.result().get();
                                FileUtil.writeFile(Subathon.CONFIG_DIR.resolve("auth").toFile(), authKey);
                                button.setMessage(Text.translatable("text.subathon.screen.connect.button.authenticated"));
                                ClientPlayNetworking.send(Subathon.id("auth_key"), PacketByteBufs.create().writeString(authKey));
                                SubathonClient.getInstance().cache.put("token", authKey);
                                SubathonClient.getInstance().connectionType = EVENTSUB;
                            }
                        });
                    } else {
                        SubathonClient.getInstance().cache.put("token", cachedKey.get());
                        button.setMessage(Text.translatable("text.subathon.screen.connect.button.authenticated"));
                        ClientPlayNetworking.send(Subathon.id("auth_key"), PacketByteBufs.create().writeString(cachedKey.get()));
                        SubathonClient.getInstance().connectionType = EVENTSUB;
                    }
                }
                case IRC -> {
                    String channel = SubathonClient.CLIENT_CONFIGS.twitchUsername();
                    if (StringUtils.isBlank(channel)) {
                        rootComponent.child(twitchUsername);
                    } else {
                        button.setMessage(Text.translatable("text.subathon.screen.connect.button.authenticated"));
                        ClientPlayNetworking.send(Subathon.id("channel"), PacketByteBufs.create().writeString(channel));
                        SubathonClient.getInstance().connectionType = ConfigsClient.ConnectionType.IRC;
                    }
                }
            }

            //endregion

            //region GET REWARDS
            Path cacheDir = Subathon.CONFIG_DIR.resolve("cache");

            Runnable getRewards = () -> SubathonClient.getInstance().rewards.forEach(reward -> {
                var image = reward.image();

                var url = image != null ? image.url4x() : reward.defaultImage().url4x();

                Optional<InputStream> cachedImage = FileUtil.readFileStream(cacheDir.resolve("reward-icons").resolve(reward.id()).toFile());
                if (cachedImage.isPresent()) {
                    try {
                        NativeImage read = NativeImage.read(cachedImage.get());
                        SubathonClient.getInstance().runtimeRewardTextures.registerTexture(Subathon.id("reward/" + reward.id()), new NativeImageBackedTexture(read));
                    } catch (Exception e) {
                        Subathon.LOGGER.error("Failed to load reward texture!", e);
                    }
                    return;
                }

                HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
                Request request = new Request.Builder().url(urlBuilder.build().toString()).build();

                Subathon.OK_HTTP_CLIENT.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        try {
                            ResponseBody body = response.body();
                            assert body != null;
                            byte[] bytes = body.bytes();
                            FileUtil.writeFileBytes(cacheDir.resolve("reward-icons").resolve(reward.id()).toFile(), bytes);
                            NativeImage read = NativeImage.read(new ByteArrayInputStream(bytes));
                            SubathonClient.getInstance().runtimeRewardTextures.registerTexture(Subathon.id("reward/" + reward.id()), new NativeImageBackedTexture(read));
                        } catch (Exception e) {
                            Subathon.LOGGER.error("Failed to load reward texture!", e);
                        }
                    }
                });
            });

            if (SubathonClient.CLIENT_CONFIGS.connectionType().requiresAuth()) {
                if (SubathonClient.getInstance().rewards.isEmpty()) {
                    Twitch.getInstance().getChannelCustomRewards(SubathonClient.getInstance().cache.get("token")).whenCompleteAsync((customRewards, throwable) -> {
                        SubathonClient.getInstance().rewards.addAll(customRewards);
                        new Thread(getRewards).start();
                    });
                } else {
                    SubathonClient.getInstance().runtimeRewardTextures.clear();
                    new Thread(getRewards).start();
                }
            }
            //endregion
        };

        Runnable disconnect = () -> {
            accountLabel.text(Texts.of("text.subathon.screen.connect.account.disconnected"));
            disconnectButton.active = false;
            //reconnectButton.active = false;
            SubathonClient.getInstance().runtimeRewardTextures.clear();
            SubathonClient.getInstance().rewards.clear();
            if (SubathonClient.CLIENT_CONFIGS.rewardId() != null) {
                UUID rewardId = SubathonClient.CLIENT_CONFIGS.rewardId();
                if (SubathonClient.getInstance().cache.get("token") != null)
                    Twitch.getInstance().toggleReward(SubathonClient.getInstance().cache.get("token"), rewardId, false);
                ClientPlayNetworking.send(Subathon.id("reward_id"), PacketByteBufs.create().writeUuid(safeUUID(rewardId)));
            }

            if (SubathonClient.getInstance().connectionType != null) {
                PacketByteBuf buf = PacketByteBufs.create().writeEnumConstant(SubathonClient.getInstance().connectionType);
                if (SubathonClient.getInstance().connectionType.requiresAuth()) {
                    if (SubathonClient.getInstance().cache.get("token") != null) {
                        buf.writeString(SubathonClient.getInstance().cache.get("token"));
                        SubathonClient.getInstance().cache.remove("token");
                    }
                } else {
                    buf.writeString("");
                }
                ClientPlayNetworking.send(Subathon.id("disconnect"), buf);
            }
        };

        var ref = new Object() {
            boolean open = false;
        };
        extraOptionsButton.onPress((ButtonComponent button) -> {
            disconnectButton.active = SubathonClient.getInstance().authenticated;
            //reconnectButton.active = SubathonClient.getInstance().authenticated;

            ref.open = !ref.open;
            if (ref.open) {
                button.setMessage(Text.literal("❌"));
                rootComponent.child(extraOptions);
            } else {
                button.setMessage(Text.literal("≡"));
                rootComponent.removeChild(extraOptions);
            }
        });

        resetKeyButton.onPress((ButtonComponent button) -> {
            disconnect.run();
            FileUtil.delete(Subathon.CONFIG_DIR.resolve("auth"));
            SubathonClient.getInstance().cache.remove("token");
            resetKeyButton.active = false;
        });

        resetCacheButton.onPress((ButtonComponent button) -> {
            disconnect.run();
            FileUtil.delete(Subathon.CONFIG_DIR.resolve("cache"));
            SubathonClient.CLIENT_CONFIGS.rewardId(null);
            SubathonClient.CLIENT_CONFIGS.twitchUsername("");
            SubathonClient.getInstance().runtimeRewardTextures.clear();
            SubathonClient.getInstance().rewards.clear();
            SubathonClient.getInstance().cache.clear();
        });

        /*reconnectButton.onPress((ButtonComponent button) -> {
            disconnectButton.active = false;
            reconnectButton.active = false;
            resetCacheButton.active = false;
            SubathonClient.getInstance().authenticated = false;
            SubathonClient.getInstance().connectionStatus.clear();
            SubathonClient.getInstance().runtimeRewardTextures.clear();
            SubathonClient.getInstance().rewards.clear();
            ClientPlayNetworking.send(Subathon.id("disconnect"), PacketByteBufs.create().writeString(SubathonClient.getInstance().cache.get("token")));
            SubathonClient.getInstance().cache.remove("token");
            connectButton.setMessage(Text.translatable("text.subathon.screen.connect.button.reconnecting"));
            connect.accept(connectButton);
        });*/

        disconnectButton.onPress((ButtonComponent button) -> disconnect.run());

        //region BUTTON TOOLTIP
        {
            List<TooltipComponent> tooltip = client.textRenderer.wrapLines(Text.translatable("text.subathon.screen.connect.button.tooltip"), 200)
                .stream().map(TooltipComponent::of).toList();
            connectButton.tooltip(tooltip);
        }
        //endregion

        SubathonClient.getInstance().connectionStatus.forEach((object, status) -> {
            FlowLayout item = getComponent(items, FlowLayout.class, "connect." + object);
            assert item != null;
            LabelComponent statusCompent = getComponent(item, LabelComponent.class, "status");
            assert statusCompent != null;
            statusCompent.text(Text.translatable("text.subathon.screen.connect." + status.name().toLowerCase()));
        });

        //region UPDATE GUI IF CONNECTED
        if (SubathonClient.getInstance().authenticated) {
            resetCacheButton.active = false;
            resetKeyButton.active = false;
            connectButton.active = false;
            connectButton.setMessage(Text.translatable("text.subathon.screen.connect.button.connected"));
        }
        //endregion

        //region RESUBS TOOLTIP
        {
            FlowLayout flowLayout = getComponent(rootComponent, FlowLayout.class, "connect.resubs");
            assert flowLayout != null;
            assert client != null;
            List<TooltipComponent> tooltip = client.textRenderer.wrapLines(Text.translatable("text.subathon.screen.connect.resubs.tooltip"), 200)
                .stream().map(TooltipComponent::of).toList();
            flowLayout.tooltip(tooltip);
        }
        //endregion

        connectButton.onPress(connect);
    }

    @Override
    public void close() {
        super.close();
    }

    public FlowLayout extraOptions() {
        return extraOptions;
    }

    public FlowLayout getExtraOptions() {
        return extraOptions;
    }

    public UUID safeUUID(UUID uuid) {
        return uuid == null ? new UUID(0, 0) : uuid;
    }

    public OptionComponentFactory.Result<FlowLayout, SubathonTextBox> createTextBox(UIModel model, Consumer<SubathonTextBox> processor) {
        var optionComponent = model.expandTemplate(FlowLayout.class, "text-box-config-option",
            new MapBuilder.StringMap()
                .put("config-option-name", "text.config.subathon/client.option.twitchUsername")
                .putAny("config-option-value", SubathonClient.CLIENT_CONFIGS.twitchUsername())
                .build());

        var valueBox = getComponent(optionComponent, SubathonTextBox.class, "value-box");

        processor.accept(valueBox);

        optionComponent.removeChild(valueBox);

        return new OptionComponentFactory.Result<>(optionComponent, valueBox);
    }
}
