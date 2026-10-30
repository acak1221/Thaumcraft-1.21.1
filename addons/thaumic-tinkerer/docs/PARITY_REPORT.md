# Parity of the add-on with Thaumic Tinkerer 1.7.10: research tree, recipes and object tags

Date of the runs: 2026-10-30. Specification: the 1.7.10 source in `/home/user/gtnewhorizons/thaumictinkerer`, commit
`28f4def938e80d7a0b6f433c2b84300dfb7c35ce` (see `NOTICE.md`). Everything below says what ran and what it showed; nothing is
claimed that did not run. This is the second version of the report: section 8 lists what two reviewers found in the first
version of the comparison and what was done about each finding.

## 1. Result in one paragraph

All 78 research keys (81 entries, three keys have two variants that depend on a flag or a mod), all 130 recipes and the 17 object
tags of the original were extracted from the Java source and compared with what the add-on registers in a running Fabric server.
**The comparison finds no undocumented difference and no defect of the port.** It finds eleven differences, all in section 3, and
every one of them is described by a sentence of `docs/deviations/*.md` (the allow-list quotes the sentence, and a unit test checks
that the passage around it names the entry). The stricter comparison of this version (full shape of shaped recipes, aspect of the six
infused seeds, every variant of a bare `Block` ingredient, the merged quartz recipes, ore names, exact figures) also found nothing in
the port that the first version had missed; what it found were holes in the first version of the *test*, which are closed and are
proved closed by mutation tests (section 7). The "about 92 research keys" of the assignment is an overestimate: the source has 79
research constructions (78 distinct keys) and 98 recipe constructions (130 recipes once the loop over the mob aspects is unrolled);
section 5 says how that was counted twice.

## 2. What ran

| What | Command | Result |
|---|---|---|
| Extraction | `python3 -I scripts/porting/tt_oracle.py` (writes `common/src/gameTest/resources/tt_oracle/{research,recipes,provenance}.json`) | 334 source files, 194 construction sites (79 research, 98 recipe, 1 object tag), 81 research entries for 78 keys, 130 recipes (28 arcane shaped, 55 infusion, 15 crucible, 30 crafting-table shaped, 1 shapeless, 1 custom recipe class), 17 object tags, 0 item expressions unresolved, 0 stacks given to a helper that writes unmodelled NBT |
| Is the checked-in oracle current | `python3 -I scripts/porting/tt_oracle.py --check` | "the checked-in oracle is current" |
| Extractor tests | `python3 -I -m unittest discover -s scripts/porting -p "test_tt_oracle.py"` | 93 tests, OK (synthetic checkouts for the reader, the interpreter, the fail-loudly cases, the stack data, bare blocks, ore names, object tags, the config defaults and `--check`; the real-checkout class replays the extraction against the pinned commit, cross-counts the constructions with an independent regex count and compares the research keys with the keys written in the source) |
| JUnit of the comparison logic | `/root/bin/gradle-mirror-loop.sh <log> :fabric:tinkererTest --tests 'thaumic.tinkerer.parity.*'` | BUILD SUCCESSFUL: `ParityComparatorTest` 41 tests, `KnownDifferencesTest` 8 tests, 0 failures (`fabric/build/test-results/tinkererTest/`); `TinkererApiOnlyTest` (1) and `thaumic.tinkerer.kamigear.*` (30) also passed in a second run |
| GameTest on a running server | `/root/bin/gradle-mirror-loop.sh <log> :common:compileJava :fabric:runTinkererGameTest -PtinkererGameTestFilter='paritygametests.*'` | exit 0, "All 7 required tests passed"; the full report is `fabric/run-tinkerer-gametest/tinkerer-parity-report.txt`. The results file `fabric/build/tinkerer-gametest-results.xml` named in the assignment was **not** written by this focused run (it does not exist after the run); the evidence is the log line and the parity report |
| Compilation with the GameTest sources | `addons/thaumic-tinkerer/tools/compile.sh -g` | compiled, no error |
| Behaviour of the custom recipe class (not part of the oracle, see section 6) | same command, `-PtinkererGameTestFilter='basicitemsgametests.spellcloth*'` | exit 0, "All 3 required tests passed" |

