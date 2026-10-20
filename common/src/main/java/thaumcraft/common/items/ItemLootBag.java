package thaumcraft.common.items;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.loot.LootBagRules;
import thaumcraft.common.lib.loot.LootTables;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Three flattened original ItemLootBag subtypes; paid results enter existing durable physical custody. */
public final class ItemLootBag extends Item {
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();
    private static final SavedData.Factory<DeferredItemDrops> DROPS = new SavedData.Factory<>(DeferredItemDrops::new,DeferredItemDrops::load,null);
    private static final SoundEvent COINS = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","coins"));
    private record Drop(ItemStack stack,Vec3 motion) {}
    private final int rarity;
    public ItemLootBag(Properties properties,int rarity) {
        super(properties.stacksTo(16));
        if(rarity<0||rarity>2)throw new IllegalArgumentException("Unknown loot bag tier");
        this.rarity=rarity;
    }
    public int lootRarity() { return rarity; }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        super.appendHoverText(stack,context,tooltip,flag); tooltip.add(Component.translatable("tc.lootbag"));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(ACTIVE.get()!=null || player.level()!=world || player.isSpectator() || stack.isEmpty() || stack.getItem()!=this)
            return InteractionResultHolder.fail(stack);
        if(world.isClientSide)return InteractionResultHolder.success(stack);
        if(!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer actor)
                || !level.getServer().isSameThread() || level.getServer().getLevel(level.dimension())!=level)
            return InteractionResultHolder.fail(stack);
        ACTIVE.set(Boolean.TRUE); boolean paid=false;
        try {
            int count=stack.getCount(); ItemStack witness=stack.copy(); Vec3 position=actor.position();
            if(!actorCurrent(level,actor))return InteractionResultHolder.fail(stack);
            List<Drop> drops=new ArrayList<>(); int rolls=8+level.random.nextInt(5);
            for(int i=0;i<rolls;i++) {
                ItemStack actual=LootTables.generateLoot(level,rarity,level.random);
                if(actual!=null && !actual.isEmpty())drops.add(new Drop(actual.copy(),LootBagRules.motion(Math::random)));
            }
            // Resolve/load the ACTUAL SavedData owner before payment; final native witnesses follow this callout.
            level.getDataStorage().computeIfAbsent(DROPS,"thaumcraft_pending_inventory_drops");
            BlockPos source=BlockPos.containing(position);
            if(!actorCurrent(level,actor) || !actor.position().equals(position) || actor.getItemInHand(hand)!=stack
                    || stack.getItem()!=this || stack.getCount()!=count || count<=0 || !ItemStack.isSameItemSameComponents(stack,witness))
                return InteractionResultHolder.fail(stack);
            stack.shrink(1); paid=true;
            for(Drop drop:drops)DeferredItemDrops.enqueueDirected(level,source,position,drop.motion(),drop.stack());
            level.playSound(null,position.x,position.y,position.z,COINS,SoundSource.PLAYERS,.75F,1F);
            // ServerPlayerGameMode's creative wrapper restores its original argument, as in 1.7.
            return InteractionResultHolder.consume(stack);
        } catch(RuntimeException failure) {
            if(!paid)throw failure;
            Thaumcraft.LOG.warn("Loot bag publication interrupted after native payment",failure);
            return InteractionResultHolder.consume(stack);
        } finally { ACTIVE.remove(); }
    }
    private static boolean actorCurrent(ServerLevel level,ServerPlayer actor) {
        return actor.level()==level && actor.isAlive() && !actor.isRemoved() && !actor.isSpectator()
                && level.getServer().getPlayerList().getPlayer(actor.getUUID())==actor;
    }
}
