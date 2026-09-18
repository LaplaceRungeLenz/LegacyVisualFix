package com.legacyvisualfix.smoke;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.legacyvisualfix.combat.CombatConfig;
import com.legacyvisualfix.combat.CombatHitEvent;
import com.legacyvisualfix.combat.CombatNetwork;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;

@Mod(
    modid = "legacyvisualfixcombatsmoke",
    name = "Combat smoke",
    version = "1",
    dependencies = "required-after:legacyvisualfix")
public final class CombatSmoke {

    private final List<CombatHitEvent> hits = new ArrayList<>();
    private boolean cancel;
    private boolean nested;
    private EntityPlayerMP attacker;
    public static volatile boolean serverPassed;
    public static volatile boolean requestNetworkHit;
    public static volatile boolean networkAbsorption;
    private volatile boolean peerRegistered;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        if (event.getSide()
            .isClient()) CombatClientSmoke.register();
    }

    @SubscribeEvent
    public void confirmed(CombatHitEvent event) {
        hits.add(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void hurting(LivingHurtEvent event) {
        if (cancel) event.setCanceled(true);
        if (nested && event.source.getEntity() == attacker) {
            nested = false;
            event.entityLiving.attackEntityFrom(DamageSource.magic, event.ammount + 2);
        }
    }

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) throws Exception {
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer world = server.worldServerForDimension(0);
        attacker = new EntityPlayerMP(
            server,
            world,
            new GameProfile(UUID.randomUUID(), "CombatTest"),
            new ItemInWorldManager(world));
        attacker.setPosition(0, 70, 0);
        List<String> checks = new ArrayList<>();
        try {
            CombatConfig.enabled = true;
            EntityLivingBase target = target(world);
            hit(target, 3);
            check(hits.size() == 1 && hits.get(0).health == 3, "ordinary post-damage health", checks);
            hit(target, 3);
            check(hits.size() == 1, "invulnerability rejects equal damage", checks);
            hit(target, 5);
            check(hits.size() == 2 && hits.get(1).health == 2, "higher damage reports only excess", checks);

            hits.clear();
            target = target(world);
            target.setAbsorptionAmount(4);
            hit(target, 3);
            check(
                hits.size() == 1 && hits.get(0).health == 0 && hits.get(0).absorbed == 3,
                "absorption-only confirmation",
                checks);

            hits.clear();
            target = target(world);
            target.setCurrentItemOrArmor(3, new ItemStack(Items.diamond_chestplate));
            float before = target.getHealth();
            hit(target, 6);
            check(
                hits.size() == 1 && hits.get(0).health == before - target.getHealth() && hits.get(0).health < 6,
                "armor uses observed loss",
                checks);

            hits.clear();
            cancel = true;
            target = target(world);
            before = target.getHealth();
            hit(target, 3);
            cancel = false;
            check(hits.isEmpty() && target.getHealth() == before, "cancelled damage has no success", checks);

            hits.clear();
            target = new HalfDamageMob(world);
            target.setPosition(3, 70, 0);
            hit(target, 6);
            check(hits.size() == 1 && hits.get(0).health == 3, "mod override of virtual damageEntity", checks);

            hits.clear();
            target = new BypassMob(world);
            hit(target, 3);
            check(hits.isEmpty(), "fully custom damage bypass is not guessed", checks);

            hits.clear();
            target = target(world);
            nested = true;
            hit(target, 6);
            check(
                hits.size() == 1 && hits.get(0).health == 6 && target.getHealth() == 2,
                "nested non-player damage is not attributed to player",
                checks);

            hits.clear();
            target = target(world);
            net.minecraftforge.common.util.FakePlayer fake = net.minecraftforge.common.util.FakePlayerFactory
                .getMinecraft(world);
            target.attackEntityFrom(DamageSource.causePlayerDamage(fake), 3);
            check(hits.isEmpty(), "machine FakePlayer excluded", checks);

            hits.clear();
            target = target(world);
            target.attackEntityFrom(
                DamageSource.causeArrowDamage(new net.minecraft.entity.projectile.EntityArrow(world), attacker),
                3);
            check(hits.isEmpty(), "player projectile excluded from melee stage", checks);

            hits.clear();
            target(world).attackEntityFrom(DamageSource.onFire, 3);
            check(hits.isEmpty(), "environmental damage excluded", checks);

            hits.clear();
            EntityLivingBase enabled = target(world), disabled = target(world);
            hit(enabled, 3);
            CombatConfig.enabled = false;
            hit(disabled, 3);
            check(
                enabled.getHealth() == disabled.getHealth()
                    && enabled.getAbsorptionAmount() == disabled.getAbsorptionAmount()
                    && enabled.motionX == disabled.motionX
                    && enabled.motionY == disabled.motionY
                    && enabled.motionZ == disabled.motionZ
                    && enabled.hurtTime == disabled.hurtTime
                    && enabled.hurtResistantTime == disabled.hurtResistantTime,
                "enabled/disabled combat state equality",
                checks);
            check(hits.size() == 1, "server disable suppresses observation", checks);
            CombatConfig.enabled = true;
            checks.add("PASS: " + checks.size() + " transformed-runtime checks");
            serverPassed = true;
        } catch (Throwable failure) {
            checks.add("FAIL: " + failure);
            failure.printStackTrace();
        } finally {
            cancel = false;
            Files.write(new File("legacyvisualfix-combat-server-smoke.txt").toPath(), checks, StandardCharsets.UTF_8);
            if (server.isDedicatedServer() || !serverPassed) server.initiateShutdown();
        }
    }

    private EntityLivingBase target(World world) {
        EntityCow target = new EntityCow(world);
        target.setPosition(3, 70, 0);
        return target;
    }

    private void hit(EntityLivingBase target, float amount) {
        target.attackEntityFrom(DamageSource.causePlayerDamage(attacker), amount);
    }

    private static void check(boolean condition, String name, List<String> checks) {
        if (!condition) throw new AssertionError(name);
        checks.add("OK: " + name);
    }

    public static final class HalfDamageMob extends EntityZombie {

        public HalfDamageMob(World world) {
            super(world);
        }

        @Override
        protected void damageEntity(DamageSource source, float amount) {
            super.damageEntity(source, amount / 2);
        }

        @Override
        public int getTotalArmorValue() {
            return 0;
        }
    }

    public static final class BypassMob extends EntityZombie {

        public BypassMob(World world) {
            super(world);
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            setHealth(getHealth() - amount);
            return true;
        }
    }

    @SubscribeEvent
    public void registered(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if (event.side == Side.SERVER && event.registrations.contains(CombatNetwork.CHANNEL_NAME))
            peerRegistered = true;
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !requestNetworkHit || !peerRegistered) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        EntityLivingBase target = target(player.worldObj);
        if (networkAbsorption) target.setAbsorptionAmount(4);
        target.attackEntityFrom(DamageSource.causePlayerDamage(player), 3);
        requestNetworkHit = false;
    }
}
