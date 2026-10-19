# Следующий bounded M06a: клапан, резонатор и shapeless arcane

**ACCEPTED bounded Fabric checkpoint.** `valve-fabric-validate4` FULLexit0:
202JUnit/79Fabric/all79required,0failures-errors-skips,3694unchangedinputs/53Python,
one productionJAR notices/isolation/single intermediary AW PASS. SHA256
`5fa9b83378943e98e960c60607d08e39c363b54dcb0fbf6bfc8e05302dca21de`,10,235,830bytes.
`valve-fabric-client3` SCRIPT_PASSED, all7exact107–113 rootinspected, run2bd69f57,
53.289seconds/1070clientticks. [Client](../porting/valve-client.json),
[verification](../porting/verification.json), sourceboundmatch=true/historical=false,
archive `.tmp/valve-accepted-verification4.json`. All owners/Gradle/game finished;
below is implementation history, not active commands. The separate binary folder
is updated to this accepted JAR; previous voidjar/Frost backups are outside mods.
Full TC4, full survival/graphics/audio/API/ordinarypackaged/NeoForge remain pending.

### Client1 failed (not accepted) / harness ordering correction

Run `client-smoke-582bb1c6-e3ff-4b2b-a2a7-484b951009fe` stopped normally with
SCRIPT_FAILED at FIRST_FLOW. Real valve/resonator crafts paid900AQ/450O/450AER,
native4coal+1fuel produced8IGNIS+8POTENTIA; three exact resonator inspections
succeeded. Only4PNG107–110 were produced and inspected. Raw report archived in
`.tmp/valve-fabric-client1-raw-report.json`; failed world is NOT a future input.
The following empty-hand reopen never reached authoritative flow (remained
closed/zero suction; no material loss). Harness lookAt followed immediately by
useItemOn in CLIENT_POST sent use before the next native movement angle update;
after crouch inspection the old pitch no longer matches the standing eye ray.
Root now sends actual current client angles through native Rot C2S before each
scripted use, with no direct server angle/clock/state writes and no production
authority change. Opening acknowledgement now fails after40ticks, not400.
Snapshot2 stale from client-harness-only change; next run uses fresh accepted
voidjar copy and a fresh snapshot. Full build2 production PASS remains historical
for its exact input set; no new export/acceptance yet.

### Client2 stopped after user noticed another stall / screenshot routing defect

Run9db01300 produced and root inspected only5exactPNG107–111. Rot-before-use
fixed the previous hand reopen: actual flow/settled closure, rising edge close,
manual reopen under stable power20samples, and secondary jar resonator all ran.
It then opened ChatScreen for112. The root capture AFTER_INIT allowlist omitted
ChatScreen, so capturePending suspended the story until the old global600s limit.
This is a harness defect, not evidence that the valve froze.

Only exactownedPID4836/parent25044/run9db01300/world9cf76f36 received normalWM_CLOSE;
normalstop/savedplayers+chunks confirmed, exit1/NOTPASS. Incomplete raw archived;
never reuse that failed save. Root changed only BookClientSmoke: one shared screen
allowlist for registration/request validation, ChatScreen added, per-capture10s
wall deadline, immediate unsupported/changed-screen refusal, pending diagnostics
written immediately, and lateasyncI/O callbacks cannot overwrite failure.
Snapshot3 stale; freshsnapshot4/client3 from accepted void save + finalFULL4 needed.
Root verified original ConfigRecipes:283–285, ConfigResearch:307–308,
TileTubeValve:28–122, ItemResonator:52–109, CraftingManager:544–627 and inventory
BlockTubeRenderer:39–64; pinned OpenTC4 HEAD is still
`80aac29c52543ef51766c4b0788fba782abb3b0b` (readable comparisons agree).
No donor copying. Fabric first; no NeoForge development/build/game.

### Exclusive source owners (central builds only after all SOURCEFROZEN)
- A `ses_eba0c66dcffesx8wm2sjjssC8n`: TileTube, BlockTube, TubeGeometry,
  new ValveRuntimeChecks.
- B `ses_eba0c3e38ffe2MrPF5RN6iMsr3`: ArcaneShapelessRecipe, ItemResonator,
  new ShapelessResonatorRuntimeChecks / focused unit test if needed.
