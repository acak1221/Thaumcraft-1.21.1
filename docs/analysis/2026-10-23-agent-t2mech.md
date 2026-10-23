# t2mech — research table from a player's inventory

Branch: `agent/t2mech`. Scope: `researchhands` (four native frames).

## Scene and boundary

`ResearchHandsClientSequence` opens the physical research table with native
`gameMode.useItemOn`, shift-clicks a real inkwell and unsolved note from the
player inventory into the real menu, right-clicks two palette aspects and clicks
the combination orb, drags aspects onto the hex sheet to solve it, then clicks
the copy widget. It never calls the server combine/edit/copy handlers directly.

Declared inputs: bounded stone platform, formed two-block research table,
unsolved four-cell NITOR puzzle, inkwell, one paper, one black dye,
RESEARCHDUPE permission and finite pools (aer10/ignis10/lux5). Camera teleport,
survival mode and invulnerability are fixtures, not earned progression.
Potentia is a known endpoint with zero spendable points; other primal starting
pools are ordinary new-player defaults and are not spent by these actions.
No solved or copied note is supplied. The mod computes both outputs.

The focused scope copies the same `fabric/run/saves/New World` input as the
full scope into its own uniquely named save; it does not depend on the accepted
infusion/equipment progression. For this clean worktree that source is a fresh
1.21.1 seed23 flat NBT input with vanilla dimensions, no chunks, inventory or
knowledge, generated locally by ignored `.tmp/t2-flat-input.py`.

## Assertions

- Insertion: actual `GuiResearchTable`, authoritative ready menu, tool in slot0,
  unsolved note in slot1, server tile agrees.
- Combination: actual screen handlers send the intent; no optimistic mutation;
  displayed knowledge pools change from 10/10/5 to 9/9/6 (aer/ignis/lux), and a
  server-thread check verifies exactly the same counts.
- Solving: actual palette drag/release fills cells1/2 with lux/ignis; two ink
  damage, discovery item and complete component arrive through menu sync;
  pools9/8/5; server independently confirms solved output and costs.
- Copying: actual copy widget leaves pools unchanged until server sync; client
  slot1 becomes count2 with copies1, paper/dye disappear, ink remains damage2,
  and pools become9/7/2. Server cross-checks count/components, ink, supplies and
  pools, and confirms NITOR was not learned just by solving/copying.

## Runtime and visual evidence

Final-source attempt3 passed all native assertions and the four-frame Gradle
contract (`BUILD SUCCESSFUL in 45s`). All four final PNGs were opened and
inspected after completion. Scoped functional GUI acceptance only; not a full
TC4/original-JAR visual parity claim.

Exact execution (one Gradle at a time):

```sh
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export GRADLE_USER_HOME=/home/cyberfox-kali/wt/t2mech/.gradle-t2
export DISPLAY=:0 XAUTHORITY=/home/cyberfox-kali/.xauth-display0
rm -rf fabric/run-clienttest/screenshots
./gradlew :fabric:runClientSmoke -PclientSmokeScope=researchhands --console=plain
```

Compilation first passed via `:fabric:clientTestClasses` in 1m15s. Attempt1
(`.tmp/t2-client-1.log`, run820ce4dd-204b-41eb-aa1a-cd2f6841e315) inserted the
note and combined successfully, but the final solve assertion failed: the
fixture omitted knowledge of the potentia endpoint. The puzzle correctly kept
its unknown endpoint incomplete. Corrected by supplying known potentia at
zero points, NOT by supplying a solution. Its two frames/report were preserved
under `.tmp/t2-attempt-1-screenshots` and `.tmp/t2-attempt-1-result.json` before
the mandated screenshot cleanup for attempt2. The inspected attempt1 frame202
shows Nitor, the unknown endpoint `?`, combination controls and lux6 correctly.
This is a harness fixture defect, not evidence of a mod gameplay defect.

