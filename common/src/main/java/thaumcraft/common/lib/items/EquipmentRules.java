package thaumcraft.common.lib.items;

import thaumcraft.common.lib.world.EldritchBlockRules;

import java.util.List;

/**
 * The numbers and decisions of the original TC4 equipment and relic items that the 1.21.1 item classes call. Everything here is a pure
 * static function or a constant so that the behaviour can be unit tested without a world.
 *
 * <p>Sources are the original 4.2.3.5 classes: {@code thaumcraft.common.items.equipment.ItemBowBone},
 * {@code ItemCrimsonSword}, {@code ItemPrimalArrow}, {@code ItemPrimalCrusher},
 * {@code thaumcraft.common.items.relics.ItemHandMirror}, {@code ItemSanityChecker},
 * {@code thaumcraft.common.items.ItemNugget}, {@code ItemNuggetEdible}, {@code ItemResource}, {@code ItemShard},
 * {@code ItemBottleTaint}, {@code ItemBucketDeath}, {@code ItemKey}, {@code ItemTripleMeatTreat}, {@code ItemCompassStone},
 * {@code ItemEldritchObject} and {@code BehaviorDispenseAlumetum}.
 */
public final class EquipmentRules {
    private EquipmentRules() {}

    // ---------------------------------------------------------------------------------------------------------------
    // Bone Bow (original ItemBowBone)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original ItemBowBone constructor: {@code func_77656_e(512)}. */
    public static final int BONE_BOW_DURABILITY = 512;
    /** Original ItemBowBone: {@code func_77619_b()} returns 3. */
    public static final int BONE_BOW_ENCHANTABILITY = 3;
    /** Original ItemBowBone constructor: {@code field_77777_bU = 1}. */
    public static final int BONE_BOW_MAX_STACK_SIZE = 1;
    /**
     * Original ItemBowBone.onUsingTick stops the player's hand once more than eighteen ticks of drawing have passed. The bow therefore
     * always fires at full draw when it is held.
     */
    public static final int BONE_BOW_MAX_DRAW_TICKS = 18;
    /**
     * The original divides the draw time by ten instead of the vanilla twenty, so the bone bow reaches full power in half the time
     * (original ItemBowBone.func_77615_a: {@code f = (float)j / 10.0f}).
     */
    public static final float BONE_BOW_DRAW_DIVISOR = 10.0F;
    /** Original ItemBowBone.func_77615_a: a draw below this power does not fire. */
    public static final float BONE_BOW_MIN_POWER = 0.1F;
    /** Original ItemBowBone.func_77615_a: the arrow leaves the bow with {@code f * 2.5f} speed. */
    public static final float BONE_BOW_ARROW_SPEED = 2.5F;
    /** A vanilla arrow carries two points of damage (original EntityArrow) and the bone bow adds half a point. */
    public static final double BONE_BOW_ARROW_BASE_DAMAGE = 2.5D;
    /** Original ItemBowBone.func_77615_a: the Power enchantment adds half a point per level and another half point. */
    public static final double BONE_BOW_POWER_BONUS = 0.5D;

    /**
     * The draw power of the bone bow. Original ItemBowBone.func_77615_a: {@code f = j / 10; f = (f * f + f * 2) / 3}, capped at one.
     */
    public static float boneBowPower(int drawTicks) {
        float f = drawTicks / BONE_BOW_DRAW_DIVISOR;
        f = (f * f + f * 2.0F) / 3.0F;
        return f > 1.0F ? 1.0F : f;
    }

    /** Whether a draw of this many ticks releases an arrow at all (original: the power has to reach 0.1). */
    public static boolean boneBowFires(int drawTicks) { return boneBowPower(drawTicks) >= BONE_BOW_MIN_POWER; }

    /** Whether the bow lets go of the player's hand by itself (original ItemBowBone.onUsingTick). */
    public static boolean boneBowAutoRelease(int elapsedDrawTicks) { return elapsedDrawTicks > BONE_BOW_MAX_DRAW_TICKS; }

    /**
     * The damage the arrow of the bone bow carries, exactly as the original computed it: two and a half points, plus the original
     * Power bonus of half a point per level and another half point when the bow carries Power at all.
     */
    public static double boneBowArrowDamage(int powerLevel) {
        return BONE_BOW_ARROW_BASE_DAMAGE + (powerLevel > 0 ? powerLevel * BONE_BOW_POWER_BONUS + BONE_BOW_POWER_BONUS : 0.0D);
    }

