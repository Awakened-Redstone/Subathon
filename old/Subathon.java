package old;

import old.commands.SubathonCommand;
import old.config.Config;
import old.config.ConfigData;
import old.config.Mode;
import old.events.LivingEntityCallback;
import old.json.JsonHelper;
import old.potions.SubathonStatusEffect;
import old.twitch.EventListener;
import old.twitch.SubathonData;
import old.twitch.TwitchIntegration;
import old.util.BotStatus;
import old.util.ConfigUtils;
import old.util.MessageUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neovisionaries.ws.client.WebSocketFactory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.registry.Registry;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;

public class Subathon implements ModInitializer {
    public static String MOD_ID = "subathon";
    public static MinecraftServer server;
    public static TwitchIntegration integration = new TwitchIntegration();
    public static CommandBossBar mainProgressBar;
    public static CommandBossBar usersProgressBar;

    public static final OkHttpClient OKHTTPCLIENT = new OkHttpClient();
    public static final StatusEffect SUBATHON_EFFECT = new SubathonStatusEffect();
    public static final EventListener eventListener = new EventListener();
    public static final WebSocketFactory webSocketFactory = new WebSocketFactory();
    public static final Config config = new Config();
    public static final Logger LOGGER = LoggerFactory.getLogger("Subathon");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private int potionTick = 10;

    private static final File configFile = new File(config.getConfigDirectory(), "subathon.json");

    @Override
    public void onInitialize() {
        Registry.register(Registry.STATUS_EFFECT, new Identifier(MOD_ID, "subathon"), SUBATHON_EFFECT);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (getConfigData().resetTimer > 0 && server.getTicks() % getConfigData().resetTimer == 0) integration.setValue(0);
            if (getConfigData().updateTimer > 0 && server.getTicks() % getConfigData().updateTimer == 0) {
                integration.increaseValue(integration.data.tempValue, true);
                integration.data.tempValue = 0;
            }

            //Send the server ticks to the players every second
            if ((getConfigData().resetTimer > 0 || getConfigData().updateTimer > 0) && server.getTicks() % 20 == 0) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(server.getTicks());
                MessageUtils.broadcast(player -> ServerPlayNetworking.send(player, new Identifier(MOD_ID, "server_ticks"), buf), "send_ticks");
            }

            if (Subathon.integration.data.value != 0 && potionTick-- == 0) {
                potionTick = 10;
                MessageUtils.broadcast(player -> player.addStatusEffect(
                        new StatusEffectInstance(SUBATHON_EFFECT, potionTick + 5, 0, false, false)), "apply_potion");
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            mainProgressBar.addPlayer(handler.player);
            usersProgressBar.addPlayer(handler.player);

            sender.sendPacket(new Identifier(Subathon.MOD_ID, "has_mod"), PacketByteBufs.create());

            if (handler.player.hasPermissionLevel(1)) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeEnumConstant(Subathon.integration.isRunning ? BotStatus.RUNNING : BotStatus.OFFLINE);
                sender.sendPacket(new Identifier(Subathon.MOD_ID, "bot_status"), buf);
            }

            if (integration.data != null) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeDouble(integration.getDisplayValue());
                sender.sendPacket(new Identifier(Subathon.MOD_ID, "value"), buf);
            }
        });
        ServerLifecycleEvents.SERVER_STARTING.register(this::registerCommands);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> registerCommands(server));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Subathon.server = server;

            mainProgressBar = server.getBossBarManager().add(new Identifier(MOD_ID, "loading_progress_main"), new TranslatableText("text.subathon.load.main", "", 0));
            mainProgressBar.setColor(BossBar.Color.RED);
            mainProgressBar.setMaxValue(6);
            mainProgressBar.setValue(0);
            mainProgressBar.setVisible(false);

            usersProgressBar = server.getBossBarManager().add(new Identifier(MOD_ID, "loading_progress_users"), new TranslatableText("text.subathon.load.users", 0, 0));
            usersProgressBar.setColor(BossBar.Color.RED);
            usersProgressBar.setMaxValue(1);
            usersProgressBar.setValue(0);
            usersProgressBar.setVisible(false);

            File file = server.getSavePath(WorldSavePath.ROOT).resolve("subathon_data.json").toFile();
            if (!file.exists())
                JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(new SubathonData()).getAsJsonObject(), file);
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                SubathonData data = Subathon.GSON.fromJson(reader, SubathonData.class);
                if (getConfigData().runAtServerStart) integration.start(data);
                else integration.data = data;
            } catch (IOException e) {
                if (getConfigData().runAtServerStart) Subathon.LOGGER.error("Failed to start the integration!", e);
                integration.simpleExecutor.execute(new TwitchIntegration.ClearProgressBar());
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (getConfigData().updateTimer > 0) integration.data.value += integration.data.tempValue;
            integration.stop(false);
        });

        LivingEntityCallback.JUMP.register((entity) -> {
            if (!entity.isPlayer() && ConfigUtils.getMode() != Mode.SUPER_JUMP) return;
            if (ConfigUtils.getMode() == Mode.JUMP || ConfigUtils.getMode() == Mode.SUPER_JUMP) increaseJump(entity);
        });

        generateConfig();
        config.loadConfigs();
    }

    private void registerCommands(MinecraftServer server) {
        SubathonCommand.register(server.getCommandManager().getDispatcher());
    }

    public static ConfigData getConfigData() {
        return config.getConfigData();
    }

    public static void generateConfig() {
        File dir = config.getConfigDirectory();
        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs()) {
            if (!configFile.exists()) {
                JsonHelper.writeJsonToFile(config.generateDefaultConfig(), configFile);
            }
        }
    }

    private void increaseJump(LivingEntity entity) {
        entity.addVelocity(0, BigDecimal.valueOf(Subathon.integration.data.value).multiply(BigDecimal.valueOf(0.1f)).floatValue(), 0);
    }
}