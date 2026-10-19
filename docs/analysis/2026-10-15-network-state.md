# Анализ сети, серверных границ и данных игрока

Дата: 2026-10-15. Анализируемый commit: `4bc68454b1063cdc1255f833dd537e0fd4fc341d`.

Это статический аудит текущего Forge 1.12.2 порта. Production-код не изменялся. Сборка, JUnit, игровой клиент и dedicated server в рамках этого раздела не запускались. Указания `AGENTS.md`, `build.gradle`, `Dockerfile` прочитаны; `.opencode/goal.md` отсутствовал. `git status --short` до работы не показал изменений.

Оригинал сравнивался по исходникам, полученным CFR 0.152 из поставленного `Thaumcraft-1.7.10-4.2.3.5.jar`. Обозначение `oracle/...` ниже соответствует каталогу `C:/Users/Acak/Documents/Codex/2026-10-15/new-chat/work/thaumcraft-4.2.3.5-oracle/`. В `thaumcraft_src/` в этой копии находятся ресурсы, а не Java-исходники. Сравнение относится к поведению и API; миграция сохранений 1.7.10 не проверялась и не является требованием проекта.

## Вывод

Серверные запросы исследований, аспектов, предметных клавиш, фокусов и полёта сейчас значительно строже оригинала: используется отправитель `MessageContext`, операции перенесены на основной поток, проверяются текущий мир, инструмент или контейнер. Эти проверки блокируют неподтверждённые мутации, но table routing делает world lookup до отклонения чужой позиции. Ранее записанные в старом аудите уязвимости скрытого исследования и удаления фиксированных узлов заметки уже закрыты в текущих исходниках.

Остаются одна существенная граница доверия (`PacketNote` с произвольными координатами/измерением), ошибка разделения client/server состояния в integrated server и несколько дефектов снимков/кэша. Последствия загрузки чанков, гонок и игровых повторов требуют отдельных runtime-проверок; они не выдаются здесь за воспроизведённые падения.

## Карта протокола: проверены все 39 регистраций

Таблица регистраций: `src/main/java/thaumcraft/common/lib/network/PacketHandler.java:59-97`. Всего 39 discriminator entries: 8 serverbound и 31 clientbound. `PacketNote` зарегистрирован в обе стороны, поэтому уникальных классов 38. Защита количества записей на `:98-100` проверяет только число, не корректность payload/обработки.

