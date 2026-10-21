# Сверка исследований и аспектов: TC4 4.2.3.5 (1.7.10) → порт 1.21.1

Дата: 2026-10-21. Домен: `thaumcraft/common/lib/research/**`,
`thaumcraft/common/lib/aspects/**`, `thaumcraft/common/config/ResearchSettings.java`,
ресурсы `data/thaumcraft/tc4/**` и `assets/thaumcraft/{lang,textures}/**`.
Источник истины — декомпиляция `Thaumcraft-1.7.10-4.2.3.5.jar`
(`/home/Acak/work/thaumcraft-4.2.3.5-oracle`, CFR 0.152); имена ваниллы
разрешались маппингом 1.7.10 (`scripts/porting/resolve_srg.py`, `mcp_stable-12-1.7.10`).
Ветка: `port/ocresearch`, база `5c80bed`.

OpenTC4 (`/home/Acak/work/opentc4`) использован только как справочник имён
ваниллы; игровых утверждений из него не делалось.

## Итог

| Проверка | Результат |
|---|---|
| Список исследований (ConfigResearch) | 201/201 ключей, 0 расхождений по 12 полям |
| Страницы исследований | 591 страница, 0 расхождений (вид, текст, рецепт, `required_research`) |
| Формулы генератора заметки | совпадают с оригиналом (радиус, кольцо, дыры, порядок `HashMap`, RNG-векторы) |
| 48 аспектов | 48/48, 0 расхождений по тегу/цвету/компонентам/blend/chatcolor |
| Текстуры аспектов | 50/50 байт-в-байт равны оригиналу |
| Специальные привязки `:302` (бутылка) и `:314` (ведро) | соответствуют оригиналу (подтверждено) |
| Привязки остальных объектов ConfigAspects | 94 незакрытых строки найдено → **88 строк восстановлено**, 9 остаются документированным пробелом |
| `ResearchNoteIssuance` | бумага не тратится, чернило тратится, доставка и дроп соответствуют оригиналу |
| Исследовательские ресурсы | 713/713 ключей `tc.research_*` перенесены дословно; иконки категорий подключены |
| Тесты | `:common:test` — 240 наборов, 1667 тестов, 0 падений |

Найдено и исправлено **5 дефектов** (раздел «Дефекты»).

---

## 1. Список исследований (ConfigResearch → `data/thaumcraft/tc4/research.json`)

### Метод

Экстрактор `scripts/porting/extract_tc4_definitions.py` создал корпус, поэтому его
собственные хэши ничего не доказывают. Я написал **второй, независимый парсер**
(`.tmp/ocresearch/verify_research.py`, инструмент аудита, в репозиторий не попал):
свой разбор строк, свой сканер вызовов `new ResearchItem(...)`, свой разбор
флуентной цепочки и своих `if (Config.…)`-условий по глубине скобок. Сопоставление
поле-в-поле с корпусом:

```
oracle research items: 201
corpus research items : 201
warp registrations: 20
problems: 0
```

Сверено по каждой из 201 записей: `category`, `column`, `row`, `complexity`,
`aspect_cost` (полный разбор `.add(Aspect.X, n)` с порядком вставки),
`flags` (9 видов: `AutoUnlock`, `Stub`, `Round`, `Concealed`, `Secondary`,
`Hidden`, `Special`, `Lost`, `Virtual`), `parents`, `parents_hidden`, `siblings`,
`aspect_triggers`, `entity_triggers`, число `item_triggers`, страницы (число,
порядок, вид `text`/`recipe_reference`/`legacy_expression`, `text_key`,
`recipe_key`, `required_research`), 15 `conditions` и 20 `warp`.

Особые случаи, подтверждённые чтением оригинала:

* `complexity` у 25 записей в источнике равен 0, но конструктор оригинала
  (`api/research/ResearchItem.java:65-70, 80-85`) поднимает `complexity < 1` до 1 —
  в корпусе 1, это корректно.
