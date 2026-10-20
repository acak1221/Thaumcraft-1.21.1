# Аудит соответствия порта: Эльдрич и генерация мира (a5)

Дата: 2026-10-21. Домен: `eldritch_and_outer_lands`, `natural_resources_and_worldgen`,
`world_structures_and_ecology`. 103 строки реестра (`docs/porting/scope-ledger.json`),
ориентир — `Thaumcraft-1.7.10-4.2.3.5.jar` (декомпиляция в
`/home/Acak/work/thaumcraft-4.2.3.5-oracle/`), порт — `common/src/main/java/thaumcraft/`
ветки `work` (коммит `a6ec3ad`).

## Итог

| вердикт | количество |
|---|---|
| `disposition: adapt` | 70 |
| `disposition: replace` | 23 |
| `disposition: port` | 6 |
| `disposition: exclude` | 3 |
| `disposition: needs_review` | 1 |
| **всего строк** | **103** |

| `native_coverage` | количество |
|---|---|
| `partial` | 75 |
| `parity_verified` | 24 |
| `not_assessed` | 3 |
| `missing` | 1 |

| `source_semantics` | количество |
|---|---|
| `adapted` | 87 |
| `equivalent` | 7 |
| `not_applicable` | 5 |
| `different` | 3 |
| `unreviewed` | 1 |

| `callsite_audit` | количество |
|---|---|
| `complete` | 56 |
| `partial` | 47 |

Главный результат аудита: **реальное измерение Внешних Земель в порту существует** (это не
«биом и документ, который сервер не читает»), но **генератор лабиринта расходится с оригиналом
на каждом сиде**, а целый класс поверхностных структур (деревни, каменные холмы, тотемы, дикие
аура-узлы) в порту зарегистрирован, но никогда не размещается.

## Что проверено и совпадает

* **Измерение.** `data/thaumcraft/dimension/outer_lands.json` (level stem) + `dimension_type/outer_lands.json`.
  В 1.21.1 ванильный `MinecraftServer.createLevels` (проверено по байт-коду
  `minecraft-merged-mojang.jar`, `fabric-loom/1.21.1/neoforge/21.1.259/`) итерирует реестр
  `Registries.LEVEL_STEM` и создаёт `ServerLevel` для каждого level stem, кроме оверворлда, с
  ключом `Registries.DIMENSION.create(Registries.DIMENSION, <id>)`. Ключ нативного порта
  `ResourceKey.create(Registries.DIMENSION, "thaumcraft:outer_lands")` совпадает с этим
  построением, значит уровень создаётся при старте сервера и доступен. Переход игрока —
  `TileEldritchPortal.transfer` (`common/.../tiles/TileEldritchPortal.java:93-133`).
* **Диспетчеризация комнат.** `OuterLandsRules.roomForFeature` (`OuterLandsRules.java:129-138`)
  повторяет `MazeHandler.generateEldritch:152-186` (признаки 1 / 2..5 / 6 / 7 / 8 / остальное).
* **Правила кольма.** Радиус 3, пять слоёв пола, четыре зачищенных слоя, один из четырёх
  пустых блоков пола, бросок алтаря 1..4 и 6..7, четыре баннера с исходными значениями
  граней 8/0/12/4, обелиск 1+4 — совпадают с `WorldGenEldritchRing:64-142`.
* **Прочность/свет/взрывоустойчивость семейства eldritch** и **текстуры всех 11 исходных
  metadata** — совпадают (`EldritchBlockRules.java:43-115`, `blockstates/eldritch*.json`).
  Все исходные metadata 0..10 достижимы: 0..3 → `thaumcraft:eldritch`, 4..5 → `eldritch_crust`,
  6 → `eldritch_deco`, 7..8 → `eldritch_doorway`, 9..10 → `eldritch_spawner`.
* **Тайминг алтаря** (80 тиков, период 40), четыре клирика, порог успеха, лимит cultist 8,
  радиусы дома 8/16, размер лабиринта `15 + nextInt(8)*2` — совпадают.
* **Сетка стен 2x2 босс-комнаты**, петли `Gen2x2` — совпадают.

## Отклонения, требующие решения

1. **Генератор лабиринта: маркеры комнат не совпадают ни для одного сида.**
   Оригинал `MazeGenerator.java:332` пропускает ячейку, у которой нет ни одного выхода
   (`!c.north && !c.south && !c.west && !c.east`), и **не** расходует случайное число.
   Порт `common/.../lib/world/MazeGenerator.java:331-334` пропускает такую ячейку только
   при наличии выхода, то есть присваивает ей случайную комнату и дополнительно вытягивает
   `nextInt(8)`. Проверка: оригинал и порт собраны рядом и сравнены на размерах 15..29 и
   40 сидах (159 821 ячейка): **45 347 ячеек различаются, и все различия — в байте признака**;
   стены совпадают полностью. Возврат одного условия оригинала в порт делает генераторы
   побайтово идентичными на всех 159 821 ячейках. Следствие: расходится не только «мусор» на
   границе, но и ключевая комната, гнездовая, библиотечная и все проходные комнаты
   лабиринта, а значит и весь маршрут игрока по Внешним Землям.
