# Улучшения големов, группы с первой по пятую — нативная реализация 19.10.2026

Контракт оригинала: [golem-upgrades](2026-10-19-golem-upgrades.md). Fabric/common; NeoForge не запускался и не собирался.
Статусы разделены: **implemented / built / game-tested / seen**. «Seen» (визуальный/клиентский проход) для обеих групп **нет**.
Первая группа — Air/Earth(сила, урон)/Entropy (разделы ниже до «Вторая группа»); вторая — вместимость Earth и радиус Water с предметом Water (раздел «Вторая группа»).
Таблица и список эффектов первой группы ниже описывают состояние на её момент; актуальное состояние Earth и Water — во второй группе.

## Что реализовано

| Часть | Где | Суть |
|---|---|---|
| Типы | `GolemUpgrade` | id оригинала 0..5 (Air, Earth, Fire, Water, Order, Entropy). `isSupported` только у **Air, Earth, Entropy** — у остальных нет ни одного готового эффекта |
| Правила и арифметика | `GolemUpgrades` (чистый Java) | слот установки, лимит «не более двух одного типа», канонический список, `byte[]` NBT, упакованный int синхронизации, скорость, сила, рукопашный урон, шипы, возврат при разборке |
| Предмет | `ItemGolemUpgrade` + `ThaumcraftGolems.UPGRADE_AIR/EARTH/ENTROPY` | `golem_upgrade_air/earth/entropy`, стек 64, `uncommon`, вкладка TC, подсказка оригинала + честная строка «что уже перенесено» |
| Рецепты | `data/.../recipe/arcane/golem_upgrade_{air,earth,entropy}.json` | arcane shaped `NNN/NCN/NNN`, N = `gold_nugget`, C = осколок **своего** типа, vis 10 (`aer`/`terra`/`perditio`), ключи исследований `UPGRADEAIR/EARTH/ENTROPY` |
| Книга/иконка | `ThaumcraftRecipeDisplay` (алиасы `UpgradeAir/Earth/Entropy`), `ResearchIconDisplay` | страницы рецептов и иконки исследований; Fire/Water/Order остаются без страницы |
| Данные плейсера | `GolemData` (7-й компонент `upgrades`) | список слотов в порядке, `-1` пусто, хвостовые пустые срезаются; в JSON/NBT поле **опущено**, если пусто (старые предметы не меняются); сетевой кодек: счётчик + байты; неподдержанные id и более 3 слотов отвергаются |
| Голем | `EntityGolemBase` | `int[] upgradeSlots` по числу слотов материала, синхронизированный `UPGRADES`; установка ПКМ без приседания (после ядра, до печенья/меню); отдача в `configuredData()`, приём в `applyConfiguration()`; NBT `upgrades` (byte[], как в оригинале); шипы в `hurt`; возврат при разборке колокольчиком |
| Плейсер | `ItemGolemPlacer` | конфигурация с большим числом улучшений, чем слотов материала, недействительна (оригинал молча обрезал); улучшения перечислены синим в подсказке |
| Тексты | `en_us.json`, `ru_ru.json` | имена и подсказки (ключи `item.thaumcraft.golem_upgrade_*`, `tooltip.thaumcraft.golem.upgrade_*`) |

### Эффекты (только они)

- **Earth:** `getGolemStrength() = material.strength + n` — его уже читают Harvest (`HarvestGolemGoals`), Lumber (`LumberGolemGoals`), Fishing (`FishingGolemGoals`); `getMeleeDamage() = 2 + (strength + n) + n`
  (Earth дважды, как оригинал E:255), им пользуется удар Guard/Butcher (`doHurtTarget`) и атрибут `ATTACK_DAMAGE`.
- **Air:** базовая скорость `MOVEMENT_SPEED = material.speed × (1 + 0.15 n)` в float, как E:376 (скорость в воде, декорации и «advanced» в порту отсутствуют).
- **Entropy:** до обычного урона тому, что **напрямую** ударило голема (`getDirectEntity`, как `getSourceOfDamage` оригинала — стрела принимает шипы, не стрелок), наносится `2n + nextInt(2n)` уроном `thorns` и звучит `THORNS_HIT`.

### Решения и отклонения (все документированы здесь)

1. **Нет «инертных» типов.** Предметы Fire/Water/Order не зарегистрированы, у их id нет ни рецепта, ни страницы; NBT/плейсер с ними — неподдержанные данные (retained, голем простаивает), не пустой голем.
   Принцип уже был записан в `2026-10-18-straw-golem-original.md`.
2. **Оплата в творческом режиме не берётся** — то же решение порта, что у установки ядра (оригинал списывает безусловно).
3. **Рекурсия шипов.** В оригинале два Entropy-голема, бьющие друг друга, уходят в бесконечную рекурсию; отражённый удар здесь не отражается снова (флаг на сущности).
4. **Никакого молчаливого усечения.** Оригинал при загрузке/размещении обрезает массив до числа слотов; порт отказывается (плейсер — недействителен, сущность — `unsupported` с сохранением исходных байтов). Короткий массив дополняется `-1`, как в оригинале.
5. **Синхронизация клиента** — упакованный int (4 бита на слот), а не шестнадцатеричная строка оригинала: `Integer.toHexString((byte)-1)` даёт `ffffffff` и сбивает чтение слота после пустого.
6. **Вместимость Earth в первой группе не переносилась** (`getCarryLimit()` оставалось материальным, иначе Alchemy/Decanting/плейсер разошлись бы в проверке ёмкости);
   она перенесена одним согласованным изменением во второй группе — см. раздел «Вторая группа» ниже.

## Проверка

- **Контракт:** [golem-upgrades](2026-10-19-golem-upgrades.md), до кода.
- **Built.** `golem-upgrades-build1` и финальная `golem-upgrades-build2` (`.tmp/axe-stream-build.ps1`): `:common:test` целиком **332 JUnit, 0 failures/errors/skips**
  (309 прежних + 23 новых: `GolemUpgradesTest` 15, `GolemDataUpgradesTest` 8) + `:fabric:remapJar` + `verifyTestIsolation`/`verifyProductionIsolation`, `BUILD SUCCESSFUL` (43 с / 15 с), exit 0.
  Fabric JAR `fabric/build/libs/Thaumcraft-fabric-0.1.0-alpha.1.jar` **11230275 bytes, SHA256 `4EC1994A40D87FC3D64CD9BD99FFDA74DB8D77ACE3F3820C319991DBC6950929`**
  (одинаков в build1 и build2, то есть мутационные правки восстановлены побайтно; три новых класса, три модели, три рецепта в нём есть, `GolemUpgradeRuntimeChecks` в продакшн-JAR не попал).
