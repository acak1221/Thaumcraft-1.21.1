# Следующий Fabric-контент: исходная инфузия → Silverwood100vis

**Принят Fabric-only `infusion-fabric-validate7`/`infusion-fabric-client5`.**
200JUnit/65Fabric/3521unchangedinputs/48Python,60captures иexact53–60осмотрены;
source-bound recorderPASS. Оригинальный контракт ниже остаётся спецификацией,
история interimfailedruns не является текущим статусом. НикакогоNeoForgePASS.
Oracle4.2.3.5 — спецификация; ARRReforged не копировался и не подменяет её.

## Реальная награда и зависимости

- ConfigResearch185: INFUSION, concealed, parentDISTILESSENTIA. ROD_silverwood109
  требует ROD_greatwood+INFUSION. Существующие research definitions не заменять.
- ConfigRecipes221: Matrix `SBS/BEB/SBS`,4arcane stone+4любых shards+ender pearl,
  ordo40. Pedestal222: `SSS/ S /SSS`,7arcane stone→2, aer5. GateINFUSION.
- **Исправление прежнего next-content предположения:** ConfigRecipes336 центр
  silverwoodLOG, НЕgreatwoodrod;7компонентов balanced+6primalshards. Instability5,
  **9каждого primal+9praecantatio**, total63essentia. ConfigItems314: rodcapacity100,
  craftCost9. Не прежние «5complex/5primals×20». Salis Mundus здесь не нужен.
- Для полезного результата нужны nativeSilverwood material/capacity10000 и codecs,
  roditem/model и existingarcaneassembly27каждого primal до modifiers. Новый wand
  пустой; затем настоящее nodecharging выше50 иFire, не suppliedchargedoutput.
- Essentia из нынешних primalshards: каждый2своегоprimal+1praecantatio+1vitreus;
  wastevitreus учитывать, не вписывать готовые63units. Balanced есть вcruciblerecipes.

## Структура и предметы

- WandManager135–205: centralpedestalC, MatrixC+(0,2,0),4brickbasesC+(±1,0,±1)
  и4arcane stone над ними, остальные3×3×3air. Formation25каждого primal через
  existingcraftingmodifier; последующиеactivate/recipe-start бесплатны поwandvis.
- Formation pillars заменяют bases/top, сохраняют ориентации углов2/3/4/5; обычные
  drops возвращают stone/bricks. StructurecheckM230–250: центральныйpedestal+4lower
  pillarBE, не повторный полныйairblueprint. Парностьupper/lower черезneighbourlogic.
- TilePedestal83–100,155–191: обычныйslotlimit1/all-sidedIO; infusion-setter может
  заменить полную outputstack безclamp1. ПКМ ejectoccupied/insert1empty, noownerlock;
  препятствие сверху ejectсодержимое (BlockStoneDevice329–378).
- M609–668 scanorderdx−12…12→dz−12…12→yy−5…10; y=matrixY−yy; центральныйXZстолбец
  исключён. Pedestals onlybelow/|dx,z|≤8/depth1…10, первый сверху встолбце.
- Symmetry: pedestal+2/occupied+1; opposite−2/bothoccupied−1. Stabilizer+0.1,
  opposite−0.2; intcast(total) кнулю. Skulls/IInfusionStabiliser, неvanillacandles/
  flowers. Sourcequirk665: oppositecallbackполучаетисходныеcoords. Tallowcandles
  доступны stringнад2tallow→3 (R477); crystalblocks могутstabilize, неinfusedores.

## Серверный цикл и источники

- M252–333 snapshotцентра/компонентов, matching unorderedexactmultiset; recipeinput
  использует originalore/NBTsubset semantics, не strictmoderncomponents всёвезде.
  Изъятия при старте нет. Craftcycle10ticks, structure20crafting/100idle; I=symmetry+r.
- M453–473: по insertionorderаспектов, перваяуспешнаяпокупка1essentia завершаетcycle.
  Каждый failedrain сшансом1/(100−3r) повышаетI, cap25; при полномнеуспехеrescan.
- M474–502: remainingingredients→orderedpedestals, countdown5 затем5decrements:
  **6успешныхcycles накомпонент**; remainder replaces consumed item. Missingindexa
  можетadd1randomrecipeaspect сшансом1/(1+a) иI+1 шанс1/(50−2r). Нулевыеkeysretain.