| ID | Направление | Пакет | Поведение получателя |
|---|---|---|---|
| 0 | Client | PacketBiomeChange | Обновление биома клиентского мира |
| 1 | Client | PacketConfig | Серверные настройки в клиентском Config |
| 2 | Client | PacketMiscEvent | Warp vignette, heartbeat, fog duration |
| 3 | Client | PacketSyncWipe | Сброс клиентского capability перед полным sync |
| 4 | Client | PacketSyncAspects | Полная замена открытых аспектов/пула |
| 5 | Client | PacketSyncResearch | Полная замена завершённых исследований |
| 6 | Client | PacketSyncScannedItems | Полная замена просканированных предметов |
| 7 | Client | PacketSyncScannedEntities | Полная замена просканированных сущностей |
| 8 | Client | PacketSyncScannedPhenomena | Полная замена просканированных явлений |
| 9 | Client | PacketResearchComplete | Добавление завершённого исследования и UI feedback |
| 10 | Client | PacketAspectPool | Авторитетное total пула и feedback о приросте |
| 11 | Client | PacketAspectDiscovery | Открытие аспекта и UI feedback |
| 12 | Server | PacketScannedToServer | Проверка инструмента/реальной цели и завершение сканирования |
| 13 | Server | PacketAspectCombinationToServer | Оплата двух входных аспектов и комбинация |
| 14 | Server | PacketPlayerCompleteToServer | Покупка вторичного исследования или выдача заметки |
| 15 | Server | PacketAspectPlaceToServer | Размещение/удаление аспекта в заметке стола |
| 16 | Client | PacketRunicCharge | Обновление runicCharge/runicInfo для entity id |
| 17 | Client | PacketBoreDig | Событие анимации бура в соответствующей tile |
| 18 | Client | PacketNote | Обновление тона note block/sensor |
| 19 | Client | PacketSyncWarp | Полная замена perm/sticky/temp/counter |
| 20 | Client | PacketWarpMessage | Уведомление и whisper sound |
| 21 | Server | PacketNote | Запрос тона tile и broadcast рядом с ней |
| 22 | Server | PacketItemKeyToServer | Клавиша колокольчика, жезла или лопаты |
| 23 | Server | PacketFocusChangeToServer | Переключение фокуса удерживаемого жезла |
| 24 | Server | PacketFlyToServer | Переключение hover надетого harness |
| 25 | Client | PacketFXBlockBubble | Частицы пузырей |
| 26 | Client | PacketFXBlockDig | Частицы/звук добычи блока или предмета |
| 27 | Client | PacketFXBlockSparkle | Частицы искр |
| 28 | Client | PacketFXBlockArc | Дуга от сущности к координатам |
| 29 | Client | PacketFXBlockZap | Молния между координатами и звук |
| 30 | Client | PacketFXEssentiaSource | Обновление ограниченного временем sourceFX |
| 31 | Client | PacketFXInfusionSource | Обновление sourceFX матрицы инфузии |
| 32 | Client | PacketFXShield | Runic shield эффект по entity ids |
| 33 | Client | PacketFXSonic | Sonic эффект по entity id |
| 34 | Client | PacketFXWispZap | Молния между сущностями |
| 35 | Client | PacketFXZap | Zap эффект между сущностями |
| 36 | Client | PacketFXVisDrain | Эффект vis drain; индекс primal aspect в декодере |
| 37 | Client | PacketFXBeamPulse | Beam pulse между сущностями |
| 38 | Client | PacketFXBeamPulseGolemBoss | Beam pulse босса-гolem |

Каждый clientbound `onMessage` переводит изменение мира/UI на `Thaumcraft.proxy.scheduleClientTask`; реализация `ClientProxy.java:1303-1306` использует `Minecraft.addScheduledTask`. `PacketNote` разделяет обе стороны и также планирует клиентскую работу. Все 8 serverbound обработчиков используют `PacketBase.scheduleServer`, где игрок берётся из соединения (`PacketBase.java:38-45`), а задача ставится через `player.getServerWorld().addScheduledTask`.

## Проверка каждого serverbound обработчика

| Пакет | Серверные проверки | Мутация | Оценка текущей границы |
|---|---|---|---|
| PacketScannedToServer | `:83-87` sender entity id/current dimension; `:103-125` разрешён только prefix `@`, thaumometer в main/offhand, сервер повторно находит цель; `:135-145` сравнивает type/item/meta/entity/phenomena | ScanManager, затем sync capability | Подмена удалённого предмета/сущности и чужого игрока перекрыта. Продолжительность не проверяется, NS-07 |
| PacketAspectCombinationToServer | `:90-97` sender/dimension, два существующих аспекта, активный совпадающий usable ContainerResearchTable; `:118-125` открытие обоих аспектов и полный preflight затрат; `:168-177` повторяющийся аспект учитывается дважды | Пул/bonus table, discovery результата, dirty/sync/cache | Нет установленного обхода bonus flags/недостаточного пула. Ранний world lookup — NS-01 |
| PacketPlayerCompleteToServer | `:64-86` key, current dimension, username, duplicate, существующее исследование, hidden/lost `@KEY`, requisites, тип и сложность; `:119-128` preflight всех затрат | Пул, research, siblings, warp/callback/sync либо inventory заметки | Закрыты старые hidden/lost и бесплатная direct purchase. Replay feedback остаётся, NS-06 |
| PacketAspectPlaceToServer | `:80-101` sender/dimension, активный research container, тождество live tile, координаты, canInteractWith | TileResearchTable.placeAspect | Проверки hex/aspect/endpoint присутствуют; world lookup выполняется до проверки координат — NS-01 |
| PacketNote | `:66-72` note = -1 и существующий загруженный dimension | Чтение произвольной tile и рассылка её тона | Не проверяет позицию относительно отправителя, NS-01 |
| PacketItemKeyToServer | `:44-52` sender/dimension, удерживаемый предмет, key 0/1 | Markers, misc toggle или NBT ориентации | Подмена игрока/предмета перекрыта. Неизвестная клавиша ничего не делает |
| PacketFocusChangeToServer | `:44-56` sender/dimension, mainhand wand, не sceptre, не открыт ContainerFocusPouch | WandManager.changeFocus по реально имеющимся inventory/pouch фокусам | Payload не создаёт произвольный ItemStack; ресурс разрешается сервером |
| PacketFlyToServer | `:39-45` sender entity id, CHEST harness; Hover.setHover проверяет energy jar | Hover map + harness NBT, последующая armour tick управляет flight | Чужой entity id и полёт без harness перекрыты; отключение собственного hover разрешено |

