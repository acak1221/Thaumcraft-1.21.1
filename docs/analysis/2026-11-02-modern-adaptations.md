# Современные адаптации к Minecraft 1.21.1 — alpha.17

Владелец одобрил семь расширений базового Thaumcraft после обзора alpha.16. Ниже описана
реализованная область для Fabric 1.21.1. Это новые возможности для современного мира,
а не заявления о том, что оригинальный TC4 содержал эти предметы, таблицы или цены.

**Состояние проверки:** финальные Fabric build/isolation, common JUnit, 11 focused native
checks и обычный production-server — **PASS**. Финальный EN/RU client с reload —
**SCRIPT_PASSED**, 25 кадров и Gradle PASS; **все 25 кадров осмотрены — PASS**.
Результаты прежних alpha не используются как доказательства alpha.17.

Канонический корпус **201 исследования** и **24 исходных рецепта зачарования наполнением**
сохранён. Два исследования зарегистрированы отдельно от корпуса: live research total — **203**.
Refined Thaum и другие аддоны в эту реализацию не входят.

## 1. Таумометр и современные мобы

`EntityAspectBindings.baselineTags()` дополняет исходные строки отдельными данными
`data/thaumcraft/port/modern_entity_aspects.json`. Исходные строки и их варианты сохранены.
Новые значения — выбранный баланс по свойствам мобов, с пояснениями в JSON:

| Моб | Аспекты |
|---|---|
| Breeze | aer 3, motus 2, tempestas 2 |
| Bogged | exanimis 3, humanus 1, terra 1, venenum 2 |
| Warden | bestia 4, sensus 4, tenebrae 4 |
| Allay | volatus 2, spiritus 2, praecantatio 2 |
| Sniffer | bestia 3, herba 2, sensus 2 |
| Frog | bestia 2, aqua 2, limus 1 |
| Armadillo | bestia 2, tutamen 2, terra 1 |
| Axolotl | bestia 2, aqua 2, sano 1 |
| Goat | bestia 3, telum 1, aer 1 |

Таблицы попадают в обычный публичный реестр однократно. Регистрации аддонов, явные null tags
и удаление строк после инициализации остаются авторитетными; скрытого восстановления baseline
при сканировании нет. Используются существующие native сигнатуры вида
`@entity:minecraft:frog`, знания родителей аспектов, 20-тактовая сессия таумометра, начисление
очков и запрет повторной награды. Аспекты современных предметов уже имелись; их inference
для этой возможности не переписывался.

`ModernEntityAspectBindingsTest` и `ModernEntityScanRuntimeChecks` проверили сохранность
исходных строк, все девять native типов, override/null/removal, настоящий held-use скан Frog,
отказ без знаний, точную награду и отказ повторной сессии — PASS в финальных unit/native runs.

## 2. Современные культуры и големы

`GolemCrops`, `GolemReplant`, `GolemHarvestPlatform` и существующий Harvest-путь голема
расширены пятью конечными группами: **свёкла, факельник, кувшинница, сладкие ягоды,
светящиеся ягоды**. Это конкретные native растения, а не допуск любого стороннего CropBlock.

Selector проверяет зрелость; у факельника распознаётся и конечный цветок, у кувшинницы —
зрелая нижняя половина вместе с корректной верхней. Order использует реальные предметы
семян/стручков в исходной области поиска 6 блоков вокруг голема (AABB) и существующий
защищённый путь посадки. Взрослые факельник и кувшинница по native правилам не роняют
семена/стручки: их нужно предоставить рядом; без них собранная клетка остаётся пустой.
Проверка отдельно проходит обе ситуации и расход предоставленного предмета. Ягоды собираются без
выкорчёвывания: сладкий куст возвращается к age 1, cave vines теряют berries flag.
Ягодная ветвь не создаёт семена и не запускает повторную пересадку; учитывается doTileDrops.
Проверки доступности чанка, границ и разрешения на изменение остаются частью harvest.

`ModernGolemCropsRuntimeChecks` — PASS в финальном native run. Визуальный клиентский
осмотр остаётся отдельной проверкой.

## 3. Магические находки в современных структурах

`ModernStructureLoot` и Fabric loot hook дополняют **14** ванильных таблиц:

- Ancient City — одна таблица;
- Trial Chambers — entrance, corridor, intersection, intersection_barrel и supply;
- vault — только верхние reward и reward_ominous;
- archaeology — desert_pyramid, desert_well, ocean_ruin_cold, ocean_ruin_warm,
  trail_ruins_common и trail_ruins_rare.

Прежние десять chest categories остаются самостоятельными. Для сундуков/vault добавляется
один необязательный bonus pool с фрагментами знаний, salis mundus или сумками согласно группе;
ванильные пулы и гарантированные награды сохраняются. Nested vault tables не изменяются,
чтобы не умножать бонус. Археология получает weight-one фрагмент в существующем первом pool,
сохраняя один native результат brushable block. Редкость и состав — новые balance choices.

