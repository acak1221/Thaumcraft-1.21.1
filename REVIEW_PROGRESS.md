# Ревью репозитория — 2026-11-02

Исходное состояние: `main`, коммит `43246a129aecc4e31fe818e7fc39a38edfa1c96b`, рабочая копия чистая. Проверки завершены до публикации. Последующей отдельной командой «запуш» пользователь разрешил commit/push в main. Оригинальные JAR, legacy `src/` и donor assets не изменяются.

## Архитектура и план

Java 21, Minecraft 1.21.1, Architectury и Gradle 8.12.1. `common` содержит игру, публичный API, NBT/data components, пакеты и клиент; `fabric`/`neoforge` подключают загрузчики и платформенные события. `compat/{emi,jei,jade}` включаются в loader source sets; `examples` проверяет API аддонов. Состояние хранится в мире Minecraft и данных игроков; конфигурация — в локальных properties. Основные границы доверия: C2S-пакеты/меню, серверные действия, NBT/datapacks и callbacks/API сторонних модов. Web-аутентификация, SQL, XSS/CSRF здесь не применимы.

Проверка: исходные тесты/сборка → трассировка подсистем → воспроизводящие тесты до исправлений → минимальные исправления → регрессии, Fabric-сборка и изолированный обычный сервер. Клиент запускается для затронутых визуальных сценариев. NeoForge читается, но его сборка/запуск отложены действующей инструкцией Fabric-first; это не будет обозначено как проверенный runtime.

## Статусы

| Область | Текущий статус |
|---|---|
| Git, инструкции, архитектура | Проверено; main/43246a12 сохранён, изменения только локальные |
| Java inventory | Исходные 1626 имеют записи статического просмотра; + 7 новых NPC helpers/mixins, текущий набор 1633. Full/control-flow/partial geometry различены |
| Сеть, меню, crafting/transfer | Исправлено/проверено: authority, reentry/cancellation, paid outputs, codecs; focused tests и final native 656 PASS |
| Машины, NBT, output custody | Исправлено/проверено; V5 и backward reads, реальные hopper/fluid/callback/reload сценарии; final 656 PASS |
| Entities, AI, golems, village/worldgen | Исправлено/проверено; normal-Brain retention/restock 3 PASS, final 656 PASS; runtime не покрывает всю survival progression |
| Клиент/EMI/render/reload | Final client 25 PASS, 6 PNG открыты; earlier focused EMI 84 PASS. Весь visual corpus и 7 больших geometry literal bodies построчно не заявлены |
| Loader glue, CI, Python/examples | Python: 177 distinct PASS / 3 SKIP из 180 после oracle replay; asset missing 0, 4 isolated CI casesPASS. Neo runtime заблокирован Fabric-first |
| Финальная сборка и ordinary JAR | Snapshot 8 Fabric build/isolationPASS; exact remapped JAR ordinary server50ticks/banker AI/save/stopPASS |


Исходная команда: `./gradlew --offline --console=plain :common:test :fabric:build :fabric:verifyProductionIsolation --rerun-tasks` — PASS, 55 s. Лог: `.tmp/review/baseline-gradle.log`; сохранены исходные счётчики и копия alpha.17 JAR. Предыдущие результаты других задач не подменяют текущие проверки.

Итерация завершена: final656 +4 CI order cases, ordinary packaged-JAR smoke и окончательный отчёт готовы. Следующие действия по непроверенным/неоднозначным областям отдельно перечислены в CODE_REVIEW_REPORT.md; Neo runtime остаётся заблокированным инструкцией Fabric-first. Ниже сохранена история итераций; ранние результаты не подменяют поздние. Инвентарь и журналы чтения — `.tmp/review/`; полный построчный охват не заявлен.

## Итерация 1: воспроизведение и исправления

На исходном поведении новые unit checks дали 9 ожидаемых отказов из 11: конфигурация, native infusion view, CustomName/quarantine, переполнение vis и отсутствующие loader access rules. Native checks дали 7 отказов из 8: два trunk/payload owner из одного placer, потеря оплаченного результата при callback-close, повторная эмиссия зеркала с IndexOutOfBounds, following-item arrival/save и underpriced API recipe. Контроль отмены/creative размещения trunk прошёл. Логи: `.tmp/review/unit-red.log`, `.tmp/review/native-red.log`; сохранены JSON/XML.

