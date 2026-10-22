package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.client.renderers.models.ModelGolem;
import thaumcraft.client.renderers.models.entities.ModelGolemAccessories;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.golems.GolemUpgrade;

/** Active original RenderGolemBase: seven materials and ordinary inventory cores.
 * All poses and cargo come from the rendered entity, never equipment or AI. */
public final class RenderGolemBase extends MobRenderer<EntityGolemBase, ModelGolem<EntityGolemBase>> {
    private static final ResourceLocation STRAW = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_straw.png");
    private static final ResourceLocation WOOD = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_wood.png");
    private static final ResourceLocation TALLOW = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_tallow.png");
    private static final ResourceLocation CLAY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_clay.png");
    private static final ResourceLocation FLESH = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_flesh.png");
    private static final ResourceLocation STONE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_stone.png");
    private static final ResourceLocation IRON = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_iron.png");
    private static final ResourceLocation THAUMIUM = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_thaumium.png");
    private static final ResourceLocation DAMAGE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_damage.png");
    private static final ResourceLocation DECORATION = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/golem_decoration.png");
    // Neutral feet end at pixel61; the frozen model owns its original0.4 scale.
    // This hook is after native Y-flip, before its -1.501 model translation.
    private static final float FOOT_OFFSET = 1.501F - 61.0F * .4F / 16.0F;

    public RenderGolemBase(EntityRendererProvider.Context context) {
        super(context, new ModelGolem<>(ModelGolem.createBodyLayer().bakeRoot()), .25F);
        addLayer(new CoreLayer(this, context.getItemRenderer()));
        addLayer(new UpgradesLayer(this, context.getItemRenderer()));
        addLayer(new AccessoriesLayer(this));
        addLayer(new FishingRodLayer(this, context.getItemRenderer()));
        addLayer(new WaterBucketLayer(this, context.getItemRenderer()));
        addLayer(new CarriedLayer(this, context.getItemRenderer()));
        addLayer(new DamageLayer(this));
    }

    private static void feedPose(ModelGolem<EntityGolemBase> model, EntityGolemBase entity, boolean healing) {
        // The frozen model's corrected scalar API supersedes its earlier draft.
        // Feed every value even when zero/false: the renderer reuses this model.
        model.setActive(entity.getGolemCore() != -1);
        model.setCarrying(entity.getGolemCore() == 5 || entity.getGolemCore() == 6 || !entity.getCarriedItem().isEmpty());
        model.setJarCarryScale(entity.getGolemCore() == 6 ? essentiaJarScale(entity) : 1);
        model.setBootupTimer(entity.getBootupTimer());
        model.setHealingTimer(healing ? entity.getHealingTimer() : 0);
        model.setCombatTimer(entity.getActionTimer());
        model.setArmTimers(entity.getLeftArmTimer(), entity.getRightArmTimer());
        model.setInactive(entity.isFettered()); // Standing on an active Golem Fetter; the open-menu pause has no getter in this slice.
    }

    @Override public void render(EntityGolemBase entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        feedPose(model, entity, true);
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override public ResourceLocation getTextureLocation(EntityGolemBase entity) {
        return switch (entity.getGolemMaterial()) {
            case 1 -> WOOD;
            case 2 -> TALLOW;
            case 3 -> CLAY;
            case 4 -> FLESH;
            case 5 -> STONE;
            case 6 -> IRON;
            case 7 -> THAUMIUM;
            default -> STRAW;
        };
    }

    @Override protected void scale(EntityGolemBase entity, PoseStack pose, float partialTick) {
        super.scale(entity, pose, partialTick);
        pose.translate(0, FOOT_OFFSET, 0);
    }

    @Override protected void setupRotations(EntityGolemBase entity, PoseStack pose, float age, float yaw, float partialTick, float scale) {
        super.setupRotations(entity, pose, age, yaw, partialTick, scale);
        if (entity.walkAnimation.speed() >= .01F) {
            // Native position(partial) is exactly position-speed*(1-partial).
            float phase = entity.walkAnimation.position(partialTick) + 6.0F;
            float sway = (Math.abs(phase % 13.0F - 6.5F) - 3.25F) / 3.25F;
            pose.mulPose(Axis.ZP.rotationDegrees(6.5F * sway));
        }
    }

    private static final class CoreLayer extends RenderLayer<EntityGolemBase, ModelGolem<EntityGolemBase>> {
        private final ItemRenderer items;
        private CoreLayer(RenderGolemBase renderer, ItemRenderer items) { super(renderer); this.items = items; }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityGolemBase entity,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            if (entity.isInvisible()) return;
            var coreItem = ThaumcraftGolems.coreForType(entity.getGolemCore());
            if (coreItem == null) return;
            // A visual representation of the fitted core, not carried stock.
            ItemStack core = new ItemStack(coreItem);
            pose.pushPose();
            try {
                pose.mulPose(Axis.XP.rotationDegrees(180));
                pose.translate(.0875, -.96, .15);
                pose.scale(.175F, .175F, .175F);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                // Old extruded sprite spans [0,1] XY and [-.2,0] Z. Native
                // generated items are centered, with a 1/16-thick sprite.
                pose.translate(.5, .5, -.1);
                pose.scale(1, 1, 3.2F);
                renderItem(items, core, entity, pose, buffers, light);
            } finally { pose.popPose(); }
        }
    }

