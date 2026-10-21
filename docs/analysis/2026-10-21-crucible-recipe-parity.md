# Тигель (crucible): сверка рецептов с оракулом TC4 4.2.3.5

Дата: 2026-10-21. Ветка: `port/fixrecipes2`. Источник истины:
`/home/Acak/work/thaumcraft-4.2.3.5-oracle` (декомпиляция
`Thaumcraft-1.7.10-4.2.3.5.jar`), `thaumcraft/common/config/ConfigRecipes.java`,
метод `initializeAlchemyRecipes`, строки 105-181.

Задача: разобрать дефекты рецептов, которые нашёл аудит
`docs/analysis/2026-10-21-audit-a6-content.md` (пп. 2-4 и раздел «Рецепты тигля —
сводка»), и проверить полноту набора рецептов тигля.

**Итог: все три «дефекта» аудита — ложные. Дерево рецептов тигля порта соответствует
оригиналу; менять данные не потребовалось.** Ниже — доказательства по каждому пункту.

## 1. `CoreGuard` / `CoreButcher`: результат и катализатор НЕ переставлены

Сигнатура оригинала (`ThaumcraftApi.java:139`):

```java
public static CrucibleRecipe addCrucibleRecipe(String key, ItemStack result, Object catalyst, AspectList tags)
```

**Результат — второй аргумент, катализатор — третий.**

* `ConfigRecipes.java:176` — `CoreGuard`: результат
  `new ItemStack(ConfigItems.itemGolemCore, 1, 4)`, катализатор
  `new ItemStack(ConfigItems.itemGolemCore, 1, 100)`;
* `ConfigRecipes.java:177` — `CoreButcher`: результат
  `new ItemStack(ConfigItems.itemGolemCore, 1, 9)`, катализатор
  `new ItemStack(ConfigItems.itemGolemCore, 1, 4)`.

Метаданные `ItemGolemCore` (`entities/golems/ItemGolemCore.java:44-57`):
`icon[4]` = `golem_core_guard`, `icon[9]` = `golem_core_butcher`, `d == 100` =
`golem_core_blank`.

Итого оригинал: **пустой сердечник → сердечник защиты**, **сердечник защиты →
сердечник мясника**. Нативные `crucible/golem_core_guard.json` и
`crucible/golem_core_butcher.json` делают ровно это. Аудит прочитал второй и третий
аргументы `addCrucibleRecipe` в обратном порядке и объявил порт переставленным.
Независимое подтверждение: легаси 1.12
(`src/main/java/thaumcraft/common/config/recipes/ConfigRecipesCrucibleSlice.java:320-331`)
тоже даёт CoreGuard = (md 4) из `coreBlank` (md 100) и CoreButcher = (md 9) из (md 4).

## 2. `bucket_of_death`: катализатор — ведро, а не стеклянная бутылка

`ConfigRecipes.java:160` — катализатор `new ItemStack(Items.field_151133_ar)`.
Аудит объявил это стеклянной бутылкой, опираясь на легаси 1.12
(`ConfigRecipesCrucibleSlice.java:216-218`, где поле подставлено как
`Items.GLASS_BOTTLE`). Легаси 1.12 here ошибается; **таблица аспектов самого
оракула** решает вопрос однозначно:

| оракул | строка | аспекты | что это |
|---|---|---|---|
| `Items.field_151133_ar` | `ConfigAspects.java:314` | METAL 8 + VOID 1 | **ведро** |
| `Items.field_151131_as` | `ConfigAspects.java:315` | ведро + WATER 4 | ведро с водой |
| `Items.field_151129_at` | `ConfigAspects.java:316` | ведро + FIRE 4 + EARTH 1 | ведро с лавой |
| `Items.field_151117_aB` | `ConfigAspects.java:317` | ведро + HUNGER 2 + HEAL 2 + WATER 2 | ведро с молоком |
| `Items.field_151069_bo` | `ConfigAspects.java:302` | VOID 1 (complex) | стеклянная бутылка |

