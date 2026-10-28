package thaumic.tinkerer.common.block.tile.tablet;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.common.core.fakeplayer.TinkererFakePlayer;

/**
 * The fake player of one Animation Tablet: it stands in the middle of the tablet, looks the way the tablet faces and
 * holds the tablet's item in the first hotbar slot (1.7.10 {@code TabletFakePlayer extends FakePlayer}).
 */
public final class TabletFakePlayer extends TinkererFakePlayer {
    /** The profile of the original tablet player. */
    public static final UUID PROFILE_ID = UUID.fromString("a8f026a0-135b-11e4-9191-0800200c9a66");
    public static final String PROFILE_NAME = "[ThaumcraftTablet]";

    /** 1.7.10 {@code rotationPitch = -15}: the tablet looks slightly up. */
    private static final float PITCH = -15F;

    private final TileAnimationTablet tablet;

    public TabletFakePlayer(ServerLevel level, TileAnimationTablet tablet) {
        super(level, new GameProfile(PROFILE_ID, PROFILE_NAME));
        this.tablet = tablet;
        this.setXRot(PITCH);
    }

    /**
     * {@code onUpdate}: put the player in the middle of the tablet, facing the way the tablet faces, drop everything but
     * the held item, and keep the held item in slot zero.
     */
    public void onUpdate() {
        BlockPos pos = tablet.getBlockPos();
        Direction facing = tablet.getFacing();
        // yaw 180 faces north, 0 south, 90 west, -90 east, as in 1.7.10 (and as Direction.toYRot).
        placeAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, facing.toYRot(), PITCH);
        this.getInventory().selected = 0;
        this.getAbilities().instabuild = false;
        // Drop everything except for the held item.
        dropAllExcept(0);
        syncSlots();
        refreshHeldItemModifiers();
    }

    /** {@code syncSlots}: slot zero is the tablet's item. */
    public void syncSlots() {
        ItemStack held = tablet.heldItem;
        if (this.getInventory().getItem(0) != held) this.getInventory().setItem(0, held);
    }

    /** The item the player holds now (an action may have replaced or used up the tablet's stack). */
    public ItemStack getHeldItem() {
        return this.getInventory().getItem(0);
    }
}
