package com.legacyvisualfix.combat.client;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import org.lwjgl.opengl.GL11;

import com.legacyvisualfix.combat.CombatConfig;
import com.legacyvisualfix.combat.FeedbackState;
import com.legacyvisualfix.combat.FeedbackStyle;
import com.legacyvisualfix.combat.HitFeedback;
import com.legacyvisualfix.combat.PendingMeleeHit;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client-only feedback from hurt signals observed shortly after local melee attempts. */
public final class CombatClient {

    private final FeedbackState feedback = new FeedbackState();
    private final CombatParticles particles = new CombatParticles();
    private Object world, connection;
    private final boolean etFuturum = Loader.isModLoaded("etfuturum");
    private long lastSoundMs, sequence;
    private final Map<EntityLivingBase, PendingMeleeHit> pending = new IdentityHashMap<>();

    private CombatClient() {}

    public static void register() {
        CombatClient client = new CombatClient();
        FMLCommonHandler.instance()
            .bus()
            .register(client);
        MinecraftForge.EVENT_BUS.register(client);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (world != mc.theWorld || connection != mc.getNetHandler()) {
            pending.clear();
            world = mc.theWorld;
            connection = mc.getNetHandler();
            feedback.reset();
            particles.reset();
            CombatReactions.reset();
            WeaponRecoil.reset();
            lastSoundMs = 0;
        }
        long now = System.nanoTime();
        particles.tick(mc, now / 1_000_000);
        CombatReactions.tick(mc, now / 1_000_000);
        WeaponRecoil.tick(mc, now / 1_000_000);
        if (!CombatConfig.enabled || mc.thePlayer == null || mc.theWorld == null) {
            pending.clear();
            feedback.reset();
            return;
        }
        long nowMs = now / 1_000_000;
        Iterator<Map.Entry<EntityLivingBase, PendingMeleeHit>> iterator = pending.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Map.Entry<EntityLivingBase, PendingMeleeHit> entry = iterator.next();
            EntityLivingBase target = entry.getKey();
            PendingMeleeHit attempt = entry.getValue();
            if (target.worldObj != mc.theWorld || mc.theWorld.getEntityByID(target.getEntityId()) != target
                || attempt.expired(nowMs)) {
                iterator.remove();
                continue;
            }
            if (!attempt.observe(target.getHealth(), target.hurtTime, nowMs)) continue;
            iterator.remove();
            // A fixed visual weight: neither damage ownership nor absorption can be known client-side.
            HitFeedback hit = new HitFeedback(++sequence, mc.thePlayer.dimension, target.getEntityId(), 1, 0);
            if (!feedback.accept(hit, nowMs)) continue;
            particles.spawn(mc, hit, nowMs);
            CombatReactions.accept(mc, hit, nowMs);
            WeaponRecoil.accept(mc, hit, nowMs);
            if (soundEnabled() && nowMs - lastSoundMs >= 50) {
                mc.thePlayer.playSound("random.successful_hit", FeedbackStyle.soundVolume(), 1.15F);
                lastSoundMs = nowMs;
            }
        }
    }

    private boolean soundEnabled() {
        return "always".equals(CombatConfig.soundMode) || ("auto".equals(CombatConfig.soundMode) && !etFuturum);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void attack(AttackEntityEvent event) {
        // The integrated server also posts on this bus; never touch client state there.
        if (!event.entityPlayer.worldObj.isRemote) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (CombatConfig.enabled && event.entityPlayer == mc.thePlayer
            && !event.isCanceled()
            && event.target instanceof EntityLivingBase
            && event.target.worldObj == mc.theWorld) {
            EntityLivingBase target = (EntityLivingBase) event.target;
            long now = System.nanoTime() / 1_000_000;
            if (!target.isDead && (pending.size() < 64 || pending.containsKey(target))) {
                PendingMeleeHit attempt = pending.get(target);
                if (attempt == null || attempt.expired(now)) {
                    pending.put(target, new PendingMeleeHit(target.getHealth(), target.hurtTime, now));
                } else {
                    attempt.retry(now);
                }
                WeaponRecoil.attempt(mc, target, now);
            }
        }
    }

    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            long now = System.nanoTime() / 1_000_000;
            CombatReactions.frame(now);
            WeaponRecoil.frame(Minecraft.getMinecraft(), now);
        }
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS || !CombatConfig.enabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.hideGUI || mc.currentScreen != null) return;
        float alpha = feedback.alpha(System.nanoTime() / 1_000_000, FeedbackStyle.markerDuration());
        if (alpha <= 0) return;
        int x = event.resolution.getScaledWidth() / 2;
        int y = event.resolution.getScaledHeight() / 2;
        int rgb = feedback.health > 0 ? 0xFFFFFF : 0xFFD36A;
        int color = ((int) (alpha * 255) << 24) | rgb;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            if (CombatConfig.marker) {
                for (int sideX : new int[] { -1, 1 }) {
                    for (int sideY : new int[] { -1, 1 }) {
                        for (int offset = 5; offset <= 7; offset++) {
                            int px = x + sideX * offset, py = y + sideY * offset;
                            Gui.drawRect(px, py, px + 1, py + 1, color);
                        }
                    }
                }
            }
            if (CombatConfig.debug) {
                String text = "Client-observed hit";
                mc.fontRenderer.drawStringWithShadow(text, x - mc.fontRenderer.getStringWidth(text) / 2, y - 24, color);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }
}
