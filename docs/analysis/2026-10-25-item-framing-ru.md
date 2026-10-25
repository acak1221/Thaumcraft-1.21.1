# Положение жезла и таумометра

Базис разработки: `ced258a00e4ff69cf4835f27440a027647f773de`, ветка `neoforge`, исходное дерево чистое. Исправляется общий клиентский код; `main` не меняется. Предыдущие данные о слишком большом жезле в отчёте NeoForge описывают состояние до этой правки.

## Причина и изменение

Старый renderer TC4 получал окружающие преобразования Minecraft/Forge1.7.10. В современном builtin item hook их больше нет. Копирование только локальных GL-сдвигов давало огромный жезл и таумометр, уходящий вправо/вверх. Это воспроизведено в настоящем клиенте, а не выведено из зелёных локальных matrix fixtures.

- Для рук восстановлены исходные `RY45 × scale0.4` с учётом современного центрирования, зеркальной руки и порядка swing-преобразований.
- Для GUI учтены прежние10px против современных16px и оригинальная ориентация. Иконка жезла/посоха центрируется по геометрическому envelope с полем1px; исходные размеры деталей, материалы, vis и игровые свойства не меняются.
- Таумометр целиком помещается в обычный16:9 первый вид; его экран находится около оптической оси. Левая рука сохраняет читаемое направление текста; readout получает действительно рисуемый стек.
- С таумометром во второй руке две собственные руки инструмента больше не дополняются третьей vanilla-рукой. Узкий client mixin проверяет обе render-cache stacks; обычная пустая рука и cached main-hand map не подавляются.

Оригинальная локальная wand-chain сохранена как реально вызываемый `applyLegacy`, а не как мёртвый помощник для старых тестов. Преобразования активного применения вынесены в общий pure matrix helper без изменения правых исходных углов/pivot.

Основные файлы: [WandContextPose](../../common/src/main/java/thaumcraft/client/renderers/item/WandContextPose.java), [WandUsePose](../../common/src/main/java/thaumcraft/client/renderers/item/WandUsePose.java), [ItemThaumometerRenderer](../../common/src/main/java/thaumcraft/client/renderers/item/ItemThaumometerRenderer.java), [thaumometer display](../../common/src/main/resources/assets/thaumcraft/models/item/thaumometer.json), [ThaumometerEmptyHandArmMixin](../../common/src/main/java/thaumcraft/common/mixin/client/ThaumometerEmptyHandArmMixin.java).

## Основание и проверки

Oracle TC4 и donor-файлы не изменялись. Пропущенный caller проверен по официальному Mojang1.7.10 client JAR: SHA1 `e80d9b3bf5085002218d4be59e668bac718abbc6`, SHA256 `a4fc2284657544e0f4bcc964f927c2fda3e3a205178ed1d5d58883aaf9780cce`; также прочитан pinned ForgeHooksClient `9274e4fe435cb415099a8216c1b42235f185443e`. Этот JAR не включён в коммит.

Новые проверки: [WandViewportTest](../../common/src/test/java/thaumcraft/client/renderers/item/WandViewportTest.java) —6 случаев с настоящими вершинами ModelWand, полным native caller, проекцией, зеркалированием и use-pivot; [ThaumometerFramingTest](../../common/src/test/java/thaumcraft/client/renderers/item/ThaumometerFramingTest.java) —4 случая с настоящими JSON ItemTransform, OBJ и экранной плоскостью. Старые локальные oracle fixtures сохранены на уровне живого компонента. Независимый агент сверил математику, реальные контракты и узкий arm guard.

Окончательная сборка после arm fix: `:common:test --rerun :neoforge:test --rerun :fabric:build :neoforge:build` — **PASS57s; common2571/2571, NeoForge22/22,0 failures/errors/skips**. Оба production isolation гейта пройдены. Манифест6166 входов до/после одинаков: `d8f7c6be65c8b1a5d17a4724c266edbaba5612119fe8054520dfe5f10fbd2fd5`.

