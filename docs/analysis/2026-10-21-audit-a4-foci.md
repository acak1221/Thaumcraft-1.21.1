# Аудит соответствия порта: фокусы, снаряжение, чары и инфузия (21.10.2026)

Домен: семейства `foci_equipment_and_enchantments` (84 строки) и `infusion` (14 строк) worklist
`docs/porting/audit-reviews-a4-foci.json`; истина — декомпиляция TC4 4.2.3.5
(`/home/Acak/work/thaumcraft-4.2.3.5-oracle`), нативный порт — `common/src/main/java/thaumcraft/`.
Метод — чтение оригинала целиком и сравнение чисел и поведения с нативным кодом; ничего в порте не менялось.

Артефакты: `docs/porting/audit-reviews-a4-foci.json` (98 ревью), этот отчёт.

## Итог

| Вердикт | Количество |
|---|---|
| `disposition: port` | 64 |
| `disposition: adapt` | 15 |
| `disposition: replace` | 19 |
| `disposition: exclude` / `needs_review` | 0 |
| **Всего строк** | **98** |

| Покрытие натива | Количество |
|---|---|
| `native_coverage: parity_verified` | 95 |
| `native_coverage: partial` | 3 |
| `native_coverage: missing` | 0 |

| Семантика источника | Количество |
|---|---|
| `source_semantics: adapted` | 82 |
| `source_semantics: equivalent` | 2 |
| `source_semantics: not_applicable` | 14 (строки без оракульного аналога: легаси-справочник 1.12) |

`callsite_audit: complete` — у всех 98 строк.

По семействам: `foci_equipment_and_enchantments` — 57 port / 12 adapt / 15 replace, покрытие 82 parity + 2 partial;
`infusion` — 7 port / 3 adapt / 4 replace, покрытие 12 parity + 2 partial.

Ключевой вывод: **числа порта совпадают с оригиналом** во всех проверенных классах — стоимости ви (центivis),
таблицы улучшений фокусов по рангам, урон, прочность, задержки, ёмкости, значения потенции/жадности/бережливости,
формулы инфузии (200·2^(rank−1), 32·2^charge, 5+charge/2), 21 вариант исхода инфузии, интервалы рунического щита.
Основные расхождения — не в числах, а в трёх местах: (1) два забытых эффекта (дефекты D1, D2 ниже),
(2) визуальные эффекты 1.7, которые не имеют нативного аналога, (3) три неполных покрытия.

## Не `parity_verified` — что именно не совпадает

### 1. `thaumcraft/common/items/armor/ItemVoidRobeArmor.java` — `partial`

