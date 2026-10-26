# 26.10.2026 — Аспекты аддонов: сквозная проверка и исправления

## Задача

Аддоны TC4 (Forbidden Magic, Magic Bees «tempus», Gadomancy, Thaumic Horizons, Avaritia «terminus»…)
добавляли аспект одним конструктором и регистрировали теги объектов:

```java
public static final Aspect TEMPUS = new Aspect("tempus", 0xB68CFF, new Aspect[]{Aspect.VOID, Aspect.ORDER},
        ResourceLocation.fromNamespaceAndPath("myaddon", "textures/aspects/tempus.png"), 1);
ThaumcraftApi.registerObjectTag(new ItemStack(Items.CLOCK), new AspectList().add(TEMPUS, 4));
```

Нужно, чтобы такой аспект работал везде, где работали 48 исходных, и там, где работал в оригинале.
Регистрация может произойти в любой момент инициализации модов на любом загрузчике (порядок
инициализаторов Fabric относительно Thaumcraft не гарантирован; NeoForge конструирует моды параллельно).

Ссылки на оригинал ниже — `javap -c -p -l` по `Thaumcraft-1.7.10-4.2.3.5.jar` (распаковка вне
репозитория, `/tmp/aspectagent`); номера строк — из `LineNumberTable`, то есть строки исходника
оригинала, а не строки приватного декомпилята (поэтому могут отличаться от «oracle …:N» в старых
комментариях порта).

## Что делает оригинал

- `Aspect`: реестр `public static LinkedHashMap<String, Aspect> aspects`; конструктор
  `(tag, color, components, image, blend)` бросает `IllegalArgumentException("<tag> already registered!")`
  при повторе; формы `(tag, color, components)`, `(tag, color, components, blend)` выводят текстуру
  `thaumcraft:textures/aspects/<tag.toLowerCase()>.png`; `(tag, color, chatcolor, blend)` — примал.
  `getName()` = `WordUtils.capitalizeFully(tag)`, перевода нет; `getLocalizedDescription()` =
  перевод `tc.aspect.<tag>`; `getPrimalAspects()/getCompoundAspects()` обходят живой реестр.
- Ключи языка: `tc.aspect.<tag>` (смысл аспекта), `tc.aspect.help.<tag>` (ScanManager, сообщение
  о недостающем родителе «To understand this you need to study %s.»).
- Кто обходит `Aspect.aspects` в оригинале (полный список по javap): `ThaumcraftApiHelper`,
  `GuiResearchTable`, `Config` (`initMisc`, порядок для труб), `ConfigAspects` (висп-варианты),
  `ConfigRecipes` (рецепты ярлыков), `ConfigResearch` (страница JARLABEL), `ContainerResearchTable`,
  `ItemCrystalEssence/ItemManaBean/ItemWispEssence` (`displayAspects`), `ItemEssence` (вкладка),
  `ItemThaumonomicon` (шпаргалка), `CommandThaumcraft`, `ResearchManager` (`getCombinationResult`,
  `loadPlayerData`), `ScanManager.generateEntityAspects` (игроки), `ThaumcraftWorldGenerator`,
  `TileResearchTable`. `getPrimalAspects()` дополнительно: `VisNetHandler`, `GuiArcaneWorkbench`,
  `EntityPech`, `EntityWisp`, `ItemEldritchObject`, `ItemResource`, `ItemAmuletVis`,
  `ItemBaubleBlanks`, `ItemWandCasting`, `WandRodPrimalOnUpdate`, `WarpEvents`, `ArcaneWandRecipe`,
  `ArcaneSceptreRecipe`, `PacketFXVisDrain`, `TileManaPod`.
