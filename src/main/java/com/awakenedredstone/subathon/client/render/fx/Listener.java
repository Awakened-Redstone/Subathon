package com.awakenedredstone.subathon.client.render.fx;

import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.util.RenderHelper;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.krlite.equator.math.geometry.flat.Box;
import net.krlite.equator.render.renderer.Flat;
import net.krlite.equator.visual.color.AccurateColor;
import net.krlite.equator.visual.color.Palette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public class Listener {

    public static void onWorldRendered(WorldRenderContext context) {
        MatrixStack matrixStack = context.matrixStack();
        matrixStack.push();
        // Quad at (0, 0, 0):
        //Renderer3d.renderFilled(matrixStack, Color.RED, Vec3d.ZERO, new Vec3d(1, 1000, 1));
        // Quad outline at (0, 0, 0):
        //Renderer3d.renderOutline(matrixStack, Color.RED, Vec3d.ZERO, new Vec3d(1, 1, 1));
        //MinecraftClient client = MinecraftClient.getInstance();
        //Camera camera = client.gameRenderer.getCamera();
        //Vec3d vec3d = RenderHelper.transformVec3d(new Vec3d(30, 4, -30));
        //matrixStack.push();
        //matrixStack.multiply(new Quaternionf().rotationYXZ((float) java.lang.Math.PI - (float) java.lang.Math.PI / 180 * camera.getYaw(), 0, 0.0f), (float) vec3d.x, (float) vec3d.y, (float) vec3d.z);
        //GradiantRenderer.readyHorizontal(new Box(vec3d.x - 0.25, vec3d.y, vec3d.x + 0.25, vec3d.y + 2500), AccurateColor.CYAN, AccurateColor.CYAN).render(matrixStack, (float) vec3d.z);
        //GradiantRenderer.readyHorizontal(new Box(vec3d.x + 0.25, vec3d.y, vec3d.x + 1.00, vec3d.y + 2500), AccurateColor.CYAN, AccurateColor.TRANSPARENT).render(matrixStack, (float) vec3d.z);
        //GradiantRenderer.readyHorizontal(new Box(vec3d.x - 0.25, vec3d.y, vec3d.x - 1.00, vec3d.y + 2500), AccurateColor.TRANSPARENT, AccurateColor.CYAN).render(matrixStack, (float) vec3d.z);
        //matrixStack.pop();
        //matrixStack.push();
        //vec3d = RenderHelper.transformVec3d(new Vec3d(0, 100, 0));
        //matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90), (float) vec3d.x, (float) vec3d.y, (float) vec3d.z);

        //new OvalRenderer(new Box(vec3d.x, vec3d.y, vec3d.x + 5, vec3d.y + 5), AccurateColor.TRANSPARENT)
        //        .addColor(10, AccurateColor.fromColor(Color.CYAN))
        //        .addColor(9, AccurateColor.fromColor(Color.CYAN))
        //        .render(matrixStack, (float) vec3d.z);
        //matrixStack.pop();

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        long time = MinecraftClient.getInstance().world.getTime();
        DrawContext drawContext = new DrawContext(MinecraftClient.getInstance(), matrixStack, VertexConsumerProvider.immediate(new BufferBuilder(12)));
        SubathonClient.getInstance().getShockwaves().forEach(shockwave -> shockwave.render(drawContext, time));
        SubathonClient.getInstance().getShockwaves().removeIf(shockwave -> time >= shockwave.endTime());

        //var pos = new Vec3d(0, 80, 0);
        //var radius = 12;
        //Vec3d vec3d2 = RenderHelper.transformVec3d(pos);
        /*matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90), (float) vec3d2.x, (float) vec3d2.y, (float) vec3d2.z);*/

        //Vec3d vec3d = RenderHelper.transformVec3d(pos.subtract(radius, radius, 0));
        //Box box = new Box(vec3d.x, vec3d.y, vec3d.x + radius * 2, vec3d.y + radius * 2);
        /*Flat.Oval oval = new Flat(drawContext, (float) vec3d.getZ() + 0.01f, box).new Oval(Palette.TRANSPARENT)
            .mode(Flat.Oval.OvalMode.GRADIANT_OUT)
            .addColor(0, Palette.CYAN);*/

        //oval.render();
        /*matrixStack.pop();*/

        RenderSystem.disableBlend();
        RenderSystem.enableCull();

        //renderCircle(matrixStack, Color.RED, new Vec3d(0, 100, 0.001), 25, 100);
        matrixStack.pop();
    }

    public static void onHudRendered(DrawContext drawContext) {
        //MatrixStack matrixStack = RendererUtils.getEmptyMatrixStack();
        // Rounded quad at (50, 50 -> 100, 100), 5 px corner, 10 samples
        //Renderer2d.renderRoundedQuad(RendererUtils.getEmptyMatrixStack(), Color.WHITE, 50, 50, 100, 100, 5, 10);
        //Renderer2d.renderCircle(RendererUtils.getEmptyMatrixStack(), Color.WHITE, 75, 75, 25, 100);
        //new OvalRenderer(new Box(50, 50, 100, 100), AccurateColor.RED).render(matrixStack);

        /*var vec3d = new Vec3d(0, 0, 0);
        var radius = 12;
        Box box = new Box(vec3d.x, vec3d.y, vec3d.x + radius * 2, vec3d.y + radius * 2);
        Flat.Oval oval = new Flat(drawContext, 0.1f, box).new Oval(0, 12, Palette.TRANSPARENT)
            .mode(Flat.Oval.OvalMode.GRADIANT_IN)
            .addColor(9, Palette.CYAN)
            .addColor(10, Palette.CYAN);

        oval.render();*/
    }
}
