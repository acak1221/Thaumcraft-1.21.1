# После M06d: кристаллизация эссенции — read-only dependency audit

**Подготовительный read-only audit, не evidence реализации.** M06d принят и
экспортирован; теперь активен [M06e baseline кристаллизатор](2026-10-17-crystallizer.md)
с назначенными A/B/C owners. Ниже source findings и предложения; актуальные scope,
ownership и проверки находятся в M06e-контракте. Fabric first.

## Источники

Original4.2.3.5 `TileEssentiaCrystalizer:41–337`, `ItemCrystalEssence:39–120`,
`TileEssentiaCrystalizerRenderer:34–89`, `ConfigResearch:309`, `ConfigRecipes:293`,
`ConfigAspects:431`, `BlockTubeItem:41–50`, `api/visnet/VisNetHandler:29–52`.
Проверен corresponding readable OpenTC4 по pinned80aac29c (reference, не donor).
Совпадают базовые gates/1unit/progress/ejection и EARTH acceleration; лишний
OpenTC4 canUpdate override не заменяет originalsoleauthority.

## Gate / зависимости

ESSENTIACRYSTAL ALCHEMY(8,-2), complexity1, Concealed primary (не Secondary),
parentTUBES, aspectcost aqua5/vitreus5/permutatio3/praecantatio5. Recipe
`IDI / QCQ / WTW`: **ironINGOTS2, DISPENSER1, BALANCEDSHARDS2**,
plainalchemicalconstruct1, qualifyingwoodplanks2, basicTube1; **5AQ/15TERRA/5ORDO**.
OriginalD=`Blocks.field_150367_z` dispenser, Q=`ConfigItems.itemShard:6`,
native`ThaumcraftContent.SHARDS.get("balanced")`/`thaumcraft:balance_shard`.
Ниdiamond, ни quartzitem/block/ore здесь нет. Первоначальныеiron4/diamond/quartz
assumptions были ошибкой read-only заметки, **не реализованным рецептом**. B
независимо сверил originalJARbytecode11789–11992/languages и pinnedOpenTC4:
`.tmp/crystallizer-input-item-audit.md`. field_150449_bY =quartzORE, но в ЭТОМrecipe
вообще неиспользуется. All9cellsoccupied, IoreingotIron/WoreplankWood, Wпринимает
также обеmagicalplanks, не толькоgreatwood какconstruct. M06dconstruct послеacceptance
нужен до этой unit; не подставлять freefinishedconstruct или advancedconstruct.
Выход кристаллизатор1, originaltube metadata7, recipekey`EssentiaCrystalizer`.
Balancedshardnativeexisting6cruciblerecipes underCRUCIBLE (e.g.AIRcatalystneeds
2eachother5primals) — настоящаяdependency, не quartzalias/freefinishedfixture.
Перед следующимclient читать фактический stoppedM06dstock/nodebudget, не обещать
неизменённые запасы из M06c и не пополнять истощённыйORDO прямой записью.

**VisNet отсутствует в текущем modern common/src/main/java.** Originalbase режим
работает вообще без источника: +1progress каждые5server ticks, 200progress →
1000native ticks после inputfill, то есть50с: freshautonomousfill наtick5,
progress1 на10, progress199 на1000, actualoutput на**1005**. Directinput/pending
reloadphase0 finishes1000, не требовать неверныйfresh1000. EARTHnetvis ускоряет
на2peractualrawnetworkquantum, request min20/max1/(200-progress)/2, **без x100/100**.
Исходное слово wholevis в этой заметке было неточным: raw grants совместимы с
centivis потребителей (original charger передаёт их в addRealVis напрямую).
Это не chargedwand, не
ordinaryauraNode и не редстоунэнергия. До переноса настоящего VisNet нельзя
объявлять ускоренный режим готовым или вшить always0 drain под видом полноценной
API. Возможный следующий boundedgate: честный **unassistedbaseline** с явно
открытым VisNetmode, либо сначала minimalrealVisNet dependency. Выбор перед
implementation нужно отразить в плане; полный M09/fullcrystallizer не закрывать
одной базовой демонстрацией. Energizedsource/relay/chargednetwork recipes и
publicAPI отдельны, test fakevisgrant не заменяет работающее питание.

## Essentia / native timing

