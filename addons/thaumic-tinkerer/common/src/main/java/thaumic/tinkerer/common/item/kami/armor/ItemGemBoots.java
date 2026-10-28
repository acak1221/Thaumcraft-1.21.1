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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Boots of the Horizontal Shield: speed, jump height (+0.3), a step height of one block that sneaking lowers to a
 * half, Haste II, dirt turning into grass underfoot, and no fall damage.
 *
 * <p>1.7.10 set {@code stepHeight} and {@code motionY} directly, in an {@code onArmorTick} that did not look at the switch
 * and a {@code LivingUpdateEvent} that did. Here step height and jump strength are attribute modifiers that the server sets
 * (the game sends them), and the walking boost, which the 1.21.1 server cannot give to a client-driven player, is added to
 * the local player's motion on the client. The fall check of the original ({@code LivingFallEvent}: no fall distance for
 * every wearer, switched off or not) is the zero fall distance of every tick.</p>
 */
public class ItemGemBoots extends ItemIchorclothArmorAdv {
    /** 1.7.10 {@code speedBonus}. */
    public static final float SPEED_BONUS = 0.075F;

    private static final ResourceLocation STEP_ID = LibMisc.id("kami_boots_step");
    private static final ResourceLocation JUMP_ID = LibMisc.id("kami_boots_jump");

    public ItemGemBoots(Item.Properties properties) {
        super(ArmorItem.Type.BOOTS, properties);
    }

    /** Runs for every player on every tick; {@code boots} is the stack in the player's feet slot. */
    public static void tick(Player player, ItemStack boots) {
        boolean worn = boots.getItem() instanceof ItemGemBoots;
        Level level = player.level();
        if (!level.isClientSide) updateAttributes(player, boots, worn);
        if (!worn) return;

        // LivingFallEvent: event.distance = 0 for everyone who wears the boots.
        player.fallDistance = 0F;
        boolean active = isActive(player, boots);

        if (!level.isClientSide && active) {
            MobEffectsHelper.haste(player);
            if (ConfigHandler.shouldKamiBootsDoGrass) {
                BlockPos below = new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()) - 1, Mth.floor(player.getZ()));
                if (level.getBlockState(below).is(Blocks.DIRT)) {
                    level.setBlock(below, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                }
            }
        }
        if (level.isClientSide && player.isLocalPlayer()) moveLocalPlayer(player, active);
    }

    /** The server's part: step height and jump strength. */
    private static void updateAttributes(Player player, ItemStack boots, boolean worn) {
        boolean active = worn && isActive(player, boots);
        boolean moving = player.xxa != 0F || player.zza != 0F;
        double step = 0;
        if (active) step = player.isShiftKeyDown() ? -0.1 : 0.4;
        else if (worn && moving && !player.isShiftKeyDown()) step = 0.4;
        setModifier(player.getAttribute(Attributes.STEP_HEIGHT), STEP_ID, step);
        setModifier(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_ID, active ? 0.3 : 0);
    }

    private static void setModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(id);
        if (amount == 0) {
            if (current != null) attribute.removeModifier(id);
        } else if (current == null || current.amount() != amount) {
            attribute.removeModifier(id);
            attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** The client's part: the push of 1.7.10's {@code moveFlying} calls and the larger air control of {@code jumpMovementFactor}. */
    private static void moveLocalPlayer(Player player, boolean active) {
        boolean flying = player.getAbilities().flying;

        // onArmorTick: not looking at the switch.
        if (player.xxa != 0F || player.zza != 0F) {
            if (player.onGround() || player.onClimbable() || flying) {
                float bonus = SPEED_BONUS;
                if (player.isInWater()) bonus /= 4.0F;
                if (player.isShiftKeyDown()) bonus /= 2.0F;
                if (player.zza > 0F) player.moveRelative(bonus, new Vec3(0, 0, player.zza));
            }
        }
        if (!active) return;

        // tickPlayer: moveFlying(0, speedBonus, 0.15 / 0.075) and jumpMovementFactor 0.04 / 0.05.
        if ((player.onGround() || flying) && player.zza > 0F) {
            player.moveRelative(flying ? 0.075F : 0.15F, new Vec3(0, 0, SPEED_BONUS));
        }
        if (!player.onGround() && !flying && !player.isInWater() && !player.isInLava() && !player.onClimbable()) {
            // 1.21.1 air control is 0.02 (0.026 sprinting); the boots made it 0.04 (0.05): the difference is added.
            float extra = player.isSprinting() ? 0.05F - 0.026F : 0.04F - 0.02F;
            player.moveRelative(extra, new Vec3(player.xxa, 0, player.zza));
        }
    }

    /** Haste II for two ticks, refreshed every tick. */
    private static final class MobEffectsHelper {
        static void haste(Player player) {
            var current = player.getEffect(MobEffects.DIG_SPEED);
            if (current == null || current.getDuration() < 2 || current.getAmplifier() < 1) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.DIG_SPEED, 2, 1, true, true));
            }
        }
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_ICHORCLOTH_BOOTS_GEM, new ItemStack(this), 13,
                new AspectList().add(Aspect.EARTH, 50).add(Aspect.ARMOR, 32).add(Aspect.MINE, 32).add(Aspect.MOTION, 32)
                        .add(Aspect.LIGHT, 64).add(Aspect.PLANT, 16).add(Aspect.TRAVEL, 16),
                TTRegistry.stack(LibItemNames.ICHOR_BOOTS), new ItemStack(Items.DIAMOND),
                ItemKamiResource.stack(ItemKamiResource.ICHOR), ItemKamiResource.stack(ItemKamiResource.ICHOR),
                KamiResearch.tc("itemThaumonomicon", 0), KamiResearch.tc("itemFocusPrimal", 0),
                new ItemStack(Items.GOLDEN_BOOTS), new ItemStack(Items.GRASS_BLOCK),
                KamiResearch.tcBlock("blockWoodenDevice", 5), KamiResearch.tcBlock("blockMetalDevice", 8),
                new ItemStack(Items.WHEAT_SEEDS), new ItemStack(Items.WHITE_WOOL), new ItemStack(Items.LEAD));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_ICHORCLOTH_BOOTS_GEM,
                new AspectList().add(Aspect.EARTH, 2).add(Aspect.TRAVEL, 1).add(Aspect.MINE, 1).add(Aspect.PLANT, 1), 15,
                10, TTRegistry.stack(LibItemNames.ICHOR_BOOTS_GEM), 0, KamiResearch.of(LibResearch.KEY_ICHORCLOTH_ARMOR),
                KamiResearch.NONE, new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_ICHORCLOTH_BOOTS_GEM));
    }
}
