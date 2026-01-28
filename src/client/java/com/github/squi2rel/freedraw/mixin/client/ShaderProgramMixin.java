package com.github.squi2rel.freedraw.mixin.client;

import com.github.squi2rel.freedraw.render.PathRenderer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gl.ShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ShaderProgram.class, priority = Integer.MIN_VALUE)
public class ShaderProgramMixin {
    @ModifyExpressionValue(
            method = "initializeUniforms",
            at = @At(value = "CONSTANT", args = "intValue=12")
    )
    private int lessTextures(int original) {
        if (PathRenderer.drawing) return 1;
        return original;
    }
}
