# Repository artefact audit — a4hygiene

Base: `c6feeda7`, branch `agent/a4hygiene`, worktree `wt/a4hygiene`.
Inspection only: no Gradle, game, JUnit or acceptance run. Commands were run in
this worktree. The requested audit plan was read from its supplied main-tree
path; shared Git metadata was measured read-only. No other worktree was inspected
or changed. Findings describe this snapshot, not the leader's concurrent work.

## 4.1 — Index, worktree and size: NOT literally clean

Inspected: initial `git status --short` (empty), `git ls-files -z`, filesystem walk
including ignored paths, `du -sh .git`, `git rev-parse --git-common-dir`, size of
that shared directory, and `git ls-files -z | xargs -0 du -b | sort -rn | head -20`.

* The worktree `.git` is a **4.0K pointer file**, not repository size. The actual
  shared Git directory was **91M**, not the inherited 115M claim. No `.gradle*`,
  generated run directory, saved world, `.log`, `.class` or generated build JAR
  was found in this worktree or its tracked index. Java package directories named
  `world` are source, not saved games. No 1.1GB Gradle home has returned here.
* **Fixed:** removed the tracked empty `.tmp/.gitkeep`. No transient payload was
  hidden beneath it. This does not delete another worktree's ignored data.
* **Open policy conflict:** root `Thaumcraft-1.7.10-4.2.3.5.jar` (**12,565,807 B**)
  and `Thaumcraft-1.12.2-6.1.BETA26.jar` (**11,360,786 B**) remain tracked binaries,
  as does `gradle/wrapper/gradle-wrapper.jar`. The first two violate the literal
  no-JAR checklist but are protected original/donor inputs under AGENTS.md; the
  wrapper is a required build input. They are not generated build artefacts.
  Removing protected originals or rewriting history requires the leader's decision.
* **21 tracked PNGs exceed 300,000 bytes**, seven unique names duplicated in the
  modern, legacy and retained asset trees: `misc/nodes.png` 905526,
  `items/lightningring.png` 596893, `items/lightningringv.png` 588467,
  `misc/cultist_portal.png` 472581, `misc/eldritch_portal.png` 442686,
  `gui/gui_researchbackeldritch.png` 390881, `gui/gui_researchback.png` 315713.
  Roots are `common/src/main/resources/assets/thaumcraft/`,
  `src/main/resources/assets/thaumcraft/`, `thaumcraft_src/assets/thaumcraft/`.
  These are retained assets, not accidental caches; this audit does not recompress
  oracle artwork or delete the protected reference tree.

Largest 20 tracked files at entry (bytes):

| Bytes | Path |
|---:|---|
| 12565807 | `Thaumcraft-1.7.10-4.2.3.5.jar` |
| 11360786 | `Thaumcraft-1.12.2-6.1.BETA26.jar` |
| 2953697 | `docs/porting/scope-ledger.json` |
| 1521424 | `docs/porting/visnet-client.json` |
| 1496174 | `docs/analysis/2026-10-15-inventory-OpenTC4-Architectury.json` |
| 1496170 | `docs/analysis/2026-10-15-inventory-OpenTC4-Architectury-upstream.json` |
| 1230655 | `docs/porting/verification.json` |
| 1217635 | `docs/analysis/2026-10-15-inventory-legacy-1.12.json` |
| 905526 | `thaumcraft_src/assets/thaumcraft/textures/misc/nodes.png` |
| 905526 | `src/main/resources/assets/thaumcraft/textures/misc/nodes.png` |
| 905526 | `common/src/main/resources/assets/thaumcraft/textures/misc/nodes.png` |
| 751101 | `docs/porting/class-inventory.json` |
| 717981 | `docs/HANDOFF.md` |
| 706164 | `docs/porting/resource-manifest.json` |
| 663257 | `common/src/test/resources/thaumcraft/research/note-generation-java8.json` |
| 643944 | `docs/porting/focal-client.json` |
| 600777 | `docs/porting/voidjar-client.json` |
| 596893 | `thaumcraft_src/assets/thaumcraft/textures/items/lightningring.png` |
| 596893 | `src/main/resources/assets/thaumcraft/textures/items/lightningring.png` |
| 596893 | `common/src/main/resources/assets/thaumcraft/textures/items/lightningring.png` |

## 4.2 — Branches/worktrees: active audit exceptions, cleanup still required

