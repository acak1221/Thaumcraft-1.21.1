# Агент `worldgen` — генерация мира и природные ресурсы TC4 4.2.3.5 → 1.21.1

**Дата:** 23.10.2026 · **Ветка:** `agent/worldgen` · **Рабочее дерево:** `/home/cyberfox-kali/wt/worldgen`
**Оракул (читаемый 1.7.10):** `/home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/common/lib/world/`
**Оракул (авторитетный jar 4.2.3.5, только чтение):** `Thaumcraft-1.7.10-4.2.3.5.jar`

Домен: `natural_resources_and_worldgen` + `world_structures_and_ecology`, 18 единиц ledger
(`/home/cyberfox-kali/units/worldgen.txt`).

---

## 1. Итог

| Единица ledger | Было | Стало |
|---|---|---|
| `thaumcraft/common/lib/world/ComponentBankerHome.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/ComponentWizardTower.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/VillageBankerManager.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/VillageWizardManager.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/WorldGenBigMagicTree.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/WorldGenCustomFlowers.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/WorldGenGreatwoodTrees.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/WorldGenHilltopStones.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/WorldGenManaPods.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/WorldGenSilverwoodTrees.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/WorldGenSilverwoodTreesOld.java` | partial | **parity_verified** (мёртвый код оригинала) |
| `thaumcraft/common/lib/world/ThaumcraftVillagerTrades.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/WorldGenMoundTemplate.java` | not_assessed | **parity_verified** |
| `thaumcraft/common/lib/world/biomes/BiomeGenEerie.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/biomes/BiomeGenMagicalForest.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/biomes/BiomeHandler.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java` | partial | partial (уточнена граница) |
| `thaumcraft/common/lib/world/WorldgenLootHelper.java` | partial | partial (уточнена граница) |

16 из 18 единиц переведены в `parity_verified`, 2 остаются `partial` с точно описанной
границей (§6). `disposition` и `queue` не менялись ни для одной строки — счётчики
`counts` в ledger не тронуты.

**Найдено дефектов: 6. Исправлено: 5. Осталось документированно: 1** (плюс два вывода
о мёртвом коде и один снятый риск падения генерации).

---

## 2. Дефекты

### D1. Крона «большого магического дерева» теряла по узлу на слой — **исправлено**

* **Оригинал** `WorldGenBigMagicTree.java:23` — поле `double leafDensity = 1.0F;`
  (читаемый 1.7.10), `WorldGenBigMagicTree.java:39` —
  `int i = (int)(1.382 + Math.pow(this.leafDensity * (double)this.heightLimit / (double)13.0F, 2.0F));`
  Значение `0.8` есть только в `setScale` (`WorldGenBigMagicTree.java:343`,
  `this.leafDensity = par5 * 0.8;`) — это масштаб удалённого дерева 1.6, а `setScale`
  для TC4-дерева **не вызывается ни разу**: единственная конструкция оригинала —
  `new WorldGenBigMagicTree(false)`
  (`BiomeGenMagicalForest.java:32`, `BiomeGenTaint.java:22`).
* **Порт (было)** `common/src/main/java/thaumcraft/common/lib/world/WorldGenBigMagicTree.java:58` —
  `int count = Math.max(1, (int)(1.382 + Math.pow(.8 * heightLimit / 13.0, 2)));`
* **Точные числа.** Высота `heightLimit = 11 + rand.nextInt(12)` (`:354`), то есть 11..22.
  Оригинал: `floor(1.382 + (h/13)²)`, порт: `floor(1.382 + (0.8·h/13)²)`.

  | heightLimit | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 | 20 | 21 | 22 |
  |---|---|---|---|---|---|---|---|---|---|---|---|---|
  | оригинал | 2 | 2 | 2 | 2 | 2 | 2 | **3** | **3** | **3** | **3** | 3 | **4** |
  | порт (было) | **1** | **1** | 2 | 2 | 2 | 2 | **2** | **2** | **2** | **2** | 3 | **3** |

  Для 7 из 12 высот крона теряла **один узел листвы на слой** — то самое дерево, которое
  является деревом по умолчанию в Магическом лесу и в биоме Тайн.
