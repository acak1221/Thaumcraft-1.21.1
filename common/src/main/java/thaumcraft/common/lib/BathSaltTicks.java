package thaumcraft.common.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.items.ItemBathSalts;

/** Native replacement for Item.getEntityLifespan + original ItemExpireEvent handler. */
public final class BathSaltTicks {
    private BathSaltTicks() {}
    public static void afterNativeTick(ItemEntity item) {
        if(!(item.level() instanceof ServerLevel level)||!level.getServer().isSameThread()
                ||level.getServer().getLevel(level.dimension())!=level||item.isRemoved()
                ||item.getAge()<SpaRules.BATH_SALT_LIFETIME||item.getItem().isEmpty()
                ||!(item.getItem().getItem() instanceof ItemBathSalts)||level.getEntity(item.getUUID())!=item)return;
        BlockPos pos=item.blockPosition();
        if(level.hasChunkAt(pos)) {
            var state=level.getBlockState(pos);
            if(state.is(Blocks.WATER)&&state.getValue(LiquidBlock.LEVEL)==0) {
                // Exactly one source for the entire expiring stack, including stack64.
                // Retire ownership before native neighbor callbacks can reenter this intent.
                item.discard();
                if(level.getServer().getLevel(level.dimension())==level&&level.hasChunkAt(pos)&&level.getBlockState(pos)==state)
                    level.setBlockAndUpdate(pos,ThaumcraftSpa.PURE_WATER.get().defaultBlockState());
                return;
            }
        }
        item.discard();
    }
}
