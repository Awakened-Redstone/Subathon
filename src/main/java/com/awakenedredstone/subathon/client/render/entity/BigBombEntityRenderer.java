package com.awakenedredstone.subathon.client.render.entity;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.client.render.entity.model.BigBombModel;
import com.awakenedredstone.subathon.entity.BigBombEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(value=EnvType.CLIENT)
public class BigBombEntityRenderer extends EntityRenderer<BigBombEntity> {
    public static final Identifier TEXTURE = Subathon.id("textures/entity/bomb/big.png");
    protected BigBombModel model;

    public BigBombEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new BigBombModel(context.getPart(SubathonClient.BIG_BOMB_MODEL_LAYER));
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(BigBombEntity entity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        matrixStack.push();
        matrixStack.scale(7, 7, 7);
        matrixStack.translate(0, -0.5, 0);
        model.render(matrixStack, vertexConsumerProvider.getBuffer(model.getLayer(TEXTURE)), i, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
        matrixStack.pop();
        super.render(entity, f, g, matrixStack, vertexConsumerProvider, i);
    }

    @Override
    public Identifier getTexture(BigBombEntity entity) {
        return TEXTURE;
    }
}
