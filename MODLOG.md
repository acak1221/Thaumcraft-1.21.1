## 2026-10-20 — Next Fabric content group (remote runtime validation pending)

Unknown knowledge and scan clues/entity variants; server research settings; Repair/Haste infusion
and native Bore repair; Ward behaviour and Excavation Silk/Dowsing; Growth/Fertility lamps,
Deconstruction table, Brainy/Giant zombies. Local 620 JUnit and 78 Python pass; full server
validation is offloaded to GitHub CI for the next commit. Client/NeoForge acceptance remains pending.

# MODLOG — анализ Thaumcraft 4.2 FOREVA

## 2026-10-20: интеграция исследований, инфузии, устройств и Warp — CI passed

- R10: исходные расходы копирования и whole-stack persistence/count100/127/128.
- Thaumatorium и Mnemonic Matrix; 22 рецепта зачарования с оплатой XP до эссенции.
- Warding/Architect/Trade, Arcane Lamp/Amber и Vis Charge Relay с оплатой до callbacks.
- Warp: серверные ветки, эффекты, Mind Spider/Guardian/Orb и клиентские ресурсы.
- GitHub CI на d508ada: 582 JUnit / 186 Fabric GameTest / 78 Python, fresh-result/production-isolation/attribution gates прошли. Клиент/NeoForge не подтверждены.
- CI: кэш Gradle/Loom, свежие тесты без принудительной пересборки всех задач, отчёты 7 дней, отмена устаревших прогонов.


## 2026-10-20: магическое ухо

- `thaumcraft:arcane_ear`: исходные настройки/рецепт, пять инструментов, 25 нот, дальность 64 и сигнал 15 на 10 тиков.
- Настоящий vanilla note hook, очередь до POST тика, EN/RU, книга, модели off/on, дроп без сохранённой настройки.
- Совместная проверка с предыдущими резервуарами: 542 JUnit / 177 Fabric GameTest / 76 Python, Fabric build/isolation.
- Субагенты выполнили реализацию и два независимых ревью; найденные ошибки timing/packet codec исправлены.
- [Контракт и ограничения](docs/analysis/2026-10-20-arcane-ear.md); client visuals/NeoForge остаются непроверенными.

## 2026-10-20: резервуары в работе алхимических големов

- Обновление с GitHub до `88d683b`; реализованы исходные ветки сбора/доставки резервуара, его приоритеты и частичный остаток.
- Содержимое резервуара и настоящий груз голема оплачиваются одним native commit; прямые вызовы без активной задачи отказывают.
- Исправлен старый предел 32 в общем переносчике: два Earth у Thaumium допускают 64.
- 542 JUnit / 176 Fabric GameTest / 76 Python прошли; Fabric JAR и production isolation проверены.
- [Источники, точные результаты и открытые проверки](docs/analysis/2026-10-20-golem-reservoir.md); клиента/NeoForge не запускали.

## 2026-10-15: начальный системный анализ

- Полный клон `0FL01/Thaumcraft-4.2-FOREVA`, ветка `master`, baseline `4bc68454b1063cdc1255f833dd537e0fd4fc341d`.
- Production code и original/donor JAR не изменялись; новый commit не создан.
- Инвентаризация: 934 main Java + 530 Java test files, 1199 test annotations, 1676 resource paths.
- Original behavioral oracle: CFR 0.152, 859 Java files вне Git-репозитория; `thaumcraft_src` здесь содержит ресурсы.
- Проверено: Python tests 13/13; donor ABI snapshot 1665 entries; semantic target snapshot 310 symbols.
- Java 8 HashMap int-array cache miss и Python HANDLE6 resolver mismatch воспроизведены отдельными fixtures.
- Сервер/клиент Minecraft и full Gradle build не запускались: Docker daemon недоступен; локальный Windows checkout имеет CRLF shell scripts.
- Подробные подтверждённые факты, условия и открытые runtime вопросы: [главный отчёт](docs/analysis/2026-10-15-CODE_ANALYSIS_RU.md).
- В Git добавлять только аналитику/собственный код; декомпиляция оригинала, transient tools, logs и saves остаются вне tracked deliverables.

Следующий этап: воспроизводимый build, focused critical state tests, dedicated server и final-JAR checks, client visual scenarios. Documentation-only files не требуют runtime smoke по AGENTS.md.

