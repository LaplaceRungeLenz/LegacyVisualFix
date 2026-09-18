package com.legacyvisualfix.mixin.combat;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.combat.client.CombatReactions;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {

    // Inside vanilla's push/pop, after position and before body yaw. Armor and held items
    // follow the model; names and world hitboxes remain outside this transform.
    // Fail soft if a renderer overhaul replaces this call; HUD/particles still work.
    @Inject(
        method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;rotateCorpse(Lnet/minecraft/entity/EntityLivingBase;FFF)V"),
        require = 0,
        allow = 1)
    private void legacyvisualfix$confirmedReaction(EntityLivingBase entity, double x, double y, double z, float yaw,
        float partialTick, CallbackInfo ci) {
        CombatReactions.apply(entity);
    }
}