Минимальные исправления вводятся по воспроизведённым причинам; green-результат пока не заявляется. Дополнительно проверяются точные варианты жидкости, immunity special items, creeper AI, реальный EMI и безопасное поведение локальных merge/evidence scripts. Первое повторное native выполнение остановилось на compile-неоднозначности нового `setTarget(null)`; это исправлено явным типом и не считается игровым результатом.

## Итерации 2–3: дополнительные границы и соседние сценарии

Native RED подтвердили: выдачу прав чужой защите ключом из другого измерения; обработку мотыгой и переносной дырой соседей за WorldBorder; потерю оплаченного входа настоящей воронки в решётке; неверную explosion immunity, дистанцию Taint Creeper и зависший cooldown Eldritch Crab. Дополнительно выявлены дублирование/потеря привязанного ключа при полном инвентаре и callback. Пример SparkFocus проверяется компиляцией его реального source в изолированный GameTest harness; первый ray fixture был отклонён и исправлен, затем actual border failure подтверждён.

Первый полный unit GREEN: **2889 tests, 0 failures/errors, 5 skipped**, `.tmp/review/unit-green-1.log`. Реальный EMI: RED 42/5, после исправления **84/84 PASS**, включая новую registry instance после resource reload (`.tmp/review/emi-green-1.json`). Полный безопасный Python discover: **169 tests, 0 failures, 9 skipped**, `.tmp/review/python-full.log`.

Расширенный native GREEN: **37/38**. Оставшийся существующий packet-roundtrip test выявил ещё один реальный контрактный дефект: JSON принимает пустой tag, но два infusion serializers отвергают его разрешённое wire-представление. Отдельный RED7 подтвердил оба случая при PASS arcane/crucible controls. Исправлена только wire-валидация; строгие JSON, matching, стоимость и bounds сохранены. Предстоит повторный native прогон, финальные unit/build и запуск обычного JAR в изолированном localhost-мире.

Полнота: журнал покрывает несколько итераций, но не является заявлением о полном построчном ревью 1626 поддерживаемых Java-файлов. Подробные фактически прочитанные и частичные области перечислены в manifests `.tmp/review/`. Пропуски автоматических oracle-проверок не подменяются прежними результатами.

## Итерация 4: широкий серверный прогон и оставшийся код

Первый полный Fabric GameTest: **610 выполнено, 608 PASS, 2 FAIL** (`.tmp/review/final-full-1.log`, XML). Common tests, сборка Fabric и production isolation в той же команде прошли. Отказы: устаревший test-only счётчик live research (203 против ожидаемых 201 bundled) и утечка forced chunks при ошибке отложенного сценария резервуара. Первый счётчик исправлен на `ResearchCatalog.bundled()`, значение 201 сохранено; резервуар разбирается, isolation guard не отключается.

Отдельно завершены журналы чтения оставшихся шести Java-разделов: API/config, blocks/tiles, entities/world, render/models, client/UI/FX, loader/core. Часть файлов читалась по control flow, геометрия не везде построчно; это не равнозначно полному runtime-покрытию. Кандидаты из этого прохода проходят отдельный цикл RED→fix→GREEN.

RED Aspect API: 3 unit tests, 2 воспроизводимых ArrayIndexOutOfBounds для разрешённых primal-массивов длиной 0/1, совместимость len>=2 PASS. Исправлена проверка длины. Первый запуск native tail с сокращённым filter отклонён самим harness (требуются точные имена); игровых результатов в нём нет, повтор запущен с 12 точными именами. Новые renderer checks подготовлены для настоящего клиентского BufferBuilder.

Safe Python discover после дополнения required-case проверки: **173 tests, 0 failures, 9 skips**. Ограниченный поиск форматов секретов в 8834 tracked text files: 0 совпадений; история git, все виды секретов и база уязвимостей этим не проверены.

## Итерация 5: дополнительные подтверждённые дефекты

Tail native RED12/12: enchantment API empty tag, direct/causing damage attribution, лестницы Outer Lands, нормализация Shapes, inverted Eldritch Nothing damage, crystal orientation NBT/update, warding onGround, pedestal custody, mirror binding cancellation/reentry, variant item collision, crucible replacement callback. Минимальные исправления выполняются по этим RED.

