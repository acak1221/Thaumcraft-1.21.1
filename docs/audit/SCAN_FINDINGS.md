# Thaumometer knowledge audit — g25scan

Baseline: `7f37e8a6`, Fabric / Minecraft 1.21.1, 2026-10-23.
Specification: original TC4 4.2.3.5 JAR, SHA-256
`3dba9786966974701578a658d1bb369bf35bdf5f363079f5ac9c4910a39113be`.
Read-only private oracle: `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle`.
Legacy `src/` is **not** the gameplay specification.

## Evidence boundaries

Five requested kinds inspected: item, block, entity, node, other phenomenon.
The original actually stores three buckets: objects (including picked blocks and
item entities), entities, phenomena (nodes). Registry-based native keys replace
old numeric-ID hashes in fresh worlds; identical raw key strings across versions
are not expected.

New tests use a connected, non-mock survival `ServerPlayer`, an embedded Netty
connection, the actual thaumometer RMB entry point, a real indexed entity or
placed block, and twenty real server-world ticks. Prerequisites and targets are
declared fixtures, not survival acquisition. Node scans use the original
`hard_mode_nodes=false` server option to isolate knowledge from hungry-node
motion/destruction; node ecology is not claimed. Outbound packet inspection proves
server emission, not rendered client visuals. Player save/read is not a socket
disconnect/reconnect.

**Result:** four focused dedicated-server GameTests PASS; **five kinds inspected
and driven** (four accepted physical categories, one rejected phenomenon),
**12 bounded rule groups driven**, **two gameplay mismatches PROVEN and fixed**.
No wrong-player record or research-table disconnect was reproduced.

## Kind matrix (5)

| Kind / witness | Original record / washed aspects | Native baseline | Test | Fix / status |
|---|---|---|---|---|
| Item: diamond drop | Object key; `vitreus:4, lucrum:4`; first discoveries add 2 each | Exact match, including 6/6 first discovery rewards | `scanKnowledgePhysicalKindsAndRules`, `scanKnowledgeMissingParentsAndDiscovery` | **PROVEN PASS** |
| Item boundary: contained essentia plus public addon base | Base `aer:4` + contained `aer:64` = **68**, not 64; object key | Baseline64; fixed68 | `scanKnowledgePreservesContainerBonusAboveBaseCap` | **PROVEN RED→GREEN**, final bonus recap removed |
| Block: stone | Picked stone object key; `terra:2` (`ConfigAspects:114`) | Exact match; table consumes one scan-earned terra, leaves one | `scanKnowledgePhysicalKindsAndRules` | **PROVEN PASS** |
| Entity: Enderman | Entity variant key; `alienis:4, iter:2, aer:2`; direct `@MIRROR` clue | Exact match, one research notice | `scanKnowledgePhysicalKindsAndRules` | **PROVEN PASS** |
| Node: all 6 types with current `ordo:50` | UUID phenomenon key; normal `ordo:5`; unstable adds `perditio:4`; hungry `fames:4`; tainted `vitium:4`; pure `ordo:7,sano:2`; dark `ordo:5,mortuus:2,tenebrae:2` | Exact match for every type; live order stock remains50 | `scanKnowledgePhysicalKindsAndRules` | **PROVEN PASS** |
| Other phenomenon: `NOT_A_NODE` callback | No implementation/reward; `completeScan` only handles `NODE…` | Callback rejected, no session/reward/rune packet | `scanKnowledgePhysicalKindsAndRules` | **PROVEN PASS** negative control, not an invented successful scan |

Additional duplicate boundary: the original `@` admission recursively checks
`#` first (`ScanManager:282–284`), but native `wasScanned` only checks the exact
prefix. `scanKnowledgeAdvancedMarkersSuppressThaumometer` drives actual RMB
against real diamond/block/entity/node targets with explicitly seeded advanced
knowledge. This does not claim native advanced-scan acquisition was driven.
Baseline real RMB failed the no-session/no-packet duplicate assertion. The
new shared check recognizes the # marker for admission, reward preflight and
readout visibility; **PROVEN RED→GREEN**. Final held-tool checks pass for item,
block, entity and node targets. The shared readout predicate is checked too;
client icon rendering is not.

There are six rows because the item kind has a separate boundary case; **five
kinds**, not six, are counted.

## Original citations and corrections to the hypothesis

* `ScanManager.java:351–417`: target-specific aspect resolution at completion,
  knowledge bucket registration, positive awarded aspects passed to clue creation.
* `ThaumcraftCraftingManager.java:215–348`: base lookup/dynamic tags end with
  `capAspects(...,64)`; `:362–481`: contained positive essentia + base + equipment/
  enchantment bonuses end with **only** `ThaumcraftApiHelper.cullTags`.
* `ThaumcraftApiHelper.java:40–80`: copies non-null tags and removes lowest
  weighted entries until at most six; it does **not** clamp amounts to 64.
  This is the relevant original item-list washing, not primal decomposition.
* `ScanManager.java:277–315,443–464`: duplicate admission and all-aspect parent
  preflight. `:420–440`: discovery +2, diminishing-return rules, discovery and RP
  notifications. `:394–397`: only NODE phenomena can complete.
* `ScanManager.java:235–274`: node type MAX/ADD bonuses; **node modifier is not
  consulted**. Bright/dim/fading therefore must not manufacture bonus aspects.
* `ScanManager.completeScan`, `ResearchManager.java:530–608`: ordinary scanning
  does **not** award warp. A clue is not research completion. Warp from completing
  warped research (`ResearchManager:512–523`) is a different operation.

## Rule matrix (12 bounded rules)

