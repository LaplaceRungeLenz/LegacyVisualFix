package com.legacyvisualfix.combat.client;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.legacyvisualfix.combat.CombatConfig;
import com.legacyvisualfix.combat.CombatInbox;
import com.legacyvisualfix.combat.FeedbackState;
import com.legacyvisualfix.combat.HitFeedbackMessage;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Server-confirmed feedback is presented only to the attacking client. */
public final class CombatClient {

    private final FeedbackState feedback = new FeedbackState();
    private final CombatParticles particles = new CombatParticles();
    private Object world, connection;
    private final boolean etFuturum = Loader.isModLoaded("etfuturum");
    private long lastSoundMs;

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
            CombatInbox.clear();
            world = mc.theWorld;
            connection = mc.getNetHandler();
            feedback.reset();
            particles.reset();
            lastSoundMs = 0;
        }
        long now = System.nanoTime();
        CombatInbox.Entry entry;
        for (int i = 0; i < 256 && (entry = CombatInbox.poll()) != null; i++) {
            if (!CombatConfig.enabled || mc.thePlayer == null
                || mc.theWorld == null
                || !entry.matches(connection, mc.thePlayer.dimension, now)) continue;
            HitFeedbackMessage hit = entry.message;
            long nowMs = now / 1_000_000;
            if (!feedback.accept(hit, nowMs)) continue;
            particles.spawn(mc, hit, nowMs);
            if (soundEnabled() && nowMs - lastSoundMs >= 50) {
                // Rate-limit sound only, never attacks or confirmed results.
                mc.thePlayer
                    .playSound("random.successful_hit", CombatConfig.soundVolume, hit.health > 0 ? 1.15F : 0.8F);
                lastSoundMs = nowMs;
            }
        }
        if (!CombatConfig.enabled || mc.thePlayer == null) feedback.reset();
    }

    private boolean soundEnabled() {
        return "always".equals(CombatConfig.soundMode) || ("auto".equals(CombatConfig.soundMode) && !etFuturum);
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS || !CombatConfig.enabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.hideGUI || mc.currentScreen != null) return;
        float alpha = feedback.alpha(System.nanoTime() / 1_000_000, CombatConfig.durationMs);
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
                String text = String.format(Locale.ROOT, "HP -%.2f | ABS -%.2f", feedback.health, feedback.absorbed);
                mc.fontRenderer.drawStringWithShadow(text, x - mc.fontRenderer.getStringWidth(text) / 2, y - 24, color);
            }
        } finally {
            GL11.glPopAttrib();
        }
    }
}
