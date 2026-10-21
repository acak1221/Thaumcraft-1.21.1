# Закрытие пробелов домена «генерация мира» TC4 4.2.3.5 → 1.21.1

**Дата:** 21.10.2026 · **Ветка:** `port/worldgengap` · **Оракул:** `/home/Acak/work/thaumcraft-4.2.3.5-oracle`
**Справочник имён ваниллы:** OpenTC4 (`/home/Acak/work/opentc4`, читаемый checkout 1.7.10) и
`scripts/porting/resolve_srg.py` для SRG-имён.

Область: объявленные пробелы домена «генерация мира» из
[openTC4-worldgen](2026-10-21-openTC4-worldgen.md) §10 плюс единственная единица ledger со
статусом `needs_review` (`thaumcraft/common/lib/world/WorldGenMound.java`).

---

## 1. Итог

| # | Пробел | Оракул | Что сделано | Статус |
|---|---|---|---|---|
| 1 | `WorldGenMound` (1 чанк из 150) без генератора | `WorldGenMound:29-2550` | Порт: слой-шаблон 19×19×16 (2441 блок, проверен по оракулу) + фича с лутовой комнатой, сундуком, двумя спавнерами и тёмным узлом | **закрыт** |
| 2 | Торги не привязаны к профессии жителя | `VillageBankerManager:41-56`, `VillageWizardManager:35-53`, `ComponentBankerHome:112,116-118` | Порт: профессии `thaumcraft:banker` / `thaumcraft:wizard`, миксин `Villager#updateTrades`, профессия ставятся спавну | **закрыт** |
| 3 | `itemNuggetChicken` / `itemNuggetBeef` в таблице торгов | `VillageWizardManager:43,45` | Порт: id таблиц исправлены на зарегистрированные `thaumcraft:nugget_chicken` / `nugget_beef`; заодно исправлены id аспектов-осколков | **закрыт** |
| 4 | `biome.topBlock` заменён «поверхностным блоком среднего столбца» | `BiomeGenBase:135-136`, `WorldGenHilltopStones:67,81-82` | Порт: таблица `BiomeTopBlocks`, холмовые камни берут верхний блок биома | **закрыт** |
| 5 | Невидимый блок узла 1.7 (`blockAiry`) | `BlockAiry:60-140`, `ThaumcraftWorldGenerator:389-397` | Порт: узел живёт в невидимом `thaumcraft:aura_node` (проверено: `RenderShape.INVISIBLE`, пустая модель, свет 8, твёрдость 2/200, бокс 0.3..0.7, хост `TileNode`) | **закрыт проверкой** |
| 6 | `place()` деревенских кусков «не вызывается» | `VillageBankerManager:58-60`, `VillageWizardManager:55-57` | Опровергнуто: игровым тестом дом и башня строятся в сгенерированном чанке | **опровергнуто** |
| 7 | Тотем ауры (1 чанк из 360) | `ThaumcraftWorldGenerator:148-187` | Порт: блок-хост узла (косметический сплошной, метаданные 8) и правило измерения; узел тотема теперь реально создаётся | **закрыт с документированным отклонением** |

Дополнительно найдено и исправлено в этом прогоне:

| Дефект | Оракул | Было в порте | Стало |
|---|---|---|---|
| Цена книги у банкира | `VillageBankerManager:47` — `7 + nextInt(3)` = 7..9 | 7..8 (предыдущий аудит ошибся в чтении оракула) | 7..9 |
| Осколки аспектов в таблице волшебника | `VillageWizardManager:46` — `itemShard` 0..5 | `thaumcraft:aer_shard` и т. п. (таких id нет) | `thaumcraft:air_shard`, `fire_shard`, `water_shard`, `earth_shard`, `order_shard`, `entropy_shard` |
| Список цветов под холмовыми камнями | `WorldGenHilltopStones:81` — только `red_flower` и `yellow_flower` (десять цветов 1.7.10) | весь тег `minecraft:flowers` (включая высокие цветы и листья) | десять цветов оригинала |
| Хост узла в косметическом сплошном | `BlockCosmeticSolid:288-296` — метаданные 8 держат `TileNode` | `TileNode` отвергал этот хост (`current()`) | хост принят, узел тикает, `NodeHarvest` его собирает |

Единица ledger `thaumcraft/common/lib/world/WorldGenMound.java` переведена из
`needs_review` в `port` / `parity_verified`; **единиц со статусом `needs_review` в ledger
больше нет** (`dispositions`: `port` 288 → 289, `needs_review` 1 → 0).

