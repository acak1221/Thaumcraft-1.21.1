# Сверка алхимии и крафта оригинала TC4 4.2.3.5 с портом 1.21.1 (OpenTC4-референс)

**Домен:** рецепты и их исполнение — `thaumcraft/common/crafting/**`, контейнеры
`thaumcraft/common/container/**`, блоки/тайлы алхимической печи, инфузионного стола, арканной
верстака, центрифуги, тауматория, сверла и столa деконструкции.
**Дата:** 2026-10-21. **Ветка:** `port/occrafting`. **Worktree:** `/home/Acak/tcw/occrafting`.

**Оракул (истина):** `/home/Acak/work/thaumcraft-4.2.3.5-oracle/` — декомпиляция
`Thaumcraft-1.7.10-4.2.3.5.jar` (CFR 0.152).
**Читаемый референс имён ваниллы:** `/home/Acak/work/opentc4/src/main/java/thaumcraft/...`
(тот же мод в читаемом виде) и SRG-резолвер
`python3 -B scripts/porting/resolve_srg.py <имя>` (mcp-stable 12-1.7.10).
**База:** ветка до fast-forward на `work` = `11c5f4b` (последний коммит аудита предметов);
ветка `port/occrafting` была на `5c80bed`, поэтому она подтянута вперёд (`git merge --ff-only work`),
иначе сверка шла бы не по последнему коду.

---

## 1. Итог

| Метрика | Значение |
|---|---|
| Дефектов найдено и исправлено | **3** (все — один узел оригинала: «доплата при переплавке») |
| Из них: отсутствовавшее поведение | 1 (каталог + подключение) |
| Из них: отсутствовавший путь чтения | 1 (fallback по имени руды) |
| Из них: вынужденная адаптация 1.21.1 | 1 (количество «0» в реестре) |
| Механически сверено рецептов оригинала | **212** (аспекты, нестабильность, число компонентов, узоры) + **48** тигельных + **64** обычных |
| Расхождений в сверенном — | **0** |
| Пробелов, найденных и НЕ исправленных | 4 (см. §5) |
| `:common:test` | **1683 теста, 0 failures, 0 errors, 0 skipped** (243 suite) |
| `:fabric:runGameTest` | прогон состоялся; **новый сценарий бонуса переплавки прошёл**; 8 падений — базовые и флаки (§6) |

Ключевая находка группы: **в порту никогда не был зарегистрирован каталог «доплаты при
переплавке» оригинала и арканная печь никогда её не платила.** Реестр
`ThaumcraftApi.addSmeltingBonus` был объявлен, но не наполнялся ни одной записью, а
`TileArcaneFurnace.setSmeltingBonus` не вызывался никогда. В оригинале арканная печь при выбросе
переплавленного предмета платит ещё и самородок: одна единица с шансом 1/4 без кузнечных мехов, по
одному броску на каждый мех с шансом 44 %.

---

## 2. Дефекты: оракул → порт → фикс

### D1. Каталог «доплаты при переплавке» не зарегистрирован; арканная печь не платит бонус

* **Оракул** `thaumcraft/common/config/ConfigRecipes.java:493-510` — восемнадцать регистраций
  `ThaumcraftApi.addSmeltingBonus(...)`: семь по имени руды (`oreGold`, `oreIron`, `oreCinnabar`,
  `oreCopper`, `oreTin`, `oreSilver`, `oreLead`), семь по точному предмету (кластеры золота/железа/
  киновари/меди/серебра/свинца и четыре сырых мяса), и по «сырой рыбе» с wildcard-метаданными
  `Short.MAX_VALUE`.
* **Оракул** `thaumcraft/common/tiles/TileArcaneFurnace.java:237-259` — арканная печь при выбросе
  результата платит бонус: `getSmeltingBonus(furnaceItemStack)`, затем при `bellows == 0` бросок
  `nextInt(4) == 0`, иначе по одному броску на мех с `nextFloat() < 0.44f`; выброшено только если
  количество > 0.
* **Порт (до фикса):**
  * `common/src/main/java/thaumcraft/api/ThaumcraftApi.java:79-103` — реестр `smeltingBonus`
    существует, но **ни одного вызова `addSmeltingBonus` во всём проекте нет** (`grep` по
    `common/src`, `fabric/src`, `neoforge/src` — ноль вызовов);
  * `common/src/main/java/thaumcraft/common/tiles/TileArcaneFurnace.java:43-48` — провайдер
    `bonusProvider` по умолчанию `input -> null`, а `setSmeltingBonus(...)` **не вызывается ни
    разу**;
  * следствие: переплавка руды/кластера в арканной печи никогда не даёт самородок, то есть
    цепочка «тигель → кластер → два слитка + самородок» оригинала была урезана.
