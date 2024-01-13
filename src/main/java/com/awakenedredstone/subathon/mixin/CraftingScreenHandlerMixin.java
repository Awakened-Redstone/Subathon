package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.Effect;
import com.awakenedredstone.subathon.registry.SubathonRegistries;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CraftingScreenHandler.class)
public class CraftingScreenHandlerMixin {

    @Redirect(
        method = "updateResult",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/CraftingResultInventory;setStack(ILnet/minecraft/item/ItemStack;)V")
    )
    private static void multiplyOutput(CraftingResultInventory instance, int slot, ItemStack stack) {
        Effect effect = SubathonRegistries.EFFECTS.get(Subathon.id("increase_recipe_output"));
        if (effect != null && effect.enabled) {
            stack.setCount((int) (stack.getCount() * effect.scale * Subathon.getPoints(null)));

        }
        instance.setStack(slot, stack);
    }
}
