# Аудит соответствия порта TC4 4.2.3.5 → 1.21.1 — домен CORE (a9)

Дата: 2026-10-21. Ветка: `audit/a9`. Worktree: `/home/Acak/tcw/audit-a9`.

Область: **ванды, ноды и виз**, **транспорт эссенции**, **публичный API**, **рецепты и арканное
крафт**, **существа и снаряды**, **контракты аспектов и данные**.

Оракул: `/home/Acak/work/thaumcraft-4.2.3.5-oracle/`.
Нативный порт: `common/src/main/java/thaumcraft/` + `common/src/main/resources/`.
Легаси 1.12 (`src/main/java/`) использовано только как справочный источник для строк с
`path_relation = legacy_only`; истина — оракул.

Ревью: `docs/porting/audit-reviews-a9-core.json` (132 строки, все `reviewed_sha256` совпадают с
worklist; проверено против `docs/porting/scope-ledger.json` тем же валидатором, что и
`scripts/porting/audit_ledger.py --merge`).

---

## 1. Итог

### 1.1 Сводка по вердиктам (все 132 строки)

| вердикт | количество |
|---|---|
| `port` (нативный класс реализует тот же контракт) | 37 |
| `adapt` (перенесено с вынужденным отклонением) | 39 |
| `replace` (нативный код заменил оригинальный класс иной конструкцией) | 27 |
| `exclude` (сознательно не переносится) | 29 |
| `needs_review` | 0 |
| **всего** | **132** |

| покрытие нативным аналогом | количество |
|---|---|
| `parity_verified` (сверено по числам/поведению) | 75 |
| `partial` (аналог есть, ведёт себя не полностью) | 26 |
| `missing` (нативного аналога нет) | 31 |
| `not_assessed` | 0 |

| аудит вызовов | количество |
|---|---|
| `complete` | 132 |

| семантика источника | количество |
|---|---|
| `equivalent` | 49 |
| `adapted` | 51 |
| `not_applicable` | 31 |
| `different` | 1 |

### 1.2 По семействам

| семейство | строк | port | adapt | replace | exclude | parity_verified | partial | missing |
|---|---|---|---|---|---|---|---|---|
| `wands_nodes_and_vis` | 37 | 16 | 2 | 9 | 10 | 16 | 11 | 10 |
| `essentia_transport` | 27 | 3 | 4 | 10 | 10 | 14 | 1 | 12 |
| `public_api_and_compatibility` | 23 | 5 | 12 | 3 | 3 | 13 | 7 | 3 |
| `recipes_and_arcane_crafting` | 20 | 6 | 5 | 3 | 6 | 11 | 3 | 6 |
| `creatures_and_projectiles` | 16 | 1 | 13 | 2 | 0 | 15 | 1 | 0 |
| `aspect_contracts_and_data` | 9 | 6 | 3 | 0 | 0 | 6 | 3 | 0 |
| **всего** | **132** | **37** | **39** | **27** | **29** | **75** | **26** | **31** |

Из 31 `missing` 25 — это классы, которые **никогда не существовали в оригинале**
(`path_relation = legacy_only`, TC6-шимпы и 1.12-рендереры легаси-дерева); 2 — классы, которые
в оригинале были, но в порту свёрнуты (`original_only`: блочные рендереры трубы и резервуара);
4 — классы оригинала, не перенесённые вовсе (`BlockCoordinates` и три рендерера труб). См. §5.

---

## 2. Что именно не совпадает (по каждому классу с `native_coverage != parity_verified`)

Ниже — только те строки, где нативный аналог есть, но ведёт себя иначе. Классы с
`native_coverage = missing` вынесены в §5.

### 2.1 `public_api_and_compatibility`

* **`thaumcraft/api/IArchitect.java` — `partial`.**
  Оригинал: `getArchitectBlocks(ItemStack, World, int, int, int, int, EntityPlayer)` и
  `showAxis(ItemStack, World, EntityPlayer, int, EnumAxis)` (oracle `thaumcraft/api/IArchitect.java:18`, `:20`).
  Порт: `int getMaxAreaSize(ItemStack)` + `default int getAxisModes()` (native
  `common/src/main/java/thaumcraft/api/IArchitect.java:7`, `:9`). Ни `getArchitectBlocks`, ни
  `showAxis`, ни `EnumAxis` в порту нет.
  Игровое поведение перенесено (`WandArchitect.java:36` `record Area`, `:84` `planarBlocks`), но
  **опубликованные точки входа не те** — аддон, написанный под оригинальный контракт, не скомпилируется.

