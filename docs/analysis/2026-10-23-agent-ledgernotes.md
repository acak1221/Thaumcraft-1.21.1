# Wave 3 — the `ledgernotes` agent — the notes of the `w3rest` lane

Worktree: `/home/cyberfox-kali/wt/ledgernotes` (branch `agent/ledgernotes`).

The lane is the unit list of the wave 3 server-side agent:
`/home/cyberfox-kali/units/w3rest.txt` (28 units). Its own report is
`docs/analysis/2026-10-23-agent-w3rest.md`; the oracle is
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, 859/859
fingerprints against the ledger).

## What actually happened

The union merge of `scripts/porting/merge_wave_branch.py` resolved the ledger
conflict by union, unit by unit, but `merge_ledger` only ever overwrites the
four verdict keys (`native_coverage`, `disposition`, `source_semantics`,
`callsite_audit`). Every other key of the base row is left alone, so the
`notes` and the `evidence` of the branch that is merged in are **dropped
silently** whenever the base row already has them. That is why the verdicts
of the `w3rest` lane survived the merge `ffd8d460` ("Merge branch
'agent/w3rest' into work") and its notes did not.

Measured at that merge:

| side of `ffd8d460` | rows whose notes the union dropped | note strings |
|---|---|---|
| the 28 units of the `w3rest` lane | 14 | 15 |
| two rows of another lane (`BlockLifterRenderer`, `LifterItemColor`) | 2 | 5 |
| **total** | **16** | **20** |

The same defect dropped the `evidence` of the lane as well: **61 evidence
strings** over the 28 units of the lane.

### The text was recoverable

The brief assumed the branch was gone and the text unrecoverable. At the
start of this pass the `agent/w3rest` branch and its commits were still in
the object store of the repository (`d94f4cd2`, `099b40ef`, `b22b3878`,
`98783380` all resolve), and the ledger of the lane was still readable from
them. The 15 lost note strings of the lane and the 61 lost evidence strings
were therefore recovered **verbatim** from the commits of the lane rather
than reconstructed by guesswork.

Recovering the text was necessary but not sufficient: the recovered text was
written against the tree of that moment, and the tree has moved on since. The
notes below are therefore **re-derived against the current tree** — a note that
does not match the code is worse than no note.

### The two node rows

`thaumcraft/api/nodes/INode.java` and `thaumcraft/common/tiles/TileNode.java`
were the two rows that still described the state **before** the `INode`
migration. The migration is done:

* `common/src/main/java/thaumcraft/common/tiles/TileNode.java:60` —
  `public final class TileNode extends BlockEntity implements INode`, exactly
  as the original does (oracle `thaumcraft/common/tiles/TileNode.java:75-77`).
* the published `thaumcraft.api.nodes.NodeType` / `NodeModifier` are the
  canonical ones and the nested `TileNode.NodeType` / `TileNode.NodeModifier`
  are gone (oracle `thaumcraft/api/nodes/NodeType.java`, `NodeModifier.java`).
* `common/src/main/java/thaumcraft/api/nodes/NodeAdapter.java:42-71` — the
  published view now **delegates** `setNodeType` / `setNodeModifier` to the
  node; it does **not** throw `UnsupportedOperationException` any more.
* the write path of the type and of the modifier is the contract of the
  original (oracle `TileNode:240-271`).

The old notes claimed the opposite ("the native game node does not implement
INode", "it keeps its own nested NodeType/NodeModifier enums", "that view
throws UnsupportedOperationException"). Those claims are false against the
tree, so both rows are rewritten whole. The difference that is really left
is the **identity of the node**:

* the original derives it from the position of the node —
  `dim:x:y:z` (oracle `TileNode:98-115`, `getId` / `generateId`);
* the port keeps a random UUID that is persisted with the node (native
  `TileNode:63,136,206-210,862-863,881`);
* that format is bound into the accepted evidence of the wave:
  `fabric/src/clientTest/java/thaumcraft/fabric/clienttest/FocalManipulatorClientSequence.java:509`
  parses the identity with `UUID.fromString`, and the accepted saved worlds
  carry the UUIDs in their node NBT — so the port cannot change it without
  invalidating the accepted client run.

The two rows keep `native_coverage = partial` for exactly that reason, and
the `REMAINING GAP` note now names it with the oracle `file:line`.

## The rows that were rewritten

**14 rows** — the notes of the rows whose notes the union merge dropped.
The notes were rewritten (not appended), so each row names the difference
that is really left and no stale claim survives next to it.

| # | `canonical_path` | verdict (unchanged) | notes |
|---|---|---|---|
| 1 | `thaumcraft/api/IArchitect.java` | `adapt` / `parity_verified` | 7 → 3 |
| 2 | `thaumcraft/api/aspects/AspectSourceHelper.java` | `adapt` / `partial` | 5 → 2 |
| 3 | `thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java` | `adapt` / `parity_verified` | 5 → 2 |
| 4 | `thaumcraft/api/nodes/INode.java` | `port` / `partial` | 4 → 4 |
| 5 | `thaumcraft/common/CommonProxy.java` | `replace` / `parity_verified` | 4 → 3 |
| 6 | `thaumcraft/common/blocks/BlockCustomPlantItem.java` | `replace` / `partial` | 4 → 4 |
| 7 | `thaumcraft/common/container/SlotGhostFluid.java` | `adapt` / `parity_verified` | 5 → 2 |
| 8 | `thaumcraft/common/items/relics/ItemSanityChecker.java` | `port` / `parity_verified` | 3 → 1 |
| 9 | `thaumcraft/common/lib/events/EventHandlerWorld.java` | `replace` / `partial` | 8 → 5 |
| 10 | `thaumcraft/common/lib/events/ServerTickEventsFML.java` | `replace` / `partial` | 5 → 2 |
| 11 | `thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java` | `replace` / `parity_verified` | 3 → 1 |
| 12 | `thaumcraft/common/lib/world/dim/GenKeyRoom.java` | `adapt` / `parity_verified` | 5 → 4 |
| 13 | `thaumcraft/common/tiles/TileNode.java` | `replace` / `partial` | 8 → 7 |
| 14 | `thaumcraft/common/tiles/TileResearchTable.java` | `adapt` / `parity_verified` | 6 → 5 |

**28 rows** — the evidence of every unit of the lane was restored (the 61
strings the same merge dropped), with the line numbers of the tree of today.
The 14 rows without lost notes keep their notes; only their evidence is
restored: `BlockCosmeticStairs`, `ItemJarNode`, `EntityItemGrate`,
`ItemNugget`, `ItemVoidRobeArmor`, `ItemThaumometer`,
`PacketScannedToServer`, `ThaumcraftWorldGenerator`, `WorldgenLootHelper`,
`TileCrucible`, `TileJarBrain`, `TileJarNode`, `AuraChunk`, `AuraHandler`.

### What each note now says

* **IArchitect** — the published interface carries the two original members
  and the original `EnumAxis` again (oracle `IArchitect:17-28`), the two
  native foci implement the original rules of the axis and of the area
  (`WandArchitect:98-149`, oracle `ItemFocusTrade:301-319` and
  `ItemFocusWarding:288-309`); the one difference left is that the native
  architect is server authoritative, so an add-on override is never consulted.
* **AspectSourceHelper** — the drain, the search, the direction and the range
  are the original ones (`AspectSourceHelper:34-64`, oracle
  `EssentiaHandler:49-56` and `:71-78`); the essence flow packet of the
  original (oracle `EssentiaHandler:54`, `PacketFXEssentiaSource:50-77`) and
  its client drawing (oracle `ClientTickEventsFML:250-269`) are missing,
  because the drawing needs `FXEssentiaTrail` of the client lane.
* **DamageSourceIndirectThaumcraftEntity** — the two modifiers of the original
  are published with their original names `setFireDamage` and `setUnblockable`
  (oracle `DamageSourceIndirectThaumcraftEntity:15-35`) and select a damage
  type that carries the property and refuse one that does not; the forced
  1.21.1 change (the `DamageSource` is a record over a registered type) is
  stated.
* **CommonProxy** — the server side of the original proxy is published again
  (native `CommonProxy:42-101`, oracle `CommonProxy:63-96`), the knowledge is
  asked for per player because there is no global registry; the about forty
  client effect entry points (oracle `CommonProxy:158-327`) stay with the
  client lane.
* **BlockCustomPlantItem** — the placement plays the break sound of its own
  sound type (native `BlockCustomPlant:186-187`) and the 1.21.1
  `BlockItem.place` plays it with exactly the volume `(volume + 1) / 2` and
  the pitch `pitch * 0.8` of the original (oracle
  `BlockCustomPlantItem:76`); the difference left is the Cave soil of the two
  Cave plants, whose Forge table is not part of the decompiled original.
* **SlotGhostFluid** — the slot accepts every native fluid container (native
  `SlotGhostFluid:33-40`), a bucket of the vanilla or the item of an add-on
  that carries the bucket entity data; the limit of one item and the rule
  that the slot never gives the container back are the original ones (oracle
  `SlotGhostFluid:24-37`).
* **ItemSanityChecker** — the warp reading of the later 1.12 reference was
  removed, because the 4.2.3.5 original has no use behaviour at all (oracle
  `ItemSanityChecker:27-55`); the relic is as pointless as the original's.
* **EventHandlerWorld** — the cancellation of a block placement next to an
  active eldritch boss of the Outer Lands is reproduced (native
  `BossPlacementRules:30-41`, oracle `EventHandlerWorld:225-245`, the
  labyrinth cell of a boss room and the boss within the original radius of
  32), with one loader hook per platform; the difference left is the chunk
  save and chunk load marker (oracle `EventHandlerWorld:114-137`), which has
  no 1.21.1 counterpart, and the maze load and save.
* **ServerTickEventsFML** — the chunk regeneration queue of the original
  regenerates the chunks whose saved data carries no Thaumcraft marker, ten
  chunks per world tick (oracle `ServerTickEventsFML:83-104` and
  `EventHandlerWorld:114-137`); 1.21.1 has no chunk data event and no per
  chunk NBT extension point, and the TC4 generation of the port runs through
  the datapack feature pipeline, which cannot be re-run for an existing chunk
  without regenerating its terrain. The options keep their default `false`.
* **PacketFXBlockSparkle** — the server half of the block sparkle exists
  again (native `BlockFxPayload:31,80-82`, oracle
  `PacketFXBlockSparkle:29-33`), it travels with the original radius of
  thirty two blocks (`BlockFxNetworking:27,82-88`), the flux scrubber sends
  it with the original colour `0xDD00FF` (native `TileFluxScrubber:138-140`,
  oracle `TileFluxScrubber:100`), and the client draws the seven sparkles
  with the colour of the packet and the random grey of the wand colour
  `-9999` (native `BlockFxClientEffects:96-108` and `BlockSparkleParticle:85-90`,
  oracle `ClientProxy:632-644`).
* **GenKeyRoom** — the spawn record carries the home of the guardian, the
  centre of the room with the radius of sixteen (native `DungeonRooms:135,842`,
  oracle `GenKeyRoom:110`), and the first guardian of a hard key room is the
  champion of the original (native `ThaumcraftOuterLands:376-384`, oracle
  `GenKeyRoom:111-113`).
* **TileResearchTable** — the copy of a solved note costs one paper and one
  black dye, the original `Items.dye` of the metadata zero
  (`minecraft:black_dye` of 1.21.1), and never an ink sac (oracle
  `TileResearchTable:398-422`); the network receiver, the commit of the
  table, the admission of the menu, the tooltip of the client and the two
  probes of the game test harness were changed together.
* **INode** and **TileNode** — the post-migration state, see above.

## The evidence that was made false and removed

Two evidence strings of the `INode` row described the state before the
migration and were removed, because the row now contradicts them:

* `native .../NodeAdapter.java setNodeType and setNodeModifier throw UnsupportedOperationException`
* `native .../TileNode.java the native node keeps its own nested NodeType/NodeModifier enums (2026-10-23 review)`

Two further evidence strings of that row were dropped as duplicates of the
line-anchored form of the same claim.

## The citations that were wrong

Three citations of the rows this pass touches pointed **past the end of the
file** of the oracle and were corrected to the real range of the same claim:

| row | was | now | the claim |
|---|---|---|---|
| `ItemNugget.java` | `ItemNugget.java:69-93` (92 lines) | `ItemNugget.java:62-88` | the creative listing of the nuggets and of the clusters of the other mods |
| `TileNode.java` | `TileNode.java:879-897` (845 lines) | `TileNode.java:828-843` | `checkLock`: the node stabilizer gives the lock one and the advanced node stabilizer the lock two |
| `TileResearchTable.java` | `TileResearchTable.java:407-441` (424 lines) | `TileResearchTable.java:398-423` | the duplicate of a solved note requires and consumes one paper and one black dye |

A script checked **every** `oracle <path>:<lines>` citation of the 28 rows
of the lane: 133 citations, all of them resolve to a real range of a real
file of the oracle. The only two "missing files" are
`thaumcraft/common/world/aura/AuraChunk.java` and `AuraHandler.java`, which
are genuinely absent from the 4.2.3.5 oracle — which is exactly what their
notes say.

## The native citations of the `TileNode` row

The evidence of the `TileNode` row was written against a tree that has since
moved the rules of the node into the shared `NodeStabilizerRules`, and it
carried the absolute path of the oracle on the machine of the audit
(`/home/Acak/work/...`). Both are corrected on that row:

| was | now |
|---|---|
| `oracle /home/Acak/work/thaumcraft-4.2.3.5-oracle/.../TileNode.java:<five citations>` | `oracle thaumcraft/common/tiles/TileNode.java:<the same five citations>` |
| `native .../TileNode.java:555 private int regenerationInterval() ...` | `native .../NodeStabilizerRules.java:24,51 the regeneration ladder ... called by the native TileNode:703-711` |
| `native .../TileNode.java:610 if (level.random.nextInt(12500) != 69 ...)` | `native .../NodeStabilizerRules.java:27-28,72 the FADING repair roll nextInt(12500 / lock) == 69` |
| `native .../TileNode.java:309 public void drainIntoWand(...)` | `native .../TileNode.java:408 public void drainIntoWand(...)` |
| `native .../TileNode.java:325 int tap = 1; ...` | `native .../TileNode.java:424-426 int tap = 1; ...` |

## The verdicts

**No verdict was changed.** `disposition`, `source_semantics`,
`callsite_audit`, `native_coverage`, `owner` and `reviewed_sha256` are
byte-identical on all 1044 rows, and the `counts` block is unchanged.

The evidence did not show a wrong verdict anywhere. The `w3rest` lane closed
its units by fixing the code, not by moving a verdict alone, and the two
rows that stay `partial` (the node rows) stay `partial` for a reason that is
still true: the identity of the node.

## The checks

```
python3 scripts/porting/update_scope_ledger.py --check --original-root /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle
{"mode": "check", "original_units": 859, "legacy_units": 934, "union_rows": 1044, ..., "stale_review_fingerprints": 0}

python3 scripts/porting/check_scope_ledger_reviews.py
{"mode": "check", "rows": 1044, "invalid_reviews": 0, "stale_review_fingerprints": 0, "counts_match": true}
```

Both exit `0`. The row order of the 1044 rows, the top level keys and the
`counts` block are unchanged; only the `notes` of 14 rows and the `evidence`
of 28 rows differ.

## What was not restored, and why

* The two rows the union merge also dropped (`BlockLifterRenderer`,
  `LifterItemColor`) are **not** the units of this lane, and the notes that
  `work` carries for them today are the *newer* ones of the wave 3 levitator
  lane (commit `3bdef302`). Restoring the text of the `w3rest` branch would
  have replaced a current note with an older one, so they were left alone.
* The same holds for the twelve `common/entities/ai/inventory/*` rows, whose
  notes in `work` come from the later golems lane (commit `402a9cf9`).
* The defect itself is in `scripts/porting/merge_wave_branch.py`: `merge_ledger`
  never unions the `notes` and the `evidence`. **That file was not changed by
  this pass** — it is the tool of the leader and the fix belongs with it.
  Every wave branch that was merged into `work` with that tool lost its notes
  and its evidence the same way (the merge of `agent/w3rest` alone lost 20
  note strings over 16 rows).

## The commits

* `2bddbee7` — `docs(ledger): the notes of the w3rest lane say what the code does now`
  (the ledger: 14 rows of notes rewritten, the evidence of 28 rows restored,
  the two false evidence strings removed, the three wrong citations
  corrected).
* `59b8acc4` — `docs(ledger): the TileNode evidence points at the code of today`
  (the native citations of the `TileNode` row and the absolute path of the
  oracle on the machine of the audit).

The report of this pass is this file.