Entity native RED5/5: Watcher native controls/fire flag, slime spit radius/flattened loot ID, transient axe NoGravity persistence. Utility RED2/2 unit +3/3 native; после устранения null CollisionContext второй RED подтвердил MISS/pickability contracts. Один Use-тест отклонён по проваленной fixture-precondition; Gather повторяется с корректными native tickCount, эти наблюдения пока не выдаются за окончательное доказательство.

Worldgen native RED8/8 подтвердил: deferred room view, taint biome, Nest mask, skull reward ID, fibre TYPE, boss barrier family/collision и mob-only ward fence. К этим исправлениям добавляется проверка открытия двери.

Настоящий клиент renderer GREEN **10/10** (`.tmp/review/renderer-green-1.json`), три PNG открыты через view_image, ограниченный визуальный результат записан в `.tmp/review/visual-inspection.json`. IMAGE reload отдельный RED **18 checks /2 failures**: настоящий ResourceManager уже обновлён, но IMAGE page всё ещё использует отсутствующую/старую текстуру. Cache reset добавлен в существующие reload hooks, GREEN предстоит.

Фокусный резервуар + bootstrap GREEN **2/2**, Aspect unit GREEN **3/3**. Исходный редкий reservoir failure в полном соседстве пока не локализован; guard сохранён и теперь выводит suppressed cause. Финальный широкий прогон обязателен после завершения исходников.

## Финальный широкий прогон 2

После всех production-исправлений: JUnit **2895/0 failures/0 errors/5 skipped**, Python **180PASS/9 skips**, asset references missing0, Fabric build/clientTestClasses/production isolation PASS. Источник зафиксирован8465fingerprints и проверен неизменным до окончания запуска. Native **652 выполнено,651PASS/1FAIL** — только reservoir после12ticks не получил эссенцию; буфер сохранил7единиц, утечек ресурсов теперь нет. Это не засчитано зелёным полным результатом. Логи/XML: `.tmp/review/final-full-2.*`.

Обычный локальный Fabric-сервер с точным собранным JAR, отдельным новым1.21.1миром и loopback25608: READY,50обычных тиков, save-all/normalstop, exit0, mapping errors0. SHA JAR2692e08a89617a369f1b977b31807029414034131821a7a712415f6503e4f07b. Это local artifact smoke, не deploy/release и не многопользовательская приёмка.

Следующий шаг: подтвердить readiness уже принудительно загруженных чанков резервуара до начала строгого12tickокна, сохранить все assertions и снова выполнить весь native набор. Report72строки findings остаётся с pending финальной validation; готовый source не коммитится.

## Дополнительная проверка callback contracts и живых NPC

Независимый peer review выявил регрессию первоначальной правки тигеля: копия до CRAFT теряла изменения live output. Новый native RED подтвердил имя/count128/empty/throw и преждевременный flush из actual REMOVE. Введены transient hold и finally reconciliation без изменения persistent schema. Отдельный RED верстака подтвердил потерю оплаченных4emerald при RuntimeException/Error в открытом/закрытом меню; добавлено восстановление live output с прежним exception. Focused **5/5PASS** вместе с прежними close/cursor/replacement controls.

Snapshot6 (8467 inputs) оставался неизменным до завершения: common JUnit2895/0/0/5skip, Python180всего/171PASS/9SKIP, Fabric build/isolation/client18PASS, full native **654/654PASS** и все4отдельных CI order scenarios PASS. Архив `.tmp/review/order-green-6/`, build/client/Python logs `*-6.log`.

После этого добавлен normal-Brain village check: действительный banker, созданный VillageBankerHome.build, и configured wizard теряют профессию на первом AI tick. Farmer vanilla-control также сбрасывается ожидаемо. Native factory guard читает live profession (не captured constructor NONE), сохраняет AI/XP/level. Role GREEN; stock RED теперь корректно подтверждает отсутствие original40idle replenishment (uses7/max7). Исправление рестока проходит отдельную итерацию до следующей окончательной сборки. Wizard Tower out-of-bounds original no-spawn quirk не менялся.

## Завершение NPC-регрессий

