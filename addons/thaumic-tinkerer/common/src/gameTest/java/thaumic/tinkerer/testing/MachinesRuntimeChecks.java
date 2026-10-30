/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart, see addons/thaumic-tinkerer/NOTICE.md): test-only
 * code, distributed under the same licence as the add-on (CC BY-NC-SA 3.0).
 */
package thaumic.tinkerer.testing;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.architectury.event.EventResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.research.ResearchCategories;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.block.BlockMagnet;
import thaumic.tinkerer.common.block.BlockRepairer;
import thaumic.tinkerer.common.block.tile.TileAspectAnalyzer;
import thaumic.tinkerer.common.block.tile.TileFunnel;
import thaumic.tinkerer.common.block.tile.TileMagnet;
import thaumic.tinkerer.common.block.tile.TileMobMagnet;
import thaumic.tinkerer.common.block.tile.TileRepairer;
import thaumic.tinkerer.common.block.tile.TileSummon;
import thaumic.tinkerer.common.block.tile.container.ContainerAspectAnalyzer;
import thaumic.tinkerer.common.block.tile.container.ContainerMobMagnet;
import thaumic.tinkerer.common.content.Machines;
import thaumic.tinkerer.common.core.helper.EnumMobAspect;
import thaumic.tinkerer.common.hooks.TinkererEvents;
import thaumic.tinkerer.common.item.ItemBloodSword;
import thaumic.tinkerer.common.item.ItemMobAspect;
import thaumic.tinkerer.common.item.ItemSoulMould;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.TTRecipes;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * <p>Runtime checks of the Machines group in a running server: registration, the interaction of the repairer and the
 * funnel, the funnel's transfer between two jars, the magnets, the necromancy tablet, the cursed blade and the
 * persistence of the block entities. They were written without being able to run them (no Gradle in the porting
 * session): the first run may show a wrong assumption of a check as well as a defect.</p>
 */
public final class MachinesRuntimeChecks {
    private MachinesRuntimeChecks() {}

    // ---- helpers ---------------------------------------------------------------------------------------------------

