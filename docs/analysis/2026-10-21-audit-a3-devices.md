# 2026-10-21 — аудит домена a3: машины и VisNet (advanced_devices_and_visnet, device_and_menu_routing_review)

Аудит **чтением**, без правок порта. Оракул — `/home/Acak/work/thaumcraft-4.2.3.5-oracle`
(декомпиляция TC4 4.2.3.5, единственный источник истины). Нативный порт — `common/src/main/java/thaumcraft/`
в worktree `audit/a3`. Ревью — `docs/porting/audit-reviews-a3-devices.json` (120 строк, те же
`reviewed_sha256`, что в worklist).

## Итог

| native_coverage | количество | что это |
|---|---|---|
| `parity_verified` | 67 | нативный аналог есть, числа/поведение сходятся |
| `partial` | 39 | аналог есть, но ведёт себя не полностью (см. раздел «Дефекты» и «Отклонения») |
| `not_assessed` | 14 | класс существует только в легаси 1.12, в оракуле его нет — обязательства порта не несёт |

| disposition | количество |
|---|---|
| `adapt` | 68 |
| `replace` | 38 |
| `exclude` | 14 |

| source_semantics | количество |
|---|---|
| `adapted` | 102 |
| `different` | 4 (`TileFluxScrubber`, `TileMirror`, `TileMirrorEssentia`, `ContainerGhostSlots`) |
| `not_applicable` | 14 |

`callsite_audit: complete` — 120 из 120.

**Главный вывод домена.** Числа порта в основном перенесены точно (диапазон узла 8, модификаторы
1.2/0.8/0.5, `floor(sqrt(amt))`, нестабильный `nextInt(5)-2` и `nextInt(500)==1`, время готовки
140/80 и −20 за кузнец, всасывание сопла 128 при отсечке 40, пятисекундный цикл кристаллизатора с
запросом `min(20, max(1, (200-progress)/2))` и удвоением прогресса, 40 тиков кристаллизатора/спа,
128 всасывания тауматориума, 5 CV за примитив у релея заряда, 27 слотов на ящик, 4096 и 10 тиков
у сенсора, 0..37 у стабилизатора, радиусы 8/16/15 у ламп и скребка). Провалились **тики**:
у трёх устройств (`BlockMagicBox`, `BlockMirror`, верхняя половина тауматориума) в 1.21.1 нет
`BlockEntityTicker`, поэтому меню открывается, а тайл не работает. Это и есть тот случай, который
надо закрывать в первую очередь.

## Что именно не сходится (по каждому классу с native_coverage != parity_verified)

### Машины и VisNet — тайлы