Normal-Brain сохранение BANKER/WIZARD и оригинальное пополнение disabled offers через40 idle ticks прошли **3/3 native checks** (`village-profession-restock-green`). Материалы NPC: RED23/4FAIL → GREEN23/0FAIL, только две TC4 профессии подключены к исходным full skins; farmer сохраняет native biome/profession/rank overlays. Root открыл front/oblique/back PNG: wizard face закрыта modern hat/rim, отсутствующими в исходном1.7.10 ModelVillager. Это отдельный визуальный RED, а не успешная визуальная приёмка. Добавляется узкая geometry-регрессия.

Required Fabric acceptance синхронизирована: **656 authored cases =382shared +274Fabric-only**. Neo runtime не запускался. Новые final source fingerprints и широкий прогон будут сделаны после заморозки последних client changes.

## Окончательный snapshot 8: сборка и клиент

8475 fingerprints созданы до проверок. JUnit2895всего/2890PASS/5SKIP, Python180всего/171PASS/9SKIP, asset missing0, ApiReference --check20files up-to-date, Fabric build/isolation PASS. Native client **25/25PASS**: original banker/wizard216vertices с head=true,hat=false,rim=false, фермер264 с head/hat/rim=true; после draw исходные flags восстановлены. Root открыл все6actual world PNG, включая NPC front/oblique/back: глаза и лицо wizard снова видны, farmer hat сохранена. Область визуальной приёмки ограничена показанными supplied fixtures; zombie/cure/все варианты не заявлены.

Полный656native +4отдельных CI order scenarios выполняются. После них будет проверен точный remapped JAR в новом обычном локальном мире и финализированы отчёт/статусы.

## Завершение проверки

Итоговый snapshot 8 (**8475inputs**) неизменен. Common JUnit **2895total/2890PASS/5SKIP**, full Fabric native **656/656PASS**, отдельно4CI order cases **4/4PASS**, real client **25/25PASS** и6открытых PNG. Все authored default native cases обязательны; shared382сохранены. Python full180дал171PASS/9SKIP; доступный source oracle затем явно подключён TC4_ORACLE, и все6его пропущенных проверок прошли. Итого **177distinctPASS/3SKIP**; повторы не увеличивают число distinct tests. Остальные3Python и5JUnit требуют отсутствующий original TC4 JAR (ConfigAspects также MCP mapping). ApiReference20files и отдельный Reference unittest PASS; asset missing0.

Ordinary remapped JAR **15984509bytes**, SHA256`575fccdf5776f1529f12d2435b09620214acbb706294e7506e48042d71936bf3`: новый isolated1.21.1world, loopback25608, normal ticks92→142, banker profession сохранена, Y изменился до поверхности (actual entity ticks), save-all/normalstop,exit0,mapping errors0. Первый NPC smoke timeout был fixture-дефектом spawn-npcs=false; native bytecode подтвердил discard. Исправлен только новый стенд, повтор в новом мире PASS. Предыдущие логи/мир сохранены.

Контрольные доказательства: `.tmp/review/verification-final.json`, `order-green-8/`, `renderer-final-8.json`, `visual-inspection-final-8.json`, `artifact-smoke-final2/production-result.json`, все final*-8логи. CODE_REVIEW_REPORT.md содержит76сгруппированных исправлений с current anchors/тестами; это не число всех возможных багов. Отдельный source-confirmed Neo damage reentry P2 оставлен без непроверенного фикса по AGENTS Fabric-first. Oracle candidate-pool/flattened family equivalence, whole visual corpus, multiplayer/survival/Neo и CVE scanner не заявлены выполненными.

Git:171changed/newpaths относятся к этому заданию, trailing whitespace0, main/HEAD43246a12не менялись; original/donor/legacy Java не затронуты. На момент завершения ревью commit/push/merge/deploy/release не выполнялись. Итоговый отчёт и текущие статусы завершены; ранние failing результаты выше оставлены как история фактических итераций.

## Отдельная команда на push

После завершения ревью пользователь дал прямое разрешение: «запуш». Scope повторно сверен независимым read-only проходом: те же 171 файлов, 8475 source fingerprints неизменны; stageable build/runtime artifacts отсутствуют. Для публикации используется существующая main, вместе с исправлениями, регрессионными тестами и отчётами. После validation изменены только пояснения в двух отчётах о новом разрешении.
