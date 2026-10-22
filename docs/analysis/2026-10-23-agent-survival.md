# Wave two — survival-progression acceptance (agent `survival`)

Date: 2026-10-23. Worktree: `/home/cyberfox-kali/wt/survival`, branch `agent/survival`.

## 1. What was asked and what was done

The port had 310 GameTest scenarios, 2200+ JUnit tests and a research catalogue of
201 entries with 294 text pages. Every one of them proves a **system**. None of them
proves that a **player in survival mode can reach the content**. The repository rule
is explicit: *"Полный каталог рецептов/аспектов/исследований должен замкнуться на
естественное получение"*.

This wave added that missing half:

* a new server-side GameTest suite `SurvivalProgressionRuntimeChecks` with six
  scenarios (`common/src/gameTest/java/thaumcraft/common/testing/`), registered in
  `scripts/porting/required-gametests.json` (316 cases now) and in both loader
  wrappers;
* the fix of the one real blocker the suite found (the Outer Lands was
  **unreachable**);
* the precise, oracle-proven list of the deliberate deviations and of the gaps that
  remain.

The oracle is `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`; every citation
below is `file:line` of that byte-exact 4.2.3.5 decompilation.

## 2. The chain the suite asserts

The suite walks the progression the way a player walks it and asserts, for every
link, that **a real recipe of the running server yields the original item in the
original count** — not that the item can be put into an inventory.

| stage | link | original |
|---|---|---|
| tools | `thaumcraft:wand_cap_iron` → `thaumcraft:wand_cap_iron` ×1 | `ConfigRecipes.java:430` |
| | `thaumcraft:wand_wood_iron` → `thaumcraft:wand` ×1 | `ConfigRecipes.java:429` |
| | `thaumcraft:table` → `thaumcraft:table` ×1 (the research table) | `ConfigRecipes.java:454` |
| | `thaumcraft:thaumometer` → `thaumcraft:thaumometer` ×1 | `ConfigRecipes.java:458` |
| | `thaumcraft:phial_empty` → `thaumcraft:phial_empty` ×8 | `ConfigRecipes.java:453` |
| | `thaumcraft:scribing_tools_phial` → `thaumcraft:inkwell` ×1 | `ConfigRecipes.java:455` |
| thaumium | `thaumcraft:thaumium_nuggets` / `thaumium_ingot_from_nuggets` / `thaumium_pickaxe` | `ConfigRecipes.java:404,419` |
| arcane devices | `arcane/arcane_stone` ×9, `arcane/arcane_pedestal` ×2, `arcane/infusion_matrix`, `arcane/vis_filter` ×2, `arcane/essentia_tube` ×8, `arcane/essentia_filter`, `arcane/alembic`, `arcane/alchemy_furnace`, `arcane/alchemical_construct`, `arcane/centrifuge`, `arcane/essentia_crystallizer`, `arcane/mnemonic_matrix`, `arcane/warded_jar`, `arcane/jar_void`, `arcane/goggles`, `arcane/deconstruction_table`, `arcane/bellows`, `arcane/arcane_lamp`, `arcane/arcane_ear`, `arcane/arcane_door`, `arcane/arcane_pressure_plate`, `arcane/arcane_spa`, `arcane/arcane_bore_base`, `arcane/hungry_chest`, `arcane/levitator`, `arcane/resonator` | `ConfigRecipes.java:191-294` |
| foci | `arcane/focus_excavation`, `focus_fire`, `focus_frost`, `focus_shock`, `focus_trade`, `focus_pouch`, `focus_primal` | `ConfigRecipes.java:247-255` |
| node devices | `arcane/node_stabilizer`, `arcane/node_transducer`, `arcane/vis_relay` ×2, `arcane/vis_charge_relay` | `ConfigRecipes.java:199-202` |
| essence | `crucible/thaumium`, `crucible/balanced_shard_air`, `crucible/nitor`, `crucible/alumentum`, `crucible/void_ingot`, `crucible/void_seed`, `crucible/pure_iron`, `crucible/tallow`, `crucible/ethereal_bloom`, `crucible/bottle_taint`, `crucible/bath_salts`, `crucible/sanity_soap`, `crucible/golem_clay`, `clusters/air`, `clusters/mixed` | `ConfigRecipes.java:110-180,442,445` |
| infusion | `infusion/eldritch_eye`, `wand_cap_thaumium`, `vis_amulet`, `jar_brain`, `essentia_reservoir`, `mirror`, `hand_mirror`, `wand_pedestal`, `wand_pedestal_focus`, `growth_lamp`, `fertility_lamp`, `primal_crusher`, `compass_stone`, `sanity_checker`, `staff_rod_primal`, `runic_amulet`, `runic_ring`, `traveling_trunk`, `hover_girdle`, `hover_harness`, `elemental_sword`, `focus_warding`, `focus_portable_hole`, `focus_hellbat`, `boots_traveller`, `advanced_node_stabilizer`, `arcane_bore`, `fortress_helm` | `ConfigRecipes.java:280-396` |

