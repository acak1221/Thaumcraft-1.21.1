package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.internal.WeightedRandomLoot;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftLoot;
import thaumcraft.common.lib.loot.LootPotionVariants;
import thaumcraft.common.lib.loot.LootTables;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Native weighted draws, mutable book aliases, actual bag payment, rejected ADD and exact durable replay. */
public final class LootBagRuntimeChecks {
    private static final String QUEUE="thaumcraft_pending_inventory_drops";
    private static final SavedData.Factory<DeferredItemDrops> FACTORY=new SavedData.Factory<>(DeferredItemDrops::new,DeferredItemDrops::load,null);
    private LootBagRuntimeChecks() {}
    public static void run(GameTestHelper test) {
        Check check=new Check(test);
        try { check.prepare(); } catch(RuntimeException|Error failure) { check.finish();throw failure; }
        test.runAfterDelay(5,()->{
            try { check.poolsAndPotions();check.drawOrderAndAliases();check.nativeChests();check.paidCancellation(); }
            catch(RuntimeException|Error failure) { check.finish();throw failure; }
        });
    }
    private static final class Script extends LegacyRandomSource {
        final int[] ints;final float[] floats;int i,f;
        Script(int[] ints,float...floats) { super(0);this.ints=ints;this.floats=floats; }
        @Override public int nextInt(int bound) { if(i>=ints.length)throw new AssertionError("Unexpected integer draw bound "+bound);int value=ints[i++];if(value<0||value>=bound)throw new AssertionError("Draw "+value+" outside "+bound);return value; }
        @Override public float nextFloat() { if(f>=floats.length)throw new AssertionError("Unexpected float draw");return floats[f++]; }
        void exhausted(GameTestHelper test) { test.assertTrue(i==ints.length&&f==floats.length,"all original RNG draws, and no additional draws, are consumed"); }
    }
    private static final class Check {
        final GameTestHelper test;final ServerLevel level;final BlockPos base;final ServerPlayer player;
        final Connection connection=new Connection(PacketFlow.SERVERBOUND);final EmbeddedChannel channel=new EmbeddedChannel(connection);
        final List<ChunkPos> forced=new ArrayList<>();final Map<UUID,CompoundTag> paid=new HashMap<>();final Set<UUID> delivered=new HashSet<>();
        EntityEvent.Add observer;ItemStack spent;boolean finished;int attempts;
        Check(GameTestHelper test) {
            this.test=test;level=test.getLevel();base=test.absolutePos(new BlockPos(3,100,3));
            player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"bag-"+UUID.randomUUID().toString().substring(0,8)),ClientInformation.createDefault());
        }
        void prepare() {
            level.getServer().getPlayerList().placeNewPlayer(connection,player,CommonListenerCookie.createInitial(player.getGameProfile(),false));
            player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.getInventory().clearContent();player.setPos(Vec3.atCenterOf(base));
            forced.addAll(GolemChunks.hold(level,List.of(base.offset(-3,-3,-3),base.offset(3,3,3))));LootTables.initialize();
        }
        List<WeightedRandomLoot> pool(int tier) { return tier==0?WeightedRandomLoot.lootBagCommon:tier==1?WeightedRandomLoot.lootBagUncommon:WeightedRandomLoot.lootBagRare; }
        void poolsAndPotions() {
            int[] counts={85,90,92},weights={2943,2913,2591};
            for(int tier=0;tier<3;tier++) {
                var pool=pool(tier);test.assertTrue(pool.size()==counts[tier]&&pool.stream().mapToInt(e->e.itemWeight).sum()==weights[tier],"original literal tier entry count and total weight "+tier);
                var coins=pool.getFirst();test.assertTrue(coins.item.is(ThaumcraftContent.MATERIALS.get("gold_coin").get())&&coins.item.getCount()==tier+1&&coins.itemWeight==2500-tier*250,"first weighted entry preserves original denomination and dominant weight");
                test.assertTrue(pool.stream().filter(e->e.item.is(Items.POTION)||e.item.is(Items.SPLASH_POTION)).count()==72,"all old potion variants retain separate native weighted entries");
                int expectedBottleWeight=tier==0?5:tier==1?10:20;
                test.assertTrue(pool.stream().anyMatch(e->e.item.is(Items.EXPERIENCE_BOTTLE)&&e.itemWeight==expectedBottleWeight),"old by item is an experience bottle, not a saddle");
            }
            for(var variant:LootPotionVariants.all()) {
                var potion=LootTables.potion(variant);var contents=potion.get(DataComponents.POTION_CONTENTS);var effects=contents.customEffects();
                test.assertTrue(contents.potion().isEmpty()&&effects.size()==1&&effects.getFirst().getDuration()==variant.duration()&&effects.getFirst().getAmplifier()==variant.amplifier()&&contents.getColor()==variant.color(),"native custom-only exact old potion effect/color "+variant.metadata());
                var roundTrip=ItemStack.parse(level.registryAccess(),potion.save(level.registryAccess())).orElseThrow();test.assertTrue(ItemStack.matches(roundTrip,potion),"potion item/count/components survive actual codec round trip");
            }
            Item[] weapons={Items.IRON_AXE,Items.IRON_SWORD,Items.GOLDEN_AXE,Items.GOLDEN_SWORD,ThaumcraftEquipment.THAUMIUM_SWORD.get(),Items.DIAMOND_SWORD,ThaumcraftEquipment.VOID_SWORD.get()};
            for(int quality=0;quality<7;quality++)test.assertTrue(LootTables.getGearItemForSlot(0,quality)==weapons[quality],"literal original weapon quality "+quality);
            test.assertTrue(LootTables.getGearItemForSlot(4,2)==Items.CHAINMAIL_HELMET&&LootTables.getGearItemForSlot(1,6)==ThaumcraftEquipment.VOID_BOOTS.get(),"original chain and void armor tiers");
            var first=ThaumcraftLoot.lootAmulet();var second=ThaumcraftLoot.lootAmulet();test.assertTrue(first!=second&&ItemStack.matches(first,second),"all amulet copies share the one process-time charge template");
        }
        void drawOrderAndAliases() {
            var originalCommon=WeightedRandomLoot.lootBagCommon;var originalUncommon=WeightedRandomLoot.lootBagUncommon;var originalRare=WeightedRandomLoot.lootBagRare;
            try {
                WeightedRandomLoot.lootBagCommon=new ArrayList<>();WeightedRandomLoot.lootBagUncommon=new ArrayList<>();WeightedRandomLoot.lootBagRare=new ArrayList<>();
                var a=new ItemStack(Items.DIAMOND,2);var b=new ItemStack(Items.EMERALD,3);ThaumcraftApi.addLootBagItem(a,2,(int[])null);ThaumcraftApi.addLootBagItem(b,3,0,1,2);
                ThaumcraftApi.addLootBagItem(new ItemStack(Items.BEDROCK),100,99);test.assertTrue(WeightedRandomLoot.lootBagCommon.size()==2&&WeightedRandomLoot.lootBagRare.size()==1,"null tier list defaults to common and unknown tier is ignored");
                Script left=new Script(new int[]{1});test.assertTrue(LootTables.generateLoot(level,0,left).is(Items.DIAMOND),"common tier never consumes a gear-gate float");left.exhausted(test);
                Script edge=new Script(new int[]{2});var copy=LootTables.generateLoot(level,0,edge);test.assertTrue(copy.is(Items.EMERALD)&&copy.getCount()==3&&copy!=b,"weighted subtraction crosses exact boundary and returns an independent actual copy");copy.shrink(1);test.assertTrue(b.getCount()==3,"caller cannot shrink public template by mutating returned copy");edge.exhausted(test);
                for(int quality=0;quality<=6;quality++) {
                    int start=quality==6?1:0;float[] gates=new float[6];gates[0]=0;for(int i=1;i<=5;i++)gates[i]=i<=quality-start?0:1;
                    Script gear=new Script(new int[]{start,0,0,3},gates);var item=LootTables.generateLoot(level,1,gear);test.assertTrue(item.getItem()==LootTables.getGearItemForSlot(0,quality)&&item.getDamageValue()==0,"actual gear uses five independent quality gates "+quality);gear.exhausted(test);
                }
                for(int tier=1;tier<=2;tier++) {
                    float threshold=.025F*tier;
                    Script exact=new Script(new int[]{0},threshold);test.assertTrue(LootTables.generateLoot(level,tier,exact).is(Items.EMERALD),"gear gate is STRICT at exact .025F*rarity boundary "+tier);exact.exhausted(test);
                    Script below=new Script(new int[]{0,0,0,3},Math.nextDown(threshold),1,1,1,1,1);test.assertTrue(LootTables.generateLoot(level,tier,below).is(Items.IRON_AXE),"gear gate accepts exactly one float step below its original boundary "+tier);below.exhausted(test);
                }
                float[] thresholds={.2F,.15F,.1F,.095F,.095F};
                Script equalQuality=new Script(new int[]{0,0,0,3},0,thresholds[0],thresholds[1],thresholds[2],thresholds[3],thresholds[4]);test.assertTrue(LootTables.generateLoot(level,1,equalQuality).is(Items.IRON_AXE),"all five original quality gates are STRICT at equality");equalQuality.exhausted(test);
                for(int gate=0;gate<thresholds.length;gate++) {
                    float[] values={0,thresholds[0],thresholds[1],thresholds[2],thresholds[3],thresholds[4]};values[gate+1]=Math.nextDown(thresholds[gate]);
                    Script below=new Script(new int[]{0,0,0,3},values);test.assertTrue(LootTables.generateLoot(level,1,below).is(Items.IRON_SWORD),"one float step below independent original quality threshold "+gate+" adds exactly one quality");below.exhausted(test);
                }
                var book=new ItemStack(Items.BOOK);WeightedRandomLoot.lootBagCommon.clear();WeightedRandomLoot.lootBagUncommon.clear();WeightedRandomLoot.lootBagRare.clear();ThaumcraftApi.addLootBagItem(book,1,0,1,2);
                var first=LootTables.generateLoot(level,0,RandomSource.create(47));var common=WeightedRandomLoot.lootBagCommon.getFirst().item;
                test.assertTrue(first.is(Items.ENCHANTED_BOOK)&&common==WeightedRandomLoot.lootBagUncommon.getFirst().item&&common==WeightedRandomLoot.lootBagRare.getFirst().item&&common!=book,"one original shared BOOK conversion updates all public tier aliases");
                Script cached=new Script(new int[]{0},1);var second=LootTables.generateLoot(level,2,cached);cached.exhausted(test);
                test.assertTrue(ItemStack.matches(first,second)&&second!=common,"later tier selection copies cached enchantments without another level/enchantment RNG draw");
                var registry=level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);for(var entry:second.get(DataComponents.STORED_ENCHANTMENTS).entrySet())test.assertTrue(registry.getHolderOrThrow(entry.getKey().unwrapKey().orElseThrow())==entry.getKey(),"returned cached book holders belong to the current native registry");
                // A declared FOREIGN native holder models a retained template from a previous server registry.
                var foreign=Holder.Reference.createStandAlone(new HolderOwner<Enchantment>() {},Enchantments.PROTECTION);
                var stale=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY.withTooltip(false));stale.set(foreign,2);
                common.set(DataComponents.STORED_ENCHANTMENTS,stale.toImmutable());common.set(DataComponents.ENCHANTMENTS,stale.toImmutable());
                Script reboundDraw=new Script(new int[]{0});var rebound=LootTables.generateLoot(level,0,reboundDraw);reboundDraw.exhausted(test);
                var expected=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY.withTooltip(false));expected.set(registry.getHolderOrThrow(Enchantments.PROTECTION),2);
                test.assertTrue(rebound.get(DataComponents.STORED_ENCHANTMENTS).equals(expected.toImmutable())&&rebound.get(DataComponents.ENCHANTMENTS).equals(expected.toImmutable()),"both foreign-registry components rebind exact key/level and hidden-tooltip state without RNG");
                test.assertTrue(ItemStack.matches(rebound,ItemStack.parse(level.registryAccess(),rebound.save(level.registryAccess())).orElseThrow()),"rebound retained book serializes through the actual current registry");
            } finally { WeightedRandomLoot.lootBagCommon=originalCommon;WeightedRandomLoot.lootBagUncommon=originalUncommon;WeightedRandomLoot.lootBagRare=originalRare; }
        }
        void nativeChests() {
            var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(base)).create(LootContextParamSets.CHEST);
            var table=level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);boolean bag=false;
            for(long seed=0;seed<2000&&!bag;seed++)for(var stack:table.getRandomItems(params,seed))if(stack.is(ThaumcraftLoot.LOOT_BAG_COMMON.get())||stack.is(ThaumcraftLoot.LOOT_BAG_UNCOMMON.get())||stack.is(ThaumcraftLoot.LOOT_BAG_RARE.get()))bag=true;
            test.assertTrue(bag,"actual native dungeon table, loaded through the Fabric weighted-pool hook, naturally reaches a bag");
            test.assertTrue(ThaumcraftLoot.dungeonEntries(level).size()==38,"Pech's original uniform descriptor list retains sixteen vanilla plus twenty-two TC entries");
        }
        DeferredItemDrops queue() { return level.getDataStorage().computeIfAbsent(FACTORY,QUEUE); }
        List<CompoundTag> rows(BlockPos source) {
            List<CompoundTag> result=new ArrayList<>();for(Tag value:queue().save(new CompoundTag(),level.registryAccess()).getList("Pending",Tag.TAG_COMPOUND)) {var row=(CompoundTag)value;if(row.getLong("Source")==source.asLong())result.add(row.copy());}return result;
        }
        ItemStack rowStack(CompoundTag row) { var stack=ItemStack.parse(level.registryAccess(),row.getCompound("Stack")).orElseThrow();stack.setCount(row.getInt("Count"));return stack; }
        void paidCancellation() {
            spent=new ItemStack(ThaumcraftLoot.LOOT_BAG_COMMON.get());player.setItemInHand(InteractionHand.MAIN_HAND,spent);Vec3 feet=player.position();
            level.random.setSeed(0);test.assertTrue(player.gameMode.useItem(player,level,spent,InteractionHand.MAIN_HAND).consumesAction()&&spent.isEmpty(),"actual connected survival use pays one real bag before publishing anything");
            var rows=rows(base);test.assertTrue(rows.size()==8,"original seed0 produces eight output rolls, independently of scatter Math.random");
            int coins=0,gold=0;for(var row:rows) {
                paid.put(row.getUUID("Id"),row);var stack=rowStack(row);if(stack.is(ThaumcraftContent.MATERIALS.get("gold_coin").get()))coins+=stack.getCount();if(stack.is(Items.GOLD_INGOT))gold+=stack.getCount();
                test.assertTrue(new Vec3(row.getDouble("X"),row.getDouble("Y"),row.getDouble("Z")).equals(feet)&&row.getDouble("DY")== (double).2F&&Math.abs(row.getDouble("DX"))<= (double).1F&&Math.abs(row.getDouble("DZ"))<= (double).1F,"durable output retains exact actual feet and original float-widened scatter");
            }
            test.assertTrue(coins==7&&gold==1,"literal original seed0 weighted results: seven single coins and one gold ingot");
            observer=(entity,world)-> { if(world!=level||!paid.containsKey(entity.getUUID()))return EventResult.pass();attempts++;test.assertTrue(spent.isEmpty(),"native payment precedes canceled physical ADD");queue().flush(level);return EventResult.interruptFalse(); };
            EntityEvent.ADD.register(observer);try {queue().flush(level);}finally {EntityEvent.ADD.unregister(observer);observer=null;}
            test.assertTrue(attempts==8&&rows(base).size()==8&&paid.keySet().stream().allMatch(id->level.getEntity(id)==null),"canceled actual ADD and recursive flush retain all eight paid outputs without physical duplicates");
            var saved=queue().save(new CompoundTag(),level.registryAccess());level.getDataStorage().set(QUEUE,DeferredItemDrops.load(saved,level.registryAccess()));
            test.assertTrue(rows(base).size()==8,"actual save/decode/storage replacement preserves all canceled paid output custody");
            player.setPos(Vec3.atCenterOf(base).add(20,0,0));
            observer=(entity,world)-> {
                var row=paid.get(entity.getUUID());if(world!=level||row==null)return EventResult.pass();
                test.assertTrue(entity instanceof ItemEntity item&&ItemStack.matches(item.getItem(),rowStack(row))&&entity.position().equals(new Vec3(row.getDouble("X"),row.getDouble("Y"),row.getDouble("Z")))&&entity.getDeltaMovement().equals(new Vec3(row.getDouble("DX"),row.getDouble("DY"),row.getDouble("DZ")))&&delivered.add(entity.getUUID()),"resumed native ADD publishes each exact saved item/count/components/trajectory once");
                queue().flush(level);return EventResult.pass();
            };
            EntityEvent.ADD.register(observer);
            test.runAfterDelay(22,()-> {try {test.assertTrue(delivered.size()==8&&rows(base).isEmpty(),"ordinary POST resumes all eight physical outputs after the bag no longer exists");creativeWrapper();}catch(RuntimeException|Error failure){finish();throw failure;}});
        }
        void creativeWrapper() {
            EntityEvent.ADD.unregister(observer);observer=null;player.setGameMode(GameType.CREATIVE);BlockPos source=base.above(8);player.setPos(Vec3.atCenterOf(source));
            var bag=new ItemStack(ThaumcraftLoot.LOOT_BAG_COMMON.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,bag);level.random.setSeed(3);
            test.assertTrue(player.gameMode.useItem(player,level,bag,InteractionHand.MAIN_HAND).consumesAction()&&player.getItemInHand(InteractionHand.MAIN_HAND)==bag&&bag.getCount()==2,"actual creative game-mode wrapper restores the original bag count, matching 1.7");
            var rows=rows(source);test.assertTrue(rows.size()==12,"original seed3 reaches maximum twelve output rolls");
            for(var row:rows)paid.put(row.getUUID("Id"),row);queue().flush(level);finish();test.succeed();
        }
        void finish() {
            if(finished)return;finished=true;if(observer!=null)EntityEvent.ADD.unregister(observer);
            for(UUID id:paid.keySet()) {var entity=level.getEntity(id);if(entity!=null)entity.discard();}
            if(!paid.isEmpty()) {var saved=queue().save(new CompoundTag(),level.registryAccess());var pending=saved.getList("Pending",Tag.TAG_COMPOUND);pending.removeIf(raw->paid.containsKey(((CompoundTag)raw).getUUID("Id")));saved.put("Pending",pending);level.getDataStorage().set(QUEUE,DeferredItemDrops.load(saved,level.registryAccess()));}
            for(ChunkPos chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
            try {if(level.getServer().getPlayerList().getPlayer(player.getUUID())==player)level.getServer().getPlayerList().remove(player);}finally {player.discard();channel.finishAndReleaseAll();}
        }
    }
}
