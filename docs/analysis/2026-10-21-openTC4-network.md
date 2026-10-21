# Сеть TC4 4.2.3.5 → Minecraft 1.21.1: инвентарь оригинальных пакетов и сверка порта

Дата: 2026-10-21. Ветка: `port/ocnetwork` (база `5c80bed`).
Оракул: `/home/Acak/work/thaumcraft-4.2.3.5-oracle` (CFR 0.152 декомпиляция
`Thaumcraft-1.7.10-4.2.3.5.jar`), справочный читаемый checkout 1.7.10:
`/home/Acak/work/opentc4/src/main/java/thaumcraft/...` (тот же код в читаемом виде).
Порт: `common/src/main/java/thaumcraft/common/lib/network/**`, `common/src/main/java/thaumcraft/client/**`,
`fabric/src`, `neoforge/src`.

Метод: полный перечень 39 регистраций `oracle/thaumcraft/common/lib/network/PacketHandler.java:58-96`,
построчное чтение всех 38 классов оракула и всех 32 payload'ов порта, сверка полей,
семантики и проверок прав; прогон `:common:test` (реальный вывод ниже).

**Ограничения этого отчёта.** Клиент не запускался, мультиплеерная приёмка не выполнялась,
dedicated server smoke в этом разделе не проводился. Выводы о поведении — статический анализ
кода плюс исполненные unit-тесты кодеков. Клиентская/мультиплеерная приёмка здесь не заявляется.

---

## 1. Полный инвентарь оригинальных пакетов

Оракул регистрирует **39 записей** на единственном simple-канале `"thaumcraft"`
(`PacketHandler.java:54`); уникальных классов 38, потому что `PacketNote` зарегистрирован
в обе стороны (дискриминаторы 18 и 21). В 1.7.10 адресация — числовой дискриминатор
(`registerMessage(..., idx++, Side)`), порядок регистрации значим.

### 1.1 Serverbound (оригинал) — 8 классов

| # | Класс (оракул) | Поля | Проверки на сервере | Действие при получении |
|---|---|---|---|---|
| 12 | `playerdata/PacketScannedToServer.java:77` | `playerid`, `dim`, `type`, `id`, `md`, `entityid`, `phenomena`, `prefix` | только `DimensionManager.getWorld(dim) != null`; **отправитель не сверяется**, клиент присылает сам результат сканирования | `ScanManager.completeScan(player, new ScanResult(...), prefix)` |
| 13 | `playerdata/PacketAspectCombinationToServer.java:90` | `dim`, `playerid`, `x/y/z`, `aspect1`, `aspect2`, `ab1`, `ab2` | `playerid` отправителя == `playerid` пакета; оба аспекта не null; `poolFor(a1) > 0 \|\| ab1` и то же для `a2` | списание 1 RP с пула игрока (или из `bonusAspects` стола, если пула нет), `PacketAspectPool` в обе стороны, `ScanManager.checkAndSyncAspectKnowledge(combo, 1)`, `scheduleSave` |
| 14 | `playerdata/PacketPlayerCompleteToServer.java:76` | `key`, `dim`, `username`, `type` (0 — купить, 1 — выдать записку) | `username` отправителя == `username` пакета; `!isResearchComplete`; `doesPlayerHaveRequisites` | type 0: списание `rr.tags` из пула, `PacketAspectPool` на каждый аспект, `PacketResearchComplete(key)`, `completeResearch`, затем **прямые siblings** с собственными `PacketResearchComplete`; type 1: `createResearchNoteForPlayer` (бумага + 1 чернило); звук `thaumcraft:learn` |
| 15 | `playerdata/PacketAspectPlaceToServer.java:79` | `dim`, `playerid`, `x/y/z`, `aspect` (может быть null), `q`, `r` | `playerid` отправителя == `playerid` пакета; позиция — **произвольная**, берётся из пакета | `TileResearchTable.placeAspect(q, r, aspect, player)`: списывает чернило со стола, кладёт аспект в клетку или стирает клетку (с шансом возврата 1 RP), `-1` RP у игрока либо из `bonusAspects` |
| 21 | `misc/PacketNote.java:76` (SERVER) | `x/y/z`, `dim`, `note` | только `note == -1` и `DimensionManager.getWorld(dim) != null`; **позиция и измерение — из пакета** | читает тон `TileEntityNote`/`TileSensor` по произвольным координатам и рассылает `PacketNote` всем игрокам в радиусе 8 |
| 22 | `misc/PacketItemKeyToServer.java:59` | `dim`, `playerid`, `key` (0 — колокольчик, 1 — жезл/лопата) | `playerid` **не сверяется**; читается только предмет в руке отправителя | key 0 → `ItemGolemBell.resetMarkers`; key 1 → `WandManager.toggleMisc` (жезл) либо `ItemElementalShovel.setOrientation(o+1)` (лопата) |
| 23 | `misc/PacketFocusChangeToServer.java:59` | `dim`, `playerid`, `focus` (строка-тег либо `"REMOVE"`) | `playerid` отправителя == `playerid` пакета; в руке `ItemWandCasting` и не скипетр | `WandManager.changeFocus(stack, world, player, focus)`: выбор фокуса по тегу из инвентаря и подсумков, возврат старого фокуса в подсумок/инвентарь |
| 24 | `misc/PacketFlyToServer.java:44` | `playerid`, `hover` | **никаких проверок**; `Hover.setHover(playerid, hover)` пишет в глобальную карту по чужому id | состояние hover меняется для указанного entity id |

