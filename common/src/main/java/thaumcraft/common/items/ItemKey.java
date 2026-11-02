package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockArcaneDoor;
import thaumcraft.common.blocks.BlockArcanePressurePlate;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.lib.items.EquipmentRules;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileOwned;

import java.util.List;
import java.util.Objects;

/**
 * The keys (original ItemKey). An iron key and a gold key link themselves to an arcane door or to an arcane pressure plate; a linked key
 * grants access to that door again, and the gold key may also hand the access on to others.
 *
 * <p>The ownership of a door lives in the TileOwned of the door: the first use of a key links the key to the door of its owner, every
 * further use writes the name of the player into the access list of the door.
 */
public final class ItemKey extends Item {
    /** Native item admission can synchronously call the held key again. */
    private static final ThreadLocal<Boolean> using = new ThreadLocal<>();

    /** The link of a key: the position of the door it was linked to and the kind of door. */
    public record KeyLink(String location, int type) {}

    /** Additional authority for a native key; the legacy public xyz/type record remains unchanged. */
    public record KeyScope(ResourceLocation dimension, String owner) {
        public KeyScope { Objects.requireNonNull(dimension, "dimension"); Objects.requireNonNull(owner, "owner"); }
    }

    /** The kind of the block a key works on: the arcane door. */
    private static final int KIND_DOOR = 0;
    /** The kind of the block a key works on: the arcane pressure plate. */
    private static final int KIND_PLATE = 1;

