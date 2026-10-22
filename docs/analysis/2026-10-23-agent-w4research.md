# Wave 4 — the `w4research` agent — the last `partial` units of the research family

Worktree: `/home/cyberfox-kali/wt/w4research` (branch `agent/w4research`).
Unit list: `/home/cyberfox-kali/units/w4research.txt` (6 rows).

The oracle is `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (byte exact, 859/859
fingerprints). `/home/cyberfox-kali/OpenTC4` was not used. The official Mojang 1.7.10
`client.jar` (`/tmp/opencode/mc1710/client.jar`, SHA1 `e80d9b3bf5085002218d4be59e668bac718abbc6`)
and the MCP mapping (`/tmp/opencode/mcp_stable-12-1.7.10.zip`) were used for the two
vanilla questions of this wave (the creative ink and the creative delivery of the note).

**No Gradle was run.** The proof is a direct `javac` of the whole `common` main and test
source sets and of the `common` game test source set against the remapped 1.21.1 jar of
the Loom cache (see §2). **No unit test and no game test was executed** — every test
below is *authored but NOT executed*.

---

## 1. The result

| unit | before | after |
|---|---|---|
| `thaumcraft/client/gui/GuiResearchBrowser.java` | partial | **parity_verified** |
| `thaumcraft/client/gui/GuiResearchRecipe.java` | partial | partial (two of the eight array pages) |
| `thaumcraft/client/gui/GuiResearchTable.java` | partial | **parity_verified** |
| `thaumcraft/client/renderers/item/ItemThaumometerRenderer.java` | partial | partial (the scan readout) |
| `thaumcraft/common/items/relics/ItemThaumometer.java` | partial | **parity_verified** |
| `thaumcraft/common/lib/network/playerdata/PacketScannedToServer.java` | partial | **parity_verified** |
| `thaumcraft/client/integration/jei/ResearchVisibility.java` (family row) | missing | **parity_verified / exclude** |
| `thaumcraft/client/integration/jei/ResearchVisibilityTicker.java` (family row) | missing | **parity_verified / exclude** |

`python3 scripts/porting/update_scope_ledger.py --check --original-root
/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`;
`counts` untouched; the diff of `docs/porting/scope-ledger.json` touches only these eight
rows.
`python3 scripts/porting/extract_tc4_definitions.py --oracle
/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle --check` →
`{"aspects": 48, "research_count": 201, "text_page_count": 294, ...}` with the pinned
hashes.

---

## 2. The compile proof

No Gradle, no daemon. `javac -nowarn -proc:none` against
`/home/cyberfox-kali/Thaumcraft-1.21.1/.gradle/loom-cache/.../minecraft-merged-*.jar`
(the access-widened jar of the leader's cache) plus the remapped architectury and fabric
API jars and the `modules-2` dependencies:

| source set | result |
|---|---|
| `common/src/main/java` (1372 files) | compiles |
| `common/src/test/java` | compiles |
| `common/src/gameTest/java` | compiles |

The script is `/tmp/opencode/check_w4research.sh` (main + test) and
`/tmp/opencode/check_gt_w4research.sh` (game test).

---

## 3. The units

### 3.1 `thaumcraft/common/items/relics/ItemThaumometer.java` — the rune particles of the scan — CLOSED

**Original.** The scan of the thaumometer draws a rune glyph over its target on **every
tick** of the use:

* oracle `thaumcraft/common/items/relics/ItemThaumometer.java:92` — the pointed entity:
  `blockRunes(world, e.x - 0.5, e.y + e.eyeHeight / 2, e.z - 0.5, 0.3 + rand * 0.7, 0.0,
  0.3 + rand * 0.7, (int)(e.height * 15), 0.03)`;
* oracle `:103` — a node: `blockRunes(world, x, y + 0.25, z, ..., 15, 0.03)`;
* oracle `:128` — a block: the same;
* oracle `thaumcraft/client/ClientProxy.java:918-922` — the client half creates exactly
  **one** `FXBlockRunes` at `(x + 0.5, y + 0.5, z + 0.5)`;
* the colours are drawn from the **client** world random (the whole scan runs on the
  client in the original), so the server world random that generates the research
  puzzles is never shifted by a scan.

**Port before.** No rune at all: the scan had no client feedback but the camera tick.

**Fix.**

* new `common/src/main/java/thaumcraft/common/lib/research/ScanRunes.java` — the pure
  rule of the original: the position, the duration, the gravity and the colour of the
  rune of a block, of a node and of an entity.
* new `common/src/main/java/thaumcraft/common/lib/network/ScanRuneFxPayload.java` — the
  S2C payload (the dimension, the point, the duration, the gravity).
* new `common/src/main/java/thaumcraft/common/lib/network/ScanRuneFxNetworking.java` —
  the sender (to the scanning player alone) and the receiver registration.
* new `common/src/main/java/thaumcraft/client/fx/ScanRuneClientEffects.java` — the client
  half: one `BlockRuneParticle` with the colour drawn from the **client** random.
* `common/src/main/java/thaumcraft/common/items/relics/ItemThaumometer.java` — the rune is
  sent on the initial use (the original `func_77659_a:144-148` calls `doScan`) and on
  every tick of the use (the original `onUsingTick:153` calls `doScan`).
* `common/src/main/java/thaumcraft/common/Thaumcraft.java` and
  `common/src/main/java/thaumcraft/client/ClientSetup.java` — the registration (one line
  each).

**A second defect fell out of the same oracle passage.** The original refuses a target
that was already scanned **before** the use begins (oracle
`thaumcraft/common/lib/research/ScanManager.java:277-315`, `isValidScanTarget`): the
instrument is raised, but there is no rune, no camera tick and no reward. The native port
started the session anyway, so a duplicate target still played the camera tick of twenty
ticks and was only refused at the end of the scan.

* **Fix** `ScanManager.alreadyScanned(player, signature, kind)` (the same gate, published
  from the same knowledge the completion uses) and the session of the scan now starts
  only for a valid target. A use without a session does nothing and the instrument stays
  raised, exactly as the original (`onUsingTick:152-168`).
* **Test changed** `common/src/gameTest/java/thaumcraft/common/testing/RuntimeChecks.java`
  (`thaumometerBlockPickSharesDroppedItemIdentity`): the dropped duplicate never starts a
  session now. The scene itself is unchanged.

**Forced change (documented).** The scan is resolved on the server, so the original's
C2S packet (`PacketScannedToServer`) has no counterpart: the server derives the target
itself. The rune therefore travels server → client as its own payload.

### 3.2 `thaumcraft/common/lib/network/playerdata/PacketScannedToServer.java` — CLOSED

The unit's remaining difference was exactly the rune particles. They are now reproduced
end to end (§3.1): the server sends the rune of the target, the client draws it. The
camera tick of the original was already reproduced by the previous wave.

The one documented deviation stays: the original played the sound on the scanning client
only and drew its pitch from the client world random; the native port plays the sound
from the server and draws the pitch from its own generator, so the server world random is
never shifted by a scan.

### 3.3 `thaumcraft/client/gui/GuiResearchBrowser.java` — the sibling connectors and the gradient line — CLOSED

**Original.** `oracle thaumcraft/client/gui/GuiResearchBrowser.java:299-348` draws two
kinds of connectors over the cells of the selected category:

* the **parent** connectors (`:303-326`): every research is joined with every parent of
  the same category that is not virtual;
* the **sibling** connectors (`:327-348`): every research is joined with every sibling of
  the same category that is not virtual and whose own parents do not name it (otherwise
  the parent connector would draw the same line twice).

The colour of a connector is the state of its two ends: flat dark grey `0.1, 0.1, 0.1`
(parent) or `0.1, 0.1, 0.2` (sibling) without the wobble when the research itself is
complete, green `0.0, 1.0, 0.0` when the other end is complete, blue `0.0, 0.0, 1.0`
otherwise. A connector is not drawn at all when one of its ends is lost, hidden without
its clue or concealed without its prerequisites.

The line itself is `oracle :732-787` (`drawLine`): a polyline of `dist / 2` segments,
**three pixels wide**, whose colour fades to black along the line, whose alpha grows
from zero to `0.6` and whose vertices wobble with the sine of the ticks of the player
(`sin((count + a) / 7) * 5 * (1 - phase)` and `sin((count + a) / 5) * 5 * (1 - phase)`).

**Port before.** Only the parent connectors, and they were a one pixel `hLine`/`vLine`
staircase in a colour of the port's own. The sibling connectors were missing.

**Fix.**

* new `common/src/main/java/thaumcraft/common/lib/research/ResearchBrowserConnectors.java`
  — the rule of the original in the common layer: the connectors, their colours, their
  gates and the geometry of the line (`segments`, `phase`, `fade`, `wobbleX`, `wobbleY`).
* `common/src/main/java/thaumcraft/common/lib/research/ResearchDefinition.java` — the
  `isConcealed()` flag of the original.
* `common/src/main/java/thaumcraft/common/lib/research/ResearchBrowserData.java` —
  `categoryEntries(category)`: the original walks **every** research of the category
  (oracle `:132-134`), so the line of a hidden research is drawn even when its cell is
  not.
* `common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java` — the connector
  line of the original (the vertices, the fade, the wobble; the 1.21.1 GUI cannot draw a
  three pixel wide diagonal line, so each segment is drawn as the staircase of the
  original polyline).

**A third defect of the same unit.** An icon expression that names nothing draws
**nothing** in the original (`oracle :404-412`); the native port fell back to the icon of
the thaumonomicon. The fallback is gone.

**Documented deviation.** The pane is the native 510x300 panel with the map at the 1.0
scale; the original draws a 256x230 pane with the map at the 0.5 scale (oracle `:75-76`,
`:285-291`). The map keeps the original 24 pixel grid, the original coordinates, the
original categories, the original frames of the cells (`badgeFrame` mirrors oracle
`:387-399`) and the original Special overlay and forbidden marker.

### 3.4 `thaumcraft/client/gui/GuiResearchTable.java` — the runes and the line — CLOSED

**Original.**

* `oracle thaumcraft/client/gui/GuiResearchTable.java:408-441` — the runes of the sheet:
  every 250 milliseconds one rune is put on a free hex of the sheet (a random hex
  around the centre, never a hex of the puzzle), the sprite is one of the sixteen cells
  of `textures/misc/script.png` and the rune fades in and out over fifteen to twenty
  five seconds (`progress < 0.25 → alpha = progress * 2`, `progress > 0.5 → alpha = 1 -
  progress`, drawn with `alpha * 0.66`).
* `oracle :517-540` (`drawRune`) — the rune is a black quad of ten pixels of the script
  sheet, turned by ninety degrees in the plane.
* `oracle :385-405` (`drawLine`) — the line of the connection is **three pixels wide**,
  the colour `0.0, 0.6, 0.8`, the **additive** blend `(770, 1)` and an alpha that pulses
  between `0.3` and `0.9` with the ticks of the player.
* `oracle :411` — the sheet is the **single** parchment `textures/misc/parchment3.png`.

**Port before.** The line was a rotated `fill` with a constant alpha `0x99` and the
ordinary blend; there were no runes at all.

**Fix** `common/src/main/java/thaumcraft/client/gui/GuiResearchTable.java`: the runes of
the sheet (the 250 ms check, the random hex, the sixteen sprites, the fade) and the line
with the additive blend and the pulsing alpha of the original.

**A wrong note of the ledger is corrected.** The old note spoke of "the six parchment
sheet looks". The original has **one** parchment texture (`:411`) and the native port
already draws that one; there was never a difference here.

**Documented deviation.** The server-authoritative edit/combine/copy requests add a
revision/session handshake that the original does not have (a deliberate native
authority decision of an earlier wave).

### 3.5 `thaumcraft/client/gui/GuiResearchRecipe.java` — the array pages — PARTIAL

**Original.** Eight research pages hold an **array** of recipes instead of one recipe,
and the book shows one of them per second (oracle
`thaumcraft/client/gui/GuiResearchRecipe.java:647-655`):

| page | array | oracle |
|---|---|---|
| BANNERS | the sixteen banner recipes | `ConfigResearch.java:193-199` |
| PRIMALARROW | the six primal arrow recipes | `ConfigResearch.java:221-226` |
| ORE | the seven crystal cluster recipes | `ConfigResearch.java:366-374` |
| CRUCIBLE | the six balanced shard recipes | `ConfigResearch.java:250-254` |
| SCEPTRE | the three sceptre recipes | `ConfigResearch.java:110-144` |
| JARLABEL | the sixteen jar label recipes | `ConfigResearch.java:302-305` |
| RUNICAUGMENTATION | the runic augmentation recipe | `ConfigResearch.java:200-208` |
| CRUCIBLE | the shard item | `ConfigResearch.java:254` |

**Fix.** Four of the eight are rendered again:

* `common/src/main/java/thaumcraft/client/gui/ThaumcraftRecipeDisplay.java` — the
  BANNERS (sixteen `arcane/banner_*`), the PRIMALARROW (six `arcane/primal_arrow_*`) and
  the ORE (seven `clusters/*`) arrays are resolved into the alternatives of the page and
  cycle once per second, exactly as the original. The CRUCIBLE smelting page (the six
  `crucible/balanced_shard_*`) was already resolved by the previous wave.
* `common/src/main/java/thaumcraft/client/gui/GuiResearchRecipe.java` — the page resolver
  now knows the key of its research. The original builds **two different arrays with the
  same Java expression** (`rc.toArray(new IRecipe[0])`, the clusters of the ORE page and
  the jar labels of the JARLABEL page), so the key is the only way to tell them apart.

**The blocker of the two remaining pages.** The data is there, the renderer is not:

* the SCEPTRE page needs a diagram of its own: the three variants of the original differ
  only in the materials of the wand (iron cap + wood rod, gold cap + greatwood rod,
  thaumium cap + silverwood rod) and in their vis cost. The native recipe
  `thaumcraft:arcane_sceptre` is a *special* recipe (`ArcaneSceptreRecipe.isSpecial()`)
  whose `getIngredients()` is empty — it reads the grid of the workbench when it is
  crafted — and the book renderer draws the ingredients of the recipe, so the three
  variants would come out empty. The fixtures exist
  (`ArcaneSceptreRecipe.displayInput(rod, cap)`), a dedicated diagram does not.
* the JARLABEL page needs sixteen synthetic crafting inputs (a blank label + a phial of
  one aspect each) for the same reason: the native recipe `thaumcraft:jar_label_bind`
  accepts any filled phial, so the book would show the same page sixteen times.

### 3.6 `thaumcraft/client/renderers/item/ItemThaumometerRenderer.java` — PARTIAL

**What the port really does.** The old note of the ledger ("the port renders the
thaumometer as a normal item model") is **stale**: the native renderer
(`common/src/main/java/thaumcraft/client/renderers/item/ItemThaumometerRenderer.java`)
draws the three dimensional scanner of the original (the scanner OBJ and the glowing scan
screen) **and** the first person view model with the mirrored arm of the player, through
the 1.21.1 builtin item hook.

**Why the unit stays `partial`.** Two parts of the original are still missing:

* the **scan readout** (`oracle :155-259`): the scanned stack, the aspect icons (only
  for a target that was already scanned) and the name of the target, drawn in the world
  space of the item;
* the two rotations that follow the swing of the arm of the player (`oracle :103-104`):
  the original uses the arm angles of 1.7.10 (`renderArmPitch` and `renderArmYaw`, the
  fields `field_71155_g` and `field_71154_f` of the MCP mapping), which have no public
  equivalent in 1.21.1.

The contract itself is a deliberate exclude: 1.21.1 removed `IItemRenderer` and
`IModelCustom`, so the published contract of the original has no target API (the same
reason as the sibling row `ThaumometerPerspectiveModel`, which is closed as
`parity_verified` + `exclude`).

### 3.7 The two JEI rows of the family — CLOSED as a deliberate `exclude`

`thaumcraft/client/integration/jei/ResearchVisibility.java` and
`thaumcraft/client/integration/jei/ResearchVisibilityTicker.java` are **legacy-only**
rows: the private oracle has **no** `thaumcraft/client/integration/jei` directory at all,
so the original 4.2.3.5 never published them. They exist only in the retained 1.12
reference tree, they drive the 1.12 JEI plugin, and the 1.21.1 port ships no JEI/EMI
dependency. Both rows are closed as a deliberate `exclude` with that evidence, exactly
like the sibling row `thaumcraft/client/integration/jei/ThaumcraftJeiPlugin.java`, which
the ledger already carries as `parity_verified` + `exclude`. The research visibility
rules they enforced are still honoured in the common layer
(`ResearchBrowserData.isVisible`, `ResearchPurchase`, `ResearchNoteIssuance`) and the
knowledge refresh they polled for is now an explicit server snapshot
(`KnowledgeEvents.sync`).

---

## 4. The research note issuance overflow — the rule proved from the oracle

The scenario named by the brief is
`common/src/gameTest/java/thaumcraft/common/testing/ResearchNoteIssuanceRuntimeChecks.java:152`
(`fullInventoryReusesFreedPaperSlotOrDrops`). The brief describes it as "a creative
overflow spends one paper **and the ink**". **That description is not the original.**
The audited behaviour is:

* the original spends the paper with `InventoryPlayer.consumeInventoryItem(Items.paper)`
  (official 1.7.10 `yx.a(adb)`, offsets 12-38: the first stack of paper is reduced by one
  and the slot is cleared when it empties). There is **no** creative branch, so the
  paper is spent for a creative player as well;
* the original spends the ink with `ItemStack.damageItem(1, player)` (official 1.7.10
  `add.a(int, sv)`, offsets 0-20: `if (entity instanceof EntityPlayer && ((EntityPlayer)
  entity).capabilities.isCreativeMode) return;`). A creative player returns **before**
  the damageability, the enchantments and the RNG: **no** ink point is spent;
* the original delivers the note with `InventoryPlayer.addItemStackToInventory(note)`
  (official 1.7.10 `yx.a(add)`, offsets 56-83): if the note cannot be added and the
  player is creative, the stack is cleared and `true` is returned, so TC4 never drops it.

The order of the original matters: the paper is spent **before** the note is created
(oracle `thaumcraft/common/lib/research/ResearchManager.java:118-135`), so the slot of
the spent paper is free and receives the note — in survival **and** in creative.

The production code of the port already implemented exactly that
(`ResearchNoteIssuance.issue`, `ScribingInk.planPlayer`) and the scenario already
asserted it. What was missing was the branch in which a **single** paper frees its slot
in creative: the note then lands in that slot and the creative discard branch of
`addItemStackToInventory` is never reached.

* **Fix (test only)** the scenario now covers that branch: with one paper and a full
  inventory, a creative player spends the paper, the freed slot receives the note, the
  ink stays untouched and nothing is dropped.
* **No production change was needed** — the production code and the scenario now agree,
  and both agree with the official bytecode of the 1.7.10 `client.jar`.
* The stale description of the scenario in the older reports
  (`docs/analysis/2026-10-23-agent-research.md:243`) is superseded by the audited
  documents `docs/analysis/2026-10-15-scribing-ink-semantics.md:19,50` and
  `docs/analysis/2026-10-23-agent-testfix4.md:325`.

---

## 5. What is still open, with the precise blocker

| unit | the exact remaining difference | the blocker |
|---|---|---|
| `GuiResearchRecipe` | the SCEPTRE page and the JARLABEL page of the book | a dedicated diagram per page: the native arcane sceptre recipe is a *special* recipe with no ingredients and the native jar label recipe accepts any phial, so the book needs synthetic inputs (the fixtures exist, the diagram does not) |
| `ItemThaumometerRenderer` | the scan readout of the first person view and the two arm-swing rotations | the readout has to draw a stack, aspect icons and a text in the world space of an item renderer; the arm angles of 1.7.10 (`renderArmPitch`, `renderArmYaw`) have no public 1.21.1 equivalent |

---

## 6. The client-side change the leader has to route

The original also draws the **rune particles over the hovered container slot** — the
aspect row of a container slot (`oracle thaumcraft/client/lib/ClientTickEventsFML.java:660-729`,
`renderAspectsInGui`). That row belongs to the parallel client wave
(`thaumcraft/client/lib/ClientTickEventsFML.java` is one of its eleven units), so it was
**not** touched here. The exact change, when the client lane picks it up:

* the container slot that is hovered is known on the client only through the menu of the
  screen; the original draws the runes of the aspects of the hovered stack with the same
  `BlockRuneParticle` of the port (`thaumcraft/client/fx/particles/BlockRuneParticle.java`),
  one particle per aspect of the stack, with the colour rule of the original
  (`0.3 + rand * 0.7`, `0.0`, `0.3 + rand * 0.7`) taken from the **client** random;
* the native `RuneEffects` (the warding stone and the runed eldritch block) is the model
  of that call.

The rune particles of the **scan** itself are **not** part of that request: they are
implemented in this wave, server and client, and the client half is
`thaumcraft/client/fx/ScanRuneClientEffects.java`.

---

## 7. The acceptance criteria for the leader's client run

1. **The scan of the thaumometer** — take the thaumometer, look at a block and hold the
   right mouse button. A **rune glyph** must appear over the block on **every tick** of
   the scan (about twenty of them), the colour between dark red and magenta
   (`0.3 + rand * 0.7` red, no green, `0.3 + rand * 0.7` blue), and the rune must fade
   out over its life (the lifetime is three times fifteen ticks, the gravity `0.03`).
   The same must happen over a pointed entity (the rune then sits on the middle of its
   body and lives fifteen times its height in ticks) and over a node.
2. **A duplicate target** — scan the same block twice. The second scan must raise the
   instrument but must show **no** rune, **no** camera tick and **no** reward.
3. **The research browser** — open the book. Between the cells of a category the
   **parent and the sibling** connectors must be drawn as a three pixel wide line that
   fades from the colour at its start to black at its end and wobbles slowly. A research
   that is complete must be joined with a flat dark grey line, a research whose parent is
   complete with a green line, an unknown parent with a blue line.
4. **The research table** — put a note on the table. Every quarter of a second a small
   black rune must appear on the empty part of the parchment and fade in and out over
   fifteen to twenty five seconds. The lines between the connected cells must be three
   pixels wide, cyan (`0.0, 0.6, 0.8`) and **additive** (they brighten what is behind
   them), and their brightness must pulse slowly.
5. **The research book** — open the BANNERS, the PRIMALARROW, the ORE and the CRUCIBLE
   research. Their recipe page must now show a recipe and **change it every second**
   (the original cycles through the array). The SCEPTRE and the JARLABEL pages still
   show no recipe — that is the known open item of §5.

---

## 8. The files that changed

* `common/src/main/java/thaumcraft/common/lib/research/ScanRunes.java` — **new**.
* `common/src/main/java/thaumcraft/common/lib/research/ResearchBrowserConnectors.java` — **new**.
* `common/src/main/java/thaumcraft/common/lib/network/ScanRuneFxPayload.java` — **new**.
* `common/src/main/java/thaumcraft/common/lib/network/ScanRuneFxNetworking.java` — **new**.
* `common/src/main/java/thaumcraft/client/fx/ScanRuneClientEffects.java` — **new**.
* `common/src/main/java/thaumcraft/common/items/relics/ItemThaumometer.java` — the rune
  of the scan, the gate of a valid target.
* `common/src/main/java/thaumcraft/common/lib/research/ScanManager.java` —
  `alreadyScanned`.
* `common/src/main/java/thaumcraft/common/lib/research/ResearchDefinition.java` —
  `isConcealed()`.
* `common/src/main/java/thaumcraft/common/lib/research/ResearchBrowserData.java` —
  `categoryEntries`.
* `common/src/main/java/thaumcraft/client/gui/GuiResearchBrowser.java` — the connector
  line of the original, the icon fallback.
* `common/src/main/java/thaumcraft/client/gui/GuiResearchTable.java` — the runes of the
  sheet, the line of the original.
* `common/src/main/java/thaumcraft/client/gui/ThaumcraftRecipeDisplay.java` — the array
  pages.
* `common/src/main/java/thaumcraft/client/gui/GuiResearchRecipe.java` — the key of the
  research in the page resolver.
* `common/src/main/java/thaumcraft/common/Thaumcraft.java` — the S2C rune registration.
* `common/src/main/java/thaumcraft/client/ClientSetup.java` — the S2C rune receiver.
* `common/src/test/java/thaumcraft/research/ScanRuneParityTest.java` — **new**.
* `common/src/test/java/thaumcraft/research/ResearchBrowserConnectorTest.java` — **new**.
* `common/src/gameTest/java/thaumcraft/common/testing/ResearchNoteIssuanceRuntimeChecks.java`
  — the creative overflow with a freed paper slot.
* `common/src/gameTest/java/thaumcraft/common/testing/RuntimeChecks.java` — the dropped
  duplicate never starts a session.
* `scripts/porting/apply_w4research_verdicts.py` — **new**, the authored ledger update.
* `docs/porting/scope-ledger.json` — the verdicts and the evidence of the eight rows.
* `docs/analysis/2026-10-23-agent-w4research.md` — this report.

---

## 9. The tests (authored, NOT executed)

| file | what it protects |
|---|---|
| `common/src/test/java/thaumcraft/research/ScanRuneParityTest.java` (new, 6 tests) | the position, the duration and the gravity of the rune of a block, of a node and of an entity, the particle offset of the original `ClientProxy.blockRunes`, and the colour rule of the original |
| `common/src/test/java/thaumcraft/research/ResearchBrowserConnectorTest.java` (new, 12 tests) | the colour and the wobble of the parent and the sibling connectors, the hidden/lost/concealed gates, the width, the vertex alpha, the number of the segments and the fade of the line |
| `common/src/gameTest/java/thaumcraft/common/testing/ResearchNoteIssuanceRuntimeChecks.java` (changed) | the creative overflow with a freed paper slot |
| `common/src/gameTest/java/thaumcraft/common/testing/RuntimeChecks.java` (changed) | the dropped duplicate never starts a session |

No test constructs a `Block`: every rule under test is a pure static function
(`ScanRunes.block`, `ScanRunes.entity`, `ScanRunes.colour`,
`ResearchBrowserConnectors.connectors`, `ResearchBrowserConnectors.parentConnector`,
`ResearchBrowserConnectors.siblingConnector`, `ResearchBrowserConnectors.segments`).

---

## 10. What is verified here and what is not

**Verified here (no Gradle needed):**

* the `javac` compile of the `common` main, test and game test source sets (§2);
* `python3 scripts/porting/update_scope_ledger.py --check --original-root
  /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`,
  `counts` untouched, only the eight rows of this lane changed;
* `python3 scripts/porting/extract_tc4_definitions.py --oracle
  /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle --check` → 201 research entries,
  294 text pages and the pinned hashes;
* the vanilla questions of §4 read out of the official 1.7.10 `client.jar`
  (`add.a(int, sv)` offsets 0-20, `yx.a(adb)` offsets 12-38, `yx.a(add)` offsets 56-83)
  and out of the MCP mapping (`fields.csv`: `field_70131_O` = height,
  `field_71155_g` = renderArmPitch, `field_71154_f` = renderArmYaw).

**NOT verified here (the leader must run it):**

* `:common:compileJava`, `:common:compileTestJava`, `:common:compileGameTestJava` — **not
  run** (the `javac` proof of §2 is the substitute);
* `:common:test` — **not run**; the two new test files are authored only;
* `:fabric:runGameTest` — **not run**; the two changed scenarios are authored only;
* the client frames — **not seen**; the acceptance criteria of §7 are written for the
  leader.