---

## 2. WorldGenMound (пробел 1) — единственная единица `needs_review`

### 2.1 Что делает оригинал

`thaumcraft/common/lib/world/WorldGenMound.java:29-2550` — это `WorldGenerator` 1.7.10,
который пишет **фиксированную** фигуру:

* `WorldGenMound:35-58` — `LocationIsValidSpawn`: над точкой должен быть воздух, под ним —
  камень, трава или земля (снег/трава-международка поверх одного из них тоже годятся);
* `WorldGenMound:63-1562` и `:1569-2519` — 2441 явных вызовов `setBlock`: оболочка из
  земли/травы/булыжника/мшистого булыжника, комнаты, вырезанные воздухом, лестница,
  окна из iron bars, тесаные каменные кирпичи, ступени из `stone_stairs` (метаданные 0..7);
* `WorldGenMound:2520-2524` — два контейнера лута (сундук/ваза, редкость 0..2 из одного
  `nextFloat`, вид из случайного числа **мира**, не генератора);
* `WorldGenMound:2525-2536` — сундук (в одной трети — ловушка с TNT под ним) и наполнение
  его dungeon-лутом, второй раз — с вероятностью 1/5;
* `WorldGenMound:2538-2544` — два спавнера (Skeleton и Zombie);
* `ThaumcraftWorldGenerator:428-434` — ветка цепочки: `nextInt(150) == 0` → mound, затем
  `random.nextInt(200) + 400` (значение не используется) → тёмный узел над центром.

### 2.2 Порт

* `common/src/main/java/thaumcraft/common/lib/world/WorldGenMoundTemplate.java` — таблица
  формы: 16 слоёв × 19 строк × 19 ячеек, по одному символу на блок; `stateFor(char)` переводит
  символ в состояние 1.21.1 (метаданные 1.7.10 → facing/half у ступеней, facing у лестницы,
  тесаные каменные кирпичи у метаданных 3);
* `common/src/main/java/thaumcraft/common/lib/world/WorldGenMound.java` — фича: бросок цепочки
  из случайного числа чанка оригинала, пять проверок грунта, таблица, лутовая комната,
  сундук, спавнеры, тёмный узел;
* регистрация: `ThaumcraftVillages.MOUND` (`thaumcraft:tc4_mound`), `placed_feature/tc4_mound.json`,
  `configured_feature/tc4_mound.json`, `BiomeModifications` в Fabric и
  `neoforge/biome_modifier/tc4_mound.json` в NeoForge.

### 2.3 Доказательство равенства таблицы

Таблица **не взята из 1.12-справочника**, а восстановлена из оракула и потом сверена с ним
ячейка за ячейкой:

```
$ python3 scripts/porting/extract_mound_template.py
oracle  : /home/Acak/work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/lib/world/WorldGenMound.java
layers  : 16
blocks  : 2441
sha256  : 93a2b6f8b8b1722733d5a005d02e25b92d0e2ba28d612b7f69409c5463a65433
```

* скрипт `scripts/porting/extract_mound_template.py` разбирает `setBlock`/`setBlockWithMetadata`
  оракула (включая `(Block)`-касты и `j - 1`), собирает 16 слоёв и печатает sha256 — проверку
  можно воспроизвести в любой момент;
* `common/src/test/java/thaumcraft/world/WorldGenMoundTemplateTest.java` сравнивает sha256
  нативной таблицы с этим значением и проверяет 2441 блок, форму и отдельные ячейки;
* **1.12-справочник** (`src/main/java/thaumcraft/common/lib/world/WorldGenMoundTemplate.java`)
  дал ту же самую таблицу: при сравнении с оракулом расхождений 0 (16 слоёв, 2441 ячейка).
  Это перекрестная проверка, а не основание порта. Сравнение воспроизводится так: вывод
  `extract_mound_template.py --flat` даёт ровно те же шестнадцать строк, что и `LAYERS`
  1.12-справочника (проверено: `identical: True`).

### 2.4 Вынужденные отклонения 1.21.1

* 1.7.10 `WorldGenerator` → фича 1.21.1: цепочка бросается из случайного числа чанка
  оригинала (`StructurePlacementRules.chunkSeed`), поэтому mound/кольцо/холмовые камни
  взаимно исключают друг друга, как в оригинале;
