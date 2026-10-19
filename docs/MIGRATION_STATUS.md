# Состояние переноса на Minecraft 1.21.1

Контрольная точка:16.10.2026, локальная ветка `port/minecraft-1.21.1`, HEAD R07 implementation `ac1734af`; native R08/R09 и частичные M02–M05 в рабочем дереве. Проверенные записки R01–R05 — `03fe0782`; предыдущие checkpoints: книга `85e22827`, Secondary purchase `d89b669f`, полный план `6406d64d`; первоначальный FOREVA baseline — `4bc68454b1063cdc1255f833dd537e0fd4fc341d`.

**Это частичный нативный порт TC4 4.2.3.5, не полный Thaumcraft.** Старый `src/` остаётся справочником Forge 1.12 и не входит в современную сборку. Поддерживаются только новые миры Minecraft 1.21.1; импорт миров 1.7/1.12 не реализован.

**Новый приоритет пользователя16.10 — сначала весь Fabric.** NeoForge-разработка и
его build/runtime прогоны отложены до завершения Fabric-контента. Модуль не удалён;
последние61NeoForgePASS — только исторический Fire7, не будущие Tube-исходники.
Новые checkpoints явно фиксируют Fabric-only, без повторного использования старых
NeoForgeXML/JAR. Общая Architectury/common основа сохранена.

## Актуальный приоритет18.10: контент, предварительный экспорт бура

Пользователь остановил доработку smoke-тестера и разрешил экспорт текущего
Arcane Bore с переносом итоговой проверки на конец общего переноса. Production
сборка для этого экспорта выполнена отдельно от незаконченного голодного сундука:
JAR10841529bytes/SHA256 `f70b32f4c1698bf3247afd1bb1525f69cdc341e045ff605d4b3ed6db8196fa8f`,
`docs/porting/bore-export.json`; предыдущий focal пакет сохранён в backups.
Это не повышение native16–26 до PASS: полный stopped-save аудит бура не принят.
Предыдущий принятый focal checkpoint сохраняется резервной копией. Текущая
разработка продолжается на Fabric. **Голодный сундук интегрирован и собран**
(`hungry-chest-build1`,38s,6 aspect-data JUnit); GUI/подбор/воронки/save-reload/рендер
в игре ещё не проверены, в экспортированный Bore-only JAR он не включён.
**Соломенный голем + Gather** также интегрирован и собран (`straw-golem-build1`,
46s, production isolation), private JAR10945193bytes/SHA256
`5e43b5b9040a418684d4814e215e5669c89e81d94969e1f0ab81177cadfad9fa`.
Игровой/визуальный проход не выполнялся. Сейчас интегрированы материалы Wood/Tallow/
Clay/Stone/Iron/Thaumium и блоки таумия/магического жира; их первая сборка
`golem-materials-build1` PASS35s, private JAR10961197bytes/SHA256
`b7a4d3a1374e599f03e6f97f512802b9512ae19b73cb116edd744e4269d32916`.
Fill0/Empty1 с контейнерными маршрутами/метками/количествами также собраны:
`golem-inventory-build1` PASS39s, private JAR10996795bytes/SHA256
`d27892c805ecc5ae59482ca77a43f1d688a7442de5fea98a4f6304ba5af58f1a`.
Sorting10, directed Fill/Empty throws и настоящее получение/food zombie brain
интегрированы и собраны `golem-automation-build1` PASS42s; private JAR11007976bytes/
SHA256 `b3dc066f612a47615cef53a4d02c482d612e8487e47feec8742442d94271fb27`.
Harvest3 также интегрирован и собран `golem-harvest-build2` PASS42s,6 существующих
aspect-data JUnit; private JAR11022073bytes/SHA256
`1353e155960af3dadbc1f87b8c02244aea95b90d0adf41e7cfe252e19443ab79`.
Guard4 собран `golem-guard-build1` PASS42s,9 существующих data JUnit, actual Fabric
main/remap/isolation; private JAR11038264bytes/SHA256
`e50f60f769103945f108b37e6815fa8cc738d3706edbe6b85fe743b21257ed9d`.
Этот же compile включает initial infusion-input damage и Fishing loot helper,
не полную рыбалку. Fishing11/runtime/bobber/client затем интегрированы и собраны
`golem-fishing-build1` PASS41s, actual main/remap/isolation; private JAR11073129bytes/
SHA256 `013564631e37ca78b5aa689765c3a5f6b6654ab8d39b717ce932df487c1b4eee`.
Decanting5 затем подключён и собран `golem-decanting-build1` PASS36s,
actual main/remap/isolation; private JAR11094943bytes/SHA256
`8ba3f263bb313b01cc9c72f5e2621acc439effcefef18202dc38a9215812383c`.
Alchemy6 затем интегрирована и собрана `golem-alchemy-build1` PASS35s,11 existing
dataJUnit; actual main/remap/isolation, private JAR11123385bytes/SHA256
`17bfb3fe8d69f6c0156ecff6c4e13c263fd459e4067c3dca890f60e8b6e11c2e`.
Butcher9 также собран `golem-butcher-build1` PASS38s,11 existingdataJUnit,
actual main/remap/isolation; private JAR11139344bytes/SHA256
`a9df7c6ce1e180d468c63b286e8041cee6425fc7a2cf3e047b05e71469450e52`.
Use8 и материал Flesh затем интегрированы и собраны `golem-use-build1` PASS33s
(exit 0, main/remap/isolation). Настоящий Axe of the Stream (предмет, тир 1500/10/3/18,
researched infusion с pristine-центром, рубка конца ствола через вложенный
`destroyBlock`, удержание ПКМ как вакуум, плывущие дропы) собран `axe-stream-build1`
PASS1m6s: полный `:common:test` 302 JUnit без падений/пропусков (12 новых чистых),
Fabric JAR SHA256 `29baa84d5fd9c4539c4cda1561a9e10d75afbb5c96208242d4b4247e458b94e2`;
см. [реализацию](analysis/2026-10-19-axe-stream-implementation.md). Существующий
`:fabric:runGameTest` теперь 141/141 (`axe-stream-gametest4`): починен давний устаревший
помощник теста кодека инфузии и добавлен целевой GameTest топора (рубка конца ствола,
защита, износ, креатив, вакуум, следование дропов). Сетевой клиент, защитные моды и
визуальный проход не выполнялись. Затем интегрирован Lumber7 ([заметка](analysis/2026-10-19-golem-lumber-implementation.md)):
ядро 7, рецепт (Harvest-центр + Axe of the Stream + три железных топора, `meto`), `LumberGolemGoals`, `chop` платформы;
`lumber-build1` PASS41s, 309 JUnit; `lumber-gametest1` **142/142** с первым GameTest настоящего голема-работника (один верхний
блок срублен, одно бревно, BREAK один раз на конце от не-OP актора). Исправлен дефект рецепта Fishing11 (`messis`→`meto`).
Все 12 ядер оригинала теперь интегрированы; серия срубов, отказы, save/reload, визуал не проверялись.
Затем реализована первая группа улучшений големов — Air/Earth/Entropy ([реализация](analysis/2026-10-19-golem-upgrades-implementation.md),
[контракт всех шести](analysis/2026-10-19-golem-upgrades.md)): предметы+арканные рецепты, слоты по материалу, установка ПКМ, `GolemData.upgrades`/NBT, возврат колокольчиком;
Earth — сила и рукопашный урон, Air — скорость, Entropy — шипы. Fire/Water/Order не зарегистрированы (нет честного эффекта). `golem-upgrades-build2` PASS:
`:common:test` 332 JUnit без падений (+23), remap/isolation, JAR SHA256 `4EC1994A…0929`; `golem-upgrades-gametest2` **143/143** (новый GameTest на настоящем игроке и големе; две мутации его ловят).
Implemented/built/game-tested — да; seen (клиент/книга/рендер) — нет.
Вторая группа: вместимость Earth (`carry = base + min(16, max(4, base))·Earth`; вода `floor(sqrt)·1000`, потолки груза 8000 мБ / 64; плейсер, сущность, NBT, колокольчик, Use, рендер банки) и Water (`16 + 4n`, живой радиус вместо `final`-полей
Harvest/Lumber/Fishing и констант 256; предмет `golem_upgrade_water` с рецептом `UPGRADEWATER`). `golem-capacity-range-build2` PASS: `:common:test` **339 JUnit** без падений (+7), remap/isolation, JAR SHA256 `3f32d7aa…e688`;
`golem-capacity-range-gametest2` **144/144** (расширенный тест на настоящих игроке, големах, пауках + новый тест Lumber/Harvest-големов, чей радиус растёт после установки Water; пять мутаций A–E его ловят). Seen — нет;
настоящие циклы набора воды/эссенции на повышенной ёмкости и Fishing на живом радиусе не проверены.
Третья группа — Fire (поджог цели на `4n` с после удавшегося удара Guard/Butcher) и Order (пересадка Harvest: семена в девять клеток ниже убранной колонки, толчок свежих дропов, какао; платёж настоящим семенем, цепочка
разрешений Architectury `PLACE` + Fabric `UseBlockCallback`). Теперь поддержаны все шесть типов. `fire-order-final-build` PASS: `:common:test` **346 JUnit**, remap/isolation, JAR SHA256 `30e0c30f…f99e`;
`fire-order-final-gametest` **145/145** (новый тест: настоящий Guard с Fire и без Fire против паука; Harvest+Order на семенах/запретах/пшенице/какао; две мутации его ловят). Найден и исправлен дефект кода: в 1.21 свет клетки после
уборки непрозрачного блока (арбуз/тыква) устаревший и ломал посадку в этой клетке — `survives()` предсказывает свет по соседям, а посадка идёт в два прохода (подробности в [HANDOFF](HANDOFF.md)). Seen (клиент/книга/рендер) — нет.
Четвёртая группа — слоты `6+6n` (Fire: страницы по шесть призрачных слотов у Fill/Empty/Gather/Use, максимум 18; Liquid остаётся на одной странице), жарка улова Fishing (Fire; без визуального огня предмета) и цвета Order
(цвет маркера и цвет слота Fill/Empty/Use по `equalsFuzzy`, цикл колокольчика `любой → 0…15 → удалён`, кнопки цвета в меню). `colors-final-build` PASS: `:common:test` **368 JUnit** без падений (+22), remap/isolation, JAR SHA256 `badcf4a0…2554`;
`colors-final-gametest` **148/148** (три новых теста на настоящих игроке, големах, меню, колокольчике и реестре рецептов; настоящий Fill-голем берёт из сундука нужного цвета, а не из ближайшего; 7 JUnit-мутаций и 6 GameTest-мутаций ловятся). Найден и исправлен
дефект `GolemData.equals` (сравнивал только первую страницу и не видел цветов). Seen (клиент/GUI/action bar) — нет; рыбалка со снастью и живой маршрут Empty/Use по цвету не проверены (подробности в [HANDOFF](HANDOFF.md)).
Пятая группа — переключатели целей Guard (Order: Monsters/Animals/Players/Creepers, кнопки 51–54 как в оригинале; **умолчание безопасное** — только монстры, в оригинале у свежего Order-Guard бьются и животные, и игроки, и криперы) и улов Fishing
(Order: шанс сокровища `0,05 + 0,0125·n`; Entropy: junk `0,1 − 0,025·n`; Air: второй улов при `nextInt(10) < n`). `guard-fish-final-build` PASS: `:common:test` **384 JUnit** без падений (+16), remap/isolation, JAR SHA256 `f1112abe…d6b5`;
`guard-fish-final-gametest` **151/151** (три новых теста: решение для 16 масок × 12 видов существ, меню 51–54, NBT/плейсер/колокольчик и четыре настоящих Guard, которые идут и бьют; внедряемые случайные числа для окон улова; первая живая рыбалка 24 големов в тестах проекта;
12 JUnit-тестов и 10 из 11 GameTest-мутаций ловят правки, одна — нет, см. [реализацию](analysis/2026-10-19-golem-upgrades-implementation.md)). Seen (клиент/GUI) — нет; реальные protection-моды, сеть и save/reload мира не проверены.
Шестая группа — продвинутый голем (19.10, [контракт](analysis/2026-10-19-golem-advanced.md)): +1 слот улучшений, скорость ×1,1, радиус +20 % (не менее 2), задержка атаки 18; плейсер/колокольчик/NBT несут флаг;
инфузия получила именованную правку центра (`result_patch`), рецепт `golem_advanced` (центр — любой из 8 плейсеров, исследование `ADVANCEDGOLEM`). Независимая проверка в чистом контейнере Linux: `:common:test` **393 JUnit** без падений,
`:fabric:remapJar` + `:fabric:runGameTest` **153/153**; десять мутаций ловятся (избыточная защита `matches` — только после добавления прямого теста). Seen (клиент/значок плейсера/вид голема) — нет; полный ритуал алтаря с этим рецептом не запускался.
Седьмая группа — аксессуары голема H G B F V P M (19.10, [контракт и реализация](analysis/2026-10-19-golem-decorations.md)): семь предметов с аркан-рецептами, установка ПКМ (повтор и пары H↔F, G↔V, B↔P отказывают без платежа),
+5 здоровья, +10 % радиуса, ×1,1 / ×0,88 скорости, ремонт ×0,66, броня +1 / +4, +2 урона, забрало метит жертву (опыт), NBT `Decoration`, сохранение колокольчиком; проверка в чистом контейнере: `:common:test` **403 JUnit**,
`:fabric:runGameTest` **154/154** (в т. ч. настоящий Guard убивает паука с забралом — 5 опыта, без — 0; ремонт феской 50 против 76 тиков), 25 мутаций ловятся. Seen — нет; дротик `R` не перенесён.
Остаются горящий предмет улова, остальное Entropy (нечёткое сравнение — [контракт](analysis/2026-10-19-golem-item-match.md), очередь Liquid), дротик, Traveling Trunk и прочие M10-элементы; полный M10 не закрыт. NeoForge отложен.

## Предыдущий этап: Arcane Bore, Fabric-only (история до смены приоритета)