- Когда: `Thaumcraft.preInit` → `ConfigItems.init` (классы предметов, их `displayAspects` в
  `<clinit>`: `ItemCrystalEssence:72`, `ItemManaBean:97`, `ItemWispEssence:68`); `postInit` →
  `ConfigRecipes.init`, `ConfigAspects.init`, `ConfigResearch.init`, `Config.initMisc`, то есть уже
  после preInit/init аддонов. Поэтому в оригинале висп-варианты, рецепты ярлыков и порядок труб
  включали аспекты аддонов, а `displayAspects` — на практике только 48.

## Аудит потребителей

| Потребитель (порт) | До изменения | Оригинал | Вердикт |
|---|---|---|---|
| `Aspect` конструкторы, `getAspect`, `isPrimal`, `getComponents` | универсально | то же | без изменений; добавлен контракт в Javadoc |
| `Aspect.getName()` | первая буква заглавная, остальное как есть | `capitalizeFully` (остальное строчными, каждое слово) | **исправлено** (для 48 исходных результат тот же) |
| `Aspect.getPrimalAspects/getCompoundAspects/getCombinationResult` | живой реестр | живой реестр (`ResearchManager.getCombinationResult`) | без изменений поведения; чтение под блокировкой реестра |
| запись в реестр | `putIfAbsent` без блокировки | однопоточный FML | **синхронизировано** на `Aspect.aspects` (параллельное конструирование NeoForge) |
| `ThaumcraftApiHelper.getAllAspects/getAllCompoundAspects` | кэш по количеству навсегда | тот же вечный кэш (`getAllAspects:175-182`) | **исправлено**: кэш пересобирается, если реестр изменился (отклонение 1) |
| `AspectHelper.getObjectAspects`, `ThaumcraftApi.registerObjectTag*`, `ObjectAspectCatalog.initializePublicTags` | по объекту; базовые теги кладутся `putIfAbsent` | — | универсально: регистрация до инициализации Thaumcraft не теряется |
| `AspectList` (NBT `key/amount`, сортировки по тегу/количеству, `reduceToPrimals`) | по строковому тегу | то же (`getAspectsSorted` через `getTag().compareTo`) | без изменений; неизвестный тег при чтении отбрасывается |
| `PhialData`, `JarLabelData`, `CrystalEssenceData`, `WispEssenceData`, `ResearchNoteData.Cell` | тег проверяется по реестру | — | универсально; сторона без аспекта отклоняет компонент (контракт сети) |
| кодеки рецептов (`CrucibleRecipe`, `InfusionStackRecipe`, `InfusionEnchantmentRecipe`, `CrucibleRecipeDisplay`) | тег по реестру, предел `Aspect.aspects.size()` | — | универсально (датапаки грузятся после инициализации модов) |
| блок-сущности: `TileJar/JarContents`, `TileTube`, `TileTubeBuffer`, `TileAlembic`, `TileAlchemyFurnaceAdvanced`, `TileCentrifuge`, `TileEssentiaReservoir`, `TileThaumatorium`, `TileEssentiaCrystalizer`, `TileInfusionMatrix`, `TileDeconstructionTable`, `TileResearchTable` (бонусы) | тег-строка, проверка `EssentiaAtomicEndpoint.validAspect` = «зарегистрирован» | — | универсально |
| `PlayerKnowledge` (открытия/очки по строке) | — | — | универсально |
| `PlayerKnowledge.initializePrimalAspects` (первый вход) | фиксированные 6 тегов | все аспекты без компонентов, 15–19 очков, в порядке реестра (`ResearchManager.loadPlayerData:796-802`) | **исправлено**; для 48 исходных те же 6 тегов в том же порядке и те же броски RNG |
| пакеты знаний: `KnowledgeSyncPayload` (NBT), `AspectDiscoveryPayload`, `AspectPoolPayload` (строка ≤128) | по тегу | — | универсально |
| пакеты стола: `ResearchTableCombinePayload`, `ResearchTableStatePayload` (`isCanonicalBonusTag` по реестру) | по тегу | — | универсально |
| синхронизация сущностей (`EntityWisp`, `EntityAspectOrb`, `EntityGolemBase`) | строка | — | универсально |
| FX-пакеты (`VisNetFxPayload`, `EssentiaFlowFxPayload`, `BlockFxPayload`) | цвет RGB | — | универсально |
| `TileTube` вентиляция (block event) | индекс в порядке реестра | `Config.aspectOrder` (`initMisc:1009-1013`) — тоже индекс | без изменений, правило оригинала; требование одинакового порядка регистрации описано в Javadoc |
| `ContainerThaumatorium` (данные меню) | статический снимок реестра при загрузке класса | — | **исправлено**: снимок при открытии меню |
| `ContainerDeconstructionTable` (слот награды) | индекс в фиксированных 6 прималах; аддонный примал терялся | награда — любой примал (`reduceToPrimals`) | **исправлено**: индекс в `Aspect.getPrimalAspects()` (исходные 6 сохраняют 0–5) |
| `TileVisRelay`/`TileMagicWorkbenchCharger` импульс | 6 цветов | жёстко 6 (`TileVisRelay.addPulse:155-166`) | правило оригинала сохранено |
| `ContainerArcaneWorkbench`, `WandData.PRIMALS`, `VisAmuletData.PRIMALS`, `ThaumcraftBaubles` (кольца), `PrimalCharmRules`, руды/осколки/кластеры | 6 исходных прималов | палочки/амулеты/кольца — через `getPrimalAspects()`; кристаллы/руды — 6 | не менялось: код палочек вне моей зоны; пробел только для аддонных **прималов** (см. «Не проверено/пробелы») |
| иконки: `HoveredSlotAspectRow`, `GuiResearchTable`, `GuiArcaneWorkbench`, `GuiThaumatorium`, `GuiDeconstructionTable`, `ThaumcraftRecipeDisplay`, `ThaumatoriumBookDiagram`, `AdvancedFurnaceBookDiagram`, `RunicAugmentationDisplay`, `PlayerNotifications`, `ThaumcraftGogglesClient`, `ThaumometerReadout` (`RenderType.eyes`), `TileJarRenderer`/`TileAlembicRenderer`/`TileBannerRenderer`/`TileDeconstructionTableRenderer` (`entityTranslucent`), Jade `AspectElement`, EMI `AspectEmiStack`, JEI `AspectIngredient` | везде `aspect.getImage()` целиком (`blit(..,16,16,16,16)`), тон `getColor()` | `UtilsFX.drawTag` биндит `getImage()` | универсально; визуально **не проверено** |
| цвета: фиал (`ClientSetup`, слой 1), `ItemCrystalEssence.color`, `ItemWispEssence.color`, `ItemManaBean.color`, узлы/сферы (`getColor/getBlend`), резервуар (`ReservoirColour`) | по аспекту | — | универсально; blend: 771 — альфа, остальное аддитивно |
| `displayAspects` (`ItemCrystalEssence`, `ItemManaBean`, `ItemWispEssence`: случайный тип нетипизированного предмета, цикл цвета) | снимок реестра при загрузке класса — зависит от порядка инициализаторов | снимок в preInit Thaumcraft = на практике 48 | правило оригинала сделано явным: `OriginalDisplayAspects` (48) |
| креативная вкладка: фиалы (`ThaumcraftContent.register`) | цикл по реестру в момент регистрации: аспект, зарегистрированный позже, без фиала | `ItemEssence.getSubItems:84-90` — живой реестр | **исправлено**: поздние аспекты публикуются при сборке вкладки (отклонение 4) |
| креативная вкладка: эссенция виспа (`ItemWispEssence.creativeVariants`) | живой реестр при сборке | живой реестр | без изменений |
| стол исследований: палитра (`GuiResearchTable.palette`), `ResearchAspectCombination`, бонусы стола, случайный бонус (`TileResearchTable`) | открытые аспекты / `getCombinationResult` / реестр | то же | универсально |
| записки (`ResearchNoteData`, `ResearchNoteEditing`, `ResearchNotePuzzle.isDirectlyConnected`, `ResearchNoteGenerator`) | по реестру/компонентам | `ResearchManager.checkConnections` | универсально |
| таумометр: `ItemThaumometer` → `ObjectAspectResolver.scanTags` → `ScanManager.completeScan/awardScan` (родители, `tc.aspect.help.<parent>`) | универсально | то же | универсально |
| сканирование виспа (`EntityAspectBindings`): строки висп-вариантов | строятся при загрузке `ThaumcraftApi` (может быть раньше аддона) → висп аддонного аспекта не сканируется | `ConfigAspects.registerEntityAspects:98-99` в postInit — включает аддоны | **исправлено**: недостающие строки дописываются при старте сервера и при серверном `describe` (отклонение 3) |
| случайные выборы: `EntityWisp`, `NodeGeneration`, `EntityAspectBindings.playerAspects`, `TileManaPod`, `EntityPech`, `WarpEvents`, `LootTables`, `TileNode` | живой реестр | живой реестр (узлы — лениво при первой генерации) | универсально |
| центрифуга (`TileCentrifuge.inputComponents`) | любой зарегистрированный компаунд с двумя компонентами | случайный из двух компонентов | универсально (аддонные компаунды включены, как в оригинале) |
| тигель, алхимическая печь, тауматориум, алембик, резервуар, буфер, трубы | по тегу/списку | — | универсально |
| команды (`ThaumcraftCommands`), шпаргалка (`ResearchCheatSheet`) | живой реестр | живой реестр | универсально |
| страница ярлыков (`ThaumcraftRecipeDisplay.jarLabelAspects`), рецепт `jar_label_bind` | живой реестр / любой фиал | `ConfigRecipes`/`ConfigResearch` в postInit | универсально (код книги не трогал) |
| EMI/JEI (регистрация при перезагрузке плагина), Jade | живой реестр; id `thaumcraft:<tag>` | — | универсально при теге-пути; иначе предупреждение в лог |
| `research.json` (`ResearchCatalog`) | проверка цен по реестру | — | содержит только 48; аддонные исследования идут через research API — вне моей зоны, не трогал |