    /**
     * The damage the 1.21.1 arrow itself carries. The native Power effect adds half a point per level when the arrow hits, so the half
     * point the original added together with the Power levels is carried here instead; the sum is the original damage.
     */
    public static double boneBowArrowBaseDamage(int powerLevel) {
        return BONE_BOW_ARROW_BASE_DAMAGE + (powerLevel > 0 ? BONE_BOW_POWER_BONUS : 0.0D);
    }

    /** The speed the arrow of the bone bow leaves with (original ItemBowBone.func_77615_a). */
    public static float boneBowArrowSpeed(float power) { return power * BONE_BOW_ARROW_SPEED; }

    /**
     * The speed the primal arrow of the bone bow leaves with. Original EventHandlerEntity.bowShot: the primal arrow is created with the
     * charge of the bow times the arrow damage of the bow, and the original EntityPrimalArrow leaves with one and a half times that
     * value. The vanilla bow already leaves with three times its charge, which is the original's two times one and a half.
     */
    public static float boneBowPrimalArrowSpeed(float arrowSpeed) { return arrowSpeed * PRIMAL_ARROW_SPEED_FACTOR; }

    /**
     * The element of the primal arrow a bow fires. Original EventHandlerEntity.bowShot: the shot is replaced by a primal arrow as soon
     * as the player carries one, and the element is the one of the first primal arrow of the player's inventory. The types are given in
     * the order of the original's main inventory array, the hotbar first.
     */
    public static int firstPrimalArrowType(int[] inventoryTypes) {
        for (int type : inventoryTypes) if (type >= 0) return type;
        return ARROW_AIR;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Crimson Blade (original ItemCrimsonSword)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original ItemCrimsonSword: the tool material {@code CVOID}. */
    public static final String CRIMSON_SWORD_MATERIAL = "CVOID";
    /** Original toolMatCrimsonVoid: harvest level four, above diamond. */
    public static final int CRIMSON_SWORD_HARVEST_LEVEL = 4;
    /** Original toolMatCrimsonVoid: two hundred uses. */
    public static final int CRIMSON_SWORD_USES = 200;
    /** Original toolMatCrimsonVoid: efficiency eight. */
    public static final float CRIMSON_SWORD_EFFICIENCY = 8.0F;
    /** Original toolMatCrimsonVoid: three and a half points of damage against entities. */
    public static final float CRIMSON_SWORD_MATERIAL_DAMAGE = 3.5F;
    /** Original toolMatCrimsonVoid: enchantability twenty. */
    public static final int CRIMSON_SWORD_ENCHANTABILITY = 20;
    /** Original ItemSword adds four points of its own to the material damage. */
    public static final float SWORD_BASE_DAMAGE = 4.0F;
    /** Original ItemCrimsonSword.getWarp: two points of warp. */
    public static final int CRIMSON_SWORD_WARP = 2;
    /** Original ItemCrimsonSword.hitEntity: the sap weakness lasts sixty ticks. */
    public static final int CRIMSON_SWORD_SAP_WEAKNESS_TICKS = 60;
    /** Original ItemCrimsonSword.hitEntity: the sap hunger lasts a hundred and twenty ticks. */
    public static final int CRIMSON_SWORD_SAP_HUNGER_TICKS = 120;
    /** Original ItemCrimsonSword.onUpdate mends the blade by one point every twenty ticks of the carrier's age. */
    public static final int SELF_REPAIR_PERIOD = 20;

    /** The attack damage of the crimson blade: the sword's four points plus the material's three and a half. */
    public static double crimsonSwordDamage() { return SWORD_BASE_DAMAGE + CRIMSON_SWORD_MATERIAL_DAMAGE; }

    /**
     * Whether the sap of the crimson blade reaches a target. Original ItemCrimsonSword.hitEntity: the sap is skipped on the client and
     * when a player hits a player while player versus player is switched off.
     */
    public static boolean crimsonSwordSaps(boolean clientSide, boolean targetIsPlayer, boolean attackerIsPlayer, boolean pvpAllowed) {
        if (clientSide) return false;
        return !(targetIsPlayer && attackerIsPlayer && !pvpAllowed);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Primal Crusher (original ItemPrimalCrusher)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original ItemPrimalCrusher: the tool material {@code PRIMALVOID}. */
    public static final String PRIMAL_CRUSHER_MATERIAL = "PRIMALVOID";
    /** Original material: harvest level five, above netherite. */
    public static final int PRIMAL_CRUSHER_HARVEST_LEVEL = 5;
    /** Original material: five hundred uses. */
    public static final int PRIMAL_CRUSHER_USES = 500;
    /** Original material: efficiency eight. */
    public static final float PRIMAL_CRUSHER_EFFICIENCY = 8.0F;
    /** Original material: four points of damage against entities. */
    public static final float PRIMAL_CRUSHER_MATERIAL_DAMAGE = 4.0F;
    /** Original material: enchantability twenty. */
    public static final int PRIMAL_CRUSHER_ENCHANTABILITY = 20;
    /** Original ItemPrimalCrusher constructor: {@code super(3.5f, material, isEffective)} - the tool's own damage. */
    public static final float PRIMAL_CRUSHER_TOOL_DAMAGE = 3.5F;
    /** The attack speed of the crusher, the pickaxe value the original 1.12 port kept. */
    public static final float PRIMAL_CRUSHER_ATTACK_SPEED = -2.8F;
    /** Original ItemPrimalCrusher.getWarp: two points of warp. */
    public static final int PRIMAL_CRUSHER_WARP = 2;

    /**
     * The nine block offsets the crusher harvests around the block that was broken (original
     * ItemPrimalCrusher.func_150894_a). The three by three plane lies across the face the player was looking at: down and up harvest the
     * x and z plane, north and south the x and y plane, west and east the z and y plane. The centre block is part of the original loop.
     */
    public static int[] primalCrusherOffsets(int side) {
        int[] offsets = new int[27];
        int index = 0;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                int x = 0, y = 0, z = 0;
                if (side <= 1) { x = a; z = b; }
                else if (side <= 3) { x = a; y = b; }
                else { z = a; y = b; }
                offsets[index++] = x;
                offsets[index++] = y;
                offsets[index++] = z;
            }
        }
        return offsets;
    }

