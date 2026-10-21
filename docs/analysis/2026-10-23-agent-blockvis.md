# Невидимые блоки и поршень — 23.10.2026 (агент `blockvis`)

Вход: пункт «Не закрыто» отчёта [worldgen-log-and-research-tabs](2026-10-22-worldgen-log-and-research-tabs.md)
§5 — «блоки `cosmetic_solid*`/`eldritch*` невидимы в клиенте (`BaseEntityBlock` → `RenderShape.INVISIBLE`,
у классов нет `getRenderShape`) и не двигаются поршнем (`hasBlockEntity` на весь блок)».

Итог одной строкой: **дефект один, с двумя последствиями, и обе причины подтверждены по байткоду
Minecraft 1.21.1** (декомпиляция `minecraft-merged-1.21.1` из кэша Loom, Vineflower 1.10.1):

1. `BaseEntityBlock#getRenderShape` отдаёт `RenderShape.INVISIBLE` — значит любой блок порта, который
   несёт блок-сущность и не переопределил форму рендера, **не рисуется вообще**;
2. `BlockState#hasBlockEntity` в 1.21.1 — это `block instanceof EntityBlock`, то есть решение
   **на блок, а не на состояние**; поршень спрашивает именно его
   (`PistonBaseBlock#isPushable` → `!blockState.hasBlockEntity()`), поэтому блок, который носит
   блок-сущность ради одного варианта, поршнем не двигается **целиком**.

Обе причины исправлены: форма рендера возвращена по оракулу, а блок-сущность теперь решается
**по состоянию**, как в 1.7.10 (`hasTileEntity(int metadata)`), через общий миксин.

## 1. Поведение оригинала 1.7.10 (оракул)

Проверено и по читаемому исходнику `OpenTC4`, и по **байткоду самого JAR** оригинала
(`Thaumcraft-1.7.10-4.2.3.5.jar`, декомпиляция Vineflower) — они совпадают.

| Класс оригинала | Наследие | `getRenderType()` | `hasTileEntity(int)` | Видим? | Форма |
|---|---|---|---|---|---|
| `BlockCosmeticSolid` | `Block` (OpenTC4:52) | не переопределён → 0 (модель) | `:259-265` — md 3 (страж-камень) и md 8 (узел) | **да** | полный куб (`:165-167`) |
| `BlockCosmeticOpaque` | `BlockContainer` (`:33`) | id семейства (`:68-69`) → `BlockCosmeticOpaqueRenderer:77` | `BlockContainer` → все md; `:190-192` и `:221-223` — тайл только у md 2 | **да** | полный куб |
| `BlockEldritch` | `BlockContainer` (`:33`) | id семейства (`:140-141`) → `BlockEldritchRenderer:86` | `:120-122` — md 0, 1, 3, 8, 9, 10 | **да** | полный куб (`:110-113`) |
| `BlockEldritchNothing` | `Block` (`:23`) | **−1** (`:57-58`) | `:61-63` — только md 1 (открытый) | **нет** | нет рамки (`:69-71`) |
| `BlockEldritchPortal` | `Block` (`:22`) | **−1** (`:78-79`) | `:102` — всегда | **нет** | нет рамки (`:80-89`) |
| `BlockAiry` | `BlockContainer` (`:45`) | **−1** (`:196-197`) | `BlockContainer` → все md; `:275-289` — тайл у md 0, 1, 5 | **нет** | по варианту (`:272-280`) |
| `BlockMagicBox` | `BlockContainer` (`:22`) | не переопределён → 0 (модель) | всегда (`createNewTileEntity:72-74`) | **да** | полный куб |
| `BlockMetalDevice` | `BlockContainer` (`:42`) | id семейства (`:145-146`) | md 2 | **да** | по варианту |
| `BlockStoneDevice` | `BlockContainer` (`:35`) | id семейства (`:69-70`) | md 14 | **да** | по варианту |
| `BlockWoodenDevice` | `BlockContainer` (`:37`) | id семейства (`:200-201`) | md 8 | **да** | по варианту |
| `BlockMirror` | `BlockContainer` (`:31`) | **−1** (`:79-80`) | всегда | **нет** (стекло рисует тайл) | пластина |
| `BlockArcaneDoor` | `BlockContainer` (`:28`) | **7** (`:109-110`), `renderAsNormalBlock()=false` (`:105-107`) | всегда (`:343-345`) | **да** | по двери |

