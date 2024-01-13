package com.awakenedredstone.subathon.mixin;

import com.awakenedredstone.subathon.Subathon;
import com.awakenedredstone.subathon.core.effect.Effect;
import com.awakenedredstone.subathon.registry.SubathonRegistries;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin extends LockableContainerBlockEntity {

    protected AbstractFurnaceBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @ModifyArg(
        method = "craftRecipe",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/DefaultedList;set(ILjava/lang/Object;)Ljava/lang/Object;"),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/DefaultedList;get(I)Ljava/lang/Object;"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;")
        ),
        index = 1
    )
    private static <E> E multiplyOutput1(E originalStack) {
        ItemStack stack = ((ItemStack) originalStack).copy();
        Effect effect = SubathonRegistries.EFFECTS.get(Subathon.id("increase_recipe_output"));
        if (effect != null && effect.enabled) {
            stack.setCount((int) (stack.getCount() * effect.scale * Subathon.getPoints(null)));
        }
        return (E) stack;
    }

    @ModifyArg(
        method = "craftRecipe",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;increment(I)V"),
        index = 0
    )
    private static int multiplyOutput2(int originalAmount) {
        Effect effect = SubathonRegistries.EFFECTS.get(Subathon.id("increase_recipe_output"));
        if (effect != null && effect.enabled) {
            return (int) (originalAmount * effect.scale * Subathon.getPoints(null));
        }
        return originalAmount;
    }
}
