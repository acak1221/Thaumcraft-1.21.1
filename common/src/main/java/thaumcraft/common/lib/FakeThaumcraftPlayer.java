package thaumcraft.common.lib;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Original TC4 {@code thaumcraft.common.lib.FakeThaumcraftPlayer}: the silent player the harvesting helpers use
 * when they need a player that never talks, never opens a window and never runs a command.
 *
 * <p>The original overrode the chat message, the command permission, the chunk coordinates and the GUI opening.
 * 1.21.1 has no chunk coordinates on the player and no command permission on the player, so the chat message and
 * the GUI opening are kept and the command permission is kept through the native game master block permission.
 */
public class FakeThaumcraftPlayer extends Player {
    public FakeThaumcraftPlayer(Level level, GameProfile profile) {
        super(level, BlockPos.ZERO, 0.0F, profile);
    }

    /** Original {@code addChatMessage(IChatComponent)}: the fake player never says anything. */
    @Override public void sendSystemMessage(Component message) {
        // The original player was silent.
    }

    /** Original {@code canCommandSenderUseCommand(int, String)}: the fake player runs no command. */
    @Override public boolean canUseGameMasterBlocks() {
        return false;
    }

    /** Original {@code openGui(Object, int, World, int, int, int)}: the fake player opens no window. */
    @Override public OptionalInt openMenu(MenuProvider provider) {
        return OptionalInt.empty();
    }

    @Override public boolean isSpectator() {
        return false;
    }

    @Override public boolean isCreative() {
        return false;
    }

    @Override public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        // The original player had no equipment.
    }

    @Override public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
}
