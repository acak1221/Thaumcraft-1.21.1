# Wave 3 — the `w3rest` agent — the remaining server-side and shared `partial` units

Worktree: `/home/cyberfox-kali/wt/w3rest` (branch `agent/w3rest`).
Unit list: `/home/cyberfox-kali/units/w3rest.txt` (28 units).

The oracle is `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, 859/859
fingerprints). `/home/cyberfox-kali/OpenTC4` was only used as a second opinion.

## The compile proof

No Gradle was run. The proof is a direct `javac` of every source set of the port against the
remapped 1.21.1 jar of the Loom cache:

| source set | result |
|---|---|
| `common/src/main/java` | compiles |
| `fabric/src/main/java` | compiles |
| `neoforge/src/main/java` | compiles |
| `common/src/gameTest/java` | compiles |
| `fabric/src/clientTest/java` | compiles |
| `common/src/test/java` | compiles |

The classpath needed three corrections that the raw Loom cache does not carry:

* `BlockEntityType$BlockEntitySupplier` is `private` in the raw merged jar (both in its own
  access flags and in the `InnerClasses` attribute); Loom widens it in the dev environment, so
  the local jar was widened the same way.
* the access wideners of the project and of the twenty Fabric API modules were applied to the
  local jar (they widen `SpawnPlacements.register`, `Projectile.<init>`, `ItemEntity.pickupDelay`
  and `LivingEntity.lastHurtByPlayerTime`).
* the Fabric API jars of the leader's `.gradle/loom-cache/remapped_mods` are used, because the
  plain `modules-2` jars are still in the intermediary names.

## The units

### 1. `thaumcraft/common/tiles/TileResearchTable.java` — CLOSED

* **Oracle** `thaumcraft/common/tiles/TileResearchTable.java:398-422` — the copy of a solved note
  requires **one paper** (`Items.field_151121_aF`) and **one black dye** (`Items.field_151100_aR`
  with the metadata zero); both are consumed.
* **Port before** `common/src/main/java/thaumcraft/common/lib/network/ResearchTableCopyNetworking.java:47`
  looked for `Items.INK_SAC`, `common/src/main/java/thaumcraft/common/tiles/TileResearchTable.java:342`
  accepted an ink sac and `common/src/main/java/thaumcraft/common/container/ContainerResearchTable.java:149`
  and `common/src/main/java/thaumcraft/client/gui/GuiResearchTable.java:104` showed the ink sac as
  the second material of the copy.
* **Fix** the copy costs `Items.BLACK_DYE` (the 1.21.1 name of the original `Items.dye` of the
  metadata zero) everywhere: the network receiver, the commit of the table, the menu admission,
  the tooltip of the GUI and the two game test probes of the harness.
* **Test** the copy cost is asserted by the existing `ResearchNoteCopyRuntimeChecks` of the
  harness; no new unit test was needed for the item identity itself.

### 2. `thaumcraft/common/lib/events/EventHandlerWorld.java` — CLOSED for the boss rule

* **Oracle** `thaumcraft/common/lib/events/EventHandlerWorld.java:225-245` — the original cancelled
  the `BlockEvent.PlaceEvent` and the `BlockEvent.MultiPlaceEvent` of a placement that stood in
  the Outer Lands, in a labyrinth cell of a boss room (the feature two to five) and while a boss
  of the original `EntityThaumcraftBoss` family was alive within thirty-two blocks.
* **Port before** no such rule existed anywhere.
* **Fix** `common/src/main/java/thaumcraft/common/lib/events/BossPlacementRules.java` holds the
  rule (the pure decision `blocksPlacement(...)`, the labyrinth cell test and the boss search
  with the original radius of 32.0). The loader half is **one hook per platform**, as the
  Architectury common artifact and the NeoForge event do not share a signature:
  `fabric/src/main/java/thaumcraft/fabric/FabricBossPlacement.java` (the Architectury
  `BlockEvent.PLACE`, whose Fabric implementation cancels the placement with `InteractionResult.FAIL`)
  and `neoforge/src/main/java/thaumcraft/neoforge/NeoForgeBossPlacement.java` (the native
  `BlockEvent.EntityPlaceEvent` and `BlockEvent.EntityMultiPlaceEvent`).
* **Cross cutting fix** the original praetor of the crimson cult was an `EntityThaumcraftBoss`
  (oracle `thaumcraft/common/entities/monster/boss/EntityCultistLeader.java:70-72`), so the
  native praetor now carries the same boss marker
  (`common/src/main/java/thaumcraft/common/entities/monster/EntityCultistLeader.java`) and names
  itself after its champion modifier exactly as the original did (oracle
  `EntityCultistLeader.generateName:105-110`). Without it the praetor was invisible to the boss
  search and to the champion subsystem.
* **Still open** the chunk regeneration queue of the original has no 1.21.1 counterpart; see the
  `ServerTickEventsFML` row.

### 3. `thaumcraft/api/nodes/INode.java` and `thaumcraft/common/tiles/TileNode.java` — the enum
   migration is CLOSED, the identity stays open

* **Oracle** `thaumcraft/api/nodes/INode.java:12-29` — the published contract with the nine
  members; `thaumcraft/api/nodes/NodeType.java` and `NodeModifier.java` — the published enums with
  six and three constants; `thaumcraft/common/tiles/TileNode.java:75-77` — **the original node
  itself implements `INode`**; `:240-271` — the type, the modifier and the base are written
  through that contract.
* **Port before** the native node kept its own nested `TileNode.NodeType`/`TileNode.NodeModifier`
  enums, so the published enums and the native ones were two types, and the node could not
  implement the published interface at all. The nested enums were used in 166 places in 25 files.
* **Decision** the **published enums are canonical**. The nested enums are gone; the node, the
  jar, the converter, the ecology, the generation, the scan aspects and the two renderers use
  `thaumcraft.api.nodes.NodeType`/`NodeModifier` directly, and `TileNode` now
  `implements INode`. `NodeAdapter` stays as the published view and shares the state, the write
  path and the identity with the node.
* **The identity stays a UUID** and that is the remaining difference of the unit: the original
  derived it from the position (`dim:x:y:z`, oracle `TileNode:98-115`), the port keeps a stable
  random identity that is persisted with the node. The format is bound into the accepted
  evidence of the wave (`fabric/src/clientTest/java/thaumcraft/fabric/clienttest/FocalManipulatorClientSequence.java:509`
  parses the identity with `UUID.fromString`, and the accepted saved worlds carry the UUIDs in
  their node NBT), so the port cannot change it without invalidating the accepted client run.
  The unit therefore stays `partial` with exactly this difference.
* **Test** `common/src/test/java/thaumcraft/api/NodeAdapterContractTest.java` gained
  `theNodeItselfImplementsThePublishedContract`; `common/src/test/java/thaumcraft/api/NodeTypeContractTest.java`
  lost the translation test, because there is no second set of enums any more.

(The remaining units of the list are described in the sections that follow.)
