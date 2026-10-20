# Полный перенос — активная работа 20.10.2026

Запрос пользователя: продолжать до полного переноса, с субагентами и коммитами в репозиторий.
Ветка `work`, база контента `0fa4a08`, CI commit `175ee4c`; `origin/main` обновлён и является предком текущей ветки.
Окончание группы — checkpoint, не завершение задачи. Сборки Gradle только последовательно у ведущего;
перед сборкой замораживаются все source sets всех агентов. Original oracle вне репозитория.

## Текущие владельцы

| Агент | Область | Общие подключения |
|---|---|---|
| root | Focus Trade, общие контракты/registry/bootstrap/book/lang, интеграция, сборки, Git | Только root меняет общие файлы |
| next_content | R10 copy и overstack lifecycle, затем R11 | table classes/network новые; GUI/bootstrap root |
| review_reservoir | Focus Warding, затем общий Architect | новые ward/focus classes; общие focus contracts root |
| review_tests | Thaumatorium | новые classes/GUI/resources; существующий crucible не менять без согласования |
| devices | Arcane Lamp/Amber, затем Vis Charge Relay | новые device classes/resources; root bootstrap/lang/book |
| enchant_infusion | Enchantment infusion, matrix integration; затем Repair/Haste/runic | Владеет TileInfusionMatrix и новыми recipe classes |
| warp | Полный Warp dispatcher/custom effects/MindSpider/Guardian/mist | новые classes, registry/packets/render; root общие подключения |

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
финальный общий серверный результат ещё ожидается в CI. Клиент не осматривался.
Новые R11, Repair/Haste, Deconstructor, lamps и Ward completion пока только .tmp drafts.