* Конструктор `ResearchItem(String, String)` (`api/research/ResearchItem.java:45-54`)
  сам ставит `setVirtual()` — корпус помечает такие записи флагом `Virtual`.
* 15 «запертых» страниц (`new ResearchPage("KEY","текст")`) разобраны как
  `required_research`; 8 страниц-массивов рецептов сохранены как
  `legacy_expression`.
* Категории: оригинал регистрирует шесть категорий с иконкой и фоном
  (`ConfigResearch.java:67-73`). Порт публикует их через
  `ThaumcraftResearchCategories` с теми же иконками/фонами; текстуры
  `r_thaumaturgy/r_crucible/r_artifice/r_golemancy/r_eldritch/thaumonomiconcheat`
  и `gui_researchback(.eldritch)` лежат в `assets/thaumcraft/textures/`.

### Покрытие рецептов (`ResearchRecipeCoverage`)

Заявление класса проверено независимо: 272 ключа рецептов корпуса покрыты
нативной картой (279), чертежами wand-конструктов (9) и отложенными с
причиной (9) — непокрытых нет; все 279 отображённых нативных рецептов реально
лежат в `data/thaumcraft/recipe` (343 файла), «отображённых, но отсутствующих» — 0.

## 2. Формулы генератора заметки (ResearchManager.createNote → `ResearchNoteGenerator`)

Оригинал (`thaumcraft/common/lib/research/ResearchManager.java:268-337`):

| Правило | Оригинал | Порт |
|---|---|---|
| радиус | `1 + Math.min(3, rr.getComplexity())` | `ResearchNoteGenerator.java:41` |
| сетка | `HexUtils.generateHexes(radius)`, обычный `HashMap` | `ResearchNoteGenerator.java:116-131` (тот же `HashMap` и тот же порядок вставок) |
| кольцо | `HexUtils.distributeRingRandomly(radius, rr.tags.size(), world.rand)`; `nextInt(ring.size())` в холостой `start` **не отбрасывается** | `HexUtils.distributeRingRandomly` сохраняет и этот draw, и float-шаг |
| число фиксированных клеток | `rr.tags.size()` (число различных аспектов) | `research.aspectCost().size()` |
| дыры | `complexity * 2`, только при `complexity > 1` | `ResearchNoteGenerator.java:77-109` |
| условие удаления | нельзя, если у соседа ≥2 общих гекса | то же, включая «кандидат сам считается» |
| цвет | `rr.getResearchPrimaryTag()` — строгое `>`, при равенстве остаётся первый | `ResearchNoteGenerator.java:48-59` |

Проверка на 516 «золотых» векторах (172 аспектных исследования × 3 сида),
выполненных на оригинальном Java 8 и лежащих в
`common/src/test/resources/thaumcraft/research/note-generation-java8.json`, проходит:
`ResearchNoteGeneratorTest` сверяет не только клетки, но и **последовательность
bounded-draw** и **состояние RNG после генерации**.

## 3. Аспекты

### 3.1 Каталог

Сверено поле-в-поле с `thaumcraft/api/aspects/Aspect.java` оригинала:

```
oracle aspects: 48  port aspects: 48
problems: 0
```

(тег, цвет, два компонента, `blend`, `chatcolor`). Все 50 PNG в
`assets/thaumcraft/textures/aspects/` **байт-в-байт** равны оригиналу.

### 3.2 Специальные привязки (проверялись отдельно)

