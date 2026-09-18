# Combat feedback stage 1 implementation plan

> Execute this plan inline using executing-plans and test-driven-development. Request an independent code review before delivery.

**Goal:** Ship only stage 1: server-confirmed direct-player melee feedback, with a short client hit marker and optional sound, without changing combat simulation.

**Architecture:** Wrap the virtual damageEntity calls inside EntityLivingBase.attackEntityFrom. Observe health/absorption deltas around the original operation, subtract nested transactions, and send a versioned result only to the attacking client advertising the protocol channel. Client drains a bounded inbox on its tick, rejects stale sessions/duplicate sequences, and renders a fading marker. Existing EFR sounds remain the owner in auto sound mode.

**Tech stack:** Forge 1.7.10, existing UniMixins/MixinExtras, Java 8 bytecode, JUnit 4, FML SimpleNetworkWrapper.

**Spec:** ../../combat-feedback-research-2026-09-18.md and the user's staged-delivery request.

## Constraints

- No cooldown, damage, invulnerability, velocity, AI, particle, model or input changes in stage 1.
- Work from current origin/main on codex/combat-feedback. Push that branch, deliver a uniquely versioned jar, then stop for user feedback.
- Only direct, non-projectile player damage; exclude FakePlayer and environmental/reflected damage.
- Mod entities are supported by damage-path inheritance, never by a vanilla mob whitelist. Document bypasses and actual tests separately.
- Same stage jar on both sides for this feature; old/missing peers get no new packets. Existing visual functions remain usable client-only.
- Do not replace EFR, change its files, or claim all EFR audio now reflects post-mitigation damage.

## Task 1: Result accounting and protocol contracts

Files: combat/DamageTransactions.java, combat/HitFeedbackMessage.java, combat/FeedbackState.java; tests under src/test/java/com/legacyvisualfix/combat/.

- [x] Write failing tests: 20→17 health produces 3 HP; 4→1 absorption produces 3 absorbed HP; unchanged/increasing/nonfinite values produce no damage; nested same-target loss is counted once and different-target loss stays independent; aborted scopes clean up.
- [x] Write failing wire roundtrip and invalid-payload tests; duplicate/out-of-order event IDs, time expiration and connection reset tests.
- [x] Implement minimal pure accounting/state and message encoding. Run the tests.

## Task 2: Server/client integration

Files: combat/CombatConfig.java, CombatServer.java, CombatHitEvent.java, CombatNetwork.java, CombatInbox.java, client/CombatClient.java; mixin/combat/MixinEntityLivingBase.java; LegacyVisualFix.java; mixins.legacyvisualfix.json.

- [x] Wrap both virtual damageEntity calls with original.call in try/finally. Observe every nested transaction inside a direct-player transaction; never call damage twice.
- [x] Filter direct player sources and publish only positive finite observed deltas. Preserve zero-damage/cancelled outcomes without success feedback.
- [x] Use lvf_combat_v1 FML channel registration as protocol capability. Remove peers on disconnect/stop; only send to the attacker.
- [x] Enqueue received messages with handler identity. Drain on client END tick, reject wrong connection/dimension, bound work, reset on world changes.
- [x] Draw white health/gold absorption confirmation near crosshair for 160 ms. Optional debug text displays HP/absorption. Sound mode auto skips additional sound with EFR installed; always/off are explicit options.

## Task 3: Runtime validation and staged release

Files: src/combatSmokeTest/java/.../CombatSmoke.java, addon.gradle, scripts/verify_release.py, docs/combat-stage1-testing.md, README.md.

- [x] Run transformed Forge runtime smoke on standard damage, absorption, armor, cancellation, invulnerability, a custom inherited monster and custom damageEntity override. Compare enabled/disabled state snapshots for health, absorption, velocity and invulnerability timers.
- [x] Validate Java 8 and Java 25 + Angelica integrated-server/client launches. Dedicated launch stopped at the launcher EULA prompt; no agreement accepted on the user's behalf. Document this unverified scope.
- [x] Run unit suite, formatting/build, release-jar verification (no smoke classes, Java 8, mapped mixin entry, matching version).
- [x] Independent review; fix actionable findings and rerun affected checks.
- [x] Commit source and testing documentation, push branch, create a stage prerelease with jar/checksum if GitHub permits, link both local and remote artifacts.
- [x] Stop. Stage 2 requires the user's feedback.

Release delivered: v0.2.0-combat.1, source commit 76193284aedf1c3afc0a1520d47995033566aa28. GitHub asset SHA-256 matches the verified local build. Stage 2 remains pending user feedback.
