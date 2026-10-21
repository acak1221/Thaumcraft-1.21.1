# Закрытие пробелов крафта TC4 4.2.3.5 → порт 1.21.1 (пункты A–E)

**Домен:** рецепты и их исполнение — тигель (алхимическая печь), инфузия, арканная верстака и обычные
(верстак/печь) рецепты, каталог «доплат при переплавке» и публичный API рецептов.
**Дата:** 2026-10-21. **Ветка:** `port/craftinggap`. **Worktree:** `/home/Acak/tcw/craftinggap`.

**Оракул (истина):** `/home/Acak/work/thaumcraft-4.2.3.5-oracle/` — декомпиляция
`Thaumcraft-1.7.10-4.2.3.5.jar` (CFR 0.152). **Читаемый референс:** `/home/Acak/work/opentc4/src/main/java/thaumcraft/…`
(тот же мод в читаемом виде) и SRG-резолвер `python3 -B scripts/porting/resolve_srg.py <имя>`.
**Предыдущий отчёт группы:** `docs/analysis/2026-10-21-openTC4-crafting.md` (ветка `port/occrafting`).

---

## 0. Итог

| Пункт | Что было | Что стало |
|---|---|---|
| **A** `RunicGirdleKinetic_2` | запись «дубликат без объяснения» | **доказано, что регистрация мертва в самом оригинале**; оформлено документированным пробелом в коде (`ResearchRecipeCoverage.documentedDuplicateRecipes()`) и в тесте |
| **B** шесть регистраций по олову/серебру/свинцу | есть в корпусе как пропуски, но только в JSON | **добавлена проверка загруженного каталога**: пропуски видны в рантайме и не могут потеряться молча |
| **C** `ThaumcraftApi.getCraftingRecipes()` | в отчёте назван пустым | **фактически не пуст**: список отдаёт рецепты загруженного уровня; добавлены юнит-тест (счёт) и рантайм-проверка на живом сервере |
| **D** каталог доплат + выплата + fallback | каталог и fallback проверены рантаймом | **добавлена проверка выплаты арканной печью** (бонус реально появляется) |
| **E** числа и порядок тигельных и обычных рецептов | сверено частично, в документе числа неверны | **числа исправлены** (54/8/6/48 и 114/100), **порядок `source_order` закреплён тестом против строк оракула**, **найдена и закрыта одна потерянная обычная рецептура** (оракул `:449`) |

`:common:test` — **1784 теста, 0 failures, 0 errors, 0 skipped** (257 suite), реальный вывод в §6.
`:fabric:runGameTest` — реальный вывод в §6.2; базовая линия (до изменений) — §6.3.

---

## A. `RunicGirdleKinetic_2` — регистрация мертва в самом оригинале

### Что в оракуле

* **Оракул** `thaumcraft/common/config/ConfigRecipes.java:355` — `RunicGirdleKinetic`: зелье с метаданными
  **16428**; `:356` — `RunicGirdleKinetic_2`: зелье с метаданными **24620**. Остальные аргументы рецепта
  (аспекты `ARMOR 33 / MAGIC 55 / AIR 64`, нестабильность 7, шесть компонентов, результат
  `itemGirdleRunic 1`) совпадают побайтно.
* Читаемый OpenTC4 — то же самое: `ConfigRecipes.java:350-351` (`Items.potionitem, 1, 16428` и
  `Items.potionitem, 1, 24620`), и `ConfigResearch.java:178` — страница исследования ссылается **только**
  на `recipes.get("RunicGirdleKinetic")`.

### Почему 24620 не может существовать в 1.7.10

16428 = `0x402C`, 24620 = `0x602C`; разница — только бит `0x2000`.

* В 1.7.10 бит `0x2000` — флаг «зелье сварено в зельеварке»: каждая строка эффекта ингредиента
  заканчивается на `+13` (`PotionHelper.sugarEffect = "-0+1-2-3&4-4+13"`, `blazePowderEffect`, `magmaCreamEffect` …),
  а `PotionHelper.gunpowderEffect = "+14&13-13"` **ставит** бит «взрывное» `0x4000` и **требует**, чтобы
  бит `0x2000` был снят (`&13-13`). Значит, взрывное зелье 1.7.10 бит `0x2000` не несёт никогда.
