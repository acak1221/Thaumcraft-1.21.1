# Use8 — first native unit agreement (preparation)

Authority: [original contract](2026-10-19-golem-use.md),
[dedicated options](2026-10-19-golem-use-data.md). Current Butcher build is not
Use implementation evidence. Core8 remains unregistered until the real unit.

## Useful bounded actions, not an entity mode

DEFAULT is block/right/not-sneaking. Without an attached inventory and with
actually empty cargo/default empty ghosts: real vanilla lever/button use.
With a supported attached home: take real WHEAT_SEEDS up to material carry,
walk to marked FARMLAND/UP with air above, plant through native player pipeline,
consume only actual seed; return unused/replaced stock with partial acceptance.
An empty existing home inventory never grants bare-hand mode. No ghost supplies,
world scan, marked-container source, fake tool, sheep use, hostile/player attack,
ignition, container GUI or sustained mining in this first unit.

Other ordinary options are source-authentic but pending. Server refuses toggling
Empty-space/Left/Sneaking away from DEFAULT; client explains “not yet ported,”
not “upgrade required.” Nondefault loaded/placer options retain/refuse rather
than normalize or authorize an unsupported action. Six ghosts have wanted0/1,
no Fill mode. UseOptions is the sixth configuration member, not stock.

## Small real ownership boundary

Runtime author owns EntityGolemBase +new UseGolemGoals and actual carried stock.
Actor author owns one finite GolemUsePlatform +Fabric hook integration (after the
native-route note specifies exactly-once UseBlockCallback coverage). Root owns
Data/menu/client/recipe/core/placer validation and actual builds.

The worker's private busy action binds exact marker/face, current clicked and
affected states, actual cargo, mode, home and native identity. Public root entry
cannot perform a click without this active real worker intent. Same-level loaded
home/target/adjacent cells, sight/reach and permission are rechecked after actual
effectful BEFORE callbacks. Marker order and selected face remain authoritative.

Only a canonical unspawned non-OP survival actor with strictly **empty inventory**
may be borrowed; foreign gear is refused, never cleared. Use a shared actor-busy
fence so another golem/harvest query cannot borrow it even for an empty hand.
Move the real worker cargo reference to its hand, never a preview copy; the worker
no longer exposes it as spendable while the native call owns it.

Invoke the supported actual native use route once. PASS/FAIL/throw can follow a
real world mutation: reconcile actual post-call hand and every new inventory
remainder in finally, not pre-call counts. Transfer those owners back to worker
**before** restoring actor state/releasing lifecycle guards. Additional paid
extras use existing DeferredItemDrops; failed publication retains a bounded
worker-owned remainder list and blocks new clicks, never replays the world use.
Snapshot/restore actor pose/sneak/selected/menu state; no broad borrowed player,
OP, fake seeds, inventory wiping, new provider/journal or callback-plan engine.

Native removal requested during a borrow finishes once after reconciliation;
dead/retiring workers evacuate returned actual items before terminal handling.
An in-flight actor borrow is transient, not a saved/replayed half-click. Native
save during that exact call must not serialize an apparently empty paid owner;
no power-loss/arbitrary-addon-callback atomicity promise is made.

## Frozen runtime / actor bridges after Butcher build

```java
// Entity's private live Use goal alone opens its busy/exact-marker attempt:
public boolean canUseMarkedBlock(GolemMarker marker, BlockState expectedTarget,
                                 BlockState expectedAdjacent);
public ItemStack detachUseCargo(); // exact actual reference, then raw EMPTY
public void restoreUseCargo(ItemStack actualHand, List<ItemStack> actualExtras);
public GolemUseOptions getUseOptions();
public boolean setUseOptions(GolemUseOptions options); // first unit only DEFAULT

// Finite root platform, no public item/plan/callback supplier:
public static boolean GolemUsePlatform.useBlock(EntityGolemBase worker,
    GolemMarker marker, BlockState expectedTarget, BlockState expectedAdjacent);
public static boolean GolemUsePlatform.isDetaching(EntityGolemBase worker);
public static boolean GolemUsePlatform.isReturning(EntityGolemBase worker);
public static boolean GolemUsePlatform.isBorrowing(EntityGolemBase worker);
public static boolean GolemUsePlatform.isActorBorrowed(ServerPlayer actor);
```

`detachUseCargo` requires exact platform detaching scope +private current job and
actual allowed cargo, raw-clears it, marks the borrowed phase, immediately returns
the real reference without sync/callback. `restoreUseCargo` requires exact returning
scope, validates everything/copies the extra-list structure BEFORE changing fields,
then raw-restores actual post-hand/extras and ends that borrowed phase with no
callback afterward. It must accept owned post-use items even if death/unsupported
state now prevents further work; native removal is deferred through reconciliation.
Root clears actor's corresponding raw slots without callbacks only after these
owners are installed. Neither method is a public credit/clone permission.

The private worker keeps a bounded pending-extra list separately from ordinary
carried stock; failed queue publication blocks new actions, preserves native NBT
and retries **publication only**. Recall/death/destructive removal must settle it
and returned actual cargo before terminal handling; unsupported raw payloads freeze.
Selection/input/return remain finite existing home-container routes, not another
inventory/provider/journal. No other core uses these actor methods.

The platform must revalidate actual actor selected hand/reference and native action
target after the real Fabric BEFORE hook, not accidentally execute a cached hand
against a replacement. If a tiny scoped native integration injection is required,
it only rejects this exact borrowed actor/attempt, never changes ordinary players
or double-dispatches UseBlockCallback. The native-route note determines the site.
Butcher actual build passed; next is actual core8 action/stock integration;
gameplay/protection/visual/save checks stay separately deferred, not passed.
