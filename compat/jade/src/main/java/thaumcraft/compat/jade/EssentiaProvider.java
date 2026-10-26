package thaumcraft.compat.jade;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import thaumcraft.api.IGoggles;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileCrucible;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TileTube;

/**
 * The essentia of a block: what a container, the crucible or an alchemical furnace holds, the
 * label or filter, and for a tube or any essentia transport its suction on the looked-at face.
 *
 * <p>TC4 shows a container's aspects only through the goggles of revealing (the world popup of
 * {@code ThaumcraftGogglesClient}); by default this provider follows that rule, and its Jade option
 * {@code thaumcraft.essentia_goggles} drops it. An aspect the player has not discovered shows the
 * unknown image, as the goggles do.</p>
 */
enum EssentiaProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "essentia");
    static final ResourceLocation GOGGLES = ResourceLocation.fromNamespaceAndPath("thaumcraft", "essentia_goggles");
    private static final int PER_LINE = 8;

    @Override public ResourceLocation getUid() { return UID; }

    @Override public boolean shouldRequestData(BlockAccessor accessor) {
        var entity = accessor.getBlockEntity();
        return aspectsOf(entity) != null || entity instanceof IEssentiaTransport;
    }

    @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        var entity = accessor.getBlockEntity();
        AspectList held = aspectsOf(entity);
        if (held != null) {
            data.put("Aspects", write(held));
            int capacity = ThaumcraftJadePlugin.capacity(entity);
            if (capacity > 0) data.putInt("Capacity", capacity);
        } else if (entity instanceof IEssentiaTransport transport) {
            // A tube or nozzle without a container view: its one essentia on the looked-at face.
            Aspect essentia = transport.getEssentiaType(accessor.getSide());
            int amount = transport.getEssentiaAmount(accessor.getSide());
            if (essentia != null && amount > 0) data.put("Aspects", write(new AspectList().add(essentia, amount)));
        }
        Aspect filter = filterOf(entity);
        if (filter != null) data.putString("Filter", filter.getTag());
        if (entity instanceof IEssentiaTransport transport) {
            Direction face = accessor.getSide();
            Aspect suction = transport.getSuctionType(face);
            int amount = transport.getSuctionAmount(face);
            if (amount > 0) {
                data.putInt("Suction", amount);
                if (suction != null) data.putString("SuctionType", suction.getTag());
            }
        }
    }

    /**
     * What a block holds as essentia: an aspect container, and the crucible and the alchemical
     * furnaces, which hold essentia without the container interface. Null for anything else.
     */
    static AspectList aspectsOf(BlockEntity entity) {
        if (entity instanceof IAspectContainer container) return container.getAspects() == null ? new AspectList() : container.getAspects();
        if (entity instanceof TileCrucible crucible) return crucible.getAspects();
        if (entity instanceof TileAlchemyFurnace furnace) return furnace.getAspects();
        if (entity instanceof TileAlchemyFurnaceAdvanced furnace) return furnace.getAspects();
        return null;
    }

    /** The label of a jar or alembic, or the filter of a filter tube. */
    private static Aspect filterOf(BlockEntity entity) {
        if (entity instanceof TileJar jar) return jar.getFilter();
        if (entity instanceof TileAlembic alembic) return alembic.getFilter();
        if (entity instanceof TileTube tube) return tube.getFilter();
        return null;
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Player player = accessor.getPlayer();
        if (config.get(GOGGLES) && !wearsGoggles(player)) return;
        CompoundTag data = accessor.getServerData();
        AspectList client = aspectsOf(accessor.getBlockEntity());
        AspectList aspects = data.contains("Aspects", Tag.TAG_LIST) ? read(data.getList("Aspects", Tag.TAG_COMPOUND))
                : client != null ? client : new AspectList();
        var knowledge = KnowledgeAccess.get(player);
        List<IElement> line = new ArrayList<>();
        int total = 0;
        for (Aspect aspect : aspects.getAspectsSortedAmount()) {
            int amount = aspects.getAmount(aspect);
            if (amount <= 0) continue;
            total += amount;
            line.add(new AspectElement(aspect, amount, knowledge.hasDiscoveredAspect(aspect.getTag())));
            if (line.size() == PER_LINE) { tooltip.add(line); line = new ArrayList<>(); }
        }
        if (!line.isEmpty()) tooltip.add(line);
        if (data.contains("Capacity")) {
            tooltip.add(Component.translatable("tc.port.jade.capacity", total, data.getInt("Capacity")).withStyle(ChatFormatting.GRAY));
        }
        if (data.contains("Filter") && Aspect.getAspect(data.getString("Filter")) != null) {
            tooltip.add(Component.translatable("tc.port.jade.filter", name(Aspect.getAspect(data.getString("Filter")), knowledge)).withStyle(ChatFormatting.GRAY));
        }
        if (data.contains("Suction")) {
            Aspect type = data.contains("SuctionType") ? Aspect.getAspect(data.getString("SuctionType")) : null;
            Component name = type == null ? Component.translatable("tc.port.jade.any_aspect") : name(type, knowledge);
            tooltip.add(Component.translatable("tc.port.jade.suction", data.getInt("Suction"), name).withStyle(ChatFormatting.GRAY));
        }
    }

    /** The aspect's name in its colour, or a question mark until the player discovers it. */
    private static Component name(Aspect aspect, thaumcraft.common.lib.research.PlayerKnowledge knowledge) {
        return knowledge.hasDiscoveredAspect(aspect.getTag()) ? Component.literal(aspect.getName()).withColor(aspect.getColor())
                : Component.literal("?");
    }

    static boolean wearsGoggles(Player player) {
        var head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.getItem() instanceof IGoggles goggles && goggles.showIngamePopups(head, player);
    }

    private static ListTag write(AspectList aspects) {
        ListTag list = new ListTag();
        if (aspects == null) return list;
        for (Aspect aspect : aspects.getAspects()) {
            int amount = aspects.getAmount(aspect);
            if (aspect == null || amount <= 0) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString("Tag", aspect.getTag());
            entry.putInt("Amount", amount);
            list.add(entry);
        }
        return list;
    }

    private static AspectList read(ListTag list) {
        AspectList aspects = new AspectList();
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            Aspect aspect = Aspect.getAspect(entry.getString("Tag"));
            if (aspect != null && entry.getInt("Amount") > 0) aspects.add(aspect, entry.getInt("Amount"));
        }
        return aspects;
    }
}
