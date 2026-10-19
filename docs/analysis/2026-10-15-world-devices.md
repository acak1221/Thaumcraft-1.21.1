# Мир, измерения, сущности, големы и устройства: анализ кода

Срез: `4bc68454b1063cdc1255f833dd537e0fd4fc341d`. Дата анализа: 15.10.2026. Анализ статический; производственный код, зависимости и игровые данные не менялись.

## Границы и источники

Прочитаны `AGENTS.md`, `build.gradle`, `Dockerfile`. Сопоставление критичных найденных путей выполнено с CFR 0.152-декомпиляцией оригинального `Thaumcraft-1.7.10-4.2.3.5.jar`; рабочий oracle расположен **вне репозитория**: `../work/thaumcraft-4.2.3.5-oracle/`. Далее `oracle:` обозначает пути внутри него. `thaumcraft_src/` в этом checkout содержит ресурсы, а не Java-исходники. Oracle использован только для чтения, его код не включён в отчёт или изменения.

Проверяются только свежие миры и NBT, созданные данным портом Forge 1.12.2. Миграция сохранений 1.7.10 не рассматривалась. Aura, infusion, crucible и wands рассмотрены в отдельном разделе анализа; здесь они упоминаются только как зависимости устройств.

Обнаружено 39 Java-файлов в `common/lib/world`, 138 в `common/entities`, 76 в `common/tiles`. Это **инвентаризация, а не заявление о полном построчном аудите всех 253 файлов**. Подробно прослежены следующие группы:

| Область | Основные просмотренные файлы | Глубина |
|---|---|---|
| Жизненный цикл мира | `EventHandlerWorld`, `ThaumcraftWorldGenerator` | Загрузка/сохранение labyrinth, генерация и retrogen, распределение по измерениям, входы структур/растительности/руд |
| Outer Lands | `WorldProviderOuter`, `ChunkProviderOuter`, `TeleporterThaumcraft`, `MazeHandler`, `MazeThread`, `MazeGenerator`, `GenPortal`, `TileEldritchAltar`, `TileEldritchPortal` | Вход/выход, выбор точки прибытия, генерация пустых чанков, размещение портала, резервирование ячеек, сохранение карты |
| Перенос essentia | `TileTube`, `TileTubeBuffer`, `TileTubeFilter`, `TileTubeOneway`, `TileTubeValve`, `TileTubeRestrict`, `TileJarFillable`, `TileAlembic`, `TileCentrifuge`, `TileEssentiaReservoir`, `TileMirrorEssentia`, `EssentiaHandler`, `ThaumcraftApiHelper` | Suction, направление, перенос одной единицы, кэш обнаружения источников и границы чанков |
| Предметные устройства | `TileMirror`, `BlockMirror`, `TileChestHungry`, `TileGrate`, `TileArcaneBore`, `TileArcaneBoreBase`, `TileArcaneFurnace`, `TileEssentiaCrystalizer`, `TileSpa` | Потребление/выдача, очереди и NBT, взаимодействие с инвентарями, FakePlayer/BreakEvent, fluid capability |
| Тикающие устройства | `TileFluxScrubber`, `TileArcaneLamp`, `TileArcaneLampGrowth`, `TileEldritchTrap` | Стоимость сканирования, loaded-chunk guards, частота действий |
| Големы и AI переноса | `InventoryMob`, `InventoryTrunk`, `InventoryPech`, `InventoryUtils`, inventory/fluid AI, связанный `GolemHelper`, NBT/переносы trunk/Pech | `ItemStack.EMPTY`, фильтры, извлечение/вставка, сохранение типа и объёма жидкости, двойные сундуки |
| Прочие сущности | `EntityPech`, `ContainerPech`, `AIPechTradePlayer`, `EntityThaumcraftBoss`, `EntityCultistPortal`, `EntityCultistPortalGreater` | Торговля/NBT подробно; базовые boss lifecycle и совместимые alias-классы выборочно |

Большинство monster AI, projectile collision paths, мировых растений/деревьев и все разновидности големных действий не прошли индивидуальную сверку с TC4. Проверка всех взаимодействий с модами не выполнена. Наличие файла в карте проекта не означает подтверждённый behavioral parity.

