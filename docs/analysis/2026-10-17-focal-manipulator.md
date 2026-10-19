# Следующая Fabric unit: манипулятор фокусов и Excavation upgrades

**Начата после принятого/экспортированного M09b; original audits завершены,
реализация в работе, ещё не compiled/native/client/accepted.** Поставляемый JAR остаётся M09b
`bac83fff54504682f7a00523d22f10e00a334eb922f559f93ea27bf349408caa`.

## Порядок и граница

План M08/M09: настоящий VisNet → focal manipulator / Excavation upgrades → Arcane
Bore. Не ждать полного M09 и не начинать NeoForge. Новый bounded gate должен дать
реальное устройство, native inventory/menu/GUI, сохранение focus upgrade data,
проверенный выбор, XP и actual VisNet оплату, применяемые Excavation эффекты.
Принятая bounded граница: Frugal/Potency для четырёх существующих фокусов,
Treasure1/3/5 и Enlarge2/4 дополнительно для Excavation; исходный относительный
порядок выбора сохранён. Silk/Dowsing и другие специализации не выбирать и не
оплачивать до реализации их эффектов. Это не full M08/все Excavation ветки.

TC4 4.2.3.5 private oracle — единственный gameplay authority. OpenTC4 по pinned
commit `80aac29c52543ef51766c4b0788fba782abb3b0b` — читаемый reference, не donor.
Root повторно прочёл GitHub pinned `common/config/ConfigRecipes.java`; не обновлял
пин и не копировал его implementation. Original/private/legacy источники неизменны.

## Root dependency audit

- Original `ConfigRecipes.java:203`: `FocalManipulator`, research
  `FOCALMANIPULATION`, all six primals32, output stoneDevice13×1. Pattern
  `IQI / SPS / GTG`: literal iron2, arcane stone slab1 (native
  `arcane_stone_brick_slab`), arcane stone2 (НЕ bricks), primal charm1, gold2,
  ordinary table1. Нельзя подменять charm дешёвым material или иной машиной.
- Original `ConfigRecipes.java` / pinned reference: `PrimalCharm`, research
  `BASICARTIFACE`, all six primals25, pattern `123 / ISI / 456`: шесть различных
  primal shards, два gold ingots и balanced shard. Native resource id уже есть,
  но рецепта сейчас нет: получение нужно включить в dependency closure.
- Charm НЕ считать полностью перенесённым по рецепту: original
  `ItemResource.java:152–191` имеет inventory world-RNG `nextInt(20000)`, отдельные
  aspect orb / `@FOCUSPRIMAL` hint branches; `:314` stack limit1. Эти callbacks
  требуют отдельного решения/теста, не silently inert parity claim. Orb/Hints
  нельзя заменять free vis/XP или эффектами другого TC.
- Root владеет registries/bootstrap, recipes/book/icon/name aliases, relevant
  component wiring, wrappers/manifest, единственными сборками и evidence/export.

## Audits завершены; назначенная реализация (без Gradle/game)

1. `ses_eb6551179ffei3AdCR4G2twV8G`: NEW BlockFocalManipulator,
   TileFocalManipulator, ContainerFocalManipulator, FocalUpgradeCosts + pure/native
   tests. Early API `.tmp/focal-machine-api.md`. Нет existing source edits.
2. `ses_eb6551174ffevAGMKuvjPflzwI`: NEW GuiFocalManipulator/BER/client effects/
   initializer, новые focal-only models/states/loot; narrow SHIFT tooltip в
   ItemWandCasting ONLY. Early API `.tmp/focal-client-api.md`. Client fixture
   пока только proposal, без sequence/source/save edits.
3. `ses_eb6551122ffeD0gDd69KYXUa0W`: NEW FocusUpgradeType/FocusUpgradeData/FocusVis,
   narrow existing ItemFocusBasic, FocusData, четырёх native focus runtimes,
   WandFocusActions, WandVisDiscount, ExcavationHarvest + new tests. НЕ редактирует
   ItemWandCasting/registry/resources/других owners. Early `.tmp/focal-upgrade-api.md`.

Root: NEW ThaumcraftFocalManipulation registry; focus_upgrades component wiring;
literal charm/manipulator recipes, book/icon aliases, charm stack1; native menu
START+ACK payloads, bootstrap, retained-art full-sheet atlas, EN/RU scope labels,
six new payload JUnit and two resource assertions (написаны, НЕ запускались).
Подключён common client initializer; renderer/assets/API integration ещё pending.