## 2026-10-15: нативная основа Minecraft 1.21.1

- Создана локальная ветка `port/minecraft-1.21.1`; legacy `src/` сохранён без изменений и исключён из новой сборки.
- Переход на Java 21, Gradle 8.12.1, Architectury API 13.0.11, Fabric и NeoForge; оба референса зафиксированы в `docs/porting/source-lock.json`.
- Реализованы 48 аспектов, данные 201 исследования, знания/сохранение/mixin/S2C-синхронизация игрока, серверное сканирование, wood/iron wand, тестовый узел, банки64 и phials8; четыре начальных рецепта соответствуют оригиналу.
- Перенесены исходные PNG/OGG, modern модели и переводы; исправлены atlas inclusion, nested filter schema, uppercase sound IDs и общий mipmap. Текстовые ресурсы/provenance fingerprints приведены к canonical LF для одинакового результата checkout на Windows/Linux.
- Проверки фактически выполнены: 46 JUnit, 19 Python asset tests, 5 Fabric и 5 NeoForge GameTests; `scripts/dev.ps1 -Task validate` через официальный wrapper завершился с exit0 (`.tmp/modern-validation3.log`).
- NeoForge client загрузил итоговый atlas1024x1024x4; integrated world login и сообщения сканирования наблюдались в логе. Полное визуальное соответствие не установлено. Fabric startup проверен до последней правки исключения unused mipmap sprites; общие ресурсы после неё проверены Neo/native codecs.
- Production JAR обоих загрузчиков проверены на отсутствие Minecraft/legacy classes и вложенных оригинальных JAR; hashes и результаты: `docs/porting/verification.json`.
- Neo mock handshake настроен до реального JOIN; проверяется отправка и декодирование фактического Architectury encoded payload. Production networking не подменялся для прохождения теста.
- CI переведён на Java21 и оба headless harness; remote workflow не запускался, автоматическая публикация старой 1.12 release workflow заменена ручной проверкой. Никаких push/release не выполнено.
- Механики, оставшиеся pending, перечислены в `docs/MIGRATION_STATUS.md`; все934 legacy paths учтены:12partial/922pending/0complete. Первый рабочий checkpoint не означает полный перенос TC4. Следующий приоритет: таумономикон, исследовательский стол/записки/подсказки, затем arcane crafting/crucible/infusion.


## 2026-10-15: таумономикон, актуальные refs и Git checkpoint

- Продолжен существующий checkpoint `8abb7feb` на ветке `port/minecraft-1.21.1`; полный порт всё ещё не завершён.
- Три субагента независимо проверили active gameplay, build/packaging и оба reference corpora. Созданы пофайловые inventories pinned FOREVA/OpenTC4/Architectury и свежего Architectury upstream3a836a39, delta45; пины gameplay/reference не заменены.
- Добавлены normal Thaumonomicon, bookshelf wand trigger без vis cost, native floating/fire-resistant item entity, server snapshot/open, original single-pass sibling effects, category/research graph visibility и completed/gated text+illustration pages с EN/RU pagination.
- Исправлены original node rewards max(4,vis/10), block picked-item identity/redstone duplicates и HexUtils original fixed orientation с расходом RNG draw. PowerShell single-task splatting исправлен; оба remapped JAR включают проверенные canonical notices+LICENSE.
- Реальные проверки final production source: `.tmp/session-book-final-validate.log` exit0;61 JUnit,9 Fabric и9 NeoForge GameTests,0 failures/errors. Python tooling24PASS; extractor/resource checksPASS;492 sprites/59 refs/66 events/111 sounds/missing0.
- Реальный клиент выявил double-blur из Screen.render1.21.1; устранён. Final isolated Fabric smoke3 exit0/SCRIPT_PASSED, все8 точных PNG просмотрены root, подтверждены sharp map/text/inline illustration, Next/Previous и RU overflow. Промежуточное подозрение о пустом Previous опровергнуто равными pixel ROI03/05; не является gameplay defect. Test-only harness снимает afterRender+flush и не входит в production JAR.
- Evidence: `docs/porting/verification.json`, `client-smoke-verification.json`; source inventory43 active Java,17partial/917pending/0complete из934. Private source/saves/originalJAR не менялись и не добавлялись.
- User explicitly предложил использовать Git: текущая проверенная unit сохраняется локальным коммитом; public push/release не выполняется. Следующая unit — research unlock/note service, затем table/puzzle и native recipe pages.


