# NeoForge: полный набор GameTest и три дефекта, видимые только на NeoForge — 26.10.2026

Базис: `3c5fe20` (ветка `fabric`). По `AGENTS.md` после Fabric нужно довести NeoForge. Эта волна
впервые с начала контентных волн собрала и запустила NeoForge на текущем общем коде. Игровые
значения TC4 не менялись. Три исправления в общем коде возвращают NeoForge к поведению оригинала
и Fabric.

## Исходное состояние

- `:neoforge:build` проходил, но набор GameTest NeoForge **не компилировался**. Тело
  `magicalFlowersGrowHarvestAndRefine` вызывало старую сигнатуру `PlantRuntimeChecks.run(test)`.
- Каталог NeoForge отстал: **307** обёрток против **469** у Fabric.

## Каталог GameTest NeoForge

Каталог пересобран из Fabric новым детерминированным скриптом
`scripts/porting/neoforge_gametests.py` (`--write` пересобирает, без флага только проверяет).
Порядок, комментарии и аннотации берутся из Fabric, шаблон переводится на `thaumcraft:empty`.
Тела, которые на NeoForge намеренно другие, сохраняются: настоящие сетевые тестовые игроки,
сессии с захватом знаний, помощники `notePlayer`/`purchaseSession`.

| Источник обёртки | Количество |
|---|---|
| Тело Fabric без изменений | 401 |
| Тело Fabric, игрок заменён на `NetworkedGameTestPlayer` | 32 |
| Сохранённое тело NeoForge | 19 |
| **Итого на NeoForge** | **452** |
| Пропущено: фикстуры на API, которого у NeoForge нет | 17 |

Пропущены 17 обёрток, чьи фикстуры работают на API Fabric и не имеют общего аналога:

- `boreProtection*` (9): `BoreProtectionProbe` проверяет событие защиты Architectury/Fabric;
- `followupLootIncludesDatapackOverrides`: `LootSourceRegressionChecks` на событиях таблиц добычи Fabric;
- `spaUsesNativeFabricTransactionsAndRetiredOwners`, пять `fluid*Conserve*` и
  `pureBucketParticipatesInActualNativeFluidTransactions`: транзакции Fabric Transfer API.

Аналоги на NeoForge (capabilities `IFluidHandler`, модификаторы добычи, события защиты) этими
GameTest **не покрыты**. Это открытый пробел, он не закрыт пропуском.

`test_neoforge_gametests.py` (запускается вместе с остальными тестами `scripts/porting`) проверяет
две вещи. У каждой обёртки Fabric, кроме перечисленных, есть обёртка NeoForge с тем же именем.
Повторная генерация даёт файл байт в байт. Отставание каталога теперь ловится без запуска NeoForge.

## Что пришлось поправить в тестовой обвязке

- **Разделённые пакеты JPMS.** На NeoForge тестовый код — отдельный модуль `thaumcraft_gametest`,
  и он не может делить пакет с модом. Три класса проверок перенесены в `thaumcraft.common.testing`:
  `MoundBoundaryRuntimeChecks`, `MoundRandomRuntimeChecks`, `WandPedestalRegressionChecks`. Для
  этого четыре метода мода стали `public`: `TileNode.beginTransduction`, `endTransduction`,
  `drawForPedestal` и `WorldGenMound.lootState`. Логика не менялась.
- **Асинхронная запись `SavedData`.** NeoForge 21.1 пишет `SavedData` на своём IO-потоке, поэтому
  проверки «сохранить и перечитать с диска» читали старый файл. `SavedDataFlush` ждёт этот поток
  через `IOUtilities.waitUntilIOWorkerComplete`. На Fabric ожидание пустое.
- **Ресурсы чужого модуля.** `sounds.json` теперь читается через класс мода, а не через тестовый
  класс: на NeoForge ресурсы модуля мода закрыты для тестового модуля.
- Зонд соседей `ResourceSafetyRuntimeChecks` регистрируется в `RegisterEvent` блоков. Добавлен
  `GolemPlacementProbe` для NeoForge на настоящем `PlayerInteractEvent.RightClickBlock`, аналог
  зонда Fabric.

## Дефекты мода, найденные только на NeoForge

### 1. Первозданный сокрушитель брал лишнюю прочность и ломал центр без инструмента

NeoForge в `ServerPlayerGameMode.destroyBlock` вызывает `ItemStack.mineBlock` **до** удаления
блока (ванилла и Fabric — после). Цикл 3×3 сокрушителя видел центр ещё на месте. Он снимал
десятую единицу прочности (тест ждёт 9) и ломал центр через `level.destroyBlock`, то есть без
инструмента: удача и шёлковое касание к центральному блоку не применялись. Удаление самого NeoForge
затем находило воздух, поэтому двойного дропа не было.

Исправление: цикл пропускает центр, его ломает и оплачивает сам загрузчик (`super.mineBlock`).
На Fabric центр уже был воздухом, поведение там не изменилось. В оригинале центр тоже ломался один
раз за удар, а всего за удар по полному квадрату снималось 9 единиц. Это видно по байткоду
оригинального JAR: `ItemPrimalCrusher.func_150894_a` без приседания не вызывает `super`, а для
каждой из девяти позиций делает `func_77972_a(1)` и `BlockUtils.harvestBlock`. Forge 1.7.10
вызывал его до удаления центра.

