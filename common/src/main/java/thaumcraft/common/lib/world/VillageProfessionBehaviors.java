package thaumcraft.common.lib.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.npc.Villager;
import thaumcraft.common.config.ThaumcraftVillageProfessions;

/** Preserve the original job-site-free professions while ordinary villagers keep their native reset behavior. */
public final class VillageProfessionBehaviors {
    private VillageProfessionBehaviors() {}

    public static BehaviorControl<Villager> preserveOriginalProfessions(BehaviorControl<Villager> original) {
        return new ProfessionReset(original);
    }

    public static boolean hasOriginalProfession(Villager villager) {
        var profession = villager.getVillagerData().getProfession();
        return profession == ThaumcraftVillageProfessions.BANKER.get()
                || profession == ThaumcraftVillageProfessions.WIZARD.get();
    }

    private static final class ProfessionReset implements BehaviorControl<Villager> {
        private final BehaviorControl<Villager> original;

        private ProfessionReset(BehaviorControl<Villager> original) { this.original = original; }

        @Override public Behavior.Status getStatus() { return original.getStatus(); }

        @Override public boolean tryStart(ServerLevel level, Villager villager, long gameTime) {
            // The generator sets profession AFTER the native Brain is built, so inspect the live actor here.
            if (hasOriginalProfession(villager)) return false;
            return original.tryStart(level, villager, gameTime);
        }

        @Override public void tickOrStop(ServerLevel level, Villager villager, long gameTime) {
            original.tickOrStop(level, villager, gameTime);
        }

        @Override public void doStop(ServerLevel level, Villager villager, long gameTime) {
            original.doStop(level, villager, gameTime);
        }

        @Override public String debugString() { return original.debugString(); }
    }
}
