package com.modernnh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;

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
}