What the final GameTest run compared (`tinkerer-parity-report.txt`): 74 research entries field by field (the other 4 keys are
conditional or dead in the original and are checked as such, see section 3), 127 of the 127 recipes that apply to the port
(125 matched one to one, the two quartz recipes that the port merged are matched with the recipe they were merged into), 3 more that
exist in the port only (Aspectalyzer, Golem Connector and Remote Placer arcane recipes, compared with their original definitions),
835 ingredient slots, of which 24 are wildcard slots checked against every variant, 2 are ore names, 18 slots of merged recipes were
checked against the union of the original recipes, 1 shaped pattern is the mirror image of the original's, and 17 object tags.

History of the GameTest runs: the first version of the comparison was run four times (the first run showed 108 differences that were
all defects of the comparison tool: it had hard-coded the tab name; the fourth run showed 0 undocumented and 10 documented
differences). After the review the changed comparison was run three times: the first run (6 tests) passed, the second and the third, with the
object tags added (7 tests), passed; the third is the final state of the files. Logs are in the session scratchpad (`gametest1.log`, `gametest2.log`, `gametest3.log`).

## 3. The differences (all documented, none a defect)

`subject` is the research key or the recipe name; the recipe subject carries the resolved output when the name is not unique.
"Original" lines are in `/home/user/gtnewhorizons/thaumictinkerer/src/main/java/thaumic/tinkerer/common/`, "port" lines in
`addons/thaumic-tinkerer/common/src/main/java/thaumic/tinkerer/common/` (data recipes in `.../resources/data/thaumictinkerer/recipe/`).
Every row is an entry of `common/src/gameTest/resources/tt_oracle/known_differences.json`, and each entry is consumed by the one
difference it matches (a second identical difference would be a failure).