* **`thaumcraft/api/ThaumcraftApi.java` — `partial`.**
  1. `getCraftingRecipes()` (native `:101`) возвращает всегда пустой список: `craftingRecipes`
     (native `:71`) никогда не наполняется, нативные рецепты лежат в датапаке. Следствие:
     `getInfusionRecipe`, `getCrucibleRecipe`, `getCrucibleRecipeFromHash` для любого нативного
     рецепта возвращают `null` (в оригинале — oracle `ThaumcraftApi.java:131`, `:145`, `:153`).
  2. `getCraftingRecipeKey` возвращает `{research, 0}` (native `:194`); оригинал возвращал
     `{researchKey, pageIndex}` (oracle `:161`, `:182`).
  3. `addSmeltingBonus(String, ItemStack)` (native `:86`) регистрирует запись по имени, которую
     `getSmeltingBonus(ItemStack)` (native `:91`) никогда не прочитает: в 1.21.1 нет OreDictionary,
     а fallback оригинала (oracle `:94`) невозможен.
  4. `armorMatSpecial` и `armorMatVoidFortress` в фасаде отсутствуют: оригинал публиковал пять
     материалов (oracle `:51`-`:55`), порт — три (native `:53`-`:55`).
  5. `enchantFrugal/Potency/WandFortune/Haste/Repair` = `-1` (native `:57`-`:61`): в 1.21.1
     зачарования — датапак-записи без числовых id.

* **`thaumcraft/api/damagesource/DamageSourceThaumcraft.java` — `partial`.**
  Оригинал публиковал четыре готовых источника: `taint`, `tentacle`, `swarm`, `dissolve`
  (oracle `:19`-`:22`). Порт оставил только `TENTACLE` и `SWARM` как `ResourceKey<DamageType>`
  (native `:11`); `taint` и `dissolve` не имеют нативного аналога, а поля стали ключами, а не
  готовыми `DamageSource`.

* **`thaumcraft/api/damagesource/DamageSourceIndirectThaumcraftEntity.java` — `partial`.**
  Оригинал — класс-наследник `EntityDamageSourceIndirect` с публичным конструктором
  `(String, Entity, Entity)` (oracle `:21`) и цепочкой `setFireDamage()` / `setUnblockable()`
  (oracle `:25`, `:30`). Порт — фабрика `create(Entity, Entity, Holder<DamageType>)`
  (native `:19`): модификаторы огня/неуязвимости перенесены в свойства типа урона, вызывающий
  обязан сам выбрать тип. Исходный конструктор в порту недоступен.

* **`thaumcraft/api/items/IGoggles.java`, `IRevealer.java`, `IVisDiscountGear.java` — `partial`.**
  Это не классы оригинала, а TC6-алиасы легаси-дерева. Порт держит канонические
  `thaumcraft.api.IGoggles`, `thaumcraft.api.nodes.IRevealer`, `thaumcraft.api.IVisDiscountGear`,
  но пакет `thaumcraft.api.items` в порту отсутствует — аддон 1.12, импортирующий алиас, не
  разрешится.

### 2.2 `aspect_contracts_and_data`

* **`thaumcraft/api/aspects/Aspect.java` — `partial`.**
  Данные проверены машинно и совпадают: все 48 аспектов, теги, цвета, `blend`, `chatcolor` и пары
  компонентов идентичны оракулу (native `Aspect.java:14`-`:61` против oracle `:26`-`:73`;
  машинная сверка — 0 расхождений; корпус `data/thaumcraft/tc4/aspects.json`, 48 записей).
  Расхождения — в контракте:
  1. Конструктор порта отвергает любой составной аспект, у которого не ровно два компонента
     (native `:74`), а `isPrimal()` проверяет только `components == null` (native `:105`).
     Оригинал допускал трёхкомпонентный аспект и считал его **праймальным**
     (oracle `:145`: `getComponents() == null || getComponents().length != 2`).
  2. Удалены `setTag` (oracle `:116`), `setComponents` (oracle `:124`), `setBlend` (oracle `:140`),
     `setChatcolor` (oracle `:171`); поля — `final`. Аддон больше не может перенастроить аспект.
  3. Удалён `getLocalizedDescription()` (oracle `:108`).

* **`thaumcraft/api/aspects/AspectList.java` — `partial`.**
  1. Нет конструктора `AspectList(ItemStack)` (oracle `:25`) — аддон, собирающий список аспектов из
     стека, обязан сам звать `ThaumcraftApiHelper.getObjectAspects`.
  2. Класс больше не `implements Serializable` (oracle `:21`).
  3. `add` отбрасывает `null`-аспект (native `:42`), тогда как оригинал клал его в карту под
     `null`-ключом (oracle `:153`-`:159`, и `:191` в `readFromNBT`). Неизвестный тег аспекта при
     чтении NBT теперь молча теряется.
  4. Добавлены `reduceToPrimals(boolean)` и `getAspectsSortedByAmount()` — в оригинале нет.

* **`thaumcraft/api/aspects/AspectSourceHelper.java` — `partial`.**
  Оригинал — общий помощник: `drainEssentia(TileEntity, Aspect, ForgeDirection, int range)` ищет
  **любой** `IAspectSource` в радиусе (oracle `:21`, `:35`; `EssentiaHandler.java:39`).
  Порт принимает только `TileInfusionMatrix` (native `:20`), а `direction` и `range` игнорирует
  (native `:19`, `:25`); `findEssentia` больше не спрашивает источники, а сообщает о неоплаченном
  запросе матрицы.

