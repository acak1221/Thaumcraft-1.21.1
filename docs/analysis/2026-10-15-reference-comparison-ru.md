# Проверка исходников и референсов для Minecraft 1.21.1

Дата: 15.10.2026. Это инвентаризация **всех committed файлов** и адресная семантическая проверка архитектуры, research и Thaumonomicon. Каждый Java-файл учтён и хэширован; утверждения о построчном аудите каждой реализации здесь нет. Сборки референсов и Gradle в рамках этой проверки не запускались.

## Зафиксированные корпуса

| Корпус | Проверенный commit | Target в исходниках |
|---|---|---|
| legacy-1.12 | `8abb7febda5e4aceac5304277b17154760fc1d38` | Активные common/Fabric/NeoForge: 1.21.1; сохранённый `src/`: 1.12 reference |
| [OpenTC4](https://github.com/IgnoreLicensesCN/OpenTC4/tree/80aac29c52543ef51766c4b0788fba782abb3b0b) | `80aac29c52543ef51766c4b0788fba782abb3b0b` | Forge 1.7.10, ForgeGradle 1.2.11 |
| [OpenTC4-Architectury](https://github.com/IgnoreLicensesCN/OpenTC4-Architectury/tree/1509b3192910aaca0af8bba1b55d189a675010bf) | `1509b3192910aaca0af8bba1b55d189a675010bf` | 1.20.1, Fabric + Forge, Java 21 |

Оба reference checkout были чистыми при проверке. Инвентарь создан через `git archive` точного commit каждого корпуса, поэтому изменения других агентов во время текущей работы в него не попадают. Original oracle остаётся вне репозитория: `../work/thaumcraft-4.2.3.5-oracle/`. Его gameplay rules имеют приоритет над исправлениями и переработками из обоих GitHub refs.

Полные файлы с путями, byte size, subsystem, Java line count и нормализованным SHA-256:

- [legacy-1.12](2026-10-15-inventory-legacy-1.12.json).
- [OpenTC4](2026-10-15-inventory-OpenTC4.json).
- [OpenTC4-Architectury](2026-10-15-inventory-OpenTC4-Architectury.json).
- [Сводные счётчики, extensions, source sets и lexical markers](2026-10-15-reference-counts.json).

### Текущий upstream проверен отдельно от baseline

15.10.2026 после `fetch origin master` актуальный Architectury upstream — [`3a836a39c416a4449e825c519dacd50a5fc9eb32`](https://github.com/IgnoreLicensesCN/OpenTC4-Architectury/tree/3a836a39c416a4449e825c519dacd50a5fc9eb32), author timestamp 15.10.2026 14:25:20 +08:00. Между ним и pinned baseline `1509b319` два commits, **45 изменённых файлов, +443/-733 строк**. Checkout HEAD остался `1509b319`; commit pin в source-lock сохранён. OpenTC4 upstream при проверке основным агентом остался `80aac29c`.

Полный свежий corpus: [отдельный latest inventory](2026-10-15-inventory-OpenTC4-Architectury-upstream.json), **6 129 файлов, 1 989 Java, 218 550 Java-строк**. [Машиночитаемый delta](2026-10-15-Architectury-upstream-delta.json) содержит все 45 изменений, rename и построчную статистику.

Дельта классифицирована по всем 45 путям; ключевые изменения реализации проверены адресно:

- Большинство затрагивает ResourceLocation templates и generated classes: сокращение templates до include directives, generated `ofExisting` factories, переход некоторых public constructors в protected. `build.gradle` меняет annotation text, убирает debug print и добавляет TODO preprocessing; target, loader set, dependencies и shadow/remap architecture не изменены.
- [`PacketAspectCombinationC2S`](https://github.com/IgnoreLicensesCN/OpenTC4-Architectury/blob/3a836a39c416a4449e825c519dacd50a5fc9eb32/common/src/main/java/thaumcraft/common/lib/network/toserveraction/PacketAspectCombinationC2S.java), `PacketAspectPlaceC2S` и `PacketPlayerCreateResearchNoteC2S` теперь используют `ofExisting` при decode и отвергают empty IDs/aspects. Это полезный пример lookup существующих registered IDs вместо создания произвольных cache keys. Полнота исправления не проверена runtime; например `PacketPlayerCompleteResearchWithAspectC2S.decode` всё ещё вызывает создающий `ResearchItemResourceLocation.of`.
- `Aspect.EMPTY` получает empty namespace вместо `thaumcraft`; `FurthestBlockFinder` переименован в `HighestFurthestBlockFinder`, call site axe обновлён, алгоритм поиска не изменён.
- Ключевые незавершённые research/GUI файлы **byte-identical** baseline: `ThaumcraftResearches` с шестью `= ;`, `ThaumonomiconItem` с TODO открытия, `ResearchPage` shell, старый `GuiResearchBrowser`, `ResearchTableMenu`. Latest также сохраняет 1.20.1/Fabric+Forge, localhost proxy и отсутствие test sources.
- Новая compile incompatibility видна статически: [`ResearchCategoryResourceLocation`](https://github.com/IgnoreLicensesCN/OpenTC4-Architectury/blob/3a836a39c416a4449e825c519dacd50a5fc9eb32/common/src/main/java/thaumcraft/common/lib/resourcelocations/ResearchCategoryResourceLocation.java) сделал constructors protected, но `api.research.client.ThaumcraftResearchCategories` из другого package по-прежнему создаёт шесть объектов через `new`. Build latest не запускался; этот факт не подменяется заявлением о результате compiler run.

Следовательно, свежие commits полезны для точечной проверки decoding/codegen, но не меняют выбор gameplay reference и не превращают upstream в готовую базу для 1.21.1. Далее числовые сравнительные таблицы описывают **pinned baseline**; свежий corpus хранится и указан отдельно.

Java hashes нормализуют UTF-8 BOM и CRLF/CR к LF. Java line counts включают comments и пустые строки. Инвентарь описывает файлы, а не число реализованных игровых units.

## Размер и размещение

| Показатель | legacy-1.12 | OpenTC4 | Architectury ref |
|---|---:|---:|---:|
| Все committed файлы | 5 562 | 2 050 | 6 129 |
| Java-файлы | 1 511 | 911 | 1 989 |
| Java-строки | 201 783 | 135 804 | 218 840 |
| PNG | 2 475 | 822 | 1 200 |
| OGG | 333 | 111 | 111 |
| JSON | 673 | 9 | 2 633 |

legacy-1.12 содержит 934 legacy main Java и 530 legacy test Java. Активная основа до текущей рабочей итерации: 29 common main, 12 common tests, 3 Fabric main, 3 NeoForge main. Поэтому большое число старых тестов и классов нельзя считать покрытием 1.21.1.

OpenTC4 содержит 911 main Java, без `src/test`. Architectury содержит 1 957 common main Java, 9 Fabric, 4 Forge, ещё 19 Java templates вне main source roots; test source set отсутствует.

### Java по основным subsystem packages

Счётчики включают main source sets; legacy-1.12 в этой таблице содержит одновременно legacy и active sources. Остальные утилиты, loader bootstrap и корневые classes подробно перечислены в JSON.

| Package/subsystem | legacy-1.12 | OpenTC4 | Architectury ref |
|---|---:|---:|---:|
| API | 91 | 101 | 302 |
| GUI | 20 | 20 | 20 |
| Renderers | 181 | 166 | 172 |
| FX | 40 | 37 | 75 |
| Blocks | 73 | 69 | 232 |
| Items | 92 | 84 | 218 |
| Tiles/block entities | 79 | 78 | 158 |
| Entities | 138 | 129 | 220 |
| Network | 43 | 40 | 51 |
| Research logic | 9 | 4 | 5 |
| World/dimensions | 39 | 38 | 53 |
| Config/registration | 22 | 9 | 9 |

## Выводы по реальному коду

1. **Architectury ref не является готовым работающим портом.** В [`ThaumcraftResearches.java:30`](https://github.com/IgnoreLicensesCN/OpenTC4-Architectury/blob/1509b3192910aaca0af8bba1b55d189a675010bf/common/src/main/java/thaumcraft/api/research/ThaumcraftResearches.java#L30) шесть initializer expressions не закончены (`= ;`), ещё три final поля не имеют assignment. Это конкретная синтаксическая проблема, независимо от отсутствующей проверки Gradle.
2. **Thaumonomicon из Architectury не открывает книгу.** [`ThaumonomiconItem.use:21`](https://github.com/IgnoreLicensesCN/OpenTC4-Architectury/blob/1509b3192910aaca0af8bba1b55d189a675010bf/common/src/main/java/thaumcraft/common/items/misc/ThaumonomiconItem.java#L21) содержит TODO для GUI и возвращает новый stack. Старый [`GuiResearchBrowser:4`](https://github.com/IgnoreLicensesCN/OpenTC4-Architectury/blob/1509b3192910aaca0af8bba1b55d189a675010bf/common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java#L4) по-прежнему импортирует FML `cpw.mods`, `GuiScreen`, `FontRenderer`, `GuiButton` и прямой LWJGL API. Его нельзя подключить к modern Screen простым переименованием imports.
3. **Новая research API меняет модель данных.** Вместо TC4 `ResearchItem` с полями/flags появляются abstract research classes, typed ResourceLocation keys, interfaces для разных способов unlock и отдельные client categories. [`ResearchPage:10`](https://github.com/IgnoreLicensesCN/OpenTC4-Architectury/blob/1509b3192910aaca0af8bba1b55d189a675010bf/common/src/main/java/thaumcraft/api/research/client/ResearchPage.java#L10) оставлен abstract shell; enum page types и constructors закомментированы. Это материал для оценки дизайна, не совместимая реализация оригинальной API.
4. **Research table тоже незавершён.** Одновременно сохранён почти полностью закомментированный `tiles/junkbox/TileResearchTable` и новый `ResearchTableBlockEntity`. [`ResearchTableMenu:38`](https://github.com/IgnoreLicensesCN/OpenTC4-Architectury/blob/1509b3192910aaca0af8bba1b55d189a675010bf/common/src/main/java/thaumcraft/common/menu/menu/ResearchTableMenu.java#L38) явно оставляет aspect placement/combination/copying как TODO.
5. **Build refs нельзя использовать как baseline 1.21.1.** Architectury properties фиксируют 1.20.1, Forge, Accessories 1.20 и локальный proxy `localhost:10808`. Root build генерирует ResourceLocation subclasses в main sources и вручную связывает shadow/remap outputs. На target нужны уже существующие 1.21.1 wrapper, mappings, components и loader modules.
6. **Количество новых файлов скрывает сосуществование разных поколений кода.** В Architectury 142 Java в `junkbox`, 195 Java с выбранными obsolete imports, 171 Java с TODO. Эти лексические признаки перечислены пофайлово в JSON и не являются процентом готовности. Между OpenTC4 refs 495 одинаковых package paths, но только 26 файлов идентичны после нормализации; часть неизменённых файлов — старые render models/AI/утилиты. Само совпадение имени класса ничего не доказывает.

## Что использовать для 1.21.1

| Область | Полезный reference | Действие |
|---|---|---|
| Original numbers, tags, research keys и quirks | Только pinned TC4 4.2.3.5 oracle | Сохранить как gameplay specification |
| Сопоставление старых классов и поведение исправленного legacy-1.12 | Legacy `src/` | Читать как вторичный reference; переносить только проверенные fixes |
| Читаемые имена, old registry/recipe call sites | OpenTC4 1.7 | Использовать для поиска и сравнения; отдельно проверять Hodgepodge/TC4Tweaks changes |
| Loader module layout и отдельные modern mapping patterns | Architectury ref | Изучать точечно, реализацию сверять с 1.21.1 target |
| Main research runtime и GUI | Existing extracted catalog + original GUI/manager/item | Реализовать нативно, с server authority и реальным клиентом |
| Textures/sounds/models/lang | Original assets и уже существующий deterministic pipeline | Сверять hashes, resource linkage и визуал; не считать 2 633 reference JSON доказательством рабочего rendering |
| Api/compat extensions | OpenTC4 extensions и Architectury abstract API | Отложить до рабочего TC4 API; не менять canonical keys ради ref-дизайна |
| Worldgen, dimensions, entities, block entities | Original + legacy unit mapping | Переносить по отдельным native units с lifecycle/save/load evidence |

Оба GitHub README прямо описывают decompiled/reworked TC4. Их template license не устанавливает права на original TC4 assets/code. Работа остаётся локальной; push/release этой проверкой не разрешались и не выполнялись.

## Точная следующая цепочка research units

1. **Читаемый Thaumonomicon.** Реальный item use, server-side normal-book sibling effects, S2C knowledge, нативный Screen с категориями/графом и завершёнными text pages. Проверять Virtual, Hidden/Lost `@` clues, Concealed parents/hidden parents, ELDRITCHMINOR tab gate, gated text. Каталог уже имеет 201 definitions и 591 descriptors: 310 text descriptors (15 gated), 273 recipe references, 8 legacy expressions. Recipe references ещё не являются native recipes.
2. **Research unlock service.** Difficulty modes, primary notes, secondary aspect purchase, server-side atomic deductions, exact client feedback, hidden clue triggers. Поля conditions в 15 definitions — исходные Java expressions, а не текущая конфигурация; их resolver остаётся отдельной работой.
3. **Research note data + item.** Persistent/network data component, original hex generation, copy/ink/paper costs, deterministic behavioral checks и malformed input rejection.
4. **Research table.** Двухблочная structure, block entity, inventory/menu, research pool/bonus aspects, place/remove/combine/copy handlers; повторять original transitions на сервере и проверять conservation/reload.
5. **Recipe pages и рабочие devices.** Native typed recipe registry, arcane/crucible/infusion matchers; после каждого типа — книга показывает тот же зарегистрированный рецепт, который действительно исполняется устройством.

### Проверенные original browser/book predicates

Original `GuiResearchBrowser` icon loop: Virtual никогда не рисуется; completion и `@KEY` каждый обходят Hidden/Lost/Concealed visibility checks; обычный research виден даже без родителей, Concealed требует normal и hidden parents. ELDRITCH category отсутствует до `ELDRITCHMINOR`. Original `GuiResearchRecipe` удаляет TEXT_CONCEALED до соответствующего completed key; незавершённый research не открывается из browser.

Original normal `ItemThaumonomicon` выполняет **один live pass** по completed research и открывает siblings **без повторной проверки их parents**. `completeResearch` добавляет warp: permanent `ceil(warp/2)`, sticky `floor(warp/2)`, обновляет warp counter; повторный unlock ничего не начисляет. Native wuss config пока отсутствует, для этой unit используется original default `false`.

Catalog отсортирован по key, original categories были LinkedHashMap, а entries внутри категории — HashMap. Для pinned corpus доказано отсутствие влияния порядка: всего три sibling links, `DISTILESSENTIA→JARLABEL`, `GOLEMSTRAW→COREGATHER`, `GOLEMSTRAW→GOLEMBELL`; targets не имеют siblings и все имеют warp 0. Fixed-point closure вводить нельзя. Реализация policy и behavioral tests добавлены в `ResearchBrowserData`/`ResearchBrowserDataTest`; итоговый build и запуск тестов выполняет основной агент.

Это целевая последовательность работ, а не заявление о полном TC4 parity.

## Уточнение fidelity: HexUtils

До подключения research puzzle исправлена найденная разница `distributeRingRandomly`: original oracle (`HexUtils.java:53–63`) вызывает `nextInt(ring.size())`, но не использует результат и начинает обход с позиции 0. Native helper теперь сохраняет фиксированную ориентацию endpoints **и** один RNG draw, в том числе при `entries=0` для допустимого кольца. Regression checks фиксируют точный список из пяти endpoints на радиусе 2 и следующее состояние RNG. Safe bounds для невозможных входов и ранее документированная `toCubicHex` deviation сохранены. Gradle/JUnit для этого исправления запускает основной агент; автор этой проверки их не запускал.