### 1.2 Clientbound (оригинал) — 31 запись / 30 уникальных классов

| # | Класс (оракул) | Поля | Действие при получении |
|---|---|---|---|
| 0 | `misc/PacketBiomeChange.java` | `x`, `z`, `biome` (short) | `Utils.setBiomeAt(clientWorld, x, z, biome)` — клиентская переопределение биома |
| 1 | `misc/PacketConfig.java` | 5 boolean + `researchDifficulty` (byte) + `aspectTotalCap` (int) | запись серверных настроек в клиентский `Config` |
| 2 | `misc/PacketMiscEvent.java` | `type` (0 warp, 1/2 fog) | виньетка warp + звук сердца, либо длительность тумана |
| 3 | `playerdata/PacketSyncWipe.java` | — | полный сброс клиентских знаний |
| 4 | `playerdata/PacketSyncAspects.java` | список `(tag, amount)` | полная замена открытых аспектов/пула на клиенте |
| 5 | `playerdata/PacketSyncResearch.java` | список ключей | полная замена завершённых исследований |
| 6 | `playerdata/PacketSyncScannedItems.java` | список ключей | полная замена просканированных предметов |
| 7 | `playerdata/PacketSyncScannedEntities.java` | список ключей | полная замена просканированных сущностей |
| 8 | `playerdata/PacketSyncScannedPhenomena.java` | список ключей | полная замена просканированных явлений |
| 9 | `playerdata/PacketResearchComplete.java` | `key` | `completeResearch` на клиенте; для `@KEY` — уведомление «clue», иначе — попап и подсветка в браузере |
| 10 | `playerdata/PacketAspectPool.java` | `key`, `amount` (short), `total` (short) | `setAspectPool(total)`; при `amount > 0` — уведомление «+N аспекта» и звук орба |
| 11 | `playerdata/PacketAspectDiscovery.java` | `key` | `addDiscoveredAspect`, уведомление, звук, подсветка вкладок ASPECTS/BASICS |
| 16 | `playerdata/PacketRunicCharge.java` | `id`, `amount` (short), `max` (short) | `runicCharge`/`runicInfo` клиентского HUD |
| 17 | `misc/PacketBoreDig.java` | `x/y/z`, `digloc` | клиентский тайл бура получает событие копания (частицы FXBoreParticles) |
| 18 | `misc/PacketNote.java` (CLIENT) | `x/y/z`, `dim`, `note` | тон note block или `TileSensor` на клиенте |
| 19 | `playerdata/PacketSyncWarp.java` | `data` (int), `type` (0 perm / 1 sticky / 2 temp) | полная замена одного из трёх счётчиков warp |
| 20 | `playerdata/PacketWarpMessage.java` | `data` (int, дельта), `type` (byte) | уведомление о warp; звук `thaumcraft:whispers` |
| 25 | `fx/PacketFXBlockBubble.java` | `x/y/z`, `color` | пузыри тигля |
| 26 | `fx/PacketFXBlockDig.java` | `x/y/z`, `bi`, `md`, `dx/dy/dz` | частицы и звук копания блока/предмета |
| 27 | `fx/PacketFXBlockSparkle.java` | `x/y/z`, `color` | `blockSparkle` (7 частиц) |
| 28 | `fx/PacketFXBlockArc.java` | `x/y/z`, `entityId` | дуга от сущности к координатам |
| 29 | `fx/PacketFXBlockZap.java` | 6 float | молния между координатами + звук |
| 30 | `fx/PacketFXEssentiaSource.java` | `x/y/z`, `dx/dy/dz`, `color` | подсветка источника эссенции |
| 31 | `fx/PacketFXInfusionSource.java` | `x/y/z`, `dx/dy/dz`, `entityId` | эффект источника матрицы инфузии |
| 32 | `fx/PacketFXShield.java` | 2 entity id | эффект рунического щита |
| 33 | `fx/PacketFXSonic.java` | entity id | звуковой эффект стража |
| 34 | `fx/PacketFXWispZap.java` | 2 entity id | молния между сущностями (висп) |
| 35 | `fx/PacketFXZap.java` | 2 entity id | молния между сущностями (фокус шока) |
| 36 | `fx/PacketFXVisDrain.java` | `x/y/z` (short), `color` (byte — **индекс в списке примальных аспектов**), `dx/dy/dz` | искры vis drain; декодер читает `Aspect.getPrimalAspects().get(color)` без проверки диапазона |
| 37 | `fx/PacketFXBeamPulse.java` | 2 entity id | луч фокуса раскопок |
| 38 | `fx/PacketFXBeamPulseGolemBoss.java` | 2 entity id | луч босса-golem |

### 1.3 События жизненного цикла соединения (оригинал)

`network/EventHandlerNetwork.java`:
- `:43-58` `playerLoggedInEvent` — при входе игрока сервер отправляет 9 пакетов:
  `SyncWipe`, `SyncResearch`, `SyncScannedItems`, `SyncScannedEntities`, `SyncScannedPhenomena`,
  `SyncAspects`, `SyncWarp(0)`, `SyncWarp(1)`, `SyncWarp(2)`, затем `PacketConfig`.
- `:61-67` `clientLoggedIn` — сброс `GuiResearchBrowser.completedResearch` клиента.
- `:69-81` `clientLogsOut` — **восстановление клиентских настроек** (`Config.allowCheatSheet`,
  `wardedStone`, `allowMirrors`, `hardNode`, `wuss`, `researchDifficulty`, `aspectTotalCap`
  возвращаются к клиентским значениям по умолчанию).

