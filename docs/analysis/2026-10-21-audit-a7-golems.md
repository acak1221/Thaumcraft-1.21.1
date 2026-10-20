# Аудит соответствия порта: големы и общая инфраструктура (a7)

Дата аудита: 21.10.2026. Домен: семейства `golems_and_tasks` (69 строк) и
`shared_infrastructure` (66 строк) — 135 строк worklist
(`.tmp/audit/a7-golems.json`).

Источник истины: `/home/Acak/work/thaumcraft-4.2.3.5-oracle` (декомпиляция
`Thaumcraft-1.7.10-4.2.3.5.jar`). Нативный порт: `common/src/main/java/thaumcraft/`,
`fabric/src/main/java/thaumcraft/fabric/`, `common/src/main/resources/`.
Легаси `src/main/java` использовано только как справочный материал.

Проверка велась **чтением**. Ничего в порте не изменялось; единственные
записанные файлы — этот отчёт и `docs/porting/audit-reviews-a7-golems.json`.

## Итог

| вердикт | golems_and_tasks | shared_infrastructure | всего |
|---|---|---|---|
| `port` (тот же контракт) | 1 | 9 | 10 |
| `adapt` (вынужденное отклонение) | 12 | 0 | 12 |
| `replace` (заменён конструкцией) | 53 | 46 | 99 |
| `exclude` (сознательно не переносится) | 3 | 11 | 14 |
| `needs_review` | 0 | 0 | 0 |
| **всего строк** | **69** | **66** | **135** |

| native_coverage | golems_and_tasks | shared_infrastructure | всего |
|---|---|---|---|
| `parity_verified` (сверено по числам) | 26 | 12 | 38 |
| `partial` (аналог есть, но не полностью) | 38 | 43 | 81 |
| `missing` (аналога нет) | 5 | 11 | 16 |
| `not_assessed` | 0 | 0 | 0 |

| source_semantics | всего |
|---|---|
| `equivalent` | 10 |
| `adapted` | 104 |
| `different` | 5 |
| `not_applicable` | 16 |

Ключевой вывод по големам: **направление приоритетов не инвертировано.**
1.7.10 отдавал высший приоритет **наименьшему** номеру, и 1.21.1 делает то же
(`WrappedGoal.canBeReplacedBy` отдаёт флаги только цели со **строго меньшим**
номером). Порт держит оригинальные номера без изменений
(`native common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java:846-866`),
и лестница приоритетов оригинала
(`oracle thaumcraft/common/entities/golems/EntityGolemBase.java:265-351`)
воспроизведена по всем ядрам. Ни одна оригинальная цель голема не выпала; но для
трёх ядер (Fill/Empty/Sorting) пять-семь оригинальных целей **слиты в одну**
нативную цель-маршрут — см. «Отклонения, требующие решения».

## Что подтверждено сверкой чисел (выборочно)

* Материалы голема: восемь строк `health/armor/speed/fireResist/upgrades/carry/regenDelay/strength`
  и их порядок (ordinal = метаданные) совпадают символ в символ —
  `oracle .../golems/EnumGolemType.java:9-27` ↔ `native .../golems/EnumGolemType.java:5-12`.
* Аксессуары: восемь букв `H G B F R V P M` в исходном порядке и исходные
  пары-исключения (F/H, G/V, B/P) — `oracle .../ItemGolemDecoration.java:62-90`,
  `oracle .../EntityGolemBase.java:912-933` ↔ `native .../GolemDecorations.java:30-76`.
  Все эффекты (шляпа +5 HP, очки +10 % радиуса, галстук ×1.1, феза ×0.66 регена,
  дротик, визор +1 брони, накладка +4 брони и ×0.88 скорости, булава +2 урона)
  совпадают.
* Улучшения: номера 0..5 (air/earth/fire/water/order/entropy), максимум два
  одинаковых, +1 слот у продвинутого голема —
  `oracle .../ItemGolemUpgrade.java:31-52`, `oracle .../EntityGolemBase.java:991-1006`
  ↔ `native .../GolemUpgrade.java:9-40`, `native .../GolemUpgrades.java:44-51,137`.
* Числа формул: скорость `×(1+0.15·Air)`, `×1.1` у продвинутого, `×0.88` от
  накладки; переносимость `carry + min(16,max(4,carry))·Earth`; предел жидкости
  `floor(sqrt(carry))·1000`; радиус `16+4·Water` +10 % (очки) +20 % (продвинутый);
  урон `2 + strength + Earth` (+2 с булавой); скорость атаки `20-(advanced?2:0)`;
  броня `min(20, base+1 visor+4 plating)` —
  `oracle .../EntityGolemBase.java:255-264,355-384,460-469,1088-1100,1190`
  ↔ `native .../GolemUpgrades.java:119-205`, `native .../GolemDecorations.java:77-106`.
