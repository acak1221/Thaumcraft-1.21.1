# Следующий отдельный gate: фильтр, ограничитель, направленная труба

**M06b ACCEPTED Fabric-only checkpoint.** `tubefilter-fabric-validate3` FULLexit0:
203JUnit/82Fabric/all82required/0failures-errors-skips,3714unchangedinputs,
56Python86.717s. Client3 SCRIPT_PASSED/all8exact114–121 rootinspected,112.0367s,
runba631792/stoppedworldcf7a6c49. [Client](../porting/tubefilter-client.json),
[verification](../porting/verification.json), archive `.tmp/tubefilter-accepted-verification3.json`.
При записи sourceboundmatch=true/historical=false. OneproductionJAR10,250,855bytes
SHA256`18169f73b820c81c4c2e67a6082214f7a6d0a19253b1f55c3b59a70b4302f409`,
ZIP/notices/testisolation/singleintermediaryAW PASS. Usableexport обновлён, прежний
valve/voidjar/Frost вbackupsвнеmods. Ниже — история реализации, не activecommands.
Night/cropping/GUIicons иwrongtooltip115 явно ограничивают pixelclaims; полное
visual/audio/packaged/remote/fullsurvival/API/TC4/NeoForge не принято.

## История начала M06b
После принятия M06a (`valve-fabric-validate4` / `valve-fabric-client3`) сразу начата
эта отдельная группа, без ожидания нового подтверждения пользователя. Три source
owner: A — общий transport/paid-label engine; B — native regression cases;
C — shared renderer/focused client. Root — registry/resources/recipes/book/icon,
Fabric item-first routing, harness scope, интеграция и итоговая приёмка. До всех
SOURCEFROZEN не запускать Gradle/game; активных сборок сейчас нет.
Предыдущая приёмка остаётся исторической для своих точных входов; usable export
остаётся на принятом M06a, не на текущем разрабатываемом коде.
Буфер, центрифуга и прочая автоматизация сюда не входят. Fabric first.

## Проверенные источники

- Original oracle `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/` — спецификация.
  `common/config/ConfigRecipes.java:286–288`, `ConfigResearch.java:308`;
  `common/tiles/TileTube.java:133–208,210–281`;
  `TileTubeFilter.java:18–83`, `TileTubeRestrict.java:11–14`,
  `TileTubeOneway.java:11–19`;
  `common/blocks/BlockTube.java:298–303,323–345`;
  `client/renderers/block/BlockTubeRenderer.java:39–77,200–234`;
  `client/renderers/tile/TileTubeOnewayRenderer.java:26–49`.
- Readable OpenTC4 at `80aac29c52543ef51766c4b0788fba782abb3b0b`:
  `TileTubeFilter`, `TileTubeOneway`, `ConfigRecipes:280–282` agree on these rules.
  References are read-only; no copying rights are established.

## Gameplay contracts

Research `TUBEFILTER`: ALCHEMY(9,1), complexity2, Secondary+Concealed,
parentTUBES, aqua3/praecantatio3/permutatio3/ordo3. Use the already extracted data,
not a new unlock. All three upgrades are **real shapeless arcane**:

| Variant | Actual ingredients | Base whole-vis cost |
| --- | --- | --- |
| Filter (old metadata3) | basic tube + Vis Filter | aqua5, ordo16 |
| Restrict (5) | basic tube + `stone` | aqua5, terra16 |
| Oneway (6) | basic tube + `dyeBlue` | aqua5, ordo8, perditio8 |

`stone` is not cobblestone. Legacy `dyeBlue` includes lapis lazuli; do not repeat
the modern-tag-only ink-sac defect from jar labels. Preserve relevant modded tag
compatibility without accepting arbitrary differently coloured dyes.

Reuse the existing one-unit transport engine, indexed geometry and locked paired
storage transactions. Derive kind from registered block state, not saved flags.
Low-level add/take stay raw-side operations; these are suction policies, not new
hard external item/essentia gates.

- **Filter:** neighboring typed suction must agree with configured filter;
  untyped suction becomes the filter type. An empty nearby network supplies no
  pressure: the filter does not invent suction. Existing buffered essentia is
  retained; typed conflicts with that buffer remain relevant. It does not purge
  or reinterpret contents on label changes.
- **Restrict:** iterate native direction order; for each accepted candidate use
  integer `neighborSuction / 2`, while the next acceptance compares against the
  **already reduced** current suction +1. This is not `max(all)/2`:
  candidate64 then35 yields32 then17. Preserve this observable quirk.
- **Oneway:** calculate suction only from `facing.opposite`; its pull scan excludes
  that same side and otherwise retains all other raw open faces. It is not a
  two-port diode and does not override external raw canInput/canOutput.

## Filter labels, API and durable returns

- Only an actual **bound** label can set a blank filter. A blank label does not
  infer from tube contents. Consume one even in creative.
- Sneak removes the configured filter and returns **one blank** label; original
  removal precedes attachment. Resonator item-first inspection must intercept
  before either path, including secondary use. Do not let a tool remove labels.
- Clear ownership before enqueuing the durable return; commit detached hand and
  filter before callbacks on attachment. Failed/stale/locked/reentrant requests
  cannot produce unpaid filters or multiple returns. Keep drop-cancellation and
  replacement callbacks covered by real native tests.
- Breaking a configured filter drops its plain own tube item plus one blank
  label, not a configured/filled item. No saved one-unit contents in any tube drop.
- Original `IAspectContainer.getAspects()` is `{filter: -1}` or null, a marker,
  **not stored essentia**. Other container methods intentionally do nothing.
  Never adapt that marker into the transactional storage amount or resonator's
  buffer-only multi-aspect branch.

