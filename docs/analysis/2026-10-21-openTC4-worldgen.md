# Сверка генерации мира TC4 4.2.3.5 (1.7.10) с портом 1.21.1

**Дата:** 21.10.2026 · **Ветка:** `port/ocworldgen` · **Оракул:** `/home/Acak/work/thaumcraft-4.2.3.5-oracle`
**Справочник имён ваниллы:** OpenTC4 (`/home/Acak/work/opentc4`, читаемый checkout 1.7.10) и
`scripts/porting/resolve_srg.py` для SRG-имён.

Область: `MazeGenerator`, `DungeonRooms`, `OuterLandsRules`/`OuterLandsWiringRules`,
`ThaumcraftOuterLands`, `ThaumcraftVillages`, `VillageWorldGenRules`, `StructurePlacementRules`,
`VillageBankerManager`/`VillageWizardManager`, `WorldGenHilltopStones`, дикие аура-узлы, биомы.

---

## 1. Итог

| Дефект | Оракул | Порт | Статус |
|---|---|---|---|
| D1. Дома деревни теряются на границе ячейки (≈6 % домов) | `VillageBankerManager:58-60`, `VillageWizardManager:55-57` | `VillageWorldGenRules.villagePlan` | **исправлено** |
| D2. Запись «place() деревенских кусков не вызывается» | — | `SurfaceStructurePlacementRuntimeChecks` (старый комментарий) | **опровергнуто прогоном, запись исправлена** |
| D3. Банкир покупает книгу за 7..9 монет вместо 7..8 | `VillageBankerManager:47` | `VillageWorldGenRules.bankerTrades()` | **исправлено** |
| D4. Волшебник покупает изумруд за золото вместо ртути и янтаря | `VillageWizardManager:39,41` | `VillageWorldGenRules.wizardTrades()` | **исправлено** |
| D5. В полу кольца один блок из четырёх был воздухом вместо обсидиана | `WorldGenEldritchRing:75-79` | `EldritchRingFeature.buildColumn` | **исправлено** |
| D6. Кольцо и холм не создавали тёмный аура-узел оригинала | `ThaumcraftWorldGenerator:446,452` | `EldritchRingFeature`, `WorldGenHilltopStones` | **исправлено** |
| D7. Список блоков, заполняемых под холмом, был шире оригинала | `WorldGenHilltopStones:81` | `WorldGenHilltopStones.replaceable` | **исправлено** |
| G1. Тотем ауры оригинала (1 чанк из 360) отсутствовал | `ThaumcraftWorldGenerator:148-187` | константа `TOTEM_CHANCE` без кода | **перенесено** |
| F1. Холмовые камни оригинала — мёртвый код | `ThaumcraftWorldGenerator:424-425` + `WorldGenHilltopStones:36` | поведение совпадало, тест врал | **зафиксировано, тест исправлен** |

Проверено и признано совпадающим: генератор лабиринта (побайтово), цепочка поверхностных структур
и её частоты, редкость диких узлов, веса деревенских кусков, состав домов, содержимое сундука башни,
раскладка эльдрич-кольца, атрибуты биомов.

Пробелы (не чинил, см. §6): `WorldGenMound` (1/150 ветки цепочки), привязка торгов к профессии
деревенского жителя, два предмета `itemNuggetChicken`/`itemNuggetBeef` в таблице торгов,
`biome.topBlock` как «поверхностный блок среднего столбца», невидимый блок узла 1.7 (`blockAiry`).

---

## 2. Частоты и порядок (задача 1)

Оригинальная цепочка (`ThaumcraftWorldGenerator.generateSurface:403-457`):

```
random.nextInt(16) -> x            (423)
random.nextInt(16) -> z            (424)
randPosY = getHeightValuePos(x,z) - 9
if (randPosY < seaLevel) {         (425)  <- порог поверхности
    nextInt(150) == 0 -> mound      (428)
    else nextInt(66) == 0 -> ring  (435)
    else nextInt(40) == 0 -> hilltop(450)
}
```

Порт: `StructurePlacementRules.draw` (`common/src/main/java/thaumcraft/common/lib/world/StructurePlacementRules.java:155-159`)
рисует позицию первыми двумя `nextInt(16)`, затем `select` — те же 150/66/40 в том же порядке и с
той же эксклюзивностью. Порог поверхности повторяет оригинал: `EldritchRingFeature.place:62-65`
(`surface - 9 < seaLevel` → отказ), `WorldGenHilltopStones.place:71-72` — то же.

