package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.blocks.BlockCustomOre;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftOres;
import thaumcraft.common.config.ThaumcraftWorldgen;

/** One loader-neutral check of native loot, tool tiers, actual survival harvest/XP and loaded data. */
public final class OreRuntimeChecks {
    private OreRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        var stateBefore = level.getBlockState(pos);
        var entityBefore = level.getBlockEntity(pos);
        CompoundTag savedEntity = entityBefore == null ? null : entityBefore.saveWithFullMetadata(level.registryAccess());
        var positionBefore = player.position();
        var modeBefore = player.gameMode.getGameModeForPlayer();
        int selectedBefore = player.getInventory().selected;
        float exhaustionBefore = player.getFoodData().getExhaustionLevel();
        List<ItemStack> inventoryBefore = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) inventoryBefore.add(player.getInventory().getItem(slot).copy());
        // Exact count comparisons reseed only this disposable GameTest world's
        // RNG, not a production world or the separate per-block private XP RNG.
        long resumeSeed = level.random.nextLong();
        try {
            player.getInventory().selected = 0;
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5);
            var enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
            silk.enchant(enchantments.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
            ItemStack fortune = new ItemStack(Items.IRON_PICKAXE);
            fortune.enchant(enchantments.getHolderOrThrow(Enchantments.FORTUNE), 3);
            ItemStack iron = new ItemStack(Items.IRON_PICKAXE);
            ItemStack wood = new ItemStack(Items.WOODEN_PICKAXE);
            List<BlockCustomOre> ores = new ArrayList<>();
            ores.add(ThaumcraftOres.CINNABAR.get());
            for (var entry : ThaumcraftOres.INFUSED.values()) ores.add(entry.get());
            ores.add(ThaumcraftOres.AMBER.get());
            for (BlockCustomOre ore : ores) {
                var state = ore.defaultBlockState();
                test.assertTrue(state.getDestroySpeed(level, pos) == 1.5F && ore.getExplosionResistance() == 5.0F,
                        "source ore hardness/resistance: " + ore);
                test.assertTrue(iron.isCorrectToolForDrops(state) && wood.isCorrectToolForDrops(state) == (ore.aspectTag() != null),
                        "iron tier for cinnabar/amber only; wooden pick for all infused stone: " + ore);
                var silkDrops = Block.getDrops(state, level, pos, null, player, silk);
                test.assertTrue(silkDrops.size() == 1 && silkDrops.getFirst().is(ore.asItem()) && silkDrops.getFirst().getCount() == 1,
                        "loaded native loot table silk-drops exactly its own ore: " + ore);
                test.assertTrue(ore.getExpDrop(state, level, pos, null, player, silk) == 0, "silk suppresses XP before drawing: " + ore);
                for (int fortuneLevel : new int[] {0, 3}) {
                    ItemStack tool = fortuneLevel == 0 ? iron : fortune;
                    for (int seed = 1; seed <= 32; seed++) {
                        int bound = (ore.originalVariant() == 7 ? 1 : 2) + fortuneLevel;
                        level.random.setSeed(seed);
                        int expected = ore.originalVariant() == 0 ? 1 : 1 + level.random.nextInt(bound);
                        level.random.setSeed(seed);
                        var drops = Block.getDrops(state, level, pos, null, player, tool);
                        Item item = dropItem(ore);
                        test.assertTrue(drops.stream().allMatch(stack -> stack.is(item))
                                && drops.stream().mapToInt(ItemStack::getCount).sum() == expected,
                                "native loot preserves exact world-RNG 1+nextInt(base+fortune), seed " + seed + ": " + ore);
                        if (ore.aspectTag() != null) test.assertTrue(drops.size() == expected && drops.stream().allMatch(stack -> stack.getCount() == 1),
                                "infused drops are q singleton stacks, never one merged stack");
                    }
                }
                level.random.setSeed(707L);
                long expectedNextWorldDraw = level.random.nextLong();
                level.random.setSeed(707L);
                for (int i = 0; i < 32; i++) {
                    int xp = ore.getExpDrop(state, level, pos, null, player, iron);
                    test.assertTrue(xp >= (ore.originalVariant() == 7 ? 1 : 0)
                            && xp <= (ore.originalVariant() == 0 ? 0 : ore.originalVariant() == 7 ? 4 : 3), "original independent XP bounds");
                }
                test.assertTrue(level.random.nextLong() == expectedNextWorldDraw, "XP must not advance world RNG");
                harvest(test, player, pos, ore, silk, ore.asItem(), 1, 1, 0, 0);
                harvest(test, player, pos, ore, ore.aspectTag() == null ? iron : wood, dropItem(ore),
                        1, ore.aspectTag() == null ? 1 : 2, ore.originalVariant() == 7 ? 1 : 0,
                        ore.originalVariant() == 0 ? 0 : ore.originalVariant() == 7 ? 4 : 3);
            }
            harvest(test, player, pos, ThaumcraftOres.CINNABAR.get(), new ItemStack(Items.STONE_PICKAXE),
                    ThaumcraftOres.CINNABAR.get().asItem(), 0, 0, 0, 0);
            harvest(test, player, pos, ThaumcraftOres.AMBER.get(), wood, dropItem(ThaumcraftOres.AMBER.get()), 0, 0, 0, 0);
            harvest(test, player, pos, ThaumcraftOres.AMBER.get(), fortune, dropItem(ThaumcraftOres.AMBER.get()), 1, 4, 1, 4);
            harvest(test, player, pos, ThaumcraftOres.INFUSED.get("aer").get(), fortune,
                    ThaumcraftContent.SHARDS.get("aer").get(), 1, 5, 0, 3);
            smelting(test, level, ThaumcraftOres.CINNABAR.get(), ThaumcraftContent.MATERIALS.get("quick_silver").get());
            smelting(test, level, ThaumcraftOres.AMBER.get(), ThaumcraftContent.MATERIALS.get("amber_gem").get());
            var placed = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getOrThrow(ThaumcraftWorldgen.ORES_KEY);
            var configured = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(ThaumcraftWorldgen.ORES_KEY.location());
            test.assertTrue(configured != null && configured.feature() == ThaumcraftWorldgen.ORES.get()
                    && placed.feature().value() == configured, "configured and placed tc4_ores resolve to the registered shared feature");
            test.assertTrue(placed.placement().size() == 1 && placed.placement().getFirst() instanceof BiomeFilter,
                    "one complete ore pass per chunk, without count/in_square/height multipliers");
            for (var dimension : List.of(Level.NETHER, Level.END)) {
                var excluded = level.getServer().getLevel(dimension);
                if (excluded != null) test.assertTrue(!ThaumcraftWorldgen.ORES.get().place(NoneFeatureConfiguration.INSTANCE,
                        excluded, excluded.getChunkSource().getGenerator(), excluded.random, pos), "feature itself excludes " + dimension.location());
            }
        } finally {
            level.setBlock(pos, stateBefore, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            var restored = level.getBlockEntity(pos);
            if (restored != null && savedEntity != null) restored.loadWithComponents(savedEntity, level.registryAccess());
            for (int slot = 0; slot < inventoryBefore.size(); slot++) player.getInventory().setItem(slot, inventoryBefore.get(slot));
            player.getInventory().selected = selectedBefore;
            player.setGameMode(modeBefore);
            player.setPos(positionBefore);
            player.getFoodData().setExhaustion(exhaustionBefore);
            level.random.setSeed(resumeSeed);
        }
        test.succeed();
    }

    private static Item dropItem(BlockCustomOre ore) {
        if (ore.originalVariant() == 0) return ore.asItem();
        return ore.originalVariant() == 7 ? ThaumcraftContent.MATERIALS.get("amber_gem").get()
                : ThaumcraftContent.SHARDS.get(ore.aspectTag()).get();
    }

    private static void harvest(GameTestHelper test, ServerPlayer player, BlockPos pos, BlockCustomOre ore,
                                ItemStack tool, Item expected, int minItems, int maxItems, int minXp, int maxXp) {
        ServerLevel level = test.getLevel();
        AABB bounds = new AABB(pos).inflate(3);
        Set<UUID> existing = outputs(level, bounds).stream().map(Entity::getUUID).collect(Collectors.toSet());
        try {
            level.setBlock(pos, ore.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            player.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
            test.assertTrue(player.gameMode.destroyBlock(pos) && level.getBlockState(pos).isAir(), "actual survival mining removes " + ore);
            int items = 0;
            int experience = 0;
            for (Entity entity : outputs(level, bounds)) {
                if (existing.contains(entity.getUUID())) continue;
                if (entity instanceof ItemEntity drop) {
                    test.assertTrue(drop.getItem().is(expected), "no extra or duplicate ore/shard drop in native harvest: " + ore);
                    items += drop.getItem().getCount();
                } else if (entity instanceof ExperienceOrb orb) {
                    CompoundTag tag = new CompoundTag();
                    orb.addAdditionalSaveData(tag);
                    experience += orb.getValue() * tag.getInt("Count");
                }
            }
            test.assertTrue(items >= minItems && items <= maxItems && experience >= minXp && experience <= maxXp,
                    "native harvest item/XP bounds (including silk and insufficient tier): " + ore + ", items=" + items + ", XP=" + experience);
        } finally {
            for (Entity entity : outputs(level, bounds)) if (!existing.contains(entity.getUUID())) entity.discard();
        }
    }

    private static List<Entity> outputs(ServerLevel level, AABB bounds) {
        return level.getEntitiesOfClass(Entity.class, bounds, entity -> entity instanceof ItemEntity || entity instanceof ExperienceOrb);
    }

    private static void smelting(GameTestHelper test, ServerLevel level, BlockCustomOre ore, Item output) {
        var input = new SingleRecipeInput(new ItemStack(ore));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).orElseThrow().value();
        var result = recipe.assemble(input, level.registryAccess());
        test.assertTrue(result.is(output) && result.getCount() == 1 && recipe.getExperience() == 1.0F && recipe.getCookingTime() == 200,
                "source smelting output1/XP1.0 and vanilla1.7 furnace200 ticks: " + ore);
    }
}
