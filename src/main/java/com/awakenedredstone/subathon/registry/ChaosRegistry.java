package com.awakenedredstone.subathon.registry;

import com.awakenedredstone.subathon.core.effect.chaos.*;
import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.minecraft.registry.Registry;

@SuppressWarnings("unused")
public class ChaosRegistry implements AutoRegistryContainer<Chaos> {

    public static final Chaos ARROWS = new Arrows();
    public static final Chaos DIMENSION_SWAP = new DimensionSwap();
    public static final Chaos DROP_EVERYTHING = new DropEverything();
    public static final Chaos DROP_HELD_ITEM = new DropHeldItem();
    public static final Chaos DROP_HOTBAR = new DropHotbar();
    public static final Chaos INFESTED_SPHERE = new InfestedSphere();
    public static final Chaos KABOOM = new Kaboom();
    public static final Chaos MOBS = new Mobs();
    public static final Chaos RANDOM_ENCHANT = new RandomEnchant();
    public static final Chaos RANDOM_ENCHANT_COMPATIBLE = new RandomCompatibleEnchant();
    public static final Chaos RANDOM_ENCHANT_INCOMPATIBLE = new RandomIncompatibleEnchant();
    public static final Chaos RANDOM_ITEM = new RandomItem();
    public static final Chaos RANDOMIZE_ENTITIES = new RandomizeEntities();
    public static final Chaos RANDO_POTION = new RandoPotion();
    public static final Chaos RETURN_TO_SPAWN = new ReturnToSpawn();
    public static final Chaos SHUFFLE_INVENTORY = new ShuffleInventory();
    public static final Chaos SNOW_BALL = new SnowBall();
    public static final Chaos TNT_SPHERE = new TntSphere();
    public static final Chaos YEET = new Yeet();

    @Override
    public Registry<Chaos> getRegistry() {
        return SubathonRegistries.CHAOS;
    }

    @Override
    public Class<Chaos> getTargetFieldType() {
        return Chaos.class;
    }
}