* фигура шириной 19 блоков выходит за пределы чанка. 1.21.1 разрешает писать в 8 соседних
  чанков на шаге FEATURES (`ChunkPyramid.GENERATION_PYRAMID`: `blockStateWriteRadius(1)`), и
  порт проверяет `hasChunk`/`ensureCanWrite` перед каждой записью;
* сундук лутовой комнаты наполняется нативной таблицей `minecraft:chest`/`simple_dungeon`
  вместо хука chests 1.7.10 — так же, как уже сделано для холмовых камней оригинала.

---

## 3. Торги деревень (пробелы 2 и 3)

### 3.1 Оракул

* `VillageBankerManager:41-56` и `VillageWizardManager:35-53` — Forge-обработчики
  `VillagerRegistry.IVillageTradeHandler`: они **добавляют** свои рецепты в список торгов
  жителя своей профессии;
* `ComponentBankerHome:112,116-118` — кусок деревни спавнит жителя и задаёт ему профессию
  куска (`getProfession()` → `ConfigEntities.entBankerId` = 191, `entWizardId` = 190);
* `ComponentWizardTower` — жителя не ставит вовсе (x = 7 вне коробки куска, `Oracle
  ComponentWizardTower:95`), это поведение порт сохранил.

### 3.2 Порт

* `common/src/main/java/thaumcraft/common/config/ThaumcraftVillageProfessions.java` —
  профессии `thaumcraft:banker` и `thaumcraft:wizard` в реестре профессий 1.21.1. У них нет
  рабочего места (`heldJobSite`/`acquirableJobSite` всегда ложны), поэтому профессию нельзя
  получить и потерять — оригинал тоже ставил жителя с профессией напрямую;
* `common/src/main/java/thaumcraft/common/lib/world/VillageTradeRules.java` — превращает
  таблицу `VillageWorldGenRules` в `MerchantOffer` 1.21.1, сохраняя порядок бросков
  оригинала (сначала количество платы, потом количество награды, потом вариант награды);
* `common/src/main/java/thaumcraft/common/mixin/VillagerVillageTradesMixin.java` — подмешивание
  в `Villager#updateTrades` (единственная точка 1.21.1, где строится список торгов);
  список наполняется один раз, при росте «карьеры» 1.21.1 вызывает `updateTrades` снова;
* `VillageBankerHome.spawnVillager` — ставит профессию `thaumcraft:banker` спавнутому жителю.

### 3.3 Исправленные предметы и цены

| Что | Оракул | Порт |
|---|---|---|
| Осколок курицы | `VillageWizardManager:43` — `itemNuggetChicken`, 24..31 | `thaumcraft:nugget_chicken` (зарегистрирован в `ThaumcraftLegacyItems:168`) |
| Осколок говядины | `VillageWizardManager:45` — `itemNuggetBeef`, 24..31 | `thaumcraft:nugget_beef` |
| Книга банкира | `VillageBankerManager:47` — `7 + nextInt(3)` = 7..9 | 7..9 (было 7..8) |
| Осколки аспектов | `VillageWizardManager:46` — `itemShard` 0..5 | `thaumcraft:air_shard` … `entropy_shard` |

Имена id предметов, которые не зарегистрированы, были не только в нутях: таблица волшебника
ссылалась на `thaumcraft:aer_shard`/`ignis_shard`/…, а в реестре лежат `air_shard`/`fire_shard`/…
(по именам стихий, а не по именам аспектов). Игровой тест теперь проверяет **каждый** предмет
обеих таблиц по реестру — это и нашло обе ошибки.

---

## 4. `biome.topBlock` (пробел 4)

* Оракул: `BiomeGenBase:135-136` — у биома есть верхний блок; по умолчанию `Blocks.grass`,
  пустыня меняет его на песок, месса — на красный песок, грибной остров — на мицелий,
  каменный пляж — на камень, край — на землю, ад — на незерит. `BiomeGenEldritch:33` — на землю.
  `WorldGenHilltopStones:67,81-82` — этим блоком заполняется грунт под кластером;
  `WorldGenEldritchRing:68` — значение читается, но не используется (мёртвый код).
* Порт: `common/src/main/java/thaumcraft/common/lib/world/biomes/BiomeTopBlocks.java` — таблица
  верхнего блока для нативных биомов; `WorldGenHilltopStones.groundBlock` берёт его вместо
  «поверхностного блока среднего столбца».
