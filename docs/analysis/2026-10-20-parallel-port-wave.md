# 2026-10-20 — параллельная волна переноса: 20 субагентов, 3 волны

## Что было

Ветка `work` стояла на `03a25b9` (после десяти Alt-рецептов тигля). Пользователь остановил
вторую, параллельную линию (Claude) и передал весь объём порта этой линии. Дальше работа шла
тремя волнами субагентов, каждая в своём `git worktree` с непересекающимся списком файлов.

## Окружение

- JDK 21 (Temurin 21.0.12) установлен в `/home/Acak/.jdk21` — Gradle 8.12.1 требует
  `languageVersion = 21`, а в системе был только JDK 25.
- `GRADLE_USER_HOME` разведён по агентам через `cp -al` (hardlink-копии): общий кэш Loom
  портили друг другу при параллельной сборке (`FileAlreadyExistsException` на
  `neoforge/21.1.259/minecraft-merged-mojang.jar`).
- Оригинальный 1.7.10 JAR декомпилирован CFR 0.152 в `/home/Acak/work/thaumcraft-4.2.3.5-oracle`
  (859 классов). Декомпиляция **проверена**: `scripts/porting/extract_tc4_definitions.py --check`
  воспроизводит хэши `research_key_category_sha256=397d6208…` и `text_page_sha256=66a10cd3…`
  ровно, то есть oracle совпадает с приватным oracle проекта.
- `javap`-дамп API Minecraft 1.21.1 (71 852 строки) в `/home/Acak/mcapi` — чтобы агенты не
  угадывали сигнатуры 1.21.1.

## Волна 1 — перенос контента (20 агентов)

| Агент | Подсистема | Ключевой результат |
|---|---|---|
| `eldritch` | Эльдрич-бестиарий | Warden, Golem, Crab, InhabitedZombie, Watcher, ThaumicSlime, Giant Taintacle + общий `EntityThaumcraftBoss` |
| `cultists` | Культисты | Knight, Cleric, Praetor, портал, увеличенный портал, 27 правил |
| `champions` | Champion-модификаторы | 13 модификаторов, таблица приоритетов, адаптер |
| `taintanimals` | Заражение | 5 заражённых животных, crawler, seed, seed prime |
| `apisurface` | Публичное API | ~50 классов `thaumcraft.api.*`, 55 тестов |
| `clientmodels` | Клиентские рендереры | 22 рендерера + 9 моделей |
| `gearmodels` | Клиентские модели | Robe, FortressArmor, Knight/Leader, HoverHarness, Wand |
| `eldritchblocks` | Эльдрич-блоки | 20 блоков, 24 тайла, правила, 39 тестов |
| `legacytiles` | Оставшиеся блоки/тайлы | ArcaneFurnace, DeathFluid, Cosmetic, WandPedestal, 33 теста |
| `devices` | Контейнеры | 15 контейнеров/слотов, 26 правил |
| `worldgen` | Outer Lands | Измерение, лабиринт, комнаты, 28 тестов |
| `villages` | Деревни | Banker/Wizard, hilltop stones, eldritch ring, 37 тестов |
| `recipes` | Каталог рецептов | 232 ключа оригинала, guard-покрытие |
| `equipment` | Предметы | BoneBow, CrimsonSword, PrimalCrusher/Arrow, HandMirror, Key, 18 тестов |
| `runicbaubles` | Аксессуары | Рунные амулет/кольцо/пояс, броня культистов, 9 тестов |
| `golemai` | AI големов | 40 целей + `GolemAiRules`, 26 тестов |
| `projectiles` | Снаряды | BottleTaint, GolemOrb, PrimalArrow, FollowingItem, Grate, 29 тестов |
| `utils` | Утилиты | EntityUtils, InventoryUtils, BlockUtils, CropUtils, 15 тестов |
| `enchantcraft` | Зачарования | Frugal, Potency, WandFortune, Sceptre, 14 тестов |
| `biomes` | Биомы | Eerie, Eldritch, MagicalForest, Taint, `BiomeHandler`, 12 тестов |
| `guiclients` | GUI | GuiMagicBox, GuiHandMirror, GuiGhostSlots, 11 тестов |

## Волна 2 — починка (5 агентов)

