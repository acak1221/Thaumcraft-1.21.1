# Палочки, фокусы и аспекты расхода — сверка TC4 4.2.3.5 ↔ порт 1.21.1 (21.10.2026)

Домен: `thaumcraft/common/items/wands/**` (`ItemWandCasting`, `WandManager`-порт, `WandData`,
`WandMaterials`, `WandVisDiscount`, `FocusVis`, `foci/**`), сборка палочек (`ArcaneWandRecipe`,
`ArcaneSceptreRecipe`), `ThaumcraftWands.java` + связанные ресурсы, и связь `ItemHandMirror` с зеркалом.

Истина: `/home/Acak/work/thaumcraft-4.2.3.5-oracle` (декомпиляция CFR оригинального JAR TC4 4.2.3.5,
1.7.10). Справочник имён ваниллы: `scripts/porting/resolve_srg.py` (карта MCP 1.7.10
`mcp_stable-12-1.7.10`). Порт: `common/src/main/java/thaumcraft/`, ветка `port/ocwands`.

Метод: чтение оригинальных классов целиком (не только выжимка SRG-имён), построчное сравнение
чисел, порядка операций и веток с нативным кодом; каждый найденный дефект исправлен в этой ветке
и закрыт тестом.

---

## 1. Итог

| Что | Вердикт |
|---|---|
| Ёмкости (centivis), скиптра ×150/100 | совпадает |
| Модификатор колпачка, **зависящий от аспекта** | совпадает (медный: 1.0 за ordo/perditio, 1.1 за остальное) |
| Порядок вычитаний модификатора (колпачок → снаряжение → frugal → скиптр) | совпадает |
| Точная арифметика (×100 → ×float → отбрасывание дробной части) | совпадает |
| Скидка снаряжения (`WandManager.getTotalVisDiscount`) и порог 0.1 | совпадает |
| Стоимости всех 10 фокусов, задержки, цвета, ранги апгрейдов | совпадает |
| Сборка палочки и скиптра (цена, сетка, исследования) | совпадает |
| Инфузия апгрейда фокуса (200·2^(rank−1), XP rank·8) | совпадает |
| Синхронизация (компоненты `wand_data`/`wand_focus`/`focus_upgrades`) | совпадает |
| **Дефект 1:** палочка не забирает обратно «вардовый камень» (плиту, стекло, дверь) | **исправлен** |
| **Дефект 2:** у фокуса Trade нет левого клика (обмен блока) | **исправлен** |
| Пробел: подсказка палочки не показывает расход по аспектам и средний | остаётся (клиент) |

---

## 2. Что делает оригинал (проверенные числа)

### 2.1 Ёмкости и материалы

`oracle thaumcraft/common/config/ConfigItems.java:302-325`:

* колпачки: iron 1.1/1, gold 1.0/3, thaumium 0.9/6, void 0.8/9; **copper** 1.1 с особыми аспектами
  `ORDER, ENTROPY` и особым модификатором 1.0, цена 2 (`ConfigItems:331`, только если другой мод
  поставляет медный слиток); **silver** 1.0 с особыми `AIR, EARTH, FIRE, WATER` и 0.95, цена 4
  (`ConfigItems:334`, тот же аддон-случай);
* стержни: wood 25/1, greatwood 50/3, silverwood 100/9, шесть элементных 75/6;
* ядра посохов: greatwood 125/8, obsidian…reed 175/14, silverwood 250/24, primal 250/32 с рунами
  (`ConfigItems:325 setRunes(true)`).

`oracle ItemWandCasting.java:119-121`: `getMaxVis = getRod(stack).getCapacity() * (isSceptre(stack) ? 150 : 100)`
— ёмкость скиптра в полтора раза больше ёмкости стержня (в centivis: 3750 для дерева, 37500 для
посоха Primal).

Порт: `common/.../wands/WandMaterials.java:52-87` (материалы) и `:115-117` (`capacity(rod, sceptre)`),
`common/.../wands/ItemWandCasting.java:78` (`getMaxVis`). Значения совпадают; целочисленное деление
`*150/100` точно, потому что все ёмкости кратны 2500.

### 2.2 Расход ви — точная арифметика

`oracle ItemWandCasting.java:247-259` (`getConsumptionModifier`):

```
1) модификатор колпачка: getSpecialCostModifierAspects() != null && contains(aspect)
                            ? getSpecialCostModifier() : getBaseCostModifier();
2) consumptionModifier -= WandManager.getTotalVisDiscount(player, aspect);   // player != null
3) если есть фокус и это НЕ крафт: consumptionModifier -= (float)getFocusFrugal(is) / 10.0f;
4) если скиптр: consumptionModifier -= 0.1f;
5) return Math.max(consumptionModifier, 0.1f);
```

