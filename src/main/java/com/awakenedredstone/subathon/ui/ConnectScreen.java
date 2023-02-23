package com.awakenedredstone.subathon.ui;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.twitch.AuthUtils;
import com.awakenedredstone.subathon.twitch.Twitch;
import com.awakenedredstone.subathon.util.FileUtil;
import com.awakenedredstone.subathon.util.MapBuilder;
import com.awakenedredstone.subathon.util.Texts;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.util.Drawer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import okhttp3.*;
import org.apache.http.util.Asserts;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ConnectScreen extends BaseScreen<FlowLayout> {
    private FlowLayout extraOptions;

    public ConnectScreen() {
        super(FlowLayout.class, "connect_screen");
    }

    @Override
    @SuppressWarnings({"SpellCheckingInspection", "DuplicatedCode"})
    protected void build(FlowLayout rootComponent) {
        Asserts.notNull(client, "client");

        var accountLabel = getComponent(rootComponent, LabelComponent.class, "account");

        if (SubathonClient.authenticated) {
            accountLabel.text(Texts.of("text.subathon.screen.connect.account", new MapBuilder.StringMap()
                    .putAny("%user%", SubathonClient.cache.get("accountName"))
                    .build()));
        } else {
            accountLabel.text(Texts.of("text.subathon.screen.connect.account.disconnected"));
        }

        //region SET HOVER EFFECT
        FlowLayout items = getComponent(rootComponent, FlowLayout.class, "connect-items");

        for (Component child : items.children()) {
            if (child instanceof FlowLayout component) {
                component.mouseEnter().subscribe(() -> component.surface((matrices, component1) -> Drawer.drawGradientRect(matrices,
                        component1.x(), component1.y(), component1.width(), component1.height(),
                        0xC0101010, 0x00101010, 0x00101010, 0xC0101010
                )));

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

        Consumer<ButtonComponent> connect = button -> {
            button.active = false;
            resetCacheButton.active = false;
            resetKeyButton.active = false;
            if (this.client == null || this.client.player == null) return;

            //region AUTHENTICATION
            if (SubathonClient.authenticated) return;

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
                        SubathonClient.cache.put("token", authKey);
                    }
                });
            } else {
                SubathonClient.cache.put("token", cachedKey.get());
                ClientPlayNetworking.send(Subathon.id("auth_key"), PacketByteBufs.create().writeString(cachedKey.get()));
                button.setMessage(Text.translatable("text.subathon.screen.connect.button.authenticated"));
            }
            //endregion

            //region GET REWARDS
            Path cacheDir = Subathon.CONFIG_DIR.resolve("cache");

            Runnable getRewards = () -> SubathonClient.rewards.forEach(reward -> {
                var image = reward.image();

                var url = image != null ? image.url4x() : reward.defaultImage().url4x();

                Optional<InputStream> cachedImage = FileUtil.readFileStream(cacheDir.resolve("reward-icons").resolve(reward.id()).toFile());
                if (cachedImage.isPresent()) {
                    try {
                        NativeImage read = NativeImage.read(cachedImage.get());
                        SubathonClient.runtimeRewardTextures.registerTexture(Subathon.id("reward/" + reward.id()), new NativeImageBackedTexture(read));
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
                            SubathonClient.runtimeRewardTextures.registerTexture(Subathon.id("reward/" + reward.id()), new NativeImageBackedTexture(read));
                        } catch (Exception e) {
                            Subathon.LOGGER.error("Failed to load reward texture!", e);
                        }
                    }
                });
            });

            if (SubathonClient.rewards.isEmpty()) {
                Twitch.getChannelCustomRewards(SubathonClient.cache.get("token")).whenCompleteAsync((customRewards, throwable) -> {
                    SubathonClient.rewards.addAll(customRewards);
                    new Thread(getRewards).start();
                });
            } else {
                SubathonClient.runtimeRewardTextures.clear();
                new Thread(getRewards).start();
            }
            //endregion
        };

        Runnable disconnect = () -> {
            accountLabel.text(Texts.of("text.subathon.screen.connect.account.disconnected"));
            disconnectButton.active = false;
            //reconnectButton.active = false;
            resetCacheButton.active = true;
            resetKeyButton.active = Subathon.CONFIG_DIR.resolve("auth").toFile().exists();
            SubathonClient.authenticated = false;
            SubathonClient.runtimeRewardTextures.clear();
            SubathonClient.rewards.clear();
            if (SubathonClient.CLIENT_CONFIGS.rewardId() != null) {
                UUID rewardId = SubathonClient.CLIENT_CONFIGS.rewardId();
                if (SubathonClient.cache.get("token") != null) Twitch.toggleReward(SubathonClient.cache.get("token"), rewardId, false);
                ClientPlayNetworking.send(Subathon.id("reward_id"), PacketByteBufs.create().writeUuid(safeUUID(rewardId)));
            }

            if (SubathonClient.cache.get("token") != null) {
                ClientPlayNetworking.send(Subathon.id("disconnect"), PacketByteBufs.create().writeString(SubathonClient.cache.get("token")));
                SubathonClient.cache.remove("token");
            }
            connectButton.active = true;
        };

        var ref = new Object() {
            boolean open = false;
        };
        extraOptionsButton.onPress((ButtonComponent button) -> {
            disconnectButton.active = SubathonClient.authenticated;
            //reconnectButton.active = SubathonClient.authenticated;

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
            SubathonClient.cache.remove("token");
            resetKeyButton.active = false;
        });

        resetCacheButton.onPress((ButtonComponent button) -> {
            disconnect.run();
            FileUtil.delete(Subathon.CONFIG_DIR.resolve("cache"));
            SubathonClient.CLIENT_CONFIGS.rewardId(null);
            SubathonClient.runtimeRewardTextures.clear();
            SubathonClient.rewards.clear();
            SubathonClient.cache.clear();
        });

        /*reconnectButton.onPress((ButtonComponent button) -> {
            disconnectButton.active = false;
            reconnectButton.active = false;
            resetCacheButton.active = false;
            SubathonClient.authenticated = false;
            SubathonClient.connectionStatus.clear();
            SubathonClient.runtimeRewardTextures.clear();
            SubathonClient.rewards.clear();
            ClientPlayNetworking.send(Subathon.id("disconnect"), PacketByteBufs.create().writeString(SubathonClient.cache.get("token")));
            SubathonClient.cache.remove("token");
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

        //region UPDATE GUI IF CONNECTED
        if (SubathonClient.authenticated) {
            resetCacheButton.active = false;
            resetKeyButton.active = false;
            connectButton.active = false;
            connectButton.setMessage(Text.translatable("text.subathon.screen.connect.button.connected"));

            SubathonClient.connectionStatus.forEach((object, status) -> {
                FlowLayout item = getComponent(items, FlowLayout.class, "connect." + object);
                assert item != null;
                LabelComponent statusCompent = getComponent(item, LabelComponent.class, "status");
                assert statusCompent != null;
                statusCompent.text(Text.translatable("text.subathon.screen.connect." + status.name().toLowerCase()));
            });
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

    public FlowLayout rootComponent() {
        return uiAdapter.rootComponent;
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
}
