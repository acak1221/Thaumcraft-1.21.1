# Магическая древесина → перегонка эссенции → первые улучшенные жезлы

Следующий content slice после проверенного `m05-validate4`/`m05-client2`.
`distillation-validate5`exit0:200JUnit/58+58GameTests/обаJAR; Client5 SCRIPT_PASSED
40captures, root exact33–40 inspected. Source-bound recorder PASS3397unchangedinputs,
48PythonPASS; `porting/verification.json` и `porting/distillation-client.json`.
Полные M02/M04/M05 не закрываются.

## Новое содержимое

- Восемь native вариантов: greatwood/silverwood logs, planks, leaves, saplings.
  Original двухъярусный greatwood/2×2 root и cross-trunk silverwood, не окрашенный
  vanilla oak. Саженцы растут через original tick gates и не принимают bone meal;
  листья имеют separate decay trigger, native distance и исходный sapling loot.
  Один log даёт4 matching planks, обе породы плавятся в настоящий charcoal.
- Один природный feature выполняет original silverwood `nextInt(60)==3`, затем
  greatwood `nextInt(25)==7`; не два независимых features с другим draw order.
  Fabric biome hook и NeoForge biome modifier добавляют его один раз на
  `VEGETAL_DECORATION`. Legacy biome types адаптированы явными native tags;
  специальные ещё отсутствующие биомы не подменены vanilla.
- Arcane stone/bricks/stairs/slab с реальными original recipes и count9/4/4/6.
  Камень тратит terra1+ignis1 и8stone+1shard, исследование ARCANESTONE.
  Native stair corners/half-double slabs, mining/beacon/structural tags.
- Vis Filter:2 за gold/silverwood plank/gold, ordo5+aqua5, DISTILESSENTIA.
- Нормальная алхимическая печь: два настоящих input/fuel slots, буфер50,
  `aspectTotal*10*(1-.125*bellows)` с float→int truncation. Alumentum fuel6400
  ускоряет **вывод** с40 до20ticks, не сокращает smelting time вдвое.
  Уже горящее топливо убывает и при невозможности плавки. Печь не даёт item-result.
- До пяти последовательных перегонных кубов по32; два original passes сначала
  дополняют каждый typed buffer, затем выбирают один пустой. Исключение аспектов,
  filter alias, uniform—not weighted—RNG и actual acceptance сохраняют количество.
  Native phial выводит8, не выдуманный обратный input в перегонный куб.
- Настоящие jar labels: ordinary paper/slime/black-dye recipe даёт4; один blank
  label+реальная phial создают binding, phial возвращается целиком с components.
  Прикрепление к наполненной банке/кубу берёт настоящий содержащийся аспект,
  пустому нужен заранее bound label. Бирка расходуется даже creative. Снятие даёт
  blank, не эссенцию. Filter/facing/contents сохраняются при drop/place банки;
  заполненные банки нестакуемы, пустые unlabelled снова обычные. Renderer отражает
  реальное содержимое/бирку. Tubes/golems этим не объявляются реализованными.
  Как original ItemResource13/IEssentiaContainerItem, bound label добавляет ровно1
  аспект при scan/dissolution (`ThaumcraftCraftingManager:365–369`), не8 как phial.
- Меха:6-way placement inward от clicked face, redstone pause, original oscillation,
  native vanilla-furnace cook+1 каждый второй tick с guard199. Печь учитывает
  направленные непитаемые меха; тигель по original считает любой соседний bellows,
  даже powered/facing-away. Это намеренно **разные** правила.
- Wood/greatwood rods и iron/gold caps: ёмкость25/50vis, modifier1.1/1.0.
  Dynamic RecipeManager recipe строго top-right cap / center rod / bottom-left
  cap, остальные6пусты; обе material researches обязательны. Base cost — product
  craftCosts (1 или3) **каждого** primal, не сумма. Wood/iron остаётся обычным craft.
  Persistent/synchronized components, material-dependent models/name/capacity.
  NODEPRESERVE работает только non-sneaking, non-wood **и** non-iron.
- Каталог аспектов расширен original logs/leaves/saplings и arcane stone/bricks;
  plankWood aliases берутся из `ConfigItems:296–297`, а не выдуманного complex tag.
  Converted crucible получает original cauldron-derived tags +fabrico4/praecantatio4.
  Wand scan bonuses используют sum craftCosts/2 и/3, не assembly product.
- Книга разрешает реальные recipe IDs новых prerequisites. Research browser
  использует bounded whitelist native ItemStacks для поддержанных original icons,
  сохраняя texture icons и явный BOOK fallback для остальных, без Java evaluation.
  Банка показывает реальную высоту/цвет эссенции через original `animatedglow`.

## Original anchors

Private oracle `thaumcraft/` (не изменялся):