`oracle ItemWandCasting.java:297-338` — умножение и отбрасывание:

* `consumeVis`: `amount = (int)((float)amount * modifier)`;
* `consumeAllVis`: `cost = (int)((float)cost * modifier)`;
* `consumeAllVisCrafting`: сначала `cost = aspects.getAmount(aspect) * 100` (целые вис → centivis),
  затем тот же `(int)((float)cost * modifier)`.

Порт: `common/.../wands/WandVisDiscount.java:75-79` (порядок вычитаний), `:95-99` (тот же порядок с
frugal), `common/.../crafting/ArcaneVis.java:42` (`(int)(amount * 100 * modifier)` — целое умножается
на 100 **до** умножения на float), `common/.../wands/FocusVis.java:25-27`
(`(int)(amount * modifier)`, где amount уже в centivis). Порядок «×100 → ×float → truncate» и
отсутствие смешивания int/float соблюдены.

**Модификатор колпачка по аспекту** в порту: `WandVisDiscount.capModifier(wand, aspect)` (`:58-62`) →
`WandMaterials.Cap.modifierFor(aspectTag)` (`:46-48`), а медный колпачок несёт
`List.of("ordo","perditio")` и особый модификатор 1.0 (`WandMaterials.java:81`). Это тот же выбор, что
`getSpecialCostModifierAspects().contains(aspect) ? … : …` в оригинале. Закреплено тестом
`WandWardedStoneTest#theCopperCapPaysItsOwnPriceForOrderAndEntropy`.

### 2.3 Скидка снаряжения

`oracle WandManager.java:75-101` (`getTotalVisDiscount`): четыре слота амулетов + четыре слота брони,
все точки целые; затем штраф за истощение `(Math.max(level1, level2) + 1) * 10`, где `level1/level2` —
`player.getPotionEffect(id).getAmplifier()`; итог `/100.0f`.

Проверка SRG-имени: `func_76458_c` в карте MCP 1.7.10 — это **`getAmplifier`** (0-based), а не
`getEffectLevel` (`+1`). Поэтому `(max(amp)+1)*10` оригинала совпадает с портом
(`WandVisDiscount.percentagePoints:38-45`, `getAmplifier()`), а не с «на единицу большим» вариантом.
`ThaumcraftFlux.visPenalty` (`:87-93`) использует ту же величину.

Порог 0.1 — в порту `Math.max(..., .1F)` в обеих ветках (`modifier`, `focusModifier`). Скиптр −0.1 —
`WandVisDiscount.SCEPTRE_DISCOUNT`. Frugal −0.1 за ранг — `focusModifier`, и **только** в пути каста:
крафт (`ArcaneVis.price`) frugal не учитывает, как в оригинале (`getConsumptionModifier(..., crafting=true)`).

### 2.4 Фокусы

| Фокус | Оригинал (стоимость, centivis) | Порт |
|---|---|---|
| Fire | ignis 10; луч ignis 10 + ordo 3; шар ignis 66 + perditio 33; кулдаун 1000 (шар) / 0 | `ItemFocusFire:34-41` ✓ |
| Frost | aqua 5 + ignis 2 + perditio 2; разброс aqua 20 + ignis 2 + perditio 2 + aer 5; валун … + terra 5; кулдаун 500/200 | `ItemFocusFrost:30-41` ✓ |
| Shock | aer 25; цепь aer 40 + aqua 10; землетрясение aer 75 + terra 25; кулдаун 500/1000/250 | `ItemFocusShock:44-53` ✓ |
| Excavation | terra 15; шёлк — по 1 каждого + terra 15; доукос — ignis 2 + ordo 2 + terra 15 | `ItemFocusExcavation:56-63`, `ExcavationVisCosts` ✓ |
| Trade | perditio 5 + terra 5 + ordo 5; шёлк — по 1 каждого | `ItemFocusTrade:23-29` ✓ |
| Warding | terra 25 + ordo 25 + aqua 10 | `ItemFocusWarding:20-22` ✓ |
| Hellbat | ignis 200 + perditio 100 + aer 100; бомбы 100/200/100; дьяволы + terra 100 | `ItemFocusHellbat:42-49` ✓ |
| Pech | terra 10 + perditio 10 + aqua 10; ночная тень — по 10 каждого | `ItemFocusPech:26-29` ✓ |
| Primal | 50…250 каждого первичного (шаг 50) | `ItemFocusPrimal:27-32` ✓ |
| Portable Hole | perditio 10 + aer 10 | `ItemFocusPortableHole:39-41` ✓ |

