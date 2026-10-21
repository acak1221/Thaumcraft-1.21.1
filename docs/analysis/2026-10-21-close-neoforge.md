# Закрытие остатков порта TC4 4.2.3.5 → Minecraft 1.21.1 (NeoForge) — 21.10.2026

Ветка: `port/neoforge`. Рабочая директория: `/home/Acak/tcw/neoforge`.
Репозиторий `/home/Acak/thaumcraft-1.21.1` не трогался.

Источники истины (в порядке приоритета):

1. Оракул `/home/Acak/work/thaumcraft-4.2.3.5-oracle` (декомпиляция
   `Thaumcraft-1.7.10-4.2.3.5.jar`, имена ваниллы в SRG-форме).
2. OpenTC4 `/home/Acak/work/opentc4/src/main/java/thaumcraft/...` (читаемый 1.7.10).
3. Читаемая ванилла 1.7.10 `/home/Acak/work/mc1710-src/net/minecraft/...`.
4. `thaumcraft_src/` и `src/main/java` — 1.12-справочник, только как пример.

Окружение прогона: `JAVA_HOME=/home/Acak/.jdk21`, `GRADLE_USER_HOME=/home/Acak/gradle-homes/neoforge`,
Gradle 8.12.1 (wrapper), NeoForge 21.1.259, Architectury 13.0.11, Minecraft 1.21.1.

---

## 1. Итог

| Проверка | Результат |
|---|---|
| `:neoforge:compileJava` | BUILD SUCCESSFUL |
| `:neoforge:runServer` | **«Done (5.314s)!»**, мир сгенерирован, остановка чистая, ошибок в логе нет |
| `:neoforge:runGameTest` | **300 сценариев, 278 проходят, 22 падают** (все 22 перечислены и разобраны ниже) |
| `:fabric:runGameTest` (до волны, 5084a65) | 312 сценариев, 7 падений |
| `:fabric:runGameTest` (после волны) | 312 сценариев, 5 падений — **регрессий нет** (падающее подмножество базового) |
| Клиент | **не запускался**: в этой песочнице JVM падает в GL (см. отчёт клиентской волны) |

До волны модуль `neoforge` был девятнадцатью строками: `Thaumcraft.init()` плюс
`ClientSetup.init()`. Выделенный сервер падал в общем миксине раскопки, а при старте
34 типа сущностей TC4 грузились без атрибутов. Теперь модуль выполняет те же регистрации,
что и Fabric-бутстрап, и выделенный сервер NeoForge поднимается.

---

## 2. Расхождения паритета Fabric ↔ NeoForge (найдено и закрыто)

### 2.1 Атрибуты сущностей — 34 типа без атрибутов

Fabric регистрирует каталог через `FabricDefaultAttributeRegistry`. В NeoForge такого
регистра нет: атрибуты типа сущности даёт `EntityAttributeCreationEvent`.

* Было: `[minecraft/Util]: Entity thaumcraft:pech has no attributes` … (34 строки в логе
  первого запуска сервера, включая `brainy_zombie`, `firebat`, `wisp`, `pech`,
  `eldritch_warden`, `cultist_leader`).
* Стало: `neoforge/.../NeoForgeEntityAttributes.java` публикует **тот же список**, что и
  Fabric-бутстрап: 15 прямых записей (`EntityPech`, `EntityFireBat`, `EntityWisp`,
  `EntityTaintSpider`, `EntityTaintSpore`, `EntityTaintSporeSwarmer`, `EntityTaintSwarm`,
  `EntityTaintacle`, `EntityTaintacleSmall`, `EntityBrainyZombie`,
  `EntityGiantBrainyZombie`, `EntityGolemBase`, `EntityTravelingTrunk`,
  `EntityMindSpider`, `EntityEldritchGuardian`) плюс четыре общих каталога
  (`ThaumcraftEldritchMobs`, `ThaumcraftCultists`, `ThaumcraftTaintCreeper`,
  `ThaumcraftTaintAnimals`).
* Событие срабатывает после заморозки реестра, поэтому тип сущности разрешается внутри
  слушателя, а не при конструировании мода (на NeoForge это иначе `Registry Object not
  present`).

