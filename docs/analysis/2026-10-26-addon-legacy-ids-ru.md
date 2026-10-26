# 26.10.2026 — Оригинальные имена предметов и блоков TC4 для аддонов

## Задача

Аддон для TC4 1.7.10 обращался к содержимому Thaumcraft двумя путями: через API
(`ItemApi.getItem("itemResource", 2)`, `ItemApi.getBlock("blockCosmeticSolid", 4)`) и напрямую через поля
`thaumcraft.common.config.ConfigItems` / `ConfigBlocks` с метаданными (`new ItemStack(ConfigItems.itemShard, 1, 6)`).
До этой работы `ItemApi` превращал имя в `thaumcraft:<имя>`: `"itemResource"` — недопустимый путь
`ResourceLocation`, вызов бросал `ResourceLocationException`, а метаданные шли в износ предмета. Оригинальные
вызовы API не работали.

## Метод

1. **Байткод оригинала.** `scripts/porting/legacy_bytecode.py` копирует классы `thaumcraft/` из
   `Thaumcraft-1.7.10-4.2.3.5.jar` во временный каталог, дизассемблирует их `javap -c -l -p` (в памяти, в
   репозиторий ничего не пишется, JAR не меняется) и берёт:
   * все `public static` поля типа `Item`/`Block` классов `ConfigItems` (111) и `ConfigBlocks` (45), класс, из
     которого строится каждое поле, его unlocalized-имя и имя `GameRegistry`;
   * метаданные из `getSubItems`/`getSubBlocks` (SRG `func_150895_a`/`func_149666_a`) класса поля и его
     родителей — метод исполняется маленьким интерпретатором: целые локальные переменные и константы конкретны,
     переключатели `Config.found*` (медь/олово/серебро/свинец других модов) проходятся в обе стороны и
     записываются как условие значения, циклы по реестрам и массивам без известной длины помечаются как
     динамические;
   * каждую `new ItemStack(ConfigX.поле, n, meta)` с константными метаданными во всех классах оригинала
     (рецепты, исследования, аспекты, лут, сущности);
   * каждую установку блока в мир `setBlock(..., ConfigBlocks.поле, meta, ...)` с константными метаданными
     (`World.func_147465_d`, `func_147449_b`, `WorldGenerator.func_150516_a`,
     `StructureComponent.func_151550_a`).
2. **Языковой файл оригинала** (`thaumcraft_src/assets/thaumcraft/lang/en_US.lang`, уже в репозитории):
   ключи `item.<имя>.<meta>.name` / `tile.<имя>.<meta>.name` — независимый источник метаданных и столбец
   «Name».
3. **Сопоставление** — таблица `MAPPING` в `scripts/porting/extract_legacy_ids.py`: для каждого поля и
   метаданных — нативный id, состояние блока, предметная форма блока, компонент данных или `null` с причиной.
   Решения сверены с нативным кодом (регистрации `ThaumcraftContent`, `ThaumcraftEldritchBlocks`,
   `EldritchBlockRules`, `ThaumcraftLegacyTiles` и др.). Где у порта есть и «семейный» блок с `variant`
   (для генерации Outer Lands), и отдельный игровой блок, выбран игровой (тот, что дают рецепты):
   `blockCosmeticSolid:4` → `thaumium_block`, а не `cosmetic_solid_thaumium[variant=4]`; `blockAiry:0/1/3/5` →
   `aura_node`/`nitor`/`arcane_light`/`energized_node`; `blockSlabWood:0` → `greatwood_slab`.
4. **Проверки скрипта** (сбой, если нарушено):
   * значение из любого источника (подтипы, использование, мир, lang) без строки в `MAPPING`;
   * строка `MAPPING` без источника и без ссылки `evidence`; ссылка `evidence` проверяется по
     LineNumberTable класса оригинала;
   * расхождение с уже существующими частичными картами порта: `ResearchIconDisplay` (все списки и
     `switch`, 236 пар поле × метаданные), `ObjectAspectCatalog.MATERIAL_IDS/SHARD_IDS/ORE_IDS`,
     `early_object_aspects.json` — кроме перечисленного в `RECONCILED` (одно место, см. «Отклонения»);
   * без JAR: каждый нативный id есть в нативном `en_us.json`, каждое `свойство=значение` есть в blockstate
     этого блока.
5. **Данные.** `common/src/main/resources/data/thaumcraft/tc4/legacy_ids.json` (`schema_version` 1, `source`,
   `policy`, `counts`, `fields`, `rows`). Строка: `kind`, `field`, `meta`, `id`, у блоков `state` и `item`,
   при необходимости `component`, `name` (оригинальное английское имя), `condition`
   (`Config.found*`), `note`, `evidence`. Строки лежат по одной на строку файла (однострочные диффы).
   `meta` 32767 (`OreDictionary.WILDCARD_VALUE`) есть у каждого поля: один id, если все метаданные поля
   ведут к нему, иначе `id: null` и список `ids`. Доказательства — строки исходника оригинала из
   LineNumberTable (`ItemResource.getSubItems:118`, `ConfigRecipes:3312`), ключ lang, `(world)` для установки
   в мир. Это строки классов JAR, а не строки декомпиляции-оракула.
