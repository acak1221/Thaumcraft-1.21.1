# Содержимое Thaumcraft, существа и мир

## Предметы и блоки Thaumcraft

У каждого предмета и блока есть id 1.21.1 в пространстве имён `thaumcraft`.

| Нужно | Вызов |
|---|---|
| предмет по id | `ItemsTC.require("thaumium_ingot")` (бросает, если такого нет), `ItemsTC.get(...)` (null, если нет), `ItemsTC.stack("salis_mundus", 4)` |
| блок по id | `BlocksTC.get("arcane_stone")`, `BlocksTC.require(...)` |
| поле `ConfigItems`/`ConfigBlocks` из 1.7.10 с метаданными | `ItemApi.getItem("itemResource", 14)` (соль мира), `ItemApi.getBlock(name, meta)`: исходные имена разрешаются в плоский предмет, блок или состояние 1.21.1 |

Полная таблица исходных имён — [LEGACY_IDS.md](../../LEGACY_IDS.md). Вызывайте это из `onThaumcraftReady` или позже:
когда выполняется ваш `onInitialize`, предметов Thaumcraft может ещё не быть.

## Стабилизаторы наполнения

Блок с `IInfusionStabiliser` (`canStabaliseInfusion(level, pos)`) стабилизирует наполнение, если стоит симметрично
вокруг алтаря, как свечи и черепа.

## Существа

| Интерфейс | Действие |
|---|---|
| `IEldritchMob` | потустороннее существо: потусторонние мобы Thaumcraft считают его союзником и не нападают |
| `ITaintedMob` | осквернённое существо: газ флюкса, бутылки скверны и обращение скверной его не трогают, щупальца и криперы скверны его не атакуют |
| `IThaumcraftBoss` | босс никогда не становится чемпионом; `generateName()` даёт ему имя по модификатору чемпиона |

Теги существ (их аспекты) — на странице [аспектов](aspects.md#теги-существ).

## Урон и эффекты

Типы урона в 1.21.1 — данные, поэтому источникам урона нужен мир или существо:

| 1.7.10 | 1.21.1 |
|---|---|
| `DamageSourceThaumcraft.taint` | `DamageSourceThaumcraft.taint(level)` |
| `DamageSourceThaumcraft.dissolve` | `DamageSourceThaumcraft.dissolve(level)` |
| `causeTentacleDamage(entity)`, `causeSwarmDamage(entity)` | те же имена |
| `DamageSourceIndirectThaumcraftEntity` | `DamageSourceIndirectThaumcraftEntity.create(direct, cause, type)` |

Исходные зелья — зарегистрированные эффекты: `PotionFluxTaint.instance()` / `holder()` / `KEY`, так же и
`PotionVisExhaust`. Наложить: `new MobEffectInstance(PotionFluxTaint.holder(), duration, amplifier)`.

## Генерация мира

| Вызов | Действие |
|---|---|
| `ThaumcraftApi.addDimBlacklist(ResourceKey<Level>, level)` | исходный числовой чёрный список измерений; 0 или 2 убирают руды и ауру Thaumcraft, любой уровень — его растительность и постройки |
| `ThaumcraftApi.addBiomeBlacklist(ResourceKey<Biome>, level)` | убирает растительность и руды Thaumcraft из биома (узлы ауры всё равно появляются) |
| тег блоков `thaumcraft:portable_hole_blacklist` | блоки, которые фокус переносной дыры не откроет (исходный `portableHoleBlackList`, теперь данные) |

## Мешки с добычей

`ThaumcraftApi.addLootBagItem(stack, weight, bagTypes...)` добавляет запись в мешки с добычей Thaumcraft; типы мешков:
0 — обычный, 1 — необычный, 2 — редкий (пример добавляет свой осколок в первые два; см.
[рецепты](recipes.md#дополнительно-к-крафту)).

## Рубка деревьев

`ThaumcraftApiHelper.isWoodLog(level, pos)` и `breakFurthestBlock(level, pos, block, player)` — два помощника, которыми
пользовался Топор потока (`Utils.isWoodLog`, `BlockUtils.breakFurthestBlock`). Аддон из примера валит дерево, когда бревно
ломают с аметистовой сердцевиной в руке:

<!-- snippet:felling -->
```java
// Breaking a log with the amethyst core in the hand fells the whole tree, the way the Axe of the Stream did. One call
// of breakFurthestBlock breaks ONE block (the far end of its walk) and wears no tool; the loop, its limit and the
// permission check are the caller's. The last call of the loop breaks the clicked log itself, so the break is cancelled.
PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
    if (!(level instanceof ServerLevel server) || player.isShiftKeyDown() || !player.getMainHandItem().is(AMETHYST_ROD)
            || !ThaumcraftApiHelper.isWoodLog(server, pos) || !server.mayInteract(player, pos)) return true;
    for (int logs = 0; logs < 64 && ThaumcraftApiHelper.breakFurthestBlock(server, pos, state.getBlock(), player); logs++) { }
    return false;
});
```
<!-- /snippet -->

- `isWoodLog` — правило обхода, а не определитель дерева: блок из тега `#minecraft:logs` проходит, листва тоже, обычное
  дубовое бревно — нет (см. `docs/audit/KNOWN_DEVIATIONS.md`, раздел 9). Позиция в незагруженном чанке — не бревно.
- `breakFurthestBlock` ломает **один** блок за вызов: конец жадного обхода, а не самое дальнее бревно дерева. Обход
  начинается в `pos` и раз за разом шагает в первую клетку в пределах двух блоков (перебор: x от −2 до +2, y от +2 вниз до
  −2, z от −2 до +2), где стоит ровно `block`, которая является бревном и дальше от `pos`, чем текущая. Он перешагивает
  промежутки в один блок и диагонали, может уйти на соседнее дерево той же породы и не выходит за 24 блока в стороны и 48
  вверх и вниз. Если ни одна клетка не подходит, ломается сам `pos`, **какой бы блок там ни был**: сначала проверьте
  `isWoodLog`.
- Блок выпадает с обычной добычей (она определяется предметом в основной руке игрока; для творческого игрока и для null
  выпадения нет) там, где он стоял. Инструмент **не** изнашивается, событие разрушения не вызывается и никто не спрашивает
  разрешения, защиты спауна или приватов для сломанной клетки: это забота вызывающего, как в примере.
- Ответ true, когда в сборщик отдан ломаемый блок, не воздух; false для клиентского мира, незагруженного старта, старта в
  воздухе и неразрушимого конца, поэтому `while (breakFurthestBlock(...))` заканчивается, когда стартовая клетка стала
  воздухом; всё равно поставьте предел. Только поток сервера (в другом — `IllegalStateException`): обход хранит
  состояние в статических полях, как в оригинале.

## Не публичный API

- У големов не было API в 1.7.10, нет и сейчас.
- Классы `thaumcraft.api` в духе Thaumcraft 6 (`theorycraft`, `ResearchEntry`/`ResearchStage`, `ScanningManager`,
  `IWandFocus`) — оболочки совместимости для аддонов, собранных под 1.12; к механикам Thaumcraft 4 они не подключены.
  Пользуйтесь классами TC4 из этих руководств.
- Всё вне `thaumcraft.api` (`thaumcraft.common`, `thaumcraft.client`) может измениться без предупреждения.
