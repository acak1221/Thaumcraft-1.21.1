package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftDistillation;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.items.ItemCrystalEssence;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileAlchemyFurnace;
import thaumcraft.common.tiles.TileCentrifuge;
import thaumcraft.common.tiles.TileEssentiaCrystalizer;

/** World scheduler acceptance: no manual tick calls, clock/NBT/RNG writes or output grants.
 * Empty placed devices and raw item/essentia inputs are explicit inputs, not survival acquisition. */
public final class DeviceWorldRuntimeChecks {
    private DeviceWorldRuntimeChecks() {}

    public static void poweredBore(GameTestHelper test) {
        var network=NodeTransductionRuntimeChecks.fixture(test,false);
        cleanup(test,network::close); network.open();
        NodeTransductionRuntimeChecks.energize(network,network.base.west(7),NodeTransductionRuntimeChecks.primals(2500),null,thaumcraft.api.nodes.NodeType.NORMAL);
        var bore=new BoreWorkRuntimeChecks.Fixture(test);
        bore.tools(); bore.put(bore.target,net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState());
        bore.power(true);
        boolean[] paused={false};
        test.succeedWhen(()->{
            test.assertTrue(bore.top.getItem(1).getDamageValue()==1,"actual world bore completed one pickaxe use");
            if(!paused[0]) { bore.power(false); paused[0]=true; }
            test.assertTrue(test.getLevel().getBlockState(bore.target).isAir() && bore.top.getAccelerationCredit()>0,
                    "real mined target is gone and actual energized-node vis supplied acceleration credit");
            var drops=test.getLevel().getEntitiesOfClass(ItemEntity.class,bore.area);
            test.assertTrue(drops.size()==1 && drops.getFirst().getItem().is(Items.DIAMOND) && drops.getFirst().getItem().getCount()==1,
                    "world powered bore brings back exactly diamond1 from ore1, not merely spinning");
            thaumcraft.common.Thaumcraft.LOG.info("G26 measured bore: ore1 -> diamond1, pick wear1, residual acceleration credit={}",bore.top.getAccelerationCredit());
            bore.close(); network.close();
        });
    }

    public static void hungryChest(GameTestHelper test) {
        BlockPos pos = test.absolutePos(new BlockPos(2, 3, 2));
        isolate(test,pos);
        test.getLevel().setBlockAndUpdate(pos, thaumcraft.common.config.ThaumcraftHungryChest.HUNGRY_CHEST.get().defaultBlockState());
        var chest = (thaumcraft.common.tiles.TileChestHungry) test.getLevel().getBlockEntity(pos);
        var input = new ItemEntity(test.getLevel(),pos.getX()+.5,pos.getY()+1.2,pos.getZ()+.5,new ItemStack(Items.IRON_INGOT,7));
        // ItemEntity's default random sideways launch can miss the chest entirely.
        // Declare a vertical drop; ordinary gravity/contact still performs collection.
        input.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        input.setPickUpDelay(1000);
        test.assertTrue(test.getLevel().addFreshEntity(input), "physical dropped iron7 input, not direct chest insertion");
        test.runAfterDelay(40, () -> {
            int count=0; for(int slot=0;slot<chest.getContainerSize();slot++) if(chest.getItem(slot).is(Items.IRON_INGOT)) count+=chest.getItem(slot).getCount();
            test.assertTrue(input.isRemoved() && count==7, "world contact delivers exactly iron7 despite pickup delay: stored="+count+", inputRemoved="+input.isRemoved()+", y="+input.getY());
            test.succeed();
        });
    }

