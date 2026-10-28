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
package thaumic.tinkerer.common.item.kami;

import dev.architectury.event.EventResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.item.kami.tool.IAdvancedTool;
import thaumic.tinkerer.common.item.kami.tool.ToolHandler;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Protoclay: with it anywhere in the inventory, starting to break a block with an awakened tool of the wrong kind swaps
 * the right awakened tool from the inventory into the hand (and cancels that click).
 *
 * <p>1.7.10 listened to every {@code PlayerInteractEvent}, but the action only counted while the player's arm was
 * swinging, which in practice is the left click on a block; the port listens to that click
 * ({@code InteractionEvent.LEFT_CLICK_BLOCK}), on the server, which swaps the stacks and lets the client's mining restart
 * with the new tool. As in the original the click is cancelled whenever the kind of the held tool differs from the one the
 * block asks for, also when the inventory has no tool of that kind.</p>
 */
public class ItemProtoclay extends KamiItemBase {

    public ItemProtoclay(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Architectury {@code InteractionEvent.LEFT_CLICK_BLOCK}. */
    public static EventResult onLeftClickBlock(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        if (player.level().isClientSide || hand != InteractionHand.MAIN_HAND || !hasProtoclay(player)) {
            return EventResult.pass();
        }
        ItemStack currentStack = player.getMainHandItem();
        if (!(currentStack.getItem() instanceof IAdvancedTool tool) || tool.getType().equals("sword")) {
            return EventResult.pass();
        }
        BlockState state = player.level().getBlockState(pos);
        String typeToFind = ToolHandler.requiredToolType(state);
        if (tool.getType().equals(typeToFind) || typeToFind.isEmpty()) return EventResult.pass();

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stackInSlot = inventory.getItem(i);
            if (stackInSlot != currentStack && stackInSlot.getItem() instanceof IAdvancedTool toolInSlot
                    && toolInSlot.getType().equals(typeToFind)) {
                inventory.setItem(inventory.selected, stackInSlot);
                inventory.setItem(i, currentStack);
                break;
            }
        }
        return EventResult.interruptFalse();
    }

    private static boolean hasProtoclay(Player player) {
        Item protoclay = TTRegistry.item(LibItemNames.PROTOCLAY);
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(protoclay)) return true;
        }
        return false;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_PROTOCLAY, new ItemStack(this), 4,
                new AspectList().add(Aspect.MINE, 16).add(Aspect.TOOL, 16), new ItemStack(Items.CLAY_BALL),
                new ItemStack(Items.DIRT), new ItemStack(Items.STONE), new ItemStack(Items.OAK_LOG),
                ItemKamiResource.stack(ItemKamiResource.ENDER_SHARD));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_PROTOCLAY,
                new AspectList().add(Aspect.TOOL, 2).add(Aspect.MINE, 1).add(Aspect.MAN, 1).add(Aspect.MECHANISM, 1),
                12, 17, TTRegistry.stack(LibItemNames.PROTOCLAY), 0, KamiResearch.of(LibResearch.KEY_ICHOR_PICK_GEM),
                KamiResearch.of(LibResearch.KEY_ICHOR_SHOVEL_GEM), new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_PROTOCLAY));
    }
}