## Rendering and focused checks

Filter core:5.5..10.5 sixteenths, `pipe_filter` shell and aspect-tinted
`pipe_filter_core` overlay (white when unset). Restrict shaft:`pipe_restrict`;
isolated/non-tube-neighbor core still uses `pipe_2`. Oneway inventory uses its
original overlay; world indicator is **three blue stem pieces**, drawn only when
the opposite transport actually connects. Its renderer uses retained valve-model
art, not a recoloured arbitrary cube or a binary item flag.

Required native cases: shapeless/refusal/one-cell costs; original stone/lapis
inputs; sequential reduced-pressure quirk; one-way source/pressure faces;
untyped filter inference/no phantom pressure; real bound-label payment and one
durable blank return under creative, secondary, cancellation and reentrancy;
save/update/raw-buffer conservation and unchanged basic/valve behavior.

Design the client only after M06a is accepted, from its real stopped output save.
Read its actual remaining vis/tubes first: the current void-jar parent has only
500ordo/500aer and M06a genuinely spends450 of each, so later recipes cannot be
financed by invented topups. Recharge from a real accepted node if needed.

## Текущая интеграция root (ещё не проверена сборкой)

IDs `essentia_filter/restrict/oneway`, все три genuine arcane-shapeless recipes,
original stone и lapis+modern/conventional blue compatibility, book aliases/icon
metadata3/5/6, loot plain own item, pickaxe tag, native EN/RU names и BER-only model
shells добавлены. Same BE type использует `TileTube::create`, чтобы filter-only
`IAspectContainer` marker сохранялся также при native load, не только placement.
Label Item.useOn и Fabric secondary/plain-stick removal направлены в общий
`BlockTube.tryLabelInteraction`, resonator идёт первым, wand исключён из label
shortcut. Scope `tubefilter` клонирует именно accepted stopped valve save в новый
`tubefilter-smoke-UUID`, anchor1778,216,377; ожидаются8кадров114–121 (final C contract
ещё проверить). Research catalogue gate и asset/data tests дополнены, не запускались.
Root asset preflight `.tmp/tubefilter-assets-preflight1.log`:30/30PASS,82.255s;
это лишь preliminary resources, не final source-bound acceptance. B SOURCEFROZEN/
root-reviewed,3native wrappers+required IDs подключены (**82expected**, ещё неrun).
C также SOURCEFROZEN/root-reviewed; root подключил все3builtinitem hooks к
`TileTubeRenderer::renderAdvancedItem`. Все production/test/client ownersdone,
активных агентов нет; можно начинать centralized Fabric-only validation. Exact
capture contract114–121 подтверждён, всего8. Новая группа ещё NOT accepted/exported.

CentralCompile1 exit0; FULL`tubefilter-fabric-validate1` exit0:203JUnit/82Fabric/
all82required/0failures-errors-skips,3714unchangedinputs, fresh56Python82.922s.
OneproductionJAR10,250,855bytes SHA256
`18169f73b820c81c4c2e67a6082214f7a6d0a19253b1f55c3b59a70b4302f409`,
ZIP/notices/testisolation/onecanonicalintermediaryAW/threeactualrecipes/models PASS.
Focusedclient1 из freshacceptedSTOPPEDvalvecopy запускается,8exact114–121 ещё
не получены/не осмотрены. Сборка/серверный PASS не равны client/visual acceptance;
usable export пока принятый M06a. Sources frozen/no parallelGradle/game.

Client1 runa6a66a2f exit1/SCRIPT_FAILED at STORE/binding,5captured114–118; native
nodecharge and five paid crafts succeeded. Harness wrongly expected phial in
cell8; original SlotCraftingArcaneWorkbench49–63 and accepted native menu139–144
correctly prefer player INVENTORY, which had room. Root fixed only client grid/
remainder expectation and removed nonexistent cellpickup; exact savedphial must
already be in inventory once, matrix empty. Failedworld35b5a67a archived/notreused.
Freshsnapshot2/client2 acceptedvalvecopy + finalFULL2 needed; no gameplay change.

Client2 run36af1d9d exit1 LABEL_ACK,5PNG114–118; actual original inventory-FIRST
phial remainder, all5paidcrafts,8+8distillation and3variantplacements passed. Bound
label remained in hand, filter blank; native resource ledger conserved all inputs.
Stoppedfailedsavea44c1813 diagnostic only. Read-only savedNBT confirmed actual
camera/bodyyaw/pitch/heldboundlabel/correctthreeblockkinds. Added native angled
camera regression: a living actor's viewyaw is HEAD, setting onlybodyyaw leaves
the ray on a neighbor. Correct complete actorfixture passes, server3b82/82 incl
inventory-first remainder test. Client-onlyuse now sends ordinary aim/Rot for
6nativeclientticks BEFORE use; labelACK bounded40ticks. Production LOS/reach/
geometry/payment unchanged; snapshot3/client3freshacceptedcopy + finalFULL3 needed.

Client budget: actual gold7 starts50AER/860AQ/500IGNIS/50ORDO/950PD/2300TERRA;
реальная зарядка из сохранённого normal node1623,218,374, без direct writes/RNG.
Five paid crafts (new8tubes,2visfilters,threeupgrades) при savedgear стоят
2250AQ/3060ORDO/1440TERRA/720PD centivis. Savedquicksilver1 + rawiron2/glass1/
goldnugget1, rawgoldingots2/silverwoodplank1, stone1/lapis1 — объявленные inputs;
label binding расходует actual savedblank1, actual savedpotentia8phial возвращается
неизменным native remainder. Output/vis/essentia/XP topups не допускаются.