---

## 2. Соответствие оракул → порт

В 1.21.1 канал определяется **типом payload'а**, а не числовым дискриминатором.
Порт регистрирует 32 идентификатора в пространстве имён `thaumcraft`:
12 serverbound (намерения) и 20 clientbound (эффекты/снимки).
Каждый идентификатор уникален; это зафиксировано новым guard-тестом
`common/src/test/java/thaumcraft/common/lib/network/PayloadRegistryTest.java`.

### 2.1 Serverbound

| Оракул | Порт | Идентификатор | Поля: оракул → порт | Проверки: оракул → порт |
|---|---|---|---|---|
| `PacketScannedToServer` | сканирование без payload | — | `playerid/dim/type/id/md/entityid/phenomena/prefix` | клиент присылал весь результат сканирования, сервер его только переносил → порт вообще не имеет C2S-канала сканирования: цель и её аспекты определяет **сервер** (`ScanManager.completeScan(player, signature, aspects, kind)`, `common/src/main/java/thaumcraft/common/lib/research/ScanManager.java:52-80`). Авторитет усилен, канал исчез |
| `PacketAspectCombinationToServer` | `ResearchTableCombineNetworking` + `ResearchTableCombinePayload` | `thaumcraft:research_table_combine` | `dim/playerid/x/y/z/aspect1/aspect2/ab1/ab2` → `containerId/sessionToken/tableRevision/left/right` | оракул: позиция стола и флаги бонуса из пакета, `playerid` отправителя. Порт: позиция стола берётся **из открытого меню** (`player.containerMenu instanceof ContainerResearchTable`), флаги бонуса — из `table.bonusPools()`, отправитель — из соединения. Требование «оба аспекта оплачиваемы» проверяется до списания (`ResearchAspectCombination.canCombine`, `.../research/ResearchAspectCombination.java:54-62`) |
| `PacketPlayerCompleteToServer` (type 0) | `ResearchPurchaseNetworking` + `ResearchPurchasePayload` | `thaumcraft:research_purchase` | `key/dim/username/type` → `key` | оракул сверял только `username` и **не проверял стоимость** (GUI проверял, сервер — нет). Порт: `ResearchPurchase.validate` (`.../research/ResearchPurchase.java:79-102`) повторно проверяет существование, режим покупки, prerequisites, `@KEY`-clue и **достаточность пула** до любого списания; siblings и warp считаются `ResearchCompletion.apply` |
| `PacketPlayerCompleteToServer` (type 1) | `ResearchNoteRequestNetworking` + `ResearchNoteRequestPayload` | `thaumcraft:research_note_request` | `key/dim/username/type` → `key` | оракул: `createResearchNoteForPlayer` (`oracle/.../research/ResearchManager.java:120-134`) сначала тратит чернило, потом генерирует. Порт (`.../research/ResearchNoteIssuance.java:48-113`) сначала генерирует и проверяет результат, потом тратит — сознательное исправление (чернило не теряется при сбое генерации); бумага только проверяется, но не тратится, как в оригинале |
| `PacketAspectPlaceToServer` | `ResearchTableEditNetworking` + `ResearchTableEditPayload` | `thaumcraft:research_table_edit` | `dim/playerid/x/y/z/aspect/q/r` → `containerId/sessionToken/tableRevision/action/q/r/aspect` | оракул: произвольная позиция стола, аспект из пакета, списание чернил со стола, −1 RP игроку или из бонуса стола, шанс возврата RP при стирании. Порт: `ResearchNoteEditing.evaluate` (`.../research/ResearchNoteEditing.java:101-155`) — чистая функция, которая до любого мутажа проверяет: записку в слоте 1, незавершённость, наличие чернил, тип клетки, что аспект **открыт** игроком, и что его есть **где платить** (пул игрока либо бонус стола). Ролл RNG происходит только после всех проверок; материальный коммит — `TileResearchTable.commitEdit` (`.../tiles/TileResearchTable.java:355-381`) одним блоком без слушателей |
| `PacketNote` (SERVER) | нативный `blockEvent`/`ClientboundBlockEntityDataPacket` | — | `x/y/z/dim/note` → нативный `level.blockEvent(pos, block, tone, note)` и `TileSensor.getUpdatePacket` | произвольный доступ к плитке по координатам из пакета устранён полностью: канал удалён, тон идёт через нативный block event и BE-синхронизацию (`.../tiles/TileSensor.java:44-56`) |
| `PacketItemKeyToServer` (key 0) | `GolemBellNetworking` + `GolemBellResetPayload` | `thaumcraft:golem_bell_reset` | `dim/playerid/key` → `hand` | оракул: любой `playerid`, читается предмет в руке. Порт: только отправитель соединения, только реально **держимый** колокольчик (`ItemGolemBell.clearMarkers(stack, player)`, `.../items/golems/ItemGolemBell.java:137-163`): проверяются рука, предмет, измерение, существование golem-а, право владения (`mayEditLinks`), целостность стека |
| `PacketItemKeyToServer` (key 1, жезл) | `WandArchitectNetworking` + `WandArchitectPayload` | `thaumcraft:architect_toggle` | `dim/playerid/key` → пустой intent | оракул: `WandManager.toggleMisc` (`oracle/.../items/wands/WandManager.java:568-609`) меняет площадь/ось Architect-фокуса. Порт: только отправитель, только **реально держащий** жезл с Architect-фокусом и апгрейдом (`WandArchitectNetworking.handle`), значение пересчитывается сервером через `WandArchitect.Area.next` (та же формула, что в оригинале) |
| `PacketItemKeyToServer` (key 1, лопата) | `ShovelOrientationNetworking` + `ShovelOrientationPayload` | `thaumcraft:shovel_orientation` | то же | оракул: `ItemElementalShovel.setOrientation(o+1)` c `o % 3` (`oracle/.../items/equipment/ItemElementalShovel.java:299-306`). Порт: `ElementalShovelGeometry.next` = `floorMod(o+1, 3)` — идентично |
| `PacketFocusChangeToServer` | `FocusSelectionNetworking` + `FocusSelectionPayload` | `thaumcraft:focus_selection` | `dim/playerid/focus(тег)` → `hand/inventorySlot` (физический адрес) | оракул: тег фокуса, поиск по всему инвентарю и подсумкам. Порт: клиент присылает **физический адрес** (слот 0..35, −1 = «текущий», либо `маркер 127 + ushort` для клетки подсумка), сервер сам достаёт стек, проверяет `IScribeTools`-подобную поддержку фокуса, снимает копию, **возвращает старый фокус только если его куда-то можно положить**, и только потом ставит новый (`FocusSelectionNetworking.select`, `.../network/FocusSelectionNetworking.java:52-140`). Порядок «сначала проверка, потом трата» соблюдён: идентичность всех слотов инвентаря перепроверяется перед коммитом |
| `PacketFlyToServer` | `HoverNetworking` + `HoverTogglePayload` | `thaumcraft:hover_toggle` | `playerid/hover` → пустой intent | оракул: **никаких проверок**, `Hover.setHover` писал в глобальную карту по произвольному entity id (`oracle/.../items/armor/Hover.java:37-47`). Порт: intent без адреса, сервер проверяет отправителя, наличие harness, заряд и горючее (`Hover.toggleHover`, `.../items/armor/Hover.java:53-63`), выдача `mayfly` — нативная, отзыв — по сохранённому кадру прав |

