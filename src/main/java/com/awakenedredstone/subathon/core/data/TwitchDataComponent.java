package com.awakenedredstone.subathon.core.data;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.DataManager;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public abstract class TwitchDataComponent<T> {
    public final T target;
    protected final ComponentKey<?> key;
    protected long points = 0;
    protected long pointStorage = 0;
    protected long bitStorage = 0;
    protected long subPointsStorage = 0;
    protected long redemptionPoints = 0;

    public TwitchDataComponent(T target, ComponentKey<?> key) {
        this.target = target;
        this.key = key;
    }

    //Code optimized by ChatGPT
    public void addPoints(long amount) {
        pointStorage += amount;

        int threshold = Subathon.COMMON_CONFIGS.threshold();

        if (threshold <= 0) {
            throw new IllegalStateException("Threshold must not be equals or lower than 0!");
        }

        long toAdd = pointStorage / threshold;

        if (target instanceof World world) {
            for (int i = 0; i < toAdd; i++) {
                DataManager.getActiveEffects().forEach(effect -> effect.trigger(world));
            }
        } else if (target instanceof PlayerEntity player) {
            for (int i = 0; i < toAdd; i++) {
                DataManager.getActiveEffects().forEach(effect -> effect.trigger(player));
            }
        }

        points += toAdd;
        pointStorage %= threshold;
        key.sync(target);
    }

    public void setPoints(int amount) {
        points = amount;
        key.sync(target);
    }

    public long getPoints() {
        return points;
    }

    public void setPointStorage(long amount) {
        pointStorage = amount;
        key.sync(target);
    }

    public long getPointStorage() {
        return pointStorage;
    }

    //Code optimized by ChatGPT
    public void addBits(long amount) {
        bitStorage += amount;

        int minimum = Subathon.COMMON_CONFIGS.bits.minimum();

        if (minimum <= 0) {
            throw new IllegalStateException("Minimum must not be equals or lower than 0!");
        }

        addPoints((bitStorage / minimum) * Subathon.COMMON_CONFIGS.subs.points());
        bitStorage %= minimum;
    }

    public void setBitStorage(int amount) {
        bitStorage = amount;
        key.sync(target);
    }

    public long getBitStorage() {
        return bitStorage;
    }

    //Code optimized by ChatGPT
    public void addSubPoints(int amount) {
        subPointsStorage += amount;

        int threshold = Subathon.COMMON_CONFIGS.subs.threshold();

        if (threshold <= 0) {
            throw new IllegalStateException("Threshold must not be equals or lower than 0!");
        }

        addPoints((subPointsStorage / threshold) * Subathon.COMMON_CONFIGS.subs.points());
        subPointsStorage %= threshold;
    }

    public void setSubPoints(int amount) {
        subPointsStorage = amount;
        key.sync(target);
    }

    public long getSubPointsStorage() {
        return subPointsStorage;
    }

    //Code optimized by ChatGPT
    public void addRewardPoints(int amount) {
        redemptionPoints += amount;

        int threshold = Subathon.COMMON_CONFIGS.rewards.threshold();

        if (threshold <= 0) {
            throw new IllegalStateException("Threshold must not be equals or lower than 0!");
        }

        addPoints((redemptionPoints / threshold) * Subathon.COMMON_CONFIGS.rewards.points());
        redemptionPoints %= threshold;
    }

    public void setRedemptionPoints(int amount) {
        redemptionPoints = amount;
        key.sync(target);
    }

    public long getRedemptionPoints() {
        return redemptionPoints;
    }
}
