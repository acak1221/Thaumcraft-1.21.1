package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import thaumcraft.common.crafting.InfusionComponentPredicate;
import thaumcraft.common.crafting.InfusionPotionRequirement;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.lib.loot.LootTables;

/** Real manager recipes and exact original bag potions, not synthetic recipe registrations. */
public final class RunicPotionRuntimeChecks {
    private RunicPotionRuntimeChecks(){}
    public static void actualLootPotionsPayRunicRecipeIdentities(GameTestHelper test){
        var level=test.getLevel();
        for(String name:List.of("runic_amulet_emergency","runic_ring_charged","runic_ring_healing","runic_girdle_kinetic","runic_girdle_kinetic_2")){
            var recipe=(InfusionStackRecipe)level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft","infusion/"+name)).orElseThrow().value();
            var predicate=recipe.selectedComponents().get(3);String id=predicate.potion().orElseThrow();
            var components=new ArrayList<ItemStack>();for(var ingredient:recipe.components())components.add(ingredient.getItems()[0].copy());
            ItemStack old=LootTables.potion(InfusionPotionRequirement.legacyVariant(id));components.set(3,old);
            var input=new InfusionRecipeInput(recipe.input().getItems()[0].copy(),components);
            test.assertTrue(recipe.matches(input,level),"genuine original bag potion pays full manager recipe "+name+"/"+id);
            var nativePotion=predicate.displayStack(recipe.components().get(3).getItems()[0]);
            test.assertTrue(predicate.matches(nativePotion),"ordinary native potion holder still pays existing recipe "+name);
            ItemStack wrongForm=old.transmuteCopy(old.is(Items.POTION)?Items.SPLASH_POTION:Items.POTION);
            test.assertTrue(!predicate.matches(wrongForm),"normal/splash original form remains significant: "+name);
            PotionContents contents=old.get(DataComponents.POTION_CONTENTS);var effect=contents.customEffects().getFirst();
            for(int change=0;change<3;change++){
                var effects=new ArrayList<MobEffectInstance>();
                effects.add(new MobEffectInstance(effect.getEffect(),effect.getDuration()+(change==0?1:0),effect.getAmplifier()+(change==1?1:0)));
                if(change==2)effects.add(new MobEffectInstance(effect));
                ItemStack wrong=old.copy();wrong.set(DataComponents.POTION_CONTENTS,new PotionContents(contents.potion(),contents.customColor(),effects));
                test.assertTrue(!predicate.matches(wrong),"wrong legacy duration/amplifier/extra effects cannot stand in for original metadata: "+name+"/"+change);
            }
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
            try {
                InfusionStackRecipe.SERIALIZER.streamCodec().encode(buffer,recipe);
                var decoded=InfusionStackRecipe.SERIALIZER.streamCodec().decode(buffer);
                test.assertTrue(!buffer.isReadable()&&decoded.matches(input,level),"actual native recipe wire retains exact loot-potion acquisition path: "+name);
            }finally{buffer.release();}
            var ops=level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            var encoded=InfusionComponentPredicate.CODEC.encodeStart(ops,predicate).getOrThrow();
            var selected=InfusionComponentPredicate.CODEC.parse(ops,encoded).getOrThrow();
            test.assertTrue(selected.matches(old)&&!selected.matches(wrongForm),"selected altar-session NBT retains exact old potion identity: "+name);
        }
        test.succeed();
    }
}