### 2.2 Clientbound

| Оракул | Порт | Идентификатор | Комментарий |
|---|---|---|---|
| `PacketConfig` | `ResearchSettingsPayload` | `thaumcraft:research_settings` | 7 фактов оригинала упакованы в 1 байт флагов + 2 varint; применяется **до** снимка знаний (в оригинале `PacketConfig` уходил последним, `:58` регистрации и `:43-58` отправка) |
| `PacketMiscEvent` | `WarpEventPayload` | `thaumcraft:warp_event` | 3 вида событий оригинала (0/1/2), диапазон проверяется в конструкторе |
| `PacketSyncWipe` + `PacketSyncAspects` + `PacketSyncResearch` + `PacketSyncScannedItems/Entities/Phenomena` + `PacketSyncWarp(0,1,2)` | `KnowledgeSyncPayload` | `thaumcraft:knowledge_sync` | 9 пакетов входа оригинала заменены **одним** нативным NBT-снимком знаний (`PlayerKnowledge.save/load`, `.../research/PlayerKnowledge.java:145-190`), который несёт те же поля: открытые аспекты с пулами, завершённые исследования, три множества просканированного, perm/sticky/temp/counter warp, флаг инициализации прималов. Клиентская сторона — `KnowledgeEvents.initClient` (`.../network/KnowledgeEvents.java:47-56`) |
| `PacketResearchComplete` | `ResearchNoticePayload` | `thaumcraft:research_notice` | различение `@KEY` (clue) и обычного ключа сохранено на клиенте |
| `PacketAspectPool` | `AspectPoolPayload` | `thaumcraft:aspect_pool` | `key/amount/total` — те же три поля; клиент больше **не** применяет `total` как авторитет (снимок знаний — единственный авторитет) |
| `PacketAspectDiscovery` | `AspectDiscoveryPayload` | `thaumcraft:aspect_discovery` | тот же тег аспекта |
| `PacketRunicCharge` | `RunicChargePayload` | `thaumcraft:runic_charge` | `id/amount/max` — те же поля и тот же размер (8 байт) |
| `PacketFXShield` | `RunicHitPayload` | `thaumcraft:runic_hit` | два entity id, включая режимы −1/−2/−3 |
| `PacketFXSonic` | `GuardianSonicPayload` | `thaumcraft:guardian_sonic` | один entity id |
| `PacketFXWispZap` | `WispZapPayload` | `thaumcraft:wisp_zap` | два entity id |
| `PacketFXZap` | `ShockFxPayload` | `thaumcraft:shock_fx` | вместо двух id — геометрия «источник→цель» + seed, что строже |
| `PacketFXVisDrain` | `VisNetFxPayload` | `thaumcraft:vis_net_fx` | **индекс примального аспекта заменён на валидный RGB** и на полные координаты/измерение: оригинальный декодер `Aspect.getPrimalAspects().get(color)` (`oracle/.../fx/PacketFXVisDrain.java:38`) падал на значении вне диапазона |
| `PacketFXInfusionSource` | `InfusionFxPayload` | `thaumcraft:infusion_fx` | матрица/событие/цель, диапазон события проверяется |
| `PacketFXBeamPulse` | `ExcavationFxPayload` | `thaumcraft:excavation_fx` | луч раскопок; сервер шлёт **только после** списания vis (`ItemFocusExcavation.java:141-165`) |
| `PacketFXBlockSparkle` (для лампы роста) | `GrowthLampFxPayload` | `thaumcraft:growth_lamp_fx` | позиция + измерение |
| `PacketNote` (CLIENT) | нативный `blockEvent` + BE-синхронизация | — | канал удалён |
| `PacketBiomeChange` | `BiomeColumns` (нативные колонки биомов) | — | канал удалён: числовой `short`-биом 1.7.10 не переносится в 1.21.1 (`.../lib/world/BiomeColumns.java`) |
| `PacketBoreDig` | — | — | **пробел**, см. §5 |
| `PacketFXBlockBubble`, `PacketFXBlockDig`, `PacketFXBlockArc`, `PacketFXBlockZap`, `PacketFXEssentiaSource`, `PacketFXBeamPulseGolemBoss` | — | — | **пробелы** (клиентские визуальные каналы), см. §5 |

