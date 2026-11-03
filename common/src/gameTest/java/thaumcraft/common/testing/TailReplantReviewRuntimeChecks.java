package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.EnumGolemType;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;

/** A real Order harvest must pay its replant before native post-placement observers can take the seed. */
public final class TailReplantReviewRuntimeChecks {
    private TailReplantReviewRuntimeChecks() {}

    public static void replantPaysSeedBeforeNativePlacementObserver(GameTestHelper test) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            // Always cross a chunk boundary, including at negative coordinates.
            // The nearest x=15 column is at most eight blocks from the owned
            // fixture; both the home and crop remain inside its drop bounds.
            BlockPos home = f.base.offset(7 - Math.floorMod(f.base.getX() + 8, 16), 0, 0);
            var held = GolemChunks.hold(f.level, List.of(home, home.east()));
            try {
                GameTestChunkAwait.entityTicking(test, home);
                GameTestChunkAwait.entityTicking(test, home.east());
                Outcome plain = harvest(test, f, home, false);
                test.assertTrue(plain.harvested() && plain.planted() && plain.playerSeeds() == 0 && plain.looseSeeds() == 0
                                && plain.events() == 1,
                        "ordinary actual Order harvest plants one wart using its one physical seed: " + plain);
                f.reset();
                Outcome observed = harvest(test, f, home, true);
                test.assertTrue(observed.harvested() && observed.planted() && observed.events() == 1,
                        "native placement observer is reached during one genuine paid replant: " + observed);
                test.assertTrue(observed.playerSeeds() == 0 && observed.looseSeeds() == 0,
                        "one physical seed pays one surviving crop before a native BLOCK_PLACE listener attempts pickup; " + observed);
            } finally { for (var chunk : held) f.level.setChunkForced(chunk.x, chunk.z, false); }
        }
        test.succeed();
    }

    private record Outcome(boolean harvested, boolean planted, int playerSeeds, int looseSeeds, int events, int ticks, String workState) {}

    private static Outcome harvest(GameTestHelper test, TubeVariantsRuntimeChecks.Fixture f, BlockPos home, boolean pickup) {
        BlockPos crop = home.east();
        test.assertTrue(f.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING), "native golem crop work is enabled");
        f.put(home.below(), Blocks.STONE);
        f.put(crop.below(), Blocks.SOUL_SAND);
        f.put(crop, Blocks.MELON);
        f.player.setPos(Vec3.atBottomCenterOf(crop));
        EntityGolemBase golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), f.level);
        golem.setGolemMaterial(EnumGolemType.IRON.ordinal());
        List<ItemStack> filters = new ArrayList<>(Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY));
        List<Integer> counts = new ArrayList<>(Collections.nCopies(GolemData.FILTER_SLOTS, 0));
        golem.applyConfiguration(new GolemData(GolemData.HARVEST, filters, counts, false, List.of(),
                GolemUseOptions.DEFAULT, List.of(GolemUpgrade.ORDER.id)));
        golem.setNoGravity(true); golem.setPersistenceRequired();
        golem.setPos(Vec3.atBottomCenterOf(home)); golem.setHome(home, Direction.UP);
        ItemEntity seed = new ItemEntity(f.level, crop.getX() + .5, crop.getY() + .1, crop.getZ() + .5,
                new ItemStack(Items.NETHER_WART), 0, 0, 0);
        seed.setNoPickUpDelay(); seed.setNoGravity(true);
        test.assertTrue(f.level.addFreshEntity(golem) && f.level.addFreshEntity(seed), "real configured Order worker and physical seed are admitted");
        test.assertTrue(Math.floorMod(home.getX(), 16) == 15 && Math.floorMod(crop.getX(), 16) == 0
                        && golem.loadedNavigationArea(crop),
                "native navigation ring covers both sides of the home/crop chunk boundary: " + home + " -> " + crop);
        test.assertTrue(golem.canWork() && golem.getUpgradeAmount(GolemUpgrade.ORDER) == 1,
                "actual configured Harvest worker can work with Order: " + golem.describeWorkState());
        int[] events = {0};
        GameEventListener listener = new GameEventListener() {
            @Override public PositionSource getListenerSource() { return new BlockPositionSource(crop); }
            @Override public int getListenerRadius() { return 4; }
            @Override public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 position) {
                if (event != GameEvent.BLOCK_PLACE || context.sourceEntity() != golem || !BlockPos.containing(position).equals(crop)) return false;
                events[0]++;
                if (pickup) seed.playerTouch(f.player);
                return true;
            }
        };
        var registry = f.level.getChunkAt(crop).getListenerRegistry(SectionPos.blockToSectionCoord(crop.getY()));
        registry.register(listener);
        try {
            int ticks = 0;
            for (; ticks < 650 && f.level.getBlockState(crop).is(Blocks.MELON); ticks++) {
                golem.setPos(Vec3.atBottomCenterOf(home));
                golem.setDeltaMovement(Vec3.ZERO); golem.setOnGround(true);
                f.level.tickNonPassenger(golem);
            }
            boolean harvested = !f.level.getBlockState(crop).is(Blocks.MELON);
            int loose = f.drops().stream().filter(drop -> drop.getItem().is(Items.NETHER_WART))
                    .mapToInt(drop -> drop.getItem().getCount()).sum();
            return new Outcome(harvested, f.level.getBlockState(crop).is(Blocks.NETHER_WART),
                    f.player.getInventory().countItem(Items.NETHER_WART), loose, events[0], ticks,
                    "area=" + golem.loadedNavigationArea(crop) + " " + golem.describeWorkState());
        } finally {
            registry.unregister(listener);
            golem.discard(); seed.discard(); f.drops();
        }
    }
}