Attempt2 (`.tmp/t2-client-2.log`, run80b9ec41-eb4c-4d77-a207-831aec8421ee)
passed the four-frame Gradle contract. Inspected all four frames: inserted
unsolved sheet with known endpoints; aer9/ignis9/lux6 after combination;
connected lux/ignis chain and `Discovery` text after solving; count2 over the
discovery slot and emptied paper/dye hotbar cells after copying. No missing
textures, clipped labels or broken input widgets were observed. These are
functional GUI judgements, not original-JAR visual parity acceptance.

Inspection also exposed an evidence-reporting integration omission: the
driver's `previous_content_replayed` flag defaulted to true for the new focused
scope. Fixed to false for `researchhands`. Added scripted native hover of the
copy widget in frame203 to make its actual material/aspect cost tooltip visible
before payment. Attempt2 frames/report were archived under
`.tmp/t2-attempt-2-screenshots` and `.tmp/t2-attempt-2-result.json` before cleanup.
No common gameplay code was changed; no mod gameplay defect was found here.

### Final run and per-frame judgement

Run: `client-smoke-050399cf-31ba-468b-90e3-f544056b90e4`.
Log: `.tmp/t2-client-3.log`. Native report:
`fabric/run-clienttest/client-smoke-result.json`, SHA256
`d16f4b60eed237c7780c527434235852ae47d9f1cfa04dfb5c11b3d501c9dac4`.
Report says `SCRIPT_PASSED`, scope `researchhands`,
`previous_content_replayed:false`, `original_world_modified:false`.
The automatic report deliberately leaves visual inspection pending; this
human/model inspection record is separate rather than falsifying that output.

Each path below is under `fabric/run-clienttest/screenshots/` with the above
run ID followed by `-` and the frame filename:

| Frame | What I actually saw | Judgement |
| --- | --- | --- |
| `201-research-hands-inserted.png` | Inkwell and note in top slots; Nitor title; two empty hexes between known aer/potentia endpoints; `Incomplete`; paper/dye in hotbar; pools10/10/5. | Pass: native slots and unsolved sheet render correctly. |
| `202-research-hands-combined.png` | Aer and Ignis in combination circles, lit orb, lux6; aer9 and ignis9; sheet remains incomplete. | Pass: visible pool update and native combination controls. |
| `203-research-hands-solved.png` | Lux/ignis now connect endpoints; `Discovery`; star-shaped copy widget; readable native tooltip: `Cost to copy this research:`, `Paper × 1`, `Black Dye × 1`, `Ignis × 1`, `Lux × 3`. | Pass: solved state, enabled copy control and complete unclipped cost tooltip. |
| `204-research-hands-copied.png` | Discovery stack visibly marked2; star dimmed; paper/dye gone; lux2/ignis7; solved sheet remains `Discovery`. | Pass: paid copy outcome and material/pool changes visible. |

Final PNG SHA256 values (same frame order):

```text
7e168b9303316078dd3b864bcf4b3d885fb230144fcafc1dc3ed0e18e291ed93
4f942a3a73ef58cdb61f02081ea10d235501809355456951c526b5ad3ae85ea2
771b937a0f9870e993cdbb4250c42d1ea552410a4c00f264fc63d6f8248d9e9e
b1afe1286db011a83601479c5312d218c009576abcaea2e9e41e38803f978564
```

No observed missing textures, clipping, slot rejection, broken widget, wrong
tooltip price or client/server divergence in these frames. Startup logs still
contain existing no-data-fixer messages for mod entities. The locally generated
minimal input also logs a missing DragonFight metadata key, which vanilla
defaults during loading; this lane does not test the End or world migration.

## Not reached

No new arcane-workbench, infusion, crucible, golem or thaumometer scenes in
this lane. No multiplayer, full survival acquisition, NeoForge or oracle visual
parity claim. Existing unrelated sequences were read, not rerun as evidence.