### 2.3 Прочие clientbound-каналы порта, не имеющие оригинального аналога

| Идентификатор | Назначение | Оригинальный аналог |
|---|---|---|
| `thaumcraft:open_thaumonomicon` | сервер авторизует откры Thaumonomicon | `ItemThaumonomicon` открывал GUI напрямую |
| `thaumcraft:accessories_contents` | снимок 4 надетых аксессуаров | Baubles API 1.7.10 |
| `thaumcraft:research_table_state` | поколение меню/сессия/ревизия/бонусы стола | `TileResearchTable.func_147471_g` + `func_70296_d` |
| `thaumcraft:focal_manipulator_ack` | подтверждение/отказ запуска задачи фокальной манипуляции | `ContainerFocalManipulator.func_75140_a` (кнопка) |
| `thaumcraft:focal_manipulator_start` | C2S-запуск задачи | там же; кнопка → intent с контейнером/сессией/ревизией |

---

## 3. Проверка «сначала проверка, потом трата» (критерий 3 задания)

| Обработчик | Что тратится | Где проверяется ДО траты | Коммит |
|---|---|---|---|
| `ResearchPurchaseNetworking.handle` | RP пула | `ResearchPurchase.validate` (`.../research/ResearchPurchase.java:97-101`) — полный preflight цены; списание идёт на **копии** знаний (`purchase`, `:54-70`), коммит — одним `copyFrom` | `ResearchPurchase.purchase` |
| `ResearchNoteRequestNetworking.handle` | 1 чернило (бумага только проверяется) | `ResearchNoteIssuance.plan` (`:144-164`) + `ScribingInk.planFor` (`:80-81`); генерация записки и её валидность — **до** траты чернила (`:64-75`) | `ResearchNoteIssuance.commit` (`:214-221`) |
| `ResearchTableEditNetworking.handle` | чернило стола, 1 RP игрока либо бонус стола | `ResearchNoteEditing.validate` (`.../research/ResearchNoteEditing.java:157-183`): записка, незавершённость, чернило, тип клетки, открыт ли аспект, есть ли чем платить — всё **до** RNG и **до** мутаций | `TileResearchTable.commitEdit` (`:355-381`) одним блоком |
| `ResearchTableCombineNetworking.handle` | 1 RP игрока либо бонус стола | `ResearchAspectCombination.canCombine` (`:54-62`) + последовательное сценирование `debit` на копии (`:72-81`); два одинаковых аспекта считаются как два платежа | `TileResearchTable.commitKnowledgeBonus` (`:313-328`) |
| `ResearchTableCopyNetworking.handle` | RP пула, 1 бумага, 1 чернильный мешок | `ResearchNoteCopying.canPay` (`:14-25`) проверяет **все** аспекты и их цену; бумага/чернило ищутся в инвентаре **до** плана (`:129-133`) | `TileResearchTable.commitCopy` (`:335-352`): `actor.copyFrom` → `paper.shrink(1)` → `inkSac.shrink(1)` → запись заметки |
| `GolemBellNetworking.handleReset` | маркеры golem-а | `ItemGolemBell.clearMarkers` проверяет руку, предмет, измерение, существование golem-а и право владения **до** записи | там же |
| `ShovelOrientationNetworking.handle` | ориентация лопаты | проверяется предмет в **главной** руке (`ItemElementalShovel`) | там же |
| `WandArchitectNetworking.handle` | площадь Architect-фокуса | проверяется жезл, фокус и апгрейд | там же |
| `HoverNetworking` | горючее harness | `Hover.toggleHover` проверяет harness, заряд и горючее до переключения состояния; сама трата горючего — в отдельном серверном тике | `Hover.handleHoverArmor` |
| `FocusSelectionNetworking.select` | предметы (фокусы) | целостность стека, рук, всех слотов инвентаря и надетых аксессуаров перепроверяется **до** любой записи (`:129-137`) | там же |
| `FocalManipulatorNetworking.handle` | ресурсы задачи | `menu.requestStart` сверяет контейнер, сессию, ревизию и `stillValid`; задача запускается только после этого | `TileFocalManipulator.startCraft` |

**Вывод по критерию 3:** нарушений «сначала проверка, потом трата» в C2S-обработчиках порта
не найдено. Ни один обработчик не принимает от клиента ни позицию мира, ни предмет, ни цену,
ни баланс: все адреса (меню, стол, golem, игрок) выводятся сервером.

Дополнительно: ни один C2S-payload порта не содержит `playerid`, `dim` или координат мира,
то есть все три «дыры» оригинала (`PacketScannedToServer` — подмена результата сканирования,
`PacketItemKeyToServer` — чужой `playerid`, `PacketFlyToServer` — чужой `playerid`,
`PacketNote`(SERVER) — произвольная плитка, `PacketAspectPlaceToServer`/`PacketAspectCombinationToServer` —
произвольная позиция стола) в порту закрыты архитектурно, а не проверкой.

