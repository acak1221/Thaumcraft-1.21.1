/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [8 Sep 2013, 19:35:02 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.block.tile.TileGolemConnector;
import thaumic.tinkerer.common.block.tile.transvector.TileTransvector;
import thaumic.tinkerer.common.block.tile.transvector.TileTransvectorInterface;
import thaumic.tinkerer.common.content.TileBlocks;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;

/**
 * The Transvector Binder: a right click on a Transvector Interface, Dislocator or Golem Connector remembers it, a right
 * click on the block it should be bound to (up to the device's range) binds it; sneaking and right-clicking a golem binds
 * a remembered Golem Connector to that golem.
 *
 * <p>1.7.10 kept the remembered position in the stack's NBT ({@code posx}, {@code posy}, {@code posz}) and used
 * {@code posy == -1} for "nothing remembered"; here the keys live in the custom-data component, and "nothing remembered"
 * is the absence of {@code posy}.</p>
 */
public class ItemConnector extends Item implements ITTinkererRegisterable {

    private static final String TAG_POS_X = "posx";
    private static final String TAG_POS_Y = "posy";
    private static final String TAG_POS_Z = "posz";
    private static final String TAG_CONNECTING_GOLEM = "ConnectingGolem";

    /** The entity type of Thaumcraft's golems: {@code EntityGolemBase} cannot be named from the API. */
    private static final ResourceLocation GOLEM_TYPE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "golem");

    public ItemConnector() {
        super(new Item.Properties().stacksTo(1));
    }

    private static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void write(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean getConnectingGolem(ItemStack stack) {
        return read(stack).getBoolean(TAG_CONNECTING_GOLEM);
    }

    public static void setConnectingGolem(ItemStack stack, boolean connecting) {
        CompoundTag tag = read(stack);
        tag.putBoolean(TAG_CONNECTING_GOLEM, connecting);
        write(stack, tag);
    }

    /** True while the binder remembers a device. */
    public static boolean hasTarget(ItemStack stack) {
        return read(stack).contains(TAG_POS_Y);
    }

    /** The remembered device; only meaningful while {@link #hasTarget} is true. */
    public static BlockPos getTarget(ItemStack stack) {
        CompoundTag tag = read(stack);
        return new BlockPos(tag.getInt(TAG_POS_X), tag.getInt(TAG_POS_Y), tag.getInt(TAG_POS_Z));
    }

    public static void setTarget(ItemStack stack, BlockPos pos) {
        CompoundTag tag = read(stack);
        tag.putInt(TAG_POS_X, pos.getX());
        tag.putInt(TAG_POS_Y, pos.getY());
        tag.putInt(TAG_POS_Z, pos.getZ());
        write(stack, tag);
    }

    /** 1.7.10 {@code setY(stack, -1)}: forget the device. */
    public static void clearTarget(ItemStack stack) {
        CompoundTag tag = read(stack);
        tag.remove(TAG_POS_X);
        tag.remove(TAG_POS_Y);
        tag.remove(TAG_POS_Z);
        write(stack, tag);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return null;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(
                LibResearch.KEY_INTERFACE + "1",
                LibResearch.KEY_INTERFACE,
                new ItemStack(this),
                new AspectList().add(Aspect.ORDER, 2),
                " I ",
                " WI",
                "S  ",
                'I',
                new ItemStack(Items.IRON_INGOT),
                'W',
                new ItemStack(Items.STICK),
                'S',
                TileBlocks.legacyItem("itemShard", 4));
    }

    /** {@code onItemUse}: remember a device, or bind the remembered device to the clicked block. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) return InteractionResult.sidedSuccess(level.isClientSide);

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        BlockEntity tile = level.getBlockEntity(pos);

        if (!hasTarget(stack)) {
            if (tile instanceof TileTransvector || tile instanceof TileGolemConnector) {
                setTarget(stack, pos);

                playSound(level, pos);
                if (tile instanceof TileTransvector) player.sendSystemMessage(Component.translatable("ttmisc.connector.set"));
                else player.sendSystemMessage(Component.translatable("ttmisc.golemconnector.set"));
            } else player.sendSystemMessage(Component.translatable("ttmisc.connector.notinterf"));
        } else {
            BlockPos origin = getTarget(stack);

            BlockEntity tile1 = level.hasChunkAt(origin) ? level.getBlockEntity(origin) : null;
            if (!(tile1 instanceof TileTransvector trans)) {
                clearTarget(stack);

                player.sendSystemMessage(Component.translatable("ttmisc.connector.notpresent"));
            } else {
                if (tile instanceof TileTransvectorInterface && tile1 instanceof TileTransvectorInterface) {
                    player.sendSystemMessage(Component.translatable("ttmisc.connector.interffail"));
                    return InteractionResult.CONSUME;
                }

                if (Math.abs(origin.getX() - pos.getX()) > trans.getMaxDistance()
                        || Math.abs(origin.getY() - pos.getY()) > trans.getMaxDistance()
                        || Math.abs(origin.getZ() - pos.getZ()) > trans.getMaxDistance()) {
                    player.sendSystemMessage(Component.translatable("ttmisc.connector.toofar"));
                    return InteractionResult.CONSUME;
                }

                trans.setTarget(pos);

                clearTarget(stack);

                playSound(level, pos);
                player.sendSystemMessage(Component.translatable("ttmisc.connector.complete"));
                // 1.7.10 marked the new target for an update; the device itself is sent as well, so that clients see the
                // binding (their tubes and pipes ask the interface).
                BlockState targetState = level.getBlockState(pos);
                level.sendBlockUpdated(pos, targetState, targetState, Block.UPDATE_ALL);
                BlockState deviceState = trans.getBlockState();
                level.sendBlockUpdated(trans.getBlockPos(), deviceState, deviceState, Block.UPDATE_ALL);
            }
        }

        return InteractionResult.CONSUME;
    }

    /** {@code itemInteractionForEntity}: sneak-click a golem to bind the remembered Golem Connector to it. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.isShiftKeyDown() || !isGolem(target)) return InteractionResult.PASS;
        Level level = target.level();

        if (!hasTarget(stack)) {
            if (level.isClientSide) return InteractionResult.PASS;
            player.sendSystemMessage(Component.translatable("ttmisc.golemconnector.notinterf"));
            return InteractionResult.CONSUME;
        }
        BlockPos origin = getTarget(stack);
        BlockEntity tile1 = level.hasChunkAt(origin) ? level.getBlockEntity(origin) : null;
        if (!(tile1 instanceof TileGolemConnector trans)) {
            clearTarget(stack);
            if (level.isClientSide) return InteractionResult.PASS;
            player.sendSystemMessage(Component.translatable("ttmisc.golemconnector.notpresent"));
            return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;

        trans.ConnectGolem(target.getUUID());

        clearTarget(stack);

        playSound(level, target.blockPosition());
        player.sendSystemMessage(Component.translatable("ttmisc.golemconnector.complete"));
        BlockState state = trans.getBlockState();
        level.sendBlockUpdated(trans.getBlockPos(), state, state, Block.UPDATE_ALL);
        return InteractionResult.CONSUME;
    }

    /** A Thaumcraft golem, recognised by its entity type (the API has no golem type). */
    public static boolean isGolem(net.minecraft.world.entity.Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(GOLEM_TYPE);
    }

    /** 1.7.10 {@code random.orb}, volume 0.8. */
    private void playSound(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1F);
        }
    }
}
