package com.awakenedredstone.subathon.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.neovisionaries.ws.client.WebSocket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static com.awakenedredstone.subathon.Subathon.eventSub;

public class DebugCommand {
    private static volatile WebSocket webSocket;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("sbt-test").requires((source) -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("websocket")
                        .then(CommandManager.literal("start").executes((source) -> websocketTestStart(source.getSource())))
                        .then(CommandManager.literal("stop").executes((source) -> websocketTestStop(source.getSource())))
                )
        );
    }

    private static int websocketTestStart(ServerCommandSource source) {
        eventSub.connect();
        return 0;
    }

    private static int websocketTestStop(ServerCommandSource source) {
        eventSub.disconnect();
        return 0;
    }
}