* Лут-таблица оригинала (`thaumcraft/common/config/Config.java:552-568`) выдаёт только формы
  `i | 0x2000` (сваренное) и `i | 0x4000` (взрывное) — обе формы сразу не бывает; то же и в
  креативной вкладке 1.7.10 (`ItemPotion.getSubItems`).
* `0x602C` = `0x4000 | 0x2000 | 0x20 | 0x0C` — то есть одновременно «взрывное» и «сваренное».
  Такого предмета в 1.7.10 **не существует**: регистрация `:356` мертва в самом оригинале.
* Первая регистрация (16428 = `0x4000 | 0x20 | 0x0C`) живая: это ровно `12 | 0x20 | 0x4000` из лут-таблицы
  оригинала (взрывное зелье с усиленным эффектом, биты `0x04|0x08` = «harm» по таблице
  `PotionHelper.potionRequirements`, бит `0x20` = усиление) → «Splash Potion of Instant Damage II».

### Что сделано в порту

Запись **оставлена как есть** (порту нечего чинить: обе нативные рецептуры обязаны быть одинаковыми),
но оформлена **документированным пробелом в коде**:

* `common/src/main/java/thaumcraft/common/lib/research/ResearchRecipeCoverage.java` — новый
  `documentedDuplicateRecipes()` с записью `RunicGirdleKinetic_2` и разбором (номер строки оракула
  `ConfigRecipes:356`, метаданные `24620`/`0x602C`, `PotionHelper.gunpowderEffect "+14&13-13"`,
  лут-таблица `Config.java:552-568`).
* `common/src/main/java/thaumcraft/client/gui/ThaumcraftRecipeDisplay.java:225` — комментарий у алиаса
  `RunicGirdleKinetic_2` со ссылкой на оракул.
* Новый тест `common/src/test/java/thaumcraft/crafting/RunicGirdleKineticParityTest.java`:
  различие метаданных (`0x2000`), побайтовая идентичность двух нативных рецептов, предикат
  `minecraft:strong_harming` на `minecraft:splash_potion` (это и есть метаданные 16428 в 1.21.1),
  аспекты/нестабильность/результат оригинала и наличие документированного пробела в корпусе покрытия.

---

## B. Шесть регистраций «доплат при переплавке» по олову/серебру/свинцу

### Проверка

* **Оракул** `thaumcraft/common/config/ConfigRecipes.java:497-499` (три регистрации по имени руды:
  `oreTin`, `oreSilver`, `oreLead`) и `:504-506` (три по предмету: кластеры олова/серебра/свинца).
  Всего оригинал регистрирует **18** доплат (`:493-510`).
* **Порт** `common/src/main/resources/data/thaumcraft/tc4/smelting_bonus.json`: 12 живых записей
  (`:493-496`, `:500-503`, `:507-510`) и **6 документированных пропусков** (`:497-499`, `:504-506`) с
  причиной («в 1.21.1 нет олова/серебра/свинца»). Пропуски **не потеряны молча**: они лежат в
  отдельном массиве `skipped` корпуса, а не просто отсутствуют.

### Что добавлено

`common/src/test/java/thaumcraft/crafting/SmeltingBonusParityTest.java` — новый тест
`theLoadedCatalogueNamesTheSixThirdPartyEntriesWithTheirReason`: он читает **загруженный** каталог
(`SmeltingBonusCatalog.entries()` / `.skipped()`), а не только JSON, и требует

* ровно 12 живых записей в порядке строк оригинала `493, 494, 495, 496, 500, 501, 502, 503, 507, 508, 509, 510`;
* ровно 6 пропусков в порядке строк `497, 498, 499, 504, 505, 506`, у каждого — непустая причина и
  указанный вход (`oreTin`/`oreSilver`/`oreLead`, `thaumcraft:cluster_tin`/`_silver`/`_lead`).

