package old.mixin;getJumpVelocity

import net.minecraft.server.ServerConfigEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerConfigEntry.class)
public interface ServerConfigEntryMixin<T> {

    @Accessor
    T getKey();

}
