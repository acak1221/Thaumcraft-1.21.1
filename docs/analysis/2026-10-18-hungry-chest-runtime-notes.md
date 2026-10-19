# Hungry Chest common content — source freeze, 2026-10-18

Status: **implemented, not compiled or runtime-tested**. This is the first Hungry
Chest content unit for the W4M09 → M10 prerequisite, not an accepted checkpoint,
visual/survival result, or NeoForge parity claim.

## Owned paths and integration API

Only these new production files were authored in this task:

- `common/src/main/java/thaumcraft/common/blocks/BlockChestHungry.java`
- `common/src/main/java/thaumcraft/common/tiles/TileChestHungry.java`

Registration remains root-owned in `ThaumcraftHungryChest`:
`HUNGRY_CHEST`, `HUNGRY_CHEST_ITEM`, and `HUNGRY_CHEST_BLOCK_ENTITY`.
The separately authored registration was read, not edited. No registry,
bootstrap, renderer, assets, language, recipe, Bore, observer, client-test,
Python, or smoke files were changed. No helper/test framework was added.

Client-facing common API:

- `BlockChestHungry.FACING`: native six-direction `BlockStateProperties.FACING`.
- `TileChestHungry(BlockPos, BlockState)`.
- `static TileChestHungry.tick(Level, BlockPos, BlockState, TileChestHungry)`:
  ticker on **both** logical sides, with server-only audio broadcasts.
- `float getOpenNess(float partialTick)`: linear previous/current lid
  interpolation. Renderer applies the original cubic `1 - (1 - open)^3` easing.
- `int getNumUsingPlayers()` for the actual opener count.
- Native `Container`/`MenuProvider`; `createMenu` uses
  `ChestMenu.threeRows(id, inventory, this)` / vanilla `GENERIC_9x3`.
- `eatItem(ItemEntity)` is the contact entrypoint; `evacuate()` is used by block
  replacement. Neither needs a renderer or other client class.

## Gameplay/source anchors

Read-only authority is
`../work/thaumcraft-4.2.3.5-oracle/thaumcraft/common/`:

| Original file/lines | Implemented behavior |
| --- | --- |
| `blocks/BlockChestHungry.java:59-63,76-98` | Wood, hardness 2.5, non-full geometry, native inventory comparator fullness. |
| `blocks/BlockChestHungry.java:100-104` | Six facing states, including vertical metadata equivalents. Placement currently uses the requested modern nearest-looking-direction opposite; default is DOWN (legacy metadata 0). |
| `blocks/BlockChestHungry.java:106-133` | Removal evacuates actual stored items, not a display/loot preview. See durable-path adaptation below. |
| `blocks/BlockChestHungry.java:136-143` | Outline X/Z 1/16–15/16 and height 14/16; collision height 15/16. |
| `blocks/BlockChestHungry.java:145-165` | Server-only native `entityInside`, live physical `ItemEntity`, no radius/attraction scan, no pickup-delay or thrower/owner check. Successful eat volume .25, pitch `(randomFloat-randomFloat)*.2+1`, event 2/parameter 2. |
| `blocks/BlockChestHungry.java:168-177` | Single chest menu; no adjacency/double-chest or blocked-lid rule. |
| `tiles/TileChestHungry.java:27,33-35,74-100` | 27 usable slots, limit 64, all real slot contents saved/loaded. |
| `tiles/TileChestHungry.java:103-105,175-176` | Actual block-entity identity, original squared-distance limit 64, public unrestricted valid inventory slots. |
| `tiles/TileChestHungry.java:111-164` | Previous/current lid, .1 per tick, open 1/closed 0, open sound at start and close sound crossing below .5; sounds volume .5 and pitch `random*.1+.9`. Event 1 is opener count; event 2 sets minimum lid angle `parameter/10`. |
| `lib/utils/InventoryUtils.java:49-60,80-102,124-159` | Merge matching occupied slots before filling empty slots; strict native item/components comparison and actual native item maximum capped at 64. |

The original intake has **no pickup-delay check**. Modern vanilla hopper pickup
delay semantics were deliberately not copied into the chest's contact intake.

