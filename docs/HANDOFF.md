## Принятый серверный срез — 20.10.2026, b1e49f2

GitHub CI для точного commit `b1e49f24d0c1112800bec4276277623939d4c670` прошёл:
**648 JUnit / 212 Fabric GameTest / 79 Python**; полный Fabric build, свежесть отчётов,
изоляция production/test и attribution подтверждены. [Прогон](https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39409582347).
Исправлены recipe codec, отменённое добавление сфер, оригинальная физика сфер и нестабильные
физические условия тестов лампы/рунного алтаря. Неудачные промежуточные прогоны сохранены
в analysis/2026-10-20-ci-regressions.md.

Следующее рабочее дерево (аксессуары/сумка/Пехи/добыча/голодные узлы/кластеры/продвинутая печь/API)
ещё не покрывается этим PASS. Клиентская визуальная проверка, multiplayer, полная survival-прогрессия
и NeoForge остаются неподтверждёнными. Перенос продолжается; ни один общий раздел ещё не закрыт полностью.
Состояния ниже относятся к более ранним срезам и сохраняются как история.

## Третья группа для CI — 20.10.2026

Новые Runic/боевые фокусы/UNSTABLE+orbs/Pearl/CheatSheet/Firebat+Wisp/scanAPI/admincommands
прошли локальную common JUnit647 и Python79 +Fabric gameTest/clientTest compile (1m16).
Полный серверный прогон будет GitHub для нового commit; клиент/NeoForge не проверены.
Предыдущий9359eb4 дал196/198: native NPE empty reward в Deconstruction и named-loot-sequence
fixture Dowsing исправлены в текущем дереве. Advanced Furnace остаётся .tmp draft, не включён.

## Следующая группа для удалённого прогона — 20.10.2026

Рабочая группа R11/R12 + research settings, Repair/Haste/Bore repair, Ward/Excavation specials,
Growth/Fertility/Deconstruction и Brainy/Giant zombies подготовлена; локально 620 JUnit/78 Python PASS.
Полный Fabric build/GameTest выполняется на GitHub для нового commit. До его результата
последний принятый полный серверный срез остаётся d508ada (582/186/78).
Клиент, survival, multiplayer и NeoForge этой группой не подтверждены.

# Полный перенос TC4 на Minecraft 1.21.1 — handoff

**20.10 — интеграция `d508ada` прошла GitHub CI: 582 JUnit / 186 Fabric GameTest / 78 Python.**
Копирование записок, Тауматориум/матрицы памяти, 22 зачарования инфузии, Architect/Warding/Trade,
лампы/зарядка и серверный Warp проверены на этом commit. Изоляция поставки, свежесть отчётов
и attribution прошли; клиент/NeoForge не подтверждены. [Точный CI-прогон](https://github.com/acak1221/Thaumcraft-1.21.1/actions/runs/39398016295).
Текущее рабочее дерево уже содержит следующие группы и не покрывается этим PASS.
См. [активную работу](ACTIVE_PORT.md).


## Актуально 20.10.2026: магическое ухо, после резервуаров `5b19be4`

**Implemented:** `thaumcraft:arcane_ear`, исходные пять инструментов/25 нот,
радиус 64 включительно, повторяемый импульс 15 на 10 тиков, настройка кликом,
сохранение настройки без импульса, арканный рецепт/книга/иконка/EN-RU/модели.
Ноты идут из настоящего vanilla `NoteBlock.playNote`; сохранена исходная очередь до POST тика.
Исправлены pre-tick gameTime gate и кодирование тихого block event (-1 становится 255 в пакете).
**Проверено:** 542 JUnit / **177 Fabric GameTest** / 76 Python, ресурсы без пропущенных ссылок,
`:fabric:clientTestClasses`, `:fabric:build`, production/test isolation.
JAR SHA-256 `0e2b9d70cd22f5e2247e8e3058a3997cf77ffaf7a251e2c73bc787bda6066136`.
Контракт и уровни evidence: [arcane-ear](analysis/2026-10-20-arcane-ear.md).
Клиент/настоящий restart/NeoForge не запускались; 10 mutation scenarios подготовлены, не исполнены.
Срез резервуаров ниже уже отправлен в `origin/work`; текущий срез продолжает ту же ветку.
Дальше остаются Warding/Trade, остальные M09 устройства, R10–R16, enchant-инфузия и Thaumatorium.

## Актуально 20.10.2026: продолжение с GitHub `88d683b`, ветка `work`

Перед изменениями выполнены `git fetch origin --prune` и fast-forward до `origin/main`.
Следующая группа после подъёмника — **алхимические големы с резервуарами**:
сбор первого аспекта через собственный порт, доставка смешанного содержимого с точным остатком,
приоритет вместе с непустыми подписанными банками; общий перенос исправлен для вместимости 64 с двумя Earth.
Контракт и ограничения: [golem-reservoir](analysis/2026-10-20-golem-reservoir.md).
**Проверено:** 542 JUnit / 176 Fabric GameTest, `:fabric:build`, изоляция production,
76 Python; JAR SHA-256 `028c10f67f4bc970bd00943e93b842c73cd83ae8014794eacd84bd72c2c30fdd`.
Клиент/настоящий restart/NeoForge не проверены. 8 mutation scenarios подготовлены, не исполнены.
Пользователь явно разрешил коммиты/push и субагентов в текущем чате. Следом уже начато магическое ухо;
его текущий статус см. в следующем отдельном checkpoint. Ниже — исторические записи.

Дата:16.10.2026. Ветка `port/minecraft-1.21.1`, HEAD=`ac1734af`, native R08/R09 и частичные M02–M05/M08 в рабочем дереве. Полная цель M00–M15 активна и не завершена. При старте прочитать AGENTS.md, проверить HEAD/status и [план](MIGRATION_PLAN.md). NeoForge/packaged visual, полный multiplayer/survival остаются открыты. Новых commits/push нет.

## Приоритет пользователя18.10: контент; предварительный экспорт бура

### Актуально20.10 (двадцать седьмая группа: Магический подъёмник, Claude): реализовано, собрано, game-tested 175/175

Контракт и реализация: [arcane-levitator](analysis/2026-10-20-arcane-levitator.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** блок `thaumcraft:levitator` и плитка `TileLevitator` (на обеих сторонах, как оригинал): дальность 10 + 10 за каждый не питаемый подъёмник прямо под ним (счёт по просьбе блока и на каждом сотом тике), столб в один блок до первого непрозрачного блока; предметы, толкаемые сущности и лошади получают +0,1 вверх (до 0,35), приседающий держит 90 % падения, падение сбрасывается; сигнал на блоке или **над** ним останавливает; блок помечает подъёмники **под** изменившимся (направление оригинала); арканный рецепт `LEVITATOR` (`WEW/BNB/WAW`, `aer 10`, `terra 5`).
**Built / game-tested:** `:common:test` **542 JUnit** (+9: 8 подъёмника и 1 резервуара), `:fabric:remapJar`, `:fabric:runGameTest` **175/175** (+`arcaneLevitatorLiftsItemsAndCreaturesAndStacks`: настоящие игрок, предметы, вагонетка, свинья, лошадь, редстоун), JAR SHA-256 `86c7dd75…13bd`; три команды шага CI с ресурсами зелёные.
**Исправления по пути:** (1) **резервуар эссенции** — слизь вспышки могла лечь в собственную клетку блока, и добыча игроком не выбрасывала предмет (в оригинале блок ещё стоял); теперь клетка пропускается (`ReservoirRules.burstTarget`, JUnit и мутант); (2) тест Портативной дыры зависел от билетов соседних тестов и порой не доводил туннель до конца — теперь принуждает свои чанки сам; (3) якорь мутанта `placed-clicked-face` резервуара не собирался (полное имя класса) — исправлен, мутант пойман.
**Мутации:** `levitator` (30: 17 JUnit + 13 GameTest): JUnit 17/17 и шесть GameTest-мутантов пойманы локально, остальные 7 GameTest у GitHub (запрос в `run-request.txt`). GitHub: `fortress` (прогон 20, `c2895b2`) — зелёный; `essentia-reservoir` (прогон 21, `4fa6164`) — 18 из 19 GameTest как ожидалось, `placed-clicked-face` «invalid» (якорь, исправлен).
**Seen:** нет. **Не проверено:** искры и свечение (не портированы), клиентский подъём игрока, провод редстоуна к боковым граням.
**Замечание для отдельной проверки:** у алембика `strength(3, 17)` — сопротивление взято как 17, а по правилу порта для оригинального `setResistance(17)` (×3, отдаётся /5) выходит 10,2 (резервуар сделан так); остальные блоки с `setResistance` стоит сверить.
**Следующее:** Warding / Trade фокусы, Бур с Киркой Огня, големы с резервуарами, остальные устройства M09 (лампы, Ear, Vis Charge Relay и др.), M01 R10–R16, M07 enchant-инфузия, Thaumatorium; варп (M11).

### Актуально20.10 (двадцать шестая группа: Резервуар эссенции, Claude): реализовано, собрано, game-tested 174/174

Контракт и реализация: [essentia-reservoir](analysis/2026-10-20-essentia-reservoir.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** блок `thaumcraft:essentia_reservoir` и плитка `TileEssentiaReservoir` (смесь до 256 единиц суммарно, один порт на стороне `facing`, притяжение 24 пока не полон, забор по единице раз в 5 тиков через **новый профиль `RESERVOIR`** общего исполнителя `EssentiaTransfers`, сторонний запрос типа пуст, неуказанный — первый аспект), компаратор `floor(объём/256·14)+1`, поворот жезлом (приседание — на сторону клика, иначе противоположную), размещение (отверстие к кликнутому блоку), слом с объёмом ≥ 16 — взрыв и `объём/16 + 1` блоков слизи/газа уровня 8, **беспроводной забор инфузии** из резервуара, сохранение (объём выше 256 — пусто, испорченные данные закрывают резервуар и хранятся дословно), инфузия `ESSENTIARESERVOIR` (буфер + 2 слитка пустоты + 4 баночки, нестабильность 6).
**Built / game-tested:** `:common:test` **533 JUnit** (+12), `:fabric:remapJar`, `:fabric:runGameTest` **174/174** (+`essentiaReservoirHoldsMixedEssentiaPullsAndBursts`: настоящие игрок, буфер, труба, жезл, кирка, матрица), JAR SHA-256 `0050bef5…a66a`; три команды шага CI с ресурсами зелёные.
**Мутации:** `essentia-reservoir` (52: 25 JUnit + 27 GameTest): JUnit 25/25 и девять GameTest-мутантов пойманы локально, остальные 18 GameTest прогнал GitHub (прогон 21) как ожидалось. GitHub-прогон `fortress` (20, c2895b2) зелёный.
**Seen:** нет. **Не проверено:** вид (рендера нет — временный полупрозрачный куб), скрип на клиенте, големы эссенции с резервуарами (ветки `AIEssentiaGather` / `AIEssentiaEmpty` не перенесены).
**Следующее:** Warding / Trade фокусы, Бур с Киркой Огня, големы с резервуарами, M01 R10–R16, M07 enchant-инфузия, Thaumatorium; варп (M11); Focus Pouch / баублы (нужно решение по контракту аксессуаров).

### Актуально20.10 (двадцать пятая группа: боевая таумиевая броня Fortress, Claude): реализовано, собрано, game-tested 173/173

Контракт и реализация: [fortress-armor](analysis/2026-10-20-fortress-armor.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `fortress_helmet` / `fortress_chestplate` / `fortress_leggings` (защита 3/7/6, прочность 440/640/600, зачаровываемость 25, rare, ремонт слитком таумия), **обобщённая магическая защита** `MagicWard` (интерфейс `MagicWardGear`: `wardsFire`, `wardFactor`; магия `защита / 35`, огонь и взрывы `защита / 20` у Fortress, множитель набора игрока `0,875 + 0,125 за вещь + 0,05 за маску`) со второй обёрткой миксина вокруг `CombatRules.getDamageAfterAbsorb` (значение брони: вещи с долей первого прохода вычитаются, обычная броня игрока умножается на набор), очки как компонент `fortress_goggles`, три маски как компонент `fortress_mask` (Sipping Fiend лечит 1 с шансом `урон / 12`; Angry Ghost иссушает нападающего на 80 тиков с шансом `урон / 10`; Grinning Devil только ставится — варп-событий ещё нет), шесть инфузий (3 брони + очки + 3 маски как именованные патчи `InfusionResultPatches`).
**Built / game-tested:** `:common:test` **521 JUnit** (+7), `:fabric:remapJar`, `:fabric:runGameTest` **173/173** (+`thaumiumFortressWardsSetsAndMasks`: настоящий подключённый игрок, настоящие свиньи с 500 здоровья), JAR SHA-256 `5875e199…a891`; три команды шага CI с ресурсами зелёные.
**Мутации:** `fortress` (40: 17 JUnit + 23 GameTest): JUnit 17/17 и семь GameTest-мутантов пойманы локально, остальные 16 GameTest у GitHub (запрос в `run-request.txt`). GitHub: `portable-hole` (прогон 18, `05e6df1`) — 34 из 34 как ожидалось; `void-robe` `wash-keeps-level` (прогон 19, `1b3007a`) в повторе **пойман** (первый выживший в 48 с остаётся необъяснённым сбоем задания), `build.yml` на `05e6df1` и `1b3007a` зелёный.
**Seen:** нет. **Не проверено:** надетый вид и рисунки масок (`ModelFortressArmor` не перенесена — временно слои таумиевой брони), Grinning Devil (варп M11), двойной износ игрока ванильным `hurtArmor` (задокументированное отклонение), клиентские значки/подсказки.
**Следующее:** Warding / Trade фокусы, Бур с Киркой Огня, Essentia Reservoir, M01 R10–R16, M07 enchant-инфузия, Thaumatorium; варп (M11); Focus Pouch / баублы (нужно решение по контракту аксессуаров).

### Актуально20.10 (двадцать четвёртая группа: фокус «Портативная дыра», Claude): реализовано, собрано, game-tested 172/172

Контракт и реализация: [portable-hole](analysis/2026-10-20-portable-hole.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `focus_portable_hole` (10 энтропии + 10 воздуха центивис за блок глубины, минимум одна единица, апгрейды Frugal / Enlarge / Extend — **новый** `FocusUpgradeType.extend`, id 7), измерение глубины (33 + 8 за Enlarge), цепочка из глубина + 1 блоков с кольцом 3 × 3, блок `thaumcraft:hole` (`BlockHole`, неразрушимый, свет 10, без коллизии и предмета, жидкость не пускает) и тайл `TileHole` (запоминает состояние блока, 120 + 60 за Extend тиков, возвращает блок и ставит обновление через 2 тика), чёрный список тегом `thaumcraft:portable_hole_blacklist`, инфузия `FOCUSPORTABLEHOLE`.
**Built / game-tested:** `:common:test` **514 JUnit** (+10), `:fabric:remapJar`, `:fabric:runGameTest` **172/172** (+`portableHoleCutsTunnelsAndGivesBlocksBack`: настоящий подключённый игрок и жезл, 10 стен: 108 дыр, оплата, достижение, остановки, возврат состояний), JAR SHA-256 `1c4811aa…5553b`; три команды шага CI с ресурсами зелёные.
**Мутации:** `portable-hole` (34): JUnit 18/18 и шесть GameTest-мутантов пойманы локально, остальные 10 GameTest прогнал GitHub (прогон 18) — 34 из 34 как ожидалось. `void-robe` (39): GitHub (прогон 17 на `f5dd37a`) — 38 из 39 как ожидалось, `wash-keeps-level` выжил в 48-секундном задании без сообщений GameTest; в повторе (прогон 19) пойман за 5 минут, причина первого выжившего не установлена.
**Seen:** нет. **Не проверено:** вид отверстия и искры (не портированы — пустая полость), звуки, защита территории (добавлена проверка `mayInteract` на кликнутом блоке — отклонение), Внешние Земли (нет измерения).
**Следующее:** Fortress-броня (ARMORFORTRESS, маски и очки как патчи инфузии, доли /20 для огня и взрыва — обобщить `MagicWard`), Warding / Trade фокусы, Бур с Киркой Огня, Essentia Reservoir, M01 R10–R16, M07 enchant-инфузия, Thaumatorium; варп (M11).

### Актуально20.10 (двадцать третья группа: Пустотные одеяния тауматурга, Claude): реализовано, собрано, game-tested 171/171

Контракт и реализация: [void-robes](analysis/2026-10-20-void-robes.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `void_robe_helmet` / `void_robe_chestplate` / `void_robe_leggings` (материал Void: защита 3/7/6, прочность 110/160/150, зачаровываемость 10, epic, ремонт слитком пустоты), самолечение (общая `VoidMetal.armorTick`: тик инвентаря + тик брони для надетой вещи), скидка вис-стоимости 5 % и варп 2 за вещь, очки (узлы и подсказки) у капюшона, **магическая защита** `защита / 35` за надетую вещь (`MagicWard` + миксин `LivingEntityMagicWardMixin` вокруг `getDamageAfterArmorAbsorb`, тег `thaumcraft:is_magic`), краска и смывание в котле общие с обычными мантиями (маркер `DyedRobe`), три инфузии `ARMORVOIDFORTRESS` (нестабильность 6, центр — Void-броня без износа, очки и мантии без износа).
**Built / game-tested:** `:common:test` **504 JUnit** (+7), `:fabric:remapJar`, `:fabric:runGameTest` **171/171** (+`voidThaumaturgeRobesMendWardAndDye`: настоящий подключённый игрок, настоящий урон и тики), JAR SHA-256 `a098d8c9…1468`; три команды шага CI с ресурсами зелёные.
**Мутации:** `void-robe` (39): JUnit 20/20 и шесть GameTest-мутантов пойманы локально; GitHub (прогон 17) — 38 из 39, выживший `wash-keeps-level` не объяснён (см. выше). Повторный прогон `elemental-shovel` (16) на `8046f2a` зелёный — оба выживших выжили, как задокументировано; `build.yml` на `8046f2a` зелёный.
**Seen:** нет. **Не проверено:** надетый вид, цвета на клиенте. **Не перенесено:** модель `ModelRobe` (58 деталей, 128 × 64; временно надеты слои Void-брони) — отдельная клиентская единица.
**Следующее:** Fortress-броня (ARMORFORTRESS, маски, доли /20 для огня и взрыва: `MagicWard` обобщить), Бур с Киркой Огня, Essentia Reservoir (нужен новый профиль `EssentiaTransfers`), M01 R10–R16, M03/M08 фокусы, M07 enchant-инфузия, Thaumatorium; варп (M11).

### Актуально20.10 (двадцать вторая группа: Меч Бури, Claude): реализовано, собрано, game-tested 170/170

Контракт и реализация: [elemental-sword](analysis/2026-10-20-elemental-sword.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `elemental_sword` (1500 прочности, скорость 10, урон 7, зачаровываемость 18, rare, ремонт слитком таумия), **удержание** кнопки (подъём +0,08 за тик с потолком 0,5→0,2, падение и дистанция падения делятся на 1,2, обнуление счётчика «парения» сервера, отталкивание всех, кроме игроков, в радиусе 2,5, звук `wind` и износ в тиках 0/20/40…), **размах удара** (`Player.attack`: каждый живой моб в боксе цели ±(1,2; 1,1; 1,2), кроме собственных големов, ручных животных и игроков, получает обычный удар игрока со своим износом меча; перезарядка сохраняется; вложенные удары не размахиваются), инфузия `ELEMENTALSWORD`.
**Built / game-tested:** `:common:test` **497 JUnit** (+7), `:fabric:remapJar`, `:fabric:runGameTest` **170/170** (+`swordOfTheZephyrLiftsBlowsAndSweeps`: настоящий подключённый игрок, настоящие `Player.attack`), JAR SHA-256 `2ceea75f…b26f`; три команды шага CI с ресурсами зелёные.
**Мутации:** `elemental-sword` (36): JUnit 20/20 и пять GameTest-мутантов пойманы локально; GitHub (прогон 15) — 35 из 36 как ожидалось, один эквивалентный выживший (`any-sword-sweeps`, задокументирован). `elemental-shovel` (35): GitHub (прогон 14) — 33 из 35, два эквивалентных выживших (`bedrock-attempted`, `thaumium-too`, задокументированы).
**Seen:** нет. **Не проверено:** подъём на настоящем клиенте, частицы, звуки, поза лука (вместо блока мечом), сеть/два игрока.
**Следующее:** Primal Crusher (ELDRITCH) — после фокуса Primal; M01 R10–R16 (копирование, unknown scrolls, clues, режимы сложности, deconstruction), M03/M08 фокусы, M07 enchant-инфузия, Thaumatorium; варп (M11).

### Актуально20.10 (двадцать первая группа: Лопата Землероя, Claude): реализовано, собрано, game-tested 169/169

Контракт и реализация: [elemental-shovel](analysis/2026-10-20-elemental-shovel.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `elemental_shovel` (1500 прочности, скорость 10, урон 4, зачаровываемость 18, rare, ремонт слитком таумия), **копание 3 × 3** поперёк грани, на которую смотрит игрок (вложенные обычные разрушения: прочность, выпадение, события защиты; не при крадущемся, не в креативе, только блоки `mineable/shovel`), **укладка 3 × 3** копии щёлкнутого блока из инвентаря (по ориентации 0/1/2 и повороту игрока; трава → земля), ориентация — компонент `thaumcraft:shovel_orientation` и клавиша G (`ShovelOrientationPayload`), инфузия `ELEMENTALSHOVEL`.
**Built / game-tested:** `:common:test` **490 JUnit** (+10), `:fabric:remapJar`, `:fabric:runGameTest` **169/169** (+`shovelOfTheEarthmoverMinesAndLaysSquares`: настоящий подключённый игрок, настоящие блоки), JAR SHA-256 `bc13bd2d…aa06`; три команды шага CI с ресурсами зелёные.
**Мутации:** `elemental-shovel` (35): JUnit 17/17 и четыре GameTest-мутанта пойманы локально; GitHub (прогон 14) — 33 из 35 как ожидалось, два эквивалентных выживших (`bedrock-attempted`, `thaumium-too`) задокументированы. `elemental-hoe` (25) и `elemental-pickaxe` (27) на GitHub — все как ожидалось.
**Seen:** нет. **Не проверено:** значок, звуки, сама клавиша G в клиенте, работа с модом защиты территории.
**Не перенесено:** призрачный предпросмотр (`IArchitect`) и искры, переключатель жезла на клавише G.
**Следующее:** Elemental Sword (взлёт и притягивание при удержании, удар по всем рядом), затем M01 R10–R16, M03/M08 фокусы, M07 enchant-инфузия, Thaumatorium.

### Актуально20.10 (двадцатая группа: Мотыга Роста, Claude): реализовано, собрано, game-tested 168/168

Контракт и реализация: [elemental-hoe](analysis/2026-10-20-elemental-hoe.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `elemental_hoe` (1500 прочности, зачаровываемость 5, rare, без урона, удар не тратит прочность), вспашка 3 × 3 (1 прочность за блок, право правки на каждый, крадущийся — один), при пустой вспашке костная мука (1 прочность), затем рост магического саженца: Greatwood за 5 при остатке от 20, Silverwood за 25 при остатке от 150 (плата и при отсутствии места), инфузия `ELEMENTALHOE`, звук `wand`.
**Built / game-tested:** `:common:test` **480 JUnit** (+4), `:fabric:remapJar`, `:fabric:runGameTest` **168/168** (+`hoeOfGrowthTillsFertilisesAndGrowsSaplings`: настоящий игрок, настоящие блоки и деревья), JAR SHA-256 `49fb705c…2a5e`; три команды шага CI с ресурсами зелёные.
**Мутации:** `elemental-hoe` (25): JUnit 14/14 и три GameTest-мутанта пойманы локально, GitHub (прогон 13) — все 25 как ожидалось. `elemental-pickaxe` (27) на GitHub (прогон 12) — тоже все как ожидалось.
**Seen:** нет. **Не проверено:** значок, звук, искры (не портированы), раздатчик.
**Следующее:** Elemental Shovel (3 × 3 копание и укладка, IArchitect) и Sword (взлёт и притягивание), затем M01 R10–R16, M03/M08 фокусы, M07 enchant-инфузия, Thaumatorium.

### Актуально20.10 (девятнадцатая группа: Кирка Огня и особая добыча кластеров, Claude): реализовано, собрано, game-tested 167/167

Контракт и реализация: [elemental-pickaxe](analysis/2026-10-20-elemental-pickaxe.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `elemental_pickaxe` (1500 прочности, скорость 10, урон 5, зачаровываемость 18, rare, ремонт слитком таумия), поджог на 2 секунды при ударе (правило PvP), 5 прочности и звук `wandfail` за правый щелчок, инфузия `ELEMENTALPICK`,
**особая добыча** (`SpecialMining` + миксин `SpecialMiningBlockMixin` в `Block.getDrops`): `raw_iron` / `raw_gold` / `cinnabar_ore` превращаются в кластеры с шансом `0,2 + удача × 0,075` (× 1,0 / 0,9 / 0,9), то же количество, без Silk Touch, только у игрока с этой киркой.
**Built / game-tested:** `:common:test` **476 JUnit** (+6), `:fabric:remapJar`, `:fabric:runGameTest` **167/167** (+`pickaxeOfTheCoreBurnsWearsAndFindsClusters`: 400-кратные выборки `Block.getDrops` и 120 настоящих добыч), JAR SHA-256 `305d58b7…347b`; три команды шага CI с ресурсами зелёные. Один красный прогон по дороге — дорожка NO-LAUNCHER в тесте дротика насчитала чужой дротик; дорожки теперь считают только дротики своего голема (`getOwner()`), причина не доказана.
**Мутации:** `elemental-pickaxe` (27): JUnit 13/13 локально, GitHub (прогон 12) — все 27 как ожидалось. `native-clusters` (24) и `void-gear` (27) на GitHub — все как ожидалось.
**Seen:** нет. **Не проверено:** значок кирки, звуки, работа с Буром (Бур пока принимает только ванильные кирки и Thaumium Pickaxe), Excavation с Dowsing.
**Не перенесено:** клиентский скан руд (правый щелчок), Primal Crusher, Dowsing.
**Следующее:** Elemental Hoe / Shovel / Sword (мотыга растит деревья, лопата 3×3, меч подбрасывает), затем M01 R10–R16, M03/M08 фокусы, M07 enchant-инфузия, Thaumatorium.

### Актуально20.10 (восемнадцатая группа: самородные кластеры железа/золота/киновари, Claude): реализовано, собрано, game-tested 166/166

Контракт и реализация: [native-clusters](analysis/2026-10-20-native-clusters.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** предметы `cluster_iron/gold/cinnabar`, четыре рецепта тигля (`PUREIRON`, `PUREGOLD` — руда и сырая руда в кластер; `TRANSIRON`, `TRANSGOLD` — самородок в три), три печных рецепта «кластер → два слитка», аспекты `ConfigAspects:360-362`, псевдонимы книги и значки исследований.
Новый тип `thaumcraft:stack_smelting` (`StackSmeltingRecipe`, остаётся `RecipeType.SMELTING`): `minecraft:smelting` 1.21.1 молча теряет количество результата, а оригинал платит два слитка.
**Built / game-tested:** `:common:test` **470 JUnit** (+2 `ClusterAssetsTest`; `EarlyObjectAspectBindingsTest` обновлён под три новые строки), `:fabric:remapJar`, `:fabric:runGameTest` **166/166** (+`nativeClustersPurifyTripleAndMelt`: рецепты, аспекты и три настоящие ванильные печи), JAR SHA-256 `a658954e…8d44`; три команды шага CI с ресурсами зелёные.
**Мутации:** `native-clusters` (24): JUnit 15/15 пойманы локально, GitHub (прогон 11) — все 24 как ожидалось. `void-cap` (20) и `void-gear` (27) на GitHub — тоже все как ожидалось.
**Seen:** нет. **Не проверено:** значки кластеров, настоящая варка в котле, особая добыча.
**Не перенесено:** особая добыча кластеров (Elemental Pickaxe, Primal Crusher, Excavation с Dowsing, Arcane Bore: `Utils.findSpecialMiningResult`, шанс `0,2 + удача × 0,075`) — придёт вместе с группой Elemental-инструментов; выпадение у Пеха и торговля волшебников (мобы M11); бонус Продвинутой алхимической печи.
**Следующее:** Elemental Pickaxe/Hoe/Sword/Shovel вместе с перехватом выпадения блока (особая добыча, Primal Crusher, Dowsing), затем M01 R10–R16, M03/M08 фокусы, M07 enchant-инфузия, Thaumatorium.

### Актуально20.10 (семнадцатая группа: Void-инструменты и броня, Claude): реализовано, собрано, game-tested 165/165

Контракт и реализация: [void-gear](analysis/2026-10-20-void-gear.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** девять предметов из Void metal (меч, кирка, топор, лопата, мотыга, шлем, кираса, поножи, ботинки) с оригинальными числами (прочность 150 / 110–160, скорость 8, урон, защита 3/7/6/3), верстачные рецепты, самолечение раз в 20 тиков носителя (надетая деталь — дважды; творческий режим не чинится),
иссушение при ударе (меч 3 с, остальные 4 с; правило PvP), `IWarpingGear` у всех девяти и сумма `WarpGear.fromGear` (рука + четыре слота брони), оригинальная особенность ремонта инструментов Primal Charm'ом, слои брони, теги, имена.
**Built / game-tested:** `:common:test` **468 JUnit** (+3), `:fabric:remapJar`, `:fabric:runGameTest` **165/165** (+`voidMetalGearMendsWithersAndWarps`), JAR SHA-256 `d3a4dd8f…8571`; три команды шага CI с ресурсами зелёные.
**Мутации:** `void-gear` (27): JUnit 4/4 локально, 23 GameTest у GitHub. `staff-rods` (36) на GitHub — все как ожидалось; `void-cap` (20) на GitHub — все как ожидалось.
**Seen:** нет. **Не проверено:** вид иконок и слоёв брони, сами события варпа (M11), Void-мантии (`ARMORVOIDFORTRESS`), сеть/два игрока, NeoForge.
**Следующее:** M01 R10–R16 (копирование, unknown scrolls, clues, режимы сложности, deconstruction), M03/M08 фокусы, M07 enchant-инфузия, Thaumatorium; варп (M11) — когда дойдёт очередь.

### Актуально20.10 (шестнадцатая группа: Void metal и колпачок Void, Claude): реализовано, собрано, game-tested 164/164

Контракт и реализация: [void-cap](analysis/2026-10-20-void-cap.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** добыча Void metal в тигле (`crucible/void_seed`: пшеничные семена + tenebrae 8, vacuos 8, alienis 2; `crucible/void_ingot`: Void Seed + metallum 8; исследование `VOIDMETAL`), самородок `void_nugget` (9 из слитка и обратно, тег `c:nuggets/void`), неактивный колпачок (`NNN/N N`, perditio 27 + ordo 27 + ignis 18 + aer 18), заряд инфузией (4 Salis Mundus, нестабильность 8, по 18 potentia / vacuos / alienis / auram),
колпачок Void как материал жезла (множитель 0,8, цена 9; жезл с ним платит на пятую часть меньше), 360 составных моделей жезла.
**Built / game-tested:** `:common:test` **465 JUnit** (+3 `VoidCapTest`), `:fabric:remapJar`, `:fabric:runGameTest` **164/164** (+`voidMetalAndTheVoidCapBrewCraftAndDiscount`; расширен тест очков `gogglesCraftProtectAndReduceVis` на четвёртый колпачок), JAR SHA-256 `5184292e…ea7a`; три команды шага CI с ресурсами зелёные.
**Мутации:** `void-cap` (20): JUnit 7/7 локально, 13 GameTest у GitHub. `staff-rods` (36) у GitHub ещё в очереди на момент записи (JUnit 19/19 локально).
**Seen:** нет. **Не проверено:** вид колпачка/значка, настоящий тигель с этими рецептами, алтарь с зарядом, сеть/два игрока, NeoForge.
**Следующее:** доспехи и инструменты из Void metal (`VoidHelm…VoidSword`, исследование `VOIDMETAL`), затем M01 R10–R16, M03/M08 фокусы, M07 enchant-инфузия, Thaumatorium.

### Актуально20.10 (пятнадцатая группа: стержни-посохи, Claude): реализовано, собрано, game-tested 163/163

Контракт и реализация: [staff-rods](analysis/2026-10-20-staff-rods.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** девять ядер посоха (greatwood 125/8, obsidian/blaze/ice/quartz/bone/reed 175/14 со своим первичным, silverwood 250/24, primal 250/32), предметы `staff_rod_*`, восемь арканных рецептов и инфузия Primal (нестабильность 8, 32 каждого первичного + 64 praecantatio), `WandRodPassive` для Primal (каждые 50 тиков +1 вис случайному первичному ниже десятой части),
посох как оружие (+6 урона нативным компонентом `attribute_modifiers`, удар 7), запрет посоха в слоте/тайле/меню арканного верстака и при превращении стола (`ArcaneVis.isWorkbenchWand`), название по обычному стержню с существительным «посох»; предел вис компонента 25000, предел цены сборки 288;
селектор моделей жезла делит значение стержня на 32 (270 переопределений, 135 новых составных моделей, `staff_shape`/`staff_focus_shape`).
**Built / game-tested:** `:common:test` **462 JUnit** (+5), `:fabric:remapJar`, `:fabric:runGameTest` **163/163** (+`staffRodsBrewAssembleStrikeAndRefill`: рецепты, сборка и сохранение, слот верстака, настоящий игрок — 7 урона по свинье, подзарядка обсидианового и Primal-посоха), JAR SHA-256 `52607405…97ae`; пять подряд прогонов GameTest зелёные; три команды шага CI с ресурсами (`record_verification --begin-run`, `convert_assets.py --verify`, `unittest discover scripts/porting` — 76 тестов) тоже зелёные.
**Исправление красной сборки предыдущего коммита:** кожи стержней жезла и сундука плейсера не были в атласе — добавлены (подробности в [wand-rods](analysis/2026-10-19-wand-rods.md)).
**Мутации:** `staff-rods` (36): JUnit 19/19 локально; 17 GameTest у GitHub.
**Seen:** нет. **Не проверено:** вид посоха/значка/позы в руке (размеры взяты из `ModelWand`, а не со снимка), руны и свечение, ритуал Primal (нужен ещё не портированный `FOCUSPRIMAL`), сеть/два игрока, NeoForge.
**Следующее:** колпачок void (нужны void-нуггет и крафт из `ConfigRecipes`), затем M01 R10–R16, M03/M08 фокусы, M07 enchant-инфузия, Thaumatorium.

### Актуально19.10 (четырнадцатая группа: элементные стержни жезлов и выбор модели жезла, Claude): реализовано, собрано, game-tested 162/162

Контракт и реализация: [wand-rods](analysis/2026-10-19-wand-rods.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** шесть стержней obsidian / blaze / ice / quartz / bone / reed (ёмкость 75, цена 6, свой первичный аспект), `WandRodPassive` + `ItemWandCasting.inventoryTick` (каждые 200 тиков игрока +1 вис своего аспекта, если запас меньше десятой части ёмкости), шесть предметов и инфузионных рецептов `ROD_*`,
`WandModels` и `scripts/porting/gen_wand_models.py` (135 переопределений `wand.json` — раньше любой жезл рисовался одной иконкой без фокуса; 90 новых составных моделей), модель `trunk_spawner` как коробки сундука.
**Built / game-tested:** `:common:test` **457 JUnit** (+10: `WandRodPassiveTest` 5, `WandModelAssetsTest` 4, `TrunkSpawnerModelTest` 1), `:fabric:remapJar`, `:fabric:runGameTest` **162/162** (+`elementalWandRodsBrewAssembleAndRefillTheirOwnPrimal`: рецепты, сборка жезлов, настоящий игрок и подзарядка на 200-м тике; в тест дротика добавлена дорожка TOO-CLOSE), JAR SHA-256 `a10da880…0109`.
**Находка про стабильность тестов:** до правки голем-GameTest падали в 7 из 12 прогонов (Fetter, радиус очков Decorations, Entropy item-match, урон дротика). Причина в тесте, а не в големе: `EntityGolemBase.loadedNavigationArea` требует загруженные чанки на 48 блоков вокруг, а GameTest стоит в случайном месте мира — если стенд пересекает границу чанка, кольцо за квадратом 3×3 не загружено, голем стоит.
Теперь `GolemChunks.hold` (common/src/gameTest) загружает и держит это кольцо; проверка радиуса очков вынесена на 5 тиков позже (сущности только что загруженного чанка не видны миру сразу); после правки — 12 зелёных прогонов из 12. Новым голем-тестам с дальними целями нужен `GolemChunks.hold`.
**Мутации:** `wand-rods` (24; JUnit 17/17 локально; 7 GameTest запрошены у GitHub). GitHub по прошлым группам: дротик — 39 пойманы, 2 ожидаемо выжили, 1 неожиданный (`too-close-still-shoots`, цель ближе трёх блоков) → добавлена дорожка TOO-CLOSE, мутант пойман локально; горящий улов — без неожиданностей.
**Seen:** нет. **Не проверено:** вид составных моделей жезла и иконок, свечение блейз-стержня, модель плейсера сундука, полный алтарный ритуал с новыми рецептами, сеть/два игрока, NeoForge.
**Следующее:** стержни-посохи (`STAFF_ROD_*`, `isStaff`), колпачок void, затем M01 R10–R16, M03/M08 фокусы, M07 enchant-инфузия, Thaumatorium.

### Актуально19.10 (тринадцатая группа: горящий улов Fire-голема Fishing, Claude): реализовано, собрано, game-tested 161/161

Контракт и реализация: [golem-fishing-burn](analysis/2026-10-19-golem-fishing-burn.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `GolemFishingSmelting.burningTicks` (40 тиков при Fire), `Catch.fireTicks` в `FishingGolemGoals` (решение при вылове, необязательный ключ `Fire` в NBT неопубликованного улова), поле `fireTicks` записи очереди `DeferredItemDrops.Pending` (необязательный ключ `Fire`, границы 0..600, только рыбацкая запись, карантин для невозможных), `ItemEntity.setRemainingFireTicks` при выбросе.
**Built / game-tested:** `:common:test` **447 JUnit**, `:fabric:remapJar`, `:fabric:runGameTest` **161/161** (+`fireGolemCatchesAreDeliveredAlightForTwoSeconds`; дополнены `fireUpgradeCooksTheFishingGolemsCatch` и живой `realFishingGolemsCastIntoPondsAndDeliverCooksAndExtraCatches`: улов Fire-голема горит, остальных — нет), JAR SHA-256 `0064e584…804abe`.
**Мутации:** 11 у GitHub; ожидаемый выживший — сохранение неопубликованного улова с огнём.
**Seen:** нет. **Не проверено:** вид огня, гашение в воде, сеть/два игрока, save/reload мира, NeoForge.
**Следующее:** остаток M10 (оригинальный рендер предмета-плейсера сундука `ItemTrunkSpawnerRenderer`, Butcher с дротиком), затем M01 R10–R16, M03/M08, M07 enchant-инфузия, Thaumatorium.

### Актуально19.10 (двенадцатая группа: дротикомёт голема `R`, Claude): реализовано, собрано, game-tested 160/160

Контракт и реализация: [golem-dart](analysis/2026-10-19-golem-dart.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `GolemDartRules`, `EntityDart` (родная стрела, сущность `thaumcraft:dart`), цель `Dart` в `GuardGolemGoals` (приоритет 2, только при `R`), `EntityGolemBase.shootDart` и левая рука (`LEFT_ARM`), перестройка целей при установке `R`, предмет `golem_decoration_dart` с аркан-рецептом, `RenderDart`, lang en/ru, подсказки Air/Water.
**Built / game-tested:** `:common:test` **447 JUnit** (+10: `GolemDartRulesTest` 7, `GolemDartAssetsTest` 3), `:fabric:remapJar`, `:fabric:runGameTest` **160/160** (+`golemDartLauncherShootsAtItsIntervalAndHurtsWhatItHits`: пять настоящих големов на дорожках), JAR SHA-256 `391e9031…faa8`.
**Находки:** сервер падал бы при сохранении мира с летящим дротиком (`AbstractArrow` с пустым стеком) — исправлено; Guard берёт БЛИЖАЙШУЮ цель, поэтому дорожки GameTest нужно разносить; широкая область очистки ломает соседний тест (Fetter).
**Мутации:** 42 (19 JUnit + 23 GameTest) запрошены у GitHub; ожидаемые выжившие — `shoots-without-sight`, `spread-ignores-water`.
**Seen:** нет. **Не проверено:** вид дротика и позы руки, Butcher с дротиком, стрельба через препятствия, статистика разброса, сеть/два игрока, save/reload мира, NeoForge.
**Следующее:** горящий предмет улова (`EntityItem.setFire(2)` при Fire; нужен `fireTicks` в `DeferredItemDrops.Pending`), затем M01 R10–R16, M03/M08, M07 enchant-инфузия, Thaumatorium.

### Актуально19.10 (одиннадцатая группа: Traveling Trunk, Claude): реализовано, собрано, game-tested 159/159

Контракт и реализация: [traveling-trunk](analysis/2026-10-19-traveling-trunk.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `TrunkRules`, `EntityTravelingTrunk` (прыжки за владельцем, телепорт, защита Fire, втягивание Entropy, лечение, смерть, колокольчик с Order, переход в измерение владельца), `InventoryTrunk`, `ContainerTravelingTrunk`, `ItemTrunkSpawner`, рецепт `traveling_trunk`;
клиент — `ModelTrunk`, `RenderTravelingTrunk` (масштаб/крышка по формулам оригинала, злая текстура, значок улучшения), `GuiTravelingTrunk`.
**Built / game-tested:** `:common:test` **437 JUnit** (+13 с записи Liquid, 424: 3 `GolemFetterAssetsTest`, 6 `TrunkRulesTest`, 4 `TravelingTrunkClientAssetsTest`), `:fabric:remapJar`, `:fabric:runGameTest` **159/159**, JAR SHA-256 `e58263e8…450d1`.
**Мутации:** 32 у GitHub (`mutation.yml`): 31 поймана, выжил `the-pull-reaches-far` (предмет за радиусом проверялся слишком рано) — тест усилен, локально пойман.
**CI:** найден и исправлен хрупкий реальный лимит 10 с у `frostFocusCraftsSelectsAndPays` и `shockFocus…` (теперь 60 с, тиковый предел прежний) — на раннере GitHub тест давал 10,02 с.
**Seen:** нет. **Не проверено:** вид модели/значка/экрана, переход вслед за владельцем в другое измерение (в GameTest не исполняется), сеть/два игрока, save/reload мира, NeoForge. Плейсер `trunk_spawner` пока с плоской моделью.
**Следующее:** дротик `R` (`AIDartAttack`, снаряд, рендер), горящий предмет улова.

### Актуально19.10 (десятая группа: Golem Fetter, Claude): реализовано, собрано, game-tested 157/157

Контракт и реализация: [golem-fetter](analysis/2026-10-19-golem-fetter.md).
**Implemented:** блок `golem_fetter` со свойством `active` (вместо метаданных 9/10), `BlockGolemFetter.neighborChanged` по редстоуну (как оригинал — только при смене соседа), `holds()` под ногами голема, `EntityGolemBase.fettered` → `isImmobile()` и `canWork()`, поза `setInactive`, рецепт `SSS/IRI/BBB`, лут, lang.
**Built / game-tested:** `:common:test` **427 JUnit** (+3 `GolemFetterAssetsTest`), `:fabric:runGameTest` **157/157** (+`golemFetterHoldsGolemsWhileItsRedstoneSignalLasts`: три настоящих Fill-голема).
**Мутации:** 16 у GitHub, пойманы 13; три выживших объяснены (клиентская охрана; `isImmobile` и `canWork` перекрываются).
**Seen:** нет. **Не проверено:** поза неактивного голема на экране, сложные редстоун-схемы, сеть/два игрока, save/reload мира, NeoForge.

### Актуально19.10 (девятая группа: очередь Liquid у Entropy, Claude): реализовано, собрано, game-tested 156/156

Контракт и реализация: [golem-liquid-queue](analysis/2026-10-19-golem-liquid-queue.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `GolemLiquidQueue` (обход в глубину по 26 соседям, дальние первыми, маркер последним), якорь добычи в `EntityGolemBase`/`LiquidGolemGoals`, перепроверка записей, предел 65536.
**Built / game-tested:** `:common:test` **424 JUnit** (+10), `:fabric:remapJar`, `:fabric:runGameTest` **156/156** (+`entropyQueuesTheFarthestConnectedWaterForLiquidGolems`: три настоящих голема с тиглями), JAR SHA-256 `cef678ad…c892c`.
**Мутации:** 19, пойманы 14; пять не пойманы — дублирующие защитные слои (см. контракт).
**CI на GitHub:** обычная проверка (`build.yml`) уже идёт на каждый пуш; в ней почти всегда красен старый `frostFocusCraftsSelectsAndPays` (жёсткий лимит 10 с реального времени на медленном раннере, чинится отдельно).
Мутационный workflow `mutation.yml` (скрипт `scripts/mutation/mutate.py`, группы `scripts/mutation/groups/*.json`) запускается пушем файла-заявки `scripts/mutation/run-request.txt` или вручную; пробный запуск на двух мутантах зелёный.
**Seen:** нет. **Не проверено:** вид откачки, сеть/два игрока, save/reload мира, NeoForge.
**Следующее:** дротик `R`, горящий предмет улова, Traveling Trunk.

### Актуально19.10 (восьмая группа: нечёткое сравнение предметов Entropy, Claude): реализовано, собрано, game-tested 155/155

Контракт и реализация: [golem-item-match](analysis/2026-10-19-golem-item-match.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `GolemItemMatch` (теги / без урона / без компонентов), `GolemData.itemMatch` (JSON `item_match`, сеть: бит 0x40 + байт, NBT `ItemMatch`), нечёткие поиск/счёт склада и пробы Fill/Empty/Sorting,
расширение Empty/Use по тегам одним Entropy, цвета слотов, кнопки меню 55–57 и три флажка `GuiGolem`, lang en/ru.
**Built / game-tested:** `:common:test` **414 JUnit** (+11), `:fabric:remapJar`, `:fabric:runGameTest` **155/155** (+`entropySwitchesChangeWhatRealGolemsFetchEmptyAndSort`: десять настоящих големов в пяти парах), JAR SHA-256 `811a950b…eb55c`.
**Мутации:** 41, пойманы 37; не пойманы два эквивалентных, строгая проба `fillTick` (перезапуск маршрута) и ядро Use (нет игрового стенда — подключено, не проверено).
**Уроки:** GameTest с настоящими тегами поймал правило групп `c:tools/pickaxe` (JUnit не видит тегов); соседний GameTest стирает блоки вокруг своей структуры — линии стоят этажами на одном пятачке.
**Seen:** нет. **Не проверено:** флажки/подписи GUI, Use в игре, сеть/два игрока, save/reload мира, NeoForge.
**Следующее:** очередь Liquid у Entropy, дротик `R`, горящий предмет улова, Traveling Trunk.

### Актуально19.10 (седьмая группа: аксессуары голема, Claude): реализовано, собрано, game-tested 154/154

Контракт и реализация: [golem-decorations](analysis/2026-10-19-golem-decorations.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`.
**Implemented:** `GolemDecorations` (чистые правила), `GolemData.decoration` (JSON/сеть: бит 0x20 + строка/NBT), семь предметов `golem_decoration_*` и аркан-рецептов, установка ПКМ, эффекты (здоровье, броня, скорость, урон, радиус, ремонт, пометка опыта у забрала),
NBT `Decoration`, подсказка плейсера, сохранение при обычной отдаче колокольчиком (приседающая разборка теряет), псевдонимы книги `Tiny*`, иконки, lang en/ru, запись `lastHurtByPlayerTime` в accesswidener.
**Built / game-tested:** `:common:test` **403 JUnit** (+10 `GolemDecorationsTest`), `:fabric:remapJar`, `:fabric:runGameTest` **154/154** (+`golemAccessoriesInstallChangeStatsAndSurviveTheBell`), JAR SHA-256 `9668a7c3…1fc40`.
Асинхронные дорожки: настоящий Guard с забралом убил паука — 5 опыта (без забрала 0); ремонт феской 50 тиков против 76.
**Мутации:** 25 по одной, все ловятся (11 чистых правил — JUnit; 14 проводок — GameTest/JUnit). Урок: скрипт читал только исключения синхронных тестов и «пропустил» две асинхронные мутации; после правки диагностики обе ловятся.
**Seen:** нет. **Не проверено:** модель/накладки на голема, значок плейсера, дротик `R` (не перенесён), сеть/два игрока, save/reload мира, NeoForge.
**Следующее:** нечёткое сравнение предметов Entropy ([контракт](analysis/2026-10-19-golem-item-match.md)), очередь Liquid, дротик `R`, Traveling Trunk.

### Актуально19.10 (шестая группа: продвинутый голем, Claude): реализовано, собрано, game-tested 153/153

Контракт и реализация: [golem-advanced](analysis/2026-10-19-golem-advanced.md); аудит и независимая проверка сборки в контейнере: [full-project-audit](analysis/2026-10-19-full-project-audit.md). Коммиты — только на приватную ветку `claude/affectionate-brahmagupta-qgsfl3`, не публиковалось.
**Implemented:** `GolemData.advanced` (JSON/NBT/сеть: бит 0x10 последнего байта), слот улучшений `материал + 1`, скорость ×1,1 после Air, радиус `+max(20 %, 2)`, атака 18 тиков, подсказка `tc.adv`, колокольчик сохраняет флаг даже в голом плейсере,
`InfusionResultPatches` + `result_patch` рецепта (центр копируется и патчится), рецепт `infusion/golem_advanced`, псевдоним книги `AdvancedGolem`, иконка исследования `ag`.
**Built / game-tested:** `:common:test` 393 JUnit (+8 `GolemAdvancedTest`, +1 `CrystalLootTableTest` до этого), `:fabric:remapJar`, `:fabric:runGameTest` **153/153** (+`advancedGolemsGainSlotSpeedRangeAndKeepTheirFlag`, +`crystalBlocksDropSixOwnShards`); JAR SHA-256 `769b2d4b…60c3`.
Исправлены два устаревших теста: `rawRecipeWire` (маркер патча) и `GolemGuardTargetsTest` (бит 16 — флаг advanced, повреждены только старшие биты).
**Мутации (по одной, затем побайтное восстановление, sha256 сверен):** слоты, скорость, радиус, задержка атаки, NBT-сохранение, голый плейсер при разборке, патч центра, ёмкость плейсера — ловятся GameTest-ом (и частью JUnit); бит в проводе — JUnit; избыточная проверка `canApply` в `matches` вначале **не ловилась** (ингредиент уже ограничивает центр), после добавления прямого теста с «свободным» ингредиентом — ловится.
**Seen:** нет. **Не проверено:** значок плейсера/вид голема, полный ритуал алтаря с этим рецептом, сеть/два игрока, save/reload мира, NeoForge.
**Следующее:** декорации ([контракт](analysis/2026-10-19-golem-decorations.md): H G B F V P M сразу, дротик R вместе с `AIDartAttack`), затем остаток Entropy и Traveling Trunk.

### Актуально19.10 (пятая группа: переключатели целей Guard (Order), улов Fishing (Order/Entropy/Air), Claude): реализовано, собрано, game-tested 151/151

Реализация, отклонения, мутации и точные формулировки: раздел «Пятая группа» в [golem-upgrades-implementation](analysis/2026-10-19-golem-upgrades-implementation.md); контракт — [§5a](analysis/2026-10-19-golem-upgrades.md). Ничего не закоммичено и не опубликовано; только Fabric/common, NeoForge не запускался.
**Implemented:**
- **Переключатели целей Guard (Order)** — `GolemGuardTargets` (маска из положительных битов Monsters 1 / Animals 2 / Players 4 / Creepers 8, кнопки меню 51–54 как в оригинале), `GolemData.guardTargets` (JSON `guard_targets`, сеть — байт в конце, опускается при умолчании), `EntityGolemBase` (NBT `GuardTargets`, `isGuardTarget` по категориям),
  `ContainerGolem` (данные меню 17–18; сервер сам проверяет Guard+Order), `GuiGolem` (четыре флажка на оригинальных позициях), подсказка плейсера. **Главное отклонение — безопасное умолчание:** в оригинале у свежего Order-Guard бьются и животные, и игроки, и криперы (все биты `toggles` сброшены = «можно»); порт по умолчанию бьёт только монстров,
  остальное включает владелец в GUI. Оригинальный `toggles` (инвертированные биты, общий с Fill/Use/Entropy) не переиспользован: свой ключ, непустой оригинальный `toggles` по-прежнему «неподдержанные данные». Игрок-цель: не владелец, не творческий/наблюдатель и только при включённом PvP сервера (новые защитные условия).
- **Улов Fishing** — Order: шанс сокровища `0,05 + 0,0125·n`; Entropy: junk `0,1 − 0,025·n`; Air: второй улов при `nextInt(10) < n` (`GolemUpgrades.fish*`, `GolemFishingLoot.nextCatch/catchCount`, цикл в `FishingGolemGoals.activeTick`: ролл → плавка → траектория → публикация для каждого улова, следующий катится после публикации предыдущего).
**Built (финал):** `guard-fish-final-build` (`.tmp/axe-stream-build.ps1`): `:common:test` **384 JUnit, 0 failures/errors/skips** (+16: `GolemGuardTargetsTest` 11, `GolemFishingUpgradesTest` 5), `remapJar` + `verifyTestIsolation`/`verifyProductionIsolation`, exit 0; JAR 11256400 bytes, SHA256 `f1112abe0754f62c2588f249e8cb55d96474050e1b36887bef8bb4b0dbb0d6b5`.
Два прежних теста чинились по раскладке/порядку, не по поведению: сетевые «склейки» `GolemColorsTest`/`GolemDataUpgradesTest` считали смещение от конца потока (теперь там байт маски), а `GolemDataUpgradesTest` создавал `ItemStack.EMPTY` до `Bootstrap` и падал лавиной, когда Gradle запускал его первым.
**Game-tested (Fabric dedicated-server GameTest): ДА, 151/151** — `guard-fish-final-gametest` (`.tmp/axe-stream-gametest.ps1`), +3 к 148: `orderGuardTargetSwitchesChooseWhomRealGuardsFight`, `orderEntropyAndAirChangeWhatAFishingGolemCatches`, `realFishingGolemsCastIntoPondsAndDeliverCooksAndExtraCatches`.
Первый из них (три настоящих игрока, настоящие големы/меню/плейсер/колокольчик): решение `isGuardTarget` для 16 масок × 12 видов существ, PvP выключен/творческий/наблюдатель/владелец, настоящее меню (кнопки 51–54 и подделанные), NBT/плейсер/колокольчик/поддельные данные, и **четыре настоящих Guard, которые идут и бьют**:
default убивает паука и 160 тиков не трогает корову, крипера и настоящего не-владельца-игрока; ANIMALS убивает корову; CREEPERS — крипера; PLAYERS бьёт чужого игрока (66 из 1000 за 200 тиков), стоящий **ближе** владелец цел. Второй: внедряемые случайные числа, все края окон Order/Entropy/пруд/Fire/Air и 12000 бросков настоящей добычи.
Третий — **первая живая рыбалка в тестах**: 24 Tallow-голема в трёх группах ловят в свои пруды; Fire-группа не отдаёт сырой рыбы, остальные — жареной; в обоих прогонах у Air-группы наблюдался один улов с двумя предметами (наблюдение, не утверждение).
**Мутации (разово, затем побайтно восстановлено, хэши сверены):** JUnit — десять правок, падают 12 тестов; GameTest — 11 правок по одной за полный прогон, **10 ловятся** (животные всегда цель, крипер как монстр, PvP не учитывается, владелец не защищён, мёртвая кнопка меню, маска не пишется в NBT, Order/Entropy/Air не доходят до броска,
цель рыбалки без `nextCatch` → «a Fire golem never delivers a raw fish: 1 minecraft:cod»), **одна нет**: `catches = 1` в `activeTick` (живой тест не требует двойного улова; `catchCount` проверен отдельно).
**Seen:** нет — флажки и подписи `GuiGolem`, строка плейсера, тексты книги/lang не запускались в клиенте. **Не проверено:** рендер GUI и синхронизация данных меню по сети, реальные protection-моды и два игрока по сети, реальный save/reload мира, обязательное наблюдение двойного улова, NeoForge.
**Остаётся по контракту §5a:** горящий предмет улова (визуал), нечёткое сравнение предметов и очередь Liquid (Entropy), `1 + n` слотов Liquid (сознательно одна страница), «advanced» голем, декорации (дротики Air/Water), иконки слотов на теле, Traveling Trunk.

### Актуально19.10 (четвёртая группа: слоты 6+6n, жарка улова Fishing, цвета Order, Claude): реализовано, собрано, game-tested 148/148

Реализация, отклонения и мутации: раздел «Четвёртая группа» в [golem-upgrades-implementation](analysis/2026-10-19-golem-upgrades-implementation.md); контракт — [§5a](analysis/2026-10-19-golem-upgrades.md). Ничего не закоммичено и не опубликовано; только Fabric/common, NeoForge не запускался.
**Implemented:**
- **Слоты `6 + 6n` (Fire)** — Fill/Empty/Gather/Use получают по странице из шести призрачных слотов на каждый Fire (максимум 18, два Fire). `GolemData.filters/wantedCounts/colors` имеют размер 6/12/18 (`slotCount`, `fireCount`), короткий список из шести дополняется, любой другой размер — ошибка (не обрезается);
  NBT `Inventory` хранит индекс слота, `WantedCounts` — 6/12/18 чисел, запись за пределами страниц делает голема неподдерживаемым; меню листает страницы кнопками 60/61 (`ContainerGolem`). **Отклонение:** Liquid остаётся на одной странице (его единственная жидкость — вода, оригинальные `1 + n` слотов были бы дублями); остальные ядра без призрачных слотов.
- **Жарка улова Fishing (Fire)** — `GolemFishingSmelting.apply` в `FishingGolemGoals.activeTick`: улов заменяется результатом зарегистрированного рецепта плавки (треска, лосось), остальное идёт как есть. **Отклонение:** оригинал ещё поджигал выданный предмет на 2 с (AIFish:124-130) — это визуальный огонь, потребовал бы нового поля в долговечной очереди улова; не воспроизводится.
- **Цвета Order** — `GolemMarker.color` (−1 «любой», 0–15 — цвета красителей) и цвета призрачных слотов Fill/Empty/Use (`GolemData.colors`, `EntityGolemBase.getSlotColor/setSlotColor/colorsMatching/markerAccepts`). Маршрутизация по оригинальному `equalsFuzzy`: слот −1 принимает любой маркер, иначе цвет должен совпасть точно; подключено к выбору источника и забору Fill (`InventoryGolemGoals`, `EntityGolemBase.takeFromInventory`),
  выдаче Empty (`inventoryAccepts`, `placeCarriedAtMarker`, `markedWorldThrowTarget`) и целям Use (`UseGolemGoals.findTarget/current`). Колокольчик (`GolemMarker.click`): без Order — переключение маркера; с Order — клик крутит `любой → 0 … 15 → удалён`, клик с приседанием удаляет сразу, перекрашенный маркер встаёт в конец списка (оригинальные remove + add); в action bar выводится цвет (`tooltip.thaumcraft.golem.marker_color`).
  Меню: кнопки 100–105 крутят цвет слота страницы (`ContainerGolem.COLOR_BUTTON`), в GUI под каждым слотом цветной значок (`GuiGolem`), `PAGER_Y` 66→72.
  Формат: NBT `colors` (ключ оригинала, байты; опускается, если все −1), JSON `colors`/`color` маркера опускаются при значении по умолчанию — старые предметы сериализуются как раньше; сетевой кодек получил байт цвета на маркер и список цветов слотов.
  **Отклонения/не перенесено:** цвета без Order — неподдерживаемые данные (не молча отбрасываются); цвет слота есть только у Fill/Empty/Use (у Gather GUI-цвет в оригинале без эффекта, у Liquid/Sorting цвет не читается — Sorting опрашивает цвет −1 = «любой»); нечёткое сравнение предметов — это Entropy, `colorsMatching` сравнивает точно (`isSameItemSameComponents`);
  колокольчик считает маркеры одной точкой по `sameSpot` (измерение + блок + грань, без цвета), чтобы остаточный цвет не давал дубль — оригинальное `Marker.equals` сравнивает и цвет; текст action bar и значки меню собственные.
- **Найдено и исправлено попутно:** `GolemData.equals/hashCode` сравнивали только первые шесть фильтров (баг страниц) и не видели цветов — теперь покрывают все страницы, цвета слотов и цвета маркеров (тест `equalityCoversSlotColoursMarkerColoursAndEveryGhostPage`).
**Built (финал):** `colors-final-build` (`.tmp/axe-stream-build.ps1`): `:common:test` **368 JUnit, 0 failures/errors/skips** (+22: `GolemDataGhostSlotsTest` 8, `GolemColorsTest` 14), `remapJar` + `verifyProductionIsolation`, exit 0; JAR 11249813 bytes, SHA256 `badcf4a03638c77b654ed5eed95b467727f2f2773da4fd260d3d198908992554`.
**Game-tested (Fabric dedicated-server GameTest): ДА, 148/148** — `colors-final-gametest` (`.tmp/axe-stream-gametest.ps1`), +3 к 145: `fireUpgradesAddPagesOfGhostSlotsToItemFilterGolems`, `orderColoursRouteMarkersAndSlotsAndTheBellCyclesThem`, `fireUpgradeCooksTheFishingGolemsCatch`.
Настоящий игрок (`placeNewPlayer`), настоящие големы и меню; Order-тест: цвет слота нужен Order и ядро Fill/Empty/Use; меню циклит 17 нажатий `−1 → 0 … 15 → −1`, вторая страница красит слот 7; `markerAccepts`/`colorsMatching` на Empty-големе и без Order; колокольчик настоящим `useOn` — 18 кликов, перекраска в конец, приседание, голем без Order; NBT/плейсер/legacy/подделанные цвета; **настоящий Fill-голем между тремя сундуками** — ближний сундук с маркером цвета 5 не тронут, предметы идут из дальнего сундука цвета 3.
Smelting-тест гоняет `apply` на настоящем Fishing-големе с Fire и без и на реестре рецептов сервера (треска, лосось → жареные; ещё 14 уловов без рецепта — рыба, мусор, сокровища — нетронуты).
Слоты `6 + 6n` проверены тем же настоящим игроком: установка Fire (до двух, третий отклонён), страницы меню, клик по призраку страницы три пишет слот 12, NBT/плейсер/legacy-запись из шести, настоящий Gather-голем берёт предмет со второй страницы и не трогает землю; мутации числа слотов и смещения страницы меню роняли именно этот тест.
**Мутации (разово, затем побайтно восстановлено, хэши сверены):** JUnit — `accepts` всегда истина, цикл колокольчика обрезан, Order не нужен для цвета слота, `equals` без цветов, `equals` только по первой странице, кодек маркера не пишет цвет, сетевой список цветов не читается — каждый роняет именно свой тест (`.tmp/mutate_colors.py`, наборы `junit`, `junit2`, `junit3`).
GameTest, по одному прогону: фильтры `markerAccepts` в маршрутизации Fill убраны → «ближний сундук не тронут: 56»; шаг цикла в меню → «click 15 … (golem −1, menu −1)»; `apply` без проверки Fire → «голем без Fire отдаёт сырую треску»; `markerAccepts` всегда истина → «цветной слот берёт ровно свой цвет»; колокольчик без Order → «colour 0: golem []»; NBT не пишет `colors` → «NBT keeps twelve slot colours».
**Seen:** нет — клиент, GUI (значки цвета, пейджер, `PAGER_Y`), action bar, книга не запускались. **Не проверено:** реальная рыбалка со снастью (в рантайме проведён шаг плавки, строка вызова в `activeTick` только компилируется); маршрут Empty/Use по цвету в живом мире (Empty/Use проверены на уровне `markerAccepts`, живой маршрут — только Fill);
синхронизация данных меню на клиент по сети; два игрока; реальный save/reload мира (проверен только NBT-круг в процессе); NeoForge.
**Остаётся по контракту §5a:** переключатели целей Guard (небезопасный умолчательный режим), шанс сокровища Fishing (Order), горящий предмет улова, нечёткое сравнение/очередь Liquid/плохой улов (Entropy), двойной улов Fishing (Air), «advanced» голем, декорации, иконки слотов на теле, Traveling Trunk.

### Актуально19.10 (третья группа Fire/Order, Claude): реализовано, собрано, game-tested 145/145

Контракт по двенадцати ядрам записан в [golem-upgrades §5a](analysis/2026-10-19-golem-upgrades.md). **Implemented:** Fire и Order — предметы/рецепты/модели/lang/алиасы/иконки (все шесть типов теперь поддержаны, `GolemUpgrade` ported=true для всех);
Fire — поджог цели `4n` с после **удавшегося** удара Guard/Butcher (`EntityGolemBase.doHurtTarget`, `GolemUpgrades.igniteSeconds`); Order — пересадка Harvest (`GolemHarvestPlatform.replant`, `GolemReplant`): ItemEntity в коробке ±6 вокруг голема, толчок свежих дропов к голему,
какао против джунглевого бревна, семена (пшеница/свёкла/арбуз/тыква/морковь/картофель/адский нарост) в девять клеток ниже сломанной колонки, по одному настоящему семени за посадку ПОСЛЕ `setBlock`, цепочка разрешений (`level.mayInteract`, Architectury `BlockEvent.PLACE`, новый `Bridge.allowsPlacement` → Fabric `UseBlockCallback`).
**Built (промежуточная сборка до исправления света; её JAR устарел):** `golem-fire-order-build1` — `:common:test` **346 JUnit, 0 failures/errors/skips** (+7: `GolemReplantTest` 6, `GolemUpgradesTest` +1; тесты, считавшие Fire/Order «неподдержанными», переведены на id 6/7), `remapJar` + `verifyProductionIsolation`, exit 0, JAR 11239503 bytes, SHA256 `2DAF69FD0E38B9E4A0219D7FC200DA55143941B199101C17304DE49F4918581D`.
**Game-tested (Fabric dedicated-server GameTest): ДА, 145/145.** Первый прогон `golem-fire-order-gametest1` упал (144/145) только на новом `fireIgnitesGuardTargetsAndOrderReplantsHarvestedCrops`
(`GolemFireOrderRuntimeChecks` + `fabric/.../GolemPlacementProbe`, 8 параллельных полос). Предыдущая сессия остановилась по лимиту, причину не нашла; найдено **три независимые причины, одна из них — настоящий дефект кода:**
1. **Дефект кода (исправлен):** сразу после уборки **непрозрачного** блока (арбуз/тыква) свет клетки в 1.21 устаревший (`raw=0`, асинхронный движок освещения), и `CropBlock.canSurvive` отказывает пшенице в клетке 0 (клетка сломанного блока). В оригинале 1.7.10 свет обновлялся сразу. Исправление в `GolemHarvestPlatform`:
   `survives()` — если собственные правила блока отказывают только из-за света (посевная культура/стебель, ферма ниже, пустая клетка), свет **предсказывается по соседям** (−1 за шаг, сверху при небе 15 без потерь; тёмный погреб по-прежнему отказывает); и `replant()` в два прохода — сначала клетки, устоявшиеся по штатному свету (оригинальный порядок «предмет→клетка» без изменений), потом клетки только с предсказанным светом, иначе посадка соседа через обновление форм (`updateShape`) **удаляла** только что посаженную пшеницу в тёмной клетке (семя тратилось зря).
   При устоявшемся свете проход 0 — весь исходный цикл. Порядок посадки меняется только для клетки, где был непрозрачный блок.
2. **Фикстура теста:** паук создавался через `test.relativePos`/`spawnWithNoFreeWill`, а структура GameTest повёрнута на 180° — паук оказывался за 226 блоков от голема, Guard никого не бил (даже без Fire). Теперь спавн в абсолютных координатах.
3. **Хрупкие утверждения теста:** порядок событий PLACE/UseBlock допускает оба порядка (исходный и «клетка сломанного блока последней»); направление толчка дропов зависит от случайной геометрии, поэтому в рантайме проверяются подъём (`y < 0,12`, а не `0,2`) и предел сдвига (≤ 0,25), направление — `GolemReplantTest`.
**Built:** финальный `fire-order-final-build`: `:common:test` **346 JUnit, 0 failures/errors/skips**, `remapJar` + `verifyProductionIsolation`, exit 0; JAR SHA256 `30e0c30f8c32214eb4fcb72c00bad55ece5342d4896da11f449af4a84ffdf99e`.
**Финальный GameTest** `fire-order-final-gametest`: exit 0, **145/145**. Покрыто: Fire/Order ставятся настоящим игроком и сохраняются как байты `[2,4]`; настоящий Guard с Fire поджигает паука на 4 с после **первого удавшегося** удара, без Fire — не поджигает (и через 60 тиков тоже); Order-Harvest: пересадка семян в три клетки с фермой (четыре семени → одно, платёж по одному за посадку), пропуск клеток без фермы, запрет Architectury (`BlockEvent.PLACE`), запрет Fabric (`UseBlockCallback`) без траты семян, пшеница и контроль без Order, какао.
**Финальные прогоны после последней правки** (`final-icon-build`, `final-icon-gametest`): сборка exit 0, 346 JUnit, **145/145 GameTest**, JAR SHA256 `30e0c30f8c32214eb4fcb72c00bad55ece5342d4896da11f449af4a84ffdf99e`. В них вошла правка иконки исследования ELEMENTALAXE: в шаблон `ResearchIconDisplay.STACK` добавлен `itemAxeElemental` (замечено сессией улучшений; проверено только сборкой, не в клиенте).
**Мутационные проверки (разово, затем побайтно восстановлено):** (1) поджог Fire отключён → упал на «паук горит 4 с после первого удара: −1 тик»; (2) предсказанный свет отключён → упал на «пшеница не посажена в клетке 0». Остальные утверждения мутациями отдельно не проверялись.
**Не проверено:** клиент, книга, рендер, сеть; реальные protection-моды; Fire на Butcher и на ударе по игроку/PvP; жарка улова Fishing; ночное/подземное освещение (только день на поверхности); NeoForge.
Остальное Fire/Order (переключатели целей Guard — небезопасный умолчательный режим, шанс сокровища Fishing) — пробелы по контракту §5a; слоты `6+6n`, жарка улова Fishing и цвета маркеров перенесены в четвёртой группе (блок выше).

### Актуально19.10 (после первой группы улучшений, Claude): вторая группа — вместимость Earth и радиус Water

Реализация, решения и результаты: раздел «Вторая группа» в [golem-upgrades-implementation](analysis/2026-10-19-golem-upgrades-implementation.md). Ничего не закоммичено и не опубликовано; только Fabric/common, NeoForge не запускался.
**Implemented:** вместимость Earth — `carry = base + min(16, max(4, base))·Earth` (E:360), вода `floor(sqrt(carry))·1000` (E:364-366), эссенция = `carry`; потолки полезной нагрузки груза поднялись до 8000 мБ / 64 (Thaumium + 2 Earth);
`GolemWaterCargo/GolemAspectCargo.capacity/validate(material, earth)` заменили `capacityForMaterial/validateMaterial`, одним изменением переведены `getCarryLimit`/новый `getFluidCarryLimit`, `waterSpace`, `liquidReady`, `alchemyReady`, `applyWaterCargo`, `applyAspectCargo`, `applyAspectCargoRaw`,
`readWaterCargo`, `readAspectCargo`, кредит воды в `GolemHarvestPlatform`, плейсер (Earth из его `GolemData.upgrades`), ёмкость руки Use и рендер банки эссенции. Custody: разборка по-прежнему отказывает при воде/эссенции, груз без нужного Earth остаётся сырыми данными, плейсер недействителен.
Water — `getRange() = 16 + 4n` (E:461, без декорации G и «advanced»): живой радиус вместо `final`-полей `radius` (Harvest ÷4, Lumber ÷3, Fishing ÷2) и констант 256 (`isGuardTarget`, `butcherState`, `eligibleItem`, `markedInventory`, `UseGolemGoals`); затем предмет `golem_upgrade_water`
(8 `gold_nugget` вокруг `water_shard`, vis `aqua` 10, исследование `UPGRADEWATER`, алиас `UpgradeWater`, иконка damage 3, модель, lang en/ru, `GolemUpgrade.WATER ported=true`, `ThaumcraftGolems.upgradeItem`).
**Built:** `golem-capacity-range-build1`/`build2` (`.tmp/axe-stream-build.ps1`): `:common:test` целиком **339 JUnit, 0 failures/errors/skips** (+7 в `GolemUpgradesTest`), `remapJar` + `verifyProductionIsolation`, BUILD SUCCESSFUL, exit 0;
JAR 11232053 bytes, SHA256 `3f32d7aa00021fa22964ec2ddb1b1d2ef7fcb13ff8af948b52a7eb02191ee688` (build1 и build2 побайтно одинаковы).
**Game-tested:** `golem-capacity-range-gametest1`/`gametest2` (`:fabric:runGameTest`, `.tmp/axe-stream-gametest.ps1`): exit 0, **144/144** (143 + новый `waterUpgradeWidensTheLiveWorkRadiusOfRunningLumberAndHarvestGolems`),
плюс расширенный `golemUpgradesInstallPersistAndApplyEarthAirEntropy` (настоящие игрок, големы, пауки: ёмкость 8 материалов × Earth, границы груза в плейсере/NBT/колокольчике, запрет разборки с грузом, Guard-диапазон 16/20/24). Новый async-тест: настоящие Lumber- и Harvest-голем не тронули цели на 7 и 5 блоках за 640 тиков, после установки Water настоящим игроком на уже идущие цели срубили верхний блок ствола и сломали арбуз.
Мутации A–E (ёмкость без Earth, радиусы Lumber/Harvest на 16, Guard на 256, вода/эссенция без Earth) каждая роняют именно тот тест — все откачены (JAR байт-в-байт).
**Seen:** нет — клиент, книга, иконка и модель Water, рендер банки по новой ёмкости не запускались. **Не проверено:** настоящий набор воды до 8000 мБ и перенос 64 эссенции в Decanting/Alchemy, Fishing на живом радиусе (только арифметика),
маркеры и поиск Fill/Empty/Sorting/Gather/Liquid/Alchemy/Butcher при радиусе 20/24, сеть, два игрока, реальный save/reload мира, NeoForge.
**Следующее:** Fire (поджог, плавка улова Fishing, призрачные слоты 6+6n), Order, остальное Air/Entropy, «advanced» голем и декорации, Traveling Trunk.

### Актуально19.10 (после Lumber7, Claude): улучшения големов — первая группа Air/Earth/Entropy

Контракт: [golem-upgrades](analysis/2026-10-19-golem-upgrades.md) (все шесть типов по оригиналу), реализация и результаты:
[golem-upgrades-implementation](analysis/2026-10-19-golem-upgrades-implementation.md).
**Implemented:** предметы `golem_upgrade_air/earth/entropy` (+ арканные рецепты: 8 золотых крупиц вокруг осколка своего типа, vis 10, исследования `UPGRADEAIR/EARTH/ENTROPY`),
слоты по материалу (Straw/Wood/Clay/Stone/Iron 1, Tallow/Flesh/Thaumium 2), установка ПКМ без приседания в первый свободный слот (до двух одного типа), сохранение в `GolemData.upgrades`
(плейсер), NBT `upgrades` (byte[]) и синхронизированный упакованный int, возврат колокольчиком (обычный удар — в плейсер, приседая — каждое улучшение с шансом 1/2).
Эффекты: **Earth** — сила +1 за штуку (Harvest/Lumber/Fishing уже читают её) и рукопашный урон `2+(сила)+n` (Earth дважды, как оригинал); **Air** — скорость ×(1+0.15n);
**Entropy** — шипы `2n+nextInt(2n)` на того, кто ударил напрямую (+ защита от рекурсии, отклонение).
Fire/Water/Order **не зарегистрированы** (нет честного эффекта; id остаются в формате, NBT с ними — неподдержанные сохраняемые данные). Усечения массива как в оригинале нет.
**Built:** `golem-upgrades-build1`/`build2` (`.tmp/axe-stream-build.ps1`): `:common:test` целиком **332 JUnit, 0 failures/errors/skips** (+23 новых: `GolemUpgradesTest` 15, `GolemDataUpgradesTest` 8),
`remapJar` + `verifyProductionIsolation`, BUILD SUCCESSFUL, exit 0; JAR 11230275 bytes, SHA256 `4EC1994A40D87FC3D64CD9BD99FFDA74DB8D77ACE3F3820C319991DBC6950929` (в build1 и build2 побайтно одинаков).
**Game-tested:** `golem-upgrades-gametest1`/`gametest2` (`:fabric:runGameTest`, `.tmp/axe-stream-gametest.ps1`): exit 0, **143/143** (142 + новый `golemUpgradesInstallPersistAndApplyEarthAirEntropy`,
настоящий `ServerPlayer` и голем: рецепты, установка/оплата, эффекты, шипы, NBT save/load, плейсер, размещение, колокольчик, разборка). Мутации (отключить шипы; монетка разборки всегда «оставить») тест ловят.
**Seen:** нет — клиент, книга, иконки/рендер улучшений на теле не запускались. **Не проверено:** реальный бой Guard/Butcher с Earth-уроном, скорость работы Harvest/Lumber/Fishing в мире, ходьба Air, сеть, два игрока, реальный save/reload мира, NeoForge.
**Следующее (по контракту):** вместимость Earth (вода/эссенция/плейсер — материал + Earth), Water (живой `getRange()` вместо `final radius` и констант 256), Fire, Order, остальное Entropy/Air, «advanced» голем, Traveling Trunk.
Замечен посторонний дефект (не правился): в `ResearchIconDisplay.STACK` нет альтернативы `itemAxeElemental` — иконка ELEMENTALAXE по этому пути недостижима. Не закоммичено, не опубликовано.

### Актуально19.10 (после топора, Claude): Lumber7 интегрирован

Заметка: [golem-lumber-implementation](analysis/2026-10-19-golem-lumber-implementation.md). Ядро 7, рецепт
(Harvest-центр + Axe of the Stream + три железных топора `damage 0`, **instrumentum16/arbor16/meto16**, нестабильность 2),
`LumberGolemGoals` (порт `AIHarvestLogs`), `GolemHarvestPlatform.chop` (один конец дерева за срабатывание, полный защищённый
путь на реальной клетке конца), `LumberGeometry` (чистая арифметика). **Исправлен дефект рецепта Fishing11:** стоимость была
`messis16`, оригинал (`ConfigRecipes:373`, `Aspect.HARVEST`) — `meto16`; прежний контракт ошибочно писал «messis».
`lumber-build1`: `:common:test` целиком **309 JUnit, 0 failures/errors/skips** (+7 `LumberGeometryTest`),
`remapJar` + `verifyProductionIsolation`, `BUILD SUCCESSFUL in 41s`, exit 0; JAR 11217653 bytes,
SHA256 `b8e51b6fbea02401abf9ee1c8b0cc7032fc2479caa7946c008781c7c2a8e2645`.
`lumber-gametest1` (`:fabric:runGameTest`): exit 0, **142/142 GameTest**, включая первый в проекте тест с **настоящим
голем-работником** `lumberGolemFellsOneFarLogThroughTheGuardedPlatform` (сам находит дерево, срубает ровно один верхний
блок, одно настоящее бревно в мир, событие BREAK один раз на конце от не-OP пустого актора). Не проверено: серия срубов и
переход на соседнее бревно, отказ защиты, Earth-усиление, save/reload, подбор Gather-големом, визуал, несколько игроков,
NeoForge. Новые файлы тестов: `StreamAxeRuntimeChecks`, `LumberGolemRuntimeChecks` (регистрация в Fabric `ThaumcraftGameTests`),
чистые `StreamAxeGeometryTest`, `LumberGeometryTest`.
Финальный `lumber-build2` после добавления GameTest-файлов: exit 0, `verifyProductionIsolation` PASS, JAR **байт-в-байт тот же**
(SHA256 `b8e51b6f…2645`, 11217653 bytes) — тестовые источники в продакшн-JAR не попали. Незакоммичено: коммитов и push не было.
**Следующая группа (на момент записи не начата; затем начата — см. раздел выше):** улучшения големов (Air/Earth/Fire/Water/Order/Entropy), затем «продвинутые» големы и
оставшиеся M10-элементы; перед этим нужен свой контракт по оригиналу. Рабочий путь запуска — `.tmp/axe-stream-build.ps1` и
`.tmp/axe-stream-gametest.ps1` (см. `scripts/dev.ps1 -Task test|gametest`).

### Актуально19.10 (позже, Claude): Use8/Flesh собраны; Axe of the Stream интегрирован

**Use8 + Flesh4:** `golem-use-build1` (`.tmp/golem-use-build1.log`, 03:00) завершилась
`BUILD SUCCESSFUL in 33s`, exit 0, 19 tasks (main/remap/production isolation). Сверено
по логу в новой сессии; ранее запись об ожидании этой сборки осталась в HANDOFF без
результата. Это только сборка: игровой/GUI/save/visual проход Use8 и Flesh **не выполнялся**.

**Axe of the Stream** (обязательный ингредиент Lumber7) — исходники, рецепт, книга,
ресурсы и 12 чистых JUnit в рабочем дереве; заметка
[axe-stream-implementation](analysis/2026-10-19-axe-stream-implementation.md).
`axe-stream-build1` (`.tmp/axe-stream-build1.log`): `:common:test` целиком (**302 JUnit,
0 failures/errors/skips**, из них 12 новых `StreamAxeGeometryTest`) + `:fabric:remapJar` +
`verifyTestIsolation`/`verifyProductionIsolation`, `BUILD SUCCESSFUL in 1m 6s`, exit 0.
Fabric JAR `fabric/build/libs/Thaumcraft-fabric-0.1.0-alpha.1.jar` 11209086 bytes,
SHA256 `29baa84d5fd9c4539c4cda1561a9e10d75afbb5c96208242d4b4247e458b94e2`
(новые классы, миксин, рецепт, модель и тег в нём присутствуют). Подготовительный
`input_damage` рецепта инфузии уже был в коде — запись «DAMAGE0 центра ещё не расширен»
в axe-stream.md устарела. NeoForge не запускался и не собирался.
Затем существующий `:fabric:runGameTest` (поднимает сервер Fabric с миксинами): первый прогон
нашёл **один давний сломанный тест** `infusionComponentPredicateCodec` (устаревший помощник
`rawRecipeWire`, не топор; починен только помощник); после этого `axe-stream-gametest4`
`BUILD SUCCESSFUL 36s`, **141/141 GameTest**, включая новый целевой
`streamAxeChopsEndpointVacuumsAndFollowsDrops` (подробности в заметке топора).
Не сделано/не проверено: реальная сеть (предсказание клиента при `destroyBlock → false`),
реальные protection-моды, плавность полёта предметов у клиента, вид иконки, Repair-зачарование,
NeoForge. Мутационная проверка нового теста не выполнялась.
Lumber7 (ядро, цель AI, платформа chop) **не начат** — это следующая группа.

### Актуально19.10: Butcher9 собран; начинается Use8

**Use runtime/actor и Flesh block авторы FINAL; root прочитал все owned sources.**
Exact actor bridge согласован с Entity private scope: raw actual hand detach,
post-native actual hand/extras return до очистки actor slots и любых уведомлений.
Fabric callback один, post-HEAD body fence подключён в mixinJSON и Fabric init.
UseOptions6-field forwarding/NBT/normal recall, no-op modes refusal, remainder
publication-only retry и deferred terminal handling подключены. Flesh material4,
настоящий block/loot/craft/crucible/skin/predicates подключены; helper capacities
берутся из enum и корректно включают4. ItemWarp1/taint adjacency остаются gaps.
Все авторы завершены; запускается единственная Fabric build
`.tmp/golem-guard-build.ps1 -RunName golem-use-build1`, main/remap/isolation
и15 dataJUnit (11 прежних +4 options). Main frozen; не запускать другой
Gradle/game/NeoForge. Gameplay/claim/save/visual acceptance отложена, не PASS.
Active shell`sh_151071d1e001x5oUW20AmEJ2bv`; ждать notification без polling.
Ниже — история до текущей интеграции, прежние author waits завершены.

**Butcher actual build PASS38s**, Fabric main/remap/production isolation и11
existingdataJUnit,0failures/errors/skips. Private JAR11139344bytes/SHA256
`a9df7c6ce1e180d468c63b286e8041cee6425fc7a2cf3e047b05e71469450e52`.
Root проверил finishedlog/XML/JAR/realGuard-catalystrecipe; Bore-only distribution
не изменялась. Нет game/harm/save/visual acceptance, никакие защищённые существа
или отклонённые saves не менялись. Gradle/game/code-авторы сейчас не активны.

Следующая actual task — Use8: DEFAULT block/right/notSneak, bare-hand lever/button
без домашнего inventory, либо realWHEAT_SEEDS из supportedhome→markedFarmlandUP.
Use-data prerequisites compiled, но core8 **ещё unsupported/unregistered**.
Три ordinary nondefault controls требуют отдельной реализации, не upgrade gate;
не включать no-op/опасные переключатели или новый entity-target pathway.
Native-route note **FINAL** прочитан root: `analysis/2026-10-19-golem-use-native.md`.
Direct GameMode.useItemOn уже dispatches FabricUseBlockCallback ровно один раз;
manual second event НЕ добавлять. Exact after-HEAD/BEFOREfirstLevel.getBlockState
fence нужен для повторной current target/hand validation. Snapshot actual empty
native actor, move настоящей ссылки/все41slots, settle до release, no snapshots
как replacement stock. APIs заморожены в updated use-api.md.
GolemData имеет6полей/useOptions, прежние2/5arg constructors сохраняются;
futureEntity save/config/recall должен FORWARD useOptions, не брать старый5arg
ctor и молча терять flags. Unknown/unsupported modes retain/refuse.

**Use runtime начат:**`ses_eafb10afaffeBegvovzC6owP6z` владеет EntityGolemBase +new
UseGolemGoals/(малый pure helper). Root не читает/не правит WIP. Public actor
bridges заморожены в updated use-api.md: canUseMarkedBlock/detachUseCargo/
restoreUseCargo; GolemUsePlatform.useBlock/isDetaching/isReturning/isBorrowing/
isActorBorrowed. Actual stock moves byreference, returns settle before release;
UseRemainders publication retry не повторяет мировое действие. Actor автор
`ses_eafb08ef9ffeJ1a2QPyw291dy9` теперь пишет ТОЛЬКО новые GolemUsePlatform,
FabricGolemUsePlatform/GolemUseServerPlayerGameModeMixin +notes. Root не читает
WIP и не дублирует stock/actor implementation. Root уже подключил согласованные
Fabric init и mixinJSON. Third read-only author готовит только новый Flesh4
source-contract, production не меняет. Gradle/game сейчас не активны.
Flesh4 source-contract FINAL прочитан root. Тот же автор теперь пишет ТОЛЬКО
новый BlockFlesh +новые flesh block/item models/loot/recipes/notes; не Enum/Entity/
registry/lang. Source — blockTaint:2, inert, hardness.2/resistance6, normal9rotten
flesh/Silkblock, craft9flesh→block (НЕreversecraft), crucibleGOLEMFLESH/block
+Humanus8/Motus8/Spiritus8. Root уже включил enum/material4 mapping/placer/skin/
registry/aliases/lang и exact BlockFlesh noninventory classification; восемь
material slots не смешиваются с coreGuard4. Existing enum values15HP/armor6/
speed.35/carry4/regen40/firefalse оставлены. **Не читать block-author WIP.**
До FINAL ВСЕХ3авторов (Use runtime/actor/Flesh block) main не собирать.
Known source itemWarp1 declaration — отдельный gap общего item-Warp engine;
researchWarp3 уже существующим completion path, не новый Flesh kill hook.
Root добавил4 узких pure-dataJUnit для UseOptions (8combinations/default/unknown
JSON/native boolean types), без actor/world/bootstrap/framework. Ещё не запускались.
Next `.tmp/golem-guard-build.ps1 -RunName golem-use-build1` включает их вместе
с existing11dataJUnit, Fabricmain/remap/isolation после ОБОИХ code FINAL.

Root уже активировал8 в Data+registry/coreForType, exactEmpty-centered infusion
recipe (comparator/flintsteel0/shears0/lever), book/icon/model/lang; core8 placer
nondefault options отвергает. Menu сохраняет6ghost whitelist и server отказ всем
неподдержанным option buttons; GUI честно показывает DEFAULT и ordinary modes
pending (неupgradegated). Root mayInteract добавил8, bound query теперь отказывает
на GolemUsePlatform.isActorBorrowed даже при пустой руке. **Класса platform пока
нет, main компиляционно незамкнут; build НЕ запускать до runtime/actor FINAL.**

Далее — история до этого completed build; старые RUNNING уже не активны.

**Butcher и Use-data авторы FINAL, root прочитал их source/notes.** Guard4
predicate не расширен, Butcher count/permission свежие на каждом strike,
core9 noGUI/noinventory/no retaliation. UseOptions строгие и отдельные;
core8 всё ещё unsupported/unregistered, default-only data prerequisite не
выдавать за работающий Use. Старые2/5arg constructors сохранены.
**Выполняется один Gradle:** shell`sh_150f0fa35001cYlLXKpMNknR9j`,
`.tmp/golem-guard-build.ps1 -RunName golem-butcher-build1`, actual Fabricmain/
remap/isolation +11existingdataJUnit. Main frozen; code-авторов нет,
другую сборку/game/NeoForge не запускать, ждать уведомления без polling.
Read-only Use-native route author ещё исследует реальное GameMode/Fabric
UseBlockCallback coverage в новом doc, production не меняет. Следом core8,
не smoke engineering; игры/harm/save/visual проверки отложены, не PASS.

Далее — история реализации перед этой сборкой, не активные author waits.

**Сейчас:** actual `golem-alchemy-build1` **PASS35s**, Fabric main/remap/isolation
и11 existing dataJUnit (6aspect/5declaration),0failures/errors/skips. Private JAR
11123385bytes/SHA256`17bfb3fe8d69f6c0156ecff6c4e13c263fd459e4067c3dca890f60e8b6e11c2e`.
Root прочитал finishedlog/XML/составJAR/typed recipe declarations. Это не
нативный transfer/recipe/save/claim/visual PASS; игры/смоука/NeoForge не было.

Butcher9 runtime автор`ses_eafb10afaffeBegvovzC6owP6z` владеет только
EntityGolemBase +GuardGolemGoals/(малый selector helper). Root не читает/не
меняет WIP и не собирает до FINAL. Agreed API butcher-api.md; finite adults
Cow/Pig/Sheep/Chicken/MushroomCow, named/leashed/allied excluded, actual native
harm pipeline и world/spawn permission. >2 eligible exact-class population
проверяется при КАЖДОМ strike, не только выборе. Core4 Guard НЕ расширяется:
отдельный selector9 без retaliation. Ни один world/harm run не разрешён.
Root подключил Data/Core9/recipe/book/icon/model/lang/placer tooltip и native
mayInteract core9. Recipe Guard catalyst +Corpus5/Bestia5, existing research row
warp1 начисляется старым completion path, новых warp hooks не добавлено.
Gradle/game не активны; остальные авторы production не меняют. После FINAL —
actualFabricmain build, не smoke engineering. Поставка Bore-only не заменена.
Use8 source contract FINAL прочитан root (`analysis/2026-10-19-golem-use.md`).
**Коррекция:** ordinary controls Block/Empty space, Right/Left click, Sneaking,
не block/entity; original не даёт новый entity-target/animal/PvP pathway.
Независимый data-автор`ses_eafb0d3d4ffeYOn4l83eswbUeq` теперь владеет ТОЛЬКО
GolemData +newGolemUseOptions: optional шестое use_options поле, прежние2/5arg
constructors/default decode, nonUseDEFAULT и strictunknown/malformed rejection.
USE=8 constant пока НЕ активирует isSupportedCore/registration; никакой Use task
или fake actor/item pipeline не заявлен готовым. Не читать/не править DataWIP,
не собирать до его и Butcher runtime FINAL. Все действительные water/aspect
stock по-прежнему отдельно от configuration/ghosts. Других code-авторов нет.
Подробные owners/APIs/команда следующего build указаны ниже; ранние слова
«авторы сейчас не активны» относятся только к завершённому Decanting checkpoint.

`golem-decanting-build1` **PASS36s**, actual Fabric main/remap/production isolation.
Private JAR11094943bytes/SHA256
`8ba3f263bb313b01cc9c72f5e2621acc439effcefef18202dc38a9215812383c`.
Root прочитал завершённый log/exit и составJAR, исходный recipe/texture;
Bore-only distribution f70b32… не менялась. Gameplay/claim/save/visual checks
не запускались и не стали PASS. Gradle/game/code-авторы сейчас не активны.
Далее Alchemy6: alembic→real aspect cargo→normal marked jars, исходный recipe
с3 настоящими water potions, не просто effectless potion item. Сначала свести
узкий entity/BE raw transfer и potion predicate; не новый provider/test journal.

**Alchemy production-авторы активны, Gradle/game не активны:**
- `ses_eafb10afaffeBegvovzC6owP6z`: EntityGolemBase +newAlchemyGolemGoals;
  bulk source/jar priorities/actual cargo/save/recall, без GUI/item route.
- `ses_eafb08ef9ffeJ1a2QPyw291dy9`: EssentiaTransfers +newGolemAspectCargo,
  два typed native BE/entity raw-before-notify entry points в существующем executor.
- `ses_eafb0d3d4ffeYOn4l83eswbUeq`: InfusionComponentPredicate/InfusionStackRecipe,
  optional water_potion/empty_jar declarations с paired session persistence;
  прежние damage/constructors/central retention unchanged. Existing focused
  declaration tests можно адаптировать, не запускать отдельно.
Recipe автор теперь **FINAL**, root прочитал оба source/notes/existing tests.
JSON/paired-session predicates и bounded stream carry typed flags; water requires
actual WATER PotionContents, empty jar — existing native meaningful-payload guard.
Old damage APIs/constructors и admission-only inputDamage/central retention не
менялись. Agreed `displayStack(preview)` точная сигнатура совпала, book hook готов.
Existing declaration JUnit теперь5cases, ещё не запускался. Автор больше не active;
Executor/cargo автор также **FINAL**: root прочитал GolemAspectCargo и оба
EssentiaTransfers entry/paired raw hook/notes. Существующий executor обновляет
exact native BE и actual Entity stock до notifications; label проверяется
отдельно от SingletonState, partial amounts сохраняются. Нового provider или
second executor нет, прежние scopes не ослаблены. Непрочитанным WIP остался
ТОЛЬКО EntityGolemBase/AlchemyGoals runtime; не собирать до FINAL этого автора.
Read-only Butcher9 source-review автор отдельно готовит следующий contract и
не меняет main. После Alchemy runtime FINAL actual build можно выполнить через
`.tmp/golem-guard-build.ps1 -RunName golem-alchemy-build1`: Fabricmain/remap/isolation
+existing6aspect/5declarationJUnit, без game/смоука/NeoForge. Shared helper changes
не выдавать за native transfer acceptance.
Butcher9 contract теперь FINAL прочитан root (`analysis/2026-10-19-golem-butcher.md`):
Guard catalyst/Corpus5+Bestia5/COREBUTCHER, existing research completion уже
начисляет original warp1, не добавлять per-craft/per-kill warp. Следующая bounded
задача — отдельная policy surplus adult farm stock, повторный >2 exact-class
recheck при каждом strike, без widening Guard и без любых запусков/убийств в
защищённых мирах. Пока код9 не начат; сначала actualAlchemybuild.
Root не читает/не правит WIP и не собирает до ВСЕХ FINAL. AgreedAPI:
`analysis/2026-10-19-golem-alchemy-api.md`. Root уже добавил core6/data/registry/
recipe/model/lang/book alias/icon/Placer payload dispatch/core6native mayInteract;
client делает только исходный material-size display jar/fraction+arm splay,
не настоящий64unitstock. Root book preview уже вызывает agreed
pairedPredicate.displayStack, чтобы рисовать настоящие water potion components;
после recipe FINAL сверить точную сигнатуру.
Новая группа ещё НЕ скомпилирована/не экспортирована/не принята в игре.

Ниже — история Decanting-интеграции, старые RUNNING уже завершены.

**Decanting5 интегрирован, actual Fabric build выполняется.** Runtime FINAL
прочитан root: LiquidGoals/Entity APIs совпали с source/root TileCrucible/Placer.
Сохранение stock отдельно, один fluid ghost, normal recall с водой, частичные
остатки и отказ dismantle с водой подключены. Root дополнительно включил
ItemGolemPlacer.isPlacementInProgress в canWork/canEditLinks (canConfigure
делегирует), и завершает deferred native removal ровно после raw water scope:
ранний return из remove больше не забывает настоящий native removal request.
**Активен только Gradle** shell`sh_150d6f8a8001SaJDidAQbdOzP7`,
`.tmp/straw-golem-build.ps1 -RunName golem-decanting-build1`: actual Fabric
main/remap/isolation; main frozen, авторы production не активны. Не poll,
не запускать второй Gradle/game/NeoForge. Gameplay/visual/claim/save acceptance
по-прежнему отложены; известная потеря воды при смерти — original, не исправлена.
После build продолжать Alchemy6 по готовому contract, не smoke framework.

Далее — история перед этой сборкой; старые ожидания авторов уже завершены.

`golem-fishing-build1` **PASS41s**, actual Fabric main/remap/production isolation.
Private JAR11073129bytes/SHA256
`013564631e37ca78b5aa689765c3a5f6b6654ab8d39b717ce932df487c1b4eee`.
Root проверил завершённый лог/exit/составJAR/точный recipe и fish-core texture;
экспортированный Bore-only JAR остался f70b32… . Игры/смоука/NeoForge не было,
новых gameplay/visual acceptance нет. Gradle/game сейчас не активны.
Следующий unit — Decanting5: actual source→water cargo→crucible, без bucket-item
подмены/нового тестера. Узкий read-only native commit review ещё активен:
`ses_eafb08ef9ffeJ1a2QPyw291dy9`, только новый golem-water-commit.md.

**Decanting runtime**`ses_eafb10afaffeBegvovzC6owP6z` теперь владеет только
EntityGolemBase +новымLiquidGolemGoals; root не читает/не меняет WIP. Agreedraw
bridges описаны в decanting-api.md. Root добавил core5/recipe/icon/book/model/lang,
placer строгий WaterCargo read/material+core validation/apply, readonly tooltip,
typed `TileCrucible.fillWaterFromGolem` (raw debit+raw water add до sync), native
mayInteract разрешает5 без fake break-query. Model carry-pose дляcore5 и display-
only native bucket/GUI mB; original OBJbucket/free-surface ещё визуальный gap,
native sprite — объявленная адаптация, не stock. Поставка не меняется.
Root также добавил static ItemGolemPlacer.isPlacementInProgress() для запрета
recall/work из ADD callback до оплаты настоящего placer. **После Entity FINAL
root должен включить этот guard в canWork/canEditLinks/canConfigure**; сейчас
EntityWIP не править. Native commit note FINAL прочитан; root реализовал source
removal в существующем GolemHarvestPlatform.collectSourceWater: actual loaded
native chunk WATER→AIR, raw worker credit, затем vanilla flags11 notification
tail и actual-old-state AFTER. Heightmap/light/dirty bookkeeping остаётся native
LevelChunk, не прямой section write. Новых mixins/журналов/providers нет; unknown
addon chunk hooks явно вне гарантии. Runtime bridges ещё у автора, main пока не
замкнут; не собирать до FINAL. Root-owned TileCrucible/Placer/client изменения
также не скомпилированы.
Alchemy6 next-source contract FINAL прочитан root:
`analysis/2026-10-19-golem-alchemy.md`. Это реальная следующая после core5
dependency: Liquid-centered infusion +normal empty jar +3 water POTION bottles,
alembic→owned aspect cargo→marked normal jars. Не substitute blank/potion bottle
без water components. Golem не BE: существующие BЕ-only transfer entry points
нельзя выдать за готовый entity cargo bridge. Пока новый Alchemy-код не пишется;
сначала замкнуть текущий Decanting runtime/source/recall и собрать Fabric.

Ниже сохранена история Fishing-интеграции и предыдущего build RUNNING.

**Fishing runtime/client/water-data авторы FINAL; actual Fabric build выполняется.**
Root прочитал FishingGolemGoals/Entity runtime и notes, свёл все agreed APIs;
добавил Entity overload в существующий RejectedEntitySpawn с прежними exact
snapshot/manager/identity checks, сохранив ItemEntity overload для прежних callers.
Исправлен count RNG на world.random по original AIFish161, не golem probe RNG.
Активен единственный Gradle shell `sh_150cab64a001WF0NeajBO71sSN`,
`.tmp/straw-golem-build.ps1 -RunName golem-fishing-build1`: actual Fabric
main/remap/production isolation. Main frozen, code-авторы не активны;
не запускать второй Gradle/game, ждать уведомления без polling. НЕТ runtime/
gameplay/visual PASS; итоговый игровой проход отложен. Decanting5 prerequisites
могут войти в compile, но core5/transport/stock recall ещё не подключены.
После build продолжать настоящее «Переливание», не smoke infrastructure.

Следующие подробности — история до этого запуска и не активное ожидание автора.

`golem-guard-build1` **PASS42s**, actual Fabric main/remap/production isolation,
9 существующих data JUnit (6 aspects +3 infusion declarations). Private JAR
11038264bytes/SHA256`e50f60f769103945f108b37e6815fa8cc738d3706edbe6b85fe743b21257ed9d`.
Также скомпилированы optional initial-input damage и Fishing loot generator;
это не отдельная runtime-проверка initial matching и не готовая Fishing11.
Game/смоук/NeoForge не запускались; Bore-only экспорт не заменён.

**Текущие авторы Fishing:**
- `ses_eafb10afaffeBegvovzC6owP6z`: EntityGolemBase +новый FishingGolemGoals,
  реальная задача/один cast/сохранность одного оплаченного pending catch.
- `ses_eafb0d3d4ffeYOn4l83eswbUeq`: новый EntityGolemBobber/RenderGolemBobber,
  ModelGolem/RenderGolemBase/ThaumcraftGolemClient; physics/леска/отображаемая
  удочка/independent arm5. Нет настоящей удочки в cargo/equipment.
Client/bobber автор **FINAL**, root прочитал все6 production files и notes:
`analysis/2026-10-19-golem-fishing-client-notes.md`. Type/constructor/model APIs
совпали; raw timer5 не смешан с folded action. Client files теперь frozen.
Root проверил source distinction: rod-display — отдельный слой, не carried
display, поэтому idle Fish не получает invented carry-pose. Visual PASS нет.
Runtime Entity/Goal ещё у автора; root не читает/не меняет WIP и не запускает
Gradle до его FINAL. Потом `.tmp/straw-golem-build.ps1 -RunName golem-fishing-build1`
соберёт actual Fabric main/remap/isolation, без game/smoke/NeoForge.
Decanting5 source-contract **FINAL**, прочитан root:
`analysis/2026-10-19-golem-decanting.md`. Уже есть настоящий typed sink
`TileCrucible.fillWater(amount,doFill)`; нового generic fluid-provider не нужно.
Тот же автор `ses_eafb08ef9ffeJ1a2QPyw291dy9` теперь пишет только новый
`items/golems/GolemWaterCargo.java` +notes: bounded water amount/strict NBT и
reserved CUSTOM_DATA WaterCargo на реальном recall item. Это standalone
prerequisite, пока НЕ подключён к placer/Entity и НЕ готовое ядро5. Старый
ItemPlacer по-прежнему отказывает CUSTOM_DATA. Helper теперь **FINAL**, root
прочитал строгий parser/material capacity/read-write asymmetry: чужие данные
не стираются. Единственный активный code-автор остался Fishing runtime.
Root дополнительно подготовил LIQUID5 data/menu prerequisites, без регистрации
ядра/задачи: только один pristine WATER_BUCKET ghost в slot0, wantedCounts ВСЕ0,
Fill modefalse; прочие slots/data отклоняются, не обнуляются. Menu имеет1 ghost и36 player slots,
серверный ghost click не платит ведром и не меняет quantities. Gui показывает
только1 рамку и water-only explanation. Пока это недоступно без настоящего
runtime/core5; не объявлять рабочим «Переливанием». API/план следующего unit:
`analysis/2026-10-19-golem-decanting-api.md`. После Fishing FINAL сначала compile
текущей группы, затем реальное water-source→cargo→crucible, без тестерной паузы.
Agreed API: `analysis/2026-10-19-golem-fishing-api.md`;
original/contracts — next-core/fishing-loot/bobber notes. Type/API уже зарегистрированы
root: GOLEM_BOBBER .25×.25/noSave/range64/update5 и CORE_FISHING11;
Data5fields EMPTY/0/false controls, coreForType/recall/icon/book/model/lang.
Точный рецепт Harvest center +rod DAMAGE0/cod/pufferfish/salmon,
instability3/Aqua16/Messis16/Bestia16, COREFISHING. No GUI/cargo-routing для11.
Root также написал `GolemHarvestPlatform.mayInteract(golem,pos)` (только native
loaded/world/profile/spawn checks без break/use callbacks) и
`DeferredItemDrops.enqueueGolemFishingCatch(level,water,motion,actualResult)`:
одна existing Pending, positionwater+.5/+1/+.5/delay20, без RNG/flush/callback;
caller raw-clears свой pending сразу после normal return. Unknown addon fishing
claim hooks не заявлены поддержанными. Непроверенный код не экспортируется.

Далее — история предыдущих этапов; старые active/RUNNING/ожидания ниже не
заменяют этот раздел. Gameplay/visual acceptance новых групп остаётся отложенной.

Ниже — история предыдущего распределения Guard. Original-контракт
`analysis/2026-10-19-golem-guard.md` FINAL. Runtime автор
`ses_eafb10afaffeBegvovzC6owP6z` владеет EntityGolemBase +новымGuardGolemGoals,
root не читает/не меняет WIP и не собирает до FINAL.
Root уже добавил Guard4 в Data/Core/registry/recipe/model/lang/book/icons и GUI:
есть настоящее меню без ghost-слотов, caption +locked Order-category explanation.
Базовая policy толькоEnemy/hostile-class, исключены игроки/пассивные/криперы;
retaliation не обходит ограничения. Flags не помещены в Fillmode, schema5fields
не расширена: Guard требуетEMPTY/0/false controls; invisible51–54 edits сервер
не принимает. Точный рецепт — blank +Telum5/Vinculum5, gateCOREGUARD.
Actor/crop platform не применяется к урону; обычный native mob-hurt/cancel pipeline,
без fake-player/owner damage attribution. ItemAxe original-review автор ещё
read-only; Lumber зависит от настоящего axe, не от регистрации placeholder.
ItemAxe contract теперь FINAL (`analysis/2026-10-19-axe-stream.md`); root подтвердил
две обязательные интеграции до его реализации: initial central DAMAGE0 predicate
в InfusionStackRecipe (runtime retention-ignore-wear не менять) и composable
completed-break hook с existing Excavation mixin, без duplicate redirect. Никакой
axe/shared-hook код ещё не пишется. Read-only автор выбирает следующую реально
доступную обычную задачу из оставшихся cores, не подменяя recipe dependencies.
Параллельная code-задача теперь разрешена только для маленького prerequisite:
`ses_eafb0d3d4ffeYOn4l83eswbUeq` владеет InfusionStackRecipe (+если нужно existing
matching/predicate helper) для optional initial `input_damage`, 6/7arg constructors
и старые recipes сохраняются. TileInfusionMatrix/centralStillMatches НЕ менять:
running ritual по original игнорирует wear. Ни axe item, ни Lumber пока не
регистрируются. Guard и этот автор работают в разных файлах; root не читает WIP
и не запускает следующий Gradle, пока ОБА не передадут FINAL.

**Harvest build2 PASS42s**, actual Fabric main/remap/production isolation и6
существующих aspect-data JUnit (0errors/failures/skips). Private JAR11022073bytes/
SHA256`1353e155960af3dadbc1f87b8c02244aea95b90d0adf41e7cfe252e19443ab79`.
Root прочитал завершённый лог/XML и составJAR. Это НЕfarm/gameplay/visual PASS:
игра/смоук/NeoForge не запускались; Bore-only экспорт остаётсяf70b32... .
Предыдущие Harvest production-авторы завершили; Guard4 теперь активен выше.
Read-only Axe of the Stream original-review автор ещё готовит contract;
root не читает его WIP.
Продолжать контент после FINAL, без нового аудитора/смоук-тестера. Далее — история
интеграции и неудачного первого compiler run, не активные инструкции ожидания.

Harvest runtime FINAL прочитан: API совпали с root-platform; finite ripe crops,
поиск/путь/work timer/cracks/action/chaining и no-inventory/no-GUI подключены.
Первая сборка остановилась на compiler error: `MelonBlock` отсутствует. Root
проверил native registration: MELON — ровноBlock.class, исправил только этот
import/type check, сохранив finite item identity. Тесты в run1 не запускались.
Теперь выполняется `.tmp/golem-harvest-build1.ps1 -RunName golem-harvest-build2`,
shell`sh_150a2c6b0001TTDEW3vVWzqnwH`: actual Fabric main/remap/isolation +существующие
6 aspect-data JUnit. НЕТ game/смоука/NeoForge. Не менять main/не запускать другой
Gradle до уведомления; никакой автор production сейчас не меняет. Guard/Axe
source-review авторы read-only и не блокируют эту сборку.
Ниже сохранено предшествующее состояние реализации; новые слова RUNNING выше
относятся только к этому run, не к давно завершённому automation build.

Все code-авторы FINAL; root прочитал runtime Sorting/throws и brain final,
интегрировал Data/Core/recipe/menu0ghosts/client/brain normal-loot hook/queue.
Actual main Fabric `golem-automation-build1` PASS42s/remap/isolation, private JAR
11007976bytes/SHA256`b3dc066f612a47615cef53a4d02c482d612e8487e47feec8742442d94271fb27`.
Игровые/визуальные проверки не выполнялись, Bore-only экспорт не менялся.

Harvest3 runtime автор`ses_eafb10afaffeBegvovzC6owP6z` сейчас владеет только
EntityGolemBase +новые HarvestGolemGoals/GolemCrops. Root не читает его WIP и
не запускает build до FINAL. API: `GolemCrops.ripe(ServerLevel,pos,state)`,
package-private `GolemHarvestPlatform.harvest(golem,pos,state)` (root уже написал),
Entity `hasConfigurationMenu()` только0/1/2/10 и `startActionTimer()`; core3 не
получает GUI/ghost/cargo-routing. Root уже интегрировал Data/Core/recipe/book/model/
lang/menu gate +common/Fabric harvest permission bridge с native/Architectury
callbacks, non-OP unspawnedFakeThaumcraftGolem, mobGriefing/spawn protection и
повторной ripe-state проверкой. Нет OP grant/online-owner borrowing/native saves.
Также добавлен originalWheat direct Messis2/Fames1 (ConfigAspects224), иначе
действительный Messis prerequisite не имел явной привязки. Существующий6-case
aspect-data JUnit обновлён для1 нового row, ещё не запускался. Meto получается
существующим native tool-bonus path; новых фиктивных aspect yields не введено.

Lumber7 source-contract FINAL: требуется настоящий Axe of the Stream, который
ещё не перенесён. Не регистрировать Lumber с fake/substitute ingredient.
Read-only авторы теперь готовят Guard4 и Axe of the Stream contracts, production
не меняют. Это dependency-очередь, не блокер пользователя и не возврат к тестеру.
Следующие записи — история интеграции до текущего Harvest-этапа.

#### История Fill/Empty → Sorting/throws/brain

Все три автора завершили работу. Root согласовал API runtime/data/menu/client,
Bell use-first, разделение initial-selection reach и редактирования ссылок уже
выбранного loaded UUID; `canEditLinks` сохраняет запрет unsupported/retiring state.
Fabric main remap/isolation `golem-inventory-build1` **PASS39s**, private JAR
10996795bytes/SHA256`d27892c805ecc5ae59482ca77a43f1d688a7442de5fea98a4f6304ba5af58f1a`.
Игровые/визуальные/полные проверки отложены. Bore-export не заменён.
Directed world throws FINAL прочитан и интегрирован; root добавил узкий queue-метод
`DeferredItemDrops.enqueueGolemThrow(level,golem,target)`. Bridge API agreed:
`canPublishDirectedThrow(target)` только внутри реального busy/throwTarget guard;
`detachDirectedThrowCargo(target,expected)` проверяет всё ДО raw clear, не делает
callback/sync после clear. Queue готовит Pending/live trajectory, публикует одну
строку и raw-clears cargo без callback/flush между ними; feedback потом. Нового
journal/schema/tracker нет. Root не читает/не правит runtime WIP и не запускает
Gradle до всех FINAL. Bridge теперь реализован, но ещё не компилировался.
Sorting10-контракт теперь FINAL: seeded destinations (реальные extractable seed
items), без ghost-слотов/quantities, только containers, точный infusion recipe
на отдельном zombie brain. Сейчас `ses_eafb08ef9ffeJ1a2QPyw291dy9` делает только
новые ItemZombieBrain/ZombieBrainDrops и notes: настоящее получение+food/warp,
не Entity/Data/Bell/Menu. Root добавил brain case в ThaumcraftContent; зависит
от согласованного нового класса. Другой read-only автор готовит Harvest3 контракт.
Brain-code автор FINAL, root прочитал ItemZombieBrain/ZombieBrainDrops и notes.
Root подключил native normal-loot ModifyArgs hook в common mixins +constructor/
wolf-food tag. Не два loot hooks: ровно один в dropAllDeathLoot на вызове
dropCustomDeathLoot(ServerLevel,DamageSource,boolean), args неизменны. Сборки пока
не запускать: Sorting runtime ещё активен.
**Текущие владельцы:** runtime автор теперь делает Sorting10 в тех же трёх
Entity/InventoryGoals/HomeInventory файлах, сохраняя throws/bridge/canEditLinks;
brain автор пишет только новые brain item/drop classes. Root включил Sorting10
в Data/Core/registry, реальный infusion recipe и aliases/переводы/model. У Sorting
**0 ghost slots** и только36 реальных слотов; opening core передаётся в extended
menu, сервер также не даёт менять отсутствующие filters/mode. Data отказывается
при non-empty Sorting ghosts/wanted/mode вместо молчаливого reset.
Throws+Sorting+brain будут собраны одним следующим Fabric batch после FINAL
авторов. Их implementation не выдавать за PASS предыдущего container-only build.
Исходная физика throws и action6 подключены; клиентские timer feeds уже верны.
Scope tooltips обновлены: только known containers/ordinary non-BE block targets,
не overflow dumping. Полные gameplay/visual проверки остаются отложены.

Далее сохранена последовательная история. Старые «автор ещё активен»/RUNNING
ниже относятся к прошедшим этапам и **не заменяют этот актуальный раздел**.

**Текущая разработка: голодный сундук собран; начат соломенный голем.**
**LATEST CONTENT: Fill0/Empty1 implementation ACTIVE; no Gradle/game.**
Data/Bell и client авторы завершили FINAL; root прочитал их исходники/API.
Root APIсогласованы: GolemData5fields/MarkerDTO, menu8datafields, button50,
Entitymarkers authority, hand-onlyresetC2S и существующийsharedF input. Scope
ограничения отображаются в core/bell tooltips, quantitylabels не реальные стеки.
Только runtimeавтор ещё пишет код. Item/data автор теперь READONLY готовит
короткий source-контракт следующей ветки directed worldthrows в
`analysis/2026-10-19-golem-world-throws.md`, не меняет текущие исходники.
Этот контракт теперь FINAL и прочитан root: spawn на половине высоты голема,
весь реальный cargo, pickupDelay10, исходная НЕнормализованная motionформула
d/3. ExistingPending уже сохраняет все нужные поля и retry/level-postpath.
Следующая ветка не требует нового journal/codecversion/source-auditor; сначала
дождаться runtimeFill/EmptyFINAL и собрать текущуюcontainer-routeгруппу.
Rootпока НЕсобирает/НЕчитаетruntimeWIP. После FINAL сначала actualFabriccompile
container-route группы; все gameplay/FX/key/save checks по-прежнему отложены.
Runtime/AI автор ещё работает, WIPнечитать/Gradleнезапускать. Root добавил два
реальных integration fixes до сборки: Bell.useOn тоже идёт через Fabric
onItemUseFirst, иначе chest GUI перехватывал маркировку; markeredit/reset работают
с уже выбранным loadedUUID без второго entity-reach, как originalBell131–207,
при сохранении initial-selection reach и nativeblock/hand/worldpermissions.
Root также локализовал Precise/Any, добавил явные scope/resettooltips и flush
только nativeRenderType.lines в FabricLAST. Source finals хеши авторов относятся
к состоянию ДО этих перечисленных root-deltas, не к текущим5 изменённым файлам.

Original contract FINAL`analysis/2026-10-19-golem-fill-empty.md`; agreed API
`analysis/2026-10-19-golem-inventory-api.md`. Container routes first; intentional
world-throw branches explicitly DEFERRED (unknown/missing endpoints never dump
cargo). Six one-count ghost exemplars +separatewanted0..256; duplicate-row deficit
SUM/clamp and single logical controls explicitly fix original quantity/overlap bugs.

Owners (не читать/правитьWIP/запускатьGradle до всех FINAL):
* Runtime`ses_eafb10afaffeBegvovzC6owP6z`: Entity/GatherGoals/HomeInventory +new
  inventory-goals; actual Fill/Empty extraction/transport/return/cargo persistence.
* Data/Bell`ses_eafb08ef9ffeJ1a2QPyw291dy9`: GolemData/Core/Bell +newGolemMarker;
  wanted/mode/real entity links, authoritativeclearMarkers(stack,player).
* Client`ses_eafb0d3d4ffeYOn4l83eswbUeq`: GuiGolem/RenderGolemBase/GolemClient;
  quantities/Filltoggle/corebadges/nativeworldmarker outlines.
* Root: ContainerGolem8datafields +safequantityghostclicks/button50, newhand-only
  GolemBellResetPayload/Networking; existing sharedF mapping extended ONLYforbell
  (no duplicatebinding), preserving ordinary hands/wands. Rootregistry/recipes/
  models/lang/book/icons addCORE_FILL/CORE_EMPTY; RootFabricLASThookcallsrenderMarkers.
These root sources depend on agreed in-progress APIs and are NOT yet compiled.
No new smoke/auditor/source-pin framework, no native world or resource fixtures.

**LATEST: семиматериальная группа +2блока ACTUALFABRIC BUILD PASS35s.**
`golem-materials-build1` exit0/remap/production isolation, privateJAR10961197bytes/
SHA256`b7a4d3a1374e599f03e6f97f512802b9512ae19b73cb116edd744e4269d32916`.
Root прочитал завершённый лог и составJAR. Нет game/смоук/NeoForge запуска,
нет утверждения gameplay/visual PASS. ПоставкаBoref70b32... не изменялась.
Всё source-writing завершено, Java/Gradleне активны. Продолжение CONTENT: Fill0/
Empty1; original-only автор ещё готовит краткий контракт marker/source/destination/
ghost-quantity и recipes в`analysis/2026-10-19-golem-fill-empty.md`. Не poll/readWIP;
после FINAL переходить к исходникам cores, не к smoke-тестеру.

**LATEST: семиматериальная группа +Thaumium/Tallow blocks интегрирована,
actual Fabric `golem-materials-build1` RUNNING**, shell
`sh_1506e6523001Y0Ip54NuS6f1Cf`. Все code-authors FINAL, root прочитал финальные
Entity/Enum/API. Поддержаны0/1/2/3/5/6/7, Flesh4не зареган. MaterialTypeName
динамический, nativeCustomName сохраняется, damaged Health не refill при load,
recall не подменяет тип наStraw. Helpers/goals прежние, corpus/NoAI/saves untouched.
Не менять main/не запускать параллельныйGradle/game; дождаться уведомления без
polling. Это actual main compile/remap/isolation, НЕgameplay acceptance/export.
Следом original-only автор читает Fill0/Empty1 в
`analysis/2026-10-19-golem-fill-empty.md`; не читать егоWIP/не трогатьruntime ради
догадокдоFINALконтракта. Никакой работы над smoke-тестером не возобновлено.

**LATEST: Straw/Gather build1 PASS46s; продолжены материалы Wood/Clay/Stone/Iron.**
**Scope update: prerequisiteThaumium/Tallow блоки FINAL+ROOTintegrated, поэтому
добавлены также Tallow2/Thaumium7 в тот же НЕСОБРАННЫЙ batch.** Actual rock blocks,
исходные9↔1 recipes/textures, pickaxe/beacon tags/bootstrap/book/lang готовы в
исходниках. Не путатьtallow_block5 сtaint/fleshblock2; последний НЕ перенесён.
Root обновил materialplacer registry/finiteguard/recipes/skins/icons для
0/1/2/3/5/6/7. ТолькоFlesh4 остаётся недопустимым. Никаких placeholders/newworld
изменений; game validation этих блоков и вариантов не выполнялась.
Runtimeавтор получил scope steering: завершитьEnum/Entity для семи поддержанных
типов, сохранить здоровье/материал в save/recall, dynamicname. Не читатьWIP/не
запускатьGradle до его FINAL. Item/module авторы больше не активны.

Actual Fabric remap/isolation PASS, privateJAR10945193bytes/SHA256
`5e43b5b9040a418684d4814e215e5669c89e81d94969e1f0ab81177cadfad9fa`.
Нет game/smoke run, прежний Bore-export не менялся. Дальше root добавил registry/
recipes/book/icons/models/skins/materialGUI/predicates четырёх обычных вариантов.
`analysis/2026-10-19-golem-materials.md` содержит точные характеристики/границы.

Сейчас ТОЛЬКО runtime-автор`ses_eafb10afaffeBegvovzC6owP6z` завершает изменение
EntityGolemBase +новыйEnumGolemType; root НЕ читает WIP и НЕ запускает Gradle.
Item-author завершил ItemGolemPlacer(Properties,int) для0/1/3/5/6: real material
применяется к fresh entity ДОcore/filters; прежние data/Core/Bell неизменны.
API: setGolemMaterial(int), getGolemMaterial()synced, rootplacerForMaterial(int).
После runtime FINAL проверить импорт/динамическое имя и собрать target заново.
Tallow2/Flesh4/Thaumium7 остаются неподдерживаемыми до блоков-prerequisites,
хотя Enum сохраняет все исходные ordinal. Не выдавать enum за готовый контент.
Параллельно Item-автор`ses_eafb08ef9ffeJ1a2QPyw291dy9` теперь владеет только новым
ThaumcraftGolemMaterials +resource/recipe files дляthaumium_block/tallow_block
и соответствующей короткой notes19.10. Никаких existing Entity/Enum/Placer/
registry/bootstrap/book/lang правок этим автором; root не читает его WIP.
Это настоящие prerequisite-блоки, не разрешение включить2/7 раньше готовности.

**LATEST19.10: три автора завершили; Straw/Gather интегрирован, actual Fabric
build1 RUNNING** shell`sh_150647047001lLR5UXSI30HZLR`, wrapper
`.tmp/straw-golem-build.ps1 -RunName straw-golem-build1`. Main production package/
isolation только; НЕТ game/смоука/NeoForge/fullQA. Ждать уведомления, не poll output
и не менять main-источники/запускать второй Gradle до завершения.
Root согласовал новый importcommon.container.ContainerGolem в entity и дробный
floatbootup с моделью (нельзя обрезать оригинальную33->boot²/33.1 анимацию intcast).
Runtime3 файла/API notes теперь прочитаны FINAL. Оригинальные/<5pickupDelay/carry1/
filters/cargo/menu/recall/persistence реализованы, но игровая проверка отложена.
После результата поправлять только реальные compiler/API errors; не возвращаться
к доработке smoke audit. Следующий контент — дальнейшие материалы/cores големов.

Рабочая группа голема: `analysis/2026-10-18-straw-golem.md`, исходный контракт
`analysis/2026-10-18-straw-golem-original.md`. Root уже добавил registry/menu/
4recipe JSON/itemmodels/recipe+icon aliases/переводы/bootstrap; сейчас они зависят
от ещё незавершённых новых runtime/items/client файлов. **НЕ запускать build до
трёх уведомлений завершения.** Root не читает/не правит их WIP.
* Runtime `ses_eafb10afaffeBegvovzC6owP6z`: только новый EntityGolemBase и локальные
  AI/helpers; carry1, Gather2, native pickup/return/deposit/filter/recall/persistence.
* Items `ses_eafb08ef9ffeJ1a2QPyw291dy9`: только новые GolemData/Placer/Core/Bell.
  Data core−1/2+6 ghost samples, native codecs, semantic full-stack equals/hash;
  bell selection не меняет home Gather. Договорённые API перечислены в заданиях.
  **Завершено:** root прочитал4 окончательных файла/API notes; дополнительные
  методы entity не нужны. Tooltips интегрированы. Codecs/equals сохраняют6ghost
  samples и core−1/2. Bell markers локальны, routing/render/reset-key не заявлены.
  Placer уже подключён к Fabric item-use-first; item/coredata/рецепты не fixtures.
* Client `ses_eafb0d3d4ffeYOn4l83eswbUeq`: модель уже передана; теперь только новые
  RenderGolemBase/ThaumcraftGolemClient/GuiGolem. Root пока НЕ читает эти3 WIP.
  **Завершено:** root прочитал окончательные client/model исходники, согласовал
  единственныйGOLEM_DATA.get() callsite с настоящим RegistrySupplier. МодельAPI
  теперьsetActive/Carrying/Bootup/Healing/Combat/Inactive (ранний draft superseded).
  Визуализация/компиляция ещё не проверены, runtime/items активны. Не запрашивать
  новый клиентский proof/review вместо их завершения.
Без новых тестовых фреймворков/Gradle/game/Python/NeoForge запусков авторами.
Все они завершат краткими API/ограничениями, а не claims полной игровой приёмки.
Только runtime автор ещё активен. Root добавил AW на namedItemEntity.pickupDelayI
для исходногоGather<5 (не playerdelay==0) и пустой native entity loot table; API
steering отправлен. До runtime FINAL сборку не запускать, WIP не читать.

Fabric build1 PASS38s,6 aspect-data JUnit PASS, production isolation PASS.
Private JAR10861935bytes/SHA256
`fe5dbcbecbe1bf70fb659650b361a52ba19ce82dfb9f910895f4c873120e3ea4`.
Никаких game/smoke/NeoForge запусков. Этот JAR не экспортирован, поставка бура
остаётсяf70b32... . Source/model runtime сундука не выдавать за проверенные в игре.
Original-review автор готовит первую gameplay-группу GOLEMSTRAW; client-автор
портирует только новый `ModelGolem<T extends LivingEntity>` с исходными boxes/UV/
ходьбой/позой переноса. Не читать их WIP; текущих Gradle/game запусков нет.

Ниже — состав выполненной интеграции сундука:
Все3 content-автора завершили работу. Подключены регистрация, vanilla27-slot GUI,
контактный сбор/остатки, сохранение, воронки/компаратор, крышка/звук/BER/itemroute,
исходные HUNGRYCHEST/recipe/icon/переводы/loot/axe-tag. Root уточнил shaped-contact
15/16+численныйepsilon и исходную nearby-height/yaw ориентацию. Исходный рецепт:
7planks+oaktrapdoor, Aer5/Ordo3/Perditio3; complex Motus1 trapdoor добавлен без
придуманных direct Hungry Chest аспектов. Детали: `analysis/2026-10-18-hungry-chest.md`.
Завершён Gradle shell `sh_1505580d8001frF3sFJLvPNp3q`: main Fabric package и
существующий узкий aspect-data JUnit, НЕТ game/clientTest/fullQA/NeoForge.
Следом готовится original-only спецификация первой игровой группы соломенного
голема; автор владеет только новым `analysis/2026-10-18-straw-golem-original.md`.
Ни сундук, ни будущие големы не объявлены проверенными в игре/полностью готовыми.

**Экспорт выполнен.** Свежий Fabric production build успешен45s, изоляция тестов
проверена. В `../Thaumcraft-Fabric-1.21.1/mods/` теперь JAR10841529bytes/SHA256
`f70b32f4c1698bf3247afd1bb1525f69cdc341e045ff605d4b3ed6db8196fa8f`.
Все3 дистрибутивных SHA256 проверены; зависимости не менялись. Focal JAR/README/
хеши сохранены в `backups/focal-fabric-validate7/`. `README.md` явно сообщает о
предварительном статусе и отложенной проверке. Запись: `docs/porting/bore-export.json`.
Нового game/smoke прогона не было; native16–26 не переоценены. В пакет вошли2045
точно закреплённых productioninputs; Hungry Chest не включён. Новая разработка
сундука уже меняет рабочие main-исходники и не меняет этот экспортированный JAR.

Пользователь остановил развитие smoke-тестера и затем прямо разрешил экспорт
бура с итоговой проверкой в конце общего переноса. Это **предварительная сборка**,
не принятие native16–26 и не утверждение о полном сохранении ресурсов. Не
возобновлять исправления codec/audit/source-pin цепочки вместо контента.

Фактически compile13 уже PASS19s/966 core checks/37 прежних checks/stop26 scenarios/
Gson3 scenarios;161-resource expectation `3ce66a496ad5e7ca461d27a8d37136c512b9e2efbd1a2bb077ab20b28fd2ac00`.
Это не новый native прогон. Незаконченный codec сохранён без отката; передача
`.tmp/bore-codec-materialization-review1/HANDOFF.md`, дальнейшая работа отменена.

Одноразовый production build `.tmp/bore-content-export1.ps1` выполнен без
игры/смоука/NeoForge: собирает только main-источники из compile13 snapshot через
точные include-списки. Новые незаконченные файлы голодного сундука в экспорт бура
не попали. Старый focal JAR/README/хеши сохранены в отдельный backup,
заменён только мод, зависимости и миры не менялись. Новый пакет маркирован
«финальные проверки отложены пользователем», не «все проверки PASS».

Следующий Fabric-контент уже в работе: **голодный сундук**, оригинальные27 слотов,
контактный подбор, GUI/сохранение/воронки/компаратор, рецепт и анимация крышки.
Runtime/client/original-review авторы работают только в своих новых файлах;
root владеет регистрацией/рецептом/книгой/переводами/интеграцией. Не читать их WIP
и не запускать параллельный Gradle. Полные големы ещё не реализованы.

Старая история ниже сохраняется; её инструкции «ждать codec/freeze/27» отменены
новым приоритетом пользователя. Все неудачные миры остаются сохранены и не inputs.

## Текущий игровой checkpoint16.10.2026

### Focal manipulator принят/экспортирован; начат Arcane Bore, Fabric-first

#### Current Arcane Bore validation — Fabric only; no current export

**LATEST exact Fastutil fix FROZEN +contact event emission corrected; actual
compile13 active, codec author still active; NO27.**
Fastutil producer seal SHA
`f6075ced13a654b78aa60efa0ee43e2ee76f229abe6b1110980533d3eb496b1a`, sourcefreeze
`5a2aa00848ddcc3e62b263ac000a242f2a0fe808269a85f838b15c9f1a199aae`.
ROOT verified allsealfile hashes and4 actual current sources. Author966 actual
detached core checks (all936old) and4 whole-file inverses PASS. Exact sole frame:
canonical nativeManager.method_31842(J)Z <-actual same-loader LongCollection
.removeIf(java.util.function.LongPredicate)Z <-nativeManager.saveAll()V, immediate
adjacency, atmost1 per span; every other library/method/overload/caller remains
forbidden. No schema/ROOT/hook/physical/clock/actor change. Exact added installed
resource `/it/unimi/dsi/fastutil/longs/LongCollection.class` SHA7a63e7e8... from
MC-declared8.5.12 cachedJAR SHAb5543ee0... . Existing3Architectury resources retained.

ROOT changed ONLY one Sequence.pickup emission line: contact-camera fixture goes
to actions (NOT physical ADD/pickup list) with actual owner.server tick/gameTime.
ExistingTP, points, item_writes0, support/noCollision/entities, itemphysics, input,
ops and deadlines untouched. Root2 exact inverse tests PASS. Exact priorbd472...
Sequence archived at `.tmp/bore-contact-action-schema-review1/`;6input freeze SHA
`7959563701b65c3f686e9f0ffa50e4e7aa46e5c5efc7c3d5094139410796b5bc`.
Physical parser MUST remain ADD/TOUCH-only; reader must separately join real timed
contact actions, not normalize old26invalid physical rows into valid events.

Fresh3967 compile13 source inputs/tool hashes frozen. Guarded actualFabric shell
`sh_15043897c001fM6xd9UPDqcJr7` active; no polling/otherJavaGradle/game/source edits.
New passive inspector `.tmp/inspect_bore_fastutil_compiled_binding.py` prepared
for exact4externalresources/twocachedJARpins, not yet executed. Old tools retained.
Codec-only independent author still owns Versions/COW/materialization task; do
not read/poll WIP or edit Python. After its freeze, root must request combined
Fastutil/contact/codec source-chain+truth review and real failed26 lossless decode
diagnostic BEFOREanother native attempt, never accepting failed26 in the process.

**LATEST actual failed26 pre-stop diagnostic returned TWO additional concrete
pipeline blockers; fix BEFORE another long game, NO27.** All18 diagnostic output
manifest hashes ROOT verified in `.tmp/bore-native26-prestop-diagnostic/`.
No worlds/source/evidence modified. All143 archive/static inputs hash exact
before/after. Worker exited; no resident report process remains. No PASS/normal
tail reconstructed; Native26 still failed and never a retry input.

1. Source Sequence954 appends `supported PLAYER contact camera fixture ONLY` to
   native_physical_UUID_events. Actual26 has10 such rows amid13ADD/13pickup; rows
   lacktick and verify_bore requires onlyADD/pickup. Merely addingtick won't fix
   unknownschema. Root will move ONLY this fixture emission to actions and add
   real server tick/gameTime, leaving all actual TP/geometry/noitemwrites/ops and
   strict physical ADD/pickup parser untouched. No root source patch yet while
   Fastutil author finishes its frozen source review; retain exact beforeimage.
2. Exact old Versions global resolver fails at node10000001 in normal POST6770/
   tick6841, not teardown.64818 versions/2786652initial cache-node visits; global
   all-report eager cloning/counting plain literal data reaches10M before end.
   Whole-report decoding is FAILED, not accepted by bounded subtree diagnostics.

Reader author`ses_eb1a06451ffe3O2TeIuVeDbBPz` now owns ONLY codec Versions class +
NEWtest_bore_codec_materialization_audit.py +NEW`.tmp/bore-codec-materialization-review1/`:
design lossless copy-on-write reuse of already-parsed literals, validate EVERY
node/ref/type/cycle/height/failedflag, materialize only changed reference paths.
10M remains explicit GLOBAL newly allocated materialization/slots budget, NOT a
claim that the old10M literal-visit accounting is unchanged. This accounting
revision must be disclosed/reviewed, never a blind cap raise/reset/chunk bypass.
512MiB raw byte/250kversions/128height limits stay strict. Do not read its WIP or
edit codec/main/Python; source-gate integration waits final producer+codec freezes.
No true raw/native report is normalized or reclassified to bypass failed stop.

Useful actual26 pre-stop facts ONLY:12268contiguousPOST72..12339; exact160-resource
catalog fingerprint91eb... matchescompile12;43 actual serialized observations/
certificates validate standard Architectury wrapper representation with canonical
native delegate and sameKnotloader. This is first actual26 proof, NEVERnative25
identity. Original7/prestop20events +12268snapshots and custody milestones returned
(departure108/pickup110/readiness118/stow356/slot7). Three local ambient birth/death
triplet fragments returned; complete geometry/trajectory/cohort/stopped/stock/
13acquisition/300-112-188 closure remains UNVERIFIED. No finalization/unknownfailed
unloadHEAD invented; no scope promotion from these partial direct calls.

Fastutil exact-frame author remains ACTIVE separately. Await both SOURCE_FROZEN
notifications, no polling/WIP reads/new JavaGradle/game. Then root contact action
emission patch, combined independent truth/source pins and fresh actual compile
BEFORE27. User was told these actual-report defects are being fixed in advance.

**LATEST native26 REJECTED only after story/full compact report: exact Fastutil
native saveAll frame rejected by QA nativeOnly.**
Run`client-smoke-e6feaa74-f0e3-4e45-ac35-5064925bfc06`, retained world
`bore-smoke-3b5f2003-369e-4b96-a1ba-b717ba2c8e72`, storySCRIPT_PASSED/tick12338/
note1/PNG10. New direct/standard-wrapper support allowed startup and whole story;
no full callback/stock/stop acceptance yet. Real FIRST_FAILURE at stop tick12339
is `Foreign native lifecycle caller it.unimi.dsi.fastutil.longs.LongCollection`:
nativeManager.processChunkUnload ->method_31842 ->LongCollection.removeIf ->
nativeManager.saveAll ->ServerLevel.save ->MinecraftServer.saveAllChunks/stopServer.
This is a concrete observed library frame, NOT a guessed failed conjunct.

Full report now really fits unchanged512MiB:447883116 bytes, SHA
`6102114f12c327f61a9cf9460ee70416a5ca6b8ab6070c73b4d93344017a9ec7`.
Both actual primary/proof closes true, full_evidence=true BUT status
FAILED_OR_INCOMPLETE: first root failure retained, finalizerTHREW, server4cleanups
returned. This is a complete encoded FAILED report, NOT normal completed ledger/
stop/conservation proof. Cannot reconstruct missing finalization/promote/reuse26.

ROOT confirmed no game/Gradle writers, copied/hash-verified full447MB primary,
proof/logs/result/copy/input/template/resources/source bytes/all10 exactPNGs to
`.tmp/bore-client26-rejected-fastutil-spine/`. All3967inputs/2525auditfiles/accepted191
hashes exact afterstop BEFORE source edits. ROOT individually opened all10 exact
174–183PNG and bound copies via exact-PNG-visual-review.json; no bounded visual
blocker identified. Pixels are not resource/UUID/timing proof. No export/finalPASS.

Two active independent tasks, no new game/Gradle or root source/Python edits:
* Author`ses_eb04d3d61ffeL9yx7XVOgPHvNt` owns only adapter/Core/CoreChecks/Binding
  andNEW`.tmp/bore-fastutil-save-spine-review1/`: prove exact actual Fastutil
  removeIf descriptor/dependency bytes/native saveAll lambda call; add ONLY exact
  nativeClass/loader/frame adjacency method_31842->removeIf->saveAll, atmost1 frame
  per span. No library namespace/othermethod/caller/loader/body/source waiver.
  Add exactONEexternal LongCollection.class installed digest, retaining existing
  exact3Architectury resources/ROOTS21/hooks22. PureNEWJava core tests allowed,
  noGradle/game/world/Python edits. Await source freeze; don't read/poll WIP.
* Reader diagnostic`ses_eafc6a689ffe8NSCnpiZanGcKK` owns only
  NEW`.tmp/bore-native26-prestop-diagnostic/`, reading archived failed447MB report
  in ONEprocess +archived source/readers, not current producer WIP. It is checking
  actual installed/shape/prestop receipt/schema facts for OTHER concrete blockers
  before another long game; NO failed-tail normalization/fullacceptance/unknown
  finalization invention. Root must not concurrently parse the full447MB report.
Wait both notifications; no27 until narrow fix/review/fresh actual compilation.

**LATEST native26 LAUNCHED; await notification, NOT acceptance.** Independent
callback355 FINAL freeze SHA
`aa3878002a5143cdeb008772e2184e8f5846fa8e1889bd5a2c0fd2e595e3c514` received.
ROOT verified all355 entries; original522+199 additive and authenticated historical
source-stage contracts PASS. No concrete producer defect identified. This advances
only exact prospective callback/source/resource support, not native25's unknown
raw identity or any rejected world/history. Native16–25 remain rejected neverinputs.

NEW26 source snapshot3967 AGAIN equals actual compile12; all1130 class outputs
match160-resource expectation`91eb68b01b44f118bd72cf72ed61a16d267adf58facfe06c1f46a0d4ffd91494`.
Exact3 mapped Architectury resources and cachedJARhash still exact; actual installed
bytes/class/loader/raw+delegate/field relations remain future native obligations.
NEW source-only template2525 actual files passed ACTUAL strict source_snapshot,
including152 exact current callback/diagnostic/lifecycle source/metadata pins and
all historical stages. Parent191 exact payload paths/hashes AGAIN before/after.
NEW26 tools/prelaunch receipts; no source/clock/metadata/cap/stock/loader waiver.

Shell`sh_1502d0972001UvCG3BhZ2w0CRy` active via usual guardedwrapper. NO polling/
runtime log/result/tail/world reads, source/Python edits or otherGradle/game. User
told not to touch controls/window. SameONE200native/14wall custody exception only.
Full normal tail/companion/completeconsole/installedcallback/PNGs/stoppedall7/11/113
and complete real acquisition/NEW300-112-188 proofs still required. No PASS/export.

**Latest actual compile12 PASS, NO26; independent exact-wrapper reader active.**
Actual Fabric clientTest exit0/BUILD SUCCESSFUL19s;936 actual lifecycle/core exact
callback checks, prior37 Java checks, stop26scenarios/87693assertions, realGson3/
52assertions and all22 required observers PASS. Root verified all3967 input hashes
unchanged afterward. No Minecraft/game/world execution. Exact compile12 raw/UTF8/
exit/input/tool/result receipts retained. New compiled expectation independently
read actual160 resources/1130 available compiledclasses, including3 exact cached
mapped Architectury resources with JARhash before/after exact; fingerprint
`91eb68b01b44f118bd72cf72ed61a16d267adf58facfe06c1f46a0d4ffd91494`.
This is NOT native installed/shape/trajectory proof. No Java/Gradle/game remains;
await final independent wrapper source/truth pins without WIP reads/polling or
root Python edits before fresh accepted-parent26. Runtime source/class/metadata
and all normal custody/conservation/stop gates still mandatory.

**LATEST exact Architectury callback bridge FROZEN; actual compile12 RUNNING,
independent source/reader review active, NO26.**
`.tmp/bore-architectury-callback-review1/FROZEN.json` SHA
`5b83bc7b47095cc9d7a14d914df1d347fdae2d541f6ae5f75bc46582a55ea534`, sourcefreeze
`faba8780737fc9525037dfe400b1cce56374f2b5cc4d2bb32d1fc9bb705cbfd8`.
ROOT verified allsealfile hashes +exact4 active sources. Author936 actual pure-core
checks and56 inverse/source contracts PASS; MC/runtime UNVERIFIED. Only adapter/
Core/CoreChecks/NativeBinding changed. Seqbd472/Inherited27127/Ledger0696/manifest22
remain byte-identical. No actual25 callback identity/class is retrospectively proved.

The finite forms are exact DIRECTnative, ONEstandard Architectury13 wrapper with
exact3declared fields/types/modifiers, and completion-only actual nativeNULL. Both
raw+canonical delegate references/shape/captured item are pinned independently per
generation/operation. Full canonical manager/entity/section/knownUUID/typed physical
proof stays strict. Unknown classes still fail with bounded actual metadata. Only
the exact wrapper onMove/onRemove adjacent to actual native callback and Entity
caller is allowed in native spans; no namespace/loader/interface/recursive waiver.

Lifecycle receipt adds exact callback_representation schema/raw+canonical+loader/
field metadata, maintaining source/native/body/role/clock obligations. Binding21
internalROOTS/22hooks unchanged, new actual adapter/core nests must be included.
Binding resource catalog now adds exactly wrapper/factory/setter-mixin bytes from
actual installed loader, never ordinary mixinClass load or callback/factory call.
Mapped dependency metadata at cached-mapped-architectury-resources.json: jar
SHA0e0c79b4...; exact3mapped class digests b306caaf.../fd776e48.../5f493496... .
Those are not upstream or loaded transformation dumps. Future actual resources
AND actual native representation/pair evidence must independently match.

Root NEW `.tmp/inspect_bore_architectury_compiled_binding.py` will extend passive
compiled expectation with exact3cached mapped class entries, checking source
literal/entryhashes/JARbeforeafter; not yet executed. Prior inspector unchanged.
Fresh3967 compile12 inputs/tool hashes frozen; guarded shell
`sh_1501e0430001FFPgVZLWd6STJ8` running actualFabric checks. Await notification, no
polling/otherJavaGradle/game/source edits. Independent reader owns Python-only
exactwrapper replay/source-preimage chain/external3binding gate, not game evidence.
No WIP reads/Python edits by root or26 before finalreview+actualcompile/source pins.

**LATEST standard Architectury wrapper statically PROVED; exact prospective
bridge implementation ACTIVE, no26.** Callback diagnosis REVIEW SHA
`076d89d187ce836b1a3b37a1a533a1103d7e3072db13896808e9862f609c199e`, ROOT
verified all27 manifest rows/11 fresh captures. Architectury13.0.11 MixinEntity
wraps each nonnull/non-NULL setLevelCallback argument in exact
`dev.architectury.hooks.level.entity.fabric.EntityHooksImpl$1`, final captured
`val$entity` and `val$callback`; actual native NULL remains unchanged. Existing
direct-native-only class predicate cannot support that normal representation.
Actual25 rawclass/loader/delegate was NOT serialized: never assert its observed
identity or promote25. Logged FabricAPI is0.116.17/attachments1.4.7/entityevents1.8.0,
not the older versions in an earlier task prompt; exact inspected pins retained.

Root chose PROSPECTIVE exact dependency-proved bridge +unknown-class diagnostics,
rather than a separate unchanged failing game solely to print classname. This
deviates from review's suggested diagnostic-first sequence, NOT its identity/
evidence requirements: future acceptance still requires actual raw/delegate/
loader/field/schema/installed bytes and native pair evidence, not a static guess.
No arbitrary callback/interface/prefix/loader/null or recursive wrapper allowance.

Author`ses_eb04d3d61ffeL9yx7XVOgPHvNt` owns only MCadapter/core/CoreChecks and
optionalNativeBinding +NEW`.tmp/bore-architectury-callback-review1/`. Root will
NOT edit/read WIP, runGradle/game or poll author. Exact supported direct native
or ONE standard Architectury shape must preserve BOTH raw and delegate object
identity across generation/operations, exact captured entity and canonical native
manager/section/knownUUID/full physical ownership; raw NULL only at completion.
No wrapper callback/factory invocation or mutable lastSectionKey authority.

Required frame bridge limited to exact standard wrapper onMove/onRemove method/
descriptor/loader and actual native delegate/caller adjacency, never all mod
frames. Bounded unknown-class/loader/field-name diagnostic must fail honestly.
Finite external wrapper/factory/MixinEntity resources should join actual installed
binding without ordinarily loading mixin classes; root later inspector must use
actual cached remapped resource bytes, not assume original dependency hash equals
runtime. Prior21 internalROOTS/22observers/general guards/clocks unchanged.

Author may run only new pure-core detached Java tests into ignored review area;
noGradle/game/world/projectoutputs/Python edits. Independent reader/source-stage
review and fresh actual Fabric compilation still required after final freeze.
No newgame/export, failed16–25 retained neverinputs, no restoration or resource
credits/actor/stock exception. Await final implementation notification.

**LATEST native25 REJECTED at ENTRY; diagnostic captured real first cause.**
Run`client-smoke-0dc92494-451d-41b7-a822-c0ac6a901c80`, world
`bore-smoke-25de58de-06fb-493c-82f3-2f4ddcf3e5bf`, note0/PNG0/no fulltail.
Actual FIRST_FAILURE at callbacks2/boundfalse, fingerprintfb96...: new lifecycle
adapter readSample122 rejected `Exact native manager callback class or actual NULL
singleton`, from its constructor via inherited observe/checkpoint/native startup.
Generic heartbeat wrapper now retains original cause and console logs actualstack.
Actual callback classname/null/loader Boolean not individually serialized, so do
not assert a specific Fabric wrapper yet. This is a new test-adapter startup
regression; earlier story completion was never full final audit acceptance.

No game/Gradle writers. Exact archive `.tmp/bore-client25-rejected-callback-class/`
retains all logs/result/copy/input/tools/template/compiled expectation/source
bytes. Rootverified3967 sourceinputs/2479 auditfiles and accepted191 exact after
stop. Failed25 NEVERretryinput; no source edit/unloadguard waiver/nextlaunch yet.

Read-only native/dependency reviewer`ses_eb11e1d53ffetuAdEbCzt7BDq2` owns NEW
`.tmp/bore-native25-callback-wrapper-diagnosis/`: identify exactly supported native/
Fabric callback wrapper/delegate from pinned cached dependency bytecode, without
game/Gradle/world/projectcompiledoutput/source edits. Canonical actual manager/
entity/delegate/section/knownUUID identity may NOT become a prefix/interface/ORnull
waiver. If no concrete wrapper can be proved, actual-class diagnostics needed,
not blind guard removal. Await completion, no WIP polling/reads. Native25 cannot
prove24's lost original cause or promote any earlier rejected attempt.

**LATEST native25 diagnostic fresh-parent attempt LAUNCHED; await notification,
NOT acceptance.** Independent diagnostic307 FINAL freeze SHA
`ef4948c88e6f9d4daca11c3ff4d1e0a387843d4b4a27a1baafabd5d7b7b779c2` verified
by ROOT all307 entries; original522+182 additive and compact2+unchanged lifecycle100
through authenticated pre-diagnostic stages PASS. No diagnostic defect/guard change
identified. Actual native24 cause still unknown;25 should retain it if reproduced,
not restore/promote24 or promise a PASS. Runtime truth gates remain unchanged.

Actual compile11/22hooks/394core checks passed; NEW25 source snapshot3967 AGAIN
equals compile11, all1127 class outputs match154-resource fingerprint
`fb96bbe60d04ae8542e50a4c04f2defd0e283cd9e1132e9cf1a47816ab1cd565`.
NEW source-only template2479 actual files passed ACTUAL strict source_snapshot
before launch;110 exact current diagnostic/lifecycle pins and full old source
stages consumed. Installed native closure remains a future obligation. Parent191
path/hash map AGAIN exact before/after; NEW25tools/prelaunch. Failed16–24 excluded.

Shell`sh_14fffa5a2001q8mbz5bOtcSAkj` guarded launch active. NO polling/runtime log/
result/tail/world reads/source/Python edits or otherGradle/game. User told not to
touch Minecraft. SameONE200native/14wall jar excursion, no phase/clock/stock/cap/
metadata/source/geometry waiver. No story/fulltail/PNG/audit/final/export PASS yet.

**Latest actual compile11 PASS, NO25; independent diagnostic pin still active.**
Actual Fabric clientTest exit0/BUILD SUCCESSFUL19s; all22 required observers,
394 actual lifecycle-core checks, prior37 Java checks, stop26scenarios/87693
assertions and realGson3scenarios/52assertions PASS. All3967 source inputs verified
unchanged afterward. `.tmp/bore-custody-integration-check11*` binds logs/result/
source inputs and154-resource/1127-class compiled expectation
`fb96bbe60d04ae8542e50a4c04f2defd0e283cd9e1132e9cf1a47816ab1cd565`.
Not native evidence. No game/Gradle remains. Root waits final independent narrow
source pin, without WIP polling/reads/Python edits, before fresh accepted-parent25.

**LATEST diagnostic-only source correction FROZEN; compile11 active, NO25.**
Static startup review found NO proven raw field/type/callback-order/constructor
or JDK getDescriptor defect; `.tmp/bore-native24-startup-static-review/` retains
source/text evidence. Original native24 cause remains unknown; do not infer it.

ROOT changed ONLY two diagnostic regions: Sequence heartbeat retains actual
failure() Throwable as the cause of the same generic fatal wrapper; inherited
first latch keeps failure=error BEFORE printing its real FIRST_FAILURE stack,
binding/callback count/bound flag even without a failureSink. Logging failure is
contained without replacing/clearing/retrying the first failure. All live/native/
physical/metadata/lifecycle/counter/cap/clock checks remain byte-identical. No
receipt/schema/binding roots change. Exact olda38a.../c953... preimages archived;
root3 exact inverse checks PASS (source-only, not native reproduction).

`.tmp/bore-startup-failure-diagnostics-review1/SOURCE_FROZEN.sha256` has11inputs,
SHA`d28e2f9b52973d6ccd710a36744a3c5dce5d231bf2d593318757fa1e4aefd1d2`.
Fresh3967 compile11 snapshot/tool hashes frozen; guarded actual Fabric compile11
shell`sh_14ff701990013x1isZV8WfMPwM` active. Await completion, no polling/other
Gradle/game or source edits. Independent reader owns narrow Python source-pin
chain update for ONLY2 diagnostic supersessions +small drift tests, preserving
all522+177 runtime truths. No WIP reads or root Python edits. Actual25 requires
both fresh compile/currentresource proof and final diagnostic source freeze; its
purpose is obtaining the actual first cause, NOT a promised pass/old24 restoration.

**LATEST native24 REJECTED at ENTRY, no note/raw/acquisition/PNG/fulltail.**
Run`client-smoke-c01ca48e-f86d-42c2-bedc-7998b1640e2d`; retained world
`bore-smoke-ed2412ca-30d7-49ba-9686-077392b5e5d0`; exit1/31s, native entrytick-1,
serverHeart0, clientHeart1. NativeServerTick.require only emitted generic
`Inherited item witness startup/tick failure` and discarded the original helper
Throwable before baseline/player bind. First actual cause/UUID/failed predicate is
NOT persisted. Do not guess a field/mapping/guard cause or call this stock loss.
No baseline stopHook existed, hence no full tail; failed24 NEVER a retry input.

ROOT confirmed no Gradle/game writers, archived exact logs/progress/copy/input/
prelaunch/compiled expectation and client-test/Python sources under
`.tmp/bore-client24-rejected-entry-observer/`. All3967 inputs/all2468 audit source
files and immutable191 parent hashes exact after stop. No native PASS or export.

Root is preparing a DIAGNOSTIC-ONLY correction: preserve the actual original
Throwable as cause in nativeServerTick and log the FIRST helper latch even before
failureSink bind. All startup/authority/physical/cap predicates must remain strict;
no blind unchanged retry. Exact current Sequencea38a... and Inheritedc953... bytes
archived `.tmp/bore-startup-failure-diagnostics-review1/`; NOsource edits yet.

Original lifecycle author`ses_eb04d3d61ffeL9yx7XVOgPHvNt` owns NEW
`.tmp/bore-native24-startup-static-review/`, READ-ONLY targeted source/capture check
for concrete adapter field/type/order defects. It may read failed24 archive, not
worlds/compiledoutputs; no source/reader edits/Java/Gradle/game. Await notification
without WIP polling. Root will not alter source while that read-only review binds
its preimages. No25 before concrete diagnosis/logging, exact source pin and fresh
actual compilation; old retained24 cannot be reused or promoted.

**LATEST native24 LAUNCHED; await completion, NOT acceptance.** Independent
lifecycle/compact FINAL294 freeze SHA
`408fc0eb8b6a84c860128417c25611e40359f404ec2e24089e5ac0cff350c9a7` received;
ROOT verified all294 entries. ROOT reran original522 +177 additive tests, all
historical8/9/10/8/5/13 contracts through authenticated stages PASS. Current compact2
inverse already passed. No concrete producer correction identified. Reviewer did
NOT inspect/claim compile results; ROOT's actual compile10 (not failed config9)
passed, all3967 inputs stayed unchanged and actual394 lifecycle checks executed.

NEW24 snapshot3967 AGAIN equals actual compile10;1127 actual class outputs match
154-resource expectation`b1619035d146eb61746910bbe71d928c70ee2f7657b28bd4cab950f050c8ae5a`.
NEW source-only audit template2468 actual files passed ACTUAL strictsource_snapshot
BEFORE launch including100 lifecycle/compact source/metadata/static-proof pins
and every old source preimage chain. Runtime installed closure remains a future
obligation. Accepted191 path/hash map AGAIN exact before/after. NEW tools/prelaunch
receipts; no failed world or earned output used as input. No cap/truth waiver.

Native24 shell`sh_14ff06acd001lqBoFREZXrIQKY` via guarded wrapper is active. NO
polling/runtimeartifact/world reads/source or Python edits/otherGradle/game. User
told not to touch controls/window. Only same ONE bounded200native/14wall custody
excursion; all actor/item-manager/typed-stock/source/body/clocks unchanged. Genuine
normal full compact envelope must still fit512MiB and pass primary/proof/complete
console +independent conservation gates. No PNG/fulltail/audit/final/export PASS.

**Latest actual compile10 PASS, NO24; independent lifecycle reader still active.**
Actual Fabric clientTest exit0/BUILD SUCCESSFUL20s;22 required observer classes,
394 actual lifecycle production-core checks, prior37 Java checks, real helper26
scenarios/87693 assertions and realGson3 scenarios/52 assertions PASS. Fault traces
are expected synthetic fault injections, not native errors. Root verified all3967
current source inputs unchanged after compilation. No game/world was executed.
`.tmp/bore-custody-integration-check10*` retains raw/UTF8/exit/input/verified result
and new compiled expectation:154 actual resources/1127 available class outputs,
fingerprint`b1619035d146eb61746910bbe71d928c70ee2f7657b28bd4cab950f050c8ae5a`.
Actual installed runtime closure remains unverified. Do not launch24 before the
independent reader final lifecycle/compact source+truth freeze and fresh parent/
compiled/source prelaunch checks. No polling/WIP reads/Python edits by root.

**LATEST lifecycle source FROZEN +compact applied; actual compile10 RUNNING,
independent reader active; NO24.** Author completed
`.tmp/bore-item-lifecycle-review1/`:394 actual detached Java core checks,37 static
source inverses and14 pinned native capture checks PASS; MC/runtime unverified.
Root authenticated FROZEN seal/all listed file hashes and all9 changed/new source
rows BEFORE edits. Exact inherited/ambient canonical manager custody, paired
tracking/unload completion, fixed7-before-baseline and22-required-observer binding
are implemented; no stock/drop/live-body authority is introduced. API/sourcepatch
and exact5 old source preimages retained. Native23 remains rejected, no replay.

Root then removed ONLY `.setPrettyPrinting()` from actual stop full-envelope Gson
encoder, after archiving exact author Sequencec922fad3...; currenta38a89ba... .
`.tmp/bore-compact-tail-review1/` two exact inverse tests PASS;13input freeze SHA
`5d5a071c9b0646de04a82ccee3b722dc419a6acaef7b55b6aaf6da3cb134a66b`.
No data/event/schema/cap/helper/fallback/proof/clock changes or actual fit claim.

Fresh3967 source inputs frozen for actual compile9, but its NEW private init
script had a Groovy `dependsOn [list]` parsing error BEFORE Java compilation.
Exact failed9 tools/logs/inputs archived `.tmp/bore-custody-check9-rejected-tool-syntax/`;
all3967 source inputs unchanged. Root fixed ONLY ignored init call parentheses,
created freshcheck10 inputs equal9 and newtoolhashes. No Java failure/pass was
inferred from9. NEW compile tools `.tmp/bore-custody-lifecycle-integration-check.*`
now run actual Fabric clientTest/22-observer verification, actual lifecycle core
checks and prior stop/Gson/custody checks into NEW ignored sandboxes.

Compile10 shell`sh_14fcc43b7001r8X09ocuuW4Rug` active; await notification, no polling/
otherGradle/game/source edits. New passive compiled inspector
`.tmp/inspect_bore_lifecycle_compiled_binding.py` prepared for exact21ROOTS/22hooks,
not yet executed. Historical21-observer inspector/tools remain unchanged.

Independent reader`ses_eb1a06451ffe3O2TeIuVeDbBPz` owns Python-only clinical source
review +strict lifecycle/actor/typed stock replay and exact compact/lifecycle ->
stop/flesh ->prior historical source chains. It must not waive general live guards,
openframes/metadata/callback/reason/age/no-forceload/current-owner authority or
original522+158 tests; no permission to promote23 or infer its lost fullreport.
Root must NOT read/poll WIP or edit Python. Wait final review and actual compilation
plus fresh compiled/source/parent checks before any fresh accepted-parent24 launch.

**LATEST native unload-order review received; strict paired lifecycle fix ACTIVE,
NO24.** `.tmp/bore-native23-unload-diagnosis/REVIEW.md` SHA
`e2c48239725730e87388b0510d32aaf19c917a4fb7b1711cde6cc5a6f4245626`.
ROOT verified all25 manifest entries, including14 fresh pinned native captures.
Important correction: unloadEntity does NOT first untrack; it calls setRemoved
UNLOADED atBCI4, NULLcallback13, RETURN18. Visibility retirement can already have
called stopTracking (visibleEntityStorage removal15) earlier, leaving an alive
hidden but manager-registered object. Moving live UUID guard to unloadHEAD alone
is insufficient. Native23 compound booleans remain unserialized; no individual
subject/cause is retrospectively fabricated, and failed23 cannot be promoted.

Implementation author`ses_eb04d3d61ffeL9yx7XVOgPHvNt` owns ONLY existing private
Sequence/Ledger/inherited-helper/NativeBinding/required mixin manifest, NEW private
lifecycle helpers/checks/mixin andNEW`.tmp/bore-item-lifecycle-review1/`. Root MUST
NOT edit/read those WIP files, apply compact patch, run Gradle/game, or ask/poll
status. Await SOURCE_FROZEN completion. Python auditor remains unchanged/root-owned
independent later review, no agent edits there now.

Requested strict design: required paired native stopTrackingHEAD/RETURN AND
unloadEntityHEAD/RETURN, canonical actual manager/callback/entity/section identity,
knownUUID and original live-owner generation certificates. setRemovedHEAD is an
inner reason observation, not completed unload. Hidden path needs real previous
tracking pair, never generic lookup-null/isStopped permission; outer RETURN must
prove actual same-object removal/reason/NULLcallback/knownUUIDabsence/section exit
and unchanged full physical state before unloaded/rebind authority. Initially
unseen/hidden late baselines, replacements/missingRETURN/throws/cancel/pending
frames and unobserved ticks fail closed. Completed tracking-retired coverage is
explicit manager-custody unavailable/audit-required, never active public-owner
body/source/POST authority. General live/touch/source guards stay strict.

Actual disconnected actor flags may appear only in separately proved native
stop-lifecycle context; no copied/flipped actor observations. Fixed inherited7
and already-proven ambient owners only; no birth/stock/credit/material/radius/
clock permission. Generic cow/mobile/acquisition protections remain independent.
Author may run ONLY NEW pure detached Java core tests into ignored sandbox, no
MCstubs/Gradle/game/world reads. Root must still review final exact sources/API,
apply separately reviewed one-literal compact encoding, independently source-pin
all supersessions, run fresh actual Fabric compilation/installed binding, and only
then consider a fresh-parent24. Existing bound512MiB and all normal gates unchanged.

**Latest volume review SOURCE_FROZEN received; compact change NOT APPLIED.**
`.tmp/bore-native23-volume-diagnosis/notes/SOURCE_FROZEN.md` plus planned-change/
RECEIPT/ARTIFACTS retain pinned cached Gson2.10.1/JDK21 evidence. ROOT verified all74
owned artifact hashes. Actual isolated Java experiment90090 assertions PASS:
ordered JSON tokens/Unicode/SNBT/records/enums/evaluation/nonfinite behavior exact,
same real helper512MiB inclusiveLF boundary still strict, failed overflow remains
diagnostic-only. These experiments are NOT native23 data or a size/fit estimate.

Reviewed minimal plan removes ONLY `.setPrettyPrinting()` from Sequence's actual
full-envelope encoder. Current source preimage`e92bb923...`, planned one-literal
postimage`c1b9b80e...`. No values/events/actors/fields/schema/caps/helpers/fallback/
proof/console change. Compact bytes are nonincreasing for reviewed default-adapter
graphs, not universally strictly less or guaranteed within512MiB. No truncation,
pruning, dedup redesign, cap increase or failed native23 report reconstruction.
Root has not edited that source while the separate native-order reviewer still
owns its frozen current-source observation. Await its completion WITHOUT polling/
WIP reads; then apply/freeze narrowly reviewed combined corrections, independent
source-pin review and fresh actual Fabric compile before any24 launch.

**Latest native23 independent endpoint diagnostic completed, NOT acceptance.**
`.tmp/bore-client23-endpoint-diagnostic-independent/` all24 output-manifest rows
ROOT verified from the manifest's own directory. Parent191 exact before/after;
both stopped saves/read sources and all prior22 diagnostic files unchanged.
All113 workshop BEs retained (105whole typed exact/8actual changes), all11 cows
WHOLE typed tags exact. Six old loose WHOLE typed tags including Age unchanged;
OLD8 exact chest7/absent loose. Old20/333 exact; separateNEW11 total188. All554
inherited BEs examined:19tag and15 outside-workshop blockstate changes individually
recorded; node aura is actual stock, not an ignored timestamp. Unrelated container
inventories exact; furnace fuel/display and declared acquisition contexts separate.
Pickdamage2/focus/payer900Aer900Ordo/XP/protection endpoints recorded, not chronology.
Three NEW loose UUIDs (two flesh2, one bone2) remain UNPROVEN; no ambient admission
or full native custody/debit/research/stop acceptance. Failed23 never retry input.
Remaining two native-order and lossless-formatting diagnoses still active; no
source edit/new game/Gradle or broader owner/cap waiver performed.

**LATEST native23 REJECTED: story/10PNG completed but stop ownership + report
volume failed.** Run`client-smoke-9bc8983e-8ce3-461b-a5d8-c3c4311ce228`, retained
world`bore-smoke-773d8fa3-178c-4cdf-9d08-10bb42647f8b`, tick12226, note1/PNG10,
wrapper exit1. New failure-safe writer WORKED: actual primary and companion closed,
Result `FAILED_DIAGNOSTIC_ONLY` with no full report. No stop/conservation/native
acceptance, and NEVER a retry input. Native16–22 remain rejected too.

Actual first latched root exception stack reaches Ledger.ambientItemScope649 via
ambientItemRemoval680 -> Entity.setRemovedHEAD -> PersistentEntitySectionManager.
unloadEntity333/storeChunkSections/processChunkUnload/saveAll ->ServerLevel.save
->MinecraftServer.saveAllChunks/stopServer620. Separate inherited observer failed
State.observe252 (`level.getEntity(uuid)==item &&item.level()==level`) from removed335
on that same native save/unload spine. Actual native normalStopTeardown HEAD
observer DID return. Neither live-owner compound serialized each boolean; do not
claim stock loss or invent which conjunct failed. Native native-order proof is
being reviewed; do not waive owner checks under a broad stopped/lookup-null flag.

Full-envelope actual pretty-Gson encoding additionally exceeded unchanged512MiB
UTF8 bound, so genuine small FAILED_DIAGNOSTIC_ONLY fallback2914bytes was written
(SHA`3955bf7a...`), with both close outcomes in exact Result footer. Actual full
payload was NOT persisted; its data/native23 history cannot be reconstructed.
No bound increase, event thinning, tail reconstruction or failed-run promotion.

NEW `.tmp/bore-client23-rejected-stop-owners-volume/` archives exact logs/progress/
failedtail/proof/copy/inputs/tools/prelaunch/source-template/compiled expectation/
all10PNG and actual client-test/Python source bytes. ROOT revalidated all3963
inputs/all2405 source-envelope hashes and parent191 exact AFTER stop before any
edits. ROOT individually opened all10 exact174–183 PNGs, hash-bound copies in
`exact-PNG-visual-review.json`; no bounded presentation blocker identified. Pixels
are not full resource, ownership or timing evidence. Independent six-argument
auditor honestly rejected failed diagnostic before complete world validation.

No source/Python fix or new Gradle/game launched. Three independent tasks active:
* `ses_eb11e1d53ffetuAdEbCzt7BDq2`, NEW`.tmp/bore-native23-unload-diagnosis/`:
  read-only pinned native lookup/unload order + exact actual23 failure stacks;
  consider strict paired actual unloadEntity HEAD/RETURN if native lookup leaves
  BEFOREsetRemovedHEAD. No guard edits/waiver, no world reads.
* `ses_eb05a656affeQa6BNnFzh2CNXX`, NEW`.tmp/bore-native23-volume-diagnosis/`:
  assess LOSSLESS compact Gson formatting only, exact same envelope/events/values,
  no cap/schema/truth changes. May run isolated realGson/Java experiments into NEW
  ignored directory only; NO game/Gradle/production edits/native payload recreation.
* `ses_eb0a73d9fffejJOtihaijHTHIc`, NEW`.tmp/bore-client23-endpoint-diagnostic-independent/`:
  repeat read-only all113/all11/all7/old20-333 vsNEW188 typed stopped endpoints,
  never full history/ambient birth acceptance. Parent/candidate hashes before/after.
Await completion notifications WITHOUT polling or reading WIP. No24 until exact
narrow fixes/reviews/source pins/current actual Fabric compile pass.

**LATEST native23 LAUNCHED; await notification, NOT acceptance.** Independent
stop/flesh reader FINAL200 freeze SHA
`3430d53e9e106e21d022a05cfc37a809ae246e110476d2361890a55f92793450`
received and ROOT verified all200 entries. ROOT reran original522+158 additive,
current8glue/9plain/10shadow and authenticated historical8bootstrap/5table/13body
checks PASS.24 current flat Python reader/test sources AST-parsed. No producer
blocker identified; native16–22 remain rejected, never inputs or retroactively
completed. All original source/body/custody/stock/clock/geometry guards retained.

NEW23 snapshot3963 inputs AGAIN equals actual compile8. All1114 available compiled
classes match143-resource expectation
`dba30ba1f9040639c0358d077790736f2e3f75658f0c1c51867d14bcce891df2`.
NEW source-only template2405 actual files was successfully consumed by ACTUAL
strict source_snapshot BEFORE launch, including all stop/flesh/preimage/helper
chains. Runtime installed closure is still a future obligation, not proven by
that expectation. Accepted191 exact payload paths/hashes AGAIN before/after.
NEW23 tools/prelaunch receipts preserve these checks. No failed world input.

Native23 shell`sh_14f99320c001tOwyNvcO3V6pT5` launched via usual guarded wrapper.
NO polling/runtime log/result/tail/world reads, source/auditor edits or other
Gradle/game while active. User told not to touch controls/switch Minecraft window.
Still only ONE bounded200native/14wall custody excursion. Full original native
stop validators PLUS primary/companion/whole-console truth gates must pass. No
story/fulltail/PNG/stopped audit/finalchecks/export acceptance claimed yet.

**LATEST actual compile8 PASS; independent stop/flesh reader review still active,
NO native23.** Actual Fabric clientTest exit0/BUILD SUCCESSFUL20s. All21 required
observers, prior37 actual Java assertions, actual stop-helper26 scenarios/87670
assertions, and real Fabric-runtime Gson3 scenarios/52 assertions PASS. The stop
assertion count includes path-length-dependent JSON parser checks; author's earlier
86566 count is retained as its own run, not rewritten or falsely matched. The
expected fault-injection stack traces are declared detached checks, NOT native
runtime errors. No Minecraft/game/save execution occurred during this compile.

Root verified all3963 frozen inputs unchanged afterward. Exact compile8 raw/UTF8
logs, exit/verified-result and compiled-resource expectation are retained under
`.tmp/bore-custody-integration-check8*`. Fresh143-resource closure/1114 available
compiled classes fingerprint
`dba30ba1f9040639c0358d077790736f2e3f75658f0c1c51867d14bcce891df2`.
Actual native installed closure remains unverified. No Gradle/game remains; root
must await independent reader's FINAL freeze without polling/WIP reads/Python edits
before actual source_snapshot/prelaunch binding and a fresh accepted-parent run.

**LATEST exact stop+plain-flesh SOURCE_FROZEN; actual compile8 RUNNING, NO23.**
Read-only flesh assessment confirmed plain empty-raw-patch encodability without
FOOD/prototype traversal; its caveat was addressed with an explicit runtime
raw-patch-size0 guard AND independent-reader plain-tag requirements (under review).
Root changed only witness exact item set, utility exact ambient item gate/plain
guard/comment, and Ledger exact item scope plus ambientTag on ALL trajectory
states. NO new FOOD/other codec/prototype/FocusData support, grant, drop mutation,
source/cohort/death/actor/type/age/cap/role/credit relaxation. Old flesh252a... remains
the fixed inherited7 obligation. No appearance is induced/required and native22
orphans remain unproven/unaccepted. Root9 pure source/model tests passed.

Plain-flesh13 freeze `.tmp/bore-ambient-flesh-support-review1/SOURCE_FROZEN.sha256`
SHA`499616ee5f596769794b245b195cec7755429da03b3d871d9623e26f7408e73b`.
Root stop56 freeze `.tmp/bore-stop-root-integration-review1/SOURCE_FROZEN.sha256`
SHA`db7fdc9bacefe9801e0f2823de86074ea72a3d42a29199afa80e2a2726b3d753`.
Root8 glue source checks PASS; original pureJava helper/Checks source remains
exactly author's frozen e2cf4e70/f841993a with26actualJava scenarios/86566assertions.

Root additionally authored NEW test-only `BoreStopTailGsonChecks.java` for actual
Fabric-runtime Gson +actual helper (3 declared-synthetic scenarios: healthy full
record/primitive envelope, failed finalizer full report, NaN->diagnostic fallback).
No MC objects/stubs/saves. Actual execution pending compile8, not claimed PASS.
Installed binding adds ONLY runtime helper root/nests, not the check classes.

Fresh3963 inputs SHA`30acb1de...` +private tool hashes frozen. NEW guarded
`.tmp/bore-custody-stop-integration-check.ps1 -RunName bore-custody-integration-check8`
launched shell`sh_14f7eede0001gNWCx4B01Ups2S`. It compiles actual Fabric clientTest,
runs37priorJava assertions +new actual helper fault checks +realGson checks in
NEW ignored sandboxes, and verifies21required observers. Await notification, NO
polling/otherGradle/game/source edits. Historical tools/results retained unchanged.

Independent reader`ses_eb1a06451ffe3O2TeIuVeDbBPz` owns Python-only combined strict
source-stage closure/root binding +post-close primary/proof/whole-console gates
and prospective plain-flesh full source/death/trajectory/stopped proof. No WIP
reads/polling or Python edits by root. It must preserve original522+120 truth
guards and every all7/11/113/13acquisition/clocks/custody/RP/OLD8/333/NEW rule.
No native23 until actual compile/current hashes and final reader freeze both pass.

**LATEST pure stop writer SOURCE_FROZEN received; ROOT glue AUTHORED but NOT yet
frozen/compiled/reviewed; no native23.** Helper e2cf4e70... /Checks f841993a... at
`.tmp/bore-stop-tail-review1/`; ARTIFACTS manifest SHA`fbbc040b...`, all72 listed
hashes ROOT verified. Author actually executed Java21 ONLY these2 exact sources:
26 scenarios/86566 assertions +15 source/9console checks PASS, noMC stubs/Gradle/
game. Helper never weakens native validators. Full encoding happens BEFORE primary
CREATE_NEW; failures get explicitly failed diagnostic-only fallback; post-close
`.write-proof.json` binds exact primary bytes/hash after actual close. Historical
source/tests/sandbox outputs remain immutable. This is NOT Fabric/native proof.

ROOT wired `Sequence` actual native HEAD observer return flag +STOPPED callback to
the real helper callbacks (unchanged Ledger validation and4 real cleanups), added
separate callback/client-submission/client-body logging/containment, and actual
post-IO original B/combined deadline checks. A unique `BORE_STOP_TAIL_RESULT ` JSON
console line after helper RETURN exposes actual Result/primary+proof close facts.
Future independent reader must require matching full primary/companion/complete
console proof, with no run-specific helper/callback error (including async/clock
errors); encoded primary STOPPED alone is NOT sufficient. NativeBinding adds ONLY
the actual `BoreStopTail` root/nest closure. Root's8 source glue contracts PASS.

Root integration preimages/API/tests are `.tmp/bore-stop-root-integration-review1/`.
NEW private compile wrapper/init `.tmp/bore-custody-stop-integration-check.*` is
prepared to run actual Fabric-compiled stop checks into a NEW nested sandbox;
NOT EXECUTED. Old private scripts untouched. Root has not edited Ledger or any
core actor/stock/ticker/phase/helper authority guard. Current source inputs now
include NEW2 helper/check sources; compile7 is historical, not current validation.

Independent stopped endpoint diagnostic completed at
`.tmp/bore-client22-endpoint-diagnostic-independent/`: typed all113 workshop BEs
retained (105exact/8actualdiffs recorded), all11 cows WHOLE typed tags exact,
oldjar typed exact chest7 +six old loose whole typed tags/Age unchanged, old20/333
exact, separateNEW11 total188. All191 parent hashes and both read save/source
trees unchanged before/after. This is ENDPOINT ONLY, not300/112 debit/history or
native acceptance. Node aura/cache/furnace/tube and11 further BE deltas outside
workshop are all reported, not broadly ignored. No item/container-stock loss
endpoint finding; native histories remain unavailable without the missing tail.

New bone2 UUID461c22d7... and flesh2 UUID8f4bf923... are UNPROVEN, not admitted by
endpoint matching. Source-only utility reviewer`ses_eb1a11a46ffeha07bPV6KPbpMz`
owns NEW`.tmp/bore-ambient-flesh-passivity-assessment1/` READ-ONLY assessment:
can a plain genuine environmental rotten_flesh native birth use the existing
passive patch serializer with ONLY an exact ambient-ID addition and no FOOD/new
codec/prototype traversal? No source edits requested. All original cohort/
HEAD-ADD-RETURN/death/trajectory/AABB/type/old7/zero-credit guards must stay strict;
no retrospective22 admission, RNG reroll, difficulty/seed/drop grant/removal.
Wait final assessment without WIP reads/polling before any implementation/freeze.

No Gradle/game running, no reader author editing Python now, no native23 launch.
Next: close source-only support assessment, freeze exact integration/support
sources, get independent reader source-chain+stop-proof gates, then actual Fabric
compile (including actual Java fault tests), bind fresh resources and only then
new accepted-parent run. Do not mistake authored glue/pure assertions for native.

**LATEST stop diagnosis received: proven writer-control-flow hole, actual22 cause
STILL UNKNOWN.** `.tmp/bore-native22-stop-diagnosis/REVIEW.md` records19 fresh
native/Fabric/Architectury captures and hashes. Normal vanilla sets stopped=true
BEFOREstopServerHEAD; FabricSTOPPED is synchronous at base stopServerTAIL after
closes, still on the server thread. Native levels/registry maps are NOT proven
cleared. Passive/inherited helpers have NO !isStopped guard. Do not relax any
authority/owner/expiry/native tick checks based on an unsupported shutdown guess.

Source proves a thrown ledger finalization/cleanup can skip creating the tail,
while catch only latches memory after smallprogress was written. Next correction
will separately contain/log stages and still attempt an explicit NEW FAILED tail;
nonfailed status must require actual validator completion, native HEAD completion,
successful server-side cleanup and no latched failures. Report serialization must
precede file opening, diagnostic-only fallback may never count as full evidence,
and no existing/partial file may be overwritten. No native22 reconstruction.

Source author`ses_eb0a88d83ffeiGIQG6bA0QxhBO` owns ONLY NEW private pureJava
`BoreStopTail.java`, `BoreStopTailChecks.java` +NEW `.tmp/bore-stop-tail-review1/`.
It may run actual detached Java21 fault-injection tests on those exact sources
into NEW ignored sandbox (NO Gradle/game/MCstubs/worlds); await SOURCE_FROZEN,
do not poll/read WIP. Root will wire Sequence/installed binding AFTER final API.
Exact current Sequence5666ad18... and NativeBinding75bca567... preimages already
archived at `.tmp/bore-stop-root-integration-review1/`; no root source edits yet.

Independent endpoint diagnostic agent`ses_eb0a73d9fffejJOtihaijHTHIc` owns ONLY NEW
`.tmp/bore-client22-endpoint-diagnostic-independent/`, read-only accepted/candidate
stopped NBT +source. It is enumerating ALL113 BEs/all11 cows/all7 loose/custody and
old20/333 vsNEW188 endpoints with hashes, not native chronology or acceptance.
No tail waiver, inferred item ticks, ambient birth admission, full300/112 proof
or failed-world input allowed. No other source/Python changes or Gradle/game now.

**LATEST native22 completed game story +10 PNG, but NOT ACCEPTED: full normal-stop
tail is genuinely MISSING; wrapper exit1.** Run
`client-smoke-86f6bb47-6c20-42a5-b501-cc6c48121a74`, retained stopped world
`bore-smoke-803f0fbc-31fe-4781-89d1-b7d5b4d15a4a`, progress SCRIPT_PASSED/errorNONE/
story_complete with ONE note and last actual tick12104. Genuine research/base/
300distillation/112infusion/top/two harvest story reached FINISH, but its small
progress file is NOT complete proof. Do not promote/reconstruct the absent tail,
reuse this world as retry input, export or claim full conservation acceptance.

The guarded Gradle task failed ONLY at required tail existence after normal game
stop/save log. Source `Sequence.serverStopped` wraps `ledger.stopping()` and tail
writer in one try; a thrown validator can prevent opening the tail and only latch
failure in memory AFTER progress file was written. Actual22 error/failed predicate
was NOT persisted/logged, so cause remains unproven; callback absence is another
possibility. No source fix or guard relaxation has yet been applied.

NEW archive `.tmp/bore-client22-story-complete-missing-tail/` hash-binds raw/UTF8
logs/result/copy/input/prelaunch/compiled expectation and all10 PNGs, plus exact
current client-test/Python sources. ROOT checked all3960 inputs and2338 source
envelope files unchanged, parent191 AGAIN exact. No Gradle/game writer remains.
ROOT individually opened EVERY exact174–183 PNG and hash-bound copies in
`exact-PNG-visual-review.json`; bounded GUI/row/whole-model/beam presentation has
no identified visual blocker. Pixels do NOT establish resource/timing/UUID stock.

ROOT executed independent stopped-auditor with all SIX explicit arguments; it
correctly failed FileNotFoundError on the absent tail BEFORE source/installation/
world conservation validation (`independent-audit-missing-tail.json`). Prelaunch
source-template binding remains an expectation, not native installed proof; no
post-run run-bound source envelope was invented without the missing native proof.
No all113/all11/all7 or300/112/188 native/stopped audit PASS or final fresh checks.

Read-only native/source stop diagnosis agent`ses_eb11e1d53ffetuAdEbCzt7BDq2` owns
NEW`.tmp/bore-native22-stop-diagnosis/` only. It is proving native lifecycle/owner/
serializer constraints and safe diagnostic-tail design; no source/Python/Gradle/
game/runtime artifact/world reads/edits. Await notification, no WIP polling. Root
has NOT started23 or promised a final retry. Next fix must retain every live/
terminal guard, persist genuine failed-stop facts rather than manufacture success,
and be independently reviewed/compiled before any later fresh-parent attempt.

**LATEST NATIVE22 LAUNCHED; await completion, NOT acceptance.** Independent
table-approach reader froze SHA`702d3e56...`; ROOT verified all130 current receipt
entries and reran original522 +120 additive pure checks PASS. Exact root3 stage
and current ONE camera delta are independently authenticated, not a root3 hash
waiver. Additional route/support checks are active; no producer blocker identified.
Root5 approach/10shadow/13body tests separately passed on unchanged sources; old
root8 wholefile bootstrap checks remain historical root3-stage proof only.

Actual compile7/37Java assertions/21observers previously passed; NEW22 snapshot
3960 inputs AGAIN equals actual compile7 and all1101 available class outputs
match136-resource compiled expectation
`cb60ffb0b3d76a9ab0e57f91e4fdee15ea42125c399c54da94a9f21d1ffb5870`.
NEW source-only audit template2338 actual files was consumed successfully by
ACTUAL `source_snapshot` BEFORE launch (root3/table/body/touch/r3 pins all strict).
This is a prelaunch expectation; actual installed native resource closure must
independently match. Accepted191 payload paths/hashes AGAIN exact before/after.
NEW tools/prelaunch receipts; no rejected world/note/earned materials as input.

Native22 shell`sh_14f3d82ca001cvBbvXsyfDWoxD` launched via the guarded wrapper.
NO polling/runtime log/result/tail/world reads/source-auditor edits or other
Gradle/game while active. User told not to touch controls/switch Minecraft window.
Every physical/native/custody/deadline guard remains unchanged; only the explicit
ONE200native/14wall rescue exception. No gameplay/fulltail/PNG/stopped audit/
freshfinal/export PASS claimed yet.

**Latest actual compile7 PASS, NO native22:** exit0/BUILD SUCCESSFUL18s; all21
required private observers and37 actual detached Java assertions passed. Root
verified all3960 inputs unchanged after compilation. `.tmp/bore-custody-integration-check7*`
binds raw/UTF8 logs/result and136-resource/1101-class compiled expectation
`cb60ffb0b3d76a9ab0e57f91e4fdee15ea42125c399c54da94a9f21d1ffb5870`.
Not native evidence. No Gradle/game remains; independent exact approach source
chain/camera review is still active. Do not read/poll WIP or launch22 yet.

**LATEST native21 REJECTED at second-table ray; one-line approach correction
SOURCE_FROZEN, compile7 running, NO22.** Run
`client-smoke-42fcc58c-d910-4811-ac3a-43023a44e502`, retained failed world
`bore-smoke-a80515c6-3f94-433e-ad84-bf3066e87ff6`, exit1/nativeTick661, note0/PNG0/
no full normal tail. Actual first BLOCK/UP validation for anchor(1603,215,382)
failed. The error only stored Java BlockHitResult object text, NOT actual hit
coordinates/face, so do not label a particular hit as observed. The early chest
is a concrete planned occluder; native21 is not accepted/custody-audit proof.

Full log/result/copy/input archive `.tmp/bore-client21-rejected-second-table-ray/`.
All3960 inputs still exact before edits; parent191 checked again before/after
finite64 saved-cell diagnosis, all exact. Rejected world NEVER a retry input.

Root changed ONLY one Sequence camera literal: second-table approach moves from
east1605.5,216,382.5 to south1603.5,216,383.5 with its precise new label. Original
stone support1603,215,383 and body AIR were explicitly read from accepted saved
cells. New ray proposal misses even the chest's full block and stays south of
first table; static geometry is NOT native clip proof. Actual camera loaded/
support/noCollision/entity checks and genuine OUTLINE firstBLOCK/UP/reach/packet
remain byte-identical. No extra Op, floor, material, action replay or clock.

`.tmp/bore-table-approach-review1/` has exact root3 Sequence preimage, metadata,
finite geometry/API and5 pure source/static tests PASS.8-input SOURCE_FROZEN SHA
`d2c1f29e5f095be33031e539499b25f66a8f8262f3446569aa4578de6d283173`.
Other16root3 rows and both corrected helpers/r3utility unchanged. Original root3
Sequence row is now HISTORICAL and must be reconciled by exact one-line source
chain, not generic waiver. Old frozen bootstrap8 wholefile test is historical
for its source revision; do not claim it freshly passes this camera change.

Actual Fabric compile7 fresh3960inputs SHA`5e5fef2e...` launched shell
`sh_14f310636001Kmh7K9G7pA9ldi`; await completion/no polling/otherGradle/game or
source edits. Independent reader agent owns Python-only exact source-chain/camera
proof checks, not world reads or relaxed ray guards. Do not read WIP or launch22
until its freeze/review and actual compile/source postcheck both pass.

**LATEST NATIVE21 LAUNCHED; await completion, NOT acceptance.** Independent
touch-RETURN reader froze SHA`7d7ba712...`; ROOT verified all119 entries and
reran original522 +108 additive tests PASS. Root's separate10 shadow/8 bootstrap/
13 body source/synthetic checks passed on these unchanged inputs. No additional
producer blocker; dead equal-content slot rebind/no-returned-hook limitations
remain disclosed, not waived. Exact corrected inherited helper SHA`6ec60003...`.

Actual compile6/37Java assertions/all21 required observers passed and all3960
inputs unchanged. NEWnative21 snapshot again enumerated3960 equal actual compile6;
all1101 available class outputs still match136-resource compiled expectation
`500774b54f9bce12697fe2e7ea591818428649f26012b9a1ce75854489251fae`.
NEW source-only audit template2330 actualfiles was consumed by ACTUAL strict
`source_snapshot` BEFORE launch (root3/body/touch/r3 independent pins intact).
Actual runtime installed resource closure must independently match that compiled
expectation; template is NOT native evidence. Accepted191 payload paths/hashes
AGAIN exact before/after. NEW21 tools/prelaunch receipts; no failed-world input.

Native21 shell`sh_14f2a7ab8001tIb0u30ar51XoA` launched via usual guarded wrapper.
NO polling/runtime log/result/tail/world reads, source/auditor edits or other
Gradle/game while active. User was told not to touch Minecraft controls/window.
Only approved ONE200native/14wall jar excursion; all7/11/113,20/333/72, original
bootstrap/phase/combined/expiry/physical accounting and real acquisition guards
unchanged. No gameplay/image/fulltail/stopped audit/finalchecks/export PASS yet.

**Latest actual compile6 PASS, NO native21:** exit0/BUILD SUCCESSFUL14s; all21
required private observers and37 actual detached Java assertions passed. Root
verified all3960 source inputs unchanged afterward. Raw/UTF8 logs/result and
compiled expectation `.tmp/bore-custody-integration-check6*`:136 bound resources,
1101 available class outputs, fingerprint
`500774b54f9bce12697fe2e7ea591818428649f26012b9a1ce75854489251fae`.
This is NOT native runtime/stock evidence. No Gradle/game remains; independent
reader's exact restored-shadow source-pin review still owns the next launch gate.

**CURRENT RETURN-only correction SOURCE_FROZEN; compile6 complete, NO21.**
Fresh read-only pinned native proof confirms normal full pickup has original
stack count1 at HEAD, zero at native discardHEAD, and count1 restored on the
already-REMOVED entity at BCI87 before RETURN108. Inventory receives a distinct
stack. Dead entity's retained shadow is not another physical stock owner.
`.tmp/bore-native19-touch-diagnosis/REVIEW.md` owns20 fresh captures, callbacks/
alias caveats and exactBCIs. This proves old EMPTY-at-RETURN requirement wrong
for the normal path, NOT which compound predicate native19/20 actually recorded.

Root changed ONLY successful RETURN predicate/comment/error in inherited helper:
same removed/DISCARDED/UUIDabsence plus EXACT original count1 typed shadow tag
instead of empty. RemovalHEAD still MUST observe empty before native setter;
all item/player/tick/Age/spines/41slotONEdelta/f.original/refusal/limits/receipts
and root independent physical custody checks byte-identical. No count0OR1 fallback,
serializer change, expected-template item, mutation or extra physical stock.
Minimal content-only fix does not claim equal-content dead-slot reference binding
or no returned foreign hooks. Allroot3/17, ambient/body and r3utility unchanged.

Exact preimage/API/10 declared-synthetic/source/capture tests PASS at
`.tmp/bore-inherited-touch-return-review1/`. New43-input SOURCE_FROZEN SHA
`a1e9a99ff40c7093ea22985257f1adf240899df1d62641276bddcdfeee102faa`.
The old r3 inherited source row alone is explicitly superseded; previous ambient
correction remains independently pinned. Native19/20 remain rejected, not inputs.

Actual Fabric compile6 with3960 fresh frozeninputs SHA`296a5f29...` launched
shell`sh_14f178b1e001xU5xR1EAPP1wvp`. Await completion/no polling/source changes/
otherGradle/game. Independent reader author owns Python-only exact helper source
pin/diff review and fixtures, not runtimeguard waivers. Do not read WIP or launch
native21 before its newfreeze and actual compile/source postcheck both pass.

**LATEST NATIVE20 COMPLETED exit1, REJECTED; no game/Gradle remains.** User-requested
unchanged retry reproduced the same inherited touch-RETURN compound failure at
nativeTick108. Run`client-smoke-cc51681a-0887-468f-877d-a31dffa0a5ff`, retained
failed world`bore-smoke-3dc5b9bb-ef5b-4ab5-aeed-e9e56bf5e2dd`. No research note,
PNG or normal full tail. Exact stopped archive `.tmp/bore-client20-rejected-touch-return/`
retains logs/result/copy/input/source receipts. All3960 current input hashes and
accepted191 immutable hashes unchanged. Failed20 never a retry input or accepted.

Root performed a separate READ-ONLY stopped endpoint diagnostic: exact oldjar
decoded components/count1 are in real player slot32, original loose UUID absent;
all six other inherited loose UUID stacks remain with unchanged saved Age. Both
level.dat/playerdata inventories agree, all actually read payload hashes unchanged
before/after. `.tmp/bore-client20-rejected-touch-return/stopped-physical7-diagnostic.json`
is decoded-value endpoint evidence ONLY, NOT typed NBT/full native custody, all113/
all11 or complete audit PASS. It cannot replace the missing failed pickup receipt.

Read-only native touch-count diagnosis agent is still active; no source edits,
new game launch, guard relaxation or restore performed. Await its SOURCE/native
proof notification without polling. Next correction needs exact narrow helper
preimage/source review, strict native discard+count+inventory proof, fresh compile
and independent reader source pin. Do not rerun unchanged again automatically.

**LATEST USER-REQUESTED NATIVE20 REPEAT LAUNCHED, NOT acceptance.** Native19 failed
at actual inherited jar touch RETURN (`Native original exhausted ItemEntity
DISCARDED` compound check), tick123, before note/PNG/full normal tail. Run
`client-smoke-c1460df6-d5d4-4122-bd6f-81a877ca4cab`; retained failed world
`bore-smoke-24d21c73-fb72-429d-b7ec-1f205bec464e`. Full result/log/copy/input archive
`.tmp/bore-client19-rejected-touch-return/`. Failed world NEVER a retry input.
Actual failing conjunct not serialized; no claim of pickup/preservation PASS.

Read-only native reviewer`ses_eb11e1d53ffetuAdEbCzt7BDq2` is checking the hypothesis
that vanilla restores the now-DEAD ItemEntity stack count AFTER its exhausted
native discard and BEFORE RETURN. It owns NEW`.tmp/bore-native19-touch-diagnosis/`
only, no source/reader edits or game/Gradle/runtime/world reads. Await notification;
do not apply a permissive count0-or1 fix or infer actual cause before native proof.

User explicitly said **«Запусти еще раз»** while that read-only work was active.
Root disclosed same unchanged guard may fail again, then prepared native20 as a
fresh accepted-parent copy with IDENTICAL compile5 inputs/guards/deadlines. All3960
source inputs still equal actual compile5; actualsource template2291 validated,
compiled1101 outputs match136-resource expectation`bce6f505...`; reader75 current
hashes and immutable191 parent hashes all exact. NEW20 tools/prelaunch receipts.

Native20 shell`sh_14f0fadda001CugZN0ZNiayrG8` launched via guarded wrapper. Await
completion, NO polling/runtimeartifact reads/source edits/otherGradle or game.
User told not to touch Minecraft controls/window. Only the same approved bounded
custody excursion; no extra stock, world reuse, restoration, clock or guard waiver.
No native PASS or export. Read-only javap investigation may continue independently.

**LATEST NATIVE19 LAUNCHED; await completion, NOT acceptance.** Independent
bootstrap-v3 reader froze SHA`25bbeee5...`; ROOT verified all75 receipt entries
and actually reran original522 +96 additive tests PASS. Its readiness diagnostic
reader now requires actual successful loaded-owner/contact readiness before any
custody exemption, without inventing wall/helper-prefix chronology. No additional
producer blocker identified. Compile5/37Java checks/21observers separately passed.

NEW`.tmp/bore-client19-inputs.json` has3960 inputs equal ACTUAL compile5; all1101
available class outputs still match compiled expectation5 with136 resources and
fingerprint`bce6f505...`. NEW source-only audit template enumerates2291 actual
files and was consumed successfully by ACTUAL `source_snapshot` before launch;
source root3/body/r3/archived preimages are all strict. Template binding is only
prelaunch expectation: actual native installed resource proof must still match.
Immutable accepted191 parent files AGAIN all exact before/after; no rejected
world or earned result input. NEW tools/prelaunch receipts bind these facts.

Native19 shell`sh_14f0bc5e7001yHxTGIRiXaJ7T1` launched via normal guarded wrapper.
NO polling/active log/result/tail/world reads, source/auditor edits or other
Gradle/game while running. User was told not to touch controls/switch Minecraft.
One approved200native/14wall jar custody exception only; original bootstrap600/60,
combined/R/B, all7 expiry margins, all113/all11 and normal20/333/72 gates unchanged.
No native result/image inspection/stopped audit/final checks/export claimed.

**Actual compile5 completed PASS:** exit0/BUILD SUCCESSFUL17s, all21 required
observer class/resources and37 actual detached Java assertions passed. Root
verified ALL3960 inputs unchanged after compilation. Raw/UTF8 logs/result and
compiled expectation are `.tmp/bore-custody-integration-check5*`; actual bound136
resources/1101 available classes fingerprint
`bce6f5050ab16b37527417f0c23ece2d86afee17303a14bc3f9d596d89433bbd`.
This remains a prelaunch expectation, not runtime evidence or native preservation.
No Gradle/game is running. Reader root3 review/source-only fixtures still own the
launch gate; do not poll/read its WIP or claim native19 started.

**LATEST NATIVE18 REJECTED before departure; bootstrap3 fix under review.**
Run`client-smoke-a436d560-4e3a-4b69-824b-318b9b70a17d`, retained stopped failed
world`bore-smoke-744d1ed4-02e0-472f-91c0-b0787ea73a4c`, exit1/nativeTick81.
Error: `Exact old jar must really be loaded before departure; never force load`.
No custody state departure/TP/gap, note, PNG or full normal-stop tail. The source
assertion correctly refused an unavailable item; no loss/user-input cause is
inferred. All3960 source inputs and immutable191 parent hashes remained exact.
New hash-bound log/result/proof/input archive `.tmp/bore-client18-rejected-jar-not-loaded/`;
failedworld never a retry input. No acceptance/export; no game process remains.

Root archived all12 schema2 inputs before changing ONLY Sequence.bootstrap and
adding Ledger.custodyBootstrapReadiness. The SAME original600native/60wall Op
binds ONE baseline/registers POST+ADD as soon as workshop20/333/72/local3 ready,
then passively waits for actual helper-bound oldjar +rawcontact chunk81,22 BEFORE
preflight/Rraw/Rorigin. Continuous normal20/critical/ambient/retirement observation
continues while waiting; no hidden delayed baseline, repeatedbaseline/new600cap,
forcedload, phase restart, expiry margin or custody deadline change. Departure and
all gameplay/camera/pickup/recovery/stow predicates remain unchanged.

Exact `.tmp/bore-root-custody-bootstrap-v3.md`;8 new declared-synthetic/source-diff
checks PASS. Root3 SOURCE_FROZEN `.tmp/bore-root-custody-native-schema-freeze3.sha256`
SHA`d079c2e23ac078f15eb25397b0826cd8a67cd953709024ce217485601c774df9` binds17 inputs,
including exact old2-source preimages. Body-admission helper/r3 utility/inherited
and all trajectory/menu/clocks/prefix/readiness/stock schemas remain unchanged.

Actual compile5 with3960 frozeninputs SHA`7f6e64b8...` launched shell
`sh_14efd6dc7001JEfh9oBcD1Wwta`. Await completion; no polling/source changes/other
Gradle/game. Independent reader agent owns Python-only root3 source pin review/
fixtures, not a generic oldhash exemption. Do not read WIP or launch native19
before fresh actual compile and independent final freeze both pass.

**LATEST NATIVE18 LAUNCHED; await completion, NOT acceptance.** Reader narrow
admission correction freeze arrived SHA`5ff2e41c...`; ROOT verified all59 entries
and reran original522 +84 additive checks PASS (prior73 unchanged), along with
its earlier13 admission/source checks. No remaining producer blocker identified.
Actual compile4/37Java assertions/21observers are separately verified below.

NEW native18 inputs equal actual compile4 (3960 files); all1101 available compiled
classes still match expectation4 fingerprint`60d13543...`,136 bound resources.
The actual source-only audit template `.tmp/bore-client18-audit-source-template.json`
enumerates2285 exact current files and was successfully consumed by the ACTUAL
strict `source_snapshot` reader BEFORE launch. Its binding is explicitly only a
prelaunch expectation; runtime installed binding must independently match.
Initial large source-enumeration command timed out AFTER creating two files,
BEFORE any game/Gradle launch; continuation did not overwrite them and completed
actual full source-template validation/current59/compiled1101/parent191 checks.
All191 immutable accepted-parent payloads again exact before/after. No failed
world or earned result is input. New tools/prelaunch receipts bind these facts.

Native18 shell`sh_14ef80ce5001RetQh73B6tmGQQ` launched through the usual guarded
`.tmp/bore-client-run.ps1 -RunName bore-client18`. NO polling/runtime world/log/
result/tail reads, source/auditor edits or other Gradle/game while active. User
was told not to touch control/switch the Minecraft window. Real native foreground
and Shift checks still mandatory. Only approved <=200native/14wall jar excursion;
normal20/333/72 coverage and all7/all11/all113/conservation/time gates unchanged.
No current gameplay PASS, image inspection, stopped audit or export is claimed.

**Latest compile4 PASS, NO native18.** Actual Fabric clientTest compile exited0,
BUILD SUCCESSFUL16s, all21 observer classes/resources and37 actual detached Java
assertions PASS. All3960 inputs independently checked unchanged afterward.
Compiled expectation4:136 bound resources/1101 available classes, fingerprint
`60d13543f530833d5e6df9f75151a1a2567e54c679fd5f68ade796a51324a67e`.
Raw/UTF8 log/result and compiled expectation are `.tmp/bore-custody-integration-check4*`.
No native-gameplay PASS is inferred. Reader narrow source binding review remains
active; no Gradle/game running and do not read/poll its WIP.

**Current narrow fix SOURCE_FROZEN; compile4 completed, NO native18.** Read-only
native reviewer proved possible vanilla `checkDespawn -> removed Mob ->
tickNonPassenger HEAD` dispatch without subject-removal recheck; this is NOT
proof of native17's actual failed boolean. Full17capture evidence and BCIs live
in `.tmp/bore-native17-body-diagnosis/REVIEW.md`.

Root changed ONLY ambient witness: exclude already-removed ordinary subject at
HEAD AFTER pending-frame assertion, BEFORE creating Body/current; no new authority
or birth/retirement waiver. Existing nine `current` success conditions unchanged,
ordered and split solely for future first-failure diagnostics. All RETURN/active
source/owner/retirement checks unchanged. Root schema2 all12 remain exact; r3
utility/inherited/codec rows exact, old ambient row alone explicitly superseded.
Exact preimage, contract and13 declared-synthetic/source-diff tests PASS are in
`.tmp/bore-ambient-body-admission-review1/`; receipt85 hashes
SHA`ae4b862c2c279067a6591d593706a490da7a72b2b8c79ea35d0c9c74b4740659`.

Fresh3960input compile4 SHA`c58f9c03...` launched shell
`sh_14ee998700014lC79hZ73DoaQ0`; await completion, no polling/otherGradle/game.
Independent reader author owns Python-only narrow source_snapshot review/update
to admit exactly new reviewed ambient bytes, NOT a generic r3/helper hash waiver.
Root will not read WIP or launch native18 until review freeze and actual compile
postcheck both pass. No game running is claimed beyond the one compile process.

Native17 exited1 and is rejected: actual helper `BoreAmbientItemWitness.current`
failed at native `bodyHead` with `Not actual same-world loaded UUID owners (no
observer loads)`. It is NOT a foreground/Shift error; user's accidental movement
is not established as the cause. The conjunction did not serialize which term
failed, so do not claim a particular mob/lifecycle cause as proven yet.
Run`client-smoke-427d9e1a-5563-4379-874a-fa75af57fac2`, stopped retained world
`bore-smoke-1022a7ed-8d24-4ce5-8e83-d23c2c93709a`, nativeTick81, no research note
requested, no PNG, no full normal-stop tail. Failed world NEVER a retry input.
All3960 source inputs remained exact. Logs/result/copy-proof/input receipts are
archived with SHA256 in `.tmp/bore-client17-rejected-observer-bootstrap/`.
Post-failure read-only parent check reconfirmed all191 exact payload paths/hashes.
No Gradle/game process remains. No audit/acceptance/export is claimed.

Read-only native-source diagnosis agent`ses_eb11e1d53ffetuAdEbCzt7BDq2` is proving
whether native `checkDespawn` can remove a Mob before still calling
`tickNonPassenger`; this is a hypothesis until its actual bytecode review arrives.
It owns only NEW ignored diagnostic notes, no Java/Python/game/world edits.
Root has not changed frozen sources; next fix needs explicit new review/freeze,
actual Fabric compile and final input check. No broader survival/coverage waiver.

**LATEST NATIVE17 LAUNCHED; await completion, NOT acceptance.** Final independent
geometry-v2 reader SOURCE_FROZEN received SHA`375bb407...`; ROOT verified ALL74
receipt entries and actually reran **522 original +73 additive pure checks PASS**.
No producer-schema blockers remain identified. Compile3/37 actual Java checks/
21observer installation/all3960unchanged inputs are separately verified below.

ROOT created NEW`.tmp/bore-client17-inputs.json` and tools/prelaunch receipts.
All3960 inputs equal actual compile3; all1101 available compiled outputs still
match expectation3 (136resources/fingerprint6ed5ef81...). Accepted191 payloads
were read-only checked before/after AGAIN, all exact. No failed world, earned
note/material or old runtime evidence is input. Fresh-copy proof remains owned
by unchanged Gradle prelaunch191 verifier.

Actual native shell`sh_14edb6165001s9sLDLPZV7862o` launched through
`.tmp/bore-client-run.ps1 -RunName bore-client17`. Await actual notification;
NO polling/active world/log/result/tail reads, source/auditor changes or other
Gradle/game. Owned Minecraft-window preflight remains mandatory. User's ONLY
exception is the single <=200native/14wall custody excursion, actual20/333/72
return and native original jar chest7; all7/all11/all113/old333/NEW300-112-188,
XP/credit/wear/UUID/phase limits remain strict. No current client PASS, image
inspection, stopped audit, final fresh284/140/76 or export is claimed.

**Latest actual compile3 PASS:** exit0/BUILD SUCCESSFUL35s; actual Fabric clientTest
classes, all21 required observers and **37 actual detached Java assertions**
(18custody/19motion) passed. ROOT checked all3960 inputs unchanged and archived
raw/UTF8 logs plus result/source postcheck JSON. Native runtime is still UNRUN.
Compiled expectation3 has136 resources/1101 available class outputs, fingerprint
`6ed5ef816cc5f77a4fe03cc86f72936b420d905c64e3ca352237f99cb9a55d6c`;
`.tmp/bore-custody-integration-check3-compiled-bindings.json` is NOT native proof.
No Gradle/game is now running. Final actor-geometry reader review is still the
native17 gate; do not launch until its freeze/pure positive+negative checks arrive.

**Current revision2, not yet native-ready:** independent reader follow-up1 arrived
SOURCE_FROZEN `1263ac60...`, original522/additive49 PASS reported by its author.
Custody/prefix/clocks/readiness/raw/native-binding gates are implemented; it found
one concrete remaining ambient evidence omission: actual actor AABBs at native
item trajectory/unload/rebind endpoints. Root did NOT waive geometry or launch.

Root byte-archived all11 schema1 sources before changes. New SOURCE_FROZEN
`.tmp/bore-root-custody-native-schema-freeze2.sha256` SHA`093eb844...` adds ONLY
passive actual actor-body/identity/status at every native pair/POST/final/lifecycle,
and the real latest pair on unload-before-POST. No standing-size reconstruction,
new gameplay, counter/Age/resource/coverage limit change. Rebind clears the old
owner's pair pointer; two extra actual-Java checks cover same-tick nonfabrication.
Exact contract: `.tmp/bore-root-ambient-geometry-v2.md`; lifecycle is explicit v2.

Actual compile3 launched shell`sh_14ecd908f001dw0EBpC1RlyzGH`, with3960 fresh frozen
inputs SHA`88948415...`; await actual completion notification, NO polling/other
Gradle/game/source edits. Compile2 below remains verified OLD-source evidence,
not evidence of this revision. Reader agent has renewed Python-only ownership to
close actual actor-geometry v2 with full positive/negative catalog fixtures; do
not read WIP or remove guards. Still no native17, no export or failed-world reuse.

**Latest verified:** actual Fabric clientTest compilation/check2 completed exit0,
BUILD SUCCESSFUL40s. Both actual Java state mains passed (18+17 assertions) and all
21 required private observer class/resources are installed. ROOT independently
verified all3960 source input hashes unchanged after completion. Raw UTF16 log and
NEW UTF8 copy/hash-bound result are `.tmp/bore-custody-integration-check2*`.
No game was run, no native preservation/constructor/physics PASS is inferred.

Root read-only classfile/NestMembers inspector additionally recorded actual
135 compiled binding resources and1100 available class outputs, expectation v2
fingerprint `acf62cb4ea204528b11134056d88d1feb41fef3d9b5cc4408e39eeecae99f489`.
`.tmp/bore-custody-integration-check2-compiled-bindings.json` is explicitly a
PRELAUNCH EXPECTATION, not runtime installation evidence. Actual native135 must
still independently report matching installed resources. No source edits or
other Gradle/game followed that successful compilation; reader source-only
follow-up still owns its Python files. Do not read WIP or launch native17 yet.

**Completed source preparation:** passive utility r3 SOURCE_FROZEN received; all40
receipt hashes verified and ROOT reran **106 detached checks PASS**. Actual
installed Frost focus is supported through the disclosed one-deep raw-field
known-codec tag encoding, not an effectful getter/general nested-stack allowance.
Only utility source changed (`ef0f0c3f...`); both witness proof/schema bodies remain
unchanged. `.tmp/bore-passive-stack-snapshot-review-r3/API.md` owns the limits.

ROOT froze **3960 actual source inputs** in
`.tmp/bore-custody-integration-check2-inputs.json` (SHA`f94bf2ee...`) and the exact
private check tools separately. Actual Fabric `clientTestClasses` +35 detached
Java state assertions + required21 observer manifest/class installation check
was launched by shell`sh_14eabef1f001f4zwbDnCTrFKSF`; await its completion
notification, **NO polling/other Gradle/game/source edits**. This is NOT a native
launch or PASS. Earlier compile-input hash enumeration attempt timed out before
creating an input file; no Gradle/game occurred for that attempt, nothing reused.

Reader follow-up `ses_eb1a06451ffe3O2TeIuVeDbBPz` owns Python only and is closing
the explicitly frozen native prefix/clock/readiness/ambient trajectory gates.
Do not read its WIP or remove its prior explicit acceptance blocker. No native17
until actual Fabric compile and independent-reader final freeze/review both pass.

User answered **«Давай»** to the exact <=200native/14wall custody-only excursion
whose necessity/temporary old-jar availability gaps were disclosed. Permission is
ONLY that finite departure->pickup<=60/4->actual recovery<=140/10 interval; no
render/simulation26 change, force loading, raw addition, restore/Age reset, broad
coverage waiver or failed-world reuse. Full20/333/72 is required again BEFORE any
normal R action resumes. Native16 remains actually REJECTED; no export.

Root schema/source freeze1 now exists:
`.tmp/bore-root-custody-native-schema-freeze1.sha256`, SHA
`b2bb6e963c9985b3f40746c6a2648e3d1a1c4d985713658e9a7f66cc6ab9fb36`
(11 root-owned sources/manifest/contract). It adds exact POST receipt prefixes,
actual native/wall phase origins/ages (entry/bootstrap charged), rich actual
20-owner/72-cell/18-critical-owner readiness, current native ticker bindings and
matrix counter continuity. Registration is explicitly NOT ticker execution.
Native source birth/cohort/death/item-motion schemas and v2 installed-class
binding are now declared in `.tmp/bore-root-custody-native-schema.md`.

Actual Java21 DIRECT compilation/execution of the FOUR detached deadline/motion
classes PASSED **18 custody +17 ambient state assertions**, source hashes unchanged;
`.tmp/bore-detached-state-check1/`. This is NOT whole Fabric linkage or gameplay.
Root read-only mapped-native javap confirms menu clicked ONE RETURN BCI129 and
actual server-packet invocation BCI222; `.tmp/bore-root-menu-native-bytecode1/`.

Independent reader core SOURCE_FROZEN received (manifest`f313918a...`): typed
SNBT+binary NBT/all7 reconciliation, exact native menu graph, owner-only OLD8/NEW
partition and strict retirement-v2 dispatch. ROOT reran **522 old +34 additive
pure checks PASS**. Its explicit acceptance BLOCKER remains until the follow-up
reader agent completes actual frozen prefix/clock/readiness/ambient trajectory
schemas; do NOT remove that guard. Agent owns Python only; root owns Java/compile.

Passive serializer r2 SOURCE_FROZEN received/ALL28 hashes verified (manifest
`d6e84649...`), avoiding live stack COW, map-view and mutable CustomData cache
writes. ROOT then checked actual ACCEPTED inventory support (read-only pinned
level.dat, NEW`.tmp/bore-accepted-inventory-component-profile1.json`) and found
inherited slot8 really contains `wand_focus` with plain `focus_frost1`. R2 would
correctly FAIL CLOSED on that supported-but-unimplemented snapshot path. A NEW
r3 source-only utility review is now authorized ONLY for passive raw-field known
FocusData serialization, not arbitrary nested items/codecs or new gameplay. No
native attempt was wasted and no stock/age/focus component was changed. Wait this
author's final freeze before actual Fabric compilation. Prior receipts stay
historical, never rewritten. All191 parent payloads were independently rechecked
before/after and still exact; no native17/game/Gradle has been launched.

ROOT has authored `BoreCustodyWindow` + detached checks, Sequence single rescue
and same planned chest-first ordering, Ledger exact native oldjar custody/slot7
owner classification (old8 separate from workshop333 and NEW300/112/188), and a
private native menu HEAD/RETURN graph. Only this proven actual original owner can
be excluded from NEW mass. Every available old BE remains fully checked; only
old-jar availability demand is conditional during this one explicit state.
Ordinary R/B/combined caps remain unchanged. Actor protection1/0 and Health20 are
strict, still NOT ordinary survival-damage evidence.

Inherited helper agent SOURCE_FROZEN received: helper `4ab843bb...`, new ItemEntity
tick mixin `6bd05790...`, API `.tmp/bore-inherited-item-witness-api.md`, manifest
`d01ec9c3...` verified by ROOT; **29 detached checks reran PASS**. Hooks are armed
before any clone tick; root forwards actual touch/removal before readiness gates,
binds during Ledger construction and checkpoints startup/bootstrap/POST/finish/
normal stop. All7 exact UUID/Age/full typed stacks, native tick/availability/unload
and genuine original pickup are independent from acquired13. Preventive expiry
margin128 and x960 stop480/hard512 are NOT freezes or extra time. All unseen
intervals remain independent stopped-audit obligations.

Native binding strengthened to installed resource/class bytes: new
`BoreNativeBinding` includes controllers/helpers/nest members (anonymous operations
too), all manifest hook class resources, manifest and parent metadata in sorted
path/NUL/hash/LF SHA256; private mixin classes are never Class.forName-loaded.
Actual Java/native linkage/application is still UNVERIFIED. Compile entry is NEW
`.tmp/bore-custody-integration-check.ps1`, to run ONLY after all Java freezes.

ROOT reran original **522 synthetic reader checks PASS**, unchanged legacy reader
bytes at that boundary; **18 detached proposed custody-reader checks PASS**.
Independent reader agent now OWNS active `.tmp/audit_bore_client.py`/codec/tests
and new validators (no candidate/world reads or native launch). Ambient helper
agent owns ONLY three new helper/mixin files and is not to be polled/read WIP.
No Gradle/game is running. Do not launch native17 until both freezes are reviewed,
all actual source schemas/strict readers integrated and Fabric clientTest compiles.
Root implementation contract `.tmp/bore-root-custody-native-schema.md` records
the additional menu/v2-binding schemas for independent review.

#### Historical blocker: native16 REJECTED by real inherited loose-jar loss

**Audit16 attempt2 ERROR is a REAL preservation failure, not another ID waiver.**
The exact generated-alias proof passed. Audit independently reached old554global/
113workshopBEs,20oldjars333/72candles, correct main gold106/phial1/cobble2, ink11,
new188stock/fuel1, then rejected a new loose physical item. ROOT read ONLY stopped
entity records to diagnose: candidate has NEW bone2 UUID66aab9f5... at1422.941,216,
378.264/Age1190, outside action areas, with NO source-linked ItemEntity birth
witness in the old harness. Native ledger separately records Skeleton2c6fbe64...
natural ON_FIRE tickDeath at1422.604,216,378.496, but proximity is NOT birth proof.
No ambient loot is retrospectively admitted or counted as raw/source acquisition.

More importantly, old item UUID`14d47d60-09eb-4949-a6fd-84dd78e8eea1` is actually
MISSING: inherited loose labelled Potentia8 jar, original1297.748853,204,361.524814,
Age4253. Its full ItemStack had maxstack1/JAR_LABELpotentia/block_entity_data
Amount8/facing3/Aspectpotentia. Native despawn is plausible but NOT exactly witnessed;
do not invent a cause or restore it. This actual loss alone makes16 UNACCEPTABLE
even though Bore itself worked, images/final284-140-76 passed. Original parent191
remains untouched; failed16/its genuine note/top/188/materials retained, NEVER
retry inputs. No further reader-only audit can promote a world missing old stock.

Archive`.tmp/bore-client16-audit-error2-inherited-loose-item/` retains ERROR2/sources2
and root's actual entity delta. Separate read-only diagnostic compared all11 named
NoAI parent cow UUID/type/position/Health/NoAI/name/persistence/invulnerability:
all exact. New entities by type: item1, villager20,cow2,iron_golem2,cat4,pig4; no new
nonliving projectile/XP in this stopped candidate. These diagnostics are NOT a
whole stopped-audit PASS. All seven inherited loose entities must remain protected.

Parallel authorized SOURCE-ONLY work (no active Java/Gradle/game):
- `ses_eb3a4c929ffeo3TEtdL8pfq6fy` owns NEW ignored
  `.tmp/bore-inherited-loose-custody-plan.md`/diagnostics. May read ONLY accepted
  parent+source for finite real sameUUID jar pickup/deposit in genuinely crafted R
  CHEST slot7 before remaining1747loadedticks; checks ALL7 loose-item ages because
  travel1297 may load old arrow/flesh near960. No grant/age reset/item teleport/
  restore/new raw/failedworld reads. Must disclose radius26 coverage gap constraints
  for far oldjars and finite real support geometry, not silently waive requirements.
- `ses_eb3a39245ffeWhhj4wswmd3eYj` owns NEW ignored
  `.tmp/bore-ambient-item-witness-stage/`+API, proposes actual native
  Entity.spawnAtLocation sourceHEAD/ADD/RETURN identity/fullstack/frame witness
  for outside-area unowned environmental mob loot (optional real Chicken egg
  branch). No active integration, no world/log/candidate reads, no gameplay calls
  or setters; read-only javap may verify actual1.21.1 descriptor. Root integration
  and independent reader support come only after SOURCE_FROZEN/review.

**Preservation-plan SOURCE_FROZEN received/root3hashes+read-only pure check passed:**
`.tmp/bore-inherited-loose-custody-plan.md`602ae576...,
diagnosticJSON1f03fd26..., checks85b011ae...; all191before/after still exact.
Actual supported contact1297.75,204,361.5 exists; proposed≤200native/14wall custody-
only excursion,≤60native/4wall pickup, actual20/333/72 readiness on return, native
slot7stow in same genuinely crafted planned chest, no extra raw/clock/resource.
BUT actor must be chunk81 while six old jars are in109/111/113/115 (>26), two more
at107(boundary). CURRENT unconditional20-live guard makes this trip impossible.
Explicit user permission is REQUIRED before implementing any narrowly bounded
honest availability-gap exception; no blanket jar waiver or claimed live20 during
unload. Existing source/candles/furnace/matrix contexts still require actual owners.
All7oldphysical protection remains: x960 arrow/flesh budgets4775/4843 permit only
bounded actual tick/unload accounting; origin egg has only298native ticks left,
spawn-ticket ticking status unproven. Do not visit origin/reset ages/infer loaded
status. Need passive native item tick/availability evidence plus stopped exact
custody. If broader rescue is needed, stop for further permission, not silent
scope expansion. Root is now asking the exact20-live exception decision; no
active source edits/native17 launch while permission is pending. Ambient witness
source-only stage owner continues independently; do not read its WIP.

No blind client17 rerun or failed16 repair. First solve real collateral custody
and honest native outside-loot provenance without loosening all113/all11/300-112-
188/XP/native timing or source rules. Current active code remains exact16 source;
284/140/76 and f70b32JAR are current VERIFIED-but-UNEXPORTED evidence, not acceptance.
Wait both source-only freezes without polling/WIP reads. Distribution still focal.

**Audit16 attempt1 returned ERROR, not acceptance:**
`.tmp/bore-client16-stopped-audit1.json` stops at strict world-config DataPacks
comparison. ROOT read exact stopped parent/candidate values: Disabled list remains
`[bundle,trade_rebalance]`; Enabled same order/list
`[vanilla,fabric,fabric-convention-tags-v2,fabric-gametest-api-v1,...]`, only final
`generated_e6c6440`->`generated_b81341a`. GameRules/WorldGenSettings/features/
difficulty/commands/GameType/Version all exact. No saved/config bytes edited.

ROOT traced the actual dependency binary, not guessed wildcard normalization:
Architectury Transformer5.2.87 `AbstractFakeMod.generateModId()` bytecode returns
`generated_ + UUID.randomUUID().toString().substring(0,7)`; GenerateFakeFabricMod
adds exact Generated Mod(Please Ignore)/1.0.0/fabric-loom:generated metadata.
Pinned runtime SHA`2fc96c2e36ad63a935c46221bee122559f43607b7dc34e49a62f0d6d6ecb81d7`.
Actual retained wrappers `.tmp/5660469607771309798.jar`(parent generated_e6c6440,
SHA804a6dd4...) and `.tmp/5025702970492148625.jar`(actual16 generated_b81341a,
SHA0c06c186...) have those exact metadata, no extra fields. Native16 startup log
confirms newID1.0.0. `.tmp/bore-client16-audit-error1-datapack/` archives ERROR1,
sources1, original371reader bytes, actual javap output, both wrapper binaries and
exact metadata/log/source SHA receipts. This is dev-wrapper identity evidence,
NOT a general datapack waiver or a rewrite of ERROR1. Candidate16 stays STOPPED.

Independent generated-alias reader SOURCE_FROZEN received/4exacthashes checked;
root reran **522pure synthetic reader checks** exit0 and actual three authorized
JAR provenance reader checks, archive`.tmp/bore-stopped-audit-generated-alias1/`.
Optional schema1 field`architectury_generated_fabric_alias` requires exact pinned
loader/class bytes, full wrapper/metadata SHA/default-profile IDs and ONE matching
Enabled ordinal4; every other Enabled/Disabled key/order stays exact. No blanket
name wildcard/gameplay config/protection/material waiver. Runtime installation is
ROOT-bound to actual native startup/source/compiled receipts, not metadata alone.
Author is DONE, no WIP/candidate read. Same stopped16 FULL byte map still matches
its original archive; no world/config/replay/input substitution occurred.

Root created NEW`.tmp/bore-client16-audit-sources2.json`, retaining actual native16
runID/binding/2222source inputs with fresh reader hashes and exact authorized alias
proof. All Java/GameTest/client writers confirmed stopped. Actual SAME-candidate
six-argument audit2 shell`sh_14c516c5f001eVIenj8uTbhNny` RUNNING, CREATE_NEW output
`.tmp/bore-client16-stopped-audit2.json`. Await actual notification, NO polling/
source/auditor/candidate edits/Java/game. ERROR1 remains archived and is not PASS;
no stopped candidate acceptance or export until all remaining checks actually pass.

Fresh FINAL3948active-input freeze`.tmp/bore-final-validate16-inputs.json` matches
native16 EXACTLY. Actual Fabric-only --rerun-tasks **284JUnit/140requiredFabric**
and **76Python77.545s** passed,0failures/errors/skips,3948unchangedinputs. Native
case manifest/prior cases, fresh packaging/notices/test isolation/access widener/
refmap and exact native remapped shadows checked. Actual all900compiled class
bytes used by16 are IDENTICAL to the fresh rebuild, separately archived comparison.
`.tmp/bore-final-validated16/` contains full logs/reports/summaries and UNEXPORTED
fresh10,841,529byte JAR SHA`f70b32f4c1698bf3247afd1bb1525f69cdc341e045ff605d4b3ed6db8196fa8f`.
Same bytes as old13 is actual fresh reproducibility, not reuse of old verification:
these current284/140/76 actually ran against16's3948inputs. All191 parent and all3
accepted distribution mods remain unchanged. Both shells completed; no Java/game
currently executing. Actual stopped audit2 now runs as described above; final
284/140/76 and full10PNG gates are already verified, but independent all113/all11/
resources/XP/native/UUID audit remains required before any artifact/export acceptance.

**Latest native16 actually PASSED exit0**, run
`client-smoke-1b8d3871-88e7-49e7-9af1-daf1ab9e7f40`, NEW accepted-focal copy
`bore-smoke-ed474cc3-e32b-44ba-9e76-30e5e520c0b8`. Main SCRIPT_PASSED/all10PNG and
tail STOPPED_UNINSPECTED/errorNONE, actual11418contiguousPOSTs72..11489. Whole
genuine route: ONE note/13combines/**10**funded edits, base900+900,9pick crafts,
52native cooks/300units,112draws+8ingredients/188retained, original label returned,
earned/installed top, original relay genuine mine/pickup/reinstall,36/9eligible
harvests/wear2, real physical UUIDs ae11848b.../64741680... and native pickups.
RawPerditio112, gold106/phial1/cobble2, XP unchanged; finite fuel4ignited+1remaining
of5, burn0. These are actual native observations, not yet independent stopped PASS.
Normal-stop null-client owner fix and Gradle real prelaunch-copy/postcheck path
actually executed successfully; BUILD SUCCESSFUL10m4s. No time/fuel/resources grant.

Root verified actual191initial-copy proof,191immutable-parent payloads and3948run
source hashes after normal process shutdown (no remaining Java/game writers).
Actual installed private manifest bytes match source and native declaration;
all17 required mixin class files present,900actual compiled class-output hashes
and full source freeze bound in archive. `.tmp/bore-client16-complete-pending-audit/`
contains log/exit/main/fulltail/freeze/initialcopyproof/full stopped-world hashes/
all10PNGcopies+hashes/compiled installation receipts. No accepted save/record/JAR
export yet, no failure-world input reuse.

ROOT actually OPENED all10 exact16PNG174–183. New note geometry retained; funded
palette/edit/learned book/base preview/earned item/Bore Width5Speed1 seen.179now
shows wide diagonal source-jar row;182/183show WHOLE stationary original-style
base/top/extended head/glass/vortex and green-white beam, no prior top cropping.
Byte/hash-bound observations in`root-exact10-PNG-inspection.json`: bounded visual
presentation PASS only, not fullFX/audio/multiplayer/survival or numerical proof.

Root created actual explicit `.tmp/bore-client16-audit-sources1.json`: real runID,
native installed-manifest SHA/binding, ALL common/Fabric source/resources from
actual3948freeze plus current auditor/codec/pure test/NBT helper =2222sourcefiles.
Independent six-explicit-argument stopped auditor now RUNNING shell
`sh_14c41d5b6001SrkgcuMPJJpWcs`, outputCREATE_NEW
`.tmp/bore-client16-stopped-audit1.json`. Await notification; NO polling/source/
auditor edits/candidate writes or any Java/game while it reads the stopped save.
Full284JUnit/140Fabric/76Python and fresh remapped artifact checks still pending.
Export remains CLOSED until independent all113/all11/resources/XP/UUID audit and
those final checks pass. Distribution remains accepted focal0104dc..., no export.

**Latest native client15:** run`client-smoke-859fa884-abc9-415c-80f2-c0e0d1429e19`,
NEW focal-copy world`bore-smoke-47a351da-c55d-4a26-85db-33adc1f9504b`. Main actual
SCRIPT_PASSED/all10PNG: genuine ONE primary note/13combines/9edits/base900+900/
nine picks/52cooks/300units/112native draws+8ingredients/188retained/earned top/
same relay mine-pickup-reinstall/installed paid old focus+operating pick/two actual
native harvests with36/9eligible countdowns, wears0->1->2, output UUIDs
32438427.../cc87a57e..., real native pickups. Final gold106/phial1/cobble2,
XP0/123/float32same, inherited actor1/0flags, five finite coal, rawPerditio112.
This is genuine STORY completion, NOT acceptance: Gradle exit1 and full tail
FAILED_OR_INCOMPLETE. At last11854POST, just after main completion/disconnect,
`phaseNativeAge()` called `nativeTick()` through cleared CLIENT integrated-server
pointer and latched NPE. Actual bound server continued/stopped normally with
11783contiguousPOSTs72..11854; actual stopServer HEAD followed same11854tick.
Root did NOT waive failed tail or promote that world. Frozen3948inputs matched
before edits;191accepted parent payloads unchanged. Archive
`.tmp/bore-client15-story-complete-failed-normal-stop/` retains log/exit/freeze/
main/fulltail/full stopped-world hashes/all10PNGcopies+hashes/native receipt.
NEVER retry input: genuine learned flags/top/300-112-188 stay in that world.

Root fixed only actual native clock owner selection: after actor binding read
`owner.server.getTickCount()` (same real server) even if the client clears its
pointer; reject a DIFFERENT current server, reject no owner before binding. No
cached tick, fabricated POST, guard skip or time/stop status rewrite. Added5pure
identity cases. This still needs actual fresh native normal-stop execution.

Gradle also failed AFTER native shutdown with unknown `sourceWorld`: source/target
locals existed only inside prepare closure, referenced inside post screenshot loop.
That loop also wrongly attempted post-play candidate==initial191 bytes. Agent
SOURCE_FROZEN received/3hashes checked/root7source-schema tests passed; archived
`.tmp/bore-gradle-copy-authored1/`. Exact fresh191copy verification now belongs
AFTERcopy/BEFORElaunch, CREATE_NEW proof bound to unique world/parent manifest;
postcheck verifies immutable ACCEPTED parent and same captured proof, NOT played
candidate equality. Existing screenshot/tail/status gates remain. Root narrowed
lock exclusion further: ONLY root session.lock and only constant UTF8snowman bytes
may be ignored, never a nested same-name payload. Seven schema tests reran exit0.
Independent owner is DONE. New root ignored Groovy behavior task evaluates ACTUAL
helper closures over191DECLARED TEXT fixtures, replacing ONLY pinned digest for
that fixture; no real accepted/candidate/save/prepare/smoke action is used.

Root opened ALL10exact15PNGs and archived honest observations in
`root-exact10-PNG-inspection.json`. R/book/base/earned top/Bore Width5Speed1 UI are
seen;179wrongly framed altar instead of11row;182/183show cropped original-style
base/glass/vortex and183beam fragment, NOT accepted full model/beam framing.
Root added actual client teleport-position acknowledgement (two stable real client
ticks INSIDE original120cameraTTL), then3real aim ticks before world photos. Row
photo now uses existing eastern1485.5,216,377.5 floor (1487isAIR and NOT used), aimed
1479.5,216.5,370.5. Model/beam photos use existing southeast1618.5,216,383.5 floor,
aimed1615.5,217.6,382; return to original lever reach before OFF. SavedSTONE+AIR
verified for proposals; all live collision/loaded/entity/support guards still apply.
No FOV/render/material/platform alteration or new acquisition. Framing remains
unverified until actual next PNG. Check14 actually compiled current Java and passed
16052motion/142profile assertions; combined check failed only because its separate
GroovyShell probe lacked the GradleException default import. Archive
`.tmp/bore-client-motion-check14-partial/` preserves those exact bytes/results and
failure; it was NOT a native game or whole combined PASS. Ignored probe import
fixed; active3948inputs unchanged. Fresh freeze
`.tmp/bore-client-motion-check15-inputs.json` +private-tools hashfile: current
compile/16052motion/142profile assertions and **12actual Groovy helper checks over
191declared text fixtures** actually passed exit0; archived
`.tmp/bore-client-motion-checked15/`. No native world/action was used by that check.
Then root froze`.tmp/bore-client16-inputs.json` and launched NEW accepted-focal copy
native16 shell`sh_14c35ade7001DAAJj4kyqfehHT` RUNNING. Await actual notification;
NO polling/otherJava/active-source edits/save reads. Root review note
`.tmp/bore-gradle-copy-root-review.md`. All15 earned outputs remain in retained
unaccepted15world, never inputs/refills. Auditor371pure freeze unchanged. Full
stopped audit/all113/all11/final284-140-76/artifact/export remain pending; mods focal.

**Latest native client14:** run`client-smoke-1f602f63-8777-4a30-95fd-cb6960a12564`,
exit1, NEW focal-copy world`bore-smoke-e2361fdd-d465-435b-b5ee-0a220c20a09e`.
Again actual ONE note/13combines/9edits, base/nine picks/ALL52nativecooks/300units.
Label attached on SECOND original alembic(index1), removed9839. Its genuine ADD
UUID64e78314... at1465.741,218.407,377.769 had real upward/lateral native velocity.
The old contact task immediately failed when its one set of proposed supported
cameras could not reach that airborne state. Native physics continued untouched;
the SAME UUID actually playerTouch-picked at9847 by the unchanged player at
1465.5,216,379.5. No contact camera was applied for that label. Failure was already
latched and stays FAILED; do NOT promote despite the later real pickup. Final
9781contiguousPOSTs71..9851, no179/new wide view/infusion/top/digs. All3948 input
hashes and191parent payloads matched before edits. Archive
`.tmp/bore-client14-failed-top-alembic-contact/` retains log/exit/freeze/main/tail/
stopped-world/5partialPNGhashes. That directory name is NOT evidence of a top-cube
failure: new`actual-airborne-label-contact-diagnosis.json` explicitly corrects it
to SECOND cube/airborne premature-failure +actual native9847contact. Failed14 and
its earned300/note/materials are NEVER retry inputs.

Root fixed only private supported-contact polling: inside the SAME original220
native-tick operation bound, use the actual current ItemEntity point INSIDE its
queued native server read, not a stale prior-POST point. If no safe supported
camera is currently available, perform no movement/item action and wait for real
native physics; do not instantly declare permanent impossibility. A queued read
also recognizes an already witnessed sameUUID pickup before checking owner.
At most one pending read, at most one successful player teleport, no gameplay
packet/output retry, item/velocity/pickup-delay/world setter, direct pickup call,
deadline reset or extension. Actual native playerTouch RETURN remains the only
completion acknowledgement. Camera candidates are finite125 (5x5localoffsets,
feet216..220);220 additionally covers the real existing third-alembic top if ever
needed, not a new support block or a claim it caused14. Every candidate now also
requires actual loaded chunks before native collision/support/other-living checks.

Fresh FINAL current compile +16052motion/**137profile assertions** actually passed;
3948unchangedinputs archived`.tmp/bore-client-motion-checked13/`. Then root froze
`.tmp/bore-client15-inputs.json` and launched NEW accepted-focal copy client15 shell
`sh_14c1ea3a0001Q3oDIQt6KNTU7v` RUNNING. Await actual notification; no polling/other
Java/active-source edits/save reads. Auditor final371pure freeze remains unchanged;
no candidate accepted/audited/exported. All failed earned materials remain saved,
never refilled/reused. Mods still focal; all10PNG/stopped/fullfinal gates pending.

**Latest native client13:** run`client-smoke-775890e1-a92c-46e6-8422-354e61526485`,
exit1, NEW focal-copy world`bore-smoke-dc3bbf92-7e6e-4a31-bd1e-2714487b6156`.
Corrected source servicing actually passed:52native cooks(8picks/4pistons/24redstone/
16phials), exactly300original-derived units physically retained in11jars, furnace
and all3alembics empty/filterless. Exact original label bound once on SECOND cube
at2779 and returned at9881 with one physical/pending owner; sameUUIDpickup retained.
Final burn909/coal0 (five finite coal reconciled), no fuel/time/essence refill.
One genuinely learned note/13combines/9edits and native paid base/9pick crafts
preceded this acquisition. Run reached pre-infusion admission at9914, NOT infusion,
top/Bore/twoharvests.74..9916/9843contiguousPOSTs retained;3948frozeninputs matched
before edits and all191accepted parent payloads remain unchanged. Archive
`.tmp/bore-client13-failed-infusion-admission-envelope/` includes log/exit/freeze/
main/fulltail/full stopped-world hashes/6PNGcopies+hashes. Failedworld NEVER reused.

Concrete failure: that one admission loop directly compared saved jar NBT against
live tag, bypassing the existing narrow native outer-chunk-envelope helper. Root
decoded ALL20 actual final old-jar rows against bound parent expectations: ONLY
outer `keepPacked:0b` is absent live; every stock/filter/component field is exact.
Archive's`actual20-oldjar-envelope-diagnosis.json` records this actual diagnostic,
not a complete candidate audit. Admission now uses `loadedOwnerExpected` just like
normal preservation: only proven outer BYTEfalse may be absent; true/wrong-type/
nested/stock/filter changes remain strict. New detached checks cover all20 bound
jar payloads and changed Amount/filter/nested fields. Compile11 actually passed
16052motion/**131geometry/route/label/service/header assertions**,3948unchangedinputs
archived`.tmp/bore-client-motion-checked11/`.

Root opened exact13 PNG179: genuine filled jars render, but old camera showed only
cropped central jars and retained the last placement yaw. Corrected photo fixture
to existing clear floor1475.5,216,384.5, aimed at rowcenter1475.5,216.5,370.5.
Saved supportSTONE/headroomAIR verified (nearer382 has original candles and was
NOT used/cleared). No materials/platform/FOV spoof. Full row visibility still needs
actual next PNG inspection. Fresh FINAL current compile +16052motion/131profile
assertions actually passed;3948unchangedinputs archived
`.tmp/bore-client-motion-checked12/`. Then root froze`.tmp/bore-client14-inputs.json`
and launched NEW accepted-focal copy client14 shell`sh_14c10d222001w5CNaNqddZ77Ni`
RUNNING. Await completion notification, no polling/otherJava/active-source edits/
save reads. Failed13's actual300 and genuine note/materials remain in failedworld,
never copied as14 inputs. No note reroll/seed selection or resource restoration.

Independent final dynamic3/pending label auditor SOURCE_FROZEN4hashes checked,
root reran ALL**371pure synthetic reader checks** exit0, archive
`.tmp/bore-stopped-audit-label-final1/`. Actual current API
`.tmp/bore-stopped-label-final-api.md` reviewed; chosen-original3 index/actual
selected inventory slot/real typed pending stacks/actual liveUUID/ADD/touch remain
strict, without future-event credit or filter migration. Owner is DONE, no WIP
writer/candidate read. Fresh inside-broad ambient births/endpoints remain explicit
unsupported reader cases, not inferred permission. Full10PNG/stopped audit/fresh
284/140/76/artifact verification and export remain pending; mods still focal.

**Latest native client12:** run`client-smoke-9cd7c17b-e271-400a-a01d-b9a473930332`,
exit1, NEW focal-copy world`bore-smoke-12ad7852-be42-482d-9432-6dadb87643e3`.
Genuine R/base/acquisition replayed; actual ONE note/13combines/9edits, then label
custody correction passed: actual metal filter bound once at2742 on SECOND original
alembic, no observer family-loss failure. Native six picks cooked at2673/2871/
3074/3616/4336/5056,108units retained; seventh real pick remains in furnace with
native Cook77/Vis31, coal3+burn84. At5672 all-eight-input3200window failed. Actual
70..5673/5604contiguousPOST tail, source3948hashes and parent191 checked unchanged.
Archive`.tmp/bore-client12-failed-pick-input-deadline/` retains log/exit/main/tail/
freeze/stopped-world hashes/5partialPNGcopies+hashes. NEVER retry input; no lost
payments or refills, no300/112/top/digs/clientacceptance.

Concrete cause from real POSTs: old service performed75 one-unit jar extractions,
leaving all3 native buffers empty almost every40tick pulse. Original/native code's
first pass refills EVERY occupied typed buffer; only second pass initializes ONE
empty buffer. Greedy clearing reduced the real throughput to about1/40 instead of
parallel occupied channels, backing up50capacity and stalling later pick cooks.
Root rechecked original TileAlchemyFurnace211..253; no gameplay change needed.

Private View now exposes already-read actual furnace tag stock. New detached
source-service chooser keeps typed positive buffers; drains actual full32, an
exhausted UNFILTERED type only when a missing actual furnace type lacks an eligible
empty channel, or final partial buffers ONLY after actual batch cooks and empty
furnace. It never predicts random tag order, sets a type/tick/fuel, adds bellows,
extends3200/3600/9200/12000/16000 or adds coal. New pure negatives cover capacities/
invalid observations and early/greedy clearing. Fresh current compile plus
**16052motion/50geometry-route-label-service assertions** actually passed;
3948unchangedinputs archived`.tmp/bore-client-motion-checked10/`. Then root froze
`.tmp/bore-client13-inputs.json` and launched NEW focal-copy client13 shell
`sh_14c020102001O6Rpx8uxQUR5Jx` RUNNING. Await actual completion notification;
no polling/other Java/active-source edits/save reads. Full final checks/export
remain outstanding; this scheduling arithmetic is NOT native throughput proof.

Independent teleport auditor SOURCE_FROZEN4hashes checked, root actually reran all
**290pure reader checks** and archived`.tmp/bore-stopped-audit-teleport1/` without
candidate audit. Followup ignored-only owner aligns its mistakenly first-alembic-
only label rule with the actual dynamic first-positive-metal choice among3 and
binds the existing typed`actual_pending_queue` LIST OF STACK SNBT strings. Strict
single binding/no migration, actual sameUUID ADD/touch, full original blank return,
all11/all113/300-112-188 remain. NEW API`.tmp/bore-stopped-label-final-api.md` pending;
do not read WIP. Fresh inside-broad ambient births/endpoints remain explicitly
unsupported by that reader, not silently broadened. No candidate audit/export.

**Latest native client11:** run`client-smoke-7bd9a1dc-88d7-4d28-ac9b-81c3c1200e6e`
exit1, NEW focal-copy world`bore-smoke-c5823ae9-a13f-4703-83ad-8039a4190120`.
Four-pad/direct-TP route actually passed; genuine9scans/13combines/ONE note/9paid
edits/base900+900/foundation/scaffold/staged ingredients/lever/nine crafted picks
reached source distillation. At native2652 first pick really cooked18units. At2697
actual selected original blank label disappeared from inventory and original
alembic1 changed filter""->metallum with amount1/tagmetallum. Generic inherited
component custody ignored that real installed owner and falsely failed. Actual
stop tail71..2698/2628contiguousPOSTs retained; all191 accepted parent hashes still
match. `.tmp/bore-client11-failed-installed-label-custody/` archives freeze/log/
exit/main/fulltail/full stopped-world hashes and5 exact PNG copies/hashes (not newly
pixel-inspected). Solved note/base/first18units are retained ONLY in FAILED world,
NEVER retry input; no300/112/top/digs or accepted candidate.

Root checked original BlockMetalDevice590..618 and native ItemJarLabel paths.
Private Ledger now accounts actual SINGLE metallum filter as temporary custody of
the exact original plain blank, separate from loose/menu/drop/pending owners.
Binding needs prior real positive-metal buffer+same selected original label;
return needs empty buffer, actual preceding Shift/vacuos16jar and all300generated.
Adjacent POST edges are honestly labelled NOT direct interact RETURNs. No synthetic
stack is added to physicalStacks, no filter/item/world setter or generic family
waiver. Duplicate/multiple/foreign filters/altered stacks/loss fail; final gate
requires exactly1bind+1return and original blank physically back. Root also removed
obsolete9travel cells from final `cobbleCells()` (earlier route/count/concrete edits
alone had left that final retained-block helper stale), with detached regression.

Fresh FINAL current compile +16052motion/**33geometry/route/label assertions**
actually passed;3948unchangedinputs archived`.tmp/bore-client-motion-checked9/`.
Then root froze`.tmp/bore-client12-inputs.json` and launched NEW accepted-focal
copy client12 shell`sh_14bf7a13d001kE0rvSOA3lJQGz` RUNNING. Await notification;
no polling/other Java/active-source edits/save reads. Independent ignored-only
auditor owner continues4pad/ordinary-ambient/label custody source alignment;
wait freeze, no candidate read by owner. Root actually opened exact run11 PNG174–178
and recorded byte/hash-bound partial observations in archive's new
`root-exact-partial-PNG-inspection.json`: funded palette/incomplete note/first paid
edit/learned book/base preview only, NOT installed Bore/model/beam acceptance.
Compile8/16052motion/12geometry remains historical before label/helper changes.
Mods/export/accepted records unchanged; all failed earned materials remain saved.

**Latest user steering: teleport smoke-test travel instead of building access bridges.**
Client10 actually exited1, run `client-smoke-21841a16-1ce1-4bf6-82ef-9ee8eba8cc3b`,
failed world `bore-smoke-ab11c581-355b-4583-9c82-0ee7c0ada085`. Root inspected
main/full normal-stop tail and archived source freeze/log/exit/result/tail/full
stopped-world hashes in `.tmp/bore-client10-failed-ordinary-creeper-motion/`.
Actual source hashes matched before edits and all191 accepted-parent payloads
still match. No note request or PNG;3 funded combinations had occurred. It failed
because ordinary unnamed/nonpersistent/normal-AI Creeper UUID0bd1b2ec... naturally
moved with Health20/other flags unchanged INSIDE a broad observation rectangle;
the old observer incorrectly placed it in strict protectedLiving. It is NOT one
of the11 parent cows. No successful research/top/dig/acceptance is inferred.

Private observer classification now protects named/NoAI/persistent identities,
not ordinary normal-AI nonpersistent wildlife solely due to a broad rectangle.
ALL11 cow validation, persistent-mobile native witnesses, concrete stock/owner+6
despawn boundaries, outside-broad environmental-death limits, native removal
attribution and item/XP custody remain mandatory. No AI/location/health or other
gameplay setters were added. Unknown forbidden removals still fail normally.

User's travel correction removes9 unnecessary travel bridge placements and their
9 raw cobbles: R13->4. Only4 real pickup/support pads remain; B foundation/scaffold
and every actual native placement/ray/support check remain. All local travel now
uses explicitly logged supported player TELEPORT fixtures, including onto the
actually placed scaffold. Final cobble2 and all resource costs remain unchanged.
Root corrected the runtime conservation constant to4 too. Read-only accepted-save
palette +detached support arithmetic checked10 sequential camera proposals and
all9 removed cells are original AIR, `.tmp/bore-teleport-route-preflight1.json`;
this is NOT live placement/collision/entity-clearance proof.

Current3948inputs frozen `.tmp/bore-client-motion-check8-inputs.json`; actual
compile/motion/geometry shell `sh_14becfed60013p6d4OcOOp0ts5` RUNNING. No other
Java/game, active-source edits or output polling. Independent ignored-only auditor
owner `ses_eb414c76affeED21zzOGzPFUSI` aligns exact4pad/ambient schema with pure
tests only, no world/log/candidate reads; wait for SOURCE_FROZEN before integration.
Old admission SOURCE_FROZEN was received and is being archived by that owner.
Client11 not launched yet. Failed10 is NEVER a retry input. No new export/mods
change, all10 exact PNGs/stopped audit/final fresh full checks remain outstanding.

**Latest: client9 real unload/rebind false-order failure retained; client10 RUNNING.**
Actual client9 `client-smoke-138dc56d-7a44-456a-9924-009160292d59` exit1 at B furnace
fuel-menu camera(tick2209), after genuine primary/base/foundation/scaffold/staged
center+8components/lever. Known cow1106 UUID443a91e4... was first actually observed
1961, genuinely UNLOADED_TO_CHUNK2123, and reappeared at2209 with the same trusted
coordinates/Health6/flags. Old `preserveEntities` ran BEFORE the coverage-row
formatter's real UUID rebind, so it latched failure despite a valid later same-POST
binding. Exact lifecycle is archived, not guessed. Failed world
`bore-smoke-9ee2c28e-ee5d-4689-a1f1-c75dcda9b832` retained, NEVER retry input;
`.tmp/bore-client9-failed-rebind-order/` contains log/exit/freeze/main/full tail/
full stopped-world hashes and5 PNG hashes(copied, not newly visually accepted).
All191 accepted parent hashes still match. No distillation/infusion/top/dig PASS.

Root moved actual current-UUID observation to the START of POST, before invariant
checks and snapshots. parentCowRows is purely a formatter again. Candidates are
trusted parent11 plus already protected living UUIDs; only actual native Mob owners
are observed, using existing exact properties and real prior-UNLOAD requirement
for seen replacements. Named/NoAI/all11 positions remain strict; a non-Mob/wrong
owner or unwitnessed replacement still fails. Origin remains explicitly
CURRENT_UUID_LOOKUP_NOT_ADD_OR_TICK, not a fabricated callback/tick/RETURN.

Root also closed a disclosed AUDIT-logging gap without changing gameplay: the
existing actual registered Mob ADD callback now serializes a first native
observation ONLY if its original cohort was unprotected ambient. The event is
`actual registered native Mob ADD first observation`, origin
ACTUAL_REGISTERED_ENTITY_ADD_CALLBACK, actual body bounds, actual living-state
typed reference and0entity mutations. This is not an entity spawn or supplied
eligibility token. Protected admissions remain in their existing strict channels.
It lets the independent reader verify real admission of future natural ambient
spawns rather than trusting a current ambient Boolean or inventing late frames.

Fresh FINAL current client compilation +16052motion/12geometry assertions passed,
**3948 unchanged inputs**, archive`.tmp/bore-client-motion-checked7/`. Compiler6
logs are historical BEFORE the later metadata-only admission additions; do not
relabel6 as final current verification. Root froze `.tmp/bore-client10-inputs.json`
then launched NEW accepted-focal copy client10, shell
**sh_14be00ca7001RChVJRcWWFvwhg**. Await notification; NO polling/other Java/active
source edits/save reads. All former failed worlds/materials/solved notes retained,
never copied or refilled. No observed note failure/easy-RNG selection was used.

B's mobile/concrete reader revision was hash-verified and archived unchanged in
`.tmp/bore-stopped-audit-mobile1/`; root actually reran its64mobile+37ambient
synthetic checks and prior pure checks exit0, NO candidate audit. B now solely
owns IGNORED auditor/codec/pure test plus NEW`.tmp/bore-stopped-admission-api.md`,
binding the earlier POST registration order and exact new native Mob ADD admission
fields/body geometry. No active Java/source/world/log reads or execution by B.
Root will review final hashes/source envelope after its freeze. Actual all10 PNG,
stopped all113/all11/resource/XP/ownership audit and final full checks still needed
before any export; mods remain accepted focal0104dc....


**Client8 failed actual menu focus check; bounded owned-HWND menu recovery added; client9 RUNNING.**
Actual client8 `client-smoke-34b9d3ff-a7eb-475c-ac60-a30c3dccc59a` exit1 during
native scanner craft in R (tick539). Old inventory input guard saw windowInactive;
the old exception did NOT record Win32 HWND/cause, so do not invent a user/process
or OS reason. Thirteen R raw receipts, no note/combination/capture. Failed world
`bore-smoke-4fd94f2e-4a93-411a-983a-f74a4157be8a` and pending real workbench/cursor
custody retained; no restored/granted input. Archive
`.tmp/bore-client8-failed-menu-foreground/` includes source/log/result/tail/exit/
full stopped-world hashes, all191 parent payloads still unchanged. Never retry input.

User had explicitly authorized automatic activation of ONLY owned Minecraft HWND.
Root added new private BoreOwnedForeground read helper (Win32 foreground, GLFW
flag, verified current-JVM window PID) without altering accepted FocalNativeShift.
After genuine native Shift preflight, before each NEXT menu operation only, lost
focus may trigger the existing authorized owned-HWND activation and wait for real
Win32+GLFW stable3ticks. Cap25wall/500clientticks is WITHIN all existing operation/
phase/combined deadlines; none is reset/extended. Step/index/ack does not advance
or resend packets while waiting. World/source/fuel/AI continue genuinely. Active
scan/mining/Shift loss remains a hard failure; no automatic channel or note retry.
Inventory packet entry also requires actual current HWND ownership and records
native focus facts on failure. No game focus flag, key query, clock, resource,
entity, mode or knowledge spoof; cleanup still releases only our native modifiers.

Fresh current client compile +16052motion/12geometry pure checks passed,
`.tmp/bore-client-motion-checked5/`; actual focus recovery is not established by
those detached tests. Root froze `.tmp/bore-client9-inputs.json` and launched NEW
accepted-focal copy client9, shell **sh_14bcdb8a40017jxd3cV5ekE1uf**. Await actual
notification; NO polling/other Java/source edits/save reads. B still edits ONLY
ignored future audit schema, not active inputs or worlds. No complete accepted
candidate, all10 inspected PNGs, independent stopped Bore audit, final checks or
export. Mods and accepted parent unchanged; prior successful notes remain saved
in failed6/7 as historical genuine acquisitions, never copied into this attempt.


**Client7 passed corrected placements; genuine vanilla despawn boundary fixed; client8 RUNNING.**
Actual client7 `client-smoke-22a35167-0d85-431b-a93b-53aa93f401ea` exit1. It
again genuinely solved ONE natural note (13combines/9edits), paid base, placed
foundation/base/scaffold, then staged center piston before an unrelated ambient
spider's native checkDespawn. Actual spider c0a763d7... was baseline-loaded and
NOT in protectedLiving. At1599.46,216,371.98 it lay inside the BROAD research
observation buffer, but far from concrete stock/work cells and player at altar
1471.5,216,375.7. Real ordered Entity.setRemoved→remove→LivingEntity.remove→discard
→Mob.checkDespawn→ServerLevel/EntityTickList→MinecraftServer.tickServer spine,
DISCARDED and damageNONE were retained, no AI freeze/cancellation/restoration.
Archive `.tmp/bore-client7-failed-native-spider-despawn/`, failed world
`bore-smoke-a1eaefa7-77c7-4cad-9207-5aa9b9900d22`, exact source hash check +191
parent match. Five PNGs copied/hash-bound but NOT newly visually accepted. No
distillation/top/harvest or complete Bore acceptance is inferred; never retry input.

Root corrected ONLY private observation classification, not production gameplay:
already registered UNPROTECTED ambient + actual unnamed/nonNoAI/nonpersistent +
no actor damage + exact vanilla checkDespawn spine may naturally DISCARDED inside
a broad buffer ONLY outside concrete protected cells and actual owner body+6.
Those cells include all113 parent BEs, old TC blocks, original supports, all new
R/B devices/pedestals/11jars/13bridges and the reserved Bore lane. Environmental
tickDeath still requires outside broad regions; protected persistent/named/NoAI/
all11 destruction, items/XP and native ownership remain strict. No simulated
despawn, modifier/AI/health/location setter or removal cancellation. Removal
events now carry actual body/owner bounds, actual flags and both broad/concrete
scope facts so the independent audit need not trust an alleged outside boolean.

New tiny client-test geometry helper/check-main uses real Minecraft AABB arithmetic
but detached fixtures only. Fresh current compile +16052motion assertions +
**12 concrete-footprint geometry assertions** actually passed,
`.tmp/bore-client-motion-checked4/`. This is NOT native despawn/game acceptance.
Root froze `.tmp/bore-client8-inputs.json`, then launched NEW accepted-focal copy
client8, shell **sh_14bc6572a00102yO2He303zSk3**. Await notification; no polling/
other Java/active-source edits/save reads. B remains solely IGNORED audit author,
now integrating exact mobile rows AND independently verified concrete-despawn
body/cell geometry with negative tests; no active sources/worlds/Java touched.
No completed candidate audit, all10 inspected PNGs, final fresh checks or export.
Mods remain0104dc...; every failed save and its earned materials is retained.


**Client6 genuinely solved research and paid for base, then failed unsupported camera; client7 RUNNING.**
Actual client6 `client-smoke-f9def8df-7317-4e53-bbe4-91b049932dbf` exit1 after
genuine R completion:9 first scans,13 native combinations,ONE RNG-issued note
hash45104eec92d00d37721bf42da674a53c2f2881fcdca9f6db9534f89ecf21e5db,
9 acknowledged paid edits, inkdamage10, actual note consumption and ONLY ARCANEBORE
added. No buffer combos, flags/pools/XP/ink grants or seed/reroll. Actual saved
NBT independently confirms researchComplete+ARCANEBORE,8 item IDs+1node ID, no
unconsumed note and one actual earned base item. Payer gold/silverwood before/after
has Aer4070→3170 and Ordo1270→370, other4 unchanged; XP/health/protection unchanged.
Both finite R+B raw batches(26family receipts) delivered; no source-pick distill/
infusion/top/harvest success yet. Source parent191 hashes unchanged.

Root actually OPENED exact174..178 native PNGs, archived with byte/hash receipts:
funded palette, original4-anchor incomplete note, actual first paid edit, green
learned Bore book node, native base recipe preview. This is5 partial views, NOT
installed Bore/model/beam parity or all10 visual acceptance. Full failed run,
tail/log/main/inputs/exit, stopped-world hashes and images in
`.tmp/bore-client6-failed-foundation-camera/`; world
`bore-smoke-4c16d49c-a839-457b-8a58-2dc376ce1d5d` retained and NEVER retry input.
The solved research/base evidence is partial historical evidence, not a promoted
accepted save or permission to bypass the same acquisition in a fresh attempt.

Concrete geometry error: proposed camera x1613.5/feet216 is unsupported AIR; the
saved stone floor begins at x1614,y215,z378..386. Root read only the accepted
parent's small native palette slice, no game/world edits. Corrected all7 such B
camera proposals to3 finite points: foundation1614.5,216,381.5; scaffold approach
1614.5,216,382.5 (north adjacent, NOT inside already placed scaffold); GUI/lever/
model1614.5,216,380.5. Actual floor/headroom verified from saved data; every live
camera still checks loaded collision/support/no other entity, and every placement
uses a real first BLOCK ray/face/reach. No new stone/floor fixture or raw input.
New points declared in report; current client compiled +pure16052 assertions
passed `.tmp/bore-client-motion-checked3/`. This does not certify live new rays.

Root froze `.tmp/bore-client7-inputs.json` and launched NEW accepted-focal copy,
shell **sh_14bbc022d001u9cYc4aaxa8wWi**. Await notification; no polling/other Java/
active-source edits/save reads. The prior note was SUCCESSFULLY solved; retry is
for a deterministic camera bug, not selection of easier RNG. No prior failed
world or learned flag/finished note is copied. The whole finite genuine prelude
must run anew and each attempt still issues at mostONE natural note; all observed
notes/results are retained, no RNG rewinds or cherry-picking. An actually unsolved
note cannot trigger reroll-until-easy. B's ignored-only mobile-audit integration
continues independently; root does not read its WIP files or execute world audits.
Full13(284/139/76), later focused140 and compile/pure checks remain separately
source-bound. No Bore acceptance, all10 PNG/stopped audit, export or mods change.


**Latest: genuine mobile callbacks passed in client5; late-NoAI read binding fixed; client6 RUNNING.**
C mobile SOURCE_FROZEN087f44ca... exact3 artifact hashes checked; root wired
actual ServerLevel.tickNonPassenger(Entity) HEAD/every RETURN in required private
BoreMobileTickObserverMixin (13th native hook class), with actual world/Entity
arguments and caught failure, no mutable/removed-owner RETURN filter. No gameplay
cancellation, movement or protection setter. Current code compiled; pure motion
checker actually passed **16052 assertions /16000 synthetic pairs**, bounded
latest receipt, `.tmp/bore-client-motion-checked1/` (not native AI proof).

Actual client5 `client-smoke-d06228c2-eab0-48a7-a999-a3bcf518a10f` exit1. It passed
bootstrap/header/actor flags/real foreground+Shift preflight and delivered the
13 declared R raw families once; the first bridge placement was in progress.
**160 actual paired mobile rows** passed real caller-spine/owner/counter/properties
checks, a genuine limited native observation result—not full client acceptance.
Then known parent NoAI cow1267/UUID2e5ede5c... became available through native UUID
lookup before separate ADD/checkDespawn observation; the old registration assertion
failed. Note/scans/captures0; no original RNG note generated or rerolled. XP/
knowledge/health/protection flags remained exact parent, but inventory legitimately
contains the actual R delivery and is NOT falsely called parent-identical.
Archive `.tmp/bore-client5-failed-late-NoAI-registration/`, owned failed world
`bore-smoke-5b8a64f9-d085-41a3-bc92-9571106a3358` retained/NEVER a retry input.

Root corrected only parentCowRows' read registration: when actual current native
UUID owner is found but unregistered, observe/validate that actual Mob through
the existing exact parent11 identity/coordinates/Health6/NoAI/name/protection gate.
Previously seen replacements still require genuine old-owner unload. It emits
`actual SERVER_POST current UUID owner registration`, explicit origin
CURRENT_UUID_LOOKUP_NOT_ADD_OR_TICK—NEVER a fabricated ADD or tick witness. Strict
NoAI position checks and all11/all113 stopped obligations remain unchanged. First
patch context failed on List generic text and changed nothing; exact narrow
context applied normally. Fresh compile/pure16052 checks passed again,
`.tmp/bore-client-motion-checked2/`; actual new registration path still pending.

Root froze `.tmp/bore-client6-inputs.json` then launched SIXTH native client on a
NEW accepted-focal copy, shell **sh_14bb2aaa2001Wh7Ok4yEuGrtV1**. Await notification;
NO polling/other Java/active input edits/save reads. Previous5 issued ZERO notes;
client5's paid R input is retained in its FAILED world, not refilled/reused.

B now owns ONLY ignored audit_bore_client.py/bore_audit_codec.py plus NEW
`.tmp/bore-stopped-mobile-api.md`, adding independent per-POST real-mobile body/
stable-property/final saved-position validation to the frozen stopped reader.
Original audit source/API preserved `.tmp/bore-stopped-audit-authored1/`. No active
Java/source edits or world reads allowed to B; Python pure checks only. Root will
review new hashes/schema and use actual current client source snapshot/binding
for any eventual stopped audit. No candidate audit, pixels or export accepted.

**Independent stopped auditor received and pure checks verified; no candidate audit.**
B SOURCE_FROZEN: `.tmp/audit_bore_client.py`75e3a61a0ac41a1feeaf0a1fe9ac766960d4440eab1c6e5882ec5ed7b289b204,
`.tmp/bore_audit_codec.py`f85cc9a0275672296d6357f5677aa0e7f8aca48f89e03addf486caaa0e6e3eee,
API`.tmp/bore-stopped-audit-api.md`7dc7ac3240cfe828a9e95d353c9ed2ddd83f9a256ba762bae563fbbdad387506.
Root checked all3 hashes and actually ran `python3 -B .tmp/audit_bore_client.py
--self-test` exit0: PURE_HELPER_SELF_TEST_OK, no game/world/candidate audit.

Invocation needs SIX explicit args(world,run-id,result,tail,sources,out). Do not
auto-select latest/reuse a failed save. Source envelope needs real run/binding/
manifest SHA and files_sha256 for all common/Fabric active source inputs plus
the3 exact audit/codec/read_native_nbt Python files; derive from the actual client
freeze and compiled manifest, not a newer checkout. It validates unchanged inputs
at both boundaries and CREATE_NEW output. Current decoder understands schema1
typed refs and actual emitted producer fields, actor inherited protection1/0,
narrow outer BYTE-false keepPacked, source-dependent300/112/188/52cooks/9picks,
native research/XP/UUID/credit/epoch/prune and all11/all113 stopped inputs. It
neither installs expectations nor treats progress summaries as full native data.
Actual Pixel/installed-hook/compilation/launcher-wall/final-artifact checks remain
root's separate responsibility; the auditor explicitly does not infer them.

Root must integrate C's forthcoming mobile-witness schema in the stopped reader
if needed, with new hashes/source envelope and pure negative checks—never merely
waive ordinary persistent entity health/identity or all11 exact NoAI positions.
C is now sole active source writer; B is DONE, no other Java/game or agent writer.
No candidate success exists; there is nothing legitimate to label audited/PASS.

**Client4 retained; no resources/note used; native persistent-AI motion fix underway.**
Actual client4 `client-smoke-b1299020-8df5-4ce8-b4c7-daa3c0a644f0` exit1, world
`bore-smoke-ec7f3fa4-9ad2-43d6-86e0-cbbf3a48f5b7`. Startup/20/333/72/NBT/actor
flags passed, then strict POSITION equality failed on unnamed persistent pig
`a71d2542-9b91-4a1e-b7ce-fc26a236bf00` near1648,-60,299, FAR outside the owned
high workshop. Its NoAIfalse/persistenttrue/Health10/type/name/invulnerablefalse
were identical across all4 actual failed POSTs; only native AI position advanced
~0.04–0.09 blocks. Root did not move/freeze/restore it. Inventory/XP/knowledge/
Health20/actor flags exactly match parent stopped NBT; raw/note/capture counts0.
Archive `.tmp/bore-client4-failed-persistent-AI-motion/` includes full typed-ref
tail/log/main/source/exit/stopped-world hashes and decoded actual pig samples.
Source3942-input snapshot still matched at archival; all191 parent hashes match.
Failed world retained, never retry input or acceptance. No Java/game active.

Concrete overly strict observer policy: persistence alone protects all fields,
including position, despite normal AI being active. Root decision is NOT to
reclassify this animal as disposable ambient, waive its health/destruction, freeze
AI, restore position or alter user entities. Only protected UNNAMED normal-AI
persistent mobs outside all owned/old-block/BE footprints may move under an exact
current-owner/native-world-tick HEAD/RETURN witness. POST must match that actual
return; unexplained movement/replacement/health/name/NoAI/flags changes still fail.
All11 named NoAI cows retain EXACT coordinates/Health6/flags; their destruction
remains globally forbidden. Unload gaps remain explicitly stopped-audit-required.

C `ses_eb5a049dbffedGPGIsw7xHvIGn` now exclusively edits ACTIVE Ledger plus NEW
narrow motion arithmetic/check helper if needed, and ignored
`.tmp/bore-client-mobile-fix-api.md`. NO Java/build/game allowed until SOURCE_FROZEN.
Root owns controller/native mixin/bootstrap integration and future execution;
do not overwrite C's active ledger or duplicate its witness design. B remains
independent IGNORED-only stopped audit author. No other active writer or Java.
After C freeze: review/hash/integrate required real entity-tick pair, compile/
detached checks, then a NEW accepted-focal copy (not any failed world). First4
attempts issued ZERO notes, so one-note/no-reroll rule is still intact. All
previous test records remain exact historical inputs, no client/pixels/export.

**Latest: inherited actor protection preserved; client4 now RUNNING.**
Client3 `client-smoke-01c16378-778d-413a-9757-d2ee75825662` exit1 after bootstrap
and BE equality succeeded. It recorded3 genuine contiguous FAILED native POSTs
(79..81), but no raw delivery/note/capture. Concrete failed guard incorrectly
required !Entity.isInvulnerable. Read-only SOURCE and STOPPED candidate NBT prove
accepted Player653 has Entity.Invulnerable=1, abilities.invulnerable=0, Survival0,
Health20; all those, Inventory/XP/knowledge stayed exactly unchanged. Failure and
world `bore-smoke-791ebcbc-b179-4685-8954-5b2e63f75ffd` are retained in
`.tmp/bore-client3-failed-inherited-flag/` and never used as retry input.

Root corrected only the client-test ledger: capture/enforce exact inherited
entity/ability flags, require accepted baseline1/0 explicitly, record real flags
at every POST and state the limitation in baseline output. No invulnerability,
health, NoAI, mode or ability setter was added/called. Existing protected actor
is NOT evidence of ordinary survival damage vulnerability; all recipe/resource/
XP/mining authority and protected-NPC gates remain unchanged. No vanilla survival
or remote-multiplayer completion is inferred. Fresh client compilation and all
detached16k/576+6 metadata checks passed, archive`.tmp/bore-client-evidence-checked4/`.
This is compilation/detached evidence, NOT live flag-preservation acceptance.

Root froze `.tmp/bore-client4-inputs.json` and launched fresh client4, shell
**sh_14b9b82670011BEOO77jmkQQWZ**. All three prior attempts issued ZERO notes and
delivered ZERO raw fixtures; no paid-note reroll or spent-resource retry occurred.
Client4 starts only from the accepted focal parent, not any failed candidate.
Await completion notification; no polling/other Java/source edits/save reads.
B's ignored-only independent stopped-audit author received the actual inherited
flag1/0 and exact false-outer-keepPacked schema facts, not a request for a waiver.
Full13(284/139/76) and focused140 remain historical; no full/new client/PNG/export
claim is made for these later observer changes. Mods remain accepted0104dc....

**Client2 warm-up succeeded; exact chunk-envelope comparison fixed; client3 RUNNING.**
Actual client2 `client-smoke-b807ceaa-298c-4a45-9073-48f509f1b5a5` exit1. Its real
bootstrap tick77 saw20 jars/333units/72candles/3local cows and zero chunk requests.
Baseline then refused unrelated old bench1458,216,377; raw/note/capture counts
still ZERO. Independent stopped NBT again matched exact parent Inventory/XP/
knowledge; that bench's SAVED tag also matched parent exactly. Failed world
`bore-smoke-1fc5d53f-b78b-4a39-a6ec-383eb9db4c8d` is retained, never reused.
Archive `.tmp/bore-client2-failed-baseline/` includes main/tail/log/inputs/exit and
stopped-world hashes. Early failure tail explicitly says BASELINE_NOT_ESTABLISHED
and NO_POST_WITNESS; no synthetic native timeline was added.

Concrete cause is now NATIVE-proven, not guessed: vanilla chunk save adds the
outer byte keepPacked=false; a live BE's saveWithFullMetadata does not. Root's
new actual chunk-vs-live SERVER case held real iron2 and confirmed ONLY that
outer envelope difference, with input tags/inventory unmodified. Focused
`bore-runtime-gametest14` **140/140 actual required Fabric PASS**0fail/errors/skips,
archive `.tmp/bore-runtime-gametest14-passed/`. All old139 retained. Root normalizes
ONLY expected outer byte-false keepPacked when absent from actual live tag, on
a copy AFTER current owner/type/state checks. True/wrong-typed/nested values and
all stock/components/other metadata remain strict. Parent metadata/save untouched.
Unexpected future comparisons now retain both compared NBT values in the report.

Fresh client helper compilation plus detached16000/576 evidence checks AND six
actual-classpath113/11/72 metadata/header boundary assertions passed:
`.tmp/bore-client-evidence-checked3/`. No world/client success is inferred from
that process, and no new JUnit/Python/JAR full PASS is attributed to GameTestOnly.
Production remains unchanged since full13 f70b32...; final fresh checks still
mandatory after client/harness settles. Root froze `.tmp/bore-client3-inputs.json`
and launched THIRD client attempt on a NEW accepted-focal copy, shell
**sh_14b925621001bKvV3SXYdKmQHc**. Earlier two issued ZERO notes, so one-actual-note/
no-reroll policy remains intact. Await notification, no polling/other Java/active
input edits/save reads while live. B's ignored-only stopped audit author remains
independent. No PNG acceptance, final stopped Bore audit or export yet.

**Latest client state: client1 failed before raw/note; fresh client2 RUNNING.**
Actual client1 exit1, script `client-smoke-18f8218c-0656-4372-a0ad-5aabc2a38a34`,
owned world `bore-smoke-090b6754-8fab-45fc-9ae6-b5bdee6499bf`. Native boot and
client/server heartbeats reached, but baseline aggregated20/333/72 before all
ordinary asynchronous chunks had become visible. Log proves normal view/sim26
was set and failure occurred ~4 server heartbeats after login; it does NOT give
the missing-count details, so do not invent a specific missing jar/stock result.
Zero screenshots/note requests, no raw delivery/preflight action was reached.
Independent stopped NBT confirmed exact parent Player653 Inventory, XP tuple and
thaumcraft:player_knowledge. All191 accepted parent files still match. Failure
archived `.tmp/bore-client1-failed-bootstrap/` with log/input/exit/main report/
owned options and complete stopped failed-world hash manifest. Failed world is
retained, NEVER a retry source or accepted result. No normal tail existed because
baseline failed before prior listener registration; no tail proof was invented.

Root changed only2 client-test helpers: bounded raw already-loaded readiness
probe (known20 jar positions/333contents,72 exact candle states,3 local cow UUIDs)
before ONE baseline. It makes zero chunk requests/tickets. If all20 are present
but contents !=333, fail immediately with actual count, never wait for a refill.
Bootstrap cap600native/60wall is WITHIN existing combined16000/960, not an
extension. Last actual probe counts/missing positions are in small progress and
full report; baseline error also reports real aggregate values. Normal-stop hook
registers early so a pre-baseline failure now gets an explicitly NO_POST_WITNESS
tail, not fabricated POSTs. Gameplay observers still start only after baseline.

Fresh current client compilation + detached16000-row checks passed again,
`.tmp/bore-client-evidence-checked2/`; that is not a readiness-in-world PASS or new
284/139/76/JAR result. Production code unchanged; full13 remains exact historical
evidence. Root froze `.tmp/bore-client2-inputs.json`, then launched fresh client2,
shell **sh_14b8a85d1001vrh4awACWXv5Mo**, using a NEW accepted-focal copy (not client1).
Await notification; NO polling/other Java/active source writes/save access while
live. One actual issued note rule remains untouched (client1 issued ZERO).
No client completion, PNG acceptance, stopped Bore audit or export yet.

**Latest full13 PASS; FIRST native Bore client attempt now RUNNING.**
`bore-client-validate13` actually **284 JUnit /139 required Fabric /76 Python
104.493s**,0failures/errors/skips,**3942 unchanged inputs**. Archive
`.tmp/bore-client-validated13/` has fresh XML/JUnit/logs/exit/input/inspection/
Python/static-remap and source-bound unexported10,841,529-byte f70b32f4... JAR.
All191 parent payload files and all3 distribution mods unchanged; exact source
recipes/assets/refmap/8shadow fields/notices/ZIP/test isolation/single AW checked.
This full13 covers current client-helper compilation, not actual native hooks,
client gameplay or pixels. Earlier checkpoints remain source-bound historical.

Root recorded `.tmp/bore-client1-inputs.json` then launched FIRST actual client
attempt `bore-client1`, shell `sh_14b8500dc001FQz1ytswy8qhT4` using the prepared
single-process guard wrapper. Await notification; do NOT poll/launch another
Gradle/game/edit active inputs or touch candidate/source saves while live.
The run owns a fresh bore-smoke-UUID copy of accepted focal only, Player653 exact
UUID, ordinary render/simulation26, native foreground/Shift gate, predeclared
R+B raw quantities, ONE actually issued note/no seed/reroll, native GUI/recipes/
300distillation/112infusion/188retention and two real Bore targets. R4000ticks/
240wall, B12000/720, combined16000/960, ten exact174–183 native captures, no
resource topups/autoextensions/retry or accepted input rewrite. Actual game
completion/PNG/stopped audit remains UNPROVEN until its real outputs arrive.

On completion FIRST archive actual log/exit/main-result/tail/owned-world path
and source/parent hashes before any next run. Failed/private attempts are NEVER
retry inputs or export proof. Inspect every exact PNG if produced, then run the
independent stopped audit (B still ignored-only authoring). Do not export even if
Gradle reports exit0: actual normal-stop tail + all113BE/all11cow/finite material/
research/XP/output ownership audit and final fresh source checks are mandatory.

**Latest: ledger source integrated; detached evidence PASS; full13 RUNNING.**
C SOURCE_FROZEN44028b6... delivered five exact hash-bound artifacts (Ledger,
ParentModel, Evidence, EvidenceChecks and isolated metadata resource); root
checked all five hashes before integration. Root installed the required actual
TileNode.serverTick HEAD/every RETURN pair and MinecraftServer.stopServer HEAD
bridge, both read-only/caught-failure observers. Current controller POST list uses
BoreClientEvidence.log(); full stopped-tail streaming and small progressSnapshot
are in place. Twelve required private hook classes compiled successfully.

The old20 jars extend to x1851/chunk115, not merely local alchemy91..92. Root set
normal finite render/simulation26 (24 geometric+2 margin) BEFORE launch, keeping
strict actual loaded20/333/72 and source/furnace ticking checks. No special
tickets/observer loads or75-chunk all11-live fiction. Far all11/all113 coverage
still requires independent stopped audit, with actual live subsets/gaps reported.

Actual `bore-client-evidence-check1` exit0, current client helpers compiled and
**16000 synthetic POST rows /576 immutable payload versions** passed lossless
immediate/retained/JSON replay, aliases/types/birth/owner/reset/missing-return/
terminal checks. Stream2,121,693bytes SHA2569f4b29a4... vs115,175,000raw fixture
bytes; archive `.tmp/bore-client-evidence-checked1/`. This is NOT actual game
heap/native callbacks/NPC/BE/pixel proof. All191 accepted parent payload hashes
rechecked unchanged. No client game/world copy has been launched.

Full `bore-client-validate13` shell`sh_14b81053e001D52t2x20VqlWOU` now RUNNING;
Python13 shell`sh_14b8110b7001iZPkbsNzDrHNl6`, log`.tmp/bore-client-assets13.log`.
Active inputs FROZEN; no other Java/game, active writer or agent source edits.
Await notifications, do NOT poll/duplicate processes/edit inputs. Inspect fresh
full13 via `.tmp/inspect_bore_runtime.py bore-client-validate13 284 139`, static
remap helper with run name, parse Python76 actual elapsed and archive all fresh
outputs before any game. Full13 is pre-client validation, not client acceptance.

B's stopped-audit script is the sole outstanding agent, IGNORED-only. Root sent
the final typed-ref schema (tail.report.payload_versions and exact current POST
fields); metadata table is expectations, never actual future-world reads. Main
result contains small child progress only; full evidence is in the actual stopped
tail. Native client wrapper prepared, still NOT EXECUTED. Mods unchanged.

**Current gate: passive ledger corrections in progress; no Java/game active.**
C review `.tmp/bore-client-ledger-review.md`07b42e81... found five real P1s plus
unbounded repeated-payload allocation. The all11 cows span chunks25..101, not one
local scene: live radius12 cannot truthfully observe all11 (75–76 geometric radius
would be required). Root explicitly preserves the historical accepted contract:
ALL11 trusted parent identities/Health6/NoAI/names/positions MUST pass independent
STOPPED audit; live checks cover exactly the genuinely loaded subset (initially
three named local UUIDs) plus every later native reactivation, with explicit
uncovered/unloaded intervals and same-parent UUID rebinding. No observer force
loads, player/NPC relocation to activate remote audits, AI freeze, restoration,
silent11→3 claim or stopped-audit waiver is allowed. Same distinction for113
parent BEs: actual local action footprint must be genuinely loaded/ticking;
remote metadata preservation remains an all113 stopped audit obligation.

C `ses_eb5a049dbffedGPGIsw7xHvIGn` exclusively edits ACTIVE BoreClientLedger.java
and NEW narrow BoreClientParentModel/Evidence helpers/resources/check-main, plus
ignored `.tmp/bore-client-ledger-fix-api.md`. No builds/execution allowed until
SOURCE_FROZEN. Fixes include raw loaded-chunk/health reads, genuine owner/lifetime
RETURN witness and bounded birth POST, native unload/rebind, witnessed exact
node modifier/prune edges, and lossless immutable payload-version references
while retaining EVERY actual POST. No production gameplay/authority changes.

Root meanwhile changed ONLY its owned controller/bootstrap/build/script paths:
- POST log now ConcurrentLinkedQueue (lossless append, no copied array each tick).
- Book result contains child progressSnapshot ONLY, not the growing full report;
  the normal-stop tail remains authoritative. Both root JSON and full stopped
  tail stream to Writer; tail still CREATE_NEW, no full JSON String duplication.
- Bore prepare task refuses any existing destination and verifies all191 copied
  payload hashes as well as the immutable source before starting the game.
- Ignored `.tmp/bore-client-run.ps1` prepared but NOT EXECUTED. Requires a recorded
  input snapshot/new log+exit and refuses a concurrent owned Gradle/game process;
  never kills unrelated processes. Actual client still must await corrected
  ledger review/fresh source-bound checks/native bindings.

B `ses_eb5c7203dffeqNezD1e4wIF71e` independently authors ONLY ignored
`.tmp/audit_bore_client.py` / `.tmp/bore-stopped-audit-api.md`, a future read-only
all113BE/all11cow/material/knowledge/XP/UUID stopped-NBT audit. No future save
exists yet; no native/world/audit PASS may be inferred from authoring. Root owns
final schema integration after C freeze; B does not read C's WIP implementation.
All prior full11/focused139/compile3 records are historical to their frozen
inputs; newer observer/report/profile changes have not compiled or run yet.
Distribution0104dc... and accepted focal parent remain unmodified.

**Latest root fixes / checks (2026-10-18), still NO CLIENT GAME:**
B action review `.tmp/bore-client-actions-review.md` d5d2a0a2... found3 concreteP1s.
Root addressed all3 in ACTIVE test helpers:
1. Keep actual vacuos16 jar(number11) carried until metal-filter removal; use
   real held-jar Shift dispatch, recover blank label UUID, then place that SAME
   vessel. Never change the inherited offhand apple or add a vessel/clear essence.
2. Wait for actual GUI combinePressedUntil AND native acknowledgement before
   another combine. Read-only deadline observation; no deadline/RNG/clock write
   or direct backend retry. GUI adapter also refuses an early caller defensively.
3. Queued own server actions now recheck failure/inert/closed/stopped INSIDE the
   scheduled lambda; closed is volatile. Raw deliveries recheck before/within
   insertion, camera/contact fixtures before their own teleport. No cancellation
   of genuine server ticks/resources/pickup/removal, refund or restoration.

Added one meaningful SERVER regression with disposable declared1-unit input:
actual label binding/drain, empty MAIN+Shift+occupied OFF really returns PASS;
the actual retained filled jar then removes only the filter, preserving full jar
and apple and emitting exactly one blank label. This is a native branch fixture,
NOT live client acquisition or a new source-essentia grant to Player653.
`bore-runtime-gametest12` ACTUAL **139/139 required Fabric PASS**0fail/errors/skips;
archive `.tmp/bore-runtime-gametest12-passed/`. Old138 retained. No new JUnit/JAR/
Python/client result attributed to this focused run. Fresh `bore-client-compile3`
also exit0, **3936 unchanged inputs**, archive `.tmp/bore-client-compiled3/`.
The first evidence-archiving attempt timed out before creating its directory;
retry120s revalidated unchanged inputs and completed normally, no overwritten data.

C's READONLY ledger/loaded coverage/heap-volume review remains pending, source/
note ownership unchanged below. No Java/game/source writer active. Do not launch
until concrete ledger/profile gates are resolved, then fresh final source-bound
checks and finite native client. Full11(284/138/76) remains historical; current
139focused/compile3 do not certify native client hooks, screenshots or stop audit.

**2026-10-18: unified client source promoted, compiled; NO GAME LAUNCH.**
A SOURCE_FROZEN41717b3032a26c5c03a03df5088954a136baebd1cb74b3866abe0c469db63663
received at `.tmp/bore-client-sequence-api.md`; root promoted only two NEW test
files BoreClientSequence(e23c2480... baseline)/BoreClientLedger(b3ebbeb2...). Root
installed ten NEW client-test-only native hook mixins, exact custom-mod/native
descriptors, unchanged older hooks, mandatory manifest binding and real heartbeat
requirements. Only owned hotbar-scroll hook can cancel UI input; lifecycle,
tick, infusion, item/XP observers do not cancel/mutate server outcomes.

Root wired explicit BookClientSmoke BORE branch BEFORE any old full-story grants,
check/tick/normal-close/capture-future events/failure/report handling; GuiArcaneBore
is in the same capture allowlist as request validation. Outer1080s startup/story
watchdog does NOT extend child960s/R240+B720 caps. Added Fabric bore scope selecting
only accepted focal record,191-file manifest hash/path/payload verification before
copy, exact Player653 username/UUID, unique bore-smoke UUID, ten exact capture
names, mandatory normal-stop tail verification. Owned radius12 is PROVISIONAL
pending actual loaded coverage review; no world has been copied by these tasks.

Actual `bore-client-compile1` failed two String/long errors: Op's inherited timer
field `first` shadowed the combine aspect parameter inside anonymous operations,
also making RP checks wrong. Root renamed ONLY timer to firstNativeTick. Fresh
`bore-client-compile2` **exit0**, common/Fabric/clientTest/gameTest classes compile
under `-PclientSmokeScope=bore`, **3935 unchanged inputs**. No native tests, JUnit,
new remapped JAR, game or screenshots are inferred. Configuration-only task also
passed exact username/UUID/no duplicate args and all ten compiled required hook
metadata checks. Archive `.tmp/bore-client-compiled2/` has both compile diagnostics,
fresh2 input/log/exit, profile inspection and summary. All191 source payload hashes
still match; `mods/` unchanged. Actual offline UUID was independently recomputed.

READONLY reviews pending, no active source writers/Java/game:
- B `ses_eb5c7203dffeqNezD1e4wIF71e`: frozen controller action review, note ONLY
  `.tmp/bore-client-actions-review.md`; root's timer-shadow fix supersedes that
  frozen-source defect if reported. No production/stage/output edits or execution.
- C `ses_eb5a049dbffedGPGIsw7xHvIGn`: frozen passive ledger preservation/falsePASS/
  loaded-coverage/memory review, note ONLY `.tmp/bore-client-ledger-review.md`.
  Workshop chunks1465↔1611 are9 apart, so old radius8 was insufficient for all
  simultaneous BE reads. Need concrete11NoAI coverage and bounded heap/report
  volume before launch; do not force-load from an observer or waive missing data.
Root must address concrete review findings, then fresh compile/full checks and
actual finite client run, exact PNG inspection and independent stopped audit.
Full11 remains historical3920-input284/138/76 runtime evidence, not new client
acceptance. Never export f70b... or relabel accepted focal data from compile alone.

**Research prelude materials resolved; client implementation now staged.**
Read-only primary route `a8ba6d2ddef2d2ba2d36e4311bd6fa105cba53f1a51db9a15611ae88dc86aa1e`
at `.tmp/bore-primary-research-route.md` proves a finite genuine scan/combine path
with13 raw Phase-R types, native apparatus crafts,13 returnable access cobbles,
one real existing unscanned node1591, six distinct shard ITEM-ID scans, preserved
resonator+strongest wand scans and13+at most6 paid combinations. No missing native
API/discovery blocker found. Main can return22occupied/14free by real chest/table
storage. Gold108+2research-2scanner-2top=106; old empty phial2-1scribe+16sources-
16distilled=1, not2. All phases' finite inputs are declared before the attempt;
B raw delivery occurs only after actual primary learning, never after a failure.

One paper and one100-ink native-crafted scribe suffice: one issuance +at most29
real edits =30 ink. **Original generator can physically disconnect fixed cells.**
The actual issued note must be inspected/solved without rerolls, seeds, free RP
or state writes. If that one note is genuinely unsolvable or bounded search fails,
retain real note/resources and STOP before B input. No production generator
change or all-RNG guarantee is claimed. Source counterexample/enumeration is
read-only, not an actual issued note or an observed live failure.

Root reviewed/promoted only new test helpers `BoreResearchNotePlan.java`f59015ad...
and `BoreResearchGuiActions.java`76c066b5...; added `BoreResearchNotePlanChecks.java`.
Actual Java21 compilation and detached checks PASSED via ignored init-task
`:fabric:verifyBoreNotePlanner`, **32 synthetic boards ×2 budget envelopes +8
boundary assertions**,0.8139385s,**3923 unchanged inputs**. Evidence/source copies:
`.tmp/bore-note-plan-checked3/` (earlier16/32 checks archived1/2). Finite-envelope
plans used9–10 cells in these samples; not minimum/all-topology/live-RP proof.
No server/client world, issued item, player RP/ink or completion was created by
that process. GUI input helper compiled but GUI actions remain UNEXECUTED.
All191 parent payload hashes rechecked before current changes; mods unchanged.
Full11 now historical to3920 exact inputs; the3 new files are client-test-only,
not new full JUnit/Fabric/Python results or a replacement production-JAR claim.

A `ses_eb5a0fd33ffeqCL3WeAeqLxoMx` now sole ignored source author:
`.tmp/bore-client-sequence-stage/**` / `.tmp/bore-client-sequence-api.md`, one full
native research→acquisition→slow/boosted Bore sequence, with exact API matching
existing Focal pattern. No active writes/Java/game; root owns bootstrap/profile/
native hook mixins/compile/run/audit. Plan first from actual RP; if necessary only
declared real motus combinations (≤6) then replan the SAME issued note with actual
new funds. Never pass prospective RP as earned funding. One proposed combined
hard ceiling16000native/960wall, research4000/240 and B12000/720, no extensions.
Ten planned captures174–183, but no client profile/whole sequence promoted yet.

Root inspected actual Loom RunConfig/Settings bytecode read-only (no game) while
preparing profile glue. No default username was found there; new profile should
explicitly pass Player653/its UUID and assert actual identity before fixtures.
Do not let another random dev profile replace the acquired state. Actual saved
world source must remain the accepted focal manifest; no failed/intermediate
world is a retry input. Await sequence SOURCE_FROZEN review before client launch.

**Client acquisition gate discovered; genuine research prelude now in progress.**
B route review SOURCE_FROZEN `a1fd2f17f55807193381a10d038a9749f3a0b5615f8295cbaaa9121352afd15f`
at `.tmp/bore-client-finite-route-review.md` reconciles exact inventory/fuel/jars/
real stone anchors/instability and191 unchanged parent payload hashes. Material
route feasible, but actual Player653 (`13848433-7be7-350d-8b4a-c038819c2260`) lacks
ARCANEBORE, its4 discovered tags, paper/ink/Thaumometer. No research flag, solved
note, RP/vis/XP/essentia grant is authorized or used. Continue with bounded genuine
scan/combine/issued-note/table-edit/learn prelude; no need to ask for a bypass.

Corrections from actual stopped save: gold stacks60+48 (108), existing alchemy
furnace has3 coal and THREE empty alembics, zero bellows. Proposed new fuel2coal,
sticks19, empty source phials16(or17 explicitly surplus), extra raw cobble1 merges
with old4. Whole ordinary jar use extracts arbitrary positive alembic amounts,
avoiding unremovable4-unit phial residues. Eleven vessels hold300 before infusion;
188 collateral remains after112 payment. Base payer loses900Aer/900Ordo; account
any research-tool gold separately rather than falsely keep108→106 for everything.
Floor y215 provides genuine anchors: foundation1615,216,381; scaffold1614,216,383;
base/top1615,217/218,381. Five cobble cover foundation/scaffold/lever/two targets.
Potency1 cobble slow/boosted countdown36/9; place second target ONLYafter credit,
not before its cached slow selection. Remove old relay after acquisition but
BEFORE top placement. Detailed positions/source order/finite proposed12000native/
720wall ceiling in review are NOT launch approval or client proof. Research
surplus inventory may change slot fit and must be resolved before final budget.

Current independent ignored-only owners:
- B `ses_eb5c7203dffeqNezD1e4wIF71e`: `.tmp/bore-primary-research-route.md`, actual
  scanner/scribe recipes, finite earned RP/discovery/material path for this parent.
  Read-only saved-input/source investigation; no active edits or Java/game.
- A `ses_eb5a0fd33ffeqCL3WeAeqLxoMx`: `.tmp/bore-note-planner-stage/**` /
  `.tmp/bore-note-planner-api.md`, detached client-only immutable note-route plan.
  No server completion/state/items/pools, no RNG/seed/reroll, no production helper.
- Root authored ignored, uncompiled GUI-input adapter
  `.tmp/bore-root-client-src/.../BoreResearchGuiActions.java` SHA25676c066b5...:
  actual screen palette arrows/right-click pair selection/combine button/hex
  drag-release, with real session/revision/pending checks. Reflection READS only
  custom presentation fields; no direct backend packets or private-field writes.
  Caller still needs foreground/native Shift preflight and authoritative ack/
  ink/RP ledgers. Whole client sequence/build profile not yet authored/promoted.

Active inputs still match full11; no Java/game active, no new client launch,
accepted-record rewrite or export. Resume after read-only research route and
planner freeze/review, then finalize finite prelude+acquisition and client harness.

`bore-runtime-validate11` **actually passed 284 JUnit /138 required Fabric /
76 Python96.361s**,0failures/errors/skips,**3920 unchanged inputs**. Archive
`.tmp/bore-runtime-validated11/` includes all fresh XML/JUnit/log/input/exit/
inspection/Python/static-remap evidence and unexported source-bound JAR.
JAR10,841,529bytes SHA256
`f70b32f4c1698bf3247afd1bb1525f69cdc341e045ff605d4b3ed6db8196fa8f`
is byte-identical to full9 (intervening additions were test-only), but full11 has
its OWN fresh artifact/time/input/check evidence. All original and new required
cases included; source mixins plus exact Loom refmap transformation,8 shadow
fields, notices, ZIP, test isolation, single intermediary AW and recipes/assets
checked. All191 accepted-parent payload files and all3 distribution mods unchanged.

No Java/game remains active. Full11 is NOT client/pixel/stopped-Bore acceptance or
permission to replace `mods/`. Root now moving into native-client harness design;
current `BookClientSmoke`/`fabric/build.gradle` only support through `focal`, no
Bore profile or client sequence is authored yet. B's read-only finite route review
is still active/ignored-only; root has not polled it or launched any client. Need
approve actual raw budget/research/legitimate placement anchors before a bounded
genuine acquisition run. Continue automatically; no new user confirmation needed.

### Historical Bore integration notes — superseded by full11 state above

The entries below retain earlier source/diagnostic history. Their “running”,
“uncompiled”, owner assignments and expected counts describe those earlier
checkpoints, **not the current execution state**. Accepted focal records remain
immutable throughout. Resume from the full11 state above, not an older pending run.

**Protection matrix promoted and actually passed:**
`bore-runtime-gametest10` **138/138 required Fabric**,0failures/errors/skips,
unchanged inputs. Archive `.tmp/bore-runtime-gametest10-passed/` contains fresh
XML/log/input/exit/inspection; no fresh JUnit/JAR/Python is attributed to this
focused run. All9 new cases passed: independent Architectury deny; Fabric
BEFORE+CANCELED deny; exact ARCH→BEFORE→one native true→AFTER; target ABA; BEFORE
top removal; AFTER top removal; real AFTER RuntimeException; native false and
RuntimeException after paid ARM. UNKNOWN held actual credit19 from a real1-unit
Perditio source, worn pick1 and unsendable journal through detached codec reload
and >40 genuine powered ticks, with no retry/refund/output/AFTER. AFTER failures
retained exactly one diamond and already-worn tool returns. Fatal Errors/partial
body-mutation faults/disk restart are not covered by this matrix.

Root promoted only3 new staged GameTest files after d712762a... freeze/hash review,
made only the requested package-private test Fixture access, wired9 wrappers/IDs
and required GAMEtest-only removal observer. Production sources unchanged since
full9. Root strengthened the new matrix to compare **all actual physical entity
UUIDs** against its whole journal set, not merely check known candidates. Existing
five-cell/owned-UUID PASS/FAIL/TIMEOUT cleanup is reused; no new fixture framework,
production permission or loader-bridge replacement. Temporary false/throw native
injection is exact-frame+source+target gated and absent from production JARs.

Fresh full11 now RUNNING: `bore-runtime-validate11`, shell
`sh_14afd606a001YGCFXKA3syE9nx`; Python11 shell
`sh_14afd6b4e001MGBDgL3PBNqNiQ` / `.tmp/bore-runtime-assets11.log`.
Inputs frozen; no other Java/game or active source writer. Await notifications,
do not poll/duplicate/edit inputs. Then inspect via `.tmp/inspect_bore_runtime.py
bore-runtime-validate11 284 138`, static remap helper with run-name argument, parse
fresh Python76 log and archive before next build. Static full9 remapped shadow
inspection independently matched all8 declared native/mod shadow fields; not a
packaged launch verdict. B's finite route review is the sole outstanding agent,
read-only/ignored note only. No client launch or export is authorized by tests.

**Full runtime checkpoint9 passed:** `bore-runtime-validate9` actual **284 JUnit /
129 required Fabric /76 Python98.046s**, zero failures/errors/skips, **3917 unchanged
inputs**. Fresh XML/JUnit/input/log/inspection/Python and source-bound candidate
are archived in `.tmp/bore-runtime-validated9/`. Candidate10,841,529bytes,
SHA256 `f70b32f4c1698bf3247afd1bb1525f69cdc341e045ff605d4b3ed6db8196fa8f`.
ZIP CRC/notices/test isolation/single intermediary AW/exact recipes/assets and
Loom-added common refmap checked. All prior284 JUnit/116 native IDs are retained.
All191 accepted stopped-parent payload paths+hashes and all3 distribution ZIPs/
hashes unchanged. The manifest intentionally excludes only the constant UTF-8
snowman `session.lock`; no other new/missing parent path is allowed. Inspection
compares source mixin JSON plus exactly Loom's added packaged refmap, not false
byte-identity of the transformed JSON. No packaged launch/remote/full survival
or pixel/client acceptance is inferred. Known inherited Architectury launch
warning remains historical; this is not a clean-log claim.

No Java/game is active after run9 and Python9. Staged protection regressions and
read-only finite route review remain independent pending owner freezes; do not
export this candidate or overwrite accepted focal records. Next: review/promote
staged cases, execute new focused native regressions, then fresh full checks after
final fixes and a finite genuine-acquisition client route/PNG/stopped audit.

Current frozen-input full run: `bore-runtime-validate9`, shell
`sh_14aed39ae001BzTvqJ6jPBXlTN`; independent Python run9 shell
`sh_14aed490a001R8h6RgHW7F16VP`, log `.tmp/bore-runtime-assets9.log`.
Do not poll, launch another Java/game, or modify active inputs before completion.
C/B work only in ignored staging/read-only notes. Root removed its temporary
test-only diagnostic return observers; the genuine test-only BLOCK_DESTROY
witness remains. The two exception catches are included in run9; targeted new
denial/failure regressions remain staged, not claimed as executed. On success use
`.tmp/inspect_bore_runtime.py bore-runtime-validate9 284 129`, inspect Python log,
archive fresh XML/JUnit/logs/inputs/JAR inspection before any next build. Inspection
also checks all 191 parent path names/hashes and all three distribution ZIPs/hashes.

`bore-runtime-gametest8` **actually passed all 129 required Fabric cases**, with
unchanged inputs and all 191 accepted-parent files unchanged. Evidence is in
`.tmp/bore-runtime-gametest8-passed/`. This focused run supplies no fresh JUnit,
Python, remapped-JAR, client, pixel or export acceptance.

The failed ore completion was a real guard false-denial: native IMMEDIATE
`getBlockEntity` calls `createBlockEntity` even for ordinary non-BE blocks, where
the native method returns null without a factory/callback. Root now invalidates
that creation watch only for an actual BE-bearing state, **before** its factory.
All explicit state/owner changes, pending promotion, removal and ABA guards remain.
The genuine wooden-pick diamond harvest, exact worn reservation/fixed-UUID drop,
no mining XP and all previous cases then passed. Temporary test-only return-site
diagnostic mixins were removed after diagnosis; run8 remains historical to those
exact sources. Runs5–7 diagnostics archived separately, never as a PASS.

After run8, root added two narrow, still-unverified RuntimeException paths:
single native removal throw -> UNKNOWN reservation + log + no retry/refund/output;
AFTER notification throw -> log, retaining the already-durable true witness and
normal finally-settlement. Fatal Errors still propagate. No general fallback or
manufactured success was added. Fresh native regressions/full checks must cover
these newer sources before client work/export.

Owners now:
- C `ses_eb5a049dbffedGPGIsw7xHvIGn`: **ignored staging only**
  `.tmp/bore-protection-stage/**` / `.tmp/bore-protection-native-api.md`, actual
  protection ordering/denial/ABA and false/throw/AFTER ownership regressions.
  Reuse existing five-cell GameTest Fixture; no production writes or execution.
- B `ses_eb5c7203dffeqNezD1e4wIF71e`: read-only finite client-route feasibility;
  writes only `.tmp/bore-client-finite-route-review.md` / ignored calculations.
- Root owns active sources, integration, execution and acceptance. No Java/game
  currently active. Do not promote ignored test stages before SOURCE_FROZEN review.

Distribution remains accepted focal `0104dc...`; Bore inventory routing, BE loot,
loose-item vacuum, Repair, special clusters and unsupported focus branches are
explicitly pending. No Bore client launch, screenshots, stopped audit or export.

Focused `bore-runtime-gametest3` completedactual129cases:128PASS/ONEFAIL
`boreordinaryoreusesrealnativeticksandnoxp` (pickwear1neverobserved). Allothernew
cases incl realbaseEssentia/retirement/malformedUUID/MAX/nativeouterfence andall
old116 passed. XML+log/inputs/exit archived `.tmp/bore-runtime-gametest3-failed-ore/`.
Required Fabric Lifecycle collision isfixed andnativeBOOTreached; stillNOwhole
BorePASS/export. Rootonlyenhancedfailedassertionwithactualcurrent/power/fenced/
base/count/selectedTarget/scan/rotation/revision reads (no testpermit/targetsetter/
extraresources). `bore-runtime-gametest4` nowRUNNING sh_14adf17c80010DR5Y4Uq9FGp31,
freshsnapshot, GameTestOnly. Awaitnotification; allsourcefrozen/nootherwriters,
doNOTpoll/duplicateJava/editinputs. Needdiagnoseactual state, notrelaxpickwear or
removefailedcase. FinalFULL+Python sourcefresh stillmandatoryafterfixes; current
distribution0104dc.../191parent unchanged andnoBoreclientyet.
**FULL2actualexit1 beforeNativeBOOT**: fresh284JUnit0fail/errors/skips, actual
76Python98.370sPASS; main/devbuild+remappedJARmade10,841,036bytes91f4fe7... but
NOTaccepted/exported. Native129NOT_REACHED: Bore lazyMap.remove @Redirect collided
with Fabric Lifecycle's REQUIRED unload callback atsamecallsite; doNOTdisable
Fabric/require0/oldguard. Root FIXED to REQUIRED methodHEAD observation using
native rawowner-isRemoved / absentowner+pendingNBT predicates. Both maps read
throughactualChunkAccess declaring-class shadows. NativeMap.remove/Fabriccallback
nowunmodified; emptyCHECK noBE/noPending doesnotinvalidate. Mainmixinsstill7.
Archive `.tmp/bore-runtime-validate2-failed-boot/` holdsfreshJUnit/logs/source/exit/
UNACCEPTEDcandidateJAR and explicitpartialstatus. Earlier129XML neverreused.

Newsourcefrozenfocused `bore-runtime-gametest3` RUNNING sh_14ad99231001rtpfa30jRf0rC3,
snapshot`.tmp/bore-runtime-gametest3-inputs.json`. ONLYGameTesttask/incremental
nativecompile, nofreshJUnit/remappedartifact/fullacceptancefromthisrun. Awaitexit,
thenrequireactualfreshXML+129requiredcases andunchangedinputs. DoNOTusefullruntime
inspectoronfocusedrun/staleJAR. AfterconcreteNativefixes neednewFULL+Python anyway.
OneGradle/NOothergame/NeoForge/activewriters. Rootconfirmed ArchitecturyTransformer
StringIndexOutOfBounds inremoveDuplicates alsoexistsinactualPASSprereq3 log;
inheritedlauncherwarning remains, NOTnewMixinconflictcause orclean-logclaim.
Accepteddistribution0104dc.../191parent immutable; noBoreclient/PNG/exportyet.
**Compile2 ACTUALexit0**, inherited-fieldAPwarninggone (onlyoldmockplayer
deprecationsremain). BEFORElaunchingFULL1, C readonlyretirementreviewfoundone
concreteMAXrevisionduplication path: RECONCILE(MAX-1)→MAX thenstableEVAC published
PendingbeforeTileincrementrefusal, fallbackopaqueownerwouldduplicate. RootFIXED
evacuateBoundMAX→opaque custody BEFOREphysicalpublication. New nativecase loads
explicitMAX-1/refunknown saved-inputfixture, letsREALworldtickreconciletoMAX,
thenactualonRemoveassertsONLYonewholequarantineandNOPending. Nativehelper owned
5cells/cleanup preserved. Requirednative129now, expectedJUnit284/Python76.
`.tmp/bore-runtime-validate1-inputs.json` is ONLY unusedpre-MAX-fixsnapshot;
NOGradle1launch/exit/XML/nativePASS/failureclaim forit. New actualFIRSTFULLrun
`bore-runtime-validate2` RUNNING sh_14ad5d7d40019xsqygJhgCzOsx, newfrozeninputs
snapshot; independentPython2 RUNNING `.tmp/bore-runtime-assets2.log` /
sh_14ad5e581001YwBaa8BKLz5yiA. OneGradle/NOothergame/NeoForge, allsourcewriters
done (Cfinalreviewreadonlycomplete/P1fixed). WAITnotifications, no polling/duplicate
build/active-inputedits. Onsuccess inspect `.tmp/inspect_bore_runtime.py RUN284129`,
archivefreshXML/JUnit/inputs/log/JAR/notices/isolation/191parent+0104dc distribution.
DoNOTexportorclientacceptfromnativechecks; clientroute/PNG/stoppedBoreauditpending.
`bore-runtime-compile1` actualexit1: productioncommon/Fabric/clientTest compiled,
but commonGameTest had1typeerror(newAABB(BlockPos,BlockPos) nonexistent). Also
APwarned inheritedLevelChunk@ShadowblockEntitiesmappingunresolved. RootFIXEDtest
withactualVec3lowercorners, andmovedrawownerfieldshadow toactualdeclaringnative
ChunkAccess inNEWrequiredBoreNativeChunkOwnersMixin+Ownersreadonlyinterface.
LevelChunk retainsregisteredticker/map observation; ownersread inheritedfrom
transformedparent, notguessedalias/remapfalse/ignoredwarning. Mainrequiredmixins
now7, notold6. CurrentactualnativeAP/remapstillunexecuted.
Compile1history+actualPython76PASS97.799s archived `.tmp/bore-runtime-compile1-partial/`.
NoJUnit/native/JAR runtimePASS inferredfrompartialbuild. SourcefixesmakePython1
historical, freshfullPythonrequiredafterfinalsource. New frozen snapshot
`.tmp/bore-runtime-compile2-inputs.json`; actualCompileOnly2 RUNNING
sh_14ad25b6b001xKhslXVyXSR3C0. DoNOTpoll/duplicate/editactiveinputs. CREADONLY
reviewA'sretirementpatchwhilecompile, ONLYignored `.tmp/bore-retirement-fix-review.md`,
noactiveedits/outputs/runclaims. Noactivegame/NeoForge/export/acceptedrecordchanges.
**ALL owners SOURCE_FROZEN; FIRST actualBore-runtime compile nowRUNNING**
`.tmp/bore-runtime-compile1-inputs.json`/shellsh_14acfa8cd001ZVwnXFvWy9w2S4.
IndependentfullPython76expected RUNNING `.tmp/bore-runtime-assets1.log`/
sh_14acfcf73001NQrCyaI3fxbhzc. OneGradle/NOgame; noNeoForge. DoNOTpoll/duplicate
build or editJAVA/resources/scripts untilcompletion. Root added3retirementnative
wrappers afterAfreeze:128requirednow(NOTresults), expectedJUnit284/Python76.

Aretirementcustody SOURCE_FROZEN44e82c78... received/rootreviewedactual5production
fixes+3newnativehelpermethods. Actualdyingslotobjects transfer+clearinsameexisting
ETrawstep; fallbackownsONEqueueopaqueunsendablecustody, no stalepreparedescrow
publication. Stableevacuationunchanged; actualnativeopenretirementretentionbinding
canretaincallback-removedoldBE withoutopeningordinarywork/lifetimepermissions.
Latercoherentnativecodec checkpoint(orfailedsavekeepsdirtyliveownerforretry),
notpretendserializer/powerlossguarantee. RootloaderUUIDdowngradefix and4argouter
destroyguard retained. Cfive-cell/nativePASSFAILTIMEOUTcleanup0f5bd25... frozen.
No newruntimePASS/client/pixels/stoppedBoreaudit/export claim; currentdistribution
0104dc.../191acceptedparent remainimmutable. Awaitactualcompile1, fixconcrete
compilererrorsifany, thenfreshFULLnative128/JUnit/Python/packagedartifactchecks.
C nativefixture cleanup TEST_SOURCE_FROZEN **0f5bd25e5ec1793fd27c24a0822e9af4c0247ba933500eb831c74b5f35654a81**
received; rootreviewed517linehelper/exact5AIR-noBEcellpreflight/PASSFAILTIMEOUT
nativeGameTestInfo listener/oncepowerpause andsame-canonical-queue exactownedIDs
cleanup. Note `.tmp/bore-work-fixture-cleanup.md`fa84693a...; actualArchitectury13
Event.java read confirmsunregisterexists (notFabriceventapi). No sourceexecution/
nativeclaim. Original3production05203557frozenhashes supersededONLYrootloaderfix/
Aongoingretirementcustody; CmadeNOPRODUCTIONedit. Optionaltestcompatibility branch
for opaque `boreRetirements` removesONLYthisfixtureUUID/Machine/Source rawinputs,
notgenericqueue reset, beforeserialization. Needverifyactualnormal/timeout cleanup
whenGradleruns. AretirementP1fix remainsSOLEwritingowner; WAITitsSOURCE_FROZENbefore
compile/full125+native/284JUnit/76Python expectedNOTresults. Otherinputs stable,
stagepromotiondone/noactiveJava/game/NeoForge/export. Sourcecodeisnotacceptance.
**TWO concreteP1 ownership findings beforefirstcompile**, fromAignored
`.tmp/bore-core-ownership-review.md` (noexecution):
1) failedretirementdeepcopy/validationcouldreturnbeforecustody thennativeonRemove
   deletesactualcurrenttools. A NOWexclusivewritesBoreWork/Deferred/TileArcaneBore
   (+ET/nativecontextONLYnarrowtypedretentionbridgeifneeded) andNEWhelper/note
   `.tmp/bore-retirement-custody-fix.md`. Preserveactualworn/currentcomponent stock
   inONEopaquequeuequarantine owner BEFOREBEclear; doNOTpublishstalepreparedcopy,
   resnapshotloop/merethrowaftersectionreplacement/publicretireintent/no-op. Normal
   stableevacuationmuststillreturnphysicaltools. Allrealgameplay/lifetimeguards
   remainstrict; onlyactualcapturednative-dying retention canhavea closedfallback.
2) rootFIXED loaderBoreUUIDdowngrade: gatherreservedUUIDs fromALLrawjournalRows
   BEFOREvalidatingjournals, thenreservedPendingwithoutBoreCOMPOUND quarantines
   beforelegacyfallback (evenmalformedjournal cannotdowngrade); unrelatedV1/2/3
   legacyrowsunchanged. A'scustodyfixMUSTpreserverootloaderpatch.
Root NEW`BoreJournalDowngradeRuntimeChecks` rawmalformedsaved-inputtestcase for
ARMED/UNKNOWN/WITNESSED/malformedjournal, plusunrelatedlegacyreaderchecks,125required
now(NOTresults). Detachedload+flush mustemit0 fromstrippedreservedrow; no real
paidBoreoutput isgranted byfixture. NewtestUNEXECUTED. CstillONLYnativefixture
cleanuphelperfollowup, never3productionfiles. RootmustawaitAproductionfixANDC
testcleanupfreeze beforeANYGradle/Java/game. Previouslypromotedruntimeoriginal
stagingbytesretained; activefutureTilefixnotold118ec...runtimeevidence. Noexport/
acceptedrecord/191parent/NeoForge changes. Latestactualresultsremainprereq3only.
C PRODUCTION SOURCE_FROZEN **05203557109ea3402a41efb42ba212f5695bfcae6751fa5c2da4692b4d21e4ca**
received; rootreviewedBoreWork/closedETrawscope/DeferredV3ARM-WITNESS-UNKNOWN/
fixedUUID<=64segments+load/receipt flow. Root guardedpromotionranONLYnew6runtime
files afterhash/existence/191parent checks; `.tmp/bore-runtime-promotion.json`.
Noexistingfileoverwritten. Stage118ececa... unchanged; activeItemArcaneBoreONLY
rootaddedhonestEN/RUejection-only/unsupportedbranches tooltip. Runtime/core/client/
nativeguard realAPIs nowACTIVE (no stubs), UNCOMPILED. Rootwired3Cnativewrappers,
124required IDs now(notresults); expectedJUnit284/Python76, newcasecounts authored.

C NEWexclusive followup writesONLYBoreWorkRuntimeChecks.java +NEWignored
`.tmp/bore-work-fixture-cleanup.md`: nativefixturecurrentlylacksrestore/cleanup
outsideEMPTY_STRUCTURE, mustown5exactcells+onlyitsjournal/entityUUIDs andclean
onPASS+FAIL+timeout beforeanybuild. C3PRODUCTIONfiles frozen/root-owned, mustnot
editduringcleanupfollowup. AREADONLY reviewsC3PRODUCTION ownership source, may
writeonlyignored `.tmp/bore-core-ownership-review.md`; no status/runtimeclaims.
Roototherinputs nowfrozenpendingCtestcleanupSOURCE_FROZEN. Needreviewfinaltest
cleanup+potentialAconcretefindings, thenbeginfreshsnapshot+FabricCompileOnly,
actualFULL124+Python76 AFTERfinalfixes. NoactiveGradle/game/NeoForge/export.
Developmenttoolsnotacceptancerecord; distribution0104dc.../191focalparent unchanged.
Root authoredactualnative regression for missingdestroyBlockouterfence:
`BoreNativeDispatchRuntimeChecks.outerDestroyCallbackRemainsClosed`,121required
now(notresults). It executesreal4argLevel.destroyBlock ondeclaredstone/dropsfalse,
TESTONLYisolatedGameTest ServerLevel.gameEvent HEAD capturesactualBLOCK_DESTROY
afterinnerstateisAIR and demandsdispatchFALSEuntilouterreturn, thenTRUEagain.
No eventcancel/throw/mutation/grant; probeThreadLocalalwaysfinallyremoved. New
GameTest-onlyrequiredmixinJSON+metadata, neverproduction; nextJARisolationmust
proveabsent. No Java/build/nativeexecutionyet; Crealserviceauthorlastactiveowner.
Independent native-guard review found ONE concrete falseadmit: native4arg
Level.destroyBlock→post-inner-setBlock BLOCK_DESTROY event couldseequeue dispatch
openbeforeouterdestroy returned. Root addedEXACTboolean(BlockPos,boolean,Entity,int)
descriptor to nativeMutationOnStack viaactualclassidentity, notmappedmethodname.
No new permit/mixin/policy/no-op; no Cwork inspected. Review
`.tmp/bore-native-guard-review.md` confirmsnootherconcreteBblocker, source-only.
Bfrozenbaseline6472... nowROOTfixmodifiedNativeContext; newhashmustbindnextbuild.
Actualnativecallback/remapped/AP checks stillpending; NOGradle/game yet because
CrealBoreWork/sharedexecutor/DeferredV3 stillactivewriting. Root also warnedC of
knownnativeItemStack/entitycodec99limit: splitstablepreparedentities<=64 ORstrict
predelegate unrepresentablelootrefusal; count1journal alone DOESNOTresolveR10.
Neverpublishunpersistable100/127ItemEntityortruncateafterremoval. Noaccepted
record/distribution/save/NeoForge changes.
B nativecontext SOURCE_FROZEN **6472b0ef2ff2b69ecd3ef2ad7d69db6fc708765e2480820d03ec93de07add2c1**
received; rootreadnativecontext+allcriticalmixins/API. Root addedALL6requiredcommon
mixinentries (strictrequire1, lazyMapremove2sites require2) andalreadywiredreal
levelUNLOAD/serverSTOPPEDclear hooks. Core uses actualbound/rebindable native
tick/privateonRemove/mutablecellABA/lifetime observation, noManualTile.tick
permission/callerboolean/fallback. Root mustverifyactualAP/refmap/injectioncounts/
dev+remappednativecallers, notclaimPASSfromclassbyteinspection. Runtime6staged
files stillfrozenunpromoted; C realBoreWork/EssentiaTransfers/DeferredV3 ACTIVE
authoring remainslastSOURCE_FROZENgatebefore promotion/compile. NoGradle/game.
Rootroute draft revisedwithUNEXECUTED300/188sourcebudget/finite14familyquantities,
firstscopeejectiononly andrelaydisconnectBEFOREtopplacement toavoidunconditional
prefillspoilingbaseline. Nativepiston68PASS remainsseparatealreadyactual3evidence.
Newsourcebudget/119othercasesstillunrun; noexport/acceptedrecord/NeoForgechanges.
Readonly rootfixture review found ONE concrete falsePASS gap: finalBoremenu
extraction asserted itemcounts only, couldmiss name/damage loss. Root fixed test
to findexactuniqueactualmaininventorystacks and ItemStack.matches both complete
expected values after realnativeextraction/reload. Counts checksretained; no
productionchange. `.tmp/bore-root-native-fixture-review.md` recordsreviewonly.
Sourcebudget300/188arithmeticsource-consistent; NOotherconcretebugsidentified,
notrun. TwoNEWPython resource/recipechecks ACTUAL PASS2/0.079s (notfullsuite,
notjavac/native/game/pixel evidence); fullPythonexpected76afterallsourcefreeze.
Aclientfrozen/rootownednow; Areadonlyreviewfinished. Bnativeguards/Crealservices
stillACTIVEwriting; NOGradle/game/NeoForge/export. Accepteddistribution0104dc...
unchanged; activeunverifiedJava120requirednativefixturesawaitintegration/build.
A CLIENT SOURCE_FROZEN **d471217aca7afbbaaca7553a3845dfc275ded5d3a71614c26edd78e362eda96a**
received; root reviewed all7actualclientclasses/API/models+BER/GUI/material/beam
read-onlybinding. Existingrootinit/builtin6argcallbacks/EN+RUkeys matchpublished
APIs. Original bore.png physical256x128/logicalUV128x64 (doNOTconfuseheaderwithUV).
Original3vortices/jarcore/nozzleUPDOWNartquirk/nativeboundstextures retained; full
particlepulse/fade/debris/audio/shaderlowalpha parityexplicitlypending. No
clientcompile/game/pixelresult yet. A nowREADONLYreviews ONLYroot3nativefixture
files and maywriteignored `.tmp/bore-root-native-fixture-review.md`, noactiveedits.
Bnativecontext/lifecycle/watch and CrealBoreWork/sharedexecutor/V3journal still
writingACTIVEsource. WAITbothSOURCE_FROZENbeforeANYbuild; noactiveGradle/game.
Rootmustthenpromotefrozen6runtimeclasseswithoutoverwritingunfamiliarfiles,
wireBrequiredmixins/actualrootAPI, reviewCtrue/unknownresource/outputboundaries,
compileandactualnativechecks. EmptyBERblockmodels/builtinitems arereal future
meshdispatch, notstand-inrenderpass; currentunresolvedactiveinputs unverified.
ROOT added FOUR newactual-native helpercases (120requirednow, NOTresults):
realbase2x900CV/9inputpayment+exacttoprecipepristinepredicates; all6two-cell
shapeANDnativeadjacentcollisioniteration/6clicked-sidewandrotations+manualtick
rejection; native2slotquickmove/fullcomponents/actualfactoryreload/stalemenu;
finite300unit rawsourcebudgetpredictions/112cost/188collateral retention. Latter
formula expectedsourcepick18 and4piston68 etc is UNEXECUTED; doNOTclaim actual
distillation300 or genuineclienttoolcraft fromarithmetic. Legacy1.12 extracted
object_aspects pick directline474 ISNOTinoriginalConfigAspects462lineoracle and
mustnotbeusedasaspec! Actualoriginalsourcepick tags are recipe-derived+bonus;
root DIDNOTadd guessedoriginaltoolrow orborrowlegacytags. Pistononlyactualnative
68 alreadyverifiedinprereq3. Originalassetrefs passread-onlyconverter --verify
(0missingrefs), notpixels/rendering. Rootnativeplatform andvanillaeventpaths
compiledonlyafterallownersfreeze. EarlyA/B/CAPIreviewed forintegration: Creal
firstscopeEJECTIONONLY; nativechest/hopper/furnace rawroutingstillpending (no unsafe
setItemloop). B received necessaryisLiveLevel/canDispatchOutputs lifetimeAPIsteer.
RootwiredA publishedinit/builtinitemmethods andEN/RUwidth/speed/propertiesaliases.
CurrentJAVAinputsMIDEDIT/UNVERIFIED; NOGradle/game/NeoForge/export/acceptedrecordedits.

**LATEST root state:** prerequisite3 actual PASS **284JUnit/116requiredFabric/
74Python103.798s**,3873unchangedinputs; freshdevelopmentJAR10,676,408bytesSHA256
`c8b756ba739ea7dcc2d65f11d0d19dab6250bc9630760ca466b96a206a013c87`,
ZIP/notices/isolation/oneAW/originalpistoncomplex332 checked. Archived fresh
XML/JUnit/log/input/exit/inspection in `.tmp/bore-prereq-validated3/`. Native four
piston68units complete collateral+fuel+jar/phial transfers PASS. This is NOTBore
gameplayacceptance/export. All191focalparent files anddistribution0104dc... exact.

Six runtime classes STAGED_SOURCE_FROZEN118ececa... `.tmp/arcane-bore-runtime-api.md`
and `.tmp/bore-runtime-src/` received, not yet promoted/compiled. Active source
integration is now IN PROGRESS with exclusive authors:
- C ses_eb5a049dbffedGPGIsw7xHvIGn owns REAL BoreWork +existingEssentiaTransfers/
  DeferredItemDrops +narrowBorehelpers/nativechecks; root services ownership was
  explicitly reassigned to C. No source builds until freeze.
- B ses_eb5c7203dffeqNezD1e4wIF71e owns ONLYNEW BoreNativeContext+NEWBore* mixins
  for actual boundnative ticker/retirement/ABAwatch/lifetime; no stageTile edits.
- A ses_eb5a0fd33ffeqCL3WeAeqLxoMx owns ONLYNEW Bore client registration/GUI/
  original models/BER classes; no resources/bootstrap.
- Root owns registry/bootstrap/platform/recipes/book/wand/resources/integration.
  New actualBorePlatform/FabricBorePlatform native callback bridge added (not stub),
  explicit canonicallevel/unspawnedFakePlayer profile checks. Registry, exact
  original base recipe255/top375(pristine4/5), book aliases/icon, zero-vis wand
  adapterbeforecasting, lifecycleclearhooks, bareblock/BERbuiltinitemmodels and
  independentdrops/axe tag added. Runtime/core/visual dependencies unresolved until
  ownersfreeze/promote; DO NOTCOMPILE now or callthisworkingBore. NOsourcePASSclaim.
  Root authored actualnative base-payment+topresourcepredicate case, now117required
  IDs(notresults); old116preserved. More machine/protection/collision/menu native
  cases stillneeded. No newclient launched/acceptedrecordrewrite/NeoForge/export.

A piston SOURCE_FROZEN three sourcefiles c221ddc1.../533d324a.../88139395...
received; root reviewed `.tmp/bore-piston-aspect-api.md`627ea817... +actualhelper.
ONLYcomplexrow332 rawmachina2/motus4, all90priorrows exacthashunchanged. Source-
derived wholepiston17 =Arbor2/Terra3/PD3/Metal3/Machina2/Motus4 (Potentia1culled),
NOTruntimePASSyet. New SERVER fixture4pistons/1coal/5emptyalembics/6emptyjars/
1EMPTYreusedphial, actualnativecook/exchange/collateralconservation authored,
no suppliedessence/research/XP/vis. Root wiredsingleFabricbatch wrapper
borePistonAspectsDistillation +requiredID, old115retained→116REQUIRED(notresults).
NewEarlyObjectAspectBindingsTest adds1JUnit, expected284NOTresult. Activeinputs
FROZEN; freshcompile/fullprereq3+Python3 next; Bonlyignoredruntimewriting.
Stagedrootplatformuncompiled/unpromoted. Noactivegame/NeoForge/export/accepted
recordrewrite; acceptedfocal0104dc.../191parentfiles immutable. Need actualnative
pistonrecipebase/cull/cache/distillation before claimingMotus source works.

Root READONLY futureclient site preflight `.tmp/bore-candidate-site-preflight.json`
and `.tmp/bore-client-route-draft.md` (NOTlaunchapproval): candidate top1615,218,381/
base1615,217,381/NORTHchest1615,217,380 all savedAIR; ALL1650width5SOUTH near-ray/
64candidate cells x1613..1617,y216..220,z382..447 savedAIR/no absent chunks. No
block/chunk/entity writes or clearing. Native source/oldrelay/top squared49/50/113
exclude directpower, permitgenuineoldrelaymine/pickup/reinstallforbaseline/boost.
Actual14free main slots3,20,22,24..34 (NOTold15), savedgold108 canpaytop2→106,
payer10AIR4070/ORDO1270 cancoverexpectedbase900+900 withoutvisgrant(subjectactual
nativepayment). Acceptedaltar center1474,216,377/matrix1474,218,377 hasALL8peripheral
pedestals+centerCount0; actualactive1/crafting0/emptydemand, onlyoldidlecapcache.
FiveBoreaspects absentfromoldnearbyjars; newsourcebindingmustproveold333untouched.
Finite14ingredientfamily/sourcebatch/toolcraftstorage draft NOTquantity/timing/
clientapproval; pistonactualfinaltags/nativeprocessingstill A-ownedpending.
All191acceptedparenthashesrecheckedexact. B runtime/rootplatform remainignored
stagingonly, no promotion/compile while A newpistonresourcesourcewriting. No
activeGradle/game; noNeoForge/acceptedrecordrewrite/newexport.

**bore-prereq-validate2 actual PASS283JUnit/ALL115requiredFabric/74Python95.563s,
3872unchangedinputs.** All113priorcasesretained; actualfreshXML/source/JARchecked.
New developmentJAR10,675,936bytesSHA25690cd9bccf505a279a18133b32f798f3fba785c5f7dcc96a56b0ff0eb481d106a,
notices/isolation/oneintermediaryAWverified. Inspectorfirsthad literalspace-vs-tab
AWassertion falsefailure; rootfixedONLYignoredchecker to tokenizedEXACT2lines,
thenactualinspectionPASS (no source/JAR/tool rerun/relabel). Evidence archived
`.tmp/bore-prereq-validated2/`; checks NOTworkingBoreacceptance and NOTexported.
Actual2nativeinfusionpredicatechecks passed selected-session/replacement/load,
all45newpureJUnit passed; no newclientvisual claim. Distribution0104dc... and
all191acceptedfocalparentfiles verifiedunchanged afterprereq2.

Next activeowner A ses_eb5a0fd33ffeqCL3WeAeqLxoMx now owns ONLYpistonoriginalcomplex
row332 resource+EarlyObjectAspectBindingsTest+NEWBorePistonAspectRuntimeChecks/
minimaltest_convert_assets specificnewrowchecks ifneeded, ignoredAPInote. Motus
source originalmachina2/motus4 additions torecipe-derivedbase, NOTliteraldirect
orfinishedessentia grant; ROOTwire/compile/testafterfreeze. Rootdoesnoteditthose
files meanwhile. B stages ONLYignoredruntime, stillunpromoted/uncompiled. C
plannercomplete/noactiveedits. Rootnewplatformeventbridge draftedONLYignored
`.tmp/bore-root-src/` and `.tmp/bore-root-platform-plan.md`; explicitFabricFakePlayer
andBEFORE/CANCELED/AFTER forwarding, no state/loot/payment permit or no-op fallback,
independentArchitecturyBREAK/rootlease/atomicjournal/lifecycle stillunimplemented.
NoactiveGradle/game after2completion notifications; noNeoForge/export/recordchange.

`bore-prereq-compile1`exit0. `bore-prereq-validate1`exit1 atJUnit:283run/1failure/
0errors/skips. ONLYnew BoreInventoryPlanTest deep-copyfixture append() on native
codec-decoded unmodifiable sibling list threw UnsupportedOperationException.
Planner production notchanged. Root test-only fix accepts native immutable-list
refusal, STILLmutates supporteddecodedcomponentstyle+NBT/count and asserts fresh
views unchanged. Never convert an actual planner alias failure into PASS.
HistoricalfailedXML/log/input/exit archived `.tmp/bore-prereq-validate1-partial/`.
Native115/JAR build were NOT_REACHED; priorfocalXML/JAR not evidence fornewinputs.
Python1 actual74PASS93.445s. Refreeze/allactiveownersdone; B ignoredruntimeauthor
continues noactiveedits. FreshFULLprereq2+Python2 next; no export/acceptedrecord
update/newclient/NeoForge. Distributionacceptedfocal0104dc... remainsunchanged.

**All ACTIVE prerequisite owners SOURCE_FROZEN.** C pureBoreInventoryPlan
55601456.../24newJUnit source received; root reviewed closed detached immutable
count1-component+separatecount snapshots, legacy64/far-no-empty/sidedfrozenpolicy/
wholebundleconservation; no publicapply/debit/deliveryauthority. A3+B18+C24newJUnit
authored(notrun), so historical238+45 suggests283expected, NOTa result. Two new
native IDs wired, all113prior retained,115required. Newinputs snapshot
`.tmp/bore-prereq-compile1-inputs.json` captured; actual Fabric-onlyCompileOnly
RUNNING shellsh_14a76b02a001fD89uzJDohelz8. NootherGradle/game. DoNOTeditACTIVE
inputs untilcompletion; B staging runtime writesONLYignoredfolder, safeparallel.
Aftercompile actualreview/fixes ifneeded, then freshFULLprereq/Python validation;
doNOTexportprereqJARasworkingBore or overwriteacceptedfocalrecords. Accepted191
parent/save/0104dc distribution unchanged. Native fixture concerns (loaded25x25
footprint/global recipe manager/restoration) require actual tests, not sourcePASS.

A+B active prerequisite sources now SOURCE_FROZEN (seven infusion files +pure
Math/JUnit); root reviewed them and wired115required Fabric cases. C active
NEW BoreInventoryPlan/JUnit still writing; wait Cfreeze before build. B mathowner
nowauthors NEXT runtime exclusively IGNORED `.tmp/bore-runtime-src/**` +API note,
NOactive-source edits, so root MAYcompile prereqs once Cfreezes while B stages.
B owns stagedbase/top/placementitem/twotiles/menu; rootownsfutureBoreWork real
protection/atomicessentia/outputjournal/registries/platform/lifecycle integration.
No stubs/no-ops registered, stageuncompiled/unpromoted until rootreview/noJava.
Strictbaseidentity/loss-hardening and initialknownnonBE/noexistingloosevacuum
safetyrefusal remain explicitlybounded, notfulloriginalBore. Same original math/
credit+/66/short/noXP/caller-ordercontracts. Currentdistributionacceptedfocal0104dc...
untouched; nextprereqbuildwillNOTbeaccepted/exportedasworkingBore.

A SOURCE_FROZEN seven files received; root reviewed predicate declarations/JSON/
wire/defaultoldconstructor/selectedpairedsession/load/consume/bookexamples and
native fixtures. `.tmp/bore-pristine-infusion-api.md`29cd1406... lists exacthashes.
Root wired TWO Fabric-only wrappers in isolated batches and added theirunique IDs
to requiredmanifest (all113priorIDsretained, now115REQUIRED, not results).
Tests use isolated recipe-manager fixture/finite source stock/native synchronous
tickers, NOT saved-world client acquisition; no dummy Bore production recipe.
B pure ArcaneBoreMath SOURCE_FROZEN c4b520d1.../18newJUnit source received, root
reviewed API; helper unwired, no working-machine claim. C pureBoreInventoryPlan
stillwritingNEWsource+JUnit, wait its freeze before ANYGradle/build. C earlier
294c103c... design confirms independent ArchitecturyBREAK thenFabricBEFORE events,
raw99!=legacy64 inventorylimit; no nativeGameMode mixin reuse/double-events.
Root approved conservative nonpositive merge room no-transfer for oldoverstacks,
not negative-room stock growth. Next compile/check expectedinputcountsare not
accepted focal evidence; distribution0104dc.../191focalparent immutable.

**NEXT GROUP ACTIVE:** [Arcane Bore](analysis/2026-10-17-arcane-bore.md).
Accepted distribution final reverified `.tmp/focal-accepted-distribution-final7.json`:
3modsZIP/checksumsOK,2depsunchanged,all8priorbackupsunchanged+newM09bbackup=9.
New readonly accepted-focal parent baseline `.tmp/bore-accepted-parent-before-client.json`
contains191actualfiles (notold187); acceptedfocal14 stays stopped/read-only.
New source owners authorized AFTER accepted export:
A ses_eb5a0fd33ffeqCL3WeAeqLxoMx owns InfusionStackRecipe/InfusionMatching/
TileInfusionMatrix+NEWpredicate/helper/tests, optional precise book presentation.
B ses_eb5c7203dffeqNezD1e4wIF71e owns ONLYNEW lib/bore/ArcaneBoreMath.java +NEWJUnit.
C ses_eb5a049dbffedGPGIsw7xHvIGn READONLY machine protection/output design note.
Root owns all registration/manifests/builds/docs/acceptance/export. NO agents run
Java/Gradle/game/tests/NeoForge/Git/saves. DoNOTbuild until A+B SOURCEFROZEN; source
changes in progress are NOT covered by historical accepted focal results. No
bore block/item/recipe/working-machine stub registered. Review root scope/pristine
predicate across CODEC/network/selectedsession/consumption, plain old recipes
unchanged; pure math preserves +3..+66/fractional credit quirks. Next native tests
must wire helper IDs/root required manifest without duplicating113 old cases.

**ACCEPTED/EXPORTED focal final7/client14.** All gates PASS:238JUnit/113required
Fabric/74Python88.423s/3865unchangedinputs; newactual14normalstop+independentaudit+
ALL10exact164–173PNGinspected. ProductionJAR10,641,110bytesSHA256
`0104dc3756199192b56af2956a6f544702b2c6bb5848fc725d96ba4f051e3f10` is NOWin
distribution mods, previousM09bbac83fff backed upOUTSIDE mods in
`backups/visnet-fabric-validate7/`; dependenciesunchanged. README/SHA256SUMS updated.
Current atrecordingmatchTRUE/historicalFALSE. Acceptance evidence archived
`.tmp/focal-accepted-final7/`, `.tmp/focal-accepted-verification7.json`,
`.tmp/focal-accepted-client14.json`, `.tmp/focal-accepted-export7.json`.
Record `docs/porting/focal-client.json` binds exact14source/normalstop/stoppedNBT/
images; verification.json updatedafterpreviousM09bbackup. NO commits/push/release.

New accepted parent is stopped `focal-smoke-8fd11823-4346-48f9-97ed-e06dbf9ec4be`;
neverusefailed1–13asfutureinput. Native4prunehistory/onevanillaambientdespawn/
11protectedactors/all197oldUUIDreconciliation/187parentfilesexact/64profilefiles
exact/stock108gold+cobble4+focus15rank1+payer10CV are explicit, notfrozenworldclaims.
DeferredpassiveCharm/Silk/Dowsing/fullM08/API/EULApackaged/remote/survival/NeoForge
remainpending. Sourcebuild/client14JARisaccepted; anynewBorechangesneednewbinding.

**Immediately begin next Fabric group**, firstboundedprerequisite explicit
pristine-damage infusion component predicate (codec/network/session/load/consume),
per `.tmp/arcane-bore-root-scope.md` and original/pinned read-only audit559f9f...
No Bore block/recipe/stub registeredyet. Keep existingfocus/harvest/VisNetfences,
no implementation donorcopy. After exportguard/baselineverification, root may
unfreezeONLYnewprerequisite owners; accepted binary/save/evidence remainimmutable.

**Client14 exit0 + independent stopped-NBT audit PASS + ALL10 exactPNG164–173
root-inspected.** Rune4c1a533-c46a-4223-a58c-31618d937b44, stopped clone
`focal-smoke-8fd11823-4346-48f9-97ed-e06dbf9ec4be`,5658POSTs29–5686.
All gameplay paid112actualXP/112gold→108,5130all6recipeCV/8LEVEL/2400rawCVPotency/
13terraharvest/OLD3plusNEW1cobble4. Pure late-Mob observer records27actualfirst
observations; distant old skeleton has genuine nativecheckDespawn/DISCARDED witness,
not backfilled from11/13. Independent ALLregion audit:11named/NoAI exact,3cows,
all197oldUUIDs exceptONLYownwitnessedambientretirement,20jars333/72candles/95old
non-nodeBEset/oldB0furnaces/fullsavedinventory+flags+abilities+EnderItems. Actual
offlinePlayer653UUID derived/logbound/nativepdata=levelPlayer;64inheritedprofile
files unchanged. Parent187files exact;78fixture/earnedcells exact. Old1551 truly
pruned4times at1200/2400/3600/4800: stoppedCURRENTterra1/ordo2/others0,BASEaer76/
aqua76/ignis76/ordo78/perditio76/terra77,NORMAL/NONE/lock0/regen600; no clockreset.
Native13/11 historical, never exporttheir reports. Current14 evidence archived
`.tmp/focal-fabric-client14-{raw-report,tail,inspected,native-stopped-audit}.json`.
Final postclientFULL7 +Python7 RUNNING(sh_14a559890001S3oqrVNpyNiKd2 /
sh_14a5598a8001S2C5sj5OaX2bHA),3865frozeninputs; awaitactualexit/results before
record/export. No activegame; no sourceedits/NeoForge/duplicateGradle. Production
candidate0104dc... still NOTexported until final7PASS/recordgates. Then backup
M09bbac83fffoutside mods, checksums/docs, immediately begin Bore prerequisite.

Final READONLY independent review of late-Mob observation + actual offline native
profile binding found NONE concrete critical blockers. No source edits/execution/
client14 inspection by reviewer. Client14 still running until its own completion
notification (shellsh_14a4f50d8001d4oSHT42nB23eM); root active inputs frozen. DoNOT
poll/relaunch/duplicate game or Gradle. After actual14 success: archive raw/tail/
verify snapshot+parent187, run independent stopped auditor, inspect ALL14-run
tenPNG164–173, finalFULL7+Python7, record/export only if all gates pass. Latest
production stays0104dc... but export stillacceptedM09bbac83fff. Onceaccepted/exported
immediately start root-approved bore pristine-infusion prerequisite per ignored
`.tmp/arcane-bore-root-scope.md`; original audit559f9f... reviewed/refconsulted,
nativeFabric FakePlayer API actually inspected. No Bore active source yet.

**Client13exit0**, runed9893e4/private32c474c6,5666POSTs30–5695/10PNG.
Auto-focus+nativeShiftDOWN/UPworked exact HWND1247946; all genuinegameplay paid/
sameUUIDoutput/108gold/OLD3plusNEW1cobble4/serverrestore passed. Rootarchive raw/tail,
parent187exact. Independent audit NOTyetPASS: first UUID-equality assertion was
wrong for Loomrandomofflineprofile Player153; root now DERIVES actualUUID from
native OfflinePlayer:name log+login, exact levelPlayer/playerdata equality and all
64inheritedprofilefiles unchanged. Actualflags/EnderItems/abilities/stockstrict.
Next genuine auditblock: old distant skeleton UUID03377... missing but ambientrows
empty because it loaded AFTERinitialscene snapshot. DoNOTwaive missingUUID or
backfill cause fromprior11. RootCLIENTONLYnew Mob.checkDespawn HEAD observer
records pure first-late-native living snapshot BEFOREpossible naturalremoval;
it never cancels/ticks/damages/moves/respawns entities. Same eligibility/caller/
currentflags/bounds/damagechecks retained. Auditor joins initial+late observations,
binds everymissingoldacceptedUUID toactualnativewitness; completeparententityindex
separates laternativebirth identities from inheritedoldentities, neverclaimsold
preservationfornewbirths. Late-born retirements still need same strictactual
nativecall-chain/current flags, not a blanketmissingwaiver. Allnamed/NoAIold11
exactstopped, all197oldUUIDs required except ownwitnessedretirements. Sources
FROZEN forfreshclient14compile/game/autoownedfocus; needactualnew10PNG/stoppedaudit/
postclientFULL7+Python7 BEFOREaccept/export. No otherJava/game/NeoForge/newexport.

**USERnewdirection at readiness prompt: "Давай сам" — automate owned game focus.**
Root keeps exact PID/HWND/nativeShift gates, never sends a key to foreign HWND.
TESTONLY focus helper now restores own window, requests normal activation and,
if denied, briefly attaches current UI input queue to foreground queue solely to
activate our VERIFIED target; always detaches in finally BEFORE any SendInput.
No elevated process/rights/foreign-window mutation/keyboard trick/fake focus flag.
This explicit user direction supersedes prior manual-focus-only root policy;
no claim auto-focus worked until actual native HWND/GLFW check. Same25second
preflight/450wall budget, no extension; fresh client13 snapshot+compile/game next.
A readonly final source review found NO remaining observer/auditor-schema blocker
in sequence f6361a47...; no edits/runtime claims by A. Auditor09ae39b... root-owned.
Client12 early failure/source11/full6 remain historical, no new acceptance/export.

**Client12 early INPUT_CHECKexit1**, run75931a75/privatee4ce8ff2. New pure-read
observer compiled, but actual Win32 foreground524388 !=owned2820784 through the
user-approved500tick/~25second wait. NO OSkeyevents/fixtures/payments/POSTs/PNG;
raw archivedclient12, native tail correctly absent before hooks. Parent187hashes
exact. No source change required for input refusal; doNOTloop auto launches.
Await user readiness for another same-bounded fresh clone/foreground click.
A readonly final schema review still pending, no agent source writes; Bore
original audit note-only pending. Client11/full6 historical, not accepted/exported;
fixes require a fresh actually focused client13 +all10PNG/stoppedaudit/full7.

**A final ignored auditor freeze09ae39b... received; client11 NOT accepted.** Root
reviewed `.tmp/focal-stopped-audit-lifecycle-fix.md`: native getLastDamageSource()
can clear expired cache and lifecycle evidence lacked bounds/Mob/persistence/
current flags/direct-source-lastHurt UUIDs/native damage key+stamp. Thus actual11
SCRIPT_PASSED/full6/10seen remain historical, not export permission. Root fixed
CLIENTTESTONLY observer: direct read-only fields + native40tick expiry locally,
no getter cache write; complete stack, exact owned old-TC/BE/fixture/device cells,
current+initial unnamed/nonNoAI/nonpersistent/invulnerable identity; pure living
field/bounds snapshot (no saveWithoutId). Logs all required native evidence and
strict ordered vanilla caller. No server/gameplay production edits/refill/respawn.
Sources refrozen for fresh client12 snapshot/compile-before-game; user authorized
foreground click may be needed again. Auditor main still unexecuted; no successful
candidate now satisfies final evidence schema until fresh actual12 run. Need
all10newPNG/rootstopped-audit/finalFULL7+Python7 BEFORE any acceptance/export.

**Client11 exit0/SCRIPT_PASSED**, run `client-smoke-120d354b-898d-4197-85bd-27614f892982`,
fresh stopped clone `focal-smoke-ee44ac08-5a61-4027-a14d-acd117eeb8be`.
Actual 5598 contiguous SERVER_POSTs93–5690 through normal stop. All ten EXACT
PNG164–173 root-inspected; observations+hashes+explicit limits archived in
`.tmp/focal-fabric-client11-inspected.json`. Native112smelts/16coal/112XP,
both recipes/one8LEVEL Potency job/2400rawCV/13terra harvest/sameUUID pickup,
OLD3plusNEW1cobble4/108gold; server-ACK restored payer10/focus15/pick5/spare0.
Actual user-focused ownedHWND native Shift DOWN/UP, native wand Potency tooltip
visible at172 (incidental phial tooltip disclosed), real Shift-F route completed.
One old ambient skeleton has explicit actual `Mob.checkDespawn`/DISCARDED witness
outside owned areas, not fabricated preservation/recreation. Raw/tail archived,
ALL187acceptedparent hashes exact. No more game run is needed for this candidate.

**Final post-client FULL6 exit0, 238JUnit/ALL113Fabric/74Python93.272s PASS**,
3864unchangedinputs. Fresh JAR10,641,110bytes SHA256
`0104dc3756199192b56af2956a6f544702b2c6bb5848fc725d96ba4f051e3f10`;
notices/isolation/oneAW/art/recipes checked. Archive
`.tmp/focal-fabric-validate6-final-evidence/` includes fresh XML/JUnit/logs/input.
Still NOT accepted/exported until A's isolated ignored-auditor metadata/lifecycle
update freezes, root reviews it and actual stopped-NBT audit passes. Prepared
record/export guards are NOT acceptance by themselves. Source/production frozen;
no active Gradle/game after both completion notifications. Arcane Bore original/
pinned OpenTC4 audit delegated READONLY to ses_eb5c7203dffeqNezD1e4wIF71e, note-only;
no next-group active-source writes before accepted focal record/export.

**client10 command failed atcompile, NOTa game/clientrun.** SingleJAVAconstant
DamageTypes.OUT_OF_WORLD unavailableinMojmap1.21.1; rootcorrectedTESTONLYobserver
toFELL_OUT_OF_WORLD. No runClientSmoke doFirst/newrunID/worldplay/PNG/POSTevidence;
actualclient-smoke-result.json remainsFAILEDclient9ab06e469 andMUSTNOTbeusedas10.
Productionunchanged. Newfresh11snapshot+compile/game next; USERalreadyauthorized
manualforegroundclick, tellwindowwillappearaftercompile. Aignored-auditor edits
continueoutsideactiveinputs; no otherJava/Gradle/game/NeoForge. FinalFULL/Python
andactualclientnormalstop/10PNG/audit stillrequired; acceptedM09b retained.

**Client9exit1duringFURNACE_FILL**, runab06e469/private4288f5ed,1140POSTs/noPNG.
USERmanualfocusWORKED: actualWin32ownedHWND=foreground1444550, nativeShiftDOWN
andUPprovedBEFOREfixtures, source/savedXPnotnormalized. Newfailureoverbroadglobal
initialentityaliveguard: oneacceptedUNNAMEDAI/nonpersistent skeleton03377f92...
at1696.045,216,381.954 OUTSIDEownedareas gone. No harnessentityaction; exactnative
causeUNPROVENin9, notdeclareKilled/despawnasfact. Raw/tail9archived,parent187exact,
failedworldNEVERretryinput. RootNEWTESTONLYEntity.setRemoved HEADobserver(no
cancel/throw/move/remove/damage/reset) capturesactualreason/fullnativecallframes/
damage/position. EligibleONLYinitialunnamed/nonNoAI/nonpersistentMobsoutsideBOTH
initialandcurrentownedareas; allowONLYactualMob.checkDespawn+DISCARDED ORnative
tickDeath+KILLED/environmentDamageTypes/entitysourceNULL/noownerlastHurt. ALLother
permanentremovalFAILS inclnamed/NoAI/persistent/inownedareas. Explicitambient
lifecycle rows/coverage, no all-global-entities-immortalclaim. Rootnode1551rows now
alsoOBSERVEactualtype/modifier/cachedlock/regen/rechargeWait so originalnative
prune-drivenPALE/FADING/cachechanges can'tbeconcealed/reset. No productionedits.
Rootactiveclient10sourceFROZEN; A ownsONLYignored-auditor extension: nativeambient
reconciliation, ALLnamed/NoAIexactstopchecks, recognizedactualAABBcontactevents,
strictoldnode metadata/cache transitions. PreviousAfrozenauditorcfc521...reviewed
butnewAupdatepending; don'trunoldmainasPASS. Newclient10snapshot/compile/game;
USERmayneedclicknewwindowagain(within25s), keepactive. Same9000POST/450wallbound.
NootherGradle/game/NeoForge/newexport; currentacceptedM09b retained. FinalFULL/
Python+all10PNG+nativeSTOPPEDaudit+sourcebinding stillrequiredbeforeexport.

**USERanswered OSblocker: "Сфокусировать игру (Recommended)".** Userwillclickowned
Minecraftwindowandkeepforeground; retainFULLplannedShiftproof, no visualscope
reduction. RootonlyINPUT_CHECKwaitraised100→500clientticks(~25s) BEFOREfixtures,
explicitbeforelaunch/user-approved; nativepressed/released100ticks andoverall
9000SERVER_POST/450wallbudgetunchanged/noautotimeextension. LaunchFRESHclient9
fromacceptedM09bONLY withnewinputsnapshot, compilebeforegame. Telluserclickwindow
whenappears/keepactive; no manualvis/XP/resource actionneeded. SourcesFROZEN;
no concurrentGradle/game/NeoForge. A ignored-auditor workdoesnotmodifyinputs.
Client8/7/othersfailedworldsdiagnosticonly, NEVERretryinput. No currentnewacceptance/
export; latestpassedmain/native238/113/74 historicalFULL5+sameproduction0104dc...
FinalFULL/Pythonafteractualacceptedclient stillrequired. DistributionM09b kept.

**ACTUAL BLOCKER: Client8earlyINPUT_CHECKexit1**, run80c9ac2a/private757c42b2.
Win32deniedordinaryownedfocusrequests: ownedHWND1311002 vsforeground524388,
clientWindowActive=true,currentPID37232; exactownedprocesscheckpassed butnative
foregroundnevermatched. NOShiftkeypresssent, nofixtures/primary/payments/POSTs/PNG.
Raw +diagnostic archived `.tmp/focal-fabric-client8-{raw-report,diagnostic}.json`;
tailCORRECTLYabsent becausepreflightBEFOREobserverregistration, donotfabricateone.
ALL187parentfiles hashidentical; failedworlddiagnosticNEVERretryinput. No active
Gradle/gameafterexit(checkbeforefuturelaunch). **Pauseautomaticclientrelaunches:**
nativeOSinputgate needsUSERmanuallyfocusownedgamewindow, ORexplicitboundedvisual
scope decision. DoNOTAttachThreadInput/escalate/nativeforegroundbypass/fake
Screen.hasShiftDown/tooltip/playerflag/XP/vis; cannotclaimactualShiftPASS.
Ifuserreadyforforegroundclick, allowreasonablepreflightfocuswaitBEFORElaunch
(current100ticks~5s tooquick formanualclick; chooseexplicitmax500ticks~25s, unchanged
overall9000server/450wallbound; noautoextension duringrun), freshinput snapshot/
freshacceptedparentclone. Actual0level/total11 oldstock retainedasinputs. Current
productionmain0104dc... remainsunacceptednewJAR, distributionacceptedM09bbac83fff.
A ignored-auditor fixes stillindependent/notactiveinputs; waititsSOURCEFROZEN,
reviewall5fixes. Finalnativeclientall10PNG/stoppedaudit/freshFULL/Python required
before record/export. UserconfirmationnotnormallyneededbutTHISisactualOSblocker.

**Client7exit1 ATCAST_PICKED**, run91f714b8/private07a7a3cd,8PNG/5529POSTs;
allgenuinegamechain+sameUUIDcobblepickupagain, but STRICTWin32foregroundguard
rejectedShift BEFOREkeyevent. NativeHWNDnotverifiedforeground (oldexceptionhad
nodetails, cannotinferotherapp/user/permission). Raw/tail7archived,parent187exact,
failedworldNEVERretryinput. RootCLIENTONLYadds EARLYINPUT_CHECK/PRESSED/RELEASED
BEFOREprimary/raw/blockfixtures/payments: actualownedHWNDcurrentJVM PID check,
ordinaryGLFWfocus+User32.SetForegroundWindow request ONLYthatgamewindow, wait
actualWin32+GLFWfocus, SendInputLEFT_SHIFT andrequireScreen.hasShiftDown true THEN
false.100clientticksperphase finite; no thread-inputattachment/escalation/fakeflag.
Detailedowned/foregroundHWND/clientWindowActive/PIDfailurestate added. LateCAST_
PICKED waits actualforeground too. IfOSdenies, failsEARLYnotafter4minofgameplay.
No privilegedworkaround; nativekeyboard/preflightnotyetproven. Allactiveinputs
refrozen; freshclient8snapshot/compile/game next. Aignored-auditorworkstillisolated.
Production0104dc... unchanged, acceptedM09b/187parentpreserved; noNeoForge/export.

**Client7 RUNNING** shellsh_14a16ae29001FJBs7xE0ffQHng; inputs snapshot
`.tmp/focal-fabric-client7-inputs.json`. RootNEWTESTONLYOSShift usesKEYEVENTF_SCANCODE
LEFT_SHIFT0x2A down/up viaexistingJNA, exactforegroundHWNDguard; noAWTmodechange,
gameflag/tooltip override. AllactiveinputsFROZEN; doNOTlaunchduplicateGradle/game
orreadpollactiveoutput. A editsONLYignoredauditor, notactiveinputs. Preparedroot
`.tmp/record_focal_client.py` and `.tmp/export_focal_binary.py` syntaxparsedONLY,
NOTEXECUTED; guardsneedactualSCRIPT_PASSED/normalstoptail/10rootinspected164–173/
independentstoppedNBT/parent187hashes andfinalFULL238/113/74. Exportwouldbackup
M09bbac83fffOUTSIDEmods atbackups/visnet-fabric-validate7/ ONLYafteracceptance;
thatbackup/newexport NOTyetcreated. CurrentM09bdistributionunchanged. Waitactual
client7result +Aauditorfreeze; rootinspect/audit THENfinalFULL/Python/record/export.

**Client6exit1 ATCAST_PICKED**, runc2df28bd/privateaacf8423,8PNG164–171/5523POSTs.
ALLgenuinechainagain inclnewcobbleUUIDcontactpickup/OLD3plusNEW1=4,112XP,
2250+2880all6/8LEVEL/2400rawCV/13terra; failedAWT Robot: explicitAWTException
headlessenvironment (Minecraft desktop usesGLFW despiteAWTheadless). Raw/tail6
archived,parent187exact, failedworldNEVERretryinput. RootNEWTESTONLYFocalNativeShift
usesEXISTINGMCJNA5.14+Win32SendInput (noaddeddependency), exact foregroundHWND via
GLFWNativeWin32+User32.GetForegroundWindow requiredbeforekeydown; pairedkeyup
cleanup. NativeGLFW+Screen.hasShiftDown+realShiftFserverC2Sstillmandatory, no fake
tooltip/gameboolean/playerflag/XP/vis/counter override. AWTglobalheadlessNOTchanged.
Client7freshsnapshot/compile/game next; backendUNVERIFIEDuntilactualphase.
Read-onlyAauditorreview found5actualpreparedscriptissues: old1551NATIVEprune/
regenproducesmixedBASE76..79/currentaer1aqua1 (notuniform79/current0), energized
nodeIdnativeblank notoldscanUUID, allowedfixturecellsunchecked, rawworldbinding/
modeInvulnerableEnderItems missing. AassignedONLYignored `.tmp/audit_focal_stopped.py`
forstricttracechain/laststate/schema/all73fixture+5earned exactstate/rootbinding/
playerfields fixes; nocode/gamerunsbyagent/no PASS mainonfailedworlds. Rootdoesnot
editthatfileuntilAfreeze. NativeOBSERVEoldpruneboundaries1200/2400/3600/4800recorded
BEFOREfailure, notnumericwaiver/clockreset. Finalauditorreview/executionstillpending.
Noexport/NeoForge, distributionacceptedM09b/parentpreserved; finalFULLafterclient.

**Client5exit1 ATCAST_PICKUP**, runa4dc0c62/private93ffa25b,8actualPNG164–171,
5512POSTs. ALLgenuine112smelts/16coal/112nativeXP HEADRETURN→LEVEL8,total123;
bothrecipes2250+2880all6; twoearneddevicesnativeUUIDminingpickuprelocation/pick3→5;
actual1001warmup/sourceBASE80; ONEbound8LEVELPotencyjobALL2400rawCV; real0/.5/1/
1.5→nexttickprotected13terraharvest. FinalnewcobbleUUID left ONEcellfloor, actual
stoppedposition1607.178154,211.544988,382.748176/nativeMotionunchanged: no supported
playercontact path, strictfail ratherthanmoveitem/fakepickup. Raw/tailclient5
archived; FAILEDworldNEVERretryinput; parent187stillhashidentical. RootDIAGNOSTIC
ONLYinspected3actualfailedrunPNG166/167/170(model/tooltip/remainingbar visible,
darkness/smalltext/fidelitylimits). Not10PNGacceptance/noclaimall8seen.
RootCLIENTONLYapproved bounded5x5catchfloor1603..1607,y215,z379..383 (24additional
AIRcells), no entityposition/velocity/pickup/refund setters. All78finalfixture/
earnedcells independentparentAIR/noBE `.tmp/focal-stopped-cell-preflight2.json`;
liveguardsmandatory. Auditorupdatedexact73fixture+5earnedcells/old3+new1cobble.
Added B-advisedSTRICTserverunfundedsession-losswitness/fail immediately (actual
ray/session/useitem/rot+client-thread-publishedinputsnapshot), neverautorestart/
reselect. Currentclient6tofreshsnapshot/compile/game; productionstill0104dc...
NoJavaactivebeforelaunch/checkfirst; needactualall10PNG/clientpass/normalstop/
independentNBT/finalFULL+Python BEFOREexport. AWTRobotShiftstillNOTproven(nextphase).

BREADONLYclient4diagnostic COMPLETE (noedits/execution):151actualtaps/15100CV,
lastgrant129827 then377VALIDPOSTsthrough130204 withnogrants. A sustained hand4
cannotexplain377ticksbecauseChargechecksobjectbindingBEFOREeveryPOST. Combined
failurepredicateunidentified; stoppedselected4 MAYhavebeenflushedDURINGcleanup
releaseUsingItem()->ensureHasSentCarriedItem, notnecessarilyprefailureproof.
Thus origin/stall UNSOLVED, noassertphysicaluserinput. Concrete nativepath:
queuedhotbar5key ORMouse.onScroll maychange3→4; gameMode.tick onlysendscurrent
selection. Separateunfundedstalls arepossible becauseChargevalidatessessiononly
whileisUsingItemtrue, thenwaitsforeverafterrelease; focus/screen guard/native
mousebuttonreleased keyUse/nativechangedray maystopchannel. Currentrootclient5
adds focus/screen failfast+bindingscroll/keyguard, but NOTyet general once-per-
transition server session-losswitness. If5fails, afterstopadd strict losswitness
withactualray/session/playerrot/useitem/clientpublishedsnapshot BEFOREcleanup;
neverauto-reselect/restart/refill. Keepnewscopedinputguardsphase-based ifneeded.
Client5currentlyRUNNING/sh_14a06e118001UE75nINjKOQDgN; doNOTeditactiveinputs orlaunch
otherGradle/gameuntilcompletion. No sourcechangesbyB. Parent/binarypreserved.

**Client4exit1/SCRIPT_FAILED duringgenuineCHARGE_LIVE**, run8fbf93c5/privatecd61eab9.
Actually8nativefurnacesfilled,64nativegoldsmelts/13coalignitions/2364POSTs, real
wandtaps(no XP/outputfixture), thenSTRICTboundhandfailed. NativeSTOppedNBTselected4
whileexactpayerstillSlot3, unchangednon-CVcomponents/healthy1623scanId+BASE80;
lastvisAER6400/AQ4800/IGN4700/ORD3900/PD2500/TERRA4100. Lasttap129827 vsPOST130204
showspriorchannelstall; originofselection4UNPROVEN (notclaimuserorproductionbug).
Raw/tailclient4archived, nocaptures/acceptance; parent187hashesexact, failedworld
NEVERretryinput. B READONLYdiagnostic ses_eb6551174ffevAGMKuvjPflzwI active/noedits.
RootCLIENTTESTONLYadds detailedstrictbindingwitness, fail-fastunrequestedrelease
withwindow/screen/selectedreason (previouscodecouldstallsilently), queuedhotbar
key suppression/log ONLYownedcharge/cast/mining, TESTONLYMouseHandler.onScroll
isolation/log ONLYsameownedwindow/activephase (scriptneverneedshotbarscroll).
No serverhand/stack/CV reset/reselection and NOidentityfenceweakening. Newmixin
client-onlytestconfig, productionmoduntouched. Activeclienttestfrozenforfresh
client5snapshot+compilebeforegame; finalFULL/Pythonrequiredafteracceptedclient.
NoNeoForge/newexport; distributionM09bbac83fff retained.

**Client3exit1/SCRIPT_FAILED beforefixtures/smelting/POSTs/captures**, run0009f8a5/
privatebd6b91a9. Mandatorycow1628.5notyetloaded instartup playerchunk97 vsfar101;
stoppedparentcowexistsunchanged, no entityclearing/movementauthorized. Raw/tail
client3archived; parent187exact. RootCLIENTONLYadds SCENE_READY bounded500client
ticks/maxsame450wallbudget: PUREgetChunkNow+actual3cow queries until naturally
loaded, THEN originalstrictUUID/name/health/positionpreservationguards/setup.
Neverforce-load/recreateanentity/chunk/resetworldclock. RootFOCAL-only owned
optionsview/simradius8(native setting, was4/5) coversactual87-blockwide two-device
mines/source/eightfurnaces so ledgerneednotpretend unloadedfurnacestick. Other
scopesunchanged; naturaloldnodes maytick/pruneasoriginal, observations/auditretain
strictboundaries. DiffvsFULL5 nowONLYclientsequence +fabric/build.gradle (owned
focalscopeoptions), production/JUnit/native/Python stillunchanged/JAR0104dc...
Refreeze/focusedclient4 snapshot+compilebeforegame next. FinalFULL/Pythonafter
acceptedclient remainsmandatory; no newexport/NeoForge/acceptedPNG.

**Client2exit1/SCRIPT_FAILED beforefixtures**, run03eb9cea/private58b2716b.
No coal/cobble0guardrejected: ROOTactualALL26stacks confirms OLDcobble3Slot5,
coal0. Raw/tailarchivedclient2, nocaptures/POSTs/newknowledge/smelting/crafts;
failedworldretainedNEVERretry. Exactfullinventory/15freeinputs/nativeXPpreflight
`.tmp/focal-actual-parent-inventory-preflight.json`, all187parentfilesunchanged.
RootCLIENTONLYcorrectedold3preservedPLUSONErealUUIDoutput: everyPOSTexpectedold
3+actualpaidcast0/1, actualcontactrequiresinventory4, restore/serverfinalSlot5=4,
finalNEW1/OLDplusNEW4explicit. Auditor expects3old+1new, no newcobblegrant/drop
discard/stackrewrite; production/JUnit/native/Pythoninputsunchanged. Allother
initialstockassumptionsnowcheckedread-only: spare1/threewands/pick3/absentcoal/
rawgold/gold/charm/machine, actualfree20..22,24..35. Sourcesrefrozen; focusedfresh
client3 snapshot/build (compilebeforegame) thenfinalFULL/Pythonmandatory. No
NeoForge/newacceptance/export. Current acceptedM09b/parentpreserved.

Retryscopecorrection: ROOTverified exactfingerprintdiffvsPASSvalidate5 isONLY
FocalManipulatorClientSequence.java; production/JUnit/native/Pythoninputs/JAR
unchanged. `.tmp/focal-client2-focused-retry-guard.json` recordsproof. Perfocused
checks priority doNOTrepeatfull113/238/Python justforclientguardcountfix. Launch
freshclient2 (runClientSmoke dependsclientTestClasses, socompilescorrectedharness
BEFOREgame), newclient2inputsnapshot. This doesNOTclaimfreshfullvalidation; final
FULLvalidate6/Python6 mandatoryAFTERactualclientbeforeacceptance/export. Newfresh
clonefromacceptedM09bONLY; previousfailedb71worldretainedasdiagnostic,neverinput.

**Client1exit1/SCRIPT_FAILED beforefixtures.** Runb5509655/privateworldb71a0519,
firstSETUPguard actualoldspares. ROOTreadnativeacceptedlevel.datconfirmed
**ONErelay atSlot3**, not3relays: priorplanmistookinventoryslotindexforcount.
Threewands/pick3 correct. No fixture/knowledge/craft/XP/vis changes/captures/client
acceptance; raw/tail `.tmp/focal-fabric-client1-{raw-report,tail}.json`, failedworld
retainedNEVERretryinput. Nativeprocessstopped; ALL187parentfilesstillhashidentical.
RootCLIENTONLYfix explicitinitial1 +liveinventory/newworldrelayconservation1,
final0spares/originalslot3EMPTY; no additionalrelaygrant/refill. Splitinitialguard
withactualcount. AuditorhandlesactualspentemptySlot3; fixtureplanamended. All
productionunchangedsincevalidate4/5(0104dc...); neednewclientinputbuildcheckpoint.
FreshsnapshotFULL`focal-fabric-validate6`/Python6 thenfreshclient2 ifPASS; noJava
activeatfix/noagentsediting/noNeoForge. Newacceptance/exportstillpending.

**PRECLIENTvalidate5PASS actual238JUnit/ALL113Fabric/74Python97.772s/3861unchanged.**
FreshproductionJAR same0104dc375619...10,641,110bytes/notices/isolation/oneAW/art/
recipesPASS. Beforeclientarchive `.tmp/focal-fabric-validate5-before-client/`
containsactualfreshXML/JUnit/log/exit/input/JARinspection/Python. Acceptedparent
ALL187files rechecked +currentdistributionM09bbac83fff rechecked. NoJavaactive.
SourcesstillFROZEN, allagentsREADONLYdone/inactive. FIRSTactualfocusedclientlaunch
`focal-fabric-client1 -Scope focal` now starts, hard9000SERVER_POST/450wallseconds,
no timeout extension,10captures164–173. Waitactualcompletion; doNOTlaunchother
Gradle/game. Needrawreport/independenttail/ALLexactPNGinspection/stoppedNBTbefore
acceptance; finalFULL/Pythonafterclient. No newexport/NeoForge. RunnerAWTRobot
ownedphysicalShift capabilityunverifieduntilactualphase, cannotfakefallback.

**FULLvalidate4actual238JUnit/ALL113Fabric/74Python101.380sPASS/3861unchanged**,
freshJAR10,641,110bytesSHA2560104dc3756199192b56af2956a6f544702b2c6bb5848fc725d96ba4f051e3f10,
ZIP/notices/isolation/oneintermediaryAW/originalart/recipesPASS. PRECLIENTONLY,
NOTaccepted/exported. Subsequent INDEPENDENTREADONLYreviews foundCLIENTONLYissues:
A STARTcounterpublicationrace, serverfinalpayer10notactuallychecked, postcomplete
jobguardtautology; C pristinevanillafurnaceCookTotal0untilactualinput, and missing
STARTXpTotal/Progressinvariance. RootfixedALLactiveCLIENTONLY: volatileintentbefore
GUIclick, actualserverexactALLrestoredslots+chargedpayer5130all6/13terra witness,
strictzerojobaftercompletion, pristine0→nativeinput200one-wayguard/neverclocksetter,
exactnative8LEVELdebit leaves total/progressunchanged. No productionedits; staged
templatesremainhistorical. Newactiveclienthashneedfreshsnapshot/build. ALLsources
refrozen; nextFULL`focal-fabric-validate5`+Python5 thenFIRSTactualfocalclient1 ifPASS.
Noagentsediting/Javaactive. Parent187files+acceptedM09bdistributionunchanged.
Preparedindependent `.tmp/audit_focal_stopped.py` guardedNOTEXECUTED; auditor is
notclientPASS. Requiresactualraw/tail/stoppedclone/187hashes, allolddevices/stocks/
cows/full54cellplan,112genuineXP/108gold/focus15/payer10/noqueues. Productionjar
shouldremain0104dc...sinceALLfixessincevalidate4 areclienttestONLY.

**`focal-fabric-compile2`exit0** including promoted3clientfiles/glue. Root stopped
cell preflight caught oldemptyarcbench1618,216,377 under proposed camera: preserved
it, activeCLIENTONLYrelaycamera now1618.5,218,376.5/column1618,216..217,376. Also
oldworkshop has isolatedpads, notcontinuousfloor;19EXACTpreviouslyAIR215stonefloor
supports nowDECLARED for furnace/camera/stone/torches (no broadfill/nooldreplacement).
`.tmp/focal-client-fixture-plan.md` amended, `.tmp/focal-stopped-cell-preflight.json`
independently proves ALL54finalfixture/earnedcellsAIR/noBE + ALL187parentfilesexact.
Liveentity/collision/loadedguards stillmandatory; no runtime/saveedit/PASS inferred.
Staged8b711...is historical; activeclientcopydiffers ONLYtheseexplicitfixtures/camera.
ALLsourcesrefrozen, readonlyAreview/CXP reviewnoedits. LaunchingfreshFULL
`focal-fabric-validate4` +Python4 beforeactualclient, expected238/113/74NOTresults.

**B STAGEDSOURCEFROZEN received/promoted exact3files**, manifest
`.tmp/focal-client-promotion1.json`. Active newseq8b711537.../XPobserver9652aa42.../
TESTONLYmixin34ebf780..., no production changes. Root wired FOCAL-only Book branch
BEFORE other scopes' fixtures/XP initialization, native capture+exact167hover,
InventoryScreen actual Shift-wand hover, focalcheck during PNGwait, successclose
preserving normal-stop tail/failurecleanup. TESTONLYclientmod/mixin metadata, focal
Gradlescope/freshacceptedvisnetparent clone/tenPNG/strictnormalstoptail finite112XP/
108gold/2400rawCV gate, PSscopeadded. Key final_native_finite_state matchedactual.
A independentlyREADONLYreviews recipe/source/START/restore/ledger, C independently
READONLYreviews nativeXPobserver/furnaces; NO sourceedits/buildsbyagents. Root
reviewedadditionalfixtures/actualnativeC2S/actions/cleanup/RobotShiftfocusguard.
AWT Robot remainsunverifiedrunnerdependency(ownedwindowonly). NoJavaactivebefore
promotion. ALLactiveinputsFROZEN: next`focal-fabric-compile2 -CompileOnly`, THEN
freshFULL+Python before ANYclientlaunch. Noactualnewclient/PNG/audit/PASS/export.

**`focal-fabric-native3`exit0/ALL113requiredPASS/0failures/errors/skips.** Root
validated actualfreshXML andALL3857unchangedactiveinputs; archived
`.tmp/focal-fabric-native3-gametests.xml` and `-native-inspection.json`.
NewrealonRemove false/hand-swap/throw/recovery cases ALLpassed after testONLY
unconditional-cobbleloot correction; production5faec33f... unchangedsinceCfix.
All105oldcases plus8new now nativePASS. This was **GameTestOnly**, NOTnewJUnit/
Python/freshremappedJAR/client acceptance. Latestfull2 had238JUnit/74PythonPASS on
its historicalinputs; mustrunfreshFULL+Python AFTERBpromotion andfinalafterclient.
OnlyBactive: ignoredSTAGING `.tmp/focal-client-src/` (notactive source set, noJava/
build/savewrites). EarlyAPI `.tmp/focal-client-sequence-api.md`; rootfutureglue
notes `.tmp/focal-root-integration-plan.md`. WAIT itsSTAGEDSOURCEFROZEN, rootreview,
then promote+wireBookscope/freshacceptedparent/TESTONLYXPobservermixin/PSrunner,
freshFULLbeforeclient. DoNOTlaunchclientfromstaging/mockcompletion. Parent187files/
distributionM09bbac83fff retained. NoNeoForge/commit/push/release/newexport.

**FULLvalidate2exit1:**238JUnitPASS/113native112PASS/ONLYnewpartial-removalprobe
FAIL/ALL105oldPASS/74Python95.952sPASS. BothpriornewfailuresFIXED. NewnormalFALSE
reproduction reachedretained70vis/DIRT/revokednextcandidate/primaryAFTER; composite
lootassertfailed. Rootchecked ACTUALcached1.21.1JAR: TNTloot requiresACTUALTNTblock
andunstable=false, soNEWcustomprobe's dropsLike(TNT) cannotbe a positivedropcontrol.
RootchangedONLYNEWhelper to vanillaCOBBLESTONEtable (no blockstate gate), expected
physicalcobble1/2; splitAFTERvsdropassertions WITHactualcounts, no production/drop
gate weakening. Currenthelperf650c07bd30386c40d4f7b40ef0fcb37c83458465cd9840f5762f118f27e3459;
C55581b...hashhistorical. Allreservations/source/oldassertionsunchanged. Archived
`.tmp/focal-fabric-validate2-partial/`; unacceptedproductionJARNOTexported.
ACTIVESOURCESrefrozen; nextFOCUSED`focal-fabric-native3 -GameTestOnly` withfresh
inputsnapshot, NOTa full238/74/JAR revalidationclaim. AfterBstagingpromotion need
freshFULLandPythonbeforeclientandfinalafterclient. Bonlyignoredstagingcontinues/
cannoteditactivefiles. No gameclient/saveopening/PNG/acceptance/NeoForge yet.

**CfixSOURCEFROZEN/rootreviewed3files.** Harvest5faec33f9692..., oldNEWupgradehelper
039a3532ebe2..., NEWearlyprobe55581b78ddc1... exact`.tmp/focal-reservation-fix.md`.
Unsafe refund path DELETED; delegatednativeFALSE/throw retains/logs exactoriginal
reservation ANDrevokescurrentexpansion; predelegatecancellation/no-match stillfree.
Successful-removal witness/drop gatingunchanged. NewEARLYregisteredprobe usesactual
onRemove AIR→DIRT replacement/nextcandidateexposure andnormalfalse, handswap,
throw+cleanrecovery scenarios; no lateBlock/fakeoutputs/nativepassclaim. Native
ReportedExceptioncause+report.originalidentitycatchfixed; oldAFTERtestsnowreachable.
Afixe77fbc71...alsoSOURCEFROZEN. Rootearlybootstrap+113manifestwrappersready; ALL
ACTIVESOURCESETS refrozen. B writesONLYignoredstaging templates, NOTactiveinput,
not a validationblocker. NoJava/gameactive. FreshFULL `focal-fabric-validate2` +
Python2 launching, expected238/113/74NOTexecutionresults. No clientworldopened/
newPNG/acceptance/export/NeoForge; distributionM09bbac83fff and187parentfiles kept.

**FULLfocal-validate1exit1/UNACCEPTED:**238JUnitPASS,112native110PASS/2NEWfail/
ALL105oldPASS,74Python95.976sPASS. RawJUnit/XML/partialresults archived
`.tmp/focal-fabric-validate1-partial/`; unacceptedJAR2a9d543a...10,642,452bytes
NOTexported. A postdebitfixture relay ticked BEFORE freshsource registration;
AfixDONE/sourcefrozen e77fbc71b43a... source-firstnativegraph tick+zeroepoch and
oneexactregisteredroot/reciprocaledge assertions; production unchanged forAfix.
Cactualtestfail confirmed nativeReportedException wrapping BREAKfixtureerror,
not fixture's directlycaughtIllegalStateException; exactwrappercausefix assigned.
B independentreview also found REALproductionreservationrefundunsafe: vanilla
removeBlockfalse canFOLLOWonRemove partialmutation. C ownsONLYHarvest+NEWhelper
andoldNEWupgradehelper: retain/logindeterminate delegatedfalse/throw (no refund),
allpredelegate cancellation/no-match stillfree; addEARLYregistered actualonRemove
mutation/false regression andexception iffeasible. RootwiredproposedearlyAPI
registerEarlyProbe()+run intoisolatedGameTestBootstrap/wrapper/manifest now113
(all105old+8new). NOGradle/game untilCexplicitSOURCEFROZEN+rootreview.
B nowONLYignoredSTAGING `.tmp/focal-client-src/` forfuturegenuine112smeltXP/client
sequence+readonlypickupobserver; noactive source-set/production/meta edits. This
staging is NOTcompiledoraccepted and doesNOTblockcorrectedproductionvalidation.
Rootowns promotion/Book/Gradle/mixins/recorder/export AFTERstagedfreeze/noGradle.
Approvedfiniteplan `.tmp/focal-client-fixture-plan.md`:9000ticks/450s/10captures
164–173, rawgold112→genuine112XP+112ingots→4paidgold/108retained; no XPwrites/orb
grants, saved0level/total11/progress.5714 preservedasinputs. All187parentfiles and
acceptedM09bdistrib unchanged. No newclient/PNG/PASS/export/NeoForge.

**`focal-fabric-compile1`exit0:** actualcommonmain/fabricclientTest/fabricgameTest
compiled, NOunit/nativegame/clientexecutionyet. AllsourcesremainFROZEN; only
requiredmanifestdescriptionupdatedtoexplicitboundedfocal scope. Nextfreshsnapshot
`focal-fabric-validate1` FULLFabric+Python1, expected238/112/74NOTexecutionresults.
No currentclientfixture/harnessapproved; readonlyEnlargeindependentreviewcontinues
withoutcodeedits/builds. AcceptedM09bbinaryand187parentfilesstillpreserved.

**Focal ALL3ownersSOURCEFROZEN/rootintegrationFROZEN for first centralcompile.**
Rootregistered112required=all105acceptedIDs+7new,7distinctisolatedbatches, no late
probe bootstrap needed. ExpectedFULL counts238JUnit/112Fabric/74Python are source
expectations ONLY, not results. B readonlyindependent Enlarge-reservation review
ses_eb6551174ffevAGMKuvjPflzwI is notbuildblocker/cannotedit. Otherownersinactive.
Root integrated tick-only CLIENTeffects BOTHblocktickerandtilehook; existing-instance
NBT update no longer resets unsavedfocalcounter (newBE naturallystarts0). Original
5-slot sorting restored EVENunupgraded (`BE-1-1-1-1-1`), old3native helpers ONLY
exactsortassertions strengthened tooriginal keys, nocase/otherassertions removed.
Root current hashes: Blockc22de9a5eadb..., Tilea53f2f29ce63..., Basic2fa2848a60c4...
versusownerfreeze3historicalhashes; allotherownerfiles preserved. Resourceconverter
actual filechecks PASS506sprites/336refs/228modelrefs/111sounds/missing0, not native
tests. All3distribution+8backupJARs remainacceptedM09b; accepted187worldfiles frozen
`.tmp/focal-accepted-parent-before-client.json`. NoJava/gameactive beforelaunch.
Launching `focal-fabric-compile1 -CompileOnly`, no fullchecks/client/pixels yet.
Finiteclient DRAFT `.tmp/focal-client-fixture-plan.md` includes genuine vanilla
furnace112XP route; savedXpLevel0/total11/progress.5714 neverdirectlychanged. Root
read-onlyNBT cell auditfoundall15candidatecellsAIR/noBE, plusTWOadditionaloldnamed
health6cows1596.5,216,381.5 and1628.5,216,369.5, preserveALLinheritedentities.
Ordinarynode transientcount is NOTsaved: nativeTileNode.load resetscount0 itself;
do NOT assume1091continues acrossrestart or imposeascriptreset. Observeactual
fresh-copy phases/pruning naturally. Newclientfixture notapproved/implemented.

**17.10.2026 M09b ACCEPTED/EXPORTED.** FinalFULL `visnet-fabric-validate7` AFTERclient:
220JUnit/ALL105Fabric/72Python95.133s/3827unchangedinputs/0failures/errors/skips.
Client5/tail/stoppedNBT/ALL10exactPNG154–163 accepted as described below; record
`docs/porting/visnet-client.json`, verification current=true/historical=false;
immutable accepted copy `.tmp/visnet-accepted-verification7.json`. Guarded exporter
ran successfully: current distribution **10,556,826bytes**, SHA256
`bac83fff54504682f7a00523d22f10e00a334eb922f559f93ea27bf349408caa`;
previous M09aebf040e7 backed up OUTSIDE mods at `backups/stabilizer-fabric-validate2/`.
Both dependencies/older backups retained; README/SHA256SUMS updated. No NeoForge,
packaged/EULA server/remote/full survival/audio/full visual parity claim.

**Next planned bounded Fabric group started immediately:** focal manipulator +
Excavation upgrades, before Arcane Bore. All3 READONLYaudits complete; implementation
assigned with non-overlapping files/API. A core+SUMplanner/nativefixtures, B GUI/BER/
new focalresources+narrowItemWandCasting SHIFTtooltip, C immutableupgrades/all4generic
effects+ExcavTreasure/Enlarge/harvest+tests. All3 IMPLEMENTING; NO Gradle/game until
ALLSOURCEFROZEN including client sources. Root owns registry/resources/build/network/
wrappers/evidence. Do not
allow paid inert upgrades; preserve exact original timing/XP/vis/rank rules and
no resets/refills/finished-output grants in focused client. Accepted M09b returned
ordinary node has six zero-current keys/count1091: original pruning at1200 is REAL,
so subsequent private-copy use must observe natural consequences, never reset it.
Accepted saves/183 parent hashes remain read-only. No concurrent Java/Gradle/game.

Newgroup audit/ownership/recipe closure notes:
`docs/analysis/2026-10-17-focal-manipulator.md`; read-only sessions
ses_eb6551179ffei3AdCR4G2twV8G (core), ses_eb6551174ffevAGMKuvjPflzwI (client),
ses_eb6551122ffeD0gDd69KYXUa0W (upgrade data/application). Primal charm recipe is
a missing real dependency; original charm onUpdate orb/hint are NOT automatically
covered by adding its recipe. RootSOURCEONLY registry/component/bootstrap/2recipes/
book+icon/charmstack1/START27byte+ACK36byte transport/S2C correction/ENRU+wandtable
atlas wired; sixnewJUnitpayload+2Pythonresourcechecks authoredNOTRUN. Supported
Frugal/PotencyALL4 +ExcavTreasure1/3/5 Enlarge2/4; paidotherbranchesREFUSED. Early
APIhandoffs `.tmp/focal-{machine,client,upgrade}-api.md` pendingowners. Original
replacementfocus free-vis bypass/crash closesbycancelbeforedebit/no refund, original
XPlevels/creative threshold/SUM/5tickphase/noopenmenutickgate retained. Rootnetwork
uses A menu.sessionId()/selectionRevision()/requestStart(sender,UUID,revision,id);
B pendingACK nooptimisticwrites. Actualcompile/native/clientNOTRUN/NOTACCEPTED.
All3 current
distribution hashes andALL8backupJARs checked vs their own manifests;23stopped
acceptedworld files rechecked. Evidence `.tmp/visnet-accepted-export7.json`, final
XML/JUnit/Python/logs archived `.tmp/visnet-accepted-final7/`. Inventory refreshed
324active mainJava (not percent); scope1044needs_review/metadata checkPASS.

**Newclient XP correction:** root independently read actual accepted M09b level.dat:
`XpTotal=11` BUT `XpLevel=0`, not11levels. Prior newgroup first-rank-affordable
assumption is WRONG; NO client XP grant/field setter/orb fixture permitted. Need
genuine native XP acquisition (e.g. declared rawgold/fuel→ordinary furnace smelting→
real output take→actual orb pickup) before rank1cost8levels. Existing accepted
M09b XP-total preservation remains valid; it never claimed manipulator purchase.
Accepted knowledge includes NODEPRESERVE and BASICARTIFACE, not FOCALMANIPULATION.
No client sequence written/approved/launched yet. Root NEW recipe native helper
`FocalRecipeRuntimeChecks.realCharmAndManipulatorPayments` tests BOTH real recipes,
same actually earned charm consumed, 2250+2880/all6, FrugalNOTrecipe discount,
missingresearch/ingredients/oneCVshort/fullinventory/stalepreview; sourceONLY,
wrapper/required case registration pending ALLowner APIs. No tests executed.

BproductionSOURCEFROZEN received,9exactfiles listed `.tmp/focal-client-api.md`.
Rootreadreviewedall4newJavafiles; source-only/UI/FX runtimeunknown. Needs A/root
`TileFocalManipulator.ClientEffects.tick(Level,BlockPos)` + staticsetter invoked
ONLYactiveclienttick, NOTBER. RootalreadywiredClientSetup.init and ALL6actual
ENRUstatus/partial/invalidkeysmatchingB/C. B nowindependentREADONLYreview ofROOT
START/ACK/network+crafthelper (cannotedit/build). A/CstillIMPLEMENTING/no source
freeze. CearlyAPI IDs0..3only/closedcatalogue +pure/synced5shortdata; root must
review its unupgraded-sort compatibility choice against original5×-1 beforefinal.
NativeXP fields precisely0levels/total11/progress0.5714285969734192 (initial state
notnormalized).112genuine furnaceXP proposed; no directfields/orbs. Bproposal
old11→3levelsINVALID, explicitlynotaccepted. Actualfiniteclientplanstillpending.

The chronological M09b execution notes below are historical, newest status above.

**Client5SCRIPT_PASSED/exit0**,30c55f15/privateworlde0a10f37,255.1941779s/5103client
ticks/5080ledgerPOST. RootALL10exact154–163PNGinspected (rain/glare/smallrelayGUI/
horizontalrecipeiconsnotfullyresolved/fullmotion/audioNOTclaimed). Real3prices/
twoOTHERnodecharges,1001/951/zero-pulse→actual1472refills/282rawEARTH,3x12steps/
60ticks94each+1FULL200steps/1000ticks0,FOURgenuineUUIDoutputpickups→old+new4+4.
Independenttailthroughnormalstoplastcount1091<1200/BASE80/NORMAL/NONE/sixzero
currentkeys, nativeTime127814. IndependentstoppedNBTauditPASSED95non-nodeBE/
20jars333/72candles/old89exactexceptactualmachine1881→1565+emptyvalvepressure;
alloldfurnacesB0,oldwands/foci/gear/phials/pick3/XP11/onlyVISPOWERknowledge,
namedNoAIcowUUID/name/health6/position exact/queueempty/parent183filesunchanged.
`.tmp/visnet-fabric-client5-{raw-report,inspected,native-stopped-audit}.json`,
`docs/porting/visnet-client.json` recordedwithsource/tail/save/PNGhashguards;
clientrecordcurrentinputsverified. No newsourceedits. **FinalFULLvalidate7 rerun
AFTERclient+Python7 next**, preclientreportsarchived; accept/exportONLYifstill
220/105/72/3827unchanged. DistributionM09aebf040e7unchanged/noexportyet/noNeoForge.

**FULLvalidate7exit0/current3827unchangedinputs:**220JUnit/ALL105Fabric/72Python
93.940s/0failures/errors/skips; JARbac83fff...10,556,826bytes/freshXML/isolation
checked. EarlypowerREADONLYauditDONE: NOconverterselfpower/autocheckpowerwrite;
vanillaqueued/heldUSEcanbypassoldCLIENT_POSTscript andnewPREguardcoversit, but
originofoldinputstillunproven. ScriptedchargealreadyreleasesheldUSE; nohuman
interferenceclaim. Ifrecurs, needactualupperleverPOWERED+directionalsignals.
Noagentsactive/no sourceedits; launchingfreshparent `visnet-fabric-client5`.
Allbeforeclient7reportsarchived/183parentfilesmustremainexact; client5/pixels/
1001/951/282CV/baseline/tail/stoppedNBT/acceptance/exportstillpending.

**ResumedafterOpenCoderestart; NOJava/gameactive.** Client4toolcancelled/restarted,
actualexitNOTcaptured/nofabricatedexitJSON. Realrun896d3ad5/worldb88a1815saved
SCRIPT_FAILED/preservedraw4/four154–157diagnostics. Allcrafts/physicalconsumer/
UPgate/upperlever/relayinstalled, but atPOWER_USE nativeupperpoweredBEFOREC's
scriptedrequest: actualordinary1072/converter1 whileCrefusesphase>100; noPOWER_ACK
log. Strictalignmentcorrectlyfailed (no clock reset/waiver). CnativePostthen
self-unregisteredINSIDEliveArchitecturytickdispatch→secondaryCME/crash. RootONLY
Csequencefix: failednativePostnowinertflagONLY; cleanupqueuedbyclientAFTERdispatch.
ClientPREsuppresses/logsunscriptedheld/queuedUSEoutsidegenuinecharge andATTACK
outsidegenuinemine; scriptednativeactionsunchanged. NewupperPowerRequestedproof
rejectsanyunrequestededge; allphase<=140/1001/951/sourcebudget/pruningguardskept.
CurrentC9d29392c851885795a314a78f91bae7d5a7cb590dfa23ca8a56b05ab85b69fe4.
Nativeearlypowercauseinputsourceuncertain, notclaimedhumaninterference; separate
READONLYaudit ses_eb6650821ffergL11d6jEdf04p notbuildblocker/cannotedit. All183parent
filesunchanged. Sourcesrefrozen/freshFULLvalidate7+Python7beforeclient5; no
productionchange/newacceptance/export. Donotresumefailedworldasacceptedinput.

Remaining-clientREADONLYreviewDONEses_eb69e38a8ffeGAYxhjSEi6pXlc: noadditional
concretedeterministicblockerfound; UPgate/upper-onlypower/route/1001/951/3fast+
1000baseline/pickupsourceassumptionsmatchproduction. Noedits/builds/runtimePASS
fromreview. ALLagentsinactive; client4alreadyRUNNING, donotpoll/duplicate/edit.

**FULLvalidate6exit0:**220JUnit/ALL105Fabric/72Python93.056s/3827unchangedinputs,
0failures/errors/skips; productionJARbac83fff...10,556,826bytes/isolation/freshXML
checked. Preclient6reportsarchived. Launching`visnet-fabric-client4 -Scope visnet`
freshacceptedparentcopyONLY, currentCnativevalveNORTHdefaultfixincluded. One
READONLYreviewnotbuildblocker/cannotedit; no sourceedits/concurrentGradle/game.
Parent183filesexact. Client4/pixels/epochs/stoppedBASE80/acceptance/exportpending.

**Client3exit1/CORE_ACK:** raw1dfa3748/worldebfae84c/diagnostic154–156preserved.
NativehandACKfixworked: BOTHrealcharges/ALL3exactpaidcrafts/truepick2→3mine/SAME
6bb67f61UUIDcontactpickup/reinstallDOWNcompleted. Actualnewfixturevalvewasnative
NORTH2(defaultconstructor), NOTplayerclickedUP1; unnecessarycoreclickcycledit
toSOUTH3whileCexpectedNORTH. RootONLYCsequence nowreadsactualdefaultandSKIPS
corecycleifalreadyNORTH, retainsactualUP→NORTHnativeactionifneeded; no orientation
setter. Explicitnativefixturedefaultrowadded. CurrentC99594feac81b24ec6ad1f00d64840aab77d58b9e9e7bf439839b174001085c12.
All183parentfilesexact. Sourcesrefrozen/freshFULLvalidate6+Python6beforeclient4.
ONEindependentREADONLYremaining-clientassumptionreview ses_eb69e38a8ffeGAYxhjSEi6pXlc
isNOTbuildblocker/cannotedit. No newpowerconversion/epochs/cycles/clientacceptance/
exportyet. Production/resource/nativeassertionsunchanged, all397oldPASSinput5.

**FULLvalidate5exit0:**220JUnit/ALL105Fabric/72Python91.728s/3827unchangedinputs,
0failures/errors/skips; freshJARbac83fff...unchangedproduction/isolationverified.
Launchingfreshacceptedparentcopy `visnet-fabric-client3 -Scope visnet`, Cexplicit
nativehandACKcorrectionincluded. Noactiveagents/sourceedits/concurrentgame/Gradle.
Client3/ten154–163/nativeepochs/tail/stoppedNBT/pixels/acceptance/exportpending;
failedclient1+2worlds/raw/diagnostic154preserved, parent183filesremainprotected.

**Client2exit1beforeSECONDcharge:** raw78e189c7/worldcd6cb166 preserved/one154PNG
diagnosticNOTaccepted. ActualOTHERfirstcharge68nodeunits→6800centivis(29AQ/25ORDO/
14TERRA), realbasic2880AQ/TERRA/ORDO andtransducer2880AER/IGNIS/PDpaid. Relayneeded
ORDO720butactual630; beforeopeningnextsession coarseconstructoridle/handguard
failed. Likely directserverprobe taskovertookselected-slotC2S; rootONLYCsequence
nowwaitsexplicitnativeSERVERhand3+exactpaidGoldACKwithin40BEFOREnewChargeProbe,
keepsALLoriginalidle/source/budgetguards andaddsdetailedguarddiagnostics. No
serverhandsetter/vis/sessionreset. CurrentC7ea45ac20967...; firstchargeledgerkept.
Client2normalshutdownhasNOpriorCME, all183acceptedparentfilesunchanged. Sources
refrozen/freshvalidate5FULL+Python5requiredbeforefreshclient3; noacceptance/export.

**FULLvalidate4exit0/current3827unchangedinputs:**220JUnit/105Fabric/ALL105required/
72Python89.845s,0failures/errors/skips. FreshJARinspectionbac83fff...10,556,826bytes
identicalproduction; onlyclienttestfixesrevalidated. Launchingfreshowned
`visnet-fabric-client2 -Scope visnet` fromacceptedSTOPPEDM09aparentONLY; client1
diagnosticworld/raw/sidecartailpreserved. Noactiveagent/sourceedits/concurrent
Gradle/game; old183parentfilesmustremainexact. NOclient2completion/pixels/stopped
audit/acceptance/exportyet. NewauditoralreadyrequiresnativeoldnamedNoAIcowUUID/
name/health6/position/AI/persistence retained, neveranewfixtureorcombatinput.

**Client1exit1beforeANYcraft/PNG:** runfbbfe664/privateworldd3fdc8b1preserved,
raw`.tmp/visnet-fabric-client1-raw-report.json`. SETUPblanketlivingentityguardfails:
nativeacceptedparentALREADYcontainscreeper1557.84,216,376.50/skeleton1555.80,216,
386.25 +namedoldNoAIcow1561.5,216,386.5/health6. All183parentfilesstillEXACT.
RootC-onlycorrection: KEEPallmobsuntouched/read-onlyUUIDrows; noitems/XPanyowned
area andnolivingbenchclearing/miningarea guardsremain. SceneisNEVERcleared:
rejectactualbboxoverlapatEVERYexactfixturewrite+all4futureearneddevicecells,
notunrelatedmobselsewhereinlargecamera/outputbounds. No kill/teleport/AI/stock
waiver or nativeeconomychange. Cactualprices/epochs/timing/BASE80guardsunchanged.
AlsodiagnosedrootTailselfunregisterfromArchitecturyArrayListSERVER_STOPPEDcauses
shutdownCME; newisolatedClientTestServerLifecycle registersONEearlydispatcher,
removesper-serverhooksBEFOREsnapshotdispatch (allhooksrun/errorpropagates/no
retainedserver), rootTail+Cstophooksnolongermutateeventlistenersduringdispatch.
OnlyNEWclienttestfiles/Bookgluechanged; Cvisual6files/production/resourcesexact.
Sourcesrefrozen: freshsnapshot4/FULLFabric+Python4requiredBEFOREfreshclient2.
Noagent/Gradle/gameactive. Validate3oldsourcePASSremainsarchived/historicalafter
clientharnesscorrection; noacceptance/export/NeoForge/parentmutation.

**FULLvalidate3exit0/currentinputsbinding:**220JUnit/105Fabric/ALL105required/
72Python89.951s,0failures/errors/skips;3826unchangedinputs. RootactualfreshXML/JAR
inspection `.tmp/visnet-fabric-validate3-jar-inspection.json`:10,556,826bytes
SHA256bac83fff54504682f7a00523d22f10e00a334eb922f559f93ea27bf349408caa,
test-onlyprobes/bootstrapabsent/oneintermediaryAW/notices/literalrecipes/classes/
9thquantum18stateschecked. No productionchangeforfixturefixes. PreclientFULL
logs/XML/JUnit/inspection archived `.tmp/visnet-fabric-validate3-before-client*`.
**Launching `visnet-fabric-client1 -Scope visnet`** freshownedacceptedM09aparent
copyONLY; noactiveJava/agents beforelaunch/no sourcechanges/concurrentGradle/game.
Parent183filesread-onlymanifest `.tmp/visnet-accepted-parent-before-client.json`
unchanged beforelaunch; auditor/recorder/exportguardnowrequiretheyremainexact.
Client1001/951/282rawEARTH/full1000baseline/physicalUUIDs/ten154–163pixels and
independentTAILthroughnormalstop+actualstoppedNBTstillNOTRUN/accepted/exported.
Actualcount<1200+BASE80/NORMAL/NONE mandatory; never waiveoriginalpruning.
AcceptedusableexportM09aebf040e7unchanged. NoNeoForge/commits/push/releases.

**BOTHfixtureownersDONE/SOURCEFROZEN/rootreviewed.** Nodecurrentd7ecc084d9c0...,
Dcurrent0cfcf021e710..., rootLifecyclecachefixture324a9cfe2e93...; exactnotes
`.tmp/visnet-{node,flux}-fixture-fix.md`. NodeusesTWOearlyregisteredprobes/reset
finallyandactualblastoriginbase.above() withFULLsaved±8margininsideheight/retained
cleanupbounds; nooldFixtureedit. Rootisolatedmainbootstrapwiresbothregistration
APIs; noactiveagents/productionedits. SourcesREFROZEN, freshvalidate3snapshot/FULL
Fabric+Python3 next. Allactualvalidate2resultsabovehistoricalinputsonly; noM09b
client/pixels/stoppedworld/acceptance/exportyet. UsableM09aebf040e7unchanged.

DfixturefixDONE/rootreviewed `.tmp/visnet-flux-fixture-fix.md`, exactnewhash
0cfcf021e7109f77905f848dfe2e17d6ee04ec200a157ab7ba724779fdc41d0a. TEST-ONLYearly
registeredid/ordinaryplacement/resetfinally; allcallbackassertionspreserved.
ONLYNodefixtureownerstillactive, noGradle/gameuntilitsfreeze. RootfocusedPython
overfullresourcecase1PASS0.041s(aftertestcorrection), NOTfull72. Independently
2002production/resource/clientinputs remainEXACTvsactualvalidate2; corrections
onlyinNEWnativehelpers/bootstrap+newPythonassert. Cachedvisibilityrestoreimplemented
sourceonly; runtimeunknownray/supportdebitassertionsstillunverifiedafterfix.

**FULLvalidate2exit1:** actual220JUnit0fail;105native/101pass/4NEWfail/ALLold95PASS;
Python72/71pass/1NEWassertfail95.353s. ArchivedXML/JUnit/partialsummaryunder
`.tmp/visnet-fabric-validate2-*`; UNACCEPTEDnewJARbac83fff...10,556,826bytes isNOT
exported. Nativefails: DandNodeconstructunregisteredNeighborProbeBlockAFTERnative
registryfreeze; blastbase-57minus8=-65below-64; visibilitytestremovesholderbut
nativegetChunkNowstillhitsitsFOUR-entrylookupcache. Rootbytecodediagnostic
`.tmp/visnet-native-chunk-cache-javap.txt`confirms cacheBEFOREvisiblemap.
RootfixedONLYNEWLifecyclefixturetoevictmissingkeyfromcachetoo/save+restoreEXACT
arrayidentities/entries andholderfinally, strictunknownassertretained. PythonNEW
testmistakenlyrequiredx180FULLgascube; acceptedM09aJARindependentlyconfirmsallold
selectors/fullmodelUNROTATED. CorrectedONLYnewassert, allresourcesunchanged.
RootNEWisolatedgameTestmainbootstrap/metadata callsearlyregisterTestProbes APIs.
TwoBOUNDEDownersACTIVE: NodefileONLYses_eb6b1773affeih62ePzEKkjRf2 fixesregistered
probe+blastfootprint; DhelperONLYses_eb6b17738ffeRDxLOaPLEKS2sN fixesregisteredprobe.
NOGradle/gameuntilBOTHexplicitfreeze; no client/pixels/acceptance/export/NeoForge.

**Compile2exit0.** Finalindependentrecipe/conversionreviewfoundonevacuoustest:
sneakingBALANCEDwhileuntyped-1 couldPASS evenwithbrokenrefusal. RootchangedONLY
NEWrecipehelperheldfixture to namedIGNIS3 soincorrectactionwouldset1; sameheld
identity/count/components checked. Production/old95 unchanged. Newrecipehelper
SHA2566f8623e472c6c27631663c8728b61230c5077698857a0d3785676baf0fb0fb69.
Snapshotvalidate1capturedBEFOREthisstrengtheningisHISTORICAL/NEVERRUN. Fresh
`.tmp/visnet-fabric-validate2-inputs.json`capturedAFTERallfinalsourcechanges;
launchingFULLFabric `visnet-fabric-validate2` +independentPython
`.tmp/visnet-assets2.log`. NOactiveagents/sourceedits/concurrentGradle/game.
Expected220JUnit/105requirednative/72Python are NOTyetexecutionresults; no
clientworld/pixels/acceptance/export. AcceptedM09aebf040e7binaryunchanged.

**Compile1exit1diagnosed:** common/fabricproduction+clientTestcompiled; ONLYerror
missingCollisionContextimport inNEWVisNetRuntimeChecks. Rootfixedimport; read-only
networkreviewalsofoundMISSINGstrictrelayNBTtests requestedearlier. Rootnowadds9
malformedvariantsfromGENUINEnativesavedvalidbaseline, positivecurrentreloadcontrol,
no-routing/debit/quarantine/exactwholeevidence/SECONDreload/validrecovery inside
existinggraphwrapper. No production/oldassertion/case-countchange. Ehashhistorical
onlyforVisNetRuntimeChecks; rootcurrent00ed0b65cc3f...documentedEAPI. Sourcesrefrozen,
launchingFabric-only `visnet-fabric-compile2 -CompileOnly`; no native/unit/client
execution oracceptednewartifact/exportyet. Expected220/105/72 unchanged.

**ALLEownersSOURCEFROZEN.** Ecompletionreceived/final4hashesexact against
`.tmp/visnet-e-api.md`; all7helpers takeONLYGameTestHelper/ownnativeactors. Root
registeredD3+E7 inTENdistinct240tickbatches: **105=ALLacceptedold95+10**, exact
wrapper/manifestsetmatch+oldXMLsubset checkedsourceonly. Rootlifecycle/consumer
source reviewed;2independentREADONLYreviews ofotherEfiles are notbuildblockers/
cannotedit. RootintegrationSOURCEFROZEN; launchingFabric-only
`visnet-fabric-compile1 -CompileOnly` (NOunit/game/clientyet). NoactiveJava process
beforelaunch; noNeoForge. Expected220JUnit/105Fabric/72Python NOTexecutionresults.

Useraskedhangdiagnostic: E3nativefileshadactualfreshwrites2026-10-17T09:48:31Z,
15secondsbeforeone-shotcheck; no evidence ofhang/no restart/duplicateagent. API
stillEARLY(notSOURCEFROZEN): currentlySEVENproposedhelperentrieswithoutactorarg,
helpersownnativeSURVIVALactors. Finalentries/counts mustcomeEcompletion, notold6/
104proposal. Rootpreparedignored `.tmp/audit_visnet_stopped.py` and
`.tmp/record_visnet_client.py`: syntaxcheckedONLY, NOTexecuted. Independentnative
regions/entities/inventory/89→95BEs/BASE80+lastlivePOST/tail/epoch282/tenPNG guards;
no newworldcopy/build/tests/client/pixels/PASS/export. Ecompletionstillrequired.

BREADONLYprunereviewDONE `.tmp/visnet-b-prune-review.md`, sourceonlynochanges.
Warmalignmentcorrect; returnednodehasONLY200sparetotal around1000baseline, closing
canpruneafterCunregister. RootNEWindependent `VisNetTailObserver` nowwiredinBook:
pureSERVER_POSTthroughSERVER_STOPPED +run-specificsidecar/captureevents/rawzero
keys/BASE/UUID/type/mod/natural/count/cache, NEVERtick/save/debit/phasewrites.
C7filesunchanged, strictBASE80checksunchanged. FabricclientgateNOWrequiresreal
tailSTOPPED/lastordinarycount<1200/base80/NORMAL/NONE too; actualstoppedNBTstill
mandatory/exportguardrequirestailhash+readback. No waiver/timefreeze/sourcebugfix;
ifdeadlinefails, keepdiagnosticworld/freshacceptedparentretry withnativeactions
schedulingonly. RootCpayments/epochs/actual104steps reviewedsourceonly. ONLYE still
implementing; no builds/games/native/pixels ornewexport yet.

**A/B/C/D SOURCEFROZEN; ONLYE nativechecks stillimplementing.** Cfinal7files/API/
exact10capture154–163/104stages `.tmp/visnet-c-api.md`. Rootreviewed6visualsources,
all3BER/sixargitems/converterFX/sharedoldshader wired; validatedS2CRGBdirectlycalls
`drain(level,payload.target(),payload.origin(),payload.rgb())`, no uncheckedindex/
sourcequery. Cnoacceptedhelper/shader/resource edits, originalSupport36 parsedNOT
drawn asoriginalrenderer; ring4096x256/16frames, parentbeam/bolts/drain callbacks
sourceonlyNOTpixels/audio/fullparity. 3accelerated+1full1000baseline planned.
Bnow ONLYoptionalREADONLY `.tmp/visnet-b-prune-review.md` audits Cwarm/returnednode
1200pruneboundary; no source edits/build block, rootactualstoppedNBTmandatory.
NoGradle/game untilEfinalSOURCEFROZEN; noM09bcompiled/native/client/PASS/export.

RootpostfreezeAstrictNBTcorrection: native relaydisk/updatewritesBYTEcolor AND
BYTEorientation always. MissingfieldsnowWHOLEquarantine, notuntypedstrongerroute;
freshplacementconstructor-1/UP unchanged/nooldnativeVIS_RELAYmigration. ONLYnew
TileVisRelay.java changed afterAfreeze, fourAhashesstillhistoricalexact. Etests
mustcovermissingfieldsfromactualserializedbaseline inadditionwrongtypes/ranges/
nativefacingmismatch. No compilation/runtimeclaim; source/API/HANDOFFdocumented.

**A/B/D SOURCEFROZEN/root-reviewed; C/E stillactive.** A5exacthashes/currentAPIs
`.tmp/visnet-a-api.md` verified, full5sourcesread. RealweakperLevelgraph/lifetime/
firstphase0→1/every40→41/biasedsource-16/perrootnearest/multirootpartial/rawdebit
frame/strictprovidergrants/nativecollisionLOS40/noConsumerLOS/noColorAspectGate
implementedNOTexecuted. RelayFACING+LINKED displayhint12nativecombinations, original
linkedlight10/unlinked2 suppliedbyAconstructor; rootemptyBERvariantcoversall,
no new routing/powergate. Exactoncewandsecondaryhook+level/servercleanup confirmed.
No amulet/playerlookupfake map/source item. AstaticgraphLRU1024 cacheslocations
ONLY, notpowercap; originalclientcoordsvalidated/fullints/wholebadrelayquarantine.
NoGradle/game untilC+Efreeze; noM09bcompile/native/client/pixelsPASSyet. Usablemods
M09aebf040e765 unchanged. Nextroot: consumeC/EfinalAPIs, wireD+Eunique-batchwrappers/
requiredmanifestALLold95, then freshsourcefreeze/CompileOnly/FULL/Python/client.

**D SOURCEFROZEN/rootreviewed9paths**, `.tmp/visnet-d-api.md`:9hashesexact/
ALL18old0..7/fullmodelsbyteunchanged/static10selectors→18statecoverage confirmed.
LEVEL0..8butcapacity8/branchspecific9flows/sourcefirstcallbackfence/gasNausea
guard+application/goo25miss→30/sourceAIR+swap tail implementedNOTrun. Whole9save
nativepalette/readback, exactRNGincludingbound1/ordinary8/signed-.125goo/actual
effectcallbacks/milk/actual14+14tickneighborwake in3NEWnativeentrypoints:
FluxOverfullRuntimeChecks.overfullFlowStateAndPersistence/originalExposureAndGooTail/
callbacksBoundariesAndNeighborWake; rootmustuseTHREEDISTINCTbatches andkeepallold95.
Registerafterallfreeze, noGradle/gameyet. DoldFluxRuntimeChecksONLYknownWither→
CONFUSION getter/messages/sourcecommentfix; M11slime/taint andfullfluidAPI/render/
onPlaceatomicity/ABA/powerloss explicitlynotclaimed. Bdone/rootreviewed, A/C/Estill
active. UsablemodsM09aebf040e765 remains accepted; noM09bcompiled/PASS/export.

**B SOURCEFROZEN/rootreviewed8files**, `.tmp/visnet-b-api.md` exactAPIs/hashes/
sources. Source1001warm/951cool+sameupdate49/currentZERO/MAX/epoch/nativeconstructor
handoff/identityboundfailure implementedNOTtested. Rootfixedonlyclientstabilizer
originalBLOCKmetadata0/5predicate (noaboveBEgate) +obsoleteTileNodeJavadoc;
sixNEWBhashes unchanged/twooldtilehasheshistoricalafterrootnarrowreviewedit.
Root4NEWreducerJUnitcases=>expected220JUnit bysourceONLY (MAXvsacceptedSUM41/
immediateequalparentdoubling/deep1100DAG/cycleinvalid). OtherA/C/D/Estillactive,
no build/game/sourcePASS yet; usablemodsM09aebf040e765 remains accepted.

RootconsumedEARLYAPIforactualglue(no statuspolling): Asecondaryrelaywandhook wired
inexistingItemWandCasting.useOn; C3BER/sixargitems/convertereffects/clientdrain/
sameacceptedlowalphashaderwired. D10selectors(9false+fulltruewildcard) cover18
actualblockstates; rootfutureinspect/exporthelpers correctedbeforeexecution,
exactrender-stateassetassertadded (expected72Python/216JUnit bysourceONLY).
No fluxgenerator exists in conversiontool, so no inventedregenrefactor; accepted
0..7modelsunmodified. Cactualproposal3accelerated+1baseline/4newcrystals, ten154–163
captures, realUPfacevalvegatesbufferafterfill. `.tmp/visnet-root-contract.md`.
Allfiveownersstillimplementing, NOTSOURCEFROZEN; no builds/games yet.

RootNEWfullcoord/dimension/RGB S2CFX+original500..599ms localRandomthrottle (NO
worldRNG/supplygate, expiredcallremovesNEXTcanemit), serverstop/actuallevelunload
clearwired. Root1primary+4payloadJUnit+3assetregressions (expected216/71 ONLY,
notrun), visnetscope/stage/report/freshacceptedparentclone+32bench/PShelper/
10captureproposal154–163 wired. Originalrelayresistance17/full4OBJgroupsSupport36
andstripitems/lightningringv4096x256 independentlyread. Rootcontract
`.tmp/visnet-root-contract.md` stableFXAPI +actualinput/rendererfacts. All5owners
stillactive; no Gradle/game untilALLSOURCEFROZEN; modsM09aebf040e765intact.

Сразу после M09a acceptance/export/all3hashesverification начата следующая unit:
realconverter/energized/relay/graph +actualEARTHcrystalacceleration +reviewed9flux/
Nauseamapping/tail. **NOTcompiled/native/client/pixels/accepted/exported.**
[Контракт](analysis/2026-10-17-visnet.md), `.tmp/visnet-*-api.md` forownerAPIs.
A ses_eba0c3e38ffe2MrPF5RN6iMsr3 NEWpublicgraph/relay; B ses_eb8fdcfa0ffedhmEktMzIArtJk
NEWsource/converter/failure/narrowoldstabilizer/node; C ses_eba0c086cffe6eh2v6uL2pSttp
NEWvisual/client; D ses_eb929819dffezYmObFv7Ydcqe4 assignedninequanta/hazards/narrow
oldincorrecteffectassertions; E ses_eb8fc2fffffeA3F3H1fYjxxi0E NEWnativechecks.
Rootregistry/recipes/book/icon/labels/tooltip/crystalCVstep integratedsourceONLY.
**NoGradle/game untilALLSOURCEFROZEN**, noNeoForge/commits/push/releases.
UsableM09aebf040e76510,409,707bytes stays accepted; oldevidencenotnewsourcePASS.

ExactIDsnode_transducer/energized_node/vis_relay, ENERGYsourceNOitem. Counter1001/
951returnCURRENT0, MAXnotSUMrates/8frombase80, rawCVcentiviscompatible NOTwholevis.
Crystallizer1+2actualdrain fifthtick, source-lessbaseline unchanged, temporary
drawframefencescallbackinputmutation; oldexecutor/escrow unchanged. Expected
freshparentbelow89BE/typed2+2/phials/allgear/XP11, proposed10captures154–163,
bench1938/realoldnode1551/relay1558/consumer1565beyonddirect8; actualearnedold
crystalizermined/UUIDpickup/relocatedifCfinalplan, nofinishedpoweroutputgrants.
Originalauthority/pinnedOpenTC4 activelyread/fetchedexactpinnedGitHubnamedhandler,
no donorcopy/newDDA/tweakpersistedlinks imported. All5ownersactive, no polls.

### Последний принятый checkpoint: M09a basic stabilizer, Fabric-only

**ACCEPTED/EXPORTED** `stabilizer-fabric-validate2` finalFULLafterclient /`client1`:
**211JUnit/95Fabric/all95required/68Python87.391s/3782unchangedinputs**,
0failures/errors/skips. Client **139.7644009s/2795ticks/2772POST**, all8exact146–153
rootinspected. [Client](porting/stabilizer-client.json), [verification](porting/verification.json),
archive `.tmp/stabilizer-accepted-verification2.json`; currentmatch=true/historical=false
atrecording. Preclientreports`.tmp/stabilizer-fabric-validate2-before-client-*`,
samecampaign2/samehash, no sourcethaw afterfinalfreeze. MainJava305not%.
Noactiveagents/Gradle/game; optionalfutureThaumatoriumauditcancelled/notrequired,
partialnotespreserved/no completedclaim. NeoForge/commits/push/releases none.

**ONLYacceptedSTOPPEDparent fornextgroup:**
`fabric/run-clienttest/saves/stabilizer-smoke-412b7979-9a6d-4ab4-bac8-0715023c191f`.
Root `.tmp/stabilizer-fabric-client1-native-stopped-audit.json` via read-only
`.tmp/audit_stabilizer_stopped.py`: **89non-nodeBE/20jars333/72candles**. All87oldBEs/
inventories/aspects/B0furnaces exact exceptONLYempty1882sourcevalve naturalpressure
parent63→127(flow1/all6open/stock0/sideNORTH unchanged). No furnaceclockexceptions!
Original1465fuel3/B0 exact. Old4typedcrystals/two8phials/foci/wands/gear/apple/XP11
retained; basics2/DROP4/filter0/empty2/blank1, savedchestWORLD1882,220empty.
Pickwooddamage2 (was1, actualONEheldmine). No finishednewdevice inventory: ONE
plainstabilizerBE1551,217,377 underSAMEsavednode1551,218,377, stoneRAWsupport1551,
216,377; realwalllever1551,216,378SOUTH/OFF. Fourdeclaredtorches1550/1553,Y216,Z375/
380, nooldnode/floorclearing. Newemptybench1906,216,377; expectednextbench1938 if
same+32layout, not mandatoryfuturepowerarenalayout. Node/animcount/lock/cacheNOT
persisted; loadmustfirstnativepoll, no guessedgoldenphase.

Gold7final `{aer:5000,aqua:20,ignis:5000,ordo:1010,perditio:5000,terra:2120}`;
mainFrostunchanged1951AER/2436AQ/2846IGNIS/933ORDO/431PD/464TERRA, all3wands.
Node1551 UUIDf99565f9 base80/typeNORMAL/nomodifier currentALL80exceptORDO78.
Node1623 UUID9bbeaa03 base80/NORMAL/nomod currentAER80/AQ53/IGNIS80/ORDO52/PD80/
TERRA66. Other3savednodesexact, actualclosing28ticks/noregain. Pending/Quarantined
emptyV2, no unaccountedowner. ONLYnewknowledge NODESTABILIZERcompletion declared,
ALREADYCRAFTEDNitor1/bricks2ingredients notacquisition; otherrawgold/quartzBLOCK2/
piston/support/lever/bench/floor/4torches/camera declared. No vis/XP/essentia/
node/progress/RNG/clock/weather/finishedstabilizer grants/moves/refunds.

Actual1recipe **2880AQ/2880TERRA/2880ORDO**, GOLDcap1F +savedworn10% gear (not
intrinsicgold.9). Source1623spent73units(29AQ/30ORDO/14TERRA)→2900AQ/3000ORDO/
1350TERRA, nativecapround50TERRA. Protectedtarget1551real4ORDOtaps→400centivis,
no workaroundpower/falseplayerdeny. Beforeepoch1200 onepreviousnaturalregain
ORDO76→77, FULLepochcount1200→1800→2400:no600regain/exactONE1200regain77→78.
Nativepowerstale40POST/unpowered76POST/5lockchanges; CLIENT37opening/closing
separate8rows.94noderows/77taps/6ownershiprows, nodepositiveRNGnotqueriedbyobserver.
Genuinewoodpickmine/UUIDdb205c54contactpickup/reinstall. OriginalFADING→PALE
cached0 preserved; rootinitialmistakennodeChangereset removedbeforebuild, native
testsprove0through1800→actualpower1850/1900→refill3600/exactRNG. Allold92retained.

Export **10,409,707bytes** SHA256
`ebf040e7654e34c6d3ee2d3c7abc5a2750282cf37c04f4deb42c7189fe3d9f7b`,
backupoldM06e`backups/crystallizer-fabric-validate2/` outside mods; README/hashes
updated, oldbackups/deps retained. Stillsinrain/daylight/partialmodel/nodeglare/
smallGUIlimitations recorded:150Ordo79 earlytapNOTepochstart77,151Ordo78 postepoch
not a visual79→78rechargeclaim; 149ownnodehaloNOTstabilizerbubble. Actualbubble
GLSL lowalpha>1/255 compiled/renderedinclient, nolegacyshaderbulkcopy, exactly2
newauthoredcorefiles. Fullmotion/audio/allface/lighting/GUI/API/specialnodes/
advancedstabilizer/VisNet/EARTHacceleration/ordinarypackaged/EULA/remote/fullsurvival/
chunkunload/crossfilepowerloss/NeoForge/fullTC4 NOTclaimed.

Nextboundedgroup **M09b realVisNetclosure** immediatelyafterexportverification:
transducer/energized/relay/network/actualEARTHcrystallizeracceleration. Required
[prep](analysis/2026-10-17-visnet-prep.md), `.tmp/visnet-dependency-closure.md`,
[fluxprep](analysis/2026-10-17-flux-overfull-prep.md), `.tmp/visnet-nine-quanta-audit.md`:
MAXnotSUMreducer/rawcentivis-compatiblebudgets/count1001conversion/951reversion/
explicit9quantaLEVEL0..8+put0..9CAPACITY8/nauseanotWither/gootail findings.
Originalsource remainsauthority/no donorcopy, all9quantafixes stillfuture until
assigned/tested, not proven by acceptedM09a. No always-zero power stub.

<details>
<summary>История M09a — не текущие команды и статусы</summary>

**CURRENT FULL`stabilizer-fabric-validate2`exit0**, **211JUnit/95Fabric/all95required/
68Python87.391s/3782unchangedinputs**,0failures/errors/skips. Snapshot2/current
XML/JARinspection `.tmp/stabilizer-fabric-validate2-*`. OneproductionJAR
**10,409,707bytes** SHA256
`ebf040e7654e34c6d3ee2d3c7abc5a2750282cf37c04f4deb42c7189fe3d9f7b`, ZIP/notices/
testisolation/oneintermediaryAW/originalrecipe+art/noVisNetstubPASS. StocklessBE
phase50mapabsence/full95oldregressions/originalFADINGcached0 branchespassed.
**Launching `stabilizer-fabric-client1 -Scope stabilizer`**, freshacceptedSTOPPED
crystallizerparentONLY. No duplicateGradle/game/sourceedits. Allagentsdone, no
client/pixel/acceptance/exportyet; usablemodsM06e806a7cadebunchanged. Eightexact
146–153/actual1200tickepoch/nativeclosed-saveaudit89BE stillrequired. Ignored
`.tmp/audit_stabilizer_stopped.py` prepared/NOTRUN, onlyreads futurestoppedactualsave.

**LATEST FULLvalidate1exit1:** actual211JUnit0fail/3782unchangedinputs,94/95native
pass; ONLYnewlockcasefailedfixture `getBlockEntity(support)` lazilyRECREATEDthe
missingBE ittriedtoassertabsent. Rootnowobservesalreadyloadedchunkmap(beforeAND
afterphase50), preservingoriginalBLOCK-onlygateandstrictabsenceassertion; no
productionchanges. Newrecipe/pricingandFADINGcached0/RNG/native3600casespassed.
Python1actual68ran/1failed oldblanket"shadersdirectoryabsent"assertion; nowEXACT
twoauthoredcorefilesallowlist +oldminecraftshaderabsence +newthresholdcontract,
not a broad legacyshaderpermission. FULL1XMLarchived, no clientworldstarted.
Freshsnapshot2/FULLvalidate2/Python2 next; oldsnapshot1historical. Sourcesfrozen
afterthese2TEST-ONLYfixes. NoacceptedM09a/export, usableM06e806a7cadeb.

**CURRENT compile1exit0**, snapshot `.tmp/stabilizer-fabric-validate1-inputs.json`
captured afterallrootintegration. **FULL`stabilizer-fabric-validate1` RUNNING**;
independentPython `.tmp/stabilizer-assets1.log` alsoRUNNING. No duplicateGradle/
game/sourceedits. Currentexpected211JUnit/95Fabric/68Python are expectations ONLY,
no native/client/pixelPASSyet. No activeagents: A/B/Cdone, future9quantaauditdone,
optionalThaumatoriumcancelled. AcceptedM06eexport806a7cadeb remains untouched.
Ignored `.tmp/record_stabilizer_client.py` prepared/NOTRUN, requires actual1payment/
full1200tickepoch/600none+1200one/twosourcecharges/physicalUUID/retained2+2crystals/
independentstopped89BEaudit/all8exact146–153pixels; no simulatedacceptance.

**CURRENT ALLSOURCEFROZEN A/B/C**, rootreviewedintegratedC3NEWfiles/API
`.tmp/stabilizer-c-api.md`, exact8capture146–153, nativeold87BE retained, ONEcraft
2880each actualsavedgear10% +GOLDcap1F. BER/sixargFabricitem/minimalnewbubbleFsh+
CoreShaderRegistrationCallback wired; vanillaPOSITION_TEX_COLORvsh reused,
alpha>1/255 notvanilla.1. OriginalOBJ/PNG unchanged, shaderassertion added
(expected68Python). Cshows1full1200ticknodeepoch+realplayerdrain/nativecache50/
actualclient37/open-close/recoverSAMEUUID/reinstall, no manualtick/stock grants.
Rootprecompileaudit fixedONLYnewBcostfixture assumptions: GOLD=1F not.9F,
bare3200/goggles3040/actualworn10%2880, firstpaymentremain1800; SECOND independent
THAUMIUM.9F+worn10%2559remain2441. Productionprice untouched, Cactual2880unchanged.
Earlierrootdocsgold.9intrinsicclaim corrected; originalFADINGcached0 quirk retained.
**Launching `stabilizer-fabric-compile1 -CompileOnly`**, no concurrentGradle/game/
sourcechanges. No M09acompile/native/client/pixelsPASSyet; acceptedmodsM06e806a7cadeb.

OptionalfutureThaumatorium READONLYaudit `ses_eba0c66dcffesx8wm2sjjssC8n`
**CANCELLED**, no completed/reviewed evidence; do notassumeresults orwaitforit.
Any partialignorednotes retained, notdeleted/usedasacceptedinput. Noautomatic
respawnneeded. This doesNOTcancelcurrentM09a: A/Bdone/root-reviewed, Ccurrent
renderer/clientstillactive; noGradle/gameuntilCfreeze. AcceptedM06eexportintact.

BfutureREADONLY9quanta auditDONE `.tmp/visnet-nine-quanta-audit.md`; rootreviewed
[summary](analysis/2026-10-17-flux-overfull-prep.md). FutureLEVEL0..8/put0..9 while
capacity/denominator8/exactAIR-vsWATER-vsSWAP branches unchanged; NOTimplemented.
Importantknownunfixedoriginalmapping: GasPotion.field_76431_k=confusion/NAUSEA,
nativecurrentlyWITHER(+oldtestassertionwrong); lowergoo25miss→30fallthroughgap too.
Rootindependentlyreadoriginal80/86+pinnedfields1664/2595/namedOpenTC4; noaccepted
flux/main/test thaw now. Futureexplicitownedfidelityfix +allregressions required,
not silentposthocPASS. M09aA/Bsourcefreeze reviewed/rootFADINGquirk restored,
Ccurrentrenderer/clientactive; noGradle/game. UsableM06eJARunchanged806a7cadeb.

**A/B SOURCEFROZEN/root-reviewed; Cstillactive.** Aactual3files/APIs/hashes
`.tmp/stabilizer-a-api.md`: no serverstabilizerticker, fullcube/ordinaryblockmatch,
puregetterlock/count/cache, first/%50cachedpolicy, noforcechunks/radius2powerpreflight,
current-server/thread/BEidentity/reentry/loadnotificationfences. NarrowTileNode
integrationonly; acceptedM06eengine/classes unchanged.
**RootsourcecorrectionBEFOREanybuild:** originalTileNode250–252 setteronlyassigns
modifier, so rareFADING→PALE **DOESNOT invalidatecached0**. Rootinitialprompt wrongly
saidnodeChange/reset; no useraskedthatbugfix. Removedextrareset, originalquirk
preserved. NewnativeFADINGtest strengthened: cache0throughreal1800, actualpower/
poll1850→900/unpowerpoll1900→1800, real3600restores1/exact17unlockedPALEbranches.
OnlyrootTileNode+newLifecycleChecks edits afterA/Bfreeze; notesoldhasheshistorical.
NoGradle/game/compile/native/client/pixels M09ayet; usableM06e806a7cadeb intact.

**B SOURCEFROZEN/root-reviewed** NEWNodeStabilizerRuntimeChecks.java +LifecycleChecks,
3nativeentries/APIs/caveats/hashes `.tmp/stabilizer-b-api.md`. Rootadded3wrappers
inseparatebatches +requiredmanifest **95expected=old92+3**, NOTcompile/runtimePASS.
Actualsourcecheckscovernativepaidrecipes/float2880/2720/2559/6facepaidplacement/
FULLcube/noGUI/stocklessreload, cachedfirst/50phases/blockNOTBEpredicate/bothbully
directions/exactRNGcalls/replacement/playerwandpayment/600vs1200/etc/FADING69/
historicaltimestampcatchup3vs1/persistence/staleidentity/negativeheight. Deterministic
RNG INPUT is scoped server-only/restoredexactidentityfinally, noclient/worldseed.
**A/C still active, noGradle/game.** Trueclient37animation/1200elapsedticks/pixels
andfullsourceboundvalidation remainpending. Nochunkunload/remote/powerloss claim.
B now independentREADONLYfuture `.tmp/visnet-nine-quanta-audit.md`, currenttwo
helpersstillfrozen; this paperdoesnotdelaycurrentbuild. Rootignoredfutureinspection
helper `.tmp/inspect_stabilizer_build.py` expects211JUnit/95Fabric bysourceONLY,
NOTexecuted. Wrapper/manifeststatic95equalityconfirmed, no namesremoved.

**Root resourcepreflight1 PASS:67/67Python83.591s**,
`.tmp/stabilizer-assets-preflight1.log`. Staticresourcesonly; no M09acompile/native/
client/pixels yet. All3currentowners stillimplementing, noGradle/game untiltheir
explicitSOURCEFROZEN. AcceptedexportM06e806a7cadeb unchanged.

Rootpreparedignored `.tmp/export_stabilizer_binary.py`, syntaxcheckedONLY/NOTRUN;
guardsactualacceptedM06e806a7cadeb +futurebackupcrystallizer-fabric-validate2outside
mods, exactrecipe/nativeclass/capture8checks. Staticrecipefinitecount7occupiedcells/
2spaces,2880AQ/2880TERRA/2880ORDOcentivis checked, notnativepayment. NoGradle/game/
snapshot/compile/pass/export forM09ayet. Awaitownercompletion +resourcepreflight,
do notpoll/restart/backgroundduplicate.

Root1primary+3asset regressions added; stabilizer scope/stage/report/freshaccepted
parentcloning+PShelper+8capturegate wired, **NOT compiled**. FullPython resource
preflight1 completed67PASS above, noGradle/game. PinnedOpenTC4HEAD80aac29c actively
read, no copying. Originalretainedmodel32x32+over32x32/bubble256/groupslock28piston12;
mount+floorAIR confirmednativeNBT (belowtarget1551), no inventedstone to mine.

Сразу после M06e acceptance/export начат **basic stabilizer/ordinaryNORMALnode**:
A engine`ses_eba0c3e38ffe2MrPF5RN6iMsr3` NEWBlock/TileNodeStabilizer+narrowTileNode;
Bnativechecks`ses_eb929819dffezYmObFv7Ydcqe4` NEWhelpers; Cnewrenderer/client
`ses_eba0c086cffe6eh2v6uL2pSttp`. Rootregistry/bootstrap/resources/recipe/book/icon/
status-tooltip integrated; **NOTcompiled**. [Актуальныйконтракт](analysis/2026-10-17-stabilizer.md),
`.tmp/stabilizer-root-contract.md`. **NoGradle/game untilALLSOURCEFROZEN**; noNeoForge/
commits/push/releases. OldAThaumatoriumREADONLYaudit doesnotowncurrentproduction.
Specialnodebehaviours/advancedstabilizer/VisNet/transducer/energized/relay andM06e
acceleration remainexplicitlyOPEN; noalways0stub. AcceptedusablemodsM06e806a7cadeb.

Stable`ThaumcraftNodeDevices.NODE_STABILIZER/_ITEM/NODE_STABILIZER_BLOCK_ENTITY`,
IDnode_stabilizer. Exactrecipe' G '/'QPQ'/'SNS':goldINGOT1/quartzBLOCK2/piston1/
arcane_stone_BRICKS2/NITORITEM1,32AQ32TERRA32ORDO; goldprice2880each. Primary
NODESTABILIZER parentNODEPRESERVE. OriginalTileNode count++→cachedpollfirst/%50/
unpoweredimmediatebelowblock(nonTEgate), bothdrainprotection, regen*2, supported
NORMALFADINGrareheal%100/nextInt12500==69. Client0..37animationnotservercache.

AcceptedSTOPPEDparentbelow87BE/20jars333/72candles/newtyped2+2preserved. Proposed
bench1906/8captures146–153, installunderREALsavednode1551; chargepayerfromsaved1623
(now80each). READONLYmount1551,217,377+floor1551,216,377 **AIR** andnearbycellsAIR,
boundeddeclaredmundanesupportallowedbutNOoldnode replacement/grants. Literal1200
nativeclientticks/recharge notclock/progress/RNGsetter; targetmaynativecatchupfull,
deficitmustcomeactualheldwandchargeonly. Nitor1/bricks2ingredientfixtureshonestly
ALREADYCRAFTEDifnotreplayed. AllA/B/Cactive; no wrappers/APIs/build/clientyet.

</details>

### Предыдущий принятый checkpoint: M06e базовый кристаллизатор, Fabric-only

**ACCEPTED/EXPORTED** `crystallizer-fabric-validate2` (finalFULL afterclient) /
`crystallizer-fabric-client1`: **210JUnit/92Fabric/all92required/64Python83.006s/
3767unchangedinputs**,0failures/errors/skips. Client **281.2714389s/5626clientticks**,
all8exactPNG138–145rootinspected. [Client](porting/crystallizer-client.json),
[verification](porting/verification.json), archive `.tmp/crystallizer-accepted-verification2.json`;
currentmatch=true/historical=false atrecording. Preclientbuildreports separately
`.tmp/crystallizer-fabric-validate2-before-client-*`, samefrozeninputs/sameJARhash.
MainJava300not%. NoGradle/game/currentimplementation agents active; A future
ThaumatoriumREADONLYaudit may still finish, notblocker. NoNeoForge/commits/push.

**ONLYacceptedSTOPPEDparent for nextgroup:**
`fabric/run-clienttest/saves/crystallizer-smoke-c987c1ab-c620-4e0f-81a2-3a0e13861ae9`.
Rootnativeaudit `.tmp/crystallizer-client1-native-stopped-audit.json` via read-only
`.tmp/audit_crystallizer_stopped.py`: **87non-nodeBE/20jars333/72candles**. Old81BE/
TCblockstates exact exceptpaid1850Burn495→0; original1465fuel3/B0 exact. ALLold
clockexceptions1722/1754/1786/1818/1820/1850 nowB0; none cannewignite/changeinventory.
New6BE: bench1874,216,377 empty; furnace1882,216,377 B0/aspects/items empty;
alembic1882,217 empty; sourcevalve1882,218 empty/flow1/all6open/sideNORTH,
actualSTOPPEDsamount63 (report127→63 nextnaturalempty-machinepoll); CF-independent
crystallizer1881,218 faceEAST5/aspectnone, no count/progresssaved; savedchest1882,220
empty. Native blockfaceEAST agreesBEdiskface. Nohiddenoutputpending/quarantine,
SavedDataVersion2/terminalreceiptsretained/no compaction/no crossfilepowerlossclaim.

Gold7final `{aer:5000,aqua:0,ignis:5000,ordo:490,perditio:5000,terra:3650}`;
mainFrostexact1951AER/2436AQ/2846IGNIS/933ORDO/431PD/464TERRA,3wands/allfoci/gear/
apple/XP11 retained. Basics2/DROP4/filter0/emptyphials2/blank1/oldPOT8+IGNIS8phials
exact. NewIGNIS2typedstackmain18/POT2typedstackhotbar3, eachtyped1perITEM;
earnedwoodpickdamage1slot14, savedchestinventory1→world1. ONLYdeclaredprimary
ESSENTIACRYSTAL appendedknowledge, balanced2ALREADYCRAFTEDingredientfixtures—not
crucibleacquisition. Vis/XP/essentia/finishedmachine/crystal/progress/RNG/clock/
weather/itemrelocation/refundgrants NONE. Rawmaterials/emptyalreadyimplemented
sourceworkbench/floor/local4torches/camera explicitlydeclared.

Node1551,218,377 UUIDf99565f9 currentAER75/AQ49/IGNIS80/ORDO44/PD75/TERRA65,
base80/typeNORMAL/modifiernone exact. Actual92chargeevents gained450AER/3100AQ/
3600ORDO/450PD/1440TERRA/0IGNIS,0sessionregen, capround50AER/50PD/60TERRA.
Untouchednode1623 UUID9bbeaa03 naturallyregen80each (21AER/44AQ/10IGNIS/51ORDO/
19PD/25TERRA) overnativewall-clock lifecycle, actualwanddebit0, notgranted/reset.
Otherpriornodes exact except native lifecycle timestamp; no deterministicfuture
charge/regen promise. New7paidcraft3150AQ/3150ORDO/1350TERRA +ordinarywoodpick.

RAW1coal+FUEL1coal→IGNIS2/POT2→4cycles actual200steps each:
1090ticks(firstpause)/1000/1000/1000. Positiveprogress56paused45POST count523→567,
notCFfreeze. FirstIGNIS2 paid into actualsinglechest/retrieved viaGUI; genuine
heldmine correctwoodpick damage1/machineUUID27a7c374 contactpickup andreplaceEAST.
POT2 paid physicalUUIDb0c50bbe/189db4fe westADD1880.85,218.5,377.5/motion-.04 and
sameUUIDcontactpickup, no item/velocity/pickupdelay writes.5602POST/19hops/67balances/
2nativeinspections/4fullcycles, no losses/grants. Independent4newnativecases include
strictcomponents/chesthalf/hopper8/furnace/unsupportedsetterNEVERinvoked/
SAVEfromROUTINGandADD/cancel-throw20tickretry/reentry/quarantine/old88regressions.

Export **10,385,075bytes** SHA256
`806a7cadeb04f6106bba46b612f466c984dd18449e5149696e421224f7d86c75`,
previouscentrifuge outside mods`backups/centrifuge-fabric-validate3/`; README/hashes
updated, dependencies/olderbackups retained. All8exactpixelsinspected withnight/
rain/occludedgeometry/smallGUI caveats.142/145hovertooltip belongsOLDIGNIS8phial,
notcrystal;144visiblephysicalitem+ventpuffs notall7FXframes/audio proof. Fullvisual/
audio/API/addonI/O/chunk-unload/crossfilepowerloss/ordinarypackaged/EULA/remote/
fullsurvival/fullTC4/NeoForge NOTclaimed. **RealVisNet/EARTH acceleration remains
OPEN**, honesttooltip/noalways0stub. Next bounded group begins now: basicnode
stabilizer with realordinary-node lock/recharge effects, then separate actualpower.

<details>
<summary>История M06e — не активные команды и статусы</summary>

**CURRENT SERVERPASS:** FULL`crystallizer-fabric-validate2`exit0, **210JUnit/
92Fabric/all92required**,0failures/errors/skips, **3767unchangedinputs/64Python
83.006s**. Snapshot `.tmp/crystallizer-fabric-validate2-inputs.json`, nativeXML+
JARinspectionрядом. OneproductionJAR **10,385,075bytes**, SHA256
`806a7cadeb04f6106bba46b612f466c984dd18449e5149696e421224f7d86c75`, ZIP/notices/
testisolation/singleintermediaryAW/exactrecipe+nativeart/noVisNetstubPASS.
**Launching `crystallizer-fabric-client1 -Scope crystallizer`**, freshaccepted
STOPPEDcentrifugeparentONLY.8exact138–145 NOTyetobtained/inspected, no acceptance/
exportyet; usablemods remainsM06d8f13de6d. DoNOTduplicateGradle/game/edit source.
Oldinfusionwrapperinownbatchpassedstrictoldjarassertions unchanged; allnewcases
inclphysicalpoweredhopper/compoundprivacy/20tickretrypassed. Noactivecurrentagents;
AfutureThaumatoriumREADONLYaudit maystillrun, notsource/buildblocker.

**server3 preflight: 91/92**, ALL4newcrystallizer casesPASS inclrealpoweredHOPPER/
ordereditem/unknowncompoundtooltip/private1000/1005/timers/callbackSAVE/20tickretry.
OnlyoldInfusionRuntimeChecks249failed missing physicaljar, afterallmaincraftsteps;
root isolates ONLYitswrapperbatch, no oldassertion/gameplay changes. First2preflight
failures/changedfixture detailsbelow, server3XMLarchiving. Freshsnapshot2/FULL2/
Python2 launching; no clientworld yet. Currentengine sourcesunchanged sinceAfreeze;
clientbalancedlookup/harness/testfixturesrootcorrected, no assertionsweakened.

**LATEST:** compile1exit0. FULLvalidate1exit1: actual210JUnit0fail/3767unchangedinputs,
64Python89.259sPASS; 2of92nativefailed (priceassertion/hopperroute). FULL1XMLarchived.
RootJava21floatprobe proves .9F−5/100F gives **5→424,15→1275**, not1274 duefloat
multiplicationroundingBEFOREintcast; testone-short1274/exacttwoTerra2450 corrected,
productionpriceunchanged. RootfullCreadfoundbalanced() incorrectlyusedMATERIALS,
fixedtoexistingSHARDS.get("balanced"); no newcrystalclientworldstarted.
NativeGameOnlyserver2exit1 thenexposed primaltooltipfixture (freshknowledgeALREADY
knows6primals), nowusesgenuinelyunknowncompoundpair+onelearnedtype; typeprivacy
assertionsretained. Hopperdiagnosticactualpending1/nativeinterfaceTRUE/emptyhopper,
addingREALpowerabovehopper+coherentBE/worldstate assertion, notbypassingIOsafety.
RetrycasealsointerferedwithsharedSavedData activityinothernativefixtures;4newcases
putinseparatebatches, same92requirednames/≥20tickretry/everyownershipassertion kept.
Server2XMLarchived, **launching GameTestOnlyserver3**; snapshots1nowhistorical,
freshsnapshot/FULL/client/exact8pixels stillrequired. No acceptedM06e/exportyet.
Allcurrentownersdone; A futureThaumatoriumauditread-onlymaybeactive, notgate.

**CURRENT ALL A/B/C SOURCEFROZEN**, currentimplementationsdone. Root wiredBER/
sixargFabricbuiltinitem/7tickclientvent callback, nativeContainerScreencapture
allowlist+neutralnorandomhoverrender, addedold_centerreportfield. Exact8captures
138–145confirmedbyC. Chosenbalanced2ALREADYCRAFTEDingredientfixturesexplicit,
notcrucibleacquisition; extraRAWplanks3/sticks2→realnativewoodpick/damage1 for
propertoolmine. Savedchestplaced/nochestgrant. Sevenpaidcraft3150AQ/3150ORDO/
1350TERRA/fourfull1000tickcycles/nativechest2+physical2. No VisNetstub/grants.
**Launching CompileOnly `crystallizer-fabric-compile1`**. DoNOTduplicateGradle/game/
edit sources. No compile/native/client/visualPASSyet; A futureThaumatoriumaudit
read-only stillactive, notbuildblocker, BfutureVisNetdone. AcceptedmodsM06d8f13de6d.

B futureVisNet READONLYauditDONE `.tmp/visnet-dependency-closure.md`, root summary
[VisNetprep](analysis/2026-10-17-visnet-prep.md). Bcurrentitem/tests SOURCEFROZEN.
Minimumrealpower =basicstabilizer+transducer+NORMALnode(+relay), noadvancedconstruct/
infusiondependency. Networkgrants RAWcentivis-compatible, notwholevis; futureCF-
independent MAXprimalreduction and9quanta fluxmetadata8 needexplicitgates. Current
M06e baseline/codeunchanged; no falseVisNetstub or fullnetworkclaim. A future
Thaumatoriumaudit stillread-only, Ccurrentrenderer/client active; noGradle/game.

**A/B SOURCEFROZEN/root-reviewed, C current client/renderer ещё active.** A12files
API/hashes `.tmp/crystallizer-a-api.md`: fullcubeFACINGblock/engine/nativebaseline,
SAMEexecutor/paidROUTINGbeforecallbacks/exactchest-hopper-furnace rawadapter,
queueV2readsV1/in-flightSAVE/stableUUID/terminaladmission+removal+pickup receipts.
No receiptcompaction/crossfilepowerloss/fulladdonI/O/VisNet claims. Root normal
BlockItem placement matchesA FACING property; no setter hook needed.
Integration audit originalBlockTube310 found oldsharedItemBlock excludedmetadata2
AND7 from valve handtoggle. Root narrowedacceptedBlockTube guard to include both
newindependentmachine BlockItems (CFmissedpreviousgate), no other handtogglechange.
NewBrecipes helper regression proves nativeplaceabovevalve/actualDOWN/held1paid,
valveflowunchanged, PLUS ordinaryconstructstillhandtoggles/notplaced/notspent.
NoGradle/game yet; old88caseswillrerunwithnew92afterCfreeze. Aproductionfrozen,
Bfilesfrozenexceptexplicitrootintegrationregression; oldCFsources unchanged.
A now independent READ-ONLY future `.tmp/thaumatorium-dependency-audit.md`;
B futureVisNetclosure separately. Neither audit blocks currentCfreeze/central
validation; no new future production. Root prepared ignored build-inspection
helper `.tmp/inspect_crystallizer_build.py`, expects210JUnit/92Fabric by source
only, **NOT executed results**. Snapshot/build/runtime/client/pixels stillpending.

**B SOURCEFROZEN/root-reviewed:** new CrystalEssenceData/ItemCrystalEssence,
ObjectAspectCatalog onlyEMPTYbase,2newnativehelpers/4entries +5codec JUnit tests.
Root integrated4wrappers/requiredmanifest **92expected**, NOT compiled/run.
APIs `.tmp/crystallizer-b-api.md`: create/data/color/tooltipViewer; immutableFULL
orderedpositive list/explicitEMPTY/nativeintoverflowbound, no singletontruncation/
reroll; unknown/duplicate/fractional/oversizedwholedecode refused withoutpartial.
ItemownRandom !=display500ms/worldRNG. IndependentoriginalBlockTube184–195/241–243
confirms **crystallizer FULLCUBE outline+collision**, not CF compact outline.
Nativehelpers sourcecover paidrecipes/timing1000vs1005/powerphase/save/reload/
quarantine/exactchesthalf/hopper8/furnace/unsupportedsetterNEVERcalled/produced
scan20+distillation/saveFROMROUTING+ADD/retry. Actualexecutionstillpending A/Cfreeze.
No fullchunk-unload/arbitraryfilesystempower-loss/VisNet/addonI/Oparity claims.
**A/C remain active, noGradle/game.** Acceptedmods staysM06d8f13de6d.
Static wrapper/manifest equality confirmed92 unique required names, not execution.
B now independent READ-ONLY future VisNet dependency closure audit
`.tmp/visnet-dependency-closure.md`; current B item/tests stay SOURCEFROZEN, this
prep must not delay current validation or change active M06e source/ownership.

**Root asset preflight3: 38/38 PASS84.050s**, log
`.tmp/crystallizer-assets-preflight3.log`; ни compile/native/client PASS не означает.
ВсеA/B/C ещё реализуют свои части, Gradle/game НЕзапускать до ALLSOURCEFROZEN.

Assetpreflight2 reachedOBJ check: establishedcanonicalLF output vsretainedCRLF
source expectedrawbytes incorrectly. Root verified BOTH completeOBJcontent matches
afterONLYnewlinecanonicalization/hashsameformat-audit; testusesEXISTINGconverter
canonical_file_bytes, PNGstrictbytes unchanged. OBJ/PNG neveredited. Asset-only
preflight3 completed PASS above; no nativebuild/game/sourcefreeze yet, allA/B/C active.

Root7paidcraft data arithmetic independently verified3150AQ/3150ORDO/1350TERRA,
не runtime payment. `.tmp/export_crystallizer_binary.py` syntaxOK/NOTRUN, guard
old8f13de6d + futurebackup`centrifuge-fabric-validate3` outside mods; capture8/138–145
pending C confirmation. Assetpreflight1 failed ONLYnewtest wrongrelay64 assumption;
actual/source bytes + read-onlyformat audit prove **vis_relay32x32**, shell64x64,
item16x16. Corrected test, textures untouched; preflight2 is historical, result3 above.
Root blockitem tooltip explicitly says unassistedonly/VisNetdeferred, no rewrite
originalresearch prose. A/B/C stillactive; noGradle/game/snapshot/nativePASSyet.

Сразу после принятия/экспорта M06d начата следующая bounded unit. Три activeowners:
A `ses_eba0c66dcffesx8wm2sjjssC8n` engine/block/SAMEatomicprofile/paid escrow/native
inventory/DeferredDrops in-flight save; B `ses_eba0c3e38ffe2MrPF5RN6iMsr3` fullordered
crystal item/data/EMPTYbase/nativechecks; C `ses_eba0c086cffe6eh2v6uL2pSttp` newOBJ
renderer/item/FX/focusedclient. [Актуальный контракт](analysis/2026-10-17-crystallizer.md).
Root registry/resources/recipe/tag/book/icon/tint/tooltipviewer/status notice/
scope-stage-report/PShelper integrated, **NOT compiled**. Primary research test+
3asset regressions added; asset-only preflight3 PASS38, no Gradle/game.
**До ALL SOURCEFROZEN не запускать Gradle/game**, не менять owned files. Никакого
NeoForge/commits/push/releases. Current verification описывает точные historical
M06d inputs, не новые активные sources; usable mods =8f13de6d/M06d.

Scope: honest +1per5tick baseline/typed1 production/fullpositiveordered item API/
durable inventory or directional paid output. Real VisNet/EARTH acceleration
explicitly deferred (serыйtooltip), **no always0drain/API stub**, full M06/M09 не
закрыты. Exactcorrectrecipeiron2/dispenser1/balanced2/planks2/basic1/construct1,
5AQ15TERRA5ORDO, nativeIDessentia_crystallizer/classoriginalsingleL spelling.
ThaumcraftTubes.ESSENTIA_CRYSTALLIZER/_ITEM/CRYSTALLIZER_BLOCK_ENTITY;
ThaumcraftContent.CRYSTAL_ESSENCE/_DATA, ItemCrystalEssence/CrystalEssenceData Bowned.
Fresh parentONLYacceptedSTOPPEDcentrifuge below; bench1874,216,377, 8captures138–145
proposalC. Finite7paidcraft3150AQ/3150ORDO/1350TERRA, real savednormalnode1551f99565f9
suggested foractualcharge, RAW1+FUEL1→2IGNIS+2POT4→fouractualcrystals chest2/physical2,
nativepause/mine/reorient/recover; Cfinalplan/fixtures/actualruntime pending.

</details>

### Предыдущий принятый checkpoint: M06d конструкция/центрифуга (Fabric-only)

**Принят и экспортирован** `centrifuge-fabric-validate3` / `centrifuge-fabric-client2`:
**204JUnit / 88Fabric / все88required / 61Python87.619с / 3742unchangedinputs**,
0failures/errors/skips. Клиент **130.199447с / 2608ticks**, все8exact130–137
осмотрены. [Client](porting/centrifuge-client.json), [verification](porting/verification.json),
archive `.tmp/centrifuge-accepted-verification3.json`, snapshot/inspection/XML
`.tmp/centrifuge-fabric-validate3-*`. При записи currentmatch=true/historical=false.
MainJava286, не процент полноты. Все agents/Gradle/game stopped; NeoForge/commits/
push/releases не было. Ниже старые статусы не являются активными командами.

**Единственный STOPPED parent следующей группы:**
`fabric/run-clienttest/saves/centrifuge-smoke-e42a8f96-aa32-49cb-a360-306488df1b0e`.
RootnativeNBT audit `.tmp/centrifuge-client2-native-stopped-audit.json`:
**81non-nodeBE/20jars333/72candles**, старые72BE/18jars325 сохранены. Newbench1842,
216,377 пуст; newfurnace1850,216,377 BurnTime495/items/aspects empty; alembic1850,
217,377 empty, CF1850,218,377 IN/OUT empty/defaultNORTH, buffer1850,219,377 empty/
all6open/chokes0, connectorWEST1849/EAST1851 atY219 empty/31IGNIS+31ORDO, jars
atY218 IGNIS3/ORDO5 unlabelled. CFcount/rotation transient, не фиксировать как save.
Futureclockexceptions **1722/1754/1786/1818/1820/1850** at216,377 — only paid
BurnTime/matchingLIT decay; inventories/aspects exact/no newignition. Original1465
fuel3/B0 remains exact, НЕclockexception. Newfurnace report515→stopped495 are20
naturalclosingticks, не рассинхрон созданныхресурсов.

Gold7 final `{aer:4550,aqua:50,ignis:5000,ordo:40,perditio:4550,terra:3560}`;
mainFrost exact `{aer:1951,aqua:2436,ignis:2846,ordo:933,perditio:431,terra:464}`.
Basic2/DROP5/emptyphials2/blank1/filter0/savedPOT8phial1+earnedIGNIS8phial1,
3wands/allfoci/gear/knowledge/XP11/apple retained. CENTRIFUGE alreadycomplete,
no researchfixture thisrun. Genuinecharge gains1150AER/2200AQ/4000ORDO/570PD/
1980TERRA/0IGNIS,100events,0sessionregen, caprounding50AER/30PD/20TERRA.
SavedNORMALnode1623,218,374 UUID9bbeaa03 base80/currentAER59/AQ36/IGNIS70/ORDO29/
PD61/TERRA55, no predetermined futurecharge/regen guarantee. Otherpriornodes retained.

11paidcrafts4950AQ/4050ORDO/450AER/1440TERRA/450PD +ordinarylabelbinding. RAW4+FUEL1
gaveIGNIS8newphial +8POT→**3IGNIS+5ORDO**,2588POSTchecks/66transferrows/68balances,
same-serverall6public8, pauseprocess29over45POST, primalrefusal46checks,
4realinspections/4indexedportactions,0lost/granted. ConstructUUID27723fde recovered
afterrealheldmining; blankUUIDe4150043 physicallyreturned/pickedup. Nativebinding
remainderoccupiedempty7; actualPICKUPswap payer7/phial13 preservedunchangedresource.
Client1failedslotexpectation and firstserver occludedlabel fixture are diagnostics
only; no sourceLOS/geometry/menu/payment weakening, failedworld NEVERINPUT.

Guardedexport **10,310,076bytes**, SHA256
`8f13de6dce8f87f0c50381c515f05f7322638893ebec652e887424ada62195ec` in
`../Thaumcraft-Fabric-1.21.1/mods/`. Priorbuffer backedupoutside mods at
`backups/buffer-fabric-validate3/`; README/SHA256SUMSupdated, dependencies/older
backups intact. Ordinarypackaged/EULA/remote/fullsurvival/API/fullTC4/NeoForge /
fullGUI-item-light-FX-audioparity and audio listening remain unverified.
Следующая bounded unit **M06e unassisted crystallizer/typedcrystal/durableoutput**
начинается сразу; [prep](analysis/2026-10-17-crystallizer-prep.md),
`.tmp/crystallizer-{input-item-audit,output-transaction}.md`. Recipe corrected
**iron2/dispenser1/balance_shard2/planks2/basic1/construct1**, неdiamond/quartz.
RealVisNet/EARTH acceleration deferredexplicitly, noalways0stub/fullM09claim.

<details>
<summary>История M06d — не текущие команды и статусы</summary>

**ТЕКУЩЕЕ client2PASS:** run7eeeffe1, STOPPED
`fabric/run-clienttest/saves/centrifuge-smoke-e42a8f96-aa32-49cb-a360-306488df1b0e`,
130.199447s/2608clientticks/2588POSTchecks, all8exact130–137rootinspected.
Actual8POT→3IGNIS+5ORDO, bufferpublic8onall6ports/same-server thenjars3+5,
primalIGNIS8withdrawal46checks/pauseprocess29over45POST/11paidcrafts4950AQ...,
constructUUID27723fde/blankUUIDe4150043actualphysicalpickup; no grants/relocations.
Payer7/phialcursor actualnativePICKUPfixed/actualphialdestination13. Native stopped
readback **81nonnodeBE/20jars333**, NOT82 (new9BE vsbuffer's10), audit
`.tmp/centrifuge-client2-native-stopped-audit.json`. Newfurnace1850 reportBurn515,
actualSTOPPED495 (20nativeclosingticks), items/aspectsempty; futureallowpaiddecay.
Finalgold `{aer:4550,aqua:50,ignis:5000,ordo:40,perditio:4550,terra:3560}`;
savednormalnode UUID9bbeaa03 nowAER59/AQ36/IGNIS70/ORDO29/PD61/TERRA55, base80
exact, charge100events/gains1150AER/2200AQ/4000ORDO/570PD/1980TERRA,0IGNIS,
rounding50AER/30PD/20TERRA/nativecharge-sessionregen0. 66transferrows/68balances/
4nativeinspections/4indexedportactions. SavedPOT8unchanged+earnedIGNIS8unchanged,
emptyphials2/basics2/DROP5/filter0/blank1/3wands+allfoci/gear/knowledge/XP11 exact.
**FULL`centrifuge-fabric-validate3` RUNNING**, Python61PASS87.619s; doNOTduplicate
Gradle/game/edit sources. Allagentsdone; no acceptance/exportyet, usableM06c.

**СЕЙЧАС:** `centrifuge-fabric-server3 -GameTestOnly`exit0, all88inclNEWactual
inventory-FIRSThotbar7payer/phial nativePICKUPregressionPASS. Freshsnapshot3
`.tmp/centrifuge-fabric-validate3-inputs.json` captured AFTERthatpreflight.
**RUNNING `centrifuge-fabric-client2 -Scopecentrifuge`**, freshacceptedSTOPPEDbuffer
parent; Pythonassets3alsoinflight (independent/noGradle). Не дублироватьGradle/game/
не менятьsource. ПослеclientPASS exact8PNG130–137inspect +FULLvalidate3 required;
не выдаватьserver3oldreportзаfreshFULL3. Helperrecordstrengthened12craftrows/
8conversions/anyactualsplit/same-server6public8/positivepause/primalrefusal/
physicalconstruct+blankUUID/actual4950AQ...price/zeroLoss. Noacceptance/exportyet.

**LATEST client1FAILED** run5e5d2f0b/world73046d45,7PNG130–136. Nativefinitecharge/
11paidcrafts/plainconstructplace-heldmine-physicalUUIDrecover/adaptiveprimalrefusal/
positiveprocesspause/eightconversions ACTUAL5IGNIS+3ORDO/fullbuffer8 allpassed.
Late ordinarybinding returnedunchangedearnedIGNIS8phial intoemptyhotbar7; script
wronglyrequiredslot7empty forpayerreturn. Read-onlystoppedNBTconfirmedactualIGNIS8
slot7; clientstoppedwithpayeroncursor, failedworldisDIAGNOSTICONLY/NEVERINPUT.
Root fixedONLYclient withnativePICKUPpayer7<->actualphialcursor thenfree-mainstore,
no deletion/refund/output/visgrant. NewnativeCentrifugeRuntimeChecks regression
reproduceshotbar0..6occupied/7empty, genuineinventoryFIRSTnamedcatalyst and exact
nativecursor swaps. Allproductionunchanged, snapshot2historical; freshsnapshot3/
client2fromacceptedSTOPPEDbufferparent +FULLvalidate3 needed. Noactiveprocesses.

**СЕЙЧАС SERVERPASS:** FULL`centrifuge-fabric-validate2`exit0: **204JUnit/88Fabric/
all88required**,0failures/errors/skips,**3742unchangedinputs/61Python84.334s**.
Snapshot `.tmp/centrifuge-fabric-validate2-inputs.json`; freshXML/JARinspectionрядом.
OneproductionJAR10,310,076bytes SHA256
`8f13de6dce8f87f0c50381c515f05f7322638893ebec652e887424ada62195ec`,ZIP/notices/
testisolation/singleintermediaryAW/2realrecipes+distinctmodelsPASS. **Запускается
`centrifuge-fabric-client1` -Scopecentrifuge**, freshacceptedSTOPPEDbuffer copy,
8exact130–137 NOTyetobtained/inspected. Не дублироватьGradle/game/не менятьsource;
allagentsdone. No client/visualacceptance/exportyet, usablemods remainsM06c.

**LATEST compile1exit0 / FULLvalidate1exit1:** 1requiredcasefailed atpaidfilter
setup, NOTCFconversion. Bfixture placedfilterNORTHbehindrealCF, Fixture.aimfromSOUTH
raycorrectlyhitsCFfirst; strictBlockTubeLOS correctlyrefused/no labelpayment.
Root changedONLYnewtest: explicitlyassertoccludedrefusal preservingSAMEheldlabel,
thenplaceSOUTHaccessiblepaidfilter+actuallyspendthatlabel; allretirement/dirty/
fullstate assertions retained, productionLOS/geometry/engineunchanged. Rawfailed
log `.tmp/centrifuge-fabric-validate1.log`; snapshot1historical. Freshsnapshot2/
FULLvalidate2/client1 needed. Allagentsdone, no currentGradle/game now.

**ТЕКУЩЕЕ: ALL A/B/C SOURCEFROZEN/root-reviewed, ВСЕagentsdone.** Cnewrenderer+
clientAPIready, exact8captures130–137confirmed, rootwiredBER+sixargbuiltinitem,
addedreportold_centerfornextacceptedparent. **Запускается CompileOnly
`centrifuge-fabric-compile1`**, no duplicateGradle/game, sourcesfrozen. No current
compile/native/client/visualPASSyet. Story11realpaidcrafts +realconstructplacement/
nativeheldbreak/sameUUIDpickup, originalnativeCFrotor/positiveprocesspause/primal
refusal/adaptivealembic/8POT→8chosencomponents (singletypesallowed)/physicalblank.
Allsame-serverfullstate/6portbufferamountequality, old72BE/18jars325 retained;
no resource/RNG/clock/backend grants. Acceptedmods remainsM06c/c3da2bf7 untilgate.

B'sreadonlynextauditDONE `.tmp/crystallizer-input-item-audit.md`: originalJAR/
bytecode/pinnedreference correct futurecrystallizerrecipe **ironINGOT2/dispenser1/
BALANCEDSHARD2/planks2/basic1/construct1**. Rootwrongread-onlyiron4/diamond/quartz
assumptions correctedBEFOREANYimplementation. CurrentM06drecipes/sources unchanged.
Existingnative`balance_shard`/6CRUCIBLErecipes are realdependency; no newquartzalias.
Futurebaselinefreshautonomousoutputtick1005 (fill5+1000progress), pendingreload1000;
fullpositiveorderedlist itemAPI !=producedtyped1; ownitemRandom vs500msdisplayonly.
Bothnextauditsdone/currentA/Bfrozen; Ccurrentrenderer/client stillactive, noGradle/game.

Aread-onlynextauditDONE `.tmp/crystallizer-output-transaction.md`; all3CFproduction
hashesunchanged, no needwaitonA. Futuregate mustfixknownDeferredDropssaveINSIDEADD
in-flightgap (currentrowremovedbeforenotify/saveonlypending), paidlevelescrow /
closedrawnativeinventoryadapters or oneoriginaldirectionalresidualdrop, no write-
then-throwsetterdupe/always0VisNetstub/crashatomicityclaim. ЭтоONLYread-onlyfuture
contract, **не currentM06dfix/не sourcefreeze reset**. Bfutureinput/item audit
active; currentBtestsSOURCEFROZEN. Ccurrentrenderer/client stillactive, noGradle/game.

**СЕЙЧАС A/B SOURCEFROZEN/root-reviewed:** newCentrifugeRuntimeChecks+
CentrifugeAtomicRuntimeChecks integrated3wrappers/requiredmanifest **88expected**,
notrun. Covers actualpaidrecipes/wornfloatdiscount424/plainconstructnoBE/native
placementdefaultNORTH/fullcollision, allINOUTtiming39→38/pause/reload0/quarantine,
genuineCFabovebufferstarvation and callbacks/sharedretirement. Expecteddirtyfailure
WARNfixtures withnativecomparatorneighbors documented B, NOTreasonwaiveassertions.
C renderer/client ещёactive; **не запускать Gradle/game**. Areadonlynextoutput
auditactive, currentAproductionfrozen. Current testscompiled/executedнет.

**НОВЕЙШЕЕ:** A SOURCEFROZEN/root-reviewed, толькоNEWTileCentrifuge/BlockCentrifuge+
EssentiaTransfers изменены. API exactplanned, `.tmp/centrifuge-a-api.md` содержит
hashes/контракты. SAMEpreparedexecutor/globalMUTATION/typedlocalrequests +actual
privateTickphase gates fordraw/completion, oneimmediatecomponentRNG onlyready.
IN39→38sameTick/OUTblockedcountdown/reload0/OUTonlycontainer/poweredfaceblind
positiveIN inclnullFACE/explicitnegative-nullguards/quarantine реализованы.
Literalclientguard canfractionallyOVERSHOOT20 afterspin-down/resume, noinvented
clamp; defaultfacingNORTH/noitemclickassignment/fullcollision verifiedsourceaudit.
B/C ещёactive, **Gradle/game НЕзапускать**. A nowindependentREADONLYnextoutput
transactionaudit `.tmp/crystallizer-output-transaction.md`, неcurrentproduction/
неблокируетM06d centralvalidation. Registry/resources/harnessrootalreadywired,
nativewrappers/BER/itemhook pendingB/Cfinalreports.

Rootpreflight `.tmp/centrifuge-assets-preflight1.log` **35/35PASS82.414s**;
dataarithmetic11paidcraft4950AQ/4050ORDO/450AER/1440TERRA/450PD пересчитан из
actualrecipes, этоНЕruntimepayment. Preparedignoredhelpers
`.tmp/record_centrifuge_client.py` / `.tmp/export_centrifuge_binary.py` syntaxOK/
NOTRUN, предполагают8exact130–137 после подтвержденияC, oldc3da guardedbackup
buffer-fabric-validate3 внеmods. Никаких nativeM06dbuilds/tests/client ещёнет.
Read-only [nextcrystallizerprep](analysis/2026-10-17-crystallizer-prep.md) готов:
baselineбезVisNet реально50с/unit, ускорениеEARTH требует отдельныйREALVisNet,
неalways0stub. Typedcrystal1 !=etherealessence/shard, durableejectioninventory/drop
иpost-callbackownership требуютгейта; эта будущая unit НЕреализуется параллельно.

Сразу после accepted/export M06c начата следующая группа, без ожидания. Три active
ownerA/B/C (`ses_eba0c66dcffesx8wm2sjjssC8n` / `ses_eba0c3e38ffe2MrPF5RN6iMsr3` /
`ses_eba0c086cffe6eh2v6uL2pSttp`): engine+block/новыеnativechecks/renderer+client.
Rootregistry/plainconstruct/resources/recipes/book/icons/harness/integration.
[Активныйконтракт](analysis/2026-10-17-centrifuge.md), sourceaudits в старом
[buffer/centrifuge spec](analysis/2026-10-17-buffer-centrifuge.md). **Не запускать
Gradle/game до ALL SOURCEFROZEN**, не перезаписывать ownedfiles. Активногосборочного/
игровогопроцесса нет, никакогоNeoForge/commits/push/releases.
RootregistrationsThaumcraftTubes.CENTRIFUGE/_ITEM/CENTRIFUGE_BLOCK_ENTITY separate,
ThaumcraftDistillation.ALCHEMICAL_CONSTRUCT/_ITEMplainBlock/noBE; exact2recipes/
models/loot/names/book/icon/scopesharness и3assetregressions added, NOT compiled.
ScopecentrifugefreshcopiesONLYacceptedSTOPPEDbufferworldbelow, bench1842,216,377;
8captures130–137proposalC. A/B/C APIs/code/runtime ещёнеaccepted. Existingverification
describes historical exactM06cinputs, NOTcurrentM06dsource. Usablemods remainsc3da2bf7.
ClientC givenfinite11paidcraftbudget4950AQ/4050ORDO/450AER/1440TERRA/450PD, actualnode
recharge,2tubebatches/newpaidbuffer, raw4+fuel1→8IGNISprimals+8POTENTIAcompound→
8chosenimmediatecomponents withnativeRNG(notfixedmix); allrawinputs+old72BE/18jars325/
gear/knowledge saved declared. Nofinishedconstruct/CF/buffer/ingredientalembic/vis/
essentia/XPgrants. Source freeze+centralcompile/tests/client/exactpixels/export
ещёвпереди; закончитьэтугруппу, затемсразуследующуюпоплану.

</details>

### Предыдущий принятый checkpoint: M06c буфер (Fabric-only)

`buffer-fabric-validate3` FULLexit0 **204JUnit/85Fabric/all85required**,0failures/
errors/skips, **3726unchangedinputs/58Python86.897s**. `buffer-fabric-client3`
SCRIPT_PASSED, **все8exactPNG122–129 осмотрены**,77.7119747s/1553clientticks.
[Client](porting/buffer-client.json), [verification](porting/verification.json),
archive `.tmp/buffer-accepted-verification3.json`, snapshot `.tmp/buffer-fabric-validate3-inputs.json`,
inspection `.tmp/buffer-fabric-validate3-jar-inspection.json`. При записи
currentmatch=true/historical=false. MainJava283, не процент полноты. Все процессы/
agentsdone, новогоNeoForge/commits/push/releasesнет. Ниже история не activecommands.

Accepted **STOPPED** parent следующейгруппы:
`fabric/run-clienttest/saves/buffer-smoke-fe4e125e-6ece-47f3-baf7-1d42d325b184`.
Read-onlyNBTrootconfirmed **72non-nodeBE/18jars325/old72candles**; nativeaudit
`.tmp/buffer-client3-native-stopped-audit.json`. Actualnewbuffer1819,217,377 empty/
all6portsopen/chokes0; bellowsSOUTH1819,217,378 inwardNORTH poweredbyexternal
lever1819,216,378. jarA1819,216,377 POT4unlabelled, jarB1819,216,376 IGNIS4;
connector1819,217,376 empty/up31IGNIS/SOUTHopen. Prior62BE/16jars317/72candles exact.

**Finalgold7** `{aer:3850,aqua:2800,ignis:5000,ordo:90,perditio:4430,terra:3020}`;
mainFrost `{aer:1951,aqua:2436,ignis:2846,ordo:933,perditio:431,terra:464}` unchanged.
Loosebasics0/emptyphials7/POTENTIA8phial1unchanged/blank1/VisFilter1/DROP7,3wands/
allfoci/worn0/0/1/1/apple/XP11/knowledgeexact. Nativecharge gained900AER/1400AQ/
700IGNIS/900ORDO/1200PD/900TERRA,60events,0sessionregen/capround;3paidcrafts
1350AQ/900ORDO/1440TERRA+ordinaryphials8+labelbinding, inventory-FIRST remainder.
1538POSTchecks/same-serverpublic8==4+4/every6portamount equality,34transferrows/
35balances/9indexedactions/5resonatorcalls/0loss/grants. NativeblankUUIDb054b183
physicallyreturned/pickedup, noitemrelocation. Blue/red125/126 darkconfirmedwith
nearestpixelcrops; localtorches/realchat/heldbuffer/label/jarsvisible, fullparitynotclaimed.

Native stopped **BurnTime1818=1045 /1820=1095**, both empty items/aspects; allow
only paidfueldecay/matchingLIT there plus1722/1754/1786 (now0), never newignitions.
Original1465fuel3/B0 and1722fuel1/IGNIS8 remain exact, not licensed clockchanges.
Client1failedcamera pickup cardinal-only; client2passedscriptbutnotaccepted due
mixedserver8/client7report; fixedONLYharnesscamera+same-serverobserver, production
geometry/authority/transfer notweakened. Failed/unacceptedworlds NEVERinputs.

Guarded exportDONE10,286,864bytes SHA256
`c3da2bf7be141c42513e440a989ca1282aa23137ee9d122bda84090429070ef1`,
`../Thaumcraft-Fabric-1.21.1/mods/`, README/SHA256SUMSupdated. Priorbinarybackedup
`backups/tubefilter-fabric-validate3/`; valve/voidjar/Frostretainedoutside mods.
Next **M06d alchemicalconstruct/centrifuge** startsimmediatelywithoutconfirmation;
[spec](analysis/2026-10-17-buffer-centrifuge.md), `.tmp/centrifuge-atomic-extension.md`
and `.tmp/centrifuge-source-audit.md` ready. NeoForge/packaged/EULA/remote/fullsurvival/
fullAPI/completeTC4/fullGUI-item-light-FX-audioparity andaudio listening remainopen.

<details>
<summary>История реализации M06c — не текущие команды, статусы и приёмка</summary>

**ТЕКУЩЕЕ client3PASS:** run580c1a71, STOPPED
`fabric/run-clienttest/saves/buffer-smoke-fe4e125e-6ece-47f3-baf7-1d42d325b184`,
77.7119747s/1553clientticks/1538POSTchecks. All8exact122–129 rootinspected,
same-serverfullmixedpublic8=4+4, all6portsamount equalityEVERYPOST, zero loss.
Raw+observations `.tmp/buffer-fabric-client3-{raw-report,inspected}.json`.
Darkblue/redmarker125/126 confirmed byexactpixelnearestcrops; localtorches don't
provefulllightingparity. **Идёт FULL`buffer-fabric-validate3`**, snapshot3current,
58Python86.897sDONE. Не дублироватьGradle/game/не менятьsource. Acceptance/export
ещёnotrecorded, usableJARpriorM06b. Noactiveagents.
Actualfinalgold7 `{aer:3850,aqua:2800,ignis:5000,ordo:90,perditio:4430,terra:3020}`,
raw2/fuel2→POT4jarA/IGNIS4jarB, buffer/connector/sourceempty.60chargeevents,
34transferrows/35balances/5nativeinspections/9indexedwandactions. Nativegoldgains
900AER/1400AQ/700IGNIS/900ORDO/1200PD/900TERRA, nocharge-sessionregen/capround,
UUID/baseunchanged. Basicloose0/emptyphials7/DROP7/VisFilter1/oldPOT8phialexact/
blank1/XP11/mainFrostexact; blankUUIDb054b183physicallypickedup, itemrelocations0.
Nextsaveallow paidemptyfurnace1818/1820 nativeBurnTime/LITdecay along1722/1754/1786.

**НОВЕЙШЕЕ:** client2rune b9326c5c/world21cebd4a SCRIPT_PASSED77.693s/1553ticks,
8PNG122–129, physicalblankUUID1237a416pickedup. НО rawactual_full_mixed_buffer
combinedSERVER8snapshotwithDELAYEDCLIENTpublictotal7; PASS неaccepted, неexport.
Root fixed ONLYobserver: server-threadsame-snapshot publictotal8==real4+4, EVERY
SERVER_POST equality for ALL6ports inclclosed. No engine/storage/source writes
or RNGchanges; client2rawarchived `.tmp/buffer-fabric-client2-raw-report.json`.
Freshsnapshot3/client3 fromacceptedSTOPPEDtubefiltercopy +finalFULL3needed;
client2world не исходник, allnewcapturepixels still awaitrootinspection. Noactive
Gradle/game/agents currently. Updatedclientharness invalidatesprevioussnapshot2.

**НОВЕЕ client1:** client1exit1/SCRIPT_FAILED PICKUP, rune97a3c38/world6311a782,
7exactPNG122–128 (notaccepted). Actualfinitecharge/all5crafts/nativechokes32→1→0→32/
mixed8/5inspections/jars4POT+4IGNIS/labelremoval passed. Harness cardinal-only
pickupapproach blocked byoriginalfulljar/furnaces/bellowscollision. Root changes
ONLYclient camera candidate search: diagonals +supportedfloor/actualnoCollision/
nativeitem-contactenvelope, never itemUUID/position/velocity/delay/outputgrants.
FailedworlddiagnosticonlyNEVERreuse, raw `.tmp/buffer-fabric-client1-raw-report.json`.
Snapshot1 nowhistoricalexactinputs; freshsnapshot2/client2 fromacceptedSTOPPED
tubefilterparent +finalFULL2needed. NoactiveGradle/game/agents currently.

**СЕЙЧАС serverPASS:** CompileOnlybuffer1exit0; FULL`buffer-fabric-validate1`exit0,
**204JUnit/85Fabric/all85required**,0failures/errors/skips,**3726unchangedinputs**,
fresh**58Python83.191s**. Snapshot `.tmp/buffer-fabric-validate1-inputs.json`,
nativeXMLиJARinspection рядом. OneproductionJAR10,286,864bytes SHA256
`c3da2bf7be141c42513e440a989ca1282aa23137ee9d122bda84090429070ef1`, ZIP/
notices/isolation/singleintermediaryAW/exactbufferrecipe+modelPASS. **Запускается
`buffer-fabric-client1` Scopebuffer**, freshcopyactualacceptedSTOPPEDtubefilter;
8exact122–129ещёнеполучены/неосмотрены. Не дублироватьGradle/game/не менятьsource.
Всеagentsdone; finalclient/visualacceptance/exportещёнет, usableJARacceptedM06b.

**ПОСЛЕДНЕЕ:** ВСЕ A/B/C SOURCEFROZEN/root-reviewed, активныхагентовнет. Root
подключил bufferBE renderer+Fabricbuiltinitem6argshook; exact8captures122–129
подтверждены C. Запускается CompileOnly `buffer-fabric-compile1`; не дублировать
Gradle/game/не менятьsource. Ещё никакого compile/runtime/client/visual PASS.
3paidcraftcost1350AQ/900ORDO/1440TERRA; fixturelever1recipe+1external, rawglass3/
clay1/stone1/2RAW+2FUEL, newemptydevices/inwardbellows/4vanillatorches. No finished
output/vis/XP/essentiagrants. Captures+actualruntime всё ещё unverified.

M06dread-onlyA `.tmp/centrifuge-atomic-extension.md` теперь DONE, не менялsource;
важный futurecontractpositivecompoundinputacceptsnullFACE (неnullASPECT),
sameexecutorIN/OUT/localcompletion/noarbitrarycredit. Из всехowners сейчас C
ещёactive; не запускать Gradle/game пока CнеSOURCEFROZEN.

**СЕЙЧАС:** A/Bproduction+tests SOURCEFROZEN/root-reviewed;3newnativewrappers+
requiredIDs подключены (**85expected**, NOT RUN). Bufferdata/preflight32PASS;
204JUnit/58Python ожидаются по sourcecounts, не выдавать за результаты. C renderer/
client ещёactive, поэтому Gradle/game НЕзапускать. A выполняет только read-only
nextcentrifuge atomicextension `.tmp/centrifuge-atomic-extension.md` (не блокирует
M06c build и не меняет текущие sourceinputs). Root ещё долженподключить C BER/item
hook и проверитьexact8capturecontract122–129 после его SOURCEFROZEN.

**НОВЕЙШЕЕ:** A SOURCEFROZEN/root-reviewed, новый EssentiaAtomicEndpoint.State/
Change, defaultsingletonadapters и SAMEexecutor+Attempt(selected,committedUnits,
failure); productiontube/jar/bufferqueryprofiles раздельны, accountingбезrandom
publictypequeries. SeparateTileTubeBuffer/BlockTubeBuffer APIs getTickCount/
getBellowsCount/getChoke/getAmount/isSideOpen/setSideOpen/cycleChoke/isQuarantined,
defensiveorderedgetAspects. Rawpaired tube/buffer sides beforecallbacks; sorted
bufferonlyresonator; nativewandsecondary integrated. Defensiveownership/null/
negativeguards, zero-container-takewithoutnotification; wholemalformedpayload
disabledverbatimquarantine `RejectedBuffer`, no truncate. B/C ещёactive, поэтому
никакогоGradle/game пока не запускать. A никакиеregistry/resources/tests/renderer
не трогал; rootreview не выявил нужды ослаблять серверную authority.

По правилу пользователя после accepted/export M06b СРАЗУ начата следующая группа.
Три active source owner: A `ses_eba0c66dcffesx8wm2sjjssC8n` — общий atomic multi-store
parent/defaultsingleadapters/executor+realbuffer/block/pairedports/geometry/wand/
inspector+необходимые oldtube/jarcaller adaptations; B `ses_eba0c3e38ffe2MrPF5RN6iMsr3`
— новые TubeBufferRuntimeChecks/BufferAtomicRuntimeChecks, не старые M06b files;
C `ses_eba0c086cffe6eh2v6uL2pSttp` — buffer renderer/item/focusedclient only.
Root owns registry/resources/recipe/book/icon/harness/wrappers/requiredmanifest/
integration/acceptance/export. **Не запускать Gradle/game до ALL SOURCEFROZEN**,
не редактировать ownedfiles одновременно; активногоGradle/game нет. Никаких
NeoForge/commits/push. Construct/centrifuge НЕ входят, M06d следующий.

Root уже добавил ESSENTIA_BUFFER/_ITEM, BUFFER_BLOCK_ENTITY (SEPARATE classes,
НЕKindBUFFER/НЕsealedcapacity1), exactCENTRIFUGE shapedrecipePVP/T T/PRP5AQ5ORDO,
particle/builtinmodels/plainloot/pickaxetag/ENRU/bookalias/icon4, newcataloguegate
test и2assettests; пока notrun/notcompiled. Scopebuffer вGradle/Bookstage/report/
PShelper клонирует actualacceptedSTOPPEDtubefilterworld ниже, bench1810,216,377.
Expected8captures122–129 покаproposalC, finalcontract ещё проверить. Renderer/BER
itemhooks и nativewrappers/requiredcases подключать после sourceownerreports.
Read-onlyNBTrootперепроверил62oldBE/16jars317; gold90ORDOнельзяtopup. ClientC
получил nativefinitecharge plan, saved5tubes/emptyphials3/blank1/phial8/VisFilter1/
DROP7, genuinephialbatch8 fromglass3/clay1, newvalve+restrict+buffercost1350AQ/
900ORDO/1440TERRA,2RAW+2FUEL→4IGNIS+4POTENTIA,8totalmixedbuffer/tworealjars,
rawports/chokes/inspector/labelreturn и declaredlocaltorchlightingfixture безclockwrites.
ParentinputphialremainderinventoryFIRST и6tickRot/headpreaim обязательны.
Root buffer-assets-preflight1 finished **32/32PASS80.235s**, resource-only/notbuild.
`.tmp/record_buffer_client.py` / `.tmp/export_buffer_binary.py` syntaxchecked,
preparedNOTRUN (scope8captures122–129 stillconfirmC); oldsha18169 guard backs up
tubefilter-v3outside mods. NativeTestwrappers/BErenderer/itemhook notwiredyet.
Новая M06c ещё не принята; verification ниже HISTORICAL exactM06binputs после
текущихsourceизменений, usablebinary остаётсяaccepted18169f73, НЕразрабатываемыйкод.

</details>

### Предыдущий принятый checkpoint: filter/restrict/oneway (Fabric-only)

`tubefilter-fabric-validate3` FULLexit0: **203JUnit/82Fabric/all82required**,0failures/
errors/skips, **3714unchangedinputs/56Python86.717s**. Один productionJAR ZIP/notices/
testisolation/единственный intermediary AW/3realshapelessrecipes+models PASS.
`tubefilter-fabric-client3` exit0/SCRIPT_PASSED, **все8exactPNG114–121 осмотрены**,
run `client-smoke-ba631792-91ff-406a-95b5-27cf0a35a152`,112.0367s/2244clientticks.
[Client](porting/tubefilter-client.json), [verification](porting/verification.json),
[контракт/история](analysis/2026-10-16-tube-filters.md), archive
`.tmp/tubefilter-accepted-verification3.json`; при записи sourceboundmatch=true/
historical=false. Snapshot `.tmp/tubefilter-fabric-validate3-inputs.json`,
inspection `.tmp/tubefilter-fabric-validate3-jar-inspection.json`. MainJava279.
Все M06b owners/Gradle/game завершены; никакого нового NeoForge/commits/push/release.

Accepted **STOPPED** save:
`fabric/run-clienttest/saves/tubefilter-smoke-cf7a6c49-0845-4cd6-a544-6352e8779cad`.
Actual upperPOTENTIA8 routed1787/1788/1789y218, earnedconnector1789y217,
jar1789y216; lowerIGNIS8 retained. Wrongdirection50ticks upstream0, nativewandUP
corrects30→15→14;2actualresonatorcalls,ONEblanklabelUUIDphysicallyreturned/pickedup.
2224POSTchecks/45transferrows/48balances/198chargeevents/0loss-void-topups.
5paidcrafts2250AQ/3060ORDO/1440TERRA/720PD; realinventoryFIRST remainder retains
oldPOTENTIA8phial unchanged. ActualNORMALnode1623,218,374 UUID9bbeaa03 spent
AIR29/TERRA27/IGNIS38/AQ42/ORDO31/PD30, gains2900/2700/3800/4140/3100/3000,
nativeIGNISregen1/caproundAQ60centivis, base80eachunchanged; never setvis/node/RNG.
**Finalgold7** `{aer:2950,aqua:2750,ignis:4300,ordo:90,perditio:3230,terra:3560}`.
**Actual loosebasic5/VisFilter1/quicksilverDROP7/blank1/emptyphials3/filledPOTENTIA8phial1**.
MainFrost remains `{aer:1951,aqua:2436,ignis:2846,ordo:933,perditio:431,terra:464}`;
3wands/allfoci/robes/goggles/XP11/apple/priorresearch/pools/scans/warp preserved,
only TUBEFILTERcompletion newdeclaredfixture.53oldBE/15oldjars309/72candles preserved;
newpatch adds9BE/onejar8, so next continuation should READ actual62BE/16jars317.
Allow native BurnTime/LIT decay at **1722,1754AND1786**, not clockfreezes. New1786
furnace may still burn its paid normalfuel; inputs empty/items/essentia exact.

Client1/2 genuinelyFAILED, not accepted: phial-in-matrix expectation contradicted
original inventoryFIRST; nativeviewHEAD needed real6tickpreaim beforeuse. Native
remainders+completeangledpose regressions pass; production reach/LOS/payment not
weakened. Both failedworlds diagnostic only, NEVER starting inputs. Exactraws kept.
Pixel limits: night/croppedjar/GUIoverlaps,115wrongResonatortooltip NOTwandvis/node;
fullGUI/item/lighting/FX/audio parity/audio listening/ordinarypackaged/EULAserver/
remote/fullsurvival/fullAPI/fullTC4/NeoForge remain pending.

Guarded export DONE:10,250,855bytes SHA256
`18169f73b820c81c4c2e67a6082214f7a6d0a19253b1f55c3b59a70b4302f409`,
in `../Thaumcraft-Fabric-1.21.1/mods/`, README/SHA256SUMS updated; previousvalve
backedup `backups/valve-fabric-validate4/`, priorvoidjar/Frost retained outside mods.
Sourceproject/.git stays in Thaumcraft-4.2-FOREVA. Next **M06c buffer only**, then
M06dconstruct/centrifuge; [spec](analysis/2026-10-17-buffer-centrifuge.md), read-only
`.tmp/buffer-transaction-design.md` and `.tmp/centrifuge-source-audit.md` ready.
User17.10: checkpoint is NOT reason to stop; begin next Fabric group immediately,
without asking confirmation. Never parallelGradle/game or develop NeoForge now.

### Предыдущий принятый checkpoint: клапан, резонатор, shapeless arcane (Fabric-only)

`valve-fabric-validate4` FULLexit0: **202 JUnit /79 Fabric**, все79 required,
0failures/errors/skips, **3694 unchanged inputs /53Python**. Один production
FabricJAR ZIP/notices/isolation/единственный intermediary AW проверены.
`valve-fabric-client3` exit0/**SCRIPT_PASSED**, **все7exactPNG107–113 осмотрены**,
run `client-smoke-2bd69f57-7216-4c54-9e07-c79ee84ee904`,53.289сек/1070clientticks.
[Client](porting/valve-client.json), [verification](porting/verification.json),
[контракт и история](analysis/2026-10-16-valves-resonator.md); source-boundmatch=true/
historical=false, archive `.tmp/valve-accepted-verification4.json`. MainJava278.
**Все владельцы, Gradle и game завершены; активных агентов/процессов нет.**

Accepted **STOPPED** save:
`fabric/run-clienttest/saves/valve-smoke-39c7d715-ac80-4b86-97a6-7fcafb0c3caa`.
Savedtube+recipelever→valve, iron4/netherquartz1/stick1→resonator; gold7 реально
оплатил900AQ/450ORDO/450AER, finalvis `{aer:50,aqua:860,ignis:500,ordo:50,perditio:950,terra:2300}`.
Вторая savedtube поставлена; **одна loose tube осталась**. MainFrostpayer exact
`{aer:1951,aqua:2436,ignis:2846,ordo:933,perditio:431,terra:464}`, другие wands/foci/
robes/goggles/offhand/knowledge/XP11/46oldBE/14oldjars301units/72candles сохранены.
Prior acquisition не повторялась. Новые raw/emptydevices/externallever/floor/camera
— fixtures, без vis/XP/essentia/output/research grants или clock/RNG overrides.

Native4rawcoal+1fuel→8IGNIS+8POTENTIA. Closure сохранила source7 при jar1, разрешая
остатку буфера стечь; resume довёл jar до8POTENTIA, верхний alembic сохранил8IGNIS.
**1049nativePOSTchecks/31transferrows/44balances/4inspections**,0void/loss/topups.
Наблюдались real PREcount%5 risingclose/fallingopen и20samples manualopen под
стабильным питанием. Save/rawports/vent/latch/locks/plain drop/comparator0,72реальных
shapeless arrangements/payment/quickmove/остатки/strict JSON+stream и exact
resonator chat/sound проверены сервером. Gate TUBES, не TUBEFILTER.

User дважды заметил harness-stall: client1 был SCRIPT_FAILED FIRST_FLOW (old
crouched pitch отправлялся позжеuse); client2 штатно закрыт exactownedwindow,
ждал незарегистрированный ChatScreen PNG112. Оба **НЕ PASS**, failedworlds не
переиспользованы. Теперь actual native Rot C2S идёт до scripteduse; единый capture
allowlist охватывает ChatScreen, любой кадр ограничен10сек, unsupported/changed
screen сразуFAIL, pendingctx публикуется, latecallback не перезаписываетFAIL.
Client3 прошёл полностью с новой acceptedvoidjar copy, включая ChatScreen112.

Видны book/две nativepreview/wheel+stem/open-closed/heldtool/realchat. 111показывает
native vent particles соседней basic tube при pressure/type transition, не
inventedclosureFX/flux.112heldtool частично закрываетcontrol/jar; full GUI/item/
lighting/FX/audio parity и audio listening не заявлены. Packaged install/EULAserver/
remote/fullsurvival/API/fullTC4/NeoForge всё ещё pending. No commits/push/release.

JAR10,235,830bytes SHA256
`5fa9b83378943e98e960c60607d08e39c363b54dcb0fbf6bfc8e05302dca21de`,
inspection `.tmp/valve-fabric-validate4-jar-inspection.json`. Guarded export уже
обновил `../Thaumcraft-Fabric-1.21.1/mods/`; README/SHA256SUMS обновлены. Старый
voidjarJAR+README+manifest в `backups/voidjar-fabric-validate2/`, прежнийFrost backup
сохранён; backups НЕ вmods. Source project/.git остаются в Thaumcraft-4.2-FOREVA.

Следующий bounded gate — [filter/restrict/oneway](analysis/2026-10-16-tube-filters.md),
пока read-only/безвладельцев; buffer исключён. Нельзя выдавать3готовые трубы или
topupvis: осталась1tube/50ORDO/50AER. Нужен настоящий новый batch tubes и зарядка
из реального сохранённого узла. При следующейкопии учитывать естественные
BurnTime/LIT обеих печей1722,216,377 и1754,216,377; не freeze/reset и не требовать
exact transient fuel clocks. Их actual items/essentia/filters/identity сохранять.

**ПРИОРИТЕТ ИЗМЕНЁН ПОЛЬЗОВАТЕЛЕМ: FABRIC FIRST.** Сначала полный TC4content на
Fabric; до этого не развивать/не билдить/не прогонять NeoForge. Не удалять уже
сделанныйNeoмодуль, не переносить common целиком вfabric. Старые NeoPASS ниже —
исторические. AGENTS/plan/devscripts/recorder обновлены: default толькоFabric,
новыйhelper `.tmp/fabric-content-validate.ps1`, НЕ старыйдвухloadera07helper.
Recorder default`--loaders fabric` не требует/не переиспользует staleNeoXML/JAR;
отдельныеloaders_checked/not_checked в новом отчёте. Текущие Tube wrappers Neo
уже существовали до коррекции, оставлены без новых проверок/дальнейших правок.

### Предыдущий принятый checkpoint: пустотные банки (Fabric-only)

`voidjar-fabric-validate2` FULLexit0/51s: **201 JUnit / 76 Fabric GameTests**, все76
required, failures/errors/skips0, **3682 unchanged inputs**, **51Python**; один
FabricJAR notices/isolation/singleintermediaryAW PASS. [Verification](porting/verification.json)
source-boundmatch=true/historical=false; archive `.tmp/voidjar-accepted-verification2.json`.
`voidjar-fabric-client2` exit0/**SCRIPT_PASSED**, **все7 exactPNG100–106 осмотрены**,
run `client-smoke-07c7b930-6dd0-47d7-88b2-85b78e7c3447`,4м1с/4829clientticks.
[Client evidence](porting/voidjar-client.json), [контракт/история](analysis/2026-10-16-void-jars.md).
MainJava276 — не процент готовности; все agents/Gradle/game завершены.

Accepted **STOPPED** save:
`fabric/run-clienttest/saves/voidjar-smoke-3eb55479-602a-4902-853b-58f93c50c093`.
Actualsavedjar+blazepowder+rawobsidian→voidjar450AQ/1350PD; rawpanes/slab→ordinary
jar90AQ; actualink_sac/slime/paper→4blanklabels. Итого goldslot7 payer540AQ/1350PD,
finalvis aer500/aqua1760/ignis500/ordo500/perditio950/terra2300. MainFrostpayerexact,
3wands/allfoci/wornrobes+goggles/37oldnon-nodeBEs/old12jars173units/72candles/XP11
сохранены, стараяacquisition не повторялась. Newraw/research/emptydevices/camera/
floor/daylight — declaredfixtures; никаких vis/XP/essentia/output grants.

8actualcoalBLOCKS с native13ignis+13potentia→**104 каждого**. Четыре genuine32held
drains наполнили void64 иordinary64; фактическискрафченныеblanklabels зафиксировали
nativeunseededtypeorder (lowerIGNIS/upperPOTENTIA). 3savedtubes поставлены настоящими
clicks: refill8, **32potentia deliberately voided**. Осталось potentia64jar+8phial,
ignis64jar+32alembic+8furnace=104. **387transferrows/218balancerows/4809nativechecks**,
каждый supplied=stored+voided; pending/physical/cursor тожеучтены. 3fuelcoalburnt,
1retained. Full64 отказал целой8phialбезрасхода; extraction/secondaryinsert56→64
сохранили label. Realbreak→physicalsameUUIDpickup→place64/filter/SOUTH/maxstack1.

Видно:100readablebook/originalO-J-P/base5/15;101nativepreview4.5/13.5;102emptyVoid
item/title;103Potentia×64tooltip+heldjar;104/106worldshell/purplecap/physicallabel/
3tuberoute/activefurnace;105actualPotentia×8phial. В103 малаяиконка/heldlid НЕ
доказывают явноразличимую liquidheight/colorparity;104still НЕвизуальныйэффект
удаления. 101/102/103/105 затемнены GUI, heldphial105тёмный. FullGUI/FX/audio/
lighting/liquidparity не заявлена, audio отдельнонепрослушан. Ordinarypackaged/
EULAserver/remote/fullsurvival/API/goals remain pending; NeoForgepaused/notrun.

FinalJAR10,213,492bytes SHA256
`4acc7c78e9d560d307fdd6e2603e6bbcdb067f5276a9edacdc294d8b547960e4`;
`.tmp/voidjar-fabric-validate2-jar-inspection.json`. По usercorrection «почему там
сфрост» exported sibling `../Thaumcraft-Fabric-1.21.1/mods/` **обновлён** доэтого
acceptedvoidjarJAR (тотже filename/новыйSHA), dependenciesunchanged. Передreplace
oldSHA проверенпротивacceptedFrost, старыйJAR+README/hashmanifest сохраненыв
`../Thaumcraft-Fabric-1.21.1/backups/frost-fabric-validate2/`, НЕвmods. README/SHA256SUMS
экспорта обновлены; source/buildJAR не менялись/новыйpackagedrunне заявлен.

Следующий bounded M06a — [клапан+резонатор+realshapelessarcane](analysis/2026-10-16-valves-resonator.md),
пока read-onlydraft/неimplemented/нетactiveowners. Не братьbuffersвэтотпакет.
Для nextstory учитывать ACTUALremaining3loose tubes иgoldvis выше, неделатьtopups.
**Важно savedworldinput:** новаяпечь1722,216,377 остаётся burning с1coal/8ignisbuffer,
ignis-alembic32full. В следующейкопии её BurnTime/LIT закономерноизменятсяnative
tick (TileAlchemyFurnace98–113); не требовать exactallNBTequalityэтихtransient
полей/неfreeze/неreset. Отдельно сохранять actualitems+essentia/filter/oldidentity.
Никаких commits/push/release. ПолныйTC4 всёещё НЕ готов.

### История реализации пустотных банок (ниже не активные процессы)

**ТЕКУЩЕЕ:** `voidjar-fabric-validate2` FULLexit0/51s: **201JUnit/76Fabric**, все76
required/0failures-errors-skips, **3682unchangedinputs**, freshPython51PASS/86.018s.
Ink_sac regression и actualtablefallback click4labels/0vis пройдены. Production
FabricJAR10,213,492bytes SHA2564acc7c78e9d560d307fdd6e2603e6bbcdb067f5276a9edacdc294d8b547960e4;
ZIP/notices/isolation/singleintermediaryAW/newjarassets+renderer/inkrecipe checked
`.tmp/voidjar-fabric-validate2-jar-inspection.json`. Обычная packaged установка этим
не проверена. Сейчас **уже идёт voidjar-fabric-client2**,freshacceptedFrostcopy,
7expected100–106. Не менятьsource/непараллельныйGradle/game. ClientPASS/все7pixels/
новыйsourceboundrecord ещё НЕ получены; verification.json всёещёacceptedFrost.

**Текущий процесс:** после inkrecipefix freshsnapshot2 создан; **уже запущен**
`voidjar-fabric-validate2` FULLFabric (bounded300s), отдельноfreshPythonassets2.
Не менятьsources/не параллельныйGradle/game. ЕслиPASS — client2изfreshFrostcopy,
семьexactPNG100–106; прежнийclient1 добровольноABORTED/exit1, не relabelPASS.

**АКТУАЛЬНО после замечания пользователя «тест залагал, не может скрафтить»:**
client1 штатно закрыт exactownedwindow PID28180/run8bdf18ba, savedgracefully;
exit1/NOTSCRIPT_PASSED. Rawreport `.tmp/voidjar-fabric-client1-raw-report.json`,
2PNG100/101 (101rootосмотрен). Stoppedworld `voidjar-smoke-c2eb4d19-90c1-4e50-adef-8c0e13e9e6a5`:
actualvoidjar1+ordinaryjar1 ужеизготовлены/вInventory, table1714,216,377 содержит
INK_SAC1/SLIME1/PAPER4; realgoldpayer1760AQ/950PD (540AQ/1350PDpaid), Frostpayerexact.
Следовательно завис **третьего vanilla labelcraft**, не voidrecipe или renderer.

OriginalConfigRecipes416 dyeBlack включаетoldink_sac; productionjar_label ошибочно
использовал только modern c:dyes/black. Rootrecipe теперь ORexistingthaumcraft:
black_dyes(ink_sac+black_dye) И c:dyes/black (moddedtagcoverage retained). Добавлены
nativeink+modernblack same-recipe regression и actualarcane-table ordinaryresult
click4labels/zeroVis. Clientтеперь readonlypreflight всех3realrecipes изкопийactual
inputs ПЕРЕД clicks, previewFAILafter40ticks, ordinaryphasebudget400 вместо6000;
реальныеdistillationwait остаются6000 иpipe1200. Stepchanges логируются. No outputs/
vis/XP seeded. Snapshot1 stale послеactualrecipefix; нуженfreshvalidate2, новый
client2изacceptedFrostcopy, НЕfailedcopy. Сейчас ниGradle/game не идёт; allownersdone.

**Самое свежее:** `voidjar-fabric-server3` exit0/**76/76actualFabric**,9.031s,
failures/errors/skips0/all76required. Bitemcase.035s, Avoidcase.556s; обычный
levelpost действительно выдал ONEsameUUIDjar32 после**24nativeTicks**,0pending.
Productionqueue/clock не меняли. Freshsnapshot `.tmp/voidjar-fabric-validate1-inputs.json`.
**Уже идёт** focused `voidjar-fabric-client1 -Scope voidjar` (bounded600s),7expected
exactPNG100–106; freshPython `.tmp/voidjar-assets1.log` отдельноread-only. Не менять
sources/не параллельныйGradle/game. ClientPASS/PNG/final201JUnit/build/JAR пока НЕТ.
Послеуспеха rootinspectall7→fullfreshvalidate1→record_void_jar_client/verification.
Independentnexttubecontract readonlyagent `ses_eba264008ffeag2Sz9vZmZWVi9` не владеет
исходниками и не блокирует эту frozenbatch; не ждать/неpollего для текущегоclient.

Pythonfreshassets1 завершён **51PASS/89.498s**. Read-onlynextagent тоже завершён,
findings сохранены в [M06a draft](analysis/2026-10-16-valves-resonator.md): valve+
resonator+realshapelessarcane, НЕbuffer; optionalTUBEFILTER отдельнымgate. ЭтоНЕ
sourceactive/неrootпроверенныйoriginalaudit/неimplementation, никакихowners или
прогоновследующегопакета. Текущийvoidclient1 всёещё идёт, источникине менять.

**Последнее:** `voidjar-fabric-compile1` exit0/28s: productioncommon/Fabricclient/
commonGameTest/Fabricwrappers реально compiled. Сейчас **уже идёт** только
`voidjar-fabric-server1 -GameTestOnly` (bounded180s), sourcefrozen; НЕдублировать
Gradle/game. Ни201JUnit/76runtimePASS, ни PNG100–106 ещё нет. Pythonpreflight1
также был запущен, его completion/result пока не получен. НовыйJAR ещё не принят.

**Новее:** server1реально75/76 за8.729s. Bitem/API/codecs/3columnscraft/namedroundtrip
PASS; Aoverflow/network/pressure/phials/helddrains/realcancelledADD дошёл до LAST
22tickretry assertion, тамFAIL. Rootдобавил ТОЛЬКОdiagnosticelapsedNativeTicks/
pending/local+wide markedentities вfixture, не productionqueue/clock. Сейчас
**voidjar-fabric-server2 -GameTestOnly**, sourcefrozen/неparallelGradle/game.
Pythonpreflight1 завершён: **51tests PASS/90.951s**, extractorPASS. Client ещё не
запускался. Не считать новый76PASS и не повторять запущенныйserver2.

Server2диагностика75/76: elapsedNativeTicks22,pending1,local/widemarked=[] — output
всё ещё сохранён, не доказаналосс. Sourcequeue имеет globalbatchretryAfter И
perentryminimum20; fixed22deadline отсутствуетвконтракте. Ужепринятыйtabletest
использует actualevent/nativelevelpost+70boundeddeadline. Rootисправил ТОЛЬКОновый
fixture аналогично: onEachTick наблюдает actual1jar32/sameUUID/0pending/min≥20,
отдельныйdeadline70; убрал ручнойtickвfinalcallback. Нет queueclocksetter/flush/
productionchange. Сейчас fastserver3, frozen. Это не сниженияcount/cost/retention
asserts и не runtimePASSдоresult. ПослеPASS freshsnapshot→focusedclient1.

[Void jar contract](analysis/2026-10-16-void-jars.md) после read-only original/
OpenTC4 audit. Владельцы и их актуальный статус, **никаких Gradle/game до ALLFROZEN**:
- A `ses_eba4b46a4ffePwTIM1Ip7uimWP` **SOURCEFROZEN/root reviewed**: storage/
  atomicoverflow/block+phial+heldalembic interactions и NEW VoidJarRuntimeChecks.
- B `ses_eba3ee6dbffe3DKuE7jzbkn4MO` **SOURCEFROZEN/root reviewed**: NEW ItemJar/
  filleditemrenderer, sharedBERfactor и NEW JarItemRuntimeChecks.
- C `ses_eba3ee6d5ffe0hivmAMci0DqL1` **SOURCEFROZEN/root reviewed**: ТОЛЬКО NEW
  VoidJarClientSequence.
- Root: все прочие existing/bootstrap/registry/models/recipe/book/scopes/wrappers/docs.
Не читать/дублировать активные ownedfiles. VOID_JAR=jar_void, общий JARBEtype;
ожидаются2новыхcompactservercase, но floor/count ещё не результат. Newscopevoidjar
толькоизfreshacceptedFrostcopy/bench1714,216,377; capturecount100+ подтвердитьCfreeze.
Сохранённый emptyjar+blazepowder и secondgoldgreatwood payer7 используются реально;
Frostpayer untouched, никаких новыхvis/XP/essentia/output grants. Fullvsphialoverflow
различаются какoriginal. Exported siblingfolder — binaries, исходники остаются здесь.
Frost verification ниже — исторически принятые3673inputs, не приёмка новых правок.

Root source-only интеграция: VOID_JAR/sharedBE/creative/light9, vertical5AQ/15PD
recipe +strictemptyjar check (no filled/labelled input erasure), book/icon/lang,
targetedjar-only generator/builtinitemmodels. Existingwandmodel hashunchanged,
3newresource tests PASS/.193s;318textures/210localmodels/0missing staticonly. Two
serverwrappers/requiredmanifest76 PROVISIONAL, не execution. Scopevoidjar/copy/
Bookstage/report/runner/.tmprecord_void_jar_client.py подготовлены;7captures100–106
PROVISIONAL доCfreeze. ItemrendererFabric hook ждёт exactBAPI. ДоALLFROZEN не
Gradle/game, activeownedfiles не читать. Следом rootreview/APIreconcile→compile.

B4files frozen/root read: ItemJar static helpers/agreed API and sixarg static
`thaumcraft.client.renderers.item.JarItemRenderer.render` wired FabricBuiltin registry
forboth +lazytooltipviewer. Itemdrawcontents/labelEAST→actualblockshell; nofakeBE/
worldwrites/recursion/commonFabricimports. Root removed ItemJar.super tooltip
delegation because it would repeat old BlockJarfilterline after newfulltooltip.
NativeBlockItemjavap подтвердил delegation; в Bcase добавлен exact2lines regression
через реальный appendHoverText, без сменыglobaltooltipviewer/knowledge.
Nativeitem/API/recipe/dropplace case authored, не запускался. A/Cещё ACTIVE, не
compile/game до ихfreeze. Renderer/production APIs совпали; visual не inspected.

A9files SOURCEFROZEN/root read: nativeboolvariant/codec/fullcollision/comparator,
cap64/fullvoidoneunitpull/48labelled32fullmin48; callback-freeoverflowpermission
и lockedpairedrawcommit. Shared `exchangeItem` pays detachedhand+endpoint before
notifications/realADD, protects nativecreative restoration/full-inventory discard,
usesexistingdurablequeue. ItemEssence item-first/secondary path shared; phials
refuse57/64, heldvoidalembicdrainsALL. BAPI exactly matches, oldBlockJarfiltertooltip
removed by A too. One new JUnit authored (expected201, NOT run);76server expected,
compactAcase finishes after22actualticks includingrealcancelledADD/retry. Still no
Gradle/game. **Only C remains SOURCE ACTIVE**; do not read/edit its new clientfile.
AfterCfreeze: review/reconcile capturecount+constructor/tick→firstFabriccompile.

**ALL SOURCEFROZEN/root reviewed.** C779lines API требует constructor(client,
capture,bench,center),tick(client); rootBook wiring приведено именно к этому.
7contiguous100–106 подтверждены; actualsavedjar/powder+slot7goldpayer→450AQ/1350PD
voidupgrade +90AQnormaljar +4nativeblanklabels. TWOreal32helddrains pertype,
nativecoalBLOCKresolverbudget/ordinaryfuel/realtypelabels без RNGchanges. Passive
ledger accounts rawpotential+furnace/alembic/tube/world/item/cursor/drop/pending,
каждый transfer supplied=stored+voided; fullphialrefusal/secondarysuccess/dropplace.
Expected8rawblocks104/type — fixture ожидание, не выполненный runtime. Primary
Frostpayer/old173/72candles/XP11 exactchecks authored. FirstFabriccompile1 следующий;
не дублироватьGradle/game/не менятьsources duringrun. Ни201/76, ниPNG ещё нет.

**Сейчас уже запущен** `voidjar-fabric-compile1 -CompileOnly` через новыйFabrichelper
(bounded180s). Отдельно read-only Pythonpreflight51checks, без game/Gradle. Actual
extractor --check ужеPASS48aspects/201research/294textpages; scoped gitdiffcheckPASS
(только existingCRLFwarning Fabricbuild). Источники frozen до completionnotice;
следом проверитьactualcompilelog, затем fastserver/fixturefix безparallelruns.

### Последний принятый checkpoint: базовый Frost (Fabric-only)

`frost-fabric-validate2` exit0: **200 JUnit / 74 Fabric GameTests**, все74 required,
failures/errors/skips0, **3673 unchanged inputs**,48Python, один production Fabric
JAR; notices/isolation PASS. [Verification](porting/verification.json) source-bound:
`matches_current_recorded_inputs=true`, `historical_evidence_only=false`.
Archive: `.tmp/frost-accepted-verification2.json` (Excavation archive тоже сохранён).
`frost-fabric-client2` exit0/**SCRIPT_PASSED**, **все5 точных PNG95–99 осмотрены**,
run `client-smoke-53423362-b758-43b7-9e5a-f3cdeba3c121`,1м1с/1208clientticks.
[Client evidence](porting/frost-client.json), [контракт/история](analysis/2026-10-16-frost.md).
Source owners/Gradle/game завершены; main Java274 — не процент готовности.

Accepted **STOPPED** save:
`fabric/run-clienttest/saves/frost-smoke-35724910-4a45-4c82-b59b-b0042f5a3734`.
Actual saved NORMALnode1623,218,374 передал37wholevis (10aer/3terra/6ignis/3aqua/
6ordo/9perditio), одна naturalignisregen, без topups/записи nodebase/current.
Native799aqua/ordo/perditio craft съел **тот самый earned diamond** из Excavation;
новое сырьё только4water/4quartz. Fswap вернул exactExcavation. Ровно2singlepaid
shots по3aqua/1ignis/1perditio: первый действительно отразился от actualwall и
завершился на4-мimpact, второй дал ordinaryTHROWN3/cow10→7. Invulnerable1savedplayer
сохранён; owner ricochet — НЕ survival damage evidence. Finalvis aer1951/aqua2436/
ignis2846/ordo933/perditio431/terra464, totalXP11 unchanged, 3wands/Fire+Shock+Exc/
wornrobes+goggles/36oldnon-nodeBEs/12jars173units/72candles/apple/knowledge retained.

95readablebook/exactrecipe/base10/10/10;96singlefour-focus selector/basecosttooltip;
97**facetedturquoise originalorb ясно виден**, но capture age2/impact0 — НЕ кадр
самогоотскока;98redcow/реальный orb видны,3damage подтверждены отдельным runtime,
fixturelabel не healthmeter;99readablefinitevis/blue6-facefocus. Tooltip частично
закрывает selectorfooter, bottombuttons всё ещё накладываются наhotbar. ImpactFX,
terminalburst и audio отдельно не осмотрены; fullFX/GUI/audio parity не заявлена.

Final JAR10,199,227bytes SHA256
`a40bec976500e0561aa4d6b912ab7ff40f0719e33036f753d5476e4d287b2b5e` содержит **один**
`thaumcraft.accesswidener`, metadataссылка и exactconstructorAW **intermediary**
проверены по Loom mappings и remapped native supercall в байткоде.
`.tmp/frost-fabric-validate2-jar-inspection.json`; обычная packaged установка этим
НЕ проверена. Dev загрузкаAW доказана реальными client/server, Neo access adaptation
по-прежнему deferred. Никаких NeoForge edits/builds/runs.

На момент принятия Frost готовился read-only M05 voidjar contract; теперь эта
следующая batch SOURCE ACTIVE (ownership приведён выше) и не наследует74PASS.
ПолныеTC4/M00–M15/API/Baubles/Repair/runic/advanced
foci/specialnodes/automation/golems/mobs/warp/EULAserver/remote/survival остаются
открыты. Ни commits, ни push/release не выполнялись.

По просьбе пользователя готовый мод локально скопирован в соседнюю папку
`../Thaumcraft-Fabric-1.21.1/`: accepted productionJAR + exactFabricAPI0.116.17/
Architectury13.0.11 в `mods/`, инструкция и `SHA256SUMS.txt`. Все три ZIP проверены,
modID/version проверены, ThaumcraftSHA совпадает с acceptedFrost. Исходный проект
не перемещён/не изменён, миры/оригинал/devJAR не экспортировались. Это НЕ новая
packagedruntime приёмка и не публичная публикация. Пользователь также спросил,
как ускорить: связанные contentпакеты, непересекающиеся sourceowners, reuse общего
focusedharness и один общий fullgate после freeze; не отключать расходы/persistence.

### История реализации Frost (ниже не активные процессы)

**Frost batch ALL SOURCEFROZEN/root reviewed, ещё не compiled/run/accepted.**

Сейчас focused **frost-fabric-client2**, freshsnapshotvalidate2. Client1exit1 на
SHOT_ACK0 после95/96/97: genuinefinitecharge/craft/Fswap/ONEpaid3/1/1spawn прошли;
97rootосмотрел — originalfacetedturquoiseorb ясно видим. Actualclienttrace показал
wallbounce Z−1.46→+.695/impact1, затем3moreimpacts/terminal4. Fixture ошибочно
требовал postposition≤2blocksотwallcenter, хотя originalPOSTbounce move отOLDpos
уже уносит его дальше2. Rootclient-onlyfix: incomingpreviousPosition+velocity
segment пересекает реальнуюsouthface/actualstonebrickwall, Zsignflip/counter
observed, не arbitraryradius и не artificialimpact. Nativephysics74PASS не менялись.
Новыйrunagain из acceptedExcavationcopy, не failedFrostcopy. Послеуспеха5exactPNG/
fullfreshvalidate2/JARremappedAWcheck/recorder. Историческийsnapshot1 stale.

**Latest:** fast `frost-fabric-server4` exit0, **74/74actualFabric**,0failures/errors/
skips, FrostShard .081s/FrostFocus1.944s. Strictnativeicepacketcost/refusedADD test
прошёл после owntransportflushfixturefix; productionзвук/цены не менялись.
Freshsnapshot `.tmp/frost-fabric-validate1-inputs.json`; идёт **только** focused
`frost-fabric-client1 -Scope frost` (bounded600sec shell),5expectedPNG95–99. Не
менятьsource/не параллельныйGradle/game. Послеуспеха все5PNG/rootinspect +fresh
fullvalidate1/JARsingle-remappedAWcheck/recorder. Ни нового clientPASS, ни finalJAR
ещё нет; старыйExcavationarchive остаётся последним принятым checkpoint.

Preclient review исправил только строгийfixtureoldBlocks: реальный savedcactus
1623,216,382 в том же random-tickingchunk, что savednode, закономерно меняет AGE/
растётдо3. Read-onlynativejavap CactusBlock.randomTick подтвердил; C теперь
позволяет ТОЛЬКО эту boundednativecolumn эволюцию, сохраняя base/старыеcactusblocks
и всёостальноеexact; пишет before/after. Не выключались randomticks/не RNGcontrol/
не worldrestore. Это предотвращает ложный nativeworldmutation failure, не меняет
Frost и не разрешает разрушать/подменять oldblocks. Allsources опять frozen.

Актуально: **production/client/gameTest Java compiled**, server2 реально **73/74**
за6.639s. ConstructorAW теперь найден в собственных Fabricresource roots, startup
прошёл; весь FrostShard physics/combat/save case PASS. FrostFocuscase остановился
на nativeice sound count (0.542s; точная phase пока неизвестна). Root добавил
ТОЛЬКО diagnosticphase/count/outboundpacketclass+soundposition в fixture, не
менял звук/расходы/cooldown. Сейчас server3GameTestOnly (bounded180sec shell),
sourcefrozen/не параллельныйGradle/game. Frostclient95–99 ещё не запускался;
production remappedAW/JAR и обычная packaged установка ещё не проверены.

Server3 опять73/74, diagnostic: phase2/paid/noncancelled, outbound=[]/soundcount0.
Nativejavap показал причину fixture: ServerCommonPacketListenerImpl.suspendFlushing
внутри server tick; initialactor ещё не suspended, delayedphases уже suspended.
Звук был realqueued, тест смотрел до wireflush. Root test-only `.connection.flushChannel()`
перед чтением СОБСТВЕННОГО embeddedoutbox (не playSound/fakepacket/resume всего
сервера/clockchange). Strictcount1/paidADDrefusal сохранился. Сейчас fastserver4;
production не менялся. ЕслиPASS — freshsnapshot/client1/mainbuild, не replayExc.
- A`ses_eba8a2343ffe0r5c6Q1RFHiHKZ` SOURCEFROZEN/root reviewed: только NEW EntityFrostShard/ThaumcraftFrost/
  RenderFrostShard/ThaumcraftFrostClient (+маленькийmeshhelper) /oneprojectileruntime.
- B`ses_eba89b71affemRVP26nf7zzwKC` SOURCEFROZEN/root reviewed: existingFoci/FocusData +NEWItemFocusFrost/
  oneFrostFocusRuntimeChecks. Не трогает dispatcher/прочиеexisting.
- C`ses_eba890f6cffe41onAFfGVUKix9` SOURCEFROZEN/root reviewed: только NEW FrostClientSequence.
- Root: otherexisting/bootstrap/resources/models/book/scopes/wrappers/runner/docs.
**Не читать/дублировать active-ownedfiles; не запускать Gradle/game до ALLFROZEN.**
ExcavationPASS ниже — 3653предыдущихinputs, не приёмкаFrost. Snapshotarchiveсохранён.

Root интеграция добавлена(НЕ compiled/run): recipe4water/4quartz/**actualsaveddiamond**,
10aqua/ordo/perditio,FOCUSFROSTsecondary; models/lang/book/icon/2wrappersfloor74.
HeldpropertyNone0/Fire.25/Exc.5/Frost.75/Shock1+9newmodels;45float32combinations
source/JSONagree,314textureRefs/210models/0missing resourcePASS only. Existingoriginal
orb.obj canonicalLFhash30f6b5b337c31d5716d6d6461519ca90486bebc6b21eb296d05cedee61d3066d
и3PNG подтверждены retainedassets (OBJnewline-only, PNGbyte-identical), новыеaliases
не нужны. ProvisionalThaumcraftFrost.register/client.init подключены, сверитьAfreeze.
Scopefrost копирует actualacceptedExcavation save вfreshownedUUID, bench1682,216,377;
PROVISIONAL5captures95–99, подтвердитьCfreeze. No sharedsource edits duringlater runs.

Существенно: mainpayerPD332<799recipe. Новыйсценарий должен РЕАЛЬНОподзарядить
существующийsavedNORMALnode1623,218,374 доPD>=1200 (не newnode/freevis), удерживать
camera вPRE+POST/actualside-ownednodesession, finitebudget+regen observed. Recipe
съедает настоящую1diamond, добытуювExcavationclient2 (НЕновыйdiamondgrant);rawnew
только4water/4quartz. FourfociFselect вернётexactoldExc; oldFire/Shock/robe/oldBE
сохранить. BasicFrost3/1/1centivis,200ms,damage3/THROWN/четвёртыйimpactterminal,
безfreeze/slowness/upgrades. [Контракт](analysis/2026-10-16-frost.md).

B4files завершены/rootreview: `.activationCost(wand,player)`,200ms/BF/5204428,
atomic3-primal debit→ONErealspawn→ice sound даже приADDrefusal, unconditionalswing
какoriginal. CompactFrostFocusRuntimeChecks ownsuniquerealplayer,9boundedphases/
30000acceleratedticks+10swall;actualcraft/fourcodecs/selection/ADDrefusal/reentrancy/
globalcooldown/unfunded/creativepayment authored, NOT executed. A/Cещёactive;
не Gradle/game до ихfreeze. .tmp/record_frost_client.py подготовлен для exact95–99,
но ничего не записывал/не принимал. Assetaspectresolver использует actualarcane
recipes; новых выдуманных fixedaspectbindings дляFrost не добавляли.

A5files завершены/rootread: oldnative`zk.e()` speed1.5F confirmed,6Gaussianlaunch,
explicitpostbounce move/.99air/.8water/.05gravity/fourthterminal/ownergrace4ticks,
currentnative thrown/protection/collision. NativeNBTowner +persistedage/impacts и
finiteguards declaredsafetyadaptations. Entitydata syncdamage/size/counter/age,
nativeentityevents61/62 produceactualfrostfragments, неcustompayload. Original
orb42vertices/80triangles boundedOBJ→nativequads/degeneratetriangle/no placeholder;
originalblocks/frostshard.png &atlasfragment alreadyresolve. .register/.init APIs
совпали, новыхaliases/converters/mixins/packets не нужно. OneFrostShardcase authored
не executed. C507lines теперь rootread: actualsaveddiamond/11totalXP подтверждены
acceptedExcavationreport; readonly SERVER_NODE_USES(pos,hand,stack) fields совпали.
**ALL FROZEN**, count5exact95–99 confirmed/runneralreadywired. MainJava274/diffcheck
PASS. Новыйclient из privatecopy и оба compactruntimecases ещё не запускались.
Root запускает firstFabriccompile; никакихsourcechanges/parallelGradle/game.

Compile1exit1: native1.21.1 `Projectile(EntityType,Level)` package-private, поэтому
прямой subclass не компилировался. Root добавил **один** constructor-only access
widener `thaumcraft.accesswidener`, wired common+Fabric Loom и fabric.mod.json.
Сохраняем nativeProjectile base/owner tick и originalPOSTbounceflight; переход на
ThrowableProjectile вернул бы нежелательное cachedPREbounce движение. Никаких
других nativeполей/методов не открывали. Сейчас **frost-fabric-compile2**; не
повторять этот уже запущенный Gradle, не менятьsource/не параллельныйgame.
Runtime/JAR remappedAW и обычная packaged установка ещё не проверены. NeoForge
conversion отложен вместе со всей Neo parity, никакихNeoправок/прогонов не было.

Compile2: production/common +Fabricclient Java **compiled** с exactconstructorAW;
остановился толькоGameTest Java на testhelper `close(Vec3,Vec3)`, скрытом nested
AutoCloseable.Check.close(). Root переименовал assertion вsameVector, tolerance
не менял. Сейчас `frost-fabric-server1 -GameTestOnly`; sourcefrozen. Ни runtime74,
ни реальные FrostPNG95–99 пока не получены. Не подменять runtime компиляцией.

Server1 **не дошёл до Minecraft server/tests**: FabricLoader `Missing accessWidener
file thaumcraft.accesswidener from mod thaumcraft`. Dev commonresource есть, но
его нет в собственных resource roots Fabricmod. Ошибка startup оставила живым
Architectury watcher, поэтому старый shell не завершался/нового XML нет. По
вопросу пользователя выполнена диагностика; остановлен ТОЛЬКО точный ownedJava
PID7712/parent40124 с проверкой root+GameTest+TransformerRuntimecommandline.
Не объявлять этот прогон74FAIL/PASS или использовать старый72XML. Нужно добавить
canonicalcommon AW в FabricprocessResources (single source, без dup committedAW),
узкую same-AW duplicate handling дляshadowJar и проверить freshserver2/finalJARAW.

Fix выполнен: FabricprocessResources.from(commonAW), единственный authoredAW
остаётсявcommon. Shadow duplicateStrategy.EXCLUDE только для exactsameAW path,
не blanketresourcecollision suppression. Server2 уже доказалdev AWload/nativector;
73/74 — не finalsource-boundacceptance и не доказательствоproductionremap.

### Предыдущий принятый checkpoint: базовый Excavation

`excavation-fabric-validate3` exit0: **200 JUnit / 72 Fabric GameTests**, все72
required, failures/errors/skips0, **3653 unchanged inputs**,48Python,1FabricJAR
notices/isolation PASS. Recorder source-bound match=true/historical=false.
`excavation-fabric-client2` exit0/SCRIPT_PASSED, **6 точных PNG89–94 осмотрены**,
run `client-smoke-0856bee0-f459-4c92-99e8-6b0e60d36526`,1м1с/696clientticks.
Evidence: `porting/excavation-client.json`, `porting/verification.json`; archive
`.tmp/excavation-accepted-verification3-before-frost.json`. MainJava269 — не процент
готовности. Все sourceowners/Gradle/game завершены. NeoForge не трогали.
Accepted **STOPPED** save:
`fabric/run-clienttest/saves/excavation-smoke-dc8878ce-5af1-4825-b4d8-c2869791f6e5`.

Новый настоящий путь:16rawplanks→2ordinarychests (без payingwand),4earthshards/
4quartz/emeraldTAG→Excavation за1599terra/399ordo/399perditio на savedpayer. Real
Fswap вернул exactoldShock, oldFire сохранён. На6.10blocks: slowwoodpartial.6500001
без платежа→release/pause/resume→chest+3actualcontents; ore→1diamond+**4nativeXP**;
вторая earnedchest→ONErealFabricBEFORE refusal, block/BE/content exact/no outputs.
Все3completedattempts платные11terra каждый,33total; interimticks бесплатны,
послеcancel progress reset. Finalvis aer951/aqua2941/ignis2248/ordo1132/perditio332/
terra164. Новыхcharge/topups/XPgrants нет. 114oldTCblocks/34BE/12jars173units/
72candles/3wands/oldFire+Shock/wornrobes+goggles/apple/knowledge preserved.

Actual native72 включает stone8/dirt4 timing, price15/11, depletion/miss/negative
hardness/release/coordinate inheritance, codecs/node priority; реальные BREAK/
BEFORE/CANCELED/AFTER counts, cancellation, staleBE/AFTERreplacement, nestedordinary
samepos/exception isolation, survival+creative containers/unopenedshulker/oreXP/
ice/door+bed/occupiedhive refusal/infestation policy. Finalray использует current
body yaw/pitch как oracleBlockUtils237–253, не отстающий nativeHEADyaw.

Осмотр:89readablebook/exactrecipe/base20/5/5;90single selector/currentShock/oldFire+
Excavation (две loose choices, третий focus equipped).91реальный continuousgreen
beam/impact виден; мелкиеearlycracks неразличимы, nativecrackstage1 наблюдён.
92видны chest/diamond/cobble3, но остался jar tooltip, не clean diamondtooltip.
93protectedchest действительно на месте/highstagecracks видны (stalecracks —
сохранённый originalreleasequirk/nativeexpiry).94readablefinitevis и actual darkgreen
sixface heldcube/ARGBff064006/Silverwood/Thaumium. Footer надHUD, bottomremovebuttons
ещё пересекаютhotbar. Beammesh/audio/GUI — не полная legacy parity.

Адаптации: realplayer.gameMode protected nativeharvest, private enchantment-empty
loottool без inventoryswap, nativeXP/onRemove/BE. Skiporiginally-absentplayercallbacks
предотвращает creative-shulker duplication, icewater, stats/exhaustion/wear. Occupied
hive/nest REFUSE, exactscoped silverfishspawn suppressed/ordinary retained; native
waterloggedfluid/rawore loot retained. Stateless same-state nonBE ABA без mutation
witness не определяется. Dimension/death/logout/identity/serverstop cleanup —
safety adaptations; ordinaryrelease не reset. Raw/research/oreterrain/floor/camera/
workbench/protectionpolicy fixtures, НЕ fullsurvival. Baubles/Repair/runic/upgrades/
otherfoci/specialnodes/fullAPI/ordinaryEULA/remote/Neo/fullTC4 всё ещё pending.
Следующим на тот момент был базовый Frost; его новая приёмка приведена выше.

### История реализации Excavation (ниже не активные процессы)

**НОВАЯ Excavation batch ALL SOURCEFROZEN/root reviewed, не compiled/run/accepted.**

Client1 exit1 **только FINAL_ACK heldModel assertion**, после5PNG89–93. Native
story уже дала actual2chestcraft/paidfocus/Fselection/freepartialpause/resume/
container+3cobble/diamond+6XP/ONEpaidprotectedrefusal. Native finalterra164/3×11,
savedoutputs exact. Diagnostic91 rootseen: continuousgreenbeam действительно
виден;90single selector/currentShock/looseFire+Excavation, footer вышеnativeHUD.
Тест ошибочно искал flat `items/focus_excavation` на held3Dwand. Root исправил
ТОЛЬКО assert на actual6tintedmodels/wand faces/opaqueRGB409606/Silverwood/Thaumium;
production модели/tints не менялись. Freshsnapshotvalidate3, сейчас client2 из
originalacceptedclothing copy, не failedworld. Послеуспеха6exactPNG89–94/fullbuild3.

Latest `excavation-fabric-server3` exit0: **72/72 Fabric**, обеnewcases исполнены,
failures/errors/skips0. Adapter current-body-yaw fix +bounded test ray chunk inputs
прошли весь compact harvest matrix, включая реальные transformed events. Fresh
snapshot `.tmp/excavation-fabric-validate2-inputs.json`; сейчас идёт **только**
`excavation-fabric-client1 -Scope excavation`, ожидаются6exactPNG89–94. Следом при
успехе — осмотретьвсе6 и finalfullvalidate2/recorder. Не менятьsource и не запускать
параллельныеGradle/game. Текущийserver3 — fastdiagnostic, не freshfinalbuild/JAR verdict.

Актуально: production/common и Fabric client harness скомпилированы после root
API reconciliation; первые compile1/validate1 остановились на fixture-only Java
ошибках (ненужный checked catch после удаления reflection и2 AABB(BlockPos,BlockPos)
вместо Vec3). Исправлены только эти fixtures. Сейчас `excavation-fabric-server2`
GameTestOnly; ни новый72PASS, ни client89–94 пока не получены. Snapshotvalidate1
stale после fixture changes. Не менять source/не запускать параллельный Gradle/game.
- A`ses_ebb0b5675ffedUYNbiUUe09uXZ` SOURCEFROZEN: NEW ExcavationHarvest +scoped native mixins+
  compact ExcavationHarvestRuntimeChecks; НИКАКИХ existing/root файлов.
- B`ses_ebb0aafbeffezwBsNfqE5xRD87` SOURCEFROZEN: existingThaumcraftFoci/FocusData, NEW
  ItemFocusExcavation/state/S2CFX/clientbeam/compactfocusruntime.
- C`ses_ebb0841b1ffetlrxcXwTcuFQb9` SOURCEFROZEN: только NEW ExcavationClientSequence.
- Root: прочие existing/build/bootstrap/mixins.json/GUI/book/resources/runner/wrappers.
**Не читать/дублировать owned activefiles; никаких Gradle/game до ALL SOURCEFROZEN.**
ClothingPASS ниже — только3630oldinputs, не новаяbatch. Archive сохранён.

Root ужеadded (НЕпроверено): original251 earth/quartz/emeraldTAGrecipe+itemmodel/
lang/book/icon; c:gems/emerald; nativeoriginalmaterialtag97IDs(82MC+15TC), неpickaxe/
shoveltags. `.tmp/excavation_material_audit.py` read-onlyjavap171oldregistrations,
51fastoriginalIDs/no unresolved; hay_block=GRASSfast, glass/metal/clay/snow slow,
TCAlchemyFurnaceMaterial.iron НЕfast, TCcrystalsglassНЕfast. Все97blockstateresources
resolve;36float32wandpredicates/286textures201models0missing resourcePASS only.
MixinExtras common0.4.1 compileOnly+AP added/rootPOMchecked; FabricLoader supplies
runtime, no shading. A ещё должен вернуть exactmixin names/wiring; mixins.json НЕ
подключён заранее наугад. B cleanupAPI/renderhookпослеfreezeсверить.

RootheldpropertyNone0/Fire1/3/Exc2/3/Shock1+9models; GUIstableAF/BE/BLsort сохраняет
duplicates и contextValidпроверяеттотжесортированныйsnapshot. Defaultviewport
selectorfeedback сдвинут выше nativeHUD (unseen). Provisional .initServer/.init/
FabricLAST APIs и2wrappers/floor72 wired, ждутactualA/Bfreeze. Scopeexcavation
копируетacceptedclothing-world вfreshUUID, bench1650,216,377/center1474,216,377;
**capturecount6 PROVISIONAL, сверитьCreturn89…final**. Новыйclient НЕreplayscharge/
robes/старыйprogression: savedpayer1796terra достаточно1599craft+197остатка под
11-centivis attempts. Research/raw4earth4quartzemerald/16oakplanks3cobble/newore/
camera/floor/protectionpolicy declaredfixtures; outputs не подставляются.

Apolicy: realplayer.gameMode.destroyBlock +exact invocation scoped privilege,
skiporiginally-absent player callbacks/nativeBlock.dropResources+BE+XPonce,
realBOTHprotectionhooks, successfulremovalwitness, nestedordinary/exception-safe.
OccupiedhiveREFUSE(no beeslost), infested scoped silverfish suppression/nativeXP
kept, nativewaterlogging allowed, iceair. Этоавторскийконтракт, ещё не executionPASS.
Детали [контракта](analysis/2026-10-16-excavation.md).

Post-freeze integration завершена: exact2mixins registered; MixinExtras **fabric**
0.4.1 compileOnly+AP (не common artifact), Loader runtime не shaded. B actualAPI
`progress(ServerPlayer)`→nullable ProgressView(pos,amount,dimension) reconciled с
C без speculative reflection. Root wires clearPlayer на quit/dimension/clone/dead
POST +clearAll/serverstop; ordinary release НЕ очищает state. C count6 **89–94**
подтверждён, single selectorframe. MainJava269, diff-check PASS. Следом first
Fabric-only compile; не менять source во время Gradle/game. Ни тестов, ни visuals
этой batch ещё не получено. Serverwrapperfloor72 — требование, не результат.

### Предыдущий принятый checkpoint: три мантии и базовый Shock

`clothing-fabric-validate8` exit0: **200 JUnit / 70 Fabric GameTests**, все70required,
failures/errors/skips0, **3630 unchanged inputs**,48Python,1FabricJAR notices/isolation
PASS. Source-bound recorder: matches_current_recorded_inputs=true/historical=false.
`clothing-fabric-client7` exit0/SCRIPT_PASSED, **8 exact PNG81–88 root осмотрены**,
run `client-smoke-da7fb896-e98b-45a3-8d9d-e578ba12ef0c`,2м16с/2117clientticks.
Evidence `porting/clothing-client.json` / `porting/verification.json`; archive
`.tmp/clothing-accepted-verification8-before-excavation.json`. All owners/processes
завершены, mainJava262. Старую research/infusion/caps/goggles цепочку не повторяли.
Accepted STOPPED save:
`fabric/run-clienttest/saves/clothing-smoke-792ae806-a552-4027-a65e-fb17312d794e`.

Actual: finite80/primal NORMALnode→206realvisunits/1040ticks (одна naturalAIRregen),
zeroFireexpense,2nativeuseevents client/server с тем же ray/id. 80string+20wool→
20paidfabric→19fabric/3robes; cactuswear1 на каждой при temporarily-disabled saved
EntityInvulnerable, nativehealth20→19.22, oldgoggles безопасноstored/unchanged.
Actualred dye6A3880→8C3960, nativecolored AND defaultwash3→2→1, twentiethfabric+
onelevel→repairchest0/repairCost1. **1 missing XP point — declared INPUT**, не XP
acquisition. All3earnedrobes+oldsavedgoggles реальнонадеты: armor7/10discountpoints.
Shock изготовлен из4airshards/4quartz/POTATO; realFselection вернулexactoldFire.
4paidpulses×19AIR=76: cow14.03blocks10→6;second pulse paid/iframeskepthealth6;
sky miss paid, wall block paid/no cowdamage. Fire7 — verifiedPRICE, НЕnewpaidFire
clientstory. Wandsfinal actualpayer: aer951/aqua2941/ignis2248/ordo1531/perditio731/
terra1796, focusSHOCK, craftCost54. Дваoldgoldwands/4phials/12jars173units/72candles/
112oldTCblocks/oldstacks preserved. RAW/research/daylight/floor/camera/machines/node/
cactus/wall/cow declared fixtures; не fullsurvival.

Осмотр:85purplewearer/base+untintedgold-grayoverlay исправленrender-onlyDYEABLE
gate;86singleShocktooltip/percast.25/currentFire. **87blue ribbon не различим**:
видны redcow/blueheldfocus, actualpaidS2Cgeometry — runtime, не seenribbonclaim.
88реальная brightblue/violet lightning/impact на wall видна. 84jar tooltip частично
закрывает XPcost;86footer пересекаетсясordinaryHUD. Этиvisual gaps сохраненывreport,
не fullFX/GUI/audio parity. Baubles/Repair/runic/upgrades/otherfoci/specialnodes/
ordinaryEULA/remote/Neo/fullTC4 pending. Следом [Excavation contract](analysis/2026-10-16-excavation.md),
ещё НЕ implemented. Его recipe может оплатить реальный savedpayer БЕЗновой зарядки:
20terra1599/5ordo399/5perditio399, останется197terra дляbasic11centivis attempts.

Production fixes: logical-side isolated NODE_USES (nativeEntity.equalsnumericID)
и render-only robe tint; no data schema/price changes. Focused release/charge и
final70server прошли, не выдавать их за replay старой acquisition. Camera POST
уже был, PREфиксация убрала дальнейшие штатные retarget/restart; client6 оставался
failure, side split не объявлен единственным доказанным объяснением всех сбоев.

### История разработки и отказов мантий/Shock (не актуальные active-процессы)

**Самое свежее:** идёт `clothing-fabric-client7 -Scope clothing`, fresh snapshot8.
Не менять source/не запускать параллельный Gradle/game. Ни один clothing client
пока не SCRIPT_PASSED, новая batch НЕ принята. All sourceagents завершены.
Latestserver70 PASS — server7 ДО следующих двух production fixes, не текущий PASS.
1. Gray wearer: client.RobeArmorLayerMixin (render-only DYEABLE gate), compile2 PASS,
   но новый worn screenshot пока не получен. Vanilla recipes/cauldron tag не включены.
2. Native `Entity.equals` сравнивает IDs: общий NODE_USES стирал session другого
   logicalside. ItemWandCasting теперь separate SERVER/CLIENT weakmaps +readonly
   isUsingNode +clearNodeUses на SERVER_STOPPED. Original ItemWandCasting521–572
   хранил object-in-use в отдельных item copies; cross-side shared state не original.
   Client6 реально подтвердил nativeequal distinct actors/id36, но всё ещё поймал
   paidFire−8 ВО ВРЕМЯ CHARGE_USE. Значит side split — необходимый sourcefix, но
   не считать его достаточным/единственной доказанной причиной всей нестабильности.
   Client7 дополнительно удерживает declaredcamera в CLIENT_PRE (раньше был только
   POSTlookAt) и пассивно пишет до64 настоящих RIGHT_CLICK_ITEM events с side/hand/
   ray/look/use/session в report. Callback ВСЕГДАpass, не перезапускает/не отменяет
   gameplay. Strictwholevis/zeroFireassert НЕ ослаблен. Нужно прочитать freshtrace.
После успеха — все8exactPNG81–88 (особенно purplewearer/single selector/actualbolt),
record_clothing_client.py и fullfreshvalidate8/server70/JAR/source-boundrecorder.
validate6/7 snapshots stale после последующих fixes; не relabel. MainJava262.

**Новая batch реализована; приёмка ещё не завершена.**

Актуальное: `clothing-fabric-server7 -GameTestOnly` exit0, **70/70 Fabric** после
исключительно fixture исправлений ниже. Fresh snapshot5 создан; сейчас идёт
`clothing-fabric-client4 -Scope clothing`, ожидаются8PNG81–88. После него нужны
осмотр всех8 точных новых PNG, `.tmp/record_clothing_client.py` и full/fresh
`clothing-fabric-validate5` (200JUnit/build/JAR/70server), затем source-boundrecorder.
Не менять source/параллельно не запускать Gradle/game. Основной код после initial
compile1 не менялся, текущие последующие fixes — client/server harness only.
Read-only Excavation helper review ещё идёт в `ses_ebb45dc7effeg1yjaRPRRY9T9E`,
без владения исходниками/сборок. Следующаяunit пока НЕ implemented.

Обновление: все три sourceowner завершились; `clothing-fabric-compile1` exit0
(common Java, Fabric clientTest/gameTest). Resource references и `git diff --check`
пройдены; inventory — 261 main Java files. Создан свежий snapshot
`.tmp/clothing-fabric-validate1-inputs.json`, идёт **только** `clothing-fabric-client1`
со scope `clothing`, 8 ожидаемых PNG81–88. Не менять исходники и не запускать
параллельный Gradle/game. Final server/build70 ещё не запускался; batch НЕ принята.
Read-only следующий Excavation contract `ses_ebb45dc7effeg1yjaRPRRY9T9E` не владеет
файлами и ничего не собирает; не блокирует текущую замороженную batch.
- A`ses_ebb62b200ffeboEsKeeoEoX0F8` **SOURCEFROZEN/root reviewed**: existingThaumcraftWearables + новые
  ItemRobeArmor/RecipesRobeArmorDyes (+маленькийdatahelperеслинужен), RobeRuntimeChecks.
- B`ses_ebb621410ffeAg4CKYoz4Bds2N` **SOURCEFROZEN/root reviewed**: existingThaumcraftFoci/FocusData/WandFocusActions,
  новыйItemFocusShock/S2CFX/clientrenderer/компактныйShockFocusRuntimeChecks.
- C`ses_ebb606f85ffegGwzG5Uq2hMQer` **SOURCEFROZEN/root reviewed**: только новыйRobeShockClientSequence.
- Root: existingbootstrap/clienthooks/book/icons/resources/lang/aliases/Bookscope.
**Не читать/дублировать активные ownedfiles, не Gradle/game до ALLSOURCEFROZEN.**
OriginalTC4 остаётсяspec; readableOpenTC4 ItemRobeArmor/ItemFocusShock дополнительно
прочитаныи используютсядляимен/сравнения, не копируютсякакdonorimplementation.

Rootужеadded(непроверено):3arcaneRobe+dynamicDye+Shockrecipes,4itemmodels/lang/
native3armortags/bookaliases/icons, robeitemtint. Original4armorbytesaliases
`robes_layer_1/_2[_overlay].png` созданы/shaв[контракте](analysis/2026-10-16-robes-and-early-foci.md).
FOCUSpropertyNone0/Fire.5/Shock1+9newheldmodels/JSON;27source/JSONcombinationsиresource
referencesPASS(258textures/192models), НЕgame-renderPASS. ProvisionalShockClient/
ShockFxNetworking bootstrap/FabricLAST иwrapperfloor70 wired, сверитьBfrozenAPIs.
Scopeclothing/StageROBE_SHOCK readsactualacceptedgoggles-client.saved_world,
newprivateUUIDcopy +32Xbench1618,216,377/center1474,216,377. SourceGGsaveнеоткрывается.
**Capturecount8 установлен**: contiguous81–88, fullstoryпо-прежнему66. Никакихnewruntimes пока.
GogglesPASSнижеисторическийдля3598oldinputs, неприёмкаэтихновыхchanges.

A4filesrootread: separatefabricrepair/SPECIAL-equivalentholder,400/375/325,
3/2/1,2/2/1points, strict16legacydyes/defaultpurple/nativeDYED_COLORprototype,
nativeuseOnwash/default+creativewaterdebit иcompactnativefixture authored. **No
runtimePASSyet**. Rootсогласовалmaterialsuffix`_overlay`счетырьмяранееcreatedbyte-
aliases: nativeLayerappendliteral, bare`overlay`wouldseeklayer_1overlay.pnginstead
oflayer_1_overlay.png. Helperpathassertupdated, visualsещёнеосмотрены. Не добавлять
`minecraft:dyeable`(wouldenablewrongmodernpalette). ЖдатьB/Cfreeze, ниGradle/game.

B8filesrootread/APIстыкуетсясrootbootstrap/FabricLAST. Sharedactivationclock250ms,
отдельныеweakmapsclient/server(нативныйEntity.equalsсравниваетnumericID), rootwired
clearCooldownsSERVER_STOPPED. Shock25centivis/AIR/direct4playerdamagepaidbeforehit,
20entityray/1.1broadphase/min.8border/percandidateeyeLOS,miss/block/PvP/refusedhurt
тожеpaid. Nativearmor/iframes/teamsнеобходятся. FocusDataтеперьтолько2realclasses,
oldFire/nodepriority сохраненыsource. S2ConlypaidFX/seededboundednativebolt(32live,
128segments+8forks/5glints) — visualapproximation, неoldfullFX. Экспорты
WandFocusActions.cooldownMillis,ItemFocusShock.pulseCost,ShockClient.activeBoltCount
readonly. Compactruntime600ticks authored inclrealclockwait/isolatedPvPtarget;
нетsleep/testclock/глобальныхiframeсбросов. **Не запускать доCfreeze**.

**ALL SOURCES FROZEN**. C874linesrootread, actualgoggles+3wands/173units retained,
RAW80string/20wool/dye/4airshards/4quartz/potato declared. Finite80/primalnode→
20realpaidfabric→3robes→actualcactuswear(oldsavedgogglesstored)→ordinarydye→
nativecoloredANDdefaultwash3→2→1→anvil20thfabric+1XP→actual3robes+oldgoggles→
realShockcraft/Fselect→2paidrangedpulses+1miss+1wall. Capture8/81–88, nofullstory.
NativeFXreflectionreadonlyBOLTS/origin/end/age/travel/glints matchesBfrozenclass.
FirstcompileFabric-onlythenfreshsnapshot/focusedclothingclient8/finalserverfloor70;
no sourcechanges duringGradle/game. Newbatchещёнеcompiled/run/visuallyaccepted.

`clothing-fabric-client1` exit1 на WEAR после PNG81; native20fabric/3robe crafts
прошли, но статичный teleport+1.25X beside cactus не вызвал contact damage/wear.
Read-only stopped save: cactus жив/на sand, player реальный Survival/health20,
все3robes damage0; gameplay defect не обнаружен. Raw archived `.tmp/clothing-fabric-client1-raw-report.json`.
Root исправил только client harness: реальный forward-key walk к cactus,
проверка≤100ticks и cleanup keyUp. Не вызывать entityInside/hurt/setDamage.
Нужен fresh snapshot2 и новый focusedclient2 из accepted goggles save, НЕ failedcopy.

Client2exit1/WEAR после81: движение реальное, но унаследован **Entity NBT Invulnerable=1**,
не abilities.invulnerable (тот0). Это actual root cause; предыдущий статичный-walk
диагноз был неполным. Stopped-save health20/armor0 подтвердили. Root client-onlyfix:
объявленное временное setInvulnerable(false) для cactuscontact, затем restore прежнего
флага (+cleanup на ошибку/stop). Нет hurt/wear/output setters. Native precheck
isInvulnerableTo(cactus) должен быть false до контакта. Нужен freshsnapshot3/client3;
все failedcopies сохранены, source acceptedgoggles не трогать. Server ещё не запускался.
Read-only Excavation contract готов: `analysis/2026-10-16-excavation.md`, не implementation.

Client3exit1 на WEAR_STOPPED: настоящий cactus уже износил все3robes на1;
поздний health<before assertion гонялся с ordinary saturation regen (saved health20,
saturation5→4). Это НЕ отказ robe wear. Client-onlyfix: существующий passive POST
observer сохраняет minimum native health и last source CACTUS, без damage/health
setters; проверка wear больше не ждёт10ticks после уже полученного wear. Actual
entity-invulnerability scope+cleanup сохранены. Budgetcharge теперь min3200/primal
(верхний AIR2999), не лишние4000. Следом freshsnapshot4, сначала finalserver/build70,
затем focusedclient4; sourcefreeze до обоих. Нет нового PASS.

Validate4exit1:200JUnit PASS,68/70server. Robe native3x3 second-mix consumption
assertfailed, rich actualcolor/slots diagnostics added (пока причина не установлена).
Shock deadline480ticks оказался0.822s acceleratedGameTest, недостаточно для11×250ms;
test-only allowance30000ticks/wrapper36000 +строгий10swall deadline, реальный clock
не подменялся. Сейчас идёт `clothing-fabric-server5 -GameTestOnly`, fast diagnostic
без повторной упаковки/--rerun-tasks. Это не finalsource-boundacceptance. Gameplay
не менялся. Client4 пока НЕ запускался; sourcefrozen до окончания server5.

Server5 runner не дошёл до Minecraft (PowerShellsingle-string splat ':'), XML был
stale, не считать новым результатом. .tmp runner typedstring[] исправлен. Server6
реально69/70 за6.249s: Shock весь compactcase PASS. RobeRGB A08BBF/robeconsumed/
blue4/white1 правильны; assertion ошибочно требовал emptyresult, но оставшиеся
BLUE+WHITE закономерно preview2LIGHT_BLUE_DYE vanilla. Исправлен **только тест**:
exactA08BBF +nativevanillapreview2 +егоисчезновениепослевозвратаleftoverinputs.
Следующийfastserver7, затем свежий snapshot5/client4/finalbuild5. Игровойкодтотже.

Client4exit1 на RADIAL_SELECT после6PNG81–86, native3robes/wear/dye/washes/anvil
уже прошли; clickправильнозакрылselector, testпотомполезвnullclient.screen.
Rootclientfix сохраняет именно clicked screen для release. **Новый production
visual defect найден осмотром failed85**: wearerсерый. Native javapHumanoidArmorLayer
renderArmorPiece88–113 подтверждает отдельный ItemTags.DYEABLE gate дажепри
dyeablematerial. Добавлен ТОЛЬКО render-scoped client.RobeArmorLayerMixin: robes
получаютcolorвэтойпроверке; vanillaDyeRecipe/cauldron tagне включается.
Failed86 также doubletooltip/footer отtestafterRenderповторногоtransparentGUI.
Clothing теперь движетcursor вownedactivewindow наShock и снимает ОДИНnativeframe,
безsecondrender; границаdeclaredвreport. MainJava262. Compile2 exit0; дальше
freshsnapshot6/client5/finalbuild6. validate5snapshotужеstale, не relabel.
НовыйExcavationcontract read-only закончен, включая scoped callback-skip option
для realprotectednativeharvest: `analysis/2026-10-16-excavation.md`. Ещёнеimplementation.

`goggles-fabric-validate3` exit0: **200 JUnit / 68 Fabric GameTests**, все68required,
failures/errors/skips0, **3598 unchanged inputs**,48Python, одинFabricJAR;
notices/isolation/source-boundrecorder PASS. `goggles-fabric-client2` exit0/
SCRIPT_PASSED: **7focusedPNG74–80**, rootосмотрелвсе7, run
`client-smoke-68c3b2cc-ad57-4940-9367-a78f7f6b5bca`,1м13с/839ticks.
`docs/porting/goggles-client.json` и`verification.json`; clientmatch actualfield
`matches_current_recorded_inputs=true`, historical_evidence_only=false.
AcceptedSTOPPEDsave: `fabric/run-clienttest/saves/goggles-smoke-056d3340-a4f2-44ed-b006-54b6984c5e7f`.
Всеsourceagents/Gradle/game завершены, mainJava255. ПолныйпортНЕготов/Neoнеrun.

Реально изготовлены2thaumometers, затемизнихgoggles450/270centivis actualThaumium
payerпослеfiniteNORMALnodecharge. Head350durability/armor1/5points, actualequip,
faintnaked/wand/offhand vsmainhandmeter48/goggles64, nodeсквозьстену,7currententries
снеизвестнымcompound13/quantityбезdiscovery. RealAIRjar16→8→16, четыреphials/
173savedessentia восстановлены; actualnewjarза85aqua, Fire6×8=48/12embers/cowignite.
Обаoldgoldwands unchanged/actualnewThaumiumwand/focus/altar72candles retained.
Daylight/сырьё/research/finite7-aspectNORMALnodes/workbench/wall/floor/camera/cow —
fixtures, неsurvival. Numbers/mirroringpopupисправлены (+X/−Y nativebillboard),
точные74–80показываютbook/eyes/meter/worn7entries/AIR8/.85menu/worngogglestexture.
Fireпослеэтогона80непоказанвполёте — его8debit/коллизияruntime, неpixelsclaim.
FullINodeAPI/inventoryShiftHUD/ArcaneEar/visbeams/Repair/runic/Baubles/Frugal/staves/
specialnodes/fullFX/audio/ordinaryEULA/remote/NeoForge остаютсяpending.

Последнийserverfix затронулТОЛЬКОgameTesthelper: GameTestHelper$2.isCreativeвсегда
true, поэтомуRMBcreative-copyневалиденкакsurvivalfixture; заменёнnativeQUICK_MOVE.
Client2не повторялсярадиэтого. Егоrecordсозданприallvalidate2inputsunchanged,
production/clientPFисключаютserver-onlygameTestroots, отсутствующиевclientclasspath;
finalbuild3 свежийпоследанныхправок иmatchesclientproductioninputs. No скрытогореруна.
Следующий [контрактмантий/Shock](analysis/2026-10-16-robes-and-early-foci.md), пока
неimplemented; originalspec+readableOpenTC4используютсяадресно, безdonorcopy.

### История интеграции предыдущих эссенции, наконечников и очков

`caps-fabric-validate1` exit0: **200 JUnit / 67 Fabric GameTests**, все67 required,
failures/errors/skips0, **3586 неизменённых inputs**,48Python; одинFabricJAR,
notices/isolation/source-boundrecorder PASS. `caps-fabric-client1` exit0/
SCRIPT_PASSED: толькоcaps scope, **7 точных PNG67–73 осмотрены**, run
`client-smoke-4940ea58-8d98-460d-b3c2-609a76b13298`. Client6м7с/6778ticks —
только новый путь с настоящей перегонкой/зарядкой, не повтор старой60-sceneистории.
Evidence: `porting/caps-client.json` / `porting/verification.json`. Остановленный
принятыйsave: `fabric/run-clienttest/saves/caps-smoke-1fd3fe4e-9a70-4861-8b82-b1551b2418b3`.

Actual: heldsurvival61ticks разрушил NORMAL AIR70node→8отдельныхessences→80essentia
из8essences+12rawcoal,10phialwithdrawals→4новыхjars. Старые129units сохранены.
6balancedshards→6настоящихSalis вvanillafurnace;2intermediateingots→18nuggets→
2paidinertcaps;2инфузии18units+3Salis каждая→2physicalcaps. Новыеsurplus44units,
8nuggets,3oldovenfuelcoal retained. Nativeassembly54/primal oldgold100vispayer→
EMPTY100visThaumium/Silverwoodwand, затемfiniteheldnodecharge иреальные6Fireticks
по9centivis=54,12embers, cow10→8+ignite. Обаoldwands/4phials/book/apple/tubes/drops
сохранены;1Silverwoodrod — явно intermediateINPUT, не разобранныйoldwand.
Nodeharvestthreshold/current/creative/doTileDrops/cancellation/codec/savepacket/
earned15essences→actualfurnaceauram30+aer30 серверPASS. Pickup10/scatteroriginal;
level-postdelivery/rejectedADDretention — documentedauthorityadaptations.
PNGночные:67tintтёмный,70непоказываетдлинныйstreamцеликом,73heldwandопущен;
nativecostsподтвержденыruntime, неугадывалисьпопикселям. Неfullvisual/audio/survival.

**Следующая unit: Goggles of Revealing**, уже sourceACTIVE, ещё не собрана/не
запущена. A `ses_ebb8ed624ffeOnYQpYvrIcEecq` **SOURCEFROZEN/root reviewed**: ItemGoggles/APIs/registry/sharedvispricing/
compactruntime. B `ses_ebb8dd577ffe1GlKdVHr6e7NI5` **SOURCEFROZEN/root reviewed**: TileNodeRenderer/TileNodecontainer/
новыйclientworldoverlay. C `ses_ebb8cdb3bffeYKRXkF7yVftVHQ` **SOURCEFROZEN/root reviewed**: только новый
GogglesClientSequence. Rootbootstrap/recipes/models/lang/book/loaderhook/scope.
Не дублировать ownedfiles; **никаких Gradle/game до всех SOURCEFROZEN**. Очки должны
реально давать hands-freenodevision/currentknown-unknowncompoundoverlay/5%vis,
не толькоhelmetmarker. Новыйfocusedrun наследует actualSTOPPEDcapssave, не полный
перезапускпрогрессии. CapsPASS выше — только3586точныхinputs, не будущиеочки.

Root gogglesintegration уже добавлен, **непроверен**: registerWearables/Clientinit,
arcane/goggles(261),bookalias/icon,complexsensus4(433),head_armor,lang/model и
original-byte goggles_layer_1 alias(128×64/hashв[goggles contract](analysis/2026-10-16-goggles.md)).
Bookstage/scopegoggles читает actualcaps-client.saved_world, sourcepathbounded,
newworkbench=1586,216,377/center1474,216,377, новыйprivateUUIDcopy; старыйsourceне
перезаписывается. Fabricwrapper/plannedrequiredfloor68 added. **Capturecountещё
нужноустановитьпослеCfreeze**, callbackFabricworldrenderпослеBfreeze. .tmprecord_goggles_client.py
готов, не выполнялся. Gradle/gameнеидут. Read-onlynextrobe/foci contractagent
`ses_ebb9b69c8ffeV0yUBQmIDfjvM7` не владеет source и не блокирует freeze.

Apricing: WandVisDiscount.percentagePoints(player,aspect) суммирует4armorslots и
вычитает strongestexhaustion вINTEGER, modifier(...) делаетединственноеfloatделение,
finalfloor.1; ArcaneVis каждыйprimal отдельно, FireAspect.FIRE. OriginalThaumium+
goggles5vis424/3vis254/Fire8 сохраненыsource, нопока runtimeнепроверены. Native
GogglesRuntimeChecks.run(test,player) API совпадаетwrapper, properties/recipe/equip/
prices/exhaustion/rounding/nativefollowup/anvil authored. Ждать B/CSOURCEFROZEN.

Breview/rootFabricLASThook wired: actual lateworldpass/loadedchunksno-generation,
rendercallbackssubtractcameraonce/flushbuffers; nativeNO_DEPTH_TESTсамno-op, shards
explicitlyclearLEQUAL/restore. Faintnaked/wand/offhanddepthtested, goggleshead64/
mainhandmeter48 revealnoDepth; normalhitResultonlycurrentcontainerallcompounds/
unknownicons+quantities/5perrow. Exports ThaumcraftGogglesClient.targetView(client)
TargetView(position,face,spaceAbove,entries[tag,amount,known,image,color],note),
TileNodeRenderer.view(node,client)->NodeView(revealed,range,distance)/inRange plus
revealedTo/revealRange. RootreadallBfiles. OriginalFOV70→threshold35.44radiansquirk
confirmedjoined.srg4043/bbj.aFconstructors; невыдумыватьузкийcone. ЖдатьCfreeze;
ещёникакогоcompile/game/sourcePASSgoggles.

**ALL SOURCES FROZEN**, C813lines/rootread/APIscoherent. Gogglesfocusedcount7,
contiguous74–80, fullstorythroughEquipmentпо-прежнему66(непланируетсяrun).
Native2meters→goggles450/270доэкипировки, realchargeknowncurrent-not-base7aspects,
naked/wand/heldmeter/offhand/goggles semanticviews, livejar16→8→16/4phials restored,
nativejar84centivis иFire6×8 authored. Всеrawresources/daylight/floor/node/research
fixtureslisted; old3wands/173essentia/72candlesretention. Rootaddedearlygateinlate
rendererчтобыnakedplayersнесканировали81chunkmaps/frame. Это всёещёsource-only.
Теперьcompile-onlyFabric; послеcompilePASS freshsnapshot+onlygogglesclient7, затем
одинfinalFabricbuild/server68. Не менять source duringGradle/game. Всеsourceagents
завершены; nextrobe/Shockcontractread-onlyготов, ещёнеimplemented.

Compile1exit0; focusedclient1exit1 после5PNG: erroneousEXPECT84дляjar. Original
FLOAT productrounds1vis85 beforeint; gameplaypricingправильный, expectation/name/
counterисправленына85 (3vis254/5vis424 unchanged). Rootувидел75–78failedPNG:
настоящиеноды/currentunknown/realjar работают, **numbersневидны/iconsmirrored**.
NativeEntityRenderer.renderNameTag.javapподтвердилscale(+X,−Y,+Z); productionpopup/
notescaleисправлены, camerajar2.4→3.6впределахexistingplatform/reach. Nooutput/
contentsinjection. Serverhelperтотжеcaseдополнен1vis105/95/85. Всеownersfrozen,
sourceпослеvalidate1snapshotизменён: нуженfreshvalidate2snapshot иfocusedclient2.

Пользовательсновауказал https://github.com/IgnoreLicensesCN/OpenTC4 испросил,
почемуоннеиспользуется. Ответ: онУЖЕpinnedread-onlyreference, неигнорируется;
ограничениекасалосьпрямогокопирования/прав, НЕизучения. RecheckedGitHubAPI
master16.10=80aac29c(тотжепин). RootсейчаспрочиталименноегоREADME/build/LICENSE/
ItemGoggles/ItemRobeArmor/ItemFocusShock: полезныечитаемыеназвания, Forge1.7.10,
READMEговоритHodgepodge/TC4Tweaksmodifications/BaublesExpanded. Это неготовый1.21.1
Fabricпорт. Использоватьактивнокакmapping/technicalreference, оригиналTC4 остаётся
spec; неделатьновыйполныйaudit/сменубазывместоконтента. Ниrefbuild, нидонорcopyнет.
Gogglesfocusedclient2ужеидёт(snapshotvalidate2), исходникипокане менять.

Client2теперьexit0/SCRIPT_PASSED7, run`client-smoke-68c3b2cc-ad57-4940-9367-a78f7f6b5bca`.
Rootосмотрелвсе7exactPNG74–80: numbersисправлены, unknown13/rowwrap7/readableAIR8,
bankprice0.85, nativewornGold/purplegoggles. Evidence`porting/goggles-client.json`
записанприВСЕХнеизменённыхvalidate2inputs. Production/client fingerprints исключают
isolatedserver-onlygameTestroots (ихвообщенетвclientclasspath).
Finalservervalidate2exit1/67of68: fixtureошибочноожидалsurvivalRMBсmockplayer.
Read-onlyjavap GameTestHelper$2.isCreative ВСЕГДАtrue независимоотGameMode;
Equipable.nativeRMB закономерноcreative-copies armor. **Gameplayне менялся**:
helperисправленнанастоящийInventoryQUICK_MOVEкаквуспешномsurvivalclient. Только
common/src/gameTest/.../GogglesRuntimeChecks.java изменёнпослеclientrecord, поэтому
клиентповторятьнезачем. Нуженfreshvalidate3snapshot+finalFabricbuild/server68;
reuseclient2exactacceptedPNG/productioninputs,невыдаватьserverfailureзаPASS.

### История интеграции принятой unit эссенции/наконечников

**Уже идёт новая unit [NodeEssence/Thaumium caps](analysis/2026-10-16-node-essence-thaumium-caps.md).**
Sourceowners ACTIVE: `ses_ebbb782b2ffemUvPaQqNiZcHHw` owns existingBlockAuraNode+
новыеessence/component/registry/API/compactruntime; `ses_ebbb5922effe881X2NDuqe83CM`
owns только новыйThaumiumCapsClientSequence. Rootownsпрочиеexistingfiles/resources/
wands/book/scopes. Не дублироватьagents и НЕrunGradle/game доALLSOURCEFROZEN.
Capmaterial/recipes/.9/cost6/codeclimit54/models/normalizedCAPproperty/aspectSalis/
WispbaseAuram2 иbootstrap/tint уже root-added, **непроверены**. Plannedfloor67,
capsfocusedcapturecountпокаожидается; не запускатьfullstoryрадиэтойunit.
EquipmentPASS ниже относится к точным3563старымinputs, не к этимновымправкам.
EssenceAтеперьSOURCEFROZEN/rootread. Rootподключил NodeHarvest.endTick перед
Deferreddrops иclearSERVER_STOPPED; tooltipviewer/48creativevariants. Не принята
агентскаяinventoryscatter-адаптация: rootдобавилузкийenqueueBlockHarvest с original
worldoffset.7F×3/defaultitemuniformmotion/pickup10, optionalqueuefielddefault0,
existinginventorypathunchanged. Осталасьлишьauthorityadaptationlevel-post/rejected
ADDretention. Runtimehelperпроверяетactualscatter/delay. NoGradle/gameдоBclientfreeze.
Bclient теперьSOURCEFROZEN, root прочиталвесьfile. **ALLSOURCEFROZEN**:
scopecaps/7captures67–73 wired; FurnaceScreencapturehook уже есть. Fullstoryчерез
equipmentостаётся66сценами, сейчасне запускается. Newcaps reusesrealformedaltar/
72candles/9pedestals иsavedoven, harvest8essences→80newunits→36paid/6Salis→2caps→
native54assembly/finitecharge/actualFire9. Всеэтиописанияпокаsource-only.
Следомcompile-onlyFabric, focusedcaps; послеуспехаодинfinalbuild/server67. Не менять
исходникивовремяGradle/game. Recorder.tmp/record_caps_client.pyготов, но не выполнялся.

### Предыдущий принятый срез: таумиевое снаряжение

`equipment-fabric-validate2` exit0:200JUnit/66Fabric/allrequired66,
failures/errors/skips0/3563unchangedinputs/48Python/FabricJARnotices+isolationPASS.
`equipment-fabric-client1` exit0/SCRIPT_PASSED, **equipment-only6captures61–66**,
root осмотрел все6; run`client-smoke-560ad46d-c8e7-453a-ae0a-4f42126c9f23`.
Client1м24с/1203ticks; длиннаястараяцепочкаНЕповторялась. RealacceptedM07save
скопирован в новыйowned equipment-smoke-UUID; обаearnedwands/129essentia/5coal
сохранены какinputs, не выданы/неперезаработанывэтомrun. Evidence:
`porting/equipment-client.json` / `porting/verification.json`, recorderPASS/1Fabricjar.
Старый M07verification: `.tmp/infusion-accepted-verification7-before-equipment.json`.
ВсеagentsиGradle/gameпроцессызавершены. MainJava243; fullFabricпортНЕготов.

Actual:24rods+12iron→12nativeingots→pick3/chest8/repair1; survivalstone/obsidian
miningсреальнымиdrops/wear, nativeanvil1level, затем7declaredintermediateingots→
leggings/nativeequipchest+legsarmor11. Все9registeredrecipes/stats/strictingotrepair/
armorattrs/attacks7/6/5/4/1 иanvilrepairsсерверPASS. Rootвиделactualbook/menu/ingot/
obsidiandrop/кирку/2armor layers, ноночнойконтраст66/tooltip65/HUDoverlapнеpolishPASS.
Hoeench5/nohitwear0 сохранены; cooldown/nativeenchantcombatнеTC4fullparity.
Repairenchantment/runic/caps/Salis/auramsource/Neo/ordinaryEULA/remote/survivalpending.

Next [originalNodeHarvest→WispEssence→auram→chargedcaps](analysis/2026-10-16-thaumium-next-unit.md).
OriginalBlockAiry265–279: currentaspectamount≥5→floor(amount/10)+1physicalessences,
каждаяcontainedaspect2+baseauram2. Неfakebeaconrecipe. ПрочитатьMC/Forgeplayer-removal
ordering/gamerules/creativeпередреализацией; не превращатьeveryBEremove/unloadвdrop.
ПокаэтаunitНЕimplemented и не тестировалась. Routinechecksfocused, fullclientstory
толькодлякрупныхsharedchanges/checkpoints. НикакихNeoedits/builds/commit/push.

### Предыдущий принятый M07: инфузия/Silverwood

`infusion-fabric-validate7`exit0:200JUnit/65Fabric/allrequired65,
failures/errors/skips0,3521unchangedinputs,48Python/FabricJARnotices+isolationPASS.
`infusion-fabric-client5`exit0/**SCRIPT_PASSED60**, rootосмотрелexact53–60;
run`client-smoke-15fab7dc-e9ea-4a9f-b22d-9239a5ff09b3`. Source-bound recorderPASS:
`porting/infusion-client.json`/`porting/verification.json`,1Fabricartifact/noNeoPASS.
ВсеGradle/gameпроцессызавершены.234mainJava; semanticledger1044needs_review,
неполныйпорт. СтарыйTubesverificationархив`.tmp/tubes-accepted-verification3-before-infusion.json`.

Actual: nativeMatrix/10pedestals(9placed+1retained)/72candles→formation25eachprimal;
48rawshards→192essentia→63one-unit10tickpayments+7components6cycles→physicalcore
eject/pickup→2nativegoldcaps→assembly27eachprimal→EMPTY100viswand→realnodeheld-use
6000aer/node80→20.129surplus/waste/oldwand+focus+book+apple+4phials+6tubes+8drops
retained.72realpairedcandlesдаютsymmetry−6/I−1, noforcedI/RNG. Input/research/oldwand
topups/emptymachines/floor/camera/nodefixturesdeclared, outputsнеинъецировались.
Rootвиделnativebook/menu/OBJpillars/runes/candles/paidstream/core/correctsilverwood
rod+goldcap. Audiolistening/fullFXparity/enchantment/runic/otherrewards/API/Neo/
ordinarydedicatedEULA/remote/fullsurvivalpending. Originalcandlewaxdrips/othercolors
иcтарыйradialfooter/HUDoverlapнеисправлены. [Правила/история](analysis/2026-10-16-infusion-first-unit.md).

Следом [9полезныхThaumiumequipmentitems](analysis/2026-10-16-thaumium-next-unit.md),
неfakecapreward. Caps требуютподтверждённогоprocessedauramsource; beaconcomplex2
не считать usableдоnativecull/distillationproof. НовыеM07bsourcechanges будут
отдельнонепроверенными, не наследуют этотPASS. НикакихNeoedits/builds.

### M07b: активные владельцы, сборку пока не запускать

Пользователь справедливо спросил, зачем полныйsmoke ради однойfeature. Workflow
сужен: **не повторять12–15минут старойresearch→infusion цепочки дляequipment**.
`-PclientSmokeScope=equipment` / `.tmp/a07-client-smoke.ps1 -Scope equipment` копирует
реальный принятый `book-smoke` save в новый owned `equipment-smoke-UUID` и начинает
толькоEQUIPMENT. Существующие earneditems — настоящие сохранённые inputs, не
reacquiredthisrun/неинъекция. Sourceмир не изменяется; captures только61…final,
scope/progression_not_replayed отраженывrawreport. Старыйfullрежим сохраняется
для крупныхsharedchanges/checkpoints, не запускаетсяавтоматическирадикаждойфичи.
CountвGradleпоканеобновлён: дождатьсяclientSOURCEFROZEN иactualnames/count, затем
задатьexpectedcapturecounts поscope. Это маленький branchвexistingrunner, неframework.

- Equipment `ses_ebbdff5d4ffeaCzZfvNd6QUvz1` **SOURCEFROZEN**: только новые ThaumcraftEquipment,
  пять ItemThaumium tools, ItemThaumiumArmor, IRepairable/IRunicArmor и один компактный
  ThaumiumEquipmentRuntimeChecks. Регистры/реальные stats/repair, не caps/способности.
- Client `ses_ebbde5a6effe0oio3MVxVcHPz4` **SOURCEFROZEN**: только новый ThaumiumEquipmentClientSequence,
  constructor(workbench,crucible),tick(client,capture),report, captures61…final.
  План: native metallurgy→pick/chest/leggings→obsidian mining/anvil/equip. НеRUN.
- Root: существующие bootstrap/aspects/book/icons/resources/lang/tags и BookClientSmoke.
  Добавлены9обычных recipes/models, c:ingots/thaumium и c:rods/wooden, native9tool/
  armor/enchantmenttags; metallurgy gateнепереноситсянаordinaryequipmentrecipes.
  THAUMIUM_TIER→legacyHarvestLevel3,9bookaliases, whitelisticons иточныеen/ruимена.
  ArmorLayerbytealiases созданы `.tmp/alias_thaumium_armor.py`; original/images
  не изменялись. BookStageEQUIPMENT/reportkeyequipment wired, freshcoords +224/+240.
  Root прочитал frozenclient и связалcount: full66(непланируетсясейчас)/focused6,
  contiguous61–66. Focusedorigin берётся из acceptedM07workbench+64X, чтобы старые
  oven/jars оставались настоящими сохранёнными inputs, не координатами от камеры.
  Всеисточникиfrozen; разрешёнцентральныйFabricbuild/server+толькоfocusedclient.

Root прочитал frozenA:9items/registry/markers/compactruntimehelper соответствуют
APIcontract. ДобавленFabricwrapper/requiredcase66. НайденMC1.7→1.21 inheritancequirk:
originalHoe(ada)extendsItem(adb), default hitboolean=false/no wear; nativeHoeextends
DiggerItemwouldspend2. Rootoverridehoe.hurtEnemy=false/postHurtEnemy=noop сохраняет
оригинальныйнулевойhitwear. Старые sword1/axe/pick/shovel2 остаются. Bytecode read-only:
originaladb.a(add,sv,sv)=false;aehspends1/acg2, modernDiggerpostHurtEnemyspends2.
Helperassertожидаетhoe0; это source-backedfix, неruntimePASS. ClientagentещёACTIVE.

ClientтеперьSOURCEFROZEN:6captures61–66, rootпрочиталвесьfile. Естьнастоящие
24rods+12iron→12ingots→pick3/chest8/repair1; отдельные7intermediateingotsтолькопосле
этогодляleggings; no output/damage/heat/aspects setters. ОбычныйanvilScreen добавлен
в existingcapturehook. ScopeequipmentкопируетнастоящийacceptedM07save; егоorigin
читаетсяизacceptedinfusion-client.json(workbench+64X), анеизплавающейкамеры.
Всеsourcefrozen. Validate1остановилсянаinvariantgenericsCreativeTabRegistry.append
дляразныхtypedRegistrySupplier; rootперешёлнаappendпоодному, безсменыitemAPI/stats.
Сейчастолькоcompile2; приPASSодинcentralbuild/serverиfocusedclient6, НЕfullstory66.

Новые источники M07b уже продвинули дерево: accepted M07snapshot7 остаётся историей,
не доказательством новойunit. Финальная проверка толькоFabric, без commit/push.

Предыдущий **Fabric-only** checkpoint: `tubes-fabric-validate3`exit0,
200JUnit/63Fabric/allrequired63, failures/errors/skips0,3458inputs unchanged,
48PythonPASS/FabricJAR notice+testisolationPASS. `tubes-fabric-client1`exit0/
SCRIPT_PASSED52, root exact47–52 inspected; run
`client-smoke-f4c37215-d61b-402e-a496-1c06a345abff`. Source-bound recorder PASS,
`porting/verification.json`loaders_checked[fabric]/not_checked[neoforge],1artifact;
client evidence`porting/tubes-client.json`. Существующий NeoForge не билдился/не
прогонялся и старые61PASS сюда не перенесены. PreviousFireverification archived
`.tmp/fire-accepted-verification7-before-tubes.json`; historicalclient retained.

Actual retainedquicksilver→9drops→8arcaneTubes за500aqua+500ordo centivis;2native
floatingplacements, nativewandclosedarms51serverticks/reopen.4inputcoal+1fuelcoal
→8potentiaвjar/8ignisостаётсявupperalembic;150actualticks exactconservation,
8increments по1. Blanklabelnativebind→survivalbreak→physicalpickup→placement
сохраняетall8+filter.6tubes/8drops/4phials/focus/wand/book/apple retained.
Fixtures: rawinputs/emptymachines/research/vis-topup/floor/camera/nativebookscreen;
no craftedtube/essentia/filledjar/boundlabel/nativejar-output injection. Неsurvival.
Serverfull64/vent/reload/wrongphials тожеPASS, ноventFX/audioнеосмотрены. Native
geometry/gap/rejoin/label видны;49/51helditemtransition иHUDoverlap неvisualperfection.
204mainJava, semanticledgerвсёещё1044needs_review. Дальше M07наFabric.

Предыдущий checkpoint: `fire-validate7`exit0,200JUnit/61Fabric/61NeoForge,
failures/errors/skips0/allrequired61,3437inputs unchanged; `.tmp/fire-assets7.log`
48PASS. После последних production/client changes выполнены оба loader builds и
runtime, затем source-bound recorder PASS. `fire-client5`exit0/SCRIPT_PASSED46,
root exact41–46 inspected; run `client-smoke-c1af70f4-508c-42b3-9790-fc8c3da7613d`.
Evidence: `porting/verification.json`, `porting/fire-focus-client.json`.

Basic Fire: actual recipe20ignis/10perditio→1focus→heldFselection→6nativepaidticks/
60centivis/12ember ADD attempts→cow10→8+ignite→ShiftFremove1focus. Во время wandF
нетswap; ordinaryhandsF выполняет2nativevanillaswaps/restore. Shimmerleaf/cinderpearl
survivalharvest→2native2×2crafts→1quicksilver+1blazepowder;4phials retained.
Никакой focusoutput/equippedcomponent/ember/damage/plant-resourceoutput не supplied;
inputs/research/vis-topup/workbench/noAIcow/plantedpatch supplied. Это не survival.
Main194Java, tags108direct+6complex/85rows. Full M02/M04/M08/API не закрыты.

Известный visual gap: footer radial/help накладывается на vanillaHUD/itemlabel;
title/tooltip/select/remove читаемы и работают, header/rings больше не blurred.
Его polish вне принятого checkpoint; нет legacyHUD/fullanimation/upgrades/otherfoci.
NativeKeyMapping/site-specificswap fix сохраняетобычныеcontrols; no global key rebind.
Один own MC window foreground request был OS fixture, без keyboard/mouse/fakeflag.
[Правила/история отказов/границы](analysis/2026-10-16-fire-focus-and-flowers.md).

Предыдущий checkpoint: `distillation-validate5`exit0,200JUnit/58Fabric/58NeoForge,
failures/errors/skips0, все58required cases;3397inputs unchanged,48PythonPASS из
`.tmp/distillation-assets5.log`. Source-bound recorder PASS; оба remapped JAR
notice/isolation PASS. `distillation-client5`exit0/SCRIPT_PASSED40captures, root
exact33–40 inspected; run `client-smoke-16f2592a-2cee-4f19-a883-c25d136f5ba8`.
Evidence: `porting/verification.json`, `porting/distillation-client.json`.

Работают magical trees/wood/saplings, arcane stone family, Filter, normal furnace,
alembics32, manualphials8→jars64, labels и bellows; wood/greatwood + iron/gold
assembly/nativevis/realmaterialmodel/50capacity/NODEPRESERVE. Nativeclient:
8coal +1Alumentum→16ignis+16potentia→4phials→2jars,4emptyphials возвращены,
machine buffers0; wandassembly дебит990eachprimal, outputgreatwood/gold заряжен
через настоящий held-use. Inputs/machines/research/node/camera/seeded trees supplied,
не survival/natural-frequency verdict. Root осмотрел transparentjar+visibleliquid,
originalOBJ alembics/animatedbellows, nativeGUI/book/icons/trees/wand.
Последние исправления: unit-codec shared recipe identity (реальный loginpacket),
camera-drop ack, jar translucent pass и original bound-label bonus1. После этих
sourcechanges выполнены client5/validate5. Inventory176main/34partial/900pending/
0complete; curated tags106direct+6complex/83rows. Полные M02/M04/M05 не закрыты.
[Срез/исходные правила/границы](analysis/2026-10-16-distillation-and-magical-wood.md).

Предыдущий checkpoint: `m05-validate4` exit0,197JUnit/51+51GameTests,
3228inputs unchanged; `.tmp/m05-assets3.log`48PASS. `m05-client2`exit0/SCRIPT_PASSED,
run `client-smoke-0556d7cf-2e83-40a9-99b6-83996b169865`,32captures/root exact27–32.
Evidence: `porting/verification.json`, `porting/alchemy-client.json`.

Тигель с10recipes/физическим растворением/нагревом/водой/decay/spill, полезные
Nitor(light15+heat) и Alumentum(6400fuel/throwable blast1.66), finiteflux/exhaustion,
ObjectAspectResolver и96originaldirect+6complexbindings. ActualFabricC2S/drop/collision:
2coal+3torch→4potentia/4ignis/3lux; glowstone→1physicalNitor, water950/remaining1+1;
pickup→Nitorplacementundercrucible. Suppliedmaterials/research/camera, не survival.
Исправлены protectedplacementcontext, canonicaleffectHolder/NBT и bluewater tint;
после finalsourcechanges выполнены validate4/client2, sourceboundrecorder PASS.
Inventory132main/34partial/900pending/0complete до следующего параллельного среза.
[M05 source anchors/gaps](analysis/2026-10-16-M05-early-alchemy.md).

Предыдущий checkpoint: `early-content-validate3` exit0,192JUnit/47+47GameTests,
3149 inputs unchanged; `.tmp/early-content-assets2.log`48PASS.
`early-content-client2` exit0/SCRIPT_PASSED, run
`client-smoke-6dd1fd3d-3770-4852-8161-9d8b8e625b02`,26captures; root exact04/12/21–26.
Evidence: `porting/verification.json`, `porting/early-content-client.json`.

Реализованы:8 natural ores/drops/smelting→thaumometer; R09combine/discovery и
602-tick bonus ecology; natural NORMAL nodes/current/base/regen/prune/death/BER;
arcane workbench с original wand trigger, server-priced110centivis и двумяrecipes;
book pages10actualrecipeIDs. Реальный Fabric GUIcombine→node-held-use→arcane craft.
Worldgen проверен на16fresh noise chunks каждогоloader, не только `/place`.
Client fixture supplies materials/node/RP; это не полный survival. [Правила](analysis/2026-10-16-early-content.md).
Исправлен старый RGB-only item tint:1.21.1 требует ARGB alpha255, иначе shards
в книге/инвентаре невидимы. Current exactPNG04 показывает оба shard ingredients.
Полныеspecialnodes/recipes, R10count100/127, multiplayer/Neo client/packaged open.
Следующий контент: M05тигель/начальнаяалхимия, затем silverwood/Vis Filter.

## Продолжение после проверенной Fabric-сети эссенции

Все source/review agents M06frozen; builds/game processes завершены. Перенесён
boundedbasicTube/jarUPpull/quicksilverDROP, [правила/границы/история](analysis/2026-10-16-basic-tubes-spec.md).
Дальше [M07infusion](analysis/2026-10-16-infusion-first-unit.md) наFabric: genuine
formation/pedestals/wireless/demand/cycle/hazards→Silverwood100vis reward. Новые
M07sources потребуют отдельногоfreeze/build/client, не подтверждаются tubes3.
Smallatomicconsumehook согласовать с сохранением обеихstoragewrites доcallbacks;
не возвращаться к callback-bearing take/refund и не начинать NeoForgeпаритет.

**M07 все source agents FROZEN; rootintegration завершена, centralFabricvalidation разрешена.**
- Engine `ses_ebc58d147ffeS8G5kI0DAq27th` **FROZEN**: новыеBlock/TileInfusionMatrix,
  InfusionStackRecipe/Input, EssentiaHandler,1compactruntimehelper. Не existingfiles.
- World `ses_ebc580356ffe928NBgW7h5Iept` **FROZEN**: новыеPedestal/Pillar/formation/scan/
  symmetry/all21hazards/candle/crystalstabilizer/taintEffect,1runtimehelper. Неexistingfiles.
- Visual `ses_ebc509578ffetcmH5wpHZl9Y4T` **FROZEN**: только новыеclientwrapper/3BERs/ModelCube/
  PillarOBJ+InfusionClientEffects/particles, безexistingfiles/resources/network/tests.
- Client `ses_ebc3c4d06ffe1v140tnfF0gSF1` **FROZEN**: только новый
  fabric/.../InfusionClientSequence.java. RootвладеетBookClientSmoke/buildcount.
  Constructor(workbench,center),tick(client,capture),report; новыеcaptures53…final.
  finite48shards→192essentia→63paid/7components→core→goldassembly→charge>=60aer;
  nativecraftmatrix/10pedestals(9placed+1retained)/72candles/formation.60captures,
  contiguous53–60. Всеописанноевsource, но ещё неcompiled/run/visual.
- Root: register/bootstrap/wand/materials10000+codecs/recipes/aspects/book/render/
  FXnetworking иminimalconsume. НикакихNeoедits/builds. ПослеALLfreeze rootможетbuild.
Stablecontracts вprompts: InfusionStructure.isValid/scan→Surroundings(pedestals,symmetry)/
tryWandInteraction; MatrixisActive/isCrafting/getDemand/getPedestalPositions/startCrafting/
setActive/sendEffect; PedestalsetInfusionStackpreservescount; rootregistryThaumcraftInfusion.
Root уже добавилInfusionDemandEndpoint3methods иEssentiaTransfers.consumeForInfusion:
sourceIAspectSource+nativeendpoint−1/matrixdemand−1 beforebothnotifications, sameTxlock;
wirelessrange12dy−12..11, NOSIDES/suction/label. M06acceptedinputsтеперьпродвинулись,
новаяM07ветка НЕ подтверждена tubes3/client1. СледующийcentralтолькоFabricпослеfreeze.
Root уже включилSilverwood10000/cost9, craftcostcomponent27, rodproperty2 и4held
models+originalroditem/sprite; прежниеcapacitychecksостаютсяcross-field. Existing
WandDataTest теперьпроверяет6materialpairs/10000roundtrip иreject10001/unsupportedstaff.
NativeS2CFXpayload/rootnetwork статические: dimension,matrix,event0..7,target,data;
sendonlyafterrealactions/64radius/noC2S, ClientwrapperC.initClient(consumer).
RecipesMatrix40ordo/Pedestal2aer5/Candle3ordinary/Silverwood63ess7componentsI5 добавлены.
**RecipeJSON input — declaredsilverwood_log**, НЕtag: это нужно originalaspectgeneration
630–656. Tagoriginal_log_wood дан отдельно; приreconciliation A убедитьсяruntime
input/ongoing ore-fuzzy допускаетoriginalsame-logWood какsource, аderiveаспектов
берёт declaredprototype. ObjectAspectResolver.rootдобавилfromInfusionпослеArcane.
RootwiresBlockCrystalвexistingfactoryс прежнимиIDs/properties; этоstabilizer,
неinventedinfusedore. Registry/bootstrapforwardcalls/newbookaliases добавлены,
rootThaumcraftInfusion ужедобавлен:3stoneDevice blocks/BEs+whitecandle+taint+STACKtype/
serializer. Stonehardness3/effectiveblast15, matrixlight10/candle14; noobtainablepillaritem.
LifecycleSERVER_STOPPEDclearswirelesscaches. TaintrealcustomdamageJSONbypassarmor/
witchmagicresist, originalpotions(3,1)iconcropped54,216→72,234 by.tmp/crop_taint_icon.py.
Rootblockstates/models/loot/miningtagдобавлены; pedestalbodyoriginal3boxes; candle
originalwax/wick2boxes(no position-randomwaxdrips/othercolors). SilverwoodbookringиInfusionAltarlayerplan
nativeviews реализованы, nofakeRecipeRegistration. PlanboundactualMatrixrecipe/
devices+sameInfusionStructure.formationVisCost(); неисполняетextractedJava.
Rootдобавилmissingoriginalcraftfailaudioevent6; noextraservercraftstart (Cownsclientaudio).
NewFabricwrappers+requiredfloor65, noNeoedits. APIsverifiedA/B/Cbysourceread,
NOTcompiled/run; frozeninputsacceptedTube3458неподтверждаютэтиновыеправки.
RootbookclientstageINFUSION/reportkeyinfusion ужеwired; originsresearch+(160,0,8)/
(176,0,8), capturecountПОКА52доactualclientfreeze. Globaltimeout420→1200sec: старые
52scenes≈225sec +48shards*100ticks+nativeinfusion, безускоренияticks/инъекцииoutputs.
ExistingWandAssemblycase расширенnativeSilverwoodgold27/primal/finite100vis/reload;
его core/caps —declaredinputfixtures, actualearnedcoreбудетотдельнымclientstory.
Read-onlyreview `ses_ebc355b6bffeyP5ZaVOMtCyMXD` active; noedits, неsourcefreezeblocker.
Reviewзавершён: найденодиноригинальныйquirk—abort/startнеобнуляютitemCount
(oracleM252–333/412–419). Rootубралдвасброса, render-targetочищаетсяотдельно,
native reloadпо-прежнемусбрасываетtransientcountdown. НеI=0исходи неrefundресурсов.
Остальнойreview—sourceonly, неruntimePASS. Clientsourceтеперьfrozen: block/registry/
APIreconciliationпрочитана, buildcount52→60, BookStageINFUSION/reportkeyinfusion
ужеwired; nextfreshsnapshot/build толькоFabric.
`infusion-fabric-validate3`exit0:200JUnit/65Fabric/allrequired65,
failures/errors/skips0,3521inputsunchanged.48PythonPASS/FabricJAR notices+isolationPASS.
Run1compilefixnativeItemColorsaccessor; Run2onlynewSilverwoodteststaleJavaobject:
applyCraftinstalls paidcopy, assertionnowreadsactualslot10 (productionunchanged).
Now `infusion-fabric-client1` background,60capturesexpected,53–60notyetinspected.
DoNOTeditsourcesduringclientrun. Pendingrecord `.tmp/record_infusion_client.py` with
rawkeyinfusion/exact53–60observations; genericrecorder--loadersfabricafteractualPASS.
Read-onlynextrewardcontract `ses_ebc1f6328ffe8EGUyrFsF2nOOL` active, nofiles/edits.
Client1failedвM07prepareнаhandoffslot8: M06законнооставляеттам6реальныхтруб.
RootисправилТОЛЬКОновыйclientsequence: nativeinventorySWAPпереноситихвпустойmain
slotдоinputfixtures, неclear/неoverwrite/неинъекциятруб. Rawfailedreport сохранён
`.tmp/infusion-fabric-client1-raw-report.json`,52предыдущихscenes выполнены заново.
Nowbackground`infusion-fabric-client2`,60expected. Freshsnapshot
`.tmp/infusion-fabric-validate4-inputs.json` takenbeforeclient2; finalcentralvalidate4
запускатьПОСЛЕclient2 иникакогоконкурентногоGradle. Productionпослеvalidate3неменялся,
нообщиеinputfingerprintsadvancedclientfixture; finalrecord3сновымclientневозможен.
Read-onlynextcontract готов: [Thaumiumequipment/caps](analysis/2026-10-16-thaumium-next-unit.md).
Сначала9реально полезныхобычныхequipmentitems безfakeabilities/researchcraftgate.
Caps требуютcharged/inertstages+Salis иреальногоauram; proposedbeaconcomplex2 НЕ
считатьusableauramдо проверкиfinalgetBonusTags/cull6+native distillation. Неbypass
cull/неfakefilledjar. Followuporiginalattackmodifier/nativeconstructors идётread-only
втойжесессии; production покаНЕизменятьдозавершениятекущегоclient2/record.
Client2failedCORE_PICKUPпослеactual63payments/7consumptions/1core/129retained;
native1unitcadence10ticks/ingredient6cycles реальны. Rawreportархивирован; root
осмотрелегоexact53–58 (но этоFAILED run, неfinalclientPASS). SavedworldNBTread-only
подтвердилnativecoreуplayerSlot4, а clientвиделghostнаpedestal. Native1.21
ClientPacketListener.method_38542 намеренноНЕзагружаетemptyBEtag; TilePedestal
раньшеслалпустойtagприstackempty. Rootproductionfix: Count0 всегдаsave/update,
безизменениястоимости/выдачидропа. Существующийservercaseпроверяетnonemptyempty-update.
`infusion-fabric-validate5`exit0:200JUnit/65Fabric/allrequired65/3521inputsunchanged,
failures/errors/skips0,48Python/notices+isolationPASS. Nowclient3background/60expected.
DoNOTeditsourcesuntilclient3ends; freshfinalevidence привязыватькsnapshot5еслидальше
ничегонеменяется. Snapshot4/планvalidate4 устарели, ихfinalresultнет.
Client3 дошёлдоREWARD_WORLD: actualcorepicked/consumed,2nativegoldcaps,2700each
primalassembly, newEMPTY100viswand chargedrealhelduse→6000aer, node80→20;
129infusionessentia иoldwandretained.59captures, FAILEDтолькоactualspriteassert.
Причина:ClampedItemPropertyFunction.callклипуетproperty2до1→Greatwoodfallback.
Rootfix RODproperty wood0/greatwood.5/silverwood1 И соответствующие8JSONpredicate
thresholds, noinventory/data/damage/recipechanges. Futurecapsнеповторятьinteger2!
`infusion-fabric-validate6`exit0/200JUnit65Fabric3521unchangedinputs/48Python/
Fabricnotices+isolationPASS послеfinalmodelchange. Nowclient4background/60expected,
SOURCEFROZENничегонеменять. FinalrecorderеслиPASS используетsnapshot6/client4,
exactPNG53–60обязательноосмотреть. Client3rawархив.tmp/infusion-fabric-client3-raw-report.json.
Открыты PURE knots/taint biome/spiderloot, advancedtubevariants, automation/API,
остальные materials/foci/armor/infusion/golems/mobs. Shimmerleaf уже реализован,
его naturalpass больше не пропускается. Нет полного semantic completion.

По новому запросу пользователя завершены **read-only reference audits**:
- `ses_ebcc5eacdffeyWX0E6C6T2UPUc`: DJDMM/Thaumcraft-6-1.20.1-port;
- `ses_ebcc5b33efferLTtU1PFZ8Twnw`: notlikethiss/thaumcraft-4-reforged.
Findings/pins/license: [два проекта](analysis/2026-10-16-user-reference-projects.md),
source-lock/THIRD_PARTY_NOTICES обновлены. Полезнее первым Reforged branchmc/1.21.1
`d1e64cc...` (main уже26.3/Java25), особенно tubes/infusion/wand rendering. TC6
`3ee6ed6...` полезен native side hit shapes/BER/OBJ, но НЕ FocusPlan/aura/research/
capacity250. Оба All Rights Reserved; donor copy permission не установлено,
runtime не запускался, code/assets не копировались. Следующая unit после Fire —
M06 трубы на нашемAPI, потомM07infusion, сверятьoriginal (Reforged explosion/loot
deviations указаны в report). Эти read-only audits не блокируют source freeze.

ПослеM06 честный M07infusion reward — Silverwood core→100vis wand: ConfigRecipes336,
подробный original [контракт](analysis/2026-10-16-infusion-first-unit.md). Исправление
раннего предположения: центрsilverwoodLOG,7shards balanced+6primal,9каждогопримала+
9praecantatio,total63,instability5 (не greatwoodrod/20×5); все21hazards обязательны.
Pedestal/Matrix recipes221–222; formation WandManager135–205 costs25eachprimal,
matrix at pedestal+(0,2,0), four brick bases+(±1,0,±1) +stone above, othercellsair.
INFUSION parentDISTILESSENTIA; не делать фиктивную instability0 или static item-output.

Предыдущий R08: Tile/Container + bounded edit/state payloads,
серверный commit, palette drag/place/erase, solved discovery и обычное изучение.
`r08-content1`: exit0,191JUnit/42+42GameTests. `r08-content-client1`: exit0,
SCRIPT_PASSED,20captures; root inspected exact17/20. Actual GUI/C2S цикл
place→erase→place→solve→take→learn, ink4/lux0/ignis2. Synthetic geometry, не survival.
Evidence: `porting/research-table-editing-client.json`, `porting/verification.json`.

User steering: приоритет **игровой контент**, не расширение инфраструктуры/автотестов.
Найден новый [reference](analysis/2026-10-16-eugene-reference.md), pinned
`a9b7a613c9c3e5a4418f30e0cf38553ac328b1e6`, checkout в sibling work/references.
Код не копировался; public source требует внешних непубликованных частей.
Донорские частоты/depth-weighting не взяты вместо original. Source pins не менялись.
M03 special rolls пропускаются без reroll; defaults/config/retrogen ещё не закрыты.

## Подготовка A07/pure R08 (история до native content)

- При восстановлении HEAD=`ac1734af` (R07 implementation). Чужие изменения,
  original/donor assets/JAR и пользовательские миры не откатывались. Новых
  commits/public push/release в продолжении нет.
- A07 перенёс11 byte-identical файлов из main в gameTest; loader test metadata
  и Neo test bootstrap отдельные. Общий production recovery сохранён. Fabric
  harnesses используют per-run classPathGroups, Neo — per-run MOD_CLASSES.
- `ResearchNoteEditing` +30JUnit scenarios — pure R08 planner, не native edit
  service. Независимый original review не нашёл существенных defects. Сохраняются
  literal ink/RNG, post-refund discoveries before solver и raw-int/short-query
  quirks; `poolChange != null/applied=false` всё равно требует source correction.
- Первый центральный `.tmp/a07-validate1.log`: exit1,191JUnit и41Fabric PASS,
  Neo до тестов — duplicate modules. Owned hung child JVM остановлена после
  thread dump. Исправление: не добавлять raw common.main рядом с Architectury
  transformed common JAR; новый gate проверяет emitted MOD_CLASSES.
- `a07-validate2` завершился exit0. Recorder исправлен: XML-suite берётся по
  объявленному Java package, не directory layout (пять существующих тестов
  намеренно package-private). Добавлена regression. Итоговый
  `.tmp/a07-validate3.log`/exit.json: exit0,191JUnit/41Fabric/41NeoForge,
  failures/errors/skips0 и все41 required IDs каждого loader. Python48PASS,
  converter/extractor/scope checks/self-test PASS. Scoped diff-check PASS;
  общий diff-check красный на прежнем legacy/donor whitespace — не исправлять.
- `docs/porting/verification.json` schema2 записан по
  `.tmp/a07-validate3-inputs.json`:2980 inputs unchanged, свежие hashes logs/XML/JAR.
  Оба remapped JAR очищены от test support/entrypoints/templates/references;
  normal/global/run scopes gate PASS. Старый client inspection явно historical.
- Ordinary dev server не запускался: guard не нашёл pre-existing accepted EULA;
  согласие не создавалось автоматически. Actual packaged runtime всё ещё open.
  Выполняется отдельный Fabric `.tmp/a07-client1.log`; его результата пока нет.
- [Обзор всех корпусов](analysis/2026-10-16-workspace-audit-ru.md),
  [source-bound verification](porting/VALIDATION.md). Inventory72main,
  33partial/901pending/0complete; scope1044needs_review/50counterparts.

Далее: проверить отдельный client harness runtime, затем native R08
bonus persistence/sync + shared revision + per-open session + staged preserving
transaction + bounded C2S + GUI. Общий Neo/Fabric client harness и ordinary
packaged launches всё ещё отдельная работа. Нижеследующее R07 evidence — история,
не подтверждение новых source/config изменений.

## Источники и границы

TC4 4.2.3.5 private oracle — единственная gameplay specification. Legacy `src/` и pinned OpenTC4 — read-only references; decompile/original JAR вне Git. Java21/Minecraft1.21.1/Fabric+NeoForge, свежие1.21.1 миры. Локальный Git разрешён; public push/release не авторизованы. Не подменять TC4 механиками TC5/TC6.

## Текущее evidence

- `.tmp/r07-validate7.log` и exit.json: exit0,161JUnit/41Fabric/41NeoForge GameTests; XML failures/errors/skips0, обязательные IDs присутствуют. Исходники всех source sets были frozen; после них выполнен client3 без production изменений.
- Runtime покрывает craft/place/conversion/topology/shared menus/distance/BE identity, count/components save, native unload/reload/cold-load repair и pending queue. Три новых cases: actual EntityEvent.ADD cancellation/retry с explicit early wake, callback-created replacement inventory и настоящий hidden UUID owner.
- Fabric отменяет ADD после UUID reservation, до section/callback attachment. Guarded RejectedEntitySpawn snapshot+discard+release проверяет exact attempt, known-before, callback NULL, world lookup и ВСЕ sections, не трогая чужого owner. Accessors зарегистрированы; NeoForge отменяет до reservation. Перезапуск не нужен для cleanup в живом мире.
- Per-output transient retryAfter сохраняет минимум20 ticks при новом enqueue; queue-wide cadence/batch64 могут задержать позже. New/reloaded queue entries готовы сразу, материальная SavedData version1 неизменна. Stable UUID/count/components сохраняются, не clamp/split.
- `.tmp/r07-assets3.log`:27PythonPASS; extractor/resource/notices проверки PASS,494sprites/69refs/66events/111OGG/missing0. Прямоугольные original table PNG сохранены; static atlas codecs/UV и GUI alpha исправлены.
- `.tmp/r07-client3.log`/exit.json: exit0/SCRIPT_PASSED, run `client-smoke-91bd518d-9a58-4451-addf-4e3becd205c8`. Root просмотрел все19 exactPNG; второй агент15–19 с matching hashes. Native place/conversion/shared menu/quick-move/drop, read-only grid/world/baked item contexts.
- Actual restart client2→client3 сравнил весь native Inventory NBT пары до setup. Source ID `client-smoke-b42fee38-3002-45f7-bf25-fa8984d74cd2`, expectation из stopped owned `.mca`. Source world не открывался; level.dat hash совпал. Owned world остаётся `fabric/run-clienttest/saves/book-smoke`; raw client2 архив `.tmp/r07-client2-raw-report.json`.
- Исторический `ac1734af:docs/porting/verification.json` и [client inspection](porting/client-smoke-verification.json) относятся к validate7/client3. Нынешний verification.json уже A07 schema2. НаR07 inventory81main/33partial/901pending/0complete; scope1044needs_review/49counterparts. Новые native mappings не являются semantic parity verdicts.

Открыто: NeoForge/packaged table client, полный remote multiplayer/human survival, R08 editing/R09 combine/ecology/R10copy и остальные M00–M15. Hand inkwell светлее world cup; full lighting/animation/audio/frustum parity не доказаны. RU long label и primary tooltip near edge — известные GUI ограничения. Crash-atomic multi-file inventory transaction не заявлена. GameTest helpers/entrypoints пока main; это следующая A07 работа, не release-ready JAR.

## Следующая работа

1. A07: common/src/gameTest и loader/src/gameTest для7 common helpers, обоих wrappers, Neo NetworkedGameTestPlayer и empty.nbt. Удалить main fabric-gametest entrypoint и unconditional Neo RegisterGameTestsEvent/reporter; не удалять production EntityInventoryItem/DeferredItemDrops/RejectedEntitySpawn/mixins.
2. Общий clientTest сценарий с тонкими Fabric/Neo adapters. Read-only исследование подтвердило native Neo ScreenEvent.Render.Post/RenderGuiEvent.Post и Loom run-scoped ModSettings.sourceSet(...). Не включать test mods глобально в normal runs. Сохранить41 exact cases и19 scene names; оба loaders actual runtime и normal packaged clean scan/launch.
3. R08: minimal shared bonus persistence/sync, Tile shared revision, Container per-open session, pure edit→native C2S/GUI. [Editing spec](analysis/2026-10-15-research-table-editing-spec.md) и ignored `.tmp/r08-draft` — проект, не production. Actor refund discovery идёт ДО solver; RP-first/bonus fallback, funding до R2 roll; exact source world RNG.
4. R08 pool boundary исследуется агентом: live underlying int, funding query `(short)raw`, negative debit зависит от raw>0; source persistence/full/incremental sync тоже short. Не принимать draft UNRESOLVED refusal/cap/clamp вместо original quirks. До natural copy R10 отдельно player/container/cursor/death-drop/save count100/127: table/entity adapters не исправляют Inventory.save codec99.

R07 spec сохраняет permissive TABLE topology, shared public table/no owner lock, discovery GUI vs automation distinction, distance²64 и exact BE identity. Constructor conversion принимает exhausted tool, не расходует ink; survival held−1, creative retains. Craft output1. Изучить оригинал перед следующей mutation; авторы не меняют общие registry/network/build параллельно без владельца.

## Команды и расположения

Repo: `C:/Users/Acak/Documents/Codex/2026-10-15/new-chat/Thaumcraft-4.2-FOREVA`.
JDK21: `../work/jdk21/jdk-21.0.12.1+1`; Python: `C:/Program Files/Python314/python.exe`.
Oracle: `../work/thaumcraft-4.2.3.5-oracle`; refs: `../work/references`; old vanilla evidence: `../work/vanilla-1.7.10-reference`. Всё read-only вне Git.

```powershell
./scripts/dev.ps1 -Task validate
./scripts/dev.ps1 -Task client-smoke
& 'C:/Program Files/Python314/python.exe' -B -m unittest discover -s scripts/porting -p 'test_*.py'
& 'C:/Program Files/Python314/python.exe' -B scripts/porting/convert_assets.py --verify
& 'C:/Program Files/Python314/python.exe' -B scripts/porting/extract_tc4_definitions.py --check
& 'C:/Program Files/Python314/python.exe' -B scripts/porting/update_migration_inventory.py
& 'C:/Program Files/Python314/python.exe' -B scripts/porting/update_scope_ledger.py --check
& 'C:/Program Files/Python314/python.exe' -B scripts/porting/update_scope_ledger.py --self-test
& 'C:/Program Files/Python314/python.exe' -B scripts/porting/record_verification.py --check-notices
```

Только один Gradle/Loom процесс. Перед validate/build заморозить ВСЕ source sets, включая clientTest. Owned client world — только `fabric/run-clienttest`; не трогать original `fabric/run/saves/New World`. Не менять пользовательские процессы/миры. Инвентарь → scope --write → --check; не повышать semantic/complete автоматически. Git identity command-local Codex<codex@localhost>.
