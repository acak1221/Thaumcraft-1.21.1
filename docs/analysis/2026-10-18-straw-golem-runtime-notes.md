# Straw + Gather common runtime — 2026-10-18 content slice

Source freeze: 2026-10-19. **Implemented, not compiled or tested by this author.**
Root owns integration and the subsequent actual Fabric compile. No gameplay,
visual, survival, export acceptance, full M10, or NeoForge parity is asserted.

## Owned files / integration contract

New production files only, all in `thaumcraft.common.entities.golems`:

- `EntityGolemBase.java`: native `AbstractGolem` living worker, configuration,
  actual cargo, interactions/recall, repair, safe return, persistence/sync.
- `GatherGolemGoals.java`: native pickup, return, swelling-creeper avoidance,
  swim/door/watch/idle goals and the five-server-tick work cadence.
- `GolemHomeInventory.java`: finite native home-container resolution; no generic
  endpoint/provider registry or new stock journal.

No existing files, registry/items/data/menu/client files, worlds, tests, Python,
smoke infrastructure, shared Bore code, or `DeferredItemDrops` were edited.
Other authors' model/item/data/bell WIP was not read. No agents were spawned.

Entity API:

```text
EntityGolemBase(EntityType<? extends EntityGolemBase>, Level)
static AttributeSupplier.Builder createAttributes()
GolemData configuredData()                       // copied configuration snapshot
void applyConfiguration(GolemData)                // fresh, unadmitted entity only
void setHome(BlockPos, Direction)
BlockPos getHomePosition()
Direction getHomeFacing()
void setOwnerName(String) / String getOwnerName()
int getGolemCore()
ItemStack getCarriedItem()                        // snapshot, not actual ownership
float getBootupTimer()
int getHealingTimer()
int getActionTimer() / getRightArmTimer() / getLeftArmTimer() // 0 for Gather
boolean canConfigure(Player)                     // alive/current/same level/<=8 blocks
ItemStack getFilter(int) / void setFilter(int, ItemStack)
void startConfiguration(Player) / void stopConfiguration(Player)
boolean recall(Player, boolean dismantle)
```

Dependencies are the agreed root-owned `ThaumcraftGolems` suppliers and item-owned
`thaumcraft.common.items.golems.GolemData` / `ItemGolemBell.select(...)`.
Root additionally owns the `ItemEntity.pickupDelay` access-widener entry and empty
`loot_table/entities/golem.json`; neither was read or edited by this author.
The entity calls **the original menu package**:
`thaumcraft.common.entities.golems.ContainerGolem.open(ServerPlayer, EntityGolemBase)`.
Root's menu must balance `startConfiguration`/`stopConfiguration` for each actual
viewer, including replacement/close. The per-golem opener set supports multiple
viewers; no global/entity `NoAI` switch is changed. There is no owner-only lock.

## Implemented behavior and original anchors

Authority: final `docs/analysis/2026-10-18-straw-golem-original.md` and the private,
read-only `../work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/` sources below.
Native mapping documentation was consulted for API signatures, not gameplay.

- `entities/golems/EnumGolemType.java:10,43-51` and
  `EntityGolemBase.java:166-185,235-264,355-383`: Straw material 0, health 10,
  armor 0, strength 0, carry 1, one original upgrade capacity (upgrade content
  remains unsupported), persistent living golem, step height 1, ordinary fire
  behavior. Root owns the entity type's .4 × .95 dimensions. Attack attribute 2
  is the original base `2 + strength`, but there are **no combat goals**.
- Movement deliberately maps material speed to native `MOVEMENT_SPEED=.38`
  with navigation modifier **1**, not `.38*.38`, and stores home restriction 32
  while work range remains 16. This is native composition, not a claim of exact
  old/new per-tick movement equivalence. Navigation floats, avoids water with a
  native malus, enters doors, and opens native wooden doors only with Gather.
- `EntityGolemBase.java:392-401,970-989,1035-1057`: fresh core -1 has no goals.
  Non-sneaking Gather attachment installs core 2 before consuming one actual
  core (creative exempt), rebuilds goals, sets bootup 33, sends event 7 and
  `thaumcraft:upgrade` at .5/1. Blank core 100 never installs/activates it.
- `ai/inventory/AIItemPickup.java:34-99`: scan home-cell AABB expanded by 16 and
  home-center sphere <=256 every five server ticks; choose the eligible entity
  nearest the golem. Strict native item/components matching, six empty ghosts
  accept all, pickup delay **<5**, no dropped-item owner restriction. Native
  navigation has a 200-tick pursuit budget; actual pickup requires distance²<=2.
  Straw takes only its one-item free capacity, retaining the rest in the same
  item entity. Pop sound .2, pitch `((rand-rand)*.7+1)*2`.
- `ai/inventory/AIHomePlace.java:35-62,85-119` and
  `lib/utils/InventoryUtils.java:49-159,260-265`: carrying + five-tick cadence +
  navigation done + home-center distance²<=5. Deposit destination is
  `home.relative(homeFacing.getOpposite())`, insertion side is `homeFacing`.
  Merge occupied slots before empty slots, ascending native slot order, strict
  components, native item/container maximum capped at 64, and native
  `WorldlyContainer` exposure/placement permissions. Missing, inaccessible,
  unloaded, unsupported or full destinations retain the unsatisfied cargo.
- Supported destinations are exact native chest/trapped-chest, barrel, hopper,
  furnace, blast furnace and smoker BE classes, plus final `TileChestHungry`.
  Native hopper lookup supplies double-chest composition with no blocked-lid
  opening rule. Connected chest chunks are checked before lookup. Unknown
  custom `Container` implementations/providers are not treated as destinations.
