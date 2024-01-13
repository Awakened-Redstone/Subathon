package com.awakenedredstone.subathon.client.render.fx;

import com.awakenedredstone.subathon.registry.SoundRegistry;
import com.awakenedredstone.subathon.util.RenderHelper;
import net.krlite.equator.math.geometry.flat.Box;
import net.krlite.equator.render.renderer.Flat;
import net.krlite.equator.visual.color.AccurateColor;
import net.krlite.equator.visual.color.Palette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class Shockwave {
    private final Vec3d pos;
    private final float startRadius;
    private final float endRadius;
    private final long startTime;
    private final long endTime;
    private final AccurateColor color;
    private boolean firstRender = true;

    public Shockwave(Vec3d pos, float startRadius, float endRadius, long startTime, long endTime, AccurateColor color) {
        this.pos = pos;
        this.startRadius = startRadius;
        this.endRadius = endRadius;
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = color;
    }

    public AccurateColor getColorCopy(long time) {
        return new AccurateColor(color.red(), color.green(), color.blue(), getOpacity(time));
    }

    public float getProgress(long time) {
        return (time - startTime) / (float) (endTime - startTime);
    }

    public float getRadius(long time) {
        return getRadius(getProgress(time));
    }

    public float getRadius(float progress) {
        return startRadius + (endRadius - startRadius) * progress;
    }

    public double getOpacity(long time) {
        return color.opacity() * (1 - Math.min(1, (getProgress(time) - 0.66) * 3));
    }

    public void render(DrawContext context, long time) {
        MatrixStack matrixStack = context.getMatrices();
        if (firstRender) {
            MinecraftClient.getInstance().world.playSound(pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 100, 1, false);
        }
        firstRender = false;
        Vec3d vec3d = RenderHelper.transformVec3d(pos);
        matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90), (float) vec3d.x, (float) vec3d.y, (float) vec3d.z);
        createRenderer(context, (float) vec3d.getZ(), time).render();/*.render(matrixStack, (float) vec3d.z - 0.1f)*/;
        matrixStack.pop();
    }

    public Flat.Oval createRenderer(DrawContext context, float z, long time) {
        Vec3d vec3d = RenderHelper.transformVec3d(pos.subtract(getRadius(time), getRadius(time), 0));
        /*new Box(vec3d.x, vec3d.y, vec3d.x + getRadius(time) * 2, vec3d.y + getRadius(time) * 2)*/
        Box box = new Box(vec3d.x, vec3d.y, vec3d.x + getRadius(time) * 2, vec3d.y + getRadius(time) * 2);
        return new Flat(context, z + 0.1f, box).new Oval(Palette.TRANSPARENT)
            .mode(Flat.Oval.OvalMode.GRADIANT_OUT)
            .addColor(0, getColorCopy(time));
    }

    public Vec3d pos() {
        return pos;
    }

    public float startRadius() {
        return startRadius;
    }

    public float endRadius() {
        return endRadius;
    }

    public long startTime() {
        return startTime;
    }

    public long endTime() {
        return endTime;
    }

    public AccurateColor color() {
        return color;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Shockwave) obj;
        return Objects.equals(this.pos, that.pos) &&
            Float.floatToIntBits(this.startRadius) == Float.floatToIntBits(that.startRadius) &&
            Float.floatToIntBits(this.endRadius) == Float.floatToIntBits(that.endRadius) &&
            this.startTime == that.startTime &&
            this.endTime == that.endTime &&
            Objects.equals(this.color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, startRadius, endRadius, startTime, endTime, color);
    }

    @Override
    public String toString() {
        return "Shockwave[" +
            "pos=" + pos + ", " +
            "startRadius=" + startRadius + ", " +
            "endRadius=" + endRadius + ", " +
            "startTime=" + startTime + ", " +
            "endTime=" + endTime + ", " +
            "color=" + color + ']';
    }

}
