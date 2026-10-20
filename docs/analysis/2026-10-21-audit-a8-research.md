# Аудит соответствия порта: исследование, сканирование и знание (TC4 4.2.3.5 → 1.21.1)

Дата: 2026-10-21. Домен: `research_scanning_and_knowledge` (57 строк worklist
`.tmp/audit/a8-research.json`). Источник истины — декомпиляция
`Thaumcraft-1.7.10-4.2.3.5.jar` (`/home/Acak/work/thaumcraft-4.2.3.5-oracle`,
декомпилятор CFR 0.152). Нативный порт — `common/src/main/java/thaumcraft/`
в ветке `audit/a8` (worktree `/home/Acak/tcw/audit-a8`, базовый коммит
`a6ec3ad`). Ревью: `docs/porting/audit-reviews-a8-research.json`.

Легаси 1.12 (`src/main/java/`) использовано **только** как справочный источник
для строк, которых нет в 1.7.10 (`legacy_only`); поведение сверялось с оракулом.

## Итог

| disposition | количество | расшифровка |
|---|---|---|
| `port` | 17 | нативный класс реализует тот же контракт (путь совпадает или отличается) |
| `replace` | 20 | оригинальный класс заменён другой конструкцией (data-корпус, набор узких классов, `CustomPacketPayload`, capability-миксин) |
| `adapt` | 16 | перенесено с вынужденным отклонением (1.21.1 API, формат данных, серверная авторитетность) |
| `exclude` | 4 | сознательно не переносится (в 1.21.1 нет аналога API/плагина) |
| **всего** | **57** | |

| native_coverage | количество |
|---|---|
| `parity_verified` | 39 |
| `partial` | 14 |
| `missing` | 4 |

| source_semantics | количество |
|---|---|
| `equivalent` | 27 |
| `adapted` | 25 |
| `not_applicable` | 5 |

`callsite_audit`: `complete` — 57 из 57.

Ключевой вывод: **корпус исследований перенесён полностью и это доказуемо.**
Оригинальный код-ориентированный `ConfigResearch` (201 регистрация
`ResearchItem`) заменён данными `data/thaumcraft/tc4/research.json`, и покрытие
проверено двумя независимыми способами (см. раздел «Полнота корпуса»).
Числа, на которые опирается игровой код — аспектная цена, сложность, клетка,
флаги, родители/скрытые родители/братья, warp, условия, страницы и их
`required_research` — совпадают с оригиналом один в один. Основные проблемы
находятся не в данных, а в **побочных эффектах**: порт тратит бумагу, которую
оригинал не тратит; сид розыгрыша скрытого исследования другой; не привязан
один из двенадцати предметных триггеров; в книге исследований потеряно шесть
страниц; пропал клиентский отклик сканирования и персональная view-модель
таумометра.

## Полнота корпуса исследований (главная проверка)

### 1. Независимый повторный разбор оракула

`scripts/porting/extract_tc4_definitions.py` — это тот же код, который создал
корпус, поэтому его хэши ничего не доказывают сами по себе. Я написал
**второй, независимый парсер** (`/tmp/opencode/a8_verify_corpus.py`, в репозиторий
не попал — это инструмент аудита) и сравнил его вывод с поставляемым
`research.json`:

| проверяемое поле | результат |
|---|---|
| число записей `new ResearchItem(...)` в оракуле vs. корпус | 201 / 201, лишних и потерянных нет |
| `category`, `column`, `row`, `complexity` | расхождений 0 |
| `aspect_cost` (полный разбор `.add(Aspect.X, n)`) | расхождений 0 |
| `flags` (`setAutoUnlock/Secondary/Concealed/Hidden/Lost/Round/Special/Stub/Virtual`) | расхождений 0 |
| `parents`, `parents_hidden`, `siblings` | расхождений 0 |
| `entity_triggers`, `aspect_triggers`, число `item_triggers` | расхождений 0 |
| страницы: количество, порядок, вид (`text`/`recipe_reference`), `recipe_key` | расхождений 0 |
| `required_research` (15 «запертых» страниц) | совпадает с 15 оригинальными `new ResearchPage("KEY","text")` |
| `warp` (20 записей `ThaumcraftApi.addWarpToResearch`) | расхождений 0 |
| `conditions` (15 записей) | совпадают с `if (Config.…)` в оракуле |

Вывод: **содержательно корпус эквивалентен оригиналу.**

