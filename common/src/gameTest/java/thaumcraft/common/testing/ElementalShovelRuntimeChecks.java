package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.equipment.ElementalShovelGeometry;
import thaumcraft.common.items.equipment.ItemElementalShovel;
import thaumcraft.common.lib.network.ShovelOrientationNetworking;

/**
 * The Shovel of the Earthmover on real content: its numbers and repair, the infusion, the 3 x 3 mining of a connected survival player who is
 * looking at a face (up, north and east squares; sneaking, creative, a thaumium shovel, a stone anchor and a look that misses take one
 * block; a stone, bedrock or protected neighbour is left; the shovel breaking ends the square), the 3 x 3 placing of a clicked block from
 * the inventory (dirt for a grass block, occupied cells skipped, replaceable ones filled, creative free, a chest refused) and the
 * orientation key's server side (0, 1, 2, 0; only a held shovel; saved with the stack). NOT the key itself, the ghost preview or the
 * sparkles (client side).
 */
public final class ElementalShovelRuntimeChecks {
    private ElementalShovelRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        numbers(test);
        recipe(test, level);
        // High above the small GameTest structure, whose beacon and barrier blocks would otherwise lie in the fixture.
        BlockPos origin = test.absolutePos(new BlockPos(3, 64, 3));
        for (int x = (origin.getX() - 18) >> 4; x <= ((origin.getX() + 18) >> 4); x++) {
            for (int z = (origin.getZ() - 18) >> 4; z <= ((origin.getZ() + 18) >> 4); z++) level.getChunk(x, z);
        }
        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -8, -8), origin.offset(8, 8, 8))) {
            test.assertTrue(level.getBlockEntity(pos) == null, "the shovel fixture does not overwrite a block entity");
            saved.put(pos.immutable(), level.getBlockState(pos));
        }
        AABB area = new AABB(origin).inflate(10);
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "shovel-" + UUID.randomUUID().toString().substring(0, 6)), ClientInformation.createDefault());
        level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.setNoGravity(true);
        List<BlockPos> vetoed = new ArrayList<>();
        BlockEvent.Break guard = (world, pos, state, actor, xp) -> vetoed.contains(pos) ? EventResult.interruptFalse() : EventResult.pass();
        BlockEvent.BREAK.register(guard);
        try {
            test.assertTrue(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player, "a real connected player works the shovel");
            mining(test, level, player, origin, saved, area, vetoed);
            placing(test, level, player, origin, saved, area);
            orientation(test, level, player);
        } finally {
            BlockEvent.BREAK.unregister(guard);
            saved.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            level.getEntitiesOfClass(ItemEntity.class, area).forEach(drop -> drop.discard());
            try {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
            } finally {
                player.discard();
                channel.finishAndReleaseAll();
            }
        }
        test.succeed();
    }

    private static ItemStack shovel() { return new ItemStack(ThaumcraftEquipment.ELEMENTAL_SHOVEL.get()); }

    private static void numbers(GameTestHelper test) {
        ItemStack shovel = shovel();
        test.assertTrue(shovel.getMaxDamage() == 1500 && shovel.getItem().getEnchantmentValue() == 18 && shovel.getRarity() == Rarity.RARE,
                "1500 uses, enchantability 18, rare: " + shovel.getMaxDamage() + " " + shovel.getItem().getEnchantmentValue() + " " + shovel.getRarity());
        test.assertTrue(shovel.getDestroySpeed(Blocks.DIRT.defaultBlockState()) == 10F && shovel.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 1F,
                "speed 10 on dirt, none on stone: " + shovel.getDestroySpeed(Blocks.DIRT.defaultBlockState()));
        double[] stats = {0, 0};
        shovel.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) stats[0] += modifier.amount();
            if (attribute.is(Attributes.ATTACK_SPEED)) stats[1] += modifier.amount();
        });
        test.assertTrue(stats[0] == 4.0 && stats[1] == -3.0, "legacy spade 1 + material 3 = 4 damage, -3 speed: " + stats[0] + " " + stats[1]);
        test.assertTrue(shovel.getItem().isValidRepairItem(shovel, new ItemStack(item("thaumcraft:thaumium_ingot"))) && !shovel.getItem().isValidRepairItem(shovel, new ItemStack(Items.IRON_INGOT)),
                "repaired with the thaumium ingot alone");
        test.assertTrue(shovel.is(ItemTags.SHOVELS) && ItemElementalShovel.orientation(shovel) == 0, "a shovel by tag, orientation 0 until the key is pressed");
    }

    private static void recipe(GameTestHelper test, ServerLevel level) {
        var infusion = (InfusionStackRecipe) level.getRecipeManager().byKey(id("infusion/elemental_shovel")).orElseThrow(() -> new AssertionError("no recipe")).value();
        test.assertTrue(infusion.research().equals("ELEMENTALSHOVEL") && infusion.instability() == 1 && infusion.result().is(item("thaumcraft:elemental_shovel"))
                        && infusion.result().getCount() == 1 && infusion.aspects().equals(Map.of("terra", 16, "fabrico", 8)) && infusion.components().size() == 4,
                "the infusion: ELEMENTALSHOVEL, instability 1, 16 earth + 8 craft, four components: " + infusion.aspects());
        ItemStack centre = new ItemStack(item("thaumcraft:thaumium_shovel"));
        List<ItemStack> parts = List.of(new ItemStack(item("thaumcraft:earth_shard")), new ItemStack(item("thaumcraft:earth_shard")),
                new ItemStack(Items.DIAMOND), new ItemStack(item("thaumcraft:greatwood_log")));
        test.assertTrue(infusion.matches(new InfusionRecipeInput(centre, parts), level), "a thaumium shovel, two earth shards, a diamond and a greatwood log complete it");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(new ItemStack(Items.IRON_SHOVEL), parts), level), "an iron shovel is not the centre");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(centre, List.of(parts.get(0), new ItemStack(item("thaumcraft:fire_shard")), parts.get(2), parts.get(3))), level), "a fire shard is not the second earth shard");
    }

    // ---- mining ---------------------------------------------------------------------------------------------------------

    /** A cube of dirt, x and z within 4 of the origin, y from 4 below it up to it. */
    private static void cube(ServerLevel level, BlockPos origin, Map<BlockPos, BlockState> saved) {
        for (BlockPos pos : saved.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -4, -4), origin.offset(4, 0, 4))) level.setBlock(pos, Blocks.DIRT.defaultBlockState(), QUIET);
    }

    /** The player stands {@code distance} blocks out from the anchor on {@code face}'s side and looks at its centre. */
    private static void stand(ServerPlayer player, BlockPos anchor, Direction face, int distance) {
        Vec3 target = Vec3.atCenterOf(anchor);
        Vec3 eye = target.add(face.getStepX() * distance, face.getStepY() * distance, face.getStepZ() * distance);
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
    }

    private static int removed(ServerLevel level, List<BlockPos> cells) {
        int gone = 0;
        for (BlockPos cell : cells) if (level.getBlockState(cell).isAir()) ++gone;
        return gone;
    }

    private static int dropped(ServerLevel level, AABB area, Item what) {
        int count = 0;
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, area)) if (drop.getItem().is(what)) count += drop.getItem().getCount();
        return count;
    }

    private static ItemStack arm(ServerPlayer player, ItemStack tool, GameType mode, boolean sneaking) {
        player.setGameMode(mode);
        player.setShiftKeyDown(sneaking);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return tool;
    }

    private static void mining(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin, Map<BlockPos, BlockState> saved, AABB area, List<BlockPos> vetoed) {
        // The top square: the player above the middle of the top layer looks down at it.
        BlockPos top = origin;
        cube(level, origin, saved);
        stand(player, top, Direction.UP, 3);
        ItemStack shovel = arm(player, shovel(), GameType.SURVIVAL, false);
        test.assertTrue(player.gameMode.destroyBlock(top), "the break goes through");
        List<BlockPos> square = ElementalShovelGeometry.plane(top, Direction.UP);
        test.assertTrue(removed(level, square) == 9 && removed(level, List.of(top.offset(2, 0, 0), top.offset(0, -1, 0), top.offset(0, 0, 2))) == 0 && shovel.getDamageValue() == 9
                        && dropped(level, area, Items.DIRT) == 9,
                "a top face takes the 3 x 3 square across it, nine blocks, nine uses, nine dirt: " + removed(level, square) + " gone, " + shovel.getDamageValue() + " uses, " + dropped(level, area, Items.DIRT) + " dirt");
        // A side face (north) and an east face.
        for (Direction face : new Direction[] {Direction.NORTH, Direction.EAST}) {
            cube(level, origin, saved);
            level.getEntitiesOfClass(ItemEntity.class, area).forEach(drop -> drop.discard());
            BlockPos anchor = origin.offset(face.getStepX() * 4, -2, face.getStepZ() * 4);
            stand(player, anchor, face, 3);
            ItemStack tool = arm(player, shovel(), GameType.SURVIVAL, false);
            player.gameMode.destroyBlock(anchor);
            List<BlockPos> wall = ElementalShovelGeometry.plane(anchor, face);
            test.assertTrue(removed(level, wall) == 9 && tool.getDamageValue() == 9, face + " face: nine blocks of the wall across it, nine uses: " + removed(level, wall) + " " + tool.getDamageValue());
            test.assertTrue(level.getBlockState(anchor.relative(face.getOpposite())).is(Blocks.DIRT), face + " face: nothing behind the wall goes");
        }
        // Neighbours the shovel does not mine, unbreakable ones and a vetoed one stay.
        cube(level, origin, saved);
        level.getEntitiesOfClass(ItemEntity.class, area).forEach(drop -> drop.discard());
        level.setBlock(top.offset(1, 0, 0), Blocks.STONE.defaultBlockState(), QUIET);
        level.setBlock(top.offset(-1, 0, 0), Blocks.BEDROCK.defaultBlockState(), QUIET);
        vetoed.add(top.offset(0, 0, 1));
        stand(player, top, Direction.UP, 3);
        ItemStack picky = arm(player, shovel(), GameType.SURVIVAL, false);
        player.gameMode.destroyBlock(top);
        vetoed.clear();
        test.assertTrue(level.getBlockState(top.offset(1, 0, 0)).is(Blocks.STONE) && level.getBlockState(top.offset(-1, 0, 0)).is(Blocks.BEDROCK) && level.getBlockState(top.offset(0, 0, 1)).is(Blocks.DIRT)
                        && removed(level, ElementalShovelGeometry.plane(top, Direction.UP)) == 6 && picky.getDamageValue() == 6,
                "stone, bedrock and a vetoed block stay, the other six go for six uses: " + removed(level, ElementalShovelGeometry.plane(top, Direction.UP)) + " " + picky.getDamageValue());
        // Everything that breaks one block only.
        record Case(String name, ItemStack tool, GameType mode, boolean sneaking, boolean look) {}
        for (Case one : List.of(new Case("sneaking", shovel(), GameType.SURVIVAL, true, true), new Case("creative", shovel(), GameType.CREATIVE, false, true),
                new Case("the thaumium shovel", new ItemStack(item("thaumcraft:thaumium_shovel")), GameType.SURVIVAL, false, true),
                new Case("a look that misses", shovel(), GameType.SURVIVAL, false, false))) {
            cube(level, origin, saved);
            if (one.look()) stand(player, top, Direction.UP, 3);
            else stand(player, top.offset(2, 0, 2), Direction.UP, 3);
            arm(player, one.tool(), one.mode(), one.sneaking());
            player.gameMode.destroyBlock(top);
            test.assertTrue(removed(level, ElementalShovelGeometry.plane(top, Direction.UP)) == 1 && level.getBlockState(top).isAir(), one.name() + ": one block only: " + removed(level, ElementalShovelGeometry.plane(top, Direction.UP)));
        }
        // A stone anchor is no shovel block: it goes alone.
        cube(level, origin, saved);
        level.setBlock(top, Blocks.STONE.defaultBlockState(), QUIET);
        stand(player, top, Direction.UP, 3);
        arm(player, shovel(), GameType.SURVIVAL, false);
        player.gameMode.destroyBlock(top);
        test.assertTrue(removed(level, ElementalShovelGeometry.plane(top, Direction.UP)) == 1, "a stone anchor goes alone");
        // The shovel gives out two blocks after the anchor: the square stops there.
        cube(level, origin, saved);
        stand(player, top, Direction.UP, 3);
        ItemStack worn = shovel();
        worn.setDamageValue(worn.getMaxDamage() - 3);
        arm(player, worn, GameType.SURVIVAL, false);
        player.gameMode.destroyBlock(top);
        test.assertTrue(removed(level, ElementalShovelGeometry.plane(top, Direction.UP)) == 3 && player.getMainHandItem().isEmpty(),
                "the shovel breaks on its third block and the rest of the square stays: " + removed(level, ElementalShovelGeometry.plane(top, Direction.UP)));
        player.setShiftKeyDown(false);
        player.setGameMode(GameType.SURVIVAL);
    }

    // ---- placing --------------------------------------------------------------------------------------------------------

    private static void flat(ServerLevel level, BlockPos origin, Map<BlockPos, BlockState> saved, Block floor) {
        for (BlockPos pos : saved.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -1, -4), origin.offset(4, -1, 4))) level.setBlock(pos, floor.defaultBlockState(), QUIET);
    }

    private static net.minecraft.world.InteractionResult click(ServerPlayer player, ItemStack tool, BlockPos pos, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5), face, pos, false);
        return tool.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static int count(ServerLevel level, BlockPos origin, Block block) {
        int found = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, 0, -4), origin.offset(4, 4, 4))) if (level.getBlockState(pos).is(block)) ++found;
        return found;
    }

    /** The stacks go to the main inventory (past the hotbar), so the shovel in the selected hotbar slot never replaces one. */
    private static void fill(ServerPlayer player, ItemStack... stacks) {
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        for (int index = 0; index < stacks.length; ++index) player.getInventory().setItem(9 + index, stacks[index]);
    }

    private static void placing(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin, Map<BlockPos, BlockState> saved, AABB area) {
        BlockPos floor = origin.below();
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        player.setYRot(0);
        // Nine dirt in the inventory lay nine dirt on a dirt floor.
        flat(level, origin, saved, Blocks.DIRT);
        fill(player, new ItemStack(Items.DIRT, 9));
        ItemStack shovel = shovel();
        var result = click(player, shovel, floor, Direction.UP);
        int laid = 0;
        for (BlockPos cell : ElementalShovelGeometry.layout(floor, Direction.UP, 0, 0)) if (level.getBlockState(cell).is(Blocks.DIRT)) ++laid;
        test.assertTrue(laid == 9 && player.getInventory().countItem(Items.DIRT) == 0 && shovel.getDamageValue() == 9 && result == net.minecraft.world.InteractionResult.CONSUME,
                "a click lays the 3 x 3 square above the clicked face from nine dirt, nine uses: " + laid + " laid, " + player.getInventory().countItem(Items.DIRT) + " left, " + shovel.getDamageValue() + " uses");
        // Not enough in the inventory: as many as there are.
        flat(level, origin, saved, Blocks.DIRT);
        fill(player, new ItemStack(Items.DIRT, 5));
        ItemStack short5 = shovel();
        click(player, short5, floor, Direction.UP);
        laid = 0;
        for (BlockPos cell : ElementalShovelGeometry.layout(floor, Direction.UP, 0, 0)) if (level.getBlockState(cell).is(Blocks.DIRT)) ++laid;
        test.assertTrue(laid == 5 && short5.getDamageValue() == 5, "five dirt lay five blocks, five uses: " + laid + " " + short5.getDamageValue());
        // A grass block with only dirt in the inventory is laid as dirt; with grass in the inventory, as grass.
        flat(level, origin, saved, Blocks.GRASS_BLOCK);
        fill(player, new ItemStack(Items.DIRT, 9));
        click(player, shovel(), floor, Direction.UP);
        test.assertTrue(count(level, origin, Blocks.DIRT) == 9 && count(level, origin, Blocks.GRASS_BLOCK) == 0, "grass with only dirt to hand is laid as dirt: " + count(level, origin, Blocks.DIRT));
        flat(level, origin, saved, Blocks.GRASS_BLOCK);
        fill(player, new ItemStack(Items.GRASS_BLOCK, 9), new ItemStack(Items.DIRT, 9));
        click(player, shovel(), floor, Direction.UP);
        test.assertTrue(count(level, origin, Blocks.GRASS_BLOCK) == 9 && player.getInventory().countItem(Items.DIRT) == 9, "with grass blocks to hand grass is laid and the dirt is kept");
        // Occupied cells are skipped, replaceable ones are filled.
        flat(level, origin, saved, Blocks.DIRT);
        level.setBlock(origin.offset(1, 0, 0), Blocks.STONE.defaultBlockState(), QUIET);
        level.setBlock(origin.offset(0, 0, 1), Blocks.SHORT_GRASS.defaultBlockState(), QUIET);
        fill(player, new ItemStack(Items.DIRT, 9));
        ItemStack mixed = shovel();
        click(player, mixed, floor, Direction.UP);
        test.assertTrue(level.getBlockState(origin.offset(1, 0, 0)).is(Blocks.STONE) && level.getBlockState(origin.offset(0, 0, 1)).is(Blocks.DIRT)
                        && player.getInventory().countItem(Items.DIRT) == 1 && mixed.getDamageValue() == 8,
                "stone is skipped, short grass is built over: " + player.getInventory().countItem(Items.DIRT) + " dirt left, " + mixed.getDamageValue() + " uses");
        // Creative pays nothing.
        flat(level, origin, saved, Blocks.DIRT);
        player.setGameMode(GameType.CREATIVE);
        fill(player);
        ItemStack free = shovel();
        click(player, free, floor, Direction.UP);
        test.assertTrue(count(level, origin, Blocks.DIRT) == 9 && free.getDamageValue() == 0, "a creative player lays the square for nothing");
        player.setGameMode(GameType.SURVIVAL);
        // A block with a block entity is not copied.
        flat(level, origin, saved, Blocks.DIRT);
        level.setBlock(floor, Blocks.CHEST.defaultBlockState(), QUIET);
        fill(player, new ItemStack(Items.CHEST, 9));
        click(player, shovel(), floor, Direction.UP);
        test.assertTrue(count(level, origin, Blocks.CHEST) == 0 && player.getInventory().countItem(Items.CHEST) == 9, "a chest is not copied");
        // Orientation 1 on a top face turns the square upright: the cells above the floor line are the only free ones.
        flat(level, origin, saved, Blocks.DIRT);
        fill(player, new ItemStack(Items.DIRT, 9));
        ItemStack upright = shovel();
        upright.set(ThaumcraftEquipment.SHOVEL_ORIENTATION.get(), 1);
        click(player, upright, floor, Direction.UP);
        List<BlockPos> wanted = new ArrayList<>();
        for (BlockPos cell : ElementalShovelGeometry.layout(floor, Direction.UP, 1, 0)) if (cell.getY() > floor.getY()) wanted.add(cell);
        int uprightLaid = 0;
        for (BlockPos cell : wanted) if (level.getBlockState(cell).is(Blocks.DIRT)) ++uprightLaid;
        test.assertTrue(wanted.size() == 6 && uprightLaid == 6 && upright.getDamageValue() == 6 && count(level, origin, Blocks.DIRT) == 6,
                "orientation 1 lays an upright square (its free six cells): " + uprightLaid + " of " + wanted.size() + ", " + upright.getDamageValue() + " uses");
        // Clean up the drops of nothing; the area is restored by the caller.
        fill(player);
    }

    // ---- orientation ----------------------------------------------------------------------------------------------------

    private static void orientation(GameTestHelper test, ServerLevel level, ServerPlayer player) {
        ItemStack shovel = shovel();
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        List<Integer> seen = new ArrayList<>();
        for (int press = 0; press < 4; ++press) {
            test.assertTrue(ShovelOrientationNetworking.handle(player), "a press with the shovel in hand turns it");
            seen.add(ItemElementalShovel.orientation(shovel));
            if (press == 1) {
                // Every orientation survives a save: check the last one, the one a narrower codec would lose.
                var context = level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                var saved = ItemStack.CODEC.encodeStart(context, shovel).getOrThrow();
                ItemStack loaded = ItemStack.CODEC.parse(context, saved).getOrThrow();
                test.assertTrue(ItemElementalShovel.orientation(loaded) == 2, "orientation 2 is saved with the stack: " + ItemElementalShovel.orientation(loaded));
            }
        }
        test.assertTrue(seen.equals(List.of(1, 2, 0, 1)), "the key cycles 0 -> 1 -> 2 -> 0 -> 1: " + seen);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        test.assertTrue(!ShovelOrientationNetworking.handle(player), "a press with anything else in hand does nothing");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        test.assertTrue(!ShovelOrientationNetworking.handle(player), "nor with an empty hand");
        ItemStack offhand = shovel();
        player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        test.assertTrue(!ShovelOrientationNetworking.handle(player) && ItemElementalShovel.orientation(offhand) == 0, "the original reads only the main hand");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }
}
