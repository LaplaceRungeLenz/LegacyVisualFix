# Temporary actual-pack QA addon

This is an opt-in, disposable-world integration probe. It mutates blocks,
entities, player equipment and game mode in worlds whose name contains
`LVF Combat Stage1 QA`. Use a fresh disposable world, never a valuable save.
It is intended for the integrated server and the GTNH versions in the report.

Build with `gradlew.bat reobfJar -PgtnhCombatQa`. This produces a combined
development JAR which **must not be distributed or installed as the release**.
Extract only `com/legacyvisualfix/packqa/*.class` from it into a separate
`lvf-pack-qa-1.jar`, then install that temporary addon alongside the verified
normal LegacyVisualFix release. Rebuild normally without the property before
running `scripts/verify_release.py` or distributing any artifact. The release
verifier rejects jars containing these QA classes.

Enter the disposable QA world and allow at least 430 server ticks. Reports are
written in the Minecraft directory as `lvf-pack-qa-server.txt` and
`lvf-pack-qa-client.txt`; the client file appends, so separate rounds explicitly.
Screenshots are saved in the normal screenshots directory. Repeat with shaders
on/off to check both rendering paths. Quit the game, remove the temporary addon,
and restore shader and combat settings after testing.

Most batch entities are constructed from the real registry but are not naturally
spawned or advanced through full AI lifecycles. The repeated-hit trace manually
advances hurt timers and does not integrate world physics. See the dated report
for supported conclusions and limitations.