Ключевые выводы:

* **Видимость в 1.7.10 решалась на блок, а не на вариант.** У `BlockCosmeticSolid` и `BlockMagicBox`
  модель обычная; у `BlockEldritch`, `BlockCosmeticOpaque` и трёх семейств устройств — своя
  `ISimpleBlockRendering` (Forge 1.7.10, `RenderingRegistry.getNextAvailableRenderId()`), которая
  тоже рисует блок. Рендер-тип **−1** означает «блок не рисуется вовсе» (так устроены `BlockAiry`,
  `BlockEldritchNothing`, `BlockEldritchPortal`, `BlockMirror` — их рисует тайл-рендерер).
* **Блок-сущность в 1.7.10 решалась на метаданные.** `hasTileEntity(int)` у `BlockCosmeticSolid`
  и `BlockEldritch` — это ровно тот список, что выше; у `BlockContainer`-классов без своего
  переопределения (`BlockCosmeticOpaque`, `BlockAiry`) она объявлена для всех метаданных, но
  `createTileEntity`/`createNewTileEntity` отдаёт `null` для тех метаданных, у которых тайла нет
  (`BlockCosmeticOpaque:190-192` + `:221-223`, `BlockAiry:275-289`).
* **Порт 1.7.10 двигал такие блоки поршнем вместе с тайлом.** Проверка поршня в 1.7.10 не смотрит
  на наличие блок-сущности (в 1.8+ это правило появилось), поэтому камень с узлом или
  эльдрич-алтарь поршень сдвигал, и тайл переезжал вместе с блоком.

## 2. Причина в 1.21.1 (по настоящему байткоду)

Декомпиляция `minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.40545-v2.jar`
(кэш `~/.gradle/caches/fabric-loom/minecraftMaven/...`, Vineflower 1.10.1):

1. **Невидимость.**
   `net/minecraft/world/level/block/BaseEntityBlock.java:22-25`
   ```java
   @Override
   protected RenderShape getRenderShape(BlockState blockState) {
       return RenderShape.INVISIBLE;
   }
   ```
   Обычный `BlockBehaviour#getRenderShape` отдаёт `RenderShape.MODEL`
   (`net/minecraft/world/level/block/state/BlockBehaviour.java:210-211`), то есть блок без
   блок-сущности рисуется всегда, а блок с блок-сущностью — **никогда**, пока не переопределит форму.

2. **Поршень.**
   `net/minecraft/world/level/block/piston/PistonBaseBlock.java:245-278`, финальная строка 277:
   ```java
   return !blockState.hasBlockEntity();
   ```
   а `net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase.java:803-805`:
   ```java
   public boolean hasBlockEntity() {
       return this.getBlock() instanceof EntityBlock;
   }
   ```
   То есть **решение на блок**, и переопределить его в подклассе блока нельзя: в 1.21.1 метода
   `BlockBehaviour#hasBlockEntity(BlockState)` не существует (в декомпиляции `BlockBehaviour` его нет),
   а `BlockState` создаётся в конструкторе `Block` жёстко: `Block.java:178`
   `builder.create(Block::defaultBlockState, BlockState::new)` — подменить класс состояния тоже нельзя.

3. **Следствие для создания блок-сущности.**
   `net/minecraft/world/level/chunk/LevelChunk.java:273-289` — сущность создаётся только при
   `blockState.hasBlockEntity()`; `WorldGenRegion#setBlock` (`WorldGenRegion.java:271-289`) пишет
   маркер `{id:"DUMMY"}` тоже только при `hasBlockEntity()`. Поэтому «сущность на весь блок» даёт
   и пустые сущности, и те самые 2164 предупреждения «Tried to load a block entity … but failed»,
   которые прошлая волна заглушила миксином `LevelChunkPendingBlockEntityMixin`.

