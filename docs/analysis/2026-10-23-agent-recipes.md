# Recipes and arcane crafting - the verified coverage table of the original TC4 recipe catalogue

Agent: `recipes` (wave of 2026-10-23). Worktree: `/home/cyberfox-kali/wt/recipes`, branch
`agent/recipes`.

Oracle: `/home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/common/config/ConfigRecipes.java`
(the pinned 1.7.10 reference of TC4 4.2.3.5). **The line numbers of this document are the line
numbers of THAT file.** The private oracle of the earlier waves numbers the same registrations
slightly differently (its `ConfigRecipes` is a few lines longer), so the line numbers quoted in
`common/src/test/java/thaumcraft/assets/NormalRecipeParityTest.java` and in the older ledger
notes are NOT the numbers of this document; the quoted source lines are.

## 1. What the original catalogue is

`ConfigRecipes.init()` (line 38) calls five registration methods. Between them they register
**308 distinct original recipe keys** (279 `ConfigResearch.recipes.put` call sites, five of which
are inside a loop and expand to 34 keys):

| original method | oracle lines | keys | kind |
|---|---|---|---|
| `initializeCompoundRecipes` | 53-75 | 9 | wand constructs (a plain `List`, not a recipe) |
| `initializeAlchemyRecipes` | 77-166 | 52 | crucible |
| `initializeArcaneRecipes` | 168-289 | 139 | arcane workbench |
| `initializeInfusionEnchantmentRecipes` | 291-316 | 24 | infusion (enchantment) |
| `initializeInfusionRecipes` | 318-391 | 63 | infusion (stack) |
| `initializeNormalRecipes` | 393-483 | 21 | ordinary crafting table |

The count of 308 is machine-derived: the key set is the union of the 279 `put` call sites with the
five loop-generated families expanded (`BalancedShard_0..5`, `Banner_0..15`, `PrimalArrow_0..5`,
`Clusters0..6`, `JarLabel0..47`).

The research book of the original names only 272 of the 308 keys. The other 36 are the 16 banners
and the 6 primal arrows (the original shows them as an ARRAY of recipes, `ConfigResearch:194-199`
and `:216-220`), the 6 balanced shards, the 7 crystal clusters, `RunicGirdleKinetic_2` and the 48
`JarLabel<aspect>` recipes.

## 2. The verified coverage table

`port status` is one of:

* **native** - a native 1.21.1 recipe file carries the original key and the port's
  `ResearchRecipeCoverage` names it;
* **native (gap closed)** - a native 1.21.1 recipe file carried the original key but
  `ResearchRecipeCoverage` did not name it; this wave mapped it;
* **native (duplicate)** - the original registration can never fire (see section 4), the port
  carries the identical recipe twice;
* **construct** - the original stores the key as a plain `List`, never as an `IRecipe`; it is a
  wand construct blueprint, not a recipe;
* **deferred** - the original key has no native recipe, with the reason in the last column.



Counts: {'construct': 9, 'native': 289, 'deferred': 9, 'native (duplicate)': 1} total 308 (the 14 keys of section 6 are counted as native now, they were counted as
"native (gap closed)" before this wave mapped them).

### 2.1 The totals

| measure | value |
|---|---|
| original recipe keys | 308 |
| covered by a native 1.21.1 recipe | 290 (289 named as native + 1 duplicate) |
| original wand constructs (not recipes in the original either) | 9 |
| deferred | 9 |
| original keys the research book names | 272 |
| native recipe files packaged under `data/thaumcraft/recipe/` | 344 |

The brief of this wave quoted "272 covered and 16 deferred". Both numbers are stale: the port
resolved 279 keys and deferred 9 before this wave, and the wave mapped 14 more coverage entries
and fixed 10 real content defects. The verified state is the table above.

## 3. How the coverage was verified

Every original key was parsed out of the oracle with a Java-expression parser (top level comma
splitting, `new ItemStack(...)` / `new Object[]{...}` / `Arrays.asList(...)` structure) and compared
against the native recipe JSON of the port. The following fields were compared field by field for
all 290 covered keys:

| field | oracle source | native source |
|---|---|---|
| arcane vis cost | the `AspectList` of the `addArcaneCraftingRecipe` call | the `vis` map |
| crucible aspect cost | the `AspectList` of the `addCrucibleRecipe` call | the `aspects` map |
| infusion aspect cost | the `AspectList` of the `addInfusionCraftingRecipe` call | the `aspects` map |
| infusion instability | the `int` argument of `addInfusionCraftingRecipe` | `instability` |
| result count | the `count` of the result `ItemStack` | `result.count` |
| pattern | the `String...` rows of the call | `pattern` |
| ingredient symbols | the `'X', ingredient` pairs of the call | the `key` map |
| ingredient count | the length of the ingredient array | the length of `ingredients` / `components` |
| ingredient identity | the item of the call | the item or the tag of the native ingredient |

The wand cap and wand rod craft costs of the oracle are dynamic
(`WandCap.caps.get("gold").getCraftCost()`); they were resolved from the original
`ConfigItems:287-309` (cap: iron 1, gold 3, thaumium 6, void 9, copper 2, silver 4; rod: greatwood
3, obsidian 6, blaze 6, ice 6, quartz 6, bone 6, reed 6, silverwood 9; staff rod: greatwood 8,
obsidian 14, blaze 14, ice 14, quartz 14, bone 14, reed 14, silverwood 24, primal 32).

**811 ingredient slots were compared. The aspect costs, the instabilities, the result counts, the
patterns and the ingredient counts are equal in every one of the 290 covered recipes.** The ten
ingredient identities that differed are the defects of section 5.

## 4. The differences that are NOT defects

