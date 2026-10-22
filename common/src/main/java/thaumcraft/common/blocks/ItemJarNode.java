package thaumcraft.common.blocks;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/**
 * The item form of the node jar.
 *
 * <p>Oracle {@code thaumcraft/common/blocks/ItemJarNode.java:56-214}: the item of the original carries
 * the aspects of the stored node, its node type, its node modifier and its identity, and it publishes
 * that data with {@code setAspects}, {@code setNodeAttributes}, {@code getNodeType},
 * {@code getNodeModifier} and {@code getNodeId}. It is an {@link IEssentiaContainerItem} and its stack
 * limit is one (oracle {@code :63-64}).</p>
 *
 * <p>Forced 1.21.1 change: the data of the item lives in the block entity data component of 1.21.1
 * instead of the NBT of 1.7.10, and the key names of the node attributes of the item are the key names
 * of the tile ({@code type}, {@code modifier}, {@code nodeId}) instead of the own key names of the
 * original item ({@code nodetype}, {@code nodemod}, {@code nodeid}). The aspects keep the key
 * {@code Aspects} of the original. The published methods, their names and their values are the original
 * ones, and the placement hands the data of the item to the placed tile exactly like the original
 * (oracle {@code :133-145}).</p>
 */
public final class ItemJarNode extends BlockItem implements IEssentiaContainerItem {
    // TooltipContext has no player in 1.21. The client supplies a lazy lookup,
    // never a cached player or a client-class dependency on a dedicated server.
    private static Supplier<? extends Player> tooltipViewer = () -> null;

    public ItemJarNode(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public static void setTooltipViewer(Supplier<? extends Player> viewer) {
        tooltipViewer = Objects.requireNonNull(viewer, "viewer");
    }

    /** The aspects of the stored node, or null when the item carries none. */
    @Override public AspectList getAspects(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CompoundTag tag = data(stack);
        if (tag.isEmpty()) return null;
        AspectList aspects = new AspectList();
        aspects.readFromNBT(tag);
        return aspects.size() > 0 ? aspects : null;
    }

    /** The aspects of the stored node; the base of the node follows them when the item is placed. */
    @Override public void setAspects(ItemStack stack, AspectList aspects) {
        if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("A node jar item is required");
        CompoundTag tag = data(stack);
        if (aspects == null) {
            tag.remove("Aspects");
        } else {
            aspects.writeToNBT(tag);
        }
        // The base of the stored node is the aspects of the item itself.
        tag.remove("AspectsBase");
        write(stack, tag);
    }

    /** The node type, the node modifier and the identity of the stored node. */
    public void setNodeAttributes(ItemStack stack, NodeType type, NodeModifier mod, String id) {
        if (stack == null || stack.isEmpty()) throw new IllegalArgumentException("A node jar item is required");
        CompoundTag tag = data(stack);
        writeNodeAttributes(tag, type, mod, id);
        write(stack, tag);
    }

    /** The writer of the node attributes of the original item. */
    public static void writeNodeAttributes(CompoundTag tag, NodeType type, NodeModifier mod, String id) {
        tag.putInt("type", type == null ? 0 : type.ordinal());
        if (mod != null) tag.putInt("modifier", mod.ordinal());
        else tag.remove("modifier");
        tag.putString("nodeId", id == null ? "" : id);
    }

    /** The node type of the stored node, null when the item carries none. */
    public NodeType getNodeType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CompoundTag tag = data(stack);
        if (tag.isEmpty()) return null;
        return nodeTypeOf(tag);
    }

    /** The node type of the stored data. */
    public static NodeType nodeTypeOf(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return null;
        NodeType[] types = NodeType.values();
        int type = tag.getInt("type");
        return type >= 0 && type < types.length ? types[type] : NodeType.NORMAL;
    }

    /** The node modifier of the stored node, null when the item carries none. */
    public NodeModifier getNodeModifier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CompoundTag tag = data(stack);
        if (tag.isEmpty()) return null;
        return nodeModifierOf(tag);
    }

    /** The node modifier of the stored data. */
    public static NodeModifier nodeModifierOf(CompoundTag tag) {
        if (tag == null || tag.isEmpty() || !tag.contains("modifier")) return null;
        NodeModifier[] modifiers = NodeModifier.values();
        int modifier = tag.getInt("modifier");
        return modifier >= 0 && modifier < modifiers.length ? modifiers[modifier] : null;
    }

    /** The identity of the stored node; the original answers "0" for an item without data. */
    public String getNodeId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "0";
        CompoundTag tag = data(stack);
        if (tag.isEmpty()) return "0";
        return tag.getString("nodeId");
    }

    /**
     * The original tooltip: the node type, the node modifier and the aspects of the stored node.
     *
     * <p>Oracle {@code ItemJarNode.java:81-98}: the type and the modifier share one line, the aspects
     * follow one line each and an aspect that the player has not discovered is the unknown marker.</p>
     */
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        Player viewer = tooltipViewer.get();
        NodeType type = getNodeType(stack);
        if (type != null) {
            MutableComponent line = Component.translatable("nodetype." + type.name() + ".name");
            NodeModifier modifier = getNodeModifier(stack);
            if (modifier != null) {
                line = line.append(Component.literal(", ")).append(Component.translatable("nodemod." + modifier.name() + ".name"));
            }
            tooltip.add(line.withStyle(ChatFormatting.DARK_PURPLE));
        }
        AspectList aspects = getAspects(stack);
        if (aspects != null) {
            for (Aspect aspect : aspects.getAspectsSorted()) {
                boolean known = viewer != null && KnowledgeAccess.get(viewer).hasDiscoveredAspect(aspect.getTag());
                tooltip.add(known ? Component.literal(aspect.getName() + " x " + aspects.getAmount(aspect))
                        : Component.translatable("tc.aspect.unknown"));
            }
        }
        super.appendHoverText(stack, context, tooltip, flags);
    }

    /** The data of the item. */
    private static CompoundTag data(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /** The writer of the data of the item. */
    private static void write(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
    }
}