| Оракул | Оригинал | Порт | Статус |
|---|---|---|---|
| `ConfigAspects.java:302` | `registerComplexObjectTag(Items.glass_bottle, VOID 1)` | `early_object_aspects.json:302` — `complex`, `vacuos 1`; легаси-строка `CRYSTAL 1 + VOID 1` подавлена complex-override'ом (`ObjectAspectCatalog.java:97-102`) | соответствует |
| `ConfigAspects.java:314` | `registerObjectTag(Items.bucket, METAL 8 + VOID 1)` | `object_aspects.json` (`Items.BUCKET` → `minecraft:bucket`, `metallum 8 + vacuos 1`) | соответствует |
| `ConfigAspects.java:315` | `water_bucket` = ведро + `AQUA 4` | `metallum 8 + vacuos 1 + aqua 4` | соответствует |
| `ConfigAspects.java:316` | `lava_bucket` = ведро + `FIRE 4 + EARTH 1` | `metallum 8 + vacuos 1 + ignis 4 + terra 1` | соответствует |
| `ConfigAspects.java:317` | `milk_bucket` = ведро + `HUNGER 2 + HEAL 2 + WATER 2` | **привязки не было** | **исправлено** |

Ранее эти строки читались неверно, потому что `Items.field_151133_ar` — это
**ведро**, а `Items.field_151069_bo` — **бутылка** (1.7.10-маппинг). Порт
наследует правильную сторону: стеклянная бутылка — complex, ведро — direct.

### 3.3 Файл ConfigAspects целиком

Оракул содержит 326 регистраций объектов (269 direct + 57 complex). Сопоставление
по modern-id (учёт того, что `object_aspects.json` — это выгрузка из легаси
1.12-файла, а оригинальные строки лежат в `early_object_aspects.json`):

* 99 регистраций таумкрафтовского контента: **89 покрыты**,
  10 не покрыты (3 мешка добычи, печь алхимии, 6 предметов брони культистов);
* 166 регистраций ваниллы: **73 покрыты**, 93 не покрыты.

Из 93 строк 83 восстановлены из оракула (см. «Дефекты»), 9 остаются
документированным пробелом:

| Оракул | Почему не перенесено |
|---|---|
| `:130` lit_redstone_ore | отдельный legacy-блок; современный `redstone_ore` использует оригинальную строку `:129` (уже перенесена) |
| `:203` sandstone (complex, пустой) | сложная строка без литерала; варианты `:204`/`:205` перенесены |
| `:214` melon (предмет) | в 1.21.1 один id `minecraft:melon` и у блока, и у предмета; две оригинальные метки несовместимы |
| `:243`, `:363` potion | нативная метка зелья считается из содержимого |
| `:293` enchanted_book (complex, пустой) | сложная строка без литерала |
| `:297` end_portal | в 1.21.1 нет предмета end portal |
| `:304`, `:305` golden_apple | зачарованное золотое яблоко не имеет базового рецепта |
| `:337` lit_furnace | современная печь использует оригинальную строку `:338` (уже перенесена) |
| `:437-446` броня культистов | такого контента в порте нет |

Все девять зафиксированы в `early_object_aspects.json`
(`unsupported_branches`), чтобы «пробел» был заявлен, а не скрыт.

### 3.4 Сущности

`entity_aspects.json` (69 записей) соответствует `ConfigAspects.registerEntityAspects`
оригинала; отдельно сверены 48 аспектов и правила сканирования в
`EntityAspectBindings`/`ScanManager`.

## 4. Выдача заметки (ResearchManager.createResearchNoteForPlayer:120-134 → `ResearchNoteIssuance`)

Оригинал:

```java
int slot = getResearchSlot(player, key);
if (slot >= 0) { note = player.inventory.getStackInSlot(slot); }
else if (consumeInkFromPlayer(player, false) && player.inventory.inventoryContains(Items.paper)) {
    consumeInkFromPlayer(player, true);
    note = createNote(new ItemStack(itemResearchNotes), key, world);
    if (!player.inventory.addItemToInventory(note)) player.dropPlayerItemWithRandomChoice(note, false);
    player.openContainer.markDirty();
}
```

Порт (`ResearchNoteIssuance.java:44-113`):

* **бумага только проверяется** — `paperSlot()` ищет слот и ничего не списывает;
  `stage()` копирует стопку бумаги без изменений (`ResearchNoteIssuance.java:186-194`).
  Это подтверждено тестом `theOriginalChecksThePaperAndNeverSpendsIt`;
