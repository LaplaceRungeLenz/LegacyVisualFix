"""Check the built distribution, including the input fixes missing from early 0.7.0 jars."""
import hashlib
import json
import pathlib
import sys
import zipfile


def verify(path):
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        if any("Smoke" in name or name.startswith("com/legacyvisualfix/smoke/") for name in names):
            raise ValueError("Distribution contains smoke test classes")
        if any(name.startswith("com/legacyvisualfix/packqa/") for name in names):
            raise ValueError("Distribution contains temporary pack QA classes")
        for name in (
            "com/legacyvisualfix/reload/ModernSplashAccess.class",
            "com/legacyvisualfix/render/ModernSplashRenderer.class",
            "com/legacyvisualfix/inventory/InventoryScreenEvents.class",
            "com/legacyvisualfix/ui/UiEffects.class",
            "com/legacyvisualfix/ui/GregTechTrailColors.class",
            "com/legacyvisualfix/ui/VoltageTrailRules.class",
            "mixins.legacyvisualfix.ui.compat.json",
            "com/legacyvisualfix/combat/PendingMeleeHit.class",
            "com/legacyvisualfix/combat/client/CombatClient.class",
            "com/legacyvisualfix/combat/client/CombatParticles.class",
            "com/legacyvisualfix/combat/client/HitParticle.class",
            "com/legacyvisualfix/combat/client/CombatReactions.class",
            "com/legacyvisualfix/combat/client/WeaponRecoil.class",
            "com/legacyvisualfix/combat/FeedbackStyle.class",
            "com/legacyvisualfix/mixin/combat/MixinItemRenderer.class",
            "com/legacyvisualfix/mixin/combat/MixinRendererLivingEntity.class",
            "com/legacyvisualfix/waila/WailaBackend.class",
            "com/legacyvisualfix/waila/WdmlaAnimationRenderer.class",
            "com/legacyvisualfix/mixin/waila/wdmla/MixinRootComponent.class",
            "com/legacyvisualfix/mixin/waila/wdmla/MixinWDMlaTickHandler.class",
            "com/legacyvisualfix/mixin/waila/wdmla/MixinGuiBlockDraw.class",
            "com/legacyvisualfix/waila/TooltipContentTransform.class",
            "com/legacyvisualfix/waila/PendingTooltip.class",
        ):
            if name not in names:
                raise ValueError("Distribution is missing required fix/effects entry: " + name)
        contracts = {
            "com/legacyvisualfix/waila/WdmlaAnimationRenderer.class": b"resetIfIdle",
            "com/legacyvisualfix/inventory/InventoryMotion.class": b"openingFrom",
            "com/legacyvisualfix/mixin/inventory/MixinGuiScreen.class":
                b"com/llamalad7/mixinextras/injector/wrapoperation/WrapOperation",
        }
        for name, marker in contracts.items():
            data = jar.read(name)
            if marker not in data:
                raise ValueError("Distribution lacks inventory regression fix: " + name)
        mixins = json.loads(jar.read("mixins.legacyvisualfix.json"))
        if mixins["mixins"]:
            raise ValueError("Client-only distribution must not register common mixins")
        for name in names:
            if "/vajra/" in name or any(token in name for token in ("CombatNetwork", "CombatServer", "CombatInbox", "MixinEntityLivingBase")):
                raise ValueError("Server interaction code remains: " + name)
        for name in names:
            if name.endswith(".class") and b"cpw/mods/fml/common/network/simpleimpl" in jar.read(name):
                raise ValueError("Custom network implementation remains: " + name)
        if "combat.MixinRendererLivingEntity" not in mixins["client"]:
            raise ValueError("Model reaction must load on the client")
        if "combat.MixinItemRenderer" not in mixins["client"]:
            raise ValueError("Weapon recoil must load on the client")
        refmap = json.loads(jar.read("mixins.legacyvisualfix.refmap.json"))
        reaction_mapping = refmap["mappings"].get("com/legacyvisualfix/mixin/combat/MixinRendererLivingEntity", {})
        if not any("rotateCorpse" in entry for entry in reaction_mapping):
            raise ValueError("Model reaction lacks renderer call remapping")
        for name in names:
            if name.startswith("com/legacyvisualfix/") and name.endswith(".class"):
                if int.from_bytes(jar.read(name)[6:8], "big") != 52:
                    raise ValueError("Expected Java 8 bytecode: " + name)
        version = json.loads(jar.read("mcmod.info"))["modList"][0]["version"]
        if path.name != "legacyvisualfix-" + version + ".jar":
            raise ValueError("Filename does not match embedded version " + version)
    print("Distribution verification PASS:", path.name)
    print("SHA-256:", hashlib.sha256(path.read_bytes()).hexdigest())


if __name__ == "__main__":
    verify(pathlib.Path(sys.argv[1]))