### 2. Разборка улучшенной алхимической печи загружала соседний чанк

У NeoForge `Level.removeBlockEntity` после удаления сущности блока всегда шлёт уведомление
компараторам (`updateNeighbourForOutputSignal`), каковы бы ни были флаги записи. За сплошным
соседом оно читает второй блок **без проверки загрузки чанка**. При разборке печи на границе
чанков часть рядом с границей синхронно грузила недоступную половину машины. Это видно по стеку:
`BlockAdvancedAlchemyFurnace.onRemove` → `BlockBehaviour.onRemove` → `Level.removeBlockEntity` →
`updateNeighbourForOutputSignal` → `Level.getBlockState` → `ServerChunkCache.getChunk`. Именно от
этого защищает уже существующая тихая разборка (флаги 18) при неизвестной местности.

Исправление: если такое уведомление дотянулось бы до незагруженного чанка, часть печи снимает
сущность блока через `LevelChunk.removeBlockEntity`. Ровно это делает ванильный
`Level.removeBlockEntity`. На NeoForge чанк по-прежнему вызывает `setRemoved`, снимает
capabilities и свет. Пропускается только уведомление компараторам, а запись в этом случае и так
без обновлений соседей.

### 3. Срабатывания жезла шли внутри захвата снимков блоков NeoForge

TC4 обрабатывал щелчок жезла по блоку (формации, ворота Окулуса, печи, тигель, верстак) в Forge
1.7.10 `onItemUseFirst`, до того как `ForgeHooks.onPlaceItemIntoWorld` начинал захват снимков
блоков. Порт обрабатывает щелчок в `useOn`. NeoForge выполняет серверную половину
`ItemStack.useOn` с `captureBlockSnapshots = true` (`CommonHooks.onPlaceItemIntoWorld`). Каждая
запись формации откладывает `onPlace` и все уведомления соседей до возврата из `useOn`.

Последствие для Окулуса: проверка «портал встал?» всегда проходила. Вис списывался, алтарь
открывался, и только потом уведомлённый сосед мог убрать портал, а тёмный узел, который код
должен восстановить, терялся. GameTest `followupOculusPreservesNodeOnFailure` падал на
NeoForge: `result=CONSUME, entity=null, unpaid=false, altarOpen=true`.

Исправление: `useOn` жезла выполняется через `WandUseCapture`. На NeoForge
(`NeoForgeWandUseCapture`) захват на это время выключается и потом восстанавливается. Снимки,
снятые до вызова, остаются. На Fabric захвата нет, мост — тождественный.

Следствие, которое надо знать: формации жезла на NeoForge больше не порождают
`BlockEvent.EntityPlaceEvent`/`EntityMultiPlaceEvent`. Так было и в оригинале (`onItemUseFirst`
событий установки не давал), и так сейчас на Fabric. Сам щелчок по-прежнему проходит
`PlayerInteractEvent.RightClickBlock`, а код формаций сам проверяет `mayInteract`.

## Метаданные и экран настроек NeoForge

- Экран настроек регистрируется через `IConfigScreenFactory` (кнопка «Config» в списке модов),
  тот же `ThaumcraftConfigScreen`, что открывает Mod Menu на Fabric.
- `neoforge.mods.toml`: описание, ссылки на трекер и страницу, иконка Таумономикона.
- **Не проверено в настоящем клиенте NeoForge**: собирается, но кадров нет.

## Что проверено

| Проверка | Результат |
|---|---|
| `:neoforge:build` (JAR, проверки изоляции тестового и производственного кода) | пройдено |
| `NeoForgeLoaderParityTest` | 12/12 |
| `:neoforge:runGameTest` на свежем мире | **452/452** обязательных за 2,8 мин; сохранение всех измерений и остановка штатные |
| Фокусный NeoForge-прогон трёх бывших падений после исправлений | 3/3 |
| `scripts/porting` Python | 115 тестов, OK (6 skipped — нужен приватный oracle) |
| `git diff --check` | чисто |
| `:common:test` после изменений общего кода | 2571/2571 |
| `:fabric:build` (JAR, проверки изоляции) | пройдено |
| `:fabric:runGameTest` на свежем мире | **467/467** обязательных за 2,8 мин; строк `No data fixer registered` — 0 |

До исправлений полный прогон NeoForge дал 446/452. Упали `saveCycleWorldDiskReload`,
`mazeAuditGeneratesRealRegionAndReloadsMap` (обвязка: асинхронный `SavedData`),
`sound_registry_covers_bundled_events` (обвязка: ресурсы модуля), а также три дефекта выше.

## Не сделано

- Плагина EMI для NeoForge нет (плагин живёт в модуле Fabric).
- Не запускались: настоящий клиент NeoForge, выделенный сервер NeoForge с игроками,
  мультиплеер. GameTest — не приёмка клиента и выживания.
- 17 фикстур на API Fabric не имеют аналогов на NeoForge (см. выше).
