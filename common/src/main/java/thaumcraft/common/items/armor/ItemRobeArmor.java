package thaumcraft.common.items.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;

/** Original ItemRobeArmor:61-163. Repair/runic interfaces are eligibility markers,
 * not an invented repair enchantment, shield or automatic enchantment glint. */
public class ItemRobeArmor extends ArmorItem implements IRepairable, IRunicArmor, IVisDiscountGear, DyedRobe {
    public static final int DEFAULT_COLOR = 0x6A3880;
    private static final DyedItemColor DEFAULT_DYED_COLOR = new DyedItemColor(DEFAULT_COLOR, false);

    public ItemRobeArmor(Holder<ArmorMaterial> material, Type type, Properties properties) {
        // Original SPECIAL25 * vanilla1.7 chest16/legs15/boots13, not goggles350.
        super(material, type, properties.durability(switch (type) {
            case CHESTPLATE -> 400;
            case LEGGINGS -> 375;
            case BOOTS -> 325;
            default -> throw new IllegalArgumentException("Enchanted robes have no helmet or body item");
        }).rarity(Rarity.UNCOMMON).component(DataComponents.DYED_COLOR, DEFAULT_DYED_COLOR));
    }

    @Override public int getRunicCharge(ItemStack stack) { return 0; }
    @Override public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) { return type == Type.BOOTS ? 1 : 2; }

    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        // Original SPECIAL has no metal repair fallback. Never call the native
        // goggles' gold-repair material or accept wool/ordinary leather here.
        return repair.is(ThaumcraftContent.MATERIALS.get("enchanted_fabric").get());
    }

    /** Original hasColor is ALWAYS true: default purple contributes to first dye. */
    public boolean hasColor(ItemStack stack) { return true; }

    /** 24-bit RGB, NOT ARGB. Native item tint callers must add opaque alpha255. */
    public static int color(ItemStack stack) {
        return stack.getOrDefault(DataComponents.DYED_COLOR, DEFAULT_DYED_COLOR).rgb() & 0xFFFFFF;
    }

    /**
     * The original armour tint of a dyed robe: the dyed colour, opaque, with the same default
     * the native armour layer uses for a dyeable layer.
     *
     * <p>The Fabric half of the port reaches this value through the DYEABLE check of the native
     * armour layer ({@code thaumcraft.fabric.mixin.client.FabricRobeArmorLayerMixin}); the
     * NeoForge half reaches it through the native
     * {@code IClientItemExtensions#getDefaultDyeColor} of the robe items, because the NeoForge
     * patch of the armour layer has no DYEABLE check at all.</p>
     */
    public static int armorTint(ItemStack stack) {
        return net.minecraft.util.FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, -6265536));
    }

    public static void setColor(ItemStack stack, int rgb) {
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb & 0xFFFFFF, false));
    }

    public static void removeColor(ItemStack stack) {
        // Restore the prototype, not component absence: HumanoidArmorLayer uses
        // leather brown when DYED_COLOR is absent even on a dyeable material layer.
        stack.set(DataComponents.DYED_COLOR, DEFAULT_DYED_COLOR);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        tooltip.add(Component.translatable("tc.visdiscount").append(": " + getVisDiscount(stack, null, null) + "%")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        InteractionResult washed = wash(context);
        return washed != null ? washed : super.useOn(context);
    }

    /**
     * Original onItemUseFirst of both robe classes: a robe used on a water cauldron holding at least one level loses its custom color
     * and lowers the cauldron one level. Null when the click is not that wash (the caller then falls back to the item's own use).
     */
    public static InteractionResult wash(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (!state.is(Blocks.WATER_CAULDRON) || state.getValue(LayeredCauldronBlock.LEVEL) <= 0) return null;
        // Native AbstractCauldronBlock looks up its interaction by exact Item;
        // unregistered robes get PASS_TO_DEFAULT_BLOCK_INTERACTION, then this
        // actual useOn fallback. Do NOT add robes to ItemTags.DYEABLE or install
        // vanilla DYED_ITEM: both would substitute vanilla color semantics.
        if (!level.isClientSide) {
            removeColor(context.getItemInHand());
            // Native level/empty-block conversion, comparator update and block
            // change notification. No XP, custom-color or creative-mode waiver.
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
