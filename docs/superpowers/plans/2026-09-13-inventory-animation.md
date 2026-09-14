# Player inventory entrance implementation plan

**Goal:** Animate vanilla survival and creative inventory screens upwards on opening, keeping NEI and potion effects stationary. User approved the preceding design and requested implementation and GitHub synchronization, without a release.

**Architecture:** Keep layout coordinates unchanged. Wrap only background, slots, foreground, vanilla button/label and creative page rendering in a reversible translation. Use exact screen classes, a per-screen monotonic clock, and intercept input before virtual dispatch to consume the first action and its matching release. Other container screens are excluded.

**Tech stack:** Forge 1.7.10, Java 8 bytecode, existing UniMixins, JUnit 4.

- [x] Add tests for first-frame timing, cadence independence, completion, disabled mode, resize, and consumed mouse press/release; run failing tests.
- [x] Implement `inventory/InventoryMotion.java`, `InventoryAnimationConfig.java`, `InventoryAnimations.java`, and `InventoryMotionAccess.java`.
- [x] Add `mixin/inventory/` adapters for GuiContainer, GuiScreen and GuiContainerCreative. Keep default background, NEI hooks and potion rendering outside translation; suppress stale hover during entry.
- [x] Add an opt-in real client smoke test, exercise survival/creative, potion rendering, unrelated containers, GUI scales, state restoration, and NEI when available.
- [x] Run normal clean build, inspect release JAR/refmap and update documentation with actual evidence and limits.
- Delivery: commit and push a feature branch to the configured GitHub repository; provide local JAR, no tag or Release. Delivery status is reported in the task response.

Default entry: 250 ms cubic ease-out, fully below the bottom edge including a 64 GUI pixel top allowance for creative pages. Configuration: enabled, durationMs (0 disables), distance (0 = below-screen; otherwise GUI pixels). Finish on resize/reinitialization, do not replay on tab changes or returning to the same screen. Closing remains immediate. The first action is consumed, including its release, to avoid inventory packets based on stale geometry.
