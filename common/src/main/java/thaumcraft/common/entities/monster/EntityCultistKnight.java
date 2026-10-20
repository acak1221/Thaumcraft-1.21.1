package thaumcraft.common.entities.monster;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.lib.cult.CultistRules;
import thaumcraft.common.lib.loot.OriginalLootEnchantments;

/**
 * Original TC4 crimson knight: thirty-six health, the crimson plate armour and, rarely,
 * a void or thaumium blade. The held weapon is enchanted with the original chance.
 */
public class EntityCultistKnight extends EntityCultist {
    public EntityCultistKnight(EntityType<? extends EntityCultistKnight> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCultist.createAttributes().add(Attributes.MAX_HEALTH, CultistRules.KNIGHT_HEALTH);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new CultistSwimGoal(this));
        goalSelector.addGoal(3, new CultistMeleeGoal(this, 1.0, false));
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new CultistHurtByTargetGoal(this, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public Component getName() {
        return hasCustomName() ? getCustomName() : Component.translatable("entity.thaumcraft.cultistknight.name");
    }

    /** Original EntityCultistKnight.addRandomArmor: the crimson plate and the rare blade. */
    @Override protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.HEAD, stack(CultistRules.ITEM_PLATE_HELM));
        setItemSlot(EquipmentSlot.CHEST, stack(CultistRules.ITEM_PLATE_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, stack(CultistRules.ITEM_PLATE_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, stack(CultistRules.ITEM_BOOTS));
        if (random.nextFloat() < CultistRules.knightRareChance(level().getDifficulty() == Difficulty.HARD)) {
            if (CultistRules.knightRareWeapon(random.nextInt(5)) == CultistRules.KNIGHT_WEAPON_VOID) {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ThaumcraftEquipment.VOID_SWORD.get()));
                setItemSlot(EquipmentSlot.HEAD, stack(CultistRules.ITEM_ROBE_HOOD));
            } else {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ThaumcraftEquipment.THAUMIUM_SWORD.get()));
                if (!CultistRules.knightKeepsHelm(random.nextBoolean())) setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            }
        } else {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    /** Original EntityCultistKnight.enchantEquipment: the held blade with the original chance and power. */
    @Override protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level, RandomSource random, DifficultyInstance difficulty) {
        ItemStack held = getMainHandItem();
        if (held.isEmpty()) return;
        float tension = level.getCurrentDifficultyAt(blockPosition()).getSpecialMultiplier();
        if (random.nextFloat() < CultistRules.knightEnchantChance(tension)) {
            int power = CultistRules.knightEnchantPower(tension, random.nextInt(18));
            OriginalLootEnchantments.enchant(level.getLevel(), held, random, power);
        }
    }
}