Oneaspect1; default facingDOWN, placedfacing=clickedface.OPPOSITE (в отличие от
обычных труб). Только facing advertiseconnect/input; outputnone. Public type/
amount показывают one actualslot на любой стороне; minimum0/typeuntyped.
Originalsuction если powered0, иначе empty+queriedfacing128, **все другие faces64**,
даже пустые/неконнектящиеся. Не заменять64на0 на основании genericporthelper.
Rawadd checks inputface, containeraccept любая registeredaspect; no fakefilter.

Count++ EVERYservertick includingpower; `%5==0&&!powered` samples branch. If empty,
tryonefacingnativepull (sourceoutput/oppositesuction/minimum/onepublictypequery/
secondpressurecheck), then **setprogress0 even if pull succeeds that tick**.
If occupied, progress+=1+2*actualEARTHdrain. Ejectioncondition outside5tick/power
branch: aspect!=null&&progress>=200. Saveaspect/facing ONLY, notprogress/count/
spin/colors; pendingreload startsprogress0, unlike centrifuge's immediate-ready
reload0. Externalcontainer removal/addition doesn't itself reset oldprogress;
the nextempty%5branch does. Preserve valid successfulstates; document malformed
nonpositive/null/aspectmismatchauthority safety separately.

PublictakeEssentia always0; container take original accepts quantity1 while
ignoring type argument. This type-ignoring callback can delete the wrongunit;
before implementation explicitly choose/test authority hardening rather than
automatically calling singletontransporttake. Add negative offer may create a
unit in original; reject as documented malformed-call fix, not gameplay.

## Output is a real inventory transaction, not a visual spawn

1inputunit→1ItemCrystalEssence with containedaspect1. Original opposite-facing
adjacentIInventory receives from originalfacing side via InventoryUtils; leftover
physicallyejects atcenter+opposite*.65 with velocityopposite*.04, event0 and fizz.
Need audit actual InventoryUtils recursion/sidechecks BEFORE implementation;
ordinary modernContainer+WorldlyContainer contracts are not automatically
equivalent to a loader transferAPI call. Preserve dropdirection and container
remainder, not a second both-insert-and-drop duplicate path.

Root audited `InventoryUtils:49–159`: copyoffer, FIRST merge matching item IDs in
exposed slot order (actual insertion then rechecks **strictNBTcomponents**), SECOND
try exposed slots/empties; sidedinventories use only originalgetSlotsForFace and
canInsert+isItemValid. Unchanged differentaspectcrystalstacks must never merge.
Ordinarydoublechest has an observable old asymmetry: selectedhalf matchingstacks,
partnerhalf matchingstacks, then selectedhalf anyslots, then partner matchingstacks
again — it does NOT fill an empty farhalf. Modern54-slot combinedchest would
silently alter that successful layout/outputdrop policy. BeforeclaimfullIOparity,
decide/test nativechest-half mapping rather than assuming genericContainer loops
match. Hopper successful insert sets transfercooldown8 and dirties; copying only
contents without cooldown is not fulloriginalautomation parity. Single-item output
makes partialamount0/1 but not permission to ignore slotvalidity/side/strictcomponents.

Existing DeferredItemDrops gives durable stableUUID ADD retry. Extend narrowly
for original outputposition/velocity/clientevent if needed, not arbitrary item
spawn setters fromharness. Before ANYtarget dirty/menu/ADD callback, detach
inputownership and install one durable outputowner so cancellation/reentry/
replacement/break/reload cannot duplicate or erase it. Same global storage locks
and retirement ordering remain; snapshot/prepare contracts must describe slot
and paidoutput, not infer capability from any IAspectContainer marker.

A completed read-only design `.tmp/crystallizer-output-transaction.md`: paidlevel
escrow beforeANYtarget callbacks, SAMEprivatepreparedexecutor, closed audited
nativeinventory adapters with rawbacking-list/cooldown commit plus escrowretirement
before notification, or one exactdirectional residualdrop. Unsupported/malicious
Container setters MUSTNOT be invoked and then catch-and-drop: they may write THEN
throw, so that path would duplicate output. ExplicitnativeI/Oallowlist is bounded
support, not fulladdoninventoryparity. Preserve originalchest-half asymmetry and
strictcomponents. OwnitemRNG forlazyuntypedcrystal is separate from worldRNG.