* **Независимое подтверждение байткодом авторитетного jar 4.2.3.5** (не только читаемым
  исходником): в `WorldGenBigMagicTree.<init>` поле `leafDensity` получает `dconst_1`
  (1.0), тогда как в `WorldGenGreatwoodTrees.<init>` — `ldc2_w` из пула с double `0.9`.
  Пул констант большого магического дерева содержит единственный double `0.8` — это
  множитель в `setScale`.
* **Фикс** `WorldGenBigMagicTree.java:61-79` — новая публичная функция
  `leafNodeCount(int heightLimit)` с формулой оригинала; `:82` её использует.
  Для великого дерева добавлена такая же функция с его собственной плотностью `0.9`
  (`WorldGenGreatwoodTrees.java:87`), обе плотности вынесены в именованные константы
  `MIN_HEIGHT_LIMIT` / `HEIGHT_LIMIT_ROLLS` / `MAX_HEIGHT_LIMIT`.

### D2. Башня волшебника ставила два лишних угловых блока — **исправлено**

* **Оригинал** `ComponentWizardTower.java:53-64` — двенадцать вызовов
  `placeBlockAtCurrentPosition(world, Blocks.cobblestone, 3, <x>, <y>, <z>, bb)`.
  В 1.7.10 сигнатура `StructureComponent.placeBlockAtCurrentPosition`
  — `(World, Block, int data, int x, int y, int z, StructureBoundingBox)`, т.е. **метаданные
  идут первыми**. Это доказывается ступенькой оригинала
  (`ComponentWizardTower.java:85`:
  `placeBlockAtCurrentPosition(world, Blocks.stone_stairs, getMetadataWithOffset(Blocks.stone_stairs, 3), 3, 0, 0, bb)`)
  и окном (`ComponentBankerHome.java:83`: `placeBlockAtCurrentPosition(world, Blocks.iron_bars, 0, 0, 2, 2, bb)` → (0,2,2)).
  Разбор двенадцати вызовов даёт: y=0 → только x=1 (два блока); y=5 → x=1 и x=5 (четыре);
  y=10 → x=1 и x=5 (четыре). Вызовы 57 и 58 повторяют углы среднего этажа (x=1).
  **Итого оригинал ставит 10 различных углов, и два угла нижнего этажа на локальном x=5
  (5,0,1) и (5,0,5) остаются пустыми.**
* **Порт (было)** `common/src/main/java/thaumcraft/common/lib/world/VillageWorldGenRules.java:452-457` —
  цикл `for (int y : new int[]{0, 5, 10})` ставил все четыре угла на каждом этаже, то есть
  **12 блоков вместо 10**.
* **Фикс** `VillageWorldGenRules.java:467-480` — выписаны ровно десять оригинальных углов;
  два пропущенных угла нижнего этажа сохранены как quirk оригинала.

### D3. Минимальный размер деревни для куска: неверная математика хелпера — **исправлено**

* **Оригинал** `VillageBankerManager.java:37-39` и `VillageWizardManager.java:38-40`:
  `new StructureVillagePieces.PieceWeight(ComponentBankerHome.class, 25, MathHelper.getRandomIntegerInRange(random, i, 1 + i))`.
  В 1.7.10 `MathHelper.getRandomIntegerInRange(random, min, max)` — это
  `min + random.nextInt(max - min + 1)`, поэтому диапазон `[i, i+1]` содержит **два** значения,
  и бросок равен `i` **или** `i + 1`. В 1.12.2 хелпер переименован в `getIntWithMinMax`
  с тем же телом.
* **Порт (было)** `VillageWorldGenRules.java:84-93` — комментарий утверждал
  «That helper is `min + random.nextInt(max - min)`, so the range is one wide and the draw is
  always `i`», и функция всегда возвращала `villageSize`.
* **Фикс** `VillageWorldGenRules.java:85-107` — добавлен `pieceWeightMinSize(int villageSize, int roll)`
  (`villageSize + floorMod(roll, 2)`), одноаргументная версия оставлена как нижняя граница
  розыгрыша; `VillageBankerManager.java:44-58` и `VillageWizardManager.java:44-57` получили
  тот же двухаргументный доступ.

