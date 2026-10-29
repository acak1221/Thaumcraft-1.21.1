/**
 * Created by pixlepix on 8/28/14.
 * <p/>
 * Prevents bonemeal being used on TT crops
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.helper;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftFX;
import thaumcraft.api.items.ItemsTC;
import thaumic.tinkerer.common.block.BlockInfusedGrain;
import thaumic.tinkerer.common.core.handler.ConfigHandler;

/**
 * Bone meal and the Hoe of Growth on the infused crops.
 *
 * <p>1.7.10 cancelled the {@code BonemealEvent} on infused grain unless {@code cropsAllowBonemeal}; that is
 * {@code BlockInfusedGrain.isValidBonemealTarget} now, there being no such event in 1.21.1. The Hoe of Growth of
 * Thaumcraft grows crops through that same method, which would shut it out as well, so the click handler of 1.7.10
 * ({@code onPlayerInteract}) is kept: a magic hoe grows an infused crop that is not ripe, costing 25 uses, with a
 * sparkle and the wand sound. While the configuration allows bone meal the hoe of Thaumcraft grows the crop itself and
 * this handler stays out of its way.</p>
 */
public class BonemealEventHandler {
    private static boolean registered;

    private BonemealEventHandler() {}

    /** Registers the click handlers (both clicks, as the 1.7.10 {@code PlayerInteractEvent} saw both). Once. */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        InteractionEvent.RIGHT_CLICK_BLOCK.register(BonemealEventHandler::onInteract);
        InteractionEvent.LEFT_CLICK_BLOCK.register(BonemealEventHandler::onInteract);
    }

    /** The Hoe of Growth of Thaumcraft (1.7.10 {@code ConfigItems.itemHoeElemental}); the electric hoe of EMT has no 1.21.1 counterpart. */
    public static boolean isMagicHoe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item hoe = ItemsTC.get("elemental_hoe");
        return hoe != null && stack.is(hoe);
    }

    private static EventResult onInteract(Player player, InteractionHand hand, BlockPos pos, Direction face) {
        Level world = player.level();
        if (world.isClientSide || !(world instanceof ServerLevel server)) return EventResult.pass();
        if (ConfigHandler.cropsAllowBonemeal) return EventResult.pass();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockInfusedGrain infusedGrain)) return EventResult.pass();

        ItemStack hoeOfGrowth = player.getMainHandItem();
        if (hand != InteractionHand.MAIN_HAND || !isMagicHoe(hoeOfGrowth)) return EventResult.pass();

        // func_149851_a (not fully grown) and func_149852_a (the roll, always true for crops)
        if (infusedGrain.isMaxAge(state) || !infusedGrain.isBonemealSuccess(world, world.random, pos, state)) {
            return EventResult.pass();
        }
        infusedGrain.growCrops(world, pos, state);
        damageHoe(hoeOfGrowth, player);
        ThaumcraftFX.blockSparkle(server, pos, 0xFFFFFF);
        SimpleBlockFx.wandSound(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.75F,
                0.9F + world.random.nextFloat() * 0.2F);
        return EventResult.pass();
    }

    private static void damageHoe(ItemStack hoeOfGrowth, Player player) {
        hoeOfGrowth.hurtAndBreak(25, player, EquipmentSlot.MAINHAND);
    }
}