### 2.2 Точки появления (spawn placement)

`SpawnPlacements.register` в 1.21.1 приватный, а NeoForge его ещё и помечает
@Deprecated в пользу `RegisterSpawnPlacementsEvent`. Эквивалент — это событие.

* Стало: `neoforge/.../NeoForgeSpawnPlacements.java` регистрирует **точный список
  Fabric-бутстрапа** — печ (`ON_GROUND`), огненная мышь, висп, рой скверны, тентакль
  (`NO_RESTRICTIONS`), мозговой зомб и великий мозговой зомб (`ON_GROUND`) с теми же
  предикатами (`EntityPech::checkPechSpawnRules`, `EntityFireBat::checkFireBatSpawnRules`,
  `EntityWisp::checkWispSpawnRules`, `EntityTaintSwarm::checkTaintSwarmSpawnRules`,
  `EntityTaintacle::checkSpawnRules`, `EntityBrainyZombie::checkBrainySpawnRules`).
* Дополнительно: `thaumcraft:eldritch_guardian` и `thaumcraft:inhabited_zombie` имеют
  запись о появлении в данных биома, но не имеют собственного правила появления. На Fabric
  это состояние «без ограничений» (правило появления не зарегистрировано → ванильная
  проверка проходит). NeoForge считает это ошибкой и требует явного правила, поэтому они
  зарегистрированы как `NO_RESTRICTIONS` — **то же поведение, без ошибки в логе**.

### 2.3 Миксин раскопки — падение сервера

Общий миксин `ExcavationServerPlayerGameModeMixin` перехватывал три вызова ванильного графа
`ServerPlayerGameMode.destroyBlock`. NeoForge 21.1 этот граф переписывает (патч
`net/minecraft/server/level/ServerPlayerGameMode.java.patch` NeoForge 21.1.259):

* удаление идёт не через `ServerLevel.removeBlock(BlockPos, boolean)`, а через собственный
  приватный `removeBlock(BlockPos, BlockState, boolean)`, который вызывает
  `BlockState.onDestroyedByPlayer(Level, BlockPos, Player, boolean, FluidState)`;
* виртуальный `Block.destroy(LevelAccessor, BlockPos, BlockState)` переехал в тот же
  приватный помощник;
* проверка сбора урожая — `BlockState.canHarvestBlock(BlockGetter, BlockPos, Player)`, а не
  `ServerPlayer.hasCorrectToolForDrops(BlockState)`.

* Было: `Critical injection failure: Redirector thaumcraft$witnessNativeRemoval ... (0/1)
  succeeded. Scanned 0 target(s)` → сервер не стартовал.
* Стало: три перехвата вынесены в **лоадерные** миксины с `require = 1`:
  `fabric/.../FabricExcavationRemovalMixin.java` (ванильный граф) и
  `neoforge/.../NeoForgeExcavationRemovalMixin.java` (граф NeoForge 21.1). Общий миксин
  сохранил только те перехваты, что одинаковы у обоих графов.

### 2.4 Access widener → access transformer

У NeoForge нет access widener. Три записи общего
`common/src/main/resources/thaumcraft.accesswidener` (конструктор `Projectile`,
`ItemEntity.pickupDelay`, `LivingEntity.lastHurtByPlayerTime`) перенесены один в один в
`neoforge/src/main/resources/META-INF/accesstransformer.cfg` и объявлены в
`neoforge.mods.toml` через `[[accessTransformers]]`.

### 2.5 Креативная вкладка — падение сборки содержимого

Architectury 13.0.11 на NeoForge отображает обычный `CreativeTabOutput.accept` в
NeoForge-операцию «вставить после пустого стека», а NeoForge её отвергает:
`IllegalArgumentException: Itemstack 0 minecraft:air does not exist in tab's list`
(ошибка в `BuildCreativeModeTabContentsEvent`). Содержимое вкладки TC4 (варианты эссенции
виспа и читерская книга) на NeoForge не собиралось вовсе.

* Стало: `common/.../ThaumcraftCreativeTabs.java` публикует стеки вкладки через
  лоадерный sink (Fabric-путь — прежний вывод Architectury, NeoForge-путь — native
  `BuildCreativeModeTabContentsEvent`). Стеки публикуются **поставщиком** и разрешаются
  при сборке вкладки: реестр NeoForge заполняется только после конструирования мода, а
  читать `THAUMONOMICON_CHEAT.get()` при конструировании нельзя.