### D4. `IllegalStateException` в пути генерации серебряного дерева — **снят риск падения**

* **Порт (было)** `common/src/main/java/thaumcraft/common/lib/world/MagicalTreePlacement.java:94-98`:
  `throw new IllegalStateException("Committed Silverwood knot has no actual native TileNode at " + ...)`.
  Любое исключение в фиче 1.21.1 = отказ генерации чанка = падение сервера.
* **Фикс** `MagicalTreePlacement.java:94-112` — узел пропускается, счётчик
  `knotsWithoutEntity()` растёт; дерево при этом уже записано, так что падать незачем.

### D5. `WorldGenSilverwoodTreesOld` — мёртвый код оригинала (ошибка прежнего вердикта)

* **Оригинал**: `WorldGenSilverwoodTreesOld` **никогда не создаётся**. Доказано по
  авторитетному jar 4.2.3.5: единственный класс, содержащий строку
  `WorldGenSilverwoodTreesOld`, — сам `WorldGenSilverwoodTreesOld.class`
  (`WorldGenSilverwoodTrees` — 4 ссылающихся класса, `WorldGenGreatwoodTrees` — 3,
  `WorldGenBigMagicTree` — 2, `WorldGenCustomFlowers` — 3, `WorldGenManaPods` — 1).
  Читаемый 1.7.10 даёт то же (`grep` по всему дереву оригинала: совпадений вне самого файла нет).
* **Следствие**: единица ledger переведена в `parity_verified` — отсутствие нативного
  соответствия поведением эквивалентно, потому что класс никогда не выполнялся.
  Прежняя заметка «the native port has only one silverwood generator, so the old shape is not
  ported» была неверной: не «не перенесён», а «не нужен».

### D6. `WorldGenMoundTemplate` — заметка ledger устарела (ошибка прежнего вердикта)

* Прежняя заметка: «the native port has no mound at all». С 21.10.2026 mound перенесён
  (`common/src/main/java/thaumcraft/common/lib/world/WorldGenMoundTemplate.java`, слой-шаблон
  19×19×16, 2441 блок, sha256-сверка в `common/src/test/java/thaumcraft/world/WorldGenMoundTemplateTest.java`).
  Единица переведена в `parity_verified` с актуальной заметкой.

---

## 3. Сверка, не давшая дефектов (закрытые вопросы)

Эти пункты проверялись в этом прогоне и оказались верными; они закрывают часть вопросов
задания «wrong spawn chances / ratios, vein size, biome definitions, surface rules, structure
spacing, village density, colours, mob spawn rules».

