/**
 * Port of Thaumic Tinkerer's {@code ItemShareBook} (a class without a licence header in the GTNewHorizons fork of
 * 1.7.10). Thaumic Tinkerer is distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 License
 * (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB); it is a Derivative Work on Thaumcraft 4.
 * Thaumcraft 4 (c) Azanor 2012 (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.item.BasicItemComponents.ShareData;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererCraftingBenchRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Tome of Knowledge Sharing. The first use binds the tome to the player and writes that player's completed research and
 * discovered aspects into it; every later use gives the user the research and the aspects of the bound player: the latest
 * ones when that player is online, the written snapshot otherwise.
 *
 * <p>The binding and the snapshot are the component {@link BasicItemComponents#SHARE_TOME} (1.7.10: the tags {@code player},
 * {@code research} and {@code aspects}; an unassigned tome is a stack without the component, 1.7.10 wrote {@code [none]}).
 * Scan clues ({@code @KEY} entries of the completed research) are not handed over: the API does not let an add-on set them
 * (see {@code docs/api-requests/BasicItems.md}).</p>
 */
public class ItemShareBook extends Item implements ITTinkererRegisterable {
    /** The research page of the recipe, and the file {@code data/thaumictinkerer/recipe/share_tome.json}. */
    public static final String RECIPE_NAME = "share_tome";

    public ItemShareBook(Item.Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        IRegisterableResearch research = (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_SHARE_TOME,
                new AspectList(),
                0,
                -1,
                0,
                new ItemStack(this)).setStub().setAutoUnlock().setRound();
        if (ConfigHandler.enableSurvivalShareTome) ((TTResearchItem) research)
                .setPages(new ResearchPage("0"), ResearchHelper.recipePage(RECIPE_NAME));
        else ((TTResearchItem) research).setPages(new ResearchPage("0"));
        return research;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        // " S ", "PTP", " P " with the scribing tools, the Thaumonomicon and paper: the file share_tome.json
        if (ConfigHandler.enableSurvivalShareTome) return new ThaumicTinkererCraftingBenchRecipe(RECIPE_NAME);
        return null;
    }

    // ---- the binding ---------------------------------------------------------------------------------------------

    /** The binding of the tome, or null for an unassigned one. */
    public static ShareData getData(ItemStack stack) {
        return stack.get(BasicItemComponents.SHARE_TOME.get());
    }

    /** The name of the bound player, or null for an unassigned tome. */
    public static String getPlayerName(ItemStack stack) {
        ShareData data = getData(stack);
        return data == null ? null : data.player();
    }

    /** What the player has researched and discovered, as the tome stores it. */
    public static ShareData snapshot(Player player) {
        List<String> research = new ArrayList<>(ThaumcraftApiHelper.getCompletedResearch(player));
        List<String> aspects = new ArrayList<>();
        AspectList discovered = ThaumcraftApiHelper.getDiscoveredAspects(player);
        if (discovered != null) {
            // Save all discovered aspect types.
            discovered.aspects.keySet().stream().map(Aspect::getTag).sorted().forEach(aspects::add);
        }
        return new ShareData(player.getGameProfile().getName(), player.getUUID(), research, aspects);
    }

    /** Hands the research and the aspects of the snapshot to the player; returns how many research entries were new. */
    public static int load(ShareData data, ServerPlayer player) {
        int completed = 0;
        for (String research : data.research()) {
            boolean had = ThaumcraftApiHelper.isResearchComplete(player, research);
            ThaumcraftApiHelper.completeResearch(player, research);
            if (!had && ThaumcraftApiHelper.isResearchComplete(player, research)) completed++;
        }
        for (String tag : data.aspects()) {
            Aspect aspect = Aspect.getAspect(tag);
            if (aspect != null) ThaumcraftApiHelper.discoverAspect(player, aspect);
        }
        return completed;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer user)) {
            return InteractionResultHolder.sidedSuccess(book, level.isClientSide);
        }

        ShareData data = getData(book);
        if (data == null) {
            book.set(BasicItemComponents.SHARE_TOME.get(), snapshot(user));
            user.sendSystemMessage(Component.translatable("ttmisc.shareTome.write"));
        } else {
            // Try to load latest research for the saved player, fall back to the stored snapshot if that player is not here.
            MinecraftServer server = user.getServer();
            ServerPlayer owner = server == null ? null : server.getPlayerList().getPlayer(data.owner());
            load(owner != null ? snapshot(owner) : data, user);
            user.sendSystemMessage(Component.translatable("ttmisc.shareTome.sync"));
        }

        return InteractionResultHolder.sidedSuccess(book, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String name = getPlayerName(stack);
        tooltip.add(name == null ? Component.translatable("ttmisc.shareTome.noAssign")
                : Component.translatable("ttmisc.shareTome.playerName", name));
    }
}