| класс | расхождение | оригинал | порт |
|---|---|---|---|
| `TileMagicBox` | блок не регистрирует тикер → `refreshLinks()` не вызывается, цепочка ящиков не образуется, инвентарь остаётся 27 слотов одного ящика | `oracle thaumcraft/common/tiles/TileMagicBox.java:165` `update()` → `refreshLinks()` при `linkedBoxes < 0`; `:44` размер = `27 * (linkedBoxes + 1)` | `native common/src/main/java/thaumcraft/common/blocks/BlockMagicBox.java:1` (нет `getTicker`); `native .../tiles/TileMagicBox.java:121` `tick()` есть, но не вызывается |
| `BlockMagicBox` | то же самое на стороне блока (открытие меню, дроп содержимого) | `oracle thaumcraft/common/blocks/BlockMagicBox.java:1` | `native .../blocks/BlockMagicBox.java:1` |
| `TileMirror` | блок зеркала без тикера: не выбрасывает предметы, не гасит нестабильность, не чинит битую связь | `oracle thaumcraft/common/tiles/TileMirror.java:287` `update()` (eject + `checkInstability` + `restoreLink`) | `native .../tiles/TileMirror.java:183` `tick()` есть, `native .../blocks/BlockMirror.java:1` тикера нет |
| `TileMirror` | потеряна оплата нестабильности висом ORDER | `oracle thaumcraft/common/tiles/TileMirror.java:314` `VisNetHandler.drainVis(..., Aspect.ORDER, min(instability,1))` | `native .../tiles/TileMirror.java:200` `checkInstability` только уменьшает счётчик |
| `TileMirror` | предмет из зеркала подбирается через 40 тиков вместо 20 | `oracle thaumcraft/common/tiles/TileMirror.java:222` `ie2.field_71088_bW = 20` | `native .../tiles/TileMirror.java:168` `setDefaultPickUpDelay()` = 40 |
| `TileMirror` | при пересылке предмета оригинал делает обновление блока, порт играет выдуманный звук beacon | `oracle thaumcraft/common/tiles/TileMirror.java:191` `func_147452_c(...)` | `native .../tiles/TileMirror.java:143` `playSound(BEACON_AMBIENT)` |
| `TileMirrorEssentia` | зеркало эссенции больше не источник эссенции: тянет из трубной сети близнеца | `oracle thaumcraft/common/tiles/TileMirrorEssentia.java:198` `EssentiaHandler.drainEssentia(te, tag, linkedFacing, 8, true)`; класс `implements IAspectSource` | `native .../tiles/TileMirrorEssentia.java:19` класс не реализует `IAspectSource`; `:136` `takeEssentia` двигает собственную пару «аспект+количество» |
| `TileMirrorEssentia` | нет тикера → битая связь эссенциального зеркала не восстанавливается | `oracle thaumcraft/common/tiles/TileMirrorEssentia.java:227` | `native .../tiles/TileMirrorEssentia.java:161` `tick()` есть, но верхний/нижний блок зеркала тикера не имеет |
| `TileFluxScrubber` | скребок никогда не тянет виз → `power` не растёт, после перезагрузки мира машина не работает | `oracle thaumcraft/common/tiles/TileFluxScrubber.java:58` `power += VisNetHandler.drainVis(..., Aspect.AIR, 10)` при `power < 5` | `native .../tiles/TileFluxScrubber.java:40` тик только списывает заряды и чистит флюкс |
| `TileFluxScrubber` | радиус чистки больше оригинала: оригинал берёт только блоки с квадратом расстояния < 256 | `oracle thaumcraft/common/tiles/TileFluxScrubber.java:92` `func_145835_a(...) < distance*distance` при `distance = 16` | `native .../tiles/TileFluxScrubber.java:56` обход всего куба ±16 без фильтра расстояния |
| `TileFluxScrubber` | не `IEssentiaTransport` → собранная эссенция не уходит в трубы | `oracle thaumcraft/common/tiles/TileFluxScrubber.java:31` `implements IEssentiaTransport`, `:183` `takeEssentia` | `native .../tiles/TileFluxScrubber.java:19` класс без интерфейса, `takeEssentia(int)` никем не вызывается |
| `TileArcaneBore` | клиентское событие копания (`digX/digY/digZ`, `digBlock`, `toDig`) не перенесено — остался только вычисляемый на клиенте `visualTarget` | `oracle thaumcraft/common/tiles/TileArcaneBore.java:695` `getDigEvent(int)`; `oracle thaumcraft/common/lib/network/misc/PacketBoreDig.java:53` | `native .../tiles/TileArcaneBore.java:86` только `visualTarget` |
| `TileArcaneLampFertility` / `TileArcaneLampGrowth` | тик есть (общий `TileEssentiaLamp::tick` → `tickLamp()`), но сверка шла по константам класса, а не построчно по всему порядку обновления оригинала | `oracle thaumcraft/common/tiles/TileArcaneLampFertility.java:1` (`update()` отсутствует — лампы обновляет `TileArcaneLamp`), `oracle thaumcraft/common/tiles/TileArcaneLampGrowth.java:1` | `native .../blocks/BlockEssentiaLamp.java:42` оба варианта тикаются через `TileEssentiaLamp::tick`; `native .../tiles/TileEssentiaLamp.java:48` `tickLamp()` |
| `TileArcaneLampLight` | отдельного тайла света в порту нет: правило переехало в `LegacyTileRules.lampLightExpires` и в `ArcaneLampLighting`, владелец мигания — другой | `oracle thaumcraft/common/tiles/TileArcaneLampLight.java:30` (проверка каждые 100 тиков) | `native .../lib/tiles/LegacyTileRules.java:184` `LAMP_LIGHT_CHECK_PERIOD = 100` |
| `TileArcanePressurePlate` | правило владения переехало в `LegacyTileRules`, отдельного `update()` нет ни в оригинале, ни в порту | `oracle thaumcraft/common/tiles/TileArcanePressurePlate.java:16` | `native .../lib/tiles/LegacyTileRules.java:275` `ownedAccess(...)` |
| `TileBanner`, `TileManaPod`, `TileNitor` | классы перенесены как 1.21.1-блок-сущности без собственного тика (в оригинале `update()` тоже нет), но у натора зарегистрирован **только клиентский** тикер — серверная выработка эссенции не подтверждена | `oracle thaumcraft/common/tiles/TileNitor.java:18` `update()` тянет эссенцию из виз-сети | `native .../blocks/BlockNitor.java:42` `level.isClientSide ? TileNitor::clientTick : null` |
| `TileNodeStabilizer` | перенесён только базовый стабилизатор (metadata 9 → lock 1); продвинутый (metadata 10 → lock 2) отсутствует, значение lock 2 не порождается | `oracle thaumcraft/common/tiles/TileNodeStabilizer.java:24` `lock = metadata == 9 ? 1 : 2`; `oracle thaumcraft/common/tiles/TileNode.java:836` metadata 10 → `nodeLock = 2` | `native .../tiles/TileNodeStabilizer.java:20` `getLock()` всегда 1; `native .../tiles/TileNode.java:498` только lock 1 |
| `TileThaumatoriumTop` | оригинал ломает осиротевшую верхнюю половину; порт тикера верхнему блоку не даёт и вместо этого превращает структуру в «алхимический конструктор» | `oracle thaumcraft/common/tiles/TileThaumatoriumTop.java:36` `func_72921_c(x,y,z,9,3)` | `native .../blocks/BlockThaumatorium.java:41` (у верхнего блока тикера нет); `:56` `neighborChanged` → `ALCHEMICAL_CONSTRUCT` |
| `TileThaumcraftInventory` | общий 1.7-тайл инвентаря разделён на специализированные классы | `oracle thaumcraft/common/tiles/TileThaumcraftInventory.java:1` | `native .../lib/essentia/NativeThaumatoriumInventory.java:1` |