### 2. Пинованные хэши экстрактора

```
python3 scripts/porting/extract_tc4_definitions.py --oracle /home/Acak/work/thaumcraft-4.2.3.5-oracle --check
{"aspects": 48, "research_count": 201,
 "research_key_category_sha256": "397d620877d429c49d4db6ae65552a681ac5b26be5d60cd1e95e5e28cae9f40e",
 "text_page_count": 294,
 "text_page_sha256": "66a10cd39a47e78472134a35d3b7b4e1b78618996ac6c87907aeb63303d4bf38",
 "mode": "check"}
```

Тот же скрипт дополнительно сверяет 1.12-корпус
(`src/main/java/thaumcraft/common/config/research/ConfigResearch*.java`) с
оригинальным по 12 полям метаданных — расхождений нет, значит и легаси, и
нативный корпус восходят к одним и тем же оригинальным определениям.

### 3. Проверка заявлений `ResearchRecipeCoverage`

`common/src/main/java/thaumcraft/common/lib/research/ResearchRecipeCoverage.java`
заявляет покрытие рецептов. Проверено по упакованному дереву
`common/src/main/resources/data/thaumcraft/recipe` (308 файлов):

* 250 оригинальных ключей рецептов отображены на существующие нативные
  `namespace:path` — **у всех 250 файл рецепта реально лежит в дереве**
  (0 «отображённых, но отсутствующих»);
* корпус ссылается на 272 оригинальных ключа рецептов;
* 6 из неразрешённых — это не рецепты вовсе, а оригинальные «wand construct»
  (`ArcTable`, `Crucible`, `InfernalFurnace`, `NodeJar`, `ResTable`,
  `Thaumonomicon`); ещё три конструкта из девяти (`AdvAlchemyFurnace`,
  `InfusionAltar`, `Thaumatorium`) имеют и обычный нативный рецепт;
* оставшиеся 16 — документированные отложения (`ResearchRecipeCoverage.buildDeferred`),
  у каждого указана причина (блок-состояние вместо отдельного предмета либо
  сторонняя руда: `PureCopper/Lead/Silver/Tin`, `TransCopper/Lead/Silver/Tin`,
  `WandCapCopper/Silver/SilverInert`, `ArcanePressurePlate`, `FluxScrubber`,
  `MirrorEssentia`, `WandPedFocus`, `NodeStabilizerAdv`).

Итого заявление класса **подтверждается**: каждый ключ рецепта, упомянутый
корпусом, либо имеет нативный рецепт, либо является оригинальным блюпринтом
wand construct, либо отложен с письменной причиной.

### 4. Нативный код honours корпус

* цена — `ResearchPurchase.validate` (`ResearchPurchase.java:79-102`) и
  `ResearchNoteGenerator.generate` (`ResearchNoteGenerator.java:45-59`);
* сложность/радиус/кольцо аспектов — `ResearchNoteGenerator.java:37-76`
  (радиус `1 + complexity`, порядок и количество фиксированных клеток — как в
  оригинальном `ResearchManager.createNote`);
* родители и скрытые родители — `ResearchDefinition.hasPrerequisites`
  (`ResearchDefinition.java:38-40`), используется везде;
* братья — `ResearchCompletion.apply` (`ResearchCompletion.java:19-23`) и
  `ResearchBrowserData.applyBookSiblingUnlocks`;
* флаги — `ResearchDefinition.isVirtual/isHidden/isLost/isSecondary/isAutoUnlock`
  и `flags().contains("Concealed"/"Special"/"Round"/"Stub")`;
* warp — `ResearchCompletion.complete` (`ResearchCompletion.java:30-36`),
  разделение `perm = warp - warp/2`, `sticky = warp/2` совпадает с оригиналом;
* условия — `ResearchConditions.evaluate` (`ResearchConditions.java:21-31`),
  все 15 оригинальных условий вычисляются по фактам, а не по legacy-Java;
* **порядок регистрации** — `ResearchRegistrationOrder` (`ResearchRegistrationOrder.java:19-45`)
  восстанавливает оригинальный порядок регистрации из provenance-поля `source.line`
  и пересобирает исходную `HashMap` по категориям, поэтому порядок розыгрыша
  скрытого исследования и подсказки совпадает с оригинальным.

### 5. Проверка выполнением

