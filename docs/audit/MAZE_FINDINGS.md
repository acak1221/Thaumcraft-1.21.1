# Outer Lands labyrinth audit — g30maze

Scope: one hypothesis, **the labyrinth does not generate, cannot be navigated to its boss, or cannot return the player home**. Fabric 1.21.1, fresh GameTest worlds; seed/map, mock players and gates are supplied test inputs, not survival acquisition.

Specification: read-only private TC4 4.2.3.5 oracle at `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`, especially `MazeThread:29–68`, `MazeGenerator`, `MazeHandler:152–193`, `GenCommon:327–393`, `GenPortal:21–117`, `GenBossRoom:24–87`, `Gen2x2`, and `TileEldritchLock`. `TeleporterThaumcraft` and `EntityCultistPortal` were inspected directly with `javap -c -p` against the original local JAR; no original decompilation is committed.

## PROVEN — 20 grouped rules

`R` = `MazeAuditRuntimeChecks.generation` / Fabric `mazeAuditGeneratesRealRegionAndReloadsMap`; `F` = `MazeFidelityTest`; `I` = `OuterLandsRoomFidelityTest.roomVoidLiningNeverFillsTheWalkableInterior`. Dimensions below distinguish allocated cells from actual block dimensions. Runtime seed **20261021**, size **15**, ring chunk **(2048,2048)**, map origin chunk **(2040,2040)**.

| # | Rule / oracle expectation | Measured native generation or behavior | Test | Fix |
|---|---|---|---|---|
| 1 | Original deterministic packed graph / wall and feature bits | Audit seed matches every oracle cell; existing comparison covers 8 sizes × 40 seeds | F | None |
| 2 | 15×15 allocated grid need not fill its outer border | 153 nonzero cells; occupied X=1..14, Z=0..13: **14×14 chunks / 224×224 block cell-footprint bounds** (allocated extent 240×240) | F, R | None |
| 3 | Room shell floor Y49, passage centre shell floor Y50 (`GenPassage`, room generators) | Correct bedrock at every generated cell centre, 153 cells inspected | R | None |
| 4 | Portal floor local X/Z=2..14 at Y49 | Every local position checked: **13×13, 169 bedrock blocks**, no extra bedrock in that layer | R | None |
| 5 | Boss four quadrants 2..5 form 2×2 chunks; shell floor local 2..30, ceiling Y63 | UL cell **(13,0)**, portal **(8,8)**; boss UL is +80 X, −128 Z blocks from portal chunk origin. **32×32 cell footprint; 29×29 actual floor, 841 bedrock blocks**, exact bounds checked; all four quadrant ceilings Y63 | F, R | None |
| 6 | Portal at cell (8,8), local (8,53,8), with native block entity | World gate **(32776,53,32776)** and live `TileEldritchPortal` | R | None |
| 7 | Portal obelisk starts Y54, four shaft blocks above | Five actual blocks **Y54..58**, original variants 1 then 2×4 | R | None |
| 8 | Portal interior must not be void lining | Before fix: feet/head/neighbor at local (7,52,7) were solid invisible filler; after fix interior sample clear of filler and route exits room | I, R | Restore perimeter/doorway skip condition |
| 9 | Key-room interior must not be void lining | Original generated placements filled centre; corrected plan and real generated key-room sample are not filler | I, R | Same condition, key generator |
| 10 | Nest-room interior must not be void lining | Original generated placements filled centre; corrected plan and all real nest-room samples are not filler | I, R | Same condition, nest generator |
| 11 | Library-room interior must not be void lining | Original generated placements filled centre; corrected plan and real library-room samples are not filler | I, R | Same condition, library generator |
| 12 | Connected chunks must retain a traversable seam (`PAT_CONNECT`, 11×11 shell cross-section) | **149** east/south connections checked on both sides of seam; at least **3 blocks clear at Y55**. Oracle five-wide centre can have edge crystals, so five permanently empty blocks are not required | R | No additional seam fix |
| 13 | Arrival room must connect physically to boss entrance | Before: flood stopped at 1 position. Final run: **12,670** supported two-block-clear positions reached, including adjacency to real boss lock (earlier run 12,669; decoration varies with world) | R | Room lining fix |
| 14 | Specific generated labyrinth marker survives disk save/load | `thaumcraft_maze.dat` saved; empty `DimensionDataStorage` cache yields distinct object; all **225 grid positions** equal live map | R | None |
| 15 | Map creation must not require eager physical generation | Immediately after `storeLabyrinth`, destination chunk is not loaded; requesting it generates its room | R | None |
| 16 | Opening lock populates the selected arena at original offsets | Supplied counter/RNG selects warden; actual generated lock opens, two five-block obelisks appear, variant-4 pedestal and one real warden appear at oracle exit offsets | R | None |
| 17 | Return to overworld chooses nearest gate, not first chunk ring | Before: 14-block-away gate selected over 2-block-away gate across boundary. After: nearer home gate, Y80, ±1 diagonal landing | `mazeAuditReturnsThroughNearestHomeGate` | Compare entire search square using actual player/gate centres |
| 18 | Original inclusive horizontal search bounds ±128 | Before: gate **143** blocks away accepted. After: rejects it, selects in-bounds gate at **128**, Y200 | `mazeAuditReturnSearchUsesExactOracleBounds` | Filter exact block-coordinate bounds |
| 19 | Search must find an unexplored portal within range | Before: portal **64** blocks away missed; fallback landed Y65 instead of Y53. After: generates destination and lands beside actual gate at Y53 | `mazeAuditFindsUnexploredDestinationGate` | Load all intersecting chunks **and resolve packed/pending block entities** |
| 20 | Real portal tick scanner transfers player into Outer Lands | Existing tick-driven dedicated-server test transfers and survives at gate landing height | `eldritchGateMovesThePlayerIntoTheOuterLands` | Regression acceptance |

