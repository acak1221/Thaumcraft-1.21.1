# Исправления замечаний ревью — 25.10.2026

Базис: `c4a8f7a8cc5f56458d6afe7a5551aa31d6957eff`, ветка `work`. Исправления находятся в рабочем дереве. Исходный [отчёт ревью](2026-10-25-deep-review-ru.md) сохранён как описание проверенного коммита; его ссылки и выводы не переписаны задним числом под исправления.

Внесены исправления R01–R18, исправлен кэш D1 и завершён путь получения/размещения essentia mirror D2. Отвергнутые кандидаты не превращались в дополнительные gameplay-правки. Мёртвый `SlotCraftingArcaneWorkbench` оставлен совместимым экспортом с явной deprecation/documentation; активный workbench продолжает использовать атомарную транзакцию.

## Изменения и доказательства

| Замечания | Что изменилось | Проверка |
|---|---|---|
| R01/R06/R07/R08: пьедестал | Целые VIS остаются единицами узла; debit1 и credit до100cV выполняются до уведомлений. Восстановлены резерв, частичная последняя единица, compound→primal только с реально установленным усилителем. Клиент не изменяет stock; вставка/изъятие/очистка отправляют update независимо от узла и зарядки | Три настоящих GameTest: обе зарегистрированные формы, iron/gold/staff, амулеты, node50/200/1, room1/50/99/100/full, отказ источника, actual chunk update и содержимое пакета |
| R02/R16: Liquid Death | Перенесено конечное распределение четырёх квантов по Forge1.7.10; source дебетуется, вертикальный перенос очищает исходную клетку. Использован существующий транзакционный writer, убран второй fluid scheduler. Контакт вызывает dissolve. По проверенному родителю восстановлены tick7, immersion height и original metadata пузырей | Exhaustive unit conservation/distribution и native world flow/fall/merge/repeated/scheduled ticks, cow contact с зарегистрированным dissolve |
| R04/R05: зеркала | Link читается до расходования singleton. Обратная ссылка проверяет измерение текущего зеркала, без hashCode вместо dimension identity | Настоящие survival placement/binding/drop/replacement, обычное и essentia зеркало; два ServerLevel, item count/components, расход эссенции, save/reload BE |
| D1: живой кэш зеркала | Неуспешный scan инвалидирует старые sources и сохраняет исходную задержку5000ms | Реальные банки, удаление источника и новый источник в другой позиции; тест ждёт пять настоящих секунд, не подменяет часы и не сокращает backoff |
| D2: essentia mirror | Добавлены отдельный item `thaumcraft:mirror_essentia`, выбор начального ESSENTIA state до создания BE, binding/drop/pick, recipe MIRRORESSENTIA по oracle, book mapping/icon, модель и en/ru названия. Старый block/item ID обычного зеркала сохранён | Loaded recipe predicate/result плюс native item placement и перенос. Fixtures не объявлены полным survival-acquisition replay |
| R09: аксессуары | CHANGE_DIMENSION безусловно отправляет полный snapshot, обходя dedup неизменившегося ServerPlayer | Реальный teleport в Nether и обратно, native outbound payload, точные компоненты, отсутствие повторной отправки при обычном unchanged tick |
| R10: API-рецепты | Runtime bridge всех пяти add*Recipe сохраняет исходные callback-объекты, порядок и stable holder IDs. Arcane/Crucible/Thaumatorium/InfusionMatrix исполняют регистрации; paid infusion session сохраняет output/cost/XP/component predicates и переживает reload | Реальные menu/тигель/инфузия/зачарование, research gates, точный расход items/VIS/эссенции/XP, output, reload, collision/unregister/re-register IDs |
| R11/R12: predicates и hooks | Явный component patch required-ингредиента проверяется; plain requirement разрешает дополнительные данные. CUSTOM_DATA использует оригинальное top-level subset правило. Arcane virtual comparator и protected enchantment comparator участвуют в matches; callback получает копии предложенных stacks | Unit positive/negative/default/extra-data cases; native subclass veto и защита caller stacks от мутации comparator |
| R13: craft event | Оплата остаётся перед произвольным callback, но listener получает независимую pre-craft matrix/wand snapshot | Настоящий menu click: внутри callback виден уже оплаченный physical inventory, snapshot содержит исходные items/name/count/VIS; mutation snapshot не изменяет physical remainder |
| R14/R15/R17: HUD | Общий HUD восстановил focus icon, cooldown, Trade selected block/count и Shift-цену; main/offhand выбираются одним helper, история разделена по источнику. NeoForge callback теперь вызывает общий HUD | Шесть JUnit-тестов фактических paint operations, native hand inventories/modes; регистрация NeoForge независимо проверена статически |
| R18: FocusTrade | Обе anchor rays используют blockInteractionRange игрока | Native use/left click с range6 и целью между5–6; range3 с целью между3–5 отказывает без оплаты; positive swaps платят ровно один раз |
| R03: NeoForge Spa | Установлен FluidAppearance bridge через фактический IClientFluidTypeExtensions21.1.259 и block atlas. Pure использует sprite своего общего world renderer, поскольку WATER_TYPE заимствован только для физики; сохранён белый tint | Независимая статическая сверка loader lifecycle, API сигнатур, Fabric/oracle textures; NeoForge runtime не проверен по ограничению AGENTS.md |