`JAVA_HOME=/home/Acak/.jdk21 ./gradlew :common:test` (Fabric/Architectury, JDK 21,
компиляция `:common:compileJava` без ошибок) с фильтрами
`thaumcraft.research.*`, `thaumcraft.scanning.*`, `thaumcraft.aspects.*`,
`thaumcraft.common.lib.research.*`, `thaumcraft.common.lib.network.*`,
`thaumcraft.api.*`:

**62 набора, 389 тестов, 0 падений, 0 пропусков** (`BUILD SUCCESSFUL`).

## Расхождения по классам (все, кроме `parity_verified`)

### API-поверхность

* `thaumcraft/api/research/ResearchCategories.java` — `adapt`/`partial`.
  Оригинал: `oracle thaumcraft/api/research/ResearchCategories.java:44-50`
  `registerCategory(key, icon, background)`; регистрация шести категорий с
  иконками — `oracle thaumcraft/common/config/ConfigResearch.java:68-73`
  (`thaumonomiconcheat.png`, `r_thaumaturgy.png`, `r_crucible.png`,
  `r_artifice.png`, `r_golemancy.png`, `r_eldritch.png`).
  Порт: `native common/src/main/java/thaumcraft/api/research/ResearchCategories.java:35`
  публикует категории корпуса с `new ResearchCategoryList(null, null)` —
  **иконки и фон категорий потеряны**. Сами текстуры в
  `assets/thaumcraft/textures/misc/` лежат, но не используются.
  Следствие: вкладки браузера — текстовые кнопки
  (`native .../GuiResearchBrowser.java:59-64`) вместо иконок оригинала
  (`oracle .../GuiResearchBrowser.java:495` `bindTexture(rcl.icon)`).
* `thaumcraft/api/research/ResearchCategoryList.java` — `adapt`/`partial`.
  Оригинал: `oracle .../ResearchCategoryList.java:21` `HashMap<String, ResearchItem>`.
  Порт: `native .../ResearchCategoryList.java:20` `LinkedHashMap` (задокументировано
  в javadoc). Наблюдаемый порядок RNG восстановлен отдельно через
  `ResearchRegistrationOrder`, но публичная карта теперь упорядочена по
  корпусу, а не по `HashMap` оригинала. Поля `icon`/`background` для корпусных
  категорий — `null`.
* `thaumcraft/api/research/ResearchItem.java` — `adapt`/`partial`.
  Оригинал: `oracle .../ResearchItem.java:90-138` (весь builder-API),
  `:159-169` (`getItemTriggers/getEntityTriggers/getAspectTriggers`),
  `:244-255` (`getResearchPrimaryTag`).
  Порт: `native .../ResearchItem.java:114-146` builder сохранён, но
  `native .../ResearchItem.java:88` создаёт элемент корпуса **без иконки**
  (`(ResourceLocation) null`), а `itemTriggers`/`aspectTriggers` остаются
  `null`: в корпусе они хранятся только как provenance-строки
  (`native .../ResearchItem.java:81-109`). В самом браузере иконка записи всё
  же рисуется — она восстанавливается из `icon_expression` через
  `ResearchIconDisplay` (`native .../ResearchIconDisplay.java:57-330`,
  белый список оригинальных предметов/блоков и их метаданных).
* `thaumcraft/api/research/ResearchPage.java` — `adapt`/`partial`.
  Оригинал: `oracle .../ResearchPage.java:26-32` и 11 значений `PageType`.
  Порт: `native .../ResearchPage.java:23-31` те же поля и те же 11 типов, но
  `native .../ResearchPage.java:130-135` `of()` отображает вид корпуса
  `legacy_expression` в `PageType.IMAGE` — восемь страниц-«массивов рецептов»
  теряют исходный тип. Сплавление (`SMELTING`) в 1.21.1 требует `Level`
  (`native .../ResearchPage.java:94-103`).

### Клиент

* `thaumcraft/client/gui/GuiResearchBrowser.java` — `adapt`/`partial`.
  Оригинал: `oracle .../GuiResearchBrowser.java:75-76` панель 256×230;
  `:387-399` четыре кадра значка (Round 54, Hidden 230/86, обычный 110/0);
  `:366` «forbidden»-метка для исследования с warp > 0; `:401` оверлей
  `Special`; `:327-348` соединительные линии между братьями; `:732-787`
  анимированные «wiggle»-линии.
  Порт: `native .../GuiResearchBrowser.java:51` панель до 510×300;
  `:96` кадр `Round ? 54 : Hidden ? 86 : purchases ? 110 : 0` — **кадр 230
  (скрытое исследование при `researchDifficulty == -1` либо скрытое вторичное
  при 0) не воспроизведён**; `:84-92` рисуются только линии по родителям;
  нет ни «forbidden»-метки, ни оверлея `Special`.
