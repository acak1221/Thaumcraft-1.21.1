package thaumcraft.common.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.items.BehaviorDispenseAlumetum;
import thaumcraft.common.items.ItemBottleTaint;
import thaumcraft.common.items.ItemBucketDeath;
import thaumcraft.common.items.ItemCompassStone;
import thaumcraft.common.items.ItemEldritchObject;
import thaumcraft.common.items.ItemKey;
import thaumcraft.common.items.ItemNuggetEdible;
import thaumcraft.common.items.ItemTripleMeatTreat;
import thaumcraft.common.items.equipment.ItemBowBone;
import thaumcraft.common.items.equipment.ItemCrimsonSword;
import thaumcraft.common.items.equipment.ItemPrimalArrow;
import thaumcraft.common.items.equipment.ItemPrimalCrusher;
import thaumcraft.common.items.relics.ItemHandMirror;
import thaumcraft.common.items.relics.ItemSanityChecker;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The equipment and relic items of the original that have no item of their own in this port: the bone bow, the crimson blade, the primal
 * arrows, the primal crusher, the hand mirror, the sanity checker, the edible nuggets, the bottle of taint, the bucket of death, the keys,
 * the triple meat treat, the compass stone and the two eldritch objects that are not ported elsewhere.
 *
 * <p>The metadata of the original is carried by the items themselves here, so every variant of an original metadata item is an item of its
 * own. The eldritch eye, the crimson rites and the primordial pearl of the original are already items of their own in this port.
 */