Дикие аура-узлы: оригинал `generateWildNodes:190` — `random.nextInt(Config.nodeRarity) == 0`
при `Config.nodeRarity = 36` (1/36). Порт: `data/thaumcraft/worldgen/placed_feature/normal_aura_nodes.json`
— `rarity_filter` с `chance: 36`; в 1.21.1 `RarityFilter` = «в среднем раз в N»
(`nextFloat() < 1/N`), то есть частота 1/36 совпадает (проверено по коду `RarityFilter` из 1.21.1).

Тотем ауры: оригинал `generateTotem:149` — `random.nextInt(Config.nodeRarity * 10) == 0`, то есть
1/360, и только в оверворлде (`generateTotem:149` требует `dimensionId == 0`), только в новом мире
(`newGen`) и только если чанк ещё не получил аура-узел (`!auraGen`).

---

## 3. Генератор лабиринта (задача 2)

Оракул `thaumcraft/common/lib/world/dim/MazeGenerator.java:15-444` и порт
`common/src/main/java/thaumcraft/common/lib/world/MazeGenerator.java` сверены построчно:

* порядок розыгрыша `java.util.Random` (старт в углу `nextInt(4)`, маркеры 512/768/1024/1280,
  центр 256, «слепые» пятна `nextInt(3)`, `Collections.shuffle` направлений, `getNextIndex`
  45 %/45 %/10 %) — совпадает;
* битовая раскладка ячейки (N=1, S=2, E=4, W=8, признак в битах 8..15) — совпадает;
* **важно:** ячейка без единого выхода пропускается без розыгрыша `nextInt(25)`
  (оракул `:332`, короткое замыкание `||`) — порт сохраняет порядок short-circuit
  (`MazeGenerator.java:336-338`), иначе сдвинулся бы весь поток случайных чисел лабиринта;
* `CellLoc` оригинала хранит `x` = номер строки, `z` = номер столбца; порт использует `Loc` с теми
  же индексами (`MazeGenerator.java:311-313`), геометрия та же.

Доказательство: `common/src/test/java/thaumcraft/world/MazeFidelityTest.java` сравнивает порт с
транскрипцией оракула (`OracleMazeGenerator`) по 8 размерам × 40 сидов; расхождений 0
(317 успешных лабиринтов, 159 821 ячейка). Тест входит в `:common:test` и проходит.

---

## 4. Комнаты подземелья (задача 3)

Класс `DungeonRooms` (1302 строки) — нативная замена оракуловых
`GenCommon`/`GenPassage`/`GenPortal`/`GenKeyRoom`/`GenLibraryRoom`/`GenNestRoom`/`GenBossRoom`
(1433 строки). Числа сверены построчно по оракулу:

| Правило | Оракул | Порт |
|---|---|---|
| сечение тоннеля 11×11 | `GenCommon.PAT_CONNECT:49` | `DungeonRooms.PAT_CONNECT:56` |
| дверь босс-комнаты 7×7 | `GenBossRoom.PAT_DOORWAY:22` | `DungeonRooms.PAT_DOORWAY:70` |
| метаданные ступеней по направлению | `GenCommon.placeBlock:195-260` | `DungeonRooms.stairsMeta:312` |
| окно слоя прохода | `GenCommon.generateConnections:335-338` | `DungeonRooms.layerStart/layerEnd:392-398` |
| ловушка в полу/потолке | `GenPassage` | `DungeonRooms.generatePassage:412` |
| редкость урн гнезда (15 %/25 %/60 %) | `GenNestRoom` | `OuterLandsRules.nestLootUrnMeta` (тест `OuterLandsRoomFidelityTest`) |
| три стража ключевой комнаты | `GenKeyRoom` | `DungeonRooms.generateKeyRoom:756` |
| варианты замка босс-комнаты | `GenBossRoom` | `DungeonRooms` (тест `theBossRoomLockTakesTheDirectionOfItsSide`) |
| предмет ключевой комнаты, metadata 2 | `GenKeyRoom:97` | `DungeonRooms` (тест `theKeyRoomKeepsThePermanentItemOfTheOriginalMetadata`) |

Покрытие тестами: `common/src/test/java/thaumcraft/world/OuterLandsRoomFidelityTest.java`
(13 кейсов) и `MazeFidelityTest` (2 кейса) — проходят в `:common:test`.
Новых дефектов в этом семействе не найдено; изменения a5/port-fixmaze в этом прогоне не
переигрывались (проверялась согласованность правил, а не повторная сверка всех 1433 строк).

