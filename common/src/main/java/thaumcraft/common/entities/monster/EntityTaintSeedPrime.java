package thaumcraft.common.entities.monster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftTaintAnimals;
import thaumcraft.common.lib.taint.TaintAnimalRules;

/**
 * The tainted prime seed of the 1.12.2 backport: 150HP/7damage, the doubled spread area and
 * the one to three flux crystal drop.
 *
 * <p>TC4 4.2.3.5 has no tainted seed: the entity comes from
 * {@code Thaumcraft-1.12.2-6.1.BETA26.jar}
 * ({@code thaumcraft/common/entities/monster/tainted/EntityTaintSeedPrime.java}).
 */
public final class EntityTaintSeedPrime extends EntityTaintSeed {
    public EntityTaintSeedPrime(EntityType<? extends EntityTaintSeedPrime> type,Level level) {
        super(type,level);
        xpReward=TaintAnimalRules.seedExperience(true);
    }
    public EntityTaintSeedPrime(Level level) { this(ThaumcraftTaintAnimals.TAINT_SEED_PRIME.get(),level); }
    @Override protected boolean isPrime() { return true; }
    public static AttributeSupplier.Builder createAttributes() {
        return EntityTaintSeed.createAttributes()
                .add(Attributes.MAX_HEALTH,TaintAnimalRules.seedMaxHealth(true))
                .add(Attributes.ATTACK_DAMAGE,TaintAnimalRules.seedAttackDamage(true));
    }
    @Override protected void dropFluxCrystals(ServerLevel level) {
        int crystals=TaintAnimalRules.seedPrimeCrystals(level.random::nextBoolean);
        for(int i=0;i<crystals;i++)spawnAtLocation(new ItemStack(ThaumcraftTaintAnimals.FLUX_CRYSTAL.get()),getBbHeight()/2F);
    }
}