* Дротик: интервал `30-8·Air`, первая задержка `интервал/2`, минимальная дистанция
  3 блока (`d²<9`), досягаемость `0.8·range`, один дротик за интервал —
  `oracle .../ai/combat/AIDartAttack.java:25-68` ↔ `native .../GolemDartRules.java:24-46`.
* Ближний бой: досягаемость `(width·2)²+1`, пересчёт пути `4+rand(7)`, кулдаун
  `getAttackSpeed()` — `oracle .../ai/combat/AIGolemAttackOnCollide.java:64-79`
  ↔ `native .../GuardGolemGoals.java:95-151`, `native .../EntityGolemBase.java:1290-1294`.
* Мясник: порядок «старше → моложе» без случайного гейта и правило популяции
  «больше двух» (`count > 2` вместе с жертвой) —
  `oracle .../ai/combat/AINearestButcherTarget.java:42-70`,
  `oracle .../ai/combat/AIOldestAttackableTargetSorter.java`
  ↔ `native .../GuardGolemGoals.java:271-308`, `native .../EntityGolemBase.java:1246-1254`.
* Сбор урожая и рубка: радиусы `ceil(range/4)` и `ceil(range/3)`, задержка
  `max(10,(20-2·strength)·hardness)` и `max(5,(20-3·strength)·hardness)`,
  порядок соседнего обхода (X,Z,Y) и диапазоны −2..2 / −1..1 —
  `oracle .../ai/interact/AIHarvestCrops.java:36,76-78,96-118`,
  `oracle .../ai/interact/AIHarvestLogs.java:38,76-78,132-148`
  ↔ `native .../HarvestGolemGoals.java:37-40,119-154`,
  `native .../LumberGolemGoals.java:40-43,124-181`.
* Путешествующий сундук: HP 75, урон 4, лечение каждый 50-й тик (каждый тик с
  Water), дистанция телепорта 20 и слежения 5, 600 тиков ярости, прыжок
  `(rand(10)+5)/3` с толчком 8.0/6.0 (×0.75 в воде) и фактором 0.04/0.03,
  притяжение предметов 0.5/3.0 при силе 0.075, 27/36 слотов —
  `oracle .../EntityTravelingTrunk.java:89-94,170-172,203-219,243-262,294-297,359-395`
  ↔ `native .../TrunkRules.java:8-52`, `native .../EntityTravelingTrunk.java:275-323,339-353`.
* Сторож (Watcher): исходная лестница 4, 5, 7, 8, 8, 9 воспроизведена один в
  один (`oracle .../monster/EntityWatcher.java:82-92` ↔
  `native .../monster/EntityWatcher.java:68-74`).
* Поиск источника эссенции: коробка 12, backoff 5000 мс, «мёртвый источник
  прерывает сканирование» — `oracle .../lib/events/EssentiaHandler.java:13-154`
  ↔ `native .../lib/events/EssentiaHandler.java:25-81`.
* Вспомогательные классы `Utils`, `InventoryUtils`, `BlockUtils`, `CropUtils`,
  `EntityUtils` сохранили **все** имена методов оригинала; три метода `Utils`
  (`addSpecialMiningResult`, `generateLoot`, `setBiomeAt`) вынесены в
  `SpecialMining`, `LootTables` и `BiomeColumns`, о чём сказано в шапке класса
  (`native .../lib/utils/Utils.java:33-44`).

## Аксессуары, слоты улучшений и «список задач» голема

* **Аксессуары (украшения).** В оригинале это один предмет с восемью значениями
  метаданных: `H` (шляпа), `G` (очки), `B` (галстук), `F` (феза), `R` (дротик),
  `V` (визор), `P` (накладка), `M` (булава)
  (`oracle .../ItemGolemDecoration.java:39-90`). Одновременно носимы не все
  восемь: пары-исключения `F`/`H`, `G`/`V`, `B`/`P` (`oracle .../EntityGolemBase.java:912-933`),
  то есть максимум пять (`H`/`F` + `G`/`V` + `B`/`P` + `R` + `M`).
  Порт переносит и набор, и порядок букв, и пары-исключения, и все эффекты
  (`native .../GolemDecorations.java:30-111`); в 1.21.1 без метаданных каждый
  аксессуар стал отдельным предметом (`native .../ItemGolemDecoration.java`).
