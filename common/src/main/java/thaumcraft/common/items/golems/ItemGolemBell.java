package thaumcraft.common.items.golems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.entities.golems.EntityGolemBase;

/** ItemGolemBell:82-249 selection/markers. EntityGolemBase.hurt owns left-click
 * recall; this item never grants a placer, drops cargo, or rehomes a golem. */
public final class ItemGolemBell extends Item {
    private static final String DATA_KEY = "thaumcraft:golem_bell";
    private static final int MAX_MARKERS = GolemData.MAX_MARKERS;
    private static final Set<String> SELECTION_FIELDS = Set.of("Golem", "Dimension", "Home", "Face", "Markers");
    private static final Set<String> MARKER_FIELDS = Set.of("Pos", "Dimension", "Side", "Color");
    private final ThreadLocal<Boolean> editing = new ThreadLocal<>();

    public ItemGolemBell(Properties properties) { super(properties); }

    /** Native tool adaptation: entity recall is not permission to mine blocks. */
    @Override public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) { return false; }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof EntityGolemBase golem)) return InteractionResult.PASS;
        return player.getItemInHand(hand) == stack ? select(stack, player, golem) : InteractionResult.FAIL;
    }

    /** Also called by the actual golem's mobInteract. Identical selection is a
     * consumed no-op, so the item fallback cannot repeat sound or data changes. */
    public static InteractionResult select(ItemStack stack, Player player, EntityGolemBase golem) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGolemBell bell)) return InteractionResult.PASS;
        if (!heldBy(player, stack) || !player.isAlive() || player.isSpectator()
                || !golem.isAlive() || golem.isRemoved() || player.level() != golem.level()) return InteractionResult.FAIL;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player.level() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || Boolean.TRUE.equals(bell.editing.get())) return InteractionResult.FAIL;
        bell.editing.set(true);
        try {
            ItemStack before = stack.copy();
            Selection old = readSelection(stack);
            if (!mayConfigure(level, player, golem)) return InteractionResult.FAIL;
            Selection selected = selectionFor(level, golem);
            if (!heldBy(player, stack) || !ItemStack.matches(stack, before) || !mayConfigure(level, player, golem)) return InteractionResult.FAIL;
            if (!selected.equals(old)) {
                writeSelection(stack, selected);
                changed(level, player, golem.blockPosition());
            }
            return InteractionResult.CONSUME;
        } catch (IllegalArgumentException malformed) {
            // Keep unknown/future configuration intact; never silently reset it.
            return InteractionResult.FAIL;
        } finally {
            bell.editing.remove();
        }
    }

    /** Edit actual selected-worker links and the bell snapshot together. Gather
     * ignores links; no marker action changes home, filters, quantities or cargo. */
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack held = context.getItemInHand();
        if (player == null || held.isEmpty() || held.getItem() != this || player.getItemInHand(context.getHand()) != held
                || !player.isAlive() || player.isSpectator() || player.level() != context.getLevel()) return InteractionResult.FAIL;
        if (Boolean.TRUE.equals(editing.get())) return InteractionResult.FAIL;
        editing.set(true);
        try {
            ItemStack before = held.copy();
            Selection selected = readSelection(held);
            if (selected == null) return InteractionResult.PASS;
            if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
            if (!(context.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()) return InteractionResult.FAIL;
            if (!selected.dimension.equals(level.dimension().location())) return InteractionResult.PASS;
            var entity = level.getEntity(selected.golem);
            if (!(entity instanceof EntityGolemBase golem) || !mayEditLinks(level, player, golem)) return InteractionResult.PASS;
            BlockPos pos = context.getClickedPos();
            Direction face = context.getClickedFace();
            if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !player.canInteractWithBlock(pos, 0) || !level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, face, held)) return InteractionResult.FAIL;
            Selection actual = selectionFor(level, golem);
            var spot = new GolemMarker(selected.dimension, pos, face);
            // Original changeMarkers: without Order the click toggles an uncoloured marker; with Order an existing marker cycles
            // any -> 0 .. 15 -> removed, and a sneaking click removes it at once.
            var markers = GolemMarker.click(actual.markers, spot, golem.getUpgradeAmount(GolemUpgrade.ORDER) > 0, player.isShiftKeyDown(), MAX_MARKERS);
            if (markers == null) return InteractionResult.FAIL;
            Selection edited = actual.withMarkers(markers);
            CustomData data = selectionData(held, edited);
            if (player.getItemInHand(context.getHand()) != held || !ItemStack.matches(held, before)
                    || !mayEditLinks(level, player, golem)) return InteractionResult.FAIL;
            golem.setMarkers(edited.markers);
            if (!golem.getMarkers().equals(edited.markers)) return InteractionResult.FAIL; // The golem refused the edit: keep the bell as it was.
            held.set(DataComponents.CUSTOM_DATA, data);
            changed(level, player, pos);
            announceColor(player, edited.markers, spot);
            return InteractionResult.CONSUME;
        } catch (IllegalArgumentException malformed) {
            return InteractionResult.FAIL;
        } finally {
            editing.remove();
        }
    }

    /** Without native player/server context, clearing a cache cannot clear the
     * selected worker. Existing callers must use the authoritative overload. */
    @Deprecated
    public static boolean clearMarkers(ItemStack stack) {
        return false;
    }

    /** Root's hand-only item-key packet calls this, never a client-supplied UUID.
     * Only an already loaded selected entity is considered; no air-use binding. */
    public static boolean clearMarkers(ItemStack stack, Player player) {
        if (player == null || stack.isEmpty() || !(stack.getItem() instanceof ItemGolemBell bell)
                || !heldBy(player, stack) || !player.isAlive() || player.isSpectator()
                || !(player.level() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || Boolean.TRUE.equals(bell.editing.get())) return false;
        bell.editing.set(true);
        try {
            ItemStack before = stack.copy();
            Selection selected = readSelection(stack);
            if (selected == null || !selected.dimension.equals(level.dimension().location())) return false;
            var entity = level.getEntity(selected.golem);
            if (!(entity instanceof EntityGolemBase golem) || !mayEditLinks(level, player, golem)) return false;
            Selection actual = selectionFor(level, golem);
            Selection cleared = actual.withMarkers(List.of());
            if (actual.markers.isEmpty() && cleared.equals(selected)) return false;
            CustomData data = selectionData(stack, cleared);
            if (!heldBy(player, stack) || !ItemStack.matches(stack, before) || !mayEditLinks(level, player, golem)) return false;
            golem.setMarkers(cleared.markers);
            stack.set(DataComponents.CUSTOM_DATA, data);
            changed(level, player, golem.blockPosition());
            return true;
        } catch (IllegalArgumentException malformed) {
            return false;
        } finally {
            bell.editing.remove();
        }
    }

    /** Read-only bell snapshot for client outlines. Invalid/future data is left
     * intact, and looking at a bell never resolves or loads its selected entity. */
    public static List<GolemMarker> selectedMarkers(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGolemBell)) return List.of();
        try {
            Selection selected = readSelection(stack);
            return selected == null ? List.of() : selected.markers;
        } catch (IllegalArgumentException malformed) {
            return List.of();
        }
    }

    private static Selection selectionFor(ServerLevel level, EntityGolemBase golem) {
        return new Selection(golem.getUUID(), level.dimension().location(), golem.getHomePosition(), golem.getHomeFacing(), golem.getMarkers());
    }

    private static boolean heldBy(Player player, ItemStack stack) {
        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    private static boolean mayConfigure(ServerLevel level, Player player, EntityGolemBase golem) {
        return player.level() == level && golem.level() == level && player.isAlive() && !player.isSpectator()
                && golem.isAlive() && !golem.isRemoved() && level.getEntity(golem.getUUID()) == golem
                && level.hasChunkAt(golem.blockPosition()) && player.canInteractWithEntity(golem, 0)
                && level.mayInteract(player, golem.blockPosition()) && golem.canConfigure(player);
    }

    private static boolean mayEditLinks(ServerLevel level, Player player, EntityGolemBase golem) {
        // Original changeMarkers/reset resolve the selected loaded worker; they
        // do not require a second entity ray/reach while clicking a distant inventory.
        // Initial selection still uses mayConfigure and native entity reach.
        return player.level() == level && golem.level() == level && player.isAlive() && !player.isSpectator()
                && golem.isAlive() && !golem.isRemoved() && level.getEntity(golem.getUUID()) == golem
                && level.hasChunkAt(golem.blockPosition()) && level.mayInteract(player, golem.blockPosition())
                && golem.canEditLinks(player);
    }

    private static void changed(ServerLevel level, Player player, BlockPos pos) {
        try {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                    .7F, 1 + level.random.nextFloat() * .1F);
        } catch (RuntimeException failure) {
            // The edit is already committed. Return CONSUME, not a retry which
            // would toggle the same marker a second time or overwrite callbacks.
            Thaumcraft.LOG.warn("Golem bell notification failed at {}", pos, failure);
        }
    }

    private record Selection(UUID golem, ResourceLocation dimension, BlockPos home, Direction face, List<GolemMarker> markers) {
        private Selection {
            if (golem == null || dimension == null || home == null || face == null) throw new IllegalArgumentException("Incomplete bell selection");
            home = home.immutable();
            packedPosition(home);
            if (markers == null || markers.size() > MAX_MARKERS) throw new IllegalArgumentException("Invalid bell marker count");
            var unique = new HashSet<GolemMarker>();
            for (GolemMarker marker : markers) {
                if (marker == null || !unique.add(marker)) throw new IllegalArgumentException("Null or duplicate bell marker");
                packedPosition(marker.pos());
            }
            markers = List.copyOf(markers);
        }

        private Selection withMarkers(List<GolemMarker> markers) {
            return new Selection(golem, dimension, home, face, markers);
        }
    }

    private static Selection readSelection(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag root = data.copyTag();
        if (!root.contains(DATA_KEY)) return null;
        if (!root.contains(DATA_KEY, Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Malformed bell selection");
        CompoundTag tag = root.getCompound(DATA_KEY);
        if (!tag.getAllKeys().equals(SELECTION_FIELDS) || !tag.hasUUID("Golem") || !tag.contains("Dimension", Tag.TAG_STRING)
                || !tag.contains("Home", Tag.TAG_LONG) || !tag.contains("Face", Tag.TAG_BYTE)
                || !tag.contains("Markers", Tag.TAG_LIST)) throw new IllegalArgumentException("Unsupported bell selection");
        ResourceLocation dimension = dimension(tag.getString("Dimension"));
        Direction face = direction(tag.getByte("Face"));
        Tag raw = tag.get("Markers");
        if (!(raw instanceof ListTag list) || list.size() > MAX_MARKERS
                || !list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND) throw new IllegalArgumentException("Invalid bell markers");
        var markers = new ArrayList<GolemMarker>(list.size());
        for (Tag entry : list) {
            if (!(entry instanceof CompoundTag marker)) throw new IllegalArgumentException("Unsupported bell marker");
            markers.add(readMarker(marker));
        }
        return new Selection(tag.getUUID("Golem"), dimension, BlockPos.of(tag.getLong("Home")), face, List.copyOf(markers));
    }

    private static ResourceLocation dimension(String name) {
        ResourceLocation dimension = ResourceLocation.tryParse(name);
        if (dimension == null || !dimension.toString().equals(name)) throw new IllegalArgumentException("Invalid bell dimension");
        return dimension;
    }

    private static Direction direction(int side) {
        if (side < 0 || side > 5) throw new IllegalArgumentException("Invalid bell face");
        return Direction.from3DDataValue(side);
    }

    private static GolemMarker readMarker(CompoundTag marker) {
        if (!marker.getAllKeys().equals(MARKER_FIELDS) || !marker.contains("Pos", Tag.TAG_LONG)
                || !marker.contains("Dimension", Tag.TAG_STRING) || !marker.contains("Side", Tag.TAG_BYTE)
                || !marker.contains("Color", Tag.TAG_BYTE) || marker.getByte("Color") < GolemMarker.ANY || marker.getByte("Color") > GolemMarker.LAST_COLOR) {
            throw new IllegalArgumentException("Unsupported bell marker");
        }
        return new GolemMarker(dimension(marker.getString("Dimension")), BlockPos.of(marker.getLong("Pos")), direction(marker.getByte("Side")),
                marker.getByte("Color"));
    }

    /** The action-bar line of the original "tc.markerchange" notification: the spot's colour after the click, if it still exists. */
    private static void announceColor(Player player, List<GolemMarker> markers, GolemMarker spot) {
        for (GolemMarker marker : markers) {
            if (!marker.sameSpot(spot) || marker.color() == GolemMarker.ANY) continue;
            player.displayClientMessage(Component.translatable("tooltip.thaumcraft.golem.marker_color",
                    Component.translatable("color.minecraft." + net.minecraft.world.item.DyeColor.byId(marker.color()).getName())), true);
            return;
        }
    }

    private static CompoundTag writeMarker(GolemMarker marker) {
        var row = new CompoundTag();
        row.putLong("Pos", packedPosition(marker.pos()));
        row.putString("Dimension", marker.dimension().toString());
        row.putByte("Side", (byte) marker.face().get3DDataValue());
        row.putByte("Color", (byte) marker.color());
        return row;
    }

    private static long packedPosition(BlockPos pos) {
        long packed = pos.asLong();
        if (!BlockPos.of(packed).equals(pos)) throw new IllegalArgumentException("Unrepresentable bell position");
        return packed;
    }

    private static void writeSelection(ItemStack stack, Selection selected) {
        stack.set(DataComponents.CUSTOM_DATA, selectionData(stack, selected));
    }

    private static CustomData selectionData(ItemStack stack, Selection selected) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Golem", selected.golem);
        tag.putString("Dimension", selected.dimension.toString());
        tag.putLong("Home", packedPosition(selected.home));
        tag.putByte("Face", (byte) selected.face.get3DDataValue());
        var markers = new ListTag();
        for (GolemMarker marker : selected.markers) markers.add(writeMarker(marker));
        tag.put("Markers", markers);
        CustomData old = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = old == null ? new CompoundTag() : old.copyTag();
        root.put(DATA_KEY, tag);
        return CustomData.of(root);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        tooltip.add(Component.translatable("tooltip.thaumcraft.golem.bell"));
        tooltip.add(Component.translatable("tooltip.thaumcraft.golem.bell_markers_local").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.thaumcraft.golem.bell_reset").withStyle(ChatFormatting.GRAY));
        try {
            Selection selected = readSelection(stack);
            if (selected != null) {
                tooltip.add(Component.translatable("tooltip.thaumcraft.golem.bell_selected", selected.dimension.toString(),
                        selected.home.getX(), selected.home.getY(), selected.home.getZ(), selected.markers.size()));
            }
        } catch (IllegalArgumentException malformed) {
            tooltip.add(Component.translatable("tooltip.thaumcraft.golem.invalid_configuration").withStyle(ChatFormatting.RED));
        }
    }
}
