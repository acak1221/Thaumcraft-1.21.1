package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftVillageProfessions;
import thaumcraft.common.lib.world.VillageBankerHome;
import thaumcraft.common.lib.world.VillageBankerManager;

/** Generated villagers retain ordinary native Brain/AI; no direct ResetProfession invocation or XP workaround. */
public final class TailVillageProfessionReviewRuntimeChecks {
    private TailVillageProfessionReviewRuntimeChecks() {}

    public static void generatedBankerKeepsProfessionWithNormalBrain(GameTestHelper test) {
        try (var home = new Home(test); var clock = new Clock(home)) {
            var banker = home.banker;
            var wizard = home.add(ThaumcraftVillageProfessions.WIZARD.get(), home.spawn.east());
            var vanilla = home.add(VillagerProfession.FARMER, home.spawn.north());
            assertFreshNoJob(test, banker, "generated banker");
            assertFreshNoJob(test, wizard, "configured native wizard");
            assertFreshNoJob(test, vanilla, "vanilla employed control");
            test.assertTrue(banker.getOffers().size() == VillageBankerManager.trades().size(),
                    "actual generated banker starts with its complete native trade list");
            List<String> observed = new ArrayList<>();
            for (int tick = 0; tick < 120; tick++) {
                clock.step(banker, wizard, vanilla);
                if (tick < 3 || tick % 20 == 19) observed.add("tick" + (tick + 1) + ": "
                        + describe(banker) + "; wizard=" + describe(wizard) + "; vanilla=" + describe(vanilla));
            }
            test.assertTrue(vanilla.getVillagerData().getProfession() == VillagerProfession.NONE,
                    "ordinary native Brain still resets an inexperienced vanilla farmer without a job site: " + observed);
            test.assertTrue(banker.getVillagerData().getProfession() == ThaumcraftVillageProfessions.BANKER.get()
                            && wizard.getVillagerData().getProfession() == ThaumcraftVillageProfessions.WIZARD.get(),
                    "generated banker and configured wizard retain their original TC4 professions under normal native Brain ticks: "
                            + observed);
            test.assertTrue(!banker.isNoAi() && !wizard.isNoAi() && banker.getVillagerXp() == 0 && wizard.getVillagerXp() == 0
                            && banker.getVillagerData().getLevel() == 1 && wizard.getVillagerData().getLevel() == 1
                            && banker.getOffers().size() == VillageBankerManager.trades().size(),
                    "retention preserves native AI, original zero XP/novice state and the generated banker's offers");
        }
        test.succeed();
    }

    /** Original 1.7.10 agn/yv: finite seven uses, last-offer trigger and forty non-trading AI updates. */
    public static void bankerLastOfferRestocksAfterFortyIdleTicks(GameTestHelper test) {
        try (var home = new Home(test); var clock = new Clock(home)) {
            Villager banker = home.banker;
            assertFreshNoJob(test, banker, "generated restock banker");
            var offers = banker.getOffers();
            test.assertTrue(offers.size() > 1, "banker has the real multi-offer trade table");
            MerchantOffer depleted = offers.getFirst(), trigger = offers.getLast();
            test.assertTrue(depleted.getMaxUses() == 7 && depleted.getUses() == 0
                            && trigger.getMaxUses() == 7 && trigger.getUses() == 0,
                    "ordinary original trades begin with exactly seven uses, never an infinite-stock substitute");
            MerchantMenu menu = home.openMenu();
            try {
                for (int count = 0; count < 7; count++) home.trade(menu, 0, depleted);
                test.assertTrue(depleted.getUses() == 7 && depleted.isOutOfStock(),
                        "seven actual native menu payments exhaust the first offer");
                home.trade(menu, offers.size() - 1, trigger);
                test.assertTrue(trigger.getUses() == 1 && !trigger.isOutOfStock(),
                        "actual last-offer payment triggers the canonical idle reset");
                for (int tick = 0; tick < 8; tick++) clock.step(banker);
                test.assertTrue(banker.getVillagerData().getProfession() == ThaumcraftVillageProfessions.BANKER.get(),
                        "restock precondition: generated banker's normal Brain retains its profession while trading; "
                                + describe(banker));
                test.assertTrue(banker.getTradingPlayer() == home.fixture.player && depleted.isOutOfStock()
                                && depleted.getMaxUses() == 7,
                        "the original forty-update restock clock waits while the real merchant menu is open");
            } finally {
                home.closeMenu(menu);
            }
            for (int tick = 0; tick < 39; tick++) clock.step(banker);
            test.assertTrue(banker.getVillagerData().getProfession() == ThaumcraftVillageProfessions.BANKER.get(),
                    "restock precondition: normal native Brain retains banker's profession without a POI: " + describe(banker));
            test.assertTrue(depleted.isOutOfStock() && depleted.getUses() == 7 && depleted.getMaxUses() == 7,
                    "original disabled offer remains exhausted for the first39 idle updates: uses="
                            + depleted.getUses() + "/" + depleted.getMaxUses());
            clock.step(banker);
            var regeneration = banker.getEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
            test.assertTrue(depleted.getUses() == 7 && depleted.getMaxUses() >= 9 && depleted.getMaxUses() <= 19
                            && !depleted.isOutOfStock() && trigger.getUses() == 1 && trigger.getMaxUses() == 7,
                    "fortieth idle update replenishes only disabled offers by original2..12 extra maximum uses; "
                            + "depleted=" + depleted.getUses() + "/" + depleted.getMaxUses()
                            + ", enabledLast=" + trigger.getUses() + "/" + trigger.getMaxUses()
                            + ", " + describe(banker));
            test.assertTrue(regeneration != null && regeneration.getAmplifier() == 0
                            && regeneration.getDuration() >= 199 && regeneration.getDuration() <= 200,
                    "original completed merchant reset grants Regeneration I for200ticks");
            test.assertTrue(!banker.getBrain().hasMemoryValue(MemoryModuleType.JOB_SITE)
                            && banker.getVillagerXp() == 0 && banker.getVillagerData().getLevel() == 1 && !banker.isNoAi(),
                    "canonical restock uses neither a job-site surrogate nor XP/level/NoAI hacks");
            MerchantMenu reopened = home.openMenu();
            try {
                var active = banker.getOffers().getFirst();
                test.assertTrue(!active.isOutOfStock(), "reopened actual merchant exposes a usable replenished offer");
                home.trade(reopened, 0, active);
            } finally {
                home.closeMenu(reopened);
            }
        }
        test.succeed();
    }