The full table (120 links) is in the source of the suite; every entry names the
original line it was taken from.

The six scenarios:

1. `survival_chain_recipes_yield_their_items` — every link above is a registered
   recipe of the running server and yields the original item and count.
2. `survival_chain_ingredients_close_on_natural_acquisition` — **closure**: every mod
   item that any mod recipe uses as an ingredient is either the result of a mod
   recipe or has a named natural world source (mine, tree, plant, merchant, mob
   drop, wand construct, world structure). The scenario also asserts that the
   cheat thaumonomicon is neither produced nor consumed by any recipe.
3. `survival_chain_research_is_purchasable_with_the_earned_pool` — every research
   entry of the chain is priced in the original aspect costs, every aspect of every
   price is discoverable in the world, every price and the **whole budget of the
   chain** fits into the pool of one aspect (the original cap of 100,
   `Config.java:393`), a primary research is refused by the direct purchase
   (the original needs a note) and a secondary research is granted with the exact
   debit, while one point short is refused atomically.
4. `survival_chain_needs_no_creative_mode_or_fixture` — the acceptance player is a
   survival player without creative abilities, the research purchase really costs
   the pool, and the Thaumonomicon is *made* by a wand on a bookshelf (the
   bookshelf is consumed and the book appears as the original special item), never
   placed into an inventory.
5. `survival_outer_lands_is_reachable_from_the_overworld` — the original oculus:
   four eldritch eyes on the eldritch altar of the eldritch ring, a dark node above
   it, the `OCULUS` research and a wand with six hundred vis open the gate, and the
   gate replaces the node.
6. `survival_boss_loot_is_the_primordial_pearl` — the Outer Lands boss drops the
   primordial pearl of the original (`EntityThaumcraftBoss.java:217`,
   `EntityCultistPortal.java:269`).

## 3. Defects found

### 3.1 The Outer Lands were unreachable — FIXED

**Original.** The gate into the Outer Lands is the *oculus*, the wand trigger of
the original event six:

* `ConfigRecipes.java:99` registers the trigger: the eldritch block, metadata zero
  (the altar), event six;
* `WandManager.java:708-740` dispatches the event and asks for the complete
  `OCULUS` research first (`WandManager.java:733`);
* `WandManager.java:427-441` (`createOculus`) demands: the clicked block is the
  altar, the altar carries **exactly four eyes** and is not open, the block above
  it is an aura node of the **DARK** type, a labyrinth already covers the altar
  (`TileEldritchAltar.checkForMaze`), the research is complete and the wand pays
  **one hundred whole vis of every primal aspect** (six hundred vis in total).
  Then the altar is marked open, the node is removed and the
  `blockEldritchPortal` is placed in its place.

