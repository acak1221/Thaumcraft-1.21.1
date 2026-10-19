# Следующая bounded content batch: мантии и базовый Shock

**Принято: clothing-fabric-validate8 / clothing-fabric-client7, Fabric-only.**

200JUnit/70Fabric/all70required/3630unchangedinputs/48Python;0failures/errors/skips.
8exactPNG81–88 осмотрены,2м16с/2117ticks, source-boundmatch=true. Детали/ограничения
в конце; история intermediate failures ниже не является текущими active-процессами.
Три мантии + Shock; Excavation отдельно, потому
что защищённая добыча/drop/XP/BE намного рискованнее простого damageaction.
Read-only audit `ses_ebb9b69c8ffeV0yUBQmIDfjvM7`; root дополнительно прочитал
ItemRobeArmor/RecipesRobeArmorDyes и cachedvanilla1.7palette. Доноры не копировались.

## Мантии

`ConfigRecipes256–260/ConfigResearch184`: общий gate**ENCHFABRIC**, secondary,
ARTIFICE(0,3)/complexity1/pannus3+praecantatio3, безexplicitparents. Fabricrecipe
ужеnative:4string+anycolorwool→1fabric,1каждогоprimal. Добавляются chest8fabric/
5aer (`I I/III/III`),legs7/5aqua (`III/I I/I I`),boots4/3terra (`I I/I I`).
AliasesRobeChest/RobeLegs/RobeBoots; originalItemChestplateRobe/ItemLeggingsRobe/
ItemBootsRobe. Нетrobehelmet. Полныйset:19fabric, не20;20-яможетостатьсядляrepair.

`ItemRobeArmor61–163/Api52/ConfigItems431–436`: SPECIALdefense chest3/legs2/boots1,
durability400/375/325, enchant25,uncommon; actualenchantedfabricrepair. Не наследовать
gogglesgoldrepair простоиз-заобщегоSPECIAL: nativeper-itemrepair mustoverride.
IRepairable/IVisDiscountGear/IRunicArmor0; скидки2/2/1points, set5 +goggles5=10,
без нового механизмаскидок/щита/саморемонта/movement/glint.

Defaultcolor**0x6A3880(6961280)**; originalhasColorВСЕГДАtrue. Одинrobe+≥1dye,
copiescount1/data/damage/enchantments, каждойoccupieddyeslotодинголос. Defaultpurple
участвуетужевпервомmix; averagingRGB/maxbrightness/FLOATrescale/truncate строго
`RecipesRobeArmorDyes51–111`. Не vanilla1.21palette/необычныйleatherfirstdyemix.
Washingheldrobe внепустомwatercauldron удаляетcustomcolor/расходует1water даже
defaultrobe/creative; serverauthority, неworld/client-onlysetter.

**Palette now anchored, not guessed:** joined.srg8370 `wp.bp=EntitySheep.field_70898_d`;
cached `client-e80d9b3bf5085002218d4be59e668bac718abbc6.jar`, `javap -c -p wp` static
initializer0–348. В wool-ID порядке0…15 (modernDyeColor.getId):
`(1,1,1),(.85,.5,.2),(.7,.3,.85),(.4,.6,.85),(.9,.9,.2),(.5,.8,.1),
(.95,.5,.65),(.3,.3,.3),(.6,.6,.6),(.3,.5,.6),(.5,.25,.7),(.2,.3,.7),
(.4,.3,.2),(.4,.5,.2),(.6,.2,.2),(.1,.1,.1)` as JavaFLOAT, then×255/truncate.
joined.srg10497 `aka.b`→`aka.c`: originaldyemeta→`(~meta)&15` woolpaletteindex.
Defaultpurple+RED therefore expected**0x8C3960**, not fullred/moderndiffusemix.

Itemsbaseclothchest/clothlegs/clothboots tint, second…overpassuntinted. Worn
robes_1/robes_1_overlay (chest/boots),robes_2/robes_2_overlay (legs); retainoriginal
bytes and nativepathaliases. ModernDYED_COLORprototype/nativeArmorMaterial.Layer
dyeable adaptation must preservepurplewhenuncustomized. Do not accidentally enable
vanillamodernDyeRecipe orcauldronbehavior that changespalette/default/creativewater;
inspectactualnativehooksbeforechoosingtags/interactionmap. No fullrobevisualPASSyet.