---

## 5. Деревня (задача 4) — главный результат

### 5.1 «place() не вызывается» — запись неверна, причина найдена

Запись была в комментарии `SurfaceStructurePlacementRuntimeChecks` (до правки, строки 34-36):
«The two village components are wired the same way, but their feature is not reached by the chunk
generation of the game test server».

**Факт:** `place()` вызывается, и оба куска строятся. Доказательство — новый игровой тест
`common/src/gameTest/java/thaumcraft/common/testing/VillageComponentPlacementRuntimeChecks.java`
(зарегистрирован в `fabric/.../ThaumcraftGameTests.java` как `villageComponentsReachTheOverworld`):
он находит по плану деревни чанки с домом банкира и башней волшебника, генерирует их в свежем мире
и находит дубовую дверь компонента. Прогон на удалённом мире (`rm -rf fabric/run-gametest/world`):

```
thaumcraftgametests.surfacestructuresreachtheoverworld  4.628 PASSED
thaumcraftgametests.villagecomponentsreachtheoverworld 1.807 PASSED
BUILD SUCCESSFUL
```

**Настоящая причина «деревни не видно» — плотность.** Измерение по 40 000 чанков (скрипт по
`VillageWorldGenRules`): 283 чанка с домом, из них 71 дом банкира и 49 башен волшебника, то есть
**1 дом банкира на ≈560 чанков и 1 башня на ≈820 чанков**. При этом 1.7.10 размещает куски
деревни по системе кусков, и деревня возникает не в каждой ячейке 32×32 — так что порядок величин
совпадает, а «не видно» — это нормальная редкость TC4, а не поломка.

Но при измерении нашлась **настоящая** потеря домов (D1): якорь деревни был случайным чанком
ячейки 32×32, а слоты домов лежат в радиусе два чанка от якоря. Слот, вышедший за границу ячейки,
никогда не строился: чанк за границей спрашивает план **своей** ячейки, у которого другой якорь.
Оригинал such losses не has (куски одной деревни получает одна система кусков).

**Фикс** (`VillageWorldGenRules.anchorOffset`, `VillageWorldGenRules.java:150-158`): смещение якоря
внутри ячейки ограничено `[HOUSE_SLOT_RADIUS, VILLAGE_CELL_CHUNKS - 1 - HOUSE_SLOT_RADIUS]` =
`[2, 29]`, поэтому все 13 слотов всегда внутри своей ячейки. Оригинал такой потери не имеет:
куски одной деревни получает одна система кусков, и она не выходит за пределы своей деревни.
Доказательство — новый юнит-тест `noHouseOfAVillageIsLostAtTheCellBorder` (0 потерянных домов на
32 сида × 16 ячеек) и `everyHouseSlotStaysInsideItsOwnVillageCell`.

### 5.2 Таблицы торгов

Оракул `VillageBankerManager:42-55` и `VillageWizardManager:36-52` сверены построчно:

| Оракул | Порт (было) | Порт (стало) |
|---|---|---|
| `VillageBankerManager:47` — книга за `7 + nextInt(2)` = 7..8 монет | 7..9 ❌ | **7..8** ✅ |
| `VillageWizardManager:39` — `itemResource` metadata 3 (ртуть, `ConfigItems:274,283`) × 4..6 | золото ❌ | **`thaumcraft:quick_silver`** ✅ |
| `VillageWizardManager:41` — `itemResource` metadata 6 (янтарь, `ConfigItems:275`) × 4..6 | золото ❌ | **`thaumcraft:amber_gem`** ✅ |

Остальные 12 торгов банкира и 15 торгов волшебника совпадают (диапазоны 20..22, 2..3, 6..8, 3..4,
16..20, 9..12, 22..24, 10..12, 25..32; 24..31; 1..2; 5..7; 2..3; порядок 6 осколков
`itemShard` = air/fire/water/earth/order/entropy = `ConfigItems:267-272`).

### 5.3 Состав домов

`ComponentBankerHome:68-111` и `ComponentWizardTower:59-97` сверены блок за блоком с
`VillageWorldGenRules.bankerHome/wizardTower`:

* дом банкира 4×5×6, пол из булыжника, земляной пол 2×3, крыша из брёвен в двух вариантах
  (высокий/низкий), кромка крыши, четыре угловых столба, стены из досок, два окна изIron_bars,
  стол из стены `cobblestone_wall` + `stone_pressure_plate` в одном из трёх слотов, дверь в
  проёме 1×2, ступень `stone_stairs` перед дверью, очистка над домом и булыжный цоколь;
  житель в локальной точке (1,1,2) — `ComponentBankerHome:112` = `spawnVillagers(world, bb, 1, 1, 2, 1)`;
  дверь ставится `placeDoorAtCurrentPosition` (OpenTC4 `ComponentBankerHome:92`), то есть
  `wooden_door` с metadata 1 — порт ставит `DOOR_BOTTOM`/`DOOR_TOP` в (1,1,0)/(1,2,0) ✅;
* башня волшебника 6×6×12, три дощатых пола, четыре каменных пилона, углы на трёх уровнях,
  шесть стеклянных панелей, лестница 1..9 с люком на 10, глоустоун на 5, сундук на (2,6,2),
  дверь в (3,1,1), очистка над башней и булыжный цоколь;
  `spawnVillagers(world, bb, 7, 1, 1, 1)` (OpenTC4 `ComponentWizardTower:95`) — x=7 вне
  шестишаговой коробки куска, поэтому житель в оригинале **не появляется**; порт это фиксирует
  (`WIZARD_TOWER_VILLAGER_X = 7`) ✅;
* содержимое сундука башни — 10 записей с весами 3/10/5/5/3/20/5/5/5/20 (сумма 81) и
  `4 + nextInt(6)` предметов: `ComponentWizardTower:35` = порт
  `VillageWorldGenRules.towerChestContents` ✅ (проверено тестом `towerChestContentKeepsTheOriginalWeights`).

---

## 6. Поверхностные структуры, кольцо, холм, тотем

### 6.1 Кольцо (D5, D6)

Оракул `WorldGenEldritchRing:64-145` (1.7.10, читаемо в OpenTC4 `WorldGenEldritchRing:60-130`):

* 5 проб почвы `(-3,-3) (0,0) (3,0) (3,3) (0,3)` и `MazeHandler.mazesInRange` — совпадает;
* пол/стены: 5 слоёв от базы вниз и 4 вверх, **один блок из четырёх — обсидиан**
  (`WorldGenEldritchRing:75-79` → OpenTC4 `world.setBlock(x, j + q, z, Blocks.obsidian)`);
  порт ставил в этот бросок **воздух** (`EldritchRingFeature.buildColumn`), то есть в полу кольца
  были дыры. **Исправлено**: правило вынесено в `StructurePlacementRules.ringFloor` (тест
  `theRingFloorKeepsTheOriginalObsidianRoll`);
* очистка над кольцом (`if (q > 0) world.setBlockToAir(...)`, OpenTC4 :83-85) — в порту
  корректно, это не «пробел» (аудит a5 читал SRG `func_147468_f` как рендер-апдейт, но в 1.7.10
  это `setBlockToAir` — сверено с OpenTC4);
* алтарь: 10 вариантов, 1..4 — «спавнер типа 0» + четыре знамени (8/0/12/4), 6..7 — «тип 1» —
  совпадает (`StructurePlacementRules.altarVariant`, `bannerFacing`);
* **D6:** оригинал вешает над алтарём тёмный аура-узел —
  `ThaumcraftWorldGenerator:446 createRandomNodeAt(world, x, y+2, z, random, false, true, false)`.
  Порт узла не создавал. **Исправлено** (`EldritchRingFeature.place`), игровой тест проверяет узел
  в колонне кольца.

### 6.2 Холмовые камни (D6, D7, F1)

* **F1 (важно).** `ThaumcraftWorldGenerator:424-425` рисует поверхностную структуру только если
  `getHeightValuePos - 9 < seaLevel` (поверхность ниже 72), а `WorldGenHilltopStones:36` требует
  базу `j >= 85`. Правила **никогда не выполняются одновременно**: в оригинале 4.2.3.5 холмовые
  камни — мёртвый код. Порт воспроизводит оба правила, то есть ведёт себя так же.
  Старый игровой тест утверждал, что кластер появляется в горах (база ≥ 95) — это невозможно по
  правилам оригинала; тест проходил только на **залитом мире** прошлой сессии. Тест переписан:
  он проверяет цепочку, конфликт правил и отсутствие кластера.
* **D7.** `WorldGenHilltopStones:81` заполняет под кластером только снежный слой, красный и жёлтый
  цветок, траву и воздух. Порт добавлял ещё папоротник и мёртвый куст — убрано.