    private static void assertFreshNoJob(GameTestHelper test, Villager villager, String label) {
        test.assertTrue(!villager.isNoAi() && villager.getVillagerXp() == 0 && villager.getVillagerData().getLevel() == 1
                        && !villager.getBrain().hasMemoryValue(MemoryModuleType.JOB_SITE),
                label + " is a real fresh novice with normal native Brain/AI and no job site: " + describe(villager));
    }

    private static String describe(Villager villager) {
        return villager.getVillagerData().getProfession() + "/level" + villager.getVillagerData().getLevel()
                + "/xp" + villager.getVillagerXp() + "/NoAI=" + villager.isNoAi()
                + "/job=" + villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
    }

    private static final class Home implements AutoCloseable {
        final GameTestHelper test;
        final TubeVariantsRuntimeChecks.Fixture fixture;
        final List<ChunkPos> held = new ArrayList<>();
        final List<Villager> owned = new ArrayList<>();
        final Map<UUID, BlockPos> anchors = new HashMap<>();
        final BlockPos origin, spawn;
        final Villager banker;

        Home(GameTestHelper test) {
            this.test = test;
            fixture = new TubeVariantsRuntimeChecks.Fixture(test);
            origin = fixture.base.offset(-1, -1, -1);
            spawn = origin.offset(1, 1, 2);
            try {
                fixture.open();
                held.addAll(GolemChunks.hold(fixture.level, List.of(origin, origin.offset(4, 6, 5))));
                GameTestChunkAwait.entityTicking(test, spawn);
                // The real public generator owns its entire footprint, roof clearance and footing here.
                for (int x = 0; x < VillageBankerHome.WIDTH; x++) {
                    for (int z = 0; z < VillageBankerHome.DEPTH; z++) {
                        int top = Math.min(fixture.level.getMaxBuildHeight() - 1, Math.max(origin.getY() + 8,
                                fixture.level.getHeight(Heightmap.Types.WORLD_SURFACE, origin.getX() + x, origin.getZ() + z)));
                        for (int y = origin.getY() - 1; y <= top; y++) {
                            BlockPos pos = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
                            remember(pos);
                            fixture.put(pos, y == origin.getY() - 1 ? Blocks.STONE : Blocks.AIR);
                        }
                    }
                }
                AABB area = new AABB(Vec3.atLowerCornerOf(origin), Vec3.atLowerCornerOf(origin.offset(
                        VillageBankerHome.WIDTH, VillageBankerHome.HEIGHT + 1, VillageBankerHome.DEPTH)));
                Set<UUID> before = new HashSet<>();
                fixture.level.getEntitiesOfClass(Villager.class, area).forEach(v -> before.add(v.getUUID()));
                test.assertTrue(VillageBankerHome.build(fixture.level, RandomSource.create(1710), origin, 0, false, 0),
                        "actual public banker-home generator builds its native house");
                var added = fixture.level.getEntitiesOfClass(Villager.class, area).stream()
                        .filter(v -> !before.contains(v.getUUID())).toList();
                owned.addAll(added);
                test.assertTrue(added.size() == 1, "actual banker-home generator publishes exactly one villager: " + added.size());
                banker = added.getFirst();
                anchors.put(banker.getUUID(), spawn);
                test.assertTrue(banker.getVillagerData().getProfession() == ThaumcraftVillageProfessions.BANKER.get(),
                        "normal generated NPC initially has the original banker profession");
                banker.setNoGravity(true); // Stationary physical fixture; Brain and AI remain enabled.
            } catch (RuntimeException | Error failure) {
                close();
                throw failure;
            }
        }

