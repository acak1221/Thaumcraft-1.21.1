package thaumcraft.common.lib.research;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;

/** TC4 thaumometer rewards, separated from world target selection and use timing. */
public final class ScanManager {
    public static final int DEFAULT_ASPECT_TOTAL_CAP = 100;

    public enum ScanKind { ITEM, ENTITY, PHENOMENA }
    public enum Failure { NONE, INVALID_TARGET, EMPTY_ASPECTS, DUPLICATE, MISSING_PARENT }

    public record ScanOutcome(boolean success, Failure failure, Aspect missingParent,
                              Map<String, Integer> awards, Set<String> discovered) {
        public ScanOutcome {
            awards = Collections.unmodifiableMap(new LinkedHashMap<>(awards));
            discovered = Collections.unmodifiableSet(new LinkedHashSet<>(discovered));
        }

        private static ScanOutcome rejected(Failure failure, Aspect missingParent) {
            return new ScanOutcome(false, failure, missingParent, Map.of(), Set.of());
        }
    }

    private ScanManager() {}

    /**
     * TC4 ScanManager.generateNodeAspects scales each node aspect before scan
     * rewards: max(4, vis / 10), in sorted aspect order. Reading a node must not
     * consume or expose its full vis as research points. The node overload below adds the original
     * node-type and brightness bonus aspects.
     */
    public static AspectList nodeScanAspects(AspectList nodeAspects) {
        AspectList result = new AspectList();
        if (nodeAspects != null) {
            for (Aspect aspect : nodeAspects.getAspectsSorted()) {
                result.merge(aspect, Math.max(4, nodeAspects.getAmount(aspect) / 10));
            }
        }
        return result;
    }

    /**
     * The caller must already have validated the held thaumometer, elapsed use
     * time, and server-observed target. No client-supplied stack is trusted here.
     */
    public static boolean completeScan(ServerPlayer player, String signature, AspectList aspects, ScanKind kind) {
        return completeScan(player, signature, aspects, kind, null);
    }

    public static boolean completeScan(ServerPlayer player, String signature, AspectList aspects, ScanKind kind, ResearchClues.Context context) {
        if (player == null || !player.server.isSameThread()) return false;
        ScanOutcome result = awardScan(KnowledgeAccess.get(player), signature, aspects, kind,
                thaumcraft.common.config.ThaumcraftSettings.server().aspectCap());
        if (!result.success()) {
            if (result.failure() == Failure.MISSING_PARENT) {
                player.sendSystemMessage(Component.translatable("tc.discoveryerror",
                        Component.translatable("tc.aspect.help." + result.missingParent().getTag())));
            } else if (result.failure() == Failure.EMPTY_ASPECTS || result.failure() == Failure.INVALID_TARGET) {
                player.sendSystemMessage(Component.translatable("tc.unknownobject"));
            }
            return false;
        }

        ResearchClues.reveal(KnowledgeAccess.get(player), context, result.awards(), bound -> player.serverLevel().random.nextInt(bound));
        KnowledgeEvents.sync(player);
        for (String tag : result.discovered()) {
            Aspect aspect = Aspect.getAspect(tag);
            player.sendSystemMessage(Component.translatable("tc.port.scan.discovery",
                    Component.literal(aspect.getName())));
        }
        if (!result.awards().isEmpty()) {
            String points = result.awards().entrySet().stream()
                    .map(entry -> Aspect.getAspect(entry.getKey()).getName() + " +" + entry.getValue())
                    .reduce((left, right) -> left + ", " + right).orElse("");
            player.sendSystemMessage(Component.translatable("tc.port.scan.points", Component.literal(points)));
        }
        return true;
    }

    /**
     * Pure with respect to Minecraft world/network state. Mutates only the
     * supplied player knowledge after the entire scan passes its preflight.
     * Native signatures replace TC4 numeric item/metadata hashes for fresh worlds.
     */
    public static ScanOutcome awardScan(PlayerKnowledge knowledge, String signature,
                                        AspectList aspects, ScanKind kind, int poolCap) {
        if (knowledge == null || kind == null || signature == null || signature.isEmpty()) {
            return ScanOutcome.rejected(Failure.INVALID_TARGET, null);
        }
        if (wasScanned(knowledge, signature, kind)) return ScanOutcome.rejected(Failure.DUPLICATE, null);
        if (aspects == null || aspects.size() == 0) return ScanOutcome.rejected(Failure.EMPTY_ASPECTS, null);
        // Original config accepts any signed integer; zero/negative caps immediately apply its soft-cap arithmetic.

        AspectList scanAspects = aspects.copy();
        for (Aspect aspect : scanAspects.getAspects()) {
            if (aspect == null) return ScanOutcome.rejected(Failure.EMPTY_ASPECTS, null);
            if (!aspect.isPrimal()) {
                for (Aspect parent : aspect.getComponents()) {
                    if (!knowledge.hasDiscoveredAspect(parent.getTag())) {
                        return ScanOutcome.rejected(Failure.MISSING_PARENT, parent);
                    }
                }
            }
        }

        Map<String, Integer> awards = new LinkedHashMap<>();
        Set<String> discovered = new LinkedHashSet<>();
        for (Aspect aspect : scanAspects.getAspects()) {
            String tag = aspect.getTag();
            int amount = scanAspects.getAmount(aspect);
            if (!knowledge.hasDiscoveredAspect(tag)) {
                knowledge.addDiscoveredAspect(tag);
                discovered.add(tag);
                amount += 2;
            }
            int currentPool = knowledge.getAspectPoolFor(tag);
            // TC4 checkAndSyncAspectKnowledge: this is diminishing returns,
            // not a hard pool ceiling. Apply the two rules in the original order.
            if (currentPool >= poolCap) amount = (int) Math.sqrt(amount);
            if (amount > 1 && (float) currentPool >= (float) poolCap * 1.25F) amount = 1;
            if (knowledge.addAspectPool(tag, amount) && amount > 0) awards.put(tag, amount);
        }
        markScanned(knowledge, signature, kind);
        return new ScanOutcome(true, Failure.NONE, null, awards, discovered);
    }

    private static boolean wasScanned(PlayerKnowledge knowledge, String signature, ScanKind kind) {
        return switch (kind) {
            case ITEM -> knowledge.hasScannedItem(signature);
            case ENTITY -> knowledge.hasScannedEntity(signature);
            case PHENOMENA -> knowledge.hasScannedPhenomena(signature);
        };
    }

    private static void markScanned(PlayerKnowledge knowledge, String signature, ScanKind kind) {
        switch (kind) {
            case ITEM -> knowledge.scanItem(signature);
            case ENTITY -> knowledge.scanEntity(signature);
            case PHENOMENA -> knowledge.scanPhenomena(signature);
        }
    }
}
