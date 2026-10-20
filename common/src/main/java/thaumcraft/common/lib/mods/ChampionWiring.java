package thaumcraft.common.lib.mods;

import net.minecraft.server.level.ServerLevel;
import thaumcraft.common.lib.world.MazeGenerator;
import thaumcraft.common.lib.world.OuterLandsProvider;

/**
 * The bridge between the champion system and the Outer Lands labyrinth.
 *
 * <p>The 1.7 original answered the dangerous location of a spawn roll from the static maze
 * map of the labyrinth (oracle {@code EventHandlerEntity.isDangerousLocation:426-431} and
 * {@code MazeHandler.getFromHashMap}). The native port keeps the same cells in the saved
 * data of the world, so the champion subsystem reads them from there.
 */
public final class ChampionWiring {
    private ChampionWiring() {}

    /**
     * The loader hook of the champion subsystem. It starts the original spawn roll of a mob
     * that enters the world and it wires the labyrinth as the dangerous location of that
     * roll.
     */
    public static void init() {
        ChampionHooks.init();
        ChampionHooks.setDangerousLocation(ChampionWiring::dangerous);
    }

    /**
     * Original {@code EventHandlerEntity.isDangerousLocation:426-431}. The 1.7 original
     * called a place dangerous when it stood in the Outer Lands and in a maze cell whose
     * feature was the key room or the library room.
     */
    public static boolean dangerous(ServerLevel level, int x, int y, int z) {
        if (!OuterLandsProvider.isOuterLands(level)) return false;
        short packed = OuterLandsProvider.maze(level).cell(ChampionWiringRules.mazeChunk(x),
                ChampionWiringRules.mazeChunk(z));
        if (packed == 0) return false;
        return ChampionWiringRules.dangerousMazeFeature(new MazeGenerator.Cell(packed).feature);
    }
}
