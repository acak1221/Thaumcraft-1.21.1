# Excavation — принятый базовый Fabric scope и контракт

**Принято: excavation-fabric-validate3 / excavation-fabric-client2, Fabric-only.**
200JUnit/72Fabric/all72required/3653unchangedinputs/48Python,0failures/errors/skips,
oneFabricJAR notices/isolationPASS.6exactPNG89–94 осмотрены/1м1с/696ticks,
source-boundmatch=true/historical=false. Последующая история ниже — не active runs.
A`ses_ebb0b5675ffedUYNbiUUe09uXZ` owns newHarvest/mixins/runtime;
B`ses_ebb0aafbeffezwBsNfqE5xRD87` owns existingFoci/FocusData+newfocus/state/FX/runtime;
C`ses_ebb0841b1ffetlrxcXwTcuFQb9` onlynewclientsequence. Root owns allremainingexisting
integration/resources/build/GUI/wrappers. Не читать active-ownedfiles/не Gradlegame
до ALL SOURCEFROZEN. Source snapshot/final72server/client89… ещё не получены.

Read-only исследование `ses_ebb45dc7effeg1yjaRPRRY9T9E`. Никакие исходники или
сборки этим исследованием не менялись. Оригинальный TC4 4.2.3.5 — спецификация;
OpenTC4 `80aac29c` использован как читаемый справочник имён. Это не свидетельство
готовой добычи, защиты, дропа или графики в нашем порте.

## Исходное поведение

Oracle `ItemFocusExcavation:113–221`, `BlockUtils:237–253`:

- Удерживаемый RMB/CHARGE, cooldown0. Research ограничивает крафт, не найденный focus.
- Каждый tick сначала проверяет доступность15terra centivis с общим FLOAT modifier
  и truncation; денег недостаточно — stop до ray/progress/FX, без платежа/reset.
- Block COLLIDER ray10, без fluids и без entity targeting. Новые координаты получают
  progress0 **без increment в этом tick**. На прежних координатах сначала проверка
  старого `progress >= hardness`, затем increment, если завершения ещё нет.
- Increment .25F для original rock/grass/ground/sand, иначе .05F; exact obsidian×3.
  Не vanilla destroy-speed/tier/haste/water/air. Stone1.5 —8ticks, dirt.5 —4ticks.
- Платёж только при завершённой попытке, **до** защищённой добычи. Отмена тоже
  платная, после неё target/progress сбрасываются. Промежуточные ticks бесплатны.
- Miss/failed canMineBlock сбрасывают progress; negative hardness сохраняет старые
  target/progress. Hardness0 завершает на следующем same-target tick.
- Tooltip per-tick сохранён, хотя реальные расходы per-completed-attempt.

Release quirk (`:269–294`, wand `:561–570`): класс переопределяет item stop callback,
а wand вызывает focus stop callback. Обычное отпускание поэтому **не сбрасывает**
progress. Не добавлять случайно reset override. Original state — side+player-name,
только координаты (даже не block-state/dimension/wand); origin(0,0,0) имеет начальную
особенность. Same-world pause/resume сохранять; изменения identity/dimension/death/
logout/reentrancy guards явно записать как safety adaptations.

Original fast materials требуют явной классификации/тега. Pickaxe/shovel tags не
равны original materials: металл/стекло/глина/снег не должны получить fast случайно.
Для новых 1.21 blocks нужен явный default/opt-in, не недоказанная полная таблица.

## Защищённая native добыча

Oracle `ItemFocusExcavation:224–256`, `BlockUtils:112–140`: real-player BreakEvent
с настоящим game mode; basic fortune0/no silk, обычный block drop+XP, без wand tier/
wear/mining-stat/exhaustion. BE существует во время вычисления drops; обычное
удаление блока выводит contents. Event-modified XP original игнорирует. Original
drops-before-unchecked-air создаёт опасность дубликатов: это не сохранять при
замене BE/block отменяющим callback.

