# Five UI effects implementation plan

The user approved the preceding source-based design and requested implementation, a GitHub push (no Release), and a local JAR. Work from committed c676caf in an isolated worktree; preserve unrelated uncommitted changes.

## Contract

Implement smooth hotbar selection, hovered slot scaling, carried-item damped rotation and scaling, identical-stack floating, and colored GUI trails. Client only; do not mutate input, slots, stack counts, or network traffic. Reimplement behavior without copying upstream code or assets. Keep existing inventory entrance animation. Use independent config switches and bounded particles. Optional mod hooks load only when installed. Clearly distinguish runtime-tested support from source-level adapters and exclusions.

## Tasks

- [x] Pure motion and trail simulation (`ui/UiMotion`, `ui/TrailParticles`): write JUnit tests first for frame-rate independent convergence, reversal, zero/large dt, bounded emission, expiration and reset; run failing tests then implement and pass.
- [x] Client integration (`ui/UiEffects`, `ui/UiEffectsConfig`, early GUI/hotbar Mixins): track frame/screen lifetime; preserve original item renderers and overlays; scope transforms to modelview; suppress slot effects during inventory entrance; emit trails only for actual carried stack; render before tooltips with GL state restored.
- [x] Optional integration: late Mixins for NEI and ModularUI slot/carried item rendering, deduplicate nested hooks, exclude ghost/fluid slots and NEI catalog/recipe icons. Verify against dependency source and actual transformed client.
- [x] Runtime smoke harness: synthetic player and real GUI/GL, measure transforms, trails and reset, verify enabled/disabled rendering, item matching, rendering state restoration and real hotbar hook. Run vanilla, NEI, ModularUI where dependencies permit. Record exact limits.
- [x] Documentation and delivery: update version to 0.7.0; document config and compatibility evidence; run formatter, tests, build; inspect reobfuscated JAR/refmap and ensure smoke code absent; code review; commit only task files and push branch to origin; deliver JAR here, no Release.

## Verification commands

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-25.0.0.36-hotspot'
./gradlew.bat test --no-configuration-cache
./gradlew.bat runClient -PuiSmoke --no-configuration-cache
./gradlew.bat runClient -PuiSmoke -PneiSmoke --no-configuration-cache
./gradlew.bat spotlessApply clean build --no-configuration-cache
```

Use local `chromaticRepository` mirror already provided by the workspace when needed. GUI smoke runs must use isolated development worlds; no user save modifications. Each test result must be observed before claiming support.

Implementation, review, documentation, 34 JUnit tests, runtime checks and local JAR delivery are complete. The user explicitly confirmed source push to LaplaceRungeLenz/ModernNH, branch codex/immersive-ui-effects, after automatic approval review requested the exact target. No Release was created.
