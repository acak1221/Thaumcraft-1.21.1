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
package thaumic.tinkerer.common.item;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.IRepairable;
import thaumcraft.api.ItemApi;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.core.helper.EnumMobAspect;
import thaumic.tinkerer.common.hooks.TinkererEvents;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Cursed Spirit's Blade. Ten points of attack damage and a quarter more speed while held, 950 uses; it hurts its
 * wielder with every blow (2 magic damage), turns the damage a blocking wielder takes into 3 magic damage, and, once
 * activated with a shift-right click, makes the mobs it kills drop soul aspects instead of their loot.
 *
 * <p>1.7.10 listened to the Forge events {@code LivingAttackEvent} and {@code LivingDropsEvent}. Here the first is the
 * Architectury event {@code EntityEvent.LIVING_HURT}, the second the loader hook {@link TinkererEvents#LIVING_DROPS}
 * (see {@code docs/HOOKS.md} section 8): the handler edits the list of drops before they enter the world, as the
 * original edited {@code event.drops}.</p>
 */
public class ItemBloodSword extends SwordItem implements IRepairable, ITTinkererRegisterable {

    private static final int DAMAGE = 10;
    private static final String TAG_ACTIVATED = "Activated";

    /** {@code EnumHelper.addToolMaterial("TT_BLOOD", 0, 950, 0, 0, thaumium enchantability)}. */
    private static final Tier TIER = new Tier() {
        @Override
        public int getUses() {
            return 950;
        }

        @Override
        public float getSpeed() {
            return 0;
        }

        @Override
        public float getAttackDamageBonus() {
            return 0;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            // harvest level 0: the tool mines like a wooden one
            return BlockTags.INCORRECT_FOR_WOODEN_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return ThaumcraftApi.toolMatThaumium.getEnchantmentValue();
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };

    public ItemBloodSword(Properties properties) {
        super(TIER, properties.attributes(attributes()));
    }

    /** {@code getItemAttributeModifiers}: +10 attack damage and +25 % movement speed while held. */
    private static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, DAMAGE, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(LibMisc.id("blood_sword_speed"), 0.25,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static boolean isActivated(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getInt(TAG_ACTIVATED) == 1;
    }

    private static void setActivated(ItemStack stack, boolean activated) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG_ACTIVATED, activated ? 1 : 0));
        // the "active" icon of 1.7.10 (blood_sword_1) is a model override on the custom model data
        if (activated) stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
        else stack.remove(DataComponents.CUSTOM_MODEL_DATA);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /** A shift-right click switches the harvest of soul aspects; every right click raises the blade to block. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (player.isShiftKeyDown() && !level.isClientSide) {
            if (!isActivated(stack)) {
                player.displayClientMessage(Component.translatable("ttmisc.bloodSword.activateEssentiaHarvest"), false);
                setActivated(stack, true);
            } else {
                player.displayClientMessage(Component.translatable("ttmisc.bloodSword.deactivateEssentiaHarvest"), false);
                setActivated(stack, false);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return (IRegisterableResearch) new TTResearchItem(LibResearch.KEY_BLOOD_SWORD,
                new AspectList().add(Aspect.HUNGER, 2).add(Aspect.WEAPON, 1).add(Aspect.FLESH, 1).add(Aspect.SOUL, 1), -4,
                6, 3, new ItemStack(this)).setWarp(1).setParents(LibResearch.KEY_CLEANSING_TALISMAN)
                        .setPages(new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_BLOOD_SWORD),
                                new ResearchPage("1"))
                        .setSecondary();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_BLOOD_SWORD, new ItemStack(this), 6,
                new AspectList().add(Aspect.HUNGER, 20).add(Aspect.DARKNESS, 5).add(Aspect.SOUL, 10).add(Aspect.MAN, 6),
                ItemApi.getItem("itemSwordThaumium", 0), new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.PORKCHOP), new ItemStack(Items.BEEF), new ItemStack(Items.BONE),
                new ItemStack(Items.DIAMOND), new ItemStack(Items.GHAST_TEAR));
    }

    // ---- the events -----------------------------------------------------------------------------------------

    /** True while the sword itself hurts someone: the damage it deals must not trigger it again. */
    private static boolean handling;

    /** Registers the events of the blade (once, from the registrar of the group). */
    public static void registerEvents() {
        EntityEvent.LIVING_HURT.register(ItemBloodSword::onDamageTaken);
        TinkererEvents.LIVING_DROPS.register(ItemBloodSword::onLivingDrops);
    }

    /**
     * {@code LivingAttackEvent} of 1.7.10. A wielder that blocks with the blade takes 3 magic damage instead of the
     * blow, and whoever strikes with the blade takes 2 magic damage. The 1.7.10 handler guarded against its own damage
     * by skipping the next three events of the whole world; the guard here covers exactly the nested damage.
     */
    public static EventResult onDamageTaken(LivingEntity entity, DamageSource source, float amount) {
        if (entity.level().isClientSide || handling) return EventResult.pass();

        EventResult result = EventResult.pass();
        if (entity instanceof Player player && player.isUsingItem()
                && player.getUseItem().getItem() instanceof ItemBloodSword) {
            hurtByTheBlade(player, 3);
            result = EventResult.interruptFalse();
        }

        Entity attacker = source.getDirectEntity();
        if (attacker instanceof LivingEntity living && living.getMainHandItem().getItem() instanceof ItemBloodSword) {
            hurtByTheBlade(living, 2);
        }

        return result;
    }

    private static void hurtByTheBlade(LivingEntity victim, float amount) {
        handling = true;
        try {
            victim.hurt(victim.damageSources().magic(), amount);
        } finally {
            handling = false;
        }
    }

    /**
     * {@code LivingDropsEvent} of 1.7.10: a mob killed in melee ({@code damageType "player"}, here the damage type
     * {@code player_attack}) by a player whose cursed blade is in the main hand and activated drops the soul aspects of
     * the mob (one item per aspect of its row in {@link EnumMobAspect}, picked up after half a second) and nothing else:
     * the list of drops is cleared, as {@code event.drops.clear()} did, and the soul aspects are added at the victim's
     * position. A mob without a row in the table keeps its loot. The event is server only and posted once per death, so
     * the handler needs no side test; it answers only for a mob killed by the blade and leaves every other death alone.
     * It never cancels: whatever a listener after it sees is the list with the soul aspects.
     */
    public static EventResult onLivingDrops(LivingEntity victim, DamageSource source, List<ItemEntity> drops, int lootingLevel,
            boolean recentlyHit) {
        if (!source.is(DamageTypes.PLAYER_ATTACK) || !(victim instanceof Mob mob)
                || !(source.getEntity() instanceof Player player)) {
            return EventResult.pass();
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemBloodSword) || !isActivated(stack)) return EventResult.pass();

        Aspect[] aspects = EnumMobAspect.getAspectsForEntity(mob);
        if (aspects == null) return EventResult.pass();

        drops.clear();
        for (Aspect aspect : aspects) {
            ItemEntity drop = new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(),
                    ItemMobAspect.getStackFromAspect(aspect));
            drop.setPickUpDelay(10);
            drops.add(drop);
        }
        return EventResult.pass();
    }
}