- `ai/misc/AIReturnHome.java:40-93`: return threshold distance²>=3, 20-tick
  attempts, stalled local detour, failed-path backoff 5 increasing by 5 to 50.
  `ai/combat/AIAvoidCreeperSwell.java:40-98`: visible swelling creeper within
  horizontal 5 / vertical 3, native escape path plus evasive jump, relative
  navigation modifiers 1.25/1.125. Watch-player and look-idle remain native goals.
- `EntityGolemBase.java:408-453`: repair counter decrements while positive;
  otherwise resets to 75 and heals 1 if damaged, with event 5/healing timer 5.
  Steady recurrence is 76 ticks. Counter resets on load as in the original.
  Only far-from-home distance²>=2304 against the original integer home anchor,
  or being in a solid block, permits safe-return relocation. Candidates are only
  the original 3×3×3 home neighborhood, with loaded chunks, sturdy support,
  actual native bounds, world border/build height, no liquid, collision or
  other live entity overlap. Ordinary item gathering never teleports.
- `EntityGolemBase.java:935-1008` / original `ContainerGolem.java:32-58`:
  wand/sneaking use passes; bell right-click calls item-owned selection; normal
  supported Gather interaction opens the root-owned configuration menu. Cookie
  consumes one as the original inventory-consume call did (including creative),
  heals 5 and uses native crafting-remainder handling, heart/eat feedback; there
  is no fuel. Owner name is attribution, not authorization. Standard interaction
  reach/world/item-use permissions are checked without an online-player gate.
- Original `ItemGolemBell.java:284-335`: only direct native `PLAYER_ATTACK`
  damage from a bell-holding player is intercepted before ordinary damage;
  projectiles, thorns/other sources do not become recalls. Failed/refused bell
  recall does not apply damage. Normal recall queues a configured placer with
  native custom name; sneak recall queues a bare placer and separate Gather
  core if fitted. Actual cargo is a separate queued stack in either case.
- `EntityGolemBase.java:581-704,902-910`: native entity persistence keeps home,
  facing, material/core, owner, complete native cargo components and six ghost
  exemplars under practical original keys (`Core` and `GolemCore` are aliases).
  Native super persistence owns living health/custom name. Synched entity data
  supplies core, owner/home, cargo display, filters, boot/healing; the latter
  animation snapshots additionally persist. Ghosts are neither equipment nor
  an entity `Container`, and never become real death/recall stock. Ordinary
  death queues only real cargo, never a replacement placer/core.

## Ownership boundaries / bounded modern adaptations

- Pickup uses the actual server-registered physical entity and existing
  `DeferredItemDrops.allowPlayerTouch` admission fence. Source debit and real
  cargo writes precede item setter/removal/sync/sound callbacks. Native splits
  pay both accepted units and the same-entity remainder; no replacement output
  entity or copied stock resource is created. A local busy guard blocks duplicate
  reentrant pickup/deposit/configuration/recall on this golem.
- Deposit rechecks live destination BE/state and physical source/slot identity
  before payment. Actual cargo is debited before known native slot setters or
  dirty notifications. Notifications cannot spend the old cargo snapshot again.
  No post-success stock-snapshot rollback is attempted after native callbacks
  may legitimately have moved/dropped accepted stock.
- Recall/death reuse only existing `DeferredItemDrops.enqueue`, before clearing
  cargo or discarding the entity. Rejected native delivery remains queue-owned.
  A death enqueue failure retains cargo on the native dead entity and retries
  before its ordinary removal; no parallel custody journal exists here.
  This is **not** a full power-loss atomicity or arbitrary addon-callback proof.
- The root-owned access widener exposes the native `ItemEntity.pickupDelay`
  integer. Runtime reads that field directly to preserve `<5`, with no NBT
  snapshot workaround, fully-zero approximation, reflection, Unsafe or new mixin.
- The native pathfinder's surrounding X/Z region is conservatively checked for
  loaded chunks (48-block margin) before path requests. This can postpone work
  near the loaded-region boundary rather than force chunks into existence.
- Unsupported materials/cores, advanced/upgrades/decorations/color/toggle/marker
  state or extra/malformed ghost entries freeze work and refuse flattening recall.
  Original configuration tags and real cargo are retained. Unparseable encoded
  cargo is retained as native entity NBT, not converted into an empty drop or
  placer. No unsupported registered variants were introduced.

## Deferred / verification boundary

No Gradle, Java, game, world, tests or smoke invocation occurred. New Java files
received whitespace-only `git diff --no-index --check` inspection without
diagnostics (exit 1 is the new-file difference), not an executable content test.
Root still must compile the actual integrated Fabric sources. All runtime,
save/reload, menu/hopper interaction, failure-path and client visual checks remain
unverified; there is no new QA framework or test-source stub.

Optional original five-tick home-chest open/close animation is explicitly deferred:
no fake player/opener is added. Native wooden doors are supported; original
fence-gate-specific door handling and client camera-tick boot sound are not ported
here. The original cookie's client-only attempted speed effect is not promoted
into a new server buff. Other materials, advanced/upgrade/decor content, all other
cores, fuzzy/color/marker routing, fetters and broad addon compatibility remain
outside this first functional Straw + Gather slice.

Frozen production SHA-256:

```text
65f2a4bedefd167e5a3ed963e5813a366d4ecf024a858ac5f3e213b7d3ad9b68  common/src/main/java/thaumcraft/common/entities/golems/EntityGolemBase.java
3c17ec124677ae5fa63a3f871480227462d2082d1b12a3f432de786ad162c165  common/src/main/java/thaumcraft/common/entities/golems/GatherGolemGoals.java
c73c998ed1a8d753d6c802a7a4fd7845c3982f5a8bdd81fd3448c031e0c46404  common/src/main/java/thaumcraft/common/entities/golems/GolemHomeInventory.java
```
