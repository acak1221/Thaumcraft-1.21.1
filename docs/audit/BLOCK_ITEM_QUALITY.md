# Block, tile and item code-quality audit

Date: 2026-10-23. Baseline: `3340abe7`. Branch: `agent/c2blocks`.

## Scope, evidence and limits

The required `find … -type f -name '*.java' | wc -l` returned **331**:
**102 blocks, 76 tiles, 153 items; 36,334 baseline lines**. The changes retain
331 files and produce 36,336 lines. Helpers/interfaces count as files, not as
registered content. [Full file census](BLOCK_ITEM_QUALITY/files.tsv).

This is a maintainability review, not a statement that every suspicious path is
a reproduced gameplay defect. Java compiler ASTs inventory declarations, literals
and calls; manual review covers the responsibility, duplication, persistence,
world-access and family examples below. Mechanical counts are **not** claims of
4,289 manually proved method contracts. Every table under
[`BLOCK_ITEM_QUALITY/`](BLOCK_ITEM_QUALITY/) is part of this report and contains
full repository-relative `file:line` references. Locations describe the final
source unless explicitly marked baseline.

**Acceptance limit:** original-JAR identity/property parity for every registered
variant, runtime callback reachability, and malformed-data round trips were not
re-proved. No original JAR/decompile, other worktree, or primary checkout was
modified. No Gradle, server or client was run. This report must not be used as
exhaustive original-parity or runtime acceptance evidence.

| Checklist | Inspected/inventoried | Findings (including fixed) |
|---|---|---:|
| 1. Size/responsibility | 331 file sizes; 9 oversized implementations reviewed | 9 |
| 2. Duplication | 4,289 method/constructor declarations; 37 substantial exact-body groups | 80 pairs |
| 3. Constants/literals | 10,838 AST literals; 5,484 numeric occurrences; 77 ResourceLocation calls; 360 local literal schema-key groups | 298 |
| 4. Null/unloaded world | 1,914 world-access lines; 387 directly in tick/use/save/load entry methods | 4 |
| 5. Component/NBT boundary | 1,006 NBT/schema-like operations; 116 save/load overrides | 10 |
| 6. Dead code | 535 private methods, 1,619 field declarations, 2,045 annotated overrides inventoried | 2 |
| 7. Consistency | 89 use entry methods, 38 tooltip overrides, 116 save/load overrides | 3 |
| 8. Registration/identity | 255 block/item-package files mapped; 91 config files; 984 registration/property lines indexed | 2 |

**408 findings/observations: 0 blockers, 21 should-fix, 387 nits.** Counts overlap
conceptually across checklists: for example, a repeated serializer appears in
both the duplication and schema-key review. A repeated-key finding is one
`(file,key)` group, not one occurrence or every combinatorial pair. This keeps
the 292 low-severity key findings distinguishable from substantive review issues.

## 1. Class size and responsibility

Inspected: **331 files**, all **9 files over 400 lines**. Counts below are the
baseline line counts (the mirror is 411 after adding four key declarations).
Classes nested in a file are included in that file's count, not falsely treated
as a separate 400-line class. No class was split.