| Subject | Field | Original | Port | Verdict and evidence |
|---|---|---|---|---|
| research `ASPECT_ANALYZER` | presence | registered only when `LoadedMods.computerCraftLoaded` (`block/BlockAspectAnalyzer.java:143` `shouldRegister`, research at `:164`) | registered | documented deviation: `docs/deviations/Machines.md` section 3, "Always registered (choice)"; port `block/BlockAspectAnalyzer.java:112`. All fields of the entry (cell 0,1, aspects, parents, pages) are still compared with the original and match |
| recipe `ASPECT_ANALYZER` | presence | arcane recipe only with ComputerCraft (`:178`) | arcane_shaped -> `thaumictinkerer:aspect_analyzer` x1 | same deviation; port `block/BlockAspectAnalyzer.java:137`. Aspects, shape and ingredients match |
| research `GOLEM_CONNECTOR` | presence | only with ComputerCraft (`block/BlockGolemConnector.java:55`, research `:76`) | registered | documented deviation: `docs/deviations/TileBlocks.md` section 2.4, "Registered without ComputerCraft (decision)"; port `block/BlockGolemConnector.java:105`. Fields match |
| recipe `GOLEM_CONNECTOR` | presence | arcane recipe only with ComputerCraft (`:94`) | arcane_shaped -> `thaumictinkerer:golem_connector` x1 | same deviation; port `block/BlockGolemConnector.java:123`. Fields match |
| research `PERIPHERALS` | presence | registered only with ComputerCraft (`research/ResearchHelper.java:177-178`) | registered | documented deviation: `docs/deviations/BasicItems.md` ("The text of the `PERIPHERALS` page (decision)") and Machines.md section 3; port `research/ResearchHelper.java:94` (`registerPeripheralsRoot`). Cell -1,0, round, auto-unlocked, no aspects: match |
| research `REMOTE_PLACER` | presence | never registered: `BlockRPlacer.shouldRegister()` returns `false` (`block/BlockRPlacer.java:91`); the research (`:102`) and recipe (`:116`) are dead code | registered | documented deviation: `docs/deviations/TileBlocks.md` section 2.2, "Registered although the fork disabled it (decision)"; port `block/BlockRPlacer.java:113` |
| recipe `REMOTE_PLACER` | presence | dead (same) | arcane_shaped -> `thaumictinkerer:remote_placer` x1 | same deviation; port `block/BlockRPlacer.java:129`. Aspects, shape and ingredients match the dead definition, including `s` = any of the 11 stone devices with an item and `d` = a dispenser (the port's `TileBlocks.anyOfLegacyBlocks("blockStoneDevice")` accepts all 11) |
| research `REMOTE_PLACER` | column | -6 (`block/BlockRPlacer.java:102-108`, the cell of `MAGNETS`) | -8 | documented deviation: `docs/deviations/TileBlocks.md`, "Research cell of the Remote Placer (lead decision)". In 1.7.10 the clash never showed because the block was not registered; Thaumcraft would drop the second entry of a cell. Port `block/BlockRPlacer.java:118`. Row, aspects, complexity, parent and pages match |
| recipe `DARK_QUARTZ0` (output `dark_quartz_item` x4) | presence (second of two) | two recipes under one name: coal and charcoal (`item/quartz/ItemDarkQuartz.java:54` and `:64`) | one data recipe whose middle ingredient accepts both (`data/.../recipe/dark_quartz_0.json`) | documented deviation: `docs/deviations/SimpleBlocks.md` section 1.2, "The two quartz recipes with coal ... and charcoal ... are one recipe with a two-item ingredient". **The second original recipe is now compared with the merged recipe too** (its charcoal has to be accepted), and the merged recipe has to accept exactly coal and charcoal in the middle and exactly quartz in the other four slots (the "union" check); deleting the charcoal, adding a third item or widening any slot fails (unit tests) |
| recipe `<unnamed>` (output `dark_quartz_stairs` x4) | presence (second of two) | the stairs recipe registered twice, left and right (`block/quartz/BlockDarkQuartzStairs.java:69` and `:77`) | one recipe (`data/.../recipe/dark_quartz_5.json`) | documented deviation: `docs/deviations/SimpleBlocks.md` section 1.2, "The stairs recipe came twice ... Shaped recipes of 1.21.1 are mirrored by default: one recipe". The second recipe is compared as the mirror image of the first and is accepted as such (the run notes it); gaps and slots of the merged recipe are checked against the union |
| research of all Kami entries | membership of the hidden parents | the 23 item-owned Kami entries never required a block-owned research of the add-on and only the item-owned research registered before each, in `HashMap` order; `WARP_GATE` (a block) required the block-owned research registered before it (`research/KamiResearchItem.java:41-70` `setPages` reads what is registered at that moment; `registry/TTRegistry.java:355-365` `postInit` registers all items before all blocks; `core/proxy/TTCommonProxy.java:191-192` runs `initResearch` before) | every Kami entry requires the union: all non-Kami research of the six tabs, among them the 19 block-owned research of the add-on (`research/KamiResearchItem.java:45-72`, `registerResearch`) | documented decision, new in this version: `docs/deviations/KamiGear.md` section 12 (written for this finding; before it the choice was documented only in the javadoc of `KamiResearchItem`). The port is stricter than any set 1.7.10 could produce, so a Kami entry is never available earlier. The test checks the port's list against exactly the union (minus lost, virtual, Kami, `MINILITH*`, `*KAMI`), explicit hidden parents first. The 19 keys are in `tinkerer-parity-report.txt`: ANIMATION_TABLET, ASPECT_ANALYZER, DISLOCATOR, ENCHANTER, FIRE_AER/AQUA/IGNIS/ORDO/PERDITIO/TERRA, FUNNEL, GOLEM_CONNECTOR, INTERFACE, LEVITATOR_LOCOMOTIVE, MAGNETS, PLATFORM, REMOTE_PLACER, REPAIRER, SUMMON |

## 4. What was found about the original that the port reproduces

These are not parity differences (the port equals the original) but they came out of the extraction and nobody has them in one place.

* **Recipes under a research key that no entry defines.** The four crucible recipes `INFUSED_POTIONSPOT0..3` (`item/ItemInfusedPotion.java`,
  key `LibResearch.KEY_POTIONS + "POT<n>"`, whose crucible recipe key is its name) and the infusion recipe `INFUSED_INKWELL`
  have no research with that key in the oracle, so in 1.7.10 Thaumcraft never unlocks them. The port carries the same keys
  (the recipes compared equal) and its Thaumic Tinkerer tab has no such entry either (the reverse check passes). The inkwell case is
  documented in `deviations/BasicItems.md`; the four potion recipes are not documented anywhere. If they are meant to be
  obtainable that is a decision for the owner, not a parity defect.
* **Names registered more than once** (`ConfigResearch.recipes` keeps the last): `SUMMON` (17 infusion recipes), `SUMMON1` (17 table
  recipes), `DARK_QUARTZ0` (2), `PAVETRAVEL` (a slab and a stairs recipe). The research pages show the last one; the test compares what the
  page shows (the output of the last), and the port matches.
* **The six infused seeds are one item.** The infusion recipes `INFUSED_POTIONS0..5` (`item/ItemInfusedSeeds.java:166-232`) all make
  `infused_seeds` x1; what tells them apart is the main aspect that `getStackFromAspect` (`:87-91`) writes into the stack's NBT (aer, ignis,
  terra, aqua, ordo, perditio; the shard of each is `itemShard:0,1,3,2,4,5`). The port keeps it in the data component
  `thaumictinkerer:infused_aspect`. The oracle records it (`output.aspect`), the research icon of `INFUSED_POTIONS` is the ignis seed
  (`item/ItemInfusedPotion.java:164`), and both are compared.
