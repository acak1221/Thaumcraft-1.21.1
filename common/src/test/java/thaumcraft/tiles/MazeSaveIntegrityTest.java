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
    @Test void largeCollidingMapsSaveIdenticallyAcrossInsertionOrdersAndRepeatedReloads() {
        // Diagonal packed coordinates all share Long.hashCode()==0, exercising
        // real ConcurrentHashMap collision/tree-bin ordering through native load.
        var rows = new java.util.ArrayList<CompoundTag>();
        for (int index = 0; index < 2048; index++) {
            CompoundTag row = new CompoundTag(); int coordinate = index - 1024;
            row.putInt("x", coordinate); row.putInt("z", coordinate); row.putShort("cell", (short)(512 + index % 31));
            rows.add(row);
        }
        CompoundTag expected = null;
        for (int order = 0; order < 4; order++) {
            var reordered = new java.util.ArrayList<>(rows);
            if (order == 1) java.util.Collections.reverse(reordered);
            if (order == 2) java.util.Collections.rotate(reordered, 731);
            if (order == 3) java.util.Collections.shuffle(reordered, new java.util.Random(20261025L));
            ListTag list = new ListTag(); reordered.forEach(row -> list.add(row.copy()));
            CompoundTag input = new CompoundTag(); input.put("cells", list);
            MazeSavedData maze = MazeSavedData.load(input, null);
            for (int index = 0; index < rows.size(); index++) assertEquals((short)(512 + index % 31),
                    maze.cell(index - 1024, index - 1024), "all original coordinate/value owners survive load");
            CompoundTag saved = maze.save(new CompoundTag(), null);
            assertEquals(2048, saved.getList("cells", Tag.TAG_COMPOUND).size(), "canonical save retains every distinct cell");
            if (expected == null) expected = saved.copy(); else assertEquals(expected, saved,
                    "same cell map has identical persistent NBT for insertion order " + order);
            for (int reload = 0; reload < 3; reload++) {
                maze = MazeSavedData.load(saved, null);
                CompoundTag next = maze.save(new CompoundTag(), null);
                assertEquals(saved, next, "full persistent NBT survives reload " + reload + " at insertion order " + order);
                saved = next;
            }
        }
    }
}