- `common/lib/world/WorldGenGreatwoodTrees:40–380`, `WorldGenSilverwoodTrees:31–164`
  (**не Old**), `ThaumcraftWorldGenerator:459–478,563–584`;
  `common/blocks/BlockMagicalLeaves:144–200,224–265`, `BlockCustomPlant:130–171`.
- `common/config/ConfigRecipes:205–208,223,225–238,279–282,429–433,487`;
  `ConfigItems:294–307`, `ConfigAspects:389–399,422`.
- `common/tiles/TileAlchemyFurnace:211–386`, `TileAlembic:41–248`, `TileBellows:29–92`;
  `common/container/ContainerAlchemyFurnace:40–138`, `common/items/ItemEssence:123–192`;
  `BlockStoneDevice:179–246,312–359`, `BlockMetalDevice:447–449,529–675,722–737`,
  `BlockWoodenDeviceItem:56–70`.
- Jar labels: `common/items/ItemResource:276–311`, `BlockJar:158–180,258–286`,
  `BlockMetalDevice:534–535,590–624`, `ConfigRecipes:416–428`,
  `common/lib/events/EventHandlerWorld:155–161`.
- `common/lib/crafting/ArcaneWandRecipe:31–153`, `TileNode:222–236,398–418`;
  `client/gui/GuiResearchBrowser:421–445`, `ConfigResearch:91–101,167–172,249–305`.
- `client/renderers/tile/TileAlembicRenderer:40–143`, `TileBellowsRenderer:30–84`,
  `TileJarRenderer:120–151`, `client/renderers/models/ModelBellows:21–53`.

## Native adaptations / исправления

- Vanilla1.7 `Block.setResistance(x)` реально сохранял `3*x`, а base getter делил
  на5. Подтверждено original Minecraft bytecode в private reference: legacy setter10
  означает native effective6, не10. Исправлены planks, bellows и ordinary stair/slab
  base resistance; full arcane stone/bricks используют original world-aware20.
- Native blockstates хранят направление, не расходящиеся BE/item NBT bytes.
  Печь сохраняет original short burn/cook/vis; дополнительные integer UI durations
  передаются двумя unsigned halves, чтобы signed-short menu packet их не обрезал.
- Furnace input re-resolves/capacity rechecks перед списанием; output платится
  после verified acceptance. Replacement/reentrancy guards не дают detached BE
  менять новый блок или zero-valued aspects создавать ресурс.
- Native vanilla datapacks могут задавать cook duration<200: меха не перешагивают
  их native equality-completion tick. Original vanilla200 сохраняет guard199.
- Common models/assets не содержат скопированных vanilla классов или PNG. Atlas
  явно stitch original retained mod model sprites; opaque native ARGB сохранён.

## Границы, которые не скрываются

- PURE-knot silverwood candidates пока атомарно пропускаются с восстановлением
  sapling, без reroll/NORMAL-копии. Это уменьшает успешную natural частоту, пока нет
  PURE ecology. Есть counters skippedPureKnots/skippedKnotTrees.
- Greatwood spider dungeon decoration и silverwood shimmerleaf пока пропущены,
  original draw passes потребляются. Нет подставленного vanilla loot/flowers.
- Trees: нет старого seed identity, retrogen, forcing недоступных chunks,
  silverwood sparkle и полного legacy low-detail foliage rendering.
- Alembic world mesh — original OBJ,156triangles; item JSON — приближение.
  Нет connector nozzle/tube engine/golem automation/direct held-jar drainage.
  Dynamic label bind/clear рецепты работают в native crafting, но их все48вариантов
  пока не показаны отдельными диаграммами в книге. Обычный JarLabel recipe показан.
- Ванильная waterlogging stair/slab — native1.21 adaptation, не TC4 mechanic.
- Label item tooltip/name пока используют native localized bound-aspect presentation;
  полный original discovery-dependent unknown-aspect tooltip mask не перенесён.
- Wand models — базовая native geometry, не весь legacy 3D/FX renderer. Нет других
  caps/rods/staves/sceptres/foci/gear discounts. Dynamic recipe без одного глобального
  output/cost не подменяется фальшивым static result ради книги.
- Ordinary packaged/dedicated smoke всё ещё не запускался без заранее принятой
  локальной EULA. NeoForge client/remote multiplayer/full survival открыты.

## Проверка

После source freeze: оба loader builds/runtime helpers, затем actual Fabric
menu/drop/use path. Supplied materials/research/machine/camera/tree fixtures явно
отделять от ресурсов, действительно полученных из перегонки и native крафта.
Не объявлять PASS, полную natural частоту или visual parity до реального запуска.

Preflight: `distillation-compile1` exit0 — actual Java21 common + Fabric/NeoForge
GameTest source sets компилируются. Это не packaged build и не runtime verdict;
clientTest был исключён, пока его отдельный agent дописывал сценарий. Resource
preflight501sprites/152refs/missing0, Python preflight48PASS. GameTest-only run
`distillation-server1` запущен отдельно; полный source-bound запуск после общего freeze.

