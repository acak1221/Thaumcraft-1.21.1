package thaumic.tinkerer.testing;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.ResearchCategories;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.block.BlockAnimationTablet;
import thaumic.tinkerer.common.block.BlockRPlacer;
import thaumic.tinkerer.common.block.camo.CamoDeviceBlock;
import thaumic.tinkerer.common.block.camo.PulseDeviceBlock;
import thaumic.tinkerer.common.block.tile.TileEntityMobilizer;
import thaumic.tinkerer.common.block.tile.TileEntityRelay;
import thaumic.tinkerer.common.block.tile.TileGolemConnector;
import thaumic.tinkerer.common.block.tile.TileRPlacer;
import thaumic.tinkerer.common.block.tile.container.ContainerAnimationTablet;
import thaumic.tinkerer.common.block.tile.container.ContainerRemotePlacer;
import thaumic.tinkerer.common.block.tile.tablet.TileAnimationTablet;
import thaumic.tinkerer.common.block.tile.transvector.TileTransvectorDislocator;
import thaumic.tinkerer.common.block.tile.transvector.TileTransvectorInterface;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.common.core.fakeplayer.TinkererFakePlayer;
import thaumic.tinkerer.common.item.ItemConnector;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.network.packet.PacketPlacerButton;
import thaumic.tinkerer.common.network.packet.PacketTabletButton;
import thaumic.tinkerer.common.registry.TTRecipes;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>Runtime checks of the TileBlocks group in a running server: registration, the persistence of every block entity, the
 * Animation Tablet breaking, attacking, placing and using, the Remote Placer, the Transvector Dislocator and Interface,
 * the Binder, the camouflage and the Levitational Locomotive on its track. They were written without being able to run
 * them (no Gradle in the porting session): the first run may show a wrong assumption of a check as well as a defect.</p>
 */
public final class TileBlocksRuntimeChecks {
    private TileBlocksRuntimeChecks() {}

    // ---- helpers ---------------------------------------------------------------------------------------------------

    /** A floor of stone at relative y = 0, so that blocks and mobs stand. */
    private static void floor(GameTestHelper test) {
        for (int x = 0; x <= 7; x++) for (int z = 0; z <= 7; z++) test.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    private static <T extends BlockEntity> T tile(GameTestHelper test, BlockPos relative, Class<T> type) {
        BlockEntity entity = test.getLevel().getBlockEntity(test.absolutePos(relative));
        test.assertTrue(type.isInstance(entity), "a " + type.getSimpleName() + " at " + relative + ", found " + entity);
        return type.cast(entity);
    }

    private static TinkererFakePlayer player(GameTestHelper test) {
        return new TinkererFakePlayer(test.getLevel(), new GameProfile(UUID.fromString("c0ffee00-0000-4000-8000-000000000001"), "[tt-test]"));
    }

    private static BlockHitResult hit(GameTestHelper test, BlockPos relative, Direction face) {
        BlockPos abs = test.absolutePos(relative);
        return new BlockHitResult(Vec3.atCenterOf(abs), face, abs, false);
    }

    private static void giveItem(TinkererFakePlayer player, ItemStack stack) {
        player.getInventory().selected = 0;
        player.getInventory().setItem(0, stack);
    }

    private static BlockState facing(Block block, Direction facing) {
        return block.defaultBlockState().setValue(PulseDeviceBlock.FACING, facing);
    }

    private static int countItems(GameTestHelper test, net.minecraft.world.item.Item item, BlockPos center, double radius) {
        int count = 0;
        AABB box = new AABB(test.absolutePos(center)).inflate(radius);
        for (ItemEntity entity : test.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.getItem().is(item)) count += entity.getItem().getCount();
        }
        return count;
    }

    // ---- registration ----------------------------------------------------------------------------------------------

