# Аудит соответствия порта: базовый контент и алхимия (TC4 4.2.3.5 → 1.21.1)

Дата: 2026-10-21. Домен: `base_content_and_small_interactions` (79 строк) и
`alchemy_and_essentia` (29 строк) — 108 стрий worklist
`.tmp/audit/a6-content.json`.

Источник истины: `/home/Acak/work/thaumcraft-4.2.3.5-oracle` (декомпиляция
`Thaumcraft-1.7.10-4.2.3.5.jar`). Нативный порт: `common/src/main/java/thaumcraft/`
и `common/src/main/resources/`. Легаси 1.12 использовано только как справочный
материал (не как истина) — в частности, чтобы расшифровать обфусцированные
имена полей 1.7.10 (`Material.field_151587_i` = FIRE, `Material.field_151581_o` = LAVA,
`Items.field_151133_ar` = GLASS_BOTTLE, `func_149711_c` = setHardness,
`func_149752_b` = setResistance, `func_149715_a` = setLightLevel).

Артефакт ревью: `docs/porting/audit-reviews-a6-content.json` (108 записей, все
`reviewed_sha256` совпадают с `docs/porting/scope-ledger.json`).

## Итог

| вердикт | base_content (79) | alchemy (29) | всего (108) |
|---|---|---|---|
| `disposition: port` | 40 | 14 | 54 |
| `disposition: replace` | 22 | 8 | 30 |
| `disposition: adapt` | 0 | 0 | 0 |
| `disposition: exclude` | 17 | 7 | 24 |
| `disposition: needs_review` | 0 | 0 | 0 |
| `native_coverage: parity_verified` | 38 | 20 | 58 |
| `native_coverage: partial` | 41 | 9 | 50 |
| `native_coverage: missing` | 0 | 0 | 0 |

`source_semantics`: `adapted` — 76, `equivalent` — 7, `not_applicable` — 25
(у 24 строк `exclude` и у `thaumcraft/common/blocks/CustomStepSound.java`).

Ключевое: **ни один класс не потерян** — у всех 108 строк есть нативный
аналог. Основные проблемы — не в отсутствии кода, а в (а) неверных числах
взрывостойкости у ряда блоков, (б) не перенесённых взаимодействиях банок
мозга/узла и (в) двух рецептах тигля, у которых в порте переставлены
результат и катализатор.

## Что сверено по числам и сошлось

* **Тигель** (`oracle thaumcraft/common/tiles/TileCrucible.java`): ёмкость воды
  1000 мБ, `maxTags` 100, кривая нагрева `+1 + 2*bellows` при `heat < 200`,
  остывание `-1`, порог разложения `heat > 150`, счётчик `counter > 100`,
  высота жидкости `0.3 + 0.5*water/1000 + tag/100*(1-base)` — всё воспроизведено
  в `native common/src/main/java/thaumcraft/common/tiles/TileCrucible.java:109-172`.
  Сохранена и оригинальная peculiarity: за одно столкновение переплавляется
  `ceil(stack/2)` предметов, а не весь стак (`TileCrucible.java:174-227`).
* **Алембик**: `maxAmount = 32`, приём «тот же аспект или пусто», разрядка
  сбросает аспект (`oracle TileAlembic.java:44,93-122` ↔
  `native tiles/TileAlembic.java:25,75-93`).
* **Банки**: ёмкость 64, фильтр-метка, `getSuctionAmount` 64/32/0 для обычной
  и 48/32 для пустотной (`oracle TileJarFillable.java:14,152-160`,
  `TileJarFillableVoid.java:33-36` ↔ `native tiles/TileJar.java:136-142`).
  Пятитактовый забор сверху воспроизведён дословно
  (`oracle TileJarFillable.java:189-209` ↔ `native tiles/TileJar.java:151-162`
  и `native lib/essentia/EssentiaTransfers.java:487-495`).
* **Меха кузнечных мехов**: амплитуда `0.35 + rand*0.55`, шаг `-0.075` / `+0.025`,
  такт ванильной печи раз в два тика (`oracle TileBellows.java:29-48` ↔
  `native tiles/TileBellows.java:34-46`).
* **Семейство cosmetic solid**: твёрдость 30/4/2, взрывостойкость 999/20/6,
  свет 9 и 4 — таблицы перенесены без изменений
  (`oracle BlockCosmeticSolid.java:213-252` ↔
  `native lib/world/EldritchBlockRules.java:205-241,265-267`).
