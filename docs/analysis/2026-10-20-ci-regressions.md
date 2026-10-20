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


b7c7845 completed all237 native scenarios;231 passed and6 failed. This is not an accepted checkpoint.
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39415113101

- Pech avoidance fixture supplied a9x7 floor to a native radius16/height7 flee search. The corrected fixture provides a real37x37 supported floor and restores all previous blocks; real path/distance and128-seed bounds remain.
- Vis Amulet drop fixture discarded the stack returned by removeItem and attempted to drop the now-empty old alias. It now verifies and drops the actual detached paid5-CV snapshot; all real-drop/no-duplicate/replay assertions remain.
- Uncommon loot expected weight2913 was a fixture arithmetic error:2669 non-potion weights plus144 potion weights is2813. Production weights match the original and were not changed.
- Advanced Furnace unload fixture retained a FULL33 core ticket, which retains the neighboring holder at34 and prevents old shell-BE retirement. The corrected scenario truly unloads both core and shell, reloads a NEW paid core, checks unavailable-shell pause/no-force-load, breaks the core, then reloads and retires the orphan shell. Timeout1200 covers its three bounded native phases.
- Crystal disk-reload fixture now waits for actual entity-section loading/ticking after full chunk reload before breaking support and demanding six individual physical drops.
- Hungry native diagnostic proved actual block-item death added an empty CONTAINER component, causing the pre-death receipt match to reject before any RNG. The bounded production adapter accepts only that exact native transition while retaining entity/item reference, count and all other components; pricing uses the pre-death receipt. New real scenarios retain the existing BASE probability checks, conserve seven named child iron items from a destroyed container and refuse three callback substitutions. Crystal focused reload passed; the Hungry diagnostic failed before this production fix, so the fix awaits the next full run.

The next active group includes real Traveller boots/Hover Harness, original RP/research/discovery feedback, Knowledge Fragments and passive Primal Charm. Local quick compile3 passed704JUnit and Fabric source sets;84Python and zero missing assets passed. The required full native manifest is243; remote acceptance is pending. New biome/node/Primal/taint units remain outside source sets in.tmp until coherent.

A read-only original mapping audit also corrected MindSpider presentation: func_70647_i is voice pitch, while func_70599_aP is volume. Its original constant.7 now overrides native getVoicePitch; the incorrectly lowered volume override is removed. This is a native mapping correction, not a gameplay balance change; audible client acceptance remains pending.

The follow-up also restores the original integer retreat-position contract before Pech distance/path-end checks. Native DefaultRandomPos returns bottom-center coordinates; legacy truncation rejected negative-coordinate endpoints. Real positive/negative navigation fixtures retain supported floors, strict endpoint/distance assertions and bounded original branch inputs. Final local build after all changes:704JUnit/0failures, common and Fabric main/gameTest/clientTest compilation,84Python, zero missing resource references. Full243-game runtime acceptance awaits the pushed GitHub commit.


075df78 completed243 native scenarios:238 passed,5 failed; NOT accepted.
https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39418828643

Pech positive/negative navigation and both crystal scenarios passed. Hungry passed all original meals/BASE/CONTAINER and callback refusal cases; its later survival assertion encountered native60-tick ServerPlayer spawn protection. The fixture now waits65 real world ticks rather than clearing private protection. PrimalCharm and later worn-Vis phases need a scoped actual embedded-player listener/tick driver; no direct item callback substitutes are allowed. Advanced Furnace now reached true disk reload and paid-core custody, then exposed an indirect neighbor/shape propagation load from a loaded inner part; all loaded-part reversions use quiet flags if ANY structure part is unavailable. Fully loaded behavior remains unchanged.

Loot creative failure is a genuine port mismatch: untouched1.7 ItemInWorldManager.a restores original count after creative item use; native1.21 ServerPlayerGameMode.useItem does not. The bag now omits the survival debit only for its actual creative game mode, preserving identical original playable behavior. The real creative wrapper/count and twelve output-roll assertions remain unchanged in substance. Future exact-input validation is pending; baseline remains b1e49f2.

Follow-up local compile after all five fixes passed in1m10s:704JUnit/0failures and all current Fabric main/gameTest/clientTest source sets. New shared EmbeddedPlayerTickDriver is test-only, explicitly drives missing native entity/listener cycles and observes PLAYER_POST, with stall/duplicate guards and scoped cleanup. Vis/Charm retain actual passive-item resource assertions. No new full native pass is claimed until the next243-case remote run. No script/resource inputs changed since84Python/zero-missing-assets evidence.