## Карта выполнения

1. `ThaumcraftWorldGenerator.worldGeneration()` (`src/main/java/thaumcraft/common/lib/world/ThaumcraftWorldGenerator.java:342`) выбирает Outer Lands / Nether / surface. Outer Lands передаёт генерацию комнате из `MazeHandler`; `ChunkProviderOuter.generateChunk()` начинает с пустого `ChunkPrimer`.
2. Eldritch ring или активация алтаря создаёт карту ячеек через **синхронный** `new MazeThread(...).run()` (`ThaumcraftWorldGenerator.java:616`, `TileEldritchAltar.java:131`). Несмотря на имя, это не background thread; запись блоков из асинхронного потока здесь не обнаружена.
3. `TileEldritchPortal` каждые пять тиков ищет игроков, использует cooldown `timeUntilPortal` и вызывает `transferPlayerToDimension()` (`TileEldritchPortal.java:37–73`). `TeleporterThaumcraft` подгружает небольшой квадрат чанков, затем ищет портал, выбирает свободный проход либо fallback.
4. Трубы получают suction от соседей; пустая труба запрашивает одну единицу через `takeEssentia()` и `addEssentia()`. Буфер вмещает восемь единиц и учитывает конкурирующее suction соседей. Беспроводное извлечение использует кэш `EssentiaHandler` по координатам и dimension.
5. Предметное зеркало сначала переносит stack в очередь назначения и убивает исходный `EntityItem`. Назначение выдаёт по одной единице. Очередь порта записывается в собственный NBT `Items`; это положительное отличие от оригинала, где `outputStacks` в просмотренном `writeCustomNBT()` не сохранялась.
6. Arcane bore выполняет `BreakEvent` с FakePlayer перед добычей, собирает drops и помещает их в соседний `IInventory` либо выдаёт `EntityItem`. Golem fluid core выполняет capability-drain, переносит один `FluidStack`, затем заполняет домашний capability-handler.

## Находки

Приоритет P2 означает функциональный дефект или ограниченный риск целостности/ресурсов. Все выводы ниже подтверждены чтением указанных ветвей; запуск в Minecraft не выполнен. «Унаследовано» означает присутствие того же дефекта в оригинале TC4, а не ошибку именно переноса. По правилам проекта исправление такого поведения нужно обосновать воспроизводимым дефектом оригинала или игровым инвариантом.

### WD-01 — P2 — Торговая AI запрещена именно приручённому Pech

**Доказательства:** `src/main/java/thaumcraft/common/entities/ai/pech/AIPechTradePlayer.java:19`; регистрация задачи `EntityPech.java:102`; открытие торговли `ContainerPech.java:44`; проверка расстояния `ContainerPech.java:70–71`.

`shouldExecute()` возвращает `false`, когда `pech.isTamed()` истинно. Штатный интерфейс торговли требует приручённого Pech; контейнер ставит `trading=true`, но задача с mutex bits `5` не стартует и её `startExecuting()` не очищает navigation path. Wander/movement tasks могут продолжать двигать Pech во время открытого GUI. Если он удалится более чем на восемь блоков, контейнер перестаёт удовлетворять `canInteractWith()`.

**Триггер:** приручённый Pech стоит на земле вне воды, имеет активный путь; игрок открывает торговлю. `isEntityAlive=true`, `isInWater=false`, `isTamed=true` — третье условие немедленно отвергает задачу независимо от `trading=true`.

**Сверка:** `oracle:thaumcraft/common/entities/ai/pech/AIPechTradePlayer.java:28` проверяет `!isTamed()`. Это подтверждённая **регрессия** порта.

**Проверка для исправления:** воспроизвести `shouldExecute()` для живого приручённого Pech с `trading=true`, затем проверить старт задачи и очистку пути; дополнительно открыть GUI в игре и убедиться, что AI не уводит торговца. Одна проверка присутствия строки в source не доказывает работу scheduler.

