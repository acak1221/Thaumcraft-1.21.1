# Agent `clientrun2` — the `full` client smoke of the TC4 4.2.3.5 → 1.21.1 port

Date: 2026-10-23. Worktree: `/home/cyberfox-kali/wt/clientrun2`, branch `agent/clientrun2`.
Task: finish the native client smoke (`:fabric:runClientSmoke`, `full` scope, 66 screenshots),
judge every produced frame against the original TC4 behaviour, and report every crash.

This report continues the work of `docs/analysis/2026-10-23-agent-clientrun.md`
(the previous `clientrun` agent). Nothing of that report is repeated here except
the short context that the new findings depend on.

---

## 0. Result in one paragraph

**The `full` scope reached `SCRIPT_PASSED` with all 66 screenshots** (run C,
06:08-06:23, 14 minutes of wall clock, the task's own `doLast` contract
satisfied: `status == SCRIPT_PASSED`, `scope == full`, exactly 66 screenshots and
every screenshot file present with the recorded size — the log line is
*"Client full check passed: 66 screenshots; visual inspection is still
required."*).

The blocker that stopped every earlier run (the `FOCUS` stage, 42 of 66
screenshots) was **a port defect, not a harness defect**: the three original TC4
key bindings had been changed to *unbound*, while the 4.2.3.5 oracle binds them
to **F**, **H** and **G** by default. With the original defaults restored, the
run passes the `FOCUS` stage and completes the whole story.

**The script result is not visual evidence.** All 66 frames were opened and
judged (§6). One cosmetic defect is open (the focus radial's centre label is
drawn with overlapping glyphs, frame 43); everything else matches the original
assets and behaviour.

---

## 1. The environment of this session

* `DISPLAY=:0` with `XAUTHORITY=/home/cyberfox-kali/.xauth-display0` works; the
  sddm greeter's X server has **no window manager**, so GLFW never receives a
  `FocusIn` event and `Minecraft#isWindowActive()` stays `false` unless the input
  focus is set explicitly. The harness refuses to fake that flag, so the run
  needs a real `XSetInputFocus` on *its own* window while it is alive.
  `.tmp/xfocus.py` (a copy of the previous agent's helper) selects the window by
  the `_NET_WM_PID` that GLFW writes, so only *this* worktree's client is
  focused; `.tmp/run-smoke.sh` runs that helper in a loop for the lifetime of
  the Gradle task.
* A stale focus loop from the finished `clientrun` agent was still running
  (`while true; do python3 .tmp/xfocus.py :0 455214; sleep 4; done`). It targets
  a dead PID, so it is harmless, but it was left behind and should not be
  restarted by anybody else.
* The fixture world was **generated in this worktree** (`:fabric:runServer` with
  `eula=true`, `level-type=minecraft:normal`, `server-port=25599`) and published
  as `fabric/run/saves/New World`; it is a real 1.21.1 world with the four level
  stems `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end`,
  `thaumcraft:outer_lands` and the overworld generator `minecraft:noise`
  (verified by reading the gzip/NBT of `level.dat`).
* The machine is shared with four other agents. At the time of the runs there
  were three Minecraft clients and ~12 Gradle daemons alive on 15 GB of RAM; the
  load average reached 123. **The single most important environment fact of
  this session: the client is OOM-killed (exit 137) when it runs next to two
  other clients.** `.tmp/wait-and-run.sh` therefore waits for a quiet machine
  (≤1 other client, ≥3.8 GB available) before it starts the run.

---

## 2. The port defect that blocked the run: the three TC4 key bindings

### 2.1 What the original does

The oracle of the 4.2.3.5 jar binds **all three** of its key bindings by default:

```
oracle thaumcraft/common/lib/events/KeyHandler.java:36
    public KeyBinding keyF = new KeyBinding("Change Wand Focus", 33, "key.categories.misc");
oracle thaumcraft/common/lib/events/KeyHandler.java:37
    public KeyBinding keyH = new KeyBinding("Activate Hover Harness", 35, "key.categories.misc");
oracle thaumcraft/common/lib/events/KeyHandler.java:38
    public KeyBinding keyG = new KeyBinding("Misc Wand Toggle", 34, "key.categories.misc");
```

`33`, `34` and `35` are the LWJGL 2 `Keyboard` codes of **F**, **G** and **H**
(`Keyboard.KEY_F == 33`, `KEY_G == 34`, `KEY_H == 35`; the next codes are 36=J,
37=K, 38=L). So the original binds

* the wand focus key to **F**,
* the hover harness to **H**,
* the misc wand toggle to **G**.

### 2.2 What the port did, and why it is wrong

`common/src/main/java/thaumcraft/client/ThaumcraftFocusClient.java:22-26`,
`ThaumcraftTravelGearClient.java:48-51` and `ThaumcraftMiscKeyClient.java:25-28`
bound all three mappings to `GLFW.GLFW_KEY_UNKNOWN` (unbound) and justified it
with two wrong facts:

1. *"oracle `thaumcraft/api/Keys.java:7-9` binds keyF, keyH and keyG of the
   original to `Keyboard.KEY_NONE`"* — **the oracle has no
   `thaumcraft/api/Keys.java` at all.** The directory listing of
   `/home/cyberfox-kali/work/thaumcraft-4.2.3.5-oracle/thaumcraft/api/` contains
   only `aspects/ crafting/ damagesource/ entities/ internal/ nodes/ potions/
   research/ wands/` and the API classes; the key bindings live in
   `thaumcraft/common/lib/events/KeyHandler.java`. The `KEY_NONE` default belongs
   to the *later* 1.7.10/1.12 copies: `OpenTC4/src/main/java/thaumcraft/api/Keys.java:5`
   (`new KeyBinding("Change Wand Focus", Keyboard.KEY_NONE, "Thaumcraft")`) and
   this repository's own legacy copy
   `src/main/java/thaumcraft/client/lib/KeyHandler.java:26-27`
   (`DEFAULT_FOCUS_KEY = Keyboard.KEY_G`, `DEFAULT_MISC_KEY = Keyboard.KEY_NONE`,
   because 1.12's vanilla swap key is F). The 4.2.3.5 jar that this port
   reproduces is the authority, and it binds F/H/G.
2. *"the numbers 33, 34 and 35 are PAGE UP, PAGE DOWN and END, not F, G and H"* —
   **false**: 33/34/35 are F/G/H; PAGE UP/PAGE DOWN/END are 73/74/75.

The change was made by commit `a2a9fdf2` *"fix(clientrender): leave the three TC4
keys unbound, as the original does"*. The repository's own scope ledger
contradicts that commit inside the same unit: the evidence of the unit
`thaumcraft/client/lib/KeyHandler.java` still contains
*"oracle thaumcraft/common/lib/events/KeyHandler.java:8-11 the TC4 original:
keyF 'Change Wand Focus' default 33, keyH 'Activate Hover Harness' default 35,
keyG 'Misc Wand Toggle' default 34"* and *"native …
ThaumcraftFocusClient.java:23-24 CHANGE_FOCUS defaults to GLFW_KEY_F"* — while
the second ledger unit (`thaumcraft/common/lib/events/KeyHandler.java`) still
claims *"the F binding (LWJGL2 key 33)"* for the port. **The port contradicted
its own accepted ledger evidence.**

### 2.3 The visible effect

With the focus key unbound, the client smoke's `FOCUS` stage could not start:
`FocusClientSequence.pressFocusKey` (fabric/src/clientTest/java/thaumcraft/fabric/clienttest/FocusClientSequence.java:749)
requires the *real* default conflict

```java
require(focus.matches(GLFW.GLFW_KEY_F, 0) && focus.same(swap),
        "isolated native input fixture tests the actual default F/offhand conflict");
```

and the leader's run of 05:13 in the `clientrun` worktree died exactly there:

```
[05:13:53] CLIENT_SMOKE_SCRIPT_FAILED at FOCUS
java.lang.IllegalStateException: isolated native input fixture tests the actual default F/offhand conflict
    at FocusClientSequence.require(FocusClientSequence.java:1061)
    at FocusClientSequence.pressFocusKey(FocusClientSequence.java:749)
    at FocusClientSequence.tick(FocusClientSequence.java:349)
```

The harness was right; the port was wrong.

### 2.4 The fix (commit `e041e890`)

```
common/src/main/java/thaumcraft/client/ThaumcraftFocusClient.java:22-30
    CHANGE_FOCUS = new KeyMapping("key.thaumcraft.change_focus",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, KeyMapping.CATEGORY_MISC);
common/src/main/java/thaumcraft/client/ThaumcraftTravelGearClient.java:48-53
    HOVER = new KeyMapping("key.thaumcraft.hover", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, ...);
common/src/main/java/thaumcraft/client/ThaumcraftMiscKeyClient.java:25-30
    MISC_TOGGLE = new KeyMapping("key.thaumcraft.misc_toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, ...);
```

with the comments corrected to cite the real oracle file and the real key table.

**Why the collision with the vanilla offhand swap is not a new problem:** 1.21.1
binds the vanilla swap-offhand key to F (it was L in 1.7.10), so the original's
F focus key now shares its key with the vanilla swap. The port already carries
exactly the adaptation for that: `common/src/main/java/thaumcraft/common/mixin/client/WandFocusKeyMixin.java:14-24`
redirects the vanilla swap site to
`ThaumcraftFocusClient.consumeOffhandSwapClick` (common/src/main/java/thaumcraft/client/ThaumcraftFocusClient.java:139-165),
which suppresses the swap while a wand/bell is in the hand and coalesces the
two counters for ordinary hands. That machinery was written for the *shared*
binding and was dead code while the key was unbound; with the original default
restored it is live again, and the harness's ordinary-hand F swap
(`FocusClientSequence.NORMAL_SWAP_ACK`, "F with ordinary hands forwards precisely
one vanilla swap and no focus intent") passes.

### 2.5 The open diagnostic of the previous agent is answered

The previous agent left one question open: *"the focus stage prints the wand
components of the client slot 7 and of the server hand — if the client copy lost
`thaumcraft:wand_data`, that is a PORT defect in the inventory sync"*.

**It did not.** With the key default restored the `FOCUS` stage ran to
completion: the client-side copy of hotbar slot 7 kept the whole
`thaumcraft:wand_data` component (the greatwood/gold wand with its vis), the
server-side hand was the same stack, and the stage reached its last screenshot
(`46-native-shimmerleaf-cinderpearl-models-and-particles-world-en.png`). The
inventory sync of the custom data component is therefore **not** defective:
`ThaumcraftContent.WAND_DATA` is registered with
`DataComponentType.<WandData>builder().persistent(...).networkSynchronized(WandData.STREAM_CODEC)`
(common/src/main/java/thaumcraft/common/config/ThaumcraftContent.java:75-76),
i.e. it is a normal network-synchronised data component.

---

## 3. The runs of this session

| run | start | result | screenshots | last stage |
|-----|-------|--------|-------------|------------|
| run A | 05:19 | `SCRIPT_FAILED` by the **OS** (exit 137, OOM kill) after 14m51s | 55 | `INFUSION` (batch distillation) |
| run B | 05:47 | `SCRIPT_FAILED` by the **OS** (exit 137, OOM kill) after ~11 min | 57 | `INFUSION` (batch distillation) |
| **run C** | **06:08** | **`SCRIPT_PASSED`** — *"Client full check passed: 66 screenshots"* | **66** | **`FINISH`** |

### 3.1 Runs A and B — the sandbox, not the port

Both runs died with **exit 137**, the OOM killer's signature, and both died
*inside* the `INFUSION` stage while the harness' own 1200 s budget was still
unspent (14m51s and ~11 min). The kernel log names the victim:

```
[11887.002961] Out of memory: Killed process 561525 (java)
                 total-vm:11497432kB, anon-rss:2578820kB, ... oom_score_adj:0
```

2.5 GB of resident memory for one client, because the client JVM takes a
"1/4 of RAM" default heap (3.9 GB on this 15 GB machine) and three other
agents' clients plus a dozen Gradle daemons were alive at the same time. The
same OOM killer had already taken four other processes of this sandbox
(pids 425935, 503346, 444867, 542148).

**Mitigation used for run C** (an environment accommodation, not a behaviour
change): the client JVM's heap is capped with `JAVA_TOOL_OPTIONS=-Xmx1280m` and
the Gradle daemon with `-Xmx768m` (`.tmp/run-smoke.sh`), and the run only starts
when the machine is quiet (`.tmp/wait-and-run.sh`: at most one other client and
at least 4.2 GB available). With that, the client stays at ~1.5 GB and the run
completes.

### 3.2 Run C — the passing run

* 06:08:0x the client starts, 06:10:07 the first stage runs, 06:23:31 the 66th
  screenshot is written, 06:23:32 the client stops — **13.5 minutes of gameplay,
  inside the 1200 s budget** (the whole Gradle task took 14 minutes).
* `fabric/run-clienttest/client-smoke-result.json`:
  `status = SCRIPT_PASSED`, `stage = FINISH`, `scope = full`,
  `run_id = client-smoke-3814fcfa-fc5d-4502-bb0b-10512ccb695d`,
  66 screenshots, 148 recorded actions, and the honest
  `visual_inspection: "pending; script completion is not visual parity evidence"`.
* The `doLast` of `tasks.named('runClientSmoke')` in `fabric/build.gradle`
  verified the status, the scope, the exact count of 66 and the existence and
  byte size of every one of the 66 PNGs.
* The world opened **without** the vanilla "Worlds using Experimental Settings"
  dialog (no `00-vanilla-world-confirmation.png` exists and the `world_open`
  evidence is empty) — the port fix of the previous agent
  (`Tc4LevelStemLifecycleMixin`) still holds.

---

## 4. The crashes of the client runs

`fabric/run-clienttest/logs/latest.log` of the runs of this session contains
**no crash and no exception** other than the known, harmless

* `ERROR … No data fixer registered for thaumcraft:<entity>` (55 lines: every
  TC4 entity type; vanilla's data-fixer registry has no entry for modded
  entities — the task marks these as known and harmless), and
* `WARN … Shader rendertype_entity_translucent_emissive could not find sampler
  named Sampler2` (the known vanilla shader warning), plus vanilla's missing
  goat-horn sound events and one `Missing data pack generated_d5d4d75`.

The crashes of the earlier sessions and their fixes are in the previous
agent's report (§2 there): the `ItemEntity → EntitySpecialItem` cast crash of
the item pickup animation and the entity types without a renderer. **No new
client crash was found in this session.**

The two failures that are *not* crashes but end the run are both in the
harness/environment and are described in §5 and §3.1.

---

## 5. The harness findings of this session

### 5.1 The `ALCHEMY` stage has a fixture that dies (harness fragility, not a port defect)

The leader's run of 05:01 in the `clientrun` worktree failed in the `ALCHEMY`
stage:

```
[05:01:59] CLIENT_SMOKE_SCRIPT_FAILED at ALCHEMY
java.lang.IllegalStateException: Alchemy client step timed out: OUTPUT;
    water=1000; slot4=0 minecraft:air;
    nearby=[1 minecraft:glowstone_dust@(0.270263671875, 92.3125, 0.512939453375)]
```

The catalyst was never consumed and the water was never spent, i.e. the
crucible did not smelt. The cause is the **fixture's heat source**: the stage
places a *vanilla* fire block under the crucible
(`AlchemyClientSequence.java:75`), and 1.21.1's fire is ephemeral — it ages
(`FireBlock#tick`: `AGE` grows by 1 with probability 1/3 per fire tick, the
fire ticks every 10-19 ticks) and is removed at `AGE == 15` when the block below
is not flammable
(`if (i == 15 && randomSource.nextInt(4) == 0 && !this.canBurn(serverLevel.getBlockState(blockPos.below())))`).
The stone under the crucible is not flammable, so the fire dies after roughly
220-250 ticks. The `ALCHEMY` stage needs ~230 ticks before the glowstone
catalyst is dropped (the heat alone needs 151 ticks to pass 150), so the
catalyst lands in a crucible whose heat has already decayed below 150 — the
recipe cannot match and the item bounces (the original's behaviour for an item
with no object tags, oracle `thaumcraft/common/tiles/TileCrucible.java:271-278`).

**It is a race, not a certainty:** in run C (06:08) the same stage passed
(frames 27-32 were produced and the nitor was crafted at 06:11), because the
machine was quiet and the stage ran faster than the fire's lifetime. The
failure of 05:01 and the success of 06:11 are the same code with a different
tick budget, which is exactly what a dying heat source predicts.

The **port is right** here: the original heat sources are the fire material,
the lava material and the airy block with metadata 1, which is the *nitor*
block (oracle `TileCrucible.java:101-105`: `mat == Material.fire || mat ==
Material.lava || bi == ConfigBlocks.blockAiry && md == 1`, and
`oracle thaumcraft/common/blocks/BlockAiry.java:315-317` shows that metadata 1
creates a `TileNitor`). The port implements exactly that
(`common/src/main/java/thaumcraft/common/tiles/TileCrucible.java:155-163`).
The fragility is in the *harness fixture*, which uses a vanilla fire that 1.21.1
burns out.

To make the failure diagnosable, the timeout message of the stage now also
prints the heat, the heat source block and the aspect contents of the crucible
(`fabric/src/clientTest/java/thaumcraft/fabric/clienttest/AlchemyClientSequence.java:58-67`).
The fixture itself was left unchanged in this session; see §7 for the exact
remaining acceptance criterion.

### 5.2 The research category tabs (already fixed by the previous agent)

The previous agent replaced the missing-vanilla-Button lookups of the
distillation/focus sequences with the browser's own hit test
(`GuiResearchBrowser.mouseClicked → tabAt`, oracle
`thaumcraft/client/gui/GuiResearchBrowser.java:668-687`). That fix is correct
and is confirmed by this session's run: the `DISTILLATION` and `FOCUS` stages
both completed.

---

## 6. The visual verdicts, frame by frame

All frames are 1280x720 PNGs of a real 1.21.1 client (gui scale 2) taken by the
harness itself; each one was opened and looked at. The objective asset check of
the previous agent still holds: every GUI texture of the port is byte identical
to the 4.2.3.5 jar.

| frame | what it shows | verdict |
|-------|---------------|---------|
| 01 `browser-en` | the research browser over a real world: the title "Thaumonomicon", the five category icons in the left margin, the original entry icons, the green completed marks, the connection lines, "Drag to explore", "Done" | **pass** — the original `GuiResearchBrowser` layout and the original assets |
| 02 `thaumonomicon-en` | the open book with the original parchment and the original text, the page counter `1 / 1`, the ←/→ and "Back" buttons | **pass** |
| 03 `research-en` | the RESEARCH entry: the two-page spread, the magenta hyperlinks, the counters `1 / 16` and `3 / 16` | **pass** |
| 04 `research-illustrated-en` | the illustrated page of the same entry | **pass** (the original illustration) |
| 05 `research-previous-en` | the page after the "Previous" click; the page index really decreased by two | **pass** |
| 06 `browser-ru-small` | the browser in Russian at a small GUI scale | **pass** (the original translation, re-laid out) |
| 07 `thaumonomicon-ru-small` | the book in Russian at a small scale | **pass** |
| 08 `thaumonomicon-ru-small-next` | the next page of the Russian book | **pass** |
| 09 `secondary-unaffordable-en` | the ARTIFICE graph with the hover tooltip of "Enchanted Fabric": "Research points (available / cost) / Pannus: 3 / 3 (green) / Praecantatio: 2 / 3 (red) / Not enough research points in the required aspects." | **pass** — the original tooltip with the original colours |
| 10 `secondary-purchased-entry-en` | the purchased entry after the server completed it | **pass** |
| 11 `note-tooltip-en` | the inventory with the research-note tooltip ("Research Notes / Nitor / Mystical Flane") and the chat line "Research completed: Enchanted Fabric" | **pass** (the dark background is the vanilla container dimming) |
| 12 `discovery-tooltip-en` | the discovery tooltip in the hotbar | **pass** |
| 13 `primary-note-missing-materials-en` | the research table with the missing-material report | **pass** |
| 14 `primary-note-issued-inventory-en` | the issued note in the real survival inventory | **pass** |
| 15 `two-base-tables-world-en` | two TC4 research tables on a stone platform in a real world (grass hills, flowers, sugar cane), the chat line "Research notes created: Nitor", the HUD | **pass** — the original table model/texture in a real render |
| 16 `research-table-filled-world-en` | the filled research table in the world | **pass** |
| 17 `research-table-menu-note-en` | the research table menu with the note | **pass** |
| 18 `table-item-previews-inventory-en` | the table item previews in the inventory | **pass** |
| 19 `table-research-hand-base-dropped-world-en` | the dropped base table and the held research table in the world | **pass** |
| 20 `research-table-player-solved-en` | the table after the player solved it | **pass** |
| 21 `combine-primal-and-table-bonus-en` | the primal combination with the table bonus | **pass** |
| 22 `combined-new-compound-en` | the new compound | **pass** |
| 23 `arcane-workbench-insufficient-vis-en` | the arcane workbench without enough vis | **pass** |
| 24 `ores-node-and-arcane-workbench-world-en` | a real world with the TC4 ore blocks, the research table, the wand in hand and the vanilla chat "Gained 2 research point(s) for Tempestus" | **pass** |
| 25 `arcane-workbench-charged-recipe-en` | the charged arcane workbench | **pass** |
| 26 `arcane-recipe-book-en` | the arcane recipe book | **pass** |
| 27 `crucible-boiling-clean-water-en` | the TC4 crucible (the original black device texture) with clean water inside, the **vanilla fire below it**, the smoke, the bucket in hand | **pass** — the original block texture and the original heat source |
| 28 `crucible-dissolved-coal-and-torches-en` | the crucible after the coal and the torches dissolved | **pass** |
| 29 `crucible-physical-floating-nitor-en` | the crucible with the **nitor item floating above it** (the original nitor texture, the rotating special item) and the coloured aspect particles | **pass** — this is the frame whose renderer used to crash the client |
| 30 `crafted-nitor-heats-crucible-world-en` | the crafted nitor placed under the crucible, lighting it | **pass** |
| 31 `nitor-crucible-recipe-book-en` | the nitor crucible recipe page | **pass** |
| 32 `flux-gas-and-goo-native-world-en` | the two sealed glass cells with the flux gas and the flux goo (the original purple flux assets) | **pass** (a supplied fixture; the runtime behaviour is proven by the server tests, not by this picture) |
| 33 `alchemy-furnace-native-fuel-and-coal-progress-en` | the TC4 alchemy furnace screen over the vanilla inventory | **pass** — the GUI texture is byte identical to the original |
| 34 `distilled-alembics-burning-furnace-and-bellows-world-en` | the alchemy furnace with the alembic on top, the fire inside, the bellows and the two alembic jars | **pass** — the original OBJ models |
| 35 `native-phial-transfers-fill-two-jars-world-en` | the phial transfers filling the two jars | **pass** |
| 36 `distillation-native-furnace-recipe-book-en` | the distillation recipe page | **pass** |
| 37 `native-greatwood-silverwood-and-masonry-world-en` | the greatwood/silverwood trees and the masonry | **pass** (visual fixtures) |
| 38 `native-greatwood-gold-wand-assembly-menu-en` | the greatwood/gold wand assembly in the arcane menu | **pass** |
| 39 `crafted-greatwood-gold-wand-native-charging-world-en` | the crafted wand being charged | **pass** |
| 40 `native-thaumaturgy-gold-cap-and-greatwood-rod-icons-en` | the gold cap and the greatwood rod icons | **pass** |
| 41 `native-fire-focus-recipe-book-en` | the Fire Focus recipe page | **pass** |
| 42 `native-fire-focus-arcane-assembly-menu-en` | the Fire Focus assembly in the arcane menu | **pass** |
| 43 `native-held-F-focus-selection-radial-en` | the held-F radial: the title "Change Wand Focus", "Equipped: None", the ring, the focus icon, the item tooltip ("Wand Focus: Fire / Wis per tick / Ignis × 0.1 / …") and the "Remove (Left Shift+F)" / "Cancel" buttons | **defect (cosmetic, reproducible)** — the centre label is unreadable. The port draws the hovered focus's name and its inventory slot as one line ("Wand Focus: Fire — slot 9"), but the pixels are two overlapping text runs: the first four glyphs ("Wand") are clean and 12 physical px apart (the font's own advance), and from the fifth character on the glyphs are drawn closer together than that advance, so they lie on top of each other. The same string renders cleanly in the item tooltip of the same frame, so the font, the string and the assets are fine — the fault is in the port's own label path (`common/src/main/java/thaumcraft/client/gui/GuiFocusSelect.java:222-224`, `centered()` → `Font#plainSubstrByWidth` + `GuiGraphics#drawCenteredString`). The frame of the passing run shows exactly the same corruption, so it is deterministic and not a capture artefact. Not fixed in this session (see §7). |
| 44 `native-fire-focus-live-embers-damage-and-ignition-world-en` | the cow target "Native Fire Focus target" burning with the Fire Focus's live embers around it | **pass** |
| 45 `native-Shift-F-removal-returned-focus-inventory-en` | the inventory with the removed Fire Focus in the offhand slot and its tooltip | **pass** |
| 46 `native-shimmerleaf-cinderpearl-models-and-particles-world-en` | the shimmerleaf and the cinderpearl with their live particles in daylight | **pass** |
| 47 `native-basic-essentia-tube-recipe-book-en` | the essentia tube recipe page | **pass** |
| 48 `native-basic-essentia-tube-arcane-assembly-menu-en` | the tube assembly in the arcane menu | **pass** |
| 49 `native-two-crafted-tubes-floating-dry-network-world-en` | the two crafted tubes between the alchemy furnace and the node, the brass connectors and the faint blue essentia in the tube | **pass** |
| 50 `native-wand-indexed-arm-closed-typed-alembics-world-en` | the wand-indexed arm with the closed typed alembic | **pass** |
| 51 `native-typed-tube-pull-labelled-jar-and-other-alembic-world-en` | the typed tube pull into the labelled jar | **pass** |
| 52 `native-transport-jar-survival-break-pickup-replace-world-en` | the transported jar broken, picked up and replaced in survival | **pass** |
| 53 `native-silverwood-infusion-recipe-book-en` | the silverwood infusion recipe page | **pass** |
| 54 `native-infusion-matrix40ordo-arcane-menu-en` | the 40-ordo infusion matrix in the arcane menu | **pass** |
| 55 `native-crafted-altar-wand-formation-and72candles-world-en` | the formed infusion altar with the 72 candles, the four pillars, the matrix, the gold-capped greatwood wand in hand and the candle smoke | **pass** — the wand's cap and rod textures are the original assets; the 3D silhouette comes from the port's JSON model converted from the original OBJ and is not verifiable against a 1.7.10 client here |
| 56 `native48shards-phials-eight-typed-jars192essentia-world-en` | the eight unlabelled jars, each holding its own typed essence (the different colours inside the original jar model), in front of the formed altar with the 72 candles, at sunset | **pass** |
| 57 `native-infusion-paid-wireless-essentia-beams-world-en` | the **paid essentia beam** — the arc of orange essence travelling from the altar's node to the target, with the original wisps along the path | **pass** |
| 58 `native63essentia-seven-components-silverwood-core-pedestal-world-en` | the infusion pedestal between the four pillars, the original **infused stone** blocks (the dark blocks with the purple markings) above it and the coloured aspect particles of the seven components | **pass** |
| 59 `native-silverwood100vis-gold-wand-arcane-assembly-menu-en` | the silverwood/gold wand assembly in the arcane menu | **pass** |
| 60 `native-crafted-silverwood100vis-wand-charged-above50-world-en` | the crafted silverwood wand (the white rod with the gold cap) in hand, with the charged vis particles in the air | **pass** |
| 61 `native-thaumium-ordinary-equipment-recipe-book-en` | the thaumium/ordinary equipment recipe pages | **pass** |
| 62 `native-twelve-thaumium-finite-crucible-last-output-world-en` | the TC4 crucible with the thaumium essence in the water and the **last crafted output floating above it** in the original special-item render (the three-ray star and the orange aspect rings) | **pass** |
| 63 `native-thaumium-pickaxe-zero-vis-no-wand-workbench-menu-en` | the thaumium pickaxe in the workbench menu | **pass** |
| 64 `native-thaumium-pickaxe-survival-obsidian-drop-and-wear-world-en` | the survival mining of the obsidian with the thaumium pickaxe | **pass** |
| 65 `native-thaumium-pickaxe-earned-ingot-anvil-repair-menu-en` | the repair of the earned pickaxe at the anvil | **pass** |
| 66 `native-thaumium-chestplate-leggings-two-armor-layers-world-en` | the third-person view of the player wearing the **thaumium chestplate and leggings** (the original armour texture, the inner and the outer layer) with the thaumium pickaxe in hand | **pass** |

---

## 7. What is still unverified, and the exact remaining criteria

1. **`SCRIPT_PASSED` for the `full` scope — DONE.** Run C satisfied the contract
   of `fabric/build.gradle` (`tasks.named('runClientSmoke') { doLast { … } }`):
   `status == SCRIPT_PASSED`, `scope == full`, exactly **66** screenshots and
   every screenshot file present with the recorded size. The evidence is
   `fabric/run-clienttest/client-smoke-result.json` and
   `fabric/run-clienttest/screenshots/client-smoke-3814fcfa-fc5d-4502-bb0b-10512ccb695d-*`
   of this worktree.
2. **The `ALCHEMY` fixture's heat source** (§5.1) is the one *flaky* part of the
   run: it passed in run C and failed in the leader's 05:01 run. The port needs
   no change; the **harness fixture** has to use a heat source that 1.21.1 does
   not burn out — the original's other two heat sources are **lava** and the
   **nitor block** (oracle `TileCrucible.java:101-105`) — or the fixture has to
   re-place the fire. The acceptance criterion is unchanged: the stage must
   prove "4 potentia/4 ignis/3 lux, 50 mB spent, one Nitor ejected" within the
   stage's tick budget, on a loaded machine as well as on a quiet one.
3. **The focus radial's centre label** (frame 43, §6) is the only *open visual
   defect* of the 66 frames. The fault is in the port's own label drawing, not
   in the font (the same string renders cleanly in the tooltip of the same
   frame) and not in the asset. The acceptance criterion: the radial must show
   the hovered focus's name and its inventory slot as one readable line —
   `common/src/main/java/thaumcraft/client/gui/GuiFocusSelect.java:222-224`
   (`centered()` → `Font#plainSubstrByWidth` + `GuiGraphics#drawCenteredString`).
4. **The five entity types without a renderer** (`thaumcraft:bottle_taint`,
   `golem_orb`, `primal_arrow`, `taint_crawler`, `taint_seed`,
   `taint_seed_prime`, `taintacle_giant`) are still unregistered in this
   worktree; another agent owns that work (the previous agent's report §5.2).
   They are not reached by the `full` scope, but every one of them is a
   guaranteed `NullPointerException` in `EntityRenderDispatcher.render` when it
   is drawn.
5. **The scope ledger** still carries the false oracle citation of §2.2 in the
   unit `thaumcraft/client/lib/KeyHandler.java` (the evidence string that
   quotes the non-existent `oracle thaumcraft/api/Keys.java:7-9`). The unit's
   *code* is now right; the ledger's evidence string should be corrected by the
   agent that owns the ledger. This session did not touch
   `docs/porting/scope-ledger.json`.
6. **Multiplayer, the NeoForge client, survival progression and the audio** are
   not verified by this session.
7. `docs/porting/*-client.json` and `docs/porting/verification.json` were not
   modified by this session.
8. **The `full` run needs a quiet machine.** With three other agents' clients
   and a dozen Gradle daemons on 15 GB, the client is OOM-killed (exit 137)
   twice out of three. The recipe that works: cap the client heap
   (`JAVA_TOOL_OPTIONS=-Xmx1280m`), cap the Gradle daemon (`-Xmx768m`) and start
   the run only when at most one other client is alive (`.tmp/wait-and-run.sh`).
   The run itself then needs 13.5 of its 20 minutes, so a machine that is 2x
   slower than this one would still fit.

---

## 8. The commits of this session (branch `agent/clientrun2`)

| commit | content |
|--------|---------|
| `e041e890` | **fix(client): the three TC4 keys keep the original F/H/G defaults** — the port fix of §2 (three files under `common/src/main/java/thaumcraft/client/`) |
| (this commit) | **docs(client): the passing `full` run, the 66 visual verdicts and the key-binding defect** — this report |
| (this commit) | **test(client): the alchemy stage prints its heat, heat source and aspects** — the harness diagnostic of §5.1 (`fabric/src/clientTest/java/thaumcraft/fabric/clienttest/AlchemyClientSequence.java`), a diagnostic-only change |

The transient helpers of this session (`.tmp/xfocus.py`, `.tmp/run-smoke.sh`,
`.tmp/wait-and-run.sh`, `.tmp/make-world.sh`, `.tmp/nbt_read.py`) are **not**
committed: they are environment tools, and `.tmp/` is ignored by the repository.
The fixture world (`fabric/run/saves/New World`) and the run directory
(`fabric/run-clienttest/`) are generated artefacts and are not committed either.

### The exact reproduction of the passing run

```bash
cd /home/cyberfox-kali/wt/clientrun2
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/clientrun2/.gradle-client2
export DISPLAY=:0
export XAUTHORITY=/home/cyberfox-kali/.xauth-display0
./.tmp/wait-and-run.sh      # waits for a quiet machine, then runs the smoke
# the run itself:  ./gradlew --console=plain :fabric:runClientSmoke
```