* **D6.** Оригинал вешает тёмный узел на 5 блоков выше базы (`ThaumcraftWorldGenerator:452`) —
  порт не создавал; исправлено (код мёртвый вместе с кластером, но правило оригинала сохранено).

### 6.3 Тотем ауры (G1)

Оракул `ThaumcraftWorldGenerator:148-187` — 1 чанк из 360: столб из cosmetic solid (metadata 0)
на земле (metadata 1) и тёмный аура-узел сверху (1-в-4 бросок даёт узел на высоте колонны, иначе
узел на 5 блоков выше земли). В порту была только константа `TOTEM_CHANCE` (это отмечено в аудите
a5 как «не перенесено»).

Перенесено: `common/src/main/java/thaumcraft/common/lib/world/ThaumcraftTotemFeature.java`
(регистрация `ThaumcraftNodes.AURA_TOTEM`, `placed_feature/aura_totem.json`,
`configured_feature/aura_totem.json`, подключение в Fabric через `BiomeModifications` и в NeoForge
через `neoforge:add_features`). Частота 1/360, правила земли (трава/песок/земля/камень), пропуск
снега и травы, минимальная высота 40, высота колонны 4 и позиция узла — как в оригинале.

Отклонение (документируется): оригинал пропускает тотем в чанке, только если структура **реально
построилась** (`auraGen`); порт пропускает чанк, если цепочка **выбрала** структуру. Различия видны
только в чанках, где кольцо не прошло проверку почвы.

---

## 7. Биомы (задача 5)

| Параметр | Оракул 1.7.10 | Порт 1.21.1 | Итог |
|---|---|---|---|
| id биомов | 192 taint, 193 magical forest, 194 eerie (`Config:831-833`) | `thaumcraft:taint`, `thaumcraft:magical_forest`, `thaumcraft:eerie`, `thaumcraft:eldritch_lands` | namespaced — вынужденное сглаживание |
| Magical Forest: температура/осадки | `0.7 / 0.6` (`BiomeGenMagicalForest:77`) | `temperature 0.7, downfall 0.6` | ✅ |
| Magical Forest: трава/листва/вода | `0x55FF81 / 0x66FFC5 / 30702` | те же | ✅ |
| Eerie: температура/осадки | `0.5 / 0.5` (`BiomeGenEerie:48`) | те же | ✅ |
| Eerie: трава/листва/небо/вода | `0x404840 / 0x405340 / 0x222299 / 0x2E535F` | те же | ✅ |
| Спавн-листы | волк 2/1-3, лошадь 2/1-3; ведьма 3, эндермен 3, печ 10, визг 10 | `data/thaumcraft/worldgen/biome/magical_forest.json` | ✅ (вес/мин/макс сверены) |
| Деревья биома | 1 серебряное из 14, затем 1 великое из 10, иначе большое волшебное | `BiomeGenMagicalForest.treeKind` | ✅ |
| Трава биома | двойная трава 1 из 4 | `BiomeGenMagicalForest.doubleGrass` | ✅ |
| Цветы | 9 типов красных, вес 10 | `BiomeSurfaceRules.MAGICAL_FOREST_FLOWERS` | ✅ |
| Блобы мшистого булыжника | `nextInt(3)` | `mossyCobblestoneBlobs` | ✅ |
| Большой гриб | 1 из 40 на сетке 4×4 | `bigMushroom` | ✅ |
| Мана-бобы | 10 штук с y=64 | `manaPods`/`manaPodY` | ✅ |
| Вишрум | 8 попыток, спуск до y=50, рядом с бревном | `vishrooms`/`vishroomMinY`/`vishroomNeedsWood` | ✅ |
| Outer Lands | `WorldProviderOuter` без неба, `BiomeGenEldritch` с чёрным небом | `OuterLandsRules` + `eldritch_lands.json` | ✅ (дефекты D3-D5 аудита a5 закрыты) |

Расхождение, не чинил: в 1.7.10 у биома есть `topBlock` (верхний блок биома), которым
оригинал заполняет землю под холмовыми камнями и под столбом тотема; в 1.21.1 порт берёт
поверхностный блок среднего столбца (`WorldGenHilltopStones.groundBlock`).

---

## 8. Что сделано в коде