* `thaumcraft/client/gui/GuiResearchRecipe.java` — `adapt`/`partial`.
  Оригинал: `oracle .../GuiResearchRecipe.java:1-1313` книга со всеми видами
  страниц.
  Порт: `native .../GuiResearchRecipe.java:31-129` — текстовые страницы и те
  страницы рецептов, чей оригинальный ключ разрешается в нативный рецепт.
  `native .../ThaumcraftRecipeDisplay.java:363` и `:374` умеют разворачивать
  только два массива (`cruc.toArray(...)`, `raug.toArray(...)`); остальные
  шесть страниц (`BANNERS`, `JARLABEL`, `ORE`, `PRIMALARROW`, `SCEPTRE` и
  страница сплавления `CRUCIBLE`) в книге **не показываются вовсе**.
* `thaumcraft/client/gui/GuiResearchTable.java` — `adapt`/`partial`.
  Оригинал: `oracle .../GuiResearchTable.java:68` `HEX_SIZE = 9`, `:100-102`
  гейты `RESEARCHER_1/2` и `RESEARCHDUPE`, `:499-516` проверка соединений.
  Порт: `native .../GuiResearchTable.java:178` гейт `RESEARCHDUPE` воспроизведён,
  броски исследователя 0.1/0.25/0.5 и порядок финансирования
  «пул игрока → бонус стола» воспроизведены в
  `native .../ResearchNoteEditing.java:108-142`; но сервер-авторитетные
  запросы правки/объединения/копирования добавляют сессию и ревизию, которых
  в оригинале нет, а «руны»/линии листа заменены нативной отрисовкой.
* `thaumcraft/client/renderers/item/ItemThaumometerRenderer.java` —
  `exclude`/`missing`.
  Оригинал: `oracle .../ItemThaumometerRenderer.java:72-75` — `IItemRenderer`
  1.7.10 с моделью `scanner.obj`; `:98-135` — персональная view-модель от
  первого лица с рукой игрока.
  Порт: `native common/src/main/resources/assets/thaumcraft/models/item/thaumometer.json`
  — обычная модель предмета; `IItemRenderer`/`IModelCustom` в 1.21.1 удалены.
* `thaumcraft/client/renderers/item/ThaumometerPerspectiveModel.java` —
  `exclude`/`missing`. Легаси-обёртка `IBakedModel` над
  `ItemThaumometerRenderer` (`legacy .../ThaumometerPerspectiveModel.java:59-63`)
  не имеет цели в 1.21.1.
* `thaumcraft/client/integration/jei/ResearchVisibility.java` и
  `thaumcraft/client/integration/jei/ResearchVisibilityTicker.java` —
  `exclude`/`missing`. Оба класса обслуживают только JEI-плагин 1.12
  (`legacy .../ThaumcraftJeiPlugin.java`); в нативном порте нет пакета
  `client/integration` и нет зависимости от JEI/EMI. Правила видимости
  исследований при этом сохранены внутри мода
  (`ResearchBrowserData.isVisible` и серверная проверка допуска в
  `ResearchPurchase`/`ResearchNoteIssuance`).

### Состояние и менеджеры

* `thaumcraft/common/lib/research/ResearchManager.java` — `replace`/`partial`.
  Оригинал: `oracle .../ResearchManager.java:84-118` `createClue`,
  `:137-160` `findHiddenResearch`, `:120-135` `createResearchNoteForPlayer`,
  `:268-337` `createNote`/`getData`/`updateData`, `:221-266`
  `checkResearchCompletion`/`checkConnections`, `:510-528` `completeResearch`.
  Порт: те же правила живут в `ResearchClues`, `ResearchHiddenSelection`,
  `ResearchNoteIssuance`, `ResearchNoteGenerator`, `ResearchNotePuzzle`,
  `ResearchCompletion`. Отклонения — в разделе «Дефекты» (расход бумаги) и
  «Отклонения» (сид RNG). Мёртвый `findMatchingResearch`
  (`oracle .../ResearchManager.java:162-185`, ни одного вызова в оракуле) не
  переносился — сознательно.