Проверены native1.21.1 bytecode и cached Fabric/Architectury source JARs:

| Метод/интеграция | Граница |
|---|---|
| `ServerPlayerGameMode.handleBlockBreakAction` :148–180 | reach/attack/vanilla progress; НЕ использовать как spell driver |
| `ServerPlayerGameMode.destroyBlock` :248–289 | actual item permission→BE→restrictions→playerWillDestroy→remove→destroy→creative-return→tier/wear→playerDestroy; не содержит внешнего mayInteract |
| Architectury Fabric `MixinServerPlayerGameMode` :46–54 | реальный BlockEvent.BREAK, отмена только `.isFalse()`, XP null |
| Fabric `ServerPlayerInteractionManagerMixin` :105–119 | реальный BEFORE перед playerWillDestroy, CANCELED при false, AFTER после remove до destroy |
| `Block.playerDestroy` :437–440 / dropResources :359–363 | нативные loot+spawnAfterBreak/XP; не добавлять outputs вручную |

Нужен строго scoped excavation context вокруг **существующего** player.gameMode.
destroyBlock: final loaded/ray/mayInteract/hardness validation; внутри только этой
операции bypass tier, suppression mineBlock/wear, private fortune0/no-silk loot-tool
copy, suppression stat/exhaustion. Не запускать events вручную перед destroyBlock:
будет двойной dispatch. InterruptTrue/default — не cancellation. Native returntrue
не доказывает successful removal: нужен настоящий removal witness и stale-state/
BE/reentrant guards. Не менять game mode/abilities и не подменять player inventory.

**Creative policy ещё требует реализации и проверки.** Только обход creative-return
недостаточен: ShulkerBox.playerWillDestroy уже делает creative contents-drop; так
можно получить две коробки. Doors/double plants тоже смотрят real-player.isCreative.
Нужны scoped конкретные callback adaptations, не глобальный fake Survival.
Native ice.playerDestroy создаёт воду, modern iron ore даёт raw iron — native harvest
сам по себе не обещает legacy loot/ice parity. Эти отклонения решить явно.

## Подключение и визуальная часть

- Новый ItemFocusExcavation: color409606, sortingBE, CHARGE, getVisCost15terra,
  isVisCostPerTick=true; общий FocusData/selection, не второй протокол.
- Recipe: CQC/Q#Q/CQC,4earth shards/4quartz/emerald **tag**,20terra/5perditio/5ordo.
  Oracle ConfigRecipes249–251 (сверить точную строку корпуса перед реализацией).
- Research FOCUSEXCAVATION, concealed **primary**, complexity2, (0,-3), FOCUSFIRE,
  terra/perditio/praecantatio3. Данные research уже присутствуют.
- Добавить book alias/icon/item name/model; existing focus_excavation.png.
- Selector должен stable-sort FireAF→ExcavationBE→ShockBL с сохранением duplicate
  inventory slots/components. Нынешний inventory-order не считать сортировкой.
- Model property clamped[0,1]: выделить4 значения и согласовать все36 wand material/
  focus combinations; не property2.
- Beam continuous0x00FF66, impact5/end-size2; rumble.3/1 каждые1200ms wall-clock,
  miss сбрасывает sound delay. Cracks progress/hardness×9, не vanilla×10; guard0.
  Beam истекает после прекращения refresh. Native break FX исключает breaker —
  нужен caster feedback без дублирования remote events.

## Компактная приёмка

1. Stone/dirt timing, нулевой interim debit, pause/resume, смена цели/miss/bedrock,
   insufficient vis/depletion. Отдельно coord-inheritance и lifecycle adaptations.
2. Обе настоящие cancellation hooks: платная попытка, block/BE/drops/XP неизменны;
   нативный adventure refusal не обходится.
3. Tier ore+XP, полный container, сохранность wand; creative filled-shulker без
   duplication. Не artificial output grants/заданные block drops.