### Меню, контейнеры, слоты

| класс | расхождение | оригинал | порт |
|---|---|---|---|
| `ContainerGhostSlots` | **дефект**: клик предметом, отличным от превью, меняет только количество превью, но не сам предмет | `oracle thaumcraft/common/container/ContainerGhostSlots.java:93-95` `slot2.set(playerStack.copy())` (копия взятого стака) | `native .../container/ContainerGhostSlots.java:81` `slot.set(current.copyWithCount(count))` — предмет остаётся прежним; правило `DeviceContainerRules.ghostClickCount` (`native .../lib/crafting/DeviceContainerRules.java:134-136`) при этом документировано как «a different item replaces the ghost with the whole held stack» |
| `ContainerGhostSlots` | не перенесены режимы клика `mod==2` (хотбар), `mod==3` (креатив/середина) и `mod==6` (масс-раздача) оригинала; порт отказывает во всех прочих `ClickType` | `oracle thaumcraft/common/container/ContainerGhostSlots.java:151,191,214` | `native .../container/ContainerGhostSlots.java:97` `else { return; }` |
| `ContainerHandMirror` / `InventoryHandMirror` / `ItemHandMirror` | транспорт предмета в связанное зеркало не работает: нативный предмет зеркала не реализует `ContainerHandMirror.Bridge`, а зеркало (вид gui 16) не перенесено вовсе | `oracle thaumcraft/common/container/ContainerHandMirror.java:55-66`; `oracle thaumcraft/common/items/relics/ItemHandMirror.java:140` `openGui(...,16,...)`, `:157` `transport(...)` | `native .../container/ContainerHandMirror.java:28` интерфейс `Bridge` (реализатора нет); `native .../items/relics/ItemHandMirror.java:71` `use(...)` только чистит «мёртвую» ссылку |
| `ContainerMagicBox` | размер меню привязан к `getContainerSize()`, а тот без тикера всегда 27 | `oracle thaumcraft/common/container/ContainerMagicBox.java:44` | `native .../container/ContainerMagicBox.java:1` + `native .../tiles/TileMagicBox.java:45` |
| `InventoryFake` | оригинал стреляет Forge-событием крафта игрока с фейковым инвентарём; в 1.21.1 такого события нет — порт зовёт свой хук | `oracle thaumcraft/common/container/InventoryFake.java:1`, `oracle thaumcraft/common/tiles/TileThaumatorium.java:241` | `native .../container/InventoryFake.java:1`, `native .../tiles/TileThaumatorium.java:170` `WarpEvents.onCrafted(...)` |
| `SlotGhostFluid` | оригинал принимает любой предмет из реестра Forge `FluidContainerRegistry`; порт — только `BucketItem` | `oracle thaumcraft/common/container/SlotGhostFluid.java:30` `FluidContainerRegistry.isContainer(...)` | `native .../container/SlotGhostFluid.java:20` `stack.getItem() instanceof BucketItem` |
| `EntityItemGrate` | предикат «это решётка» никогда не устанавливается (`setGrateTest` не имеет вызывающего кода), и решётка роняет обычный `ItemEntity` — предмет не выталкивается из фильтра | `oracle thaumcraft/common/entities/EntityItemGrate.java:26` `pushOutOfBlocks` для metadata 5/6 | `native .../entities/EntityItemGrate.java:49` `setGrateTest(...)` без вызовов; `native .../tiles/TileGrate.java:40` `new ItemEntity(...)` |
| `TileGrate` | то же следствие: оригинал роняет `EntityItemGrate`, порт — обычный предмет | `oracle thaumcraft/common/tiles/TileGrate.java:43` | `native .../tiles/TileGrate.java:40` |

