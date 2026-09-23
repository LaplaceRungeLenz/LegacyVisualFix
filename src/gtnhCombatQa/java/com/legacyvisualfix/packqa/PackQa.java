package com.legacyvisualfix.packqa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.legacyvisualfix.combat.CombatConfig;
import com.legacyvisualfix.combat.CombatHitEvent;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Temporary test mod. Only runs in the explicitly named disposable QA world. */
@Mod(modid = "lvfpackqa", name = "LVF Pack QA", version = "1", dependencies = "required-after:legacyvisualfix")
public final class PackQa {

    private final List<CombatHitEvent> hits = new ArrayList<>();
    private final List<String> report = new ArrayList<>();
    private boolean cancel, ran;
    private int ticks;
    private EntityLivingBase active;
    private EntityCow comboTarget;
    private final List<String> enabledCombo = new ArrayList<>();
    private final List<String> disabledCombo = new ArrayList<>();
    public static volatile String capture = "";
    public static volatile long captureAt;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        if (event.getSide()
            .isClient()) PackQaClient.register();
    }

    @SubscribeEvent
    public void hit(CombatHitEvent event) {
        hits.add(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void hurt(LivingHurtEvent event) {
        if (cancel && event.entityLiving == active) event.setCanceled(true);
    }

    @SubscribeEvent
    public void tick(TickEvent.PlayerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        World world = player.worldObj;
        if (!world.getWorldInfo()
            .getWorldName()
            .contains("LVF Combat Stage1 QA")) return;
        ++ticks;
        if (!ran && ticks >= 100) {
            ran = true;
            // Remove previous QA targets only inside the explicitly disposable world.
            for (Object entity : new ArrayList<>(world.loadedEntityList)) {
                if (entity instanceof EntityLivingBase
                    && !(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
                    ((EntityLivingBase) entity).setDead();
                }
            }
            batch(player);
            for (int x = -6; x <= 6; x++) for (int z = -6; z <= 8; z++) world.setBlock(x, 239, z, Blocks.stone);
            world.getGameRules()
                .setOrCreateGameRule("doDaylightCycle", "false");
            world.getGameRules()
                .setOrCreateGameRule("doMobSpawning", "false");
            world.setWorldTime(6000);
            player.setPositionAndUpdate(0.5, 240, 0.5);
            player.playerNetServerHandler.setPlayerLocation(0.5, 240, 0.5, 0, 18);
            player.motionX = player.motionY = player.motionZ = 0;
            player.inventory.currentItem = 1;
            player.inventory.mainInventory[1] = null;
        }
        if (ticks == 200 || ticks == 260) {
            EntityCow cow = new EntityCow(world);
            cow.setLocationAndAngles(player.posX, player.posY, player.posZ + 2, 180, 0);
            cow.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                .setBaseValue(0);
            if (ticks == 260) cow.setAbsorptionAmount(4);
            world.spawnEntityInWorld(cow);
            active = cow;
        }
        if (ticks == 205 || ticks == 265) {
            hits.clear();
            float before = active.getHealth(), absorption = active.getAbsorptionAmount();
            player.attackTargetEntityWithCurrentItem(active);
            record(ticks == 205 ? "NETWORK_WHITE" : "NETWORK_GOLD", before, absorption, active);
            captureAt = System.nanoTime();
            capture = ticks == 205 ? "white" : "gold";
            save();
        }
        if (ticks == 240 || ticks == 300) {
            if (active != null) active.setDead();
        }
        if (ticks == 320) {
            EntityCow cow = new EntityCow(world);
            cow.setLocationAndAngles(player.posX, player.posY, player.posZ + 2, 180, 0);
            cow.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                .setBaseValue(0);
            cow.setCustomNameTag("LVF manual target");
            cow.setAbsorptionAmount(4);
            world.spawnEntityInWorld(cow);
        }
        if (ticks == 340 || ticks == 400) {
            comboTarget = new EntityCow(world);
            comboTarget.getEntityAttribute(SharedMonsterAttributes.maxHealth)
                .setBaseValue(100);
            comboTarget.setHealth(100);
        }
        if ((ticks >= 340 && ticks < 370) || (ticks >= 400 && ticks < 430)) {
            boolean on = ticks < 400;
            boolean previous = CombatConfig.enabled;
            CombatConfig.enabled = on;
            try {
                comboTarget.setPosition(player.posX, player.posY, player.posZ + 2);
                if (comboTarget.hurtResistantTime > 0) --comboTarget.hurtResistantTime;
                if (comboTarget.hurtTime > 0) --comboTarget.hurtTime;
                hits.clear();
                player.attackTargetEntityWithCurrentItem(comboTarget);
                (on ? enabledCombo : disabledCombo).add(snapshot(comboTarget));
            } finally {
                CombatConfig.enabled = previous;
            }
        }
        if (ticks == 430) {
            report.add(
                (enabledCombo.equals(disabledCombo) ? "PASS" : "FAIL")
                    + " 30 tick repeated-attack enabled/disabled trace");
            report.add("combo enabled=" + enabledCombo);
            report.add("combo disabled=" + disabledCombo);
            save();
        }
        if (ticks == 460 || ticks == 520 || ticks == 580) {
            for (Object entity : new ArrayList<>(world.loadedEntityList)) {
                if (entity instanceof EntityLivingBase
                    && !(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
                    ((EntityLivingBase) entity).setDead();
                }
            }
            player.setPositionAndUpdate(0.5, 240, 0.5);
            player.playerNetServerHandler.setPlayerLocation(0.5, 240, 0.5, 0, 18);
            player.motionX = player.motionY = player.motionZ = 0;
            world.setWorldTime(18000);
            String id = ticks == 460 ? "SpecialMobs.BrutishZombie"
                : ticks == 520 ? "Thaumcraft.BrainyZombie" : "TwilightForest.Helmet Crab";
            active = (EntityLivingBase) EntityList.createEntityByName(id, world);
            active.setLocationAndAngles(player.posX, player.posY, player.posZ + 2, 180, 0);
            active.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                .setBaseValue(0);
            world.spawnEntityInWorld(active);
        }
        if (ticks == 465 || ticks == 525 || ticks == 585) {
            hits.clear();
            float before = active.getHealth(), absorption = active.getAbsorptionAmount();
            player.attackTargetEntityWithCurrentItem(active);
            String name = ticks == 465 ? "special" : ticks == 525 ? "thaumcraft" : "twilight";
            record("NETWORK_" + name, before, absorption, active);
            captureAt = System.nanoTime();
            capture = name;
            save();
        }
        if (ticks == 500 || ticks == 560 || ticks == 620) {
            if (active != null) active.setDead();
        }
    }

    private void batch(EntityPlayerMP player) throws Exception {
        World world = player.worldObj;
        report.add("Actual GTNH instance; player melee path; Java " + System.getProperty("java.version"));
        ItemStack previous = player.inventory.getCurrentItem();
        boolean enabled = CombatConfig.enabled;
        net.minecraft.world.WorldSettings.GameType oldMode = player.theItemInWorldManager.getGameType();
        try {
            CombatConfig.enabled = true;
            player.theItemInWorldManager.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
            equip(player, new ItemStack(Items.iron_sword));
            String[] ids = { "Cow", "Zombie", "Skeleton", "SpecialMobs.SpecialZombie", "SpecialMobs.BrutishZombie",
                "SpecialMobs.GiantZombie", "SpecialMobs.FireZombie", "SpecialMobs.PlagueZombie",
                "SpecialMobs.BrutishSkeleton", "SpecialMobs.VampirePigZombie", "Thaumcraft.BrainyZombie",
                "Thaumcraft.GiantBrainyZombie", "Thaumcraft.CultistKnight", "Thaumcraft.Wisp", "enderzoo.Enderminy",
                "enderzoo.FallenKnight", "TwilightForest.Swarm Spider", "TwilightForest.Minotaur",
                "TwilightForest.Helmet Crab", "TwilightForest.Naga", "TwilightForest.Twilight Lich" };
            for (String id : ids) {
                try {
                    Object entity = EntityList.createEntityByName(id, world);
                    if (!(entity instanceof EntityLivingBase)) {
                        report.add("UNAVAILABLE " + id);
                        continue;
                    }
                    active = (EntityLivingBase) entity;
                    active.setPosition(player.posX, player.posY, player.posZ + 2);
                    hits.clear();
                    float hp = active.getHealth(), abs = active.getAbsorptionAmount();
                    player.attackTargetEntityWithCurrentItem(active);
                    record(
                        id + " ["
                            + active.getClass()
                                .getName()
                            + "]",
                        hp,
                        abs,
                        active);
                } catch (Throwable t) {
                    report.add("ERROR " + id + " " + t);
                }
            }
            for (String modifier : new String[] { "Bulwark", "Vengeance", "Regen", "1UP" }) {
                try {
                    active = new EntityZombie(world);
                    Class<?> core = Class.forName("atomicstryker.infernalmobs.common.InfernalMobsCore");
                    Class<?> base = Class.forName("atomicstryker.infernalmobs.common.modifiers.MobModifier");
                    Object mod = Class.forName("atomicstryker.infernalmobs.common.modifiers.MM_" + modifier)
                        .getConstructor(base)
                        .newInstance(new Object[] { null });
                    Object instance = core.getMethod("instance")
                        .invoke(null);
                    core.getMethod("addEntityModifiers", EntityLivingBase.class, base, boolean.class)
                        .invoke(instance, active, mod, true);
                    report.add(
                        "Infernal attached=" + core.getMethod("getMobModifiers", EntityLivingBase.class)
                            .invoke(null, active));
                    observe("Infernal " + modifier, player, false);
                    core.getMethod("removeEntFromElites", EntityLivingBase.class)
                        .invoke(null, active);
                } catch (Throwable error) {
                    report.add("ERROR Infernal " + modifier + " " + error);
                }
            }
            try {
                Class<?> materials = Class.forName("gregtech.api.enums.Materials");
                Class<?> tools = Class.forName("gregtech.common.items.MetaGeneratedTool01");
                Object tool = tools.getField("INSTANCE")
                    .get(null);
                Class<?> toolIds = Class.forName("gregtech.common.items.IDMetaTool01");
                int id = toolIds.getField("ID")
                    .getInt(
                        toolIds.getField("KNIFE")
                            .get(null));
                ItemStack sword = (ItemStack) tools
                    .getMethod("getToolWithStats", int.class, int.class, materials, materials, long[].class)
                    .invoke(
                        tool,
                        id,
                        1,
                        materials.getField("Steel")
                            .get(null),
                        materials.getField("Wood")
                            .get(null),
                        null);
                equip(player, sword);
                active = new EntityZombie(world);
                observe("GT steel knife", player, false);
                report.add("GT weapon=" + sword);
            } catch (Throwable error) {
                report.add("ERROR GT weapon " + error);
            }
            try {
                Class<?> tools = Class.forName("tconstruct.tools.TinkerTools");
                Class<?> builder = Class.forName("tconstruct.library.crafting.ToolBuilder");
                ItemStack blade = new ItemStack(
                    (Item) tools.getField("swordBlade")
                        .get(null),
                    1,
                    2);
                ItemStack rod = new ItemStack(
                    (Item) tools.getField("toolRod")
                        .get(null),
                    1,
                    2);
                ItemStack guard = new ItemStack(
                    (Item) tools.getField("wideGuard")
                        .get(null),
                    1,
                    2);
                ItemStack sword = (ItemStack) builder
                    .getMethod("buildTool", ItemStack.class, ItemStack.class, ItemStack.class, String.class)
                    .invoke(
                        builder.getField("instance")
                            .get(null),
                        blade,
                        rod,
                        guard,
                        "LVF QA iron sword");
                if (sword == null) throw new IllegalStateException("ToolBuilder returned null");
                equip(player, sword);
                active = new EntityZombie(world);
                observe("TiC iron broadsword", player, false);
                report.add("TiC weapon=" + sword + " nbt=" + sword.getTagCompound());
            } catch (Throwable error) {
                report.add("ERROR TiC weapon " + error);
            }
            equip(player, null);
            active = new EntityCow(world);
            active.setAbsorptionAmount(4);
            observe("absorption-only", player, false);
            active = new EntityCow(world);
            NBTTagCompound nbt = new NBTTagCompound();
            active.writeToNBT(nbt);
            nbt.setBoolean("Invulnerable", true);
            active.readFromNBT(nbt);
            observe("invulnerable", player, false);
            active = new EntityCow(world);
            cancel = true;
            observe("Forge cancellation", player, false);
            cancel = false;
            active = new EntityCow(world);
            observe("combo first", player, false);
            observe("combo same-tick repeat", player, false);
            active = new EntityCow(world);
            observe("environment excluded", player, true);
            active = new EntityCow(world);
            hits.clear();
            active.attackEntityFrom(
                DamageSource
                    .causeArrowDamage(new net.minecraft.entity.projectile.EntityArrow(world, player, 1), player),
                3);
            record("player arrow excluded", 10, 0, active);
            active = new EntityCow(world);
            hits.clear();
            active.attackEntityFrom(
                DamageSource.causePlayerDamage(
                    net.minecraftforge.common.util.FakePlayerFactory
                        .getMinecraft((net.minecraft.world.WorldServer) world)),
                3);
            record("FakePlayer excluded", 10, 0, active);
            active = new EntityCow(world);
            CombatConfig.enabled = false;
            observe("disabled", player, false);
            String disabledState = snapshot(active);
            CombatConfig.enabled = true;
            active = new EntityCow(world);
            observe("enabled comparison", player, false);
            report.add(
                (disabledState.equals(snapshot(active)) ? "PASS" : "FAIL")
                    + " enabled/disabled health absorption motion hurtTime hurtResistantTime "
                    + snapshot(active));
        } finally {
            CombatConfig.enabled = enabled;
            cancel = false;
            equip(player, previous);
            player.theItemInWorldManager.setGameType(oldMode);
            save();
        }
    }

    private void observe(String label, EntityPlayerMP player, boolean environment) {
        active.setPosition(player.posX, player.posY, player.posZ + 2);
        active.getRNG()
            .setSeed(42);
        hits.clear();
        float hp = active.getHealth(), abs = active.getAbsorptionAmount();
        if (environment) active.attackEntityFrom(DamageSource.fall, 3);
        else player.attackTargetEntityWithCurrentItem(active);
        record(label, hp, abs, active);
    }

    private void equip(EntityPlayerMP player, ItemStack stack) {
        ItemStack old = player.getHeldItem();
        if (old != null) player.getAttributeMap()
            .removeAttributeModifiers(old.getAttributeModifiers());
        player.inventory.mainInventory[player.inventory.currentItem] = stack;
        if (stack != null) player.getAttributeMap()
            .applyAttributeModifiers(stack.getAttributeModifiers());
    }

    private String snapshot(EntityLivingBase entity) {
        return entity.getHealth() + "/"
            + entity.getAbsorptionAmount()
            + "/"
            + entity.motionX
            + "/"
            + entity.motionY
            + "/"
            + entity.motionZ
            + "/"
            + entity.hurtTime
            + "/"
            + entity.hurtResistantTime;
    }

    private void record(String label, float hp, float abs, EntityLivingBase target) {
        float sumHp = 0, sumAbs = 0;
        for (CombatHitEvent hit : hits) {
            sumHp += hit.health;
            sumAbs += hit.absorbed;
        }
        report.add(
            label + " loss="
                + (hp - target.getHealth())
                + " absorbed="
                + (abs - target.getAbsorptionAmount())
                + " events="
                + hits.size()
                + " reported="
                + sumHp
                + "/"
                + sumAbs
                + " motion="
                + target.motionX
                + "/"
                + target.motionY
                + "/"
                + target.motionZ);
    }

    private void save() throws Exception {
        Files.write(new File("lvf-pack-qa-server.txt").toPath(), report, StandardCharsets.UTF_8);
    }
}
