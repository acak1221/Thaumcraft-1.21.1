package thaumic.tinkerer.common.core.fakeplayer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A server-side player that is never connected and never added to the level: the stand-in of the Animation Tablet and
 * the Remote Placer (1.7.10: Forge's {@code FakePlayer}).
 *
 * <p>The add-on is loader neutral, so it cannot use the FakePlayer of Fabric API or NeoForge. This is a plain
 * {@link ServerPlayer} with a dummy packet listener (every packet is dropped), no menus, no chat, no damage and no
 * collision box. It is not ticked by the level; its owner positions it and drives it ({@link #useItemOnBlock},
 * {@link #useItemInAir}, {@link #attackEntity}, {@link #interactWithEntity}), which run the vanilla paths
 * ({@code ServerPlayerGameMode}, {@code Player.attack}, {@code Player.interactOn}), so enchantments, tool wear, drops,
 * and the loaders' interaction and break events apply as they would to a player.</p>
 */
public class TinkererFakePlayer extends ServerPlayer {
    /** The attack-strength ticker value that counts as a fully charged swing (1.7.10 had no attack cooldown). */
    private static final int CHARGED = 1000;

    private final Multimap<Holder<Attribute>, AttributeModifier> appliedModifiers = HashMultimap.create();
    private ItemStack modifiersFor = ItemStack.EMPTY;

    public TinkererFakePlayer(ServerLevel level, GameProfile profile) {
        super(level.getServer(), level, profile, ClientInformation.createDefault());
        this.connection = new SilentConnection(level, this, profile);
        this.getAbilities().instabuild = false;
        this.setOnGround(true);
    }

    /** The super constructor looks up the world spawn height through the chunk source: not needed, and it loads chunks. */
    @Override
    public BlockPos adjustSpawnLocation(ServerLevel level, BlockPos pos) {
        return pos;
    }

    /** Puts the fake player at a point, looking along {@code yRot}/{@code xRot} (1.7.10 {@code posX/rotationYaw}). */
    public void placeAt(double x, double y, double z, float yRot, float xRot) {
        this.moveTo(x, y, z, yRot, xRot);
        this.setYHeadRot(yRot);
        this.setYBodyRot(yRot);
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0;
    }

    /**
     * Makes the attribute map agree with the stack in the main hand. A level ticks its players, which is where vanilla
     * applies the attribute modifiers of held items (a sword's attack damage); a fake player is never ticked, so the
     * owner calls this before it attacks.
     */
    public void refreshHeldItemModifiers() {
        ItemStack held = this.getMainHandItem();
        if (ItemStack.isSameItemSameComponents(modifiersFor, held)) return;
        this.getAttributes().removeAttributeModifiers(appliedModifiers);
        appliedModifiers.clear();
        if (!held.isEmpty()) {
            held.forEachModifier(EquipmentSlot.MAINHAND, appliedModifiers::put);
            this.getAttributes().addTransientAttributeModifiers(appliedModifiers);
        }
        modifiersFor = held.copy();
    }

    /** A right click on a block, as a player would do it; the vanilla path fires the interaction events. */
    public InteractionResult useItemOnBlock(BlockHitResult hit) {
        ItemStack stack = this.getMainHandItem();
        return this.gameMode.useItemOn(this, this.serverLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }

    /** A right click into the air ({@code sendUseItem} of 1.7.10). The use of an item that needs holding is dropped at once. */
    public InteractionResult useItemInAir() {
        ItemStack stack = this.getMainHandItem();
        InteractionResult result = this.gameMode.useItem(this, this.serverLevel(), stack, InteractionHand.MAIN_HAND);
        if (this.isUsingItem()) this.stopUsingItem();
        return result;
    }

    /** {@code attackTargetEntityWithCurrentItem}: a left click on an entity with a charged swing. */
    public void attackEntity(Entity target) {
        refreshHeldItemModifiers();
        this.attackStrengthTicker = CHARGED;
        this.attack(target);
    }

    /** {@code interactWith}: a right click on an entity; true when something happened. */
    public boolean interactWithEntity(Entity target) {
        InteractionResult result = this.interactOn(target, InteractionHand.MAIN_HAND);
        if (this.isUsingItem()) this.stopUsingItem();
        return result.consumesAction();
    }

    /** Throws out everything the fake player carries except the slots in {@code keep}. */
    public void dropAllExcept(int... keep) {
        for (int slot = 0; slot < this.getInventory().getContainerSize(); slot++) {
            boolean kept = false;
            for (int k : keep) kept |= k == slot;
            if (kept) continue;
            ItemStack stack = this.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack, 0);
                this.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    // ---- everything below keeps the fake player inert -------------------------------------------------------------

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.0F, 0.0F);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    @Override
    public boolean setGameMode(GameType gameMode) {
        return false;
    }

    @Override
    public void tick() {}

    @Override
    public void doTick() {}

    @Override
    public void setHealth(float health) {}

    @Override
    public void sendSystemMessage(Component message) {}

    @Override
    public void displayClientMessage(Component message, boolean actionBar) {}

    /** No menu is ever shown to the fake player, so a chest it "opens" does not keep a viewer. */
    @Override
    public OptionalInt openMenu(MenuProvider provider) {
        return OptionalInt.empty();
    }

    /** The packet listener of the fake player: it has no channel, so every packet is dropped. */
    private static final class SilentConnection extends ServerGamePacketListenerImpl {
        SilentConnection(ServerLevel level, ServerPlayer player, GameProfile profile) {
            super(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player,
                    CommonListenerCookie.createInitial(profile, false));
        }

        @Override
        public void send(Packet<?> packet) {}

        @Override
        public void send(Packet<?> packet, PacketSendListener listener) {}

        @Override
        public void disconnect(Component reason) {}
    }
}
