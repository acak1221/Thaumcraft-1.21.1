# Remote Fabric regression runs — 2026-10-20

These failed runs are not accepted checkpoints. The last full accepted server run remains d508ada.

- ded7bef:193/198 GameTests;647 belongs to a later local compile, not this run. The five failures were stale focus candidates, an unreachable small seed range, an always-creative mock used for paid table placement, an embedded player transport outside the server living-tick loop, and exact original float Bore credit.
- 9359eb4:196/198; remaining failures were the table menu's real indexOf(null) crash after reward claim and a loot fixture that seeded the world stream but not Minecraft's named iron-ore sequence. Both corrected in4d6a458.
- 4d6a458:206/212 GameTests,647 JUnit/79Python and compilation. New failures: stateless runic recipe network identity checking (also breaking two existing recipe packet scenarios), an obsolete scan-session fixture, detached UUID reservation after a canceled orb ADD, wrong cow health assumptions, and a pearl fixture mixing native explosion-shuffle draws with original placement draws. Follow-up fixes preserve the original gameplay assertions and add a real independent-instance recipe codec regression.

Runs:
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39402946505
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39404338634
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39406033014

With cached dependencies/Gradle outputs, the last Gradle run took2m43s versus7m6s for the first cold run.
Full runtime checks remain on GitHub while development continues locally against a separate source window.