* **Слоты улучшений.** Оригинал: массив `upgrades` длиной
  `golemType.upgrades + (advanced ? 1 : 0)`
  (`oracle .../EntityGolemBase.java:188-196`), где `upgrades` у материалов равен
  1 (Straw/Wood/Clay/Stone/Iron) или 2 (Tallow/Flesh/Thaumium), то есть
  **максимум 3 слота** у продвинутого голема; и одного типа можно поставить
  **не больше двух** (`oracle .../EntityGolemBase.java:991-1006`).
  Порт: `GolemUpgrades.slotCount(materialSlots, advanced)` и `MAX_SLOTS = 3`,
  `MAX_SAME_KIND = 2` (`native .../GolemUpgrades.java:17-21,44-51,137`) —
  значения совпадают с оригиналом.
* **«Список задач» голема** — это двенадцать ядер оригинала и набор целей у
  каждого (`oracle .../EntityGolemBase.java:269-345`). Все двенадцать ядер
  присутствуют в порту (`native .../GolemData.java:41-52`), и для каждого
  проверено соответствие набора целей и их номеров (см. раздел «Что
  подтверждено сверкой чисел» и список отклонений ниже).
* **Фильтры/задания (шесть «призрачных» слотов на страницу).** Оригинал:
  `ContainerGolem.bindGolemInventory` — 6 слотов на страницу, число страниц
  `slots/6 - 1`, страница прокручивается кнопками 66/67
  (`oracle .../ContainerGolem.java:46-59,82-94`).
  Порт: те же шесть слотов на страницу, число страниц считается от числа слотов
  голема, каждая страница — шесть «призрачных» образцов без реального
  склада (`native .../ContainerGolem.java:27,60-61,105`).

## Расхождения по каждому не-`parity_verified` классу

Ниже — сгруппированный список. Полные per-row вердикты, evidence и notes —
в `docs/porting/audit-reviews-a7-golems.json`.

### Големы: сущность, предметы, меню

* `thaumcraft/common/entities/golems/EntityGolemBase.java` — `replace`/`partial`.
  Один класс с полным списком целей стал тонкой сущностью плюс per-core
  классы целей (`GuardGolemGoals`, `InventoryGolemGoals`, `HarvestGolemGoals`,
  `LumberGolemGoals`, `LiquidGolemGoals`, `AlchemyGolemGoals`, `UseGolemGoals`,
  `FishingGolemGoals`) и чистые Rules-классы (`GolemUpgrades`,
  `GolemDecorations`, `GolemDartRules`, `GolemCargoRules`, `TrunkRules`).
  Все числа поведения совпадают, кроме перечисленных ниже отклонений.
* `thaumcraft/common/entities/golems/GolemHelper.java` — `replace`/`partial`.
  Хелпер разложен на `GolemCargoRules`, `GolemHomeInventory`, `GolemMarker`,
  `GolemItemMatch`, `GolemUseOptions`; публичного API-хелпера в native нет.
* `thaumcraft/common/entities/golems/ContainerGolem.java` — `replace`/`partial`.
  1.7 `Container` → `AbstractContainerMenu`; шесть «призрачных» слотов на
  страницу и число страниц сохранены, но кнопки пагинации переехали с 66/67
  на 60/61, а кнопки цвета — в диапазон 100+i.
* `thaumcraft/common/entities/golems/ItemGolemBell.java` — `replace`/`partial`.
  Семантика маркеров сохранена (без Order — переключение неокрашенного маркера;
  с Order — цикл any → 0..15 → удаление; shift-клик удаляет), но «колокол»
  теперь хранит UUID голема, измерение, упакованный дом, сторону и маркеры в
  компоненте `CUSTOM_DATA`, а разборка голема/сундука переехала из
  `onLeftClickEntity` в путь урона сущности.
* `thaumcraft/common/entities/golems/ItemGolemPlacer.java` — `replace`/`partial`.
  Метаданные 1.7 → один предмет на материал с компонентом `GolemData`;
  все поля оригинального плейсера сохранены, но реальный склад заменён
  образцами-экземплярами.
* `thaumcraft/common/entities/golems/ItemGolemCore.java`,
  `ItemGolemUpgrade.java`, `ItemGolemDecoration.java` — `replace`/`adapted`.
  Метаданных в 1.21.1 нет, поэтому на каждый вариант свой предмет; наборы
  id/букв и наборы «имеет GUI / умеет сортировать / имеет инвентарь»
  воспроизведены.
* `thaumcraft/common/entities/golems/EnumGolemType.java` — `port`/`parity_verified`.
* `thaumcraft/common/entities/golems/Marker.java` — `replace`/`adapted`,
  шесть полей сохранены.