### Блоки

| класс | расхождение | оригинал | порт |
|---|---|---|---|
| `BlockMirror` | тикера нет (см. `TileMirror`) | `oracle thaumcraft/common/blocks/BlockMirror.java:1` | `native .../blocks/BlockMirror.java:1` |
| `BlockMagicBox` | тикера нет (см. `TileMagicBox`) | `oracle thaumcraft/common/blocks/BlockMagicBox.java:1` | `native .../blocks/BlockMagicBox.java:1` |
| `BlockMirrorItem` | исходный класс 1.7 стал связкой «предмет + data component»; зеркало при этом не тикает | `oracle thaumcraft/common/blocks/BlockMirrorItem.java:1` | `native .../items/relics/ItemHandMirror.java:33` `MirrorLink`; `native .../blocks/BlockMirror.java:110` `CustomData` |

### Клиент (частицы, рендереры, экраны)

| класс | расхождение | оригинал | порт |
|---|---|---|---|
| `TileThaumatoriumRenderer` | тауматориум рисуется простым блок-боксом; исходная OBJ-геометрия не рисуется, а загрузчик `ModelThaumatorium` в порту не используется | `oracle thaumcraft/client/renderers/tile/TileThaumatoriumRenderer.java:25` `textures/models/thaumatorium.obj` | `native common/src/main/resources/assets/thaumcraft/models/block/thaumatorium.json:1` (`minecraft:block/orientable`); `native client/renderers/models/ModelThaumatorium.java:29` — класс без ссылок |
| `TileMirrorRenderer` | вид связанного зеркала (gui 16) не перенесён | `oracle thaumcraft/client/renderers/tile/TileMirrorRenderer.java:1` | `native client/renderers/tile/TileMirrorRenderer.java:43` рисует только два вида зеркал |
| `FXBoreSparkle` | искра копания «1 из 10» в порту отсутствует (эффекты инфузии перенесены) | `oracle thaumcraft/client/ClientProxy.java:866` `boreDigFx` | `native client/fx/particles/InfusionParticle.java:31`; `native client/BoreClientEffects.java:54` — только цель копания |
| `FXSpark`, `FXSparkle`, `FXSparkleTrail` | общие 1.7-хелперы частиц расщеплены на несколько нативных классов; параметры «мультипликатор/режим смешивания/цвет» единым нативным классом не сохранились | `oracle thaumcraft/client/fx/particles/FXSparkle.java:23` | `native client/fx/particles/CrystalSparkParticle.java:1`, `FocusFrostSparkleParticle.java:1`, `HungryNodeFragment.java:1` |
| `ItemTrunkSpawnerRenderer` | отдельного рендерера предмета нет | `oracle thaumcraft/client/renderers/item/ItemTrunkSpawnerRenderer.java:1` | `native client/renderers/item/JarItemRenderer.java:1` — общий путь 1.21.1 |
| `PacketFXBlockSparkle` | единого пакета «искра в блоке» нет: нативных payload'ов по семействам эффектов несколько | `oracle thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java:1` | `native .../lib/network/VisNetFxNetworking.java:56`, `.../ExcavationFxNetworking.java:36` |
| `PacketBoreDig` | отдельного пакета копания нет — состояние синхронизируется как клиентский `visualTarget` | `oracle thaumcraft/common/lib/network/misc/PacketBoreDig.java:53` | `native .../tiles/TileArcaneBore.java:799` |
| `ItemSpawnerEgg` | выбор моба метаданными оригинала заменён реестром 1.21.1 | `oracle thaumcraft/common/entities/ItemSpawnerEgg.java:1` | `native .../entities/ItemSpawnerEgg.java:1` (`SpawnEggItem`) |
| `TileFluxScrubberRenderer` | анимации чистки не рисуется | `oracle thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java:1` | `native client/renderers/tile/TileFluxScrubberRenderer.java:1` |

