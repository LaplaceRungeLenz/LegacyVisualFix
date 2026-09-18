"""Check the built distribution, including the input fixes missing from early 0.7.0 jars."""
import hashlib
import json
import pathlib
import sys
import zipfile


def verify(path):
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        if any("Smoke" in name for name in names):
            raise ValueError("Distribution contains smoke test classes")
        if any(name.startswith("com/legacyvisualfix/packqa/") for name in names):
            raise ValueError("Distribution contains temporary pack QA classes")
        for name in (
            "com/legacyvisualfix/inventory/InventoryScreenEvents.class",
            "com/legacyvisualfix/ui/UiEffects.class",
            "mixins.legacyvisualfix.ui.compat.json",
            "com/legacyvisualfix/combat/CombatServer.class",
            "com/legacyvisualfix/combat/client/CombatClient.class",
            "com/legacyvisualfix/mixin/combat/MixinEntityLivingBase.class",
        ):
            if name not in names:
                raise ValueError("Distribution is missing required fix/effects entry: " + name)
        contracts = {
            "com/legacyvisualfix/inventory/InventoryMotion.class": b"openingFrom",
            "com/legacyvisualfix/mixin/inventory/MixinGuiScreen.class":
                b"com/llamalad7/mixinextras/injector/wrapoperation/WrapOperation",
        }
        for name, marker in contracts.items():
            data = jar.read(name)
            if marker not in data:
                raise ValueError("Distribution lacks inventory regression fix: " + name)
        mixins = json.loads(jar.read("mixins.legacyvisualfix.json"))
        if "combat.MixinEntityLivingBase" not in mixins["mixins"]:
            raise ValueError("Combat observation must load on both logical sides")
        refmap = json.loads(jar.read("mixins.legacyvisualfix.refmap.json"))
        combat_mapping = refmap["mappings"].get("com/legacyvisualfix/mixin/combat/MixinEntityLivingBase", {})
        if not any("damageEntity" in entry for entry in combat_mapping):
            raise ValueError("Combat mixin lacks damage method remapping")
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
