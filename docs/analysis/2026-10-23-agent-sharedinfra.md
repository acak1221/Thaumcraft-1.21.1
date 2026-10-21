# Shared infrastructure: the 2026-10-23 port wave (`agent/sharedinfra`)

Дата: 2026-10-23. Ветка: `agent/sharedinfra`. Worktree: `/home/cyberfox-kali/wt/sharedinfra`.

Домен: **shared_infrastructure** — 34 открытые единицы ledger (все `partial`).
Оракул: `/home/cyberfox-kali/OpenTC4/src/main/java/thaumcraft/...` (читаемый checkout 1.7.10
4.2.3.5, тот же код, что в приватном оракуле), плюс декомпиляция 1.21.1 из
`~/.gradle/.../minecraft-merged-*.jar` для проверки сигнатур.

**Ограничения волны.** Gradle не запускался (общий Loom-кэш принадлежит лидеру), поэтому
все JUnit-тесты ниже **авторские, но НЕ выполненные**. Клиент не запускался, мультиплеерная
приёмка не выполнялась, dedicated-server smoke не проводился. Все выводы — статический
анализ кода плюс машинное чтение 1.21.1 API.

---

## 1. Итог

| величина | значение |
|---|---|
| единиц в домене | 34 (все были `partial`) |
| переведено в `parity_verified` | **29** |
| оставлено `partial` (реальные пробелы, названы точно) | **5** |
| найдено и исправлено дефектов | **6** |
| исправлено неверных утверждений ledger | **6** |
| авторских тестов (НЕ выполнены) | **18** в 3 новых классах |
| изменённых файлов | 8 (+ этот отчёт) |
| коммитов | 4 |

Единицы, оставленные `partial`, и их точный пробел:

| единица | пробел |
|---|---|
| `thaumcraft/common/CommonProxy.java` | ~40 клиентских эффектов прокси разнесены по отдельным клиентским классам; клиент в этой среде не запускался, визуальный паритет не заявлен |
| `thaumcraft/common/config/Config.java` | набор из ~70 опций оригинала не воспроизведён (воспроизведены исследования, тайнт, биомы) |
| `thaumcraft/common/lib/events/EventHandlerEntity.java` | оригинал писал знания игрока в отдельный файл мира при load/save игрока; порт держит те же факты в родных данных игрока (миграция миров 1.7 не авторизована) |
| `thaumcraft/common/lib/events/EventHandlerWorld.java` | оригинал **отменял установку блока рядом с активным элдритч-боссом** в Outer Lands — в порте такого правила нет; плюс отсутствует очередь регенерации чанков (в 1.21.1 нет аналога события данных чанка) |
| `thaumcraft/common/lib/events/ServerTickEventsFML.java` | очередь регенерации чанков оригинала не воспроизведена (та же причина) |

---

## 2. Дефекты: найдено и исправлено (6)

### D-1 (средняя) — минимальная дистанция выбора цели сравнивалась в квадратах

* **Оригинал:** `OpenTC4/src/main/java/thaumcraft/common/lib/utils/EntityUtils.java:78`
  `!((double) entity.getDistanceToEntity(entityplayer) < minrange)`.
  1.7.10 `Entity.getDistanceToEntity` — это **линейное** расстояние (корень из квадрата).
* **Порт (было):** `common/src/main/java/thaumcraft/common/lib/utils/EntityUtils.java:128`
  `entity.distanceToSqr(entityplayer) < minrange` — квадрат расстояния сравнивался с **самим
  радиусом**.
* **Следствие:** при `minrange = 4` оригинал отбрасывал цель на расстоянии 3, а порт
  отбрасывал только на расстоянии < 2 (9 < 4 — ложь). Любая цель между `sqrt(minrange)` и
  `minrange` выбиралась портом, хотя оригинал её не выбирал.
* **Фикс:** `EntityUtils.java:128` → `belowMinRange(entity.distanceToSqr(entityplayer), minrange)`,
  где `EntityUtils.java:203` `belowMinRange(d2, r) = d2 < r * r` (эквивалент линейного
  сравнения при `r >= 0`).