### 2.3 `wands_nodes_and_vis`

* **`thaumcraft/api/nodes/INode.java` — `partial`.**
  Интерфейс перенесён член в член (native `:13`-`:28`). Но **игровой узел его не реализует**:
  `TileNode` объявляет `implements IAspectContainer` (native `TileNode.java:49`) и держит свои
  вложенные копии `NodeType`/`NodeModifier` (native `TileNode.java:50`-`:51`). Доступ к узлу — только
  через `NodeAdapter`, у которого `setNodeType`/`setNodeModifier` бросают
  `UnsupportedOperationException` (native `NodeAdapter.java:36`, `:40`).

* **`thaumcraft/api/wands/IWandable.java` — `partial`.**
  Контракт перенесён (native `:18`-`:25`), но **в порту нет ни одного реализатора и ни одного
  запроса `instanceof IWandable`**. Правая кнопка палочки в порту жёстко зашита на `TileNode` и
  `TileCrucible` (native `ItemWandCasting.java:152`-`:170`) и на статические хуки
  `BlockTube.tryWandInteraction` (native `BlockTube.java:180`). Аддон-блок, реализующий
  `IWandable`, вызова не получит.

* **`thaumcraft/api/wands/WandTriggerRegistry.java` — `partial`.**
  В 1.21.1 нет метаданных блока, поэтому триггер ключуется блоком либо точным состоянием
  (native `:30`-`:55`). Оригинальный аргумент `meta` сохранился в сигнатуре, но **игнорируется**
  (native `:37`-`:39`): триггер, зарегистрированный на одну мета, теперь срабатывает на всех
  состояниях блока.

* **`thaumcraft/common/items/wands/ItemWandCasting.java` — `partial`.**
  1. **Скептр потерял обе свои особенности.** Оригинал: ёмкость сцептра `rod.capacity * 150`
      против `* 100` у палочки (oracle `:120`) и скидка `-0.1f` на потребление (oracle `:256`).
      Порт: `getMaxVis` = `data(stack).capacity()` без множителя (native `:65`), а
      `WandVisDiscount.modifier/focusModifier` про сцептр не знают (native `:52`, `:67`).
      Маркер сцептра есть (`ThaumcraftWandEnchantments.java:55`), но его «читающая» половина
      отсутствует.
  2. Класс разбит на `WandData` / `FocusData` / `WandVisDiscount` / компонент — исходная
      публичная поверхность (`setCap`, `setRod`, `getFocus`, `addVis`, `getAllVis`,
      `getAspectsWithRoom`) в порту отсутствует, аддон переписывается.

* **`thaumcraft/common/items/wands/WandManager.java` — `partial`.**
  Оригинал — 806 строк в одном классе (oracle `WandManager.java`). Порт разделил его на
  `WandManager` (85 строк), `WandVisDiscount`, `WandArchitect`, `FocusSelectionSources`,
  `FocusData`, `FocusUpgradeData`, `WandMaterials`. Арифметика сохранена (скидка `-0.1` у
  скупого фокуса, `max(cap - discount/100, 0.1)` — native `WandVisDiscount.java:52`, `:67`), но
  **статические точки входа оригинала исчезли** — аддон, вызывавший, например,
  `WandManager.toggleMisc`, должен быть переписан.

* **`thaumcraft/common/tiles/TileNode.java` — `partial`.**
  Числа совпадают: интервал регенерации 600 / 400 (BRIGHT) / 900 (PALE) / 0 (FADING) и множитель
  `×2` при блокировке 1 (native `:555`), интервал разряда 2/1/3, бросок
  `1 + (base / (shiny ? 1.5 : 1.0))`, подрезка каждые 1200 тиков, броски стабильности
  `nextInt(10000/lock) == 42` и `nextInt(12500/lock) == 69` (native `AspectOrbRules.java:10`,
  `TileNode.java:618`), откачка палочкой 1 единица + по одному за `NODETAPPER1/2` каждый пятый тик
  (native `:309`, `:325`). Расхождения:
  1. **Уровня блокировки 2 нет** (продвинутый стабилизатор узла не перенесён) — значит, множитель
     регенерации `×20` оригинала (oracle `:628`) не имеет аналога.
  2. Узел не реализует `thaumcraft.api.nodes.INode` и держит свои вложенные енамы.
  3. `handleDarkNode` и `handlePureNode` оригинала (oracle `:768`, `:750`) — биомные эффекты тёмного
     и чистого узла — в порту отсутствуют; тайтовый перенос уведомлён через `NodeEcology`.

* **`thaumcraft/api/aura/AuraHelper.java` — `partial`.** Не класс оригинала (TC6-хелпер). Порт
  проецирует его на узел (`AuraHelper.java:29`), но почанковой ауры и флюкса TC4 не имеет вовсе.

* **`thaumcraft/client/lib/REHWandHandler.java` — `partial`.** Оригинал держал список блоков
  архитектора на клиенте (oracle `:79`, `:311`, `:319`); порт перенёс это на сервер
  (`WandArchitect.java:36`, `:84`), а `REHWandHandler` заменён на `GuiFocusSelect`
  (native `GuiFocusSelect.java:30`).