    /**
     * Original RenderGolemBase:116-141 — the installed upgrades are shown as small flat icons in a
     * row on the golem's back. The original draws one icon per upgrade SLOT: a free slot shows the
     * blank "golem_upgrade_empty" icon (oracle ItemGolemUpgrade:45-57 — the icon of a negative
     * damage value is the empty one), an installed slot the icon of its upgrade.
     */
    private static final class UpgradesLayer extends RenderLayer<EntityGolemBase, ModelGolem<EntityGolemBase>> {
        private static final ResourceLocation EMPTY_ICON =
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/items/golem_upgrade_empty.png");
        private final ItemRenderer items;

        private UpgradesLayer(RenderGolemBase renderer, ItemRenderer items) { super(renderer); this.items = items; }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityGolemBase entity,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            if (entity.isInvisible()) return;
            int slots = entity.getUpgradeSlotCount();
            if (slots <= 0) return;
            float shift = 0.08F;
            for (int a = 0; a < slots; a++) {
                GolemUpgrade upgrade = GolemUpgrade.byId(entity.getUpgrade(a));
                if (upgrade == null) { emptyIcon(pose, buffers, light, shift, slots, a); continue; }
                Item item = ThaumcraftGolems.upgradeItem(upgrade);
                if (item == null) continue;
                pose.pushPose();
                try {
                    pose.mulPose(Axis.XP.rotationDegrees(180));
                    pose.translate(-0.05F - shift * (slots - 1) / 2F + shift * a, -1.106F, 0.099F);
                    pose.scale(0.1F, 0.1F, 0.1F);
                    // The original draws a flat quad; the native item sprite is centred and 1/16 thick.
                    pose.translate(.5F, .5F, 0F);
                    pose.scale(1F, 1F, .001F);
                    renderItem(items, new ItemStack(item), entity, pose, buffers, light);
                } finally { pose.popPose(); }
            }
        }

