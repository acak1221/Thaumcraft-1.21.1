/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami.tool;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** The awakened Ichorium tools: {@link #getType()} is {@code pick}, {@code shovel}, {@code axe} or {@code sword}. */
public interface IAdvancedTool {

    String getType();

    /**
     * 1.7.10 {@code Item.onBlockStartBreak}: called on the server before a player's block break, with the block still in
     * place. The port calls it from the break event ({@link ToolHandler#onBlockBreak}); the swords have nothing to do here.
     */
    default void onBlockStartBreak(ItemStack stack, ServerLevel level, BlockPos pos, BlockState state,
            ServerPlayer player) {}
}
