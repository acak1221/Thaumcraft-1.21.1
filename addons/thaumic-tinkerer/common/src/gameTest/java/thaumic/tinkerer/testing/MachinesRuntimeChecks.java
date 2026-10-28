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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
import thaumic.tinkerer.common.item.ItemBloodSword;
import thaumic.tinkerer.common.item.ItemMobAspect;
import thaumic.tinkerer.common.item.ItemSoulMould;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.TTRecipes;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
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

    public static void anActivatedBladeReapsTheSoulAspectsOfTheMobItKills(GameTestHelper test) {
        floor(test);
        ServerPlayer player = mockPlayer(test);
        ItemStack blade = freshBlade();
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, blade);
        blade.getItem().use(test.getLevel(), player, InteractionHand.MAIN_HAND);
        player.stopUsingItem();
        test.assertTrue(ItemBloodSword.isActivated(blade), "activated");

        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 1, 3));
        ServerLevel level = test.getLevel();
        pig.hurt(level.damageSources().playerAttack(player), 100F);
        test.runAfterDelay(3, () -> {
            // the pig is beast, earth, travel
            int souls = 0;
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, test.getBounds().inflate(6))) {
                if (entity.getItem().getItem() instanceof ItemMobAspect) souls++;
                test.assertTrue(!entity.getItem().is(Items.PORKCHOP), "no pork: the vanilla drops are replaced");
            }
            test.assertTrue(souls == 3, "three soul aspects, found " + souls);
            test.assertItemEntityPresent(TTRegistry.item(ItemMobAspect.path(Aspect.BEAST, ItemMobAspect.Form.BASIC)));
            test.assertItemEntityPresent(TTRegistry.item(ItemMobAspect.path(Aspect.EARTH, ItemMobAspect.Form.BASIC)));
            test.assertItemEntityPresent(TTRegistry.item(ItemMobAspect.path(Aspect.TRAVEL, ItemMobAspect.Form.BASIC)));
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
        test.runAfterDelay(3, () -> {
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, test.getBounds().inflate(6))) {
                test.assertTrue(!(entity.getItem().getItem() instanceof ItemMobAspect), "no soul aspects");
            }
            finish(test, player);
        });
    }

    public static void everyMobOfTheTableHasAnEntityType(GameTestHelper test) {
        for (EnumMobAspect row : EnumMobAspect.values()) {
            test.assertTrue(row.getEntityType() != null, row.name() + " has an entity type in the registry");
            test.assertTrue(row.createEntity(test.getLevel()) != null, row.name() + " can be made");
        }
        test.succeed();
    }
}
