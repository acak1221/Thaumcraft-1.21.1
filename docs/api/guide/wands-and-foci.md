# Wands, wand parts and foci

## Units first

A wand stores vis in **centivis** (hundredths of vis, as 1.7.10 stored it). A rod's capacity, an arcane recipe's cost
and `WandHelper.addVis` are in **whole vis**; a focus cost is in **centivis**. Mixing them up makes a focus a hundred
times too cheap or a rod a hundred times too small.

## Rods, staff cores and caps

`new WandRod(...)`, `new StaffRod(...)` and `new WandCap(...)` are the original constructors and register the part:

<!-- snippet:wand-parts -->
```java
// A wand rod and a wand cap, the 1.7.10 constructors: capacity in whole vis, the craft cost of the part, and an
// entity-style texture with the UV layout of the original ModelWand (a 1.7.10 PNG works unchanged). The arcane
// workbench builds wands, staves and sceptres from the part items once ROD_<tag> and CAP_<tag> are researched.
IWandRodOnUpdate regrowth = (wand, player) -> {
    // Server, every tick, for each wand with this rod in the player's inventory: one Ordo every five seconds
    // up to a tenth of the capacity. getVis/getMaxVis are centivis, addVis takes whole vis.
    if (player.tickCount % 100 == 0 && WandHelper.getVis(wand, Aspect.ORDER) < WandHelper.getMaxVis(wand) / 10) {
        WandHelper.addVis(wand, Aspect.ORDER, 1, true);
    }
};
new WandRod("amethyst", 80, new ItemStack(AMETHYST_ROD), 4, regrowth,
        ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_rod_quartz.png"));
WandCap cap = new WandCap("amethyst", 0.95F, java.util.List.of(Aspect.ORDER, Aspect.ENTROPY), 0.85F, new ItemStack(AMETHYST_CAP), 3);
cap.setTexture(ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/wand_cap_thaumium.png"));
new ResearchItem("ROD_amethyst", "EXAMPLE", new AspectList().add(Aspect.CRYSTAL, 3).add(Aspect.TOOL, 2), 0, 2, 1,
        new ItemStack(AMETHYST_ROD)).setParents("EXAMPLE_SHARDS").setSecondary()
        .setPages(new ResearchPage("tc4_example.research_page.ROD_amethyst.1")).registerResearchItem();
new ResearchItem("CAP_amethyst", "EXAMPLE", new AspectList().add(Aspect.CRYSTAL, 2).add(Aspect.ORDER, 2), 2, 2, 1,
        new ItemStack(AMETHYST_CAP)).setParents("EXAMPLE_SHARDS").setSecondary()
        .setPages(new ResearchPage("tc4_example.research_page.CAP_amethyst.1")).registerResearchItem();
```
<!-- /snippet -->

- **Rod:** tag, capacity in whole vis, the item that is the part, its craft cost, an optional `IWandRodOnUpdate`, and the
  texture. The texture is entity-style with the UV layout of the original `ModelWand`, so a 1.7.10 PNG works unchanged.
- **Staff core:** `StaffRod` is a rod registered under `<tag>_staff`; a wand built from it is a staff (the long model, +6
  attack damage, no arcane workbench slot). `setRunes(true)` adds the primal staff's runes and one focus potency.
- **Cap:** tag, the discount multiplier for every aspect, the special aspects and their multiplier, the item, its craft
  cost; `setTexture` sets its texture.
- The arcane workbench builds wands, staves and sceptres from the part items once `ROD_<tag>` and `CAP_<tag>` are
  researched; register those entries yourself, as in 1.7.10. The vis cost of the craft is rod cost × cap cost.
- The wand's name is built from `item.Wand.<tag>.rod` and `item.Wand.<tag>.cap`.
- A part registered under a native tag never replaces the native part.

### `IWandRodOnUpdate`

The hook of an add-on rod runs on the **server**, once per tick, for every wand, staff or sceptre with that rod in a
player's inventory, with the live stack (original `ItemWandCasting.onUpdate`). Changing the stack's vis there is saved
and synchronised like any inventory change. The example's regrowth is the lambda at the top of the snippet above.

## The `WandHelper` facade

`thaumcraft.api.wands.WandHelper` is the original `ItemWandCasting` surface for code that only has the stack:

| Call | Unit / meaning |
|---|---|
| `isWand`, `isStaff`, `isSceptre` | what the stack is |
| `getRod`, `getCap`, `setRod`, `setCap`, `hasRunes` | the parts, as their API views (native parts included) |
| `getFocus`, `getFocusItem`, `setFocus` | the mounted focus |
| `getVis(stack, aspect)`, `getAllVis`, `getMaxVis`, `getAspectsWithRoom` | centivis |
| `addRealVis(stack, aspect, amount, doit)` | centivis; returns what did not fit |
| `addVis(stack, aspect, amount, doit)` | whole vis; returns what did not fit |
| `consumeVis(stack, player, aspect, amount, crafting)` | centivis, one aspect, discounted |
| `consumeAllVis(stack, player, aspects, doit, crafting)` | centivis, all or nothing, discounted |
| `consumeAllVisCrafting(stack, player, aspects, doit)` | whole vis, all or nothing (arcane crafting) |
| `getConsumptionModifier(stack, player, aspect, crafting)` | the multiplier a payment uses |
| `getFocusPotency`, `getFocusTreasure`, `getFocusFrugal`, `getFocusEnlarge`, `getFocusExtend` | upgrade levels of the mounted focus |