* **Фикс:**
  * новый корпус `common/src/main/resources/data/thaumcraft/tc4/smelting_bonus.json` — 12 живых
    записей с исходной строкой оригинала и исходной регистрацией в поле `original`, плюс 6
    документированных пропусков (олово/серебро/свинец — в 1.21.1 этих металлов нет);
  * новый загрузчик `common/src/main/java/thaumcraft/common/lib/crafting/SmeltingBonusCatalog.java`
    — парсит корпус на старте мода (без резолва предметов: реестр ещё не заморожен) и отдаёт
    предметы по id только по запросу;
  * `common/src/main/java/thaumcraft/api/ThaumcraftApi.java` — `loadOriginalSmeltingBonuses()`
    складывает корпус в тот же реестр (без перезаписи уже зарегистрированных аддонных записей),
    поэтому приоритет оригинала сохраняется: точная запись важнее записи по имени руды;
  * `common/src/main/java/thaumcraft/common/Thaumcraft.java` — `TileArcaneFurnace.setSmeltingBonus(
    ThaumcraftApi::getSmeltingBonus)` и `SmeltingBonusCatalog.load()` при старте;
  * `common/src/main/java/thaumcraft/common/tiles/TileArcaneFurnace.java:252-268` — выплата
    бонуса (число бросков и шансы уже были в `LegacyTileRules.bonusRolls/bonusChance`, сверено с
    оригиналом).

### D2. `getSmeltingBonus(ItemStack)` никогда не читал записи, зарегистрированные по имени руды

* **Оракул** `thaumcraft/api/ThaumcraftApi.java:88-99` — порядок поиска: точная пара
  `(item, damage)`, затем wildcard `(item, Short.MAX_VALUE)`, затем **имя первой руды**, в
  которую входит предмет: `OreDictionary.getOreName(OreDictionary.getOreID(stack))`.
* **Порт (до фикса):** `getSmeltingBonus` знал только первые два шага. Запись, зарегистрированная
  через `addSmeltingBonus(String, ItemStack)`, была недостижима — это уже было зафиксировано
  собственным аудитом порта: `docs/analysis/2026-10-21-audit-a9-core.md:92-93` и
  `docs/porting/scope-ledger.json:670` («registers a name-keyed entry that getSmeltingBonus can
  never return»). Но вывод «нечитаемо» был неполным: без fallback'а не срабатывали бы и семь
  оригинальных записей по имени руды, то есть каталог из D1 всё равно был бы мёртвым.
* **Фикс:** `common/src/main/java/thaumcraft/common/lib/utils/OriginalOreTags.java` — новый
  `firstOreName(ItemStack)`: первый **в порядке регистрации** оригинальный ore-name, чей
  нативный тег содержит предмет (порядок оригинальных имён руды — порядок регистрации в
  `OriginalOreTags`, `LinkedHashMap`). `ThaumcraftApi.getSmeltingBonus` получил третий шаг
  оригинала.

### D3. Количество «0» в записи реестра не переносится в 1.21.1 как есть

* **Оракул** `thaumcraft/api/ThaumcraftApi.java:80-86` — реестр хранит
  `new ItemStack(out.getItem(), 0, out.getItemDamage())`: количество всегда ноль, потому что
  количество решает сама печь своими бросками по мехам.
* **Порт (до фикса):** `addSmeltingBonus` сохранял количество, которое задал аддон.
* **Почему нельзя просто перенести ноль:** в 1.21.1 `ItemStack.copy()` стека с количеством 0
  возвращает общий синглтон `ItemStack.EMPTY`, то есть запись реестра потеряла бы предмет и
  превратилась бы в воздух. Это подтвердилось прогоном: первая версия фикса (количество 0)
  уронила существующий тест `CraftingApiTest.theSmeltingBonusRegistryAnswersTheOriginalQuery` —
  `expected: <minecraft:iron_ingot> but was: <minecraft:air>`.
* **Фикс (вынужденная адаптация, задокументирована в javadoc):** реестр сохраняет количество
  аддона; выплата в печи строится из предмета (`new ItemStack(bonus.getItem(), count)`), а не из
  копии записи, поэтому запись без количества не может превратиться в общий пустой стек.
  Количество, которое платит печь, по-прежнему определяется только бросками по мехам.

---

## 3. Что сверено и совпало

### 3.1 Рецепты алхимии (тигель) — `ConfigRecipes.initializeAlchemyRecipes`, строки 105-181