    private static final SoundEvent KEY = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "key"));

    private final int keyType;

    public ItemKey(Properties properties, int keyType) {
        super(properties.stacksTo(64).rarity(Rarity.UNCOMMON));
        this.keyType = keyType;
    }

    /** The kind of key: zero the iron key, one the gold key. */
    public int keyType() { return keyType; }

    /**
     * Oracle ItemKey:90-92 glints a key that carries its link data. The original used the
     * presence of the NBT compound; the native equivalent is the presence of the link component.
     */
    @Override public boolean isFoil(ItemStack stack) { return stack.has(ThaumcraftLegacyItems.KEY_LINK.get()); }

    /** The link of the stack, or null when the key was never linked. */
    public static KeyLink link(ItemStack stack) { return stack.get(ThaumcraftLegacyItems.KEY_LINK.get()); }

    /** Original ItemKey.onItemUseFirst: the key links itself to a door, or grants access to the door it is linked to. */
    @Override public InteractionResult useOn(UseOnContext context) {
        if (Boolean.TRUE.equals(using.get())) return InteractionResult.FAIL;
        using.set(true);
        try {
            return useKey(context);
        } finally {
            using.remove();
        }
    }

    private InteractionResult useKey(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Target target = target(level, pos);
        if (target == null) return InteractionResult.PASS;
        TileOwned owned = owned(level, target.owner());
        if (owned == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        if (stack.isEmpty() || !stack.is(this)) return InteractionResult.PASS;
        KeyLink link = link(stack);
        String name = player.getGameProfile().getName();
        String location = EquipmentRules.keyLocation(target.owner().getX(), target.owner().getY(), target.owner().getZ());
        KeyScope expectedScope = new KeyScope(level.dimension().location(), Objects.requireNonNullElse(owned.owner(), ""));
        if (link == null) {
            // Original ItemKey.onItemUseFirst: the key claims the door of its owner, or a door the player may enter with the gold key.
            if (!EquipmentRules.keyMayLink(name.equals(owned.owner()),
                    EquipmentRules.keyHasGrantAccess(owned.accessList(), name), keyType)) return InteractionResult.PASS;
            if (!level.isClientSide) {
                if (!(level instanceof ServerLevel server)) return InteractionResult.FAIL;
                ItemStack linked = new ItemStack(stack.getItem(), 1);
                linked.set(ThaumcraftLegacyItems.KEY_LINK.get(), new KeyLink(location, target.kind()));
                linked.set(ThaumcraftLegacyItems.KEY_SCOPE.get(), expectedScope);
                if (player.getInventory().add(linked)) {
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                } else {
                    var output = prepareOverflow(server, player, linked);
                    if (!output.ready()) return InteractionResult.FAIL;
                    // Payment and durable ownership precede every ADD callback.
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    output.commitRaw();
                    DeferredItemDrops.tick(server);
                }
                player.displayClientMessage(Component.translatable(target.kind() == KIND_DOOR ? "tc.key1" : "tc.key2")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
                level.playSound(null, target.owner().getX() + 0.5, target.owner().getY() + 0.5, target.owner().getZ() + 0.5,
                        KEY, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }
        boolean owner = name.equals(owned.owner());
        boolean sameBlock = location.equals(link.location()) && target.kind() == link.type();
        boolean sameScope = expectedScope.equals(stack.get(ThaumcraftLegacyItems.KEY_SCOPE.get()));
        // Old native keys have no provable dimension/issuer. Only the current
        // lock owner may explicitly bind that existing physical key again.
        if (owner && sameBlock && !sameScope) {
            if (!level.isClientSide) {
                stack.set(ThaumcraftLegacyItems.KEY_SCOPE.get(), expectedScope);
                player.displayClientMessage(Component.translatable(target.kind() == KIND_DOOR ? "tc.key1" : "tc.key2")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
                level.playSound(null, target.owner(), KEY, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }
        boolean hasEntry = EquipmentRules.keyHasAccess(owned.accessList(), keyType, name);
        boolean hasGrant = EquipmentRules.keyHasGrantAccess(owned.accessList(), name);
        if (!owner && !hasEntry && !hasGrant && sameBlock && sameScope) {
            // Original ItemKey.onItemUseFirst: the key writes its access entry into the access list of the door.
            if (!level.isClientSide) {
                String entry = EquipmentRules.keyAccessEntry(keyType, name);
                owned.addAccess(entry);
                if (target.kind() == KIND_DOOR) {
                    // Original ItemKey.onItemUseFirst: the upper half of the door carries the same access list.
                    TileOwned other = owned(level, target.other());
                    if (other != null) other.addAccess(entry);
                }
                player.displayClientMessage(Component.translatable(target.kind() == KIND_DOOR ? "tc.key3" : "tc.key5")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
                        .append(keyType == EquipmentRules.KEY_GOLD
                                ? Component.translatable(target.kind() == KIND_DOOR ? "tc.key4" : "tc.key6")
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
                                : Component.empty()), false);
                level.playSound(null, target.owner().getX() + 0.5, target.owner().getY() + 0.5, target.owner().getZ() + 0.5,
                        KEY, SoundSource.BLOCKS, 1.0F, 1.1F);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide) {
            // Original ItemKey.onItemUseFirst: the key belongs to another door, or the player may not enter this one.
            player.displayClientMessage(Component.translatable(sameBlock && sameScope ? "tc.key8" : "tc.key7")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
        }
        return InteractionResult.SUCCESS;
    }

    /** Preserve the native player throw, including its sampled motion and pickup delay, across refused admission. */
    private static DeferredItemDrops.PreparedBatch prepareOverflow(ServerLevel level, Player player, ItemStack linked) {
        float yaw = player.getYRot() / 180 * (float) Math.PI, pitch = player.getXRot() / 180 * (float) Math.PI;
        double vx = -Mth.sin(yaw) * Mth.cos(pitch) * .3F;
        double vz = Mth.cos(yaw) * Mth.cos(pitch) * .3F;
        double vy = -Mth.sin(pitch) * .3F + .1F;
        float angle = player.getRandom().nextFloat() * (float) Math.PI * 2;
        float spread = .02F * player.getRandom().nextFloat();
        vx += Math.cos(angle) * spread;
        vy += (double) ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * .1F);
        vz += Math.sin(angle) * spread;
        Vec3 position = new Vec3(player.getX(), player.getY() - (double) .3F + player.getEyeHeight(), player.getZ());
        return DeferredItemDrops.prepareDirectedBatch(level, player.blockPosition(), List.of(
                new DeferredItemDrops.DirectedDrop(linked, position, new Vec3(vx, vy, vz), 40)));
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        KeyLink link = link(stack);
        if (link == null) {
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }
        tooltip.add(Component.translatable("tc.key9").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(link.type() == KIND_DOOR ? "tc.key10" : "tc.key11").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        String[] parts = link.location().split(",");
        String where = parts.length == 3 ? "x " + parts[0] + ", z " + parts[2] + ", y " + parts[1] : link.location();
        tooltip.add(Component.literal(where).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** The block a key works on: the arcane door or the arcane pressure plate of the wooden device. */
    private static Target target(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BlockArcaneDoor) {
            // Original ItemKey.onItemUseFirst: the owner of a door is its lower half, the upper half carries the same access list.
            boolean upper = state.getValue(BlockArcaneDoor.HALF) == Half.TOP;
            return new Target(KIND_DOOR, upper ? pos.below() : pos, upper ? pos : pos.above());
        }
        if (state.getBlock() instanceof BlockArcanePressurePlate) {
            int variant = BlockArcanePressurePlate.variant(state);
            // Original ItemKey.onItemUseFirst: the wooden device with the metadata two or three, the arcane pressure plate.
            if (variant == EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE
                    || variant == EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE_PRESSED) {
                return new Target(KIND_PLATE, pos, pos);
            }
        }
        return null;
    }

    /** The owned block entity of a position, or null when the position carries none. */
    private static TileOwned owned(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileOwned owned ? owned : null;
    }

    /** The block a key works on: its kind and the positions of the halves that carry the ownership. */
    private record Target(int kind, BlockPos owner, BlockPos other) {}
}
