# ModernNH resource reload screen

Approved in conversation on 2026-09-12; the user explicitly requested implementation.

## Scope and architecture

Build a client-side Minecraft 1.7.10 / Forge 10.13.4.1614 mod named ModernNH (mod ID modernnh), using GTNH's official Project Starter and its pinned GTNHGradle build. Keep Java 8 bytecode compatibility. Use client-only Mixins for resource reload observation. Do not replace startup splash rendering or change resource listener order/thread affinity.

Intercept runtime resource refresh (language changes, resource pack application, F3+T). Draw on the Minecraft thread before/after reload listeners and at available texture progress boundaries. Explicitly swap/pump the display while preserving GL state. Do not run resource listeners or GL work on a background thread. An indivisible third-party operation may still pause the screen.

## Components and data flow

`ReloadProgress` is a pure Java session/state model. `ReloadScreen` owns session lifecycle and frame throttling. Mixins forward start, listener/texture progress and end events. `Theme` parses bounded configuration independently of Minecraft. `ThemeTextures` caches independently owned textures before reload starts and only replaces them after successful reload. `LoadingRenderer` draws a background and track/fill textures or solid fallback rectangles. The mod's client initialization enables runtime rendering only after startup.

Use completed listeners / total listeners for overall progress, labelled as stages rather than elapsed-time percentage. Texture subprogress is separate when available. Never advance a fake timer to 100%. A new session resets all counters; failures always release session state and preserve the original resource reload exception. Renderer/config failures disable custom drawing for that session and log the error without swallowing reload failures.

## Theme contract

Read `config/modernnh/theme.properties`; default assets live under `assets/modernnh/textures/gui/`. Configurable background, track and fill PNG resource locations, normalized bar position, logical pixel size, colors, text visibility and background fit (cover/contain/stretch). Resource packs may override default asset paths. Optional PNG files under `config/modernnh/` support pack authors without repacking a jar. Reject path traversal, non-finite/out-of-bounds dimensions and invalid colors; fall back safely. Use an immutable theme snapshot throughout each reload; new theme applies to the next session.

## Verification and boundaries

Unit tests cover session reset, nested callbacks, progress bounds, failure cleanup, malformed configuration and layout. Build/reobfuscation must pass with generated Mixin refmap. Document manual client checks for language changes, resource packs, F3+T, repeated reload, missing/broken theme assets, resizing and fullscreen. Angelica/lwjgl3ify integration requires actual client runs; do not claim compatibility based on compilation. Provide a GTNH-inspired example theme without official artwork or endorsement.
