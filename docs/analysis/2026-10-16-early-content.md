# Ранний игровой контент: руды, R09, обычные узлы и arcane

Эти units продолжают существующий native порт. Полные M01–M04 и весь TC4 этим не закрываются. Original TC4 4.2.3.5 private oracle — спецификация; код нового EugeneHeisst reference не копировался.

| Unit | Native counterpart | Original anchors |
|---|---|---|
| Восемь руд, добыча, smelting и генерация | `ThaumcraftOres`, `BlockCustomOre`, `OreDropCount`, `ThaumcraftOreFeature`, `ThaumcraftWorldgen` | `BlockCustomOre:57–72,132–154`, `ConfigBlocks:239–240`, `ConfigRecipes:485–486`, `ThaumcraftWorldGenerator:481–546` |
| Объединение аспектов и окружение стола | `ResearchAspectCombination`, combine payload/receiver, `TileResearchTable`, `GuiResearchTable` | `PacketAspectCombinationToServer:90–124`, `ScanManager:420–440`, `TileResearchTable:115–122,189–290`, `GuiResearchTable:155–208,231–274,540–605` |
| Natural NORMAL nodes и lifecycle | `ThaumcraftNodes`, `ThaumcraftNormalNodeFeature`, `TileNode`, `TileNodeRenderer` | Подробно в [M03 notes](2026-10-16-M03-normal-nodes.md) |
| Arcane worktable и два рецепта | `ThaumcraftArcane`, `TileArcaneWorkbench`, `ContainerArcaneWorkbench`, `ArcaneShapedRecipe`, `ArcaneVis`, GUI/BER | `BlockTable:224–238`, `ContainerArcaneWorkbench:60–69`, `SlotCraftingArcaneWorkbench:40–65`, `ConfigRecipes:223,256`, `ConfigItems:302`, `ItemWandCasting:306–335` |

## Сохранённые правила и явные адаптации

- Руды: 18 одиночных cinnabar, 20 amber, 8 infused veins size6 на chunk; первичный uniform выбор с original type-uniform biome affinity. Не взяты donor depth weighting, 36 cinnabar attempts или дополнительные deepslate veins. Native biome tags заменяют Forge dictionary. Замена по умолчанию — только stone.
- Y остаётся zero-anchored. `actualHeight/5` при native span384 даёт cinnabar Y0–75; infused использует `nextInt(max(5,surface−5))`, amber — surface−`nextInt(25)`. Это явная адаптация высоты, не тождество мира/seed1.7. Добавлены thin Fabric hook и NeoForge biome modifier.
- Drops сохраняют original Fortune/world-RNG формулы и отдельные singleton shard stacks; XP использует независимый RNG и подавляется Silk Touch. Только cinnabar/amber требуют iron tier. Два smelting outputs дают1 item, XP1.0, vanilla furnace200 ticks.
- Combine: RP-first, затем реальный shared bonus; нет ink/paper/Researcher discount. Новый compound даёт3 RP, известный1; неверная пара расходует оба входа. Ecology сохраняет 602 ticks, short-circuit RNG, scan order x→z→y и `merge=max(existing,1)`; неизвестные будущие brain jars не подменяются обычной банкой.
- **Исправление original authority defects:** original packet принимает client `ab1/ab2` и достаточно проверяет каждый input до обоих дебетов. Поэтому ложный bonus flag позволяет бесплатный вход, а same-aspect RP1 проходит обе исходные проверки и оплачивает только один из двух входов. Native sender/menu/session/revision и staged sequential debits требуют реальные два средства. Это намеренное закрытие reproducible packet exploits, не новое снижение обычной стоимости. Runtime helper содержит RP1 refusal и RP1+bonus1 acceptance.
- Узлы: отдельные current/base, compound retention, recharge/prune/death и normal bullying; special rolls пропускаются без reroll. Нет обещания special ecology, capture/jars, всех tiers или полного visual parity.
- Независимый source review R09/normal nodes не нашёл другого high-confidence defect. Выявлен прежний hardcoded wand ray10/centre11: acquisition и continued use теперь используют native player block-interaction reach, как original Forge reach query. Это восстановление дальности, а не новое усиление жезла.
- Arcane: vanilla recipe имеет приоритет; native recipe manager используется сервером. Полный wand transfer из base0/1 бесплатен и сохраняет компоненты. Iron modifier1.1F с original truncation, повторная проверка research/vis перед выдачей. Enchanted Fabric — `ENCHFABRIC`,1vis каждого primal; Warded Jar — `DISTILESSENTIA`,1aqua. Vis Filter не добавлен без silverwood planks.
- Original прямоугольный worktable PNG128×64 сохранён. Для baked item model добавлен отдельный full-image atlas sprite, как для уже работающих table/restable; растяжение/обрезка PNG не производится.

## Границы проверки

`early-content-validate3` прошёл:192JUnit и47 GameTests на каждом loader, оба JAR,
3149 unchanged inputs. `early-content-client2`:26captures/SCRIPT_PASSED, root
inspected exact04/12/21–26. Итоги/hashes: `MIGRATION_STATUS.md`,
`porting/verification.json`, `porting/early-content-client.json`.

Client scenario использует supplied материалы, RP и six-primal node fixture. Его genuine GUI/C2S actions не являются полным survival progression verdict. Отдельный runtime scenario проверяет fresh native noise chunks → ore drops → действующий thaumometer recipe.

Первый `early-content-validate1`: compilation/JAR build и46 из47 Fabric scenarios прошли; fresh-noise check не нашёл свой test dimension. Vanilla `GameTestServer.create` строит dimensions из `minecraft:flat` с пустым LevelStem registry и игнорирует standalone dimension JSON. Исправлен только test preset: vanilla flat Overworld/Nether/End сохранены, добавлена отдельная normal-noise dimension. Production worldgen ради теста не менялся; требуется повторный полный запуск.

Повторные validate2/3 подтвердили все8руд в реальных noise chunks обоих loaders.
Client1 дал script PASS, но визуальный review PNG04 нашёл невидимые shards.
Native `ItemRenderer.renderQuadList` читает alpha цвета: legacy RGB имеет alpha0.
Shared item tint handlers теперь возвращают opaque ARGB; client2 exactPNG04
показывает оба входных осколка. Filled phials/ore item contexts отдельно не осмотрены.

Далее: базовая алхимия/тигель и recipe-derived scanning; silverwood/greatwood и Vis Filter; R10 после решения native count100/127 lifecycle. Retrogen/configuration, все recipe bindings, special nodes и остальные M00–M15 остаются открытыми.