    static void cleanup(GameTestHelper test, Runnable action) {
        BoreWorkRuntimeChecks.testInfo(test).addListener(new net.minecraft.gametest.framework.GameTestListener() {
            @Override public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo info) {}
            @Override public void testPassed(net.minecraft.gametest.framework.GameTestInfo info, net.minecraft.gametest.framework.GameTestRunner runner) { action.run(); }
            @Override public void testFailed(net.minecraft.gametest.framework.GameTestInfo info, net.minecraft.gametest.framework.GameTestRunner runner) { action.run(); }
            @Override public void testAddedForRerun(net.minecraft.gametest.framework.GameTestInfo previous, net.minecraft.gametest.framework.GameTestInfo next, net.minecraft.gametest.framework.GameTestRunner runner) { action.run(); }
        });
    }

    /** These wrappers have separate batches; preserve pre-existing cells and drops. */
    private static void isolate(GameTestHelper test, BlockPos... cells) {
        var level=test.getLevel();
        var saved=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(var pos:cells) {
            test.assertTrue(level.getBlockEntity(pos)==null,"audit refuses to overwrite an existing device at "+pos);
            saved.put(pos,level.getBlockState(pos));
        }
        var bounds=new AABB(cells[0]).inflate(5);
        var existing=level.getEntitiesOfClass(ItemEntity.class,bounds).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
        boolean[] closed={false};
        cleanup(test,()->{
            if(closed[0]) return; closed[0]=true;
            // Remove the declared audit input/output owners without generating break loot.
            saved.forEach((pos,state)->{ level.removeBlockEntity(pos); level.setBlockAndUpdate(pos,state); });
            level.getEntitiesOfClass(ItemEntity.class,bounds).stream().filter(drop->!existing.contains(drop.getUUID())).forEach(ItemEntity::discard);
        });
    }

    public static void deconstruction(GameTestHelper test) {
        BlockPos pos = test.absolutePos(new BlockPos(2, 3, 2));
        isolate(test,pos);
        test.getLevel().setBlockAndUpdate(pos, thaumcraft.common.config.ThaumcraftDeconstruction.TABLE.get().defaultBlockState());
        var table = (thaumcraft.common.tiles.TileDeconstructionTable) test.getLevel().getBlockEntity(pos);
        var input = new ItemStack(ThaumcraftContent.PRIMORDIAL_PEARL.get());
        var primals = thaumcraft.common.lib.research.DeconstructionRules.primals(
                thaumcraft.common.lib.research.ObjectAspectResolver.get(input, test.getLevel()));
        test.assertTrue(primals.visSize() >= 80, "real pearl guarantees original nextInt(80) success without RNG injection");
        thaumcraft.common.Thaumcraft.LOG.info("G26 measured deconstruction input: pearl1 has reduced primal total={}",primals.visSize());
        table.setItem(0, input);
        test.runAfterDelay(39, () -> test.assertTrue(table.getItem(0).getCount()==1 && table.breakTime()==1 && table.reward()==null,
                "39 world work ticks preserve the actual pearl, one tick remains"));
        test.runAfterDelay(40, () -> test.assertTrue(table.getItem(0).isEmpty() && table.reward()!=null,
                "exact fortieth world work tick consumes pearl1 for pending RP1"));
        test.runAfterDelay(45, () -> {
            test.assertTrue(table.getItem(0).isEmpty() && table.reward() != null && table.reward().isPrimal(),
                    "world deconstructor consumes pearl1 for exactly one pending primal RP");
            var player = test.makeMockServerPlayerInLevel();
            player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + 1.5);
            var menu = new thaumcraft.common.container.ContainerDeconstructionTable(26, player.getInventory(), table);
            player.containerMenu = menu;
            Aspect reward = table.reward();
            var knowledge = thaumcraft.common.lib.capabilities.KnowledgeAccess.get(player);
            int before = knowledge.getAspectPoolFor(reward.getTag());
            test.assertTrue(menu.clickMenuButton(player, 1) && knowledge.getAspectPoolFor(reward.getTag()) == before + 1
                            && !menu.clickMenuButton(player, 1), "real menu claims exactly RP1 once, not full item aspect total");
            player.closeContainer();
            test.succeed();
        });
    }

    public static void furnace(GameTestHelper test) {
        BlockPos pos = test.absolutePos(new BlockPos(2, 3, 2));
        isolate(test,pos,pos.above(),pos.above(2));
        test.getLevel().setBlockAndUpdate(pos, ThaumcraftDistillation.ALCHEMY_FURNACE.get().defaultBlockState());
        test.getLevel().setBlockAndUpdate(pos.above(), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState());
        test.getLevel().setBlockAndUpdate(pos.above(2), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState());
        var furnace = (TileAlchemyFurnace) test.getLevel().getBlockEntity(pos);
        var first = (TileAlembic) test.getLevel().getBlockEntity(pos.above());
        var second = (TileAlembic) test.getLevel().getBlockEntity(pos.above(2));
        furnace.setItem(0, new ItemStack(Items.COAL));
        furnace.setItem(1, new ItemStack(Items.COAL, 2));
        test.runAfterDelay(39, () -> test.assertTrue(furnace.getItem(0).getCount()==1 && furnace.getCookTime()==39
                        && furnace.getItem(1).getCount()==1 && furnace.getVis()==0,
                "39 world cook ticks pay one fuel but preserve input until completion"));
        test.runAfterDelay(40, () -> test.assertTrue(furnace.getItem(0).isEmpty() && furnace.getVis()==4
                        && furnace.getAspects().getAmount(Aspect.FIRE)==2 && furnace.getAspects().getAmount(Aspect.ENERGY)==2,
                "fortieth world cook tick creates exact ignis2+potentia2 from one input"));
        test.runAfterDelay(180, () -> {
            test.assertTrue(furnace.getItem(0).isEmpty() && furnace.getItem(1).getCount() == 1,
                    "world furnace consumes exactly input coal1/fuel coal1");
            test.assertTrue(furnace.getVis() == 0 && first.getAmount() == 2 && second.getAmount() == 2
                            && first.getAspect() != second.getAspect()
                            && (first.getAspect() == Aspect.FIRE || first.getAspect() == Aspect.ENERGY)
                            && (second.getAspect() == Aspect.FIRE || second.getAspect() == Aspect.ENERGY),
                    "world furnace/alembics deliver ignis2+potentia2: slurry="+furnace.getVis()+", alembics="+first.getAmount()+"/"+second.getAmount());
        });
        test.runAfterDelay(220, () -> {
            test.assertTrue(first.getAmount() + second.getAmount() == 4 && furnace.getItem(1).getCount() == 1,
                    "idle world ticks cannot replay the input or ignite another fuel");
            test.succeed();
        });
    }

    public static void centrifuge(GameTestHelper test) {
        BlockPos pos = test.absolutePos(new BlockPos(2, 3, 2));
        isolate(test,pos,pos.below());
        test.getLevel().setBlockAndUpdate(pos.below(), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState());
        test.getLevel().setBlockAndUpdate(pos, ThaumcraftTubes.CENTRIFUGE.get().defaultBlockState());
        var source = (TileAlembic) test.getLevel().getBlockEntity(pos.below());
        var machine = (TileCentrifuge) test.getLevel().getBlockEntity(pos);
        test.assertTrue(source.addToContainer(Aspect.ENERGY, 1) == 0, "declared raw potentia1 accepted");
        test.runAfterDelay(60, () -> {
            Aspect output = machine.getOutputAspect();
            test.assertTrue(source.getAmount() == 0 && machine.getInputAspect() == null
                            && (output == Aspect.FIRE || output == Aspect.ORDER),
                    "world centrifuge pays potentia1 for ONE immediate component: source="+source.getAmount()+", in="+machine.getInputAspect()+", out="+output);
            test.assertTrue(machine.takeFromContainer(output, 1), "collect the one real completed output");
        });
        test.runAfterDelay(120, () -> {
            test.assertTrue(machine.getInputAspect() == null && machine.getOutputAspect() == null && source.getAmount() == 0,
                    "empty source cannot pay a second output");
            test.succeed();
        });
    }

    public static void crystallizer(GameTestHelper test) {
        BlockPos pos = test.absolutePos(new BlockPos(2, 3, 2));
        isolate(test,pos,pos.below());
        test.getLevel().setBlockAndUpdate(pos.below(), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState());
        test.getLevel().setBlockAndUpdate(pos, ThaumcraftTubes.ESSENTIA_CRYSTALLIZER.get().defaultBlockState());
        var source = (TileAlembic) test.getLevel().getBlockEntity(pos.below());
        var machine = (TileEssentiaCrystalizer) test.getLevel().getBlockEntity(pos);
        test.assertTrue(source.addToContainer(Aspect.ENERGY, 1) == 0, "declared raw potentia1 accepted");
        test.runAfterDelay(990, () -> test.assertTrue(source.getAmount() == 0 && machine.getAspect() == Aspect.ENERGY
                        && crystals(test, pos) == 0, "baseline is not complete before 1005 world ticks"));
        test.runAfterDelay(1030, () -> {
            test.assertTrue(source.getAmount() == 0 && machine.getAspect() == null && crystals(test, pos) == 1,
                    "world crystallizer produces crystal1 for essentia1: source="+source.getAmount()+", input="+machine.getAspect()+", progress="+machine.getProgress()+", outputs="+crystals(test,pos));
        });
        test.runAfterDelay(1070, () -> {
            test.assertTrue(crystals(test, pos) == 1, "empty world machine cannot duplicate paid crystal");
            test.succeed();
        });
    }

    public static void tubeAndBuffer(GameTestHelper test) {
        BlockPos pos = test.absolutePos(new BlockPos(2, 4, 2));
        isolate(test,pos,pos.below(),pos.below(2),pos.below(3));
        var level=test.getLevel();
        level.setBlockAndUpdate(pos.below(3),ThaumcraftAlembics.ALEMBIC.get().defaultBlockState());
        level.setBlockAndUpdate(pos.below(2),ThaumcraftTubes.ESSENTIA_BUFFER.get().defaultBlockState());
        level.setBlockAndUpdate(pos.below(),ThaumcraftTubes.ESSENTIA_TUBE.get().defaultBlockState());
        level.setBlockAndUpdate(pos,ThaumcraftTubes.ESSENTIA_CRYSTALLIZER.get().defaultBlockState());
        var source=(TileAlembic)level.getBlockEntity(pos.below(3));
        var buffer=(thaumcraft.common.tiles.TileTubeBuffer)level.getBlockEntity(pos.below(2));
        var tube=(thaumcraft.common.tiles.TileTube)level.getBlockEntity(pos.below());
        var machine=(TileEssentiaCrystalizer)level.getBlockEntity(pos);
        test.assertTrue(source.addToContainer(Aspect.ENERGY,1)==0,"only raw input is potentia1 at bottom alembic");
        test.runAfterDelay(1100,()->{
            test.assertTrue(source.getAmount()==0 && buffer.getAmount()==0 && tube.getEssentiaAmount(null)==0
                            && machine.getAspect()==null && crystals(test,pos)==1,
                    "world alembic->buffer->tube->crystallizer conserves1: source="+source.getAmount()+", buffer="+buffer.getAmount()+", tube="+tube.getEssentiaAmount(null)+", input="+machine.getAspect()+", progress="+machine.getProgress()+", outputs="+crystals(test,pos));
            test.succeed();
        });
    }

    private static int crystals(GameTestHelper test, BlockPos pos) {
        return test.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).stream()
                .filter(drop -> drop.getItem().is(ThaumcraftContent.CRYSTAL_ESSENCE.get())
                        && ItemCrystalEssence.data(drop.getItem()) != null
                        && ItemCrystalEssence.data(drop.getItem()).amount("potentia") == 1)
                .mapToInt(drop -> drop.getItem().getCount()).sum();
    }
}