* `thaumcraft/common/lib/research/PlayerKnowledge.java` — `adapt`/`partial`.
  Оригинал: `oracle .../PlayerKnowledge.java:14-22` девять глобальных карт по
  имени игрока; `:98` `return (short)known.getAmount(aspect)`;
  `:24-33` `wipePlayerKnowledge` не чистит `warpCount`.
  Порт: `native .../PlayerKnowledge.java:21-30` — один неизменяемый объект на
  игрока; `:66` `getAspectPoolFor` возвращает `int` (short-обрезка сохранена
  только там, где она наблюдаема — `ResearchNoteEditing.queryPool`, строка 93);
  `:116-125` `reset()` дополнительно обнуляет `warpCounter`.
* `thaumcraft/common/lib/research/ScanManager.java` — `replace`/`partial`.
  Оригинал: `oracle .../ScanManager.java:420-441` `checkAndSyncAspectKnowledge`
  (бонус +2 при открытии, мягкий потолок `sqrt`, правило 1.25×),
  `:443-464` `validScan`, `:235-275` `generateNodeAspects` (`max(4, vis/10)`),
  `:132-181`/`:76-130` числовые хэши предмета и существа.
  Порт: `native .../ScanManager.java:110-124` — те же три правила арифметики
  пула; `:42-50` — то же масштабирование аспектов узла; но числовые хэши
  заменены строковыми подписями (свежий мир 1.21.1), а объектные и сущностные
  привязки вынесены в `ObjectAspectResolver`/`EntityAspectBindings`.
* `thaumcraft/common/lib/research/ResearchNoteData.java` —
  `replace`/`parity_verified`: изменяемый NBT-DTO оригинала
  (`oracle .../ResearchNoteData.java:11-20`) стал неизменяемой записью с
  ограниченным кодеком (`native .../ResearchNoteData.java:26`,
  `:80-144`), семантика ячеек (hexq/hexr/type/aspect), цвет, флаг `complete`
  и счётчик `copies` сохранены.
* `thaumcraft/common/tiles/TileResearchTable.java` — `adapt`/`partial`.
  Оригинал: `oracle .../TileResearchTable.java:64` два слота,
  `:340-342` лимит 64, `:117` пересчёт бонуса раз в 602 тика,
  `:189-291` экология бонуса по `Material`, `:141-187` `placeAspect`,
  `:398-422` `duplicate`.
  Порт: `native .../TileResearchTable.java:68/129/114` — те же числа;
  `:254-301` — те же производители бонуса, но на явном списке блоков 1.21.1 и
  тегах вместо `Material`; правка/копирование стали серверными запросами с
  ревизией, побочный эффект «заменить стол» оригинала (`:415`) не воспроизведён.
* `thaumcraft/common/items/ItemResearchNotes.java` — `adapt`/`partial`.
  Оригинал: `oracle .../ItemResearchNotes.java:87-104` (обучение по решённой
  ноте), `:105-116` (нота 42/24 → скрытое исследование или 7-9 осколков).
  Порт: `native .../ItemResearchNotes.java:36-55` и
  `native .../UnknownKnowledge.java:19-47` — см. «Дефекты» (сид розыгрыша).
* `thaumcraft/common/items/relics/ItemThaumometer.java` — `adapt`/`partial`.
  Оригинал: `oracle .../ItemThaumometer.java:79-81` длительность 25 тиков,
  `:155-158` завершение при `count <= 5` (то есть через 20 тиков),
  `:87-140` порядок выбора цели, `:92/103/128` частицы рун, `:163` звук
  `thaumcraft:cameraticks`.
  Порт: `native .../ItemThaumometer.java:31/56` — те же числа и тот же порядок
  выбора цели, но сканирование выполняется на сервере и **клиентский отклик
  (звук и частицы) отсутствует**.
* `thaumcraft/common/items/relics/ItemThaumonomicon.java` —
  `adapt`/`parity_verified`: обе ветви книги (чит-лист и «дочерние» братья)
  повторяют оригинальские значения, включая пул аспектов 50 и деление warp;
  вынужденные изменения — отдельный предмет вместо метаданных 42 и
  отсутствие клиентского звука `thaumcraft:page` (`oracle ...:109`).
* `thaumcraft/common/lib/network/playerdata/PacketScannedToServer.java` —
  `replace`/`partial`: C2S-пакета скана больше нет, цель выбирает сервер
  (`native .../ItemThaumometer.java:56`); как следствие — потерян
  клиентский отклик сканирования.
