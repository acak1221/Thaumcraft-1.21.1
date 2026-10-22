package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.renderers.models.gear.HarnessArmorRenderer;
import thaumcraft.client.renderers.models.gear.ModelFortressArmor;
import thaumcraft.client.renderers.models.gear.ModelGearArmor;
import thaumcraft.client.renderers.models.gear.ModelHoverHarness;
import thaumcraft.client.renderers.models.gear.ModelKnightArmor;
import thaumcraft.client.renderers.models.gear.ModelLeaderArmor;
import thaumcraft.client.renderers.models.gear.ModelRobe;
import thaumcraft.client.renderers.models.gear.ModelWand;
import thaumcraft.client.renderers.item.WandUsePose;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.common.items.armor.HarnessData;
import thaumcraft.common.items.armor.ItemFortressArmor;
import thaumcraft.common.items.armor.ItemRobeArmor;
import thaumcraft.common.items.armor.ItemVoidRobeArmor;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandMaterials;
import thaumcraft.common.items.wands.foci.FocusData;

/**
 * Client-only bindings of the ported 1.7.10 gear models: the taumaturge robe
 * ({@code ModelRobe}), the Thaumium Fortress armour ({@code ModelFortressArmor}),
 * the cultist plate and leader armour ({@code ModelKnightArmor} and
 * {@code ModelLeaderArmor}), the hover harness ({@code ModelHoverHarness}) and
 * the wand ({@code ModelWand}).
 *
 * <p>The model layers are registered here so both loaders can bake them; the
 * actual drawing is loader specific, so the loader's own bootstrap calls the
 * {@code render*} entry points below.
 */
public final class ThaumcraftGearModelsClient {
    /** The original robe sheet of the cultists. */
    public static final ResourceLocation CULTIST_ROBE_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/cultist_robe_armor.png");
    /** The original robe sheet of the void thaumaturge: the metal trim, drawn untinted. */
    public static final ResourceLocation VOID_ROBE_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/void_robe_armor.png");
    /**
     * The original robe sheet of the void thaumaturge: the dyed cloth.
     *
     * <p>Oracle {@code thaumcraft/common/items/armor/ItemVoidRobeArmor.java:98-100}: the worn look of
     * the void thaumaturge is <b>two</b> sheets. The layer that carries the dye is the
     * {@code _overlay} sheet (the cloth), the layer that carries the trim is the plain sheet, and
     * the plain sheet is drawn <em>after</em> the dyed one, so the trim stays untinted.</p>
     */
    public static final ResourceLocation VOID_ROBE_OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/void_robe_armor_overlay.png");
    /** The original fortress armour sheet. */
    public static final ResourceLocation FORTRESS_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/fortress_armor.png");
    /** The original cultist plate armour sheet. */
    public static final ResourceLocation KNIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/cultist_plate_armor.png");
    /** The original cultist leader armour sheet. */
    public static final ResourceLocation LEADER_TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/cultist_leader_armor.png");

    public static final ModelLayerLocation ROBE_ARMOURED = layer("gear/robe", "armoured");
    public static final ModelLayerLocation ROBE_CLOTH = layer("gear/robe", "cloth");
    public static final ModelLayerLocation FORTRESS_ARMOURED = layer("gear/fortress", "armoured");
    public static final ModelLayerLocation FORTRESS_LIGHT = layer("gear/fortress", "light");
    public static final ModelLayerLocation KNIGHT_ARMOURED = layer("gear/knight", "armoured");
    public static final ModelLayerLocation KNIGHT_LIGHT = layer("gear/knight", "light");
    public static final ModelLayerLocation LEADER_ARMOURED = layer("gear/leader", "armoured");
    public static final ModelLayerLocation LEADER_LIGHT = layer("gear/leader", "light");