    /**
     * Whether the crusher may harvest a block at all. The original breaks the neighbours only when the tool is effective against them,
     * either through the tool's own class or through the crusher's list of effective blocks.
     */
    public static boolean primalCrusherHarvests(boolean toolEffective, boolean inEffectiveBlocks) { return toolEffective || inEffectiveBlocks; }

    // ---------------------------------------------------------------------------------------------------------------
    // Primal Arrow (original ItemPrimalArrow and EntityPrimalArrow)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original ItemPrimalArrow metadata: the air arrow. */
    public static final int ARROW_AIR = 0;
    /** Original ItemPrimalArrow metadata: the fire arrow. */
    public static final int ARROW_FIRE = 1;
    /** Original ItemPrimalArrow metadata: the water arrow. */
    public static final int ARROW_WATER = 2;
    /** Original ItemPrimalArrow metadata: the earth arrow. */
    public static final int ARROW_EARTH = 3;
    /** Original ItemPrimalArrow metadata: the order arrow. */
    public static final int ARROW_ORDER = 4;
    /** Original ItemPrimalArrow metadata: the entropy arrow. */
    public static final int ARROW_ENTROPY = 5;
    /** Original EntityPrimalArrow: the arrow carries two point one points of damage. */
    public static final double PRIMAL_ARROW_BASE_DAMAGE = 2.1D;
    /** Original EntityPrimalArrow.func_70242_d: the earth arrow's damage is multiplied by one and a half. */
    public static final double PRIMAL_ARROW_EARTH_FACTOR = 1.5D;
    /** Original EntityPrimalArrow.func_70242_d: the order and entropy arrows' damage is multiplied by 0.8. */
    public static final double PRIMAL_ARROW_WEAK_FACTOR = 0.8D;
    /** Original EntityPrimalArrow constructor: the arrow leaves with {@code par3 * 1.5f} speed. */
    public static final float PRIMAL_ARROW_SPEED_FACTOR = 1.5F;
    /** Original EntityPrimalArrow.inflictDamage: the fire arrow sets the target alight for five seconds. */
    public static final int PRIMAL_ARROW_FIRE_TICKS = 5;

