package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockCustomOre;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftOres;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.world.ThaumcraftOreFeature;

/** Actual registered deep variants, native loot/harvest, and fresh negative-height noise terrain. */
public final class DeepslateOreRuntimeChecks {
    private static final Map<String, String> NAMES = Map.of(
            "aer", "air", "ignis", "fire", "aqua", "water", "terra", "earth", "ordo", "order", "perditio", "entropy");

    private DeepslateOreRuntimeChecks() {}

    public static void registeredLootAndHarvest(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3)).atY(160);
        var oldState = level.getBlockState(pos);
        test.assertTrue(level.getBlockEntity(pos) == null, "the owned deep-ore harvest fixture has no foreign block entity");
        ChunkPos chunk = new ChunkPos(pos);
        boolean ownedForce = !level.getForcedChunks().contains(chunk.toLong());
        if (ownedForce) level.setChunkForced(chunk.x, chunk.z, true);
        level.getChunkAt(pos);
        GameTestChunkAwait.entityTicking(test, pos);
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        var previousPosition = player.position();
        var previousMode = player.gameMode.getGameModeForPlayer();
        int previousSelected = player.getInventory().selected;
        float previousExhaustion = player.getFoodData().getExhaustionLevel();
        List<ItemStack> previousInventory = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            previousInventory.add(player.getInventory().getItem(slot).copy());
        long resumeSeed = level.random.nextLong();
        try {
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().selected = 0;
            player.setPos(pos.getX() + .5, pos.getY() + 3, pos.getZ() + .5);
            var enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            ItemStack wood = new ItemStack(Items.WOODEN_PICKAXE);
            ItemStack silk = pick.copy();
            silk.enchant(enchantments.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
            ItemStack fortune = pick.copy();
            fortune.enchant(enchantments.getHolderOrThrow(Enchantments.FORTUNE), 3);
            test.assertTrue(ThaumcraftOres.DEEPSLATE_INFUSED.size() == 6
                            && ThaumcraftOres.DEEPSLATE_INFUSED.keySet().equals(ThaumcraftOres.INFUSED.keySet()),
                    "every existing primal infused stone has one registered deep counterpart");
            for (var entry : ThaumcraftOres.DEEPSLATE_INFUSED.entrySet()) {
                String tag = entry.getKey(), name = NAMES.get(tag);
                BlockCustomOre deep = entry.getValue().get(), stone = ThaumcraftOres.INFUSED.get(tag).get();
                var state = deep.defaultBlockState();
                var id = ResourceLocation.fromNamespaceAndPath("thaumcraft", "deepslate_" + name + "_infused_stone");
                test.assertTrue(BuiltInRegistries.BLOCK.getKey(deep).equals(id)
                                && BuiltInRegistries.ITEM.getKey(deep.asItem()).equals(id)
                                && deep != stone && deep.aspectTag().equals(tag) && deep.originalVariant() == stone.originalVariant(),
                        "distinct registered block/item retain the exact original aspect identity: " + tag);
                test.assertTrue(state.getDestroySpeed(level, pos) == 3.0F && deep.getExplosionResistance() == 6.0F
                                && state.getSoundType() == SoundType.DEEPSLATE,
                        "new host has the native deepslate hardness, resistance and sound: " + tag);
                test.assertTrue(state.requiresCorrectToolForDrops() && state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                                && wood.isCorrectToolForDrops(state) && !new ItemStack(Items.WOODEN_SHOVEL).isCorrectToolForDrops(state),
                        "the deep host keeps infused-stone pickaxe harvesting and does not silently require an iron tier");
                var sharedOre = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/infused_" + name));
                var sharedItem = TagKey.create(Registries.ITEM, sharedOre.location());
                var legacyItem = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("thaumcraft", "original_ore/ore_infused_" + name));
                test.assertTrue(state.is(sharedOre) && new ItemStack(deep).is(sharedItem) && new ItemStack(deep).is(legacyItem),
                        "loaded shared and original ore tags recognize the new variant: " + tag);
                var stoneAspects = ObjectAspectResolver.get(new ItemStack(stone), level);
                var deepAspects = ObjectAspectResolver.get(new ItemStack(deep), level);
                test.assertTrue(stoneAspects.size() > 0 && stoneAspects.aspects.equals(deepAspects.aspects),
                        "scanning and alchemy see the same actual aspect amounts as the matching stone ore: " + tag);
                test.assertTrue(sameNullable(ThaumcraftApi.getSmeltingBonus(new ItemStack(stone)),
                                ThaumcraftApi.getSmeltingBonus(new ItemStack(deep))),
                        "original ore-name smelting bonus lookup retains stone/deep parity: " + tag);
                smeltingParity(test, level, stone, deep);
                var silkDrops = Block.getDrops(state, level, pos, null, player, silk);
                test.assertTrue(silkDrops.size() == 1 && silkDrops.getFirst().is(deep.asItem()) && silkDrops.getFirst().getCount() == 1
                                && deep.getExpDrop(state, level, pos, null, player, silk) == 0,
                        "loaded loot silk-returns exactly the new deep ore, with no shard or XP: " + tag);
                for (ItemStack tool : List.of(pick, fortune)) for (int seed = 1; seed <= 8; seed++) {
                    level.random.setSeed(seed);
                    var original = Block.getDrops(stone.defaultBlockState(), level, pos, null, player, tool);
                    level.random.setSeed(seed);
                    var actual = Block.getDrops(state, level, pos, null, player, tool);
                    test.assertTrue(ItemStack.listMatches(original, actual)
                                    && actual.stream().allMatch(stack -> stack.is(ThaumcraftContent.SHARDS.get(tag).get()) && stack.getCount() == 1),
                            "real loot keeps original Fortune/RNG singleton shard drops: " + tag + ", seed=" + seed);
                }
                level.random.setSeed(707L);
                long expectedDraw = level.random.nextLong();
                level.random.setSeed(707L);
                for (int i = 0; i < 16; i++) {
                    int xp = deep.getExpDrop(state, level, pos, null, player, pick);
                    test.assertTrue(xp >= 0 && xp <= 3, "deep ore keeps original independent XP bounds: " + tag);
                }
                test.assertTrue(level.random.nextLong() == expectedDraw, "XP remains independent of the world RNG");
                harvest(test, player, pos, deep, silk, deep.asItem(), 1, 1, 0);
                harvest(test, player, pos, deep, wood, ThaumcraftContent.SHARDS.get(tag).get(), 1, 2, 3);
                harvest(test, player, pos, deep, fortune, ThaumcraftContent.SHARDS.get(tag).get(), 1, 5, 3);
            }
        } finally {
            level.setBlock(pos, oldState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            for (int slot = 0; slot < previousInventory.size(); slot++) player.getInventory().setItem(slot, previousInventory.get(slot));
            player.getInventory().selected = previousSelected;
            player.getFoodData().setExhaustion(previousExhaustion);
            player.setGameMode(previousMode);
            player.setPos(previousPosition);
            level.random.setSeed(resumeSeed);
            if (ownedForce) level.setChunkForced(chunk.x, chunk.z, false);
        }
        test.succeed();
    }

    public static void naturalDeepGeneration(GameTestHelper test) {
        var key = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "natural"));
        ServerLevel level = test.getLevel().getServer().getLevel(key);
        test.assertTrue(level != null && level.getMinBuildHeight() < 0, "fresh normal-noise test dimension exposes negative build height");
        Map<Block, String> deep = new LinkedHashMap<>();
        ThaumcraftOres.DEEPSLATE_INFUSED.forEach((tag, ore) -> deep.put(ore.get(), tag));
        Map<String, Integer> counts = new LinkedHashMap<>();
        var cursor = new BlockPos.MutableBlockPos();
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        int harvested = 0;
        // Fresh terrain is the input under test. This is the same read-only region as
        // NaturalOreGenerationRuntimeChecks, which scans above zero: full-suite runs
        // need not allocate another terrain region merely to inspect its lower half.
        // Production generation itself never acquires chunk tickets.
        for (int chunkX = 96; chunkX < 100; chunkX++) for (int chunkZ = 96; chunkZ < 100; chunkZ++) {
            var chunk = level.getChunk(chunkX, chunkZ);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                cursor.set(chunkX * 16 + x, level.getMinBuildHeight(), chunkZ * 16 + z);
                test.assertTrue(chunk.getBlockState(cursor).is(Blocks.BEDROCK),
                        "native ore generation preserves every bedrock floor cell");
                for (int y = level.getMinBuildHeight(); y < 0; y++) {
                    cursor.setY(y);
                    var state = chunk.getBlockState(cursor);
                    String tag = deep.get(state.getBlock());
                    if (tag == null) continue;
                    counts.merge(tag, 1, Integer::sum);
                    test.assertTrue(!state.is(ThaumcraftOres.INFUSED.get(tag).get()), "negative depth contains the distinct deepslate identity");
                    if (harvested++ < 6) {
                        var drops = Block.getDrops(state, level, cursor.immutable(), null, null, pick);
                        test.assertTrue(pick.isCorrectToolForDrops(state) && !drops.isEmpty()
                                        && drops.stream().allMatch(stack -> stack.is(ThaumcraftContent.SHARDS.get(tag).get())),
                                "fresh naturally generated deep ore supplies usable matching primal shards");
                    }
                }
            }
        }
        test.assertTrue(!counts.isEmpty() && harvested >= 6, "actual configured biome generation creates a mineable deep crystal population below zero");
        RandomSource disabled = RandomSource.create(419L), untouched = RandomSource.create(419L);
        test.assertTrue(!ThaumcraftOreFeature.placePass(level, disabled, 97, 97, false, false, false)
                        && disabled.nextLong() == untouched.nextLong(),
                "disabling all ore types suppresses the new depth pass as well as the original pass");
        Thaumcraft.LOG.info("Natural deepslate crystal ore population below Y0: {}", counts);
        test.succeed();
    }

    private static void smeltingParity(GameTestHelper test, ServerLevel level, BlockCustomOre stone, BlockCustomOre deep) {
        var ordinaryInput = new SingleRecipeInput(new ItemStack(stone));
        var deepInput = new SingleRecipeInput(new ItemStack(deep));
        var ordinary = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, ordinaryInput, level);
        var actual = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, deepInput, level);
        test.assertTrue(ordinary.isPresent() == actual.isPresent(), "deep host does not add or remove a smelting recipe");
        if (ordinary.isPresent()) {
            var before = ordinary.get().value(); var after = actual.orElseThrow().value();
            test.assertTrue(ItemStack.matches(before.assemble(ordinaryInput, level.registryAccess()), after.assemble(deepInput, level.registryAccess()))
                            && before.getExperience() == after.getExperience() && before.getCookingTime() == after.getCookingTime(),
                    "a loaded smelting recipe keeps the exact stone output, XP and time");
        }
    }

    private static boolean sameNullable(ItemStack first, ItemStack second) {
        return first == null ? second == null : second != null && ItemStack.matches(first, second);
    }

    private static void harvest(GameTestHelper test, ServerPlayer player, BlockPos pos, BlockCustomOre ore,
                                ItemStack tool, Item output, int min, int max, int maxXp) {
        var level = player.serverLevel();
        AABB bounds = new AABB(pos).inflate(3);
        Set<UUID> before = outputs(level, bounds).stream().map(Entity::getUUID).collect(Collectors.toSet());
        try {
            level.setBlock(pos, ore.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            player.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());
            test.assertTrue(player.gameMode.destroyBlock(pos) && level.getBlockState(pos).isAir(), "actual survival mining removes the deep ore");
            int items = 0, xp = 0;
            for (Entity entity : outputs(level, bounds)) {
                if (before.contains(entity.getUUID())) continue;
                if (entity instanceof ItemEntity dropped) {
                    test.assertTrue(dropped.getItem().is(output), "survival harvest emits only the matching deep block/shard");
                    items += dropped.getItem().getCount();
                } else if (entity instanceof ExperienceOrb orb) {
                    CompoundTag tag = new CompoundTag(); orb.addAdditionalSaveData(tag);
                    xp += orb.getValue() * tag.getInt("Count");
                }
            }
            test.assertTrue(items >= min && items <= max && xp >= 0 && xp <= maxXp,
                    "deep native harvest respects singleton shard/Fortune/XP bounds, items=" + items + ", XP=" + xp);
        } finally {
            for (Entity entity : outputs(level, bounds)) if (!before.contains(entity.getUUID())) entity.discard();
        }
    }

    private static List<Entity> outputs(ServerLevel level, AABB bounds) {
        return level.getEntitiesOfClass(Entity.class, bounds, entity -> entity instanceof ItemEntity || entity instanceof ExperienceOrb);
    }
}