## Изменения

- `common/src/main/java/thaumcraft/api/aspects/Aspect.java` — Javadoc-контракт аддонного аспекта
  (когда регистрировать, тег, компоненты, текстура 32×32 белая/серая с прозрачностью, blend, chat colour,
  ключи `tc.aspect.<tag>`/`tc.aspect.help.<tag>`, сеть, потоки); `getName()` по `capitalizeFully`;
  запись в реестр и обход в `getPrimalAspects/getCompoundAspects/getCombinationResult` под
  `synchronized (aspects)`; предупреждение в лог для тега, не являющегося путём ресурса.
- `common/src/main/java/thaumcraft/api/ThaumcraftApiHelper.java` — кэши `getAllAspects`/`getAllCompoundAspects`
  пересобираются при изменении реестра или компонентов; методы `synchronized`.
- `common/src/main/java/thaumcraft/common/config/ThaumcraftContent.java` — фиалы аспектов, известных
  при регистрации, остаются на своих местах; поздние публикуются через `ThaumcraftCreativeTabs.publish`.
- `common/src/main/java/thaumcraft/common/items/OriginalDisplayAspects.java` (новый) и
  `ItemCrystalEssence`, `ItemManaBean`, `ItemWispEssence` — явный список 48 вместо снимка при загрузке класса.
