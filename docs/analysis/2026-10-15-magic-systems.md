# Анализ магических систем Thaumcraft 4 Reload

Снимок: `4bc68454b1063cdc1255f833dd537e0fd4fc341d`. Дата: 15.10.2026. Режим: статический обзор исходников и выборочное сравнение с декомпилированным оригиналом TC4 4.2.3.5. Производственный код не изменялся, сборка и игровой запуск в этой части аудита не выполнялись.

## Объём и метод

Прочитаны `AGENTS.md`, `build.gradle`, `Dockerfile`; исходное `git status --short` было пустым. `thaumcraft_src` содержит ресурсы и метаданные, но не Java-исходники. Поведение оригинала сверялось по CFR-декомпиляции исходного `Thaumcraft-1.7.10-4.2.3.5.jar`, расположенной вне репозитория: `../work/thaumcraft-4.2.3.5-oracle/`. Далее пути с префиксом `oracle/` относятся к этой папке; номера строк относятся к текущей декомпиляции. Обфусцированные методы Minecraft учитывались как методы среды, а не как изменение поведения мода.

Инвентаризация и просмотр точек входа охватывают весь перечисленный ниже набор; глубокий анализ выполнен для основных переходов сканирования, создания/завершения заметок, стола исследований, получения аспектов, arcane-крафта, тигля, тауматория, инфузии, затрат жезла, дренирования узлов и улучшения фокусов. Это не построчная семантическая проверка всех рецептов, всех эффектов фокусов или полный сертификат parity.

| Набор | Java-файлов | Строк | Уровень проверки |
|---|---:|---:|---|
| `common/lib/research` | 4 | 1 523 | Глубоко: ScanManager, gameplay-часть ResearchManager; PlayerKnowledge и сохранение переданы сетевому обзору |
| `common/lib/crafting` | 5 | 1 080 | Глубоко: получение тегов и матчеры; выборочно: динамические рецепты |
| `common/items/wands` с foci | 17 | 4 180 | Глубоко: жезл и ключевые lifecycle-ветки; выборочно: WandManager и фокусы |
| `common/config/research` | 7 | 4 295 | Инвентаризация регистраций, графа и связей с рецептами; без полного пересчёта каждой записи |
| `common/config/recipes` | 7 | 2 678 | Инвентаризация групп рецептов и проверка формы infusion-выходов |
| `common/world/aura` | 2 | 80 | Полный просмотр TC6-фасада; он отделён от native TC4 узлов |

Дополнительно просмотрены `ConfigAspects`, `ConfigRecipes`, lifecycle в `Thaumcraft`, контейнеры исследования/arcane-крафта, `ItemResearchNotes`, `ItemThaumometer`, `WarpEvents`, `TileResearchTable`, `TileInfusionMatrix`, `TilePedestal`, `TileCrucible`, `TileThaumatorium`, `TileAlchemyFurnace`, `TileFocalManipulator`, `TileNode`, `TileNodeEnergized`, `TileNodeConverter`, `TileWandPedestal` и соответствующие тестовые контракты. Пакетная авторизация, player capability, сохранение knowledge, API/TC6-совместимость и большинство устройств транспорта имеют отдельные отчёты; здесь зафиксированы пересечения, необходимые для понимания механики.

## Подтверждённые замечания

### MAG-01 — P2: инфузия теряет количество выходных предметов

**Условие:** рецепт инфузии возвращает `ItemStack` с количеством больше единицы, например результат аддона `4 × diamond`. В просмотренных штатных infusion-регистрациях выходы имеют количество 1; проблема не означает, что каждый штатный крафт сейчас теряет предметы.

**Доказательство:**

- `src/main/java/thaumcraft/common/tiles/TileInfusionMatrix.java:366-367`: результат копируется и записывается через `setInventorySlotContentsFromInfusion`.
- `src/main/java/thaumcraft/common/tiles/TilePedestal.java:79-80`: специальный infusion-setter делегирует обычному setter.
- `src/main/java/thaumcraft/common/tiles/TilePedestal.java:69-75,84`: обычный setter обрезает количество до `getInventoryStackLimit()`, равного 1.
- `oracle/thaumcraft/common/tiles/TilePedestal.java:83-99`: обычный setter оригинала ограничивает количество, но специальный setter инфузии отдельно присваивает стек **без этого ограничения**.