Only the six primal aspects are stored in a wand. Payment goes through the native discount rules: the cap, worn
`IVisDiscountGear`, the frugal upgrade (casting only) and vis exhaustion.

## Foci

Any subclass of `ItemFocusBasic` is a focus: it mounts on a wand (the change focus key, F by default, and the focus
pouch list it), it is drawn on the wand with its colour and ornament, and the wand calls its hooks.

<!-- snippet:focus -->
```java
public class SparkFocus extends ItemFocusBasic {
    /** Focus costs are centivis: half a vis of Ignis and a quarter of Aer per cast. */
    private static final AspectList COST = new AspectList().add(Aspect.FIRE, 50).add(Aspect.AIR, 25);

    public SparkFocus(Properties properties) { super(properties); } // stack size one and the rare rarity are set here

    @Override public AspectList getVisCost(ItemStack focus) { return COST.copy(); }
    @Override public int getFocusColor(ItemStack focus) { return 0xFF9A2E; }
    @Override public int getActivationCooldown(ItemStack focus) { return 500; } // milliseconds, as in 1.7.10

    /** The focal manipulator offers these at every rank; frugal is applied by the wand itself. */
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency};
    }

    @Override public ItemStack onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        double range = 16 + 4 * WandHelper.getFocusPotency(wand);
        HitResult hit = player.pick(range, 1.0F, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) return null; // not handled
        if (level.isClientSide) return wand; // both sides run this; the server pays and acts
        // All or nothing; the cost is discounted by the wand's cap, worn gear and the frugal upgrade.
        if (!WandHelper.consumeAllVis(wand, player, getVisCost(WandHelper.getFocusItem(wand)), true, false)) return null;
        ThaumcraftFX.zap((ServerLevel) level, player.getEyePosition().add(0, -0.2, 0), hit.getLocation());
        BlockPos fire = block.getBlockPos().relative(block.getDirection());
        if (level.isEmptyBlock(fire) && BaseFireBlock.canBePlacedAt(level, fire, block.getDirection())) {
            level.setBlockAndUpdate(fire, BaseFireBlock.getState(level, fire));
        }
        return wand;
    }
}
```
<!-- /snippet -->

| Method | Contract |
|---|---|
| `getVisCost(focus)` | centivis per cast (or per tick when `isVisCostPerTick` is true); shown in the tooltip |
| `onFocusRightClick(wand, level, player, hand)` | the cast, on **both** sides. Return the stack the hand holds afterwards (normally `wand`) when the focus acted, `null` to let the click pass. The focus pays for itself |
| `onUsingFocusTick(wand, player, count)` | every tick while the wand is held in use; call `player.startUsingItem(hand)` in `onFocusRightClick` to get there |
| `onPlayerStoppedUsingFocus(wand, level, player, count)` | when the player releases the use key |
| `getActivationCooldown(focus)` | milliseconds between casts |
| `getFocusColor(focus)` | the tint of the focus on the wand |
| `getOrnament(focus)`, `getFocusDepthLayerIcon(focus)` | full texture paths of the ornament and the depth layer, or `null` |
| `getPossibleUpgradesByRank(focus, rank)` | what the focal manipulator offers at rank 1–5 |
| `getUpgradeLevel(focus, type)`, `isUpgradedWith(focus, type)` | the applied upgrades |

`FocusUpgradeType` keeps the original upgrades (`potency`, `frugal`, `treasure`, `enlarge`, `extend`...) and their ids.

## Blocks that answer a wand: `IWandable`

A block, or its block entity, that implements `IWandable` answers a wand click before the wand's own behaviour, as in
1.7.10: the block is asked first, then its block entity.

- `onWandRightClick(level, wand, player, x, y, z, side, event)`: the click on the block, on both sides. A negative
  answer means "not handled" and the wand goes on; otherwise the click is handled and nothing else happens (1 swings the
  arm, any other value is a quiet refusal). `event` was the block metadata and is always 0.
- `onWandRightClick(level, wand, player)`: the click without a target, asked of the block the player looks at. Return
  the stack the hand holds afterwards, or `null`.
- `onUsingWandTick` and `onWandStoppedUsing` are not called by this port: in 1.7.10 they ran only for an object an
  add-on had marked through the internal `ItemWandCasting.setObjectInUse`.

The example's essentia bowl ([essentia and vis](essentia-and-vis.md#a-block-that-answers-a-wand)) empties on a wand
click and condenses Ordo on a sneaking one.

## Wand triggers

`WandTriggerRegistry.registerWandBlockTrigger(manager, event, block, modid)` (or with one exact `BlockState`) keeps
the original multiblock hook: a wand click on a registered block calls
`IWandTriggerManager.performTrigger(level, wand, player, x, y, z, side, event)`, right after `IWandable` and before
Thaumcraft's own structures. The metadata argument of the old forms is ignored: 1.21.1 has no metadata.