| Finding / severity | File:line | Lines | Distinct responsibilities; split recommendation |
|---|---|---:|---|
| S01 should-fix | `common/src/main/java/thaumcraft/common/blocks/BlockEldritch.java:86` | 421 | Variant/property construction, collision/crust masks, lighting/drops, BE selection/ticking, eye/key interaction, destruction effects. Extract variant metadata/factory policy; keep native block callbacks together. |
| S02 nit | `common/src/main/java/thaumcraft/common/blocks/FluxFiniteBlock.java:66` | 416 | Finite-fluid geometry, neighbor scheduling, vertical/horizontal flow, transactional observations/commit. Cohesive fluid algorithm; do not split by arbitrary line count. A separately testable transaction helper is optional. |
| S03 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileArcaneBore.java:89` | 872 | Physical inventory/menu, mining scan/raycast, tool/focus stats, work ownership/snapshots, lifecycle/serialization fences, client visuals and disk/client schemas. Extract persistence DTO/parser and visual state first; keep atomic work authority in the owner. |
| S04 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileCrucible.java:78` | 597 | Water transactions, heat/bellows, recipe consumption, essentia, world ejection, saved pending-output retry queue. Extract pending-output storage/retry owner; preserve debit/ejection ordering. |
| S05 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileInfusionMatrix.java:120` | 594 | Structure scanning, ingredient/session selection, XP/essentia payments, craft completion, lifecycle reentrancy, session serialization and render sync. Extract immutable session schema/validation and structure scanner; do not move transaction guards casually. |
| S06 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:65` | 407 | Link discovery/repair, remote world lookup, transport, instability/vis, buffered item ejection, hopper facade and NBT/network persistence. Share link payload/parser with essentia mirror; keep their transport semantics separate. |
| S07 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileNode.java:85` | 900 | Aspect state, natural generation, pearl/transduction mutations, wand drain, hungry-node combat/ecology, stability/orb emission, regeneration and persistence. Extract ecology and persisted node-state parsing; retain single mutation authority. |
| S08 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileResearchTable.java:94` | 531 | Topology, inventory/menu, environmental bonus scan, research commits/revisions, evacuation and persistence. Extract bonus producer/scanner and schema parser; keep material revision checks near commits. |
| S09 should-fix | `common/src/main/java/thaumcraft/common/items/golems/GolemData.java:36` | 532 | Configuration value, compatibility constructors, codec/stream transport, exact-number validation, core/upgrade/ghost-slot policy, copy/equality/hash semantics. Extract codecs and configuration validation without exposing mutable ItemStacks. |

## 2. Duplication

Inspected: **4,289 declarations**. AST-rendered bodies with whitespace removed
and more than 90 characters produce **37 cross-file groups / 75 cross-file
pairs**. Each pair is listed with severity and both locations in the
**[complete duplication table, D001–D075](BLOCK_ITEM_QUALITY/duplication.tsv)**.
It includes constructors as well as methods; it excludes tiny return/default
boilerplate and does not claim alpha-renamed or every semantic clone is an exact
match. These are **nits**, not 75 reasons to introduce a giant universal base class.

Notable families in that table: candle support/placement/particles, family
variant-property handoff, result-enum conversion, node harvesting, native-owner
`current()` predicates, essence tooltips/stream encoding, and Thaumium/Void tool
attributes. An exact copy is sometimes clearer than an abstraction; extract only
when the shared contract can be named and tested. No duplicated independent
"right tool" check was established: `ItemPrimalCrusher.java:52` and `:105` in
`common/src/main/java/thaumcraft/common/items/equipment/` are different callback
and helper roles, not proof of a copy/paste defect.

Additional manually reviewed sub-body/small-method pairs:

| Finding / severity | First file:line | Second file:line | Shared responsibility |
|---|---|---|---|
| D076 nit | `common/src/main/java/thaumcraft/common/tiles/TileWardingStone.java:83` | `common/src/main/java/thaumcraft/common/tiles/TileWardingStoneFence.java:43` | Identical `count` load override. |
| D077 nit | `common/src/main/java/thaumcraft/common/tiles/TileWardingStone.java:88` | `common/src/main/java/thaumcraft/common/tiles/TileWardingStoneFence.java:48` | Identical `count` save override. |
| D078 nit | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:306` | `common/src/main/java/thaumcraft/common/tiles/TileMirrorEssentia.java:230` | Five-field link reader; constants now remove spelling repetition locally, not schema duplication between classes. |
| D079 nit | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:323` | `common/src/main/java/thaumcraft/common/tiles/TileMirrorEssentia.java:239` | Five-field link writer, also partially copied into `TileMirror.java:65`. |
| D080 nit | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:262` | `common/src/main/java/thaumcraft/common/tiles/TileMirrorEssentia.java:213` | Dimension resolution including the same misplaced null check. |

Repeated schema-key sites are exhaustively enumerated **within the extracted
operation set** in [repeated-keys.tsv](BLOCK_ITEM_QUALITY/repeated-keys.tsv).
Each row names all sites; any two locations on a row form the repeated-literal
pair. Unrelated uses of a spelling such as `amount` are not an instruction to
couple otherwise independent schemas.

## 3. Magic numbers and string literals

Inventoried **10,838 literal AST nodes**, including **5,484 numbers**:
**846** inside static-final field initializers and **4,638** inline or instance
initializers. [numbers.tsv](BLOCK_ITEM_QUALITY/numbers.tsv) groups every numeric
occurrence by file/value/classification and lists every source location, including
0/1, geometry coordinates, colors and legacy ordinal tables. Classification is
lexical: a number inside a static-final codec lambda is not automatically a named
gameplay constant. A repeated 0 or coordinate is not itself a review failure.

**77 ResourceLocation construction/parse calls** are individually listed in
[resource-locations.tsv](BLOCK_ITEM_QUALITY/resource-locations.tsv), including
dynamic identifiers. **360 `(file,literal key)` groups** occur in extracted
NBT/codec operations; **292 repeat locally** and remain as **K001–K292, nit** in
[the key table](BLOCK_ITEM_QUALITY/repeated-keys.tsv). Declare schema-scoped
constants; preserve case, value, numeric tag type and omission semantics.
Do not "normalize" `Aspect`, `aspect`, `Aspects` across different owners.

Other findings (six, including four fixed):

| Finding / severity | File:line | Observation |
|---|---|---|
| K293 nit — fixed | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:39` | Hoisted `linkX` used by dropped-item data, load and save to `LINK_X_KEY`. |
| K294 nit — fixed | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:40` | Same for `linkY`. |
| K295 nit — fixed | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:41` | Same for `linkZ`. |
| K296 nit — fixed | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:42` | Same for `linkDim`. |
| K297 nit | `common/src/main/java/thaumcraft/common/blocks/BlockAlembic.java:121` | Jar capacity 64 repeated at :124/:126 instead of the named `JarContents.CAPACITY` (`common/src/main/java/thaumcraft/common/tiles/JarContents.java:7`). Value is correct; intent has drift risk. |
| K298 nit | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:192` | Literal ejection offsets/speed at :192–194 duplicate the named mirror geometry policy used by `common/src/main/java/thaumcraft/common/items/relics/ItemHandMirror.java:178`. Share named geometry only after checking both contracts; no numeric value was changed. |

## 4. Null and unloaded-world safety

Inspected/indexed **1,914 world-access lines**, **387** directly in `tick`,
`serverTick`, use callbacks, `saveAdditional` or `loadAdditional`.
[world-access.tsv](BLOCK_ITEM_QUALITY/world-access.tsv) records each expression
and enclosing method, including helper methods called by those entry points.
This is a lexical access census, not a dataflow proof that 1,914 lines are unsafe.
Native tick/use Level parameters are non-null contracts; adding a null guard to
every such access would hide invariant violations, not improve code quality.
`instanceof Tile…` lookups correctly handle absent BEs throughout the sampled
entry paths. Lamp candidate selection checks loaded chunks before reading them;
the crucible/bore/infusion paths have explicit ownership/reentrancy machinery.

| Finding / severity | File:line | Review concern; left unchanged |
|---|---|---|
| N01 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:264`; `common/src/main/java/thaumcraft/common/tiles/TileMirrorEssentia.java:216`; `common/src/main/java/thaumcraft/common/items/relics/ItemHandMirror.java:143` | `tryParse` may yield null but is passed into `ResourceKey.create` before testing the resulting key. Check the parsed location before key construction; the current null test documents the wrong nullable boundary. No runtime crash claim is made here. |
| N02 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:80`; `common/src/main/java/thaumcraft/common/tiles/TileMirrorEssentia.java:213`; `common/src/main/java/thaumcraft/common/items/relics/ItemHandMirror.java:101` | Remote mirror lookup has no explicit target-chunk-readability policy. The tick/use chain can cross dimensions and read a remote BE without distinguishing unavailable from absent. Specify/test the intended loading policy before changing link invalidation or loading behavior. |
| N03 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileArcaneLamp.java:53` | `removeLights` scans its cleanup cube without `hasChunkAt`, unlike spreadLight's candidate check at :38. Review chunk-edge cleanup contract; do not silently change original cleanup coverage. |
| N04 nit | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:176` | Public `eject()` dereferences nullable inherited `level` without making its attached-server precondition explicit. Tick caller normally satisfies it; narrow visibility or document/centralize preconditions rather than assert an ordinary tick NPE. |