## Ownership, persistence, and bounded adaptations

- Inventory is a native unsided 27-slot `Container`, so adjacent vanilla hoppers
  can insert/extract through their existing container path. No new Fabric
  storage/provider layer was added.
- Admission requires the live entity actually registered in the same server
  level and intersecting the chest's block cell. The native contact callback is
  the sole runtime caller; the ticker never searches for or attracts items.
- The existing `DeferredItemDrops.allowPlayerTouch` admission fence is reused.
  An unadmitted queue candidate cannot supply stock. Settled physical drops
  remain ordinary entities; no Bore journal/reservation is interpreted as stock.
- Intake plans capacities without slot writes, then rechecks live source and
  slot identities/counts. Paid stock is split from the actual entity-held stack
  and fully committed to actual slots before entity setters/removal,
  `setChanged`, block events, or sounds. A same-chest reentry guard covers the
  entire operation. No new output entity or unbacked copied stock is created.
- A partial remainder is also split from the live source and installed on the
  **same** entity, preserving UUID, motion, age, pickup delay, and owner. Its old
  stack object is already empty before setter observers run. Full transfer
  discards only after all accepted units have real inventory ownership. A full
  chest does not change the physical entity/stack or emit eat effects.
- `ContainerHelper.saveAllItems/loadAllItems` with the 1.21.1 registry lookup
  retains ordinary slot counts and complete native components. No load-time
  count clamp or custom item-serialization substitute was added.
- Original initial allocation was 36 but public capacity and post-load
  allocation were 27. Fresh modern worlds allocate/persist only the 27 usable
  slots. Empty-slot intake also honors the native item's maximum instead of
  accepting an illegal low-max overstack up to 64.
- Invalid slot operations are harmless. Out-of-contract oversized `setItem`
  values are refused unchanged, not silently truncated; native menus/hoppers
  supply valid bounded stacks. Physical overstack intake is split across actual
  available slots, with the unsatisfied remainder left in the original entity.
- Opening checks actual level/block entity, player reach, native world
  permission and item-use permission. There is no owner, research, online-player,
  or new session gate. Standard native spectator menu access remains available;
  spectators do not affect the lid opener count. Title is
  `block.thaumcraft.hungry_chest` (legacy Hungry Chest name).
- Opener/lid state is transient, as in TC4. Initial client chunk tags additionally
  contain the current transient opener/lid snapshot for newly tracking clients;
  ordinary disk inventory saves do not persist that animation state.
- Removal uses **existing** `DeferredItemDrops.enqueue` before clearing slots,
  like the nearby alchemy furnace. There are no synchronous spawns/comparator
  callbacks inside block removal, no new recovery journal, and no lost stock
  from a rejected `addFreshEntity`. Deferred delivery keeps the existing
  production drop adapter's scatter/stack segmentation rather than introducing
  a separate implementation of the original random 10–30-item breakup.
- The block item's normal drop remains the root-owned vanilla loot table's
  responsibility; this content code does not manually drop a second chest.

## Verification boundary / remaining work

- No Gradle, Java, game, world, smoke, or NeoForge invocation was made.
- No executable content test ran. No source-text stub test was substituted.
- Both new Java files received a whitespace-only `git diff --no-index --check`
  inspection (no diagnostics; exit 1 denotes the new-file difference).
- Root still owns the focused final Fabric compile after all authors are ready.
  Native save/load, hopper/menu operation, partial/full/reentrant intake,
  canceled-spawn deferred evacuation, and client visuals remain unverified.
- The requested six-direction modern placement expression is present, but
  equivalence to the vintage piston eye-height algorithm and original renderer's
  horizontal metadata mapping is **not** asserted; independent source/render
  review remains with root/reviewer.

Frozen production SHA-256:

```text
aa1247bb230bed7ac725ea2bf05084b47f527ea8480c4c187d2df740baffe416  common/src/main/java/thaumcraft/common/blocks/BlockChestHungry.java
363bdd0f41f6e23801079f21625d292952c441399871ea32eafa20dc0bb339c0  common/src/main/java/thaumcraft/common/tiles/TileChestHungry.java
```