### D-2 (средняя) — подушка бокса выбора цели бралась из высоты, а не из границы столкновения

* **Оригинал:** `EntityUtils.java:79` и `:112`
  `float f2 = Math.max(0.8F, entity.getCollisionBorderSize());`
  1.7.10 `Entity.getCollisionBorderSize()` = `width * 0.5F`.
* **Порт (было):** `EntityUtils.java:131` и `:168`
  `Math.max(0.8f, entity.getBbHeight())`.
* **Следствие:** у обычного моба (высота 1.8) подушка была 1.8 вместо 0.8 — бокс выбора
  раздувался втрое, и цель «видна» с гораздо большего расстояния. У очень широкой сущности
  (ширина 16) оригинал давал 8.0, порт — 1.8.
* **Фикс:** `EntityUtils.java:131`, `:168` → `pickPadding(entity.getBbWidth())`, где
  `EntityUtils.java:192` `pickPadding(width) = Math.max(0.8f, width * 0.5f)`.

### D-3 (средняя) — конус видимости был привязан к ВЕРХУ bounding box

* **Оригинал:** `EntityUtils.java:199-200` и `:211-212`
  `ent2.boundingBox.minY + (double)(ent2.height / 2.0F)` и
  `ent.boundingBox.minY + (double)ent.getEyeHeight()` — обе точки отсчитываются от **низа**.
* **Порт (было):** `EntityUtils.java:285-286` и `:297-298` использовали
  `getBoundingBox().maxY + ...`.
* **Следствие:** для игрока (рост 1.8) оригинал брал цель на 0.9 над ногами и вершину конуса
  на 1.62 над ногами; порт брал 2.7 и 3.42 — конус целился на **целую высоту сущности выше**.
  `isVisibleTo` — это проверка «объект в зоне видимости» для Warp-целей и компаса (`WarpEvents`,
  `ItemCompassStone`).
* **Фикс:** `EntityUtils.java:322-323` и `:335` → `midBody(...)`/`eyePoint(...)`
  (`EntityUtils.java:213`, `:220`), которые якорятся на `minY`.

### D-4 (высокая) — запрос «жезл в хотбаре с местом» был инвертирован

* **Оригинал:** `InventoryUtils.java:446-464` — обход **девяти** слотов хотбара
  (`InventoryPlayer.getHotbarSize()`), условие
  `wand.addVis(stack, aspect, amount, false) < amount`.
  `ItemWandCasting.addVis` (`ItemWandCasting.java:331-343`) возвращает **остаток** запроса в
  целых визах: `max(storeAmount - maxVis, 0) / 100`, где `storeAmount = getVis + amount * 100`.
* **Порт (было):** `InventoryUtils.java:417-425` — обход всех **36** слотов инвентаря и
  условие `ItemWandCasting.addRealVis(stack, aspect.getTag(), amount, false) > 0`.
* **Следствие:** условие было **противоположным** оригиналу (порт возвращал первый жезл, в
  который виз **не помещался**, вместо первого, в который помещался хотя бы один виз), плюс
  неверный объём обхода (36 вместо 9) и неверная единица (нативный `addRealVis` оперирует
  центовизами, а параметр `amount` в оригинале — целые визы).
* **Фикс:** `InventoryUtils.java:445-462` — обход девяти слотов хотбара
  (`HOTBAR_SIZE = 9`, `InventoryUtils.java:49`) и сравнение остатка в целых визах;
  `InventoryUtils.java:475` `visLeftover(current, max, amount)` воспроизводит формулу
  оригинала в `long`, чтобы большой запрос не переполнялся (в оригинале переполнялся).

### D-5 (средняя) — потеряны два правила сравнения стеков оригинала

* **Оригинал:** `InventoryUtils.java:330-337` и `:366-373` — при `ignoreDamage` разница
  метаданных снималась, если **оба** стека изнашиваемы, **и** метаданные `32767` («любое
  значение») также снимали разницу. `InventoryUtils.java:339` — сравнение для крафта
  дополнительно требовало `stack0.stackSize <= stack0.getMaxStackSize()`.
