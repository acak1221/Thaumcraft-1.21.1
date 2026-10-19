package thaumcraft.common.blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import thaumcraft.common.config.ThaumcraftContent;

/** Eight flattened TC4 ore variants, not the separately registered crystal clusters. */
public final class BlockCustomOre extends Block {
    public static final MapCodec<BlockCustomOre> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 7).fieldOf("original_variant").forGetter(BlockCustomOre::originalVariant),
            propertiesCodec()).apply(instance, BlockCustomOre::new));
    private static final String[] ASPECT_TAGS = {null, "aer", "ignis", "aqua", "terra", "ordo", "perditio", null};
    // These are BlockCustomOreItem.colors, deliberately NOT the aspect icon colours.
    private static final int[] TINTS = {0xffffff, 0xffff7e, 0xff3c01, 0x0090ff, 0x00a000, 0xeeccff, 0x555577, 0xffffff};
    private final int originalVariant;
    // Original BlockCustomOre:55,147-154 uses a private Random for XP, not world RNG.
    private final Random experienceRandom = new Random();

    public BlockCustomOre(int originalVariant, Properties properties) {
        super(properties);
        if (originalVariant < 0 || originalVariant > 7) throw new IllegalArgumentException("Unknown TC4 ore variant");
        this.originalVariant = originalVariant;
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    public int originalVariant() { return originalVariant; }
    public String aspectTag() { return ASPECT_TAGS[originalVariant]; }
    /**
     * Apply only to model tintindex 0; the stone base is untinted. The two
     * full-cube model layers use the native grass-style same-face design. The
     * original base/overlay alpha masks are disjoint and fully complementary
     * in all 16 animation frames, so cutout has neither holes nor z-fighting.
     */
    public int tintColor() { return TINTS[originalVariant]; }

    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (aspectTag() == null) return drops;
        // The loot function supplies the original world-random count. Keep TC4's
        // q separate one-shard stacks (BlockCustomOre:139-142), not one q-stack.
        var shard = ThaumcraftContent.SHARDS.get(aspectTag()).get();
        List<ItemStack> separate = new ArrayList<>();
        for (ItemStack stack : drops) {
            if (stack.is(shard)) {
                for (int i = 0; i < stack.getCount(); i++) separate.add(stack.copyWithCount(1));
            } else {
                separate.add(stack);
            }
        }
        return separate;
    }

    @Override protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (dropExperience) {
            int experience = EnchantmentHelper.processBlockExperience(level, tool,
                    getExpDrop(state, level, pos, null, null, tool));
            if (experience > 0) popExperience(level, pos, experience);
        }
    }

    /**
     * Also the native NeoForge IBlockExtension hook, intentionally without
     * @Override so common compiles against vanilla. NeoForge calls this through
     * BlockDropsEvent, applies enchantments, then spawnAfterBreak(..., false).
     * Fabric uses spawnAfterBreak(..., true). Neither path awards XP twice.
     */
    public int getExpDrop(BlockState state, LevelAccessor level, BlockPos pos, BlockEntity blockEntity,
                          Entity breaker, ItemStack tool) {
        if (originalVariant == 0 || EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), tool) > 0) {
            return 0;
        }
        return (originalVariant == 7 ? 1 : 0) + experienceRandom.nextInt(4);
    }
}