    /** The aspect tags of the six primal arrows, in the original metadata order. */
    public static final List<String> PRIMAL_ARROW_ASPECTS = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio");

    /** The damage the primal arrow of this element carries (original EntityPrimalArrow.func_70242_d). */
    public static double primalArrowDamage(int type) {
        return switch (type) {
            case ARROW_EARTH -> PRIMAL_ARROW_BASE_DAMAGE * PRIMAL_ARROW_EARTH_FACTOR;
            case ARROW_ORDER, ARROW_ENTROPY -> PRIMAL_ARROW_BASE_DAMAGE * PRIMAL_ARROW_WEAK_FACTOR;
            default -> PRIMAL_ARROW_BASE_DAMAGE;
        };
    }

    /** The aspect tag of a primal arrow, or null when the metadata is not one of the six. */
    public static String primalArrowAspect(int type) {
        return type >= 0 && type < PRIMAL_ARROW_ASPECTS.size() ? PRIMAL_ARROW_ASPECTS.get(type) : null;
    }

    /** The seconds a burning primal arrow sets its target alight for; the water arrow never burns (original EntityPrimalArrow). */
    public static int primalArrowFireTicks(int type, boolean burning) { return burning && type != ARROW_WATER ? PRIMAL_ARROW_FIRE_TICKS : 0; }

    /** The extra fire the fire arrow adds to a burning arrow (original EntityPrimalArrow.inflictDamage: five more ticks). */
    public static int primalArrowExtraFire(int type) { return type == ARROW_FIRE ? PRIMAL_ARROW_FIRE_TICKS : 0; }

    // ---------------------------------------------------------------------------------------------------------------
    // Hand Mirror (original ItemHandMirror)
    // ---------------------------------------------------------------------------------------------------------------

    /** The registry path of the mirror block the hand mirror links to (original ConfigBlocks.blockMirror). */
    public static final String MIRROR_BLOCK_PATH = "thaumcraft:mirror";
    /** Original ItemHandMirror.transport pushes the item out of the linked mirror by this much of a block. */
    public static final double MIRROR_PUSH_OFFSET = 0.3D;
    /** Original ItemHandMirror.transport: the item leaves the mirror with this speed. */
    public static final double MIRROR_ITEM_SPEED = 0.15D;
    /** Original ItemHandMirror.transport: the item is given twenty ticks to live in the mirror. */
    public static final int MIRROR_ITEM_LIFETIME = 20;

    /** Whether a block id is the mirror the hand mirror can be linked to. */
    public static boolean isMirrorBlock(String blockId) { return MIRROR_BLOCK_PATH.equals(blockId); }

    // ---------------------------------------------------------------------------------------------------------------
    // Sanity Checker (original ItemSanityChecker, read with the 1.12 reference of the same item)
    // ---------------------------------------------------------------------------------------------------------------

    /** The language key of the sanity reading (original tc.sanity). */
    public static final String SANITY_MESSAGE = "tc.sanity";
    /** The language key of the detailed reading (original tc.sanity.detail). */
    public static final String SANITY_DETAIL_MESSAGE = "tc.sanity.detail";

