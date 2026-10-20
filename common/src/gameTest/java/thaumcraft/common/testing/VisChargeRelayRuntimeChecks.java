package thaumcraft.common.testing;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.blocks.BlockVisRelay;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.config.ThaumcraftVisCharging;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileMagicWorkbenchCharger;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeEnergized;

/** Declared raw-node/workbench/wand inputs; real conversion, renewable source,
 * graph, registered charger ticker and durable physical workbench evacuation.
 * No production stock/phase fields are injected. */
public final class VisChargeRelayRuntimeChecks {
    private VisChargeRelayRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        BlockPos origin = test.absolutePos(new BlockPos(3, 3, 3));
        var forced = GolemChunks.hold(test.getLevel(), List.of(origin.west(12), origin.east(12)));
        try (var f = NodeTransductionRuntimeChecks.fixture(test, false)) {
            f.open();
            chargeAndRoute(f);
            attunementAndPersistence(f);
            pulseRemovalKeepsPaidWand(f);
            recipe(f);
        } finally {
            for (var chunk : forced) test.getLevel().setChunkForced(chunk.x, chunk.z, false);
        }
        test.succeed();
    }

    private record Network(TileNodeEnergized source, PulseRelay middle, TileMagicWorkbenchCharger charger, TileArcaneWorkbench table) {}
    private static Network network(Fixture f) {
        f.reset();
        var source = NodeTransductionRuntimeChecks.energize(f, f.base.west(6), NodeTransductionRuntimeChecks.primals(64), null, TileNode.NodeType.NORMAL);
        NodeTransductionRuntimeChecks.sourceTick(f, source);
        f.put(f.base.below(), Blocks.STONE);
        f.level.setBlock(f.base, ThaumcraftNodeDevices.VIS_RELAY.get().defaultBlockState().setValue(BlockVisRelay.FACING, Direction.UP), TubeVariantsRuntimeChecks.QUIET);
        f.level.removeBlockEntity(f.base);
        var middle = new PulseRelay(f.base, f.level.getBlockState(f.base)); f.level.setBlockEntity(middle);
        TileVisNode.tick(f.level, f.base, middle.getBlockState(), middle);
        f.put(f.base.east(6).below(), ThaumcraftArcane.ARCANE_WORKBENCH.get());
        var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base.east(6).below());
        f.put(f.base.east(6), ThaumcraftVisCharging.VIS_CHARGE_RELAY.get());
        var charger = (TileMagicWorkbenchCharger) f.level.getBlockEntity(f.base.east(6));
        tick(f, charger); // Empty workbench still attaches the real relay.
        f.check(parent(middle) == source && parent(charger) == middle && charger.getRootSource().get() == source,
                "source-6 -> actual intermediary0 -> charger+6: root is outside direct eight-block range");
        f.check(charger.getRange() == 8 && !charger.isSource() && charger.getOrientation() == Direction.DOWN
                        && charger.getBlockState().getLightEmission() == 0,
                "original fixed DOWN hanging charger, range8, no independent generation or ordinary-relay dynamic light");
        var shape = charger.getBlockState().getShape(f.level, charger.getBlockPos()).bounds();
        f.check(shape.minX == 5D/16 && shape.minY == .5 && shape.minZ == 5D/16
                        && shape.maxX == 11D/16 && shape.maxY == 1 && shape.maxZ == 11D/16,
                "actual original hanging bounds5,8,5..11,16,11");
        return new Network(source, middle, charger, table);
    }

    private static void chargeAndRoute(Fixture f) {
        var n = network(f); ItemStack wand = wand(0); n.table.setItem(TileArcaneWorkbench.WAND_SLOT, wand);
        tick(f, n.charger);
        for (String tag : WandData.PRIMALS) f.check(ItemWandCasting.data(wand).amount(tag) == 5
                        && n.source.getAvailableVis().getAmount(Aspect.getAspect(tag)) == 3,
                "one native charger tick transfers five raw CV of EACH primal to the actual slot10 wand: " + tag);
        tick(f, n.charger);
        for (String tag : WandData.PRIMALS) f.check(ItemWandCasting.data(wand).amount(tag) == 8
                        && n.source.getAvailableVis().getAmount(Aspect.getAspect(tag)) == 0,
                "same renewable epoch grants only its remaining three, never invented five: " + tag);
        tick(f, n.charger);
        f.check(ItemWandCasting.data(wand).vis().values().stream().allMatch(value -> value == 8), "empty source never increments the wand");
        NodeTransductionRuntimeChecks.sourceTick(f, n.source);
        var full = new LinkedHashMap<String, Integer>();
        for (String tag : WandData.PRIMALS) full.put(tag, WandData.EMPTY.capacity());
        full.put("aer", WandData.EMPTY.capacity() - 2);
        wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("wood", "iron", full));
        tick(f, n.charger);
        for (String tag : WandData.PRIMALS) f.check(ItemWandCasting.data(wand).amount(tag) == WandData.EMPTY.capacity()
                        && n.source.getAvailableVis().getAmount(Aspect.getAspect(tag)) == (tag.equals("aer") ? 6 : 8),
                "near capacity requests only two aer; already full primals incur zero source charge: " + tag);
        var before = ItemWandCasting.data(wand);
        f.check(VisNetHandler.drainVis(f.level, n.charger.getBlockPos().east(), Aspect.FIRE, 1) == 1
                        && ItemWandCasting.data(wand).equals(before) && n.source.getAvailableVis().getAmount(Aspect.FIRE) == 7,
                "positional API consumer uses the charger as an ordinary relay without granting a second wand credit");
        f.check(n.charger.consumeVis(Aspect.FIRE, 1) == 1 && ItemWandCasting.data(wand).equals(before),
                "explicit relay API consumption outside prepared charging retains ordinary semantics");
        boolean refused = false;
        try { n.charger.acceptPreparedVisGrant(Aspect.AIR, 1); } catch (IllegalStateException expected) { refused = true; }
        f.check(refused && ItemWandCasting.data(wand).equals(before), "public grant entry cannot forge or replay executor credit");
        n.table.setItem(TileArcaneWorkbench.WAND_SLOT, new ItemStack(Items.STICK));
        var stock = n.source.getAvailableVis(); tick(f, n.charger);
        f.check(n.source.getAvailableVis().aspects.equals(stock.aspects), "non-wand in physical slot10 consumes no vis");
        n.table.setItem(TileArcaneWorkbench.WAND_SLOT, ItemStack.EMPTY); n.table.setItem(0, wand(0));
        tick(f, n.charger);
        f.check(n.source.getAvailableVis().aspects.equals(stock.aspects), "a wand in crafting slot0 is not the charging target");
    }

    private static void attunementAndPersistence(Fixture f) {
        var n = network(f); var tile = n.charger; BlockPos pos = tile.getBlockPos();
        f.aim(Vec3.atCenterOf(pos));
        ItemStack shard = new ItemStack(ThaumcraftContent.SHARDS.get("ignis").get(), 3);
        f.player.setItemInHand(InteractionHand.MAIN_HAND, shard);
        f.check(f.use(pos).consumesAction() && tile.getAttunement() == 1 && shard.getCount() == 3,
                "actual native shard click attunes without consuming the shard");
        f.check(f.use(pos).consumesAction() && tile.getAttunement() == -1 && shard.getCount() == 3,
                "clicking matching shard returns to unattuned");
        f.use(pos); var saved = tile.saveWithFullMetadata(f.level.registryAccess());
        var old = tile; tile = NodeTransductionRuntimeChecks.reload(f, tile, saved, TileMagicWorkbenchCharger.class);
        f.check(tile.getAttunement() == 1 && tile.getOrientation() == Direction.DOWN
                        && tile.saveWithoutMetadata(f.level.registryAccess()).getShort("orientation") == 0,
                "registered charger save/load preserves attunement and canonical fixed orientation0");
        ItemStack wand = wand(0); f.player.setItemInHand(InteractionHand.MAIN_HAND, wand); f.player.setShiftKeyDown(true);
        f.check(f.use(pos).consumesAction() && tile.getAttunement() == 2 && ItemWandCasting.data(wand).vis().isEmpty(),
                "native sneaking wand hook cycles attunement with no vis payment");
        var bad = saved.copy(); bad.putByte("color", (byte) 6);
        tile = NodeTransductionRuntimeChecks.reload(f, tile, bad, TileMagicWorkbenchCharger.class);
        f.check(tile.isQuarantined() && tile.saveWithoutMetadata(f.level.registryAccess()).getCompound("RejectedVisChargeRelay").equals(bad),
                "out-of-domain saved attunement is preserved as disabled evidence, never defaulted to active");
        tick(f, old);
        f.check(f.level.getBlockEntity(pos) == tile && tile.isQuarantined(), "retired ticker cannot mutate installed replacement");
    }

    private static void pulseRemovalKeepsPaidWand(Fixture f) {
        var n = network(f); ItemStack wand = wand(0); n.table.setItem(TileArcaneWorkbench.WAND_SLOT, wand);
        int[] callbacks = {0};
        n.middle.action = () -> {
            ++callbacks[0];
            f.check(n.source.getAvailableVis().getAmount(Aspect.AIR) == 3 && ItemWandCasting.data(n.table.getItem(TileArcaneWorkbench.WAND_SLOT)).amount("aer") == 5,
                    "FIRST parent pulse observes paid source8->3 AND actual wand0->5 before any removal callback");
            f.check(f.level.destroyBlock(n.table.getBlockPos(), true), "actual synchronous pulse removes workbench and evacuates physical inventory");
        };
        tick(f, n.charger); f.flushFresh();
        var drops = f.drops().stream().filter(entity -> entity.getItem().is(ThaumcraftContent.WAND.get())).toList();
        f.check(callbacks[0] == 1 && n.table.isRemoved() && n.table.isEmpty() && drops.size() == 1
                        && drops.getFirst().getItem() != wand && drops.getFirst().getItem().getCount() == 1
                        && ItemWandCasting.data(drops.getFirst().getItem()).amount("aer") == 5
                        && n.source.getAvailableVis().getAmount(Aspect.AIR) == 3 && f.pendingRows().isEmpty(),
                "exactly one PHYSICAL evacuated wand owns paid aer5 after bench removal; no lost detached credit, duplicated drop or refund");
        for (String tag : WandData.PRIMALS) if (!tag.equals("aer")) f.check(ItemWandCasting.data(drops.getFirst().getItem()).amount(tag) == 0
                        && n.source.getAvailableVis().getAmount(Aspect.getAspect(tag)) == 8,
                "destroyed bench stops the remaining per-primal transfers: " + tag);
        var stock = n.source.getAvailableVis(); tick(f, n.charger);
        f.check(f.level.getBlockEntity(n.charger.getBlockPos()) == n.charger && n.source.getAvailableVis().aspects.equals(stock.aspects),
                "charger remains a floating graph relay with no workbench and no further charge");
    }

    private static void recipe(Fixture f) {
        var recipe = (ArcaneShapedRecipe) f.level.getRecipeManager().byKey(id("arcane/vis_charge_relay")).orElseThrow().value();
        ItemStack rod = new ItemStack(BuiltInRegistries.ITEM.get(id("wand_rod_greatwood"))), iron = new ItemStack(Items.IRON_INGOT);
        var input = new ArcaneRecipeInput(List.of(ItemStack.EMPTY, new ItemStack(ThaumcraftNodeDevices.VIS_RELAY_ITEM.get()), ItemStack.EMPTY,
                rod, ItemStack.EMPTY, rod, iron, ItemStack.EMPTY, iron));
        f.check(recipe.matches(input, f.level) && recipe.research().equals("VISCHARGERELAY")
                        && recipe.vis().equals(Map.of("ignis", 16, "ordo", 16, "aer", 16))
                        && recipe.assemble(input, f.level.registryAccess()).is(ThaumcraftVisCharging.VIS_CHARGE_RELAY_ITEM.get()),
                "actual original NodeChargeRelay recipe: relay/two greatwood rods/two iron, exact research and three16-vis costs");
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static ItemStack wand(int amount) {
        var wand = new ItemStack(ThaumcraftContent.WAND.get());
        if (amount != 0) {
            var values = new LinkedHashMap<String, Integer>(); for (String tag : WandData.PRIMALS) values.put(tag, amount);
            wand.set(ThaumcraftContent.WAND_DATA.get(), new WandData("wood", "iron", values));
        }
        return wand;
    }
    private static TileVisNode parent(TileVisNode node) { WeakReference<TileVisNode> ref = node.getParent(); return ref == null ? null : ref.get(); }
    private static void tick(Fixture f, TileMagicWorkbenchCharger tile) {
        var ticker = tile.getBlockState().getTicker(f.level, ThaumcraftVisCharging.VIS_CHARGE_RELAY_TILE.get());
        if (ticker == null) throw new IllegalStateException("Missing native charger ticker");
        ticker.tick(f.level, tile.getBlockPos(), tile.getBlockState(), tile);
    }
    /** Only the post-payment effect differs. Native final graph/debit methods
     * and the built-in source/charger are the production implementations. */
    private static final class PulseRelay extends TileVisNode {
        Runnable action;
        PulseRelay(BlockPos pos, BlockState state) { super(ThaumcraftNodeDevices.VIS_RELAY_BLOCK_ENTITY.get(), pos, state); }
        @Override public int getRange() { return 8; }
        @Override public boolean isSource() { return false; }
        @Override public void triggerConsumeEffect(Aspect aspect) {
            if (action == null) return;
            var once = action; action = null; once.run();
        }
    }
}
