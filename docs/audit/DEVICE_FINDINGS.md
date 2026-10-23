# Automated-device world audit — g26achieve

Scope: one hypothesis, **a built machine spends resources without output, or outputs without payment**. Fabric 1.21.1 only. Empty devices/raw ingredients are explicitly supplied GameTest inputs; survival acquisition and client visuals are not claimed. Original specification: private TC4 4.2.3.5 oracle under `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (read-only).

## Execution status

First actual dedicated-server run: **15/16 GameTests passed**, hungry chest failed with stored0/input still alive at floorY=-60. The initial drop constructor supplies random sideways velocity, so that failure does not yet prove contact with the chest. Correcting that declared input and rerunning is required. No production defect is proven. The initial offline compile lacked cached Loom; the online compile subsequently passed. Configuration-on-demand is incompatible with the existing cross-project `gameTest` source-set access; normal configuration works.

## World-scheduled coverage

There are **14 named device types** in scope (basic and advanced furnaces counted separately; tubes and buffers separately). The expected ratios below are assertions/specification, **not measured results until a recorded passing run**.

| Device | Measured in/out | Oracle ratio / behavior | World test | Fix |
|---|---|---|---|---|
| Arcane bore | Pending | Ore1 → native drop1, pick wear1; optional Perditio CV/5 → acceleration credit | `deviceWorldPoweredBoreMinesAndReturnsItem` | None proven |
| Basic alchemy furnace | Pending | Coal input1 → ignis2 + potentia2; coal fuel1 ignites1600 burn ticks; cook40 without bellows | `deviceWorldFurnacePaysAndDelivers` | None proven |
| Advanced alchemy furnace | Pending | Coal1 + ignis CV8 + perditio CV4 + aqua CV4 → ignis2 + potentia2 | `deviceWorldAdvancedFurnacePaysAndDelivers` | None proven |
| Centrifuge | Pending | Compound essentia1 → **one** randomly chosen immediate component1, not both/recursive primals; fifth idle tick draws, process39 decrements that same tick | `deviceWorldCentrifugePaysAndDelivers` | None proven |
| Essentia crystallizer | Pending | Essentia1 → typed crystal1; +1 progress per fifth tick, threshold200; from empty source draw at5 and baseline completion1005 | `deviceWorldCrystallizerPaysAndDelivers` | None proven |
| Deconstruction table | Pending | One tagged item per40 ticks; chance `nextInt(80) < reduced primal total`; success → exactly primal RP1 | `deviceWorldDeconstructionPaysAndDelivers` | None proven |
| Hungry chest | Pending | Physical contact item stack7 → stored7; no fuel; pickup delay irrelevant | `deviceWorldHungryChestCollectsPhysicalInput` | None proven |
| Arcane levitator | Pending | No recurring fuel; +0.1F lift behavior; native redstone disables | `arcaneLevitatorLiftsItemsAndCreaturesAndStacks` (existing) | None proven |
| Arcane lamp | Pending | No recurring fuel; native lamp light15 and dark-air light placement | `arcaneLampLightsDarkAirAndCleansItsOriginalRange` (existing) | None proven |
| Vis relay | Pending | Lossless routing of renewable source pulse, not a finite battery; pulse8 = grant3 + grant5 + remaining0 | `deviceWorldRelayPaysActualConsumer` | None proven |
| Basic essentia tube | Pending | Transport1 in →1 out; no manufacture | `deviceWorldTubeAndBufferDeliverToConsumer` | None proven |
| Essentia buffer | Pending | Transport1 in →1 out, eight-total capacity | `deviceWorldTubeAndBufferDeliverToConsumer` | None proven |
| Alembic | Pending | Furnace’s four generated units all reach two actual typed alembics; transport sources pay each draw | Furnace and tube/buffer world tests | None proven |
| Thaumatorium | Pending | Iron nugget catalyst1 + metallum2 → iron nuggets3 | `deviceWorldThaumatoriumPaysAndDelivers` | None proven |

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

## PROVEN versus reasoned-only

**PROVEN defects: none yet.** No gameplay change is justified before a failing behavioral test. All current expected outcomes remain unexecuted/reasoned-only until runtime evidence is recorded.