---

## 4. Регистрация и идентификаторы (критерий 4)

* 1.7.10: `SimpleNetworkWrapper` на канале `"thaumcraft"`, 39 числовых дискриминаторов, порядок регистрации значим.
* 1.21.1: канал определяется `CustomPacketPayload.Type`; идентификатор — `ResourceLocation`.
  Порт: 32 идентификатора, все в пространстве имён `thaumcraft`, все уникальны, C2S и S2C не пересекаются.
* Регистрация S2C-типов выполняется на сервере (`NetworkManager.registerS2CPayloadType` под
  `Platform.getEnvironment() == Env.SERVER`), приёмники — на клиенте (`NetworkManager.registerReceiver(Side.S2C, …)`);
  в интегрированном сервере обе стороны живут в одной JVM, регистрация общая.
  C2S-приёмники регистрируются в общем `Thaumcraft.init()`, поэтому тип C2S зарегистрирован и у клиента
  (нужно для кодирования), и у сервера (нужно для декодирования).
* Порядок регистрации S2C-типов на сервере (по `common/src/main/java/thaumcraft/common/Thaumcraft.java`):
  `research_settings` (:68) → `warp_message` (:69) → `aspect_pool`/`aspect_discovery`/`research_notice` (:70) →
  `runic_charge`/`runic_hit` (:88) → `accessories_contents` (:89) → `growth_lamp_fx` (:138) →
  `warp_event`/`guardian_sonic` (:145) → `wisp_zap` (:146) → `knowledge_sync`/`open_thaumonomicon` (:166) →
  `research_table_state` (:169) → `focal_manipulator_ack` (:175) → `infusion_fx` (:176) →
  `shock_fx` (:177) → `excavation_fx` (:178) → `vis_net_fx` (:179).
  C2S: `accessories_open` (:89) → `hover_toggle` (:92) → `architect_toggle` (:154) →
  `research_purchase` (:167) → `research_note_request` (:168) → `research_table_edit` (:169) →
  `research_table_combine` (:170) → `research_table_copy` (:171) → `focus_selection` (:172) →
  `golem_bell_reset` (:173) → `shovel_orientation` (:174) → `focal_manipulator_start` (:175).
  Порядок в 1.21.1 не влияет на адресацию (её задаёт `Type`), но фиксирован, чтобы порядок
  синхронизации входа был воспроизводим: настройки исследования уходят раньше снимка знаний.
* Новый guard-тест `PayloadRegistryTest` фиксирует: уникальность id, раздельность направлений,
  точный набор id, каноничность написания (`[a-z0-9_]+`), пространство имён.
  Это эквивалент оригинальной проверки числа регистраций (`PacketHandler.java:98-100` в 1.12-порте),
  перенесённый на идентификаторы вместо дискриминаторов.

---

## 5. Клиент-серверная синхронизация состояний (критерий 5)

| Состояние | Оригинал | Порт | Синхронизируется при |
|---|---|---|---|
| Знания игрока (аспекты, пулы, исследования, просканированное) | 7 пакетов `Sync*` | один `KnowledgeSyncPayload` | вход, респав, смена измерения (`KnowledgeEvents.initServer`, `.../network/KnowledgeEvents.java:27-38`) |
| Warp perm/sticky/temp/counter | 3 пакета `SyncWarp` | в `KnowledgeSyncPayload` | вместе со знаниями |
| Настройки исследования (cheat sheet, warded stone, mirrors, hard nodes, wuss, difficulty, aspect cap) | `PacketConfig` | `ResearchSettingsPayload` | вход игрока (`ResearchSettingsNetworking.initServer`) |
| Пул аспекта (дельта+итог) | `PacketAspectPool` | `AspectPoolPayload` | после каждого изменения пула |
| Открытие аспекта | `PacketAspectDiscovery` | `AspectDiscoveryPayload` | после открытия |
| Завершение исследования | `PacketResearchComplete` | `ResearchNoticePayload` | после покупки/выдачи |
| Warp-уведомления | `PacketWarpMessage` | `WarpMessagePayload` | после начисления warp |
| Бонусы стола, ревизия, сессия | `TileResearchTable.func_147471_g` | `ResearchTableStatePayload` | при каждом изменении материала стола |
| Аксессуары (4 слота) | Baubles API | `AccessoryContentsPayload` | при изменении надетого набора |
| Runiс-заряд HUD | `PacketRunicCharge` | `RunicChargePayload` | при изменении заряда |
| Надетые аксессуары/слот-синк | ванильные слоты | ванильные слоты | нативно |

Известные асимметрии, найденные сверкой:

* **A-1 (исправлено).** Восстановление клиентских настроек при отключении было реализовано только
  в Fabric-загрузчике, а в общем слое отсутствовало; NeoForge-клиент его не выполнял вовсе.
* **A-2 (пробел, вне зоны).** Сброс клиентских HUD-состояний (warp, runic, уведомления, travel gear,
  taint spores) при отключении регистрируется только в `fabric/.../ThaumcraftFabricClient.java:64-76`;
  NeoForge-клиент его не делает. Это клиентский слой, не сеть: правку оставляю владельцу клиентских
  задач, чтобы не пересечься с ветками `port/guiclients`/`port/fixclient`.

---

## 6. Находки и исправления