ALL3owners SOURCEFROZEN. Root просмотрел core/planner/data/casting/GUI/BER и
подключил missing tick-only client effect hook (blockticker + tilecallback),
сохранил unsaved focal counter при update existing BE (новый BE naturally0),
восстановил original пять `-1` даже для unupgraded sorting. Старые3native helpers
обновлены ТОЛЬКО exact sorting expectations до подтверждённого original contract;
другие assertions и все105cases сохранены. Добавлены7isolated wrappers, required112.
Ожидание238JUnit/112Fabric/74Python — НЕ результаты. Первый центральный compile
ещё не завершён; новый focusedclient/harness/приёмка не запускались. Independent
read-only Enlarge reservation review не блокирует компиляцию и не меняет source.

## Итоговая приёмка 17.10.2026 — принята и экспортирована

`focal-fabric-validate7` после фактического `client14`: **238JUnit/113requiredFabric/
74Python88.423s**,0failures/errors/skips,3865unchangedinputs. `client14` exit0,
5658POSTs29–5686, все10exactPNG164–173 осмотрены root; отдельный независимый
stopped-NBT audit прошёл. [Client record](../porting/focal-client.json),
[verification](../porting/verification.json), архив `.tmp/focal-accepted-final7/`.
JAR10,641,110bytesSHA256`0104dc3756199192b56af2956a6f544702b2c6bb5848fc725d96ba4f051e3f10`
экспортирован в distribution/mods; M09b сохранён в backups/visnet-fabric-validate7/.
Зависимости/187accepted-parentfiles/64inheritedprofilefiles неизменны.

Истинные платежи:112nativeXP,8LEVELS,2250+2880каждогоprimalrecipeCV,2400rawCV
Potencyjob,13terraharvest. Сохранены108gold/OLD3plusNEW1cobble4/pickdamage5/
focus15rank1/payer10; это не grants. Все11named/NoAIactors и3cowhealth6 точны.
Один поздно загруженный старый skeleton вышел через фактический vanilla
Mob.checkDespawn/DISCARDED; все197oldUUID проверены,27late-native snapshots
не подменяют inherited identity. Старый1551узел честно pruned4times1200..4800:
BASE76/77/78 и currentTerra1/Ordo2, NORMAL/NONE/cachelock0/regen600 подтверждены
точной цепочкой и stoppedNBT, без RNG/clock/AI/entity/reset writes.
Нативный новый offlineprofile отдельно связан с log/login/playerdata, не выдан
за прежнийUUID. Auto-owned-focus/Shift/XP/lifecycle instrumentation TESTONLY,
отсутствует в productionJAR. Full visual/audio/packaged/EULA-server/remote/
survival/API/fullTC4/NeoForge и passiveCharm/Silk/Dowsing остаются pending.

Новый принятый stopped parent: `focal-smoke-8fd11823-4346-48f9-97ed-e06dbf9ec4be`.
Сразу начат следующий Arcane Bore prerequisite — явный pristine-tool predicate
инфузии; ниже сохранена история неуспешных/исторических прогонов, не текущий блокер.

## История: первые фактические прогоны — НЕ ПРИНЯТЫ

### Итоговый клиент и post-client проверки (ещё ожидается stopped-audit)

`focal-fabric-client11` **exit0 / SCRIPT_PASSED**, run
`client-smoke-120d354b-898d-4197-85bd-27614f892982`, fresh private clone
`focal-smoke-ee44ac08-5a61-4027-a14d-acd117eeb8be`. Нормальная остановка
зафиксирована в log; независимый SERVER_POST tail содержит5598наблюдений93–5690.
Исходный M09b parent187files по-прежнему побайтно неизменён.

Root осмотрел все десять exactPNG164–173 и записал отдельные наблюдения/хеши в
`.tmp/focal-fabric-client11-inspected.json`. Нативные рецепты/модель/GUI/прогресс/
rank1 history/Shift-Potency tooltip/rank2 red16 видны. У172есть дополнительный
phial tooltip; мелкие CV цифры, тёмный фон GUI и отсутствие полного motion/audio
parity явно отмечены. Наличие подписи файла не считается доказательством платежа.

