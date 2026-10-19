# Alchemy6 — native implementation agreement

Authority: [original contract](2026-10-19-golem-alchemy.md).
Decanting build PASS36s is earlier evidence, not Alchemy verification.

## Separate ownership / file allocation

- Runtime author: EntityGolemBase +new AlchemyGolemGoals, actual source/path/jar
  selection and real aspect cargo, persistence/normal recall.
- Transfer author: existing EssentiaTransfers +new GolemAspectCargo, two closed
  native BE/entity entry points, no new provider/executor/journal.
- Recipe author: InfusionComponentPredicate/InfusionStackRecipe typed pedestal
  requirements (water_potion/empty_jar), preserving damage and central retention.
- Root: Data/Core/registry/recipe/placer/book/client/platform query, actual build.
  Nobody reads or edits another author's WIP; build follows every FINAL.

```java
// New immutable real stock, NOT GolemData/display/filled-jar supplies:
public record GolemAspectCargo(Aspect aspect, int amount) {
  // EMPTY=(null,0);0..32; positive canonical Aspect; material item-carry capacity
  public static int capacityForMaterial(int material);
  public void validateMaterial(int material);
  public CompoundTag save();
  public static GolemAspectCargo parse(CompoundTag tag);
  public static GolemAspectCargo readPlacer(ItemStack stack);
  public void writeToPlacer(ItemStack stack);
}
// Exact compound {Version:1(int),Aspect:string,Amount:int}; empty uses Aspect="".
// Reserved CUSTOM_DATA sole AspectCargo key; unknown data is retained/refused.

// Entity public, immutable stock/synced display values:
GolemAspectCargo getAspectCargo();
Aspect getEssentiaAspect();
int getEssentiaAmount();
void applyAspectCargo(GolemAspectCargo stock); // fresh unadmitted, material/core6
boolean canCollectEssentia(TileAlembic source, Aspect aspect, int units);
boolean canDeliverEssentia(TileJar sink, Aspect aspect, int units);
void applyAspectCargoRaw(GolemAspectCargo before, GolemAspectCargo after);
void notifyGolemEssentiaChanged();

// Existing executor, finite native classes/actual guarded worker intent only:
EssentiaTransfers.Attempt collectAlembic(EntityGolemBase, TileAlembic,
                                       Direction output, Aspect, int limit);
EssentiaTransfers.Attempt deliverNormalJar(EntityGolemBase, TileJar,
                                         Aspect, int limit);
void EssentiaTransfers.requireGolemCargoChange(EntityGolemBase,
                                             GolemAspectCargo before,
                                             GolemAspectCargo after);
boolean EssentiaTransfers.isGolemMutationActive(EntityGolemBase);
```

`canCollect`/`canDeliver` only authorize inside private busy/live attempt guards,
after root's nonbreaking native/profile/spawn query on all actual endpoints.
They recheck home/marker/current BE/aspect/label/side/room/reach, not just snapshots.
Executor prepares both owners before mutation; raw BE update and exact prepared
Entity cargo replacement run without callbacks, then both notifications. Raw
Entity apply checks the executor's exact before/after objects and existing cargo
BEFORE assigning fields. Notification failure reports committed units, not refund.
No arbitrary callback/plan supplier or BE-casting fake entity endpoint.

`GolemHarvestPlatform.mayInteract(golem,pos)` root query will support core6, without
break/use callbacks/OP/online-player impersonation. Runtime's private paid attempt
holds this preflight; public executor calls alone cannot produce cargo. Native
removal requested during a held mutation must finish once when the entity scope
ends, not vanish behind a permanent early return. Unknown stock remains raw/frozen.

First sources: actual attached TileAlembic (respect homeFacing eligibility, takeUP),
or contiguous actual alembics1…5 above an attached native alchemy furnace, largest
compatible positive amount/highest on ties. First sinks: directly marked normal
warded jars; fresh label/type/space, original four normal-jar priority stages,
least home-distance within16. No void/addon/tube/reservoir/connected-jar promise.
Bulk take up to actual carry limit, tick2/earlyfailure20 cadence adaptation;
integer jar path200/+20 unstick, idle deposit within center²≤4, partial leftover.

Data core6 uses EMPTY6/wanted0/modefalse, no GUI/item routing/throw or upgrades.
Normal recall preserves separate AspectCargo in the one real placer before raw
clear after existing durable queue publication; positive stock refuses dismantle.
Known essentia death loss follows original for now; no fake filled jars/phials.
Placement material/core/custom-key validation moves actual stock once; callback
guard and all existing water/fishing/health/protected-payload policies remain.
