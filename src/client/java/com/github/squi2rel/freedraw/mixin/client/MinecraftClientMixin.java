package com.github.squi2rel.freedraw.mixin.client;

import com.github.squi2rel.freedraw.FreeDrawClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "setWorld", at = @At("HEAD"))
    private void setWorld(ClientWorld world, CallbackInfo ci) {
        FreeDrawClient.clear();
    }
}