Настоящие112smelts/16coal→112XP,2250+2880каждого primal recipeCV,
единственный8LEVELSSTART/2400rawCV Potency job, protected13terra stone harvest,
native same-UUID pickup→OLD3plusNEW1cobble4/108gold и server-restored payer10/focus15
подтверждены ledger. Native Shift DOWN/UP и `Screen.hasShiftDown()` прошли на
фактически активном HWND после ручного клика пользователя. Никаких XP/vis/clock/
finished-output/item-motion grants. Один старый unnamed/nonpersistent skeleton
вне ownedareas действительно вышел через `Mob.checkDespawn`/DISCARDED, что
зафиксировал невмешивающийся test-only HEAD observer; это не обещание бессмертия
всех ambient mobs. Именованные/NoAI/ownedarea сущности остаются защищёнными.

Финальный post-client `focal-fabric-validate6`: **238JUnit /113requiredFabric /
74Python93.272s**,0failures/errors/skips, **3864unchangedinputs**. Fresh JAR
10,641,110bytesSHA256`0104dc3756199192b56af2956a6f544702b2c6bb5848fc725d96ba4f051e3f10`,
notices/isolation/oneintermediaryAW/art/recipes проверены. Архив
`.tmp/focal-fabric-validate6-final-evidence/` содержит XML/JUnit/log/input evidence.
Это ещё не экспорт: независимый stopped-NBT auditor проходит отдельный root review.
Подготовленные recorder/exporter не являются результатом и не должны обходить gate.

### Promoted focused client и предпусковая проверка

B завершил staging и не менял production. Root перенёс точные3файла
(`.tmp/focal-client-promotion1.json`), подключил FOCAL-only Book branch ДО старых
setup/XP fixtures, TESTONLY pickup observer mixin, fresh accepted-M09b clone и
strict ten164–173PNG/normal-stop tail gate. `focal-fabric-compile2` exit0.
Независимые read-only A/C reviews проверили economy/source/XP/furnaces отдельно.

Root read-only preflight обнаружил старый1618,216,377верстак на месте площадки:
его не трогали, activeCLIENTONLYкамера/колонна перенесены на1618,376. Старые pads
не образуют сплошной пол:19точных previouslyAIR floor supports явно добавлены в
fixtureplan без broad terrain rewrite. Все54финальные fixture/earned cells AIR/
noBE подтверждены stopped reader;187parentfiles неизменны. Live entity/loaded/
collision guards остаются обязательными, это не runtime evidence.

Полный `focal-fabric-validate4`:238JUnit/ALL113Fabric/74Python101.380sPASS,
3861unchangedinputs; productionJAR10,641,110bytesSHA256
`0104dc3756199192b56af2956a6f544702b2c6bb5848fc725d96ba4f051e3f10`.
FreshZIP/notices/isolation/oneintermediaryAW/originalart/recipes проверены.

После него root исправил ТОЛЬКО client harness по A/C findings: volatile intent
публикуется ДО GUI C2S, сервер проверяет все восстановленные savedslots и точный
payer10/CV; post-completion job строго0. Pristine vanilla furnace имеет cookTotal0
до реальной input insertion: observer разрешает только0→native200 и не пишет часы.
START8LEVELS сохраняет total/progress строго, как native nonnegative method.
Начат НОВЫЙ full `focal-fabric-validate5` +Python5 на исправленных frozen inputs;
его успех ещё не заявлен. Production после validate4 не менялся.

Actualclient/PNG/stoppedclone ещё не запускались; `.tmp/audit_focal_stopped.py`
подготовлен, не является PASS. Все прежние accepted binaries/saves сохранены.

Последующее: validate5 также238/113/74PASS(97.772с),3861inputs/JAR0104dc...;
все evidence archived `.tmp/focal-fabric-validate5-before-client/`. Первые actual
client1–3 exit1 безPNG/POSTledger/smelting/crafts: исходные guard assumptions были
ошибочны. Parent187files каждый раз hash-identical, failed privateworlds retained/
NEVER retry inputs. Client1: Slot3 — индекс, actual spare relay1, а не3. Client2:
oldcobble3Slot5, не0. ALL26native inventory stacks/15freefixture slots затем
проверены independently (`.tmp/focal-actual-parent-inventory-preflight.json`).
Теперь один настоящий savedrelay устанавливается, finalspares0/Slot3empty;
oldcobble3 сохраняются плюсONEactualpaidUUIDoutput→4Slot5. Нет grants/refills/
discard/relabeloldstock; observer/cursor/slot conservation strengthened accordingly.

