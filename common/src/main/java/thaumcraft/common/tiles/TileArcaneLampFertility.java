package thaumcraft.common.tiles;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ThaumcraftBiologicalLamps;

/** Original two-victus pairing, exact runtime class, density cap7, one pair per300 eligible ticks. */
public final class TileArcaneLampFertility extends TileEssentiaLamp {
    private int count;
    public TileArcaneLampFertility(BlockPos pos, BlockState state) { super(ThaumcraftBiologicalLamps.FERTILITY_TILE.get(), pos, state, 0); }
    public int getPairingCount() { return count; }
    @Override protected void tickLamp() {
        if (charges < 4) draw(Fill.FERTILITY);
        if (!current() || isQuarantined()) return;
        if (charges > 1 && count++ % 300 == 0) updateAnimals();
    }
    private void updateAnimals() {
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, new AABB(worldPosition).inflate(7));
        for (Animal animal : animals) {
            if (animal.getAge() != 0 || animal.isInLove()) continue;
            var sameClass = animals.stream().filter(other -> other.getClass() == animal.getClass()).toList();
            if (sameClass.size() > 7) continue;
            Animal partner = null;
            for (Animal other : sameClass) {
                if (other.getAge() != 0 || other.isInLove()) continue;
                if (partner == null) { partner = other; continue; }
                if (!current() || charges < 2 || isQuarantined()) return;
                charges -= 2; // Pay BOTH before native in-love entity/particle callbacks.
                other.setInLove(null); partner.setInLove(null);
                if (current()) changed();
                return;
            }
        }
    }
}
