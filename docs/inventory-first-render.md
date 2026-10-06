# First inventory render timing regression (issue #15)

Omgise clarified on 2026-10-06 that the first inventory opening after world
entry skips its entrance, while reopening works. The new 30 fps recording
shows a survival inventory panel appearing at its settled position; subsequent
openings have intermediate positions.

The animation clock previously began before drawing the first inventory frame.
Lazy item/player rendering could consume the entire 250 ms entrance while the
panel was still displaced below the screen. The next frame then jumped to zero.
The clock now anchors once at the first completed Forge GUI draw. Later frames
retain the existing elapsed-time curve, and input cancellation, close, resize,
screen navigation and Satchels recognition retain their existing behavior.

## Verification

- Baseline: locally built, reobfuscated `0.2.3` from
  `8a93a76d253acea9e803d42496f0ab0bbd55f273`, SHA256
  `92957756d0ddb16f74a10a955ee507636b22780aa043ae08964ed07dc8351c85`.
  This was not an official GitHub 0.2.3 release.
- New regression first failed with expected offset 4 but actual 0 after a slow
  first render. Full build, formatting and style checks subsequently passed;
  all 91 unit tests passed.
- Normal Mojang 1.7.10 client, Forge 1614, Java 22, lwjgl3ify 3.0.35 and
  UniMixins 0.3.2, with `devEnvironment=false`; new isolated integrated worlds
  and the vanilla inventory key counter, without inventory pre-warming.
- NEI 2.8.156, ModularUI2 2.3.92 and An Extra Touch 7462907: unmodified
  cold openings progressed, but the first survival opening had 8 frames versus
  17 on reopening, and creative had 12 versus 17. An earlier natural cold
  draw of 239 ms had no intermediate frame. This timing depends on render load.
- A separate diagnostic mod delayed only the first GUI draw by 400 ms. The
  baseline reproduced a two-frame jump in both survival and creative; reopening
  worked. With the fix, the identical controls both passed, including intermediate
  frames and natural expiry. No diagnostic code is included in the deliverable.
- Eight normal production cases passed with NEI/MUI2/AET: survival and creative
  default, search and player-inventory tabs at GUI scales 1 and 2. Hover hit
  testing is suppressed during entrance and restored afterward. Native LWJGL
  event-queue left/right pickup, placement and merge hit the correct slots.
- With Satchels 1.0.7, the same eight cases passed, including all 15 equipped bag
  slots. Normal equipment-tab navigation returned to Satchels without replaying
  entrance. Existing hovered-item scaling reached approximately 1.25 at both GUI
  scales in survival/Satchels and the creative player-inventory tab.

The reporter's exact JAR bytes, full modpack and configuration were not supplied.
These results establish and fix the slow-first-render mechanism, rather than
proving every cause in that installation. Java 8/LWJGL2, physical keyboard
delivery, complete custom creative pages and unrelated RiftFlux behavior were
not tested. Rendering delays after the first GUI Post event or in later frames
can still consume elapsed animation time.
