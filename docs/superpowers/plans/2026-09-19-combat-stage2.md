# Combat feedback stage 2: confirmed local impact particles

User approved continuation and direct actual-instance testing after stage 1.
Execute on the existing dedicated `codex/combat-feedback` checkout. Scope is
bounded to the previously proposed short impact particles; no model or physics
changes. The research document remains the broader design reference.

## Implementation

- [x] Add client-local white HP / gold absorption-only particles after accepted,
  deduplicated protocol-v1 messages. Reuse target ID; do not change the wire format.
- [x] Resolve the current client entity; skip missing, invisible, invalid or distant
  targets. Placement is an approximate facing surface, not a reported contact point.
- [x] Small, short-lived particles; per-hit and global limits independent of damage.
  Respect Minimal/Decreased particle settings. No world RNG or entity state writes.
- [x] Auto mode yields to EFR damage particles when enabled; unknown compatibility
  state suppresses particles. Explicit always/off options; no changes to EFR files.
- [x] Test budget, invalid inputs and setting policies; independently review code.

## Runtime and delivery

- [x] Extend the disposable actual-pack QA addon to observe particle generation,
  rendering, color, expiry and suppression, retaining stage-1 damage regression.
- [x] Build a normal stage-2 JAR, isolate QA classes, verify release contents.
- [x] Install in the user's existing GTNH test instance, retaining the previous JAR.
  Launch via Prism UI and use only the disposable QA world.
- [x] Run with Photon on/off, inspect screenshots and logs, fix observed bugs.
- [x] Remove temporary QA, restore user shader/settings, leave stage-2 JAR active.
- [ ] Publish source on development branch and stage prerelease JAR with checksum;
  report actual compatibility samples, limitations and manual test instructions.
- [ ] Stop after this stage for user feedback.

Ruling: Keep protocol v1 unchanged. Missing client targets get the HUD confirmation
but no guessed world particle; this avoids a wire migration and false positions.