- EssentiaHandler39–59,84–134: wireless=IAspectSource.takeFromContainer(type,1),
  **неjarUP/suction/label**. Scandx−12…12→dz−12…12→dy−12…11, безLOS/nearestsort.
  Cachecoordinates, каждыйsourceBEresolveзаново; stale/non-sourceentry делаетbreak.
  Exhaustion очищаетсписок+5000mswallclockbackoff; refreshнеудаляетdelay. Nativecache
  scopedServerLevel/unloadcleanup/loaded-only. Не общийmapпоdimensionмеждусерверами.
- ПослеM06 нужен маленький consume transaction: jar−1 иmatrixdemand−1 доcallbacks;
  никаких take→callback→дублированныйдебит/потеря. Новыйgenericframework не нужен.

## Instability без пустых handlers

- M335–419: invalidcentral ИЛИ validI>0&&nextInt500≤I→nextInt21. Validhazard съедает
  цикл; invalidпослеэффектаabort/no refund. Не clampI0/не rerollunsupportedoutcome.
- EjectIDs0/2/10/13→type0;6/17→1;1/11→2;19→3;7→4;4/15→5. До50попытоквыбрать
  peripheralnonemptypedestal.0/1/2/5dropstack,3/4destroy;1/3goo7,2/4gas7,5blast1.
- IDs3/8/14zapfirst,12zapall: **magicdamage4…7**, неweatherLightningBolt.
  5/16harmfirst,18harmall: taintpoison120/0 илиambientuncurablevisexhaust2400/0.
  9blast1.5+nextFloat;20warpnearbyplayer25%sticky1иначеtemporary1…5/counter+sync.
  Эти instabilityвзрывы НЕразрушаютterrain. Снятие craftingmatrix — отдельный
  originalblast2 Сразрушениемterrain (BlockStoneDevice312–317).
- Нужен небольшой realPotionFluxTaint52–64 (taintdamage/undead/ITaintedMob/40>>amp),
  не полнаяM11ecology. Для зачётной награды поддержать все21достижимый исход.
  Честные intermediateformation/pedestal/sourcepartials допустимы, zero-Icraft —нет.

## Сохранение и владельцы

- M120–201 persistactive/crafting/I/demand/remainingingredients/centralsnapshot/
  output+label/recipeinstability/type/XP/player. Reloadнеповторяетmatching/startdebit.
  count/itemCount/delay/symmetry/FX/cache transient:rescan/ingredientcountdownrestart.
  Brokenstructureinactive сохраняетsession; repair+wandresume. Logoutнеблокируетcraft;
  recipePlayer — attribution/callback, неowner. NativeUUID —явнаяadaptation.
- ПерваяunitSTACKoutput; enchantment/tagpatchregistrations отдельноpending. Исходный
  Enchantmentoutputне сериализуется — отдельная будущаяnativefix, неготовыйresume.
- Aengine: Matrix+recipe/serializer/session/save/wireless. Bworld: Pedestal/Pillar/
  formation/orderedscan/symmetry/hazards/candles/taint. Root: registry/wand/Silverwood/
  codecs/recipes/book/models/FX. ПослеTubeFabriccheckpoint согласовать малыеAPI.
  Actualclient:63essentia+7components→core→wand27/primal→charge>50→Fire. Всё выше —
  следующая работа, не заявленный build/runtime/visualPASS.

## Реализация и текущие границы

- Engine/world/visual sources frozen. Root связал регистры, bootstrap, wand-hook,
  реальные Matrix/Pedestal/Candle/Silverwood recipes, 10000centivis/27craftCost
  codecs и модели Silverwood. Клиентский сценарий ещё пишется отдельным агентом;
  до его freeze центральный Gradle/game не запускать. Подробные владельцы вHANDOFF.
- Native wireless атомарно уменьшает source и matrix-demand до обоих notifications.
  Не вызывает callback-bearing take/refund; сторонний IAspectSource без внутреннего
  storage endpoint пока отказывается обслуживать. Это явная API-граница, не loss/dup.
- Все21исхода нестабильности имеют настоящие world effects. Сложные enchantment/
  NBTpatch/runic recipes не регистрировались. GenerallegacyNBT/wildcard/addonore
  сериализация вне первогоplainSTACKrecipe ещё отсутствует; original_log_wood
  эквивалентность вынесена в явный itemtag из8исходных пород, не все modernlogs.
