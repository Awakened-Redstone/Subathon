package old.mixin;

import old.Subathon;
import old.json.JsonHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.nio.file.Path;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow public abstract Path getSavePath(WorldSavePath worldSavePath);

    @Inject(method = "save(ZZZ)Z", at = @At(value = "TAIL"))
    private void save(boolean suppressLogs, boolean flush, boolean force, CallbackInfoReturnable<Boolean> cir) {
        File file = this.getSavePath(WorldSavePath.ROOT).resolve("subathon_data.json").toFile();
        JsonHelper.writeJsonToFile(Subathon.GSON.toJsonTree(Subathon.integration.data).getAsJsonObject(), file);
    }
}