- **Game-tested (Fabric dedicated-server GameTest).** `golem-upgrades-gametest1` и финальный `golem-upgrades-gametest2` (`.tmp/axe-stream-gametest.ps1`): `BUILD SUCCESSFUL`, exit 0,
  **143/143** (прежние 142 + новый `golemUpgradesInstallPersistAndApplyEarthAirEntropy`), 0 failures/errors. Новый тест (`GolemUpgradeRuntimeChecks`, настоящий подключённый `ServerPlayer`,
  настоящие голем/свинья как входы; установка, оплата, сохранение, загрузка, размещение, возврат, шипы не подставлены):
  1. три рецепта (исследование, узор, ключи, vis 10, результат), стек 64/uncommon, у Fire/Water/Order нет ни предмета, ни рецепта;
  2. установка настоящим `interact`: первый свободный слот, оплата одной штуки, два Earth в Tallow, отказ без слота (PASS, без оплаты), приседание не ставит, креатив не платит, голем с ядром Lumber принимает улучшение и сохраняет ядро;
  3. эффекты: Tallow 0/1/2 Earth → сила 2/3/4, урон 4/6/8, атрибут урона, `LumberGeometry.delay` 28→16; Iron+Earth → сила 5, урон 8, задержка 10; Straw+Air → скорость 0.38×1.15 при неизменных силе/здоровье/броне/вместимости;
  4. шипы: без Entropy ничего, Entropy×1 → 2 или 3 (оба значения видны за 40 ударов), Entropy×2 → 4..7, без источника и по себе — ничего, два Entropy-голема бьют друг друга по разу без рекурсии;
  5. сохранение: NBT `upgrades` = `[1,0]` (byte[]), перезагрузка сохраняет слоты и пересчитывает силу/урон/скорость, короткий массив дополняется, отсутствие тега = пусто, массивы `[1,1,0]`, `[2,-1]`, `[1,7]` и int-массив = неподдержанные данные с побайтным сохранением;
  6. плейсер: Iron-плейсер с двумя улучшениями отказывает и остаётся; Tallow-плейсер ставит голема с улучшениями и эффектами; обычный колокольчик возвращает плейсер с обоими улучшениями в `GolemData`; разборка приседая — голый плейсер и монетка только по занятым слотам по порядку (подобрано зерно генератора «оставить, потерять»: вернулся Earth, Air потерян).
- **Мутационные проверки (разово, затем восстановлено):** (A) шипы отключены → тест упал на «one Entropy reflects 2 + nextInt(2) = 2 or 3, saw 0.0»; (B) монетка разборки всегда «оставить» → упал на «first coin kept the Earth upgrade, the second lost the Air upgrade».
  Значит предшествующие этапы действительно исполнялись. Остальные утверждения — конкретные значения, пустыми пройти не могут; мутации на них по отдельности не делались.

### Не проверено (не заявляется)

Реальный бой Guard/Butcher с Earth-уроном (формула и значение в `getMeleeDamage()` проверены, сам удар `doHurtTarget` нет); измерение скорости работы Harvest/Lumber/Fishing в мире с Earth (проверена только живая сила и чистая задержка);
ходьба Air-голема (проверен базовый атрибут, не реальный путь); клиентская синхронизация упакованного int, подсказки, рецепты и иконки в книге (клиент не запускался); иконки улучшений на теле голема (рендер не перенесён);
сеть, два игрока, настоящие protection-моды; реальный мир save/reload (проверен NBT-цикл на сущности без чанка); вкладка творчества; NeoForge.

## Вторая группа: вместимость Earth и радиус Water (19.10.2026)

Контракт — разделы «Earth / Вместимость» и «Water» файла [golem-upgrades](2026-10-19-golem-upgrades.md); значения оригинала не менялись. Fabric/common, NeoForge не запускался.

| Часть | Где | Суть |
|---|---|---|
| Формулы | `GolemUpgrades.carry/fluidLimitMb/range/workRadius` (чистая арифметика) | `carry = base + min(16, max(4, base))·Earth` (E:355-362); вода `floor(sqrt(carry))·1000` (E:364-366); `range = 16 + 4·Water` (E:460-469 без декорации G и «advanced», которых нет); `radius = ceil(range / 4 \| 3 \| 2)` (Harvest / Lumber / Fishing) |
| Груз | `GolemWaterCargo.capacity/validate(material, earth)`, `GolemAspectCargo.capacity/validate(material, earth)` | заменили `capacityForMaterial/validateMaterial(material)`; потолок полезной нагрузки поднят **5000→8000 мБ** и **32→64** — это наибольшая достижимая ёмкость (Thaumium + 2 Earth), равенство закреплено JUnit |
| Сущность | `EntityGolemBase`: `getCarryLimit()` (+Earth), новый `getFluidCarryLimit()`; `waterSpace`, `liquidReady`, `alchemyReady`, `applyWaterCargo`, `applyAspectCargo`, `applyAspectCargoRaw`, `readWaterCargo`, `readAspectCargo`; `GolemHarvestPlatform` (кредит воды) | везде берут Earth **самого голема** (`getUpgradeAmount(EARTH)`), порядок чтения NBT: слоты улучшений раньше груза |
| Плейсер | `ItemGolemPlacer.configuration` | груз валидируется по материалу плейсера и Earth из его `GolemData.upgrades` |
| Предметы / рендер | `GolemUsePlatform` (ёмкость руки = `worker.getCarryLimit()`), `RenderGolemBase.essentiaCapacity` (= `getCarryLimit()`, как оригинальный RenderGolemBase:204) | |
| Радиус | `getRange()` = 16 + 4n, `rangeSqr()` (float, как `dmod*dmod`); живой радиус вместо константы 256 в `isGuardTarget`, `butcherState`, `eligibleItem` (Gather), `markedInventory`, `markedSortingInventory`, `UseGolemGoals.markerLoaded`; Harvest/Lumber/Fishing: метод `radius()` вместо `final`-поля, Harvest пересобирает список колонок при смене радиуса | |
| Предмет Water | `GolemUpgrade.WATER ported=true`, `ThaumcraftGolems.UPGRADE_WATER`, `upgradeItem(WATER)`, вкладка TC, модель, `data/.../recipe/arcane/golem_upgrade_water.json` (8 `gold_nugget` вокруг `water_shard`, vis `aqua` 10, исследование `UPGRADEWATER`), алиас `UpgradeWater` в `ThaumcraftRecipeDisplay`, `ResearchIconDisplay` damage 3, lang en/ru | предмет зарегистрирован **после** живого радиуса |