* **Порт (было):** `InventoryUtils.java:342` и `:382` — ни правила `32767`, ни ограничения
  размера стека не было.
* **Фикс:** `InventoryUtils.java:404` `damageMatches(...)` (с правилом `ANY_DAMAGE = 32767`,
  `InventoryUtils.java:57`) и `InventoryUtils.java:415` `withinStackLimit(...)`; оба
  сравнения используют их (`:349-352`, `:390`).

### D-6 (низкая) — костная мука удобряла уже выросшую культуру

* **Оригинал:** `Utils.java:65-86` `useBonemealAtLoc` отказывал культуре, которая больше не
  может расти (`IGrowable.canGrow`), и отвечал `false`.
* **Порт (было):** `Utils.java:287-297` проверял только `CropUtils.isGrowable`.
* **Фикс:** `Utils.java:293` — добавлена проверка `CropUtils.canGrow(state)`.
  (Потребитель `ItemElementalHoe` реализует ту же проверку через
  `isValidBonemealTarget`, так что геймплей не меняется; починка делает общий помощник
  согласованным с оригиналом.)

---

## 3. Неверные утверждения ledger, исправленные в этой волне (6)

Это не «закрытие пробела», а исправление **неверной оценки**: предыдущая волна записала
пробел, которого в оригинале нет.

1. **`MaterialAiry`** — предыдущая оценка считала пробелом «requires-a-tool флаг оригинального
   материала не имеет нативного аналога». Но оригинал **переопределяет** `setRequiresTool()`
   пустым телом (`OpenTC4/.../config/MaterialAiry.java:31-33`), поэтому виртуальный вызов
   конструктора (`:12`) **не устанавливает** флаг:airy-материал требовал инструмента
   **только формально, а по факту не требовал**. Порт, не объявляющий
   `requiresCorrectToolForDrops`, ведёт себя **точно как оригинал**. Пробел снят.
2. **`RefGui`** — предыдущая оценка утверждала, что «нумерация идентификаторов не
   воспроизведена». Воспроизведена: все 17 констант оригинала (`RefGui.java:3-21`) присутствуют
   с теми же значениями и теми же пропусками (4, 6, 7, 14).
3. **`FakeThaumcraftPlayer`** — «отмена подбора предметов для игроков, чьё имя начинается с
   FakeThaumcraft» — это правило `EventHandlerEntity`, а **не** этого класса; при этом
   оригинальное дерево **никогда не создаёт** `FakeThaumcraftPlayer` (ссылки только на
   `instanceof` в `Thaumcraft.java:297-298` и `:326-327`), то есть у правила нет и в оригинале
   цели.
4. **`EventHandlerNetwork`** — «сброс клиентского кэша исследований не имеет аналога».
   Нативный снимок знаний **заменяет** клиентское состояние при каждом входе
   (`KnowledgeEvents.java:25-35`, `:42-48`) — тот же эффект, что wipe + девять снимков.
5. **`KeyHandler`** — «связывания клавиш не входят в этот аудит». Все три оригинальных
   связывания есть с исходными кодами LWJGL2: **F** (33) — выбор фокуса/сброс колокольчика
   (`ThaumcraftFocusClient.java:23-25`), **G** (34) — переключатель Architect/лопаты
   (`ThaumcraftMiscKeyClient.java:24-25`), **H** (35) — harness парения
   (`ThaumcraftTravelGearClient.java:70`).
6. **`EntityUtils` / `InventoryUtils` / `Utils`** — предыдущая оценка была `parity_verified`,
   хотя в коде были дефекты D-1…D-6. Оценка остаётся `parity_verified`, но теперь в ledger
   записано, что она была **неверной** и что именно исправлено.

---

## 4. Сетевой слой: что проверено в этой волне

Предыдущая волна (`docs/analysis/2026-10-21-openTC4-network.md`) закрыла большую часть
сети; здесь — собственная перепроверка, без повторения её выводов.