### WD-02 — P2 — В свежий NBT Pech сохраняется состояние открытого GUI

**Доказательства:** `src/main/java/thaumcraft/common/entities/monster/EntityPech.java:171,192,501,516`; сброс нормальным закрытием `ContainerPech.java:216–219`; сброс задачей `AIPechTradePlayer.java:31–32`.

Порт записывает и восстанавливает `trading`. Это состояние текущей сессии контейнера, которой после загрузки уже нет. Снимок сущности, сделанный во время открытой торговли, содержит `trading=true`. При загрузке приручённого Pech `canPickup()` и `pickupItem()` отказывают в подборе предметов. WD-01 дополнительно препятствует старту/сбросу торговой AI для приручённой сущности, поэтому флаг не очищается этим путём. Открыть и закрыть GUI вручную можно; это обход, а не корректная загрузка.

**Триггер:** NBT записан во время торговли (`trading=true`), затем сущность загружена в новой серверной сессии. Здесь рассматривается только NBT, созданный портом 1.12.2, а не legacy migration.

**Сверка:** оригинальный `EntityPech` записывает PechType, Anger, Tamed и Loot (`oracle:.../EntityPech.java:314–341`), но не `trading`. Это отдельная **регрессия** состояния сохранения.

**Проверка для исправления:** round-trip свежего NBT Pech, записанного при открытом контейнере; после загрузки не должно остаться активного состояния торговли без контейнера. Проверить доступность подбора ценных предметов после restart.

### WD-03 — P2 — Spa перестала размещать лаву в Nether

**Доказательства:** `src/main/java/thaumcraft/common/tiles/TileSpa.java:211–238,255–258`.

В `isValidLocation()` запрещён любой `FluidRegistry.WATER` **или `FluidRegistry.LAVA`**, если provider сообщает `doesWaterVaporize()`. В Nether это свойство относится к испарению воды. Следовательно, `mix=false`, lava tank >=1000 mB, свободное replaceable пространство и сплошная опора всё равно дают `false`: Spa никогда не размещает лаву в Nether. Ресурсы не расходуются, но штатная функция перестаёт работать.

**Сверка:** оригинальный `TileSpa.isValidLocation()` (`oracle:thaumcraft/common/tiles/TileSpa.java:255`) ограничивает только `Blocks.field_150355_j`/`field_150358_i` — flowing_water/water. Lava в этот запрет не входит. Это **регрессия** порта, а не обоснованное изменение Nether mechanics.

**Проверка для исправления:** Spa `mix=false` с lava tank1000, Nether provider, air над solid top. После одного допустимого рабочего цикла должен появиться lava source, tank должен уменьшиться на1000. Для воды в Nether должен сохраняться отказ. Проверка нужна также для pure-fluid режима: нельзя расширить исправление на все жидкости механически.

### WD-04 — P2 — Зеркало удаляет выход из очереди, даже когда entity spawn отклонён

**Доказательства:** `src/main/java/thaumcraft/common/tiles/TileMirror.java:218–225,244–245`.

`spawnItem()` вызывает `world.spawnEntity(ie2)`, игнорирует возвращаемый boolean и возвращает `true`, если не было exception. `eject()` после этого безусловно уменьшает stack в `outputStacks`. Forge `EntityJoinWorldEvent` может отменить spawn без исключения; тогда предмет не появляется, а очередь уже уменьшена и помечена dirty. Путь не защищён наличием сохранения очереди.

**Триггер:** очередь назначения содержит предмет; обработчик Forge/другой мод отменяет создание этого `EntityItem`. Возврат `spawnEntity=false` трактуется как успех. Это ограниченный integration-trigger, а не заявление о постоянной потере предметов в стандартном одиночном мире.

**Сверка:** оригинальный `TileMirror.spawnItem()` также игнорирует `func_72838_d()` и возвращает `true`. Это **унаследованный дефект**, нарушающий инвариант «выдача подтверждена до списания».

**Проверка для исправления:** fake/test world возвращает `false` из spawn: queue size/count и NBT до/после `eject()` совпадают; при успешном spawn уменьшается ровно одна единица. Затем integration check с отменой `EntityJoinWorldEvent`. Не использовать общий `try/catch` как замену проверки результата.

