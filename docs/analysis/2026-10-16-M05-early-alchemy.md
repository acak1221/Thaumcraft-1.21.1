# M05 — тигель и полезные результаты ранней алхимии

Это проверенный implementation slice после `early-content-validate3`: `m05-validate4`
197JUnit/51+51GameTests/обаJAR PASS; `m05-client2`SCRIPT_PASSED32captures, root
exact27–32. Evidence: `porting/verification.json`, `porting/alchemy-client.json`.
Последующее продолжение деревьев/перегонки/жезлов этими результатами не подтверждено.
Полные M01/M02/M05/M11 не объявляются закрытыми.

## Контент и исходные правила

- `ThaumcraftAlchemy` / `BlockCrucible` / `TileCrucible`: жезл бесплатно превращает ванильный котёл в тигель; вода прежнего котла не переносится, жезл остаётся в руке. У тигля нет выдуманного GUI. Ведро/бутылка заполняют native резервуар1000mB; огонь, lava или настоящий Nitor нагревают содержимое. Original heat threshold — **>150**, heat200, transient counter−100.
- Физические брошенные предметы растворяются в аспекты. Реальный catalyst + знания **серверного thrower** дают researched recipe; offline/unresolved thrower может растворять, но не крафтить. Нет доверия custom-NBT/client username. Выбор рецепта — наибольшее **число различных** аспектов, при равенстве первый original registration; JSON `source_order` сохраняет этот порядок.
- Десять настоящих RecipeManager recipes: шесть balanced shards, Alumentum, Nitor, Thaumium и Tallow. Исходные research keys/catalysts/costs взяты из `ConfigRecipes:105–120`, не из позднего TC. За крафт расходуются аспекты и50mB; original допускает и последние1–49mB. Catalyst не растворяется дополнительно при успешном крафте.
- Original shrink-loop `for(i < remaining)` подтверждён bytecode: за один collision обрабатывается приблизительно половина уменьшающегося стека. Это не исправлено на whole-stack. После растворения counter−150, крафта−250; overflow100, compound/primal decay и water−2 сохранены. Bellows пока не перенесён: учитывается только точный ID, не случайные ванильные блоки.
- Результат — настоящий `EntitySpecialItem`, не запись в журнале и не подставленный инвентарь. Stable UUID и level SavedData сохраняют оплаченный output при отмене spawn/разрушении тигля. OnRemove лишь ставит разливы в очередь, не изменяет соседние BE в процессе native replacement. Это адаптация доставки к native lifecycle, не crash-atomic multi-file save.
- Нитор действительно ставится, светит15, имеет исходные bounds/no collision, выпадает обратно и нагревает тигель; перенесены частицы original atlas. Исходный расход Nitor даже creative сохранён.
- Алюментум — настоящее метательное взрывное средство и топливо. Original `EventHandlerWorld:139–146` даёт **6400 ticks**, не32000. Projectile speed0.75, radius1.66, нет дополнительного direct-hit damage/огня; terrain damage управляется `mobGriefing` даже при броске игроком. Есть native dispenser behavior.
- Страницы Nitor/Alumentum/Thaumium/Tallow в книге читают реальные catalysts/results/aspect amounts из тех же RecipeHolders, что тигель. Остальные recipe kinds не подменены заглушками.
- Страница balanced shards чередует шесть реальных native рецептов раз в секунду по original `ConfigResearch:250–254`. Только точная закреплённая array-expression сопоставлена с этими IDs; Java из research data не исполняется.

Основные anchors: `TileCrucible:94–156,186–325`, `BlockMetalDevice:244–261,380–385,529–570`, `WandManager:121–133,713–714`, `ThaumcraftCraftingManager:127–145`, `ItemResource:196–259`, `BlockAiry:143–163,217–261,282–319`, `TileNitor:18–27`, `EntityAlumentum:32–55`.

## Аспекты предметов — работающий вход алхимии и сканирования

`ObjectAspectResolver.get(stack, level)` используется и таумометром, и растворением.
Для доступных native recipe kinds реализованы original priority crucible→arcane→crafting,
0.75/output truncation, sqrt(cost)/output, bonus контейнера/снаряжения/enchantments/
potion components, cap64 и weighted cull до6. Количество предметов в стеке не умножает
per-item aspects; тигель списывает сами предметы по одному.

`early_object_aspects.json` — отдельная ограниченная сверка original TC4, не новый
полный каталог:96 authoritative direct IDs,6 complex raw additions,75 source rows.
Исправлены унаследованные значения coal/charcoal (`potentia2 ignis2`), glowstone dust
(`sensus1 lux2`), redstone, древесины/добычи мобов и др. Torch — **direct lux1**,
crafting table — **direct fabrico4**, не complex. Empty phial — vacuos1; filled base
явно пуст,8 содержимого добавляются из настоящего component. Source hashes/line
anchors и исключённые ветви находятся в самом JSON.