Client3: startup thirdcow1628.5ещё не loaded; PRIMARY FOCALMANIPULATION fixture уже
был объявлен/добавлен, но block/raw fixtures ещё не создавались. Root добавил
bounded500clienttick SCENE_READY через PUREgetChunkNow+actualentityqueries, без
force-load/recreate/move/health/AI writes. FOCAL-only owned view/sim8/8 вместо4/5
нужен для ширины87блоков olddevice mines→8furnaces и трёх прежних cows; native
chunk activation/ticks/pruning наблюдаются как есть, не выдаются за frozen scene.
Freshclient4 запущен, результата ещё нет. Эти fixes толькоclientTest/ownedrunner;
production/JUnit/native/Python unchanged, historicalfull5 не переименован в новый
PASS. Fresh finalFULL/Python после принятого клиента обязательны перед экспортом.

**Последующее фактическое состояние:** `focal-fabric-native3 -GameTestOnly` exit0,
**все113required Fabric PASS / failures/errors/skips0 /3857unchangedactiveinputs**.
Свежий XML и source snapshot проверены root, архив `.tmp/focal-fabric-native3-*`.
Это native-only, не новый full238JUnit/74Python/freshJAR/client acceptance.
В fullvalidate2 обе прежние ошибки были закрыты (112/113PASS); единственная новая
ошибка была positive-loot fixture: vanilla TNT table требует actualTNT+unstablefalse,
поэтому customprobe не мог выдать положенный контрольный output. Подтверждено по
реальному cached1.21.1JAR. Root заменил ТОЛЬКО loot пробника на vanilla cobblestone
без block-state gate и разделил строгие AFTER/drop assertions с actual counters.
Теперь все четыре partial-removal cases прошли, без production refund/drop waivers.
B пишет client ТОЛЬКО в ignored staging; следующая source-bound FULL/Python после
promotion и отдельно после фактического клиента. Пока новая unit не принята.

`focal-fabric-compile1` exit0. Затем `focal-fabric-validate1` exit1:
**238 JUnit PASS**, **112 Fabric / 110 PASS / 2 новых failure**, все105старых cases
без ошибок; **74 Python PASS / 95.976с**. Архив реальных XML/JUnit/partial results:
`.tmp/focal-fabric-validate1-partial/`. Непринятый JAR10,642,452bytes/SHA256
`2a9d543a2d8d789ed864a9b9963b95143270e88569e0ebf1e9b7ebee8c6aabcd`
не экспортировался; текущие исправления уже другие inputs.

- `focalPostDebitFences`: relay fixture впервые ticked до регистрации нового source.
  A исправил только новый native helper: source first native tick, zero-first-pulse,
  exact registration и reciprocal route assertions; другие postdebit guards сохранены.
  Fix frozen `e77fbc71b43a...`, пока не повторно исполнен.
- `excavationUpgradeTreasureAndEnlarge`: fixture ловил IllegalStateException, но
  реальный ServerPlayer.doTick заворачивает его в ReportedException. C исправляет
  catch с проверкой точной исходной exception identity, не ослабляя state assertions.
- Независимый B review дополнительно нашёл production unsafe refund: нормальный
  `removeBlock=false` может следовать ПОСЛЕ частичного onRemove mutation. Значит
  reservation нельзя возвращать на одном false/snapshot. C assigned retain/log
  indeterminate delegated false/throw; cancellation/no-match ДО delegate остаются
  бесплатными. Accepted successful-removal witness и drop gating не менять.
  Новый EARLY registered onRemove probe должен воспроизвести actual false mutation
  и проверить оплату/сохранённый replacement/отсутствие ложных drops.

Root уже подключил planned `ExcavationRemovalFailureRuntimeChecks.registerEarlyProbe()`
и `.run(GameTestHelper)` в isolated test bootstrap/новую113-ю обязательную wrapper.
Нового test PASS ещё нет. Следующий центральный запуск только после C freeze,
с НОВЫМ input snapshot; старый validate1 не переименовывать в успешный.

B пишет будущий focusedclient ТОЛЬКО в ignored `.tmp/focal-client-src/`; эти templates
не входят в active source sets, не compiled/исполнены. Root согласовал finite
`.tmp/focal-client-fixture-plan.md`, включая настоящие112furnaceXP/нет XP grants.
Promotion/Book scope/Gradle/mixin metadata и новый source-bound прогон — отдельно,
после staged freeze и без одновременного Gradle. Клиент/PNG/stopped NBT acceptance
пока не запускались. Accepted M09b parent187files и distribution сохранены.

## Экономика и authority decisions

