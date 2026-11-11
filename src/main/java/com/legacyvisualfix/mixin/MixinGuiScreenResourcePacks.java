package com.legacyvisualfix.mixin;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.client.resources.ResourcePackListEntryFound;
import net.minecraft.client.resources.ResourcePackRepository;

import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.LegacyVisualFix;
import com.legacyvisualfix.resourcepack.PackDirectoryWatcher;

@Mixin(GuiScreenResourcePacks.class)
public abstract class MixinGuiScreenResourcePacks extends GuiScreen {

    @Shadow
    public abstract List<ResourcePackListEntry> func_146964_g();

    @Shadow
    public abstract List<ResourcePackListEntry> func_146963_h();

    @Unique
    private static final int legacyvisualfix$RELOAD_DELAY_TICKS = 20;

    @Unique
    private PackDirectoryWatcher legacyvisualfix$packWatcher;

    @Unique
    private int legacyvisualfix$ticksToRefresh;

    @Inject(method = "initGui", at = @At("HEAD"))
    private void legacyvisualfix$watchPackDirectory(CallbackInfo ci) {
        if (legacyvisualfix$packWatcher != null) return;
        try {
            legacyvisualfix$packWatcher = new PackDirectoryWatcher(
                mc.getResourcePackRepository()
                    .getDirResourcepacks()
                    .toPath());
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            LegacyVisualFix.LOG.warn("Could not watch the resource pack directory", e);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (legacyvisualfix$packWatcher != null) {
            try {
                if (legacyvisualfix$packWatcher.pollForChanges()) {
                    legacyvisualfix$ticksToRefresh = legacyvisualfix$RELOAD_DELAY_TICKS;
                }
            } catch (IOException | SecurityException e) {
                LegacyVisualFix.LOG.warn("Stopped watching the resource pack directory", e);
                legacyvisualfix$closePackWatcher();
            }
        }

        if (legacyvisualfix$ticksToRefresh > 0 && --legacyvisualfix$ticksToRefresh == 0) {
            legacyvisualfix$refreshPackLists();
        }
    }

    @Override
    public void onGuiClosed() {
        legacyvisualfix$closePackWatcher();
        legacyvisualfix$ticksToRefresh = 0;
        super.onGuiClosed();
    }

    @Unique
    private void legacyvisualfix$closePackWatcher() {
        IOUtils.closeQuietly(legacyvisualfix$packWatcher);
        legacyvisualfix$packWatcher = null;
    }

    @Unique
    private void legacyvisualfix$refreshPackLists() {
        ResourcePackRepository repository = mc.getResourcePackRepository();
        repository.updateRepositoryEntriesAll();

        Map<String, ResourcePackRepository.Entry> remaining = new LinkedHashMap<>();
        for (ResourcePackRepository.Entry entry : repository.getRepositoryEntriesAll()) {
            remaining.put(entry.getResourcePackName(), entry);
        }

        List<ResourcePackListEntry> available = func_146964_g();
        // The screen's selected list includes staged changes that the repository has not received.
        legacyvisualfix$reconcilePackList(func_146963_h(), remaining);
        legacyvisualfix$reconcilePackList(available, remaining);
        for (ResourcePackRepository.Entry entry : remaining.values()) {
            available.add(new ResourcePackListEntryFound((GuiScreenResourcePacks) (Object) this, entry));
        }
    }

    @Unique
    private void legacyvisualfix$reconcilePackList(List<ResourcePackListEntry> rows,
        Map<String, ResourcePackRepository.Entry> remaining) {
        ListIterator<ResourcePackListEntry> iterator = rows.listIterator();
        while (iterator.hasNext()) {
            if (!(iterator.next() instanceof ResourcePackListEntryFound found)) continue;
            ResourcePackRepository.Entry previous = found.func_148318_i();
            ResourcePackRepository.Entry current = remaining.remove(previous.getResourcePackName());
            if (current == null) {
                iterator.remove();
            } else if (current != previous) {
                iterator.set(new ResourcePackListEntryFound((GuiScreenResourcePacks) (Object) this, current));
            }
        }
    }
}
