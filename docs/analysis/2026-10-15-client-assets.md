# Клиент, визуализация и ресурсы

Дата: 15.10.2026. Исходная ревизия: `4bc68454b1063cdc1255f833dd537e0fd4fc341d`.

Это статический аудит клиентской архитектуры, GUI, рендереров, моделей, шейдеров, частиц, JEI/Hwyla и ресурсов. Проверены `AGENTS.md`, `build.gradle`, `Dockerfile`; активный `.opencode/goal.md` отсутствует. Production-код и оригинальные материалы не изменялись. Компиляция, Gradle/JUnit, сервер и игровой клиент в рамках этого раздела не запускались. Выводы о наличии файлов и управляющих ветвях подтверждены исходниками; визуальные эффекты и производительность требуют игрового воспроизведения. Наличие ресурсов и успешный разбор JSON не доказывают визуальное соответствие TC4.

## Масштаб и архитектура

Инвентаризация всех отслеживаемых файлов выполнена через `git ls-files`, поэтому учитывает регистр имён даже на Windows. Ручной подробный разбор сосредоточен на точках регистрации, загрузке ресурсов, частицах, шейдерах, исследовательских GUI и интеграциях; все 258 клиентских файлов не прочитаны построчно.

| Область | Java-файлов | Основные обязанности |
|---|---:|---|
| `ClientProxy`, `ClientModelRegistry` | 2 | Регистрация моделей, TESR, сущностей, цветов, обработчиков, маршрутизация GUI |
| `client/gui` | 20 | Исследования, Thaumonomicon, интерфейсы устройств, карта предметов |
| `client/lib` | 9 | HUD, клавиши, клиентские события, уведомления, shader groups |
| `client/fx` | 40 | Очереди и слои частиц, лучи, молнии |
| `client/renderers/block` | 5 | Замены baked models, дополнительные состояния геометрии |
| `client/renderers/entity` | 46 | Мобы, снаряды, предметы-сущности |
| `client/renderers/item` | 26 | TEISR, перспективы, позы и калибровка палочки |
| `client/renderers/models` | 46 | Java-модели устройств, брони и сущностей |
| `client/renderers/tile` | 58 | TESR, отрисовка узлов, транспорт, портал и кэш поверхности дыры |
| `client/integration/jei` | 6 | Преобразование рецептов, категории, фильтрация по исследованиям |
| **Всего client Java** | **258** | |

Реестр моделей подписан только на клиентские события (`src/main/java/thaumcraft/client/ClientModelRegistry.java:29`). `ModelRegistryEvent` вызывает proxy-регистрацию (`:77`), `TextureStitchEvent.Pre` добавляет используемые спрайты (`:82`), `ModelBakeEvent` заменяет baked models и создаёт frost shard OBJ (`:112`, `:145`). Это правильное разделение этапов 1.12.2, а не механическое использование renderer API из 1.7.10.

`ClientProxy.registerModelLocations()` содержит явные маршруты для метаданных предметов, затем fallback по registry name (`src/main/java/thaumcraft/client/ClientProxy.java:364`, `:692`). TESR устройств подключены в отдельной регистрации (`:1198`); обработчики частиц, HUD и клавиш регистрируются на клиенте (`:1271`, `:1281`). `getClientGuiElement()` проверяет `WorldClient`, типы tile/entity и границы индекса руки перед созданием GUI (`:1426`). `GuiFactory.hasConfigGui()` возвращает false (`src/main/java/thaumcraft/client/gui/GuiFactory.java:15`): графического редактора конфигурации пока нет, это явное ограничение функциональности.

## Инвентаризация ресурсов

В `src/main/resources` отслеживается **1 676 файлов**: 827 PNG, 111 OGG, 588 JSON, 102 `.mcmeta`, 22 `.lang`, 16 OBJ, 3 MTL, 6 shader source-файлов и 1 `.info`.

| Поддерево | Файлов |
|---|---:|
| `assets/thaumcraft/blockstates` | 45 |
| `assets/thaumcraft/models` | 535 |
| `assets/thaumcraft/textures` | 947 |
| `assets/thaumcraft/lang` | 22 |
| `assets/thaumcraft/sounds` | 111 |
| `assets/thaumcraft/sounds.json` | 1 |
| `assets/thaumcraft/render_calibration` | 1 |
| `assets/thaumcraft/shader` | 3 |
| `assets/minecraft/shaders` | 9 |
| Корень: metadata | 2 |