**Историческое состояние:** рабочая волна W4, часть M09. Последний принятый экспорт
по-прежнему focal manipulator; полного M09 или всего Fabric ещё нет. Бур в native16
действительно получен/установлен/дважды добыл блоки, но сохранение ОТКЛОНЕНО из-за
реальной потери унаследованной loose Potentia8-банки. Native16 сохранён отдельно,
никогда не вход повторной попытки; экспорт не выполнен. User разрешил ОДИН
custody-only выезд <=200native/14wall с временными честными oldjar coverage gaps
и восстановлением20/333/72 до продолжения. Сейчас реализуется/проверяется реальный
подбор исходного UUID, неизменный слот7 того же запланированного сундука, защита
всех7 физических старых предметов и source-linked учёт естественного skeletal loot.
Сырьё/XP/возраст/vis/исследования не восстанавливаются и не выдаются сверх прежних
конечных fixtures. Новый клиент ещё НЕ запущен. Точное резюме и текущие source-only
авторы/блокеры — в начале HANDOFF.md; нижняя хронология сохранена как история.

Сразу после принятия/экспорта focal checkpoint начата следующая группа: Arcane Bore.
Original/pinned OpenTC4 audit выявил prerequisite: явное pristine-tool условие
компонентов инфузии должно сохраняться в codec/network/session и при последующем
потреблении. Нельзя принимать worn diamond tools или выдавать один JSON за готовый
бур. Машинная авторизация добычи не должна ослаблять held Excavation.

Подготовительные части реально прошли `bore-prereq-validate3`:
**284JUnit/116Fabric/74Python103.798s**,3873unchangedinputs. Проверены сохранение
damage predicates, математика, detached bundle planner и настоящая перегонка
четырёх поршней68units(Motus16) со всем collateral. Development JARc8b756ba...
не экспортирован и не является игровым буром.

Сейчас в исходниках интегрируются registry/исходные рецепты/книга/поворот жезлом,
native-origin/lifecycle guards, защищённая добыча/сохранение output и GUI/BER.
Шесть runtime-классов frozen в ignored staging; до сведения всех owners новые
исходники не скомпилированы. Первый scope — известные обычные nonBEblocks и
настоящий durable выброс; targetBE/существующий loose-item vacuum и контейнерная
маршрутизация пока pending, не имитация успешного original поведения. Новые
`bore-runtime-gametest8` действительно прошёл **129/129 required Fabric**:
настоящая добыча алмаза деревянной киркой, износ/выброс без XP, меню, рецепты,
сохранение, повреждённая очередь и эвакуация. Найденная ложная блокировка обычного
nonBE lookup исправлена без отключения наблюдения за реальными мутациями.
После этого `bore-runtime-validate11` прошёл полный **284JUnit/138Fabric/76Python
96.361s**,0failures/errors/skips,3920unchangedinputs. Source-bound development
JAR10,841,529bytes/f70b32f4... проверен и не экспортирован; архив
`.tmp/bore-runtime-validated11/`. Все девять новых сценариев защиты/исключений
реально прошли: запреты/ABA/удаление top в callback/AFTER throw/native false+throw,
точная сохранность wear/credit/output без повторной выдачи или возврата оплаты.
Это не проверка всех partial-body fault/Error/restart исходов.
Клиентская подготовка выявила реальный acquisition gate: у принятого Player653
нет PRIMARY `ARCANEBORE`. Готовится отдельный ограниченный этап настоящего
сканирования/соединения аспектов и решения выданной записки; флаг, RP или готовая
записка не выдаются. Материалы/топливо/три alembic/11банок и настоящие placement
anchors для самого бура рассчитаны, но это ещё не разрешение запуска.
Конечный маршрут первичного исследования теперь рассчитан; detached-планировщик
скомпилирован и прошёл32синтетические сетки в двух ресурсных бюджетах +8проверок
границ. Это не исследование живого игрока: клиентский сценарий и passive ledger
теперь подключены и скомпилированы (`bore-client-compile2`,3935unchangedinputs),
но ещё не выполнены. Точные профиль Player653 и10native hook metadata проверены
без копирования/запуска мира. Перед игрой идут отдельные ревью действий и учёта
ресурсов/загрузки/объёма отчёта; compile не является native/pixel приёмкой.
После ревью исправлены снятие метки с занятой offhand, GUI-cooldown комбинаций и
запрет отложенных собственных fixture-действий после сбоя. Native-regression
снятия метки реально прошёл вместе со старыми: **139/139 Fabric**; свежая
client-profile компиляция3 также прошла(3936inputs). Игра ещё не запускалась.
Обновление: после исправлений ledger выполнен полный **284JUnit/139Fabric/
76Python104.493s**,0fail/errors/skips,3942unchangedinputs; архив
`.tmp/bore-client-validated13/`. Первый настоящий Bore client1 теперь запущен
на свежей копии принятого focal-save с конечными сырьевыми бюджетами и настоящим
исследованием. Результата/PNG/stopped-аудита ещё нет; экспорт не разрешён.
Client1 реально остановился до выдачи сырья/записки на слишком ранней проверке
загрузки. Остановленный Inventory/XP/knowledge совпал с parent; неудачный мир
сохранён отдельно и не используется повторно. Добавлено ограниченное ожидание
реально загруженных20/333/72 без chunk requests, код скомпилирован. Client2 теперь
выполняется на новой копии принятого focal; успех ещё не заявлен.
Client2 действительно дождался20/333/72 и остановился до сырья/записки на
сравнении chunk envelope с live BE tag. Native-regression доказал различие
keepPacked=false; исправлено только это служебное поле сравниваемой копии.
**140/140 Fabric** прошли, также compile и6граничных metadata checks. Client3
запущен на новой accepted-focal копии; ни одна записка пока не выдавалась, PNG/
stopped-приёмки и экспорта нет.
Client3 выявил неверное требование !Entity.Invulnerable: у принятого Player653
исходно флаг1, при abilities.invulnerable0/Survival0/Health20. Флаги не менялись;
observer теперь проверяет их точное сохранение и явно сообщает ограничение
survival-damage проверки. Compile/detached checks прошли; Client4 выполняется на
новой accepted-focal копии. Предыдущие попытки не выдавали ни сырья, ни записок.
Client4 тоже остановился до сырья/записки: persistent unnamed pig с обычным AI
далеко от участка естественно изменял координаты, а observer требовал их
неизменности. Health10 и все флаги сохранились. Готовится строгий native-witness
движения только таких мобов вне участка; здоровье/identity остаются защищены,
а all11 NoAI-коровы по-прежнему обязаны сохранять точные позиции. Сейчас игры нет.
Client6 впервые действительно выполнил исследование:9scan/13combines/ONEissued
note/9paid edits/ink10/learning и скрафтил base с900Aer+900Ordo. NBT подтвердил
ARCANEBORE и физический base;5nativePNG174..178 открыты/осмотрены. Затем fail на
камере x1613.5 без пола; сохранённый пол начинается x1614. Исправлены все7точек
подхода без выдачи пола/материалов, compile прошёл. Client7 запущен на новой копии
принятого focal и обязан повторить настоящую acquisition, не копируя флаг/записку
из failed6. Установленного бура/двух добыч/полной PNG+stopped-приёмки ещё нет.
Client7 действительно установил base/foundation/scaffold и затем остановился на
vanilla checkDespawn незащищённого паука в широком research-buffer. Признание
такого despawn теперь требует точного native-стека и отсутствия пересечения
конкретных защищённых ячеек/игрока+6; all11 и прочие защищённые сущности не
ослаблены. Compile/16052motion/12geometry checks прошли; Client8 выполняется на
свежей принятой копии, без переноса результата/флага из неудачных миров.
Client8 остановился на реальной потере активности окна в меню scanner-craft,
до записки. Добавлен ограниченный возврат фокуса только своему HWND перед
следующим menu intent, без повтора пакетов/продления дедлайнов; активные scan/
mining/Shift потери остаются FAIL. Compile/pure checks прошли; Client9 теперь
выполняется. Ни complete Bore client, ни экспорт пока не приняты.
Client9 дошёл до полной раскладки компонентов и рычага, затем поймал ошибку
порядка observer-проверок при настоящей выгрузке/повторной загрузке дальней
NoAI-коровы. Регистрация фактического UUID-владельца теперь идёт ДО проверок POST,
по-прежнему требуя реальную прежнюю выгрузку и точные защищённые свойства.
Текущий код скомпилирован(3948inputs), pure16052motion/12geometry прошли; Client10
выполняется. Независимый аудитор пока проверял только синтетические данные.
Сохраняется исходный риск несвязной записки; один фактический результат генерации
нельзя подменять, пересоздавать до удачи или объявлять решённым планировщиком.
Приёмки/клиентского
сценария бура ещё нет; в distribution по-прежнему focal0104dc...,191файл его
принятого parent неизменён. NeoForge остаётся отложен.

## Последний проверенный срез: focal manipulator, bounded M08, Fabric-only

Принят и экспортирован `focal-fabric-validate7` / `focal-fabric-client14`:
**238 JUnit /113 required Fabric /74 Python88.423s**,0failures/errors/skips,
**3865 unchanged inputs**. [Клиент](porting/focal-client.json),
[verification](porting/verification.json), [контракт](analysis/2026-10-17-focal-manipulator.md).
Архив `.tmp/focal-accepted-final7/`; currentmatch=true/historical=false при записи.

Действительные крафты Charm2250 и манипулятора2880 каждого primal CV,
112native smelts/16coal→112XP, один8LEVEL Potency job/2400rawCV, protected13terra
harvest/sameUUID pickup. Итог108gold, OLD3plusNEW1cobble4; payer10/focus15 и pick3→5
восстановлены сервером. Все10exactPNG164–173 осмотрены отдельно от native evidence.
Frugal/Potency работают для всех4фокусов, Excavation Treasure1/3/5 и Enlarge2/4.
Пассивный Charm/Silk/Dowsing/прочие specializations не объявлены завершёнными и не
разрешены к оплате. Bound job нельзя переносить на другой предмет; неоднозначный
delegated Enlarge false/throw сохраняет reservation и прекращает expansion.

Независимый stopped-save audit:5658contiguousPOSTs29–5686, все11named/NoAIactors
точны (включая3cowhealth6); одно старое ambient skeleton retirement подтверждено
настоящим Mob.checkDespawn вне ownedcells. Все197исходныхUUID проверены,27late-Mob
observations учтены.20jars333units/72candles/oldB0furnaces/прочиеBEs/инвентарь и
пустые queues сохранены. Старый1551узел естественно прошёл4prunes1200..4800:
BASE{aer76,aqua76,ignis76,ordo78,perditio76,terra77},CURRENTterra1/ordo2,
NORMAL/NONE. Это наблюдение, не reset/freeze/BASE80 waiver. Parent187files неизменны.
Нативный offline-profile Player653/UUID отдельно bound к login/level/playerdata;
все64старыхprofilefiles сохранены. Auto-focus/Shift test-only не входят в JAR.

Экспорт **10,641,110bytes**, SHA256
`0104dc3756199192b56af2956a6f544702b2c6bb5848fc725d96ba4f051e3f10`;
M09b сохранён в `backups/visnet-fabric-validate7/`, зависимости/старые backups
не менялись. ZIP/notices/isolation/oneintermediaryAW/recipes/art проверены.
Нормальная packaged/EULA-server установка, remote multiplayer/full survival,
audio/full motion/API/full TC4/NeoForge остаются непроверенными. Новый accepted
stopped parent: `fabric/run-clienttest/saves/focal-smoke-8fd11823-4346-48f9-97ed-e06dbf9ec4be`.

## Предыдущий проверенный срез: M09b настоящий VisNet, Fabric-only

Принят и экспортирован `visnet-fabric-validate7` / `visnet-fabric-client5`:
**220 JUnit / 105 Fabric (все required) / 72 Python (95.133с)**, без failures,
errors или skips, **3827 неизменённых входов**; финальная полная проверка после
клиента. [Клиент](porting/visnet-client.json), [verification](porting/verification.json),
архив `.tmp/visnet-accepted-verification7.json`, [контракт](analysis/2026-10-17-visnet.md).
При записи client binding current=true/historical=false.

Настоящие transducer/energized source/relay graph, raw CV per-source-tick budget
и Terra-ускорение кристаллизатора. Исходные MAX reduction, 1001 warmup / 951 cooldown,
zero first source pulse, возврат CURRENT0 и ordinary zero-key pruning сохранены.
Native fixtures также проверили malformed relay quarantine/reload, lifecycle fences,
исключительные 9 quanta flux, Nausea вместо Wither и goo tail; это не full taint/fluid API.

Клиент прошёл за **255.1941779с / 5103 client ticks / 5080 ledger POST**; root осмотрел
все **10 PNG154–163**. Три настоящие оплаты, две зарядки от другого узла, genuine
mining/pickup/reinstall прежнего кристаллизатора и четыре новых физических кристалла:
**3×60 tick / 94 raw EARTH**, затем **1×1000 tick / 0 CV**, итоговый расход **282 CV**.
Дальность source→consumer²197 исключает прямое питание; relay пути²49/50.
Никаких vis/XP/essentia/clock/RNG/finished-output grants. Объявленные исходные
ингредиенты/primary VISPOWER и сохранённые inputs не выдаются за новое получение.

Независимый хвост до нормальной остановки: returned count**1091<1200**; stopped NBT
подтвердил **BASE80/NORMAL/NONE/шесть нулевых current keys**. **95 non-node BEs,
20 jars / 333 units / 72 candles**, старые 89 BEs сохранены кроме настоящей релокации
машины и естественного давления пустого клапана; старые furnaces B0, все wands/foci/
gear/phials/apple/XP11, прежняя именованная NoAI cow/health6/position сохранены.
Очереди пусты, все **183 файла принятого parent** неизменны. Сохранение —
`fabric/run-clienttest/saves/visnet-smoke-e0a10f37-f378-4c8a-8b43-6b69afc167fd`.

Экспорт **10,556,826 bytes**, SHA256
`bac83fff54504682f7a00523d22f10e00a334eb922f559f93ea27bf349408caa`;
M09a сохранён в `backups/stabilizer-fabric-validate2/`, прежние backups/dependencies
не изменены. ZIP/notices/test isolation/one intermediary AW/recipes/resources проверены.
Полные special nodes/advanced tier/other consumers/addon API, full chunk unload,
cross-file power-loss recovery, ordinary packaged/EULA-server/remote/full survival,
audio/full visual parity и NeoForge не заявлены. Дальнейшее естественное ticking
нулевого ordinary node может достигнуть исходной pruning boundary1200.

