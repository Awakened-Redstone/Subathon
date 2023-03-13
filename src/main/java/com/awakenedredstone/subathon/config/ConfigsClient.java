package com.awakenedredstone.subathon.config;

import blue.endless.jankson.Comment;
import com.awakenedredstone.subathon.Subathon;
import io.wispforest.owo.config.annotation.*;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.UUID;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
@Config(name = Subathon.MOD_ID + "/client", wrapperName = "ClientConfigs")
public class ConfigsClient {
    @ExcludeFromScreen
    @Comment("The reward ID used, set to null (without \"s) to disable *****DO NOT TOUCH IF YOU DON'T KNOW WHAT YOU ARE DOING*****")
    public UUID rewardId = null;

    @Comment("The connection type, IRC is authless")
    public ConnectionType connectionType = ConnectionType.EVENTSUB;
    @Comment("The username used for the IRC connection type")
    @PredicateConstraint("validTwitchUsername")
    public String twitchUsername = "";

    @Comment("Toggles showing the points on the screen")
    public boolean showValue = true;
    @Comment("Toggles showing the update timer")
    public boolean showTimer = true;

    @Hook
    @Comment("Sets the scale of teh points display on the screen")
    @RangeConstraint(min = 0, max = 5)
    public float pointsFontScale = 1f;

    @PredicateConstraint("aboveZero")
    @Comment("The time for quick message will stay on the screen (in milliseconds)")
    public int quickMessageStayTime = 1000;

    @Nest
    public Client$Toasts toasts = new Client$Toasts();

    public static class Client$Toasts {
        public boolean enabled = true;
        @Nest public Client$Subs subs = new Client$Subs();
        @Nest public Client$Gifts gifts = new Client$Gifts();
        @Nest public Client$Bits bits = new Client$Bits();

        public static class Client$Subs {
            public boolean enabled = true;
            public Tier minimumTier = Tier.TIER2;

            public enum Tier {
                TIER1,
                TIER2,
                TIER3
            }
        }

        public static class Client$Gifts {
            public boolean enabled = true;
            @Nest public Client$Tier1 tier1 = new Client$Tier1();
            @Nest public Client$Tier2 tier2 = new Client$Tier2();
            @Nest public Client$Tier3 tier3 = new Client$Tier3();

            public static class Client$Tier1 {
                public boolean enabled = true;
                @PredicateConstraint("aboveZero")
                public int minimum = 20;

                public static boolean aboveZero(int value) {
                    return value > 0;
                }
            }

            public static class Client$Tier2 {
                public boolean enabled = true;
                @PredicateConstraint("aboveZero")
                public int minimum = 5;

                public static boolean aboveZero(int value) {
                    return value > 0;
                }
            }

            public static class Client$Tier3 {
                public boolean enabled = true;
                @PredicateConstraint("aboveZero")
                public int minimum = 1;

                public static boolean aboveZero(int value) {
                    return value > 0;
                }
            }
        }

        public static class Client$Bits {
            public boolean enabled = true;
            @PredicateConstraint("aboveZero")
            public int minimum = 5000;

            public static boolean aboveZero(int value) {
                return value > 0;
            }
        }
    }

    public static boolean aboveZero(int value) {
        return value > 0;
    }

    public static boolean validTwitchUsername(String username) {
        return Pattern.compile("^[a-zA-Z\\d]\\w{0,24}$").matcher(username).matches();
    }

    public enum ConnectionType {
        EVENTSUB(true),
        IRC(false);

        private final boolean requiresAuth;

        ConnectionType(boolean requiresAuth) {
            this.requiresAuth = requiresAuth;
        }

        public boolean requiresAuth() {
            return requiresAuth;
        }
    }
}