    /** The warp the sanity checker reads: the permanent, the sticky and the temporary warp together. */
    public static int sanity(int permanentWarp, int stickyWarp, int temporaryWarp) {
        return permanentWarp + stickyWarp + temporaryWarp;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Nugs, resources and shards (original ItemNugget, ItemNuggetEdible, ItemResource, ItemShard)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original ItemNugget metadata: the iron nugget of the other mods. */
    public static final int NUGGET_IRON = 0;
    /** Original ItemNugget metadata: the copper nugget of the other mods. */
    public static final int NUGGET_COPPER = 1;
    /** Original ItemNugget metadata: the tin nugget of the other mods. */
    public static final int NUGGET_TIN = 2;
    /** Original ItemNugget metadata: the silver nugget of the other mods. */
    public static final int NUGGET_SILVER = 3;
    /** Original ItemNugget metadata: the lead nugget of the other mods. */
    public static final int NUGGET_LEAD = 4;
    /** Original ItemNugget metadata: the quicksilver drop. */
    public static final int NUGGET_QUICKSILVER = 5;
    /** Original ItemNugget metadata: the thaumium nugget. */
    public static final int NUGGET_THAUMIUM = 6;
    /** Original ItemNugget metadata: the void metal nugget. */
    public static final int NUGGET_VOID = 7;
    /** Original ItemNugget metadata: the native iron cluster. */
    public static final int NUGGET_CLUSTER_IRON = 16;
    /** Original ItemNugget metadata: the native copper cluster. */
    public static final int NUGGET_CLUSTER_COPPER = 17;
    /** Original ItemNugget metadata: the native tin cluster. */
    public static final int NUGGET_CLUSTER_TIN = 18;
    /** Original ItemNugget metadata: the native silver cluster. */
    public static final int NUGGET_CLUSTER_SILVER = 19;
    /** Original ItemNugget metadata: the native lead cluster. */
    public static final int NUGGET_CLUSTER_LEAD = 20;
    /** Original ItemNugget metadata: the native cinnabar cluster. */
    public static final int NUGGET_CLUSTER_CINNABAR = 21;
    /** Original ItemNugget metadata: the native gold cluster. */
    public static final int NUGGET_CLUSTER_GOLD = 31;

    /** Original ItemNuggetEdible metadata: the chicken nugget. */
    public static final int NUGGET_EDIBLE_CHICKEN = 0;
    /** Original ItemNuggetEdible metadata: the beef nugget. */
    public static final int NUGGET_EDIBLE_BEEF = 1;
    /** Original ItemNuggetEdible metadata: the pork nugget. */
    public static final int NUGGET_EDIBLE_PORK = 2;
    /** Original ItemNuggetEdible metadata: the fish nugget. */
    public static final int NUGGET_EDIBLE_FISH = 3;
    /** Original ItemNuggetEdible: one point of hunger and a third of a point of saturation. */
    public static final int NUGGET_EDIBLE_HUNGER = 1;
    /** Original ItemNuggetEdible: a third of a point of saturation. */
    public static final float NUGGET_EDIBLE_SATURATION = 0.3F;
    /** Original ItemNuggetEdible.func_77626_a: the nugget is eaten in ten ticks. */
    public static final int NUGGET_EDIBLE_USE_DURATION = 10;

    /** Original ItemResource metadata: the alumentum. */
    public static final int RESOURCE_ALUMENTUM = 0;
    /** Original ItemResource metadata: the nitor. */
    public static final int RESOURCE_NITOR = 1;
    /** Original ItemResource metadata: the thaumium ingot. */
    public static final int RESOURCE_THAUMIUM_INGOT = 2;
    /** Original ItemResource metadata: the quicksilver. */
    public static final int RESOURCE_QUICKSILVER = 3;
    /** Original ItemResource metadata: the magic tallow. */
    public static final int RESOURCE_TALLOW = 4;
    /** Original ItemResource metadata: the zombie brain. */
    public static final int RESOURCE_BRAIN = 5;
    /** Original ItemResource metadata: the amber. */
    public static final int RESOURCE_AMBER = 6;
    /** Original ItemResource metadata: the enchanted fabric. */
    public static final int RESOURCE_CLOTH = 7;
    /** Original ItemResource metadata: the vis filter. */
    public static final int RESOURCE_FILTER = 8;
    /** Original ItemResource metadata: the knowledge fragment. */
    public static final int RESOURCE_KNOWLEDGE_FRAGMENT = 9;
    /** Original ItemResource metadata: the mirrored glass. */
    public static final int RESOURCE_MIRRORED_GLASS = 10;
    /** Original ItemResource metadata: the tainted goo. */
    public static final int RESOURCE_TAINTED_GOO = 11;
    /** Original ItemResource metadata: the taint tendril. */
    public static final int RESOURCE_TAINT_TENDRIL = 12;
    /** Original ItemResource metadata: the jar label. */
    public static final int RESOURCE_LABEL = 13;
    /** Original ItemResource metadata: the salis mundus. */
    public static final int RESOURCE_SALIS_MUNDUS = 14;
    /** Original ItemResource metadata: the primal charm. */
    public static final int RESOURCE_PRIMAL_CHARM = 15;
    /** Original ItemResource metadata: the void metal ingot. */
    public static final int RESOURCE_VOID_INGOT = 16;
    /** Original ItemResource metadata: the void seed. */
    public static final int RESOURCE_VOID_SEED = 17;
    /** Original ItemResource metadata: the gold coin. */
    public static final int RESOURCE_GOLD_COIN = 18;

    /** Original ItemResource.onUpdate: the tainted resources infect their carrier once in four thousand three hundred and twenty one ticks. */
    public static final int TAINT_ITEM_CHANCE = 4321;
    /** Original ItemResource.onUpdate: the taint poison lasts a hundred and twenty ticks. */
    public static final int TAINT_ITEM_POISON_TICKS = 120;

    /** Original ItemShard metadata: the air shard. */
    public static final int SHARD_AIR = 0;
    /** Original ItemShard metadata: the fire shard. */
    public static final int SHARD_FIRE = 1;
    /** Original ItemShard metadata: the water shard. */
    public static final int SHARD_WATER = 2;
    /** Original ItemShard metadata: the earth shard. */
    public static final int SHARD_EARTH = 3;
    /** Original ItemShard metadata: the order shard. */
    public static final int SHARD_ORDER = 4;
    /** Original ItemShard metadata: the entropy shard. */
    public static final int SHARD_ENTROPY = 5;
    /** Original ItemShard metadata: the balanced shard. */
    public static final int SHARD_BALANCED = 6;

    /** The aspect tags of the seven shards, in the original metadata order; the balanced shard has none of its own. */
    public static final List<String> SHARD_ASPECTS = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio", "balanced");

    /** Whether a nugget metadata is one of the native clusters (original ItemNugget metadata 16 to 21 and 31). */
    public static boolean nuggetIsCluster(int type) {
        return type >= NUGGET_CLUSTER_IRON && type <= NUGGET_CLUSTER_CINNABAR || type == NUGGET_CLUSTER_GOLD;
    }

    /** Whether a resource metadata is one of the two tainted resources that poison their carrier. */
    public static boolean resourceIsTainted(int type) { return type == RESOURCE_TAINTED_GOO || type == RESOURCE_TAINT_TENDRIL; }

    /** The aspect tag of a shard, or null for the balanced shard and for unknown metadata. */
    public static String shardAspect(int type) {
        if (type < 0 || type >= SHARD_ASPECTS.size()) return null;
        return type == SHARD_BALANCED ? null : SHARD_ASPECTS.get(type);
    }

    /**
     * Whether a tainted resource infects its carrier on this tick. Original ItemResource.onUpdate: the roll is
     * {@code world.rand.nextInt(4321) > stack.getMaxDamage()}, so with an undamaged stack the taint comes on one tick in 4321.
     */
    public static boolean taintItemInfects(int roll, int maxDamage) { return roll > maxDamage; }

    // ---------------------------------------------------------------------------------------------------------------
    // Bottle of Taint (original ItemBottleTaint)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original ItemBottleTaint constructor: {@code field_77777_bU = 8}. */
    public static final int BOTTLE_TAINT_MAX_STACK_SIZE = 8;
    /** Original EntityBottleTaint.func_70185_h: the bottle falls a little. */
    public static final float BOTTLE_TAINT_GRAVITY = 0.05F;
    /** Original EntityBottleTaint.func_70182_d: the bottle is thrown at half a block per tick. */
    public static final float BOTTLE_TAINT_SPEED = 0.5F;
    /** Original EntityBottleTaint.func_70183_g: the bottle is thrown at minus twenty degrees. */
    public static final float BOTTLE_TAINT_PITCH = -20.0F;
    /** Original EntityBottleTaint.onUpdate: the taint spreads over a square of this many blocks around the impact. */
    public static final double BOTTLE_TAINT_SPREAD = 5.0D;
    /** Original EntityBottleTaint.onUpdate: ten patches of taint fibres are seeded. */
    public static final int BOTTLE_TAINT_PATCHES = 10;
    /** Original EntityBottleTaint.onUpdate: the taint poison lasts a hundred ticks. */
    public static final int BOTTLE_TAINT_POISON_TICKS = 100;

    // ---------------------------------------------------------------------------------------------------------------
    // Bucket of Death (original ItemBucketDeath)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original ItemBucketDeath constructor: the bucket holds one. */
    public static final int BUCKET_DEATH_MAX_STACK_SIZE = 1;
    /** The bucket of death is an infusion fluid in the original (ConfigFluids deathFluid). */
    public static final String BUCKET_DEATH_FLUID = "death";
    /** The registry path of the death fluid block of the original (ConfigBlocks.blockFluidDeath). */
    public static final String BUCKET_DEATH_BLOCK_PATH = "thaumcraft:death_fluid";

    /**
     * Whether the bucket can pour its fluid into a spot. Original ItemBucketDeath.tryPlaceContainedLiquid: the spot has to be empty
     * or made of a material that is not solid, and a replaceable liquid is not destroyed first.
     */
    public static boolean bucketDeathCanPour(boolean air, boolean solid, boolean liquid) { return air || !solid; }

    /**
     * Whether the spot has to be cleared before the death fluid is placed there. Original ItemBucketDeath.tryPlaceContainedLiquid: a
     * spot that is not solid and not a liquid is emptied first.
     */
    public static boolean bucketDeathClearsSpot(boolean air, boolean solid, boolean liquid) { return !air && !solid && !liquid; }

    // ---------------------------------------------------------------------------------------------------------------
    // Key (original ItemKey)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original ItemKey metadata: the iron key. */
    public static final int KEY_IRON = 0;
    /** Original ItemKey metadata: the gold key. */
    public static final int KEY_GOLD = 1;
    /** Original ItemKey: the key grants access to others. */
    public static final int KEY_GRANT_ACCESS = 1;

    /**
     * The access entry a key writes into the door's access list: the kind of the key followed by the name of the player. The iron key
     * writes the plain zero, the gold key the one that carries the owner's access (original ItemKey.onItemUseFirst:
     * {@code accessList.add(itemstack.getMetadata() + player.getCommandSenderName())}).
     */
    public static String keyAccessEntry(int keyType, String playerName) {
        return keyType + playerName;
    }

    /** Whether the player may take the key of a door they do not own (original ItemKey: the owner or an access entry of the right kind). */
    public static boolean keyMayLink(boolean owner, boolean hasAccess, int keyType) {
        return owner || hasAccess && keyType == KEY_IRON;
    }

    /**
     * Whether the access list of a block holds the entry of the gold key of this player (original ItemKey and BlockWoodenDevice:
     * {@code accessList.contains("1" + name)}). The gold key grants the access of the owner.
     */
    public static boolean keyHasGrantAccess(List<String> accessList, String playerName) {
        return accessList.contains(KEY_GRANT_ACCESS + playerName);
    }

    /** Whether the access list of a block already holds the entry of this key of this player. */
    public static boolean keyHasAccess(List<String> accessList, int keyType, String playerName) {
        return accessList.contains(keyAccessEntry(keyType, playerName));
    }

    /** The location string the key remembers: the x, y and z of the door (original ItemKey). */
    public static String keyLocation(int x, int y, int z) { return x + "," + y + "," + z; }

    // ---------------------------------------------------------------------------------------------------------------
    // Triple Meat Treat (original ItemTripleMeatTreat)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original ItemTripleMeatTreat: six points of hunger. */
    public static final int TREAT_HUNGER = 6;
    /** Original ItemTripleMeatTreat: four fifths of a point of saturation. */
    public static final float TREAT_SATURATION = 0.8F;
    /** Original ItemTripleMeatTreat: the regeneration effect appears two thirds of the time. */
    public static final float TREAT_EFFECT_CHANCE = 0.66F;
    /** Original ItemTripleMeatTreat: five seconds of regeneration, that is a hundred ticks. */
    public static final int TREAT_REGENERATION_TICKS = 100;

    // ---------------------------------------------------------------------------------------------------------------
    // Compass Stone (original ItemCompassStone)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original ItemCompassStone: the sinister node entries older than this many milliseconds are dropped. */
    public static final long COMPASS_STONE_EXPIRY_MILLIS = 10000L;
    /** Original ItemCompassStone: a sinister node is looked for within this range. */
    public static final float COMPASS_STONE_RANGE = 256.0F;
    /** Original ItemCompassStone: the field of view the node has to be in. */
    public static final float COMPASS_STONE_FOV = 0.66F;

    /** Whether a sinister node entry has expired (original ItemCompassStone: ten seconds). */
    public static boolean compassStoneExpired(long stamp, long now) { return stamp < now - COMPASS_STONE_EXPIRY_MILLIS; }

    /**
     * Whether a sinister node is in sight of the holder. Original ItemCompassStone: the node has to be within the range and in front of
     * the holder, whose field of view is two thirds of a radian.
     */
    public static boolean compassStoneVisible(double distance, double range, double dot) {
        if (distance <= 0.0D || distance > range) return false;
        return dot > Math.cos(COMPASS_STONE_FOV / 2.0D);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Eldritch Object (original ItemEldritchObject)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original ItemEldritchObject metadata: the eldritch eye. */
    public static final int ELDRITCH_EYE = 0;
    /** Original ItemEldritchObject metadata: the crimson rites. */
    public static final int ELDRITCH_CRIMSON_RITES = 1;
    /** Original ItemEldritchObject metadata: the runed tablet. */
    public static final int ELDRITCH_TABLET = 2;
    /** Original ItemEldritchObject metadata: the node stabilizer. */
    public static final int ELDRITCH_NODE_STABILIZER = 3;
    /** Original ItemEldritchObject metadata: the obelisk placer, a creative tool. */
    public static final int ELDRITCH_OBELISK_PLACER = 4;

    /** Original ItemEldritchObject.onItemUseFirst: the node stabilizer spends the item on a node. */
    public static final int NODE_STABILIZER_STUDY_RESEARCH = "PRIMNODE".hashCode();

    /**
     * The heights above the ground the obelisk of the eldritch occupies. Original ItemEldritchObject.onItemUseFirst: the six blocks
     * above the ground have to be free before the obelisk is put up.
     */
    public static final int ELDRITCH_PLACER_FREE_HEIGHT = 6;

    /**
     * The eldritch variant the obelisk placer puts at the given height above the ground. Original ItemEldritchObject.onItemUseFirst:
     * the altar at one, the obelisk at three and the middle of the obelisk at four, five, six and seven.
     */
    public static int eldritchPlacerVariant(int height) {
        if (height == 1) return EldritchBlockRules.ELDRITCH_ALTAR;
        if (height == 3) return EldritchBlockRules.ELDRITCH_OBELISK;
        return EldritchBlockRules.ELDRITCH_OBELISK_MIDDLE;
    }

    /**
     * The base vis a primal aspect of a node gets from the stabilizer. Original ItemEldritchObject.onItemUseFirst: the current value less
     * two plus a roll below nine with the research and below six without it.
     */
    public static int nodeStabilizerPrimalVis(int current, int roll, boolean researched) {
        return current - 2 + roll % (researched ? 9 : 6);
    }

    /**
     * The base vis a compound aspect of a node gets from the stabilizer. Original ItemEldritchObject.onItemUseFirst: a roll below four with
     * the research and below three without it, and only a roll above the current value counts.
     */
    public static int nodeStabilizerCompoundVis(int current, int roll, boolean researched) {
        int value = roll % (researched ? 4 : 3);
        return value <= 0 || value <= current ? current : value;
    }

    /** Whether the stabilizer adds a point of the primal aspect to the node's container (original ItemEldritchObject.onItemUseFirst). */
    public static boolean nodeStabilizerAddsVis(int current, int roll, boolean researched) {
        int value = roll % (researched ? 4 : 3);
        return value > 0 && value > current;
    }

    /** The modifier the node ends up with: fading turns pale, pale turns plain and a plain node may turn bright (original ItemEldritchObject). */
    public static String nodeStabilizerModifier(String current, boolean firstRoll, int thirdRoll) {
        if ("FADING".equals(current)) return firstRoll ? "PALE" : current;
        if ("PALE".equals(current)) return firstRoll ? null : current;
        if (current == null && thirdRoll == 0) return "BRIGHT";
        return current;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Alumentum dispenser (original BehaviorDispenseAlumetum)
    // ---------------------------------------------------------------------------------------------------------------

    /** Original BehaviorDispenseAlumetum: the projectile is thrown with this power. */
    public static final float ALUMENTUM_DISPENSE_POWER = 1.1F;
    /** Original BehaviorDispenseAlumetum: the projectile is thrown with this uncertainty. */
    public static final float ALUMENTUM_DISPENSE_UNCERTAINTY = 6.0F;
    /** Original BehaviorDispenseAlumetum: the projectile is lifted by this much. */
    public static final double ALUMENTUM_DISPENSE_LIFT = 0.1D;
    /** Original BehaviorDispenseAlumetum.func_82485_a: the sound event of the launch. */
    public static final int ALUMENTUM_DISPENSE_EVENT = 1009;
}