**Результат:** матрица расходует essentia и компоненты, но на центральном пьедестале остаётся только один предмет. Это реальная регрессия TC4-контракта специального setter, даже если встроенный набор рецептов её не проявляет.

**Проверка для исправления:** вызвать специальный setter с `new ItemStack(Items.DIAMOND, 4)`, получить из слота количество 4; отдельно убедиться, что обычная установка стека на пьедестал по-прежнему ограничена одним предметом. Затем выполнить infusion-рецепт с таким выходом и проверить количество после сохранения/перезагрузки.

**Уверенность:** высокая для потери количества по коду и отличия от оригинала; игровое воспроизведение не проводилось. `TileInfusionMatrixArtificeParityTest.java:79-111` проверяет NBT состояния с одноштучным выходом; найденные вызовы специального setter в `TileWandPedestalRuntimeTest` тоже передают по одному алмазу. Нужного контракта для многоштучного результата не найдено.

### MAG-02 — P2, условный inherited-дефект: ранний выход тигля позволяет сохранить уже использованные катализаторы

**Условие:** внешний рецепт тигля использует катализатор без собственных аспектов; бросается стек из 3 предметов; essentia хватает только на первый результат. Исследование рецепта у бросившего игрока завершено, тигель горячий и содержит воду. Это воспроизводимый сценарий для тестового/addon-рецепта; наличие конкретного штатного катализатора без аспектов этим обзором не доказано.

**Доказательство:**

- `src/main/java/thaumcraft/common/tiles/TileCrucible.java:392-411`: после успешного рецепта расходуются essentia/вода и создаётся output; уменьшается локальный `stacksize`, но не входной `ItemStack`.
- `src/main/java/thaumcraft/common/tiles/TileCrucible.java:415-425`: следующий проход без подходящего рецепта и без аспектов катализатора выполняет `return`.
- Корректировка количества entity или его уничтожение находятся только в `src/main/java/thaumcraft/common/tiles/TileCrucible.java:447-453`, которые ранний выход обходит.
- `oracle/thaumcraft/common/tiles/TileCrucible.java:248-300` содержит тот же порядок действий и ранний выход. Это **ошибка оригинала, сохранённая портом**, а не доказательство ухудшения parity.

**Сценарий:** после первой итерации локальный остаток равен 2. Во второй итерации `a == 1 < 2`; essentia уже нет, fallback обнаруживает пустые теги и выходит. Исходный entity по-прежнему содержит 3 катализатора, а выход первого крафта уже существует. Так нарушается атомарность потребления входа и выдачи результата.

**Направление исправления:** фиксация уже использованного количества перед любым выходом либо уменьшение входа в момент подтверждённого потребления; тест должен учитывать несколько исходных катализаторов, истощение essentia внутри партии и rejection-path. `TileCrucibleSmeltContractStaticGuardTest.java:16-30` проверяет наличие строк, включая ранний `return`, но не проверяет сохранение количества входа после частичного успеха.

**Уверенность:** высокая для указанного control-flow-сценария; средняя для практического влияния на конкретный установленный аддон. Без зарегистрированного такого рецепта проблему нельзя называть подтверждённым эксплойтом стандартного модпака.

### MAG-03 — P3: ветка прямого сканирования с префиксом `#` всегда урезает базовую награду

**Условие:** внешний код напрямую вызывает `ScanManager.completeScan(player, scan, "#")` для ещё не просканированного объекта.

**Доказательство:**