* **Семейство airy**: твёрдость 2/100/-1, взрывостойкость 200/50/MAX, свет
  15/0/8 (`oracle BlockAiry.java:115-140` ↔ `native EldritchBlockRules.java:333,409-416`).
* **Свечи**: твёрдость 0.1, свет 14, бокс `(3,0,3)-(10,8,10)`/16
  (`oracle BlockCandle.java:48-52,104-106` ↔ `native blocks/BlockTallowCandle.java:28-33`).
* **Кристаллы**: `setHardness(0.7)`, `setResistance(1)` → эффективные 0.6 и
  `setLightLevel(0.5)` → 7 (`oracle BlockCrystal.java:57-62` ↔
  `native config/ThaumcraftCrystalClusters.java:37`).
* **Мешочки с лутом**: 8 + `rand(5)` бросков, шанс снаряжения `0.025*rarity`,
  уровень зачарования `5 + rarity*0.75*rand(18)` (`oracle ItemLootBag.java:89-101`,
  `oracle lib/utils/Utils.java:380-406` ↔ `native items/ItemLootBag.java:54-57`,
  `native lib/loot/LootTables.java:67-86,105-111`).
* **Тройное мясо**: 6 голода / 0.8 насыщения / регенерация 5 с (100 тиков) с
  вероятностью 0.66 (`oracle ItemTripleMeatTreat.java:23-26` ↔
  `native lib/items/EquipmentRules.java:450-456`).
* **Раздатчик алюментума**: сила броска 1.1, разброс 6.0, подъём 0.1, событие
  1009 (`oracle BehaviorDispenseAlumetum.java:17-38` ↔
  `native items/BehaviorDispenseAlumetum.java:21-35`,
  `native lib/items/EquipmentRules.java:535-541`).
* **Рецепты тигля**: 46 JSON-рецептов ↔ 54 рецепта оригинала (8 из них
  условные по модам — медь/олово/серебро/свинец — в 1.21.1 не существуют).
  Аспекты, катализаторы и результаты совпадают; расхождения — в разделе
  «Отклонения».

## Отклонения, требующие решения

### 1. Взрывостойкость блоков: сырое значение сеттера вместо эффективного (7 блоков)

В 1.7.10 `Block.setResistance(x)` сохраняет `3*x`, а геттер делит поле на 5,
то есть **эффективная** взрывостойкость равна `0.6*x`. Это уже установлено в
самом порту (`docs/analysis/2026-10-19-golem-material-blocks.md:11`,
`docs/analysis/2026-10-20-crystal-clusters.md:31`, сверено с байткодом
ванильного `Block` 1.7.10). Порту удалось применить это правило **не везде**:

| блок | оригинал (`setResistance`) | эффективное 1.7.10 | порт | где |
|---|---|---|---|---|
| BlockMetalDevice (алембик, тигель) | 17 | **10.2** | 17 | `native config/ThaumcraftAlchemy.java:33`, `ThaumcraftAlembics.java:26` |
| BlockWoodenDevice | 10 | **6.0** | 10 | `native lib/world/EldritchBlockRules.java:501` |
| BlockStoneDevice | 25 | **15.0** | 25 | `native lib/world/EldritchBlockRules.java:504` |
| BlockCosmeticOpaque | 5 | **3.0** | 5 | `native lib/world/EldritchBlockRules.java:293` |
| BlockCustomOre (6 руд) | 5 | **3.0** | 5 | `native config/ThaumcraftOres.java:44` |
| BlockArcaneDoor | 999 | **599.4** | 999 | `native config/ThaumcraftEldritchBlocks.java:150` |
| magic box (wooden device) | 10 | **6.0** | 12.5 | `native config/ThaumcraftEldritchBlocks.java:153` — значение 12.5 в оригинале отсутствует |

Контрпримеры в том же порту, где правило применено: `alchemical_construct` и
`advanced_alchemical_furnace` — 10.2 (`native config/ThaumcraftDistillation.java:40-42`,
`ThaumcraftAdvancedFurnace.java:25`), левитатор — 9.0
(`ThaumcraftLevitator.java:26`), кристалл — 0.6, cosmetic solid — 6.0,
хрупкий брус — 6.0. То есть внутри одной кодовой базы одни и те же блоки
получили разные значения; это не системное решение, а пропуск.

