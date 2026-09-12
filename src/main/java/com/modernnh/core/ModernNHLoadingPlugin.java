package com.modernnh.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.Name("ModernNH")
@IFMLLoadingPlugin.TransformerExclusions("com.modernnh.core")
public final class ModernNHLoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.modernnh.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        // The manifest also registers this config: keep all targets in its client list.
        // Optional Angelica targets use @Pseudo and are skipped when the mod is absent.
        return Collections.emptyList();
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
