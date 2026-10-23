package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockLevitator;
import thaumcraft.common.config.ThaumcraftLevitator;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.lib.LevitatorRules;
import thaumcraft.common.tiles.TileLevitator;

/**
 * The Arcane Levitator on real content: its block and arcane recipe, its range (open air, an opaque block overhead, a stack, a
 * powered one in the stack and which levitators the block tells), the speed it gives items, creatures, horses and a real connected
 * player (a sneaker slows its fall) and leaves armor stands alone, and real ticks lifting real items and a pig, stopped by power on
 * the block or the one above it. NOT the sparkles, the glow or the client-side lift of the local player.
 */
public final class LevitatorRuntimeChecks {
    private static final double SPEED = (double) 0.1F;

    private LevitatorRuntimeChecks() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static TileLevitator place(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ThaumcraftLevitator.LEVITATOR.get().defaultBlockState(), Block.UPDATE_ALL);
        return (TileLevitator) level.getBlockEntity(pos);
    }

    private static void tick(ServerLevel level, TileLevitator tile) { TileLevitator.tick(level, tile.getBlockPos(), tile.getBlockState(), tile); }

    private static int range(ServerLevel level, TileLevitator tile) {
        tile.requestUpdate();
        tick(level, tile);
        return tile.getRange();
    }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        numbers(test, level);
        BlockPos origin = test.absolutePos(new BlockPos(3, 100, 3));
        Set<ChunkPos> forced = new LinkedHashSet<>();
        List<Entity> spawned = new ArrayList<>();
        try {
            for (int x = (origin.getX() - 3) >> 4; x <= ((origin.getX() + 52) >> 4); x++) for (int z = (origin.getZ() - 3) >> 4; z <= ((origin.getZ() + 3) >> 4); z++) {
                level.getChunk(x, z);
                ChunkPos chunk = new ChunkPos(x, z);
                if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(x, z, true); forced.add(chunk); }
            }
            // Real lifts: stand-alone, powered, powered from above, a pig, an armor stand, and a stack of three.
            BlockPos lift = origin.offset(12, 0, 0), powered = origin.offset(18, 0, 0), above = origin.offset(24, 0, 0), pigPos = origin.offset(30, 0, 0),
                    standPos = origin.offset(36, 0, 0), stack = origin.offset(48, 0, 0);
            place(level, lift);
            place(level, powered);
            level.setBlock(powered.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            place(level, above);
            level.setBlock(above.above().east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            place(level, pigPos);
            place(level, standPos);
            for (int layer = 0; layer < 3; layer++) place(level, stack.above(layer));
            test.runAfterDelay(5, () -> {
                try {
                    var items = new ItemEntity[4];
                    BlockPos[] spots = {lift, powered, above, stack.above(2)};
                    for (int index = 0; index < spots.length; index++) {
                        items[index] = new ItemEntity(level, spots[index].getX() + .5, spots[index].getY() + 1.2, spots[index].getZ() + .5, new ItemStack(Items.STICK));
                        items[index].setNoPickUpDelay();
                        items[index].setDeltaMovement(Vec3.ZERO);
                        items[index].setUnlimitedLifetime();
                        level.addFreshEntity(items[index]);
                        spawned.add(items[index]);
                    }
                    // A pushable entity that really moves by its own motion (a mob without AI does not): a minecart off the rails.
                    Minecart cart = EntityType.MINECART.create(level);
                    cart.setPos(Vec3.atBottomCenterOf(pigPos.above()));
                    level.addFreshEntity(cart);
                    spawned.add(cart);
                    ArmorStand stand = EntityType.ARMOR_STAND.create(level);
                    stand.setPos(Vec3.atBottomCenterOf(standPos.above()));
                    level.addFreshEntity(stand);
                    spawned.add(stand);
                    double standY = stand.getY();
                    physics(test, level, origin, forced, spawned);
                    test.runAfterDelay(45, () -> {
                        try {
                            test.assertTrue(items[0].getY() > lift.getY() + 6 && items[0].getY() < lift.getY() + 14, "a lone levitator carries an item up toward its ten blocks: " + (items[0].getY() - lift.getY()) );
                            test.assertTrue(items[1].getY() < powered.getY() + 2, "a powered levitator lifts nothing: " + (items[1].getY() - powered.getY()));
                            test.assertTrue(items[2].getY() < above.getY() + 2, "power on the block above stops it too: " + (items[2].getY() - above.getY()));
                            test.assertTrue(cart.getY() > pigPos.getY() + 6 && cart.isAlive(), "a minecart (pushable) is carried up too: " + (cart.getY() - pigPos.getY()));
                            thaumcraft.common.Thaumcraft.LOG.info("G26 measured levitator after45 world ticks: item height={}, powered item height={}, minecart height={}",items[0].getY()-lift.getY(),items[1].getY()-powered.getY(),cart.getY()-pigPos.getY());
                            test.assertTrue(Math.abs(stand.getY() - standY) < 1e-6, "an armor stand cannot be pushed, so it stays");
                            test.runAfterDelay(40, () -> {
                                try {
                                    test.assertTrue(items[3].getY() > stack.getY() + 2 + 14, "a stack of three lifts far past ten blocks: " + (items[3].getY() - stack.getY()));
                                    test.succeed();
                                } finally {
                                    cleanup(level, forced, spawned);
                                }
                            });
                        } catch (RuntimeException | Error failure) {
                            cleanup(level, forced, spawned);
                            throw failure;
                        }
                    });
                } catch (RuntimeException | Error failure) {
                    cleanup(level, forced, spawned);
                    throw failure;
                }
            });
        } catch (RuntimeException | Error failure) {
            cleanup(level, forced, spawned);
            throw failure;
        }
    }

    private static void cleanup(ServerLevel level, Set<ChunkPos> forced, List<Entity> spawned) {
        spawned.forEach(Entity::discard);
        for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        forced.clear();
    }

    // ---- numbers ---------------------------------------------------------------------------------------------------------

    private static void numbers(GameTestHelper test, ServerLevel level) {
        var block = ThaumcraftLevitator.LEVITATOR.get();
        var state = block.defaultBlockState();
        BlockPos pos = test.absolutePos(new BlockPos(1, 1, 1));
        test.assertTrue(BuiltInRegistries.BLOCK.getKey(block).equals(id("levitator")) && block instanceof BlockLevitator, "the block is registered");
        test.assertTrue(state.getDestroySpeed(level, pos) == 2.5F && Math.abs(block.getExplosionResistance() - 9.0F) < 1e-4F && state.getSoundType() == SoundType.WOOD
                        && !state.requiresCorrectToolForDrops() && state.is(BlockTags.MINEABLE_WITH_AXE) && state.canOcclude() && state.getRenderShape() == RenderShape.MODEL,
                "wood, hardness 2.5, resistance 15 (3 x 15 / 5 = 9), an opaque model block an axe chops: " + state.getDestroySpeed(level, pos) + " " + block.getExplosionResistance());
        test.assertTrue(BuiltInRegistries.ITEM.get(id("levitator")) instanceof BlockItem blockItem && blockItem.getBlock() == block, "the item places the block");
        test.assertTrue(ThaumcraftLevitator.LEVITATOR_BLOCK_ENTITY.get().isValid(state) && BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id("levitator")) == ThaumcraftLevitator.LEVITATOR_BLOCK_ENTITY.get(), "the tile type is registered");
        var recipe = (ArcaneShapedRecipe) level.getRecipeManager().byKey(id("arcane/levitator")).orElseThrow(() -> new AssertionError("no arcane recipe")).value();
        test.assertTrue(recipe.research().equals("LEVITATOR") && recipe.pattern().equals(List.of("WEW", "BNB", "WAW")) && recipe.vis().equals(Map.of("aer", 10, "terra", 5))
                        && recipe.result().is(block.asItem()) && recipe.result().getCount() == 1, "LEVITATOR, the original shape, 10 air and 5 earth: " + recipe.vis());
        ItemStack planks = new ItemStack(item("thaumcraft:greatwood_planks")), earth = new ItemStack(item("thaumcraft:earth_shard")), air = new ItemStack(item("thaumcraft:air_shard")),
                nitor = new ItemStack(item("thaumcraft:nitor")), iron = new ItemStack(Items.IRON_INGOT);
        test.assertTrue(recipe.matches(new ArcaneRecipeInput(List.of(planks, earth, planks, iron, nitor, iron, planks, air, planks)), level), "planks, earth over air, iron and nitor complete it");
        test.assertTrue(!recipe.matches(new ArcaneRecipeInput(List.of(planks, air, planks, iron, nitor, iron, planks, earth, planks)), level), "earth and air the other way round do not");
        test.assertTrue(!recipe.matches(new ArcaneRecipeInput(List.of(planks, earth, planks, iron, new ItemStack(Items.GLOWSTONE_DUST), iron, planks, air, planks)), level), "glowstone is not nitor");
    }

    // ---- range, speed and which levitators the block tells --------------------------------------------------------------

    private static void physics(GameTestHelper test, ServerLevel level, BlockPos origin, Set<ChunkPos> forced, List<Entity> spawned) {
        // Open air: ten blocks; an opaque block overhead ends it; a powered block or the one above is not seen by the range.
        BlockPos lone = origin;
        TileLevitator tile = place(level, lone);
        test.assertTrue(tile.isUpdateRequested() && range(level, tile) == 10 && !tile.isUpdateRequested(), "a new levitator counts when it first ticks: ten blocks");
        level.setBlock(lone.above(5), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(range(level, tile) == 4, "stone five blocks up leaves four: " + tile.getRange());
        level.setBlock(lone.above(5), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(range(level, tile) == 10, "glass does not stop it (it is not opaque): " + tile.getRange());
        level.setBlock(lone.above(5), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(lone.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(range(level, tile) == 0, "stone right above leaves none");
        level.setBlock(lone.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(range(level, tile) == 10, "and ten again when the stone is gone");
        // Without a request the count is kept for a hundred ticks.
        level.setBlock(lone.above(3), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        tick(level, tile);
        test.assertTrue(tile.getRange() == 10 || tile.getRange() == 2, "an unrequested tick keeps (or, on the hundredth, renews) the count: " + tile.getRange());
        level.setBlock(lone.above(3), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(range(level, tile) == 10, "back to ten");

        // Speed: an item, a creature, a horse; a sneaker; a fixed armor stand; the fall is forgotten.
        Vec3 inColumn = Vec3.atBottomCenterOf(lone.above(2));
        var stick = new ItemEntity(level, inColumn.x, inColumn.y, inColumn.z, new ItemStack(Items.STICK));
        stick.setDeltaMovement(0.2, -0.5, 0.3);
        stick.fallDistance = 9F;
        level.addFreshEntity(stick);
        spawned.add(stick);
        tick(level, tile);
        test.assertTrue(Math.abs(stick.getDeltaMovement().y - (-0.5 + SPEED)) < 1e-9 && stick.getDeltaMovement().x == 0.2 && stick.getDeltaMovement().z == 0.3 && stick.fallDistance == 0F,
                "an item gains a tenth upward, keeps its sideways motion and forgets its fall: " + stick.getDeltaMovement());
        stick.setDeltaMovement(0, 0.3, 0);
        tick(level, tile);
        test.assertTrue(Math.abs(stick.getDeltaMovement().y - (0.3 + SPEED)) < 1e-9, "slower than 0.35 up still gains");
        stick.setDeltaMovement(0, 0.5, 0);
        tick(level, tile);
        test.assertTrue(stick.getDeltaMovement().y == 0.5, "faster than 0.35 up is left alone");
        Pig pig = EntityType.PIG.create(level);
        pig.setNoAi(true);
        pig.setPersistenceRequired();
        pig.setPos(inColumn.add(0, 1, 0));
        level.addFreshEntity(pig);
        spawned.add(pig);
        pig.setDeltaMovement(0, -0.5, 0);
        pig.fallDistance = 7F;
        tick(level, tile);
        test.assertTrue(Math.abs(pig.getDeltaMovement().y - (-0.5 + SPEED)) < 1e-9 && pig.fallDistance == 0F, "a creature is lifted like an item and keeps no fall");
        pig.setShiftKeyDown(true);
        pig.setDeltaMovement(0, -0.5, 0);
        tick(level, tile);
        test.assertTrue(Math.abs(pig.getDeltaMovement().y - (-0.5 * (double) 0.9F)) < 1e-9, "a sneaking creature keeps ninety percent of its fall: " + pig.getDeltaMovement().y);
        pig.setDeltaMovement(0, 0.0, 0);
        tick(level, tile);
        test.assertTrue(pig.getDeltaMovement().y == 0.0, "a sneaker at rest is not lifted");
        pig.discard();
        Horse horse = EntityType.HORSE.create(level);
        horse.setNoAi(true);
        horse.setPersistenceRequired();
        horse.setPos(inColumn.add(0, 1, 0));
        level.addFreshEntity(horse);
        spawned.add(horse);
        horse.setDeltaMovement(0, -0.5, 0);
        tick(level, tile);
        test.assertTrue(Math.abs(horse.getDeltaMovement().y - (-0.5 + SPEED)) < 1e-9, "a horse is lifted");
        horse.discard();
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        stand.setPos(inColumn.add(0, 1, 0));
        level.addFreshEntity(stand);
        spawned.add(stand);
        stand.setDeltaMovement(0, -0.5, 0);
        tick(level, tile);
        test.assertTrue(stand.getDeltaMovement().y == -0.5, "an armor stand is not pushable and is not lifted");
        stand.discard();
        stick.setPos(inColumn.x + 2, inColumn.y, inColumn.z);
        stick.setDeltaMovement(0, -0.5, 0);
        tick(level, tile);
        test.assertTrue(stick.getDeltaMovement().y == -0.5, "only the column above the block is lifted");
        stick.setPos(inColumn.x, inColumn.y + 11.5, inColumn.z);
        tick(level, tile);
        test.assertTrue(stick.getDeltaMovement().y == -0.5, "and only as far as the range");
        stick.setPos(inColumn.x, inColumn.y, inColumn.z);
        level.setBlock(lone.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        tick(level, tile);
        test.assertTrue(tile.gettingPower() && stick.getDeltaMovement().y == -0.5, "a signal on the block stops the lift");
        level.setBlock(lone.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(lone.above().east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        tick(level, tile);
        test.assertTrue(tile.gettingPower() && stick.getDeltaMovement().y == -0.5, "so does a signal on the block above it");
        level.setBlock(lone.above().east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        tick(level, tile);
        test.assertTrue(!tile.gettingPower() && Math.abs(stick.getDeltaMovement().y - (-0.5 + SPEED)) < 1e-9, "and without a signal it lifts again");
        stick.discard();

        // A real connected player is lifted too, a sneaking one is slowed.
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "lev-" + UUID.randomUUID().toString().substring(0, 6)), ClientInformation.createDefault());
        level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        try {
            player.setNoGravity(true);
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(inColumn.add(0, 1, 0));
            player.setDeltaMovement(0, -0.5, 0);
            player.fallDistance = 6F;
            tick(level, tile);
            test.assertTrue(Math.abs(player.getDeltaMovement().y - (-0.5 + SPEED)) < 1e-9 && player.fallDistance == 0F, "a player is lifted and keeps no fall: " + player.getDeltaMovement().y);
            player.setShiftKeyDown(true);
            player.setDeltaMovement(0, -0.5, 0);
            tick(level, tile);
            test.assertTrue(Math.abs(player.getDeltaMovement().y - (-0.5 * (double) 0.9F)) < 1e-9, "a sneaking player is slowed: " + player.getDeltaMovement().y);
        } finally {
            try {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
            } finally {
                player.discard();
                channel.finishAndReleaseAll();
            }
        }
        level.setBlock(lone, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        stack(test, level, origin.offset(6, 0, 0));
    }

    private static void stack(GameTestHelper test, ServerLevel level, BlockPos bottomPos) {
        TileLevitator bottom = place(level, bottomPos), middle = place(level, bottomPos.above()), top = place(level, bottomPos.above(2));
        test.assertTrue(range(level, top) == 30 && range(level, middle) == 0 && range(level, bottom) == 0, "a stack of three lifts 30 from the top one, the lower ones are walled in by the one above: "
                + top.getRange() + " " + middle.getRange() + " " + bottom.getRange());
        // A signal beside the middle one: it is no longer counted for the top one.
        level.setBlock(bottomPos.above().east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(bottom.isUpdateRequested() && !top.isUpdateRequested(),
                "a change of power tells the levitators UNDER it (the original's direction), the one above learns at its hundredth tick: " + bottom.isUpdateRequested() + " " + top.isUpdateRequested());
        test.assertTrue(range(level, top) == 10, "a powered levitator in the stack is not counted: " + top.getRange());
        level.setBlock(bottomPos.above().east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(range(level, top) == 30, "it is counted again without the signal");
        // A signal beside the bottom one: the middle one counts, the bottom does not.
        level.setBlock(bottomPos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(range(level, top) == 20, "a powered levitator two down ends the count there: " + top.getRange());
        level.setBlock(bottomPos.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        // A signal beside the top one stops it lifting but not the count.
        level.setBlock(bottomPos.above(2).east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(range(level, top) == 30 && top.gettingPower(), "a powered top one still counts its stack: " + top.getRange());
        level.setBlock(bottomPos.above(2).east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        // Placing one tells every levitator under it; removing one does too.
        range(level, top);
        TileLevitator crown = place(level, bottomPos.above(3));
        test.assertTrue(top.isUpdateRequested() && middle.isUpdateRequested() && bottom.isUpdateRequested() && crown.isUpdateRequested(), "placing a levitator tells all those under it");
        test.assertTrue(range(level, top) == 0 && range(level, crown) == 40, "the new top one lifts 40, the old one is walled in");
        range(level, middle);
        level.setBlock(bottomPos.above(3), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        test.assertTrue(top.isUpdateRequested() && middle.isUpdateRequested() && bottom.isUpdateRequested(), "removing one tells those under it too");
        level.setBlock(bottomPos.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(bottomPos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(bottomPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }
}
