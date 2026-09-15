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
        for name in (
            "com/modernnh/inventory/InventoryScreenEvents.class",
            "com/modernnh/ui/UiEffects.class",
            "mixins.modernnh.ui.compat.json",
        ):
            if name not in names:
                raise ValueError("Distribution is missing required fix/effects entry: " + name)
        contracts = {
            "com/modernnh/inventory/InventoryMotion.class": b"openingFrom",
            "com/modernnh/mixin/inventory/MixinGuiScreen.class":
                b"com/llamalad7/mixinextras/injector/wrapoperation/WrapOperation",
        }
        for name, marker in contracts.items():
            data = jar.read(name)
            if marker not in data:
                raise ValueError("Distribution lacks inventory regression fix: " + name)
        for name in names:
            if name.startswith("com/modernnh/") and name.endswith(".class"):
                if int.from_bytes(jar.read(name)[6:8], "big") != 52:
                    raise ValueError("Expected Java 8 bytecode: " + name)
        version = json.loads(jar.read("mcmod.info"))["modList"][0]["version"]
        if path.name != "modernnh-" + version + ".jar":
            raise ValueError("Filename does not match embedded version " + version)
    print("Distribution verification PASS:", path.name)
    print("SHA-256:", hashlib.sha256(path.read_bytes()).hexdigest())


if __name__ == "__main__":
    verify(pathlib.Path(sys.argv[1]))