`ModernExplorationLootRuntimeChecks` проверил итоговые live loot tables — PASS.
Изменение hook реализовано на Fabric; отдельная NeoForge parity здесь не заявлена.

## 4. Строительные семейства Greatwood и Silverwood

`ThaumcraftWoodwork` добавляет по десять блоков на породу: дверь, люк, забор, калитку,
обычную и настенную таблички, подвесную и настенную подвесную таблички, кнопку и нажимную
плиту. Итого **20 блоков, 16 предметов и 16 crafting recipes**: настенные формы используют
общие предметы табличек. Добавлены blockstates/models/loot/tags, creative entries,
горючесть и топливо; исходные брёвна и их поведение с узлами не меняются.

Таблички используют native sign/hanging-sign block entities, редактирование текста и
соответствующие клиентские renderer/editor integrations. Материалы используют существующую
магическую древесину. Лодки и обтёсанные брёвна в этот набор не включены.

В ходе визуальной подготовки исправлено наложение UV табличек: native entity unwrap 64×32
не должен проецироваться через block sprite магических досок. `WoodworkSignModels` сохраняет
native форму, но задаёт каждому лицу UV в пределах назначенного sprite; standing board учитывает
native масштаб 2/3. Подвесная доска и настенная опора используют свои bounded UV, а цепи —
отдельный vanilla alpha sheet с native геометрией и видимостью крепления. `WoodworkSignModelUvTest`
проверяет реальные отрисованные вершины/UV: **2/2 PASS** в финальном common run. Оба door item
model содержат обе половины (32 model pixels), а не только нижний блок двери.

`WoodworkRuntimeChecks`: native размещение, sign data и оплата рецептов — PASS. Осмотр
итоговых моделей, текста, цепей, предметов дверей и reload в настоящем клиенте — PASS.

## 5. Камнерез

Пять native `minecraft:stonecutting` recipes дополняют существующий crafting:

| Вход | Выход |
|---|---|
| Arcane Stone | Arcane Stone Bricks ×1 |
| Arcane Stone | Arcane Stone Brick Stairs ×1 |
| Arcane Stone | Arcane Stone Brick Slab ×2 |
| Arcane Stone Bricks | Arcane Stone Brick Stairs ×1 |
| Arcane Stone Bricks | Arcane Stone Brick Slab ×2 |

Способ получения исходного магического камня сохранён. Ресурсы лежат в
`data/thaumcraft/recipe/stonecutting/`; `ModernUtilityRuntimeChecks` проверил реальное menu
и расход материалов — PASS.

## 6. Кузнечные узоры на нестандартной броне

Уточнение предварительного обзора: обычная магическая броня **уже поддерживала native
smithing/trim admission и обычный trim renderer**. Новая работа добавляет видимый слой
на нестандартных моделях крепостной брони и пустотных роб, используя native atlas/material
узора. `FortressArmorTrims`, `RobeArmorTrims` и модели trim повторяют позы исходной брони;
маски, очки, окрашенная ткань и исходные характеристики остаются в прежних путях.

`ArmorTrimRuntimeChecks` провёл через настоящий SmithingMenu **17 частей
брони**: thaumium 4, void 4, fortress 3, thaumaturge robes 3 и void robes 3. Он проверяет
preview, получение результата, расход шаблона/материала, сохранность остальных components,
save/load, отказ повторного того же узора и неверного ингредиента — PASS. Визуальный осмотр
готовых слоёв на настоящем клиенте — PASS.

## 7. Современные зачарования через наполнение

В `data/thaumcraft/recipe/infusion_enchantment/modern/` добавлены **12 рецептов**:

- `MODERNINFUSION`: loyalty, impaling, riptide, channeling, multishot, piercing,
  quick_charge, density, breach;
- `DEEPINFUSION`: wind_burst, soul_speed, swift_sneak.

`ThaumcraftModernResearch` регистрирует обе ветви ARTIFICE рядом с исходными исследованиями.
Первая требует `INFUSIONENCHANTMENT`, вторая — первую и скрытого родителя `ELDRITCHMINOR`.
Добавлены отдельные текстовые страницы и recipe pages/значки для EN/RU. Ингредиенты, аспекты,
instability и first-level minimum cost новых рецептов — явно современные balance choices.
Существующий native движок продолжает определять применимость, максимальный уровень,
несовместимые зачарования и оплату настоящего infusion; исходные 24 рецепта не заменяются.

`ModernEnchantmentInfusionRuntimeChecks` и native research check проверили live recipes,
исследования/recipe pages, совместимость/лимиты и оплату настоящего infusion — PASS.
Осмотр новых страниц и значков в клиенте — PASS: все 12 рецептов на семи разворотах.

## Проверка и границы

