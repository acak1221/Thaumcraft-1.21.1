package thaumcraft.common.items.wands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.entities.EntitySpecialItem;

/** Original bookshelf trigger only; other TC4 wand structures remain pending. */
public final class WandManager {
    private WandManager() {}

    public static boolean canCreateThaumonomicon(UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        return player != null && !player.isSpectator()
                && context.getItemInHand().is(ThaumcraftContent.WAND.get())
                && context.getLevel().getBlockState(pos).is(Blocks.BOOKSHELF)
                && context.getLevel().mayInteract(player, pos)
                && player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand());
    }

    public static boolean createThaumonomicon(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || !canCreateThaumonomicon(context)) return false;
        BlockPos pos = context.getClickedPos();
        EntitySpecialItem result = ThaumcraftContent.SPECIAL_ITEM.get().create(level);
        if (result == null) return false;
        result.setPos(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5);
        result.setItem(new ItemStack(ThaumcraftContent.THAUMONOMICON.get()));
        result.setDeltaMovement(Vec3.ZERO);
        // Original trigger consumes one bookshelf, with no vis or wand cost.
        if (!level.removeBlock(pos, false)) return false;
        if (!level.addFreshEntity(result)) {
            level.setBlockAndUpdate(pos, Blocks.BOOKSHELF.defaultBlockState());
            return false;
        }
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, pos, SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand")), SoundSource.PLAYERS, 1, 1);
        return true;
    }
}
