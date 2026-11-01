package thaumcraft.common.testing;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.config.ThaumcraftTravelGear;
import thaumcraft.common.items.ItemJar;
import thaumcraft.common.items.armor.HarnessData;
import thaumcraft.common.items.armor.Hover;
import thaumcraft.common.items.equipment.ElementalShovelGeometry;
import thaumcraft.common.lib.world.EarthmoverMaterial;
import thaumcraft.common.mixin.ExperienceOrbCountAccessor;

/** Native conservation, ordinary player harvest events and flight grant custody for S01-S03. */
public final class SecondReviewEquipmentRuntimeChecks {
    private SecondReviewEquipmentRuntimeChecks() {}

    public static void earthmoverPaysEveryMaterialUnit(GameTestHelper test) {
        try (var a = new FocusUpgradeRuntimeChecks.Actor(test)) {
            a.clearLane(); a.player.getInventory().clearContent();
            BlockPos source = a.root;
            var doubleSlab = Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE);
            a.put(source,doubleSlab);
            var shovel = new ItemStack(ThaumcraftEquipment.ELEMENTAL_SHOVEL.get());
            a.player.setItemInHand(InteractionHand.MAIN_HAND,shovel);
            a.player.getInventory().setItem(1,new ItemStack(Items.STONE_SLAB,1));
            a.player.getInventory().setItem(2,new ItemStack(Items.STONE_SLAB,17));
            List<BlockPos> output = ElementalShovelGeometry.layout(source,Direction.UP,0,0);
            for(BlockPos pos:output)a.put(pos,Blocks.AIR.defaultBlockState());
            use(a,source);
            a.require(output.stream().allMatch(pos->a.level.getBlockState(pos)==doubleSlab)
                    &&a.player.getInventory().countItem(Items.STONE_SLAB)==0,"18 slabs, including a split first payment, buy exactly nine double slabs");
            a.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            for(BlockPos pos:output)a.require(a.player.gameMode.destroyBlock(pos),"native paid double slab is harvested");
            int returned = a.level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(source).inflate(4)).stream()
                    .filter(item->item.getItem().is(Items.STONE_SLAB)).mapToInt(item->item.getItem().getCount()).sum();
            a.require(returned==18,"native harvest returns exactly the 18 paid slabs, with no multiplication");