* `thaumcraft/common/lib/network/playerdata/PacketResearchComplete.java` —
  `replace`/`parity_verified`: 1.7.10-пакет заменён на
  `ResearchNoticePayload`; клиентские эффекты оригинала
  (`oracle .../PacketResearchComplete.java:56-76`) воспроизведены один в один
  в `native .../client/lib/ResearchNotifications.java:33-46`.
* `PacketSyncResearch`, `PacketSyncScannedEntities`, `PacketSyncScannedItems`,
  `PacketSyncScannedPhenomena` — `replace`/`parity_verified`: четыре
  отдельных списка заменены одним NBT-снимком знаний
  (`native .../KnowledgeSyncPayload.java:11-31`,
  `native .../PlayerKnowledge.java:166-169`).
* `thaumcraft/common/lib/capabilities/{IPlayerKnowledge,PlayerKnowledgeCapability,PlayerKnowledgeProvider}.java`
  — `replace`: контракт ушёл в публичное API
  (`native .../api/capabilities/IPlayerKnowledge.java:14-81`,
  `NativePlayerKnowledge`), состояние — в `PlayerKnowledge`, доступ — через
  миксин `PlayerKnowledgeHolder`/`KnowledgeAccess`. Forge-API `Capability`
  в 1.21.1 отсутствует.

## Отклонения, требующие решения

1. **Расход бумаги при выдаче первичной ноты исследования.**
   Оригинал делает X: `oracle thaumcraft/common/lib/research/ResearchManager.java:126`
   только *проверяет* наличие бумаги (`InventoryUtils.isPlayerCarrying(...) >= 0`),
   и нигде её не списывает — `InventoryUtils.isPlayerCarrying`
   (`oracle thaumcraft/common/lib/utils/InventoryUtils.java:412-418`) только
   возвращает номер слота. Порт делает Y:
   `native .../ResearchNoteIssuance.java:80` `staged.get(plan.paper).shrink(1)` —
   бумага списывается за каждую выданную ноту. Комментарий в коде
   (`native .../ResearchNoteIssuance.java:79`) утверждает обратное
   («Original consumeInventoryItem also decrements paper in creative») — это
   поведение **легаси 1.12** (`legacy .../ResearchManager.java:410`
   `clearMatchingItems(Items.PAPER, -1, 1, null)`), а не оригинала 4.2.3.5.
   Влияние: каждая выданная нота стоит игроку лишнюю бумагу.
2. **Сид розыгрыша скрытого исследования.**
   Оригинал: `oracle .../ResearchManager.java:154`
   `new Random(player.worldObj.getTotalWorldTime()/10L/5L)` — абсолютное время мира.
   Порт: `native .../UnknownKnowledge.java:24` →
   `native .../ResearchHiddenSelection.java:13` `new Random(dayTime/10L/5L)`, где
   `dayTime` — `level.getDayTime()` (время суток 0..24000), а не
   `getTotalWorldTime()`. Форма формулы сохранена, источник времени — нет:
   в одинаковом состоянии мира оригинал и порт открывают **разные**
   скрытые исследования.
3. **Иконки категорий исследования потеряны** (см. `ResearchCategories`):
   оригинал рисует иконку вкладки, порт — текстовую кнопку. Текстуры в
   ассетах есть, данные — нет.
4. **Один из двенадцати предметных триггеров не привязан.**
   Оригинал: `oracle thaumcraft/common/config/ConfigResearch.java:414`
   `ROD_primal_staff ... setItemTriggers(new ItemStack(ConfigItems.itemFocusPrimal))`.
   Порт: `native .../ResearchTriggerBindings.java:11-17` связывает только
   `BONEBOW`, `JARBRAIN`, `MIRROR`, `PRIMPEARL` и четыре `TINY*`; при этом
   `thaumcraft:focus_primal` **в порту существует**
   (`native .../ThaumcraftPrimalFoci.java:18`,
   `native .../ResearchIconDisplay.java:142`). Следствие: сканирование
   первичного фокуса в порту никогда не выдаёт подсказку `ROD_primal_staff`,
   хотя в оригинале выдаёт.
   (Остальные непривязанные триггеры — `BANNERS`, `OUTERREV` — относятся к
   ещё не перенесённому контенту, это заявлено в javadoc класса.)