> **Замечание к постановке задачи.** В задании было сказано, что «правильный механизм 1.21.1 —
> это `BlockBehaviour#hasBlockEntity`, возвращающий false». В 1.21.1 такого метода **нет**;
> ближайшее — `BlockState#hasBlockEntity`, и он не переопределяем (см. п. 2.2). Второе предложение
> задания («блок, который является обычным `Block` и реализует `EntityBlock`») тоже не работает:
> `hasBlockEntity()` — это ровно `instanceof EntityBlock`, поэтому такой блок по-прежнему не двигается.
> Реальный механизм — общий миксин, который отдаёт вендоринговое решение по состоянию (п. 3).

## 3. Исправление

### 3.1 Форма рендера — по оракулу

Все четырнадцать классов порта, которые наследуют `BaseEntityBlock`/`EntityBlock` и не имели
`getRenderShape`, получили явный ответ с ссылкой на оригинал:

| Класс порта | Ответ | Оракул |
|---|---|---|
| `BlockCosmeticSolid` | `RenderShape.MODEL` | `BlockCosmeticSolid:52` (обычный `Block`) |
| `BlockCosmeticOpaque` | `RenderShape.MODEL` | `BlockCosmeticOpaque:68-69` + рендерер семейства |
| `BlockEldritch` | `RenderShape.MODEL` | `BlockEldritch:140-141` + рендерер семейства |
| `BlockMagicBox` | `RenderShape.MODEL` | `BlockMagicBox:22` (обычный блок) |
| `BlockMetalDevice` | `RenderShape.MODEL` | `BlockMetalDevice:145-146` |
| `BlockStoneDevice` | `RenderShape.MODEL` | `BlockStoneDevice:69-70` |
| `BlockWoodenDevice` | `RenderShape.MODEL` | `BlockWoodenDevice:200-201` |
| `BlockArcaneDoor` | `RenderShape.MODEL` | `BlockArcaneDoor:109-110` (дверь видна) |
| `BlockArcanePressurePlate` | `RenderShape.MODEL` | md 2 и 3 семейства `BlockWoodenDevice` |
| `BlockSilverwoodNode` | `RenderShape.MODEL` | полный брусок с узлом (`blockMagicalLog` md 2) |
| `BlockEldritchNothing` | `RenderShape.INVISIBLE` | `BlockEldritchNothing:57-58` (рендер-тип −1) |
| `BlockEldritchPortal` | `RenderShape.INVISIBLE` | `BlockEldritchPortal:78-79` (рендер-тип −1) |
| `BlockAiry` | `RenderShape.INVISIBLE` | `BlockAiry:196-197` (рендер-тип −1) |
| `BlockMirror` | `RenderShape.INVISIBLE` | `BlockMirror:79-80` (рендер-тип −1) |

Рамка выделения (selection shape) сверена с оригиналом и совпадает: полный куб у камня и эльдрича
(`BlockCosmeticSolid:165-167`, `BlockEldritch:110-113`), пусто у «ничто», врат иairy
(`BlockEldritchNothing:69-71`, `BlockEldritchPortal:80-89`, `BlockAiry:272-280`).

### 3.2 Блок-сущность по состоянию — поршень

* `common/src/main/java/thaumcraft/common/blocks/PerStateBlockEntity.java` — новый контракт:
  `boolean hasBlockEntityForState(BlockState state)`, то есть ответ оригинала `hasTileEntity(int)`
  для метаданных этого состояния.
* `common/src/main/java/thaumcraft/common/mixin/PerStateBlockEntityMixin.java` — общий миксин
  (`@Mixin(BlockBehaviour.BlockStateBase.class)`), который подменяет `hasBlockEntity()` на ответ
  контракта. Миксин **общий**: и `fabric.mod.json`, и `neoforge.mods.toml` подключают
  `thaumcraft-common.mixins.json`, поэтому правок в `fabric/src`/`neoforge/src` **не требуется**.
* Контракт реализуют пять классов: `BlockCosmeticSolid` (md 3 и 8), `BlockCosmeticOpaque` (md 2),
  `BlockEldritch` (md 0, 1, 3, 8, 9, 10 и кристалл порта), `BlockEldritchNothing` (открытый вариант),
  `BlockAiry` (только забор-страж).

Что это даёт по сравнению с 1.7.10:

* **Сущность создаётся только там, где она была.** Ровно как `hasTileEntity(int)` оригинала; пустых
  блок-сущностей больше нет, и маркеры `{id:"DUMMY"}` во время генерации чанка больше не пишутся
  для этих вариантов (`WorldGenRegion.java:271-289`) — исчезает и сам источник прежних 2164 строк
  «Tried to load a block entity … but failed» (заглушавший миксин предыдущей волны остаётся для
  остальных семейств, он им ещё нужен).
* **Поршень двигает варианты без тайла** — как в 1.7.10: 14 из 16 камней косметики, 5 из 12
  вариантов эльдрича, янтарный блок и кирпич, закрытое «ничто», прочие airy-блоки.

## 4. Тот же корень в остальных классах порта

Обход всего `common/src/main/java/thaumcraft/common/blocks/**` (65 классов) на
`extends BaseEntityBlock` / `implements EntityBlock` дал **14 классов без `getRenderShape`** — все
исправлены (таблица п. 3.1). Остальные 51 класс уже переопределяли форму сами
(`RenderShape.MODEL`, `ENTITYBLOCK_ANIMATED` или `INVISIBLE` — например `BlockAuraNode:37`,
`BlockHole:35`, `BlockNitor:40`).

Отдельно: `BlockSilverwoodNode` — единственный класс, который реализует `EntityBlock` **напрямую**
(не через `BaseEntityBlock`), поэтому невидимым он не был; форма зафиксирована явно.

Поршневой случай для блоков, у которых тайл есть **во всех** состояниях (магический ящик, арканная
дверь, зеркало, устройства с тайлом), в 1.21.1 не лечится на уровне блока: см. п. 6.

## 5. Проверки

| Проверка | Результат |
|---|---|
| `javac` всего `common/src/main/java` (1329 файлов) против `minecraft-merged-1.21.1` из кэша Loom | **105 ошибок, ровно те же, что и на чистом `HEAD`** (посимвольное сравнение списков ошибок): ни одной новой. Ошибки чужие — доступ, который даёт `thaumcraft.accesswidener`, и более старый `datafixerupper` в рукописном classpath |
| `javac` `common/src/test/java/thaumcraft/blocks/BlockVisibilityAndPistonTest.java` вместе с main | **0 ошибок в тесте** (105 чужих — те же) |
| `python3 scripts/porting/convert_assets.py --verify` | `missing_references: 0` (2903 локальные ссылки, 3879 текстур) |
| `python3 scripts/porting/update_scope_ledger.py --check` | **не запускается в этой среде**: локальный снимок оракула (`/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`) не совпадает с отпечатками, на которых построен ledger (703 устаревших отпечатка). На чистом `HEAD` — тот же самый отказ, то есть причина не в этой правке. `counts` не тронут, `stale_review_fingerprints` в нём остался `0` |
| JUnit | **не выполнены** (Gradle запускать нельзя) |
| Игра/клиент | **не запускались** |

## 6. Что осталось незакрытым (и почему)

1. **Варианты, у которых тайл есть, поршень в 1.21.1 не двигает — иначе нельзя.**
   `PistonBaseBlock#isPushable` смотрит только на `state.hasBlockEntity()`, а создание блок-сущности
   в `LevelChunk#setBlockState` (`:273-289`) идёт по **той же** галочке. Нет способа сказать
   «сущность есть, но поршень меня не тронет»: `hasBlockEntity()` — единственный переключатель, и он
   один на состояние. Полное воспроизведение 1.7.10 (поршень двигает и блок с тайлом, и тайл едет
   вместе с блоком) требует **второго миксина в загрузочном модуле**:
   * `PistonBaseBlock#isPushable` — разрешить состояниям TC4 с тайлом;
   * `PistonMovingBlockEntity` — перенести NBT блок-сущности (в 1.21.1 `PistonMovingBlockEntity`
     несёт только `movedState` (`:37`, `:51-57`), а на месте назначения
     `LevelChunk#setBlockState` создаёт **новую пустую** сущность, то есть данные тайла потерялись бы).
   Эти два миксина — работа загрузочных модулей (`fabric/src`, `neoforge/src`), в мой домен они не
   входят; в задании их делать нельзя, поэтому оставляю их лидеру (рецепт — в п. 2 и в этом списке).