Все пути рассмотрены как статические. Полную проверку item/tile gameplay выполняют соседние отчёты; здесь не заявляется runtime-доказательство безопасности этих подсистем.

## Находки

### NS-01 — Произвольный доступ к tile через PacketNote и ранний table lookup

Приоритет: высокий для multiplayer availability. Подтверждённая часть: отсутствие авторизации координат и измерения. Возможная загрузка/генерация чанков и тяжесть DoS требуют runtime-проверки.

`PacketNote.java:66-72` принимает от любого подключённого игрока `dim,x,y,z` и вызывает `DimensionManager.getWorld(dim).getTileEntity(pos)`. Нет сравнения dimension игрока, расстояния, высоты/world border, `isBlockLoaded`, активного GUI или rate limit. Проверка `note == -1` ограничивает вид запроса, но не область доступа. Ответ рассылается всем игрокам возле заданной tile (`:79-82`), а не только отправителю.

Конкретный trigger: клиент в обычном мире посылает запросы тона для удалённых координат в любом уже загруженном сервером измерении. Сервер гарантированно выполняет tile lookup по этим координатам; при стандартной реализации 1.12.2 lookup может подтянуть отсутствующий чанк. Повторение по новым координатам является правдоподобным путём расходования main-thread времени/world I/O. Здесь не утверждается, что это уже воспроизведённая генерация или падение сервера.

Вторая точка с тем же риском: `PacketAspectPlaceToServer.resolveResearchTable` (`:97`) выполняет `player.world.getTileEntity(pos)` до `pos.equals(researchTable.getPos())` на `:98` и `canInteractWith` на `:101`. Если клиент открыл любой research table, valid sender/dimension и произвольная позиция вызовут lookup, после чего мутация будет отвергнута. Этот resolver используется и Combination (`PacketAspectCombinationToServer.java:93`). Проверка равенства должна предшествовать world lookup: наличие корректного контейнера не авторизует чтение произвольного чанка.

Оригинал: `oracle/thaumcraft/common/lib/network/misc/PacketNote.java:86-103` использует те же пользовательские dimension/координаты без проверки отправителя. Это унаследованная проблема; scheduling в порте устраняет сетевой поток, но не границу доверия. `PacketNoteStaticGuardTest.java:28-42` проверяет наличие старого lookup/broadcast, не авторизацию.

Рекомендация: запросы только в мире отправителя, в допустимом радиусе и уже загруженном чанке; ответ инициатору; ограничение частоты. В table resolver сначала проверить равенство позиции и допустимость доступа, затем обращаться к миру. Проверить запрос из другого мира/незагруженного чанка так, чтобы он не вызвал chunk provider.

### NS-02 — Клиентский API в integrated server выбирает серверный capability

Приоритет: средний. Статически подтверждён ошибочный выбор стороны; конкретное падение от гонки не воспроизведено.