- `src/main/java/thaumcraft/common/lib/research/ScanManager.java:191-203` записывает отметку `#` до расчёта награды.
- `src/main/java/thaumcraft/common/lib/research/ScanManager.java:216` затем вычисляет fallback через `!isValidScanTarget(..., "@")`.
- `src/main/java/thaumcraft/common/lib/research/ScanManager.java:144` проверяет `@` рекурсивно через незавершённость `#`; отметка уже записана, поэтому проверка всегда ложна.
- `src/main/java/thaumcraft/common/lib/research/ScanManager.java:220-222` всегда заменяет исходное количество аспектов на 0, а затем добавляет 1.
- `oracle/thaumcraft/common/lib/research/ScanManager.java:351-355` вычисляет этот флаг **до** изменения состояния; записывает скан позже, например на строке 369.

**Проверка:** свежий игрок знает primal-аспекты; предмет содержит `EARTH × 3`. Первый прямой `#` scan должен дать 4 при низком aspect pool, как оригинал; порт даёт 1 и отмечает объект просканированным. Если предмет ранее сканировали через `@`, добавочная награда 1 является ожидаемой; эти два случая требуется проверять отдельно.

**Ограничение влияния:** штатный `ItemThaumometer.java:90-92` использует `@`, как и оригинал. `PacketScannedToServer.java:103-104` принимает только нормализованный `@`; подмена пакета на `#` запрещена. Поэтому это не дефект обычного сканирования и не доступный через текущий пакет способ получить больше очков. Обнаруженное отличие относится к прямому compatibility/addon-вызову. `ScanProgressionRuntimeTest` проверяет количество награды для `@`, но не для `#`.

**Уверенность:** высокая для прямого вызова; низкая значимость без аддона, который использует эту ветку.

### MAG-04 — P2, условный inherited-дефект: высокая complexity аддона может зациклить генерацию заметки

**Условие:** аддон создаёт research через поддерживаемый builder и вызывает `.setComplexity(40)`; имеется хотя бы один research-aspect. Встроенные регистрации конструктором ограничивают complexity значениями 1–3; вызовов `setComplexity` в проверенных `common/config/research` не найдено.

**Доказательство:**

- `src/main/java/thaumcraft/api/research/ResearchItem.java:66-71,84-89`: конструкторы ограничивают complexity.
- `src/main/java/thaumcraft/api/research/ResearchItem.java:252-254`: setter записывает значение без ограничения.
- `src/main/java/thaumcraft/common/lib/research/ResearchManager.java:625,642-676`: radius ограничен 4, но количество удаляемых hexes равно `complexity * 2`; у `while (blanks > 0)` нет лимита попыток/проверки достаточности удаляемых клеток.
- Hex-grid radius 4 содержит 61 клетку. При complexity 40 требуется удалить 80 клеток, при этом фиксированные клетки `type == 1` не удаляются. После исчерпания допустимых клеток цикл продолжает выбирать неподходящие клетки и не уменьшает `blanks`.
- `oracle/thaumcraft/api/research/ResearchItem.java:239-241` и `oracle/thaumcraft/common/lib/research/ResearchManager.java:281-322` содержат тот же недостаток.

**Результат:** вызов создания заметки на серверном потоке способен зависнуть; это также inherited-робастность, а не нарушение port-parity. Нельзя распространять вывод на штатные исследования с complexity ≤ 3.

**Проверка для исправления:** параметризованный bounded-time тест генерации с complexity 1–3 и с большим значением из setter; сохранение штатного алгоритма при нормальных параметрах, явный отказ/ограничение при невозможной конфигурации. Уверенность высокая по конечному числу hexes и отсутствию условия завершения; процесс игрового сервера не запускался и намеренно не блокировался.

## Как связаны механики

### Регистрация и исследовательская прогрессия

`Thaumcraft.java:106-157` регистрирует capability, scan-handler, сетевые/мировые события, блоки/предметы/tiles. Категории создаются в init (`188`), рецепты и аспекты — в postInit (`209-212`), затем research получает ссылки на реальные рецепты. `ConfigRecipes.java:80-93` делит инициализацию на arcane, infusion, enchantment, crucible и smelting, а `ConfigResearch.recipes` связывает страницы таумономикона с рецептами.