### Решения и отклонения второй группы

1. **Custody.** Новая ёмкость не может потерять груз: разборка колокольчиком уже отказывает, пока в големе есть вода или эссенция (`recall(dismantle)`), так что Earth не теряется вместе с грузом сверх базового лимита;
   обычный удар сохраняет Earth и груз в плейсере. Плейсер с грузом, которому не хватает его же Earth, недействителен (красная подсказка, не размещается, не расходуется); NBT сущности, где Earth меньше нужного,
   делает голема `unsupported`, сырой груз сохраняется побайтно (как любые непринимаемые данные), а не обрезается.
2. **Потолки полезной нагрузки поднялись (8000 мБ / 64)**; сохранения со значениями ≤ 5000 / ≤ 32 остаются валидными без изменений.
3. **Use.** Оригинальный `AIUseItem` вычисляет `ceil(range/3)` и не использует его; граница вокруг дома 16 в порту — собственная. Она теперь равна живому радиусу (при Water 0 поведение прежнее).
4. **Gather** (`eligibleItem`, оригинал `AIItemPickup:43`) тоже переведён на живой радиус: прямоугольник поиска уже читал `getRange()`, а проверка предмета держала 256 и расходилась бы с ним.
5. **Цели не пересоздаются** при установке улучшения (оригинальный `setupGolem()` строит их заново): радиусы читаются на лету, Harvest перестраивает колонки по смене радиуса, идущий заброс Fishing остаётся допустимым (радиус только растёт).
6. **Нет** декорации G / «advanced» (+10 % / +20 %), дальних атак с разбросом `7 − 1.75n` (декорация R); подсказка предмета честно перечисляет перенесённое («радиус работы») и нет («точность дальних атак»).

### Проверка второй группы

- **Контракт** — до кода (раздел выше и [golem-upgrades](2026-10-19-golem-upgrades.md)).
- **Built.** `golem-capacity-range-build1` (`.tmp/axe-stream-build.ps1`): `:common:test` целиком **339 JUnit, 0 failures/errors/skips** (332 + 7 новых в `GolemUpgradesTest`: таблица ёмкости по восьми материалам × 0..2 Earth,
  вода `floor(sqrt)`, эссенция, валидация груза «материал + Earth», потолки равны максимуму достижимой ёмкости, `range`/радиусы Harvest-Lumber-Fishing, Water ставится как любой тип),
  `:fabric:remapJar` + `verifyProductionIsolation`, BUILD SUCCESSFUL 44 с, exit 0. Fabric JAR 11232053 bytes, SHA256 `3f32d7aa00021fa22964ec2ddb1b1d2ef7fcb13ff8af948b52a7eb02191ee688`; в нём модель, текстура и рецепт Water;
  тестовые классы (`GolemWaterRangeRuntimeChecks`, `GolemUpgradeRuntimeChecks`) в нём отсутствуют.
- **Game-tested** (`:fabric:runGameTest`, `.tmp/axe-stream-gametest.ps1`). `golem-capacity-range-gametest1`: exit 0, **144/144** (143 + новый), 0 failures/errors.
  - расширенный `golemUpgradesInstallPersistAndApplyEarthAirEntropy` (настоящий `ServerPlayer`, настоящие големы/пауки): рецепт Water; Tallow 0/1/2 Earth → ёмкость 8/16/24 предметов и 2000/4000/4000 мБ, ещё 10 строк материал × Earth
    (Straw+Earth 5/2000, Iron+Earth 48/6000, Thaumium+2 Earth 64/8000); плейсер Thaumium Decanting: 6000 мБ без Earth и 8000 мБ с одним Earth — **отказ**, 5000/6000/8000 при 0/1/2 Earth — размещён;
    круг «плейсер → голем → NBT → колокольчик → плейсер → голем» с 8000 мБ и двумя Earth; разборка с водой/эссенцией отказывает; NBT с одним Earth вместо двух оставляет 8000 мБ сырыми байтами; то же для эссенции (33 без Earth — отказ, 64 при двух Earth, плейсер возвращает 64);
    настоящий Guard-голем и шесть настоящих пауков на расстоянии 15/16/19/20/23/24: цели при диапазоне 16/20/24 — строго внутри сферы (16 вне при Water 0, 20 вне при Water 1, 24 вне при Water 2); установка Water настоящим `interact` и оплата; сохранение (`upgrades` = `[3,3]`) и плейсер;
  - **новый** `waterUpgradeWidensTheLiveWorkRadiusOfRunningLumberAndHarvestGolems` (`GolemWaterRangeRuntimeChecks`): настоящие Lumber- и Harvest-голем (Iron), ствол на 7 блоках (радиус 6) и спелый арбуз на 5 (радиус 4); за 640 тиков (полный цикл Harvest при радиусе 5 — 605) их не тронули;
    после установки настоящим игроком Water на уже идущие цели Lumber срубил ровно один верхний блок ствола, Harvest сломал арбуз с выпавшими дольками.
- **Финальные прогоны на восстановленном коде.** `golem-capacity-range-build2`: exit 0, `verifyProductionIsolation` PASS, JAR **байт-в-байт тот же** (SHA256 `3f32d7aa…e688`, 11232053 bytes, то есть все мутационные правки откачены точно;
  :common:test взят из кэша Gradle по тем же входам, 339 JUnit без падений); `golem-capacity-range-gametest2`: exit 0, **144/144**, 0 failures/errors.