## 2026-10-15: отдельная спецификация следующего research этапа

- После user steering про дополнительных субагентов и Git продолжен read-only original audit: записаны research-unlock-spec и research-notes-spec, production исходники не менялись.
- Установлены original default difficulty0, Secondary-only point purchase, exact cost/prerequisite/clue/warp rules; native purchase обязана атомарно recheck баланс на сервере вместо original authority defect.
- Сопоставлены note NBT/data kinds, solved vs learned flags, endpoint/RNG/HashMap ordering, paper/tool costs, original copy ink_sac и расхождение legacy FOREVA FEATHER. Эти документы — specification/test plan, не реализованная research note механика.
- Проверенная playable book unit сохранена локально в commit85e22827; следующий docs commit отделяет план будущей unit от реализации. Никакой public push не выполнялся.

## 2026-10-15: покупка вторичных исследований

- Продолжена ветка `port/minecraft-1.21.1` после8540b3dc. Три субагента разделили policy/tests, client UI/smoke и real-loader fixtures; независимые повторные review policy и networking/UI не нашли конкретных дефектов.
- Книга покупает Secondary при default difficulty0/wussfalse за исходные aspect costs. Native key-only C2S (лимит128) использует sender connection и server thread. Полный admission/balance preflight, atomic copy/commit, exact debit, first-completion warp и direct ordered siblings; replay без повторных расходов.
- Исправлен подтверждённый original authority defect: server больше не доверяет client affordability/difficulty/clue. Публичный PlayerKnowledge overdraw quirk не изменён; native config, primary notes и clue generation остаются pending.
- Реальные проверки `.tmp/purchase-validate1.log` exit0:79JUnit,13Fabric+13NeoForgeGameTests,0failures/errors;24PythonPASS, extractor/resource/noticesPASS. Новые NeoForge scenarios декодируют настоящий encoded knowledge S2C. Both production JAR собраны с final main source; clientTest в них отсутствует.
- Actual Fabric client `.tmp/purchase-client1.log` exit0/SCRIPT_PASSED: node click при3/2 без изменений, затем6/6→3/3 по C2S/S2C, без optimistic completion/debit, completed-entry reopening без доплаты. Root просмотрел все10 exact PNG;09 показывает green3/3/red2/3 tooltip,10 readable purchased text. Снимки EN/RU, map и pagination сохранены. Полное TC4 visual parity и NeoForge purchase client не заявлены.
- Обновлены verification/hashes, asset manifest, handoff и inventory:47active main Java,19partial/915pending/0complete из934. Следующий этап — primary note component/generation/ink/paper и research table.
- Всё сохранено локально через Git; public push/release, original/legacy edits и включение private decompile/saves не выполнялись.

## 2026-10-15: план полного переноса

- Добавлен `docs/MIGRATION_PLAN.md`: границы TC4 4.2.3.5 → Java21/Minecraft1.21.1/Fabric+NeoForge, этапы M00–M15, зависимости, конкретные research units R01–R13 и критерии приёмки полного игрового цикла.
- Три субагента проверили gameplay coverage, architecture/verification и original-vs-legacy scope. Сверка paths:859 original,934 legacy,749 shared,185 legacy-only,110 original-only. Пофайловые числа не объявлены процентом игрового прогресса; введён план отдельного disposition ledger.
- Учтены early survival resources/nodes, accessory adapter, API, save schema/version policy, bounded knowledge sync, clean CI/fresh evidence, test helper isolation, packaged multiplayer, визуальные/звуковые и человеческие проверки.
- Изменена только документация. Production code, проверенные counts и evidence не повышались; Gradle/игра заново не запускались. Проверены ссылки, структура плана и Git diff; план сохранён отдельным локальным commit.

## 2026-10-15: начало полного плана, данные записок и изучение