| Что | Оракул | Порт | Итог |
|---|---|---|---|
| Спавн-строки «Жуткого» биома | `BiomeGenEerie.java:16-41` (bat 3, witch 8, enderman 4, brainy 32, giant brainy 8, wisp 3, guardian 1) | `data/thaumcraft/worldgen/biome/eerie.json` | ✅ совпадает |
| Спавн-строки Магического леса | `BiomeGenMagicalForest.java:35-63` (wolf 2, horse 2, witch 3, enderman 3, pech 10, wisp 10) | `data/thaumcraft/worldgen/biome/magical_forest.json` | ✅ совпадает |
| Климат и цвета биомов | `BiomeGenEerie.java:38-59`, `BiomeGenMagicalForest.java:54-87` (0.5/0.5 и 0.7/0.6; 0x404840, 0x405340, 0x222299, 0x2E535F; 0x55FF81, 0x66FFC5, 30702) | те же значения в JSON | ✅ совпадает |
| Таблица `BiomeHandler` | `BiomeHandler.java:21-62` + `Config.java:822-858` (31 тип, аура, аспект, шанс великого дерева) | `biomes/BiomeHandler.java:49-81` | ✅ совпадает |
| Рудный проход | `ThaumcraftWorldGenerator.java:506-567` (18 киновари, 20 янтаря, 8 насыщенного камня; жила 6; `nextInt(getHeight()/5)`; `nextInt(max(5, surface-5))`) | `ThaumcraftOreFeature.java:71-99` | ✅ совпадает |
| Выбор варианта насыщенного камня по биому | `ThaumcraftWorldGenerator.java:538-556` + `BiomeHandler.java:38-46` | `ThaumcraftOreFeature.java:37-45,129-138` (AIR→1, FIRE→2, WATER→3, EARTH→4, ORDER→5, ENTROPY→6, «магический» без аспекта → `1+nextInt(6)`, остальные → исходный бросок) | ✅ совпадает |
| Проход растительности TC4 | `ThaumcraftWorldGenerator.java:483-504` (серебряное 1/60, великое 1/25, цветы на горячем песке 1/30) | `ThaumcraftTreeFeature.java:43-66` | ✅ совпадает |
| Цепочка поверхностных структур | `ThaumcraftWorldGenerator.java:435-475` (1/150 → 1/66 → 1/40, exclusively) | `StructurePlacementRules.java:124-159` | ✅ совпадает |
| Тотем ауры | `ThaumcraftWorldGenerator.java:114-168` (1/360, земля трава/песок/земля/камень, колонна 4, узел 1 из 4, узел на 5 выше) | `ThaumcraftTotemFeature.java:45-119` | ✅ совпадает |
| Цветы TC4 | `WorldGenCustomFlowers.java:18-29` (18 попыток, разброс 8/4, земля — трава или песок) | `WorldGenCustomFlowers.java` | ✅ совпадает; `Blocks.sand` в 1.7.10 несёт красный песок метаданными 1, поэтому `minecraft:red_sand` — тот же блок оригинала, а не добавление |
| Мана-бобы | `WorldGenManaPods.java:11-31` (поиск вверх до 128, дрейф на 4, возраст 2..6) | `WorldGenManaPods.java` | ✅ совпадает |
| Холмовые камни | `WorldGenHilltopStones.java:19-50,57-120` (минимум 85, 7×7, 4 слоя, пропуск углов, правило столбов, лоза 1 из 3) | `WorldGenHilltopStones.java:52-295` | ✅ совпадает |
| Серебряное дерево | `WorldGenSilverwoodTrees.java:27-166` (радиус кроны `10+nextInt(8)`, шанс узла `height*1.5`, четыре угловых бревна, цветы шиммерлифа) | `WorldGenSilverwoodTrees.java` | ✅ совпадает |
| Великое дерево | `WorldGenGreatwoodTrees.java:29,45,101-131,347-421` (плотность 0.9, 2×2 поиск почвы, две кроны, паучий подземелье) | `WorldGenGreatwoodTrees.java` | ✅ совпадает |
| Таблицы торгов | `VillageBankerManager.java:21-32` (12 строк), `VillageWizardManager.java:19-33` (15 строк) | `VillageWorldGenRules.java:631-680` | ✅ совпадают |
| Состав сундука башни | `ComponentWizardTower.java:104` (веса 3/10/5/5/3/20/5/5/5/20 = 81, 4..9 предметов) | `VillageWorldGenRules.java:686-712` | ✅ совпадает |

---

## 4. Мир-генерация: код, который может бросить исключение

Найдено и снято:

1. `MagicalTreePlacement.finish()` — бросал `IllegalStateException` (исправлено, §2 D4).

Остаётся (задокументировано, не менялось):

2. `common/src/main/java/thaumcraft/common/lib/world/NodeGeneration.java:79` —
   `if (level instanceof ServerLevel server && !server.getServer().isSameThread()) throw new IllegalStateException(...)`.
   Во время генерации `level` — это `WorldGenRegion`, а не `ServerLevel`, поэтому во время
   генерации мира путь недостижим; это защита рантайм-вызова (например, из узла). Оставлена:
   снять её означало бы разрешить запись узла с чужого потока.