```
common/src/main/java/thaumcraft/common/lib/world/
  EldritchRingFeature.java        D5 (обсидиан), D6 (тёмный узел над алтарём)
  WorldGenHilltopStones.java      D6 (тёмный узел), D7 (список заполняемых блоков)
  StructurePlacementRules.java     D5 (RING_OBSIDIAN_CHANCE + ringFloor)
  VillageWorldGenRules.java        D1 (anchorOffset), D3/D4 (таблицы торгов), houseChunkOffsetX/Z
  ThaumcraftTotemFeature.java      G1 (тотем ауры, новый)
common/src/main/java/thaumcraft/common/config/ThaumcraftNodes.java   G1 (регистрация тотема)
common/src/main/resources/data/thaumcraft/worldgen/{placed,configured}_feature/aura_totem.json  G1
fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabric.java          G1 (подключение)
neoforge/src/main/resources/data/thaumcraft/neoforge/biome_modifier/aura_totem.json  G1
common/src/test/java/thaumcraft/world/VillageWorldGenRulesTest.java  D1, D3, D4, F1 (+3 кейса)
common/src/test/java/thaumcraft/world/StructurePlacementRulesTest.java D5 (+1 кейс)
common/src/gameTest/java/thaumcraft/common/testing/VillageComponentPlacementRuntimeChecks.java  D2 (новый)
common/src/gameTest/java/thaumcraft/common/testing/SurfaceStructurePlacementRuntimeChecks.java  D2, D6, F1
fabric/src/gameTest/java/thaumcraft/fabric/testing/ThaumcraftGameTests.java  регистрация нового теста
```

Проверки:

* `./gradlew :common:test` — BUILD SUCCESSFUL, 1678 тестов, 0 падений (реальный вывод в §9);
* `./gradlew -PgameTestFilter=surfacestructuresreachtheoverworld,villagecomponentsreachtheoverworld
  :fabric:runGameTest` на удалённом мире — 2 теста PASSED, BUILD SUCCESSFUL (§9).

---

## 9. Реальный вывод проверок

```
$ export JAVA_HOME=/home/Acak/.jdk21; export PATH=$JAVA_HOME/bin:$PATH
$ export GRADLE_USER_HOME=/home/Acak/gradle-homes/ocworldgen
$ ./gradlew --no-daemon --console=plain :common:test
> Task :common:test
BUILD SUCCESSFUL in 20s
5 actionable tasks: 2 executed, 3 up-to-date
(результаты: tests 1678, failures 0, errors 0, skipped 0)

$ rm -rf fabric/run-gametest/world
$ ./gradlew --no-daemon --console=plain \
    -PgameTestFilter=surfacestructuresreachtheoverworld,villagecomponentsreachtheoverworld \
    :fabric:runGameTest
[12:5x:xx] [Server thread/INFO] (Minecraft) All 2 required tests passed :)
BUILD SUCCESSFUL in 45s
gametest-focused-results.xml:
  thaumcraftgametests.surfacestructuresreachtheoverworld  4.628 PASSED
  thaumcraftgametests.villagecomponentsreachtheoverworld 1.807 PASSED
```

Клиентскую приёмку не проводил: изменения затрагивают только серверную генерацию, визуальная
проверка остаётся за клиентским прогоном.

---

## 10. Пробелы (не перенесено / не проверено)

1. **`WorldGenMound` (1/150 ветки цепочки) отсутствует.** Оригинал — 2550 строк
   (`WorldGenMound`), порта нет; ветка цепочки бросает 1/150 и ничего не строит. Как и в аудите a5.
2. **Торги не привязаны к профессии жителя.** Таблицы живут как данные
   (`VillageBankerManager.trades()`, `VillageWizardManager.trades()`), в 1.21.1 нет
   `VillagerRegistry`; житель деревни в порту появляется «пустым».
3. **Два предмета в таблице торгов не зарегистрированы:** `thaumcraft:chicken_nugget` и
   `thaumcraft:beef_nugget` (`ConfigItems.itemNuggetChicken/itemNuggetBeef`) — их нет в
   `ThaumcraftContent.MATERIALS`. Это долг контент-юнита (a6/a10), не worldgen.
4. **`biome.topBlock`** оригинала заменён поверхностным блоком среднего столбца.
5. **Блок узла:** оригинал вешает узел в невидимом `blockAiry` (metadata 8 cosmetic solid),
   порт использует видимый нативный `thaumcraft:aura_node` — решение юнита узлов.
6. **Внешние земли (лабиринт и комнаты)** сверены на согласованность правил и покрытие тестами;
   построчная повторная сверка всех 1433 строк оракула в этом прогоне не проводилась.
7. **Клиент не проверялся.**