- **Мутационные проверки (разово, затем восстановлено; каждая — полный `:fabric:runGameTest`, JUnit в них не запускался):**
  (A) `GolemUpgrades.carry` без Earth (`golem-capacity-range-mutation-ab`) → расширенный `golemUpgrades…` упал на «one Earth: … capacity 8 + 8»;
  (B) радиусы Lumber и Harvest зафиксированы на 16 — как было бы со старым `final`-полем, посчитанным в конструкторе (тот же прогон) → новый `waterUpgradeWidens…` упал по дедлайну:
  «installed=true, trunk logs left=5, melon=Block{minecraft:melon}, ranges 20.0/20.0» (диапазон 20 есть, а голем его не использует);
  (C) `isGuardTarget` снова с константой 256 (`…-mutation-c`) → упал на «one Water: range 20, spiders up to 19 are targets, 20 is out: [true, false, false, false, false, false]»;
  (D) `GolemWaterCargo.capacity` без Earth (`…-mutation-d`) → упал на «6000 mB with 1 Earth: placement FAIL, expected CONSUME»;
  (E) `GolemAspectCargo.capacity` без Earth (`…-mutation-e`) → упал на «48 essentia with 1 Earth: placement FAIL, expected CONSUME».
  Значит ёмкость, граница Guard, живой радиус целей и проверка груза по Earth действительно исполнялись. Отдельных мутаций на остальные утверждения (NBT-проверка с одним Earth, запрет разборки с грузом, запись `upgrades=[3,3]`, плейсер Water) не делалось —
  там конкретные значения, которые пустыми пройти не могут.

### Не проверено (не заявляется)

Настоящий Decanting с набором воды до повышенного лимита и перенос 64 эссенции Alchemy (проверены границы лимита, размещение, NBT, колокольчик и запрет разборки, не сами циклы работы); реально несомые >базовой ёмкости предметы (Gather/Fill/Empty);
Fishing с живым радиусом на настоящем пруду (только арифметика `workRadius`); Butcher/Sorting/Fill/Empty/Gather/Liquid/Alchemy-маркеры и поиск при радиусе 20/24 (проверен Guard и Lumber/Harvest; остальные читают тот же `getRange()`/`rangeSqr()`, но отдельными проверками не покрыты);
клиент: рендер банки эссенции по новой ёмкости, подсказки, страница и иконка Water в книге, модель предмета; сеть, два игрока, реальный мир save/reload, NeoForge.

## Третья группа: Fire и Order (19.10.2026)

Контракт — §5a файла [golem-upgrades](2026-10-19-golem-upgrades.md). Fabric/common; NeoForge не запускался.

| Часть | Где | Суть |
|---|---|---|
| Fire | `EntityGolemBase.doHurtTarget`, `GolemUpgrades.igniteSeconds` | жертва поджигается на `4n` с только после **удавшегося** удара Guard/Butcher (оригинал E:1120–1122) |
| Order | `GolemHarvestPlatform.replant`, `GolemReplant` | после реального сбора: предметы в коробке ±6 вокруг голема, свежие дропы толкаются к голему, какао снова растёт у джунглевого бревна, семена (пшеница/свёкла/арбуз/тыква/морковь/картофель/адский нарост) в девять клеток под колонкой; платёж одним настоящим семенем после `setBlock`; цепочка разрешений `mayInteract` → Architectury `BlockEvent.PLACE` → Fabric `UseBlockCallback` (`Bridge.allowsPlacement`) |
| Предметы | `ThaumcraftGolems`, рецепты, модели, lang, алиасы, иконки | все шесть типов `GolemUpgrade` теперь поддержаны |

### Исправление, найденное при проверке: свет в клетке убранного непрозрачного блока

Первый прогон GameTest упал: пшеница не росла в клетке 0 (там, где стоял арбуз). Установлено: движок освещения 1.21 асинхронный,
и сразу после уборки **непрозрачного** блока клетка читается как тёмная (`raw=0`, `sky=false`), поэтому `CropBlock.canSurvive` отказывает.
В оригинале 1.7.10 свет обновлялся сразу. Два следствия и исправление в `GolemHarvestPlatform`:

1. `survives()` — если правила блока отказывают только из-за света (культура или стебель, ферма ниже, клетка пуста), свет **предсказывается по шести соседям**
   (каждый теряет 1, а небо 15 сверху — нет); тёмный погреб по-прежнему отказывает. Нормальный свет по-прежнему идёт штатным `canSurvive`.
2. `replant()` в два прохода: сначала клетки, которые движок уже принимает (исходный порядок «предмет → клетка»), затем клетки только с предсказанным светом.
   Иначе посадка соседа (с обновлением форм) находила только что посаженную пшеницу «тёмной» и **удаляла** её вместе с семенем.
   Отклонение: порядок посадки меняется только для клетки, где был непрозрачный блок; при устоявшемся свете проход 0 — исходный цикл.

Побочно исправлены три проблемы теста `GolemFireOrderRuntimeChecks`: (а) паук создавался через повёрнутый `relativePos` и оказывался в 226 блоках от Guard-голема — теперь абсолютные координаты;
(б) порядок событий допускает оба варианта (исходный и «клетка убранного блока последней»); (в) направление толчка дропов зависит от случайной геометрии — в рантайме проверяются подъём и предел сдвига,
направление остаётся за `GolemReplantTest`.

### Проверка третьей группы

- **Built** (`fire-order-final-build`): `:common:test` **346 JUnit, 0 failures/errors/skips**, `remapJar`, `verifyProductionIsolation`, exit 0, JAR SHA256 `30e0c30f8c32214eb4fcb72c00bad55ece5342d4896da11f449af4a84ffdf99e`.
- **Game-tested** (`fire-order-final-gametest`): exit 0, **145/145** (144 прежних + `fireIgnitesGuardTargetsAndOrderReplantsHarvestedCrops`, 8 параллельных полос на настоящих игроке, големах и пауках: Fire и Order ставятся и сохраняются `[2,4]`;
  Guard с Fire поджигает паука на 4 с после первого удавшегося удара, без Fire не поджигает; Harvest+Order сажает три клетки с фермой по одному семени, пропускает клетки без фермы, учитывает запреты Architectury и Fabric без траты семян, контроль без Order не сажает, какао).
- **Мутации** (разово, затем восстановлено побайтно): поджог Fire отключён → упал на «паук горит 4 с…: −1 тик»; предсказанный свет отключён → упал на «пшеница не посажена в клетке 0».
- **Не проверено:** клиент, книга, рендер, сеть; реальные protection-моды; Fire у Butcher и в PvP; ночное/подземное освещение (только дневная поверхность); жарка улова Fishing; NeoForge.

## Четвёртая группа: слоты 6+6n, жарка улова Fishing, цвета Order (19.10.2026)

