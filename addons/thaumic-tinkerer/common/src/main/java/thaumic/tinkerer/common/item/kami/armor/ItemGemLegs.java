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
package thaumic.tinkerer.common.item.kami.armor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Leggings of the Burning Mantle: fire resistance, and burning heals the wearer; optionally (config
 * {@code kamiLeggingsLightPlacing}, off by default) a light of gaseous nitor around and in front of the wearer.
 */
public class ItemGemLegs extends ItemIchorclothArmorAdv {

    public ItemGemLegs(Item.Properties properties) {
        super(ArmorItem.Type.LEGGINGS, properties);
    }

    /** 1.7.10 {@code tickPlayer}, run every five ticks while the leggings are worn. */
    public static void tickPlayer(Player player, ItemStack armor) {
        if (player.level().isClientSide || player.tickCount % 5 != 0) return;
        if (!isActive(player, armor)) return;

        if (player.isOnFire()) player.heal(0.5F);
        ensureEffect(player, MobEffects.FIRE_RESISTANCE, 119, 0);
        if (ConfigHandler.shouldKamiLegsDoLightFeature) setNearBrightNitor(player);
    }

    private static void setNearBrightNitor(Player player) {
        Level level = player.level();
        int x = Mth.floor(player.getX());
        int y = Mth.floor(player.getY()) + 1;
        int z = Mth.floor(player.getZ());

        // ItemBrightNitor.onUpdate with meta 1: the bright gas on the player; the line after it uses the plain gas (the
        // 1.7.10 code had put the meta back to 0 before the loop).
        setNitor(level, new BlockPos(x, y, z), true);

        double yaw = Mth.wrapDegrees(player.getYRot() + 90F) * Math.PI / 180F;
        double lookX = Math.cos(yaw);
        double lookZ = Math.sin(yaw);
        double newX = lookX;
        double newZ = lookZ;
        for (int i = 0; i < 5; i++) {
            newX += lookX;
            newZ += lookZ;
            setNitor(level, new BlockPos(x + (int) newX, y, z + (int) newZ), false);
        }
    }

    /**
     * 1.7.10 {@code ItemBrightNitor.setBlock}: gaseous nitor into air or into nitor. The "bright" variant is metadata 1 of
     * the nitor gas block; the SimpleBlocks group represents it by a state property called {@code bright} (boolean) or
     * {@code meta}/{@code variant} (integer); without such a property the plain gas is placed.
     */
    private static void setNitor(Level level, BlockPos pos, boolean bright) {
        if (level.isClientSide || !(level instanceof ServerLevel)) return;
        Block gas = BuiltInRegistries.BLOCK.getOptional(LibMisc.id(LibBlockNames.NITOR_GAS)).orElse(null);
        if (gas == null) return;
        BlockState current = level.getBlockState(pos);
        if (!level.isEmptyBlock(pos) && !current.is(gas)) return;
        BlockState state = gas.defaultBlockState();
        if (bright) {
            for (Property<?> property : gas.getStateDefinition().getProperties()) {
                if (property instanceof BooleanProperty flag && flag.getName().equals("bright")) {
                    state = state.setValue(flag, true);
                } else if (property instanceof IntegerProperty number
                        && (number.getName().equals("meta") || number.getName().equals("variant"))
                        && number.getPossibleValues().contains(1)) {
                    state = state.setValue(number, 1);
                }
            }
        }
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_ICHORCLOTH_LEGS_GEM, new ItemStack(this), 13,
                new AspectList().add(Aspect.FIRE, 50).add(Aspect.ARMOR, 32).add(Aspect.HEAL, 32).add(Aspect.ENERGY, 32)
                        .add(Aspect.LIGHT, 64).add(Aspect.GREED, 16).add(Aspect.ELDRITCH, 16),
                TTRegistry.stack(LibItemNames.ICHOR_LEGS), new ItemStack(Items.DIAMOND),
                ItemKamiResource.stack(ItemKamiResource.ICHOR), ItemKamiResource.stack(ItemKamiResource.ICHOR),
                KamiResearch.tc("itemFocusPrimal", 0), KamiResearch.tc("itemThaumonomicon", 0),
                new ItemStack(Items.GOLDEN_LEGGINGS),
                PotionContents.createItemStack(Items.POTION, Potions.FIRE_RESISTANCE),
                TTRegistry.stack(LibItemNames.FOCUS_SMELT), TTRegistry.stack(LibItemNames.BRIGHT_NTIOR),
                new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.FIRE_CHARGE), new ItemStack(Items.BLAZE_ROD));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_ICHORCLOTH_LEGS_GEM,
                new AspectList().add(Aspect.FIRE, 2).add(Aspect.HEAL, 1).add(Aspect.GREED, 1).add(Aspect.ENERGY, 1), 17,
                9, TTRegistry.stack(LibItemNames.ICHOR_LEGS_GEM), 0, KamiResearch.of(LibResearch.KEY_ICHORCLOTH_ARMOR),
                KamiResearch.NONE, new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_ICHORCLOTH_LEGS_GEM), new ResearchPage("1"));
    }
}