Оригинал: `oracle thaumcraft/common/blocks/BlockMetalDevice.java:101`,
`BlockWoodenDevice.java:82`, `BlockStoneDevice.java:93`,
`BlockCosmeticOpaque.java:66`, `BlockCustomOre.java:60`,
`BlockArcaneDoor.java:68`.

### 2. Два рецепта тигля в порте «починены», а оригинал делает наоборот

* `oracle thaumcraft/common/config/ConfigRecipes.java:175` —
  **CoreGuard**: результат `itemGolemCore` md **100** (пустой сердечник),
  катализатор md **4** (guard). То есть оригинал превращает *guard-сердечник
  в пустой*.
* `oracle ConfigRecipes.java:177` — **CoreButcher**: результат md **4**
  (guard), катализатор md **9** (butcher). Оригинал превращает *butcher-сердечник
  в guard*.

Порт: `native data/thaumcraft/recipe/crucible/golem_core_guard.json`
(катализатор `golem_core_blank` → результат `golem_core_guard`) и
`golem_core_butcher.json` (катализатор `golem_core_guard` → результат
`golem_core_butcher`). Это «логичная» версия, но она не совпадает с
оригиналом: в оригинале guard и butcher явно перепутаны местами
(метаданные: `oracle thaumcraft/common/entities/golems/ItemGolemCore.java:44-57`,
где 4 = guard, 9 = butcher, 100 = blank). AGENTS.md требует сохранять
«канонические значения и причуды TC4», поэтому это отклонение, а не
исправление, и требует отдельного решения.

### 3. Алюментум принимается и из угля, и из древесного угля

`oracle ConfigRecipes.java:115` — катализатор `Items.coal` с
`WildcardValue`-метаданными, то есть **только** уголь (в 1.7.10 `Items.coal`
и `Items.charcoal` — разные предметы). Порт:
`native data/thaumcraft/recipe/crucible/alumentum.json` допускает
`minecraft:coal` **или** `minecraft:charcoal`. Расширение полезное, но это
изменение рецепта относительно оригинала.

### 4. Рецепт «Bucket of Death» использует ведро вместо стеклянной бутылки

`oracle ConfigRecipes.java:160` — катализатор `Items.field_151133_ar`
= **GLASS_BOTTLE** (подтверждается легаси 1.12:
`src/main/java/thaumcraft/common/config/recipes/ConfigRecipesCrucibleSlice.java:216-218`).
Порт: `native data/thaumcraft/recipe/crucible/bucket_of_death.json` —
катализатор `minecraft:bucket`. Это меняет рецепт и доступность ресурса.

### 5. Источник нагрева тигля: `BlockTags.FIRE` шире, чем `Material.FIRE`

`oracle TileCrucible.java:90` — нагрев только при `Material.FIRE` (только блок
огня), `Material.LAVA` (лава) либо `blockAiry` md 1 (нитор). Порт:
`native tiles/TileCrucible.java:146-149` — `state.is(BlockTags.FIRE)`, куда
входят `campfire` и `soul_campfire`. Тигель теперь греется от костра, чего в
оригинале не было. (Нитор перенесён корректно: `blockAiry` md 1 в оригинале —
это `TileNitor`, см. `oracle BlockAiry.java:313-320`, в порту это отдельный
блок `thaumcraft:nitor`.)

### 6. Свет банки: оригинал 0, порт 9; узловая банка: оригинал 11, порт 0

`oracle BlockJar.java:299-305` — `getLightValue` возвращает **11** только для
метаданных 2 (банка узла) и 0 для обычной и пустотной банки (блок не задаёт
`setLightLevel`). Порт: `native config/ThaumcraftContent.java:91-92` задаёт
`lightLevel(state -> 9)` **обеим** банкам, а `native
config/ThaumcraftEldritchBlocks.java:197-198` (блок `jar_node`) не задаёт
света вовсе. Итог: обычная банка в порте стала источником света 9 (в
оригинале — 0), а банка узла потеряла свой свет 11.

### 7. Уничтожение банки мозга теряет накопленный опыт

`oracle BlockJar.java:182-193` — при разрушении банки мозга (метаданные 1)
в мир выбрасываются сферы опыта, соответствующие накопленному `xp`.
Порт: `native config/ThaumcraftEldritchBlocks.java:350-373` (`JarBlock`) не
имеет `onRemove`/`playerWillDestroy`, а `native
data/thaumcraft/loot_table/blocks/jar_brain.json` роняет только сам блок —
накопленный опыт исчезает.