`GuiResearchTable.java:81-83` в клиентском GUI вызывает `ResearchManager.isResearchComplete(player.getName(), ...)`. `ResearchManager.getResearchData(String)` на `:209-218` сначала ищет живого server player, затем только client capability. В одиночной игре integrated server существует в том же JVM, поэтому `findPlayer` (`:924-935`) возвращает `EntityPlayerMP`, несмотря на вызов с client thread. Перед возвратом `:213` запускает `updateCache`; `:245,:884-886` выполняют полную NBT-сериализацию server capability.

NBT-сериализация на `PlayerKnowledgeCapability.java:377,389,414` перебирает mutable collections, которые server thread одновременно меняет при сканировании/исследовании. Так возникают cross-thread чтение состояния и concurrent writes общего `HashMap` кэша. Возможные следствия — несовместимый snapshot или `ConcurrentModificationException`; оба требуют controlled integrated-server race test. Удалённый клиент не получает прямого API-доступа к чужому server capability из этой находки.

Рекомендация: выбирать client capability по фактической стороне до server player lookup; клиентским запросам не сериализовать/изменять server cache. Cache snapshots создавать на server thread. Не заменять только HashMap на concurrent map: это не решает сериализацию живых HashSet.

### NS-03 — Кэш исследований не привязан к миру и не сбрасывается

Приоритет: средний для правильности API. Статически подтверждён, runtime переход между мирами не проверялся.

`ResearchManager.java:53` содержит process-wide `Map<String,IPlayerKnowledge>`; ключ нормализуется только по имени (`:940-944`). `getResearchData` на `:221-226` возвращает существующий snapshot до чтения актуального world/playerdata. Во всём дереве нет удаления/очистки `playerDataCache`; `Thaumcraft.serverStopping` (`Thaumcraft.java:222-225`) занимается только прикреплёнными крабами.

Trigger: в integrated server войти в мир A с LAN-игроком Bob, сохранить snapshot Bob, закрыть мир, открыть другой свежий мир B и обратиться к name-based research API для offline Bob до его подключения. Возвращается snapshot из A даже при отсутствии playerdata в B. Online Bob получает свой live capability, поэтому это не доказательство переноса постоянного прогресса при обычном login. Однако addon/offline проверки видят неверные данные и кэш удерживает всех ранее просмотренных игроков.

Рекомендация: world/server-scoped cache, UUID identity, очистка на остановке/смене мира, явная политика offline entries. Проверить два свежих 1.12.2 мира без каких-либо legacy saves.

### NS-04 — Копирование через save NBT удаляет auto-unlock состояние из кэша

Приоритет: низкий; нарушена согласованность public cache API, установленного встроенного потребителя `getResearchForPlayerSafe` не найдено.

`updateCache` (`ResearchManager.java:242-245`) делает `copyKnowledge` через serialize/deserialize (`:884-886`). `PlayerKnowledgeCapability.writeResearchSet` на `:395` намеренно не сохраняет auto-unlock исследования. Такое исключение корректно для disk persistence, где `EventHandlerEntity.grantAutoUnlockResearch` (`:753-770`) восстанавливает их при load/clone. Для in-memory snapshot восстановление не вызывается. `getResearchForPlayerSafe` (`ResearchManager.java:351-354`) читает именно этот урезанный snapshot; offline `getResearchData`/`isResearchComplete` также его используют.

Trigger: live capability содержит auto-unlock `ASPECTS` (`ConfigResearchBasics.java:25-40`), выполнить updateCache и getResearchForPlayerSafe — запись пропадёт из возвращаемого списка. Существующий serializer test `PlayerKnowledgeCapabilityTest.researchSerializationFiltersUnknownAutoUnlockAndRedundantClues` подтверждает omission в NBT, но не проверяет cache API.

Оригинал отдельно фильтрует save NBT (`oracle/.../ResearchManager.java:783-801`), а `getResearchForPlayerSafe` (`:424-426`) возвращает полный live completed-research list. В порте объединены два разных назначения одного представления.