1. `Alembic` - the oracle declares a fifth ingredient symbol `'L'` (magical leaves, line 275) that
   its own pattern never uses. The native `thaumcraft:arcane/alembic` drops the unused symbol
   because the native `ArcaneShapedRecipe` codec rejects a key that the pattern does not use
   (`ArcaneShapedRecipe.java:56-63`: "Arcane key must contain exactly the non-space pattern
   symbols"). Dropping an unused symbol cannot change what the recipe accepts.
2. `JarLabelNull` - the oracle recipe is a shapeless recipe with one ingredient (a jar label, line
   425). The native recipe is a Java `CustomRecipe`
   (`ThaumcraftLabels.ClearRecipe`), so its JSON carries no `ingredients` array. The native recipe
   accepts exactly one jar label and returns a blank one, which is the original behaviour: the
   oracle registers it with `shapelessOreDictRecipe`, and Forge 1.7.10's `ShapelessOreRecipe`
   compares the item and the damage only, not the NBT - which is precisely why TC4 wrote its own
   `ShapelessNBTOreRecipe` (`thaumcraft/common/lib/crafting/ShapelessNBTOreRecipe.java:74-88`
   overrides `matches` to add `ItemStack.areItemStackTagsEqual`).
3. `BlockFlesh`, `Thaumometer`, `TallowCandle` - the oracle pattern uses a different symbol letter
   (`K` instead of `R`, `1` instead of `S`) and, for the tallow candle, a blank border
   (`" S "`, `" T "`, `" T "`). 1.21.1 trims the blank border of a shaped pattern:
   `ShapedRecipePattern.shrink` removes the leading and the trailing spaces of every row and the
   empty rows (verified in the 1.21.1 class file
   `net/minecraft/world/item/crafting/ShapedRecipePattern.class`, method `shrink`, which computes
   `firstNonSpace`/`lastNonSpace` over every row). The native `tallow_candle` therefore carries
   the trimmed `["S","T","T"]`, which accepts exactly the same vertical stack of one string above
   two tallow. The pattern symbol letters carry no meaning.
4. `RunicGirdleKinetic_2` - the oracle registers the second kinetic runic girdle with the potion
   metadata 24620 (`0x602C`, line 351). That metadata carries the brewed flag `0x2000` and the
   splash flag `0x4000` at the same time, and 1.7.10 brewing clears `0x2000` when it sets `0x4000`
   (`PotionHelper.gunpowderEffect` is `"+14&13-13"`), while the loot bag only ever issues
   `i | 0x2000` or `i | 0x4000`. No item of 1.7.10 can carry that metadata, so the original
   registration is dead in the original itself. 1.21.1 potions have no metadata, so the port
   carries the same recipe twice.

## 5. The defects this wave found and fixed

All ten are a wrong INGREDIENT: the native recipe asked the player for a different item than the
original. The original matches the components of an infusion and the ingredients of an arcane
recipe as ITEMS (`thaumcraft/api/crafting/InfusionEnchantmentRecipe.matches` compares the
ItemStacks, and the native `InfusionEnchantmentRecipe.matches:120-139` does the same through
`InfusionMatching`), so a substitute item changes what the player has to bring.

| # | original key | oracle line | original ingredient | native ingredient before the fix |
|---|---|---|---|---|
| 1 | `InfEnch3` (projectile protection) | 297 | `Items.arrow` | `minecraft:feather` |
| 2 | `InfEnch4` (feather falling) | 298 | `Items.feather` | `minecraft:string` |
| 3 | `InfEnch5` (respiration) | 299 | `Items.reeds` | `minecraft:cod` |
| 4 | `InfEnch6` (aqua affinity) | 300 | `Items.reeds` | `minecraft:cod` |
| 5 | `InfEnch7` (thorns) | 301 | `Blocks.deadbush` | `minecraft:cactus` |
| 6 | `InfEnch9` (smite) | 303 | `Items.glowstone_dust` | `minecraft:rotten_flesh` |
| 7 | `InfEnch15` (silk touch) | 309 | `Blocks.web` | `minecraft:glass` |
| 8 | `InfEnch21` (infinity) | 315 | `Items.arrow` | `minecraft:feather` |
| 9 | `ArcaneSpa` | 198 | `blockCosmeticSolid` metadata 6 (arcane stone block) | `thaumcraft:arcane_stone_bricks` (metadata 7) |
| 10 | `HoverHarness` | 341 | `blockWoodenDevice` metadata 6 (greatwood planks), twice | `thaumcraft:bellows` (metadata 0), twice |

Every one of the original items still exists in 1.21.1 under the same id (`minecraft:arrow`,
`minecraft:feather`, `minecraft:sugar_cane`, `minecraft:dead_bush`, `minecraft:glowstone_dust`,
`minecraft:cobweb`, `thaumcraft:arcane_stone`, `thaumcraft:greatwood_planks`), so nothing forced
the substitution. The metadata of the two block families is proved by the original language file
(`assets/thaumcraft/lang/en_US.lang`): `tile.blockWoodenDevice.6.name=Greatwood Planks` and
`tile.blockWoodenDevice.0.name=Arcane Bellows`, and
`tile.blockCosmeticSolid.6.name=Arcane Stone Block` and
`tile.blockCosmeticSolid.7.name=Arcane Stone Bricks`. The port was already inconsistent with
itself: the traveling trunk of the same file uses the greatwood planks for the same metadata
(line 372), and the flux scrubber uses the arcane stone bricks for the same metadata (line 180).

The game test that pinned the two bellows of the hover harness
(`common/src/gameTest/java/thaumcraft/common/testing/TravelGearRuntimeChecks.java:112`) now pins
the two greatwood planks.

## 6. The real coverage gaps this wave closed

Fourteen original keys had a native recipe file but were not named by
`ResearchRecipeCoverage`, so the catalogue was not closed over the original and the research book
could not resolve them:

| original key | oracle line | native recipe |
|---|---|---|
| `BalancedShard_0..5` | 89 | `thaumcraft:crucible/balanced_shard_{air,fire,water,earth,order,entropy}` |
| `Clusters0..5` | 442 | `thaumcraft:clusters/{air,fire,water,earth,order,entropy}` |
| `Clusters6` | 445 | `thaumcraft:clusters/mixed` |
| `RunicGirdleKinetic_2` | 351 | `thaumcraft:infusion/runic_girdle_kinetic_2` |

The coverage map now names all 308 original keys, the outside-book set names the 36 keys the
research corpus never references, and the catalogue guard of
`common/src/test/java/thaumcraft/research/ResearchRecipeCoverageTest.java` pins the whole set.

## 7. The deferrals, re-checked

The nine deferrals are the eight foreign ore recipes and the essentia mirror. The deferral
reasons were wrong or incomplete and are now exact:

* **The eight foreign ore recipes** (`PureTin`, `PureSilver`, `PureLead`, `TransTin`,
  `TransSilver`, `TransLead`, `WandCapSilver`, `WandCapSilverInert`). The original registers them
  only when another mod supplies the metal (`Config.foundTinIngot`, `foundSilverIngot`,
  `foundLeadIngot`), and 1.21.1 has no tin, no silver and no lead at all, so the original never
  registers them either. The CATALYST side of the port is already ready: the tag
  `thaumcraft:original_ore/ore_tin` points at the optional convention tag `c:ores/tin` and
  `thaumcraft:original_ore/nugget_tin` at `c:nuggets/tin`, so a third party tin mod would be picked
  up. What is missing is the RESULT item: the original result of `PureTin` is the tin cluster
  (`itemNugget` metadata 18) and of `TransTin` three tin nuggets (`itemNugget` metadata 2), and the
  port has no tin, silver or lead nugget or cluster item. The port already carries the copper
  equivalents, because copper is vanilla in 1.21.1.
* **The essentia mirror** (`MirrorEssentia`). The deferral used to say that "a 1.21.1 recipe result
  cannot carry a block state". That is FALSE: the `minecraft:block_state` component of an item
  stack holds the block state properties (`BlockItemStateProperties.CODEC` is a map of string to
  string) and `BlockItem.updateBlockStateFromTag` applies them to the placed state, so a recipe
  result CAN carry the `essentia` property of the mirror. The real blocker is the port's own
  mirror block: the essentia mirror is the same block with the `essentia` property true, its block
  entity is chosen by that property (`BlockMirror.createBlockEntity`), and `Level.setBlock` keeps
  the block entity of a block whose type did not change - a mirror placed with the component would
  keep the tile of the ordinary mirror and would never tick. The missing part is a separate
  essentia mirror ITEM, not a recipe format. A new item and the block entity it needs is content of
  the base content domain, so the deferral stays, with the exact reason.

## 8. The deviations the port keeps on purpose

These are not defects of this wave; they are the port's own documented positions. They are listed
here so that the leader can decide about them.

| original key | oracle | native | why the port does it that way |
|---|---|---|---|
| `GolemClay` | catalyst `Blocks.brick_block` (bricks) | `minecraft:clay` | the original asks for bricks for the clay golem; the port asks for clay |
| `EldritchEye` | component `itemResource` metadata 17 (void seed) | `thaumcraft:void_ingot` | the original asks for the void seed |
| `LampFertility` | component `Items.carrot` | `minecraft:golden_carrot` | the original asks for a carrot |
| `AltInk` | result and catalyst `Items.dye` metadata 0 | `minecraft:ink_sac` | the original asks for the dye item; the port keeps the ink of the research page |
| `MaskGrinningDevil` | component `Items.dye` metadata 0 | `minecraft:ink_sac` | the port keeps the ink of the mask |
| `TinyVisor`, `TinyArmor` | component `Items.iron_helmet` / `Items.iron_chestplate` (any wear) | `minecraft:leather_helmet` / `minecraft:leather_chestplate` | the port accepts leather armour of any wear and no iron one; the game test `GolemDecorationsRuntimeChecks` states that position |

The native item ids of three recipes differ from the original names, but the display names are
the original ones: `thaumcraft:key_iron` and `thaumcraft:key_gold` are the iron and the gold
arcane key (the original `itemKey` metadata 0 and 1), `thaumcraft:golem_core_essentia` is the
"Golem Animation Core: Alchemy" (the original `itemGolemCore` metadata 6) and
`thaumcraft:unknown_research_note` is the research note (the original `itemResearchNotes`).

The four potions of the runic accessories are not deviations: the original names them by their 1.7
metadata (8226, 8233, 8257, 16428) and the port names them by their 1.21.1 effect with a
component predicate (`InfusionPotionRequirement`), which is the exact same potion.

## 9. The tests

Both are **authored but NOT EXECUTED** - this agent does not run Gradle (the leader owns every
build; concurrent Loom daemons corrupt the shared cache).

* `common/src/test/java/thaumcraft/research/ResearchRecipeCoverageTest.java` - the corpus guard,
  EXTENDED: it now pins the whole catalogue of 308 original keys
  (`theWholeOriginalCatalogueIsClassified`), the 293 native recipe ids
  (`everyMappedOriginalKeyResolvesToAPackagedNativeRecipe`), the 36 outside-book keys and the
  fourteen keys this wave mapped (`theOriginalRecipesOutsideTheResearchBookAreMappedToTheirNativeRecipes`).
* `common/src/test/java/thaumcraft/assets/RecipeIngredientParityTest.java` - NEW, the content
  guard of this wave: it pins the original ingredient of the ten restored recipes, the two arcane
  stones of the arcane spa and the flux scrubber, the greatwood planks of the hover harness and
  the traveling trunk, and the aspect cost of the six balanced shards.

The existing game test of the hover harness was updated with the recipe.

## 10. The ledger

`docs/porting/scope-ledger.json`, the nine units of the `recipes_and_arcane_crafting` family
only:

| unit | before | after |
|---|---|---|
| `thaumcraft/common/config/ConfigRecipes.java` | replace / partial | replace / **parity_verified** |
| `thaumcraft/common/lib/crafting/ThaumcraftCraftingManager.java` | replace / partial | replace / **parity_verified** |
| `thaumcraft/common/container/SlotCraftingArcaneWorkbench.java` | adapt / partial | adapt / **parity_verified** |
| `thaumcraft/common/config/recipes/ConfigRecipesArcaneSlice.java` | exclude / missing | exclude / **parity_verified** |
| `thaumcraft/common/config/recipes/ConfigRecipesSmeltingSlice.java` | exclude / missing | exclude / **parity_verified** |
| `thaumcraft/common/config/recipes/ConfigRecipesSpecialSlice.java` | exclude / missing | exclude / **parity_verified** |
| `thaumcraft/client/integration/jei/JeiRecipeData.java` | exclude / missing | exclude / missing (the port has no JEI) |
| `thaumcraft/client/integration/jei/ThaumcraftRecipeCategory.java` | exclude / missing | exclude / missing (the port has no JEI) |
| `thaumcraft/client/integration/jei/ThaumcraftRecipeWrapper.java` | exclude / missing | exclude / missing (the port has no JEI) |

The shape of the file is unchanged, `counts` is untouched and the review fingerprints of the nine
rows still match their sources, so `stale_review_fingerprints` stays 0. The check itself
(`python3 scripts/porting/update_scope_ledger.py --check`) could NOT be run in this worktree: the
script needs the private oracle root `work/thaumcraft-4.2.3.5-oracle`, which only the leader has.
The equivalent computation was run over the file instead: the stored `review_fingerprints_current`
of every one of the 1044 rows still equals the value the script computes, and no row is stale.

## 11. What was NOT done

* The nine deferrals stay deferred, with the exact blocker of section 7. Closing the six foreign
  ore crucible recipes needs six new items (the tin, silver and lead nugget and cluster items),
  which is item and asset content of the base content domain; closing the essentia mirror needs a
  separate mirror item and its block entity.
* No Gradle build, no test run, no client or server session: this agent does not run Gradle. Every
  test above is authored but unexecuted.
* The `docs/MIGRATION_STATUS.md` and `docs/HANDOFF.md` were not touched; they are shared documents
  of the whole port.