    private static ModelRobe robeArmoured, robeCloth;
    private static ModelFortressArmor fortressArmoured, fortressLight;
    private static ModelKnightArmor knightArmoured, knightLight;
    private static ModelLeaderArmor leaderArmoured, leaderLight;
    private static ModelWand wand;
    private static ModelHoverHarness hoverHarness;
    private static boolean initialized;

    private ThaumcraftGearModelsClient() {}

    private static ModelLayerLocation layer(String path, String variant) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("thaumcraft", path), variant);
    }

    /** Registers the gear model layers for both loaders. */
    public static void init() {
        if (initialized) return;
        initialized = true;
        EntityModelLayerRegistry.register(ROBE_ARMOURED, () -> ModelRobe.createBodyLayer(true));
        EntityModelLayerRegistry.register(ROBE_CLOTH, () -> ModelRobe.createBodyLayer(false));
        EntityModelLayerRegistry.register(FORTRESS_ARMOURED, () -> ModelFortressArmor.createBodyLayer(true));
        EntityModelLayerRegistry.register(FORTRESS_LIGHT, () -> ModelFortressArmor.createBodyLayer(false));
        EntityModelLayerRegistry.register(KNIGHT_ARMOURED, () -> ModelKnightArmor.createBodyLayer(true));
        EntityModelLayerRegistry.register(KNIGHT_LIGHT, () -> ModelKnightArmor.createBodyLayer(false));
        EntityModelLayerRegistry.register(LEADER_ARMOURED, () -> ModelLeaderArmor.createBodyLayer(true));
        EntityModelLayerRegistry.register(LEADER_LIGHT, () -> ModelLeaderArmor.createBodyLayer(false));
    }

    /** The original ModelWand, baked once. */
    public static ModelWand wandModel() {
        if (wand == null) wand = new ModelWand();
        return wand;
    }

    /**
     * The original ModelHoverHarness. The retained HarnessArmorRenderer already
     * draws this geometry with the original OBJ pack; this accessor exposes the
     * canonical port of the original model.
     */
    public static ModelHoverHarness hoverHarnessModel() {
        if (hoverHarness == null) hoverHarness = new ModelHoverHarness();
        return hoverHarness;
    }

    /**
     * The original ModelWand83-243 for the loader's builtin item hook: the rod
     * in its own material sheet, the cap in its own sheet and the focus cube
     * with the focus colour. The sceptre, the ornament, the focus depth layer
     * and the runes of the original need items this port does not have.
     *
     * <p>Original {@code thaumcraft/client/renderers/item/ItemWandRenderer.java:87} applies the
     * model basis correction of a hundred and eighty degrees about X before the model is drawn.
     * The model of the original is authored with the rod above the origin and the focus cube
     * below it, so the correction is what makes the focus of the wand point up, exactly like the
     * original. The use pose of the original follows the correction (oracle
     * {@code ItemWandRenderer.java:88-113}) and is therefore expressed in the corrected
     * basis.</p>
     */
    public static void renderWand(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof ItemWandCasting)) return;
        var data = ItemWandCasting.data(stack);
        var rod = WandMaterials.rod(data.rod());
        var cap = WandMaterials.cap(data.cap());
        boolean staff = rod.staff();
        var model = wandModel();
        pose.pushPose();
        try {
            // Original ItemWandRenderer:87 - the model basis correction.
            pose.mulPose(Axis.XP.rotationDegrees(180.0F));
            // Original ItemWandRenderer:88-113 - the use pose of the wand.
            applyWandUsePose(pose, stack, context);
            // Original ModelWand:95-98 - the staff shift of the model itself.
            if (staff) pose.translate(0, .2, 0);
            pose.pushPose();
            try {
                if (staff) {
                    pose.translate(0, ModelWand.Geometry.STAFF_ROD_SHIFT, 0);
                    pose.scale(ModelWand.Geometry.STAFF_ROD_XZ, ModelWand.Geometry.STAFF_ROD_Y, ModelWand.Geometry.STAFF_ROD_XZ);
                }
                model.renderRod(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(ModelWand.rodTexture(rod.baseTag()))), light, overlay);
            } finally { pose.popPose(); }
            pose.pushPose();
            try {
                if (staff) pose.scale(ModelWand.Geometry.STAFF_CAP_XZ, ModelWand.Geometry.STAFF_CAP_Y, ModelWand.Geometry.STAFF_CAP_XZ);
                else pose.scale(ModelWand.Geometry.WAND_CAP_XZ, ModelWand.Geometry.WAND_CAP_Y, ModelWand.Geometry.WAND_CAP_XZ);
                var capBuffer = buffers.getBuffer(RenderType.entityCutoutNoCull(ModelWand.capTexture(cap.tag())));
                model.renderCap(pose, capBuffer, light, overlay);
                if (staff) {
                    pose.translate(0, ModelWand.Geometry.STAFF_RING_SHIFT, 0);
                    pose.pushPose();
                    try {
                        pose.scale(1, ModelWand.Geometry.STAFF_RING_Y, 1);
                        model.renderCap(pose, capBuffer, light, overlay);
                    } finally { pose.popPose(); }
                    pose.translate(0, ModelWand.Geometry.STAFF_BOTTOM_SHIFT, 0);
                }
                model.renderCapBottom(pose, capBuffer, light, overlay);
            } finally { pose.popPose(); }
            var focusStack = FocusData.get(stack);
            if (!focusStack.isEmpty() && focusStack.getItem() instanceof ItemFocusBasic basic) {
                int rgb = basic.getFocusColor(focusStack);
                int tint = (int) (0.95F * 255) << 24 | (rgb & 0xFFFFFF);
                pose.pushPose();
                try {
                    if (staff) {
                        pose.translate(0, ModelWand.Geometry.STAFF_FOCUS_SHIFT, 0);
                        pose.scale(ModelWand.Geometry.STAFF_FOCUS_XZ, ModelWand.Geometry.STAFF_FOCUS_Y, ModelWand.Geometry.STAFF_FOCUS_XZ);
                    } else pose.scale(ModelWand.Geometry.WAND_FOCUS, ModelWand.Geometry.WAND_FOCUS, ModelWand.Geometry.WAND_FOCUS);
                    model.renderFocus(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(ModelWand.FOCUS_TEXTURE)), LightTexture.FULL_BRIGHT, overlay, tint);
                } finally { pose.popPose(); }
            }
        } finally { pose.popPose(); }
    }

    /**
     * The original ItemWandRenderer:88-113 - the use pose of the wand.
     *
     * <p>The original raises the wand while the player uses it: the pose turns around a pivot one
     * block above the model, the third person view rolls the wand by thirty three degrees around
     * Z, the first person view turns it by ten degrees around X and ten degrees around Z, and
     * the wand itself is raised by up to sixty degrees around -X over the first three ticks of the
     * use. A focus with the WAVE animation adds a wave of ten degrees, a focus with the CHARGE
     * animation adds a wave of one degree.</p>
     *
     * <p>The original only poses the wand in the hand: its wielder is only known for the
     * {@code EQUIPPED} and the {@code EQUIPPED_FIRST_PERSON} context, so the inventory and the
     * dropped item are never posed.</p>
     */
    private static void applyWandUsePose(PoseStack pose, ItemStack stack, ItemDisplayContext context) {
        if (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) return;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        // Original ItemWandRenderer:88 - the pose needs a player that uses an item.
        if (player == null || player.getUseItem().isEmpty()) return;
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        // The 1.7.10 EntityPlayer.getItemInUseDuration() (MCP stable 12-1.7.10: field_71057_bx)
        // counts the elapsed use up, and so does the 1.21.1 LivingEntity.getTicksUsingItem().
        int ticks = player.getTicksUsingItem();
        // Original ItemWandRenderer:93 - the pivot of the pose.
        pose.translate(0, WandUsePose.PIVOT_Y, 0);
        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            // Original ItemWandRenderer:97-98.
            pose.mulPose(Axis.XP.rotationDegrees(WandUsePose.FIRST_PERSON_PITCH));
            pose.mulPose(Axis.ZP.rotationDegrees(WandUsePose.FIRST_PERSON_ROLL));
        } else {
            // Original ItemWandRenderer:95.
            pose.mulPose(Axis.ZP.rotationDegrees(WandUsePose.THIRD_PERSON_ROLL));
        }
        // Original ItemWandRenderer:100 - the raise of the wand around -X.
        pose.mulPose(Axis.XN.rotationDegrees(WandUsePose.raiseDegrees(WandUsePose.useTicks(ticks, partial))));
        applyWandFocusWave(pose, stack, player, ticks, partial);
        // Original ItemWandRenderer:112 - the pivot is released again.
        pose.translate(0, -WandUsePose.PIVOT_Y, 0);
    }

    /**
     * Original ItemWandRenderer:101-111 - the wave of the focus.
     *
     * <p>The original carries the WAVE animation on the wand itself while the wand is used on a
     * node (oracle {@code ItemWandCasting.java:550}), and a focus with the WAVE animation waves
     * as well. The native keeps the node use of the wand in
     * {@link ItemWandCasting#isUsingNode(net.minecraft.world.entity.LivingEntity)}.</p>
     */
    private static void applyWandFocusWave(PoseStack pose, ItemStack stack, Player player, int ticks, float partial) {
        var focusStack = FocusData.get(stack);
        var focus = focusStack.getItem() instanceof ItemFocusBasic basic ? basic : null;
        boolean wave = ItemWandCasting.isUsingNode(player)
                || focus != null && focus.getAnimation(focusStack) == ItemFocusBasic.WandFocusAnimation.WAVE;
        if (wave) {
            pose.mulPose(Axis.ZP.rotationDegrees(WandUsePose.waveRoll(ticks, partial)));
            pose.mulPose(Axis.XP.rotationDegrees(WandUsePose.wavePitch(ticks, partial)));
        } else if (focus != null && focus.getAnimation(focusStack) == ItemFocusBasic.WandFocusAnimation.CHARGE) {
            pose.mulPose(Axis.ZP.rotationDegrees(WandUsePose.chargeRoll(ticks, partial)));
            pose.mulPose(Axis.XP.rotationDegrees(WandUsePose.chargePitch(ticks, partial)));
        }
    }

    /**
     * The loader independent dispatch of the original TC4 gear models.
     *
     * <p>The Fabric loader registers the three renderers of the Fabric
     * {@code ArmorRenderer} API and the NeoForge loader draws the very same pieces from its own
     * armour layer. Both call the renderers of this class, so the two loaders cannot draw the
     * gear differently: the void robe, the Thaumium Fortress armour and the hover harness with
     * the chest cube and the retained OBJ pack.</p>
     *
     * <p>The cultist plate and the cultist leader armour are NOT drawn here: the Fabric
     * loader never registered them, so they keep the native armour layer on both loaders.</p>
     *
     * @return true when the original TC4 model drew the piece
     */
    public static boolean renderGearItem(PoseStack pose, MultiBufferSource buffers, ItemStack armor, LivingEntity entity,
                                          EquipmentSlot slot, int light, HumanoidModel<LivingEntity> context) {
        if (armor.getItem() instanceof ItemVoidRobeArmor) {
            renderRobe(pose, buffers, armor, entity, slot, light, context);
            return true;
        }
        if (armor.getItem() instanceof ItemFortressArmor) {
            renderFortressArmor(pose, buffers, armor, entity, slot, light, context);
            return true;
        }
        if (HarnessData.isHarness(armor)) {
            HarnessArmorRenderer.render(pose, buffers, armor, entity, slot, light, context);
            return true;
        }
        return false;
    }

    /**
     * The original ModelRobe for a worn robe. The helm and the greaves use the
     * armoured model and the chest and the boots the cloth model, exactly as
     * the original ItemCultistRobeArmor:112-115 does.
     *
     * <p>Oracle {@code thaumcraft/common/items/armor/ItemVoidRobeArmor.java:98-100,187-189}: the
     * void thaumaturge is drawn in <b>two</b> layers, the dyed cloth sheet
     * ({@code void_robe_armor_overlay.png}) first and the untinted trim sheet
     * ({@code void_robe_armor.png}) after it. The cultist robe has no colour and therefore no
     * second layer (the original's {@code hasOverlay} stays false there), so it is drawn once with
     * its own sheet.</p>
     */
    public static void renderRobe(PoseStack pose, MultiBufferSource buffers, ItemStack armor, Entity entity, EquipmentSlot slot, int light, HumanoidModel<?> context) {
        boolean armoured = slot == EquipmentSlot.HEAD || slot == EquipmentSlot.LEGS;
        var model = armoured ? robeArmoured() : robeCloth();
        // Original ItemVoidRobeArmor:205-226 carries the dye colour, the cultist
        // robe has none; its sheet is already the original colour.
        boolean voidRobe = armor.getItem() instanceof thaumcraft.common.items.armor.ItemVoidRobeArmor;
        if (!voidRobe) {
            renderGear(pose, buffers, model, CULTIST_ROBE_TEXTURE, entity, slot, light, context, -1,
                    RenderType.entityCutoutNoCull(CULTIST_ROBE_TEXTURE));
            return;
        }
        int tint = 0xff000000 | ItemRobeArmor.color(armor);
        // The original's two worn layers, the dyed cloth first and the untinted trim after it.
        for (var layer : thaumcraft.common.items.armor.VoidRobeLayers.WORN_LAYERS) {
            ResourceLocation sheet = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/" + layer.sheet());
            renderGear(pose, buffers, model, sheet, entity, slot, light, context,
                    layer.dyed() ? tint : -1,
                    layer.dyed() ? RenderType.entityTranslucent(sheet) : RenderType.entityCutoutNoCull(sheet));
        }
    }

    /**
     * The original ModelFortressArmor for a worn fortress piece. The original
     * ornaments and the extra shoulder plates need the number of worn pieces
     * and the infused mask and goggles of the helm.
     */
    public static void renderFortressArmor(PoseStack pose, MultiBufferSource buffers, ItemStack armor, Entity entity, EquipmentSlot slot, int light, HumanoidModel<?> context) {
        var model = fortressArmoured();
        int set = fortressSet(entity);
        int mask = ItemFortressArmor.mask(armor);
        boolean goggles = armor.getItem() instanceof ItemFortressArmor && armor.has(thaumcraft.common.config.ThaumcraftEquipment.FORTRESS_GOGGLES.get());
        model.setPieceVisibility(set, mask, goggles);
        renderGear(pose, buffers, model, FORTRESS_TEXTURE, entity, slot, light, context, -1);
    }

    /** The original ModelKnightArmor for a worn cultist plate piece. */
    public static void renderKnightArmor(PoseStack pose, MultiBufferSource buffers, ItemStack armor, Entity entity, EquipmentSlot slot, int light, HumanoidModel<?> context) {
        boolean armoured = slot == EquipmentSlot.HEAD || slot == EquipmentSlot.LEGS;
        renderGear(pose, buffers, armoured ? knightArmoured() : knightLight(), KNIGHT_TEXTURE, entity, slot, light, context, -1);
    }

    /** The original ModelLeaderArmor for a worn cultist leader piece. */
    public static void renderLeaderArmor(PoseStack pose, MultiBufferSource buffers, ItemStack armor, Entity entity, EquipmentSlot slot, int light, HumanoidModel<?> context) {
        boolean armoured = slot == EquipmentSlot.HEAD || slot == EquipmentSlot.LEGS;
        renderGear(pose, buffers, armoured ? leaderArmoured() : leaderLight(), LEADER_TEXTURE, entity, slot, light, context, -1);
    }

    /** The number of worn fortress pieces, -1 when the wearer has none. */
    private static int fortressSet(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return -1;
        int set = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            if (living.getItemBySlot(slot).getItem() instanceof ItemFortressArmor) set++;
        }
        return set == 0 ? -1 : set;
    }

    /** The shared drawing path: the wearer's pose, the original slot visibility and the sheet. */
    private static void renderGear(PoseStack pose, MultiBufferSource buffers, ModelGearArmor model,
                                   ResourceLocation texture, Entity entity, EquipmentSlot slot, int light, HumanoidModel<?> context, int tint) {
        renderGear(pose, buffers, model, texture, entity, slot, light, context, tint,
                RenderType.entityCutoutNoCull(texture));
    }

    /** The shared drawing path with an explicit sheet type, for the two layers of the void robe. */
    private static void renderGear(PoseStack pose, MultiBufferSource buffers, ModelGearArmor model,
                                   ResourceLocation texture, Entity entity, EquipmentSlot slot, int light, HumanoidModel<?> context,
                                   int tint, RenderType type) {
        if (entity instanceof LivingEntity living) {
            // The original cloth swing uses the same limb swing and limb swing
            // amount as the worn model; the wearer's own animation supplies them.
            float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            model.riding = living.isPassenger();
            model.young = living.isBaby();
            model.swimAmount = living.getSwimAmount(partial);
            model.attackTime = living.getAttackAnim(partial);
            model.setupAnim(living, living.walkAnimation.position(partial), living.walkAnimation.speed(partial),
                    living.tickCount + partial, 0, 0);
        }
        // The wearer's own humanoid pose wins over the model's own animation,
        // exactly as the original copies the biped rotations of the player model.
        model.copyPose(context);
        model.showSlot(slot);
        var buffer = buffers.getBuffer(type);
        model.renderToBuffer(pose, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, tint);
    }

    private static ModelRobe robeArmoured() {
        if (robeArmoured == null) robeArmoured = new ModelRobe(bake(ROBE_ARMOURED), true);
        return robeArmoured;
    }

    private static ModelRobe robeCloth() {
        if (robeCloth == null) robeCloth = new ModelRobe(bake(ROBE_CLOTH), false);
        return robeCloth;
    }

    private static ModelFortressArmor fortressArmoured() {
        if (fortressArmoured == null) fortressArmoured = new ModelFortressArmor(bake(FORTRESS_ARMOURED), true);
        return fortressArmoured;
    }

    private static ModelFortressArmor fortressLight() {
        if (fortressLight == null) fortressLight = new ModelFortressArmor(bake(FORTRESS_LIGHT), false);
        return fortressLight;
    }

    private static ModelKnightArmor knightArmoured() {
        if (knightArmoured == null) knightArmoured = new ModelKnightArmor(bake(KNIGHT_ARMOURED), true);
        return knightArmoured;
    }

    private static ModelKnightArmor knightLight() {
        if (knightLight == null) knightLight = new ModelKnightArmor(bake(KNIGHT_LIGHT), false);
        return knightLight;
    }

    private static ModelLeaderArmor leaderArmoured() {
        if (leaderArmoured == null) leaderArmoured = new ModelLeaderArmor(bake(LEADER_ARMOURED), true);
        return leaderArmoured;
    }

    private static ModelLeaderArmor leaderLight() {
        if (leaderLight == null) leaderLight = new ModelLeaderArmor(bake(LEADER_LIGHT), false);
        return leaderLight;
    }

    private static net.minecraft.client.model.geom.ModelPart bake(ModelLayerLocation layer) {
        return Minecraft.getInstance().getEntityModels().bakeLayer(layer);
    }
}
