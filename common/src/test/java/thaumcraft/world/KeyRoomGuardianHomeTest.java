package thaumcraft.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.DungeonRooms;
import thaumcraft.common.lib.world.MazeGenerator;
import thaumcraft.common.lib.world.OuterLandsRules;

/**
 * The home of the guardians of a key room.
 *
 * <p>Oracle {@code thaumcraft/common/lib/world/dim/GenKeyRoom.java:110} (CFR 0.152 decompilation
 * of {@code Thaumcraft-1.7.10-4.2.3.5.jar}) - the guardian of the key room keeps the centre of its
 * room, the block {@code x+8, y+2, z+8}, as its home with a radius of sixteen, and the first
 * guardian of a hard key room is a champion (oracle {@code :111-113}).</p>
 */
class KeyRoomGuardianHomeTest {
    private static final int BASE = 50;

    private static DungeonRooms.Builder keyRoom(int difficulty, long seed) {
        MazeGenerator.Cell cell = new MazeGenerator.Cell((short) 0);
        cell.feature = OuterLandsRules.FEATURE_KEY_ROOM;
        DungeonRooms.Builder builder = new DungeonRooms.Builder();
        DungeonRooms.generateKeyRoom(builder, emptyView(), RandomSource.create(seed),
                RandomSource.create(seed * 31L + 7L), 0, 0, BASE, cell, difficulty);
        return builder;
    }

    /** A world of air: the room code asks for the blocks it would place. */
    private static DungeonRooms.View emptyView() {
        return new DungeonRooms.View() {
            @Override public boolean isAir(int x, int y, int z) { return true; }
            @Override public boolean isVoidBlock(int x, int y, int z) { return false; }
            @Override public boolean isSolidAdjacent(int x, int y, int z) { return false; }
            @Override public boolean isCosmeticSolidOrStair(int x, int y, int z) { return false; }
        };
    }

    @Test void everyGuardianKeepsTheCentreOfTheRoomAsItsHome() {
        DungeonRooms.Builder builder = keyRoom(2, 11L);
        List<DungeonRooms.Spawn> guardians = builder.spawns.stream()
                .filter(spawn -> spawn.kind() == DungeonRooms.SpawnKind.ELDRITCH_GUARDIAN).toList();
        assertTrue(!guardians.isEmpty(), "the key room holds its guardians");
        for (DungeonRooms.Spawn guardian : guardians) {
            DungeonRooms.Home home = guardian.home();
            assertNotNull(home, "the guardian of the original keeps a home");
            assertEquals(8, home.x(), "the home is the centre of the room");
            assertEquals(BASE + 2, home.y(), "the home is the floor of the room");
            assertEquals(8, home.z(), "the home is the centre of the room");
            assertEquals(16, home.radius(), "the radius of the home of the original is sixteen");
        }
    }

    @Test void theFirstGuardianOfAHardKeyRoomIsAChampion() {
        List<DungeonRooms.Spawn> guardians = keyRoom(3, 13L).spawns.stream()
                .filter(spawn -> spawn.kind() == DungeonRooms.SpawnKind.ELDRITCH_GUARDIAN).toList();
        assertTrue(guardians.get(0).champion(), "the first guardian of a hard key room is a champion");
        for (int index = 1; index < guardians.size(); ++index) {
            assertTrue(!guardians.get(index).champion(), "only the first guardian of a hard key room is a champion");
        }
    }
}
