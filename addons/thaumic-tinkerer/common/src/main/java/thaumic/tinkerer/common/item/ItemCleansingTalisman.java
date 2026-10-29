/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [11 Sep 2013, 15:45:16 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.ItemApi;
import thaumcraft.api.ThaumcraftSoundIds;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.potions.WarpEffects;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Talisman of Remedium. Worn in the amulet slot and switched on (sneak-use), it removes one harmful effect from the
 * wearer every second (the rules are those of {@link CleansingLogic}) and keeps the damage itself: one point of durability
 * per effect, ten for the five effects that come from warp.
 *
 * <p>The switch is the component {@link BasicItemComponents#CLEANSING_ENABLED} (1.7.10: the tag {@code enabled}). The
 * memory of what was removed last, kept per player, lives in this class (1.7.10: two maps on the item) and is forgotten when
 * the player leaves.</p>
 */
public class ItemCleansingTalisman extends Item implements IBauble, ITTinkererRegisterable {
    /** {@code LibFeatures.CLEANSING_TALISMAN_USES}: the durability of the talisman. */
    public static final int USES = 100;
    /**
     * The effects of warp that wear the talisman ten times as much (1.7.10 {@code Config.potionBlurredID},
     * {@code potionDeathGazeID}, {@code potionInfVisExhaustID}, {@code potionSunScornedID} and {@code potionUnHungerID}),
     * by the registry id of the effect.
     */
    public static final Set<String> WARP_EFFECTS = Set.of(id(WarpEffects.BLURRED_VISION), id(WarpEffects.DEATH_GAZE),
            id(WarpEffects.INFECTIOUS_VIS_EXHAUST), id(WarpEffects.SUN_SCORNED), id(WarpEffects.UNNATURAL_HUNGER));
    /**
     * The warp ward: 1.7.10 registered it as a bad effect and the talisman skipped it by class ({@code PotionWarpWard}); the
     * port registers it as beneficial, so this is only a guard that keeps the skip if that ever changes.
     */
    private static final String WARP_WARD = id(WarpEffects.WARP_WARD);

    /** What the talisman remembers of each wearer (server side only). */
    private static final Map<UUID, CleansingLogic.State> STATES = new HashMap<>();

    public ItemCleansingTalisman(Item.Properties properties) {
        super(properties.durability(USES).stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    private static String id(ResourceKey<MobEffect> key) {
        return key.location().toString();
    }

    public static boolean isEnabled(ItemStack stack) {
        return stack.getOrDefault(BasicItemComponents.CLEANSING_ENABLED.get(), false);
    }

    public static void setEnabled(ItemStack stack, boolean enabled) {
        if (enabled) stack.set(BasicItemComponents.CLEANSING_ENABLED.get(), true);
        else stack.remove(BasicItemComponents.CLEANSING_ENABLED.get());
    }

    public static void flipEnabled(ItemStack stack) {
        setEnabled(stack, !isEnabled(stack));
    }

    /** Forgets what was removed last for the player (called when the player leaves and when the server stops). */
    public static void forget(UUID player) {
        STATES.remove(player);
    }

    public static void forgetAll() {
        STATES.clear();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                flipEnabled(stack);
                BasicItemSupport.playSound(level, player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.3F, 0.1F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (isEnabled(stack)) tooltip.add(Component.translatable("ttmisc.active"));
        else tooltip.add(Component.translatable("ttmisc.inactive"));
    }

    /** 1.7.10 {@code getItemEnchantability}: "Equivalent to iron." */
    @Override
    public int getEnchantmentValue() {
        return 10;
    }

    /** The Smokey Quartz (the dark quartz item) mends the talisman at an anvil. */
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(TTRegistry.item(LibItemNames.DARK_QUARTZ)) || super.isValidRepairItem(stack, repairCandidate);
    }

    // ---- research and recipe -------------------------------------------------------------------------------------

    @Override
    public IRegisterableResearch getResearchItem() {
        return (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_CLEANSING_TALISMAN,
                new AspectList().add(Aspect.HEAL, 2).add(Aspect.ORDER, 1).add(Aspect.POISON, 1),
                -3,
                4,
                3,
                new ItemStack(this)).setSecondary().setParents(LibResearch.KEY_DARK_QUARTZ).setPages(
                        new ResearchPage("0"),
                        ResearchHelper.infusionPage(LibResearch.KEY_CLEANSING_TALISMAN));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(
                LibResearch.KEY_CLEANSING_TALISMAN,
                new ItemStack(this),
                5,
                new AspectList().add(Aspect.HEAL, 10).add(Aspect.TOOL, 10).add(Aspect.MAN, 20).add(Aspect.LIFE, 10),
                new ItemStack(Items.ENDER_PEARL),
                TTRegistry.stack(LibItemNames.DARK_QUARTZ),
                TTRegistry.stack(LibItemNames.DARK_QUARTZ),
                TTRegistry.stack(LibItemNames.DARK_QUARTZ),
                TTRegistry.stack(LibItemNames.DARK_QUARTZ),
                new ItemStack(Items.GHAST_TEAR),
                ItemApi.getItem("itemResource", 1));
    }

    // ---- bauble --------------------------------------------------------------------------------------------------

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.AMULET;
    }

    /**
     * A click with the talisman in the hand puts it on, unless the player sneaks (that click switches it): the plain click
     * did nothing in 1.7.10, so this takes nothing away from the original.
     */
    @Override
    public boolean equipsOnUse(ItemStack stack, Player player) {
        return !player.isShiftKeyDown();
    }

    @Override
    public void onWornTick(ItemStack stack, LivingEntity wearer) {
        Level level = wearer.level();
        if (!isEnabled(stack) || level.isClientSide || !(wearer instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        CleansingLogic.State state = STATES.computeIfAbsent(player.getUUID(), id -> new CleansingLogic.State());

        // The harmful effects that have a duration of their own; effects without one (infinite, or a flash) are skipped.
        Map<String, Holder<MobEffect>> effects = new LinkedHashMap<>();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (instance.isInfiniteDuration() || instance.getDuration() < 20) continue;
            Holder<MobEffect> effect = instance.getEffect();
            String id = effect.unwrapKey().map(key -> key.location().toString()).orElse(null);
            if (id == null || id.equals(WARP_WARD)) continue;
            if (effect.value().getCategory() == MobEffectCategory.HARMFUL) effects.put(id, effect);
        }

        CleansingLogic.Plan plan = CleansingLogic.plan(state, player.isOnFire(), new ArrayList<>(effects.keySet()));

        for (String id : plan.toRemove()) {
            if (id.equals(CleansingLogic.BURNING)) player.clearFire();
            else player.removeEffect(effects.get(id));
        }

        if (!plan.removed().isEmpty()) {
            int damage = WARP_EFFECTS.contains(plan.removed()) ? 10 : 1;
            stack.hurtAndBreak(damage, (ServerLevel) level, player,
                    broken -> BasicItemSupport.playSound(level, player, SoundEvents.ITEM_BREAK, 0.8F, 0.8F + level.random.nextFloat() * 0.4F));
            // A broken talisman leaves an empty stack in its slot, which the accessory slots treat as free.
        }

        if (!plan.removed().isEmpty() || plan.permanentRemoved()) {
            BasicItemSupport.playThaumcraftSound(level, player, ThaumcraftSoundIds.WAND, 0.3F, 0.1F);
        }
    }
}
