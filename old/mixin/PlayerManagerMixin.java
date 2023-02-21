package old.mixin;

import old.override.ExtendedOperatorList;
import net.minecraft.server.OperatorList;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.server.PlayerManager.OPERATORS_FILE;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Mutable @Final @Shadow private final OperatorList ops = new ExtendedOperatorList(OPERATORS_FILE);
}
