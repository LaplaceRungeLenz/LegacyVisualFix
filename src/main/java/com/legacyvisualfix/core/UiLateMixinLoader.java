package com.legacyvisualfix.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

/** Only names are resolved here: optional client APIs remain unloaded on servers. */
@LateMixin
public final class UiLateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.legacyvisualfix.ui.compat.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        if (!FMLLaunchHandler.side()
            .isClient()) return Collections.emptyList();
        List<String> mixins = new ArrayList<>();
        if (loadedMods.contains("NotEnoughItems")) mixins.add("MixinNeiSlotItems");
        if (loadedMods.contains("modularui")) {
            mixins.add("MixinMui1SlotWidget");
            mixins.add("MixinMui1Gui");
        }
        if (loadedMods.contains("modularui2")) {
            mixins.add("MixinMui2ItemSlot");
            mixins.add("MixinMui2ScreenHandler");
        }
        return mixins;
    }
}
