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
package thaumic.tinkerer.common.item.kami.foci;

import dev.architectury.networking.NetworkManager;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.network.packet.kami.PacketShadowBeam;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Wand Focus: Shadowbeam. Held, it fires a beam every tick that damages everything along it and mirrors off blocks; the
 * cost per tick is {@code aerVisCost}, {@code ordoVisCost} and {@code perditioVisCost} of the config, the damage
 * {@code baseDamage} (+20% per potency).
 */
public class ItemFocusShadowbeam extends ItemModKamiFocus {
    /** The beam is drawn for players within this many blocks. */
    private static final double SEE_DISTANCE = 64;

    public ItemFocusShadowbeam(Item.Properties properties) {
        super(properties);
    }

    @Override
    public String getSortingHelper(ItemStack focusstack) {
        return "TTKSH" + super.getSortingHelper(focusstack);
    }

    @Override
    public void onUsingFocusTick(ItemStack wandstack, Player player, int count) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer server)) return;
        int potency = WandHelper.getFocusPotency(wandstack);
        if (!WandHelper.consumeAllVis(wandstack, player, getVisCost(WandHelper.getFocusItem(wandstack)), true, false)) {
            return;
        }
        if (player.getRandom().nextInt(10) == 0) {
            KamiHooks.broadcastThaumcraftSound(player.level(), player, "brain", 0.5F, 1F);
        }
        List<Vec3> path = ShadowBeam.fire(server, potency, ConfigHandler.baseDamageShadowbeam);
        PacketShadowBeam packet = new PacketShadowBeam(path);
        NetworkManager.sendToPlayers(server.serverLevel().getPlayers(p -> p.distanceToSqr(server) < SEE_DISTANCE * SEE_DISTANCE),
                packet);
    }

    @Override
    public boolean isVisCostPerTick(ItemStack focusstack) {
        return true;
    }

    @Override
    protected boolean hasOrnament() {
        return true;
    }

    @Override
    protected boolean isKamiRarity() {
        return true;
    }

    @Override
    public int getFocusColor(ItemStack focusstack) {
        return 0x4B0053;
    }

    @Override
    public AspectList getVisCost(ItemStack focusstack) {
        return new AspectList().add(Aspect.ORDER, ConfigHandler.visShadowbeamOrder)
                .add(Aspect.ENTROPY, ConfigHandler.visShadowbeamEntropy).add(Aspect.AIR, ConfigHandler.visShadowbeamAir);
    }

    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focusstack, int rank) {
        return rank >= 1 && rank <= 5 ? new FocusUpgradeType[] { FocusUpgradeType.frugal, FocusUpgradeType.potency }
                : null;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_FOCUS_SHADOWBEAM, new ItemStack(this), 12,
                new AspectList().add(Aspect.DARKNESS, 65).add(Aspect.ELDRITCH, 32).add(Aspect.MAGIC, 50)
                        .add(Aspect.WEAPON, 32),
                KamiResearch.tc("itemFocusShock", 0), ItemKamiResource.stack(ItemKamiResource.ICHOR),
                new ItemStack(Items.ARROW), new ItemStack(Items.DIAMOND), KamiResearch.tc("itemFocusExcavation", 0),
                TTRegistry.stack(LibItemNames.FOCUS_DEFLECT), ItemKamiResource.stack(ItemKamiResource.ICHOR));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_FOCUS_SHADOWBEAM,
                new AspectList().add(Aspect.DARKNESS, 2).add(Aspect.MAGIC, 1).add(Aspect.ELDRITCH, 1)
                        .add(Aspect.TAINT, 1),
                14, 4, TTRegistry.stack(LibItemNames.FOCUS_SHADOWBEAM), 0,
                KamiResearch.of(LibResearch.KEY_ICHORCLOTH_ROD), KamiResearch.NONE, new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_FOCUS_SHADOWBEAM));
    }
}