2. **Пустые варианты эльдрич-семейства больше не получают «DUMMY»-маркер**, и прежний
   `LevelChunkPendingBlockEntityMixin` для них becomes лишним — он остаётся для
   `BlockArcaneFurnace`, `BlockInfusionPillar`, `BlockMetalDevice`, `BlockStoneDevice`,
   `BlockWoodenDevice`, `BlockWandPedestal` (там контракт по состоянию ещё не введён; это домен
   «устройств»).
3. **Клиент не запускался**: визуальная приёмка (скриншот, что блоки наконец видны) не выполнена.
   Клиентские единицы ledger (`thaumcraft/client/renderers/block/BlockEldritchRenderer.java` и
   соседние, `partial`) намеренно **не** переведены в `parity_verified`: без запущенного клиента
   такой вердикт нечестный.
4. **Найденная рядом мелочь (не чинил, чужой домен).** `TileJarNode:132` ставит `thaumcraft:airy`
   с вариантом 0 (узел), а `BlockAiry#newBlockEntity` отдаёт тайл только забору-стражу
   (`BlockAiry.java:171-174`), поэтому узел в банке-узле остаётся без тайла. В 1.7.10 у md 0 был
   `TileNode` (`BlockAiry:275-283`). Это домен тайлов, здесь не трогал.

## 7. Тесты (авторованы, **не выполнены**)

`common/src/test/java/thaumcraft/blocks/BlockVisibilityAndPistonTest.java` (JUnit 5, 13 наборов):

* `theCosmeticStonesAreDrawnAsTheirModel`, `theEldritchStonesAreDrawnAsTheirModel`,
  `theDeviceFamiliesAndTheMagicBoxAreDrawnAsTheirModel` — форма рендера **не** `INVISIBLE`
  у всех видимых в оригинале блоков;
* `theBlocksTheOriginalDidNotDrawStayInvisible` — «ничто», врата, зеркало и airy остаются
  `INVISIBLE`, как в оригинале (рендер-тип −1);
* `theCosmeticSolidCarriesTheTileOfTheWardingStoneAndOfTheNodeOnly`,
  `theCosmeticOpaqueCarriesTheTileOfTheWardedGlassOnly`,
  `theEldritchStoneCarriesTheTileOfTheOriginalMetadataOnly`,
  `theEldritchNothingCarriesTheTileOfTheExposedFillerOnly`,
  `theAiryFamilyCarriesTheTileOfTheWardingFenceOnly` — контракт «сущность по состоянию» совпадает
  с `hasTileEntity(int)` оригинала;
* `theStatesWithoutATileAreTheOnesThePistonMoves` — 14 из 16 камней и 5 из 12 вариантов эльдрича
  без тайла, то есть их двигает поршень;
* `theMixinThatReadsTheContractIsRegistered` — общий миксин есть в `thaumcraft-common.mixins.json`
  (иначе контракт не дошёл бы до вендорингового кода);
* `theStonesAreSelectedAsTheFullCubeOfTheOriginal`, `theFillerWithoutASelectionBoxHasNone` — рамка
  выделения совпадает с оригиналом.

Ни один из них **не запускался**: Gradle в этой волне запускать нельзя, а JUnit-прогон требует
собранных классов. Компиляция теста проверена `javac`-ом (0 ошибок в файле теста).

## 8. Файлы

Изменены (14): `common/src/main/java/thaumcraft/common/blocks/`
`BlockAiry.java`, `BlockArcaneDoor.java`, `BlockArcanePressurePlate.java`, `BlockCosmeticOpaque.java`,
`BlockCosmeticSolid.java`, `BlockEldritch.java`, `BlockEldritchNothing.java`,
`BlockEldritchPortal.java`, `BlockMagicBox.java`, `BlockMetalDevice.java`, `BlockMirror.java`,
`BlockSilverwoodNode.java`, `BlockStoneDevice.java`, `BlockWoodenDevice.java`.

Добавлены (3): `blocks/PerStateBlockEntity.java`, `mixin/PerStateBlockEntityMixin.java`,
`common/src/test/java/thaumcraft/blocks/BlockVisibilityAndPistonTest.java`.

Изменены (2): `common/src/main/resources/thaumcraft-common.mixins.json` (одна строка),
`docs/porting/scope-ledger.json` (доказательства и заметки 12 единиц; `counts` не тронут).