- `common/src/main/java/thaumcraft/common/container/ContainerThaumatorium.java` — список аспектов меню на экземпляр.
- `common/src/main/java/thaumcraft/common/container/ContainerDeconstructionTable.java` — индекс награды в `getPrimalAspects()`.
- `common/src/main/java/thaumcraft/common/lib/research/PlayerKnowledge.java` — первый вход по правилу оригинала.
- `common/src/main/java/thaumcraft/common/lib/research/EntityAspectBindings.java` — `registerLateWispVariants()`,
  вызов в серверном `describe` и в `forOriginal`; снимок реестра под блокировкой.
- `common/src/main/java/thaumcraft/common/Thaumcraft.java` — `registerLateWispVariants()` в `SERVER_STARTED`.
- Тесты: `common/src/test/java/thaumcraft/api/AddonAspectTest.java` (10),
  `common/src/test/java/thaumcraft/items/OriginalDisplayAspectsTest.java` (1),
  `common/src/gameTest/java/thaumcraft/common/testing/AspectApiRuntimeChecks.java` (7 сценариев),
  обёртки в конце `fabric/.../ThaumcraftGameTests.java`, NeoForge-обёртки сгенерированы скриптом,
  7 новых имён в `scripts/porting/required-gametests.json`.

Тестовый аспект регистрируется только в тестах (уникальный тег `addon_test_<uuid>` /
`gametest_<назначение>_<uuid>`, компоненты vacuos + ordo — эту пару не занимает ни один исходный
аспект) и удаляется на всех путях выхода; JUnit проверяет после каждого теста, что в реестре снова 48.
GameTest-сценарии стоят в собственных батчах `addon_aspect_scan` и `addon_aspects`; все, кроме скана,
выполняются за один тик сервера.

