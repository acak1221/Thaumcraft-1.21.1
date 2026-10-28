/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item.kami.armor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.core.handler.kami.KamiArmorHandler;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Robes of the Stratosphere: flight (the wings are drawn by the client, see {@code WingsLayer}) and a passive shield that
 * pushes simple projectiles past the wearer, like the Focus of Distortion.
 *
 * <p>1.7.10 kept a list of the players it had given flight ({@code playersWithFlight}, entries "name:remote"); the server
 * keeps their ids and sets the abilities, which the game sends to the client.</p>
 */
public class ItemGemChest extends ItemIchorclothArmorAdv {
    /** The players whose flight this armour has granted. */
    private static final Set<UUID> PLAYERS_WITH_FLIGHT = ConcurrentHashMap.newKeySet();

    public ItemGemChest(Item.Properties properties) {
        super(ArmorItem.Type.CHESTPLATE, properties);
    }

    private static boolean shouldPlayerHaveFlight(Player player) {
        ItemStack armor = player.getItemBySlot(EquipmentSlot.CHEST);
        return armor.getItem() instanceof ItemGemChest && KamiArmorHandler.getArmorStatus(player) && !isDisabled(armor)
                && ConfigHandler.enableFlight;
    }

    /** 1.7.10 {@code tickPlayer}, run while the chestplate is worn. */
    public static void tickPlayer(Player player, ItemStack armor) {
        if (player.level().isClientSide || isDisabled(armor) || !KamiArmorHandler.getArmorStatus(player)) return;
        protectFromProjectiles(player, 0);
    }

    /** 1.7.10 {@code updatePlayerFlyStatus}, for every player on every tick; the server decides, the abilities are sent. */
    public static void updatePlayerFlyStatus(Player player) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer) || player.isSpectator()) return;
        Abilities abilities = player.getAbilities();
        UUID id = player.getUUID();
        if (PLAYERS_WITH_FLIGHT.contains(id)) {
            if (shouldPlayerHaveFlight(player)) {
                if (!abilities.mayfly) {
                    abilities.mayfly = true;
                    player.onUpdateAbilities();
                }
            } else {
                if (!abilities.instabuild) {
                    abilities.mayfly = false;
                    abilities.flying = false;
                    abilities.invulnerable = false;
                    player.onUpdateAbilities();
                }
                PLAYERS_WITH_FLIGHT.remove(id);
            }
        } else if (shouldPlayerHaveFlight(player)) {
            PLAYERS_WITH_FLIGHT.add(id);
            abilities.mayfly = true;
            player.onUpdateAbilities();
        }
    }

    /** A player left the server: the list of the flying forgets it. */
    public static void forget(Player player) {
        PLAYERS_WITH_FLIGHT.remove(player.getUUID());
    }

    /**
     * 1.7.10 {@code ItemFocusDeflect.protectFromProjectiles(player, null)} (the Distortion focus belongs to the Foci
     * group; this is the Kami copy of the same rule): every arrow and thrown object within 4 blocks (3 above and to the
     * east/south) that was not shot by the player is moved along its flight by twice its distance from the player, which
     * carries it past. The experience bottle is exempt, as in the original blacklist.
     */
    public static void protectFromProjectiles(Player player, int range) {
        Level level = player.level();
        AABB box = new AABB(player.getX() - (4 + range), player.getY() - (4 + range), player.getZ() - (4 + range),
                player.getX() + (3 + range), player.getY() + (3 + range), player.getZ() + (3 + range));
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box)) {
            if (!(projectile instanceof AbstractArrow || projectile instanceof ThrowableProjectile)) continue;
            if (projectile instanceof ThrownExperienceBottle || projectile.getOwner() == player) continue;
            Vec3 motion = projectile.getDeltaMovement();
            if (motion.lengthSqr() < 1.0E-7) continue;
            double distance = projectile.position().distanceTo(player.position());
            Vec3 shift = motion.normalize().scale(distance * 2);
            projectile.setPos(projectile.getX() + shift.x, projectile.getY() + shift.y, projectile.getZ() + shift.z);
        }
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_ICHORCLOTH_CHEST_GEM, new ItemStack(this), 13,
                new AspectList().add(Aspect.AIR, 50).add(Aspect.ARMOR, 32).add(Aspect.FLIGHT, 32)
                        .add(Aspect.ORDER, 32).add(Aspect.LIGHT, 64).add(Aspect.ELDRITCH, 16).add(Aspect.SENSES, 16),
                TTRegistry.stack(LibItemNames.ICHOR_CHEST), new ItemStack(Items.DIAMOND),
                ItemKamiResource.stack(ItemKamiResource.ICHOR), ItemKamiResource.stack(ItemKamiResource.ICHOR),
                KamiResearch.tc("itemFocusPrimal", 0), KamiResearch.tc("itemThaumonomicon", 0),
                new ItemStack(Items.GOLDEN_CHESTPLATE), TTRegistry.stack(LibItemNames.FOCUS_FLIGHT),
                KamiResearch.tc("itemHoverHarness", 0), TTRegistry.stack(LibItemNames.FOCUS_DEFLECT),
                new ItemStack(Items.FEATHER), new ItemStack(Items.GHAST_TEAR), new ItemStack(Items.ARROW));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_ICHORCLOTH_CHEST_GEM,
                new AspectList().add(Aspect.AIR, 2).add(Aspect.MOTION, 1).add(Aspect.FLIGHT, 1).add(Aspect.ELDRITCH, 1),
                17, 7, TTRegistry.stack(LibItemNames.ICHOR_CHEST_GEM), 0, KamiResearch.of(LibResearch.KEY_ICHORCLOTH_ARMOR),
                KamiResearch.NONE, new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_ICHORCLOTH_CHEST_GEM));
    }
}