`distillation-server1`/`distillation-validate1` выявили интеграционные особенности:

- Native LootTable действительно возвращает count-zero ItemStack после неуспешного
  custom sapling roll. Листья теперь фильтруют такие пустые результаты, не меняя
  world `nextInt(200/250)`. Helper строго проверяет один draw на sapling attempt,
  ноль для shears/Silk/persistent/untriggered и продолжение того же RNG.
- Furnace drop diagnostic показал правильные7named iron/11named coal, неизменный
  соседний alembic и ещё одну законную blank label из ранее снятого filtered
  alembic этого же fixture. Baseline фиксируется после доставки этой ранней бирки,
  а не объявляет её дубликатом печи. Production deferred delivery не ослаблялась.
- Синхронный test снимал canopy и сразу вызывал randomTick второго sapling до
  обработки threaded skylight. Growth-gate assertions вынесены до обеих больших
  крон; production survival/light/RNG не менялись ради прохождения fixture.

Все source sets frozen; `distillation-validate2` завершился exit0:200JUnit,
58Fabric/58NeoForge, failures/errors/skips0, все58required IDs. Input snapshot
не изменился, оба remapped JAR прошли notice/test-isolation checks. Свежий
`.tmp/distillation-assets2.log`48PythonPASS. Это ещё не client/visual verdict;
`distillation-client1` запущен отдельно.

`distillation-client1` не прошёл login: реальный `ClientboundUpdateRecipesPacket`
не кодировал dynamic wand recipe. Причина — два разных экземпляра stateless recipe
в `MapCodec.unit` и `StreamCodec.unit`; последний проверяет equality при записи.
Оба codec теперь разделяют один stateless instance. В существующий wand helper
добавлен round-trip настоящего login update-recipes packet со всеми native recipes,
не новый mock serializer. Client harness сразу останавливается на DisconnectedScreen,
не ждёт420секунд после уже видимой ошибки. Новые source changes требуют общего
`distillation-validate3` и повторного client; validate2 не подтверждает исправление.

`distillation-validate3`exit0. `distillation-client2` после исправления действительно
прошёл login и10снимков, затем прерван перезапуском harness (exit1073807364), не
записан как PASS/FAIL gameplay. Partial raw report сохранён. Snapshot3 после restart
не изменился; owned Java процессов не осталось. Запущен отдельный client3 без
необоснованного повторного server build.

`distillation-client3` прошёл login/старые GUI и дошёл до physical alchemy, затем
timeout OUTPUT; не записан как visual PASS. Обнаружен race самого camera fixture:
native C2S drop packet и `server.execute` teleport не являются одной ordered channel.
Теперь камера перемещается только после server-observed расхода catalyst slot,
не перемещая item и не подставляя результат. Timeout также выводит воду/slot4/
реальные nearby items, чтобы отличать проблему контента от fixture по данным.
Client4 запущен до финальной общей validate4; gameplay main после validate3 не
менялся, но client fixture/snapshot обновлены. Final evidence ещё не зафиксирован.

`distillation-client4` SCRIPT_PASSED40captures, actual32essentia и paid50-vis wand
подтверждены native actions/server state. Root inspected exact33–40. Обнаружен
настоящий visual defect: jar_side alpha84/255 проходил CUTOUT как opaque blue shell,
скрывая liquid; original BlockJar:114–115 требует translucent pass1. Native registry
переключён на translucent; assets не менялись. Также при сверке original getBonusTags
добавлен содержащийся1aspect bound-label component и assertion в existing helper.
`distillation-validate4` прошёл200/58+58 ДО этих двух последних production changes.
Сейчас client5/snapshot5; после него требуется финальный общий validate5. Старый
снимок35 не называется доказательством видимой жидкости.

`distillation-client5`exit0/SCRIPT_PASSED40, run
`client-smoke-16f2592a-2cee-4f19-a883-c25d136f5ba8`; root exact33–40 inspected.
Теперь35 действительно показывает orange/cyan liquid через прозрачное стекло,
а34 — пустые прозрачные банки. 8coal→16ignis+16potentia→4filledphials→2jars;
4emptyphials возвращены, furnace/alembic buffers0. Native dynamic wand assembly
тратит990centivis каждого primal один раз; outputgreatwood/gold capacity5000 и
modifier1.0 реально заряжается у NORMAL node. Seeded trees не выданы за natural
distribution. Report: `docs/porting/distillation-client.json` (8of40 exact inspected).
Общий validate5 завершился exit0 ПОСЛЕ этого client;200JUnit/58+58/allrequired58,
failures/errors/skips0. Recorder успешно проверил3397unchangedinputs, fresh48Python,
оба artifact notices/isolation и записал final evidence. Дальнейшие новые content
изменения не должны выдаваться за проверенные этим checkpoint.
