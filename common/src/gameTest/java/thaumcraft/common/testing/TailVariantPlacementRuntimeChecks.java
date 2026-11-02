package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.blocks.BlockWandPedestal;
import thaumcraft.common.blocks.ItemVariantBlock;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/** Actual native game-mode item placement through a real entity, with vanilla and clear controls. */
public final class TailVariantPlacementRuntimeChecks {
    private TailVariantPlacementRuntimeChecks() {}

    public static void variantAndFocusRespectNativeEntityCollision(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var failures = new ArrayList<String>();
            var focus = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand_pedestal_focus"));
            for (Item item : List.of(Blocks.STONE.asItem(), ThaumcraftEldritchBlocks.PAVING_STONE_TRAVEL_ITEM.get(),
                    ThaumcraftEldritchBlocks.PAVING_STONE_WARDING_ITEM.get(), ThaumcraftEldritchBlocks.WARDED_GLASS_ITEM.get(),
                    ThaumcraftEldritchBlocks.OBSIDIAN_TILE_ITEM.get(), focus)) {
                f.reset(); f.put(f.base.below(), Blocks.STONE);
                var source = new ItemStack(item, 2);
                f.player.setItemInHand(InteractionHand.MAIN_HAND, source);
                var pig = EntityType.PIG.create(f.level);
                test.assertTrue(pig != null, "real obstruction exists");
                try {
                    pig.setPos(f.base.getX() + .5, f.base.getY(), f.base.getZ() + .5);
                    pig.setNoAi(true); pig.setNoGravity(true);
                    test.assertTrue(f.level.addFreshEntity(pig), "native obstruction is admitted");
                    var result = f.use(f.base.below(), Direction.UP, InteractionHand.MAIN_HAND);
                    if (!f.level.getBlockState(f.base).isAir() || source.getCount() != 2 || result.consumesAction()) {
                        failures.add(BuiltInRegistries.ITEM.getKey(item) + " occupied placement=" + f.level.getBlockState(f.base)
                                + ", count=" + source.getCount() + ", result=" + result);
                    }
                } finally { pig.discard(); }
                f.reset(); f.put(f.base.below(), Blocks.STONE);
                var clear = new ItemStack(item, 2); f.player.setItemInHand(InteractionHand.MAIN_HAND, clear);
                var result = f.use(f.base.below(), Direction.UP, InteractionHand.MAIN_HAND);
                var placed = f.level.getBlockState(f.base);
                test.assertTrue(result.consumesAction() && clear.getCount() == 1 && !placed.isAir(),
                        "unobstructed native placement remains successful: " + BuiltInRegistries.ITEM.getKey(item));
                if (item instanceof ItemVariantBlock variant) test.assertTrue(placed.equals(variant.variantState()),
                        "clear placement preserves the dedicated original metadata variant");
                if (item == focus) test.assertTrue(placed.getValue(BlockWandPedestal.FOCUS) && f.level.getBlockEntity(f.base) == null,
                        "clear compound focus placement uses focus state without inventing a pedestal inventory");
            }
            test.assertTrue(failures.isEmpty(), "registered item overrides preserve vanilla occupied-target refusal: " + failures);
        }
        test.succeed();
    }
}