### Классы, исключённые как легаси-only (в оракуле 4.2.3.5 их нет)

`AlchemyFurnaceBakedModel`, `ArcaneFurnaceBakedModel`, `BlockWardedRenderer`, `WardedGlassBakedModel`,
`TrunkSpawnerPerspectiveModel`, `ModelBoreEmit`, `ModelFluxScrubber`, `ModelNodeStabilizer`,
`ModelThaumatorium`, `TileArcanePressurePlateRenderer`, `TileSensorRenderer`,
`ItemBlocks/BlockArcaneFurnaceItem`, `ItemBlocks/BlockMirrorItem` — все присутствуют только в
`src/main/java` (1.12 FOREA), в оракуле отсутствуют; `disposition: exclude`,
`callsite_audit: complete`, доказательство — отсутствие класса в оракуле.

`SlotLimitedByItemstack` — единственный класс из оракула, исключённый как мёртвый код: в оракуле
нет ни одного места, конструирующего его (`grep -rn SlotLimitedByItemstack` по оракулу даёт только
собственное определение `thaumcraft/common/container/SlotLimitedByItemstack.java:19`); правило
«совпадение стака» сохранено в `native .../lib/crafting/DeviceContainerRules.java:150`.

## Отклонения, требующие решения

1. **Зеркало (предметное) — тик.** Оригинал делает X: `oracle thaumcraft/common/tiles/TileMirror.java:287`
   в `update()` выбрасывает предмет из выходного списка, гасит нестабильность и восстанавливает битую
   связь. Порт делает Y: `native .../tiles/TileMirror.java:183` содержит тот же `tick()`, но
   `native .../blocks/BlockMirror.java:1` не возвращает `BlockEntityTicker`, поэтому `tick()` не
   вызывается никогда. Следствие: предмет, попавший в зеркало, остаётся в его буфере навсегда,
   нестабильность не гаснет, связь не чинится.