## Shock

`ConfigRecipes251/ConfigResearch87/ConfigItems349–358/ItemFocusShock59–182`:
patternCQC/Q#Q/CQC,4airshards+4netherquartz+**potato**exactcenter;10aer/ordo/perditio.
GateFOCUSSHOCK, secondaryconcealedcomplexity1,parentFOCUSFIRE,THAUMATURGY(3,-5),
aer3/potentia6/praecantatio3. ResearchgatesCRAFT, notusingfoundfocus.
HeldRMB/CHARGE,25aercentivis/pulse,250mswallclock. DebitBEFOREhitresolution: misses,
PvPrefusals/nonliving/rejecteddamagealsocost. Entityreach20/blockLOS,broadphase1.1,
hitboxexpand≥.8. Basicdamage4 ordinaryplayer-attributed (armor/iframes/PvPrespected),
notlightningentity/magicdamage. Basicnochain/fire/orb/destruction. Beam/impact/
thaumcraft:shock sound perpulse; visualmissblockray/10fallback≠entityreach10.
Tooltipperactivation(notpertick),spritefocus_shock/color10466239/sortingBL.
ExtendFocusData'scurrentFire-onlycodec/selectionpath, notasecondprotocol orFireclone.
Fullupgradesstillpending. Currentresources suffice; noNiter/Quartzrod/infusionneeded.

## Excavation, отдельная следующая unit

`ConfigRecipes249/ConfigResearch83/ItemFocusExcavation113–298/BlockUtils134–140,237–253`:
4earthshards+4netherquartz+gemEmerald(tag),20terra/5perditio/5ordo. GateFOCUSEXCAVATION,
ordinaryconcealedcomplexity2,parentFOCUSFIRE. BlockCOLLIDERray10 ignoresfluids/no-
collision nodes. Affordability15terra/tick, **debitonlycompletedattempt** before
protectedharvest, cancellationstillpaid. Progress.05 or.25 foroldrock/grass/ground/
sand;obsidian×3,completionbeforeincrement,negativehardnessneverbreaks. Realdrop/XP/
BEhandling, noordinarypicktier/wandwear,creativecustomharvestnotfree/drop-less.
Originalreleasecallbackmismatch/tooltippertickquirks mustnotbesilently«fixed».

Приёмка только новой batch/focusedstory изacceptedstoppedgogglessave. Старые
crafting/infusionacquisitions не повторять, rawinputs declare; сохранитьoldfocus/
wands/finitevis/essentia. Мантиифункциональнытолькосdye/wash/repair/actualdiscount,
Shockтолькосactualpaidtarget/miss/blocked/refusedbehaviorandnativeFX, неcatalogue.

## Текущие ownership / root integration

A`ses_ebb62b200ffeboEsKeeoEoX0F8` SOURCEFROZEN/root reviewed: Wearables/robeitem/dye/compactruntime.
B`ses_ebb621410ffeAg4CKYoz4Bds2N` SOURCEFROZEN/root reviewed: Foci/FocusData/actions+newShock/FX/runtime.
C`ses_ebb606f85ffegGwzG5Uq2hMQer` SOURCEFROZEN/root reviewed: только newRobeShockClientSequence.
Rootownsexistingbootstrap/clienthooks/resources/book/Bookscope; никакихGradle/game
до всехSOURCEFROZEN. Scopeclothing копируетactualacceptedSTOPPEDgogglesworld из
goggles-client.saved_world вновыйprivateUUID; newbench1618,216,377/center1474,216,377.
Старыйsourceмир/3wands/173essentia/72candles/oldgoggles — INPUTS, непересоздаются.