Inspected `git branch` and `git worktree list`. Branches were `main`, `work`,
`agent/a1ledger`, `agent/a2quality`, `agent/a3tests`, `agent/a4hygiene`. Worktrees
were the main tree on `work` plus those four audit worktrees, all at `c6feeda7`.
No other branch/worktree was listed. They fail the user's strict final two-branch /
one-worktree target **now**, but are the active-use exception allowed by PLAN.md:115.
No branch or worktree was deleted. Leader must remove them after collecting work.

## 4.3 — Notices: hashes correct, provenance incomplete; font absent

Inspected every asset-specific paragraph in `THIRD_PARTY_NOTICES.md`, referenced
armor-layer reports, `docs/porting/source-lock.json`, all 1060 `copied_files`
targets in `docs/porting/resource-manifest.json`, original TC4 ZIP members, and
the external official `/tmp/opencode/mc1710/client.jar`.

**16 existing individual output-file notices checked**: flux-taint icon, nine
armor aliases (two Thaumium, two Void, one Goggles, four robes), toast, three blur
programs, bone-bow glint, ore stone. All exist. All existing explicit retained-file
SHA256 claims match; **0 wrong hashes, 0 phantom output files**. Hashes delegated
to the older armor reports also match. The icon's actual path is `flux_taint.png`,
not an inferred `taint_poison.png`; the notice now gives that exact path/hash.
All **1060/1060 manifest target hashes match**; this includes 111 sound outputs.

Independently checked official client SHA1
`e80d9b3bf5085002218d4be59e668bac718abbc6` and its stated SHA256; checked the
achievement-atlas source hash. Pixel-compared the toast and bobber crops, and the
taint icon crop. Compared both TC4 particle sheets and scanner atlas byte-for-byte
to original TC4 ZIP members; scanner OBJ agrees after canonical LF conversion.
109 sounds match the same-case original member, the other two match original
`runicShieldCharge.ogg` / `runicShieldEffect.ogg` after lowercase path adaptation.

**Fixed omissions:** explicit MCP/FML mapping provenance; named TC4 particle
sheets, scanner PNG/OBJ, Mojang particle-sheet bobber crop and source hash;
retained toast/blur output hashes; explicit sound ownership and all-111 hash
manifest pointer. Five newly enumerated retained outputs also verified, bringing
the individually checked output total to **21**. Original TC4/oracle, official
Mojang client and Forge are explicitly distinguished from shipped mod contents.
Also removed the false “one grep hit” claim at former notices line 41: the test
tree contains CodeChicken geometry citations as well as the production comment.

**Unresolved asset/claim mismatch:** no font atlas/file exists anywhere in the
tracked checkout (`*font*`, `*ascii*` and resource-tree inspection). The checklist's
assumption that a legacy font was extracted is false on this snapshot. A notice
cannot license or hash a nonexistent extraction. Added an explicit limitation,
not a made-up font notice. Whether modern glyph shapes satisfy visual parity
requires arbitration/frames; no asset was invented to turn the ledger green.

## 4.4 — Status: stale top-level state corrected, historical runs preserved

Recomputed the actual `units[*].review.native_coverage` counter with Python:
**1044 parity_verified**, zero other coverage values. Recomputed dispositions:
327 replace / 291 port / 263 adapt / 163 exclude. No ledger edit was made here.

* `docs/MIGRATION_STATUS.md:1`: replaced the leading current-state impression
  **1017 verified / 27 partial** with **1044/0/0/0**. Explicitly superseded older
  **976/55/10/3, 969/62/10/3, 945/86/10/3** and earlier snapshots by marking the
  entire inherited body historical. Its remaining-partial family lists are not
  current counts. No historical test measurement was fabricated or overwritten.
* `docs/HANDOFF.md:1`: same **1017/27 -> 1044/0** correction, zero missing and
  not_assessed; same explicit arbitration of 976, 969, 945 and old remainder
  lists. Newcomers are directed to current implementation evidence and acceptance
  work, not an obsolete instruction to rebuild an already implemented contract.
* `MODLOG.md:1`: added the missing 23 October **1044/1044** checkpoint. The prior
  first entry was 21 October and still said no client could run (former lines
  32-34). It is now explicitly historical, as are old counts and no-runtime
  claims throughout the log. A changelog must preserve actual past measurements.

