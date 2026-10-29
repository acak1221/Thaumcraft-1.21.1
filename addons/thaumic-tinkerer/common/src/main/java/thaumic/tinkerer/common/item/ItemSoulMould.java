/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [12 Sep 2013, 17:36:35 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererCrucibleRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;

/**
 * The soul mould. 1.7.10 stored the pattern as the legacy entity name ({@code EntityList.getEntityString}) in the
 * item NBT key {@code pattern}; the stack carries the registry id of the entity type ({@code minecraft:zombie}) in the
 * same key of its custom data.
 */
public class ItemSoulMould extends Item implements ITTinkererRegisterable {

    private static final String TAG_PATTERN = "pattern";

    public ItemSoulMould(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static void setPattern(ItemStack stack, LivingEntity entity) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TAG_PATTERN, id));
    }

    /** The registry id of the entity type this mould is set to, or the empty string. */
    public static String getPatternName(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString(TAG_PATTERN);
    }

    private static void clearPattern(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(TAG_PATTERN));
        CustomData left = stack.get(DataComponents.CUSTOM_DATA);
        if (left != null && left.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
    }

    /** The name of the entity type of a pattern, as the game shows it. */
    public static Component patternDisplayName(String pattern) {
        ResourceLocation id = ResourceLocation.tryParse(pattern);
        if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return BuiltInRegistries.ENTITY_TYPE.get(id).getDescription();
        }
        return Component.literal(pattern);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return null;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererCrucibleRecipe(LibResearch.KEY_MAGNETS, new ItemStack(this),
                new ItemStack(Items.ENDER_PEARL),
                new AspectList().add(Aspect.BEAST, 4).add(Aspect.MIND, 8).add(Aspect.SENSES, 8));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
            InteractionHand hand) {
        if (target instanceof Player) return InteractionResult.sidedSuccess(player.level().isClientSide);
        if (!player.level().isClientSide) {
            // 1.7.10 set the pattern on the stack the player holds (getCurrentEquippedItem). In 1.21.1 Player.interactOn
            // of a creative player passes a COPY of the held stack here, so the pattern would be lost.
            ItemStack held = player.getItemInHand(hand);
            setPattern(held.getItem() instanceof ItemSoulMould ? held : stack, target);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide) clearPattern(stack);
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            net.minecraft.world.item.TooltipFlag flag) {
        String name = getPatternName(stack);

        if (name.isEmpty()) tooltip.add(Component.translatable("ttmisc.soulMould.nonAssigned"));
        else tooltip.add(Component.translatable("ttmisc.soulMould.pattern", patternDisplayName(name)));
    }
}