* Заодно: список блоков, которые заменяет этот блок, приведён к оригиналу
  (`WorldGenHilltopStones:81`): снег, трава-международка, воздух и **десять** цветов 1.7.10
  (`red_flower` 0..8 и `yellow_flower` 0), а не весь тег `minecraft:flowers` 1.21.1.

---

## 5. Невидимый блок узла 1.7 (`blockAiry`) (пробел 5)

**Проверено, что порт уже несёт именно этот блок.** `BlockAiry` оригинала 1.7.10 —
`BlockContainer` с метаданными 0..12; метаданные 0 — невидимый хост узла:
`BlockAiry:115-140` (твёрдость 2, сопротивление 200), `:216-230` (свет 8),
`:246-252` (бокс выбора 0.3..0.7), `:288-296` (`createTileEntity` → `TileNode`),
`:277` (`isAir` = false), `:279-281` (нет твёрдых граней).

Нативный `thaumcraft:aura_node` (`common/src/main/java/thaumcraft/common/blocks/BlockAuraNode.java`)
— это тот же блок: `RenderShape.INVISIBLE`, пустая модель
(`models/block/aura_node.json` — `"elements": []`), без столкновений, свет 8,
`strength(2.0F, 200.0F)`, бокс `box(4.8, 4.8, 4.8, 11.2, 11.2, 11.2)` (это 0.3..0.7),
хост `TileNode`. Комментарии в классе прямо ссылаются на строки `BlockAiry` оригинала.

**Что действительно отличается и остаётся за юнитом узлов:**

* оригинал держит **один** блок с семейством метаданных (узел, нитор, листья, защитный
  камень, заряженный узел, газ, слизь, неразрушимый) — порт разносит это по нескольким
  блокам (`airy`, `airy_node`, `airy_hole`, `airy_unbreakable`, `aura_node`, `energized_node`),
  это задокументированное сглаживание API;
* `BlockAiry:277` — метаданные 2, 3, 10, 11 считаются воздухом; нативный `BlockAiry`
  (`common/src/main/java/thaumcraft/common/blocks/BlockAiry.java`) это не переносит
  (`EldritchBlockRules.airyIsAir` есть, но не вызывается). Это пробел юнита блоков, не
  генерации мира.

---

## 6. `place()` деревенских кусков (пробел 6)

Запись «`place()` деревенских кусков не вызывается при генерации чанка» **опровергнута**
ещё в волне `port/ocworldgen` и подтверждена здесь: игровой тест
`villageComponentsReachTheOverworld` генерирует чанки, которые план деревни отдал дому
банкира и башне волшебника, и находит дубовую дверь компонента.

Настоящая причина «деревни не видно» — плотность: 1 дом банкира на ≈560 чанков, 1 башня на
≈820 (замер предыдущей волны). Это нормальная редкость TC4, а не поломка.

**Новое наблюдение этого прогона (важно для отчётности):** жители домов **доходят** до мира —
спавн во время генерации кладёт их NBT в proto-чанк (`ProtoChunk.addEntity`), при переводе
чанка в уровень они читаются обратно и попадают в менеджер сущностей; игровой тест находит
их с запросом по миру и проверяет их профессию (`thaumcraft:banker`) и список торгов. Запрос
по одному только чанку сразу после `getChunk` их ещё не видит — это деталь порядка
перевода proto-чанка в 1.21.1, а не потеря жителей. Проверка читает торговцев
загруженного мира.

---

## 7. Тотем ауры (пробел 7)

### 7.1 Оракул

`ThaumcraftWorldGenerator:148-187`: 1 чанк из 360 (`Config.nodeRarity * 10`), только в
измерении 0 (оверворлд), только если чанк не получил аура-узел; земля под тотемом — трава,
песок, земля или камень; колонна из косметического сплошного (метаданные 0) высотой 4;
**на позиции узла ставится косметический сплошной метаданных 8** и только потом вызывается
`createRandomNodeAt`.

### 7.2 Найденные расхождения

1. **Блок-хост узла не ставился.** Порт вызывал `createRandomNodeAt` прямо на блоке
   косметического сплошного метаданных 0 → узел не создавался вовсе; а во второй ветке
   («узел над колонной») позиция была воздухом, и порт **создавал** узел там, где у оригинала
   его нет. Исправлено: сначала ставится метаданные 8, затем вызывается создание узла —
   порядок оригинала.
2. **Правило измерения.** Порт требовал ключ `minecraft:overworld`. Оракул требует id 0;
   мод генерации мира 1.7.10 менял оверворлд, не меняя id измерения, поэтому нативная форма
   принимает любое измерение **типа** оверворлда (`ThaumcraftTotemFeature.isOverworld`).