* Машинная сверка **всех 48 регистраций тигля**: ключ исследования, аспекты, результат,
  катализатор, число, порядок регистрации — расхождений нет.
* Порядок тигля в порту совпадает с порядком оригинала при «медь есть, олово/серебро/свинец нет»:
  `BalancedShard_0..5` → `Alumentum` … `PureCopper` → `TransIron` → `TransGold` → `TransCopper` →
  `EtherealBloom` → `LiquidDeath` → `BottleTaint` → восемь `Golem*` → семь `Core*` →
  `BathSalts` → `SaneSoap`. Медные рецепты (`pure_copper`, `trans_copper`) стоят именно там, где
  их место в оригинале: `PureCopper` — строка 134 (после `PureGold`), `TransCopper` — строка 148
  (после `TransGold`).
* Поле `source_order` в порту неоднородно: у первых 28 рецептов это индекс регистрации
  (0…27), у остальных — номер строки оригинала (159…180). **Это не дефект**: значения строго
  возрастают в порядке оригинала, а тигель выбирает первую зарегистрированную запись из
  равных по числу разных аспектов (`ThaumcraftCraftingManager.java:127-145`), то есть
  монотонность — единственное свойство, которое нужно.
* Катализаторы, проверенные по SRG-резолверу: `Items.field_151044_h` = `coal` (у `Alumentum`
  wildcard-метаданные, значит и уголь, и древесный уголь — порт перечисляет оба),
  `Items.field_151133_ar` = `bucket` (у `LiquidDeath` это именно ведро, а не бутылка;
  подтверждено аспектами `ConfigAspects.java:314-317`), `Items.field_151042_j` = `iron_ingot`
  (у `Thaumium`), `Items.field_151014_N` = `wheat_seeds` (у `VoidSeed`),
  `ConfigItems.itemNugget` metadata 0 = самородок железа, 1 = меди, 16/17/31 = кластеры
  железа/меди/золота (`ConfigItems.java:276-293`).

### 3.2 Остальные рецепты — 212 регистраций машинно

Скрипт сверки (`.tmp/compare_recipes.py`, не в коммите) сопоставляет каждую регистрацию
`addCrucibleRecipe` / `addArcaneCraftingRecipe` / `addShapelessArcaneCraftingRecipe` /
`addInfusionCraftingRecipe` / `addInfusionEnchantmentRecipe` оригинала с нативным JSON через
карту «оригинальный ключ → нативный рецепт» самого порта
(`ResearchRecipeCoverage.buildNativeRecipeIds`). Сверялись: **аспекты (после подстановки
`WandCap/WandRod.getCraftCost()`), нестабильность инфузии, число компонентов, узоры
арканных рецептов, наличие рецепта**. Расхождений нет ни в одном из 212 случаев.

Отдельно проверены цены крафта палочек/колпачков/посохов оригинала
(`ConfigItems.java:302-323`: железо 1, золото 3, таумиум 6, пустота 9, медь 2, серебро 4;
дерево 1, greatwood 3, обсидиан/блейз/лёд/кварц/кость/тростник 6, серебряное дерево 9;
посохи greatwood 8, остальные 14, серебряное дерево 24, первобытный 32) — совпадают с портом.

### 3.3 Обычные рецепты — 64 регистрации `initializeNormalRecipes`

Сверены результаты и форма: 9 самородков железа/таумия/пустоты/меди/квиксибелера ↔ слитки,
кластеры (6 + смешанный), гирлянды/свечи, одежда таумия и пустоты, тотем, фонарь, инквель,
фармацевтические колбы, подставки, лестницы/плиты greatwood и silverwood. Все 64 имеют
нативный аналог; добавленные портом `copper_nuggets.json` и `copper_ingot_from_nuggets.json` —
это 1.21.1-эквивалент ванильных рецептов меди, которых в 1.7.10 не было (меди не было в
ванилле 1.7.10 вообще).

### 3.4 Механизмы

