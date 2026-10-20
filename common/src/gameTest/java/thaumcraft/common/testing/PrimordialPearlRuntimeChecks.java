package thaumcraft.common.testing;

import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.items.ItemPrimordialPearl;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.testing.NodeTransductionRuntimeChecks.InputRandom;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileNode;

/** Explicit native node/item/branch INPUT fixtures, not survival pearl acquisition or inspected-client evidence. */
public final class PrimordialPearlRuntimeChecks {
    private PrimordialPearlRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        List<ChunkPos> forced = new ArrayList<>();
        try (var f = new Fixture(test)) {
            f.open();
            BlockPos pos = f.base.above(100);
            forced.addAll(GolemChunks.hold(f.level, List.of(pos)));
            chamber(f, pos);
            f.player.setInvulnerable(true); // Declared survival actor remains alive through the actual explosion.
            unresearchedAndClosedGrant(f, pos);
            fallbackAndCreativePayment(f, pos);
            researchedAndSave(f, pos);
            nativeExplosionAndFlux(f, pos);
        } finally { for (ChunkPos chunk : forced) test.getLevel().setChunkForced(chunk.x, chunk.z, false); }
        test.succeed();
    }
    private static void chamber(Fixture f, BlockPos center) {
        // Nine-cube bedrock chamber isolates the REAL block-damaging blast from neighboring test cases.
        for (BlockPos mutable : BlockPos.betweenClosed(center.offset(-4,-4,-4), center.offset(4,4,4))) {
            BlockPos pos = mutable.immutable();
            f.states.putIfAbsent(pos, f.level.getBlockState(pos));
            if (f.level.getBlockEntity(pos) != null) f.originals.putIfAbsent(pos, f.level.getBlockEntity(pos));
            f.level.removeBlockEntity(pos);
            boolean shell = Math.abs(pos.getX()-center.getX()) == 4 || Math.abs(pos.getY()-center.getY()) == 4 || Math.abs(pos.getZ()-center.getZ()) == 4;
            f.level.setBlock(pos, (shell ? Blocks.BEDROCK : Blocks.AIR).defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
        }
    }
    private static TileNode node(Fixture f, BlockPos pos, AspectList values, TileNode.NodeModifier modifier) {
        f.level.setBlock(pos, Blocks.AIR.defaultBlockState(), TubeVariantsRuntimeChecks.QUIET);
        f.put(pos, ThaumcraftContent.AURA_NODE.get());
        TileNode node = (TileNode) f.level.getBlockEntity(pos);
        node.initializeNatural(values, modifier);
        f.aim(Vec3.atCenterOf(pos)); f.player.setDeltaMovement(Vec3.ZERO);
        return node;
    }
    private static ItemStack pearl(Fixture f) {
        ItemStack pearl = new ItemStack(ThaumcraftContent.PRIMORDIAL_PEARL.get());
        pearl.set(DataComponents.CUSTOM_NAME, Component.literal("one-physical-primordial-pearl"));
        f.player.setItemInHand(InteractionHand.MAIN_HAND, pearl);
        return pearl;
    }
    private static void unresearchedAndClosedGrant(Fixture f, BlockPos pos) {
        KnowledgeAccess.get(f.player).removeResearch("PRIMNODE");
        var node = node(f, pos, new AspectList().add(Aspect.AIR,1).add(Aspect.FIRE,2).add(Aspect.METAL,7).add(Aspect.LIGHT,0), null);
        var before = node.saveWithoutMetadata(f.level.registryAccess());
        var change = node.preparePrimalPearl(RandomSource.create(3), false);
        f.check(change != null && node.matchesPreparedPrimalPearl(change), "current ordinary node can prepare an opaque exact mutation");
        boolean refused = false;
        try { node.applyPreparedPrimalPearl(change); } catch (IllegalStateException expected) { refused = true; }
        f.check(refused && before.equals(node.saveWithoutMetadata(f.level.registryAccess())), "unpaid public opaque token cannot mutate current/base/modifier");
        node.takeFromContainer(Aspect.METAL,1);
        f.check(!node.matchesPreparedPrimalPearl(change), "a changed real node rejects its stale prepared token");
        node = node(f, pos, new AspectList().add(Aspect.AIR,1).add(Aspect.FIRE,2).add(Aspect.METAL,7).add(Aspect.LIGHT,0), null);
        String id = node.getScanId(); int regeneration = node.getRegenerationTicks();
        var pearl = pearl(f);
        f.check(pearl.getItem() instanceof ItemPrimordialPearl && pearl.getRarity() == Rarity.EPIC && pearl.getMaxStackSize() == 1,
                "real flattened metadata3 has original EPIC and stack1 identity");
        var pearlAspects = thaumcraft.common.lib.research.ObjectAspectResolver.get(pearl,f.level);
        f.check(pearlAspects.size()==6 && pearlAspects.visSize()==96, "actual native primordial pearl resolves exact original96 essentia, six primal tags");
        for(Aspect primal:Aspect.getPrimalAspects()) f.check(pearlAspects.getAmount(primal)==16, "original direct primordial pearl aspect16: "+primal.getTag());
        try (var rng = new InputRandom(f, bound -> 0, true)) {
            f.check(f.use(pos).consumesAction(), "actual native survival game-mode use accepts a physical pearl without PRIMNODE research");
            prefix(f, rng.bounds, List.of(6,6,3,3,3,3,3,3,5));
            tail(f, rng.bounds);
        }
        f.check(pearl.isEmpty() && f.player.getMainHandItem().isEmpty(), "one actual physical pearl is consumed once");
        f.check(node.getNodeVisBase(Aspect.AIR) == 1 && node.getNodeVisBase(Aspect.FIRE) == 0
                        && node.getAspectsBase().aspects.containsKey(Aspect.FIRE) && node.getAspectsBase().aspects.containsKey(Aspect.LIGHT)
                        && node.getNodeVisBase(Aspect.METAL) == 7 && node.getAspects().getAmount(Aspect.FIRE) == 2,
                "negative target is REFUSED, zero BASE key retained, false compound roll skips, lowered BASE leaves current temporarily over capacity");
        f.check(node.getNodeModifier() == TileNode.NodeModifier.BRIGHT && node.getRegenerationTicks() == regeneration
                        && node.getScanId().equals(id) && node.isNatural() && node.getTickCount() == 0,
                "one null→BRIGHT upgrade retains actual regeneration cache, identity, natural flag and transient count");
        var paid = node.saveWithoutMetadata(f.level.registryAccess());
        f.check(!f.use(pos).consumesAction() || node.saveWithoutMetadata(f.level.registryAccess()).equals(paid), "empty hand cannot replay paid primordial mutation");
        f.check(node.saveWithoutMetadata(f.level.registryAccess()).equals(paid), "node state after empty-handed use remains paid result");
    }
    private static void fallbackAndCreativePayment(Fixture f, BlockPos pos) {
        var node = node(f, pos, new AspectList().add(Aspect.AIR,3), TileNode.NodeModifier.FADING);
        f.mode(GameType.CREATIVE); var pearl = pearl(f);
        try (var rng = new InputRandom(f, bound -> bound == 3 ? 2 : 0, true)) {
            f.check(f.use(pos).consumesAction(), "native creative wrapper accepts original consumable use");
            prefix(f, rng.bounds, List.of(6,3,3,3,3,3,3)); tail(f, rng.bounds);
        }
        f.check(f.player.getMainHandItem().isEmpty() && f.player.getMainHandItem() != pearl
                        && f.player.getInventory().countItem(ThaumcraftContent.PRIMORDIAL_PEARL.get()) == 0,
                "native creative wrapper may restore its detached argument, but cannot restore a physical owned pearl to the hand/inventory");
        f.check(node.getNodeVisBase(Aspect.AIR) == 2 && node.getAspects().getAmount(Aspect.AIR) == 2,
                "original fallback raises lowered BASE1→2 and addToContainer REDUCES overfull current3→2");
        for (Aspect aspect : Aspect.getPrimalAspects()) if (aspect != Aspect.AIR)
            f.check(node.getNodeVisBase(aspect) == 2 && node.getAspects().getAmount(aspect) == 1, "new primal receives BASE2 but ONE actual current unit: " + aspect.getTag());
        f.check(node.getNodeModifier() == TileNode.NodeModifier.FADING && node.getRegenerationTicks() == -1,
                "false fading coin stops the modifier chain without a second pale/null upgrade or regeneration reset");
        f.mode(GameType.SURVIVAL);
    }
    private static void researchedAndSave(Fixture f, BlockPos pos) {
        KnowledgeAccess.get(f.player).addResearch("PRIMNODE");
        var node = node(f, pos, new AspectList().add(Aspect.AIR,3), TileNode.NodeModifier.PALE);
        pearl(f);
        try (var rng = new InputRandom(f, bound -> bound == 9 ? 8 : bound == 4 ? 3 : 0, true)) {
            f.check(f.use(pos).consumesAction(), "actual actor's PRIMNODE selects researched random bounds");
            prefix(f, rng.bounds, List.of(9,4,4,4,4,4,4)); tail(f, rng.bounds);
        }
        f.check(node.getNodeVisBase(Aspect.AIR) == 9 && node.getAspects().getAmount(Aspect.AIR) == 3,
                "researched first pass m−2+8 raises capacity without inventing current refill when fallbackr3 is below BASE9");
        for (Aspect aspect : Aspect.getPrimalAspects()) if (aspect != Aspect.AIR)
            f.check(node.getNodeVisBase(aspect) == 3 && node.getAspects().getAmount(aspect) == 1, "researched fallback bound4 admits capacity3, only one current unit");
        f.check(node.getNodeModifier() == TileNode.NodeModifier.PALE, "false pale coin retains modifier without null/bright followthrough");
        var saved = node.saveWithFullMetadata(f.level.registryAccess());
        f.level.removeBlockEntity(pos);
        var restored = (TileNode) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, node.getBlockState(), saved, f.level.registryAccess());
        f.level.setBlockEntity(restored);
        f.check(restored != node && restored.getAspects().aspects.equals(node.getAspects().aspects)
                        && restored.getAspectsBase().aspects.equals(node.getAspectsBase().aspects)
                        && restored.getNodeModifier() == node.getNodeModifier() && restored.getScanId().equals(node.getScanId()),
                "actual native current/base/modifier/scan identity survive encode/decode and installation, with no extra pearl or refill");
        f.check(node.preparePrimalPearl(RandomSource.create(1), true) == null, "retired original BE cannot prepare mutation of its replacement");
    }
    private static void nativeExplosionAndFlux(Fixture f, BlockPos pos) {
        KnowledgeAccess.get(f.player).removeResearch("PRIMNODE");
        node(f, pos, new AspectList().add(Aspect.AIR,1), TileNode.NodeModifier.BRIGHT);
        f.put(pos.above(), Blocks.GLASS); pearl(f);
        f.chat(); // Discard prior operations' packets before observing THIS native blast.
        int[] previousBound = {0}, placementDraws = {0}; boolean[] explosionShuffled = {false};
        try (var rng = new InputRandom(f, bound -> {
            // Native terrain-damaging Explosion.finalizeExplosion calls Util.shuffle(toBlow, level.random).
            // Its Fisher-Yates integer prefix descends ...7,6,5,4,3,2 BEFORE the original pearl's33 trials.
            // Do not confuse that native bound6 with a pearl x/y/z draw (ExplosionInteraction.NONE has no such prefix).
            if (!explosionShuffled[0]) {
                if (previousBound[0] == 3 && bound == 2) explosionShuffled[0] = true;
                previousBound[0] = bound;
                return 0;
            }
            f.check(bound == 6, "after the completed native block-explosion shuffle, only original pearl bound6 placement draws remain");
            int draw = placementDraws[0]++;
            // Trial1=(+1,-1,0), trial2=(+1,0,0); all later trials hit occupied origin.
            return draw == 0 || draw == 3 || draw == 6 ? 1 : 0;
        }, true)) {
            f.check(f.use(pos).consumesAction(), "actual physical use reaches its paid native explosion and flux tail");
            prefix(f, rng.bounds, List.of(6,3,3,3,3,3,3));
            tail(f, rng.bounds);
            f.check(explosionShuffled[0] && placementDraws[0] == 198,
                    "native block-explosion shuffle completes before EXACTLY198 original pearl placement draws, none consumed by native shuffle");
        }
        f.check(f.level.getBlockState(pos.above()).isAir(), "original smoking=true maps to an actual terrain-damaging native blast");
        var goo = f.level.getBlockState(pos.offset(1,-1,0)); var gas = f.level.getBlockState(pos.offset(1,0,0));
        f.check(goo.is(ThaumcraftFlux.FLUX_GOO.get()) && goo.getValue(FluxFiniteBlock.LEVEL) == 8
                        && gas.is(ThaumcraftFlux.FLUX_GAS.get()) && gas.getValue(FluxFiniteBlock.LEVEL) == 8,
                "33 actual six-draw trials place original metadata8 goo below node Y and metadata8 gas at node Y; goo="+goo+", gas="+gas);
        f.connection.flushChannel(); f.channel.runPendingTasks(); Object packet; int blasts = 0;
        while ((packet = f.channel.readOutbound()) != null) {
            try {
                if (packet instanceof ClientboundExplodePacket blast) {
                    blasts++; f.check(blast.getPower() == 5.5F && blast.getToBlow().contains(pos.above()),
                            "actual packet proves unresearched strength3+.5*5 and affected real glass, not guessed particle/audio radius");
                }
            } finally { ReferenceCountUtil.release(packet); }
        }
        f.check(blasts == 1, "one paid physical pearl publishes exactly one native explosion");
    }
    private static void prefix(Fixture f, List<Integer> actual, List<Integer> expected) {
        f.check(actual.size() >= expected.size() && actual.subList(0,expected.size()).equals(expected),
                "actual native current-key then six-primal branch RNG prefix is original: " + expected + ", actual=" + actual);
    }
    private static void tail(Fixture f, List<Integer> bounds) {
        f.check(bounds.size() >= 198 && bounds.subList(bounds.size()-198,bounds.size()).stream().allMatch(bound -> bound == 6),
                "after actual native explosion, all33 flux trials consume SIX bound6 offsets, even when target is occupied");
    }
}