Цвета, ключи сортировки, `getMaxAreaSize` (Trade 3+2·enlarge, Warding 3+enlarge), задержки активации и
таблицы `getPossibleUpgradesByRank` совпадают с оригиналом (проверено построчно).

**Важная особенность оригинала, которую порт сохраняет:** `getVisCost` почти всегда вызывается с
**жезлом**, а не с фокусом (`ItemFocusFire:99`, `ItemFocusFrost:61`, `ItemFocusShock:123`,
`ItemFocusExcavation:126`), поэтому реальный платёж — базовая цена, а «дорогая» цена видна только в
подсказке фокуса. Порт это воспроизводит (`ItemFocusFire:78-80`, `ItemFocusFrost:56-58`,
`ItemFocusShock:88`, `ItemFocusExcavation:84-88`).

### 2.5 Сборка палочки и скиптра

* `oracle common/lib/crafting/ArcaneWandRecipe.java:32-65`: колпачок в (0,2) и (2,0), стержень в (1,1),
  остальные шесть слотов пусты; оба колпачка должны совпадать; цена = `cc * cr` на каждый первичный
  аспект; комбинация wood+iron **не** даёт результата (`:57`), хотя `matches`/`getAspects` её принимают.
  Порт: `common/.../crafting/ArcaneWandRecipe.java:30-86` — та же раскладка (CAP1=6, CAP2=2, ROD=4),
  то же исключение wood+iron (`:81`), та же цена.
* `oracle common/lib/crafting/ArcaneSceptreRecipe.java:32-68`: три колпачка (1,0), (2,1), (0,2), стержень
  (1,1), Primal Charm (2,0); цена `(int)((float)(cc * cr) * 1.5f)`; тег `sceptre` на(result).
  Порт: `common/.../lib/crafting/ArcaneSceptreRecipe.java:49-110` — та же сетка, та же цена
  (`WandEnchantmentRules.sceptreCost:107-110`), маркер `thaumcraft:wand_sceptre`.

### 2.6 Инфузия апгрейда фокуса

`oracle common/tiles/TileFocalManipulator.java:124-162`: стоимость апгрейда ранга *r* — `200 · 2^(r−1)`
каждого ключевого аспекта (`FocusUpgradeType.aspects` — по **ключам**, значения игнорируются), XP
`rank · 8`; сервер дополнительно проверяет, что тип входит в `getPossibleUpgradesByRank(rank)`.
Порт: `common/.../lib/foci/FocalUpgradeCosts.java:45-55` (`200 << (rank-1)`, по ключам) и
`common/.../tiles/TileFocalManipulator.java` — совпадает.

### 2.7 Синхронизация

В 1.7.10 данные палочки лежат в NBT и уезжают с предметом. В порте это три сетевых компонента:
`ThaumcraftContent.WAND_DATA` (`ThaumcraftContent.java:63-64`, `WandData.CODEC`/`STREAM_CODEC`),
`ThaumcraftFoci.WAND_FOCUS` (`ThaumcraftFoci.java:25-27`) и `ThaumcraftFoci.FOCUS_UPGRADES`
(`:28-30`) — все с `networkSynchronized`. После каждого списания ви сервер вызывает
`player.inventoryMenu.broadcastChanges()` (`ItemFocusFire:152-153`, `ItemFocusShock:161-162`,
`ItemFocusExcavation:150-151`), поэтому клиент видит те же числа. **Пакетов вида `PacketWandUpdate` в
оригинале нет** — такого класса в декомпиляции TC4 4.2.3.5 не существует; синхронизация NBT-меток
выполняет ту же роль.

---

## 3. Дефекты

### D1. Палочка не забирает обратно «вардовый камень» (плита, стекло, арканная дверь)

* **Оригинал:** `oracle thaumcraft/common/items/wands/ItemWandCasting.java:390-415`
  (`onItemUseFirst`): клик палочкой по деревянному устройству md 2 (арканная нажимная плита) или
  непрозрачному косметическому блоку md 2 (вардовое стекло) **и** по арканной двери снимает блок и
  выдаёт его предметом; у двери снимаются обе половины, предмет выдаётся только с нижней половины,
  если опция «вардовый камень» выключена (`:407`).
