# Axe of the Stream — implementation agreement

Authority: [original contract](2026-10-19-axe-stream.md),
[Lumber traversal](2026-10-19-golem-lumber.md). Actual Use/Flesh build PASS33s
and15 data tests precede this unit; no axe gameplay/visual acceptance.

## Non-overlapping owners

- Item/motion author: new `ItemElementalAxe`, new `StreamAxeItemMotion`, one new
  `StreamAxeItemMixin`, notes. No registry/resources/other mixins/harvest/Entity.
- Chop author: new `StreamAxeHarvest`, new `TreeLogTraversal`, existing
  `ExcavationServerPlayerGameModeMixin` composition, notes. No item/motion WIP.
- Root: equipment tier/registry, recipe/item tags/model/book/lang, mixinJSON/build.
  Lumber runtime is separate and follows the genuine usable axe, not a placeholder.

```java
// common/items/equipment:
public ItemElementalAxe(Tier tier, Item.Properties properties);

// common/lib/world, item/motion-owned:
public static void StreamAxeItemMotion.vacuum(ServerPlayer player);
public static void StreamAxeItemMotion.followNewDrops(ServerPlayer player, BlockPos endpoint);

// common/lib/world, chop-owned, finite classic/Greatwood/ordinary Silverwood:
public static boolean TreeLogTraversal.isLog(ServerLevel level, BlockPos pos, BlockState state);
public static BlockPos TreeLogTraversal.endpoint(ServerLevel level, BlockPos anchor, BlockState expected);
// null is refusal/stale/unloaded, not permission to break a different fallback cell.
```

Chop author chooses the smallest exact invocation scope inside the **existing**
destroyBlock wrapper/redirects. All ordinary player/Excavation behavior must
remain unchanged when no actual elemental axe log attempt is active. No competing
removeBlock redirect or whole native game-mode clone. Real player's endpoint
destroyBlock runs genuine BREAK/BEFORE/AFTER; recheck actual state, loaded/border/
home-independent endpoint permissions and original held axe after callbacks.
Anchor remains original player reach/ray target; never teleport player to branch.

Non-sneaking recognized log claims the anchor even on refusal: no fallback break.
Greedy ordered same-block endpoint, one actual native removal/attempt, real
native loot with held components. Witness removeBlock, not optimistic destroyBlock
boolean or a subsequent AIR guess. Durability exactly once on committed removal,
creative free, Unbreaking native; no ordinary mineBlock plus duplicate manual wear.
Indeterminate delegate failure never invents loot/refund/retry. Post-commit item
feedback/following must not repeat a removal. Follow age0 drops only after paid
native loot, not every old nearby item.

Motion keeps actual existing ItemEntity stacks/owner/pickup delay, retargets in
place instead of unchecked spawn/copy/discard. One transient per-item target,
no global strong-reference cache, save/replay journal or arbitrary destination.
Already-following items are never stolen; invalid/dead/cross-world targets
release noClip without teleport or inventory insertion. Native lifetime/pickup/
merge/deferred-drop ownership hooks stay effective. Declared narrowed fire/FX/
sync parity is preferable to fake full FollowingItem compatibility.

Vacuum is held-use AABB±10, normalized pull.3, each component clamp±.35, original
Y=itemY−playerY+height/2, no vis/wear/item cost and no added Shift guard.
Item is real rare elemental axe, BOW72000 use and no modern strip/scrape branch;
ordinary sneak/non-log chopping retains normal axe behavior. Optional Repair
enchantment is not implemented or advertised as free regeneration.

Root tier:1500uses/speed10/damageBonus3/enchant18/diamond incorrect-block tag,
actual thaumium repair ingredient. Attribute+6 attack modifier (player total7),
modern axe attack speed−3 is declared adaptation. Genuine researched infusion
requires pristine thaumium axe +2water shards/diamond/Greatwood log, aqua16/arbor8,
instability1. No builtin enchantment or inherited central name/components.
