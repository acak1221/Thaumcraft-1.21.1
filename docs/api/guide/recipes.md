# Recipes

An add-on can register Thaumcraft recipes in two ways, and may mix them:

- **through the API**, with the 1.7.10 calls of `ThaumcraftApi` and the recipe objects they return;
- **as data**, with JSON files of the native recipe types in `data/<modid>/recipe/`, like any 1.21.1 recipe.

Both are crafted, gated by research, shown in EMI and JEI, and can be put on a research page. Costs keep the original
units: arcane costs are **whole vis** of the six primals, crucible and infusion costs are **essentia**.

## Through the API

<!-- snippet:api-recipes -->
```java
// The same calls and the same units as 1.7.10: arcane costs are whole vis of the six primals, crucible and
// infusion costs are essentia. A Thaumcraft item is looked up by its 1.21.1 id.
ShapedArcaneRecipe arcane = ThaumcraftApi.addArcaneCraftingRecipe("EXAMPLE_SHARDS", new ItemStack(VIS_SHARD, 4),
        new AspectList().add(Aspect.ORDER, 5).add(Aspect.AIR, 5),
        " A ", "ASA", " A ", 'A', new ItemStack(Items.AMETHYST_SHARD), 'S', new ItemStack(ItemsTC.require("balance_shard")));
ShapelessArcaneRecipe shapeless = ThaumcraftApi.addShapelessArcaneCraftingRecipe("EXAMPLE_SCROLL", new ItemStack(INSIGHT_SCROLL, 2),
        new AspectList().add(Aspect.AIR, 2).add(Aspect.ORDER, 2),
        new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(VIS_SHARD), "dyeBlack");
CrucibleRecipe crucible = ThaumcraftApi.addCrucibleRecipe("EXAMPLE_SHARDS", new ItemStack(Items.AMETHYST_SHARD),
        new ItemStack(Items.QUARTZ), new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.MAGIC, 2));
InfusionRecipe infusion = ThaumcraftApi.addInfusionCraftingRecipe("EXAMPLE_INFUSION", new ItemStack(Items.ECHO_SHARD), 2,
        new AspectList().add(Aspect.SENSES, 8).add(Aspect.DARKNESS, 8), new ItemStack(Items.AMETHYST_SHARD),
        new ItemStack[]{new ItemStack(VIS_SHARD), new ItemStack(Items.SCULK), new ItemStack(VIS_SHARD), new ItemStack(Items.SCULK)});
```
<!-- /snippet -->

| Call | Notes |
|---|---|
| `addArcaneCraftingRecipe(research, result, vis, Object... recipe)` | shaped: pattern rows, then symbol/ingredient pairs, as a 1.7.10 shaped recipe |
| `addShapelessArcaneCraftingRecipe(research, result, vis, Object... ingredients)` | up to nine ingredients |
| `addCrucibleRecipe(research, result, catalyst, aspects)` | the catalyst is thrown into the crucible that holds the aspects |
| `addInfusionCraftingRecipe(research, result, instability, aspects, central, components)` | instability 0–24 |
| `addInfusionEnchantmentRecipe(research, enchantment, instability, aspects, components)` | takes a `Holder<Enchantment>`; enchantments are a world registry in 1.21.1, so prefer the JSON form below |

An ingredient may be an `ItemStack`, an `Item`, a `Block`, an `Ingredient`, a namespaced tag id (`"c:ingots/iron"`) or
a 1.7.10 ore dictionary name (`"ingotIron"`, `"logWood"`, `"dyeBlack"`). The recipes of the API survive data pack
reloads; recipe viewers list them under stable ids of the form `thaumcraft:api/...`.

## As data

The native recipe types are the ones Thaumcraft's own recipes use
([`data/thaumcraft/recipe/`](../../../common/src/main/resources/data/thaumcraft/recipe) holds about 240 of them).

### `thaumcraft:arcane_shaped`

<!-- file:examples/example-addon/src/main/resources/data/tc4_example/recipe/spark_focus.json -->
```json
{
  "type": "thaumcraft:arcane_shaped",
  "research": "EXAMPLE_FOCUS",
  "pattern": [" F ", "FQF", " F "],
  "key": {
    "F": {"item": "thaumcraft:fire_shard"},
    "Q": {"item": "minecraft:quartz"}
  },
  "result": {"id": "tc4_example:spark_focus", "count": 1},
  "vis": {"ignis": 10, "aer": 5, "perditio": 5}
}
```
<!-- /file -->

