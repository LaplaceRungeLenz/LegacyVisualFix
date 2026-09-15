# LegacyVisualFix Reload Screen Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking. The user has authorized implementation in this task.

**Goal:** Create a buildable GTNH-based client mod with a configurable resource reload progress screen.

**Architecture:** Observe synchronous reloads with client Mixins, retain independent theme textures, redraw at listener and texture boundaries. Keep progress and configuration independent of GL for deterministic testing.

**Tech Stack:** Official GTNH Project Starter, GTNHGradle 2.0.20, Minecraft 1.7.10, Forge 10.13.4.1614, Java 8 bytecode, Mixin, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-09-12-reload-screen-design.md`

## Global Constraints

- Mod name LegacyVisualFix; mod ID legacyvisualfix.
- Preserve original reload execution order, thread and exceptions.
- Render only on the Minecraft thread after client startup.
- Single opaque operations may pause rendering; no fabricated time percentage.
- Preserve Java 8 bytecode compatibility.
- Do not claim client compatibility until tested in that client.

## Task 1: Build foundation and progress model

Files: official starter build files, `src/main/java/com/legacyvisualfix/LegacyVisualFix.java`, `reload/ReloadProgress.java`, `src/test/java/com/legacyvisualfix/reload/ReloadProgressTest.java`.

Interface: `begin(int)`, `beforeListener(String)`, `afterListener()`, `finish()`, `isActive()`, `getCompleted()`, `getTotal()`.

- [x] Configure starter metadata, Mixin support, JUnit, and pin versions; remove example classes.
- [x] Write tests that check `begin(3); afterListener();` yields completed=1 and total=3, completion is clamped, `finish()` deactivates and a new session resets completed=0.
- [x] Observe missing implementation with javac/JUnit while Gradle bootstraps; implement model, then run the complete Gradle test suite.
- [x] Resolve dependencies and inspect mapped Minecraft/Forge source before selecting exact injection descriptors.

## Task 2: Theme and layout

Files: `theme/Theme.java`, `theme/ThemeTextures.java`, `render/LoadingRenderer.java`, `src/test/java/com/legacyvisualfix/theme/ThemeTest.java`, default config and sample theme under `examples/`.

Interface: `Theme.parse(Properties)` returns immutable validated settings; `ReloadScreen` consumes the theme and cached textures.

- [x] Test invalid/non-finite numeric input, invalid color, background fit, file traversal and fallback defaults before implementing parser.
- [x] Implement bounded parsing and test with `gradlew test`.
- [x] Implement isolated GL texture ownership and GL-state-safe background / track / clipped fill drawing. Cache before reload; delete replaced owned textures.
- [x] Add default properties and GTNH-inspired example with resource-pack asset override instructions.

## Task 3: Reload integration

Files: `reload/ReloadScreen.java`, `core/LegacyVisualFixLoadingPlugin.java`, `mixin/MixinMinecraft.java`, `mixin/MixinReloadableResourceManager.java`, texture hooks chosen from decompiled source, Mixin configuration.

- [x] Wrap runtime refresh in begin/try/finally/end; observe listeners without reordering. Skip startup and recursive drawing.
- [x] Draw initial frame, actual completed listener count and current stage. Observe Forge texture progress if the target exposes it; throttle redraw without skipping completion.
- [x] On renderer failure disable screen for session; preserve reload exceptions and restore GL state in finally.
- [x] Build with `gradlew test build`; verify refmap and client-only Mixin declarations in jar.

## Task 4: Delivery verification

Files: `README.md`, `docs/testing.md`.

- [x] Run formatting and required build checks once final sources are in place.
- [x] Attempt a development client run if available, document exact tested runtime and distinguish untested compatibility cases.
- [x] Review implementation against spec, inspect git diff and final jar; deliver artifact path and configuration instructions.