4. Только новый client acquisition/book/3-focus F switch, held model/continuous
   beam/cracks/реальная добыча. Старые saved foci/robes/essentia — объявленные inputs.

Advanced upgrades/fortune/silk/treasure и original wrong-stack/shared-cost quirks
не включать незаметно в basic scope. Ни gameplay, ни client evidence ещё не созданы.

## Уточнённый scoped adapter (read-only, ещё не написан)

Root перечитал oracle224–254: spell **не вызывает** playerWillDestroy/harvestBlock/
playerDestroy. Повторная native проверка `ses_ebb45dc7effeg1yjaRPRRY9T9E` рекомендует
границу вокруг настоящего `gameMode.destroyBlock(pos)`:

- Redirect Block.playerWillDestroy(Level,BlockPos,BlockState,Player):BlockState
  возвращает capturedstate без callback, **не** пропуская Fabric BEFORE перед ним.
- Internal GameMode.isCreative early-returncall→false (не realplayer/gameMode),
  ServerPlayer.hasCorrectToolForDrops→true, ItemStack.mineBlock→no-op.
- ServerLevel.removeBlock(BlockPos,boolean) проверяет exactstate/BE/authorization,
  delegatesonce и фиксирует настоящий successful-removal witness.
- Block.destroy(LevelAccessor,BlockPos,BlockState) callback пропустить, **не**
  пропуская Fabric AFTER перед ним. OnRemove/neighbor updates оставить native.
- Block.playerDestroy заменить native static Block.dropResources(snapshot,level,
  pos,capturedBE,realplayer,private enchantment-empty tool), без manual outputs/XP.

Так избегаются creative shulker pre-drop, icewater, stats/exhaustion; native loot/
spawnAfterBreak остаются один раз. AFTER replacement не трогать, old output из
old capturedBE. Mixin0.8.7 callback injectors order1000, redirects10000 поддерживают
composition в принципе, **но transformed actual event counts обязательны**.
Exact single-match redirects; no overwrite. MixinExtras0.4.1 имеет WrapMethod:
whole-method finally frame для КАЖДОГО native invocation, привилегия только exact
top-level permit, nested ordinary samepos и exceptions без privilege. Dependency
wiring не compiled. Не подменять инвентарь/tool/gameMode/globalPlayer.isCreative.

Nativeblock policy до реализации:
- Filled/unopened shulker loot сам копирует container/name/lock/loot из capturedBE;
  callback ему не нужен. Не раскрывать unopened loot ради fakeplayerluck.
- Chest contents только onRemove, TC jar drops используют supplied BE: не извлекать дважды.
- Occupied hive/nest теряет bees без modern adapter (release в playerDestroy):
  либо явный отказ, либо доказанный native postcommit emptyAllLivingFromHive, не fake silk.
- Door/bed neighbor updates убирают вторую половину; loot lower/head. Проверить обе halves.
- Double plants loot зависит от opposite half, но vanilla plants не collision targets.
- InfestedBlock.spawnAfterBreak спавнит silverfish: явно сохранить nativeextra либо
  suppress exact scoped infestation, не весь generic XP. Original прямые getDrops
  не равны такому extra-spawn.
- Native removeBlock сохраняет waterlogged fluid. Явная modern adaptation, не literalair.
- Skipped player callback убирает FX/piglinanger/gameevent; original-style visual
  break feedback восстановить onceafterwitness, включая caster. Modern effects отдельно.

Musttest realbothprotectionhooks/cancel/replacement/nestedordinarysamepos/exception,
survival+creative filled/unopenedshulker/chest/jar,ore+spawnerXP/ice/no stats-wear,
failedremove/AFTERcounts,door/bed/hive/infestationpolicy. Это всё пока контракт.

## Root integration / native material provenance

