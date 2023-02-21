package com.awakenedredstone.subathon.client;

import com.google.common.collect.ImmutableMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class RuntimeRewardTextures {
    private final Map<Identifier, AbstractTexture> textures = new HashMap<>();

    public void registerTexture(@NotNull Identifier id, @NotNull AbstractTexture texture) {
        if (textures.containsKey(id)) return;
        this.textures.putIfAbsent(id, texture);
    }

    public @Nullable AbstractTexture getTexture(Identifier id) {
        return getTextures().get(id);
    }

    public Map<Identifier, AbstractTexture> getTextures() {
        return ImmutableMap.copyOf(textures);
    }

    public void clear() {
        textures.clear();
    }
}