5. **Шесть страниц-«массивов рецептов» отсутствуют в книге исследования**
   (`BANNERS`, `JARLABEL`, `ORE`, `PRIMALARROW`, `SCEPTRE`, страница сплавления
   `CRUCIBLE`): оригинал их показывает, порт — нет.
6. **Клиентский отклик сканирования отсутствует:** оригинал играет
   `thaumcraft:cameraticks` каждые два тика и рисует частицы рун на цели
   (`oracle .../ItemThaumometer.java:92/103/128/163`); порт не посылает
   клиенту ничего.
7. **Персональная view-модель таумометра отсутствует** (в оригинале — сканер от
   первого лица с рукой игрока). В 1.21.1 нет `IItemRenderer`; таумометр
   рисуется обычной моделью.
8. **Нет интеграции с просмотрщиками рецептов (JEI/EMI)** — в оригинале 1.7.10
   её тоже не было, но в легаси 1.12 она была, и правила скрытия рецептов
   («видно только после исследования») в порту не воспроизведены ни в каком
   виде.
9. **Порядок вывода тегов цены в подсказке браузера.**
   Оригинал: `oracle thaumcraft/client/gui/GuiResearchBrowser.java:584`
   `tags.getAspectsSortedAmount()` — стабильная сортировка по убыванию,
   равные значения сохраняют порядок объявления.
   Порт: `native .../GuiResearchBrowser.java:159-162` — сортировка по
   убыванию количества, затем по имени тега. Набор и количество аспектов
   совпадают, но порядок иконок цены отличается в **115 из 201** записей
   (смещение только для равных количеств; сама покупка от порядка не зависит).
   Смежная деталь API аспектов: оригинальный
   `AspectList.getAspectsSortedAmount` (`oracle .../AspectList.java:100-123`)
   вообще не двигает аспекты с нулевым количеством, а нативный
   (`native .../AspectList.java:33-37`) сортирует все значения; на корпусе
   это не проявляется (все цены > 0), но behaviour API-метода отличается.

## Дефекты

1. **Выдача ноты исследования списывает бумагу, которую оригинал не списывает**
   (см. «Отклонения», п. 1). Воспроизведение: взять первичное исследование с
   `tags.size() > 0` (например `FOCUSFIRE`), иметь в инвентаре бумагу и
   чернила, кликнуть по нему в браузере. Порт: бумаги станет на одну меньше.
   Оригинал: количество бумаги не изменится.
   * `oracle thaumcraft/common/lib/research/ResearchManager.java:120-135`
   * `native common/src/main/java/thaumcraft/common/lib/research/ResearchNoteIssuance.java:80`
   * комментарий `native .../ResearchNoteIssuance.java:79` утверждает, что это
     поведение оригинала — это неверно, оно перенесено из легаси 1.12.
2. **Сид скрытого исследования другой, чем в оригинале** (см. «Отклонения»,
   п. 2). Воспроизведение: в одном и том же состоянии мира (абсолютное время
   мира) открыть «ноту неизвестного исследования» (метаданные 42/24 в
   оригинале, `unknown_research_note` в порту) — оригинал и порт выберут
   разные записи.
   * `oracle thaumcraft/common/lib/research/ResearchManager.java:154`
   * `native common/src/main/java/thaumcraft/common/lib/research/UnknownKnowledge.java:24`
3. **Не привязан предметный триггер `ROD_primal_staff`** (см. «Отклонения»,
   п. 4), хотя предмет `thaumcraft:focus_primal` в порту есть. Воспроизведение:
   отсканировать первичный фокус таумометром — оригинал выдаёт подсказку
   `@ROD_primal_staff`, порт не выдаёт никакой.
   * `oracle thaumcraft/common/config/ConfigResearch.java:414`
   * `native common/src/main/java/thaumcraft/common/lib/research/ResearchTriggerBindings.java:11-17`
4. **Категория исследования теряет иконку** (см. «Отклонения», п. 3):
   `ResearchCategories.loadNative` публикует категории с `null`-иконкой, хотя
   оригинал регистрирует её явно. Это не «дефект данных», а недосборка
   провенанса: `research.json` не хранит иконку категории, хотя хранит
   иконку записи (`icon_expression`).
   * `native common/src/main/java/thaumcraft/api/research/ResearchCategories.java:35`
   * `oracle thaumcraft/common/config/ConfigResearch.java:68-73`