* **`thaumcraft/client/fx/beams/FXBeamWand.java` — `partial`.** Один класс эффекта луча стал
  системой частиц по фокусам (`FocusFireballParticle`, `ShockSparkParticle`,
  `FrostSparkleParticle`); точного соответствия исходной отрисовки из исходников не выводится.

* **`thaumcraft/client/renderers/item/ItemWandRenderer.java` — `partial`.** Класса с таким именем в
  порту нет: есть `PrimalWandFocusRenderer` и каталог `WandModels`. Общая отрисовка
  rod/cap/focus-комбинаций в исходниках порта не выводится.

* **`thaumcraft/client/renderers/tile/ItemNodeRenderer.java` — `partial`.** Оригинал рисовал узел как
  выпадающий предмет (oracle, 172 строки); в порту узел — вечный блок без формы предмета, поэтому
  рисователя нет.

### 2.4 `essentia_transport`

* **`thaumcraft/common/blocks/BlockTube.java` — `partial`.**
  Оригинал: один блок с метаданными 0..7 (`createTileEntity`, oracle `:251`), включая
  центрифугу и кристаллизатор, и **собственный выход компаратора** для буфера (oracle `:287`).
  Порт: один блок на пять «видов труб» (`BlockTube.Kind`, native `:46`), центрифуга и
  кристаллизатор стали отдельными блоками, а компаратор трубы всегда 0
  (`getAnalogOutputSignal` → `0`, native `:84`); у буфера своё правило.

* **Транспорт в целом — числа сходятся.** Проверено построчно:
  труба-«однонаправленная» (оба направленных правила — native `TileTube.java:184`, `:226`),
  труба-«ограничитель» (`suck/2` — native `:196`), клапан (блокировка ствола и
  `allowFlow`-гейт — native `:240`, `:252`), буфер (ёмкость 8 **суммарно**, лестница удушья
  0/1/2 → `bellows*32`/1/0, случайный **тип** хранимых аспектов, арбитраж при изъятии против
  более сильного соседа, забор раз в 5 тиков — native `TileTubeBuffer.java:33`, `:91`, `:109`,
  `:171`), резервуар (256 **суммарно**, всасывание 24, тяга раз в 5 тиков, ответ про тип только на
  «безсторонний» запрос, компаратор `floor(units/256*14) + (units>0?1:0)`, разбрызгивание
  `units/16` блоков флюкса, 50 попыток в кубе 9, уровень флюкса 8 — native `ReservoirRules.java:9`,
  `:11`, `:23`, `:29`, `:43`, `:50`).

### 2.5 `recipes_and_arcane_crafting`

* **`thaumcraft/common/config/ConfigRecipes.java` — `partial`.** Разбор ниже (§3.1).
* **`thaumcraft/common/container/SlotCraftingArcaneWorkbench.java` — `partial`.** Оригинал уменьшал
  каждую ячейку матрицы **дважды** (сначала по типу предмета, затем по ячейкам — oracle `:48`-`:64`).
  Порт уменьшает один раз (`native `:66`-`:75`), потому что реминанды 1.21.1 уже моделируют
  расход. Отклонение задокументировано в исходнике (`native `:46`-`:49`).
* **`thaumcraft/common/lib/crafting/ThaumcraftCraftingManager.java` — `partial`.** Оригинал — 810
  строк в одном классе; порт разбил на `ArcaneCrafting`, `CrucibleCrafting`,
  `InfusionStackRecipe`, `ArcaneVis`. Поведение совпадает (рабочая станция платит ровно аспекты
  найденного рецепта — native `SlotCraftingArcaneWorkbench.java:59`-`:62`), но статические точки
  входа оригинала (`findMatchingArcaneRecipeAspects`) отсутствуют.

### 2.6 `creatures_and_projectiles`

* **`thaumcraft/common/entities/InventoryMob.java` — `partial`.** Поведение перенесено, но
  оригинальный публичный базовый класс с защищённым инвентарём и правилами дропа заменён нативной
  иерархией (`InventoryMob` / `InventoryPech`); аддон, наследовавший оригинал, переписывается.
  Остальные 15 классов семейства (`EntityAspectOrb`, `EntityFollowingItem`,
  `EntityPermanentItem`, `EntitySpecialItem`, восемь снарядов, три модели) — `parity_verified`:
  числа оригинала сохранены (например, орб: 150 тиков жизни, 5 HP, гравитация 0.03, радиус
  притяжения 8, трение 0.98/0.588 — oracle `EntityAspectOrb.java:45`, `:47`, `:115`; native
  `AspectOrbRules.java:10`, `EntityAspectOrb.java:81`, `:90`, `:107`).

---

## 3. Отклонения, требующие решения

### 3.1 Покрытие рецептов: цифры в документации устарели, и 22 рецепта оригинала не перенесены

**Проверка заявления.** Заявлено: «247 оригинальных ключей закрыты, 19 отложены»
(`docs/MIGRATION_STATUS.md:30`, `docs/HANDOFF.md:30`).

