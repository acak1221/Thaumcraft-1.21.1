# GUI/container agreement audit — g12gui — 2026-10-23

## Scope and evidence standard

Inspected **18 menu implementations/use sites**: all **17** custom production
`AbstractContainerMenu` subclasses in `common/.../container`, plus Hungry Chest's
vanilla `ChestMenu.threeRows`. Test observer menus and the research-note count
mixin are not additional production menus. Indices below are menu indices;
ranges are half-open. Every menu appends 27 main-inventory and 9 hotbar slots.

Read the native menus, their backing inventories, menu registrations, and the
retained 1.12 references. Also compared slot/transfer/close methods in the
readable `/home/cyberfox-kali/OpenTC4` reference. That reference is **not** a
substitute for the private original-JAR oracle; no new gameplay parity claim is
made here. In particular unrestricted manual furnace fuel is intentional, not
a finding. Changes concern rejection/ownership, not TC4 ingredient selection.

## PROVEN

**One confirmed wrong slot/rejection path, in one menu; no proven dupe.**
`guiMagicBoxRejectedShiftConservesItems` / `GuiRejectionRuntimeChecks.magicBox`
was run on the unchanged production code and **FAILED**:

> rejected shift-click must preserve all seven dirt across player, backend,
> cursor and drops; actual=0

The test first opens the actual registered Magic Box menu through ordinary
block use by a connected survival player, moves seven dirt in and out, then
loads a declared malformed `Items` tag into the same live BE. Backend input and
validity are rejected, but the old menu remained valid and accepted a native
shift-click: its setter refused the item after the player source was shrunk.
Baseline log: `.tmp-g12-before.log:656-686` (local/ignored); Gradle exit 1,
one required test failed. This establishes **loss in a fault-state fixture**,
not survival reachability of malformed NBT and not duplicate generation.

Fix implemented in `ContainerMagicBox`: delegate placement to backend
`canPlaceItem`, deny pickup when menu/backend validity fails, require backend
`stillValid` even for BEs, and guard ordinary clicks by the same active session
gate as quick-move. Healthy unrestricted inputs and linked routing are unchanged.
Fresh-world post-fix Fabric run **PASSED**, all 1 required tests passed;
`.tmp-g12-after.log:429,455` records `BUILD SUCCESSFUL in 2m 18s`.
The final-source run compiled the actual target and exercised healthy transfer,
rejected shift-click count conservation (seven retained), invalid menu state,
and rejection by both slot predicates. XML:
`fabric/build/gametest-focused-results.xml`. The log/XML are ignored local
artifacts; this document records their results, not client-visual acceptance.

## Menu-by-menu inspection

`R` means reasoned/source inspection only in this audit, not a newly executed
GameTest. Prior test names identify existing coverage, **not rerun results**.