2. **Зеркало — оплата нестабильности висом.** Оригинал: `oracle .../TileMirror.java:314` тянет
   `Aspect.ORDER` из виз-сети (до `min(instability,1)` за вызов). Порт: `native .../TileMirror.java:200`
   не обращается к виз-сети вовсе — нестабильность гасится только на 1 единицу каждые 20 тиков.
3. **Зеркало эссенции — источник эссенции.** Оригинал: `oracle .../TileMirrorEssentia.java:198`
   тянет эссенцию из трубной сети близнеца и отдаёт в свою (класс `implements IAspectSource`).
   Порт: `native .../TileMirrorEssentia.java:19` интерфейс не реализован, `:136` двигает собственную
   пару «аспект+количество»; трубная сеть не участвует.
4. **Скребок флюкса — питание висом.** Оригинал: `oracle .../TileFluxScrubber.java:58`
   `power += drainVis(..., Aspect.AIR, 10)`. Порт: `native .../TileFluxScrubber.java:40` виз не
   запрашивает; константа `FLUX_POWER_DRAIN = 10` (`native .../lib/world/EldritchBlockRules.java:791`)
   объявлена, но не используется. После перезагрузки мира скребок не работает.
5. **Скребок флюкса — радиус.** Оригинал: `oracle .../TileFluxScrubber.java:92` — только флюкс с
   квадратом расстояния < 256 (радиус 16). Порт: `native .../TileFluxScrubber.java:56` — весь куб
   ±16 (до 27.7 блока), фильтра расстояния нет.
6. **Скребок флюкса — выдача эссенции.** Оригинал: `oracle .../TileFluxScrubber.java:31` — интерфейс
   `IEssentiaTransport`, `:183` `takeEssentia`. Порт: `native .../TileFluxScrubber.java:19` — без
   интерфейса, эссенция не уходит в трубы.
7. **Магический ящик — связывание.** Оригинал: `oracle .../TileMagicBox.java:165` связывает
   соседние ящики при `linkedBoxes < 0`, размер = `27 * (linkedBoxes + 1)`. Порт:
   `native .../tiles/TileMagicBox.java:121` содержит `tick()`/`refreshLinks()`, но
   `native .../blocks/BlockMagicBox.java:1` тикера не даёт; `native .../tiles/TileMagicBox.java:42`
   дополнительно зажимает `linkedBoxes` через `Math.max(0, linkedBoxes)`, поэтому даже при
   сохранённом в NBT «linked» поведение иное.
8. **Слот-превью — смена предмета.** Оригинал: `oracle .../ContainerGhostSlots.java:93-95` кладёт в
   превью копию взятого стака. Порт: `native .../ContainerGhostSlots.java:81` сохраняет прежний
   предмет и меняет только количество.
9. **Слот-превью — режимы клика.** Оригинал: `oracle .../ContainerGhostSlots.java:151,191,214`
   (хотбар, креатив, масс-раздача). Порт: `native .../ContainerGhostSlots.java:97` любой иной
   `ClickType` отвергается.
10. **Зеркало предметов — задержка подбора.** Оригинал: `oracle .../TileMirror.java:222` — 20 тиков.
    Порт: `native .../TileMirror.java:168` — ванильные 40 тиков.
11. **Зеркало предметов — звук пересылки.** Оригинал: `oracle .../TileMirror.java:191` — обновление
    блока, звука нет. Порт: `native .../TileMirror.java:143` — `SoundEvents.BEACON_AMBIENT`
    (не документировано как отклонение).
12. **Решётка — выталкивание предмета.** Оригинал: `oracle .../TileGrate.java:43` роняет
    `EntityItemGrate`, который не может остаться в блоке-решётке (`oracle .../EntityItemGrate.java:26`).
    Порт: `native .../tiles/TileGrate.java:40` роняет обычный `ItemEntity`, а
    `native .../entities/EntityItemGrate.java:49` `setGrateTest` никем не вызывается.
