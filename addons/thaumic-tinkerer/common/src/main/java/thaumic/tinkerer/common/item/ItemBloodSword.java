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
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
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
 * Architectury event {@code EntityEvent.LIVING_HURT}; there is no common event for the second, so the drops of a
 * reaped mob are replaced with the events {@code LIVING_DEATH}, {@code ADD} and the end of the level tick (see
 * {@link Harvest}).</p>
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
        Harvest.register();
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
     * {@code LivingDropsEvent}: a mob killed in melee by a player whose cursed blade is activated drops the soul
     * aspects of the mob (one item per aspect of its row in {@link EnumMobAspect}, picked up after half a second) and
     * nothing else. The death event only notes the victim; the items that the death itself spawns at the victim are
     * refused when they are added, and the aspects are dropped when the tick of the level ends, if the victim did die.
     */
    static final class Harvest {
        /** A victim noted by the death event; only values, no entity or level is kept. */
        private record Victim(ResourceKey<Level> dimension, UUID id, double x, double y, double z, double height) {}

        private static final List<Victim> PENDING = new ArrayList<>();
        private static boolean spawningAspects;

        private Harvest() {}

        static void register() {
            EntityEvent.LIVING_DEATH.register(Harvest::onDeath);
            EntityEvent.ADD.register(Harvest::onItemAdded);
            TickEvent.SERVER_LEVEL_POST.register(Harvest::onLevelTickEnd);
        }

        private static EventResult onDeath(LivingEntity entity, DamageSource source) {
            if (entity.level().isClientSide || !(entity instanceof Mob mob)) return EventResult.pass();
            if (!source.is(DamageTypes.PLAYER_ATTACK) || !(source.getEntity() instanceof Player player)) {
                return EventResult.pass();
            }

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof ItemBloodSword) || !isActivated(stack)) return EventResult.pass();

            if (EnumMobAspect.getAspectsForEntity(mob) != null) {
                PENDING.add(new Victim(mob.level().dimension(), mob.getUUID(), mob.getX(), mob.getY(), mob.getZ(),
                        mob.getBbHeight()));
            }
            return EventResult.pass();
        }

        /** Refuses the items that the death of a noted victim spawns at the victim. */
        private static EventResult onItemAdded(Entity entity, Level level) {
            if (spawningAspects || PENDING.isEmpty() || !(entity instanceof ItemEntity item)) return EventResult.pass();
            for (Victim victim : PENDING) {
                if (victim.dimension().equals(level.dimension()) && Math.abs(item.getX() - victim.x()) < 1.0E-3
                        && Math.abs(item.getZ() - victim.z()) < 1.0E-3 && item.getY() >= victim.y() - 1.0E-3
                        && item.getY() <= victim.y() + victim.height() + 1.0) {
                    return EventResult.interruptFalse();
                }
            }
            return EventResult.pass();
        }

        private static void onLevelTickEnd(ServerLevel level) {
            if (PENDING.isEmpty()) return;
            List<Victim> mine = new ArrayList<>();
            for (Victim victim : PENDING) if (victim.dimension().equals(level.dimension())) mine.add(victim);
            PENDING.removeAll(mine);

            for (Victim victim : mine) {
                Entity entity = level.getEntity(victim.id());
                if (!(entity instanceof LivingEntity living) || !living.isDeadOrDying()) continue;
                Aspect[] aspects = EnumMobAspect.getAspectsForEntity(living);
                if (aspects == null) continue;
                spawningAspects = true;
                try {
                    for (Aspect aspect : aspects) {
                        ItemEntity drop = new ItemEntity(level, victim.x(), victim.y(), victim.z(),
                                ItemMobAspect.getStackFromAspect(aspect));
                        drop.setPickUpDelay(10);
                        level.addFreshEntity(drop);
                    }
                } finally {
                    spawningAspects = false;
                }
            }
        }
    }
}