* `thaumcraft/common/entities/golems/EntityTravelingTrunk.java`,
  `InventoryTrunk.java`, `ContainerTravelingTrunk.java`,
  `ItemTrunkSpawner.java`, `EntityGolemBobber.java`,
  `projectile/EntityGolemOrb.java` — адаптация формата, числа сохранены.

### Големы: AI-классы

* Набор целей оригинала воспроизведён **полностью**; ни одна оригинальная
  цель не потеряна, ни один номер не инвертирован. Классы-объекты целей
  оригинала удалены коммитом `c4329c2` («Remove the duplicate TC4 goal set…»)
  и заменены нативными целями.
* `ai/combat/AIDartAttack.java`, `AIGolemAttackOnCollide.java`,
  `AIHurtByTarget.java`, `AINearestButcherTarget.java`,
  `ai/interact/AIFish.java`, `AIHarvestCrops.java`, `AIHarvestLogs.java`,
  `ai/misc/AIAltarFocus.java`, `AIConvertGrass.java`, `AIWander.java`,
  `ai/combat/AIAttackOnCollide.java`, `AILongRangeAttack.java`,
  `AICreeperSwell.java` — `replace`/`parity_verified` (сверены номера и числа;
  у не-големных целей — в их собственном хозяине: культист, заражённый зверь,
  культист-жрец, заражённая овца, часовой, заражённый крипер).
* `ai/inventory/*` (13 классов) — `replace`/`partial`: пять–семь оригинальных
  целей на ядро слиты в одну нативную цель-маршрут; номер маршрута равен
  **наименьшему** из объединённой группы (Fill 4, Empty 3, Sorting 3), а
  остальная работа (взять/положить/выбросить/заменить) выполняется в
  собственном серверном тике голема. Наблюдаемые правила сохранены
  (пятитиковый `golemDelay`, таймаут 10000 мс, бюджет маршрута 200 тиков,
  тест соседства 4.0 блока, двойные сундуки), но **исходная лестница
  приоритетов внутри этих трёх ядер не воспроизведена один в один**.
* `ai/misc/AIDoorInteract.java`, `AIOpenDoor.java` — `replace`/`partial`:
  заменены ванильным `OpenDoorGoal` на том же номере 5; исходный скан
  двух следующих точек пути в радиусе 2.25 и окно «закрыть через 20 тиков»
  ушли.
* `ai/combat/AINearestAttackableTarget.java`, `AITarget.java` —
  `replace`/`partial` (см. отклонение про FOLLOW_RANGE).
* `ai/fluid/*`, `ai/interact/AIUseItem.java`, `ai/misc/AIReturnHome.java` —
  `replace`/`partial`: слиты в per-core классы, номера сохранены.

### Общая инфраструктура

* `common/config/Config.java` — `replace`/`partial`: в native остались только
  настройки исследования/taint/биома; остальные ~70 опций оригинала
  (`golem_delay`, `golem_link_quality`, `championMobs`, `shieldRecharge/Wait/Cost`,
  `nodeRarity`, `notificationDelay/Max`, id измерения Outer Lands, id зелий,
  переключатели регенерации мира) не имеют нативного аналога.
  Значения по умолчанию для големов при этом воспроизведены константами
  (5 тиков и 10000 мс — `native .../InventoryGolemGoals.java:111,131,313`).
* `common/config/ConfigAspects.java`, `ConfigBlocks.java`, `ConfigEntities.java`,
  `ConfigItems.java` — `replace`/`partial`: статические поля-реестры 1.7 разложены
  по доменным классам `Thaumcraft*` (DeferredRegister 1.21.1).
* `common/config/MaterialAiry.java` — `exclude`: класса `Block.Material` в 1.21.1
  нет.
* `common/CommonProxy.java`, `common/Thaumcraft.java` — `replace`/`partial`:
  прокси заменён архитектурой Architectury (общий bootstrap + bootstrap каждого
  лоадера).
* `common/lib/events/EventHandlerEntity.java` — `replace`/`different`/`partial`.
  Обработчик Forge заменён архитектурой mixin + события платформы; большинство
  поведений на месте, но часть отсутствует (см. «Дефекты»).
* `common/lib/events/EventHandlerWorld.java` — `replace`/`different`/`partial`.
  Обработчик Forge заменён mixin'ами; отсутствуют очередь регенерации чанков,
  fuel-хендлер и запрет постановки блока рядом с активным боссом.
* `common/lib/events/ServerTickEventsFML.java` — `replace`/`partial`:
  отсутствует очередь регенерации чанков; раскопки и очистка note-block
  перенесены в native-код.
