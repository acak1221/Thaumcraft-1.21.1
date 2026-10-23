# c3items — entity / AI quality audit

2026-10-23; branch `agent/c3items`; baseline `3340abe7`.

## Outcome

Audited the 95-file / 19,285-line entity surface. Full report:
`docs/audit/ENTITY_QUALITY.md`. It contains all eight requested sections, concrete-goal
flags/acquisition/continuation/cleanup tables, a 161-slot state ledger, all ten >400-line
source units, twenty duplicate-method groups, all save/load declaration counts, thirty
magic-number sites and a 95-file target/navigation/level inventory.

| Item | Inspected | Grouped findings |
|---|---:|---:|
| Goal lifecycle | 59 concrete + 4 bases | 7 |
| Ladder | 12 core configurations | 3 |
| Switch state | 157 mutable fields + 4 collections | 3 |
| Size / duplication | 95 files; 31 save + 33 load methods | 24 |
| Magic numbers | 803 tokens screened; worst 30 sites | 30 (1 fixed) |
| Dead code | 324 private methods; 817 fields; 1,042 overrides | 0 confirmed |
| Null safety | 909 API calls; 123 tick entry declarations | 2 readability findings; 0 proven NPEs |
| Consistency | 8 family/policy comparisons | 4 |

Total 73 grouped review findings, not 73 proven runtime bugs. 72 remain open.
Some low-priority duplicate adapter bodies may reasonably stay duplicated.

## Highest-priority handoff

* **Bulk rebuild skips stop.** Remapped GoalSelector bytecode confirms `removeAllGoals`
  only removes entries; `removeGoal` explicitly stops running goals. EntityGolemBase's
  manual reset sequence does not close the private ladder chest or active door. Review
  `golems/EntityGolemBase.java:1026-1039` with interruption/reconfiguration tests.
* Five custom goals acquire navigation but do not stop it: golem AvoidSwell, Pech Avoid,
  Pech Item, Pech Ranged and Cultist LongRange. Do not assume clearing a target releases a path.
* WatchClosestWatcher reserves MOVE but writes LOOK, allowing concurrent look writers.
  Watcher's client target cache is never invalidated on synced target changes.
* Gather home transfer happens outside a selector goal, despite the registration narrative.
* Cultist melee omits requiresUpdateEveryTick while Pech/TaintAnimal equivalents request it.

## Ladder summary

Fill = 5 M+L entries (0/1/2/3/4). Empty = 5 M+L entries (0/1/2/3/4).
Sorting = **4**, not 5, M+L entries (0/1/3/4). No simultaneous golem movement owners
were found in native selectors. All enabled cores share AvoidSwell0(M), Door5(J),
ReturnHome6(M+L), Watch7(L), IdleLook8(L). Guard/Butcher targeting separately owns TARGET.
Equal-priority goals exclude one another; registration order matters, but does not create
two MOVE owners. Gather's transfer side effect is a separate ownership concern.

## Safe change

Only production file changed:
`common/src/main/java/thaumcraft/common/entities/ai/pech/PechRangedGoal.java`.
Named seven existing numeric policies: attack range and square, visible-update stop gate,
minimum attack interval, chase speed, aim limit and minimum attack power. Kept exact
arithmetic and precision. No goal stop/reset behavior, registrations, saves, drops or public
API changed. No private method was deleted: apparent no-caller candidates use method references.

## Verification

1. Required first command succeeded in this worktree; initial status clean, baseline above.
2. `find common/src/main/java/thaumcraft/common/entities/ -type f | wc -l` → 95.
3. This checkout initially had no compiled common classes. Bootstrapped all 1,445 common
   Java sources using **javac 21**, `--release 21 -proc:none -sourcepath ''`, the cached
   Mojang remapped Minecraft 1.21.1 jar, existing access-widener overlay and dependencies.
   Output: this worktree's ignored `common/build/classes/java/main`; exit 0.
4. Focused changed-file javac against the remapped classpath **plus those common classes**:
   exit 0. `javap -c -p` method disassembly exactly matches pre-change compilation.
5. Final full common-source compilation repeated after the source change; exit 0.

Transient evidence: `.tmp/c3items/javac-bootstrap.log`, `javac-focused.log`,
`javac-final.log`, `inventory.tsv`, `classpath.txt`. These are intentionally not committed.
No Gradle was run. No other worktree or main checkout was accessed. No runtime/server/client
or oracle behavioral pass is claimed. Source screening and compiler success do not prove
all override hooks reachable or all entity paths exercised.
