package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.items.ItemJarLabel;
import thaumcraft.common.items.ItemEssence;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileTube;

/** M06b native recipe/transport case. Raw materials, research, charged wand and
 * controlled neighboring pressure/stock are explicit SERVER INPUT fixtures.
 * No recipe injection, clock/phase/RNG edits, or survival acquisition claim. */
public final class TubeVariantsRuntimeChecks {
    static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private TubeVariantsRuntimeChecks() {}

    public static void recipesAndNativeTransport(GameTestHelper test) {
        try (var f = new Fixture(test)) {
            f.open(); recipes(f); restrict(f); filter(f); oneWay(f); venting(f); persistenceAndDrops(f);
        }
        test.succeed();
    }

    private record Upgrade(String id, Item ingredient, Item wrong, Map<String, Integer> vis, BlockTube.Kind kind) {}
    private static void recipes(Fixture f) {
        f.reset(); f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
        f.aim(Vec3.atCenterOf(f.base)); f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        f.check(f.use(f.base).consumesAction() && f.player.containerMenu instanceof ContainerArcaneWorkbench, "native interaction opens the actual arcane workbench menu");
        var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base); var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
        var upgrades = List.of(
                new Upgrade("essentia_filter", ThaumcraftContent.MATERIALS.get("vis_filter").get(), Items.PAPER, Map.of("aqua", 5, "ordo", 16), BlockTube.Kind.FILTER),
                new Upgrade("essentia_restrict", Items.STONE, Items.COBBLESTONE, Map.of("aqua", 5, "terra", 16), BlockTube.Kind.RESTRICT),
                new Upgrade("essentia_oneway", Items.LAPIS_LAZULI, Items.LIGHT_BLUE_DYE, Map.of("aqua", 5, "ordo", 8, "perditio", 8), BlockTube.Kind.ONEWAY));
        for (Upgrade upgrade : upgrades) {
            table.clearContent(); f.player.getInventory().clearContent();
            var holder = f.level.getRecipeManager().byKey(id("arcane/" + upgrade.id())).orElseThrow();
            f.check(holder.value() instanceof ArcaneShapelessRecipe, "real manager loads a genuine shapeless recipe: " + upgrade.id());
            var recipe = (ArcaneShapelessRecipe) holder.value();
            f.check(recipe.research().equals("TUBEFILTER") && recipe.vis().equals(upgrade.vis()) && recipe.getIngredients().size() == 2
                    && recipe.getResultItem(f.level.registryAccess()).is(item(upgrade.kind())) && recipe.getResultItem(f.level.registryAccess()).getCount() == 1,
                    "original gate, whole-vis cost, two ingredients and one own output: " + upgrade.id());
            for (int[] pair : new int[][] {{0, 8}, {8, 0}, {1, 6}, {5, 2}}) {
                var input = upgradeInput(pair[0], pair[1], upgrade.ingredient(), 2);
                var actual = f.level.getRecipeManager().getRecipeFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(), input, f.level);
                f.check(actual.isPresent() && actual.get().id().equals(holder.id()), "actual manager accepts arbitrary occupied cells " + pair[0] + "/" + pair[1] + ": " + upgrade.id());
            }
            if (upgrade.kind() == BlockTube.Kind.ONEWAY) {
                f.check(recipe.matches(upgradeInput(7, 2, Items.BLUE_DYE, 1), f.level)
                        && !recipe.matches(upgradeInput(7, 2, Items.CYAN_DYE, 1), f.level), "original dyeBlue includes lapis AND modern blue, not unrelated cyan/light-blue dyes");
            }
            matrix(table, upgradeInput(8, 1, upgrade.ingredient(), 2)); table.setItem(10, wand(5000));
            KnowledgeAccess.get(f.player).addResearch("TUBES"); KnowledgeAccess.get(f.player).removeResearch("TUBEFILTER");
            menu.broadcastChanges(); f.check(menu.getSlot(0).getItem().isEmpty(), "TUBES alone does not unlock " + upgrade.id());
            refuse(f, table, menu, "missing TUBEFILTER refuses without consuming stock/vis");
            KnowledgeAccess.get(f.player).addResearch("TUBEFILTER");
            table.setItem(4, new ItemStack(Items.STICK)); menu.broadcastChanges();
            refuse(f, table, menu, "extra occupied native cell invalidates " + upgrade.id()); table.setItem(4, ItemStack.EMPTY);
            table.setItem(8, new ItemStack(ThaumcraftTubes.ESSENTIA_VALVE_ITEM.get(), 2)); menu.broadcastChanges();
            refuse(f, table, menu, "flattened valve cannot substitute for basic tube");
            table.setItem(8, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(), 2)); table.setItem(1, new ItemStack(upgrade.wrong(), 2)); menu.broadcastChanges();
            refuse(f, table, menu, "wrong original ingredient is refused by the real menu: " + upgrade.wrong());
            table.setItem(1, new ItemStack(upgrade.ingredient(), 2));
            ItemStack poor = wand(5000); String expensive = upgrade.kind() == BlockTube.Kind.RESTRICT ? "terra" : "ordo";
            poor.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(poor).withAmount(expensive, upgrade.vis().get(expensive) * 110 - 1));
            table.setItem(10, poor); menu.broadcastChanges(); refuse(f, table, menu, "one short priced primal refuses the entire native upgrade");
            table.setItem(10, wand(5000)); menu.broadcastChanges();
            var match = ArcaneCrafting.find(f.player, table);
            f.check(match != null && match.arcane() && match.recipe().id().equals(holder.id()) && menu.craftable(), "generic ArcaneCrafting matches the actual upgrade without a special menu path");
            for (String primal : WandData.PRIMALS) f.check(menu.requiredVis(primal) == upgrade.vis().getOrDefault(primal, 0) * 110, "original iron-cap price for " + upgrade.id() + "/" + primal);
            menu.clicked(0, 0, ClickType.PICKUP, f.player); ItemStack earned = menu.getCarried().copy(); menu.setCarried(ItemStack.EMPTY);
            f.check(earned.is(item(upgrade.kind())) && earned.getCount() == 1 && table.getItem(8).getCount() == 1 && table.getItem(1).getCount() == 1,
                    "one output consumes exactly one per occupied cell, not whole stacks");
            f.player.getInventory().add(earned);
            var paid = table.saveWithoutMetadata(f.level.registryAccess()); KnowledgeAccess.get(f.player).removeResearch("TUBEFILTER");
            f.check(menu.quickMoveStack(f.player, 0).isEmpty() && paid.equals(table.saveWithoutMetadata(f.level.registryAccess())), "stale preview rechecks the actual actor's upgrade research");
            KnowledgeAccess.get(f.player).addResearch("TUBEFILTER"); menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
            f.check(f.player.getInventory().countItem(item(upgrade.kind())) == 2 && table.getItem(1).isEmpty() && table.getItem(8).isEmpty()
                    && menu.quickMoveStack(f.player, 0).isEmpty(), "native quick-move pays the one remaining craft and cannot replay it");
            for (String primal : WandData.PRIMALS) f.check(ItemWandCasting.getVis(table.getItem(10), primal) == 5000 - upgrade.vis().getOrDefault(primal, 0) * 220,
                    "two real crafts debit only their canonical primals: " + upgrade.id() + "/" + primal);
        }
        table.clearContent(); f.player.getInventory().clearContent();
        ItemStack catalyst = ItemEssence.filled("potentia");
        catalyst.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("actual-reusable-phial-components"));
        ItemStack catalystBefore = catalyst.copy(), payer = wand(5000);
        table.setItem(0, ItemJarLabel.blank()); table.setItem(8, catalyst); table.setItem(10, payer);
        menu.broadcastChanges();
        var binding = ArcaneCrafting.find(f.player, table);
        f.check(binding != null && !binding.arcane() && binding.recipe().id().equals(id("jar_label_bind"))
                && binding.prices().isEmpty(), "actual ordinary label binding runs through the same native workbench without a vis charge");
        menu.clicked(0, 0, ClickType.PICKUP, f.player);
        f.check(menu.getCarried().getCount() == 1 && ItemJarLabel.aspect(menu.getCarried()) == Aspect.ENERGY
                && f.player.getInventory().countItem(ThaumcraftContent.FILLED_PHIAL.get()) == 1
                && f.player.getInventory().items.stream().anyMatch(stack -> ItemStack.matches(stack, catalystBefore))
                && table.getItem(0).isEmpty() && table.getItem(8).isEmpty() && ItemStack.matches(table.getItem(10), payer),
                "original inventory-FIRST remainder returns exactly one unchanged component-bearing filled phial; no fake matrix remainder, vis debit or copied catalyst");
        menu.setCarried(ItemStack.EMPTY);
        f.player.closeContainer();
    }

    private static void restrict(Fixture f) {
        f.reset(); TileTube restrict = f.tube(f.base, BlockTube.Kind.RESTRICT), down = f.tube(f.base.below(), BlockTube.Kind.BASIC);
        down.setSuction(Aspect.WATER, 64); f.calculate(restrict);
        f.check(restrict.getSuctionAmount(null) == 32, "restrict uses neighbor64/2, not ordinary decrement");
        TileTube up = f.tube(f.base.above(), BlockTube.Kind.BASIC); up.setSuction(Aspect.WATER, 35); f.calculate(restrict);
        f.check(restrict.getSuctionAmount(null) == 17 && restrict.getSuctionType(null) == Aspect.WATER, "native DOWN64 then UP35 overwrites reduced32 with integer17; never max(all)/2");
        up.setSuction(Aspect.WATER, 33); f.calculate(restrict);
        f.check(restrict.getSuctionAmount(null) == 32, "candidate33 equals reduced32+1 and is strictly refused");
        down.setSuction(null, 0); up.setSuction(Aspect.WATER, 35); f.calculate(restrict);
        f.check(restrict.getSuctionAmount(null) == 17, "odd pressure35 truncates integer division, not rounding");
        up.setSuction(null, 0); f.calculate(restrict);
        f.check(restrict.getSuctionAmount(null) == 0 && restrict.getSuctionType(null) == null, "restrict also resets stale pressure with no neighbor source");
        for (BlockTube.Kind kind : List.of(BlockTube.Kind.BASIC, BlockTube.Kind.VALVE)) {
            f.reset(); TileTube ordinary = f.tube(f.base, kind); ordinary.setFacing(Direction.EAST);
            f.tube(f.base.below(), BlockTube.Kind.BASIC).setSuction(Aspect.WATER, 64);
            f.tube(f.base.above(), BlockTube.Kind.BASIC).setSuction(Aspect.WATER, 35); f.calculate(ordinary);
            f.check(ordinary.getSuctionAmount(null) == 63 && !ordinary.isRestrict(), "basic/open-valve policy is unchanged by variant dispatch: " + kind);
        }
    }

    private static void filter(Fixture f) {
        f.reset(); TileTube filter = f.tube(f.base, BlockTube.Kind.FILTER); f.bind(filter, Aspect.WATER); f.calculate(filter);
        f.check(filter.getFilter() == Aspect.WATER && filter.getSuctionAmount(null) == 0 && filter.getSuctionType(null) == null, "configured filter in an empty network invents neither pressure nor suction type");
        TileTube neighbor = f.tube(f.base.below(), BlockTube.Kind.BASIC); neighbor.setSuction(null, 64); f.calculate(filter);
        f.check(filter.getSuctionAmount(null) == 63 && filter.getSuctionType(null) == Aspect.WATER, "untyped neighbor pressure becomes the configured filter type");
        neighbor.setSuction(Aspect.FIRE, 64); f.calculate(filter);
        f.check(filter.getSuctionAmount(null) == 0 && filter.getSuctionType(null) == null, "wrong typed neighbor cannot project pressure through a water filter");
        neighbor.setSuction(Aspect.WATER, 64); f.calculate(filter);
        f.check(filter.getSuctionAmount(null) == 63 && filter.getSuctionType(null) == Aspect.WATER, "matching typed pressure passes normally");
        f.check(filter.addEssentia(Aspect.FIRE, 99, Direction.EAST) == 1, "filter is NOT a new hard low-level add type gate"); f.calculate(filter);
        f.check(filter.getEssentiaType(null) == Aspect.FIRE && filter.getEssentiaAmount(null) == 1 && filter.getSuctionAmount(null) == 0,
                "old conflicting buffered unit is retained and blocks matching-but-conflicting TYPED suction");
        neighbor.setSuction(null, 64); f.calculate(filter);
        f.check(filter.getSuctionAmount(null) == 63 && filter.getSuctionType(null) == Aspect.WATER && filter.getEssentiaType(null) == Aspect.FIRE,
                "original untyped-pressure inference does not purge or reinterpret an incompatible existing buffer");
        var marker = ((IAspectContainer) filter).getAspects();
        f.check(marker != null && marker.size() == 1 && marker.getAmount(Aspect.WATER) == -1 && filter.getStoredEssentiaAmount() == 1,
                "filter:-1 is only an API marker, never the physical raw storage amount");
        neighbor.setSuction(null, 0);
        f.check(EssentiaTransfers.pullOne(neighbor, Direction.UP, Aspect.FIRE) == 1 && neighbor.getEssentiaAmount(null) == 1
                && filter.getEssentiaAmount(null) == 0 && filter.getFilter() == Aspect.WATER,
                "real paired storage transfers the actual fire unit, not marker water/-1; configuration is retained");
        f.check(filter.addEssentia(Aspect.FIRE, 99, Direction.EAST) == 1 && filter.takeEssentia(Aspect.FIRE, 99, Direction.WEST) == 1,
                "external raw add/take remain one-unit operations regardless of configured filter");
    }

    private static void oneWay(Fixture f) {
        f.reset(); TileTube oneWay = f.tube(f.base, BlockTube.Kind.ONEWAY); oneWay.setFacing(Direction.EAST);
        f.tube(f.base.west(), BlockTube.Kind.BASIC).setSuction(Aspect.WATER, 64);
        f.tube(f.base.east(), BlockTube.Kind.BASIC).setSuction(Aspect.ENERGY, 100); f.calculate(oneWay);
        f.check(oneWay.getSuctionAmount(null) == 63 && oneWay.getSuctionType(null) == Aspect.WATER, "oneway pressure reads facing.opposite WEST, not facing EAST or the largest other pressure");
        for (Direction input : Direction.values()) {
            f.reset(); oneWay = f.tube(f.base, BlockTube.Kind.ONEWAY); oneWay.setFacing(Direction.EAST);
            TileTube pressure = f.tube(f.base.west(), BlockTube.Kind.BASIC); pressure.setSuction(Aspect.WATER, 64);
            f.phase(oneWay, 10, 4); // The next native tick pulls WITHOUT recalculating suction.
            f.check(oneWay.getSuctionAmount(null) == 63 && oneWay.getSuctionType(null) == Aspect.WATER, "native phase4 has actually calculated pressure; no saved count/clock rewrite");
            pressure.setSuction(null, 0); pressure.addEssentia(Aspect.WATER, 1, Direction.UP);
            TileTube source = input == Direction.WEST ? pressure : f.tube(f.base.relative(input), BlockTube.Kind.BASIC);
            if (source != pressure) source.addEssentia(Aspect.WATER, 1, Direction.UP);
            f.tick(oneWay);
            f.check(pressure.getEssentiaAmount(null) == 1 && oneWay.getEssentiaAmount(null) == (input == Direction.WEST ? 0 : 1)
                    && source.getEssentiaAmount(null) == (input == Direction.WEST ? 1 : 0),
                    "pull scan excludes ONLY the pressure side, while accepting every other raw face: " + input);
            for (Direction face : Direction.values()) f.check(oneWay.isConnectable(face) && oneWay.canInputFrom(face) && oneWay.canOutputTo(face), "oneway remains a six-raw-port transport, not a two-port diode: " + face);
        }
        f.reset(); oneWay = f.tube(f.base, BlockTube.Kind.ONEWAY); oneWay.setFacing(Direction.EAST);
        f.check(oneWay.addEssentia(Aspect.FIRE, 9, Direction.WEST) == 1 && oneWay.takeEssentia(Aspect.FIRE, 9, Direction.WEST) == 1,
                "even the excluded network-pull face still supports canonical external raw add/take");
    }

    private static void venting(Fixture f) {
        f.reset(); TileTube tube = f.tube(f.base, BlockTube.Kind.FILTER); f.bind(tube, Aspect.WATER);
        f.tube(f.base.below(), BlockTube.Kind.BASIC).setSuction(Aspect.WATER, 64);
        TileTube competing = f.tube(f.base.above(), BlockTube.Kind.BASIC);
        f.phase(tube, 10, 9); competing.setSuction(Aspect.ENERGY, 63);
        f.put(f.base.east(), ThaumcraftAlembics.ALEMBIC.get()); var source = (TileAlembic) f.level.getBlockEntity(f.base.east()); source.addToContainer(Aspect.WATER, 1);
        f.tick(tube); int phase = tube.getTickCount();
        f.check(phase % 10 == 0 && tube.getVenting() == 40 && tube.getEssentiaType(null) == Aspect.WATER
                && tube.getEssentiaAmount(null) == 1 && source.getAmount() == 0, "filter uses the SAME local suction/vent/pull engine: conflict starts40 yet that very tick still pulls exactly one real unit");
        f.tick(tube);
        f.check(tube.getTickCount() == phase && tube.getVenting() == 39 && tube.takeEssentia(Aspect.WATER, 1, Direction.SOUTH) == 1
                && tube.addEssentia(Aspect.FIRE, 1, Direction.SOUTH) == 1, "vent freezes phase, not ordinary raw one-unit access or filter buffer identity");
        competing.setSuction(null, 0); f.ticks(tube, tube.getVenting());
        f.check(tube.getVenting() == 0 && tube.getTickCount() == phase + 1 && tube.getEssentiaType(null) == Aspect.FIRE && tube.getEssentiaAmount(null) == 1,
                "only vent1->0 resumes the unchanged native local counter, without deleting incompatible stock");
    }

    private static void persistenceAndDrops(Fixture f) {
        for (BlockTube.Kind kind : BlockTube.Kind.values()) {
            f.reset(); TileTube tube = f.tube(f.base, kind);
            f.check(tube.kind() == kind && ((BlockTube) tube.getBlockState().getBlock()).kind() == kind
                    && (tube instanceof IAspectContainer) == (kind == BlockTube.Kind.FILTER), "fresh native factory derives kind from block; ONLY filter exposes IAspectContainer: " + kind);
            if (kind == BlockTube.Kind.FILTER) f.bind(tube, Aspect.WATER);
            tube.setFacing(Direction.EAST); tube.setSideOpen(Direction.UP, false); tube.setSuction(Aspect.FIRE, 17);
            f.check(tube.addEssentia(Aspect.FIRE, 99, Direction.SOUTH) == 1, "every variant keeps exactly one bounded raw fixture unit");
            var saved = tube.saveWithFullMetadata(f.level.registryAccess()); var update = tube.getUpdateTag(f.level.registryAccess());
            f.check(saved.getInt("amount") == 1 && saved.getInt("samount") == 17 && !update.contains("samount") && !update.contains("stype")
                    && !saved.contains("count") && !saved.contains("venting") && !saved.contains("rotation")
                    && (kind != BlockTube.Kind.FILTER || saved.getString("AspectFilter").equals("aqua") && update.getString("AspectFilter").equals("aqua")),
                    "native disk/update retain buffer/filter/config while omitting authoritative suction from custom updates and transient phases");
            TileTube mirror = ThaumcraftTubes.TUBE_BLOCK_ENTITY.get().create(f.base, tube.getBlockState());
            f.check(mirror != null && (mirror instanceof IAspectContainer) == (kind == BlockTube.Kind.FILTER), "registered native update-target factory also preserves the filter-only interface");
            mirror.setSuction(Aspect.ENERGY, 9); mirror.loadWithComponents(update, f.level.registryAccess());
            f.check(mirror.getSuctionType(null) == Aspect.ENERGY && mirror.getSuctionAmount(null) == 9
                    && mirror.getEssentiaType(null) == Aspect.FIRE && mirror.getEssentiaAmount(null) == 1
                    && mirror.getFilter() == (kind == BlockTube.Kind.FILTER ? Aspect.WATER : null) && !mirror.isSideOpen(Direction.UP),
                    "custom update applies actual storage/filter/config while leaving the receiver's local suction untouched");
            saved.putString("kind", kind == BlockTube.Kind.FILTER ? "basic" : "filter");
            saved.putBoolean("valve", true); saved.putBoolean("restrict", true); saved.putBoolean("oneway", true);
            if (kind != BlockTube.Kind.FILTER) saved.putString("AspectFilter", "aqua");
            TileTube stale = tube; f.level.removeBlockEntity(f.base);
            BlockEntity loaded = BlockEntity.loadStatic(f.base, f.level.getBlockState(f.base), saved, f.level.registryAccess());
            f.check(loaded instanceof TileTube && (loaded instanceof IAspectContainer) == (kind == BlockTube.Kind.FILTER), "actual BlockEntity.loadStatic/registered BEType factory preserves the filter-only public interface: " + kind);
            f.level.setBlockEntity(loaded); tube = (TileTube) loaded;
            f.check(tube.kind() == kind && tube.getFilter() == (kind == BlockTube.Kind.FILTER ? Aspect.WATER : null)
                    && tube.getFacing() == Direction.EAST && !tube.isSideOpen(Direction.UP) && tube.getEssentiaType(null) == Aspect.FIRE
                    && tube.getEssentiaAmount(null) == 1 && tube.getSuctionAmount(null) == 17 && tube.getTickCount() == 0 && tube.getVenting() == 0
                    && tube.getUpdatePacket() != null && stale.takeEssentia(Aspect.FIRE, 1, Direction.SOUTH) == 0,
                    "forged kind flags/AspectFilter cannot grant variant behavior; native load preserves real fields and rejects stale storage identity");
            if (kind == BlockTube.Kind.FILTER) f.check(((IAspectContainer) tube).getAspects().getAmount(Aspect.WATER) == -1, "marker survives actual native deserialization independently of buffered fire");
            f.check(tube.getBlockState().getAnalogOutputSignal(f.level, f.base) == 0 && f.level.destroyBlock(f.base, true, f.player), "filled configured variant breaks natively with comparator0");
            f.flushFresh();
            var own = f.drops().stream().filter(drop -> drop.getItem().is(item(kind))).toList();
            f.check(own.size() == 1 && ItemStack.matches(own.getFirst().getItem(), new ItemStack(item(kind)))
                    && f.blankDrops() == (kind == BlockTube.Kind.FILTER ? 1 : 0), "break returns plain OWN tube plus exactly one BLANK only for a configured filter; no stored/configured tube item: " + kind);
            if (kind == BlockTube.Kind.FILTER) f.check(tube.getFilter() == null, "broken filter's old BE clears label ownership BEFORE durable return");
            ItemStack plain = own.getFirst().getItem().copy(); own.getFirst().discard();
            f.put(f.base.below(), Blocks.STONE); f.player.setItemInHand(InteractionHand.MAIN_HAND, plain); f.player.setShiftKeyDown(false); f.aim(Vec3.atCenterOf(f.base.below()));
            f.check(f.use(f.base.below(), Direction.UP, InteractionHand.MAIN_HAND).consumesAction() && f.level.getBlockEntity(f.base) instanceof TileTube placed
                    && placed.kind() == kind && placed.getEssentiaAmount(null) == 0 && placed.getFilter() == null && placed.getFacing() == Direction.UP
                    && placed.isSideOpen(Direction.UP) && (!placed.isValve() || placed.isFlowAllowed()), "own plain drop places with fresh defaults, no old buffer/config, and clicked-face orientation: " + kind);
        }
    }

    static BlockTube block(BlockTube.Kind kind) {
        return switch (kind) {
            case BASIC -> ThaumcraftTubes.ESSENTIA_TUBE.get(); case VALVE -> ThaumcraftTubes.ESSENTIA_VALVE.get();
            case FILTER -> ThaumcraftTubes.ESSENTIA_FILTER.get(); case RESTRICT -> ThaumcraftTubes.ESSENTIA_RESTRICT.get(); case ONEWAY -> ThaumcraftTubes.ESSENTIA_ONEWAY.get();
        };
    }
    static Item item(BlockTube.Kind kind) { return block(kind).asItem(); }
    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }
    private static ArcaneRecipeInput upgradeInput(int tube, int second, Item ingredient, int count) {
        var grid = NonNullList.withSize(9, ItemStack.EMPTY); grid.set(tube, new ItemStack(ThaumcraftTubes.ESSENTIA_TUBE_ITEM.get(), count)); grid.set(second, new ItemStack(ingredient, count)); return new ArcaneRecipeInput(grid);
    }
    private static void matrix(TileArcaneWorkbench table, ArcaneRecipeInput input) { for (int i = 0; i < 9; i++) table.setItem(i, input.getItem(i)); }
    private static ItemStack wand(int amount) {
        var stack = new ItemStack(ThaumcraftContent.WAND.get()); var vis = new LinkedHashMap<String, Integer>();
        for (String tag : WandData.PRIMALS) vis.put(tag, amount);
        stack.set(ThaumcraftContent.WAND_DATA.get(), new WandData("greatwood", "iron", vis)); return stack;
    }
    private static void refuse(Fixture f, TileArcaneWorkbench table, ContainerArcaneWorkbench menu, String why) {
        var before = table.saveWithoutMetadata(f.level.registryAccess()); menu.clicked(0, 0, ClickType.PICKUP, f.player);
        f.check(menu.getCarried().isEmpty() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), why);
    }

    /** Shared TEST-ONLY fixture for the two new source files; never a production API. */
    static final class Fixture implements AutoCloseable {
        static final String QUEUE_ID = "thaumcraft_pending_inventory_drops";
        static final SavedData.Factory<DeferredItemDrops> QUEUE = new SavedData.Factory<>(DeferredItemDrops::new, DeferredItemDrops::load, null);
        final GameTestHelper test; final ServerLevel level; final ServerPlayer player; final BlockPos base; final AABB bounds;
        final Connection connection = new Connection(PacketFlow.SERVERBOUND); final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final Map<BlockPos, BlockState> states = new LinkedHashMap<>(); final Map<BlockPos, BlockEntity> originals = new LinkedHashMap<>();
        final Set<UUID> oldDrops, originalQueue = new HashSet<>(), ownedReturns = new HashSet<>(), ownedDrops = new HashSet<>(); boolean touched, closed;
        Fixture(GameTestHelper test) {
            this.test = test; level = test.getLevel(); base = test.absolutePos(new BlockPos(3, 3, 3)); bounds = new AABB(base).inflate(8);
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-2, -2, -2), base.offset(3, 3, 3))) {
                var pos = mutable.immutable(); states.put(pos, level.getBlockState(pos)); if (level.getBlockEntity(pos) != null) originals.put(pos, level.getBlockEntity(pos));
            }
            oldDrops = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
            var queue = level.getDataStorage().get(QUEUE, QUEUE_ID); if (queue != null) originalQueue.addAll(queue.pendingIds());
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "m06b-" + UUID.randomUUID().toString().substring(0, 8)), ClientInformation.createDefault());
        }
        void open() {
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            check(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player, "owned real connected native actor belongs to this test level");
            player.setNoGravity(true); mode(GameType.SURVIVAL); touched = true; reset();
        }
        void mode(GameType mode) { player.setGameMode(mode); check(player.isCreative() == (mode == GameType.CREATIVE), "actual native actor, not always-creative GameTest mock: " + mode); }
        void reset() {
            check(pendingRows().isEmpty(), "previous owned operation has no silently discarded pending label");
            player.containerMenu.setCarried(ItemStack.EMPTY); player.closeContainer(); player.getInventory().clearContent(); player.getInventory().selected = 0; player.setShiftKeyDown(false); mode(GameType.SURVIVAL);
            for (BlockPos pos : states.keySet()) level.removeBlockEntity(pos);
            for (BlockPos pos : states.keySet()) level.setBlock(pos, (pos.getY() == base.getY() - 2 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), QUIET);
            drops().forEach(ItemEntity::discard); aim(Vec3.atCenterOf(base)); chat();
        }
        void put(BlockPos pos, Block block) { level.removeBlockEntity(pos); level.setBlock(pos, block.defaultBlockState(), QUIET); }
        TileTube tube(BlockPos pos, BlockTube.Kind kind) { put(pos, block(kind)); return (TileTube) level.getBlockEntity(pos); }
        void check(boolean value, String why) { test.assertTrue(value, why); }
        void bind(TileTube tube, Aspect aspect) {
            player.setShiftKeyDown(false); player.setItemInHand(InteractionHand.MAIN_HAND, ItemJarLabel.bound(aspect.getTag())); aim(Vec3.atCenterOf(tube.getBlockPos()));
            check(use(tube.getBlockPos()).consumesAction() && tube.getFilter() == aspect && player.getMainHandItem().isEmpty(), "actual bound label pays one and configures only the native filter");
        }
        void tick(TileTube tube) {
            var pos = tube.getBlockPos(); var state = level.getBlockState(pos); var ticker = state.getTicker(level, ThaumcraftTubes.TUBE_BLOCK_ENTITY.get());
            if (ticker == null) throw new IllegalStateException("Missing actual shared variant ticker"); ticker.tick(level, pos, state, tube);
        }
        void ticks(TileTube tube, int count) { for (int i = 0; i < count; i++) tick(tube); }
        void phase(TileTube tube, int modulus, int remainder) {
            for (int i = 0; i < 24 && (tube.getTickCount() == 0 || tube.getTickCount() % modulus != remainder); i++) tick(tube);
            check(tube.getTickCount() > 0 && tube.getTickCount() % modulus == remainder && tube.getVenting() == 0, "bounded native ticker advancement reaches local phase " + remainder + "/" + modulus);
        }
        void calculate(TileTube tube) { phase(tube, 2, 1); tick(tube); }
        void aim(Vec3 target) { player.setPos(target.x, target.y - player.getEyeHeight(), target.z + 2); player.setDeltaMovement(Vec3.ZERO); player.setYRot(180); player.setYHeadRot(180); player.yHeadRotO = 180; player.setXRot(0); }
        BlockHitResult hit(BlockPos pos, Direction face) { return new BlockHitResult(Vec3.atCenterOf(pos).relative(face, .5), face, pos, false); }
        UseOnContext context(BlockPos pos) { return new UseOnContext(player, InteractionHand.MAIN_HAND, hit(pos, Direction.SOUTH)); }
        InteractionResult use(BlockPos pos) { return use(pos, Direction.SOUTH, InteractionHand.MAIN_HAND); }
        InteractionResult use(BlockPos pos, Direction side, InteractionHand hand) { return player.gameMode.useItemOn(player, level, player.getItemInHand(hand), hand, hit(pos, side)); }
        List<Component> chat() {
            connection.flushChannel(); channel.runPendingTasks(); var out = new ArrayList<Component>(); Object packet;
            while ((packet = channel.readOutbound()) != null) { try { if (packet instanceof ClientboundSystemChatPacket message) out.add(message.content()); } finally { ReferenceCountUtil.release(packet); } }
            return out;
        }
        List<ItemEntity> drops() {
            // Called while preparing/inspecting an owned synchronous operation,
            // not by asynchronous cleanup that could discover a neighbor's drop.
            var drops = level.getEntitiesOfClass(ItemEntity.class, bounds).stream().filter(entity -> !oldDrops.contains(entity.getUUID())).toList();
            drops.forEach(drop -> ownedDrops.add(drop.getUUID())); return drops;
        }
        int blankDrops() { return drops().stream().filter(drop -> ItemStack.isSameItemSameComponents(drop.getItem(), ItemJarLabel.blank())).mapToInt(drop -> drop.getItem().getCount()).sum(); }
        List<CompoundTag> pendingRows() {
            var queue = level.getDataStorage().get(QUEUE, QUEUE_ID); var rows = new ArrayList<CompoundTag>(); if (queue == null) return rows;
            for (Tag raw : queue.save(new CompoundTag(), level.registryAccess()).getList("Pending", Tag.TAG_COMPOUND)) {
                var row = (CompoundTag) raw; UUID id = row.getUUID("Id");
                if (!originalQueue.contains(id) && states.containsKey(BlockPos.of(row.getLong("Source")))) { rows.add(row.copy()); ownedReturns.add(id); }
            }
            return rows;
        }
        void flushFresh() { pendingRows(); DeferredItemDrops.tick(level); check(pendingRows().isEmpty(), "fresh native durable delivery leaves no owned pending output"); }
        @Override public void close() {
            if (closed) return; closed = true;
            try {
                player.containerMenu.setCarried(ItemStack.EMPTY); player.closeContainer(); player.getInventory().clearContent();
                if (touched) {
                    // Failure cleanup removes only this fixture's recorded new
                    // queue IDs, never another test's pending items or SavedData.
                    if (!pendingRows().isEmpty()) {
                        var queue = level.getDataStorage().get(QUEUE, QUEUE_ID); var saved = queue.save(new CompoundTag(), level.registryAccess()); var rows = saved.getList("Pending", Tag.TAG_COMPOUND);
                        for (int i = rows.size() - 1; i >= 0; i--) if (ownedReturns.contains(rows.getCompound(i).getUUID("Id"))) rows.remove(i);
                        level.getDataStorage().set(QUEUE_ID, DeferredItemDrops.load(saved, level.registryAccess()));
                    }
                    for (UUID id : ownedDrops) if (level.getEntity(id) instanceof ItemEntity drop && !oldDrops.contains(id)) drop.discard();
                    for (UUID id : ownedReturns) if (level.getEntity(id) instanceof ItemEntity drop && !oldDrops.contains(id)) drop.discard();
                    for (BlockPos pos : states.keySet()) level.removeBlockEntity(pos);
                    states.forEach((pos, state) -> level.setBlock(pos, state, QUIET)); originals.values().forEach(tile -> { tile.clearRemoved(); level.setBlockEntity(tile); });
                }
            } finally {
                try { if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player); }
                finally { player.discard(); channel.finishAndReleaseAll(); }
            }
        }
    }
}
