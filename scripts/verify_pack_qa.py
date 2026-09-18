"""Validate saved actual-pack QA observations (not a substitute for running Minecraft)."""
import pathlib
import re
import sys


def verify(folder):
    server = (folder / "server.txt").read_text(encoding="utf-8-sig")
    if re.search(r"^(ERROR|UNAVAILABLE|FAIL)\b", server, re.M):
        raise ValueError("Server probe contains unsuccessful cases")
    cases = re.findall(
        r"^(.+?) loss=([\d.Ee+-]+) absorbed=([\d.Ee+-]+) events=(\d+) "
        r"reported=([\d.Ee+-]+)/([\d.Ee+-]+)", server, re.M
    )
    if len(cases) != 39:
        raise ValueError(f"Expected 39 observations, got {len(cases)}")
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
        if len(lines) != 2 or not all(line.startswith(color + " PASS pixel=")
                                      for color, line in zip(("white", "gold"), lines)):
            raise ValueError(f"Missing successful render evidence: {name}")
    print("PASS: 39 damage observations, 2 comparisons, 4 render checks")


if __name__ == "__main__":
    verify(pathlib.Path(sys.argv[1]))