## Предыдущий проверенный срез: M09a базовый стабилизатор обычного узла, Fabric-only

`stabilizer-fabric-validate2` finalFULLafterclient **211JUnit/95Fabric/all95required/
68Python87.391s/3782unchangedinputs**,0failures/errors/skips. `client1` SCRIPT_PASSED
**139.7644009s/2795clientticks/2772SERVER_POSTchecks**; all8exactPNG146–153inspected.
[Client](porting/stabilizer-client.json), [verification](porting/verification.json),
archive `.tmp/stabilizer-accepted-verification2.json`; currentmatch=true/historical=false
atrecording. [Контракт](analysis/2026-10-17-stabilizer.md). MainJava305not%.

Ordinary NORMAL node: original cachedlock1/firstand50polls, bothdischarge directions
protected/exactRNGsites, interval×2/600→1200/BRIGHT400→800/PALE900→1800, nativeplayer
drain unaffected. Original FADING→PALE cached0 quirk PRESERVED, not unauthorized
nodeChange reset; tests prove realpowercycle1850/1900 +refill3600. Stocklessdevice
FULLcube/clientonly37animation/noGUI/plainownloot, native2highbubble with authored
minimal low-alpha shader and original32pxtextures/lock28+piston12OBJ. Node current-
identity/serverthread/reentry/loadnotification fences; unknownpower area noforce-load.

ONEpaidrecipe **2880AQ/2880TERRA/2880ORDO** fromactualGOLDcap1F +savedworn10%gear,
not intrinsicgold discount. Nativefixtures also baregold3200/gogglesgold3040/
THAUMIUMfullgear2559 confirmed. Realnode1623 charge73wholeunits→2900AQ/3000ORDO/
1350TERRA(capround50TERRA), lockedtarget1551gave4ORDO→400centivis. No resource/
vis/XP/clock/weather/RNG/timer/output/node grants/moves. Fullnativeepoch counts
1200→1800→2400: no600regain, exactly1ORDO77→78 after1200ticks. Nativecachepower
stale40POST/unpowered76POST/5lockchanges independentCLIENT37opening/closing.
Actual heldmine/woodpickdamage1→2/UUIDdb205c54contactpickup/reinstall under SAME
node; old4typedcrystals/two8phials/3wands/foci/gear/apple/XP11 retained. Rawinputs,
ALREADYCRAFTEDNitor1/bricks2/primaryNODESTABILIZER completion/bench/4torches/camera
declared; acquisition not replayed. Gold7final5000AER/20AQ/5000IGNIS/1010ORDO/
5000PD/2120TERRA.94noderows/77actualtaps/6ownershiprows/8clientanimationrows.

Independent stoppedNBT **89non-nodeBE/20jars333/72candles**, old87BE/allB0furnaces/
inventories/aspects exact, empty1882valve naturalpressure63→127 ONLYexception.
Newemptybench1906,216,377; plainbasicstabilizer1551,217,377 onrealvanillasupport
1551,216,377, realwalllever1551,216,378SOUTH/OFF. Savednode1551base80/allcurrent80
exceptORDO78, node1623AER80/AQ53/IGNIS80/ORDO52/PD80/TERRA66. Actualclosing28ticks
no regain. Pending/Quarantined empty, onlydeclaredNODESTABILIZER knowledge added.
Item/mesh partialocclusion/rain/nodeglare/GUIlimits recorded; still150Ordo79 early
tap NOTepochstart77, so pair150/151 not a visual+1claim. Stills notaudio/fullparity.

Export **10,409,707bytes**, SHA256
`ebf040e7654e34c6d3ee2d3c7abc5a2750282cf37c04f4deb42c7189fe3d9f7b`;
previousM06e`backups/crystallizer-fabric-validate2/`, deps/olderbackups intact.
ZIP/notices/isolation/oneintermediaryAW/exactrecipe+originalart+twoauthoredshaderfiles
verified. **VisNet/energized/relay/acceleration/specialnodes/advancedtier/fullAPI/
chunkunload/crossfilepowerloss/packaged/EULA/remote/fullsurvival/NeoForge/fullTC4**
notclaimed. [Knownfluxfindings](analysis/2026-10-17-flux-overfull-prep.md) stillunfixed:
Nausea-vs-Wither/exceptional9/tail; nextM09b explicitlyowns these beforetruepower.

## Предыдущий проверенный срез: M06e базовая кристаллизация, Fabric-only

`crystallizer-fabric-validate2` финальный FULL после клиента: **210JUnit/92Fabric/
все92required/64Python83.006с/3767неизменённыхвходов**,0failures/errors/skips.
`crystallizer-fabric-client1` SCRIPT_PASSED **281.2714389с/5626clientticks**,
все8exactPNG138–145 осмотрены; [client](porting/crystallizer-client.json),
[verification](porting/verification.json), archive `.tmp/crystallizer-accepted-verification2.json`,
[контракт](analysis/2026-10-17-crystallizer.md). При записи currentmatch=true /
historical=false. MainJava300 — не процент полноты.

Отдельная машинаFULLcube/placementopposite,1aspect slot, исходные rawface suction
quirks, +1/5tick native baseline/count continues powered, freshfill5/output1005,
pendingload progress0/output1000. Полный orderedpositive item API/strict immutable
data+codecs/typed1factory, EMPTYbase+actualcontents once, unknown tooltip, produced
scan20/distillation проверены. Shared atomic executor/paidROUTINGescrow до callbacks,
exact chest/hopper/furnace native adapters/strictcomponents/doublechest asymmetry/
cooldown8 и in-flightSAVE/stableUUID/cancel/throw/retry/dirtyfences покрыты runtime.
Unsupported inventory/setter не вызывается с последующим catch-and-drop. Whole
invalid payload quarantine preserved; receipts retained, compaction не реализована.

Actual7paidcrafts **3150AQ/3150ORDO/1350TERRA**, normalnode1551 UUIDf99565f9 charged
gold7 from50AQ/40ORDO through92real events; gains450AER/3100AQ/3600ORDO/450PD/1440TERRA,
0IGNIS/0sessionregen, capround50AER/50PD/60TERRA. Finalgold
`{aer:5000,aqua:0,ignis:5000,ordo:490,perditio:5000,terra:3650}`. OneRAWcoal+oneFUEL
→IGNIS2/POT2→4typedcrystals; full200steps each, actual1090/1000/1000/1000ticks,
firstpositiveprogress56pause45POST count523→567. FirstIGNIS2 stack paid into real
savedchest/retrieved, then same mined machineUUID27a7c374 physically recovered and
re-placedWEST/facingEAST. POT2 actualWESTdrops UUIDb0c50bbe/189db4fe pickedup separately,
originalADDgeometry1880.85,218.5,377.5/motionWEST.04 verified. No item moves/refunds/
resource/vis/XP/RNG/clock/weather/progress/output grants.5602POST/19hops/67balances/
2actualresonator inspections/4fullcycles/no loss. Raw fixtures,2ALREADYCRAFTEDbalanced
ingredients and primaryESSENTIACRYSTAL completion declared; acquisition not replayed.

Independent stoppedNBT **87non-nodeBE/20jars333/72candles**, allold81BE+TCcells exact
except paid1850Burn495→0; oldoriginal1465fuel3/B0 exact. Both savedphials/all3wands/
foci/armor/apple/XP11 retained; loosebasic2/DROP4/filter0/emptyphials2/blank1,
newwoodpickdamage1/twotypedstacks2+2, savedchestinventory→world. Newfurnace1882B0/
aspects/items empty. New valve suction report127→stopped63 is natural next empty
machine poll, stock0/flow1/all6open; do not freeze transient pressure/count. Node1551
stoppedAER75/AQ49/IGNIS80/ORDO44/PD75/TERRA65, base80; untouched1623 naturally
regenerated to80each, no sessiondebit. SavedDataV2Pending/Quarantined empty.

Recipe/model/art/ZIP/notices/testisolation/singleintermediaryAW checked. Export
**10,385,075bytes**, SHA256
`806a7cadeb04f6106bba46b612f466c984dd18449e5149696e421224f7d86c75`;
previouscentrifuge in`backups/centrifuge-fabric-validate3/`, olderbackups/deps unchanged.
Rain/night/GUIoverlap/partialmodel occlusion recorded;142/145tooltip is OLDIGNIS8
phial, notcrystal. Stillframes not full animation/audio/FX proof. **RealVisNet/EARTH
acceleration/fullM06/M09/API/addonI/O/chunkunload/crossfilepowerloss/ordinarypackaged/
EULA/remote/fullsurvival/fullTC4/NeoForge** remain open. Next bounded group begins
immediately: basicnode stabilizer with genuine ordinary-node integration, notfullpower.

## Предыдущий проверенный срез: M06d конструкция/центрифуга, Fabric-only

`centrifuge-fabric-validate3` FULL exit0: **204 JUnit / 88 Fabric / все88required**,
0 failures/errors/skips, **3742 неизменённых входа / 61 Python (87.619с)**.
`centrifuge-fabric-client2` SCRIPT_PASSED **130.199447с / 2608clientticks**,
все8exactPNG130–137 осмотрены; [client](porting/centrifuge-client.json),
[verification](porting/verification.json), archive `.tmp/centrifuge-accepted-verification3.json`,
[контракт](analysis/2026-10-17-centrifuge.md). При записи currentmatch=true /
historical=false. MainJava286 — не процент полноты.

Plain construct с исходным fullcube/noBE/noGUI и настоящей добычей; отдельная
IN1/OUT1 центрифуга:39→38, питание/пауза, blockedOUT/reload0, один непосредственный
компонент, исходные rawface quirks, authority hardening/quarantine. SAMEatomic
executor, callbacks/reentry/replacement/dirtyfailure/labelretirement проверены.
11 платных крафтов **4950AQ/4050ORDO/450AER/1440TERRA/450PD**, реальная конечная
зарядка gold7 из saved NORMALnode:100events, gains1150AER/2200AQ/4000ORDO/570PD/
1980TERRA,0IGNIS,0sessionregen; native caprounding50AER/30PD/20TERRA. Без vis/XP/
RNG/clock/эссенции/готовых outputs/newresearch grants. RAWcoal4 + FUELcoal1 дали
IGNIS8 в реальную новую фиалу и8POT→**3IGNIS+5ORDO** в банках; распределение
нативное случайное, не обязательное4+4. 2588POSTchecks/66transferrows/68balances,
same-server mixed8/all6public totals, positiveprocess29pause45ticks/primalrefusal46,
4inspections/4indexedactions. КонструкцияUUID27723fde и blankUUIDe4150043 физически
подобраны без перемещения предмета/возвратных grants. Обычная binding-фиала заняла
пустойpayer7; реальный PICKUPswap сохранил payer7 и фиалу13. Новая regression это
воспроизводит; первый failedsave не переиспользовался.

Gold7 final `{aer:4550,aqua:50,ignis:5000,ordo:40,perditio:4550,terra:3560}`;
mainFrost/все3жезла/фокусы/gear/knowledge/XP11 сохранены. Basics2/DROP5/filter0/
emptyphials2/blank1, savedPOT8+earnedIGNIS8 exact. Старые72BE/18jars325/72candles
сохранены; independent stoppedNBT **81non-nodeBE/20jars333** (9новыхBE).
Newfurnace1850 stoppedBurn495 только естественно догорает, inventories/aspects empty.
Ночь/дождь/перекрытие стекла/малые GUI-цифры отмечены; stills не доказывают audio,
всю анимацию/полную visual parity, ordinarypackaged/EULA/remote/fullsurvival/API/
полныйTC4/NeoForge. ZIP/notices/testisolation/единственныйintermediaryAW проверены.
Экспорт **10,310,076bytes**, SHA256
`8f13de6dce8f87f0c50381c515f05f7322638893ebec652e887424ada62195ec`;
предыдущий buffer в `backups/buffer-fabric-validate3/`, зависимости и более ранние
backups не изменены. Следующая bounded unit — базовый кристаллизатор, отдельно от
realVisNet acceleration; полный M06/M09 этим не закрыт.

## Предыдущий проверенный срез: M06c буфер эссенции, Fabric-only

`buffer-fabric-validate3` FULLexit0: **204JUnit/85Fabric/all85required**,0failures/
errors/skips, **3726unchangedinputs/58Python86.897s**, oneproductionJAR ZIP/notices/
testisolation/singleintermediaryAW/exactrecipe/modelPASS. `buffer-fabric-client3`
SCRIPT_PASSED77.7119747s/1553clientticks, **все8exactPNG122–129 осмотрены**;
[client](porting/buffer-client.json), [verification](porting/verification.json),
archive `.tmp/buffer-accepted-verification3.json`, [контракт](analysis/2026-10-17-buffer-centrifuge.md).
При записи currentmatch=true/historical=false. MainJava283 — не процент полноты.

Actualmixedbuffer8TOTAL/orderedstock/publicamount/all6ports, oneatomicexecutor+
immutablefullsnap/defaultsingleadapters, alloriginalquerysites/noexecutorRNG,
closedreceiverfill/firstselectedfailedSTOP, real6bellows/chokes20tickcache/5tickfill,
prioritytypedORuntyped/negative-nullguards/malformedquarantine/loadStatic и
reentry/notificationfailure/oldpaidfilterlocks покрытыnativecases. Строгие разные
публичныеконтракты IAspectContainer и rawtransport не заменены genericstorage.

Client использует gold7отsavedNORMALnode, gained900AER/1400AQ/700IGNIS/900ORDO/
1200PD/900TERRA,0sessionregen/capround,60chargeevents.3paidcrafts1350AQ/900ORDO/
1440TERRA +ordinaryphials8+nativebinding; saved3+earned8−4=7emptyphials, saved
POTENTIA8phialunchangedinventoryFIRST.4actualsavedbasics consumedrecipes/fifth
connector, nofinisheddevices/vis/essentia/XP/RNG/clockgrants. Actual2RAW+2FUEL→
4IGNIS+4POTENTIA8; fullsame-serverpublic8snapshot,9nativeindexedwandactions,
32→1→0→32chokes/20tickpoweredcache→1/strongtyped64vsconnector31fromuntyped32,
5realresonatorcalls,ONEphysicalblankUUIDb054b183returned/pickedup;1538POSTchecks/
34transferrows/35balances/0loss. Finalgold `{aer:3850,aqua:2800,ignis:5000,ordo:90,perditio:4430,terra:3020}`.
62priorBE/16jars317/72candles/3wands/allfoci/gear/XP11/knowledge retained; only
CENTRIFUGEcompletion declarednewfixture. Stoppedoutput72BE/18jars325 independently
readback fromnativeNBT. Actualsavednode/currentvis not predetermined grants.

