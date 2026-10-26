# Рецепты

Аддон может регистрировать рецепты Thaumcraft двумя способами и смешивать их:

- **через API** — вызовами `ThaumcraftApi` из 1.7.10 и объектами рецептов, которые они возвращают;
- **как данные** — JSON-файлами родных типов рецептов в `data/<modid>/recipe/`, как любой рецепт 1.21.1.

Оба способа крафтятся, закрыты исследованием, видны в EMI и JEI и могут стоять на странице исследования. Стоимость в
исходных единицах: магические рецепты — **целые вис** шести первичных аспектов, тигель и наполнение — **эссенция**.

## Через API

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

| Вызов | Примечание |
|---|---|
| `addArcaneCraftingRecipe(research, result, vis, Object... recipe)` | с формой: строки шаблона, затем пары символ/ингредиент, как фигурный рецепт 1.7.10 |
| `addShapelessArcaneCraftingRecipe(research, result, vis, Object... ingredients)` | до девяти ингредиентов |
| `addCrucibleRecipe(research, result, catalyst, aspects)` | катализатор бросают в тигель, где есть аспекты |
| `addInfusionCraftingRecipe(research, result, instability, aspects, central, components)` | нестабильность 0–24 |
| `addInfusionEnchantmentRecipe(research, enchantment, instability, aspects, components)` | принимает `Holder<Enchantment>`; чары в 1.21.1 — реестр мира, поэтому лучше форма JSON ниже |

Ингредиентом может быть `ItemStack`, `Item`, `Block`, `Ingredient`, id тега с пространством имён (`"c:ingots/iron"`) или
имя рудного словаря 1.7.10 (`"ingotIron"`, `"logWood"`, `"dyeBlack"`). Рецепты API переживают перезагрузку пакетов
данных; просмотрщики рецептов показывают их под стабильными id вида `thaumcraft:api/...`.

## Как данные

Родные типы рецептов — те же, что у рецептов самого Thaumcraft
([`data/thaumcraft/recipe/`](../../../../common/src/main/resources/data/thaumcraft/recipe) — около 240 штук).

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

`pattern` — от одной до трёх строк одной ширины; каждый символ `key` используется; `vis` называет первичные аспекты по
тегу (`aer`, `terra`, `ignis`, `aqua`, `ordo`, `perditio`) в целых вис; `mirrored` (по умолчанию `true`) принимает и
зеркальное отражение; `research` можно не указывать — тогда крафтить может любой.

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

`research` обязателен. `aspects` называет любые аспекты по тегу, в эссенции. `source_order` решает, какой рецепт
выигрывает, если катализатор подходит к нескольким: тигель перебирает рецепты JSON по `source_order`, затем по id, а
рецепты API — после всех них. Рецепты самого Thaumcraft занимают 0–180, так что аддону стоит брать числа от 1000.

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

`input` — предмет на пьедестале матрицы, `components` — предметы на внешних пьедесталах; `instability` 0–24;
`research` можно не указывать. Необязательные поля, которыми пользуются рецепты Thaumcraft: `input_damage`,
`component_predicates` и `result_patch`.

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

Здесь `research` обязателен.

## На странице исследования

Объект рецепта API ставится на страницу как `new ResearchPage(recipe)`, рецепт из данных — по id:
`ResearchPage.recipe(id("spark_focus"))` (см. [исследования](research.md#страницы)). Книга тогда открывается на этой
странице по результату рецепта, как в 1.7.10.

## Дополнительно к крафту

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

- `addWarpToItem(stack, amount)`: стойкий (sticky) варп, когда игрок создаёт предмет на верстаке, магическом верстаке или
  алтаре наполнения.
- `addSmeltingBonus(input, bonus)`: адская печь иногда выдаёт бонус вместе с переплавленным предметом. Форма со
  `String` принимает исходное имя руды из рудного словаря (`"oreCopper"`).
- `addLootBagItem(stack, weight, bagTypes...)`: запись мешков с добычей; 0 — обычный, 1 — необычный, 2 — редкий.