Rootadded3robe/1dye/1Shockrecipe,4itemmodels/lang/bookaliases/3armortags/tint;
FOCUSpropertyNone0/Fire.5/Shock1 согласовансJSON/9newheldmodels. 27predicatecombos
source-checkPASS, resourcevalidation258textureRefs/192models/0missing — неgamePASS.
ProvisionalBbootstrap/rendererhook/serverwrapper70needsreviewпослеfreeze.
Clientновые81–88, actualcount8rootwired. Остальныеfull66сцены не запускаются.

Originalarmoraliases созданыровно `.tmp/alias_robe_armor.py`,unchangedsourcebytes,
все64×32/nativeArmorMaterial.Layer(ResourceLocationrobes,suffixoverlay) paths:

| Native path under textures/models/armor | SHA-256 |
|---|---|
| robes_layer_1.png | e0e1c4923c17f07f984af17790c80b2afd2523b4b5bc1536aa2d50dcc1eee47e |
| robes_layer_1_overlay.png | 2f0cf93617377f85c18fed283cf687bdf5524549f70713239c6bdcceac9b54d5 |
| robes_layer_2.png | aad4f2dfcf1e255aa8719acf5b87d765bb3416e6a741af920a779d0f0550f526 |
| robes_layer_2_overlay.png | e8b7eabad1e8325cf3a45dc615c276eab097dc27be3b6a252380183f331d4307 |

Defaultpurple/colorbase vsuntintedoverlay, distinctper-itemfabricrepair вnativeholder,
cauldroncreative/defaultwaterconsumption иnativeDyeRecipeavoidance — реальные
интеграционныеграницыA, неистолковыватьодниresourcealiasesкакготовуюодежду.
Currentnativepricequirks: Gogglesalone1vis85/3vis254/5vis424; полные10points с
ThaumiumcapдолжныдатьFire7/Shock19 (FLOATmultiply/truncate), не8/20. Вclient
планируетсяреальныйcactuswearбезoldheaditem→20thfabricanvilrepair, rawresources/
finitecharge/actualFselect/paidrangedhit+miss+wall; без full100visdepletiontreadmill.

Aпослеfreezeпрочитанroot: implementrecipe/DEFAULTprototype/strictsheeppalette/
useOncauldronfallback+creative/defaultwater/nativeequip/anvilcontracts. Rootсогласовал
nativeMaterial.Layer literal suffix`_overlay` иhelperassert сoriginal-bytealiasesвыше;
имя`overlay`безразделителяошибочноискалобыlayer_1overlay.png. Это sourcefix,
неосмотренныеслои/неruntimePASS. SerializerID`robe_dye`, ItemRobeArmor.colorRGB24,
DEFAULT_COLOR/setColor/removeColorAPIподходятroot/C. Не добавлятьminecraft:dyeable.

Bпослеfreezeпрочитанroot: платныйдоhitresolution basicShock/real250msdispatch,
20entityray+percandidateeyeLOS/armor+immunity/PvPteamprotection,2focuscodec/classes,
S2ConlyFX безC2Starget/damage. RootinitServer/clientinit/LAST APIsсовпадают,
SERVER_STOPPED clearCooldowns wired. Client/serverweakmapsразделены, потому что
Entity.equalsnumericIDсовпалбывintegratedclient; valuesнеудерживаютplayers/worlds.
FXнезависимаяnativeapproximation(32live/128mainsegments/8two-segmentforks/5glints),
камерусдвигаетсамrendercallback. Истинныйlegacyfractal/joints/branchparityпокаНЕPASS.
ReadonlycooldownMillis/pulseCost/activeBoltCount доступныC, авторскихtestcontrolsнет.
Runtimecaseauthored600ticks/realclockawait, nativearcane/codec/selection/range/LOS/
miss/nonliving/hurtcancel/armor/iframes/PvP/team/depletion/nodepriority. Ещё не запускался;
sourceCвработе, allGradle/gameзапрещеныдоfreeze.

Все sourceowners теперь FROZEN, C874linesrootreview/readonlyFXfieldsсовпадают.
Actualsavedinputsretained;20finitefabric/3robes/cactuswear/nativeoriginaldye/
coloredANDdefaultwash3→2→1/anvil20thfabric+onelevel/Fselection/4paidShockpulses
authored. 8точныхPNG81–88 planned, not yet produced/seen. NextcompileFabriconly,
затемfreshsnapshot/focusedclient/finalserverfloor70. Никакихtest/gamePASSещёнет.