Что посчитал я по оракулу:

| величина | значение |
|---|---|
| `ConfigResearch.recipes.put(...)` в оракуле | 279 сайтов, 303 уникальных ключа после раскрытия циклов (`BalancedShard_0..5`, `Banner_0..15`, `PrimalArrow_0..5`) |
| из них условных (чужая руда / `Config.wardedStone` / `Config.allowMirrors`) | 16 |
| ключей, на которые ссылается книга исследований (oracle `ConfigResearch.java`, `recipes.get(...)`) | **272** (272 уникальных, 273 обращения) |
| нативный корпус `data/thaumcraft/tc4/research.json` | **272** ключа — **в точности те же**, ни одного лишнего и ни одного пропущенного |

Что делает нативный `ResearchRecipeCoverage` (фактически, а не по документации):

| класс | количество |
|---|---|
| `NATIVE_RECIPE_IDS` (оригинальный ключ → id нативного рецепта) | **250** |
| `CONSTRUCT_BLUEPRINTS` (девять «построек палочкой» оригинала) | **9** (из них 3 уже входят в 250) |
| `DEFERRED` (отложено с причиной) | **16** |
| объединение | **272** = все ключи, на которые ссылается книга исследований |

Я дополнительно проверил, что **каждый** из 250 сопоставленных ключей действительно имеет
нативный файл рецепта (`data/thaumcraft/recipe/<path>.json`): 250 из 250 найдено, 0 отсутствующих.
Все 16 отложенных ключей имеют записанную причину (10 — чужая руда: медь/олово/серебро/свинец;
3 — результат оригинала был отдельным предметом, а в 1.21.1 стал состоянием блока, и рецепт
1.21.1 не может нести состояние; 3 — `NodeStabilizerAdv` и два педсталя).

**Вывод 1 (дефект документации).** Числа «247 / 19» в `docs/MIGRATION_STATUS.md` и
`docs/HANDOFF.md` **устарели**: код сейчас закрывает 250 и откладывает 16. Само покрытие при этом
полное (272/272 ключа книги исследований классифицированы), но заявленная в документации цифра
не соответствует коду.

**Вывод 2 (не перенесено).** В оригинале есть ещё 31 рецепт, на которые книга исследований **не**
ссылается, и 22 из них не имеют нативного аналога:

* 16 арканных рецептов `Banner_<colour>` (oracle `ConfigRecipes.java:184`-`:189`) — в нативном дереве
  рецептов **нет ни одного рецепта, производящего баннер**;
* 6 арканных рецептов `PrimalArrow_<aspect>` (oracle `ConfigRecipes.java:218`-`:220`) — в нативном
  дереве **нет ни одного рецепта, производящего первичную стрелу**, хотя сам предмет
  `primal_arrow_air`…`primal_arrow_entropy` в порту зарегистрирован
  (`ThaumcraftLegacyItems.java:156`).

Оставшиеся 9 покрыты: `BalancedShard_0..5` → `crucible/balanced_shard_*.json`,
`Clusters`/`Clusters6` → `clusters/*.json`, `RunicGirdleKinetic_2` →
`infusion/runic_girdle_kinetic_2.json`.

**Вывод 3 (почему это не видно).** `ResearchRecipeCoverage` считает только ключи, на которые
ссылается книга исследований, поэтому 22 «вне книги» рецепта не попадают ни в один отчёт.

### 3.2 Сцептр потерял ёмкость и скидку (виз)

Оригинал: `getMaxVis` = `rod.getCapacity() * (isSceptre ? 150 : 100)` (oracle
`ItemWandCasting.java:120`) и `consumptionModifier -= 0.1f` для сцептра (oracle `:256`).
Порт: `getMaxVis` = `data(stack).capacity()` (native `ItemWandCasting.java:65`), скидки нет
(`WandVisDiscount.java:52`, `:67`). Маркер сцептра зарегистрирован
(`ThaumcraftWandEnchantments.java:44`) и пишется при крафте (`ArcaneSceptreRecipe.java:110`),
но ни ёмкость, ни скидка по нему не применяются. Для деревянной палочки это 25 → 37.5 виза и
−10 % потребления.

### 3.3 Контракт `IWandable` не имеет диспетчера

Интерфейс объявлен (`thaumcraft/api/wands/IWandable.java:18`-`:25`), но в порту **нет ни одного
реализатора и ни одного запроса `instanceof IWandable`**. Правая кнопка палочки обрабатывается
жёстко: `ItemWandCasting.java:152`-`:170` (узел и тигель) плюс статические хуки вида
`BlockTube.tryWandInteraction` (native `BlockTube.java:180`). Опубликованная в оригинале точка
расширения для аддон-блоков не работает.

### 3.4 `thaumcraft.api.BlockCoordinates` отсутствует, а `IArchitect` — другой контракт

