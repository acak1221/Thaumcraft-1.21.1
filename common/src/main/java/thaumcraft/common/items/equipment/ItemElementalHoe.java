package thaumcraft.common.items.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.IRepairable;
import thaumcraft.common.blocks.BlockMagicalSapling;

/**
 * Hoe of Growth (original ItemElementalHoe, Api THAUMIUM_ELEMENTAL: 1500 uses, tier 3, repaired with the thaumium ingot, enchantability 5,
 * no attack damage). A click tills the clicked block and the eight around it in its layer, a use apiece; a sneaking player tills only the
 * clicked block. A click that tilled nothing tries bone meal on the clicked block (one use), and failing that a magical sapling: it grows
 * into its tree, a greatwood sapling for 5 uses while 20 remain, a silverwood one for 25 while 150 remain (the original pays even when the
 * tree finds no room). The original's sparkle effects are not ported.
 */
public class ItemElementalHoe extends HoeItem implements IRepairable {
    private static final SoundEvent GROW = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand"));

    public ItemElementalHoe(Tier tier, Properties properties) {
        // Original hoe has NO attack-damage entry. Only adapt native attack speed.
        super(tier, properties.rarity(Rarity.RARE).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -1,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()));
    }

    @Override public int getEnchantmentValue() { return 5; } // ItemElementalHoe:36-38, deliberately not the tier's 18.

    // In 1.7 ItemHoe extended Item, not ItemTool: a hit did not spend the two uses of a digger.
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { return false; }
    @Override public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {}

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return super.useOn(context);
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        boolean did = false;
        for (int x = -ElementalHoeRules.TILL_RADIUS; x <= ElementalHoeRules.TILL_RADIUS; ++x) {
            for (int z = -ElementalHoeRules.TILL_RADIUS; z <= ElementalHoeRules.TILL_RADIUS; ++z) {
                BlockPos cell = pos.offset(x, 0, z);
                if (!player.mayUseItemAt(cell, face, stack)) continue;
                var hit = new BlockHitResult(Vec3.atCenterOf(cell), face, cell, context.isInside());
                if (super.useOn(new UseOnContext(player, context.getHand(), hit)).consumesAction()) did = true;
            }
        }
        if (!did) {
            if (bonemeal(level, pos)) {
                stack.hurtAndBreak(ElementalHoeRules.BONEMEAL_WEAR, player, EquipmentSlot.MAINHAND);
                did = true;
            } else if (level.getBlockState(pos).getBlock() instanceof BlockMagicalSapling sapling
                    && ElementalHoeRules.mayGrow(stack.getDamageValue(), stack.getMaxDamage(), sapling.isSilverwood())) {
                if (level instanceof ServerLevel server) sapling.grow(server, pos, level.random);
                stack.hurtAndBreak(ElementalHoeRules.wear(sapling.isSilverwood()), player, EquipmentSlot.MAINHAND);
                did = true;
            }
            if (did && level instanceof ServerLevel server) {
                server.playSound(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, GROW, SoundSource.PLAYERS, 0.75F, 0.9F + level.random.nextFloat() * 0.2F);
            }
        }
        return did ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    /** Original {@code Utils.useBonemealAtLoc}: a growable block that can still be fertilised is, whether or not this roll takes. */
    private static boolean bonemeal(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BonemealableBlock growable) || !growable.isValidBonemealTarget(level, pos, state)) return false;
        if (level instanceof ServerLevel server && growable.isBonemealSuccess(level, level.random, pos, state)) growable.performBonemeal(server, level.random, pos, state);
        return true;
    }
}