`pattern` is one to three rows of equal width; every symbol of `key` is used; `vis` names primal aspects by tag
(`aer`, `terra`, `ignis`, `aqua`, `ordo`, `perditio`) in whole vis; `mirrored` (default `true`) also accepts the mirror
image; `research` may be left out, and then everyone can craft it.

### `thaumcraft:arcane_shapeless`

```json
{
  "type": "thaumcraft:arcane_shapeless",
  "research": "TUBEFILTER",
  "ingredients": [{"item": "thaumcraft:essentia_tube"}, {"item": "thaumcraft:vis_filter"}],
  "result": {"id": "thaumcraft:essentia_filter", "count": 1},
  "vis": {"aqua": 5, "ordo": 16}
}
```

### `thaumcraft:crucible`

<!-- file:examples/example-addon/src/main/resources/data/tc4_example/recipe/insight_scroll_from_crucible.json -->
```json
{
  "type": "thaumcraft:crucible",
  "research": "EXAMPLE_SCROLL",
  "source_order": 1000,
  "catalyst": {"item": "minecraft:paper"},
  "result": {"id": "tc4_example:insight_scroll", "count": 1},
  "aspects": {"cognitio": 4, "praecantatio": 2}
}
```
<!-- /file -->

`research` is required. `aspects` names any aspect by tag, in essentia. `source_order` decides which recipe wins when
one catalyst fits several: the crucible tries the JSON recipes by `source_order`, then by id, and the recipes of the API
after all of them. Thaumcraft's own recipes use 0–180, so an add-on uses numbers from 1000 up.

### `thaumcraft:infusion_stack`

<!-- file:examples/example-addon/src/main/resources/data/tc4_example/recipe/order_amulet.json -->
```json
{
  "type": "thaumcraft:infusion_stack",
  "research": "EXAMPLE_AMULET",
  "input": {"item": "thaumcraft:mundane_amulet"},
  "components": [
    {"item": "thaumcraft:order_shard"}, {"item": "tc4_example:vis_shard"},
    {"item": "thaumcraft:order_shard"}, {"item": "tc4_example:vis_shard"}
  ],
  "aspects": {"ordo": 16, "praecantatio": 8, "tutamen": 8},
  "instability": 2,
  "result": {"id": "tc4_example:order_amulet", "count": 1}
}
```
<!-- /file -->

`input` is the item on the pedestal of the matrix, `components` the items on the outer pedestals; `instability` is
0–24; `research` may be left out. Optional advanced fields used by Thaumcraft's own recipes: `input_damage`,
`component_predicates` and `result_patch`.

### `thaumcraft:infusion_enchantment`

```json
{
  "type": "thaumcraft:infusion_enchantment",
  "research": "INFUSIONENCHANTMENT",
  "enchantment": "minecraft:aqua_affinity",
  "components": [{"item": "minecraft:sugar_cane"}, {"item": "minecraft:slime_ball"}, {"item": "thaumcraft:salis_mundus"}],
  "aspects": {"praecantatio": 4, "motus": 8, "aqua": 8},
  "instability": 2,
  "first_level_minimum_cost": 1
}
```

`research` is required here.

## On a research page

A recipe object of the API goes on a page as `new ResearchPage(recipe)`. A data-driven recipe goes on a page by its id:
`ResearchPage.recipe(id("spark_focus"))` (see [research](research.md#pages)). The book then opens on that page from
the recipe's output, as in 1.7.10.

## Crafting extras

<!-- snippet:misc-registrations -->
```java
// Crafting the amulet (infusion fires the crafted-item event) gives one sticky warp, as in 1.7.10.
ThaumcraftApi.addWarpToItem(new ItemStack(ORDER_AMULET), 1);
// The infernal furnace sometimes pays a bonus: 1.21.1 raw copper gets the copper nugget its ore gave in 1.7.10.
ThaumcraftApi.addSmeltingBonus(new ItemStack(Items.RAW_COPPER), new ItemStack(ItemsTC.require("copper_nugget")));
// Loot bags: weight 15 in the common (0) and uncommon (1) bags.
ThaumcraftApi.addLootBagItem(new ItemStack(VIS_SHARD, 2), 15, 0, 1);
```
<!-- /snippet -->

- `addWarpToItem(stack, amount)`: sticky warp when the player crafts the item at a crafting table, an arcane workbench
  or an infusion altar.
- `addSmeltingBonus(input, bonus)`: the infernal furnace sometimes ejects the bonus with the smelted item. The
  `String` form takes an original ore dictionary name (`"oreCopper"`).
- `addLootBagItem(stack, weight, bagTypes...)`: an entry of the loot bags; 0 common, 1 uncommon, 2 rare.
