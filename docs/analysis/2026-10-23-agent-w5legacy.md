# Wave 5 — the `w5legacy` agent — the last eleven units that were neither `parity_verified` nor `partial`

Worktree: `/home/cyberfox-kali/wt/w5legacy` (branch `agent/w5legacy`).
Unit list: the eleven rows of the brief — eight `missing`, three `not_assessed`.

The oracle is `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (859 files, byte exact,
the authority). `/home/cyberfox-kali/OpenTC4` was **not** used. **No Gradle was run** and
**no Java source was changed** — this is a ledger and documentation wave; the proof is the
oracle grep plus the `update_scope_ledger.py --check`.

---

## 0. The result

| unit | before | after | why |
|---|---|---|---|
| `thaumcraft/client/fx/beams/WandEffectOrigin.java` | missing | **exclude / parity_verified** | the original has no wand tip origin at all |
| `thaumcraft/client/integration/jei/JeiRecipeData.java` | missing | **exclude / parity_verified** | TC4 4.2.3.5 has no JEI package |
| `thaumcraft/client/integration/jei/ThaumcraftRecipeCategory.java` | missing | **exclude / parity_verified** | TC4 4.2.3.5 has no JEI package |
| `thaumcraft/client/integration/jei/ThaumcraftRecipeWrapper.java` | missing | **exclude / parity_verified** | TC4 4.2.3.5 has no JEI package |
| `thaumcraft/client/renderers/item/FirstPersonWandTipOrigin.java` | missing | **exclude / parity_verified** | the original never reads the GL matrices back |
| `thaumcraft/client/renderers/item/WandPoseMath.java` | missing | **exclude / parity_verified** | the original's pose is inline in `ItemWandRenderer` |
| `thaumcraft/client/renderers/item/WandUsePose.java` | missing | **exclude / parity_verified** | the original's pose is inline in `ItemWandRenderer` |
| `thaumcraft/client/renderers/item/WandUsePoseSampler.java` | missing | **exclude / parity_verified** | the original's pose is inline in `ItemWandRenderer` |
| `thaumcraft/client/lib/EldritchDiagnostics.java` | not_assessed | **exclude / parity_verified** | a 1.12 FPS instrument, dead even in the 1.12 tree |
| `thaumcraft/client/renderers/item/ItemEldritchRenderer.java` | not_assessed | **exclude / parity_verified** | the original has no item stack renderer at all |
| `thaumcraft/common/integration/waila/GolemWailaPlugin.java` | not_assessed | **exclude / parity_verified** | WAILA did not exist for 1.7.10 |

`python3 scripts/porting/update_scope_ledger.py --check --original-root
/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`;
the `counts` block is **byte-identical** to the one before this wave (the `disposition` of
all eleven rows was already `exclude`, and `counts` does not carry `native_coverage`);
the diff of `docs/porting/scope-ledger.json` touches **exactly the eleven rows** and
nothing else (checked row by row against `git show HEAD:docs/porting/scope-ledger.json`).

**The ledger is now free of `missing` and `not_assessed`:**

```
before:  parity_verified 1006 | partial 27 | missing 8 | not_assessed 3
after:   parity_verified 1017 | partial 27 | missing 0 | not_assessed 0
dispositions after:  adapt 257/6 | exclude 161/2 | port 290/1 | replace 309/18   (verified / partial)
```

No unit was closed as `parity_verified` on the strength of a guess: each of the eleven
carries a **complete callsite audit** (the ledger requires one for an exclusion) and the
oracle search that proves the original never had the class.

---

## 1. The searches that were run

All of them against `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` (859 `.java`
files).

```sh
# 1. the class names themselves
for n in WandEffectOrigin JeiRecipeData ThaumcraftRecipeCategory ThaumcraftRecipeWrapper \
         FirstPersonWandTipOrigin WandPoseMath WandUsePose WandUsePoseSampler \
         EldritchDiagnostics ItemEldritchRenderer GolemWailaPlugin; do
  grep -rl --include='*.java' -F "$n" /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle
done
# -> 0 files for every one of the eleven names

# 2. the directories
ls /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/thaumcraft/client/fx/beams/
ls /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/thaumcraft/client/renderers/item/
find /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle -type d -name 'integration*'
find /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle -iname '*waila*'
find /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle -iname '*jei*'