Проверки файлами и разбором JSON:

- Все 690 JSON/`.mcmeta` разобраны `ConvertFrom-Json` без ошибки синтаксического чтения. Это не проверка schema Minecraft/Forge и не проверка дублирующихся ключей строгим JSON-парсером.
- Сопоставлены явные локальные `thaumcraft:` parent/model/texture-ссылки в model JSON и model-ссылки blockstates; отсутствующих целевых файлов среди этих ссылок не выявлено. Vanilla/Forge parents, динамические ссылки и соответствие каждого runtime blockstate JSON-вариантам полностью не проверены.
- Проверены 262 литеральные `ResourceLocation` с расширением `.png/.obj/.json/.ttf/.ogg` в Java. Найдена одна отсутствующая локальная texture-ссылка, описанная ниже. Строки вида `thaumcraft:textures/...png` в броне и регистрации жителей также проверены без дополнительных отсутствующих файлов.
- `sounds.json` содержит 66 событий и 111 ссылок на sound files; все явно указанные локальные OGG существуют. Декодирование аудио и сопоставление каждого `SoundEvent` с JSON не выполнялись.
- Сравнение Git blob поддеревьев textures с read-only `thaumcraft_src/assets/thaumcraft/textures`, после нормализации регистра пути: все **940 исходных путей сохранены**; 939 blob совпадают, изменён `misc/potions.png`. Добавлены 6 путей: `blocks/chesthungry.png` и inventory-текстуры `centrifuge`, `decontable`, `table`, `wandtable`, `worktable`. Это проверка происхождения/сохранности байтов; визуальная корректность изменения `potions.png` не оценивалась.

Shaders с именами `shaders/post/...` намеренно лежат в `assets/minecraft`, а не в `assets/thaumcraft`: однопараметрический `ResourceLocation` в `ClientTickEventsFML.java:35` использует Minecraft namespace. Четыре post JSON существуют. Custom bloom programs и vertex shader поставляются в том же namespace; программы `blur`, `blit`, `color_convolve` ожидаются от vanilla. Три файла `assets/thaumcraft/shader/test*` не имеют обнаруженных ссылок загрузки в production Java; наличие файла не означает, что этот эффект исполняется.

### Коллизия регистра на Windows

Git содержит `textures/models/Bore.png` и `textures/models/bore.png`. Оба пути имеют один и тот же blob `0c9d0a462ff01981b421518944562ef9e96f04f3`; различий в байтах нет. В текущем Windows worktree физическое имя — `bore.png`, используемое в `TileArcaneBoreRenderer.java:16`, `TileArcaneBoreBaseRenderer.java:12`, `TileAlembicRenderer.java:23`, `TileArcaneLampRenderer.java:24`.

Это подтверждённая платформенная коллизия checkout/packaging, но не доказательство потери разных текстур. Финальный JAR на case-sensitive системе должен быть проверен по именам entries. Возможная будущая правка: оставить канонический lowercase-путь при сохранении тех же байтов; в этом аудите переименование не выполнялось.

## Выявленные проблемы и риски

### CA-01 — отсутствующая texture-ссылка powered taint creeper

**Приоритет: средний; отсутствие ресурса подтверждено, визуальный дефект не воспроизводился.**

`src/main/java/thaumcraft/client/renderers/entity/RenderTaintCreeper.java:19` задаёт `thaumcraft:textures/entity/creeper/creeper_armor.png`. В Git этого файла нет; `CreeperArmorLayer` использует его при `getPowered()` (`:66`, `:70`). Базовая texture `textures/models/creeper.png` существует и не является проблемой.

Ожидаемый эффект: слой заряженного крипера получает missing texture при отсутствии исправляющего resource pack. Нужно в игре вызвать powered taint creeper и проверить текстуру/лог; затем установить правильный namespace или исходный asset по оригинальной модели. Read-only извлечение строк constant pool оригинального `RenderTaintCreeper.class` показывает тот же относительный путь armor texture; оно не доказывает namespace конструктора без декомпиляции.

### CA-02 — shader groups не очищаются при выходе из мира

**Приоритет: средний; проблема жизненного цикла подтверждена управляющим потоком, GPU-эффект не измерялся.**

