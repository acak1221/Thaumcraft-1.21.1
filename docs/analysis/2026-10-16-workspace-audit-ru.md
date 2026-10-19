# Обзор всего рабочего набора и продолжение переноса

Срез: основной read-only обход 15.10.2026, восстановление после перезапуска и
продолжение 16.10.2026. HEAD на момент восстановления — `ac1734af`.
Это инвентаризация корпусов и адресный анализ поведения, **не заявление о
построчном прочтении каждого файла или полном переносе**. Состояние исполнения
проверок фиксируется в `../MIGRATION_STATUS.md` и `../porting/verification.json`.

## Что находится в папках

| Путь относительно workspace | Назначение и охват |
|---|---|
| `Thaumcraft-4.2-FOREVA/common/src/main` | Активные общие gameplay/API/resources 1.21.1 |
| `Thaumcraft-4.2-FOREVA/fabric`, `neoforge` | Loader bootstrap, platform integrations; A07 отделяет test-only source sets |
| `Thaumcraft-4.2-FOREVA/src` | Read-only справочник 1.12: 934 main + 530 test Java; не компилируется modern build |
| `Thaumcraft-4.2-FOREVA/thaumcraft_src` | 1096 assets/metadata файлов, 0 Java; не original Java oracle |
| `Thaumcraft-4.2-FOREVA/raw` | 3 JPEG, не игровые исходники |
| `Thaumcraft-4.2-FOREVA/docs`, `scripts`, `gradle`, root build files | Планы, provenance, manifests, конвертеры, тесты, Java21/Gradle8.12.1 сборка |
| `work/thaumcraft-4.2.3.5-oracle` | Приватная original gameplay specification: 859 Java; read-only вне repository |
| `work/references` | OpenTC4: 2050 tracked файлов; Architectury ref: 6129. Пины в source-lock, оба лишь references |
| `work/note-generator-oracle` | Приватный generator oracle, 516 Java8 и 516 Java21 vectors; результаты byte-identical в проверенном срезе |
| `work/vanilla-1.7.10*` | Vanilla binaries/disassembly и 3 private Java для inherited semantics |
| `work/jdk21`, `work/gradle8`, архивы toolchains | Инструменты, не игровые исходники |
| `outputs/thaumcraft-1.21.1` | 7 PNG 1280×720, не дистрибутив и не актуальный полный client evidence |
| `.tmp`, `.gradle*`, `build`, `run*` | Scratch, caches, результаты проверок и test worlds; не считать реализованным контентом |
| Оба `%SystemDrive%` | По 4 Windows cache DB в исследованном срезе; DB не открывались |
| `NVIDIA Corporation` | Пустой каталог в исследованном срезе |

В исследованном repository срезе — 5658 tracked файлов. Все 1464 legacy Java
пути есть в прежнем CSV; raw SHA-256 совпали. Original/legacy union:
**1044 = 749 shared + 110 original-only + 185 legacy-only**. Совпадение пути не
равно семантическому соответствию. У всех 1044 verdict пока `needs_review`;
33 partial / 901 pending / 0 complete — только authored class mappings.

После выделения A07 и добавления pure R08 текущий main inventory содержит
72 Java, а scope — 50 unique native counterparts. Изменение счётчика main
объясняется выносом тестов, не удалением gameplay. Автоматического повышения
статуса классов нет.

## Реально доступная цепочка и пробелы

Уже существует частичная цепочка жезл → книжная полка/таумономикон → Secondary
purchase либо primary note. Есть общий стол, inventory/menu/save, ограниченное
сканирование, тестовый узел/зарядка, банка/фиалы. Полный survival не замкнут:
shards для таумометра ещё нельзя получить естественно; GUI стола ещё read-only.
Новый pure edit planner сам по себе не делает редактирование доступным игроку.

В срезе найдено 8 исполняемых vanilla recipe JSON. Данные 201 исследования
содержат 310 text descriptors, 273 recipe references и 8 legacy expressions;
последние 281 descriptors не являются работающими native рецептами. Указанные
в старых отчётах 42 модели устарели: в проверенном срезе 44 identity mappings и
45 item-model JSON. Регистрация и модель не заменяют механику/получение предмета.

Основная оставшаяся работа: естественные ресурсы/worldgen, lifecycle узлов,
arcane crafting, алхимия, транспорт эссенции, инфузия, фокусы/экипировка,
VisNet/devices, големы, warp/mobs/taint/Внешние Земли, API/config/commands и
полный packaged/multiplayer/survival/visual/audio аудит.

Architectury reference незавершён и нацелен на **1.20.1 Fabric/Forge**, не на
1.21.1 NeoForge. Перенос его всего дерева вместо реализации TC4 не обоснован.

## Существенные открытые риски

1. **Overstack:** player/container save codec99 остаётся блокером copying R10
   для count100/127; table/entity adapters недостаточно. Не clamp/split молча.
2. **State schema:** `PlayerKnowledge.load` пока не обрабатывает dataVersion.
3. **Другие output paths:** `BlockJar.give()` не проверяет результат fallback
   `player.drop`, хотя caller уже расходует материалы. Это статический кандидат
   на потерю при отменённом spawn, **не воспроизведённый runtime-дефект**.
4. **Networking:** большие knowledge snapshots не разбиваются, измеренный бюджет
   запросов/corrective resync ещё не закреплён; это availability-риск, не
   доказанный exploit.
5. **Assets:** manifest имеет 1060 записей / 1059 targets; проверенные target
   hashes совпали. Для 7 записей есть отличие/отсутствие original counterpart;
   полный authored resource disposition ledger ещё нужен. Перенесены EN/RU из
   22 локализаций; полный translation parity не заявлен.
6. **Поставка:** original/donor JAR уже tracked — не удалять их по ошибочной
   формулировке «JAR вне Git». Приватная декомпиляция остаётся вне repository.
   Локальная разработка не даёт права публичной публикации original code/assets.
7. **Воспроизводимость:** 25 scratch code/script файлов в `.tmp` предыдущего
   среза не считаются production. В частности, генератор прежних reference
   inventories `.tmp/reference_inventory.py` ещё не tracked. Private external
   oracle helpers намеренно не входят в gameplay Java ledger.

Старые R07 JAR содержали 19/21 testing class entries. Это характеристика
**прежних артефактов**, а не вердикт для пересобранных A07 JAR. Старые 161 JUnit,
41+41 GameTests и 19 просмотренных Fabric PNG не подтверждают новые исходники.
Новый протокол fresh-source/fresh-output evidence описан в
`../porting/VALIDATION.md`; удалённый workflow здесь не запускался.

## Следующая игровая граница

После центральной проверки A07/pure R08 подключить native staged commit,
сохраняемый shared bonus, revision/session/menu admission, bounded C2S и GUI.
Проверить solve→learn у двух игроков и обоих loaders. Затем R09/R10 с отдельной
overstack-приёмкой; параллельно минимальные ресурсы/worldgen→arcane для
естественного начала. Полная очередь остаётся в `../MIGRATION_PLAN.md`.