* **A bare `Block` in a recipe is every metadata value, a bare `Item` is damage 0.** `CraftingManager.addRecipe` and Thaumcraft's
  `ShapedArcaneRecipe` turn a bare `Block` into `ItemStack(block, 1, 32767)`; `new ItemStack(block)` is damage 0. Affected: the dark quartz
  slab, block and stairs recipes (`DARK_QUARTZ2..5` and the second stairs recipe: any of plain, chiseled, pillar; `DARK_QUARTZ4`: the slab),
  the Remote Placer (`s` any stone device, `d` any dispenser) and the Infused Inkwell centre (`new ItemStack(this, 1, 32767)`). All 24
  wildcard slots of the run accept every variant.
* **Kami hidden parents.** `KamiResearchItem.setPages` collects "every other research" that is registered at that moment, in whatever order
  the registry's `HashMap` visited the classes, so the 1.7.10 list is not reproducible; see the last row of section 3 for what is
  deterministic. The oracle records the explicit hidden parents, `parentsHiddenAuto` and `ownerKind` (item, block or helper); the test
  expects the explicit ones first and then the set of every non-Kami, non-lost, non-virtual research of Thaumcraft's five tabs and the
  add-on tab except `MINILITH*` and `*KAMI`. `DIMENSION_SHARDS` is auto-unlocked before `setPages`, so it has no automatic requirements.
* **`TTResearchItem.setPages` prepends `INFUSION` to the hidden parents for every infusion page** (and `setParentsHidden` after `setPages` overwrites
  them); the oracle emulates this exactly. `INFUSED_POTIONS` declares `setParentsHidden("INFUSION")` itself and has an infusion page, so its hidden
  parents are `[INFUSION, INFUSION]` in both variants; the port's research has the same list (compared).

## 5. How the oracle was built and how far it can be trusted

* `scripts/porting/tt_oracle.py` tokenizes and parses the Java the add-on uses (builder chains, casts, array creation, `if`/`for`/`switch`,
  local helper methods) and interprets it: it replays `TTRegistry.preInit` (the blocks and items, with the instances of the five
  multi-variant classes), `init` (every `getRecipeItem()`), `ResearchHelper.initResearch` and `postInit` (every `getResearchItem()`).
  An `if` on a flag or a mod forks the path and records the condition; a flag that has no effect on the entry is merged away.
* It fails (exit 2, nothing written) when a research or recipe construction is not understood or not reached. The check compares the
  evaluated constructions with a token scan of all 334 files (`provenance.json`, `constructions`: per file). During development this
  caught the dead `BlockRPlacer` code, the multi-variant armour, the constructor-registered `SpellClothRecipe`. The same holds for
  `ThaumcraftApi.registerObjectTag` calls and for a helper that is given a stack and touches its NBT without being modelled (listed in
  `provenance.json` as `unmodelled_stack_data`; empty today; only `ItemInfusedSeeds.setAspect` is modelled).
* Independent checks of the extraction: `RealCheckoutTest` counts the constructions with a regex (79 research, 98 recipe) and compares the set of
  research keys with the keys found by regex in the source; I spot-checked the extracted ENCHANTER, MAGNET, MOB_MAGNET, GASEOUS_LIGHT/SHADOW, ICHOR,
  ICHORCLOTH_ARMOR, WARP_GATE, both variants of FOCUS_ENDER_CHEST and the shape of the necromancy loop against the Java by reading both; in this
  version also the six seed recipes, the Remote Placer, the dark quartz recipes and the Kami owners.