GameTest-сценарии (`AspectApiRuntimeChecks`):

1. `addonAspectObjectTagScanDiscoversIt` — `registerObjectTag(CLOCK, tempus 4)`, резолвер, отказ
   `ScanManager.completeScan` без vacuos, затем настоящий таумометр по сущности-часам 20 реальных тиков →
   открыт, 4 + 2 очка.
2. `addonAspectEssentiaFillsAJarAndSurvivesReload` — рецепт `jar_label_bind` с аддонным фиалом, ярлык
   кликом, алембик → труба → банка (тикеры, сохранение суммы на каждом тике), фиал из банки и обратно,
   кодеки фиала, перезагрузка BE, update-тег клиента.
3. `addonAspectCombinesAtTheResearchTable` — настоящее меню стола и `ResearchTableCombineNetworking.handle`
   (vacuos + ordo → открыт, 3, затем 4), бонус стола сохраняется, `ResearchTableStatePayload`, записка с
   ячейкой аддонного аспекта, правило связи гексов.
4. `addonAspectRoundTripsThroughClientCodecs` — `KnowledgeSyncPayload` → `PlayerKnowledge.load` (как
   клиент), пакеты открытия/очков, `ItemStack.STREAM_CODEC`/диск для фиала, кристалла, боба, эссенции
   виспа, ярлыка; цвет; сторона без аспекта отклоняет фиал.
5. `addonAspectMenusSynchroniseIt` — тауматориум с аддонным запасом и стол разбора с аддонным
   приналом через настоящий `ContainerSynchronizer` на серверное и клиентское меню.
6. `addonAspectWispScansAndCentrifugeSplitsIt` — висп аддонного аспекта: aspect2 + praecantatio1 +
   aer1, подпись `@entity:Thaumcraft.WispType<tag>`; центрифуга: вход переживает перезагрузку, выход —
   vacuos или ordo.
7. `addonAspectCreativeTabListsItsPhial` — сборка вкладки: 48 исходных фиалов по порядку и фиал аддона один раз.

## Проверки

