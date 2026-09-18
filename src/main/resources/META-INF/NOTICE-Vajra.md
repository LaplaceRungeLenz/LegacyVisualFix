# Vajra grid integration

The `com.legacyvisualfix.vajra` module is licensed under GPL-3.0-only.
It adapts the wrench/wire-cutter interactions from PinkYuDeer/GTNH-Qol-Improvements:
https://github.com/PinkYuDeer/GTNH-Qol-Improvements
Source revision: 0db60ec77a0b38ff2fa7ffdde4b28a0f96fea6bd.

Adapted source files: VajraEventHandler.java, VajraOverlayHandler.java,
VajraToolClickMessage.java, ServerVajraClickQueue.java. Modified by LegacyVisualFix
contributors in September 2026. Changes isolate rotation and connection interactions,
remove offhand replacement, held-mining protection and AE part dismantling,
and add LegacyVisualFix configuration, server capability detection and input/permission checks.
VajraAeOrientation.java extracts the generic AE orientation logic and adds the
ME interface output-direction mapping.

The combined code distribution is provided under GPL-3.0-only, with complete
corresponding source available in the matching LegacyVisualFix source commit and source archive:
https://github.com/LaplaceRungeLenz/LegacyVisualFix
The existing MIT notices remain applicable to the original LegacyVisualFix code.
The separately licensed GTNH logo retains its existing asset notice.
See `licenses/GTNH-QoL-GPL-3.0.txt`, `LICENSE` and `NOTICE-GTNH.md`.
