package com.awakenedredstone.subathon.command;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.command.argument.TwitchUsernameArgumentType;
import com.awakenedredstone.subathon.core.DataManager;
import com.awakenedredstone.subathon.core.data.Components;
import com.awakenedredstone.subathon.core.data.PlayerComponent;
import com.awakenedredstone.subathon.core.data.WorldComponent;
import com.awakenedredstone.subathon.integration.twitch.Twitch;
import com.awakenedredstone.subathon.util.MapBuilder;
import com.awakenedredstone.subathon.util.Shockwave;
import com.awakenedredstone.subathon.util.Texts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.krlite.equator.visual.color.AccurateColor;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SubathonCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("subathon").requires(source -> source.hasPermissionLevel(2) || source.getServer().isSingleplayer())
            .then(literal("world")
                .then(literal("trigger")
                    .executes(context -> {
                        DataManager.getActiveEffects().forEach(effect -> effect.trigger(context.getSource().getWorld()));
                        return 1;
                    })
                ).then(literal("points")
                    .executes(context -> {
                        WorldComponent data = Components.WORLD_DATA.get(context.getSource().getWorld());
                        context.getSource().sendFeedback(() -> Texts.of("command.subathon.get.points", new MapBuilder.StringMap().putAny("value", data.getPoints()).build()), false);
                        return (int) data.getPoints();
                    })
                    .then(argument("amount", IntegerArgumentType.integer())
                        .executes(context -> {
                            WorldComponent data = Components.WORLD_DATA.get(context.getSource().getWorld());
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            data.setPoints(amount);
                            context.getSource().sendFeedback(() -> Texts.of("command.subathon.set.points", new MapBuilder.StringMap().putAny("value", amount).build()), false);
                            return 1;
                        })
                    )
                ).then(literal("pointStorage")
                    .executes(context -> {
                        WorldComponent data = Components.WORLD_DATA.get(context.getSource().getWorld());
                        context.getSource().sendFeedback(() -> Texts.of("command.subathon.get.pointStorage", new MapBuilder.StringMap().putAny("value", data.getPointStorage()).build()), false);
                        return (int) data.getPointStorage();
                    })
                    .then(argument("amount", IntegerArgumentType.integer())
                        .executes(context -> {
                            WorldComponent data = Components.WORLD_DATA.get(context.getSource().getWorld());
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            context.getSource().sendFeedback(() -> Texts.of("command.subathon.set.pointStorage", new MapBuilder.StringMap().putAny("value", amount).build()), false);
                            data.setPointStorage(amount);
                            return 1;
                        })
                    )
                ).then(literal("subPoints")
                    .executes(context -> {
                        WorldComponent data = Components.WORLD_DATA.get(context.getSource().getWorld());
                        context.getSource().sendFeedback(() -> Texts.of("command.subathon.get.subPoints", new MapBuilder.StringMap().putAny("value", data.getSubPointsStorage()).build()), false);
                        return (int) data.getSubPointsStorage();
                    })
                    .then(argument("amount", IntegerArgumentType.integer())
                        .executes(context -> {
                            WorldComponent data = Components.WORLD_DATA.get(context.getSource().getWorld());
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            data.setSubPoints(amount);
                            context.getSource().sendFeedback(() -> Texts.of("command.subathon.set.subPoints", new MapBuilder.StringMap().putAny("value", amount).build()), false);
                            return 1;
                        })
                    )
                ).then(literal("bitStorage")
                    .executes(context -> {
                        WorldComponent data = Components.WORLD_DATA.get(context.getSource().getWorld());
                        context.getSource().sendFeedback(() -> Texts.of("command.subathon.get.bitStorage", new MapBuilder.StringMap().putAny("value", data.getBitStorage()).build()), false);
                        return (int) data.getBitStorage();
                    })
                    .then(argument("amount", IntegerArgumentType.integer())
                        .executes(context -> {
                            WorldComponent data = Components.WORLD_DATA.get(context.getSource().getWorld());
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            data.setBitStorage(amount);
                            context.getSource().sendFeedback(() -> Texts.of("command.subathon.set.bitStorage", new MapBuilder.StringMap().putAny("value", amount).build()), false);
                            return 1;
                        })
                    )
                ).then(literal("redemptionPoints")
                    .executes(context -> {
                        WorldComponent data = Components.WORLD_DATA.get(context.getSource().getWorld());
                        context.getSource().sendFeedback(() -> Texts.of("command.subathon.get.redemptionPoints", new MapBuilder.StringMap().putAny("value", data.getRedemptionPoints()).build()), false);
                        return (int) data.getRedemptionPoints();
                    })
                    .then(argument("amount", IntegerArgumentType.integer())
                        .executes(context -> {
                            WorldComponent data = Components.WORLD_DATA.get(context.getSource().getWorld());
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            data.setRedemptionPoints(amount);
                            context.getSource().sendFeedback(() -> Texts.of("command.subathon.set.redemptionPoints", new MapBuilder.StringMap().putAny("value", amount).build()), false);
                            return 1;
                        })
                    )
                )
            ).then(literal("player")
                .then(argument("player", EntityArgumentType.player())
                    .then(literal("trigger")
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            DataManager.getActiveEffects().forEach(effect -> effect.trigger(player));
                            return 1;
                        })
                    ).then(literal("points")
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            PlayerComponent data = Components.PLAYER_DATA.get(player);
                            context.getSource().sendFeedback(() -> Texts.of("command.subathon.get.points", new MapBuilder.StringMap().putAny("value", data.getPoints()).build()), false);
                            return (int) data.getPoints();
                        })
                        .then(argument("amount", IntegerArgumentType.integer())
                            .executes(context -> {
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                PlayerComponent data = Components.PLAYER_DATA.get(player);
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                data.setPoints(amount);
                                context.getSource().sendFeedback(() -> Texts.of("command.subathon.set.points", new MapBuilder.StringMap().putAny("value", amount).build()), false);
                                return 1;
                            })
                        )
                    ).then(literal("pointStorage")
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            PlayerComponent data = Components.PLAYER_DATA.get(player);
                            context.getSource().sendFeedback(() -> Texts.of("command.subathon.get.pointStorage", new MapBuilder.StringMap().putAny("value", data.getPointStorage()).build()), false);
                            return (int) data.getPointStorage();
                        })
                        .then(argument("amount", IntegerArgumentType.integer())
                            .executes(context -> {
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                PlayerComponent data = Components.PLAYER_DATA.get(player);
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                context.getSource().sendFeedback(() -> Texts.of("command.subathon.set.pointStorage", new MapBuilder.StringMap().putAny("value", amount).build()), false);
                                data.setPointStorage(amount);
                                return 1;
                            })
                        )
                    ).then(literal("subPoints")
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            PlayerComponent data = Components.PLAYER_DATA.get(player);
                            context.getSource().sendFeedback(() -> Texts.of("command.subathon.get.subPoints", new MapBuilder.StringMap().putAny("value", data.getSubPointsStorage()).build()), false);
                            return (int) data.getSubPointsStorage();
                        })
                        .then(argument("amount", IntegerArgumentType.integer())
                            .executes(context -> {
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                PlayerComponent data = Components.PLAYER_DATA.get(player);
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                data.setSubPoints(amount);
                                context.getSource().sendFeedback(() -> Texts.of("command.subathon.set.subPoints", new MapBuilder.StringMap().putAny("value", amount).build()), false);
                                return 1;
                            })
                        )
                    ).then(literal("bitStorage")
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            PlayerComponent data = Components.PLAYER_DATA.get(player);
                            context.getSource().sendFeedback(() -> Texts.of("command.subathon.get.bitStorage", new MapBuilder.StringMap().putAny("value", data.getBitStorage()).build()), false);
                            return (int) data.getBitStorage();
                        })
                        .then(argument("amount", IntegerArgumentType.integer())
                            .executes(context -> {
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                PlayerComponent data = Components.PLAYER_DATA.get(player);
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                data.setBitStorage(amount);
                                context.getSource().sendFeedback(() -> Texts.of("command.subathon.set.bitStorage", new MapBuilder.StringMap().putAny("value", amount).build()), false);
                                return 1;
                            })
                        )
                    ).then(literal("redemptionPoints")
                        .executes(context -> {
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            PlayerComponent data = Components.PLAYER_DATA.get(player);
                            context.getSource().sendFeedback(() -> Texts.of("command.subathon.get.redemptionPoints", new MapBuilder.StringMap().putAny("value", data.getRedemptionPoints()).build()), false);
                            return (int) data.getRedemptionPoints();
                        })
                        .then(argument("amount", IntegerArgumentType.integer())
                            .executes(context -> {
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                PlayerComponent data = Components.PLAYER_DATA.get(player);
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                data.setRedemptionPoints(amount);
                                context.getSource().sendFeedback(() -> Texts.of("command.subathon.set.redemptionPoints", new MapBuilder.StringMap().putAny("value", amount).build()), false);
                                return 1;
                            })
                        )
                    )
                )
            ).then(literal("test")
                .then(literal("irc")
                    .then(literal("joinChannel")
                        .then(argument("channel", TwitchUsernameArgumentType.create())
                            .executes(context -> {
                                String channel = TwitchUsernameArgumentType.getUsername(context, "channel");
                                Twitch.getInstance().getChatPool().joinChannel(channel);
                                context.getSource().sendFeedback(() -> Text.literal("Joined channel " + channel), true);
                                return 1;
                            })
                        )
                    ).then(literal("leaveChannel")
                        .then(argument("channel", TwitchUsernameArgumentType.create())
                            .suggests((context, builder) -> CommandSource.suggestMatching(Twitch.getInstance().getChatPool().getChannels(), builder))
                            .executes(context -> {
                                String channel = TwitchUsernameArgumentType.getUsername(context, "channel");
                                Twitch.getInstance().getChatPool().leaveChannel(channel);
                                context.getSource().sendFeedback(() -> Text.literal("Left channel " + channel), true);
                                return 1;
                            })
                        )
                    )
                ).then(literal("shockwave")
                    .then(argument("pos", Vec3ArgumentType.vec3())
                        .then(argument("radius", FloatArgumentType.floatArg())
                            .then(argument("duration", IntegerArgumentType.integer())
                                .then(argument("color", StringArgumentType.word())
                                    .executes(context -> {
                                        Vec3d pos = Vec3ArgumentType.getVec3(context, "pos");
                                        float radius = FloatArgumentType.getFloat(context, "radius");
                                        int duration = IntegerArgumentType.getInteger(context, "duration");
                                        String color = StringArgumentType.getString(context, "color");

                                        Shockwave.builder()
                                            .world(context.getSource().getWorld())
                                            .pos(pos)
                                            .radius(radius)
                                            .duration(duration)
                                            .color(AccurateColor.fromHexString(color))
                                            .build()
                                            .spawn();
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    private static void placeSculk(ServerWorld world, BlockPos blockPos) {
        world.getRegistryManager().getOptional(RegistryKeys.CONFIGURED_FEATURE)
            .flatMap(registry -> registry.getEntry(RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Subathon.id("sculk_patch"))))
            .ifPresent(reference -> reference.value().generate(world, world.getChunkManager().getChunkGenerator(), world.random, blockPos.up()));
    }
}