Таблица аспектов оригинала (легаси 1.12 `ConfigAspects.java:199` и `:208`) даёт
`Items.GLASS_BOTTLE` = CRYSTAL 1 + VOID 1, а `Items.BUCKET` = **METAL 8 + VOID 1** —
точная сигнатура `field_151133_ar`. Следовательно `field_151133_ar` = `Items.bucket`,
и нативный `minecraft:bucket` верен.

Это поле — ещё и пустой сосуд ведра смерти (`ConfigItems.java:489-490`) и то, что
остаётся после выливания (`items/ItemBucketDeath.java:87`), то есть нативный
`ItemBucketDeath`, возвращающий `Items.BUCKET`, тоже верен. И `'B'` в арканном
рецепте alembic (`ConfigRecipes.java:281`) — то же ведро, поэтому
`recipe/arcane/alembic.json` с `minecraft:bucket` верен.

**Смена катализатора на `minecraft:glass_bottle` была бы регрессией.**

## 3. `alumentum`: уголь И древесный уголь — это оригинальное поведение

`ConfigRecipes.java:115` — катализатор
`new ItemStack(Items.field_151044_h, 1, Short.MAX_VALUE)`. `Short.MAX_VALUE` —
`OreDictionary.WILDCARD_VALUE`.

`Items.field_151044_h` — предмет угля 1.7.10, у которого **два** значения метаданных:
0 = уголь, 1 = древесный уголь (в 1.7.10 отдельного `minecraft:charcoal` ещё нет,
расщепление на два предмета — 1.8). Проверка катализатора —
`ThaumcraftApiHelper.itemMatches` (`ThaumcraftApiHelper.java:148-153`):

```java
return target.getItem() == input.getItem()
    && (target.getItemDamage() == Short.MAX_VALUE && !strict || target.getItemDamage() == input.getItemDamage());
```

Для wildcard-стека сравнение метаданных **пропускается**, поэтому катализатор
принимает и уголь, и древесный уголь.

То же видно и по плавке: `ConfigRecipes.java:487` —
`addSmelting(blockMagicalLog, new ItemStack(Items.field_151044_h, 1, 1), 0.5f)`,
**метадата 1** = древесный уголь. Порт это учитывает:
`recipe/charcoal_from_smelting_greatwood_log.json` и
`recipe/charcoal_from_smelting_silverwood_log.json` дают `minecraft:charcoal`
(см. `docs/analysis/2026-10-16-distillation-and-magical-wood.md`: «обе породы
плавятся в настоящий charcoal»), а `tc4/early_object_aspects.json` (binding 21,
источник `ConfigAspects.java:144`) фиксирует
`modern_ids: ["minecraft:coal", "minecraft:charcoal"]` с пометкой «original
metadata0 coal and metadata1 charcoal both inherit the wildcard binding».

В 1.21.1 угля и древесного угля два предмета, поэтому нативный катализатор
`alumentum` перечисляет оба id. **Удаление `minecraft:charcoal` было бы регрессией.**
Аудит утверждал, что «в 1.7.10 `Items.coal` и `Items.charcoal` — разные предметы» —
это неверно.

## 4. Счёт рецептов тигля: 46 = 54 − 8, набор полный

Пересчёт оракула (`initializeAlchemyRecipes`, строки 105-181):

| группа | количество | строки |
|---|---|---|
| `BalancedShard_0..5` | 6 | 113 |
| `Alumentum`, `Nitor`, `Thaumium`, `VoidMetal`, `VoidSeed`, `Tallow` | 6 | 115-120 |
| `Alt*` (Gunpowder, Slime, Clay, Glowstone, Ink, Web, MossyCobble, Ice, CrackedBrick, Bonemeal) | 10 | 121-130 |
| `PureIron`, `PureGold` | 2 | 131-132 |
| `PureCopper`, `PureTin`, `PureSilver`, `PureLead` | 4 | 134-143, **условные** |
| `TransIron`, `TransGold` | 2 | 145-146 |
| `TransCopper`, `TransTin`, `TransSilver`, `TransLead` | 4 | 148-157, **условные** |
| `EtherealBloom`, `LiquidDeath` | 2 | 159-160 |
| `BottleTaint` | 1 | 163 |
| `Golem*` (Straw, Wood, Tallow, Clay, Flesh, Stone, Iron, Thaumium) | 8 | 164-171 |
| `Core*` (Gather, Fill, Empty, Harvest, Guard, Butcher, Liquid) | 7 | 172-178 |
| `BathSalts`, `SaneSoap` | 2 | 179-180 |
| **всего** | **54** | |

