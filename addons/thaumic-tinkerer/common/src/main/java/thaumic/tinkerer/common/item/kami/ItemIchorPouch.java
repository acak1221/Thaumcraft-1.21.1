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

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.IFocusPouch;
import thaumic.tinkerer.common.block.tile.container.kami.ContainerIchorPouch;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Bottomless Pouch: a belt with 13 x 9 slots that opens on right click. 1.7.10 made it a focus pouch with a bigger
 * inventory ({@code ItemFocusPouch}, stack tag {@code Inventory}), so that the foci in it could be chosen by a wand.
 * Here it is an {@link IFocusPouch}, the API form of that class (the wand's focus selector reads the foci of any pouch
 * that implements it, 128 cells at most), and the slots are the {@link KamiComponents#POUCH_CONTENTS} component.
 */
public class ItemIchorPouch extends KamiItemBase implements IBauble, IFocusPouch {
    public static final int COLUMNS = 13;
    public static final int ROWS = 9;
    public static final int SIZE = COLUMNS * ROWS;

    /** The menu type of the pouch; set by the registrar. */
    public static RegistrySupplier<MenuType<ContainerIchorPouch>> MENU;

    public ItemIchorPouch(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    // ---- contents -------------------------------------------------------------------------------------------

    /** The 117 slots of the pouch, empty stacks where there is nothing; every stack is a copy. */
    public static ItemStack[] readContents(ItemStack pouch) {
        ItemStack[] contents = new ItemStack[SIZE];
        Arrays.fill(contents, ItemStack.EMPTY);
        ItemContainerContents stored = pouch.get(KamiComponents.POUCH_CONTENTS.get());
        if (stored != null) {
            int slot = 0;
            for (ItemStack stack : stored.stream().toList()) {
                if (slot >= SIZE) break;
                contents[slot++] = stack.copy();
            }
        }
        return contents;
    }

    /** Stores the 117 slots (copies of the given stacks). */
    public static void writeContents(ItemStack pouch, ItemStack[] contents) {
        pouch.set(KamiComponents.POUCH_CONTENTS.get(), ItemContainerContents.fromItems(Arrays.asList(contents)));
    }

    /** 1.7.10 {@code getInventory}, as {@link IFocusPouch} asks for it. */
    @Override
    public ItemStack[] getInventory(ItemStack pouch) {
        return readContents(pouch);
    }

    /** 1.7.10 {@code setInventory}, as {@link IFocusPouch} asks for it. */
    @Override
    public void setInventory(ItemStack pouch, ItemStack[] contents) {
        if (contents == null || contents.length != SIZE) {
            throw new IllegalArgumentException("the Bottomless Pouch has " + SIZE + " cells, not "
                    + (contents == null ? "none" : String.valueOf(contents.length)));
        }
        writeContents(pouch, contents);
    }

    /** 1.7.10 {@code GuiHandler.isDimensionAllowed}: the pouch does not open in the dimensions of {@code ConfigHandler.forbiddenDimensions}. */
    public static boolean isDimensionAllowed(Level level) {
        String id = level.dimension().location().toString();
        for (String forbidden : ConfigHandler.forbiddenDimensions) {
            if (id.equals(forbidden)) return false;
        }
        return true;
    }

    // ---- use ------------------------------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isDimensionAllowed(level)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            MenuRegistry.openExtendedMenu(server,
                    new SimpleMenuProvider((id, inventory, viewer) -> new ContainerIchorPouch(id, inventory, hand),
                            Component.translatable("container.thaumictinkerer.ichor_pouch")),
                    buf -> buf.writeBoolean(hand == InteractionHand.OFF_HAND));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // ---- bauble ---------------------------------------------------------------------------------------------

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.BELT;
    }

    /** The right click of this pouch opens it instead of putting it on ({@code IBauble.equipsOnUse}, API request A1). */
    @Override
    public boolean equipsOnUse(ItemStack stack, Player player) {
        return false;
    }

    // ---- research and recipe --------------------------------------------------------------------------------

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_ICHOR_POUCH, new ItemStack(this), 9,
                new AspectList().add(Aspect.VOID, 64).add(Aspect.MAN, 32).add(Aspect.CLOTH, 32)
                        .add(Aspect.ELDRITCH, 32).add(Aspect.AIR, 64),
                KamiResearch.tc("itemFocusPouch", 0), ItemKamiResource.stack(ItemKamiResource.ICHOR_CLOTH),
                KamiResearch.tc("itemFocusPortableHole", 0), new ItemStack(Items.DIAMOND),
                ItemKamiResource.stack(ItemKamiResource.ICHOR_CLOTH), KamiResearch.tcBlock("blockChestHungry", 0),
                KamiResearch.tcBlock("blockJar", 3));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_ICHOR_POUCH,
                new AspectList().add(Aspect.VOID, 2).add(Aspect.CLOTH, 1).add(Aspect.ELDRITCH, 1).add(Aspect.MAN, 1),
                13, 6, TTRegistry.stack(LibItemNames.ICHOR_POUCH), 0, KamiResearch.of(LibResearch.KEY_ICHOR_CLOTH),
                KamiResearch.NONE, new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_ICHOR_POUCH));
    }
}