- C `ses_eba0c086cffe6eh2v6uL2pSttp`: TileTubeRenderer and helpers,
  new ValveClientSequence.
- Root: registries, JSON recipes/resources, book/aspect resolution, early native
  jar/alembic + Fabric generic item-first routing, label fallback, harness scope,
  Fabric test wrappers/required manifest, asset checks, docs and acceptance.

Integration IDs: `ThaumcraftTubes.ESSENTIA_VALVE` / `ESSENTIA_VALVE_ITEM`,
`thaumcraft:essentia_valve`; same existing TileTube BE type. `BlockTube(Properties,
true)` denotes valve, derived from registered state, never editable item/NBT.
`ThaumcraftContent.RESONATOR`; `ThaumcraftArcane.ARCANE_SHAPELESS_SERIALIZER`.
One static `ItemResonator.tryInspect(UseOnContext)` shared dispatcher. Fabric
UseBlockCallback handles addon transports before arbitrary block GUIs; consuming
it prevents the native block/item fallback from emitting duplicate chat.
Parent save is cloned fresh for scope `valve`, workbench1746,216,377 and old center
1474,216,377. Three actual loose tube items remain; prior fuel clocks evolve
naturally (old accepted furnace has no raw input, so no extra fuel ignition).
No new checkpoint/binary export is accepted yet.

All three owners are SOURCEFROZEN and root-reviewed. Root integrated
the three required Fabric runtime cases (expected total79, not yet obtained).
Review corrected hand use to validate the real native outline/reach/occlusion,
not accidentally require a narrow indexed wand hit; a corner-outline regression
was added. Wand/resonator continue to use authoritative indexed retracing.
Root registered the shared `TileTubeRenderer.renderValveItem` Fabric builtin hook
and native item model. All implementation inputs are now frozen for central
compilation; no runtime/visual acceptance is claimed yet.

Initial `valve-fabric-compile1` passed (production, client harness, Fabric
GameTests); both new resource checks passed. Root then made passive observation
robust to a real lever packet arriving between SERVER_PRE and the BE ticker:
record a within-tick sampled latch edge instead of discarding its evidence.
This is read-only fixture accounting; no production clock/redstone/state changes.
Final frozen-input validation and client run remain pending.

`valve-fabric-validate1`:202 JUnit PASS,53 Python PASS; actual78/79 required
Fabric cases passed. Valve phases/ports/hand/wand/drop and resonator chat/audio/
authority passed. The shapeless codec case caught a real data-validation issue:
vanilla ItemStack.STRICT_CODEC still silently defaults an explicitly invalid
count0 to1 via its lenient optional field. New shapeless result codec now uses
strict native id/count/components fields plus native component/max-stack
validation. Zero/negative/nonnumeric counts must fail both JSON and stream, not
be relabelled as a valid one-item result. Snapshot1 is stale after this fix;
archived XML `.tmp/valve-fabric-validate1-gametests.xml`. Next focused server check
and fresh final snapshot/build/client are required. No accepted new package yet.

Source prefixes below: O=`../work/thaumcraft-4.2.3.5-oracle/thaumcraft` (spec),
R=`../work/references/OpenTC4/src/main/java/thaumcraft` (readable comparison,
pin80aac29c confirmed), P=`common/src/main/java/thaumcraft` (current native).
No reference implementation is authorized for copying.

## Boundary / reuse

Smallest useful next package: **valve tubes + resonator + real shapeless arcane**.
Filter/restricted/oneway may follow as one separate TUBEFILTER package; do not
inflate this first unit or claim those variants from a generic model/registration.
**Buffer excluded**: original is8multi-aspect/randomtype/bellows/chokes/arbitration,
not another variant of the current capacity1 raw-storage contract.

Reuse shared single-unit BE/tick, paired side mutations, authoritative indexed
geometry, callback-free EssentiaStorageEndpoint and locked paired commits in
EssentiaTransfers. Do not replace them with callback take→add/refund/hiddenbuffer.
Jar minimum-suction exception remains caller-specific. Derive TubeKind from actual
registered block, never editable saveditemflags; keep one ticker, not subclass
copy/pasted phases. Separate isSideOpen(rawflag) from overridden isConnectable.