Контракт — §5a файла [golem-upgrades](2026-10-19-golem-upgrades.md). Fabric/common; NeoForge не запускался. Ничего не закоммичено.

| Часть | Где | Суть |
|---|---|---|
| Слоты `6+6n` | `GolemData` (`slotCount`, `fireCount`, `hasItemGhosts`), `EntityGolemBase` (`getSlotCount`, `defineGhosts`, границы, NBT), `InventoryGolemGoals`, `ContainerGolem`, `GuiGolem` | Fill/Empty/Gather/Use: `6 · (1 + Fire)` слотов, максимум 18 (два Fire; в порту максимум двух одного вида). Списки `filters`/`wantedCounts`/`colors` имеют размер 6/12/18; список из шести дополняется пустыми (старые плейсеры и сохранения), другой размер — ошибка. Меню листает страницы по шесть, кнопки 60/61; клик по призраку пишет слот `страница · 6 + индекс` |
| Жарка Fishing | `GolemFishingSmelting`, одна строка в `FishingGolemGoals.activeTick` | с Fire улов заменяется результатом зарегистрированного рецепта плавки (`RecipeType.SMELTING`; в 1.21 вместо `FurnaceRecipes`), иначе остаётся как есть; результат не умножается на размер улова |
| Цвета маркеров | `GolemMarker` (`color`, `click`, `accepts`, `sameSpot`), `ItemGolemBell`, `GolemData.markers` | −1 «любой», 0–15; цвет участвует в равенстве; колокольчик крутит цикл при Order |
| Цвета слотов | `GolemData.colors`, `EntityGolemBase` (`getSlotColor`, `setSlotColor`, `colorsMatching`, `markerAccepts`), `ContainerGolem` (кнопки 100–105, данные меню 17), `GuiGolem` | цвет слота у Fill/Empty/Use при Order; `colorsMatching` повторяет оригинальный `getColorsMatching` (слоты, чей образец совпал; пустая рука — пустые слоты; нет ни одного образца — цвета всех слотов) |
| Маршрутизация | `InventoryGolemGoals` (`fillRequest`-ветка забора, `nearestSupply`, `inventoryAccepts`), `EntityGolemBase` (`takeFromInventory`, `placeCarriedAtMarker`, `markedWorldThrowTarget`), `UseGolemGoals` (`findTarget`, `current`) | `markerAccepts(marker, stack)`: слот −1 принимает любой маркер, иначе цвет совпадает точно (`Marker.equalsFuzzy`). Sorting/Gather/Liquid цвет не читают (в оригинале Sorting опрашивает цвет −1) |

### Отклонения (документированы)

1. **Liquid** остаётся с одной страницей: единственная его жидкость — вода, оригинальные `1 + n` слотов были бы дублями. Остальные ядра без призрачных слотов Fire не меняет.
2. **Огонь улова:** оригинал после плавки поджигал выданный предмет на 2 с (AIFish:124-130). Это визуальный огонь, а улов лежит в долговечной очереди (`DeferredItemDrops.enqueueGolemFishingCatch`), формат которой разделяют другие хранители, — поля не добавлено.
3. **Цвет без Order — неподдерживаемые данные:** в плейсере (`GolemData`) это ошибка, в NBT голем становится неподдерживаемым и хранит конфигурацию как есть; цвет не отбрасывается молча. Нечёткое сопоставление предметов (Entropy) не перенесено, поэтому `colorsMatching` сравнивает точно.
4. **Колокольчик:** маркеры одной точки (`sameSpot`: измерение + блок + грань, без цвета) считаются одним, чтобы остаточный цвет не создавал дубль и клик его удалял/перекрашивал; оригинальное `Marker.equals` сравнивает и цвет. Перекрашенный маркер идёт в конец списка (порядок первого подходящего маркера — как remove + add оригинала). Текст action bar и значки цвета в меню — собственные.
5. **Формат:** NBT `colors` (ключ оригинала, массив байт; опускается, если все −1), JSON `colors` и `color` маркера опускаются при значении по умолчанию, поэтому все прежние предметы и сохранения сериализуются как раньше. Сетевой кодек `GolemData` получил байт цвета на маркер и список цветов слотов; кодек плейсера принимает размеры 6/12/18 и точные целые.
6. **Найденный дефект:** `GolemData.equals` сравнивал только первые шесть фильтров и не учитывал `colors`, поэтому тесты кругов (`assertEquals`) не видели потерь на страницах и цветов. Исправлен (все страницы, цвета слотов, цвета маркеров; `hashCode` тоже), добавлен тест, мутации ловятся.

### Проверка четвёртой группы

- **Built** (`colors-final-build`): `:common:test` **368 JUnit, 0 failures/errors/skips** (+22: `GolemDataGhostSlotsTest` 8, `GolemColorsTest` 14), `remapJar`, `verifyProductionIsolation`, exit 0; JAR 11249813 bytes, SHA256 `badcf4a03638c77b654ed5eed95b467727f2f2773da4fd260d3d198908992554`.
- **Game-tested** (`colors-final-gametest`): exit 0, **148/148** (145 + 3). Настоящий игрок через `placeNewPlayer` с `EmbeddedChannel`, настоящие големы Tallow, настоящие `ContainerGolem` и `ItemGolemBell.useOn`:
  - `fireUpgradesAddPagesOfGhostSlotsToItemFilterGolems` — установка Fire игроком, страницы 6/12/18, клики меню, NBT/плейсер/legacy из шести/запись за пределами страниц, настоящий Gather-голем берёт предмет со второй страницы;
  - `orderColoursRouteMarkersAndSlotsAndTheBellCyclesThem` — Order нужен для цвета слота, ядра Fill/Empty/Use; цикл меню 17 нажатий; вторая страница; `colorsMatching`/`markerAccepts`; колокольчик 18 кликов, перекраска в конец, приседание, голем без Order; NBT/плейсер/legacy/подделанные цвета и размер списка;
    **настоящий Fill-голем между тремя сундуками**: ближний сундук с маркером цвета 5 не тронут, предметы идут из дальнего сундука цвета 3;
  - `fireUpgradeCooksTheFishingGolemsCatch` — `GolemFishingSmelting.apply` на настоящем Fishing-големе с Fire и без, на живом менеджере рецептов сервера.