2. **Бросок «заложенного камня» в проходе.** Оригинал `GenPassage.java:54-56` берёт
   `nextInt(3)` дважды (пол и потолок), порт `DungeonRooms.java:425-427` один раз на пару —
   расходится поток случайных чисел всех проходов с признаком 11.
3. **Редкость добычи в гнездовой комнате.** Оригинал `GenNestRoom.java:117` использует
   таблицу 15 % / 25 % / 60 %, порт `DungeonRooms.java:917` + `OuterLandsRules.lootUrnMeta`
   используют общую таблицу декораций 2.5 % / 7.5 % / 90 %. В гнезде лут заметно беднее.
4. **Смещение стражей ключевой комнаты.** Оригинал `GenKeyRoom.java:105-107`:
   `(1..3) * (-1..0)`, то есть никогда не правее центра. Порт `DungeonRooms.java:821-822`:
   `(1..3) * (-1..1)` — страж может встать на три блока дальше, чем в оригинале.
5. **Замок босс-комнаты не получает направление.** Оригинал `GenBossRoom.java:72-75` ставит
   `TileEldritchLock.setFacing(dir.ordinal())`. Порт `DungeonRooms.java:1064-1067` только
   размещает блок, tile не конфигурируется — замок всегда «смотрит» в дефолтную сторону.
6. **Верхняя полка в библиотечной комнате.** Оригинал `GenLibraryRoom.java:86,96` кладёт
   slab с metadata 9 (верхняя половина). Порт `DungeonRooms.java:1016` пишет вариант 9 в
   целочисленное свойство, но slab — это `SlabBlock` со свойством `type` (Half), а не
   `variant`, поэтому `ThaumcraftOuterLands.withVariant` ничего не меняет: все столы
   получают нижнюю половину.
7. **Нижний правый угол портальной комнаты.** Оригинал `GenPortal.java:84` заменяет блок
   на воздух и для области `a>=12 && b>=12`; порт `DungeonRooms.java:697-705` чистит только
   три других угла — там остаётся «невидимая стена» (`eldritch_nothing`).
8. **Кольцо не создаёт аура-узел.** Оригинал `ThaumcraftWorldGenerator.java:446` создаёт
   eerie-узел над алтарём кольца; порт `EldritchRingFeature.java` этого не делает. Из-за
   этого же меняется и сид лабиринта (в оригинале `random.nextLong()` идёт после генерации
   узла, в порту — сразу после кольца).
9. **Центр кольца смещается.** Оригинал берёт `chunkX*16 + random.nextInt(16)` (равномерно),
   порт `EldritchRingFeature.java:52-53,86-91` зажимает центр в диапазон 3..12 блока чанка
   (вынужденно из-за генерации внутри чанка в 1.21).
10. **Цвета эльдрич-биома в документе.** `worldgen/biome/eldritch_lands.json` пишет
    `fog_color`, `grass_color`, `foliage_color` = 0, тогда как `OuterLandsRules.java:53-59`
    хранит исходные значения 1.7 (`0xC0C0FF`, `0xC6D3FF`). Правило и datapack противоречат
    друг другу.
11. **Отсутствуют эффекты измерения.** `dimension_type/outer_lands.json` ссылается на
    `thaumcraft:outer_lands` в реестре dimension effects, но файла
    `data/thaumcraft/dimension_effects/outer_lands.json` нет — клиент берёт эффекты оверворлда
    вместо пустоты оригинала (в 1.21.1 поле `effects` имеет значение по умолчанию, поэтому
    измерение грузится, но выглядит не так).

## Дефекты

1. **D1 — `MazeGenerator` (критично).** Описание в «Отклонениях», п. 1. Воспроизведение:
   сгенерировать лабиринт для (w=h=15, seed=1) оригиналом и портом; строка y=0 содержит
   в оригинале `0x0` (нет признака), в порту `0xb00/0xe00/0xd00/...` (случайные комнаты).
   Проверяется сравнением сеток: 45 347 расхождений из 159 821 ячейки.
2. **D2 — визуальные структуры порта никогда не генерируются.** `hilltop_stones`,
   `village_banker_homes`, `village_wizard_towers` зарегистрированы как features и имеют
   `placed_feature`, но **ни один `biome_modifier` и ни один биом их не ставит**
   (в `data/thaumcraft/worldgen/` есть единственный модификатор — `add_eldritch_ring.json`).
   То же с `normal_aura_nodes`: `placed_feature/normal_aura_nodes.json` содержит пустой
   список `"placement": []`, поэтому дикие аура-узлы в оверворлде не появляются, хотя
   оригинал их генерирует (`ThaumcraftWorldGenerator.generateWildNodes:189-216`,
   `Config.nodeRarity = 36`).