`ScanManager.getScanAspects` получает object-tags плюс bonus-tags, entity-tags или теги явления/узла (`308-327`). `validScan` требует знания родительских аспектов (`265-275`). Завершение отмечает scan, открывает новые аспекты, пополняет pool, выдаёт hidden/lost clue через `ResearchManager.createClue` и синхронизирует состояние (`181-234`). Primal discovery, cap и diminishing return расположены в `checkAndSyncAspectKnowledge` (`237-262`). Нельзя считать пустой `scanPhenomena(ItemStack,World,EntityPlayer)` на `49-50` незавершённой механикой: это pass-through для последовательности обработчиков, что явно проверяет `ScanProgressionRuntimeTest.builtInPhenomenaHandlerPassesThroughWithoutStarvingLaterHandlers`.

`ResearchManager.java:357-387` проверяет одновременно обычных и скрытых родителей; `389-416` создаёт заметку за бумагу и чернила, переиспользуя уже имеющуюся заметку того же key. Генерация hex-grid и NBT — `611-756`; завершение проверяет связность фиксированных hexes через отношения parent/compound и открытые игроком аспекты (`759-825`). Стол не разрешает менять fixed-hex, недоступный hex или расходовать неизвестный/отсутствующий аспект (`TileResearchTable.java:293-347`), поддерживает бонусные аспекты, возврат исследователю и затраты чернил (`348-372`). Дублирование завершённых notes — отдельная, платная механика (`236-283`), а не автоматически «dupe».

Использование готовой заметки проверяет prerequisites, добавляет research и доступные siblings, затем уменьшает стек (`ItemResearchNotes.java:127-148`). Неизвестное открытие либо становится hidden research, либо распадается на фрагменты (`151-171`). Сохранение/кэш knowledge и сетевую авторизацию следует читать в отдельном отчёте; этот обзор не подтверждает их полную корректность.

### Аспекты и изготовление

`ThaumcraftCraftingManager.java:46-116` объединяет зарегистрированные/генерируемые аспекты с тегами жезлов, зелий, содержимого essentia, инструментов и enchantments. Отдельные object-tags не равны итоговым smelting/scan-тегам: потребители добавляют bonus-tags. Поиск meta/group/wildcard — `123-154`, ограничение рекурсии и регистрация сгенерированных тегов — `157-174`. `ConfigAspects` содержит vanilla, utility/mechanism, ore-dictionary, alchemy-baseline и entity-секции; здесь не доказано полное числовое равенство каждой регистрации оригиналу.

Arcane-верстак сначала пробует vanilla-рецепт и правило `doLimitedCrafting`, затем arcane-рецепт и vis-стоимость (`ArcaneWorkbenchRecipeResolver.java:19-58`). Перед фактической выдачей output контейнер заново разрешает текущий рецепт, сравнивает ожидаемый output, вычисляет container-item remainders и расходует vis (`ContainerArcaneWorkbench.java:181-215`); затем уменьшает ингредиенты и возвращает остатки в слоты/инвентарь/мир (`218-240`). Это существенно сильнее простой проверки preview. Ручной и shift-click расход требуют runtime-проверки всей транзакции, особенно совместного использования верстака; такой эксперимент здесь не выполнялся.

Тигель проверяет нагрев/наличие воды, разлагает остатки и выпускает flux (`TileCrucible.java:124-196`), выбирает доступный исследованный рецепт с наибольшим количеством типов аспектов (`ThaumcraftCraftingManager.java:493-521`), иначе разлагает предмет (`TileCrucible.java:380-454`). Вода расходуется по 50 mB при рецепте. Fluid capability привязан к water-only tank с dirty/sync callback (`53-68,107-118`). Сам факт присутствия capability не означает доказанный полный round-trip save-test.

Тауматорий выбирает запрограммированный crucible-рецепт по catalyst, проверяет возможность вывода в инвентарь, накапливает essentia и расходует один catalyst (`TileThaumatorium.java:89-191`). Список рецептов сохраняется хешами, восстанавливается через registry (`218-244`). Анализ addon recipe-hash stability и контейнерной авторизации вне этой части отчёта.

