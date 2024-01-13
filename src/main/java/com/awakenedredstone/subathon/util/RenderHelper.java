package com.awakenedredstone.subathon.util;

import com.mojang.blaze3d.systems.RenderSystem;
import me.x150.renderer.render.Renderer3d;
import me.x150.renderer.util.AlphaOverride;
import me.x150.renderer.util.BufferUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RenderHelper {

    public static void setup3dRender() {
        setup3dRender(false);
    }

    public static void setup3dRender(boolean renderThroughWalls) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(renderThroughWalls ? GL11.GL_ALWAYS : GL11.GL_LEQUAL);
    }

    public static void end3dRender() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static float[] getColor(Color c) {
        return new float[] { c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, transformColor(c.getAlpha() / 255f) };
    }

    public static float transformColor(float f) {
        return AlphaOverride.compute(f);
    }

    public static Vec3d transformVec3d(Vec3d in) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        return in.subtract(camPos);
    }

    public static VertexConsumer vertex(VertexConsumer buffer, Matrix4f matrix, double x, double y, double z) {
        return buffer.vertex(matrix, (float) x, (float) y, (float) z);
    }

    public static void useBuffer(VertexFormat.DrawMode mode, VertexFormat format, Supplier<ShaderProgram> shader, Consumer<BufferBuilder> runner) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder bb = t.getBuffer();

        bb.begin(mode, format);

        runner.accept(bb);

        setup3dRender();
        RenderSystem.setShader(shader);
        BufferUtils.draw(bb);
        setup3dRender();
    }

    public static void genericAABBRender(VertexFormat.DrawMode mode, VertexFormat format, Supplier<ShaderProgram> shader, Matrix4f stack, Vec3d start, Vec3d dimensions, Color color, RenderAction action) {
        float red = color.getRed() / 255f;
        float green = color.getGreen() / 255f;
        float blue = color.getBlue() / 255f;
        float alpha = transformColor(color.getAlpha() / 255f);
        Vec3d vec3d = transformVec3d(start);
        Vec3d end = vec3d.add(dimensions);
        float x1 = (float) vec3d.x;
        float y1 = (float) vec3d.y;
        float z1 = (float) vec3d.z;
        float x2 = (float) end.x;
        float y2 = (float) end.y;
        float z2 = (float) end.z;
        useBuffer(mode, format, shader, bufferBuilder -> action.run(bufferBuilder, x1, y1, z1, x2, y2, z2, red, green, blue, alpha, stack));
    }

    public interface RenderAction {
        void run(BufferBuilder buffer, float x, float y, float z, float x1, float y1, float z1, float red, float green, float blue, float alpha, Matrix4f matrix);
    }
}