### 8. Право-клик по банке мозга больше не отдаёт опыт

`oracle BlockJar.java:223-242` — правый клик по банке мозга ставит
`eatDelay = 40` и выбрасывает случайную долю накопленного опыта
(`rand.nextInt(min(xp+1, 64))`). В порту у `JarBlock` нет обработчика
использования, а `native tiles/TileJarBrain.java:38-41` — единственный
обработчик — работает только на сервере и не имеет `use`-обработчика.

### 9. Банка узла: не восстанавливается узел палочкой

`oracle thaumcraft/common/tiles/TileJarNode.java:207-225` — правый клик
палочкой по банке узла превращает её обратно в узел (`blockAiry` md 0) и
переносит аспекты, тип, модификатор и идентификатор узла. Порт:
`native tiles/TileJarNode.java:61-73` содержит такой метод, но **никто его не
вызывает**: в нативном дереве нет ни одного вызова `IWandable.onWandRightClick`
(grep по `common/src/main/java` и `fabric/src/main/java` даёт только объявление
интерфейса `api/wands/IWandable.java:18,21`). Функциональность «собрать узел
обратно» в порте отсутствует. Кроме того, нативный метод уменьшает стек
палочки на 1 (`TileJarNode.java:71`), чего оригинал никогда не делает.

### 10. Банка узла теряет аспекты при разрушении

`oracle BlockJar.java:169-178` — при разрушении банки узла (метаданные 2)
выпадает `itemJarNode` с аспектами и атрибутами узла. Порт:
`native data/thaumcraft/loot_table/blocks/jar_node.json` роняет пустой
`thaumcraft:jar_node` без данных.

### 11. Шаг банки: «thaumcraft:jar» → ванильное стекло

`oracle BlockJar.java:73` — `new CustomStepSound("jar", 1.0f, 1.0f)`.
Порт: `native config/ThaumcraftContent.java:91-92` — `SoundType.GLASS`.
Класс `thaumcraft/common/blocks/CustomStepSound.java` и его потомок
`JarStepSound.java` в порту отсутствуют как класс (1.21.1 берёт звук из
`SoundType`), и для банки это единственное видимое отличие.

### 12. Поведение «съеденного» катализатора в тигле

`oracle TileCrucible.java:256-263` — если предмет не имеет аспектов, тигель
возвращается из `attemptSmelt` **до** записи количества обратно в стак, из-за
чего успешно сваренные до этого предметы могут удваиваться. Порт
(`native tiles/TileCrucible.java:206-212`) фиксирует количество и выходит из
цикла, не тиражируя выдачу. Это отклонение в пользу корректности — совпадает с
принципом «фиксировать воспроизводимые дефекты оригинала только с
доказательствами», но должно быть зафиксировано как решение.

### 13. Событие крафта игрока в тигле не вызывается

`oracle TileCrucible.java:246` — `FMLCommonHandler.firePlayerCraftingEvent`
при успешной переплавке. В порте аналога нет (Forge-события в 1.21.1/Fabric
нет). Событие не воспроизведено.

### 14. У кузнечных мехов «соседний» отсчёт питания

`oracle TileBellows.java:62` — соседние меха считаются только если они
**не запитаны** (`world.isBlockIndirectlyGettingPowered`, то есть прямое или
косвенное питание). Порт: `native tiles/TileBellows.java:28,70` использует
`level.hasNeighborSignal(worldPosition)` — только непосредственный сигнал
соседа. Косвенно запитанные меха в порте будут учтены.

### 15. `TileJarBrain`: радиус и задержка поедания опыта

`oracle TileJarBrain.java:93-99` — банка съедает **все** сферы опыта в рамке
`x-0.1..x+1.1` и добавляет их суммарный опыт (`eo.getXPDrops()`).
`oracle TileJarBrain.java:106-108` — `eatDelay` (который оригинал ставит в 40
при клике по банке, `BlockJar.java:225`) тормозит поедание.
Порт: `native tiles/TileJarBrain.java:56-65` съедает только **одну** ближайшую
сферу на расстоянии < 1.0 и ставит `eatDelay = 5`. Радиус притяжения 7 и
коэффициенты 0.15/0.33/0.15 совпадают, но условие поедания и пауза — нет.

### 16. Тигель: интерфейс fluid-handler отсутствует