| Команда | Результат |
|---|---|
| `./gradlew :common:test --no-daemon --tests 'thaumcraft.api.AddonAspectTest' --tests 'thaumcraft.common.items.OriginalDisplayAspectsTest'` | **11/11** |
| `./gradlew :common:test --no-daemon --tests 'thaumcraft.api.*' --tests 'thaumcraft.research.*' --tests 'thaumcraft.common.lib.research.*' --tests 'thaumcraft.items.*' --tests 'thaumcraft.common.items.*' --tests 'thaumcraft.aspects.*' --tests 'thaumcraft.wands.*' --tests 'thaumcraft.tiles.*' --tests 'thaumcraft.golems.*' --tests 'thaumcraft.visnet.*' --tests 'thaumcraft.scanning.*' --tests 'thaumcraft.machines.*' --tests 'thaumcraft.integration.*' --tests 'thaumcraft.foci.*' --tests 'thaumcraft.essentia.*' --tests 'thaumcraft.crafting.*' --tests 'thaumcraft.common.lib.network.*' --tests 'thaumcraft.client.gui.*' --tests 'thaumcraft.baubles.*' --tests 'thaumcraft.common.lib.entities.*'` (все пакеты, тесты которых используют `Aspect`, включая `AspectContractTest`, `AspectApiTest`, `ApiCacheOwnershipTest`, `AspectParityTest`, `EntityAspectBindingsTest`, `EntityScanApiTest`, `CrystalEssenceDataTest`) | **1278/1278** (205 классов) |
| `./gradlew :common:test --no-daemon --tests 'thaumcraft.common.config.*' --tests 'thaumcraft.assets.*' --tests 'thaumcraft.common.lib.utils.*' --tests 'thaumcraft.client.*' --tests 'thaumcraft.common.*'` (аудиты исходников изменённых файлов) | **804/804** (161 класс; пересекается с предыдущим запуском) |
| `rm -rf fabric/run-gametest/world && ./gradlew :fabric:runGameTest --no-daemon -PgameTestFilter=<7 новых>,bootstrapAndItemComponents,playerKnowledgeMixinPersists,harvestedNodeEssenceDistillsRealAura,jarsPullFromNativeTransportAndKeepManualActions,researchAspectsCombineAndBonusesRegenerate,thaumatoriumFormsPullsAndPaysNativeOutputs,deviceWorldThaumatoriumPaysAndDelivers,deconstructionConsumesItemsAndProducesOriginalResearchPoints,scanApiUsesNativeNbtAndNodeLocators,objectAspectApiDirectAndGroupedTagsReachProductionConsumers,objectAspectApiCallbacksRetainOriginalExistingAmounts,constructAndCentrifugeRecipesAndPlainNativeBlocks,pechEntityScansReadActualThreeByteVariants,entityScansUseOriginalVariantsAndGrantNaturalClues,wispsKeepOriginalCombatMovementAndEssence,scanKnowledgeMissingParentsAndDiscovery,scanKnowledgePhysicalKindsAndRules,cheatSheetUsesServerPermissionAndOriginalKnowledgeRules,nuggetOwnership,adminCommandsRespectOriginalStateAndNativePermissions` | **27/27** («All 27 required tests passed»). Первый прогон (19 сценариев) — 17/19: оба отказа в моих новых сценариях (ключ записки должен быть существующим исследованием; неверное ожидание позиции фиала во вкладке на Fabric), исправлены тесты, не код |
| `python3 scripts/porting/neoforge_gametests.py --write`, затем `python3 scripts/porting/neoforge_gametests.py` (режим проверки — по умолчанию, флага `--check` у скрипта нет) | «NeoForge GameTest catalogue matches Fabric (470 wrappers)» |
| `python3 -m unittest scripts/porting/test_gametest_order.py scripts/porting/test_neoforge_gametests.py` | 6/6 OK |
| `./gradlew :fabric:build :neoforge:build --no-daemon` (после последних изменений исходников) | BUILD SUCCESSFUL, включая `verifyTestIsolation`/`verifyProductionIsolation` обоих загрузчиков |
| `./gradlew :neoforge:compileGameTestJava --no-daemon` (сгенерированные обёртки) | компилируется; запуск NeoForge GameTest не выполнялся |

## Отклонения