Серверные классы совпадают с предыдущим checkpoint; в common mixin config добавлена только client-запись, старые refmap mappings сохранены. Полный серверный473/467 здесь не переименован в новый прогон. Оба финальных физических клиента завершены штатно: NeoForge exit0/11m27s, Fabric exit0/5m44s. Дополнительный dedicated smoke прошёл: NeoForge1/1 и Fabric1/1, общий Gradle exit0/1m, оба свежих тестовых мира нормально сохранены и остановлены.

| Сцена | Подтверждение |
|---|---|
| Исходный дефект | [Таумометр до исправления](images/2026-10-25-framing-scanner-before.png): верх/правая часть обрезаны |
| NeoForge, правая рука | [После исправления](images/2026-10-25-framing-scanner-neoforge.png): полное лицо инструмента по центру |
| NeoForge, wand/staff/sceptre | [Посох и четыре иконки](images/2026-10-25-framing-wands-neoforge.png); отдельные wand/sceptre кадры сохранены в ignored evidence |
| NeoForge, левая рука | Scanner с двумя руками, без третьей; читаемый readout; offhand wand помещается |
| Fabric | [Таумометр после исправления](images/2026-10-25-framing-scanner-fabric.png); обе руки и wand проверены в клиенте |
| Отрицательная проверка arm mixin | Обычная пустая рука без scanner остаётся видимой; рука также видна рядом с offhand wand |

Клиенты используют собственные creative-миры этой задачи `NFParity-20261025-New World` и `FabricParity-20261025-New World`, обычные диагностические item-replace/F-swap команды. Это переиспользованные свежие1.21.1 входы, не survival replay. Размер окна854×480 на Xorg1280×720, FOV Normal, GUI Auto2, Mesa llvmpipe. Диагностические время суток/предметы объявлены в логах; production gameplay не менялся.

Ограничения: нет полного original-TC4 pixel match, всех FOV/ресурс-паков, исчерпывающей третьеличной калибровки scanner и всех одновременных offhand-действий. Математические проверки не выдаются за осмотр каждого кадра анимации. Звук/online multiplayer в этой среде не приняты.

Все receipts, исходные/промежуточные кадры, provenance, input hashes и логи находятся в ignored `.tmp/item-framing/`. Оригинальные JAR,934 legacy Java и859 oracle Java сохранены. CI и release-публикация не выполнялись.

## Собранные артефакты

- Fabric: `fabric/build/libs/Thaumcraft-fabric-0.1.0-alpha.6.jar`, SHA256 `4a3633dba0d66e64328a656a8e594eae70f42ab9030a14c198cfc1bcaa19a58f`.
- NeoForge: `neoforge/build/libs/Thaumcraft-neoforge-0.1.0-alpha.6.jar`, SHA256 `e949d3ece9c9d1a75b532e62ec6f7e283ac1fd6690209e60865be3d6036c8e72`.

Это сборки проверенной общей правки из ветки NeoForge; отдельная ветка Fabric получает тот же shared diff с собственной проверкой. GitHub release assets не публикуются этой операцией.

## Отдельная ветка Fabric

Общая правка перенесена на собственный Fabric baseline `7c831ebc`. Конфликты затронули только документацию; история этой ветки сохранена. Все10 общих framing code/test/resource файлов совпадают с проверенными в NeoForge; Fabric сохранил свою исходную taint-обвязку.

Отдельный запуск `:common:test --rerun :fabric:build` — **PASS57s,2571/2571 common JUnit,0 failures/errors/skips**, production isolation PASS.6136 входов неизменны: `864f838fe12e8462f89a7605d5138e0b43aae16e6aa1a445481e3c4e64cdf694`. Серверные классы совпадают с собственной проверенной alpha.6, а не сравниваются с другой веткой. NF22 и его runtime результаты выше относятся к ветке NeoForge.

Итоговый Fabric JAR из этой ветки:15296105bytes, SHA256 `ef11cfc4e22222bf778e0db6fe8e30edc4eefc8918c509e771687e2dc060544a`.