13. **Продвинутый стабилизатор узла.** Оригинал: `oracle .../TileNodeStabilizer.java:24`
    (metadata 9 → lock 1, metadata 10 → lock 2) и `oracle .../TileNode.java:836`.
    Порт: `native .../TileNodeStabilizer.java:20` — только lock 1; значение 2 не порождается,
    продвинутый вариант отсутствует (это же признано в
    `native .../lib/research/ResearchRecipeCoverage.java:457`).
14. **Верхняя половина тауматориума.** Оригинал: `oracle .../TileThaumatoriumTop.java:36` ломает
    осиротевшую половину. Порт: `native .../blocks/BlockThaumatorium.java:55` вместо этого
    превращает структуру в «алхимический конструктор» (согласовано с тем, что в 1.21.1 тауматориум
    — два настоящих блока, а не конструктор).
15. **Ручное зеркало.** Оригинал: `oracle .../ItemHandMirror.java:140` открывает вид зеркала (gui 16),
    `:157` — статический транспорт. Порт: `native .../items/relics/ItemHandMirror.java:71` при
    «живой» ссылке не делает ничего; интерфейс `Bridge` (`native .../container/ContainerHandMirror.java:28`)
    не реализован ни одним классом.
16. **Натор.** Оригинал: `oracle .../TileNitor.java:18` в `update()` тянет эссенцию из виз-сети.
    Порт: `native .../blocks/BlockNitor.java:42` регистрирует только клиентский тикер; серверная
    выработка в этом аудите не подтверждена.

## Дефекты

Воспроизводимые дефекты, найденные в порту (порт в этом аудите **не правился**):

1. **`BlockMagicBox` без `BlockEntityTicker` → магический ящик не связывается и не имеет слотов.**
   Шаги: поставить два магических ящика рядом, открыть правый. Меню показывает 27 слотов одного
   ящика; содержимое левого ящика не видно и не переносится; после перезагрузки мира — то же
   (в NBT `linked` не записывается, `linkedBoxes` остаётся `-1`). Ожидалось по оригиналу
   (`oracle .../TileMagicBox.java:45,165`) — 54 слота и общий буфер.
2. **`BlockMirror` без `BlockEntityTicker` → предметное и эссенциальное зеркала не работают.**
   Шаги: связать два зеркала, бросить предмет в одно. Предмет попадает во внутренний буфер
   (`native .../tiles/TileMirror.java:138 addStack`), но никогда не выбрасывается наружу; при
   `instability > 0` счётчик не гаснет (гасится только `checkInstability`, но она не вызывается);
   разрыв связи не чинится (`inc` не растёт). Ожидалось по оригиналу
   (`oracle .../TileMirror.java:197-212,287-306`).
3. **`TileMirror` потерял оплату нестабильности висом ORDER** (`oracle .../TileMirror.java:314`).
   Воспроизведение: положить зеркало в сеть с источником ORDER, накопить нестабильность (пересылка
   предметов) — виз ORDER не списывается, нестабильность не гасится.
4. **`TileMirrorEssentia` больше не `IAspectSource`** → эссенция не перетекает между зеркалами и
   не уходит в трубы (`oracle .../TileMirrorEssentia.java:31,198`).
5. **`TileFluxScrubber` не берёт виз** (`oracle .../TileFluxScrubber.java:58`) → после перезагрузки
   мира скребок не чистит флюкс, а при включённой сети без источника AIR не работает вовсе.
6. **`TileFluxScrubber` чистит флюкс дальше оригинального радиуса** (куб ±16 вместо квадрата
   расстояния < 256).
