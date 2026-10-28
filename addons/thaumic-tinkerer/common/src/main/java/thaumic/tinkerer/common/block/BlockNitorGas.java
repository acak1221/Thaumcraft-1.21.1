/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [11 Sep 2013, 17:47:28 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.core.helper.SimpleBlockFx;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * The light of the Hyperenergetic Nitor: a gas that stays only while a player who carries the item (or wears the
 * Kami leggings) is near. Level 1 is the strong variant of the Kami leggings (light 15, range 6), level 0 the one of
 * the item (light 12, range 1).
 */
public class BlockNitorGas extends BlockGas {

    public BlockNitorGas(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return gasProperties().lightLevel(state -> state.getValue(LEVEL) == 1 ? 15 : 12);
    }

    /** 1.7.10 {@code tickRate}: sixty ticks in the Nether, twenty elsewhere. */
    public int tickRate(Level level) {
        return level.dimension() == Level.NETHER ? 60 : 20;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.03F) {
            level.addParticle(SimpleBlockFx.SPARKLE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    (random.nextFloat() - 0.5) * 0.01, (random.nextFloat() - 0.5) * 0.01, (random.nextFloat() - 0.5) * 0.01);
        }
    }

    /** Whether a player carries the item, or wears the Kami leggings (while Kami is enabled). */
    public static boolean carriesLight(Player player) {
        Item nitor = BuiltInRegistries.ITEM.getOptional(LibMisc.id(LibItemNames.BRIGHT_NTIOR)).orElse(null);
        if (nitor != null && player.getInventory().contains(stack -> stack.is(nitor))) return true;
        if (!ConfigHandler.enableKami) return false;
        Item legs = BuiltInRegistries.ITEM.getOptional(LibMisc.id(LibItemNames.ICHOR_LEGS_GEM)).orElse(null);
        ItemStack worn = player.getItemBySlot(EquipmentSlot.LEGS);
        return legs != null && !worn.isEmpty() && worn.is(legs);
    }

    /** 1.7.10 {@code updateTick}: stay while a carrier is near, else vanish. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean remove = false;
        int dist = state.getValue(LEVEL) == 1 ? 6 : 1;
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos.getX() - dist, pos.getY() - dist,
                pos.getZ() - dist, pos.getX() + dist, pos.getY() + dist, pos.getZ() + dist));
        if (players.isEmpty()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            remove = true;
        } else {
            boolean has = false;
            for (Player player : players) {
                if (carriesLight(player)) {
                    has = true;
                    break;
                }
            }

            if (!has) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                remove = true;
            }
        }
        if (!remove) level.scheduleTick(pos, this, tickRate(level));
    }

    /** 1.7.10 {@code onBlockAdded}. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) level.scheduleTick(pos, this, tickRate(level));
    }
}
