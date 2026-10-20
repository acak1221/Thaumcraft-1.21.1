# Полный перенос — активная работа 20.10.2026

Запрос пользователя: продолжать до полного переноса, с субагентами и коммитами в репозиторий.
Ветка `work`, база контента `0fa4a08`, CI commit `175ee4c`; `origin/main` обновлён и является предком текущей ветки.
Окончание группы — checkpoint, не завершение задачи. Сборки Gradle только последовательно у ведущего;
перед сборкой замораживаются все source sets всех агентов. Original oracle вне репозитория.

## Текущие владельцы

| Агент | Область | Общие подключения |
|---|---|---|
| root | R13 settings, общие registry/bootstrap/book/lang, интеграция, сборки, Git/CI | Только root меняет общие файлы без явной передачи |
| next_content | R11 unknown knowledge, R12 clues/entity scans, затем R15/API | Общие wrappers временно переданы и возвращены root |
| review_reservoir | Ward behaviour, Excavation specials, затем Fire/Frost/Shock | Владеет согласованными focus contracts и mixin entries |
| review_tests | Deconstruction, затем UNSTABLE nodes + aspect orbs | Closed paid orb queue; root bootstrap/render integration |
| devices | Growth/Fertility, затем cheat sheet и advanced furnace | ClientSetup временно передан и возвращён root |
| enchant_infusion | Repair/Haste, Bore repair, затем runic armour | Владеет matrix/recipes и согласованным WandManager |
| warp | Brainy/Giant zombies, затем Firebat/Wisp | Новые entities/render/network; root общие подключения |

## Остающиеся крупные системы

Ни один M00–M15 пока не закрыт полностью. Помимо текущих областей остаются остальные R11–R16,
особые узлы и полная генерация/структуры, оставшиеся фокусы и их улучшения, аксессуары и runic,
все устройства/продвинутая печь/зеркала, остальной бестиарий/Пех/культисты/заражение,
Внешние Земли/лабиринт/боссы/награды, полный API и отдельный аудит соответствия.
Полный каталог рецептов/аспектов/исследований должен замкнуться на естественное получение.
Финальные клиентские, restart/chunk, multiplayer и survival проверки остаются обязательными;
после готовности Fabric — NeoForge parity и packaged проверки. Успешный GameTest не закрывает их.

Точные исходные контракты, проверенные результаты, ограничения и хэши записываются в analysis docs,
HANDOFF/MIGRATION_STATUS и MODLOG с каждым принятым checkpoint.

## Передача полного прогона GitHub Actions

По запросу пользователя 20.10 полные регрессии выполняются в CI для неизменяемого commit.
Локально остаются компиляция и focused проверки; длительная CI-проверка больше не блокирует
следующую source window. Кэш Gradle/Loom включён, global --rerun-tasks убран, JUnit запускается
с task-specific --rerun, GameTest — обычным uncached JavaExec. Результаты CI сохраняются 7 дней.
Нельзя приписывать PASS изменённому рабочему дереву: фиксировать exact commit/run.

Текущая интеграция: R10 copying/whole stacks, Thaumatorium+Brainbox, 22 enchantment-infusion
recipes/engine, Warding/Architect, Trade, Lamp/Amber/VisChargeRelay, Warp dispatcher/effects/mobs.
582 JUnit и 78 Python прошли; integrated3 дал 185/186, integrated4 — 184/186. Последние
два сценария исправлены в тестовых fixtures (native connection tick и cached source discovery),
финальный результат этой группы подтверждён CI ниже. Клиент не осматривался.
Следующая группа R11/R12, Repair/Haste, Deconstructor, lamps, zombies и Ward completion уже в active source; её CI ещё не принят.

CI `d508ada` завершён SUCCESS: 582 JUnit,186/186 GameTest,78 Python; fresh-result/attribution gates passed.
Run: https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39398016295; first cold Gradle execution 7m6s, server56.67s. Agents worked on the next source window during this run.
Active next groups: R11/R12, Repair/Haste+BoreRepair, Ward completion+Excavation specials,
Growth/Fertility, Deconstruction, Brainy/Giant zombies, root R13 settings.