- **Мутации** (`.tmp/mutate_colors.py`, затем побайтно восстановлено, хэши сверены): JUnit — `accepts` всегда истина → `aSlotColourAcceptsAnyMarker…`; цикл колокольчика обрезан → `withOrderAClickCycles…`; Order не нужен слоту → `colouredSlotsNeedAnOrderUpgrade…`; `equals` без цветов и `equals` по первой странице → `equalityCoversSlotColours…` (две отдельные строки);
  кодек маркера не пишет цвет → `markerColoursRoundTrip…`; сеть не читает цвета → `coloursAndMarkersRoundTrip…`. GameTest (по одному): фильтры `markerAccepts` в забор/выбор источника Fill убраны → «ближний сундук не тронут: 56»; шаг цикла меню → «click 15 … (golem −1, menu −1)»; `apply` не смотрит на Fire → «голем без Fire отдаёт сырую треску»;
  `markerAccepts` всегда истина → «цветной слот берёт ровно свой цвет»; колокольчик без Order → «colour 0: golem []»; NBT не пишет `colors` → «NBT keeps twelve slot colours». Для слотов ранее: мутации числа слотов и смещения страницы меню роняли первый тест.
- **Seen:** нет — клиент, цветные значки и пейджер `GuiGolem` (`PAGER_Y` 66→72), action bar, книга не запускались.
- **Не проверено:** реальная рыбалка со снастью (в рантайме проведён шаг плавки; вызов в `activeTick` только компилируется); живой маршрут Empty/Use по цвету (Empty и Use проверены на уровне `markerAccepts`/`colorsMatching`, живой маршрут — только Fill); синхронизация данных меню на клиент по сети; два игрока;
  реальный save/reload мира (только NBT-круг в процессе); NeoForge.

## Пятая группа: переключатели целей Guard (Order) и улов Fishing (Order, Entropy, Air) (19.10.2026)

Контракт — §5a файла [golem-upgrades](2026-10-19-golem-upgrades.md) (E:800-831, 1150-1180, GuiGolem:167-189, ContainerGolem:92; AIFish:118-120, 183-184). Fabric/common; NeoForge не запускался. Ничего не закоммичено.

| Часть | Где | Суть |
|---|---|---|
| Правила переключателей | `GolemGuardTargets` (чистый Java) | маска из четырёх **положительных** битов `MONSTERS 1 / ANIMALS 2 / PLAYERS 4 / CREEPERS 8` (бит = «можно бить»), по умолчанию `MONSTERS`; кнопки меню сохраняют оригинальные id **51–54**; `configurable(core, order)` — переключатели есть только у Guard с Order; `toOriginalToggles/fromOriginalToggles` — соответствие инвертированному байту оригинала |
| Данные плейсера | `GolemData.guardTargets` (девятый компонент) | JSON `guard_targets` (0–15, опускается при умолчании), NBT плейсера, сетевой кодек — один байт в конце, шаблон проверок как у `colors`; не-умолчательная маска допустима только для ядра Guard с Order, иначе `IllegalArgumentException` / ошибка декодирования |
| Сущность | `EntityGolemBase`: `getGuardTargets/setGuardTargets/toggleGuardTarget/canSwitchGuardTargets`, `guardCategoryAllowed` (в `isGuardTarget`), NBT `GuardTargets` (byte) | NBT опускается при умолчании; маска без Guard+Order, значение вне 0–15 или не-байт — `unsupported` с сохранением сырых данных (ключ в `CONFIG_KEYS`); оригинальный `toggles != 0` по-прежнему `unsupported` |
| Меню / GUI | `ContainerGolem` (данные меню 17–18, `GUARD_BUTTON`), `GuiGolem` | кнопки 51–54 переключают категорию **только** у Guard с Order (сервер проверяет сам); четыре флажка на оригинальных позициях `(104, 5 + 16i)` с подписями Monsters/Animals/Players/Creepers (lang en/ru); без Order остаётся прежний текст |
| Плейсер / колокольчик | `ItemGolemPlacer` (строка «Атакует: …» при не-умолчательной маске), `configuredData()/applyConfiguration()` | обычный удар колокольчиком возвращает плейсер **с маской** (оригинальный плейсер её терял); разборка приседая — голый плейсер, как раньше |
| Улов Fishing | `GolemUpgrades.fishJunkBase/fishTreasureBase/fishCategory/fishCatches`, `GolemFishingLoot` (`Pond.junkChance(entropy)`, `treasureChance(order)`, `rollCatch(…, entropy, order)`, `nextCatch`, `catchCount`), `FishingGolemGoals.activeTick` | `f1 = 0,1 − 0,025·Entropy`, `f2 = 0,05 + 0,0125·Order` плюс прудовые добавки, как AIFish:183-184; Air: второй улов при `nextInt(10) < Air` (AIFish:118-120), жребий берётся только при наличии Air; `activeTick` для каждого улова катит результат, жарит (Fire), считает траекторию и публикует, и только потом катит следующий |

### Решения и отклонения пятой группы

1. **Безопасный умолчательный режим (отклонение).** В оригинале все четыре бита `toggles[1..4]` по умолчанию сброшены, а бит *запрещает* категорию, поэтому свежий Order-Guard бьёт животных, игроков и криперов, пока владелец не выключит их в GUI. Порт **не копирует**
   небезопасное умолчание: по умолчанию включены только враждебные монстры (поведение Guard без Order); Animals, Players и Creepers — выбор владельца, и установка Order сама ничего не включает. Monsters по умолчанию включён, как в оригинале, и отключается только при Order.
2. **Положительная маска и свой ключ.** Оригинальный байт `toggles` хранит *отрицательные* биты и делится с Fill (бит 0), Use (0–2) и Entropy (5–7). Порт хранит положительную маску под своими именами (`GuardTargets` в NBT, `guard_targets` в JSON), чтобы тот же ключ не означал обратное;
   `GolemGuardTargets.fromOriginalToggles/toOriginalToggles` фиксируют соответствие в JUnit (оригинальный «все разрешены» = маска 15, умолчание порта = маска 1). Непустой оригинальный `toggles` остаётся неподдерживаемыми данными (переноса миров нет), а отсутствие ключа читается как безопасное умолчание.
