package com.legacyvisualfix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacyvisualfix.combat.CombatConfig;
import com.legacyvisualfix.combat.client.CombatClient;
import com.legacyvisualfix.fov.FovConfig;
import com.legacyvisualfix.inventory.InventoryAnimationConfig;
import com.legacyvisualfix.inventory.InventoryScreenEvents;
import com.legacyvisualfix.ui.UiEffects;
import com.legacyvisualfix.ui.UiEffectsConfig;
import com.legacyvisualfix.waila.WailaAnimationConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = LegacyVisualFix.MODID,
    name = "LegacyVisualFix",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*",
    dependencies = "required-after:unimixins@[0.2.1,)")
public final class LegacyVisualFix {

    public static final String MODID = "legacyvisualfix";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (!event.getSide()
            .isClient()) return;
        CombatConfig.load(event.getModConfigurationDirectory());
        CombatClient.register();
        UiEffectsConfig.load(event.getModConfigurationDirectory());
        UiEffects.register();
        InventoryAnimationConfig.load(event.getModConfigurationDirectory());
        InventoryScreenEvents.register();
        FovConfig.load(event.getModConfigurationDirectory());
        if (Loader.isModLoaded("Waila") || Loader.isModLoaded("wdmla")) {
            WailaAnimationConfig.load(event.getModConfigurationDirectory());
        }
    }
}
