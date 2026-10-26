package thaumcraft.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileAlchemyFurnaceAdvanced;
import thaumcraft.common.tiles.TileArcaneBore;
import thaumcraft.common.tiles.TileArcaneFurnace;
import thaumcraft.common.tiles.TileCentrifuge;
import thaumcraft.common.tiles.TileCrucible;
import thaumcraft.common.tiles.TileEssentiaCrystalizer;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJarBrain;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileThaumatorium;

/**
 * What a Thaumcraft machine is doing: the physical state anyone can see by looking at it (water,
 * boiling, fuel, progress, the infusion in hand), not the essentia, which stays behind the goggles.
 * Progress counters live on the server only, so the server sends them; the rest is read from the
 * synchronized block entity. A node's kind is shown only to the eyes that can see nodes: the
 * goggles of revealing or a thaumometer in hand, as the node renderer decides.
 */
enum MachineProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "machine");
    /** Original TileCrucible: the crucible boils, and dissolves items, above heat 150. */
    private static final int BOILING = 150;
    /** Original TileEssentiaCrystalizer: a crystal grows in 200 ticks. */
    private static final int CRYSTAL_TICKS = 200;
    /** Original TileCentrifuge: one essentia splits in 40 ticks, counting down from 39. */
    private static final int CENTRIFUGE_TICKS = 40;

    @Override public ResourceLocation getUid() { return UID; }

    @Override public boolean shouldRequestData(BlockAccessor accessor) {
        BlockEntity entity = accessor.getBlockEntity();
        return entity instanceof TileCrucible || entity instanceof TileAlchemyFurnace || entity instanceof TileAlchemyFurnaceAdvanced
                || entity instanceof TileArcaneFurnace || entity instanceof TileInfusionMatrix || entity instanceof TileCentrifuge
                || entity instanceof TileEssentiaCrystalizer || entity instanceof TileThaumatorium;
    }

    @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity entity = accessor.getBlockEntity();
        if (entity instanceof TileCrucible crucible) {
            data.putInt("Heat", crucible.getHeat());
            data.putInt("Water", crucible.getWaterAmount());
        } else if (entity instanceof TileAlchemyFurnace furnace) {
            data.putInt("Burn", furnace.getBurnTime());
            data.putInt("Cook", furnace.getCookTime());
            data.putInt("Smelt", furnace.getSmeltTime());
            data.putInt("Vis", furnace.getVis());
            data.putInt("MaxVis", furnace.getMaxVis());
        } else if (entity instanceof TileAlchemyFurnaceAdvanced furnace) {
            data.putInt("Vis", furnace.getVis());
            data.putInt("MaxVis", furnace.getMaxVis());
            data.putInt("Heat", furnace.getHeat());
        } else if (entity instanceof TileArcaneFurnace furnace) {
            data.putInt("Cook", furnace.furnaceCookTime);
            data.putInt("Speedy", furnace.speedyTime);
        } else if (entity instanceof TileInfusionMatrix matrix && matrix.isCrafting()) {
            data.putBoolean("Crafting", true);
            data.putInt("Essentia", matrix.getDemand().visSize());
            data.putInt("Items", matrix.getRemainingIngredientCount());
            // The output has no getter; the saved recipe carries it, as the client update tag does.
            CompoundTag saved = matrix.saveWithoutMetadata(accessor.getLevel().registryAccess());
            if (saved.getCompound("recipeout").contains("Stack", Tag.TAG_COMPOUND)) {
                data.put("Output", saved.getCompound("recipeout").getCompound("Stack"));
            }
        } else if (entity instanceof TileCentrifuge centrifuge) {
            data.putInt("Process", centrifuge.getProcess());
        } else if (entity instanceof TileEssentiaCrystalizer crystalizer) {
            data.putInt("Progress", crystalizer.getProgress());
        } else if (entity instanceof TileThaumatorium thaumatorium) {
            data.putBoolean("Heated", thaumatorium.isHeated());
        }
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity entity = accessor.getBlockEntity();
        CompoundTag data = accessor.getServerData();
        if (entity instanceof TileCrucible crucible) {
            int water = data.contains("Water") ? data.getInt("Water") : crucible.getWaterAmount();
            int heat = data.contains("Heat") ? data.getInt("Heat") : crucible.getHeat();
            // Jade's own fluid view already shows the water; only the boiling is Thaumcraft's.
            if (water > 0) tooltip.add(Component.translatable(heat > BOILING ? "tc.port.jade.boiling" : "tc.port.jade.not_boiling")
                    .withStyle(heat > BOILING ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        } else if (entity instanceof TileAlchemyFurnace) {
            if (!data.contains("Vis")) return;
            tooltip.add(gray("tc.port.jade.vis", data.getInt("Vis"), data.getInt("MaxVis")));
            if (data.getInt("Burn") > 0) tooltip.add(gray("tc.port.jade.burning", seconds(data.getInt("Burn"))));
            if (data.getInt("Cook") > 0 && data.getInt("Smelt") > 0) {
                tooltip.add(gray("tc.port.jade.progress", percent(data.getInt("Cook"), data.getInt("Smelt"))));
            }
        } else if (entity instanceof TileAlchemyFurnaceAdvanced furnace) {
            int vis = data.contains("Vis") ? data.getInt("Vis") : furnace.getVis();
            tooltip.add(gray("tc.port.jade.vis", vis, furnace.getMaxVis()));
            tooltip.add(gray("tc.port.jade.heat", data.contains("Heat") ? data.getInt("Heat") : furnace.getHeat()));
        } else if (entity instanceof TileArcaneFurnace) {
            if (data.getInt("Cook") > 0) tooltip.add(gray("tc.port.jade.cooking", seconds(data.getInt("Cook"))));
            if (data.getInt("Speedy") > 0) tooltip.add(Component.translatable("tc.port.jade.boosted", seconds(data.getInt("Speedy")))
                    .withStyle(ChatFormatting.GOLD));
        } else if (entity instanceof TileInfusionMatrix) {
            if (!data.getBoolean("Crafting")) return;
            ItemStack output = data.contains("Output") ? ItemStack.parseOptional(accessor.getLevel().registryAccess(), data.getCompound("Output"))
                    : ItemStack.EMPTY;
            tooltip.add(Component.translatable("tc.port.jade.infusing", output.isEmpty() ? Component.literal("?") : output.getHoverName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(gray("tc.port.jade.infusion_left", data.getInt("Items"), data.getInt("Essentia")));
        } else if (entity instanceof TileCentrifuge centrifuge) {
            if (centrifuge.getInputAspect() != null && data.getInt("Process") > 0) {
                tooltip.add(gray("tc.port.jade.progress", percent(CENTRIFUGE_TICKS - data.getInt("Process"), CENTRIFUGE_TICKS)));
            }
        } else if (entity instanceof TileEssentiaCrystalizer) {
            if (data.getInt("Progress") > 0) tooltip.add(gray("tc.port.jade.progress", percent(data.getInt("Progress"), CRYSTAL_TICKS)));
        } else if (entity instanceof TileThaumatorium thaumatorium) {
            tooltip.add(gray("tc.port.jade.recipes", thaumatorium.recipeCount(), thaumatorium.recipeCapacity()));
            if (data.contains("Heated")) tooltip.add(Component.translatable(data.getBoolean("Heated") ? "tc.port.jade.heated" : "tc.port.jade.not_heated")
                    .withStyle(data.getBoolean("Heated") ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        } else if (entity instanceof TileJarBrain brain) {
            tooltip.add(gray("tc.port.jade.experience", brain.getXp(), brain.getXpMax()));
        } else if (entity instanceof TileArcaneBore bore) {
            tooltip.add(Component.translatable(bore.isPowered() ? "tc.port.jade.bore_active" : "tc.port.jade.bore_idle")
                    .withStyle(bore.isPowered() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            int width = bore.stats().width();
            tooltip.add(gray("tc.port.jade.bore_area", width, width));
            ItemStack focus = bore.getItem(0);
            if (!focus.isEmpty()) tooltip.add(gray("tc.port.jade.focus", focus.getHoverName()));
        } else if (entity instanceof TileNode node && seesNodes(accessor.getPlayer())) {
            Component kind = Component.translatable("nodetype." + node.getNodeType().name() + ".name");
            if (node.getNodeModifier() != null) {
                kind = Component.translatable("nodemod." + node.getNodeModifier().name() + ".name").append(" ").append(kind);
            }
            tooltip.add(Component.translatable("tc.port.jade.node", kind).withStyle(ChatFormatting.AQUA));
        }
    }

    /** The node renderer's eyes: a revealing helmet, or a thaumometer in the main hand. */
    private static boolean seesNodes(Player player) {
        var head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.getItem() instanceof IRevealer revealer && revealer.showNodes(head, player)
                || player.getMainHandItem().getItem() instanceof ItemThaumometer;
    }

    private static Component gray(String key, Object... arguments) {
        return Component.translatable(key, arguments).withStyle(ChatFormatting.GRAY);
    }

    private static String seconds(int ticks) { return Integer.toString(Math.max(1, (ticks + 19) / 20)); }

    private static int percent(int done, int total) { return Math.max(0, Math.min(100, done * 100 / Math.max(1, total))); }
}
