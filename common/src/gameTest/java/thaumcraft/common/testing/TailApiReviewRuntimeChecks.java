package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionEnchantmentRecipe;
import thaumcraft.api.crafting.NativeRecipes;
import thaumcraft.api.damagesource.DamageSourceIndirectThaumcraftEntity;
import thaumcraft.common.blocks.BlockCosmeticStairs;
import thaumcraft.common.config.ThaumcraftOuterLands;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.lib.world.DungeonRooms;

/** Registry-backed facade, damage attribution and actual world-generation state adapters. */
public final class TailApiReviewRuntimeChecks {
    private TailApiReviewRuntimeChecks() {}

    public static void emptyEnchantmentTagKeepsPublicNativeView(GameTestHelper test) {
        var level = test.getLevel();
        var fixtureId = ResourceLocation.fromNamespaceAndPath("thaumcraft_review", "tail_api_empty_tag_enchantment");
        var fixture = level.getRecipeManager().byKey(fixtureId).orElseThrow().value();
        test.assertTrue(fixture instanceof thaumcraft.common.crafting.InfusionEnchantmentRecipe,
                "the optional component tag passed the actual enchantment infusion datapack loader");
        var nativeRecipe = (thaumcraft.common.crafting.InfusionEnchantmentRecipe) fixture;
        test.assertTrue(nativeRecipe.components().size() == 1 && !nativeRecipe.components().getFirst().isEmpty()
                        && nativeRecipe.components().getFirst().getItems().length == 0,
                "a declared currently empty optional tag retains its native predicate");

        Object converted = NativeRecipes.view(nativeRecipe);
        test.assertTrue(converted instanceof InfusionEnchantmentRecipe,
                "an unavailable optional ingredient still has a public enchantment infusion view");
        var view = (InfusionEnchantmentRecipe) converted;
        test.assertTrue(view.nativeRecipe() == nativeRecipe && view.components.length == 1 && view.components[0].isEmpty(),
                "the empty display sample does not replace or discard the real native recipe");
        var central = new ItemStack(Items.IRON_SWORD);
        test.assertTrue(!view.matches(new ArrayList<>(List.of(new ItemStack(Items.STICK))), central, level, null)
                        && !view.matches(new ArrayList<>(List.of(ItemStack.EMPTY)), central, level, null),
                "neither ordinary stock nor a missing component can craft the disabled recipe");

        long count = ThaumcraftApi.getCraftingRecipes().stream()
                .filter(value -> value instanceof InfusionEnchantmentRecipe recipe && recipe.nativeRecipe() == nativeRecipe).count();
        test.assertTrue(count == 1, "global public recipe enumeration retains exactly one view of the actual loaded fixture");
        var ordinaryNative = new thaumcraft.common.crafting.InfusionEnchantmentRecipe("", nativeRecipe.enchantment(),
                List.of(Ingredient.of(Items.STICK)), Map.of("aer", 1), 0, 1);
        var ordinary = InfusionEnchantmentRecipe.of(ordinaryNative);
        test.assertTrue(ordinary != null && ordinary.nativeRecipe() == ordinaryNative
                        && ordinary.matches(new ArrayList<>(List.of(new ItemStack(Items.STICK))), central, level, null),
                "the same registered enchantment keeps an ordinary enabled recipe craftable");

        boolean strictRejected = false;
        try {
            new InfusionEnchantmentRecipe("", level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.UNBREAKING), 0, new AspectList().add(Aspect.AIR, 1),
                    new ItemStack[]{ItemStack.EMPTY});
        } catch (IllegalArgumentException expected) {
            strictRejected = true;
        }
        test.assertTrue(strictRejected, "programmatic recipes must still reject an empty physical component");
        test.succeed();
    }

    public static void indirectDamageFactoriesPreserveDirectAndCausingEntities(GameTestHelper test) {
        var level = test.getLevel();
        var owner = EntityType.ZOMBIE.create(level);
        var direct = EntityType.ARROW.create(level);
        var victim = EntityType.PIG.create(level);
        test.assertTrue(owner != null && direct != null && victim != null, "the fixture uses three actual native entities");
        try {
            Vec3 origin = Vec3.atCenterOf(test.absolutePos(new BlockPos(1, 2, 1)));
            owner.setPos(origin);
            direct.setPos(origin.add(3, 1, 0));
            direct.setOwner(owner);
            victim.setPos(origin.add(4, 1, 0));
            var registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            var projectileType = registry.getHolderOrThrow(DamageTypes.ARROW);
            var fireType = registry.getHolderOrThrow(DamageTypes.ON_FIRE);
            var unblockableKey = ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "indirect_magic"));
            var unblockableType = registry.getHolderOrThrow(unblockableKey);
            test.assertTrue(fireType.is(DamageTypeTags.IS_FIRE)
                            && unblockableType.is(DamageTypeTags.BYPASSES_ARMOR) && unblockableType.value().exhaustion() == 0,
                    "native tagged damage types satisfy both modifier-factory contracts");

            Map<String, DamageSource> sources = new LinkedHashMap<>();
            sources.put("holder", DamageSourceIndirectThaumcraftEntity.create(direct, owner, projectileType));
            sources.put("registry key", DamageSourceIndirectThaumcraftEntity.create(direct, owner, DamageTypes.ARROW, level.registryAccess()));
            sources.put("fire modifier", DamageSourceIndirectThaumcraftEntity.setFireDamage(direct, owner, fireType));
            sources.put("unblockable modifier", DamageSourceIndirectThaumcraftEntity.setUnblockable(direct, owner, unblockableType));
            for (var entry : sources.entrySet()) {
                var source = entry.getValue();
                test.assertTrue(source.getDirectEntity() == direct && source.getEntity() == owner,
                        entry.getKey() + " must report the real arrow as direct source and the shooter as causing attacker");
                test.assertTrue(direct.position().equals(source.getSourcePosition()),
                        entry.getKey() + " uses the projectile's position for native knockback and shield direction");
            }
            var unowned = DamageSourceIndirectThaumcraftEntity.create(direct, null, projectileType);
            test.assertTrue(unowned.getDirectEntity() == direct && unowned.getEntity() == null,
                    "an ownerless projectile remains direct damage without inventing an attacker");
            test.assertTrue(sources.get("fire modifier").is(DamageTypeTags.IS_FIRE)
                            && sources.get("unblockable modifier").is(DamageTypeTags.BYPASSES_ARMOR)
                            && sources.get("unblockable modifier").getFoodExhaustion() == 0,
                    "fixing attribution retains fire, armor bypass and zero exhaustion modifiers");
            test.assertTrue(victim.hurt(sources.get("holder"), 1) && victim.getLastHurtByMob() == owner,
                    "the actual native hurt path attributes the indirect hit to its living shooter");
        } finally {
            if (direct != null) direct.discard();
            if (owner != null) owner.discard();
            if (victim != null) victim.discard();
        }
        test.succeed();
    }

    public static void outerLandsStairAdapterRetainsAllMetadataDirections(GameTestHelper test) {
        var level = test.getLevel();
        Block registered = ThaumcraftOuterLands.stairsEldritch();
        test.assertTrue(registered instanceof BlockCosmeticStairs,
                "the actual Outer Lands registry resolves its native eldritch stairs");
        BlockPos pos = test.absolutePos(new BlockPos(1, 2, 1));
        BlockState before = level.getBlockState(pos);
        try {
            for (int metadata = 0; metadata < 8; metadata++) {
                Direction expected = switch (metadata & 3) {
                    case 0 -> Direction.EAST;
                    case 1 -> Direction.WEST;
                    case 2 -> Direction.SOUTH;
                    default -> Direction.NORTH;
                };
                Half half = (metadata & 4) == 0 ? Half.BOTTOM : Half.TOP;
                BlockState state = ThaumcraftOuterLands.state(DungeonRooms.Kind.STAIRS, metadata, -1);
                test.assertTrue(state != null && state.is(registered)
                                && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING),
                        "the room adapter returns a real horizontally oriented stair, metadata " + metadata);
                test.assertTrue(state.getValue(BlockStateProperties.HORIZONTAL_FACING) == expected
                                && state.getValue(BlockStateProperties.HALF) == half,
                        "the real room adapter preserves original facing " + expected + " and half " + half + " for metadata " + metadata);
                level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                BlockState placed = level.getBlockState(pos);
                test.assertTrue(placed.is(registered) && placed.getValue(BlockStateProperties.HORIZONTAL_FACING) == expected
                                && placed.getValue(BlockStateProperties.HALF) == half,
                        "native world placement retains the adapted stair facing and half, metadata " + metadata);
            }
        } finally {
            level.setBlock(pos, before, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        test.succeed();
    }
}
