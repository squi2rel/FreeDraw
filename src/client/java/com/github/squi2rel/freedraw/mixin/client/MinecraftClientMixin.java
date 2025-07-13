package com.github.squi2rel.freedraw.mixin.client;

import com.github.squi2rel.freedraw.FreeDrawClient;
import com.github.squi2rel.freedraw.brush.InputHandler;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.github.squi2rel.freedraw.FreeDrawClient.*;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "setWorld", at = @At("HEAD"))
    private void setWorld(ClientWorld world, CallbackInfo ci) {
        FreeDrawClient.clear();
    }

    @Inject(
            method = "doItemUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;isItemEnabled(Lnet/minecraft/resource/featuretoggle/FeatureSet;)Z"
            ),
            cancellable = true
    )
    private void onItemUse(CallbackInfo ci, @Local ItemStack itemStack) {
        if (InputHandler.checkItem(itemStack, brushItem, brushIdStart, brushIdEnd) || InputHandler.checkItem(itemStack, eraserItem, eraserId, eraserId)) ci.cancel();
    }
}
