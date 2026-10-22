package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.tiles.TileMagicBox;

/** Oracle BlockMagicBox:72-75: inventory evacuation belongs to block removal. */
public final class BlockDropRuntimeChecks {
    private BlockDropRuntimeChecks() {}

    public static void jarRemoval(GameTestHelper test, boolean voidJar) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(1, 2, 1));
        var block = (voidJar ? thaumcraft.common.config.ThaumcraftContent.VOID_JAR
                : thaumcraft.common.config.ThaumcraftContent.JAR).get();
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var jar = (thaumcraft.common.tiles.TileJar) level.getBlockEntity(pos);
        jar.addToContainer(thaumcraft.api.aspects.Aspect.AIR, 37);
        jar.setFilter(thaumcraft.api.aspects.Aspect.AIR);
        level.destroyBlock(pos, true);
        test.runAfterDelay(2, () -> {
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
            test.assertTrue(drops.size() == 1, "one filled jar, no duplicate jar or loose label");
            ItemStack stack = drops.getFirst().getItem();
            var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            var filter = stack.get(thaumcraft.common.config.ThaumcraftLabels.JAR_LABEL_DATA.get());
            test.assertTrue(stack.is(block.asItem()) && stack.getCount() == 1 && data != null
                            && data.copyTag().getInt("Amount") == 37 && data.copyTag().getString("Aspect").equals("aer")
                            && filter != null && filter.tag().equals("aer") && stack.getMaxStackSize() == 1,
                    "jar variant, exact aspect amount and label component survive the ground drop");
            test.assertTrue(jar.isRemoved() && level.getBlockEntity(pos) == null, "no live jar owner remains");
            drops.forEach(ItemEntity::discard);
            test.succeed();
        });
    }

    public static void inventoryRemoval(GameTestHelper test, String id) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(1, 2, 1));
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", id));
        test.assertTrue(block != Blocks.AIR, "registered fixture block " + id);
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        test.assertTrue(!net.minecraft.world.level.block.piston.PistonBaseBlock.isPushable(
                level.getBlockState(pos), level, pos, net.minecraft.core.Direction.EAST, false, net.minecraft.core.Direction.EAST),
                "native piston rejects inventory block " + id);
        var tile = level.getBlockEntity(pos);
        test.assertTrue(tile instanceof net.minecraft.world.Container, "fixture inventory exists " + id);
        var inventory = (net.minecraft.world.Container) tile;
        ItemStack stock = new ItemStack(Items.DIAMOND);
        stock.set(DataComponents.CUSTOM_NAME, Component.literal("g2-" + id));
        inventory.setItem(0, stock.copy());
        test.assertTrue(ItemStack.matches(inventory.getItem(0), stock), "fixture stock accepted " + id);
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        test.assertTrue(ItemStack.matches(inventory.getItem(0), stock), "same-state update retains stock " + id);
        level.destroyBlock(pos, true);
        test.runAfterDelay(3, () -> {
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2),
                    item -> item.getItem().is(Items.DIAMOND));
            test.assertTrue(drops.size() == 1 && ItemStack.matches(drops.getFirst().getItem(), stock),
                    "exactly one component-bearing stock survives " + id);
            test.assertTrue(tile.isRemoved() && inventory.isEmpty(), "old inventory retired empty " + id);
            drops.forEach(ItemEntity::discard);
            test.succeed();
        });
    }

    public static void magicBoxNonPlayerRemoval(GameTestHelper test, boolean replace) {
        magicBoxRemoval(test, replace ? "replace" : "destroy");
    }

    public static void magicBoxRemoval(GameTestHelper test, String mode) {
        var level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, ThaumcraftEldritchBlocks.MAGIC_BOX.get().defaultBlockState());
        test.assertTrue(!net.minecraft.world.level.block.piston.PistonBaseBlock.isPushable(
                level.getBlockState(pos), level, pos, net.minecraft.core.Direction.EAST, false, net.minecraft.core.Direction.EAST),
                "native piston rejects the Magic Box, like original BlockContainer");
        var box = (TileMagicBox) level.getBlockEntity(pos);
        ItemStack stock = new ItemStack(Items.DIAMOND, 17);
        stock.set(DataComponents.CUSTOM_NAME, Component.literal("g2-blockdrop-stock"));
        box.setItem(0, stock.copy());
        if (mode.equals("replace")) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        else if (mode.equals("creative") || mode.equals("silk")) {
            ServerPlayer player = test.makeMockServerPlayerInLevel();
            player.setGameMode(mode.equals("creative") ? GameType.CREATIVE : GameType.SURVIVAL);
            ItemStack tool = new ItemStack(Items.DIAMOND_AXE);
            tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tool);
            test.assertTrue(player.gameMode.destroyBlock(pos), "native player break succeeds");
        } else if (mode.equals("explosion")) {
            level.explode(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 1,
                    net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        } else level.destroyBlock(pos, true);
        test.runAfterDelay(2, () -> {
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2),
                    item -> item.getItem().is(Items.DIAMOND));
            test.assertTrue(drops.size() == 1 && ItemStack.matches(drops.getFirst().getItem(), stock),
                    "non-player removal must drop exactly the original 17 diamonds with components");
            test.assertTrue(box.isRemoved() && box.isEmpty(), "removed box must not retain a second stock copy");
            test.assertTrue(level.getBlockEntity(pos) == null, "no old block entity remains");
            drops.forEach(ItemEntity::discard);
            test.succeed();
        });
    }
}
