/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [28 Sep 2013, 18:27:56 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import thaumcraft.api.ItemApi;
import thaumcraft.api.ThaumcraftSoundIds;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Talisman of Withhold. Worn in the amulet slot and switched on (sneak-use), it absorbs the experience orbs within 3
 * blocks into the talisman, up to 1500 points; used with a glass bottle in the inventory it turns 10 points and the
 * bottle into a Bottle o' Enchanting.
 *
 * <p>The stored points are the component {@link BasicItemComponents#XP} and the switch is the component
 * {@link BasicItemComponents#ABSORBING} (1.7.10: the tag {@code xp} and the damage value 1).</p>
 */
public class ItemXPTalisman extends Item implements IBauble, ITTinkererRegisterable {
    /** {@code LibFeatures.XP_TALISMAN_RANGE}: the half width of the cube the talisman gathers orbs from. */
    public static final int RANGE = 3;
    /** {@code LibFeatures.XP_TALISMAN_MAX_XP}: the most points a talisman holds. */
    public static final int MAX_XP = 1500;
    /** {@code LibFeatures.XP_TALISMAN_ENCHANTING_BOTTLE_COST}: the points of a Bottle o' Enchanting. */
    public static final int BOTTLE_COST = 10;

    public ItemXPTalisman(Item.Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    // ---- the stored values ---------------------------------------------------------------------------------------

    /** 1.7.10 {@code hasCmp}: whether the talisman carries any value of its own. */
    public static boolean hasCmp(ItemStack stack) {
        return stack.has(BasicItemComponents.XP.get()) || stack.has(BasicItemComponents.ABSORBING.get());
    }

    public static int getXP(ItemStack stack) {
        return stack.getOrDefault(BasicItemComponents.XP.get(), 0);
    }

    public static void setXP(ItemStack stack, int xp) {
        if (xp > 0) stack.set(BasicItemComponents.XP.get(), xp);
        else stack.remove(BasicItemComponents.XP.get());
    }

    /** True while the talisman absorbs orbs (1.7.10: damage value 1). */
    public static boolean isAbsorbing(ItemStack stack) {
        return stack.getOrDefault(BasicItemComponents.ABSORBING.get(), false);
    }

    public static void setAbsorbing(ItemStack stack, boolean absorbing) {
        if (absorbing) stack.set(BasicItemComponents.ABSORBING.get(), true);
        else stack.remove(BasicItemComponents.ABSORBING.get());
    }

    // ---- use -----------------------------------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (getXP(stack) < MAX_XP) {
                if (!level.isClientSide) {
                    setAbsorbing(stack, !isAbsorbing(stack));
                    BasicItemSupport.playSound(level, player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.3F, 0.1F);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        } else if (getXP(stack) >= BOTTLE_COST) {
            ItemStack bottle = BasicItemSupport.findInInventory(player, Items.GLASS_BOTTLE);
            if (!bottle.isEmpty()) {
                if (!level.isClientSide) {
                    bottle.shrink(1);
                    ItemStack experienceBottle = new ItemStack(Items.EXPERIENCE_BOTTLE);
                    if (!player.getInventory().add(experienceBottle)) player.spawnAtLocation(experienceBottle);
                    setXP(stack, getXP(stack) - BOTTLE_COST);
                    BasicItemSupport.playSound(level, player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1F,
                            (float) (0.1F + Math.random() / 2F));
                    if (level instanceof ServerLevel serverLevel) {
                        // 1.7.10: six sparkles of Thaumcraft, within half a block of the player
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY(), player.getZ(),
                                6, 0.5, 0.5, 0.5, 0.0);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    private static void consumeXPOrb(Level level, ExperienceOrb orb) {
        if (!level.isClientSide) {
            orb.discard();
            BasicItemSupport.playThaumcraftSound(level, orb, ThaumcraftSoundIds.ZAP, orb.getValue() / 10F, 1F);
        } else {
            // 1.7.10: a green wisp of the size xp / 5 (Thaumcraft's particle engine is not API: a green dust particle)
            level.addParticle(new DustParticleOptions(new Vector3f(0.1F, 0.9F, 0.1F), Mth.clamp(orb.getValue() / 5F, 0.5F, 4F)),
                    orb.getX(), orb.getY(), orb.getZ(), 0, 0.02, 0);
        }
    }

    // ---- display -------------------------------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("ttmisc.xpTalisman.xp", getXP(stack)));
        if (getXP(stack) >= MAX_XP) tooltip.add(Component.translatable("ttmisc.full"));
        else if (!isAbsorbing(stack)) tooltip.add(Component.translatable("ttmisc.notAbsorbing"));
        else tooltip.add(Component.translatable("ttmisc.absorbing"));
    }

    // ---- research and recipe -------------------------------------------------------------------------------------

    @Override
    public IRegisterableResearch getResearchItem() {
        // 1.7.10: the icon is the stack of damage 1, the switched-on look
        ItemStack icon = new ItemStack(this);
        setAbsorbing(icon, true);
        return (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_XP_TALISMAN,
                new AspectList().add(Aspect.GREED, 1).add(Aspect.MAGIC, 1).add(Aspect.MAN, 1),
                4,
                -1,
                2,
                icon).setParents("JARBRAIN", LibResearch.KEY_SPELL_CLOTH).setConcealed()
                        .setPages(new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_XP_TALISMAN))
                        .setSecondary();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(
                LibResearch.KEY_XP_TALISMAN,
                new ItemStack(this),
                6,
                new AspectList().add(Aspect.GREED, 20).add(Aspect.EXCHANGE, 10).add(Aspect.BEAST, 10)
                        .add(Aspect.MECHANISM, 5),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.QUARTZ),
                new ItemStack(Items.QUARTZ),
                ItemApi.getItem("itemZombieBrain", 0),
                new ItemStack(Items.DIAMOND));
    }

    // ---- bauble --------------------------------------------------------------------------------------------------

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.AMULET;
    }

    /** The click toggles the talisman and bottles its points, so it is never put on by clicking: the Baubles menu does that. */
    @Override
    public boolean equipsOnUse(ItemStack stack, Player player) {
        return false;
    }

    @Override
    public void onWornTick(ItemStack stack, LivingEntity wearer) {
        Level level = wearer.level();
        if (!isAbsorbing(stack)) return;

        int currentXP = getXP(stack);
        int xpToAdd = 0;
        int maxXP = MAX_XP - currentXP; // Max, to prevent overflow.
        if (maxXP <= 0) {
            if (!level.isClientSide) setAbsorbing(stack, false);
            return; // Can't take any XP.
        }

        AABB boundingBox = new AABB(wearer.getX() - RANGE, wearer.getY() - RANGE, wearer.getZ() - RANGE,
                wearer.getX() + RANGE, wearer.getY() + RANGE, wearer.getZ() + RANGE);
        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, boundingBox);

        for (ExperienceOrb orb : orbs) {
            if (!orb.isRemoved()) {
                int xp = orb.getValue();
                if (xpToAdd + xp <= maxXP) {
                    xpToAdd += xp;
                    consumeXPOrb(level, orb);
                }

                maxXP -= xpToAdd;

                if (maxXP <= 0) break;
            }
        }
        if (!level.isClientSide) {
            setXP(stack, Math.min(MAX_XP, currentXP + xpToAdd));
        }
    }
}