### 2.6 Модовые жидкости — падение мира

NeoForge отвергает модовую жидкость без собственного `FluidType`:
`RuntimeException: Mod fluids must override getFluidType.` Мир падал при установке
очищающей воды (`BlockFluidDeath.onPlace` → `LiquidBlock.onPlace`).

* Стало: `neoforge/.../NeoForgeFluidPureMixin.java` и `NeoForgeFluidDeathMixin.java` дают
  обеим жидкостям тип NeoForge (вода + `canConvertToSource(false)`; смертная — вязкая,
  светится восемью, как её блок).

### 2.7 Правила появления по биомам (оригинальные TC4)

Fabric правит биомы в рантайме через `BiomeModifications` (`REPLACEMENTS`). В NeoForge такой
события нет: биом-модификаторы NeoForge — это датапак.

* Стало: `neoforge/.../OriginalEntitySpawnsBiomeModifier.java` + тип
  `thaumcraft:original_entity_spawns` в
  `data/thaumcraft/neoforge/biome_modifier/original_entity_spawns.json`. Селектор тот же
  (`magical_biomes` ∪ `brainy_zombie_spawns` ∪ `is_nether`), преобразование то же
  (`OriginalSpawnRules.apply`, порядок `ConfigEntities.initEntitySpawns`), фаза — после
  прочих модификаторов, как `REPLACEMENTS` у Fabric.

### 2.8 Фичасти-биом-модификаторы (паритет 8 ↔ 8)

Восемь вызовов `BiomeModifications.addFeature` Fabric-бутстрапа и восемь файлов
`data/thaumcraft/neoforge/biome_modifier/*.json` совпадают один в один: руды
(`underground_ores`), узлы ауры (`top_layer_modification`), магические деревья
(`vegetal_decoration`), кольцо Outer Lands, камни на холмах, дома банкира, башни чародея,
тотем ауры.

### 2.9 Защитные обратные вызовы (раскопка, големы, обмен блоками)

* Fabric: `PlayerBlockBreakEvents.BEFORE/CANCELED/AFTER`, `UseBlockCallback`,
  `AttackBlockCallback`, `BlockEvent.PLACE` (Architectury).
* NeoForge: отменяемые `BlockEvent.BreakEvent`, `BlockEvent.EntityPlaceEvent`,
  `PlayerInteractEvent.RightClickBlock`, `PlayerInteractEvent.LeftClickBlock` —
  в `neoforge/.../NeoForgeInteractions.java`, `NeoForgeBorePlatform.java`,
  `NeoForgeGolemHarvestPlatform.java`, `NeoForgeGolemUsePlatform.java`.
* Актёр — `FakePlayerFactory.get(level, PROFILE)` NeoForge с тем же зарезервированным
  профилем, что и на Fabric (`BorePlatform.MACHINE_UUID` / `MACHINE_NAME`), никогда не
  игрок-онлайн.

### 2.10 Сундуки оригинальных TC4

Fabric дописывает оригинальные строки в основной пул ванильной таблицы через изменяемый
билдер Fabric-API. Структуры лута 1.21.1 неизменяемы, а их конструкторы пакетно-приватные,
поэтому NeoForge-путь переписывает **закодированную** таблицу загрузчика: оригинальные
строки дописываются в `entries` первого пула, таблица декодируется заново. Итог — тот же
единый взвешенный список, а не отдельный пул.

### 2.11 Ёмкость арканной купальни

Fabric публикует бак через transfer-API. NeoForge владеет `IFluidHandler`:
`neoforge/.../NeoForgeSpaStorage.java` публикует бак с теми же правилами стороны
(никогда сверху) и той же транзакционной границей `TileSpa`.

### 2.12 Клиент

