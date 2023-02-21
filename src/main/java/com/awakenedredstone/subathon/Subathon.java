package com.awakenedredstone.subathon;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.awakenedredstone.subathon.command.SubathonCommand;
import com.awakenedredstone.subathon.config.CommonConfigs;
import com.awakenedredstone.subathon.config.ConfigsClient;
import com.awakenedredstone.subathon.core.DataManager;
import com.awakenedredstone.subathon.core.effect.chaos.process.Chaos;
import com.awakenedredstone.subathon.core.effect.chaos.process.ChaosRegistry;
import com.awakenedredstone.subathon.core.effect.process.Effect;
import com.awakenedredstone.subathon.core.effect.process.EffectRegistry;
import com.awakenedredstone.subathon.entity.FireballEntity;
import com.awakenedredstone.subathon.event.RegistryFreezeCallback;
import com.awakenedredstone.subathon.twitch.Twitch;
import com.awakenedredstone.subathon.util.*;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.network.serialization.PacketBufSerializer;
import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.InfestedBlock;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class Subathon implements ModInitializer {
    public static final String MOD_ID = "subathon";
    public static final Logger LOGGER = LoggerFactory.getLogger("Sub-a-thon");
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    public static final CommonConfigs COMMON_CONFIGS;
    //public static final ServerConfigs SERVER_CONFIGS = ServerConfigs.createAndLoad();
    public static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder().build();
    public static MinecraftServer server = null;
    public static ScheduleUtils scheduler;
    public static WeightedRandom<StatusEffect> potionsRandom = new WeightedRandom<>();
    public static WeightedRandom<Chaos> chaosRandom = new WeightedRandom<>();
    public static final Queue<Runnable> delayedEvents = new LinkedList<>();
    public static List<Block> infestedBlocks;

    @Override
    public void onInitialize() {
        Twitch.init();

        FieldRegistrationHandler.register(EntityInitializer.class, MOD_ID, false);
        EffectRegistry.registry.forEach((identifier, effect) -> {
            COMMON_CONFIGS.effects().putIfAbsent(identifier, effect);
        });

        Registries.STATUS_EFFECT.forEach(statusEffect -> COMMON_CONFIGS.potionWeights().putIfAbsent(Registries.STATUS_EFFECT.getId(statusEffect), 1));
        ChaosRegistry.registry.forEach((identifier, chaos) -> COMMON_CONFIGS.chaosWeights().putIfAbsent(identifier, 1));

        COMMON_CONFIGS.potionWeights().forEach((identifier, integer) -> potionsRandom.add(integer, Registries.STATUS_EFFECT.get(identifier)));
        COMMON_CONFIGS.chaosWeights().forEach((identifier, integer) -> chaosRandom.add(integer, ChaosRegistry.registry.get(identifier)));

        COMMON_CONFIGS.subscribeToPotionWeights(map -> {
            potionsRandom = new WeightedRandom<>();
            map.forEach((identifier, integer) -> {
                potionsRandom.add(integer, Registries.STATUS_EFFECT.get(identifier));
            });
        });

        registerPacketReceivers();
        registerEventListeners();
    }

    private void registerPacketReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(Subathon.id("auth_key"), (server, player, handler, buf, responseSender) -> {
            String token = buf.readString();
            server.execute(() -> {
                Twitch.data.put(player.getUuid(), new Twitch.Data(token));
                Twitch.connect(token, player);
            });
        });
        
        ServerPlayNetworking.registerGlobalReceiver(Subathon.id("disconnect"), (server, player, handler, buf, responseSender) -> {
            String token = buf.readString();
            server.execute(() -> {
                Twitch.disconnect(token);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Subathon.id("reward_id"), (server, player, handler, buf, responseSender) -> {
            UUID rewardId = buf.readUuid();
            server.execute(() -> {
                Twitch.data.get(player.getUuid()).setRewardId(rewardId);
            });
        });
    }

    private void registerEventListeners() {
        RegistryFreezeCallback.EVENT.register(() -> {
            infestedBlocks = Registries.BLOCK.stream().filter(block -> block instanceof InfestedBlock).toList();
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SubathonCommand.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Subathon.server = server;
            Subathon.scheduler = new ScheduleUtils();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            while (!delayedEvents.isEmpty()) {
                delayedEvents.poll().run();
            }

            Subathon.server = null;
            Twitch.data.clear();
            Twitch.reset();
            Subathon.scheduler.destroy();
            Subathon.scheduler = null;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String version = FabricLoader.getInstance().getModContainer(Subathon.MOD_ID).get().getMetadata().getVersion().getFriendlyString();
            sender.sendPacket(Subathon.id("mod_version"), PacketByteBufs.create().writeString(version));
            sender.sendPacket(Subathon.id("eventsub_warning"), PacketByteBufs.create());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Twitch.disconnect(Twitch.data.remove(handler.getPlayer().getUuid()).token);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            scheduler.tick(server);
            if (updateCooldown() > 0 && server.getTicks() % updateCooldown() == 0) {
                while (!delayedEvents.isEmpty()) {
                    delayedEvents.poll().run();
                }
            }

            if (updateCooldown() > 0 && server.getTicks() % 20 == 0) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(updateCooldown() - (server.getTicks() % updateCooldown()));
                MessageUtils.broadcastPacket(id("next_update"), buf);
            } else if (updateCooldown() == 0) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(-1);
                MessageUtils.broadcastPacket(id("next_update"), buf);
            }
        });
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    public static Identifier spriteId(String path) {
        return new Identifier(MOD_ID, "subathon/" + path);
    }

    public static int updateCooldown() {
        return ConversionUtils.timeStringToTicks(COMMON_CONFIGS.updateCooldown());
    }

    public static <T extends FireballEntity> FabricEntityTypeBuilder<T> createEntity(SpawnGroup spawnGroup, EntityType.EntityFactory<T> factory) {
        return FabricEntityTypeBuilder.create(spawnGroup, factory);
    }

    static {
        PacketBufSerializer.register(Effect.class, (buf, effect) -> {
            buf.writeIdentifier(effect.identifier);
            buf.writeDouble(effect.scale);
            buf.writeBoolean(effect.enabled);
        }, buf -> {
            Identifier identifier = buf.readIdentifier();
            double scale = buf.readDouble();
            boolean enabled = buf.readBoolean();
            Effect effect = EffectRegistry.registry.get(identifier);
            effect.enabled = enabled;
            effect.scale = scale;
            return effect;
        });
        COMMON_CONFIGS = CommonConfigs.createAndLoad(builder -> {
            builder.registerSerializer(Effect.class, (effect, marshaller) -> {
                JsonObject json = new JsonObject();
                json.put("identifier", new JsonPrimitive(effect.identifier));
                json.put("enabled", new JsonPrimitive(effect.enabled));
                json.put("scale", new JsonPrimitive(effect.scale));
                return json;
            });

            builder.registerDeserializer(JsonObject.class, Effect.class, (json, m) -> {
                try {
                    Identifier identifier = new Identifier(((JsonPrimitive) json.get("identifier")).asString());
                    boolean enabled = json.getBoolean("enabled", false);
                    double scale = json.getDouble("scale", 1.0);

                    var effect = EffectRegistry.registry.get(identifier);
                    effect.enabled = enabled;
                    effect.scale = scale;
                    return effect;
                } catch (Exception e) {
                    return null;
                }
            });
        });

    }
}