`clothing-fabric-compile1` exit0. Resource refs258/192/0missing и diff-check PASS,
inventory261mainJava. Fresh `.tmp/clothing-fabric-validate1-inputs.json`; идёт
только `clothing-fabric-client1 -Scope clothing`, ожидаются81–88. Сервер70/build
ещё не запускались. Не менять source во время Gradle/game; не считать compile
приёмкой dye/wash/repair/targeting или осмотренными robes/bolt visuals.

Первый client1 завершился exit1 на WEAR после PNG81: 20fabric и все3robes реально
изготовлены/оплачены, но неподвижная teleport-позиция рядом с inset cactus не дала
контактного урона. Stopped-save NBT/region inspection: Survival, invulnerable=false,
health20, robes damage0, cactus существует на sand. Исправлен только клиентский
сценарий: настоящее native forward-key movement к cactus с пределом100ticks и
cleanup. Gameplay не менялся; failed PNG81 — только diagnostic, не visual PASS.
Новый snapshot/client2 должен копировать принятый goggles save, не failedcopy.

Client2exit1 на WEAR. Уточнение причины: сохранённый player имеет **Entity
Invulnerable=1**, независимо от abilities.invulnerable=0 и Survival. Первая проверка
смотрела только abilities, поэтому диагноз отсутствующего движения был неполным.
Настоящее движение в client2 произошло, но иммунитет закономерно отказал damage.
Client-onlyfixture теперь явно временно отключает Entity flag для cactus contact,
проверяет native isInvulnerableTo(cactus)=false и восстанавливает прежний flag после
wear/при cleanup. Это не damage/wear injection и не gameplay fix. Fresh snapshot3/
client3 из accepted goggles save; failed worlds/reports остаются диагностическими.

Client3 подтвердил native wear1 на всех3robes, но поздняя health assertion увидела
уже ordinary saturation regeneration (health20/saturation5→4). Исправлен только
passive observer: minimum native POST health + actual last damage source CACTUS;
никаких hurt/heal/wear setters. Проверка завершает contact сразу после native wear,
не выдерживает лишние10ticks. Charge budget уменьшен до3200/primal при верхней
потребности AIR2999. Freshsnapshot4: сначала server/build70, затем focusedclient4.

Validate4:200JUnit PASS,68/70server,exit1. Robe3x3 second-mix/consumption assertion
нуждается в actual-slot диагнозе. Shock реально ждал wall time, но accelerated
GameTest прошёл480ticks за0.822s; test-only hard budget повышен до30000native ticks/
36000wrapper, с отдельным10swall deadline. Нет fake clock/sleep или gameplay fix.
Fast server5 diagnostic идёт отдельно от final build/recorder; client4 ещё не run.

Server5 не запустил Minecraft из-за PowerShell single-task splat; stale XML не
свидетельство нового исполнения. .tmp runner typedstring[] исправлен. Server6
реально69/70, Shockcase PASS. Причина Robeassert — **оставшиеся blue4/white1 дают
vanilla preview2light_blue_dye**; сам robe nativeRGB A08BBF/consumption правильны.
Только fixture исправлен на exactA08BBF+законный vanilla preview, очищающийся при
возврате leftovers. Vanilla recipes и gameplay не менялись. Fastserver7 следующий.

Fastserver7 exit0/70. Client4 прошёл wear/dye/двеwash/anvil/actualequip/realShockcraft,
но testRADIAL_SELECT вызвал mouseReleased через уже закрытый nullscreen. Исправлено
хранением clickedinstance. Failed85 root осмотрел: **реальный gray wearer defect**.
Native HumanoidArmorLayer javap88–113 игнорирует DYED_COLOR без ItemTags.DYEABLE,
даже если ArmorMaterial.Layer.dyeable=true. Новый client.RobeArmorLayerMixin opt-in
делает ТОЛЬКО native render gate; vanilla dye recipe/cauldron не включены. Это
production визуальная правка, требует нового actualclient после freshsnapshot6.

