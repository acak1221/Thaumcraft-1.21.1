package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockInfusionPillar;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumcraft.common.tiles.TileJar;
import thaumcraft.common.tiles.TilePedestal;

/** Declared formed altar, stabilizers, raw components, levels and finite essence are fixtures. A real augmentation must be produced by natural altar ticks; a declared fc25 boundary session must still pay essence. */
public final class RunicAugmentationRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private RunicAugmentationRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        Check check = new Check(test);
        try { check.prepare(); check.observe(); } catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        test.runAfterDelay(5, () -> {
            if (check.started) return;
            check.started = true;
            try { check.startAugmentation(); }
            catch (RuntimeException | Error failure) { check.finish(); throw failure; }
        });
    }
    private static final class Check {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos centerPos, matrixPos;
        final Map<BlockPos, BlockState> oldStates = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final ServerPlayer player;
        TileInfusionMatrix matrix;
        TilePedestal center, first, second;
        TileJar magic, armor, energy;
        final List<TilePedestal> peripherals = new ArrayList<>();
        long boundaryStarted;
        int phase;
        boolean started, naturalStarted, finished;
        Check(GameTestHelper test) {
            this.test = test; level = test.getLevel(); centerPos = test.absolutePos(new BlockPos(3, 80, 3)); matrixPos = centerPos.above(2);
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "runic-" + UUID.randomUUID().toString().substring(0, 8)), ClientInformation.createDefault());
        }
        void set(BlockPos pos, BlockState state) { oldStates.putIfAbsent(pos, level.getBlockState(pos)); level.setBlock(pos, state, QUIET); }
        void prepare() {
            forced.addAll(GolemChunks.hold(level, List.of(centerPos.offset(-13, 0, -13), centerPos.offset(13, 3, 13))));
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                set(centerPos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                set(centerPos.offset(x, 0, z), Math.abs(x) <= 1 && Math.abs(z) <= 1 ? Blocks.AIR.defaultBlockState() : Blocks.SKELETON_SKULL.defaultBlockState());
            }
            set(centerPos, ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
            set(matrixPos, ThaumcraftInfusion.MATRIX.get().defaultBlockState());
            for (int x : new int[]{-1,1}) for (int z : new int[]{-1,1}) {
                set(centerPos.offset(x, 0, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.LOWER));
                set(centerPos.offset(x, 1, z), ThaumcraftInfusion.PILLAR.get().defaultBlockState().setValue(BlockInfusionPillar.HALF, DoubleBlockHalf.UPPER));
            }
            var componentPositions = new ArrayList<BlockPos>();
            componentPositions.add(centerPos.west(3)); componentPositions.add(centerPos.east(3));
            // Original symmetry counts BOTH physical pedestals and occupied opposites.
            // Build fourteen physical pairs; only27 slots carry the boundary's27 ingredients.
            // The final empty mirror avoids an accidental one-sided75-point instability fixture.
            outer: for (int x = 2; x <= 7; x++) for (int z = 2; z <= 7; z++) {
                componentPositions.add(centerPos.offset(-x,0,z));
                componentPositions.add(centerPos.offset(x,0,-z));
                if(componentPositions.size()==28) break outer;
            }
            for (int index=0;index<componentPositions.size();index++) {
                BlockPos pos=componentPositions.get(index);set(pos,ThaumcraftInfusion.PEDESTAL.get().defaultBlockState());
                if(index<27)peripherals.add((TilePedestal)level.getBlockEntity(pos));
            }
            center = (TilePedestal)level.getBlockEntity(centerPos); first=peripherals.get(0); second=peripherals.get(1);
            matrix = (TileInfusionMatrix)level.getBlockEntity(matrixPos);
            armor=jar(-8,Aspect.ARMOR); magic=jar(-7,Aspect.MAGIC); energy=jar(-6,Aspect.ENERGY);
        }
        void startAugmentation() {
            var recipe=level.getRecipeManager().getAllRecipesFor(thaumcraft.common.config.ThaumcraftRunic.AUGMENT_TYPE.get()).getFirst().value();
            test.assertTrue(recipe.canApply(recipe.sample(39)) && !recipe.canApply(recipe.sample(40)),
                    "native dynamic admission accepts inst24 and refuses original inst25 nextInt(0) boundary before ritual mutation");
            ItemStack signed=thaumcraft.common.lib.runic.RunicArmor.augment(recipe.sample(127));
            signed=ItemStack.parse(level.registryAccess(),signed.save(level.registryAccess())).orElseThrow();
            test.assertTrue(thaumcraft.common.lib.runic.RunicArmor.hardening(signed)==-128,
                    "native ItemStack component codec preserves original signed-byte127→-128 wrap without broadening unsafe ritual admission");
            level.getServer().getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(player.getGameProfile(),false));
            player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setPos(Vec3.atCenterOf(matrixPos).add(0,0,2)); player.giveExperienceLevels(9);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ThaumcraftContent.WAND.get()));
            useWand(); test.assertTrue(matrix.isActive(),"real wand activates formed runic altar");
            KnowledgeAccess.get(player).addResearch("RUNICAUGMENTATION");
            ItemStack robe=new ItemStack(thaumcraft.common.config.ThaumcraftWearables.ROBE_CHESTPLATE.get());
            robe.setDamageValue(7); robe.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Runic fixture"));
            center.setInfusionStack(robe); first.setInfusionStack(new ItemStack(Items.DIAMOND)); second.setInfusionStack(material("salis_mundus"));
            useWand(); test.assertTrue(matrix.isCrafting() && matrix.getRemainingXPLevels()==0 && player.experienceLevel==9,"actual armor augmentation begins with no XP charge");
            naturalStarted=true;
        }
        void observe() {
            test.onEachTick(()-> {
                if(finished||!naturalStarted) return;
                try {
                    test.assertTrue(player.experienceLevel==9 && matrix.getRemainingXPLevels()==0,"runic augmentation never takes enchanting XP");
                    if(phase==0) {
                        if(matrix.isCrafting()) return;
                        ItemStack output=center.getItem(0);
                        test.assertTrue(thaumcraft.common.lib.runic.RunicArmor.hardening(output)==1 && output.getDamageValue()==7
                                && output.getHoverName().getString().equals("Runic fixture") && first.isEmpty() && second.isEmpty()
                                && armor.getAmount()==16 && magic.getAmount()==16 && energy.getAmount()==0,
                                "ordinary native altar creates hardened robe preserving wear/name, after diamond+salis and exact16/16/32 essence payment");
                        // Huge boundary centre is a DECLARED saved-input fixture, not25 prior acquisition rituals.
                        ItemStack boundary=new ItemStack(thaumcraft.common.config.ThaumcraftWearables.ROBE_CHESTPLATE.get());
                        boundary.set(thaumcraft.common.config.ThaumcraftRunic.HARDENING.get(),25); center.setInfusionStack(boundary);
                        for(int i=0;i<peripherals.size();i++) peripherals.get(i).setInfusionStack(i==0 ? new ItemStack(Items.DIAMOND) : material("salis_mundus"));
                        useWand(); test.assertTrue(matrix.isCrafting() && matrix.getDemand().getAmount(Aspect.ARMOR)==536870912
                                && matrix.getDemand().getAmount(Aspect.MAGIC)==536870912 && matrix.getDemand().getAmount(Aspect.ENERGY)==1073741824,
                                "fc25 ritual is accepted with ORIGINAL individual costs although original int total overflows negative");
                        test.assertTrue(matrix.getInstability()<=0,"declared mirrored pedestals and actual skulls make boundary payment deterministic without suppressing production hazards: "+matrix.getInstability());
                        boundaryStarted=level.getGameTime(); phase=1;
                    } else {
                        if(level.getGameTime()-boundaryStarted<35) return;
                        var demand=matrix.getDemand();
                        test.assertTrue(matrix.isCrafting() && demand.getAmount(Aspect.ARMOR)<536870912 && demand.getAmount(Aspect.ARMOR)>536870896
                                && demand.getAmount(Aspect.MAGIC)==536870912 && demand.getAmount(Aspect.ENERGY)==1073741824
                                && peripherals.stream().noneMatch(TilePedestal::isEmpty) && thaumcraft.common.lib.runic.RunicArmor.hardening(center.getItem(0))==25,
                                "actual native fc25 cycle debits finite armor source and keeps all27 ingredients/centre pending instead of overflowing into free output: crafting="+matrix.isCrafting()
                                        +", demand="+demand.aspects+", armorJar="+armor.getAmount()+", instability="+matrix.getInstability()
                                        +", occupied="+peripherals.stream().filter(pedestal->!pedestal.isEmpty()).count()+", centralHardening="+thaumcraft.common.lib.runic.RunicArmor.hardening(center.getItem(0)));
                        var saved=matrix.saveWithFullMetadata(level.registryAccess()); matrix.loadWithComponents(saved,level.registryAccess());
                        test.assertTrue(matrix.isCrafting() && matrix.getDemand().aspects.equals(demand.aspects)
                                && matrix.getPendingEssentia(Aspect.ENERGY)==1073741824,
                                "native session NBT decode and atomic demand endpoint retain individual huge int costs without aggregate overflow");
                        finish(); test.succeed();
                    }
                } catch(RuntimeException|Error failure) { finish(); throw failure; }
            });
            test.runAfterDelay(1500,()->{if(!finished){finish();test.fail("runic ordinary ritual/fc25 paid-demand boundary did not complete: phase="+phase);}});
        }
        ItemStack material(String path) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", path))); }
        TileJar jar(int x, Aspect aspect) {
            BlockPos pos = centerPos.offset(x, 0, 9); set(pos, ThaumcraftContent.JAR.get().defaultBlockState());
            TileJar jar = (TileJar)level.getBlockEntity(pos); test.assertTrue(jar.addToContainer(aspect, 32) == 0, "declared finite jar for " + aspect.getTag()); return jar;
        }
        void useWand() {
            var hit = new BlockHitResult(Vec3.atCenterOf(matrixPos), Direction.UP, matrixPos, false);
            test.assertTrue(player.getMainHandItem().getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "real wand handles altar action");
        }
        void finish() {
            if (finished) return; finished = true;
            try {
                if (matrix != null && !matrix.isRemoved()) {
                    var tag = matrix.saveWithFullMetadata(level.registryAccess()); tag.putBoolean("crafting", false); tag.putBoolean("active", false); matrix.loadWithComponents(tag, level.registryAccess());
                }
                // These are declared fixtures; remove their contents before pedestal removal can evacuate them.
                var allPedestals = new ArrayList<>(peripherals); if(center != null) allPedestals.add(center);
                for (TilePedestal pedestal : allPedestals) {
                    if (pedestal != null && !pedestal.isRemoved()) pedestal.setInfusionStack(ItemStack.EMPTY);
                }
                oldStates.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                for (ChunkPos pos : forced) level.setChunkForced(pos.x, pos.z, false);
            } finally {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                player.discard(); channel.finishAndReleaseAll();
            }
        }
    }
}