Аналогичный риск намеренно проверять отдельно у `TileArcaneFurnace.ejectItem()`, `TileEssentiaCrystalizer.spawnItem()` и `TileGrate.setInventorySlotContents()`: здесь также есть side effect без подтверждения spawn. Для этих устройств отчёт не заявляет отдельно проверенную rollback-семантику.

### WD-05 — P2 — Голем объединяет разные жидкости в один carried stack

**Доказательства:** `src/main/java/thaumcraft/common/entities/ai/fluid/AILiquidGather.java:121–141,196`; `src/main/java/thaumcraft/common/entities/golems/GolemHelper.java:780–811`.

`getMissingLiquids()` фильтрует по уже переносимой жидкости только во время создания списка. Для пустого голема домашний приёмник нескольких типов может вернуть WATER и LAVA. `updateTask()` получает список один раз и затем обходит оба типа. После первого drain он не пересчитывает список и не сверяет `drained.isFluidEqual(fluidCarried)`: при существующем stack увеличивает только `amount`.

**Статическая трасса:** WOOD-golem capacity2000, пустые fluid filters, домашний capability принимает два типа, возле голема помечены WATER500 и LAVA500. При обходе WATER первым: drain WATER500 создаёт carried(WATER,500); drain LAVA500 увеличивает carried до(WATER,1000). Lava source потерял500, water появилась из её объёма. Если порядок жидкости обратный, преобразование идёт в обратную сторону. Oversized drain дополнительно ограничивается свободным объёмом в порте, но это не сохраняет тип.

**Сверка:** оригинальные `AILiquidGather` и `GolemHelper.getMissingLiquids()` содержат тот же snapshot и amount-only merge. Это **унаследованный дефект** сохранения типа ресурса.

**Покрытие тестами:** `src/test/java/thaumcraft/common/entities/ai/fluid/LiquidEssentiaBackendRuntimeTest.java` содержит многожидкостный sink и отдельно discovery; тест gather capacity использует один тип WATER. Эти проверки не покрывают два успешных drain разных типов в одном `updateTask()`.

**Проверка для исправления:** тот же multi-fluid capability fixture с двумя частично заполненными источниками; сумма объёмов должна сохраняться **для каждого fluid**, а голем после gather переносит один тип. Проверить и handler-source, и world-block source, где выполняется аналогичный amount-only merge.

### WD-06 — P2 — Неподходящий первый сосед блокирует entropy-ускорение bore

**Доказательства:** `src/main/java/thaumcraft/common/tiles/TileArcaneBoreBase.java:26–40`; использование `drawEssentia()` для ускорения `TileArcaneBore.java:145–156`.

Метод перебирает шесть соседей, но при первом connectable transport с `canOutputTo(remote)=false` возвращает `false` из всего метода. Он не продолжает искать другой источник. Например, input-only crystalizer на более ранней стороне и entropy source на более поздней стороне приводят к пропуску исправного источника. Сущности/предметы не дублируются; bore теряет доступное essentia-ускорение в зависимости от расположения соседей.

**Сверка:** `oracle:thaumcraft/common/tiles/TileArcaneBoreBase.java:73–74` имеет тот же ранний return. Это **унаследованный дефект**, требующий отдельного обоснования при изменении поведения.

**Проверка для исправления:** два соседа: первый connectable input-only, второй допустимый output ENTROPY с suction ниже128. `drawEssentia()` должен проверить второй и списать ровно одну единицу. Повторить расположение по всем сторонам; отдельно проверить output-side orientation с suction0.

### WD-07 — P2 — WeakHashMap не позволяет освободить WorldServer

**Доказательства:** `src/main/java/thaumcraft/common/lib/world/dim/TeleporterThaumcraft.java:21–34,24–25`; World unload handler `src/main/java/thaumcraft/common/lib/events/EventHandlerWorld.java:99–107`.