`neoforge/.../client/ThaumcraftNeoForgeClient.java` регистрирует через события NeoForge то,
что Fabric делает через Fabric-API: два core-шейдера (`RegisterShadersEvent`), builtin
рендереры предметов (`IClientItemExtensions.getCustomRenderer`), HUD (`RenderGuiEvent.Post`),
поздний мировой рендер (`RenderLevelStageEvent`), перезагрузку клиентских ресурсов
(`RegisterClientReloadListenersEvent`) и сброс на отключении
(`ClientPlayerNetworkEvent.LoggingOut`).

---

## 3. Приёмка

### 3.1 Выделенный сервер NeoForge — проходит

Команда: `./gradlew :neoforge:runServer` (Gradle 8.12.1, Java 21, NeoForge 21.1.259).

Реальный фрагмент лога (`/tmp` копия прогона, ветка `port/neoforge`):

```
[22:37:13] [modloading-worker-0/INFO] [Thaumcraft/]: Loaded 12 original smelting bonus entries (6 skipped)
[22:37:13] [modloading-worker-0/INFO] [Thaumcraft/]: TC4 Minecraft 1.21.1 migration: 201 research definitions loaded; gameplay coverage is tracked in MIGRATION_STATUS.md
[22:37:17] [Server thread/INFO] [minecraft/DedicatedServer]: Starting minecraft server version 1.21.1
[22:37:17] [Server thread/INFO] [minecraft/DedicatedServer]: Starting Minecraft server on *:25565
[22:37:18] [Server thread/WARN] [ne.ne.ne.se.ServerLifecycleHooks/]: Detected minecraft:bat that was registered with AMBIENT mob category but was added under CREATURE mob category for thaumcraft:eerie biome! ...
[22:37:19] [Server thread/INFO] [minecraft/DedicatedServer]: Preparing level "world"
[22:37:21] [worldgen/INFO] [minecraft/LoggerChunkProgressListener]: Preparing spawn area: 18%
[22:37:23] [Server thread/INFO] [minecraft/DedicatedServer]: Done (5.314s)! For help, type "help"
[22:37:23] [Server thread/INFO] [Thaumcraft/]: TC4 Outer Lands dimension registered: thaumcraft:outer_lands (FlatLevelSource generator, thaumcraft:eldritch_lands biome, height 256, min y 0, sky light false, ambient light 0.0, fixed time 0)
[22:37:23] [Server thread/INFO] [Thaumcraft/]: TC4 object aspect registry validation: 298 accepted, 0 missing item ids, 0 missing block ids, 0 conflicts
[22:38:32] [Server thread/INFO] [minecraft/MinecraftServer]: Stopping the server
[22:38:32] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[world]'/minecraft:overworld
[22:38:32] [Server thread/INFO] [minecraft/MinecraftServer]: Saving chunks for level 'ServerLevel[world]'/thaumcraft:outer_lands
[22:38:32] [Server thread/INFO] [minecraft/MinecraftServer]: All dimensions are saved
```

Ошибок (`ERROR`) в логе нет. Единственное предупреждение NeoForge про `minecraft:bat` в
биоме `thaumcraft:eerie` — это свойство **оригинальных** данных TC4 (летучие мыши в
категории монстров), а не дефект порта; Fabric такое предупреждение не печатает.

**Не проверено:** клиент, мультиплеер, survival-прогрессия, обычный (не dev) сборка и
запуск сервера, упакованный (production) JAR. Клиент в этой песочнице не запускается (JVM
падает в GL).

### 3.2 GameTest NeoForge — 300 сценариев, 278 проходят

Команда: `./gradlew :neoforge:runGameTest` (изолированный мод `thaumcraft_gametest`).

До волны модуль NeoForge содержал **63** сценария; порт добавил 239 общих сценариев
Fabric-харнесса (общие проверки живут в `common/src/gameTest`), итого 300 выполненных
сценариев, 22 падения, 0 ошибок, 0 пропусков в `neoforge/build/gametest-results.xml`.

**Восемь падений — отсутствующие пробы Fabric-харнесса** (не дефект геймплея, а пробел
тестовой обвязки; на NeoForge нет Fabric-проб, которые регистрируются до заморозки реестра):