* `common/lib/events/EssentiaHandler.java` — `replace`/`parity_verified`.
* `common/lib/events/CommandThaumcraft.java` — `replace`/`parity_verified`:
  Brigadier-дерево с теми же алиасами, тем же уровнем прав 2, теми же действиями
  и с fallback'ом на оригинальное написание.
* `common/lib/network/PacketHandler.java`, `EventHandlerNetwork.java` и 20
  пакетов (`fx/*`, `misc/*`, `playerdata/*`) — `replace`/`partial`: `IMessage` →
  payload Architectury; набор сообщений и их смысл сохранены, идентификаторы
  канала и пакетов изменились.
* `common/lib/utils/*` (`Utils`, `InventoryUtils`, `BlockUtils`, `CropUtils`,
  `EntityUtils`, `HexUtils`, `TCVec3`, `TCVec3Pool`, `UUIDHelper`) —
  `port`/`replace`; наборы методов сохранены, кроме трёх перенесённых методов
  `Utils`.
* `common/lib/CreativeTabThaumcraft.java`, `CustomSoundType.java`,
  `FakeThaumcraftPlayer.java`, `InternalMethodHandler.java`, `RefGui.java` —
  `replace`/`adapted`/`port`; у `RefGui` числовые id GUI не имеют аналога в
  1.21.1 (у каждого меню свой `MenuType`).

## Отклонения, требующие решения

1. **Цели Fill/Empty/Sorting слиты в одну цель-маршрут.**
   Оригинал: Fill = HomeReplace 0, HomePlace 1, HomeDrop 2, FillTake 3, FillGoto 4
   (`oracle .../EntityGolemBase.java:270-277`); Empty = HomeReplace 0, EmptyPlace 1,
   EmptyDrop 2, EmptyGoto 3, HomeTake 4 (`:278-285`); Sorting = HomeReplace 0,
   SortingPlace 1, SortingGoto 3, HomeTakeSorting 4 (`:335-341`).
   Порт: одна цель на ядро с номером 4 (Fill), 3 (Empty), 3 (Sorting), а остальные
   четыре действия выполняются в серверном тике голема
   (`native .../InventoryGolemGoals.java:33-37,65-77`).
   *Оригинал делает X (пять раздельных целей селектора с приоритетами 0..4), порт
   делает Y (одна цель-маршрут + тик голема).* Числа и правила переносов
   сохранены, но структура выбора цели и, следовательно, порядок «побеждает
   соседняя цель» — нет. Требуется решение: считать ли это приемлемым
   перепроектированием или возвращать раздельные цели.
2. **Маска целей Стража инвертирована и по умолчанию сужена.**
   Оригинал: `canAttackHostiles/Animals/Players/Creepers` = отрицательные биты
   `toggles[1..4]`, у свежего голема биты чисты, значит голем с Order по умолчанию
   атакует и монстров, и зверей, и игроков, и криперов
   (`oracle .../EntityGolemBase.java:818-845,1150-1179`).
   Порт: маска положительная, по умолчанию только монстры, а животные, игроки и
   криперы — opt-in (`native .../GolemGuardTargets.java:18-29`,
   `native .../EntityGolemBase.java:1160-1170`).
   *Оригинал делает X (Order-страж атакует зверей/игроков/криперов сразу), порт
   делает Y (то же самое только после ручного включения).* Отклонение
   задокументировано в самом порту, но оно меняет геймплей и должно быть
   подтверждено как осознанное.
3. **Радиус удержания цели Стража — ванильный FOLLOW_RANGE (32), а не радиус
   голема.** Оригинал `AITarget.func_75253_b` сравнивает расстояние с
   `targetDistance = getRange()` (`oracle .../ai/combat/AITarget.java:31-42`,
   `oracle .../ai/combat/AINearestAttackableTarget.java:41`).
   Порт: `golem.getAttributeValue(Attributes.FOLLOW_RANGE)` — атрибут, заданный
   один раз как 32 (`native .../GuardGolemGoals.java:251-253`,
   `native .../EntityGolemBase.java:238`).
   *Оригинал делает X (бросает цель дальше радиуса голема), порт делает Y
   (держит цель до 32 блоков).*
4. **Таймаум-голем больше не halve-ит магический урон.**
   Оригинал: `if (golemType == THAUMIUM && ds == DamageSource.magic) par2 *= 0.5f`
   (`oracle .../EntityGolemBase.java:1131-1138`).
   Порт: в `hurt` этого нет (`native .../EntityGolemBase.java:2376-2384`).
   *Оригинал делает X (таймаум-голем получает половину магического урона), порт
   делает Y (полный урон).*