| Rule | New runtime assertion | Status |
|---|---|---|
| Exact washed aspects and RP | Exact maps, no extra nonzero pools, all physical kinds | **PROVEN PASS** |
| Twenty-tick admission | No scanned entry before elapsed 20 world ticks | **PROVEN PASS** |
| Resolve values at completion | Same physical jar changes contained air 32→64 at tick 10; final reward must be 68 | **PROVEN PASS** |
| Duplicate / runes / side-effect replay | Second actual RMB leaves NBT unchanged, no session and no Thaumcraft outbound packet; existing # marker must also block @ scan | **PROVEN PASS**, # mismatch fixed |
| Per-player ownership | Independent ServerPlayer holder has no scanned diamond | **PROVEN PASS** |
| Persistence | Actual ServerPlayer additional-save/read round-trip preserves complete knowledge NBT | **PROVEN PASS**; not socket relogin |
| Research-table consumer | Real two-piece table menu/receiver consumes scan-earned terra, plus one declared aqua input, to make victus | **PROVEN PASS** |
| Atomic missing-parent failure | Diamond with only primal discoveries changes no knowledge or RP/discovery packets | **PROVEN PASS** |
| First discovery and +2 | Retry with greed parents gives exactly 6 vitreus/6 lucrum and two discovery/two RP packets | **PROVEN PASS** server emission |
| Research popup once | Actual Enderman scan gives @MIRROR and one research notice; duplicate gives none | **PROVEN PASS** server emission |
| Warp side effects | Every accepted fixture scan leaves total warp zero | **PROVEN PASS** |
| Node type/modifier semantics | Six held scans; exact maps and no vis consumption; all modifiers plus null checked on real readout | **PROVEN PASS**, not a held scan for every modifier combination |

## PROVEN findings

### 1. Finished item bonus capped a second time

**PROVEN RED:** the first dedicated-server GameTest failed with
`exact scan RP expected {aer=68} actual {aer=64}` after the real held scan.
Log `/tmp/opencode/g25scan-red.log`; original XML receipt copied to
`/tmp/opencode/g25scan-red-container.xml` before another run could overwrite it.
The actual target remained a real full jar, with controlled addon base air4.
This is not an assertion against a fake source-text implementation.

Fix: `ObjectAspectResolver.get` now ends with `cull(result)`, preserving the
original base cap in `baseTags` and the original six-aspect culling, but no longer
erasing positive contained/bonus amounts after addition. Shared scanning and
dissolution use this same resolver. **GREEN:** same held-scan regression passes
with exact68, target retains64 contained essentia, readout matches68.
Commit: `8d7f779a`.

### 2. Advanced scan marker did not suppress thaumometer scanning

**PROVEN RED:** with a declared pre-existing `#minecraft:diamond` marker, the
real survival RMB started a new session and violated
`duplicate real RMB emits no rune/reward/discovery/research packet and changes no knowledge`.
Log `/tmp/opencode/g25scan-red-advanced.log`; receipt
`/tmp/opencode/g25scan-red-advanced.xml`.

Fix: `ScanManager.wasScanned` checks both @ and # for an @ request, preserving
the original one-way rule (an @ record alone does not block # admission).
`hasBeenScanned` provides the original either-prefix readout predicate, now
shared with `ItemThaumometerRenderer`. The GameTest checks that shared predicate
and direct award-boundary refusal as well as real RMB. Actual client rendering
remains uninspected.
**GREEN:** real RMB and direct award-boundary checks pass for every stored
bucket; shared readout predicate passes. Commit: `a0c8ff31`.

The second run passed `scanKnowledgeMissingParentsAndDiscovery`. Its physical
kind test initially failed on an erroneous **test fixture expectation** of
stone terra1; original `ConfigAspects:114` explicitly says terra2, matching native.
Corrected the test and its research-table remaining-pool expectation; **not** a
third gameplay mismatch and no stone production change was made.

## Final dedicated-server receipt

Fresh-world invocation (Java21, private `.gradle-g25`):

```sh
rm -rf fabric/run-gametest/world
./gradlew :fabric:runGameTest \
  -PgameTestFilter=scanKnowledgePreservesContainerBonusAboveBaseCap,scanKnowledgeAdvancedMarkersSuppressThaumometer,scanKnowledgePhysicalKindsAndRules,scanKnowledgeMissingParentsAndDiscovery \
  -PgameTestHeap=1G --no-daemon --max-workers=2
```

`/tmp/opencode/g25scan-green.log`: **BUILD SUCCESSFUL**.
`/tmp/opencode/g25scan-green.xml`: four testcases, zero failures;
receipt timestamp `2026-10-23T00:50:53.581574792Z`.
The original report is `fabric/build/gametest-focused-results.xml`.

Final build/regression gate: **48 JUnit tests / 9 suites PASS**, zero failures,
errors or skips; `:fabric:build` **PASS**, including access-widener validation and
test/production archive isolation checks. Log
`/tmp/opencode/g25scan-build-unit.log`; JUnit receipts under
`common/build/test-results/test/`. Fabric tests are invoked explicitly above;
the packaging invocation skips only the empty Fabric JUnit source set. No
NeoForge build/test task or client runtime was run.

## Reasoned-only / not established by this lane

* Real socket relogin, client discovery-toast animation, rune rendering and research
  popup rendering are not exercised by this server harness.
* Full object/entity catalog parity, addon-specific arbitrary scan handlers, all
  equipment/enchantment combinations, and every diminishing-return boundary are
  outside these declared witnesses.
* Acquisition through a native advanced scanner was not driven: advanced-marker
  tests explicitly seed the valid persisted knowledge shape. Readout's shared
  visibility predicate is tested, not its actual client draw.
* Other phenomenon rejection is faithful to original `completeScan`, not evidence
  that every addon-defined phenomenon can or should be scanned successfully.