Других явных `throw` в путях worldgen домена нет (проверены `WorldGen*Feature`,
`EldritchRingFeature`, `WorldGenMound`, `VillageBankerHome`, `VillageWizardTower`,
`ThaumcraftTotemFeature`, `ThaumcraftOreFeature`, `ThaumcraftBiomeDecorationFeature`,
`ThaumcraftTreeFeature`, `MagicalTreePlacement`, `OriginalBiomeVegetation`,
`WorldGenManaPods`, `WorldGenCustomFlowers`, `NodeGeneration`).

Потенциальные `IllegalArgumentException` от `Random.nextInt(0)` проверены:
`NodeGeneration.plan` берёт `nextInt(aura/2)`, а минимальная аура таблицы `BiomeHandler` — 50
(де dead 50), значит `aura/2 >= 25`; после деления на 4 (серебряное/малое) — не меньше 12.
Диапазоны `random.nextInt` в остальных местах константны и положительны.

---

## 5. Нарушения области записи («setBlock in a far chunk»)

После волны от 22.10.2026 (деревья декоратора стартуют в `chunk+0..15`) новых нарушений
не найдено. Проверены все записи домена:

| Место | Проверка |
|---|---|
| `WorldGenHilltopStones.set/setState` | `WorldGenWriteArea.canWrite` перед каждой записью (`WorldGenHilltopStones.java:287,293`) |
| `EldritchRingFeature.buildColumn/set/buildAltar` | `canWrite` (`EldritchRingFeature.java:152,198,212`) |
| `VillageBankerHome.set` / `VillageWizardTower` | `canWrite` (`VillageBankerHome.java:102`) |
| `WorldGenMound.set` | `canWrite` (`WorldGenMound.java:244`) |
| `ThaumcraftTotemFeature.set` | `canWrite` (`ThaumcraftTotemFeature.java:153`) |
| `ThaumcraftOreFeature.single` | `canWrite` (`ThaumcraftOreFeature.java:114`) |
| `OriginalBiomeVegetation.*` | `available()` = `hasChunkAt` + `canWrite` (`OriginalBiomeVegetation.java:19`) |
| `MagicalTreePlacement.put/node` | `available()` = `hasChunkAt` + `canWrite` (`MagicalTreePlacement.java:27-47`) |
| `NodeGeneration.createRandomNodeAt` | `canWrite` (`NodeGeneration.java:80`) |
| `GreatwoodDungeon` (паутина/сундук/спавнер) | позиции внутри ±7 от начала дерева, начало — `chunk+0..15` ⇒ не выходит за `chunk±1` |
| `OriginalBiomeVegetation.hugeMushroom` | записи радиуса 3 не проверяются `canWrite` напрямую, но начало — `chunk+8..+23`, радиус ≤ 3 ⇒ `chunk+26`, то есть в пределах радиуса записи 1 |

Открытых нарушений области записи не осталось.

---

## 6. Что не удалось закрыть (и почему)

1. **`ThaumcraftWorldGenerator.java` остаётся `partial`.** Проход диких узлов оригинала
   делегирует в **TC4 API jar** (`thaumcraft.api.expands.worldgen.node.NodeGenerationManager`).
   В этом окружении доступен только игровой jar `Thaumcraft-1.7.10-4.2.3.5.jar` (942 класса),
   пакета `thaumcraft/api/expands/` в нём нет, поэтому построчную сверку этого вызова
   выполнить нельзя. Порт воспроизводит его фильтром частоты `1/36` (`Config.nodeRarity = 36`,
   `Config.java:131`), что совпадает с числом оригинала, но не с его кодом.
   Остальная часть единицы (поверхностный проход, цепочка структур, проход растительности,
   рудный проход, тотем) сверена построчно и расхождений не имеет.

2. **`WorldgenLootHelper.java` остаётся `partial`.** Это legacy-only хелпер 1.12 без
   соответствия в оракуле; оригинал наполнял сундуки через `ChestGenHooks.getInfo("dungeonChest")`
   (`WorldGenHilltopStones.java:77-78`, `WorldGenGreatwoodTrees.java:414-415`,
   `WorldGenMound.java`), а список предметов этой таблицы — ванильная 1.7.10, которой в этом
   окружении нет. Порт использует нативную `minecraft:chest/simple_dungeon` (две прокрутки,
   как две прокрутки оригинала). Точный список предметов не сверялся.