## 5. Component/NBT boundary

Inspected **116 save/load declarations** and indexed **1,006 NBT/schema-like
operations** in [nbt-operations.tsv](BLOCK_ITEM_QUALITY/nbt-operations.tsv).
The inventory includes typed getters/setters, known tag/map receiver operations,
and codec field declarations. It is intentionally labeled schema-like because
`MapLike.get` and validation-set membership are not CompoundTag reads. Indirect
`AspectList`/`ContainerHelper` calls and loops with dynamic key names require the
owner/helper review; the inventory is not a whole-program type-resolved NBT proof.

Good patterns: `RetainedDataBlockEntity.java:21` keeps unread ownership opaque;
`TileResearchTable.java:480` validates slots/counts/bonus rows before mutation;
`TileFocalManipulator.java:319` checks paid-job bindings; `GolemData.java:89`
refuses malformed optionals/unknown fields and validates exact numbers;
`CrystalEssenceData.java:59` strips partial-list recovery. These are useful family
patterns, not justification for rewriting preserved canonical behavior.

| Finding / severity | File:line | Type / absence / malformed-value assessment |
|---|---|---|
| P01 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileJar.java:163`; `common/src/main/java/thaumcraft/common/tiles/JarContents.java:33`; `common/src/main/java/thaumcraft/common/tiles/TileAlembic.java:143` | Missing data defaults empty, but wrong-type/unknown aspect and invalid amounts also become empty or clamped. Jar restore discards the original invalid amount. Separate absent/new placement from supplied-but-invalid input, with an explicit preservation policy. |
| P02 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileJarNode.java:205` | Load clears live aspect maps before validation, skips unknown/malformed entries and defaults invalid type/modifier ordinals. Missing base list legitimately falls back to current aspects, but invalid supplied data is indistinguishable from that fallback. Parse/validate a complete temporary value first. |
| P03 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileTube.java:323` | Amount is clamped to 0..1, unknown type becomes null, bad side becomes NORTH and wrong-length side array is ignored. An omitted partial-client field can be intentional; present invalid storage should not silently acquire those defaults. |
| P04 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileAlchemyFurnace.java:345`; `common/src/main/java/thaumcraft/common/tiles/TileArcaneWorkbench.java:103`; `common/src/main/java/thaumcraft/common/tiles/TileChestHungry.java:240` | Clear-and-`ContainerHelper.loadAllItems` delegates item decoding without this family's strict slot/whole-payload retention contract. Absent Items is a legitimate empty snapshot; document/test refusal for wrong-type lists, duplicate slots and unsupported components instead of assuming helper tolerance is preservation. |
| P05 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileMemory.java:65`; `common/src/main/java/thaumcraft/common/tiles/TileHole.java:91` | Remembered block-state tags use untyped `contains` plus permissive block-state decoding/AIR fallback. A present invalid remembered state is materially different from an intentionally absent state. Preserve the opaque raw data before a future save overwrites it. |
| P06 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileOwned.java:83` | Absent owner/access defaults are explicit, but present wrong-type ownership/access entries also become empty strings/list. Ownership schema deserves typed validation and a single policy; no claim of a demonstrated permission bypass. |
| P07 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:303`; `common/src/main/java/thaumcraft/common/tiles/TileMirrorEssentia.java:228` | Item mirror validates buffered stacks, not link scalar tag types or dimension syntax; essentia mirror validates neither. Absent link and malformed persisted link should be distinct before targetWorld is called. |
| P08 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileThaumcraftInventory.java:125`; `common/src/main/java/thaumcraft/common/tiles/TilePedestal.java:198` | CustomName JSON decoding occurs outside the earlier retention validator; untyped contains also accepts wrong tag types. The inventory may be validated while the complete owner payload is not. Put optional-name validation inside the same parse-before-commit boundary. |
| P09 should-fix | `common/src/main/java/thaumcraft/common/items/WispEssenceData.java:35` | Uses `Codec.intRange` and list `comapFlatMap`, unlike CrystalEssenceData's exact-integer/partial-result refusal. Constructors validate converted integers, not the original fractional/oversized representation. Make parser policy explicit and add malformed component tests before changing it. Preserve the intentional nonempty wisp versus empty crystal distinction. |
| P10 nit | `common/src/main/java/thaumcraft/common/tiles/TileEssentiaReservoir.java:568` | Over-capacity list intentionally becomes empty to preserve original behavior; tube buffer instead quarantines at `common/src/main/java/thaumcraft/common/tiles/TileTubeBuffer.java:219`. This is a documented canonical exception, not permission to "fix" it in a style pass. Keep an explicit regression test/policy annotation. |

