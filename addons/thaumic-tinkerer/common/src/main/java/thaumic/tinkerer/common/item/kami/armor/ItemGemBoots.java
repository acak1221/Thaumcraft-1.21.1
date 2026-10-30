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

import dev.architectury.event.EventResult;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
import thaumic.tinkerer.common.hooks.TinkererEvents;
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
 * and a {@code LivingUpdateEvent} that did. Here the step height is an attribute modifier that the server sets (the game sends
 * it), and the walking boost, which the 1.21.1 server cannot give to a client-driven player, is added to the local player's
 * motion on the client. The two handlers of the original's {@code EventHandler} are listeners of the loader events
 * ({@code docs/HOOKS.md}): {@link #onLivingJump} is {@code onPlayerJump} ({@code motionY += 0.3} for a wearer whose boots are
 * switched on, after the jump), {@link #onLivingFall} is {@code onFall} (no fall distance for every wearer, switched off or
 * not). Both run on both sides and change a result that both sides compute; neither touches the world.</p>
 */
public class ItemGemBoots extends ItemIchorclothArmorAdv {
    /** 1.7.10 {@code speedBonus}. */
    public static final float SPEED_BONUS = 0.075F;

    /** 1.7.10 {@code onPlayerJump}: {@code player.motionY += 0.3 * getJumpModifier(boots)}; the modifier (Thaumic Boots) is 1. */
    public static final double JUMP_BONUS = 0.3;

    private static final ResourceLocation STEP_ID = LibMisc.id("kami_boots_step");

    public ItemGemBoots(Item.Properties properties) {
        super(ArmorItem.Type.BOOTS, properties);
    }

    /** Runs for every player on every tick; {@code boots} is the stack in the player's feet slot. */
    public static void tick(Player player, ItemStack boots) {
        boolean worn = boots.getItem() instanceof ItemGemBoots;
        Level level = player.level();
        if (!level.isClientSide) updateAttributes(player, boots, worn);
        if (!worn) return;

        boolean active = isActive(player, boots);
        // 1.7.10 tickPlayer, while the boots are on: player.fallDistance = 0 (the wearer of switched-off boots is spared by onLivingFall)
        if (active) player.fallDistance = 0F;

        if (!level.isClientSide && active) {
            haste(player);
            if (ConfigHandler.shouldKamiBootsDoGrass) {
                BlockPos below = new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()) - 1, Mth.floor(player.getZ()));
                if (level.getBlockState(below).is(Blocks.DIRT)) {
                    level.setBlock(below, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                }
            }
        }
        if (level.isClientSide && player.isLocalPlayer()) moveLocalPlayer(player, active);
    }

    /** The server's part: the step height. (The jump is {@link #onLivingJump}; it used to be an attribute modifier as well.) */
    private static void updateAttributes(Player player, ItemStack boots, boolean worn) {
        boolean active = worn && isActive(player, boots);
        boolean moving = player.xxa != 0F || player.zza != 0F;
        double step = 0;
        if (active) step = player.isShiftKeyDown() ? -0.1 : 0.4;
        else if (worn && moving && !player.isShiftKeyDown()) step = 0.4;
        setModifier(player.getAttribute(Attributes.STEP_HEIGHT), STEP_ID, step);
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

    /** The boots on the feet of {@code entity}, or {@link ItemStack#EMPTY} when it is not a player or wears other boots. */
    private static ItemStack bootsOf(LivingEntity entity) {
        if (!(entity instanceof Player player)) return ItemStack.EMPTY;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        return boots.getItem() instanceof ItemGemBoots ? boots : ItemStack.EMPTY;
    }

    /**
     * Listener of {@code TinkererEvents.LIVING_JUMP} (1.7.10 {@code EventHandler.onPlayerJump}): a player whose Gem Boots are
     * worn, with the master switch and this piece switched on, rises {@link #JUMP_BONUS} higher than the jump alone. It adds
     * to the vertical motion that {@code jumpFromGround} has applied, as {@code motionY += 0.3} did, so the block's jump
     * factor (honey) and Jump Boost act on the jump only. Runs on both sides: the client's own jump and the server's
     * ({@code KamiArmorHandler.getArmorStatus} answers with the side's own copy of the flag, synchronised by the packets).
     *
     * <p>This listener runs <i>before</i> Ascent Boost's: it is registered by {@code KamiGear.register()} and the Enchantments group
     * registers Ascent Boost's in its ready phase ({@code ModEnchantments.initJump()}), so the 0.3 is added first and the factor
     * multiplies the sum, as in 1.7.10, where the boots' handler was registered in {@code preInit} and Ascent Boost's in {@code init}
     * (docs/deviations/KamiGear.md section 3).</p>
     */
    public static void onLivingJump(LivingEntity entity) {
        ItemStack boots = bootsOf(entity);
        if (boots.isEmpty() || !isActive((Player) entity, boots)) return;
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, motion.y + JUMP_BONUS, motion.z);
    }

    /**
     * Listener of {@code TinkererEvents.LIVING_FALL} (1.7.10 {@code EventHandler.onFall}): a player who wears the Gem Boots
     * lands with a fall distance of 0, whatever the switches say ({@code getCurrentArmor(0).getItem() == ItemGemBoots.this},
     * no look at the damage value or the master flag). With no distance there is no damage and no thud. Runs on both sides,
     * changes only the distance and nothing in the world.
     */
    public static EventResult onLivingFall(LivingEntity entity, TinkererEvents.FallData fall) {
        if (!bootsOf(entity).isEmpty()) fall.setDistance(0F);
        return EventResult.pass();
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
    private static void haste(Player player) {
        MobEffectInstance current = player.getEffect(MobEffects.DIG_SPEED);
        if (current == null || current.getDuration() < 2 || current.getAmplifier() < 1) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2, 1, true, true));
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
