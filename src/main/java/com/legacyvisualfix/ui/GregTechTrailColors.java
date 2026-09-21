package com.legacyvisualfix.ui;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.common.blocks.ItemMachines;

/** Optional GT bridge. Loaded only with GregTech present; registry initialization is deferred until gameplay. */
final class GregTechTrailColors {

    private static final Map<Item, Map<Integer, Integer>> COMPONENT_TIERS = new IdentityHashMap<>();
    private static boolean initialized;

    private GregTechTrailColors() {}

    static int color(ItemStack stack) {
        int tier = tier(stack);
        // MAX+ is GT's error sentinel, not an actual voltage tier.
        return tier >= 0 && tier <= 14 && tier < GTValues.TIER_COLORS.length
            ? VoltageTrailRules.formatColor(GTValues.TIER_COLORS[tier])
            : -1;
    }

    private static int tier(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return -1;
        if (stack.getItem() instanceof ItemMachines) {
            int id = stack.getItemDamage();
            if (id < 0 || id >= GregTechAPI.METATILEENTITIES.length) return -1;
            IMetaTileEntity meta = GregTechAPI.METATILEENTITIES[id];
            if (meta instanceof MTETieredMachineBlock) {
                MTETieredMachineBlock machine = (MTETieredMachineBlock) meta;
                return machine.isSteampowered() ? -1 : machine.mTier;
            }
            return -1;
        }
        if (!initialized) {
            for (ItemList entry : ItemList.values()) {
                int tier = VoltageTrailRules.componentTier(entry.name());
                if (tier < 0 || !entry.hasBeenSet()) continue;
                ItemStack component = entry.get(1);
                if (component == null || component.getItem() == null) continue;
                COMPONENT_TIERS.computeIfAbsent(component.getItem(), item -> new HashMap<>())
                    .put(component.getItemDamage(), tier);
            }
            initialized = true;
        }
        Map<Integer, Integer> components = COMPONENT_TIERS.get(stack.getItem());
        if (components != null) {
            Integer tier = components.get(stack.getItemDamage());
            if (tier != null) return tier;
        }
        int circuitTier = -1;
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            int tier = VoltageTrailRules.circuitTier(OreDictionary.getOreName(oreId));
            if (tier < 0) continue;
            // Ambiguous multi-tier registrations retain their ordinary rarity color.
            if (circuitTier >= 0 && circuitTier != tier) return -1;
            circuitTier = tier;
        }
        return circuitTier;
    }
}