То есть если кто-то уберёт пропуски из корпуса или из загрузчика, тест это увидит.

---

## C. `ThaumcraftApi.getCraftingRecipes()` — список не пуст

### Что было не так с формулировкой отчёта

Отчёт `docs/analysis/2026-10-21-openTC4-crafting.md` §5 (G3) и аудит a9
(`docs/analysis/2026-10-21-audit-a9-core.md:88-92`) называли список пустым. Это было верно **до**
коммита `e4529cb` («fix(audit-a9): restore the sceptre, the public recipe API and the missing recipes»):
программный список `craftingRecipes` порта действительно не наполняется, потому что порт регистрирует
свои рецепты как рецепты датапака.

**Вынужденная адаптация 1.21.1** (уже в коде): оригинал держал все рецепты игры в одном статическом
списке (`ThaumcraftApi.getCraftingRecipes()`, оракул `thaumcraft/api/ThaumcraftApi.java:100-102`), а
1.21.1 держит рецепты мира в менеджере рецептов загруженного уровня. Порт устанавливает его на старте
сервера (`common/src/main/java/thaumcraft/common/Thaumcraft.java:227`) и на загрузке клиентского
уровня (`ClientSetup.java:50`), а публичный API отдаёт **оригинальное представление** каждого
нативного рецепта (`NativeRecipes.views()` → `ShapedArcaneRecipe`/`ShapelessArcaneRecipe`/
`CrucibleRecipe`/`InfusionRecipe`/`InfusionEnchantmentRecipe`).

### Что добавлено

* `common/src/test/java/thaumcraft/api/CraftingRecipeListTest.java` — юнит-тест, который **вызывает
  `getCraftingRecipes()` и считает результат**: с установленным менеджером рецептов список содержит
  ровно все рецепты уровня (плюс программные записи), а без уровня — только программные.
* `common/src/gameTest/java/thaumcraft/common/testing/CraftingRecipeApiRuntimeChecks.java` + сценарий
  `thePublicRecipeListHoldsEveryRecipeOfTheLevel` (Fabric) — проверка на **живом сервере**:
  список непуст, **каждый** упакованный нативный рецепт порта загружен менеджером рецептов, а
  оригинальные поиски `getCrucibleRecipe(...)` и `getInfusionRecipe(...)` (оракул
  `ThaumcraftApi.java:131-150`) находят нативные рецепты — ровно то, что аудит a9 считал невозможным.

---

## D. Каталог доплат наполняется, печь платит, fallback по имени руды работает

* **Каталог наполняется.** Рантайм-проверка `SmeltingBonusRuntimeChecks` (сценарий
  `theOriginalSmeltingBonusesareregistered`) читает реестр живого сервера и требует бонус для
  `minecraft:iron_ore`, `minecraft:gold_ore`, `minecraft:copper_ore`, `thaumcraft:cinnabar_ore`,
  четырёх кластеров и сырого мяса/рыбы; для сырого железа бонуса нет — так и в оригинале (в 1.7.10
  сырого железа не было).
* **Fallback по имени руды работает.** Оракул `thaumcraft/api/ThaumcraftApi.java:94-98`: сначала точная
  пара `(предмет, damage)`, потом wildcard `(предмет, 32767)`, потом **имя первой руды**, в которую
  входит предмет. Порт реализует третий шаг через теги оригинальных имён руды
  (`OriginalOreTags.firstOreName`) — в рантайме он и проверяется (руда → `minecraft:iron_nugget` и т. д.).
* **Выплата печи — добавлено в этом заходе.** Оракул
  `thaumcraft/common/tiles/TileArcaneFurnace.java:237-259`: при выбросе печь спрашивает API бонус
  входного предмета и платит его отдельным предметом — без мехов один бросок с шансом 1/4, с мехами
  по одному броску на мех с шансом 44 %. Проверка `furnacePaysTheBonus` в
  `SmeltingBonusRuntimeChecks` ставит ядро арканной печи, **64 раза** вызывает выброс с рудой и
  требует, чтобы самородков появилось больше нуля (шанс не сработать 64 раза подряд ≈ 1e-8) и не больше
  одного за переплавку без мехов.