`ClientTickEventsFML.playerTick()` делает return при `mc.player == null` (`src/main/java/thaumcraft/client/lib/ClientTickEventsFML.java:52`), а `checkShaders()` расположен после этой проверки (`:63`). Поэтому существующая null-player ветвь `deactivateAllShaders()` (`:86`) из этого единственного вызова недостижима. Shader groups находятся в static map (`src/main/java/thaumcraft/client/lib/RenderEventHandler.java:82`). Logout-обработчик восстанавливает только конфигурацию (`src/main/java/thaumcraft/common/lib/network/EventHandlerNetwork.java:9`); других вызовов shader cleanup на disconnect/unload в production Java не обнаружено.

Следствие из исходников: после выхода во время shader-эффекта объекты остаются в map до следующей подходящей проверки или завершения процесса. Возможные видимые последствия — shader в menu overlay и удержание framebuffer/GL resources; это требует воспроизведения. Проверка: получить один из четырёх potion-эффектов, выйти в меню, проверить map/GL allocation, затем подключиться к другому миру. Cleanup целесообразно привязать к клиентскому tick без player и к смене/выгрузке мира.

### CA-03 — pending-очередь TC particles не ограничена

**Приоритет: средний; отсутствие лимита подтверждено, исчерпание памяти не воспроизводилось.**

`ParticleEngine` объявляет лимит pending particles 2 000 (`src/main/java/thaumcraft/client/fx/ParticleEngine.java:39`), но ветвь `particle instanceof ITCParticle` напрямую `offer()` и возвращается до проверки этого лимита (`:62`). Ограничение применяется только к другой очереди (`:70`). Для `pendingTCParticles` используется неограниченная `ConcurrentLinkedQueue` (`:43`). Drain принимает максимум 4 096 активных добавлений за tick (`:151`); ограничение 2 000 применяется позже к уже активному списку (`:162`). Во время паузы drain не идёт (`:92`).

При длительном потоке генерации быстрее потребления очередь может расти; высокую нагрузку и генерацию во время pause ещё нужно подтвердить конкретными источниками FX. Ограничивать следует pending TC по dimension/layer и выбирать явную политику drop. `WorldEvent.Unload` и world-null tick уже очищают очереди (`:129`, `:87`), поэтому не следует описывать эту проблему как доказанную вечную утечку после выгрузки мира.

### CA-04 — неполное восстановление GL blend state в узлах и layered field

**Приоритет: низкий/средний; выходное состояние видно статически, влияние на следующий renderer не доказано.**

`TileNodeRenderer.renderNodeSeeded()` выбирает additive blend для normal/pure/hungry узлов (`src/main/java/thaumcraft/client/renderers/tile/TileNodeRenderer.java:312`, `:327`, `:332`), затем отключает blending без возврата стандартной blend function (`:341`). Fallback также оставляет `SRC_ALPHA, ONE` после `disableBlend` (`:356`, `:362`). `LayeredFieldPlaneHelper` после первого слоя переходит на `blendFunc(1, 1)` (`src/main/java/thaumcraft/client/renderers/tile/LayeredFieldPlaneHelper.java:96`) и заканчивает только отключением blend (`:118`). Матрица, culling/depth и lightmap восстанавливаются отдельно, что не восстанавливает blend coefficients.

Это не означает автоматически неверный кадр: следующий renderer может задать свою blend function. Но shared GL state зависит от порядка отрисовки. Полезно воспроизвести соседство узла/портала и полупрозрачных объектов, затем привести выходной blend contract к стандартному или сохранённому входному состоянию. Положительный пример уже есть в `RenderWisp.java:59` и `ParticleEngine.java:255`; соответствующий guard расположен в `src/test/java/thaumcraft/client/WispRenderStateStaticGuardTest.java:15`.

Кроме того, `ParticleEngine.renderTCParticles()` и `TileHoleRenderer.render()` не используют `finally` для всего GL/BufferBuilder pass (`ParticleEngine.java:219`, `TileHoleRenderer.java:36`). Исключение из particle renderer или field helper может оставить изменённое состояние; произвольное исключение в этих местах не воспроизводилось. `ItemWandRenderer.renderByItem()` уже содержит cleanup в `finally` (`src/main/java/thaumcraft/client/renderers/item/ItemWandRenderer.java:125`).

### CA-05 — addon callbacks вызываются из фонового mapper thread

**Приоритет: низкий/средний; риск совместимости, не подтверждённый crash.**