None of these behavior-sensitive findings was changed. No blanket claim that all
absent/wrong-type data must throw is intended: partial update tags and fresh
placements need their own explicitly tested schemas.

## 6. Dead code

Inventoried **535 private methods**, **1,619 fields** (including record component
declarations), **2,045 annotated overrides**; full declarations are in
[methods.tsv](BLOCK_ITEM_QUALITY/methods.tsv) and
[fields.tsv](BLOCK_ITEM_QUALITY/fields.tsv). A same-file identifier census found
**0 private-method singletons**. The only private-field singleton candidates
were `TravelGearRules.java:6` record components `sustained`/`consumed`; generated
record accessors make those false positives. **0 proven write-only fields** and
**0 proven obsolete callbacks** were established. Token presence is not semantic
reachability; compilation verifies signatures, not that the engine calls them.
No uncertain method/field was deleted.

| Finding / severity | File:line | Outcome |
|---|---|---|
| DC01 nit — fixed | `common/src/main/java/thaumcraft/common/tiles/RetainedDataBlockEntity.java:17` (baseline :20) | Removed loadAdditional override that only forwarded identical arguments to super. Inherited callback is still present and used. This was redundant live code, not an uncalled method. |
| DC02 nit — fixed | `common/src/main/java/thaumcraft/common/tiles/RetainedDataBlockEntity.java:17` (baseline :21) | Removed equivalent saveAdditional forwarder. No serialization branch or guard was removed. |

## 7. Consistency

Inspected/indexed **89 use entry declarations, 38 tooltip declarations, 116
save/load declarations**. Candle support behavior and the crystal/wisp/bean
discovered/unknown-aspect tooltip style agree; do not invent a tooltip mismatch
from whitespace differences. The crystal may be initialized-empty while wisp
essence may not: that is a semantic distinction, not an inconsistency to erase.

| Finding / severity | File:line | Family review |
|---|---|---|
| C01 should-fix | `common/src/main/java/thaumcraft/common/tiles/TileJar.java:163`; `common/src/main/java/thaumcraft/common/tiles/TileTubeBuffer.java:207` | Essentia owners use empty/clamp, whole-payload retention and local rejection envelopes. Define one family-level malformed-data contract with documented canonical exceptions (P01/P03/P10), not ad-hoc per-machine policy. |
| C02 nit | `common/src/main/java/thaumcraft/common/tiles/TileThaumcraftInventory.java:113`; `common/src/main/java/thaumcraft/common/tiles/TileResearchTable.java:466`; `common/src/main/java/thaumcraft/common/tiles/TileMirror.java:335` | Inventory payloads use Items/ItemsSynced, Inventory+StoredCount and top-level native stack entries. These have justified original/native transport differences; share named schema adapters/documentation, not a save-key rename. In particular mirror's top-level stack format must not be replaced with nested Stack. |
| C03 nit | `common/src/main/java/thaumcraft/common/blocks/BlockColoredTallowCandle.java:29`; `common/src/main/java/thaumcraft/common/blocks/BlockTallowCandle.java:30` | Same family has compressed one-line callbacks versus readable multi-line callbacks. Apply existing readable formatting when next editing the colored family; do not bury domain changes in a broad reformat. |

## 8. Registration and identity