- Native Matrix8cubes/runes/halo, PillarOBJ и реальный itemBER реализованы; FX без
  gameplay-mutation. Boltfractal/endcaps, particlepushout/per-facefragment — адаптации.
  Root добавил originalcraftfail на event6; craftstart/infuserstart/cadence принадлежат
  клиенту, без второго serveraudio. Пока нет runtime/audio/visual verdict.
- Book STACKdiagram сохраняет declaredsilverwood input,7shards,63cost иI5; формирование
  показывается3layerplan по реальным deviceIDs/costhelper. Это nativeGUI-адаптация,
  не исходный вращаемый OpenGLconstructpreview. Whitecandle сохраняет исходные wax/
  wickboxes(6,0,6→10,8,10)/(7.6,8,7.6→8.4,10,8.4), но position-randomizedwaxdrips и
  остальные цвета pending. Пьедестал — исходные3boxes.
- PotionFluxTaint — исходный урон1/undead/ITaintedMob/period40>>amp. Customtaintdamage
  exhaustion0/never-scaled/bypassesarmor/witchmagicresist. Его 18×18 icon вырезан без
  перерисовки из исходного `textures/misc/potions.png` rect[54,216,72,234],
  соответствующего original setIcon(3,1); `.tmp/crop_taint_icon.py` воспроизводит crop.
- Pendingcentralfloor **200JUnit/65Fabric**, не заявленный результат. Ordinary
  dedicatedserver EULA/packagedremote/полныйsurvival и NeoForge остаются внеприёмки.
  Metadata inventory234mainJava/34partial/900pending/0complete; ledger1044needs_review,
  не процент завершения. `.tmp/record_infusion_client.py` принимает только новый
  SCRIPT_PASSED с key`infusion` и exactinspections на всех53…final.

Read-only review нашёл и rootисправил originalquirk: M252–333/412–419 не сбрасывает
`itemCount` при abort/start в той же загруженной матрице. Частичный countdown теперь
переживает такой restart; только reload сбрасывает transientprogress. Render-target
очищается независимо. Это source-backed сохранение поведения, не выдача нового
runtimePASS и не возврат уже потраченной эссенции.

Все sourceagents теперь frozen, clientcontinuation53–60 включён в rootBookStage/
reportkey`infusion`, счётчик52→60. Первый Fabricbuild остановился на одном неверном
1.21.1API: `Minecraft.getItemColors()` отсутствует. Root добавил client-only read-only
accessor к настоящей native tint table для particlefragments, без подмены цветов/
gameplay. `infusion-fabric-validate2` выполняется поfreshsnapshot; итог ещё не принят.

`infusion-fabric-validate3`exit0:200JUnit/65Fabric/allrequired65,
failures/errors/skips0/3521inputsunchanged;48Python/FabricJARnotice+isolationPASS.
Послеrun2 поправлен только новыйtest: nativeapplyCraft заменяет wand оплаченной
копией; проверкачитаетреальныйslot10, нестарыйprecommitJavaobject. Производственный
платёж27/primal не менялся. Клиент`infusion-fabric-client1` выполняет60scenes;
exact53–60 ещёнеосмотрены. Это сервернаяприёмка, неfullclient/visual/survivalPASS.

Client1 reached52старыеscenes, затем новыйM07fixture отказался из-за ожидаемого
«пустого»hotbar8: тамзаконнооставались6нативныхтрубM06. Rootдобавил настоящий
inventorySWAPвпустойmain slot передM07prepare; outputs неочищены/неперезаписаны.
Client2 выполняется, freshsnapshotvalidate4снятдозапуска; finalbuild/gamevalidate4
нуженпосле последнегоclient-sourcechange. Клиентский/визуальныйPASSещёнепринят.

### Найденный настоящий client/server дефект и последующий прогон

Client2 выполнил настоящие48shards→192units→63one-unitpayments/7components→core,
129surplus/waste осталось. Стержень был физически подобран: read-onlysavedNBT
показывает playerInventorySlot4/count1. Сценарий остановился, потому что client
продолжал видеть его на пустом пьедестале. Native1.21ClientPacketListener игнорирует
**пустой**CompoundTagBEupdate (`method_38542`/isEmpty). Rootисправил TilePedestal:
явныйCount0 записывается всегда, поэтому emptyupdateнепустой и убираетghostitem.
Ресурсы/спавн/оплата не менялись; servercase дополненпроверкойactualupdatepacket.