`ClientTickEventsFML` запускает daemon `MappingThread` (`src/main/java/thaumcraft/client/lib/ClientTickEventsFML.java:57`), который обходит registry и вызывает `item.getSubItems()` для каждого vanilla/modded item (`src/main/java/thaumcraft/client/gui/MappingThread.java:16`, `:23`). Кэш безопасен для конкурентной записи (`src/main/java/thaumcraft/client/gui/GuiResearchRecipe.java:63`, `:135`), но `ConcurrentHashMap` не делает произвольные addon callbacks безопасными вне client thread. RuntimeException при таком вызове молча игнорируется (`MappingThread.java:31`), вследствие чего часть карты предметов может не появиться и диагностика теряется.

Нужно проверить callback сложного аддона и полноту карты ASPECTS. Долгосрочный вариант — выполнять callback на client thread порциями, а тяжёлые чистые вычисления выносить отдельно. Факта гонки или потерянного конкретного предмета в этом аудите нет.

### CA-06 — shader reload и ошибки shader loading не диагностируются

**Приоритет: низкий; статическое ограничение обслуживания.**

Resource reload listener регистрируется для калибровки палочки (`src/main/java/thaumcraft/client/ClientProxy.java:1295`), но не для сохранённых shader groups. `ensureShader()` пропускает создание уже существующего id (`ClientTickEventsFML.java:120`), то есть изменение shader assets через F3+T не сопровождается явным пересозданием этих групп. `ensureShader()` и shader rendering проглатывают ошибки (`:125`, `RenderEventHandler.java:380`); неудачное создание при сохраняющемся potion-эффекте будет снова пробоваться каждым player tick.

Проверка: модифицировать shader resource в pack, перезагрузить ресурсы при активном эффекте, проверить программу и логи; поведение внутреннего vanilla ShaderGroup reload без запуска игры не утверждается. Минимальный диагностический путь — rate-limited logging и явный reload/cleanup contract.

## JEI/Hwyla и client/server границы

JEI и Hwyla объявлены `deobfProvided`, без обязательной runtime-зависимости (`build.gradle:55`, `:56`). JEI entrypoint — `@JEIPlugin` (`src/main/java/thaumcraft/client/integration/jei/ThaumcraftJeiPlugin.java:20`); ссылки `mezz.jei` ограничены `client/integration/jei`. Waila entrypoint — `@WailaPlugin` (`src/main/java/thaumcraft/common/integration/waila/GolemWailaPlugin.java:17`); он не импортирует `net.minecraft.client` и подключает только body provider для `IGolemInfo` (`:25`). Обычный proxy/lifecycle не выполняет прямое создание этих plugin-классов.

Есть static boundary tests (`src/test/java/thaumcraft/integration/jei/JeiOptionalBoundaryGuardTest.java:18`, `src/test/java/thaumcraft/common/integration/waila/WailaOptionalBoundaryGuardTest.java:18`). Они проверяют исходные границы и metadata; запуск без JEI/Hwyla или с конкретной версией modset в этом разделе не выполнялся. Отсутствие eager-ссылок повышает надёжность optional loading, но не заменяет dedicated-server/modset smoke.

JEI формирует три категории: arcane, crucible, infusion (`ThaumcraftJeiPlugin.java:23`). `JeiRecipeData` использует canonical `ThaumcraftApi.getCraftingRecipes()` через plugin (`:43`), делает копии stacks/aspects и учитывает skipped classes. Infusion enchantment/runic augment и dynamic IArcaneRecipe сознательно пропускаются (`src/main/java/thaumcraft/client/integration/jei/JeiRecipeData.java:135`, `:146`); полное покрытие всех рецептов в JEI пока не реализовано. Это не потеря server-рецептов.

Research-gated recipes изначально скрыты (`ThaumcraftJeiPlugin.java:95`), затем visible set обновляется по completed research. Смена player/world identity и null context скрывают прежние записи (`src/main/java/thaumcraft/client/integration/jei/ResearchVisibility.java:51`, `:59`). Такая модель предотвращает перенос видимости между мирами. Ticker каждый END tick снимает копию completed research (`src/main/java/thaumcraft/client/integration/jei/ResearchVisibilityTicker.java:26`); state machine дополнительно сравнивает snapshot перед пересчётом. На очень больших исследованиях есть стоимость копирования set каждый tick, профилирование не проводилось.