* **Поток декодирования.** Ни один из 12 C2S-приёмников не трогает состояние мира на
  сетевом потоке: каждый использует `context.queue(...)`
  (`HoverNetworking.java:11`, `GolemBellNetworking.java:16`, `ShovelOrientationNetworking.java:19`,
  `WandArchitectNetworking.java:20`, `AccessoryNetworking.java:23`,
  `ResearchPurchaseNetworking.java:24`, `ResearchNoteRequestNetworking.java:23`,
  `ResearchTableEditNetworking.java:34`, `ResearchTableCombineNetworking.java:20`,
  `ResearchTableCopyNetworking.java:23`, `FocusSelectionNetworking.java:30`,
  `FocalManipulatorNetworking.java:23`). На Fabric приёмники `ServerPlayNetworking` и так
  приходят на серверный поток, на NeoForge `playToServer` по умолчанию — `HandlerThread.MAIN`;
  `context.queue()` — перестраховка, которая к тому же одинакова на обоих загрузчиках.
* **Проверка серверного потока.** Каждый C2S-обработчик дополнительно требует
  `player.server.isSameThread()` и отказывает мёртвому/наблюдателю/отключённому игроку.
* **Кодировки.** Все 12 C2S-кодеков ограничивают длину кадра, отвергают хвост и обрезку и
  проверяют диапазон в конструкторе записи (`FocusSelectionPayload`, `ResearchTableEditPayload`,
  `ResearchTableCombinePayload`, `ResearchTableCopyPayload`, `FocalManipulatorStartPayload`,
  `GolemBellResetPayload`, `ShovelOrientationPayload`, `HoverTogglePayload`,
  `WandArchitectPayload`, `AccessoryOpenPayload`, `ResearchPurchasePayload`,
  `ResearchNoteRequestPayload`).
* **Идентификаторы.** 32 идентификатора каналов уникальны и разделены по направлениям
  (проверено машинно по `Type`/`ResourceLocation`; `PayloadRegistryTest` это фиксирует).
  Дубликатов и «забытых» регистраций нет.
* **Мёртвый код оригинала.** `PacketFXBlockDig`, `PacketFXBeamPulse` и
  `PacketFXBeamPulseGolemBoss` в дереве 4.2.3.5 **регистрируются, но никогда не создаются**
  (проверено полным поиском по исходникам). Переносить их нечего; нативные
  `ExcavationFxPayload`/`VisNetFxPayload` покрывают реально используемые оригиналом лучи.

---

## 5. Валидация клиентского ввода: найденные классы риска

### 5.1 Уже закрыто в порте (перепроверено в этой волне)

| риск в оригинале 1.7.10 | состояние в порте |
|---|---|
| `PacketFXVisDrain:38` — `Aspect.getPrimalAspects().get(color)` **без проверки диапазона**: клиентский индекс вне списка **крашит клиент** | RGB валидируется в конструкторе `VisNetFxPayload:22-28` |
| `PacketNote` (SERVER) — сервер читал **любую** плитку по координатам из пакета | канал удалён, тон идёт через нативный block event |
| `PacketAspectPlaceToServer` / `PacketAspectCombinationToServer` — запись в **произвольный** стол по координатам из пакета | стол берётся из открытого меню отправителя |
| `PacketItemKeyToServer` / `PacketFlyToServer` — **чужой** `playerid` в пакете | отправитель берётся из соединения |
| `PacketScannedToServer` — клиент присылал **результат сканирования** | канала нет, сканирование считает сервер |

### 5.2 Остаточные риски, найденные в порте (НЕ исправлены — не мой домен)

1. **`PacketFXBlockZap`: два из трёх отправителей оригинала не воспроизведены.**
   Оригинал шлёт болт из `TileInfusionMatrix:537` и `:606` и из `TileNode:612`; в порте его
   шлёт только `TileEldritchTrap:46`. Канал и валидация нативные, недостаёт двух **точек
   вызова** (это юниты тайлов, а не сети). Записано в ledger-строку `PacketFXBlockZap`.