The last integrated full-run numbers present in the inherited wave-4/5 summary
are **2384 JUnit / 325 Fabric GameTest / 90 Python**. These are **recorded prior
measurements, not independently reproduced audit results**. Reports
`2026-10-23-agent-w8fx.md:104-128`, `w8world.md:128-160` and
`w8blocks.md:150-184` explicitly describe javac-only tests and pending
runtime/frame acceptance. The current docs do not promote them to executed tests.
Wave-3 66/66 smoke frames (65 accepted, last-frame fix after run) and two-client
37/37 are historical acceptance evidence, not acceptance of wave-8 renderers.
NeoForge's old 300/306 result is not a clean final gate. The a3tests lane must
publish the actual new integrated result after its run; this worktree has none.

## 4.5 — Analysis inventory and recorded arbitration

Every entry is listed in [ANALYSIS_INVENTORY.txt](ANALYSIS_INVENTORY.txt), generated
from this worktree only (320 pre-existing files plus this lane's report).
Reports are historical evidence, not independent simultaneous truth. The scope
of this inspection is repository claims and the named reversed verdicts, not a
new oracle/code gameplay audit of all 1044 units. Exact report anchors below are
relative to `docs/analysis/` and retain their original line numbers.

| Unit / contradictory assertions | Arbitration / current evidence | Status |
|---|---|---|
| Linked mirror: `2026-10-23-agent-w3clientfx.md:305`, `w4devices.md:293` call texgen an unavoidable platform limitation; `w7items.md:125-149` rejects the approximation | `w8world.md:46-80,117` implements homogeneous S/T/Q + textureProj and corrects OBJECT_LINEAR vs EYE_LINEAR. Oracle/native anchors are in that report | Code contradiction resolved by wave 8; frames pending, NOT visually verified |
| Chunk replay: `w7world.md:77-102` expressly rejects the inherited “cannot replay” claim and finds stream reseeding / missing Nether path | `w7world.md:83-96` fixes shared Random; `w8world.md:7-44,119-120` implements Nether dispatch and preserves ten-chunk drain/seed | Code contradiction resolved; live save/reload and replay unrun |
| Scattered features: `w4devices.md:291` says no modern structure subsystem or shared aura flag; `w5world.md:18` still lists that gap | `w7world.md:124-140` finds modern starts and flag already wired; `w8world.md:21-26` confirms it and adds missing Nether behavior | Resolved in later source report; not proof of seed-identical modern terrain |
| Particle cap: `w3clientfx.md:17,293` calls original 2000 FIFO a platform limitation | `w7fx.md:57-95` says implementable, still missing; `w8fx.md:5-69` installs ownership and per-layer/dimension FIFO | Resolved in code; mixin/bootstrap/layer frames explicitly unrun |
| Ore pass: `w2clientfx.md:272` says fast graphics; `w3clientrender.md:184` and `w7blocks.md:72-83` say particle setting | `w8blocks.md:30-70` cites FML fields.csv:4214 = anisotropicFiltering and implements AF>1 bridge | Resolved: AF, not either earlier explanation; live AF bridge unrun |
| Stairs: `w5world.md:63-76` refutes “one in ~12000”, but :96-103 calls single-state model a limit | `w7world.md:104-122` rejects the exemption; `w8blocks.md:13-28` implements per-face position-aware UVs | Resolved in code; frames pending |
| Mirror axis / levitator: opposing early model verdicts | Dedicated `2026-10-23-agent-w3arb.md:135-228` resolves mirror units/normals; same report resolves levitator in section A | Explicit existing arbitration, not majority vote |
| Font: `w2clientfx.md:300` admits old glyph shapes not reproduced; :511-524 then upgrades TCFontRenderer because unused galactic atlas does not matter | Absence of galactic text does NOT prove equality of 1.7.10 and modern vanilla glyph artwork. No font extraction exists | **UNRESOLVED visual/font equivalence assertion**; keep asset absence explicit, no automatic parity upgrade |

The five specifically requested reversed verdicts have later code arbitration;
none establishes final runtime/visual acceptance. **Unresolved:** the font/glyph
equivalence claim and whether the later unrun renderer/mixin implementations
actually satisfy acceptance. The a1ledger/a3tests audits, not this documentation
lane, decide coverage honesty and fresh gates. There is no basis here to claim
all 321 reports were gameplay-verified or that every possible contradiction was
exhaustively disproved.

The current `TCFontRenderer` ledger row explicitly treats changed platform glyph
shapes as an adaptation, not a port defect. That arbitrates its **code-scope**
disposition; it does not establish equal artwork. The open font question above
is exact visual acceptance, not a claim that the ledger promises an extracted
font or that a galactic font needs to be used.
