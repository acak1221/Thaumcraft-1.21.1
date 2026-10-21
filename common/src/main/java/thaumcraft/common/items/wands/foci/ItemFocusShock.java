package thaumcraft.common.items.wands.foci;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.ItemFocusBasic;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.entities.projectile.EntityShockOrb;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.lib.network.ShockFxNetworking;

/** TC4 Shock with chain lightning and the earthshock orb. */
public final class ItemFocusShock extends ItemFocusBasic {
    public static final int BASE_COST_CENTIVIS = 25;
    public static final int COOLDOWN_MILLIS = 250;
    public static final double ENTITY_RANGE = 20;
    private static final SoundEvent SHOCK = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "shock"));
    private static final SoundEvent ZAP = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "zap"));

    public ItemFocusShock(Properties properties) { super(properties); }

    @Override public AspectList getVisCost(ItemStack focus) {
        if (focus.getItem() instanceof ItemFocusShock) {
            if (isUpgradedWith(focus, FocusUpgradeType.chainlightning)) return new AspectList().add(Aspect.AIR, 40).add(Aspect.WATER, 10);
            if (isUpgradedWith(focus, FocusUpgradeType.earthshock)) return new AspectList().add(Aspect.AIR, 75).add(Aspect.EARTH, 25);
        }
        return new AspectList().add(Aspect.AIR, BASE_COST_CENTIVIS);
    }
    @Override public int getActivationCooldown(ItemStack focus) {
        return isUpgradedWith(focus, FocusUpgradeType.chainlightning) ? 500 : isUpgradedWith(focus, FocusUpgradeType.earthshock) ? 1000 : COOLDOWN_MILLIS;
    }
    @Override public int getFocusColor(ItemStack focus) { return 10466239; }
    @Override public String getSortingHelper(ItemStack focus) { return "BL" + super.getSortingHelper(focus); }
    @Override public WandFocusAnimation getAnimation(ItemStack focus) { return isUpgradedWith(focus, FocusUpgradeType.earthshock) ? WandFocusAnimation.WAVE : WandFocusAnimation.CHARGE; }
    @Override public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1, 2 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency};
            case 3 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency, FocusUpgradeType.chainlightning, FocusUpgradeType.earthshock};
            case 4, 5 -> new FocusUpgradeType[]{FocusUpgradeType.frugal, FocusUpgradeType.potency, FocusUpgradeType.enlarge};
            default -> new FocusUpgradeType[0];
        };
    }
    @Override public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgradeType type, int rank) {
        return super.canApplyUpgrade(focus, player, type, rank) && (type != FocusUpgradeType.enlarge
                || isUpgradedWith(focus, FocusUpgradeType.chainlightning) || isUpgradedWith(focus, FocusUpgradeType.earthshock));
    }

    @Override public ItemStack onFocusRightClick(ItemStack wand, Level level, Player player, InteractionHand hand) {
        if (isUpgradedWith(FocusData.get(wand), FocusUpgradeType.earthshock)) {
            if (level.isClientSide ? FocusVis.canPay(wand, Map.of("aer", pulseCost(wand, player)))
                    : player instanceof ServerPlayer server && earthshock(wand, server, hand)) player.swing(hand);
            return wand;
        }
        // :135-136, not a research/affordability gate. The first held tick may
        // fire immediately; subsequent dispatches use the real 250ms clock.
        player.startUsingItem(hand);
        WandFocusActions.clearCooldown(player);
        return wand;
    }

    private static boolean earthshock(ItemStack wand, ServerPlayer player, InteractionHand hand) {
        if (!player.server.isSameThread()) throw new IllegalStateException("Earthshock requires the server thread");
        if (!player.isAlive() || player.isSpectator() || player.getItemInHand(hand) != wand || wand.getCount() != 1
                || !ArcaneVis.isCraftingWand(wand) || !(FocusData.get(wand).getItem() instanceof ItemFocusShock)) return false;
        ItemStack before = wand.copy(), paid = before.copy();
        // Original :123 calls getVisCost(WAND), so the real debit remains25aer.
        Map<String, Integer> costs = Map.of("aer", pulseCost(before, player));
        if (!FocusVis.debit(paid, costs) || player.getItemInHand(hand) != wand || !ItemStack.matches(wand, before)
                || !costs.equals(Map.of("aer", pulseCost(wand, player))) || !FocusVis.canPay(wand, costs)) return false;
        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid)); player.getInventory().setChanged();
        ItemStack focus = FocusData.get(before);
        var orb = new EntityShockOrb(player.level(), player);
        orb.configure(4 + FocusUpgradeData.level(focus, FocusUpgradeType.enlarge) * 2,
                (int)(5 + ItemWandCasting.focusPotency(before) * 1.33));
        player.serverLevel().addFreshEntity(orb);
        player.serverLevel().playSound(null, orb.getX(), orb.getY(), orb.getZ(), ZAP, SoundSource.PLAYERS,
                1F, 1F + (player.level().random.nextFloat() - player.level().random.nextFloat()) * .2F);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        return true;
    }

    public static int pulseCost(ItemStack wand, Player player) {
        // CENTIVIS, not ArcaneVis.price's whole-vis recipe units. Keep FLOAT:
        // Thaumium .9F minus ten gear points prices25 at19, not rounded20.
        return (int) (BASE_COST_CENTIVIS * WandVisDiscount.focusModifier(wand, player, Aspect.AIR));
    }

    @Override public void onUsingFocusTick(ItemStack wand, Player actor, int remaining) {
        if (!(actor instanceof ServerPlayer player) || player.level().isClientSide) return;
        if (!player.server.isSameThread()) throw new IllegalStateException("Shock focus use requires the server thread");
        InteractionHand hand = player.getUsedItemHand();
        if (!player.isAlive() || player.isSpectator() || !player.isUsingItem()
                || player.getUseItem() != wand || player.getItemInHand(hand) != wand || wand.getCount() != 1
                || !ArcaneVis.isCraftingWand(wand) || !(FocusData.get(wand).getItem() instanceof ItemFocusShock)) return;

        ItemStack before = wand.copy(), paid = before.copy();
        if (FocusUpgradeData.level(FocusData.get(before), FocusUpgradeType.earthshock) > 0) return;
        int price = pulseCost(before, player);
        Map<String, Integer> costs = Map.of("aer", price);
        // :147-151 pays before even resolving the ray. Creative is NOT free.
        if (!FocusVis.debit(paid, costs)) {
            player.stopUsingItem();
            return;
        }
        if (player.getItemInHand(hand) != wand || player.getUseItem() != wand || !player.isUsingItem()
                || !ItemStack.matches(wand, before) || price != pulseCost(wand, player) || !FocusVis.canPay(wand, costs)) return;
        wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
        player.getInventory().setChanged();

        int potency = ItemWandCasting.focusPotency(wand);
        boolean chain = FocusUpgradeData.level(FocusData.get(wand), FocusUpgradeType.chainlightning) > 0;
        int jumps = chain ? 2 + FocusUpgradeData.level(FocusData.get(wand), FocusUpgradeType.enlarge) * 2 : 0;
        ItemStack committed = wand.copy();
        var castLevel = player.serverLevel();
        Entity target = pointedEntity(player);
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        HitResult block = player.level().clip(new ClipContext(eye, eye.add(look.scale(10)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        // BlockUtils:237-253's ten-block VISUAL ray is independent of the
        // twenty-block ENTITY ray. A target always supplies its body midpoint.
        Vec3 endpoint = target != null ? target.position().add(0, target.getBbHeight() / 2, 0)
                : block.getType() == HitResult.Type.BLOCK ? block.getLocation() : player.position().add(look.scale(10));
        float yaw = player.getYRot() * (float) Math.PI / 180;
        Vec3 origin = eye.add(-Math.cos(yaw) * .06, -.06, -Math.sin(yaw) * .06).add(look.scale(.3));
        if (player.level() != castLevel || !player.isAlive() || !player.isUsingItem()
                || player.getUseItem() != wand || player.getItemInHand(hand) != wand || !ItemStack.matches(wand, committed)) return;
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SHOCK, SoundSource.PLAYERS, .25F, 1F);
        ShockFxNetworking.send(player, origin, endpoint, target != null || block.getType() == HitResult.Type.BLOCK);

        if (player.level() == castLevel && player.isAlive() && player.isUsingItem()
                && player.getUseItem() == wand && player.getItemInHand(hand) == wand && ItemStack.matches(wand, committed)
                && target instanceof LivingEntity living && (!(target instanceof Player) || player.server.isPvpAllowed())) {
            // Native hurt owns protection events, armor, immunity frames and
            // player-team refusals. Never Player.attack(), magic or a bolt entity.
            living.hurt(player.damageSources().playerAttack(player), (chain ? 6F : 4F) + potency);
            if (chain) chain(player, wand, hand, committed, living, jumps, potency);
        }
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
    }

    private static void chain(ServerPlayer player, ItemStack wand, InteractionHand hand, ItemStack committed,
                              LivingEntity center, int jumps, int potency) {
        var level = player.serverLevel();
        Set<Integer> visited = new HashSet<>(); visited.add(center.getId());
        while (jumps-- > 0) {
            // An actual damage/protection callback cannot lend a paid channel to
            // another held wand or dimension. A refused hurt still chains, as TC4.
            if (player.level() != level || !player.isAlive() || !player.isUsingItem() || player.getUseItem() != wand
                    || player.getItemInHand(hand) != wand || !ItemStack.matches(wand, committed)) return;
            LivingEntity closest = null; double nearest = Double.MAX_VALUE;
            AABB area = new AABB(center.position(), center.position()).inflate(8);
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (candidate == player || visited.contains(candidate.getId()) || candidate instanceof Player && !player.server.isPvpAllowed()) continue;
                double distance = candidate.distanceToSqr(center);
                if (distance < nearest) { closest = candidate; nearest = distance; }
            }
            if (closest == null) continue;
            ShockFxNetworking.sendChain(player, center, closest);
            visited.add(closest.getId());
            closest.hurt(player.damageSources().playerAttack(player), 4F + potency);
            center = closest;
        }
    }

    /** EntityUtils:67-97: broadphase1.1, border>=.8, PER-CANDIDATE eye LOS.
     * A block along the nominal look ray does not replace that original LOS test. */
    private static Entity pointedEntity(Player player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle(), end = eye.add(look.scale(ENTITY_RANGE));
        AABB search = player.getBoundingBox().expandTowards(look.scale(ENTITY_RANGE)).inflate(1.1F);
        Entity selected = null;
        double nearest = 0;
        for (Entity candidate : player.level().getEntities(player, search)) {
            if (!candidate.isPickable() || candidate.isRemoved()
                    || player.level().clip(new ClipContext(eye, candidate.getEyePosition(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) continue;
            AABB box = candidate.getBoundingBox().inflate(Math.max(.8F, candidate.getPickRadius()));
            if (box.contains(eye)) {
                selected = candidate;
                nearest = 0;
                continue;
            }
            var intersection = box.clip(eye, end);
            if (intersection.isEmpty()) continue;
            double distance = eye.distanceTo(intersection.get());
            if (nearest == 0 || distance < nearest) { selected = candidate; nearest = distance; }
        }
        return selected;
    }
}