* **Алхимическая печь** (`TileAlchemyFurnace`): ёмкость 50 vis, `smeltTime = (int)((float)(vis*10) * (1 - 0.125*bellows))` — совпадает; обогрев мехами `count % (speedBoost ? 20 : 40)`; alumentum даёт ускорение; стороны доступа (снизу — топливо, сверху — ничего, сбоку — вход) и правило «ведро можно вынуть снизу» — совпадают; перенос аспектов в алебтики двумя проходами (сначала долив, потом одна пустая) — совпадает; свет 12 при горении, частицы рта из четырёх mouth-точек — совпадает.
* **Инфузионный стол** (`TileInfusionMatrix`): нестабильность `symmetry + recipeInstability`, цикл `count % countDelay` (10/20), оплата эссенции по 1 за цикл с ростом нестабильности при нехватке (`nextInt(100 - recipeInstability*3)`, потолок 25), «просрочка» рецепта при отсутствии игрока рядом, 21 исход катастрофы — совпадают.
* **Арканная верстака**: потребление — `vis * 100` (центivis), затем модификатор колпачка/скидки с полом 0.1; гейт по исследованию через `KnowledgeAccess`; превращение обычного стола в арканную верстак палочкой (в оригинале `BlockTable.onWandRightClick`, `:224-238`) — в порту `BlockArcaneWorkbench.tryCreate`, палочка кладётся в слот 10 и снимается с руки, даже в креативе.
* **Центрифуга**: 39 тактов обработки, добор эссенции раз в 5 тактов, скорость вращения +2 / −0.5, звук насоса на 160-180° — совпадает.
* **Тауматорий**: нагрев раз в 40 тактов (и на нулевом счётчике), работа раз в 5 тактов, ёмкость рецептов по апгрейдам — совпадает.
* **Сверл и стол деконструкции** — числа оригинала сверены точечно (счётчик, задержка, порог `heat`), расхождений нет.

### 3.5 Контейнеры

Размеры и быстрые перемещения сверены с оригиналом по всем 13 контейнерам
(`thaumcraft/common/container/**`):

| Контейнер | Оригинал | Порт |
|---|---|---|
| `ContainerAlchemyFurnace` | 38 слотов (2 + 27 + 9), quick-move: топливо → 1, затем 0; вход → 0; из инвентаря 2…29 → 29…38 и наоборот | совпадает |
| `ContainerArcaneWorkbench` | 47 (результат + палочка + 9 + 27 + 9), shift из 11…38 → 38…47, из 38…47 → 11…38 | совпадает |
| `ContainerDeconstructionTable` | 37 (1 + 27 + 9), предмет с аспектами → 0, остальное 28…37 / 1…28 | совпадает |
| `ContainerThaumatorium` | 37 (1 + 27 + 9), из слота 0 → 1…37, из инвентаря → 0…1 | совпадает |
| `ContainerArcaneBore` | 38 (2 + 27 + 9), фокус → 0, кирка → 1 | совпадает |
| `ContainerFocalManipulator` | 37, фокус → 0 | совпадает |
| `ContainerResearchTable` | 2 + инвентарь, слот < 2 → в инвентарь | совпадает |
| `ContainerSpa` | 37, слот 0 → в инвентарь, инвентарь → 0 | совпадает |
| `ContainerFocusPouch` | 54 (18 + 36), слот < 18 → в инвентарь | совпадает |
| `ContainerMagicBox` | граница по числу рядов | совпадает |
| `ContainerHandMirror` | слот 0 → остальное, остальное → 0; предмет `ItemHandMirror` не вынимается | совпадает |
| `ContainerHoverHarness` | слот блока заперт, остальное — как слот 0 | совпадает |
| `ContainerGhostSlots` | в оригинале нет override — используется ванильный | в порте ghost-слоты исключены явно (безопаснее) |

---

## 4. Что изменено в порту (файлы)

| Файл | Изменение |
|---|---|
| `common/src/main/resources/data/thaumcraft/tc4/smelting_bonus.json` | **новый** — корпус оригинальных 18 регистраций (12 живых + 6 документированных пропусков) |
| `common/src/main/java/thaumcraft/common/lib/crafting/SmeltingBonusCatalog.java` | **новый** — загрузчик корпуса, отложенный резолв предметов |
| `common/src/main/java/thaumcraft/api/ThaumcraftApi.java` | складка оригинального каталога в реестр + fallback по имени руды + javadoc о вынужденном отличии по количеству |
| `common/src/main/java/thaumcraft/common/lib/utils/OriginalOreTags.java` | `firstOreName(ItemStack)` — замена оригинального `OreDictionary.getOreName(getOreID(...))` |
| `common/src/main/java/thaumcraft/common/tiles/TileArcaneFurnace.java` | выплата бонуса при выбросе; стек бонуса строится из предмета |
| `common/src/main/java/thaumcraft/common/Thaumcraft.java` | подключение провайдера бонуса и загрузка корпуса; перезагрузка корпуса на `SERVER_STARTED` (когда привязаны теги датапака) |
| `common/src/test/java/thaumcraft/crafting/SmeltingBonusParityTest.java` | **новый** — 4 теста, прикалывают корпус к оригиналу |
| `common/src/gameTest/java/thaumcraft/common/testing/SmeltingBonusRuntimeChecks.java` | **новый** — runtime-сценарий на живом сервере |
| `fabric/src/gameTest/java/thaumcraft/fabric/testing/ThaumcraftGameTests.java` | новый сценарий `theOriginalSmeltingBonusesAreRegistered` |
| `scripts/porting/required-gametests.json` | новый обязательный кейс |