| # | Menu; device and total slots | Placement / pickup rules | Quick-move path | Validity / close ownership | Test; fix/status |
|---|---|---|---|---|---|
| 1 | Arcane Workbench; result + wand + 9 matrix = 47 | Result refuses placement; extraction recomputes paid recipe; wand predicate/max 1; matrix unrestricted | Result consumes recipe then reverse `[11,47)`, drops leftover; wand to `[1,2)`; other player stock main↔hotbar; inputs to player | Viewer/alive/non-spectator; backend identity/distance ≤8; close clears preview only, shared BE inputs persist; refresh on changes/broadcast | R; `ArcaneWorkbenchRuntimeChecks`; no new defect |
| 2 | Alchemy Furnace; 2 / 38 | Aspect-bearing input; **manual fuel unrestricted in original**; automated/shift fuel predicate differs intentionally | Player fuel→1 then aspect input fallback; aspects→0; other main↔hotbar; device→`[2,38)` | Active viewer; BE identity/distance ≤8; live BE writes, no detached close snapshot | R; `AlchemyFurnaceRuntimeChecks`; no new defect |
| 3 | Research Table; 2 / 38 | Scribe tools / research-note classes; GUI intentionally accepts solved notes beyond automation rules | Player→`[0,2)` with `mayPlace` checked in both custom merge phases; reverse return; bounded loops; whole-note overstack quirk retained | Viewer/alive/non-spectator and backend distance ≤8; shared BE slots persist; oversized cursor close uses durable drops | R; `ResearchTableRuntimeChecks`; no new defect |
| 4 | Thaumatorium; 1 / 37 | Unrestricted catalyst as original; research/recipe selection separate from placement | Player→0; device→`[1,37)` | Active viewer/server thread and backend identity/distance ≤8; live BE writes | R; no new defect |
| 5 | Arcane Bore; 2 / 38 | Excavation focus / pickaxe backend predicate; max 1; pickup denied during unavailable inventory | Matching items→0/1 only; device→reverse `[2,38)`; commits defensive getter copy via setter | Connected active viewer, lifetime and current BE/distance ≤8; live writes | R; no new defect |
| 6 | Deconstruction Table; 1 / 37 | Aspect slot plus backend `canPlaceItem`; reward is a button, not an output slot | Aspects→0; other main↔hotbar; device→`[1,37)` | Active server viewer + backend identity/distance ≤8; live BE writes | R; no new defect |
| 7 | Focal Manipulator; 1 / 37 | Focus class; pickup allowed (native removal cancels job) | Focus→0; other main↔hotbar; device→`[1,37)` | Connected active viewer + lifetime/current BE/distance ≤8; live writes/revision observation | R; `FocalManipulatorRuntimeChecks`; no new defect |
| 8 | Spa; 1 / 37 | Bath salts only; backend predicate/lease; pickup requires active inventory | Salts only in either direction; reverse device return; no unrelated shuffle | Active/current BE and distance ≤8; BE writes survive close | R; `SpaMenuRuntimeChecks` has rejection/full/remainder/close assertions; not rerun |
| 9 | Focus Pouch; 18 / 54 | Focus class/max 1; physical source hotbar slot denies placement/pickup/swap | Focus-only player↔pouch, reverse return; source slot excluded | Exact hand stack, connected live owner and observed components; every mutation persists, valid close persists again | R; no new defect |
| 10 | Hover Harness; 1 / 37 | Valid fuel jar/max 1; physical harness source slot locked | Jar-only player↔input, reverse return | Exact hand stack/live owner/observed components; persist per mutation and valid close | R; no new defect |
| 11 | Hand Mirror; 1 / 37 | Input accepts anything as original; transport bridge controls delivery | Player mirror refused; other player stock→0; input→reverse player | Viewer/non-spectator only (no physical hand binding); callback writes transport; close returns untransported input | R; physical lifetime/transport callback scenarios remain unverified, not claimed bugs |
| 12 | Magic Box; 27 / 63 | **Fixed:** backend `canPlaceItem`; pickup requires valid viewer/backend | Boundary uses **whole linked size**, not bound 27; player→box / reverse return; rejected backend now refuses transfer | **Fixed:** backend validity plus native BE identity/distance ≤8; ordinary clicks require active session; close calls `stopOpen` | **PROVEN loss before fix (7→0), PASS after fix (7 retained)**; `guiMagicBoxRejectedShiftConservesItems` |
| 13 | Golem; 0, 1 or 6 ghosts / 36, 37 or 42 | Ghosts deny real placement/pickup; explicit ghost click copies exemplar without payment | Only main↔hotbar; ghost source refused | Entity UUID/core/configurability + active viewer; close resumes golem and never drops exemplars | R; `GolemGhostSlotsRuntimeChecks`; no new defect |
| 14 | Traveling Trunk; rows×9 / rows×9+36 | Real entity inventory unrestricted | Player↔actual row count, reverse return | Active viewer/alive entity/same level/owner-upgrade rules/distance ≤10; close shuts lid; inventory lives on entity | R; no new defect |
| 15 | Pech; 1 input + 4 output / 41 | Input unrestricted; outputs deny placement | Player→input only; all merchant slots→reverse `[5,41)` | Current customer/tamed live entity/active viewer/distance ≤8; close queues all remaining stock exactly once | R; no new defect |
| 16 | Accessories; 4 / 40 | Type + `canEquip`, max 1; pickup calls `canUnequip` | Player→matching accessory, fallback main↔hotbar; accessory→reverse player | Owner/live/non-spectator/attachment not rejected; attached inventory persists immediately | R; no new defect |
| 17 | Shared Ghost Slots; N / N+36 | `SlotGhost` denies pickup but inherits permissive `mayPlace`; fluid ghost has its own predicate; special clicked handler | Refuses ghost source; player→`[0,N)` can insert physical stock into a preview if directly invoked | Base `stillValid=false`, not independently usable; configuration preview persists only in supplied inventory | R; `GhostSlotMenuTest`; inactive-base transfer risk, no runtime proof |
| 18 | Hungry Chest (`ChestMenu.threeRows`); 27 / 63 | Native unrestricted chest slots backed by 27 bounded BE entries | Vanilla player↔`[0,27)` / reverse return | `ChestMenu` delegates backend identity/distance ≤8; backend live writes; open/close lid accounting | R; no new custom routing defect |

Client and server constructors use the same slot-building path for every custom
menu. Golem serializes its opening core (which chooses ghost count); Trunk
serializes rows. Magic Box deliberately binds 27 on both sides even when its
backend has more capacity: this is not a client/server slot-count discrepancy,
but its linked quick-move boundary remains a separate concern below.

### Requested devices with no screen/menu

Crucible, jars (including void jars), infusion pedestal, and wand pedestal use
world/item interactions rather than `AbstractContainerMenu`. There is no
quick-move path or client slot list to audit for those devices. Harness's jar
slot is included above. No nonexistent menus were invented or counted.

## Reasoned-only / limitations

* The 17 non-Magic-Box rows above are source inspections, not exhaustive runtime
  rejection/full-target/close/distance tests. They are **not** 17 proven-safe menus.
* Magic Box's linked-size quick-move boundary can differ from bound slots and
  may exceed the 63-entry menu. Existing code explicitly preserves an original
  quirk. No failing GameTest for linked boxes was run here; no routing change or
  new dupe claim is made for it.
* Hand Mirror has no hand/item lifetime binding; stale bridge callbacks and
  full-target merges merit runtime testing. Original permissive validity is
  not alone proof of an ownership defect.
* Shared Ghost Slots' base quick-move targets preview slots whose ordinary
  item predicate permits insertion. The base is deliberately invalid and
  production Golem uses its own safe transfer implementation. Reachability
  and count loss through the generic registered base remain reasoned-only;
  no fix or runtime defect claim is made.
* Malformed payload transitions in other devices, modded reentrant item hooks,
  oversized legacy research-note stacks, and all client packet/visual behavior
  are not runtime-proven by this focused audit.
* No duplicate-generation claim: the conservation regression specifically
  measures **loss**, counting player inventory, backend, cursor and
  nearby dropped items. Live malformed reload is a declared fixture, not proof
  an ordinary survival player can create rejected data.
