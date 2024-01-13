package com.awakenedredstone.subathon.client.command;

import com.awakenedredstone.subathon.client.SubathonClient;
import com.awakenedredstone.subathon.client.render.fx.Shockwave;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.krlite.equator.visual.color.AccurateColor;
import net.krlite.equator.visual.color.Palette;
import net.minecraft.client.MinecraftClient;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SubathonClientCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("subathonc")
                .then(literal("test")
                        .then(literal("shockwave")
                                .executes(context -> {
                                    SubathonClient.getInstance().getShockwaves().add(new Shockwave(
                                            MinecraftClient.getInstance().player.getPos(),
                                            0.0f,
                                            500.0f,
                                            MinecraftClient.getInstance().world.getTime(),
                                            MinecraftClient.getInstance().world.getTime() + 50,
                                            Palette.CYAN
                                    ));
                                    return 1;
                                })
                        )
                )
        );
    }
}
