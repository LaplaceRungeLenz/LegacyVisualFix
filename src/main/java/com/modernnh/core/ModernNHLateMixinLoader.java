package com.modernnh.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.slprime.chromatictooltipscompat.CompatConfig;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

/** Ordinary mod classes are available at the late mixin stage. */
@LateMixin
public final class ModernNHLateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.modernnh.waila.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        if (!FMLLaunchHandler.side()
            .isClient() || !loadedMods.contains("Waila")) return Collections.emptyList();
        if (loadedMods.contains("chromatictooltipscompat") && loadedMods.contains("chromatictooltips")
            && CompatConfig.wailaEnabled) {
            return Arrays.asList(
                "AccessorTooltip",
                "chromatic.MixinWailaOverlay",
                "chromatic.MixinTooltipContext",
                "chromatic.MixinSectionBox");
        }
        return Arrays.asList("AccessorTooltip", "MixinOverlayRenderer");
    }
}