6. **Справочник** `docs/api/LEGACY_IDS.md` (англ.) генерируется тем же скриптом из JSON; `--check` падает,
   если JSON или справочник устарели. Без JAR `--check` пересобирает справочник из закоммиченного JSON и
   выполняет все проверки, которым JAR не нужен; повтор по байткоду тогда сообщается как пропущенный.

## API

* `ItemApi.getItem(name, meta)` / `getBlock(name, meta)`: сначала таблица (только поля `ConfigItems` для
  `getItem` и `ConfigBlocks` для `getBlock`, как в оригинальной рефлексии), затем прежнее поведение для
  нативных id (`"thaumium_ingot"`, `"thaumcraft:thaumium_ingot"`), затем рефлексия, затем оригинальное
  предупреждение и `null`. Неизвестное или некорректное имя больше не бросает исключение (раньше
  `"itemResource"` давал `ResourceLocationException`); оригинал тоже никогда не бросал.
  Поле без подтипов (одна строка, meta 0) отвечает на любые метаданные, и метаданные остаются износом
  повреждаемого предмета, как `new ItemStack(item, 1, meta)` оригинала. Для блочной строки с состоянием,
  отличным от состояния по умолчанию, когда предмет — собственный предмет блока, стек получает
  `minecraft:block_state`, и установка даёт состояние оригинала (верхняя плита, вариант косметического
  камня, активные путы). Остальные компоненты только описаны в таблице.
* Новое: `ItemApi.legacyItemId`, `legacyBlockId`, `legacyItemIds` (раскрытие wildcard), `legacyBlockState`
  (из живого реестра), `isLegacyName`, `legacyNames`, `legacyMetadata`, константа `ItemApi.WILDCARD`.
* `ItemsTC.id/get/require/stack/isRegistered` принимают оригинальные имена полей (meta 0, как
  `new ItemStack(ConfigItems.поле)`), `ItemsTC.get(name, meta)`; `BlocksTC.id/get/require/stack` —
  имена полей `ConfigBlocks`, `BlocksTC.get(name, meta)`, `BlocksTC.state(name, meta)`. Договор нативных
  имён (null/пустое → `IllegalArgumentException`, некорректный простой путь → `ResourceLocationException`)
  не изменён; оригинальное имя поля исключения не бросает.
* Таблица читается лениво (holder-идиома) из classpath при первом обращении, без клиента и без игры;
  нативные объекты берутся из реестров в момент вызова. Сломанный ресурс логируется, оригинальные имена
  тогда просто не находятся.
* `ResearchIconDisplay` не переведён на таблицу: его результат зависит от зарегистрированных предметов, и в
  модульном тесте без игры равенство не доказать. Вместо этого Python-проверка доказывает, что для каждого
  псевдонима `ResearchIconDisplay` таблица даёт тот же предмет (0 расхождений).

## Покрытие

| | Количество |
|---|---|
| Поля `ConfigItems` / `ConfigBlocks` (все `public static Item/Block`) | 111 / 45 = **156** |
| Пары поле × метаданные (без wildcard) | **472** (267 предметных, 205 блочных) |
| — с нативным id | **463** (390 разных id; 1 ванильный: `itemNugget:0` → `minecraft:iron_nugget`) |
| — `null` с причиной | **9** |
| Строки wildcard (32767) | 156: 109 с одним id, 44 со списком `ids`, 3 без аналога |
| Блочные строки с состоянием (`state`) | 86 |
| Блочные строки без предметной формы (`item: null`) | 21 (жидкости, пустота портативной дыры, части мультиблоков, `blockAiry`, стручок, `warded`) |
| Строки с описанным компонентом данных | 47 |
| Строки с условием `Config.found*` | 11 |

`null` (причина в `note`): `itemWandCap:4/5` (серебряные колпачки — нужно серебро другого мода, в порте нет),
`itemPhotoPlate:0`, `itemFlyingCarpet:0`, `itemBootsFortress:0` (поля объявлены, но 4.2.3.5 их не
присваивает — в оригинале они `null`), `itemSpawnerEgg:3/5/6/11` (Firebat, MindSpider, EldritchGuardian,
CultistPortal: сущности в порте есть, яиц призыва нет).

Медь/олово/серебро/свинец: самородки и кластеры (`itemNugget:1-4,17-20`) и медный колпачок (`itemWandCap:3`)
в порте есть как нативные предметы, поэтому они **сопоставлены** (не `null`), с условием оригинала
`Config.found*`.

## Проверки

