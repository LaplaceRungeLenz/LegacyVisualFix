package com.legacyvisualfix.mixin.compat;

import net.irisshaders.iris.api.v0.IrisApi;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.WrapWithCondition;

import cpw.mods.fml.common.Loader;
import gregtech.client.BlockOverlayRenderer;

@Pseudo
@Mixin(value = BlockOverlayRenderer.class, remap = false)
public abstract class MixinGtBlockOverlayRenderer {

    @WrapWithCondition(
        method = "drawGrid",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL20;glUseProgram(I)V"),
        require = 0,
        remap = false)
    private static boolean legacyvisualfix$switchGridProgram(int program) {
        // Let Iris draw both the grid and its markers in the same render region as the terrain.
        // GT's fixed-function fallback ignores a shader pack's render scale and viewport offset.
        return !Loader.isModLoaded("angelica") || !IrisApi.getInstance()
            .isShaderPackInUse();
    }
}