Рекомендация: отдельный faithful in-memory copy или восстановление derived state в snapshot. Не возвращать auto-unlock в диск лишь ради исправления кэша.

### NS-05 — Отрицательные/ошибочные clientbound counts не отвергаются

Приоритет: низкий. Это client-state robustness, не server authority exploit.

В `PacketSyncResearch.java:29-33` count — signed int без допустимого диапазона. Аналогично `PacketSyncScannedItems/Entities/Phenomena.java:29-33` и `PacketSyncAspects.java:28-36`. Отрицательный count декодируется как пустая collection; затем client handler очищает предыдущую (`PacketSyncResearch.java:60`, остальные scan packets `:60`) или заменяет aspects. Число записей сверх payload бросает buffer underflow; точная политика disconnect зависит от Forge decoder. Нет выделенной sanity check/atomic decode rejection.

Кроме того, `PacketFXVisDrain.java:38` без проверки использует сетевой int как индекс `Aspect.getPrimalAspects()`. Значение -1 или >= размер списка приводит к `IndexOutOfBoundsException` ещё при decode. Встроенные VisNet producer paths проверены: передают valid primal index (`VisNetHandler.java:80-83,132-134`), поэтому нормальный игровой crash от них здесь не установлен.

Оригинальные PacketSync* использовали signed short counts (`oracle/.../PacketSyncResearch.java:45-56`, соответствующие scan/aspect классы), хотя также не имели явной malformed policy. Порт расширил count до int; совместимость wire protocol с клиентом 1.7.10 не является требованием.

Рекомендация: явные границы коллекций/index, decode полностью до замены состояния, тесты negative/oversized/truncated payload. Не описывать эту находку как возможность обычного игрока стереть серверные исследования: направления регистрации этого не позволяют.

### NS-06 — Повтор запроса существующей заметки повторяет learn sound

Приоритет: низкий, статически подтверждено; live sound spam не воспроизведён.

`ResearchManager.createResearchNoteForPlayer` (`:393-396`) возвращает уже имеющуюся заметку без новой оплаты/выдачи. `PacketPlayerCompleteToServer.java:82-83` считает любую непустую заметку успешным запросом; `:55-57` воспроизводит learn sound при каждом успехе. Trigger — повторять валидный type-1 запрос с существующей несданной заметкой и допустимыми requisites. Дубликат заметки и повторное завершение исследования этим путём не установлены. Оригинал также проигрывал звук после type-1 запроса (`oracle/.../PacketPlayerCompleteToServer.java:99-102`).

Рекомендация: разделять создание новой заметки и idempotent возврат уже существующей; feedback только для нового действия или cooldown.

### NS-07 — Продолжительность сканирования проверяется только клиентом

Приоритет: низкий gameplay authority, статически подтверждено; runtime speed-scan не проверялся.

`ItemThaumometer.java:56` задаёт duration 25; `:81-83` серверную `onUsingTick` обработку пропускает; `:85-92` клиент посылает completion после count <= 5. `PacketScannedToServer.java:83-87,103-125` проверяет текущую серверную цель и удерживаемый thaumometer, но не начало/продолжительность item use и непрерывность удержания одной цели.

Trigger: с thaumometer в руке и корректной текущей raytrace целью немедленно отправить valid completion request, не выдерживая клиентский интервал. Remote impossible target не проходит; ускоряется только обычное доступное сканирование. Оригинальные `ItemThaumometer` и `PacketScannedToServer` также доверяли client timing. При сохранении этого поведения следует явно считать задержку только presentation, иначе вести server scan session с minimum elapsed ticks.

## Данные игрока и NBT свежих миров

