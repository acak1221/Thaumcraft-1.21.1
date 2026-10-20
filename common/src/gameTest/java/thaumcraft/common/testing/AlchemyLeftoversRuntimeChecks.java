package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.crafting.CrucibleCrafting;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.crafting.CrucibleRecipeInput;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileCrucible;

/**
 * The ten alchemy recipes that sat unmade in the book (ConfigRecipes:101-111, AltGunpowder .. AltBonemeal) and the shapeless arcane
 * Mirrored Glass (ConfigRecipes:196), checked on the real recipe manager, the real thrower-knowledge selection and a real heated
 * crucible tile. Recipes, research and aspect pools are never supplied by the test. NOT a client check (the book pages are unseen).
 */
public final class AlchemyLeftoversRuntimeChecks {
    private AlchemyLeftoversRuntimeChecks() {}

    private record Alt(String file, String research, int order, String catalyst, String result, int count, Map<String, Integer> aspects) {}

    private static final List<Alt> ALTS = List.of(
            new Alt("alt_gunpowder", "ALCHEMICALDUPLICATION", 12, "minecraft:gunpowder", "minecraft:gunpowder", 2, Map.of("ignis", 4, "perditio", 4)),
            new Alt("alt_slime", "ALCHEMICALDUPLICATION", 13, "minecraft:slime_ball", "minecraft:slime_ball", 2, Map.of("aqua", 2, "victus", 2)),
            new Alt("alt_clay", "ALCHEMICALDUPLICATION", 14, "minecraft:clay_ball", "minecraft:clay_ball", 2, Map.of("aqua", 1, "terra", 2)),
            new Alt("alt_glowstone", "ALCHEMICALDUPLICATION", 15, "minecraft:glowstone_dust", "minecraft:glowstone_dust", 2, Map.of("lux", 3, "sensus", 1)),
            new Alt("alt_ink", "ALCHEMICALDUPLICATION", 16, "minecraft:ink_sac", "minecraft:ink_sac", 2, Map.of("aqua", 2, "sensus", 2)),
            new Alt("alt_web", "ALCHEMICALMANUFACTURE", 17, "minecraft:string", "minecraft:cobweb", 1, Map.of("vinculum", 2, "pannus", 2)),
            new Alt("alt_mossy_cobble", "ALCHEMICALMANUFACTURE", 18, "minecraft:cobblestone", "minecraft:mossy_cobblestone", 1, Map.of("herba", 2, "praecantatio", 1)),
            new Alt("alt_ice", "ALCHEMICALMANUFACTURE", 19, "minecraft:snow_block", "minecraft:ice", 1, Map.of("ordo", 1, "gelum", 1)),
            new Alt("alt_cracked_brick", "ENTROPICPROCESSING", 20, "minecraft:stone_bricks", "minecraft:cracked_stone_bricks", 1, Map.of("perditio", 2)),
            new Alt("alt_bonemeal", "ENTROPICPROCESSING", 21, "minecraft:bone", "minecraft:bone_meal", 4, Map.of("perditio", 1)));

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    private static AspectList list(Map<String, Integer> amounts) {
        AspectList list = new AspectList();
        amounts.forEach((tag, amount) -> list.add(Aspect.getAspect(tag), amount));
        return list;
    }

    private static CrucibleRecipe recipe(ServerLevel level, Alt alt) {
        return (CrucibleRecipe) level.getRecipeManager().byKey(id("crucible/" + alt.file())).orElseThrow(() -> new AssertionError("no recipe " + alt.file())).value();
    }

    public static void run(GameTestHelper test, ServerPlayer player) {
        ServerLevel level = test.getLevel();
        var knowledge = KnowledgeAccess.get(player).copy();
        try {
            data(test, level);
            selection(test, level, player);
            brew(test, level, player);
            mirror(test, level);
        } finally {
            KnowledgeAccess.get(player).copyFrom(knowledge);
        }
        test.succeed();
    }