        /**
         * The original's blank icon of a free upgrade slot: the same flat quad, the same place and
         * the same scale as an installed upgrade, with the texture of the empty upgrade.
         */
        private static void emptyIcon(PoseStack pose, MultiBufferSource buffers, int light, float shift, int slots, int slot) {
            pose.pushPose();
            try {
                pose.mulPose(Axis.XP.rotationDegrees(180));
                pose.translate(-0.05F - shift * (slots - 1) / 2F + shift * slot, -1.106F, 0.099F);
                pose.scale(0.1F, 0.1F, 0.1F);
                VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(EMPTY_ICON));
                // Original RenderGolemBase:133-139 — the quad in the XY plane, the icon upright.
                quad(consumer, pose, light, 0, 0, 1);
                quad(consumer, pose, light, 1, 0, 1);
                quad(consumer, pose, light, 1, 1, 0);
                quad(consumer, pose, light, 0, 1, 0);
            } finally { pose.popPose(); }
        }

        private static void quad(VertexConsumer consumer, PoseStack pose, int light, float x, float y, float v) {
            consumer.addVertex(pose.last(), x, y, 0).setColor(255, 255, 255, 255).setUv(x, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose.last(), 0, 0, 1);
        }
    }

    /**
     * Original RenderGolemBase:145-149 — the worn accessories are a second pass over the golem
     * model, drawn with the decoration texture whenever the golem wears something or is advanced.
     * The bow tie and the head jar of the original are blended, every other piece is cut out.
     */
    private static final class AccessoriesLayer extends RenderLayer<EntityGolemBase, ModelGolem<EntityGolemBase>> {
        private final ModelGolemAccessories accessories;

        private AccessoriesLayer(RenderGolemBase renderer) {
            super(renderer);
            accessories = new ModelGolemAccessories(ModelGolemAccessories.createBodyLayer(30F).bakeRoot(), 30F);
        }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityGolemBase entity,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            if (entity.isInvisible()) return;
            String decoration = entity.getDecoration();
            if ((decoration == null || decoration.isEmpty()) && !entity.isAdvanced()) return;
            accessories.prepareMobModel(entity, walkPhase, walkSpeed, partialTick);
            accessories.setupAnim(entity, walkPhase, walkSpeed, age, headYaw, headPitch);
            int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0);
            accessories.setBlendPass(false);
            accessories.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(DECORATION)), light, overlay, 0xFFFFFFFF);
            accessories.setBlendPass(true);
            accessories.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(DECORATION)), light, overlay, 0xFFFFFFFF);
        }
    }

    /** Original G:170–191: core11 always displays a rod, independently of cargo
     * or a live cast. This stack never enters equipment, saves, drops or stock. */
    private static final class FishingRodLayer extends RenderLayer<EntityGolemBase, ModelGolem<EntityGolemBase>> {
        private final ItemRenderer items;
        private final ItemStack rod = new ItemStack(Items.FISHING_ROD);
        private FishingRodLayer(RenderGolemBase renderer, ItemRenderer items) { super(renderer); this.items = items; }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityGolemBase entity,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            if (entity.isInvisible() || entity.getGolemCore() != 11) return;
            pose.pushPose();
            try {
                pose.mulPose(Axis.XP.rotationDegrees(5.0F + 90.0F * getParentModel().golemRightArm.xRot / Mth.PI));
                pose.translate(-.26875, 1.6, -.53);
                pose.mulPose(Axis.YP.rotationDegrees(-90));
                pose.mulPose(Axis.ZP.rotationDegrees(-30));
                pose.scale(.66F, -.66F, .66F);
                // Native generated sprite is centered, thickness1/16. Match
                // the old [0,1] XY / [-1/16,0] Z extruded-icon placement.
                pose.translate(.5, .5, -.03125);
                // NONE: no second handheld transform. The native cast predicate
                // is player-only; this actual golem uses the original base icon.
                renderItem(items, rod, entity, pose, buffers, light);
            } finally { pose.popPose(); }
        }
    }

    /** Native display-only bucket adaptation; real fluid is a separate mB field,
     * never an equipped/returned bucket or a second transferable stack. The bucket
     * is the one of the fluid of the cargo, exactly like the original's
     * {@code new ItemStack(Item.getItemById(fluidID), 1, amount)}
     * (oracle {@code EntityGolemBase:877-883}). */
    private static final class WaterBucketLayer extends RenderLayer<EntityGolemBase, ModelGolem<EntityGolemBase>> {
        private final ItemRenderer items;
        private final ItemStack empty = new ItemStack(Items.BUCKET);
        private WaterBucketLayer(RenderGolemBase renderer, ItemRenderer items) { super(renderer); this.items = items; }
        private static ItemStack cargoBucket(EntityGolemBase entity) {
            var id = entity.getFluidId();
            if (id == null) return new ItemStack(Items.BUCKET);
            var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id);
            if (fluid == null) return new ItemStack(Items.BUCKET);
            return new ItemStack(fluid.getBucket());
        }
        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityGolemBase entity,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            if (entity.isInvisible() || entity.getGolemCore() != 5) return;
            pose.pushPose();
            try {
                pose.scale(.4F, .4F, .4F);
                pose.translate(0, 2.5, -1.1);
                pose.mulPose(Axis.XP.rotationDegrees(180));
                renderItem(items, entity.getFluidAmountMb() > 0 ? cargoBucket(entity) : empty, entity, pose, buffers, light);
            } finally { pose.popPose(); }
        }
    }

    private static final class CarriedLayer extends RenderLayer<EntityGolemBase, ModelGolem<EntityGolemBase>> {
        private final ItemRenderer items;
        private CarriedLayer(RenderGolemBase renderer, ItemRenderer items) { super(renderer); this.items = items; }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityGolemBase entity,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            ItemStack carried = carriedDisplay(entity);
            if (entity.isInvisible() || entity.getGolemCore() == 5 || carried.isEmpty() || entity.hurtTime != 0) return;
            boolean jar = ItemJar.isJar(carried);
            boolean block = !jar && carried.getItem() instanceof BlockItem
                    && items.getModel(carried, entity.level(), entity, entity.getId()).isGui3d();
            pose.pushPose();
            try {
                // Separate from the mesh's internal scale, as in original195.
                pose.scale(.4F, .4F, .4F);
                if (block) {
                    pose.translate(0, 2.5, -1.25);
                    pose.mulPose(Axis.XP.rotationDegrees(180));
                } else if (jar) {
                    pose.translate(0, 2.5, -1);
                    if (entity.getGolemCore() == 6) {
                        float scale = essentiaJarScale(entity);
                        pose.scale(scale, scale, scale);
                    }
                    pose.translate(-.5, 0, -.8);
                    turnFlatItem(pose);
                    // Original non-player jar hook draws its block at [0,1].
                    // Cancel native ItemRenderer's centering for that shell.
                    pose.translate(.5, .5, .5);
                } else {
                    pose.translate(-.5, 2.5, -1.25);
                    turnFlatItem(pose);
                    pose.translate(.5, .5, -.03125);
                }
                // NONE avoids applying inventory/hand JSON transforms twice.
                // Native rendering retains component overrides, tints, foil,
                // multiple baked layers and builtin block-entity item routes.
                renderItem(items, carried, entity, pose, buffers, light);
            } finally { pose.popPose(); }
        }

        private static void turnFlatItem(PoseStack pose) {
            pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.mulPose(Axis.ZP.rotationDegrees(-335));
            pose.mulPose(Axis.YP.rotationDegrees(-50));
        }
    }

    /** Original RenderGolemBase:204 / E:886 read getCarryLimit(): material plus Earth upgrades. */
    private static int essentiaCapacity(EntityGolemBase entity) {
        return Math.max(1, entity.getCarryLimit());
    }
    private static float essentiaJarScale(EntityGolemBase entity) {
        return .5F + Math.min(64, essentiaCapacity(entity)) / 128F;
    }
    /** Original core6 display is a scaled fraction, NOT paid jar contents.
     * This local preview never enters actual cargo, equipment or an output queue. */
    private static ItemStack carriedDisplay(EntityGolemBase entity) {
        if (entity.getGolemCore() != 6) return entity.getCarriedItem();
        ItemStack preview = new ItemStack(ThaumcraftContent.JAR.get());
        var aspect = entity.getEssentiaAspect();
        if (aspect == null || entity.getEssentiaAmount() <= 0) return preview;
        int displayAmount = Math.min(64, (int) (64L * entity.getEssentiaAmount() / essentiaCapacity(entity)));
        return ItemJar.withContents(preview, aspect, displayAmount);
    }

    private static void renderItem(ItemRenderer items, ItemStack stack, EntityGolemBase entity, PoseStack pose, MultiBufferSource buffers, int light) {
        items.renderStatic(entity, stack, ItemDisplayContext.NONE, false, pose, buffers, entity.level(), light, OverlayTexture.NO_OVERLAY, entity.getId());
    }

    private static final class DamageLayer extends RenderLayer<EntityGolemBase, ModelGolem<EntityGolemBase>> {
        private final ModelGolem<EntityGolemBase> damage = new ModelGolem<>(ModelGolem.createBodyLayer().bakeRoot());
        private DamageLayer(RenderGolemBase renderer) { super(renderer); }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityGolemBase entity,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            if (entity.isInvisible()) return;
            float alpha = Mth.clamp(1.0F - entity.getHealth() / entity.getMaxHealth(), 0, 1);
            if (alpha <= 0) return;
            getParentModel().copyPropertiesTo(damage);
            feedPose(damage, entity, false); // Original pass2 never applies healing RGB.
            damage.prepareMobModel(entity, walkPhase, walkSpeed, partialTick);
            damage.setupAnim(entity, walkPhase, walkSpeed, age, headYaw, headPitch);
            int color = (int) (alpha * 255.0F) << 24 | 0x00ffffff;
            damage.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(DAMAGE)), light,
                    LivingEntityRenderer.getOverlayCoords(entity, 0), color);
        }
    }
}