Capability прикрепляется к каждому EntityPlayer под ключом `thaumcraft:player_knowledge` (`EventHandlerEntity.java:154-164`); registration/storage делегируют NBT instance (`PlayerKnowledgeProvider.java:48-83`). Сохраняются warp perm/sticky/temp/counter, initializedAspects, discovered aspect amounts, scanned sets, completed research (`PlayerKnowledgeCapability.java:339-354`), deserialize заменяет collections (`:358-372`). Unknown aspect tags пропускаются (`:430-432`). Signed aspect pool/counter намеренно допускаются и имеют тесты; они не объявлены дефектом только из-за отрицательного значения.

Initial primal pools назначаются один раз, 15..19 каждому (`ResearchManager.java:255-265`); initializedAspects записывается в NBT. Clone копирует состояние и восстанавливает derived auto-unlock (`EventHandlerEntity.java:275-292`). Load-from-file также восстанавливает auto-unlock (`:744-770`) без repeat warp/callback; это важно, поскольку serialize его фильтрует. RunicCharge намеренно не сохраняется и обнуляется в deserialize (`PlayerKnowledgeCapability.java:365`); max/runic info берутся из текущего equipment/runtime handler. Это не потеря обязательного сохранённого состояния.

Full sync на server-side join/clone идёт через `EventHandlerEntity.syncAllData` (`:298-320`): config, wipe, aspects, research, scans, warp, runic. Incremental исследования/аспекты отправляются отдельными packets; смена измерения сверяется серверным запросом, а не произвольным DimensionManager lookup за исключением NS-01. Disconnect восстанавливает client config (`EventHandlerNetwork.java:9-20`).

Offline load читает только текущий мир `playerdata/UUID.dat` и его `ForgeCaps/thaumcraft:player_knowledge` (`ResearchManager.java:890-918`). Offline mutations с суффиксом `Unsaved` действительно не пишут файл; такое поведение не объявляется дефектом по названию. Ключевые проблемы этого пути — lifetime/identity кэша и урезанный in-memory snapshot, описанные выше.

## Покрытие тестами и ограничения

Исходники тестов прочитаны, выполнение не заявляется:

- `PacketHandlerRegistrationStaticGuardTest`, `PlayerDataPacketSerializationTest`, `PacketFXSerializationTest` — таблица/valid roundtrip; malformed collection/index cases не подтверждены.
- `PacketScannedAuthorityRuntimeTest`, `ResearchTableAuthorityRuntimeTest`, `PacketPlayerCompleteRuntimeTest`, `PacketFocusChangeAuthorityRuntimeTest` — mocked runtime contracts; проверяют существенные sender/target/container/payment paths. Такие имена не означают real-server validation.
- `PlayerKnowledgeCapabilityTest` — roundtrip, reset, signed pools, omission auto-unlock/unknown/redundant clue, ignored old runicCharge field.
- `PacketNoteStaticGuardTest` — лишь source shape payload/lookup/broadcast; malicious coordinate request не проверяет.
- `EventHandlerEntityCloneStaticGuardTest`, `EventHandlerNetworkContractTest`, packet client-boundary guards — проверки wiring/source, не integrated client/server гонки или переход между мирами.

Старый отчёт `docs/goals/2026-08-03-eldritch-full-parity-audit-fixes/reports/A-024-research-networking.md` прочитан как historical evidence, но результаты перепроверены в текущем commit: F01/F02 закрыты, F03/F04 остаются в узкой описанной форме. Их нельзя автоматически перенести в список актуальных high-severity findings.

Команды: `git status --short`; `Get-Content AGENTS.md/build.gradle/Dockerfile`; `rg --files src/main/java/thaumcraft/common/lib`; `rg -n 'onMessage|scheduleServer|registerMessage' src/main/java/thaumcraft/common/lib/network`; targeted `Get-Content` каждого serverbound и clientbound packet, capability/event/cache и соответствующих оригинальных CFR-файлов. Никаких production edits, builds или installs этим анализом не выполнено. Commit не создан. Runtime smoke для documentation-only diff не требуется; поведенческие гипотезы помечены как непроверенные и нуждаются в будущей отдельной воспроизводимой проверке.
