package com.legacyvisualfix.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.eventhandler.Event;

/** Non-cancellable observation after the wrapped damage operation, on the server thread. */
public final class CombatHitEvent extends Event {

    public final EntityPlayerMP attacker;
    public final EntityLivingBase target;
    public final float health, absorbed;

    public CombatHitEvent(EntityPlayerMP attacker, EntityLivingBase target, float health, float absorbed) {
        this.attacker = attacker;
        this.target = target;
        this.health = health;
        this.absorbed = absorbed;
    }
}
