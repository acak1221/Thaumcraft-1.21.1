# h2visual — pipeline defect hunt

Worktree `/home/cyberfox-kali/wt/h2visual`, branch `agent/h2visual`, baseline `e79b8558`. Initial required status/log succeeded with clean status. No client or Gradle was run; no other worktree/main checkout was accessed or modified. Original oracle and the approved leader-cache remapped jar were read only.

Deliverable: `docs/audit/VISUAL_FINDINGS.md`, including per-hunt counts, file/line anchors, user-visible consequences, evidence strength and explicit validation limits.

## Outcome

12 distinct **PROVEN BY READING** defects; 11 fixed, advanced stabilizer item left open. Two suspected effects retained without speculative changes. No visual seen/verified claims.

| Commit | Fix |
|---|---|
| `04ae3e7b` | Invoke dormant shared eldritch block client initializer: 13 BER registry calls previously never executed on either loader |
| `1773945b` | Restore mirror pane oracle transforms, 0..1 corners, UV ordering and normal |
| `0bf886dc` | Node-jar item model inherits builtin jar item, enabling its registered renderer |
| `322db81b` | Node-jar crossed plane uses Y, not Z |
| `4e49ba29` | Excavation beam local +Y aligns with target using complementary pitch |
| `e2ccbd20` | Retain previous excavation width each tick instead of interpolating from permanent zero |
| `cef593f4` | Beam UV scroll uses original floor wrap, not sine |
| `e073ed04` | NeoForge invokes the shared elemental-pickaxe scan overlay |
| `cafcd96a` | Aura-node item enters builtin renderer instead of rendering a shard sprite |
| `cd6c5408` | Preserve aura-node builtin model in asset generator |
| `56802cd1` | Replace incorrect sine assertion with numeric oracle examples; Java test not run |
| `f68e974b` | NeoForge reload listener signals PreparationBarrier before applying resets; otherwise vanilla reload cannot finish |

## Unfixed / leader follow-up

* **Proven:** advanced stabilizer item uses `builtin/entity` but neither loader registers a renderer. Basic renderer rejects its stack and only implements lock1; do not paper over the absent lock2 visuals with basic artwork.
* **Suspected:** architect final translucent batch can remain queued beyond the world callback; trace flush and matrix ownership on a frame.
* **Suspected consequence:** relay flare supplies particle light coordinates to the unlit bubble shader, which cannot consume them. Compare original/ported dark-room flare before substituting shaders.
* None of the three core shaders has a demonstrated JSON/GLSL interface mismatch. GPU compilation still unperformed. Fabric namespace rewriting was checked in installed API sources rather than guessed from unpatched vanilla ShaderInstance.
* All 20 common/Fabric visual mixin entries match inspected vanilla members/anchors. Three NeoForge-specific entries and common targets on patched NeoForge remain **not runtime/patch verified**; the approved vanilla jar cannot prove patched overload/local stability.

## Evidence run

* 58 asset-converter Python tests passed, 3.707s.
* 29 custom item ancestry chains checked after fixes; 170 literal PNG references present (including eight vanilla references).
* Three core shader declaration/interface checks passed against actual remapped vanilla vertex stages.
* Independent Python matrix calculations: 24 mirror corners at expected .02/.98 block faces; 1,004 beam directions map local +Y correctly. Width assignment and floor wrap checked structurally.
* `javap -p -c -l -s` target outputs in ignored `.tmp/h2visual/`; signatures, redirect anchors and SectionCompiler local-variable capture inspected.

No compiled/runtime claim. Leader must build the changed tree and actually inspect the now-reachable effects. Passing old source-derived tests had positively concealed the wrong sine scroll; “tests green” was not evidence that a beam pointed toward its target.