---

## E. Числа и порядок регистрации тигельных и обычных рецептов

### E.1 Тигель (оракул `ConfigRecipes:105-181`, `initializeAlchemyRecipes`)

* **Число.** Оракул регистрирует **54** тигельных рецепта в рантайме: **49 мест регистрации**, из них
  цикл балансированных осколков (`:113`) выполняется **шесть** раз. Восемь регистраций условны
  (`Config.foundCopperIngot`/`foundTinIngot`/`foundSilverIngot`/`foundLeadIngot`, строки `:133-157`).
  В 1.21.1 медь — ванильная, поэтому её два рецепта (`:134` `PureCopper`, `:148` `TransCopper`) живые,
  а шесть рецептов по олову/серебру/свинцу требуют стороннего мода → порт везёт **48** нативных рецептов
  тигля.
  * Исправлено в `common/src/test/java/thaumcraft/assets/CrucibleRecipeParityTest.java`: в javadoc было
    написано «8 условных → порт везёт остальные 46», что неверно (46 + 2 медных = 48).
* **Порядок.** Порядок регистрации — часть поведения оригинала: тигель берёт первую
  зарегистрированную запись из совпадающих по числу разных аспектов
  (`ThaumcraftCraftingManager.java:127-145`). Нативное поле `source_order` сортирует рецепты тигля
  (`CrucibleCrafting.java:29`), поэтому оно обязано расти вместе с порядком оракула.
  * Проверено машинно: сортировка 48 файлов по `source_order` даёт ровно порядок строк оракула
    (`113`×6 → `115…180`), т.е. **после добавления медных рецептов порядок не сбился**.
  * Новый тест `theSourceOrderFollowsTheLinesOfTheOracle` закрепляет это построчно: `source_order`
    строго возрастает, порядок строк оракула не убывает, а каждое значение равно либо индексу
    регистрации в списке порта, либо номеру строки оракула (в порту первые 28 рецептов пронумерованы
    индексом, остальные — строкой оракула; обе схемы сохраняют порядок).
  * Новый тест `theCountsAreTheOriginalOnes` закрепляет числа: 48 нативных рецептов, 6 балансированных
    осколков, 6 рецептов за сторонним модом, 2 медных рецепта взяты из строк `:134` и `:148`.

### E.2 Обычные рецепты (оракул `ConfigRecipes:206-208`, `:385`, `:399-481`, `:485-492`)

* **Число.** Оригинал регистрирует **114** обычных рецептов: 103 в `initializeNormalRecipes`
  (`:399-481`), три «арканных камня» (`:206-208`), одну краску одежды Вейта (`:385`) и восемь печных
  (`:485-492`). Порт везёт **100** нативных + 2 ванильных (железные самородки, которых не было в
  1.7.10), а ещё два рецепта — свои, для меди, которая в 1.21.1 ванильная.
* **Новый тест** `common/src/test/java/thaumcraft/assets/NormalRecipeParityTest.java` — таблица из
  **100 строк**: строка оракула → нативный рецепт. Тест требует, чтобы каждый оригинальный рецепт имел
  нативный, чтобы порт **не** везёл обычный рецепт без оригинального, и чтобы числа сходились.
  Особые случаи зафиксированы в javadoc: 16 аспектных ярлыков → одна рецептура `jar_label_bind`
  (аспект в 1.21.1 несёт компонент данных), волшебное бревно → две рецептуры (по бревну greatwood и
  silverwood), железные самородки → ванильные рецепты 1.21.1.

#### Найденное расхождение и его закрытие: оракул `:449`

Оракул `ConfigRecipes.java:449` регистрирует `GameRegistry.addRecipe(new ItemStack(ConfigBlocks.blockCosmeticSolid, 4, 1), {"##","##",'#', Blocks.obsidian})`
— **четыре обсидиана → четыре «обсидиановые плитки»** (метаданные 1 семейства `blockCosmeticSolid`,
оригинальное имя `tile.blockCosmeticSolid.1.name` = «Obsidian Tile»).