5. **Утрачен иммунитет к общему источнику урона.**
   Оригинал: `if (ds == DamageSource.<X>) return false;` — одинаково в големе и в
   сундуке (`oracle .../EntityGolemBase.java:1133`, `oracle .../EntityTravelingTrunk.java:107`).
   Порт: у сундука осталось `DamageTypes.CACTUS` (`native .../EntityTravelingTrunk.java:158`),
   у голема — ничего. Требуется однозначно определить, какой именно источник
   имелся в виду в 1.7 (`field_76367_g`) и восстановить его.
6. **Сундук «воина» (`Order`) и `Entropy`:** в оригинале `getUpgrade()==3` (Water)
   делает сундук неуязвимым и открытым только владельцу — это воспроизведено;
   однако проверка владельца в порту сделана через UUID игрока, тогда как
   оригинал сравнивал строки имён (`oracle .../EntityTravelingTrunk.java:335`,
   `native .../EntityTravelingTrunk.java:116,169`) — при переименовании игрока
   поведение отличается (это скорее улучшение, но требует записи).
7. **Кнопки меню голема.** Оригинал: кнопка 50 — «взять весь стек»
   (`toggles[0]`, `oracle .../ai/inventory/AIFillTake.java:40`), 51..54 — атакуемые
   категории, 55..57 — сравнение предметов.
   Порт: 50 — режим «любое количество» Fill, 51..54 — цели Стража, 55..57 —
   сравнение предметов (`native .../ContainerGolem.java:29-34,198-235`).
   *Оригинал делает X (50 = брать весь стек), порт делает Y (50 = any amount;
   «брать весь стек» выражено флагом Fill any amount в конфигурации).*
8. **Страница плейсера/меню:** кнопки пагинации 66/67 → 60/61, кнопки цвета
   перенесены в 100+i (`oracle .../ContainerGolem.java:82-110` ↔
   `native .../ContainerGolem.java:29-30`). Это вынужденное отклонение
   (menu-layout 1.21.1), но id кнопок стоит зафиксировать в документации.

## Дефекты

1. **Защита владения големом отсутствует.**
   Оригинал: `EventHandlerEntity.playerInteract` отменял *любую* взаимодействие
   с големом, у которого есть владелец, если взаимодействующий — не владелец, и
   отправлял «You are not my Master!»
   (`oracle .../lib/events/EventHandlerEntity.java:348-356`).
   Порт: владелец сохраняется (`native .../EntityGolemBase.java:386-389`,
   `:2521`), но проверки владельца в `mobInteract`
   (`native .../EntityGolemBase.java:2261-2299`) и в `canConfigure`
   (`:739-741`) нет — **любой игрок может открыть меню голема, установить ему
   ядро/улучшения/аксессуар и разобрать его колоколом**. Ключ
   `tc.golem.notowner` в `common/src/main/resources/assets/thaumcraft/lang/en_us.json:891`
   присутствует, но нативным кодом не используется (только легаси-деревом).
   Воспроизведение: `EntityGolemBase.mobInteract` → `ContainerGolem.open` →
   меню открывается чужому игроку; `EntityGolemBase.recall` (колокол) не
   спрашивает владельца.
2. **Эндер-жемчуг не уничтожается рядом с зачарованным (warded) объектом.**
   Оригинал: `EventHandlerEntity.entitySpawns` сканировал куб 5×5×5 вокруг жемчуга
   на `TileOwned` и уничтожал жемчуг с сообщением
   (`oracle .../lib/events/EventHandlerEntity.java:362-379`).
   Порт: в `common/src/main/java` нет ни одного упоминания `EnderPearl`
   (`grep -rn "ender|Ender" common/src/main/java` — только ложные совпадения в
   именах). Поведение утрачено.
3. **Тег «бросивший» на выпавшем предмете не воспроизведён — но правило
   сохранено иным механизмом (не дефект, а отклонение).**
   Оригинал: `EventHandlerEntity.droppedItem` писал `thrower` в NBT предмета
   (`oracle .../lib/events/EventHandlerEntity.java:170-174`), а
   `TileCrucible.attemptSmelt` читал его и передавал владельца в
   `ThaumcraftCraftingManager.findMatchingCrucibleRecipe(username, ...)`
   (`oracle .../tiles/TileCrucible.java:248-259`).
   Порт: NBT-тега нет, но владелец берётся из ванильного `ItemEntity.getOwner()`
   (его выставляет `Player.drop`), а тигель спрашивает его через
   `CrucibleCrafting.thrower` (`native .../crafting/CrucibleCrafting.java:15-22`).
   Правило владения тиглем сохранено; отличается только носитель данных.
