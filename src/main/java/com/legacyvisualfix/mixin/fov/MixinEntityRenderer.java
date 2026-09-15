package com.legacyvisualfix.mixin.fov;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.legacyvisualfix.fov.FovConfig;
import com.legacyvisualfix.fov.FovTransition;

/** Keep Forge's target multiplier, vanilla bounds and partial-tick interpolation intact. */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @ModifyConstant(method = "updateFovModifierHand", constant = @Constant(floatValue = 0.5F), require = 1, allow = 1)
    private float legacyvisualfix$fovCoefficient(float original) {
        return FovTransition.coefficient(FovConfig.enabled, FovConfig.transitionMs, original);
    }
}