3. **Сервер проверяет наличие переключателей.** Оригинальный `ContainerGolem` принимал кнопки 50–57 от любого меню; здесь кнопка 51–54 меняет маску только если и меню открыто для Guard, и у голема есть Order (иначе `false`, ничего не меняется) — подделанный пакет не включает категории.
4. **Категории в современных терминах.** Monsters — `Enemy` без `Creeper` (как раньше); Creepers — `Creeper`; Animals — `Animal` и `WaterAnimal` (аналог `EntityAnimal`/`IAnimals`: добавлены кальмар и прочие водные), кроме `Enemy`, `AbstractGolem`, жителей, летучих мышей (оригинальный `canAttackClass`),
   приручённых и «владеемых» (`OwnableEntity` с владельцем, шире оригинального `isTamed`), союзников по команде; Players — `Player`. Радиус остаётся строгой сферой вокруг дома и для новых категорий (проверено на корове в 20 блоках).
5. **Игроки (новые защитные условия).** Цель-игрок — не владелец (по имени, как в оригинале `getOwnerName()`), **не в творческом режиме и не наблюдатель** (такого игрока нельзя ранить, голем гонялся бы за ним вечно) и только пока сервер разрешает **PvP** (`server.isPvpAllowed()`).
   Это не из оригинала: в 1.7.10 такого серверного правила для мобов не было, а голем игрока обходил бы запрет PvP.
6. **Маска не теряется** при размещении и возврате колокольчиком (в оригинале плейсер хранил только улучшения), по той же причине, что `FillAny`, цвета и маркеры: настройка владельца не должна исчезать молча. Новая маска на новом големе всё равно начинается с умолчания, если плейсер её не нёс.
7. **Fishing: разделение улова.** Каждый улов проходит «ролл → плавка → траектория → публикация» и **только после публикации** катится следующий, поэтому сбой публикации не оставляет уже брошенный, но не принадлежащий никому предмет (ограничение: при сбое первой публикации бонусный улов просто не выпадает; сам первый хранится и повторяется как раньше).
   Оригинал создавал оба предмета в одном тике — здесь тоже, второй публикуется сразу за первым.
8. **Float-арифметика.** Базы `0,1f − Entropy·0,025f` и `0,05f + Order·0,0125f` — как в оригинале; прудовые добавки по-прежнему агрегированы, а не вычитаются по сторонам по очереди (расхождение порядка округления ≤ 1e-7; без улучшений значения побитно прежние).
9. **Горящий предмет улова** по-прежнему не перенесён (четвёртая группа); Fire-плавка теперь исполняется в реальной рыбалке (см. проверку).

### Проверка пятой группы

- **Built** (`guard-fish-final-build`, `.tmp/axe-stream-build.ps1`): `:common:test` целиком **384 JUnit, 0 failures/errors/skips** (368 + 16: `GolemGuardTargetsTest` 11, `GolemFishingUpgradesTest` 5), `:fabric:remapJar` + `verifyTestIsolation`/`verifyProductionIsolation`, BUILD SUCCESSFUL, exit 0.
  Fabric JAR 11256400 bytes, **SHA256 `f1112abe0754f62c2588f249e8cb55d96474050e1b36887bef8bb4b0dbb0d6b5`**; тестовых классов (`*RuntimeChecks`) в нём нет. (Предыдущая сборка `guard-fish-build5` отличалась одной строкой javadoc: 11256401 bytes, `b01eb793…b721b73`.)
  Два прежних теста потребовали правки раскладки/порядка, а не поведения: `GolemColorsTest` и `GolemDataUpgradesTest` склеивали испорченные сетевые потоки по смещению от конца буфера — теперь в конце лежит байт маски; `GolemDataUpgradesTest`
  создавал `ItemStack.EMPTY` в статическом поле до регистрации реестров, и Gradle (который запускает упавший класс первым) ронял весь JVM лавиной `ExceptionInInitializerError` — добавлен статический блок `Bootstrap` перед полем. Старая склейка проходила «случайно» (любое исключение декодера).
- **Game-tested** (Fabric dedicated-server GameTest): `guard-fish-gametest2` и финальный `guard-fish-final-gametest` — exit 0, **151/151** (148 прежних + 3 новых). Первый прогон `guard-fish-gametest1` дал 150/151: ошибка самого теста (я восстанавливал PvP сервера к исходному `false`, а потом ждал `true`) — исправлена в тесте, код не менялся.
  1. `orderGuardTargetSwitchesChooseWhomRealGuardsFight` (`GolemGuardTargetsRuntimeChecks`; три настоящих подключённых игрока через `placeNewPlayer`, настоящие големы, меню, плейсер, колокольчик):
     - установка Order настоящим `interact`; без Order переключатели не существуют и маска не пишется;
     - настоящее меню: кнопки 52/53/54/51 меняют маску голема и синхронизированные данные меню, повторный щелчок возвращает, кнопки 50, 55–57, 60, 61, 99, 100, 105 ничего не меняют; меню Guard без Order и меню Fill с Order отказывают на 51–54;
     - **решение `isGuardTarget` для всех 16 масок × 12 видов настоящих существ** (паук, зомби, крипер, корова, кальмар, дикий волк, приручённый волк, житель, железный голем, летучая мышь, корова за пределами радиуса, чужой игрок, владелец) и Guard без Order; режим создатель/наблюдатель, PvP выключен (`server.setPvpAllowed(false)`), владелец по имени;
     - сохранение: NBT-байт `GuardTargets`, перезагрузка, отсутствие ключа при умолчании, маска 0, поддельные данные (без Order, 16/31/−1/−128, int вместо byte, Harvest-голем, оригинальный `toggles`) — `unsupported` и сырой байт сохраняется; плейсер → голем (владелец — разместивший) → колокольчик → плейсер несёт маску;
     - **четыре настоящих Guard-голема идут и бьют** (`Iron`+Order, 1 слот): *DEFAULT* убивает паука за 40 тиков, а корова, крипер и настоящий не-владелец-игрок остаются нетронутыми ещё 160 тиков после смерти паука; *ANIMALS* убивает корову, не трогая паука и крипера; *CREEPERS* убивает крипера;
       *PLAYERS* бьёт чужого игрока (потеря 66 из 1000 за 200 тиков), владелец, стоящий **ближе**, не теряет ни очка (20/20), паук, корова и крипер рядом целы. Здоровье наблюдается каждый тик (минимум), поэтому исцеление не прячет удар.
  2. `orderEntropyAndAirChangeWhatAFishingGolemCatches` (`GolemFishingUpgradesRuntimeChecks`): десять настоящих Fishing-големов с разными улучшениями; **случайные числа внедряются** (`RandomSource` с очередью) — окна всех краёв: junk `[0; 0,10)` / treasure `[0,10; 0,15)` без улучшений, Order 1/2 → treasure до `0,1625`/`0,175`, Entropy 1/2 → junk до `0,075`/`0,05` и сдвиг окна treasure, Entropy+Order вместе, озеро (junk 0,06 / treasure 0,072) с каждым улучшением,
     Fire готовит треску, но не трогает мусор и сокровище; Air: жребий берётся из `nextInt(10)` только при Air, `catchCount` даёт 2 при `draw < Air` и 1 иначе; затем 12000 бросков настоящей генерации добычи с фиксированным зерном — доли junk/treasure совпадают с 10/5 %, 10/7,5 % (Order×2) и 5/5 % (Entropy×2).
  3. `realFishingGolemsCastIntoPondsAndDeliverCooksAndExtraCatches` (`GolemFishingLiveRuntimeChecks`; **первая настоящая рыбалка в тестах проекта**): 24 настоящих Tallow-голема в трёх группах (plain, Fire, Air) закидывают снасть в свои пруды; тест ждёт по три доставки на группу, инвариант: Fire-голем никогда не отдаёт сырую треску/лосось, голем без Fire — жареную.
     Финальный прогон (`guard-fish-final-gametest`, 540 тиков): события доставки `{PLAIN=3, FIERY=3, AIRY=4}`, закидов `{16, 17, 17}`; доставлены `cooked_cod` и `cooked_salmon` у Fire-группы, сырые `cod`/`salmon`/`pufferfish`/`leather`/`fishing_rod` у остальных. В обоих прогонах у Air-группы было ровно по одному событию с двумя
     предметами сразу (настоящий второй улов из цикла в `activeTick`) — это **наблюдение, а не утверждение** теста: двойной улов случаен, и тест его не требует.