3. **Узел тотема в оригинале 4.2.3.5 не появляется.** `BlockCosmeticSolid` оригинала
   наследует `Block`, а не `BlockContainer` (проверено `javap` по самому jar
   `Thaumcraft-1.7.10-4.2.3.5.jar`: `public class thaumcraft.common.blocks.BlockCosmeticSolid
   extends net.minecraft.block.Block`), а `Chunk.getBlockTileEntityInChunk` (ванильная 1.7.10)
   создаёт плитку только у блоков с `hasTileEntity() == true`. Значит `createTileEntity`
   косметического сплошного — мёртвый код, и узел тотема оригинала не создаётся никогда.
   Порт следует за 1.12-справочником, где этот дефект исправлен (`hasTileEntity` переопределён),
   поэтому **тотем порта несёт живой тёмный узел** — это документированное отклонение от
   4.2.3.5 в пользу более поздней версии самого TC4.
   Чтобы узел в косметическом сплошном работал, `TileNode.isNodeHost` принял этот хост,
   `BlockCosmeticSolid` получил тик узла, а `NodeHarvest` — сбор этого узла.

### 7.3 Проверка

Игровой тест `surfacestructuresreachtheoverworld` находит в сгенерированном чанке колонну
косметического сплошного и тёмный узел над ней.

---

## 8. Что сделано в коде

```
common/src/main/java/thaumcraft/common/lib/world/WorldGenMound.java                 новая: фича мound
common/src/main/java/thaumcraft/common/lib/world/WorldGenMoundTemplate.java          новая: слой-шаблон мound
common/src/main/java/thaumcraft/common/lib/world/VillageTradeRules.java             новая: таблица торгов → MerchantOffer
common/src/main/java/thaumcraft/common/lib/world/VillageWorldGenRules.java          цена книги 7..9, id нутов и осколков
common/src/main/java/thaumcraft/common/lib/world/ThaumcraftTotemFeature.java         хост узла (метаданные 8), правило измерения
common/src/main/java/thaumcraft/common/lib/world/WorldGenHilltopStones.java          верхний блок биома, список цветов оригинала
common/src/main/java/thaumcraft/common/lib/world/biomes/BiomeTopBlocks.java          новая: таблица верхнего блока биома
common/src/main/java/thaumcraft/common/lib/world/VillageBankerHome.java              профессия спавнутого банкира
common/src/main/java/thaumcraft/common/config/ThaumcraftVillageProfessions.java      новая: профессии banker и wizard
common/src/main/java/thaumcraft/common/mixin/VillagerVillageTradesMixin.java         новая: торговля по профессии
common/src/main/java/thaumcraft/common/tiles/TileNode.java                            хост узла (isNodeHost)
common/src/main/java/thaumcraft/common/blocks/BlockCosmeticSolid.java                тик узла в метаданных 8
common/src/main/java/thaumcraft/common/lib/world/NodeHarvest.java                    сбор узла в косметическом сплошном
common/src/main/java/thaumcraft/common/Thaumcraft.java                               регистрация профессий
common/src/main/java/thaumcraft/common/config/ThaumcraftVillages.java                регистрация фичи tc4_mound
common/src/main/resources/data/thaumcraft/worldgen/{placed,configured}_feature/tc4_mound.json  новая
common/src/main/resources/thaumcraft-common.mixins.json                              миксин торговли
common/src/main/resources/assets/thaumcraft/lang/{en_us,ru_ru}.json                   имена профессий
fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabric.java                         biome modification для tc4_mound
neoforge/src/main/resources/data/thaumcraft/neoforge/biome_modifier/tc4_mound.json   biome modifier для tc4_mound
common/src/test/java/thaumcraft/world/WorldGenMoundTemplateTest.java                 новая: sha256 таблицы против оракула
common/src/test/java/thaumcraft/world/VillageWorldGenRulesTest.java                   цена книги, id нутов и осколков
common/src/gameTest/java/thaumcraft/common/testing/SurfaceStructurePlacementRuntimeChecks.java  mound и totem
common/src/gameTest/java/thaumcraft/common/testing/VillageTradeRuntimeChecks.java     новая: профессии и таблицы торгов
fabric/src/gameTest/java/thaumcraft/fabric/testing/ThaumcraftGameTests.java          регистрация нового теста
scripts/porting/extract_mound_template.py                                           новая: сборка таблицы из оракула
docs/porting/scope-ledger.json                                                       вердикт единицы WorldGenMound
```