Во время реального исполнения R10 обнаружен дополнительный корень проблемы: native username research backend никогда не получал начальный Server reference. Исправлены lifecycle bind/clear и player-scoped dispatch; старые addon handlers с username override продолжают работать через default fallback. Тесты проверяют первый username-запрос сразу после входа, до recipe lookup. Серверный контекст больше не восстанавливается случайным обращением к старому Player после остановки сервера.

Также устранена рассинхронизация меню Thaumatorium при регистрации рецепта только на сервере: opening data несёт ограниченный display-only snapshot. Сервер и клиент используют один порядок/размер списка, callbacks остаются на сервере. Регрессия удаляет локальную регистрацию перед декодированием, проверяет ID/output components/costs и отвергает некорректные размеры. Это проверка настоящих menu/codec путей, не запуск физического клиента.

## Проверки

Среда: официальный Temurin JDK21.0.13+9, wrapper Gradle8.12.1. Исходная среда имела JRE без `bin/javac`; полный JDK помещён только в ignored `.tmp/fix-review/jdk21`, SHA-256 архива проверен (`d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c`). Gradle получил sandboxed network grant для локальных сокетов; разрешение зависимостей оставалось `--offline`.

Команды запускает один координатор проверок с `JAVA_HOME=<repo>/.tmp/fix-review/jdk21`, `GRADLE_USER_HOME=<repo>/.gradle21`, `./gradlew --no-daemon --offline`:

| Проверка | Результат |
|---|---|
| `:common:test` | **2544 теста,0 failures/errors/skips** |
| `:fabric:build` | **BUILD SUCCESSFUL**, remapped production JAR собран |
| `verifyTestIsolation` / `verifyProductionIsolation` | **PASS**, test harness не попал в production JAR |
| Focused `:fabric:runGameTest -PgameTestFilter=<14 review cases>` | **14/14 PASS** в одном финальном прогоне |
| Полный `:fabric:runGameTest` без фильтра | **459/459 required PASS**,0 failures/errors/skips, нормальный save/stop, Gradle exit0,2m47s |
| `TC4_ORACLE=/workspace/work/thaumcraft-4.2.3.5-oracle python3 -m unittest discover -s scripts/porting -p 'test_*.py' -v` | **112/112 PASS**, без skips |
| `python3 scripts/porting/convert_assets.py --verify` | **PASS, missing_references0** |
| `python3 scripts/porting/update_scope_ledger.py --check` | **PASS**; это согласованность учёта, не доказательство полной TC4 parity |
| `git diff --check` | **PASS** |
| NeoForge build/tests/client | **Не выполнялись**, запрещены AGENTS.md |
| Физический клиент / driver / два клиента | **Не выполнялись**; в среде нет Xvfb/display tooling, inspected frames не заявлены |

Промежуточные красные результаты сохранены. Первые ошибки выявили недостающие названия/research mapping нового mirror item и скрытый backend research bug. В тестах устранены доказанные ошибки fixture: vanilla magma-cream recipe имел законный приоритет, GameTest mock не переключался в spectator, ray проходил через внешнюю геометрию, accelerated ticks не равнялись wall-clock backoff. Production priority, часы и backoff ради тестов не менялись. Assertions сохранены и усилены явными preconditions/реальными actor/packet paths.

Контракт ресурсов обновлён после чтения GUI diffs и конечных resource expansions, а не путём слепого refresh hashes. Добавлены mutation checks удаления обеих research backgrounds и research book sheet; пропажа каждого ресурса должна по-прежнему ломать verify. Новые computed recipe IDs описаны как нересурсные идентификаторы; от них не создавались фиктивные assets.

## Доказательства и пределы

Полные журналы и отчёты агентов находятся в ignored `.tmp/fix-review/`; координатор ведёт `build-checks.md`. В `final-inputs-before-checks.json` сохранены SHA-2566118 входных файлов с базисом коммита; SHA-256 этого manifest — `d34461ddb04c3b87461dc653b084c572647e8f553bc323e6e619d0db4d55cd3a`. До и после финального прогона снимок совпал: изменённых source/test/resource inputs0. Последующие изменения документации отдельно отличимы от этих входов.

Итоговый Fabric JAR: `fabric/build/libs/Thaumcraft-fabric-0.1.0-alpha.5.jar`,15276472bytes, SHA-256 `3b36bab769c686efe470625aedb1d9844a0aa6c8a66c0135a1dee73b21b0fe13`. Он собран локально и не опубликован. Основные логи: `gradle-final-focused.log`, `gradle-full-gametest-final.log`, `python-tests-final.log`, `assets-verify-final.log`, `ledger-check-final.log`; копия native XML — `gametest-final-459.xml`.

Исправления independently cross-reviewed: pedestal↔mirrors, finite fluid↔pedestal, API↔finite-fluid/root, HUD/NeoForge↔root, root lifecycle/crafting/reach↔HUD. Дополнительные проблемы, обнаруженные регрессиями, исправлены в production с соответствующими тестами.

Пройденные headless проверки не заменяют визуальную приёмку, latency/multiplayer, физический NeoForge client, произвольные аддоны и полное прохождение survival. Original/donor JAR и `thaumcraft_src` не изменены. Push, PR, публикация, платный CI и commit не выполнялись. Полный перечень изменённых/созданных файлов и финальный Git status сохраняются в `.tmp/fix-review/final-git-status.txt` и `created-files.txt`.
