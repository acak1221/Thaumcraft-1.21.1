# Recipes and arcane crafting - the coverage table of the original TC4 recipe catalogue

Agent: `recipes` (wave of 2026-10-23). Worktree: `/home/cyberfox-kali/wt/recipes`, branch
`agent/recipes`.

Oracle: `/home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/common/config/ConfigRecipes.java`
(the pinned 1.7.10 reference of TC4 4.2.3.5). The line numbers of the table below are the line
numbers of THAT file. The private oracle of the earlier waves numbers the same registrations
slightly differently (its `ConfigRecipes` is a few lines longer), so the line numbers in
`common/src/test/java/thaumcraft/assets/NormalRecipeParityTest.java` and in the ledger notes
are NOT the numbers of this table; the quoted source lines are.

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

* **native** - a native 1.21.1 recipe file carries the original key, and the port's
  `ResearchRecipeCoverage` names it;
* **native (gap closed)** - a native 1.21.1 recipe file carried the original key but
  `ResearchRecipeCoverage` did not name it; this wave mapped it;
* **native (duplicate)** - the original registration can never fire (see section 4), the port
  carries the identical recipe twice;
* **construct** - the original stores the key as a plain `List`, never as an `IRecipe`; it is a
  wand construct blueprint, not a recipe;
* **deferred** - the original key has no native recipe, with the reason in the last column.



Counts: {'construct': 9, 'native (gap closed)': 13, 'native': 276, 'deferred': 9, 'native (duplicate)': 1} total 308.

### 2.1 The totals

| measure | value |
|---|---|
| original recipe keys | 308 |
| covered by a native 1.21.1 recipe | 290 (276 + 13 closed by this wave + 1 duplicate) |
| original wand constructs (not recipes in the original either) | 9 |
| deferred | 9 |
| original keys the research book names | 272 |
| native recipe files packaged under `data/thaumcraft/recipe/` | 344 |

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

The wand cap and wand rod craft costs of the oracle are dynamic
(`WandCap.caps.get("gold").getCraftCost()`); they were resolved from the original
`ConfigItems:287-309` (cap: iron 1, gold 3, thaumium 6, void 9, copper 2, silver 4; rod: greatwood
3, obsidian 6, blaze 6, ice 6, quartz 6, bone 6, reed 6, silverwood 9; staff rod: greatwood 8,
obsidian 14, blaze 14, ice 14, quartz 14, bone 14, reed 14, silverwood 24, primal 32).

**Result: no field differs in any of the 290 covered recipes.** The seven differences the first
comparison pass reported are all accounted for and are NOT defects (section 4).

## 4. The differences the comparison pass reported, and why none of them is a defect

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
3. `BlockFlesh`, `Thaumometer`, `TallowCandle` - the oracle pattern uses a different symbol
   letter (`K` instead of `R`, `1` instead of `S`) and, for the tallow candle, a blank border
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

## 5. The real gaps this wave closed