Rootadded originalConfigRecipes251/emerald `c:gems/emerald`, item/book/icon/lang,
9heldmodels и fourfocus values0/1÷3/2÷3/1.36float32predicate combinations agree,
resource286textureRefs/201models/0missing: source/resourcecheck, не game-renderPASS.
Fselector AF/BE/BL stable sort keepsslot/components/duplicates; contextValid проверяет
тотжеsortedlist. Feedback defaultviewport перенесён выше ordinary HUD, ещё не seen.
Scopeexcavation reads actualclothing-client.saved_world, newbench1650,216,377,
center1474,216,377, sourcecopy neveropened. Capturecount6 provisional untilCfreeze.
Bootstrap/renderer/2runtimewrappers72provisional APIs требуют postfreeze review.
MixinExtras-common0.4.1 compile/APonly added, POMMavenCentralread-onlychecked;
FabricLoader supplies runtime. Actual wrapper transformation ещё не проверена.

`.tmp/excavation_material_audit.py` обрабатывает javap `aji.p` и constructor/super
material references 1.7.10 client-e80d9b3bf5085002218d4be59e668bac718abbc6.jar +joined.srg.
171registrations разобраны,51originalIDs имеют field151577_b/151578_c/151576_e/151595_p
(grass/ground/rock/sand), unresolved0. Metadata/renamed nativecounterparts разложены
в explicit `thaumcraft:excavation_fast`:82vanilla/15portedTC IDs, все97 имеют actual
blockstate resources. ЭтоНЕgenericpickaxe/shoveltag и НЕновый Material enum.
В частности hay_block=grassFAST; metal/gemcompressedblocks/glass/infestedclay/snow
SLOW. NativeTCAlchemyFurnace originalMaterial.iron (:57) иcrystalsMaterial.glass
НЕfast; CustomOre(:58),CosmeticSolid(:65),StoneDevice(:92) rockFAST. Slab flattening
сохраняет smooth_stone_slab/petrified_oak_slab; новые granite/deepslate/etc не
получаютfastавтоматически. Неизвестные/новые blocktypes defaultslow, opt-in datapacktag.
Private javap/report outputs `.tmp/excavation-original-materials.json` и
`.tmp/vanilla1710-block-constructors.javap.txt`, не donor code/не изменение JAR.

Текущая agreedpolicy для реализации: occupiedhive conservative REFUSE; scoped
InfestedBlock silverfish extra suppressed, genericXP/ordinary external behavior
не suppressed; nativewaterlogging allowed/iceair. Same-world coord-only progress
и release mismatch preserved; dimension/death/logoutidentity cleanup documented.
Все эти политики ещё нуждаются в native execution, не считать prose реализацией.

Новыйclient использует реальные earnedclothing/3wands/Fire+Shock/173jarunits/4phials
какsavedINPUTS. Exactpayer1796terra/1531ordo/731perditio уже оплачивает recipe
1599/399/399;197terra достаточно для11-unitbasicattempts. Новую зарядку/старую
прогрессию не повторять. Raw4earthshards4quartzemerald/16planks3cobble/ore/research/
camera/floor/protectionpolicy — fixtures, nativechestcraft/contents/drop/XPoutputs
не предоставлять. PlayerEntityInvulnerable remains oldfixture, incomingdamage нет.

Postfreeze root review: A4files/B7files/Coneclient все завершены, никаких active
owners/runs. Root подключил2actualmixins, MixinExtras **fabric**0.4.1 compileOnly/AP
по cachedartifact (runtimeLoaderalreadybundled); Bactualprogress(ServerPlayer) /
ProgressView(pos,amount,dimension) согласован сC. Lifecyclequit/dimension/clone/dead
POST/serverstop wired, release НЕ reset. Capturecount6contiguous89–94 confirmed;
compact2wrappers floor72, mainJava269. Diff-checkPASS; firstcompile ещё не запущен.

## Первая центральная проверка