4. **Отмена подбора предметов фейковым игроком не перенесена — но и не нужна.**
   Оригинал: `EventHandlerEntity.itemPickup` отменял подбор для любого игрока,
   чьё имя начинается с «FakeThaumcraft»
   (`oracle .../lib/events/EventHandlerEntity.java:441-447`).
   Порт: фейковый игрок **никогда не добавляется в мир** — класс
   `FakeThaumcraftPlayer` есть (`native .../lib/FakeThaumcraftPlayer.java:23`),
   но ни одного использования в `common/src/main/java` нет
   (`grep -rn "FakeThaumcraftPlayer" common/src/main/java` → только сам класс).
   Сбор урожая и рубка делаются через `GolemHarvestPlatform`
   (`native .../entities/golems/EntityGolemBase.java:1343,1357`), а не через
   фейкового игрока. Хук отменять нечего. Побочно: класс
   `FakeThaumcraftPlayer` в native — мёртвый код.
5. **Луч эльдрического голема потерян.**
   Оригинал: `FXBeamGolemBoss` + `PacketFXBeamPulseGolemBoss` рисовали пульсирующий
   луч от головы босса к цели (`oracle .../client/fx/beams/FXBeamGolemBoss.java:59-88`,
   `oracle .../common/lib/network/fx/PacketFXBeamPulseGolemBoss.java:61-80`).
   Порт: луч заменён снарядом `EntityGolemOrb`
   (`native .../monster/EntityEldritchGolem.java:198-211`); классов
   `FXBeamGolemBoss` и `PacketFXBeamPulseGolemBoss` в native нет. Урон, заряд и
   кулдаун перенесены, **визуальный луч — нет**.
6. **Очередь регенерации чанков утрачена.**
   Оригинал: `EventHandlerWorld.chunkLoad` складывал чанки без тега
   `Thaumcraft.regen` в `ServerTickEventsFML.chunksToGenerate`, а
   `ServerTickEventsFML.tickChunkRegeneration` перегенерировал до 10 чанков за
   тик с воспроизведением сида мира
   (`oracle .../lib/events/EventHandlerWorld.java:121-137`,
   `oracle .../lib/events/ServerTickEventsFML.java:29-52`).
   Порт: аналогичной очереди нет; в 1.21.1 нет и события данных чанка с тем же
   тегом. Воспроизведение невозможно — механизм отсутствует в API.
7. **Правило «волшебное бревно горит 400 тиков» утрачено.**
   Оригинал: fuel-хендлер возвращал 6400 для `itemResource` (метаданные 0) и 400
   для `blockMagicalLog` (`oracle .../lib/events/EventHandlerWorld.java:139-147`).
   Порт: правило 6400 перенесено — `ALUMENTUM_BURN_TIME = 6400` и
   `FuelRegistry.register(ALUMENTUM_BURN_TIME, item)`
   (`native .../config/ThaumcraftResources.java:25,53-56`), — а правило 400 для
   бревна **нет**: `FuelRegistry.register` вызывается в native ровно один раз
   (`grep -rn "FuelRegistry.register" common/src/main/java`).
8. **Запрет постановки блока рядом с активным боссом Outer Lands утрачен.**
   Оригинал: `EventHandlerWorld.placeBlockEvent` отменял постановку, если рядом
   (32 блока) есть `EntityThaumcraftBoss` в мире Outer Lands
   (`oracle .../lib/events/EventHandlerWorld.java:225-245`).
   Порт: аналогичной проверки нет.
9. **Клиентский сброс кэша исследования при выходе утрачен.**
   Оригинал: `EventHandlerNetwork.clientLogsOut` очищал
   `GuiResearchBrowser.completedResearch` и восстанавливал клиентские флаги
   конфигурации (`oracle .../lib/network/EventHandlerNetwork.java:47-60`).
   Порт: часть с настройками воспроизведена (`ThaumcraftSettings.resetClient`),
   сброса кэша исследования — нет.

## Не перенесено

Полностью отсутствуют в native (сознательно или по необходимости):