Failed86 осмотрен: testafterRender повторно рисовал transparent selector поверх
первого, поэтому два tooltip/footer. Только clothing capture теперь берёт один
nativeframe после объявленного cursor positioning внутри ownedactivewindow;
ни generated screenshot, ни secondoverlayrender. Compile2 exit0, client5 ещё не run.

Client5exit1 на CHARGE_STOP (paidFire во время node charge). Source inspection:
ItemWandCasting.NODE_USES был один weakmap, хотя Entity.equals nativeID сравнивает
client/server actor как equal. Разделены SERVER/CLIENT maps, readonlyisUsingNode,
SERVER_STOPPEDcleanup; original521–572 отдельные itemcopies не имели общей сессии.
Это source authority fix, не смена цены/node/focus/data schema. **Client6 всё ещё
exit1**, actualdistinct/nativeequalactors id36 подтверждены, но Fire−8 во время
CHARGE_USE сохранился. Не считать side split уже проверенным решением всех причин.

Fresh snapshot8/client7: declaredcamera теперь фиксируется и в CLIENT_PRE до
native packets (POSTlookAt уже существовал). Пассивные native RIGHT_CLICK_ITEM
events (max64, обе стороны, callback всегдаpass) дают ray/look/hand/use/session
trace. StrictzeroFire/wholevis assertion не ослаблен. Нет нового finalserver/
SCRIPT_PASSED/осмотренного purplewearer пока. Старые70 относятся к прежним inputs.

## Приёмка текущего bounded scope

Final validate8 exit0,200JUnit/70Fabric/all70required/0failures-errors-skips,
3630unchangedinputs/48Python/oneFabricJAR notices+isolationPASS. Client7 exit0/
SCRIPT_PASSED, run`client-smoke-da7fb896-e98b-45a3-8d9d-e578ba12ef0c`,8exactPNG81–88
rootосмотрены/2м16с/2117ticks. Source-bound recorder match=true/historical=false.

Actualfinitecharge206visunits/1040ticks/zeroFire,80string+20wool→20fabric→3robes+
lastfabricrepair;nativecactuswear1each/health20→19.22(temporarysavedEntityInvulnerable
scope restored),red8C3960,двеrealwash3→2→1,anvil1fabric/1level. Missing1XPpoint
declared INPUT, не XPprogression. Threeactualearnedrobes+oldgoggles/10points/armor7,
paidShockcrafted/Fselected,oldFire returnedexact. Native4×19AIR=76,14.03blockcow
10→6,secondpaid/iframes6,paidmiss,paidwall/no extra damage. Fire7 — PRICE checked,
не новыйpaidFireclient. Старые2goldwands/3total/4phials/12jars173units/72candles/
112TCblocks/knowledgeпослеdeclaredresearchfixtures сохранены.

85purplewearer правильныйпосле render-onlyDYEABLE gate, originalbase+untinted
overlay/boots;86singleShocktooltip/currentFire. **87непоказываетразличимуюranged
lightning ribbon**, хотяcapturerequestвиделactualpaidS2C geometry. Pixelsпоказывают
redcow/blueheldfocus. 88brightblue/violet actualpaidwalllightning/impact видимы.
84jar tooltip partlycoversXPline;86footer/ordinaryHUD overlap. Эти ограничения
записаны в `porting/clothing-client.json`, не маскируются fullGUI/FX/audio verdict.

AcceptedSTOPPEDsave`fabric/run-clienttest/saves/clothing-smoke-792ae806-a552-4027-a65e-fb17312d794e`.
Rawmaterials/research/daylight/node80/floor/camera/machines/cactus/wall/cow — INPUTS,
не fullsurvival. SourceNodeUses separate logicalsides/nativeequalactorid validated
along actualcharge/release; PREcamera stabilizedfixture, двеreal item-useevents с
одинаковымray, без extraFireexpense. Нетpersisteddata/pricechanges. Baubles/Repair/
runic/upgrades/fullFX/layout/ordinaryEULA/remote/Neo/fullTC4 остаются pending.
Следующий [Excavation](2026-10-16-excavation.md): ещё contract, не implementation.
