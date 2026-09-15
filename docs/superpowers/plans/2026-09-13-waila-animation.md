# Waila tooltip animation implementation plan

**Goal:** Add the user-approved first version of Waila size transitions to LegacyVisualFix; push source to GitHub and deliver a local JAR without a Release.

**Design:** Waila remains responsible for information and content layout. A persistent client animation tracks dimensions over render frames. A late Mixin wraps the existing renderer, temporarily moves the content origin, substitutes background dimensions, and clips content to the visible interior. Original tooltip fields and scissor state are restored in `finally`. Content is never stretched. No show/hide fade or old-content cache.

**Constraints:** Minecraft 1.7.10, Forge 10.13.4.1614, Java 8 bytecode, UniMixins >= 0.2.1. Waila is optional and must not be bundled. Compile/test against GTNH Waila 1.19.34. Existing reload screens retain their behavior.

**User addition:** Explicitly support ChromaticTooltips 1.0.35-GTNH and Compat 1.0.36-GTNH before/after theme resource packs. The Compat overwrite requires a mutually exclusive adapter: scope Waila's public overlay call, animate root decorators and transform bounds, retain full-size content, and refresh the Waila renderer at context construction. Test default, author's GregTech simple/icon packs, removal and Compat disabled. Actual smoke now enters public `renderOverlay` with a temporary world/ray target and a custom progress renderer.

## 1. Animation and configuration

- [x] Add `waila/TooltipAnimation.java`, with monotonic-time, fixed-duration smoothstep interpolation and explicit reset. New targets start at the currently displayed size. Repeated equal targets do not restart the transition.
- [x] First add `TooltipAnimationTest`: assert initial size 100, midpoint 150 when moving to 200 over 100 ms, endpoint 200; retarget at midpoint without jumping; compare 30/144 FPS endpoint; reset and zero-duration snap.
- [x] Run the test before and after implementation.
- [x] Add independent startup configuration (`config/legacyvisualfix/waila-animation.cfg`): enabled and durationMs (150 default, 0 disables transitions, maximum 1000).

## 2. Optional Waila rendering adapter

- [x] Add a client-only `ILateMixinLoader` and separate JSON; only return the two Waila mixins if `Waila` is installed.
- [x] Add `AccessorTooltip` for x/y/w/h/pos. Add `MixinOverlayRenderer` to wrap the actual draw call, reset after a frame with no drawn tooltip, substitute background width/height and begin content clipping after background rendering.
- [x] Put Minecraft/GL adaptation in `waila/WailaAnimationRenderer.java`; use integer output for crisp pixel edges, original Waila anchor proportions, original content offsets, and scissor intersection in framebuffer coordinates. Reset on world/screen/scale changes.
- [x] Add compile-only Waila dependency and opt-in `wailaSmoke` runtime/source set. Smoke tests call the transformed Waila renderer in a real GL context with synthetic text/icon/custom renderer tooltips, verify interpolation, restoration and capture images. Also run without Waila.

## 3. Delivery

- [x] Update README, metadata, version to 0.4.0 and testing record with actual results and limitations.
- [x] Run formatting, tests, client smoke checks and a clean normal build. Inspect JAR for mixins/refmap, Java 8 bytecode and absence of smoke classes or bundled Waila.
- [x] Review changes; commit and push the source to the existing GitHub repository without tags or Release. Link the normal JAR in the final response.

Verification complete: 14 unit tests, real vanilla and Chromatic render entry tests, resource-pack transitions, Compat-disabled fallback, and no-Waila regression. Delivery commit/push follows this checklist update; see Git history for synchronization evidence.