**Port (before).** There was no event-six trigger anywhere. `WandTriggerRegistry`
existed but **nothing was ever registered with it**; the port implements the other
constructs as per-block handlers (`BlockCrucible.tryCreate`,
`BlockArcaneWorkbench.tryCreate`, `InfusionStructure.tryWandInteraction`,
`ThaumatoriumFormation.tryForm`, `AdvancedFurnaceFormation.tryForm`,
`BlockAlembic.tryWandInteraction`, `BlockVisRelay.tryWandInteraction`,
`BlockTube.tryWandInteraction`, `BlockTubeBuffer.tryWandInteraction`,
`BlockEssentiaReservoir.tryWandInteraction`, `TileJarNode` as `IWandable`) — the
oculus was the only one of the eight original triggers with no native path. The
consequence was fatal for the progression: **no player could ever enter the Outer
Lands**, so the eldritch research (`OCULUS`, `ENTEROUTER`, `PRIMPEARL`), the
primordial pearl and the whole eldritch half of the game were unreachable in
survival. The ledger even claimed `thaumcraft/common/items/wands/WandManager.java`
as `parity_verified` on the strength of the focus gate alone.

**Fix.** `common/src/main/java/thaumcraft/common/lib/eldritch/OculusFormation.java`
implements the original method with every original condition, and
`ItemWandCasting.useOn` dispatches it directly after the block trigger registry
(`ItemWandCasting.java:202-208`). The native path uses the block state instead of
the original metadata (a forced 1.21.1 change) and the native vis pricing
(`ArcaneVis.price`/`debit`), which is the original `consumeAllVisCrafting` with the
original consumption modifier.

**Proof.** `survival_outer_lands_is_reachable_from_the_overworld` places the four
eyes with the real interaction of the altar (`BlockEldritch.java:272-290`, the
original `BlockEldritch.java:266-272`), uses the real wand, and asserts the gate,
the opened altar and the exact six hundred vis that were paid.

### 3.2 The arcane furnace does not exist in survival — OPEN, documented

**Original.** The arcane furnace is the *infernal furnace*, the wand construct of
the original event two: `ConfigRecipes.java:90-92` registers the trigger for the
nether brick, the nether bricks and the iron bars, `WandManager.java:718` dispatches
it, `WandManager.java:332-407` (`createArcaneFurnace`, `fitArcaneFurnace`,
`replaceArcaneFurnace`) fits the three by three by three nether brick shell with a
nether brick fence in the middle, an iron bar in the middle layer and air above
the centre, and replaces it with the furnace. It costs fifty vis of fire and fifty
vis of earth.

**Port (before and after).** `thaumcraft:arcane_furnace` is registered as a block
(`ThaumcraftLegacyTiles.java:58`) with a loot table that drops obsidian and nether
bricks, but it has **no item, no recipe and no wand construct**: the event-two
trigger was never ported, exactly like the oculus.

**Consequence, measured by the new closure scenario.** Four mod items of the recipe
tree have no source at all because of it: `thaumcraft:nugget_beef`,
`thaumcraft:nugget_chicken`, `thaumcraft:nugget_fish` and `thaumcraft:nugget_pork`.
They are the smelting bonus of the arcane furnace (`ConfigRecipes.java:507-510`,
`Config.java:552-568` in the port), and they are the only ingredients of the four
`triple_meat_treat` recipes (`ConfigRecipes.java:410-413`). The triple meat treat
is therefore not craftable in the port.

**Status.** Not fixed in this wave. The native arcane furnace models the eleven
original metadata values as a `variant` and a `level` block state
(`BlockArcaneFurnace.java:52-60`), so porting the original three-by-three-by-three
replacement is a block-model question, not a trigger question, and it needs its own
careful unit. The gap is **executable**: `survival_chain_ingredients_close_on_natural_acquisition`
asserts that the set of mod ingredients without any source is **exactly** the four
documented ones. The scenario fails the moment a fifth one appears, and it fails
when one of the four is fixed, so the list can never rot silently.

### 3.3 The thaumometer is two shards cheaper than the original — FIXED

**Original.** `ConfigRecipes.java:458`: the thaumometer is
`{" 1 ", "IGI", " 1 "}` with `I` a gold ingot, `G` a glass pane and `1` an
`itemShard` of **any** metadata (`Short.MAX_VALUE` is the 1.7.10 wildcard). That is
**four shards**, one in each corner of the three by three.

