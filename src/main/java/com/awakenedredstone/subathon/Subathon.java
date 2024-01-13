package com.awakenedredstone.subathon;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.awakenedredstone.subathon.command.SubathonCommand;
import com.awakenedredstone.subathon.command.argument.TwitchUsernameArgumentType;
import com.awakenedredstone.subathon.config.CommonConfigs;
import com.awakenedredstone.subathon.config.ConfigsClient;
import com.awakenedredstone.subathon.core.data.ComponentManager;
import com.awakenedredstone.subathon.core.effect.Effect;
import com.awakenedredstone.subathon.core.effect.chaos.Chaos;
import com.awakenedredstone.subathon.event.RegistryFreezeCallback;
import com.awakenedredstone.subathon.registry.*;
import com.awakenedredstone.subathon.integration.twitch.Twitch;
import com.awakenedredstone.subathon.util.ConversionUtils;
import com.awakenedredstone.subathon.util.MessageUtils;
import com.awakenedredstone.subathon.util.ScheduleUtils;
import com.awakenedredstone.subathon.util.WeightedRandom;
import io.wispforest.owo.network.serialization.PacketBufSerializer;
import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
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
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

//TODO: Make all translations use PlaceholderAPI
public class Subathon implements ModInitializer {
    public static final String MOD_ID = "subathon";
    public static final Logger LOGGER = LoggerFactory.getLogger("Sub-a-thon");
    public static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    public static CommonConfigs COMMON_CONFIGS;
    //public static final ServerConfigs SERVER_CONFIGS = ServerConfigs.createAndLoad();
    public static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder().build();
    @Getter
    private static Subathon instance;
    private MinecraftServer server = null;

    @Getter
    private ScheduleUtils scheduler;
    public final WeightedRandom<StatusEffect> potionsRandom = new WeightedRandom<>();
    public final WeightedRandom<Chaos> chaosRandom = new WeightedRandom<>();
    public final Queue<Runnable> delayedEvents = new LinkedList<>();
    @Getter private List<Block> infestedBlocks;

    @Override
    public void onInitialize() {
        instance = this;
        ArgumentTypeRegistry.registerArgumentType(id("twitch_username"), TwitchUsernameArgumentType.class, ConstantArgumentSerializer.of(TwitchUsernameArgumentType::create));
        FieldRegistrationHandler.register(EntityRegistry.class, MOD_ID, false);
        FieldRegistrationHandler.register(EffectRegistry.class, MOD_ID, false);
        FieldRegistrationHandler.register(ChaosRegistry.class, MOD_ID, false);
        FieldRegistrationHandler.register(SoundRegistry.class, MOD_ID, false);
        SubathonCriteria.init();
        FeatureRegistry.init();

        RegistryFreezeCallback.EVENT.register(() -> {
            COMMON_CONFIGS = CommonConfigs.createAndLoad(builder -> {
                builder.registerSerializer(Effect.class, (effect, marshaller) -> {
                    JsonObject json = new JsonObject();
                    json.put("identifier", new JsonPrimitive(effect.getIdentifier().toString()));
                    json.put("enabled", new JsonPrimitive(effect.enabled));
                    json.put("scale", new JsonPrimitive(effect.scale));
                    return json;
                });

                builder.registerDeserializer(JsonObject.class, Effect.class, (json, m) -> {
                    try {
                        Identifier identifier = new Identifier(((JsonPrimitive) json.get("identifier")).asString());
                        boolean enabled = json.getBoolean("enabled", false);
                        double scale = json.getDouble("scale", 1.0);

                        var effect = SubathonRegistries.EFFECTS.get(identifier);
                        if (effect == null) return null;
                        effect.enabled = enabled;
                        effect.scale = scale;
                        return effect;
                    } catch (Exception e) {
                        return null;
                    }
                });
            });

            COMMON_CONFIGS.subscribeToPotionWeights(map -> {
                potionsRandom.reset();
                map.forEach((identifier, integer) -> potionsRandom.add(integer, Registries.STATUS_EFFECT.get(identifier)));
            });

            COMMON_CONFIGS.subscribeToChaosWeights(map -> {
                chaosRandom.reset();
                map.forEach((identifier, integer) -> chaosRandom.add(integer, SubathonRegistries.CHAOS.get(identifier)));
            });

            Registries.STATUS_EFFECT.forEach(statusEffect -> COMMON_CONFIGS.potionWeights().putIfAbsent(Registries.STATUS_EFFECT.getId(statusEffect), 1));
            SubathonRegistries.CHAOS.forEach(chaos -> COMMON_CONFIGS.chaosWeights().putIfAbsent(chaos.getIdentifier(), chaos.getDefaultWeight()));
            SubathonRegistries.EFFECTS.forEach(effect -> COMMON_CONFIGS.effects().putIfAbsent(effect.getIdentifier(), effect));
            COMMON_CONFIGS.save();
        });

        registerPacketReceivers();
        registerEventListeners();
    }

