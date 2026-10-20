package thaumcraft.common.items;

import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.entities.projectile.EntityAlumentum;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The dispenser behaviour of the alumentum (original BehaviorDispenseAlumetum). A dispenser that holds alumentum throws it as a projectile
 * with the power, the uncertainty and the lift of the original, and the launch sound of the original's event; anything else is dropped the
 * way a dispenser drops things.
 */
public final class BehaviorDispenseAlumetum implements DispenseItemBehavior {
    @Override public ItemStack dispense(BlockSource source, ItemStack stack) {
        if (stack.getItem() != ThaumcraftContent.MATERIALS.get("alumentum").get()) {
            return new DefaultDispenseItemBehavior().dispense(source, stack);
        }
        var level = source.level();
        Position position = DispenserBlock.getDispensePosition(source);
        var facing = source.state().getValue(DispenserBlock.FACING);
        var projectile = new EntityAlumentum(level, position.x(), position.y(), position.z());
        projectile.shoot(facing.getStepX(), facing.getStepY() + EquipmentRules.ALUMENTUM_DISPENSE_LIFT, facing.getStepZ(),
                EquipmentRules.ALUMENTUM_DISPENSE_POWER, EquipmentRules.ALUMENTUM_DISPENSE_UNCERTAINTY);
        level.addFreshEntity(projectile);
        stack.shrink(1);
        level.playSound(null, source.pos(), SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.0F);
        return stack;
    }
}