Оригинал (`oracle thaumcraft/common/items/armor/ItemVoidRobeArmor.java`) задаёт собственный вид надетой
пустотной одежды через `getArmorTexture`/`func_77618_c` и рисует её моделью `ModelRobe`; натив
(`native common/src/main/java/thaumcraft/common/items/armor/ItemVoidRobeArmor.java`) использует слои пустотной
брони как «interim» — это зафиксировано в `native common/src/main/java/thaumcraft/common/config/ThaumcraftEquipment.java`
(комментарий «INTERIM: … the worn layers are the void armor's, undyed»).

Поведение при этом совпадает: защита 3/7/6, фактор прочности 10 (110/160/150), зачарованность 10, починка
пустотным слитком, само-починка (раз в инвентаре и раз при надевании), 5 % скидка ви, 2 warp, окраска 0x6A3880
и стирка в водяном котле. Не перенесён только **видимый вид надетой одежды**.

### 2. `thaumcraft/common/config/recipes/ConfigRecipesInfusionDeviceSlice.java` — `partial`

Легаси-справочник 1.12 (оракульного аналога нет), натив заменил код-регистрацию рецептов на data-файлы
(`native common/src/main/resources/data/thaumcraft/recipe/infusion/`). Не перенесены два рецепта оригинала:

* `NODESTABILIZERADV` (стабилизатор узлов, оригинал `oracle thaumcraft/common/config/ConfigRecipes.java:343`,
  аспекты aura 32 / magic 16 / order 16 / energy 16, нестабильность 10) — в нативе нет файла рецепта;
* `WANDPEDFOC` (ванд-педестал с блоком-фокусом, оригинал `ConfigRecipes.java:342`) — есть только обычный
  `wand_pedestal`.

Оба относятся к семейству продвинутых устройств (M09), но запись рецепта — мой домен, поэтому отмечено здесь.

### 3. `thaumcraft/common/config/recipes/ConfigRecipesInfusionSlice.java` — `partial`

Рецепты колпачков и стержней перенесены (`wand_cap_thaumium`, `wand_cap_void`, `wand_rod_*`, `staff_rod_primal`),
но оригинальный `CAP_silver` (`oracle ConfigRecipes.java:331-335`, колпачки из чужого мода: медь и серебро с
«особыми» аспектами стоимости) в нативе нет — нет ни файла рецепта, ни самих колпачков. Это аддон-случай
оригинала (существует, только если другой мод поставляет слитки), но поведение `getSpecialCostModifierAspects`
нативом не поддерживается вообще.

## Отклонения, требующие решения

Формулировка «оригинал делает X, порт делает Y». Числа здесь верны; расходятся именно триггеры и носители.

1. **Призванный staff-rod не даёт +1 потенции.**
   Оригинал: `oracle thaumcraft/common/items/wands/ItemWandCasting.java:262-267` —
   `getFocusPotency = уровень апгрейда potency + (hasRunes(wand) ? 1 : 0)`, где `hasRunes` истинно только для
   `STAFF_ROD_PRIMAL` (`oracle thaumcraft/common/config/ConfigItems.java:325 setRunes(true)`).
   Порт: все фокусы берут потенциал только из апгрейда —
   `native common/src/main/java/thaumcraft/common/items/wands/foci/ItemFocusFire.java:132`,
   `ItemFocusShock.java:133`, `ItemFocusFrost.java:77`, `ItemFocusExcavation.java:155`.
   Данные о рунах в порту есть (`native common/src/main/java/thaumcraft/common/items/wands/ItemWandRod.java:69
   hasRunes(int)`, `native .../lib/baubles/RunicBaubleRules.java:185` — `primal_staff` с `runes=true`), но
   **потребителя нет**: ни один нативный вызов `hasRunes` не существует. См. дефект D1.

2. **Септр не даёт −10 % потребления ви.**
   Оригинал: `oracle thaumcraft/common/items/wands/ItemWandCasting.java:256-259` —
   `if (this.isSceptre(is)) consumptionModifier -= 0.1f;` (это касается и крафта, и каста).
   Порт: маркер септра пишется рецептом (`native .../lib/crafting/ArcaneSceptreRecipe.java:110`), читатель есть
   (`native .../common/config/ThaumcraftWandEnchantments.java:55 isSceptre`), но ни `WandVisDiscount.modifier`
   (`native .../items/wands/WandVisDiscount.java:51-55`), ни `focusModifier` (`:64-68`), ни `ArcaneVis.price`
   (`native .../crafting/ArcaneVis.java:42`) скидку не применяют. См. дефект D2.

3. **Пустотные инструменты: эффект «иссохнуть» перенесён с левого клика на попадание.**
   Оригинал: `oracle thaumcraft/common/items/equipment/ItemVoidAxe.java:85-90` — `onLeftClickEntity`, то есть
   эффект срабатывает даже при промахе; порт: `native .../equipment/ItemVoidAxe.java:29-31` —
   `hurtEnemy`, то есть только при реальном попадании. То же для кирки, лопаты, мотыги и кристального меча
   (`ItemCrimsonSword`). Портовые комментарии это фиксируют как «the native equivalent», но поведение шире.

4. **Ванд-педестал не берёт составные аспекты через блок-фокус.**
   Оригинал: `oracle thaumcraft/common/tiles/TileWandPedestal.java:238-263` — при блоке-фокусе сверху
   (`hasThingy`) педестал берёт из узла и **примитивные части составного аспекта**, приведённые к примитивам.
   Порт: `native .../tiles/TileWandPedestal.java:103-127` берёт только уже примитивные аспекты; ветки `hasThingy`
   нет.

5. **Дальность луча фокуса Trade фиксирована в 5 блоков.**
   Оригинал: `oracle thaumcraft/common/items/wands/foci/ItemFocusTrade.java:120-124` использует
   `EntityPlayerMP.field_71134_c.getBlockReachDistance()` (у Forge это 5.0, но значение настраиваемо).
   Порт: `native .../wands/foci/ItemFocusTrade.java:42` и `:57` — жёсткое `player.pick(5, 1, false)`.

6. **Игнорируемые источники урона рунического щита сопоставлены приблизительно.**
   Оригинал: `oracle thaumcraft/common/lib/events/EventHandlerRunic.java:180` — не действует щит на
   `outOfWorld`, `magic`, `starve`, `suffocate`.
   Порт: `native .../lib/runic/RunicShield.java:95-96` — `DROWN` (это suffocate/утопление), `WITHER`
   (в 1.7 «magic» — это общий магический урон, в 1.21 точного аналога нет), `FELL_OUT_OF_WORLD`, `STARVE`.
   Четыре источника сопоставлены один-в-один по смыслу, кроме `magic → WITHER`.

7. **Рунный щит не рисует «ripple»-кадры для не-культистских мобов.**
   Оригинал: `oracle thaumcraft/client/fx/other/FXShieldRunes.java:79-84` — для `EntityMob` (не культист)
   берётся набор `ripple1..15.png`, для остальных `hemis1..15.png`.
   Порт: `native common/src/main/java/thaumcraft/client/fx/particles/RunicHitParticle.java:71` всегда рисует
   `hemis{frame}`. Текстуры `ripple*.png` в ресурсах при этом лежат.

8. **Оглушение/защита педестала от компаратора не перенесена.**
   Оригинал: `oracle thaumcraft/common/tiles/TilePedestal.java:186-201` — события 11 и 12 рисуют искры.
   В 1.21.1 у блок-сущности нет аналога этого события, в нативе его нет.

9. **Блок-стабилизатор инфузии: `Blocks.field_150465_bP` → `AbstractSkullBlock`.**
   Оригинал: `oracle thaumcraft/common/tiles/TileInfusionMatrix.java:629` принимает этот блок всегда.
   Порт: `native .../lib/infusion/InfusionStructure.java:121-123` принимает любой `AbstractSkullBlock`
   (стоящая и настенная черепа) — то есть семейство блоков шире оригинала.

10. **Размер стека базового `ItemRunic` — 1 вместо 64.**
    Оригинал: `oracle thaumcraft/api/ItemRunic.java:19` — обычный `Item` со стандартным стеком 64 (каждый
    конкретный аксессуар сам ставил 1). Порт: `native .../api/ItemRunic.java:16` — `stacksTo(1)`.

## Дефекты

### D1. Призванный staff-rod (+1 потенции) зарегистрирован, но не работает — `ItemWandCasting`/фокусы

* Оригинал: `oracle thaumcraft/common/items/wands/ItemWandCasting.java:266` добавляет к потенции фокуса
  единицу, если стержень — рунический (`oracle thaumcraft/common/config/ConfigItems.java:325`:
  `STAFF_ROD_PRIMAL.setRunes(true)`; `oracle thaumcraft/api/wands/StaffRod.java:37-43`).
* Порт: данные есть (`native common/src/main/java/thaumcraft/common/items/wands/ItemWandRod.java:69`
  `hasRunes(int)`, `native .../lib/baubles/RunicBaubleRules.java:185` — `primal_staff` с `runes=true`),
  но **вызовов `hasRunes` в порту нет ни одного** (`grep -rn "hasRunes" common/src/main/java` → только
  объявление). Все фокусы считают потенцию как `FocusUpgradeData.level(focus, FocusUpgradeType.potency)`
  (`native .../wands/foci/ItemFocusFire.java:132`, `ItemFocusShock.java:133`, `ItemFocusFrost.java:77`,
  `ItemFocusExcavation.java:155`, `ItemFocusHellbat.java:98`).
* Воспроизведение: сварить посох с призванным стержнем (primal_staff), вложить огненный фокус с одним
  апгрейдом Potency, выстрелить. Оригинал даёт 4 снаряда (`2 + potency`, где potency = 1 + 1 = 2), порт — 3
  (`2 + 1`). Для шока: оригинал 8 урона (`6 + potency`), порт 7 (`6 + 1`).
* Классификация: зарегистрировано, но не применимо → `native_coverage: partial` у затронутых строк.

### D2. Септр (−10 % потребления ви) зарегистрирован, но не работает

* Оригинал: `oracle thaumcraft/common/items/wands/ItemWandCasting.java:257-259` — `isSceptre` уменьшает
  модификатор потребления на 0.1 (применяется и к крафту, и к касту: `func_77663_a`/`consumeAllVisCrafting`
  идут через общий `getConsumptionModifier`).
* Порт: маркер `thaumcraft:wand_sceptre` ставится рецептом
  (`native .../lib/crafting/ArcaneSceptreRecipe.java:110`), но ни один путь расчёта цены его не читает:
  `native .../items/wands/WandVisDiscount.java:44-55` (`modifier`), `:58-68` (`focusModifier`),
  `native .../crafting/ArcaneVis.java:36-45` (`price`). `ThaumcraftWandEnchantments.isSceptre`
  (`native .../config/ThaumcraftWandEnchantments.java:55`) не имеет вызывающего кода.
* Воспроизведение: сварить септр (рецепт `arcane_sceptre`, 1.5× цены), наполнить его ви, затем использовать
  его как каст-источник. Оригинал списывает на 10 % меньше, порт — столько же, сколько обычный посох.
* Классификация: зарегистрировано, но не применимо.

Оба дефекта — одного класса («число/эффект присутствует в данных, но не в пути исполнения») и оба
противоречат правилу AGENTS.md «каталог или заглушка — не завершённая игровая единица».

### D3. (дефект точности, не поведения) Комментарий в `ItemElementalAxe` устарел

`native common/src/main/java/thaumcraft/common/items/equipment/ItemElementalAxe.java:24` утверждает
«Optional Repair-enchantment self repair is NOT implemented», однако
`native .../lib/enchantment/EnchantmentRepair.java:241-253` чинит любой предмет, реализующий `IRepairable`,
а `ItemElementalAxe` его реализует. Поведение в порядке, комментарий вводит в заблуждение.

## Не перенесено

| Что | Где в оригинале | Почему |
|---|---|---|
| Рецепт `NODESTABILIZERADV` | `oracle ConfigRecipes.java:343` | нет нативного файла рецепта; устройство принадлежит M09 |
| Рецепт `WANDPEDFOC` (педестал с блоком-фокусом) | `oracle ConfigRecipes.java:342` | нет нативного файла рецепта |
| Рецепты колпачков из чужого мода (`CAP_silver`, `CAP_copper`) и «особый» модификатор стоимости | `oracle ConfigRecipes.java:331-335`, `oracle thaumcraft/api/wands/WandCap.java:36-45` | аддон-случай оригинала; в порте нет ни самих колпачков, ни `getSpecialCostModifierAspects` в пути цены |
| Внешний вид надетой пустотной одежды (модель `ModelRobe`) | `oracle thaumcraft/common/items/armor/ItemVoidRobeArmor.java` | натив явно помечает слои пустотной брони как «INTERIM» |
| Клиентские искры элементарных инструментов (кирка/лопата/меч) | `oracle ItemElementalShovel.java:154`, `ItemElementalPickaxe.java`, `ItemElementalSword.java:118-133` | 1.7 `Thaumcraft.proxy.blockSparkle`/`smokeSpiral` не имеют нативного аналога; в порте это объявлено |
| Скан руды элементарной киркой (клиентский оверлей) | `oracle ItemElementalPickaxe.java:89-90 startScan` | клиентский рендер 1.7, не перенесён |
| «Ripple»-кадры рунического щита для мобов | `oracle thaumcraft/client/fx/other/FXShieldRunes.java:79-84` | ветка заменена на `hemis` для всех целей |
| Сетка-компаратор педестала (события 11/12) | `oracle thaumcraft/common/tiles/TilePedestal.java:186-201` | в 1.21.1 у блок-сущности нет такого события |

## Что НЕ удалось проверить

* **Клиентский визуал.** Ни один из 9 клиентских классов не проверялся в игре: аудит читал код и сравнивал
  с оракулом, но рендер 1.21.1 (модели, слои, частицы) не сверялся с картинками оригинала. Для `FXShieldRunes`,
  `GuiFocalManipulator`, `GuiFocusPouch`, рендереров матрицы/педестала/пилона/пьедестала и моделей это
  остаётся `parity_verified` только по коду.
* **Коллизии и физика.** Поведение поднятых/летящих сущностей (меч стихий, гейты, падение) в 1.21.1
  отличается от 1.7 на уровне движка; совпадение чисел формул не означает совпадение ощущений.
* **Инфузионные рецепты вне моей строки.** Сверялись 63 рецепта оригинала (`ConfigRecipes.java`) с 59+1
  нативными файлами; совпадение проверено точечно (`focus_warding` — аспекты, нестабильность, research,
  компоненты), а не построчно для всех 63.
* **Баланс и тайминги в живой игре.** Ни один из записанных дефектов (D1, D2) не воспроизводился запуском
  сервера: воспроизведение описано по коду оригинала и натива, а не по фактическому прогону.
* **Классы вне worklist.** `ItemFocusPech` (фокус печа) и `FocusPech` в 1.12-справочнике по эвристике
  роутинга ledger-а попали в семейство `warp_creatures_and_ecology` (M11) и в этот аудит не входят;
  тип апгрейда `nightshade` (id 15) при этом зарегистрирован в нативном `FocusUpgradeType`.
* **Внешние аддоны.** Ни одна из возможностей, зависящих от чужих модов (медный/серебряный колпачок,
  `WandCap` с особыми аспектами), не проверялась на совместимость.

## Сводка по подсистемам

* **Фокусы (9 классов).** Все девять зарегистрированы и пригодны: `ThaumcraftFoci` (fire, excavation, frost,
  shock, portable hole, warding, trade), `ThaumcraftHellbatFoci` (hellbat), `ThaumcraftPrimalFoci` (primal).
  Все 21 тип апгрейда `FocusUpgradeType` присутствуют с исходными id и аспектами; улучшения не «пустые»:
  frugal уменьшает цену (`WandVisDiscount.focusModifier`), potency/treasure/silktouch/dowsing работают в
  раскопках (`native .../lib/world/ExcavationHarvest.java:328-332`), enlarge — в раскопках/сделке/защите/шоке,
  fireball/firebeam/alchemistsfire — в огне, scattershot/iceboulder/alchemistsfrost — во морозе,
  chainlightning/earthshock — в шоке, batbombs/devilbats/vampirebats — в нежити, seeker — в первобытном,
  architect/extend — в сделке и защите.
* **Посуда и снаряжение.** Материалы ( Thaumium 3/400/7.0/2.0/22, Void 4/150/8.0/3.0/10, Elemental
  3/1500/10.0/3.0/18, CVOID 4/200/8.0/3.5/20) и брони (2/6/5/2, 3/7/6/3, 1/3/2/1, 4/8/7/4) совпадают с
  `oracle thaumcraft/api/ThaumcraftApi.java:48-55`. Пустотная броня, пустотные инструменты, доспехи Таймурия и
  одежда сохраняют свои прочности, скидки ви, warp, само-починку и зачарованность.
* **Чары.** Frugal (5+11·(l−1), max 3, шанс 1−l/5), Potency (10+11·(l−1)), Wand Fortune (15+9·(l−1)),
  Haste (15+9·(l−1)), Repair (20+10·(l−1), max 2) — те же числа, что в `oracle .../lib/enchantment/*.java`.
  Как и в оригинале, Frugal/Potency/Wand Fortune в 4.2.3.5 не реализуют `IWandFocus.acceptsEnchant`, поэтому
  применимы только к книгам — натив это воспроизводит.
* **Инфузия.** Матрица, пьедестал, пьедестал для посоха, столб и рецепты перенесены; цикл, 21 исход,
  рост нестабильности, правила XP и стоимость улучшения фокуса (200·2^(rank−1)) совпадают.