Client1FAILED pickupcamera searchedcardinalsonly, originalfulljarcollision untouched.
Client2SCRIPT_PASSED but notaccepted: evidence mixedserver8withdelayedclient7;
root strengthenedONLYobserver tosame-server+all6port-equalityEVERYPOST. Client3
freshacceptedparentcopy; failed/unacceptedworlds never reused. All8inspected,
darkblue/red125/126 confirmednearestpixelcrops; visibleheldbuffer/jars/blank/chat,
smallGUIoverlap/night/glasslimits remain. FullGUI/item/light/FX/audio parity,
audio listening, ordinarypackaged/EULA/remote/fullsurvival/API/fullTC4/NeoForge
notclaimed. Boundedexport updated10,286,864bytes SHA256
`c3da2bf7be141c42513e440a989ca1282aa23137ee9d122bda84090429070ef1`; priorbinary
`backups/tubefilter-fabric-validate3/`, earlierbackups retainedoutside mods.
Следующий M06d alchemicalconstruct/centrifuge начинается сразу, без ожидания.

## Предыдущий проверенный срез: filter/restrict/oneway, Fabric-only

`tubefilter-fabric-validate3` FULLexit0: **203JUnit/82Fabric/all82required**,0failures/
errors/skips, **3714unchangedinputs/56Python**, oneproductionJAR ZIP/notices/
testisolation/единственный remappedAW проверены. `tubefilter-fabric-client3` exit0/
SCRIPT_PASSED, **все8exactPNG114–121 осмотрены**,112.0367s/2244clientticks.
[Контракт/история](analysis/2026-10-16-tube-filters.md),
[client](porting/tubefilter-client.json), [verification](porting/verification.json),
archive `.tmp/tubefilter-accepted-verification3.json`; при записи sourceboundmatch=true/
historical=false. MainJava279 — не процент полноты. NeoForge не запускался.

Один capacity1 engine, block-derivedkind; filter-only IAspectContainer marker:-1
не является хранилищем. Typed/untyped pressure, restrict sequential64→32/35→17,
oneway pressurefacing.opposite/pullscanисключаеттолькоэтусторону и raw6ports
проверены. Только boundlabel оплачиваетfilter, дажеcreative; снятие/breakclear
ownership доодной durableblankreturn; canceled/throwingADD/reentry/replacement/
native load/update/plain own drops и старые basic/valve кейсы проходят.

Client реально зарядил gold7изсуществующего NORMALnodeUUID9bbeaa03: все6primalgains/
sourceunits/IGNISregen1/caproundAQ60centivis учтены, directvis/nodewrites0.
Fivepaidcrafts2250AQ/3060ORDO/1440TERRA/720PD плюс zero-vis binding. Actualsaved
POTENTIA8phial вернулась неизменной вinventoryFIRST. Реальный4RAW+1FUEL→8+8;
верхнийPOTENTIA routed viafilter/restrict/oneway/earnedconnector→jar8, lowerIGNIS8
сохранён. Wrongdirection50ticks upstream0; wandUP исправил30→15→14suction;
2actualresonatorcalls, label UUID2e9b811e physicallyreturned/pickedup.
2224POSTchecks/45transferrows/48balances/198chargeevents;0loss/void/grants.
Finalgold `{aer:2950,aqua:2750,ignis:4300,ordo:90,perditio:3230,terra:3560}`,
loosebasic5/VisFilter1/quicksilverDROP7/blank1/oldphialexact/XP11/mainFrostunchanged;
53oldBE/15oldjars309/72candles/вся мастерская и прошлыеitems/knowledge сохранены.
Client1/2 FAILED не выдаются заPASS: inventoryfirstphial expectation и nativehead
aim были harness-only fixes; новый regression +6tickrealRot preaim/40tickACK,
production reach/LOS/payment не ослаблены. Client3 на новой acceptedvalvecopy.

Видны tinted→whitefilter/restrictcuffs/blueindicatorhorizontal→vertical/actual15+14
chat/heldblank. Night/croppedscene/маленькиеoverlappingGUIicons ограничивают
visualclaim;115показываетResonatortooltip, НЕwandvis/nodeview. Full lighting/GUI/
item/FX/audio parity, audio listening, ordinarypackaged/EULAserver/remote/fullsurvival/
API/fullTC4 не заявлены. Бинарный комплект обновлён до этой принятой сборки:
10,250,855bytes SHA256`18169f73b820c81c4c2e67a6082214f7a6d0a19253b1f55c3b59a70b4302f409`.
Предыдущийvalve в `backups/valve-fabric-validate4/`, прежниеvoidjar/Frost сохранены
внеmods. Сразу продолжать **M06c buffer**, затем construct/centrifuge; спецификация
и read-only audits [здесь](analysis/2026-10-17-buffer-centrifuge.md).

## Предыдущий проверенный срез: клапан, резонатор и shapeless arcane, Fabric-only

