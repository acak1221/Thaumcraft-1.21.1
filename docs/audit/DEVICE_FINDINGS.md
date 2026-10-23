# Automated-device world audit — g26achieve

Scope: one hypothesis, **a built machine spends resources without output, or outputs without payment**. Fabric 1.21.1 only. Empty devices/raw ingredients are explicitly supplied GameTest inputs; survival acquisition and client visuals are not claimed. Original specification: private TC4 4.2.3.5 oracle under `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (read-only).

## Execution status

**Final: 16/16 required GameTests passed on a fresh Fabric dedicated-server world; all14 named device types driven end to end. PROVEN production defects:0. Production fixes:0.**

First run:15/16 passed. Hungry chest reported stored0/input still alive at floorY=-60. Its `ItemEntity` input had the constructor’s random sideways launch, so it missed the contact surface. The only correction sets the declared drop’s initial velocity to zero; gravity and native collision still perform collection. The unchanged production chest then collected exactly7. This was a **test-input defect**, not evidence of a broken chest. Red evidence: `DEVICE_RUN_INITIAL.json`; green evidence: `DEVICE_RUN_FINAL.json`.

Final run log: `.tmp-g26-runtime-final.log`; generated report: `fabric/build/gametest-focused-results.xml`. Server reported `All 16 required tests passed :)` and completed normal world saving/shutdown; Gradle reported `BUILD SUCCESSFUL in 35s`. Java21, private `.gradle-g26`, `-PgameTestHeap=1500M`, `:fabric:runGameTest`. Every launch removed only this worktree’s `fabric/run-gametest/world` first. The new private home required online dependency provisioning. Initial offline attempts lacked Loom/Architectury’s runtime agent; online compile passed. Configuration-on-demand cannot resolve the existing cross-project gameTest source set, so actual acceptance uses normal configuration.

The supplementary `visNetCrystallizer` intentionally exercises throwing post-debit callbacks and malformed providers; its expected warning messages are not failed GameTest verdicts.

## World-scheduled coverage

There are **14 named device types** in scope (basic and advanced furnaces counted separately; basic tubes and buffers separately). All rows are **PROVEN operational for the sampled path**, not claims of exhaustive machine correctness. Ten new world-operation cases plus two existing natural-world cases cover these14 types; four additional existing controlled-ticker cases check exact timing/acceleration and adversarial accounting.

| Device | Measured in/out | Oracle ratio / behavior | World test | Fix |
|---|---|---|---|---|
| Arcane bore | Diamond ore1 removed → diamond1 returned; actual wooden pick wear1; supplied-node acceleration credit remains19.0 | Ore1 → native drop1, pick wear1; optional Perditio CV/5 → acceleration credit | `deviceWorldPoweredBoreMinesAndReturnsItem` | None needed |
| Basic alchemy furnace | Input coal1 + fuel coal1 consumed; at39 input intact/cook39; at40 ignis2+potentia2; later all4 in alembics, fuel reserve1 unchanged | Coal input1 → ignis2 + potentia2; coal fuel1 ignites1600 burn ticks; cook40 without bellows | `deviceWorldFurnacePaysAndDelivers` | None needed |
| Advanced alchemy furnace | Actual formed core pays coal1 + CV8/4/4; all4 essentia recovered from real nozzle | Coal1 + ignis CV8 + perditio CV4 + aqua CV4 → ignis2 + potentia2 | `deviceWorldAdvancedFurnacePaysAndDelivers` | None needed |
| Centrifuge | Source potentia1 depleted → exactly ignis1 OR ordo1; collecting that output leaves no replay | Compound essentia1 → **one** randomly chosen immediate component1, not both/recursive primals; fifth idle tick draws, process39 decrements that same tick | `deviceWorldCentrifugePaysAndDelivers` | None needed |
| Essentia crystallizer | Source potentia1 depleted → physical typed crystal1; zero premature output at990; no duplicate at1070. Additional controlled test:11×8+6=94 Terra CV, output at65 | Essentia1 → typed crystal1; +1 progress per fifth tick, threshold200; from empty source draw at5 and baseline completion1005; assisted increment `1+2×CV` | `deviceWorldCrystallizerPaysAndDelivers`, `visNetCrystallizer` | None needed |
| Deconstruction table | Pearl1 (reduced primal total96) intact at39, consumed at40 → pending RP1 → actual player pool+1 exactly once | One tagged item per40 ticks; chance `nextInt(80) < reduced primal total`; success → exactly primal RP1 | `deviceWorldDeconstructionPaysAndDelivers` | None needed |
| Hungry chest | Physical iron7 consumed → stored iron7 despite pickup delay1000 | Physical contact item stack7 → stored7; no fuel; pickup delay irrelevant | `deviceWorldHungryChestCollectsPhysicalInput` | **Test only:** vertical drop instead of random sideways launch |
| Arcane levitator | At45 ordinary ticks: item height above lift11.41214344524635; powered item1.0; minecart11.210049989260419; no recurring fuel | No recurring fuel; +0.1F lift behavior; native redstone disables | `arcaneLevitatorLiftsItemsAndCreaturesAndStacks` (existing) | None needed |
| Arcane lamp | No recurring fuel; emission15;44 secondary lights in this run; support loss returns exactly lamp1 and removes lights within15, not16 | No recurring fuel; native lamp light15 and dark-air light placement; random secondary-light count is NOT fixed | `arcaneLampLightsDarkAirAndCleansItsOriginalRange` (existing) | None needed |
| Vis relay | Source8 → grants3+5, remaining0 and next request0; actual consumer14 blocks from root produces crystal1 through relay | Lossless routing of renewable source pulse, not a finite battery; pulse8 = grant3 + grant5 + remaining0 | `deviceWorldRelayPaysActualConsumer` | None needed |
| Basic essentia tube | Alembic1 → buffer → tube → crystal1; all transit stores end0 | Transport1 in →1 out; no manufacture | `deviceWorldTubeAndBufferDeliverToConsumer` | None needed |
| Essentia buffer | Same real chain: input1 → output1; terminal buffer0 | Transport1 in →1 out, eight-total capacity | `deviceWorldTubeAndBufferDeliverToConsumer` | None needed |
| Alembic | Furnace generated4 → two typed alembics2+2; separate single-unit transport sources depleted exactly1 | Furnace’s four generated units all reach two actual typed alembics; transport sources pay each draw | Furnace and tube/buffer world tests | None needed |
| Thaumatorium | Catalyst iron nugget1 + source metallum2 consumed → chest iron nuggets3; no replay | Iron nugget catalyst1 + metallum2 → iron nuggets3 | `deviceWorldThaumatoriumPaysAndDelivers` | None needed |

## Evidence boundaries

- New machine-operation tests leave ticking to the actual world scheduler. They do not grant finished output, set progress, seed machine-operation RNG, or invoke machine tickers to advance work.
- Bore/advanced-furnace/relay tests reuse the existing transduction helper **during setup** to form an energized node from an ordinary node. That helper advances converter ticks with a declared branch RNG. This is not natural-time node acquisition. Actual subsequent source/machine operation is world-scheduled.
- The advanced furnace is wand-formed from the real17-piece structure. Its physical coal entity is delivered through the registered core block’s collision entry and all four units are extracted through a real nozzle. Collision is invoked explicitly to make the immediate `8/4/4` debit measurable before renewable refill.
- The thaumatorium is wand-formed from a crucible/two constructs and programmed through its real menu, then runs naturally into a real chest.
- Deconstruction uses a real primordial pearl with at least80 reduced primal units, making the original chance100% without selecting or injecting the reward. This does **not** measure the distribution for low-value items.
- The centrifuge accepts compound **essentia**, not a physical item. One random component per paid input is original behavior. Never “fix” it to output both components.
- A bore’s unassisted mining and a fuel-free lamp/levitator are not free-output bugs: the oracle permits them. Energized nodes renew their throughput every source tick, so conservation is checked within a pulse rather than treating node capacity as an exhaustible battery.

## Oracle anchors

Paths are relative to the private oracle root:

- `thaumcraft/common/tiles/TileAlchemyFurnace.java:255-323` — ignition, cook, tag sum, one input consumed; `:326-375` — alembic transfer.
- `thaumcraft/common/tiles/TileAlchemyFurnaceAdvanced.java:113-142` — three CV aspects and exact `2a/a/a` debit.
- `thaumcraft/common/tiles/TileCentrifuge.java:190-239` — native timing, one source take, random immediate component.
- `thaumcraft/common/tiles/TileEssentiaCrystalizer.java:212-225` — progress, Terra CV acceleration and one typed crystal.
- `thaumcraft/common/tiles/TileDeconstructionTable.java:155-200` —40-tick consumption and original chance/reward.
- `thaumcraft/common/tiles/TileArcaneBore.java:154` — Perditio CV/5 acceleration.
- `thaumcraft/common/tiles/TileVisRelay.java:65-72` — range8, not a source.
- `thaumcraft/common/config/ConfigRecipes.java:145` — TransIron catalyst and metallum2/output3.
- `thaumcraft/common/blocks/BlockChestHungry.java:145-165` — native contact, real leftovers/death, no pickup-delay gate.
- `thaumcraft/common/tiles/TileArcaneLamp.java:24-36,50-58` — fuel-free random secondary lights and removal cube.

## PROVEN versus reasoned-only

**PROVEN:** all14 sampled device operations, the ratios in the table, and the hungry-chest test-input correction (red15/16 → green16/16). **PROVEN production failures:0.** The hypothesis was not reproduced in these sampled production paths; no invented red test or speculative gameplay fix was made.

**Reasoned-only / not covered by this acceptance:**

- Exact aggregate CV debit for the bore’s whole run (only pick wear, material output and residual powered credit measured); no claim that its observed19.0 credit independently proves source-side debit100.
- Natural-time node conversion/acquisition: source setup deliberately reuses controlled converter advancement. The advanced furnace’s collision entry is explicitly invoked, not evidence of an unattended naturally falling item trajectory.
- Statistical50/50 centrifuge distribution or low-value deconstructor probability distribution; no seeded machine-operation RNG in new world tests.
- Every recipe/item, bellows/fuel variant, blocked/full output, long-run operation, cross-file crash/restart, full tube variant network, growth/fertility lamps, or upgraded buffer arrangement. Existing supplementary cases test some boundaries but are not exhaustive.
- Survival crafting/acquisition of every supplied device/input, client visuals, multiplayer and NeoForge.