    /** A floor of stone at relative y = 0, so that items and mobs stand. */
    private static void floor(GameTestHelper test) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) test.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static <T extends BlockEntity> T tile(GameTestHelper test, BlockPos relative, Class<T> type) {
        BlockEntity entity = test.getLevel().getBlockEntity(test.absolutePos(relative));
        test.assertTrue(type.isInstance(entity), "a " + type.getSimpleName() + " at " + relative + ", found " + entity);
        return type.cast(entity);
    }

    /**
     * A mock player in the corner of the test area, away from what the checks place. {@code makeMockServerPlayerInLevel}
     * leaves the player where the world spawns, which is neither near the blocks (menus check the distance) nor out of
     * the way of the items (a player picks up the items lying around).
     */
    private static ServerPlayer mockPlayer(GameTestHelper test) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        player.setPos(test.absoluteVec(new Vec3(6.5, 1.0, 6.5)));
        return player;
    }

    /** Removes a mock player (it would stay in the world and pick up what later checks drop) and ends the check. */
    private static void finish(GameTestHelper test, ServerPlayer player) {
        player.discard();
        test.succeed();
    }

    private static double distance(ItemEntity item, Vec3 point) {
        return item.position().distanceTo(point);
    }

    private static ItemStack filledJar(Aspect aspect, int amount) {
        ItemStack jar = new ItemStack(BlocksTC.jar().asItem());
        ((IEssentiaContainerItem) jar.getItem()).setAspects(jar, new AspectList().add(aspect, amount));
        return jar;
    }

    // ---- registration ----------------------------------------------------------------------------------------------

    public static void registrationIsComplete(GameTestHelper test) {
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());
        for (String block : List.of("aspect_analyzer", "repairer", "funnel", "magnet", "mob_magnet", "spawner")) {
            test.assertTrue(BuiltInRegistries.BLOCK.containsKey(LibMisc.id(block)), "block " + block);
            test.assertTrue(BuiltInRegistries.ITEM.containsKey(LibMisc.id(block)), "block item " + block);
        }
        for (String item : List.of(LibItemNames.SOUL_MOULD, LibItemNames.BLOOD_SWORD, LibItemNames.MOB_DISPLAY)) {
            test.assertTrue(BuiltInRegistries.ITEM.containsKey(LibMisc.id(item)), "item " + item);
        }
        int soulAspects = 0;
        for (Aspect aspect : ItemMobAspect.ASPECTS) {
            for (ItemMobAspect.Form form : ItemMobAspect.Form.values()) {
                test.assertTrue(TTRegistry.hasItem(ItemMobAspect.path(aspect, form)), ItemMobAspect.path(aspect, form));
                soulAspects++;
            }
        }
        test.assertTrue(soulAspects == 51, "51 soul aspect items");
        test.assertTrue(Machines.MENU_ASPECT_ANALYZER.isPresent() && Machines.MENU_MOB_MAGNET.isPresent(), "menus");
        test.assertTrue(Machines.TILE_SUMMON.isPresent() && Machines.TILE_FUNNEL.isPresent(), "block entities");

        for (String key : List.of(LibResearch.KEY_ASPECT_ANALYZER, LibResearch.KEY_REPAIRER, LibResearch.KEY_FUNNEL,
                LibResearch.KEY_MAGNETS, LibResearch.KEY_SUMMON, LibResearch.KEY_BLOOD_SWORD)) {
            test.assertTrue(ResearchCategories.getResearch(key) != null, "research " + key);
        }
        for (String recipe : List.of(LibResearch.KEY_ASPECT_ANALYZER, LibResearch.KEY_REPAIRER, LibResearch.KEY_FUNNEL,
                LibResearch.KEY_MAGNET, LibResearch.KEY_MOB_MAGNET, LibResearch.KEY_MAGNETS, LibResearch.KEY_SUMMON + "0",
                LibResearch.KEY_SUMMON, LibResearch.KEY_BLOOD_SWORD, ItemMobAspect.CONDENSE_RECIPE_NAME)) {
            test.assertTrue(TTRecipes.has(recipe), "recipe " + recipe);
        }
        test.succeed();
    }

    // ---- Thaumic Restorer ------------------------------------------------------------------------------------------

    public static void repairerTakesAndGivesBackADamagedTool(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 3);
        test.setBlock(pos, Machines.REPAIRER.get().defaultBlockState());
        TileRepairer repairer = tile(test, pos, TileRepairer.class);
        ServerPlayer player = mockPlayer(test);

        ItemStack mended = new ItemStack(Items.DIAMOND_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, mended);
        test.useBlock(pos, player);
        test.assertTrue(repairer.getItem(0).isEmpty(), "a pickaxe without damage is not taken");
        test.assertTrue(player.getMainHandItem() == mended, "and stays in the hand");

        ItemStack damaged = new ItemStack(Items.DIAMOND_PICKAXE);
        damaged.setDamageValue(500);
        player.setItemInHand(InteractionHand.MAIN_HAND, damaged);
        test.useBlock(pos, player);
        test.assertTrue(repairer.getItem(0).is(Items.DIAMOND_PICKAXE) && repairer.getItem(0).getDamageValue() == 500,
                "a damaged pickaxe is taken");
        test.assertTrue(player.getMainHandItem().isEmpty(), "out of the hand");
        test.assertTrue(repairer.isPowering(), "a damaged tool in the slot: the repairer gives redstone power");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        test.useBlock(pos, player);
        test.assertTrue(repairer.getItem(0).isEmpty(), "an empty hand takes the tool back");
        test.assertTrue(player.getInventory().contains(stack -> stack.is(Items.DIAMOND_PICKAXE) && stack.getDamageValue() == 500),
                "into the inventory, still damaged");
        test.assertTrue(!repairer.isPowering(), "no power without a tool");
        finish(test, player);
    }

    public static void repairerAutomationTakesDamagedToolsAndGivesMendedOnes(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 3);
        test.setBlock(pos, Machines.REPAIRER.get().defaultBlockState());
        TileRepairer repairer = tile(test, pos, TileRepairer.class);
        ItemStack damaged = new ItemStack(Items.IRON_SWORD);
        damaged.setDamageValue(10);
        test.assertTrue(repairer.canPlaceItemThroughFace(0, damaged, Direction.UP), "hopper in: damaged");
        test.assertTrue(!repairer.canPlaceItemThroughFace(0, new ItemStack(Items.IRON_SWORD), Direction.UP), "mended is refused");
        test.assertTrue(!repairer.canTakeItemThroughFace(0, damaged, Direction.DOWN), "hopper out: not before it is mended");
        test.assertTrue(repairer.canTakeItemThroughFace(0, new ItemStack(Items.IRON_SWORD), Direction.DOWN), "mended may leave");
        test.assertTrue(repairer.getMaxStackSize() == 1, "one item");
        test.succeed();
    }

    public static void repairerFacesTheWayThePlayerLooked(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 3);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            test.setBlock(pos, Machines.REPAIRER.get().defaultBlockState().setValue(BlockRepairer.FACING, facing));
            TileRepairer repairer = tile(test, pos, TileRepairer.class);
            test.assertTrue(repairer.getOrientation() == facing, "orientation " + facing);
            test.assertTrue(repairer.isConnectable(facing) && !repairer.isConnectable(facing.getOpposite()), "tube side " + facing);
            test.assertTrue(repairer.getSuctionType(facing) == Aspect.TOOL && repairer.getSuctionType(facing.getOpposite()) == null,
                    "suction type");
            test.assertTrue(repairer.getSuctionAmount(facing) == 0, "no suction without a tool");
        }
        test.succeed();
    }

    public static void repairerSavesItsToolAndItsState(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 3);
        test.setBlock(pos, Machines.REPAIRER.get().defaultBlockState());
        TileRepairer repairer = tile(test, pos, TileRepairer.class);
        ItemStack damaged = new ItemStack(Items.DIAMOND_AXE);
        damaged.setDamageValue(77);
        repairer.setItem(0, damaged);
        repairer.tookLastTick = false;

        CompoundTag saved = repairer.saveWithFullMetadata(test.getLevel().registryAccess());
        TileRepairer loaded = new TileRepairer(test.absolutePos(pos), test.getLevel().getBlockState(test.absolutePos(pos)));
        loaded.loadWithComponents(saved, test.getLevel().registryAccess());
        test.assertTrue(loaded.getItem(0).is(Items.DIAMOND_AXE) && loaded.getItem(0).getDamageValue() == 77, "the tool with its damage");
        test.assertTrue(!loaded.tookLastTick, "the flag of the last tick");
        test.succeed();
    }

    // ---- Essentia Funnel -------------------------------------------------------------------------------------------

    /** Hopper at (2,1,3) pointing east, the funnel on top of it, the target jar east of the hopper. */
    private static TileFunnel funnelSetup(GameTestHelper test) {
        floor(test);
        test.setBlock(new BlockPos(2, 1, 3), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        test.setBlock(new BlockPos(2, 2, 3), Machines.FUNNEL.get().defaultBlockState());
        test.setBlock(new BlockPos(3, 1, 3), BlocksTC.jar().defaultBlockState());
        return tile(test, new BlockPos(2, 2, 3), TileFunnel.class);
    }

    public static void funnelNeedsAHopperUnderIt(GameTestHelper test) {
        floor(test);
        BlockState funnel = Machines.FUNNEL.get().defaultBlockState();
        test.assertTrue(!funnel.canSurvive(test.getLevel(), test.absolutePos(new BlockPos(2, 1, 3))), "not on the ground");
        test.setBlock(new BlockPos(2, 1, 3), Blocks.HOPPER);
        test.assertTrue(funnel.canSurvive(test.getLevel(), test.absolutePos(new BlockPos(2, 2, 3))), "on a hopper");
        test.succeed();
    }

    public static void funnelTakesFilledJarsOnly(GameTestHelper test) {
        TileFunnel funnel = funnelSetup(test);
        test.assertTrue(funnel.canPlaceItemThroughFace(0, filledJar(Aspect.FIRE, 5), Direction.UP), "a filled jar");
        test.assertTrue(!funnel.canPlaceItemThroughFace(0, new ItemStack(BlocksTC.jar().asItem()), Direction.UP), "a plain empty jar");
        test.assertTrue(!funnel.canPlaceItemThroughFace(0, new ItemStack(Items.GLASS_BOTTLE), Direction.UP), "a bottle");
        test.assertTrue(funnel.getSlotsForFace(Direction.DOWN).length == 0, "nothing can be taken from below");
        test.assertTrue(funnel.getSlotsForFace(Direction.UP).length == 1, "the slot from above");
        test.assertTrue(!funnel.canTakeItemThroughFace(0, filledJar(Aspect.FIRE, 5), Direction.DOWN), "not downwards");
        test.assertTrue(funnel.canTakeItemThroughFace(0, filledJar(Aspect.FIRE, 5), Direction.UP), "upwards");
        test.succeed();
    }

    public static void funnelMovesEssentiaToTheJarTheHopperPointsAt(GameTestHelper test) {
        TileFunnel funnel = funnelSetup(test);
        funnel.setItem(0, filledJar(Aspect.FIRE, 3));
        BlockPos target = new BlockPos(3, 1, 3);
        test.runAfterDelay(20, () -> {
            var jar = (thaumcraft.api.aspects.IAspectContainer) test.getLevel().getBlockEntity(test.absolutePos(target));
            test.assertTrue(jar != null && jar.getAspects().getAmount(Aspect.FIRE) == 3,
                    "all three points arrived, found " + (jar == null ? null : jar.getAspects().aspects));
            ItemStack left = funnel.getItem(0);
            test.assertTrue(((IEssentiaContainerItem) left.getItem()).getAspects(left) == null, "the funnel's jar is empty");
            test.assertTrue(!TileFunnel.isFilledJar(left) && left.getMaxStackSize() > 1,
                    "and it is a plain empty jar now (it had no label)");
            test.succeed();
        });
    }

    public static void funnelDoesNotCrashOnAJarLabelledWithAnotherAspect(GameTestHelper test) {
        TileFunnel funnel = funnelSetup(test);
        BlockPos target = new BlockPos(3, 1, 3);
        BlockEntity jar = test.getLevel().getBlockEntity(test.absolutePos(target));
        CompoundTag tag = jar.saveWithoutMetadata(test.getLevel().registryAccess());
        tag.putString("AspectFilter", Aspect.WATER.getTag());
        jar.loadCustomOnly(tag, test.getLevel().registryAccess());
        funnel.setItem(0, filledJar(Aspect.FIRE, 4));
        test.runAfterDelay(15, () -> {
            var container = (thaumcraft.api.aspects.IAspectContainer) test.getLevel().getBlockEntity(test.absolutePos(target));
            test.assertTrue(container.getAspects().size() == 0, "a jar labelled for water takes no fire");
            ItemStack stack = funnel.getItem(0);
            test.assertTrue(((IEssentiaContainerItem) stack.getItem()).getAspects(stack).getAmount(Aspect.FIRE) == 4,
                    "nothing left the funnel; 1.7.10 crashed the server here");
            test.succeed();
        });
    }

    public static void funnelDoesNotFillAJarOfAnotherAspect(GameTestHelper test) {
        TileFunnel funnel = funnelSetup(test);
        BlockPos target = new BlockPos(3, 1, 3);
        var container = (thaumcraft.api.aspects.IAspectContainer) test.getLevel().getBlockEntity(test.absolutePos(target));
        container.addToContainer(Aspect.WATER, 2);
        funnel.setItem(0, filledJar(Aspect.FIRE, 4));
        test.runAfterDelay(15, () -> {
            test.assertTrue(container.getAspects().getAmount(Aspect.WATER) == 2 && container.getAspects().getAmount(Aspect.FIRE) == 0,
                    "a jar with water takes no fire");
            test.succeed();
        });
    }

    public static void funnelSavesItsJar(GameTestHelper test) {
        TileFunnel funnel = funnelSetup(test);
        funnel.setItem(0, filledJar(Aspect.EARTH, 9));
        CompoundTag saved = funnel.saveWithFullMetadata(test.getLevel().registryAccess());
        TileFunnel loaded = new TileFunnel(funnel.getBlockPos(), funnel.getBlockState());
        loaded.loadWithComponents(saved, test.getLevel().registryAccess());
        ItemStack stack = loaded.getItem(0);
        test.assertTrue(TileFunnel.isFilledJar(stack), "a filled jar");
        test.assertTrue(((IEssentiaContainerItem) stack.getItem()).getAspects(stack).getAmount(Aspect.EARTH) == 9, "with its essentia");
        test.assertTrue(loaded.getAspects().getAmount(Aspect.EARTH) == 9, "shown to the goggles");
        test.succeed();
    }

    // ---- magnets ---------------------------------------------------------------------------------------------------

    private static void powerMagnet(GameTestHelper test, BlockPos magnet) {
        test.setBlock(magnet.north(), Blocks.REDSTONE_BLOCK);
    }

    public static void aKineticAttractorPullsItemsAndPushesThemAway(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 4);
        test.setBlock(pos, Machines.MAGNET.get().defaultBlockState());
        powerMagnet(test, pos);
        Vec3 center = Vec3.atCenterOf(test.absolutePos(pos));
        ItemEntity pulled = test.spawnItem(Items.APPLE, 3.5F, 1.5F, 7.5F);
        double before = distance(pulled, center);
        test.runAfterDelay(10, () -> {
            test.assertTrue(distance(pulled, center) < before - 1.0, "pulled: " + before + " -> " + distance(pulled, center));
            pulled.discard();

            // now it pushes
            test.setBlock(pos, Machines.MAGNET.get().defaultBlockState().setValue(BlockMagnet.PULLING, false));
            ItemEntity pushed = test.spawnItem(Items.APPLE, 3.5F, 1.5F, 6.0F);
            double start = distance(pushed, center);
            test.runAfterDelay(10, () -> {
                test.assertTrue(distance(pushed, center) > start + 0.5, "pushed: " + start + " -> " + distance(pushed, center));
                test.succeed();
            });
        });
    }

    public static void aMagnetWithoutSignalDoesNothing(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 4);
        test.setBlock(pos, Machines.MAGNET.get().defaultBlockState());
        Vec3 center = Vec3.atCenterOf(test.absolutePos(pos));
        ItemEntity item = test.spawnItem(Items.APPLE, 3.5F, 1.5F, 7.5F);
        double before = distance(item, center);
        test.runAfterDelay(10, () -> {
            test.assertTrue(Math.abs(distance(item, center) - before) < 0.3, "an unpowered magnet leaves the item alone");
            test.succeed();
        });
    }

    public static void aMagnetOnlyMovesItems(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 4);
        test.setBlock(pos, Machines.MAGNET.get().defaultBlockState());
        TileMagnet magnet = tile(test, pos, TileMagnet.class);
        Pig pig = test.spawn(EntityType.PIG, new BlockPos(3, 1, 6));
        ItemEntity item = test.spawnItem(Items.APPLE, 3.5F, 1.5F, 6.5F);
        test.assertTrue(magnet.isSelected(item), "items");
        test.assertTrue(!magnet.isSelected(pig), "not animals");
        test.assertTrue(magnet.isPulling(), "pulls when placed");
        test.succeed();
    }

    public static void aMagnetChangesPolarityWhenClicked(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 4);
        test.setBlock(pos, Machines.MAGNET.get().defaultBlockState());
        ServerPlayer player = mockPlayer(test);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        test.useBlock(pos, player);
        test.assertBlockProperty(pos, BlockMagnet.PULLING, false);
        test.useBlock(pos, player);
        test.assertBlockProperty(pos, BlockMagnet.PULLING, true);
        test.assertTrue(tile(test, pos, TileMagnet.class) != null, "the block entity survives the changes");
        finish(test, player);
    }

    public static void aCorporealAttractorChoosesByMouldAndAge(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 4);
        test.setBlock(pos, Machines.MOB_MAGNET.get().defaultBlockState());
        TileMobMagnet magnet = tile(test, pos, TileMobMagnet.class);
        Cow adultCow = test.spawn(EntityType.COW, new BlockPos(1, 1, 6));
        Cow calf = test.spawn(EntityType.COW, new BlockPos(2, 1, 6));
        calf.setBaby(true);
        Pig pig = test.spawn(EntityType.PIG, new BlockPos(4, 1, 6));
        ItemEntity item = test.spawnItem(Items.APPLE, 3.5F, 1.5F, 6.5F);
        ServerPlayer player = mockPlayer(test);

        test.assertTrue(!magnet.isSelected(item), "no items");
        test.assertTrue(!magnet.isSelected(player), "no players");
        test.assertTrue(magnet.adult, "searches adults at first");
        test.assertTrue(magnet.isSelected(adultCow) && magnet.isSelected(pig) && !magnet.isSelected(calf), "without a mould: every adult");

        magnet.adult = false;
        test.assertTrue(!magnet.isSelected(adultCow) && !magnet.isSelected(pig) && magnet.isSelected(calf), "children only");
        magnet.adult = true;

        // a mould set to the cow: the adult cow only
        ItemStack mould = TTRegistry.stack(LibItemNames.SOUL_MOULD);
        mould.getItem().interactLivingEntity(mould, player, adultCow, InteractionHand.MAIN_HAND);
        test.assertTrue("minecraft:cow".equals(ItemSoulMould.getPatternName(mould)), "the mould learned the cow: " + ItemSoulMould.getPatternName(mould));
        magnet.setItem(0, mould);
        test.assertTrue(magnet.isSelected(adultCow) && !magnet.isSelected(pig) && !magnet.isSelected(calf), "the adult cow");
        magnet.adult = false;
        test.assertTrue(!magnet.isSelected(adultCow) && magnet.isSelected(calf), "the calf");

        // a mould without a pattern selects nothing
        ItemStack blank = TTRegistry.stack(LibItemNames.SOUL_MOULD);
        magnet.setItem(0, blank);
        magnet.adult = true;
        test.assertTrue(!magnet.isSelected(adultCow) && !magnet.isSelected(pig), "an empty mould: nothing");
        finish(test, player);
    }

    public static void aCorporealAttractorMovesTheMobsItSelects(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 4);
        test.setBlock(pos, Machines.MOB_MAGNET.get().defaultBlockState());
        powerMagnet(test, pos);
        Cow cow = test.spawn(EntityType.COW, new BlockPos(3, 1, 7));
        double before = cow.position().distanceTo(Vec3.atCenterOf(test.absolutePos(pos)));
        test.runAfterDelay(10, () -> {
            double after = cow.position().distanceTo(Vec3.atCenterOf(test.absolutePos(pos)));
            test.assertTrue(after < before - 0.5, "the cow was pulled: " + before + " -> " + after);
            test.succeed();
        });
    }

    public static void theMobMagnetMenuSetsTheAdultFlagWithItsButtons(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 4);
        test.setBlock(pos, Machines.MOB_MAGNET.get().defaultBlockState());
        TileMobMagnet magnet = tile(test, pos, TileMobMagnet.class);
        ServerPlayer player = mockPlayer(test);
        ContainerMobMagnet menu = new ContainerMobMagnet(1, player.getInventory(), magnet);

        test.assertTrue(menu.isAdult(), "adult at first");
        test.assertTrue(menu.clickMenuButton(player, ContainerMobMagnet.BUTTON_CHILD) && !magnet.adult && !menu.isAdult(), "child");
        test.assertTrue(menu.clickMenuButton(player, ContainerMobMagnet.BUTTON_ADULT) && magnet.adult && menu.isAdult(), "adult");
        test.assertTrue(!menu.clickMenuButton(player, 7), "no such button");

        // only moulds go into the slot
        test.assertTrue(menu.getSlot(0).mayPlace(TTRegistry.stack(LibItemNames.SOUL_MOULD)), "a mould");
        test.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.STICK)), "not a stick");
        test.assertTrue(menu.getSlot(0).getMaxStackSize() == 1, "one mould");
        test.assertTrue(menu.stillValid(player), "near");
        finish(test, player);
    }

    public static void theMobMagnetSavesItsMouldAndItsFlag(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 4);
        test.setBlock(pos, Machines.MOB_MAGNET.get().defaultBlockState());
        TileMobMagnet magnet = tile(test, pos, TileMobMagnet.class);
        ServerPlayer player = mockPlayer(test);
        Pig pig = test.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        ItemStack mould = TTRegistry.stack(LibItemNames.SOUL_MOULD);
        mould.getItem().interactLivingEntity(mould, player, pig, InteractionHand.MAIN_HAND);
        magnet.setItem(0, mould);
        magnet.setAdult(false);

        CompoundTag saved = magnet.saveWithFullMetadata(test.getLevel().registryAccess());
        TileMobMagnet loaded = new TileMobMagnet(magnet.getBlockPos(), magnet.getBlockState());
        loaded.loadWithComponents(saved, test.getLevel().registryAccess());
        test.assertTrue(!loaded.adult, "the flag");
        test.assertTrue("minecraft:pig".equals(ItemSoulMould.getPatternName(loaded.getItem(0))), "the pattern of the mould");
        finish(test, player);
    }

    public static void aSoulMouldLearnsAndForgets(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test);
        Pig pig = test.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        ItemStack mould = TTRegistry.stack(LibItemNames.SOUL_MOULD);
        test.assertTrue(mould.getMaxStackSize() == 1, "does not stack");
        test.assertTrue(ItemSoulMould.getPatternName(mould).isEmpty(), "blank");
        mould.getItem().interactLivingEntity(mould, player, pig, InteractionHand.MAIN_HAND);
        test.assertTrue("minecraft:pig".equals(ItemSoulMould.getPatternName(mould)), "a pig");
        mould.getItem().interactLivingEntity(mould, player, test.spawn(EntityType.COW, new BlockPos(2, 1, 1)), InteractionHand.MAIN_HAND);
        test.assertTrue("minecraft:cow".equals(ItemSoulMould.getPatternName(mould)), "a cow: the newer pattern wins");
        // a player cannot be moulded
        mould.getItem().interactLivingEntity(mould, player, player, InteractionHand.MAIN_HAND);
        test.assertTrue("minecraft:cow".equals(ItemSoulMould.getPatternName(mould)), "players are skipped");
        // a right click without sneaking keeps the pattern, a sneaking one clears it
        player.setItemInHand(InteractionHand.MAIN_HAND, mould);
        mould.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue("minecraft:cow".equals(ItemSoulMould.getPatternName(mould)), "kept by a plain click");
        player.setShiftKeyDown(true);
        mould.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemSoulMould.getPatternName(mould).isEmpty(), "forgotten by a sneaking click");
        finish(test, player);
    }

    /**
     * The way a player really uses the mould: through {@code Player.interactOn}, which hands the item a COPY of the held
     * stack in creative mode (the direct calls of the other checks cannot see that).
     */
    public static void aSoulMouldLearnsThroughAPlayersInteractionAlsoInCreative(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test);
        Pig pig = test.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        for (boolean creative : new boolean[] { false, true }) {
            player.getAbilities().instabuild = creative;
            ItemStack mould = TTRegistry.stack(LibItemNames.SOUL_MOULD);
            player.setItemInHand(InteractionHand.MAIN_HAND, mould);
            InteractionResult result = player.interactOn(pig, InteractionHand.MAIN_HAND);
            test.assertTrue(result.consumesAction(), "the click is used (creative " + creative + ")");
            ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            test.assertTrue(held.getItem() instanceof ItemSoulMould, "still a mould in the hand (creative " + creative + ")");
            test.assertTrue("minecraft:pig".equals(ItemSoulMould.getPatternName(held)),
                    "the held mould learned the pig (creative " + creative + "): " + ItemSoulMould.getPatternName(held));
        }
        finish(test, player);
    }

    // ---- Aspectalyzer ----------------------------------------------------------------------------------------------

    public static void theAspectalyzerHoldsAStackAndKnowsItsAspects(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 3);
        test.setBlock(pos, Machines.ASPECT_ANALYZER.get().defaultBlockState());
        TileAspectAnalyzer analyzer = tile(test, pos, TileAspectAnalyzer.class);
        test.assertTrue(!analyzer.hasItemMethod() && analyzer.getAspectList() == null && !analyzer.itemHasAspectsMethod(), "empty");
        analyzer.setItem(0, new ItemStack(Items.IRON_INGOT, 40));
        test.assertTrue(analyzer.hasItemMethod() && analyzer.getItem(0).getCount() == 40, "a whole stack fits");
        test.assertTrue(analyzer.itemHasAspectsMethod(), "iron has aspects");
        test.assertTrue(analyzer.getAspectsMethod().contains(Aspect.METAL.getTag()), "metallum: " + analyzer.getAspectsMethod());
        test.assertTrue(analyzer.getAspectsAmountsMethod().get(Aspect.METAL.getTag()) > 0, "with an amount");

        CompoundTag saved = analyzer.saveWithFullMetadata(test.getLevel().registryAccess());
        TileAspectAnalyzer loaded = new TileAspectAnalyzer(analyzer.getBlockPos(), analyzer.getBlockState());
        loaded.loadWithComponents(saved, test.getLevel().registryAccess());
        test.assertTrue(loaded.getItem(0).is(Items.IRON_INGOT) && loaded.getItem(0).getCount() == 40, "saved");
        test.succeed();
    }

    public static void theAspectalyzerSlotTakesItemsWithAspectsOnly(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 3);
        test.setBlock(pos, Machines.ASPECT_ANALYZER.get().defaultBlockState());
        TileAspectAnalyzer analyzer = tile(test, pos, TileAspectAnalyzer.class);
        ServerPlayer player = mockPlayer(test);
        ContainerAspectAnalyzer menu = new ContainerAspectAnalyzer(1, player.getInventory(), analyzer);
        test.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(Items.IRON_INGOT)), "iron");
        test.assertTrue(!menu.getSlot(0).mayPlace(ItemStack.EMPTY), "nothing");
        test.assertTrue(menu.stillValid(player), "near");
        // shift-click moves between the slot and the inventory
        player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 5));
        menu.quickMoveStack(player, 1 + 27); // the first hot bar slot
        test.assertTrue(analyzer.getItem(0).is(Items.IRON_INGOT) && analyzer.getItem(0).getCount() == 5, "into the analyzer");
        menu.quickMoveStack(player, 0);
        test.assertTrue(analyzer.getItem(0).isEmpty() && player.getInventory().contains(stack -> stack.is(Items.IRON_INGOT)), "and back");
        finish(test, player);
    }

    // ---- Tablet of Necromancy ---------------------------------------------------------------------------------------

    /** The tablet at (3,1,3), pedestals at (1,1,3), (5,1,3), (3,1,5), carrying three soul aspects. */
    private static TileSummon tabletWith(GameTestHelper test, ItemStack a, ItemStack b, ItemStack c) {
        floor(test);
        test.setBlock(new BlockPos(3, 1, 3), Machines.SUMMON.get().defaultBlockState());
        BlockPos[] spots = { new BlockPos(1, 1, 3), new BlockPos(5, 1, 3), new BlockPos(3, 1, 5) };
        ItemStack[] stacks = { a, b, c };
        for (int i = 0; i < 3; i++) {
            test.setBlock(spots[i], BlocksTC.get("arcane_pedestal").defaultBlockState());
            ((Container) test.getLevel().getBlockEntity(test.absolutePos(spots[i]))).setItem(0, stacks[i]);
        }
        return tile(test, new BlockPos(3, 1, 3), TileSummon.class);
    }

    private static ItemStack soul(Aspect aspect, ItemMobAspect.Form form) {
        return TTRegistry.stack(ItemMobAspect.path(aspect, form));
    }

    private static Container pedestal(GameTestHelper test, BlockPos relative) {
        return (Container) test.getLevel().getBlockEntity(test.absolutePos(relative));
    }

    public static void theTabletSummonsAWolfFromThreeBestiaAndUsesThemUp(GameTestHelper test) {
        ItemStack b = soul(Aspect.BEAST, ItemMobAspect.Form.BASIC);
        TileSummon tablet = tabletWith(test, b.copy(), b.copy(), b.copy());
        test.assertTrue(tablet.summon(test.getLevel(), 300), "a wolf is summoned");
        test.assertEntityPresent(EntityType.WOLF);
        for (BlockPos spot : List.of(new BlockPos(1, 1, 3), new BlockPos(5, 1, 3), new BlockPos(3, 1, 5))) {
            test.assertTrue(pedestal(test, spot).getItem(0).isEmpty(), "the soul aspect on " + spot + " is used up");
        }
        test.assertTrue(!tablet.summon(test.getLevel(), 300), "nothing is left to summon");
        test.succeed();
    }

    public static void theTabletDoesNothingForAnUnknownSet(GameTestHelper test) {
        TileSummon tablet = tabletWith(test, soul(Aspect.AIR, ItemMobAspect.Form.BASIC), soul(Aspect.AIR, ItemMobAspect.Form.BASIC),
                soul(Aspect.AIR, ItemMobAspect.Form.BASIC));
        test.assertTrue(!tablet.summon(test.getLevel(), 300), "no mob has three aer");
        test.assertTrue(!pedestal(test, new BlockPos(1, 1, 3)).getItem(0).isEmpty(), "the items stay");
        test.succeed();
    }

    public static void infusedSoulAspectsSummonOnlyEveryTwentyMinutesAreKeptAndBindToTheLastTablet(GameTestHelper test) {
        ItemStack b = soul(Aspect.BEAST, ItemMobAspect.Form.INFUSED);
        TileSummon tablet = tabletWith(test, b.copy(), b.copy(), b.copy());
        BlockPos tabletPos = test.absolutePos(new BlockPos(3, 1, 3));
        test.assertTrue(!tablet.summon(test.getLevel(), 300), "300 is not a multiple of 1200: nothing happens");
        test.assertEntityNotPresent(EntityType.WOLF);

        // a set that was never used matches every tablet, so the first cycle at a multiple of 1200 summons
        test.assertTrue(tablet.summon(test.getLevel(), 1200), "1200: summoned");
        test.assertTrue(wolves(test) == 1, "one wolf, found " + wolves(test));
        ItemStack kept = pedestal(test, new BlockPos(1, 1, 3)).getItem(0);
        test.assertTrue(ItemMobAspect.isInfused(kept), "the infused soul aspect is kept");
        test.assertTrue(ItemMobAspect.lastUsedTabletMatches(kept, tabletPos), "and remembers the tablet");
        test.assertTrue(!ItemMobAspect.lastUsedTabletMatches(kept, test.absolutePos(new BlockPos(0, 1, 0))), "not another one");

        test.assertTrue(tablet.summon(test.getLevel(), 2400), "2400: the same tablet summons again");
        test.assertTrue(wolves(test) == 2, "two wolves, found " + wolves(test));
        test.assertTrue(ItemMobAspect.isInfused(pedestal(test, new BlockPos(5, 1, 3)).getItem(0)), "and the set is still there");

        // 1.7.10 quirk: a second tablet in reach of the same pedestals summons nothing, but takes the mark
        BlockPos otherPos = new BlockPos(3, 1, 6);
        test.setBlock(otherPos, Machines.SUMMON.get().defaultBlockState());
        TileSummon other = tile(test, otherPos, TileSummon.class);
        test.assertTrue(other.summon(test.getLevel(), 3600), "3600: the set is recognised by the other tablet");
        test.assertTrue(wolves(test) == 2, "but nothing is summoned: " + wolves(test));
        test.assertTrue(ItemMobAspect.lastUsedTabletMatches(pedestal(test, new BlockPos(1, 1, 3)).getItem(0),
                test.absolutePos(otherPos)), "the other tablet took the mark");
        test.succeed();
    }

    private static int wolves(GameTestHelper test) {
        return test.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Wolf.class, test.getBounds()).size();
    }

    public static void aMixedSetIsUsedUpEvenWhenOneIsInfused(GameTestHelper test) {
        TileSummon tablet = tabletWith(test, soul(Aspect.BEAST, ItemMobAspect.Form.INFUSED), soul(Aspect.BEAST, ItemMobAspect.Form.BASIC),
                soul(Aspect.BEAST, ItemMobAspect.Form.BASIC));
        test.assertTrue(tablet.summon(test.getLevel(), 301), "summoned at any time: not all are infused");
        test.assertTrue(pedestal(test, new BlockPos(1, 1, 3)).getItem(0).isEmpty(), "the infused one is consumed too");
        test.succeed();
    }

    public static void aSummonedSlimeIsSizeOne(GameTestHelper test) {
        TileSummon tablet = tabletWith(test, soul(Aspect.SLIME, ItemMobAspect.Form.BASIC), soul(Aspect.SLIME, ItemMobAspect.Form.BASIC),
                soul(Aspect.BEAST, ItemMobAspect.Form.BASIC));
        test.assertTrue(tablet.summon(test.getLevel(), 300), "a slime is summoned");
        List<Slime> slimes = test.getLevel().getEntitiesOfClass(Slime.class, test.getBounds().inflate(10));
        test.assertTrue(slimes.size() == 1 && slimes.get(0).getSize() == 1, "one slime of size 1: " + slimes);
        test.succeed();
    }

    public static void aWitherSkeletonComesWithAStoneSword(GameTestHelper test) {
        TileSummon tablet = tabletWith(test, soul(Aspect.UNDEAD, ItemMobAspect.Form.BASIC), soul(Aspect.POISON, ItemMobAspect.Form.BASIC),
                soul(Aspect.FIRE, ItemMobAspect.Form.BASIC));
        test.assertTrue(tablet.summon(test.getLevel(), 300), "a wither skeleton is summoned");
        test.assertEntityPresent(EntityType.WITHER_SKELETON);
        List<net.minecraft.world.entity.monster.WitherSkeleton> found = test.getLevel()
                .getEntitiesOfClass(net.minecraft.world.entity.monster.WitherSkeleton.class, test.getBounds());
        test.assertTrue(found.size() == 1 && found.get(0).getMainHandItem().is(Items.STONE_SWORD),
                "it carries the stone sword that onSpawnWithEgg gives it: " + found);
        test.succeed();
    }

    // ---- Cursed Spirit's Blade ---------------------------------------------------------------------------------------

    private static ItemStack freshBlade() {
        ItemStack blade = TTRegistry.stack(LibItemNames.BLOOD_SWORD);
        return blade;
    }

    public static void theBladeSwitchesTheHarvestWithAShiftRightClick(GameTestHelper test) {
        ServerPlayer player = mockPlayer(test);
        ItemStack blade = freshBlade();
        player.setItemInHand(InteractionHand.MAIN_HAND, blade);
        test.assertTrue(!ItemBloodSword.isActivated(blade), "off at first");
        player.setShiftKeyDown(true);
        blade.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(ItemBloodSword.isActivated(blade), "on");
        test.assertTrue(blade.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA) != null, "the active icon");
        player.stopUsingItem();
        blade.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        test.assertTrue(!ItemBloodSword.isActivated(blade), "off again");
        test.assertTrue(blade.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA) == null, "the plain icon");
        finish(test, player);
    }

    public static void theBladeHasTheOriginalNumbers(GameTestHelper test) {
        ItemStack blade = freshBlade();
        test.assertTrue(blade.getMaxDamage() == 950, "950 uses");
        test.assertTrue(blade.getMaxStackSize() == 1, "one item");
        double[] damage = { 0 };
        double[] speed = { 0 };
        blade.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) damage[0] += modifier.amount();
            if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)) speed[0] += modifier.amount();
        });
        test.assertTrue(damage[0] == 10.0, "ten points of attack damage: " + damage[0]);
        test.assertTrue(speed[0] == 0.25, "a quarter more speed: " + speed[0]);
        test.succeed();
    }

    public static void aBladeHurtsWhoStrikesWithIt(GameTestHelper test) {
        floor(test);
        Villager wielder = test.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        wielder.setItemSlot(EquipmentSlot.MAINHAND, freshBlade());
        Pig target = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 2));
        float before = wielder.getHealth();
        EventResult result = ItemBloodSword.onDamageTaken(target, test.getLevel().damageSources().mobAttack(wielder), 4F);
        test.assertTrue(result.isEmpty(), "the blow itself is not cancelled");
        test.assertTrue(wielder.getHealth() == before - 2F, "the wielder takes 2 magic damage: " + before + " -> " + wielder.getHealth());
        test.succeed();
    }

    public static void aBlockingWielderTakesThreeMagicDamageInsteadOfTheBlow(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test);
        // the test world is creative, and a new player is untouchable for 60 ticks
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, freshBlade());
        player.startUsingItem(InteractionHand.MAIN_HAND);
        test.assertTrue(player.isUsingItem(), "raised");
        Villager attacker = test.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 1, 2));
        test.runAfterDelay(70, () -> {
            test.assertTrue(player.isUsingItem(), "still raised");
            float before = player.getHealth();
            EventResult result = ItemBloodSword.onDamageTaken(player, test.getLevel().damageSources().mobAttack(attacker), 8F);
            test.assertTrue(result.isFalse(), "the blow is cancelled");
            test.assertTrue(player.getHealth() == before - 3F, "and 3 magic damage taken instead: " + before + " -> " + player.getHealth());

            // lowered, the blade does not protect
            player.stopUsingItem();
            EventResult lowered = ItemBloodSword.onDamageTaken(player, test.getLevel().damageSources().mobAttack(attacker), 8F);
            test.assertTrue(lowered.isEmpty(), "a lowered blade leaves the blow alone");
            finish(test, player);
        });
    }

    /** A mock player holding a freshly activated blade (the shift-right click of the original). */
    private static ServerPlayer reaper(GameTestHelper test) {
        ServerPlayer player = mockPlayer(test);
        ItemStack blade = freshBlade();
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, blade);
        blade.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        player.stopUsingItem();
        player.setShiftKeyDown(false);
        test.assertTrue(ItemBloodSword.isActivated(blade), "activated");
        return player;
    }

    private static List<ItemEntity> itemsInTheArea(GameTestHelper test) {
        return test.getLevel().getEntitiesOfClass(ItemEntity.class, test.getBounds().inflate(6));
    }

    private static boolean hasItem(List<ItemEntity> items, Item item) {
        return items.stream().anyMatch(e -> e.getItem().is(item));
    }

    private static long soulAspects(List<ItemEntity> items) {
        return items.stream().filter(e -> e.getItem().getItem() instanceof ItemMobAspect).count();
    }

    /**
     * {@code LivingDropsEvent} of the original, through {@code TinkererEvents.LIVING_DROPS}: the soul aspects are in the
     * world as soon as the blow that kills returns (not a tick later), at the victim's position, with the pick-up delay of ten
     * ticks, and the loot table's pork is gone; experience is not an item and stays.
     */
    public static void anActivatedBladeReapsTheSoulAspectsOfTheMobItKills(GameTestHelper test) {
        floor(test);
        ServerPlayer player = reaper(test);
        test.assertTrue(TinkererEvents.hasLivingDropsListeners(), "the blade's listener of LIVING_DROPS is registered at start-up");

        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
        ServerLevel level = test.getLevel();
        pig.hurt(level.damageSources().playerAttack(player), 100F);
        test.assertTrue(pig.isDeadOrDying(), "the blow killed the pig");

        // no waiting: the event edits the drops in the call of the death
        List<ItemEntity> items = itemsInTheArea(test);
        test.assertTrue(soulAspects(items) == 3, "three soul aspects at once, found " + soulAspects(items) + " in " + items);
        test.assertTrue(items.size() == 3, "and nothing else: " + items);
        test.assertTrue(!hasItem(items, Items.PORKCHOP), "no pork: the vanilla drops are replaced");
        // the pig is beast, earth, travel
        for (Aspect aspect : List.of(Aspect.BEAST, Aspect.EARTH, Aspect.TRAVEL)) {
            Item soul = TTRegistry.item(ItemMobAspect.path(aspect, ItemMobAspect.Form.BASIC));
            test.assertTrue(items.stream().anyMatch(e -> e.getItem().is(soul) && e.getItem().getCount() == 1),
                    "one " + aspect.getTag() + " soul aspect");
        }
        for (ItemEntity soul : items) {
            test.assertTrue(soul.position().distanceTo(pig.position()) < 1.0E-6, "dropped where the pig died: " + soul.position());
            test.assertTrue(soul.saveWithoutId(new CompoundTag()).getShort("PickupDelay") == 10, "pick-up delay of ten ticks");
        }
        test.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, test.getBounds().inflate(6)).isEmpty(),
                "experience is not an item drop and still comes");

        test.runAfterDelay(3, () -> {
            List<ItemEntity> later = itemsInTheArea(test);
            test.assertTrue(soulAspects(later) == 3 && later.size() == 3 && !hasItem(later, Items.PORKCHOP),
                    "nothing is added or taken a moment later: " + later);
            later.forEach(ItemEntity::discard);
            finish(test, player);
        });
    }

    public static void aBladeThatIsNotActivatedLeavesTheLootAlone(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test);
        player.setItemInHand(InteractionHand.MAIN_HAND, freshBlade());
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
        ServerLevel level = test.getLevel();
        pig.hurt(level.damageSources().playerAttack(player), 100F);
        List<ItemEntity> items = itemsInTheArea(test);
        test.assertTrue(hasItem(items, Items.PORKCHOP), "the pork is dropped: " + items);
        test.assertTrue(soulAspects(items) == 0, "no soul aspects");
        test.runAfterDelay(3, () -> {
            List<ItemEntity> later = itemsInTheArea(test);
            test.assertTrue(soulAspects(later) == 0, "no soul aspects a moment later either");
            later.forEach(ItemEntity::discard);
            finish(test, player);
        });
    }

    /**
     * The listener is global and answers only for a melee kill by the player who holds the activated blade in the main hand,
     * of a mob that has a row in the table: every other death keeps its loot.
     */
    public static void theBladeLeavesTheDeathsThatAreNotItsKillsAlone(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        ServerPlayer armed = reaper(test);
        ServerPlayer plain = mockPlayer(test);
        ServerPlayer offHand = mockPlayer(test);
        ItemStack spare = freshBlade();
        offHand.setItemInHand(InteractionHand.MAIN_HAND, spare);
        offHand.setShiftKeyDown(true);
        spare.getItem().use(level, offHand, InteractionHand.MAIN_HAND);
        offHand.stopUsingItem();
        offHand.setShiftKeyDown(false);
        offHand.setItemInHand(InteractionHand.OFF_HAND, spare);
        offHand.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        test.assertTrue(ItemBloodSword.isActivated(offHand.getOffhandItem()) && !(offHand.getMainHandItem().getItem() instanceof ItemBloodSword),
                "an activated blade in the off hand, a stick in the main hand");

        // 1. the activated blade is in the off hand of the killer: the original asked the main hand
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
        pig.hurt(level.damageSources().playerAttack(offHand), 100F);
        List<ItemEntity> items = itemsInTheArea(test);
        test.assertTrue(pig.isDeadOrDying() && hasItem(items, Items.PORKCHOP) && soulAspects(items) == 0, "off hand: the loot stays: " + items);
        items.forEach(ItemEntity::discard);

        // 2. another player (not the killer) holds the activated blade
        pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
        pig.hurt(level.damageSources().playerAttack(plain), 100F);
        items = itemsInTheArea(test);
        test.assertTrue(pig.isDeadOrDying() && hasItem(items, Items.PORKCHOP) && soulAspects(items) == 0,
                "the blade of a bystander: the loot stays: " + items);
        items.forEach(ItemEntity::discard);

        // 3. the killer holds the activated blade but the death is not a melee blow (the 1.7.10 damage type "player")
        pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
        pig.hurt(level.damageSources().indirectMagic(null, armed), 100F);
        items = itemsInTheArea(test);
        test.assertTrue(pig.isDeadOrDying() && hasItem(items, Items.PORKCHOP) && soulAspects(items) == 0,
                "a kill by magic of the wielder is not a blow: the loot stays: " + items);
        items.forEach(ItemEntity::discard);

        // 4. nobody killed it
        pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
        pig.hurt(level.damageSources().generic(), 100F);
        items = itemsInTheArea(test);
        test.assertTrue(pig.isDeadOrDying() && hasItem(items, Items.PORKCHOP) && soulAspects(items) == 0, "no killer: the loot stays: " + items);
        items.forEach(ItemEntity::discard);

        // 5. a mob without a row in the table keeps everything it drops (a rabbit that carries a sword it always drops)
        Rabbit rabbit = test.spawnWithNoFreeWill(EntityType.RABBIT, new BlockPos(3, 1, 3));
        test.assertTrue(EnumMobAspect.getAspectsForEntity(rabbit) == null, "the rabbit has no row");
        rabbit.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        rabbit.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
        rabbit.hurt(level.damageSources().playerAttack(armed), 100F);
        items = itemsInTheArea(test);
        test.assertTrue(rabbit.isDeadOrDying() && hasItem(items, Items.IRON_SWORD) && soulAspects(items) == 0,
                "a mob outside the table: its drops stay: " + items);
        items.forEach(ItemEntity::discard);

        // 6. control: the same pig, killed by the player with the activated blade in the main hand, gives the souls
        pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
        pig.hurt(level.damageSources().playerAttack(armed), 100F);
        items = itemsInTheArea(test);
        test.assertTrue(soulAspects(items) == 3 && !hasItem(items, Items.PORKCHOP), "control: the souls come for the wielder: " + items);
        items.forEach(ItemEntity::discard);
        armed.discard();
        plain.discard();
        finish(test, offHand);
    }

    /** {@code event.drops.clear()}: the equipment a mob drops goes with the loot table when the blade reaps it. */
    public static void anActivatedBladeClearsTheEquipmentToo(GameTestHelper test) {
        floor(test);
        ServerLevel level = test.getLevel();
        ServerPlayer player = reaper(test);

        // control: killed without the blade, the zombie drops the helmet it wears (a drop chance above 1 always drops)
        Zombie control = PlainMobs.spawnWithNoFreeWill(test, EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        control.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        control.setDropChance(EquipmentSlot.HEAD, 2.0F);
        control.hurt(level.damageSources().generic(), 100F);
        List<ItemEntity> items = itemsInTheArea(test);
        test.assertTrue(control.isDeadOrDying() && hasItem(items, Items.GOLDEN_HELMET) && soulAspects(items) == 0,
                "control: the helmet is dropped: " + items);
        items.forEach(ItemEntity::discard);

        Zombie zombie = PlainMobs.spawnWithNoFreeWill(test, EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        zombie.setDropChance(EquipmentSlot.HEAD, 2.0F);
        zombie.hurt(level.damageSources().playerAttack(player), 100F);
        items = itemsInTheArea(test);
        // the zombie is flesh, flesh, undead
        test.assertTrue(zombie.isDeadOrDying() && items.size() == 3 && soulAspects(items) == 3 && !hasItem(items, Items.GOLDEN_HELMET)
                && !hasItem(items, Items.ROTTEN_FLESH), "the helmet and the flesh are gone, three soul aspects: " + items);
        items.forEach(ItemEntity::discard);
        finish(test, player);
    }

    /**
     * The production path of the kill: a real {@code ServerPlayer.attack(target)} of a Survival player, not a damage source made
     * by hand. {@code Player.attack} builds the {@code player_attack} source itself and calls {@code hurt}, which posts the blade's
     * own {@code LIVING_HURT} handler (the 2 points of magic damage to the wielder) in the middle of the blow; the kill's drops
     * must come out as in the hand-made cases and the nested damage must not disturb them. The target has a hundredth of a
     * heart, because the mock player is not ticked (its attack strength and attribute modifiers are those of a fresh player).
     */
    public static void aRealSwingOfTheActivatedBladeReapsAndHurtsItsWielder(GameTestHelper test) {
        floor(test);
        ServerPlayer player = reaper(test);
        // the test world is creative, and a new player is untouchable for 60 ticks
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        test.runAfterDelay(70, () -> {
            try {
                test.assertTrue(ItemBloodSword.isActivated(player.getMainHandItem()), "the blade is still activated in the main hand");
                Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
                pig.setHealth(0.01F);
                float before = player.getHealth();

                player.attack(pig);

                test.assertTrue(pig.isDeadOrDying(), "the swing killed the pig");
                List<ItemEntity> items = itemsInTheArea(test);
                test.assertTrue(soulAspects(items) == 3 && items.size() == 3, "three soul aspects and nothing else, found " + items);
                test.assertTrue(!hasItem(items, Items.PORKCHOP), "no pork: the vanilla drops are replaced");
                for (Aspect aspect : List.of(Aspect.BEAST, Aspect.EARTH, Aspect.TRAVEL)) {
                    Item soul = TTRegistry.item(ItemMobAspect.path(aspect, ItemMobAspect.Form.BASIC));
                    test.assertTrue(items.stream().anyMatch(e -> e.getItem().is(soul)), "one " + aspect.getTag() + " soul aspect");
                }
                for (ItemEntity soul : items) {
                    test.assertTrue(soul.position().distanceTo(pig.position()) < 1.0E-6, "dropped where the pig died: " + soul.position());
                    test.assertTrue(soul.saveWithoutId(new CompoundTag()).getShort("PickupDelay") == 10, "pick-up delay of ten ticks");
                }
                test.assertTrue(player.getHealth() == before - 2F,
                        "and the nested LIVING_HURT handler hurt the wielder by 2: " + before + " -> " + player.getHealth());
                items.forEach(ItemEntity::discard);
            } finally {
                player.discard();
            }
            test.succeed();
        });
    }

    /** The drops of a death as the listeners left them; the check's own listener, the last one, cancels them, so nothing reaches the level. */
    private static List<ItemEntity> theDropsOfTheDeath(net.minecraft.world.entity.Mob mob, net.minecraft.world.damagesource.DamageSource source) {
        List<ItemEntity> seen = new java.util.ArrayList<>();
        TinkererEvents.LivingDrops probe = (who, by, drops, looting, recentlyHit) -> {
            if (who != mob) return EventResult.pass();
            seen.addAll(drops);
            return EventResult.interruptFalse();
        };
        TinkererEvents.LIVING_DROPS.register(probe);
        try {
            mob.die(source);
        } finally {
            TinkererEvents.LIVING_DROPS.unregister(probe);
        }
        return seen;
    }

    /**
     * The order of two groups' listeners: {@code Machines} registers the blade's before {@code KamiWorld} registers the
     * dimensional shard's ({@code ContentGroups.ALL}, both in {@code register()}), so a reaped Enderman in the End or Zombified
     * Piglin in the Nether still rolls its shard, which lands in the list after the soul aspects. Were the order the other way
     * round, the blade's {@code drops.clear()} would delete the shard and nothing else would notice. The deaths are real
     * ({@code Mob.die}), the dice are made certain, and the drops are read after both listeners from a listener of the check that
     * cancels them, so the End and the Nether stay empty.
     */
    public static void aReapedEndermanAndZombifiedPiglinStillRollTheirDimensionalShard(GameTestHelper test) {
        net.minecraft.server.MinecraftServer server = test.getLevel().getServer();
        ServerLevel end = server.getLevel(net.minecraft.world.level.Level.END);
        ServerLevel nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
        test.assertTrue(end != null && nether != null, "the End and the Nether exist");
        test.assertTrue(TinkererEvents.hasLivingDropsListeners(), "the listeners of LIVING_DROPS are registered at start-up");
        ServerPlayer armed = reaper(test);
        ServerPlayer plain = mockPlayer(test);
        plain.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        Item enderShard = TTRegistry.item(LibItemNames.KAMI_RESOURCE_NAMES[7]);
        Item netherShard = TTRegistry.item(LibItemNames.KAMI_RESOURCE_NAMES[6]);
        java.util.function.DoubleSupplier realDice = thaumic.tinkerer.common.core.handler.kami.DimensionalShardDropHandler.dice;
        try {
            thaumic.tinkerer.common.core.handler.kami.DimensionalShardDropHandler.dice = () -> 0D; // every roll succeeds

            // an Enderman in the End: eldritch, eldritch, man; a golden helmet it always drops stands for the loot
            net.minecraft.world.entity.monster.EnderMan control = EntityType.ENDERMAN.create(end);
            control.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            control.setDropChance(EquipmentSlot.HEAD, 2.0F);
            List<ItemEntity> without = theDropsOfTheDeath(control, end.damageSources().playerAttack(plain));
            test.assertTrue(hasItem(without, Items.GOLDEN_HELMET) && hasItem(without, enderShard),
                    "control, a player without the blade: the helmet and the Ender Shard: " + without);

            net.minecraft.world.entity.monster.EnderMan enderman = EntityType.ENDERMAN.create(end);
            enderman.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            enderman.setDropChance(EquipmentSlot.HEAD, 2.0F);
            List<ItemEntity> reaped = theDropsOfTheDeath(enderman, end.damageSources().playerAttack(armed));
            assertSoulsThenShard(test, reaped, List.of(Aspect.ELDRITCH, Aspect.ELDRITCH, Aspect.MAN), enderShard, "an Enderman in the End");

            // a Zombified Piglin in the Nether: undead, flesh, fire; a golden sword it always drops
            net.minecraft.world.entity.monster.ZombifiedPiglin piglinControl = EntityType.ZOMBIFIED_PIGLIN.create(nether);
            piglinControl.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            piglinControl.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
            List<ItemEntity> piglinWithout = theDropsOfTheDeath(piglinControl, nether.damageSources().playerAttack(plain));
            test.assertTrue(hasItem(piglinWithout, Items.GOLDEN_SWORD) && hasItem(piglinWithout, netherShard),
                    "control, a player without the blade: the sword and the Nether Shard: " + piglinWithout);

            net.minecraft.world.entity.monster.ZombifiedPiglin piglin = EntityType.ZOMBIFIED_PIGLIN.create(nether);
            piglin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            piglin.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
            List<ItemEntity> piglinReaped = theDropsOfTheDeath(piglin, nether.damageSources().playerAttack(armed));
            assertSoulsThenShard(test, piglinReaped, List.of(Aspect.UNDEAD, Aspect.FLESH, Aspect.FIRE), netherShard, "a Zombified Piglin in the Nether");
        } finally {
            thaumic.tinkerer.common.core.handler.kami.DimensionalShardDropHandler.dice = realDice;
            armed.discard();
            plain.discard();
        }
        test.succeed();
    }

    /** Exactly the three soul aspects of the row, in the order of the row, then the one shard, and nothing else. */
    private static void assertSoulsThenShard(GameTestHelper test, List<ItemEntity> drops, List<Aspect> row, Item shard, String what) {
        test.assertTrue(drops.size() == 4, what + ": three soul aspects and one shard, nothing of the loot table or the equipment: " + drops);
        for (int i = 0; i < 3; i++) {
            Item soul = TTRegistry.item(ItemMobAspect.path(row.get(i), ItemMobAspect.Form.BASIC));
            test.assertTrue(drops.get(i).getItem().is(soul), what + ": soul aspect " + i + " is " + row.get(i).getTag() + ": " + drops);
        }
        test.assertTrue(drops.get(3).getItem().is(shard) && drops.get(3).getItem().getCount() == 1,
                what + ": the shard comes after the soul aspects, as the second listener adds it: " + drops);
    }

    /**
     * 1.7.10 {@code isNormalCube()}: true for the Aspectalyzer and the Funnel (an opaque material with the default
     * {@code renderAsNormalBlock}), false for the Restorer (which also provides power) and the thin Attractors and Tablet.
     */
    public static void onlyTheAspectalyzerAndTheFunnelConductRedstoneAsIn1710(GameTestHelper test) {
        floor(test);
        test.setBlock(new BlockPos(5, 1, 1), Blocks.HOPPER); // the funnel stands on a hopper
        List<BlockPos> positions = List.of(new BlockPos(1, 1, 1), new BlockPos(3, 1, 1), new BlockPos(5, 2, 1),
                new BlockPos(1, 1, 4), new BlockPos(3, 1, 4), new BlockPos(5, 1, 4));
        List<Block> blocks = List.of(Machines.ASPECT_ANALYZER.get(), Machines.REPAIRER.get(), Machines.FUNNEL.get(),
                Machines.MAGNET.get(), Machines.MOB_MAGNET.get(), Machines.SUMMON.get());
        List<Boolean> conducts = List.of(true, false, true, false, false, false);
        for (int i = 0; i < blocks.size(); i++) {
            test.setBlock(positions.get(i), blocks.get(i).defaultBlockState());
            BlockPos absolute = test.absolutePos(positions.get(i));
            BlockState state = test.getLevel().getBlockState(absolute);
            test.assertTrue(state.is(blocks.get(i)), blocks.get(i) + " stands");
            test.assertTrue(state.isRedstoneConductor(test.getLevel(), absolute) == conducts.get(i),
                    blocks.get(i) + (conducts.get(i) ? " conducts redstone" : " does not conduct redstone"));
        }
        test.succeed();
    }

    // ---- removal and drops -----------------------------------------------------------------------------------------

    /** The number of items of a kind lying around a block (dropped stacks may merge, so the counts are summed). */
    private static int dropped(GameTestHelper test, BlockPos relative, Item item) {
        AABB box = new AABB(test.absolutePos(relative)).inflate(3);
        int sum = 0;
        for (ItemEntity entity : test.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.getItem().is(item)) sum += entity.getItem().getCount();
        }
        return sum;
    }

    /** Breaks the block as a player's tool would (drops from the loot table, then {@code onRemove}). */
    private static void breakBlock(GameTestHelper test, BlockPos relative) {
        test.getLevel().destroyBlock(test.absolutePos(relative), true);
    }

    public static void breakingTheAspectalyzerScattersItsStackAndDropsTheBlock(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 3);
        test.setBlock(pos, Machines.ASPECT_ANALYZER.get().defaultBlockState());
        TileAspectAnalyzer analyzer = tile(test, pos, TileAspectAnalyzer.class);
        analyzer.setItem(0, new ItemStack(Items.IRON_INGOT, 40));
        breakBlock(test, pos);
        test.assertTrue(test.getLevel().getBlockState(test.absolutePos(pos)).isAir(), "the block is gone");
        test.assertTrue(dropped(test, pos, Items.IRON_INGOT) == 40, "all 40 ingots left the block: " + dropped(test, pos, Items.IRON_INGOT));
        test.assertTrue(analyzer.getItem(0).isEmpty(), "the slot was emptied");
        test.assertTrue(dropped(test, pos, Machines.ASPECT_ANALYZER.get().asItem()) == 1, "and the block dropped itself");
        test.succeed();
    }

    public static void breakingTheRestorerScattersItsToolAndDropsTheBlock(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 3);
        test.setBlock(pos, Machines.REPAIRER.get().defaultBlockState());
        TileRepairer repairer = tile(test, pos, TileRepairer.class);
        ItemStack damaged = new ItemStack(Items.DIAMOND_PICKAXE);
        damaged.setDamageValue(500);
        repairer.setItem(0, damaged);
        breakBlock(test, pos);
        test.assertTrue(test.getLevel().getBlockState(test.absolutePos(pos)).isAir(), "the block is gone");
        boolean found = false;
        for (ItemEntity entity : test.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(test.absolutePos(pos)).inflate(3))) {
            if (entity.getItem().is(Items.DIAMOND_PICKAXE) && entity.getItem().getDamageValue() == 500) found = true;
        }
        test.assertTrue(found, "the pickaxe came out with its damage");
        test.assertTrue(dropped(test, pos, Machines.REPAIRER.get().asItem()) == 1, "and the block dropped itself");
        test.succeed();
    }

    public static void breakingTheFunnelScattersItsJarAndDropsTheBlock(GameTestHelper test) {
        TileFunnel funnel = funnelSetup(test);
        funnel.setItem(0, filledJar(Aspect.FIRE, 3));
        BlockPos pos = new BlockPos(2, 2, 3);
        breakBlock(test, pos);
        test.assertTrue(test.getLevel().getBlockState(test.absolutePos(pos)).isAir(), "the funnel is gone");
        test.assertTrue(dropped(test, pos, BlocksTC.jar().asItem()) == 1, "the jar came out");
        test.assertTrue(dropped(test, pos, Machines.FUNNEL.get().asItem()) == 1, "and the funnel dropped itself");
        test.succeed();
    }

    public static void theFunnelPopsOffWithItsJarWhenTheHopperGoes(GameTestHelper test) {
        TileFunnel funnel = funnelSetup(test);
        funnel.setItem(0, filledJar(Aspect.FIRE, 3));
        BlockPos pos = new BlockPos(2, 2, 3);
        test.setBlock(new BlockPos(2, 1, 3), Blocks.AIR);
        test.assertTrue(test.getLevel().getBlockState(test.absolutePos(pos)).isAir(), "the funnel did not stay in the air");
        test.assertTrue(dropped(test, pos, Machines.FUNNEL.get().asItem()) == 1, "as an item");
        test.assertTrue(dropped(test, pos, BlocksTC.jar().asItem()) == 1, "with its jar");
        test.succeed();
    }

    public static void breakingTheCorporealAttractorScattersItsMouldAndDropsTheBlock(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 4);
        test.setBlock(pos, Machines.MOB_MAGNET.get().defaultBlockState());
        TileMobMagnet magnet = tile(test, pos, TileMobMagnet.class);
        // The slot keeps this very instance and the scattering splits it down to count 0, and a stack of count 0 reports
        // Items.AIR as its item: the item to look for is taken before the block is broken.
        Item mouldItem = TTRegistry.item(LibItemNames.SOUL_MOULD);
        magnet.setItem(0, new ItemStack(mouldItem));
        breakBlock(test, pos);
        test.assertTrue(test.getLevel().getBlockState(test.absolutePos(pos)).isAir(), "the block is gone");
        test.assertTrue(magnet.getItem(0).isEmpty(), "the slot was emptied");
        test.assertTrue(dropped(test, pos, mouldItem) == 1, "the mould came out");
        test.assertTrue(dropped(test, pos, Machines.MOB_MAGNET.get().asItem()) == 1, "and the block dropped itself");
        test.succeed();
    }

    public static void everyMachineBlockDropsItselfFromItsLootTable(GameTestHelper test) {
        floor(test);
        List<Block> blocks = List.of(Machines.ASPECT_ANALYZER.get(), Machines.REPAIRER.get(), Machines.FUNNEL.get(),
                Machines.MAGNET.get(), Machines.MOB_MAGNET.get(), Machines.SUMMON.get());
        int x = 0;
        for (Block block : blocks) {
            BlockPos pos = new BlockPos(x++, 2, 2);
            // the funnel stands on a hopper
            test.setBlock(pos.below(), block == Machines.FUNNEL.get() ? Blocks.HOPPER : Blocks.STONE);
            test.setBlock(pos, block.defaultBlockState());
            BlockPos absolute = test.absolutePos(pos);
            BlockState state = test.getLevel().getBlockState(absolute);
            test.assertTrue(state.is(block), block + " stands at " + pos);
            List<ItemStack> drops = Block.getDrops(state, test.getLevel(), absolute, test.getLevel().getBlockEntity(absolute));
            test.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()) && drops.get(0).getCount() == 1,
                    block + " drops exactly itself, got " + drops);
        }
        test.succeed();
    }

    public static void everyMobOfTheTableHasAnEntityType(GameTestHelper test) {
        for (EnumMobAspect row : EnumMobAspect.values()) {
            test.assertTrue(row.getEntityType() != null, row.name() + " has an entity type in the registry");
            test.assertTrue(row.createEntity(test.getLevel()) != null, row.name() + " can be made");
        }
        test.succeed();
    }
}
