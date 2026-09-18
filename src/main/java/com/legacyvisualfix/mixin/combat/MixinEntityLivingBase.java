package com.legacyvisualfix.mixin.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.legacyvisualfix.combat.CombatServer;
import com.legacyvisualfix.combat.DamageTransactions;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

    @WrapOperation(
        method = "attackEntityFrom",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLivingBase;damageEntity(Lnet/minecraft/util/DamageSource;F)V"),
        require = 0)
    private void legacyvisualfix$observeDamage(EntityLivingBase target, DamageSource source, float amount,
        Operation<Void> original) {
        DamageTransactions.Scope scope = CombatServer.begin(target, source);
        boolean completed = false;
        try {
            original.call(target, source, amount);
            completed = true;
        } finally {
            CombatServer.finish(target, source, scope, completed);
        }
    }
}
