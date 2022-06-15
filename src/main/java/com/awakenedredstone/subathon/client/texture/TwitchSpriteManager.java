package com.awakenedredstone.subathon.client.texture;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.SubathonClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasHolder;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class TwitchSpriteManager extends SpriteAtlasHolder {
    public TwitchSpriteManager(TextureManager textureManager) {
        super(textureManager, new Identifier(Subathon.MOD_ID, "textures/atlas/twitch.png"), "twitch");
    }

    @Override
    protected Stream<Identifier> getSprites() {
        return SubathonClient.TWITCH_SPRITES.stream();
    }

    public Sprite getSprite(Identifier identifier) {
        return super.getSprite(identifier);
    }
}
