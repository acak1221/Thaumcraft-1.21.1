package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.config.ThaumcraftPech;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.ScanCallbacks;
import thaumcraft.common.lib.research.ScanManager;

/** Real registered Pech entities and their saved byte predicates; no replacement aspect registrations or injected rewards. */
public final class PechScanRuntimeChecks {
    private PechScanRuntimeChecks() {}

    public static void run(GameTestHelper test, ServerPlayer player) {
        var level = test.getLevel(); var knowledge = KnowledgeAccess.get(player); var oldKnowledge = knowledge.copy();
        var entities = new ArrayList<EntityPech>(); var signatures = new HashSet<String>();
        try {
            // Declared fixture: all parent aspects are known, with empty RP pools, so original variant payouts are observable.
            knowledge.reset(); for (Aspect aspect : Aspect.aspects.values()) knowledge.addDiscoveredAspect(aspect.getTag());
            for (int type = 0; type < 3; type++) {
                var pech = ThaumcraftPech.PECH.get().create(level);
                test.assertTrue(pech != null, "actual registered Pech entity exists"); entities.add(pech);
                pech.setNoAi(true); pech.setNoGravity(true); pech.setSilent(true); pech.setPechType(type);
                pech.setPos(Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(2 + type, 3, 3))));
                test.assertTrue(level.addFreshEntity(pech), "actual native entity enters the world index");
                var saved = pech.saveWithoutId(new CompoundTag());
                test.assertTrue(saved.contains("PechType", Tag.TAG_BYTE) && saved.getByte("PechType") == type,
                        "original predicate reads the real entity's saved PechType BYTE");
                var described = EntityAspectBindings.describe(pech);
                test.assertTrue(described != null && described.originalName().equals("Thaumcraft.Pech")
                                && described.signature().equals("@entity:Thaumcraft.PechPechType" + type)
                                && signatures.add(described.signature()),
                        "native Pech ID uses original name and a distinct persistent identity for each saved byte variant");
                var aspects = described.aspects();
                test.assertTrue(aspects.size() == (type == 2 ? 3 : 4)
                                && aspects.getAmount(Aspect.MAN) == 2 && aspects.getAmount(Aspect.EXCHANGE) == 2
                                && aspects.getAmount(Aspect.MAGIC) == (type == 2 ? 4 : 2)
                                && aspects.getAmount(Aspect.GREED) == (type == 0 ? 2 : 0)
                                && aspects.getAmount(Aspect.WEAPON) == (type == 1 ? 2 : 0),
                        "original ConfigAspects84..86 replace whole rewards:0 greed,1 weapon,2 four magic");
                var resolved = ScanCallbacks.resolve(level, ScanResult.entity(pech));
                test.assertTrue(resolved != null && resolved.kind() == ScanManager.ScanKind.ENTITY
                                && resolved.signature().equals(described.signature()) && resolved.sessionKey().equals("entity:" + pech.getId()),
                        "actual indexed public scan target preserves variant knowledge and entity-instance use identity");
                test.assertTrue(ScanManager.completeScan(player, resolved.signature(), resolved.aspects(), resolved.kind(), resolved.clue())
                                && knowledge.hasScannedEntity(described.signature()),
                        "actual production receiver accepts every distinct original Pech variant");
                var beforeDuplicate = knowledge.save();
                test.assertTrue(!ScanManager.completeScan(player, resolved.signature(), resolved.aspects(), resolved.kind(), resolved.clue())
                                && knowledge.save().equals(beforeDuplicate), "same variant cannot repay RP or reroll a clue");
            }
            test.assertTrue(knowledge.getAspectPoolFor("humanus") == 6 && knowledge.getAspectPoolFor("permutatio") == 6
                            && knowledge.getAspectPoolFor("praecantatio") == 8 && knowledge.getAspectPoolFor("lucrum") == 2
                            && knowledge.getAspectPoolFor("telum") == 2,
                    "three actual entity scans pay exact original cumulative6 man/6 exchange/8 magic/2 greed/2 weapon");
            entities.getFirst().setPechType(3);
            test.assertTrue(EntityAspectBindings.describe(entities.getFirst()) == null
                            && ScanCallbacks.resolve(level, ScanResult.entity(entities.getFirst())) == null,
                    "unregistered saved PechType3 cannot fall back to a neighboring variant's reward");
            test.succeed();
        } finally {
            for (var entity : entities) entity.discard(); knowledge.copyFrom(oldKnowledge);
        }
    }
}
