package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.Subathon;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {
    @Shadow @Final private GameProfile profile;
    @Shadow @Final private Map<Type, Identifier> textures;
    @Shadow private boolean texturesLoaded;

    @Inject(method = "loadTextures", at = @At("HEAD"))
    private void subathon$addCustomCapes(CallbackInfo ci) {
        if (profile.getId().toString() != null) {
            if (!texturesLoaded) {
                registerCapes();
            }
            Identifier cape = getCape(profile);
            if (cape == null) return;
            this.textures.put(Type.CAPE, cape);
        }
    }

    private void registerCapes() {
        Optional<Path> capesPath = FabricLoader.getInstance().getModContainer(Subathon.MOD_ID).get().findPath("data/subathon/capes/players.json");
        if (capesPath.isPresent()) {
            try (BufferedReader reader = Files.newBufferedReader(capesPath.get())) {
                JsonElement json = JsonParser.parseReader(new JsonReader(reader));
                if (json == null) return;
                JsonElement capeId = json.getAsJsonObject().get(profile.getId().toString());
                if (capeId != null) {
                    Optional<Path> path = FabricLoader.getInstance().getModContainer(Subathon.MOD_ID).get().findPath(String.format("assets/subathon/textures/cape/%s.png", capeId.getAsString()));
                    if (path.isPresent()) {
                        try (InputStream stream = Files.newInputStream(path.get())) {
                            MinecraftClient.getInstance().getTextureManager().registerTexture(new Identifier(Subathon.MOD_ID, String.format("%s", capeId.getAsString())), new NativeImageBackedTexture(NativeImage.read(stream)));
                        }
                    }
                }
            } catch (IOException ignored) {}
        }
    }

    private Identifier getCape(GameProfile profile) {
        Optional<Path> capesPath = FabricLoader.getInstance().getModContainer(Subathon.MOD_ID).get().findPath("data/subathon/capes/players.json");
        if (capesPath.isPresent()) {
            try (BufferedReader reader = Files.newBufferedReader(capesPath.get())) {
                JsonElement json = JsonParser.parseReader(new JsonReader(reader));
                if (json == null) return null;
                JsonElement capeId = json.getAsJsonObject().get(profile.getId().toString());
                if (capeId != null) {
                    return new Identifier(Subathon.MOD_ID, String.format("%s", capeId.getAsString()));
                }
            } catch (IOException ignored) {}
        }
        return null;
    }

}