`oracle TileCrucible.java:48,358-394` — тигель реализует `IFluidHandler` с
tank` на 1000 мБ воды, доступный другим модам (заливка/слив через
`fill`/`drain`). Порт: `native tiles/TileCrucible.java:41,68-76` — только
внутренний `fillWater(int, boolean)`, внешнего fluid-API нет. Для Fabric это
осознанное ограничение платформы, но это видно в игре: мод, разливающий
воду в тигель, больше не сможет этого делать.

### 17. В ward-стекле потеряны особые числа оригинала

`oracle BlockCosmeticOpaque.java:253-266` — для метаданных 2 (warded glass)
возвращается твёрдость `Config.wardedStone ? -1.0f : 5.0f` (то есть
неразрушимый блок при включённой опции) и взрывостойкость **999** (всегда).
Порт: `native blocks/BlockCosmeticOpaque.java:22,24-27` — вариант есть, но
свойства блока общие: твёрдость 1.5 и взрывостойкость 5.0
(`native config/ThaumcraftEldritchBlocks.java:127-131`). Т.е. ward-стекло в
порте и разрушимо, и держит взрыв в 200 раз хуже оригинала.

То же относится к двери: `oracle BlockArcaneDoor.java:68` — твёрдость
`Config.wardedStone ? -1.0f : 15.0f`; порт всегда 15.0
(`native config/ThaumcraftEldritchBlocks.java:148-150`), хотя в порте есть
настройка `wardedStone` (`native config/ResearchSettings.java:6`,
`native lib/research/ResearchConditions.java:34`) — она влияет только на
условия исследований, но не на прочность блоков.

## Дефекты

### D1. `thaumcraft:triple_meat_treat` зарегистрирован, но без имени

Предмет зарегистрирован (`native
common/src/main/java/thaumcraft/common/config/ThaumcraftLegacyItems.java`), у
него есть модель, но в `assets/thaumcraft/lang/en_us.json` и `ru_ru.json`
**нет** ключа `item.thaumcraft.triple_meat_treat`. В игре будет показана
сырая строка перевода. Проверено скриптом по всем регистрациям
`ITEMS.register(...)`: из 252 предметов без имени остались только
`triple_meat_treat` (этот домен), `thaumonomicon_cheat`, `crimson_sword`,
`eldritch_tablet`, `eldritch_placer`, `unknown_research_note(_24)`,
`flux_crystal`, `discount_ring_`, спавн-яйца и броня культистов — последние
вне этого домена.

Тот же скрипт показал, что **блоки** покрыты полностью: у всех 98
зарегистрированных блоков есть blockstate, у всех — модель (кроме
намеренных бесконечных исключений `mana_pod`, `flux_gas`, `flux_goo`,
`taint_fibres`), имя в обоих языках (кроме документированных `shock_trap`,
`hole`, `warded`, `arcane_light`) и loot-таблица (кроме 13 документированных
исключений в `common/src/test/java/thaumcraft/assets/BlockResourceCoverageTest.java`).
То есть «блок без модели/имени/лута» в моём домене не найдено — в отличие от
предметов.

### D2. Мёртвый код: `IWandable.onWandRightClick` никем не вызывается

См. п. 9 выше. Это не только потеря функции «собрать узел обратно»: интерфейс
`thaumcraft/api/wands/IWandable.java` объявлен, реализации есть, но вызова нет
нигде — значит, весь контракт «палочка по блоку» в порте не работает ни для
одного блока. Проверено: `grep -rn "onWandRightClick" common/src/main/java
fabric/src/main/java` даёт только объявление интерфейса и определения
методов.

### D3. Банка мозга/узла: `JarBlock` не имеет ни света, ни лута с данными, ни `use`

См. пп. 6, 7, 8, 10. Один и тот же класс (`native
config/ThaumcraftEldritchBlocks.java:350-373`) не реализует ни одной из
поведенческих особенностей оригинального `BlockJar` для метаданных 1 и 2.

## Не перенесено

| что | оригинал | причина |
|---|---|---|
| fluid-handler тигля | `oracle TileCrucible.java:48,358-394` (`IFluidHandler`, tank 1000 мБ) | в Fabric/1.21.1 нет Forge-fluid API; оставлен внутренний `fillWater` |
| событие крафта игрока в тигле | `oracle TileCrucible.java:246` | Forge-событие отсутствует в 1.21.1 |
| интерфейс палочки `IWandable` | `oracle api/wands/IWandable.java` | в порте нет ни одного вызова (дефект D2) |
| клиентская анимация и вздох банки мозга | `oracle TileJarBrain.java:51-89` (поворот головы, звук `thaumcraft:brain`) | нативный `TileJarBrain.tick` на клиенте сразу выходит (`native tiles/TileJarBrain.java:39`) |
| «кривой» наклон этикетки на банке | `oracle TileJarRenderer.java:71-86` (`Config.crooked`) | не воспроизведено в `native client/renderers/tile/TileJarRenderer.java` |
| отдельные предметы `itemJarFilled` / `itemJarNode` | `oracle blocks/ItemJarFilled.java`, `blocks/ItemJarNode.java` | свёрнуты в один предмет банки с компонентами (`native blocks/BlockJar.java:115-127`) |
| `ItemResource` md 8 (фильтр), 9 (фрагмент знания), 10 (зеркальное стекло), 14 (пыль), 16/17/18, md 4 (жир) и др. как поведенческие предметы | `oracle items/ItemResource.java` | 19 метаданных стали 19 предметами (`native config/ThaumcraftContent.java:139-149`), но девять из них — простые `Item` без поведения; сам порт отмечает их как «pending in MIGRATION_STATUS» |

## Остальное: что не `parity_verified` и почему

Полный список 50 строк со `native_coverage: partial` — в
`docs/porting/audit-reviews-a6-content.json` (поле `notes` у каждой строки).
Группы причин:

1. **Разделение метаданных на блоки** (`BlockMetalDevice`, `BlockStoneDevice`,
   `BlockWoodenDevice`, `BlockCosmeticSolid`, `BlockCosmeticOpaque`,
   `BlockCustomOre`, `BlockCustomPlant`, `BlockMagicalLog`, `BlockMagicalLeaves`,
   `BlockLoot`, `BlockTable`, `BlockManaPod`, `BlockCrystal`, `BlockJar` и
   соответствующие `*Item`): поведение сохранено, но перенесённые «мелочи»
   оригинала (бит 4 у листьев, `IPlantable`, hook установки ориентации
   кристалла у `BlockCustomOreItem`, отдельные `placeBlockAt`-хуки) не имеют
   аналога после снятия метаданных.
2. **Forge-специфика** (fluid-handler тигля, `IPlantable`, `IEssentiaContainerItem`
   как NBT): заменены на компоненты 1.21.1, поведение для игрока сохранено.
3. **Незавершённые подсистемы вне домена** (SPA для `ItemBathSalts`, GUI
   резонатора и проверщика рассудка, еда для `ItemManaBean`/`ItemNuggetEdible`):
   числа не перепроверялись построчно, кода в этом домене нет.

## Рецепты тигля — сводка

Оригинал: `oracle thaumcraft/common/config/ConfigRecipes.java:105-181`
(54 рецепта, 8 из них условные по модам). Порт: 46 JSON-файлов в
`common/src/main/resources/data/thaumcraft/recipe/crucible/`.

* Совпадают по аспектам, катализатору и результату: 42 рецепта, включая все
  6 `BalancedShard_*` (по 2 единицы пяти прочих аспектов), `Alumentum`
  (3/3/3), `Nitor` (3/3/3), `Thaumium` (4), `VoidMetal` (8), `VoidSeed`
  (8/8/2), `Tallow` (2), все `Alt*`, `PureIron`/`PureGold`, `TransIron`/`TransGold`,
  `EtherealBloom` (16/16/16/8), `BottleTaint` (8/8), все 8 `Golem*`,
  5 `Core*` (кроме двух переставленных), `BathSalts` (6/6/6/6),
  `SaneSoap` (16/16/16/16).
* Расходятся: `Alumentum` (добавлен древесный уголь), `Bucket of Death`
  (ведро вместо стеклянной бутылки), `CoreGuard` и `CoreButcher` (переставлены
  результат и катализатор).
* Отсутствуют: `PureCopper/PureTin/PureSilver/PureLead` и
  `TransCopper/TransTin/TransSilver/TransLead` — в оригинале они появляются
  только при наличии модов на эти руды (`Config.foundCopperIngot` и т. п.,
  `oracle ConfigRecipes.java:133-157`), в 1.21.1 таких руд нет.
* Порядок выбора рецепта сохранён: оригинал берёт рецепт с **наибольшим числом
  различных аспектов**, при равенстве — первый зарегистрированный
  (`oracle lib/crafting/ThaumcraftCraftingManager.java:127-145`); порт делает
  то же (`native common/crafting/CrucibleCrafting.java:21-46`) и хранит
  исходный порядок в поле `source_order`.
* Совпадает и требование «рецепт доступен только при завершённом
  исследовании»: `oracle ThaumcraftCraftingManager.java:136` ↔
  `native CrucibleCrafting.java:38`.

## Эссенция и её хранение

* Флакон-пустышка/флакон с эссенцией: `oracle items/ItemEssence.java:108-178`
  ↔ `native items/ItemEssence.java:45-86`. Цена — целый флакон **8** единиц
  (`native items/PhialData.java:12`), проверка «в банке есть место под 8» и
  «метка принимает этот аспект» сохранены.
* Кристалл эссенции: `oracle items/ItemCrystalEssence.java:76-96` ↔
  `native items/ItemCrystalEssence.java:43-82,76-82`. Список аспектов в
  оригинале — целиком в NBT; в порте это упорядоченный список в компоненте
  (`native items/CrystalEssenceData.java`), включая запрет на усечение и
  сортировку. Цикл подсветки 500 мс сохранён как **только отображение**.
* Перелив эссенции между контейнерами: `native
  common/lib/essentia/EssentiaTransfers.java:436-545` воспроизводит
  оригинальные правила «давление/минимальная тяга/тип» для бака (`JAR`,
  строки 487-495), для труб (`TUBE`, 475-486), для ламп (`LAMP`, 460-464) и
  для буферов (`BUFFER`, 496-503).

## Что я НЕ смог проверить

1. **Визуальную** эквивалентность клиентских моделей и рендереров
   (`ModelAlembic`, `ModelBellows`, `ModelJar`, `TileAlembicRenderer`,
   `TileBellowsRenderer`, `TileCrucibleRenderer`, `TileJarRenderer`,
   `JarItemRenderer`): сверены только координаты кубов, пивоты и преобразования
   по исходникам; рендер в игре не запускался.
2. **Звуки**: сравнение звуковых событий (`thaumcraft:jar`, `thaumcraft:brain`,
   `thaumcraft:coins`) — по именам в коде; наличие и параметры `.ogg` в
   `assets/thaumcraft/sounds` не проверялось.
3. **Полные таблицы метаданных**: `ItemResource` (19 вариантов),
   `ItemNugget` (64 иконки), `ItemShard`, `BlockCustomPlant` (6),
   `BlockMagicalLog` (4) — проверены только используемые рецептами и лутом
   значения; остальные слоты в порте могут отсутствовать намеренно.
4. **Поведение GUI** (алембик, тигель, банка, стол) — в этом домене нет
   строк GUI; проверялась только серверная логика контейнеров.
5. **Мирогенерацию** руд и растений, **спавн** существ, **воронку** —
   вне моего домена (M11/M02-other).
6. **Полный прогон Gradle**: не запускался (по условию задачи). Проверка
   ресурсов выполнена скриптом по каталогу `common/src/main/resources`, а не
   тестом `BlockResourceCoverageTest`; этот тест идёт по blockstate-файлам и
   **не** ловит зарегистрированный предмет без имени (см. D1).
7. **Совместимость с NeoForge**: аудит только по Fabric/common.

## Рекомендации

1. Привести взрывостойкость семи блоков к эффективным значениям 1.7.10
   (10.2 / 6.0 / 15.0 / 3.0 / 3.0 / 599.4) — правило уже применено в порту
   для остальных блоков.
2. Решить статус двух переставленных рецептов сердечников: вернуть оригинальное
   поведение или зафиксировать исправление как сознательное отклонение.
3. Вернуть катализатор `minecraft:glass_bottle` в рецепте `bucket_of_death` и
   убрать `minecraft:charcoal` из `alumentum` (или задокументировать оба
   расширения).
4. Починить `IWandable`: либо вызвать `onWandRightClick` из
   `ItemWandCasting`, либо явно пометить контракт как нереализованный.
5. Доработать `JarBlock`: свет 11 для `jar_node`, выброс опыта при разрушении
   и по правому клику для `jar_brain`, лут `jar_node` с аспектами узла.
6. Вернуть обычной банке свет 0 (сейчас 9).
7. Добавить `item.thaumcraft.triple_meat_treat` в `en_us.json` и `ru_ru.json`
   и расширить тест ассетов проверкой **зарегистрированных предметов**, а не
   только blockstate-файлов.