Инфузия включает проверку структуры, снимок input/components, разрешение обычного или enchantment-рецепта, копию оставшейся стоимости и игрока (`TileInfusionMatrix.java:109-116,208-273`). Цикл сначала проверяет неизменность центрального input, обрабатывает instability, XP, essentia, компоненты и лишь затем завершает output (`276-395`). Проверка input — `440-447`, XP drains — `450-479`, компоненты заменяются container-item (`493-501`). Состояние активного крафта сохраняется (`120-189`). Симметрия ищет загруженные колонки и зеркальные пары (`687-739`); missing-input остановка очищает recipe state (`665-684`). Требуется игровой тест unload/reload посреди каждой фазы: наличие NBT-сериализации само по себе не доказывает корректное возобновление.

### Жезлы, узлы, фокусы и warp

Жезл хранит vis в сотых единицы, а capacity rod — в целых; crafting-cost умножается на 100 (`ItemWandCasting.java:110-184,236-242`). Модификатор учитывает cap, armor/baubles-discount, frugal, sceptre и нижнюю границу 0.1 (`191-206`). `consumeAllVis` сначала проверяет все аспекты и только потом расходует их, причём сервер авторитетен при `doit` (`212-233`). Поэтому один успешный lookup не является доказательством потребления всех ресурсов; важен именно этот двухфазный участок.

Взаимодействие с IWandable/trigger и фокусами — `ItemWandCasting.java:594-697`; удержание узла/фокуса и выбор настоящей руки — `699-731`. В просмотренных Fire/Shock/Excavation ветках vis и действия над миром разделены по client/server, continuous-use прекращается при нехватке vis. Полный эффект каждой upgrade-комбинации, включая Frost/Pech/Primal/PortableHole, не сертифицирован.

`TileNode.java:246-275` связывает активную руку с узлом и регулярно проверяет луч; `386-438` учитывает NODETAPPER/NODEPRESERVE и снимает vis с источника. Native TC4 aura — это эти узлы, их recharge, hungry/dark/pure/tainted и стабилизация (`285-325`), а не TC6 chunk-aura. Energized-node пересчитывает primal-проекцию, модификаторы и доступный за тик поток (`TileNodeEnergized.java:29-68,134-144`); VisNet и relay являются отдельным public API-механизмом.

Focal manipulator проверяет допустимый upgrade и rank, XP и research-gate, затем получает primal-vis из VisNet (`TileFocalManipulator.java:106-171`). XP списывается при старте, vis — постепенно; извлечение focus отменяет оставшуюся стоимость (`215-246`). Это требует тестов отмены/автоматизации и restart, но само по себе не является найденной ошибкой: аналогичные cancellation-переходы присутствуют в оригинале.

Warp использует permanent/sticky/temp, counter и gear contribution, уменьшает counter и применяет события с пороговыми открытиями BATHSALTS/ELDRITCH (`WarpEvents.java:50-173`). `checkDeathGaze` проверяет видимость и PvP (`180-205`). Эти вероятностные ветки выборочно сверены с original `WarpEvents`; временной lifecycle вызова и capability/NBT принадлежат отдельному отчёту. Полная статистическая parity распределений событий здесь не проверена.

TC6 `common/world/aura/AuraHandler.java:15-24` явно бросает `UnsupportedOperationException` для chunk-ауры/flux; `drainVis` (`27-34`) проецируется в TC4 VisNet и округляет запрос вниз. Это сознательно ограниченный compatibility-фасад, который способен не удовлетворить конкретный аддон; его нельзя описывать как «в TC4 вовсе отсутствует работающая аура».

## Тестовое покрытие и следующий этап

В репозитории имеются и поведенческие тесты с fake-world, и static guards, проверяющие только текст исходника. Важные примеры: `ScanProgressionRuntimeTest`, `ScanManagerValidScanRuntimeTest`, `ResearchClueAndNotesRuntimeTest`, `TileResearchTableRuntimeContractTest`, `TileResearchTablePersistenceRuntimeTest`, `TileInfusionMatrixArtificeParityTest`, `ItemWandCastingNbtSubstrateRuntimeTest`, `TileNodeWandDrainRuntimeTest`, `TileWandPedestalRuntimeTest`, `FocusHellbatUpgradeGateRuntimeTest`, `ThaumcraftCraftingManagerArcaneIngredientBehaviorTest`. Они не равнозначны успешному игровому тесту; в рамках этого read-only обзора тесты не запускались.

