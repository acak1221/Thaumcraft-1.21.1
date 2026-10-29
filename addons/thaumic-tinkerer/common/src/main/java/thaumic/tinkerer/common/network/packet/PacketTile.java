/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [9 Sep 2013, 17:00:10 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.network.packet;

import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * What the 1.7.10 {@code PacketTile} base class did for the packets that change a block entity from a screen: find the
 * block entity at the position the packet names. 1.21.1 payloads are records and cannot share a base class, so the lookup
 * is this helper.
 *
 * <p>The original trusted any position in any dimension. Here the block entity is found in the sender's own level, must
 * be loaded, and the sender must have the menu of exactly that block entity open; a client that did not open the screen
 * cannot change a device it is not using.</p>
 */
public final class PacketTile {
    private PacketTile() {}

    /**
     * The block entity at {@code pos} for a sender that has a menu of type {@code menuType} open on it, or null.
     *
     * @param tileOfMenu the block entity a menu of that type edits
     */
    @Nullable
    public static <T extends BlockEntity, M extends AbstractContainerMenu> T resolve(ServerPlayer sender, BlockPos pos,
            Class<T> tileType, Class<M> menuType, Function<M, ? extends BlockEntity> tileOfMenu) {
        ServerLevel level = sender.serverLevel();
        if (!level.isLoaded(pos)) return null;
        BlockEntity entity = level.getBlockEntity(pos);
        if (!tileType.isInstance(entity)) return null;
        AbstractContainerMenu open = sender.containerMenu;
        if (!menuType.isInstance(open) || tileOfMenu.apply(menuType.cast(open)) != entity) return null;
        return tileType.cast(entity);
    }
}