            for(BlockState state:List.of(Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS,8),
                    Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES,4),
                    Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES,4),
                    Blocks.TURTLE_EGG.defaultBlockState().setValue(TurtleEggBlock.EGGS,4))) {
                a.clearLane(); a.player.getInventory().clearContent();
                a.put(source.below(),Blocks.GRASS_BLOCK.defaultBlockState());a.put(source,state);
                output=ElementalShovelGeometry.layout(source,Direction.UP,0,0);
                for(BlockPos pos:output)a.put(pos,Blocks.STONE.defaultBlockState());
                BlockPos target=output.getFirst();a.put(target.below(),Blocks.GRASS_BLOCK.defaultBlockState());a.put(target,Blocks.AIR.defaultBlockState());
                int units=EarthmoverMaterial.units(state);
                shovel=new ItemStack(ThaumcraftEquipment.ELEMENTAL_SHOVEL.get());a.player.setItemInHand(InteractionHand.MAIN_HAND,shovel);
                a.player.getInventory().setItem(1,new ItemStack(state.getBlock().asItem(),units-1));
                use(a,source);
                a.require(a.level.getBlockState(target).isAir()&&a.player.getInventory().countItem(state.getBlock().asItem())==units-1
                        &&shovel.getDamageValue()==0,"insufficient clustered/layered stock is neither partially paid nor copied: "+state);
                a.player.getInventory().setItem(1,new ItemStack(state.getBlock().asItem(),units));
                use(a,source);
                a.require(a.level.getBlockState(target)==EarthmoverMaterial.placementState(state)&&a.player.getInventory().countItem(state.getBlock().asItem())==0
                        &&shovel.getDamageValue()==1,"exact clustered/layered material quantity copies one complete state: "+state);
            }
            a.clearLane();a.player.getInventory().clearContent();
            var material=ThaumcraftEldritchBlocks.PAVING_STONE_TRAVEL_ITEM.get();
            var wrongMaterial=ThaumcraftEldritchBlocks.PAVING_STONE_WARDING_ITEM.get();
            BlockState paving=((thaumcraft.common.blocks.ItemVariantBlock)material).variantState();a.put(source,paving);
            output=ElementalShovelGeometry.layout(source,Direction.UP,0,0);
            for(BlockPos pos:output)a.put(pos,Blocks.STONE.defaultBlockState());
            BlockPos target=output.getFirst();a.put(target,Blocks.AIR.defaultBlockState());
            a.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ThaumcraftEquipment.ELEMENTAL_SHOVEL.get()));
            a.player.getInventory().setItem(1,new ItemStack(wrongMaterial));use(a,source);
            a.require(a.level.getBlockState(target).isAir()&&a.player.getInventory().countItem(wrongMaterial)==1,
                    "a different original metadata item cannot buy the remembered paving stone");
            a.player.getInventory().setItem(1,new ItemStack(material));use(a,source);
            a.require(a.level.getBlockState(target)==paving&&a.player.getInventory().countItem(material)==0,
                    "the exact registered variant item pays for the matching copied state");

            a.clearLane();a.player.getInventory().clearContent();
            var wetSlab=Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP)
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true);
            a.put(source,wetSlab);
            output=ElementalShovelGeometry.layout(source,Direction.UP,0,0);
            for(BlockPos pos:output)a.put(pos,Blocks.STONE.defaultBlockState());
            target=output.getFirst();a.put(target,Blocks.AIR.defaultBlockState());
            a.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ThaumcraftEquipment.ELEMENTAL_SHOVEL.get()));
            a.player.getInventory().setItem(1,new ItemStack(Items.STONE_SLAB));use(a,source);
            a.require(a.level.getBlockState(target)==EarthmoverMaterial.placementState(wetSlab)
                            &&a.level.getFluidState(target).isEmpty()&&a.player.getInventory().countItem(Items.STONE_SLAB)==0,
                    "one paid top slab preserves its shape without copying a second unpaid water source");
        }
        test.succeed();
    }

    private static void use(FocusUpgradeRuntimeChecks.Actor a, BlockPos source) {
        a.player.gameMode.useItemOn(a.player,a.level,a.player.getMainHandItem(),InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(source).add(0,.5,0),Direction.UP,source,false));
    }

    public static void crusherKeepsToolAndProtection(GameTestHelper test) {
        try(var a = new FocusUpgradeRuntimeChecks.Actor(test)) {
            a.clearLane();a.player.getInventory().clearContent();
            BlockPos center=a.root.above();BlockPos veto=center.east(),outside=center.east(2);
            var events=new ArrayList<BlockPos>();
            var denied=new java.util.HashSet<BlockPos>();denied.add(veto);
            BlockEvent.Break listener=(world,pos,state,actor,xp)->{
                if(world!=a.level||actor!=a.player)return EventResult.pass();
                events.add(pos.immutable());return denied.contains(pos)?EventResult.interruptFalse():EventResult.pass();
            };
            BlockEvent.BREAK.register(listener);
            try {
                for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)a.put(center.offset(x,y,0),Blocks.STONE.defaultBlockState());
                a.put(outside,Blocks.STONE.defaultBlockState());
                var tool=new ItemStack(ThaumcraftLegacyItems.PRIMAL_CRUSHER.get());
                tool.enchant(a.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
                a.player.setItemInHand(InteractionHand.MAIN_HAND,tool);aim(a,center);
                a.require(a.player.gameMode.destroyBlock(center),"native center break succeeds");
                var drops=a.level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(4));
                int stone=drops.stream().filter(item->item.getItem().is(Items.STONE)).mapToInt(item->item.getItem().getCount()).sum();
                int cobble=drops.stream().filter(item->item.getItem().is(Items.COBBLESTONE)).mapToInt(item->item.getItem().getCount()).sum();
                a.require(stone==8&&cobble==0&&tool.getDamageValue()==8,"eight accepted Silk Touch breaks yield eight stone and eight native wear; denied cell is free");
                a.require(a.level.getBlockState(veto).is(Blocks.STONE)&&a.level.getBlockState(outside).is(Blocks.STONE)
                        &&events.size()==9&&!events.contains(outside),"all eight neighbours receive native protection events, with no recursive AOE");
                denied.clear();events.clear();a.clearLane();
                for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)a.put(center.offset(x,y,0),Blocks.REDSTONE_ORE.defaultBlockState());
                tool=new ItemStack(ThaumcraftLegacyItems.PRIMAL_CRUSHER.get());
                tool.enchant(a.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),3);
                a.player.setItemInHand(InteractionHand.MAIN_HAND,tool);aim(a,center);
                a.require(a.player.gameMode.destroyBlock(center),"native ordinary ore square is mined");
                int xp=a.level.getEntitiesOfClass(ExperienceOrb.class,new net.minecraft.world.phys.AABB(center).inflate(4)).stream()
                        .mapToInt(orb->orb.getValue()*((ExperienceOrbCountAccessor)(Object)orb).thaumcraft$getOrbCount()).sum();
                int redstone=a.level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(4)).stream()
                        .filter(item->item.getItem().is(Items.REDSTONE)).mapToInt(item->item.getItem().getCount()).sum();
                a.require(xp>=9&&events.size()==9&&tool.getDamageValue()==9&&redstone>=36&&redstone<=72,
                        "all nine Fortune3 ores use native drop/XP/tool wear paths; xp="+xp+", redstone="+redstone);
            } finally {BlockEvent.BREAK.unregister(listener);}
        }
        test.succeed();
    }

    private static void aim(FocusUpgradeRuntimeChecks.Actor a,BlockPos center) {
        a.player.setPos(center.getX()+.5,center.getY()+.5-a.player.getEyeHeight(),center.getZ()-3);
        a.player.setYRot(0);a.player.setXRot(0);a.player.setShiftKeyDown(false);
    }

    public static void hoverGrantUsesPlayerIdentity(GameTestHelper test) {
        try(var a = new FocusUpgradeRuntimeChecks.Actor(test)) {
            a.clearLane();a.player.setGameMode(GameType.SURVIVAL);
            for(boolean intrinsic:new boolean[]{false,true}) {
                Hover.forget(a.player);
                a.player.getAbilities().mayfly=intrinsic;a.player.getAbilities().flying=false;
                var armor=new ItemStack(ThaumcraftTravelGear.HOVER_HARNESS.get());
                HarnessData.setJar(armor,ItemJar.withContents(new ItemStack(ThaumcraftContent.JAR.get()),Aspect.ENERGY,2));
                a.player.setItemSlot(EquipmentSlot.CHEST,armor);
                a.require(Hover.toggleHover(a.player),"actual funded harness is enabled");Hover.handleHoverArmor(a.player);
                a.require(a.player.getAbilities().mayfly&&a.player.getAbilities().flying,"actual paid controller grants temporary flight");
                var foreign=new ServerPlayer(a.player.server,a.level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"same-id-shadow"),ClientInformation.createDefault());
                foreign.setId(a.player.getId());
                a.require(foreign.equals(a.player)&&foreign!=a.player,"fixture reproduces vanilla id equality across distinct players");
                Hover.forget(foreign);
                CompoundTag saved=a.player.saveWithoutId(new CompoundTag());
                a.require(saved.getCompound("abilities").getBoolean("mayfly")==intrinsic&&!saved.getCompound("abilities").getBoolean("flying"),
                        "foreign same-id cleanup cannot consume the grant required to preserve intrinsic saved abilities");
                Hover.forget(a.player);
                a.require(a.player.getAbilities().mayfly==intrinsic&&!a.player.getAbilities().flying,
                        "the actual owner cleanup restores its own original permissions exactly once");
                a.player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);foreign.discard();
            }
        } finally { /* Actor.close invokes the actual server logout, including Hover.forget. */ }
        test.succeed();
    }
}