Главный proxy разделён через `@SidedProxy` (`src/main/java/thaumcraft/common/Thaumcraft.java:94`). В common-классах встречаются client imports и модели брони; сами imports не являются доказательством server crash. Например, model creation заключён в `@SideOnly(Side.CLIENT)` callback (`src/main/java/thaumcraft/common/items/armor/ItemCultistPlateArmor.java:51`), однако поля `ModelBiped` находятся в common item (`:22`). Проверка classloading таких границ требует server smoke; автоматической некорректности этих полей здесь не заявляется.

## Существующие тесты и границы их доказательности

В репозитории присутствуют проверки визуальных контрактов, геометрии, atlas textures, client FX и optional integrations. `MissingTextureParticleAssetContractTest` разбирает block model textures и проверяет квадратный/animated carrier (`src/test/java/thaumcraft/rendering/MissingTextureParticleAssetContractTest.java:31`); model coverage guard проверяет дверные parents, Forge fluid names и варианты arcane furnace (`src/test/java/thaumcraft/client/ModelErrorBurstAssetCoverageTest.java:16`). Эти проверки полезнее простого existence check, но в текущем разделе они только прочитаны.

Часть client tests сравнивает текст исходников; например shader guard проверяет наличие `deleteShaderGroup()`, но не доказывает выполнение этой ветви при logout (`src/test/java/thaumcraft/client/ClientFxEventLayerStaticGuardTest.java:18`). Глобальное CI-правило исключает `thaumcraft/client/**` при `CI=true`/`-Pci` (`build.gradle:154`), включая часть чисто статических client guards. Успех headless CI сам по себе не покрывает весь визуальный контракт.

## Следующий воспроизводимый этап

| Сценарий | Что требуется установить |
|---|---|
| Powered taint creeper | Точная missing texture, правильный namespace, восстановление armor layer |
| Potion shader → выход/другой мир | Очистка shader map/framebuffers, отсутствие остаточного эффекта |
| Активный shader + F3+T + resize | Перезагрузка shader assets, framebuffer lifetime, отсутствие silent retry loop |
| Узел/портал рядом с прозрачными объектами | Влияние blend state и render order |
| Массовые частицы, pause, dimension switch | Размер pending TC queue, время drain, стоимость frame rendering |
| Thaumonomicon + сложные addon items | Полнота ASPECTS item cache, отсутствие thread-unsafe callbacks |
| JEI отсутствует/присутствует, player/world switch | Optional loading и изоляция research visibility |
| JEI enchant/runic recipes | Установить желаемое расширение coverage отдельно от server crafting |
| Windows и Linux resource packaging | Каноническое имя bore texture в JAR |
| Все устройства/сущности, first/third person | Отдельная ручная визуальная сверка с TC4; отсутствие заявления о parity до evidence |

Автоматический client smoke не предлагался как routine check: `AGENTS.md` явно отделяет ручную игровую визуальную проверку от build/tests. При последующих исправлениях клиентского кода нужны focused tests и финальный `./scripts/dev.sh build`; изменения proxy/classloading потребуют dedicated-server smoke по правилам проекта.

## Выполненные команды и ограничения

- `git status --short`: на старте раздела чистый worktree; в конце появляются только совместно создаваемые `docs/analysis/` (scratch `.tmp` игнорируется).
- `Get-Content AGENTS.md`, `Get-Content build.gradle`, `Get-Content Dockerfile`: прочитаны.
- `rg --files`, `rg -n` по client/common/integration/tests: инвентаризация и целевой статический разбор.
- `git ls-files src/main/resources`, `git ls-tree -r HEAD ...`, `git rev-parse HEAD:<path>`: проверка имён и происхождения assets, идентичности Bore/bore.
- `& ./.tmp/client-assets-audit.ps1 > .tmp/client-assets-audit.txt`: синтаксис JSON, явные локальные ссылки models/blockstates/ResourceLocation/sounds; результат — 690 прочитанных JSON, 1 отсутствующая literal texture, 0 отсутствующих проверенных model/blockstate/sound targets.
- Read-only `System.IO.Compression.ZipFile` для оригинального JAR: просмотр строк constant pool двух client-классов; декомпиляция поведения не выполнялась.
- Build/JUnit/runtime/server/client: **не запускались в этом разделе**, потому что задача — анализ без production-изменений; документационные правки не требуют runtime smoke. Результаты общего build/environment анализа следует читать в отдельном отчёте команды.

Коммит не создавался. Исходная ревизия, production-файлы, API и assets сохранены. Отчёт описывает начальную системную проверку, а не сертификацию полного функционального и визуального соответствия.