- Активирована полная пользовательская цель M00–M15; она не завершена этим checkpoint. Три субагента реализовали note data/generator/solver и independent reviews, client scenarios, scope ledger и original ink audit.
- R01 immutable validated component persistent/wire, R02 original HashMap/RNG generator и516 private algorithm goldens наJava8/21 для172definitions, R03 direct-component solver/pruning. R04 частично:100inktool+IScribeTools+3nativeScribe recipes. R05 normal/discovery item learning/presentation с server parents/transfer/consume1/warp/siblings; shared completion helper сохраняет purchase semantics.
- Final central `.tmp/notes-validate5.log` exit0:131JUnit,17Fabric+17NeoForgeGameTests;24PythonPASS, extract/assets/noticesPASS. Initial integration failures исправлены до final run; validate компилирует clientTest, его source надо замораживать тоже.
- Actual `.tmp/notes-client1.log` exit0/SCRIPT_PASSED, все12PNG просмотреныroot;11/12 native inventory notes/discovery, ordinary useItem учитNITOR/consume1 безoptimisticstate и безreader lux, replayоставляеткопию. Pools/warp/другаяnote/ink99неизменны. Interactive table ещёнеигрался; серверfixtureгенерирует/решаетgrid.
- M00 scopeledger1044paths учёл859original/934legacy/749shared с conservative needs_review verdicts; self-test/checkPASS. Inventory56active/23partial/911pending/0complete. 42itemmodels/7recipes/64texturerefs, безclaimsполногоgameplay.
- Oldvanilla ink semantics доказаныofficialJAR bytecode/mappings, privateevidenceвнеGit; R06 ScribingInk draft вignored.tmp (15directJUnitPASS) ещёнеproduction. Следующий шаг — book primary issuance сpaper/ink/dedup/atomicdelivery, затемtableR07/R08.
- Original/legacy/private assets и JAR не изменялись; публичныйpush/release не выполнялись. Goal остаётсяactive.

## 2026-10-15: пересмотр плана полного переноса

- По запросу пользователя пересмотрен `docs/MIGRATION_PLAN.md` с тремя субагентами. Обновлена отправная точка до проверенного `03fe0782`: 131 JUnit, 24 Python, 17+17 GameTests, 12 просмотренных PNG. Это прежние результаты; новых игровых запусков в рамках планирования не было.
- Уточнены этапы M00–M15, технические пакеты A01–A07, исследовательские units R01–R16 и игровые checkpoints C1–C5. Добавлены зависимости manipulator → VisNet, deconstruction → arcane, harness → jars/accessory contract; original commands/IMC, fake-player adapter, crafting warp, infusion XP и retrogen собственных миров 1.21.1.
- Расширены критерии API/config/resource coverage, учёт 22 исходных локализаций, ранние packaged проверки обоих loaders, protocol/reload matrix, public/private CI и привязка evidence к проверенной сборке. Роль OpenTC4 в source-lock уточнена как modified reference; commit pins и целевые версии не менялись.
- R06 остаётся замороженной незавершённой работой в исходниках. Центральные validate/client smoke после этих изменений ещё не выполнены; новые тестовые сценарии не объявлены пройденными. Документация и metadata плана сохраняются отдельным локальным commit, без включения незавершённого кода и без публичной отправки.

## 2026-10-15: R06 — выдача первичных записок

- Продолжен полный перенос после `35232eaa`. Добавлены key-only request, authoritative admission/dedup, staged paper/ink, native inventory/drop delivery и book GUI/feedback без optimistic changes. Issuance не завершает research и не меняет RP/warp. Независимый review service/network не нашёл блокирующих дефектов текущего default0 пути.
- Подключён проверенный old ink:99→100/count1, creative/Unbreakable bypass, один player nextInt для positive Unbreaking. Сохранён original full-creative discard с расходом бумаги; native null/unspawned drop отказывает без debit. Old InventoryPlayer paper/discard bytecode evidence добавлен к ink audit, private disassembly остаётся вне Git.
- Central `.tmp/r06-validate1.log` exit0:155JUnit/26Fabric/26NeoForge GameTests, без failures/errors/skips. Новые9cases на loader проверяют request/reuse/refusals/order/full inventory/ink/RNG/generation/delivery failure. Python24/extractor/resource/notices PASS; actual XML и remapped artifact hashes записаны.
- Client1 прошёл actual request/S2C до смерти test player от Slime и завершился failure. Test-only entity invulnerability сохраняет survival costs; failed owned world сохранён, fresh isolated retry client2 exit0/SCRIPT_PASSED. Production AI/gameplay не менялись.
- Все14 exact PNG client2 просмотрены root,13/14 независимо другим агентом. Actual outbound request1, paper2→1/ink99→100, одна unfinished NITOR note; missing/duplicate GUI не отправляет intent, immediate client не списывает ресурсы, knowledge/RP/warp неизменны. Full visuals/NeoForge primary client/packaged multiplayer не заявлены.
- Inventory61main/23partial/911pending/0complete; scope1044needs_review,32counterparts. Обновлены status/handoff/plan/README/evidence; добавлена подробная R07 table specification. Следующий код — shared research table, затем hex edits. Полная цель M00–M15 остаётся активной; public push/release не выполнялись.