`infusion-fabric-validate5`exit0 послефикса:200JUnit/65Fabric/allrequired65,
failures/errors/skips0,3521unchangedinputs,48Python/Fabricnotices+isolationPASS.
Rootосмотрелexactclient2PNG53–58: читаемыйbookс7shards/7cost9/I5, nativeMatrixmenu,
formedOBJpillars/eight-cubeMatrix/72свечи,8typedjars,цветнойpaidessentiastream,
реальныйcore. Ноclient2FAILED, неfinalPASS. Client3 выполняетполные60scenes заново;
его53–60нужноосмотретьотдельно. НикакихNeoForgebuilds/runtimeнебыло.

Client3 уже прошёл pickup/нативные2goldcaps/assembly27каждогоprimal иреально
зарядил заработанный100viswand до60aer (NORMALnode80→20), сохранив oldwand/
129essentia. Остановилсянаvisualprobe: nativeClampedItemPropertyFunction.call
клипуетродproperty2до1, значит выбирался Greatwoodmodel. Rootсогласованноизменил
propertyиJSONthresholdsнаwood0/greatwood.5/silverwood1. Никакой заменыitemoutput/
данныхwand дляproof. Validate6exit0:200JUnit/65Fabric/48Python/3521unchangedinputs/
Fabricnotices+isolationPASS послеэтогоfix. Client4 выполняется, final60PNG/evidence
ещёнеприняты; oldclient3FAILED/59scenes невыдаватьзаfinalPASS.

Client4 остановился раньшеM07: nativejaritem со случайным offset оказался вне
fixedTube-pickupcamera. Root меняеттолькоfixture: реальныйplayerподходитк ТОМУЖЕ
jarentity с collision-free стороны, с исходной nativepickup задержкой и физикой;
самitem не перемещается/невыдаётся, outputданные сверяются с observedUUID.
Validate7exit0послеэтойправки; client5 выполняется заново. Финальныйрезультат60PNG
и source-boundevidence всёещёpending, никакогоNeoPASS.

## Принятый итог

Client5exit0/SCRIPT_PASSED60, run`client-smoke-15fab7dc-e9ea-4a9f-b22d-9239a5ff09b3`;
rootосмотрелвсеexact53–60 этогоrun. NativeMatrix/5pedestalcrafts→10/24candlecrafts→72,
9pedestalsplaced/1retained, actualformation4lower+4upperpillarhalves за2500eachprimal
greatwood/goldpayer.48rawshards→192units черезordinaryfurnace/3typedalembics/
24nativephialwithdrawals+24jarinsertions;3из8coalзажжены/5осталосьнеиспользованными.
Native63drainsпо1/10ticks+7ingredientsпо6cycleseach,1057observedserverTicks,
actualcoreeject/pickup/2nativegoldcaps/assembly2700eachprimal→EMPTY100viswand;
realheld-use6000aer, finiteNORMALnode80→20.129essentia/oldpaidwand/focus/book/apple/
4emptyphials/6tubes/8drops/sparepedestal retained. Настоящие72pairedcandles дают
symmetry−6/I−1, noinstability/seedwrites; все21hazardsпроверенысервером, неclientstory.

Rootвиделreadablebook/actualmenus/OBJpillars/eightcubematrix+glow/candlewax+wick/
coloredpaidstream/core/правильныйpaleSilverwoodheldmodel. Nativeoverrideprobe
подтвердилspriteIDssilverwoodrod+goldcap, tooltipCapacity100/Aer60/100. Exactcharges
иconservation невыводилисьизPNG. Audioнепрослушивалось, nativebolt/fragments/
waxdrips/othercolors/полныйUI/FXparity pending;56снялimmediateempty-handtransition,
55heldwandчастичноскрываеталтарь. Bookformationlayerplanнеcaptureвэтомstory.
Фокус/Fire сNEWwandнепроверялись, никакихfullsurvival/Neo/packaged/remoteclaims.

Freshsnapshot7 связываетвесьacceptedtree; recorder`porting/verification.json`PASS
200JUnit/65Fabric/48Python/3521unchangedinputs/Fabricnotices+testisolation,1artifact.
`porting/infusion-client.json` хранитrawboundaries/hashes/8exactinspectionobservations.
Следующаяequipmentunit отдельная, новыеправки неявляютсявалидированнымиэтимсрезом.