---

## 5. Пробелы (найдены, НЕ исправлены — и почему)

* **G1. `RunicGirdleKinetic_2` (строка 356) не отличим от `RunicGirdleKinetic` (строка 355).**
  Оригинал требует зелье с метаданными `16428` и `24620`; в 1.21.1 у зелья нет метаданных, и оба
  сворачиваются в `minecraft:splash_potion` + `strong_harming`. Поэтому два нативных рецепта
  `infusion/runic_girdle_kinetic.json` и `…_2.json` побайтово одинаковы, а второй оригинальный
  ключ недостижим. Это задокументированное решение порта
  (`docs/analysis/2026-10-20-native-accessories.md:17`), и исходная регистрация в 1.7.10 тоже
  мертва: лут-таблица оригинала (`Config.java:552-568`) выдаёт только формы `i | 0x2000`
  (обычное зелье) и `i | 0x4000` (взрывное), а `24620 = 0x602C` несёт оба флага сразу и
  получается в игре невозможно.
* **G2. Шесть оригинальных регистраций по олову/серебру/свинцу** (`ConfigRecipes:497-499`,
  `504-506`) не переносятся: в 1.21.1 нет ни олова, ни серебра, ни свинца. В корпусе они
  зафиксированы как пропуски с причиной (в отличие от `ResearchRecipeCoverage`, где они просто
  отсутствуют).
* **G3. Публичный API `ThaumcraftApi.getCraftingRecipes()` возвращает пустой список** — это
  расхождение зафиксировано аудитом `a9` (`docs/analysis/2026-10-21-audit-a9-core.md:88-92`) и
  относится к общему API рецептов, а не к домену этой группы; здесь не исправлялось.
* **G4. Базовая ветка `work` уже падает на 8-9 GameTest-сценариях** (культисты, угли-файрболы,
  книжная полка, резервуар эссенции, переполнение инвентаря, феттер голема, сундук-странник,
  биомы/печ, атрибуты голема) — падения флаки и не связаны с этой группой; базовая линия замерена
  отдельным прогоном (§6).

---

## 6. Проверки (реальный вывод)

### 6.1 `:common:test` — 0 падений

```
export JAVA_HOME=/home/Acak/.jdk21; export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/occrafting
./gradlew --no-daemon --console=plain :common:test
…
> Task :common:test
BUILD SUCCESSFUL in 26s
```

Итог по XML-отчётам (`common/build/test-results/test/TEST-*.xml`, 243 suite):
**tests=1683, failures=0, errors=0, skipped=0**. Из них 4 новых —
`thaumcraft.crafting.SmeltingBonusParityTest` (все зелёные).

### 6.2 `:fabric:runGameTest` — новый сценарий прошёл

```
./gradlew --no-daemon --console=plain :fabric:runGameTest
```

Прогон стартует выделенный сервер, выполняет сценарии и пишет `fabric/build/gametest-results.xml`.
В прогоне **с моими изменениями** — 311 сценариев, из них падают 8, и
`thaumcraftgametests.theoriginalsmeltingbonusesareregistered` **в списке падений отсутствует**
(сценарий выполнен и пройден).

Чтобы отделить мои изменения от базовых падений, тот же прогон сделан на чистой базе
(`git stash push -u`, `11c5f4b`): **310 сценариев, 9 падений**. Мой набор падений — **подмножество
базового** (в базе падает ещё и `fireignitesguardtargets…`), а базовый прогон падает и на сценариях,
которые в моём прогоне прошли (`golemaccessoriesinstall…`, `magicalbiomesgeneratenaturally…`).
Вывод: падения флаки и существовали до этой группы; моё изменение их не добавляет.

Итоговый статус прогона — **не принят как «зелёный»**: сборка `:fabric:runGameTest` падает с
exit code 8 из-за базовых падений, а не из-за этой группы.

---

## 7. Проверенные, но не принятые замечания

* Поле `source_order` тигельных рецептов неоднородно (индекс vs строка оригинала) — см. §3.1;
  порядок при этом точно оригинальный, поэтому дефектом не считается.
* `ContainerGhostSlots` в порту реализует quick-move явно (в оригинале — ванильное поведение);
  расхождение в сторону безопасности, не дефект.
* `ThaumcraftApi.addSmeltingBonus` хранит количество аддона, а не ноль оригинала — §D3,
  вынужденная адаптация 1.21.1, задокументирована в коде.
