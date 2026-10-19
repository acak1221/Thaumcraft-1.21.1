# Golem world throws — next content unit, 2026-10-19

**Source contract only; not implemented by the current container-only Fill/Empty slice.** Only this note was added; original sources and the explicitly shared queue were read, not runtime WIP. Data/Bell/Core/Marker stay untouched. Interfaces remain those in [the final inventory API](2026-10-19-golem-inventory-api.md).

Oracle root: `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/`. Below, AI files are `common/entities/ai/inventory/`; E/H mean `common/entities/golems/EntityGolemBase.java` / `GolemHelper.java`.

## Original eligible throws, not overflow disposal

| Goal | Actual condition and target |
| --- | --- |
| Fill0 `AIHomeDrop:37–47,71–88` | Cargo present, navigation finished, golem-feet distance² to walking-home center **≤5**; attached target has no tile or a non-`IInventory` tile. With walking home H₀ and placement face F, target block **B=H₀−F**, aim at B's center. |
| Empty1 `AIEmptyDrop:30–43,58–78`; H:78–85 | Cargo present, navigation finished; first eligible marked non-inventory block with golem-feet-to-center distance² **<4**. Matching-color order then marker-list order; ordinary color−1 means unrestricted. Aim at marker block center, **not its face or adjacent air**. |

- Both goals have mutex3/priority2; HomePlace/EmptyPlace priority1 precedes them. Gather2 has **no HomeDrop goal** (E:269–290). A full/sided-refusing inventory is still an inventory: never convert insertion failure into a world throw.
- Empty preflight accepts a non-inventory marker before home withdrawal; Goto first considers inventory destinations, then non-inventory markers in list order. That fallback has **no inventory-range16 guard** (H:515–541; `AIEmptyGoto:49–90`). Original `cc == home` is a reference comparison, ineffective for H's newly allocated coordinates—not a value-based home exclusion. Native markers use `dimension()/pos()/face()`; skip other dimensions without deleting them.
- Neither throw goal itself checks `golemDelay`, doTileDrops or mobGriefing. Each starts count200, but this is **not one throw per200 ticks**: clearing cargo terminates its continuation. After throwing, start action6/status4 if idle and update carried display (E:1010–1018). No owner/thrower, no-gravity, invulnerability or special lifetime flag is set.

## Exact physical output

For live feet G=(x,y,z), actual golem height h, and target block center T:

```text
spawn P = (x, y + (float h / 2.0F), z)
d = sqrt((T.x-x)^2 + (T.y-y)^2 + (T.z-z)^2)   // feet, not P
motion = ((T.x-x)*(d/3), 0.1+(T.y-P.y)*(d/3), (T.z-z)*(d/3))
```

One `EntityItem` contains a copy of the **entire actual carried stack**, not one item, wanted quantity, or repeated pellets. Set pickup delay **10**; there is no normalized direction, velocity clamp or scatter. Home face only determines B; marker face never offsets the aim. Native equivalent: `new ItemEntity(level,P.x,P.y,P.z,physical,v.x,v.y,v.z)`, `setPickUpDelay(10)`, normal physics. Sources: `AIHomeDrop:78–88`, `AIEmptyDrop:66–78`. Original spawn return is ignored before cargo is cleared; native delivery must not repeat that loss defect.

E's death/recall `dropStuff:902–909` instead calls generic entity-drop with vertical offset0.5. `InventoryUtils.java:354–394` uses inventory scatter/default entity motion; `:445–454` is chance/doTileDrops harvest output. None is directed-throw parity.

## Minimal shared queue / root delta

Proposed single public overload in `common/lib/utils/DeferredItemDrops.java`:

```java
public static boolean enqueueGolemThrow(ServerLevel level, EntityGolemBase golem,
                                       BlockPos target);
```

- Called inside the existing worker cargo/busy guard for a freshly validated Fill/Empty throw intent. Read **actual current cargo and live geometry**, not configuredData, display stacks, or caller-supplied count/motion/stack grants. Validate level/thread/live entity and target again after any preparation; translate native home representation once, never subtract F twice.
- `true` means one Pending owns the whole paid cargo **and that exact cargo was detached**; not “already spawned.” Prepare first, append before raw cargo clear, finish both without callbacks/flush between them; feedback/display follows. Failure before publication leaves cargo unchanged; after handoff never refund, re-extract or re-enqueue it. This is a local handoff through the existing guarded cargo path, not another journal/token framework.
- Reuse existing Pending with source=`golem.blockPosition()`, P/motion above, full components/count, pickupDelay10, retryAfter=`Long.MIN_VALUE`, crystalFace−1/routing=false/bore=null; set deliveryRequested/dirty. Existing `enqueue:411–433` scatters; `enqueueBlockHarvest:439–455` randomizes position/motion. **Do not call either as a throw approximation.**
- Pending already has all fields (`:74–83`); flush already chooses the explicit-velocity native ItemEntity constructor for positive pickupDelay (`:516–527`). Existing level-post delivery/retry (`:430–433,457–575`) and save/load (`:659–698,804–810`) retain position, motion, count and delay10. Reuse that path unchanged: pickup delay starts on entity delivery, not while queued; retries use the paid stored trajectory. **No version bump, new journal/tracker or new scheduling hook.**
- Runtime/root then add only throw eligibility, Empty preflight/Goto and final cargo handoff branches. First safety bound: **known loaded non-container, non-BE blocks/air only**, with world bounds/border checks. Reject container-provider blocks even without a BE; missing/unsupported BE, unknown inventory/provider or unloaded target is refusal, never “empty world.” This deliberately narrows original acceptance of non-inventory tiles. Recheck before withdrawal and throw; if invalid, retain/return cargo. Any extra16-block clamp must also be disclosed as a deviation.

No Data/Bell schema, recipes or new input are needed. Existing initial-selection versus already-selected-worker Bell permission roles remain root-owned. Intentional throws, native delivery integration and corresponding scope-tooltip changes remain next-unit work after the first container compile; no gameplay/visual result is claimed here.