---

## 9. Реальный вывод проверок

### 9.1 Модульные тесты

```
$ export JAVA_HOME=/home/Acak/.jdk21; export PATH=$JAVA_HOME/bin:$PATH
$ export GRADLE_USER_HOME=/home/Acak/gradle-homes/worldgengap
$ rm -rf common/build/test-results/test
$ ./gradlew --no-daemon --console=plain --no-build-cache :common:test
> Task :common:test
BUILD SUCCESSFUL in 16s
5 actionable tasks: 2 executed, 3 up-to-date
```

Результат JUnit: **tests 1779, failures 0, errors 0, skipped 0**.

### 9.2 Игровые тесты (три сценария домена)

```
$ rm -rf fabric/run-gametest/world
$ ./gradlew --no-daemon --console=plain \
    -PgameTestFilter=surfacestructuresreachtheoverworld,villagecomponentsreachtheoverworld,villagetradesreachthevillagers \
    :fabric:runGameTest
[18:17:33] [Server thread/INFO] (Minecraft) All 3 required tests passed :)
BUILD SUCCESSFUL in 51s
```

`fabric/build/gametest-focused-results.xml`:

```
thaumcraftgametests.surfacestructuresreachtheoverworld  5.786 PASSED
thaumcraftgametests.villagecomponentsreachtheoverworld  1.573 PASSED
thaumcraftgametests.villagetradesreachthevillagers       1.674 PASSED
```

### 9.3 Полный игровой набор Fabric

```
$ rm -rf fabric/run-gametest/world
$ ./gradlew --no-daemon --console=plain :fabric:runGameTest
```

313 сценариев. Первый прогон дал 6 падений, второй — 4. Оставшиеся четыре — это ровно тот
нестабильный набор, который был красным и до этой волны
(`docs/MIGRATION_STATUS.md`: «4-5 падений, только нестабильные сценарии, красные и до волны:
`constructandcentrifugerecipes`, `bookshelfwandcreatesonefloatingbook`, `golemfetterholdsgolems`,
`golemaccessories` и иногда `essentiareservoir`»):

```
thaumcraftgametests.constructandcentrifugerecipesandplainnativeblocks
thaumcraftgametests.bookshelfwandcreatesonefloatingbook
thaumcraftgametests.golemfetterholdsgolemswhileitsredstonesignallasts
thaumcraftgametests.golemaccessoriesinstallchangestatsandsurvivethebell
```

В первом прогоне дополнительно красными были два сценария, зависящих от времени
(`mixedcrystalorientationsurvivesactualchunkunload` — «не успел за 540 тиков»,
`swordofthezephyrliftsblowsandsweeps`); по отдельности они проходят:

```
$ ./gradlew --no-daemon --console=plain \
    -PgameTestFilter=mixedcrystalorientationsurvivesactualchunkunload,swordofthezephyrliftsblowsandsweeps \
    :fabric:runGameTest
[18:22:00] [Server thread/INFO] (Minecraft) All 2 required tests passed :)
BUILD SUCCESSFUL in 29s
```

Клиентскую визуальную приёмку не проводил: изменения затрагивают только серверную генерацию,
визуальная проверка остаётся за клиентским прогоном.

---

## 10. Что осталось

* **Клиент не проверялся** (приёмка визуала узла тотема, косметического сплошного метаданных 8
  и деревеньских домов).
* **Метаданные `blockAiry`:** порт не переносит `isAir` для метаданных 2, 3, 10, 11 и не
  держит одно семейство блоков вместо одного блока с метаданными. Это юнит блоков, не
  генерации мира.
* **Форма `topBlock` для мутированных биомов 1.7.10** (hills/taiga/savanna) сведена к
  нативным биомам 1.21.1 приблизительно; точные метаданные «снежной земли» в 1.21.1 нет.
* **Правило «цветы в жарком песке»** оригинала (`ThaumcraftWorldGenerator:476`, проверка
  `topBlock == sand && temperature > 1.0`) в порту не реализовано: в порту цветы TC4
  привязаны к деревьям (`WorldGenCustomFlowers`). Это домен растительности, не мой пробел.
* **Сущности-спавнеры мound** используют нативные типы (`minecraft:skeleton`,
  `minecraft:zombie`) — оригинальные строки совпадают.
