package thaumcraft.common.tiles;

import net.minecraft.SharedConstants;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InfusionSessionSaveIntegrityTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private final HolderLookup.Provider registries=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private TileInfusionMatrix tile() { return new TileInfusionMatrix(BlockEntityType.CHEST,BlockPos.ZERO,Blocks.CHEST.defaultBlockState()); }
    private CompoundTag paidSession() {
        CompoundTag tag=new CompoundTag(); tile().saveAdditional(tag,registries);
        tag.putBoolean("active",true); tag.putBoolean("crafting",true); tag.putInt("recipexp",7);
        tag.putString("recipeplayer","owner"); tag.putUUID("RecipePlayerId",new java.util.UUID(1,2));
        CompoundTag input=new CompoundTag(); input.put("Stack",new ItemStack(Items.DIAMOND).save(registries)); input.putInt("StoredCount",3);
        tag.put("recipeinput",input); tag.put("recipeout",input.copy());
        ListTag debt=new ListTag(); CompoundTag row=new CompoundTag(); row.putString("key","aer"); row.putInt("amount",12); debt.add(row); tag.put("Aspects",debt);
        return tag;
    }
    private void check(CompoundTag input) {
        var tile=tile(); tile.loadAdditional(input,registries);
        CompoundTag saved=new CompoundTag(); tile.saveAdditional(saved,registries);
        assertEquals(input,saved); assertTrue(tile.isDataRejected());
        assertFalse(tile.startCrafting(null));
        var next=tile(); next.loadAdditional(saved,registries); next.setRemoved();
        CompoundTag again=new CompoundTag(); next.saveAdditional(again,registries); assertEquals(input,again);
    }
    @Test void malformedPredicateDoesNotBecomeAnEmptyIngredient() {
        CompoundTag tag=paidSession(); ListTag list=new ListTag(); CompoundTag bad=new CompoundTag(); bad.putString("ingredient","unread predicate"); list.add(bad); tag.put("recipein",list); check(tag);
    }
    @Test void wrongRequirementElementDoesNotBecomeNoRequirements() {
        CompoundTag tag=paidSession(); ListTag list=new ListTag(); list.add(StringTag.valueOf("unread requirement")); tag.put("recipein",list); check(tag);
    }
    @Test void unreadCentralStackDoesNotErasePaidSession() {
        CompoundTag tag=paidSession(); tag.getCompound("recipeinput").getCompound("Stack").putString("id","missing_addon:paid_input"); check(tag);
    }
    @Test void validEmptyRequirementsAreStillReadable() {
        CompoundTag tag=new CompoundTag(); tile().saveAdditional(tag,registries);
        var restored=tile(); restored.loadAdditional(tag,registries);
        assertFalse(restored.isDataRejected());
        CompoundTag saved=new CompoundTag(); restored.saveAdditional(saved,registries); assertEquals(tag,saved);
    }
}