public final class ThaumcraftLegacyItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);

    /** The blocks the primal crusher is incorrect for: none, it is above netherite. */
    public static final TagKey<Block> INCORRECT_FOR_PRIMAL_CRUSHER = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "incorrect_for_primal_crusher"));

    /**
     * The material of the crimson blade: Api toolMatCrimsonVoid, harvest level four, two hundred uses, an efficiency of eight, three and a
     * half points of damage and an enchantability of twenty. It is repaired with the primal charm, the original's own quirk.
     */
    public static final Tier CRIMSON_TIER = new Tier() {
        @Override public int getUses() { return EquipmentRules.CRIMSON_SWORD_USES; }
        @Override public float getSpeed() { return EquipmentRules.CRIMSON_SWORD_EFFICIENCY; }
        @Override public float getAttackDamageBonus() { return EquipmentRules.CRIMSON_SWORD_MATERIAL_DAMAGE; }
        @Override public int getEnchantmentValue() { return EquipmentRules.CRIMSON_SWORD_ENCHANTABILITY; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ThaumcraftContent.MATERIALS.get("primal_charm").get()); }
    };

    /**
     * The material of the primal crusher: Api material PRIMALVOID, harvest level five, five hundred uses, an efficiency of eight, four points
     * of damage and an enchantability of twenty. It is repaired with the primal charm, like the void tools.
     */
    public static final Tier PRIMAL_CRUSHER_TIER = new Tier() {
        @Override public int getUses() { return EquipmentRules.PRIMAL_CRUSHER_USES; }
        @Override public float getSpeed() { return EquipmentRules.PRIMAL_CRUSHER_EFFICIENCY; }
        @Override public float getAttackDamageBonus() { return EquipmentRules.PRIMAL_CRUSHER_MATERIAL_DAMAGE; }
        @Override public int getEnchantmentValue() { return EquipmentRules.PRIMAL_CRUSHER_ENCHANTABILITY; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return INCORRECT_FOR_PRIMAL_CRUSHER; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ThaumcraftContent.MATERIALS.get("primal_charm").get()); }
    };

    /** The link of a hand mirror. */
    public static final Codec<ItemHandMirror.MirrorLink> MIRROR_LINK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(ItemHandMirror.MirrorLink::x),
            Codec.INT.fieldOf("y").forGetter(ItemHandMirror.MirrorLink::y),
            Codec.INT.fieldOf("z").forGetter(ItemHandMirror.MirrorLink::z),
            Codec.STRING.fieldOf("dimension").forGetter(ItemHandMirror.MirrorLink::dimension),
            Codec.STRING.fieldOf("dimension_name").forGetter(ItemHandMirror.MirrorLink::dimensionName)
    ).apply(instance, ItemHandMirror.MirrorLink::new));
    public static final StreamCodec<io.netty.buffer.ByteBuf, ItemHandMirror.MirrorLink> MIRROR_LINK_STREAM = StreamCodec.composite(
            ByteBufCodecs.INT, ItemHandMirror.MirrorLink::x,
            ByteBufCodecs.INT, ItemHandMirror.MirrorLink::y,
            ByteBufCodecs.INT, ItemHandMirror.MirrorLink::z,
            ByteBufCodecs.STRING_UTF8, ItemHandMirror.MirrorLink::dimension,
            ByteBufCodecs.STRING_UTF8, ItemHandMirror.MirrorLink::dimensionName,
            ItemHandMirror.MirrorLink::new);
    public static final RegistrySupplier<DataComponentType<ItemHandMirror.MirrorLink>> MIRROR_LINK = COMPONENTS.register("mirror_link",
            () -> DataComponentType.<ItemHandMirror.MirrorLink>builder().persistent(MIRROR_LINK_CODEC).networkSynchronized(MIRROR_LINK_STREAM).build());

    /** The link of a key: the door it was linked to and the kind of door. */
    public static final Codec<ItemKey.KeyLink> KEY_LINK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("location").forGetter(ItemKey.KeyLink::location),
            Codec.INT.fieldOf("type").forGetter(ItemKey.KeyLink::type)
    ).apply(instance, ItemKey.KeyLink::new));
    public static final StreamCodec<io.netty.buffer.ByteBuf, ItemKey.KeyLink> KEY_LINK_STREAM = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ItemKey.KeyLink::location,
            ByteBufCodecs.INT, ItemKey.KeyLink::type,
            ItemKey.KeyLink::new);
    public static final RegistrySupplier<DataComponentType<ItemKey.KeyLink>> KEY_LINK = COMPONENTS.register("key_link",
            () -> DataComponentType.<ItemKey.KeyLink>builder().persistent(KEY_LINK_CODEC).networkSynchronized(KEY_LINK_STREAM).build());

    /** The bone bow, original ItemBowBone. */
    public static final RegistrySupplier<ItemBowBone> BONE_BOW = ITEMS.register("bone_bow", () -> new ItemBowBone(new Item.Properties()));
    /** The crimson blade, original ItemCrimsonSword. */
    public static final RegistrySupplier<ItemCrimsonSword> CRIMSON_SWORD = ITEMS.register("crimson_sword",
            () -> new ItemCrimsonSword(CRIMSON_TIER, new Item.Properties()));
    /** The primal crusher, original ItemPrimalCrusher. */
    public static final RegistrySupplier<ItemPrimalCrusher> PRIMAL_CRUSHER = ITEMS.register("primal_crusher",
            () -> new ItemPrimalCrusher(PRIMAL_CRUSHER_TIER, new Item.Properties()));
    /** The hand mirror, original ItemHandMirror. */
    public static final RegistrySupplier<ItemHandMirror> HAND_MIRROR = ITEMS.register("hand_mirror",
            () -> new ItemHandMirror(new Item.Properties()));
    /** The sanity checker, original ItemSanityChecker. */
    public static final RegistrySupplier<ItemSanityChecker> SANITY_CHECKER = ITEMS.register("sanity_checker",
            () -> new ItemSanityChecker(new Item.Properties()));
    /** The bottle of taint, original ItemBottleTaint. */
    public static final RegistrySupplier<ItemBottleTaint> BOTTLE_TAINT = ITEMS.register("bottle_taint",
            () -> new ItemBottleTaint(new Item.Properties()));
    /** The bucket of death, original ItemBucketDeath. */
    public static final RegistrySupplier<ItemBucketDeath> BUCKET_DEATH = ITEMS.register("bucket_death",
            () -> new ItemBucketDeath(new Item.Properties()));
    /** The iron key, original ItemKey metadata zero. */
    public static final RegistrySupplier<ItemKey> KEY_IRON = ITEMS.register("key_iron",
            () -> new ItemKey(new Item.Properties(), EquipmentRules.KEY_IRON));
    /** The gold key, original ItemKey metadata one. */
    public static final RegistrySupplier<ItemKey> KEY_GOLD = ITEMS.register("key_gold",
            () -> new ItemKey(new Item.Properties(), EquipmentRules.KEY_GOLD));
    /** The triple meat treat, original ItemTripleMeatTreat. */
    public static final RegistrySupplier<ItemTripleMeatTreat> TRIPLE_MEAT_TREAT = ITEMS.register("triple_meat_treat",
            () -> new ItemTripleMeatTreat(new Item.Properties()));
    /** The compass stone, original ItemCompassStone. */
    public static final RegistrySupplier<ItemCompassStone> COMPASS_STONE = ITEMS.register("compass_stone",
            () -> new ItemCompassStone(new Item.Properties()));
    /** The runed tablet, original ItemEldritchObject metadata two. */
    public static final RegistrySupplier<ItemEldritchObject> ELDRITCH_TABLET = ITEMS.register("eldritch_tablet",
            () -> new ItemEldritchObject(new Item.Properties(), EquipmentRules.ELDRITCH_TABLET));
    /** The eldritch obelisk placer, original ItemEldritchObject metadata four. */
    public static final RegistrySupplier<ItemEldritchObject> ELDRITCH_PLACER = ITEMS.register("eldritch_placer",
            () -> new ItemEldritchObject(new Item.Properties(), EquipmentRules.ELDRITCH_OBELISK_PLACER));

    /** The six primal arrows, original ItemPrimalArrow metadata zero to five. */
    public static final RegistrySupplier<ItemPrimalArrow>[] PRIMAL_ARROWS = arrows();
    /** The four edible nuggets, original ItemNuggetEdible metadata zero to three. */
    public static final RegistrySupplier<ItemNuggetEdible>[] EDIBLE_NUGGETS = edibleNuggets();

    @SuppressWarnings("unchecked")
    private static RegistrySupplier<ItemPrimalArrow>[] arrows() {
        String[] ids = {"primal_arrow_air", "primal_arrow_fire", "primal_arrow_water", "primal_arrow_earth",
                "primal_arrow_order", "primal_arrow_entropy"};
        RegistrySupplier<ItemPrimalArrow>[] result = new RegistrySupplier[ids.length];
        for (int type = 0; type < ids.length; type++) {
            final int metadata = type;
            result[type] = ITEMS.register(ids[type], () -> new ItemPrimalArrow(new Item.Properties(), metadata));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static RegistrySupplier<ItemNuggetEdible>[] edibleNuggets() {
        String[] ids = {"nugget_chicken", "nugget_beef", "nugget_pork", "nugget_fish"};
        RegistrySupplier<ItemNuggetEdible>[] result = new RegistrySupplier[ids.length];
        for (int type = 0; type < ids.length; type++) {
            final int metadata = type;
            result[type] = ITEMS.register(ids[type], () -> new ItemNuggetEdible(new Item.Properties(), metadata));
        }
        return result;
    }

    private static boolean registered;
    private ThaumcraftLegacyItems() {}

    public static void register() {
        if (registered) return;
        registered = true;
        COMPONENTS.register();
        ITEMS.register();
        for (RegistrySupplier<? extends Item> item : List.of(BONE_BOW, CRIMSON_SWORD, PRIMAL_CRUSHER, HAND_MIRROR, SANITY_CHECKER,
                BOTTLE_TAINT, BUCKET_DEATH, KEY_IRON, KEY_GOLD, TRIPLE_MEAT_TREAT, COMPASS_STONE, ELDRITCH_TABLET, ELDRITCH_PLACER)) {
            CreativeTabRegistry.append(ThaumcraftContent.TAB, item);
        }
        for (RegistrySupplier<ItemPrimalArrow> arrow : PRIMAL_ARROWS) CreativeTabRegistry.append(ThaumcraftContent.TAB, arrow);
        for (RegistrySupplier<ItemNuggetEdible> nugget : EDIBLE_NUGGETS) CreativeTabRegistry.append(ThaumcraftContent.TAB, nugget);
        // The original dispenser behaviour of the alumentum, with its own launch sound and lift.
        ThaumcraftContent.MATERIALS.get("alumentum").listen(item -> DispenserBlock.registerBehavior(item, new BehaviorDispenseAlumetum()));
    }
}