| класс | причина |
|---|---|
| `thaumcraft/api/entities/GolemIds.java` | нет в jar TC4 4.2.3.5 (легаси-1.12); id перенесены в `GolemData`/`GolemUpgrade` |
| `thaumcraft/api/entities/IGolemInfo.java` | нет в jar TC4 4.2.3.5; данные доступны на `EntityGolemBase` |
| `thaumcraft/common/integration/waila/GolemWailaPlugin.java` | нет в jar TC4 4.2.3.5; WAILA не существует для 1.21.1 |
| `thaumcraft/common/lib/CommandThaumcraft.java` | нет в jar TC4 4.2.3.5; дубль легаси-команды, в native одна реализация |
| `thaumcraft/common/lib/SoundsTC.java`, `TCSounds.java` | нет в jar TC4 4.2.3.5 |
| `thaumcraft/common/lib/events/LootHandler.java` | нет в jar TC4 4.2.3.5; поведение перенесено в `lib/loot/*` |
| `thaumcraft/common/lib/network/PacketBase.java` | нет в jar TC4 4.2.3.5 |
| `thaumcraft/common/lib/utils/ConnectedTextureUtils.java` | нет в jar TC4 4.2.3.5; в 1.21.1 нет CTM |
| `thaumcraft/truetyper/{FontHelper,FontLoader,Formatter,TrueTypeFont}.java` | нет в jar TC4 4.2.3.5; 1.21.1 рисует экраны ванильным шрифтом |
| `thaumcraft/common/config/MaterialAiry.java` | `net.minecraft.block.material.Material` в 1.21.1 отсутствует |
| `thaumcraft/client/fx/beams/FXBeamGolemBoss.java` | заменён снарядом `EntityGolemOrb` (см. дефект 5) |
| `thaumcraft/common/lib/network/fx/PacketFXBeamPulseGolemBoss.java` | вместе с лучом |

Помимо этого, из `EventHandlerEntity` / `EventHandlerWorld` /
`ServerTickEventsFML` утрачены: защита владения големом, эндер-жемчуг против
warded-объекта, очередь регенерации чанков, правило «волшебное бревно горит
400 тиков» и запрет постановки блока у босса (дефекты 1, 2, 5–8).
Перенесено иным механизмом, а не утрачено: NBT-тег «бросивший» (теперь —
ванильный `ItemEntity.getOwner()`) и отмена подбора фейковым игроком (фейковый
игрок в native не создаётся).

## Что я НЕ смог проверить

* **Клиентскую графику.** `GuiGolem`, `RenderGolemBase`, `RenderGolemBobber`,
  `ModelGolem`, `ModelGolemAccessories` сверены только чтением кода: соответствие
  текстур, геометрии и анимации игровому виду не подтверждено (клиент в этой
  среде не запускался). Для всех клиентских строк стоит `partial`.
* **Поведение в игре.** Ни один из дефектов не воспроизводился запуском: сборка
  и GameTest в этом аудите не выполнялись (аудит — чтение). Дефекты 1, 2, 5, 6,
  7, 8 выведены из отсутствия кода в native и из чтения оригинала; дефект 9
  следует из отсутствия целого механизма.
* **Полный перечень опций `Config`.** Сопоставление шло по ~90 полям оригинала;
  часть опций может влиять на код вне моего домена (например, `Config.golemLinkQuality`
  — «качество связи» колокола, 16). Проверить, что нативная реализация связи
  колокола использует эквивалент 16, я не успел.
* **Пакеты вне моего домена.** `PacketFXBlockSparkle`, `PacketFXInfusionSource`,
  `PacketFXShield`, `PacketFXWispZap`, `PacketBoreDig`, `PacketFocusChangeToServer`,
  `PacketRunicCharge`, `PacketScannedToServer`, `PacketSyncResearch`,
  `PacketSyncScanned*`, `PacketSyncWarp`, `PacketWarpMessage`,
  `PacketResearchComplete` в моём worklist не входили — их наличие в native я
  проверял только косвенно (по спискам payload-классов).
* **`EventHandlerRunic`** в мой worklist не входит (он отнесён к другому
  семейству). Я проверил только то, что рунический слой в native существует
  (`common/src/main/java/thaumcraft/common/lib/runic/RunicRules.java`,
  `RunicArmor.java`, `RunicShield.java`), но построчно его не сверял.
* **Точная идентичность MCP-имён `DamageSource.field_76367_g`**
  в 1.7.10 установить не удалось (в oracle используется старая MCP-схема имён).
  Дефект 5 сформулирован через «общий источник урона голема и сундука»;
  конкретное имя требует сверки с ремапнутым 1.7.10.
* **NeoForge-часть.** Проверялся только common-код и Fabric-bootstrap; NeoForge
  находится в состоянии «deferred» по политике проекта.

## Артефакты

* Ревью ledger: `docs/porting/audit-reviews-a7-golems.json` (135 записей,
  `reviewed_sha256` совпадают с worklist).
* Настоящий отчёт: `docs/analysis/2026-10-21-audit-a7-golems.md`.