`WORLD_TELEPORTERS` — статический `WeakHashMap<WorldServer, TeleporterThaumcraft>`. Map держит **сильную ссылку на значение**. Teleporter, переданный в `super(world)`, держит этот WorldServer в inherited поле `world`, которое используется самим классом. Получается цепочка strong references `static map -> value teleporter -> world key`. Слабость ключа не помогает, потому что ключ остаётся достижимым через значение. Явного удаления teleporter при world unload в просмотренном lifecycle нет.

**Триггер:** `forWorld()` вызван хотя бы раз для измерения, затем WorldServer выгружен или закрыт integrated world; пока classloader мода жив, map удерживает старый WorldServer. Это подтверждённая ошибка графа ссылок; **объём удержанной памяти и практический эффект на heap не измерялись**. Кэш по миру исправляет смешение portal positions между мирами, но его lifecycle недостаточен.

**Покрытие тестами:** `src/test/java/thaumcraft/common/lib/world/dim/OuterLandsArrivalAndSpawnStaticGuardTest.java:29–31` проверяет наличие строки `WeakHashMap`/`forWorld`. Такой guard подтверждает выбранную конструкцию и не проверяет освобождение мира.

**Проверка для исправления:** deterministic cleanup при `WorldEvent.Unload` и assert отсутствия записи после unload; затем integrated-world A -> B. Дополнительный heap/WeakReference probe полезен, но timeout-GC сам по себе ненадёжен. Сохранять per-world isolation portal cache.

### WD-08 — Производительность — Полный поиск портала и массивная очередь scrubber

**Teleporter:** `src/main/java/thaumcraft/common/lib/world/dim/TeleporterThaumcraft.java:20,77–92` делает `(2*128+1)^2 * actualHeight` чтений block state при cache miss. При height256 это **16 908 544** позиции. Поиск продолжается после найденного портала для поиска ближайшего. `prepareDestination()` с radius1/2 (`:40–46,102–110`) не ограничивает цикл radius128. Если портал не найден, второй вызов повторяет полный поиск — до33 817 088 запросов перед fallback. Без `isBlockLoaded()` затрагиваются удалённые чанки; требуется проверка реального chunk-load поведения и измерение server tick duration. Сам алгоритм radius128 присутствует и в TC4 oracle; это **унаследованная архитектурная стоимость**, а не новый доказанный freeze.

**Scrubber:** `src/main/java/thaumcraft/common/tiles/TileFluxScrubber.java:62–80` строит `ArrayList` из33³=35 937 координат и удаляет head через `remove(0)` до16 раз за тик. Пустой полный проход даёт `n*(n-1)/2 = 645 716 016` перемещений ссылок в backing array, помимо чтений мира. Начало прохода также создаёт35 937 объектов. Это доказанная O(n²)-стоимость структуры данных; TPS/GC конкретного modpack не измерены. Не нужно превращать её в недоказанный «краш сервера».

**Проверка следующего этапа:** profiler/Java Flight Recorder либо sampling profiler на cache-miss teleport и группе scrubbers; измерять время server thread, allocations и новые loaded chunks. Оптимизация должна сохранять ближайший портал и случайный порядок очистки. Перенос произвольной записи World в background thread недопустим.

### WD-09 — P2 — Без фильтров голем не смотрит вторую половину двойного сундука

**Доказательства:** `src/main/java/thaumcraft/common/entities/ai/inventory/AIHomeTake.java:64–84`.

В ветке без `neededList` после исчерпания первой половины сундука выполняется `return false` (`:80`), хотя ниже расположен fallback `InventoryUtils.getDoubleChest(tile)` (`:84`). Если голем с Empty/Use-core привязан к пустой половине и предметы находятся только во второй, перенос не начинается. В filtered-ветке fallback достижим.

**Сверка:** та же ветвь есть в оригинальном `AIHomeTake`. **Унаследованный дефект**, а не повод менять идентификаторы core или save format.

**Проверка для исправления:** double chest, первая половина пустая, вторая содержит предмет, у голема пустые фильтры. Проверить извлечение одной порции и сохранение суммарного item count. Повторить с filtered-режимом и blocked item timeout.

## Риски, которые требуют отдельного воспроизведения