* **Порт:** ветки не было вообще — `common/.../wands/ItemWandCasting.java:168-202` (`useOn`) знал
  только про IWandable, триггеры устройств и тауномоникон. `TileOwned.safeToRemove`
  (`common/.../tiles/TileOwned.java:35`) объявлялся, но **не устанавливался нигде** — «мёртвый» флаг.
* **Последствие:** при включённой опции «вардовый камень» (в порте она включена по умолчанию,
  `ResearchSettings.DEFAULTS:8`) у арканной нажимной плиты (`BlockArcanePressurePlate:69-72`),
  вардового стекла (`BlockCosmeticOpaque:73-75`) и арканной двери (`BlockArcaneDoor:72-74`) прогресс
  разрушения равен нулю, то есть их **невозможно ни сломать, ни забрать** — блок пропадает у игрока
  навсегда.
* **Исправление:**
  * `common/.../wands/WandWardedStoneRules.java` (новый) — чистые правила: гейт владельца
    (`mayRemove`) и правило выдачи предмета двери (`dropsDoorItem`);
  * `common/.../wands/WandWardedStone.java` (новый) — исполнение на сервере: снятие блока, выдача
    предмета в центре блока, `levelEvent(2001, …)`, `safeToRemove` на обеих половинах двери;
  * `common/.../wands/ItemWandCasting.java:199-202` — вызов `WandWardedStone.tryRemove(context)` в
    конце `useOn` (после ветки триггеров, как в оригинале);
  * `common/.../blocks/BlockArcaneDoor.java:101-120` — дверь больше **не глотает** клик палочкой: в
    оригинале у `BlockArcaneDoor` вообще нет `onBlockActivated`, поэтому клик доходил до
    `ItemWandCasting:400`; в 1.21.1 блок обрабатывается раньше предмета, поэтому без этой правки
    `useOn` палочки был бы недостижим для двери. Проверяется только **главная** рука — в 1.7.10 палочка
    действовала только из `getCurrentEquippedItem()`, и вторая рука тогда не существовала.* **Замечание об отклонении (осознанное):** в оракуле проверка владельца — это
  `player.getGameProfile().equals(((TileOwned) tile).owner)`, то есть сравнение `GameProfile` со
  `String`, которое **никогда не равно** (`GameProfile.equals` принимает только `GameProfile`). То есть
  в оригинале при включённом «вардовом камне» владелец не мог забрать свой блок никогда. Порт
  сравнивает **имя** владельца — так же, как уже сделано в самом порту для двери и ключа
  (`BlockArcaneDoor.useWithoutItem`, `ItemKey.useOn`), и так же, как это исправил OpenTC4 в коде 1.7.10
  (`OpenTC4 ItemWandCasting:392` — `player.getCommandSenderName().equals(...)`). Это единственное
  отступление от оракула в этом дефекте, и оно зафиксировано здесь.

### D2. У фокуса Trade нет левого клика (обмен блока на запомненный)

* **Оригинал:** `oracle ItemWandCasting.java:583-590` (`onEntitySwing`) передаёт взмах палочки фокусу;
  `oracle common/items/wands/foci/ItemFocusTrade.java:168-181` (`onEntitySwing`) ставит в очередь обмен
  **только целевого** блока (`addSwapper(..., 0, …)`), без распространения по площади.
* **Порт:** `common/.../wands/foci/ItemFocusTrade.java:55-60` содержит готовый
  `leftClick(player, wand, pos)`, но **вызывающих у него не было** (`grep` по `common/src/main`:
  единственное упоминание — само объявление). Правый клик (запомнить/разложить) работал, левый —
  нет.
* **Исправление:** `common/.../mixin/TradeFocusServerPlayerGameModeMixin.java` (новый) — инъекция в
  `ServerPlayerGameMode.handleBlockBreakAction` на `START_DESTROY_BLOCK`: в 1.21.1 это тот же пакет,
  который клиент шлёт на то же самое действие (левый клик по блоку). Обычное разрушение блока не
  отменяется — оригинал тоже не отменял его, а только ставил обмен в очередь. Миксин зарегистрирован в
  `common/src/main/resources/thaumcraft-common.mixins.json`.
  Заодно `ItemFocusTrade.leftClick` теперь сначала проверяет, что актор действительно держит фокус Trade
  (`TradeSwaps.actor`), и только потом трассирует луч: раньше трассировка шла при каждом
  `START_DESTROY_BLOCK` в мире.

---

## 4. Пробелы (не исправлено в этой ветке)

