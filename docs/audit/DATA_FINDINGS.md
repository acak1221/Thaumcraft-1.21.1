# Data/save integrity audit — h5data — 2026-10-23

Baseline: `e79b8558`, branch `agent/h5data`. Native paths below are relative to
`common/src/main/java/thaumcraft/`; oracle paths are relative to
`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/thaumcraft/`.
Fresh native worlds only. Malformed-save recovery is not legacy-world migration.

## Coverage and counting

| Hunt | Checked | Result / limit |
| --- | ---: | --- |
| Save/load | 69 source pairs | 59 explicit BE pairs, 6 SavedData pairs, 3 player owners, 1 golem entity pair. Schema/body inspection, **not 69 executed round-trip tests**. Four confirmed asymmetric state/recovery paths fixed below. |
| Player data | 3 owners × 4 transitions = 12 paths | Knowledge (including research, scans, warp), accessories, runic charge; dimension, reconnect, respawn, restart inspected. Live multiplayer/restart execution not claimed. |
| Components | 26 registrations indexed; 25 persistent | Seven value families inspected deeply (wand, phial, label, crystal, wisp, vis amulet, banner); no proven component Java-type mismatch. Remaining codecs not exhaustively behavior-tested here. |
| Silent loss | 15 loader sites | Three fixed; 12 source-established destructive malformed/missing-registry-input paths remain (F05–F09). One site means one owner loader, not each field or catch. |
| Item/block retirement | 4 requested owner families | Jar, crucible, golem, research table compared at source level. No new gameplay acceptance claim. |
| Registry stability | 8 owner families screened | Golem, growth queue, pedestal, research table, mirror, magic box, remembered block, infusion; missing-ID retention varies. |

The 59 BE pairs are every class declaring both `saveAdditional` and
`loadAdditional` under `common/src/main/java`. One additional save-only class,
`TileArcaneLamp`, writes redundant orientation; native blockstate owns direction.
The load-only advanced-furnace nozzle resets transient cached links.
The six SavedData owners are GrowthLampTicks, DeferredItemDrops, AspectOrbQueue,
OuterLandsProvider.MazeSavedData, OuterLandsBossRooms.BossSavedData, and
TileCrucible.CrucibleActions. Other entity subclasses and all codecs are **not**
claimed exhaustively audited.

### F01 — Medium — Empty inventory snapshots retain old contents — FIXED

* Oracle `common/tiles/TileThaumcraftInventory.java:94-102,119-130` creates a fresh
  inventory before applying synced and disk lists.
* Native `common/tiles/TileThaumcraftInventory.java:112-121,145-152` previously
  only assigned listed entries. Loading an empty snapshot into a populated owner
  kept both previous stacks; an empty synced update after removing a wand kept
  its stale client representation. The actual subclass is TileWandPedestal.
* Reachable without corrupt NBT: insert wand, receive update, remove wand,
  receive empty update. Disk replacement is also a complete snapshot, not a merge.
* Fix: clear the fixed-size NonNullList before decoding either group, matching
  the oracle. Three actual-class JUnit cases cover populated round trip, empty
  replacement, and successive synced insert/remove snapshots in
  `TileInventorySaveIntegrityTest`.

### F02 — High — Wrong accessory-list element type becomes empty inventory — FIXED

* Oracle `common/items/wands/WandManager.java:81-84` accesses four player-owned
  Baubles slots. Their persistence implementation is an external Baubles
  dependency, **not present in this Thaumcraft oracle**; no oracle claim about
  malformed Baubles-file recovery is made.
* Native `common/lib/baubles/PlayerAccessoryInventory.java:59-80`: checking only
  that `Items` is a list accepted a nonempty string list. `getList(..., COMPOUND)`
  hid it as empty, and the next save erased it instead of using existing quarantine.
* Corrupt-save entry point: `PlayerAccessories.load:55-56`. Native integrity
  defect, not a newly discovered difference in legacy Baubles gameplay.
* Fix: validate raw element type before typed access. Existing inert quarantine
  now preserves and re-saves the complete owner. Two JUnit cases in
  `AccessorySaveIntegrityTest` cover malformed re-save/copy isolation and
  empty-versus-absent inventories. Null owner/registry is used only because these
  fixtures contain no decodable stacks and need no gameplay player.

### F03 — Medium — Paid growth updates disappear on malformed collections — FIXED

* Oracle `common/tiles/TileArcaneLampGrowth.java:124-131` debits charge and schedules
  a one-tick world update. The native persistent queue adapts that scheduled work.
