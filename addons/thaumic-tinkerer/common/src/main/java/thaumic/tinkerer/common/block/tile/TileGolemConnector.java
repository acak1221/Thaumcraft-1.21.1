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
package thaumic.tinkerer.common.block.tile;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumic.tinkerer.common.block.camo.CamoDeviceBlockEntity;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.common.core.golems.GolemAccess;
import thaumic.tinkerer.common.core.golems.GolemAccessProvider;
import thaumic.tinkerer.common.core.golems.GolemConnectorMethods;
import thaumic.tinkerer.common.item.ItemConnector;

/**
 * The Golem Connector: bound to one golem by the Transvector Binder (sneak-click the golem), it keeps the golem's id and
 * looks for that golem within thirty blocks every five seconds. In 1.7.10 its only use was to be a ComputerCraft and
 * OpenComputers peripheral that reads and edits the golem ({@link GolemConnectorMethods}).
 *
 * <p>Not available in 1.21.1: the peripheral registration (no computer mod can be reached from loader-neutral code) and
 * the golem data (Thaumcraft's golems have no API, see {@link GolemAccessProvider}). The binding, the saved data and the
 * camouflage work.</p>
 */
public class TileGolemConnector extends CamoDeviceBlockEntity {

    private static final String TAG_UUID_MOST = "UUIDMost";
    private static final String TAG_UUID_LEAST = "UUIDLeast";

    /** How far from the connector the bound golem is searched for. */
    private static final int GOLEM_RANGE = 30;

    @Nullable
    public UUID golemConnected;
    /** The bound golem when it was last found (not saved). */
    @Nullable
    public Entity golem;

    public TileGolemConnector(BlockPos pos, BlockState state) {
        super(TileBlocks.golemConnectorType(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileGolemConnector tile) {
        if (level.getGameTime() % 100 == 1) {
            tile.verifyGolem();
        }
    }

    public void verifyGolem() {
        if (golemConnected == null || level == null) return;
        if (golem != null && golem.isRemoved()) golem = null;
        List<Entity> list = level.getEntitiesOfClass(Entity.class, new AABB(worldPosition.getX() - GOLEM_RANGE,
                worldPosition.getY() - GOLEM_RANGE, worldPosition.getZ() - GOLEM_RANGE, worldPosition.getX() + GOLEM_RANGE,
                worldPosition.getY() + GOLEM_RANGE, worldPosition.getZ() + GOLEM_RANGE), ItemConnector::isGolem);

        for (Entity entity : list) {
            if (entity.getUUID().equals(golemConnected)) {
                this.golem = entity;
                break;
            }
        }
    }

    /** {@code ConnectGolem}: bind to the golem with this id (null unbinds). */
    public void ConnectGolem(@Nullable UUID golemID) {
        golemConnected = golemID;
        if (golemID == null) golem = null;
        else {
            verifyGolem();
        }
        setChanged();
    }

    /** The bound golem as the connector's methods see it. */
    public GolemAccess golemAccess() {
        return GolemAccessProvider.forEntity(golem);
    }

    /** The peripheral surface ({@code tt_golemconnector}) of this connector, ready for a computer-mod bridge. */
    public GolemConnectorMethods peripheral() {
        return new GolemConnectorMethods(golemAccess(), worldPosition,
                level == null ? "" : level.dimension().location().toString());
    }

    @Override
    protected void writeCustomData(CompoundTag cmp, HolderLookup.Provider registries) {
        super.writeCustomData(cmp, registries);
        if (golemConnected != null) {
            cmp.putLong(TAG_UUID_MOST, golemConnected.getMostSignificantBits());
            cmp.putLong(TAG_UUID_LEAST, golemConnected.getLeastSignificantBits());
        }
    }

    @Override
    protected void readCustomData(CompoundTag cmp, HolderLookup.Provider registries) {
        super.readCustomData(cmp, registries);
        if (cmp.contains(TAG_UUID_LEAST) && cmp.contains(TAG_UUID_MOST)) {
            golemConnected = new UUID(cmp.getLong(TAG_UUID_MOST), cmp.getLong(TAG_UUID_LEAST));
        } else {
            golemConnected = null;
        }
    }
}