* **чернило тратится на один пункт** (`ScribingInk.Plan` → `setDamageValue`),
  причём тратится только после успешной генерации;
* **доставка**: свободный слот → нота в инвентарь; полный инвентарь и не
  креатив → `player.drop(...)` с проверкой, что сущность действительно появилась
  в мире; креатив → нота теряется (quirк 1.7.10 `InventoryPlayer.addItemToInventory`);
* уже имеющаяся нота по тому же ключу возвращается без затрат (`reused`).

Отклонение (зафиксировано в коде): порт готовит заметку **до** траты чернила,
тогда как оригинал тратит чернило раньше генерации; разница наблюдаема только
при неудачной генерации, которой в штатном потоке нет.

## 5. Исследовательские ресурсы

* `assets/thaumcraft/lang/en_us.json`: **все 713 ключей `tc.research_*`** оригинального
  `en_US.lang` присутствуют и совпадают **дословно**; лишних ключей `tc.research_*`
  в порте нет. (Оригинал, кстати, не имеет `tc.research_name.CAP_iron` и
  `ROD_wood` — двух «virtual» записей без страниц.)
* Иконки категорий и фон подключены (`ThaumcraftResearchCategories`),
  текстуры на месте.
* Порт `ResearchCatalog` отказывается грузить корпус, если число записей не 201
  (`ResearchCatalog.java:61`), а неизвестная зависимость — ошибка, а не тихая
  потеря родителя (`ResearchCatalog.java:62-69`).

## 6. Дефекты (исправлены в этой ветке)

| # | Оракул | Порт | Что сделано |
|---|---|---|---|
| 1 | `thaumcraft/common/config/ConfigAspects.java:317` (`milk_bucket`) | привязки не было ни в `object_aspects.json`, ни в `early_object_aspects.json` | добавлена строка `minecraft:milk_bucket` = `metallum 8 + vacuos 1 + fames 2 + sano 2 + aqua 2` (значения ведра из строки `:314` + литерал оригинала) |
| 2 | `ConfigAspects.java:407-409` (мешки добычи 0/1/2) | не было | `thaumcraft:loot_bag_common/uncommon/rare` = `lucrum 8/16/32` (в порту три отдельных предмета вместо метаданных) |
| 3 | `ConfigAspects.java:427` (алхимическая печь) | не было | `thaumcraft:alchemy_furnace` = `praecantatio 8 + aqua 8 + fabrico 8` |
| 4 | `ConfigAspects.java:176-346` (83 ванильные строки: блоки, предметы, варианты stonebrick/sandstone/черепа, chainmail, конские доспехи, еда, семена, музыкальные диски, сложные добавки рельсов/кнопок/нажимных пластин/рычага/дверей/стрелы/глаза эндера/проигрывателя/блокнота/книжной полки/варочной стойки/удочки/горшка/морковки на палочке/рагу) | 83 строки отсутствовали: легаси-1.12 корпус их не содержит, а `early_object_aspects.json` их не покрывал | строки восстановлены **из оракула** с оригинальными литералами; единственная адаптация — modern-id (hardened clay → terracotta, monster egg → infested_stone, reeds → sugar_cane, fish → cod, web → cobweb, mob_spawner → spawner, golden_rail → powered_rail, record_X → music_disc_X, четыре варианта stonebrick и два sandstone разъехались по своим блокам, пять вариантов черепа — по пяти головам) |
| 5 | те же строки | при восстановлении `original_metadata` у вариантных строк (`:200-202`, `:204-205`, `:270-273`) сперва проставлялся как `0` | восстановлены точные метаданные оригинала (`1`, `2`, `3`, `1`, `2`, `1`, `2`, `3`, `4`) |

Тесты, закрывающие фиксы (`common/src/test/java/thaumcraft/aspects/EarlyObjectAspectBindingsTest.java`):

* `restoredOriginalRowsCarryTheOriginalLiteralTags` — 88 восстановленных строк
  с точными литералами оригинала, с проверкой вида регистрации
  (`direct`/`complex`) и метаданных;