    private static void data(GameTestHelper test, ServerLevel level) {
        for (Alt alt : ALTS) {
            CrucibleRecipe recipe = recipe(level, alt);
            ItemStack result = recipe.getResultItem(level.registryAccess());
            test.assertTrue(recipe.research().equals(alt.research()) && recipe.sourceOrder() == alt.order() && recipe.aspects().equals(alt.aspects())
                            && result.is(item(alt.result())) && result.getCount() == alt.count(),
                    alt.file() + ": research " + alt.research() + ", registration order " + alt.order() + ", " + alt.aspects() + " -> " + alt.count() + " x " + alt.result() + ", found " + recipe.research() + "/" + recipe.sourceOrder() + "/" + recipe.aspects() + "/" + result);
            AspectList full = list(alt.aspects());
            test.assertTrue(recipe.matches(new CrucibleRecipeInput(new ItemStack(item(alt.catalyst())), full), level), alt.file() + " takes its own catalyst with the full brew");
            test.assertTrue(!recipe.matches(new CrucibleRecipeInput(new ItemStack(Items.DIRT), full), level), alt.file() + " refuses a stranger as catalyst");
            for (var aspect : alt.aspects().entrySet()) {
                AspectList short1 = list(alt.aspects()).remove(Aspect.getAspect(aspect.getKey()), 1);
                test.assertTrue(!recipe.matches(new CrucibleRecipeInput(new ItemStack(item(alt.catalyst())), short1), level),
                        alt.file() + " does not brew with one " + aspect.getKey() + " missing");
            }
        }
        test.assertTrue(ALTS.stream().map(Alt::order).distinct().count() == ALTS.size(), "twelve to twenty-one: every registration slot is used once");
    }