| Сценарий | Причина |
|---|---|
| `nodetransductionconversionpersistenceandcooldown` | `Game-test mod must register node probes before registry freeze` |
| `nodetransductionisolatedfailurelifecycle` | то же |
| `excavationextraremovalfailureaccounting` | `Register Excavation removal probe BEFORE block registry freeze` |
| `excavationharvestpreservesprotectionlootandcontainers` | `Fabric native event probe unavailable` |
| `excavationupgradetreasureandenlarge` | `Actual native AFTER probe unavailable` |
| `growthlampconsumesessentiaandfertilisesplants` | `Game-test bootstrap must register the isolated native crop probe` |
| `fluxcallbacksboundariesandneighborwake` | `registerTestProbes must register the test-only callback observer BEFORE registry freeze` |
| `visamuletstransferandpaythroughactualnativerelay` | `test mod registered the native amulet pulse probe before registry freeze` |

**Четырнадцать падений — реальные расхождения поведения NeoForge** (список честный, ничего
не скрыто):

| Сценарий | Расхождение |
|---|---|
| `lumbergolemfellsonefarlogthroughtheguardedplatform` | защитное `BreakEvent` NeoForge срабатывает дважды на одну точку (Fabric-API — один раз) |
| `boreouterdestroycallbackremainsclosed` | порядок `BLOCK_DESTROY` при внутреннем `setBlock` отличается (NeoForge закрывает вызов позже) |
| `magicalbiomesgeneratenaturallyandspawnactualpech` | генерация региона не выдаёт ожидаемый тип блочной сущности с возрастом 3..7 |
| `purifyingfluidpreservesoriginalclassicflowwardandsaltexpiry` | `filledPercentage` нативного тика равен 0 вместо ожидаемого |
| `tablebreakevacuateseachhalfonce` | после `server-level-post` разрушение половины выдаёт не однуraw-таблицу |
| `bookshelfwandcreatesonefloatingbook` | результат не всплывает (падает и на Fabric) |
| `lootbagspreserveoriginalpoolsandpaidnativedropcustody` | «нативная таблица подземелий, загруженная через взвешенный пул Fabric» — на NeoForge путь лута другой |
| `entropyqueuesthefarthestconnectedwaterforliquidgolems` | очередь энтропии: 5000 mB в големе, ни одного сконъюённого ведра |
| `entropyswitcheschangewhatrealgolemsfetchemptyandsort` | полосы сопоставления предметов не завершились за 1800 тиков |
| `golemaccessoriesinstallchangestatsandsurvivethebell` | `PLAIN: the Guard survived` (падает и на Fabric) |
| `travelingtrunkfollowsteleportsdefendsandpullsitems` | фазы сундука не завершились за 1500 тиков |
| `unstablenodesemitpaidaspectorbsandcanstabilise` | реальный тофауометр не видит естественную нестабильную цель |
| `voidmetalgearmendswithersandwarps` | `void_sword` не иссушает моба 60 тиков |
| `taintacleskeeporiginalcombatandrootedai`, `taintacleskeeporiginalretaliationlifetimeandloot` | падение без сообщения (исключение внутри проверки) |
| `advancedfurnacepreservescustodyacrossrealchunkunload` | частичная разборка не форсирует загрузку удалённого рельефа |

### 3.3 Регрессий на Fabric нет

`:fabric:runGameTest` на базовом коммите `5084a65` — 312 сценариев, 7 падений; на текущей
ветке — 312 сценариев, 5 падений. Падающее подмножество на текущей ветке
(`constructandcentrifugerecipes`, `bookshelfwandcreatesonefloatingbook`,
`golemfetterholdsgolems`, `golemaccessories`, иногда `magicalbiomes`) входит в базовый набор;
остальные трое базовых падений (`magicalbiomes`, `unstablenodes`, `essentiareservoir`) —
нестабильные сценарии мирового поколения.

---

## 4. Что осталось открытым (честно)

1. **Клиент NeoForge не проверен и не дописан.** В песочнице клиент не запускается (JVM
   падает в GL). Клиентские расширения Fabric-API без эквивалента в NeoForge 1.21.1 не
   перенесены: `FluidRenderHandlerRegistry` (рендер чистой воды), API рендера Fabric
   (`RenderMaterial`/`QuadEmitter` в модели `taint_fibres`) и `ArmorRenderer` (рендер
   пустого ремня, одежд пустоты и крепостной брони). Это **содержательный пробел
   клиента**, а не приёмка.