The walk flood is a bounded block-grid check with two-block clearance, support and ±1 vertical steps, not a Minecraft pathfinder or a manual client walk. It proves a physical route for this seed, not every generated seed or all jump/sprint/collision behaviors.

## Execution record

- `room-red.log`: new oracle interior test failed for all four features before edits. `maze-red-isolated.log`: real room filler/navigation failure plus all three portal destination failures before edits.
- `room-green.log`: both JUnit classes and real-region GameTest passed after room fix, before portal fix.
- `maze-green-final.log`: all new generation/transfer checks and original entry check passed; the separate no-warning check caught a fixture promotion warning after immediate lock opening. The fixture now resolves packed arena tiles before opening, as normal explored chunks do.
- **Final `maze-acceptance.log`: 6/6 GameTests passed; 18/18 JUnit tests passed** (4 `MazeFidelityTest`, 14 `OuterLandsRoomFidelityTest`). No normal generation exception/server crash occurred. **One warning still occurs after the immediate fixture lock opening**, at `(32856,55,32669)`, trying to load a block entity for air; it is outside the separate room-generation capture. Resolving packed tiles did not eliminate this warning, so a globally warning-free server run is **not** claimed. Ordinary player-timed lock opening was not replayed to classify it further.
- Final arena measurement: centre **(32864,32656)**, exit feature **4**, pedestal **(32872,52,32648)**, obelisk bases **(32872,54,32656)** and **(32864,54,32648)**. One actual warden spawns at the pedestal + (0.5,1,0.5).
- Logs are intentionally ignored under `.gradle-g30/`; focused XML is under `fabric/build/gametest-focused-results.xml`.

Earlier failing fixture attempts are not counted as gameplay defects: the first fixture incorrectly expected the entire allocated grid to be occupied; mock cross-dimension transfers require acknowledgement/entity-section promotion; decorative crystals may occupy the outer tunnel edge; a gate landing settles down one block before walking. Dedicated destination cases invoke the **real production transfer** reflectively, while the existing independent entry case exercises the real portal ticker.

## Reasoned-only — 4 additional inspected rules (24 total)

| # | Rule inspected | Oracle/native source assessment | Test / fix |
|---|---|---|---|
| 21 | Crimson portal returns the player | Original **crimson cult portal is a boss entity**, not a transporter; original bytecode has no dimension transfer. The tested return path is `TileEldritchPortal` | No cult-portal interaction runtime test; no invented teleport behavior |
| 22 | Boss shell must initially contain altar and node | `GenBossRoom`/`Gen2x2` produce a sealed shell/lock, not an initial aura node. Lock opening creates a selected arena, obelisks/pedestal as applicable. Universal initial node/altar expectation is unsupported | Warden branch tested in rule 16; no universal node assertion/fix |
| 23 | Graph grows as player explores | Original `MazeThread` computes/stores whole graph up-front; `MazeHandler` populates physical blocks by chunk. Native keeps that separation | Native lazy generation tested in rule 15; original exploration timing not replayed; no change |
| 24 | Corrupted map cannot crash generation | Rejected SavedData fences reads by throwing from `cell()`; feature does not catch it: **reasoned high-severity server-crash risk for a corrupted map** | Not reproduced as a runtime chunk crash; no change without failing runtime case |

No client visual, manual survival traversal, boss combat/victory, full server process restart, or NeoForge acceptance is claimed. A fresh `DimensionDataStorage` cache reading the actual saved file is narrower than restarting the whole server. The selected warden arena is tested, not all four arena selections. Destination selection is tested without the original teleporter's transient 600-tick cache; cache identity/timing parity is not claimed. Full-square portal search intentionally restores oracle loading behavior and may cost more than the incorrect early-exit implementation.
