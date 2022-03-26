package com.awakenedredstone.subathon.connection;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe, configurable helper for applying the exponential backoff algorithm with optional jitter and/or truncation.
 */
public class ExponentialBackoffStrategy {
    /**
     * The maximum backoff value (on average), in milliseconds.
     * If set to a negative value, the algorithm will not be of the truncated variety.
     */
    private long maximumBackoff = Duration.ofMinutes(2).toMillis();

    /**
     * The multiplier on back-offs that is in the base of the exponent.
     * <p>
     * The default is 2, which results in doubling of average delays with additional failures.
     * This generally should be set to a value greater than 1 so that delays tend to increase with more failures.
     */
    private double multiplier = 2.0;

    /**
     * Whether the first attempt after a failure should take place without delay.
     * <p>
     * To avoid a "stampeding herd" of reconnecting clients, {@link ExponentialBackoffStrategyBuilder#jitter(boolean)} can be enabled
     * and {@link ExponentialBackoffStrategyBuilder#initialJitterRange(long)} can optionally be configured.
     */
    private boolean immediateFirst = true;

    /**
     * Whether (pseudo-)randomness should be applied when computing the exponential backoff.
     * <p>
     * Highly useful for avoiding the <a href="https://en.wikipedia.org/wiki/Thundering_herd_problem">thundering herd problem</a>.
     */
    private boolean jitter = true;

    /**
     * The range of initial jitter amounts (in milliseconds) for when both {@link #isImmediateFirst()} and {@link #isJitter()} are true.
     */
    private long initialJitterRange = Duration.ofSeconds(5).toMillis();

    /**
     * The milliseconds value for the first non-zero backoff.
     * When {@link #isJitter()} is true, this becomes an average targeted value rather than a strictly enforced constant.
     */
    private long baseMillis = Duration.ofSeconds(1).toMillis();

    /**
     * The maximum number of retries that should be allowed.
     * <p>
     * A negative value corresponds to no limit.
     * A zero value corresponds to no retries allowed.
     * A positive value enforces a specific maximum.
     */
    private int maxRetries = -1;

    /**
     * The number of consecutive failures that have occurred.
     */
    private final AtomicInteger failures = new AtomicInteger();

    public boolean sleep() {
        long millis = this.get();
        if (millis < 0L) {
            return false;
        } else {
            if (millis > 0L) {
                try {
                    Thread.sleep(millis);
                } catch (Exception var4) {
                    Thread.currentThread().interrupt();
                }
            }

            return true;
        }
    }

    public long get() {
        int f = this.failures.getAndIncrement();
        if (this.maxRetries >= 0 && f >= this.maxRetries) {
            return -1L;
        } else {
            if (this.immediateFirst) {
                if (f == 0) {
                    if (this.jitter) {
                        return ThreadLocalRandom.current().nextLong(this.initialJitterRange);
                    }

                    return 0L;
                }

                --f;
            }

            double delay = Math.pow(this.multiplier, (double)f) * (double)this.baseMillis;
            if (this.maximumBackoff >= 0L) {
                delay = Math.min(delay, (double)this.maximumBackoff);
            }

            if (this.jitter && delay != 0.0D) {
                delay *= 2.0D;
                delay *= ThreadLocalRandom.current().nextDouble();
            }

            return Math.round(delay);
        }
    }

    public void reset() {
        this.setFailures(0);
    }

    public void setFailures(int failures) {
        this.failures.set(failures);
    }

    public int getFailures() {
        return this.failures.get();
    }

    public ExponentialBackoffStrategy copy() {
        return this.toBuilder().build();
    }

    ExponentialBackoffStrategy(long maximumBackoff, double multiplier, boolean immediateFirst, boolean jitter, long initialJitterRange, long baseMillis, int maxRetries) {
        this.maximumBackoff = maximumBackoff;
        this.multiplier = multiplier;
        this.immediateFirst = immediateFirst;
        this.jitter = jitter;
        this.initialJitterRange = initialJitterRange;
        this.baseMillis = baseMillis;
        this.maxRetries = maxRetries;
    }