## 2026-10-15: полный маршрут переноса после R06

- По запросу пользователя уточнён `docs/MIGRATION_PLAN.md` после read-only проверки трёх субагентов: сохранён полный охват M00–M15, добавлены практические волны W0–W7, явные выходы C1–C5 и границы отложенного контента. Последний принятый игровой checkpoint — R06 `7222a5a1`.
- Закреплены зависимости VisNet→manipulator/Excavation upgrades→полный Arcane Bore и warp-driven research gates; добавлены below-class contracts, packet/event/config manifests, публичные поля API и исходящий MFR capture contract. Расширены resource closure, consumer/atlas/UV, production/test isolation, ранний packaged smoke, mixed-loader policy и ранние performance budgets.
- R07 существует в dirty tree и остаётся непринятой: resource/Python проверка FAIL из-за baked atlas references прямоугольных table textures; центральные Gradle/runtime/client запуски ещё не выполнены. Написанные36 cases на loader и17PNG сценарий не названы PASS; реальные unload/reload/restart остаются отдельной проверкой. Проверенный baseline155JUnit/26+26GameTests/14PNG не изменён.
- Обновлены status/handoff. Этот commit содержит только документацию плана; незавершённый R07 код и verification JSON в него не входят. Для documentation-only изменений Minecraft/Gradle не запускались; публичная отправка не выполнялась.

## 2026-10-15: актуальный полный план и очередь после R07

- По новому запросу пользователя три субагента повторно проверили gameplay, платформу/поставку и original scope. Полный маршрут W0–W7/M00–M15 сохранён; добавлены конкретные следующие задачи P01–P06 с результатами, владельцами и независимой проверкой.
- Отправная точка отделена от промежуточного R07 evidence: R06 `7222a5a1` принят; validate3 имеет161 JUnit/38+38 GameTests, client2 —19 просмотренных PNG и process restart; последующий validate5 —39/40 Fabric PASS и реальный cancellation retry FAIL. Четыре UUID adapter/accessor файла ещё не интегрированы и не приняты. Старый atlas FAIL больше не назван текущим блокером.
- Добавлены minimal shared bonus prerequisite R08, post-refund discovery до solver, полный count100/127 lifecycle prerequisite R10, общий spawn-admission/rollback contract, нагрузка небольших C2S, CI required case IDs и внешний addon fixture обоих loaders. Сохранена подробная спецификация R08 как проект будущей реализации.
- Актуализированы plan/status/handoff; ledger snapshots и принятая готовность не повышены. Этот checkpoint содержит только документацию: production, build, worlds и verification JSON не менялись; новые Gradle/client прогоны не запускались. Публичный push/release не выполнялся.

## 2026-10-15: R07 — исследовательский стол и безопасная выдача содержимого