Compile1/validate1 остановились на Javafixture API (checked catch после typed API
и AABB(BlockPos,BlockPos)); эти три места исправлены, production/client compiled.
Server2 реально71/72 за6.769s: complete focus/payment/timing case прошёл, harvest
case остановился до первого positive break/eventcount. Native javap показал
ошибку adapter ray: LivingEntity.getViewVector(1) использует HEAD yaw, тогда как
фокус и oracleBlockUtils237–253 используют current body yaw/pitch. Root final
authorization переведён на тот же `getLookAngle`, не на stale head interpolation.
Дополнительно test-only bounded10block ray lane явно loaded, как требует helper;
дальний unloaded4096 refusal остался untouched. Сейчас diagnosticserver3; ещё
нет текущего72PASS или client89–94, validate1snapshot stale. Donor/price/schema
не менялись; отказ защиты/безопасность не ослаблялись.

Server3 exit0/**72Fabric**, обеactualExcavationcases прошли,failures/errors/skips0.
Текущийrayfix/realprotection/container/XP/nestedscope matrix исполнен, не только
скомпилирован. Freshsnapshotvalidate2; идёт focusedclient1,6expectedPNG89–94.
Ни exactPNGосмотра, ни currentfullbuild/JAR/source-boundrecorder пока нет.

Client1exit1/FINAL_ACK: всёnativegameplay прошло, только heldmodeltest ожидалflat
focus_excavation sprite вместо originalsharedmodels/wand6-facecube+tint409606.
Root test-only fix проверяет actual6tintfaces/opaqueARGB/Silverwood/Thaumium;
модели/tintproduction не менялись. Nativefinalterra164/3×11,diamond1+6XP,freepause
.6500001,paidprotectionBEunchanged. Failed91rootseen:greenbeam plainvisible;
90single tooltip/currentShock/oldFire иfeedbackвышеHUD, неfullGUIparity. Rawreport
archived `.tmp/excavation-fabric-client1-raw-report.json`. Freshsnapshot3/client2
идёт из acceptedclothing save; failedcopy не используется какacceptedinput.

## Приёмка базовой unit

Finalvalidate3/client2 прошли без изменения production после current-body-rayfix.
Actualrecipe1599terra/399ordo/399perditio;2nativechestcrafts/16planks;realFswap вернул
exactShock,oldFire retained. Native6.10block slowchest freeprogress.6500001→pause→
resume→chest+3actualcontents. Diamondore→1diamond+**4nativeXP**. ONErealFabricBEFORE
paidrefusal: sameearnedemptychest/BE/nooutputs.3attempts×11terra=33, finalterra164,
otherfinalaer951/aqua2941/ignis2248/ordo1132/perditio332. Нетnewcharge/vis/XPgrants.
114oldTCblocks/34BE/12jars173units/72candles/threewands/oldfoci/wornrobes+goggles/
apple/knowledge preserved. Весь strictmatrix обоих newservercases прошёл вместе
с70oldcases; это не replay старой acquisition или всехmodernblocks.

89book/base20/5/5/readableexactrecipe.90single selectorcurrentShock/Fire+Excavation,
misleadingoriginalperTick.15 preserved.91**greenbeam/impact действительно видимы**,
nativecrackstage1actual, fineearlycracks визуальнонеразличимы.92chest/diamond/cobble3
видны, jar tooltip остался, нет explicit4XPnumeral.93highstagecracks на оставшемся
protectedchest видны, staleoriginalexpiry deliberatelypreserved.94actualfinitevis/
darkgreenheldsixfacecube/ff064006/Silverwood/Thaumium видимы и runtimeprobed. Footer
вышеHUD, bottombuttonsещёoverlap hotbar. Fullbeamrotatingplanes/audio/GUI pending.

Accepted STOPPEDsave`fabric/run-clienttest/saves/excavation-smoke-dc8878ce-5af1-4825-b4d8-c2869791f6e5`.
Evidence`porting/excavation-client.json`/`porting/verification.json`,archive
`.tmp/excavation-accepted-verification3-before-frost.json`. Не fullsurvival/Neo/API/
upgrade verdict. Следующий basicFrost ещё не implemented. Allowners/processesstopped.
