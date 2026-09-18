package com.legacyvisualfix.mixin.combat;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.combat.client.WeaponRecoil;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    // Forge's four-argument overload has no SRG mapping. Inject after its own matrix push:
    // vanilla/Forge custom item renderers share this scope, including all texture passes.
    @Inject(
        method = "renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glPushMatrix()V",
            ordinal = 0,
            shift = At.Shift.AFTER,
            remap = false),
        remap = false,
        require = 0,
        allow = 1)
    private void legacyvisualfix$weaponRecoil(EntityLivingBase owner, ItemStack stack, int pass, ItemRenderType type,
        CallbackInfo ci) {
        if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) WeaponRecoil.apply(owner, stack);
    }
}
