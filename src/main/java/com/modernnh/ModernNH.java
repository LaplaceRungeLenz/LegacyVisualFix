package com.modernnh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.modernnh.waila.WailaAnimationConfig;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = ModernNH.MODID,
    name = "ModernNH",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*",
    dependencies = "required-after:unimixins@[0.2.1,)")
public final class ModernNH {

    public static final String MODID = "modernnh";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide()
            .isClient() && Loader.isModLoaded("Waila")) {
            WailaAnimationConfig.load(event.getModConfigurationDirectory());
        }
    }
}