* Native `common/lib/GrowthLampTicks.java:62-91`: malformed `Pending` or `Rejected`
  collection types were read as empty and then overwritten on save. Individual
  bad rows were already retained; collection-level errors were not.
* Fix: quarantine the entire owner before reading either collection. Extended
  `GrowthLampTicksTest` checks both wrong list element types and scalar-instead-
  of-list, including an unchanged second save/load cycle.

### F04 — High — Crucible's paid-output queue drops malformed collections — FIXED

* Oracle `common/tiles/TileCrucible.java:78-91` persists its paid material state;
  :239-243 emits actual crafted item entities (the oracle has no native deferred queue).
* Native `common/tiles/TileCrucible.java:531-569` previously validated individual
  outputs but not the `Outputs`, `Spills`, or `Quarantined` containers. A wrong
  list type became empty, losing paid output or even previously retained evidence.
* Fix: preserve the entire owner on collection-shape errors, just as the existing
  unknown-version path does. `CrucibleActionSaveIntegrityTest` invokes the **real
  private SavedData loader** reflectively; it checks all three collections with
  both malformed shapes, repeated saves, and an empty queue. No production
  visibility or public enqueue capability was added for testing.

## Remaining source-established malformed-save losses

These are reachable from on-disk corrupt data or a previously saved missing
registry ID. They are not alleged normal valid-save regressions, and no live
server reproduction was performed. Fixing safely requires preserving original
payloads **and fencing every mutation/retirement path**, not just adding a catch.

### F05 — High — Knowledge/attachment type errors reset player-owned data — OPEN (3 sites)

* Native `common/lib/research/PlayerKnowledge.java:178-205` resets first, then
  trusts typed list/scalar getters. A string-valued `researchComplete`, or a
  wrong-element `discoveredAspects`, becomes empty/default and is rewritten.
  `dataVersion` is written at :157 but never checked. Valid unknown research and
  aspect strings are retained, which is good for removed/reinstalled addons.
* Native `common/mixin/PlayerKnowledgeMixin.java:24-33` and
  `common/lib/baubles/PlayerAccessories.java:54-56` ignore a present non-compound
  outer attachment; the subsequent save replaces it with a default owner.
* Oracle `common/lib/research/ResearchManager.java:611-678,733-778` attempts a
  backup on unreadable files and logs errors; :681-724 loads research/aspects/scans.
  External Baubles serialization is absent, as noted in F02.
* Required fix: version/type-aware retained player attachment, explicit repair
  policy, fenced gameplay mutations, and clone/sync handling. Do not throw out of
  player loading and assume vanilla will preserve the original player file.

### F06 — High — Failed stack decode is re-saved as absent stock — OPEN (6 sites)

| Native loader | Oracle round-trip contract |
| --- | --- |
| `common/tiles/TilePedestal.java:176-182` | `common/tiles/TilePedestal.java:115-152` |
| `common/tiles/TileResearchTable.java:467-490` | `common/tiles/TileResearchTable.java:71-112` |
| `common/tiles/TileMirror.java:278-296` | `common/tiles/TileMirror.java:321-344` |
| `common/tiles/TileMagicBox.java:239-259` | `common/tiles/TileMagicBox.java:115-149` |
| `common/tiles/TileArcaneFurnace.java:333-344` | `common/tiles/TileArcaneFurnace.java:86-114` |
| `common/tiles/TileThaumcraftInventory.java:145-152` | `common/tiles/TileThaumcraftInventory.java:94-147` |

All six use failed `ItemStack.parse` → EMPTY and then omit the row on save. A
saved item from an uninstalled addon is a concrete input, not an imaginary null.
The oracle citations establish ownership/serialization, **not** a promise that
1.7.10 recovered unknown numeric item IDs. These are native integrity-hardening
gaps. The research table additionally truncates incoming rows and drops invalid
bonus tags; a malformed bonus payload is not retained either.
Required fix: opaque original row/owner custody, block normal use until repaired,
and ensure breaking the block does not discard retained NBT. F01 fixes empty
replacement, not this separate base-inventory failure path.

### F07 — High — Malformed maze data becomes a new empty world map — OPEN (1 site)

* Native `common/lib/world/OuterLandsProvider.java:83-104` accepts typed default
  getters; malformed `cells` becomes empty, duplicate coordinates overwrite,
  wrong scalar types become zero. Save discards the original map.
* Oracle `common/lib/world/dim/MazeHandler.java:89-139` loads/saves the persistent
  labyrinth map. Native map loss can remove navigation/room identity independently
  of the already generated blocks.