### D-1 (средняя, межзагрузчиковая асимметрия) — клиентские настройки исследования не восстанавливаются при отключении на NeoForge

* **Оракул:** `thaumcraft/common/lib/network/EventHandlerNetwork.java:69-81` — `clientLogsOut`
  восстанавливает `Config.allowCheatSheet/wardedStone/allowMirrors/hardNode/wuss/researchDifficulty/aspectTotalCap`
  к клиентским значениям при разрыве соединения.
* **Порт (было):** `ThaumcraftSettings.resetClient()` (`common/src/main/java/thaumcraft/common/config/ThaumcraftSettings.java:32`)
  существовал, но **не вызывался нигде в общем слое**; единственный вызов был в
  `fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java:75` (блок `ClientPlayConnectionEvents.DISCONNECT`).
  `neoforge/src/main/java/thaumcraft/neoforge/ThaumcraftNeoForge.java:16` подключает только `ClientSetup::init`,
  то есть NeoForge-клиент после выхода с сервера сохранял в статическом `ThaumcraftSettings.remote`
  настройки последнего сервера (`difficulty`, `aspectCap`, `cheatSheet`, `wardedStone`, `mirrors`, `wuss`).
  Эти значения читаются клиентским кодом: `GuiResearchBrowser.java:169` (что покупается),
  `ThaumcraftContent.java:193` (выдача читерского тауматоника), `BlockArcaneDoor.java:73`,
  `BlockArcanePressurePlate.java:70`, `BlockCosmeticOpaque.java:74` (зачарованный камень/зеркала),
  `ThaumcraftHungryNodeClient.java:37` (отрисовка узлов), `ResearchConditions.java:34-35`.
* **Фикс:** восстановление перенесено в общий сетевой слой — `ResearchSettingsNetworking.initClient()`
  (`common/src/main/java/thaumcraft/common/lib/network/ResearchSettingsNetworking.java:29`) регистрирует
  `ClientPlayerEvent.CLIENT_PLAYER_QUIT` → `ThaumcraftSettings.resetClient()`; событие срабатывает и в Fabric
  (`MixinMinecraft.disconnect`), и в NeoForge (`EventHandlerImplClient`, `LoggingOut`).
  `ThaumcraftSettings.resetClient()` (`.../config/ThaumcraftSettings.java:37-41`) теперь допускает
  ещё не инициализированный клиентский поток (нечего восстанавливать), но по-прежнему требует
  клиентский поток, если он уже установлен.
  Вызов в Fabric-загрузчике оставлен как дублирующий (идемпотентный) путь.
* **Проверка:** `:common:test` — см. §8; отдельного функционального теста клиентского disconnect
  нет, поведение подтверждено статически (общий слой теперь регистрирует восстановление сам).

### D-2 (покрытие, не дефект) — у кадровых payload'ов исследовательского стола не было тестов кодеков

`ResearchTableEditPayload`, `ResearchTableCombinePayload`, `ResearchTableStatePayload` — самые сложные
кодекы порта (кадр с префиксом-длиной, лимит байтов, канонические теги аспектов, защита от
усечения/хвоста/переполнения) — не имели ни одного теста, хотя соседние payload'ы имели.
Добавлены `ResearchTableEditPayloadTest` (10 тестов), `ResearchTableCombinePayloadTest` (6),
`ResearchTableStatePayloadTest` (7) — `common/src/test/java/thaumcraft/common/lib/network/`.

### D-3 (покрытие, не дефект) — не было guard-теста набора идентификаторов payload'ов

Добавлен `PayloadRegistryTest` (6 тестов) — `common/src/test/java/thaumcraft/common/lib/network/`.

### D-4 (покрытие, не дефект) — каналы «клавиши предмета» не были покрыты кодек-тестами

Оригинальный `PacketItemKeyToServer` был расщеплён на три канала; добавлен `ItemKeyPayloadsTest`
(6 тестов) на `HoverTogglePayload`, `ShovelOrientationPayload`, `GolemBellResetPayload`.

### Наблюдения без правки (сознательные решения порта)

* `ResearchSettingsPayload` принимает `difficulty`/`aspectCap` во всём диапазоне `int`.
  Это намеренно: существующий тест `ResearchSettingsPayloadTest.allFlagsAndSignedIntegerBoundariesRoundTripWithoutTheLegacyDifficultyByteTruncation`
  фиксирует отказ от усечения оригинального `byte` (`PacketConfig.toBytes` писал `researchDifficulty` байтом).
  Ограничение диапазона было бы регрессией относительно этого решения.
* `ExcavationFxPayload.crackStage` намеренно не ограничен сверху: порт передаёт «родную» долю прогресса,
  которая может превысить 9 после замены блока на более мягкий (`ItemFocusExcavation.java:136-138`),
  и нативный `destroyBlockProgress` такую трещину снимает.
* Мёртвая перегрузка `ItemGolemBell.clearMarkers(ItemStack)` (`.../items/golems/ItemGolemBell.java:131-133`)
  возвращает `false` и нигде не используется. Удаление не производилось, чтобы не менять публичную
  поверхность без нужды; отмечено здесь.
* Оригинальный `PacketFXVisDrain` декодировал индекс примального аспекта без проверки диапазона
  (`oracle/.../fx/PacketFXVisDrain.java:38`) — потенциальный клиентский crash. Порт этого кода не несёт
  (RGB валиден в конструкторе `VisNetFxPayload`).

---

## 7. Пробелы (что не закрыто)

