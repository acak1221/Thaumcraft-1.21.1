package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.blocks.BlockLoot;
import thaumcraft.common.blocks.BlockLootItem;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/** Real registered items and survival placement, with declared creative pick-block inputs. */
public final class LootContainerRarityRuntimeChecks {
    private LootContainerRarityRuntimeChecks() {}

    public static void pickPlaceAndTooltip(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            for (var block : List.of(ThaumcraftEldritchBlocks.LOOT_URN.get(), ThaumcraftEldritchBlocks.LOOT_CRATE.get())) {
                var variants = BlockLoot.creativeVariants(block.asItem());
                f.check(variants.size() == 3, "each loot container exposes exactly three original metadata variants");
                for (int tier = 0; tier <= 2; tier++) {
                    var state = block.defaultBlockState().setValue(BlockLoot.RARITY, tier);
                    f.level.setBlock(f.base, state, 3);
                    ItemStack picked = block.getCloneItemStack(f.level, f.base, state);
                    Rarity expectedRarity = new Rarity[]{Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE}[tier];
                    f.check(picked.is(block.asItem()) && BlockLootItem.rarity(picked) == tier
                            && picked.getRarity() == expectedRarity, "pick-block retains container metadata and native item name rarity");
                    f.check(ItemStack.isSameItemSameComponents(picked, variants.get(tier)),
                            "creative and pick-block variants share canonical stack components");
                    List<Component> tooltip = new ArrayList<>();
                    picked.getItem().appendHoverText(picked, Item.TooltipContext.of(f.level), tooltip, TooltipFlag.NORMAL);
                    String expectedLabel = new String[]{"Common", "Uncommon", "Rare"}[tier];
                    f.check(tooltip.stream().anyMatch(line -> line.getString().equals(expectedLabel)),
                            "original rarity tooltip renders a label, including common, instead of an untranslated key");
                    picked.set(DataComponents.CUSTOM_NAME, Component.literal("preserved caller data"));
                    BlockLootItem.setRarity(picked, tier);
                    f.check(picked.getHoverName().getString().equals("preserved caller data"), "rarity update retains unrelated components");
                    var saved = picked.save(f.level.registryAccess());
                    picked = ItemStack.parseOptional(f.level.registryAccess(), (net.minecraft.nbt.CompoundTag) saved);
                    f.check(BlockLootItem.rarity(picked) == tier && picked.getRarity() == expectedRarity,
                            "native serialization preserves item tier and name rarity");
                    f.put(f.base, Blocks.AIR);
                    f.put(f.base.below(), Blocks.STONE);
                    f.player.setItemInHand(InteractionHand.MAIN_HAND, picked);
                    f.aim(net.minecraft.world.phys.Vec3.atCenterOf(f.base));
                    f.check(f.use(f.base.below(), Direction.UP, InteractionHand.MAIN_HAND).consumesAction(),
                            "native survival use places the picked container");
                    f.check(f.level.getBlockState(f.base).is(block)
                            && f.level.getBlockState(f.base).getValue(BlockLoot.RARITY) == tier
                            && f.player.getMainHandItem().isEmpty(), "placement restores exactly the picked loot tier and consumes one item");
                }
            }
        }
        test.succeed();
    }
}
