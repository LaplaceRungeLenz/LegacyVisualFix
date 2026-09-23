package com.legacyvisualfix.combat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.legacyvisualfix.combat.CombatConfig;
import com.legacyvisualfix.combat.FeedbackStyle;
import com.legacyvisualfix.combat.HitFeedback;

/** Presentation only. Local attempts track held-item identity; uncertain associations are skipped. */
public final class WeaponRecoil {

    private static Entity attemptedTarget;
    private static Item item;
    private static int slot, subtype;
    private static long attemptedAt, started;
    private static int duration;
    private static float from, peak, frameAngle;
    private static boolean active;
    private static Object world, connection;

    private WeaponRecoil() {}

    public static void reset() {
        attemptedTarget = null;
        item = null;
        active = false;
        frameAngle = 0;
        world = connection = null;
    }

    private static boolean ready(Minecraft mc) {
        return CombatConfig.enabled && CombatConfig.weaponRecoil
            && mc.theWorld != null
            && mc.thePlayer != null
            && !mc.thePlayer.isDead
            && mc.thePlayer.getHealth() > 0
            && !mc.thePlayer.isInvisible()
            && mc.renderViewEntity == mc.thePlayer
            && mc.gameSettings.thirdPersonView == 0
            && mc.currentScreen == null
            && mc.thePlayer.getItemInUseCount() == 0;
    }

    private static boolean heldMatches(Minecraft mc) {
        ItemStack held = mc.thePlayer.getHeldItem();
        return item != null && held != null
            && held.stackSize > 0
            && held.getItem() == item
            && mc.thePlayer.inventory.currentItem == slot
            && (!held.getHasSubtypes() || held.getItemDamage() == subtype);
    }

    public static void tick(Minecraft mc, long now) {
        if (!ready(mc) || world != mc.theWorld || connection != mc.getNetHandler() || !heldMatches(mc)) {
            reset();
            return;
        }
        if (attemptedTarget != null && (now < attemptedAt || now - attemptedAt > 500)) attemptedTarget = null;
        if (active && (now < started || now - started >= duration)) {
            active = false;
            frameAngle = 0;
        }
    }

    public static void attempt(Minecraft mc, Entity target, long now) {
        if (!ready(mc) || target == null || target.worldObj != mc.theWorld) {
            reset();
            return;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || held.stackSize <= 0 || held.getItem() instanceof ItemMap) {
            reset();
            return;
        }
        String id = Item.itemRegistry.getNameForObject(held.getItem());
        String className = held.getItem()
            .getClass()
            .getName();
        if (FeedbackStyle.excluded(CombatConfig.recoilExcludedItems, id, className) || FeedbackStyle.excluded(
            CombatConfig.recoilExcludedItems,
            id + "@" + held.getItemDamage(),
            className + "@" + held.getItemDamage())) {
            reset();
            return;
        }
        if (world != mc.theWorld || connection != mc.getNetHandler() || !heldMatches(mc)) reset();
        world = mc.theWorld;
        connection = mc.getNetHandler();
        item = held.getItem();
        slot = mc.thePlayer.inventory.currentItem;
        subtype = held.getItemDamage();
        attemptedTarget = target;
        attemptedAt = now;
    }

    public static void accept(Minecraft mc, HitFeedback hit, long now) {
        tick(mc, now);
        if (attemptedTarget == null || hit == null
            || !hit.valid()
            || hit.dimension != mc.thePlayer.dimension
            || attemptedTarget.getEntityId() != hit.targetId
            || mc.theWorld.getEntityByID(hit.targetId) != attemptedTarget) return;
        // At most one recoil per local attempt, even if a weapon reports several damage results.
        attemptedTarget = null;
        float degrees = FeedbackStyle.recoilDegrees();
        if (!Float.isFinite(degrees) || degrees <= 0) return;
        from = angle(now);
        peak = Math.min(5F, degrees);
        from = Math.min(from, peak);
        duration = Math.max(60, Math.min(240, FeedbackStyle.recoilDuration()));
        started = now;
        active = true;
    }

    private static float angle(long now) {
        if (!active || now < started || now - started >= duration) return 0;
        long age = now - started;
        if (age < 15) return from + (peak - from) * smooth(age / 15F);
        return peak * (1F - smooth((age - 15) / (float) (duration - 15)));
    }

    private static float smooth(float t) {
        return t * t * (3F - 2F * t);
    }

    public static void frame(Minecraft mc, long now) {
        tick(mc, now);
        // A single sample for every texture/foil pass in this frame.
        frameAngle = angle(now);
    }

    public static void apply(EntityLivingBase owner, ItemStack rendered) {
        Minecraft mc = Minecraft.getMinecraft();
        if (frameAngle <= 0 || !ready(mc)
            || world != mc.theWorld
            || connection != mc.getNetHandler()
            || owner != mc.thePlayer
            || !heldMatches(mc)
            // Avoid animating a departing equip model, inventory preview or offhand copy.
            || rendered != mc.thePlayer.getHeldItem()) return;
        GL11.glRotatef(-frameAngle, 1F, 0F, 0F);
    }
}