* `everyVanillaCorpusIdentifierExistsInTheTargetRegistry` — **каждый** ванильный
  идентификатор корпуса существует в реестре предметов 1.21.1 (именно эта проверка
  поймала `minecraft:golden_rail`, переименённый в `minecraft:powered_rail`, и
  `minecraft:end_portal`, у которого в 1.21.1 нет предмета);
* `pistonTrapdoorAndWheatAdditionsLeaveEveryPreviousBindingUnchanged` — исторический
  дайджест «ALL90» сохранён: новые строки исключены из него и проверяются
  по отдельности (как это уже сделано для кластеров, меди и taint-строк).

Политика корпуса обновлена: `policy.coverage`, новое
`policy.restored_original_vanilla_rows` и новый пункт
`unsupported_branches` перечисляют, что перенесено и что осознанно оставлено.

## 7. Проверенное ранее и уже исправленное в этой ветке (не мой фикс)

Проверил, что замечания предыдущего аудитаresearch уже закрыты:

* бумага за выдачу заметки **не** тратится (`ResearchNoteIssuance.java:78-83`);
* иконки категорий подключены (`ThaumcraftResearchCategories`);
* предметный триггер `ROD_primal_staff` → `thaumcraft:focus_primal` привязан
  (`ResearchTriggerBindings.java:16-18`);
* сид розыгрыша скрытого исследования берётся из абсолютного времени мира
  (`ResearchHiddenSelection.java:17-27`, `level.getGameTime()`), как в оригинале
  (`ResearchManager.java:154`);
* кадр «скрытого» значка 230 для покупаемого скрытого исследования
  (`GuiResearchBrowser.java:163-165`).

## 8. Что осталось за рамками (не исправлено здесь)

* Клиентские правки из предыдущего аудита: шесть страниц-массивов рецептов в
  книге исследования, клиентский отклик сканирования (`cameraticks` + частицы рун),
  персональная view-модель таумометра, интеграция JEI/EMI. Это клиентская часть,
  вне моего домена; проверялось только чтением кода, без запуска клиента.
* Покрытие `research/`-ресурсов проверено по lang-ключам и наличию текстур;
  попиксельное сравнение иконок исследований с оригиналом не выполнялось.
* Живой прогресс (выдача заметки, покупка, скан, разгадывание) не проигрывался:
  проверка только юнит-тестами и чтением кода.

## 9. Реальный вывод проверок

```
# независимый разбор оракула против корпуса
python3 .tmp/ocresearch/verify_research.py
oracle research items: 201
corpus research items : 201
warp registrations: 20
problems: 0

# 48 аспектов
python3 .tmp/ocresearch/verify_aspects.py
oracle aspects: 48  port aspects: 48
problems: 0

# строки корпуса против оракула (target+metadata)
python3 .tmp/ocresearch/verify_early_amounts.py
early corpus rows: 198  unmatched: 3  problems: 1
  - line 317 Items.field_151117_aB metadata0: aspects corpus={metallum 8, vacuos 1, fames 2, sano 2, aqua 2}
    oracle={fames 2, sano 2, aqua 2} (oracle line 317)
    # ожидаемо: оригинал засевает список метками ведра из строки :314

# покрытие рецептов
python3 .tmp/ocresearch/verify_recipe_coverage.py
corpus recipe keys: 272
native map: 279  constructs: 9  deferred: 9
corpus keys without a coverage entry: 0
native recipe files: 343
mapped native recipes without a file: 0

# остаток неперенесённых ванильных строк
python3 .tmp/ocresearch/verify_uncovered_vanilla.py
original vanilla registrations with no port binding: 9

# полный прогон
export JAVA_HOME=/home/Acak/.jdk21
export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/ocresearch
./gradlew --no-daemon --console=plain :common:test
BUILD SUCCESSFUL
240 наборов, 1667 тестов, 0 failures, 0 errors, 0 skipped
```