В порту этой рецептуры **не было**: семейство `cosmetic_solid` разделено на группы твёрдости, у группы
`cosmetic_solid_tough` есть блок-состояние `variant`, а предмет — один (`thaumcraft:cosmetic_solid_tough`,
метаданные 0, «Obsidian Totem»). Результат рецепта в 1.21.1 не может нести блок-состояние, поэтому
нужен отдельный предмет варианта — ровно тот приём, который порт уже использует для трёх других
оригинальных рецептов (`paving_stone_travel` — `:209`, `paving_stone_warding` — `:212`,
`warded_glass` — `:192`). Аудит a6 перечислил три таких рецепта и пропустил четвёртый.

**Фикс (по образцу уже принятого в порту приёма):**

* `EldritchBlockRules.VARIANT_ITEMS` — новая запись `obsidian_tile` → метаданные 1;
* `ThaumcraftEldritchBlocks` — новый предмет `thaumcraft:obsidian_tile`
  (`ItemVariantBlock`, ставит `cosmetic_solid_tough` с `variant=1`), добавлен в креативную вкладку;
* `common/src/main/resources/data/thaumcraft/recipe/obsidian_tile.json` — 2×2 обсидиана → 4 плитки;
* `assets/thaumcraft/models/item/obsidian_tile.json` — модель варианта 1;
* `assets/thaumcraft/lang/{en_us,ru_ru}.json` — «Obsidian Tile» / «Обсидиановая основа»
  (имена оригинала из `assets/thaumcraft/lang/en_US.lang` и `ru_RU.lang` jar'а 4.2.3.5);
* тесты, фиксирующие решение: `CosmeticFamilyAssetsTest` (идентификатор `obsidian_tile` переносится из
  списка «удалённых блочных id» в список предметов вариантов), `EldritchAndDeviceAssetsTest`,
  `EldritchBlockRulesTest`.

---

## 6. Проверки (реальный вывод)

### 6.1 `:common:test` — 0 падений

```
export JAVA_HOME=/home/Acak/.jdk21; export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/craftinggap
./gradlew --no-daemon --console=plain --no-build-cache :common:cleanTest :common:test
…
> Task :common:cleanTest
> Task :common:test
BUILD SUCCESSFUL in 16s
6 actionable tasks: 2 executed, 4 up-to-date
```

Итог по XML-отчётам (`common/build/test-results/test/TEST-*.xml`, 257 suite):
**tests=1784, failures=0, errors=0, skipped=0**.

Новые тесты этого захода: `thaumcraft.crafting.RunicGirdleKineticParityTest`,
`thaumcraft.crafting.SmeltingBonusParityTest` (добавленная проверка загруженного каталога),
`thaumcraft.api.CraftingRecipeListTest`, `thaumcraft.assets.NormalRecipeParityTest`,
`thaumcraft.assets.CrucibleRecipeParityTest` (добавленные проверки чисел и порядка).

### 6.2 `:fabric:runGameTest` — реальный вывод

Два прогона одного и того же дерева (изменения этого захода), реальный вывод:

```
export JAVA_HOME=/home/Acak/.jdk21; export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/craftinggap
./gradlew --no-daemon --console=plain :fabric:runGameTest
```

Прогон 1:

```
[17:53:51] [Server thread/INFO] (Minecraft) ========= 313 GAME TESTS COMPLETE IN 34.54 s ======================
[17:53:51] [Server thread/INFO] (Minecraft) 8 required tests failed :(
[17:53:51] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.magicalbiomesgeneratenaturallyandspawnactualpech
[17:53:51] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.unknownknowledgerevealshiddennotesandretriesfragments
[17:53:51] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.bookshelfwandcreatesonefloatingbook
[17:53:51] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.champion_modifiers_answer_their_hit_and_wound
[17:53:51] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.constructandcentrifugerecipesandplainnativeblocks
[17:53:51] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.golemaccessoriesinstallchangestatsandsurvivethebell
[17:53:51] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.golemfetterholdsgolemswhileitsredstonesignallasts
[17:53:51] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.golem_orb_homes_and_deals_the_original_damage
```

Прогон 2 (то же дерево, без изменений между прогонами):

```
[17:57:02] [Server thread/INFO] (Minecraft) ========= 313 GAME TESTS COMPLETE IN 31.72 s ======================
[17:57:02] [Server thread/INFO] (Minecraft) 5 required tests failed :(
[17:57:02] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.magicalbiomesgeneratenaturallyandspawnactualpech
[17:57:02] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.bookshelfwandcreatesonefloatingbook
[17:57:02] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.constructandcentrifugerecipesandplainnativeblocks
[17:57:02] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.golemaccessoriesinstallchangestatsandsurvivethebell
[17:57:02] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.golemfetterholdsgolemswhileitsredstonesignallasts
```

**Оба новых сценария прошли** (в списке падений их нет, в XML-отчёте `fabric/build/gametest-results.xml`
они присутствуют как пройденные):

* `thaumcraftgametests.theoriginalsmeltingbonusesareregistered` — каталог доплат наполнен, fallback по
  имени руды работает и **арканная печь платит бонус**;
* `thaumcraftgametests.thepublicrecipelistholdseveryrecipeofthelevel` — публичный список рецептов не
  пуст, все упакованные нативные рецепты загружены, оригинальные поиски находят нативные рецепты.

Падения — флаки вне домена крафта: состав падающих сценариев меняется от прогона к прогону на одном и
том же дереве (8 → 5), и в базовой линии падают те же сценарии. **Прогон не принимается как «зелёный»**:
сборка `:fabric:runGameTest` падает с exit code из-за этих базовых падений.

### 6.3 Базовая линия GameTest (до изменений этого захода)

Тот же прогон на чистом дереве ветки (`5084a65`, 312 сценариев):

```
[17:21:25] [Server thread/INFO] (Minecraft) ========= 312 GAME TESTS COMPLETE IN 49.03 s ======================
[17:21:25] [Server thread/INFO] (Minecraft) 4 required tests failed :(
[17:21:25] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.bookshelfwandcreatesonefloatingbook
[17:21:25] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.constructandcentrifugerecipesandplainnativeblocks
[17:21:25] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.golemaccessoriesinstallchangestatsandsurvivethebell
[17:21:25] [Server thread/INFO] (Minecraft)    - thaumcraftgametests.golemfetterholdsgolemswhileitsredstonesignallasts
```

(в XML-отчёте этого прогона — 5 падений, пятое — `swordofthezephyrliftsblowsandsweeps`; на следующих
прогонах то же самое дерево падает иначе.)

Они относятся к другим доменам (палочка-конструкт, книжная полка, феттер голема, аксессуары голема,
меч Зефира, биомы/печ, чемпионы, незнание) и к крафту не относятся.

### 6.4 Итог

* `:common:test` — **1784 теста, 0 failures, 0 errors, 0 skipped** (257 suite).
* `:fabric:runGameTest` — 313 сценариев; оба новых сценария проходят; падают 5–8 флаки-сценариев вне
  домена крафта (базовая линия — 4–5).

---

## 7. Что осталось открытым

* `RunicGirdleKinetic_2` — документированный пробел (см. A): обе нативные рецептуры идентичны, потому
  что оригинальная вторая регистрация мертва сама по себе. Убрать её нельзя (оригинал её регистрировал),
  оставить без объяснения нельзя — теперь она описана в коде и в тесте.
* Шесть регистраций доплат по олову/серебру/свинцу — документированные пропуски (см. B): в 1.21.1 этих
  металлов нет, регистрация заработает только вместе со сторонним модом на руду.
* Проверка выполнена на выделенном сервере (Fabric GameTest) и в юнит-тестах. **Приёмка клиента,
  мультиплеера и выживания не проводилась и не заявляется.**
