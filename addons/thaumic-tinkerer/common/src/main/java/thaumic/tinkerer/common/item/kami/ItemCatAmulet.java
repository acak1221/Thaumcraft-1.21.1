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
package thaumic.tinkerer.common.item.kami;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Feline Amulet: while it is worn in the amulet slot, creepers run away from the wearer and cannot gather the
 * stoke to explode.
 *
 * <p>1.7.10 rewrote the goals of every mob around the wearer once and for good: a creeper's "avoid ocelots" goal
 * avoided players from then on, and every "nearest attackable player" goal of any mob targeted an ender crystal
 * instead. 1.21.1 goals have final fields and Fabric would have to reach them by name, so the effect is applied again
 * every tick while the amulet is worn and ends with it (see {@code docs/deviations/KamiGear.md}): creepers within 10 x 4
 * blocks lose the fuse (a creeper that was already swelling stops) and walk away, and mobs that would hunt the wearer
 * without having been provoked lose that target (see {@link #stopsLookingFor}).</p>
 */
public class ItemCatAmulet extends KamiItemBase implements IBauble {
    private static final int RANGE = 10;
    private static final int RANGE_Y = 4;
    /** The distance 1.7.10's ocelot avoidance used ({@code EntityCreeper}: 6 blocks, walking speed 1.0, running 1.2). */
    private static final double FLEE_DISTANCE = 6.0;

    public ItemCatAmulet(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.AMULET;
    }

    @Override
    public void onWornTick(ItemStack stack, LivingEntity wearer) {
        Level level = wearer.level();
        if (level.isClientSide) return;
        AABB box = new AABB(wearer.getX() - RANGE, wearer.getY() - RANGE_Y, wearer.getZ() - RANGE, wearer.getX() + RANGE,
                wearer.getY() + RANGE_Y, wearer.getZ() + RANGE);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
            if (mob instanceof Creeper creeper) {
                // 1.7.10: timeSinceIgnited = 2 and no attack target, for every creeper around, whoever it was after.
                creeper.setTarget(null);
                creeper.setSwellDir(-1);
                flee(creeper, wearer);
            } else if (stopsLookingFor(mob, wearer)) {
                mob.setTarget(null);
            }
        }
    }

    /**
     * What the swapped goals of 1.7.10 amounted to for the other mobs: a mob with a "nearest attackable player" goal never found the
     * wearer, but a mob that already had a target, or hit back at the wearer, went on. So a mob loses the wearer as its target
     * only if it is after the wearer without having been hurt by the wearer lately (a mob hit by the wearer keeps its revenge
     * for the 100 ticks the game remembers it). Left alone, because 1.7.10's goals did not reach them: the neutral mobs (wolves,
     * bees, zombified piglins... which are after the wearer only when angered), the Wither and Endermen (their own targeting) and the
     * mobs that think with a brain (Warden, piglins, hoglins: they did not exist, and reading a memory the brain does not have
     * throws).
     */
    private static boolean stopsLookingFor(Mob mob, LivingEntity wearer) {
        if (mob.getTarget() != wearer || mob.getLastHurtByMob() == wearer) return false;
        if (mob instanceof NeutralMob || mob instanceof WitherBoss || mob instanceof EnderMan) return false;
        return !mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED);
    }

    private static void flee(Creeper creeper, LivingEntity from) {
        double distance = creeper.distanceToSqr(from);
        if (distance >= FLEE_DISTANCE * FLEE_DISTANCE) return;
        if (!creeper.getNavigation().isDone() && (creeper.tickCount + creeper.getId()) % 10 != 0) return;
        Vec3 away = DefaultRandomPos.getPosAway(creeper, 16, 7, from.position());
        if (away != null) creeper.getNavigation().moveTo(away.x, away.y, away.z, distance < 49 ? 1.2 : 1.0);
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_CAT_AMULET, new ItemStack(this), 8,
                new AspectList().add(Aspect.DARKNESS, 16).add(Aspect.ORDER, 32).add(Aspect.MIND, 16),
                new ItemStack(Items.QUARTZ_BLOCK), ItemKamiResource.stack(ItemKamiResource.ICHOR),
                new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.COCOA_BEANS),
                new ItemStack(Items.JUNGLE_LEAVES), new ItemStack(Items.COD));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_CAT_AMULET,
                new AspectList().add(Aspect.MIND, 2).add(Aspect.ORDER, 1).add(Aspect.DARKNESS, 1).add(Aspect.DEATH, 1),
                13, 10, TTRegistry.stack(LibItemNames.CAT_AMULET), 0, KamiResearch.of(LibResearch.KEY_ICHORIUM),
                KamiResearch.NONE, new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_CAT_AMULET));
    }
}
