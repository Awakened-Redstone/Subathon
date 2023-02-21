package old.server;

import old.Subathon;
import old.util.MessageUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.SERVER)
public class SubathonServer implements DedicatedServerModInitializer {
    public static List<ServerPlayerEntity> playersWithMod = new ArrayList<>();
    public static List<ServerPlayerEntity> playersWithoutMod = new ArrayList<>();

    @Override
    public void onInitializeServer() {
        ServerPlayNetworking.registerGlobalReceiver(new Identifier(Subathon.MOD_ID, "has_mod"), (server, player, handler, buf, responseSender) ->
                server.execute(() -> {
                    playersWithoutMod.remove(player);
                    playersWithMod.add(player);
                })
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> playersWithoutMod.add(handler.player));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playersWithoutMod.remove(handler.player);
            playersWithMod.remove(handler.player);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> MessageUtils.broadcast(playersWithoutMod,
                player -> player.sendMessage(new LiteralText(String.format("Modifier: %s", Subathon.integration.data.value)), true),
                "send_value_to_players_without_mod"));
    }
}
