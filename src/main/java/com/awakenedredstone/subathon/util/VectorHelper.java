package com.awakenedredstone.subathon.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class VectorHelper {
    public static Vec3d getDirectionNormalized(Vec3d destination, Vec3d origin) {
        return new Vec3d(destination.x - origin.x, destination.y - origin.y, destination.z - origin.z).normalize();
    }

    public static Vec3d getVectorFromPos(BlockPos pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3d add(Vec3d a, Vec3d b) {
        return new Vec3d(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vec3d subtract(Vec3d a, Vec3d b) {
        return new Vec3d(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static Vec3d multiply(Vec3d velocity, float speed) {
        return new Vec3d(velocity.x * (double) speed, velocity.y * (double) speed, velocity.z * (double) speed);
    }

    public static Vec3d getMovementVelocity(Vec3d current, Vec3d target, float speed) {
        return VectorHelper.multiply(VectorHelper.getDirectionNormalized(target, current), speed);
    }

    public static Vec2f normalize(Vec2f v) {
        float length = (float) Math.sqrt(v.x * v.x + v.y * v.y);
        return new Vec2f(v.x / length, v.y / length);
    }

    public static Vec2f rotateDegrees(Vec2f v, float angleDeg) {
        float angle = (float) Math.toRadians(angleDeg);
        float cosAngle = MathHelper.cos(angle);
        float sinAngle = MathHelper.sin(angle);
        return new Vec2f(v.x * cosAngle - v.y * sinAngle, v.x * sinAngle + v.y * cosAngle);
    }
}