**Port (before).** `common/src/main/resources/data/thaumcraft/recipe/thaumometer.json`
was `[" S ","IGI"," S "]` — **two shards**, at the middle of the top and the
bottom row, with the same `thaumcraft:shards` tag (all seven shards) as the
original wildcard. The deviation was nowhere documented.

**Fix.** The native recipe now uses the original shape
(`["S S","IGI","S S"]`, four shards of any kind). The two scenarios that craft the
thaumometer were corrected to the original grid, not weakened:
`RuntimeChecks.originalEntryRecipes` and
`NaturalOreGenerationRuntimeChecks` now collect and place **four** naturally mined
shards.

## 4. Deliberate deviations confirmed with the oracle

These are the deviations the port makes on purpose. They are proven here, not
guessed.

1. **The eight metal recipes of tin, silver and lead.** The original registers
   `PureTin`, `PureSilver`, `PureLead`, `TransTin`, `TransSilver`, `TransLead`
   (`ConfigRecipes.java:138-147`) and the two silver wand caps
   (`WandCapSilver`, `WandCapSilverInert`) **only** when
   `Config.foundTinIngot`, `Config.foundSilverIngot` and `Config.foundLeadIngot`
   are true, i.e. only when another mod supplies that metal. 1.21.1 has no tin, no
   silver and no lead at all, so the original never registers them either. The port
   documents exactly this in
   `ResearchRecipeCoverage.buildDeferred()` (`ResearchRecipeCoverage.java:600-629`)
   and in the smelting bonus corpus
   (`data/thaumcraft/tc4/smelting_bonus.json`, six skipped entries with their
   original line). Copper is vanilla, so `PureCopper`, `TransCopper` and
   `WandCapCopper` are carried.
2. **The essentia mirror.** The original result is `blockMirror` metadata six; the
   native mirror keeps the essentia mirror as a block state and has no separate
   item, because the block entity of the mirror is chosen by that state and
   `Level.setBlock` keeps the block entity of a block whose type did not change.
   Documented in `ResearchRecipeCoverage.java:602-616`.
3. **The dead second kinetic runic girdle.** `ConfigRecipes.java:355-356` registers
   the girdle twice with the potion metadata 24620, which carries the brewed flag
   `0x2000` and the splash flag `0x4000` at the same time; 1.7.10 brewing clears
   `0x2000` when it sets `0x4000` and the loot bag only ever issues one of the two
   flags, so no item of 1.7.10 can carry that metadata. The port carries the
   identical recipe twice and documents it in
   `ResearchRecipeCoverage.buildDocumentedDuplicates()`.
4. **The metadata of the original is a block state.** Every original metadata value
   became a `variant` property or its own block. This is forced by 1.21.1 and is
   applied consistently; the oculus uses it for the altar
   (`BlockEldritch.variant(state) == EldritchBlockRules.ELDRITCH_ALTAR`).
5. **The wand trigger registry is empty.** The original keys a trigger by block and
   metadata; 1.21.1 has no metadata. The port therefore answers the eight original
   triggers with per-block native handlers instead of one central registry, and the
   registry itself keeps the original public contract
   (`thaumcraft/api/wands/WandTriggerRegistry.java`).

## 5. What is still unproven

* **The full session of a real player.** The suite drives the *mechanics* of the
  chain on the real server; it does not play the game for hours. The world
  generation of the eldritch ring, the altar and the dark node above it is proved
  by the existing `SurfaceStructurePlacementRuntimeChecks`, and the natural ore
  drops that feed the thaumometer by `NaturalOreGenerationRuntimeChecks`.
* **The note path of the primary research.** The suite proves that a primary
  research is *not* bought with the aspect pool (the original rule) and that a
  secondary one is. The full "write a note on the research table, solve the hex
  grid, read the note" session is covered by the existing
  `ResearchNoteRuntimeChecks` / `ResearchTableEditingRuntimeChecks`, not by the new
  suite.
