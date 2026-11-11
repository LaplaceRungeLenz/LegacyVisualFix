package com.legacyvisualfix.mixin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiResourcePackAvailable;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.client.resources.ResourcePackListEntryFound;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.EnumChatFormatting;

import org.apache.commons.io.IOUtils;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.legacyvisualfix.LegacyVisualFix;
import com.legacyvisualfix.resourcepack.PackDirectoryWatcher;

@Mixin(GuiScreenResourcePacks.class)
public abstract class MixinGuiScreenResourcePacks extends GuiScreen {

    @Shadow
    private GuiScreen field_146965_f;

    @Shadow
    private GuiResourcePackAvailable field_146970_i;

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

    @Unique
    private final List<ResourcePackListEntry> legacyvisualfix$visiblePacks = new ArrayList<>();

    @Unique
    private GuiTextField legacyvisualfix$packSearch;

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

    // The widget displays a filtered list while vanilla pack transfers edit the full lists.
    @ModifyArg(
        method = "initGui",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiResourcePackAvailable;<init>(Lnet/minecraft/client/Minecraft;IILjava/util/List;)V"),
        index = 3)
    private List<ResourcePackListEntry> legacyvisualfix$useFilteredList(List<ResourcePackListEntry> available) {
        return legacyvisualfix$visiblePacks;
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    private void legacyvisualfix$initPackSearch(CallbackInfo ci) {
        GuiTextField previous = legacyvisualfix$packSearch;
        int x = Math.max(4, field_146970_i.left + 5);
        legacyvisualfix$packSearch = new GuiTextField(fontRendererObj, x, 32, field_146970_i.right - x - 5, 15);
        legacyvisualfix$packSearch.setMaxStringLength(128);
        if (previous != null) {
            legacyvisualfix$packSearch.setText(previous.getText());
            legacyvisualfix$packSearch.setFocused(previous.isFocused());
        }
        field_146970_i.top = 51;
        Keyboard.enableRepeatEvents(true);
        legacyvisualfix$filterAvailablePacks();
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void legacyvisualfix$drawPackSearch(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GuiTextField search = legacyvisualfix$packSearch;
        if (search == null) return;
        search.drawTextBox();
        if (search.getText()
            .isEmpty()) {
            drawString(
                fontRendererObj,
                I18n.format("legacyvisualfix.resourcePack.search"),
                search.xPosition + 4,
                search.yPosition + 4,
                0xA0A0A0);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        GuiTextField search = legacyvisualfix$packSearch;
        String previous = search.getText();
        if (search.textboxKeyTyped(typedChar, keyCode)) {
            if (!previous.equals(search.getText())) {
                legacyvisualfix$filterAvailablePacks();
                field_146970_i.scrollBy(-field_146970_i.getAmountScrolled());
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void legacyvisualfix$focusPackSearch(int mouseX, int mouseY, int button, CallbackInfo ci) {
        legacyvisualfix$packSearch.mouseClicked(mouseX, mouseY, button);
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void legacyvisualfix$filterAfterPackTransfer(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (mc.currentScreen == (Object) this) legacyvisualfix$filterAvailablePacks();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void legacyvisualfix$skipUnchangedPackReload(GuiButton button, CallbackInfo ci) {
        if (!button.enabled || button.id != 1) return;
        List<ResourcePackRepository.Entry> applied = mc.getResourcePackRepository()
            .getRepositoryEntries();
        // The screen presents the applied priority order in reverse.
        int index = applied.size();
        for (ResourcePackListEntry row : func_146963_h()) {
            if (row instanceof ResourcePackListEntryFound found) {
                if (index == 0 || !applied.get(--index)
                    .getResourcePackName()
                    .equals(
                        found.func_148318_i()
                            .getResourcePackName()))
                    return;
            }
        }
        if (index == 0) {
            mc.displayGuiScreen(field_146965_f);
            ci.cancel();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        legacyvisualfix$packSearch.updateCursorCounter();
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
        legacyvisualfix$packSearch = null;
        Keyboard.enableRepeatEvents(false);
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
        legacyvisualfix$filterAvailablePacks();
    }

    @Unique
    private void legacyvisualfix$filterAvailablePacks() {
        String query = legacyvisualfix$packSearch.getText()
            .trim()
            .toLowerCase(Locale.ROOT);
        legacyvisualfix$visiblePacks.clear();
        for (ResourcePackListEntry row : func_146964_g()) {
            if (query.isEmpty()) {
                legacyvisualfix$visiblePacks.add(row);
            } else if (row instanceof ResourcePackListEntryFound found) {
                ResourcePackRepository.Entry entry = found.func_148318_i();
                if (entry.getResourcePackName()
                    .toLowerCase(Locale.ROOT)
                    .contains(query)
                    || EnumChatFormatting.getTextWithoutFormattingCodes(entry.getTexturePackDescription())
                        .toLowerCase(Locale.ROOT)
                        .contains(query)) {
                    legacyvisualfix$visiblePacks.add(row);
                }
            }
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