## Research / recipes

O/common/config/ConfigResearch.java:300,307–310:
- TUBES, parentDISTILESSENTIA, Secondary+Concealed, complexity1, ALCHEMY(7,0),
  aqua3/praecantatio3/permutatio3; basic tube/valve/resonator use this SAME gate.
- TUBEFILTER, parentTUBES, Secondary+Concealed, complexity2, ALCHEMY(9,1),
  aqua3/praecantatio3/permutatio3/ordo3. Extracted data already exists.

O/common/config/ConfigRecipes.java:279,283–288:
- Existing Tube: Q/IGI/B with quicksilverDROP,iron2,glass,goldnugget,5AQ/5O→8.
- Resonator shaped `I I/INI/ S `: iron4/**netherquartz1**/stickWood1,5AQ/5AIR→1.
- TubeValve **SHAPELESS** basic tube+lever,5AQ/5O→1, gateTUBES.
- Later TubeFilter shapeless tube+VisFilter,5AQ/16O; TubeRestrict tube+stone,
  5AQ/16TERRA; TubeOneway tube+dyeBlue,5AQ/8O/8PD, allgateTUBEFILTER.
- VisFilter alreadyavailable `GWG`: gold2+silverwoodplanks,5AQ/5O→2.
Rawwholevis values, not actual actorprice. Stone is NOT cobble; legacydyeBlue
includeslapis, not silently modernblue-dye-only.

Add real ArcaneShapelessRecipe implementing NativeArcaneRecipe +serializer, not
fake shaped permutations. Existing ArcaneCrafting.generic can consume it. Book
alias/resolution and recipe-derived ObjectAspectResolver must also support it
(original CraftingManager585–611), not only native menu matching.

## Valve / common engine exact traps

O/common/tiles/TileTube.java:44–53,55–120,133–291,315–393;
TileTubeValve.java:28–122; O/common/blocks/BlockTube.java:306–321;
BlockTubeItem.java:41–49. Readable Rclasses agree core rules, no copying.

- Capacity1/raw6open=true/NORTHdefault; placementfacing=clickedface, notopposite.
- Initialcount0 seedsnextInt10; outsidevent local++/suction+vent every2/pull every5.
  DOWN/UP/NORTH/SOUTH/WEST/EAST order. Vent pausescounter, notexternaladd/take;
  conflictstarttick maystillpull. Vent40server/50client, noflux/essentiadestruction.
- Pull onlyempty, own+neighborsidedports, ownsuctionsourceSTRICT>, own≥sourcemin.
  Wanted suctiontype→sidedtype→UNKNOWN; firstsuccessstops. Variants min0,
  extendedrenderfalse. Vent requires equal orown−1 neighborsuction, typesdifferent
  including null disagreement.
- Valve defaults flowtrue/hadpowerfalse. Facing is wheel/stemside, excluded by
  isConnectable ALWAYS. Closed prevents setSuction after basecalcresetsraw0/null.
  **Not a hardadd/takegate**: bufferkept, downstream maydrain, oldsuctionsurvives
  untilrecalc. InheritedcanInput/Output checkrawflags, notstemexclusion.
- Samplepower BEFOREbasetick when PREcount%5==0; risingedgeclosesifopen,
  fallingopensifclosed. Stablepowerdoesnotforceclosure; manualreopen staysuntil
  anotheredge. hadpowerupdatesoneachsample. Count0samplesbeforeRNGseed; ventfrozen
  count maymeansampleeverytick ornone. No secondtimer/continuousredstonepoll.
- Hand togglesexceptwand/resonator/ANY tube-blockitem (all flattenedvariants).
  Wandarmassignssamerawflagtooppositeneighbortube, nottoggleisConnectable(stemtrap).
  Valvecore cycles tofacingwithNOtransportneighbor; othercores seek opposite
  connected/openneighbor. Closedarmsstayindexedtraceableforreopening.
- Valveclientrotation20degrees/tickwithin0..360, includingvent; nooriginalpartial
  interpolation. Saveflow/hadpower alongside sharedfields; omittedboolloadfalse,
  newplacementtrue. Count/vent/rotationtransient; customupdateomitsauthoritativesuction.