1. **Кэши `ThaumcraftApiHelper.getAllAspects/getAllCompoundAspects`.** Оригинал кэшировал первый ответ
   на количество навсегда; аспект, зарегистрированный после первого вызова, в ответы не попадал. Порт
   пересобирает кэш, если тот не совпадает с реестром (или с составными после `setComponents`). Если
   все аспекты зарегистрированы до первого вызова, ответ совпадает с оригиналом.
2. **`getName()` и локаль.** Оригинал понижал регистр локалью по умолчанию (`String.toLowerCase()` в
   `capitalizeFully`), порт — `Locale.ROOT`, чтобы турецкая локаль не меняла имена. Для 48 исходных и
   любых ASCII-тегов результат одинаков.
3. **Висп-варианты поздних аспектов.** В оригинале строки виспов строились в postInit Thaumcraft. В
   порте базовые строки строятся при загрузке `ThaumcraftApi`, а недостающие дописываются в конец
   публичного `ThaumcraftApi.scanEntities` при `SERVER_STARTED` и при первом серверном `describe`.
   Поэтому поздняя строка виспа стоит после строк, которые аддон добавил во время инициализации, и
   для того же `Type` побеждает её (оригинал: строки postInit Thaumcraft шли после init-строк аддонов,
   но до postInit-строк аддонов). Строки, удалённые аддоном, не восстанавливаются.
4. **Позиция поздних фиалов во вкладке.** Фиалы аспектов, известных при `ThaumcraftContent.register()`,
   остаются на своих местах; фиал аспекта, зарегистрированного позже, публикуется при сборке вкладки.
   На Fabric опубликованные стеки Architectury идут до добавленных (как уже было с эссенцией виспа),
   на NeoForge — через событие `BuildCreativeModeTabContentsEvent`; рядом с исходными фиалами поздний
   фиал не стоит. В оригинале все фиалы шли подряд.
5. **Синхронизация реестра.** Запись и обход внутри `Aspect` идут под `synchronized (Aspect.aspects)`;
   оригинал был однопоточным. В одном потоке поведение не меняется.
6. **Предупреждение о теге.** Тег, не являющийся путём ресурса (`[a-z0-9/._-]`), принимается, как в
   оригинале, но пишется предупреждение: идентификаторы EMI/JEI `thaumcraft:<tag>` для него не
   строятся.
7. **Формат слота награды стола разбора** — внутренний индекс меню теперь в `getPrimalAspects()`, а не
   в фиксированных шести; на сохранения не влияет, исходные 6 сохраняют индексы 0–5.

Правила оригинала, сохранённые сознательно: индекс вентиляции труб в порядке реестра; 6 цветов
импульса реле/зарядника; кристаллы, осколки, кластеры и руды — только 6 исходных прималов;
`displayAspects` — 48 (теперь явно, `OriginalDisplayAspects`); бандл `research.json` — 48.

## Не проверено и пробелы

- Отрисовка иконок аддонного аспекта (таумометр, книга, стол, подсказки, банки/ярлыки, Jade/EMI/JEI)
  в игре не осматривалась; вывод сделан по коду (везде `getImage()` целиком и тон `getColor()`).
- NeoForge: только сборка; GameTest-сценарии на NeoForge не запускались. Параллельное конструирование
  модов NeoForge не воспроизводилось — синхронизация проверена только компиляцией и однопоточными
  тестами.
- Клиент с другим набором аспектов проверен только на уровне кодека (`addonAspectRoundTripsThroughClientCodecs`),
  не настоящим подключением.
- Аддонные **прималы**: палочки, скипетры, амулеты, кольца скидки, аркан-верстак, заряд реле — в порте
  только 6 исходных прималов, хотя оригинал брал `getPrimalAspects()`. Код палочек сейчас меняют
  другие; не трогал. Первичные очки при первом входе и награда стола разбора для таких прималов
  исправлены.
- Аддонные исследования с ценой в аддонных аспектах (research API) не проверялись — вне зоны задачи.
