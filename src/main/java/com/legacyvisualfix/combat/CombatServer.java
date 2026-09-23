package com.legacyvisualfix.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;

/** Observes virtual damageEntity dispatch, including mod overrides which are reached through it. */
public final class CombatServer {

    private static final ThreadLocal<DamageTransactions> LEDGER = ThreadLocal.withInitial(DamageTransactions::new);

    private CombatServer() {}

    public static DamageTransactions.Scope begin(EntityLivingBase target, DamageSource source) {
        if (!CombatConfig.enabled || target.worldObj.isRemote) return null;
        DamageTransactions ledger = LEDGER.get();
        if (!directPlayer(source) && !ledger.active()) {
            LEDGER.remove();
            return null;
        }
        return ledger.begin(target, target.getHealth(), target.getAbsorptionAmount());
    }

    public static void finish(EntityLivingBase target, DamageSource source, DamageTransactions.Scope scope,
        boolean completed) {
        if (scope == null) return;
        DamageTransactions ledger = LEDGER.get();
        DamageTransactions.Result result = ledger
            .finish(scope, target.getHealth(), target.getAbsorptionAmount(), completed);
        if (!ledger.active()) LEDGER.remove();
        if (!result.hasDamage() || !directPlayer(source)) return;
        EntityPlayerMP attacker = (EntityPlayerMP) source.getEntity();
        MinecraftForge.EVENT_BUS.post(new CombatHitEvent(attacker, target, result.health, result.absorbed));
        CombatNetwork.send(attacker, target, result);
    }

    private static boolean directPlayer(DamageSource source) {
        return source != null && "player".equals(source.damageType)
            && !source.isProjectile()
            && source.getEntity() instanceof EntityPlayerMP
            && !(source.getEntity() instanceof FakePlayer)
            && source.getSourceOfDamage() == source.getEntity();
    }
}
