package thaumcraft.common.testing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.equipment.ElementalHoeRules;

/**
 * The Hoe of Growth on real content: its numbers and repair, the infusion that makes it, tilling a 3 x 3 square (a use per block; a sneaking
 * player tills one; a block with something above it is skipped; creative pays nothing), bone meal on a crop when nothing tilled (one use,
 * none on a grown crop), and the magical saplings: a greatwood sapling grows for 5 uses while 20 remain, a silverwood sapling for 25 while
 * 150 remain, refused one use short of the margin and still paid when the tree finds no room. A real survival player clicks real blocks. NOT
 * the sparkles and the sound (client side) or the hoe in a dispenser.
 */
public final class ElementalHoeRuntimeChecks {
    private ElementalHoeRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        numbers(test);
        recipe(test, level);
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        // High above the small GameTest structure, whose beacon and barrier blocks would otherwise lie in the fixture.
        BlockPos origin = test.absolutePos(new BlockPos(3, 64, 3));
        for (int x = (origin.getX() - 18) >> 4; x <= ((origin.getX() + 18) >> 4); x++) {
            for (int z = (origin.getZ() - 18) >> 4; z <= ((origin.getZ() + 18) >> 4); z++) level.getChunk(x, z);
        }
        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-13, -2, -13), origin.offset(13, 26, 13))) {
            test.assertTrue(level.getBlockEntity(pos) == null, "the hoe fixture does not overwrite a block entity");
            saved.put(pos.immutable(), level.getBlockState(pos));
        }
        var oldMode = player.gameMode.getGameModeForPlayer();
        var oldPosition = player.position();
        var oldHand = player.getMainHandItem().copy();
        long resumeSeed = level.random.nextLong();
        try {
            player.setGameMode(GameType.SURVIVAL);
            tilling(test, level, player, origin, saved);
            adventure(test, level, player, origin, saved);
            blows(test, player);
            bonemeal(test, level, player, origin, saved);
            saplings(test, level, player, origin, saved);
        } finally {
            saved.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, oldHand);
            player.setGameMode(oldMode);
            player.setPos(oldPosition);
            level.random.setSeed(resumeSeed);
        }
        test.succeed();
    }

    private static ItemStack hoe() { return new ItemStack(ThaumcraftEquipment.ELEMENTAL_HOE.get()); }

    private static void numbers(GameTestHelper test) {
        ItemStack hoe = hoe();
        test.assertTrue(hoe.getMaxDamage() == 1500 && hoe.getItem().getEnchantmentValue() == 5 && hoe.getRarity() == Rarity.RARE,
                "1500 uses, enchantability 5 (not the tier's 18), rare: " + hoe.getMaxDamage() + " " + hoe.getItem().getEnchantmentValue() + " " + hoe.getRarity());
        double[] stats = {0, 0};
        hoe.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) stats[0] += modifier.amount();
            if (attribute.is(Attributes.ATTACK_SPEED)) stats[1] += modifier.amount();
        });
        test.assertTrue(stats[0] == 0 && stats[1] == -1, "a hoe has no attack damage in the original: " + stats[0] + " " + stats[1]);
        test.assertTrue(hoe.getItem().isValidRepairItem(hoe, new ItemStack(item("thaumcraft:thaumium_ingot"))) && !hoe.getItem().isValidRepairItem(hoe, new ItemStack(Items.IRON_INGOT)),
                "repaired with the thaumium ingot alone");
        test.assertTrue(hoe.is(ItemTags.HOES), "a hoe by tag");
    }

    private static void recipe(GameTestHelper test, ServerLevel level) {
        var infusion = (InfusionStackRecipe) level.getRecipeManager().byKey(id("infusion/elemental_hoe")).orElseThrow(() -> new AssertionError("no recipe")).value();
        test.assertTrue(infusion.research().equals("ELEMENTALHOE") && infusion.instability() == 1 && infusion.result().is(item("thaumcraft:elemental_hoe"))
                        && infusion.result().getCount() == 1 && infusion.aspects().equals(Map.of("meto", 8, "herba", 8, "terra", 8)) && infusion.components().size() == 4,
                "the infusion: ELEMENTALHOE, instability 1, 8 harvest + 8 plant + 8 earth, four components: " + infusion.aspects());
        ItemStack centre = new ItemStack(item("thaumcraft:thaumium_hoe"));
        List<ItemStack> parts = List.of(new ItemStack(item("thaumcraft:order_shard")), new ItemStack(item("thaumcraft:entropy_shard")),
                new ItemStack(Items.DIAMOND), new ItemStack(item("thaumcraft:greatwood_log")));
        test.assertTrue(infusion.matches(new InfusionRecipeInput(centre, parts), level), "a thaumium hoe, an order and an entropy shard, a diamond and a greatwood log complete it");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(new ItemStack(Items.IRON_HOE), parts), level), "an iron hoe is not the centre");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(centre, List.of(parts.get(0), parts.get(0), parts.get(2), parts.get(3))), level), "two order shards are not order and entropy");
        test.assertTrue(!infusion.matches(new InfusionRecipeInput(centre, parts.subList(0, 3)), level), "no log does not");
    }

    /** Clear the working volume and lay a floor of {@code floor} one block under {@code origin}'s layer. */
    private static void clear(ServerLevel level, Map<BlockPos, BlockState> saved) {
        for (BlockPos pos : saved.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private static InteractionResult click(ServerPlayer player, ItemStack hoe, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        return hoe.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static void tilling(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin, Map<BlockPos, BlockState> saved) {
        clear(level, saved);
        Block[][] layout = {{Blocks.DIRT, Blocks.DIRT, Blocks.DIRT}, {Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT}, {Blocks.DIRT_PATH, Blocks.DIRT, Blocks.DIRT}};
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) level.setBlock(origin.offset(x, 0, z), layout[x + 1][z + 1].defaultBlockState(), Block.UPDATE_CLIENTS);
        ItemStack hoe = hoe();
        InteractionResult result = click(player, hoe, origin);
        int farmland = 0, dirt = 0;
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) {
            BlockState state = level.getBlockState(origin.offset(x, 0, z));
            if (state.is(Blocks.FARMLAND)) ++farmland;
            else if (state.is(Blocks.DIRT)) ++dirt;
        }
        test.assertTrue(result.consumesAction() && farmland == 8 && level.getBlockState(origin.offset(0, 0, 1)).is(Blocks.DIRT) && dirt == 1 && hoe.getDamageValue() == 9,
                "one click tills the clicked block and the eight around it, coarse dirt becomes dirt, a use each: " + farmland + " farmland, " + dirt + " dirt, " + hoe.getDamageValue() + " uses, " + result);
        // Something above a block blocks that block alone.
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) level.setBlock(origin.offset(x, 0, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(origin.offset(-1, 1, -1), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        ItemStack covered = hoe();
        click(player, covered, origin);
        test.assertTrue(level.getBlockState(origin.offset(-1, 0, -1)).is(Blocks.DIRT) && covered.getDamageValue() == 8, "a covered block is skipped and costs nothing: " + covered.getDamageValue());
        level.setBlock(origin.offset(-1, 1, -1), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        // A sneaking player tills one.
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) level.setBlock(origin.offset(x, 0, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        player.setShiftKeyDown(true);
        ItemStack sneaking = hoe();
        click(player, sneaking, origin);
        player.setShiftKeyDown(false);
        int tilled = 0;
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) if (level.getBlockState(origin.offset(x, 0, z)).is(Blocks.FARMLAND)) ++tilled;
        test.assertTrue(tilled == 1 && level.getBlockState(origin).is(Blocks.FARMLAND) && sneaking.getDamageValue() == 1, "sneaking tills only the clicked block: " + tilled + " " + sneaking.getDamageValue());
        // The thaumium hoe tills one.
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) level.setBlock(origin.offset(x, 0, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        click(player, new ItemStack(item("thaumcraft:thaumium_hoe")), origin);
        tilled = 0;
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) if (level.getBlockState(origin.offset(x, 0, z)).is(Blocks.FARMLAND)) ++tilled;
        test.assertTrue(tilled == 1, "the thaumium hoe tills one block: " + tilled);
        // Nothing to do at all: stone alone in its layer.
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) level.setBlock(origin.offset(x, 0, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(origin, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        ItemStack idle = hoe();
        test.assertTrue(click(player, idle, origin) == InteractionResult.PASS && idle.getDamageValue() == 0, "stone: nothing happens and nothing is paid");
        // Creative pays nothing.
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) level.setBlock(origin.offset(x, 0, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        player.setGameMode(GameType.CREATIVE);
        ItemStack free = hoe();
        click(player, free, origin);
        test.assertTrue(level.getBlockState(origin.offset(1, 0, 1)).is(Blocks.FARMLAND) && free.getDamageValue() == 0, "a creative player tills and pays nothing");
        player.setGameMode(GameType.SURVIVAL);
    }

    /** The original asked {@code canPlayerEdit} of every block it tilled: an adventure player may only reach what the hoe is allowed to be used on. */
    private static void adventure(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin, Map<BlockPos, BlockState> saved) {
        clear(level, saved);
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) {
            level.setBlock(origin.offset(x, 0, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(origin.offset(x, -1, z), (x == 1 && z == 0 ? Blocks.STONE : Blocks.DIRT).defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        ItemStack hoe = hoe();
        hoe.set(DataComponents.CAN_PLACE_ON, new AdventureModePredicate(List.of(BlockPredicate.Builder.block().of(Blocks.DIRT).build()), true));
        player.setGameMode(GameType.ADVENTURE);
        click(player, hoe, origin);
        player.setGameMode(GameType.SURVIVAL);
        int farmland = 0;
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) if (level.getBlockState(origin.offset(x, 0, z)).is(Blocks.FARMLAND)) ++farmland;
        test.assertTrue(farmland == 8 && level.getBlockState(origin.offset(1, 0, 0)).is(Blocks.DIRT) && hoe.getDamageValue() == 8,
                "an adventure player tills what the hoe may be used on, eight blocks, not the one over stone: " + farmland + " " + hoe.getDamageValue());
    }

    /** A hit never spends the hoe (in 1.7 ItemHoe was not a digger). */
    private static void blows(GameTestHelper test, ServerPlayer player) {
        Pig pig = test.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1);
        ItemStack hoe = hoe();
        hoe.hurtEnemy(pig, player);
        hoe.postHurtEnemy(pig, player);
        test.assertTrue(hoe.getDamageValue() == 0 && !pig.isOnFire(), "hitting a pig costs the hoe nothing and sets nothing alight: " + hoe.getDamageValue());
    }

    private static void bonemeal(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin, Map<BlockPos, BlockState> saved) {
        clear(level, saved);
        level.setBlock(origin.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(origin, Blocks.WHEAT.defaultBlockState(), Block.UPDATE_CLIENTS);
        ItemStack hoe = hoe();
        InteractionResult result = click(player, hoe, origin);
        int age = level.getBlockState(origin).getValue(CropBlock.AGE);
        test.assertTrue(result.consumesAction() && age >= 2 && hoe.getDamageValue() == 1, "a click that tilled nothing fertilises the crop, one use: age " + age + ", " + hoe.getDamageValue() + " uses, " + result);
        level.setBlock(origin, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7), Block.UPDATE_CLIENTS);
        ItemStack grown = hoe();
        test.assertTrue(click(player, grown, origin) == InteractionResult.PASS && grown.getDamageValue() == 0, "a grown crop takes no bone meal and costs nothing");
        // Tilling comes first: grass beside the crop's layer is not reached, but the crop's own layer has none to till.
        level.setBlock(origin, Blocks.WHEAT.defaultBlockState(), Block.UPDATE_CLIENTS);
        ItemStack thaumium = new ItemStack(item("thaumcraft:thaumium_hoe"));
        click(player, thaumium, origin);
        test.assertTrue(level.getBlockState(origin).getValue(CropBlock.AGE) == 0 && thaumium.getDamageValue() == 0, "the thaumium hoe fertilises nothing");
    }

    private static void saplings(GameTestHelper test, ServerLevel level, ServerPlayer player, BlockPos origin, Map<BlockPos, BlockState> saved) {
        for (boolean silverwood : new boolean[] {false, true}) {
            var sapling = silverwood ? ThaumcraftNaturals.SILVERWOOD_SAPLING.get() : ThaumcraftNaturals.GREATWOOD_SAPLING.get();
            var log = silverwood ? ThaumcraftNaturals.SILVERWOOD_LOG.get() : ThaumcraftNaturals.GREATWOOD_LOG.get();
            int margin = ElementalHoeRules.margin(silverwood), wear = ElementalHoeRules.wear(silverwood);
            String species = silverwood ? "silverwood" : "greatwood";
            clear(level, saved);
            for (BlockPos soil : BlockPos.betweenClosed(origin.offset(-3, -1, -3), origin.offset(3, -1, 3))) level.setBlock(soil, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(origin, sapling.defaultBlockState(), Block.UPDATE_CLIENTS);
            // One use short of the margin: refused.
            ItemStack short_ = hoe();
            short_.setDamageValue(short_.getMaxDamage() - margin + 1);
            int before = short_.getDamageValue();
            test.assertTrue(click(player, short_, origin) == InteractionResult.PASS && short_.getDamageValue() == before && level.getBlockState(origin).is(sapling),
                    species + ": " + (margin - 1) + " uses left is one short: nothing grows, nothing is paid");
            // Exactly the margin: it grows (the generator's random is fixed to a seed known to succeed).
            ItemStack exact = hoe();
            exact.setDamageValue(exact.getMaxDamage() - margin);
            level.random.setSeed(silverwood ? 7 : 0);
            InteractionResult result = click(player, exact, origin);
            int logs = 0;
            for (BlockPos pos : saved.keySet()) if (level.getBlockState(pos).is(log)) ++logs;
            test.assertTrue(result.consumesAction() && exact.getDamageValue() == exact.getMaxDamage() - margin + wear && !level.getBlockState(origin).is(sapling) && logs > 16,
                    species + ": exactly " + margin + " uses left grows the tree for " + wear + " uses: " + result + ", " + exact.getDamageValue() + " uses, " + logs + " logs");
        }
        // The thaumium hoe grows nothing.
        clear(level, saved);
        for (BlockPos soil : BlockPos.betweenClosed(origin.offset(-3, -1, -3), origin.offset(3, -1, 3))) level.setBlock(soil, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(origin, ThaumcraftNaturals.GREATWOOD_SAPLING.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        ItemStack plain = new ItemStack(item("thaumcraft:thaumium_hoe"));
        test.assertTrue(click(player, plain, origin) == InteractionResult.PASS && level.getBlockState(origin).is(ThaumcraftNaturals.GREATWOOD_SAPLING.get()), "the thaumium hoe leaves a sapling alone");
        // One soil block cannot sustain the greatwood trunk: no tree, but the original still takes its 5 uses.
        clear(level, saved);
        level.setBlock(origin.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(origin, ThaumcraftNaturals.GREATWOOD_SAPLING.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        ItemStack blocked = hoe();
        level.random.setSeed(0);
        InteractionResult result = click(player, blocked, origin);
        test.assertTrue(result.consumesAction() && blocked.getDamageValue() == ElementalHoeRules.GREATWOOD_WEAR && level.getBlockState(origin).is(ThaumcraftNaturals.GREATWOOD_SAPLING.get()),
                "a tree with no room is paid for all the same, and the sapling stays: " + result + ", " + blocked.getDamageValue() + " uses");
    }
}