* Item ids: `ConfigItems`/`ConfigBlocks` fields through `legacy_ids.json` (a wildcard lists the item of every metadata row that has one),
  Thaumic Tinkerer classes through `getItemName()`/`getBlockName()` of the original and the port's `Lib*Names`, vanilla names through the explicit
  `VANILLA` table of the script, variants of one item through `TT_META` and the rules for the infused grain/potion and the mob aspects (all
  written from the port's deviation texts and its `path(...)` functions), ore dictionary names through the explicit `ORE_NAMES` table
  (`ingotCopper` is `minecraft:copper_ingot`, evidence `Machines.md`). These tables are mine; nothing is guessed, an expression the tables do
  not cover is listed in `provenance.json` (`unresolved_items`, empty).
* The configuration flags the entries depend on are read from the original `ConfigHandler` (`configDefaults` in `research.json`; the evidence is in
  `provenance.json`, `config_defaults`); `Config.allowMirrors` is Thaumcraft's own flag (default true, the mod's `Config.Defaults.ALLOW_MIRRORS`).

## 6. What the comparison does not cover

* Texts and translations (page texts are compared by key only), icons other than item icons (the 14 enchantment entries use a texture path: not compared).
* The order of the research a Kami entry requires after its explicit hidden parents is compared as a set (the original's order is not reproducible).
* Ingredient identity is compared by registry id. **Potion effects** (data components on 1.21.1) are not: the two potion ingredients
  (`ICHORCLOTH_HELM_GEM` damage 8198 night vision, `ICHORCLOTH_LEGS_GEM` damage 8195 fire resistance) are compared as `minecraft:potion`, so a
  swap of the two effects would not be seen. Stack counts of ingredients are not compared. Of the NBT of stacks only the main aspect of the infused
  seeds is modelled; no other helper of the original writes NBT into a recipe stack (the extraction would list it).
* **The custom recipe class `SpellClothRecipe`** (`item/SpellClothRecipe.java:23-66`, constructed and added to the crafting manager at `item/ItemSpellCloth.java:45-46`; one
  entry of kind `crafting_custom`): the oracle has only its class name and no output, so the comparison checks that the port has a recipe of kind
  `crafting_custom:SpellClothRecipe` and nothing of its behaviour (matches exactly one cloth plus one enchanted stack that is not an
  `INoRemoveEnchant`; the result is a copy without the `ench` tag). That behaviour is tested by the BasicItems group's own GameTests
  `spellClothWipesTheEnchantmentsInTheCraftingMenu`, `spellClothRefusesWhatIsNotACleaning` and
  `spellClothLastsThirtySixCleaningsAndTheLastOneDestroysIt`, which passed in the run listed in section 2; they are not part of the parity check.
* **Scope of the reverse check (what the add-on registers that the original did not).** Research: the add-on's own tab is read completely; a
  research of the original that is missing there but exists under that key in another tab is reported as such; a research the add-on would put into
  *another* tab under a key the oracle does not have is not seen (Thaumcraft's own research cannot be told apart from it). Recipes: everything
  registered through `ThaumcraftApi` plus every recipe of every type in the `thaumictinkerer` namespace (so a smelting or stonecutting recipe would
  be an extra); a crafting recipe of the add-on in another namespace is not seen. Object tags: those of items in the add-on's namespace.
* **Other registrations with Thaumcraft are not compared:** the tab itself (`ResearchCategories.registerCategory` in `ResearchHelper.java:85`: name,
  icon, background) and every research/aspect registration that is not a research entry, a recipe or an object tag (warp values are compared with
  the research they belong to).
* A mirror image of a shaped pattern is accepted as the same recipe (1.7.10 and 1.21.1 both match mirrored); the run lists each case (one today:
  the left staircase of the dark quartz). The tag of an ore name is an open set (the port's `c:ingots/copper` may hold what other mods add; the
  comparison requires the items the name stands for, and counts the slot: 2 today, the Mob Magnet's `G`).
* Data recipes are matched to the original by output item (the port renamed them); two original recipes for the same output (coal/charcoal quartz, the
  two staircases) share one port recipe, which is a documented difference and is checked as described in section 3.
* Client-side display, the book and the research table were not looked at; this is a registration check.
* The Fabric build only. NeoForge was not run.

## 7. How strong the comparison is, and how to run it again

What a change of the port is noticed in, per dimension (each is a mutation test in `ParityComparatorTest`, most of them also against the live
registries in `aChangedOracleValueFailsAgainstTheLiveRegistries`):

* **Research:** category; aspects (amount, order, a removed or added one); column; row; complexity; parents; hidden parents (the Kami rule: a missing,
  an unexpected or a misplaced one); siblings; the nine flags; warp; icon (item and, for the infused seeds, its aspect); pages (count, kind, text key,
  and the recipes a page shows: item, aspect and count). Every applicable entry is mutated in column, first aspect amount and parents.
* **Recipes:** kind; research key; result item, count and the aspect datum of the infused seeds; aspects; instability; the box and **every slot of
  the full grid** (where the gaps are: moving the gaps of all 22 shaped recipes that have them fails while the order of the ingredients and the box
  stay; a slot that should be empty or filled fails); for each slot **what it accepts**: every item the original's ingredient stood for (all variants of
  a bare `Block`, the items of an ore name) and, unless the recipe is a documented merge, nothing more (a tag that grew, an `Item` that gained charcoal
  fail); infusion components as a multiset; the crucible catalyst; the merged quartz recipes against the union of the originals; and presence in both
  directions. Every recipe is mutated in result count and first aspect amount.
* **Object tags:** presence and aspects of each of the 17 mob essences, and no other tag for an item of the namespace.
* **The allow-list:** an entry allows exactly `occurrences` identical differences (one by default) and fails when it matches fewer; a second identical
  difference is a failure; the number of allowed differences must equal the sum of the entries (11, asserted in the GameTest) and the quote has to be
  in the cited deviation text with the entry's name in the passage around it (`KnownDifferencesTest`, with negative tests for a sentence of another
  passage and for a borrowed name).
* **The expectations are the original's:** the four flags the entries depend on must be at the defaults of the original's `ConfigHandler` in the port
  (a changed default fails the run instead of silently changing what is expected), and the figures are exact (74 research and 127 recipes compared,
  17 object tags, 11 documented differences), not lower bounds.

```bash
python3 -I scripts/porting/tt_oracle.py --check                       # is the oracle current (needs the pinned checkout)
python3 -I -m unittest discover -s scripts/porting -p "test_tt_oracle.py"
cd /home/user/Thaumcraft-1.21.1 && /root/bin/gradle-mirror-loop.sh <log> :common:compileJava :fabric:runTinkererGameTest -PtinkererGameTestFilter='paritygametests.*'
cd /home/user/Thaumcraft-1.21.1 && /root/bin/gradle-mirror-loop.sh <log> :fabric:tinkererTest --tests 'thaumic.tinkerer.parity.*'
```

Files: `scripts/porting/tt_oracle.py`, `scripts/porting/test_tt_oracle.py`; `common/src/gameTest/resources/tt_oracle/{research,recipes,provenance,known_differences}.json`;
`common/src/gameTest/java/thaumic/tinkerer/testing/ParityRuntimeChecks.java` and `.../testing/parity/{OracleData,PortSnapshot,ParityComparator,KnownDifference}.java`;
`fabric/src/gameTest/java/thaumic/tinkerer/fabric/testing/ParityGameTests.java` (listed in `fabric/src/gameTest/resources/fabric.mod.json`);
`common/src/test/java/thaumic/tinkerer/parity/{ParityComparatorTest,KnownDifferencesTest,DeviationQuotes,PortFixtures}.java`.

A new difference fails `researchMatchesTheOriginal`, `recipesMatchTheOriginal`, `objectTagsMatchTheOriginal` or `theAddOnRegistersNothingTheOriginalDidNot` with
the subject, the field and both values; the way to accept it is an entry in `known_differences.json` whose `deviation`/`quote`/`mentions` name the
sentence of a deviation text and the entry (the unit test fails otherwise), and an entry that stops matching fails `everyKnownDifferenceStillMatches`.

## 8. The review of the first version: findings and what became of them

Two reviewers read the first version; both reproduced their findings on the real comparator and oracle. All were real; none was a defect of the
port, all were holes of the test or of the report.

| # | Finding (severity) | Verified | What was done |
|---|---|---|---|
| 1 | The six `INFUSED_POTIONS0..5` seed recipes were indistinguishable in the oracle: the aspect from `getStackFromAspect` was dropped (major) | yes: 6 outputs, 1 distinct JSON | the extractor records `output.aspect` (and `icon.aspect`) for the one helper that writes it (`STACK_DATA_SETTERS`), and lists any other NBT-writing helper in `provenance.json`; the comparator reads the port's `infused_aspect` component for results, icons and the recipes a page shows; JUnit: the six differ, each swapped or dropped aspect fails; GameTest mutation `seed` |
| 2 | A bare `Block` ingredient was exact metadata 0 and the `wildcard` flag was never read (major) | yes: seven bare-`Block` keys in six recipes (`DARK_QUARTZ2..5`, the second staircase, the Remote Placer) | the extractor makes a bare `Block` a wildcard and lists every variant (`variants`: from `TT_META`, `legacy_ids.json`, `VANILLA`); the comparator requires the port slot to accept every variant and nothing else; JUnit: narrowed, widened, bare `Item` stays exact; GameTest mutation `variant` |
| 3 | The Kami hidden-parent rule was presented as an ordering problem only; membership differs and was in no list (minor) | yes, from `KamiResearchItem.java:41-70`, `TTRegistry.java:355-365`, `TTCommonProxy.java:191-192` | `ownerKind` in the oracle; one reported difference with the new allow-list entry and the new deviation text (`KamiGear.md` section 12); section 3 row; the 19 keys in the run report |
| 4 | The custom recipe `SpellClothRecipe` was compared by class name only and was not in the report (minor) | yes | section 6 says so, with the three behaviour GameTests that cover it (run: 3 passed); the oracle was not extended because its matcher is not data |
| 5 | The position of the gaps of shaped recipes was dropped on both sides (major) | yes: moving all gaps of the 22 recipes gave `passed=true` | the full grid is kept on both sides (`null` / empty list for a gap) and compared slot by slot (a mirror image is accepted and noted); JUnit: the helm and the quartz X-to-plus, and the gaps of all 22 recipes moved; GameTest mutation `gap` |
| 6 | The allow-list entries for the merged recipes hid the second variant (major) | yes: the documented-state fixture passed without charcoal | the second recipe is matched with the merged one and compared (many to one, as the mirror image where needed); the merged recipe is checked against the union of the originals; the entries now say "merged into ..." (documented-state fixture, deleted charcoal, extra item, narrowed stairs, moved gap, missing recipe: all fail) |
| 7 | An allow-list entry was not consumed by the difference it matched (minor) | yes: 11 allowed for 10 entries | `occurrences` (default 1); consumed; stale if matched less often; the GameTest asserts allowed == sum of the entries == 11; JUnit with two identical differences |
| 8 | Ore names were skipped and broad slots accepted by `contains` (minor) | yes | ore names carry `oreIds`; a slot broader than the original is a difference unless it belongs to a merge; counts of ore names, wildcard slots, broad merged slots, mirrored patterns and union slots are in the run report |
| 9 | `KnownDifferencesTest` only checked that a quote exists somewhere in the file (minor) | yes | `mentions` per entry, checked against the quote's passage (its paragraph or bullet, the two before and the heading), at least one naming the entry; negative tests for another passage, a borrowed name, no mentions, no file |
| 10 | Scope narrower than the report said (minor) | partly: object tags, the tab and other namespaces are real gaps | object tags are now in the oracle and compared (17); recipes of every type in the namespace are read; an oracle key in another tab is reported; the rest is listed in section 6 |
| 11 | Expectations followed the port's own configuration, with slack in the thresholds (minor) | yes | the flags must equal the original's defaults (read from its `ConfigHandler`); exact figures 74 / 127 / 17 / 11 |
| 12 | Section 6 and section 7 of the report overstated the check (minor) | yes | sections 6 and 7 rewritten from what the code does |

## 9. Change to a shared file

`addons/thaumic-tinkerer/fabric.gradle` (the lead's): the `thaumicTinkererGameTest` source set also reads resources from
`common/src/gameTest/resources` (the oracle is on the GameTest classpath), and the `thaumicTinkererTest` source set also compiles the plain-Java
parity classes of `common/src/gameTest/java` (excluding `thaumic/tinkerer/testing/*.java` and `thaumic/tinkerer/fabric/**`, which need a world) and reads the same resources,
so the comparison logic is JUnit-tested. No group code was changed. The only edit outside the parity files is the new section 12 of
`docs/deviations/KamiGear.md`.