Оригинальный `BlockCoordinates` (oracle `api/BlockCoordinates.java:13`) использовался только
архитектором: `IArchitect.java:18`, `REHWandHandler.java:79/311/319`,
`ItemFocusWarding.java:196`, `ItemElementalShovel.java:228`. В порту архитектор работает на
`BlockPos` и `WandArchitect.Area` (`WandArchitect.java:36`, `:84`), поэтому класс не перенесён, а
`IArchitect` (`native `:7`, `:9`) публикует **другой** контракт, чем оригинал.

### 3.5 `ThaumcraftApi.getCraftingRecipes()` всегда пуст

`craftingRecipes` (native `ThaumcraftApi.java:71`) никогда не наполняется, потому что нативные
рецепты — датапак. Публичные методы `getInfusionRecipe`, `getCrucibleRecipe`,
`getCrucibleRecipeFromHash`, `getCraftingRecipeKey` для нативных рецептов бесполезны. Это тихая
регрессия контракта: у аддона, который ищет рецепт по результату, ответ всегда `null`.

### 3.6 Резервуар не показывает накопленную эссенцию

Оригинал рисовал жидкость в резервуаре (`TileEssentiaReservoirRenderer.java:46`-`:57`) и тело
резервуара (`BlockEssentiaReservoirRenderer.java:39`). В порту резервуар — обычный блок со
статической моделью (`assets/thaumcraft/models/block/essentia_reservoir.json`), а
`ThaumcraftTubeClient.java:28`-`:31` регистрирует блочные рендереры только для трубы, буфера,
центрифуги и кристаллизатора. Тайл хранит цвет и `displayAspect`, но **рисовать их некому**.

### 3.7 Продвинутый стабилизатор узла отсутствует — теряется множитель регенерации `×20`

Оригинал: `getLock()` может вернуть 2, и тогда `regeneration *= 20` (oracle `TileNode.java:628`).
Порт: блокировка только 0 или 1 (`TileNode.java:503`), `regenerationInterval()` знает только `×2`
(native `:555`). Это согласуется с `ResearchRecipeCoverage`, где `NodeStabilizerAdv` помечен как
отложенный.

### 3.8 Аспект больше нельзя перенастроить

`setTag`, `setComponents`, `setBlend`, `setChatcolor` удалены, поля `final`
(oracle `Aspect.java:116`, `:124`, `:140`, `:172` против native `Aspect.java:63`-`:68`).
Дополнительно трёхкомпонентный аспект, который оригинал считал праймальным, теперь
не регистрируется вовсе (native `:74`).

---

## 4. Дефекты (воспроизводимые)

1. **Сцептр не даёт ни ёмкости, ни скидки.**
   *Воспроизведение:* скрафтить сцептр (рецепт `thaumcraft:arcane/wand_sceptre`), посмотреть
   подсказку ёмкости и сравнить с той же палочкой без маркера.
   *Ожидание (оригинал):* ёмкость в 1.5 раза больше, потребление на 10 % ниже.
   *Факт (порт):* ёмкость и потребление идентичны обычной палочке.
   *Ссылки:* oracle `ItemWandCasting.java:120`, `:256`; native
   `ItemWandCasting.java:65`, `WandVisDiscount.java:52`, `:67`.

2. **`ThaumcraftApi.getCraftingRecipes()` пуст, и все «поиск рецепта по результату» не работают.**
   *Воспроизведение:* `ThaumcraftApi.getInfusionRecipe(new ItemStack(ThaumcraftItems.X))` →
   `null` для любого X, хотя рецепт существует в датапаке.
   *Ожидание (оригинал):* возвращается зарегистрированный `InfusionRecipe` (oracle `:131`).
   *Факт (порт):* `craftingRecipes` пуст (native `:71`, `:101`).

3. **Ни один рецепт не производит первичную стрелу.**
   *Воспроизведение:* открыть реестр рецептов — ни один `data/thaumcraft/recipe/*.json` не
   содержит `primal_arrow`; предмет `primal_arrow_air` существует, но его нельзя получить крафтом.
   *Ожидание (оригинал):* шесть арканных рецептов `PrimalArrow_0..5` (oracle
   `ConfigRecipes.java:218`-`:220`).
   *Факт (порт):* рецептов нет.

4. **Ни один рецепт не производит баннер.**
   *Воспроизведение:* в дереве рецептов нет `banner`; блок баннера есть
   (`ThaumcraftOuterLands.java:247`), но не получается крафтом.
   *Ожидание (оригинал):* шестнадцать арканных рецептов `Banner_0..15` (oracle
   `ConfigRecipes.java:184`-`:189`).

5. **Нет `taint` и `dissolve` источников урона.**
   *Воспроизведение:* `DamageSourceThaumcraft` не имеет полей `taint`/`dissolve`; только
   `TENTACLE` и `SWARM` (native `:11`).
   *Ожидание (оригинал):* четыре публичных статических источника (oracle `:19`-`:22`).

6. **`AspectSourceHelper` не работает ни с одним источником, кроме матрицы инфузии.**
   *Воспроизведение:* `AspectSourceHelper.drainEssentia(tile, aspect, direction, range)` с любым
   `TileEssentiaReservoir` или зеркалом возвращает `false` (native `:20`).
   *Ожидание (оригинал):* ищет любой `IAspectSource` в радиусе (oracle `:21`).

7. **Резервуар не рисует накопленную эссенцию.**
   *Воспроизведение:* залить резервуар и посмотреть на него — жидкости нет, только статическая
   модель блока.
   *Ожидание (оригинал):* цветная жидкость по объёму (`TileEssentiaReservoirRenderer.java:57`).

8. **Документация о покрытии рецептов содержит неверные числа.**
   *Воспроизведение:* `docs/MIGRATION_STATUS.md:30` и `docs/HANDOFF.md:30` утверждают
   «247 закрыто, 19 отложено»; фактически в коде 250 и 16.

---

## 5. Не перенесено

### 5.1 Строки с `native_coverage = missing` (9 из 132; полный список — 31, из них 25 — легаси-шимпы, см. §5.3)

| класс оригинала | причина | что вместо |
|---|---|---|
| `thaumcraft/api/BlockCoordinates.java` | контракт архитектора переписан на `BlockPos` + `Area` | `WandArchitect.Area` (native `WandArchitect.java:28`) |
| `thaumcraft/api/items/IGoggles.java` | не класс оригинала; TC6-алиас легаси-дерева | `thaumcraft.api.IGoggles` |
| `thaumcraft/api/items/IRevealer.java` | не класс оригинала; TC6-алиас | `thaumcraft.api.nodes.IRevealer` |
| `thaumcraft/api/items/IVisDiscountGear.java` | не класс оригинала; TC6-алиас | `thaumcraft.api.IVisDiscountGear` |
| `thaumcraft/api/internal/CommonInternals.java` | не класс оригинала; TC6-поверхность | `thaumcraft.api.internal.ThaumcraftInternalMethods` |
| `thaumcraft/common/compat/ThaumcraftSixCompatibility.java` | не класс оригинала; бутстрап TC6-шимпов легаси-дерева | отдельные классы-алиасы присутствуют, инициализатор — нет |
| `thaumcraft/common/world/aura/AuraChunk.java` | не класс оригинала; почанковая аура TC4 отсутствует как концепт | хранение в `TileNode` |
| `thaumcraft/common/world/aura/AuraHandler.java` | не класс оригинала; почанковая аура TC4 отсутствует как концепт | `thaumcraft.api.aura.AuraHelper` |
| `thaumcraft/client/renderers/tile/TileEssentiaReservoirRenderer.java` | 1.21.1: у резервуара нет блочного рендерера | статическая модель блока (см. дефект 7) |

### 5.2 Классы оригинала, свёрнутые в другую конструкцию (7)

Из них четыре (`TileTubeValveRenderer`, `TileTubeOnewayRenderer`, `BlockTubeRenderer`,
`BlockEssentiaReservoirRenderer`) числятся в §5.1 как `missing`: класса с таким именем в порту нет.

| класс оригинала | куда свёрнут |
|---|---|
| `common/tiles/TileTubeValve.java` | `BlockTube.Kind.VALVE` + `TileTube` (native `TileTube.java:240`, `:252`) |
| `common/tiles/TileTubeOneway.java` | `BlockTube.Kind.ONEWAY` (native `:184`, `:226`) |
| `common/tiles/TileTubeRestrict.java` | `BlockTube.Kind.RESTRICT` (native `:196`) |
| `client/renderers/tile/TileTubeValveRenderer.java` | один `TileTubeRenderer` на все виды труб |
| `client/renderers/tile/TileTubeOnewayRenderer.java` | один `TileTubeRenderer` на все виды труб |
| `client/renderers/block/BlockTubeRenderer.java` | блочный рендерер трубы (native `BlockTube.java:70` `ENTITYBLOCK_ANIMATED`) |
| `client/renderers/block/BlockEssentiaReservoirRenderer.java` | статическая модель резервуара |

### 5.3 Классы, отсутствующие только в легаси-дереве (не классов оригинала)

25 строк `legacy_only` из 31 `missing` — это 1.12-шимпы и 1.12-рендереры
(пакеты `thaumcraft.api.items`, `thaumcraft.common.config.recipes`, `thaumcraft.client.integration.jei`,
`thaumcraft.client.renderers.item.Wand*`, `thaumcraft.client.renderers.tile.TileTube*Renderer`
кроме нативных, `thaumcraft.common.blocks.ItemBlocks.*`, `thaumcraft.common.world.aura.*`).
Их отсутствие — сознательное решение, а не потеря: оригинал TC4 таких классов не содержит.
Исключение — `thaumcraft/api/aspects/AspectEventProxy`, `AspectHelper`, `AspectRegistryEvent`,
`thaumcraft/api/aura/AuraHelper`, `thaumcraft/api/blocks/BlocksTC`, `thaumcraft/api/items/ItemsTC`,
`thaumcraft/api/ThaumcraftMaterials`, `thaumcraft/common/container/ArcaneWorkbenchRecipeResolver`,
`thaumcraft/client/renderers/entity/RenderWandEntityItem`, `thaumcraft/client/renderers/tile/TileTubeRenderer`
— эти **перенесены**.

---

## 6. Что я НЕ смог проверить

1. **Клиентскую визуальную эквивалентность.** Ни один рендерер не запускался; все выводы о
   визуальных расхождениях (резерввуар без жидкости, отсутствие `ItemWandRenderer`,
   `FXBeamWand` → частицы) сделаны по исходникам и по регистрациям, а не по кадрам игры.
2. **Поведение в живом мире.** Транспорт эссенции, зарядка узла и крафт проверены чтением кода и
   сверкой чисел, а не прогоном: серверный/клиентский прогон в рамках этого аудита не выполнялся
   (по границам задачи).
3. **Полнота графа вызовов по всему порту.** `callsite_audit = complete` означает, что я проверил
   вызовы каждого класса **внутри своей доменной зоны** (API, аспекты, ванды/ноды/виз, транспорт
   эссенции, рецепты, существа/снаряды) и его ближайших потребителей; классы эльдрита, тайнта,
   устройств и исследований лежат в доменах других аудитов.
4. **Рецепты, зависящие от модов.** Оригинальные 16 условных рецептов (медь/олово/серебро/свинец,
   `wardedStone`, `allowMirrors`) я учёл как отложенные, но не проверял поведение при установке
   соответствующих модов — в 1.21.1 соответствующих предметов в порту нет.
5. **Загрузка/сохранение миров.** Форматы NBT оригинала (1.7.10) и порта (1.21.1) несопоставимы;
   перенос старых миров не авторизован, и совместимость не проверялась. Проверялась только
   согласованность «пишется — читается» внутри порта.
6. **Пакеты сетевой синхронизации.** 1.21.1 требует явной синхронизации произвольных полей;
   я сверил только те места, где речь шла о числах поведения (например, порог `rechargeWait`,
   `count`), но не проверял полноту синхронизации всех полей сущностей.
7. **Точность орбитального/стабилизаторного баланса.** Множители регенерации и периоды
   подтверждены по коду; фактический «темп» в секундах в игре не измерялся.

---

## 7. Сводка ключевых проверенных чисел (для быстрой сверки)

| величина | оригинал | порт | строки |
|---|---|---|---|
| количество аспектов | 48 | 48 | oracle `Aspect.java:26`-`:73` / native `Aspect.java:14`-`:61` |
| ёмкость палочки: дерево/древо великое/серебряное | 25/50/100 | 25/50/100 | oracle `ConfigItems.java:306`-`:314` / native `WandMaterials.java:30`-`:32` |
| ёмкость посоха: великое/стихийные/серебряное/первичный | 125/175/250/250 | 125/175/250/250 | oracle `ConfigItems.java:316`-`:324` / native `WandMaterials.java:40`-`:48` |
| модификатор колпачка: железо/золото/таумий/пустота | 1.1/1.0/0.9/0.8 | 1.1/1.0/0.9/0.8 | oracle `ConfigItems.java:302`-`:305` / native `WandMaterials.java:49`-`:53` |
| регенерация узла | 600 / 400 / 900 / 0 | 600 / 400 / 900 / 0 | oracle `TileNode.java:293`-`:307` / native `TileNode.java:555` |
| множитель регенерации при блокировке | ×2 (lock 1), ×20 (lock 2) | ×2 (lock 1), lock 2 отсутствует | oracle `TileNode.java:624`-`:629` / native `TileNode.java:562` |
| интервал разряда узла | 2 / 1 / 3 | 2 / 1 / 3 | oracle `TileNode.java:558` / native `TileNode.java:519` |
| ёмкость буфера (суммарно) | 8 | 8 | oracle `TileTubeBuffer.java:44` / native `TileTubeBuffer.java:33` |
| всасывание резервуара / ёмкость | 24 / 256 | 24 / 256 | oracle `TileEssentiaReservoir.java:164`, `:35` / native `ReservoirRules.java:9` |
| компаратор резервуара | `floor(u/256*14) + (u>0?1:0)` | то же | oracle `BlockEssentiaReservoir.java:99` / native `ReservoirRules.java:23` |
| разбрызгивание резервуара | `u/16` блоков, 50 попыток, куб 9, уровень 8 | то же | oracle `BlockEssentiaReservoir.java:107`-`:121` / native `ReservoirRules.java:11`, `:29` |
| откачка узла палочкой | 1 + `NODETAPPER1` + `NODETAPPER2`, каждый 5-й тик | то же | oracle `TileNode.java:398`-`:427` / native `TileNode.java:309`, `:325` |
| орб: жизнь / HP / гравитация / радиус | 150 / 5 / 0.03 / 8 | 150 / 5 / 0.03 / 8 | oracle `EntityAspectOrb.java:45`, `:47`, `:115` / native `AspectOrbRules.java:10` |
| ключи рецептов книги исследований | 272 | 272 (те же самые) | oracle `ConfigResearch.java` / native `data/thaumcraft/tc4/research.json` |