    private static ItemEntity drop(ServerLevel level, BlockPos pos, Item item, int count, ServerPlayer thrower) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + .5, pos.getY() + .4, pos.getZ() + .5, new ItemStack(item, count));
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setThrower(thrower);
        if (!level.addFreshEntity(entity)) throw new IllegalStateException("Alchemy fixture item spawn was rejected");
        return entity;
    }

    private static void selection(GameTestHelper test, ServerLevel level, ServerPlayer player) {
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        AABB box = new AABB(pos).inflate(3);
        Set<UUID> before = level.getEntitiesOfClass(ItemEntity.class, box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        try {
            for (Alt alt : ALTS) {
                KnowledgeAccess.get(player).reset();
                ItemEntity thrown = drop(level, pos, item(alt.catalyst()), 1, player);
                var holder = CrucibleCrafting.find(level, thrown, list(alt.aspects()));
                test.assertTrue(holder == null, alt.file() + ": a thrower who has not done " + alt.research() + " crafts nothing");
                KnowledgeAccess.get(player).addResearch(alt.research());
                holder = CrucibleCrafting.find(level, thrown, list(alt.aspects()));
                test.assertTrue(holder != null && holder.id().equals(id("crucible/" + alt.file())),
                        alt.file() + ": the researched thrower gets exactly this recipe, found " + (holder == null ? "nothing" : holder.id()));
                AspectList thin = list(alt.aspects());
                Map.Entry<String, Integer> first = alt.aspects().entrySet().iterator().next();
                thin.remove(Aspect.getAspect(first.getKey()), 1);
                test.assertTrue(CrucibleCrafting.find(level, thrown, thin) == null, alt.file() + ": a brew one short crafts nothing");
                thrown.discard();
            }
        } finally {
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, box)) if (!before.contains(entity.getUUID())) entity.discard();
        }
    }

    private static void brew(GameTestHelper test, ServerLevel level, ServerPlayer player) {
        BlockPos pos = test.absolutePos(new BlockPos(3, 2, 3));
        AABB box = new AABB(pos).inflate(3);
        BlockState saved = level.getBlockState(pos);
        CompoundTag savedTile = level.getBlockEntity(pos) == null ? null : level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
        Set<UUID> before = level.getEntitiesOfClass(ItemEntity.class, box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        try {
            KnowledgeAccess.get(player).reset();
            level.setBlock(pos, ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_ALL);
            for (Alt alt : List.of(ALTS.get(0), ALTS.get(7), ALTS.get(9))) {
                TileCrucible tile = (TileCrucible) level.getBlockEntity(pos);
                fill(level, tile, alt.aspects());
                int pending = TileCrucible.pendingOutputCount(level);
                ItemEntity unpaid = drop(level, pos, item(alt.catalyst()), 1, player);
                tile.attemptSmelt(unpaid);
                test.assertTrue(TileCrucible.pendingOutputCount(level) == pending && tile.getWaterAmount() == 1000,
                        alt.file() + ": without " + alt.research() + " the catalyst only dissolves, no output and no water spent");
                fill(level, tile, alt.aspects());
                KnowledgeAccess.get(player).addResearch(alt.research());
                ItemEntity catalyst = drop(level, pos, item(alt.catalyst()), 1, player);
                tile.attemptSmelt(catalyst);
                test.assertTrue(catalyst.isRemoved() && tile.getWaterAmount() == 950 && tile.tagAmount() == 0
                                && TileCrucible.pendingOutputCount(level) == pending + 1,
                        alt.file() + ": one catalyst, its aspects and 50 water are paid exactly once; water " + tile.getWaterAmount() + ", aspects " + tile.tagAmount());
                TileCrucible.tickOutputs(level);
                var outputs = level.getEntitiesOfClass(EntitySpecialItem.class, box).stream()
                        .filter(entity -> !before.contains(entity.getUUID()) && entity.getItem().is(item(alt.result()))).toList();
                test.assertTrue(outputs.size() == 1 && outputs.getFirst().getItem().getCount() == alt.count(),
                        alt.file() + ": the crucible brings out " + alt.count() + " x " + alt.result() + ", found " + outputs.stream().map(e -> e.getItem().toString()).toList());
                for (var output : outputs) output.discard();
                for (var entity : level.getEntitiesOfClass(ItemEntity.class, box)) if (!before.contains(entity.getUUID())) entity.discard();
                KnowledgeAccess.get(player).reset();
            }
        } finally {
            if (level.getBlockEntity(pos) instanceof TileCrucible tile) tile.spillRemnants();
            TileCrucible.tickOutputs(level);
            level.setBlock(pos, saved, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (savedTile != null && level.getBlockEntity(pos) != null) level.getBlockEntity(pos).loadWithComponents(savedTile, level.registryAccess());
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, box)) if (!before.contains(entity.getUUID())) entity.discard();
        }
    }

    private static void fill(ServerLevel level, TileCrucible tile, Map<String, Integer> aspects) {
        CompoundTag tag = tile.saveWithoutMetadata(level.registryAccess());
        tag.putShort("Heat", (short) 200);
        tag.putInt("Water", 1000);
        tag.remove("Aspects");
        list(aspects).writeToNBT(tag);
        tile.loadWithComponents(tag, level.registryAccess());
    }

    private static ArcaneRecipeInput slots(int quicksilver, int pane) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) items.add(ItemStack.EMPTY);
        items.set(quicksilver, new ItemStack(item("thaumcraft:quick_silver")));
        items.set(pane, new ItemStack(Items.GLASS_PANE));
        return new ArcaneRecipeInput(items);
    }

    private static void mirror(GameTestHelper test, ServerLevel level) {
        var holder = level.getRecipeManager().byKey(id("arcane/mirrored_glass")).orElseThrow(() -> new AssertionError("no mirrored glass recipe"));
        test.assertTrue(holder.value() instanceof ArcaneShapelessRecipe, "MirrorGlass is genuinely shapeless");
        var recipe = (ArcaneShapelessRecipe) holder.value();
        test.assertTrue(recipe.research().equals("BASICARTIFACE") && recipe.vis().equals(Map.of("ignis", 10, "terra", 10)) && recipe.getIngredients().size() == 2,
                "BASICARTIFACE, ignis 10 + terra 10, two ingredients: " + recipe.research() + recipe.vis());
        ItemStack result = recipe.getResultItem(level.registryAccess());
        test.assertTrue(result.is(item("thaumcraft:mirrored_glass")) && result.getCount() == 1, "one mirrored glass: " + result);
        for (int quicksilver = 0; quicksilver < 9; quicksilver++) {
            for (int pane = 0; pane < 9; pane++) {
                if (quicksilver == pane) continue;
                test.assertTrue(recipe.matches(slots(quicksilver, pane), level), "quicksilver and a glass pane in any two slots: " + quicksilver + "/" + pane);
            }
        }
        List<ItemStack> glassBlock = new ArrayList<>(slots(1, 7).items());
        glassBlock.set(7, new ItemStack(Items.GLASS));
        test.assertTrue(!recipe.matches(new ArcaneRecipeInput(glassBlock), level), "a glass block is not the glass pane");
        List<ItemStack> extra = new ArrayList<>(slots(1, 7).items());
        extra.set(4, new ItemStack(Items.STICK));
        test.assertTrue(!recipe.matches(new ArcaneRecipeInput(extra), level), "an extra item spoils it");
        List<ItemStack> twoPanes = new ArrayList<>(slots(1, 7).items());
        twoPanes.set(1, new ItemStack(Items.GLASS_PANE));
        test.assertTrue(!recipe.matches(new ArcaneRecipeInput(twoPanes), level), "two panes and no quicksilver is not it");
        test.assertTrue(recipe.assemble(slots(0, 8), level.registryAccess()).is(item("thaumcraft:mirrored_glass")), "assembled output is the mirrored glass");
    }
}