- `EssentiaHandler.getSources()` проверяет `isBlockLoaded()` при начальном сканировании (`:104`), но cached-drain/find (`:38,:76`) идут прямо через `getTileEntity()`. Проверить, не подгружается ли ранее обнаруженный source-chunk после unload. Поиск пустого source list задерживается на пять секунд; `refreshSources()` очищает только source-list, не delay.
- `SourceKey` кэша essentia хранит position/dimension, но не range/direction и world identity. Повторные вызовы с изменённым направлением/другим типом потребителя в той же точке могут использовать старую область. В отчёте это не объявлено подтверждённой игровой регрессией: внутренние текущие callers обычно используют постоянные параметры.
- ItemMirror `isItemValidForSlot()`/`setInventorySlotContents()` (`:378–390,429–432`) проверяют наличие destination mirror, но не reciprocal link; item-entity `transport()` проверяет его. Это унаследованная асимметрия. Нужна трасса break/relink + automation, прежде чем менять TC4 sink contract.
- Mirror peer lookup, bore depth scan, lamps и scrubber читают world за пределами текущего чанка без единообразного loaded guard. Не все такие чтения вредны; механики некоторых устройств предполагают радиус действия. Нужны управляемые chunk-boundary fixtures вместо массового добавления `isBlockLoaded()`.
- `MazeHandler.saveMaze()` проверяет финальный rename fallback и имеет backup, но промежуточные delete/rename не проверяются. Ошибки файловой системы/abrupt shutdown отдельно не воспроизведены. Никакого заключения о legacy labyrinth migration здесь нет.

## Что уже реализовано корректно в просмотренных путях

- Проверка reciprocity у item mirror transport, origin marker против немедленного повторного захвата своей выдачи, `null`-проверка отсутствующего target dimension и dirty marking очереди присутствуют.
- Очередь mirror сохраняется через собственный `Items` NBT и фильтрует пустые stacks при загрузке/сохранении; подтверждённой потери **при обычной выгрузке/загрузке порта** в этом пути не найдено.
- Свежие inventory-классы используют `ItemStack.EMPTY`; подтверждённый дефект только из-за пустого sentinel в проверенных `InventoryMob/Trunk/Pech` не найден. `InventoryMob` отдельно сохраняет расширенный размер ghost-фильтров через `tcGhostCount`.
- Bore отправляет cancellable `BlockEvent.BreakEvent` с FakePlayer перед добычей; проверка protection integration этим не исчерпывается, но полный обход event в рассмотренном пути не обнаружен.
- `MazeHandler` использует scoped streams, backup и positive-cell serialization; `MazeThread` в текущих вызовах запускается синхронно. Новый async world-write race не найден.
- `ChunkProviderOuter.recreateStructures()` не пересоздаёт maze над сохранёнными чанками, что снижает риск перезаписи свежего мира при reload.

## Проверки и ограничения

Выполнены `git status --short`, `rg --files`, `rg -n`, выборочные/полные `Get-Content` чтения классов и соответствующих tests; сравнение найденных ветвей с оригинальным CFR oracle. Количество операций portal/scrubber проверено арифметически в PowerShell. Просмотрены relevant mirror, tube, hungry chest, fluid/golem, maze persistence/invariant, arrival static-guard и worldgen tests; **их чтение не равно запуску**.

Сборка, JUnit и server/client smoke в рамках этого раздела **не запускались**. Производственный код не менялся, поэтому documentation-only diff не требует runtime smoke по `AGENTS.md`. Для закрытия gameplay findings требуются focused runtime tests и соответствующие воспроизведения, перечисленные выше. В частности, compile success не подтвердит scheduler Pech, conservation per fluid, NBT session cleanup, отменённый spawn или выгрузку мира.

Коммит не создавался. В этом подразделе добавлен только данный файл отчёта; оригинальные jar/oracle/`thaumcraft_src` не менялись. Главное ограничение — первая широкая статическая ревизия с углублением выбранных критичных путей, а не завершённая проверка каждой сущности, каждого tile и всех сочетаний модов.
