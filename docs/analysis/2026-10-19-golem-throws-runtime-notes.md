# Fill / Empty directed world throws — runtime final, 2026-10-19

Common source implementation is final for root integration; **not compiled or
run by this author**. The reported preceding Fill/Empty Fabric PASS39s validates
the preceding container-only sources, not these throws. No Gradle/Java, game,
tests, Python or save runs were performed for this change.

## Owned changes / original authority

Only these existing production files changed, under
`common/src/main/java/thaumcraft/common/entities/golems/`:

- `EntityGolemBase.java`: live target checks, guarded queue bridge, action timer.
- `InventoryGolemGoals.java`: actual throw phase and world-aware preflight/routes.
- `GolemHomeInventory.java`: positive bounded world-target eligibility predicate.

No queue, Gather goals, data/marker/bell/core, menu/client, registry, resources,
recipes or persistence schema were edited. Root's `canEditLinks(Player)` and
delegating `canConfigure(Player)` are unchanged, including unsupported/retiring
refusal and remote loaded-worker linking. Seven materials/Flesh refusal,
health/fire/names, separate physical cargo, six quantity ghosts, recall and
native component/sided inventory transfers remain intact.

Oracle paths below are under
`../work/thaumcraft-4.2.3.5-oracle/thaumcraft/`:

| Anchor | Implemented branch |
| --- | --- |
| `common/entities/ai/inventory/AIHomeDrop.java:37–47,71–88` | Fill cargo, idle navigation, home-center distance²≤5; aim at attached block `home − homeFacing`. |
| `common/entities/ai/inventory/AIEmptyDrop.java:30–43,58–78`; `common/entities/golems/GolemHelper.java:78–85` | Empty cargo, idle navigation, first eligible adjacent world marker in list order, center distance²<4. No face offset. |
| `GolemHelper.java:515–541`; `common/entities/ai/inventory/AIEmptyGoto.java:49–90` | Empty withdrawal preflight and Goto prefer eligible inventory destinations (nearest), then first eligible world marker, without the inventory range16 clamp. |
| `common/entities/golems/EntityGolemBase.java:269–290,408–420,1010–1018,1035–1038` | Inventory insertion precedes directed throw; Gather has no throw. Six-tick action/status4 only on an actual throw. |

## Actual behavior / safety bound

- World eligibility requires an already-loaded, in-height, in-border target,
  with no actual BE, no BE-providing state/block, and no native `Container` or
  `WorldlyContainerHolder` block (including non-BE inventory providers).
  Accepted blocks are registered `minecraft` blocks backed by the native Block
  class package, plus `thaumcraft` solids whose class is **exactly** native
  `Block`. The native package is derived from `Block.class`, not a hard-coded
  development name, so this check remains valid after Fabric remapping.
- Unknown/addon blocks, **all non-inventory BEs**, missing inventory BEs,
  unloaded/out-of-bounds targets and unsupported providers are refused. A full
  or side-refusing inventory is still an inventory, never an open-world target.
  No insertion-failure fallback, forced chunks or world fixtures were added.
- Fill may fetch only with a populated exemplar and either its existing known
  home inventory or a positively eligible home world target. For the latter,
  precise mode uses summed matching wanted quantities minus logical stock0;
  Any uses material carry limit. This explicitly enables the missing-home throw
  branch rather than leaving `fillRequest` stuck at0 for every null inventory.
  Native first-slot extraction, capacity/item limits and source remainders remain.
- Empty accepts an eligible world route **before** taking from home, and checks
  the actual selected item's route again immediately before withdrawal. Inventory
  routes retain their previous range/side/home-storage rules. World routes have
  no added16-block clamp or value-based home exclusion. Other-dimension markers
  persist unchanged but never authorize work in this dimension.
- Adjacent accepting inventories take precedence over Empty's throw phase.
  Otherwise it throws toward the first currently eligible adjacent world marker,
  even if a remote inventory route exists, as the original Drop-before-Goto
  priority permits. Throws do not acquire a five-tick decision/interaction gate
  or a200-tick rate limit. Existing Goto cadence, native path,200-update budget,
  stalled20-update detour, loaded-region guards and wall-clock10s failures remain.
- If a route changes after taking, the worker rechecks at action time. Invalid
  targets retain cargo or allow an actual home return; Gather2 never throws.

## Exact queue dependency / narrow public bridge

Root owns and implements this sole new shared-queue dependency:

```java
DeferredItemDrops.enqueueGolemThrow(ServerLevel level, EntityGolemBase golem,
                                   BlockPos target); // boolean
```

The worker enters `busy` plus a separate private `throwTarget` only inside its
private real throw method, then calls that exact API. Public entity bridges:

```java
public boolean canPublishDirectedThrow(BlockPos target);
public boolean detachDirectedThrowCargo(BlockPos target, ItemStack expected);
```

Authorization is false outside that private intent. It rechecks server thread,
live ID/UUID identity, `canWork`, actual cargo, idle navigation and the exact
currently eligible Fill-home or first adjacent Empty-marker target. Detach also
requires native `ItemStack.matches` (count AND full components). Its only success
mutation is raw `carried = ItemStack.EMPTY`, immediately followed by returntrue:
no sync, event, navigation, sound or world callback follows inside that bridge.

Root prepares/appends one existing Pending before this raw detach, with no
callbacks/flush between append and payment. False removes only its unpublished
row. True means the whole actual paid stack belongs to that queue, **not spawn
success**. Display sync and idle action6/status4 occur only after true; private
guards clear in `finally`. Feedback failure never refunds/re-extracts/requeues
the paid cargo. No new escrow, journal, tracker or queue version was introduced.

Physical output is root's existing Pending delivery path with the exact live
feet/half-height spawn and unnormalized `distance/3` trajectory from
`2026-10-19-golem-world-throws.md:18–28`, whole stack/components and pickup delay10.
No generic scatter enqueue or direct `addFreshEntity` substitute is called here.

## Action timer / exact final client integration

`ACTION` is synced, transient, starts at6 only after a successful throw handoff
and only when idle, and decrements on native server AI ticks. Client status4
sets the same countdown to6. `getActionTimer()` preserves original
`3 − abs(ACTION − 3)` (pose sequence0→3→0), not raw6 or normalized attackTime.
Healing remains its separate existing countdown; no idle-work timer spam or
invented throw sound was added.

The final client files were read as authorized. **Required pose-wiring delta:
none** in their current versions. Keep these separate existing feeds in
`client/renderers/entity/RenderGolemBase.java:45–53`:

```java
model.setHealingTimer(healing ? entity.getHealingTimer() : 0);
model.setCombatTimer(entity.getActionTimer());
```

Despite its name, final `client/renderers/models/ModelGolem.java:64–66,102–104`
uses `setCombatTimer` for the original action arm pose. `setHealingTimer` affects
skin RGB only (`:61–62,118–127`), not action. This matches oracle
`client/renderers/models/entities/ModelGolem.java:91–94,133–150`. Do not swap the
feeds or send raw ACTION to the model; the damage layer still disables only
healing tint, not the action pose. Root can now remove obsolete container-only
scope tooltips and compile after its queue implementation is final.

Unknown non-inventory BEs remain an explicit original-parity gap. No full
power-loss atomicity, delivery/gameplay success or inspected FX/visual result is
claimed. Queue implementation and final integrated Fabric compilation are root's
work; this handoff changes neither the shared queue nor any client/resource file.