5. **Кадр «скрытого» значка исследования в браузере выбирается неверно для
   части режимов сложности** (см. «Расхождения»): оригинал для скрытого
   исследования при `researchDifficulty == -1` (или скрытого вторичного при 0)
   рисует кадр 230, порт всегда рисует 86.
   * `oracle thaumcraft/client/gui/GuiResearchBrowser.java:389-393`
   * `native common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java:96`

## Не перенесено

* `thaumcraft/client/integration/jei/ResearchVisibility.java`,
  `thaumcraft/client/integration/jei/ResearchVisibilityTicker.java` — JEI-плагин
  1.12; в порте нет просмотрщика рецептов вообще.
* `thaumcraft/client/renderers/item/ItemThaumometerRenderer.java`,
  `thaumcraft/client/renderers/item/ThaumometerPerspectiveModel.java` —
  персональная view-модель таумометра; в 1.21.1 нет `IItemRenderer`.
* `ResearchManager.findMatchingResearch` (`oracle .../ResearchManager.java:162-185`) —
  в оригинале не имеет ни одного вызова (мёртвый код), не переносился.
* Клиентские частицы рун и звук `thaumcraft:cameraticks` при сканировании.
* Шесть страниц-«массивов рецептов» в книге исследования.
* Исполняемые объекты рецептов и предметные триггеры как данные: корпус
  хранит их только как provenance-строки (по условию извлекателя
  «expression_policy» в `research.json`).
* `thaumcraft/common/config/research/ConfigResearchEldritch.java` — 16
  оригинальных записей категории ELDRITCH. Файл не попал в мою worklist:
  маршрутизатор очереди `update_scope_ledger.queue_for` относит любое имя с
  «eldritch» к семейству `eldritch_and_outer_lands` (M12). Содержательно он
  покрыт тем же корпусом и той же проверкой экстрактора — это ошибка
  маршрутизации, а не пробел в переносе.

## Что я НЕ смог проверить

* Клиентскую картинку: сравнение 1.21.1-рендеринга с оригиналом (вкладки
  категорий, книга, всплывающее окно, таблица исследования, вид таумометра)
  сделано **по коду и по числам**, без запуска клиента и без скриншотов.
* Живой прогресс: ни один сценарий выдачи ноты, покупки исследования, скана
  и решения головоломки не проигран в игре — проверены только юнит-тесты
  (389 тестов, 0 падений) и статическое чтение.
* Поведение JEI/EMI: сравнивать нечего, интеграции нет ни в оригинале 1.7.10,
  ни в порте.
* Полный `:common:test` не запускался — только 62 набора, относящиеся к
  research/scanning/aspects/api/network (Gradle требует JDK 21; в системе по
  умолчанию Java 25, сборка падает на «Unsupported class file major
  version 69» — прогон выполнялся с `JAVA_HOME=/home/Acak/.jdk21`).
* Сетевой multiplayer: проверка «пакет → сервер → снимок знаний» сделана по
  коду; прогон с двумя клиентами не проводился.
* Ассеты: наличие текстур (`gui_research.png`, `gui_researchback.png`,
  `research_toast.png`, `r_*.png`) проверено по файлам, но их попиксельное
  сравнение с оригинальным JAR не выполнялось.

## Приложение: использованная проверка

```
# 1) корпус против оракула — второй, независимый парсер
python3 /tmp/opencode/a8_verify_corpus.py
oracle research entries parsed: 201
corpus entries: 201
warp registrations: 20
problems: 0

# 2) пинованные хэши экстрактора
python3 scripts/porting/extract_tc4_definitions.py \
    --oracle /home/Acak/work/thaumcraft-4.2.3.5-oracle --check
{"aspects": 48, "research_count": 201, "research_key_category_sha256": "397d6208…",
 "text_page_count": 294, "text_page_sha256": "66a10cd3…", "mode": "check"}

# 3) сборка и тесты research-подмножества
JAVA_HOME=/home/Acak/.jdk21 ./gradlew :common:test \
  --tests "thaumcraft.research.*" --tests "thaumcraft.scanning.*" \
  --tests "thaumcraft.aspects.*" --tests "thaumcraft.common.lib.research.*" \
  --tests "thaumcraft.common.lib.network.*" --tests "thaumcraft.api.*"
BUILD SUCCESSFUL — 62 набора, 389 тестов, 0 падений
```
