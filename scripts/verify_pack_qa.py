"""Validate saved actual-pack QA observations (not a substitute for running Minecraft)."""
import pathlib
import re
import sys


def verify(folder, stage2=False, server_name="server.txt"):
    server = (folder / server_name).read_text(encoding="utf-8-sig")
    if re.search(r"^(ERROR|UNAVAILABLE|FAIL)\b", server, re.M):
        raise ValueError("Server probe contains unsuccessful cases")
    cases = re.findall(
        r"^(.+?) loss=([\d.Ee+-]+) absorbed=([\d.Ee+-]+) events=(\d+) "
        r"reported=([\d.Ee+-]+)/([\d.Ee+-]+)", server, re.M
    )
    expected_count = 42 if stage2 else 39
    if len(cases) != expected_count:
        raise ValueError(f"Expected {expected_count} observations, got {len(cases)}")
    excluded = {"environment excluded", "player arrow excluded", "FakePlayer excluded", "disabled"}
    for label, hp, absorption, events, reported_hp, reported_absorption in cases:
        hp, absorption = float(hp), float(absorption)
        expected_hp, expected_absorption = (0, 0) if label in excluded else (hp, absorption)
        if (abs(float(reported_hp) - expected_hp) > 0.0001
                or abs(float(reported_absorption) - expected_absorption) > 0.0001
                or int(events) != int(expected_hp > 0 or expected_absorption > 0)):
            raise ValueError(f"Observation mismatch: {label}")
    for marker in ("PASS enabled/disabled health absorption motion hurtTime hurtResistantTime",
                   "PASS 30 tick repeated-attack enabled/disabled trace"):
        if marker not in server:
            raise ValueError(f"Missing comparison: {marker}")
    for name in ("shader-on.txt", "shader-off.txt"):
        lines = (folder / name).read_text(encoding="utf-8-sig").splitlines()
        colors = ("white", "gold", "special", "thaumcraft", "twilight") if stage2 else ("white", "gold")
        if len(lines) != len(colors) or not all(line.startswith(color + " PASS pixel=")
                                               for color, line in zip(colors, lines)):
            raise ValueError(f"Missing successful render evidence: {name}")
    if stage2:
        for name in ("particles-shader-on.txt", "particles-shader-off.txt"):
            lines = (folder / name).read_text(encoding="utf-8-sig").splitlines()
            expected = ("network white", "network gold", "expired", "mode off", "minimal",
                        "EFR enabled auto", "EFR enabled always", "EFR disabled auto", "decreased",
                        "invisible", "missing", "invalid", "burst cap", "network special",
                        "network thaumcraft", "network twilight")
            if len(lines) != len(expected) or any(not line.startswith("PASS " + case + " ")
                                                 for line, case in zip(lines, expected)):
                raise ValueError(f"Particle checks incomplete or unsuccessful: {name}")
    print(f"PASS: {expected_count} damage observations, 2 comparisons, both shader render checks"
          + (", 32 particle observations" if stage2 else ""))


if __name__ == "__main__":
    folder = pathlib.Path(sys.argv[1])
    stage2 = "--stage2" in sys.argv[2:]
    verify(folder, stage2)
    if stage2:
        verify(folder, True, "server-shader-on.txt")