**Known next-gate persistence gap (NOT fixed in M06d):** currentDeferredItemDrops
flush removes a row before ADD and save serializes only pending; an explicitsave
INSIDEanADDhook can omit the in-flight owner. Newcrystal delivery must serialize
IN_FLIGHT (and ROUTING escrow), restore assameUUID retry, never assume stableID
alone solved this. Add regression and preserve alloldinventory/harvest rows.
No claim of crash-atomicity across separatechunk/entity/SavedDatafiles: arbitrary
power-loss/stalefile replay needs separatejournal+acks; percrystalUUIDcomponent
would break same-aspect stackcompatibility and is not an acceptable shortcut.
Read-onlydesign/proposals only, not an authorization to thaw currentM06dsource.

## Crystal item is not ethereal essence or primal shard

OriginalItemCrystalEssence `IEssentiaContainerItem`, maxstack64, peritemaspect1,
untyped creativeunit lazilybinds one real registeredaspect on serverinventorytick/
creation using native RNG; don't hardcode randomcolour as realcontents. Typed
unit tints bycontainedaspect; untyped clientpreview colour cycles every500ms,
tooltip hides undiscoveredaspect. Crystalizer outputs are alreadytyped and must
not reroll; nativecomponent/codec/save/inventory/stackmerge/drop/reload preservation
andrecipe-derived objectaspects need tests. OriginalConfigAspects explicitly
registers EMPTYbase tags; runtimecontainedaspect is dynamic1, not extraAuram or
VisFilter material. Current ObjectAspectResolver already honors real positive
IEssentiaContainerItem contents, but nativeitem/component/tint not yet exist.
OriginalEntityHandler nativecrystaldeathdrop is a laterconsumer, not evidence of
automatic currentworldmobacquisition; cannot close it just by registering item.

B's independent `.tmp/crystallizer-input-item-audit.md` adds an IMPORTANT itemAPI
boundary: producedfactorytyped1 doesNOTjustify replacing the original full
`setAspects` orderedpositiveAspectList contract with a singleton component. Prefer
immutableorderedentrylist/defensivefull-list reads+wholevalidation, strictorder-
sensitive component equality (firstentrytint matters); no firstentrytruncation.
If only producedtyped1 is implemented, that is explicitly partialitemAPI and
unsupportedvalidlist writes mustleaveoldstackunchanged. Freshuntyped versus
explicitEMPTYlist versus unrelatedlegacyNBT need honestcomponentpatch mapping:
original lazytyping runs only when NO NBTcompound, not just noaspectvalue. Its
OWNitemRandom draws contents;500mswallclockcycle is display-only/no RNG/write.
Defaultprototypecomponents cannot falsely markeveryfreshunitalreadyinitialized.
Originalemptybase tag must be bound to the flattened nativeID explicitly so
dynamiccontent1 is not accidentally combined with fallbackrecipe/shard/Auram tags.

## Visual / evidence contract for later gate

Original retained `crystalizer.obj/.png` whole shell plus `vis_relay.obj/.png`
Crystal part. Facingorientation thenlocalZ−.5; four .75-scaled relaycrystals
quarter-turnZ, localtranslate(.34,0,1.2125), actualspin+spinInc*partialtick. The
four crystals have originalsin-drivenbrightness, aspecttintcurrentRGB smoothstep
.05 towardaspectRGB/220, not inventedfluidgauge. Clientspin follows native+.1to20/
-.2to0 and seven-tick eventvent. ModelOBJ/MTL parser and renderer resource rights
audited separately; fileexistence/colourunit test not actualpixelacceptance.

Focusedclient should freshcopy the acceptedSTOPPEDM06doutput and use actualnode/
rawingredient recipes, finite genuineessentia, one realinventoryejection and one
unassistedphysicalpickup, allclock/vis/essentia/outputfixtures declared. Native
1000ticks is short boundedcontentplay, not excuse for clock/progress shortcuts.
Nativecases also cover all6facinginputs/oppositeoutputs, raw API/baselinepause,
partialinventory/fullcontainer/canceledADD/exceptions/loadedpendingoutputs,
savedprogressreset, sourcequeryRNGprogram and dynamictypeditemaspects. Acceleration
gets separate realVisNetevidence when its source/relay infrastructure is playable.