    private void registerPacketReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(Subathon.id("auth_key"), (server, player, handler, buf, responseSender) -> {
            String token = buf.readString();
            server.execute(() -> {
                Twitch.getInstance().addData(player.getUuid(), new Twitch.Data(token));
                Twitch.getInstance().connectEventSub(token, player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Subathon.id("channel"), (server, player, handler, buf, responseSender) -> {
            String channel = buf.readString();
            server.execute(() -> {
                Twitch.getInstance().addData(player.getUuid(), new Twitch.Data(""));
                Twitch.getInstance().connectIRC(channel, player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Subathon.id("disconnect"), (server, player, handler, buf, responseSender) -> {
            ConfigsClient.ConnectionType type = buf.readEnumConstant(ConfigsClient.ConnectionType.class);
            String key = buf.readString();
            server.execute(() -> {
                switch (type) {
                    case IRC -> Twitch.getInstance().disconnectIRC(player.getUuid());
                    case EVENTSUB -> Twitch.getInstance().disconnectEventSub(key);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Subathon.id("reward_id"), (server, player, handler, buf, responseSender) -> {
            UUID rewardId = buf.readUuid();
            server.execute(() -> {
                Twitch.getInstance().getData().get(player.getUuid()).setRewardId(rewardId);
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
            this.server = server;
            this.scheduler = new ScheduleUtils();

            Registries.STATUS_EFFECT.forEach(statusEffect -> COMMON_CONFIGS.potionWeights().putIfAbsent(Registries.STATUS_EFFECT.getId(statusEffect), 1));
            SubathonRegistries.CHAOS.forEach(chaos -> COMMON_CONFIGS.chaosWeights().putIfAbsent(SubathonRegistries.CHAOS.getId(chaos), 1));

            COMMON_CONFIGS.potionWeights().forEach((identifier, integer) -> potionsRandom.add(integer, Registries.STATUS_EFFECT.get(identifier)));
            COMMON_CONFIGS.chaosWeights().forEach((identifier, integer) -> chaosRandom.add(integer, SubathonRegistries.CHAOS.get(identifier)));
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            while (!delayedEvents.isEmpty()) {
                delayedEvents.poll().run();
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            this.server = null;
            Twitch.getInstance().reset();
            this.scheduler.close();
            this.scheduler = null;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String version = FabricLoader.getInstance().getModContainer(Subathon.MOD_ID).get().getMetadata().getVersion().getFriendlyString();
            sender.sendPacket(Subathon.id("mod_version"), PacketByteBufs.create().writeString(version));
            //sender.sendPacket(Subathon.id("ui_warning"), PacketByteBufs.create());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Twitch.getInstance().removeConnection(handler.getPlayer().getUuid());
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

    public static <T extends AbstractFireballEntity> FabricEntityTypeBuilder<T> createFireballEntity(SpawnGroup spawnGroup, EntityType.EntityFactory<T> factory) {
        return FabricEntityTypeBuilder.create(spawnGroup, factory);
    }

    public static <T extends ProjectileEntity> FabricEntityTypeBuilder<T> createProjectileEntity(SpawnGroup spawnGroup, EntityType.EntityFactory<T> factory) {
        return FabricEntityTypeBuilder.create(spawnGroup, factory);
    }

    public static MinecraftServer getServer() {
        return instance.server;
    }

    public static long getPoints(PlayerEntity player) {
        return ComponentManager.getComponent(getServer(), player).getPoints();
    }

    public static void schedule(long delay, Runnable runnable) {
        instance.scheduler.schedule(getServer(), delay, runnable);
    }

    static {
        PacketBufSerializer.register(Effect.class, (buf, effect) -> {
            buf.writeIdentifier(effect.getIdentifier());
            buf.writeDouble(effect.scale);
            buf.writeBoolean(effect.enabled);
        }, buf -> {
            Identifier identifier = buf.readIdentifier();
            double scale = buf.readDouble();
            boolean enabled = buf.readBoolean();
            Effect effect = SubathonRegistries.EFFECTS.get(identifier);
            if (effect == null) return null;
            effect.enabled = enabled;
            effect.scale = scale;
            return effect;
        });
    }
}
