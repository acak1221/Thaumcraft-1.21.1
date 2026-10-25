package thaumcraft.common.lib.eldritch;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileEldritchAltar;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.api.nodes.NodeType;

/**
 * Original {@code WandManager.createOculus} (oracle
 * {@code thaumcraft/common/items/wands/WandManager.java:427-441}), the wand trigger
 * of the original event six ({@code ConfigRecipes.java:83}: the eldritch altar) that
 * opens the gate into the Outer Lands.
 *
 * <p>The original registration is {@code WandTriggerRegistry.registerWandBlockTrigger(
 * wandManager, 6, ConfigBlocks.blockEldritch, 0, "Thaumcraft")} and the original
 * dispatch is {@code WandManager.performTrigger} case six, which asks for the
 * complete {@code OCULUS} research before it calls this method.</p>
 *
 * <p>Every condition of the original is kept:</p>
 * <ul>
 *   <li>the clicked block is the eldritch altar, its tile carries exactly four eyes
 *       and the altar is not open yet;</li>
 *   <li>the block directly above the altar is an aura node of the original dark type
 *       ({@code TileNode.getNodeType() == NodeType.DARK});</li>
 *   <li>a labyrinth already covers the altar ({@code TileEldritchAltar.checkForMaze()}
 *       returns true; the call also creates the labyrinth when there is none, exactly
 *       like the original, which then answers false and refuses the gate);</li>
 *   <li>the wand pays one hundred whole vis of every primal aspect
 *       ({@code Aspect.AIR/FIRE/EARTH/WATER/ORDER/ENTROPY}, oracle
 *       {@code WandManager.java:434-435}), which the native pricing turns into
 *       centivis with the original consumption modifier;</li>
 *   <li>the gate replaces the node, the altar is marked open and the wand sound plays.</li>
 * </ul>
 */
public final class OculusFormation {
    private OculusFormation() {}

    /** Original WandManager.java:434-435 - one hundred whole vis of every primal aspect. */
    public static Map<String, Integer> oculusVisCost() {
        Map<String, Integer> vis = new LinkedHashMap<>();
        for (String primal : WandData.PRIMALS) vis.put(primal, 100);
        return Map.copyOf(vis);
    }

    /**
     * The native wand click on the eldritch altar.
     *
     * <p>Forced 1.21.1 change: the original answered the click through the block
     * metadata of the wand trigger registry, which no longer exists. The native port
     * answers the eldritch altar block itself.</p>
     */
    public static InteractionResult tryWandInteraction(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        ItemStack held = context.getItemInHand();
        if (player == null || player.isSpectator() || !player.isAlive() || held.isEmpty()
                || !(held.getItem() instanceof ItemWandCasting) || !level.hasChunkAt(pos)) return InteractionResult.PASS;
        BlockState state = level.getBlockState(pos);
        if (!state.is(ThaumcraftEldritchBlocks.ELDRITCH.get())) return InteractionResult.PASS;
        if (thaumcraft.common.blocks.BlockEldritch.variant(state) != EldritchBlockRules.ELDRITCH_ALTAR) return InteractionResult.PASS;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), held)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Oculus interaction requires the server thread");
        if (!(player instanceof ServerPlayer actor)) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof TileEldritchAltar altar)) return InteractionResult.PASS;
        // Original WandManager.java:432 - four eyes and an altar that is not open yet.
        if (altar.getEyes() != EldritchBlockRules.ALTAR_MAX_EYES || altar.isOpen()) return InteractionResult.PASS;
        // Original WandManager.java:432 - the node above the altar is a dark node.
        BlockPos above = pos.above();
        if (!level.hasChunkAt(above)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(above) instanceof TileNode node)) return InteractionResult.PASS;
        if (node.getNodeType() != NodeType.DARK) return InteractionResult.PASS;
        // Original WandManager.java:432 - the OCULUS research, checked before the vis is paid.
        if (!KnowledgeAccess.get(actor).isResearchComplete("OCULUS")) return InteractionResult.FAIL;
        // Original WandManager.java:432 - a labyrinth has to cover the altar. The call
        // creates one when there is none, and the original then refuses the gate.
        if (!altar.checkForMaze()) return InteractionResult.FAIL;
        Map<String, Integer> price = ArcaneVis.price(oculusVisCost(), held, actor);
        ItemStack paid = held.copy();
        if (!ArcaneVis.debit(paid, price)) return InteractionResult.FAIL;
        // Replace directly: removing the node first destroys its block entity even
        // when the following placement is refused. Retain its complete data for a
        // partial native replacement, rather than recreating a default aura node.
        BlockState originalNodeState = node.getBlockState();
        CompoundTag originalNodeData = node.saveWithFullMetadata(server.registryAccess());
        BlockState portal = ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get().defaultBlockState();
        if (!server.setBlock(above, portal, Block.UPDATE_ALL) || server.getBlockState(above) != portal) {
            restoreNode(server, above, node, originalNodeState, originalNodeData, portal);
            return InteractionResult.FAIL;
        }
        held.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
        altar.setOpen(true);
        altar.setChanged();
        server.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        server.playSound(null, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D,
                SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand")), SoundSource.BLOCKS, 1, 1);
        return InteractionResult.CONSUME;
    }

    private static void restoreNode(ServerLevel level, BlockPos pos, TileNode original, BlockState state,
                                    CompoundTag data, BlockState portal) {
        if (level.getBlockEntity(pos) == original) return;
        BlockState current = level.getBlockState(pos);
        // A notification may deliberately install another owner; do not erase it.
        if (current != portal && !current.isAir()) return;
        if (!level.setBlock(pos, state, Block.UPDATE_KNOWN_SHAPE)) return;
        if (level.getBlockEntity(pos) instanceof TileNode restored) {
            restored.loadWithComponents(data, level.registryAccess());
            restored.setChanged();
            level.sendBlockUpdated(pos, current, state, Block.UPDATE_CLIENTS);
        }
    }
}