- Реализованы original table craft output1, placement/conversion всех направлений и permissive topology quirks, общий public inventory/menu38slots, predicates/quick-move, save/update, first-native-tick repair, world BER/baked item модели и read-only note GUI. Ink/pools/edit/combine/copy не подменены заглушками и остаются R08–R10.
- Evacuation переносит точные stacks в durable per-level SavedData, очищает physical BE и доставляет только в level-post, сохраняя callback-created replacement inventory. Stable UUID/count/components, batch64, quarantine и over99 entity serialization проверены; player/container codec99 остаётся R10 blocker.
- Actual Fabric ADD cancellation выявила UUID reservation leak (validate5). Guarded snapshot/discard/cleanup с native accessor mixins сохраняет существующих visible/hidden владельцев. Validate6 выявил early wake общей очереди; transient per-output cooldown и явный unrelated-enqueue regression исправили это без ослабления tests/RNG/material semantics.
- Final `.tmp/r07-validate7.log` exit0:161JUnit и41Fabric+41NeoForge GameTests; XML failures/errors/skips0, required case IDs проверены. Actual native cancellation/retry, hidden owner, callback replacement, unload/reload и cold-load repair прошли.27PythonPASS, extractor/resource/notices PASS;494sprites/69refs/66events/111OGG/missing0.
- Original table PNG byte-identical; explicit full-frame static atlas sources/native codec mutation tests, DOWN-face UV probe264vertices и scoped GUI alpha исправлены. Client1 script PASS не скрыл visual-дефект17; повторные client2/client3 просмотрены после исправления.
- Final Fabric client3 exit0/SCRIPT_PASSED, run `client-smoke-91bd518d-9a58-4451-addf-4e3becd205c8`; root19exactPNG, independent15–19 matching hashes. Process restart client2→client3 сравнил весь native Inventory NBT до setup, expectation read-only из owned saved region. Source fixture world не открывался; level.dat hash совпал.
- Обновлены evidence с final JAR hashes, inventory81main/33partial/901pending/0complete, scope1044needs_review/49counterparts, status/handoff/plan/README. R07 implementation checkpoint не закрывает NeoForge/packaged visual приёмку, full visual/audio parity или весь M01. Следующая работа A07 production/test isolation и R08 shared bonus/hex editing. Публичный push/release не выполнялся; полная цель остаётся активной.

## 2026-10-17: M09b — настоящий VisNet, принят и экспортирован (Fabric-only)

- Реализованы обычный node transducer, energized source с обновляемым per-source-tick raw CV budget, relay graph и Terra-ускорение кристаллизатора. Сохранены original MAX reduction, 1001/951 переходы, первый нулевой pulse, CURRENT0 при возврате и ordinary zero-key pruning. Исправлены исходно подтверждённые flux9/Nausea/goo-tail native расхождения; special nodes/advanced tier/other consumers/full API остаются открытыми.
- После реального клиента финальный `visnet-fabric-validate7`: **220 JUnit / все105 Fabric / 72 Python (95.133с)**, failures/errors/skips0, **3827 неизменённых входов**. Проверены свежие XML, production ZIP/notices/test isolation/one intermediary AW/recipes/resources. NeoForge не запускался.
- `visnet-fabric-client5` SCRIPT_PASSED:255.1941779с/5103 client ticks, все10 точных PNG154–163 осмотрены root. Три настоящие оплаты, две зарядки другого узла, mining/pickup/reinstall прежнего кристаллизатора, three60tick cycles по94rawEARTH и full1000tick baseline без энергии, четыре новых физических outputs. Итог282CV; прямое source питание исключено расстоянием²197, relay пути²49/50.
- Независимые normal-stop tail и stopped-NBT audit: count1091<1200, BASE80/NORMAL/NONE/шесть нулевых current keys;95non-nodeBE/20jars333/72candles, oldstock/gear/wands/foci/XP11 и именованная cowhealth6 сохранены, pending/quarantine пусты, все183parentfiles неизменны. Failedclient1–4 worlds/diagnostics сохранены и не используются как accepted input.
- Source-bound evidence: `docs/porting/visnet-client.json`, `docs/porting/verification.json`, accepted archive `.tmp/visnet-accepted-verification7.json`. Экспорт **10,556,826bytes / SHA256 bac83fff54504682f7a00523d22f10e00a334eb922f559f93ea27bf349408caa**. Прежний M09a сохранён внеmods в `backups/stabilizer-fabric-validate2/`; dependencies/olderbackups неизменны, README/hashes обновлены.
- Это не fullTC4/fullvisual/audio/ordinarypackaged/EULAserver/remote/survival/NeoForge приёмка. Сразу начата следующая boundedFabric unit: focal manipulator + Excavation upgrades перед Arcane Bore; original audit/контракты, пока не новая реализация. Public push/release/commits не выполнялись.
