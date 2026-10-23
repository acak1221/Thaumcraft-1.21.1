package thaumcraft.tiles;

import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.OuterLandsProvider.MazeSavedData;
import static org.junit.jupiter.api.Assertions.*;

class MazeSaveIntegrityTest {
    private void retainedAndFenced(CompoundTag input) {
        MazeSavedData maze = MazeSavedData.load(input, null);
        assertEquals(input, maze.save(new CompoundTag(), null));
        assertThrows(IllegalStateException.class, () -> maze.cell(0, 0));
        assertThrows(IllegalStateException.class, () -> maze.mazesInRange(0, 0, 5, 5));
        assertThrows(IllegalStateException.class, () -> maze.storeLabyrinth(null, 0, 0));
        assertFalse(maze.availableForGeneration());
        assertTrue(maze.forGeneration(net.minecraft.core.BlockPos.ZERO).isEmpty());
        assertTrue(maze.forGeneration(new net.minecraft.core.BlockPos(16, 50, 16)).isEmpty());
        assertFalse(maze.isDirty());
        assertEquals(input, MazeSavedData.load(maze.save(new CompoundTag(), null), null).save(new CompoundTag(), null));
    }
    @Test void validEmptyMapRemainsAvailableToGeneration() {
        CompoundTag input = new CompoundTag(); input.put("cells", new ListTag());
        MazeSavedData maze = MazeSavedData.load(input, null);
        assertTrue(maze.availableForGeneration());
        assertSame(maze, maze.forGeneration(net.minecraft.core.BlockPos.ZERO).orElseThrow());
        assertEquals(0, maze.cell(0, 0));
        assertFalse(maze.mazesInRange(0, 0, 5, 5));
    }
    @Test void malformedCollectionsDoNotBecomeEmptyWorldMaps() {
        CompoundTag input = new CompoundTag(); input.putString("cells", "unread labyrinth"); retainedAndFenced(input);
        ListTag list = new ListTag(); list.add(StringTag.valueOf("unread cell")); input.put("cells", list); retainedAndFenced(input);
    }
    @Test void malformedCoordinatesAndDuplicateCellsAreNotNormalized() {
        CompoundTag input = new CompoundTag(), row = new CompoundTag();
        row.putString("x", "east"); row.putInt("z", 7); row.putShort("cell", (short)1);
        ListTag list = new ListTag(); list.add(row); input.put("cells", list); retainedAndFenced(input);
        row.putInt("x", 5); list.add(row.copy()); retainedAndFenced(input);
    }
}