1. **Клиентские визуальные каналы, не перенесённые в 1.21.1** (геймплей есть, эффекта нет):
   `PacketBoreDig` (частицы копания бура), `PacketFXBlockBubble` (пузыри тигля, `ItemElementalAxe`),
   `PacketFXBlockArc` (`EntityEldritchWarden`, `EntityCultistPortal`), `PacketFXBlockZap`
   (`TileNode`, `TileEldritchTrap`, `TileInfusionMatrix`), `PacketFXEssentiaSource` (`EssentiaHandler`),
   `PacketFXBeamPulseGolemBoss`. Это клиентский слой; в этом отчёте они зафиксированы, но не исправлялись.
2. **A-2:** сброс клиентских HUD-состояний при отключении есть только в Fabric-загрузчике.
3. Клиент не запускался; мультиплеерная приёмка, проверка «двух игроков» и реал-серверный прогон
   C2S-каналов в этом разделе **не выполнялись**. Unit-тесты покрывают кодек и чистую логику,
   но не диспетчеризацию Architectury в живом соединении.
4. NeoForge-модуль в этом разделе не собирался и не тестировался (по `AGENTS.md` приоритет — Fabric).

---

## 8. Проверка

Команда (как предписано заданием), выполненная с принудительным перезапуском задачи,
чтобы вывод был реальным, а не `FROM-CACHE`:

```
export JAVA_HOME=/home/Acak/.jdk21
export PATH=$JAVA_HOME/bin:$PATH
export GRADLE_USER_HOME=/home/Acak/gradle-homes/ocnetwork
./gradlew --no-daemon --console=plain :common:cleanTest :common:test --rerun-tasks
```

Реальный вывод (хвост):

```
> Task :common:cleanTest

> Task :common:compileJava
/home/Acak/tcw/ocnetwork/common/src/main/java/thaumcraft/common/mixin/ResearchNoteStackCodecMixin.java:13: warning: Unable to locate method mapping for @At(INVOKE.<target>) 'Lcom/mojang/serialization/Codec;lazyInitialized(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;'
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;lazyInitialized(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;", ordinal = 0))
    ^
Note: Some input files use or override a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
Note: Some input files use unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
1 warning

> Task :common:processResources
> Task :common:classes

> Task :common:compileTestJava
Note: /home/Acak/tcw/ocnetwork/common/src/test/java/thaumcraft/essentia/BoreInventoryPlanTest.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.

> Task :common:processTestResources
> Task :common:testClasses
> Task :common:test

[Incubating] Problems report is available at: file:///home/Acak/tcw/ocnetwork/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 38s
6 actionable tasks: 6 executed
```

Итог по XML-отчётам `common/build/test-results/test/TEST-*.xml`
(245 классов, агрегация атрибутов `tests`/`skipped`/`failures`/`errors`):

```
classes=245 tests=1700 skipped=0 failures=0 errors=0
```

Отдельно — сетевой пакет `thaumcraft.common.lib.network.*` (14 классов, 70 тестов):

```
thaumcraft.common.lib.network.AccessoryPayloadsCodecTest               tests=6  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.FocusSelectionPayloadTest                tests=7  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.FxPayloadsCodecTest                      tests=5  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.ItemKeyPayloadsTest                      tests=6  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.KnowledgeSyncPayloadTest                 tests=1  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.PayloadRegistryTest                      tests=6  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.ResearchNoteRequestPayloadTest           tests=3  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.ResearchPurchasePayloadTest              tests=3  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.ResearchSettingsPayloadTest              tests=2  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.ResearchTableCombinePayloadTest          tests=6  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.ResearchTableCopyPayloadTest             tests=3  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.ResearchTableEditPayloadTest             tests=10 skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.ResearchTableStatePayloadTest            tests=7  skipped=0 failures=0 errors=0
thaumcraft.common.lib.network.WarpPayloadsCodecTest                    tests=5  skipped=0 failures=0 errors=0
network package total: 70
```

Что эти тесты покрывают и что не покрывают: покрыты **кодек и чистая логика**
(границы кадров, отказ от усечения/хвоста/переполнения, каноничность идентификаторов и тегов
аспектов, набор и уникальность id каналов, границы доменов настроек). **Не покрыты**
диспетчеризация Architectury в живом соединении, реальный вход/выход игрока и
мультиплеерное поведение — для них нужен клиент/сервер, который здесь не запускался.

---

## 9. Итог

* Все 39 регистраций оригинала (38 уникальных классов) разобраны и сопоставлены с 32 идентификаторами порта.
* C2S-границы порта строже оригинальных: ни один клиентский запрос не несёт игрока, измерения,
  позиции мира, предмета, цены или баланса.
* Порядок «сначала проверка, потом трата» соблюдён во всех 12 C2S-обработчиках (таблица §3).
* Исправлен один межзагрузчиковый дефект (**D-1**): восстановление клиентских настроек при отключении
  перенесено в общий сетевой слой, NeoForge-клиент больше не сохраняет настройки чужого сервера.
* Добавлено 35 новых тестов (5 новых классов) на кодек-и идентификаторы сетевого слоя:
  `ResearchTableEditPayloadTest` (10), `ResearchTableStatePayloadTest` (7), `ItemKeyPayloadsTest` (6),
  `ResearchTableCombinePayloadTest` (6), `PayloadRegistryTest` (6).
* Пробелы: 6 оригинальных клиентских визуальных каналов и клиентский HUD-сброс на NeoForge (§7);
  клиентская и мультиплеерная приёмка не заявляются.