Native recipe ID ordering стабилизирован; global registration/cache/seed identity1.7
не заявлена. Recipe index заменяется при `/reload`, аспектные результаты не остаются
между запросами/мирами. Recursion history ограничивает циклы; arbitrary custom/
infusion recipes и внешние tier mappings пока отсутствуют. Современные raw metals,
copper/netherite/deepslate не получают выдуманные original direct tags. Остальной
legacy fallback пока не полностью сверён с original.

## Исправления reproducible defects и явные границы

Original `attemptSmelt` return на неизвестном catalyst может произойти после успешных
крафтов, но **до** записи уменьшенного local stack count обратно в entity. Native
списывает каждый оплаченный предмет сразу, поэтому повторный bounce не дублирует
результат. CFR use-before-assignment в `ejectItem` не перенесён: порядок copy→cap
проверен по исходному bytecode. Эти изменения не меняют обычных цен/выходов.

Открыты: loader fluid/automation adapters, comparator, crafting-event bridge,
remaining алхимические recipes, полный external API и visual/audio parity.
Flux finite/exhaustion интегрируется отдельно; taint/slime ecology не заменяется
ванильными слизнями или фиктивной biome. Ни один проход сборки сам по себе не
доказывает все эти системы.

## Flux — реализованный ограниченный срез

`FluxFiniteBlock` сохраняет8quanta, vertical-first flow (газ вверх, goo вниз),
density swap, overflow, N/S/W/E sharing и remainder RNG. Spill — original1/4,
сначала выше тигля/merge до7, затем x/y/z scatter; неизвестные chunks не загружаются.
Exposure даёт настоящие vis-exhaust/wither effects, goo замедляет X/Z; low-density
goo decay связан с газом. Exhaustion повышает цену поддерживаемого жезла на10% за
уровень, по максимальному amplifier двух вариантов, не сумме.

Для per-instance empty curatives добавлены два scoped mixins. Milk не лечит свежий
uncurable instance, но admin removeAllEffects работает. Исходные merge/reload quirks
не заменены глобально невыводимым effect type. Forge algorithm reference pinned
`9274e4fe435cb415099a8216c1b42235f185443e`; attribution/licence в THIRD_PARTY_NOTICES.

Открыты M11 Thaumic Slime growth/spawn и taint fibres/biome: pending conversions не
расходуют goo впустую. Flux rendering пока flat quanta/full-height, не original
neighbor-weighted slopes/double-sided renderer/bubble particles; registered-fluid/
bucket/external adapters отсутствуют.

## Ход проверки

`m05-compile1` выявил protected native BlockPlaceContext constructor в ItemNitor;
исправлен public context adapter. `m05-validate1` собрал JARs и дошёл до runtime,
где NBT effect выявил неверное использование Architectury RegistrySupplier как
native Holder: этого wrapper нет в canonical holder map. Production effects,
lookups и tests переключены на настоящие `BuiltInRegistries.MOB_EFFECT` holders,
чтобы работали и сохранение/сеть, и повторные эффекты после reload. Это исправление
native integration, не изменение original gameplay. Полный повторный запуск обязателен.

`m05-validate3` прошёл197JUnit/51Fabric/51NeoForge, failures/errors/skips0.
`m05-client1` SCRIPT_PASSED32captures: настоящий drop/collision craft и placement
выполнены. Root осмотрел exact27–32; found gray-water issue: original1.7 water sprite
pre-coloured blue, native1.21 sprite grayscale (сверено с обоими original/native
Minecraft PNG вне модификации). Renderer теперь подаёт native ordinary-water tint
`0x3F76E4` перед original pollution multiplier, без новой biome-dependent механики.
Minecraft PNG не копировался; exact old pixels не заявлены. Снимок чистой воды ждёт
12clientticks после boil update, чтобы видеть настоящий FX. Эти последние renderer/
fixture changes требуют `m05-validate4` и повторного client; прежний PASS не считается
их визуальным подтверждением.

Оба повторных запуска выполнены: `m05-validate4`exit0,197JUnit/51+51;
`m05-client2`exit0/SCRIPT_PASSED, run `client-smoke-0556d7cf-2e83-40a9-99b6-83996b169865`.
Root exact27–32 inspected: bluewater/boilingcontents/physicalresult/placedNitor/
bookrecipe/fluxcells. Sourcebound recorder PASS3228unchangedinputs, fresh
`.tmp/m05-assets3.log`48PythonPASS. Это не NeoForge client/fullsurvival/fullM05 verdict.

## План проверки текущего среза

По одному compact server scenario для тигля, ресурсов и flux плюс общий aspects
scenario, на обоих loaders. Fabric scenario выполняет native cauldron conversion,
bucket use, player drop→real collision→Nitor output→pickup→placement. Материалы,
изученное NITOR и перемещения камеры supplied; aspects/heat/results не инжектируются.
Снимки и итоговые результаты фиксировать только после фактического запуска.