| Проверка финальных входов alpha.17 | Статус этой записи |
|---|---|
| Fabric compile/package и production/test isolation | PASS, `.tmp/modern-adaptations/build-final.log` |
| Common JUnit | PASS: 2874 tests, 0 failures/errors, 5 прежних skips; `.tmp/modern-adaptations/unit-final.log` |
| Focused native GameTests семи адаптаций | PASS: 11/11, `.tmp/modern-adaptations/gametest-3.xml` |
| Настоящий клиент EN/RU, native scan/menu/use, книга, модели и resource reload | PASS: client4 SCRIPT_PASSED, 25 captures, Gradle PASS |
| Осмотр игровых кадров и запись точных fixtures | PASS: все 25 кадров; root 3 + wood 6 + armour 5 + client 11 |
| Обычный production-server, save/stop | PASS: ready, ticks 7→57, normal stop, exit 0 |
| Финальный JAR и SHA | Зафиксированы ниже; совпадают с production inputs |

Финальный артефакт: `Thaumcraft-fabric-0.1.0-alpha.17.jar`, **15 955 736 байт**,
SHA-256 `a67c0b233c5b4f69d9f6b1c7a789d4f7946e46a09d86c2d8387af7693d568913`.
Обычный сервер запускался с этим JAR, Fabric API 0.116.17+1.21.1 и Architectury Fabric 13.0.11;
точные пути, размеры и SHA всех трёх модов записаны в
`.tmp/modern-adaptations/production/production-inputs.json`, результат — в соседнем
`production-result.json`. Test harness в production JAR отсутствует согласно isolation check.

Два ранних native receipt не приняты: `gametest-1.xml` содержит отказы fixture из-за foreign
block entity и bounded проверки archaeology loot; `gametest-2.xml` — несовпадение
подготовки ягодной фазы. Финальный `gametest-3.xml` после корректировок fixture — 11/11 PASS.
Первый client probe `modern-adaptations-2026-11-02T10-19-21.980971972Z` завершился timeout
на stage 24 (12 captures) и также **отклонён**. Его частичные checks и кадры не объявляются
приёмкой финального клиента; исходный failed receipt сохранён в
`.tmp/modern-adaptations/client-1-result.json`.

Финальный client4 `modern-adaptations-2026-11-02T10-34-32.410750373Z` завершил script
с **SCRIPT_PASSED**, Gradle PASS и **25 captures**. Receipt:
[docs/porting/modern-adaptations.json](../porting/modern-adaptations.json).
Использован свежий flat world 1.21.1 с явно выданными woodwork, текстами табличек,
armor stands/components, знаниями аспектов/исследований и Frog; это не survival acquisition.
Автоматические проверки подтверждают EN/RU имена 16 woodwork items, сохранение typed text
`Synced` на сервере в обоих native sign editors, use двери, glow/wax текст, вызовы trim layers
с ожидаемыми sprite, фактическую запись `@entity:minecraft:frog` после timed scan и native
разрешение всех 12 recipe IDs в семи новых book spreads. **Все 25 кадров client4 осмотрены
root и тремя агентами — PASS**: root 3 кадра, wood 6, armour 5, client 11. Проверены все
16 woodwork icons и обе семьи, standing/hanging sign editors, trim поверх крепостных масок/
очков и окрашенных void robes, три стадии настоящего сканирования Frog, crop fixture и семь
новых разворотов со всеми 12 рецептами и значками.

Client2/3 также успешно завершались SCRIPT_PASSED и использованы для диагностики читаемости
кадров. В итоговом client4 подсказка не закрывает woodwork inventory, а инструмент не закрывает
crop fixture. Эти изменения подготовки кадров не меняли production-код или JAR; финальные
картинки и их SHA относятся исключительно к client4. Экспорт повторно подтвердил тот же SHA JAR.

## Галерея финального клиента

- [Обе семьи магической древесины](../images/modern-adaptations/en-woodwork-both-families.png).
- [Крепостная броня: diamond/sentry и gold/dune](../images/modern-adaptations/ru-fortress-trim-diamond-sentry-gold-dune-close.png).
- [Последний разворот Deep Infusion](../images/modern-adaptations/ru-deep-infusion-native-book-last.png).

[Все 25 кадров с SHA, точные входы и итоговые проверки](../porting/modern-adaptations.json).

Результат клиента не выводится из headless tests. Полное survival acquisition, multiplayer и новая NeoForge приёмка
этой записью не заявляются. Глубинная киноварь/янтарь, лодки, обтёсанные брёвна,
скалк-датчик и новый воздушный фокус остаются за пределами семи согласованных адаптаций.

Связанные записи: [HANDOFF](../HANDOFF.md), [MIGRATION_STATUS](../MIGRATION_STATUS.md),
[KNOWN_DEVIATIONS §17](../audit/KNOWN_DEVIATIONS.md#17-modern-minecraft-adaptations--owner-request-2026-11-02).