- `cosmeticfix` — два конфликтующих дизайна семейства cosmetic-блоков сведены к одному.
- `assetswave` — ~30 блоков эльдрич-семейства получили blockstate, модель, лут и имена.
- `gametestwave` — серверные GameTest-сценарии на новый контент.
- `runtimefix` — запуск выделенного сервера и починка рантайм-ошибок.
- `golemaiwire` — проверка, что нативный AI големов уже содержит оригинальные цели один-в-один.

## Волна 3 — закрытие отложенных пробелов (8 агентов)

Ключевое наблюдение: каждая волна 1 честно перечислила, что она **не** портировала, потому что
контента ещё не было. Волна 3 закрыла именно эти пункты:

| Агент | Закрыто |
|---|---|
| `gaprecipes` | 15 рецептов оригинала (arcane door, ключи, bone bow, ethereal bloom, bucket of death, bottle of taint, brain jar, зеркала, hand mirror, sanity checker, compass stone, primal crusher, wand pedestal) |
| `gapdevices` | Magic Box → реальное меню, Hand Mirror → реальный `TileMirror` + транспорт, item filter → `EntityItemGrate` |
| `gapprojectiles` | Bone Bow → `EntityPrimalArrow`, бутылка заражения → реальный снаряд, bucket of death → реальная жидкость, Key → `TileOwned` |
| `gapcultists` | Реальная броня культистов, ритуалист → настоящий `TileEldritchAltar`, портал → настоящие баннеры/ящики |
| `gapchampions` | «Опасная локация» → реальные клетки лабиринта, правило босса, награда за смерть чемпиона |
| `gapouterlands` | Reflection → типизированные вызовы, контракт `variant`, телепорт в измерение |
| `gapclientblocks` | Клиентские рендереры эльдрич-блоков |
| `gapintegration` | Конверсия при смерти заражённых, аура, cargo големов, ore-теги, публичный API |

## Найденные и исправленные расхождения с оригиналом

1. **Два конфликтующих дизайна cosmetic-семейства.** Одна волна сделала блок на метаданные,
   другая — блок на группу твёрдости с состоянием `variant`. Оригинал (`BlockCosmeticSolid`) —
   **один** блок с 16 метаданными и единой твёрдостью 2.0 (`func_149711_c(2.0f)`), поэтому
   семейство переведено на один блок с `variant` 0..15.
2. **Дубли registry-id**, которые уронили бы игру при старте: `eldritch_stairs` (два класса),
   `wand_pedestal` (два block entity type), `magic_box` (два menu type), `greatwood_stairs` /
   `silverwood_stairs` (владеет верхняя линия).
3. **`ThaumcraftEldritchBlocks.register()` нигде не вызывался** — всё эльдрич-семейство блоков
   было незарегистрированным, а `ThaumcraftLegacyItems.register()` — тоже.
4. **`ChampionHooks.init()` нигде не вызывался** — вся система чемпионов была неактивна: ни один
   моб не проходил бросок.
5. **Единый снаряд `EntityGolemOrb`.** В оригинале он общий для эльдрич-голема и культистов;
   две волны сделали по вложенному классу, что давало бы два id `golem_orb`.
6. **Ключ писал `"Steve"` вместо `"0Steve"`.** Оригинал пишет метаданные ключа в список доступа.
7. **Приоритет целей.** 1.21.1 `GoalSelector` отдаёт победу **наименьшему** номеру, как и 1.7.10 —
   инверсия приоритетов была бы ошибкой.

## Итог

- `:common:compileJava` — чисто.
- `:common:test` — 1494 теста, 0 падений (кроме четырёх, которые чинит `cosmeticsingle`).
- `python3 -B scripts/porting/convert_assets.py --verify` — 0 missing references.
- `python3 -B -m unittest discover -s scripts/porting` — 84/84 OK.
- Каталог рецептов: 247 оригинальных ключей закрыты, 19 отложены (11 — нужны чужие руды,
  8 — требуют состояния блока, которое рецепт в 1.21.1 не умеет нести).

## Не сделано (честно)

- Клиентский визуальный осмотр, multiplayer, полная survival-прогрессия и NeoForge-паритет
  не подтверждены этим чекпойнтом.
- Два GameTest зависят от случайного сида мира (`pechoriginaltradeusesnativegameplay`,
  `magicalbiomesgeneratenaturallyandspawnactualpech`) — это известно в `docs/analysis/claude-lane.md`.