    public static ExponentialBackoffStrategyBuilder builder() {
        return new ExponentialBackoffStrategy.ExponentialBackoffStrategyBuilder();
    }

    public ExponentialBackoffStrategyBuilder toBuilder() {
        return (new ExponentialBackoffStrategy.ExponentialBackoffStrategyBuilder()).maximumBackoff(this.maximumBackoff).multiplier(this.multiplier).immediateFirst(this.immediateFirst).jitter(this.jitter).initialJitterRange(this.initialJitterRange).baseMillis(this.baseMillis).maxRetries(this.maxRetries);
    }

    public long getMaximumBackoff() {
        return this.maximumBackoff;
    }

    public double getMultiplier() {
        return this.multiplier;
    }

    public boolean isImmediateFirst() {
        return this.immediateFirst;
    }

    public boolean isJitter() {
        return this.jitter;
    }

    public long getInitialJitterRange() {
        return this.initialJitterRange;
    }

    public long getBaseMillis() {
        return this.baseMillis;
    }

    public int getMaxRetries() {
        return this.maxRetries;
    }

    public String toString() {
        return "ExponentialBackoffStrategy(maximumBackoff=" + this.getMaximumBackoff() + ", multiplier=" + this.getMultiplier() + ", immediateFirst=" + this.isImmediateFirst() + ", jitter=" + this.isJitter() + ", initialJitterRange=" + this.getInitialJitterRange() + ", baseMillis=" + this.getBaseMillis() + ", maxRetries=" + this.getMaxRetries() + ", failures=" + this.getFailures() + ")";
    }

    public static class ExponentialBackoffStrategyBuilder {
        private long maximumBackoff = Duration.ofMinutes(2).toMillis();
        private double multiplier = 2.0;
        private boolean immediateFirst = true;
        private boolean jitter = true;
        private long initialJitterRange = Duration.ofSeconds(5).toMillis();
        private long baseMillis = Duration.ofSeconds(1).toMillis();
        private int maxRetries = -1;

        public ExponentialBackoffStrategy.ExponentialBackoffStrategyBuilder maximumBackoff(long maximumBackoff) {
            this.maximumBackoff = maximumBackoff;
            return this;
        }

        public ExponentialBackoffStrategy.ExponentialBackoffStrategyBuilder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public ExponentialBackoffStrategy.ExponentialBackoffStrategyBuilder immediateFirst(boolean immediateFirst) {
            this.immediateFirst = immediateFirst;
            return this;
        }

        public ExponentialBackoffStrategy.ExponentialBackoffStrategyBuilder jitter(boolean jitter) {
            this.jitter = jitter;
            return this;
        }

        public ExponentialBackoffStrategy.ExponentialBackoffStrategyBuilder initialJitterRange(long initialJitterRange) {
            this.initialJitterRange = initialJitterRange;
            return this;
        }

        public ExponentialBackoffStrategy.ExponentialBackoffStrategyBuilder baseMillis(long baseMillis) {
            this.baseMillis = baseMillis;
            return this;
        }

        public ExponentialBackoffStrategy.ExponentialBackoffStrategyBuilder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public ExponentialBackoffStrategy build() {
            return new ExponentialBackoffStrategy(maximumBackoff, multiplier, immediateFirst, jitter, initialJitterRange, baseMillis, maxRetries);
        }

        public String toString() {
            return "ExponentialBackoffStrategy.ExponentialBackoffStrategyBuilder(maximumBackoff=" + this.maximumBackoff + ", multiplier=" + this.multiplier + ", immediateFirst=" + this.immediateFirst + ", jitter=" + this.jitter + ", initialJitterRange=" + this.initialJitterRange + ", baseMillis=" + this.baseMillis + ", maxRetries=" + this.maxRetries + ")";
        }
    }
}