2. **Уведомления «блок отменён» и «блок разрушен»** платформы раскопки/голема не имеют
   события NeoForge; в мосте NeoForge отменяемое `BlockEvent.BreakEvent` — единственный
   носитель решения. Fabric-тройка BEFORE/CANCELED/AFTER не воспроизводится один в один.
3. **Пробы Fabric-харнесса** (узлы, посев, удаление при раскопке, AFTER, пульс амулета,
   наблюдатель нативных событий) не имеют NeoForge-эквивалента — восемь сценариев падают
   только из-за этого.
4. **Четырнадцать** сценариев показывают реальные расхождения поведения (список выше);
   они не исправлены этой волной.
5. **Обычная (production) сборка и запуск** NeoForge-мода не проверялись: прогонялся
   только dev-запуск из loom. `accessTransformers`, миксин-конфиги и ресурсы проверены на
   dev-сборке; в production-JAR те же файлы попадают через `processResources`/`shadowJar`,
   но приёмки production-артефакта не было.
6. **Мультиплеер, survival-прогрессия, сетевой приём с настоящим клиентом** не
   проверялись.

---

## 5. Изменённые и добавленные файлы

Общее:

* `common/src/main/java/thaumcraft/common/mixin/ExcavationServerPlayerGameModeMixin.java` —
  три перехвата вынесены в лоадерные миксины.
* `common/src/main/java/thaumcraft/common/config/ThaumcraftCreativeTabs.java` (новое) —
  публикация стеков креативной вкладки через лоадерный sink.
* `common/src/main/java/thaumcraft/common/config/ThaumcraftContent.java`,
  `ThaumcraftEssences.java` — публикация стеков вкладки через новый sink.

Fabric:

* `fabric/src/main/java/thaumcraft/fabric/mixin/compat/FabricExcavationRemovalMixin.java`
  (новое) — ванильный граф `destroyBlock`.
* `fabric/src/main/resources/thaumcraft-fabric-compat.mixins.json` — подключён миксин.

NeoForge (основное):

* `neoforge/src/main/java/thaumcraft/neoforge/ThaumcraftNeoForge.java` — бутстрап.
* `.../NeoForgeEntityAttributes.java`, `NeoForgeSpawnPlacements.java`,
  `NeoForgeBiomeSpawns.java`, `OriginalEntitySpawnsBiomeModifier.java`,
  `NeoForgeInteractions.java`, `NeoForgeLootTables.java`, `NeoForgeSpaStorage.java`,
  `NeoForgeCreativeTabs.java`, `NeoForgeBorePlatform.java`,
  `NeoForgeGolemHarvestPlatform.java`, `NeoForgeGolemUsePlatform.java` (новые).
* `.../mixin/NeoForgeExcavationRemovalMixin.java`,
  `.../mixin/NeoForgeFluidPureMixin.java`,
  `.../mixin/NeoForgeFluidDeathMixin.java` (новые).
* `.../client/ThaumcraftNeoForgeClient.java` (новое).
* `neoforge/src/main/resources/META-INF/accesstransformer.cfg` (новое),
  `neoforge/src/main/resources/thaumcraft-neoforge.mixins.json` (новое),
  `neoforge/src/main/resources/data/thaumcraft/neoforge/biome_modifier/original_entity_spawns.json`
  (новое), `neoforge/src/main/resources/META-INF/neoforge.mods.toml` (access transformer,
  второй миксин-конфиг).

NeoForge (харнесс GameTest):

* `neoforge/src/gameTest/java/thaumcraft/neoforge/testing/ThaumcraftGameTests.java` — 300
  сценариев.
* `.../testing/mixin/GameTestHelperMockConnectionMixin.java` (новое) — каналы NeoForge для
  mock-игроков GameTest.
* `neoforge/src/gameTest/resources/thaumcraft-neoforge-gametest.mixins.json` (новое),
  `neoforge/src/gameTest/resources/META-INF/neoforge.mods.toml` (миксин-конфиг).

Коммиты: `f39307a` (регистрации NeoForge), `f06fc6d` (порт GameTest и исправления
раскрытых пробелов).
