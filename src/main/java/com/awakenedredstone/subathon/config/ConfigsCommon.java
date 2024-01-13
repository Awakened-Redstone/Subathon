package com.awakenedredstone.subathon.config;

import blue.endless.jankson.Comment;
import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.Effect;
import com.awakenedredstone.subathon.registry.SubathonRegistries;
import com.awakenedredstone.subathon.util.Utils;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

@SuppressWarnings("unused")
@Sync(Option.SyncMode.OVERRIDE_CLIENT)
@Config(name = Subathon.MOD_ID + "/common", wrapperName = "CommonConfigs")
public class ConfigsCommon {

    @ExcludeFromScreen
    @Comment("Developer configuration, enable if you are using a local mock Twitch API (hosted on 0.0.0.0:3000)")
    public boolean useMockApi = false;

    @ExcludeFromScreen
    @Comment("Effects configurations, some parts of this is sensitive!")
    public Map<Identifier, Effect> effects = new HashMap<>();

    @ExcludeFromScreen
    @PredicateConstraint("triesRange")
    @Comment("Defines how many times the chaos effect can try to trigger a chaos before failing")
    public byte chaosTries = 20;

    @ExcludeFromScreen
    @PredicateConstraint("mobsConstrain")
    @Comment("Mobs that are not spawned excluded from the chaos effect")
    //TODO: tags
    public List<String> excludedEntities = new ArrayList<>(Arrays.asList("minecraft:player", "minecraft:egg", "minecraft:item",
            "minecraft:item_frame", "minecraft:paiting", "minecraft:fishing_bobber", "minecraft:experience_bottle",
            "minecraft:ender_pearl", "minecraft:spectral_arrow", "minecraft:snowball", "minecraft:small_fireball",
            "minecraft:shulker_bullet", "minecraft:marker", "minecraft:firework_rocket", "minecraft:falling_block",
            "minecraft:eye_of_ender", "minecraft:experience_orb", "minecraft:evoker_fangs", "minecraft:dragon_fireball",
            "minecraft:boat", "minecraft:chest_boat", "minecraft:arrow", "minecraft:area_effect_cloud", "minecraft:armor_stand",
            "minecraft:minecart", "minecraft:furnace_minecart", "minecraft:chest_minecart", "minecraft:hopper_minecart",
            "minecraft:spawner_minecart", "minecraft:command_block_minecart", "minecraft:giant", "minecraft:gloe_item_frame",
            "minecraft:leash_know", "minecraft:llama_spit", "minecraft:potion", "minecraft:wither_skull", "subathon:fireball",
            "minecraft:text_display", "minecraft:item_display", "minecraft:block_display", "lil-donk:penguin", "subathon:chaos_fireball", "subathon:big_bomb"));

    @ExcludeFromScreen
    @PredicateConstraint("mobsConstrain")
    //TODO: tags
    @Comment("Mobs that can't be affected by things like randomize entities")
    public List<String> protectedEntities = new ArrayList<>(Arrays.asList("minecraft:player", "lil-donk:penguin",
            "minecraft:text_display", "minecraft:item_display", "minecraft:block_display"));

    @Hook
    @ExcludeFromScreen
    @PredicateConstraint("potionWeightConstrain")
    @Comment("The weights of the potions used on the rando potions effect")
    public Map<Identifier, Integer> potionWeights = new HashMap<>();

    @Hook
    @ExcludeFromScreen
    @PredicateConstraint("chaosWeightConstrain")
    @Comment("The weights of the chaos effects used on the chaos mode")
    public Map<Identifier, Integer> chaosWeights = new HashMap<>();

    @Comment("Points and triggers are shared between all players")
    public boolean sharedEffects = false;

    @PredicateConstraint("validTime")
    @Comment("The time events are held for before updating the points")
    public String updateCooldown = "0";

    @PredicateConstraint("thresholdRange")
    @Comment("The threshold for points, it stores the points as a separate value until the threshold is hit, with that it will add 1 point")
    public int threshold = 1;

    @SectionHeader("subs")
    @Nest public Common$Subs subs = new Common$Subs();

    public static class Common$Subs {
        public boolean enabled = true;
        @PredicateConstraint("thresholdRange")
        @Comment("The threshold for points, it stores the points as a separate value until the threshold is hit, with that it will add 1 point (scales with \"points\")")
        public int threshold = 1;
        @Comment("The amount of points to add per threshold")
        public int points = 1;
        @Nest public Common$Tier1 tier1 = new Common$Tier1();
        @Nest public Common$Tier2 tier2 = new Common$Tier2();
        @Nest public Common$Tier3 tier3 = new Common$Tier3();

        public static class Common$Tier1 {
            public boolean enabled = true;
            public int points = 1;
        }

        public static class Common$Tier2 {
            public boolean enabled = true;
            public int points = 2;
        }

        public static class Common$Tier3 {
            public boolean enabled = true;
            public int points = 3;
        }

        public static boolean thresholdRange(int value) {
            return value > 0;
        }
    }

    @SectionHeader("bits")
    @Nest public Common$Bits bits = new Common$Bits();

    public static class Common$Bits {
        public boolean enabled = true;
        public int points = 1;
        //@RangeConstraint(min = 0, max = 100000)
        @PredicateConstraint("bitsRange")
        @Comment("The minimum bits for adding points")
        public int minimum = 500;
        @Comment("When someone donates it will store the bits on a buffer and use it instead of bits directly")
        public boolean cumulative = true;
        @Comment("Only accumulates if the cheer is equals or bigger than the minimum amount. Only works with \"cumulative\" enabled")
        public boolean requiresMinimumToAccumulate = true;

        public static boolean bitsRange(int value) {
            return value >= 0;
        }
    }

    /*@SectionHeader("hypeChat")
    @Nest public Common$HypeChat hypeChat = new Common$HypeChat();

    public static class Common$HypeChat {
    }*/

    @SectionHeader("rewards")
    @Nest public Common$Rewards rewards = new Common$Rewards();

    public static class Common$Rewards {
        public boolean enabled = true;
        public int points = 1;
        @PredicateConstraint("thresholdRange")
        @Comment("The threshold for points, it stores the points as a separate value until the threshold is hit, with that it will add 1 point (scales with \"points\")")
        public int threshold = 0;

        public static boolean thresholdRange(int value) {
            return value > 0;
        }
    }

    public static boolean thresholdRange(int value) {
        return value > 0;
    }

    public static boolean triesRange(byte value) {
        return value > 5;
    }

    public static boolean validTime(String value) {
        return Utils.isValidTimeString(value);
    }

    /*public static boolean potionsConstrain(List<String> list) {
        return list.stream().allMatch(v -> Identifier.isValid(v) && Registry.STATUS_EFFECT.containsId(new Identifier(v)));
    }*/

    public static boolean mobsConstrain(List<String> list) {
        return list.stream().allMatch(v -> Identifier.isValid(v) && Registries.ENTITY_TYPE.containsId(new Identifier(v)));
    }

    public static boolean potionWeightConstrain(Map<Identifier, Integer> map) {
        return map.entrySet().stream().allMatch(v -> v.getValue() >= 0 && Registries.STATUS_EFFECT.containsId(v.getKey()));
    }

    public static boolean chaosWeightConstrain(Map<Identifier, Integer> map) {
        return map.entrySet().stream().allMatch(v -> v.getValue() >= 0 && SubathonRegistries.CHAOS.containsId(v.getKey()));
    }
}