* **The arcane furnace** (3.2) and everything that depends on it.
* **The client presentation** of the oculus, the gate and the boss loot.
* **The multiplayer and the NeoForge run** of the new scenarios. The scenarios are
  registered in both loader wrappers; only the Fabric run was executed here.
* **The two red cases of the suite** (`originalbiometreesanddungeonsuserealworldgen`,
  `magicalflowersgrowharvestandrefine`) are pre-existing on the base commit and
  belong to the worldgen and the plant domains; this wave did not touch them.

## 6. How it was run and what it said

```
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/survival/.gradle-survival
cd /home/cyberfox-kali/wt/survival
./gradlew --console=plain :common:test
./gradlew --console=plain :fabric:runGameTest
```

The dedicated server is the crash surface that matters: the scenarios run inside a
real dedicated server with the real world, not in a mock harness.

**JUnit** — `:common:test`: **2208 tests, 0 failures, 0 errors, 0 skipped**.

**GameTest** — `:fabric:runGameTest`: **324 scenarios, 322 passed**. The two red
cases are `originalbiometreesanddungeonsuserealworldgen` and
`magicalflowersgrowharvestandrefine`; **both fail identically on the leader's base
commit `708fe82c` without a single line of this branch** (checked by running the
two cases on the detached base commit), so they are pre-existing and not a
regression of this wave. All six new scenarios pass.

**The server log of a real survival world.** A fresh dedicated server was started
on a new survival world (`fabric/run/survival-smoke`, `server-port=25599`,
`Default game type: SURVIVAL`), the spawn area was generated, and chunks far away
from the spawn were generated with the console command
`forceload add 4000 4000` and `forceload add -6000 8000` (the region files
`r.-12.15`, `r.-12.16`, `r.-13.15`, `r.-13.16`, `r.-6.-10`, `r.-6.-11`, `r.-7.-10`
and `r.-7.-11` of the world are the proof that the far chunks were really
written). The server was stopped with the console command `stop`; every dimension
was saved, the Outer Lands included.

`fabric/run/logs/latest.log` — 162 lines, **no exception, no error, no ticking
crash**. The only entries are:

* 55 vanilla notices `No data fixer registered for thaumcraft:<entity>` (the
  vanilla data-fixer notice for every modded entity type; present in every run of
  the port, GameTest and server alike);
* the offline-mode notice of my own `server.properties`;
* the Fabric `Untranslated Item Tags` convention warning (every run);
* three `Can't keep up!` warnings, which are the memory pressure of the five
  other Gradle daemons on this machine, not a defect of the mod.

**The NeoForge run of the six new scenarios was not executed** (the Fabric
workloads and the NeoForge build share the machine; the wave runs Fabric first
per `AGENTS.md`). The scenarios themselves are loader independent: they live in
`common/src/gameTest` and only the wrapper differs.

## 7. Files

* `common/src/main/java/thaumcraft/common/lib/eldritch/OculusFormation.java` — new,
  the original oculus.
* `common/src/main/java/thaumcraft/common/items/wands/ItemWandCasting.java` — the
  dispatch of the oculus.
* `common/src/gameTest/java/thaumcraft/common/testing/SurvivalProgressionRuntimeChecks.java`
  — new, the six scenarios.
* `common/src/main/resources/data/thaumcraft/recipe/thaumometer.json` — the
  original four shard shape.
* `common/src/gameTest/java/thaumcraft/common/testing/RuntimeChecks.java`,
  `common/src/gameTest/java/thaumcraft/common/testing/NaturalOreGenerationRuntimeChecks.java`
  — the corrected thaumometer grids.
* `fabric/src/gameTest/java/thaumcraft/fabric/testing/ThaumcraftGameTests.java`,
  `neoforge/src/gameTest/java/thaumcraft/neoforge/testing/ThaumcraftGameTests.java` —
  the six wrappers.
* `scripts/porting/required-gametests.json` — the six required cases (316 in all).
* `docs/porting/scope-ledger.json` — the evidence of the oculus for
  `thaumcraft/common/items/wands/WandManager.java` and
  `thaumcraft/common/tiles/TileEldritchAltar.java`.
