package thaumcraft.common.lib.capabilities;

import java.util.Objects;
import net.minecraft.world.entity.player.Player;
import thaumcraft.common.lib.research.PlayerKnowledge;

public final class KnowledgeAccess {
    private KnowledgeAccess() {}

    public static PlayerKnowledge get(Player player) {
        Objects.requireNonNull(player, "player");
        if (!(player instanceof PlayerKnowledgeHolder holder)) {
            throw new IllegalStateException("Thaumcraft player knowledge mixin was not applied");
        }
        return holder.thaumcraft$getPlayerKnowledge();
    }
}