Нужные сценарии после согласованного исправления, в порядке полезности:

1. Multi-output infusion: специальный setter, полный цикл, save/reload и извлечение результата.
2. Тигель: частичная партия плюс отказ fallback с нетегированным catalyst; итоговый баланс входов/выходов и essentia.
3. Создание заметки с невозможным количеством удаляемых hexes через addon setter; жёсткое время завершения.
4. Прямой `#` scan до/после `@` с проверкой награды; сохранить штатное поведение `@` и запрет `#` в пакете.
5. Полные игровые цепочки: scan → discovered parents → clue → note → research → recipe; arcane shift-click; infusion reload на этапах XP/essentia/компонентов; focal manipulator отмена и reload; node drain/preserve/recharge.

Командная проверка выполнялась чтением файлов, `rg --files`, `rg -n`, `Get-Content`, подсчётом файлов/строк и `git status --short`. Исходный статус был чистым. Документационный diff не требует smoke по `AGENTS.md`; для будущих правок перечисленных common-систем требуется `./scripts/dev.sh validate --smoke`, затем `./scripts/dev.sh build`. Никакого утверждения об успешном compile/runtime или полной parity этот отчёт не делает.

## Полный реестр пакетного scope

В `common/lib/research`: `PlayerKnowledge`, `ResearchManager`, `ResearchNoteData`, `ScanManager`.

В `common/lib/crafting`: `ArcaneSceptreRecipe`, `ArcaneWandRecipe`, `InfusionRunicAugmentRecipe`, `ShapelessNBTOreRecipe`, `ThaumcraftCraftingManager`.

В `common/items/wands`: `ItemFocusPouch`, `ItemFocusPouchBauble`, `ItemWandCap`, `ItemWandCasting`, `ItemWandRod`, `WandManager`, `WandRodPrimalOnUpdate`; в `foci`: `FocusExcavation`, `FocusFire`, `FocusFrost`, `FocusHellbat`, `FocusPech`, `FocusPortableHole`, `FocusPrimal`, `FocusShock`, `FocusTrade`, `FocusWarding`.

В `common/config/research`: `ConfigResearch`, `ConfigResearchAlchemy`, `ConfigResearchArtifice`, `ConfigResearchBasics`, `ConfigResearchEldritch`, `ConfigResearchGolemancy`, `ConfigResearchThaumaturgy`.

В `common/config/recipes`: `ConfigRecipesArcaneSlice`, `ConfigRecipesCrucibleSlice`, `ConfigRecipesInfusionDeviceSlice`, `ConfigRecipesInfusionEquipmentSlice`, `ConfigRecipesInfusionSlice`, `ConfigRecipesSmeltingSlice`, `ConfigRecipesSpecialSlice`.

В `common/world/aura`: `AuraChunk`, `AuraHandler`.

Сопряжённые devices дополнительно инвентаризованы: `TileAlchemyFurnaceAdvanced`, `TileAlchemyFurnaceAdvancedNozzle`, `TileAlembic`, `TileArcaneFurnace`, `TileArcaneFurnaceNozzle`, `TileArcaneWorkbench`, `TileCentrifuge`, `TileDeconstructionTable`, `TileEssentiaCrystalizer`, `TileEssentiaReservoir`, `TileFluxScrubber`, `TileInfusionPillar`, `TileJarFillable`, `TileJarFillableVoid`, `TileJarNode`, `TileMagicWorkbench`, `TileMagicWorkbenchCharger`, `TileMirrorEssentia`, `TileNodeStabilizer`, `TileThaumatoriumTop`, `TileVisRelay`, трубки и `EssentiaHandler`; основной обзор большинства из них находится в отчёте устройств/API. Их наличие в реестре не означает построчную проверку каждым автором.