**255 block/item-package files** mapped (102+153, not 255 registries), plus 76
tile files, against **91 config source files**. All **331** class-file rows are in
[registration-class-map.tsv](BLOCK_ITEM_QUALITY/registration-class-map.tsv).
[registration.tsv](BLOCK_ITEM_QUALITY/registration.tsv) records **984 source
lines** containing registration/helper calls or properties. Loop-generated
registrations, ordinary vanilla Block/Item instances and aliases mean these
counts cannot honestly be presented as a resolved registry-entry count. Helpers
with no direct config reference are not automatically unregistered content.

Reviewed family examples:

* `common/src/main/java/thaumcraft/common/config/ThaumcraftTubes.java:34`–57:
  consistent tube/valve/filter/buffer metal profile; reservoir deliberately has
  different strength. Comments explain original resistance-unit conversion.
* `common/src/main/java/thaumcraft/common/config/ThaumcraftMasonry.java:25`–47:
  stone/bricks versus stairs/slabs differ intentionally because original
  metadata overrides did not propagate through the old stairs constructor.
* `common/src/main/java/thaumcraft/common/config/ThaumcraftColoredCandles.java:26`:
  metadata 0 remains existing white, 1..15 flatten into dye-named IDs. Both
  constructors set .1 hardness, wool sound, light 14 and no collision.
* `common/src/main/java/thaumcraft/common/config/ThaumcraftLamps.java:25`–37:
  lamp emits 15, amber family matches, invisible light deliberately has no item.
  Original piston interfaces also supply per-state behavior: a Properties-only
  scan does not establish full push-reaction parity.

| Finding / severity | File:line | Maintainability issue |
|---|---|---|
| R01 nit | `common/src/main/java/thaumcraft/common/config/ThaumcraftTubes.java:36`; :39, :45, :48, :51, :112 | Same .5/3 metal tube-family profile is constructed repeatedly. Extract a fresh-properties factory (never reuse a mutable Properties instance) to prevent neighboring variants drifting. |
| R02 nit | `common/src/main/java/thaumcraft/common/config/ThaumcraftColoredCandles.java:28`; `common/src/main/java/thaumcraft/common/blocks/BlockColoredTallowCandle.java:29`; `common/src/main/java/thaumcraft/common/blocks/BlockTallowCandle.java:32` | Property authority is split between registration and mutating constructors. Reviewing registry Properties.of alone conceals hardness/sound/light. Keep final-property provenance adjacent to registration or use a clearly named fresh profile. Values currently agree. |

**Not certified:** every original registry name, every flattened variant alias,
every hardness/resistance/light/sound/push value against the original private
oracle. The source map is a review index, not a substitute for that missing
per-entry original comparison. No unsupported rename/property correction made.

## Fixes and verification

Only two production files changed: four local mirror key constants replace
twelve literal call arguments, and two redundant superclass forwarders were
removed. No schema bytes, key spelling, world lookup, inventory semantics,
canonical policy or runtime algorithm changed. Source responsibilities were not
split; all other findings remain open.

Verification actually executed:

1. Java 21 `javac` baseline of **1,445 common sources**: passed.
2. `javac --release 21 -proc:none -sourcepath ""` on both changed sources against
   the remapped Minecraft jar/dependencies plus this worktree's
   `common/build/classes/java/main`: passed.
3. Final all-common-source `javac` (**1,445 sources**): passed; only existing
   deprecation/unchecked notes. No Gradle invoked.
4. Source-equivalence assertions against baseline: expanding the four constants
   recreates the original mirror file exactly; removing only the two identical
   super-forwarder lines recreates the final retained-data base exactly. All TSV
   source references resolve to existing files and in-range lines.

Classpath reused the cached remapped Minecraft jar under
`/home/cyberfox-kali/.gradle-leader/caches/fabric-loom/minecraftMaven/`, the
existing `/tmp/opencode/a2quality/minecraft-access-overlay.jar`, cached dependency
jars and this worktree's classes. The overlay is reused access-widening support,
not a stub/replacement mod implementation. Argument files/logs are transient at
`.tmp/c2blocks/{baseline.args,changed.args,baseline.log,changed.log,final.log}`.
No gameplay, malformed-save fixture, client, multiplayer or server pass claimed.