2. **`EntityAspectOrb.firstWand` (`common/entities/EntityAspectOrb.java:142-149`, юнит
   `creatures_and_projectiles`)** ищет жезл с **любым** свободным местом
   (`getVis < getMaxVis`), тогда как оригинал через `isWandInHotbarWithRoom` требовал
   места хотя бы под один целый виз; при нехватке места порт не съедает орб, а оригинал
   выбрал бы другой жезл. Каталог `common/entities/**` в этой волне не трогался.
3. **`ItemThaumometer.findEntity` (`common/items/relics/ItemThaumometer.java:106-125`)** берёт
   подушку из `getPickRadius()` (в 1.21.1 это 0.0 для всех сущностей, то есть всегда 0.8),
   тогда как оригинал брал `getCollisionBorderSize()` (ширина/2). Расхождение видно только на
   очень широких сущностях; юнит не мой.

### 5.3 Клиентские значения, которые порт всё же принимает — и почему это безопасно

| значение | как ограничено |
|---|---|
| `FocusSelectionPayload.inventorySlot` | диапазон `FocusSelectionSources.valid` (слот 0..35, −1 «текущий», либо маркер 1000/2000 + индекс ячейки < 18); сервер **перечитывает** стак по этому адресу, снимает копию инвентаря и **перепроверяет идентичность и содержимое всех слотов** перед фиксацией (`FocusSelectionNetworking.java:57-140`). Адрес ячейки выводится из проверенного адреса, а подсумок всегда имеет ровно 18 ячеек (`ItemFocusPouch.java:21`, `:30-35`) — выход за границы невозможен |
| `ResearchTableEditPayload.q/r` | радиус `ResearchNoteData.MAX_RADIUS` и правило `abs(q+r) <= radius` в конструкторе; сервер дополнительно проверяет тип клетки (`ResearchNoteEditing.java:101-183`) |
| `ResearchTableCombinePayload.left/right` | только канонические теги аспектов (`TileResearchTable.isCanonicalBonusTag`) |
| `ResearchPurchasePayload.key` / `ResearchNoteRequestPayload.key` | длина ≤ 128, существование, режим покупки, prerequisites, `@KEY`-clue и **достаточность пула** проверяются до любого списания (`ResearchPurchase.java:79-102`) |
| `FocalManipulatorStartPayload.containerId/sessionId/revision/upgradeId` | диапазоны в конструкторе; сервер сверяет контейнер, сессию, ревизию и `stillValid` |

Итог: **ни один C2S-канал порта не даёт клиенту выбрать игрока, измерение, позицию мира,
предмет, цену или баланс**; все адреса выводятся сервером. Новых дефектов этого класса в
этой волне не найдено, найденные ранее закрыты (см. 5.1).

---

## 6. Что НЕ удалось закрыть и почему

1. **Отмена установки блока рядом с элдритч-боссом** (оригинал
   `EventHandlerWorld.java:197-224`; проверка: измерение Outer Lands, не-креативный игрок,
   клетка лабиринта с признаком 2..5, `EntityThaumcraftBoss` в радиусе 32). В 1.21.1 нет
   события установки блока; порт реализует такие правила через mixin на
   `ServerPlayerGameMode`/`BlockItem.place`, а тайлы, генерация лабиринта и mixin-слой
   принадлежат другим доменам волны. Требует отдельной правки с риском конфликта.
2. **Очередь регенерации чанков** (`ServerTickEventsFML` / `EventHandlerWorld`): в 1.21.1 нет
   аналога события данных чанка, а свежие миры 1.21.1 — единственный авторизованный вход.
3. **Набор опций `Config`**: ~70 опций оригинала не воспроизведены. Эффективные умолчания
   семи «фактов исследования» совпадают (проверено, `ResearchSettings.java:8-21`), но сама
   опция как объект отсутствует.