- **Мутации** (разово, затем побайтно восстановлено — хэши семи затронутых файлов сверены с исходными; скрипты `.tmp/mutate_guard.py`, `.tmp/summarize_mutation.py`, итог в `.tmp/guard-mutation-summary.txt`):
  - JUnit, десять правок в одном прогоне → падают 12 тестов: `toggle` не снимает бит и `button <= …` (кнопка 55) → `validMasks…`, `menuButtons…`; `equals` без маски → `equalityAndHashingCoverTheMask`; инвариант «нужен Guard+Order» ослаблен → `aMaskNeedsAGuardCoreAndAnOrderUpgrade` и `aMalformedOrOutOfRange…`;
    сетевой декодер не читает маску и JSON-геттер всегда умолчание → `everyMask…RoundTrips…`, `theNetworkRefusesAnOutOfRangeMask`; `0,0125→0,012`, `0,025→0,02`, `< air → <= air`, окно `roll − junk < treasure → roll < treasure` → все пять `GolemFishingUpgradesTest`.
  - GameTest, по одной правке за полный `:fabric:runGameTest` (каждый раз 151 тест, падает ровно один): животные всегда цель (`Order Guard, mask 0, cow: expected false`); крипер как монстр (`mask 1, creeper`); PvP не учитывается (`with PvP forbidden … never targets a player`); владелец не защищён (`mask 4, owner`); кнопка меню мертва (`button 52 is Animals`);
    маска не пишется в NBT (`saved under its own key …`); Order не передаётся в бросок (`one Order turns that same roll into treasure: … cod (FISH), expected TREASURE`); Entropy не передаётся (`one Entropy: 0.08 … leather_boots (JUNK), expected TREASURE`); Air не передаётся (`one Air, draw 0: expected 2 catch(es) … got 1`);
    **цель рыбалки вызывает `rollCatch` вместо `nextCatch`** (без плавки и без улучшений) → живой тест: `a Fire golem never delivers a raw fish: 1 minecraft:cod` (вызов в `activeTick`, раньше только компилировавшийся, теперь исполняется).
  - **Не поймана:** `catches = 1` в `activeTick` (убрать второй улов из цикла цели) — живой тест не утверждает двойной улов (он случаен), `catchCount` проверен отдельно; самим циклом второй улов исполняется только как случайное наблюдение.
- **Seen:** нет — клиент не запускался: четыре флажка и подписи `GuiGolem`, строка «Атакует: …» плейсера, тексты lang и tooltips видны только компиляцией; расположение подписей (длина «Животные» у правого края 176 px) оценено арифметикой, не глазами.
- **Не проверено:** рендер GUI и сетевая синхронизация данных меню 17–18 на клиент по сети; два игрока по сети и реальные protection-моды (удар по игроку идёт обычным `hurt` без события защиты, как у остальных ударов Guard); реальный save/reload мира (проверен NBT-круг в процессе); бой Guard с владельцем-на-команде (`isAlliedTo`);
  двойной улов как обязательное наблюдение; NeoForge.

## Пробелы (видимы, не стерты) и следующая очередь

- ~~Earth, вместимость~~ и ~~Water, радиус~~ перенесены во второй группе (раздел выше); остаются непроверенные места из списка «Не проверено».
- **Fire:** ~~поджог цели при ударе~~ перенесён (третья группа); ~~плавка улова Fishing~~ и ~~призрачные слоты `6+6n`~~ перенесены (четвёртая группа); остаются горящий предмет улова (визуал) и `1 + n` слотов Liquid (в порту сознательно одна страница).
- **Order:** ~~повторная посадка Harvest~~ перенесена (третья группа); ~~цвета маркеров/слотов~~ перенесены (четвёртая группа); ~~переключатели целей Guard~~ и ~~шанс редкого улова~~ перенесены (пятая группа; умолчание безопасное, см. отклонения); **Entropy (остальное):** нечёткое сравнение предметов (поэтому цвет сравнивает точно), очередь Liquid (~~плохой улов~~ перенесён); ~~**Air:** двойной улов Fishing~~ перенесён (пятая группа).
- Рендер иконок слотов на теле, «advanced» голем (+1 слот, ×1.1 скорость, +20 % радиус), декорации (G +10 % радиуса, R дротики с разбросом `7 − 1.75n`), Traveling Trunk (те же шесть предметов).
- ~~Замеченный дефект `ResearchIconDisplay.STACK`~~ (нет альтернативы `itemAxeElemental`) исправлен позже: шаблон дополнен, иконка ELEMENTALAXE теперь достижима (проверена только сборкой).
