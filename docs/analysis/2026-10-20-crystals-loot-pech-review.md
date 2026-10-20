# Independent crystal/chest/Pech review

Read-only production review against the TC4 4.2.3.5 oracle,20October2026.
No source changes, Gradle execution, runtime pass or visual acceptance are
claimed by this review. The source owners are still completing the shared loot
and trade unit.

## Concrete finding: reversed dungeon-entry rejection fields

At the reviewed snapshot, `PechTrade.prepare` accepted a dungeon descriptor
when `minCount<=5 && weight<=1`. Original `ContainerPech.generateContents`
retries while `wc.field_76292_a>5 || wc.field_76296_e>1`.

The original1.7 mappings and vanilla bytecode establish:

| Original field | Actual meaning | Native descriptor |
| --- | --- | --- |
| inherited `WeightedRandom.Item.field_76292_a` | item weight | `weight` |
| `WeightedRandomChestContent.field_76295_d` | minimum count | `minCount` |
| `WeightedRandomChestContent.field_76296_e` | maximum count | `maxCount` |

Therefore the acceptance predicate is **weight<=5 && maxCount<=1**. Preserve
the original unconditional acceptance of the fiftieth nonempty descriptor.
For example, golden horse armor(weight2,max1) must be accepted immediately;
a common loot bag(weight5,max3) must be rejected until the unconditional
fiftieth draw. The wrong predicate changes both reward availability and the
world RNG position used by subsequent trade rounds.

This was sent directly to the Pech source owner and root for correction. This
document records the reviewed defect and its exact contract; it does not claim
that the owner's later patch has compiled or passed. A meaningful regression
should distinguish both examples with scripted descriptor indices and verify
the number/order of rejection draws, including the fiftieth fallback.

## Other reviewed contracts

The native crystal group preserves original metadata0..6 through six retained
primal identities and one mixed identity, six face orientations, an inert tile
with SHORT orientation(default constructor1, absent saved key0),7light,
.7hardness and actual.6explosion resistance. Its full collision cube with
empty support shape matches the original nonopaque/nonnormal support behavior.
The loaded loot tables contain six independent shard entries, no Fortune or
Silk condition; mixed metadata6 supplies one of each primal. The renderer's
private positional random schedule, six-piece geometry, orientation transforms,
mixed order and RGB values agree with original TileCrystalRenderer. Actual
renderer/item hooks and physical client visibility still require their owner's
checks; this is not a client acceptance result.

`ThaumcraftLoot.chestAdditions` has the original Config.initLoot common,
uncommon, rare and blacksmith count/weight groups, including the extra library
fragment row and shared copied lesser-amulet charge template. The Fabric
adapter appends once to a modern chest's primary weighted pool and preserves
that pool's native roll/function policy. Mapping the old single blacksmith
category to native smith chest identities is an explicit modern-world adapter,
not an exact old structure-generation claim.

The canonical15 vanilla dungeon descriptors were checked against the1.7
WorldGenDungeons initializer. Forge1.7 ChestGenHooks.getItems calls each item's
getChestGenBase in list order; Item's enchanted-book branch regenerates the
book on every invocation before Pech uniformly samples descriptor entries.
The native dungeon descriptor adapter preserves that contract, rather than
substituting a weighted modern dungeon loot roll. Final enchantment algorithm
parity belongs to the separately owned OriginalLootEnchantments group.

Pech's staged planner otherwise preserves the untame draw, value adjustment,
pack-choice short-circuit draw order, one-item pack extraction, uniform direct
table selection with tier rejection, duplicate Silverwood entries, native
candle0..14 entries, and dungeon fiftieth fallback. Native output overflow is
an unpaid refusal rather than the original discarded merge remainder. Full
paid material commit precedes onCraftedBy and sound callbacks. The ManaBean
initializer uses its item-local RNG on the private staged reward; the later
actual item callback sees initialized contents and does not reroll it.

## Remaining unverified

LootBag production/runtime, OriginalLootEnchantments, potion loot tables and
source-owner regression tests were still in progress at this review boundary.
No absence during that coordinated window is reported as an additional defect.
Actual server compilation, full native runtime cases, persisted deferred Pech
close drops and client crystal visual inspection remain centralized acceptance
work. Scope-ledger absence alone cannot establish missing native gameplay:
Portable Hole already has a working native implementation despite stale empty
counterpart mappings, so no duplicate implementation was started.