1. **Подсказка палочки.** Оригинал (`ItemWandCasting.java:164-205`) показывает для каждого аспекта
   «§цвет × количество, §o(расход % vis cost)», средний расход в строке ёмкости и цену фокуса.
   Порт (`ItemWandCasting.java:280-295`) показывает ёмкость, одну строку «Base vis cost: N %» и
   «Aspect: amount/capacity». Для медного колпачка эта строка вводит в заблуждение: реальный расход
   зависит от аспекта (1.0 за ordo/perditio, 1.1 за остальное). Исправление — клиентская работа
   (сдвиг-режим, `DecimalFormat("#######.##")`, средний расход), в этом проходе не делалось.
2. **Серебряный колпачок** (`ConfigItems:334`, 1.0 / особые aer, terra, ignis, aqua → 0.95 / цена 4) —
   аддон-случай оригинала (существует, только если другой мод поставляет слитки). В порту нет ни
   колпачка, ни его особого модификатора; в 1.21.1 серебра нет в ванилле. Осознанно не переносится.
3. **Клиентский вид** (модель палочки, луч фокуса, эффекты) в этом проходе не сверялся — только код и
   числа. `PrimalWandItemRendererMixin`/`WandModels` не проверялись в игре.
4. **Ни один из двух дефектов не воспроизводился запуском сервера/клиента**: проверка — компиляция
   обоих модулей, `:common:test` и чтение кода. Снятие «вардового камня» и левый клик Trade требуют
   мира с узлом, зеркалом и блочным фокусом; в этом проходе только unit-тесты правил.
5. **Педостал палочки** (`TileWandPedestal`) не берёт составные аспекты через блочный фокус
   (`oracle TileWandPedestal.java:224-263` против `common/.../tiles/TileWandPedestal.java:104-131`).
   Это уже зафиксированное отклонение аудита a4 и домен плитки, не палочек; здесь не исправлялось.
6. `ItemHandMirror` (связь с зеркалом) сверен: `useOn` (`:67-81`) — привязка к зеркалу с звуком jar и
   сообщением; `use` (`:87-106`) — открытие вида зеркала или обрыв связи с zap-звуком; `transport`
   (`:152-182`) — предмет появляется перед зеркалом в его мире. Соответствует
   `oracle thaumcraft/common/items/relics/ItemHandMirror.java:97-188`. Расхождений нет.

---

## 5. Проверка

Команда (реальный вывод, `--rerun`, чтобы задача не была взята из кэша):

```
export JAVA_HOME=/home/Acak/.jdk21; export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/ocwands
./gradlew --no-daemon --console=plain :common:test --rerun
```

```
> Task :common:compileJava
> Task :common:processResources UP-TO-DATE
> Task :common:classes UP-TO-DATE
> Task :common:compileTestJava UP-TO-DATE
> Task :common:processTestResources UP-TO-DATE
> Task :common:testClasses UP-TO-DATE
> Task :common:test

BUILD SUCCESSFUL in 25s
5 actionable tasks: 1 executed, 4 up-to-date
```

Разбор XML-отчёта `common/build/test-results/test/*.xml`:
**1670 тестов, 0 failures, 0 errors, 0 skipped** (до правок было 1665; добавлено 5 в
`thaumcraft.wands.WandWardedStoneTest`).

Новый тест `common/src/test/java/thaumcraft/wands/WandWardedStoneTest.java` (5 тестов):

* `withoutWardedStoneAnybodyMayTakeTheBlockBack` — гейт владельца при выключенной опции;
* `withWardedStoneOnlyTheOwnerMayTakeTheBlockBack` — гейт владельца при включённой опции;
* `theArcaneDoorOnlyDropsItsItemFromTheLowerHalf` — правило выдачи предмета двери;
* `theTradeLeftClickMixinIsRegistered` — миксин левого клика Trade зарегистрирован в
  `thaumcraft-common.mixins.json` (без регистрации он был бы мёртвым);
* `theCopperCapPaysItsOwnPriceForOrderAndEntropy` — модификатор колпачка по аспекту и цена
  (10000 centivis за order, 11000 за air, 9000 с frugal 1).

**Не выполнялось:** клиентский приём, GameTest-прогон, мультиплеер. Новый миксин
(`TradeFocusServerPlayerGameModeMixin`) проверен только компиляцией и чтением сигнатуры
`ServerPlayerGameMode.handleBlockBreakAction(BlockPos, Action, Direction, int, int)` из маппингов 1.21.1;
в игре он не запускался.