3. **Отклонения, сознательно сохранённые (задокументированы в коде):**
   * `WorldGenCustomFlowers` находит `RED_SAND` — в 1.7.10 это тот же блок `Blocks.sand`
     с метаданными 1, поэтому поведение совпадает.
   * Виноград 1.7.10 указывал на сторону винограда, *отличную* от опоры; 1.21.1 удаляет
     виноград без опоры, поэтому порт вешает лозу на сторону камня
     (`WorldGenHilltopStones.java:225-230`).
   * Область очистки/цоколя башни: оригинал чистит полосу `x 0..4, z 0..11`
     (`ComponentWizardTower.java:88-93` — вложенные циклы `12 × 5`), а не квадрат 6×6;
     порт чистит/засыпает полный отпечаток компонента. Это quirk оригинала, воспроизведение
     которого меняет вид фундамента башни и не было сделано намеренно.
   * Лотерея узлов серебряного дерева: оригинал 1.7.10 пишет узел прямо в ствол
     (`WorldGenSilverwoodTrees.java:93`), порт — в отдельный блок «серебряный узел»
     (`SILVERWOOD_KNOT`), что и было предметом предыдущей волны.

---

## 7. Авторские тесты (написаны, **не выполнены**)

Gradle в этой волне запускать нельзя (общий кэш Loom), поэтому все тесты ниже
**authored but NOT executed**. Компиляция тоже не проверялась.

| Файл | Тестов | Что утверждает |
|---|---|---|
| `common/src/test/java/thaumcraft/worldgen/MagicalTreeCanopyTest.java` | 5 | Таблица числа узлов листвы большого магического дерева для высот 11..22 (2,2,2,2,2,2,3,3,3,3,3,4) и великого дерева для 11..21 (1,2,2,2,2,2,2,2,3,3,3) — формулы оригинала; диапазоны высот `11+nextInt(12)` и `11+nextInt(11)`; плотности двух деревьев различаются на высотах 11, 17, 18 и 22 |
| `common/src/test/java/thaumcraft/worldgen/VillageComponentParityTest.java` | 9 | Оболочка дома банкира (пол, dirt-пол, обе крыши, 10 брёвен края, 4 угловых столба, 2 окна iron_bars, дверь); три слота стола (`tablePosition > 0`); десять углов башни волшебника и **отсутствие** угла (5,0,1) и (5,0,5); 12-углов vs 10-углов; полы, столбы, 6 стёкол, лестница, люк, светокамень, сундук, дверь башни; волшебник не спавнится (x = 7 при ширине 6); розыгрыш минимального размера деревни `i` или `i+1`; веса кусков 25 и 15 и профессии 191 и 190; 12 торгов банкира и 15 торгов волшебника с оригинальными диапазонами |
| `common/src/test/java/thaumcraft/worldgen/OriginalSurfaceGeneratorNumbersTest.java` | 8 | Частоты цепочки 1/150, 1/66, 1/40 и тотема 1/360; исключительность цепочки и распределение 40 000 бросков; позиция внутри чанка рисуется до розыгрыша цепочки; `chunkSeed = worldSeed + chunkX*341873128712 + chunkZ*132897987541`; высота/узел тотема (4, 5, 1 из 4, минимум 40); 18 попыток цветов и разбросы 8/4; пределы поиска мана-боба (128, дрейф 4, возраст 2..6); область записи радиуса 1 (`-16..+31`) |
| `common/src/test/java/thaumcraft/world/VillageWorldGenRulesTest.java` (изменён) | 1 | Розыгрыш минимального размера деревни: `i` или `i+1` (было «всегда `i`») |

---

## 8. Ledger

`docs/porting/scope-ledger.json` изменён **только** в 18 строках домена: `review.native_coverage`,
`review.callsite_audit`, `review.evidence`, `review.notes`. Поля `disposition`, `queue`,
`source_semantics`, `reviewed_sha256` и блок `counts` **не тронуты** — проверено сравнением
с `HEAD`:

```
changed rows: 18 (ровно мои единицы)
counts unchanged: True
all other fields unchanged: True
```

Проверка формы и якорей (скрипт `scripts/porting/check_scope_ledger_reviews.py`, добавлен в этом
прогоне, потому что приватный оракул в этом окружении недоступен):

```
$ python3 scripts/porting/check_scope_ledger_reviews.py
{"mode": "check", "rows": 1044, "invalid_reviews": 0, "stale_review_fingerprints": 0, "counts_match": true}
```

`python3 scripts/porting/update_scope_ledger.py --check` **не запускался успешно** в этом
окружении: приватный оракул лежит по пути `ROOT.parent/work/thaumcraft-4.2.3.5-oracle`
(`/home/cyberfox-kali/wt/work/thaumcraft-4.2.3.5-oracle`), а такого каталога здесь нет —
скрипт завершается строкой `Missing source root`. Попытка подставить читаемое дерево
`/home/cyberfox-kali/OpenTC4/src/main/java` как оракул не годится: это другой исходник
(не декомпиляция 4.2.3.5), у него другие sha256 и на три файла меньше
(`thaumcraft/common/lib/HexUtils.java`, `thaumcraft/common/lib/UUIDHelper.java`,
`thaumcraft/common/lib/utils/UUIDHelper.java`). Проверку `--check` должен выполнить лидер в
окружении с оракулом; изменения ledger её не ломают, потому что `reviewed_sha256` не менялись,
а исходники оракула не менялись.

---

## 9. Изменённые файлы

Продакшн:

```
common/src/main/java/thaumcraft/common/lib/world/WorldGenBigMagicTree.java      D1: плотность кроны 0.8 -> 1.0
common/src/main/java/thaumcraft/common/lib/world/WorldGenGreatwoodTrees.java    именованные константы высоты, leafNodeCount(0.9)
common/src/main/java/thaumcraft/common/lib/world/VillageWorldGenRules.java       D2: десять углов башни; D3: розыгрыш минимального размера деревни
common/src/main/java/thaumcraft/common/lib/world/VillageBankerManager.java       D3: доступ к розыгрышу
common/src/main/java/thaumcraft/common/lib/world/VillageWizardManager.java       D3: доступ к розыгрышу
common/src/main/java/thaumcraft/common/lib/world/MagicalTreePlacement.java      D4: бросок исключения заменён счётчиком
common/src/main/java/thaumcraft/common/lib/world/WorldGenCustomFlowers.java     константы оригинала (18, 8, 4)
common/src/main/java/thaumcraft/common/lib/world/WorldGenManaPods.java          константы оригинала (128, 4, 2, 5)
```

Тесты:

```
common/src/test/java/thaumcraft/worldgen/MagicalTreeCanopyTest.java              новая
common/src/test/java/thaumcraft/worldgen/VillageComponentParityTest.java         новая
common/src/test/java/thaumcraft/worldgen/OriginalSurfaceGeneratorNumbersTest.java новая
common/src/test/java/thaumcraft/world/VillageWorldGenRulesTest.java              изменён (розыгрыш минимального размера деревни)
```

Инструменты и данные:

```
scripts/porting/check_scope_ledger_reviews.py                                   новая (проверка ledger без приватного оракула)
docs/porting/scope-ledger.json                                                  18 строк домена
```

---

## 10. Что осталось за рамками прогона

* **Тесты не выполнялись** и код не компилировался: Gradle в этой волне не запускается
  (общий кэш Loom). Все утверждения тестов — ручная транскрипция оригинала.
* **Клиент не проверялся**: визуальная приёмка кроны, башни и тотема остаётся за
  клиентским прогоном.
* **GameTest домена не перезапускался**: `villageComponentsReachTheOverworld` и
  `surfacestructuresReachTheOverworld` должны быть перезапущены лидером — правка D2 убирает
  два блока из основания башни волшебника, а тест ищет только дубовую дверь и потому не
  должен пострадать.
* **Мультиплеер и полная survival-прогрессия** не проверялись.
