package com.legacyvisualfix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacyvisualfix.fov.FovConfig;
import com.legacyvisualfix.inventory.InventoryAnimationConfig;
import com.legacyvisualfix.inventory.InventoryScreenEvents;
import com.legacyvisualfix.ui.UiEffects;
import com.legacyvisualfix.ui.UiEffectsConfig;
import com.legacyvisualfix.vajra.VajraConfig;
import com.legacyvisualfix.vajra.VajraNetwork;
import com.legacyvisualfix.vajra.client.VajraClient;
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
        if (event.getSide()
            .isClient()) {
            UiEffectsConfig.load(event.getModConfigurationDirectory());
            UiEffects.register();
        }
        if (Loader.isModLoaded("gregtech") && Loader.isModLoaded("IC2") && Loader.isModLoaded("appliedenergistics2")) {
            VajraConfig.load(event.getModConfigurationDirectory());
            if (VajraConfig.enabled) {
                VajraNetwork.register();
                if (event.getSide()
                    .isClient()) VajraClient.register();
            }
        }
        if (event.getSide()
            .isClient()) {
            InventoryAnimationConfig.load(event.getModConfigurationDirectory());
            InventoryScreenEvents.register();
        }
        if (event.getSide()
            .isClient()) FovConfig.load(event.getModConfigurationDirectory());
        if (event.getSide()
            .isClient() && Loader.isModLoaded("Waila")) {
            WailaAnimationConfig.load(event.getModConfigurationDirectory());
        }
    }
}