Условные восемь (`Config.foundCopperIngot`, `Config.foundTinIngot`,
`Config.foundSilverIngot`, `Config.foundLeadIngot`) регистрируются, только если
чужая руда есть; в 1.21.1 меди, олова, серебра и свинца нет, поэтому они
отсутствуют и в порту (`ResearchRecipeCoverage.DEFERRED` их перечисляет с
причинами).

**54 − 8 = 46**, и в `common/src/main/resources/data/thaumcraft/recipe/crucible/`
ровно 46 файлов. Ни одного настоящего рецепта не потеряно, лишних нет.

Порядок регистрации тоже сохранён: `source_order` 0-25 — первые 26 безусловных
рецептов, дальше номера строк оракула (159, 160, 163-180). Относительный порядок
совпадает с оригиналом, а «пробелы» в номерах соответствуют восьми условным
рецептам. Это важно, потому что тигель берёт рецепт с наибольшим числом различных
аспектов, а при равенстве — **первый зарегистрированный**
(`ThaumcraftCraftingManager.java:127-145` ↔ нативный `CrucibleCrafting.java:21-46`).

Попутно сверены и все 46 значений: катализатор, результат, количество, гейт
исследования и аспекты — все совпадают с оракулом (в том числе
`void_seed` = `minecraft:wheat_seeds`: `Items.field_151014_N` = WHEAT_SEEDS по SRG
`mcp_stable/39/fields.csv:1905`, см.
`docs/goals/2026-08-03-eldritch-full-parity-audit-fixes/reports/A-006-void-recipes.md`).

## Проверка

`common/src/test/java/thaumcraft/assets/CrucibleRecipeParityTest.java` — весь
каталог тигля (46 рецептов) закреплён против оракула: гейт исследования,
катализатор, результат, количество, аспекты и `source_order`; отдельно закреплены
три решения выше (направление цепочки сердечников, уголь+древесный уголь, ведро),
утверждается, что набор равен оракулу без восьми условных рецептов, и что
алембик использует то же ведро.

`GapRecipesAssetsTest` (строка 70) обновлён: катализатор `LiquidDeath` теперь
закреплён как `minecraft:bucket` с комментарием про доказательство из таблицы
аспектов.

## Что осталось за пределами этой правки

* **22 рецепта вне книги исследований** (`Banner_0..15`, `PrimalArrow_0..5`,
  `ConfigRecipes.java:185-189` и `217-220`) в дереве рецептов порта
  отсутствуют; их добавляет другой агент. Спецификация из оракула:
  * `Banner_<цвет>`: арканный shaped-рецепт, гейт `BANNERS`, паттерн
    `WS` / `WS` / `WB`, `W` = шерсть соответствующего цвета
    (`Blocks.field_150325_L`, метадата `a`), `S` = `stickWood`, `B` = `slabWood`,
    виз `WATER 5 + EARTH 5`, результат — `blockWoodenDevice` метадата 8 с NBT
    `color` = байт `a` (в нативе это состояние блока `wooden_device`, поэтому
    результат требует носителя состояния);
  * `PrimalArrow_<аспект>`: арканный shaped-рецепт, гейт `PRIMALARROW`, паттерн
    `AAA` / `ASA` / `AAA`, `A` = `Items.field_151032_g` = `minecraft:arrow`,
    `S` = `itemShard` метадата `a`, виз — 8 соответствующего первичного аспекта,
    результат — 8 `itemPrimalArrow` метадаты `a`
    (`ItemPrimalArrow.java:37-46`: метадаты 0-5 = air/fire/water/earth/order/entropy).
