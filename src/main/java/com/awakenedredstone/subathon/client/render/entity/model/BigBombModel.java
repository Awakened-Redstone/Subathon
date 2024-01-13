package com.awakenedredstone.subathon.client.render.entity.model;

import com.awakenedredstone.subathon.entity.BigBombEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class BigBombModel extends EntityModel<BigBombEntity> {
    private final ModelPart base;

    public BigBombModel(ModelPart modelPart) {
        this.base = modelPart.getChild("base");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        ModelPartData base = modelPartData.addChild("base", ModelPartBuilder.create()
            .uv(0, 12).cuboid(-4.0F, -0.5F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F))
            .uv(0, 0).cuboid(-3.0F, -6.5F, -3.0F, 6.0F, 6.0F, 6.0F, new Dilation(0.0F)),
            ModelTransform.pivot(0.0F, 14.5F, 0.0F));

        ModelPartData top = base.addChild("top", ModelPartBuilder.create()
                .uv(18, 0).cuboid(-1.5F, -8.25F, -1.5F, 3.0F, 0.25F, 3.0F, new Dilation(0.0F)),
            ModelTransform.pivot(0.0F, 1.5F, 0.0F));

        ModelPartData group = top.addChild("group", ModelPartBuilder.create()
            .uv(0, 0).cuboid(-0.075F, -5.9375F, 1.5F, 0.25F, 0.25F, 0.25F, new Dilation(0.0F))
            .uv(0, 0).cuboid(-0.075F, -5.9375F, -1.75F, 0.25F, 0.25F, 0.25F, new Dilation(0.0F))
            .uv(0, 0).cuboid(-1.225F, -5.9375F, 1.5F, 0.25F, 0.25F, 0.25F, new Dilation(0.0F))
            .uv(0, 0).cuboid(-1.225F, -5.9375F, -1.75F, 0.25F, 0.25F, 0.25F, new Dilation(0.0F))
            .uv(0, 0).cuboid(1.025F, -5.9375F, 1.5F, 0.25F, 0.25F, 0.25F, new Dilation(0.0F))
            .uv(0, 0).cuboid(1.025F, -5.9375F, -1.75F, 0.25F, 0.25F, 0.25F, new Dilation(0.0F)),
            ModelTransform.pivot(-0.025F, -2.3125F, 0.0F));

        ModelPartData wings = base.addChild("wings", ModelPartBuilder.create()
            .uv(0, 28).cuboid(-16.0F, -9.0F, 7.5F, 16.0F, 9.0F, 1.0F, new Dilation(0.0F)),
            ModelTransform.pivot(8.0F, 9.5F, -8.0F));

        ModelPartData cube = wings.addChild("cube", ModelPartBuilder.create()
            .uv(0, 28).cuboid(-7.975F, 1.3125F, -0.5F, 16.0F, 9.0F, 1.0F, new Dilation(0.0F)),
            ModelTransform.of(-8.025F, -10.3125F, 8.0F, 0.0F, 1.5708F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(BigBombEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {

    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        this.base.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}