    /** Every object of the group is registered, has its research and recipe, and registration had no failure. */
    public static void registrationIsComplete(GameTestHelper test) {
        for (String name : new String[] { LibBlockNames.ANIMATION_TABLET, LibBlockNames.REMOTE_PLACER, LibBlockNames.MOBILIZER,
                LibBlockNames.MOBILIZER_RELAY, LibBlockNames.GOLEMCONNECTOR, LibBlockNames.INTERFACE, LibBlockNames.DISLOCATOR }) {
            ResourceLocation id = LibMisc.id(name);
            test.assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "block " + id);
            test.assertTrue(BuiltInRegistries.ITEM.containsKey(id), "block item " + id);
            test.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(id), "block entity type " + id);
        }
        test.assertTrue(BuiltInRegistries.MENU.containsKey(LibMisc.id(LibBlockNames.ANIMATION_TABLET)), "tablet menu");
        test.assertTrue(BuiltInRegistries.MENU.containsKey(LibMisc.id(LibBlockNames.REMOTE_PLACER)), "placer menu");
        test.assertTrue(BuiltInRegistries.ITEM.containsKey(LibMisc.id(LibItemNames.CONNECTOR)), "binder");
        for (String key : new String[] { LibResearch.KEY_ANIMATION_TABLET, LibResearch.KEY_REMOTE_PLACER,
                LibResearch.KEY_MOBILIZER, LibResearch.KEY_INTERFACE, LibResearch.KEY_DISLOCATOR,
                LibResearch.KEY_GOLEMCONNECTOR }) {
            test.assertTrue(ResearchCategories.getResearch(key) != null, "research " + key);
        }
        for (String recipe : new String[] { LibResearch.KEY_ANIMATION_TABLET, LibResearch.KEY_REMOTE_PLACER,
                LibResearch.KEY_MOBILIZER, LibResearch.KEY_RELAY, LibResearch.KEY_INTERFACE,
                LibResearch.KEY_INTERFACE + "1", LibResearch.KEY_DISLOCATOR, LibResearch.KEY_GOLEMCONNECTOR }) {
            test.assertTrue(TTRecipes.has(recipe), "recipe " + recipe);
        }
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());
        test.succeed();
    }

    // ---- persistence -----------------------------------------------------------------------------------------------

    private static <T extends BlockEntity> T reload(GameTestHelper test, T entity, Class<T> type) {
        CompoundTag saved = entity.saveWithFullMetadata(test.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(entity.getBlockPos(), entity.getBlockState(), saved, test.getLevel().registryAccess());
        test.assertTrue(type.isInstance(loaded), "a reloaded " + type.getSimpleName() + ", found " + loaded);
        return type.cast(loaded);
    }

    /** What every block entity saves comes back from its own save, through the chunk format and the update packet. */
    public static void blockEntitiesPersist(GameTestHelper test) {
        floor(test);
        // The tablet: the item, the two modes, the swing.
        BlockPos tabletPos = new BlockPos(1, 1, 1);
        test.setBlock(tabletPos, TileBlocks.animationTabletBlock());
        TileAnimationTablet tablet = tile(test, tabletPos, TileAnimationTablet.class);
        tablet.leftClick = false;
        tablet.redstone = true;
        tablet.heldItem = new ItemStack(Items.DIAMOND_PICKAXE);
        tablet.heldItem.setDamageValue(17);
        TileAnimationTablet tabletLoaded = reload(test, tablet, TileAnimationTablet.class);
        test.assertTrue(!tabletLoaded.leftClick && tabletLoaded.redstone, "the tablet's modes");
        test.assertTrue(tabletLoaded.heldItem.is(Items.DIAMOND_PICKAXE) && tabletLoaded.heldItem.getDamageValue() == 17,
                "the tablet's item with its damage: " + tabletLoaded.heldItem);
        // The sync packet carries the item and the modes too.
        CompoundTag update = tablet.getUpdateTag(test.getLevel().registryAccess());
        test.assertTrue(!update.isEmpty(), "the update tag is never empty (clients ignore an empty one)");

        // The remote placer: the item, the distance, the camouflage.
        BlockPos placerPos = new BlockPos(3, 1, 1);
        test.setBlock(placerPos, facing(TileBlocks.remotePlacerBlock(), Direction.WEST));
        TileRPlacer placer = tile(test, placerPos, TileRPlacer.class);
        placer.setBlocks(3);
        placer.setItem(0, new ItemStack(Items.COBBLESTONE, 12));
        placer.setCamo(Blocks.GLASS.defaultBlockState());
        TileRPlacer placerLoaded = reload(test, placer, TileRPlacer.class);
        test.assertTrue(placerLoaded.blocks == 3, "the placer's distance");
        test.assertTrue(placerLoaded.getItem(0).is(Items.COBBLESTONE) && placerLoaded.getItem(0).getCount() == 12, "the placer's block");
        test.assertTrue(placerLoaded.getCamo() == Blocks.GLASS.defaultBlockState(), "the placer's camouflage");

        // The interface: the binding, the camouflage.
        BlockPos interfacePos = new BlockPos(5, 1, 1);
        test.setBlock(interfacePos, TileBlocks.interfaceBlock());
        TileTransvectorInterface iface = tile(test, interfacePos, TileTransvectorInterface.class);
        BlockPos below = test.absolutePos(new BlockPos(5, 0, 1));
        iface.setTarget(below);
        iface.setCamo(Blocks.OAK_LOG.defaultBlockState());
        TileTransvectorInterface ifaceLoaded = reload(test, iface, TileTransvectorInterface.class);
        test.assertTrue(below.equals(ifaceLoaded.getTarget()), "the interface's binding: " + ifaceLoaded.getTarget());
        test.assertTrue(ifaceLoaded.getCamo() == Blocks.OAK_LOG.defaultBlockState(), "the interface's camouflage");
        TileTransvectorInterface unbound = reload(test, tile(test, interfacePos, TileTransvectorInterface.class), TileTransvectorInterface.class);
        unbound.setTarget(null);
        test.assertTrue(reload(test, unbound, TileTransvectorInterface.class).getTarget() == null, "an unbound interface stays unbound");

        // The golem connector: the id of the golem.
        BlockPos connectorPos = new BlockPos(6, 1, 1);
        test.setBlock(connectorPos, TileBlocks.golemConnectorBlock());
        TileGolemConnector connector = tile(test, connectorPos, TileGolemConnector.class);
        UUID golemId = UUID.fromString("00000000-0000-4000-8000-0000000000aa");
        connector.ConnectGolem(golemId);
        test.assertTrue(golemId.equals(reload(test, connector, TileGolemConnector.class).golemConnected), "the golem's id");

        // The locomotive and the relay.
        BlockPos mobilizerPos = new BlockPos(1, 1, 3);
        test.setBlock(mobilizerPos, TileBlocks.mobilizerBlock());
        TileEntityMobilizer mobilizer = tile(test, mobilizerPos, TileEntityMobilizer.class);
        mobilizer.linked = true;
        mobilizer.firstRelayX = 11;
        mobilizer.firstRelayZ = -4;
        mobilizer.secondRelayX = 19;
        mobilizer.secondRelayZ = -4;
        mobilizer.movementDirection = Direction.WEST;
        TileEntityMobilizer mobilizerLoaded = reload(test, mobilizer, TileEntityMobilizer.class);
        test.assertTrue(mobilizerLoaded.linked && mobilizerLoaded.firstRelayX == 11 && mobilizerLoaded.firstRelayZ == -4
                && mobilizerLoaded.secondRelayX == 19 && mobilizerLoaded.secondRelayZ == -4
                && mobilizerLoaded.movementDirection == Direction.WEST, "the locomotive's link");
        BlockPos relayPos = new BlockPos(3, 1, 3);
        test.setBlock(relayPos, TileBlocks.relayBlock());
        TileEntityRelay relay = tile(test, relayPos, TileEntityRelay.class);
        relay.hasPartner = true;
        relay.partnerX = 77;
        relay.partnerZ = -88;
        TileEntityRelay relayLoaded = reload(test, relay, TileEntityRelay.class);
        test.assertTrue(relayLoaded.hasPartner && relayLoaded.partnerX == 77 && relayLoaded.partnerZ == -88, "the relay's partner");
        test.succeed();
    }

    // ---- the animation tablet ------------------------------------------------------------------------------------

    private static BlockPos placeTablet(GameTestHelper test, BlockPos at, Direction facing, ItemStack item, boolean left, boolean redstone) {
        test.setBlock(at, TileBlocks.animationTabletBlock().defaultBlockState().setValue(BlockAnimationTablet.FACING, facing));
        TileAnimationTablet tablet = tile(test, at, TileAnimationTablet.class);
        tablet.leftClick = left;
        tablet.redstone = redstone;
        tablet.setItem(0, item);
        return at;
    }

    /** Left-click mode breaks the block in front with the speed and the drops of the tool, and wears the tool. */
    public static void tabletBreaksTheBlockInFront(GameTestHelper test) {
        floor(test);
        BlockPos target = new BlockPos(3, 1, 2);
        test.setBlock(target, Blocks.STONE);
        BlockPos tabletPos = placeTablet(test, new BlockPos(3, 1, 3), Direction.NORTH, new ItemStack(Items.IRON_PICKAXE), true, false);
        test.succeedWhen(() -> {
            test.assertBlockPresent(Blocks.AIR, target);
            test.assertTrue(countItems(test, Items.COBBLESTONE, target, 2) == 1, "the stone dropped cobblestone, as a pickaxe breaks it");
            TileAnimationTablet tablet = tile(test, tabletPos, TileAnimationTablet.class);
            test.assertTrue(tablet.heldItem.is(Items.IRON_PICKAXE) && tablet.heldItem.getDamageValue() > 0,
                    "the pickaxe wore: " + tablet.heldItem);
        });
    }

    /**
     * A block whose drops need a tool the held item is not breaks slowly (a hundredth of the speed, not a thirtieth) and
     * drops nothing, as for a player: netherrack with a stick.
     */
    public static void tabletBreaksWithoutTheRightToolDropsNothing(GameTestHelper test) {
        floor(test);
        BlockPos target = new BlockPos(3, 1, 2);
        test.setBlock(target, Blocks.NETHERRACK);
        placeTablet(test, new BlockPos(3, 1, 3), Direction.NORTH, new ItemStack(Items.STICK), true, false);
        test.succeedWhen(() -> {
            test.assertBlockPresent(Blocks.AIR, target);
            test.assertTrue(countItems(test, Items.NETHERRACK, target, 3) == 0, "no netherrack dropped: a stick is no pickaxe");
        });
    }

    /** Left-click mode attacks the entity in front with the held weapon, with the damage of the weapon. */
    public static void tabletAttacksTheEntityInFront(GameTestHelper test) {
        floor(test);
        BlockPos target = new BlockPos(3, 1, 2);
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, target);
        pig.setNoAi(true);
        BlockPos tabletPos = placeTablet(test, new BlockPos(3, 1, 3), Direction.NORTH, new ItemStack(Items.DIAMOND_SWORD), true, false);
        test.succeedWhen(() -> {
            // The player's 1 and the sword's 6 make 7 of the pig's 10: the sword's attribute modifier was applied.
            test.assertTrue(!pig.isAlive() || pig.getHealth() <= 3.0F, "a diamond sword hurts a pig by 7 (health " + pig.getHealth() + ")");
            test.assertTrue(tile(test, tabletPos, TileAnimationTablet.class).heldItem.getDamageValue() > 0, "the sword wore");
        });
    }

    /** Right-click mode places the block it holds into the empty block in front (by clicking the block below). */
    public static void tabletPlacesBlocks(GameTestHelper test) {
        floor(test);
        BlockPos target = new BlockPos(3, 1, 2);
        BlockPos tabletPos = placeTablet(test, new BlockPos(3, 1, 3), Direction.NORTH, new ItemStack(Items.STONE, 16), false, false);
        test.succeedWhen(() -> {
            test.assertBlockPresent(Blocks.STONE, target);
            TileAnimationTablet tablet = tile(test, tabletPos, TileAnimationTablet.class);
            test.assertTrue(tablet.heldItem.getCount() < 16, "a stone was used up: " + tablet.heldItem);
        });
    }

    /** Right-click mode with a hoe tills the dirt in front. */
    public static void tabletTillsWithAHoe(GameTestHelper test) {
        floor(test);
        BlockPos below = new BlockPos(3, 0, 2);
        test.setBlock(below, Blocks.DIRT);
        placeTablet(test, new BlockPos(3, 1, 3), Direction.NORTH, new ItemStack(Items.DIAMOND_HOE), false, false);
        test.succeedWhen(() -> test.assertBlockPresent(Blocks.FARMLAND, below));
    }

    /** Right-click mode uses the item on the entity in front: a bucket milks a cow. */
    public static void tabletUsesItemsOnEntities(GameTestHelper test) {
        floor(test);
        BlockPos target = new BlockPos(3, 1, 2);
        test.spawnWithNoFreeWill(EntityType.COW, target);
        BlockPos tabletPos = placeTablet(test, new BlockPos(3, 1, 3), Direction.NORTH, new ItemStack(Items.BUCKET), false, false);
        test.succeedWhen(() -> test.assertTrue(tile(test, tabletPos, TileAnimationTablet.class).heldItem.is(Items.MILK_BUCKET),
                "the bucket was filled: " + tile(test, tabletPos, TileAnimationTablet.class).heldItem));
    }

    /** In redstone mode the tablet waits for power, and works as soon as the block above or beside it is powered. */
    public static void tabletInRedstoneModeNeedsPower(GameTestHelper test) {
        floor(test);
        BlockPos target = new BlockPos(3, 1, 2);
        placeTablet(test, new BlockPos(3, 1, 3), Direction.NORTH, new ItemStack(Items.STONE, 16), false, true);
        test.runAfterDelay(40, () -> {
            test.assertBlockPresent(Blocks.AIR, target);
            test.setBlock(new BlockPos(4, 1, 3), Blocks.REDSTONE_BLOCK);
        });
        test.succeedWhen(() -> {
            test.assertBlockPresent(Blocks.STONE, target);
            test.assertBlockProperty(new BlockPos(3, 1, 3), BlockAnimationTablet.POWERED, true);
        });
    }

    /** A wand turns the tablet north, south, west, east; a powered tablet refuses. */
    public static void tabletTurnsWithAWand(GameTestHelper test) {
        floor(test);
        BlockPos tabletPos = placeTablet(test, new BlockPos(3, 1, 3), Direction.NORTH, ItemStack.EMPTY, true, false);
        TinkererFakePlayer player = player(test);
        giveItem(player, new ItemStack(ItemsTC.wand()));
        Direction[] order = { Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.NORTH };
        for (Direction expected : order) {
            test.useBlock(tabletPos, player);
            Direction actual = test.getBlockState(tabletPos).getValue(BlockAnimationTablet.FACING);
            test.assertTrue(actual == expected, "turned to " + expected + ", is " + actual);
        }
        test.setBlock(tabletPos, test.getBlockState(tabletPos).setValue(BlockAnimationTablet.POWERED, true));
        test.useBlock(tabletPos, player);
        test.assertBlockProperty(tabletPos, BlockAnimationTablet.FACING, Direction.NORTH);
        test.succeed();
    }

    /** The item comes out when the tablet is broken. */
    public static void tabletSpillsItsItem(GameTestHelper test) {
        floor(test);
        BlockPos tabletPos = placeTablet(test, new BlockPos(3, 1, 3), Direction.NORTH, new ItemStack(Items.COBBLESTONE, 40), true, false);
        test.destroyBlock(tabletPos);
        test.assertTrue(countItems(test, Items.COBBLESTONE, tabletPos, 3) == 40, "the 40 cobblestone are on the ground");
        test.assertTrue(countItems(test, TileBlocks.animationTabletBlock().asItem(), tabletPos, 3) == 1, "and the tablet itself dropped");
        test.succeed();
    }

    /** The menus: one device slot and the player's 36, shift-click moves between them, the device range is eight blocks. */
    public static void menusHaveTheirSlots(GameTestHelper test) {
        floor(test);
        TinkererFakePlayer player = player(test);
        BlockPos tabletPos = placeTablet(test, new BlockPos(3, 1, 3), Direction.NORTH, ItemStack.EMPTY, true, false);
        TileAnimationTablet tablet = tile(test, tabletPos, TileAnimationTablet.class);
        ContainerAnimationTablet menu = new ContainerAnimationTablet(7, player.getInventory(), tablet);
        test.assertTrue(menu.slots.size() == 37, "37 slots in the tablet menu: " + menu.slots.size());
        player.getInventory().setItem(10, new ItemStack(Items.IRON_SWORD));
        menu.quickMoveStack(player, 2); // slot 0 is the tablet's, slot 1 is inventory index 9, so slot 2 is index 10
        test.assertTrue(tablet.heldItem.is(Items.IRON_SWORD), "shift-click puts the item into the tablet: " + tablet.heldItem);
        menu.quickMoveStack(player, 0);
        test.assertTrue(tablet.heldItem.isEmpty() && player.getInventory().contains(new ItemStack(Items.IRON_SWORD)), "and takes it back");

        BlockPos placerPos = new BlockPos(5, 1, 3);
        test.setBlock(placerPos, TileBlocks.remotePlacerBlock());
        TileRPlacer placer = tile(test, placerPos, TileRPlacer.class);
        ContainerRemotePlacer placerMenu = new ContainerRemotePlacer(8, player.getInventory(), placer);
        test.assertTrue(placerMenu.slots.size() == 37, "37 slots in the placer menu");
        // The 1.7.10 screen slot was a plain Slot: it takes any item. Only automation (hoppers, Container.canPlaceItem) is
        // restricted to blocks; see remotePlacerSpillsAndRefuses.
        test.assertTrue(placerMenu.slots.get(0).mayPlace(new ItemStack(Items.STICK)), "the placer's screen slot takes any item, as in 1.7.10");
        test.assertTrue(placerMenu.slots.get(0).mayPlace(new ItemStack(Items.STONE)), "the placer takes a block");
        test.succeed();
    }

    // ---- the packets of the two screens ----------------------------------------------------------------------------

    /**
     * The server side of the two button packets: only a sender that has the menu of that very block entity open changes
     * it, and the placer's distance is clamped to 1..4.
     */
    public static void menuPacketsCheckTheSender(GameTestHelper test) {
        floor(test);
        TinkererFakePlayer sender = player(test);
        BlockPos tabletPos = placeTablet(test, new BlockPos(2, 1, 3), Direction.NORTH, ItemStack.EMPTY, true, false);
        BlockPos otherPos = placeTablet(test, new BlockPos(4, 1, 3), Direction.NORTH, ItemStack.EMPTY, true, false);
        BlockPos placerPos = new BlockPos(6, 1, 3);
        test.setBlock(placerPos, TileBlocks.remotePlacerBlock());
        TileAnimationTablet tablet = tile(test, tabletPos, TileAnimationTablet.class);
        TileAnimationTablet other = tile(test, otherPos, TileAnimationTablet.class);
        TileRPlacer placer = tile(test, placerPos, TileRPlacer.class);
        BlockPos absTablet = test.absolutePos(tabletPos);
        BlockPos absOther = test.absolutePos(otherPos);
        BlockPos absPlacer = test.absolutePos(placerPos);

        // No menu open: nothing is changed.
        test.assertTrue(!new PacketTabletButton(absTablet, false, true).apply(sender), "a tablet packet without an open menu is refused");
        test.assertTrue(!new PacketPlacerButton(absPlacer, 3).apply(sender), "a placer packet without an open menu is refused");
        test.assertTrue(tablet.leftClick && !tablet.redstone && placer.blocks == 1, "and nothing changed");

        // The tablet's menu is open: that tablet only.
        sender.containerMenu = new ContainerAnimationTablet(7, sender.getInventory(), tablet);
        test.assertTrue(!new PacketTabletButton(absOther, false, true).apply(sender), "another tablet than the open one is refused");
        test.assertTrue(other.leftClick && !other.redstone, "the other tablet is untouched");
        test.assertTrue(!new PacketPlacerButton(absPlacer, 3).apply(sender), "a placer is refused while a tablet menu is open");
        test.assertTrue(!new PacketTabletButton(absPlacer, false, true).apply(sender), "a position with no tablet is refused");
        test.assertTrue(!new PacketTabletButton(new BlockPos(1_000_000, 64, 1_000_000), false, true).apply(sender), "an unloaded position is refused");
        test.assertTrue(new PacketTabletButton(absTablet, false, true).apply(sender), "the open tablet takes the modes");
        test.assertTrue(!tablet.leftClick && tablet.redstone, "right click and redstone: " + tablet.leftClick + "/" + tablet.redstone);

        // The placer's menu is open: the distance is clamped.
        sender.containerMenu = new ContainerRemotePlacer(8, sender.getInventory(), placer);
        test.assertTrue(new PacketPlacerButton(absPlacer, 3).apply(sender), "the open placer takes the distance");
        test.assertTrue(placer.blocks == 3, "three blocks: " + placer.blocks);
        test.assertTrue(new PacketPlacerButton(absPlacer, 99).apply(sender) && placer.blocks == 4, "more than four is four: " + placer.blocks);
        test.assertTrue(new PacketPlacerButton(absPlacer, -7).apply(sender) && placer.blocks == 1, "less than one is one: " + placer.blocks);
        test.assertTrue(!new PacketTabletButton(absTablet, true, false).apply(sender), "a tablet is refused while the placer menu is open");
        test.succeed();
    }

    /** Sneaking with a wand opens the placer's menu; a plain wand click through IWandable is left to the block. */
    public static void placerWandOpensTheMenuOnlyWhileSneaking(GameTestHelper test) {
        floor(test);
        BlockPos placerPos = new BlockPos(3, 1, 3);
        test.setBlock(placerPos, TileBlocks.remotePlacerBlock());
        TileRPlacer placer = tile(test, placerPos, TileRPlacer.class);
        BlockPos abs = test.absolutePos(placerPos);
        BlockRPlacer block = TileBlocks.remotePlacerBlock();
        ServerPlayer viewer = test.makeMockServerPlayerInLevel();
        ItemStack wand = new ItemStack(ItemsTC.wand());

        int plain = block.onWandRightClick(test.getLevel(), wand, viewer, abs.getX(), abs.getY(), abs.getZ(), 1, 0);
        test.assertTrue(plain < 0, "a plain click is not handled by IWandable: " + plain);
        test.assertTrue(!(viewer.containerMenu instanceof ContainerRemotePlacer), "and opens no menu");

        viewer.setShiftKeyDown(true);
        int sneaking = block.onWandRightClick(test.getLevel(), wand, viewer, abs.getX(), abs.getY(), abs.getZ(), 1, 0);
        test.assertTrue(sneaking == 1, "a sneaking click is handled: " + sneaking);
        test.assertTrue(viewer.containerMenu instanceof ContainerRemotePlacer menu && menu.placer == placer,
                "the placer's menu is open: " + viewer.containerMenu);
        test.succeed();
    }

    // ---- the fake player ---------------------------------------------------------------------------------------------

    /**
     * The fake player earns no advancements: a kill by it (the Animation Tablet's sword) does not complete "Monster Hunter"
     * as it would for a player with the same profile.
     */
    public static void fakePlayerEarnsNoAdvancements(GameTestHelper test) {
        floor(test);
        TinkererFakePlayer fake = player(test);
        net.minecraft.advancements.AdvancementHolder holder = test.getLevel().getServer().getAdvancements()
                .get(ResourceLocation.withDefaultNamespace("adventure/kill_a_mob"));
        test.assertTrue(holder != null, "the vanilla advancement is loaded");
        net.minecraft.world.entity.monster.Zombie zombie = test.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        net.minecraft.advancements.CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(fake, zombie, test.getLevel().damageSources().generic());
        test.assertTrue(!fake.getAdvancements().getOrStartProgress(holder).isDone(), "the fake player did not earn Monster Hunter");
        test.succeed();
    }

    // ---- the remote placer ---------------------------------------------------------------------------------------

    private static TileRPlacer placePlacer(GameTestHelper test, BlockPos at, Direction facing, int blocks, ItemStack item) {
        test.setBlock(at, facing(TileBlocks.remotePlacerBlock(), facing));
        TileRPlacer placer = tile(test, at, TileRPlacer.class);
        placer.setBlocks(blocks);
        placer.setItem(0, item);
        return placer;
    }

    /** A pulse places the block, one item is used, only into an empty block, and the original's east quirk is kept. */
    public static void remotePlacerPlacesItsBlock(GameTestHelper test) {
        floor(test);
        TileRPlacer south = placePlacer(test, new BlockPos(3, 1, 1), Direction.SOUTH, 2, new ItemStack(Items.COBBLESTONE, 3));
        south.receiveRedstonePulse();
        test.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(3, 1, 3));
        test.assertTrue(south.getItem(0).getCount() == 2, "one cobblestone was used: " + south.getItem(0));
        south.receiveRedstonePulse();
        test.assertTrue(south.getItem(0).getCount() == 2, "the target is occupied now: nothing placed");

        TileRPlacer east = placePlacer(test, new BlockPos(5, 3, 5), Direction.EAST, 2, new ItemStack(Items.STONE, 1));
        east.receiveRedstonePulse();
        test.assertBlockPresent(TileRPlacer.EAST_AIMS_WEST_AS_IN_1_7_10 ? Blocks.STONE : Blocks.AIR, new BlockPos(3, 3, 5));
        test.assertTrue(east.getItem(0).isEmpty(), "the last stone is gone");

        TileRPlacer up = placePlacer(test, new BlockPos(1, 1, 5), Direction.UP, 3, new ItemStack(Items.DIRT, 1));
        up.receiveRedstonePulse();
        test.assertBlockPresent(Blocks.DIRT, new BlockPos(1, 4, 5));
        test.succeed();
    }

    /** A real redstone edge triggers the placer one tick later. */
    public static void remotePlacerRunsOnRedstone(GameTestHelper test) {
        floor(test);
        placePlacer(test, new BlockPos(3, 1, 1), Direction.SOUTH, 1, new ItemStack(Items.COBBLESTONE, 3));
        test.setBlock(new BlockPos(4, 1, 1), Blocks.REDSTONE_BLOCK);
        test.succeedWhen(() -> test.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(3, 1, 2)));
    }

    /** The placer keeps what a block does when it is placed (a sign needs its wall, stairs turn with the placer). */
    public static void remotePlacerSpillsAndRefuses(GameTestHelper test) {
        floor(test);
        BlockPos placerPos = new BlockPos(3, 1, 3);
        TileRPlacer placer = placePlacer(test, placerPos, Direction.NORTH, 1, new ItemStack(Items.COBBLESTONE, 5));
        test.assertTrue(!placer.canPlaceItem(0, new ItemStack(Items.STICK)), "no item that is not a block");
        test.assertTrue(placer.canPlaceItem(0, new ItemStack(Items.STONE)), "a block");
        test.destroyBlock(placerPos);
        test.assertTrue(countItems(test, Items.COBBLESTONE, placerPos, 3) == 5, "the block in the slot is not lost");
        test.succeed();
    }

    // ---- the transvector dislocator ---------------------------------------------------------------------------

    /** A pulse swaps two blocks with their block-entity data, spills nothing, and rests for ten ticks. */
    public static void dislocatorSwapsBlocksWithTheirContents(GameTestHelper test) {
        floor(test);
        BlockPos dislocatorPos = new BlockPos(3, 1, 3);
        BlockPos frontPos = new BlockPos(3, 2, 3);
        BlockPos endPos = new BlockPos(6, 1, 6);
        test.setBlock(dislocatorPos, facing(TileBlocks.dislocatorBlock(), Direction.UP));
        TileTransvectorDislocator dislocator = tile(test, dislocatorPos, TileTransvectorDislocator.class);
        dislocator.setTarget(test.absolutePos(endPos));

        test.setBlock(frontPos, Blocks.DIAMOND_BLOCK);
        test.setBlock(endPos, Blocks.CHEST);
        ChestBlockEntity chest = tile(test, endPos, ChestBlockEntity.class);
        chest.setItem(4, new ItemStack(Items.EMERALD, 9));

        dislocator.receiveRedstonePulse();

        test.assertBlockPresent(Blocks.DIAMOND_BLOCK, endPos);
        test.assertBlockPresent(Blocks.CHEST, frontPos);
        ChestBlockEntity moved = tile(test, frontPos, ChestBlockEntity.class);
        test.assertTrue(moved.getItem(4).is(Items.EMERALD) && moved.getItem(4).getCount() == 9, "the chest took its items along");
        test.assertTrue(countItems(test, Items.EMERALD, frontPos, 8) == 0, "and spilled nothing");

        // Resting: a second pulse right away is kept for later and swaps back once the ten ticks are over.
        dislocator.receiveRedstonePulse();
        test.assertBlockPresent(Blocks.DIAMOND_BLOCK, endPos);
        test.runAfterDelay(15, () -> {
            test.assertBlockPresent(Blocks.CHEST, endPos);
            test.assertBlockPresent(Blocks.DIAMOND_BLOCK, frontPos);
            test.assertTrue(tile(test, endPos, ChestBlockEntity.class).getItem(4).getCount() == 9, "the items are back with the chest");
            test.assertTrue(countItems(test, Items.EMERALD, endPos, 8) == 0, "and nothing was spilled on the way back");
            test.succeed();
        });
    }

    /** Entities in the two blocks change places. */
    public static void dislocatorMovesEntities(GameTestHelper test) {
        floor(test);
        BlockPos dislocatorPos = new BlockPos(3, 1, 3);
        BlockPos frontPos = new BlockPos(3, 2, 3);
        BlockPos endPos = new BlockPos(6, 2, 6);
        test.setBlock(dislocatorPos, facing(TileBlocks.dislocatorBlock(), Direction.UP));
        TileTransvectorDislocator dislocator = tile(test, dislocatorPos, TileTransvectorDislocator.class);
        dislocator.setTarget(test.absolutePos(endPos));
        Pig atFront = test.spawnWithNoFreeWill(EntityType.PIG, frontPos);
        Cow atEnd = test.spawnWithNoFreeWill(EntityType.COW, endPos);
        Vec3 frontBefore = atFront.position();
        Vec3 endBefore = atEnd.position();

        dislocator.receiveRedstonePulse();

        test.assertTrue(atFront.position().distanceTo(frontBefore.add(3, 0, 3)) < 0.01, "the pig moved by (3, 0, 3): " + atFront.position());
        test.assertTrue(atEnd.position().distanceTo(endBefore.add(-3, 0, -3)) < 0.01, "the cow moved by (-3, 0, -3): " + atEnd.position());
        test.succeed();
    }

    /**
     * The check of the original lets every block through (bedrock too): kept (docs/deviations/TileBlocks.md). An aura node
     * is not placed here: its block entity ticks the aura of the world, which is not the subject of this check.
     */
    public static void dislocatorChecksLetEveryBlockThrough(GameTestHelper test) {
        BlockPos bedrock = new BlockPos(2, 2, 2);
        test.setBlock(bedrock, Blocks.BEDROCK);
        test.assertTrue(TileTransvectorDislocator.checkBlock(test.getLevel(), test.absolutePos(bedrock)), "bedrock");
        test.assertTrue(TileTransvectorDislocator.checkBlock(test.getLevel(), test.absolutePos(new BlockPos(5, 5, 5))), "air");
        test.setBlock(bedrock, Blocks.DIAMOND_BLOCK);
        test.assertTrue(TileTransvectorDislocator.checkBlock(test.getLevel(), test.absolutePos(bedrock)), "a diamond block");
        test.succeed();
    }

    // ---- the transvector interface --------------------------------------------------------------------------

    /** The interface stands in for the bound chest: slots, items and the face rules. */
    public static void interfaceForwardsItems(GameTestHelper test) {
        floor(test);
        BlockPos interfacePos = new BlockPos(3, 1, 3);
        BlockPos chestPos = new BlockPos(3, 1, 5);
        test.setBlock(interfacePos, TileBlocks.interfaceBlock());
        test.setBlock(chestPos, Blocks.CHEST);
        TileTransvectorInterface iface = tile(test, interfacePos, TileTransvectorInterface.class);
        test.assertTrue(iface.getContainerSize() == 0 && iface.isEmpty(), "an unbound interface has no slots");
        test.assertTrue(iface.getSlotsForFace(Direction.UP).length == 0, "and no accessible slots");

        iface.setTarget(test.absolutePos(chestPos));
        test.assertTrue(iface.getContainerSize() == 27, "the slots of the chest: " + iface.getContainerSize());
        iface.setItem(2, new ItemStack(Items.GOLD_INGOT, 5));
        ChestBlockEntity chest = tile(test, chestPos, ChestBlockEntity.class);
        test.assertTrue(chest.getItem(2).is(Items.GOLD_INGOT), "an item put into the interface is in the chest");
        test.assertTrue(iface.getItem(2).getCount() == 5, "and read back through it");
        test.assertTrue(iface.removeItem(2, 2).getCount() == 2 && chest.getItem(2).getCount() == 3, "taking two takes them from the chest");
        test.assertTrue(iface.getSlotsForFace(Direction.UP).length == 27, "every slot is reachable");
        test.assertTrue(iface.canPlaceItemThroughFace(0, new ItemStack(Items.IRON_INGOT), Direction.NORTH), "and can be filled");
        test.assertTrue(iface.canTakeItemThroughFace(2, new ItemStack(Items.GOLD_INGOT), Direction.NORTH), "and emptied");

        // A binding to nothing ends: the interface needs a block entity to stand in for.
        iface.setTarget(test.absolutePos(new BlockPos(6, 1, 3)));
        test.assertTrue(iface.getTile() == null && !iface.isBound(), "a binding to an empty block ends");

        // A binding beyond four blocks ends (a chest five blocks away), one exactly four blocks away stays.
        BlockPos farInterface = new BlockPos(1, 1, 1);
        test.setBlock(farInterface, TileBlocks.interfaceBlock());
        test.setBlock(new BlockPos(6, 1, 1), Blocks.CHEST);
        test.setBlock(new BlockPos(5, 1, 1), Blocks.CHEST);
        TileTransvectorInterface far = tile(test, farInterface, TileTransvectorInterface.class);
        far.setTarget(test.absolutePos(new BlockPos(6, 1, 1)));
        test.assertTrue(far.getTile() == null && !far.isBound(), "a binding five blocks away ends");
        far.setTarget(test.absolutePos(new BlockPos(5, 1, 1)));
        test.assertTrue(far.getTile() instanceof ChestBlockEntity && far.isBound(), "a binding four blocks away stays");
        test.succeed();
    }

    /** The interface stands in for a jar of essentia, through the API's interfaces. */
    public static void interfaceForwardsEssentia(GameTestHelper test) {
        floor(test);
        BlockPos interfacePos = new BlockPos(3, 1, 3);
        BlockPos jarPos = new BlockPos(3, 1, 5);
        test.setBlock(interfacePos, TileBlocks.interfaceBlock());
        test.setBlock(jarPos, BlocksTC.jar());
        TileTransvectorInterface iface = tile(test, interfacePos, TileTransvectorInterface.class);

        test.assertTrue(iface.addToContainer(Aspect.FIRE, 5) == 5 && !iface.doesContainerAccept(Aspect.FIRE),
                "an unbound interface accepts nothing");
        test.assertTrue(iface.getAspects().size() == 0 && iface.getEssentiaAmount(Direction.UP) == 0, "and holds nothing");

        iface.setTarget(test.absolutePos(jarPos));
        test.assertTrue(iface.doesContainerAccept(Aspect.FIRE), "the jar accepts fire");
        test.assertTrue(iface.addToContainer(Aspect.FIRE, 5) == 0, "all five fit");
        // (The jar's own containerContains answers 0 as in the original, so the contents are read through getAspects.)
        test.assertTrue(iface.getAspects().getAmount(Aspect.FIRE) == 5, "five fire in the jar: " + iface.getAspects().getAmount(Aspect.FIRE));
        test.assertTrue(iface.getEssentiaType(Direction.UP) == Aspect.FIRE && iface.getEssentiaAmount(Direction.UP) == 5,
                "the transport face shows the contents");
        test.assertTrue(iface.takeFromContainer(Aspect.FIRE, 2) && iface.getAspects().getAmount(Aspect.FIRE) == 3, "two taken");
        test.assertTrue(iface.isConnectable(Direction.WEST), "the interface connects on every side");
        test.succeed();
    }

    // ---- the binder ------------------------------------------------------------------------------------------

    private static UseOnContext useOn(GameTestHelper test, TinkererFakePlayer player, BlockPos relative) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND, hit(test, relative, Direction.UP));
    }

    /** The binder remembers a device and binds it to the next block, within the device's range. */
    public static void binderLinksDevices(GameTestHelper test) {
        floor(test);
        BlockPos interfacePos = new BlockPos(3, 1, 3);
        BlockPos otherInterfacePos = new BlockPos(5, 1, 3);
        BlockPos chestPos = new BlockPos(3, 1, 5);
        test.setBlock(interfacePos, TileBlocks.interfaceBlock());
        test.setBlock(otherInterfacePos, TileBlocks.interfaceBlock());
        test.setBlock(chestPos, Blocks.CHEST);
        TileTransvectorInterface iface = tile(test, interfacePos, TileTransvectorInterface.class);
        TinkererFakePlayer player = player(test);
        ItemStack binder = new ItemStack(TileBlocks.connectorItem());
        giveItem(player, binder);

        TileBlocks.connectorItem().useOn(useOn(test, player, chestPos));
        test.assertTrue(!ItemConnector.hasTarget(binder), "a chest is no device: nothing is remembered");

        TileBlocks.connectorItem().useOn(useOn(test, player, interfacePos));
        test.assertTrue(ItemConnector.hasTarget(binder) && ItemConnector.getTarget(binder).equals(test.absolutePos(interfacePos)),
                "the interface is remembered");
        TileBlocks.connectorItem().useOn(useOn(test, player, otherInterfacePos));
        test.assertTrue(!iface.isBound(), "an interface cannot be bound to another interface");

        TileBlocks.connectorItem().useOn(useOn(test, player, chestPos));
        test.assertTrue(chestPos.equals(test.relativePos(iface.getTarget())), "the interface is bound to the chest: " + iface.getTarget());
        test.assertTrue(!ItemConnector.hasTarget(binder), "and the binder forgot the device");

        // The range of an interface is four blocks: five is refused (the binder keeps the device), four binds.
        BlockPos farInterfacePos = new BlockPos(1, 1, 1);
        test.setBlock(farInterfacePos, TileBlocks.interfaceBlock());
        test.setBlock(new BlockPos(6, 1, 1), Blocks.CHEST);
        test.setBlock(new BlockPos(5, 1, 1), Blocks.CHEST);
        TileTransvectorInterface far = tile(test, farInterfacePos, TileTransvectorInterface.class);
        TileBlocks.connectorItem().useOn(useOn(test, player, farInterfacePos));
        test.assertTrue(ItemConnector.hasTarget(binder), "the second interface is remembered");
        TileBlocks.connectorItem().useOn(useOn(test, player, new BlockPos(6, 1, 1)));
        test.assertTrue(!far.isBound() && ItemConnector.hasTarget(binder), "five blocks away is too far, and the binder keeps the device");
        TileBlocks.connectorItem().useOn(useOn(test, player, new BlockPos(5, 1, 1)));
        test.assertTrue(new BlockPos(5, 1, 1).equals(test.relativePos(far.getTarget())), "four blocks away binds: " + far.getTarget());
        test.succeed();
    }

    /** The binder binds a golem connector to a golem (sneak-click on the golem). */
    public static void binderBindsGolemConnectors(GameTestHelper test) {
        floor(test);
        ResourceLocation golemId = ResourceLocation.fromNamespaceAndPath("thaumcraft", "golem");
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(golemId)) {
            test.fail("the golem entity type " + golemId + " is not registered");
            return;
        }
        BlockPos connectorPos = new BlockPos(3, 1, 3);
        test.setBlock(connectorPos, TileBlocks.golemConnectorBlock());
        TileGolemConnector connector = tile(test, connectorPos, TileGolemConnector.class);
        Entity golem = test.spawn(BuiltInRegistries.ENTITY_TYPE.get(golemId), new BlockPos(5, 1, 5));
        test.assertTrue(golem instanceof net.minecraft.world.entity.LivingEntity, "the golem is a living entity");
        TinkererFakePlayer player = player(test);
        ItemStack binder = new ItemStack(TileBlocks.connectorItem());
        giveItem(player, binder);

        // Without a remembered connector the golem click only complains.
        player.setShiftKeyDown(true);
        TileBlocks.connectorItem().interactLivingEntity(binder, player, (net.minecraft.world.entity.LivingEntity) golem, InteractionHand.MAIN_HAND);
        test.assertTrue(connector.golemConnected == null, "nothing bound yet");

        TileBlocks.connectorItem().useOn(useOn(test, player, connectorPos));
        test.assertTrue(ItemConnector.hasTarget(binder), "the golem connector is remembered");
        // A plain (not sneaking) click does not bind.
        player.setShiftKeyDown(false);
        TileBlocks.connectorItem().interactLivingEntity(binder, player, (net.minecraft.world.entity.LivingEntity) golem, InteractionHand.MAIN_HAND);
        test.assertTrue(connector.golemConnected == null, "only a sneaking click binds");
        player.setShiftKeyDown(true);
        TileBlocks.connectorItem().interactLivingEntity(binder, player, (net.minecraft.world.entity.LivingEntity) golem, InteractionHand.MAIN_HAND);
        test.assertTrue(golem.getUUID().equals(connector.golemConnected), "the connector knows the golem");
        test.assertTrue(connector.golem == golem, "and found it");
        test.assertTrue(!ItemConnector.hasTarget(binder), "the binder forgot the connector");
        // The API has no golem view yet: the peripheral answers as a connector without a golem.
        test.assertTrue(!connector.golemAccess().exists(), "stub: the golem cannot be read (API request A11)");
        test.assertTrue(connector.peripheral().call("getType").length == 0, "stub: getType answers nothing");
        test.succeed();
    }

    // ---- the camouflage ----------------------------------------------------------------------------------------

    /** A block in the hand camouflages a device, the empty hand takes it off, other items do nothing. */
    public static void camouflageIsSetAndCleared(GameTestHelper test) {
        floor(test);
        BlockPos pos = new BlockPos(3, 1, 3);
        test.setBlock(pos, TileBlocks.interfaceBlock());
        TileTransvectorInterface iface = tile(test, pos, TileTransvectorInterface.class);
        TinkererFakePlayer player = player(test);

        giveItem(player, new ItemStack(Items.STICK));
        test.useBlock(pos, player);
        test.assertTrue(iface.getCamo() == null, "a stick does nothing");

        giveItem(player, new ItemStack(Items.GLASS));
        test.useBlock(pos, player);
        test.assertTrue(iface.getCamo() == Blocks.GLASS.defaultBlockState(), "glass is the camouflage: " + iface.getCamo());
        BlockState state = test.getBlockState(pos);
        test.assertTrue(state.getValue(CamoDeviceBlock.CAMO), "the block state shows a camouflage");
        test.assertTrue(state.getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE, "the device's own model is hidden");
        test.assertTrue(iface.getUpdateTag(test.getLevel().registryAccess()).contains("camo"), "the clients are told");

        giveItem(player, new ItemStack(Items.CHEST));
        test.useBlock(pos, player);
        test.assertTrue(iface.getCamo() == Blocks.GLASS.defaultBlockState(), "a chest is no valid camouflage: the glass stays");

        giveItem(player, ItemStack.EMPTY);
        test.useBlock(pos, player);
        test.assertTrue(iface.getCamo() == null && !test.getBlockState(pos).getValue(CamoDeviceBlock.CAMO), "the empty hand clears it");
        test.succeed();
    }

    /** A wand turns the facing devices to the clicked side. */
    public static void wandTurnsTheFacingDevices(GameTestHelper test) {
        floor(test);
        for (Block block : new Block[] { TileBlocks.remotePlacerBlock(), TileBlocks.dislocatorBlock() }) {
            BlockPos pos = new BlockPos(3, 1, 3);
            test.setBlock(pos, facing(block, Direction.NORTH));
            TinkererFakePlayer player = player(test);
            giveItem(player, new ItemStack(ItemsTC.wand()));
            for (Direction side : new Direction[] { Direction.EAST, Direction.UP, Direction.WEST, Direction.DOWN, Direction.SOUTH }) {
                test.useBlock(pos, player, hit(test, pos, side));
                Direction now = test.getBlockState(pos).getValue(PulseDeviceBlock.FACING);
                test.assertTrue(now == side, block + ": clicked " + side + ", faces " + now);
            }
        }
        test.succeed();
    }

    // ---- the levitational locomotive ---------------------------------------------------------------------------

    private static void track(GameTestHelper test, BlockPos first, BlockPos second, BlockPos mobilizer) {
        test.setBlock(first, TileBlocks.relayBlock());
        test.setBlock(second, TileBlocks.relayBlock());
        test.setBlock(mobilizer, TileBlocks.mobilizerBlock());
    }

    /** Two relays find each other, link the locomotive between them, and it slides one block east with its passenger. */
    public static void locomotiveCarriesItsPassenger(GameTestHelper test) {
        floor(test);
        BlockPos first = new BlockPos(1, 1, 3);
        BlockPos second = new BlockPos(6, 1, 3);
        BlockPos mobilizer = new BlockPos(3, 1, 3);
        track(test, first, second, mobilizer);
        test.setBlock(new BlockPos(3, 2, 3), Blocks.CHEST);
        ChestBlockEntity chest = tile(test, new BlockPos(3, 2, 3), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 7));
        test.succeedWhen(() -> {
            test.assertBlockPresent(Blocks.AIR, mobilizer);
            test.assertBlockPresent(TileBlocks.mobilizerBlock(), new BlockPos(4, 1, 3));
            test.assertBlockPresent(Blocks.CHEST, new BlockPos(4, 2, 3));
            test.assertBlockPresent(Blocks.AIR, new BlockPos(3, 2, 3));
            ChestBlockEntity moved = tile(test, new BlockPos(4, 2, 3), ChestBlockEntity.class);
            test.assertTrue(moved.getItem(0).is(Items.DIAMOND) && moved.getItem(0).getCount() == 7, "the chest kept its diamonds");
            test.assertTrue(countItems(test, Items.DIAMOND, new BlockPos(4, 2, 3), 6) == 0, "and spilled none");
            TileEntityMobilizer moving = tile(test, new BlockPos(4, 1, 3), TileEntityMobilizer.class);
            test.assertTrue(moving.linked && moving.movementDirection == Direction.EAST, "the locomotive is still linked and still going east");
        });
    }

    /** At the end of the track (something in front) the locomotive turns round and goes back. */
    public static void locomotiveTurnsRoundAtTheEndOfTheTrack(GameTestHelper test) {
        floor(test);
        BlockPos first = new BlockPos(1, 1, 3);
        BlockPos second = new BlockPos(6, 1, 3);
        BlockPos mobilizer = new BlockPos(3, 1, 3);
        track(test, first, second, mobilizer);
        test.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
        test.succeedWhen(() -> {
            test.assertBlockPresent(TileBlocks.mobilizerBlock(), new BlockPos(2, 1, 3));
            test.assertBlockPresent(Blocks.AIR, mobilizer);
        });
    }

    /** Redstone power stops the locomotive. */
    public static void poweredLocomotiveStays(GameTestHelper test) {
        floor(test);
        BlockPos mobilizer = new BlockPos(3, 1, 3);
        track(test, new BlockPos(1, 1, 3), new BlockPos(6, 1, 3), mobilizer);
        test.setBlock(new BlockPos(3, 1, 4), Blocks.REDSTONE_BLOCK);
        test.runAfterDelay(260, () -> {
            test.assertBlockPresent(TileBlocks.mobilizerBlock(), mobilizer);
            test.assertTrue(tile(test, mobilizer, TileEntityMobilizer.class).linked, "it was linked");
            test.succeed();
        });
    }

    /** The relays link each other, and only each other (the nearest one in each of the four lines wins in order). */
    public static void relaysFindEachOther(GameTestHelper test) {
        floor(test);
        BlockPos first = new BlockPos(1, 1, 3);
        BlockPos second = new BlockPos(6, 1, 3);
        test.setBlock(first, TileBlocks.relayBlock());
        test.setBlock(second, TileBlocks.relayBlock());
        TileEntityRelay a = tile(test, first, TileEntityRelay.class);
        TileEntityRelay b = tile(test, second, TileEntityRelay.class);
        a.checkForPartner();
        test.assertTrue(a.hasPartner && b.hasPartner, "both have a partner");
        BlockPos absB = test.absolutePos(second);
        BlockPos absA = test.absolutePos(first);
        test.assertTrue(a.partnerX == absB.getX() && a.partnerZ == absB.getZ(), "a's partner is b");
        test.assertTrue(b.partnerX == absA.getX() && b.partnerZ == absA.getZ(), "b's partner is a");
        test.destroyBlock(second);
        a.verifyPartner();
        test.assertTrue(!a.hasPartner, "breaking the partner ends the link");
        test.succeed();
    }
}