4. **Клиентские эффекты `CommonProxy`**: ~40 записей прокси разнесены по клиентским
   классам; клиент в этой песочнице не запускается (нет GLX-контекста), поэтому визуальная
   приёмка не выполнена и не заявлена.
5. **Проверка 1.21.1 API**: сделана чтением класса `net.minecraft.world.entity.Entity` из
   `minecraft-merged-*.jar` минимальным парсером class-файлов (метода `getPickRadius` в
   `LivingEntity`/`Mob` нет — он объявлен только в `Entity`), а не компиляцией: Gradle в этой
   волне запускать нельзя.

---

## 7. Авторские тесты (НЕ ВЫПОЛНЕНЫ)

| файл | тестов | что закрепляет |
|---|---|---|
| `common/src/test/java/thaumcraft/common/lib/utils/EntityUtilsParityTest.java` | 4 | подушка выбора `max(0.8, width/2)` (0.6 → 0.8, 16 → 8.0); линейное сравнение минимальной дистанции (9 < 4² → «ниже»); `minY + height/2` = 64.9 и `minY + eyeHeight` = 65.62 |
| `common/src/test/java/thaumcraft/common/lib/utils/InventoryUtilsParityTest.java` | 5 | остаток `addVis` в целых визах (0/25/24/0 — с исходным усечением); отсутствие переполнения (`Integer.MAX_VALUE − 25`); правило «метаданные 32767 = любое значение»; ограничение размера стека в сравнении для крафта; правило тегов крафта |
| `common/src/test/java/thaumcraft/utils/SharedInfrastructureParityTest.java` | 9 | 17 идентификаторов `RefGui`; умолчания семи фактов исследования и пустой файл конфигурации; три режима `researchDifficulty`; `pack`/`unpack` (0b10110010); таблица типов NBT (1..11, null на 12); конус `isLyingInCone` (внутри / позади вершины / за круглой крышкой / сбоку); `calculateVelocity` (2.5, 1.0, 0.0); `distanceSquared2d/3d` |

Итого **18** тестовых методов в 3 новых классах. **Ни один не выполнен** — Gradle в этой
волне не запускался.

---

## 8. Ledger

`docs/porting/scope-ledger.json` изменён **только** в строках домена
`shared_infrastructure`: 34 единицы из моего списка + 3 строки уже закрытых единиц
(`EntityUtils`, `InventoryUtils`, `Utils`), в которые записана правка D-1…D-6.

* `counts` не тронут (менялись только `native_coverage`, `evidence` и `notes`, а
  `counts.dispositions` зависит только от `disposition`).
* `disposition` не тронут ни в одной строке.
* `reviewed_sha256` не тронут.
* Проверка: `update_scope_ledger.py --check` → `stale_review_fingerprints: 0`.
  Локально оракула нет, поэтому проверка выполнялась **воспроизведением** `refresh()` из
  `scripts/porting/update_scope_ledger.py` с заменой корня оригинала на читаемый checkout
  `OpenTC4` и подстановкой записанных в ledger отпечатков для трёх файлов, которых в этом
  checkout нет (`HexUtils`, `UUIDHelper` ×2). Результат совпал с файлом байт в байт.

---

## 9. Коммиты

| коммит | содержание |
|---|---|
| `83ebc6a7` | `fix(sharedinfra)`: D-1…D-6 |
| `dd31d561` | `test(sharedinfra)`: 18 авторских тестов |
| `9fec3e0b` | `docs(sharedinfra)`: 34 единицы ledger |

---

## 10. Что осталось открытым по домену

* 5 единиц `partial` с названными пробелами (см. §1).
* Три «клиентских визуальных» канала оригинала не имеют ни одного нативного отправителя
  (`PacketFXBlockDig`, `PacketFXBeamPulse`, `PacketFXBeamPulseGolemBoss` — мёртвый код
  оригинала) либо потеряли часть вызывающих мест (`PacketFXBlockZap`).
* `EntityAspectOrb` и `ItemThaumometer` содержат расхождения с оригиналом в выборе цели
  (см. §5.2) — это юниты других доменов, в этой волне не менялись.