# 3. the distinctive members
grep -rn --include='*.java' -E 'TipCalibration|debugFrame' <oracle>
grep -rn --include='*.java' -E 'gluUnProject|GL_MODELVIEW_MATRIX|GL_PROJECTION_MATRIX|GL_VIEWPORT|glGetFloatv|glGetIntegerv' <oracle>
grep -rn --include='*.java' -E 'transformPoint|contextRotateX|contextRotateZ|startupRotateX|waveRotateX|waveRotateZ' <oracle>
grep -rn --include='*.java' -E 'mezz\.jei|IRecipeCategory|IRecipeWrapper|IRecipeLayout|IGuiItemStackGroup' <oracle>
grep -rn --include='*.java' -iE 'waila|huduma|journeymap' <oracle>
grep -rn --include='*.java' -E 'TileEntityItemStackRenderer|renderByItem|setTileEntityItemStackRenderer' <oracle>
grep -rn --include='*.java' -E 'EldritchPerf|nothingTESRCalls|obeliskTESRCalls|fieldHelperFaces' <oracle>
```

The results are quoted per unit below.

---

## 2. The eleven units

### 2.1 `thaumcraft/client/fx/beams/WandEffectOrigin.java` — CLOSED

**Search.**

* `grep -rl 'WandEffectOrigin' <oracle>` → **0 files**.
* `ls <oracle>/thaumcraft/client/fx/beams/` → **exactly six files**: `FXArc.java`,
  `FXBeam.java`, `FXBeamBore.java`, `FXBeamGolemBoss.java`, `FXBeamPower.java`,
  `FXBeamWand.java`. **No `WandEffectOrigin`.**
* `grep -rn -E 'TipCalibration|debugFrame' <oracle>` → **0 hits** (the 1.12 helper's
  per staff/sceptre/wand calibration table and its debug frame exist nowhere in the
  original).
* `grep -rn -E 'WandEffectOrigin' <oracle>` also covers the *members*: the original has
  no class that resolves a wand tip at all.

**Verdict.** The class is **not** an original TC4 4.2.3.5 class. It is a 1.12 helper.

**The stale gap note — corrected.** The old note said:

> *"REMAINING GAP (client lane): the native beam of a focus starts at the player, while the
> original drew it from the wand tip."*

**That is false.** The original starts its beam **at the player**:

* oracle `thaumcraft/client/fx/beams/FXBeamWand.java:37` — `private double offset = 0.0;`
* oracle `:58-62` — the offset is `0` for the local player and `player.height/2 + 0.25`
  for a remote one; it is never a wand tip.
* oracle `:209-228` — the origin is `lastTickPos/pos` of the player, moved by the yaw
  (`-cos(yaw)*0.066`, `-sin(yaw)*0.04`, `-0.06` on y) and then by the **look vector times
  0.3** (`vec3d.xCoord * 0.3` etc.).

The native already used exactly that origin:
`common/src/main/java/thaumcraft/common/items/wands/foci/ItemFocusExcavation.java:162-163`
(`eye.add(-cos(yaw)*.066, -.06, -sin(yaw)*.04).add(look.scale(.3))`).

The `FXBeamWand` row is `parity_verified` since the **w4wands** wave, which reproduced the
fan of the original (`ThaumcraftExcavationClient`), and its evidence now carries the same
proof. **There is no wand tip origin left to port.**

**Ledger change.** `native_coverage: missing → parity_verified`, `disposition` stays
`exclude`; the stale note is replaced by the proof; the callsite audit is recorded
(the 1.12 callers are `FXBeamWand.java:115,119-120,180`, `ClientProxy.java:1926-1927`,
`TileNodeRenderer.java:23` — all 1.12-only; `grep` over `common/src`, `fabric/src` and
`neoforge/src` finds **no** reference to a wand tip).

**Remaining difference.** None. Nothing of the original is lost by the exclusion.

---

### 2.2 `thaumcraft/client/integration/jei/JeiRecipeData.java` — CLOSED

**Search.**

* `find <oracle> -type d -name 'integration*'` → **0 hits**. The private oracle has
  **no `thaumcraft/client/integration` directory at all**, and no `thaumcraft/common/integration`
  either.
* `grep -rl 'JeiRecipeData' <oracle>` → **0 files**.
* `grep -rn -E 'mezz\.jei|IRecipeCategory|IRecipeWrapper|IRecipeLayout|IGuiItemStackGroup' <oracle>`
  → **0 hits**. The **only** recipe-viewer line of the whole original is
  `oracle thaumcraft/client/ClientProxy.java:444` — `Loader.isModLoaded("NotEnoughItems")`,
  which only shifts the aspect icons.

**Verdict.** Not an original class: a 1.12 JEI integration. The 1.12 holder imports the
1.12-only `mezz.jei` API (`JeiRecipeData.java:1-15`).

**Ledger change.** `missing → parity_verified`, `disposition: exclude`. Evidence now
carries the oracle tree search, the `mezz.jei` search, the `ClientProxy:444` line, the
callsite audit (the only caller is the 1.12 plugin `ThaumcraftJeiPlugin.java:45,50,56`)
and the native coverage (`grep 'jei'` over `common/src`, `fabric/src`, `neoforge/src` →
nothing; `ResearchRecipeCoverage.java:41` maps **all 308 original recipe keys**;
`ThaumcraftRecipeDisplay.java` shows them in the research book).

**Remaining difference.** None of the original. The 1.21.1 port ships no JEI/EMI
dependency — the same verdict the w4research wave recorded for
`ThaumcraftJeiPlugin`, `ResearchVisibility` and `ResearchVisibilityTicker`.

### 2.3 `thaumcraft/client/integration/jei/ThaumcraftRecipeCategory.java` — CLOSED

Same search, same verdict: no `thaumcraft/client/integration` directory in the oracle, no
`IRecipeCategory` anywhere, no `ThaumcraftRecipeCategory`. The 1.12 class implements the
1.12-only `mezz.jei.api.recipe.IRecipeCategory`
(`ThaumcraftRecipeCategory.java:1-11`); its only caller is the 1.12 plugin
(`ThaumcraftJeiPlugin.java:33,35,37`).

**Ledger change.** `missing → parity_verified`, `disposition: exclude`.

**Remaining difference.** None of the original.

### 2.4 `thaumcraft/client/integration/jei/ThaumcraftRecipeWrapper.java` — CLOSED

Same search, same verdict: no `IRecipeWrapper` / `IGuiItemStackGroup` anywhere in the
oracle, no `ThaumcraftRecipeWrapper`. The 1.12 wrapper
(`ThaumcraftRecipeWrapper.java:1-20`) implements the 1.12-only `IRecipeWrapper` and draws
the aspect row of a recipe with `UtilsFX.drawTag`; its only callers are the 1.12 plugin
(`ThaumcraftJeiPlugin.java:26-27,33,35,37,49-57`) and the 1.12 category
(`ThaumcraftRecipeCategory.java:9`).

**Ledger change.** `missing → parity_verified`, `disposition: exclude`, plus the proof
that the aspect row the 1.12 wrapper drew is drawn by the native research book
(`ThaumcraftRecipeDisplay.java:587-607`) in the same place the original drew it
(`oracle thaumcraft/client/gui/GuiResearchRecipe.java:318-345`).

**Remaining difference.** None of the original.

---

### 2.5 `thaumcraft/client/renderers/item/FirstPersonWandTipOrigin.java` — CLOSED

**Search.**

* `grep -rl 'FirstPersonWandTipOrigin' <oracle>` → **0 files**.
* `grep -rn -E 'gluUnProject|GL_MODELVIEW_MATRIX|GL_PROJECTION_MATRIX|GL_VIEWPORT|glGetFloatv|glGetIntegerv' <oracle>`
  → **0 hits**. (The only `GLU` calls of the original are `gluBuild2DMipmaps` in
  `truetyper/TrueTypeFont.java:314` and `gluPerspective` in `GuiGolem.java:280`.)
  **The original never reads the GL matrices back, so it never recovers a world space
  wand tip from the drawn model** — which is the whole mechanism of the 1.12 helper
  (`FirstPersonWandTipOrigin.java:52-59`, `GLU.gluUnProject`).
* The first person wand of the original is drawn by transforms only:
  `oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:45-119` (the context
  transforms `:59-86`, the basis correction `:87`, the use pose `:88-113`, the first
  person case `:73-75` and `:94-99`).

**Verdict.** Not an original class.

**The stale gap note — corrected.** The old note said:

> *"Open gap: the native port draws no beam from the wand tip, so the origin helper has no
> counterpart."* / *"REMAINING GAP (client lane): the tip origin of the original beam is the
> business of the original FXBeamWand class."*

**The premise is false** — the original never drew its beam from the wand tip
(`FXBeamWand.java:37,58-62,209-228`, see §2.1). The `FXBeamWand` row is `parity_verified`
since the w4wands wave. The 1.12 readback of the GL matrices has no original contract to
lose.

**Ledger change.** `missing → parity_verified`, `disposition: exclude`; the stale note is
replaced by the proof; the callsite audit is recorded (the 1.12 callers are
`FXBeamWand.java:101`, `TileNodeRenderer.java:127`, `ItemWandRenderer.java:121`,
`RenderEventHandler.java:437`; the native has no `wandTip`/`tipOrigin` reference at all).

**Remaining difference.** None.

---

### 2.6 `thaumcraft/client/renderers/item/WandPoseMath.java` — CLOSED

**Search.**

* `grep -rl 'WandPoseMath' <oracle>` → **0 files**.
* `grep -rn -E 'transformPoint|contextRotateX|contextRotateZ|startupRotateX|waveRotateX|waveRotateZ' <oracle>`
  → **0 hits** — none of the six members of the 1.12 pose appears in the 4.2.3.5 tree.
* The use pose of the original is **inline** in the renderer:
  `oracle thaumcraft/client/renderers/item/ItemWandRenderer.java:88-113` — the elapsed use
  time capped at three ticks (`:89-92`), the raise of the wand by one block (`:93`), the
  thirty three degrees of the third person (`:95`), the ten plus ten degrees of the first
  person (`:97-98`), the sixty degrees of the startup (`:100`), the WAVE animation
  (`:101-105`), the CHARGE animation (`:106-110`) and the pivot one block below (`:112`),
  all in the basis that the model basis correction of `:87` establishes.

**Verdict.** Not an original class: the 1.12 helper only mirrors the original's inline
pose for the 1.12 tree (`WandPoseMath.java:14-30`; its only caller is the 1.12
`WandEffectOrigin.java:13,79`).

**The stale gap note — corrected.** The old note said:

> *"Open gap: the native port does not raise the wand while the player uses a focus, so the
> pose has no counterpart."* / *"REMAINING GAP (client lane): the use pose of the original
> ItemWandRenderer:63-93 is a client renderer duty."*

The remaining difference is **real**, but it is **not this row's**: the owner of the use
pose is the original's `ItemWandRenderer`, whose own row
(`thaumcraft/client/renderers/item/ItemWandRenderer.java`, `native_coverage: partial`) is
open and is being worked on by another lane. The note now **points at that row** instead
of claiming the gap here.

**Ledger change.** `missing → parity_verified`, `disposition: exclude`; the note names the
`ItemWandRenderer` row as the owner of the remaining difference and records that the
numbers of the 1.12 sampler are the numbers of the original.

**Remaining difference.** None of *this* row. The use pose of the original is still open —
in the `ItemWandRenderer` row.

### 2.7 `thaumcraft/client/renderers/item/WandUsePose.java` — CLOSED

Same search and same verdict: `grep -rl 'WandUsePose' <oracle>` → 0 files, and none of the
six pose fields appears in the oracle. The 1.12 holder (`WandUsePose.java:11-25`) is used
only by the 1.12 tree (`WandEffectOrigin.java:14,71`, `WandPoseMath.java:14`,
`WandUsePoseSampler.java:35`).

**Ledger change.** `missing → parity_verified`, `disposition: exclude`; the stale gap note
is replaced by the proof and by the pointer at the `ItemWandRenderer` row.

**Remaining difference.** None of this row; the use pose belongs to the
`ItemWandRenderer` row.

### 2.8 `thaumcraft/client/renderers/item/WandUsePoseSampler.java` — CLOSED

Same search and same verdict: `grep -rl 'WandUsePoseSampler' <oracle>` → 0 files; the
original computes the pose inline (`ItemWandRenderer.java:88-113`). The 1.12 sampler
(`WandUsePoseSampler.java:16-36`) is used only by the 1.12 tree
(`WandEffectOrigin.java:15,71`, `ItemWandRenderer.java:272`).

**Ledger change.** `missing → parity_verified`, `disposition: exclude`; the stale gap note
is replaced by the proof and by the pointer at the `ItemWandRenderer` row.

**Remaining difference.** None of this row; the use pose belongs to the
`ItemWandRenderer` row.

---

### 2.9 `thaumcraft/client/lib/EldritchDiagnostics.java` — CLOSED

**Search.**

* `grep -rl 'EldritchDiagnostics' <oracle>` → **0 files**.
* `grep -rn -E 'EldritchPerf|nothingTESRCalls|obeliskTESRCalls|fieldHelperFaces' <oracle>`
  → **0 hits**. The 4.2.3.5 tree has **no FPS instrumentation of any kind**.

**Verdict.** Not an original class: a 1.12 **development instrument** — the per frame
counters of an Eldritch dimension FPS investigation, logged every sixty frames
(`EldritchDiagnostics.java:11-16,44-86`).

**The callsite audit is decisive: the class is dead code even in the retained 1.12
reference.** Every call site is commented out —
`src/main/java/thaumcraft/client/renderers/tile/TileEldritchNothingRenderer.java:26,48,50,52`
("EldritchDiagnostics: disabled for release") and
`…/LayeredFieldPlaneHelper.java:67,79`. The native has no reference to it at all.

**Ledger change.** `not_assessed → parity_verified`, `disposition: exclude`; the evidence
now carries the oracle search, the dead-code callsite audit and the fact that the
eldritch rendering it measured is the original's and is present in the native
(`oracle ClientProxy.java:533-541,609` ↔ the native
`TileEldritchNothingRenderer` / `TileEldritchRenderer`).

**Remaining difference.** None. An instrument is not a gameplay contract.

---

### 2.10 `thaumcraft/client/renderers/item/ItemEldritchRenderer.java` — CLOSED

**Search.**

* `grep -rl 'ItemEldritchRenderer' <oracle>` → **0 files**.
* `grep -rn -E 'TileEntityItemStackRenderer|renderByItem|setTileEntityItemStackRenderer' <oracle>`
  → **0 hits**. The 4.2.3.5 tree never uses the item stack renderer API at all (and
  1.21.1 removed it).
* `oracle thaumcraft/client/ClientProxy.java:453-462` is the **complete** list of the item
  renderers of the original: the filled jar, the node jar, the aura node, the
  thaumometer, the casting wand, the trunk spawner, the bone bow and the wooden device.
  **No eldritch tile among them.**
* `oracle thaumcraft/client/ClientProxy.java:533-541,609` — the eldritch tiles of the
  original are rendered in the **world only** (the special tile renderers and
  `registerBlockRenderer(new BlockEldritchRenderer())`). They are world structures, not
  items.

**Verdict.** Not an original class: the 1.12 item stack renderer that draws the eldritch
tiles in the inventory (`ItemEldritchRenderer.java:18-84`; its only caller is the 1.12
`ClientProxy.java:886`).

**Ledger change.** `not_assessed → parity_verified`, `disposition: exclude`; the evidence
now carries the two oracle searches, the complete item renderer list of the original, the
world-only rendering of the eldritch tiles and the native's own world renderers
(`TileEldritchCapRenderer`, `TileEldritchObeliskRenderer`, `TileEldritchLockRenderer`,
`TileEldritchCrabSpawnerRenderer`).

**Remaining difference.** None. The eldritch tiles are drawn in the world exactly as the
original draws them.

---

### 2.11 `thaumcraft/common/integration/waila/GolemWailaPlugin.java` — CLOSED

**Search.**

* `grep -i 'waila' <oracle>` → **0 hits** over the 859 files; `grep -iE 'huduma|journeymap' <oracle>`
  → **0 hits**. WAILA did not exist for 1.7.10.
* `find <oracle> -iname '*waila*'` → **0 hits**; `find <oracle> -type d -name 'integration*'`
  → **0 hits**.
* `grep -rl 'GolemWailaPlugin' <oracle>` → **0 files**.

**Verdict.** Not an original class: a 1.12 WAILA provider that writes the material, the
core and the upgrade list into the hover tooltip of the golem **entity**
(`GolemWailaPlugin.java:16-34,41-69`; it has no caller in the 1.12 tree, it is registered
by the `@WailaPlugin` annotation at `:16`).

**The "installed upgrades" question — answered honestly.** The old note said:

> *"The information the plugin showed is only partly reproduced: the golem screen shows the
> material and the core, while the list of installed upgrades is shown nowhere in the
> native."*

Checked against the current tree:

* the **golem screen** does **not** list the installed upgrades — the native
  `GuiGolem.java` only *tests* the amounts of the two upgrades that enable features
  (`GuiGolem.java:244`, `GolemUpgrade.ENTROPY`);
* but the original **did not list them there either**:
  `oracle thaumcraft/client/gui/GuiGolem.java:102,134,167,237,330,399` only tests
  `getUpgradeAmount(4)` and `getUpgradeAmount(5)` to enable the colour bar, the guard
  targets, the sorting and the switch captions. **The original never showed a list of
  installed upgrades on the golem screen.**
* the upgrade **is** shown in the native, in the original's own place: the original names
  it in the hover text of the trunk spawner item that carries the golem
  (`oracle thaumcraft/common/entities/golems/ItemTrunkSpawner.java:76-91`), and the
  native reproduces that exactly —
  `common/src/main/java/thaumcraft/common/items/golems/ItemTrunkSpawner.java:28-38`
  (the name of the upgrade in blue at `:33`, the contents line at `:37`).
* the material and the core are shown by the native golem screen
  (`GuiGolem.java:52` the material frame, `:159-169` the core blurb / threat line).

So: **no original contract is lost.** The only thing missing is the *1.12 hover tooltip of
the golem entity*, which the original never had either. The note now says so and says
that, if that list is ever wanted, it belongs to the `GuiGolem` row — **not** to this one.

**Ledger change.** `not_assessed → parity_verified`, `disposition: exclude`; the evidence
now carries the two oracle searches, the original's own way of showing the upgrade
(`ItemTrunkSpawner:76-91` ↔ native `ItemTrunkSpawner:28-38`), the six `GuiGolem` lines
that prove the original never listed the upgrades, the callsite audit and the native
"no WAILA reference at all" grep.

**Remaining difference.** The hover tooltip of the golem **entity** is not in the native —
but that is a 1.12 WAILA feature, not an original behaviour. WAILA has no 1.21.1 version,
so the integration target does not exist; the native carries no WAILA code and no WAILA
reference and the mod loads unchanged without it.

---

## 3. The other rows I did **not** touch (reported, not edited)

Two rows carry notes that the present wave proved stale or wrong. They are **other units'**
rows and another lane owns them, so they are only reported here:

1. **`thaumcraft/client/fx/beams/FXBeamWand.java`** (owner `agent-w3clientfx`, now
   `parity_verified`) still carries the note *"REMAINING GAP (client lane): the original
   beam starts at the wand tip; the native focus particles start at the player."* — that
   is the very claim the w4wands wave disproved inside the same row (its evidence already
   says *"the origin of the original is the eye of the player … it is NOT the tip of the
   wand"*, `FXBeamWand.java:209-228`). The two notes contradict each other; the first one
   should be dropped.
2. **`thaumcraft/client/renderers/item/ItemWandRenderer.java`** (owner `audit-a9-core`,
   `partial`) — its remaining difference is the use pose and the basis correction. Its note
   already names the 1.12 helpers (`WandPoseMath`, `WandUsePose`, `WandUsePoseSampler`) as
   having "no native counterpart either"; with this wave those three rows are closed as
   legacy-only exclusions, so the note can now point at the three closed rows instead of
   leaving them dangling. The **use pose is still open** and belongs to that row.

---

## 4. What was and was not verified here

**Verified here (no Gradle needed):**

* the eleven oracle searches of §1, quoted with their results per unit;
* the eleven callsite audits over the retained 1.12 tree and over
  `common/src` + `fabric/src` + `neoforge/src`;
* `python3 scripts/porting/update_scope_ledger.py --check --original-root
  /home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle` → `stale_review_fingerprints: 0`,
  the `counts` block byte-identical, and exactly the eleven rows changed;
* a row-by-row diff of the ledger against `git show HEAD:docs/porting/scope-ledger.json`:
  11 rows changed, all of them `native_coverage missing/not_assessed → parity_verified`
  with `disposition: exclude` kept.

**NOT verified here (the leader must run it):**

* `:common:compileJava` and the rest of the build — **not run**; **no Java source was
  changed by this wave**, so there is nothing to compile;
* `:common:test`, `:fabric:runGameTest` — **not run**; **no test was authored or
  executed** by this wave (a ledger wave has nothing to test);
* the client frames — **not seen**; this wave changes no rendering.

**The count in `docs/MIGRATION_STATUS.md` and `docs/HANDOFF.md` is now stale** (both still
say `976 parity_verified / 55 partial / 10 missing / 3 not_assessed` in their top
snapshot). Those files are the leader's, and six other lanes are changing the `partial`
rows in parallel, so I did not touch them: the leader should refresh the snapshot from
the ledger itself after the merge. The authoritative numbers after this wave are
**`1017 parity_verified / 27 partial / 0 missing / 0 not_assessed`**.

---

## 5. The files that changed by this wave

```
scripts/porting/apply_w5legacy_verdicts.py   (new, the authored ledger update)
docs/porting/scope-ledger.json               (the eleven rows of this wave)
docs/analysis/2026-10-23-agent-w5legacy.md   (this report)
```

No source file of the mod was touched: every one of the eleven units is a class the
original 4.2.3.5 never had, so there was nothing to port.
