package thaumcraft.common.testing;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.BlockBellows;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftBellows;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileBellows;
import thaumcraft.common.tiles.TileTube;
import thaumcraft.common.tiles.TileTubeBuffer;

/** M06c native cases. Raw ingredients, research, charged paying wand, empty
 * devices and essentia are explicit SERVER INPUT fixtures, not client/survival
 * acquisition. No injected recipes, clock/counter edits or seeded world RNG.
 * Uses the accepted connected-actor fixture without changing M06b sources. */
public final class TubeBufferRuntimeChecks {
    private TubeBufferRuntimeChecks() {}

    public static void recipesStockPersistenceAndTools(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open(); recipe(f); stockAndPersistence(f); malformedSavesAndSnapshots(f); tools(f);
        }
        test.succeed();
    }

    public static void bellowsChokesAndFillCadence(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open(); bellowsAndChokes(f); cadenceAndClosedFill(f);
        }
        test.succeed();
    }

    private static void recipe(Fixture f) {
        f.reset(); f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
        var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
        f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); f.aim(Vec3.atCenterOf(f.base));
        f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench, "native block use opens the actual arcane menu");
        var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
        var holder = f.level.getRecipeManager().byKey(TubeVariantsRuntimeChecks.id("arcane/essentia_buffer")).orElseThrow();
        f.check(holder.value() instanceof ArcaneShapedRecipe, "buffer is a real shaped recipe in the native manager");
        var recipe = (ArcaneShapedRecipe) holder.value();
        f.check(recipe.research().equals("CENTRIFUGE") && recipe.pattern().equals(List.of("PVP", "T T", "PRP"))
                && recipe.vis().equals(Map.of("aqua", 5, "ordo", 5)) && recipe.getResultItem(f.level.registryAccess()).is(ThaumcraftTubes.ESSENTIA_BUFFER_ITEM.get())
                && recipe.getResultItem(f.level.registryAccess()).getCount() == 1, "original buffer gate/literal shape/aqua5-ordo5/own output1 are actual recipe data");
        matrix(table, 3); table.setItem(10, wand(5000)); KnowledgeAccess.get(f.player).addResearch("TUBEFILTER"); KnowledgeAccess.get(f.player).removeResearch("CENTRIFUGE");
        menu.broadcastChanges(); f.check(menu.getSlot(0).getItem().isEmpty(), "TUBEFILTER does not substitute for the original CENTRIFUGE research");
        refuse(f, table, menu, "missing CENTRIFUGE preserves all eight occupied input cells and paying wand");
        KnowledgeAccess.get(f.player).addResearch("CENTRIFUGE");
        table.setItem(0, ItemEssence.filled("aqua")); menu.broadcastChanges(); refuse(f, table, menu, "filled phial cannot substitute for one of the FOUR empty phials");
        matrix(table, 3); table.setItem(7, new ItemStack(ThaumcraftTubes.ESSENTIA_FILTER_ITEM.get(), 3)); menu.broadcastChanges();
        refuse(f, table, menu, "filter tube is not the original restricted bottom-middle ingredient");
        matrix(table, 3); table.setItem(3, new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get(), 3)); menu.broadcastChanges();
        refuse(f, table, menu, "a valve cannot stand in for either BASIC side tube");
        matrix(table, 3); table.setItem(4, new ItemStack(Items.STICK)); menu.broadcastChanges(); refuse(f, table, menu, "explicit blank center refuses an extra input");
        matrix(table, 3); ItemStack paid = table.removeItemNoUpdate(10); menu.broadcastChanges(); refuse(f, table, menu, "no inserted wand cannot take a researched buffer preview");
        ItemStack shortWand = wand(5000); shortWand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(shortWand).withAmount("ordo", 549));
        table.setItem(10, shortWand); menu.broadcastChanges(); refuse(f, table, menu, "one short primal refuses the entire native payment");
        table.setItem(10, paid); menu.broadcastChanges();
        var found = f.level.getRecipeManager().getRecipeFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(), ArcaneRecipeInput.snapshot(table), f.level);
        f.check(found.isPresent() && found.get().id().equals(holder.id()) && menu.craftable() && menu.requiredVis("aqua") == 550 && menu.requiredVis("ordo") == 550,
                "actual native manager/menu price the original five whole vis through the real iron cap");
        menu.clicked(0, 0, ClickType.PICKUP, f.player); ItemStack first = menu.getCarried().copy(); menu.setCarried(ItemStack.EMPTY);
        f.check(first.is(ThaumcraftTubes.ESSENTIA_BUFFER_ITEM.get()) && first.getCount() == 1, "native extraction earns ONE actual buffer");
        for (int slot = 0; slot < 9; slot++) f.check(slot == 4 ? table.getItem(slot).isEmpty() : table.getItem(slot).getCount() == 2, "one ingredient consumed per occupied cell, including each separate phial: " + slot);
        f.player.getInventory().add(first); KnowledgeAccess.get(f.player).removeResearch("CENTRIFUGE"); var before = table.saveWithoutMetadata(f.level.registryAccess());
        f.check(menu.quickMoveStack(f.player, 0).isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), "stale funded preview rechecks actual actor research");
        KnowledgeAccess.get(f.player).addResearch("CENTRIFUGE"); menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
        f.check(f.player.getInventory().countItem(ThaumcraftTubes.ESSENTIA_BUFFER_ITEM.get()) == 3 && ArcaneRecipeInput.snapshot(table).items().stream().allMatch(ItemStack::isEmpty)
                && menu.quickMoveStack(f.player, 0).isEmpty(), "native quick-move pays exactly the two remaining recipes and cannot replay empty input");
        for (String tag : WandData.PRIMALS) f.check(ItemWandCasting.getVis(table.getItem(10), tag) == (tag.equals("aqua") || tag.equals("ordo") ? 3350 : 5000), "three actual buffer payments debit only aqua/ordo: " + tag);
        f.player.closeContainer();
        int source = -1; for (int slot = 0; slot < 36; slot++) if (f.player.getInventory().getItem(slot).is(ThaumcraftTubes.ESSENTIA_BUFFER_ITEM.get())) { source = slot; break; }
        f.check(source >= 0, "placement consumes an actually earned native inventory output, never a supplied finished buffer");
        ItemStack earned = f.player.getInventory().removeItem(source, 1);
        int emptyHand = -1; for (int slot = 0; slot < 9; slot++) if (f.player.getInventory().getItem(slot).isEmpty()) { emptyHand = slot; break; }
        f.check(emptyHand >= 0, "an actual empty hotbar slot receives the earned unit without overwriting the other two paid outputs"); f.player.getInventory().selected = emptyHand;
        f.put(f.base, Blocks.STONE); f.player.setItemInHand(InteractionHand.MAIN_HAND, earned); f.aim(Vec3.atCenterOf(f.base));
        f.check(f.use(f.base, Direction.UP, InteractionHand.MAIN_HAND).consumesAction() && f.level.getBlockEntity(f.base.above()) instanceof TileTubeBuffer placed
                && placed.getAspects().visSize() == 0 && placed.getTickCount() == 0 && placed.getBellowsCount() == -1 && f.player.getMainHandItem().isEmpty()
                && f.player.getInventory().countItem(ThaumcraftTubes.ESSENTIA_BUFFER_ITEM.get()) == 2,
                "earned buffer places through its real native BlockItem, empty and unseeded, with an actual survival hand debit");
    }

    private static void stockAndPersistence(Fixture f) {
        f.reset(); TileTubeBuffer buffer = buffer(f, f.base);
        f.check(!TileTube.class.isAssignableFrom(buffer.getClass()) && buffer.getTickCount() == 0 && buffer.getBellowsCount() == -1, "buffer has its own BE/factory and counter/cache, not a capacity8 TileTube kind");
        f.check(buffer.getEssentiaType(null) == null && buffer.getEssentiaAmount(null) == 0 && buffer.containerContains(null) == 0
                && !buffer.doesContainerAccept(null) && !buffer.doesContainerContainAmount(null, 0), "empty/invalid-type queries create no hidden null-key stock");
        var empty = buffer.saveWithFullMetadata(f.level.registryAccess());
        for (int offer : new int[] {-1, 0, 2, Integer.MAX_VALUE}) f.check(buffer.addToContainer(Aspect.WATER, offer) == offer
                && empty.equals(buffer.saveWithFullMetadata(f.level.registryAccess())), "amount1-only public add refuses other offers even with EIGHT FREE cells, not merely because of a full cap: " + offer);
        for (Direction face : Direction.values()) f.check(buffer.isSideOpen(face) && buffer.isConnectable(face) && buffer.canInputFrom(face) && buffer.canOutputTo(face)
                && buffer.getChoke(face) == 0 && buffer.getSuctionAmount(face) == 1 && buffer.getSuctionType(face) == null, "six initially open raw ports and original no-bellows pressure1: " + face);
        f.check(!buffer.isConnectable(null) && !buffer.canInputFrom(null) && !buffer.canOutputTo(null) && !buffer.isSideOpen(null)
                && buffer.getSuctionAmount(null) == 0 && buffer.getMinimumSuction() == 0 && !buffer.renderExtendedTube(), "UNKNOWN raw ports/suction are safely refused; minimum/render retain original values");
        buffer.setSuction(Aspect.ENERGY, 999);
        f.check(buffer.getSuctionAmount(Direction.UP) == 1 && buffer.getSuctionType(Direction.UP) == null && buffer.getBellowsCount() == -1, "setSuction is a no-op, and queries do not eagerly refresh bellows");
        Aspect[] supplied = {Aspect.FIRE, Aspect.WATER, Aspect.ORDER, Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.AIR, Aspect.FIRE};
        for (int index = 0; index < supplied.length; index++) {
            f.check(buffer.addToContainer(supplied[index], 1) == 0 && buffer.getEssentiaAmount(null) == index + 1, "native amount1 add increases the SHARED mixed total by exactly one");
            f.check(buffer.getBlockState().getAnalogOutputSignal(f.level, f.base) == (index + 1) * 14 / 8 + 1, "original full-range comparator follows total, not selected aspect");
        }
        f.check(buffer.addToContainer(Aspect.ENTROPY, 1) == 1 && buffer.getEssentiaAmount(null) == 8, "ninth unit is refused across all aspects, not eight per type");
        for (Direction face : Direction.values()) f.check(buffer.getEssentiaAmount(face) == 8, "public amount is TOTAL mixed stock on every face, never the selected type's amount");
        Map<Aspect, Integer> expected = ordered(Aspect.FIRE, 3, Aspect.WATER, 2, Aspect.ORDER, 1, Aspect.AIR, 2);
        assertStock(f, buffer, expected, "native insertion order and all per-type amounts");
        AspectList detached = buffer.getAspects(); detached.aspects.clear(); detached.add(Aspect.ENTROPY, 1000); buffer.setAspects(detached);
        assertStock(f, buffer, expected, "defensive getAspects and original setAspects stub cannot bypass cap/ownership");
        var unchanged = buffer.saveWithFullMetadata(f.level.registryAccess());
        for (int invalid : new int[] {-1, 0, 2, Integer.MAX_VALUE}) f.check(buffer.addToContainer(Aspect.FIRE, invalid) == invalid, "public add accepts exactly1; other offers retain their entire remainder: " + invalid);
        f.check(buffer.addToContainer(null, 1) == 1 && !buffer.takeFromContainer(null, 1) && !buffer.takeFromContainer(Aspect.FIRE, -1)
                && !buffer.takeFromContainer(Aspect.ENTROPY, -1) && !buffer.doesContainerContainAmount(Aspect.WATER, -1)
                && buffer.addEssentia(null, 1, Direction.UP) == 0 && buffer.takeEssentia(null, 1, Direction.UP) == 0
                && buffer.takeEssentia(Aspect.FIRE, -1, Direction.UP) == 0 && buffer.takeEssentia(Aspect.FIRE, 0, Direction.UP) == 0
                && buffer.addEssentia(Aspect.FIRE, 1, null) == 0 && buffer.takeEssentia(Aspect.FIRE, 1, null) == 0, "explicit authority hardening: null types/ports and negative withdrawals refuse, rather than original free creation/crashes");
        f.check(buffer.takeFromContainer(Aspect.FIRE, 0) && buffer.takeFromContainer(Aspect.ENTROPY, 0), "original valid-type zero container take succeeds without moving or notifying stock");
        buffer.setSideOpen(null, false); buffer.cycleChoke(null);
        f.check(unchanged.equals(buffer.saveWithFullMetadata(f.level.registryAccess())), "zero take/invalid port operations leave every real unit and configuration unchanged");
        for (int query = 0; query < 64; query++) {
            Aspect type = buffer.getEssentiaType(query % 2 == 0 ? null : Direction.UP);
            f.check(type != null && expected.containsKey(type) && buffer.getAspects().getAmount(type) > 0, "actual unseeded public RNG returns a held TYPE on every sided/unsided query; no distribution assertion");
        }
        inspect(f, buffer, expected);
        f.check(!buffer.takeFromContainer(Aspect.WATER, 3) && buffer.takeEssentia(Aspect.FIRE, 99, Direction.SOUTH) == 3, "exact container take refuses insufficient per-type stock; positive public transport take clips to actual held fire3");
        f.check(buffer.takeFromContainer(Aspect.WATER, 2) && buffer.addToContainer(Aspect.WATER, 1) == 0, "positive withdrawal/removal/re-add preserves per-type ownership");
        expected = ordered(Aspect.ORDER, 1, Aspect.AIR, 2, Aspect.WATER, 1); assertStock(f, buffer, expected, "re-added aspect moves to the end, without sorting the RNG index map");
        buffer.setSideOpen(Direction.EAST, false); choke(buffer, Direction.UP, 1); choke(buffer, Direction.NORTH, 2); ticks(f, buffer, 7);
        var saved = buffer.saveWithFullMetadata(f.level.registryAccess()); var update = buffer.getUpdateTag(f.level.registryAccess());
        f.check(saved.getByteArray("open").length == 6 && saved.getByteArray("choke").length == 6 && !saved.contains("count") && !saved.contains("bellows")
                && !update.contains("count") && !update.contains("bellows"), "disk/custom updates keep six open/choke flags and stock, not local phase or cached bellows");
        TileTubeBuffer stale = buffer; f.level.removeBlockEntity(f.base);
        BlockEntity loaded = BlockEntity.loadStatic(f.base, f.level.getBlockState(f.base), saved, f.level.registryAccess());
        f.check(loaded instanceof TileTubeBuffer, "actual native BlockEntity.loadStatic uses the separately registered buffer factory");
        f.level.setBlockEntity(loaded); buffer = (TileTubeBuffer) loaded;
        assertStock(f, buffer, expected, "native loadStatic round trip retains the exact ordered mixed map");
        f.check(buffer.getTickCount() == 0 && buffer.getBellowsCount() == -1 && !buffer.isSideOpen(Direction.EAST) && buffer.getChoke(Direction.UP) == 1
                && buffer.getChoke(Direction.NORTH) == 2 && buffer.getUpdatePacket() != null && stale.addToContainer(Aspect.FIRE, 1) == 1
                && !stale.takeFromContainer(Aspect.ORDER, 1), "reload preserves raw policy but resets only transient state, and stale BE cannot mutate old stock");
        TileTubeBuffer mirror = ThaumcraftTubes.BUFFER_BLOCK_ENTITY.get().create(f.base, buffer.getBlockState());
        f.check(mirror != null, "actual registered native update-target factory exists"); mirror.loadWithComponents(update, f.level.registryAccess());
        assertStock(f, mirror, expected, "custom update carries all ordered stock independently of transient cache");
        f.check(!mirror.isSideOpen(Direction.EAST) && mirror.getChoke(Direction.UP) == 1 && mirror.getChoke(Direction.NORTH) == 2, "custom update carries the original port/choke configuration");
        f.check(f.level.destroyBlock(f.base, true, f.player), "native break removes the actual configured mixed buffer");
        var drops = f.drops().stream().filter(drop -> drop.getItem().is(ThaumcraftTubes.ESSENTIA_BUFFER_ITEM.get())).toList();
        f.check(drops.size() == 1 && ItemStack.matches(drops.getFirst().getItem(), new ItemStack(ThaumcraftTubes.ESSENTIA_BUFFER_ITEM.get()))
                && !drops.getFirst().getItem().has(DataComponents.BLOCK_ENTITY_DATA) && f.pendingRows().isEmpty(), "one plain OWN buffer drops, never its mixed stock/chokes or an invented label/refund");
        ItemStack plain = drops.getFirst().getItem().copy(); drops.getFirst().discard(); f.put(f.base.below(), Blocks.STONE);
        f.player.setItemInHand(InteractionHand.MAIN_HAND, plain); f.aim(Vec3.atCenterOf(f.base.below()));
        f.check(f.use(f.base.below(), Direction.UP, InteractionHand.MAIN_HAND).consumesAction() && f.level.getBlockEntity(f.base) instanceof TileTubeBuffer fresh
                && fresh.getAspects().size() == 0 && fresh.getTickCount() == 0 && fresh.getBellowsCount() == -1
                && Arrays.stream(Direction.values()).allMatch(face -> fresh.isSideOpen(face) && fresh.getChoke(face) == 0), "native own-item placement has fresh empty stock and six open/choke0 sides, no phantom orientation/state");
    }

    /** Fresh-world safety, NOT a claim of legacy/malformed-world migration. A
     * rejected payload remains verbatim in quarantine; no arbitrary eight-unit
     * winner is selected and no valid subset is exposed as spendable stock. */
    private static void malformedSavesAndSnapshots(Fixture f) {
        f.reset(); TileTubeBuffer buffer = buffer(f, f.base);
        for (Aspect type : new Aspect[] {Aspect.WATER, Aspect.WATER, Aspect.FIRE}) buffer.addToContainer(type, 1);
        var before = buffer.snapshotAtomicState(); var insert = buffer.prepareInsert(before, Aspect.AIR, 1);
        var extract = buffer.prepareExtract(before, Aspect.WATER, 1);
        f.check(insert != null && extract != null && insert.owner() == buffer && insert.before() == before && extract.units() == 1
                && buffer.matchesAtomicState(before) && buffer.prepareInsert(before, Aspect.AIR, 2) == null
                && buffer.prepareInsert(before, null, 1) == null && buffer.prepareExtract(before, Aspect.WATER, 3) == null
                && buffer.prepareExtract(before, Aspect.FIRE, -1) == null, "callback-free preparation is exact, bound to owner/snapshot, and never itself changes the mixed pool");
        boolean rawDenied = false, sideDenied = false;
        try { buffer.applyAtomicRaw(extract); } catch (IllegalStateException expected) { rawDenied = true; }
        try { buffer.setSideOpenRaw(Direction.EAST, false); } catch (IllegalStateException expected) { sideDenied = true; }
        f.check(rawDenied && sideDenied && buffer.matchesAtomicState(before) && !EssentiaTransfers.isLocked(buffer), "a caller-created prepared change/raw side hook grants no write authority outside the shared executor");
        buffer.takeFromContainer(Aspect.FIRE, 1);
        f.check(!buffer.matchesAtomicState(before) && buffer.containerContains(Aspect.WATER) == 2, "full native snapshot fences a changed UNSELECTED aspect, not only wanted aqua2");
        before = buffer.snapshotAtomicState(); buffer.setSideOpen(Direction.EAST, false);
        f.check(!buffer.matchesAtomicState(before), "full native snapshot includes even an unrelated raw side");
        before = buffer.snapshotAtomicState(); buffer.cycleChoke(Direction.NORTH);
        f.check(!buffer.matchesAtomicState(before), "full native snapshot includes choke policy");
        before = buffer.snapshotAtomicState(); tick(f, buffer);
        f.check(!buffer.matchesAtomicState(before) && buffer.getTickCount() == 1 && buffer.getBellowsCount() == 0, "full native snapshot includes the genuinely advanced local count/bellows cache");
        f.check(buffer.addToContainer(Aspect.FIRE, 1) == 0, "restore the declared ignis fixture so malformed cases also contain a separate valid aqua row");
        var valid = buffer.saveWithFullMetadata(f.level.registryAccess());
        var malformed = new LinkedHashMap<String, CompoundTag>();
        CompoundTag bad = valid.copy(); bad.getList("Aspects", Tag.TAG_COMPOUND).getCompound(0).putString("key", "buffer-unknown-aspect"); malformed.put("unknown aspect alongside real paid stock", bad);
        bad = valid.copy(); bad.getList("Aspects", Tag.TAG_COMPOUND).getCompound(0).remove("key"); malformed.put("missing aspect key", bad);
        for (int amount : new int[] {-1, 0, 9, Integer.MAX_VALUE}) {
            bad = valid.copy(); bad.getList("Aspects", Tag.TAG_COMPOUND).getCompound(0).putInt("amount", amount); malformed.put("invalid total/amount " + amount, bad);
        }
        bad = valid.copy(); bad.getList("Aspects", Tag.TAG_COMPOUND).getCompound(0).putString("amount", "2"); malformed.put("wrong numeric NBT type", bad);
        bad = valid.copy(); var strings = new ListTag(); strings.add(StringTag.valueOf("not-an-aspect-row")); bad.put("Aspects", strings); malformed.put("non-compound aspect list", bad);
        bad = valid.copy(); bad.putInt("Aspects", 2); malformed.put("non-list stock", bad);
        for (String name : List.of("open", "choke")) {
            bad = valid.copy(); bad.putByteArray(name, new byte[5]); malformed.put("short " + name, bad);
            bad = valid.copy(); bad.putByteArray(name, new byte[7]); malformed.put("overlong " + name, bad);
            bad = valid.copy(); byte[] flags = bad.getByteArray(name); flags[3] = -1; bad.putByteArray(name, flags); malformed.put("negative " + name, bad);
            bad = valid.copy(); flags = bad.getByteArray(name); flags[3] = (byte) (name.equals("open") ? 2 : 3); bad.putByteArray(name, flags); malformed.put("invalid value " + name, bad);
        }
        for (var entry : malformed.entrySet()) {
            f.level.removeBlockEntity(f.base);
            BlockEntity raw = BlockEntity.loadStatic(f.base, f.level.getBlockState(f.base), entry.getValue(), f.level.registryAccess());
            f.check(raw instanceof TileTubeBuffer, "real registered loadStatic retains a diagnosable buffer object for malformed data: " + entry.getKey());
            f.level.setBlockEntity(raw); var quarantined = (TileTubeBuffer) raw;
            f.check(quarantined.isQuarantined() && quarantined.getAmount() == 0 && quarantined.getEssentiaType(null) == null
                    && quarantined.addToContainer(Aspect.WATER, 1) == 1 && !quarantined.takeFromContainer(Aspect.WATER, 1)
                    && !quarantined.setSideOpen(Direction.UP, false) && !quarantined.cycleChoke(Direction.UP)
                    && Arrays.stream(Direction.values()).noneMatch(quarantined::isConnectable), "malformed save is wholly unavailable, never a spendable/truncated valid subset: " + entry.getKey());
            ticks(f, quarantined, 21);
            var saved = quarantined.saveWithFullMetadata(f.level.registryAccess()); var update = quarantined.getUpdateTag(f.level.registryAccess());
            f.check(quarantined.getTickCount() == 0 && quarantined.getBellowsCount() == -1
                    && saved.getCompound("RejectedBuffer").equals(entry.getValue()) && update.getCompound("RejectedBuffer").equals(entry.getValue()), "quarantine keeps the ENTIRE original malformed payload verbatim on disk/update, without ticking it into new units: " + entry.getKey());
            f.level.removeBlockEntity(f.base);
            var reloaded = (TileTubeBuffer) BlockEntity.loadStatic(f.base, f.level.getBlockState(f.base), saved, f.level.registryAccess());
            f.check(reloaded != null && reloaded.isQuarantined() && reloaded.getUpdateTag(f.level.registryAccess()).getCompound("RejectedBuffer").equals(entry.getValue()), "quarantine survives a second real native reload without nesting/forgetting the rejected payload");
            f.level.setBlockEntity(reloaded);
        }
        var duplicate = valid.copy(); var rows = new ListTag();
        for (Object[] row : new Object[][] {{"aqua", 2}, {"ignis", 1}, {"aqua", 3}}) {
            var compound = new CompoundTag(); compound.putString("key", (String) row[0]); compound.putInt("amount", (Integer) row[1]); rows.add(compound);
        }
        duplicate.put("Aspects", rows); duplicate.remove("open"); duplicate.remove("choke");
        f.level.removeBlockEntity(f.base); buffer = (TileTubeBuffer) BlockEntity.loadStatic(f.base, f.level.getBlockState(f.base), duplicate, f.level.registryAccess());
        f.check(buffer != null && !buffer.isQuarantined(), "valid repeated rows merge quantities without inventing new kinds or rejecting a legal total6"); f.level.setBlockEntity(buffer);
        assertStock(f, buffer, ordered(Aspect.WATER, 5, Aspect.FIRE, 1), "valid row merge preserves FIRST key order and all exact paid quantities");
        for (Direction face : Direction.values()) f.check(buffer.isSideOpen(face) && buffer.getChoke(face) == 0, "omitted side/choke arrays use six safe original fresh defaults, not invalid indices");
    }

    private static void tools(Fixture f) {
        for (BlockTube.Kind kind : BlockTube.Kind.values()) {
            f.reset(); TileTubeBuffer buffer = buffer(f, f.base); TileTube neighbor = f.tube(f.base.above(), kind);
            if (kind == BlockTube.Kind.VALVE) neighbor.setFacing(Direction.DOWN);
            ItemStack wand = wand(731); f.player.setItemInHand(InteractionHand.MAIN_HAND, wand); f.aim(Vec3.atLowerCornerOf(f.base).add(.5, .9, .5));
            var held = wand.copy();
            f.check(f.use(f.base, Direction.WEST, InteractionHand.MAIN_HAND).consumesAction() && !buffer.isSideOpen(Direction.UP) && !neighbor.isSideOpen(Direction.DOWN),
                    "actual indexed buffer arm (not supplied clickedface WEST) assigns the same closed flag to every old tube kind: " + kind);
            f.aim(Vec3.atLowerCornerOf(f.base.above()).add(.5, .1, .5));
            f.check(f.use(f.base.above(), Direction.EAST, InteractionHand.MAIN_HAND).consumesAction() && buffer.isSideOpen(Direction.UP) && neighbor.isSideOpen(Direction.DOWN)
                    && (!neighbor.isValve() || !neighbor.isConnectable(Direction.DOWN)), "old tube indexed closed arm reopens the buffer partner, including valve's always-excluded raw stem");
            f.aim(Vec3.atLowerCornerOf(f.base).add(.5, .9, .5)); f.player.setShiftKeyDown(true);
            for (int next : new int[] {1, 2, 0}) f.check(f.use(f.base, Direction.WEST, InteractionHand.MAIN_HAND).consumesAction() && buffer.getChoke(Direction.UP) == next
                    && buffer.isSideOpen(Direction.UP) && neighbor.isSideOpen(Direction.DOWN), "secondary native wand cycles only that arm choke0->1->2->0, never closes or rewrites its partner");
            f.player.setShiftKeyDown(false); f.aim(Vec3.atCenterOf(f.base)); var saved = buffer.saveWithFullMetadata(f.level.registryAccess());
            f.use(f.base); f.check(saved.equals(buffer.saveWithFullMetadata(f.level.registryAccess())) && ItemStack.matches(held, f.player.getMainHandItem()), "buffer core click invents no facing function and no tool cost");
        }
        f.reset(); TileTubeBuffer lower = buffer(f, f.base), upper = buffer(f, f.base.above());
        f.player.setItemInHand(InteractionHand.MAIN_HAND, wand(731)); f.aim(Vec3.atLowerCornerOf(f.base).add(.5, .9, .5));
        f.check(f.use(f.base).consumesAction() && !lower.isSideOpen(Direction.UP) && !upper.isSideOpen(Direction.DOWN), "buffer-to-buffer indexed raw flags commit as one paired operation");
        f.player.setShiftKeyDown(true); f.use(f.base);
        f.check(lower.getChoke(Direction.UP) == 1 && !lower.isSideOpen(Direction.UP) && !upper.isSideOpen(Direction.DOWN), "closed arms remain traceable for choke changes without an unwanted reopen");
        f.player.setShiftKeyDown(false); f.use(f.base);
        f.check(lower.isSideOpen(Direction.UP) && upper.isSideOpen(Direction.DOWN), "closed buffer-to-buffer arms remain indexed for reopening");
    }

    private static void bellowsAndChokes(Fixture f) {
        for (Direction side : Direction.values()) {
            f.reset(); TileTubeBuffer buffer = buffer(f, f.base); bellows(f, f.base, side, true);
            f.check(buffer.getBellowsCount() == -1 && buffer.getSuctionAmount(Direction.UP) == 1, "real bellows do not change cached pressure before the first native tick");
            tick(f, buffer); f.check(buffer.getTickCount() == 1 && buffer.getBellowsCount() == 1 && buffer.getSuctionAmount(Direction.UP) == 32, "first native tick discovers one actual inward-facing bellows, including vertical neighbors: " + side);
            BlockPos power = f.base.relative(side, 2); f.level.setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
            f.check(((TileBellows) f.level.getBlockEntity(f.base.relative(side))).isPowered(), "actual adjacent native redstone powers only the intended bellows fixture");
            ticks(f, buffer, 18); f.check(buffer.getTickCount() == 19 && buffer.getBellowsCount() == 1 && buffer.getSuctionAmount(Direction.UP) == 32, "query-time rescans cannot invalidate the cached bellows before local tick20");
            tick(f, buffer); f.check(buffer.getTickCount() == 20 && buffer.getBellowsCount() == 0 && buffer.getSuctionAmount(Direction.UP) == 1, "native twentieth tick refresh excludes powered bellows");
            f.level.setBlock(power, Blocks.AIR.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET); ticks(f, buffer, 19);
            f.check(buffer.getTickCount() == 39 && buffer.getBellowsCount() == 0 && buffer.getSuctionAmount(Direction.UP) == 1, "unpowering likewise waits for the existing cache cadence");
            tick(f, buffer); f.check(buffer.getBellowsCount() == 1 && buffer.getSuctionAmount(Direction.UP) == 32, "tick40 rediscovers the unpowered physical bellows without counter/RNG writes");
            bellows(f, f.base, side, false); ticks(f, buffer, 20);
            f.check(buffer.getBellowsCount() == 0 && buffer.getSuctionAmount(Direction.UP) == 1, "actual outward-facing bellows is excluded at the next native cache refresh");
        }
        f.reset(); TileTubeBuffer buffer = buffer(f, f.base);
        for (Direction side : Direction.values()) bellows(f, f.base, side, true);
        tick(f, buffer); f.check(buffer.getBellowsCount() == 6, "all six physical inward/unpowered neighbors count, not just horizontal or open transport faces");
        for (Direction face : Direction.values()) {
            f.check(buffer.getSuctionAmount(face) == 192, "six cached bellows produce6*32 on choke0"); choke(buffer, face, 1);
            f.check(buffer.getSuctionAmount(face) == 1, "choke1 forces pressure1 independently of bellows"); choke(buffer, face, 2);
            f.check(buffer.getSuctionAmount(face) == 0 && buffer.canInputFrom(face) && buffer.canOutputTo(face), "choke2 is pressure0, NOT a raw input/output-port closure"); choke(buffer, face, 0);
        }
        choke(buffer, Direction.EAST, 2); f.check(buffer.addEssentia(Aspect.WATER, 1, Direction.EAST) == 1 && buffer.takeEssentia(Aspect.WATER, 1, Direction.EAST) == 1, "pressure0 choke retains original external raw add/take behavior");
    }

    private static void cadenceAndClosedFill(Fixture f) {
        f.reset(); TileTubeBuffer buffer = buffer(f, f.base); f.put(f.base.below(), ThaumcraftAlembics.ALEMBIC.get());
        var source = (TileAlembic) f.level.getBlockEntity(f.base.below()); source.addToContainer(Aspect.WATER, 8);
        for (int count = 1; count <= 10; count++) {
            tick(f, buffer); f.check(buffer.getTickCount() == count && buffer.getEssentiaAmount(null) == count / 5 && source.getAmount() == 8 - count / 5,
                    "actual buffer cadence draws at most one on native ticks5/10, preserving all supplied stock after every call");
        }
        buffer.setSideOpen(Direction.DOWN, false);
        f.check(buffer.addEssentia(Aspect.WATER, 1, Direction.DOWN) == 0 && !buffer.isConnectable(Direction.DOWN), "closed receiver raw port still refuses ordinary external insertion");
        ticks(f, buffer, 5);
        f.check(buffer.getEssentiaAmount(null) == 3 && source.getAmount() == 5 && !buffer.isSideOpen(Direction.DOWN), "original autonomous fill may use its OWN closed receiving side; the global executor must not impose ordinary receiver-port policy here");
        for (int count = 16; count <= 45; count++) {
            tick(f, buffer); int expected = Math.min(8, count / 5);
            f.check(buffer.getEssentiaAmount(null) == expected && source.getAmount() == 8 - expected, "native fill reaches eight TOTAL then stops without a hidden ninth unit/refund");
        }
    }

    private static void inspect(Fixture f, TileTubeBuffer buffer, Map<Aspect, Integer> expected) {
        f.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftContent.RESONATOR.get())); f.aim(Vec3.atCenterOf(buffer.getBlockPos()));
        for (boolean secondary : new boolean[] {false, true}) {
            f.player.setShiftKeyDown(secondary); var before = buffer.saveWithFullMetadata(f.level.registryAccess()); f.chat();
            f.check(f.use(buffer.getBlockPos()).consumesAction(), "native primary/secondary resonator dispatch inspects the actual separate buffer");
            List<Component> messages = f.chat(); var sorted = expected.keySet().stream().sorted(java.util.Comparator.comparing(Aspect::getTag)).toList();
            f.check(messages.size() == sorted.size() + 1, "genuine buffer emits one sorted contents line per stored type plus ONE suction line, not a representative singleton");
            for (int index = 0; index < sorted.size(); index++) {
                Aspect type = sorted.get(index); f.check(messages.get(index).getContents() instanceof TranslatableContents text && text.getKey().equals("tc.resonator1")
                        && text.getArgs()[0].equals(Integer.toString(expected.get(type))) && text.getArgs()[1].equals(type.getName()), "native resonator sorted contents line reports real positive stock: " + type.getTag());
            }
            f.check(messages.getLast().getContents() instanceof TranslatableContents text && text.getKey().equals("tc.resonator2")
                    && before.equals(buffer.saveWithFullMetadata(f.level.registryAccess())), "inspector neither purges/reorders buffer input nor cycles a choke or toggles a side");
        }
        f.player.setShiftKeyDown(false);
    }

    static TileTubeBuffer buffer(Fixture f, BlockPos pos) { f.put(pos, ThaumcraftTubes.ESSENTIA_BUFFER.get()); return (TileTubeBuffer) f.level.getBlockEntity(pos); }
    static void tick(Fixture f, TileTubeBuffer buffer) {
        var pos = buffer.getBlockPos(); var state = f.level.getBlockState(pos); var ticker = state.getTicker(f.level, ThaumcraftTubes.BUFFER_BLOCK_ENTITY.get());
        if (ticker == null) throw new IllegalStateException("Missing real buffer ticker"); ticker.tick(f.level, pos, state, buffer);
    }
    static void ticks(Fixture f, TileTubeBuffer buffer, int count) { for (int i = 0; i < count; i++) tick(f, buffer); }
    static void choke(TileTubeBuffer buffer, Direction face, int wanted) { for (int i = 0; i < 3 && buffer.getChoke(face) != wanted; i++) buffer.cycleChoke(face); if (buffer.getChoke(face) != wanted) throw new AssertionError("Native choke cycle did not reach " + wanted); }
    static void bellows(Fixture f, BlockPos core, Direction side, boolean inward) {
        BlockPos pos = core.relative(side); f.level.removeBlockEntity(pos);
        f.level.setBlock(pos, ThaumcraftBellows.BELLOWS.get().defaultBlockState().setValue(BlockBellows.FACING, inward ? side.getOpposite() : side), TubeVariantsRuntimeChecks.QUIET);
    }
    static Map<Aspect, Integer> ordered(Object... entries) { var out = new LinkedHashMap<Aspect, Integer>(); for (int i = 0; i < entries.length; i += 2) out.put((Aspect) entries[i], (Integer) entries[i + 1]); return out; }
    static void assertStock(Fixture f, TileTubeBuffer buffer, Map<Aspect, Integer> expected, String why) {
        var actual = buffer.getAspects(); f.check(actual.aspects.equals(expected) && Arrays.asList(actual.getAspects()).equals(List.copyOf(expected.keySet()))
                && buffer.getEssentiaAmount(null) == expected.values().stream().mapToInt(Integer::intValue).sum(), why);
    }
    private static void matrix(TileArcaneWorkbench table, int count) {
        for (int slot = 0; slot < 9; slot++) table.setItem(slot, ItemStack.EMPTY);
        for (int slot : new int[] {0, 2, 6, 8}) table.setItem(slot, new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get(), count));
        table.setItem(1, new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get(), count)); table.setItem(7, new ItemStack(ThaumcraftTubes.ESSENTIA_RESTRICT_ITEM.get(), count));
        for (int slot : new int[] {3, 5}) table.setItem(slot, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(), count));
    }
    private static ItemStack wand(int amount) {
        var stack = new ItemStack(ThaumcraftContent.WAND.get()); var vis = new LinkedHashMap<String, Integer>(); for (String tag : WandData.PRIMALS) vis.put(tag, amount);
        stack.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "iron", vis)); return stack;
    }
    private static void refuse(Fixture f, TileArcaneWorkbench table, ContainerArcaneWorkbench menu, String why) {
        var before = table.saveWithoutMetadata(f.level.registryAccess()); menu.clicked(0, 0, ClickType.PICKUP, f.player);
        f.check(menu.getCarried().isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), why);
    }
}