        private void remember(BlockPos pos) {
            fixture.states.putIfAbsent(pos, fixture.level.getBlockState(pos));
            BlockEntity entity = fixture.level.getBlockEntity(pos);
            if (entity != null) fixture.originals.putIfAbsent(pos, entity);
        }

        Villager add(VillagerProfession profession, BlockPos pos) {
            Villager villager = EntityType.VILLAGER.create(fixture.level);
            test.assertTrue(villager != null, "native profession control exists");
            villager.setVillagerData(villager.getVillagerData().setProfession(profession));
            villager.setPersistenceRequired();
            villager.setNoGravity(true);
            villager.setPos(Vec3.atBottomCenterOf(pos));
            test.assertTrue(fixture.level.addFreshEntity(villager), "native profession control is admitted");
            owned.add(villager);
            anchors.put(villager.getUUID(), pos);
            return villager;
        }

        MerchantMenu openMenu() {
            banker.setTradingPlayer(fixture.player);
            MerchantMenu menu = new MerchantMenu(793, fixture.player.getInventory(), banker);
            fixture.player.containerMenu = menu;
            return menu;
        }

        void trade(MerchantMenu menu, int index, MerchantOffer offer) {
            int previousUses = offer.getUses();
            ItemStack reward = offer.getResult().copy();
            menu.setCarried(ItemStack.EMPTY);
            menu.setSelectionHint(index);
            menu.getSlot(0).set(offer.getCostA().copy());
            menu.getSlot(1).set(ItemStack.EMPTY);
            test.assertTrue(ItemStack.matches(menu.getSlot(2).getItem(), reward),
                    "actual merchant menu displays its selected registered reward before payment");
            menu.clicked(2, 0, ClickType.PICKUP, fixture.player);
            test.assertTrue(ItemStack.matches(menu.getCarried(), reward) && menu.getSlot(0).getItem().isEmpty()
                            && offer.getUses() == previousUses + 1,
                    "actual native result slot delivers one paid reward and notifies the selected offer");
            menu.setCarried(ItemStack.EMPTY); // Retire only this fixture's witnessed reward.
        }

        void closeMenu(MerchantMenu menu) {
            menu.setCarried(ItemStack.EMPTY);
            menu.getSlot(0).set(ItemStack.EMPTY);
            menu.getSlot(1).set(ItemStack.EMPTY);
            menu.removed(fixture.player);
            fixture.player.containerMenu = fixture.player.inventoryMenu;
            banker.setTradingPlayer(null);
        }

        @Override public void close() {
            owned.forEach(Villager::discard);
            owned.clear();
            for (ChunkPos chunk : held) fixture.level.setChunkForced(chunk.x, chunk.z, false);
            held.clear();
            fixture.close();
        }
    }

    /** Only owned villagers are ticked while explicit native world time advances; restore both clocks on exit. */
    private static final class Clock implements AutoCloseable {
        final Home home;
        final ServerLevelData data;
        final long originalGameTime, originalDayTime;
        int elapsed;
        Clock(Home home) {
            this.home = home;
            data = (ServerLevelData) home.fixture.level.getLevelData();
            originalGameTime = home.fixture.level.getGameTime();
            originalDayTime = home.fixture.level.getDayTime();
        }
        void step(Villager... villagers) {
            data.setGameTime(originalGameTime + ++elapsed);
            home.fixture.level.setDayTime(6000L + elapsed);
            for (Villager villager : villagers) {
                BlockPos anchor = home.anchors.get(villager.getUUID());
                villager.setPos(Vec3.atBottomCenterOf(anchor));
                villager.setDeltaMovement(Vec3.ZERO);
                villager.setOnGround(true);
                home.fixture.level.tickNonPassenger(villager);
            }
        }
        @Override public void close() {
            data.setGameTime(originalGameTime);
            home.fixture.level.setDayTime(originalDayTime);
            home.fixture.level.updateSkyBrightness();
        }
    }
}