`valve-fabric-validate4` FULLexit0: **202 JUnit /79 Fabric GameTests**, все79 required,
0failures/errors/skips, **3694 unchanged inputs /53Python**, один productionJAR
с проверенными notices/изоляцией тестов/единственным remappedAW.
`valve-fabric-client3` exit0/**SCRIPT_PASSED**: **все7exactPNG107–113 осмотрены**,
run `client-smoke-2bd69f57-7216-4c54-9e07-c79ee84ee904`,53.289с/1070clientticks.
[Client evidence](porting/valve-client.json), [verification](porting/verification.json),
[контракт и история исправлений](analysis/2026-10-16-valves-resonator.md).
Source-boundmatch=true/historical=false; archive `.tmp/valve-accepted-verification4.json`.
MainJava278 — не процент готовности. Все agents/Gradle/game завершены, NeoForge не запускался.

Клапан повторяет исходную capacity1/suction-политику: stem всегда исключён из
соединений, закрытие отключает новое всасывание, не запирает raw add/take и не
удаляет буфер. PREcount%5 опрашивает именно фронты; стабильный сигнал не отменяет
ручное открытие. Жезл меняет raw-флаги обеих соседних сторон до callbacks.
Работают save/update flow+hadpower, native20° animation, свой plain drop/comparator0.
Резонатор через единый item-first helper читает любой IEssentiaTransport, не снимает
бирки/не переключает клапан, сообщает один раз и не расходует vis/прочность.
Настоящий shapeless serializer подключён к manager/menu/book/aspect resolver;
72перестановки, запреты лишних/неверных входов, оплата/quickmove/остатки и строгие
JSON/stream refusals проверены. Recipe gates/стоимости — исходный TUBES.

Client реально потратил одну savedtube+lever на клапан и iron4/quartz1/stick1 на
резонатор; gold7 оплатил900AQ/450ORDO/450AER, finalvis50AER/860AQ/500IGNIS/50ORDO/
950PD/2300TERRA. Ещё одна savedtube поставлена, одна сохранена. Native4rawcoal+
1fuel→8IGNIS+8POTENTIA: закрытие сохранило7единиц upstream при1вбанке, потом
все8POTENTIA дошли в банку,8IGNIS остались в верхнем alembic;0loss/void/topups.
1049POSTchecks/31transferrows/44balances,4реальных inspector calls, stablepowered
manualreopen20samples, оба redstone edge наблюдались. Старые46BE/14jars301units/
72candles/все wands/foci/armor/knowledge/XP11 сохранены; только oldfurnace BurnTime/
LIT менялись естественно. Прошлое получение входов не повторялось.

Client1 не прошёл из-за порядка camera/use packets; client2 был штатно закрыт
после замечания пользователя: screenshot112 ждал незарегистрированный ChatScreen.
Оба НЕ считаются PASS. Теперь realRot отправляется до use; единый capture-screen
allowlist,10сек deadline/немедленный unsupported-screen FAIL и актуальные pending
диагностики не дают ждать общий10мин timeout. Client3 полностью прошёл с нуля на
новой копии принятого void-save, включая настоящий ChatScreen PNG112.

Видны book/две actualpreview/крупное колесо+stem/open-closed/heldresonator/realchat.
111 содержит native pale vent particles соседней трубы при смене pressure/type,
не inventedclosureFX/flux;112 инструмент частично закрывает control/jar. GUI108/109
затемнены, полная lighting/item/GUI/FX/audio parity не заявлена. Packaged install,
EULAserver/remote/fullsurvival/полный API остаются открыты; это не полный TC4.
JAR10,235,830bytes SHA256
`5fa9b83378943e98e960c60607d08e39c363b54dcb0fbf6bfc8e05302dca21de`.
Соседняя `Thaumcraft-Fabric-1.21.1/mods/` обновлена; предыдущийvoidjar в
`backups/voidjar-fabric-validate2/`, прежнийFrost backup тоже сохранён, не вmods.
Следующий [TUBEFILTER gate](analysis/2026-10-16-tube-filters.md) теперь в работе выше;
filters/restrict/oneway/buffer/golems/fullTC4/NeoForge этим пакетом не закрыты.

## Предыдущий проверенный срез: пустотные банки, Fabric-only

`voidjar-fabric-validate2` exit0: **201JUnit/76Fabric/all76required**,0failures/errors/
skips, **3682unchangedinputs/51Python**,1FabricJARnotices/isolation/singleAW PASS.
`voidjar-fabric-client2` exit0/SCRIPT_PASSED: **7exactPNG100–106 осмотрены**, run
`client-smoke-07c7b930-6dd0-47d7-88b2-85b78e7c3447`,4м1с/4829clientticks.
[Client](porting/voidjar-client.json),[verification](porting/verification.json),
[контракт/история](analysis/2026-10-16-void-jars.md). Source-boundmatch=true,
historical=false; archive `.tmp/voidjar-accepted-verification2.json`. MainJava276,
все sourceowners/Gradle/game завершены, NeoForge не запускался.

Voidvariant хранит64, уничтожаетcompatibleoverflow в lowlevel/top tube/heldalembic
drain; fullmanual8phialrefused. Suction48labelled/nonfull,32full/min48, однапопытка
каждые5localticks. Atomicpair/paidhand+storage commit beforecallbacks/deferredADD;
реальный cancelled/reentrantADD retry24ticks/sameUUID/0dupes. Общие ItemJarAPI/
contents/filter/components/save/drop/maxstack1/knowledge-gatedtooltip и itemrender
переиспользуются обычнойбанкой. Sharedlight9/fullentitycollision/comparator добавлены.
Originalink_sac labelrecipe исправлен после usernotice/client1stall; оба dyeinputs
и ordinary4blanknativecraft проверены, preview теперьfailfast40ticks.

Client использовал actualsavedjar/powder/goldpayer7,540AQ/1350PDtotal; actualvoid/
ordinary/4blanklabels скрафчены. 8rawcoalblocks→104ignis+104potentia,4real32helddrains,
3savedtubes→8refill/**32voided**, remainingpotentia64jar+8phial, ignis64jar+32alembic+
8furnace. 4809nativeaccountingchecks/387transfers/218balancerows, rawpotential до
smelt тожеучтён. Реальныйfullphialrefusal, whole8secondarysuccess, break/physical
pickup/replacement сохраняют64/filter/variant. MainFrostpayer/другиеwands/foci/
robes/goggles/37oldBE/old173units/72candles/XP11 сохранены; прошлуюцепь не повторяли.
Research/raw/emptydevices/floor/camera/daylight — fixtures, не fullsurvival.

Worldjar/physicalPotentialabel/tuberoute/activefurnace, readablebook/preview/64tooltip/
8phial видны. Smallitem/heldlid103 не доказывают clearlyisolatedliquidheight/color;
удалениеизлишка — runtimeaccounting, не inventedFX. GUI/heldlighting затемняет часть
кадров; fullvisual/audio parity, ordinarypackaged/EULAserver/remote/API остаются
открыты. SHAJAR4acc7c78e9d560d307fdd6e2603e6bbcdb067f5276a9edacdc294d8b547960e4.
Экспортированная соседняя папка `Thaumcraft-Fabric-1.21.1` обновлена доэтогоvoidjar
JAR; прежнийFrost сохранён в её `backups/frost-fabric-validate2/`, не в `mods/`.
Следовавший за этим M06a принят выше; filters/buffers/golems/fullTC4/NeoForge не закрыты.

## Предыдущий проверенный срез: базовый Frost, Fabric-only

`frost-fabric-validate2` exit0: **200 JUnit / 74 Fabric / все74 required**, failures/
errors/skips0, **3673 unchanged inputs**,48Python,1FabricJAR notices/isolation PASS.
`frost-fabric-client2` exit0/SCRIPT_PASSED: **5 точных PNG95–99 осмотрены**, run
`client-smoke-53423362-b758-43b7-9e5a-f3cdeba3c121`,1м1с/1208clientticks.
[Client](porting/frost-client.json),[verification](porting/verification.json),
[контракт/адаптации/история](analysis/2026-10-16-frost.md). Source-boundmatch=true,
historical=false; archive `.tmp/frost-accepted-verification2.json`. MainJava274;
sourceowners/Gradle/game завершены, NeoForge не запускался.

Работают originalwater/quartz/diamondrecipe, FOCUSFROSTgate, fourfocuscodecs/stable
AF/BE/BF/BLselector, atomicpaidspawn3/1/1 сfullgear/200ms. CancelledADD остаётся
paid и даёт один ice sound packet. NativeProjectile сохраняет originalPOSTbounce
flight/двойнойnoisylaunch/speed1.5F/damage3/четвёртыйterminalimpact, обычные native
armor/PvP/iframes. Age/bounce persistence и finiteguards — явные safetyadaptations.
Basic Frost не замораживает блоки и не замедляет; upgrades пока отсутствуют.

Actual saved NORMALnode передал37visunits, одна naturalignisregen; никаких vis/
node topups. За799aqua/ordo/perditio focus съел настоящий earneddiamond из прошлой
добычи. NativeF вернул exactExcavation; два singlepaid3/1/1shots реально дали wall
reflection и cow10→7. Finalvis aer1951/aqua2436/ignis2846/ordo933/perditio431/terra464,
11savedXP/3wands/Fire+Shock+Exc/robes+goggles/36oldBE/173jarunits/72candles сохранены.
Старая acquisition не повторялась; raw4water/4quartz, research/floor/daylight/
camera/emptybench/wall/scaledcow — fixtures. SavedplayerInvulnerable1 сохранён,
поэтому owner ricochet не доказательствоsurvivaldamage.

97ясно показывает facetedturquoiseorb в полёте, НЕ самотскок (captureimpact0),
98redcow и orb,99finitevis/blueheldfocus. Книга95 и singlefourfocusselector96
читаемы; tooltip закрываетчастьfooter/bottombuttons пересекаютhotbar. Isolated
impact/terminalFX и audio отдельно не inspected, fullvisual/audio parity не принята.
ProductionJAR: singleAW/metadata/exactconstructor intermediary mapping+native
supercall проверены; ordinarypackagedlaunch этим не заменён. Neoaccess deferred.

Следующая boundedunit — M05 void jars, пока read-only original/OpenTC4 contract.
ПолныйTC4/latefoci/Baubles/Repair/runic/API/automation/golems/mobs/warp/specialnodes/
ordinaryEULA/remote/survival и поздняяNeoForge parity остаются pending.

## Предыдущий проверенный срез: базовый Excavation, Fabric-only

`excavation-fabric-validate3` exit0: **200JUnit/72Fabric/all72required**,0failures/
errors/skips,3653unchangedinputs/48Python/1FabricJAR notices+isolationPASS.
`excavation-fabric-client2` exit0/SCRIPT_PASSED, **6exactPNG89–94 осмотрены**,
run`client-smoke-0856bee0-f459-4c92-99e8-6b0e60d36526`,1м1с/696ticks.
[Client](porting/excavation-client.json),[verification](porting/verification.json),
[контракт/адаптации](analysis/2026-10-16-excavation.md). Source-boundmatch=true,
historical=false,269mainJava. Все процессы завершены, NeoForge не запускался.

Работают exactearth/quartz/emeraldTAGrecipe/research/book, actualthirdfocuscodec/
Fswap/stableAF-BE-BLsort, originalCHARGE/range10/COLLIDER/no fluids, floatprice15base/
11fullgear, stone8/dirt4/native-material speeds. Платёж только по завершённой
попытке **до** реальной protected harvest, включая отказ; misleadingperTicktooltip
сохранён. Release сохраняет progress какoriginal; negativehardness/inheritance/
depletion поведение и lifecycle safety guards проверены сервером.

Native existingrealplayer.gameMode запускает настоящие Architectury/Fabric events,
native loot/XP/onRemove/capturedBE. Scoped/finally privilege не передаётся nested
ordinary breaks; successful removal witness не подменяется optimisticreturn/air.
Сохранность paidcancel/BEreplacement/AFTERreplacement/containers/creative-shulker/
XP/ice/halves/infested/hive и unchanged wand/stats/exhaustion проверена72suite.

Client реально изготовил2chests из16planks и focus4earth/4quartz/emerald, оплатил
1599terra/399ordo/399perditio из savedvis. На6.10blocks freepartial.6500001→pause→
resumedchest+3contents, diamondore→1diamond+**4nativeXP**, ONEprotectedearnedchest
осталсясexactBE/nooutputs. Все3attempts×11terra=33, finalterra164. Новыхnodecharge/
XPgrants/topups нет. OldFire+Shock/3wands/wornrobes+goggles/114TCblocks/34BE/12jars
173units/72candles/apple/knowledge сохранены; старая acquisition не повторялась.

Виден реальный greenbeam/impact91 и darkgreen6-facefocus/finitevis94, книга89 и
single-selector90. Earlyfinecracks91 неразличимы (native stage1 observed), highstage
cracks на protectedchest93 видны. Jar tooltip остался на92, bottomselectorbuttons
пересекают hotbar; не fullFX/GUI/audio parity. Occupiedhive refusal/nativewaterlogging/
raworeloot/scopedinfested policy и statelessnonBE ABA limit задокументированы.
Rawmaterials/research/oreterrain/floor/camera/workbench/protectionpolicy — fixtures,
не fullsurvival. Следующим на тот момент был basicFrost; принят выше.

## Предыдущий проверенный срез: три мантии и базовый Shock, Fabric-only

`clothing-fabric-validate8` exit0: **200JUnit/70Fabric/all70required**,0failures/
errors/skips,3630unchangedinputs/48Python,1FabricJAR notices/isolation PASS.
`clothing-fabric-client7` exit0/SCRIPT_PASSED, **8 exact PNG81–88 осмотрены**,
run`client-smoke-da7fb896-e98b-45a3-8d9d-e578ba12ef0c`,2м16с/2117ticks.
[Client](porting/clothing-client.json),[verification](porting/verification.json),
[контракт/адаптации/история](analysis/2026-10-16-robes-and-early-foci.md).
Source-boundmatch=true/historical=false. MainJava262 — не процент готовности.

Реально работают19fabric/3robes (400/375/325 durability,armor3/2/1,enchant25,
uncommon,fabric-onlyrepair,discount2/2/1), nativeold-sheep dye/default-purple mix,
colored/default/creative water debit и сохранность компонентов. Client сам сделал
20fabric из80string/20wool, nativecactuswear1/3robes, RED6A3880→8C3960, wash3→2→1,
anvil twentiethfabric/onelevel→repairedchest. Одна недостающая XPpoint предоставлена
как INPUT. Native realgear +goggles даёт10points/armor7. Fire7 здесь толькоPRICE,
не новый клиентский paidFire. Render-onlyDYEABLE gate исправил graywearer без
vanilla dye-recipe/cauldron opt-in; native inner/outer/base/untintedoverlay работают.

BasicShock: exact4air/4quartz/POTATO,10aer/ordo/perditio gateFOCUSSHOCK; realF
swap/persistent+wirefocusdata. Native250ms wallclock/20entityray/per-candidateLOS,
paidbeforehit25baseAIR, ordinaryplayer4damage, nativearmor/iframes/PvP/protection.
Client4×19AIR=76:14.03blockscow10→6,secondpaid/iframes6,paidmiss/paidwallrefusal.
Старый Fire returnedexact. Ни lightningentity, ни fire/blockdamage/upgrades.
Native bounded seeded lightning — approximate, не полная oldfractalparity.

Finite NORMAL80/primal реально передал206units/1040ticks безFireexpense; отдельные
client/serverNODE_USES исправили nativeequal-key hazard. Clienttrace показывает
две реальные native активации с одинаковым node ray; PREcamera fixture держал
наведение до packet dispatch. Main save/price schema не менялась. Final70server
и focusedclient — не повтор старых research/infusion/caps/goggles acquisitions.

Visual observations честно ограничены:85purple/base+gold-grayoverlay/boots/goggles,
86singleShocktooltip/currentFire; **87ranged ribbon не различим** (видны redhit/
blueheldfocus),88brightblue/violet paidwallFX видны. 84tooltip partlycoversXPcost,
86footer/HUD overlaps. Полная GUI/FX/audio parity ещё не закрыта. Old2goldwands/
3totalwands/4phials/173units12jars/72candles/112TCblocks/прочие oldstacks сохранены.
Rawmaterials/research/node/machines/daylight/floor/camera/cactus/wall/cow — fixtures,
не fullsurvival/естественное исследование. ПолныйTC4/NeoForge ещё НЕ готовы.
Следующая на тот момент unit — Excavation; её новая приёмка приведена выше.

## Предыдущий проверенный срез: очки откровения, Fabric-only

Исторический goggles checkpoint ниже не заменяет более новую приёмку мантий/Shock.

`goggles-fabric-validate3` exit0: **200JUnit/68Fabric**, все68required,
failures/errors/skips0,3598unchangedinputs/48Python,1FabricJAR notices/isolation PASS.
`goggles-fabric-client2` exit0/SCRIPT_PASSED: **7focusedPNG74–80** осмотрены,
run`client-smoke-68c3b2cc-ad57-4940-9367-a78f7f6b5bca`,1м13с/839ticks.
[Client](porting/goggles-client.json),[verification](porting/verification.json),
[контракт/адаптации](analysis/2026-10-16-goggles.md). Старуюresearch/infusion/caps
цепочку не повторяли: actualSTOPPEDcapssave с3wands/173essentia/altar — inputs.

Работают originalGOGGLESrecipe/researchgate,350durability/head1/enchant25/rare/
goldrepair,5percentagepointsvisdiscount. Realnative2thaumometers→goggles за450/270
Thaumiumpayer, finitechargeбезtopup. Naked/wand/offhand — faintdepthtestedcore;
mainhandmeterfull48/headIRevealerfull64, обаfullpathsигнорируютterrainocclusion.
LivegogglesoverlayпоказываетALLCURRENTcontaineraspects/compounds, unknownicon+
quantityбезdiscovery/scan,5вряд; normalhitResult, неxraycontents. NoteBlockNOTE
поддерживаетсяsyncedstate, ноотдельнойclientcaptureегоещёнет.

Actualclient:7-entrynodecurrentпослеcharge≠base, compoundunknown13; AIRjar16→8→16
nativephialиliveHUD,173savedunits/4phials restored. Новыйjarреальнооплачен85aqua
centivis, Fire6×8=48/12embers/cowdamage+ignite, обаoldgoldwandsипрочиеsavedoutputs
сохранены. ТочныеPNGпоказываютправильнуюориентацию/icons/numbers/unknown13/AIR8/
.85price/worngogglestexture. Fireвоflightвэтих7PNGнепоказан, debit/collisionruntime.

GameTest3freshпосле**test-only**исправления mockcreativeRMB→nativeinventoryequip;
client2неповторялся. ClientPFнеучитываетserver-onlygameTests, совпадаетсfinalmain/
client/resources; recordverificationmatches_current_recorded_inputs=true. Не выдавать
failedserver2заполныйPASS. No gameplaypricingподгонки: FLOAT1vis85/3vis254/5vis424.
SourceFOV70→35.44radquirkсохранён, nativecamera billboards/pathlayersадаптированы.

Сырьё/research/finiteNORMALnodes/workbench/floor/wall/daylight/camera/cow — fixtures,
неfullsurvival/naturaldiscovery. InventoryShiftHUD/ArcaneEar/vis-network/drainbeams,
fullINodeAPI/Baubles/Frugal/staves/Repair/runic/specialnodes/fullFX/audio/ordinary
EULA/remote/NeoForge pending. MainJava255, неcompletionpercentage. Следующийпакет —
[тримантии+basicShock](analysis/2026-10-16-robes-and-early-foci.md), ещёнеimplemented.

## Предыдущий проверенный срез: эфирная эссенция и таумиевые наконечники, Fabric-only

`caps-fabric-validate1` exit0: **200JUnit/67Fabric**, все67required,
failures/errors/skips0,3586unchangedinputs,48Python,1FabricJAR notices/isolation PASS.
`caps-fabric-client1` exit0/SCRIPT_PASSED: **7focusedcaptures**, root осмотрел67–73,
run`client-smoke-4940ea58-8d98-460d-b3c2-609a76b13298`. Длительность6м7с/6778ticks;
старые research/equipment/altar acquisition НЕ повторялись. Использован настоящий
сохранённый алтарь/инвентарь, не новые outputfixtures. [Client evidence](porting/caps-client.json),
[verification](porting/verification.json),[контракт/границы](analysis/2026-10-16-node-essence-thaumium-caps.md).

Работает original current-nodeharvest: amount≥5→floor(amount/10)+1essences/aspect,
containedaspect2+baseauram2, nativecomponent/save/packet/tint/knowledge-gatedtooltip.
Не любойremove/unload/explosion; originalcreative/doTileDrops поведение и успешный
commit/отмена проверены. Originalhardness2/resistance200 восстановлены. Blockscatter/
initialmotion/pickup10 сохранены, level-postdelivery/canceledADDretention явноадаптированы.
Балансированныйосколок плавится вSalis, nuggetreversal/inertcap/chargedcap recipes,
CAP_thaumiumgate/.9Fcap/craftcost6/nativeassembly54/componentcodec/modelselection
реализованы. Salisaspects вычисляются изunculledbalancedbase по originalConfigAspects421.

Actualclient: NORMALAIR70→8физическихessences;8essences+12coal→80essentia,
10withdrawals фиалами→4jars. Двеинфузии расходуют36units+6Salis идают2caps;
44surplus+старые129units retained. 2declaredintermediateingots→18nuggets→10spent/
8retained. 6intermediatebalancedshards→nativeSalis;1intermediateSilverwoodcore+
earnedcaps→EMPTY100viswand за54каждогоprimal actualoldgoldpayer. Finitecharge→
6actualFireticks×9=54centivis,12embers,cow10→8+ignite. Обаoldwands/прочиеoutputs
сохранены, ovenfuel5→3. Без output/essentia/damage/vis injection; node/сырьё/research/
emptydevices/floor/camera — declaredfixtures, не survival/naturalnodediscovery.

ТочныеPNG осмотрены: ночь затрудняетtint67, stream70 целикомнеразличим,
73 показываетcollision/Fire, анеcloseupобоихновыхcaps. Costs/состав/dropcounts
установленыruntime. Wispmobs/PURE/knotdrops/othercaps/enchantedinfusion/runic/Repair/
fullpublicAPI/fullvisual/audio/ordinaryEULA/remote/NeoForge всёещёpending.
MainJava248, не процентготовности. Следующие **очки откровения уже пишутся**, ещё
не собраны/не прогонялись; их новые исходники не наследуют этотPASS.

## Предыдущий проверенный срез: таумиевое снаряжение, Fabric-only

`equipment-fabric-validate2` exit0: **200JUnit/66Fabric**, failures/errors/skips0,
все66required cases,3563неизменённых inputs,48Python, FabricJAR notices/isolation PASS.
`equipment-fabric-client1` exit0/SCRIPT_PASSED: **только equipment scope,6captures**,
root осмотрел точные61–66; run `client-smoke-560ad46d-c8e7-453a-ae0a-4f42126c9f23`.
Длительность клиентского запуска1м24с,1203native ticks. **Это не полный66-scene run**:
предыдущая цепочка не повторялась. Настоящий сохранённый M07 inventory/jars/fuel —
входы из копии isolated save, не выданные заново результаты.
[Client evidence](porting/equipment-client.json),[source-bound verification](porting/verification.json).

Реализованы пять инструментов и четыре части брони, обычные рецепты без research/
vis gate, miningtier3/400uses/speed7/enchant22(hoe5), damage modifiers6/5/4/3/none,
armor2/6/5/2 и исходный ремонт actualThaumium ingot. Все9 рецептов/характеристик/
native anvil repairs проверены сервером, включая отсутствие THAUMIUM research;
настоящие native атаки дают7/6/5/4/1 с originalwear1/2/2/2/0. Это не возвращение
всего pre1.9combat: cooldown/Sharpness/sweeping/native interactions — отдельная граница.

Actual client:24rods+12iron→48praecantatio→12physical ingots, затем pick3/chest8/
repair1. Кирка реально добыла stone/obsidian (220client miningticks дляobsidian),
получила2wear и отремонтирована за1ingot/1level. После этого7отдельных declared
intermediateingots→leggings; nativeQUICK_MOVE экипировал chest+legs (armor11).
Старые оба жезла/6000Aer/129эссенции/5угля и inventory сохранены без topups.
Точные recipe/menu/ingot/drop/pick/armorlayer PNG осмотрены: ночь ограничивает
контраст66; tooltipчастичнозакрываетцену65, advancementnoticeперекрываетчастьHUD.
Exact costs/stats/retention установлены runtime, не угадывались по картинке.

Сырьё/research/XP/пустые устройства/Nitorheat/floor/targets/camera — fixtures,
не fullsurvival. Входящий урон брони вclientstory, Repairenchantment/runicabilities/
caps/Salis/auramsource и fullvisual/audio/packaged/remote ещё не закрыты. NeoForge
не изменялся/не запускался;1Fabricartifact, безновогоNeoPASS. MainJava243,
semanticledger1044needs_review — не процент завершения. [Контракт/границы](analysis/2026-10-16-thaumium-next-unit.md).

Следующая unit — originalNodeHarvest→WispEssence→настоящийauram→chargedThaumiumcaps.
Пока не реализована и не подтверждается этим checkpoint.

## Предыдущий проверенный срез: инфузия и серебродревесный жезл100vis, Fabric-only

`infusion-fabric-validate7`exit0: **200JUnit/65Fabric**, failures/errors/skips0,
allrequired65,3521unchangedinputs,48Python/FabricJARnotices+testisolationPASS.
`infusion-fabric-client5`exit0/**SCRIPT_PASSED60**, rootосмотрелточные53–60;
run`client-smoke-15fab7dc-e9ea-4a9f-b22d-9239a5ff09b3`.
[Client evidence](porting/infusion-client.json),[source-bound recorderPASS](porting/verification.json).
NeoForge не билдился/не прогонялся;1Fabricartifact, никакого новогоNeoPASS.

Работают матрица/пьедесталы/колонны, nativeформирование25visкаждогоprimal с
модификатором наконечников, wirelessизбанок, сохранение/пауза/возобновление,
оригинальная симметрия и все21исход нестабильности. Настоящий первыйSTACKрецепт:
silverwoodlog+balanced/sixprimalshards+9каждогоprimal/9praecantatio,I5→core.
Серебродревесный жезл100vis собирается за27каждогоprimal сgoldcaps; outputпустой.

Actualclient: изготовленыmatrix/10pedestals/72candles,9pedestalsпоставлены/1сохранён;
формирование4+4pillarhalves,48rawshards→192unitsчерезнастоящуюпечь/3alembics/
24phialtransfers→8jars. Поглощено63по1/10ticks+7компонентовпо6циклов;129units
surplus/wasteостались. Genuinepaired72candles даютsymmetry−6/I−1, беззаписиI/RNG.
Nativecoreejection/pickup→2реальноизготовленныхgoldcaps→assembly→held-usecharge
6000aer, конечныйNORMALnode80→20. Старыеwand/focus/book/apple/4phials/6tubes/8drops
сохранены; nooutput/essentia/filledjar/sessioninjection. Raw/intermediateinputs,
emptydevices, research,oldwandtopups,floor/camera/node —declaredfixtures, неfullsurvival.

Rootвиделbook/menu/OBJpillars/матричныеrunes/свечи/цветнойpaidstream/core иправильный
Silverwood/goldheldmodel. ИсправленыnativeemptyBEupdateghost,clippedmodelproperty
иfixtureshandoff/pickupapproach. Неfullvisual/audio parity: audioнепрослушивалось,
bolt/fragments —boundedadaptations,candlewaxdrips/othercolors отсутствуют; старый
radialfooter/HUDoverlapнеисправлен. Enchantment/tagpatch/runic/остальныеinfusion
rewards/APIadd-ons/обычныйdedicatedсEULA/remote/fullsurvival/NeoForgeещёоткрыты.
[Правила и история](analysis/2026-10-16-infusion-first-unit.md). Следом —
[9полезныхThaumiumequipmentitems](analysis/2026-10-16-thaumium-next-unit.md), caps
только с подтверждённым настоящимauramsource. ПолныйпортНЕзавершён.

## Предыдущий проверенный срез: базовые трубы и сетевые банки, Fabric-only

`tubes-fabric-validate3`exit0: **200JUnit/63Fabric**, failures/errors/skips0,
allrequired63,3458inputs unchanged,48PythonPASS/FabricJAR notices+isolationPASS.
`tubes-fabric-client1`exit0/SCRIPT_PASSED52, root exact47–52 inspected; [client](porting/tubes-client.json)
и[source-bound verification](porting/verification.json). **NeoForgeнебилдился/не
прогонялся**,1artifact, loaders_checked толькоfabric.

Настоящие basic tubes сone-unitbuffers/localphase/suction/venting, jarUPpull,
wandindexedsideclose/reopen ивыделеннаяquicksilverDROP prerequisite. ActualFabric:
прежняяquicksilver→9drops→8tubes (500aqua+500ordo centivis);2nativefloatingplacements;
wandcut51ticks/reopen;4inputcoal+1fuel→8potentia вlabelledjar/8ignis вotheralembic.
150native-tick exactconservation,8реальныхincrements по1; survivalbreak/pickup/
replacement сохраняетall8+label.6tubes/8drops/4phials/прежниеfocus/wand/book/apple
сохранены. Входы/emptydevices/research/vis-topup/floor/camera/bookscreen —fixtures,
outputs/essentia/filledjar/boundlabel неинъецировались; это неfullsurvival.

Rootвиделnativebook/menu,geometry,closedgap/rejoinedarms иlabelпослепереносабанки.
Serverfull64/vent/reload проверены отдельно; **ventFX/audioнеосмотрены**,49/51имеют
vanillahelditemtransition,51HUDчастичноскрываетнизбанки. Неfullvisualparity/M06/M13:
valves/filters/buffers/reservoir/consumers/addonIE-onlyatomiccompatещёотсутствуют.
[Originalrules/forcedguards/история](analysis/2026-10-16-basic-tubes-spec.md).
## Предыдущий проверенный срез: огненный фокус и магические растения

`fire-validate7`exit0: **200JUnit/61Fabric/61NeoForge**, failures/errors/skips0,
allrequired61,3437inputs unchanged;48PythonPASS, обаJAR notices/test-isolationPASS.
`fire-client5`exit0/SCRIPT_PASSED46, root exact41–46 inspected; [client evidence](porting/fire-focus-client.json)
и[source-bound verification](porting/verification.json).

Работают basic Fire Focus: настоящий arcane craft20ignis/10perditio, server-owned
F-selection/persistent focus, RMBchannel10centivis/tick (gold), два Ember/paidtick,
native attributed damage2/ignite3sec/life20 и ShiftFremove без потерь. Actualclient
6paidticks→60centivis/12ADDattempts→cowhealth10→8+fire; non-wandF сохраняетvanilla
swap (2packets/restore), node-use session предшествует focus и не переключается в
выстрел при отведении взгляда. Shimmerleaf/cinderpearl имеют original nativeblocks,
worldgen passes, tags/harvest/recipes/light8/FX; клиент добыл2plants и через реальные
2×2crafts получил1quicksilver+1blazepowder.4прежнихphials/paidwand/apple сохранены.

Supplied inputs/research/vis-topup/workbench/noAIcow/plantedpatch/camera — не полная
survival/naturalgen проверка. Root видел world/Embers/head/новые models и sharp
radial title/tooltip. **Footer/help radial пока накладывается на vanillaHUD**, не
объявляется polishedUI/fullvisualparity. Нет otherfoci/upgrades/pouches/staves/
sceptres/legacyHUD/fullheldanimation и полного M02/M04/M08/API. NeoForge client,
ordinarypackaged/dedicated,remote/fullsurvival по-прежнему открыты.
[Правила/ограничения/история](analysis/2026-10-16-fire-focus-and-flowers.md).

Следующая unit — [M06 basic tubes](analysis/2026-10-16-basic-tubes-spec.md): native
transport/jarUPpull и настоящая quicksilverDROP prerequisite, не wholeTC6 transplant.
Новые sourcechanges не подтверждаются принятым Fire7. [Аудит двух новых refs](analysis/2026-10-16-user-reference-projects.md).

## Предыдущий проверенный срез: древесина, перегонка и улучшенные жезлы

`distillation-validate5`exit0: **200JUnit/58Fabric/58NeoForge**, failures/errors/skips0,
все58required cases;3397inputs unchanged.48PythonPASS, оба remapped JAR прошли
notice/test-isolation checks. `distillation-client5`exit0/SCRIPT_PASSED40captures,
root exact33–40 inspected. [Client](porting/distillation-client.json),
[source-bound verification](porting/verification.json).

Реализованы genuine magical trees/logs/leaves/saplings/planks, arcane stone family,
Vis Filter, normal alchemical furnace, alembics32, manual phials8→jars64, labels,
bellows и wood/greatwood + iron/gold wand assembly. Клиент реально выполнил
8coal +1Alumentum→16ignis+16potentia→4phials→2jars, сохранив4emptyphials и обнулив
machine buffers. Новый wand стоит990eachprimal при сборке wood/iron, имеет50vis
ёмкости/1.0gold modifier и действительно заряжается у node. Прозрачное стекло и
цветная жидкость видны на35; другие новые world/GUI/book/icons тоже осмотрены.

Supplied materials/machines/research/node/camera/seeded tree fixtures, не полный
survival или natural-frequency verdict. Не закрыты full M02/M04/M05, PURE knots,
spider dungeon/loot/shimmerleaf, tubes/golems, всеwandmaterials/foci, full rendering/
audio parity. NeoForge client/packaged/remote multiplayer открыты.
[Исходные правила/адаптации/ошибки запусков](analysis/2026-10-16-distillation-and-magical-wood.md).

## Предыдущий проверенный срез M05: тигель и ранняя алхимия

Добавлены native тигель, нагрев/вода, физическое растворение и10 original recipes;
Нитор ставится/светит/греет, Алюментум бросается/взрывается и горит6400ticks.
Реализованы finite flux и vis exhaustion с per-instance milk rules, без фиктивной
taint/slime замены. Scans и тигель используют один recipe/component resolver;
исправлены96 direct bindings из original TC4 поверх неполного legacy fallback.
В книге доступны реальные crucible pages, включая цикл шести balanced shard recipes.

`m05-validate4`: exit0, **197JUnit/51Fabric/51NeoForge**, failures/errors/skips0,
3228 unchanged inputs, все51required cases на loader, оба remapped JAR.
`.tmp/m05-assets3.log`:48PythonPASS. Resource validation495sprites/103refs/missing0.
`m05-client2`: exit0/SCRIPT_PASSED32captures, root exact27–32 inspected. Реальные
wand→cauldron conversion, bucket→boil, drop2coal+3torches→4potentia/4ignis/3lux,
dropglowstone→physicalNitor, pickup→placement/light15. Крафт оставляет950mB и по1
potentia/ignis. Материалы, NITOR и перемещения камеры supplied, не полный survival.
[Client evidence](porting/alchemy-client.json), [сборки/сервер](porting/verification.json).

Native effect holder/NBT ошибка исправлена и проверена на обоих loaders; original
milk/merge/reload quirks сохранены. Серую modern water texture исправляет explicit
ordinary-water tint перед original pollution multiplier; exact old pixels/full FX
не заявлены. Полные M05/M11, NeoForge client и packaged/remote multiplayer открыты.
[Исходные правила, исправления и границы](analysis/2026-10-16-M05-early-alchemy.md).

## Предыдущий проверенный срез16.10: руды → узлы → магический верстак

Реализованы и подключены; **центральный implementation checkpoint прошёл**:

- Восемь original ore variants, loot/Fortune/Silk/XP, cinnabar/amber smelting и fresh-chunk generation на обоих loaders; осколки подходят существующему thaumometer recipe. У руд есть original object-aspect bindings, модели и EN/RU названия.
- R09: настоящее GUI/C2S объединение с discovery и расходом RP/бонусов; shared table ecology производит бонусы окружения. Copy остаётся R10, не включён поверх нерешённого count100/127 persistence.
- Natural NORMAL aura nodes, current/base, regeneration, pruning/death и normal bullying; original-atlas renderer. Special rolls явно пропускаются без замены. [Границы M03](analysis/2026-10-16-M03-normal-nodes.md).
- Бесплатная original wand-конверсия base table0/1 в магический верстак; persistent3×3/wand, vanilla fallback, server-authoritative arcane crafting. Реальные Enchanted Fabric и Warded Jar; Vis Filter ждёт silverwood.
- Страницы10 действующих recipe IDs в книге используют те же native RecipeHolders, что крафт, с item tooltips и vis. Остальные recipe references/kinds не объявлены перенесёнными.

`early-content-validate3`: exit0, **192JUnit/47Fabric/47NeoForge** GameTests, failures/errors/skips0, все47 required IDs. 3149 input files unchanged; production JARs не содержат test support или test flat preset. 48 Python checks PASS; resource verify495sprites/76refs/missing0. В16 настоящих noise chunks найдены все8руд, их shard drops подходят реальному thaumometer recipe.

`early-content-client2`: exit0/SCRIPT_PASSED,26captures. Root просмотрел exactPNG04/12/21–26: combine/discovery, недостаток и достаток vis, руды/node/worktable в мире и native recipe page. Реальные GUI/C2S combine→wand-use ticks→craft выполнены; recipe списал110centivis каждого primal. Supplied материалы/узел не являются survival verdict. [Client evidence](porting/early-content-client.json).

В первом client run визуально обнаружены невидимые shard ingredients: native1.21.1 ItemRenderer использует ARGB alpha, старый RGB давал alpha0. Исправлены все shared item tint handlers и client assertions; повторный PNG04 показывает оба осколка. Это не pixel-perfect/full visual verdict. Полные M01–M04/M00–M15, NeoForge client/packaged/remote multiplayer остаются открытыми.

Сохранённые правила, source anchors и authority corrections: [early-content notes](analysis/2026-10-16-early-content.md). Исторический инвентарь этого среза101main/34partial/900pending/0complete; новые units не повышались автоматически до semantic completion.

## Предыдущий проверенный срез: R08 редактирование исследовательского стола

Теперь R08 подключён к настоящему GUI/серверу: палитра, drag→PLACE, click→ERASE,
расход RP-first/bonus fallback и literal ink, solver и превращение в discovery.
Общий bonus сохраняется/синхронизируется; ecology/combine на этом R08 checkpoint ещё отсутствовали. Компоненты/count
записки не теряются при превращении, изучение остаётся отдельным использованием.

`r08-content1` — сборки обоих loaders,191JUnit/42Fabric/42NeoForge GameTests PASS.
`r08-content-client1` — настоящий Fabric SCRIPT_PASSED: через mouse handlers/C2S
выполнены place→erase→place→solve→quick-move→use→learn. Ink0→4, lux2→0,
ignis3→2; NITOR не выдаётся до использования открытия. Это synthetic четырёхклеточная
fixture, не новый generator/survival verdict. Root просмотрел exactPNG17 и20,
не все20 текущих PNG. [Client evidence](porting/research-table-editing-client.json).

Новый reference пользователя: [EugeneHeisst/thaumcraft-port](analysis/2026-10-16-eugene-reference.md).
Этот R08 результат не подтверждает последующий новый контент без отдельного запуска.
R10 copy, NeoForge client/packaged/multiplayer/survival и остальные системы остаются открыты.

## Техническая подготовка A07/pure R08 до подключения игрового действия (история)

R07 implementation сохранён в `ac1734af`; его NeoForge/packaged visual приёмка
остаётся открытой. [Новый обзор всего workspace](analysis/2026-10-16-workspace-audit-ru.md)
разделяет активные исходники, legacy/original/reference корпуса, caches и реальные
пробелы. Он не заявляет построчного аудита всех файлов.

- A07: 7 common helpers, оба wrappers, Neo mock player и empty template перенесены
  в `src/gameTest`. Все11 файлов byte-identical относительно HEAD, старые пути
  отсутствуют. Test-only metadata/bootstrap отделены от main; Fabric client smoke
  больше не регистрируется глобально. Production entity/drop recovery не удалена.
- Gradle gates проверяют global/normal-run scopes и final remapped archives;
  recorder независимо отклоняет test classes/references/entrypoints/templates.
  [Новый протокол evidence](porting/VALIDATION.md) связывает результаты со всеми
  source sets/resources/configs и требует свежие XML/JAR, все41 обязательных
  GameTests на каждом loader, каждый common JUnit suite и отсутствие skips.
- Pure R08: `ResearchNoteEditing` планирует place/erase, literal ink, RP-first и
  bonus fallback, Researcher RNG, refund discovery **до** solver, pruning и solved
  presentation. Сохранены original raw-int/short-query/no-op debit/overflow quirks.
  Независимый read-only review существенных дефектов не обнаружил. Это **не**
  native commit и ещё не доступное игроку редактирование.
- Выполнено:48 Python tests, extractor48aspects/201researches, resource verify
  494sprites/69refs/66events/111OGG/missing0; scope check/self-test и scoped
  `git diff --check`. Общий diff-check остаётся красным на ранее существовавших
  whitespace/CRLF в legacy/donor assets; эти файлы не исправлялись.
- Первый центральный Java21/Gradle8.12.1 запуск `.tmp/a07-validate1.log` завершился
  exit1:191JUnit и41Fabric прошли, но NeoForge упал до GameTests на duplicate
  module packages (`generated_*` и `thaumcraft`). Зависшая после fatal startup
  только owned test JVM остановлена после сохранения thread dump. Это не PASS
  всей итерации. После исправления dev mod roots `a07-validate2` завершился exit0.
  Recorder выявил свою неверную directory→package догадку для пяти существующих
  package-private JUnit suites; теперь берётся объявленный Java package и добавлена
  regression. Итоговый полный `a07-validate3` после этого исправления — exit0,
  **191JUnit/41Fabric/41NeoForge**, failures/errors/skips0, все41 обязательных
  IDs каждого loader присутствуют. Оба packaging/isolation gate PASS.
- [verification.json](porting/verification.json), schema2, записан из свежего
  snapshot2980 input files и hashes logs/XML/JAR; inputs до/после неизменны.
  Старая inspected client запись сохранена как `historical_evidence_only=true`.
  Новый Fabric harness runtime проверяется отдельно, не выводится из compilation.
- Ordinary dev server smoke не стартовал: проверенный локальный EULA отсутствует;
  согласие не создавалось автоматически. Normal-run scopes проверены gate, но
  ordinary/packaged dedicated runtime этим не подтверждён.

Текущий inventory:72main/33partial/901pending/0complete; scope1044needs_review и
50unique counterparts. Остаются native R08 bonus persistence/sync, shared revision,
per-open menu session, preserving stack transaction, bounded C2S, GUI/effects и
проверка двух игроков; R09/R10 и весь остальной M00–M15 не закрыты.

Полный перенос продолжается по [плану](MIGRATION_PLAN.md), дополненному волнами W0–W7 и контрольными точками C1–C5. Последний принятый игровой checkpoint — **R06 `7222a5a1`**: выдача первичных записок, расход чернил и настоящий запрос из книги. [R07 table](analysis/2026-10-15-research-table-spec.md) имеет проверенный implementation checkpoint; финальная NeoForge/packaged visual приёмка ещё открыта. Ни один полный этап M00–M15 этим не закрыт.

## Проверенный implementation checkpoint R07 до выделения A07

- Реализованы table craft/placement/conversion, общий two-slot inventory и38-slot menu, persistence/sync, teardown/delivery queue, BER и read-only GUI. Interactive editing, combine/bonus и copying остаются R08–R10.
- Итоговый `.tmp/r07-validate7.log`/exit.json: exit0/BUILD SUCCESSFUL, **161 JUnit,41 Fabric+41 NeoForge GameTests**. XML: failures/errors/skips0; отдельно проверены обязательные case IDs. Runtime различает loaded-cross-chunk NBT recreation, настоящий unload/disk reload и cold-load repair.
- Actual Architectury ADD cancellation и retry проверены на обоих loaders. Guarded cleanup освобождает только новую unattached UUID reservation без visible/hidden owner. Native hidden-owner fixture создаёт владельца после snapshot, скрывает section, затем проверяет сохранность UUID/entity/stack. Callback-created replacement table не теряет inventory.
- Failed output сохраняет stable UUID/count/components и transient cooldown не менее20 ticks. Явный unrelated enqueue доставляет свой stack, не будит failed output раньше срока. После restart transient delay сбрасывается; pending material schema не меняется. Batch64 и queue-wide wake могут отложить доставку готовой записи; точный upper-bound20 не заявлен.
- История исправлений: validate5 выявил Fabric UUID leak; validate6 прошёл cleanup/hidden guard, но поймал early retry при новом enqueue; validate7 проверил оба исправления. Это не ослабление прежнего теста: добавлен явный early-wake regression.
- `.tmp/r07-assets3.log`:27 Python PASS. Converter/extractor/resource/notices checks PASS;494 atlas sprites,69 model/atlas refs,66 sound events/111 OGG,missing0. Original table PNG byte-identical; native codecs и UV probe проверены. Полный visual/audio parity не заявлен.
- Actual Fabric client3 `.tmp/r07-client3.log`/exit.json: exit0/SCRIPT_PASSED, run `client-smoke-91bd518d-9a58-4451-addf-4e3becd205c8`. Все19 exact PNG просмотрены root;15–19 независимо с совпавшими SHA256. Проверены native placement/conversion, обе половины menu, quick-move, world/item/GUI contexts. Предыдущий alpha-дефект17 исправлен.
- Настоящий process restart client2→client3 до setup mutations сравнил весь native Inventory NBT сохранённой пары. Source run `client-smoke-b42fee38-3002-45f7-bf25-fa8984d74cd2`; expectation извлечён read-only из owned `.mca`, а не создан setup повторно. Source fixture world не открывался; его level.dat hash до/после совпал. Это проверка process restart, не crash-atomic cross-file transaction.
- [Verification](porting/verification.json) содержит итоговые XML/cases/artifact hashes и [client inspection](porting/client-smoke-verification.json). Inventory81 main/33partial/901pending/0complete; scope1044needs_review/49unique counterparts. Semantic completion автоматически не повышалась.
- Открыто: NeoForge/packaged table client, research-item dropped, все ориентации/GUI scales, full lighting/animation/audio parity. Hand cup светлее world cup; native UP/side UV совпали, полное lighting parity не доказано. Ранее отмеченные RU label/primary tooltip layout ограничения остаются.
- Следующая реализация: A07 изоляция GameTest helpers/entrypoints и общий client harness обоих loaders; параллельно R08 pure edit policy по [editing spec](analysis/2026-10-15-research-table-editing-spec.md). Draft в ignored scratch не считается подключённой механикой. До natural copy R10 остаётся native player/container/cursor/drop count100/127 persistence blocker.

## Текущая проверенная итерация R06

- Bounded key-only C2S не принимает player identity, slots, цену или готовую сетку. Actual sender/server thread берутся из connection; сервер проверяет normal/hidden parents, clues/category gates и primary classification при default difficulty0.
- Existing normal/discovery note ищется в main inventory до расходов/RNG. Новая note генерируется до commit; расходуется первая бумага и первый пригодный IScribeTools. Выдача не завершает исследование, не списывает RP и не меняет warp/discoveries.
- Player ink:99→100 оставляет count1; creative/Unbreakable обходят damage; Unbreaking level>0 использует ровно один player nextInt(level+1), а не modern world RNG. Table adapter выделен отдельно; native interactive table consumption ещё pending.
- Последняя бумага освобождает место для note. Полный survival inventory использует native drop с проверкой world membership; null/unspawned output не списывает staged материалы. Failure проверен настоящим ServerPlayer subtype на обоих loaders; actual ItemTossEvent subscriber cancellation отдельно не эмулировался.
- Quirk kept: full creative inventory сpaper2 расходует бумагу до1, сохраняет ink и теряет созданную note. Old InventoryPlayer подтверждён bytecode; это отдельный исход discarded, не survival drop.
- Handler синхронизирует inventory/feedback/knowledge. GUI не меняет ресурсы заранее и не отправляет запрос при уже существующей note. Независимый review не обнаружил блокирующих дефектов текущего default0/registered-inkwell пути.
- Central `.tmp/r06-validate1.log` exit0/BUILD SUCCESSFUL: **155 JUnit,26 Fabric,26 NeoForge GameTests**, XML failures/errors/skipped0. Новые24 JUnit —15ink/6policy/3payload;9runtime cases добавлены на каждый loader. Python24, extractor/resource/notices checks прошли.
- Actual Fabric `.tmp/r06-client2.log` exit0/SCRIPT_PASSED: **14 PNG просмотрены root**,13/14 независимо другим агентом. Outbound observer увидел ровно1note-request; paper2→1, ink99→100/count1, одна unfinished NITOR19cells note. Knowledge/RP/warp не меняются; immediate client state до S2C прежний; missing-material/existing-note clicks не отправляют intent. Расходы подтверждены client/server assertions, не PNG.
- Первый client run прерван смертью fixture player от Slime. Только test harness получил entity invulnerability с сохранением survival/instabuild=false. Failed owned world сохранён, повтор прошёл на свежей isolated copy; production gameplay/AI не менялись. После central validate менялся только clientTest, который заново скомпилирован и исполнен успешным client2.
- Evidence: [verification.json](porting/verification.json), [client-smoke-verification.json](porting/client-smoke-verification.json). Active main Java61; legacy934=23partial/911pending/0complete, scope1044needs_review. Packaged runtime/remote multiplayer/NeoForge primary client/full visual parity остаются unverified.

Новые primary tooltips проверены только EN/Fabric GUI640×360; в13 рамка близко к правому краю. Table/edit/combine/copy/unknown/full config/clues и остальные M00–M15 не закрыты этим checkpoint.

## Сборка и источники

| Компонент | Версия |
|---|---|
| Minecraft / Java | 1.21.1 / 21 |
| Gradle / Loom | 8.12.1 / 1.11.458 |
| Architectury plugin / API | 3.4.162 / 13.0.11 |
| Shadow | 8.1.1 |
| Fabric Loader / API | 0.16.14 / 0.116.17+1.21.1 |
| NeoForge | 21.1.259 |
| Локальный alpha artifact | 0.1.0-alpha.1 |
| Mappings | official Mojang |

Полные версии, SHA-1/SHA-256 оригинального JAR и checksums wrapper/distribution — `porting/source-lock.json`. Gameplay specification — original TC4 4.2.3.5, приватный oracle вне репозитория. Пины референсов сохранены: OpenTC4 `80aac29c52543ef51766c4b0788fba782abb3b0b`, Architectury `1509b3192910aaca0af8bba1b55d189a675010bf`. Свежий Architectury upstream `3a836a39c416a4449e825c519dacd50a5fc9eb32` проверен отдельно; его 45 изменённых файлов не меняют целевую версию 1.20.1 и незавершённость research/GUI. Baseline не заменён автоматически.

## Реализованные участки

| Область | Реализовано | Остаётся |
|---|---|---|
| Реестры и данные | Нативные common/Fabric/NeoForge, 48 аспектов, 201 research definition, components | Полная addon API и весь контент |
| Знания | Player-owned state, save/load, clone/respawn/dimension lifecycle, S2C sync | Большие истории выше payload limit, полный multiplayer/adversarial audit |
| Таумометр | Серверная цель, 20 стабильных тиков, блоки/предметы/обычные узлы; reward scaling и duplicate protection | Существа, clue triggers, complex/recipe-derived object aspects |
| Жезл и узел | Wood/iron: 25 vis/primal; centivis, original rounding, тестовый узел | Node ecology/types/recharge/worldgen, фокусы, rods/caps/assembly |
| Банка и фиалы | Один аспект, банка64/фиала8, drop data component | Labels, void/brain/node variants, transport, полный BlockItem.place scenario |
| Таумономикон | Жезл превращает bookshelf в один normal book без vis cost; floating fire-resistant result; S2C-authorized open; original siblings side effects | Creative cheat sheet/config modes, остальные wand triggers |
| Карта исследований | Original coordinates, категории, normal/hidden parents, Hidden/Lost @ clues, Concealed, Virtual, ELDRITCHMINOR gate, primary note action | Оригинальные item-backed icons/анимации, config modes |
| Покупка Secondary | Default difficulty=0/wuss=false, exact aspect costs, полный server preflight, atomic debit/completion/warp, direct siblings, corrective S2C и EN/RU feedback | Native config/difficulty modes, создание clues |
| Записки | Сохраняемый component, original generator, pure solver, normal/discovery learning, inkwell/3 recipes, выдача из книги с paper/ink/dedup/drop | Интерактивный стол, edit/combine/copy, unknown scrolls и full research modes |
| Страницы книги | Завершённые text и gated text, BR/LINE/29 embedded IMG, overflow pagination, EN/RU | Native recipe/aspect/legacy-expression pages, полное visual parity |
| Ресурсы | Оригинальные PNG/OGG, 42 native item models, локализация, mip guard4 | Полные TC4 renderers, FX/UV/animation parity |

Research catalogue и 273 recipe references не являются исполняемыми рецептами. Из 591 descriptors каталог содержит 310 text descriptors (15 gated), 273 recipe references и 8 legacy expressions. Conditional definitions пока скрыты до native config/compat resolver; исходный `if (Config...)` хранится только как provenance.

Object-aspect registry validation: 137 accepted, missing item/block IDs 0, conflicts 0. Это только уже разрешённые unconditional literal bindings.

## Предыдущая итерация: данные записок и изучение открытий

- R01: immutable note component, ordered cells, persistent JSON/NBT и bounded framed wire codecs; ключи/аспекты/геометрия/дубли/negative copies проверяются до принятия состояния. Это не importer старых сохранений.
- R02: canonical generator для всех172 aspectful definitions, исходные primary ties/insertion order, два HashMap прохода, discarded RNG draw и holes.516 независимых private oracle vectors (172×3seeds) исполнены на Java8/21 и сравниваются целиком в JUnit, включая последующее RNG. В Git включены числовые test vectors/provenance, не private code.
- R03: pure direct-component connectivity, оба discovery gates, все endpoints, pruning с сохранением connected branches. Никаких research/warp/point grants от решения сетки.
- R04/R05: IScribeTools, inkwell max100/stack1 и Scribe1/2/3 native recipes; обычная записка и discovery имеют отдельные identities/models/rarities, solved flag независим. Server item use проверяет normal/hidden parents, изучает transfer note без RP/reader clue/discovery cost, consume1 даже creative, warp/siblings один раз. Общий completion helper сохраняет различие с normal book siblings.
- M00: воспроизводимый metadata [scope ledger](porting/scope-ledger.json):859original+934legacy→1044 union paths. Все1044 semantic verdicts ещё needs_review; это очередь аудита, не завершение M00. Self-test и check прошли.

В настоящем Fabric клиенте generated NITOR19cells решён server fixture до6cells; у reader нет lux. Обычный vanilla useItem учит NITOR и расходует одну discovery после server sync, без optimistic grant. Повторная копия остаётся; pools/discovery, warp, другая записка и ink99 неизменны. Все12 PNG просмотрены root;11/12 показывают читаемые native inventory tooltips и исходные разные note/discovery icons. Интерактивная table puzzle ещё не игралась.

**Остаётся:** R07–R13 table/edit/combine/copy/unknown/clues/config; R14–R16 deconstruction/full scanning/book bindings. Player ink adapter проверен в R06; table integration предстоит. Unknown24/42 формы ещё не зарегистрированы. Этот checkpoint ещё не даёт полного исследовательского цикла.

## Предыдущая итерация: вторичные исследования

Добавлен ограниченный C2S intent с одним canonical key (до128 символов), без клиентских цен/баланса/имени игрока. Sender берётся из connection; обработка и повторная проверка выполняются на server thread. Main completion и прямые siblings собираются на копии знаний, затем применяются одним commit. Отказ не меняет pools, discovered aspects, completion/clues или warp. Повторная покупка не списывает очки и не начисляет warp снова.

Исправлено подтверждённое authority defect оригинального TC4: original packet доверял клиентской проверке полного баланса, difficulty и clues. Native server проверяет их сам; публичный original overdraw quirk `PlayerKnowledge.addAspectPool` сохранён. Все normal/hidden parents обязательны; Hidden/Lost нужен отдельный `@KEY`, который после покупки остаётся. Primary, Virtual, AutoUnlock и unresolved conditional rows не превращаются в покупки.

Независимый review policy сверил original packet/manager/warp; другой reviewer проверил реальные Architectury C2S регистрации, sender/threading, bounds и UI. В настоящих77 unconditional Secondary нет siblings: generic ordered/nonrecursive/free sibling path проверяется synthetic JUnit, а не вымышленными связями каталога. Действует исходный default0/wuss=false; настройки сложности пока не реализованы.

## Сохранённые исправления предыдущей итерации

1. Node scan теперь получает `max(4, vis / 10)` на аспект, как original TC4, вместо всего vis. Сам узел не расходуется.
2. Block scan получает ITEM identity через native clone picking. Поставленная красная пыль и выпавший redstone используют `@minecraft:redstone`, не дают двойной награды.
3. `HexUtils.distributeRingRandomly` сохраняет original quirk: RNG draw расходуется, начальная позиция равна нулю. Точный список endpoints и следующее состояние RNG проверяются тестами.
4. `scripts/dev.ps1` теперь типизирует task array: одноэлементные `test`/`build` больше не распадаются при native splatting.
5. Оба remapped JAR включают актуальные `THIRD_PARTY_NOTICES.md` и `LICENSE` в UTF-8/LF; отсутствие/дубликат/устаревшее содержимое отвергаются проверкой.
6. Устранено повторное размытие новой книги: в 1.21.1 `Screen.render` сам вызывает `renderBackground`; второй вызов после custom content размывал карту/страницы. Проверяется реальными снимками.

Первый запуск новых scanner GameTests упал из-за fixture: игрок стоял за barrier wall. Позиция исправлена внутри 8³ template, добавлена строгая диагностика луча; production assertions не ослаблены. Повторный central validate прошёл.

## Инвентарь и охват анализа

[`porting/class-inventory.json`](porting/class-inventory.json) содержит все **934** legacy main Java paths с SHA-256, строками и явным counterpart/status.

| Статус | Количество |
|---|---:|
| partial | 34 |
| pending | 900 |
| complete | 0 |
| Активные main Java-файлы | 75 |

Scoped partial mappings охватывают IScribeTools/inkwell/notes/data и ResearchManager generator/solver/learning/completion/issuance/ink; type0/type1 network и GUI basis обновлены. Table/unknown/config и полный mutable addon API остаются pending. Совпадение path или успешный тест участка не закрывают весь legacy class; ни один legacy unit не принят полностью.

Пофайловый committed inventory на начало итерации: FOREVA 5562 файла; OpenTC4 2050; Architectury 6129. Для текущего upstream Architectury создан отдельный inventory и delta. Это полная инвентаризация файлов и адресный семантический аудит, не заявление о построчном чтении всех 13 тысяч файлов. Отчёты:

- [Сравнение и пофайловые JSON](analysis/2026-10-15-reference-comparison-ru.md).
- [Аудит всех 35 active main Java до добавления книги](analysis/2026-10-15-native-gameplay-audit.md).
- [Build/packaging/CI audit](analysis/2026-10-15-native-build-audit.md).
- [Предыдущий системный аудит legacy](analysis/2026-10-15-CODE_ANALYSIS_RU.md).

## Проверки предыдущего checkpoint 03fe0782

Central `scripts/dev.ps1 -Task validate` после final source/resource изменений: `.tmp/notes-validate5.log`, exit0, BUILD SUCCESSFUL. Ранние integration failures устранены; проверенная последняя сборка включает полный clientTest source. Клиентский smoke: `.tmp/notes-client1.log`, exit0. Итоговый snapshot — `porting/verification.json`; GUI smoke — `porting/client-smoke-verification.json`.

| Проверка | Проверенный результат |
|---|---|
| Common JUnit | 131 tests, failures/errors/skipped0; включает26 data,6 generator (516vectors),11 solver и9 learning |
| Python tooling | 24 tests, PASS |
| Fabric GameTests | 17 cases, failures/errors0 |
| NeoForge GameTests | 17 cases, failures/errors0; actual encoded knowledge S2C проверен |
| Extractor --check | 48 aspects, 201 researches, 294 ungated text corpus; fingerprints сохранены |
| Resource verify | 492 atlas sprites,64 model/atlas refs,66 sound events,111 sounds,missing0;42 item models |
| Notices в обоих remapped JAR | PASS; актуальные bytes/hashes проверены |
| Fabric native GUI smoke | notes-client1 SCRIPT_PASSED/exit0; все12 PNG просмотрены root, включая note/discovery native inventory |
| NeoForge новая книга/notes в клиенте | Отдельно не проверялась;17 server GameTests проходят |
| Remote GitHub Actions | Не запускались |
| Production remapped JAR в обычном loader install | Не запускался; структурная проверка JAR не заменяет packaged runtime |

GameTests проверяют реальные mapped loaders, registries, mixins, item-use paths, persistence и согласованные state transitions. Четыре новых сценария на каждом loader вызывают общий server receiver body: exact debit/replay, partial funds atomic refusal, clue/hidden parent, warp и native player NBT. Fabric mock observer проверяет состояние; NeoForge декодирует реальный encoded S2C и сверяет его с сервером. Это не доказательство dispatch C2S само по себе.

Настоящий Fabric client smoke подтверждает C2S dispatch из browser click и authoritative S2C: ENCHFABRIC при pools3/2 не меняется, после server setup6/6 клиент при клике ещё не завершён и не списывает очки, затем получает completion/pools3/3 и открывает text. Повторное открытие сохраняет3/3. Scene09 показывает native hover с программными координатами, OS pointer не перемещается; root увидел зелёный Pannus3/3, красный Praecantatio2/3 и полный отказ без обрезки. Recipe pages этим не проверялись.

Это development runtime. Отдельный Fabric `clientTest` source set не входит в normal/shadow/remapped JAR. Существующие common/loader GameTest helpers всё ещё находятся в main — известное ограничение alpha.

Исходные dev worlds не изменяются: smoke работает только с отдельной копией `fabric/run-clienttest/saves/book-smoke`, исключая session.lock. Скриптовый SUCCESS сам по себе не доказывает visual parity: нужны просмотренные PNG.

## Явные отклонения и ограничения

- UI использует исходную карту/тексты/иллюстрации; item-backed research icons пока отображают book fallback, connections упрощены. Полного визуального соответствия нет.
- Bookshelf trigger использует native ENCHANT particles вместо original BlockSparkle packet. Звук wand и исходный предмет сохранены. Focus guard будет подключён вместе с фокусами; сейчас у зарегистрированного wood/iron wand focus отсутствует.
- Special-item gravity адаптирована отменой native gravity вместо сложения +0.04 до vanilla -0.04. Floating/no-fire behavior проверяется отдельно.
- `HexUtils.toCubicHex` сохраняет ранее принятое исправление round-trip defect; impossible radii/distributions ограничены. Это документированное исключение к сохранению original quirks.
- Sibling order в extracted catalogue отличается от старого HashMap, но corpus test доказал отсутствие chains и warp у трёх targets; один live pass сохраняет результат pinned TC4.
- Full knowledge snapshots ещё не разделяют слишком большие истории; C2S knowledge import не существует.
- Purchase admission и полный баланс проверяются сервером; это намеренное исправление подтверждённого original authority defect. Native difficulty/wuss config и автоматическая выдача clues ещё отсутствуют; clue gate проверен через test setup.
- На Windows dev transformer пишет пойманный nonfatal `StringIndexOutOfBoundsException` в duplicate cleanup из-за path separator. Запуск продолжается; это не исправлялось speculative upgrade.

## Следующая последовательность

Полный маршрут, охват original/legacy, зависимости, параллельные задачи и критерии приёмки: [MIGRATION_PLAN.md](MIGRATION_PLAN.md). План не повышает статусы units и не является evidence реализации.

1. R07 [research table](analysis/2026-10-15-research-table-spec.md): craft/place/conversion/break, shared inventory/menu, save/sync и renderer. Публичный доступ без нового owner lock; distinct GUI/automation predicates и8-block main distance.
2. R08–R13 hex edits/combine/copy/unknown, [native difficulty/config и clue triggers](analysis/2026-10-15-research-unlock-spec.md). Проверить source topology quirks, chunk boundary и teardown output failure; обычная пара столов не закрывает их.
3. Native recipe registries и arcane/crucible/infusion devices; книга должна показывать тот же рецепт, который исполняется устройством.
4. Node lifecycle/worldgen, foci/upgrades, essentia devices, golems, mobs, taint и Outer Lands.
5. Поэтапный client/render audit и packaged-JAR/modpack/multiplayer checks.

Полный перенос остаётся незавершённым. Работа локальная; public push/release не выполнялись. Права original TC4 code/assets не меняются лицензией template reference. Атрибуция — `../THIRD_PARTY_NOTICES.md`.
