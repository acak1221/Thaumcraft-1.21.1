# Remote Fabric regression runs — 2026-10-20

The failed runs below are not accepted checkpoints. Follow-up b1e49f2 is now accepted:648JUnit/212FabricGameTests/79Python, full build and fresh packaging/attribution/isolation gates passed.
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39409582347

- ded7bef:193/198 GameTests;647 belongs to a later local compile, not this run. The five failures were stale focus candidates, an unreachable small seed range, an always-creative mock used for paid table placement, an embedded player transport outside the server living-tick loop, and exact original float Bore credit.
- 9359eb4:196/198; remaining failures were the table menu's real indexOf(null) crash after reward claim and a loot fixture that seeded the world stream but not Minecraft's named iron-ore sequence. Both corrected in4d6a458.
- 4d6a458:206/212 GameTests,647 JUnit/79Python and compilation. New failures: stateless runic recipe network identity checking (also breaking two existing recipe packet scenarios), an obsolete scan-session fixture, detached UUID reservation after a canceled orb ADD, wrong cow health assumptions, and a pearl fixture mixing native explosion-shuffle draws with original placement draws. Follow-up fixes preserve the original gameplay assertions and add a real independent-instance recipe codec regression.

- 1cb5723:210/212 GameTests. Recipe networking, session scan identity, rejected orb UUID retry, Shock and Pearl checks now pass. The remaining Arcane Lamp fixture raced the threaded native lighting engine; it now waits for observed block light while retaining the bright-air assertion. The orb trajectory failure exposed a production incompatibility: modern Entity.moveTowardsClosestSpace applies random escape velocity in air and damps other axes. The adapter now preserves the original vanilla1.7 collision guard, WEST/EAST/UP/NORTH/SOUTH selection and single-axis impulse; the real trajectory assertion remains and a solid-block escape case was added. These fixes still need the next remote run.

- 7b9dccd:211/212 GameTests. Lamp lighting and both original orb trajectory/solid-escape assertions pass. The sole failure was the runic fc25 fixture: one-sided occupied pedestals added original instability and allowed real destructive hazards. The next fixture uses actual mirrored pedestals/skulls and checks nonpositive matrix instability while retaining all original resource/payment assertions; production behavior is unchanged.

Runs:
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39402946505
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39404338634
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39406033014
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39407177380
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39408815980

With cached dependencies/Gradle outputs, the last Gradle run took2m43s versus7m6s for the first cold run.
Full runtime checks remain on GitHub while development continues locally against a separate source window.


Fourth-group run04208de failed; it is not accepted and produced no completed GameTest XML.
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39413473246

The server aborted on a test-only PulseRelay installed with the production TileVisRelay type/ticker.
Its replacement has its own registered test block/type and native matching ticker. Six earlier failures
were: stale manually encoded optional-potion bytes, an incorrect 1% discount expectation (9ratherthan10
centivis), inspecting a typed payload rather than Architectury's actual encoded wrapper, ignoring native
XP-orb world RNG draws, Pech's original wrong-WAND cost query rejected by strict focus data, and an
Advanced Furnace handler rejecting unrelated wand targets before checking its trigger. The last two
are production integration fixes; the other assertions retain their original resource/state requirements.

The follow-up also includes original Pech avoidance/watch goals and the actual Hover Girdle.
Local691JUnit/84Python, Fabric main/gameTest/clientTest compilation and zero missing asset references
pass;237nativecases require the next remote run. Full server acceptance still belongs to b1e49f2.