| Проверка | Результат |
|---|---|
| `python3 scripts/porting/extract_legacy_ids.py --jar <корень>/Thaumcraft-1.7.10-4.2.3.5.jar` | 156 полей, 472 строки (463 / 9), 156 wildcard; записаны 2 файла |
| `python3 scripts/porting/extract_legacy_ids.py --check` (JAR найден) | код 0 |
| `python3 scripts/porting/extract_legacy_ids.py --check --jar /nonexistent.jar` | код 0, «bytecode replay skipped» |
| `python3 -m unittest scripts/porting/test_extract_legacy_ids.py -v` (с JAR) | **10/10** |
| `TC4_ORIGINAL_JAR=/nonexistent python3 -m unittest scripts/porting/test_extract_legacy_ids.py` | 10, OK (**2 пропущены** — повтор по байткоду) |
| `python3 -B -m unittest discover -s scripts/porting -p 'test_extract_legacy_ids.py'` (как в CI) | 10/10 |
| `./gradlew :common:test --tests 'thaumcraft.api.LegacyIdsTest' --no-daemon` | **11/11** (первый прогон: 10/11, см. ниже) |
| `./gradlew :common:test --tests 'thaumcraft.api.*' --no-daemon` | **139/139**, 25 классов |
| `./gradlew :common:test --no-daemon` | **2604 теста, 0 падений, 5 пропущено** (416 классов; задача `:common:test` выполнена, не из кэша) |
| `./gradlew :fabric:build --no-daemon` | **BUILD SUCCESSFUL**; `Thaumcraft-fabric-0.1.0-alpha.7.jar` содержит `data/thaumcraft/tc4/legacy_ids.json` и `thaumcraft/api/ItemApi$Legacy*` |
| Выделенный сервер / клиент Fabric, NeoForge | не запускались |

`LegacyIdsTest`: каждая строка называет зарегистрированный нативный объект. Модульные тесты не запускают
игру, поэтому id содержимого порта читаются из тех же `DeferredRegister` всех классов
`thaumcraft.common.config`, которые регистрирует `Thaumcraft.init()`; ванильные id — из бутстрапнутых
ванильных реестров. Первый прогон нашёл 4 строки (`arcane_furnace`, `wand_pedestal`,
`wand_pedestal_focus`): `ThaumcraftLegacyTiles.register()` добавляет эти три предмета в реестр уже внутри
`register()`, статически их не видно. Тест принимает именно эти три с проверкой, что они всё ещё
регистрируются там и что модели есть. Каждое `свойство=значение` проверено по blockstate-файлу; два
свойства, которые модели не различают (`advanced_alchemical_furnace part`, `death_fluid level`), проверены
по `Property` классов блоков. Семейство eldritch, которое тестовая платформа регистрирует настоящими
блоками (`EldritchTestRegistry`), проверено как реальные `BlockState` для `blockEldritch:0..10`.
Точечные проверки: `itemResource:2` → слиток таумия, `itemShard:6` → уравновешенный осколок,
`blockJar:0` → `jar` (Warded Jar), метаданные `blockCosmeticSolid`, `itemNugget:0` → настоящий стек
`minecraft:iron_nugget`, wildcard, неизвестные имена → `null` без исключений, нативные имена работают как
раньше.

Не проверялось: работа в запущенной игре (выделенный сервер, клиент) — таблица и API не меняют поведения
игры, пока аддон их не вызывает; реальная установка стека с `minecraft:block_state` в мире не
запускалась. Это **не проверено**, а не «пройдено».

## Отклонения

* `ConfigBlocks.blockAlchemyFurnace` — это Advanced Alchemical Furnace (`tile.blockAlchemyFurnace.name`),
  таблица ведёт его к `advanced_alchemical_furnace` (части 1–4 — состояния `part`). Корпус
  `early_object_aspects.json` привязывает теги `ConfigAspects:492` этого поля к `thaumcraft:alchemy_furnace`
  (печь `blockStoneDevice:0`). Это код аспектов, его меняют другие — расхождение записано в `RECONCILED`
  скрипта и оставлено владельцам аспектов.
* `itemNugget:0` (железный самородок TC4) → ванильный `minecraft:iron_nugget`: своего в порте нет.
  `ResearchIconDisplay` для него иконки не даёт (он разрешает только id `thaumcraft:`), это не расхождение.
* `blockTable:1` — в оригинале простой стол в другой ориентации, но предметный рендер оригинала рисует
  метаданные 1 как исследовательский стол, и единственное использование — иконка `ConfigResearch:647`.
  Предметная форма — `research_table` (как в `ResearchIconDisplay`), состояние — `table[variant=1]`.
* `blockMetalDevice:10/11`: состояние — `thaumatorium`/`thaumatorium_top`, предметная форма —
  `alchemical_construct` (оригинальный `damageDropped`), как в `ResearchIconDisplay`.
* Двойные плиты (`blockDoubleSlab*`) сопоставлены состоянию `type=double` игровых плит, а не блокам
  `cosmetic_double_slab_*` семейства Outer Lands.
* `ItemApi.getItem/getBlock` больше не бросают `ResourceLocationException` для некорректного простого
  имени (возвращают `null` с оригинальным предупреждением). `getBlockItem` и договор `ItemsTC`/`BlocksTC`
  для нативных имён не менялись.