* Required fix must also fence generation/room operations while rejected; merely
  retaining NBT alongside an empty active map could generate overlapping mazes.

### F08 — High — Invalid infusion requirements become destructive sentinels — OPEN (1 site)

* Native `common/tiles/TileInfusionMatrix.java:499-509,543-550`: an invalid
  predicate is replaced with `Ingredient.EMPTY`; failed stack decode becomes
  empty. The following save writes the replacement instead of original bytes.
  A wrong-element `recipein` list bypasses even the sentinel and becomes no
  requirements. A partially paid session is a reachable saved owner.
* Oracle `common/tiles/TileInfusionMatrix.java:136-190` persists the selected
  recipe input/output/remaining requirements, rather than rematching a fresh job.
* Required fix: inert retained **whole session**, including debts and target
  binding. A sentinel may pause work, but is not lossless recovery.

### F09 — Medium — Advanced furnace accepts wrong-element aspect list — OPEN (1 site)

* Native `common/tiles/TileAlchemyFurnaceAdvanced.java:186-198` checks LIST but
  not its element type. `Aspects=["..."]` with omitted/zero `vis` becomes empty
  stock and passes validation, rather than reaching its existing quarantine.
* Oracle `common/tiles/TileAlchemyFurnaceAdvanced.java:54-81` persists aspect
  stock and heat/power caches. This is a malformed native-schema recovery gap.
* Required fix: raw collection validation plus actual block-entity regression
  coverage; also preserve malformed `RejectedAdvancedFurnace` envelopes.

## Lifecycle and retirement observations

* Knowledge is per-player, not a global name cache: `PlayerKnowledgeMixin:17`;
  `KnowledgeEvents:25-35` joins, clones via deep `copyFrom`, respawns and dimension
  syncs. All research/scans/aspect pools and four warp integers are written/read.
  Oracle `ResearchManager:638-660,733-746` establishes those persistent fields.
* Accessories: `PlayerAccessories:32-38,54-68` transfers remaining ownership on
  clone, respects keepInventory, restores a refused death spawn to its slot.
  Malformed inner owners remain inert; malformed outer attachment is F05.
* Runic charge: `RunicShield:23-49,126-134` scopes its map by server and player
  entity ID and persists charge, not clocks. Oracle `ResearchManager:642-644,740-741`
  also saves charge; death/entity-ID continuity needs the live check below.
* Jar: native `common/blocks/BlockJar.java:123-134` makes one dynamic drop with BE
  components when contents/filter exist, matching oracle `BlockJar:158-182`.
* Crucible: native BE :428-441 stores heat/water/aspects; the queue keeps failed
  paid results. F04 fixes malformed queue containers, not all spawn reentrancy.
* Golem: native `EntityGolemBase:2776-2827,2881-2897` keeps unread physical cargo
  verbatim and blocks unsupported owners; known water/essentia death loss is
  explicitly intentional (:2770-2773). Oracle entity :581-674 stores owner,
  configuration and carried items. No new lost-cargo defect proved here.
* Research table: native :403-435 queues physical contents before clearing;
  oracle `common/blocks/BlockTable.java:127-129` uses InventoryUtils drops.
  F06 concerns decoding before that retirement path, not ordinary valid breaking.

Redundant persisted direction/cache fields are not reported as content losses:
alembic/bellows/lamps/charger directions live in native blockstate, furnace `Vis`
is a recomputed aspect cache, and client-only link/lid/infusion render fields do
not need disk writers. Resetting transient counters is generally oracle behavior.

## Unproven / follow-up list

1. Runic charge through a real death/End respawn: entity-ID map ownership versus
   clone lifecycle. No proved oracle divergence; do not add a copy blindly.
2. Removed block registry entries in TileMemory (:65-70) and TileHole (:91-97)
   may turn remembered blocks into air; exact native registry-removal/reinstall
   scenario and repair policy have not been run.
3. Node-jar explicit empty base falls back to current aspects (:221-230). Need a
   legitimate nonempty-current/empty-base producer before calling it a defect.
4. All 25 persistent components need complete registry-aware malformed/absent/
   empty codec execution, especially partial DataResults and addon removal.
5. End-to-end dedicated-server restart, chunk unload, canceled spawn callbacks,
   and multiplayer transitions remain unexecuted in this audit checkpoint.

## Verification

Regression tests added for F01–F04. Build/test outcomes are recorded in
`docs/analysis/2026-10-23-agent-h5data.md`; source proof is distinct from executed
JUnit/GameTest evidence. No original/donor files were changed.