7. **`TileFluxScrubber` не отдаёт эссенцию** (нет `IEssentiaTransport`).
8. **`ContainerGhostSlots` не меняет предмет превью** (`oracle .../ContainerGhostSlots.java:93-95`).
   Шаги: открыть конфигурацию голема, взять в руку другой предмет, кликнуть по слоту-превью —
   оригинал кладёт в превью взятый предмет, порт меняет только число.
9. **`EntityItemGrate` инертен**: `setGrateTest` (`native .../entities/EntityItemGrate.java:49`) не
   имеет вызывающего кода, решётка роняет обычный `ItemEntity` → предмет не выталкивается из
   блока-фильтра, как в оригинале (`oracle .../TileGrate.java:43`).
10. **Ручное зеркало не открывает вид зеркала и не переносит предметы** (нет `Bridge`, нет gui 16).
11. **Верхняя половина тауматориума не ломается** при осиротевании (заменено на «алхимический
    конструктор» — это осознанная адаптация, но требует решения владельца).

## Не перенесено

- 14 классов, существующих только в легаси 1.12 (в оракуле 4.2.3.5 их нет) — см. список выше;
  обязательства порта они не несут, но `ArcaneFurnaceBakedModel` и `WardedGlassBakedModel` в порту
  продублированы собственными классами.
- `SlotLimitedByItemstack` — мёртвый код оригинала, исключён осознанно.
- Вид связанного зеркала (gui 16) и статический `ItemHandMirror.transport` — не перенесены.
- Продвинутый стабилизатор узла (metadata 10, lock 2) — не перенесён.
- Клиентское событие копания бура (`digX/digY/digZ`, `digBlock`, `toDig`, `PacketBoreDig`) — не
  перенесено, остался только клиентский `visualTarget`.
- Общие 1.7-частицы `FXSpark`/`FXSparkle`/`FXSparkleTrail` как единые классы с параметрами
  «мультипликатор/режим смешивания/замедление» — не перенесены (расщеплены на нативные классы).
- Искра копания бура «1 из 10» (`FXBoreSparkle` в `boreDigFx`) — не перенесена.
- Геометрия тауматориума из `textures/models/thaumatorium.obj` — не рисуется (боксовая модель);
  класс `ModelThaumatorium` в порту не используется.
- Серверная выработка эссенции натором — не подтверждена (только клиентский тикер).

## Что я НЕ смог проверить

- Ничего не запускалось: ни `:common:compileJava`, ни игрового прогона. Все вердикты — по чтению
  кода; заявленные дефекты воспроизводятся логически (по коду тика и по отсутствию
  `BlockEntityTicker`), а не наблюдением в игре.
- Клиентская графика (частицы, рендереры, экраны) проверялась структурно: наличие класса,
  регистрация, сохранённые константы. Визуального сравнения с оригиналом не было.
- `TileArcaneBore` (806 строк оригинала) и `BoreWork` проверены по точкам входа/выхода, а не
  построчно; полная арифметика рытья, износа и ремонта не сверялась.
- Семейства `TileAlchemyFurnaceAdvanced*`, `TileBellows`, `TileCrucible`, `TileAlembic`,
  `TileInfusionMatrix`, `TileTube*`, `TileTubeFilter`, `TileEssentiaReservoir`, `TileJar`,
  `TileNitor`/`TileNode` в мой worklist **не входят** (они отнесены к другим семействам) — их
  поведение я не аудировал, кроме перечисленных перекрёстных ссылок.
- Точная семантика `AbstractContainerMenu`/`Slot` в 1.21.1 (в частности обработка `ClickType.SWAP`
  и `THROW` ванилью) сверялась по API, а не по декомпиляции ванили: предполагалось, что порт
  перехватывает клики по ghost-слотам до `super.clicked`.
- Аспектные цвета, порядок примитивов и `getAspectsSorted()` оригинала сверены только там, где
  порт на них опирается (suction тауматориума, пульс реле, кристаллизатор).
- `TileNitor`, `TileBanner`, `TileManaPod`, `TileArcanePressurePlate` проверены на отсутствие
  `update()` в оригинале и на регистрацию тикера в порту, но не построчно.
