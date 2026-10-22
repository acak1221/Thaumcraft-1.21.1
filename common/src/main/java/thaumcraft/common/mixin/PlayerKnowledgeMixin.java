package thaumcraft.common.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.capabilities.PlayerKnowledgeHolder;
import thaumcraft.common.lib.research.PlayerKnowledge;

@Mixin(Player.class)
public abstract class PlayerKnowledgeMixin implements PlayerKnowledgeHolder {
    @Unique private static final String thaumcraft$KNOWLEDGE_KEY = "thaumcraft:player_knowledge";
    @Unique private final PlayerKnowledge thaumcraft$knowledge = new PlayerKnowledge();

    @Override
    public PlayerKnowledge thaumcraft$getPlayerKnowledge() {
        return thaumcraft$knowledge;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void thaumcraft$saveKnowledge(CompoundTag tag, CallbackInfo callback) {
        tag.put(thaumcraft$KNOWLEDGE_KEY, thaumcraft$knowledge.saveAttachment());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void thaumcraft$loadKnowledge(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains(thaumcraft$KNOWLEDGE_KEY)) {
            Player player = (Player)(Object)this;
            thaumcraft$knowledge.loadAttachment(tag.get(thaumcraft$KNOWLEDGE_KEY), player.getUUID()+" at "+player.blockPosition());
        }
    }
}