- Brokenvalvedropsownplainitem, NOTbuffer/config; comparator0.

Later variants: restrict integerneighbor`suck/2`, keepsequentialacceptance
`suck>currentReduced+1` (64→32 then35→17!), notmaxthenhalve. OnewaygetsuctionONLY
fromfacing.opposite, pullscanEXCLUDESthatfacebutallothersallowed; no2-portdiode.
Filter restrictsSUCTION, notlowleveladd; untypedsuctionbecomesfiltertype, cannot
createpressurealone/purgebuffer. ItsIAspectContainer `{filter:-1}`/null is a marker,
notstorage; stubs mustnotfeedatomictransfers. Boundlabelonly/blankdoesnotinfer,
creativepays1; sneakreturnblanklabelonce, nofilledtubedrop. Atomicfilter+detachedhand
beforecallbacks and cleardownershipbeforedurablereturn, notnotifythenpay.

## Resonator / item-first routing

O/common/items/relics/ItemResonator.java:52–109: stack1/nondamageable/uncommon,
items/resonator. AnyIEssentiaTransport, notonlynativeendpoints. Clickedface replaced
byactualindexedarm0..5; corekeepsclickedface. Report tc.resonator1 whennonnulltype,
always tc.resonator2 suction/type (nullname tc.resonator3). Buffer-onlyspecialcase
doesnotauthorizefilter−1contents. Serverchat+alembicknock.5volume/pitch1.9–2;
clientswing, no vis/damage/researchusegate, glintonlyNBTpresence.
One validatedinspecthelper usedearlyblockdispatch+secondaryitemuse, avoiding
duplicatemessages/valvetoggle/jarlabelremoval while sneaking. useOnalone is not
item-first. Rootmustwireearlyjar/alembic/tube paths and label fallback consciously.

## Visuals / checks

O/client/renderers/block/BlockTubeRenderer.java:39–77,131–235 and
tile/TileTubeValveRenderer.java:32–64:
- Valvealwayslargecore6..10sixteenths/pipe_2; stemmodels/valve.png and extruded
  blocks/pipe_valve (verified actual BlockTube:105 icon registration),
  scale.5/extrusion.1, rotation−1.5*rotation/travel−.12*r/360.
- Filtercore5.5..10.5/pipe_filter+aspecttintedpipe_filter_corewhiteunset.
- Restrictshaftpipe_restrict, isolated/nontubecorepipe_2override.
- OnewayworldindicatorTHREEbluestempiecesonlywhenoppositeconnects, pipe_oneway
  inventorytexture isNOTthatindicator. Existingnativepipe_3facingmark is a known
  discrepancy, notcanonicaldiode. RetainedPNGs alreadyexist, noartworkdonation.

Native/servertests: realshapelessmanager/menu/arbitraryslots/extra+variant+gate+vis
refusal/onecellpayment/quickmove; valvecount/redstoneedges/stablemanualoverride/
stemrawports/residualbuffer/ventphase/savehadpower; callbacklockfences; wandindexed
closedreopen/corewheel; resonator onjar/alembic/tube includingsecondary; plainown
drop/save/customupdate. Optionalfilterphase gets separate paidlabelgate.

FocusedclientfromactualSTOPPEDnextacceptedvoidsave (notassumeall6tubeitemsremain):
retainedbasicTube+oneRECIPElever+iron4/quartz1/stick1→realvalve/resonator, secondlever
externalredstonefixture. Realplacement/wandorientation/handtoggle/leveredges/chat,
4rawcoal+1fuel→8IGNIS+8POTENTIA throughnewemptyfurnace/twoalembics. Allowresidual
bufferdrain, assessclosureaftersuctionssettle. Noessentiagrants/clock/RNGchanges,
nooldprogressionreplay. Visualframes/outputretention pricedwithactualsavedgear.

Suggestedownership: AtransportBlockTube/TileTube/Geometry+compactruntime;
Bshapeless/resonator+focusedrecipes/toolcase; Cvalverenderer/focusedstory;
rootregistry/recipes/resources/book/aspectresolver/earlyrouting/wrappers/docs.
Ownership above supersedes the original proposal; tests/visual acceptance remain pending.