- Original rank1..5: threshold/charge **8/16/24/32/40 XP LEVELS**. Creative тоже
  требует threshold, но не списывает. `200 << (rank-1)` за КАЖДЫЙ distinct upgrade
  aspect key; его объявленное amount игнорируется. Primals — **SUM**, НЕ source MAX.
- Пульс каждой пятой native update, до100 raw CV КАЖДОГО remaining aspect; start
  не reset counter, counter не persisted. Нет passive/redstone/open-menu gate.
  Reload продолжает remaining без второго XP. No refund при отмене/поломке.
- Original setter очищает только remaining и позволяет применить paid job к
  replacement focus без vis/eligibility; raw nonfocus replacement может crash.
  Native job bound к точному focus+revision; extraction/replacement отменяют ДО
  дальнейшего debit без refund. Обычный slot access не блокируется. Это explicit
  authority/crash adaptation, а не silent gameplay change.
- Native request only `(containerId, sessionUUID, expectedRevision, upgradeId)`;
  только authenticated current menu/server thread, revision/BE/slot/distance/
  research/XP перепроверяются. Vanilla unversioned button не имеет bypass.
  Selection локальна; ACK после native inventory+BE correction разрешает следующий
  запрос, в том числе после отказа. Никаких client price/rank/XP/stack claims.
- Frugal только casting: cap - netgear/100F - level/10F, затем clamp. Arcane recipe
  reducer/payment неизменны; derived casting zero-cost entries допустимы. Potency
  реально меняет все4фокуса; Fortune применяется только к private loot tool.
- Enlarge требует server-created successful-primary permit, exact native-state
  matching соседей и новый shuffle D/U/N/S/E/W на каждую funded попытку. Сохраняются
  native protection/cancellation/removal witness, не generic skip-ray flag. Billing
  extras после success; callback-funding/identity safety adaptation явно тестировать.
- Original invalid-rank indexing проверять ДО доступа к rank-1; immutable five-short
  ordered data на focus, не enchantments/дубликат WandData. Не превращать invalid
  или неподдерживаемые upgraded stacks в рабочие якобы basic фокусы.

Общие signatures/source ownership назначены; точный finite client fixture ещё
pending отдельного одобрения root. Перед центральной сборкой заморозить ВСЕ source sets,
включая clientTest; только один Gradle/game process. Никаких green-by-stub tests.

## Обязательная последующая проверка

- Точные rank/choice requirements, original порядок и XP/vis economics; не считать
  menu preview оплатой. Отказ по XP/power/research/invalid/replayed/stale intent.
- Native sender/open-menu/distance/session; два игрока, slot transfer, extraction,
  отмена, reload, актуальный BE/focus identity и callback fences. Сохранённые
  расходы не refund/replay произвольно.
- Upgrade persistent/network codecs: whole invalid data, отсутствие mutable aliases,
  wand equip/remove, item transfer/drop и save/reload. Эффекты реально исполняются
  через existing native protected harvest, не direct block removal bypass.
- Все прежние 105 Fabric cases и220 JUnit не заменять новыми. Focused client на
  свежей частной копии принятого STOPPED save; не replay всей прошлой progression.
  Старые stocks/gear/XP/мир — объявленные inputs, не новый survival achievement.
- Не grant/refill XP/vis/essentia/готовые улучшенные outputs. **Root native-NBT
  readback уточнил: XpTotal11, но XpLevel0, а НЕ11уровней.** Значит даже первый
  платёж8levels требует настоящего XP acquisition. Предварительное предположение
  о готовом первом платеже отменено; client нельзя начинать без finite native XP
  маршрута. Возможен обычный furnace/smelt/extract с объявленным raw gold/fuel и
  реальными XP-orb UUID/pickup, но не direct XP writes/spawns или сбор старых mobs.
  Точный объём/время и fixture требуют проверки; higher-rank native fixtures не
  выдавать за прохождение всех рангов в клиенте. Client сценарий ещё не назначен.
- Accepted M09b returned node count1091/current0 подчиняется исходной boundary1200.
  При следующем private-copy запуске не reset/замораживать/«лечить» pruning.
  Accepted parent и все другие accepted saves не открывать и не изменять напрямую.
- Actual source-bound build/native tests, осмотр exact PNG, independent stopped NBT,
  finite inventory/XP/CV ledger и previous-workshop preservation перед экспортом.
  Packaged/EULA/remote/full survival/full visual/audio/NeoForge отдельно pending.
