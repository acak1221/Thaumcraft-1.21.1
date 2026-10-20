package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftEldritchItems;
import thaumcraft.common.config.ThaumcraftGolemMaterials;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.crafting.CrucibleCrafting;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeSyncPayload;
import thaumcraft.common.lib.network.ResearchNoticePayload;
import thaumcraft.common.lib.network.WarpMessagePayload;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileCrucible;

/** Real native held use/release, packets and crucible collision. Declared warp, fluid and branch RNG inputs;
 * holding drives native ServerLevel.tickNonPassenger plus the embedded game listener, not elapsed client or survival-acquisition evidence.
 * Root registers three wrappers, timeout200 each. No private counter injection or fake BE types. */
public final class EldritchItemsRuntimeChecks {
    private EldritchItemsRuntimeChecks(){}
    public static void reusableRitesCompleteCrimson(GameTestHelper test){
        var original=ThaumcraftSettings.server();
        try(var f=new Fixture(test)){
            f.open();configure(false);f.player.setPos(Vec3.atBottomCenterOf(f.base));
            var knowledge=KnowledgeAccess.get(f.player);knowledge.reset();knowledge.setInitializedAspects(true);
            var book=new ItemStack(ThaumcraftEldritchItems.CRIMSON_RITES.get());book.set(DataComponents.CUSTOM_NAME,Component.literal("same reusable crimson book"));
            var tag=new CompoundTag();tag.putString("owner-fixture","rites");book.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));var snapshot=book.copy();
            f.player.setItemInHand(InteractionHand.MAIN_HAND,book);capture(f);
            f.check(f.player.gameMode.useItem(f.player,f.level,book,InteractionHand.MAIN_HAND).consumesAction(),"real survival RMB studies the physical Rites without any prerequisite/research clue");
            var receipts=capture(f);var data=receipts.data();
            f.check(knowledge.isResearchComplete("CRIMSON")&&knowledge.getResearchComplete().equals(java.util.Set.of("CRIMSON"))
                            &&knowledge.getWarpPerm()==2&&knowledge.getWarpSticky()==1&&knowledge.getWarpTemp()==0&&knowledge.getWarpCounter()==3,
                    "one actual Rites use directly completes only CRIMSON with original warp3 split2/1");
            f.check(data.size()==4&&data.getFirst() instanceof KnowledgeSyncPayload&&data.subList(1,4).equals(List.of(new ResearchNoticePayload("CRIMSON"),new WarpMessagePayload(2,(byte)0),new WarpMessagePayload(1,(byte)1)))
                            &&receipts.learnSounds().size()==1&&receipts.learnSounds().getFirst().getVolume()==.75F&&receipts.learnSounds().getFirst().getPitch()==1F,
                    "actual committed native full snapshot precedes one CRIMSON notice/ordered warp packets and original learn.75/1 sound");
            f.check(f.player.getMainHandItem()==book&&ItemStack.matches(book,snapshot)&&book.getMaxStackSize()==1&&book.getRarity()==Rarity.UNCOMMON,
                    "survival study keeps exact one-stack uncommon book and all unrelated native components");
            f.player.gameMode.useItem(f.player,f.level,book,InteractionHand.MAIN_HAND);
            var repeat=capture(f);f.check(repeat.data().isEmpty()&&repeat.learnSounds().isEmpty()&&ItemStack.matches(book,snapshot),"known CRIMSON use cannot replay warp, popup, sound or consume the reusable book");
            var tooltip=new ArrayList<Component>();book.getItem().appendHoverText(book,Item.TooltipContext.of(f.level),tooltip,TooltipFlag.NORMAL);
            f.check(tooltip.size()==2&&tooltip.get(0).getStyle().getColor().getValue()==0xAA00AA&&tooltip.get(1).getStyle().getColor().getValue()==0x0000AA,"actual original Rites lines retain dark-purple/dark-blue presentation");
            var saved=ItemStack.parse(f.level.registryAccess(),book.save(f.level.registryAccess())).orElseThrow();f.check(ItemStack.matches(saved,book),"reusable named/native-component Rites saves and reloads without a hidden spent flag");
            configure(true);knowledge.reset();knowledge.setInitializedAspects(true);knowledge.setWarpPerm(4);knowledge.setWarpCounter(77);f.mode(GameType.CREATIVE);
            f.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.player.setItemInHand(InteractionHand.OFF_HAND,book);capture(f);
            f.player.gameMode.useItem(f.player,f.level,book,InteractionHand.OFF_HAND);var quiet=capture(f);
            f.check(knowledge.isResearchComplete("CRIMSON")&&knowledge.getWarpPerm()==4&&knowledge.getWarpSticky()==0&&knowledge.getWarpCounter()==77
                            &&quiet.data().size()==2&&quiet.data().getFirst() instanceof KnowledgeSyncPayload&&quiet.data().get(1).equals(new ResearchNoticePayload("CRIMSON"))
                            &&quiet.learnSounds().size()==1&&f.player.getOffhandItem()==book&&ItemStack.matches(book,snapshot),
                    "actual creative offhand Wuss study still learns/notifies once, keeps physical book and suppresses only warp/counter mutation");
        }finally{ThaumcraftSettings.configureServer(original);}
        test.succeed();
    }
    public static void soapUsesNativeReleaseAndOriginalChances(GameTestHelper test){
        try(var f=new Fixture(test)){
            f.open();f.player.setPos(Vec3.atBottomCenterOf(f.base));var item=ThaumcraftEldritchItems.SANITY_SOAP.get();
            var knowledge=KnowledgeAccess.get(f.player);knowledge.reset();knowledge.setInitializedAspects(true);
            var early=namedSoap(3);f.player.setItemInHand(InteractionHand.MAIN_HAND,early);
            knowledge.setWarpPerm(9);knowledge.setWarpSticky(3);knowledge.setWarpTemp(7);knowledge.setWarpCounter(99);var before=knowledge.save();capture(f);
            begin(f,early);advance(f,5);f.player.releaseUsingItem();
            f.check(early.getCount()==3&&!f.player.isUsingItem()&&knowledge.save().equals(before)&&capture(f).data().isEmpty(),"real native release at195 elapsed/count5 does not spend soap, roll cleansing or mutate warp");
            f.check(item.getUseDuration(early,f.player)==200&&item.getUseAnimation(early)==UseAnim.BLOCK&&early.getMaxStackSize()==64,"actual soap keeps original200 duration/BLOCK animation/default64 stacking");
            for(int variant=0;variant<4;variant++){
                boolean ward=(variant&1)!=0,pure=(variant&2)!=0;f.mode(variant==3?GameType.CREATIVE:GameType.SURVIVAL);
                f.player.removeEffect(ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD));f.put(f.base,Blocks.AIR);f.player.setPos(Vec3.atBottomCenterOf(f.base));
                knowledge.setWarpPerm(9);knowledge.setWarpSticky(3);knowledge.setWarpTemp(7);knowledge.setWarpCounter(99);
                var soap=namedSoap(3);var sameComponents=soap.copy();sameComponents.setCount(2);f.player.setItemInHand(InteractionHand.MAIN_HAND,soap);capture(f);
                begin(f,soap);advance(f,6);
                if(ward)f.player.addEffect(new MobEffectInstance(ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD),1000,0));
                if(pure)f.level.setBlock(f.base,ThaumcraftSpa.PURE_WATER.get().defaultBlockState().setValue(LiquidBlock.LEVEL,1),TubeVariantsRuntimeChecks.QUIET);
                // Let actual native water-entry/splash bookkeeping settle before the declared cleansing RNG input.
                tick(f);tick(f);f.check(f.player.isUsingItem()&&f.player.getUseItemRemainingTicks()==4,"native flow entry settles before final release boundary");
                var fake=soap.copy();item.releaseUsing(fake,f.level,f.player,4);item.onUseTick(f.level,f.player,fake,4);
                f.check(soap.getCount()==3&&knowledge.getWarpTemp()==7&&f.player.isUsingItem(),"display-copy callbacks cannot spend another physical stack or clear its actor warp");
                f.check(f.player.getShoulderEntityLeft().isEmpty()&&f.player.getShoulderEntityRight().isEmpty(),"declared native player has two empty shoulder tags");
                long seed=seed(variant==3?.70F:.40F,variant==3?.80F:.55F);var expected=RandomSource.create(seed);float roll=expected.nextFloat();
                // Native1.21 Player.aiStep rolls nextInt200 for EACH non-null shoulder tag, even an empty one.
                // The actual traced order is the soap float, then these two vanilla rolls. No extra soap roll fits.
                expected.nextInt(200);expected.nextInt(200);float next=expected.nextFloat();f.level.random.setSeed(seed);
                tick(f);boolean sticky=variant!=0;var actual=capture(f);
                f.check(!f.player.isUsingItem()&&soap.getCount()==2&&ItemStack.matches(soap,sameComponents)&&knowledge.getWarpPerm()==9
                                &&knowledge.getWarpSticky()==(sticky?2:3)&&knowledge.getWarpTemp()==0&&knowledge.getWarpCounter()==(sticky?11:12),
                        "native automatic count4 release spends one even creative, keeps components and applies actual ward/flowing-fluid chance variant"+variant+" roll"+roll);
                var deltas=actual.data().stream().filter(WarpMessagePayload.class::isInstance).map(WarpMessagePayload.class::cast).toList();
                var expectedDeltas=sticky?List.of(new WarpMessagePayload(-1,(byte)1),new WarpMessagePayload(-7,(byte)2)):List.of(new WarpMessagePayload(-7,(byte)2));
                float observedNext=f.level.random.nextFloat();
                f.check(deltas.equals(expectedDeltas),"actual channel feedback keeps sticky-before-temporary order, variant"+variant+" expected="+expectedDeltas+" actual="+deltas);
                f.check(observedNext==next,"native frame consumes one soap float before two empty-shoulder checks, variant"+variant+" expectedNext="+Float.floatToRawIntBits(next)+" observedNext="+Float.floatToRawIntBits(observedNext)+" playerTicks="+f.player.tickCount+" water="+f.player.isInWater()+" block="+f.level.getBlockState(f.base)+" using="+f.player.isUsingItem());
                if(pure)f.check(f.level.getBlockState(f.base).is(ThaumcraftSpa.PURE_WATER.get())&&f.level.getBlockState(f.base).getValue(LiquidBlock.LEVEL)==1,"flowing pure fluid boosts soap without a new source-only condition or source consumption");
                if(ward)f.check(f.player.hasEffect(ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD)),"cleansing does not spend the actual Ward effect");
            }
            f.mode(GameType.SURVIVAL);f.player.removeEffect(ThaumcraftWarp.effect(ThaumcraftWarp.WARP_WARD));f.put(f.base,Blocks.AIR);f.player.setPos(Vec3.atBottomCenterOf(f.base));
            knowledge.setWarpPerm(0);knowledge.setWarpSticky(0);knowledge.setWarpTemp(0);knowledge.setWarpCounter(77);
            var empty=namedSoap(2);f.player.setItemInHand(InteractionHand.MAIN_HAND,empty);begin(f,empty);advance(f,4);capture(f);
            f.check(f.player.getShoulderEntityLeft().isEmpty()&&f.player.getShoulderEntityRight().isEmpty(),"zero-warp native player still has two empty shoulder tags");
            var expected=RandomSource.create(1234);expected.nextFloat();expected.nextInt(200);expected.nextInt(200);float following=expected.nextFloat();f.level.random.setSeed(1234);tick(f);
            f.check(empty.getCount()==1&&knowledge.getWarpCounter()==77&&capture(f).data().isEmpty()&&f.level.random.nextFloat()==following,"zero-warp scrub still spends one soap/one cleansing float before native shoulder checks, with no fake deltas or stale-counter refresh");
        }
        test.succeed();
    }
    public static void soapCrucibleRecipePaysActualIngredients(GameTestHelper test){
        try(var f=new Fixture(test)){
            f.open();f.put(f.base,ThaumcraftAlchemy.CRUCIBLE.get());var tile=(TileCrucible)f.level.getBlockEntity(f.base);
            var contents=new AspectList().add(Aspect.MIND,16).add(Aspect.ELDRITCH,16).add(Aspect.ORDER,16).add(Aspect.HEAL,16);
            var prepared=tile.saveWithoutMetadata(f.level.registryAccess());prepared.putShort("Heat",(short)151);prepared.putInt("Water",1000);contents.writeToNBT(prepared);tile.loadWithComponents(prepared,f.level.registryAccess());
            // Declared saved hot/filled/aspect input; actual physical catalyst, research gate and output payment below.
            var catalyst=new ItemEntity(f.level,f.base.getX()+.5,f.base.getY()+.4,f.base.getZ()+.5,new ItemStack(ThaumcraftGolemMaterials.TALLOW_BLOCK_ITEM.get()));
            catalyst.setThrower(f.player);catalyst.setDeltaMovement(Vec3.ZERO);f.check(f.level.addFreshEntity(catalyst),"actual physical tallow block catalyst enters native entity index");
            try{
                KnowledgeAccess.get(f.player).removeResearch("SANESOAP");f.check(CrucibleCrafting.find(f.level,catalyst,tile.getAspects())==null,"actual original SANESOAP research gates the tallow catalyst");
                KnowledgeAccess.get(f.player).addResearch("SANESOAP");var recipe=CrucibleCrafting.find(f.level,catalyst,tile.getAspects());
                f.check(recipe!=null&&recipe.id().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft","crucible/sanity_soap"))&&recipe.value().sourceOrder()==180,"actual loaded original SaneSoap recipe wins with four distinct16-aspect demands");
                int pending=TileCrucible.pendingOutputCount(f.level);f.level.getBlockState(f.base).entityInside(f.level,f.base,catalyst);
                f.check(catalyst.isRemoved()&&tile.tagAmount()==0&&tile.getWaterAmount()==950&&TileCrucible.pendingOutputCount(f.level)==pending+1,"real crucible collision spends one tallow, all64 essentia and original50 water before output");
                TileCrucible.tickOutputs(f.level);var outputs=f.drops().stream().filter(drop->drop instanceof EntitySpecialItem&&drop.getItem().is(ThaumcraftEldritchItems.SANITY_SOAP.get())).toList();
                f.check(outputs.size()==1&&outputs.getFirst().getItem().getCount()==1&&TileCrucible.pendingOutputCount(f.level)==pending,"actual paid durable crucible output is one functional soap, no fake catalogue result");
            }finally{catalyst.discard();tile.spillRemnants();TileCrucible.tickOutputs(f.level);f.drops();}
        }
        test.succeed();
    }
    private static ItemStack namedSoap(int count){var soap=new ItemStack(ThaumcraftEldritchItems.SANITY_SOAP.get(),count);soap.set(DataComponents.CUSTOM_NAME,Component.literal("same native soap"));var tag=new CompoundTag();tag.putInt("preserved",42);soap.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return soap;}
    private static void begin(Fixture f,ItemStack soap){f.check(f.player.gameMode.useItem(f.player,f.level,soap,InteractionHand.MAIN_HAND).consumesAction()&&f.player.isUsingItem()&&f.player.getUseItem()==soap,"real native RMB starts the actual held soap");}
    private static void tick(Fixture f){
        f.player.setDeltaMovement(Vec3.ZERO);
        int before=f.player.tickCount;
        // The native level owns age and old-position bookkeeping; the embedded game listener owns Player.tick.
        f.level.tickNonPassenger(f.player);
        f.player.connection.tick();
        f.check(f.player.tickCount==before+1,"one declared native player frame advances entity age once");
    }
    private static void advance(Fixture f,int remaining){for(int tick=0;tick<205&&f.player.isUsingItem()&&f.player.getUseItemRemainingTicks()>remaining;tick++)tick(f);f.check(f.player.isUsingItem()&&f.player.getUseItemRemainingTicks()==remaining,"actual native use counter reaches declared boundary"+remaining);}
    private static long seed(float low,float high){for(long seed=0;seed<100000;seed++){float roll=RandomSource.create(seed).nextFloat();if(roll>=low&&roll<high)return seed;}throw new AssertionError("No declared soap chance input");}
    private static void configure(boolean wuss){ThaumcraftSettings.configureServer(new ResearchSettings(false,true,true,true,wuss,0,100));}
    private record Capture(List<CustomPacketPayload> data,List<ClientboundSoundPacket> learnSounds){}
    private static Capture capture(Fixture f){
        f.connection.flushChannel();f.channel.runPendingTasks();Object packet;var data=new ArrayList<CustomPacketPayload>();var sounds=new ArrayList<ClientboundSoundPacket>();
        while((packet=f.channel.readOutbound())!=null){try{
            if(packet instanceof ClientboundSoundPacket sound&&sound.getSound().value().getLocation().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft","learn")))sounds.add(sound);
            if(!(packet instanceof ClientboundCustomPayloadPacket custom))continue;var id=custom.payload().type().id();
            if(!id.equals(KnowledgeSyncPayload.TYPE.id())&&!id.equals(ResearchNoticePayload.TYPE.id())&&!id.equals(WarpMessagePayload.TYPE.id()))continue;
            if(custom.payload() instanceof dev.architectury.impl.NetworkAggregator.BufCustomPacketPayload wrapped){
                var wire=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(wrapped.payload()),f.level.registryAccess());try{
                    data.add(id.equals(KnowledgeSyncPayload.TYPE.id())?KnowledgeSyncPayload.STREAM_CODEC.decode(wire):id.equals(ResearchNoticePayload.TYPE.id())?ResearchNoticePayload.STREAM_CODEC.decode(wire):WarpMessagePayload.STREAM_CODEC.decode(wire));
                    f.check(!wire.isReadable(),"actual native knowledge/warp feedback body has no ignored trailing bytes");
                }finally{wire.release();}
            }else data.add(custom.payload());
        }finally{ReferenceCountUtil.release(packet);}}
        return new Capture(List.copyOf(data),List.copyOf(sounds));
    }
}