3. **D3 — «биом в документе, который никто не читает».** `data/thaumcraft/worldgen/biome/eldritch_lands.json`
   объявляет `features[9] = ["thaumcraft:tc4_outer_lands_maze"]`, а `placed_feature/tc4_outer_lands_maze.json`
   ставит фичу без модификатора `count` (то есть одна попытка на чанк) и с фильтром `biome`.
   Генератор лабиринта при этом читает клетки из saved data, которые записывает кольцо в
   оверворлде. Связка работает, но она неочевидна и не покрыта сквозным тестом
   «кольцо в оверворлде → клетки лабиринта → комната в Внешних Землях»: тест
   `outer_lands_maze_uses_the_original_cell_layout` проверяет только сам генератор на
   синтетической сетке.
4. **D4 — постоянные константы без данных.** `OuterLandsRules.WATER_COLOR = 0x3F76E4` и
   `BIOME_FOG_COLOR = 0xC0C0FF` расходятся с `eldritch_lands.json` (`0x3F76E4` совпадает,
   `fog_color` = 0 расходится). Проверяется сравнением JSON и констант.
5. **D5 — постоянный предмет ключевой комнаты теряет metadata.** Оригинал
   `GenKeyRoom.java:97` кладёт `new ItemStack(itemEldritchObject, 1, 2)`; порт
   `ThaumcraftOuterLands.spawnPermanentItem` (`ThaumcraftOuterLands.java:315-324`) создаёт
   предмет без варианта 2.

## Не перенесено

* **`WorldGenMound` (оригинал, 2550 строк) — отсутствует.** В порту нет класса-модели
  «холм»; `StructurePlacementRules.MOUND_CHANCE = 150` остался, но ветка бросает 1/150 и
  ничего не строит (`VillageWorldGenRules.java:569` это фиксирует). `WorldGenMoundTemplate`
  (только в легаси 1.12) исключён как вспомогательный класс без оригинала.
* **Тотем ауры оригинала** (`ThaumcraftWorldGenerator.generateTotem:148-187`, 1 чанк из 360
  при `Config.nodeRarity = 36`) — в порту нет ни тотема, ни его правила, кроме константы
  `TOTEM_CHANCE`.
* **`WorldGenSilverwoodTreesOld`** — второй, старый силуэт серебряного дерева не перенесён,
  в порту один генератор серебряного дерева.
* **`EntityCultistPortalGreater`** существует только в легаси 1.12 (в оригинале нет) —
  нативный переносит сущность в плоский пакет `entities/monster`.
* **Клиентские диагностики `EldritchDiagnostics` и `ItemEldritchRenderer`** — только легаси
  1.12, в оригинале их нет; исключены.
* **Не реализовано как отдельный класс:** `MapBossData` (заменён поквартальной генерацией
  босс-комнаты), `CellLoc` (заменён упакованным `long`-ключом), `MazeHandler`/`MazeThread`
  (заменены saved data оверворлда), `ThaumcraftWorldGenerator` (заменён набором 1.21-фич).

## Что НЕ удалось проверить

* **Пиксельная и геометрическая паритетность клиентских рендереров** (17 строк семейства
  `client/renderers/**`): переход на 1.21.1-систему моделей слишком велик для сверки «по
  числам» в этом аудите; сверены только наличие классов и общая структура. Все эти строки
  помечены `partial`.
* **Поведение сущностей** (культисты, эльдрич-краб, гардиан, гolem, warden, орб) — сверены
  только атрибуты гардиана (50 HP / 40 / 0.28 / 7 / 4 — совпадают) и правила краба из
  `EldritchMobRules`; остальное помечено `partial`.
* **Тайлы** `TileEldritchLock`, `TileEldritchObelisk`, `TileEldritchTrap`,
  `TileEldritchCrabSpawner` — сверены только структура и то, что они зарегистрированы и
  тикают; числа оригинала построчно не сверялись.
* **Списки блоков деревьев и цветов** (`WorldGenBigMagicTree`, `WorldGenGreatwoodTrees`,
  `WorldGenSilverwoodTrees`, `WorldGenCustomFlowers`, `WorldGenManaPods`) — сверено только
  наличие переноса, формы не сравнивались.
* **Живой прогон генерации в мире**: аудит — чтение кода; генерация кольца и лабиринта в
  работающем мире не наблюдалась (сборочный прогон Gradle не запускался). Все числовые
  утверждения о лабиринте получены сравнением двух скомпилированных копий генератора.
* **Поведение `MazesInRange` при гонке** (в оригинале лабиринт строится в отдельном потоке,
  в порту синхронно) — не воспроизводилось.

## Артефакты

* `docs/porting/audit-reviews-a5-eldritch.json` — 103 ревью (`canonical_path -> review`),
  `reviewed_sha256` содержит точные хэши `original`/`legacy` из
  `docs/porting/scope-ledger.json`.
* Внешняя проверка: `python3 scripts/porting/audit_ledger.py --merge
  docs/porting/audit-reviews-a5-eldritch.json` (проверено локально на копии леджера).
